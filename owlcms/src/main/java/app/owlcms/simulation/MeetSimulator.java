/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.fieldofplay.BreakType;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsFactory;
import ch.qos.logback.classic.Logger;

/**
 * @author "Jean-FranÃ§ois Lamy"
 *
 */
public class MeetSimulator implements Runnable {

    static <K, V> Map<V, K> invertMap(Map<K, V> map) {
        if (map == null) {
            return Collections.emptyMap();
        }
        return map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }

    private Logger logger = (Logger) LoggerFactory.getLogger(MeetSimulator.class);

    @Override
    public void run() {
        logger.warn("run simulation");
        Random r = new Random(0);
        // use first platform only for now
        List<Platform> ps = PlatformRepository.findAll().stream().limit(1).collect(Collectors.toList());
        List<Group> gs = GroupRepository.findAll();
        logger.warn("gs {}", gs);
        Map<Platform, List<Group>> p2gs = new TreeMap<>();

        int i = 0;
        for (Group g : gs) {
            logger.warn("g {}", g);
            Platform curP = ps.get(i % ps.size());
            List<Group> curGroupList = p2gs.get(curP);
            if (curGroupList == null) {
                curGroupList = new ArrayList<>();
            }
            curGroupList.add(g);
            p2gs.put(curP, curGroupList);
        }

        boolean done = false;

        // pick a platform at random -- this will go inside the loop
        Platform p = ps.get(0); // only one for now
        FieldOfPlay f = OwlcmsFactory.getFOPByName(p.getName());
        logger.warn("platform {}  fop {}", p, f);

        while (!done) {
            // take the first group from the valueset for the platform currently lifting
            List<Group> curGs = p2gs.get(p);
            if (curGs.size() > 0) {
                Group g = curGs.get(0);
                f.loadGroup(g, this);
                if (f.getCurAthlete() != null) {
                    logger.warn("simulating group {}", g);
                    f.getFopEventBus().post(new FOPEvent.StartLifting(this));
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                    }

                    FOPState state = f.getState();

                    // do a lift in group g
                    f.getFopEventBus().post(new FOPEvent.TimeStarted(this));
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                    }
                    f.getFopEventBus().post(new FOPEvent.TimeStopped(this));
                    f.getFopEventBus().post(new FOPEvent.DecisionUpdate(this, 0, goodLift(r)));
                    f.getFopEventBus().post(new FOPEvent.DecisionUpdate(this, 1, goodLift(r)));
                    f.getFopEventBus().post(new FOPEvent.DecisionUpdate(this, 2, goodLift(r)));
                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException e) {
                    }

                    if ((state == FOPState.BREAK && f.getBreakType() == BreakType.GROUP_DONE)) {
                        // if the group is done, remove the group from the map
                        curGs.remove(0);
                        done = curGs.size() == 0;
                        p2gs.put(p, curGs);
                    }
                }
            }
        }

    }

    private boolean goodLift(Random r) {
        return r.nextFloat() < 0.7;
    }

}
