package closeness.centrality.entity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SSSPTree {
	
	public int totalDistance;
	
	public int totalReachableVertices;
	
	public Map<Integer, Integer> nodeLevelMap;
	
	public int source;
	
	public Map<Integer, Set<Integer>> parentsMap;
	
	public Map<Integer, Set<Integer>> childrenMap;
	
	public Map<Integer, Set<Integer>> graph;
	
	public SSSPTree(Map<Integer, Set<Integer>> graph, int totalDistance, int totalReachableVertices, int source, Map<Integer, Integer> nodeLevelMap, Map<Integer, Set<Integer>> parentsMap, Map<Integer, Set<Integer>> childrenMap) {
		this.graph = graph;
		this.totalDistance = totalDistance;
		this.totalReachableVertices = totalReachableVertices;
		this.source = source;
		
		this.nodeLevelMap = nodeLevelMap;
		this.parentsMap = parentsMap;
		this.childrenMap = childrenMap;
	}
	
	
	public void insertDirectedEdge(int from, int to) {
		// Update graph first
		if (!this.graph.containsKey(from)) {
			Set<Integer> vs = new HashSet<Integer>();
			this.graph.put(from, vs);
		}
		
		this.graph.get(from).add(to);
		
		if (!this.nodeLevelMap.containsKey(from)) {
			return;
		}
		int levelFrom = this.nodeLevelMap.get(from);
		
		if (this.nodeLevelMap.containsKey(to)) {
			int levelTo = this.nodeLevelMap.get(to);
			
			if (levelTo < levelFrom + 1) return;
			
			if (levelTo == levelFrom + 1) {
				this.updateParentChildrenMap(from, to);
				return;
			}
			
			this.totalDistance -= levelTo;
			this.totalDistance += levelFrom + 1;
			this.nodeLevelMap.put(to, levelFrom + 1);
		} else {
			this.nodeLevelMap.put(to, levelFrom + 1);
			this.totalDistance += levelFrom + 1;
			this.totalReachableVertices += 1;
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
						// Shorter distance
						this.nodeLevelMap.put(child, level);
						this.totalDistance += level;
						this.totalReachableVertices += 1;

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
							this.parentsMap.get(child).clear();
						}
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
