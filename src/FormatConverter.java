import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class FormatConverter {
	
	public static void main(String[] args) {
		
		String path = "data/dblp-2018-01-01.xml.teg.sim";
		path = "data/Scale21_Edge16.raw.uniform.4000";
		path = "data/out.wikipedia-growth.teg.sim";
		path = "data/Scale21_Edge16.raw.uniform.8000";
		
		try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(path + ".csv"), "utf-8"));			
		    String line;
		    String[] parts;

		    int numLines = 0;
		    
		    while ((line = reader.readLine()) != null) {
		    	
		    	numLines++;
		    	if (numLines % 1000000 == 0) {
					System.out.println("Reading line " + numLines + "...");
				}
		    	
		    	parts = line.split(",");
		    	
				String newLine = parts[2] + " " + parts[0] + " " + parts[1];
				writer.write(newLine);
				writer.newLine();
		    }
		    
		    reader.close();
		    writer.close();
		    
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
