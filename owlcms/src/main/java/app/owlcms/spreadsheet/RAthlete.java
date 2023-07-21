/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.group.Group;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.utils.DateTimeUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Used for registration. Converts from String to data types as required to
 * simplify Excel/CSV imports
 *
 * @author Jean-François Lamy
 *
 */
public class RAthlete {

	public static final String NoTeamMarker = "/NoTeam";
	private Pattern legacyPattern;
	Athlete a = new Athlete();

	final Logger logger = (Logger) LoggerFactory.getLogger(RAthlete.class);

	{
		logger.setLevel(Level.INFO);
	}

	public RAthlete() {
	}

	public Athlete getAthlete() {
		return a;
	}

//	public String getCoach() {
//		return a.getCoach();
//	}
//
//	public String getCustom1() {
//		return a.getCustom1();
//	}
//
//	public String getCustom2() {
//		return a.getCustom2();
//	}
//
//	public String getFederationCodes() {
//		return a.getFederationCodes();
//	}

	/**
	 * @param bodyWeight
	 */
	public void setBodyWeight(Double bodyWeight) {
		a.setBodyWeight(bodyWeight);
	}

	/**
	 * @param category
	 * @throws Exception
	 * @see app.owlcms.data.athlete.Athlete#setCategory(app.owlcms.data.category.Category)
	 */
	public void setCategory(String s) throws Exception {
		if (s != null) {
			s = CharMatcher.javaIsoControl().removeFrom(s);
		}
		if (s == null || s.isBlank()) {
			// no category, infer from age and body weight
			a.computeMainAndEligibleCategories();
			a.getParticipations().stream().forEach(p -> p.setTeamMember(true));
			if (a.getCategory() == null) {
				throw new Exception(Translator.translate("Upload.CannotDetermineRegistrationCategory"));
			}
			return;
		}
		s = CharMatcher.javaIsoControl().removeFrom(s);
		String[] parts = s.split("\\|");
		if (parts.length >= 1) {
			String catName = parts[0].trim();

			// check for team exclusion marker.
			boolean teamMember = true;
			if (catName.endsWith(NoTeamMarker)) {
				catName = catName.substring(0, s.length() - NoTeamMarker.length());
				teamMember = false;
			}

			Category c;
			if ((c = RCompetition.getActiveCategories().get(catName)) != null) {
				// exact match for a category. This is the athlete's registration category.
				processEligibilityAndTeams(parts, c, teamMember);
			} else {
				// we have a short form category. infer from age and category limit
				setCategoryHeuristics(s);
				a.getParticipations().stream().forEach(p -> p.setTeamMember(true));
			}
		}

		return;
	}

	/**
	 * @param s
	 */
	public void setCleanJerk1Declaration(String s) {
		if (s != null) {
			s = CharMatcher.javaIsoControl().removeFrom(s);
		}
		a.setCleanJerk1Declaration(s);
	}

	public void setCoach(String s) {
//		if (s != null) {
//			s = CharMatcher.javaIsoControl().removeFrom(s);
//		}
		a.setCoach(s);
	}

	public void setCustom1(String s) {
//		if (s != null) {
//			s = CharMatcher.javaIsoControl().removeFrom(s);
//		}
		a.setCustom1(s);
	}

	public void setCustom2(String s) {
//		if (s != null) {
//			s = CharMatcher.javaIsoControl().removeFrom(s);
//		}
		a.setCustom2(s);
	}

	public void setFederationCodes(String s) {
		if (s != null) {
			s = CharMatcher.javaIsoControl().removeFrom(s);
		}
		a.setFederationCodes(s);
	}

	/**
	 * @param s
	 * @see app.owlcms.data.athlete.Athlete#setFirstName(java.lang.String)
	 */
	public void setFirstName(String s) {
		if (s != null) {
			s = CharMatcher.javaIsoControl().removeFrom(s);
		}
		a.setFirstName(s);
	}

	/**
	 * Note the mapping file must process the birth date before the category, as it
	 * is a required input to determine the category.
	 *
	 * @param category
	 * @throws Exception
	 * @see app.owlcms.data.athlete.Athlete#setCategory(app.owlcms.data.category.Category)
	 */
	public void setFullBirthDate(String s) throws Exception {
		if (s != null) {
			s = CharMatcher.javaIsoControl().removeFrom(s);
		}
		s = CharMatcher.javaIsoControl().removeFrom(s);
		try {
			long l = Long.parseLong(s);
			if (l < 3000) {
				a.setYearOfBirth((int) l);
				// logger.debug("short " + l);
			} else {
				LocalDate epoch = LocalDate.of(1900, 1, 1);
				LocalDate plusDays = epoch.plusDays(l - 2); // Excel quirks: 1 is 1900-01-01 and 1900-02-29 did not
				                                            // exist.
				// logger.debug("long " + plusDays);
				a.setFullBirthDate(plusDays);
			}
			return;
		} catch (NumberFormatException e) {
			// logger.debug("localized");
			LocalDate parse = DateTimeUtils.parseLocalizedOrISO8601Date(s, OwlcmsSession.getLocale());
			a.setFullBirthDate(parse);
		}
	}

