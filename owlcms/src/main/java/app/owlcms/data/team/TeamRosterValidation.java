package app.owlcms.data.team;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;

public final class TeamRosterValidation {

	private TeamRosterValidation() {
	}

	public static boolean isMember(Participation participation, Championship championship, Gender teamGender) {
		return teamGender == Gender.MF && championship.isExplicitMixedTeamMembers()
		        ? Boolean.TRUE.equals(participation.getMixedTeamMember())
		        : Boolean.TRUE.equals(participation.getTeamMember());
	}

	public static Result validate(Collection<Participation> participations, Championship championship,
	        Gender teamGender) {
		Map<Long, Integer> categoryCountsById = new LinkedHashMap<>();
		Map<Long, Category> categoriesById = new LinkedHashMap<>();
		int memberCount = 0;
		for (Participation participation : participations) {
			if (!isMember(participation, championship, teamGender)) {
				continue;
			}
			memberCount++;
			Category category = participation.getCategory();
			Long categoryId = category.getId();
			categoriesById.putIfAbsent(categoryId, category);
			categoryCountsById.merge(categoryId, 1, Integer::sum);
		}
		Map<Category, Integer> categoryCounts = new LinkedHashMap<>();
		categoryCountsById.forEach((categoryId, count) -> categoryCounts.put(categoriesById.get(categoryId), count));
		Integer teamLimit = teamGender == Gender.MF
		        ? (championship.isExplicitMixedTeamMembers() ? championship.getExplicitTeamSize() : null)
		        : championship.getMaxTeamSize();
		return new Result(memberCount, teamLimit, championship.getMaxPerCategory(), categoryCounts);
	}

	public record Result(int memberCount, Integer teamLimit, int categoryLimit,
	        Map<Category, Integer> categoryCounts) {

		public boolean isTeamOverLimit() {
			return teamLimit != null && teamLimit > 0 && memberCount > teamLimit;
		}

		public boolean isCategoryOverLimit(Category category) {
			return categoryCounts.entrySet().stream()
			        .filter(entry -> Objects.equals(entry.getKey().getId(), category.getId()))
			        .mapToInt(Map.Entry::getValue)
			        .sum() > categoryLimit;
		}
	}
}