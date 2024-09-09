/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.export;

import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.competition.CompetitionRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.data.records.RecordConfig;
import app.owlcms.data.records.RecordEvent;
import app.owlcms.data.records.RecordRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import ch.qos.logback.classic.Logger;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CompetitionData {

	final static Logger logger = (Logger) LoggerFactory.getLogger(CompetitionData.class);
	private List<AgeGroup> ageGroups;
	private List<Athlete> athletes;
	private Competition competition;
	private Config config;
	private List<Group> groups;
	private List<Platform> platforms;
	private List<RecordEvent> records;
	private RecordConfig recordConfig;

	public CompetitionData() {
	}

	public InputStream exportData() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		try {
			ObjectWriter writerWithDefaultPrettyPrinter = mapper.writerWithDefaultPrettyPrinter();

			PipedOutputStream out = new PipedOutputStream();
			PipedInputStream in = new PipedInputStream(out);
			new Thread(() -> {
				try {
					writerWithDefaultPrettyPrinter.writeValue(out, this.fromDatabase());
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
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
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

	/**
	 * For debugging
	 *
	 * @return
	 */
	public String exportDataAsString() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		ObjectWriter writerWithDefaultPrettyPrinter = mapper.writerWithDefaultPrettyPrinter();
		String serialized;
		try {
			serialized = writerWithDefaultPrettyPrinter.writeValueAsString(this.fromDatabase());
			// System.out.println(serialized);
			return serialized;
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public CompetitionData fromDatabase() {
		setAgeGroups(AgeGroupRepository.findAll());
		List<Athlete> allAthletes = AthleteRepository
		        .findAll()
		        .stream()
		        // .findFirst()
		        // .map(Arrays::asList).orElseGet(List::of);
		        .collect(Collectors.toList());

		setAthletes(allAthletes);
		setGroups(GroupRepository.findAll());
		setPlatforms(PlatformRepository.findAll());
		setConfigForExport(Config.getCurrent());
		setCompetitionForExport(Competition.getCurrent());
		setRecords(RecordRepository.findAll());
		setRecordConfig(RecordConfig.getCurrent());
		return this;
	}

	@JsonProperty(index = 30)
	public List<AgeGroup> getAgeGroups() {
		return this.ageGroups;
	}

	@JsonProperty(index = 40)
	public List<Athlete> getAthletes() {
		return this.athletes;
	}

	@JsonProperty(index = 5)
	public Competition getCompetition() {
		return this.competition;
	}

	@JsonProperty(index = 1)
	public Config getConfig() {
		return this.config;
	}

	@JsonProperty(index = 20)
	public List<Group> getGroups() {
		return this.groups;
	}

	@JsonProperty(index = 10)
	public List<Platform> getPlatforms() {
		return this.platforms;
	}

	@JsonProperty(index = 60)
	public RecordConfig getRecordConfig() {
		return this.recordConfig;
	}

	@JsonProperty(index = 50)
	public List<RecordEvent> getRecords() {
		return this.records;
	}

	public CompetitionData importData(InputStream serialized) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		CompetitionData newData;
		try {
			newData = mapper.readValue(serialized, CompetitionData.class);
			logger.debug("after unmarshall {}", newData.getPlatforms());
			return newData;
		} catch (Exception e) {
			LoggerUtils.logError(logger, e);
			return null;
		}
	}

	public CompetitionData importDataFromString(String serialized)
	        throws JsonMappingException, JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		CompetitionData newData = mapper.readValue(serialized, CompetitionData.class);
		// logger.debug("after unmarshall {}", newData.getPlatforms());
		return newData;
	}

	public void restore(InputStream inputStream) {
		this.removeAll();
		JPAService.runInTransaction(em -> {
			try {
				Athlete.setSkipValidationsDuringImport(true);
				OwlcmsFactory.resetFOPByName();

				CompetitionData updated = this.importData(inputStream);
				Config config = updated.getConfig();
				byte[] blob = config.getLocalZipBlob();
				
				if ( blob != null) {
					logger.info("override zip found {} bytes", blob.length);
				}
				Config.setCurrent(config);
				ResourceWalker.setInitializedLocalDir(false);
				ResourceWalker.initLocalDir();

				Locale defaultLocale = config.getDefaultLocale();
				Translator.reset();
				Translator.setForcedLocale(defaultLocale);

				Competition competition = updated.getCompetition();

				for (AgeGroup ag : updated.getAgeGroups()) {
					em.persist(ag);
				}

				for (Athlete a : updated.getAthletes()) {
					em.persist(a);
				}

				for (Group g : updated.getGroups()) {
					em.merge(g);
				}

				if (updated.getRecords() != null) {
					for (RecordEvent r : updated.getRecords()) {
						em.merge(r);
					}
				}

				if (updated.getPlatforms() != null) {
					for (Platform p : updated.getPlatforms()) {
						em.merge(p);
					}
				}

				if (updated.getRecordConfig() != null) {
						em.merge(updated.getRecordConfig());
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
		Championship.reset();
		CategoryRepository.resetCodeMap();
		// register the new FOPs for events and MQTT
		OwlcmsFactory.initDefaultFOP();

		// set the record order if empty (compensate for issue #766)
		RecordConfig current = RecordConfig.getCurrent();
		current.addMissing(RecordRepository.findAllRecordNames());
	}

	public void setAgeGroups(List<AgeGroup> ageGroups) {
		this.ageGroups = ageGroups;
	}

	public void setAthletes(List<Athlete> athletes) {
		this.athletes = athletes;
	}

	/**
	 * When importing data, set the imported Competition instance as the current instance. This is required because it
	 * affects how some objects are processed (e.g., birth dates).
	 *
	 * @param competition the competition to set
	 */
	public void setCompetition(Competition competition) {
		this.competition = competition;
		Competition.setCurrent(this.competition);
		logger.info("Applied imported Competition settings. useBirthYear={}",
		        Competition.getCurrent().isUseBirthYear());
	}

	/**
	 * When importing data, set the imported Competition instance as the current instance. This is prudent in case the
	 * configuration might affect further processing.
	 *
	 * @param config the config to set
	 */
	public void setConfig(Config config) {
		this.config = config;
		Config.setCurrent(this.getConfig());
		logger.info("Applied imported language and system settings.");
	}

	public void setGroups(List<Group> groups) {
		this.groups = groups;
	}

	public void setPlatforms(List<Platform> platforms) {
		this.platforms = platforms;
	}

	public void setRecordConfig(RecordConfig recordConfig) {
		this.recordConfig = recordConfig;
	}

	public void setRecords(List<RecordEvent> records) {
		this.records = records;
	}

	private void removeAll() {
		JPAService.runInTransaction(em -> {
			CompetitionRepository.doRemoveAll(em);
			try {
				this.fromDatabase();
				for (Athlete a : this.getAthletes()) {
					Athlete aX = em.find(Athlete.class, a.getId());
					if (aX != null) {
						em.remove(aX);
					}
				}
				for (Group g : this.getGroups()) {
					Group gX = em.find(Group.class, g.getId());
					if (gX != null) {
						em.remove(gX);
					}
				}
				for (AgeGroup ag : this.getAgeGroups()) {
					AgeGroup agX = em.find(AgeGroup.class, ag.getId());
					if (agX != null) {
						em.remove(agX);
					}
				}
				for (Platform p : this.getPlatforms()) {
					Platform pX = em.find(Platform.class, p.getId());
					if (pX != null) {
						em.remove(pX);
					}
				}
			} catch (Exception e) {
				LoggerUtils.logError(logger, e);
			}
			return null;
		});
	}

	/**
	 * @param competition the competition to set
	 */
	private void setCompetitionForExport(Competition competition) {
		this.competition = competition;
	}

	/**
	 *
	 * @param config the config to set
	 */
	private void setConfigForExport(Config config) {
		this.config = config;
	}
}
