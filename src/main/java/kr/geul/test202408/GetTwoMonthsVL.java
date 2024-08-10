package kr.geul.test202408;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
import org.apache.commons.math3.exception.TooManyEvaluationsException;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import kr.geul.bkm.BKMEstimator;
import kr.geul.options.exception.AtTheMoneyException;
import kr.geul.options.exception.DuplicateOptionsException;
import kr.geul.options.exception.InconsistentArgumentLengthException;
import kr.geul.options.exception.InconsistentOptionException;
import kr.geul.options.exception.InvalidArgumentException;
import kr.geul.options.option.CallOption;
import kr.geul.options.option.Option;
import kr.geul.options.option.PutOption;
import kr.geul.options.structure.OptionCurve;
import kr.geul.options.structure.OptionSurface;

public class GetTwoMonthsVL {
	
	static final double extrapolationMultiplier = 3.0,
			strikePriceGap = 0.1,
			precisionMultiplier = 1.0 / strikePriceGap;
	static final double[] targetTimeToMaturities = {2.0 / 12.0, 4.0 / 12.0};
	static final String[] variableNames = {"S", "K", "R", "T", "D", "C", "V", "delta"};

	static BKMEstimator bkmEstimator;
	
	static ArrayList<KRIndexObservation> indexList;
	static ArrayList<KROptionTransaction> optionList;
	static ArrayList<KRImpliedDividend_jfec> impliedDividendList;
	static ArrayList<KRKoriborObservation> koriborList;

	static double[] kMin, kMax, timeToMaturity;
	static File koriborFile = new File("e:/koribor.csv"), optionFolder = new File("e:/KOSPI200/closing_filtered");
	static File[] optionFiles = optionFolder.listFiles();
	static String targetFolderPath = "e:/KOSPI200/vl_endpoints/";
	
	static KRKoriborObservation koribor;
	
	static UnivariateFunction kMinFunction, kMaxFunction;	

	public static void run() throws CsvValidationException, InvalidArgumentException, InconsistentArgumentLengthException, AtTheMoneyException, InconsistentOptionException, DuplicateOptionsException, FileNotFoundException {

		bkmEstimator = new BKMEstimator();
		readKoriborFile();

		for (File optionFile : optionFiles) {

			String fileName = optionFile.getName();
			
			if (fileName.substring(0, 4).equals("2021") && fileName.substring(5, 7).equals("04") && new File(targetFolderPath + fileName).exists() == false) {
				
				System.out.println(fileName);
				readFiles(optionFile);
				koribor = getKoribor(optionFile);

				if (koribor != null && optionList.size() > 0) {

					OptionSurface optionSurface = createSurface();
					//				printInfo(optionSurface);

					if (optionSurface.getCurves().size() > 2) {

						getTruncationLocations(optionSurface);
						fixTimeToMaturities(optionSurface);
						
						if (optionSurface.getCurves().size() >= 1) {
							Double[][] rawEndpoints = getRawEndpoints(optionSurface);
							interpolate(optionSurface, true);
							extrapolate(optionSurface);
						
							double[][] bkmEstimates = getEstimate(optionSurface);
							Double[][] endpoints = getEndpoints(rawEndpoints, bkmEstimates, optionSurface);
							
							writeFile(optionFile, bkmEstimates, endpoints, optionSurface);
							
						}
						
					}

				}
				
			}

		}

	}

