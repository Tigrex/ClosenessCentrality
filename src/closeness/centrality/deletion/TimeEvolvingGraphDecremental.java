package closeness.centrality.deletion;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import closeness.centrality.entity.EdgeWithTimestamp;
import closeness.centrality.entity.SSSPTree;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TimeEvolvingGraphDecremental {
	
	private int numVertices;
	private int numSnapshots;
	
	private List<List<EdgeWithTimestamp>> condensedGraph;
	
	private List<Map<Integer, Set<Integer>>> deltaGraph;
	
	final private Logger logger = LoggerFactory.getLogger(TimeEvolvingGraphDecremental.class);
	
	public int getNumVertices() {
		return this.numVertices;
	}
	
	public int getNumSnapshots() {
		return this.numSnapshots;
	}
	
	public void constructGraph(String path) {
		this.constructGraph(path, false);
	}
	
	public void constructGraph(String path, boolean reverseEdges) {

		logger.debug("+constructGraph({}, reverseEdges = {})", path, reverseEdges);
		
		Set<Integer> vertices = new HashSet<Integer>();
		Set<Integer> timestamps = new HashSet<Integer>();
		
		
		Map<Integer, Map<Integer, Integer>> condensed = new HashMap<Integer, Map<Integer, Integer>>() ;
		
		Map<Integer, Map<Integer, Set<Integer>>> snapshots = new HashMap<Integer, Map<Integer, Set<Integer>>>();


		
		try (BufferedReader br = new BufferedReader(new FileReader(path))) {
		    String line;
		    String[] parts;

		    int numLines = 0;
		    
		    while ((line = br.readLine()) != null) {
		    	
		    	numLines++;
		    	if (numLines % 1000000 == 0) {
					logger.debug("Reading line {}...", numLines);
				}
		    	
		    	parts = line.split(",");
		    	
		    	int source, target;
		    	
		    	if (reverseEdges) {
		    		source = Integer.valueOf(parts[1]);
			    	target = Integer.valueOf(parts[0]);
		    	} else {
			    	source = Integer.valueOf(parts[0]);
			    	target = Integer.valueOf(parts[1]);
		    	}
		    	
		    	
		    	int timestamp = Integer.valueOf(parts[2]);
		    	
		    	vertices.add(source);
		    	vertices.add(target);
		    	timestamps.add(timestamp);
		    	
		    	// Update deltaGraph
	    		if (snapshots.containsKey(timestamp)) {
		    		Map<Integer, Set<Integer>> snapshot = snapshots.get(timestamp);
		    		
		    		if (snapshot.containsKey(source)) {
		    			if (snapshot.get(source).contains(target)) {
		    				logger.error("Duplicate edge found ({}, {}, {})", source, target, timestamp);
		    				System.exit(1);
		    			} else {
		    				snapshot.get(source).add(target);
		    			}
		    		} else {
		    			Set<Integer> outgoingVertices = new HashSet<Integer>();
		    			outgoingVertices.add(target);
		    			snapshot.put(source, outgoingVertices);
		    		}
		    	} else {
		    		Set<Integer> outgoingVertices = new HashSet<Integer>();
	    			outgoingVertices.add(target);
		    		Map<Integer, Set<Integer>> snapshot = new HashMap<Integer, Set<Integer>>();
	    			snapshot.put(source, outgoingVertices);
	    			snapshots.put(timestamp, snapshot);
		    	}
		    	
		    	
		    	// Update condensedGraph
		    	if (condensed.containsKey(source)) {
		    		
		    		Map<Integer, Integer> outgoingEdges = condensed.get(source);
		    		
		    		if (outgoingEdges.containsKey(target)) {
		    			logger.error("Duplicate edges found ({}, {}), with timestamps {}, {}.", source, target, timestamp, outgoingEdges.get(target));
		    			System.exit(1);
		    		} else {
		    			outgoingEdges.put(target, timestamp);
		    		}
		    		
		    	} else {
		    		Map<Integer, Integer> outgoingEdges = new HashMap<Integer, Integer>();
		    		outgoingEdges.put(target, timestamp);
		    		condensed.put(source, outgoingEdges);
		    	}
		    	
		    }
		    

			int minVertexId = Integer.MAX_VALUE;
			int maxVertexId = 0;
			int minTimestamp = Integer.MAX_VALUE;
			int maxTimestamp = 0;
			
			for (int v: vertices) {
				if (v < minVertexId) {
					minVertexId = v;
				}
				if (v > maxVertexId) {
					maxVertexId = v;
				}
			}
			
			for (int t: timestamps) {
				if (t < minTimestamp) {
					minTimestamp = t;
				}
				if (t > maxTimestamp) {
					maxTimestamp = t;
				}
			}
			
			logger.info("Number of edges is {}.", numLines);
			logger.info("Number of vertices is {}.", vertices.size());
			logger.info("Number of snapshots is {}.", timestamps.size());
			
			if (minVertexId != 0) {
				logger.error("Min vertex id is {}.", minVertexId);
			}
			
			if (minTimestamp != 0) {
				logger.error("Min timestamp is {}.", minTimestamp);
			}
			
			if (maxVertexId - minVertexId + 1 != vertices.size()) {
				logger.error("Vertex id normalization error. Max vertex id is {}.", maxVertexId);
				System.exit(1);
			}

			if (maxTimestamp - minTimestamp + 1 != timestamps.size()) {
				logger.error("Timestamp id normalization error. Max timestamp id is {}.", maxTimestamp);
				System.exit(1);
			}
			
			this.numVertices = vertices.size();
			this.numSnapshots = timestamps.size();

			// Build array-based snapshot graph
			this.deltaGraph = new ArrayList<Map<Integer, Set<Integer>>>(snapshots.size());
			for (int i = 0; i < snapshots.size(); i++) {
				Map<Integer, Set<Integer>> delta = snapshots.get(i);
				
				this.deltaGraph.add(delta);
			}
			
			// Build array-based condensed graph
			this.condensedGraph = new ArrayList<List<EdgeWithTimestamp>>(this.numVertices);
			for (int i = 0; i < this.numVertices; i++) {
				if (!condensed.containsKey(i)) {
					this.condensedGraph.add(new ArrayList<EdgeWithTimestamp>());
					continue;
				}
				
				Map<Integer, Integer> outgoingEdgesMap = condensed.get(i);
				List<EdgeWithTimestamp> outgoingEdgesList = new ArrayList<EdgeWithTimestamp>(outgoingEdgesMap.size());
				for (Integer target: outgoingEdgesMap.keySet()) {
					int timestamp = outgoingEdgesMap.get(target);
					outgoingEdgesList.add(new EdgeWithTimestamp(target, timestamp));
				}
				
				Collections.sort(outgoingEdgesList);
				this.condensedGraph.add(outgoingEdgesList);
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
		logger.debug("-constructGraph({}, reverseEdges = {})", path, reverseEdges);

	}
	
	
	public double[] getCentralityRangeBased(int source) {
		
//		long start = System.currentTimeMillis();
//		this.logger.info("+getCentralityRangeBased({})", source);
		
		int[] sccSize = new int[this.numSnapshots]; // Calculate scc size
		
		double[] centralities = new double[this.numSnapshots];
		Arrays.fill(centralities, 0);
		
		// Values default to be 0
		int[] totalDistances = new int[this.numSnapshots];
		
		int[] discoveredTime = new int[this.numVertices];
		Arrays.fill(discoveredTime, Integer.MAX_VALUE);
		
		int level = 0;
		
		int[] currentLevel = new int[this.numVertices];
		Arrays.fill(currentLevel, Integer.MAX_VALUE);
		int[] nextLevel = new int[this.numVertices];
		Arrays.fill(nextLevel, Integer.MAX_VALUE);

		currentLevel[source] = 0;
		
		int[] verticesPerTimestamp = new int[this.numSnapshots];

		boolean hasNext = true;
				
		while (hasNext) {
			
			Arrays.fill(verticesPerTimestamp, 0);
			
			for (int vertex = 0; vertex < this.numVertices; vertex++) {
				
				if (currentLevel[vertex] != Integer.MAX_VALUE) {
					
					// Newly discovered vertex, update distance at the end
					if (discoveredTime[vertex] == Integer.MAX_VALUE) {
						verticesPerTimestamp[currentLevel[vertex]]++;
					}
					// Existing vertex, update distance separately
					else {
						
						for (int time = currentLevel[vertex]; time < discoveredTime[vertex]; time++) {
							totalDistances[time] += level;
							sccSize[time]++; // Calculate scc size
						}
						
					}
					
					// Update discovered time
					discoveredTime[vertex] = currentLevel[vertex];
					
					//Add next level
					for (EdgeWithTimestamp edge: this.condensedGraph.get(vertex)) {
						
						int neighbor = edge.getTarget();
						int neighborDiscoverTime = Math.max(edge.getTimestamp(), currentLevel[vertex]);
						
						if (neighborDiscoverTime < discoveredTime[neighbor] && neighborDiscoverTime < currentLevel[neighbor]) {
							
							if (neighborDiscoverTime < nextLevel[neighbor]) {
								nextLevel[neighbor] = neighborDiscoverTime;
							}
							
						}
						
						
					}
					
				}
				
			}
			
			
			// Update distance
			for (int i = 0; i < this.numSnapshots; i++) {
								
				for (int j = i; j < this.numSnapshots; j++) {
					totalDistances[j] += level * verticesPerTimestamp[i];
					sccSize[j] += verticesPerTimestamp[i];
				}

			}	

			
			// Begin next iteration
			hasNext = false;
			for (int i = 0; i < this.numVertices; i++) {
				currentLevel[i] = nextLevel[i];
				if (nextLevel[i] != Integer.MAX_VALUE) {
					hasNext = true;
					nextLevel[i] = Integer.MAX_VALUE;
				}
				
			}
			
			level++;
			
		}
		
		
		for (int i = 0; i < this.numSnapshots; i++) {
			if (totalDistances[i] == 0) {
				centralities[i] = 0;
			} else {
				centralities[i] = 1.0 * (double)(sccSize[i] - 1) * (double)(sccSize[i] - 1) / (double)totalDistances[i] / (double)(this.numVertices - 1);
			}
			
		}
		

		
//		long end = System.currentTimeMillis();
//		this.logger.info("Calculating centrality range based time: {} seconds.", (end-start)*1.0/1000);
//		this.logger.info("-getCentralityRangeBased({})", source);
		
		return centralities;
				
	}
	
	public double[] getCentralityRangeSetBased(int source) {
		
		long start = System.currentTimeMillis();
		this.logger.info("+getCentralityRangeSetBased({})", source);
		
		int[] sccSize = new int[this.numSnapshots]; // Calculate scc size
		
		double[] centralities = new double[this.numSnapshots];
		Arrays.fill(centralities, 0);
		
		// Values default to be 0
		int[] totalDistances = new int[this.numSnapshots];
		
		int[] discoveredTime = new int[this.numVertices];
		Arrays.fill(discoveredTime, Integer.MAX_VALUE);
		
		int level = 0;
		
		Map<Integer, Integer> currentLevel = new HashMap<Integer, Integer>();
		Map<Integer, Integer> nextLevel = new HashMap<Integer, Integer>();
		currentLevel.put(source, 0);

		int[] verticesPerTimestamp = new int[this.numSnapshots];
		int[] startingPoints = new int[this.numSnapshots];
		int[] endingPoints = new int[this.numSnapshots];
		
		while (currentLevel.size() > 0) {
			
			Arrays.fill(verticesPerTimestamp, 0);
			Arrays.fill(startingPoints, 0);
			Arrays.fill(endingPoints, 0);
			
			for (int vertex: currentLevel.keySet()) {
				
				int currentTime = currentLevel.get(vertex);
				
				// Newly discovered vertex
				if (discoveredTime[vertex] == Integer.MAX_VALUE) {
					verticesPerTimestamp[currentTime]++;
				}
				// Existing vertex
				else {
					startingPoints[currentTime]++;
					endingPoints[discoveredTime[vertex]]++;
				}
				
				// Update discovered time
				discoveredTime[vertex] = currentTime;
				
				//Add next level
				for (EdgeWithTimestamp edge: this.condensedGraph.get(vertex)) {
					
					int neighbor = edge.getTarget();
					int neighborDiscoverTime = Math.max(edge.getTimestamp(), currentTime);
					
					if (neighborDiscoverTime < discoveredTime[neighbor] && (!currentLevel.containsKey(neighbor) || neighborDiscoverTime < currentLevel.get(neighbor))) {
						
						if (!nextLevel.containsKey(neighbor) || neighborDiscoverTime < nextLevel.get(neighbor)) {
							nextLevel.put(neighbor, neighborDiscoverTime);
						}
						
					}
					
					
				}
					
				
			}
			
			
			// Update distance
			int localSummary = verticesPerTimestamp[0];
			int positiveCount = startingPoints[0] - endingPoints[0];
			
			totalDistances[0] += level * (localSummary + positiveCount);
			sccSize[0] += (localSummary + positiveCount);
				
			for (int i = 1; i < this.numSnapshots; i++) {
				localSummary += verticesPerTimestamp[i];
				positiveCount += startingPoints[i] - endingPoints[i];

				totalDistances[i] += level * (localSummary + positiveCount);
				sccSize[i] += (localSummary + positiveCount);
			}
			
			
			// Begin next iteration
			currentLevel.clear();
			currentLevel.putAll(nextLevel);
			nextLevel = new HashMap<Integer, Integer>();
			level++;
			
		}
		
		
		for (int i = 0; i < this.numSnapshots; i++) {
			if (totalDistances[i] == 0) {
				centralities[i] = 0;
			} else {
				centralities[i] = 1.0 * (double)(sccSize[i] - 1) * (double)(sccSize[i] - 1) / (double)totalDistances[i] / (double)(this.numVertices - 1);
			}
			
		}
		

		
		long end = System.currentTimeMillis();
		this.logger.info("Calculating centrality range set based time: {} seconds.", (end-start)*1.0/1000);
		this.logger.info("-getCentralityRangeSetBased({})", source);
		
		return centralities;
				
	}

	public double getAverageNumberOfLabels(int source) {
		
		int totalLabels = 0;

		int[] sccSize = new int[this.numSnapshots]; // Calculate scc size
		
		int[] discoveredTime = new int[this.numVertices];
		Arrays.fill(discoveredTime, Integer.MAX_VALUE);
		
		int[] currentLevel = new int[this.numVertices];
		Arrays.fill(currentLevel, Integer.MAX_VALUE);
		int[] nextLevel = new int[this.numVertices];
		Arrays.fill(nextLevel, Integer.MAX_VALUE);

		currentLevel[source] = 0;
		
		int[] verticesPerTimestamp = new int[this.numSnapshots];
		int[] startingPoints = new int[this.numSnapshots];
		int[] endingPoints = new int[this.numSnapshots];
		
		boolean hasNext = true;
				
		while (hasNext) {
			
			Arrays.fill(verticesPerTimestamp, 0);
			Arrays.fill(startingPoints, 0);
			Arrays.fill(endingPoints, 0);
			
			for (int vertex = 0; vertex < this.numVertices; vertex++) {
				
				if (currentLevel[vertex] != Integer.MAX_VALUE) {

					totalLabels++;

					// Newly discovered vertex, update distance at the end
					if (discoveredTime[vertex] == Integer.MAX_VALUE) {
						verticesPerTimestamp[currentLevel[vertex]]++;
					}
					// Existing vertex, update distance separately
					else {
						
						startingPoints[currentLevel[vertex]]++;
						endingPoints[discoveredTime[vertex]]++;
						
					}
					
					// Update discovered time
					discoveredTime[vertex] = currentLevel[vertex];
					
					//Add next level
					for (EdgeWithTimestamp edge: this.condensedGraph.get(vertex)) {
						
						int neighbor = edge.getTarget();
						int neighborDiscoverTime = Math.max(edge.getTimestamp(), currentLevel[vertex]);
						
						if (neighborDiscoverTime < discoveredTime[neighbor] && neighborDiscoverTime < currentLevel[neighbor]) {
							
							if (neighborDiscoverTime < nextLevel[neighbor]) {
								nextLevel[neighbor] = neighborDiscoverTime;
							}
							
						}
						
						
					}
					
				}
				
			}
			
			
			// Update sccSize
			int localSummary = verticesPerTimestamp[0];
			int positiveCount = startingPoints[0] - endingPoints[0];
			
			sccSize[0] += (localSummary + positiveCount);
				
			for (int i = 1; i < this.numSnapshots; i++) {
				localSummary += verticesPerTimestamp[i];
				positiveCount += startingPoints[i] - endingPoints[i];

				sccSize[i] += (localSummary + positiveCount);
			}
			
			// Begin next iteration
			hasNext = false;
			for (int i = 0; i < this.numVertices; i++) {
				currentLevel[i] = nextLevel[i];
				if (nextLevel[i] != Integer.MAX_VALUE) {
					hasNext = true;
					nextLevel[i] = Integer.MAX_VALUE;
				}
				
			}
			
		}
		
		
		System.out.println("Total number of labels: " + totalLabels);
		System.out.println("Total number of reachable vertices in the last time stamp: " + sccSize[this.numSnapshots - 1]);
		
		double numOfLabels = 1.0 * totalLabels / sccSize[this.numSnapshots - 1];
		System.out.println("Average number of labels: " + numOfLabels);

		
		return numOfLabels;
	}
	
	public double[] getCentralityRangeBufferUpdate(int source) {
		
//		long start = System.currentTimeMillis();
//		this.logger.info("+getCentralityRangeBufferUpdate({})", source);
		
		int[] sccSize = new int[this.numSnapshots]; // Calculate scc size
		
		double[] centralities = new double[this.numSnapshots];
		Arrays.fill(centralities, 0);
		
		// Values default to be 0
		int[] totalDistances = new int[this.numSnapshots];
		
		int[] discoveredTime = new int[this.numVertices];
		Arrays.fill(discoveredTime, Integer.MAX_VALUE);
		
		int level = 0;
		
		int[] currentLevel = new int[this.numVertices];
		Arrays.fill(currentLevel, Integer.MAX_VALUE);
		int[] nextLevel = new int[this.numVertices];
		Arrays.fill(nextLevel, Integer.MAX_VALUE);

		currentLevel[source] = 0;
		
		int[] verticesPerTimestamp = new int[this.numSnapshots];
		int[] startingPoints = new int[this.numSnapshots];
		int[] endingPoints = new int[this.numSnapshots];
		
		boolean hasNext = true;
				
		while (hasNext) {
			
			Arrays.fill(verticesPerTimestamp, 0);
			Arrays.fill(startingPoints, 0);
			Arrays.fill(endingPoints, 0);
			
			for (int vertex = 0; vertex < this.numVertices; vertex++) {
				
				if (currentLevel[vertex] != Integer.MAX_VALUE) {

					// Newly discovered vertex, update distance at the end
					if (discoveredTime[vertex] == Integer.MAX_VALUE) {
						verticesPerTimestamp[currentLevel[vertex]]++;
					}
					// Existing vertex, update distance separately
					else {
						
						startingPoints[currentLevel[vertex]]++;
						endingPoints[discoveredTime[vertex]]++;
						
					}
					
					// Update discovered time
					discoveredTime[vertex] = currentLevel[vertex];
					
					//Add next level
					for (EdgeWithTimestamp edge: this.condensedGraph.get(vertex)) {
						
						int neighbor = edge.getTarget();
						int neighborDiscoverTime = Math.max(edge.getTimestamp(), currentLevel[vertex]);
						
						if (neighborDiscoverTime < discoveredTime[neighbor] && neighborDiscoverTime < currentLevel[neighbor]) {
							
							if (neighborDiscoverTime < nextLevel[neighbor]) {
								nextLevel[neighbor] = neighborDiscoverTime;
							}
							
						}
						
						
					}
					
				}
				
			}
			
			
			// Update distance
			int localSummary = verticesPerTimestamp[0];
			int positiveCount = startingPoints[0] - endingPoints[0];
			
			totalDistances[0] += level * (localSummary + positiveCount);
			sccSize[0] += (localSummary + positiveCount);
				
			for (int i = 1; i < this.numSnapshots; i++) {
				localSummary += verticesPerTimestamp[i];
				positiveCount += startingPoints[i] - endingPoints[i];

				totalDistances[i] += level * (localSummary + positiveCount);
				sccSize[i] += (localSummary + positiveCount);
			}
			
			
			// Begin next iteration
			hasNext = false;
			for (int i = 0; i < this.numVertices; i++) {
				currentLevel[i] = nextLevel[i];
				if (nextLevel[i] != Integer.MAX_VALUE) {
					hasNext = true;
					nextLevel[i] = Integer.MAX_VALUE;
				}
				
			}
			
			level++;
			
		}
		
		
		for (int i = 0; i < this.numSnapshots; i++) {
//			System.out.println("t:" + i + ", n:" + sccSize[i] + ", d:" + totalDistances[i]);
//			if (i >= 0 && i <= 10) {
//				System.out.println("Number reachable vertices: " + sccSize[i]);
//				System.out.println("Total distances: " + totalDistances[i]);
//			}
			if (totalDistances[i] == 0) {
				centralities[i] = 0;
			} else {
				centralities[i] = 1.0 * (double)(sccSize[i] - 1) * (double)(sccSize[i] - 1) / (double)totalDistances[i] / (double)(this.numVertices - 1);
			}
			
		}
//		System.out.println();
		
//		long end = System.currentTimeMillis();
//		this.logger.info("Calculating centrality range based buffer update time: {} seconds.", (end-start)*1.0/1000);
//		this.logger.info("-getCentralityRangeBufferUpdate({})", source);
		
		return centralities;
				
	}

	
	
	public void maxDegreeTest() {
		
		for (int i = 17; i <= 21; i++) {
			String path = "data/Scale" + i + "_Edge16.raw.uniform.2000";
			this.maxDegreeTestHelper(path);
		}
		
		int[] timestamps = {100, 200, 500, 1000, 4000, 8000};
		
		for (int i = 0; i < timestamps.length; i++) {
			String path = "data/Scale21_Edge16.raw.uniform." + timestamps[i];
			this.maxDegreeTestHelper(path);
		}
		
	}
	
	private long[] maxDegreeTestHelper(String path) {
		
		this.logger.info("+maxDegreeTest({})", path);

		this.constructGraph(path, false);

		// Get max degree vertex
		int maxDegreeVertex = 0;
		int maxDegree = this.condensedGraph.get(maxDegreeVertex).size();
		for (int i = 0; i < this.numVertices; i++) {
			int degree = this.condensedGraph.get(i).size();
			if (degree > maxDegree) {
				maxDegree = degree;
				maxDegreeVertex = i;
			}
		}
		this.logger.debug("Vertex {} has the max degree {}.", maxDegreeVertex, maxDegree);

		long[] runningTimes = this.performanceTestHelper(maxDegreeVertex);
		
		this.logger.info("{},{}", runningTimes[0], runningTimes[1]);

		this.logger.info("-maxDegreeTest({})", path);

		return runningTimes;
		
	}
	
	public void randomDegreeTest(int numOfRuns) {
		
		for (int i = 17; i <= 21; i++) {
			String path = "data/Scale" + i + "_Edge16.raw.uniform.2000";
			this.randomDegreeTestHelper(path, numOfRuns);
		}
		
		int[] timestamps = {100, 200, 500, 1000, 4000, 8000};
		
		for (int i = 0; i < timestamps.length; i++) {
			String path = "data/Scale21_Edge16.raw.uniform." + timestamps[i];
			this.randomDegreeTestHelper(path, numOfRuns);
		}
		
	}
	
	private long[] randomDegreeTestHelper(String path, int numOfRuns) {
		
		this.logger.info("+randomDegreeTestHelper({},{})", path, numOfRuns);

		this.constructGraph(path, false);

		long[] totalRunningTimes = new long[2];
		totalRunningTimes[0] = 0;
		totalRunningTimes[1] = 0;
		
		// Get random degrees
		Random random = new Random(0);
		for (int i = 0; i < numOfRuns; i++) {
			int degree = random.nextInt(this.numVertices);
			long[] runningTimes = this.performanceTestHelper(degree);
			this.logger.info("{}:{},{}", degree, runningTimes[0], runningTimes[1]);
			
			totalRunningTimes[0] += runningTimes[0];
			totalRunningTimes[1] += runningTimes[1];
			
		}
				 
		this.logger.info("Total running time {},{}", totalRunningTimes[0], totalRunningTimes[1]);
		this.logger.info("-randomDegreeTestHelper({})", path);

		return totalRunningTimes;
		
	}
	
	
	public void syntheticGraphTestHelper(int numOfRuns) {
		
//		String scale = "21";
//		String[] timestamps = {"100", "200", "500", "1000", "2000", "4000", "8000"};
//		
//		for (String timestamp: timestamps) {
//			String path = "data/Scale" + scale + "_Edge16.raw.uniform." + timestamp;	
//			this.constructGraph(path);
//			realGraphTestHelper(path, numOfRuns);
//		}
		
		for (int scale = 17; scale <= 21; scale++) {
			String path = "data/Scale" + scale + "_Edge16.raw.uniform.2000";	
			this.constructGraph(path);
			realGraphTestHelper(path, numOfRuns);
		}
		
	}
	
	
	public void realGraphTestHelper(String path, int numOfRuns) {
		
		this.logger.info("+realGraphTestHelper({},{})", path, numOfRuns);

		long[] totalRunningTimes = new long[2];
		totalRunningTimes[0] = 0;
		totalRunningTimes[1] = 0;
		
		// Get random degrees
		Random random = new Random(0);
		for (int i = 0; i < numOfRuns; i++) {
			int degree = random.nextInt(this.numVertices);
			long[] runningTimes = this.performanceTestHelper(degree);
			this.logger.info("{}:{},{}", degree, runningTimes[0], runningTimes[1]);
			
			totalRunningTimes[0] += runningTimes[0];
			totalRunningTimes[1] += runningTimes[1];
			
		}
				 
		this.logger.info("Total running time {},{}", totalRunningTimes[0], totalRunningTimes[1]);
		this.logger.info("-realGraphTestHelper({})", path);

		
	}

	
	
	public void generateSourceIds(int numOfRuns) {
		
		// Get random degrees
		Random random = new Random(0);
		for (int i = 0; i < numOfRuns; i++) {
			int degree = random.nextInt(this.numVertices);
			System.out.println(degree);
		}
		
	}

	public void averageNumOfLabelsTestHelper(String path, int numOfRuns) {
		
		this.logger.info("+averageNumOfLabelsTestHelper({},{})", path, numOfRuns);

		double total = 0;
		
		// Get random degrees
		Random random = new Random(0);
		for (int i = 0; i < numOfRuns; i++) {
			int degree = random.nextInt(this.numVertices);
			
			double avgLabels = this.getAverageNumberOfLabels(degree);
			total += avgLabels;
		}
		
		double avg = total / numOfRuns;
		this.logger.info("Average number of labels of {} vertices is {}.", numOfRuns, avg);
		this.logger.info("-averageNumOfLabelsTestHelper({})", path);
		
	}
	
	
	private long[] performanceTestHelper(int sourceVertex) {
		
		long start = System.currentTimeMillis();
		double[] centralities1 = this.getCentralityDynamic(sourceVertex);
		long end = System.currentTimeMillis();
		long time1 = end - start;
		
		start = System.currentTimeMillis();
		double[] centralities2 = this.getCentralityRangeBufferUpdate(sourceVertex);
		end = System.currentTimeMillis();
		long time2 = end - start;

		// Correctness test
		if (compareDoubleArray(centralities1, centralities2) == false) {
			throw new RuntimeException("Results are wrong: Number of centralities not equal.");
		}
		
		long[] runningTimes = new long[2];
		runningTimes[0] = time1;
		runningTimes[1] = time2;
		
		return runningTimes;
	}
	
	
	public boolean correctnessTest(String path) {

		this.logger.info("+correctnessTest({})", path);
		
		this.constructGraph(path, false);
		
		for (int i = 0; i < 10; i++) {
			double[] centralities1 = this.getCentralityDynamic(i + this.numVertices / 2);
			double[] centralities3 = this.getCentralityRangeBased(i + this.numVertices / 2);
			double[] centralities4 = this.getCentralityRangeBufferUpdate(i + this.numVertices / 2);
			double[] centralities5 = this.getCentralityRangeSetBased(i + this.numVertices / 2);
			
			if (compareDoubleArray(centralities1, centralities3) == false) {
				this.logger.error("Centralities1 and centralities3 are not equal.");
				this.logger.info("-correctnessTest({})", path);
				return false;
			}
			
			if (compareDoubleArray(centralities1, centralities4) == false) {
				this.logger.error("Centralities1 and centralities4 are not equal.");
				this.logger.info("-correctnessTest({})", path);
				return false;
			}
			
			if (compareDoubleArray(centralities1, centralities5) == false) {
				this.logger.error("Centralities1 and centralities5 are not equal.");
				this.logger.info("-correctnessTest({})", path);
				return false;
			}
			
		}
		
		this.logger.info("Successfully passed all tests (Dynamic, Range, RangeModified).");
		this.logger.info("-correctnessTest({})", path);
		return true;
		
	}
	
	private boolean compareDoubleArray(double[] da1, double[] da2) {
		if (da1.length != da2.length) {
			return false;
		}
		for (int i = 0; i < da1.length; i++) {
			if (compareDouble(da1[i], da2[i]) == false) {
				logger.debug("Index {} not equal.", i);
				return false;
			}
		}
		return true;
	}

	private boolean compareDouble(double d1, double d2) {
		
		if (Math.abs(d1 - d2) <= 0.0000001) {
			return true;
		}
		
		return false;
		
	}
	
	public double[] getCentralityDynamic(int source) {
		
		double[] centralities = new double[this.numSnapshots];
		
		Map<Integer, Set<Integer>> initialGraph = new HashMap<Integer, Set<Integer>>();
		this.mergeDelta(initialGraph, this.deltaGraph.get(0));

		int[] distance = new int[this.numVertices];
		for (int i = 0; i < this.numVertices; i++) {
			distance[i] = Integer.MAX_VALUE;
		}
		
		int totalDistances = 0;		
		int numReachableVertices = 1;
		
		int level = 0;
		
		Set<Integer> currentLevel = new HashSet<Integer>();
		Set<Integer> nextLevel = new HashSet<Integer>();

		currentLevel.add(source);
		distance[source] = 0;
		
		while (currentLevel.size() > 0) {
			
			// Update distance
			totalDistances += level * currentLevel.size();

			// Add next level
			for (int vertex: currentLevel) {
				
				if (!initialGraph.containsKey(vertex)) continue;
				
				for (Integer child: initialGraph.get(vertex)) {
					
					int previousLevel = distance[child];
					if (previousLevel < level) continue;
					if (currentLevel.contains(child) || nextLevel.contains(child)) continue;
					
					distance[child] = level + 1;
					nextLevel.add(child);
					numReachableVertices++;
										
				}
				
			}
			
			// Begin next iteration
			currentLevel.clear();
			currentLevel.addAll(nextLevel);
			nextLevel.clear();
			
			level++; 
			
		}
		double initialCentrality = 0.0;
		if (totalDistances != 0) {
			initialCentrality = 1.0 * (double)(numReachableVertices - 1) * (double)(numReachableVertices - 1) / (double)totalDistances / (double)(this.numVertices - 1);
		}
//		System.out.println("Num reachable vertices: " + numReachableVertices);
//		System.out.println("Total distances: " + totalDistances);
		centralities[0] = initialCentrality;
		
		
		for (int t = 1; t < this.numSnapshots; t++) {
//			System.out.println("Snapshot " + t);
			Map<Integer, Set<Integer>> delta = this.deltaGraph.get(t);
			
			this.mergeDelta(initialGraph, delta);
			
			Map<Integer, Integer> affectedVertices = new HashMap<Integer, Integer>();
			
			// Get all directly affected vertices
			for (int from: delta.keySet()) {
				int levelFrom = distance[from];
				
				if (levelFrom == Integer.MAX_VALUE) continue;
				
				for (int to: initialGraph.get(from)) {
					int levelTo = distance[to];
					if (levelTo != Integer.MAX_VALUE && levelTo <= levelFrom + 1) continue;
					
//					distance[to] = levelFrom + 1;
//					if (levelTo == Integer.MAX_VALUE) {
//						totalDistances += levelFrom + 1;
//						numReachableVertices++;
//					} else {
//						totalDistances -= levelTo - (levelFrom + 1);
//					}
					if (affectedVertices.containsKey(to)) {
						int previousValue = affectedVertices.get(to);
						if (levelFrom + 1 < previousValue) {
							affectedVertices.put(to, levelFrom + 1);
//							System.out.println("Type 1: " + to + " " + (levelFrom + 1));
						}
					} else {
						affectedVertices.put(to, levelFrom + 1);
//						System.out.println("Type 2: " + to + " " + (levelFrom + 1));
					}
					
				}	
			}
//			System.out.println("Number of affected vertices: " + affectedVertices.size());
			// If no affected vertices, centrality value remains the same
			if (affectedVertices.size() == 0) {
				centralities[t] = centralities[t - 1];
				continue;
			}
			
			
			// Sort affected vertices by level
			Map<Integer, List<Integer>> levelToVerticesMap = new HashMap<Integer, List<Integer>>();
			List<Integer> levels = new ArrayList<Integer>();
			for (int vertex: affectedVertices.keySet()) {
				int newLevel = affectedVertices.get(vertex);
				if (levelToVerticesMap.containsKey(newLevel)) {
					levelToVerticesMap.get(newLevel).add(vertex);
				} else {
					List<Integer> vertices = new ArrayList<Integer>();
					vertices.add(vertex);
					levelToVerticesMap.put(newLevel, vertices);
					levels.add(newLevel);
				}
			}
			Collections.sort(levels);
//			System.out.println("Printing levels:");
//			for (int lvl: levels) {
//				System.out.print(lvl + " ");
//			}
//			System.out.println();
			
			
			// Start from the lowest level among affected vertices
			int lvl = levels.remove(0);
//			System.out.println("Removed level: " + lvl);
			currentLevel.clear();
			nextLevel.clear();
			currentLevel.addAll(levelToVerticesMap.get(lvl));
//			System.out.println("Number of affected vertices: " + levelToVerticesMap.get(lvl).size());
			
			while (!levels.isEmpty() || !currentLevel.isEmpty()) {
				
				for (int vertex: currentLevel) {
					
					int previousLvl = distance[vertex];
					
					if (previousLvl < lvl) {
						logger.error("Previous level is even smaller.");
						logger.info("Previous level:{}, current level:{}", previousLvl, lvl);
						System.exit(1);
					}
					
					if (previousLvl == Integer.MAX_VALUE) {
						totalDistances += lvl;
						numReachableVertices++;
					} else {
						totalDistances -= (previousLvl - lvl);
					}
					
					distance[vertex] = lvl;
					
					if (!initialGraph.containsKey(vertex)) continue;
					
					for (int to: initialGraph.get(vertex)) {
						
						if (currentLevel.contains(to)) continue;
						
						int levelTo = distance[to];
						if (levelTo <= lvl + 1) continue;
						
						nextLevel.add(to);
					}
					
				}
				
				currentLevel.clear();
				currentLevel.addAll(nextLevel);
				nextLevel.clear();
				
				lvl++; 
				
				// Add other affected vertices at the same level
				if (levels.size() > 0 && levels.get(0) == lvl) {
					levels.remove(0);
					List<Integer> affectedV = levelToVerticesMap.get(lvl);
					for (int v: affectedV) {
						if (lvl < distance[v]) {
							currentLevel.add(v);
						}
					}
				}
				
				if (currentLevel.size() == 0 && !levels.isEmpty()) {
					lvl = levels.remove(0);
					List<Integer> affectedV = levelToVerticesMap.get(lvl);
					for (int v: affectedV) {
						if (lvl < distance[v]) {
							currentLevel.add(v);
						}
					}
				}
				
			}
			
//			if (t >= 1 && t <= 10) {
//				System.out.println("Num reachable vertices: " + numReachableVertices);
//				System.out.println("Total distances: " + totalDistances);
//			}
			double newCentrality = 1.0 * (double)(numReachableVertices - 1) * (double)(numReachableVertices - 1) / (double)totalDistances / (double)(this.numVertices - 1);
			centralities[t] = newCentrality;

		}
		
		
		return centralities;
	}
	
	
	private void mergeDelta(Map<Integer, Set<Integer>> initial, Map<Integer, Set<Integer>> delta) {
		for (int from: delta.keySet()) {
			for (int to:delta.get(from)) {
				
				if (initial.containsKey(from)) {
					initial.get(from).add(to);
				} else {
					Set<Integer> vertices = new HashSet<Integer>();
					vertices.addAll(delta.get(from));
					initial.put(from, vertices);
				}
			}
		}
	}
	
	
	@SuppressWarnings("unused")
	private SSSPTree buildSSSPTree(int source) {
		
		Map<Integer, Set<Integer>> initialGraph = this.deltaGraph.get(0);
		
		Map<Integer, Integer> nodeLevelMap = new HashMap<Integer, Integer>();
		Map<Integer, Set<Integer>> parentsMap = new HashMap<Integer, Set<Integer>>();
		Map<Integer, Set<Integer>> childrenMap = new HashMap<Integer, Set<Integer>>();
		
		int totalDistances = 0;		
		int numReachableVertices = 0;
		
		int level = 0;
		
		Set<Integer> currentLevel = new HashSet<Integer>();
		Set<Integer> nextLevel = new HashSet<Integer>();

		currentLevel.add(source);
		nodeLevelMap.put(source, 0);
		
		while (currentLevel.size() > 0) {
			
			// Update distance
			totalDistances += level * currentLevel.size();

			// Add next level
			for (int vertex: currentLevel) {
						
				if (!initialGraph.containsKey(vertex)) continue;
				
				for (Integer child: initialGraph.get(vertex)) {
					
					if (!nodeLevelMap.containsKey(child)) {
						nodeLevelMap.put(child, level + 1);
						Set<Integer> parents = new HashSet<Integer>();
						parents.add(vertex);
						parentsMap.put(child, parents);
						
						if (childrenMap.containsKey(vertex)) {
							Set<Integer> children = childrenMap.get(vertex);
							children.add(child);
						} else {
							Set<Integer> children = new HashSet<Integer>();
							children.add(child);
							childrenMap.put(vertex, children);
						}
						
						if (!nextLevel.contains(child)) {
							nextLevel.add(child);
						}
						
					} else {
						int bestLevel = nodeLevelMap.get(child);
						
						if (!currentLevel.contains(child) && bestLevel == level + 1) {
							if (parentsMap.containsKey(child)) {
								Set<Integer> parents = parentsMap.get(child);
								parents.add(vertex);
							} else {
								Set<Integer> parents = new HashSet<Integer>();
								parents.add(vertex);
								parentsMap.put(child, parents);
							}
							
							if (childrenMap.containsKey(vertex)) {
								Set<Integer> children = childrenMap.get(vertex);
								children.add(child);
							} else {
								Set<Integer> children = new HashSet<Integer>();
								children.add(child);
								childrenMap.put(vertex, children);
							}
							
							if (!nextLevel.contains(child)) {
								nextLevel.add(child);
							}
							
						}
					}
					
					
				}

				
			}
			
			// Begin next iteration
			currentLevel.clear();
			currentLevel.addAll(nextLevel);
			nextLevel.clear();
			
			level++; 
			
		}
		
		numReachableVertices = nodeLevelMap.size();
		
		SSSPTree tree = new SSSPTree(totalDistances, numReachableVertices, source, nodeLevelMap, parentsMap, childrenMap);
		
		return tree;
		
	}
	
	
	public void singleSourceCentralityTest(String path, int numOfQueries) {
	
		this.constructGraph(path, false);
		
		int[] vertexIDs = new int[numOfQueries];
		
		// Highest 100 degrees test
		this.logger.debug("+Highest({})", numOfQueries);
		for (int i = 0; i < vertexIDs.length; i++) {
			vertexIDs[i] = i;
		}
		this.singleSourceTestHelper(path, vertexIDs);
		this.logger.debug("-Highest({})", numOfQueries);

		
		// Lowest 100 degrees test
		this.logger.debug("+Lowest({})", numOfQueries);
		for (int i = 0; i < vertexIDs.length; i++) {
			vertexIDs[i] = this.numVertices - 1 - i;
		}
		this.singleSourceTestHelper(path, vertexIDs);
		this.logger.debug("-Lowest({})", numOfQueries);

		
		// Random 100 degrees test
		this.logger.debug("+Random({})", numOfQueries);
		Random rand = new Random(0);
		for (int i = 0; i < vertexIDs.length; i++) {
			vertexIDs[i] = rand.nextInt(this.numVertices);
		}
		this.singleSourceTestHelper(path, vertexIDs);
		this.logger.debug("-Random({})", numOfQueries);

	}

	public void optimizationTest(String path, int numOfQueries) {
		
		this.constructGraph(path, false);
		
		int[] vertexIDs = new int[numOfQueries];
		
		// Highest 100 degrees test
		this.logger.debug("+Highest({})", numOfQueries);
		for (int i = 0; i < vertexIDs.length; i++) {
			vertexIDs[i] = i;
		}
		this.optimizationTestHelper(path, vertexIDs);
		this.logger.debug("-Highest({})", numOfQueries);

		
		// Lowest 100 degrees test
		this.logger.debug("+Lowest({})", numOfQueries);
		for (int i = 0; i < vertexIDs.length; i++) {
			vertexIDs[i] = this.numVertices - 1 - i;
		}
		this.optimizationTestHelper(path, vertexIDs);
		this.logger.debug("-Lowest({})", numOfQueries);

		
		// Random 100 degrees test
		this.logger.debug("+Random({})", numOfQueries);
		Random rand = new Random(0);
		for (int i = 0; i < vertexIDs.length; i++) {
			vertexIDs[i] = rand.nextInt(this.numVertices);
		}
		this.optimizationTestHelper(path, vertexIDs);
		this.logger.debug("-Random({})", numOfQueries);

	}

	
	private void singleSourceTestHelper(String path, int[] sourceIDs) {
		
		long start, end;
		// Dynamic
		start = System.currentTimeMillis();
		for (int i = 0; i < sourceIDs.length; i++) {
			this.getCentralityDynamic(sourceIDs[i]);
		}
		end = System.currentTimeMillis();
		this.logger.info("Dynamic-based running time: {}.", (end-start)*1.0/1000);
		
		// Range-based
		start = System.currentTimeMillis();
		for (int i = 0; i < sourceIDs.length; i++) {
			this.getCentralityRangeBased(sourceIDs[i]);
		}
		end = System.currentTimeMillis();
		this.logger.info("Range-based running time: {}.", (end-start)*1.0/1000);
		
		// Range-based-modified
		start = System.currentTimeMillis();
		for (int i = 0; i < sourceIDs.length; i++) {
			this.getCentralityRangeBufferUpdate(sourceIDs[i]);
		}
		end = System.currentTimeMillis();
		this.logger.info("Range-based modified running time: {}.", (end-start)*1.0/1000);
		
	}
	

	private void optimizationTestHelper(String path, int[] sourceIDs) {
		
		long start, end;

		// Range-based
		this.logger.info("+Range-based tests.");
		for (int i = 0; i < sourceIDs.length; i++) {
			start = System.currentTimeMillis();
			this.getCentralityRangeBased(sourceIDs[i]);
			end = System.currentTimeMillis();
			System.out.println(end-start);
		}
		this.logger.info("-Range-based tests.");

		// Range-based-modified
		this.logger.info("+Range-based modified tests.");
		for (int i = 0; i < sourceIDs.length; i++) {
			start = System.currentTimeMillis();
			this.getCentralityRangeBufferUpdate(sourceIDs[i]);
			end = System.currentTimeMillis();
			System.out.println(end-start);
		}
		this.logger.info("-Range-based modified tests.");


		
	}
	
	public void printSnapshotSize(String path) {
		
		logger.debug("+printSnapshotSize({})", path);
		
		Map<Integer, Integer> edgeCount = new HashMap<Integer, Integer>();

		try (BufferedReader br = new BufferedReader(new FileReader(path))) {
		    String line;
		    String[] parts;

		    int numLines = 0;
		    
		    while ((line = br.readLine()) != null) {
		    	
		    	numLines++;
		    	if (numLines % 1000000 == 0) {
					logger.debug("Reading line {}...", numLines);
				}
		    	
		    	parts = line.split(",");
		    	int timestamp = Integer.valueOf(parts[2]);
		    	
		    	if (edgeCount.containsKey(timestamp)) {
		    		edgeCount.put(timestamp, edgeCount.get(timestamp) + 1);
		    	} else {
		    		edgeCount.put(timestamp, 1);
		    	}
		    }
		    	
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
		
		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(path + ".count"), "utf-8"));
			
			for (int i = 0; i < edgeCount.size(); i++) {
				writer.write(i + "," + edgeCount.get(i));
				writer.newLine();
			}
			
			writer.close();
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		logger.debug("-printSnapshotSize({})", path);
		
	}

	public void simpleTest(int source) {
		double[] centralities1 = this.getCentralityRangeBufferUpdate(source);
		
//		SSSPTree tree = this.buildSSSPTree(0);
//		double firstCentrality = tree.getCentrality(this.numVertices);
		
		double[] centralities2 = this.getCentralityDynamic(source);
		
		if (compareDoubleArray(centralities1, centralities2) == false) {
			this.logger.error("Centralities1 and centralities2 are not equal.");
		} else {
			this.logger.info("Test passed");
		}
		
	}
	
	
	public static void main(String[] args) {
		
		TimeEvolvingGraphDecremental graph = new TimeEvolvingGraphDecremental();
//		graph.maxDegreeTest();
//		graph.randomDegreeTest(100);
		
		/*
		// wiki reverse
		String path = "data/out.wikipedia-growth.teg.sim";
		graph.constructGraph(path, false, true);
		graph.realGraphTestHelper(path, 100);
		*/
		
//		String path = "data/youtube-growth-sorted.txt.teg.sim";
//		String path = "data/youtube-d-growth.txt.teg.sim";
//		String path = "data/out.wikipedia-growth.teg.sim";
//		String path = "data/dblp-2018-01-01.xml.teg.sim";
		String path = "data/Scale17_Edge16.raw.uniform.2000";
//		String path = "data/sample.txt";
		
		path = "data/dblp-2018-01-01.xml.teg.sim";
		graph.constructGraph(path);
		graph.generateSourceIds(100);
		
//		graph.realGraphTestHelper(path, 100);
		
//		graph.constructGraph(path);
//		long start = System.currentTimeMillis();
//		double[] centralities = graph.getCentralityRangeBufferUpdate(1000);
//		double[] centralities = graph.getCentralityDynamic(1);
//		long end = System.currentTimeMillis();
//		long time1 = end - start;
//		System.out.println("Range buffer update time: " + time1);
//		for (double centrality: centralities) {
//			System.out.println(centrality);
//		}
		
		
//		graph.syntheticGraphTestHelper(100);
		
//		graph.realGraphTestHelper(path, 100);
//		graph.averageNumOfLabelsTestHelper(path, 100);
		
		
//		graph.singleSourceCentralityTest(path, 100);
//		graph.optimizationTest(path, 100);
//		graph.printSnapshotSize(path);
//		graph.correctnessTest(path);
		
	}
	

}
