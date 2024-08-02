package kr.geul.test202408;

import java.io.FileNotFoundException;
import java.io.IOException;

import com.opencsv.exceptions.CsvValidationException;

public class Main_202408 {

	static long startTime, endTime;
	
	public static void main(String[] args) throws CsvValidationException, FileNotFoundException, IOException {
		
		tic();
		GetImpliedDividend_koribor.run();
		toc();
		
	}

	static void tic() {	
		startTime = System.currentTimeMillis();		
	}

	static void toc() {

		endTime = System.currentTimeMillis();
		System.out.println(" DONE, Elapsed time: "
				+ (double) (((double) endTime - startTime) / 1000.00)
				+ " seconds");

	}
	
}
