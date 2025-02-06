package app.owlcms.nui.preparation;

import java.io.InputStream;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;

import app.owlcms.data.technicalofficial.TechnicalOfficialReader;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class TechnicalOfficialsUploadDialog extends Dialog {
	Logger logger = (Logger) LoggerFactory.getLogger(TechnicalOfficialsUploadDialog.class);
    private Runnable callback;
    private MemoryBuffer buffer = new MemoryBuffer();
    private Upload upload;

    public TechnicalOfficialsUploadDialog() {
        Button uploadButton = new Button(Translator.translate("TechnicalOfficials.Upload"));
        upload = new Upload(buffer);
        upload.setUploadButton(uploadButton);
        upload.setDropLabel(new NativeLabel(Translator.translate("TechnicalOfficials.UploadDropZone")));
        upload.addSucceededListener(e -> {
            try {
                InputStream is = buffer.getInputStream();
                TechnicalOfficialReader.importFromXLS(is);
                close();
                if (callback != null) {
                    callback.run();
                }
            } catch (Exception ex) {
                // Handle error
            }
        });
        add(upload);
    }

    public void setCallback(Runnable callback) {
        this.callback = callback;
    }
}
