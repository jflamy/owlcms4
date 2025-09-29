/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.records.RecordEvent;
import app.owlcms.data.records.RecordRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

/**
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public class JXLSExportRecords extends JXLSWorkbookStreamSource {

	Logger logger = (Logger) LoggerFactory.getLogger(JXLSExportRecords.class);
	Group group;
	private List<RecordEvent> records;
	// private List<RecordEvent> bestRecords;
	private boolean allRecords;
	private boolean currentOnly;

	public JXLSExportRecords(Group group, boolean excludeNotWeighed, UI ui) {
	}

	public JXLSExportRecords(UI ui, boolean allRecords, boolean currentOnly) {
		this.setAllRecords(allRecords);
		this.currentOnly = currentOnly;
	}

	/**
	 * Constructor that accepts a pre-filtered list of records.
	 * Used when we want to export exactly what is shown in the grid.
	 */
	public JXLSExportRecords(UI ui, List<RecordEvent> filteredRecords) {
		this.setAllRecords(true); // Not used when records are pre-filtered
		this.currentOnly = false; // Not used when records are pre-filtered
		this.records = new ArrayList<>(filteredRecords);
	}

	@Override
	public Group getGroup() {
		return this.group;
	}

	/**
	 * Must be called immediately after getSortedAthletes due to reliance on "records" variable side-effect.
	 *
	 * @param cat
	 * @return
	 */
	public List<RecordEvent> getRecords(Category cat) {
		if (cat == null) {
			return this.records.isEmpty() ? null : this.records;
		}
		this.logger.debug("category {} age >= {} <= {}  bw > {} <= {}",
		        cat.getGender(),
		        cat.getAgeGroup().getMinAge(),
		        cat.getAgeGroup().getMaxAge(),
		        cat.getMinimumWeight(),
		        cat.getMaximumWeight());
		List<RecordEvent> catRecords = new ArrayList<>();
		for (RecordEvent record : this.records) {
			Integer athleteAge = record.getAthleteAge();
			Double athleteBW = record.getAthleteBW();
			try {
				if (record.getGender() == cat.getGender()
				        && athleteAge >= cat.getAgeGroup().getMinAge()
				        && athleteAge <= cat.getAgeGroup().getMaxAge()
				        && athleteBW > cat.getMinimumWeight()
				        && athleteBW <= cat.getMaximumWeight()) {
					catRecords.add(record);
				}
			} catch (Exception e) {
				this.logger.error("faulty record {}", record);
			}
		}
		return catRecords.isEmpty() ? null : catRecords;
	}

	@Override
	public List<Athlete> computeSortedAthletes() {
		HashMap<String, Object> reportingBeans = getReportingBeans();

		// prevent irrelevant "No Athletes" error message.
		List<Athlete> athletes = List.of(new Athlete());

		// Only fetch records if they haven't been pre-provided
		if (this.records == null) {
			String groupName = this.group != null ? this.group.getName() : null;
			this.setRecords(RecordRepository.findFiltered(null, null, null, groupName, !this.isAllRecords()));
			logger.debug("found {} records",getRecords().size());

			if (this.currentOnly) {
				var recordMap = this.keepNewest();
				this.setRecords(new ArrayList<>(recordMap.values().stream().toList()));
				this.getRecords().sort(sortRecords());
			} else {
				this.getRecords().sort(sortRecords());
			}
		} else {
			// Records were pre-filtered, just ensure they're sorted
			logger.debug("using pre-filtered {} records", getRecords().size());
			this.getRecords().sort(sortRecords());
		}

		Map<String, List<RecordEvent>> grouped = groupByAgeGroup(this.getRecords());
		List<Entry<String, List<RecordEvent>>> list = new ArrayList<>();
		for (Entry<String, List<RecordEvent>> v : grouped.entrySet()) {
			list.add(v);
		}
		reportingBeans.put("agegroups", list);
		reportingBeans.put("records", this.getRecords());
		
		//logger.debug("put {}",getRecords().size());
		return athletes;
	}

	@Override
	public InputStream getTemplate(Locale locale) throws IOException {
		if (this.inputStream != null) {
			this.logger.debug("explicitly set template {}", this.inputStream);
			return new BufferedInputStream(this.inputStream);
		}
		this.logger.debug("getTemplate {}", LoggerUtils.whereFrom());
		return getLocalizedTemplate("/templates/records/exportRecords", ".xls", locale);
	}

	public Map<String, RecordEvent> keepNewest() {
		return this.getRecords().stream()
		        .collect(Collectors.groupingBy(
		                RecordEvent::getKey,
		                Collectors.collectingAndThen(
		                        Collectors.maxBy((r1, r2) -> r1.getRecordLift().compareTo(r2.getRecordLift())),
		                        record -> record.orElseThrow(() -> new IllegalStateException("No record found")))));
	}

	@Override
	public void setGroup(Group group) {
		this.group = group;
	}

	public Comparator<RecordEvent> sortRecords() {
		// Use the same ordering as RecordRepository.findWithFilters for consistency
		return Comparator
		        .comparing(RecordEvent::getRecordFederation) // federation first
		        .thenComparing(RecordEvent::getRecordName) // then record name (type)
		        .thenComparing(RecordEvent::getGender) // then gender
		        .thenComparing(RecordEvent::getAgeGrpUpper) // then age group upper
		        .thenComparing(RecordEvent::getAgeGrpLower) // then age group lower
		        .thenComparing(RecordEvent::getBwCatUpper) // then body weight category
		        .thenComparing((r) -> r.getRecordLift().ordinal()) // then lift type (SNATCH, CJ, TOTAL)
		        .thenComparing(RecordEvent::getRecordValue) // finally record value
		;
	}

	@Override
	protected void setReportingInfo() {
		List<Athlete> athletes = getSortedAthletes();
		if (athletes != null) {
			getReportingBeans().put("athletes", athletes);
			getReportingBeans().put("lifters", athletes); // legacy
		}
		Competition competition = Competition.getCurrent();
		getReportingBeans().put("t", Translator.getMap());
		getReportingBeans().put("competition", competition);
		getReportingBeans().put("session", getGroup()); // legacy
		getReportingBeans().put("group", getGroup());

		// reuse existing logic for processing records
		JXLSExportRecords jxlsExportRecords = this;
		// jxlsExportRecords.setGroup(getGroup());
		this.logger.debug("fetching records for session {} category {}", getGroup(), getCategory());
		try {
			// Must be called as soon as possible after getSortedAthletes()
			List<RecordEvent> records = jxlsExportRecords.getRecords(getCategory());
			this.logger.debug("{} records found", records.size());
			for (RecordEvent e : records) {
				if (e.getBwCatUpper() > 250) {
					e.setBwCatString(">" + e.getBwCatLower());
				} else {
					e.setBwCatString(Integer.toString(e.getBwCatUpper()));
				}
			}
			getReportingBeans().put("records", records);
		} catch (Exception e) {
			// no records
		}

		getReportingBeans().put("masters", Competition.getCurrent().isMasters());
		List<Group> sessions = GroupRepository.findAll().stream().sorted((a, b) -> {
			int compare = ObjectUtils.compare(a.getWeighInTime(), b.getWeighInTime(), true);
			if (compare != 0) {
				return compare;
			}
			return compare = ObjectUtils.compare(a.getPlatform(), b.getPlatform(), true);
		}).collect(Collectors.toList());
		getReportingBeans().put("groups", sessions);
		getReportingBeans().put("sessions", sessions);
	}

	private Map<String, List<RecordEvent>> groupByAgeGroup(List<RecordEvent> events) {
		Map<String, List<RecordEvent>> groupedEvents = new LinkedHashMap<>();
		for (RecordEvent record : events) {
			String ageGroup = record.getAgeGrp();
			boolean masters = record.getAgeGrpLower() >= 30 && (record.getAgeGrp().startsWith("W") || record.getAgeGrp().startsWith("M"));
			groupedEvents.computeIfAbsent(masters ? ageGroup : ageGroup + " " + record.getTranslatedGender(), k -> new ArrayList<>()).add(record);
		}
		return groupedEvents;
	}

	private boolean isAllRecords() {
		return this.allRecords;
	}

	private void setAllRecords(boolean allRecords) {
		this.allRecords = allRecords;
	}

	private List<RecordEvent> getRecords() {
		return records;
	}

	private void setRecords(List<RecordEvent> records) {
		this.records = records;
	}

}
