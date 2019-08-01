/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.spreadsheet;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    public class SessionStats {

        String groupName = null;

        int nbAthletes;
        LocalDateTime maxTime = LocalDateTime.MIN; // forever ago
        LocalDateTime minTime = LocalDateTime.MAX; // long time in the future
        int nbAttemptedLifts;
        public SessionStats() {
        }

        public SessionStats(String groupName) {
            this.setGroupName(groupName);
        }

        public String getGroupName() {
            return groupName;
        }

        public LocalDateTime getMaxTime() {
            return maxTime;
        }

        public LocalDateTime getMinTime() {
            return minTime;
        }

        public int getNbAthletes() {
            return nbAthletes;
        }

        public int getNbAttemptedLifts() {
            return nbAttemptedLifts;
        }

        public String getSDuration() {
            Duration delta = Duration.between(minTime, maxTime);
            if (delta.isNegative()) {
                delta = Duration.ZERO;
            }
            return formatDuration(delta);
        }
        
        /**
         * @return duration as a fraction of day, for Excel
         */
        public Double getDayDuration() {
            Duration delta = Duration.between(minTime, maxTime);
            if (delta.isNegative()) {
                delta = Duration.ZERO;
                return null;
            }
            return  ((double)delta.getSeconds()/(24*3600));
        }


        public String getSMaxTime() {
            if (maxTime.isEqual(LocalDateTime.MIN)) return "-";
            return maxTime.format(DateTimeFormatter.ISO_LOCAL_TIME);
        }

        public String getSMinTime() {
            if (minTime.isEqual(LocalDateTime.MAX)) return "-";
            return minTime.format(DateTimeFormatter.ISO_LOCAL_TIME);
        }

        public void setGroupName(String groupName) {
            this.groupName = groupName;
        }

        public void setMaxTime(LocalDateTime maxTime) {
            this.maxTime = maxTime;
        }

        public void setMinTime(LocalDateTime minTime) {
            this.minTime = minTime;
        }

        public void setNbAthletes(int nbAthletes) {
            this.nbAthletes = nbAthletes;
        }

        public void setNbAttemptedLifts(int nbAttemptedLifts) {
            this.nbAttemptedLifts = nbAttemptedLifts;
        }

        @Override
        public String toString() {
            return "SessionStats [groupName=" + getGroupName() + ", nbAthletes=" + nbAthletes + ", minTime=" + minTime //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    + ", maxTime=" + maxTime //$NON-NLS-1$
                    + ", nbAttemptedLifts=" + nbAttemptedLifts + " Hours=" + getDayDuration() + " AthletesPerHour=" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    + getAthletesPerHour() + "]"; //$NON-NLS-1$
        }

        private Double getHours() {
            Duration delta = Duration.between(minTime, maxTime);
            if (delta.isNegative()) {
                delta = Duration.ZERO;
            }
            Double hours = delta.getSeconds()/3600.0D;
            return hours;
        }
        
        public Double getAthletesPerHour() {
            Double hours = getHours();
            return hours > 0 ? (nbAthletes/ hours) : null;
        }

        public void updateMaxTime(LocalDateTime newTime) {
            if (newTime.isAfter(this.maxTime)) {
                this.maxTime = newTime;
            } else {
            }

        }

        public void updateMinTime(LocalDateTime newTime) {
            if (newTime.isBefore(this.minTime)) {
                this.minTime = newTime;
            } else {
            }

        }
    }

    public static String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        long absSeconds = Math.abs(seconds);
        String positive = String.format(
                "%d:%02d:%02d",
                absSeconds / 3600,
                (absSeconds % 3600) / 60,
                absSeconds % 60);
        return seconds < 0 ? "-" + positive : positive;
    }

    Logger logger = (Logger) LoggerFactory.getLogger(JXLSTimingStats.class);

    public JXLSTimingStats() {
        super();
    }

    public JXLSTimingStats(Group group, boolean excludeNotWeighed) {
        super();
    }

    @Override
    public InputStream getTemplate(Locale locale) throws IOException {
        String templateName = "/templates/timing/TimingStats_" + locale.getLanguage() + ".xls"; //$NON-NLS-1$ //$NON-NLS-2$
        final InputStream resourceAsStream = this.getClass().getResourceAsStream(templateName);
        if (resourceAsStream == null)
            throw new IOException("resource not found: " + templateName); //$NON-NLS-1$
        return resourceAsStream;
    }

    @Override
    protected List<Athlete> getSortedAthletes() {
        HashMap<String, Object> reportingBeans = getReportingBeans();

        List<Athlete> athletes = AthleteSorter
                .registrationOrderCopy(AthleteRepository.findAllByGroupAndWeighIn(null, isExcludeNotWeighed()));
        if (athletes.isEmpty())
            // prevent outputting silliness.
            throw new RuntimeException(""); //$NON-NLS-1$

        // extract group stats
        Group curGroup = null;
        Group prevGroup = null;

        List<SessionStats> sessions = new LinkedList<>();

        SessionStats curStat = null;
        for (Athlete curAthlete : athletes) {
            curGroup = curAthlete.getGroup();
            if (curGroup == null) {
                continue; // we simply skip over athletes with no groups
            }
            if (curGroup != prevGroup) {
                processGroup(sessions, curStat);

                String name = curGroup.getName();
                curStat = new SessionStats(name);
            }
            // update stats, min, max.
            curStat.setNbAthletes(curStat.getNbAthletes() + 1);
            LocalDateTime minTime = curAthlete.getFirstAttemptedLiftTime();
            curStat.updateMinTime(minTime);

            LocalDateTime maxTime = curAthlete.getLastAttemptedLiftTime();
            curStat.updateMaxTime(maxTime);

            int nbAttemptedLifts = curAthlete.getAttemptedLifts();
            curStat.setNbAttemptedLifts(curStat.getNbAttemptedLifts() + nbAttemptedLifts);
            logger.debug(curStat.toString());
            prevGroup = curGroup;
        }
        if (curStat.getNbAthletes() > 0) {
            processGroup(sessions, curStat);
        }
        reportingBeans.put("groups", sessions); //$NON-NLS-1$
        return athletes;
    }

    private void processGroup(List<SessionStats> sessions, SessionStats curStat) {
        if (curStat == null)
            return;
        sessions.add(curStat);
    }
}
