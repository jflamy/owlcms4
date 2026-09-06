/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.internal.AllowInert;
import com.vaadin.flow.component.littemplate.LitTemplate;

import app.owlcms.data.competition.Competition;
import app.owlcms.data.competition.CompetitionRepository;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.technicalofficial.OfficialRole;
import app.owlcms.data.technicalofficial.TechnicalOfficialsTimetable;
import app.owlcms.data.technicalofficial.TechnicalOfficialsTimetableRepository;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@SuppressWarnings({ "serial", "deprecation" })
@Tag("team-schedule-editor")
@JsModule("./components/TeamScheduleEditor.js")
public class TeamScheduleElement extends LitTemplate {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(TeamScheduleElement.class);
	private static final List<OfficialRole> DEFAULT_ROLES = List.of(
			OfficialRole.JURY,
			OfficialRole.REFEREE,
			OfficialRole.MARSHALL,
			OfficialRole.TIMEKEEPER,
			OfficialRole.TECHNICAL_CONTROLLER,
			OfficialRole.DOCTOR,
			OfficialRole.COMPETITION_SECRETARY,
			OfficialRole.ANNOUNCER,
			OfficialRole.WEIGHIN);
	private static final ObjectMapper mapper = new ObjectMapper();

	public TeamScheduleElement() {
		getElement().getStyle().set("height", "100%");
		getElement().getStyle().set("width", "100%");
		getElement().setProperty("title", Translator.translate("Timetable.TeamAssignments"));
		getElement().setProperty("sessionLabel", Translator.translate("Session"));
		getElement().setProperty("clearLabel", Translator.translate("Clear"));
		getElement().setProperty("copyLabel", Translator.translate("Copy"));
		getElement().setProperty("pasteLabel", Translator.translate("Paste"));
		getElement().setProperty("saveLabel", Translator.translate("Save"));
		getElement().setProperty("cancelLabel", Translator.translate("Cancel"));
		refreshSchedule();
	}

