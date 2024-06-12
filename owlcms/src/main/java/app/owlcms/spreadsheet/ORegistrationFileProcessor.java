package app.owlcms.spreadsheet;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import ch.qos.logback.classic.Logger;
import net.sf.jxls.reader.ReaderBuilder;
import net.sf.jxls.reader.ReaderConfig;
import net.sf.jxls.reader.XLSReadMessage;
import net.sf.jxls.reader.XLSReadStatus;
import net.sf.jxls.reader.XLSReader;

public class ORegistrationFileProcessor implements IRegistrationFileProcessor {
	static final String GROUPS_READER_SPEC = "/templates/registration/GroupsReader.xml";
	static final String REGISTRATION_READER_SPEC = "/templates/registration/RegistrationReader.xml";
	Logger logger = (Logger) LoggerFactory.getLogger(ORegistrationFileProcessor.class);
	public boolean keepParticipations;

	public ORegistrationFileProcessor() {
	}

	@Override
	public void adjustParticipations() {
		if (!this.keepParticipations) {
			AthleteRepository.resetParticipations();
		}
	}

	@Override
	public String cleanMessage(String localizedMessage) {
		localizedMessage = localizedMessage.replace("Can't read cell ", "");
		String cell = localizedMessage.substring(0, localizedMessage.indexOf(" "));
		String ss = "spreadsheet";
		int ix = localizedMessage.indexOf(ss) + ss.length();
		localizedMessage = localizedMessage.substring(ix);
		if (localizedMessage.trim().contentEquals("text")) {
			localizedMessage = "Empty or invalid.";
		}
		String cleanMessage = Translator.translate("Cell") + " " + cell + ": " + localizedMessage;
		return cleanMessage;
	}

	@Override
	@SuppressWarnings("unchecked")
	public int doProcessAthletes(InputStream inputStream, boolean dryRun, Consumer<String> errorConsumer,
	        Runnable displayUpdater, boolean resetAthletes) {
		try (InputStream xmlInputStream = ResourceWalker.getResourceAsStream(REGISTRATION_READER_SPEC)) {
			inputStream.reset();
			ReaderConfig readerConfig = ReaderConfig.getInstance();
			readerConfig.setUseDefaultValuesForPrimitiveTypes(true);
			readerConfig.setSkipErrors(true);
			XLSReader reader = ReaderBuilder.buildFromXML(xmlInputStream);

			try (InputStream xlsInputStream = inputStream) {
				RCompetition c = new RCompetition();
				if (resetAthletes) {
					RCompetition.resetActiveCategories();
					RCompetition.resetActiveGroups();
					RCompetition.resetAthleteToEligibles();
					RCompetition.resetAthleteToTeams();
				}

				List<RAthlete> athletes = new ArrayList<>();

				Map<String, Object> beans = new HashMap<>();
				beans.put("competition", c);
				beans.put("athletes", athletes);

				XLSReadStatus status = reader.read(inputStream, beans);

				// get back the updated athletes
				athletes = (List<RAthlete>) beans.get("athletes");
				// if exact matches were found for categories, the processing for eligibility
				// has been done, and we keep the eligibilities exactly as in the file.
				this.keepParticipations = athletes.stream()
				        .filter(r -> r.getAthlete().getEligibleCategories() != null).findFirst()
				        .isPresent();

				this.logger.info(Translator.translate("DataRead") + " " + athletes.size() + " athletes");
				if (dryRun) {
					return athletes.size();
				}

				if (athletes.size() > 0) {
					updateAthletes(errorConsumer, c, athletes);
					appendErrors(displayUpdater, errorConsumer, status);
				} else {
					errorConsumer.accept(Translator.translate("NoAthletes"));
					displayUpdater.run();
				}
				if (status.getReadMessages().isEmpty()) {
					// TODO: add UI Event to invalidate all sessions.
					OwlcmsSession.invalidate();
				}
				return athletes.size();
			} catch (InvalidFormatException | IOException e) {
				LoggerUtils.stackTrace(e);
				LoggerUtils.logError(this.logger, e);
			}
		} catch (IOException | SAXException e1) {
			LoggerUtils.stackTrace(e1);
			LoggerUtils.logError(this.logger, e1);
		}
		return 0;
	}

	@Override
	public int doProcessGroups(InputStream inputStream, boolean dryRun, Consumer<String> errorConsumer,
	        Runnable displayUpdater) {
		try (InputStream xmlInputStream = ResourceWalker.getResourceAsStream(GROUPS_READER_SPEC)) {
			inputStream.reset();
			ReaderConfig readerConfig = ReaderConfig.getInstance();
			readerConfig.setUseDefaultValuesForPrimitiveTypes(true);
			readerConfig.setSkipErrors(true);
			XLSReader reader = ReaderBuilder.buildFromXML(xmlInputStream);

			try (InputStream xlsInputStream = inputStream) {
				List<RGroup> groups = new ArrayList<>();

				Map<String, Object> beans = new HashMap<>();
				beans.put("groups", groups);

				// logger.info(Translator.translate("ReadingData_"));
				XLSReadStatus status = reader.read(inputStream, beans);
				this.logger.info("Read {} groups.", groups.size());
				if (!dryRun) {
					updatePlatformsAndGroups(groups);
				}

				appendErrors(displayUpdater, errorConsumer, status);
				return groups.size();
			} catch (InvalidFormatException | IOException e) {
				LoggerUtils.logError(this.logger, e);
			}
		} catch (IOException | SAXException e1) {
			LoggerUtils.logError(this.logger, e1);
		}
		return 0;
	}

	@Override
	public void resetAthletes() {
		// delete all athletes and groups (naive version).
		JPAService.runInTransaction(em -> {
			List<Athlete> athletes = AthleteRepository.doFindAll(em);
			for (Athlete a : athletes) {
				em.remove(a);
			}
			em.flush();
			return null;
		});
	}

