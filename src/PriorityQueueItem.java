import java.util.PriorityQueue;

public class PriorityQueueItem implements Comparable<PriorityQueueItem> {

	private int vertexId;
	private int distance;
	private int timestamp;
	
	public PriorityQueueItem(int vertexId, int distance, int timestamp) {
		this.vertexId = vertexId;
		this.distance = distance;
		this.timestamp = timestamp;
	}
	
	public int getVertexId() {
		return vertexId;
	}

	public int getDistance() {
		return distance;
	}

	public int getTimestamp() {
		return timestamp;
	}

	@Override
	public int compareTo(PriorityQueueItem other) {
		int r1 = Integer.compare(this.timestamp, other.timestamp);
		if (r1 != 0) {
			return r1;
		}
		int r2 = Integer.compare(this.distance, other.distance);
		if (r2 != 0) {
			return r2;
		}
		return Integer.compare(this.vertexId, other.vertexId);
	}

	
	public static void main(String[] args) {
		PriorityQueue<PriorityQueueItem> pq = new PriorityQueue<PriorityQueueItem>();
		
		pq.add(new PriorityQueueItem(0, 2, 0));
		pq.add(new PriorityQueueItem(1, 1, 1));
		pq.add(new PriorityQueueItem(2, 3, 2));
		pq.add(new PriorityQueueItem(3, 2, 1));
		pq.add(new PriorityQueueItem(5, 1, 1));
		
		while (pq.size() > 0) {
			PriorityQueueItem first = pq.poll();
			System.out.println(first.getVertexId() + ", " + first.getDistance() + ", " + first.getTimestamp());
		}
		
		
	}
	
}
