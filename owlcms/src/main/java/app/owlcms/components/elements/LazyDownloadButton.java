/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.elements;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.InputStreamFactory;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.TransferContext;
import com.vaadin.flow.server.streams.TransferProgressListener;
import com.vaadin.flow.shared.Registration;

import app.owlcms.i18n.Translator;
import app.owlcms.spreadsheet.InputStreamWrapper;
import app.owlcms.spreadsheet.JXLSWorkbookStreamSource;
import app.owlcms.spreadsheet.XLSXWorkbookStreamSource;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

/**
 * Extension of anchor, that will display a vaadin button as clickable instance to initiate a download. The download content is generated at click time.
 *
 * @author Stefan Uebe
 * @implNote Copied from https://github.com/stefanuebe/vaadin-lazy-download-button
 */

@SuppressWarnings("serial")
public class LazyDownloadButton extends Button {

	public static class DownloadStartsEvent extends ComponentEvent<LazyDownloadButton> {

		private final DomEvent clientSideEvent;

		/**
		 * Creates a new event using the given source and indicator whether the event originated from the client side or the server side.
		 *
		 * @param source     the source component
		 * @param fromClient <code>true</code> if the event originated from the client
		 */
		public DownloadStartsEvent(LazyDownloadButton source, boolean fromClient, DomEvent clientSideEvent) {
			super(source, fromClient);
			this.clientSideEvent = clientSideEvent;
		}

		public DomEvent getClientSideEvent() {
			return this.clientSideEvent;
		}
	}

	/**
	 * Run preCheck() on known workbook stream sources. Returns Optional.empty() when no error.
	 */
	private Optional<Exception> runPreCheck() {
		System.err.println("*** LazyDownloadButton.runPreCheck");
		InputStreamFactory cb = getInputStreamCallback();
		if (cb instanceof XLSXWorkbookStreamSource) {
			System.err.println("*** LazyDownloadButton.runPreCheck: XLSXWorkbookStreamSource");
			try {
				return ((XLSXWorkbookStreamSource) cb).prepare();
			} catch (Exception e) {
				// Convert thrown exception into Optional to let the caller show a notification
				LoggerUtils.logError(logger, e);
				return Optional.of(e);
			}
		}
		if (cb instanceof JXLSWorkbookStreamSource) {
			System.err.println("*** LazyDownloadButton.runPreCheck: JXLSWorkbookStreamSource");
			try {
				return ((JXLSWorkbookStreamSource) cb).prepare();
			} catch (Exception e) {
				LoggerUtils.logError(logger, e);
				return Optional.of(e);
			}
		}
		System.err.println("*** LazyDownloadButton.runPreCheck: no preCheck "+LoggerUtils.whereFrom());
		return Optional.empty();
	}

	private static final String DEFAULT_FILE_NAME = "download";
	private static final Supplier<String> DEFAULT_FILE_NAME_SUPPLIER = () -> DEFAULT_FILE_NAME;
	Logger logger = (Logger) LoggerFactory.getLogger(LazyDownloadButton.class);
	private Anchor anchor;
	private Supplier<String> fileNameCallback;
	private InputStreamFactory inputStreamCallback;
	private Notification notification;

	public LazyDownloadButton() {
	}

	public LazyDownloadButton(Component icon) {
		super(icon);
	}

	public LazyDownloadButton(Component icon, InputStreamFactory inputStreamFactory) {
		this(icon, DEFAULT_FILE_NAME_SUPPLIER, inputStreamFactory);
	}

	public LazyDownloadButton(Component icon, Supplier<String> fileNameCallback,
	        InputStreamFactory inputStreamFactory) {
		this("", icon, fileNameCallback, inputStreamFactory);
	}

	public LazyDownloadButton(String text) {
		super(text);
	}

	public LazyDownloadButton(String text, Component icon, InputStreamFactory inputStreamFactory) {
		this(text, icon, DEFAULT_FILE_NAME_SUPPLIER, inputStreamFactory);
	}

