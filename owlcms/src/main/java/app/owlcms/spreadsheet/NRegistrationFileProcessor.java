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
import java.text.MessageFormat;
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
import ch.qos.logback.classic.Logger;

public class NRegistrationFileProcessor {

	/* some setters must be called in a specific order; */
	private enum DelayedSetter {
		BIRTHDATE, BODYWEIGHT, QUALIFYING_TOTAL, GENDER, CATEGORY
	}

	public enum SessionOptions {
		IGNORE_SESSIONS, DELETE_SESSIONS, UPDATE_ADD_SESSIONS
	}

	public enum AthleteOptions {
		IGNORE_ATHLETES, DELETE_ATHLETES, ADD_ATHLETES, UPDATE_ADD_ATHLETES
	}

	static final String GROUPS_READER_SPEC = "/templates/registration/GroupsReader.xml";
	Integer[] delayedSetterColumns = new Integer[DelayedSetter.values().length];
	Logger logger = (Logger) LoggerFactory.getLogger(NRegistrationFileProcessor.class);
	// private boolean keepParticipations;
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
		// if (!this.keepParticipations) {
		// AthleteRepository.resetParticipations(false, true);
		// }
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

			String dataProcessedTemplate = Translator.translate("Upload.DataProcessed.Athletes");
			String dataProcessedMsg = MessageFormat.format(dataProcessedTemplate, athletes.size());
			this.logger.info(dataProcessedMsg);
			// also surface in the UI via the supplied consumer/updater
			if (errorConsumer != null) {
				errorConsumer.accept(dataProcessedMsg + "\n");
			}
			if (displayUpdater != null) {
				displayUpdater.run();
			}
			if (dryRun) {
				return athletes.size();
			}

