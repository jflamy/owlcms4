package app.owlcms.data.competition;

import com.vaadin.flow.component.textfield.TextField;

@SuppressWarnings("serial")
public class DebuggingTextField extends TextField {
@Override
public void setInvalid(boolean invalid) {
    System.err.println("setting invalid="+invalid);
    super.setInvalid(invalid);
}
}
