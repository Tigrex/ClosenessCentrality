import java.util.ArrayList;
import java.util.List;

public class DistanceLabel {

	/**
	 * Start time of the interval, inclusive
	 */
	private int startTime;
	/**
	 * End time of the interval, inclusive
	 */
	private int endTime;
	
	/**
	 * Current shortest path distance
	 */
	private int distance;
	
	public DistanceLabel(int startTime, int endTime, int distance) {
		this.startTime = startTime;
		this.endTime = endTime;
		this.distance = distance;
	}
	
	public int getStartTime() {
		return this.startTime;
	}
	
	public int getEndTime() {
		return this.endTime;
	}
	
	public int getDistance() {
		return this.distance;
	}
	
	@Override
	public String toString() {
		return "[" + this.startTime + "," + this.endTime + "]:" + this.distance;
	}
	
	public static List<DistanceLabel> goThroughEdge(List<DistanceLabel> labels, int startTime, int endTime) {
		
		List<DistanceLabel> result = new ArrayList<DistanceLabel>();
		boolean startProcessing = false;
		for (DistanceLabel label: labels) {
			
			if (startProcessing) {
				
				if (label.getStartTime() > endTime) {
					break;
				}
				
				if (label.getEndTime() >= endTime) {
					DistanceLabel newLabel = new DistanceLabel(label.getStartTime(), endTime, label.getDistance() + 1);
					result.add(newLabel);
					break;
				} 
		
				DistanceLabel newLabel = new DistanceLabel(label.getStartTime(), label.getEndTime(), label.getDistance() + 1);
				result.add(newLabel);
				
			} else {
				
				if (label.getEndTime() >= startTime) {
					
					if (label.getStartTime() > endTime) {
						break;
					}
					
					int start = label.getStartTime() > startTime ? label.getStartTime() : startTime;
					
					if (label.getEndTime() >= endTime) {
						
						DistanceLabel newLabel = new DistanceLabel(start, endTime, label.getDistance() + 1);
						result.add(newLabel);
						break;
					}
					
					startProcessing = true;
					DistanceLabel newLabel = new DistanceLabel(start, label.getEndTime(), label.getDistance() + 1);
					result.add(newLabel);
				}

			}
			
		}
		
		return result;
		
	}
	
	
	/**
	 * Assume the list is sorted.
	 * Connect neighboring labels if their time interval is connected and have the same distance.
	 * @param list
	 */
	public static void simplify(List<DistanceLabel> list) {

		if (list == null || list.size() == 0) {
			return;
		}
		
		List<Integer> startingPositions = new ArrayList<Integer>();
		
		for (int i = 0; i < list.size() - 1; i++) {
			
			DistanceLabel cur = list.get(i);
			DistanceLabel next = list.get(i + 1);
			
			if (cur.endTime + 1 == next.startTime && cur.distance == next.distance) {
				startingPositions.add(i);
			}
			
		}
		
		if (startingPositions.size() == 0) {
			return;
		}
		
		for (int i = startingPositions.size() - 1; i >= 0; i--) {
			int position = startingPositions.get(i);
			DistanceLabel cur = list.get(position);
			DistanceLabel next = list.get(position + 1);
			DistanceLabel mergedLabel = new DistanceLabel(cur.startTime, next.endTime, cur.distance);
			list.remove(position + 1);
			list.remove(position);
			list.add(position, mergedLabel);
		}
		
	}

	
	public static String listToString(List<DistanceLabel> list) {
		if (list.size() == 0) {
			return "empty";
		}
		String result = "";
		for (DistanceLabel label: list) {
			result += label + " ";
		}
		result = result.substring(0, result.length() - 1);
		return result;
	}
	
