package kr.geul.test202408;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class KROptionTransaction implements Comparable<KROptionTransaction> {

	boolean isIndex;
	int tradingVolume;
	double strikePrice, optionPrice;
	LocalDate maturityDate;
	LocalDateTime transactionDateTime;
	String isCall;
	
	public KROptionTransaction() {
	}
	
	public KROptionTransaction(String rawString) {
		
		String[] rawData = rawString.split("\\|");
	
		optionPrice = Double.parseDouble(rawData[4]);
		tradingVolume = Integer.parseInt(rawData[5]);

		String isCallString = rawData[2].substring(3, 4),
			   isIndexString = rawData[2].substring(4, 6);
		
		if (isCallString.equals("2"))
			isCall = "CALL";
		else if (isCallString.equals("3"))
			isCall = "PUT";
		else
			isCall = "OTHER";
		
		if (isIndexString.equals("01"))
			isIndex = true;
		else
			isIndex = false;
		
		int maturityYear = getMaturityYear(rawData[2].substring(6, 7)); 
		int maturityMonth = (int) rawData[2].substring(7, 8).charAt(0) - 48;
		
		if (maturityMonth > 9)
			maturityMonth -= 7;

		maturityDate = getMaturityDate(maturityYear, maturityMonth);
		
		strikePrice = Double.parseDouble(rawData[2].substring(8, 11));
		if (strikePrice % 5 > 0)
			strikePrice += 0.5; 
	
		String rawDate = rawData[7];
		String rawTime = rawData[8];
		
		int year = Integer.parseInt(rawDate.substring(0, 4)),
			month = Integer.parseInt(rawDate.substring(4, 6)),
			day = Integer.parseInt(rawDate.substring(6, 8)),
			hour = Integer.parseInt(rawTime.substring(0, 2)), 
			minute = Integer.parseInt(rawTime.substring(2, 4)),
			second = Integer.parseInt(rawTime.substring(4, 6)),
			millisecond = Integer.parseInt(rawTime.substring(6, 9));
		
		transactionDateTime = LocalDateTime.of(year, month, day, hour, minute, second, millisecond * 1000000);
			
	}

	public KROptionTransaction(String[] rawData) {
		
		DateTimeFormatter formatterTransaction = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
				  		  formatterMaturity = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		
		if (rawData.length == 6) {
			
			if (rawData[0].length() < 2) 
				transactionDateTime = null;
			else 
				transactionDateTime = LocalDateTime.parse(rawData[0].trim(), formatterTransaction);
			if (rawData[1].length() < 2) 
				maturityDate = null;
			else
				maturityDate = LocalDate.parse(rawData[1].trim(), formatterMaturity);
			
			isCall = rawData[2];
			strikePrice = Double.parseDouble(rawData[3]);
			optionPrice = Double.parseDouble(rawData[4]);
			tradingVolume = Integer.parseInt(rawData[5]);
			
		}
		
	}

	private int getMaturityYear(String rawString) {
		int rawMaturityYear = (int) rawString.charAt(0); 
		
		if (rawMaturityYear > 85)
			rawMaturityYear -= 3;
		else if (rawMaturityYear > 79)
			rawMaturityYear -= 2;
		else if (rawMaturityYear > 73)
			rawMaturityYear--;
			
		return rawMaturityYear + 1941;
	}

	private LocalDate getMaturityDate(int year, int month) {
		
		LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
		DayOfWeek dayOfWeek = firstDayOfMonth.getDayOfWeek();
		
		int daysToAdd = DayOfWeek.THURSDAY.getValue() - dayOfWeek.getValue() + (dayOfWeek.getValue() >= DayOfWeek.THURSDAY.getValue() ? 7 : 0);
		
        LocalDate secondThursday = firstDayOfMonth.plusDays(daysToAdd + 7);      
		
		return secondThursday;
		
	}
	
	public String toString() {
		
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
						  formatterMaturity = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		String formattedDateTime = transactionDateTime.format(formatter),
			   formattedMaturityDate = maturityDate.format(formatterMaturity);
		
		return formattedDateTime + "," + formattedMaturityDate + "," + isCall + "," + strikePrice + "," + optionPrice + "," + tradingVolume;
		
	}


	public String[] toStringArray() {

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
						  formatterMaturity = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		String formattedDateTime = transactionDateTime.format(formatter),
			   formattedMaturityDate = maturityDate.format(formatterMaturity);
		
		String[] array = {formattedDateTime, formattedMaturityDate, isCall, strikePrice + "", optionPrice + "", tradingVolume + ""};
		return array;
		
	}
	
	public String getDate() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		String formattedDateTime = transactionDateTime.format(formatter);
		return formattedDateTime;
	}

	public LocalDate getTransactionDate() {
		
		return LocalDate.of(transactionDateTime.getYear(), transactionDateTime.getMonth(), transactionDateTime.getDayOfMonth());
		
	}
	
	public String getMinute() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
		String formattedDateTime = transactionDateTime.format(formatter);
		return formattedDateTime;
	}
	
	@Override
	public int compareTo(KROptionTransaction otherTransaction) {
		
		if (maturityDate.toEpochDay() > otherTransaction.getMaturityDate().toEpochDay()) 
			return 1;
		else if (maturityDate.toEpochDay() < otherTransaction.getMaturityDate().toEpochDay())
			return -1;
		else {
			
			if (transactionDateTime.atZone(ZoneOffset.UTC).toInstant().toEpochMilli() > otherTransaction.getTransactionDateTime().atZone(ZoneOffset.UTC).toInstant().toEpochMilli())
				return 1;
			else if (transactionDateTime.atZone(ZoneOffset.UTC).toInstant().toEpochMilli() < otherTransaction.getTransactionDateTime().atZone(ZoneOffset.UTC).toInstant().toEpochMilli())
				return -1;
			else {
				
				if (isCall.equals("CALL") && otherTransaction.getIsCall().equals("PUT"))
					return 1;
				else if (isCall.equals("PUT") && otherTransaction.getIsCall().equals("CALL"))
					return -1;
				
				else {

					if (strikePrice > otherTransaction.getStrikePrice())
						return 1;
					else if (strikePrice < otherTransaction.getStrikePrice())
						return -1;
					else 
						return 0;
					
				}
				
			}
				
		}
		
	}