	@Override
	public void resetGroups() {
		// delete all athletes and groups (naive version).
		JPAService.runInTransaction(em -> {
			List<Group> oldGroups = GroupRepository.doFindAll(em);
			for (Group g : oldGroups) {
				em.remove(g);
			}
			em.flush();
			return null;
		});
		this.logger.info("previous groups removed");
	}

	@Override
	public void updateAthletes(Consumer<String> errorConsumer, RCompetition c, List<RAthlete> athletes) {
		JPAService.runInTransaction(em -> {
			Competition curC = Competition.getCurrent();
			try {
				Competition rCompetition = c.getCompetition();
				// save some properties from current database that do not appear on spreadheet
				rCompetition.setEnforce20kgRule(curC.isEnforce20kgRule());
				rCompetition.setUseBirthYear(curC.isUseBirthYear());
				rCompetition.setMasters(curC.isMasters());

				// update the current competition with the new properties read from spreadsheet
				BeanUtils.copyProperties(curC, rCompetition);
				// update in database and set current to result of JPA merging.
				Competition.setCurrent(em.merge(curC));

				// Create the new athletes.
				athletes.stream().forEach(r -> {
					Athlete athlete = r.getAthlete();
					em.merge(athlete);
				});
				em.flush();
			} catch (IllegalAccessException | InvocationTargetException | RuntimeException e) {
				LoggerUtils.stackTrace(e);
				errorConsumer.accept(e.getLocalizedMessage());
			}

			return null;
		});

		JPAService.runInTransaction(em -> {
			AthleteRepository.findAll().stream().forEach(a2 -> {
				LinkedHashSet<Category> eligibles = (LinkedHashSet<Category>) RCompetition
				        .getAthleteToEligibles()
				        .get(a2.getId());
				LinkedHashSet<Category> teams = (LinkedHashSet<Category>) RCompetition
				        .getAthleteToTeams()
				        .get(a2.getId());
				if (eligibles != null) {
					Category first = eligibles.stream().findFirst().orElse(null);
					a2.setCategory(first);
					// logger.debug("setting eligibility {} {}", a2.getShortName(), eligibles);
					a2.setEligibleCategories(eligibles);
					List<Participation> participations2 = a2.getParticipations();
					for (Participation p : participations2) {
						if (teams.contains(p.getCategory())) {
							p.setTeamMember(true);
						} else {
							this.logger.info("Excluding {} as team member for {}", a2.getShortName(),
							        p.getCategory().getComputedCode());
							p.setTeamMember(false);
						}
					}
					// logger.debug("participations {} {}", a2.getShortName(), a2.getParticipations());
					em.merge(a2);
				}
			});
			em.flush();
			return null;
		});
	}

	@Override
	public void updatePlatformsAndGroups(List<RGroup> groups) {
		Set<String> futurePlatforms = groups.stream().map(RGroup::getPlatform).filter(p -> (p != null && !p.isBlank()))
		        .collect(Collectors.toSet());

		String defaultPlatformName = OwlcmsFactory.getDefaultFOP().getName();
		if (futurePlatforms.isEmpty()) {
			// keep the current default if no group is linked to a platform.
			futurePlatforms.add(defaultPlatformName);
		}
		this.logger.debug("to be kept if present: {}", futurePlatforms);

		PlatformRepository.deleteUnusedPlatforms(futurePlatforms);
		PlatformRepository.createMissingPlatforms(groups);

		// recompute the available platforms, unregister the existing FOPs, etc.
		OwlcmsFactory.initDefaultFOP();
		String newDefault = OwlcmsFactory.getDefaultFOP().getName();

		JPAService.runInTransaction(em -> {
			groups.stream().forEach(g -> {
				String platformName = g.getPlatform();
				Group readGroup = g.getGroup();
				Group group = GroupRepository.doFindByName(g.getGroupName(), em);
				if (group == null) {
					// new group
					group = readGroup;
				} else {
					try {
						group.copy(readGroup);
					} catch (IllegalAccessException | InvocationTargetException e) {
						this.logger.error(LoggerUtils.shortStackTrace(e));
					}
				}

				if (platformName == null || platformName.isBlank()) {
					platformName = newDefault;
				}
				this.logger.info("setting platform '{}' for group {}", platformName, g.getGroupName());
				Platform op = PlatformRepository.findByName(platformName);
				group.setPlatform(op);
				em.merge(group);
			});
			em.flush();
			return null;
		});

		groups.stream().forEach(g -> {
			this.logger.debug("group {} weighIn {} competition {}", g.getGroup(), g.getWeighinTime(),
			        g.getCompetitionTime());
		});
	}

	private void appendErrors(Runnable displayUpdater, Consumer<String> errorAppender, XLSReadStatus status) {
		@SuppressWarnings("unchecked")
		List<XLSReadMessage> errors = status.getReadMessages();
		for (XLSReadMessage m : errors) {
			String cleanMessage = cleanMessage(m.getMessage());
			errorAppender.accept(cleanMessage);
			Exception e = m.getException();
			if (e != null) {
				Throwable cause = e.getCause();
				String causeMessage = cause != null ? cause.getLocalizedMessage() : e.getLocalizedMessage();
				// causeMessage = LoggerUtils.stackTrace(cause);
				causeMessage = causeMessage != null ? causeMessage : e.toString();
				if (causeMessage.contentEquals("text")) {
					causeMessage = "Empty or invalid.";
				}
				errorAppender.accept(causeMessage);
				this.logger.debug(cleanMessage + causeMessage);
			}
			errorAppender.accept(System.lineSeparator());
		}
		displayUpdater.run();
	}
}