/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * The Class LoggerUtils.
 */
public class LoggerUtils {
	
	/**
	 * Where from.
	 *
	 * @return the string
	 */
	public static String whereFrom() {
		return Thread.currentThread().getStackTrace()[3].toString();
	}
	
	/**
	 * Where from.
	 *
	 * @return the string
	 */
	public static String stackTrace() {
		StringWriter sw = new StringWriter();
		new Exception("").printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}
	
	public static String stackTrace(Throwable t) {
		StringWriter sw = new StringWriter();
		t.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}
}
