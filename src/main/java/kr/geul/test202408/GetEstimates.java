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

public class GetEstimates {

	static final double extrapolationMultiplier = 3.0,
			strikePriceGap = 0.1,
			precisionMultiplier = 1.0 / strikePriceGap;
	static final double[] targetTimeToMaturities = {2.0 / 12.0, 4.0 / 12.0};
	static final String[] variableNames = {"S", "K", "R", "T", "D", "C", "V", "delta"};

	static BKMEstimator bkmEstimator;
	static UnivariateFunction kMinFunction, kMaxFunction;	

	static double[] kMin, kMax, timeToMaturity;

	static ArrayList<KRIndexObservation> indexList;
	static ArrayList<KROptionTransaction> optionList;
	static ArrayList<KRImpliedDividend_jfec> impliedDividendList;
	static ArrayList<KRKoriborObservation> koriborList;

	static File koriborFile = new File("e:/koribor.csv"), optionFolder = new File("e:/KOSPI200/closing_filtered");
	static File[] optionFiles = optionFolder.listFiles();
	static String targetFolderPath = "e:/KOSPI200/estimates/";

	static KRKoriborObservation koribor;

	public static void run() throws CsvValidationException, InvalidArgumentException, InconsistentArgumentLengthException, AtTheMoneyException, InconsistentOptionException, DuplicateOptionsException, FileNotFoundException {

		bkmEstimator = new BKMEstimator();
		readKoriborFile();

		for (File optionFile : optionFiles) {

			String fileName = optionFile.getName();
			Integer month = Integer.parseInt(optionFile.getName().substring(5, 7));
			
			if (fileName.substring(0, 4).equals("2023") && month > 6 && new File(targetFolderPath + fileName).exists() == false) {

				System.out.println(fileName);
				readFiles(optionFile);
				koribor = getKoribor(optionFile);

				if (koribor != null && optionList.size() > 0) {

					OptionSurface optionSurface = createSurface();
					ArrayList<OptionCurve> curves = optionSurface.getCurves();

					if (curves.size() > 2) {

						getTruncationLocations(optionSurface);
						fixTimeToMaturities(optionSurface);

						curves = optionSurface.getCurves();
						
						if (curves.size() >= 1) {

							Double[][] estimates = new Double[curves.size()][36],
									endpoints_strike = new Double[curves.size()][12];

							interpolate(optionSurface, true);
							double[][] estimates_temporary = getEstimate(optionSurface.getCurves());
							Double[][] endpoints_temporary = getEndpoints(optionSurface.getCurves());

							for (int i = 0; i < curves.size(); i++) {
 
								estimates[i][0] = estimates_temporary[i][0];
								estimates[i][1] = estimates_temporary[i][1];
								estimates[i][2] = estimates_temporary[i][2];
								endpoints_strike[i][0] = endpoints_temporary[i][0];
								endpoints_strike[i][1] = endpoints_temporary[i][1];

							}

							extrapolate(optionSurface);
							estimates_temporary = getEstimate(optionSurface.getCurves());

							for (int i = 0; i < curves.size(); i++) {

								estimates[i][3] = estimates_temporary[i][0];
								estimates[i][4] = estimates_temporary[i][1];
								estimates[i][5] = estimates_temporary[i][2]; 

							}

							double[] referenceVolatility = new double[curves.size()];

							for (int i = 0; i < curves.size(); i++) {
								referenceVolatility[i] = estimates[i][3];
							}

							optionSurface.trim(getTrimmingLocations(optionSurface, referenceVolatility, -0.014378, 0.007317, -0.030977, 0.016435));
							estimates_temporary = getEstimate(optionSurface.getCurves());
							endpoints_temporary = getEndpoints(optionSurface.getCurves());

							for (int i = 0; i < curves.size(); i++) {

								estimates[i][6] = estimates_temporary[i][0];
								estimates[i][7] = estimates_temporary[i][1];
								estimates[i][8] = estimates_temporary[i][2];
								endpoints_strike[i][2] = endpoints_temporary[i][0];
								endpoints_strike[i][3] = endpoints_temporary[i][1];

							}

							extrapolate(optionSurface);
							estimates_temporary = getEstimate(optionSurface.getCurves());

							for (int i = 0; i < curves.size(); i++) {

								estimates[i][9] = estimates_temporary[i][0];
								estimates[i][10] = estimates_temporary[i][1];
								estimates[i][11] = estimates_temporary[i][2]; 

							}

							optionSurface.trim(getTrimmingLocations(optionSurface, referenceVolatility, -0.008202, 0.004735, -0.019823, 0.011278));
							estimates_temporary = getEstimate(optionSurface.getCurves());
							endpoints_temporary = getEndpoints(optionSurface.getCurves());

							for (int i = 0; i < curves.size(); i++) {

								estimates[i][12] = estimates_temporary[i][0];
								estimates[i][13] = estimates_temporary[i][1];
								estimates[i][14] = estimates_temporary[i][2];
								endpoints_strike[i][4] = endpoints_temporary[i][0];
								endpoints_strike[i][5] = endpoints_temporary[i][1];

							}							

							extrapolate(optionSurface);
							estimates_temporary = getEstimate(optionSurface.getCurves());						

							for (int i = 0; i < curves.size(); i++) {

								estimates[i][15] = estimates_temporary[i][0];
								estimates[i][16] = estimates_temporary[i][1];
								estimates[i][17] = estimates_temporary[i][2]; 

							}						

							optionSurface.trim(getTrimmingLocations(optionSurface, referenceVolatility, -0.005718, 0.003339, -0.014755, 0.008493));
							estimates_temporary = getEstimate(optionSurface.getCurves());
							endpoints_temporary = getEndpoints(optionSurface.getCurves());

							for (int i = 0; i < curves.size(); i++) {

								estimates[i][18] = estimates_temporary[i][0];
								estimates[i][19] = estimates_temporary[i][1];
								estimates[i][20] = estimates_temporary[i][2];
								endpoints_strike[i][6] = endpoints_temporary[i][0];
								endpoints_strike[i][7] = endpoints_temporary[i][1];

							}							

							extrapolate(optionSurface);
							estimates_temporary = getEstimate(optionSurface.getCurves());						

							for (int i = 0; i < curves.size(); i++) {

								estimates[i][21] = estimates_temporary[i][0];
								estimates[i][22] = estimates_temporary[i][1];
								estimates[i][23] = estimates_temporary[i][2]; 

							}

							optionSurface.trim(getTrimmingLocations(optionSurface, referenceVolatility, -0.004523, 0.002619, -0.011980, 0.007168));
							estimates_temporary = getEstimate(optionSurface.getCurves());
							endpoints_temporary = getEndpoints(optionSurface.getCurves());

							for (int i = 0; i < curves.size(); i++) {

								estimates[i][24] = estimates_temporary[i][0];
								estimates[i][25] = estimates_temporary[i][1];
								estimates[i][26] = estimates_temporary[i][2];
								endpoints_strike[i][8] = endpoints_temporary[i][0];
								endpoints_strike[i][9] = endpoints_temporary[i][1];

							}

							extrapolate(optionSurface);
							estimates_temporary = getEstimate(optionSurface.getCurves());						

							for (int i = 0; i < curves.size(); i++) {

								estimates[i][27] = estimates_temporary[i][0];
								estimates[i][28] = estimates_temporary[i][1];
								estimates[i][29] = estimates_temporary[i][2]; 

							}

							optionSurface.trim(getTrimmingLocations(optionSurface, referenceVolatility, -0.003054, 0.001876, -0.009651, 0.005469));
							estimates_temporary = getEstimate(optionSurface.getCurves());
							endpoints_temporary = getEndpoints(optionSurface.getCurves());

							for (int i = 0; i < curves.size(); i++) {

								estimates[i][30] = estimates_temporary[i][0];
								estimates[i][31] = estimates_temporary[i][1];
								estimates[i][32] = estimates_temporary[i][2];
								endpoints_strike[i][10] = endpoints_temporary[i][0];
								endpoints_strike[i][11] = endpoints_temporary[i][1];

							}			

							extrapolate(optionSurface);
							estimates_temporary = getEstimate(optionSurface.getCurves());						

							for (int i = 0; i < curves.size(); i++) {

								estimates[i][33] = estimates_temporary[i][0];
								estimates[i][34] = estimates_temporary[i][1];
								estimates[i][35] = estimates_temporary[i][2]; 

							}

							writefile(optionFile, curves, estimates, endpoints_strike);

						}

					}

				}

			}

		}

	}

