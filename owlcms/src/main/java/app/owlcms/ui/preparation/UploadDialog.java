package app.owlcms.ui.preparation;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.jpa.JPAService;
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

	private static final String REGISTRATION_READER_SPEC = "/templates/registration/RegistrationReader.xml";

	final static Logger logger = (Logger) LoggerFactory.getLogger(UploadDialog.class);
	final static Logger jxlsLogger = (Logger) LoggerFactory.getLogger("net.sf.jxls.reader.SimpleBlockReaderImpl");
	static {
		jxlsLogger.setLevel(Level.OFF);
	}

	public UploadDialog() {

		MemoryBuffer buffer = new MemoryBuffer();
		Upload upload = new Upload(buffer);
		upload.setWidth("40em");
		
		TextArea ta = new TextArea("Errors");
		ta.setHeight("20ex");
		ta.setWidth("80em");
		ta.setVisible(false);

		upload.addSucceededListener(event -> {
			processInput(buffer.getInputStream(),ta);
		});
		
		upload.addStartedListener(event -> {
			ta.clear();
			ta.setVisible(false);
		});
		
		H3 title = new H3("Upload Registration File");
		VerticalLayout vl = new VerticalLayout(title, upload, ta);
		add(vl);
	}

	private List<Athlete> processInput(InputStream inputStream, TextArea ta) {
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
				beans.put("competition", c);
				beans.put("athletes", athletes);

				logger.info("Reading the data...");
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
				logger.info("Read " + athletes.size() + " athletes");

				List<Athlete> collect = athletes.stream().map(r -> r.getAthlete()).collect(Collectors.toList());
				JPAService.runInTransaction(em -> {
					for (Athlete a : collect) {
						em.merge(a);
					}
					return null;
				});
				return collect;
			} catch (InvalidFormatException | IOException e) {
				logger.error(LoggerUtils.stackTrace(e));
			}
		} catch (IOException | SAXException e1) {
			logger.error(LoggerUtils.stackTrace(e1));
		}
		return null;
	}

	public String cleanMessage(String localizedMessage) {
		localizedMessage = localizedMessage.replace("Can't read cell ", "");
		String cell = localizedMessage.substring(0,localizedMessage.indexOf(" "));
		String ss = "spreadsheet";
		int ix = localizedMessage.indexOf(ss)+ss.length();
		String cleanMessage = cell+" "+localizedMessage.substring(ix);
		return cleanMessage;
	}
}
