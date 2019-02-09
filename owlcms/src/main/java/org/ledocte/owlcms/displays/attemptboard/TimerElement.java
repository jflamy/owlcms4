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
package org.ledocte.owlcms.displays.attemptboard;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.templatemodel.TemplateModel;

/**
 * Countdown timer element.
 */
@Tag("timer-element")
@HtmlImport("frontend://components/TimerElement.html")
public class TimerElement extends PolymerTemplate<TimerElement.TimerModel> {

	private static final long serialVersionUID = 1L;
    
    public interface TimerModel extends TemplateModel {

        double getCurrentTime();
        void setCurrentTime(double seconds);
        
        double getStartTime();
        void setStartTime(double seconds);
        
        boolean isRunning();
        void setRunning(boolean running);
        
        boolean isCountUp();
        void setCountUp(boolean countUp);
        
        boolean isInteractive();
        void setInteractive(boolean interactive);
    }

	@SuppressWarnings("deprecation")
	public TimerElement() {
    	//new Exception().printStackTrace();
    	double seconds = 45.00D;
		TimerModel model = getModel();
		model.setStartTime(seconds);
    	model.setCurrentTime(seconds);
    	model.setCountUp(false);
    	model.setRunning(false);
    	//model.setInteractive(true);
    	
    	this.getElement().synchronizeProperty("running","running-changed");
    	this.getElement().addPropertyChangeListener("running", (e) -> {
    		System.err.println(e.getPropertyName()+" changed to "+e.getValue()+" isRunning()="+this.getModel().isRunning());
    	});
    }
    
    public void start() {
    	getElement().callFunction("start");
    }
    
    public void pause() {
    	getElement().callFunction("pause");
    }
    
    @ClientCallable
    public void timerStopped(double remainingTime) {
    	System.err.println("timer stopped "+remainingTime);
    }
}
