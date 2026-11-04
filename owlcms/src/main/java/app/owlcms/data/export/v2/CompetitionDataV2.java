/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.export.v2;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;

import java.time.Instant;

import app.owlcms.audit.AuditActor;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.audit.ExportAudit;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.agegroup.ChampionshipRepository;
import app.owlcms.data.account.UserAccount;
import app.owlcms.data.account.UserAccountRepository;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.RankingConfig;
import app.owlcms.data.athleteSort.RegistrationOrderComparator;
import app.owlcms.data.coach.Coach;
import app.owlcms.data.coach.CoachRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.competition.CompetitionRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.ConfigRepository;
import app.owlcms.data.export.ChecksummedJsonExport;
import app.owlcms.data.export.ChecksummedJsonExport.ChecksummedInputStream;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.data.records.RecordConfig;
import app.owlcms.data.records.RecordEvent;
import app.owlcms.data.records.RecordRepository;
import app.owlcms.data.technicalofficial.TechnicalOfficial;
import app.owlcms.data.technicalofficial.TechnicalOfficialRepository;
import app.owlcms.data.technicalofficial.TechnicalOfficialsTimetable;
import app.owlcms.data.technicalofficial.TechnicalOfficialsTimetableRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Logger;

/**
 * Version 2 of the competition data export format.
 * Uses code/name references instead of IDs, renames Group to Session,
 * and uses numeric values for lifts instead of strings.
 * 
 * Field order is explicitly controlled to ensure:
 * 1. Competition rules and config are available first during import
 * 2. Referenced entities (teams, ageGroups) appear before referencing entities (athletes)
 * 3. Logical reading order matches processing dependencies
 */
@JsonPropertyOrder({
	"formatVersion",
	"exportDate",
	"competition",
	"config",
	"accounts",
	"championships",
	"ageGroups",
	"teams",
	"coaches",
	"sessions",
	"athletes",
	"platforms",
	"records",
	"recordConfig",
	"technicalOfficials",
	"technicalOfficialsTimetable"
})
public class CompetitionDataV2 {

	final static Logger logger = (Logger) LoggerFactory.getLogger(CompetitionDataV2.class);

	private static final Comparator<AgeGroup> AGE_GROUP_EXPORT_ORDER = Comparator
	        .comparing(AgeGroup::getAgeDivision, Comparator.nullsLast(Comparator.naturalOrder()))
	        .thenComparing(AgeGroup::getMinAge, Comparator.nullsLast(Comparator.naturalOrder()))
	        .thenComparing(AgeGroup::getMaxAge, Comparator.nullsLast(Comparator.naturalOrder()))
	        .thenComparing(AgeGroup::getGender, Comparator.nullsLast(Comparator.naturalOrder()))
	        .thenComparing(AgeGroup::getCode, Comparator.nullsLast(Comparator.naturalOrder()));
	
	private String formatVersion = "2.0";
	private String exportDate;
	private Competition competition;
	private Config config;
	private List<UserAccount> accounts;
	// sections written by V2Json: bound to entities during the import transaction, where references are resolved
	private List<JsonNode> championships;
	private List<JsonNode> ageGroups;
	private List<TeamDTO> teams;
	private List<Coach> coaches;
	private List<JsonNode> sessions;
	private List<JsonNode> athletes;
	private List<Platform> platforms;
	private List<RecordEvent> records;
	private RecordConfig recordConfig;
	private List<TechnicalOfficial> technicalOfficials;
	private List<JsonNode> technicalOfficialsTimetable;

	public CompetitionDataV2() {
	}