	public static List<DistanceLabel> mergeLabels(List<DistanceLabel> labels1, List<DistanceLabel> labels2) {

		if (labels1.size() == 0) {
			List<DistanceLabel> result = new ArrayList<DistanceLabel>();
			result.addAll(labels2);
			return result;
		}

		if (labels2.size() == 0) {
			List<DistanceLabel> result = new ArrayList<DistanceLabel>();
			result.addAll(labels1);
			return result;		
		}

		List<DistanceLabel> newLabels = new ArrayList<DistanceLabel>();
		
		int pointer1 = 0;
		int pointer2 = 0;
		
		int currentTimestamp = 0;
		
		while (pointer1 / 3 != labels1.size() && pointer2 / 3 != labels2.size()) {
			
			DistanceLabel label1 = labels1.get(pointer1 / 3);
			DistanceLabel label2 = labels2.get(pointer2 / 3);

			// Both are at starts
			if (pointer1 % 3 == 0 && pointer2 % 3 == 0) {
				int start1 = label1.getStartTime();
				int start2 = label2.getStartTime();
				
				if (start1 < start2) {
					
					if (start2 > label1.getEndTime()) {
						newLabels.add(label1);
						pointer1 += 3;
						// Both are at starts again
					} else {
						DistanceLabel newLabel = new DistanceLabel(start1, start2 - 1, label1.getDistance());
						newLabels.add(newLabel);
						pointer1++;
						currentTimestamp = start2;
						// One is at start, the other in the middle
					}
					
				} else if (start2 < start1) {
					
					if (start1 > label2.getEndTime()) {
						newLabels.add(label2);
						pointer2 += 3;
						// Both are at starts again
					} else {
						DistanceLabel newLabel = new DistanceLabel(start2, start1 - 1, label2.getDistance());
						newLabels.add(newLabel);
						pointer2++;
						currentTimestamp = start1;
						// One is at start, the other in the middle
					}
					
				} else {
					
					int end1 = label1.getEndTime();
					int end2 = label2.getEndTime();
					int distance = label1.getDistance() < label2.getDistance() ? label1.getDistance() : label2.getDistance();
					
					if (end1 < end2) {
						DistanceLabel newLabel = new DistanceLabel(start1, end1, distance);
						newLabels.add(newLabel);
						pointer1 += 3;
						pointer2++;
						currentTimestamp = end1 + 1;
						// One is at start, the other in the middle
					} else if (end2 < end1) {
						DistanceLabel newLabel = new DistanceLabel(start1, end2, distance);
						newLabels.add(newLabel);
						pointer2 += 3;
						pointer1++;
						currentTimestamp = end2 + 1;
						// One is at start, the other in the middle
					} else {
						DistanceLabel newLabel = new DistanceLabel(start1, end1, distance);
						newLabels.add(newLabel);
						pointer1 += 3;
						pointer2 += 3;
						// Both are at starts again
					}

				}
				
			} 

			// One is at start, the other in the middle
			else if (pointer1 % 3 == 0 && pointer2 % 3 == 1) {

				int start1 = label1.getStartTime();
				int end1 = label1.getEndTime();
				int end2 = label2.getEndTime();
				int distance = label1.getDistance() < label2.getDistance() ? label1.getDistance() : label2.getDistance();
				
				if (label1.getStartTime() > label2.getEndTime()) {
					DistanceLabel newLabel = new DistanceLabel(currentTimestamp, label2.getEndTime(), label2.getDistance());
					newLabels.add(newLabel);
					pointer2 += 2;
					// Both are at starts again
				} 
				else {
					
					if (start1 > currentTimestamp) {
						DistanceLabel newLabel = new DistanceLabel(currentTimestamp, start1 - 1, label2.getDistance());
						newLabels.add(newLabel);
						// One is at start, the other in the middle
					}
					
					if (end1 < end2) {
						DistanceLabel newLabel = new DistanceLabel(start1, end1, distance);
						newLabels.add(newLabel);
						pointer1 += 3;
						currentTimestamp = end1 + 1;
						// One is at start, the other in the middle
					} else if (end2 < end1) {
						DistanceLabel newLabel = new DistanceLabel(start1, end2, distance);
						newLabels.add(newLabel);
						pointer2 += 2;
						pointer1++;
						currentTimestamp = end2 + 1;
						// One is at start, the other in the middle
					} else {
						DistanceLabel newLabel = new DistanceLabel(start1, end1, distance);
						newLabels.add(newLabel);
						pointer2 += 2;
						pointer1 += 3;
						// Both are at starts again
					}
				}
				
			} 
			// reverse of the previous case
			else if (pointer2 % 3 == 0 && pointer1 % 3 == 1) {
				int start1 = label2.getStartTime();
				int end1 = label2.getEndTime();
				int end2 = label1.getEndTime();
				int distance = label1.getDistance() < label2.getDistance() ? label1.getDistance() : label2.getDistance();
				
				if (label2.getStartTime() > label1.getEndTime()) {
					DistanceLabel newLabel = new DistanceLabel(currentTimestamp, label1.getEndTime(), label1.getDistance());
					newLabels.add(newLabel);
					pointer1 += 2;
					// Both are at starts again
				} 
				else {
					
					if (start1 > currentTimestamp) {
						DistanceLabel newLabel = new DistanceLabel(currentTimestamp, start1 - 1, label2.getDistance());
						newLabels.add(newLabel);
						// One is at start, the other in the middle
					}
					
					if (end1 < end2) {
						DistanceLabel newLabel = new DistanceLabel(start1, end1, distance);
						newLabels.add(newLabel);
						pointer2 += 3;
						currentTimestamp = end1 + 1;
						// One is at start, the other in the middle
					} else if (end2 < end1) {
						DistanceLabel newLabel = new DistanceLabel(start1, end2, distance);
						newLabels.add(newLabel);
						pointer1 += 2;
						pointer2++;
						currentTimestamp = end2 + 1;
						// One is at start, the other in the middle
					} else {
						DistanceLabel newLabel = new DistanceLabel(start1, end1, distance);
						newLabels.add(newLabel);
						pointer1 += 2;
						pointer2 += 3;
						// Both are at starts again
					}
				}
			} else {
				System.out.println("pointer1: " + pointer1 + ", pointer2: " + pointer2);
			}
			
					
		}
		
		if (pointer1 / 3 < labels1.size()) {
			
			DistanceLabel first = labels1.get(pointer1 / 3);
			DistanceLabel newLabel = new DistanceLabel(currentTimestamp, first.getEndTime(), first.getDistance());
			newLabels.add(newLabel);
			
			for (int i = pointer1 / 3 + 1; i < labels1.size(); i++) {
				newLabels.add(labels1.get(i));
			}
		}
		
		else if (pointer2 / 3 < labels2.size()) {
			
			DistanceLabel first = labels2.get(pointer2 / 3);
			DistanceLabel newLabel = new DistanceLabel(currentTimestamp, first.getEndTime(), first.getDistance());
			newLabels.add(newLabel);
			
			for (int i = pointer2 / 3 + 1; i < labels2.size(); i++) {
				newLabels.add(labels2.get(i));
			}
		}
		
		return newLabels;
		
	}
	
