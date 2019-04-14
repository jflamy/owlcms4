package app.owlcms.fieldofplay;

import org.slf4j.LoggerFactory;

import app.owlcms.fieldofplay.FOPEvent.BreakStarted;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class BreakRelayTimer extends RelayTimer {

	final private Logger breakLogger = (Logger) LoggerFactory.getLogger(BreakRelayTimer.class);
	{ breakLogger.setLevel(Level.DEBUG); }

	public BreakRelayTimer(FieldOfPlay fop) {
		super(fop);
	}

	/* (non-Javadoc)
	 * @see app.owlcms.fieldofplay.ICountdownTimer#setTimeRemaining(int)
	 */
	@Override
	public void setTimeRemaining(int timeRemaining) {
		breakLogger.debug("{} -- timeRemaining = {} [{}]", this, timeRemaining, LoggerUtils.whereFrom());
		this.timeRemaining = timeRemaining;
	}
	

	/* only for testing
	 * @see app.owlcms.fieldofplay.RelayTimer#start()
	 */
	@Override
	public void start() {
		throw new UnsupportedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see app.owlcms.fieldofplay.RelayTimer#start()
	 */
	@Override
	public void start(FOPEvent e) {
		startMillis = System.currentTimeMillis();
		breakLogger.debug("{} start = {} [{}]", this, timeRemaining, LoggerUtils.whereFrom());
		fop.getUiEventBus().post(new UIEvent.BreakStarted((BreakStarted) e, e.getOrigin()));
	}


	/* (non-Javadoc)
	 * @see app.owlcms.fieldofplay.RelayTimer#stop()
	 */
	@Override
	public void stop() {
		stopMillis =  System.currentTimeMillis();
		long elapsed = stopMillis-startMillis;
		timeRemaining = (int) (timeRemaining - elapsed);
		breakLogger.debug("{} stop = {} [{}]", this, timeRemaining, LoggerUtils.whereFrom());
		fop.getUiEventBus().post(new UIEvent.BreakPaused(this));
	}
}
