/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.util.ArrayList;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.EligibleForIndividualRankingStatus;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.group.Group;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.utils.DateTimeUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Used for registration. Converts from String to data types as required to simplify Excel/CSV imports
 *
 * @author Jean-François Lamy
 *
 */
public class RAthlete {

	private static final String YesTeamMarker = "+T";
	private static final String NoTeamMarker = "-T";
	private static final String YesMixedMarker = "+MT";
	private static final String NoMixedMarker = "-MT";
	private static final String YesTeamValue = "YesTeam";
	private static final String NoTeamValue = "NoTeam";
	private static final String YesMixedValue = "YesMixed";
	private static final String NoMixedValue = "NoMixed";
	private Pattern legacyPattern;
	Athlete a;
	final Logger logger = (Logger) LoggerFactory.getLogger(RAthlete.class);

	private static class ParticipationSpec {
		private final String categoryName;
		private final boolean teamMember;
		private final boolean mixedTeamMember;

		private ParticipationSpec(String categoryName, boolean teamMember, boolean mixedTeamMember) {
			this.categoryName = categoryName;
			this.teamMember = teamMember;
			this.mixedTeamMember = mixedTeamMember;
		}
	}

	{
		this.logger.setLevel(Level.INFO);
	}

	public RAthlete() {
		this.a = new Athlete();
		this.a.setCategoryFinished(false);
	}

	public Athlete getAthlete() {
		return this.a;
	}

	public static String appendMembershipMarkers(String categoryName, boolean teamMember, boolean mixedTeamMember) {
		List<String> markers = new ArrayList<>();
		if (!teamMember) {
			markers.add(NoTeamMarker);
		}
		if (mixedTeamMember) {
			markers.add(YesMixedMarker);
		}
		if (markers.isEmpty()) {
			return categoryName;
		}
		return categoryName + "/" + String.join(",", markers);
	}

	/**
	 * @param bodyWeight
	 */
	public void setBodyWeight(Double bodyWeight) {
		this.a.setBodyWeight(bodyWeight);
	}

	/**
	 * @param category
	 * @throws Exception
	 * @see app.owlcms.data.athlete.Athlete#computeCategory(app.owlcms.data.category.Category)
	 */
	public void setCategory(String s) throws Exception {
		if (s != null) {
			s = CharMatcher.javaIsoControl().removeFrom(s);
		}
		if (s == null || s.isBlank()) {
			// no category, infer from age and body weight
			this.a.computeMainAndEligibleCategories();
			this.a.getParticipations().stream().forEach(p -> p.setTeamMember(true));
			if (this.a.getCategory() == null) {
				Integer athleteAge = null;
				try {
					athleteAge = this.a.getAge();
				} catch (Exception e) {
				}
				throw new Exception(Translator.translate("Upload.NoEligibleCategoryMatch",
					athleteAge != null ? athleteAge.toString() : "?",
					this.a.getGender() != null ? this.a.getGender().toString() : "?",
					this.a.getBodyWeight() != null ? this.a.getBodyWeight().toString() : "?"));
			}
			return;
		}
		s = CharMatcher.javaIsoControl().removeFrom(s);
		if (s.contains("|")) {
			String[] parts = s.split(Pattern.quote("|"));
			doLegacyParts(s, parts);
		} else {
			getPartsWithSeparator(s);
		}

	}

	/**
	 * @param s
	 */
	public void setCleanJerk1Declaration(String s) {
		if (s != null) {
			s = CharMatcher.javaIsoControl().removeFrom(s);
		}
		this.a.setCleanJerk1Declaration(s);
	}

	public void setCoach(String s) {
		this.a.setCoach(s);
	}

	public void setCustom1(String s) {
		this.a.setCustom1(s);
	}

	public void setCustom2(String s) {
		this.a.setCustom2(s);
	}

	public void setFederationCodes(String s) {
		if (s != null) {
			s = CharMatcher.javaIsoControl().removeFrom(s);
		}
		this.a.setFederationCodes(s);
	}

