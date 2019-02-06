/*
 * Copyright 2000-2017 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.ledocte.owlcms.ui.attemptboard;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.templatemodel.TemplateModel;

/**
 * Countdown timer element.
 */
@Tag("decision-element")
@HtmlImport("frontend://components/DecisionElement.html")
public class DecisionElement extends PolymerTemplate<DecisionElement.DecisionModel> {

	private static final long serialVersionUID = 1L;
    
    public interface DecisionModel extends TemplateModel {
        Boolean isRef1();
        void setRef1(Boolean running);
        
        Boolean isRef2();
        void setRef2(Boolean running);
        
        Boolean isRef3();
        void setRef3(Boolean running);
    }

	@SuppressWarnings("deprecation")
	public DecisionElement() {
		DecisionModel model = getModel();
    	model.setRef1(null);
    	model.setRef2(null);
    	model.setRef3(null);
    	
    	this.getElement().synchronizeProperty("ref1","ref1-changed");
    	this.getElement().addPropertyChangeListener("ref1", (e) -> {
    		System.err.println(e.getPropertyName()+" changed to "+e.getValue());
    	});
    	this.getElement().synchronizeProperty("ref2","ref2-changed");
    	this.getElement().addPropertyChangeListener("ref2", (e) -> {
    		System.err.println(e.getPropertyName()+" changed to "+e.getValue());
    	});
    	this.getElement().synchronizeProperty("ref3","ref3-changed");
    	this.getElement().addPropertyChangeListener("ref3", (e) -> {
    		System.err.println(e.getPropertyName()+" changed to "+e.getValue());
    	});
    	this.getElement().synchronizeProperty("decision","decision-changed");
    	this.getElement().addPropertyChangeListener("decision", (e) -> {
    		System.err.println(e.getPropertyName()+" changed to "+e.getValue());
    	});
    }
    
    public void reset() {
    	getElement().callFunction("reset");
    }
    
    @ClientCallable
    public void decisionMade(boolean decision) {
    	System.err.println("decision made "+decision);
    }
}
