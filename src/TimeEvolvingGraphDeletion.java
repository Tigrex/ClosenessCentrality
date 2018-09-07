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

public class TimeEvolvingGraphDeletion {
	
	private int numVertices;
	private int numSnapshots;
	
	private List<List<EdgeWithTimeInterval>> condensedGraph;
	
	final private Logger logger = LoggerFactory.getLogger(TimeEvolvingGraphDeletion.class);
	
	public int getNumVertices() {
		return this.numVertices;
	}
	
	public int getNumSnapshots() {
		return this.numSnapshots;
	}
	
	public void constructGraph(String path, boolean buildSnapshotGraph) {

		logger.debug("+constructGraph({}, buildSnapshotGraph = {})", path, buildSnapshotGraph);
		
		Set<Integer> vertices = new HashSet<Integer>();
		Set<Integer> timestamps = new HashSet<Integer>();
		
		Map<Integer, Map<Integer, EdgeWithTimeInterval>> condensed = new HashMap<Integer, Map<Integer, EdgeWithTimeInterval>>() ;
		
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
		    	int source = Integer.valueOf(parts[0]);
		    	int target = Integer.valueOf(parts[1]);
		    	int startTime = Integer.valueOf(parts[2]);
		    	int endTime = Integer.valueOf(parts[3]);
		    	
		    	vertices.add(source);
		    	vertices.add(target);
		    	timestamps.add(startTime);
		    	timestamps.add(endTime);
		    	
		    	// Update condensedGraph
		    	if (condensed.containsKey(source)) {
		    		
		    		Map<Integer, EdgeWithTimeInterval> outgoingEdges = condensed.get(source);
		    		
		    		if (outgoingEdges.containsKey(target)) {
		    			logger.error("Duplicate edges found ({}, {})}.", source, target);
		    			System.exit(1);
		    		} else {
		    			outgoingEdges.put(target, new EdgeWithTimeInterval(target, new TimeInterval(startTime, endTime)));
		    		}
		    		
		    	} else {
		    		Map<Integer, EdgeWithTimeInterval> outgoingEdges = new HashMap<Integer, EdgeWithTimeInterval>();
		    		outgoingEdges.put(target, new EdgeWithTimeInterval(target, new TimeInterval(startTime, endTime)));
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

			// Build array-based condensed graph
			this.condensedGraph = new ArrayList<List<EdgeWithTimeInterval>>(this.numVertices);
			for (int i = 0; i < this.numVertices; i++) {
				if (!condensed.containsKey(i)) {
					this.condensedGraph.add(new ArrayList<EdgeWithTimeInterval>());
					continue;
				}
				
				Map<Integer, EdgeWithTimeInterval> outgoingEdgesMap = condensed.get(i);
				List<EdgeWithTimeInterval> outgoingEdgesList = new ArrayList<EdgeWithTimeInterval>(outgoingEdgesMap.size());
				for (Integer target: outgoingEdgesMap.keySet()) {
					EdgeWithTimeInterval e = outgoingEdgesMap.get(target);
					outgoingEdgesList.add(e);
				}
				
				Collections.sort(outgoingEdgesList);
				this.condensedGraph.add(outgoingEdgesList);
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
		logger.debug("-constructGraph({}, buildSnapshotGraph = {})", path, buildSnapshotGraph);

	}
	
	public double[] getCentralitySnapshotBased(int source, boolean useCondensedGraph) {

//		long start = System.currentTimeMillis();
//		this.logger.info("+getCentralitySnapshotBased({})", source);
		
		double[] centralities = new double[this.numSnapshots];
		
		for (int i = 0; i < this.numSnapshots; i++) {
			if (useCondensedGraph) {
				centralities[i] = this.getSnapshotCentralityWithCG(source, i);
			} else {
				centralities[i] = this.getSnapshotCentralityWithCG(source, i);
			}
			
		}
		

//		long end = System.currentTimeMillis();
//		this.logger.info("Calculating centrality snapshot based time: {} seconds.", (end-start)*1.0/1000);
//		this.logger.info("-getCentralitySnapshotBased({})", source);
		
		return centralities;
		
	}
	
	
	public double[] getCentralityRangeBased(int source) {
		
		long start = System.currentTimeMillis();
		this.logger.info("+getCentralityRangeBased({})", source);
		
		int[] sccSize = new int[this.numSnapshots]; // Calculate scc size
		
		// Values default to be 0
		int[] totalDistances = new int[this.numSnapshots];
		
		double[] centralities = new double[this.numSnapshots];
		Arrays.fill(centralities, 0);
		
		List<List<Label>> labels = new ArrayList<List<Label>>(this.numVertices);
		for (int i = 0; i < this.numVertices; i++) {
			labels.add(new ArrayList<Label>());
		}

		int level = 0;
		
		List<Label> current = new ArrayList<Label>();
		List<Label> next = new ArrayList<Label>();

		Label sourceLabel = new Label(source, level, new TimeInterval(0, this.numSnapshots - 1));
		current.add(sourceLabel);
		labels.get(source).add(sourceLabel);

		while (current.size() > 0) {
			
			for (Label label: current) {
				int vertex = label.getVertex();
				
				//Add next level
				for (EdgeWithTimeInterval edge: this.condensedGraph.get(vertex)) {
					
					int neighbor = edge.getTarget();
					
					TimeInterval newInterval = label.getTimeInterval().intersect(edge.getTimeInterval());
					
					if (newInterval != null) {
						
						List<Label> oldLabels = labels.get(neighbor);
						List<Label> newLabels = Label.insertLabel(oldLabels, new Label(neighbor, level + 1, newInterval));
						
						if (Label.equalityCheck(oldLabels, newLabels) != true) {
							labels.get(neighbor).clear();
							labels.get(neighbor).addAll(newLabels);
							
							next.add(new Label(neighbor, level + 1, newInterval));
						}
						
					}
					
					
				}
				
			}

			current.clear();
			current.addAll(next);
			next.clear();
			level++;
			
		}
		
		for (int i = 0; i < this.numVertices; i++) {
			List<Label> list = labels.get(i);
			
			for (Label label: list) {
				int distance = label.getDistance();
				int startTime = label.getStartTime();
				int endTime = label.getEndTime();
				
				for (int snapshot = startTime; snapshot <= endTime; snapshot++) {
					totalDistances[snapshot] += distance;
					sccSize[snapshot]++;
				}
			}
			
		}
		
//		for (int i = 0; i < 10; i++) {
//			List<Label> list = labels.get(i);
//			String result = "";
//			for (Label label: list) {
//				int distance = label.getDistance();
//				int startTime = label.getStartTime();
//				int endTime = label.getEndTime();
//
//				result += "[" + startTime + "," + endTime + "]:" + distance + " ";
//			}
//			System.out.println(result);
//		}
		
		
		for (int i = 0; i < this.numSnapshots; i++) {
			if (totalDistances[i] == 0) {
				centralities[i] = 0;
			} else {
				centralities[i] = 1.0 * (double)(sccSize[i] - 1) * (double)(sccSize[i] - 1) / (double)totalDistances[i] / (double)(this.numVertices - 1);
			}
			
		}

		
		long end = System.currentTimeMillis();
		this.logger.info("Calculating centrality range based time: {} seconds.", (end-start)*1.0/1000);
		this.logger.info("-getCentralityRangeBased({})", source);
		
		return centralities;
				
	}
	
	
	public void randomDegreeTest(int numOfRuns, String suffix) {
		
		for (int i = 17; i <= 21; i++) {
			String path = "data/Scale" + i + "_Edge16.raw.uniform.2000" + suffix;
			this.randomDegreeTestHelper(path, numOfRuns);
		}
		
		int[] timestamps = {100, 200, 500, 1000, 4000, 8000};
		
		for (int i = 0; i < timestamps.length; i++) {
			String path = "data/Scale21_Edge16.raw.uniform." + timestamps[i] + suffix;
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
			long[] runningTimes = this.syntheticDataTestHelper(degree);
			this.logger.info("{}:{},{}", degree, runningTimes[0], runningTimes[1]);
			
			totalRunningTimes[0] += runningTimes[0];
			totalRunningTimes[1] += runningTimes[1];
			
		}
				 
		this.logger.info("Total running time {},{}", totalRunningTimes[0], totalRunningTimes[1]);
		this.logger.info("-randomDegreeTestHelper({})", path);

		return totalRunningTimes;
		
	}


	
	public void getAverageNumOfLabelsTest(int numOfRuns, String suffix) {
		
		for (int i = 17; i <= 21; i++) {
			String path = "data/Scale" + i + "_Edge16.raw.uniform.2000" + suffix;
			double avgDegree = this.getAverageNumOfLabelsHelper(path, numOfRuns);
			logger.info("{}: " + avgDegree);
		}
		
		int[] timestamps = {100, 200, 500, 1000, 4000, 8000};
		
		for (int i = 0; i < timestamps.length; i++) {
			String path = "data/Scale21_Edge16.raw.uniform." + timestamps[i] + suffix;
			double avgDegree = this.getAverageNumOfLabelsHelper(path, numOfRuns);
			logger.info("{}: " + avgDegree);
		}
		
	}
	
	
	
	public void maxDegreeTest(String suffix) {
		
		for (int i = 17; i <= 21; i++) {
			String path = "data/Scale" + i + "_Edge16.raw.uniform.2000" + suffix;
			this.maxDegreeTestHelper(path);
		}
		
		int[] timestamps = {100, 200, 500, 1000, 4000, 8000};
		
		for (int i = 0; i < timestamps.length; i++) {
			String path = "data/Scale21_Edge16.raw.uniform." + timestamps[i] + suffix;
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

		long[] runningTimes = this.syntheticDataTestHelper(maxDegreeVertex);
		
		this.logger.info("{},{}", runningTimes[0], runningTimes[1]);

		this.logger.info("-maxDegreeTest({})", path);

		return runningTimes;
		
	}
	
	private double getAverageNumOfLabelsHelper(String path, int numOfRuns) {
		
		this.logger.info("+getAverageNumOfLabelsHelper({},{})", path, numOfRuns);

		double avgLabels = 0;
		
		this.constructGraph(path, false);

		// Get random degrees
		Random random = new Random(0);
		for (int i = 0; i < numOfRuns; i++) {
			int degree = random.nextInt(this.numVertices);
			
			double avg = this.dataLabelTestHelper(degree);
			
			avgLabels += avg;
		}
				
		avgLabels = avgLabels / numOfRuns;
		
		this.logger.info("-getAverageNumOfLabelsHelper({},{})", path, numOfRuns);

		return avgLabels;
		
	}
	
	private long[] syntheticDataTestHelper(int sourceVertex) {
		
		long start = System.currentTimeMillis();
		double[] centrality1 = this.getCentralitySnapshotBased(sourceVertex, true);
		long end = System.currentTimeMillis();
		long time1 = end - start;
		
		start = System.currentTimeMillis();
		double[] centrality2 = this.getCentralityRangeBasedFaster(sourceVertex);
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
	
	
	public double dataLabelTestHelper(int sourceVertex) {
		
		List<List<DistanceLabel>> allLabels = new ArrayList<List<DistanceLabel>>(this.numVertices);
		for (int i = 0; i < this.numVertices; i++) {
			allLabels.add(new ArrayList<DistanceLabel>());
		}

		List<VertexWithLabel> current = new ArrayList<VertexWithLabel>();
		List<VertexWithLabel> next = new ArrayList<VertexWithLabel>();

		DistanceLabel sl = new DistanceLabel(0, this.numSnapshots - 1, 0);
		VertexWithLabel sourceLabel = new VertexWithLabel(sourceVertex, sl);
		current.add(sourceLabel);
		
		allLabels.get(sourceVertex).add(sl);

		while (current.size() > 0) {
			
			for (VertexWithLabel label: current) {
				int vertex = label.getVertex();
				List<DistanceLabel> currentLabels = label.getLabels();
				
				//Add next level
				for (EdgeWithTimeInterval edge: this.condensedGraph.get(vertex)) {
					
					int neighbor = edge.getTarget();
					
					int startTime = edge.getTimeInterval().getStartTime();
					int endTime = edge.getTimeInterval().getEndTime();
					List<DistanceLabel> nextLabels = DistanceLabel.goThroughEdge(currentLabels, startTime, endTime);
					
					if (nextLabels.size() > 0) {
						
						DistanceLabel.simplify(nextLabels);

						List<DistanceLabel> nextExisting = allLabels.get(neighbor);
						List<DistanceLabel> updated = DistanceLabel.mergeLabelsAndGetUpdated(nextExisting, nextLabels);
						DistanceLabel.simplify(nextExisting);

						
						if (updated != null && updated.size() > 0) {
							
							DistanceLabel.simplify(updated);
							
							next.add(new VertexWithLabel(neighbor, updated));
						}
						
					}
					
				}
				
			}

			current.clear();
			current.addAll(next);
			next.clear();
			
		}
		
		double totalLabels = 0;
		for (int i = 0; i < this.numVertices; i++) {
			List<DistanceLabel> list = allLabels.get(i);
			totalLabels += list.size();
		}
		
		double avgLabels = totalLabels / this.numVertices;
		
		return avgLabels;
	}
	
	
	
	public double[] getCentralityRangeBasedFaster(int source) {
		
//		long start = System.currentTimeMillis();
//		this.logger.info("+getCentralityRangeBasedFaster({})", source);
		
		int[] sccSize = new int[this.numSnapshots]; // Calculate scc size
		
		// Values default to be 0
		int[] totalDistances = new int[this.numSnapshots];
		
		double[] centralities = new double[this.numSnapshots];
		Arrays.fill(centralities, 0);
		
		List<List<DistanceLabel>> allLabels = new ArrayList<List<DistanceLabel>>(this.numVertices);
		for (int i = 0; i < this.numVertices; i++) {
			allLabels.add(new ArrayList<DistanceLabel>());
		}

		List<VertexWithLabel> current = new ArrayList<VertexWithLabel>();
		List<VertexWithLabel> next = new ArrayList<VertexWithLabel>();

		DistanceLabel sl = new DistanceLabel(0, this.numSnapshots - 1, 0);
		VertexWithLabel sourceLabel = new VertexWithLabel(source, sl);
		current.add(sourceLabel);
		
		allLabels.get(source).add(sl);

		while (current.size() > 0) {
			
			for (VertexWithLabel label: current) {
				int vertex = label.getVertex();
				List<DistanceLabel> currentLabels = label.getLabels();
				
				//Add next level
				for (EdgeWithTimeInterval edge: this.condensedGraph.get(vertex)) {
					
					int neighbor = edge.getTarget();
					
					int startTime = edge.getTimeInterval().getStartTime();
					int endTime = edge.getTimeInterval().getEndTime();
//					System.out.println("Before go through edge: " + DistanceLabel.listToString(currentLabels));
//					System.out.println("Edge: " + startTime + "," + endTime);
					List<DistanceLabel> nextLabels = DistanceLabel.goThroughEdge(currentLabels, startTime, endTime);
//					System.out.println("After go through edge: " + DistanceLabel.listToString(nextLabels));
					
					if (nextLabels.size() > 0) {
						
						DistanceLabel.simplify(nextLabels);

						List<DistanceLabel> nextExisting = allLabels.get(neighbor);
//						System.out.println("Existing: " + DistanceLabel.listToString(nextExisting));
//						System.out.println("New: " + DistanceLabel.listToString(nextLabels));
						List<DistanceLabel> updated = DistanceLabel.mergeLabelsAndGetUpdated(nextExisting, nextLabels);
						DistanceLabel.simplify(nextExisting);

//						System.out.println("Merged: " + DistanceLabel.listToString(nextExisting));
//						System.out.println("Updated: " + DistanceLabel.listToString(updated));
						
						
						if (updated != null && updated.size() > 0) {
							
							DistanceLabel.simplify(updated);
							
							next.add(new VertexWithLabel(neighbor, updated));
						}
						
					}
					
				}
				
			}

			current.clear();
			current.addAll(next);
			next.clear();
			
		}
		
		for (int i = 0; i < this.numVertices; i++) {
			List<DistanceLabel> list = allLabels.get(i);
			
			for (DistanceLabel label: list) {
				int distance = label.getDistance();
				int startTime = label.getStartTime();
				int endTime = label.getEndTime();
				
				for (int snapshot = startTime; snapshot <= endTime; snapshot++) {
					totalDistances[snapshot] += distance;
					sccSize[snapshot]++;
				}
			}
			
		}
		
//		for (int i = 0; i < 10; i++) {
//			List<DistanceLabel> list = allLabels.get(i);
//			String result = "";
//			for (DistanceLabel label: list) {
//				int distance = label.getDistance();
//				int startTime = label.getStartTime();
//				int endTime = label.getEndTime();
//
//				result += "[" + startTime + "," + endTime + "]:" + distance + " ";
//			}
//			System.out.println(result);
//		}
		
		for (int i = 0; i < this.numSnapshots; i++) {
			if (totalDistances[i] == 0) {
				centralities[i] = 0;
			} else {
				centralities[i] = 1.0 * (double)(sccSize[i] - 1) * (double)(sccSize[i] - 1) / (double)totalDistances[i] / (double)(this.numVertices - 1);
			}
			
		}

		
//		long end = System.currentTimeMillis();
//		this.logger.info("Calculating centrality range based faster time: {} seconds.", (end-start)*1.0/1000);
//		this.logger.info("-getCentralityRangeBasedFaster({})", source);
		
		return centralities;
				
	}

//	public double[] getCentralityRangeBufferUpdate(int source) {
//		
//		long start = System.currentTimeMillis();
//		this.logger.info("+getCentralityRangeBufferUpdate({})", source);
//		
//		int[] sccSize = new int[this.numSnapshots]; // Calculate scc size
//		
//		double[] centralities = new double[this.numSnapshots];
//		Arrays.fill(centralities, 0);
//		
//		// Values default to be 0
//		int[] totalDistances = new int[this.numSnapshots];
//		
//		int[] discoveredTime = new int[this.numVertices];
//		Arrays.fill(discoveredTime, Integer.MAX_VALUE);
//		
//		int level = 0;
//		
//		int[] currentLevel = new int[this.numVertices];
//		Arrays.fill(currentLevel, Integer.MAX_VALUE);
//		int[] nextLevel = new int[this.numVertices];
//		Arrays.fill(nextLevel, Integer.MAX_VALUE);
//
//		currentLevel[source] = 0;
//		
//		int[] verticesPerTimestamp = new int[this.numSnapshots];
//		int[] startingPoints = new int[this.numSnapshots];
//		int[] endingPoints = new int[this.numSnapshots];
//		
//		boolean hasNext = true;
//				
//		while (hasNext) {
//			
//			Arrays.fill(verticesPerTimestamp, 0);
//			Arrays.fill(startingPoints, 0);
//			Arrays.fill(endingPoints, 0);
//			
//			for (int vertex = 0; vertex < this.numVertices; vertex++) {
//				
//				if (currentLevel[vertex] != Integer.MAX_VALUE) {
//					
//					// Newly discovered vertex, update distance at the end
//					if (discoveredTime[vertex] == Integer.MAX_VALUE) {
//						verticesPerTimestamp[currentLevel[vertex]]++;
//					}
//					// Existing vertex, update distance separately
//					else {
//						
//						startingPoints[currentLevel[vertex]]++;
//						endingPoints[discoveredTime[vertex]]++;
//						
//					}
//					
//					// Update discovered time
//					discoveredTime[vertex] = currentLevel[vertex];
//					
//					//Add next level
//					for (EdgeWithTimestamp edge: this.condensedGraph.get(vertex)) {
//						
//						int neighbor = edge.getTarget();
//						int neighborDiscoverTime = Math.max(edge.getTimestamp(), currentLevel[vertex]);
//						
//						if (neighborDiscoverTime < discoveredTime[neighbor] && neighborDiscoverTime < currentLevel[neighbor]) {
//							
//							if (neighborDiscoverTime < nextLevel[neighbor]) {
//								nextLevel[neighbor] = neighborDiscoverTime;
//							}
//							
//						}
//						
//						
//					}
//					
//				}
//				
//			}
//			
//			
//			// Update distance
//			int localSummary = verticesPerTimestamp[0];
//			int positiveCount = startingPoints[0] - endingPoints[0];
//			
//			totalDistances[0] += level * (localSummary + positiveCount);
//			sccSize[0] += (localSummary + positiveCount);
//				
//			for (int i = 1; i < this.numSnapshots; i++) {
//				localSummary += verticesPerTimestamp[i];
//				positiveCount += startingPoints[i] - endingPoints[i];
//
//				totalDistances[i] += level * (localSummary + positiveCount);
//				sccSize[i] += (localSummary + positiveCount);
//			}
//			
//			
//			// Begin next iteration
//			hasNext = false;
//			for (int i = 0; i < this.numVertices; i++) {
//				currentLevel[i] = nextLevel[i];
//				if (nextLevel[i] != Integer.MAX_VALUE) {
//					hasNext = true;
//					nextLevel[i] = Integer.MAX_VALUE;
//				}
//				
//			}
//			
//			level++;
//			
//		}
//		
//		
//		for (int i = 0; i < this.numSnapshots; i++) {
//			if (totalDistances[i] == 0) {
//				centralities[i] = 0;
//			} else {
//				centralities[i] = 1.0 * (double)(sccSize[i] - 1) * (double)(sccSize[i] - 1) / (double)totalDistances[i] / (double)(this.numVertices - 1);
//			}
//			
//		}
//		
//
//		
//		long end = System.currentTimeMillis();
//		this.logger.info("Calculating centrality range based buffer update time: {} seconds.", (end-start)*1.0/1000);
//		this.logger.info("-getCentralityRangeBufferUpdate({})", source);
//		
//		return centralities;
//				
//	}

	
	
	
	
	public boolean correctnessTest(String path) {

		this.logger.info("+correctnessTest({})", path);
		
		this.constructGraph(path, true);
		
		for (int i = 0; i < 10; i++) {
			double[] centralities2 = this.getCentralityRangeBasedFaster(i + this.numVertices / 2);
			double[] centralities3 = this.getCentralityRangeBased(i + this.numVertices / 2);
			double[] centralities1 = this.getCentralitySnapshotBased(i + this.numVertices / 2, true);
			
			if (compareDoubleArray(centralities1, centralities2) == false) {
				this.logger.error("Run {}:Centralities1 and centralities2 are not equal.", i);
				this.logger.info("-correctnessTest({})", path);
				return false;
			}
			
			if (compareDoubleArray(centralities1, centralities3) == false) {
				this.logger.error("Run {}:Centralities1 and centralities3 are not equal.", i);
				this.logger.info("-correctnessTest({})", path);
				return false;
			}
		}
		
		this.logger.info("Successfully passed all tests (SnapshotCG, Range).");
		this.logger.info("-correctnessTest({})", path);
		return true;
		
	}
	
	private boolean compareDoubleArray(double[] da1, double[] da2) {
		if (da1.length != da2.length) {
			return false;
		}
		for (int i = 0; i < da1.length; i++) {
			if (compareDouble(da1[i], da2[i]) == false) {
				logger.debug("Index {} not equal: {}, {}", i, da1[i], da2[i]);
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
				
				for (EdgeWithTimeInterval edge: this.condensedGraph.get(vertex)) {
					
					TimeInterval timeInterval = edge.getTimeInterval();
					if (timeInterval.getEndTime() < timestamp || timeInterval.getStartTime() > timestamp) {
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
		
//		// Snapshot-SG
//		if (sgTest) {
//			start = System.currentTimeMillis();
//			for (int i = 0; i < sourceIDs.length; i++) {
//				this.getCentralitySnapshotBased(sourceIDs[i], false);
//			}
//			end = System.currentTimeMillis();
//			this.logger.info("Snapshotbased-SG running time: {}.", (end-start)*1.0/1000);
//		}
		
		// Range-based
		start = System.currentTimeMillis();
		for (int i = 0; i < sourceIDs.length; i++) {
			this.getCentralityRangeBased(sourceIDs[i]);
		}
		end = System.currentTimeMillis();
		this.logger.info("Range-based running time: {}.", (end-start)*1.0/1000);
		
//		// Range-based-modified
//		start = System.currentTimeMillis();
//		for (int i = 0; i < sourceIDs.length; i++) {
//			this.getCentralityRangeBufferUpdate(sourceIDs[i]);
//		}
//		end = System.currentTimeMillis();
//		this.logger.info("Range-based modified running time: {}.", (end-start)*1.0/1000);
		
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
//		this.logger.info("+Range-based modified tests.");
//		for (int i = 0; i < sourceIDs.length; i++) {
//			start = System.currentTimeMillis();
//			this.getCentralityRangeBufferUpdate(sourceIDs[i]);
//			end = System.currentTimeMillis();
//			System.out.println(end-start);
//		}
//		this.logger.info("-Range-based modified tests.");


		
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
		
		TimeEvolvingGraphDeletion graph = new TimeEvolvingGraphDeletion();
		
//		graph.maxDegreeTest(".0.15");
		
		graph.getAverageNumOfLabelsTest(100, ".0.15");

//		graph.randomDegreeTest(100, ".0.15");

		
//		String path = "data/Scale17_Edge16.raw.uniform.2000.0.05";
//		graph.correctnessTest(path);
		
//		String path = "data/IMDB-Movie-Data.csv.teg.sim.deletions";
//		String path = "data/Scale17_Edge16.raw.uniform.2000.0.05";
		
//		graph.singleSourceCentralityTest(path, true, false, 1);
		
//		String path = "data/dblp-2018-01-01.xml.teg.sim.deletions";
//		String path = "data/IMDB-Movie-Data.csv.teg.sim.deletions";
//		String path = "data/wikipedia-growth.txt.teg.sim.deletions";
//		String path = "data/youtube-d-growth.txt.teg.sim.deletions";
		
//		graph.singleSourceCentralityTest(path, false, false, 100);
//		graph.optimizationTest(path, 100);
//		graph.printSnapshotSize(path);
//		graph.correctnessTest(path);
		
//		graph.constructGraph(path, false);
//		graph.getCentralityRangeBased(0);
//		graph.getCentralitySnapshotBased(0, true);
		
//		graph.correctnessTest(path);
		
//		graph.singleSourceCentralityTest(path, true, false, 10);
		
	}
	

}