	private static ObjectMapper createMapper() {
		// Jackson 3 defaults FAIL_ON_NULL_FOR_PRIMITIVES to true; Jackson 2 defaulted to false.
		// Restore the lenient behavior so null primitive fields (e.g. ranks/flags) map to their default value.
		return JsonMapper.builder()
		        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
		        .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
		        .configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, true)
		        .build();
	}

	/** Serializes this instance as-is (used by tests and tooling); the SHA-256 is still written to the audit log. */
	public ChecksummedInputStream exportData() {
		return export(null, null, null, ExportAudit.CHANNEL_INTERNAL, false);
	}

	/** Exports the current database contents; the SHA-256 of the exact bytes is written to the audit log. */
	public ChecksummedInputStream exportData(AuditActor actor, String channel) {
		return export(null, null, actor, channel, true);
	}

	public ChecksummedInputStream exportData(UI ui, Notification notification, AuditActor actor, String channel) {
		return export(ui, notification, actor, channel, true);
	}

	private ChecksummedInputStream export(UI ui, Notification notification, AuditActor actor, String channel,
			boolean fromDatabase) {
		if (ui != null) {
			ui.access(() -> notification.open());
		}
		ObjectWriter writerWithDefaultPrettyPrinter = createMapper().writerWithDefaultPrettyPrinter();
		try {
			return ChecksummedJsonExport.audited(2, channel, actor, out -> {
				try {
					writerWithDefaultPrettyPrinter.writeValue(out, fromDatabase ? this.fromDatabase() : this);
				} finally {
					if (ui != null) {
						ui.access(() -> notification.close());
					}
				}
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public CompetitionDataV2 fromDatabase() {
		// Set export timestamp in ISO 8601 format
		setExportDate(Instant.now().toString());

		// Read championships through the cache (Championship.findAllIncludingTemplate) rather than
		// the per-call DB query in ChampionshipRepository.
		setChampionships(Championship.findAllIncludingTemplate().stream()
			.map(V2Json::toJson)
			.collect(Collectors.toList()));
		
		// lists are written in a canonical order, so that exporting the same data twice gives the same file
		setAgeGroups(AgeGroupRepository.findAll().stream()
			.sorted(AGE_GROUP_EXPORT_ORDER)
			.map(V2Json::toJson)
			.collect(Collectors.toList()));
		
		List<Athlete> allAthletes = AthleteRepository.findAll()
		        .stream()
		        .sorted(RegistrationOrderComparator.athleteRegistrationOrderComparator
		                .thenComparing(Athlete::getKey))
		        .collect(Collectors.toList());
		
		// the teams table: external readers look up the athletes' numeric team ids in it
		Set<String> teamNames = new TreeSet<>();
		for (Athlete athlete : allAthletes) {
			String teamName = athlete.getTeam();
			if (teamName != null && !teamName.isBlank()) {
				teamNames.add(teamName);
			}
		}
		setTeams(teamNames.stream().map(TeamDTO::new).collect(Collectors.toList()));
		TeamDTO.idCollisions(teamNames).forEach((id, names) -> logger.error(
		        "V2 export: teams {} share the id {}; readers using the teams table will merge them", names, id));
		setCoaches(CoachRepository.findAll());
		
		// Convert sessions first so athletes can reference sessions on import
		List<Group> allGroups = GroupRepository.findAll();
		setSessions(allGroups.stream()
			.map(V2Json::toJson)
			.collect(Collectors.toList()));

		setAthletes(allAthletes.stream()
			.map(V2Json::toJson)
			.collect(Collectors.toList()));
		
		setPlatforms(PlatformRepository.findAll());
		setAccounts(UserAccountRepository.findAll());
		Config config = Config.getCurrent();
		// export metadata only: transient, never stored
		config.setAppVersion(StartupUtils.getVersion());
		// not setConfig/setCompetition: those are the import setters, and setConfig saves
		setConfigForExport(config);
		setCompetitionForExport(Competition.getCurrent());
		setRecords(RecordRepository.findAll());
		setRecordConfig(RecordConfig.getCurrent());
		setTechnicalOfficials(TechnicalOfficialRepository.findAll());
        
		setTechnicalOfficialsTimetable(
			JPAService.runInTransaction(em -> TechnicalOfficialsTimetableRepository.findAll(em)).stream()
				.map(V2Json::toJson)
				.collect(Collectors.toList())
		);
		
		return this;
	}

	public CompetitionDataV2 importData(InputStream serialized) {
		ObjectMapper mapper = createMapper();
		CompetitionDataV2 newData;
		try {
			newData = mapper.readValue(serialized, CompetitionDataV2.class);
			RecordConfig.normalizeImportedRecordNames(newData.getRecords(), newData.getRecordConfig());
			newData.setPlatforms(PlatformRepository.canonicalizeImportedPlatforms(newData.getPlatforms(), null));
			logger.info("V2 import: {} ageGroups, {} teams, {} sessions, {} athletes, {} platforms", 
				newData.getChampionships() != null ? newData.getChampionships().size() : 0,
				newData.getAgeGroups() != null ? newData.getAgeGroups().size() : 0,
				newData.getTeams() != null ? newData.getTeams().size() : 0,
				newData.getSessions() != null ? newData.getSessions().size() : 0,
				newData.getAthletes() != null ? newData.getAthletes().size() : 0,
				newData.getPlatforms() != null ? newData.getPlatforms().size() : 0);
			return newData;
		} catch (Exception e) {
			LoggerUtils.logError(logger, e);
			throw new IllegalStateException("V2 import failed: " + LoggerUtils.exceptionMessage(e), e);
		}
	}

	/**
	 * The championships as the import restores them, as the V1 import does: missing orders follow the highest, and
	 * team-points policies are made consistent.
	 */
	public List<Championship> importedChampionships() {
		if (this.championships == null) {
			return List.of();
		}
		List<Championship> result = this.championships.stream().map(V2Json::toChampionship)
		        .collect(Collectors.toList());
		Championship.assignMissingOrder(result);
		result.forEach(Championship::normalizeTeamPointsPolicy);
		return result;
	}

	public void restore(InputStream inputStream) {
		restore(inputStream, AuditOrigin.system("import"));
	}

	/** @param origin who imported and from where; recorded as the last saver of the replaced settings */
	public void restore(InputStream inputStream, AuditOrigin origin) {
		this.removeAll();
		JPAService.runInTransaction(em -> {
			try {
				Athlete.setSkipValidationsDuringImport(true);
				OwlcmsFactory.resetFOPByName();

				CompetitionDataV2 updated = this.importData(inputStream);
				Config config = updated.getConfig();
				
				config.setLocalDateTimeUtcNormalized(true);
				
				byte[] blob = config.getLocalZipBlob();
				if (blob != null) {
					logger.info("override zip found {} bytes", blob.length);
				}
				ConfigRepository.replaceForImport(config, origin);
				
				ResourceWalker.setInitializedLocalDir(false);
				ResourceWalker.initLocalDir();

				Translator.reset();
				Translator.setForcedLocale(config.getDefaultLocale());

				Competition competition = updated.getCompetition();
				Competition.setCurrent(competition);			// Recompute mustCompute rankings based on imported Competition and age groups
			RankingConfig.updateMustCompute();
			if (updated.getChampionships() != null) {
				for (Championship championship : updated.importedChampionships()) {
					if (championship.isCompetitionTemplate()) {
						championship.setName(Championship.COMPETITION_TEMPLATE_NAME);
					}
					Championship existing = championship.isCompetitionTemplate()
					        ? ChampionshipRepository.ensureCompetitionTemplate(em)
					        : ChampionshipRepository.findByName(championship.getName());
					if (existing == null) {
						em.persist(championship);
					} else {
						// championships are not deleted before an import: the stored row takes the imported values
						V2Json.copyChampionship(championship, existing);
						em.merge(existing);
					}
				}
				em.flush();
			}
			for (JsonNode ageGroup : updated.getAgeGroups()) {
				AgeGroup imported = V2Json.toAgeGroup(ageGroup);
				imported.stamp(origin, null);
				em.persist(imported);
			}
		// Flush to ensure categories from AgeGroups are available for lookup
		em.flush();

			if (updated.getPlatforms() != null) {
				for (Platform p : updated.getPlatforms()) {
					em.merge(p);
				}
				em.flush();
			}

			if (updated.getAccounts() != null) {
				UserAccountRepository.replaceAll(em, updated.getAccounts());
			}

		// Build team ID to name map for athlete import
		Map<Integer, String> teamIdToNameMap = new HashMap<>();
		if (updated.getTeams() != null) {
			for (TeamDTO team : updated.getTeams()) {
				if (team.getId() != null && team.getName() != null) {
					teamIdToNameMap.put(team.getId(), team.getName());
				}
			}
		}

		// IMPORTANT: Groups/Sessions must be created BEFORE Athletes
		// Athletes reference Groups via GroupRepository.findByName() during toAthlete()
		for (JsonNode session : updated.getSessions()) {
			Group g = V2Json.toSession(session, em);
			g.cleanImportedOfficialNames();
			em.merge(g);
		}

		// Now create Athletes which will look up their Groups by name
		for (JsonNode athlete : updated.getAthletes()) {
			Athlete a = V2Json.toAthlete(athlete, em, teamIdToNameMap);
			a.cleanImportedText();
			em.persist(a);
		}

		if (updated.getCoaches() != null) {
			for (Coach coach : updated.getCoaches()) {
				em.persist(coach);
			}
		}

		if (updated.getRecordConfig() != null) {
			RecordConfig importedRecordConfig = updated.getRecordConfig();
			// the record settings row is not deleted before an import: it is replaced in place
			RecordConfig existing = em.find(RecordConfig.class, importedRecordConfig.getId());
			importedRecordConfig.stamp(origin, existing != null ? existing.getLastSavedAt() : null);
			em.merge(importedRecordConfig);
		}

		if (updated.getRecords() != null) {
			for (RecordEvent r : updated.getRecords()) {
				r.cleanImportedText();
				em.merge(r);
			}
		}

		if (updated.getTechnicalOfficials() != null) {
			for (TechnicalOfficial p : updated.getTechnicalOfficials()) {
				p.cleanImportedNames();
				em.merge(p);
			}
		}
		
		// timetable entries refer to sessions by name, resolved with the import's entity manager
		if (updated.getTechnicalOfficialsTimetable() != null) {
			for (JsonNode node : updated.getTechnicalOfficialsTimetable()) {
				TechnicalOfficialsTimetable entry = V2Json.toTimetableEntry(node, em);
				if (entry.getGroup() != null) {
					em.merge(entry);
				} else {
					logger./**/warn("V2 import: timetable entry without a known session ignored: {}", node);
				}
			}
		}
		
		competition.stamp(origin, null);
		em.merge(competition);
		em.flush();
		} catch (Exception e) {
			LoggerUtils.logError(logger, e);
			throw e instanceof RuntimeException re ? re : new IllegalStateException(e);
		} finally {
			Athlete.setSkipValidationsDuringImport(false);
		}
		return null;
		});
		// the account cache may have been refilled with the old accounts before the import committed
		UserAccountRepository.invalidate();
		ChampionshipRepository.reconcileImportedAgeGroups();
		// publish the stored competition, not the deserialized object (which has no id): saving that one
		// would insert a second competition
		Competition.setCurrent(CompetitionRepository.findAll().stream().findFirst().orElse(null));
		Championship.reset();
		Championship.recomputeParticipantCounts();
		Competition.recomputeAllAthleteRanks();
		
		RecordConfig current = RecordConfig.getCurrent();
		current.addMissing(RecordRepository.findAllRecordNames());
	}

	private void removeAll() {
		// Use existing removal logic from CompetitionRepository
		JPAService.runInTransaction(em -> {
			CompetitionRepository.doRemoveAll(em);
			return null;
		});
	}

	// Getters and setters
	
	public String getFormatVersion() {
		return formatVersion;
	}

	public void setFormatVersion(String formatVersion) {
		this.formatVersion = formatVersion;
	}

	public String getExportDate() {
		return exportDate;
	}

	public void setExportDate(String exportDate) {
		this.exportDate = exportDate;
	}

	public List<JsonNode> getChampionships() {
		return championships;
	}

	public void setChampionships(List<JsonNode> championships) {
		this.championships = championships;
	}

	public List<JsonNode> getAgeGroups() {
		return ageGroups;
	}

	public void setAgeGroups(List<JsonNode> ageGroups) {
		this.ageGroups = ageGroups;
	}

	/** Athletes in their V2 form; {@link V2Json#toAthlete} binds them. */
	public List<JsonNode> getAthletes() {
		return athletes;
	}

	public void setAthletes(List<JsonNode> athletes) {
		this.athletes = athletes;
	}

	public Competition getCompetition() {
		return competition;
	}

	public void setCompetition(Competition competition) {
		this.competition = competition;
		Competition.setCurrent(this.competition);
		// Recompute mustCompute rankings based on imported Competition and age groups
		RankingConfig.updateMustCompute();
	}

	/** Export side: assignment only. {@link #setCompetition} is the import setter and republishes. */
	private void setCompetitionForExport(Competition competition) {
		this.competition = competition;
	}

	public Config getConfig() {
		return config;
	}

	public void setConfig(Config config) {
		this.config = config;
		// import setter (export uses setConfigForExport): the imported settings are in force while the rest is read
		ConfigRepository.replaceForImport(this.config, AuditOrigin.system("import"));
	}

	/** Export side: assignment only. {@link #setConfig} is the import setter and replaces the stored settings. */
	private void setConfigForExport(Config config) {
		this.config = config;
	}

	public List<UserAccount> getAccounts() {
		return accounts;
	}

	public void setAccounts(List<UserAccount> accounts) {
		this.accounts = accounts;
	}

	/** Sessions in their V2 form, see {@link V2Json}; bound to entities during the import transaction. */
	public List<JsonNode> getSessions() {
		return sessions;
	}

	public void setSessions(List<JsonNode> sessions) {
		this.sessions = sessions;
	}

	public List<Platform> getPlatforms() {
		return platforms;
	}

	public void setPlatforms(List<Platform> platforms) {
		this.platforms = platforms;
	}

	public RecordConfig getRecordConfig() {
		return recordConfig;
	}

	public void setRecordConfig(RecordConfig recordConfig) {
		this.recordConfig = recordConfig;
	}

	public List<TeamDTO> getTeams() {
		return teams;
	}

	public void setTeams(List<TeamDTO> teams) {
		this.teams = teams;
	}

	public List<Coach> getCoaches() {
		return coaches;
	}

	public void setCoaches(List<Coach> coaches) {
		this.coaches = coaches;
	}

	public List<RecordEvent> getRecords() {
		return records;
	}

	public void setRecords(List<RecordEvent> records) {
		this.records = records;
	}

	public List<TechnicalOfficial> getTechnicalOfficials() {
		return technicalOfficials;
	}

	public void setTechnicalOfficials(List<TechnicalOfficial> technicalOfficials) {
		this.technicalOfficials = technicalOfficials;
	}

	public List<JsonNode> getTechnicalOfficialsTimetable() {
		return technicalOfficialsTimetable;
	}

	public void setTechnicalOfficialsTimetable(List<JsonNode> timetable) {
		this.technicalOfficialsTimetable = timetable;
	}
}
