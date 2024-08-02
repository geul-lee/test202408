package kr.geul.test202408;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

public class GetOneMinuteFile {

	static ArrayList<KROptionTransaction> rawObservationList, filteredObservationList, augmentedObservationList, finalObservationList;
	static String csvFilePath_targetFolder = "e:/KOSPI200/1minute/";

	static File sourceFolder = new File("e:/KOSPI200/daily_sorted"), targetFile;
	static File[] sourceFiles = sourceFolder.listFiles();

	public static void run() throws CsvValidationException, FileNotFoundException, IOException {

		for (File file : sourceFiles) {

			readFile(file);
			modifyObservationList();
			writeFile(file);
			System.out.println(file.getName());

		}

	}

	private static void writeFile(File file) {

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

	private static void modifyObservationList() {
		removeRedundantObservations();	
		augmentObservations();
		fillEnds();
	}

	private static void fillEnds() {
		
		finalObservationList = new ArrayList<KROptionTransaction>();
		
		for (int i = 0; i < augmentedObservationList.size() - 1; i++) {
			
			KROptionTransaction currentObservation = augmentedObservationList.get(i),
					nextObservation = augmentedObservationList.get(i + 1);
			
			finalObservationList.add(currentObservation);
			
			if (currentObservation.getStrikePrice() - nextObservation.getStrikePrice() != 0
					|| currentObservation.getIsCall().equals(nextObservation.getIsCall()) == false
					|| currentObservation.getMaturityDate().equals(nextObservation.getMaturityDate()) == false) {
				
				System.out.println(currentObservation.getStrikePrice() + "," + nextObservation.getStrikePrice() + "," + currentObservation.getIsCall() + "," + nextObservation.getIsCall());
				
				LocalDateTime currentTime = currentObservation.getTransactionDateTime(),
						currentEndTime = LocalDateTime.of(currentTime.getYear(), currentTime.getMonth(), currentTime.getDayOfMonth(), 15, 30, 0, 0);
				
				Instant currentInstant = currentTime.toInstant(ZoneOffset.UTC), currentEndInstant = currentEndTime.toInstant(ZoneOffset.UTC);
				long minutesDifference = Duration.between(currentInstant, currentEndInstant).toMinutes();
				
				if (minutesDifference > 0) {
					
					for (int j = 1; j < minutesDifference + 1; j++) {
						
						KROptionTransaction extrapolatedObservation = new KROptionTransaction();
						
		        		LocalDateTime extrapolatedTime = currentTime.plusMinutes(j);
		        		extrapolatedObservation.setTransactionDateTime(extrapolatedTime);
		        		extrapolatedObservation.setIsCall(currentObservation.getIsCall());
		        		extrapolatedObservation.setMaturityDate(currentObservation.getMaturityDate());
		        		extrapolatedObservation.setStrikePrice(currentObservation.getStrikePrice());
		        		extrapolatedObservation.setOptionPrice(currentObservation.getOptionPrice());
		        		extrapolatedObservation.setTradingVolume(0);
		        		
		        		finalObservationList.add(extrapolatedObservation);
		        		
					}
					
				}
				
			}
			
		}
		
	}

	private static void augmentObservations() {
		
		augmentedObservationList = new ArrayList<KROptionTransaction>();

		for (int i = 0; i < filteredObservationList.size() - 1; i++) {

			KROptionTransaction currentObservation = filteredObservationList.get(i),
					nextObservation = filteredObservationList.get(i + 1);

			LocalDateTime currentTime = currentObservation.getTransactionDateTime(),
						  currentTrimmedTime = LocalDateTime.of(currentTime.getYear(), currentTime.getMonth(), currentTime.getDayOfMonth(), currentTime.getHour(), currentTime.getMinute(), 0, 0);

			currentObservation.setTransactionDateTime(currentTrimmedTime);
			
			augmentedObservationList.add(currentObservation);
			
			if (currentObservation.getStrikePrice() - nextObservation.getStrikePrice() == 0
					&& currentObservation.getIsCall().equals(nextObservation.getIsCall())
					&& currentObservation.getMaturityDate().equals(nextObservation.getMaturityDate())) {
				
				LocalDateTime nextTime = nextObservation.getTransactionDateTime(),
							  nextTrimmedTime = LocalDateTime.of(nextTime.getYear(), nextTime.getMonth(), nextTime.getDayOfMonth(), nextTime.getHour(), nextTime.getMinute(), 0, 0);
		        Instant currentInstant = currentTrimmedTime.toInstant(ZoneOffset.UTC), nextInstant = nextTrimmedTime.toInstant(ZoneOffset.UTC);
		        long minutesDifference = Duration.between(currentInstant, nextInstant).toMinutes();
		        
		        if (minutesDifference > 1) {
		        	
		        	for (int j = 1; j < minutesDifference; j++) {
						
		        		KROptionTransaction interpolatedObservation = new KROptionTransaction();
		        		
		        		LocalDateTime interpolatedTime = currentTrimmedTime.plusMinutes(j);
		        		interpolatedObservation.setTransactionDateTime(interpolatedTime);
		        		interpolatedObservation.setIsCall(currentObservation.getIsCall());
		        		interpolatedObservation.setMaturityDate(currentObservation.getMaturityDate());
		        		interpolatedObservation.setStrikePrice(currentObservation.getStrikePrice());
		        		interpolatedObservation.setOptionPrice(currentObservation.getOptionPrice());
		        		interpolatedObservation.setTradingVolume(0);
		        		
		        		augmentedObservationList.add(interpolatedObservation);
		        		
					}
		        	
		        }
		        				
			}

		}

	}

	private static void removeRedundantObservations() {

		filteredObservationList = new ArrayList<KROptionTransaction>();

		for (int i = 0; i < rawObservationList.size() - 1; i++) {

			KROptionTransaction currentObservation = rawObservationList.get(i),
					nextObservation = rawObservationList.get(i + 1);

			LocalDateTime currentTime = currentObservation.getTransactionDateTime(),
					nextTime = nextObservation.getTransactionDateTime();

			if ((currentObservation.getIsCall().equals(nextObservation.getIsCall()) == false 
					|| currentObservation.getMaturityDate().equals(nextObservation.getMaturityDate()) == false 
					|| currentObservation.getStrikePrice() - nextObservation.getStrikePrice() != 0 
					|| currentTime.getHour() - nextTime.getHour() != 0
					|| currentTime.getMinute() - nextTime.getMinute() != 0)
					&& currentTime.getHour() > 8 
					&& (currentTime.getHour() < 15 || (currentTime.getHour() == 15 && currentTime.getMinute() <= 30)))
				filteredObservationList.add(currentObservation);

		}

	}

	private static void readFile(File file) throws FileNotFoundException, IOException, CsvValidationException {

		try (CSVReader reader = new CSVReader(new FileReader(file))) {

			rawObservationList = new ArrayList<KROptionTransaction>();

			String[] nextLine;

			while ((nextLine = reader.readNext()) != null) {

				KROptionTransaction observation = new KROptionTransaction(nextLine);

				if (observation.getTransactionDateTime() != null && observation.getMaturityDate() != null)
					rawObservationList.add(observation);				

			}

		}

	}

}
