package closeness.centrality.topk;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import closeness.centrality.entity.EdgeWithTimestamp;

public class TimeEvolvingGraphInsertionOnly {
	
	private int numVertices;
	private int numSnapshots;
	
	private List<Map<Integer, List<Integer>>> snapshotsGraph;
	private List<List<EdgeWithTimestamp>> condensedGraph;
	
	private int[][] sccSizes;
	
	final private Logger logger = LoggerFactory.getLogger(TimeEvolvingGraphInsertionOnly.class);
	
	public int getNumVertices() {
		return this.numVertices;
	}
	
	public int getNumSnapshots() {
		return this.numSnapshots;
	}
	
	
	public void constructGraph(String path, boolean buildSnapshotGraph) {
		this.constructGraph(path, buildSnapshotGraph, false);
	}
	
	
	public void constructGraph(String path, boolean buildSnapshotGraph, boolean reverseEdges) {

		logger.debug("+constructGraph({}, buildSnapshotGraph = {}, reverseEdges = {})", path, buildSnapshotGraph, reverseEdges);
		
		Set<Integer> vertices = new HashSet<Integer>();
		Set<Integer> timestamps = new HashSet<Integer>();
		
		Map<Integer, Map<Integer, Set<Integer>>> snapshots = null;
		if (buildSnapshotGraph) {
			snapshots = new HashMap<Integer, Map<Integer, Set<Integer>>>();
		}
		
		Map<Integer, Map<Integer, Integer>> condensed = new HashMap<Integer, Map<Integer, Integer>>() ;
		
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
		    	
		    	// Update snapshotsGraph
		    	if (buildSnapshotGraph) {
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
			if (buildSnapshotGraph) {
				this.snapshotsGraph = new ArrayList<Map<Integer, List<Integer>>>(snapshots.size());
				for (int i = 0; i < snapshots.size(); i++) {
					Map<Integer, List<Integer>> newSnapshot = new HashMap<Integer, List<Integer>>();
					this.snapshotsGraph.add(newSnapshot);
				}
				
				for (int i = 0; i < snapshots.size(); i++) {
					Map<Integer, Set<Integer>> oldSnapshot = snapshots.get(i);

					for (int j = i; j < snapshots.size(); j++) {
						Map<Integer, List<Integer>> newSnapshot = this.snapshotsGraph.get(j);

						for (Integer source: oldSnapshot.keySet()) {
							Set<Integer> outgoingVerticesSet = oldSnapshot.get(source);
							
							List<Integer> outgoingVerticesList;
							if (newSnapshot.containsKey(source)) {
								outgoingVerticesList = newSnapshot.get(source);
							} else {
								outgoingVerticesList = new ArrayList<Integer>(outgoingVerticesSet.size());
								newSnapshot.put(source, outgoingVerticesList);
							}
							
							outgoingVerticesList.addAll(outgoingVerticesSet);
						}
						
					}
					
				}
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
		
		logger.debug("-constructGraph({}, buildSnapshotGraph = {}, reverseEdges = {})", path, buildSnapshotGraph, reverseEdges);

	}
	
	public double[] getCentralitySnapshotBased(int source, boolean useCondensedGraph) {
		
//		long start = System.currentTimeMillis();
//		this.logger.info("+getCentralitySnapshotBased({}, useCondensedGraph = {})", source, useCondensedGraph);
		
		double[] centralities = new double[this.numSnapshots];
		
		for (int i = 0; i < this.numSnapshots; i++) {
			if (useCondensedGraph) {
				centralities[i] = this.getSnapshotCentralityWithCG(source, i);
			} else {
				centralities[i] = this.getSnapshotCentralityWithSG(source, i);
			}
			
		}
		
//		long end = System.currentTimeMillis();
//		this.logger.info("Calculating centrality snapshot based time: {} seconds.", (end-start)*1.0/1000);
//		this.logger.info("-getCentralitySnapshotBased({}, useCondensedGraph = {})", source, useCondensedGraph);
		
		return centralities;
		
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
			if (totalDistances[i] == 0) {
				centralities[i] = 0;
			} else {
				centralities[i] = 1.0 * (double)(sccSize[i] - 1) * (double)(sccSize[i] - 1) / (double)totalDistances[i] / (double)(this.numVertices - 1);
			}
			
		}
		
		
//		long end = System.currentTimeMillis();
//		this.logger.info("Calculating centrality range based buffer update time: {} seconds.", (end-start)*1.0/1000);
//		this.logger.info("-getCentralityRangeBufferUpdate({})", source);
		
		return centralities;
				
	}

	
	
	public List<List<VertexCentrality>> getTopKBruteForce(int k, int maxNumber) {
		
		long start = System.currentTimeMillis();
		this.logger.info("+getTopKBruteForce()");
		
		List<List<VertexCentrality>> allCentrality = new ArrayList<List<VertexCentrality>>(this.numSnapshots);
		for (int i = 0; i < this.numSnapshots; i++) {
			List<VertexCentrality> centralityPerSnapshot = new ArrayList<VertexCentrality>(this.numVertices);
			allCentrality.add(centralityPerSnapshot);
		}
		
		List<List<Double>> currentTopKs = new ArrayList<List<Double>>(this.numSnapshots);
		
		for (int i = 0; i < this.numSnapshots; i++) {
			List<Double> topK = new ArrayList<Double>(k);
			currentTopKs.add(topK);
		}
		
		long previous = start;
		for (int i = 0; i < this.numVertices && i < maxNumber; i++) {
			
			if (i % 1000 == 0 && i > 0) {
				long end = System.currentTimeMillis();
				this.logger.debug("Processing vertex {}: {} seconds.", i, (end-previous)*1.0/1000);
				previous = end;
				
			}
			
			double[] centrality = getCentralityRangeBased(i);
			
			for (int j = 0; j < this.numSnapshots; j++) {
				allCentrality.get(j).add(new VertexCentrality(i, centrality[j]));
			}

			
		}

		
		List<List<VertexCentrality>> topKs = new ArrayList<List<VertexCentrality>>(this.numSnapshots);
		
		for (int i = 0; i < this.numSnapshots; i++) {
			Collections.sort(allCentrality.get(i));
			Collections.reverse(allCentrality.get(i));
			
			List<VertexCentrality> topK = new ArrayList<VertexCentrality>();
			for (int j = 0; j < k; j++) {
				topK.add(allCentrality.get(i).get(j));
			}
			topKs.add(topK);
		}
		
		
		long end = System.currentTimeMillis();
		this.logger.info("Calculating top k brute force time: {} seconds.", (end-start)*1.0/1000);
		this.logger.info("-getTopKBruteForce()");
		
		return topKs;
		
	}
	
	
	public List<List<VertexCentrality>> getTopKCentralityPruned(int k, int maxNumber) {
		
		long start = System.currentTimeMillis();
		System.out.println();
		System.out.println("+getTopKPruned()");

		List<List<VertexCentrality>> allCentrality = new ArrayList<List<VertexCentrality>>(this.snapshotsGraph.size());
		for (int i = 0; i < this.snapshotsGraph.size(); i++) {
			List<VertexCentrality> centralityPerSnapshot = new ArrayList<VertexCentrality>(this.numVertices);
			allCentrality.add(centralityPerSnapshot);
		}

		
		List<List<Double>> currentTopKs = new ArrayList<List<Double>>(this.snapshotsGraph.size());
		double[] threshold = new double[this.snapshotsGraph.size()];
		
		for (int i = 0; i < this.snapshotsGraph.size(); i++) {
			List<Double> topK = new ArrayList<Double>(k);
			currentTopKs.add(topK);
		}
		
		long previous = start;
		for (int i = 0; i < this.numVertices && i < maxNumber; i++) {
			
			if (i % 1000 == 0 && i > 0) {
				long end = System.currentTimeMillis();
				System.out.println("Processing vertex " + i + ": " + (end-previous)*1.0/1000 + " seconds.");
				previous = end;
				
//				System.out.print("Current top k: ");
//				for (int t = 0; t < this.snapshots.size(); t++) {
//					System.out.print(currentTopKs.get(t).get(0) + " ");
//				}
//				System.out.println();
			}
			
			double[] centrality = getCentralityPruned(i, threshold);
			
			for (int j = 0; j < this.snapshotsGraph.size(); j++) {
				allCentrality.get(j).add(new VertexCentrality(i, centrality[j]));
			
				if (currentTopKs.get(j).size() < k) {
					currentTopKs.get(j).add(centrality[j]);
				} else {
					if (centrality[j] > threshold[j]) {

						currentTopKs.get(j).add(centrality[j]);
						Collections.sort(currentTopKs.get(j));
						currentTopKs.get(j).remove(0);
						
						threshold[j] = currentTopKs.get(j).get(0);
						
					}
				}
			
			
			}

			
		}

		
		List<List<VertexCentrality>> topKs = new ArrayList<List<VertexCentrality>>(this.snapshotsGraph.size());
		
		for (int i = 0; i < this.snapshotsGraph.size(); i++) {
			Collections.sort(allCentrality.get(i));
			Collections.reverse(allCentrality.get(i));
			
			List<VertexCentrality> topK = new ArrayList<VertexCentrality>();
			for (int j = 0; j < k; j++) {
				topK.add(allCentrality.get(i).get(j));
			}
			topKs.add(topK);
		}
		
		
		long end = System.currentTimeMillis();
		System.out.println("-getTopKPruned()");
		System.out.println("Calculating top k pruned time: " + (end-start)*1.0/1000 + " seconds.");
		System.out.println();
		
		
		return topKs;
	}
	
	
		
//	public void findWCCsUnionFind() {
//		
//		System.out.println();
//		System.out.println("+findWCCsUnionFind()");
//		long start = System.currentTimeMillis();
//		
//		this.sccSizes = new int[this.numVertices][this.numSnapshots];
//		
//		List<List<Integer>> communitySizes = new ArrayList<List<Integer>>(this.snapshots.size());
//		
//		QuickUnion union = new QuickUnion(this.numVertices);
//
//		for (List<SimpleEdge> edges: this.normalizedSnapshots) {
//
//			for (SimpleEdge edge: edges) {
//				
//				if (!union.find(edge.getSource(), edge.getTarget())) {
//					union.unite(edge.getSource(), edge.getTarget());
//				}
//			}
//			
//			List<Integer> communitySize = new ArrayList<Integer>(this.numVertices);
//			for (int i = 0; i < this.numVertices; i++) {
//				communitySize.add(0);;
//			}
//			
//			List<List<Integer>> communities = union.getComponents();
//			for (int i = 0; i < communities.size(); i++) {
//				
//				List<Integer> community = communities.get(i);
//				for (int vertex: community) {
//					communitySize.set(vertex, community.size());
//				}
//				
//			}
//			
//			communitySizes.add(communitySize);
//			
//		}
//
//		for (int vertex = 0; vertex < this.numVertices; vertex++) {
//			for (int timestamp = 0; timestamp < this.numSnapshots; timestamp++) {
//				sccSizes[vertex][timestamp] = communitySizes.get(timestamp).get(vertex);
//			}
//		}
//
//		
//		long end = System.currentTimeMillis();
//		System.out.println("Calculating SCCs time: " + (end-start)*1.0/1000 + " seconds.");
//		System.out.println("-findWCCsUnionFind()");		
//		System.out.println();
//		
//	}
	
	
//	public void findWCCsBFS() {
//		
//		List<Integer> vertexStartingTimes = new LinkedList<>();
//		
//		List<List<Set<Integer>>> wccs = new ArrayList<List<Set<Integer>>>();
//		
//		for (int i = 0; i < this.snapshots.size(); i++) {
//			wccs.add(new ArrayList<Set<Integer>>());
//		}
//		
//		// Sort the vertices based on starting time
//		List<List<Integer>> startingTimes = new ArrayList<List<Integer>>(this.snapshots.size());
//		for (int i = 0; i < this.snapshots.size(); i++) {
//			startingTimes.add(new ArrayList<Integer>());
//		}
//		for (int i = 0; i < this.numVertices; i++) {
//			startingTimes.get(vertexStartingTimes.get(i)).add(i);
//		}
//		
//		List<Integer> sortedVertices = new ArrayList<Integer>(this.numVertices);
//		for (List<Integer> vertices: startingTimes) {
//			sortedVertices.addAll(vertices);
//		}
//		if (sortedVertices.size() != this.numVertices) {
//			System.out.println("Error, vertices not all included.");
//		}
//
//	
//		int numVisited = 0;
//		List<Boolean> visited = new ArrayList<Boolean>(this.numVertices);
//		for (int i = 0; i < this.numVertices; i++) {
//			visited.add(false);
//		}
//		
//		while (numVisited != this.numVertices) {
//			
//			// Find the next vertex to start wcc exploration
//			int source = -1;
//			for (int v: sortedVertices) {
//				if (!visited.get(v)) {
//					source = v;
//					break;
//				}
//			}
//			
//			// Add connected component container
//			int startingTime = vertexStartingTimes.get(source);
//			for (int i = startingTime; i < wccs.size(); i++) {
//				wccs.get(i).add(new HashSet<Integer>());
//			}
//			
//			
//			List<Integer> queue = new ArrayList<Integer>();
//			queue.add(source);
//
//			while (queue.size() > 0) {
//				int vertex = queue.remove(0);
//				visited.set(vertex, true);
//				
//				for (int i = vertexStartingTimes.get(vertex); i < wccs.size(); i++) {
//					List<Set<Integer>> snapshot = wccs.get(i);
//					snapshot.get(snapshot.size() - 1).add(i);
//				}
//				
//				Set<TemporalEdge> edges = this.normalizedTemporalGraph.get(vertex);
//				
//				for (TemporalEdge e: edges) {
//					
//					int target = e.getTarget();
//					
//					if (!visited.get(target)) {
//						queue.add(target);
//					}
//					
//				}
//				
//			}
//			
//			
//			
//		}
//		
//		
//	}
	
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
		double[] centrality1 = this.getCentralitySnapshotBased(sourceVertex, true);
		long end = System.currentTimeMillis();
		long time1 = end - start;
		
		start = System.currentTimeMillis();
		double[] centrality2 = this.getCentralityRangeBufferUpdate(sourceVertex);
		end = System.currentTimeMillis();
		long time2 = end - start;

		// Correctness test
		if (centrality1.length != centrality2.length) {
			throw new RuntimeException("Results are wrong: Number of centralities not equal.");
		}
		
		for (int i = 0; i < centrality1.length; i++) {
			if (!compareDouble(centrality1[i], centrality2[i])) {
				throw new RuntimeException("Results are wrong: centralities value not equal.");
			}
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
			double[] centralities1 = this.getCentralitySnapshotBased(i + this.numVertices / 2, true);
//			double[] centralities2 = this.getCentralitySnapshotBased(i, false);
			double[] centralities3 = this.getCentralityRangeBased(i + this.numVertices / 2);
			double[] centralities4 = this.getCentralityRangeBufferUpdate(i + this.numVertices / 2);
			double[] centralities5 = this.getCentralityRangeSetBased(i + this.numVertices / 2);
			
//			if (compareDoubleArray(centralities1, centralities2) == false) {
//				this.logger.error("Centralities1 and centralities2 are not equal.");
//				this.logger.info("-correctnessTest({})", path);
//				return false;
//			}
			
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
		
		this.logger.info("Successfully passed all tests (SnapshotCG, SnapshotSG, Range, RangeModified).");
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
	
	
	public double[] getCentralityPruned(int source, double[] threshold) {
		
		int[] sccSize = this.sccSizes[source];
		int[] visitedSize = new int[this.numSnapshots];
		
		boolean[] pruned = new boolean[this.numSnapshots];
		int prunedSnapshots = 0;
		
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
		
		boolean hasNext = true;
		
		int snapshotUpperbound = this.numSnapshots - 1;
		int snapshotLowerbound = Integer.MAX_VALUE;
				
		while (hasNext) {
			
			int[] verticesPerTimestamp = new int[this.numSnapshots];
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
							visitedSize[time]++;
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
			for (int i = snapshotLowerbound; i <= snapshotUpperbound; i++) {
								
				for (int j = i; j <= snapshotUpperbound; j++) {
					totalDistances[j] += level * verticesPerTimestamp[i];
					visitedSize[j] += verticesPerTimestamp[i];
				}

			}

			
			// Begin next iteration
			hasNext = false;
			snapshotLowerbound = Integer.MAX_VALUE;
			for (int i = 0; i < this.numVertices; i++) {
				currentLevel[i] = nextLevel[i];
				
				if (nextLevel[i] != Integer.MAX_VALUE) {
					hasNext = true;
					nextLevel[i] = Integer.MAX_VALUE;
				}
				
				if (currentLevel[i] != Integer.MAX_VALUE && currentLevel[i] < snapshotLowerbound) {
					snapshotLowerbound = currentLevel[i];
				}
				
			}
			
			level++;
			
			
			// Pruning
			int tempSnapshotUpperbound = 0;
			for (int i = snapshotLowerbound; i <= snapshotUpperbound; i++) {
				
				if (pruned[i]) {
					continue;
				}
				
				
				int communitySize = sccSize[i];
				
				int distanceLowerbound = totalDistances[i] + level * (communitySize - visitedSize[i]);
				double upperbound;
				if (distanceLowerbound - 0 < 0.00001) {
					upperbound = 0;
				} else {
					upperbound = 1.0 * (communitySize - 1) * (communitySize - 1) / distanceLowerbound / (numVertices - 1);
				}
				
				if (upperbound < threshold[i]) {
					centralities[i] = 0;
					pruned[i] = true;
					prunedSnapshots++;
				} else {
					tempSnapshotUpperbound = i;
				}
				
			}
			snapshotUpperbound = tempSnapshotUpperbound;
			
			if (prunedSnapshots == this.numSnapshots) {
				break;			
			}
			
			if (snapshotUpperbound < snapshotLowerbound) {
				break;
			}
			
			
		}
		
		for (int i = 0; i < this.numSnapshots; i++) {
			
			if (pruned[i]) {
				centralities[i] = 0;
				continue;
			}
		
			if (totalDistances[i] == 0) {
				centralities[i] = 0;
			} else {
				centralities[i] = 1.0 * (double)(sccSize[i] - 1) * (double)(sccSize[i] - 1) / (double)totalDistances[i] / (double)(this.numVertices - 1);
			}
			
		}
		
		return centralities;

	}
	
	
	private double getSnapshotCentralityWithSG(int source, int timestamp) {
		
		Map<Integer, List<Integer>> snapshot = this.snapshotsGraph.get(timestamp);
		
		int totalDistances = 0;
		
		boolean[] visited = new boolean[this.numVertices];
		Arrays.fill(visited, false);
		
		int level = 0;
		
		Set<Integer> currentLevel = new HashSet<Integer>();
		Set<Integer> nextLevel = new HashSet<Integer>();

		currentLevel.add(source);
		
		
		while (currentLevel.size() > 0) {
			
			// Update distance
			totalDistances += level * currentLevel.size();

			// Add next level
			for (int vertex: currentLevel) {
				
				visited[vertex] = true;
				
				if (snapshot.containsKey(vertex)) {
					for (Integer neighbor: snapshot.get(vertex)) {
						if (!visited[neighbor] && !nextLevel.contains(neighbor) && !currentLevel.contains(neighbor)) {
							nextLevel.add(neighbor);
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
		
		// Find connected component
		int sccSize = 0;
		for (int i = 0; i < visited.length; i++) {
			if (visited[i]) {
				sccSize++;
			}
		}
		
		
		if (totalDistances == 0) {
			return 0;
		}

		return  1.0 * (double)(sccSize - 1) * (double)(sccSize - 1) / (double)totalDistances / (double)(this.numVertices - 1);

	}
	
	private double getSnapshotCentralityWithCG(int source, int timestamp) {
		
		int totalDistances = 0;
		
		boolean[] visited = new boolean[this.numVertices];
		Arrays.fill(visited, false);
		
		int level = 0;
		
		Set<Integer> currentLevel = new HashSet<Integer>();
		Set<Integer> nextLevel = new HashSet<Integer>();

		currentLevel.add(source);
		
		
		while (currentLevel.size() > 0) {
			
			// Update distance
			totalDistances += level * currentLevel.size();

			// Add next level
			for (int vertex: currentLevel) {
				
				visited[vertex] = true;
				
				for (EdgeWithTimestamp edge: this.condensedGraph.get(vertex)) {
					
					if (edge.getTimestamp() > timestamp) {
						continue;
					}


					int neighbor = edge.getTarget();

					if (!visited[neighbor] && !nextLevel.contains(neighbor) && !currentLevel.contains(neighbor)) {
						nextLevel.add(neighbor);
					}

				}
			
			}
			
			// Begin next iteration
			currentLevel.clear();
			currentLevel.addAll(nextLevel);
			nextLevel.clear();
			
			level++; 
			
		}
		
		// Find connected component
		int sccSize = 0;
		for (int i = 0; i < visited.length; i++) {
			if (visited[i]) {
				sccSize++;
			}
		}
		
		if (totalDistances == 0) {
			return 0;
		}

		return  1.0 * (double)(sccSize - 1) * (double)(sccSize - 1) / (double)totalDistances / (double)(this.numVertices - 1);
	}
	
	public void singleSourceCentralityTest(String path, boolean cgTest, boolean sgTest, int numOfQueries) {
	
		this.constructGraph(path, sgTest);
		
		int[] vertexIDs = new int[numOfQueries];
		
		// Highest 100 degrees test
		this.logger.debug("+Highest({})", numOfQueries);
		for (int i = 0; i < vertexIDs.length; i++) {
			vertexIDs[i] = i;
		}
		this.singleSourceTestHelper(path, vertexIDs, cgTest, sgTest);
		this.logger.debug("-Highest({})", numOfQueries);

		
		// Lowest 100 degrees test
		this.logger.debug("+Lowest({})", numOfQueries);
		for (int i = 0; i < vertexIDs.length; i++) {
			vertexIDs[i] = this.numVertices - 1 - i;
		}
		this.singleSourceTestHelper(path, vertexIDs, cgTest, sgTest);
		this.logger.debug("-Lowest({})", numOfQueries);

		
		// Random 100 degrees test
		this.logger.debug("+Random({})", numOfQueries);
		Random rand = new Random(0);
		for (int i = 0; i < vertexIDs.length; i++) {
			vertexIDs[i] = rand.nextInt(this.numVertices);
		}
		this.singleSourceTestHelper(path, vertexIDs, cgTest, sgTest);
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

	
	private void singleSourceTestHelper(String path, int[] sourceIDs, boolean cgTest, boolean sgTest) {
		
		long start, end;
		// Snapshot-CG
		if (cgTest) {
			start = System.currentTimeMillis();
			for (int i = 0; i < sourceIDs.length; i++) {
				this.getCentralitySnapshotBased(sourceIDs[i], true);
			}
			end = System.currentTimeMillis();
			this.logger.info("Snapshotbased-CG running time: {}.", (end-start)*1.0/1000);
			}
		
		// Snapshot-SG
		if (sgTest) {
			start = System.currentTimeMillis();
			for (int i = 0; i < sourceIDs.length; i++) {
				this.getCentralitySnapshotBased(sourceIDs[i], false);
			}
			end = System.currentTimeMillis();
			this.logger.info("Snapshotbased-SG running time: {}.", (end-start)*1.0/1000);
		}
		
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

	
	public static void main(String[] args) {
		
		TimeEvolvingGraphInsertionOnly graph = new TimeEvolvingGraphInsertionOnly();
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
		String path = "data/Scale21_Edge16.raw.uniform.1000";
		
		graph.constructGraph(path, false, false);
//		graph.realGraphTestHelper(path, 100);
//		graph.averageNumOfLabelsTestHelper(path, 100);
		
		
//		graph.singleSourceCentralityTest(path, false, false, 100);
//		graph.optimizationTest(path, 100);
//		graph.printSnapshotSize(path);
		graph.correctnessTest(path);
		
	}
	

}