	/**
	 * Merge labels2 into labels1, and get a list of better labels.
	 * @param labels1
	 * @param labels2
	 * @return
	 */
	public static List<DistanceLabel> mergeLabelsAndGetUpdated(List<DistanceLabel> labels1, List<DistanceLabel> labels2) {

		if (labels1.size() == 0) {
			labels1.addAll(labels2);
			return labels2;
		}

		if (labels2.size() == 0) {
			return null;		
		}

		List<DistanceLabel> newLabels = new ArrayList<DistanceLabel>();
		List<DistanceLabel> updated = new ArrayList<DistanceLabel>();
		int pointer1 = 0;
		int pointer2 = 0;
		
		int currentTimestamp = 0;
		
		while (pointer1 / 3 != labels1.size() && pointer2 / 3 != labels2.size()) {
			
//			System.out.println("pointer1: " + pointer1 + ", pointer2: " + pointer2);
			
			DistanceLabel label1 = labels1.get(pointer1 / 3);
			DistanceLabel label2 = labels2.get(pointer2 / 3);
			
//			System.out.println("label1: " + label1);
//			System.out.println("label2: " + label2);

			// Both are at starts
			if (pointer1 % 3 == 0 && pointer2 % 3 == 0) {
				int start1 = label1.getStartTime();
				int start2 = label2.getStartTime();
				
				if (start1 < start2) {
					
					if (start2 > label1.getEndTime()) {
						newLabels.add(label1);
						pointer1 += 3;
						currentTimestamp = label1.getEndTime() + 1;
						// Both are at starts again
					} else {
						DistanceLabel newLabel = new DistanceLabel(start1, start2 - 1, label1.getDistance());
						newLabels.add(newLabel);
						pointer1++;
						currentTimestamp = start2;
						// One is at start, the other in the middle
					}
					
				} else if (start2 < start1) {
					
					if (start1 > label2.getEndTime()) {
						newLabels.add(label2);
						updated.add(label2);
						pointer2 += 3;
						currentTimestamp = label2.getEndTime() + 1;
						// Both are at starts again
					} else {
						DistanceLabel newLabel = new DistanceLabel(start2, start1 - 1, label2.getDistance());
						newLabels.add(newLabel);
						updated.add(newLabel);
						pointer2++;
						currentTimestamp = start1;
						// One is at start, the other in the middle
					}
					
				} else {
					
					int end1 = label1.getEndTime();
					int end2 = label2.getEndTime();
					int distance = label1.getDistance() < label2.getDistance() ? label1.getDistance() : label2.getDistance();
					DistanceLabel newLabel;
					if (end1 < end2) {
						newLabel = new DistanceLabel(start1, end1, distance);
						newLabels.add(newLabel);
						pointer1 += 3;
						pointer2++;
						currentTimestamp = end1 + 1;
						// One is at start, the other in the middle
					} else if (end2 < end1) {
						newLabel = new DistanceLabel(start1, end2, distance);
						newLabels.add(newLabel);
						pointer2 += 3;
						pointer1++;
						currentTimestamp = end2 + 1;
						// One is at start, the other in the middle
					} else {
						newLabel = new DistanceLabel(start1, end1, distance);
						newLabels.add(newLabel);
						pointer1 += 3;
						pointer2 += 3;
						currentTimestamp = end1 + 1;
						// Both are at starts again
					}
					
					if (distance < label1.getDistance()) {
						updated.add(newLabel);
					}

				}
				
			} 

			// One is at start, the other in the middle
			else if (pointer1 % 3 == 0 && pointer2 % 3 == 1) {
				
				int start1 = label1.getStartTime();
				int end1 = label1.getEndTime();
				int end2 = label2.getEndTime();
				int distance = label1.getDistance() < label2.getDistance() ? label1.getDistance() : label2.getDistance();
				
				if (label1.getStartTime() > label2.getEndTime()) {
					DistanceLabel newLabel = new DistanceLabel(currentTimestamp, label2.getEndTime(), label2.getDistance());
					newLabels.add(newLabel);
					updated.add(newLabel);
					pointer2 += 2;
					currentTimestamp = label2.getEndTime() + 1;
					// Both are at starts again
				} 
				else {
					
					if (start1 > currentTimestamp) {
						DistanceLabel newLabel = new DistanceLabel(currentTimestamp, start1 - 1, label2.getDistance());
						newLabels.add(newLabel);
						updated.add(newLabel);
						currentTimestamp = start1;
						// One is at start, the other in the middle
					}
					
					DistanceLabel newLabel;
					if (end1 < end2) {
						newLabel = new DistanceLabel(start1, end1, distance);
						newLabels.add(newLabel);
						pointer1 += 3;
						currentTimestamp = end1 + 1;
						// One is at start, the other in the middle
					} else if (end2 < end1) {
						newLabel = new DistanceLabel(start1, end2, distance);
						newLabels.add(newLabel);
						pointer2 += 2;
						pointer1++;
						currentTimestamp = end2 + 1;
						// One is at start, the other in the middle
					} else {
						newLabel = new DistanceLabel(start1, end1, distance);
						newLabels.add(newLabel);
						pointer2 += 2;
						pointer1 += 3;
						currentTimestamp = end1 + 1;
						// Both are at starts again
					}
					
					if (distance < label1.getDistance()) {
						updated.add(newLabel);
					}
					
				}
				
			} 
			// reverse of the previous case
			else if (pointer2 % 3 == 0 && pointer1 % 3 == 1) {
				int start2 = label2.getStartTime();
				int end2 = label2.getEndTime();
				int end1 = label1.getEndTime();
				int distance = label1.getDistance() < label2.getDistance() ? label1.getDistance() : label2.getDistance();
				
				if (label2.getStartTime() > label1.getEndTime()) {
					DistanceLabel newLabel = new DistanceLabel(currentTimestamp, label1.getEndTime(), label1.getDistance());
					newLabels.add(newLabel);
					pointer1 += 2;
					currentTimestamp = label1.getEndTime() + 1;
					// Both are at starts again
				} 
				else {
					
					if (start2 > currentTimestamp) {
						DistanceLabel newLabel = new DistanceLabel(currentTimestamp, start2 - 1, label1.getDistance());
						newLabels.add(newLabel);
						currentTimestamp = start2;
						// One is at start, the other in the middle
					}
					
					DistanceLabel newLabel;
					if (end2 < end1) {
						newLabel = new DistanceLabel(start2, end2, distance);
						newLabels.add(newLabel);
						pointer2 += 3;
						currentTimestamp = end2 + 1;
						// One is at start, the other in the middle
					} else if (end1 < end2) {
						newLabel = new DistanceLabel(start2, end1, distance);
						newLabels.add(newLabel);
						pointer1 += 2;
						pointer2++;
						currentTimestamp = end1 + 1;
						// One is at start, the other in the middle
					} else {
						newLabel = new DistanceLabel(start2, end2, distance);
						newLabels.add(newLabel);
						pointer1 += 2;
						pointer2 += 3;
						currentTimestamp = end1 + 1;
						// Both are at starts again
					}
					
					if (distance < label1.getDistance()) {
						updated.add(newLabel);
					}
					
				}
			} else {
				System.out.println("pointer1: " + pointer1 + ", pointer2: " + pointer2);
			}
			
					
		}
		
		if (pointer1 / 3 < labels1.size()) {
			
			DistanceLabel first = labels1.get(pointer1 / 3);
			if (pointer1 % 3 == 1) {
				DistanceLabel newLabel = new DistanceLabel(currentTimestamp, first.getEndTime(), first.getDistance());
				newLabels.add(newLabel);
			} else {
				newLabels.add(first);
			}
			
			
			for (int i = pointer1 / 3 + 1; i < labels1.size(); i++) {
				newLabels.add(labels1.get(i));
			}
		}
		
		else if (pointer2 / 3 < labels2.size()) {
			DistanceLabel first = labels2.get(pointer2 / 3);
			if (pointer2 % 3 == 1) {
				DistanceLabel newLabel = new DistanceLabel(currentTimestamp, first.getEndTime(), first.getDistance());
				newLabels.add(newLabel);
				updated.add(newLabel);
			} else {
				newLabels.add(first);
				updated.add(first);
			}
			
			for (int i = pointer2 / 3 + 1; i < labels2.size(); i++) {
				newLabels.add(labels2.get(i));
				updated.add(labels2.get(i));
			}
		}
		
		if (!DistanceLabel.checkValid(newLabels)) {
			System.out.println("Error!!!");
			System.out.println("Labels1: " + DistanceLabel.listToString(labels1));
			System.out.println("Labels2: " + DistanceLabel.listToString(labels2));
			System.out.println("Merged: " + DistanceLabel.listToString(newLabels));
			System.exit(1);
		}
		
		
		labels1.clear();
		labels1.addAll(newLabels);

		return updated;
		
	}
	
