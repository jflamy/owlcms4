/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.io.InputStream;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;

import app.owlcms.i18n.Translator;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class LocalOverrideUploadDialog extends Dialog {

    final static Logger logger = (Logger) LoggerFactory.getLogger(LocalOverrideUploadDialog.class);

    public LocalOverrideUploadDialog(ZipFileField f) {

//        H5 label = new H5(Translator.translate("Upload.WarningWillReplaceAll"));
//        label.getStyle().set("color", "red");

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setWidth("40em");

        TextArea ta = new TextArea(getTranslation("Errors"));
        ta.setHeight("20ex");
        ta.setWidth("80em");
        ta.setVisible(false);

        upload.addSucceededListener(event -> {
            logger.info("zip type {}", event.getMIMEType());
            processInput(event.getFileName(), buffer.getInputStream(), ta, f);
            if (ta.isEmpty()) {
                this.close();
            }
        });
        upload.setAcceptedFileTypes("application/zip", "application/x-zip-compressed");

        upload.setUploadButton(new Button(Translator.translate("Config.Select")));

        upload.addStartedListener(event -> {
            ta.clear();
            ta.setVisible(false);
        });

        H3 title = new H3(getTranslation("Config.Select"));
        VerticalLayout vl = new VerticalLayout(title, upload, ta);
        add(vl);
    }

    private void appendErrors(TextArea ta, String sb) {
        if (sb.length() > 0) {
            ta.setValue(sb.toString());
            ta.setVisible(true);
        }
    }

    private void processInput(String fileName, InputStream inputStream, TextArea ta, ZipFileField f) {
        try (inputStream) {
            byte[] bytes = inputStream.readAllBytes();
            ResourceWalker.unzipBlobToTemp(bytes);
            f.setValue(bytes);
        } catch (Exception e) {
            String localizedMessage = e.getLocalizedMessage();
            appendErrors(ta, localizedMessage != null ? localizedMessage : e.toString());
            logger.error("{}", LoggerUtils.stackTrace(e));
        }
        return;
    }

}
