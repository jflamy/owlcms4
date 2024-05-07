/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.records.RecordEvent;
import app.owlcms.data.records.RecordRepository;
import app.owlcms.i18n.Translator;
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

	public JXLSExportRecords(Group group, boolean excludeNotWeighed, UI ui) {
	}

	public JXLSExportRecords(UI ui, boolean allRecords) {
		this.allRecords = allRecords;
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
	public List<Athlete> getSortedAthletes() {
		HashMap<String, Object> reportingBeans = getReportingBeans();

		List<Athlete> athletes = AthleteSorter
		        .registrationOrderCopy(AthleteRepository.findAllByGroupAndWeighIn(null, isExcludeNotWeighed()));
		if (athletes.isEmpty()) {
			// prevent outputting silliness.
			throw new RuntimeException("");
		} else {
			this.logger.debug("{} athletes", athletes.size());
		}

		String groupName = this.group != null ? this.group.getName() : null;
		this.records = RecordRepository.findFiltered(null, null, null, groupName, !this.allRecords);
		this.records.sort(sortRecords());
		reportingBeans.put("records", this.records);
		return athletes;
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
		//jxlsExportRecords.setGroup(getGroup());
		logger.debug("fetching records for session {} category {}", getGroup(), getCategory());
		try {
			// Must be called as soon as possible after getSortedAthletes()
			List<RecordEvent> records = jxlsExportRecords.getRecords(getCategory());
			logger.debug("{} records found", records.size());
			for (RecordEvent e: records) {
				if (e.getBwCatUpper() > 250) {
					e.setBwCatString(">"+e.getBwCatLower());
				} else {
					e.setBwCatString(Integer.toString(e.getBwCatUpper()));
				}
			};
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

	@Override
	public InputStream getTemplate(Locale locale) throws IOException {
		return getLocalizedTemplate("/templates/records/exportRecords", ".xls", locale);
	}

	@Override
	public void setGroup(Group group) {
		this.group = group;
	}

	public Comparator<RecordEvent> sortRecords() {
		return Comparator
		        .comparing(RecordEvent::getRecordFederation) // all records for a federation go together (masters are
		                                                     // separate)
		        .thenComparing(RecordEvent::getRecordName) // sometimes several record names for same federation
		                                                   // (example: event-specific)
		        .thenComparing(RecordEvent::getGender) // all women, then all men
		        .thenComparing(RecordEvent::getAgeGrpUpper) // U13 U15 U17 U20 U23 SR
		        .thenComparing(RecordEvent::getAgeGrpLower) // increasing age groups for masters (35, 40, 45...)
		        .thenComparing(RecordEvent::getBwCatUpper) // increasing body weights
		        .thenComparing((r) -> r.getRecordLift().ordinal()) // SNATCH, CJ, TOTAL
		        .thenComparing(RecordEvent::getRecordValue) // increasing records
		;
	}

}
