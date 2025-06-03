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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.technicalofficial.OfficialRole;
import app.owlcms.data.technicalofficial.SessionAssignment;
import app.owlcms.data.technicalofficial.TechnicalOfficial;
import app.owlcms.data.technicalofficial.TechnicalOfficialRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

/**
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public class JXLSExportTechnicalOfficials extends JXLSWorkbookStreamSource {

	Logger logger = (Logger) LoggerFactory.getLogger(JXLSExportTechnicalOfficials.class);

	private Map<TechnicalOfficial, List<SessionAssignment>> officialToAssignments = new HashMap<>();
	private Map<Group, List<SessionAssignment>> groupToAssignments = new HashMap<>();
	private Map<String, TechnicalOfficial> nameToOfficials = new HashMap<>();
	private List<SessionAssignment> assignments = new ArrayList<>();

	public JXLSExportTechnicalOfficials(UI ui) {
	}

	@Override
	public List<Athlete> getSortedAthletes() {
		// unused. prevent spurious warning.
		return List.of(new Athlete());
	}

	@Override
	public InputStream getTemplate(Locale locale) throws IOException {
		if (this.inputStream != null) {
			this.logger.debug("explicitly set template {}", this.inputStream);
			return new BufferedInputStream(this.inputStream);
		}
		this.logger.debug("getTemplate {}", LoggerUtils.whereFrom());
		return getLocalizedTemplate("/templates/toAssignments/toAssignments", ".xls", locale);
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
		List<Group> sessions = GroupRepository.findAll().stream().sorted((a, b) -> {
			int compare = ObjectUtils.compare(a.getWeighInTime(), b.getWeighInTime(), true);
			if (compare != 0) {
				return compare;
			}
			return compare = ObjectUtils.compare(a.getPlatform(), b.getPlatform(), true);
		}).collect(Collectors.toList());
		getReportingBeans().put("groups", sessions);
		getReportingBeans().put("sessions", sessions);

		populateMaps();

		getReportingBeans().put("sessionToAssignments", groupToAssignments);
		getReportingBeans().put("officialToAssignments", officialToAssignments);
		getReportingBeans().put("assignments", assignments);
	}

	private List<TechnicalOfficial> populateMaps() {
		// pre-populate the name lookup map from a single database query
		TechnicalOfficialRepository.findAll().forEach(official -> nameToOfficials.put(official.getFullName(), official));
		assignments.clear();
		groupToAssignments.clear();
		officialToAssignments.clear();

		// iterate over all groups
		GroupRepository.findAll().forEach(group -> {
			// iterate over all roles
			sessionRoleGetterMap().forEach((role, getter) -> {
				// get the official name assigned to the role
				String officialName = getter.apply(group);
				if (officialName != null) {
					// lookup official from our pre-populated map
					TechnicalOfficial official = nameToOfficials.get(officialName);
					if (official != null) {
						// create a new GroupAssignment object
						SessionAssignment assignment = new SessionAssignment(official, group);
						assignment.getRoles().add(role);
						// add to maps
						groupToAssignments.computeIfAbsent(group, k -> new ArrayList<>()).add(assignment);
						officialToAssignments.computeIfAbsent(official, k -> new ArrayList<>()).add(assignment);
						// add to list of all assignments
						assignments.add(assignment);
					}
				}
			});
		});

		// return list of officials with assignments
		return new ArrayList<>(officialToAssignments.keySet());
	}

	/**
	 * Creates a mapping from official role names to the corresponding getter in the Group class.
	 */
	private Map<OfficialRole, Function<Group, String>> sessionRoleGetterMap() {
		Map<OfficialRole, Function<Group, String>> map = new EnumMap<>(OfficialRole.class);

		map.put(OfficialRole.CENTER_REFEREE, Group::getReferee2);
		map.put(OfficialRole.LEFT_REFEREE, Group::getReferee1);
		map.put(OfficialRole.RIGHT_REFEREE, Group::getReferee3);
		map.put(OfficialRole.TIMEKEEPER, Group::getTimeKeeper);
		map.put(OfficialRole.TECHNICAL_CONTROLLER1, Group::getTechnicalController);
		map.put(OfficialRole.TECHNICAL_CONTROLLER2, Group::getTechnicalController2);
		map.put(OfficialRole.MARSHAL1, Group::getMarshall);
		map.put(OfficialRole.MARSHAL2, Group::getMarshal2);
		map.put(OfficialRole.JURY_PRESIDENT, Group::getJury1);
		map.put(OfficialRole.JURY_A, Group::getJury2);
		map.put(OfficialRole.JURY_B, Group::getJury3);
		map.put(OfficialRole.JURY_C, Group::getJury4);
		map.put(OfficialRole.JURY_D, Group::getJury5);
		map.put(OfficialRole.ANNOUNCER, Group::getAnnouncer);
		map.put(OfficialRole.COMPETITION_SECRETARY, Group::getCompetitionSecretary);
		map.put(OfficialRole.COMPETITION_SECRETARY2, Group::getCompetitionSecretary2);
		map.put(OfficialRole.COMPETITION_DIRECTOR, Group::getCompetitionDirector);
		map.put(OfficialRole.REFEREE_RESERVE, Group::getReserve);
		map.put(OfficialRole.JURY_RESERVE, Group::getReserveJury);
		return map;
	}

}
