package kr.geul.test202408;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

public class MergeEndpointData {

	static File vlEndpointFolder = new File("e:/KOSPI200/vl_endpoints"), mergedFile = new File("e:/KOSPI200/vl_endpoints.csv");
	static File[] vlEndpointFiles = vlEndpointFolder.listFiles();
	static ArrayList<String[]> vlEndpointList;
	
	public static void run() throws CsvValidationException {
		
		vlEndpointList = new ArrayList<String[]>();
		
		for (File vlEndpointFile : vlEndpointFiles) {
			
			System.out.println(vlEndpointFile.getName());
			
			try (CSVReader reader = new CSVReader(new FileReader(vlEndpointFile))) {

				String[] nextLine;

				while ((nextLine = reader.readNext()) != null) {

					String[] dateArray = {vlEndpointFile.getName().substring(0, 10)};
					String[] finalArray = concatenateArrays(dateArray, nextLine);
					
					vlEndpointList.add(finalArray);
						
				}

			} catch (IOException e) {
				e.printStackTrace(); 
			}
			
		}
		
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(mergedFile))) {

			for (int i = 0; i < vlEndpointList.size(); i++) {
				
				String[] vlEndpoints = vlEndpointList.get(i);
				
				StringBuilder sb = new StringBuilder();

				for (int j = 0; j < vlEndpoints.length; j++) {

					sb.append(vlEndpoints[j]);
					if (j < vlEndpoints.length - 1) {
						sb.append(",");
					}

				}

				writer.write(sb.toString());
				writer.newLine();
				
			}
		
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
    public static String[] concatenateArrays(String[] array1, String[] array2) {
    	
        int length1 = array1.length;
        int length2 = array2.length;
        String[] result = new String[length1 + length2];

        // Copy elements from the first array
        System.arraycopy(array1, 0, result, 0, length1);

        // Copy elements from the second array
        System.arraycopy(array2, 0, result, length1, length2);

        return result;
        
    }
	
}
