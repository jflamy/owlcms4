package app.owlcms.ui.preparation;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.spreadsheet.RAthlete;
import app.owlcms.spreadsheet.RCompetition;
import ch.qos.logback.classic.Logger;
import net.sf.jxls.reader.ReaderBuilder;
import net.sf.jxls.reader.ReaderConfig;
import net.sf.jxls.reader.XLSDataReadException;
import net.sf.jxls.reader.XLSReader;

@SuppressWarnings("serial")
public class UploadDialog extends Dialog {
	
	private static final String REGISTRATION_READER_SPEC = "/templates/registration/RegistrationReader.xml";
	final static Logger logger = (Logger) LoggerFactory.getLogger(UploadDialog.class);
	
	public UploadDialog() {
		
		MemoryBuffer buffer = new MemoryBuffer();
		Upload upload = new Upload(buffer);
		upload.addSucceededListener(event -> {
			processInput(buffer.getInputStream());
		});
		
		Label errorLabel = new Label();
		errorLabel.getStyle().set("color", "red");

		VerticalLayout vl = new VerticalLayout(upload, errorLabel);
		add(vl);
	}
	
	private void processInput(InputStream inputStream) {
		String message = null;
		try(InputStream xmlInputStream = this.getClass().getResourceAsStream(REGISTRATION_READER_SPEC)) {
			ReaderConfig.getInstance().setUseDefaultValuesForPrimitiveTypes(true);
			XLSReader reader = ReaderBuilder.buildFromXML(xmlInputStream);

			try (InputStream xlsInputStream = inputStream) {
				RCompetition c = new RCompetition();
				List<RAthlete> athletes = new ArrayList<RAthlete>();

				Map<String, Object> beans = new HashMap<>();
				beans.put("competition", c);
				beans.put("athletes", athletes);

				logger.info("Reading the data...");
				reader.read(inputStream, beans);
				
				logger.info("Read " + athletes.size() + " athletes into `athletes` list");

				List<Athlete> collect = athletes.stream().map(r -> r.getAthlete()).collect(Collectors.toList());
			} catch (XLSDataReadException e) {
				Throwable cause = e.getCause();
				Throwable cause2 = (cause != null ? cause.getCause() : null);
				if (cause == null && cause2 == null) {
					message = e.getLocalizedMessage();
					logger.error(message);
				}
				message = MessageFormat.format("cannot read cell {0}: {1}", 
					e.getCellName(), cause2 != null ? cause2.getLocalizedMessage() : cause.getLocalizedMessage());
			} catch (InvalidFormatException e) {
				message = e.getLocalizedMessage();
			} catch (IOException e) {
				message = e.getLocalizedMessage();
			}
		} catch (IOException e1) {
			message = e1.getLocalizedMessage();
		} catch (SAXException e1) {
			message = e1.getLocalizedMessage();
		}
		if (message != null) {
			logger.error(message);
		}
	}

}
