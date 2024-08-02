package kr.geul.test202408;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class KRIndexObservation {

	private LocalDateTime dateTime;
	private double indexValue;
	private long tradingVolume;
	
	public KRIndexObservation(String[] rawArray) {
		
		String dateString = rawArray[0], timeString = rawArray[1];
		int year = Integer.parseInt(dateString.substring(0, 4)),
				month = Integer.parseInt(dateString.substring(4, 6)),
				day = Integer.parseInt(dateString.substring(6, 8)),
				hour = timeString.length() == 4 ? Integer.parseInt(timeString.substring(0, 2)) : Integer.parseInt(timeString.substring(0, 1)),
				minute = timeString.length() == 4 ? Integer.parseInt(timeString.substring(2, 4)) : Integer.parseInt(timeString.substring(1, 3));
		dateTime = LocalDateTime.of(year, month, day, hour, minute, 0, 0);
		
		if (rawArray.length == 11) {
			
			indexValue = Double.parseDouble(rawArray[8]);
			tradingVolume = Long.parseLong(rawArray[10]);
			
		}
		
		else if (rawArray.length == 4) {
			
			indexValue = Double.parseDouble(rawArray[2]);
			tradingVolume = Long.parseLong(rawArray[3]);
			
		}
				
	}

	public LocalDate getDate() {
		return dateTime.toLocalDate();
	}

	public String getMinute() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
		String formattedDateTime = dateTime.format(formatter);
		return formattedDateTime;
	}

	public double getValue() {
		return indexValue;
	}
	
	public String toString() {
		
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd"),
						  timeFormatter = DateTimeFormatter.ofPattern("Hmm");
		String formattedDate = dateTime.format(dateFormatter),
			   formattedTime = dateTime.format(timeFormatter);
		
		return formattedDate + "," + formattedTime + "," + indexValue + "," + tradingVolume;
		
	}
	
}