	/**
	 * @param s
	 * @see app.owlcms.data.athlete.Athlete#setFirstName(java.lang.String)
	 */
	public void setFirstName(String s) {
		if (s != null) {
			s = CharMatcher.javaIsoControl().removeFrom(s);
		}
		this.a.setFirstName(s);
	}

	/**
	 * Note the mapping file must process the birth date before the category, as it is a required input to determine the category.
	 *
	 * @param category
	 * @throws Exception
	 * @see app.owlcms.data.athlete.Athlete#computeCategory(app.owlcms.data.category.Category)
	 */
	public void setFullBirthDate(String s) throws Exception {
		if (s != null) {
			s = CharMatcher.javaIsoControl().removeFrom(s);
		}
		try {
			long l = Long.parseLong(s);
			if (l < 3000) {
				this.a.setYearOfBirth((int) l);
				// logger.debug("short " + l);
			} else {
				// assume that a large number is an Excel date as an integer
				LocalDate epoch = LocalDate.of(1900, 1, 1);
				LocalDate plusDays = epoch.plusDays(l - 2); // Excel quirks: 1 is 1900-01-01 and 1900-02-29 did not
				                                            // exist.
				// logger.debug("long " + plusDays);
				this.a.setFullBirthDate(plusDays);
			}
		} catch (NumberFormatException e) {
			// logger.debug("localized");
			LocalDate parse = DateTimeUtils.parseLocalizedOrISO8601Date(s, OwlcmsSession.getLocale());
			this.a.setFullBirthDate(parse);
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
		this.logger.trace("setting gender {} for athlete {}", s, this.a.getLastName());
		if (s == null) {
			return;
		}
		String t = s.trim();

		// 1) Try matching the canonical gender names among the allowed set
		for (Gender g : Gender.mfValues()) {
			if (g.name().equalsIgnoreCase(t)) {
				this.a.setGender(g);
				return;
			}
		}

		// 2) Try matching each allowed gender's translated gender code
		for (Gender g : Gender.mfValues()) {
			try {
				String translated = g.getTranslatedGenderCode();
				if (translated != null && translated.equalsIgnoreCase(t)) {
					this.a.setGender(g);
					return;
				}
			} catch (Exception ex) {
				// ignore translation failures and continue
			}
		}

		// 3) Fallback to original behavior (this will throw IllegalArgumentException if invalid)
		this.a.setGender(Gender.valueOf(t.toUpperCase()));
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
			this.a.setGroup(g);
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
		this.a.setLastName(s);
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
		this.a.setLotNumber(Integer.parseInt(s));
	}

	/**
	 * @param s
	 * @see app.owlcms.data.athlete.Athlete#setMembership(java.lang.String)
	 */
	public void setMembership(String s) {
		if (s != null) {
			s = CharMatcher.javaIsoControl().removeFrom(s);
		}
		this.a.setMembership(s);
	}

	public void setPersonalBestCleanJerk(String s) {
		if (s != null) {
			s = CharMatcher.javaIsoControl().removeFrom(s);
		}
		if (s != null && !s.isEmpty()) {
			this.a.setPersonalBestCleanJerk(Integer.parseInt(s));
		}
	}

	public void setPersonalBestSnatch(String s) {
		if (s != null) {
			s = CharMatcher.javaIsoControl().removeFrom(s);
		}
		if (s != null && !s.isEmpty()) {
			this.a.setPersonalBestSnatch(Integer.parseInt(s));
		}
	}

	public void setPersonalBestTotal(String s) {
		if (s != null) {
			s = CharMatcher.javaIsoControl().removeFrom(s);
		}
		if (s != null && !s.isEmpty()) {
			this.a.setPersonalBestTotal(Integer.parseInt(s));
		}
	}

	/**
	 * @param qualifyingTotal
	 * @see app.owlcms.data.athlete.Athlete#setQualifyingTotal(java.lang.Integer)
	 */
	public void setQualifyingTotal(Integer qualifyingTotal) {
		this.a.setQualifyingTotal(qualifyingTotal);
	}

	/**
	 * @param s
	 */
	public void setSnatch1Declaration(String s) {
		if (s != null) {
			s = CharMatcher.javaIsoControl().removeFrom(s);
		}
		this.a.setSnatch1Declaration(s);
	}

	public void setSubCategory(String s) {
		this.a.setSubCategory(s);
	}

	/**
	 * @param s
	 * @see app.owlcms.data.athlete.Athlete#setTeam(java.lang.String)
	 */
	public void setTeam(String s) {
		if (s != null) {
			s = CharMatcher.javaIsoControl().removeFrom(s);
		}
		this.a.setTeam(s);
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
		        && (athleteAge == null
		                || (athleteAge >= minAge && athleteAge <= maxAge))) {
			eligibleCategories.add(c2);
			//logger.debug("eligible categories {}",c2);
			added = true;
			if (teamMember) {
				//logger.debug("teams {}",c2);
				teams.add(c2);
			}
		}
		return added;
	}

