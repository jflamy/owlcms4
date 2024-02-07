package app.owlcms.spreadsheet;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.LoggerFactory;

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
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import ch.qos.logback.classic.Logger;
import net.sf.jxls.reader.ReaderConfig;

public class NRegistrationFileProcessor implements IRegistrationFileProcessor {
	static final String GROUPS_READER_SPEC = "/templates/registration/GroupsReader.xml";
	static final String REGISTRATION_READER_SPEC = "/templates/registration/RegistrationReader.xml";
	Logger logger = (Logger) LoggerFactory.getLogger(NRegistrationFileProcessor.class);
	public boolean keepParticipations;
	@SuppressWarnings("unchecked")
	TriConsumer<RAthlete, String, Cell>[] setters = new TriConsumer[25];

	public NRegistrationFileProcessor() {
	}

	/**
	 * @see app.owlcms.spreadsheet.IRegistrationFileProcessor#doProcessAthletes(java.io.InputStream, boolean,
	 *      java.util.function.Consumer, java.lang.Runnable)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public int doProcessAthletes(InputStream inputStream, boolean dryRun, Consumer<String> errorConsumer,
	        Runnable displayUpdater) {

		try (InputStream xlsInputStream = inputStream) {
			inputStream.reset();
			RCompetition c = new RCompetition();
			RCompetition.resetActiveCategories();
			RCompetition.resetActiveGroups();
			RCompetition.resetAthleteToEligibles();
			RCompetition.resetAthleteToTeams();

			List<RAthlete> athletes = new ArrayList<>();

			Map<String, Object> beans = new HashMap<>();
			beans.put("competition", c);
			beans.put("athletes", athletes);

			List<String> status = new ArrayList<>();
			try (Workbook workbook = WorkbookFactory.create(xlsInputStream)) {
				status = readAthletes(workbook);
			} catch (Exception e) {
				logger.error("could not process registration data. See logs for details");
			}

			// get back the updated athletes
			athletes = (List<RAthlete>) beans.get("athletes");
			// if exact matches were found for categories, the processing for eligibility
			// has been done, and we keep the eligibilities exactly as in the file.
			keepParticipations = athletes.stream()
			        .filter(r -> r.getAthlete().getEligibleCategories() != null).findFirst()
			        .isPresent();

			logger.info(Translator.translate("DataRead") + " " + athletes.size() + " athletes");
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
			return athletes.size();
		} catch (IOException e) {
			LoggerUtils.stackTrace(e);
			LoggerUtils.logError(logger, e);
		}
		return 0;
	}

	private List<String> readAthletes(Workbook workbook) {
		Sheet sheet = workbook.getSheetAt(1);
		Iterator<Row> rowIterator = sheet.rowIterator();
		int iRow = 0;
		rows: while (rowIterator.hasNext()) {
			int iColumn = 0;
			Row row = rowIterator.next();
			if (iRow == 0) {
				// header, create map.
				Iterator<Cell> cellIterator = row.cellIterator();
				while (cellIterator.hasNext()) {
					Cell cell = cellIterator.next();
					String cellValue = cell.getStringCellValue();
					String trim = cellValue.trim();

					if (trim.contentEquals(Translator.translate("Membership"))) {
						setters[iColumn] = (a, s, c) -> {
							a.setMembership(s);
						};
					} else if (trim.contentEquals(Translator.translate("Card.lotNumber"))) {
						setters[iColumn] = ((a, s, c) -> {
							a.setLotNumber(s);
						});
					} else if (trim.contentEquals(Translator.translate("LastName"))) {
						setters[iColumn] = ((a, s, c) -> {
							a.setLastName(s);
						});
					} else if (trim.contentEquals(Translator.translate("FirstName"))) {
						setters[iColumn] = ((a, s, c) -> {
							a.setFirstName(s);
						});
					} else if (trim.contentEquals(Translator.translate("Scoreboard.Team"))) {
						setters[iColumn] = ((a, s, c) -> {
							a.setTeam(s);
						});
					} else if (trim.contentEquals(Translator.translate("Registration.birth"))) {
						setters[iColumn] = ((a, s, c) -> {
							try {
								a.setFullBirthDate(s);
							} catch (Exception e) {
								processException(a, s, c, e);
							}
						});
					} else if (trim.contentEquals("M/F")) {
						setters[iColumn] = ((a, s, c) -> {
							a.setGender(s);
						});
					} else if (trim.contentEquals(Translator.translate("Card.category"))) {
						setters[iColumn] = ((a, s, c) -> {
							try {
								a.setCategory(s);
							} catch (Exception e) {
								processException(a, s, c, e);
							}
						});
					} else if (trim.contentEquals(Translator.translate("Results.Snatch") + " "
					        + Translator.translate("Results.Declaration_abbrev"))) {
						setters[iColumn] = ((a, s, c) -> {
							a.setSnatch1Declaration(s);
						});
					} else if (trim.contentEquals(Translator.translate("Results.CJ_abbrev") + " "
					        + Translator.translate("Results.Declaration_abbrev"))) {
						setters[iColumn] = ((a, s, c) -> {
							a.setCleanJerk1Declaration(s);
						});
					} else if (trim.contentEquals(Translator.translate("Group"))) {
						setters[iColumn] = ((a, s, c) -> {
							try {
								a.setGroup(s);
							} catch (Exception e) {
								processException(a, s, c, e);
							}
						});
					} else if (trim.contentEquals(Translator.translate("Card.entryTotal"))) {
						setters[iColumn] = ((a, s, c) -> {
							try {
								int i = Integer.parseInt(s);
								a.setQualifyingTotal(i);
							} catch (Exception e) {
								processException(a, s, c, e);
							}
						});
					} else if (trim.contentEquals(Translator.translate("Coach"))) {
						setters[iColumn] = ((a, s, c) -> {
							a.setCoach(s);
						});
					} else if (trim.contentEquals(Translator.translate("Custom1.Title"))) {
						setters[iColumn] = ((a, s, c) -> {
							a.setCustom1(s);
						});
					} else if (trim.contentEquals(Translator.translate("Custom2.Title"))) {
						setters[iColumn] = ((a, s, c) -> {
							a.setCustom2(s);
						});
					} else if (trim.contentEquals(Translator.translate("Registration.FederationCodesShort"))) {
						setters[iColumn] = ((a, s, c) -> {
							a.setFederationCodes(s);
						});
					} else if (trim.contentEquals(Translator.translate("PersonalBestSnatch"))) {
						setters[iColumn] = ((a, s, c) -> {
							a.setPersonalBestSnatch(s);
						});
					} else if (trim.contentEquals(Translator.translate("PersonalBestCleanJerk"))) {
						setters[iColumn] = ((a, s, c) -> {
							a.setPersonalBestCleanJerk(s);
						});
					} else if (trim.contentEquals(Translator.translate("PersonalBestTotal"))) {
						setters[iColumn] = ((a, s, c) -> {
							a.setPersonalBestTotal(s);
						});
					} else if (trim.contentEquals(Translator.translate("SubCategory"))) {
						setters[iColumn] = ((a, s, c) -> {
							a.setSubCategory(s);
						});
					}
					iColumn++;
				}
			}

			RAthlete ra = new RAthlete();
			Iterator<Cell> cellIterator = row.cellIterator();
			iColumn = 0;
			while (cellIterator.hasNext()) {
				Cell cell = cellIterator.next();
				String cellValue = cell.getStringCellValue();
				if (iColumn == 0) {
					String trim = cellValue.trim();
					if (trim.isBlank()) {
						break rows;
					}
				} else {
					// set the value inside the ra object
					setters[iColumn].accept(ra, cell.getStringCellValue(), cell);
				}
				iColumn++;
			}
		}
		// FIXME: accumulate error messages
		return new ArrayList<String>();
	}

	private void processException(RAthlete a, String s, Cell c, Exception e) {
		// TODO Auto-generated method stub
	}

	/**
	 * @see app.owlcms.spreadsheet.IRegistrationFileProcessor#doProcessGroups(java.io.InputStream, boolean,
	 *      java.util.function.Consumer, java.lang.Runnable)
	 */
	@Override
	public int doProcessGroups(InputStream inputStream, boolean dryRun, Consumer<String> errorConsumer,
	        Runnable displayUpdater) {
		try (InputStream xmlInputStream = ResourceWalker.getResourceAsStream(GROUPS_READER_SPEC)) {
			inputStream.reset();
			ReaderConfig readerConfig = ReaderConfig.getInstance();
			readerConfig.setUseDefaultValuesForPrimitiveTypes(true);
			readerConfig.setSkipErrors(true);

			try (InputStream xlsInputStream = inputStream) {
				List<RGroup> groups = new ArrayList<>();

				Map<String, Object> beans = new HashMap<>();
				beans.put("groups", groups);

				// logger.info(getTranslation("ReadingData_"));
				logger.info("Read {} groups.", groups.size());
				if (!dryRun) {
					updatePlatformsAndGroups(groups);
				}

				// FIXME real errors
				appendErrors(displayUpdater, errorConsumer, new ArrayList<String>());
				return groups.size();
			} catch (IOException e) {
				LoggerUtils.logError(logger, e);
			}
		} catch (IOException e1) {
			LoggerUtils.logError(logger, e1);
		}
		return 0;
	}

