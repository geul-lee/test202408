package kr.geul.test202408;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

public class GetImpliedDividend_futures {
	
	static ArrayList<Integer> dividendMaturities;
	static ArrayList<Double> dividendRates;
	
	static ArrayList<KRFuturesTransaction> futuresTransactionList;
	static ArrayList<KRIndexObservation> indexObservationList;
	static ArrayList<KRKoriborObservation> koriborObservationList;
	static ArrayList<KROptionTransaction> optionTransactionList;
	static File indexObservationFile, koriborObservationFile = new File("e:/koribor.csv"), futuresFolder = new File("e:/KOSPI200f/closing"), optionsFolder = new File("e:/KOSPI200/closing");
	static File[] futuresFiles = futuresFolder.listFiles();
	
	public static void run() throws CsvValidationException {
		
		setKoribor();
		
		for (File file : futuresFiles) {
	
			System.out.println(file.getName());
			
			readFiles(file);
			getIndex(); 
			
			getDividend(file);	
			
		}
		
	}

	private static void getDividend(File file) {
		
		dividendMaturities = new ArrayList<Integer>();
		dividendRates = new ArrayList<Double>();
		
		KRKoriborObservation koriborObservation = getKoriborObservation(file);
		
		Double s = indexObservationList.get(0).getValue(), f, r, q, tau;
		
		for (int i = 0; i < futuresTransactionList.size(); i++) {
			
			KRFuturesTransaction futuresTransaction = futuresTransactionList.get(i);
			f = futuresTransaction.getFuturesPrice();
			
			LocalDate t = futuresTransaction.getTransactionDate(),
				      T = futuresTransaction.getMaturityDate();
			
			tau = Double.valueOf(ChronoUnit.DAYS.between(t, T));
			
			r = koriborObservation.getRate(tau) / 100d;
			
			q = r - (Math.log(f / s) / (tau / 365d));
			
			System.out.println(s + ", " + f + ", " + tau + ", " + r + ", " + q);			
					
		}
		
	}

	private static KRKoriborObservation getKoriborObservation(File file) {
		
		String date = file.getName().substring(0, 10);
		
		for (int i = 0; i < koriborObservationList.size(); i++) {
			
			KRKoriborObservation koriborObservation = koriborObservationList.get(i);
			
			if (koriborObservation.getDate().equals(date))
				return koriborObservation;
			
		}
		
		return null;
		
	}

	private static void getIndex() {
		
		String optionMinute = optionTransactionList.get(0).getMinute();
		
		for (int i = indexObservationList.size() - 1; i >= 0; i--) {
			
			String indexMinute = indexObservationList.get(i).getMinute();
			
			if (optionMinute.equals(indexMinute) == false)
				indexObservationList.remove(i);
			
		}
		
	}

	private static void readFiles(File file) throws CsvValidationException {
		
		futuresTransactionList = new ArrayList<KRFuturesTransaction>();
		indexObservationList = new ArrayList<KRIndexObservation>();
		optionTransactionList = new ArrayList<KROptionTransaction>();
		
		File futuresTransactionFile = file, indexObservationFile = new File("e:/KOSPI200/index/" + file.getName()), optionTransactionFile = new File("e:/KOSPI200/closing/" + file.getName()); 
		
		try (CSVReader reader = new CSVReader(new FileReader(futuresTransactionFile))) {

			String[] nextLine;

			while ((nextLine = reader.readNext()) != null) {
				futuresTransactionList.add(new KRFuturesTransaction(nextLine));					
			}
			
		} catch (IOException e) {
			e.printStackTrace(); 
		}
		
		try (CSVReader reader = new CSVReader(new FileReader(indexObservationFile))) {

			String[] nextLine;
			
			while ((nextLine = reader.readNext()) != null) {
				if (nextLine.length > 1)
					indexObservationList.add(new KRIndexObservation(nextLine));
			}
			
		} catch (IOException e) {
			e.printStackTrace(); 
		}
		
		try (CSVReader reader = new CSVReader(new FileReader(optionTransactionFile))) {

			String[] nextLine;

			while ((nextLine = reader.readNext()) != null) {
				optionTransactionList.add(new KROptionTransaction(nextLine));					
			}
			
		} catch (IOException e) {
			e.printStackTrace(); 
		}
		
	}


	private static void setKoribor() throws CsvValidationException {
		
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
