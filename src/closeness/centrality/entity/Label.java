package closeness.centrality.entity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Label implements Comparable<Label> {
	
	
	private int vertex;
	private int distance;
	private TimeInterval timeInterval;
	
	public Label(int vertex, int distance, TimeInterval timeInterval) {
		this.vertex = vertex;
		this.distance = distance;
		this.timeInterval = timeInterval;
	}
	
	public Label(int distance, TimeInterval timeInterval) {
		this.distance = distance;
		this.timeInterval = timeInterval;
	}

	public int getDistance() {
		return distance;
	}

	public int getVertex() {
		return this.vertex;
	}
	
	public TimeInterval getTimeInterval() {
		return timeInterval;
	}
	
	public void setTimeInterval(TimeInterval newInterval) {
		this.timeInterval = newInterval;
	}
	
	public int getStartTime() {
		return this.timeInterval.getStartTime();
	}
	
	public int getEndTime() {
		return this.timeInterval.getEndTime();
	}
	
	public Label clone() {
		return new Label(this.distance, new TimeInterval(this.getStartTime(), this.getEndTime()));
	}
	
	public boolean hasIntersectionWith(Label other) {
		TimeInterval intersect = this.timeInterval.intersect(other.timeInterval);
		if (intersect == null) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * Assume they are intersected
	 * Remove dominated part (take smallest distance for each timestamp)
	 * @param other
	 * @return
	 */
	public List<Label> intersect(Label other) {
		
		if (!this.hasIntersectionWith(other)) {
			throw new RuntimeException("Error, no intersection.");
		}
		
		List<Label> result = new ArrayList<Label>();

		if (this.distance == other.distance) {
			TimeInterval interval = this.getTimeInterval().unionIntersected(other.getTimeInterval());
			Label l = new Label(this.distance, interval);
			result.add(l);
			return result;
		}
		
		int smallestDistance;
		if (this.distance < other.distance) {
			smallestDistance = this.distance;
		} else {
			smallestDistance = other.distance;
		}
		
		if (this.getStartTime() <= other.getStartTime()) {

			if (this.getStartTime() < other.getStartTime()) {
				Label first = new Label(this.distance, new TimeInterval(this.getStartTime(), other.getStartTime() - 1));
				result.add(first);
			}
			
			if (other.getEndTime() == this.getEndTime()) {
				Label second = new Label(smallestDistance, new TimeInterval(other.getStartTime(), other.getEndTime()));
				result.add(second);
				return result;
			}
			
			if (other.getEndTime() < this.getEndTime()) {
				Label second = new Label(smallestDistance, new TimeInterval(other.getStartTime(), other.getEndTime()));
				result.add(second);
				Label third = new Label(this.distance, new TimeInterval(other.getEndTime() + 1, this.getEndTime()));
				result.add(third);
				return result;
			}
			
			if (other.getEndTime() > this.getEndTime()) {
				Label second = new Label(smallestDistance, new TimeInterval(other.getStartTime(), this.getEndTime()));
				result.add(second);
				Label third = new Label(other.distance, new TimeInterval(this.getEndTime() + 1, other.getEndTime()));
				result.add(third);
				return result;
			}
			
		}
		
		Label first = new Label(other.distance, new TimeInterval(other.getStartTime(), this.getStartTime() - 1));
		result.add(first);
		
		if (other.getEndTime() == this.getEndTime()) {
			Label second = new Label(smallestDistance, new TimeInterval(this.getStartTime(), other.getEndTime()));
			result.add(second);
			return result;
		}
		
		if (other.getEndTime() < this.getEndTime()) {
			Label second = new Label(smallestDistance, new TimeInterval(this.getStartTime(), other.getEndTime()));
			result.add(second);
			Label third = new Label(this.distance, new TimeInterval(other.getEndTime() + 1, this.getEndTime()));
			result.add(third);
			return result;
		}
		
		else { //if (other.getEndTime() > this.getEndTime()) {
			Label second = new Label(smallestDistance, new TimeInterval(this.getStartTime(), this.getEndTime()));
			result.add(second);
			Label third = new Label(other.distance, new TimeInterval(this.getEndTime() + 1, other.getEndTime()));
			result.add(third);
			return result;
		}
	
	}
	
	public static List<Label> concatenateLabels(List<Label> labels) {
		if (labels.size() <= 1) {
			return labels;
		}
		
		List<Label> newLabels = new ArrayList<Label>();
		Label pre = labels.get(0);
		for (int i = 1; i < labels.size(); i++) {
			Label cur = labels.get(i);
			if (pre.getDistance() == cur.getDistance() && pre.getEndTime() + 1 == cur.getStartTime()) {
				pre = new Label(pre.getDistance(), new TimeInterval(pre.getStartTime(), cur.getEndTime()));
			} else {
				newLabels.add(pre);
				pre = cur;
			}
		}
		newLabels.add(pre);
		return newLabels;
	}
	
	public static boolean equalityCheck(List<Label> l1, List<Label> l2) {
		if (l1.size() != l2.size()) {
			return false;
		}
		
		for (int i = 0; i < l1.size(); i++) {
			if (!l1.get(i).equals(l2.get(i))) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * @param labels
	 * @param newLabel
	 * @return
	 */
	public static List<Label> mergeLabel2(List<Label> labels, Label newLabel) {

		List<Label> noIntersections = new ArrayList<Label>();
		
		List<Label> intersected = new ArrayList<Label>();
		for (int i = 0; i < labels.size(); i++) {
			Label l = labels.get(i);
			if (l.hasIntersectionWith(newLabel)) {
				intersected.add(l);
			} else {
				noIntersections.add(l);
			}
		}
		
		if (intersected.size() == 0) {
			List<Label> result = new ArrayList<Label>();
			result.addAll(labels);
			result.add(newLabel);
			Collections.sort(result);
			return result;
		}

		if (intersected.size() == 1) {
			noIntersections.addAll(intersected.get(0).intersect(newLabel));
			Collections.sort(noIntersections);
			return Label.concatenateLabels(noIntersections);
		}
		
		List<Label> result = new ArrayList<Label>();
		if (intersected.size() == 2) {
			result.addAll(intersected.get(0).intersect(new Label(newLabel.distance, new TimeInterval(newLabel.getStartTime(), intersected.get(0).getEndTime()))));
			result.addAll(intersected.get(1).intersect(new Label(newLabel.distance, new TimeInterval(intersected.get(0).getEndTime() + 1, newLabel.getEndTime()))));
			result.addAll(noIntersections);
			Collections.sort(result);
			return Label.concatenateLabels(result);
		}
		
		result.addAll(intersected.get(0).intersect(new Label(newLabel.distance, new TimeInterval(newLabel.getStartTime(), intersected.get(0).getEndTime()))));
		for (int i = 1; i < intersected.size() - 1; i++) {
			result.addAll(intersected.get(i).intersect(new Label(newLabel.distance, new TimeInterval(intersected.get(i - 1).getEndTime() + 1, intersected.get(i).getEndTime()))));
		}
		result.addAll(intersected.get(intersected.size() - 1).intersect(new Label(newLabel.distance, new TimeInterval(intersected.get(intersected.size() - 2).getEndTime() + 1, newLabel.getEndTime()))));
		result.addAll(noIntersections);
		Collections.sort(result);
		return Label.concatenateLabels(result);
		
	}
	
	/**
	 * Assume that the distance of newLabel can not be smaller than the distance of any label in labels
	 * @param labels
	 * @param newLabel
	 * @return
	 */
	public static boolean mergeLabel(List<Label> labels, Label newLabel) {
		boolean changed = false;
		
		List<Label> result = new ArrayList<Label>();
		
		for (int i = 0; i < labels.size(); i++) {
			Label label = labels.get(i);
			if (label.getStartTime() > newLabel.getEndTime()) {
				result.add(newLabel);
				for (int j = i; j < labels.size(); j++) {
					result.add(labels.get(j));
				}
				labels.clear();
				labels.addAll(result);
				return true;
			}
			if (label.getEndTime() < newLabel.getStartTime()) {
				result.add(label);
				continue;
			}
			
			// label.start <= new.end, label.end >= new.start
			if (newLabel.getStartTime() < label.getStartTime()) {
				Label l = new Label(newLabel.getDistance(), new TimeInterval(newLabel.getStartTime(), label.getStartTime() - 1));
				result.add(l);
				changed = true;
				newLabel.setTimeInterval(new TimeInterval(label.getStartTime(), newLabel.getEndTime()));
			}
			
			
			if (newLabel.getEndTime() <= label.getEndTime()) {
				
				for (int j = i; j < labels.size(); j++) {
					result.add(labels.get(j));
				}
				labels.clear();
				labels.addAll(result);
				return changed;
			} else {
				result.add(label);
				newLabel.setTimeInterval(new TimeInterval(label.getEndTime() + 1, newLabel.getEndTime()));
			}
			
		}
		
		result.add(newLabel);
		labels.clear();
		labels.addAll(result);
		return true;
	}
	
	
	@Override
	public String toString() {
		return "(" + this.distance + ", " + this.timeInterval + ")";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + distance;
		result = prime * result + ((timeInterval == null) ? 0 : timeInterval.hashCode());
		result = prime * result + vertex;
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
		Label other = (Label) obj;
		if (distance != other.distance)
			return false;
		if (timeInterval == null) {
			if (other.timeInterval != null)
				return false;
		} else if (!timeInterval.equals(other.timeInterval))
			return false;
		if (vertex != other.vertex)
			return false;
		return true;
	}


	@Override
	public int compareTo(Label other) {
		return Integer.compare(this.getTimeInterval().getStartTime(), other.getTimeInterval().getStartTime());
	}
	
	public static void main(String[] args) {
		
		List<Label> labels = new ArrayList<Label>();
		labels.add(new Label(0, new TimeInterval(1, 2)));
		labels.add(new Label(1, new TimeInterval(4, 6)));
		labels.add(new Label(0, new TimeInterval(8, 10)));
		Label newLabel = new Label(2, new TimeInterval(11, 12));
		
		System.out.println(Label.mergeLabel(labels, newLabel));
		
		for (Label l: labels) {
			System.out.println(l);
		}
		
		System.exit(0);
		
		System.out.println(new Label(1, new TimeInterval(1, 3)).intersect(new Label(1, new TimeInterval(1,  4))));
		System.out.println(new Label(1, new TimeInterval(1, 3)).intersect(new Label(1, new TimeInterval(2,  3))));
		System.out.println(new Label(1, new TimeInterval(1, 3)).intersect(new Label(1, new TimeInterval(2,  2))));
		System.out.println(new Label(1, new TimeInterval(1, 3)).intersect(new Label(1, new TimeInterval(0,  5))));
		System.out.println();
		
		System.out.println(new Label(2, new TimeInterval(1, 3)).intersect(new Label(1, new TimeInterval(2,  4))));
		System.out.println(new Label(2, new TimeInterval(1, 3)).intersect(new Label(1, new TimeInterval(2,  2))));
		System.out.println();
		
		System.out.println(new Label(2, new TimeInterval(2, 4)).intersect(new Label(1, new TimeInterval(1,  3))));
		System.out.println(Label.concatenateLabels(new Label(2, new TimeInterval(2, 4)).intersect(new Label(1, new TimeInterval(1,  3)))));
		System.out.println(new Label(2, new TimeInterval(1, 3)).intersect(new Label(1, new TimeInterval(0,  5))));
		System.out.println(Label.concatenateLabels(new Label(2, new TimeInterval(1, 3)).intersect(new Label(1, new TimeInterval(0,  5)))));
		System.out.println();
		
		
		List<Label> existingLabels = new ArrayList<Label>();
		existingLabels.add(new Label(1, new TimeInterval(0, 3)));
		existingLabels.add(new Label(2, new TimeInterval(4, 6)));
		existingLabels.add(new Label(3, new TimeInterval(7, 8)));
		existingLabels.add(new Label(2, new TimeInterval(9, 9)));
		
		System.out.println(Label.mergeLabel(existingLabels, new Label(0, new TimeInterval(0, 1))));
		System.out.println(Label.mergeLabel(existingLabels, new Label(0, new TimeInterval(3, 4))));
		System.out.println(Label.mergeLabel(existingLabels, new Label(5, new TimeInterval(3, 4))));
		System.out.println(Label.mergeLabel(existingLabels, new Label(0, new TimeInterval(0, 11))));
		System.out.println(Label.mergeLabel(existingLabels, new Label(4, new TimeInterval(10, 10))));
		
		System.out.println(Label.equalityCheck(existingLabels, existingLabels));
		
	}
	
	
}
