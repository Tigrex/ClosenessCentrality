import java.util.ArrayList;
import java.util.List;

public class DistanceLabels {

	private List<DistanceLabel> labels;
	
	public DistanceLabels() {
		this.labels = new ArrayList<DistanceLabel>();
	}
	
	public void setLabels(List<DistanceLabel> labels) {
		this.labels = labels;
	}
	
	
	/**
	 * @param otherLabels
	 */
	public void addLabelsSlow(DistanceLabels otherLabels) {
		List<DistanceLabel> others = otherLabels.getLabels();
		if (others.size() == 0) {
			return;
		}
		if (this.labels.size() == 0) {
			this.labels.addAll(others);
			return;
		}

		List<DistanceLabel> newLabels = new ArrayList<DistanceLabel>();
		
		int currentTimestamp = 0;
		
		int thisIndex = 0;
		int otherIndex = 0;
		
		while (thisIndex != this.labels.size() && otherIndex != others.size()) {
			
			DistanceLabel thisLabel = this.labels.get(thisIndex);
			DistanceLabel otherLabel = others.get(otherIndex);
			
			int startTime1 = thisLabel.getStartTime();
			int startTime2 = otherLabel.getStartTime();
			int endTime1 = thisLabel.getEndTime();
			int endTime2 = otherLabel.getEndTime();
			
			while (currentTimestamp <= endTime1 && currentTimestamp <= endTime2) {
	
//				System.out.println("Current timestamp: " + currentTimestamp);
//				System.out.println("Label 1: " + thisLabel);
//				System.out.println("Label 2: " + otherLabel);
				
				// Not started yet
				if (currentTimestamp < startTime1 && currentTimestamp < startTime2) {
				} 
				// Only 1 is covered
				else if (currentTimestamp >= startTime1 && currentTimestamp < startTime2) {
					DistanceLabel label = new DistanceLabel(currentTimestamp, currentTimestamp, thisLabel.getDistance());
					newLabels.add(label);
				}
				// Only 2 is covered
				else if (currentTimestamp >= startTime2 && currentTimestamp < startTime1) {
					DistanceLabel label = new DistanceLabel(currentTimestamp, currentTimestamp, otherLabel.getDistance());
					newLabels.add(label);
				}
				// Both are covered
				else {
					int distance = thisLabel.getDistance() < otherLabel.getDistance() ? thisLabel.getDistance() : otherLabel.getDistance();
					DistanceLabel label = new DistanceLabel(currentTimestamp, currentTimestamp, distance);
					newLabels.add(label);
				}
				
				currentTimestamp++;
			}
			
			if (currentTimestamp == endTime1 + 1) {
				thisIndex++;
			}
			if (currentTimestamp == endTime2 + 1) {
				otherIndex++;
			}
			
		}
		
		DistanceLabel endLabel1 = this.labels.get(this.labels.size() - 1);
		int endTime1 = endLabel1.getEndTime();
		DistanceLabel endLabel2 = others.get(others.size() - 1);
		int endTime2 = endLabel2.getEndTime();

		// It might be wrong here
		if (currentTimestamp <= endTime1) {
			DistanceLabel label = new DistanceLabel(currentTimestamp, endTime1, endLabel1.getDistance());
			newLabels.add(label);
		}
		
		if (currentTimestamp <= endTime2) {
			DistanceLabel label = new DistanceLabel(currentTimestamp, endTime2, endLabel2.getDistance());
			newLabels.add(label);
		}
		
		DistanceLabel.simplify(newLabels);
		this.labels = newLabels;
		
	}
	
	
	/**
	 * @param otherLabels
	 */
	public void addLabelsFast(DistanceLabels otherLabels) {
		List<DistanceLabel> others = otherLabels.getLabels();
		if (others.size() == 0) {
			return;
		}
		if (this.labels.size() == 0) {
			this.labels.addAll(others);
			return;
		}

		List<DistanceLabel> newLabels = new ArrayList<DistanceLabel>();
		
		int thisPointer = 0;
		int otherPointer = 0;
		
		int currentTimestamp = 0;
		
		while (thisPointer / 3 != this.labels.size() && otherPointer / 3 != others.size()) {
			
//			System.out.println("ThisPointer: " + thisPointer + ", otherPointer: " + otherPointer);
//			DistanceLabels toPrint = new DistanceLabels();
//			toPrint.setLabels(newLabels);
//			System.out.println(toPrint);
			
			
			DistanceLabel thisLabel = this.labels.get(thisPointer / 3);
			DistanceLabel otherLabel = others.get(otherPointer / 3);

			// Both are at starts
			if (thisPointer % 3 == 0 && otherPointer % 3 == 0) {
				int start1 = thisLabel.getStartTime();
				int start2 = otherLabel.getStartTime();
				
				if (start1 < start2) {
					
					if (start2 > thisLabel.getEndTime()) {
						newLabels.add(thisLabel);
						thisPointer += 3;
						// Both are at starts again
					} else {
						DistanceLabel newLabel = new DistanceLabel(start1, start2 - 1, thisLabel.getDistance());
						newLabels.add(newLabel);
						thisPointer++;
						currentTimestamp = start2;
						// One is at start, the other in the middle
					}
					
				} else if (start2 < start1) {
					
					if (start1 > otherLabel.getEndTime()) {
						newLabels.add(otherLabel);
						otherPointer += 3;
						// Both are at starts again
					} else {
						DistanceLabel newLabel = new DistanceLabel(start2, start1 - 1, otherLabel.getDistance());
						newLabels.add(newLabel);
						otherPointer++;
						currentTimestamp = start1;
						// One is at start, the other in the middle
					}
					
				} else {
					
					int end1 = thisLabel.getEndTime();
					int end2 = otherLabel.getEndTime();
					int distance = thisLabel.getDistance() < otherLabel.getDistance() ? thisLabel.getDistance() : otherLabel.getDistance();
					
					if (end1 < end2) {
						DistanceLabel newLabel = new DistanceLabel(start1, end1, distance);
						newLabels.add(newLabel);
						thisPointer += 3;
						otherPointer++;
						currentTimestamp = end1 + 1;
						// One is at start, the other in the middle
					} else if (end2 < end1) {
						DistanceLabel newLabel = new DistanceLabel(start1, end2, distance);
						newLabels.add(newLabel);
						otherPointer += 3;
						thisPointer++;
						currentTimestamp = end2 + 1;
						// One is at start, the other in the middle
					} else {
						DistanceLabel newLabel = new DistanceLabel(start1, end1, distance);
						newLabels.add(newLabel);
						thisPointer += 3;
						otherPointer += 3;
						// Both are at starts again
					}

				}
				
			} 

			// One is at start, the other in the middle
			else if (thisPointer % 3 == 0 && otherPointer % 3 == 1) {

				int start1 = thisLabel.getStartTime();
				int end1 = thisLabel.getEndTime();
				int end2 = otherLabel.getEndTime();
				int distance = thisLabel.getDistance() < otherLabel.getDistance() ? thisLabel.getDistance() : otherLabel.getDistance();
				
				if (thisLabel.getStartTime() > otherLabel.getEndTime()) {
					DistanceLabel newLabel = new DistanceLabel(currentTimestamp, otherLabel.getEndTime(), otherLabel.getDistance());
					newLabels.add(newLabel);
					otherPointer += 2;
					// Both are at starts again
				} 
				else {
					
					if (start1 > currentTimestamp) {
						DistanceLabel newLabel = new DistanceLabel(currentTimestamp, start1 - 1, otherLabel.getDistance());
						newLabels.add(newLabel);
						// One is at start, the other in the middle
					}
					
					if (end1 < end2) {
						DistanceLabel newLabel = new DistanceLabel(start1, end1, distance);
						newLabels.add(newLabel);
						thisPointer += 3;
						currentTimestamp = end1 + 1;
						// One is at start, the other in the middle
					} else if (end2 < end1) {
						DistanceLabel newLabel = new DistanceLabel(start1, end2, distance);
						newLabels.add(newLabel);
						otherPointer += 2;
						thisPointer++;
						currentTimestamp = end2 + 1;
						// One is at start, the other in the middle
					} else {
						DistanceLabel newLabel = new DistanceLabel(start1, end1, distance);
						newLabels.add(newLabel);
						otherPointer += 2;
						thisPointer += 3;
						// Both are at starts again
					}
				}
				
			} 
			// reverse of the previous case
			else if (otherPointer % 3 == 0 && thisPointer % 3 == 1) {
				int start1 = otherLabel.getStartTime();
				int end1 = otherLabel.getEndTime();
				int end2 = thisLabel.getEndTime();
				int distance = thisLabel.getDistance() < otherLabel.getDistance() ? thisLabel.getDistance() : otherLabel.getDistance();
				
				if (otherLabel.getStartTime() > thisLabel.getEndTime()) {
					DistanceLabel newLabel = new DistanceLabel(currentTimestamp, thisLabel.getEndTime(), thisLabel.getDistance());
					newLabels.add(newLabel);
					thisPointer += 2;
					// Both are at starts again
				} 
				else {
					
					if (start1 > currentTimestamp) {
						DistanceLabel newLabel = new DistanceLabel(currentTimestamp, start1 - 1, otherLabel.getDistance());
						newLabels.add(newLabel);
						// One is at start, the other in the middle
					}
					
					if (end1 < end2) {
						DistanceLabel newLabel = new DistanceLabel(start1, end1, distance);
						newLabels.add(newLabel);
						otherPointer += 3;
						currentTimestamp = end1 + 1;
						// One is at start, the other in the middle
					} else if (end2 < end1) {
						DistanceLabel newLabel = new DistanceLabel(start1, end2, distance);
						newLabels.add(newLabel);
						thisPointer += 2;
						otherPointer++;
						currentTimestamp = end2 + 1;
						// One is at start, the other in the middle
					} else {
						DistanceLabel newLabel = new DistanceLabel(start1, end1, distance);
						newLabels.add(newLabel);
						thisPointer += 2;
						otherPointer += 3;
						// Both are at starts again
					}
				}
			} else {
				
				System.out.println("ThisPointer: " + thisPointer + ", otherPointer: " + otherPointer);
			}
			
					
		}
		
		if (thisPointer / 3 < this.labels.size()) {
			
			DistanceLabel first = this.labels.get(thisPointer / 3);
			DistanceLabel newLabel = new DistanceLabel(currentTimestamp, first.getEndTime(), first.getDistance());
			newLabels.add(newLabel);
			
			for (int i = thisPointer / 3 + 1; i < this.labels.size(); i++) {
				newLabels.add(this.labels.get(i));
			}
		}
		
		else if (otherPointer / 3 < others.size()) {
			
			DistanceLabel first = others.get(otherPointer / 3);
			DistanceLabel newLabel = new DistanceLabel(currentTimestamp, first.getEndTime(), first.getDistance());
			newLabels.add(newLabel);
			
			for (int i = otherPointer / 3 + 1; i < others.size(); i++) {
				newLabels.add(others.get(i));
			}
		}
		
		this.labels = newLabels;
		
	}

