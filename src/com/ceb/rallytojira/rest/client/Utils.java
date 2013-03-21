package com.ceb.rallytojira.rest.client;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

public class Utils {

	public static boolean isEmpty(String s) {
		return s == null || s.length() == 0 || s.equalsIgnoreCase("null") || s.replace(" ", "").length() == 0;
	}

	public static boolean isNotEmpty(String s) {
		return !isEmpty(s);
	}

	public static boolean isEmpty(Object o) {
		return o == null;
	}

	public static boolean isNotEmpty(Object o) {
		return !isEmpty(o);
	}

	public static String listToString(List<String> list) {
		StringBuilder sb = new StringBuilder();
		for (String s : list) {
			sb.append(s + " | ");
		}
		return sb.toString();
	}

	public static void addError(List<String> errors, String errorMessage, Logger logger) {
		logger.debug("Error: " + errorMessage);
		errors.add((errors.size() + 1) + ". [" + errorMessage + "]");
	}

	public static String getDate(Date date, String format) {
		if (isEmpty(date)) {
			date = new Date();
		}
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(date);
	}

	public static void rollOverFile(Date date, String fileName) {
		File oldFile = new File(fileName);
		if (oldFile.exists()) {
			File newFile = new File(fileName + "_" + Utils.getDate(date, "MMddyyyy_HHmmss"));
			oldFile.renameTo(newFile);
		}
	}

}
