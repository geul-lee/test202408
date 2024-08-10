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

public class GetImpliedDividend_koribor {

	static ArrayList<KRIndexObservation> indexObservationList;
	static ArrayList<KROptionTransaction> optionTransactionList;
	static ArrayList<KRImpliedDividend_jfec> impliedDividendList;
	static ArrayList<KRKoriborObservation> koriborObservationList;
	static File indexObservationFile, koriborObservationFile = new File("e:/koribor.csv"), sourceFolder = new File("e:/KOSPI200/closing");
	static File[] sourceFiles = sourceFolder.listFiles();
	static double rfr;
	static String targetFolderPath = "e:/KOSPI200/impdiv_options/";

	public static void run() throws CsvValidationException {

		setRfr();

		for (File file : sourceFiles) {

			readFile(file);
			System.out.println(file.getName()); 

			if (indexObservationList.size() > 0) {

				estimateImpliedDividend(file);
				writeFile(file);
				
			}

		}

	}

	private static void writeFile(File file) {

		if (impliedDividendList.size() > 0) {
			
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFolderPath + file.getName()))) {

				for (KRImpliedDividend_jfec impliedDividend : impliedDividendList) {

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

	}

	private static void estimateImpliedDividend(File file) {

		impliedDividendList = new ArrayList<KRImpliedDividend_jfec>();
		KRIndexObservation index = getIndexObservation(optionTransactionList.get(0).getMinute());

		if (index != null) {
		
			ArrayList<String> maturityList = getMaturityList();	

			for (int i = 0; i < maturityList.size(); i++) {

				ArrayList<KROptionTransaction> optionsList = getMaturitySpecificTransactionList(maturityList.get(i));
				KROptionTransaction[] options = getBestOptions(index.getValue(), optionsList);

				if (options[0] != null) {

					double S = index.getValue(),
	   					   K = options[0].getStrikePrice(),
						   C = options[0].getOptionPrice(),
						   P = options[1].getOptionPrice(),
						   tauInDays = ChronoUnit.DAYS.between(options[0].getTransactionDate(), options[0].getMaturityDate()),
						   tau = (double) (tauInDays) / 365d;

					rfr = getRfr(file, tauInDays);

					double minusErt = Math.exp(-1d * rfr * tau), 
						   impliedDividendEstimate = (-1d / tau) * Math.log((C - P + (K * minusErt)) / S) ; 

					KRImpliedDividend_jfec impliedDividend = new KRImpliedDividend_jfec();
					impliedDividend.setDateTime(options[0].getTransactionDateTime());
					impliedDividend.setImpliedDividend(impliedDividendEstimate);
					impliedDividend.setRfr(rfr);
					impliedDividend.setUnderlying(S);
					impliedDividend.setTau(tau);
					impliedDividend.setLnMoneyness(Math.log(K / S));
					impliedDividend.setMaturity(options[0].getMaturityDate());

					impliedDividendList.add(impliedDividend);

				}

			}
			
		}
		
	}

	private static Double getRfr(File file, double tauInDays) {

		KRKoriborObservation koriborObservation;
		String date = file.getName().substring(0, 10);

		for (int i = 0; i < koriborObservationList.size(); i++) {

			koriborObservation = koriborObservationList.get(i);
			
			if (koriborObservation.getDate().equals(date))
				return Math.log(1 + (koriborObservation.getRate(tauInDays) / 100d));

		}

		return null;

	}
	
	private static ArrayList<KROptionTransaction> getMaturitySpecificTransactionList(String maturity) {

		ArrayList<KROptionTransaction> oneMinuteOptionsTransactionList = new ArrayList<KROptionTransaction>();

		for (int i = 0; i < optionTransactionList.size(); i++) {

			KROptionTransaction optionsTransaction = optionTransactionList.get(i);
			if (optionsTransaction.getMaturityString().equals(maturity))
				oneMinuteOptionsTransactionList.add(optionsTransaction);			

		}

		return oneMinuteOptionsTransactionList;

	}

	private static KRIndexObservation getIndexObservation(String minute) {

		for (int i = 0; i < indexObservationList.size(); i++) {

			KRIndexObservation indexObservation = indexObservationList.get(i);
			if (indexObservation.getMinute().equals(minute))
				return indexObservation;

		}

		return null;

	}

	private static ArrayList<String> getMaturityList() {

		ArrayList<String> maturityList = new ArrayList<String>();

		for (int i = 0; i < optionTransactionList.size(); i++) {

			KROptionTransaction optionTransaction = optionTransactionList.get(i);
			String maturity = optionTransaction.getMaturityString();

			boolean doesExist = false;

			for (int j = 0; j < maturityList.size(); j++) {

				if (maturityList.get(j).equals(maturity)) {
					doesExist = true;
					break;
				}

			}

			if (doesExist == false)
				maturityList.add(maturity);

		}

		return maturityList;

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

	private static void readFile(File file) throws CsvValidationException {

		indexObservationList = new ArrayList<KRIndexObservation>();
		optionTransactionList = new ArrayList<KROptionTransaction>();

		File indexObservationFile = new File("e:/KOSPI200/index/" + file.getName()), optionTransactionFile = file; 

		if (indexObservationFile.exists()) {
			
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

	}

	private static void setRfr() throws CsvValidationException {

		koriborObservationList = new ArrayList<KRKoriborObservation>();

		try (CSVReader reader = new CSVReader(new FileReader(koriborObservationFile))) {

			String[] nextLine;

			while ((nextLine = reader.readNext()) != null) {    
				if (nextLine[0].length() > 0)
					koriborObservationList.add(new KRKoriborObservation(nextLine));
			}
		} catch (IOException e) {
			e.printStackTrace(); 
		}

	}

}
