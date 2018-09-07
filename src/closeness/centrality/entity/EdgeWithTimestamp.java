package closeness.centrality.entity;

public class EdgeWithTimestamp implements Comparable<EdgeWithTimestamp> {

	private int target;
	private int timestamp;
	
	public EdgeWithTimestamp(int target, int timestamp) {
		this.target = target;
		this.timestamp = timestamp;
	}
	
	public int getTarget() {
		return target;
	}
	public int getTimestamp() {
		return timestamp;
	}
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + target;
		result = prime * result + timestamp;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EdgeWithTimestamp other = (EdgeWithTimestamp) obj;
		if (target != other.target)
			return false;
		if (timestamp != other.timestamp)
			return false;
		return true;
	}

	@Override
	public int compareTo(EdgeWithTimestamp other) {
		return Integer.compare(this.target, other.target);
	}

}
