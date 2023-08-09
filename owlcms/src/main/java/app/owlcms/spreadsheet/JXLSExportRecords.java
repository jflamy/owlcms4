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

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.category.Category;
import app.owlcms.data.group.Group;
import app.owlcms.data.records.RecordEvent;
import app.owlcms.data.records.RecordRepository;
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
	//private List<RecordEvent> bestRecords;
	private boolean allRecords;

	public JXLSExportRecords(Group group, boolean excludeNotWeighed, UI ui) {
		super();
	}

	public JXLSExportRecords(UI ui, boolean allRecords) {
		super();
		this.allRecords = allRecords;
	}

	@Override
	public Group getGroup() {
		return group;
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

	@Override
	public List<Athlete> getSortedAthletes() {
		HashMap<String, Object> reportingBeans = getReportingBeans();

		List<Athlete> athletes = AthleteSorter
		        .registrationOrderCopy(AthleteRepository.findAllByGroupAndWeighIn(null, isExcludeNotWeighed()));
		if (athletes.isEmpty()) {
			// prevent outputting silliness.
			throw new RuntimeException("");
		} else {
			logger.debug("{} athletes", athletes.size());
		}

		String groupName = group != null ? group.getName() : null;
		records = RecordRepository.findFiltered(null, null, null, groupName, !allRecords);
		records.sort(sortRecords());

//		// keep best record if beaten several times
//		RecordEvent[] best = records.toArray(new RecordEvent[0]);
//		RecordEvent prev = null;
//		for (int i = best.length - 1; i >= 0; i--) {
//			if (best[i].sameRecordAs(prev)) {
//				prev = best[i];
//				best[i] = null;
//			} else {
//				prev = best[i];
//			}
//		}
//		bestRecords = Arrays.stream(best).filter(re -> re != null).collect(Collectors.toList());
//		if (!bestRecords.isEmpty()) {
//			reportingBeans.put("records", bestRecords);
//		}
		reportingBeans.put("records", records);
		return athletes;
	}

	/**
	 * Must be called immediately after getSortedAthletes due to reliance on "records" variable side-effect.
	 * 
	 * @param cat
	 * @return
	 */
	public List<RecordEvent> getRecords(Category cat) {
		if (cat == null) {
			return records.isEmpty() ? null : records;
		}
		logger.debug("category {} age >= {} <= {}  bw > {} <= {}", 
				cat.getGender(), 
				cat.getAgeGroup().getMinAge(), 
				cat.getAgeGroup().getMaxAge(),
				cat.getMinimumWeight(),
				cat.getMaximumWeight()
				);
		List<RecordEvent> catRecords = new ArrayList<>();
		for (RecordEvent record : records) {
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
				logger.error("faulty record {}", record);
			}
		}
		return catRecords.isEmpty() ? null : catRecords;
	}

}