	private void doLegacyParts(String s, String[] parts) throws Exception {
		if (parts.length >= 1) {
			ParticipationSpec mainParticipation = parseParticipationSpec(parts[0].trim());
			String catName = mainParticipation.categoryName;

			Category c = findActiveCategoryByName(catName);
			if (c != null) {
				// exact match for a category. This is the athlete's registration category.
				processEligibilityAndTeams(parts, c, mainParticipation.teamMember, mainParticipation.mixedTeamMember);
			} else {
				if (parts.length == 1 && !parts[0].contains(" ")) {
					// we have a short form category. infer from age and category limit
					setCategoryHeuristics(catName);
					final var tm = mainParticipation.teamMember;
					final var mtm = mainParticipation.mixedTeamMember;
					this.a.getParticipations().stream().forEach(p -> {
						p.setTeamMember(tm);
						p.setMixedTeamMember(mtm && p.getCategory() != null && p.getCategory().getAgeGroup() != null
						        && p.getCategory().getAgeGroup().isMixedTeams());
					});
				} else {
					throw new Exception(
					        Translator.translate("Upload.CategoryNotFoundByName", catName.trim()));
				}

			}
		}
	}

	private Category findByAgeBW(Matcher legacyResult, double searchBodyWeight, int age, int qualifyingTotal)
	        throws Exception {
		List<Category> eligibles = CategoryRepository.doFindEligibleCategories(this.a, this.a.getGender(), age,
		        searchBodyWeight, qualifyingTotal);
		
		RCompetition.putEligibles(this.a.getId(), new LinkedHashSet<>(eligibles));
		RCompetition.putTeams(this.a.getId(), new LinkedHashSet<>(eligibles));

		Category category = eligibles.size() > 0 ? eligibles.get(0) : null;
		if (category == null) {
			// The short-hand notation was provided but doesn't match any category
			String notation = legacyResult.group(2) + legacyResult.group(3);
			throw new Exception(
			        Translator.translate(
			                "Upload.CategoryNotFoundByName", notation + " (" + this.a.getGender() + ", age " + age + ")"));
		}
		return category;
	}

	private void fixLegacyGender(Matcher result) throws Exception {
		String genderLetter = result.group(1);
		if (this.a.getGender() == null) {
			if (genderLetter.equalsIgnoreCase("f")) {
				this.a.setGender(Gender.F);
			} else if (genderLetter.equalsIgnoreCase("m")) {
				this.a.setGender(Gender.M);
			}
		} else if (!genderLetter.isEmpty()) {
			// letter present, should match gender
			if ((genderLetter.equalsIgnoreCase("f") && this.a.getGender() != Gender.F)
			        || (genderLetter.equalsIgnoreCase("m") && this.a.getGender() != Gender.M)) {
				throw new Exception(Translator.translate("Upload.GenderMismatch", result.group(0), this.a.getGender()));
			}
		} else {
			// nothing to do gender is known and consistent.
		}
	}

