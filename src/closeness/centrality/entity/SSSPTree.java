package closeness.centrality.entity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class SSSPTree {
	
	public int totalDistance;
	
	public int totalReachableVertices;
	
	public Map<Integer, Integer> nodeLevelMap;
	
	public int source;
	
	public Map<Integer, Set<Integer>> parentsMap;
	
	public Map<Integer, Set<Integer>> childrenMap;
	
	public Map<Integer, Set<Integer>> graph;

	public Map<Integer, Set<Integer>> reverseGraph;
	
	
	public SSSPTree(Map<Integer, Set<Integer>> graph, int totalDistance, int source, Map<Integer, Integer> nodeLevelMap, Map<Integer, Set<Integer>> parentsMap, Map<Integer, Set<Integer>> childrenMap) {
		this.graph = graph;
		this.buildReverseGraph();
		this.totalDistance = totalDistance;
		this.totalReachableVertices = nodeLevelMap.size();
		this.source = source;
		
		this.nodeLevelMap = nodeLevelMap;
		this.parentsMap = parentsMap;
		this.childrenMap = childrenMap;
	}

	public void buildReverseGraph() {
		
		this.reverseGraph = new HashMap<Integer, Set<Integer>>();
		
		for (int source: this.graph.keySet()) {
			Set<Integer> targets = this.graph.get(source);
			
			for (int target: targets) {
				if (this.reverseGraph.containsKey(target)) {
					this.reverseGraph.get(target).add(source);
				} else {
					Set<Integer> sources = new HashSet<Integer>();
					sources.add(source);
					this.reverseGraph.put(target, sources);
				}
			}
			
		}
		
	}
	
	public void deleteDirectedEdge(int from, int to) {
		// Update graph first
		if (!this.graph.containsKey(from)) {
			throw new RuntimeException("Edge " + from + "-" + to + " does not exist.");
		}
		this.graph.get(from).remove(to);
		this.reverseGraph.get(to).remove(from);
		
		if (!this.childrenMap.containsKey(from)) {
			return;
		}
		
		Set<Integer> children = this.childrenMap.get(from);
		if (!children.contains(to)) {
			return;
		}
		
		if (this.parentsMap.get(to).size() > 1 && this.parentsMap.get(to).contains(from)) {
			this.parentsMap.get(to).remove(from);
			this.childrenMap.get(from).remove(to);
			
			return;
		}
		
		Set<Integer> affectedVertices = new HashSet<Integer>();
		List<Integer> currentLevel = new ArrayList<Integer>();
		List<Integer> nextLevel = new ArrayList<Integer>();
		if (this.parentsMap.get(to).size() == 1 && this.parentsMap.get(to).contains(from)) {
			// the only shortest path link
			this.parentsMap.get(to).remove(from);
			this.childrenMap.get(from).remove(to);
			currentLevel.add(to);

			this.totalDistance -= this.nodeLevelMap.get(to);
			this.nodeLevelMap.remove(to);
		}
		
		while (currentLevel.size() > 0) {
			affectedVertices.addAll(currentLevel);
			
			for (int v: currentLevel) {
				
				if (!this.childrenMap.containsKey(v)) {
					continue;
				}
				
				List<Integer> toBeRemoved = new ArrayList<Integer>();
				
				Set<Integer> childs = this.childrenMap.get(v);
				for (Integer child: childs) {
					if (this.parentsMap.get(child).size() == 1) {
						nextLevel.add(child);
						toBeRemoved.add(child);
						
						this.totalDistance -= this.nodeLevelMap.get(child);
						this.nodeLevelMap.remove(child);
					}
				}
				
				for (Integer i: toBeRemoved) {
					this.parentsMap.get(i).remove(v);
					this.childrenMap.get(v).remove(i);
				}
				
				
				
			}
			
			currentLevel.clear();
			currentLevel.addAll(nextLevel);
			nextLevel.clear();
		}
		
		if (affectedVertices.size() > 0) {

			PriorityQueue<PQItem> pq = new PriorityQueue<PQItem>();
			for (int v: affectedVertices) {
				
				int distance = Integer.MAX_VALUE;
				List<Integer> parents = new ArrayList<Integer>();
				
				Set<Integer> sources = this.reverseGraph.get(v);
				if (sources != null) {
					for (int source: sources) {
						if (!this.nodeLevelMap.containsKey(source)) {
							continue;
						}
						
						if (this.nodeLevelMap.get(source) + 1 < distance) {
							distance = this.nodeLevelMap.get(source) + 1;
							parents.clear();
							parents.add(source);
						} else if (this.nodeLevelMap.get(source) + 1 == distance) {
							parents.add(source);
						}
						
					}
					
					PQItem item = new PQItem(v, distance, parents);
					pq.add(item);
					
				}
				
				
					
			}
			
			while (pq.size() > 0) {
				PQItem first = pq.poll();
				int v = first.getVertexId();
				int distance = first.getDistance();
				List<Integer> parents = first.parents;
				
				if (distance == Integer.MAX_VALUE) {
					break;
				}
				if (!this.nodeLevelMap.containsKey(v)) {
					this.nodeLevelMap.put(v, distance);
					
					for (int parent: parents) {
						this.updateParentChildrenMap(parent, v);
					}
					
					this.totalDistance += distance;
					
				} else if (distance == this.nodeLevelMap.get(v)) {
					for (int parent: parents) {
						this.updateParentChildrenMap(parent, v);
					}
				} else if (distance < this.nodeLevelMap.get(v)){
					
					this.totalDistance -= this.nodeLevelMap.get(v);
					this.totalDistance += distance;

					this.nodeLevelMap.put(v, distance);
					
					// Break existing parents and move to new parent
					if (this.parentsMap.containsKey(v)) {
						for (int p: this.parentsMap.get(v)) {
							this.childrenMap.get(p).remove(v);
						}
					}
					this.parentsMap.get(v).clear();
					
					for (int parent: parents) {
						this.updateParentChildrenMap(parent, v);
					}
					
					
				} else {
					continue;
				}
				
				affectedVertices.remove(v);
				
				if (!this.graph.containsKey(v)) {
					continue;
				}
				
				Set<Integer> childs = this.graph.get(v);
				for (int child: childs) {
					if (affectedVertices.contains(child)) {
						
						if (!this.nodeLevelMap.containsKey(child) || this.nodeLevelMap.get(child) > distance + 1) {
						
							List<Integer> ps = new ArrayList<Integer>();
							ps.add(v);
							PQItem item = new PQItem(child, distance + 1, ps);
							pq.add(item);
						}
					}
				}
				
			}
			
		}
		
		
	}
	
	
	
	public void insertDirectedEdge(int from, int to) {
		// Update graph first
		if (!this.graph.containsKey(from)) {
			Set<Integer> vs = new HashSet<Integer>();
			this.graph.put(from, vs);
		}
		this.graph.get(from).add(to);

		if (!this.reverseGraph.containsKey(to)) {
			Set<Integer> vs = new HashSet<Integer>();
			this.reverseGraph.put(to, vs);
		}
		this.reverseGraph.get(to).add(from);
		
		
		if (!this.nodeLevelMap.containsKey(from)) {
			return;
		}
		int levelFrom = this.nodeLevelMap.get(from);
		
		if (this.nodeLevelMap.containsKey(to)) {
			int levelTo = this.nodeLevelMap.get(to);
			if (levelTo < levelFrom + 1) {
				return;
			}
			
			if (levelTo == levelFrom + 1) {
				this.updateParentChildrenMap(from, to);
				return;
			}
			
			this.totalDistance -= levelTo;
			this.totalDistance += levelFrom + 1;
			this.nodeLevelMap.put(to, levelFrom + 1);
			
			// Break existing parents and move to new parent
			if (this.parentsMap.containsKey(to)) {
				for (int p: this.parentsMap.get(to)) {
					this.childrenMap.get(p).remove(to);
				}
			}
			this.parentsMap.get(to).clear();
			
			this.updateParentChildrenMap(from, to);
			
		} else {
			this.nodeLevelMap.put(to, levelFrom + 1);
			this.totalDistance += levelFrom + 1;
			
			this.updateParentChildrenMap(from, to);

		}
		
		int level = levelFrom + 1;
		List<Integer> currentLevel = new ArrayList<Integer>();
		currentLevel.add(to);
		List<Integer> nextLevel = new ArrayList<Integer>();
		
		while (currentLevel.size() > 0) {
			level++;
			
			for (int vertex: currentLevel) {
				if (!this.graph.containsKey(vertex)) {
					continue;
				}
				
				Set<Integer> children = this.graph.get(vertex);
				
				for (int child: children) {
					if (!this.nodeLevelMap.containsKey(child)) {
						// New connected vertex
						this.nodeLevelMap.put(child, level);
						this.totalDistance += level;

						this.updateParentChildrenMap(vertex, child);
						nextLevel.add(child);
						continue;
					}
					
					if (this.nodeLevelMap.get(child) > level) {
						// Shorter distance
						this.totalDistance -= this.nodeLevelMap.get(child);
						this.totalDistance += level;

						this.nodeLevelMap.put(child, level);
						
						if (this.parentsMap.containsKey(child)) {
							for (int p: this.parentsMap.get(child)) {
								this.childrenMap.get(p).remove(child);
							}
						}
						this.parentsMap.get(child).clear();
						
						
						this.updateParentChildrenMap(vertex, child);
						nextLevel.add(child);
					}
					
				}
				
				
			}
			
			currentLevel.clear();
			currentLevel.addAll(nextLevel);
			nextLevel.clear();
			
		}
		
		
		
	}
	
	
	private void updateParentChildrenMap(int parent, int child) {
		if (this.parentsMap.containsKey(child)) {
			this.parentsMap.get(child).add(parent);
		} else {
			Set<Integer> parents = new HashSet<Integer>();
			parents.add(parent);
			this.parentsMap.put(child, parents);
		}
		
		if (this.childrenMap.containsKey(parent)) {
			this.childrenMap.get(parent).add(child);
		} else {
			Set<Integer> children = new HashSet<Integer>();
			children.add(child);
			this.childrenMap.put(parent, children);
		}
		
	}
	
	
	public double getCentrality(int totalNumOfVertices) {
		System.out.println("---");
		System.out.println("distance:" + totalDistance);
		System.out.println("rechables:" + this.nodeLevelMap.size());
		System.out.println("---");
		
		this.totalReachableVertices = this.nodeLevelMap.size();
		
		if (totalDistance == 0) {
			return 0.0;
		} else {
			return 1.0 * (double)(totalReachableVertices - 1) * (double)(totalReachableVertices - 1) / (double)totalDistance / (double)(totalNumOfVertices - 1);
		}
	}

	public Map<Integer, Integer> getNodeLevelMap() {
		return nodeLevelMap;
	}

	public void setNodeLevelMap(Map<Integer, Integer> nodeLevelMap) {
		this.nodeLevelMap = nodeLevelMap;
	}

	public Map<Integer, Set<Integer>> getParentsMap() {
		return parentsMap;
	}

	public void setParentsMap(Map<Integer, Set<Integer>> parentsMap) {
		this.parentsMap = parentsMap;
	}

	public Map<Integer, Set<Integer>> getChildrenMap() {
		return childrenMap;
	}

	public void setChildrenMap(Map<Integer, Set<Integer>> childrenMap) {
		this.childrenMap = childrenMap;
	}
	
}