	/**
	 * @param lastName
	 * @see app.owlcms.data.athlete.Athlete#setLastName(java.lang.String)
	 */
	public void setGender(String s) {
		if (s != null) {
			s = CharMatcher.javaIsoControl().removeFrom(s);
		}
		logger.trace("setting gender {} for athlete {}", s, a.getLastName());
		if (s == null) {
			return;
		}
		a.setGender(Gender.valueOf(s.toUpperCase()));
	}

	/**
	 * @param group
	 * @throws Exception
	 * @see app.owlcms.data.athlete.Athlete#setGroupName(app.owlcms.data.category.Group)
	 */
	public void setGroup(String s) throws Exception {
		if (s != null) {
			s = CharMatcher.javaIsoControl().removeFrom(s);
		}
		if (s == null) {
			return;
		}
		Group g;
		if ((g = RCompetition.getActiveGroups().get(s)) != null) {
			a.setGroup(g);
		} else {
			throw new Exception(Translator.translate("Upload.GroupNotDefined", s));
		}
	}

	/**
	 * @param s
	 * @see app.owlcms.data.athlete.Athlete#setLastName(java.lang.String)
	 */
	public void setLastName(String s) {
		if (s != null) {
			s = CharMatcher.javaIsoControl().removeFrom(s);
		}
		a.setLastName(s);
	}

	/**
	 * @param s
	 * @see app.owlcms.data.athlete.Athlete#setLotNumber(java.lang.Integer)
	 */
	public void setLotNumber(String s) {
		if (s != null) {
			s = CharMatcher.javaIsoControl().removeFrom(s);
		}
		if (s == null) {
			return;
		}
		a.setLotNumber(Integer.parseInt(s));
	}

	/**
	 * @param s
	 * @see app.owlcms.data.athlete.Athlete#setMembership(java.lang.String)
	 */
	public void setMembership(String s) {
		if (s != null) {
			s = CharMatcher.javaIsoControl().removeFrom(s);
		}
		a.setMembership(s);
	}

	public void setPersonalBestCleanJerk(String s) {
		if (s != null) {
			s = CharMatcher.javaIsoControl().removeFrom(s);
		}
		if (s != null && !s.isEmpty()) {
			a.setPersonalBestCleanJerk(Integer.parseInt(s));
		}
	}

	public void setPersonalBestSnatch(String s) {
		if (s != null) {
			s = CharMatcher.javaIsoControl().removeFrom(s);
		}
		if (s != null && !s.isEmpty()) {
			a.setPersonalBestSnatch(Integer.parseInt(s));
		}
	}

	public void setPersonalBestTotal(String s) {
		if (s != null) {
			s = CharMatcher.javaIsoControl().removeFrom(s);
		}
		if (s != null && !s.isEmpty()) {
			a.setPersonalBestTotal(Integer.parseInt(s));
		}
	}

	/**
	 * @param qualifyingTotal
	 * @see app.owlcms.data.athlete.Athlete#setQualifyingTotal(java.lang.Integer)
	 */
	public void setQualifyingTotal(Integer qualifyingTotal) {
		a.setQualifyingTotal(qualifyingTotal);
	}

	/**
	 * @param s
	 */
	public void setSnatch1Declaration(String s) {
		if (s != null) {
			s = CharMatcher.javaIsoControl().removeFrom(s);
		}
		a.setSnatch1Declaration(s);
	}

	/**
	 * @param s
	 * @see app.owlcms.data.athlete.Athlete#setTeam(java.lang.String)
	 */
	public void setTeam(String s) {
		if (s != null) {
			s = CharMatcher.javaIsoControl().removeFrom(s);
		}
		a.setTeam(s);
	}

	private boolean addIfEligible(Set<Category> eligibleCategories, Set<Category> teams, Integer athleteQTotal,
	        Integer athleteAge,
	        boolean teamMember, Category c2) {
		boolean added = false;
		Integer minAge = c2.getAgeGroup().getMinAge();
		Integer maxAge = c2.getAgeGroup().getMaxAge();
		// logger.debug("{} athleteAge {} min {} max {}", athleteAge, minAge, maxAge);
		if (((athleteQTotal != null && athleteQTotal >= c2.getQualifyingTotal())
		        || ((athleteQTotal == null || athleteQTotal == 0) && c2.getQualifyingTotal() == 0))
		        && athleteAge >= minAge
		        && athleteAge <= maxAge) {
			eligibleCategories.add(c2);
			added = true;
			if (teamMember) {
				teams.add(c2);
			}
		}
		return added;
	}

	private Category findByAgeBW(Matcher legacyResult, double searchBodyWeight, int age, int qualifyingTotal)
	        throws Exception {
//		List<Category> found = CategoryRepository.findByGenderAgeBW(a.getGender(), age, searchBodyWeight);
//		Set<Category> eligibles = new LinkedHashSet<>();
//		eligibles = found.stream().filter(c -> qualifyingTotal >= c.getQualifyingTotal())
//		        .collect(Collectors.toSet());
		List<Category> eligibles = CategoryRepository.doFindEligibleCategories(a, a.getGender(), age, searchBodyWeight, qualifyingTotal);
		a.setEligibleCategories(new HashSet<>(eligibles));

		Category category = eligibles.size() > 0 ? eligibles.get(0) : null;
		if (category == null) {
			throw new Exception(
			        Translator.translate(
			                "Upload.CategoryNotFound", age, a.getGender(),
			                legacyResult.group(2) + legacyResult.group(3)));
		}
		return category;
	}
	
