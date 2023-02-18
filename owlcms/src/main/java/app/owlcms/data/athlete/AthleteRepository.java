/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athlete;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.slf4j.LoggerFactory;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.jpa.JPAService;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class AthleteRepository.
 */
public class AthleteRepository {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(AthleteRepository.class);
	static {
		logger.setLevel(Level.INFO);
	}

	public static void assignCategoryRanks() {
		JPAService.runInTransaction(em -> {
			// assign ranks to all groups.
			List<Athlete> l = AthleteSorter.assignCategoryRanks(null);
			for (Athlete a : l) {
				em.merge(a);
			}
			em.flush();
			return null;
		});
	}

	public static void assignStartNumbers(Group group) {
		JPAService.runInTransaction((em) -> {
			List<Athlete> currentGroupAthletes = AthleteRepository.doFindAllByGroupAndWeighIn(em, group, true,
			        (Gender) null);
			AthleteSorter.displayOrder(currentGroupAthletes);
			AthleteSorter.assignStartNumbers(currentGroupAthletes);
			return currentGroupAthletes;
		});
	}

	/**
	 * Count filtered.
	 *
	 * @param lastName    the last name
	 * @param group       the group
	 * @param ageDivision the age division
	 * @param weighedIn   the weighed in
	 * @return the int
	 */
	public static int countFiltered(String lastName, Group group, Category category, AgeGroup ageGroup,
	        AgeDivision ageDivision, Gender gender, Boolean weighedIn) {
		return JPAService.runInTransaction(em -> {
			return doCountFiltered(lastName, group, category, ageGroup, ageDivision, gender, weighedIn, em);
		});
	}

	public static long countTeamMembers(String curTeamName, Gender gender) {
		return JPAService.runInTransaction(em -> {
			Query query = em.createQuery(
			        "select count(distinct a.id) from Athlete a where a.team=:team and a.gender=:gender and a.eligibleForTeamRanking=true");
			query.setParameter("team", curTeamName);
			query.setParameter("gender", gender);
			return (long) query.getFirstResult();
		});
	}

	/**
	 * Delete an athlete
	 *
	 * @param Athlete the athlete
	 */
	public static void delete(Athlete Athlete) {
		JPAService.runInTransaction(em -> {
			em.remove(getById(Athlete.getId(), em));
			Competition.getCurrent().setRankingsInvalid(true);
			return null;
		});
	}

	public static Integer doCountFiltered(String lastName, Group group, Category category, AgeGroup ageGroup,
	        AgeDivision ageDivision, Gender gender, Boolean weighedIn, EntityManager em) {
		if (group != null && group.getName() == "*") {
			group = null;
		}
		String selection = filteringSelection(lastName, group, category, ageGroup, ageDivision, gender, weighedIn);
		String qlString = "select count(a.id) from Athlete a " + selection;
		logger.trace("count query = {}", qlString);
		Query query = em.createQuery(qlString);
		setFilteringParameters(lastName, group, category, ageGroup, ageDivision, gender, query);
		int i = ((Long) query.getSingleResult()).intValue();
		return i;
	}

	@SuppressWarnings("unchecked")
	public static List<Athlete> doFindAll(EntityManager em) {
		return em.createQuery("select distinct a from Athlete a").getResultList();
	}

	public static List<Athlete> doFindAllByAgeroupGroupAndWeighIn(EntityManager em, AgeGroup ageGroup, Group group,
	        Boolean weighedIn,
	        Gender gender) {
		if (group != null && group.getName() == "*") {
			group = null;
		}
		return doFindFiltered(em, (String) null, group, (Category) null, ageGroup, (AgeDivision) null, gender,
		        weighedIn,
		        -1, -1);
	}

	public static List<Athlete> doFindAllByGroupAndWeighIn(EntityManager em, Group group, Boolean weighedIn,
	        Gender gender) {
		if (group != null && group.getName() == "*") {
			group = null;
		}
		return doFindFiltered(em, (String) null, group, (Category) null, (AgeGroup) null, (AgeDivision) null, gender,
		        weighedIn,
		        -1, -1);
	}

	public static List<Athlete> doFindFiltered(EntityManager em, String lastName, Group group, Category category,
	        AgeGroup ageGroup,
	        AgeDivision ageDivision, Gender gender, Boolean weighedIn, int offset, int limit) {
		if (group != null && group.getName() == "*") {
			group = null;
		}
		String qlString = "select a from Athlete a"
		        + filteringSelection(lastName, group, category, ageGroup, ageDivision, gender, weighedIn)
		        + " order by a.category";
		// logger.debug("find query = {}", qlString);
		Query query = em.createQuery(qlString);
		setFilteringParameters(lastName, group, category, ageGroup, ageDivision, gender, query);
		if (offset >= 0) {
			query.setFirstResult(offset);
		}
		if (limit > 0) {
			query.setMaxResults(limit);
		}
		@SuppressWarnings("unchecked")
		List<Athlete> resultList = query.getResultList();
		return resultList;
	}

