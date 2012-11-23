/**
 This software was developed at the National Institute of Standards and Technology by employees
of the Federal Government in the course of their official duties. Pursuant to title 17 Section 105 of the
United States Code this software is not subject to copyright protection and is in the public domain.
This is an experimental system. NIST assumes no responsibility whatsoever for its use by other parties,
and makes no guarantees, expressed or implied, about its quality, reliability, or any other characteristic.
We would appreciate acknowledgement if the software is used. This software can be redistributed and/or
modified freely provided that any derivative works bear some notice that they are derived from it, and any
modified versions bear some notice that they have been modified.

Project: NWHIN-DIRECT
Authors: Frederic de Vaulx
		Diane Azais
		Julien Perugini
 */


package gov.nist.direct.utils;

import gov.nist.toolkit.errorrecording.ErrorRecorder;
import gov.nist.toolkit.errorrecording.TextErrorRecorder;
import gov.nist.toolkit.utilities.io.Io;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;


public class Utils {
	static ErrorRecorder er = new TextErrorRecorder();




	/**
	 * Parses a text file and returns its contents
	 * 
	 * @param testData input text file
	 * @return text file data
	 */
	public static byte[] getMessage(String testData) {
		byte[] input = null;
		try {
			input = Io.bytesFromFile(new File(testData));
		} catch (Exception e) {
			er.err(null, e);
			return input;
		}

		//		HttpParser parser = null;
		//		try {
		//			parser = new HttpParser(input, er, false /* not appendex V compliant */);
		//		} catch (Exception e) {
		//			er.err(null, e);
		//			return input;
		//
		//		}
		return input;

	}



	public static ArrayList<String> readFile(File f) {
		ErrorRecorder er = new TextErrorRecorder();

		ArrayList<String> array = new ArrayList<String>();
		BufferedReader bufferedReader = null;

		try {
			bufferedReader = new BufferedReader(new FileReader(f));
			String line = null;
			while ((line = bufferedReader.readLine()) != null) { 
				array.add(line);
			}

		} catch (FileNotFoundException ex) {
			// er.err(null, null, ex.printStackTrace(), null);
		} catch (IOException ex) {
			// er.err(null, null, ex.printStackTrace(), null);
		} finally {
			try {
				if (bufferedReader != null)
					bufferedReader.close();
			} catch (IOException ex) {
				// er.err(null, ex.printStackTrace());
			}
		}
		return array;
	}

	/**
	 * Reads a text file and store it into a HashMap structure
	 * 
	 * @param f input file
	 * @return the data from the input file
	 */
	public static Map<String, Integer> readFileToMap(File f) {
		ErrorRecorder er = new TextErrorRecorder();
		Map<String, Integer> data = new HashMap<String, Integer>();
		BufferedReader bufferedReader = null;

		try {
			bufferedReader = new BufferedReader(new FileReader(f));
			String line = null;
			String[] splitLine = null;
			while ((line = bufferedReader.readLine()) != null) { 
				splitLine = line.split(" ", 2);
				if (!splitLine[0].equals("") && !splitLine[1].equals("")){ // if the code matched to the check is not null
					data.put(splitLine[1].toLowerCase(), Integer.parseInt(splitLine[0]));
				}
				// else do nothing
				// headers without error codes are accepted, error code without header names are not.
			}

		} catch (FileNotFoundException ex) {
			// er.err(null, null, ex.printStackTrace(), null);
		} catch (IOException ex) {
			// er.err(null, null, ex.printStackTrace(), null);
		} finally {
			try {
				if (bufferedReader != null)
					bufferedReader.close();
			} catch (IOException ex) {
				// er.err(null, ex.printStackTrace());
			}
		}
		return data;
	}


	public static void printToFile(MimeMessage msg, String outputFile){
		try {
			msg.writeTo(new FileOutputStream(outputFile));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// TODO Auto-generated catch block
	}


	public static void printHeader(String dts, String textToValidate){
		er.detail("\nValidating DTS "+ dts +" - " + textToValidate);
	}

	public static String headerToString(String dts, String textToValidate){
		return("Validating DTS "+ dts +" - " + textToValidate);
	}

	public static String[] concat(String[] array1, String[] array2) {
		String[] array1and2 = new String[array1.length + array2.length];
		System.arraycopy(array1, 0, array1and2, 0, array1.length);
		System.arraycopy(array2, 0, array1and2, array1.length, array2.length);
		return array1and2;
	}



	public static String inputStreamToString(InputStream is) throws IOException{
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		String line;
		while((line = br.readLine()) != null){
			sb.append(line);
		}
		br.close();
		return line;
	}
	
	public static InputStream stringArrayToInputStream(String[] str) throws IOException{
		InputStream is = new ByteArrayInputStream("file content".getBytes());
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line);
		}
		return null;
		
	}
	
	public static ArrayList<String> byteArrayToStringArrayList(byte[] input) throws IOException{
		InputStream is = new ByteArrayInputStream(input);
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		String line;
		ArrayList<String> array = new ArrayList<String>();
		
		while ((line = br.readLine()) != null) {
			array.add(line);
		}
		
		br.close();
		is.close();
		
		return array;
		
	}
	

	// Getters and Setters
	public static void setErrorRecorder(ErrorRecorder er) {
		Utils.er = er;
	}




}
