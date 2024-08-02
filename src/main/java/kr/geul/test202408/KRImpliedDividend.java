package kr.geul.test202408;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class KRImpliedDividend {

	double impliedDividend, underlying, rfr, tau, lnMoneyness;
	LocalDateTime dateTime;
	
	public KRImpliedDividend() {
	}
	
	public KRImpliedDividend(String[] rawArray) {
		
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"),
		  		  		  timeFormatter = DateTimeFormatter.ofPattern("Hmm");
		LocalDate temporaryDate = LocalDate.parse(rawArray[0], dateFormatter);
		LocalTime temporaryTime = LocalTime.parse(rawArray[1], timeFormatter);
		
		dateTime = LocalDateTime.of(temporaryDate, temporaryTime);
		impliedDividend = Double.parseDouble(rawArray[2]);
		rfr = Double.parseDouble(rawArray[3]);
		underlying = Double.parseDouble(rawArray[4]);
		lnMoneyness = Double.parseDouble(rawArray[5]);
		
	}

	public LocalDateTime getDateTime() {
		return dateTime;
	}
	
	public double getLnMoneyness() {
		return lnMoneyness;
	}
	
	public Double getRfr() {
		return rfr;
	}
	
	public Double getImpliedDividend() {
		return impliedDividend;
	}
	
	public Double getUnderlying() {
		return underlying;
	}
	
	public void setDateTime(LocalDateTime dateTime) {
		this.dateTime = dateTime;
	}

	public void setImpliedDividend(double impliedDividend) {
		this.impliedDividend = impliedDividend;
	}	

	public void setLnMoneyness(double lnMoneyness) {
		this.lnMoneyness = lnMoneyness;
	}
	
	public void setRfr(double rfr) {
		this.rfr = rfr;
	}	

	public void setTau(double tau) {
		this.tau = tau;
	}	
	
	public void setUnderlying(double underlying) {
		this.underlying = underlying;
	}	
	
	public String[] toStringArray() {

		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"),
						  timeFormatter = DateTimeFormatter.ofPattern("Hmm");
		String formattedDate = dateTime.format(dateFormatter),
			   formattedTime = dateTime.format(timeFormatter);
		
		String[] array = {formattedDate, formattedTime, impliedDividend + "", rfr + "", underlying + "", tau + "", lnMoneyness + ""};
		return array;
		
	}
	
}