	public void testFast() {
		//*[0,2]:1,[5,8]:2,[9,11]:3
		DistanceLabels l1 = new DistanceLabels();
		List<DistanceLabel> labels1 = new ArrayList<DistanceLabel>();
		labels1.add(new DistanceLabel(0, 2, 1));
		labels1.add(new DistanceLabel(5, 8, 2));
		labels1.add(new DistanceLabel(9, 11, 3));
		l1.setLabels(labels1);
		System.out.println("List 1: " + l1);

		//*[1,3]:3,[5,7]:2,[8,10]:1
		DistanceLabels l2 = new DistanceLabels();
		List<DistanceLabel> labels2 = new ArrayList<DistanceLabel>();
		labels2.add(new DistanceLabel(1, 3, 3));
		labels2.add(new DistanceLabel(5, 7, 2));
		labels2.add(new DistanceLabel(8, 10, 1));
		l2.setLabels(labels2);
		System.out.println("List 2: " + l2);
		
		l1.addLabelsFast(l2);
		DistanceLabel.simplify(l1.getLabels());
		System.out.println("After merging: " + l1);
		
		//*[0,5]:10,[6,9]:8,[10,18]:12,[19,20]:7
		l1 = new DistanceLabels();
		labels1 = new ArrayList<DistanceLabel>();
		labels1.add(new DistanceLabel(0, 5, 10));
		labels1.add(new DistanceLabel(6, 9, 8));
		labels1.add(new DistanceLabel(10, 18, 12));
		labels1.add(new DistanceLabel(19, 20, 7));
		l1.setLabels(labels1);
		System.out.println("List 1: " + l1);

		//*[0,7]:9,[8,12]:10,[13,15]:8,[16,20]:100
		l2 = new DistanceLabels();
		labels2 = new ArrayList<DistanceLabel>();
		labels2.add(new DistanceLabel(0, 7, 9));
		labels2.add(new DistanceLabel(8, 12, 10));
		labels2.add(new DistanceLabel(13, 15, 8));
		labels2.add(new DistanceLabel(16, 20, 100));
		l2.setLabels(labels2);
		System.out.println("List 2: " + l2);
		
		l1.addLabelsFast(l2);
		DistanceLabel.simplify(l1.getLabels());
		System.out.println("After merging: " + l1);
	}
	
