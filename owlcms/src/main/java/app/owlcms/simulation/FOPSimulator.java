/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.simulation;

import java.util.List;
import java.util.Random;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.uievents.UIEvent;
import ch.qos.logback.classic.Logger;

/**
 *
 * Simulate the flow of a competition on a field of play.
 *
 * The actions of technical officials are simulated: the the events that the user interface would send (FOPEvents) are
 * posted The state automaton in the FieldOfPlay triggers the user interface updates as required. It is therefore
 * possible to create as many real browser windows as required to observe the updates taking place.
 *
 * @author Jean-François Lamy
 *
 */
public class FOPSimulator {

    static private Random r = new Random(0);

    final private Logger logger = (Logger) LoggerFactory.getLogger(FOPSimulator.class);

    final private Logger uiEventLogger = (Logger) LoggerFactory.getLogger("Simulation-" + logger.getName());

    private FieldOfPlay fop;

//    private EventBus fopEventBus;

    private List<Group> groups;

    private Object origin;

    private EventBus uiEventBus;

    private boolean groupDone;

    public FOPSimulator(FieldOfPlay f, List<Group> groups) {
        this.fop = f;
        this.groups = groups;
    }

    public void go() throws InterruptedException {
        uiEventBus = fop.getUiEventBus();
        uiEventBus.register(this);
        this.setOrigin(this);

        logger.info("****** simulating fop {}", fop.getName());
        startNextGroup(groups);
    }

    @Subscribe
    public void slaveDecisionReset(UIEvent.DecisionReset e) throws InterruptedException {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
        new Thread(() -> {
            if (groupDone) {
                if (groups.size() > 0) {
                    groups.remove(0);
                    startNextGroup(groups);
                } else {
                    return;
                }
            } else {
                doNextAthlete(e);
            }
        }).start();
    }

    @Subscribe
    public void slaveDownSignal(UIEvent.DownSignal e) {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
        // nothing to do, wait for decision reset
    }

    @Subscribe
    public void slaveGroupDone(UIEvent.GroupDone e) throws InterruptedException {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());

        // this event happens several times per group due to the possibility of jury reversal of the last
        // lift. So we wait for the first decision after group done (i.e. the decision for the last athlete)
        // to do anything

        // note that the group is done.
        groupDone = true;
        new Thread(() -> {
            logger.info("########## group {} done", e.getGroup());
            if (groups.size() > 0) {
                groups.remove(0);
                startNextGroup(groups);
            } else {
                return;
            }
        }).start();
    }

    @Subscribe
    public void slaveOrderUpdated(UIEvent.LiftingOrderUpdated e) {
        // nothing to do
    }

    @Subscribe
    public void slaveRefereeDecision(UIEvent.Decision e) {
        // nothing to do
    }

    @Subscribe
    public void slaveStartBreak(UIEvent.BreakStarted e) {
        // nothing to do
    }

    @Subscribe
    public void slaveStartLifting(UIEvent.StartLifting e) throws InterruptedException {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
        new Thread(() -> doNextAthlete(e)).start();
    }

    @Subscribe
    public void slaveStopBreak(UIEvent.BreakDone e) {
        // nothing to do
    }

    @Subscribe
    public void slaveSwitchGroup(UIEvent.SwitchGroup e) throws InterruptedException {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
        new Thread(() -> doSwitchGroup(e)).start();
    }

    public void unregister() {
        logger.debug("***** unregister simulator {}", this.fop.getName());
        uiEventBus.unregister(this);
    }

    protected void doEmpty() {
    }

    protected void doLift(Athlete a) {
        if (a == null) {
            doEmpty();
            return;
        } else if (a.getAttemptsDone() >= 6) {
            // do nothing. wait on decision reset for last athlete.
            // doDone(fop.getGroup());
            return;
        }

        // do a lift in group g: start timer
        fop.fopEventPost(new FOPEvent.TimeStarted(this));

        // wait for clock to run down a bit
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }

        // stop time and get decisions
        fop.fopEventPost(new FOPEvent.TimeStopped(this));
        fop.fopEventPost(new FOPEvent.DecisionUpdate(this, 0, goodLift(r)));
        fop.fopEventPost(new FOPEvent.DecisionUpdate(this, 1, goodLift(r)));
        fop.fopEventPost(new FOPEvent.DecisionUpdate(this, 2, goodLift(r)));
    }

    Object getOrigin() {
        return this.origin;
    }

    private void doNextAthlete(UIEvent e) {
        List<Athlete> order = fop.getLiftingOrder();
        Athlete athlete = order.size() > 0 ? order.get(0) : null;

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e1) {
        }
        doLift(athlete);
    }

    private void doSwitchGroup(UIEvent.SwitchGroup e) {
        switch (fop.getState()) {
        case INACTIVE:
            doEmpty();
            break;
        case BREAK:
            if (e.getGroup() == null) {
                doEmpty();
            } else {
                // doBreak();
            }
            break;
        default:
            // doLift(fop.getCurAthlete());
        }
    }

    private boolean goodLift(Random r) {
        return r.nextFloat() < 0.7;
    }

    private void setOrigin(Object origin) {
        this.origin = origin;
    }

    private boolean startNextGroup(List<Group> curGs) {
        if (curGs.size() > 0) {
            Group g = curGs.get(0);
            logger.info("########## waiting to start group {} of {}", g, curGs);
            try {
                Thread.sleep(6000);
            } catch (InterruptedException e) {
            }
            logger.info("########## switching group {} of {}", g, curGs);
            fop.fopEventPost(new FOPEvent.SwitchGroup(g, this));
            logger.info("########## starting group");
            groupDone = false;
            fop.fopEventPost(new FOPEvent.StartLifting(this));

            return true;
        } else {
            return false;
        }
    }

}
