package kr.geul.test202408;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class KRRfrObservation {

	LocalDate date;
	double rfr;
	
	public KRRfrObservation(String[] rawArray) {
		
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
		date = LocalDate.parse(rawArray[0], dateFormatter);
		rfr = Double.parseDouble(rawArray[3]);
				
	}

	public String getDate() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		String formattedDateTime = date.format(formatter);
		return formattedDateTime;
	}

	public double getRate() { 
		return rfr;
	}
	
}
