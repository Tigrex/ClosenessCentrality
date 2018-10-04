package closeness.centrality.deletion;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import closeness.centrality.entity.EdgeWithTimeInterval;
import closeness.centrality.entity.Label;
import closeness.centrality.entity.SSSPTree;
import closeness.centrality.entity.TimeInterval;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeEvolvingGraphDecremental {
	
	private int numVertices;
	private int numSnapshots;
	private List<List<EdgeWithTimeInterval>> condensedGraph;
	private List<Map<Integer, Set<Integer>>> deltaGraphIncremental;
	private List<Map<Integer, Set<Integer>>> deltaGraphDecremental;	
	
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
	
	public void constructGraph(String path, boolean buildSnapshotGraph) {

		logger.debug("+constructGraph({}, buildSnapshotGraph = {})", path, buildSnapshotGraph);
		
		Set<Integer> vertices = new HashSet<Integer>();
		Set<Integer> timestamps = new HashSet<Integer>();
		
		Map<Integer, Map<Integer, EdgeWithTimeInterval>> condensed = new HashMap<Integer, Map<Integer, EdgeWithTimeInterval>>() ;
		
		Map<Integer, Map<Integer, Set<Integer>>> snapshotsIncremental = new HashMap<Integer, Map<Integer, Set<Integer>>>();
		Map<Integer, Map<Integer, Set<Integer>>> snapshotsDecremental = new HashMap<Integer, Map<Integer, Set<Integer>>>();
		
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
		    	
		    	// Update deltaGraph
	    		if (snapshotsIncremental.containsKey(startTime)) {
		    		Map<Integer, Set<Integer>> snapshot = snapshotsIncremental.get(startTime);
		    		
		    		if (snapshot.containsKey(source)) {
		    			if (snapshot.get(source).contains(target)) {
		    				logger.error("Duplicate edge found ({}, {}, {})", source, target, startTime);
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
	    			snapshotsIncremental.put(startTime, snapshot);
		    	}
	    		
	    		if (snapshotsDecremental.containsKey(endTime)) {
		    		Map<Integer, Set<Integer>> snapshot = snapshotsDecremental.get(endTime);
		    		
		    		if (snapshot.containsKey(source)) {
		    			if (snapshot.get(source).contains(target)) {
		    				logger.error("Duplicate edge found ({}, {}, {})", source, target, endTime);
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
	    			snapshotsDecremental.put(endTime, snapshot);
		    	}
	    		
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

			int ts1 = snapshotsIncremental.size();
			int ts2 = snapshotsDecremental.size();
			
			Set<Integer> keys = snapshotsDecremental.keySet();
			List<Integer> keyList = new ArrayList<Integer>(keys);
			Collections.sort(keyList);
			System.out.println("Min key for snapshotDecremental: " + keyList.get(0));
			System.out.println("Max key for snapshotDecremental: " + keyList.get(keyList.size() - 1));

			
			logger.debug("Number of snapshots for edge insertions: {}.", ts1);
			logger.debug("Number of snapshots for edge deletions: {}.", ts2);
			
//			if (ts1 != numSnapshots || ts2 != numSnapshots) {
//				System.exit(1);
//			}
			
			// Build array-based snapshot graph
			this.deltaGraphIncremental = new ArrayList<Map<Integer, Set<Integer>>>(numSnapshots);
			this.deltaGraphDecremental = new ArrayList<Map<Integer, Set<Integer>>>(numSnapshots);
			
			for (int i = 0; i < numSnapshots - 1; i++) {
				Map<Integer, Set<Integer>> deltaIncremental = snapshotsIncremental.get(i);
				if (deltaIncremental != null) {
					this.deltaGraphIncremental.add(deltaIncremental);
				} else {
					logger.error("No edge insertions at time {}.", i);
					this.deltaGraphIncremental.add(new HashMap<Integer, Set<Integer>>());
				}
				
				Map<Integer, Set<Integer>> deltaDecremental = snapshotsDecremental.get(i + 1);
				if (deltaDecremental != null) {
					this.deltaGraphDecremental.add(deltaDecremental);
				} else {
					logger.error("No edge deletions at time {}.", i + 1);
					this.deltaGraphDecremental.add(new HashMap<Integer, Set<Integer>>());
				}
				
			}
			
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
	
	
	public double[] getCentralitySnapshotBased(int source) {
		
		double[] centralities = new double[this.numSnapshots];
		Arrays.fill(centralities, 0);
		
		Map<Integer, Set<Integer>> graph = this.deltaGraphIncremental.get(0);
		
		SSSPTree tree = this.buildSSSPTree(source, graph);
		centralities[0] = tree.getCentrality(this.numVertices);
		
		
		for (int i = 1; i < this.numSnapshots; i++) {
			
			if (i < this.numSnapshots - 1) {
				Map<Integer, Set<Integer>> insertedEdges = this.deltaGraphIncremental.get(i);
				
				for (int s: insertedEdges.keySet()) {
					Set<Integer> ts = insertedEdges.get(s);
					for (int t: ts) {
						
						if (graph.containsKey(s)) {
							graph.get(s).add(t);
						} else {
							Set<Integer> setI = new HashSet<Integer>();
							setI.add(t);
							graph.put(s, setI);
						}
					}
					
				}
			}
			
			if (i >= 2) {
				Map<Integer, Set<Integer>> deletedEdges = this.deltaGraphDecremental.get(i - 2);
				
				for (int s: deletedEdges.keySet()) {
					Set<Integer> ts = deletedEdges.get(s);
					for (int t: ts) {
						if (graph.containsKey(s)) {
							graph.get(s).remove(t);
						}
					}
				}
			}

			tree = this.buildSSSPTree(source, graph);
			centralities[i] = tree.getCentrality(this.numVertices);
			
		}
		
		return centralities;
		
	}
	
	
	public double[] getCentralityDynamicIncremental(int source) {
		double[] centralities = new double[this.numSnapshots];
		Arrays.fill(centralities, 0);
		
		
		SSSPTree tree = this.buildSSSPTree(source, this.deltaGraphIncremental.get(0));
		centralities[0] = tree.getCentrality(this.numVertices);
		
		for (int i = 1; i < this.numSnapshots; i++) {
			
			if (i < this.numSnapshots - 1) {
				Map<Integer, Set<Integer>> insertedEdges = this.deltaGraphIncremental.get(i);
				
				for (int s: insertedEdges.keySet()) {
					Set<Integer> ts = insertedEdges.get(s);
					for (int t: ts) {
						
						tree.insertDirectedEdge(s, t);
					}
					
				}
			}
			
			if (i >= 2) {
				Map<Integer, Set<Integer>> deletedEdges = this.deltaGraphDecremental.get(i - 2);
				
				for (int s: deletedEdges.keySet()) {
					Set<Integer> ts = deletedEdges.get(s);
					for (int t: ts) {
						
						tree.deleteDirectedEdge(s, t);
					}
				}
			}
			
			centralities[i] = tree.getCentrality(this.numVertices);
			
		}
		
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
						boolean changed = Label.mergeLabel(oldLabels, new Label(neighbor, level + 1, newInterval));

						if (changed) {
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
//			System.out.println("----");
//			System.out.println("Distance: " + totalDistances[i]);
//			System.out.println("reachables: " + sccSize[i]);
//			System.out.println("----");

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
	
	
	public SSSPTree buildSSSPTree(int source, Map<Integer, Set<Integer>> initialGraph) {
		
		Map<Integer, Integer> nodeLevelMap = new HashMap<Integer, Integer>();
		Map<Integer, Set<Integer>> parentsMap = new HashMap<Integer, Set<Integer>>();
		Map<Integer, Set<Integer>> childrenMap = new HashMap<Integer, Set<Integer>>();
		
		int totalDistances = 0;		
		
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
		
		SSSPTree tree = new SSSPTree(initialGraph, totalDistances, source, nodeLevelMap, parentsMap, childrenMap);
		
		return tree;
		
	}
	
	
	
	public static void main(String[] args) {
		
		TimeEvolvingGraphDecremental graph = new TimeEvolvingGraphDecremental();
		
//		String path = "data/youtube-growth-sorted.txt.teg.sim";
//		String path = "data/youtube-d-growth.txt.teg.sim";
//		String path = "data/out.wikipedia-growth.teg.sim";
//		String path = "data/dblp-2018-01-01.xml.teg.sim";
		String path = "data/Scale17_Edge16.raw.uniform.2000.0.1";
//		String path = "data/sample.txt";
		
		
//		path = "data/dblp-2018-01-01.xml.teg.sim";
		graph.constructGraph(path);
//		double[] centralities1 = graph.getCentralityRangeBased(10);
//		for (double d: centralities1) {
//			System.out.println(d);
//		}
//		
//		double[] centralities2 = graph.getCentralityDynamicIncremental(10);
//		for (double d: centralities2) {
//			System.out.println(d);
//		}
		
		double[] centralities3 = graph.getCentralitySnapshotBased(10);
		for (double d: centralities3) {
			System.out.println(d);
		}
		
	}
	

}