	public static boolean checkValid(List<DistanceLabel> labels) {
		
		if (labels == null || labels.size() <= 1) {
			return true;
		}
		
		for (int i = 0; i < labels.size() - 1; i++) {
			DistanceLabel cur = labels.get(i);
			DistanceLabel next = labels.get(i + 1);
			if (next.getStartTime() <= cur.getEndTime()) {
				return false;
			}
		}
		
		return true;
	}
	
	
	public static void testMergeLabels() {
		//[0,2]:1,[5,8]:2,[9,11]:3
		List<DistanceLabel> labels1 = new ArrayList<DistanceLabel>();
		labels1.add(new DistanceLabel(0, 2, 1));
		labels1.add(new DistanceLabel(5, 8, 2));
		labels1.add(new DistanceLabel(9, 11, 3));
		System.out.println("List 1: [0,2]:1,[5,8]:2,[9,11]:3");

		//[1,3]:3,[5,7]:2,[8,10]:1
		List<DistanceLabel> labels2 = new ArrayList<DistanceLabel>();
		labels2.add(new DistanceLabel(1, 3, 3));
		labels2.add(new DistanceLabel(5, 7, 2));
		labels2.add(new DistanceLabel(8, 10, 1));
		System.out.println("List 2: [1,3]:3,[5,7]:2,[8,10]:1");
		
		List<DistanceLabel> result = DistanceLabel.mergeLabels(labels1, labels2);
		System.out.println("After merging: " + DistanceLabel.listToString(result));
		DistanceLabel.simplify(result);
		System.out.println("After simplify: " + DistanceLabel.listToString(result));
		
		
		//[0,5]:10,[6,9]:8,[10,18]:12,[19,20]:7
		labels1 = new ArrayList<DistanceLabel>();
		labels1.add(new DistanceLabel(0, 5, 10));
		labels1.add(new DistanceLabel(6, 9, 8));
		labels1.add(new DistanceLabel(10, 18, 12));
		labels1.add(new DistanceLabel(19, 20, 7));
		System.out.println("List 1: [0,5]:10,[6,9]:8,[10,18]:12,[19,20]:7");

		//[0,7]:9,[8,12]:10,[13,15]:8,[16,20]:100
		labels2 = new ArrayList<DistanceLabel>();
		labels2.add(new DistanceLabel(0, 7, 9));
		labels2.add(new DistanceLabel(8, 12, 10));
		labels2.add(new DistanceLabel(13, 15, 8));
		labels2.add(new DistanceLabel(16, 20, 100));
		System.out.println("List 2: [0,7]:9,[8,12]:10,[13,15]:8,[16,20]:100");
		
		result = DistanceLabel.mergeLabels(labels1, labels2);
		System.out.println("After merging: " + DistanceLabel.listToString(result));
		DistanceLabel.simplify(result);
		System.out.println("After simplify: " + DistanceLabel.listToString(result));
	}

