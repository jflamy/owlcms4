/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.simulation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.category.Category;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.ui.shared.BreakManagement.CountdownType;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.UIEvent;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 *
 * Simulate a meet by triggering events and reacting to response.
 *
 * @author Jean-François Lamy
 *
 */
public class FOPSimulator implements Runnable {

    final private static Logger logger = (Logger) LoggerFactory.getLogger(FOPSimulator.class);

    static private Random r = new Random(0);

    final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("Simulation-" + logger.getName());

    static public void runSimulation() {
        Map<Platform, List<Group>> groupsByPlatform = new TreeMap<>();

        logger.setLevel(Level.DEBUG);
        uiEventLogger.setLevel(Level.DEBUG);

        // only use one platform for now.
        // List<Platform> ps = PlatformRepository.findAll().stream().limit(1).collect(Collectors.toList());

        List<Platform> ps = PlatformRepository.findAll().stream().collect(Collectors.toList());
        List<Group> gs = GroupRepository.findAll().stream().sorted((a, b) -> {
            LocalDateTime ta = a.getCompetitionTime();
            LocalDateTime tb = b.getCompetitionTime();
            return ObjectUtils.compare(ta, tb, true);
        }).collect(Collectors.toList());

        int i = 0;
        for (Group g : gs) {

            List<Athlete> as = AthleteRepository.findAllByGroupAndWeighIn(g, true);

            if (as.size() == 0) {
                as = weighIn(g);
            }
            as = AthleteRepository.findAllByGroupAndWeighIn(g, true);
            if (as.size() == 0) {
                logger.info("skipping group {} size {}", g.getName(), as.size());
                continue;
            }
            logger.info("group {} size {} platform {}", g.getName(), as.size(), g.getPlatform());

            int index;

            Platform curP;
            curP = g.getPlatform();
            if (curP == null) {
                index = i % ps.size();
                curP = ps.get(index);
                i++;
            }

            List<Group> curGroupList = groupsByPlatform.get(curP);
            if (curGroupList == null) {
                curGroupList = new ArrayList<>();
            }

            logger.info("platform {}", curP.getName());
            if (as.size() > 0) {
                curGroupList.add(g);
                groupsByPlatform.put(curP, curGroupList);
                logger.info("platform {} groups {}", curP.getName(), groupsByPlatform.get(curP));
            } 
        }

        for (Platform p : ps) {
            FieldOfPlay f = OwlcmsFactory.getFOPByName(p.getName());
            FOPSimulator fopSimulator = new FOPSimulator(f, groupsByPlatform.get(p));
            fopSimulator.run();
        }
    }

    private static List<Athlete> weighIn(Group g) {
        List<Athlete> as = AthleteRepository.findAllByGroupAndWeighIn(g, null);
        for (Athlete a : as) {
            Category c = a.getCategory();
            Double catLimit = c.getMaximumWeight();
            if (catLimit > 998) {
                catLimit = c.getMinimumWeight() * 1.1;
            }
            double bodyWeight = catLimit - (r.nextDouble() * 2.0);
            a.setBodyWeight(bodyWeight);
            double sd = catLimit * (1 + (r.nextGaussian() / 10));
            long isd = Math.round(sd);
            a.setSnatch1Declaration(Long.toString(isd));
            long icjd = Math.round(sd * 1.20D);
            a.setCleanJerk1Declaration(Long.toString(icjd));
            AthleteRepository.save(a);
        }
        return as;
    }

    private FieldOfPlay fop;

    private EventBus fopEventBus;

    private List<Group> groups;

    private Object origin;

    private EventBus uiEventBus;

    public FOPSimulator(FieldOfPlay f, List<Group> groups) {
        this.fop = f;
        this.groups = groups;
    }

    @Override
    public void run() {
        fopEventBus = fop.getFopEventBus();
        fopEventBus.register(this);
        uiEventBus = fop.getUiEventBus();
        uiEventBus.register(this);
        this.setOrigin(this);

        logger.debug("simulating fop {}", fop);
        startNextGroup(groups);
    }

    @Subscribe
    public void slaveDecisionReset(UIEvent.DecisionReset e) {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
        doNextAthlete(e);
    }

    /**
     * Multiple attempt boards and athlete-facing boards can co-exist. We need to show down on the slave devices -- the
     * master device is the one where refereeing buttons are attached.
     *
     * @param e
     */
    @Subscribe
    public void slaveDownSignal(UIEvent.DownSignal e) {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
        // nothing to do, wait for decision reset
    }

    @Subscribe
    public void slaveGroupDone(UIEvent.GroupDone e) {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
        doDone(e.getGroup());
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
    public void slaveStartLifting(UIEvent.StartLifting e) {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
        doNextAthlete(e);
    }

    @Subscribe
    public void slaveStopBreak(UIEvent.BreakDone e) {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
    }

    @Subscribe
    public void slaveSwitchGroup(UIEvent.SwitchGroup e) {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());

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
            doLift(fop.getCurAthlete());
        }
    }

    protected void doBreak(FieldOfPlay fop) {
        fop.fopEventPost(new FOPEvent.BreakStarted(BreakType.FIRST_SNATCH, CountdownType.DURATION, 5000, null, fop));
    }

    protected void doEmpty() {
    }

    protected void doLift(Athlete a) {
        if (a == null) {
            doEmpty();
            return;
        } else if (a.getAttemptsDone() >= 6) {
            doDone(fop.getGroup());
            return;
        }

        // do a lift in group g
        fop.fopEventPost(new FOPEvent.TimeStarted(this));
        try {
            // wait for clock to run down a bit
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
        fop.fopEventPost(new FOPEvent.TimeStopped(this));
        fop.fopEventPost(new FOPEvent.DecisionUpdate(this, 0, goodLift(r)));
        fop.fopEventPost(new FOPEvent.DecisionUpdate(this, 1, goodLift(r)));
        fop.fopEventPost(new FOPEvent.DecisionUpdate(this, 2, goodLift(r)));
    }

    Object getOrigin() {
        return this.origin;
    }

    private Object doDone(Group group) {
        logger.info("########## group {} done", group);
        if (groups.size() > 0 && groups.get(0).equals(group)) {
            groups.remove(0);
            startNextGroup(groups);
        } else {
            try {
                // wait for decision reset and display to update
                Thread.sleep(5 * 1000);
            } catch (InterruptedException e) {
            }
        }
        return null;
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

    private boolean goodLift(Random r) {
        return r.nextFloat() < 0.7;
    }

    private void setOrigin(Object origin) {
        this.origin = origin;
    }

    private boolean startNextGroup(List<Group> curGs) {
        if (curGs.size() > 0) {
            Group g = curGs.get(0);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
            logger.info("########## starting group {} of {}", g, curGs);
            fop.loadGroup(g, this, true);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            fop.startLifting(g, this);
            return true;
        } else {
            return false;
        }
    }

}