	public void testSlow() {
		//*[0,2]:1,[5,8]:2,[9,11]:3
		DistanceLabels l1 = new DistanceLabels();
		List<DistanceLabel> labels1 = new ArrayList<DistanceLabel>();
		labels1.add(new DistanceLabel(0, 2, 1));
		labels1.add(new DistanceLabel(5, 8, 2));
		labels1.add(new DistanceLabel(9, 11, 3));
		l1.setLabels(labels1);
		System.out.println("List 1: " + l1);

		//*[1,3]:3,[5,7]:2,[8,10]:1
		DistanceLabels l2 = new DistanceLabels();
		List<DistanceLabel> labels2 = new ArrayList<DistanceLabel>();
		labels2.add(new DistanceLabel(1, 3, 3));
		labels2.add(new DistanceLabel(5, 7, 2));
		labels2.add(new DistanceLabel(8, 10, 1));
		l2.setLabels(labels2);
		System.out.println("List 2: " + l2);
		
		l1.addLabelsSlow(l2);
		
		DistanceLabel.simplify(l1.getLabels());
		
		System.out.println("After merging: " + l1);
	}
	
	
	
	public List<DistanceLabel> getLabels() {
		return this.labels;
	}
	
	@Override
	public String toString() {
		if (this.labels.size() == 0) {
			return "empty";
		}
		
		String result = "";
		for (DistanceLabel label: this.labels) {
			result += label + " ";
		}
		result = result.substring(0, result.length() - 1);
		return result;
	}
	
	public static void main(String[] args) {
		
		DistanceLabels labels = new DistanceLabels();
		labels.testSlow();
		
		labels.testFast();
	}
	
}



