package kr.geul.test202408;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException; 

public class GetImpliedDividend_secondNearestMaturity {

	static ArrayList<KRIndexObservation> indexObservationList;
	static ArrayList<KROptionTransaction> optionTransactionList;
	static ArrayList<KRImpliedDividend> impliedDividendList;
	static ArrayList<KRRfrObservation> rfrObservationList = new ArrayList<KRRfrObservation>();
	static File indexObservationFile, rfrObservationFile = new File("c:/test/rfr.csv"), sourceFolder = new File("c:/test/daily_1minute_sorted");
	static File[] sourceFiles = sourceFolder.listFiles();
	static double rfr;
	static String targetFolderPath = "c:/test/impliedDividend_secondNearestMaturity/";
	
	public static void run() throws CsvValidationException {

		setRfr();
		
		for (File file : sourceFiles) {
			
			readFile(file);
			System.out.println(file.getName());
			
			String targetMaturity = getTargetMaturity();
			filterByMaturity(targetMaturity);
			rfr = getRfr(file.getName().substring(0, 10));
			
			estimateImpliedDividend();
			writeFile(file);
			
		}
		
	}

	private static void writeFile(File file) {
		
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFolderPath + file.getName()))) {

			for (KRImpliedDividend impliedDividend : impliedDividendList) {
				
				if (impliedDividend.getDateTime() != null) {
					
					String[] row = impliedDividend.toStringArray();
					
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

	private static void estimateImpliedDividend() {
		
		impliedDividendList = new ArrayList<KRImpliedDividend>();
		
		for (int i = 0; i < indexObservationList.size(); i++) {
			
			KRIndexObservation index = indexObservationList.get(i);
			String minute = index.getMinute();
			
			ArrayList<KROptionTransaction> optionsList = getOneMinuteOptionsTransactionList(minute);
			KROptionTransaction[] options = getBestOptions(index.getValue(), optionsList);
			
			if (options[0] != null) {

				double S = index.getValue(),
					   K = options[0].getStrikePrice(),
					   C = options[0].getOptionPrice(),
					   P = options[1].getOptionPrice(),
					   tau = (double) (ChronoUnit.DAYS.between(options[0].getTransactionDate(), options[0].getMaturityDate())) / 365d,
					   minusErt = Math.exp(-1d * rfr * tau);
				
				double impliedDividendEstimate = (-1d / tau) * Math.log((C - P + (K * minusErt)) / S) ; 
						
				KRImpliedDividend impliedDividend = new KRImpliedDividend();
				impliedDividend.setDateTime(options[0].getTransactionDateTime());
				impliedDividend.setImpliedDividend(impliedDividendEstimate);
				impliedDividend.setRfr(rfr);
				impliedDividend.setUnderlying(S);
				impliedDividend.setTau(tau);
				impliedDividend.setLnMoneyness(Math.log(K / S));
				
				impliedDividendList.add(impliedDividend);
				
			}
			
		}
		
	}

	private static KROptionTransaction[] getBestOptions(double index, ArrayList<KROptionTransaction> optionsList) {
		
		double difference = 999999;
		
		KROptionTransaction[] bestOptions = new KROptionTransaction[2];
		
		for (int i = 0; i < optionsList.size(); i++) {

			KROptionTransaction call = optionsList.get(i);
						
			double strikePriceGap = Math.abs(call.getStrikePrice() - index); 
			
			if (strikePriceGap < difference && call.getIsCall().equals("CALL") && getPut(call, optionsList) != null) { 
				bestOptions[0] = call;
				bestOptions[1] = getPut(call, optionsList);
				difference = strikePriceGap;
			}
			
		}
		
		return bestOptions;
		
	}

	private static KROptionTransaction getPut(KROptionTransaction call, ArrayList<KROptionTransaction> optionsList) {
		
		double strike = call.getStrikePrice();
		
		for (int i = 0; i < optionsList.size(); i++) {
			
			KROptionTransaction put = optionsList.get(i);
			
			if (put.getStrikePrice() == strike && put.getIsCall().equals("PUT"))
				return put;
			
		}
		
		return null;
		
	}

	private static ArrayList<kr.geul.test202408.KROptionTransaction> getOneMinuteOptionsTransactionList(String minute) {

		ArrayList<KROptionTransaction> oneMinuteOptionsTransactionList = new ArrayList<KROptionTransaction>();
		
		for (int i = 0; i < optionTransactionList.size(); i++) {
			
			KROptionTransaction optionsTransaction = optionTransactionList.get(i);
			if (optionsTransaction.getMinute().equals(minute))
				oneMinuteOptionsTransactionList.add(optionsTransaction);			
			
		}
		
		return oneMinuteOptionsTransactionList;
		
	}

	private static double getRfr(String dateString) {
		
		for (int i = 0; i < rfrObservationList.size(); i++) {
			
			KRRfrObservation rfrObservation = rfrObservationList.get(i);
			if (rfrObservation.getDate().equals(dateString))
				return rfrObservation.getRate();
			
		}
		
		return 999999;
		
	}

	private static void filterByMaturity(String targetMaturity) {

		for (int i = optionTransactionList.size() - 1; i >= 0; i--) {
			
			KROptionTransaction optionsTransaction = optionTransactionList.get(i);
			
			if (optionsTransaction.getMaturityDate().toString().equals(targetMaturity) == false) 
				optionTransactionList.remove(i);
			
		}		
		
	}

	private static String getTargetMaturity() {
		
		ArrayList<LocalDate> maturityList = new ArrayList<LocalDate>();
		
		for (int i = 1; i < optionTransactionList.size(); i++) {
			
			if (i == 1 || optionTransactionList.get(i).getMaturityDate().equals(optionTransactionList.get(i - 1).getMaturityDate()) == false) 
				maturityList.add(optionTransactionList.get(i).getMaturityDate());
			
		}
		
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		
		if (ChronoUnit.DAYS.between(optionTransactionList.get(0).getTransactionDate(), maturityList.get(0)) >= 7)
			return maturityList.get(1).format(formatter);
		else
			return maturityList.get(2).format(formatter);
		
	}

	private static void readFile(File file) throws CsvValidationException {
		
		indexObservationList = new ArrayList<KRIndexObservation>();
		optionTransactionList = new ArrayList<KROptionTransaction>();
		
		File indexObservationFile = new File("c:/test/index/" + file.getName()), optionTransactionFile = file; 
		
		try (CSVReader reader = new CSVReader(new FileReader(indexObservationFile))) {

			String[] nextLine;
			
			while ((nextLine = reader.readNext()) != null) {
				if (nextLine.length == 4 && nextLine[1].equals("�帶��") == false && nextLine[1].equals("�����帶��") == false)
					indexObservationList.add(new KRIndexObservation(nextLine));
			}
			
		} catch (IOException e) {
			e.printStackTrace(); 
		}		
		
		try (CSVReader reader = new CSVReader(new FileReader(optionTransactionFile))) {

			String[] nextLine;

			while ((nextLine = reader.readNext()) != null) {
				if (nextLine[1].equals("�帶��") == false && nextLine[1].equals("�����帶��") == false)
					optionTransactionList.add(new KROptionTransaction(nextLine));
			}
			
		} catch (IOException e) {
			e.printStackTrace(); 
		}		
		
	}

	private static void setRfr() throws CsvValidationException {

		try (CSVReader reader = new CSVReader(new FileReader(rfrObservationFile))) {

			String[] nextLine;

			while ((nextLine = reader.readNext()) != null) {    
				if (nextLine[0].length() > 0)
					rfrObservationList.add(new KRRfrObservation(nextLine));
			}
		} catch (IOException e) {
			e.printStackTrace(); 
		}
		
	}

}
