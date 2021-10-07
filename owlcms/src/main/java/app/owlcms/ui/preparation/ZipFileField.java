package app.owlcms.ui.preparation;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.customfield.CustomField;

import app.owlcms.i18n.Translator;

@SuppressWarnings("serial")
public class ZipFileField extends CustomField<byte[]>{
    
    ZipFileField() {
        Button uploadButton = new Button(Translator.translate("Config.Select"),
                buttonClickEvent -> new LocalOverrideUploadDialog(this).open());
        add(uploadButton);
    }

    
    @Override
    protected byte[] generateModelValue() {
        // not used the field is updated by the upload using setValue.
        return null;
    }

    @Override
    protected void setPresentationValue(byte[] newPresentationValue) {
        // not used we only display a hardwired button
    }

}
