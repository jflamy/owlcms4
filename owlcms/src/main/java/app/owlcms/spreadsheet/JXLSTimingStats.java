/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.spreadsheet;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.group.Group;
import ch.qos.logback.classic.Logger;

/**
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public class JXLSTimingStats extends JXLSWorkbookStreamSource {
	
    Logger logger = (Logger) LoggerFactory.getLogger(JXLSTimingStats.class);

    public class SessionStats {

        @Override
        public String toString() {
            double hours = (maxTime.getTime()-minTime.getTime())/1000.0/60.0/60.0;
            return "SessionStats [groupName=" + getGroupName() + ", nbAthletes=" + nbAthletes + ", minTime=" + minTime + ", maxTime=" + maxTime //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                    + ", nbAttemptedLifts=" + nbAttemptedLifts + " Hours=" + hours+ " AthletesPerHour=" + nbAthletes/hours+ "]" ; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        }

        String groupName = null;
        int nbAthletes;
        Date maxTime = new Date(0L); // forever ago
        Date minTime = new Date(); // now
        int nbAttemptedLifts;

        public SessionStats() {
        }

        public SessionStats(String groupName) {
            this.setGroupName(groupName);
        }

        public Date getMaxTime() {
            return maxTime;
        }

        public Date getMinTime() {
            return minTime;
        }

        public int getNbAttemptedLifts() {
            return nbAttemptedLifts;
        }

        public int getNbAthletes() {
            return nbAthletes;
        }

        public void setMaxTime(Date maxTime) {
            this.maxTime = maxTime;
        }

        public void setMinTime(Date minTime) {
            this.minTime = minTime;
        };

        public void setNbAttemptedLifts(int nbAttemptedLifts) {
            this.nbAttemptedLifts = nbAttemptedLifts;
        }

        public void setNbAthletes(int nbAthletes) {
            this.nbAthletes = nbAthletes;
        }

        public void updateMaxTime(Date newTime) {
            if (this.maxTime.compareTo(newTime) < 0) {
                this.maxTime = newTime;
            } else {
            }

        }

        public void updateMinTime(Date newTime) {
            if (this.minTime.compareTo(newTime) > 0) {
                this.minTime = newTime;
            } else {
            }

        }

        public String getGroupName() {
            return groupName;
        }

        public void setGroupName(String groupName) {
            this.groupName = groupName;
        }
    }

    public JXLSTimingStats() {
        super();
    }

    public JXLSTimingStats(Group group, boolean excludeNotWeighed) {
        super();
    }

    @Override
    protected List<Athlete> getSortedAthletes() {
        HashMap<String, Object> reportingBeans = getReportingBeans();

        List<Athlete> athletes = AthleteSorter.registrationOrderCopy(AthleteRepository.findAllByGroupAndWeighIn(null,isExcludeNotWeighed()));
        if (athletes.isEmpty()) {
            // prevent outputting silliness.
            throw new RuntimeException(""); //$NON-NLS-1$
        }

        // extract group stats
        Group curGroup = null;
        Group prevGroup = null;

        List<SessionStats> sessions = new LinkedList<SessionStats>();

        SessionStats curStat = null;
        for (Athlete curAthlete : athletes) {
            curGroup = curAthlete.getGroup();
            if (curGroup == null) {
                continue;  // we simply skip over athletes with no groups
            }
            if (curGroup != prevGroup) {
                processGroup(sessions, curStat);

                String name = curGroup.getName();
                curStat = new SessionStats(name);
            }
            // update stats, min, max.
            curStat.setNbAthletes(curStat.getNbAthletes() + 1);
            Date minTime = curAthlete.getFirstAttemptedLiftTime();
            curStat.updateMinTime(minTime);

            Date maxTime = curAthlete.getLastAttemptedLiftTime();
            curStat.updateMaxTime(maxTime);

            int nbAttemptedLifts = curAthlete.getAttemptedLifts();
            curStat.setNbAttemptedLifts(curStat.getNbAttemptedLifts() + nbAttemptedLifts);

            prevGroup = curGroup;
        }
        if (curStat.getNbAthletes() > 0) {
            processGroup(sessions, curStat);
        }
        reportingBeans.put("groups", sessions); //$NON-NLS-1$
        return athletes;
    }

    @Override
    public InputStream getTemplate(Locale locale) throws IOException {
        String templateName = "/timing/TimingStatsTemplate_" + locale.getLanguage() + ".xls"; //$NON-NLS-1$ //$NON-NLS-2$
        final InputStream resourceAsStream = this.getClass().getResourceAsStream(templateName);
        if (resourceAsStream == null) {
            throw new IOException("resource not found: " + templateName);} //$NON-NLS-1$
        return resourceAsStream;
    }

    private void processGroup(List<SessionStats> sessions, SessionStats curStat) {
        if (curStat == null) return;
        sessions.add(curStat);
    }

}