//	@Override
//	public int compareTo(KROptionTransaction otherTransaction) {
//		
//		if (maturityDate.toEpochDay() > otherTransaction.getMaturityDate().toEpochDay()) 
//			return 1;
//		else if (maturityDate.toEpochDay() < otherTransaction.getMaturityDate().toEpochDay())
//			return -1;
//		else {
//			
//			if (isCall.equals("CALL") && otherTransaction.getIsCall().equals("PUT"))
//				return 1;
//			else if (isCall.equals("PUT") && otherTransaction.getIsCall().equals("CALL"))
//				return -1;
//			
//			else {
//
//				if (strikePrice > otherTransaction.getStrikePrice())
//					return 1;
//				else if (strikePrice < otherTransaction.getStrikePrice())
//					return -1;
//				else {
//					
//					if (transactionDateTime.atZone(ZoneOffset.UTC).toInstant().toEpochMilli() > otherTransaction.getTransactionDateTime().atZone(ZoneOffset.UTC).toInstant().toEpochMilli())
//						return 1;
//					else if (transactionDateTime.atZone(ZoneOffset.UTC).toInstant().toEpochMilli() < otherTransaction.getTransactionDateTime().atZone(ZoneOffset.UTC).toInstant().toEpochMilli())
//						return -1;
//					else
//						return 0;
//					
//				}
//
//			}
//			
//		}
//		
//	}
	
	
	public LocalDateTime getTransactionDateTime() { 
		return transactionDateTime;
	}

	public String getIsCall() { 
		return isCall;
	}

	public double getStrikePrice() {
		return strikePrice;
	}

	public double getOptionPrice() {
		return optionPrice;
	}
	
	public LocalDate getMaturityDate() { 
		return maturityDate;
	}

	public void setIsCall(String isCall) {
		this.isCall = isCall;
	}
	
	public void setMaturityDate(LocalDate maturityDate) {
		this.maturityDate = maturityDate;
	}

	public void setStrikePrice(double strikePrice) {
		this.strikePrice = strikePrice;
	}

	public void setOptionPrice(double optionPrice) {
		this.optionPrice = optionPrice;
	}

	public void setTradingVolume(int tradingVolume) {
		this.tradingVolume = tradingVolume;
	}

	public void setTransactionDateTime(LocalDateTime transactionDateTime) {
		this.transactionDateTime = transactionDateTime;
	}
	
}
