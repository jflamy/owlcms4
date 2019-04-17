/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/master/License.txt
 */
package app.owlcms.tests;

public interface ICountDownTimer {

	void start();

	void stop();

	int getTimeRemaining();

	void setTimeRemaining(int timeRemaining);

}