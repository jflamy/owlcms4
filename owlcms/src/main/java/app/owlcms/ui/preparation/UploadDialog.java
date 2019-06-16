/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.preparation;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;

import app.owlcms.data.competition.Competition;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.i18n.TranslationProvider;
import app.owlcms.spreadsheet.RAthlete;
import app.owlcms.spreadsheet.RCompetition;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import net.sf.jxls.reader.ReaderBuilder;
import net.sf.jxls.reader.ReaderConfig;
import net.sf.jxls.reader.XLSReadMessage;
import net.sf.jxls.reader.XLSReadStatus;
import net.sf.jxls.reader.XLSReader;

@SuppressWarnings("serial")
public class UploadDialog extends Dialog {

	private static final String REGISTRATION_READER_SPEC = "/templates/registration/RegistrationReader.xml"; //$NON-NLS-1$

	final static Logger logger = (Logger) LoggerFactory.getLogger(UploadDialog.class);
	final static Logger jxlsLogger = (Logger) LoggerFactory.getLogger("net.sf.jxls.reader.SimpleBlockReaderImpl"); //$NON-NLS-1$
	static {
		jxlsLogger.setLevel(Level.OFF);
	}

	public UploadDialog() {

		MemoryBuffer buffer = new MemoryBuffer();
		Upload upload = new Upload(buffer);
		upload.setWidth("40em"); //$NON-NLS-1$
		
		TextArea ta = new TextArea(TranslationProvider.getString("UploadDialog.0")); //$NON-NLS-1$
		ta.setHeight("20ex"); //$NON-NLS-1$
		ta.setWidth("80em"); //$NON-NLS-1$
		ta.setVisible(false);

		upload.addSucceededListener(event -> {
			processInput(buffer.getInputStream(),ta);
		});
		
		upload.addStartedListener(event -> {
			ta.clear();
			ta.setVisible(false);
		});
		
		H3 title = new H3(TranslationProvider.getString("UploadDialog.1")); //$NON-NLS-1$
		VerticalLayout vl = new VerticalLayout(title, upload, ta);
		add(vl);
	}

	private void processInput(InputStream inputStream, TextArea ta) {
		StringBuffer sb = new StringBuffer();
		try (InputStream xmlInputStream = this.getClass().getResourceAsStream(REGISTRATION_READER_SPEC)) {
			ReaderConfig readerConfig = ReaderConfig.getInstance();
			readerConfig.setUseDefaultValuesForPrimitiveTypes(true);
			readerConfig.setSkipErrors(true);
			XLSReader reader = ReaderBuilder.buildFromXML(xmlInputStream);

			try (InputStream xlsInputStream = inputStream) {
				RCompetition c = new RCompetition();
				List<RAthlete> athletes = new ArrayList<RAthlete>();

				Map<String, Object> beans = new HashMap<>();
				beans.put("competition", c); //$NON-NLS-1$
				beans.put("athletes", athletes); //$NON-NLS-1$

				logger.info(TranslationProvider.getString("UploadDialog.2")); //$NON-NLS-1$
				XLSReadStatus status = reader.read(inputStream, beans);
				@SuppressWarnings("unchecked")
				List<XLSReadMessage> errors = status.getReadMessages();

				for (XLSReadMessage m : errors) {
					sb.append(cleanMessage(m.getMessage()));
					Exception e = m.getException();
					Throwable cause = e.getCause();
					sb.append(cause != null ? cause.getLocalizedMessage() : e.getLocalizedMessage());
					sb.append(System.lineSeparator());
				}
				if (sb.length() > 0) {
					ta.setValue(sb.toString());
					ta.setVisible(true);
				}
				logger.info(TranslationProvider.getString("UploadDialog.3") + athletes.size() + " athletes"); //$NON-NLS-1$ //$NON-NLS-2$

				JPAService.runInTransaction(em -> {
					
					Competition curC = Competition.getCurrent();
					try {
						// update the current competition with the new properties
						BeanUtils.copyProperties(curC, c.getCompetition());
						// update in database and set current to result of JPA merging.
						Competition.setCurrent(em.merge(curC));
						
						// update the athletes with the values read; create if not present.
						// because the athletes in the file have got no Id, this will create
						// new athletes if the file is reloaded.
						athletes.stream().forEach(r -> em.merge(r.getAthlete()));
						em.flush();
					} catch (IllegalAccessException | InvocationTargetException e) {
						sb.append(e.getLocalizedMessage());
					}
					

					return null;
				});
			} catch (InvalidFormatException | IOException e) {
				logger.error(LoggerUtils.stackTrace(e));
			}
		} catch (IOException | SAXException e1) {
			logger.error(LoggerUtils.stackTrace(e1));
		}
		return;
	}

	public String cleanMessage(String localizedMessage) {
		localizedMessage = localizedMessage.replace(TranslationProvider.getString("UploadDialog.4"), ""); //$NON-NLS-1$ //$NON-NLS-2$
		String cell = localizedMessage.substring(0,localizedMessage.indexOf(" ")); //$NON-NLS-1$
		String ss = "spreadsheet"; //$NON-NLS-1$
		int ix = localizedMessage.indexOf(ss)+ss.length();
		String cleanMessage = TranslationProvider.getString("UploadDialog.5")+cell+": "+localizedMessage.substring(ix); //$NON-NLS-1$ //$NON-NLS-2$
		return cleanMessage;
	}
}
