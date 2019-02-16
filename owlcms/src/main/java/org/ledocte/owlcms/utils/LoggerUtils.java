package org.ledocte.owlcms.utils;

public class LoggerUtils {
	
	public static String whereFrom() {
		return Thread.currentThread().getStackTrace()[3].toString();
	}
}