	private Pattern getLegacyPattern() {
		if (this.legacyPattern == null) {
			String regex = "([mMfFwW]?) *([>" + Pattern.quote("+") + "]?) *(\\d+) *(" + Pattern.quote("+") + "?)$";
			setLegacyPattern(Pattern.compile(regex));
		}
		return this.legacyPattern;
	}

	private void getPartsWithSeparator(String s) throws Exception {
		if (s == null || s.isBlank()) {
			return;
		}
		// create a parts as in the legacy
		boolean usaw = Config.getCurrent().featureSwitch("usawSessionBlocks");
		if (Config.getCurrent().featureSwitch("usawSessionBlocks")) {
			s = s.replaceAll("(\\d+)\\s?kg", "$1");
		}

		String[] allParts = useLegacyUsawSplit(s, usaw) ? s.split(",|;|\\/") : s.split(",|;");
		List<String> partsList = mergeMarkerTokens(Arrays.asList(allParts).stream()
		        .filter(s1 -> (s1 != null && !s1.isBlank()))
		        .map(s1 -> s1.trim())
		        .toList());
		// logger.debug("partsList {}",partsList);

		String[] parts;
		if (partsList.size() == 1) {
			parts = new String[1];
			parts[0] = partsList.get(0);
			doLegacyParts(s, parts);
		} else if (partsList.size() >= 1) {
			parts = new String[2];
			parts[0] = partsList.get(0);
			// brain-dead logic to reuse existing code. Should fix old to use new instead...
			StringBuffer sb = new StringBuffer();
			for (int i = 1; i < partsList.size(); i++) {
				if (i > 1) {
					sb.append(";");
				}
				sb.append(partsList.get(i).trim());
			}
			parts[1] = sb.toString();
			doLegacyParts(s, parts);
		}
	}

	private void processEligibilityAndTeams(String[] parts, Category c, boolean mainCategoryTeamMember,
	        boolean mainCategoryMixedTeamMember)
	        throws Exception {
		LinkedHashSet<Category> eligibleCategories = new LinkedHashSet<>();
		LinkedHashSet<Category> teams = new LinkedHashSet<>();
		LinkedHashSet<Category> mixedTeams = new LinkedHashSet<>();
		Integer athleteQTotal = this.getAthlete().getQualifyingTotal();
		Integer athleteAge = null;
		try {
			athleteAge = this.getAthlete().getAge();
		} catch (Exception e) {
		}

		if (mainCategoryMixedTeamMember && !c.getAgeGroup().isMixedTeams()) {
			throw new Exception(Translator.translate("Upload.CategoryNotFoundByName",
			        c.getDisplayName() + "/" + YesMixedMarker));
		}

		boolean addedToMainCat = addIfEligible(eligibleCategories, teams, athleteQTotal, athleteAge,
		        mainCategoryTeamMember, c);
		if (!addedToMainCat) {
			throw new Exception(Translator.translate("Upload.AthleteNotEligibleForCategory", c.getDisplayName(), 
				athleteAge != null ? athleteAge.toString() : "?", 
				athleteQTotal != null ? athleteQTotal.toString() : "0"));
		} else {
			this.a.setCategory(c);
			if (mainCategoryMixedTeamMember) {
				mixedTeams.add(c);
			}
		}

		// process the other participations. They are ; separated.
		if (parts.length > 1) {
			//logger.debug("additional categories {}",parts[1]);
			String[] eligibleNames = parts[1].split(";");
			for (String eligibleName : eligibleNames) {
				ParticipationSpec participationSpec = parseParticipationSpec(eligibleName);
				Category c2 = findActiveCategoryByName(participationSpec.categoryName.trim());
					if (c2 != null) {
					if (participationSpec.mixedTeamMember && !c2.getAgeGroup().isMixedTeams()) {
						throw new Exception(Translator.translate("Upload.CategoryNotFoundByName",
							        participationSpec.categoryName + "/" + YesMixedMarker));
					}
					boolean addedToEligible = addIfEligible(eligibleCategories, teams, athleteQTotal, athleteAge,
					        participationSpec.teamMember, c2);
					if (!addedToEligible) {
						throw new Exception(Translator.translate("Upload.AthleteNotEligibleForCategory", participationSpec.categoryName,
							athleteAge != null ? athleteAge.toString() : "?",
							athleteQTotal != null ? athleteQTotal.toString() : "0"));
					}
					if (participationSpec.mixedTeamMember) {
						mixedTeams.add(c2);
					}
				} else {
					// logger.debug("{} {}\n{}",Translator.translate("Upload.CategoryNotFoundByName", eligibleName.trim(), LoggerUtils.stackTrace()));
					throw new Exception(
					        Translator.translate("Upload.CategoryNotFoundByName", participationSpec.categoryName.trim()));
				}
			}
		} else {
			//logger.debug("no other part");
		}

		//logger.debug("{} this.a.getCategory {} {}",this.a.getId(), this.a.getCategory(), eligibleCategories);
		RCompetition.putEligibles(this.a.getId(), eligibleCategories);
		RCompetition.putTeams(this.a.getId(), teams);
		RCompetition.putMixedTeams(this.a.getId(), mixedTeams);
	}