	/**
	 * Creates a download button.
	 *
	 * The first two parameters are used for the button display.
	 * <p>
	 * The third parameter is a callback, that is used to generate the download file name
	 * <p/>
	 * <p>
	 * The fourth parameter is a callback to generate the input stream sent to the client. This callback will be called in a separate thread (so that the UI
	 * thread is not blocked).
	 * <p/>
	 * <p>
	 * You can add an additional listener using {@link #addDownloadStartsListener(ComponentEventListener)} for when the download starts
	 * </p>
	 *
	 * @param text                button text
	 * @param icon                button icon
	 * @param fileNameCallback    callback for file name generation
	 * @param inputStreamCallback callback for input stream generation
	 */
	public LazyDownloadButton(String text, Component icon, Supplier<String> pFileNameCallback,
	        InputStreamFactory pInputStreamCallback) {
		super(text);

		this.setFileNameCallback(pFileNameCallback);
		this.setInputStreamCallback(pInputStreamCallback);

		if (icon != null) {
			setIcon(icon);
		}

		super.addClickListener(event -> {
			// We are in the Vaadin UI thread here.
			getParent().ifPresent(component -> {
				Objects.requireNonNull(getFileNameCallback(), "File name callback must not be null");
				Objects.requireNonNull(getInputStreamCallback(), "Input stream callback must not be null");

				if (this.anchor == null) {
					this.anchor = new Anchor();
					Element anchorElement = this.anchor.getElement();
					anchorElement.setAttribute("download", true);
					anchorElement.getStyle().set("display", "none");
					component.getElement().appendChild(this.anchor.getElement());
					anchorElement.addEventListener("click",
					        event1 -> fireEvent(new DownloadStartsEvent(this, true, event1)));
				}

				Optional<UI> optionalUI = getUI();
				try {
					// Shared flag to ensure we only show one error notification per download attempt
					final AtomicBoolean errorNotified = new AtomicBoolean(false);

					// Run the pre-check on the UI thread BEFORE creating the DownloadHandler so we can
					// show a notification (via UI.access) and abort without creating the handler/anchor.
					java.util.Optional<java.lang.Exception> pre = runPreCheck();
					if (pre.isPresent()) {
						Exception e = pre.get();
						// If the input source is a known workbook stream source and it has a doneCallback,
						// delegate the error notification to that doneCallback (it will show the top-right
						// notification). For other input sources, show the local notification here.
						InputStreamFactory cb = getInputStreamCallback();
						boolean delegatedToDoneCallback = false;
						try {
							if (cb instanceof XLSXWorkbookStreamSource) {
								java.util.function.Consumer<Throwable> done = ((XLSXWorkbookStreamSource) cb).getDoneCallback();
								if (done != null) {
									try { done.accept(e); } catch (Throwable ignore) { /* swallow */ }
									delegatedToDoneCallback = true;
								}
							} else if (cb instanceof JXLSWorkbookStreamSource) {
								java.util.function.Consumer<Throwable> done = ((JXLSWorkbookStreamSource) cb).getDoneCallback();
								if (done != null) {
									try { done.accept(e); } catch (Throwable ignore) { /* swallow */ }
									delegatedToDoneCallback = true;
								}
							}
						} catch (Throwable ignore) {}

						if (!delegatedToDoneCallback) {
							optionalUI.ifPresent(ui -> {
								showDownloadErrorNotification(ui, e.getMessage() == null ? e.toString() : e.getMessage(), e);
								errorNotified.set(true);
							});
						}
						return;
					}
					System.err.println("*** LazyDownloadButton creating DownloadHandler");
					DownloadHandler downloadHandler = DownloadHandler.fromInputStream(
						(downloadEvent) -> {
									try {
										InputStream downloadStream = getInputStreamCallback().createInputStream();
										System.err.println("*** LazyDownloadButton created download stream: "+downloadStream);

										// If the stream is our wrapper, poll briefly for a fast writer failure and
										// return an error response immediately if one occurred.
										if (downloadStream instanceof InputStreamWrapper) {
											InputStreamWrapper wrapper = (InputStreamWrapper) downloadStream;
											// Poll up to 200ms (in 50ms increments) for an immediate failure
											IOException fastEx = null;
											for (int i = 0; i < 4; i++) {
												fastEx = wrapper.getWriterException();
												if (fastEx != null) break;
												try {
													Thread.sleep(50);
												} catch (InterruptedException ie) {
													Thread.currentThread().interrupt();
													break;
												}
											}
											if (fastEx != null) {
												final IOException ex = fastEx;
												final String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
												optionalUI.ifPresent(ui -> {
													showDownloadErrorNotification(ui, msg, ex);
													errorNotified.set(true);
												});
												return DownloadResponse.error(500, msg);
											}
										}

										return new DownloadResponse(
												downloadStream,
												getFileNameCallback().get(),
												null, // content type - let Vaadin determine it
												-1 // content length - unknown
										);
									} catch (Exception e) {
										return DownloadResponse.error(500, e.getMessage());
									}
						}, new TransferProgressListener() {
						        @Override
						        public void onStart(TransferContext tc) {
							        logger.warn("download starting in UI {}", optionalUI.get());
							        if (notification != null && !notification.isOpened()) {
								        notification.open();
							        }
						        }

						        @Override
						        public void onComplete(TransferContext tc, long transferredBytes) {
							        if (notification != null && notification.isOpened()) {
								        notification.close();
							        }
							        logger.info("Download succeeded: {} bytes", transferredBytes);
						        }

								@Override
								public void onError(TransferContext tc, IOException error) {
									// Only show the notification if we haven't already (preCheck or fast-fail)
									if (!errorNotified.getAndSet(true)) {
										try {
											UI ui = tc.getUI();
											if (ui != null) {
												ui.access(() -> showDownloadErrorNotification(ui, error.getMessage(), error));
											} else {
												// No UI available on context; fall back to logging
												logger.error("Download failed: {}", error.getMessage(), error);
											}
										} catch (Exception e) {
											// Ensure we still log the original error even if UI access fails
											logger.error("Download failed (notify failed): {}", error.getMessage(), error);
										}
									} else {
										// Still log the error even if notification was already shown
										logger.error("Download failed (already notified): {}", error.getMessage(), error);
									}
								}
					        });

					optionalUI.ifPresent(ui -> ui.access(() -> {
						this.anchor.setHref(downloadHandler);
						this.anchor.getElement().callJsFunction("click");
					}));

				} catch (Exception e) {
					LoggerUtils.logError(logger, e);
				}

			});
		});
	}

