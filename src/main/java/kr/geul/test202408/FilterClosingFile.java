package kr.geul.test202408;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

public class FilterClosingFile {

	static final double minimumOptionPrice = 0.375; 
	
	static ArrayList<KROptionTransaction> rawObservationList, filteredObservationList;
	static ArrayList<KRIndexObservation> indexObservationList;
	
	static String csvFilePath_targetFolder = "e:/KOSPI200/closing_filtered/";
	
	static File sourceFolder = new File("e:/KOSPI200/closing"), targetFile;
	static File[] sourceFiles = sourceFolder.listFiles();
	
	public static void run() throws CsvValidationException, FileNotFoundException, IOException {

		for (File file : sourceFiles) {

			System.out.println(file.getName());
			readFile(file);
			filter();
			writeFile(file); 

		}

	}

	private static void writeFile(File file) {
		
		File targetFile = new File(csvFilePath_targetFolder + file.getName());
		
		if (targetFile.exists() == false) {
			
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFilePath_targetFolder + file.getName()))) {			

				for (KROptionTransaction observation : filteredObservationList) {

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
	
	private static void filter() {
		
		filteredObservationList = new ArrayList<KROptionTransaction>();
		Double sRaw = getIndexValue(rawObservationList.get(0).getMinute()),
			   r = 0d,
			   q = 0d;
				
		for (int i = 0; i < rawObservationList.size(); i++) {
			
			KROptionTransaction rawObservation = rawObservationList.get(i);
			
			if (isOTM(rawObservation, sRaw, r, q) == true 
					&& isExpensiveEnough(rawObservation) == true
					&& doesViolateEuropeanArbitrageRestriction
					(rawObservation, sRaw, r, q) == false
					&& isMaturityWithinRange(rawObservation) == true)
				filteredObservationList.add(rawObservation);
			
		}
		
	}

	private static boolean isMaturityWithinRange(KROptionTransaction rawObservation) {
		
		LocalDate t = rawObservation.getTransactionDate(),
			      T = rawObservation.getMaturityDate();
		
		Double tau = Double.valueOf(ChronoUnit.DAYS.between(t, T));
		
		if (tau < 7 || tau > 365)
			return false;
		else
			return true;
		
	}

	private static boolean doesViolateEuropeanArbitrageRestriction(KROptionTransaction rawObservation, Double sharePrice, Double rfr,
			Double dividendRate) {
		
		LocalDate t = rawObservation.getTransactionDate(),
			      T = rawObservation.getMaturityDate();
		
		Double strike = rawObservation.getStrikePrice(),
			   price = rawObservation.getOptionPrice(),
			   tau = Double.valueOf(ChronoUnit.DAYS.between(t, T)) / 365d;
		
		String cpFlag = rawObservation.getIsCall();
		
		if (cpFlag.equals("CALL")) {

			if (price > sharePrice * Math.exp(-1.0 * dividendRate) || 
					price < Math.max(0, sharePrice * Math.exp(-1.0 * dividendRate) - (strike * Math.exp(-1.0 * rfr * tau))))
				return true;
			else
				return false;

		}

		else {

			if (price > sharePrice * Math.exp(-1.0 * dividendRate) || 
					price < Math.max(0, (strike * Math.exp(-1.0 * rfr * tau))) - sharePrice * Math.exp(-1.0 * dividendRate))
				return true;
			else
				return false;

		}		
		
	}

	private static boolean isExpensiveEnough(KROptionTransaction rawObservation) {
		
		boolean isExpensiveEnough;
		
		if (rawObservation.getOptionPrice() < minimumOptionPrice)
			isExpensiveEnough = false;

		else
			isExpensiveEnough = true;

		return isExpensiveEnough;
		
	}

	private static boolean isOTM(KROptionTransaction rawObservation, Double sRaw, Double r, Double q) {
		
		boolean isOTM;
		String cpFlag = rawObservation.getIsCall();
		
		LocalDate t = rawObservation.getTransactionDate(),
			      T = rawObservation.getMaturityDate();
		
		Double tau = Double.valueOf(ChronoUnit.DAYS.between(t, T)) / 365d,
			   k = rawObservation.getStrikePrice(),
			   s = sRaw * Math.exp(-tau * q);
		
		if (cpFlag.equals("CALL") && k > s)
			isOTM = true;

		else if (cpFlag.equals("P") && k < s)
			isOTM = true;

		else
			isOTM = false;

		return isOTM;
		
	}

	private static Double getIndexValue(String minute) {
		
		for (int i = 0; i < indexObservationList.size(); i++) {
			
			KRIndexObservation indexObservation = indexObservationList.get(i);
			if (indexObservation.getMinute().equals(minute))
				return indexObservation.getValue();
			
		}
		
		return null;
		
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
