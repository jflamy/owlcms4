/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.export.v2;

import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;

import java.time.Instant;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.agegroup.ChampionshipRepository;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.RankingConfig;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
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
	"championships",
	"ageGroups",
	"teams",
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
	
	private String formatVersion = "2.0";
	private String exportDate;
	private Competition competition;
	private Config config;
	private List<ChampionshipDTO> championships;
	private List<AgeGroupDTO> ageGroups;
	private List<TeamDTO> teams;
	private List<SessionDTO> sessions;
	private List<AthleteDTO> athletes;
	private List<Platform> platforms;
	private List<RecordEvent> records;
	private RecordConfig recordConfig;
	private List<TechnicalOfficial> technicalOfficials;
	private List<TimetableEntryDTO> technicalOfficialsTimetable;

	public CompetitionDataV2() {
	}

	private static ObjectMapper createMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return mapper;
	}

	public InputStream exportData() {
		ObjectMapper mapper = createMapper();
		try {
			ObjectWriter writerWithDefaultPrettyPrinter = mapper.writerWithDefaultPrettyPrinter();

			PipedOutputStream out = new PipedOutputStream();
			PipedInputStream in = new PipedInputStream(out);
			new Thread(() -> {
				try {
					writerWithDefaultPrettyPrinter.writeValue(out, this);
					out.flush();
					out.close();
				} catch (Throwable e) {
					LoggerUtils.logError(logger, e);
				}
			}).start();
			return in;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public InputStream exportData(UI ui, Notification notification) {
		if (ui != null) {
			ui.access(() -> notification.open());
		}
		ObjectMapper mapper = createMapper();
		try {
			ObjectWriter writerWithDefaultPrettyPrinter = mapper.writerWithDefaultPrettyPrinter();

			PipedOutputStream out = new PipedOutputStream();
			PipedInputStream in = new PipedInputStream(out);
			new Thread(() -> {
				try {
					writerWithDefaultPrettyPrinter.writeValue(out, this.fromDatabase());
					out.flush();
					out.close();
					if (ui != null) {
						ui.access(() -> notification.close());
					}
				} catch (Throwable e) {
					LoggerUtils.logError(logger, e);
				}
			}).start();
			return in;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public CompetitionDataV2 fromDatabase() {
		// Set export timestamp in ISO 8601 format
		setExportDate(Instant.now().toString());

		setChampionships(Championship.findAllIncludingTemplate().stream()
			.map(ChampionshipDTO::fromChampionship)
			.collect(Collectors.toList()));
		
		// Convert AgeGroups to DTOs with category codes
		setAgeGroups(AgeGroupRepository.findAll().stream()
			.map(AgeGroupDTO::fromAgeGroup)
			.collect(Collectors.toList()));
		
		List<Athlete> allAthletes = AthleteRepository.findAll()
		        .stream()
		        .collect(Collectors.toList());
		
		// Build team map from unique team names
		Map<String, TeamDTO> teamMap = new HashMap<>();
		for (Athlete athlete : allAthletes) {
			String teamName = athlete.getTeam();
			if (teamName != null && !teamName.trim().isEmpty() && !teamMap.containsKey(teamName)) {
				teamMap.put(teamName, new TeamDTO(teamName));
			}
		}
		setTeams(teamMap.values().stream().collect(Collectors.toList()));
		
		// Convert sessions first so athletes can reference sessions on import
		List<Group> allGroups = GroupRepository.findAll();
		setSessions(allGroups.stream()
			.map(SessionDTO::fromGroup)
			.collect(Collectors.toList()));

		// Convert athletes with team references (after sessions)
		setAthletes(allAthletes.stream()
			.map(a -> AthleteDTO.fromAthlete(a, teamMap))
			.collect(Collectors.toList()));
		
		setPlatforms(PlatformRepository.findAll());
		Config config = Config.getCurrent();
		config.setAppVersion(StartupUtils.getVersion());
		setConfig(config);
		setCompetition(Competition.getCurrent());
		setRecords(RecordRepository.findAll());
		setRecordConfig(RecordConfig.getCurrent());
		setTechnicalOfficials(TechnicalOfficialRepository.findAll());
        
		// Convert timetable entries from V2 format (DTO with session names)
		setTechnicalOfficialsTimetable(
			JPAService.runInTransaction(em -> TechnicalOfficialsTimetableRepository.findAll(em)).stream()
				.map(TimetableEntryDTO::fromEntity)
				.collect(Collectors.toList())
		);
		
		return this;
	}

	public CompetitionDataV2 importData(InputStream serialized) {
		ObjectMapper mapper = createMapper();
		CompetitionDataV2 newData;
		try {
			newData = mapper.readValue(serialized, CompetitionDataV2.class);
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
			return null;
		}
	}

	public void restore(InputStream inputStream) {
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
				Config.setCurrent(config);
				
				ResourceWalker.setInitializedLocalDir(false);
				ResourceWalker.initLocalDir();

				Translator.reset();
				Translator.setForcedLocale(config.getDefaultLocale());

				Competition competition = updated.getCompetition();
				Competition.setCurrent(competition);			// Recompute mustCompute rankings based on imported Competition and age groups
			RankingConfig.updateMustCompute();
			if (updated.getChampionships() != null) {
				for (ChampionshipDTO championshipDto : updated.getChampionships()) {
					Championship championship = championshipDto.toChampionship();
					Championship existing = championship.isCompetitionTemplate()
					        ? ChampionshipRepository.ensureCompetitionTemplate(em)
					        : ChampionshipRepository.findByName(championship.getName());
					if (existing == null) {
						em.persist(championship);
					} else {
						existing.setCompetitionTemplate(championship.isCompetitionTemplate());
						existing.setType(championship.getType());
						existing.setUseCompetitionDefaults(championship.usesCompetitionDefaults());
						existing.setScoringSystem(championship.getScoringSystem());
						existing.setBestAthleteScoringSystem(championship.getBestAthleteScoringSystem());
						existing.setBestSnatchScoringSystem(championship.getBestSnatchScoringSystem());
						existing.setBestCJScoringSystem(championship.getBestCJScoringSystem());
						existing.setSnatchCJTotalMedals(championship.isSnatchCJTotalMedals());
						existing.setTeamPoints1st(championship.getTeamPoints1st());
						existing.setTeamPoints2nd(championship.getTeamPoints2nd());
						existing.setTeamPoints3rd(championship.getTeamPoints3rd());
						existing.setMensBestN(championship.getMensBestN());
						existing.setWomensBestN(championship.getWomensBestN());
						existing.setMixedMensBestN(championship.getMixedMensBestN());
						existing.setMixedWomensBestN(championship.getMixedWomensBestN());
						existing.setMixedBestN(championship.getMixedBestN());
						existing.setExplicitTeamSize(championship.getExplicitTeamSize());
						existing.setMaxTeamSize(championship.getMaxTeamSize());
						existing.setMaxPerCategory(championship.getMaxPerCategory());
						existing.setExplicitMixedTeamMembers(championship.isExplicitMixedTeamMembers());
						existing.setMixedTeamEnabled(championship.isMixedTeamEnabled());
						existing.setTeamScoringSystem(championship.getTeamScoringSystem());
						existing.setMixedTeamScoringSystem(championship.getMixedTeamScoringSystem());
						em.merge(existing);
					}
				}
				em.flush();
			}
			for (AgeGroupDTO agDto : updated.getAgeGroups()) {
				// Convert DTO to entity
				AgeGroup ag = agDto.toAgeGroup();
			em.persist(ag);
		}
		// Flush to ensure categories from AgeGroups are available for lookup
		em.flush();

			if (updated.getPlatforms() != null) {
				for (Platform p : updated.getPlatforms()) {
					em.merge(p);
				}
				em.flush();
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
		for (SessionDTO sDto : updated.getSessions()) {
			Group g = sDto.toGroup(em);
			em.merge(g);
		}

		// Now create Athletes which will look up their Groups by name
		for (AthleteDTO aDto : updated.getAthletes()) {
			Athlete a = aDto.toAthlete(em, teamIdToNameMap);
			em.persist(a);
		}

		if (updated.getRecordConfig() != null) {
			em.merge(updated.getRecordConfig());
		}

		if (updated.getRecords() != null) {
			for (RecordEvent r : updated.getRecords()) {
				em.merge(r);
			}
		}

		if (updated.getTechnicalOfficials() != null) {
			for (TechnicalOfficial p : updated.getTechnicalOfficials()) {
				em.merge(p);
			}
		}
		
		// Restore timetable entries: convert DTOs to entities, looking up Groups by session name
		if (updated.getTechnicalOfficialsTimetable() != null) {
			for (TimetableEntryDTO dto : updated.getTechnicalOfficialsTimetable()) {
				if (dto.getSessionName() != null) {
					Group group = GroupRepository.findByName(dto.getSessionName());
					if (group != null) {
						TechnicalOfficialsTimetable entity = dto.toEntity(group);
						em.merge(entity);
					}
				}
			}
		}
		
		em.merge(competition);
		em.flush();
		} catch (Exception e) {
			LoggerUtils.logError(logger, e);
		} finally {
			Athlete.setSkipValidationsDuringImport(false);
		}
		return null;
		});
		
		RecordConfig current = RecordConfig.getCurrent();
		current.addMissing(RecordRepository.findAllRecordNames());
	}

	private void removeAll() {
		// Use existing removal logic from CompetitionRepository
		JPAService.runInTransaction(em -> {
			app.owlcms.data.competition.CompetitionRepository.doRemoveAll(em);
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

	public List<ChampionshipDTO> getChampionships() {
		return championships;
	}

	public void setChampionships(List<ChampionshipDTO> championships) {
		this.championships = championships;
	}

	public List<AgeGroupDTO> getAgeGroups() {
		return ageGroups;
	}

	public void setAgeGroups(List<AgeGroupDTO> ageGroups) {
		this.ageGroups = ageGroups;
	}

	public List<AthleteDTO> getAthletes() {
		return athletes;
	}

	public void setAthletes(List<AthleteDTO> athletes) {
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

	public Config getConfig() {
		return config;
	}

	public void setConfig(Config config) {
		this.config = config;
		Config.setCurrent(this.config);
	}

	public List<SessionDTO> getSessions() {
		return sessions;
	}

	public void setSessions(List<SessionDTO> sessions) {
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

	public List<TimetableEntryDTO> getTechnicalOfficialsTimetable() {
		return technicalOfficialsTimetable;
	}

	public void setTechnicalOfficialsTimetable(List<TimetableEntryDTO> timetable) {
		this.technicalOfficialsTimetable = timetable;
	}
}