	public LazyDownloadButton(String text, InputStreamFactory inputStreamFactory) {
		this(text, DEFAULT_FILE_NAME_SUPPLIER, inputStreamFactory);
	}

	public LazyDownloadButton(String text, Supplier<String> fileNameCallback, InputStreamFactory inputStreamFactory) {
		this(text, null, fileNameCallback, inputStreamFactory);
	}

	public Registration addDownloadStartsListener(ComponentEventListener<DownloadStartsEvent> listener) {
		return addListener(DownloadStartsEvent.class, listener);
	}

	public Supplier<String> getFileNameCallback() {
		return this.fileNameCallback;
	}

	public InputStreamFactory getInputStreamCallback() {
		return this.inputStreamCallback;
	}

	public Notification getNotification() {
		return this.notification;
	}

	public void setFileNameCallback(Supplier<String> fileNameCallback) {
		this.fileNameCallback = fileNameCallback;
	}

	public void setInputStreamCallback(InputStreamFactory inputStreamCallback) {
		this.inputStreamCallback = inputStreamCallback;
	}

	public void setNotification(Notification notification) {
		this.notification = notification;
	}

	@Override
	protected void onDetach(DetachEvent detachEvent) {
		if (this.anchor != null) {
			getParent().map(Component::getElement).ifPresent(parentElement -> {
				Element anchorElement = this.anchor.getElement();
				if (anchorElement != null && parentElement.getChildren().anyMatch(anchorElement::equals)) {
					parentElement.removeChild(anchorElement);
				}
			});
		}
	}

	/**
	 * Show the standard download error notification and log the error.
	 * This is safe to call from any thread: it will schedule UI access if needed.
	 */
	private void showDownloadErrorNotification(UI ui, String message, Throwable error) {
		try {
			if (ui == null) return;
			ui.access(() -> {
				String body = message == null ? Translator.translate("Download.failed") : Translator.translate("Download.failed", message);
				Notification notification = new Notification(body);
				notification.setDuration(5000);
				notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
				notification.open();
				logger.error("Download failed: {}", message, error);
			});
		} catch (Exception e) {
			// If UI access fails, still log the original error
			logger.error("Download failed (and UI notify failed): {}", message, error);
		}
	}
}