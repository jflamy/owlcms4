package app.owlcms.spreadsheet;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
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
import net.sf.jxls.reader.ReaderBuilder;
import net.sf.jxls.reader.ReaderConfig;
import net.sf.jxls.reader.XLSReadMessage;
import net.sf.jxls.reader.XLSReadStatus;
import net.sf.jxls.reader.XLSReader;

public class NRegistrationFileProcessor implements IRegistrationFileProcessor {

	record AthleteInput(List<RAthlete> athletes) {
	}

	/* some setters must be called in a specific order; */
	private enum DelayedSetter {
		BIRTHDATE, BODYWEIGHT, QUALIFYING_TOTAL, GENDER, CATEGORY
	}

	static final String GROUPS_READER_SPEC = "/templates/registration/GroupsReader.xml";
	Integer[] delayedSetterColumns = new Integer[DelayedSetter.values().length];
	Logger logger = (Logger) LoggerFactory.getLogger(NRegistrationFileProcessor.class);
	public boolean keepParticipations;
	@SuppressWarnings("unchecked")
	TriConsumer<RAthlete, String, Cell>[] setterForColumn = new TriConsumer[25];
	FormulaEvaluator formulaEvaluator;
	DataFormatter formatter;
	private boolean createMissingGroups = true;

	public NRegistrationFileProcessor() {
	}

	/**
	 * @see app.owlcms.spreadsheet.IRegistrationFileProcessor#adjustParticipations()
	 */
	@Override
	public void adjustParticipations() {
		if (!this.keepParticipations) {
			AthleteRepository.resetParticipations();
		}
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
	 * @see app.owlcms.spreadsheet.IRegistrationFileProcessor#doProcessAthletes(java.io.InputStream, boolean, java.util.function.Consumer, java.lang.Runnable,
	 *      boolean)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public int doProcessAthletes(InputStream inputStream, boolean dryRun, Consumer<String> errorConsumer,
	        Runnable displayUpdater, boolean resetAthletes) {
		try (InputStream xlsInputStream = inputStream) {
			inputStream.reset();
			RCompetition c = new RCompetition();
			RCompetition.resetActiveCategories();
			RCompetition.resetActiveGroups();
			if (resetAthletes) {
				RCompetition.resetAthleteToEligibles();
				RCompetition.resetAthleteToTeams();
			}

			List<RAthlete> athletes = new ArrayList<>();
			AthleteInput athleteInput;
			try (Workbook workbook = WorkbookFactory.create(xlsInputStream)) {
				this.formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
				this.formatter = new DataFormatter();
				athleteInput = readAthletes(workbook, c, errorConsumer);
			} catch (IOException | EncryptedDocumentException e) {
				errorConsumer.accept(e.getLocalizedMessage());
				LoggerUtils.logError(this.logger, e);
				return 0;
			}

			// get back the updated athletes
			athletes = athleteInput.athletes;
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
				appendErrors(displayUpdater, errorConsumer);
			} else {
				errorConsumer.accept(Translator.translate("NoAthletes"));
				displayUpdater.run();
			}
			return athletes.size();
		} catch (IOException e) {
			LoggerUtils.stackTrace(e);
			LoggerUtils.logError(this.logger, e);
		}
		return 0;
	}

	/**
	 * @see app.owlcms.spreadsheet.IRegistrationFileProcessor#doProcessGroups(java.io.InputStream, boolean, java.util.function.Consumer, java.lang.Runnable)
	 */
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

	public boolean isCreateMissingGroups() {
		return this.createMissingGroups;
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

	public void setCreateMissingGroups(boolean createMissingGroups) {
		this.createMissingGroups = createMissingGroups;
	}

	/**
	 * @see app.owlcms.spreadsheet.IRegistrationFileProcessor#updateAthletes(java.util.function.Consumer, app.owlcms.spreadsheet.RCompetition, java.util.List)
	 */
	@Override
	public void updateAthletes(Consumer<String> errorConsumer, RCompetition c, List<RAthlete> athletes) {
		JPAService.runInTransaction(em -> {
			// Competition curC = Competition.getCurrent();
			try {
				// Create the new athletes.
				athletes.stream().forEach(r -> {
					Athlete athlete = r.getAthlete();
					athlete.setCategoryFinished(false);
					// logger.debug("merging {}", athlete.getShortName());
					em.merge(athlete);
				});
				em.flush();
			} catch (Exception e) {
				LoggerUtils.stackTrace(e);
				errorConsumer.accept(e.toString());
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
					a2.setCategoryFinished(false);
					// logger.debug("setting eligibility {} {}", a2.getShortName(), eligibles);
					a2.setEligibleCategories(eligibles);
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
				readGroup.setPlatform(op);
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

	/**
	 * @see app.owlcms.spreadsheet.IRegistrationFileProcessor#appendErrors(java.lang.Runnable, java.util.function.Consumer, net.sf.jxls.reader.XLSReadStatus)
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

	private void processException(RAthlete a, String s, Cell c, Exception e, Consumer<String> errorConsumer) {
		errorConsumer.accept(c.getAddress() + " " + e.getLocalizedMessage() + System.lineSeparator());
		logger.error("{} {} {}", c.getAddress(), s, e.getMessage());
		// LoggerUtils.logError(this.logger, e, true);
	}

	private AthleteInput readAthletes(Workbook workbook, RCompetition rComp, Consumer<String> errorConsumer) {
		Sheet sheet = workbook.getSheetAt(0);
		Iterator<Row> rowIterator = sheet.rowIterator();
		List<RAthlete> athletes = new LinkedList<>();
		int iRow = 0;

		rows: while (rowIterator.hasNext()) {
			int iColumn = 0;
			Row row = rowIterator.next();
			if (iRow == 0) {
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
								processException(a, s, c, new Exception(Translator.translate("Registration.IllegalGender",s)), errorConsumer);
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
								if (isCreateMissingGroups()) {
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
				athletes.add(ra);
			}

			iRow++;
		}
		return new AthleteInput(athletes);
	}

	private boolean checkTranslation(String valueRead, String string, String string2) {
		return valueRead.contentEquals(Translator.translate(string) + " " + Translator.translate(string2))
		        || valueRead.contentEquals(Translator.translateExplicitLocale(string, Locale.ENGLISH) + " "
		                + Translator.translateExplicitLocale(string2, Locale.ENGLISH));
	}

	private boolean checkTranslation(String valueRead, String string) {
		String translate = Translator.translate(string);
		String translate2 = Translator.translateExplicitLocale(string, Locale.ENGLISH);
		return valueRead.contentEquals(translate)
		        || valueRead.contentEquals(translate2);
	}

}