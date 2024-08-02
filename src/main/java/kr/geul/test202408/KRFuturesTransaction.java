package kr.geul.test202408;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class KRFuturesTransaction implements Comparable {

	public boolean isIndexFutures;
	int tradingVolume;
	double futuresPrice;
	LocalDate maturityDate;
	LocalDateTime transactionDateTime;

	public KRFuturesTransaction() {
	}

	public KRFuturesTransaction(String rawString) {

		String[] rawData = rawString.split("\\|");

		futuresPrice = Double.parseDouble(rawData[4]);
		tradingVolume = Integer.parseInt(rawData[5]);

		String isIndexFuturesString = rawData[2].substring(3, 6);

		if (isIndexFuturesString.equals("101"))
			isIndexFutures = true;
		else
			isIndexFutures = false;

		int maturityYear = getMaturityYear(rawData[2].substring(6, 7)); 
		int maturityMonth = (int) rawData[2].substring(7, 8).charAt(0) - 48;

		if (maturityMonth > 9)
			maturityMonth -= 7;

		maturityDate = getMaturityDate(maturityYear, maturityMonth);

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

	public KRFuturesTransaction(String[] rawData) {

		DateTimeFormatter formatterTransaction = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
				formatterMaturity = DateTimeFormatter.ofPattern("yyyy-MM-dd");

		if (rawData.length == 4) {

			if (rawData[0].length() < 2) 
				transactionDateTime = null;
			else 
				transactionDateTime = LocalDateTime.parse(rawData[0].trim(), formatterTransaction);
			if (rawData[1].length() < 2) 
				maturityDate = null;
			else
				maturityDate = LocalDate.parse(rawData[1].trim(), formatterMaturity);

			isIndexFutures = true;
			futuresPrice = Double.parseDouble(rawData[2]);
			tradingVolume = Integer.parseInt(rawData[3]);

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

	public String getDate() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		String formattedDateTime = transactionDateTime.format(formatter);
		return formattedDateTime;
	}

	public String getMinute() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm");
		String formattedDateTime = transactionDateTime.format(formatter);
		return formattedDateTime;
	}
	
	public String toString() {

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
				formatterMaturity = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		String formattedDateTime = transactionDateTime.format(formatter),
				formattedMaturityDate = maturityDate.format(formatterMaturity);

		return formattedDateTime + "," + formattedMaturityDate + "," + futuresPrice + "," + tradingVolume;

	}

	public String[] toStringArray() {

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
						  formatterMaturity = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		String formattedDateTime = transactionDateTime.format(formatter),
			   formattedMaturityDate = maturityDate.format(formatterMaturity);
		
		String[] array = {formattedDateTime, formattedMaturityDate, futuresPrice + "", tradingVolume + ""};
		return array;
		
	}

	public LocalDate getTransactionDate() {
		return LocalDate.of(transactionDateTime.getYear(), transactionDateTime.getMonth(), transactionDateTime.getDayOfMonth());
	}
	
	public LocalDateTime getTransactionDateTime() { 
		return transactionDateTime;
	}

	public LocalDate getMaturityDate() { 
		return maturityDate;
	}

	public double getFuturesPrice() {
		return futuresPrice;
	}
	
	public void setFuturesPrice(double futuresPrice) {
		this.futuresPrice = futuresPrice;
	}
	
	public void setMaturityDate(LocalDate maturityDate) {
		this.maturityDate = maturityDate;
	}
	
	public void setTransactionDateTime(LocalDateTime transactionDateTime) {
		this.transactionDateTime = transactionDateTime;
	}
	
	public void setTradingVolume(int tradingVolume) {
		this.tradingVolume = tradingVolume;
	}
	
	public int compareTo(KRFuturesTransaction otherTransaction) {

		if (maturityDate.toEpochDay() > otherTransaction.getMaturityDate().toEpochDay()) 
			return 1;
		else if (maturityDate.toEpochDay() < otherTransaction.getMaturityDate().toEpochDay())
			return -1;
		else {

			if (transactionDateTime.atZone(ZoneOffset.UTC).toInstant().toEpochMilli() > otherTransaction.getTransactionDateTime().atZone(ZoneOffset.UTC).toInstant().toEpochMilli())
				return 1;
			else if (transactionDateTime.atZone(ZoneOffset.UTC).toInstant().toEpochMilli() < otherTransaction.getTransactionDateTime().atZone(ZoneOffset.UTC).toInstant().toEpochMilli())
				return -1;
			else
				return 0;

		}

	}

	@Override
	public int compareTo(Object o) {
		
		KRFuturesTransaction otherTransaction = (KRFuturesTransaction) o;
		
		if (maturityDate.toEpochDay() > otherTransaction.getMaturityDate().toEpochDay()) 
			return 1;
		else if (maturityDate.toEpochDay() < otherTransaction.getMaturityDate().toEpochDay())
			return -1;
		else {

			if (transactionDateTime.atZone(ZoneOffset.UTC).toInstant().toEpochMilli() > otherTransaction.getTransactionDateTime().atZone(ZoneOffset.UTC).toInstant().toEpochMilli())
				return 1;
			else if (transactionDateTime.atZone(ZoneOffset.UTC).toInstant().toEpochMilli() < otherTransaction.getTransactionDateTime().atZone(ZoneOffset.UTC).toInstant().toEpochMilli())
				return -1;
			else
				return 0;

		}
		
	}

}