	private List<String> mergeMarkerTokens(List<String> rawParts) {
		List<String> merged = new ArrayList<>();
		for (String part : rawParts) {
			if (!merged.isEmpty() && isStandaloneMarkerToken(part)) {
				int last = merged.size() - 1;
				merged.set(last, merged.get(last) + "," + part);
			} else {
				merged.add(part);
			}
		}
		return merged;
	}

	private ParticipationSpec parseParticipationSpec(String entry) throws Exception {
		String trimmed = entry != null ? entry.trim() : "";
		boolean teamMember = true;
		boolean mixedTeamMember = false;

		if (trimmed.endsWith("/")) {
			return new ParticipationSpec(trimmed.substring(0, trimmed.length() - 1).trim(), false, false);
		}

		int slashIndex = trimmed.indexOf('/');
		if (slashIndex < 0) {
			return new ParticipationSpec(trimmed, true, false);
		}

		String categoryName = trimmed.substring(0, slashIndex).trim();
		String markerSection = trimmed.substring(slashIndex + 1).trim();
		if (markerSection.isEmpty()) {
			return new ParticipationSpec(categoryName, false, false);
		}

		for (String marker : markerSection.split(",")) {
			String normalized = normalizeMembershipMarker(marker);
			if (normalized.isEmpty()) {
				continue;
			}
			switch (normalized) {
				case YesTeamMarker:
					teamMember = true;
					break;
				case NoTeamMarker:
					teamMember = false;
					break;
				case YesMixedMarker:
					mixedTeamMember = true;
					break;
				case NoMixedMarker:
					mixedTeamMember = false;
					break;
				default:
					throw new Exception(Translator.translate("Upload.CategoryNotFoundByName", entry.trim()));
			}
		}
		return new ParticipationSpec(categoryName, teamMember, mixedTeamMember);
	}

	private String normalizeMembershipMarker(String marker) {
		String trimmed = marker != null ? marker.trim() : "";
		if (matchesMarker(trimmed, YesTeamMarker, YesTeamValue)) {
			return YesTeamMarker;
		}
		if (matchesMarker(trimmed, NoTeamMarker, NoTeamValue)) {
			return NoTeamMarker;
		}
		if (matchesMarker(trimmed, YesMixedMarker, YesMixedValue)) {
			return YesMixedMarker;
		}
		if (matchesMarker(trimmed, NoMixedMarker, NoMixedValue)) {
			return NoMixedMarker;
		}
		return trimmed;
	}

	private boolean matchesMarker(String value, String... acceptedValues) {
		for (String acceptedValue : acceptedValues) {
			if (acceptedValue.equalsIgnoreCase(value)) {
				return true;
			}
		}
		return false;
	}

	private boolean isStandaloneMarkerToken(String token) {
		String normalized = normalizeMembershipMarker(token);
		return normalized.equals(YesTeamMarker) || normalized.equals(NoTeamMarker)
		        || normalized.equals(YesMixedMarker) || normalized.equals(NoMixedMarker);
	}