	/**
	 * @see app.owlcms.spreadsheet.IRegistrationFileProcessor#appendErrors(java.lang.Runnable,
	 *      java.util.function.Consumer, net.sf.jxls.reader.XLSReadStatus)
	 */
	private void appendErrors(Runnable displayUpdater, Consumer<String> errorAppender, List<String> status) {
		// FIXME show the errors
		// List<XLSReadMessage> errors = status.getReadMessages();
		// for (XLSReadMessage m : errors) {
		// String cleanMessage = cleanMessage(m.getMessage());
		// errorAppender.accept(cleanMessage);
		// Exception e = m.getException();
		// if (e != null) {
		// Throwable cause = e.getCause();
		// String causeMessage = cause != null ? cause.getLocalizedMessage() : e.getLocalizedMessage();
		// // causeMessage = LoggerUtils.stackTrace(cause);
		// causeMessage = causeMessage != null ? causeMessage : e.toString();
		// if (causeMessage.contentEquals("text")) {
		// causeMessage = "Empty or invalid.";
		// }
		// errorAppender.accept(causeMessage);
		// logger.debug(cleanMessage + causeMessage);
		// }
		// errorAppender.accept(System.lineSeparator());
		// }
		displayUpdater.run();
	}

	/**
	 * @see app.owlcms.spreadsheet.IRegistrationFileProcessor#updateAthletes(java.util.function.Consumer,
	 *      app.owlcms.spreadsheet.RCompetition, java.util.List)
	 */
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
							logger.info("Excluding {} as team member for {}", a2.getShortName(),
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

	/**
	 * @see app.owlcms.spreadsheet.IRegistrationFileProcessor#updatePlatformsAndGroups(java.util.List)
	 */
	@Override
	public void updatePlatformsAndGroups(List<RGroup> groups) {
		Set<String> futurePlatforms = groups.stream().map(RGroup::getPlatform).filter(p -> (p != null && !p.isBlank()))
		        .collect(Collectors.toSet());

		String defaultPlatformName = OwlcmsFactory.getDefaultFOP().getName();
		if (futurePlatforms.isEmpty()) {
			// keep the current default if no group is linked to a platform.
			futurePlatforms.add(defaultPlatformName);
		}
		logger.debug("to be kept if present: {}", futurePlatforms);

		PlatformRepository.deleteUnusedPlatforms(futurePlatforms);
		PlatformRepository.createMissingPlatforms(groups);

		// recompute the available platforms, unregister the existing FOPs, etc.
		OwlcmsFactory.initDefaultFOP();
		String newDefault = OwlcmsFactory.getDefaultFOP().getName();

		JPAService.runInTransaction(em -> {
			groups.stream().forEach(g -> {
				String platformName = g.getPlatform();
				Group group = g.getGroup();
				if (platformName == null || platformName.isBlank()) {
					platformName = newDefault;
				}
				logger.info("setting platform '{}' for group {}", platformName, g.getGroupName());
				Platform op = PlatformRepository.findByName(platformName);
				group.setPlatform(op);
				em.merge(group);
			});
			em.flush();
			return null;
		});

		groups.stream().forEach(g -> {
			logger.debug("group {} weighIn {} competition {}", g.getGroup(), g.getWeighinTime(),
			        g.getCompetitionTime());
		});
	}

	/**
	 * @see app.owlcms.spreadsheet.IRegistrationFileProcessor#cleanMessage(java.lang.String)
	 */
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

	/**
	 * @see app.owlcms.spreadsheet.IRegistrationFileProcessor#resetAthletes()
	 */
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

	/**
	 * @see app.owlcms.spreadsheet.IRegistrationFileProcessor#resetGroups()
	 */
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
	}

	/**
	 * @see app.owlcms.spreadsheet.IRegistrationFileProcessor#adjustParticipations()
	 */
	@Override
	public void adjustParticipations() {
		if (!keepParticipations) {
			AthleteRepository.resetParticipations();
		}
	}
}