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
// using per-task Threads instead of a pooled ExecutorService avoids inheritable ThreadLocal
// leakage from pooled threads. A dedicated daemon Thread is started for each request.
import java.util.concurrent.atomic.AtomicReference;
import java.util.Optional;
import java.io.InputStream;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.InputStreamFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import app.owlcms.init.OwlcmsSessionThreadLocal;

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

	// No shared executor here; each download starts a short-lived daemon Thread.

	protected java.util.function.Consumer<Throwable> doneCallback;
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

		Thread writerThread = new Thread(() -> {
			try {
				writeStream(out);
				// notify success
				try {
					if (this.doneCallback != null) this.doneCallback.accept(null);
				} catch (Throwable cb) { /* swallow */ }
			} catch (Throwable t) {
				// notify error to caller
				try { if (this.doneCallback != null) this.doneCallback.accept(t); } catch (Throwable cb) { /* swallow */ }
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
				// Defensive cleanup of thread-local state
				try { OwlcmsSessionThreadLocal.remove(); } catch (Throwable ignore) {}
				try { out.close(); } catch (IOException e) { logger.warn("Error closing piped output stream", e); }
			}
		}, "XLSXWorkbookStreamSource-writer");
		writerThread.setDaemon(true);
		writerThread.start();

		// Return a lightweight wrapper that checks the writer exception before/after reads.
		return new InputStreamWrapper(in, writerException);
	}

	public java.util.function.Consumer<Throwable> getDoneCallback() {
		return this.doneCallback;
	}

	public void setDoneCallback(java.util.function.Consumer<Throwable> action) {
		this.doneCallback = action;
	}

	/**
	 * Optional pre-check invoked before creating the input stream. Implementations should return an Optional
	 * containing an Exception when the download should be aborted early (for example when there's no data).
	 * The default implementation returns empty (no error).
	 */
	public Optional<Exception> prepare() {
		return Optional.empty();
	}

	protected abstract void writeStream(OutputStream stream) throws IOException;

}