	/**
	 * @return the list of all athletes
	 */

	public static List<Athlete> findAll() {
		return JPAService.runInTransaction(em -> doFindAll(em));
	}

	/**
	 * Find all by group and weigh in.
	 *
	 * @param group     the group
	 * @param weighedIn the weighed in
	 * @return the list
	 */
	public static List<Athlete> findAllByGroupAndWeighIn(Group group, Boolean weighedIn) {
		List<Athlete> findFiltered = findFiltered((String) null, group, (Category) null, (AgeGroup) null,
		        (AgeDivision) null, (Gender) null, weighedIn,
		        -1, -1);
		logger.debug("findFiltered found {}", findFiltered.size());
		return findFiltered;
	}

	public static List<Athlete> findAllByGroupAndWeighIn(Group group, Gender gender, boolean weighedIn) {
		return JPAService.runInTransaction(em -> {
			return doFindAllByGroupAndWeighIn(em, group, weighedIn, gender);
		});
	}

	/**
	 * Fetch all athletes needed for leader board
	 *
	 * @param g
	 * @return
	 */
	public static List<Athlete> findAthletesForCategory(Category c) {
		return JPAService.runInTransaction((em) -> {
			TypedQuery<Athlete> q = em.createQuery(
			        "select distinct a from Athlete a join a.participations p join p.category c where c.id = :catId",
			        Athlete.class);
			q.setParameter("catId", c.getId());
			return q.getResultList();
		});
	}

	public static List<Athlete> findAthletesForGlobalRanking(EntityManager emgr, Group g) {
		return doFindAthletesForGlobalRanking(g, emgr);
	}

	/**
	 * Fetch all athletes and participations for the categories present in the
	 * group. If group is null, all athletes and their participations.
	 *
	 * @param g
	 * @return
	 */

	public static List<Athlete> findAthletesForGlobalRanking(Group g) {
		return JPAService.runInTransaction((em) -> {
			return doFindAthletesForGlobalRanking(g, em);
		});
	}

	public static Athlete findById(long id) {
		return JPAService.runInTransaction(em -> {
			return getById(id, em);
		});
	}

	/**
	 * Find filtered.
	 *
	 * @param lastName    the last name
	 * @param group       the group
	 * @param ageDivision the age division
	 * @param weighedIn   if weighed (bodyweight > 0)
	 * @param offset      the offset
	 * @param limit       the limit
	 * @return the list
	 */
	public static List<Athlete> findFiltered(String lastName, Group group, Category category, AgeGroup ageGroup,
	        AgeDivision ageDivision, Gender gender, Boolean weighedIn, int offset, int limit) {
		return JPAService.runInTransaction(em -> {
			return doFindFiltered(em, lastName, group, category, ageGroup, ageDivision, gender, weighedIn, offset,
			        limit);
		});
	}

	/**
	 * Gets the by id.
	 *
	 * @param id the id
	 * @param em the em
	 * @return the by id
	 */
	@SuppressWarnings("unchecked")
	public static Athlete getById(Long id, EntityManager em) {
		Query query = em.createQuery("select a from Athlete a where a.id=:id");
		query.setParameter("id", id);

		return (Athlete) query.getResultList().stream().findFirst().orElse(null);
	}

	/**
	 * Use the athlete bodyweight (or presumed body weight if weigh-in has not taken
	 * place) to determine category.
	 */
	public static void resetParticipations() {
		// logger.debug("recomputing eligibles");
		JPAService.runInTransaction(em -> {
			List<Athlete> athletes = AthleteRepository.doFindAll(em);
			for (Athlete a : athletes) {
				// do not clear category, required if no body weight
				// a.setCategory(null);
				a.setEligibleCategories(null);
				em.merge(a);
			}
			em.flush();
			Competition.getCurrent().setRankingsInvalid(true);
			return null;
		});
		// logger.debug("recomputing main cat");
		JPAService.runInTransaction(em -> {
			List<Athlete> athletes = AthleteRepository.doFindAll(em);
			for (Athlete a : athletes) {
				a.computeMainAndEligibleCategories();
				em.merge(a);
			}
			em.flush();
			Competition.getCurrent().setRankingsInvalid(true);
			return null;
		});
		assignCategoryRanks();
	}

