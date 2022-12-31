/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
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

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.category.Category;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.utils.NaturalOrderComparator;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 *
 * Simulate a meet by triggering events and reacting to response.
 *
 * @author Jean-François Lamy
 *
 */
public class CompetitionSimulator {

    final private static Logger logger = (Logger) LoggerFactory.getLogger(CompetitionSimulator.class);

    private static List<FOPSimulator> registeredSimulators = new ArrayList<>();

    private Random r = new Random(0);

    public CompetitionSimulator() {
    }

    public String runSimulation() throws InterruptedException {
        logger.setLevel(Level.DEBUG);

        Map<Platform, List<Group>> groupsByPlatform = new TreeMap<>();
        List<Platform> ps = PlatformRepository.findAll().stream().collect(Collectors.toList());
        List<Group> gs = GroupRepository.findAll().stream().sorted(new NaturalOrderComparator<>()).sorted((a, b) -> {
            LocalDateTime ta = a.getCompetitionTime();
            LocalDateTime tb = b.getCompetitionTime();
            return ObjectUtils.compare(ta, tb, true);
        }).collect(Collectors.toList());

        clearLifts();

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

        for (FOPSimulator s : registeredSimulators) {
            s.unregister();
        }
        registeredSimulators.clear();

        for (Platform p : ps) {
            FieldOfPlay f = OwlcmsFactory.getFOPByName(p.getName());
            FOPSimulator fopSimulator = new FOPSimulator(f, groupsByPlatform.get(p));
            registeredSimulators.add(fopSimulator);
            fopSimulator.go();
        }
        return "simulation done.";
    }

    private void clearLifts() {
        JPAService.runInTransaction(em -> {
            List<Athlete> athletes = AthleteRepository.doFindAll(em);
            for (Athlete a : athletes) {
                a.clearLifts();
                em.merge(a);
            }
            em.flush();
            return null;
        });
    }

    private List<Athlete> weighIn(Group g) {
        List<Athlete> as = AthleteRepository.findAllByGroupAndWeighIn(g, null);
        for (Athlete a : as) {
            Category c = a.getCategory();
            Double catLimit = c.getMaximumWeight();
            if (catLimit > 998) {
                catLimit = c.getMinimumWeight() * 1.1;
            }
            double bodyWeight = catLimit - (r.nextDouble() * 2.0);
            a.setBodyWeight(bodyWeight);
            
            
            Integer entryTotal = a.getEntryTotal();
            if (entryTotal != null && entryTotal > 0) {
                long isd = Math.round(entryTotal * 0.44D); // qualification snatch
                long icjd  = Math.round(entryTotal * 0.56D); // qualification CJ
                a.setSnatch1Declaration(Long.toString(isd));
                a.setCleanJerk1Declaration(Long.toString(icjd));
                AthleteRepository.save(a);
            } else {
                double sd = catLimit * (1 + (r.nextGaussian() / 10));
                long isd = Math.round(sd);
                a.setSnatch1Declaration(Long.toString(isd));
                long icjd = Math.round(sd * 1.20D);
                a.setCleanJerk1Declaration(Long.toString(icjd));
                AthleteRepository.save(a);
            }
        }
        return as;
    }

}
