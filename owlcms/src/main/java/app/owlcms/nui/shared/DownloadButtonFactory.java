/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.shared;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.server.InputStreamFactory;

import app.owlcms.components.elements.LazyDownloadButton;
import app.owlcms.data.export.CompetitionData;
import app.owlcms.spreadsheet.JXLSWorkbookStreamSource;
import app.owlcms.spreadsheet.XLSXWorkbookStreamSource;
import ch.qos.logback.classic.Logger;

/**
 * A factory for creating Download Button objects.
 */
public class DownloadButtonFactory {

	@SuppressWarnings("unused")
	final private static Logger logger = (Logger) LoggerFactory.getLogger(DownloadButtonFactory.class);

	public static Div createDynamicJsonDownloadButton(String prefix, String label, Notification notification) {
		UI ui = UI.getCurrent();
		final LazyDownloadButton downloadButton = new LazyDownloadButton(
		        label,
		        new Icon(VaadinIcon.DOWNLOAD_ALT),
		        () -> {
			        LocalDateTime now = LocalDateTime.now().withNano(0);
			        return prefix
			                + "_" + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH'h'mm"))
			                + ".json";
		        },
		        () -> {
			        return new CompetitionData().exportData(ui, notification);
		        });
		downloadButton.setNotification(notification);
		return new Div(downloadButton);
	}

	/**
	 * Creates a new DownloadButton object for a dynamically created file.
	 *
	 * @param prefix    the prefix
	 * @param label     the label
	 * @param xlsSource the xls source
	 * @return the div
	 */
	public static Div createDynamicXLSXDownloadButton(String prefix, String label, XLSXWorkbookStreamSource xlsSource) {
		final LazyDownloadButton downloadButton = new LazyDownloadButton(
		        label,
		        new Icon(VaadinIcon.DOWNLOAD_ALT),
		        () -> {
			        LocalDateTime now = LocalDateTime.now().withNano(0);
			        String value = ".xlsx";
			        return prefix
			                + "_" + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH'h'mm';'ss"))
			                + value;
		        },
		        xlsSource);

		return new Div(downloadButton);
	}
	
	public static Div createDynamicJXLSDownloadButton(String fileNamePrefix, String buttonLabel, JXLSWorkbookStreamSource xlsSource, Notification notification) {
		final LazyDownloadButton downloadButton = new LazyDownloadButton(
		        buttonLabel,
		        new Icon(VaadinIcon.DOWNLOAD_ALT),
		        () -> {
			        LocalDateTime now = LocalDateTime.now().withNano(0);
			        String value = ".xlsx";
			        return fileNamePrefix
			                + "_" + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH'h'mm';'ss"))
			                + value;
		        },
		        xlsSource);
		downloadButton.setNotification(notification);
		downloadButton.setWidthFull();
		return new Div(downloadButton);
	}

	public static Div createDynamicDownloadButton(String prefix, String label, InputStreamFactory supplier, Supplier<String> extensionSupplier) {
		final LazyDownloadButton downloadButton = new LazyDownloadButton(
		        label,
		        new Icon(VaadinIcon.DOWNLOAD_ALT),
		        () -> {
			        LocalDateTime now = LocalDateTime.now().withNano(0);
			        return prefix
			                + "_" + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH'h'mm"))
			                + extensionSupplier.get();
		        },
		        supplier);
		return new Div(downloadButton);
	}

	public static Div createDynamicDownloadButton(Supplier<String> prefixSupplier, String label, InputStreamFactory supplier, Supplier<String> extensionSupplier) {
		final LazyDownloadButton downloadButton = new LazyDownloadButton(
		        label,
		        new Icon(VaadinIcon.DOWNLOAD_ALT),
		        () -> {
			        LocalDateTime now = LocalDateTime.now().withNano(0);
			        return prefixSupplier.get()
			                + "_" + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH'h'mm"))
			                + extensionSupplier.get();
		        },
		        supplier);
		return new Div(downloadButton);
	}

	public static Div createDynamicZipDownloadButton(String prefix, String label, byte[] content) {
		final LazyDownloadButton downloadButton = new LazyDownloadButton(
		        label,
		        new Icon(VaadinIcon.DOWNLOAD_ALT),
		        () -> {
			        LocalDateTime now = LocalDateTime.now().withNano(0);
			        return prefix
			                + "_" + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH'h'mm"))
			                + ".zip";
		        },
		        () -> {
			        return new ByteArrayInputStream(content);
		        });

		return new Div(downloadButton);
	}
	
	public static Div createDynamicZipDownloadButton(String prefix, String label, byte[] content, Icon icon) {
		final LazyDownloadButton downloadButton = new LazyDownloadButton(
		        label,
		        icon,
		        () -> {
			        LocalDateTime now = LocalDateTime.now().withNano(0);
			        return prefix
			                + "_" + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH'h'mm"))
			                + ".zip";
		        },
		        () -> {
			        return new ByteArrayInputStream(content);
		        });

		return new Div(downloadButton);
	}

	public static Div createDynamicZipDownloadButton(String prefix, String label, InputStreamFactory supplier) {
		final LazyDownloadButton downloadButton = new LazyDownloadButton(
		        label,
		        new Icon(VaadinIcon.DOWNLOAD_ALT),
		        () -> {
			        LocalDateTime now = LocalDateTime.now().withNano(0);
			        return prefix
			                + "_" + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH'h'mm"))
			                + ".zip";
		        },
		        supplier);

		return new Div(downloadButton);
	}
	
	public static Div createDynamicZipDownloadButton(String prefix, String label, InputStreamFactory supplier, Icon icon) {
		final LazyDownloadButton downloadButton = new LazyDownloadButton(
		        label,
		        icon,
		        () -> {
			        LocalDateTime now = LocalDateTime.now().withNano(0);
			        return prefix
			                + "_" + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH'h'mm"))
			                + ".zip";
		        },
		        supplier);

		return new Div(downloadButton);
	}


}
