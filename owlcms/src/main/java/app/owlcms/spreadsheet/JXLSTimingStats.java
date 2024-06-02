/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import ch.qos.logback.classic.Logger;

/**
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public class JXLSTimingStats extends JXLSWorkbookStreamSource {

	public class SessionStats {

		String groupName = null;
		LocalDateTime maxTime = LocalDateTime.MIN; // forever ago
		LocalDateTime minTime = LocalDateTime.MAX; // long time in the future
		int nbAthletes;
		int nbAttemptedLifts;

		public SessionStats() {
		}

		public SessionStats(String groupName) {
			this.setGroupName(groupName);
		}

		public Double getAthletesPerHour() {
			Double hours = getHoursForGroup();
			double athleteEquivalents = (getNbAttemptedLifts()) / 6.0D;

			return hours > 0 ? athleteEquivalents / hours : 0;
		}

		/**
		 * @return duration as a fraction of day, for Excel
		 */
		public Double getDayDuration() {
			Duration delta = Duration.between(this.minTime, this.maxTime);
			if (delta.isNegative()) {
				delta = Duration.ZERO;
				return 0.0D;
			}
			return ((double) delta.getSeconds() / (24 * 3600));
		}

		public String getGroupName() {
			return this.groupName;
		}

		public LocalDateTime getMaxTime() {
			return this.maxTime;
		}

		public LocalDateTime getMinTime() {
			return this.minTime;
		}

		public int getNbAthletes() {
			return this.nbAthletes;
		}

		public int getNbAttemptedLifts() {
			return this.nbAttemptedLifts;
		}

		public String getSDuration() {
			Duration delta = Duration.between(this.minTime, this.maxTime);
			if (delta.isNegative()) {
				delta = Duration.ZERO;
			}
			return formatDuration(delta);
		}

		public String getSMaxTime() {
			if (this.maxTime == null || this.maxTime.isEqual(LocalDateTime.MIN)) {
				return "-";
			}
			return this.maxTime.truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_LOCAL_TIME);
		}

		public String getSMinTime() {
			if (this.minTime == null || this.minTime.isEqual(LocalDateTime.MAX)) {
				return "-";
			}
			return this.minTime.truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_LOCAL_TIME);
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
			return "SessionStats [groupName=" + getGroupName() + ", nbAthletes=" + this.nbAthletes + ", minTime="
			        + this.minTime
			        + ", maxTime=" + this.maxTime + ", nbAttemptedLifts=" + this.nbAttemptedLifts + " Hours="
			        + getDayDuration()
			        + " AthletesPerHour=" + getAthletesPerHour() + "]";
		}

		public void updateMaxTime(LocalDateTime newTime) {
			if (newTime != null && newTime.isAfter(this.maxTime)) {
				this.maxTime = newTime;
			} else {
			}

		}

		public void updateMinTime(LocalDateTime newTime) {
			if (newTime != null && newTime.isBefore(this.minTime)) {
				this.minTime = newTime;
			} else {
			}

		}

		private Double getHoursForGroup() {
			Duration delta;
			if (this.minTime == null || this.maxTime == null) {
				delta = Duration.ZERO;
			} else {
				delta = Duration.between(this.minTime, this.maxTime);
			}
			if (delta.isNegative()) {
				delta = Duration.ZERO;
			}
			double hours = delta.getSeconds() / 3600.0D;
			return hours;
		}
	}

	public static String formatDuration(Duration duration) {
		if (duration == null) {
			return "-";
		}
		long seconds = duration.getSeconds();
		long absSeconds = Math.abs(seconds);
		String positive = String.format("%d:%02d:%02d", absSeconds / 3600, (absSeconds % 3600) / 60, absSeconds % 60);
		return seconds < 0 ? "-" + positive : positive;
	}

	Logger logger = (Logger) LoggerFactory.getLogger(JXLSTimingStats.class);

	public JXLSTimingStats(Group group, boolean excludeNotWeighed, UI ui) {
	}

	public JXLSTimingStats(UI ui) {
	}

	@Override
	public List<Athlete> getSortedAthletes() {
		HashMap<String, Object> reportingBeans = getReportingBeans();

		List<Athlete> athletes = AthleteRepository.findAllByGroupAndWeighIn(null, isExcludeNotWeighed());
		athletes = AthleteSorter.registrationExportCopy(athletes);
		
		if (athletes.isEmpty()) {
			// prevent outputting silliness.
			throw new RuntimeException("");
		} else {
			this.logger.debug("{} athletes", athletes.size());
		}
		
		List<Group> groups = GroupRepository.findAll();
		groups.sort(Group.groupWeighinTimeComparator);

		// extract group stats

		List<SessionStats> sessions = new LinkedList<>();
		SessionStats curStat = new SessionStats("");
		for (Group curGroup : groups) {
			String groupName = curGroup.getName();
			curStat = new SessionStats(groupName);
			for (Athlete curAthlete : curGroup.getAthletes()) {
				curGroup = curAthlete.getGroup();
				if (curGroup == null) {
					continue; // we simply skip over athletes with no groups
				}
	
				// update stats, min, max.
				curStat.setNbAthletes(curStat.getNbAthletes() + 1);
				LocalDateTime minTime = curAthlete.getFirstAttemptedLiftTime();
				curStat.updateMinTime(minTime);
	
				LocalDateTime maxTime = curAthlete.getLastAttemptedLiftTime();
				curStat.updateMaxTime(maxTime);
	
				int nbAttemptedLifts = curAthlete.getActuallyAttemptedLifts();
				curStat.setNbAttemptedLifts(curStat.getNbAttemptedLifts() + nbAttemptedLifts);
				this.logger.debug(curStat.toString());
			}
			if (curStat.getNbAthletes() > 0) {
				addSessionStatsIfNotEmpty(sessions, curStat);
			}
		}
		reportingBeans.put("groupStats", sessions);
		return athletes;
	}

	@Override
	public InputStream getTemplate(Locale locale) throws IOException {
		return getLocalizedTemplate("/templates/timing/TimingStats", ".xls", locale);
	}

	private void addSessionStatsIfNotEmpty(List<SessionStats> sessions, SessionStats curStat) {
		if (curStat == null) {
			return;
		}
		sessions.add(curStat);
	}

}
