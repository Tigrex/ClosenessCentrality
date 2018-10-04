package closeness.centrality.entity;
import java.util.List;
import java.util.PriorityQueue;

public class PQItem implements Comparable<PQItem> {

	private int vertexId;
	private int distance;
	public List<Integer> parents;
	
	public PQItem(int vertexId, int distance, List<Integer> parents) {
		this.vertexId = vertexId;
		this.distance = distance;
		this.parents = parents;
	}
	
	public PQItem(int vertexId, int distance) {
		this.vertexId = vertexId;
		this.distance = distance;
	}
	
	public int getVertexId() {
		return vertexId;
	}

	public int getDistance() {
		return distance;
	}


	@Override
	public int compareTo(PQItem other) {
		int r1 = Integer.compare(this.distance, other.distance);
		if (r1 != 0) {
			return r1;
		}
		return Integer.compare(this.vertexId, other.vertexId);
	}

	
	public static void main(String[] args) {
		PriorityQueue<PQItem> pq = new PriorityQueue<PQItem>();
		
		pq.add(new PQItem(0, 2));
		pq.add(new PQItem(1, 1));
		pq.add(new PQItem(2, 3));
		pq.add(new PQItem(3, 2));
		pq.add(new PQItem(5, 1));
		
		while (pq.size() > 0) {
			PQItem first = pq.poll();
			System.out.println(first.getVertexId() + ", " + first.getDistance());
		}
		
		
	}
	
}