	/**
	 * Save an athlete
	 *
	 * @param athlete the athlete
	 * @return the athlete
	 */
	public static Athlete save(Athlete athlete) {
		if (athlete == null) {
			return athlete;
		}
		return JPAService.runInTransaction((em) -> {
			Competition.getCurrent().setRankingsInvalid(true);
			Athlete merged = em.merge(athlete);
			return merged;
		});
	}

	private static List<Athlete> doFindAthletesForGlobalRanking(Group g, EntityManager em) {
		String onlyCategoriesFromCurrentGroup = "";
		if (g != null) {
			String categoriesFromCurrentGroup = "select distinct c2 from Athlete b join b.group g join b.participations p join p.category c2 where g.id = :groupId";
			onlyCategoriesFromCurrentGroup = " join p.category c where exists (" + categoriesFromCurrentGroup
			        + " and c2.id = c.id)";
//                Query q2 = em.createQuery(categoriesFromCurrentGroup);
//                q2.setParameter("groupId", g.getId());
//                List<Category> q2Results = q2.getResultList();
//                logger.debug("categories for currentGroup {}",q2Results);
		}
		Query q = em.createQuery(
		        "select distinct a, p from Athlete a join fetch a.participations p"
		                + onlyCategoriesFromCurrentGroup);
		if (g != null) {
			q.setParameter("groupId", g.getId());
		}

		@SuppressWarnings("unchecked")
		List<Athlete> resultList = (List<Athlete>) q.getResultList().stream().filter(a -> {
			Double bw = ((Athlete) a).getBodyWeight();
			return bw != null && bw >= 0.01;
		}).collect(Collectors.toList());
		return resultList;
	}

	private static String filteringJoins(Group group, Category category, AgeGroup ageGroup, AgeDivision ageDivision) {
		List<String> fromList = new LinkedList<>();
		if (group != null) {
			fromList.add("join a.group g"); // group is via a relationship, join on id
		}
		if (category != null || ageGroup != null || ageDivision != null) {
			fromList.add("join a.category c"); // group is via a relationship, join on id
		}
		if (fromList.size() == 0) {
			return "";
		} else {
			return String.join(" ", fromList);
		}
	}

	private static String filteringSelection(String lastName, Group group, Category category, AgeGroup ageGroup,
	        AgeDivision ageDivision, Gender gender,
	        Boolean weighedIn) {
		String joins = filteringJoins(group, category, ageGroup, ageDivision);
		String where = filteringWhere(lastName, group, category, ageGroup, ageDivision, gender, weighedIn);
		String selection = (joins != null ? " " + joins : "") + (where != null ? " where " + where : "");
		return selection;
	}

	private static String filteringWhere(String lastName, Group group, Category category, AgeGroup ageGroup,
	        AgeDivision ageDivision, Gender gender,
	        Boolean weighedIn) {
		List<String> whereList = new LinkedList<>();
		if (ageGroup != null) {
			whereList.add("c.ageGroup = :ageGroup");
		}
		if (ageDivision != null) {
			whereList.add("c.ageGroup.ageDivision = :division");
		}
		if (group != null) {
			whereList.add("g.id = :groupId"); // group is via a relationship, select the joined id.
		}
		if (category != null) {
			whereList.add("c.id = :categoryId"); // category is via a relationship, select the joined id.
		}
		if (lastName != null && lastName.trim().length() > 0) {
			whereList.add("lower(a.lastName) like :lastName");
		}
		if (gender != null) {
			whereList.add("a.gender = :gender");
		}
		if (weighedIn != null) {
			whereList.add(weighedIn ? "a.bodyWeight > 0" : "(a.bodyWeight is null) OR (a.bodyWeight <= 0.1)");
		}
		if (whereList.size() == 0) {
			return null;
		} else {
			String join = String.join(" and ", whereList);
			return join;
		}
	}

	private static void setFilteringParameters(String lastName, Group group, Category category, AgeGroup ageGroup,
	        AgeDivision ageDivision, Gender gender,
	        Query query) {
		if (lastName != null && lastName.trim().length() > 0) {
			// starts with
			query.setParameter("lastName", lastName.toLowerCase() + "%");
		}
		if (group != null) {
			query.setParameter("groupId", group.getId()); // group is via a relationship, we join and select on id.
		}
		if (category != null) {
			query.setParameter("categoryId", category.getId()); // category is via a relationship, we join and select on
			                                                    // id.
		}
		if (ageGroup != null) {
			query.setParameter("ageGroup", ageGroup);
		}
		if (ageDivision != null) {
			query.setParameter("division", ageDivision);
		}
		if (gender != null) {
			query.setParameter("gender", gender);
		}
	}
}
