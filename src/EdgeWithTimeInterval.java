
public class EdgeWithTimeInterval implements Comparable<EdgeWithTimeInterval> {

	private int target;
	private TimeInterval timeInterval;
	
	public EdgeWithTimeInterval(int target, TimeInterval timeInterval) {
		this.target = target;
		this.timeInterval = timeInterval;
	}
	
	public int getTarget() {
		return target;
	}

	public TimeInterval getTimeInterval() {
		return this.timeInterval;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + target;
		result = prime * result + ((timeInterval == null) ? 0 : timeInterval.hashCode());
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
		EdgeWithTimeInterval other = (EdgeWithTimeInterval) obj;
		if (target != other.target)
			return false;
		if (timeInterval == null) {
			if (other.timeInterval != null)
				return false;
		} else if (!timeInterval.equals(other.timeInterval))
			return false;
		return true;
	}

	@Override
	public int compareTo(EdgeWithTimeInterval other) {
		return Integer.compare(this.target, other.target);
	}

}