	public static void testMergeLabelsAndGetUpdated() {
		//[0,2]:1,[5,8]:2,[9,11]:3
		List<DistanceLabel> labels1 = new ArrayList<DistanceLabel>();
		labels1.add(new DistanceLabel(0, 2, 1));
		labels1.add(new DistanceLabel(5, 8, 2));
		labels1.add(new DistanceLabel(9, 11, 3));
		System.out.println("List 1: [0,2]:1,[5,8]:2,[9,11]:3");

		//[1,3]:3,[5,7]:2,[8,10]:1
		List<DistanceLabel> labels2 = new ArrayList<DistanceLabel>();
		labels2.add(new DistanceLabel(1, 3, 3));
		labels2.add(new DistanceLabel(5, 7, 2));
		labels2.add(new DistanceLabel(8, 10, 1));
		System.out.println("List 2: [1,3]:3,[5,7]:2,[8,10]:1");
		
		List<DistanceLabel> updated = DistanceLabel.mergeLabelsAndGetUpdated(labels1, labels2);
		DistanceLabel.simplify(labels1);
		DistanceLabel.simplify(updated);	
		System.out.println("After merging: " + DistanceLabel.listToString(labels1));
		System.out.println("Updated: " + DistanceLabel.listToString(updated));
		
		
		//[0,5]:10,[6,9]:8,[10,18]:12,[19,20]:7
		labels1 = new ArrayList<DistanceLabel>();
		labels1.add(new DistanceLabel(0, 5, 10));
		labels1.add(new DistanceLabel(6, 9, 8));
		labels1.add(new DistanceLabel(10, 18, 12));
		labels1.add(new DistanceLabel(19, 20, 7));
		System.out.println("List 1: [0,5]:10,[6,9]:8,[10,18]:12,[19,20]:7");

		//[0,7]:9,[8,12]:10,[13,15]:8,[16,20]:100
		labels2 = new ArrayList<DistanceLabel>();
		labels2.add(new DistanceLabel(0, 7, 9));
		labels2.add(new DistanceLabel(8, 12, 10));
		labels2.add(new DistanceLabel(13, 15, 8));
		labels2.add(new DistanceLabel(16, 20, 100));
		System.out.println("List 2: [0,7]:9,[8,12]:10,[13,15]:8,[16,20]:100");
		
		updated = DistanceLabel.mergeLabelsAndGetUpdated(labels1, labels2);
		DistanceLabel.simplify(labels1);
		DistanceLabel.simplify(updated);	
		System.out.println("After merging: " + DistanceLabel.listToString(labels1));
		System.out.println("Updated: " + DistanceLabel.listToString(updated));
		
		
		
		//[0,5]:10
		labels1 = new ArrayList<DistanceLabel>();
		labels1.add(new DistanceLabel(0, 9, 10));
		System.out.println("List 1: [0,9]:10");

		//[0,7]:9
		labels2 = new ArrayList<DistanceLabel>();
		labels2.add(new DistanceLabel(0, 7, 9));
		System.out.println("List 2: [0,7]:9");
		
		updated = DistanceLabel.mergeLabelsAndGetUpdated(labels1, labels2);
		DistanceLabel.simplify(labels1);
		DistanceLabel.simplify(updated);	
		System.out.println("After merging: " + DistanceLabel.listToString(labels1));
		System.out.println("Updated: " + DistanceLabel.listToString(updated));
		
		
		//[10,10]:3
		labels1 = new ArrayList<DistanceLabel>();
		labels1.add(new DistanceLabel(10, 10, 3));
		System.out.println("List 1: [10,10]:3");

		//[7,7]:2 [8,8]:3
		labels2 = new ArrayList<DistanceLabel>();
		labels2.add(new DistanceLabel(7, 7, 2));
		labels2.add(new DistanceLabel(8, 8, 3));
		System.out.println("List 2: [7,7]:2 [8,8]:3");
		
		updated = DistanceLabel.mergeLabelsAndGetUpdated(labels1, labels2);
		DistanceLabel.simplify(labels1);
		DistanceLabel.simplify(updated);	
		System.out.println("After merging: " + DistanceLabel.listToString(labels1));
		System.out.println("Updated: " + DistanceLabel.listToString(updated));

	}

	
	
