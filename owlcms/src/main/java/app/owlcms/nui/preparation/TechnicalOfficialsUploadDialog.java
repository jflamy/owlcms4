package app.owlcms.nui.preparation;

import java.io.InputStream;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.component.textfield.TextArea;

import app.owlcms.data.technicalofficial.TechnicalOfficialReader;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class TechnicalOfficialsUploadDialog extends Dialog {
    Logger logger = (Logger) LoggerFactory.getLogger(TechnicalOfficialsUploadDialog.class);
    private Runnable callback;
    private MemoryBuffer buffer = new MemoryBuffer();
    private Upload upload;
    // Change from NativeLabel to TextArea for multi-line error output:
    private TextArea errorArea = new TextArea();

    public TechnicalOfficialsUploadDialog() {
        Button uploadButton = new Button(Translator.translate("SelectFile"));
        upload = new Upload(buffer);
        upload.setUploadButton(uploadButton);
        upload.setDropLabel(new NativeLabel(Translator.translate("DropZone")));

        errorArea.setReadOnly(true);
        errorArea.setVisible(false);
        errorArea.setWidth("100%");

        upload.addSucceededListener(e -> {
            try {
                InputStream is = buffer.getInputStream();
                StringBuilder errors = new StringBuilder();
                var officials = new TechnicalOfficialReader().importFromXLS(is, errors);
                if (errors.length() > 0) {
                    errorArea.setValue(errors.toString());
                    errorArea.setVisible(true);
                } else {
                    errorArea.setValue(Translator.translate("TechnicalOfficials.UploadSuccess", officials.size()));
                    if (callback != null) {
                        callback.run();
                    }
                }
            } catch (Exception ex) {
                errorArea.setVisible(true);
                errorArea.setValue(ex.getMessage());
                logger.error("Upload failed", ex);
            }
        });

        add(upload);
        add(errorArea);
        // Add close button to dialog footer using getFooter()
        Button closeButton = new Button(Translator.translate("Close"), event -> close());
        getFooter().add(closeButton);
    }

    public void setCallback(Runnable callback) {
        this.callback = callback;
    }
}