	private boolean useLegacyUsawSplit(String value, boolean usaw) {
		if (!usaw) {
			return false;
		}
		String markerPattern = String.join("|",
		        Pattern.quote(YesTeamMarker),
		        Pattern.quote(NoTeamMarker),
		        Pattern.quote(YesMixedMarker),
		        Pattern.quote(NoMixedMarker),
		        Pattern.quote(YesTeamValue),
		        Pattern.quote(NoTeamValue),
		        Pattern.quote(YesMixedValue),
		        Pattern.quote(NoMixedValue));
		return !value.matches("(?i).*\\/(" + markerPattern + ")(,.*)?$");
	}

	private void setCategoryHeuristics(String categoryName) throws Exception {
		Matcher legacyResult = getLegacyPattern().matcher(categoryName);
		double searchBodyWeight;
		if (!legacyResult.matches()) {
			// try by explicit name
			Category category = findActiveCategoryByName(categoryName);
			if (category == null) {
				throw new Exception(Translator.translate("Upload.CategoryNotFoundByName", categoryName));
			}
			if (category.getGender() != this.a.getGender()) {
				throw new Exception(
				        Translator.translate("Upload.GenderMismatch", categoryName, this.a.getGender()));
			}
			this.a.computeCategory(category);
			return;
		} else {
			fixLegacyGender(legacyResult);
			if (!legacyResult.group(2).isEmpty() || !legacyResult.group(4).isEmpty()) {
				// > or +
				searchBodyWeight = Integer.parseInt(legacyResult.group(3)) + 0.1D;
			} else {
				searchBodyWeight = Integer.parseInt(legacyResult.group(3)) - 0.1D;
			}
		}

		int age;
		// if no birth date, try with 0 and see if we get the default group.
		if (this.a.getFullBirthDate() == null) {
			age = 0;
		} else {
			age = this.a.getAge();
		}

		Integer qualifyingTotal = this.getAthlete().getQualifyingTotal();
		Category category = findByAgeBW(legacyResult, searchBodyWeight, age,
		        qualifyingTotal != null ? qualifyingTotal : 999);

		this.a.computeCategory(category);
		// logger.debug("setting category to {} athlete {}",category.longDump(),
		// a.longDump());
	}

	private void setLegacyPattern(Pattern legacyPattern) {
		this.legacyPattern = legacyPattern;
	}

	public void setInvited(boolean b) {
		if (b) {
			this.a.setIndividualEligibilityStatus(EligibleForIndividualRankingStatus.OOC_INVITED);
		} else {
			this.a.setIndividualEligibilityStatus(EligibleForIndividualRankingStatus.ELIGIBLE);
		}
	}

	private Category findActiveCategoryByName(String categoryName) {
		if (categoryName == null) {
			return null;
		}
		String trimmed = categoryName.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		for (String candidate : buildCategoryNameCandidates(trimmed)) {
			String catCode = Category.codeFromName(candidate);
			// logger.debug("candidate {} -> code {}", candidate, catCode);
			if (catCode != null) {
				Category category = RCompetition.getActiveCategories().get(catCode);
				if (category != null) {
					return category;
				}
			} else {
				// logger.debug("active categories do not contain candidate {}: [{}]", candidate, RCompetition.getActiveCategories().keySet());
			}
		}
		return null;
	}

	private LinkedHashSet<String> buildCategoryNameCandidates(String baseName) {
		LinkedHashSet<String> candidates = new LinkedHashSet<>();
		if (!baseName.contains("+")) {
			// + requires canonicalization
			candidates.add(baseName);
		}

		// if +number or number+ is present, add >number as candidate using regex replaceAll
		String nc = baseName.replaceAll("(\\d+)[+]", ">$1");
		if (!nc.contentEquals(baseName)) {
			candidates.add(nc);
		}
		nc = baseName.replaceAll("[+](\\d+)", ">$1");
		if (!nc.contentEquals(baseName)) {	
			candidates.add(nc);
		}
		logger.debug("candidates: {}",candidates);
		return candidates;
	}
}
