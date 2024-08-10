package kr.geul.test202408;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

public class MergeEstimateData {

	static File estimateFolder = new File("e:/KOSPI200/estimates"), mergedFile = new File("e:/KOSPI200/estimates.csv");
	static File[] estimateFiles = estimateFolder.listFiles();
	static ArrayList<String[]> estimateList;
	
	public static void run() throws CsvValidationException {
		
		estimateList = new ArrayList<String[]>();
	
		for (File estimateFile : estimateFiles) {
			
			System.out.println(estimateFile.getName());
			
			try (CSVReader reader = new CSVReader(new FileReader(estimateFile))) {

				String[] nextLine;

				while ((nextLine = reader.readNext()) != null) 
					estimateList.add(nextLine);

			} catch (IOException e) {
				e.printStackTrace(); 
			}
			
		}
		
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(mergedFile))) {

			writer.write("date,s,tau_1,vol_raw_1,skew_raw_1,kurt_raw_1,vol_raw_le_1,skew_raw_le_1,kurt_raw_le_1,vol_25_1,skew_25_1,kurt_25_1,vol_25_le_1,skew_25_le_1,kurt_25_le_1,vol_50_1,skew_50_1,kurt_50_1,vol_50_le_1,skew_50_le_1,kurt_50_le_1,vol_75_1,skew_75_1,kurt_75_1,vol_75_le_1,skew_75_le_1,kurt_75_le_1,vol_90_1,skew_90_1,kurt_90_1,vol_90_le_1,skew_90_le_1,kurt_90_le_1,vol_99_1,skew_99_1,kurt_99_1,vol_99_le_1,skew_99_le_1,kurt_99_le_1,tau_2,vol_raw_2,skew_raw_2,kurt_raw_2,vol_raw_le_2,skew_raw_le_2,kurt_raw_le_2,vol_25_2,skew_25_2,kurt_25_2,vol_25_le_2,skew_25_le_2,kurt_25_le_2,vol_50_2,skew_50_2,kurt_50_2,vol_50_le_2,skew_50_le_2,kurt_50_le_2,vol_75_2,skew_75_2,kurt_75_2,vol_75_le_2,skew_75_le_2,kurt_75_le_2,vol_90_2,skew_90_2,kurt_90_2,vol_90_le_2,skew_90_le_2,kurt_90_le_2,vol_99_2,skew_99_2,kurt_99_2,vol_99_le_2,skew_99_le_2,kurt_99_le_2,tauk_1,kmin_raw_1,kmax_raw_1,kmin_25_1,kmax_25_1,kmin_50_1,kmax_50_1,kmin_75_1,kmax_75_1,kmin_90_1,kmax_90_1,kmin_99_1,kmax_99_1,tauk_2,kmin_raw_2,kmax_raw_2,kmin_25_2,kmax_25_2,kmin_50_2,kmax_50_2,kmin_75_2,kmax_75_2,kmin_90_2,kmax_90_2,kmin_99_2,kmax_99_2");
			writer.newLine();
			
			for (int i = 0; i < estimateList.size(); i++) {
				
				String[] estimates = estimateList.get(i);
				
				StringBuilder sb = new StringBuilder();

				for (int j = 0; j < estimates.length; j++) {

					sb.append(estimates[j]);
					if (j < estimates.length - 1) {
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
	
}
