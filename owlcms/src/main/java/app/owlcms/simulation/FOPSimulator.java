/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.stream.Collectors;

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
import app.owlcms.init.OwlcmsSession;
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

    final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("Simulation-" + logger.getName());

    static Map<Platform, List<Group>> groupsByPlatform = new TreeMap<>();

    private static Random r = new Random(0);

    public static void runSimulation() {
        logger.setLevel(Level.DEBUG);
        uiEventLogger.setLevel(Level.DEBUG);
        try {
            Thread.sleep(15000);
        } catch (InterruptedException e) {
        }

        // only use one platform for now.
        List<Platform> ps = PlatformRepository.findAll().stream().limit(1).collect(Collectors.toList());
        List<Group> gs = GroupRepository.findAll();

        int i = 0;
        for (Group g : gs) {
            Platform curP = ps.get(i % ps.size());
            List<Group> curGroupList = groupsByPlatform.get(curP);
            if (curGroupList == null) {
                curGroupList = new ArrayList<>();
            }
            List<Athlete> as = AthleteRepository.findAllByGroupAndWeighIn(g, true);
            if (as.size() == 0) {
                as = weighIn(g);
            }
            if (as.size() > 0) {
                curGroupList.add(g);
                groupsByPlatform.put(curP, curGroupList);
                logger.debug("platform {} groups {}", System.identityHashCode(curP), groupsByPlatform.get(curP));
            }
        }

        // use first platform only for now
//        for (Platform p: ps) {
//            FieldOfPlay f = OwlcmsFactory.getFOPByName(p.getName());
//            LATER create more listeners.
        FieldOfPlay f = OwlcmsFactory.getFOPByName(ps.get(0).getName());
        FOPSimulator fopSimulator = new FOPSimulator(f);
        fopSimulator.run();
//        }
    }

    static <K, V> Map<V, K> invertMap(Map<K, V> map) {
        if (map == null) {
            return Collections.emptyMap();
        }
        return map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
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

    private EventBus fopEventBus;

    private EventBus uiEventBus;

    private Object origin;

    private FieldOfPlay fop;

    private List<Group> curGs;

    public FOPSimulator(FieldOfPlay f) {
        this.fop = f;
    }

    @Override
    public void run() {
        fopEventBus = fop.getFopEventBus();
        fopEventBus.register(this);
        uiEventBus = fop.getUiEventBus();
        uiEventBus.register(this);
        this.setOrigin(this);

        logger.debug("simulating fop {}", fop);
        startNextGroup();
    }

    @Subscribe
    public void slaveDecisionReset(UIEvent.DecisionReset e) {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
        doFirstAthlete(e);
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
        startNextGroup();
    }

    @Subscribe
    public void slaveOrderUpdated(UIEvent.LiftingOrderUpdated e) {
        // nothing to do
    }

    /**
     * Multiple attempt boards and athlete-facing boards can co-exist. We need to show decisions on the slave devices --
     * the master device is the one where refereeing buttons are attached.
     *
     * @param e
     */
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
        doFirstAthlete(e);
    }

    @Subscribe
    public void slaveStopBreak(UIEvent.BreakDone e) {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
        doFirstAthlete(e);
    }

    @Subscribe
    public void slaveSwitchGroup(UIEvent.SwitchGroup e) {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
        OwlcmsSession.withFop(fop -> {
            switch (fop.getState()) {
            case INACTIVE:
                doEmpty();
                break;
            case BREAK:
                if (e.getGroup() == null) {
                    doEmpty();
                } else {
                    doBreak();
                }
                break;
            default:
                doAthleteUpdate(fop.getCurAthlete());
            }
        });
        // uiEventLogger./**/warn("#### reloading {}", this.getElement().getClass());
        // this.getElement().callJsFunction("reload");
    }

    protected void doAthleteUpdate(Athlete a) {
        logger.debug("$$$ a {}  ", a);

        if (a == null) {
            doEmpty();
            return;
        } else if (a.getAttemptsDone() >= 6) {
            OwlcmsSession.withFop((fop) -> doDone(fop.getGroup()));
            return;
        }

        // do a lift in group g
        fopEventBus.post(new FOPEvent.TimeStarted(this));
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
        fopEventBus.post(new FOPEvent.TimeStopped(this));
        fopEventBus.post(new FOPEvent.DecisionUpdate(this, 0, goodLift(r)));
        fopEventBus.post(new FOPEvent.DecisionUpdate(this, 1, goodLift(r)));
        fopEventBus.post(new FOPEvent.DecisionUpdate(this, 2, goodLift(r)));
    }

    protected void doBreak(FieldOfPlay fop) {
    }

    protected void doEmpty() {
    }

    Object getOrigin() {
        return this.origin;
    }

    private void doBreak() {
        // nothing for now
    }

    private Object doDone(Group group) {
        if (curGs.size() > 0) {
            curGs.remove(0);
            startNextGroup();
        }
        return null;
    }

    private void doFirstAthlete(UIEvent e) {
        Athlete a = e.getAthlete();
        if (a == null) {
            OwlcmsSession.withFop(fop -> {
                List<Athlete> order = fop.getLiftingOrder();
                Athlete athlete = order.size() > 0 ? order.get(0) : null;
                doAthleteUpdate(athlete);
            });
        } else {
            doAthleteUpdate(a);
        }
    }

    private boolean goodLift(Random r) {
        return r.nextFloat() < 0.7;
    }

    private void setOrigin(Object origin) {
        this.origin = origin;
    }

    private boolean startNextGroup() {
        curGs = groupsByPlatform.get(fop.getPlatform());
        if (curGs.size() > 0) {
            Group g = curGs.get(0);
            logger.info("########## starting group {} of {}", g, curGs);
            fop.startLifting(g, this);
            return true;
        } else {
            return false;
        }
    }

}
