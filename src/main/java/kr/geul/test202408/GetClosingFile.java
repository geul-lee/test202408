package kr.geul.test202408;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

public class GetClosingFile {

	static ArrayList<KROptionTransaction> rawObservationList, finalObservationList;
	static ArrayList<KRIndexObservation> indexObservationList;
	static String csvFilePath_targetFolder = "e:/KOSPI200/closing/";

	static File sourceFolder = new File("e:/KOSPI200/1minute_sorted"), indexFolder = new File("e:/KOSPI200/index"), targetFile;
	static File[] sourceFiles = sourceFolder.listFiles();
	
	public static void run() throws CsvValidationException, FileNotFoundException, IOException {

		for (File file : sourceFiles) {

			readFile(file);
			getClosingObservationList();
			writeFile(file);
			if (finalObservationList.size() > 0)
				System.out.println(file.getName());

		}

	}

	private static void writeFile(File file) {
		
		File targetFile = new File(csvFilePath_targetFolder + file.getName());
		
		if (targetFile.exists() == false) {
			
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFilePath_targetFolder + file.getName()))) {			

				for (KROptionTransaction observation : finalObservationList) {

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
		
	}
	
	private static void getClosingObservationList() {
		
		finalObservationList = new ArrayList<KROptionTransaction>();
		KRIndexObservation referenceIndexObservation = indexObservationList.get(indexObservationList.size() - 1);
		Integer referenceTime = Integer.parseInt(referenceIndexObservation.getMinute().substring(0, 2)) * 100 + Integer.parseInt(referenceIndexObservation.getMinute().substring(3, 5));  		
		
		for (int i = 0; i < rawObservationList.size(); i++) {
			
			KROptionTransaction transaction = rawObservationList.get(i);
			if ((referenceTime <= 1530 && transaction.getMinute().equals(referenceIndexObservation.getMinute())) || (referenceTime > 1530 && transaction.getMinute().equals("15:30"))) {
				
				finalObservationList.add(transaction);
				
			}
			
		}
		
		System.out.println(finalObservationList.size());
		
	}

	private static void readFile(File file) throws FileNotFoundException, IOException, CsvValidationException {

		rawObservationList = new ArrayList<KROptionTransaction>();
		File indexFile = new File("e:/KOSPI200/index/" + file.getName());
		
		if (indexFile.exists()) {
			
			try (CSVReader reader = new CSVReader(new FileReader(file))) {

				String[] nextLine;

				while ((nextLine = reader.readNext()) != null) {

					KROptionTransaction observation = new KROptionTransaction(nextLine);

					if (observation.getTransactionDateTime() != null && observation.getMaturityDate() != null)
						rawObservationList.add(observation);				

				}

			}

			try (CSVReader indexReader = new CSVReader(new FileReader(indexFile))) {

				indexObservationList = new ArrayList<KRIndexObservation>();

				String[] nextLine;

				while ((nextLine = indexReader.readNext()) != null) {

					if (nextLine.length == 4) {
						
						KRIndexObservation observation = new KRIndexObservation(nextLine);

						if (observation.getDate() != null && observation.getMinute() != null)
							indexObservationList.add(observation);		
						
					}
					
				}

			}
			
		}
			
	}
	
}
