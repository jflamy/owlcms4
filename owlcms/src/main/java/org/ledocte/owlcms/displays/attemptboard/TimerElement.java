/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Affero GNU License amended with the
 * Commons Clause.
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.displays.attemptboard;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.templatemodel.TemplateModel;

import ch.qos.logback.classic.Logger;

/**
 * Countdown timer element.
 */
@SuppressWarnings("serial")
@Tag("timer-element")
@HtmlImport("frontend://components/TimerElement.html")
public class TimerElement extends PolymerTemplate<TimerElement.TimerModel> {

	final private static Logger logger = (Logger)LoggerFactory.getLogger(TimerElement.class);
    
    /**
     * The Interface TimerModel.
     */
    public interface TimerModel extends TemplateModel {

        /**
         * Gets the current time.
         *
         * @return the current time
         */
        double getCurrentTime();
        
        /**
         * Sets the current time.
         *
         * @param seconds the new current time
         */
        void setCurrentTime(double seconds);
        
        /**
         * Gets the start time.
         *
         * @return the start time
         */
        double getStartTime();
        
        /**
         * Sets the start time.
         *
         * @param seconds the new start time
         */
        void setStartTime(double seconds);
        
        /**
         * Checks if is running.
         *
         * @return true, if is running
         */
        boolean isRunning();
        
        /**
         * Sets the running.
         *
         * @param running the new running
         */
        void setRunning(boolean running);
        
        /**
         * Checks if is count up.
         *
         * @return true, if is count up
         */
        boolean isCountUp();
        
        /**
         * Sets the count up.
         *
         * @param countUp the new count up
         */
        void setCountUp(boolean countUp);
        
        /**
         * Checks if is interactive.
         *
         * @return true, if is interactive
         */
        boolean isInteractive();
        
        /**
         * Sets the interactive.
         *
         * @param interactive the new interactive
         */
        void setInteractive(boolean interactive);
    }

	/**
	 * Instantiates a new timer element.
	 */
	public TimerElement() {
    	//new Exception().printStackTrace();
    	double seconds = 45.00D;
		TimerModel model = getModel();
		model.setStartTime(seconds);
    	model.setCurrentTime(seconds);
    	model.setCountUp(false);
    	model.setRunning(false);
    	//model.setInteractive(true);
    	
    	this.getElement().addPropertyChangeListener("running","running-changed", (e) -> {
    		logger.info(e.getPropertyName()+" changed to "+e.getValue()+" isRunning()="+this.getModel().isRunning());
    	});
    }
    
    /**
     * Start.
     */
    public void start() {
    	getElement().callFunction("start");
    }
    
    /**
     * Pause.
     */
    public void pause() {
    	getElement().callFunction("pause");
    }
    
    /**
     * Timer stopped.
     *
     * @param remainingTime the remaining time
     */
    @ClientCallable
    public void timerStopped(double remainingTime) {
    	logger.info("timer stopped "+remainingTime);
    }
}