			if (athletes.size() > 0) {
				updateAthletes(errorConsumer, c, athletes);
				appendErrors(displayUpdater, errorConsumer);
			} else {
				// If the caller asked to ignore athletes, don't report this as an error
				if (!isIgnoreAthletes()) {
					errorConsumer.accept(Translator.translate("NoAthletes"));
				}
				displayUpdater.run();
			}
			return athletes.size();
		} catch (Exception e) {
			LoggerUtils.logError(this.logger, e);
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
	Map<String, Athlete> priorAthletes = new HashMap<>();

	private void updateAthletes(Consumer<String> errorConsumer, RCompetition c, List<RAthlete> sbdeAthletes) {
		// logger.debug(") step 1 - copy away participations");
		JPAService.runInTransaction(em -> {
			// retrieve existing ids
			AthleteRepository.doFindAll(em).stream()
			        .forEach(a -> {
				        String athleteKey = athleteKey(a);
				        priorAthletes.put(athleteKey, a);

				        if (!isOnlyAddAthletes()) {
					        // copy the participation categories away
					        RCompetition.putEligibles(a.getId(), new LinkedHashSet<>(a.getEligibleCategories()));
					        RCompetition.putTeams(a.getId(), a.computeTeams());
					        a.getParticipations().clear();

					        if (isDeleteSessions()) {
						        // reset the group that was cleared.
						        String sessionCode = RCompetition.getSessionCode(a.getId());
						        // logger.debug("++++++ prior session code for {} = {}", a.getFullId(), sessionCode);
						        a.setGroup(RCompetition.activeGroups.get(sessionCode));
						        // logger.debug("++++++ new session for {} = {}", a.getFullId(), a.getGroup());
					        }
				        } else {
					        // logger.debug("skipping prior {}",a.getAbbreviatedName());
				        }

				        em.merge(a);
				        em.flush();
			        });
			return null;
		});

		List<Athlete> toBeMerged = new ArrayList<>(priorAthletes.size());
		// Create the new athletes.
		sbdeAthletes.stream().forEach(r -> {
			Athlete sbdeAthlete = r.getAthlete();
			String athleteKey = athleteKey(sbdeAthlete);

			Athlete existingAthlete = priorAthletes.get(athleteKey(sbdeAthlete));
			if (existingAthlete != null) {
				if (isUpdateExistingAthletes() || isDeleteAthletes()) {
					existingAthlete.getParticipations().clear();
					// logger.debug("* existing athlete {} {} {}", existingAthlete.getAbbreviatedName(),
					// existingAthlete.getId(),existingAthlete.getParticipations());
					// logger.debug("* sbde {} {}", sbdeAthlete.getAbbreviatedName(), sbdeAthlete.getId(), sbdeAthlete.getParticipations());

					// can't happen, mutually exclusive from enclosing conditions, paranoia.
					if (!isOnlyAddAthletes()) {
						updateExistingAthlete(existingAthlete, sbdeAthlete);
						toBeMerged.add(existingAthlete);
					}
				} else {
					errorConsumer.accept("Existing athlete ignored: " + athleteKey);
					logger.error("Existing Athlete Entry {} {}", athleteKey, existingAthlete.getId());
				}
			} else {
				sbdeAthlete.setCategoryFinished(false);
				// logger.debug("adding sbdeAthlete {} {}", sbdeAthlete.getShortName(), sbdeAthlete.getId());
				toBeMerged.add(sbdeAthlete);
			}
		});
		// logger.debug("( end step 1");

		// logger.debug(") step 2 - updating participations and teams");
		JPAService.runInTransaction(em -> {
			try {
				for (Athlete a : toBeMerged) {
					// logger.debug("merging {} {}", a.getAbbreviatedName(), a.getId());
					em.merge(a);
				}
				em.flush();
			} catch (Exception e) {
				LoggerUtils.stackTrace(e);
				errorConsumer.accept(e.toString());
			}
			return null;
		});
		// logger.debug("( end step 2");

		JPAService.runInTransaction(em -> {
			// logger.debug(") step 3 - database athletes");
			AthleteRepository.findAll().stream().forEach(a2 -> {
				if (isPriorAthlete(a2)) {
					// logger.debug("skipping prior athlete {}",a2.getAbbreviatedName());
					return;
				}
				LinkedHashSet<Category> eligibles = RCompetition
				        .getAthleteToEligibles()
				        .get(a2.getId());
				LinkedHashSet<Category> teams = RCompetition
				        .getAthleteToTeams()
				        .get(a2.getId());
				if (teams == null) {
					// logger.debug("no teams for athlete {}", a2.getFullId());
					teams = new LinkedHashSet<Category>();
				}
				// logger.debug("athlete {} eligibles {}", a2.getId(), eligibles);
				if (eligibles != null) {
					Category first = eligibles.stream().findFirst().orElse(null);
					a2.setCategory(first);
					a2.setCategoryFinished(false);

					if (!a2.getEligibleCategories().isEmpty()) {
						logger.error("eligibility already set for {}", a2.getShortName());
					} else {
						// logger.debug("setting eligibility {} {}", a2.getShortName(), eligibles);
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
					// logger.debug("participations {} {}", a2.getShortName(), a2.getParticipations());
					em.merge(a2);
				}
			});
			em.flush();
			// logger.debug(") end step 3");
			return null;
		});
	}

	private boolean isPriorAthlete(Athlete a2) {
		return priorAthletes.get(athleteKey(a2)) != null;
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
		// System.err./**/println("> updateExistingAthlete");
		Athlete.conditionalCopy(existingAthlete, sbdeAthlete, false, false, false);
		RCompetition.putEligibles(existingAthlete.getId(), RCompetition.getEligibles(sbdeAthlete.getId()));
		RCompetition.putTeams(existingAthlete.getId(), RCompetition.getTeams(sbdeAthlete.getId()));
		// System.err./**/println("< updateExistingAthlete");
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
					// logger.debug("updating platorm for {} to {}", existingGroup, existingGroup.getPlatform());
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

	private String cellToString(Cell cell) {
		String raw;
		switch (cell.getCellType()) {
			case NUMERIC:
				if (DateUtil.isCellDateFormatted(cell)) {
					this.logger.debug("Date Cell {}", cell.getDateCellValue());
				}
				raw = this.formatter.formatCellValue(cell);
				break;
			case FORMULA:
				raw = this.formatter.formatCellValue(cell, this.formulaEvaluator);
				break;
			default:
				raw = this.formatter.formatCellValue(cell);
		}
		if (raw == null) {
			return "";
		}
		return raw.trim();
	}

	// Use RGroup as the parsing target — it already wraps a Group and stores string fields
	@FunctionalInterface
	private interface CellSetterRG {
		void set(RGroup g, Cell cell) throws Exception;
	}

	private final Map<String, CellSetterRG> GROUP_SETTER_MAP = buildGroupSetterMap();

	private Map<String, CellSetterRG> buildGroupSetterMap() {
		Map<String, CellSetterRG> base = new HashMap<>();
		base.put("Group", (rg, cell) -> rg.setGroupName(cellToString(cell)));
		base.put("Platform", (rg, cell) -> rg.setPlatform(cellToString(cell)));
		base.put("Group.Description", (rg, cell) -> rg.setDescription(cellToString(cell)));
		base.put("Masters", (rg, cell) -> rg.setMasters(cellToString(cell)));

		base.put("WeighInTime", (rg, cell) -> {
			// Prefer Excel numeric/formula date values so RGroup.parse expects a numeric serial string.
			try {
				if (cell.getCellType() == CellType.NUMERIC) {
					double v = cell.getNumericCellValue();
					rg.setWeighinTime(Double.toString(v));
					return;
				} else if (cell.getCellType() == CellType.FORMULA && this.formulaEvaluator != null) {
					org.apache.poi.ss.usermodel.CellValue cv = this.formulaEvaluator.evaluate(cell);
					if (cv != null && cv.getCellType() == CellType.NUMERIC) {
						rg.setWeighinTime(Double.toString(cv.getNumberValue()));
						return;
					}
				}
			} catch (Exception e) {
				// fall through to string path
			}
			rg.setWeighinTime(cellToString(cell));
		});
		base.put("StartTime", (rg, cell) -> {
			try {
				if (cell.getCellType() == CellType.NUMERIC) {
					double v = cell.getNumericCellValue();
					rg.setCompetitionTime(Double.toString(v));
					return;
				} else if (cell.getCellType() == CellType.FORMULA && this.formulaEvaluator != null) {
					org.apache.poi.ss.usermodel.CellValue cv = this.formulaEvaluator.evaluate(cell);
					if (cv != null && cv.getCellType() == CellType.NUMERIC) {
						rg.setCompetitionTime(Double.toString(cv.getNumberValue()));
						return;
					}
				}
			} catch (Exception e) {
				// fall through
			}
			rg.setCompetitionTime(cellToString(cell));
		});
		base.put("Weighin1", (rg, cell) -> rg.setWeighInTO1(cellToString(cell)));
		base.put("Weighin2", (rg, cell) -> rg.setWeighInTO2(cellToString(cell)));

		base.put("Announcer", (rg, cell) -> rg.setAnnouncer(cellToString(cell)));
		base.put("Marshall", (rg, cell) -> rg.setMarshall(cellToString(cell)));
		base.put("Marshal2", (rg, cell) -> rg.setMarshal2(cellToString(cell)));
		base.put("TimeKeeper", (rg, cell) -> rg.setTimekeeper(cellToString(cell)));
		base.put("TechnicalController", (rg, cell) -> rg.setTechController(cellToString(cell)));
		base.put("TechnicalController2", (rg, cell) -> rg.setTechController2(cellToString(cell)));
		base.put("Referee1", (rg, cell) -> rg.setRef1(cellToString(cell)));
		base.put("Referee2", (rg, cell) -> rg.setRef2(cellToString(cell)));
		base.put("Referee3", (rg, cell) -> rg.setRef3(cellToString(cell)));
		base.put("Reserve", (rg, cell) -> rg.setReserve(cellToString(cell)));

		base.put("Jury1", (rg, cell) -> rg.setJury1(cellToString(cell)));
		base.put("Jury2", (rg, cell) -> rg.setJury2(cellToString(cell)));
		base.put("Jury3", (rg, cell) -> rg.setJury3(cellToString(cell)));
		base.put("Jury4", (rg, cell) -> rg.setJury4(cellToString(cell)));
		base.put("Jury5", (rg, cell) -> rg.setJury5(cellToString(cell)));
		base.put("ReserveJury", (rg, cell) -> rg.setReserveJury(cellToString(cell)));

		base.put("Doctor", (rg, cell) -> rg.getGroup().setDoctor(cellToString(cell)));

		Map<String, CellSetterRG> result = new HashMap<>();
		for (Map.Entry<String, CellSetterRG> e : base.entrySet()) {
			String key = e.getKey();
			CellSetterRG setter = e.getValue();
			// Only register translations as valid header names. Do not register the canonical key itself.
			try {
				// current locale translation
				String tCurrent = Translator.translate(key);
				if (tCurrent != null && !tCurrent.isBlank()) {
					result.putIfAbsent(tCurrent.trim().toLowerCase(), setter);
				}
			} catch (Exception ex) {
				// ignore translation failures
			}
			try {
				// English explicit translation
				String tEng = Translator.translateExplicitLocale(key, Locale.ENGLISH);
				if (tEng != null && !tEng.isBlank()) {
					result.putIfAbsent(tEng.trim().toLowerCase(), setter);
				}
			} catch (Exception ex) {
				// ignore
			}
		}
			// Practical fallback: register a few common canonical English keys directly
			// so spreadsheets that use the legacy canonical names are accepted even if
			// translations are missing. Keys are stored lowercased to match lookup.
			String[] explicitFallbacks = new String[] { "TimeKeeper", "Masters" };
			for (String fk : explicitFallbacks) {
				CellSetterRG s = base.get(fk);
				if (s != null) {
					result.putIfAbsent(fk.trim().toLowerCase(), s);
				}
			}
		return result;
	}

		private CellSetterRG[] createGroupSetterTableFromHeaderRow(Row headerRow, List<String> errors) {
		List<CellSetterRG> setters = new ArrayList<>();
		for (int i = 0; i < headerRow.getLastCellNum(); i++) {
			Cell cell = headerRow.getCell(i);
			if (cell == null) {
				break;
			}
			if (cell.getCellType() == CellType.BLANK || (cell.getCellType() == CellType.STRING && cell.getStringCellValue().isBlank())
			        || (cell.getCellType() != CellType.STRING)) {
				break;
			}
			String headerValue = cell.getStringCellValue().trim().toLowerCase();
			CellSetterRG setter = GROUP_SETTER_MAP.get(headerValue);
			if (setter != null) {
				logger.debug("Mapped group header '{}' to setter", headerValue);
			} else {
				logger.warn("No setter found for group header '{}'", headerValue);
				// append a newline so each error appears on its own line when displayed
				errors.add(MessageFormat.format("Ignoring unknown column ''{0}'' at sheet {1} [{2}]\n",
				        headerValue, cell.getSheet().getSheetName(), cell.getAddress()));
				setter = (rg, c) -> {
					/* noop */ };
			}
			setters.add(setter);
		}
		return setters.toArray(new CellSetterRG[0]);
	}

	private boolean checkTranslation(String valueRead, String string, String string2) {
		return valueRead.contentEquals(Translator.translate(string) + " " + Translator.translate(string2))
		        || valueRead.contentEquals(Translator.translateExplicitLocale(string, Locale.ENGLISH) + " "
		                + Translator.translateExplicitLocale(string2, Locale.ENGLISH));
	}

	/**
	 * Check whether a header cell or header text matches the given canonical key's translation. The canonical key itself is not expected to be in the sheet;
	 * the routine compares the cell text against the current-locale translation and the explicit English translation.
	 */
	private boolean headerMatches(Cell headerCell, String canonicalKey) {
		if (headerCell == null) {
			return false;
		}
		String value = cellToString(headerCell);
		return headerMatches(value, canonicalKey);
	}

	private boolean headerMatches(String valueRead, String canonicalKey) {
		if (valueRead == null) {
			return false;
		}
		String trimmed = valueRead.trim();
		try {
			String tCurrent = Translator.translate(canonicalKey);
			if (tCurrent != null && !tCurrent.isBlank() && trimmed.equalsIgnoreCase(tCurrent.trim())) {
				return true;
			}
		} catch (Exception ex) {
			// ignore translation errors
		}
		try {
			String tEng = Translator.translateExplicitLocale(canonicalKey, Locale.ENGLISH);
			if (tEng != null && !tEng.isBlank() && trimmed.equalsIgnoreCase(tEng.trim())) {
				return true;
			}
		} catch (Exception ex) {
			// ignore
		}
		return false;
	}

	private void processException(RAthlete a, String s, Cell c, Exception e, Consumer<String> errorConsumer) {
		errorConsumer.accept(c.getAddress() + " " + e.getLocalizedMessage() + System.lineSeparator());
		this.logger.error("{} {} {}", c.getAddress(), s, e.getStackTrace());
		// LoggerUtils.logError(this.logger, e, true);
	}

	// ... header row detection removed; SBDE uses fixed header row at index 1 when Session is in A2

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

					if (headerMatches(trimmedCellValue, "Membership")) {
						this.setterForColumn[iColumn] = (a, s, c) -> {
							a.setMembership(s);
						};
					} else if (headerMatches(trimmedCellValue, "Card.lotNumber")) {
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							a.setLotNumber(s);
						});
					} else if (headerMatches(trimmedCellValue, "LastName")) {
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							a.setLastName(s);
						});
					} else if (headerMatches(trimmedCellValue, "FirstName")) {
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							a.setFirstName(s);
						});
					} else if (headerMatches(trimmedCellValue, "Scoreboard.Team")) {
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							a.setTeam(s);
						});
					} else if (headerMatches(trimmedCellValue, "Registration.birth")) {
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
					} else if (headerMatches(trimmedCellValue, "Card.category")) {
						this.delayedSetterColumns[DelayedSetter.CATEGORY.ordinal()] = iColumn;
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							try {
								a.setCategory(s);
							} catch (Exception e) {
								processException(a, s, c, e, errorConsumer);
							}
						});
					} else if (headerMatches(trimmedCellValue, "Scoreboard.BodyWeight")) {
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
					} else if (headerMatches(trimmedCellValue, "Group")) {
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
					} else if (headerMatches(trimmedCellValue, "Card.entryTotal")) {
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
					} else if (headerMatches(trimmedCellValue, "Coach")) {
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							a.setCoach(s);
						});
					} else if (headerMatches(trimmedCellValue, "Custom1.Title")) {
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							a.setCustom1(s);
						});
					} else if (headerMatches(trimmedCellValue, "Custom2.Title")) {
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							a.setCustom2(s);
						});
					} else if (headerMatches(trimmedCellValue, "Registration.FederationCodesShort")) {
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							a.setFederationCodes(s);
						});
					} else if (headerMatches(trimmedCellValue, "PersonalBestSnatch")) {
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							try {
								a.setPersonalBestSnatch(s);
							} catch (Exception e) {
								processException(a, s, c, new Exception(Translator.translate("Registration.IllegalInteger", s)), errorConsumer);
							}
						});
					} else if (headerMatches(trimmedCellValue, "PersonalBestCleanJerk")) {
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							try {
								a.setPersonalBestCleanJerk(s);
							} catch (Exception e) {
								processException(a, s, c, new Exception(Translator.translate("Registration.IllegalInteger", s)), errorConsumer);
							}
						});
					} else if (headerMatches(trimmedCellValue, "PersonalBestTotal")) {
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							try {
								a.setPersonalBestTotal(s);
							} catch (Exception e) {
								processException(a, s, c, new Exception(Translator.translate("Registration.IllegalInteger", s)), errorConsumer);
							}
						});
					} else if (headerMatches(trimmedCellValue, "SubCategory")) {
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							a.setSubCategory(s);
						});
					} else if (headerMatches(trimmedCellValue, "ComputedWeightClass")) {
						this.setterForColumn[iColumn] = ((a, s, c) -> {
							// do nothing
						});
					} else if (headerMatches(trimmedCellValue, "Competition.Invited/Extra")) {
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

	public boolean isDeleteAthletes() {
		return this.getAthleteOptions() == AthleteOptions.DELETE_ATHLETES;
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

	private boolean isOnlyAddAthletes() {
		return this.getAthleteOptions() == AthleteOptions.ADD_ATHLETES;
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

	/**
	 * Read groups (sessions) from the workbook using a header-driven approach.
	 */
	public int doProcessGroups(InputStream inputStream, boolean dryRun, Consumer<String> errorConsumer,
	        Runnable displayUpdater) {
		try (InputStream xlsInputStream = inputStream) {
			inputStream.reset();
			List<RGroup> parsed = new ArrayList<>();
			List<String> errors = new ArrayList<>();

			try (Workbook workbook = WorkbookFactory.create(xlsInputStream)) {
				this.formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
				this.formatter = new DataFormatter();

				// Only process sheets that have the translated key "Session" in cell A2 (row index 1, col 0).
				// If no sheet meets this criterion, nothing will be processed.
				if (isIgnoreAthletes()) {
					this.logger.info("Import configured to ignore athletes; athlete sheets will be skipped.");
				}
				for (Sheet sheet : workbook) {
					Row rCheck = sheet.getRow(1); // A2
					if (rCheck == null) {
						continue;
					}
					Cell cCheck = rCheck.getCell(0);
					if (cCheck == null) {
						this.logger.debug("Sheet '{}' A2 is empty", sheet.getSheetName());
						continue;
					}
					String a2Text = cellToString(cCheck);
					boolean isSessionHeader = headerMatches(cCheck, "Session");
					boolean isGroupHeader = headerMatches(cCheck, "Group");
					String tSessionCur = "";
					String tSessionEng = "";
					String tGroupCur = "";
					String tGroupEng = "";
					try {
						tSessionCur = Translator.translate("Session");
					} catch (Exception ex) {
						// ignore
					}
					try {
						tSessionEng = Translator.translateExplicitLocale("Session", Locale.ENGLISH);
					} catch (Exception ex) {
						// ignore
					}
					try {
						tGroupCur = Translator.translate("Group");
					} catch (Exception ex) {
						// ignore
					}
					try {
						tGroupEng = Translator.translateExplicitLocale("Group", Locale.ENGLISH);
					} catch (Exception ex) {
						// ignore
					}
					// Use warn for temporary diagnostics so they are easy to remove later.
					this.logger.warn(
					        "Sheet '{}' A2='{}'. headerMatches(Session)={} session={} (session current='{}' en='{}'; session key current='{}' en='{}')",
					        sheet.getSheetName(), a2Text, isSessionHeader, isGroupHeader, tSessionCur, tSessionEng, tGroupCur, tGroupEng);
					// Only accept the sheet when the canonical A2 key 'Group' matches.
					if (!isGroupHeader) {
						// not the sessions sheet -> skip
						continue;
					}
					Iterator<Row> rowIter = sheet.rowIterator();
					CellSetterRG[] setterTable = null;
					int headerRowIndex = 0;
					// For sessions sheets, the header row is fixed to index 1 (row 2 human).
					headerRowIndex = 1; // A2 indicates headers start on the next row (row 2 human)

					rows: while (rowIter.hasNext()) {
						Row row = rowIter.next();
						int currentRowNum = row.getRowNum();
						if (currentRowNum < headerRowIndex) {
							continue;
						}
						if (currentRowNum == headerRowIndex) {
							setterTable = createGroupSetterTableFromHeaderRow(row, errors);
							continue;
						}

						RGroup rg = new RGroup();
						boolean rowHasData = false;
						for (Cell cell : row) {
							int iColumn = cell.getAddress().getColumn();
							if (setterTable != null && iColumn < setterTable.length) {
								try {
									setterTable[iColumn].set(rg, cell);
									rowHasData = true;
								} catch (Exception e) {
									String msg = MessageFormat.format("{0}[{1}] {2}\n", sheet.getSheetName(), cell.getAddress(), e.getMessage());
									errors.add(msg);
									logger.error(msg);
								}
							}
						}

						if (!rowHasData || rg.getGroupName() == null || rg.getGroupName().isBlank()) {
							// empty row -> stop processing this sheet
							break rows;
						}

						parsed.add(rg);
					}
				}
			} catch (IOException e) {
				LoggerUtils.logError(this.logger, e);
				return 0;
			}

			String dataReadMsg;
			try {
				if (dryRun) {
					// Dry-run identified count: keep this as plain English for logs/UI-suppressed callers.
					dataReadMsg = MessageFormat.format("{0} sessions identified.", Integer.valueOf(parsed.size()));
				} else {
					String tpl = Translator.translate("Upload.DataProcessed.Sessions");
					dataReadMsg = MessageFormat.format(tpl, Integer.valueOf(parsed.size()));
				}
			} catch (Exception ex) {
				dataReadMsg = Translator.translate("DataRead") + " " + parsed.size() + " sessions.";
			}
			this.logger.info(dataReadMsg);
			if (!dryRun) {
				updatePlatformsAndSessions(parsed);
			}

			// surface a summary message so the UI status shows the number of sessions (dry-run = identified, real = processed)
			errorConsumer.accept(dataReadMsg);
			// surface errors
			for (String e : errors) {
				errorConsumer.accept(e);
			}
			displayUpdater.run();
			return parsed.size();
		} catch (Exception e) {
			LoggerUtils.logError(this.logger, e);
		}
		return 0;
	}

}