	public static void testSimplify() {
		List<DistanceLabel> labels = new ArrayList<DistanceLabel>();
		labels.add(new DistanceLabel(0, 1, 8));
		labels.add(new DistanceLabel(2, 3, 12));
		labels.add(new DistanceLabel(4, 5, 12));
		labels.add(new DistanceLabel(6, 6, 12));
		labels.add(new DistanceLabel(7, 9, 12));
		labels.add(new DistanceLabel(10, 13, 4));
		System.out.println("Before simplify: " + DistanceLabel.listToString(labels));
		DistanceLabel.simplify(labels);
		System.out.println("After simplify: " + DistanceLabel.listToString(labels));
		
	}
	
	public static void testGoThroughEdge() {
		
		List<DistanceLabel> labels = new ArrayList<DistanceLabel>();
		labels.add(new DistanceLabel(0, 5, 10));
		labels.add(new DistanceLabel(6, 9, 8));
		labels.add(new DistanceLabel(10, 18, 12));
		labels.add(new DistanceLabel(19, 20, 7));
		System.out.println("List: [0,5]:10,[6,9]:8,[10,18]:12,[19,20]:7");
		List<DistanceLabel> result = DistanceLabel.goThroughEdge(labels, 7, 100);
		System.out.println("Go through [7, 100]: " + DistanceLabel.listToString(result));
		result = DistanceLabel.goThroughEdge(labels, 12, 19);
		System.out.println("Go through[12, 19]: " + DistanceLabel.listToString(result));

		labels.clear();
		labels.add(new DistanceLabel(10, 10, 8));
		result = DistanceLabel.goThroughEdge(labels, 3, 10);
		System.out.println("Go through [10, 10]: " + DistanceLabel.listToString(result));
		
		labels.clear();
		labels.add(new DistanceLabel(3, 8, 1));
		result = DistanceLabel.goThroughEdge(labels, 0, 10);
		System.out.println("Go through [0, 10]: " + DistanceLabel.listToString(result));
		
	}
	
	public static void main(String[] args) {
		
//		testSimplify();
		System.out.println();
//		testMergeLabels();
		System.out.println();
//		testGoThroughEdge();
		System.out.println();
		testMergeLabelsAndGetUpdated();
		
	}
	
}
