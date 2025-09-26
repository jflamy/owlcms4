/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Optional;
import java.io.InputStream;
import java.util.function.Consumer;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.InputStreamFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Encapsulate a spreadsheet as a StreamSource so that it can be used as a source of data when the user clicks on a link. This class converts the output stream
 * to an input stream that the vaadin framework can consume.
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

	/** Shared executor for background writer tasks. Daemon threads so they don't block shutdown. */
	private static final ExecutorService WRITER_EXECUTOR = Executors.newCachedThreadPool(r -> {
		Thread t = new Thread(r, "XLSXWorkbookStreamSource-writer");
		t.setDaemon(true);
		return t;
	});

	protected Consumer<String> doneCallback;
	protected UI ui;

	public UI getUi() {
		return ui;
	}

	public void setUi(UI ui) {
		this.ui = ui;
	}

	public XLSXWorkbookStreamSource(UI ui) {
		this.ui = ui;
	}

	@Override
	public InputStream createInputStream() {
		final PipedInputStream in;
		final PipedOutputStream out;
		try {
			in = new PipedInputStream();
			out = new PipedOutputStream(in);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// Record writer IOExceptions so the reader can observe them
		final AtomicReference<IOException> writerException = new AtomicReference<>();

		WRITER_EXECUTOR.submit(() -> {
			try {
				writeStream(out);
			} catch (Throwable t) {
				// If the exporter wrapped an IOException in a RuntimeException, unwrap it.
				if (t instanceof IOException) {
					writerException.set((IOException) t);
				} else if (t.getCause() instanceof IOException) {
					writerException.set((IOException) t.getCause());
				} else {
					// Record a generic IOException so the reader/poller sees a failure
					writerException.set(new IOException("Writer failed", t));
				}
			} finally {
				try {
					out.close();
				} catch (IOException e) {
					logger.warn("Error closing piped output stream", e);
				}
			}
		});

		// Return a lightweight wrapper that checks the writer exception before/after reads.
		return new InputStreamWrapper(in, writerException);
	}

	public Consumer<String> getDoneCallback() {
		return this.doneCallback;
	}

	public void setDoneCallback(Consumer<String> action) {
		this.doneCallback = action;
	}

	/**
	 * Optional pre-check invoked before creating the input stream. Implementations should return an Optional
	 * containing an Exception when the download should be aborted early (for example when there's no data).
	 * The default implementation returns empty (no error).
	 */
	public Optional<Exception> preCheck() {
		return Optional.empty();
	}

	protected abstract void writeStream(OutputStream stream) throws IOException;

}
