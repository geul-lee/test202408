package kr.geul.test202408;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

public class SplitIndexFile {

	static ArrayList<KRIndexObservation> sourceList = new ArrayList<KRIndexObservation>();
	static BufferedWriter writer;
	static File sourceFile = new File("c:/test/s.csv"), targetFolder = new File("c:/test/index");
	
	public static void run() throws CsvValidationException, FileNotFoundException, IOException {
		
		readFile(sourceFile);
				
		LocalDate previousDate = LocalDate.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		String formattedDate;
		
		for (int i = 0; i < sourceList.size(); i++) {
			
			KRIndexObservation observation = sourceList.get(i);
			LocalDate currentDate = observation.getDate();
			 
			if (previousDate.equals(currentDate) == false) {
			
				System.out.println(observation.getDate().toString());
				
				if (isOpen(writer)) 
					writer.close();
				
				formattedDate = currentDate.format(formatter);
				writer = new BufferedWriter(new FileWriter("c:/test/index/" + formattedDate + ".csv", true));
				System.out.println(formattedDate);
				
			}
			
			writer.append(observation.toString() + "\n");
			previousDate = currentDate;
			
		}
		
		writer.close();
		
	}

	private static void readFile(File file) throws FileNotFoundException, IOException, CsvValidationException {
		
		sourceList = new ArrayList<KRIndexObservation>();
		
		try (CSVReader reader = new CSVReader(new FileReader(file))) {
			
			String[] nextLine;
			reader.readNext();
			
			while ((nextLine = reader.readNext()) != null) { 		
				
				if (!nextLine[1].equals("�帶��") && !nextLine[1].equals("�����帶��")) {
					KRIndexObservation observation = new KRIndexObservation(nextLine);
					sourceList.add(observation); 
				}
	
			}
			
		} catch (IOException e) {
            e.printStackTrace();
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
