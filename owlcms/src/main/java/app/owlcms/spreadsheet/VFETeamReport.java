package app.owlcms.spreadsheet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.util.WorkbookUtil;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Participation;
import app.owlcms.data.team.TeamRosterValidation;
import app.owlcms.i18n.Translator;

public final class VFETeamReport {

	private VFETeamReport() {
	}

	public record Scope(String key, String label, Championship championship) {
	}

	public record Membership(String category, boolean selected, boolean warning, boolean teamWarning) {
	}

	public static final class Member {
		private final Athlete athlete;
		private final String key;
		private final List<Membership> memberships;

		Member(Athlete athlete, String key, List<Membership> memberships) {
			this.athlete = athlete;
			this.key = key;
			this.memberships = memberships;
		}

		public Athlete getAthlete() { return this.athlete; }
		public String getKey() { return this.key; }
		public List<Membership> getMemberships() { return this.memberships; }
	}

	public static final class Block {
		private final Gender gender;
		private final List<Member> members = new ArrayList<>();
		private final List<String> violations = new ArrayList<>();

		Block(Gender gender) { this.gender = gender; }
		public String getTitle() {
			return Translator.translate(this.gender == Gender.F ? "Gender.Women"
			        : this.gender == Gender.M ? "Gender.Men" : "Gender.Mixed");
		}
		public List<Member> getMembers() { return this.members; }
		public List<String> getViolations() { return this.violations; }
	}

	public static final class Delegation {
		private final String name;
		private final String sheetName;
		private final List<Block> blocks = new ArrayList<>();

		Delegation(String name, String sheetName) { this.name = name; this.sheetName = sheetName; }
		public String getName() { return this.name; }
		public String getSheetName() { return this.sheetName; }
		public List<Block> getBlocks() { return this.blocks; }
	}

	public static final class Data {
		private final List<Scope> scopes = new ArrayList<>();
		private final List<Delegation> teams = new ArrayList<>();
		private final Map<String, Member> rows = new LinkedHashMap<>();

		public List<Scope> getScopes() { return this.scopes; }
		public List<Delegation> getTeams() { return this.teams; }
		public Map<String, Member> getRows() { return this.rows; }
	}

	private static String scopeKey(AgeGroup ageGroup) {
		return ageGroup.getChampionship().getName() + "|" + ageGroup.getCode();
	}

	private static String scopeLabel(AgeGroup ageGroup) {
		String championshipName = ageGroup.getChampionship().getName();
		String ageGroupCode = ageGroup.getCode();
		return championshipName.equalsIgnoreCase(ageGroupCode)
		        ? ageGroupCode
		        : championshipName + " / " + ageGroupCode;
	}

