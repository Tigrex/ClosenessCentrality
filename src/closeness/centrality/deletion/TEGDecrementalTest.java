package closeness.centrality.deletion;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tigrex.sg.edu.ntu.utility.FileUtility;


public class TEGDecrementalTest {
	
	private TimeEvolvingGraphDecremental teg = new TimeEvolvingGraphDecremental();
	
	final private Logger logger = LoggerFactory.getLogger(TEGDecrementalTest.class);
	
	public int getNumVertices() {
		return this.teg.getNumVertices();
	}
	
	public int getNumSnapshots() {
		return this.teg.getNumSnapshots();
	}
	
	public void loadTEG(String path) {
		this.teg.constructGraph(path);
	}
	
	public boolean correctnessTest(String path) {

		this.logger.info("+correctnessTest({})", path);
		
		this.loadTEG(path);
		
		for (int i = 0; i < 10; i++) {
			double[] centralities1 = this.teg.getCentralityDynamicIncremental(i + this.getNumVertices() / 2);
			double[] centralities2 = this.teg.getCentralityRangeBased(i + this.getNumVertices() / 2);
			
			if (compareDoubleArray(centralities1, centralities2) == false) {
				this.logger.error("Centralities1 and centralities2 are not equal.");
				this.logger.info("-correctnessTest({})", path);
				return false;
			}
			
		}
		
		this.logger.info("Successfully passed all tests (Dynamic, Range).");
		this.logger.info("-correctnessTest({})", path);
		return true;
	}
	
	
	
	public void maxDegreeTest() {
		
		for (int i = 17; i <= 21; i++) {
			String path = "data/Scale" + i + "_Edge16.raw.uniform.2000";
			this.maxDegreeTestHelper(path);
		}
		
		int[] timestamps = {100, 200, 500, 1000, 4000, 8000};
		
		for (int i = 0; i < timestamps.length; i++) {
			String path = "data/Scale21_Edge16.raw.uniform." + timestamps[i];
			this.maxDegreeTestHelper(path);
		}
		
	}
	
	public void randomDegreeTest(int numOfRuns) {
		
		for (int i = 17; i <= 21; i++) {
			String path = "data/Scale" + i + "_Edge16.raw.uniform.2000";
			this.randomDegreeTestHelper(path, numOfRuns);
		}
		
		int[] timestamps = {100, 200, 500, 1000, 4000, 8000};
		
		for (int i = 0; i < timestamps.length; i++) {
			String path = "data/Scale21_Edge16.raw.uniform." + timestamps[i];
			this.randomDegreeTestHelper(path, numOfRuns);
		}
		
	}
	
	
	public void syntheticGraphTestHelper(int numOfRuns, String deletion) {
		
		int scale = 21;
		String[] timestamps = {"100", "200", "500", "1000", "2000", "4000", "8000"};
		
		for (String timestamp: timestamps) {
			String path = "data/Scale" + scale + "_Edge16.raw.uniform." + timestamp + deletion;	
			this.loadTEG(path);
			singleGraphTestHelper(path, numOfRuns);
		}
		
		for (scale = 17; scale <= 21; scale++) {
			String path = "data/Scale" + scale + "_Edge16.raw.uniform.2000" + deletion;	
			this.loadTEG(path);
			singleGraphTestHelper(path, numOfRuns);
		}
		
	}
	
	
	public void singleGraphTestHelper(String path, int numOfRuns) {
		
		this.logger.info("+singleGraphTestHelper({},{})", path, numOfRuns);

		long[] totalRunningTimes = new long[2];
		totalRunningTimes[0] = 0;
		totalRunningTimes[1] = 0;
		
		// Get random degrees
		Random random = new Random(0);
		for (int i = 0; i < numOfRuns; i++) {
			int degree = random.nextInt(this.getNumVertices());
			long[] runningTimes = this.performanceTestHelper(degree);
			this.logger.info("{}:{},{}", degree, runningTimes[0], runningTimes[1]);
			
			totalRunningTimes[0] += runningTimes[0];
			totalRunningTimes[1] += runningTimes[1];
			
		}
				 
		this.logger.info("Total running time {},{}", totalRunningTimes[0], totalRunningTimes[1]);
		this.logger.info("-singleGraphTestHelper({})", path);
		
	}


	
	public void printSnapshotSize(String path) {
		
		logger.debug("+printSnapshotSize({})", path);
		
		Map<Integer, Integer> edgeCount = new HashMap<Integer, Integer>();

		try (BufferedReader br = new BufferedReader(new FileReader(path))) {
		    String line;
		    String[] parts;

		    int numLines = 0;
		    
		    while ((line = br.readLine()) != null) {
		    	
		    	numLines++;
		    	if (numLines % 1000000 == 0) {
					logger.debug("Reading line {}...", numLines);
				}
		    	
		    	parts = line.split(",");
		    	int timestamp = Integer.valueOf(parts[2]);
		    	
		    	if (edgeCount.containsKey(timestamp)) {
		    		edgeCount.put(timestamp, edgeCount.get(timestamp) + 1);
		    	} else {
		    		edgeCount.put(timestamp, 1);
		    	}
		    }
		    	
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
		
		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(path + ".count"), "utf-8"));
			
