package kr.geul.test202408;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;

public class KRImpliedDividend_jfec {

	double impliedDividend, underlying, rfr, tau, lnMoneyness;
	LocalDateTime dateTime;
	LocalDate maturity;
	
	public KRImpliedDividend_jfec() {
	}
	
	public KRImpliedDividend_jfec(String[] rawArray) {
		
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"),
		  		  		  timeFormatter = DateTimeFormatter.ofPattern("Hmm");
		LocalDate temporaryDate = LocalDate.parse(rawArray[0], dateFormatter),
				  maturityDate = LocalDate.parse(rawArray[2], dateFormatter);
		LocalTime temporaryTime = LocalTime.parse(rawArray[1], timeFormatter);
		
		dateTime = LocalDateTime.of(temporaryDate, temporaryTime);
		maturity = maturityDate;
		impliedDividend = Double.parseDouble(rawArray[3]);
		rfr = Double.parseDouble(rawArray[4]);
		underlying = Double.parseDouble(rawArray[5]);
		lnMoneyness = Double.parseDouble(rawArray[6]);
		
	}

	private LocalDate getDate() {
		return LocalDate.of(dateTime.getYear(), dateTime.getMonth(), dateTime.getDayOfMonth());
	}
	
	public LocalDateTime getDateTime() {
		return dateTime;
	}
	
	public double getLnMoneyness() {
		return lnMoneyness;
	}
	
	public LocalDate getMaturity() {
		return maturity;
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

	public void setMaturity(LocalDate maturityLocalDate) {
		maturity = maturityLocalDate;
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
			   formattedTime = dateTime.format(timeFormatter),
			   formattedMaturity = maturity.format(dateFormatter);
		
		String[] array = {formattedDate, formattedTime, formattedMaturity, impliedDividend + "", rfr + "", underlying + "", tau + "", lnMoneyness + ""};
		return array;
		
	}

	public Integer getMaturityInTaus() {
		return (int) ChronoUnit.DAYS.between(getDate(), maturity);
	}
	
}