	private static void writefile(File optionFile, ArrayList<OptionCurve> curves, Double[][] estimates, Double[][] endpoints_strike) {

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFolderPath + optionFile.getName()))) {

			Double s = curves.get(0).getVariableArray()[0], tau, sigma;
			Double[] sigmaArray = new Double[curves.size()];
			String date = optionFile.getName().substring(0, 10);

			for (int i = 0; i < curves.size(); i++) {
				sigmaArray[i] = estimates[i][3];
			}
			
			writer.write(date + "," + s + ",");

			for (int i = 0; i < curves.size(); i++) {

				tau = curves.get(i).getVariableArray()[1];
				
				if (curves.get(0).getVariableArray()[1] > 0.2) {

					for (int j = 0; j < 37; j++) {
						writer.write(",");					
					}	

				}

				writer.write(tau + ",");
				
				for (int j = 0; j < 36; j++) {
					writer.write(estimates[i][j] + ",");					
				}

			}

			if (curves.size() == 1 && curves.get(0).getVariableArray()[1] < 0.2) {

				for (int j = 0; j < 37; j++) {
					writer.write(",");					
				}	

			}

			for (int i = 0; i < curves.size(); i++) {

				tau = curves.get(i).getVariableArray()[1];
				sigma = sigmaArray[i];
				
				if (curves.get(0).getVariableArray()[1] > 0.2) {

					for (int j = 0; j < 13; j++) {
						writer.write(",");					
					}	

				}

				writer.write(tau + ",");
				
				for (int j = 0; j < 12; j++) {
					writer.write((Math.log(endpoints_strike[i][j] / s) * sigma * Math.sqrt(tau)) + ",");					
				}

			}

			if (curves.size() == 1 && curves.get(0).getVariableArray()[1] < 0.2) {

				for (int j = 0; j < 13; j++) {
					writer.write(",");					
				}	

			}
			
			writer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static double[][] getTrimmingLocations(OptionSurface surface, double[] referenceVolatility, double kMin2Multiplier, double kMax2Multiplier,
			double kMin4Multiplier, double kMax4Multiplier) {

		ArrayList<OptionCurve> curves = surface.getCurves();
		double[][] locations = new double[curves.size()][2];

		for (int i = 0; i < curves.size(); i++) {

			OptionCurve curve = curves.get(i);
			double sharePrice = curve.getVariableArray()[0];
			double maturity = curve.getVariableArray()[1];
			double vol = referenceVolatility[i];

			if (maturity < 0.2) {
				locations[i][0] = sharePrice * Math.exp(kMin2Multiplier / (vol * Math.sqrt(maturity)));
				locations[i][1] = sharePrice * Math.exp(kMax2Multiplier / (vol * Math.sqrt(maturity)));
			}

			else {
				locations[i][0] = sharePrice * Math.exp(kMin4Multiplier / (vol * Math.sqrt(maturity)));
				locations[i][1] = sharePrice * Math.exp(kMax4Multiplier / (vol * Math.sqrt(maturity)));				
			}

		}

		return locations;

	}

	private static double[][] getEstimate(ArrayList<OptionCurve> curves) throws DuplicateOptionsException, InconsistentOptionException {

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

	private static Double[][] getEndpoints(ArrayList<OptionCurve> curves) throws InvalidArgumentException, InconsistentArgumentLengthException {

		Double[][] endpoints = new Double[curves.size()][2];

		for (int i = 0; i < curves.size(); i++) {

			ArrayList<Option> options = curves.get(i).getOptions();
			endpoints[i][0] = options.get(0).getStrikePrice();
			endpoints[i][1] = options.get(options.size() - 1).getStrikePrice();

		}

		return endpoints;

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

	private static ArrayList<Integer> getImpliedDividendTaus() {

		ArrayList<Integer> impliedDividendTaus = new ArrayList<Integer>();

		for (int i = 0; i < impliedDividendList.size(); i++) {
			impliedDividendTaus.add(impliedDividendList.get(i).getMaturityInTaus());
		}

		return impliedDividendTaus;

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
