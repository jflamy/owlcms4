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
					this.logger.debug("setting platform '{}' for group {}", platformName, g.getGroupName());
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

	private static class AthleteHeaderInfo {
		/**
		 * Holds the setter to apply for a header and optional delayed-setter slot.
		 *
		 * <p>
		 * If {@code delayed} is non-null it identifies which ordered delayed slot (see {@link DelayedSetter}) this header corresponds to; the column index for
		 * that delayed slot is recorded during header parsing and the actual value is applied in a second pass after immediate setters. If {@code delayed} is
		 * {@code null} the setter is invoked immediately while reading the row.
		 */
		final TriConsumer<RAthlete, String, Cell> setter;
		final DelayedSetter delayed;

		AthleteHeaderInfo(TriConsumer<RAthlete, String, Cell> setter, DelayedSetter delayed) {
			this.setter = setter;
			this.delayed = delayed;
		}
	}

	private final Map<String, AthleteHeaderInfo> ATHLETE_SETTER_MAP = buildAthleteSetterMap();

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

	private Map<String, AthleteHeaderInfo> buildAthleteSetterMap() {
		Map<String, AthleteHeaderInfo> base = new HashMap<>();
		// simple setters
		base.put("Membership", new AthleteHeaderInfo((a, s, c) -> a.setMembership(s), null));
		base.put("Card.lotNumber", new AthleteHeaderInfo((a, s, c) -> a.setLotNumber(s), null));
		base.put("LastName", new AthleteHeaderInfo((a, s, c) -> a.setLastName(s), null));
		base.put("FirstName", new AthleteHeaderInfo((a, s, c) -> a.setFirstName(s), null));
		base.put("Scoreboard.Team", new AthleteHeaderInfo((a, s, c) -> a.setTeam(s), null));
		base.put("Registration.birth", new AthleteHeaderInfo((a, s, c) -> {
			try {
				a.setFullBirthDate(s);
			} catch (Exception e) {
				throw new IllegalArgumentException("Invalid birth date: " + s);
			}
		}, DelayedSetter.BIRTHDATE));
		base.put("M/F", new AthleteHeaderInfo((a, s, c) -> {
			if (s != null && s.length() > 0)
				s = s.substring(0, 1).toUpperCase();
			a.setGender(s);
		}, DelayedSetter.GENDER));
		base.put("Card.category", new AthleteHeaderInfo((a, s, c) -> {
			try {
				a.setCategory(s);
			} catch (Exception e) {
				throw new IllegalArgumentException("Invalid category: " + s);
			}
		}, DelayedSetter.CATEGORY));
		base.put("Scoreboard.BodyWeight", new AthleteHeaderInfo((a, s, c) -> {
			if (s == null || s.isBlank())
				return;
			double d = Double.parseDouble(s);
			a.setBodyWeight(d);
		}, DelayedSetter.BODYWEIGHT));
		base.put("Results.Snatch Results.Declaration_abbrev", new AthleteHeaderInfo((a, s, c) -> a.setSnatch1Declaration(s), null));
		base.put("Results.CJ_abbrev Results.Declaration_abbrev", new AthleteHeaderInfo((a, s, c) -> a.setCleanJerk1Declaration(s), null));
		base.put("Group", new AthleteHeaderInfo((a, s, c) -> {
			try {
				a.setGroup(s);
			} catch (Exception e) {
				throw new IllegalArgumentException("Invalid group: " + s);
			}
		}, null));
		base.put("Card.entryTotal", new AthleteHeaderInfo((a, s, c) -> {
			if (s != null && !s.isBlank())
				a.setQualifyingTotal(Integer.parseInt(s));
		}, DelayedSetter.QUALIFYING_TOTAL));
		base.put("Coach", new AthleteHeaderInfo((a, s, c) -> a.setCoach(s), null));
		base.put("Custom1.Title", new AthleteHeaderInfo((a, s, c) -> a.setCustom1(s), null));
		base.put("Custom2.Title", new AthleteHeaderInfo((a, s, c) -> a.setCustom2(s), null));
		base.put("Registration.FederationCodesShort", new AthleteHeaderInfo((a, s, c) -> a.setFederationCodes(s), null));
		base.put("PersonalBestSnatch", new AthleteHeaderInfo((a, s, c) -> a.setPersonalBestSnatch(s), null));
		base.put("PersonalBestCleanJerk", new AthleteHeaderInfo((a, s, c) -> a.setPersonalBestCleanJerk(s), null));
		base.put("PersonalBestTotal", new AthleteHeaderInfo((a, s, c) -> a.setPersonalBestTotal(s), null));
		base.put("SubCategory", new AthleteHeaderInfo((a, s, c) -> a.setSubCategory(s), null));
		base.put("ComputedWeightClass", new AthleteHeaderInfo((a, s, c) -> {
			/* noop */ }, null));
		base.put("Competition.Invited/Extra", new AthleteHeaderInfo((a, s, c) -> a.setInvited(s != null && s.trim().toLowerCase().equals("true")), null));

		Map<String, AthleteHeaderInfo> result = new HashMap<>();
		for (Map.Entry<String, AthleteHeaderInfo> e : base.entrySet()) {
			String key = e.getKey();
			AthleteHeaderInfo info = e.getValue();
			try {
				//logger.debug("Registering athlete header key '{}' as '{}'", key, Translator.translate(key));
				String tCurrent = Translator.translate(key);
				if (tCurrent != null && !tCurrent.isBlank())
					result.putIfAbsent(tCurrent.trim().toLowerCase(), info);
			} catch (Exception ex) {
			}
			try {
				String tEng = Translator.translateExplicitLocale(key, Locale.ENGLISH);
				if (tEng != null && !tEng.isBlank())
					result.putIfAbsent(tEng.trim().toLowerCase(), info);
			} catch (Exception ex) {
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
			// Require a STRING cell with non-empty trimmed content; stop at any
			// blank/non-string/whitespace-only header cell.
			if (cell.getCellType() != CellType.STRING) {
				break;
			}
			String raw = cell.getStringCellValue();
			if (raw == null || raw.trim().isEmpty()) {
				break;
			}
			String headerValue = raw.trim().toLowerCase();
			CellSetterRG setter = GROUP_SETTER_MAP.get(headerValue);
			if (setter != null) {
				logger.debug("Mapped group header '{}' to setter", headerValue);
			} else {
				logger.debug("No setter found for group header '{}'", headerValue);
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
		// Fallback: compare normalized forms (remove non-alphanumerics, collapse spaces, lowercase)
		try {
			String tCurrent = Translator.translate(canonicalKey);
			String tEng = Translator.translateExplicitLocale(canonicalKey, Locale.ENGLISH);
			String normRead = normalizeHeader(trimmed);
			if (tCurrent != null && !tCurrent.isBlank() && normalizeHeader(tCurrent).equals(normRead))
				return true;
			if (tEng != null && !tEng.isBlank() && normalizeHeader(tEng).equals(normRead))
				return true;
		} catch (Exception ex) {
			// ignore
		}
		return false;
	}

	/**
	 * Normalize header text for more permissive matching: remove non-alphanumeric characters, replace multiple whitespace with single space, trim, and
	 * lowercase.
	 */
	private String normalizeHeader(String s) {
		if (s == null)
			return "";
		// Replace NBSP and other unicode spaces, then remove non-alphanumeric (keep letters and digits),
		// but preserve spaces so word boundaries remain. Collapse multi-space and lowercase.
		String cleaned = s.replaceAll("\\u00A0", " ");
		cleaned = cleaned.replaceAll("[^\\p{Alnum}\\s]", " ");
		cleaned = cleaned.replaceAll("\\s+", " ");
		return cleaned.trim().toLowerCase();
	}

	private void processException(RAthlete a, String s, Cell c, Exception e, Consumer<String> errorConsumer) {
		// Build the human-friendly message sent to the UI/error consumer
		String uiMsg = c.getAddress() + " " + e.getLocalizedMessage();
		if (errorConsumer != null) {
			errorConsumer.accept(uiMsg+ System.lineSeparator());
		}
		// Also log the same message (with context) and include the exception so it appears in logs
		this.logger.error(uiMsg);
		//LoggerUtils.logError(logger, e, true);
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
		int athleteHeaderStopColumn = Integer.MAX_VALUE;

		rows: while (rowIterator.hasNext()) {
			int iColumn = 0;
			Row row = rowIterator.next();
			try {
				Cell first = row.getCell(0);
				String firstVal = first == null ? "<null>" : cellToString(first);
				this.logger.debug("readAthletes: rowsToSkip={} rowIndex={} firstCell='{}'", rowsToSkip, iRow, firstVal);
			} catch (Exception ex) {
				this.logger.debug("readAthletes: rowsToSkip={} rowIndex={} firstCell=<error>", rowsToSkip, iRow);
			}
			if (iRow < rowsToSkip) {
				iRow++;
				continue;
			}
			if (iRow == rowsToSkip) {
				int lastCol = row.getLastCellNum() <= 0 ? 0 : row.getLastCellNum();
				List<AthleteHeaderInfo> orderedAthleteHeaderInfo = new ArrayList<>();
				for (iColumn = 0; iColumn < lastCol; iColumn++) {
					Cell cell = row.getCell(iColumn);
					if (cell == null) {
						athleteHeaderStopColumn = iColumn;
						orderedAthleteHeaderInfo.add(null);
						break;
					}
					if (cell.getCellType() != CellType.STRING) {
						athleteHeaderStopColumn = iColumn;
						orderedAthleteHeaderInfo.add(null);
						break;
					}
					String cellValue = cell.getStringCellValue();
					if (cellValue == null || cellValue.trim().isEmpty()) {
						athleteHeaderStopColumn = iColumn;
						orderedAthleteHeaderInfo.add(null);
						break;
					}
					String trimmedCellValue = cellValue.trim();
					AthleteHeaderInfo info = ATHLETE_SETTER_MAP.get(trimmedCellValue.toLowerCase());
					orderedAthleteHeaderInfo.add(info);
					if (info != null) {
						this.setterForColumn[iColumn] = (a, s, c) -> {
							try {
								info.setter.accept(a, s, c);
							} catch (RuntimeException rex) {
								processException(a, s, c, rex.getCause() == null ? rex : (Exception) rex.getCause(), errorConsumer);
							}
						};
						if (info.delayed != null) {
							this.delayedSetterColumns[info.delayed.ordinal()] = iColumn;
						}
					} else {
						String msg = Translator.translate("Registration.UnknownColumnHeader", trimmedCellValue) + " " + trimmedCellValue;
						if (errorConsumer != null) {
							errorConsumer.accept(msg);
						}
						this.logger.warn("Unknown header: {} (sheet={} row={})", trimmedCellValue, row.getSheet().getSheetName(), iRow);
					}
				}
				if (athleteHeaderStopColumn == Integer.MAX_VALUE) {
					athleteHeaderStopColumn = lastCol;
				}
				try {
					AthleteHeaderInfo[] orderedHeaderTable = orderedAthleteHeaderInfo.toArray(new AthleteHeaderInfo[0]);
					this.logger.debug("readAthletes: ordered athlete header table size={} (columns 0..{})", orderedHeaderTable.length, athleteHeaderStopColumn - 1);
					for (int idx = 0; idx < orderedHeaderTable.length; idx++) {
						Cell hcCell = row.getCell(idx);
						String headerText = hcCell == null ? "<null>" : cellToString(hcCell);
						AthleteHeaderInfo ahi = orderedHeaderTable[idx];
						if (ahi == null) {
							this.logger.debug("  [{}] '{}' -> <no setter>", idx, headerText);
						} else {
							this.logger.debug("  [{}] '{}' -> setter present delayed={}", idx, headerText, ahi.delayed);
						}
					}
				} catch (Exception ex) {
					this.logger.debug("readAthletes: failed to log ordered header table", ex);
				}
				// Log header discovery for debugging: header row index, stop column, and a preview
				try {
					StringBuilder headersPreview = new StringBuilder();
					for (int hc = 0; hc < athleteHeaderStopColumn; hc++) {
						Cell hcCell = row.getCell(hc);
						String v = hcCell == null ? "<null>" : cellToString(hcCell);
						if (hc > 0) {
							headersPreview.append(" | ");
						}
						headersPreview.append(v);
					}
					this.logger.debug("readAthletes: headerFound rowIndex={} stopCol={} headers={}", iRow, athleteHeaderStopColumn, headersPreview.toString());
				} catch (Exception ex) {
					this.logger.debug("readAthletes: headerFound rowIndex={} stopCol={} headers=<error>", iRow, athleteHeaderStopColumn);
				}
				// move to next row
				iRow++;
				continue;
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

					// Stop reading cells in this row when we've reached the first column
					// beyond the header (the first empty header column).
					if (iColumn >= athleteHeaderStopColumn) {
						break;
					}
					curRowEmpty = false;
					int delayedOrder = ArrayUtils.indexOf(this.delayedSetterColumns, iColumn);

					// If this column index has no header-mapped setter and is not a delayed setter,
					// treat it as the first empty header and stop processing further cells in this row.
					if ((iColumn >= this.setterForColumn.length || this.setterForColumn[iColumn] == null)
					        && delayedOrder < 0) {
						break;
					}

					if (delayedOrder < 0) {
						if (iColumn < this.setterForColumn.length && this.setterForColumn[iColumn] != null
						        && cell != null) {
							this.logger.debug("setting column {} {}", iColumn, cell.getAddress());
							try {
								this.setterForColumn[iColumn].accept(ra, cellValue.trim(), cell);
								this.logger.debug("applied setter for col {} value='{}'", iColumn, cellValue.trim());
							} catch (Exception e) {
								processException(ra, cellValue.trim(), cell, e, errorConsumer);
							}
						}
					} else {
						delayedSetterValues[delayedOrder] = cellValue.trim();
						delayedSetterCells[delayedOrder] = cell;
					}
				}
				if (curRowEmpty) {
					this.logger.debug("readAthletes: stopping at empty rowIndex={}", iRow);
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
				} else {
					this.logger.warn("readAthletes: skipping rowIndex={} missing names last='{}' first='{}'", iRow, lastName, firstName);
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
					this.logger.debug(
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
							// Stop processing cells in this row at the first column beyond the header
							if (setterTable == null || iColumn >= setterTable.length) {
								break;
							}
							try {
								setterTable[iColumn].set(rg, cell);
								rowHasData = true;
							} catch (Exception e) {
								String msg = MessageFormat.format("{0}[{1}] {2}\n", sheet.getSheetName(), cell.getAddress(), e.getMessage());
								errors.add(msg);
								logger.error(msg);
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