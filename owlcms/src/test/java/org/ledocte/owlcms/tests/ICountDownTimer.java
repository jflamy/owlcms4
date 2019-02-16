package org.ledocte.owlcms.tests;

public interface ICountDownTimer {

	void start();

	void stop();

	int getTimeRemaining();

	void setTimeRemaining(int timeRemaining);

}