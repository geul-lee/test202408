package kr.geul.test202408;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

public class GetDailyFile {

	static BufferedWriter writer;
	
	public static void run() throws CsvValidationException {

		for (int i = 2010; i < 2011; i++) {

			for (int j = 1; j < 5; j++) {

				try (CSVReader reader = new CSVReader(new FileReader("e:/KOSPI200/DOKNXTRDSHRTH_" + i + "_Q" + j + ".dat"))) {

					String[] nextLine;
					String previousDate = "";			

					while ((nextLine = reader.readNext()) != null) {                

						KROptionTransaction option = new KROptionTransaction(nextLine[0]);
						String currentDate = option.getDate();

						if (previousDate.equals(currentDate) == false) {

							if (isOpen(writer)) 
								writer.close();

							writer = new BufferedWriter(new FileWriter("e:/KOSPI200/daily/" + currentDate + ".csv", true));
							System.out.println(currentDate);

						}							

						if (option.isCall.equals("OTHER") == false && option.isIndex)
							writer.append(option.toString() + "\n");

//						System.out.println(nextLine[0]);						
						previousDate = currentDate;

					}
					
					writer.close();

				} catch (IOException e) {
					e.printStackTrace(); 
				}					

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
