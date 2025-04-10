/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import app.owlcms.components.GroupCategorySelectionMenu.TriConsumer;
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
import app.owlcms.utils.DateTimeUtils;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import ch.qos.logback.classic.Logger;
import net.sf.jxls.reader.ReaderBuilder;
import net.sf.jxls.reader.ReaderConfig;
import net.sf.jxls.reader.XLSReadMessage;
import net.sf.jxls.reader.XLSReadStatus;
import net.sf.jxls.reader.XLSReader;

public class NRegistrationFileProcessor {

	/* some setters must be called in a specific order; */
	private enum DelayedSetter {
		BIRTHDATE, BODYWEIGHT, QUALIFYING_TOTAL, GENDER, CATEGORY
	}

	public enum SessionOptions {
		IGNORE_SESSIONS, DELETE_SESSIONS, UPDATE_ADD_SESSIONS
	}

	public enum AthleteOptions {
		IGNORE_ATHLETES, DELETE_ATHLETES, UPDATE_ADD_ATHLETES, ADD_ATHLETES
	}

	static final String GROUPS_READER_SPEC = "/templates/registration/GroupsReader.xml";
	Integer[] delayedSetterColumns = new Integer[DelayedSetter.values().length];
	Logger logger = (Logger) LoggerFactory.getLogger(NRegistrationFileProcessor.class);
	//private boolean keepParticipations;
	@SuppressWarnings("unchecked")
	TriConsumer<RAthlete, String, Cell>[] setterForColumn = new TriConsumer[25];
	FormulaEvaluator formulaEvaluator;
	DataFormatter formatter;
	private boolean sbdeFormat;
	private SessionOptions sessionOptions;
	private AthleteOptions athleteOptions;

	public NRegistrationFileProcessor(boolean sbdeFormat) {
		this.sbdeFormat = sbdeFormat;
	}

	public void adjustParticipations() {
//		if (!this.keepParticipations) {
//			AthleteRepository.resetParticipations(false, true);
//		}
	}

