package kr.geul.test202408;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

public class SortOneMinuteFile {

	static ArrayList<KROptionTransaction> observationList;
	static String csvFilePath_targetFolder = "c:/test/daily_1minute_sorted/";
	
	static File sourceFolder = new File("c:/test/daily_1minute"), targetFile;
	static File[] sourceFiles = sourceFolder.listFiles();
	
	public static void run() throws FileNotFoundException, IOException, CsvValidationException {
		
		for (File file : sourceFiles) {
			
			readFile(file);
			Collections.sort(observationList);
			writeFile(file);
			System.out.println(file.getName());	
			
		}
		
	}
	
	private static void writeFile(File file) {
		
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFilePath_targetFolder + file.getName()))) {

			for (KROptionTransaction observation : observationList) {
				
				if (observation.getTransactionDateTime() != null && observation.getMaturityDate() != null) {
					
					String[] row = observation.toStringArray();
					
					StringBuilder sb = new StringBuilder();

					for (int i = 0; i < row.length; i++) {

						sb.append(row[i]);
						if (i < row.length - 1) {
							sb.append(",");
						}

					}

					writer.write(sb.toString());
					writer.newLine();
					
				}
				

			}

			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private static void readFile(File file) throws FileNotFoundException, IOException, CsvValidationException {
		
		try (CSVReader reader = new CSVReader(new FileReader(file))) {
			
			observationList = new ArrayList<KROptionTransaction>();
			
			String[] nextLine;
			
			while ((nextLine = reader.readNext()) != null) {
				
				KROptionTransaction observation = new KROptionTransaction(nextLine);
				
				if (observation.getTransactionDateTime() != null && observation.getMaturityDate() != null)
					observationList.add(observation);				
				
			}
			
		}
		
	}
	
}
