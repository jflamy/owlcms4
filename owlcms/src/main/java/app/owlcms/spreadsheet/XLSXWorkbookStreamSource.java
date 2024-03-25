/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.function.Consumer;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.InputStreamFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Encapsulate a spreadsheet as a StreamSource so that it can be used as a source of data when the user clicks on a
 * link. This class converts the output stream to an input stream that the vaadin framework can consume.
 */
@SuppressWarnings("serial")
public abstract class XLSXWorkbookStreamSource implements InputStreamFactory {

	final private static Logger jexlLogger = (Logger) LoggerFactory.getLogger("org.apache.commons.jexl2.JexlEngine");
	final private static Logger logger = (Logger) LoggerFactory.getLogger(XLSXWorkbookStreamSource.class);
	final private static Logger tagLogger = (Logger) LoggerFactory.getLogger("net.sf.jxls.tag.ForEachTag");
	static {
		logger.setLevel(Level.INFO);
		jexlLogger.setLevel(Level.ERROR);
		tagLogger.setLevel(Level.ERROR);
	}
	protected Consumer<String> doneCallback;

	public XLSXWorkbookStreamSource() {
	}

	@Override
	public InputStream createInputStream() {
		try {
			PipedInputStream in = new PipedInputStream();
			PipedOutputStream out = new PipedOutputStream(in);
			new Thread(
			        new Runnable() {
				        @Override
				        public void run() {
					        try {
						        writeStream(out);
						        out.close();
					        } catch (IOException e) {
						        throw new RuntimeException(e);
					        }
				        }
			        }).start();
			return in;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Consumer<String> getDoneCallback() {
		return this.doneCallback;
	}

	public void setDoneCallback(Consumer<String> action) {
		this.doneCallback = action;
	}

	protected abstract void writeStream(OutputStream stream);

}