	public static Data build(List<Athlete> athletes) {
		Data data = new Data();
		Map<String, Scope> scopes = new LinkedHashMap<>();
		Map<String, List<Athlete>> teams = new LinkedHashMap<>();
		for (Athlete athlete : athletes) {
			String team = athlete.getTeam();
			if (team == null || team.isBlank()) {
				continue;
			}
			teams.computeIfAbsent(team, ignored -> new ArrayList<>()).add(athlete);
			for (Participation participation : athlete.getParticipations()) {
				AgeGroup ageGroup = participation.getCategory().getAgeGroup();
				if (ageGroup == null) {
					continue;
				}
				String key = scopeKey(ageGroup);
				scopes.putIfAbsent(key, new Scope(key, scopeLabel(ageGroup), ageGroup.getChampionship()));
			}
		}
		data.scopes.addAll(scopes.values());
		for (Map.Entry<String, List<Athlete>> team : teams.entrySet()) {
			String suffix = "-" + (data.teams.size() + 1);
			String safeName = WorkbookUtil.createSafeSheetName(team.getKey());
			String sheetName = safeName.substring(0, Math.min(safeName.length(), 31 - suffix.length())) + suffix;
			Delegation delegation = new Delegation(team.getKey(), sheetName);
			for (Gender gender : List.of(Gender.F, Gender.M, Gender.MF)) {
				if (gender == Gender.MF && data.scopes.stream().noneMatch(scope -> scope.championship().isMixedTeamEnabled())) {
					continue;
				}
				List<Athlete> blockAthletes = team.getValue().stream()
				        .filter(athlete -> gender == Gender.MF || athlete.getGender() == gender).toList();
				if (blockAthletes.isEmpty()) {
					continue;
				}
				Block block = new Block(gender);
				Map<String, TeamRosterValidation.Result> validations = new LinkedHashMap<>();
				Map<String, TeamRosterValidation.Result> genderValidations = new LinkedHashMap<>();
				for (Scope scope : data.scopes) {
					if (gender == Gender.MF && !scope.championship().isMixedTeamEnabled()) {
						continue;
					}
					List<Participation> participations = blockAthletes.stream()
					        .flatMap(athlete -> athlete.getParticipations().stream())
					        .filter(participation -> participation.getCategory().getAgeGroup() != null)
					        .filter(participation -> scope.key().equals(scopeKey(participation.getCategory().getAgeGroup())))
					        .toList();
					TeamRosterValidation.Result result = TeamRosterValidation.validate(participations, scope.championship(), gender);
					validations.put(scope.key(), result);
					result.categoryCounts().forEach((category, count) -> {
						if (result.isCategoryOverLimit(category)) {
							block.violations.add(Translator.translate("TeamRoster.CategoryViolation", scope.label(),
							        category.getNameWithAgeGroup(), count, result.categoryLimit()));
						}
					});
					if (result.isTeamOverLimit()) {
						block.violations.add(Translator.translate("TeamRoster.TeamViolation", scope.label(),
						        result.memberCount(), result.teamLimit()));
					}
					if (gender == Gender.MF && !scope.championship().isExplicitMixedTeamMembers()) {
						// sum-of-teams mixed roster: each gender's team limit still applies
						for (Gender memberGender : List.of(Gender.F, Gender.M)) {
							List<Participation> genderParticipations = participations.stream()
							        .filter(participation -> participation.getAthlete().getGender() == memberGender)
							        .toList();
							TeamRosterValidation.Result genderResult = TeamRosterValidation.validate(genderParticipations,
							        scope.championship(), memberGender);
							genderValidations.put(scope.key() + "|" + memberGender, genderResult);
							if (genderResult.isTeamOverLimit()) {
								block.violations.add(Translator.translate("TeamRoster.TeamViolation",
								        scope.label() + " - " + new Block(memberGender).getTitle(),
								        genderResult.memberCount(), genderResult.teamLimit()));
							}
						}
					}
				}
				for (Athlete athlete : blockAthletes) {
					List<Membership> memberships = new ArrayList<>();
					for (Scope scope : data.scopes) {
						TeamRosterValidation.Result result = validations.get(scope.key());
						List<Participation> eligible = result == null ? List.of() : athlete.getParticipations().stream()
						        .filter(participation -> participation.getCategory().getAgeGroup() != null)
						        .filter(participation -> scope.key().equals(scopeKey(participation.getCategory().getAgeGroup())))
						        .toList();
						List<Participation> selected = eligible.stream()
						        .filter(participation -> TeamRosterValidation.isMember(participation, scope.championship(), gender))
						        .toList();
						String categories = eligible.stream().map(participation -> participation.getCategory().getNameWithAgeGroup())
						        .collect(Collectors.joining("; "));
						boolean warning = selected.stream().anyMatch(participation -> result.isCategoryOverLimit(participation.getCategory()));
						TeamRosterValidation.Result genderResult = genderValidations.get(scope.key() + "|" + athlete.getGender());
						boolean teamWarning = !selected.isEmpty() && (result.isTeamOverLimit()
						        || (genderResult != null && genderResult.isTeamOverLimit()));
						memberships.add(new Membership(categories, !selected.isEmpty(), warning, teamWarning));
					}
					if (gender == Gender.MF && memberships.stream().noneMatch(Membership::selected)) {
						continue;
					}
					String key = "VFE_ROW_" + data.rows.size();
					Member member = new Member(athlete, key, memberships);
					block.members.add(member);
					data.rows.put(key, member);
				}
				if (!block.members.isEmpty()) {
					delegation.blocks.add(block);
				}
			}
			data.teams.add(delegation);
		}
		return data;
	}
}