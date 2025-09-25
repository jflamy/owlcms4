package app.owlcms.nui.preparation;

import java.io.ByteArrayInputStream;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.component.textfield.TextArea;

import app.owlcms.data.coach.CoachReader;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class CoachUploadDialog extends Dialog {
    Logger logger = (Logger) LoggerFactory.getLogger(CoachUploadDialog.class);
    private Runnable callback;
    private Upload upload;
    private TextArea errorArea = new TextArea();

    public CoachUploadDialog() {
        Button uploadButton = new Button(Translator.translate("SelectFile"));

        UploadHandler uploadHandler = UploadHandler.inMemory((metadata, bytes) -> {
            try {
                ByteArrayInputStream is = new ByteArrayInputStream(bytes);
                StringBuilder errors = new StringBuilder();
                var coaches = new CoachReader().importFromXLS(is, errors);
                if (errors.length() > 0) {
                    errorArea.setValue(errors.toString());
                    errorArea.setVisible(true);
                } else {
                    errorArea.setValue(Translator.translate("Coaches.UploadSuccess", coaches));
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

        upload = new Upload(uploadHandler);
        upload.setUploadButton(uploadButton);
        upload.setDropLabel(new NativeLabel(Translator.translate("DropZone")));

        errorArea.setReadOnly(true);
        errorArea.setVisible(false);
        errorArea.setWidth("100%");

        add(upload);
        add(errorArea);
        Button closeButton = new Button(Translator.translate("Close"), event -> close());
        getFooter().add(closeButton);
    }

    public void setCallback(Runnable callback) {
        this.callback = callback;
    }
}
