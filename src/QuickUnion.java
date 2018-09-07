import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuickUnion {

	private int[] root;
	
	private int[] size;

	public QuickUnion(int N) {

		root = new int[N];
		size = new int[N];
		
		for (int i = 0; i < N; i++) {
			root[i] = i;
			size[i] = 1;
		}
	}

	public boolean find(int p, int q) {
		return root(p) == root(q);
	}

	public void unite(int p, int q) {
		int i = root(p);
		int j = root(q);
		
		if (size[i] < size[j]) {
			root[i] = j;
			size[j] += size[i];
		} else {
			root[j] = i;
			size[i] += size[j];
		}
		
	}
	
	public String checkRoots() {
		String result = "";
		for (int i = 0; i < root.length; i++) {
			result += "root[" + i + "]=" + root[i] + " ";
		}
		return result;
	}
	
	public List<List<Integer>> getComponents() {
		
		Map<Integer, List<Integer>> components = new HashMap<Integer, List<Integer>>();
		
		for (int i = 0; i < root.length; i++) {
			
			int topVertex = root(i);
			
			if (i != topVertex) {
				
				if (components.get(topVertex) == null) {
					
					List<Integer> neighbors = new ArrayList<Integer>();
					neighbors.add(i);
					components.put(topVertex, neighbors);
					
				} else {
					components.get(topVertex).add(i);
				}
				
			} else {
				
				if (components.get(topVertex) == null) {
					
					List<Integer> neighbors = new ArrayList<Integer>();
					neighbors.add(topVertex);
					components.put(topVertex, neighbors);
					
				} else {
					components.get(topVertex).add(topVertex);
				}
				
			}
			
		}
		
		return new ArrayList<List<Integer>>(components.values());
		
	}

	private int root(int i) {
		while (i != root[i]) {
			root[i] = root[root[i]];
			i = root[i];
		}
		return i;
	}
	
	public static void main(String[] args) {
		
		QuickUnion union = new QuickUnion(11);
		
		int[] sources = {0, 1, 3, 3, 6, 2,  9, 3, 7};
		int[] dests   = {1, 2, 4, 5, 7, 8, 10, 6, 9};
		
		for (int i = 0; i < sources.length; i++) {
			
			if (!union.find(sources[i], dests[i])) {
				union.unite(sources[i], dests[i]);
			}
			
		}
		
		List<List<Integer>> components = union.getComponents();
		for (int i = 0; i < components.size(); i++) {
			for (Integer vertex: components.get(i)) {
				System.out.print(vertex + " ");
			}
			System.out.println();
		}
		
	}
	
}

