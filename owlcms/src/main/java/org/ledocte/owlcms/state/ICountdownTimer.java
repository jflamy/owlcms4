package org.ledocte.owlcms.state;

public interface ICountdownTimer {

	public void start();

	public void stop();

	public int getTimeRemaining();

	public void setTimeRemaining(int timeRemaining);

}