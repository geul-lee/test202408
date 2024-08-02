package kr.geul.test202408;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class KRKoriborObservation {

	static final Double[] maturities = {7d, 365d / 12d, 365d / 6d, 365d / 4d, 365d / 2d, 365d};
	
	Double[] rates;
	LocalDate date;
	
	public KRKoriborObservation(String[] rawArray) {
		
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		date = LocalDate.parse(rawArray[0], dateFormatter);
		rates = new Double[6];
		
		for (int i = 0; i < 6; i++) {
			rates[i] = Double.parseDouble(rawArray[i + 1]);
		}
		
	}

	public String getDate() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		String formattedDateTime = date.format(formatter);
		return formattedDateTime;
	}

	public Double getRate(Double tau) {
		
		if (tau < 7d)
			return rates[0];
		else if (tau > 365d)
			return rates[5];
		else {
			
			for (int i = 0; i < 5; i++) {
				
				if (tau >= maturities[i] && tau <= maturities [i+1]) 
					return rates[i] + (rates[i + 1] - rates[i]) * ((tau - maturities[i]) / (maturities[i + 1] - maturities[i]));
				
			}
			
			return null;
			
		}
		
	}
	
}
