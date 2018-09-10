package closeness.centrality.entity;
import java.util.Map;
import java.util.Set;

public class SSSPTree {
	
	public int totalDistance;
	
	public int totalReachableVertices;
	
	public Map<Integer, Integer> nodeLevelMap;
	
	public int source;
	
	public Map<Integer, Set<Integer>> parentsMap;
	
	public Map<Integer, Set<Integer>> childrenMap;
	
	public SSSPTree(int totalDistance, int totalReachableVertices, int source, Map<Integer, Integer> nodeLevelMap, Map<Integer, Set<Integer>> parentsMap, Map<Integer, Set<Integer>> childrenMap) {
		this.totalDistance = totalDistance;
		this.totalReachableVertices = totalReachableVertices;
		this.source = source;
		
		this.nodeLevelMap = nodeLevelMap;
		this.parentsMap = parentsMap;
		this.childrenMap = childrenMap;
	}
	
	
	public void insertDirectedEdge(int from, int to) {
		int levelFrom = this.nodeLevelMap.get(from);
		int levelTo = this.nodeLevelMap.get(to);
		
		if (levelFrom == levelTo) return;
		
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
