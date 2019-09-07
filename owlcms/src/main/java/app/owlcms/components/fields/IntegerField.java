package app.owlcms.components.fields;

import java.text.NumberFormat;

import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.textfield.TextField;

@SuppressWarnings("serial")
public class IntegerField extends CustomField<Integer> {
    
    private final TextField wrappedField = new TextField();
    private NumberFormat format = NumberFormat.getIntegerInstance();
    
    public IntegerField() {
        add(wrappedField);
        format.setGroupingUsed(false);
    }

    @Override
    protected Integer generateModelValue() {
        return Integer.parseInt(wrappedField.getValue());
    }

    @Override
    protected void setPresentationValue(Integer newPresentationValue) {
        wrappedField.setValue(format.format(newPresentationValue));
    }

}