	private static void writeFile(File optionFile, double[][] bkmEstimates, Double[][] endpoints, OptionSurface optionSurface) {
		
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFolderPath + optionFile.getName()))) {

			OptionCurve firstCurve = optionSurface.getCurves().get(0);
			
			writer.write(firstCurve.get(0).getVariableArray()[2] + "," + bkmEstimates[0][0] + "," + endpoints[0][0] + "," + endpoints[0][1] + "," + endpoints[2][0] + "," + endpoints[2][1]);
			
			if (optionSurface.getCurves().size() > 1) {
				
				OptionCurve secondCurve = optionSurface.getCurves().get(1);
				writer.newLine();
				writer.write(secondCurve.get(0).getVariableArray()[2] + "," + bkmEstimates[1][0] + "," + endpoints[1][0] + "," + endpoints[1][1] + "," + endpoints[3][0] + "," + endpoints[3][1]);			
				
			}
			
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	private static Double[][] getEndpoints(Double[][] rawEndpoints, double[][] bkmEstimates, OptionSurface optionSurface) {
		
		Double[][] endpoints = new Double[4][2];
		Double firstMaturity = optionSurface.getCurves().get(0).getVariableArray()[1];
		
		endpoints[0][0] = rawEndpoints[0][0];
		endpoints[0][1] = rawEndpoints[0][1];	
		endpoints[2][0] = endpoints[0][0] * bkmEstimates[0][0] * Math.sqrt(firstMaturity);
		endpoints[2][1] = endpoints[0][1] * bkmEstimates[0][0] * Math.sqrt(firstMaturity);
		
		if (rawEndpoints[1][0] != null) {
			
			Double secondMaturity = optionSurface.getCurves().get(1).getVariableArray()[1];
			endpoints[1][0] = rawEndpoints[1][0];
			endpoints[1][1] = rawEndpoints[1][1];	
			endpoints[3][0] = endpoints[1][0] * bkmEstimates[1][0] * Math.sqrt(secondMaturity);
			endpoints[3][1] = endpoints[1][1] * bkmEstimates[1][0] * Math.sqrt(secondMaturity);
			
		}
		
		else {

			endpoints[1][0] = null;
			endpoints[1][1] = null;
			endpoints[3][0] = null;
			endpoints[3][1] = null;
			
		}
		
		return endpoints;
		
	}
	
	private static Double[][] getRawEndpoints(OptionSurface surface) {
		
		Double[][] endpoints = new Double[2][2];
		
		ArrayList<OptionCurve> curves = surface.getCurves();
		
		OptionCurve twoMonthsCurve = curves.get(0), fourMonthsCurve = null;
		
		endpoints[0][0] = Math.log(twoMonthsCurve.get(0).getStrikePrice() / twoMonthsCurve.get(0).getVariableArray()[0]);
		endpoints[0][1] = Math.log(twoMonthsCurve.get(twoMonthsCurve.size() - 1).getStrikePrice() / twoMonthsCurve.get(0).getVariableArray()[0]);
		endpoints[1][0] = null;
		endpoints[1][1] = null;
		
		if (curves.size() > 1) {
		
			fourMonthsCurve = curves.get(1);
			endpoints[1][0] = Math.log(fourMonthsCurve.get(0).getStrikePrice() / fourMonthsCurve.get(0).getVariableArray()[0]);
			endpoints[1][1] = Math.log(fourMonthsCurve.get(fourMonthsCurve.size() - 1).getStrikePrice() / fourMonthsCurve.get(0).getVariableArray()[0]);
			
		}
			
		return endpoints;
		
	}
	
	private static double[][] getEstimate(OptionSurface surface) throws DuplicateOptionsException, InconsistentOptionException {

		ArrayList<OptionCurve> curves = surface.getCurves();

		double[][] estimates = new double[curves.size()][3];

		for (int i = 0; i < estimates.length; i++) {
			OptionCurve curve = curves.get(i);
			bkmEstimator.setOptions(curve);
			estimates[i] = bkmEstimator.getEstimates();
		}

		return estimates;

	}
	
	private static void extrapolate(OptionSurface surface) throws TooManyEvaluationsException, InvalidArgumentException, 
	InconsistentArgumentLengthException, DuplicateOptionsException, InconsistentOptionException, 
	AtTheMoneyException, FileNotFoundException {
		
		double sharePrice = getIndexValue(optionList.get(0).getMinute()),
			kMin = Math.round(sharePrice / extrapolationMultiplier * precisionMultiplier) /	precisionMultiplier, 
			kMax = Math.round(sharePrice * extrapolationMultiplier * precisionMultiplier) /	precisionMultiplier;

		surface.setStrikePriceGap(strikePriceGap);
		surface.setExtrapolationRange(kMin, kMax);
		surface.extrapolate(precisionMultiplier);

		System.out.println("Extrapolation has been completed.");
		ArrayList<OptionCurve> curves = surface.getCurves();

		for (int i = 0; i < curves.size(); i++) {
			OptionCurve curve = curves.get(i);
			double[] strikePrices = curve.getStrikePrices();
			System.out.println("Volatility curve for T = " + curve.getVariableArray()[1] + ": " +
					strikePrices.length + " options, K_min: " + strikePrices[0] +
					", K_max: " + strikePrices[strikePrices.length - 1]);

		}

	}

	private static void interpolate(OptionSurface surface, boolean isATMOptionsGenerated) throws InvalidArgumentException, InconsistentArgumentLengthException, 
	AtTheMoneyException, DuplicateOptionsException, InconsistentOptionException, 
	FileNotFoundException {

		surface.setStrikePriceGap(strikePriceGap);
		surface.interpolate(precisionMultiplier, isATMOptionsGenerated);

	}

	private static void fixTimeToMaturities(OptionSurface surface) throws FileNotFoundException, InvalidArgumentException, InconsistentArgumentLengthException, AtTheMoneyException, DuplicateOptionsException, InconsistentOptionException {

		surface.fixTimeToMaturities(targetTimeToMaturities, kMinFunction, kMaxFunction,
				precisionMultiplier);

		System.out.println("Time to maturities have been fixed.");
		ArrayList<OptionCurve> curves = surface.getCurves();

		for (int i = 0; i < curves.size(); i++) {

			OptionCurve curve = curves.get(i);
			double[] strikePrices = curve.getStrikePrices();
			System.out.println("Volatility curve for T = " + curve.getVariableArray()[1] + ": " +
					strikePrices.length + " options, K_min: " + strikePrices[0] +
					", K_max: " + strikePrices[strikePrices.length - 1]);

		}

	}

	private static void getTruncationLocations(OptionSurface surface) throws FileNotFoundException, InvalidArgumentException {

		ArrayList<OptionCurve> curves = surface.getCurves();
		timeToMaturity = getTimeToMaturities(curves);
		kMin = new double[curves.size()];
		kMax = new double[curves.size()]; 

		for (int i = 0; i < curves.size(); i++) {

			OptionCurve curve = curves.get(i);

			// Getting truncation locations
			double[] strikePrices = curve.getStrikePrices();
			kMin[i] = Math.round((double) strikePrices[0] * precisionMultiplier) / precisionMultiplier;
			kMax[i] = Math.round((double) strikePrices[strikePrices.length - 1] * precisionMultiplier) / precisionMultiplier;
			System.out.println("Volatility curve for T = " + curve.getVariableArray()[1] + ": " +
					strikePrices.length + " options, K_min: " + kMin[i] +
					", K_max: " + kMax[i]);

		}

		UnivariateInterpolator interpolator = new LinearInterpolator();
		kMinFunction = interpolator.interpolate(timeToMaturity, kMin);
		kMaxFunction = interpolator.interpolate(timeToMaturity, kMax);

	}

	private static double[] getTimeToMaturities(ArrayList<OptionCurve> curves) {

		ArrayList<Double> ttmArray = new ArrayList<Double>();

		for (int i = 0; i < curves.size(); i++) {
			OptionCurve curve = curves.get(i);
			ttmArray.add(curve.getVariableArray()[1]);
		}

		double[] ttm = new double[ttmArray.size()];

		for (int i = 0; i < ttm.length; i++) {
			ttm[i] = ttmArray.get(i);
		}

		return ttm;

	}

	private static void printInfo(OptionSurface optionSurface) throws DuplicateOptionsException, InconsistentOptionException {

		ArrayList<OptionCurve> curves = optionSurface.getCurves();

		for (int i = 0; i < curves.size(); i++) {

			OptionCurve curve = curves.get(i);
			double[] variableArray = curve.getVariableArray();
			System.out.print("[" + variableArray[1] + "," + variableArray[2] + "," + variableArray[3] + "] "
					+ curve.getCallCurve().size() + ", " + curve.getPutCurve().size() + " // ");

		}

		System.out.println("");

	}

	private static OptionSurface createSurface() throws InvalidArgumentException, InconsistentArgumentLengthException, AtTheMoneyException, InconsistentOptionException, DuplicateOptionsException {

		OptionSurface surface = new OptionSurface();
		OptionCurve curve = new OptionCurve();
		Integer numberOfCalls = 0, numberOfPuts = 0;
		Double s = getIndexValue(optionList.get(0).getMinute()), k, r, q, price, tau, tauInDays, volume, delta;
		String cp;
		ArrayList<Integer> impliedDividendTaus = getImpliedDividendTaus();
		Option previousOption = null;

		for (int i = 0; i < optionList.size(); i++) {

			KROptionTransaction optionTransaction = optionList.get(i);
			tauInDays = optionTransaction.getTauInDays();

			cp = optionTransaction.getIsCall();
			k = optionTransaction.getStrikePrice();
			r = koribor.getRate(tauInDays) / 100d;
			q = getDividendRate(tauInDays, impliedDividendTaus);
			price = optionTransaction.getOptionPrice();
			tau = tauInDays / 365d;
			volume = 1d;
			delta = 1d;

			if (s.isNaN() == false && k.isNaN() == false && r.isNaN() == false && q.isNaN() == false && price.isNaN() == false) {
			
				double[] variableValues = {s, k, r, tau, q, price, volume, delta};

				if (previousOption == null || previousOption.get("timeToMaturity") != tau) {

					if (numberOfCalls >= 2 && numberOfPuts >= 2) {

						surface.add(curve);
						numberOfCalls = 0;
						numberOfPuts = 0;

					}

					curve = new OptionCurve();

				}

				Option option;

				if (cp.equals("CALL")) {
					option = new CallOption();
					numberOfCalls++;
				}

				else {
					option = new PutOption();
					numberOfPuts++;
				}

				option.set(variableNames, variableValues);	
				curve.add(option);

				previousOption = option; 

				
			}
						
		}

		return surface;

	}

	private static ArrayList<Integer> getImpliedDividendTaus() {

		ArrayList<Integer> impliedDividendTaus = new ArrayList<Integer>();

		for (int i = 0; i < impliedDividendList.size(); i++) {
			impliedDividendTaus.add(impliedDividendList.get(i).getMaturityInTaus());
		}

		return impliedDividendTaus;

	}

	private static Double getDividendRate(Double tauInDays, ArrayList<Integer> impliedDividendTaus) {

		Integer tau = tauInDays.intValue();

		if (tau <= impliedDividendTaus.get(0))
			return impliedDividendList.get(0).getImpliedDividend();

		else if (tau >= impliedDividendTaus.get(impliedDividendTaus.size() - 1))
			return impliedDividendList.get(impliedDividendTaus.size() - 1).getImpliedDividend();

		else {

			for (int i = 0; i < impliedDividendTaus.size() - 1; i++) {

				if (tau >= impliedDividendTaus.get(i) && tau <= impliedDividendTaus.get(i + 1)) {

					Double rateT = impliedDividendList.get(i).getImpliedDividend(), rateTPlusOne = impliedDividendList.get(i + 1).getImpliedDividend();  
					return rateT + (rateTPlusOne - rateT) * ((tau - impliedDividendTaus.get(i)) / (impliedDividendTaus.get(i + 1) - impliedDividendTaus.get(i)));

				}

			}

		}

		return null;

	}

	private static Double getIndexValue(String minute) {

		for (int i = 0; i < indexList.size(); i++) {

			KRIndexObservation indexObservation = indexList.get(i);
			if (indexObservation.getMinute().equals(minute))
				return indexObservation.getValue();

		}

		return null;

	}

	private static KRKoriborObservation getKoribor(File optionFile) {

		String date = optionFile.getName().substring(0, 10);

		KRKoriborObservation koriborObservation;

		for (int i = 0; i < koriborList.size(); i++) {

			koriborObservation = koriborList.get(i);

			if (koriborObservation.getDate().equals(date)) {
				return koriborObservation;	
			}

		}

		return null;

	}

	private static void readFiles(File optionFile) throws CsvValidationException {

		impliedDividendList = new ArrayList<KRImpliedDividend_jfec>();
		indexList = new ArrayList<KRIndexObservation>(); 
		optionList = new ArrayList<KROptionTransaction>();

		File impliedDividendFile = new File("e:/KOSPI200/impdiv_options/" + optionFile.getName()), 
				indexFile = new File("e:/KOSPI200/index/" + optionFile.getName());

		if (impliedDividendFile.exists() && indexFile.exists()) {

			try (CSVReader reader = new CSVReader(new FileReader(impliedDividendFile))) {

				String[] nextLine;

				while ((nextLine = reader.readNext()) != null) {
					impliedDividendList.add(new KRImpliedDividend_jfec(nextLine));
				}

			} catch (IOException e) {
				e.printStackTrace(); 
			}	

			try (CSVReader reader = new CSVReader(new FileReader(indexFile))) {

				String[] nextLine;

				while ((nextLine = reader.readNext()) != null) {
					if (nextLine.length >= 4)
						indexList.add(new KRIndexObservation(nextLine));
				}

			} catch (IOException e) {
				e.printStackTrace(); 
			}	

			try (CSVReader reader = new CSVReader(new FileReader(optionFile))) {

				String[] nextLine;

				while ((nextLine = reader.readNext()) != null) {
					optionList.add(new KROptionTransaction(nextLine));
				}

			} catch (IOException e) {
				e.printStackTrace(); 
			}		



		}

	}

	private static void readKoriborFile() throws CsvValidationException {

		koriborList = new ArrayList<KRKoriborObservation>();

		try (CSVReader reader = new CSVReader(new FileReader(koriborFile))) {

			String[] nextLine;

			while ((nextLine = reader.readNext()) != null) {

				if (nextLine[0].length() > 0)
					koriborList.add(new KRKoriborObservation(nextLine));

			}

		} catch (IOException e) {
			e.printStackTrace(); 
		}

	}

}
