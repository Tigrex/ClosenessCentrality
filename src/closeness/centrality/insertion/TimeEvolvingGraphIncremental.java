package closeness.centrality.insertion;
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
import java.util.Set;

import closeness.centrality.entity.EdgeWithTimestamp;
import closeness.centrality.entity.SSSPTree;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TimeEvolvingGraphIncremental {
	
	private int numVertices;
	private int numSnapshots;
	private List<List<EdgeWithTimestamp>> condensedGraph;
	private List<Map<Integer, Set<Integer>>> deltaGraph;
	
	final private Logger logger = LoggerFactory.getLogger(TimeEvolvingGraphIncremental.class);
	
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
	
	
	public double[] getCentralityRangeBased_v1(int source) {
		
		int[] sccSize = new int[this.numSnapshots];
		
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
							sccSize[time]++;
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

		return centralities;
				
	}
	
	public double[] getCentralityRangeBased_v2(int source) {
		
		int[] sccSize = new int[this.numSnapshots];
		
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
		
		return centralities;
				
	}

	public double[] getCentralityRangeBased_v3(int source) {
		
		int[] sccSize = new int[this.numSnapshots];
		
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
				
				int t = currentLevel[vertex];
				
				if (t != Integer.MAX_VALUE) {

					verticesPerTimestamp[t]++;
					
					if (discoveredTime[vertex] != Integer.MAX_VALUE) {
						verticesPerTimestamp[discoveredTime[vertex]]--;
					}
					
					// Update discovered time
					discoveredTime[vertex] = t;
					
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
			int localSummary = 0;
			
			for (int i = 0; i < this.numSnapshots; i++) {
				localSummary += verticesPerTimestamp[i];
				sccSize[i] += localSummary;
				totalDistances[i] += level * localSummary;
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
		
		return centralities;
				
	}
	
	public double getAverageNumberOfLabels(int source) {
		
		int totalLabels = 0;

		int[] sccSize = new int[this.numSnapshots];
		
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
		
		logger.info("Total number of labels: {}", totalLabels);
		logger.info("Total number of reachable vertices in the last time stamp: {}", sccSize[this.numSnapshots - 1]);
		
		double numOfLabels = 1.0 * totalLabels / sccSize[this.numSnapshots - 1];
		logger.info("Average number of labels: {}", numOfLabels);
		
		return numOfLabels;
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
		centralities[0] = initialCentrality;
		
		
		for (int t = 1; t < this.numSnapshots; t++) {
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
					
					if (affectedVertices.containsKey(to)) {
						int previousValue = affectedVertices.get(to);
						if (levelFrom + 1 < previousValue) {
							affectedVertices.put(to, levelFrom + 1);
						}
					} else {
						affectedVertices.put(to, levelFrom + 1);
					}
					
				}	
			}

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
			
			// Start from the lowest level among affected vertices
			int lvl = levels.remove(0);
			currentLevel.clear();
			nextLevel.clear();
			currentLevel.addAll(levelToVerticesMap.get(lvl));
			
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
			
			double newCentrality = 1.0 * (double)(numReachableVertices - 1) * (double)(numReachableVertices - 1) / (double)totalDistances / (double)(this.numVertices - 1);
			centralities[t] = newCentrality;

		}
		
		return centralities;
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
	
}