			for (int i = 0; i < edgeCount.size(); i++) {
				writer.write(i + "," + edgeCount.get(i));
				writer.newLine();
			}
			
			writer.close();
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		logger.debug("-printSnapshotSize({})", path);
		
	}
	
	
	private boolean compareDouble(double d1, double d2) {
		
		if (Math.abs(d1 - d2) <= 0.0000001) {
			return true;
		}
		
		return false;
		
	}
	
	private boolean compareDoubleArray(double[] da1, double[] da2) {
		if (da1.length != da2.length) {
			return false;
		}
		for (int i = 0; i < da1.length; i++) {
			if (compareDouble(da1[i], da2[i]) == false) {
				logger.debug("Index {} not equal.", i);
				return false;
			}
		}
		return true;
	}

	
	private long[] maxDegreeTestHelper(String path) {
		
		this.logger.info("+maxDegreeTest({})", path);

		this.loadTEG(path);

		// Get max degree vertex
		int maxDegreeVertex = 0;

		long[] runningTimes = this.performanceTestHelper(maxDegreeVertex);
		
		this.logger.info("{},{}", runningTimes[0], runningTimes[1]);

		this.logger.info("-maxDegreeTest({})", path);

		return runningTimes;
		
	}
	
	
	private long[] randomDegreeTestHelper(String path, int numOfRuns) {
		
		this.logger.info("+randomDegreeTestHelper({},{})", path, numOfRuns);

		this.loadTEG(path);
		
		List<Integer> sourceIds = new ArrayList<Integer>(numOfRuns);
		
		long[] totalRunningTimes = new long[2];
		totalRunningTimes[0] = 0;
		totalRunningTimes[1] = 0;
		
		// Get random degrees
		Random random = new Random(0);
		for (int i = 0; i < numOfRuns; i++) {
			int degree = random.nextInt(this.getNumVertices());
			sourceIds.add(degree);
			long[] runningTimes = this.performanceTestHelper(degree);
			this.logger.info("{}:{},{}", degree, runningTimes[0], runningTimes[1]);
			
			totalRunningTimes[0] += runningTimes[0];
			totalRunningTimes[1] += runningTimes[1];
			
		}
				 
		this.logger.info("Total running time {},{}", totalRunningTimes[0], totalRunningTimes[1]);
		
		FileUtility utility = new FileUtility();
		utility.writeListToFile(sourceIds, path + ".ids");
		
		this.logger.info("-randomDegreeTestHelper({})", path);

		return totalRunningTimes;
		
	}
	
	
	private long[] performanceTestHelper(int sourceVertex) {
		
		long start = System.currentTimeMillis();
		double[] centralities1 = this.teg.getCentralityDynamicIncremental(sourceVertex);
		long end = System.currentTimeMillis();
		long time1 = end - start;
		
		start = System.currentTimeMillis();
		double[] centralities2 = this.teg.getCentralityRangeBased(sourceVertex);
		end = System.currentTimeMillis();
		long time2 = end - start;

		// Correctness test
		if (compareDoubleArray(centralities1, centralities2) == false) {
			this.logger.error("Results are wrong: Number of centralities not equal.");
			System.exit(1);
		}
		
		long[] runningTimes = new long[2];
		runningTimes[0] = time1;
		runningTimes[1] = time2;
		
		return runningTimes;
	}
	

	
	public static void main(String[] args) {
		
		TEGDecrementalTest test = new TEGDecrementalTest();
		
//		String path = "data/youtube-d-growth.txt.teg.sim";
//		String path = "data/out.wikipedia-growth.teg.sim";
//		String path = "data/dblp-2018-01-01.xml.teg.sim";
//		String path = "data/sample.txt";
		
//		path = "data/dblp-2018-01-01.xml.teg.sim";
//		test.correctnessTest(path);
		
//		test.realGraphTestHelper(path, 100);
		
		test.syntheticGraphTestHelper(100, ".0.05");
		
	}
	
}