	@AllowInert
	@ClientCallable
	public void saveSchedule(String payloadJson) {
		try {
			Map<String, Object> payload = mapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {});
			List<String> columnNames = stringValues(payload.get("columns"));
			List<ScheduleRow> rows = scheduleRows(payload.get("rows"));
			List<OfficialRole> roles = normalizedRoles(columnNames);
			Map<Long, Group> groupsById = JPAService.runInTransaction(em -> {
				Map<Long, Group> groups = new java.util.HashMap<>();
				for (Group group : GroupRepository.doFindAll(em)) {
					groups.put(group.getId(), group);
				}
				List<TechnicalOfficialsTimetable> entries = new ArrayList<>();
				for (ScheduleRow row : rows) {
					Group group = groups.get(groupId(row.id));
					if (group == null) {
						throw new IllegalArgumentException("Unknown timetable session");
					}
					if (row.cells == null) {
						continue;
					}
					for (OfficialRole role : roles) {
						Integer teamNumber = teamNumber(row.cells.get(role.name()));
						if (teamNumber != null) {
							entries.add(new TechnicalOfficialsTimetable(group, role, teamNumber));
						}
					}
				}
				TechnicalOfficialsTimetableRepository.deleteAll(em);
				for (TechnicalOfficialsTimetable entry : entries) {
					TechnicalOfficialsTimetableRepository.save(em, entry);
				}
				return groups;
			});
			if (groupsById.isEmpty() && !rows.isEmpty()) {
				throw new IllegalArgumentException("No timetable sessions are available");
			}
			Competition competition = Competition.getCurrent();
			competition.setTeamScheduleColumnOrder(roles.stream().map(Enum::name).toList());
			CompetitionRepository.save(competition);
			logger.info("Saved technical officials timetable for {} sessions with {} assigned cells",
			        rows.size(),
			        rows.stream().mapToLong(row -> row.cells == null ? 0 : row.cells.values().stream()
			                .filter(value -> teamNumber(value) != null).count()).sum());
		} catch (JacksonException | IllegalArgumentException e) {
			logger.error("Unable to save technical officials timetable: {}", e.getMessage());
			throw new IllegalArgumentException("Invalid technical officials timetable");
		}
	}

	public void refreshSchedule() {
		List<OfficialRole> roles = normalizedRoles(Competition.getCurrent().getTeamScheduleColumnOrder());
		List<Group> groups = GroupRepository.findAll();
		groups.sort(Comparator.comparing(Group::getCompetitionTime, Comparator.nullsLast(Comparator.naturalOrder())));
		Map<Long, Map<OfficialRole, Integer>> assignments = JPAService.runInTransaction(em -> {
			Map<Long, Map<OfficialRole, Integer>> result = new java.util.HashMap<>();
			for (TechnicalOfficialsTimetable entry : TechnicalOfficialsTimetableRepository.findAll(em)) {
				if (entry.getGroup() != null && entry.getRoleCategory() != null && entry.getTeamNumber() != null) {
					result.computeIfAbsent(entry.getGroup().getId(), ignored -> new EnumMap<>(OfficialRole.class))
					        .put(entry.getRoleCategory(), entry.getTeamNumber());
				}
			}
			return result;
		});
		List<Map<String, Object>> rows = new ArrayList<>();
		for (Group group : groups) {
			Map<String, Object> row = new java.util.LinkedHashMap<>();
			row.put("id", group.getId().toString());
			row.put("session", group.getName());
			Map<String, String> cells = new java.util.LinkedHashMap<>();
			Map<OfficialRole, Integer> groupAssignments = assignments.getOrDefault(group.getId(), Map.of());
			for (OfficialRole role : roles) {
				Integer teamNumber = groupAssignments.get(role);
				cells.put(role.name(), teamNumber == null ? "" : teamNumber.toString());
			}
			row.put("cells", cells);
			rows.add(row);
		}
		try {
			Map<String, String> roleLabels = new java.util.LinkedHashMap<>();
			for (OfficialRole role : roles) {
				roleLabels.put(role.name(), Translator.translate(role.getAssignmentKey()));
			}
			getElement().setProperty("rolesJson", mapper.writeValueAsString(roles.stream().map(Enum::name).toList()));
			getElement().setProperty("roleLabelsJson", mapper.writeValueAsString(roleLabels));
			getElement().setProperty("scheduleJson", mapper.writeValueAsString(rows));
		} catch (JacksonException e) {
			throw new IllegalStateException("Unable to prepare technical officials timetable", e);
		}
	}

	private static Long groupId(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return Long.valueOf(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static List<OfficialRole> normalizedRoles(List<String> names) {
		Set<OfficialRole> roles = new LinkedHashSet<>();
		if (names != null) {
			for (String name : names) {
				try {
					OfficialRole role = OfficialRole.valueOf(name);
					if (role.isGenericTeamRole()) {
						roles.add(role);
					}
				} catch (IllegalArgumentException ignored) {
					// Ignore obsolete role names retained in an imported competition.
				}
			}
		}
		roles.addAll(DEFAULT_ROLES);
		return new ArrayList<>(roles);
	}

	private static Integer teamNumber(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			int teamNumber = Integer.parseInt(value.trim());
			return teamNumber > 0 ? teamNumber : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static List<String> stringValues(Object values) {
		if (!(values instanceof List<?> list)) {
			return List.of();
		}
		return list.stream().map(String::valueOf).toList();
	}

	private static List<ScheduleRow> scheduleRows(Object values) {
		if (!(values instanceof List<?> list)) {
			return List.of();
		}
		List<ScheduleRow> rows = new ArrayList<>();
		for (Object value : list) {
			if (!(value instanceof Map<?, ?> map)) {
				throw new IllegalArgumentException("Invalid timetable row");
			}
			ScheduleRow row = new ScheduleRow();
			Object id = map.get("id");
			row.id = id == null ? null : String.valueOf(id);
			Object cells = map.get("cells");
			if (cells instanceof Map<?, ?> cellMap) {
				row.cells = new java.util.HashMap<>();
				for (Map.Entry<?, ?> cell : cellMap.entrySet()) {
					row.cells.put(String.valueOf(cell.getKey()),
						        cell.getValue() == null ? "" : String.valueOf(cell.getValue()));
				}
			}
			rows.add(row);
		}
		return rows;
	}

	public static class ScheduleRow {
		public String id;
		public String session;
		public Map<String, String> cells;
	}
}