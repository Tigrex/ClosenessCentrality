import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TimeInterval implements Comparable<TimeInterval> {

	private int startTime;
	private int endTime;
	
	public TimeInterval(int startTime, int endTime) {
		this.startTime = startTime;
		this.endTime = endTime;
	}
	
	public int getStartTime() {
		return this.startTime;
	}
	
	public int getEndTime() {
		return this.endTime;
	}
	
	public TimeInterval unionIntersected(TimeInterval other) {
		int startTime = Math.min(this.getStartTime(), other.getStartTime());
		int endTime = Math.max(this.getEndTime(), other.getEndTime());
		return new TimeInterval(startTime, endTime);
	}
	
	public TimeInterval intersect(TimeInterval other) {
		
		if (this.getStartTime() <= other.getStartTime()) {
			
			if (this.getEndTime() < other.getStartTime()) {
				return null;
			} 
			
			if (other.getEndTime() < this.getEndTime()) {
				return new TimeInterval(other.getStartTime(), other.getEndTime());
			}
			
			return new TimeInterval(other.getStartTime(), this.getEndTime());
			
		}
		
		if (other.getEndTime() < this.getStartTime()) {
			return null;
		}
		
		if (this.getEndTime() < other.getEndTime()) {
			return new TimeInterval(this.getStartTime(), this.getEndTime());
		}

		return new TimeInterval(this.getStartTime(), other.getEndTime());
	}
	
	@Override
	public String toString() {
		return "[" + this.startTime + ", " + this.endTime + "]"; 
	}

	@Override
	public int compareTo(TimeInterval other) {
		return Integer.compare(this.startTime, other.startTime);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + endTime;
		result = prime * result + startTime;
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
		TimeInterval other = (TimeInterval) obj;
		if (endTime != other.endTime)
			return false;
		if (startTime != other.startTime)
			return false;
		return true;
	}
	
	
	public static void main(String[] args) {

		TimeInterval t1 = new TimeInterval(1, 4);
		TimeInterval t2 = new TimeInterval(2, 3);
		List<TimeInterval> times = new ArrayList<TimeInterval>();
		times.add(t1);
		times.add(t2);
		Collections.sort(times);
		System.out.println(times.get(0));
		
		System.out.println(new TimeInterval(1, 5).intersect(new TimeInterval(6, 7)));
		System.out.println(new TimeInterval(6, 7).intersect(new TimeInterval(1, 5)));
		System.out.println();
		
		System.out.println(new TimeInterval(6, 7).intersect(new TimeInterval(1, 6)));
		System.out.println(new TimeInterval(1, 6).intersect(new TimeInterval(6, 7)));
		System.out.println();
		
		System.out.println(new TimeInterval(1, 5).intersect(new TimeInterval(2, 4)));
		System.out.println(new TimeInterval(2, 4).intersect(new TimeInterval(1, 5)));
		System.out.println();
		
		System.out.println(new TimeInterval(1, 5).intersect(new TimeInterval(2, 8)));
		System.out.println(new TimeInterval(2, 8).intersect(new TimeInterval(1, 5)));

	}

}