	private void fixLegacyGender(Matcher result) throws Exception {
		String genderLetter = result.group(1);
		if (a.getGender() == null) {
			if (genderLetter.equalsIgnoreCase("f")) {
				a.setGender(Gender.F);
			} else if (genderLetter.equalsIgnoreCase("m")) {
				a.setGender(Gender.M);
			}
		} else if (!genderLetter.isEmpty()) {
			// letter present, should match gender
			if ((genderLetter.equalsIgnoreCase("f") && a.getGender() != Gender.F)
			        || (genderLetter.equalsIgnoreCase("m") && a.getGender() != Gender.M)) {
				throw new Exception(Translator.translate("Upload.GenderMismatch", result.group(0), a.getGender()));
			}
		} else {
			// nothing to do gender is known and consistent.
		}
	}

	private Pattern getLegacyPattern() {
		if (legacyPattern == null) {
			setLegacyPattern(Pattern
			        .compile("([mMfF]?) *([>" + Pattern.quote("+") + "]?) *(\\d+) *(" + Pattern.quote("+") + "?)$"));
		}
		return legacyPattern;
	}

	private void processEligibilityAndTeams(String[] parts, Category c, boolean mainCategoryTeamMember)
	        throws Exception {
		Set<Category> eligibleCategories = new LinkedHashSet<>();
		Set<Category> teams = new LinkedHashSet<>();
		Integer athleteQTotal = this.getAthlete().getQualifyingTotal();
		Integer athleteAge = this.getAthlete().getAge();

		boolean addedToMainCat = addIfEligible(eligibleCategories, teams, athleteQTotal, athleteAge,
		        mainCategoryTeamMember, c);
		if (!addedToMainCat) {
			throw new Exception(Translator.translate("Upload.AthleteRegistrationCategoryProblem"));
		}

		// process the other participations. They are ; separated.
		if (parts.length > 1) {
			String[] eligibleNames = parts[1].split(";");
			for (String eligibleName : eligibleNames) {
				boolean teamMember = true;
				if (eligibleName.endsWith(NoTeamMarker)) {
					eligibleName = eligibleName.substring(0, eligibleName.length() - NoTeamMarker.length());
					teamMember = false;
				}
				Category c2;
				if ((c2 = RCompetition.getActiveCategories().get(eligibleName.trim())) != null) {
					addIfEligible(eligibleCategories, teams, athleteQTotal, athleteAge, teamMember, c2);
				} else {
					throw new Exception(
					        Translator.translate("Upload.CategoryNotFoundByName", eligibleName.trim()));
				}
			}
		}

		RCompetition.getAthleteToEligibles().put(a.getId(), eligibleCategories);
		RCompetition.getAthleteToTeams().put(a.getId(), teams);
	}

	private void setCategoryHeuristics(String categoryName) throws Exception {
		Matcher legacyResult = getLegacyPattern().matcher(categoryName);
		double searchBodyWeight;
		if (!legacyResult.matches()) {

			// try by explicit name
			Category category = RCompetition.getActiveCategories().get(categoryName);
			if (category == null) {
				throw new Exception(Translator.translate("Upload.CategoryNotFoundByName", categoryName));
			}
			if (category.getGender() != a.getGender()) {
				throw new Exception(
				        Translator.translate("Upload.GenderMismatch", categoryName, a.getGender()));
			}
			a.setCategory(category);
			return;
		} else {
			fixLegacyGender(legacyResult);
			if (!legacyResult.group(2).isEmpty() || !legacyResult.group(4).isEmpty()) {
				// > or +
				searchBodyWeight = Integer.parseInt(legacyResult.group(3)) + 0.1D;
			} else {
				searchBodyWeight = Integer.parseInt(legacyResult.group(3)) - 0.1D;
			}
			// logger.debug("gt 1:'{}' 2:'{}' 3:'{}' 4:'{}'", legacyResult.group(1),
			// legacyResult.group(2),
			// legacyResult.group(3), legacyResult.group(4));
		}

		int age;
		// if no birth date, try with 0 and see if we get the default group.
		if (a.getFullBirthDate() == null) {
			age = 0;
		} else {
			age = a.getAge();
		}

		Integer qualifyingTotal = this.getAthlete().getQualifyingTotal();
		Category category = findByAgeBW(legacyResult, searchBodyWeight, age,
		        qualifyingTotal != null ? qualifyingTotal : 999);

		a.setCategory(category);
		// logger.debug("setting category to {} athlete {}",category.longDump(),
		// a.longDump());
	}

	private void setLegacyPattern(Pattern legacyPattern) {
		this.legacyPattern = legacyPattern;
	}
}
