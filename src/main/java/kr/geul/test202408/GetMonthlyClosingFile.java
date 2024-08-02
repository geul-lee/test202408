package kr.geul.test202408;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

public class GetMonthlyClosingFile {

	static File sourceFolder = new File("e:/KOSPI200/closing");
	static File[] sourceFiles = sourceFolder.listFiles();
	
	static BufferedWriter writer;
	
	public static void run() throws CsvValidationException, FileNotFoundException, IOException {

		for (File file : sourceFiles) {
			
			try (CSVReader reader = new CSVReader(new FileReader(file))) {
				
				String fileName = file.getName();
				String[] nextLine;
								
				if (isOpen(writer)) 
					writer.close();
				System.out.println(fileName);	
				
				writer = new BufferedWriter(new FileWriter("e:/KOSPI200/closing_monthly/" + fileName.substring(0, 4) + fileName.substring(5, 7) + ".csv", true));
				
				while ((nextLine = reader.readNext()) != null) {     
				
					StringBuilder sb = new StringBuilder();

					for (int i = 0; i < nextLine.length; i++) {

						sb.append(nextLine[i]);
						if (i < nextLine.length - 1) {
							sb.append(",");
						}

					}

					writer.write(sb.toString());
					writer.newLine();
					
				}
				
			} catch (IOException e) {
				e.printStackTrace(); 
			}	
			
		}
		
	}
	
	private static boolean isOpen(BufferedWriter writer2) {
		try {
			writer.append(' ');
			return true;
		} catch (NullPointerException | IOException e) {
			return false;
		}
	}
	
}
