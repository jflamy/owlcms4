/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.data.competition;

import com.vaadin.flow.component.textfield.TextField;

@SuppressWarnings("serial")
public class DebuggingTextField extends TextField {
    @Override
    public void setInvalid(boolean invalid) {
        System.err.println("setting invalid=" + invalid);
        super.setInvalid(invalid);
    }
}
