package kr.geul.test202408;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

public class GetChanges_excessDividend_thirdNearestMaturity {

	static ArrayList<KRImpliedDividend> dayTImpliedDividendList, dayTMinusOneImpliedDividendList;
	static ArrayList<String[]> dividendChangesList = new ArrayList<String[]>();
	static File sourceFolder = new File("c:/test/impliedDividend_thirdNearestMaturity"), targetFile = new File("c:/test/changes_excessDividend_thirdNearestMaturity.csv"), dayTFile, dayTMinusOneFile;	
	static File[] sourceFiles = sourceFolder.listFiles();
	static double daytimeChange, overnightChange, daytimeChange_underlying, overnightChange_underlying, rfr, rfrTMinusOne, rfrChange, lnMoneyness, lnMoneynessChange, dayDifference; 
	
	public static void run() throws CsvValidationException {
		
		for (int i = 1; i < sourceFiles.length; i++) {
			
			dayTFile = sourceFiles[i]; 
			dayTMinusOneFile = sourceFiles[i - 1];
			readImpliedDividends();
			
			if (dayTImpliedDividendList.size() > 0 && dayTMinusOneImpliedDividendList.size() > 0) {

				getChanges();
				String[] dividendChanges = {dayTImpliedDividendList.get(0).toStringArray()[0], daytimeChange + "", overnightChange + "", daytimeChange_underlying + "", overnightChange_underlying + "", rfr + "", rfrChange + "", lnMoneyness + "", lnMoneynessChange + ""};			
				dividendChangesList.add(dividendChanges);
				
			}
						
		}
		
		writeFile();
		
	}

	private static void writeFile() {
		
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile))) {

			StringBuilder sbFirstLine = new StringBuilder();
			sbFirstLine.append("date,daytime,overnight,daytimeS,overnightS,rfr,rfrchange,lnmoneyness,lnmoneynesschange");
			writer.write(sbFirstLine.toString());
			writer.newLine();
			
			for (String[] row : dividendChangesList) {
				
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

			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	private static void getChanges() {
		
		System.out.println(dayTFile.getName());
		System.out.println(dayTImpliedDividendList.size());
		
		rfr = dayTImpliedDividendList.get(0).getRfr();
		rfrTMinusOne = dayTMinusOneImpliedDividendList.get(0).getRfr();
		rfrChange = rfr - rfrTMinusOne;
		lnMoneyness = dayTImpliedDividendList.get(0).getLnMoneyness();
		lnMoneynessChange = dayTImpliedDividendList.get(0).getLnMoneyness() - dayTMinusOneImpliedDividendList.get(0).getLnMoneyness();
		
		Double tOpen = dayTImpliedDividendList.get(0).getImpliedDividend() - rfr,
			   tOpen_underlying = dayTImpliedDividendList.get(0).getUnderlying(),
			   tClose = dayTImpliedDividendList.get(dayTImpliedDividendList.size() - 1).getImpliedDividend() - rfr,
			   tClose_underlying = dayTImpliedDividendList.get(dayTImpliedDividendList.size() - 1).getUnderlying(),
			   tMinusOneClose = dayTMinusOneImpliedDividendList.get(dayTMinusOneImpliedDividendList.size() - 1).getImpliedDividend() - rfrTMinusOne,
			   tMinusOneClose_underlying = dayTMinusOneImpliedDividendList.get(dayTMinusOneImpliedDividendList.size() - 1).getUnderlying();
		
		dayDifference = ChronoUnit.DAYS.between(dayTMinusOneImpliedDividendList.get(0).getDateTime(), dayTImpliedDividendList.get(0).getDateTime());
		
		daytimeChange = tClose - tOpen;
		daytimeChange_underlying = (tClose_underlying / tOpen_underlying) - 1;
		overnightChange = tOpen - tMinusOneClose;		
		overnightChange_underlying = (tOpen_underlying / tMinusOneClose_underlying) - 1 - (rfrTMinusOne * (dayDifference / 365));
				
	}

	private static void readImpliedDividends() throws CsvValidationException {
		
		dayTImpliedDividendList = new ArrayList<KRImpliedDividend>();
		dayTMinusOneImpliedDividendList = new ArrayList<KRImpliedDividend>();
		
		try (CSVReader reader = new CSVReader(new FileReader(dayTFile))) {

			String[] nextLine;
			
			int jj = 0;
			
			while ((nextLine = reader.readNext()) != null) {
				dayTImpliedDividendList.add(new KRImpliedDividend(nextLine));
			}
						
		} catch (IOException e) {
			e.printStackTrace(); 
		}		

		try (CSVReader reader = new CSVReader(new FileReader(dayTMinusOneFile))) {

			String[] nextLine;
			
			while ((nextLine = reader.readNext()) != null) {
				dayTMinusOneImpliedDividendList.add(new KRImpliedDividend(nextLine)); 				
			}
			
		} catch (IOException e) {
			e.printStackTrace(); 
		}		
		
	}
	
}