	private String cleanMessage(String localizedMessage) {
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

	@SuppressWarnings("unchecked")
	public int doProcessAthletes(InputStream inputStream, boolean dryRun, Consumer<String> errorConsumer,
	        Runnable displayUpdater) {
		try (InputStream xlsInputStream = inputStream) {
			inputStream.reset();

			RCompetition c = new RCompetition();
			RCompetition.resetActiveCategories();
			RCompetition.resetActiveGroups();
			if (isDeleteAthletes()) {
				RCompetition.resetAthleteToEligibles();
				RCompetition.resetAthleteToTeams();
			}

			List<RAthlete> athletes = new ArrayList<>();
			try (Workbook workbook = WorkbookFactory.create(xlsInputStream)) {
				this.formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
				this.formatter = new DataFormatter();
				// pass the number of rows to skip
				athletes = readAthletes(workbook, c, errorConsumer, sbdeFormat ? 8 : 0);
			} catch (IOException | EncryptedDocumentException e) {
				errorConsumer.accept(e.getLocalizedMessage());
				LoggerUtils.logError(this.logger, e);
				return 0;
			}

			this.logger.info(Translator.translate("DataRead") + " " + athletes.size() + " athletes");
			if (dryRun) {
				return athletes.size();
			}

			if (athletes.size() > 0) {
				updateAthletes(errorConsumer, c, athletes);
				appendErrors(displayUpdater, errorConsumer);
			} else {
				errorConsumer.accept(Translator.translate("NoAthletes"));
				displayUpdater.run();
			}
			return athletes.size();
		} catch (Exception e) {
			LoggerUtils.stackTrace(e);
			LoggerUtils.logError(this.logger, e);
		}
		return 0;
	}

	public boolean isDeleteAthletes() {
		return getAthleteOptions() == AthleteOptions.DELETE_ATHLETES;
	}

	public int doProcessGroups(InputStream inputStream, boolean dryRun, Consumer<String> errorConsumer,
	        Runnable displayUpdater) {
		try (InputStream xmlInputStream = ResourceWalker.getResourceAsStream(GROUPS_READER_SPEC)) {
			inputStream.reset();
			ReaderConfig readerConfig = ReaderConfig.getInstance();
			readerConfig.setUseDefaultValuesForPrimitiveTypes(true);
			readerConfig.setSkipErrors(true);
			XLSReader reader = ReaderBuilder.buildFromXML(xmlInputStream);

			try (InputStream xlsInputStream = inputStream) {
				List<RGroup> sessions = new ArrayList<>();

				Map<String, Object> beans = new HashMap<>();
				beans.put("groups", sessions);

				// logger.info(Translator.translate("ReadingData_"));
				XLSReadStatus status = reader.read(inputStream, beans);
				this.logger.info("Read {} sessions.", sessions.size());
				if (!dryRun) {
					updatePlatformsAndSessions(sessions);
				}

				appendErrors(displayUpdater, errorConsumer, status);
				return sessions.size();
			} catch (InvalidFormatException | IOException e) {
				LoggerUtils.logError(this.logger, e);
			}
		} catch (IOException | SAXException e1) {
			LoggerUtils.logError(this.logger, e1);
		}
		return 0;
	}

	private boolean isCreateMissingSessions() {
		return this.getSessionOptions() == SessionOptions.UPDATE_ADD_SESSIONS;
	}

	public void resetAthletes() {
		// delete all athletes and sessions (naive version).
		JPAService.runInTransaction(em -> {
			List<Athlete> athletes = AthleteRepository.doFindAll(em);
			for (Athlete a : athletes) {
				em.remove(a);
			}
			em.flush();
			return null;
		});
	}

	public void resetSessions() {
		// remove the athletes from the sessions prior to deleting the sessions.
		// same as using the delete button on a session.
		JPAService.runInTransaction(em -> {
			List<Group> oldSessions = GroupRepository.doFindAll(em);
			for (Group g : oldSessions) {
				GroupRepository.doDelete(g, em);
			}
			em.flush();
			return null;
		});
	}

	/**
	 * Update the athletes present in the database.
	 * 
	 * In the most common SBDE scenarios, the athletes are removed first.
	 * 
	 */
	private void updateAthletes(Consumer<String> errorConsumer, RCompetition c, List<RAthlete> sbdeAthletes) {
		Map<String, Athlete> allAthletes = new HashMap<>();

		JPAService.runInTransaction(em -> {
			// retrieve existing ids
			AthleteRepository.doFindAll(em).stream()
			        .forEach(a -> {
				        String athleteKey = athleteKey(a);
				        allAthletes.put(athleteKey, a);

				        // copy the participation categories away
				        RCompetition.putEligibles(a.getId(), new LinkedHashSet<>(a.getEligibleCategories()));
				        RCompetition.putTeams(a.getId(), a.getTeams());
				        a.getParticipations().clear();
				        
				        // reset the group that was cleared.
						String sessionCode = RCompetition.getSessionCode(a.getId());
						logger.warn("++++++ prior session code for {} = {}",a.getFullId(),sessionCode);
						a.setGroup(RCompetition.activeGroups.get(sessionCode));
						logger.warn("++++++ new session for {} = {}",a.getFullId(), a.getGroup());
				        em.merge(a);
				        em.flush();
			        });
			return null;
		});

		List<Athlete> toBeMerged = new ArrayList<>(allAthletes.size());
		// Create the new athletes.
		sbdeAthletes.stream().forEach(r -> {
			Athlete sbdeAthlete = r.getAthlete();
			String athleteKey = athleteKey(sbdeAthlete);

			Athlete existingAthlete = allAthletes.get(athleteKey(sbdeAthlete));
			if (existingAthlete != null) {
				if (isUpdateExistingAthletes() || isDeleteAthletes()) {
					existingAthlete.getParticipations().clear();
					logger.warn("* existing athlete {} {} {}", existingAthlete.getAbbreviatedName(), existingAthlete.getId(),
					        existingAthlete.getParticipations());
					logger.warn("* sbde {} {}", sbdeAthlete.getAbbreviatedName(), sbdeAthlete.getId(), sbdeAthlete.getParticipations());
					updateExistingAthlete(existingAthlete, sbdeAthlete);
					toBeMerged.add(existingAthlete);
				} else {
					throw new IllegalArgumentException("Duplicate Athlete Entry " + athleteKey);
				}
			} else {
				sbdeAthlete.setCategoryFinished(false);
				logger.warn("adding sbdeAthlete {} {}", sbdeAthlete.getShortName(), sbdeAthlete.getId());
				toBeMerged.add(sbdeAthlete);
			}
		});

		logger.warn(") step 2");
		JPAService.runInTransaction(em -> {
			try {
				for (Athlete a : toBeMerged) {
					logger.warn("merging {} {}", a.getAbbreviatedName(), a.getId());
					em.merge(a);
				}
				em.flush();
			} catch (Exception e) {
				LoggerUtils.stackTrace(e);
				errorConsumer.accept(e.toString());
			}
			return null;
		});
		logger.warn("( step 2");

		JPAService.runInTransaction(em -> {
			logger.warn(") step 3 - database athletes");
			AthleteRepository.findAll().stream().forEach(a2 -> {
				LinkedHashSet<Category> eligibles = RCompetition
				        .getAthleteToEligibles()
				        .get(a2.getId());
				LinkedHashSet<Category> teams = RCompetition
				        .getAthleteToTeams()
				        .get(a2.getId());
				if (teams == null) {
					logger.warn("no teams for athlete {}", a2.getFullId());
					teams = new LinkedHashSet<Category>();
				}
				logger.warn("athlete {} eligibles {}", a2.getId(), eligibles);
				if (eligibles != null) {
					Category first = eligibles.stream().findFirst().orElse(null);
					a2.setCategory(first);
					a2.setCategoryFinished(false);

					if (!a2.getEligibleCategories().isEmpty()) {
						logger.error("eligibility already set for {}", a2.getShortName());
					} else {
						logger.warn("setting eligibility {} {}", a2.getShortName(), eligibles);
						a2.setEligibleCategories(eligibles);
					}
					List<Participation> participations2 = a2.getParticipations();
					for (Participation p : participations2) {
						if (teams.contains(p.getCategory())) {
							p.setTeamMember(true);
						} else {
							this.logger.debug("Excluding {} as team member for {}", a2.getShortName(),
							        p.getCategory().getComputedCode());
							p.setTeamMember(false);
						}
					}
					logger.warn("participations {} {}", a2.getShortName(), a2.getParticipations());
					em.merge(a2);
				}
			});
			em.flush();
			logger.warn(") step 3");
			return null;
		});
	}

	/**
	 * Update the athlete, except for the athlete card information that was signed, which is kept from the current athlete.
	 * 
	 * @param existingAthlete
	 * @param sbdeAthlete
	 */
	private void updateExistingAthlete(Athlete existingAthlete, Athlete sbdeAthlete) {
		// keep the bw, declarations, changes, and actual lifts from the existing athlete
		// must fix participations to point to the existing athlete, not the sbde athlete.
		System.err.println("> updateExistingAthlete");
		Athlete.conditionalCopy(existingAthlete, sbdeAthlete, false, false, false);
		RCompetition.putEligibles(existingAthlete.getId(), RCompetition.getEligibles(sbdeAthlete.getId()));
		RCompetition.putTeams(existingAthlete.getId(), RCompetition.getTeams(sbdeAthlete.getId()));
		System.err.println("< updateExistingAthlete");
	}

	private String athleteKey(Athlete a) {
		return a.getLastName() + "_" + a.getFirstName() + "_" + a.getLotNumber();
	}

	@SuppressWarnings("unused")
	private void updatePlatformsAndSessions(List<RGroup> sessions) {
		Set<String> futurePlatforms = sessions.stream().map(RGroup::getPlatform).filter(p -> (p != null && !p.isBlank()))
		        .collect(Collectors.toSet());

		String defaultPlatformName = OwlcmsFactory.getDefaultFOP().getName();
		if (futurePlatforms.isEmpty()) {
			// keep the current default if no group is linked to a platform.
			futurePlatforms.add(defaultPlatformName);
		}
		this.logger.debug("to be kept if present: {}", futurePlatforms);

		PlatformRepository.deleteUnusedPlatforms(futurePlatforms);
		PlatformRepository.createMissingPlatforms(sessions);

		// recompute the available platforms, unregister the existing FOPs, etc.
		OwlcmsFactory.initDefaultFOP();
		String newDefault = OwlcmsFactory.getDefaultFOP().getName();

		JPAService.runInTransaction(em -> {
			sessions.stream().forEach(g -> {
				String platformName = g.getPlatform();
				Group readGroup = g.getGroup();
				Group existingGroup = GroupRepository.doFindByName(g.getGroupName(), em);
				if (platformName == null || platformName.isBlank()) {
					platformName = newDefault;
				}

				Platform op = PlatformRepository.findByName(platformName);
				if (existingGroup == null) {
					// create a new group
					readGroup.setPlatform(op);
					this.logger.info("setting platform '{}' for group {}", platformName, g.getGroupName());
					em.merge(readGroup);
				} else {
					// update the existing group
					existingGroup.copyFrom(readGroup);
					existingGroup.setPlatform(op);
					logger.warn("updating platorm for {} to {}", existingGroup, existingGroup.getPlatform());
					em.merge(existingGroup);
				}
			});
			em.flush();
			return null;
		});

		sessions.stream().forEach(g -> {
			this.logger.debug("group {} weighIn {} competition {}", g.getGroup(), g.getWeighinTime(),
			        g.getCompetitionTime());
		});
	}

	/**
	 * @see app.owlcms.spreadsheet.NRegistrationFileProcessor#appendErrors(java.lang.Runnable, java.util.function.Consumer, net.sf.jxls.reader.XLSReadStatus)
	 */
	private void appendErrors(Runnable displayUpdater, Consumer<String> errorAppender) {
		displayUpdater.run();
	}

	private void appendErrors(Runnable updater, Consumer<String> appender, XLSReadStatus status) {
		@SuppressWarnings("unchecked")
		List<XLSReadMessage> errors = status.getReadMessages();
		for (XLSReadMessage m : errors) {
			String cleanMessage = cleanMessage(m.getMessage());
			appender.accept(cleanMessage);
			Exception e = m.getException();
			if (e != null) {
				Throwable cause = e.getCause();
				String causeMessage = cause != null ? cause.getLocalizedMessage() : e.getLocalizedMessage();
				// causeMessage = LoggerUtils.stackTrace(cause);
				causeMessage = causeMessage != null ? causeMessage : e.toString();
				if (causeMessage.contentEquals("text")) {
					causeMessage = "Empty or invalid.";
				}
				appender.accept(causeMessage);
				this.logger.debug(cleanMessage + causeMessage);
			}
			appender.accept(System.lineSeparator());
		}
		updater.run();
	}

	private String cellToString(Cell cell) {
		switch (cell.getCellType()) {
			case NUMERIC:
				if (DateUtil.isCellDateFormatted(cell)) {
					this.logger.debug("Date Cell {}", cell.getDateCellValue());
				} else {
					return this.formatter.formatCellValue(cell);
				}
			case FORMULA:
				return this.formatter.formatCellValue(cell, this.formulaEvaluator);
			default:
				return this.formatter.formatCellValue(cell);
		}
	}

	private boolean checkTranslation(String valueRead, String string) {
		String translate = Translator.translate(string);
		String translate2 = Translator.translateExplicitLocale(string, Locale.ENGLISH);
		return valueRead.contentEquals(translate)
		        || valueRead.contentEquals(translate2);
	}

	private boolean checkTranslation(String valueRead, String string, String string2) {
		return valueRead.contentEquals(Translator.translate(string) + " " + Translator.translate(string2))
		        || valueRead.contentEquals(Translator.translateExplicitLocale(string, Locale.ENGLISH) + " "
		                + Translator.translateExplicitLocale(string2, Locale.ENGLISH));
	}

	private void processException(RAthlete a, String s, Cell c, Exception e, Consumer<String> errorConsumer) {
		errorConsumer.accept(c.getAddress() + " " + e.getLocalizedMessage() + System.lineSeparator());
		this.logger.error("{} {} {}", c.getAddress(), s, e.getStackTrace());
		// LoggerUtils.logError(this.logger, e, true);
	}

	private List<RAthlete> readAthletes(Workbook workbook, RCompetition rComp, Consumer<String> errorConsumer, int rowsToSkip) {
		if (isIgnoreAthletes()) {
			return List.of();
		}
		Sheet sheet = workbook.getSheetAt(0);
		Iterator<Row> rowIterator = sheet.rowIterator();
		List<RAthlete> athletes = new LinkedList<>();
		int iRow = 0;

		rows: while (rowIterator.hasNext()) {
			int iColumn = 0;
			Row row = rowIterator.next();
			if (iRow < rowsToSkip) {
				iRow++;
				continue;
			}
			if (iRow == rowsToSkip) {
				// header, create a map from column to the appropriate setter.
				Iterator<Cell> cellIterator = row.cellIterator();
				while (cellIterator.hasNext()) {
					Cell cell = cellIterator.next();
					String cellValue = cell.getStringCellValue();
					String trimmedCellValue = cellValue.trim();

					if (checkTranslation(trimmedCellValue, "Membership")) {
						this.setterForColumn[iColumn] = (a, s, c) -> {
							a.setMembership(s);
						};
					} else if (checkTranslation(trimmedCellValue, "Card.lotNumber")) {
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							a.setLotNumber(s);
						});
					} else if (checkTranslation(trimmedCellValue, "LastName")) {
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							a.setLastName(s);
						});
					} else if (checkTranslation(trimmedCellValue, "FirstName")) {
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							a.setFirstName(s);
						});
					} else if (checkTranslation(trimmedCellValue, "Scoreboard.Team")) {
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							a.setTeam(s);
						});
					} else if (checkTranslation(trimmedCellValue, "Registration.birth")) {
						this.delayedSetterColumns[DelayedSetter.BIRTHDATE.ordinal()] = iColumn;
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							try {
								a.setFullBirthDate(s);
							} catch (Exception e) {
								processException(a, s, c, e, errorConsumer);
							}
						});
					} else if (trimmedCellValue.contentEquals("M/F")) {
						this.delayedSetterColumns[DelayedSetter.GENDER.ordinal()] = iColumn;
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							try {
								if (s != null && s.length() > 0) {
									s = s.substring(0, 1).toUpperCase();
								}
								a.setGender(s);
							} catch (Exception e) {
								processException(a, s, c, new Exception(Translator.translate("Registration.IllegalGender", s)), errorConsumer);
							}
						});
					} else if (checkTranslation(trimmedCellValue, "Card.category")) {
						this.delayedSetterColumns[DelayedSetter.CATEGORY.ordinal()] = iColumn;
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							try {
								a.setCategory(s);
							} catch (Exception e) {
								processException(a, s, c, e, errorConsumer);
							}
						});
					} else if (checkTranslation(trimmedCellValue, "Scoreboard.BodyWeight")) {
						this.delayedSetterColumns[DelayedSetter.BODYWEIGHT.ordinal()] = iColumn;
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							try {
								if (s == null || s.isBlank()) {
									return;
								}
								double d = Double.parseDouble(s);
								a.setBodyWeight(d);
							} catch (Exception e) {
								processException(a, s, c, e, errorConsumer);
							}
						});
					} else if (checkTranslation(trimmedCellValue, "Results.Snatch", "Results.Declaration_abbrev")) {
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							try {
								a.setSnatch1Declaration(s);
							} catch (Exception e) {
								processException(a, s, c, new Exception(Translator.translate("Registration.IllegalInteger", s)), errorConsumer);
							}
						});
					} else if (checkTranslation(trimmedCellValue, "Results.CJ_abbrev", "Results.Declaration_abbrev")) {
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							try {
								a.setCleanJerk1Declaration(s);
							} catch (Exception e) {
								processException(a, s, c, new Exception(Translator.translate("Registration.IllegalInteger", s)), errorConsumer);
							}
						});
					} else if (checkTranslation(trimmedCellValue, "Group")) {
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							try {
								a.setGroup(s);
							} catch (Exception e) {
								if (isCreateMissingSessions()) {
									Group g = GroupRepository.add(new Group(s));
									rComp.addGroup(g);
									try {
										a.setGroup(s);
									} catch (Exception e1) {
										processException(a, s, c, e, errorConsumer);
									}
								} else {
									processException(a, s, c, e, errorConsumer);
								}
							}
						});
					} else if (checkTranslation(trimmedCellValue, "Card.entryTotal")) {
						this.delayedSetterColumns[DelayedSetter.QUALIFYING_TOTAL.ordinal()] = iColumn;
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							try {
								if (s != null && !s.isBlank()) {
									int i = Integer.parseInt(s);
									a.setQualifyingTotal(i);
								}
							} catch (Exception e) {
								processException(a, s, c, new Exception(Translator.translate("Registration.IllegalInteger", s)), errorConsumer);
							}
						});
					} else if (checkTranslation(trimmedCellValue, "Coach")) {
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							a.setCoach(s);
						});
					} else if (checkTranslation(trimmedCellValue, "Custom1.Title")) {
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							a.setCustom1(s);
						});
					} else if (checkTranslation(trimmedCellValue, "Custom2.Title")) {
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							a.setCustom2(s);
						});
					} else if (checkTranslation(trimmedCellValue, "Registration.FederationCodesShort")) {
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							a.setFederationCodes(s);
						});
					} else if (checkTranslation(trimmedCellValue, "PersonalBestSnatch")) {
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							try {
								a.setPersonalBestSnatch(s);
							} catch (Exception e) {
								processException(a, s, c, new Exception(Translator.translate("Registration.IllegalInteger", s)), errorConsumer);
							}
						});
					} else if (checkTranslation(trimmedCellValue, "PersonalBestCleanJerk")) {
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							try {
								a.setPersonalBestCleanJerk(s);
							} catch (Exception e) {
								processException(a, s, c, new Exception(Translator.translate("Registration.IllegalInteger", s)), errorConsumer);
							}
						});
					} else if (checkTranslation(trimmedCellValue, "PersonalBestTotal")) {
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							try {
								a.setPersonalBestTotal(s);
							} catch (Exception e) {
								processException(a, s, c, new Exception(Translator.translate("Registration.IllegalInteger", s)), errorConsumer);
							}
						});
					} else if (checkTranslation(trimmedCellValue, "SubCategory")) {
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							a.setSubCategory(s);
						});
					} else if (checkTranslation(trimmedCellValue, "ComputedWeightClass")) {
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							// do nothing
						});
					} else if (checkTranslation(trimmedCellValue, "Competition.Invited/Extra")) {
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							a.setInvited(s != null && s.trim().toLowerCase().equals("true"));
						});
					} else {
						errorConsumer
						        .accept(Translator.translate("Registration.UnknownColumnHeader", trimmedCellValue) + " "
						                + trimmedCellValue);
					}
					iColumn++;
				}
			} else {
				// process the values
				RAthlete ra = new RAthlete();
				Iterator<Cell> cellIterator = row.cellIterator();

				// first pass, memorize cell values for setters that need to be called in a specific order
				// setters that can be called immediately are invoked in this pass
				String[] delayedSetterValues = new String[DelayedSetter.values().length];
				Cell[] delayedSetterCells = new Cell[DelayedSetter.values().length];

				boolean curRowEmpty = true;
				while (cellIterator.hasNext()) {
					Cell cell = cellIterator.next();
					String cellValue = cellToString(cell);
					String trim = cellValue.trim();
					if (trim.isBlank()) {
						continue;
					}

					iColumn = cell.getColumnIndex();
					curRowEmpty = false;
					int delayedOrder = ArrayUtils.indexOf(this.delayedSetterColumns, iColumn);
					if (delayedOrder < 0) {
						if (iColumn < this.setterForColumn.length && this.setterForColumn[iColumn] != null
						        && cell != null) {
							this.logger.debug("setting column {} {}", iColumn, cell.getAddress());
							this.setterForColumn[iColumn].accept(ra, cellValue.trim(), cell);
						}
					} else {
						delayedSetterValues[delayedOrder] = cellValue.trim();
						delayedSetterCells[delayedOrder] = cell;
					}
				}
				if (curRowEmpty) {
					break rows;
				}

				// second pass, call the delayed setters in the correct order.
				for (int delayedOrder = 0; delayedOrder < DelayedSetter.values().length; delayedOrder++) {
					Integer setterColumn = this.delayedSetterColumns[delayedOrder];
					this.logger.debug("delayed setter [{}] {} {}", delayedOrder, DelayedSetter.values()[delayedOrder],
					        setterColumn);
					if (setterColumn != null && delayedSetterCells[delayedOrder] != null) {
						this.setterForColumn[setterColumn].accept(ra, delayedSetterValues[delayedOrder],
						        delayedSetterCells[delayedOrder]);
					}
				}
				String lastName = ra.getAthlete().getLastName();
				String firstName = ra.getAthlete().getFirstName();
				if (lastName != null && firstName != null && !lastName.isBlank() && !firstName.isBlank()) {
					athletes.add(ra);
				}
			}

			iRow++;
		}
		return athletes;
	}

	private boolean isIgnoreAthletes() {
		return athleteOptions == NRegistrationFileProcessor.AthleteOptions.IGNORE_ATHLETES;
	}

	public void doProcessCompetitionHeader(InputStream inputStream, Consumer<String> errorConsumer, Runnable displayUpdater) {
		try (InputStream xlsInputStream = inputStream) {
			inputStream.reset();

			try (Workbook workbook = WorkbookFactory.create(xlsInputStream)) {
				this.formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
				this.formatter = new DataFormatter();

				Sheet sheet = workbook.getSheetAt(0);
				Competition competition = Competition.getCurrent();
				Row row;

				row = sheet.getRow(1 - 1);
				setCompetitionString(competition::setFederation, row.getCell('A' - 'A')); // A1
				setCompetitionString(competition::setCompetitionName, row.getCell('F' - 'A')); // F1
				setCompetitionDate(competition::setCompetitionDate, row.getCell('M' - 'A')); // M1
				setCompetitionDate(competition::setCompetitionEndDate, row.getCell('N' - 'A')); // N1

				row = sheet.getRow(2 - 1);
				setCompetitionString(competition::setFederationAddress, row.getCell('A' - 'A')); // A2
				setCompetitionString(competition::setCompetitionCity, row.getCell('F' - 'A')); // F2

				row = sheet.getRow(3 - 1);
				setCompetitionString(competition::setFederationWebSite, row.getCell('A' - 'A')); // A3
				setCompetitionString(competition::setCompetitionSite, row.getCell('F' - 'A')); // F3

				row = sheet.getRow(4 - 1);
				setCompetitionString(competition::setFederationEMail, row.getCell('A' - 'A')); // A4
				setCompetitionString(competition::setCompetitionOrganizer, row.getCell('F' - 'A')); // F4
			} catch (IOException | EncryptedDocumentException e) {
				errorConsumer.accept(e.getLocalizedMessage());
				LoggerUtils.logError(this.logger, e);
			}

		} catch (IOException e) {
			LoggerUtils.stackTrace(e);
			LoggerUtils.logError(this.logger, e);
		}
	}

	private void setCompetitionDate(Consumer<LocalDate> setter, Cell cell) {
		LocalDate ld = null;
		if (cell.getCellType() == CellType.NUMERIC) {
			ld = cell.getLocalDateTimeCellValue().toLocalDate();
		} else if (cell.getCellType() == CellType.STRING) {
			try {
				ld = DateTimeUtils.parseExcelDate(cell.getStringCellValue(), OwlcmsSession.getLocale());
			} catch (Exception e) {
			}
		}
		if (ld != null) {
			setter.accept(ld);
		}
	}

	private void setCompetitionString(Consumer<String> setter, Cell cell) {
		String stringCellValue = cell.getStringCellValue();
		if (stringCellValue != null) {
			setter.accept(stringCellValue.trim());
		}
	}

	private boolean isUpdateExistingAthletes() {
		return this.getAthleteOptions() == AthleteOptions.UPDATE_ADD_ATHLETES;
	}

	public AthleteOptions getAthleteOptions() {
		return athleteOptions;
	}

	public void setAthleteOptions(AthleteOptions athleteOptions) {
		this.athleteOptions = athleteOptions;
	}

	public SessionOptions getSessionOptions() {
		return sessionOptions;
	}

	public void setSessionOptions(SessionOptions sessionOptions) {
		this.sessionOptions = sessionOptions;
	}

	public boolean isIgnoreSessions() {
		return sessionOptions == SessionOptions.IGNORE_SESSIONS;
	}

	public boolean isDeleteSessions() {
		return sessionOptions == SessionOptions.DELETE_SESSIONS;
	}

}