/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.elements;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.templatemodel.TemplateModel;

import app.owlcms.ui.shared.SafeEventBusRegistration;

/**
 * ExplicitDecision display element.
 */
@SuppressWarnings("serial")
@Tag("beep-element")
@JsModule("./components/BeepElement.js")
public class BeepElement extends PolymerTemplate<BeepElement.BeepModel>
        implements SafeEventBusRegistration {

    /**
     * The Interface BeepModel.
     */
    public interface BeepModel extends TemplateModel {
        void setSilent(boolean b);
    }

    public void beep() {
        this.getElement().callJsFunction("beep");
    }

    /*
     * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component. AttachEvent)
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        getModel().setSilent(false);
    }
}
