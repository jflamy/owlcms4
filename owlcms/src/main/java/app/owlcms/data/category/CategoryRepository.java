/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.category;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.LoggerFactory;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class CategoryRepository.
 *
 */
public class CategoryRepository {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(CategoryRepository.class);
	private static Map<String, Category> allCategories = new TreeMap<>();

	static {
		logger.setLevel(Level.INFO);
	}

	/**
	 * Count filtered.
	 *
	 * @param name        the name
	 * @param ageDivision the age division
	 * @param active      active category
	 * @return the int
	 */
	public static int countFiltered(String name, Championship ageDivision, AgeGroup ageGroup, Gender gender,
	        Integer age,
	        Double bodyWeight, Boolean active) {
		return JPAService.runInTransaction(em -> {
			return doCountFiltered(name, gender, ageDivision, ageGroup, age, bodyWeight, active, em);
		});
	}

	public static int countParticipations() {
		return JPAService.runInTransaction((em) -> {
			String qlString = "select count(p) from Participation p";
			Query query = em.createQuery(qlString);
			int i = ((Long) query.getSingleResult()).intValue();
			return i;
		});
	}

	/**
	 * Delete.
	 *
	 * @param Category the category
	 */
	public static void delete(Category Category) {
		JPAService.runInTransaction(em -> {
			em.remove(getById(Category.getId(), em));
			return null;
		});
	}

	public static Integer doCountFiltered(String name, Gender gender, Championship ageDivision, AgeGroup ageGroup,
	        Integer age, Double bodyWeight, Boolean active, EntityManager em) {
		String selection = filteringSelection(name, gender, ageDivision, ageGroup, age, bodyWeight, active);
		String qlString = "select count(c.id) from Category c " + selection;
		logger.trace("count = {}", qlString);
		Query query = em.createQuery(qlString);
		setFilteringParameters(name, gender, ageDivision, ageGroup, age, bodyWeight, true, query);
		int i = ((Long) query.getSingleResult()).intValue();
		return i;
	}

	@SuppressWarnings("unchecked")
	public static Category doFindByCode(String code, EntityManager em) {
		Query query;
		if (code != null) {
			query = em.createQuery("select c from Category c where lower(code) = lower(:string)");
			query.setParameter("string", code);
		} else {
			return null;
		}
		return (Category) query.getResultList().stream().findFirst().orElse(null);
	}

	@SuppressWarnings("unchecked")
	public static Category doFindByName(String string, EntityManager em) {
		Query query = em.createQuery("select c from Category c where lower(name) = lower(:string) order by c.name");
		query.setParameter("string", string);
		return (Category) query.getResultList().stream().findFirst().orElse(null);
	}

	public static List<Category> doFindEligibleCategories(Athlete a, Gender gender, Integer ageFromFields, Double bw,
	        int qualifyingTotal) {
		
		List<Category> allEligible = CategoryRepository.findByGenderAgeBW(gender, ageFromFields, null);
		if (logger.isEnabledFor(Level.TRACE) && a.getLastName().contentEquals("Molnar")) {
			logger.trace("allEligible bw={} {} -- {}", bw, allEligible.size(), LoggerUtils.whereFrom());
		}

		// if youth F >81, athlete may be jr87 or jr>87;
		allEligible = checkMultipleBWClasses(gender, ageFromFields, bw, allEligible);
		allEligible.sort(new RegistrationPreferenceComparator());

		allEligible = allEligible.stream()
		        .filter(c -> (qualifyingTotal >= c.getQualifyingTotal()))
		        .filter(c -> (bw == null || (bw > c.getMinimumWeight() && bw <= c.getMaximumWeight())))
		        .collect(Collectors.toList());
		return allEligible;
	}

	private static List<Category> checkMultipleBWClasses(Gender gender, Integer ageFromFields, Double bw,
	        List<Category> allEligible) {
		// > 998 is our signal for max weight in category
		if ((bw != null && bw > 998) && !allEligible.isEmpty()) {
			double bodyWeight = allEligible.get(0).getMinimumWeight() + 1;
			List<Category> otherEligibles = CategoryRepository.findByGenderAgeBW(gender, ageFromFields, bodyWeight);
			HashSet<Category> allEligibleSet = new HashSet<>(allEligible);
			for (Category otherEligible : otherEligibles) {
				if (!otherEligible.sameAsAny(allEligibleSet)) {
					allEligible.add(otherEligible);
				}
			}
			allEligible = allEligible.stream()
			        .collect(Collectors.toList());
		}
		return allEligible;
	}

	public static List<Category> doFindFiltered(EntityManager em, String name, Gender gender, Championship ageDivision,
	        AgeGroup ageGroup, Integer age, Double bodyWeight, Boolean active, int offset, int limit) {
		String qlString = "select c from Category c"
		        + filteringSelection(name, gender, ageDivision, ageGroup, age, bodyWeight, active)
		        + " order by c.ageGroup.ageDivision, c.gender, c.ageGroup.minAge, c.ageGroup.maxAge, c.ageGroup, c.maximumWeight";
		logger.trace("query = {}", qlString);

		Query query = em.createQuery(qlString);
		setFilteringParameters(name, gender, ageDivision, ageGroup, age, bodyWeight, active, query);
		if (offset >= 0) {
			query.setFirstResult(offset);
		}
		if (limit > 0) {
			query.setMaxResults(limit);
		}
		@SuppressWarnings("unchecked")
		List<Category> resultList = query.getResultList();
		logger.debug("resultList {}", resultList);
		return resultList;
	}

	/**
	 * @return active categories
	 */
	public static List<Category> findActive() {

		List<Category> findFiltered = findFiltered((String) null, (Gender) null, (Championship) null, (AgeGroup) null,
		        (Integer) null, (Double) null,
		        true, -1, -1);
		
		//logger.debug("findActive **** {} {}", findFiltered.size(), LoggerUtils.stackTrace());
		findFiltered.sort(new RegistrationPreferenceComparator());
		return findFiltered;
	}

	public static Collection<Category> findActive(Gender gender, Double bodyWeight) {
		List<Category> findFiltered = findFiltered((String) null, gender, (Championship) null, (AgeGroup) null,
		        (Integer) null,
		        bodyWeight, true, -1, -1);
		// sort comparison to put more specific category age before. M30 before O21, O21
		// also before SR (MASTERS, then
		// U, then IWF/other)
		findFiltered.sort(new RegistrationPreferenceComparator());
		return findFiltered;
	}

	/**
	 * Find all.
	 *
	 * @return the list
	 */
	@SuppressWarnings("unchecked")
	public static List<Category> findAll() {
		return JPAService
		        .runInTransaction(em -> em.createQuery("select c from Category c order by c.name").getResultList());
	}

	/**
	 * Find by code.
	 *
	 * @param string the code
	 * @return the category
	 */
	public static Category findByCode(String string) {
		return JPAService.runInTransaction(em -> {
			return doFindByCode(string, em);
		});
	}

	public static List<Category> findByGenderAgeBW(Gender gender, Integer age, Double bodyWeight) {
		Boolean active = true;
		List<Category> findFiltered = findFiltered((String) null, gender, (Championship) null, (AgeGroup) null, age,
		        bodyWeight, active, -1, -1);
		// sort comparison to put more specific category age before. M30 before O21, O21
		// also before SR (MASTERS, then
		// U, then IWF/other)
		findFiltered = findFiltered.stream().filter(c -> c.getAgeGroup().isActive())
		        .sorted(new RegistrationPreferenceComparator()).collect(Collectors.toList());
		return findFiltered;
	}

	public static List<Category> findByGenderDivisionAgeBW(Gender gender, Championship ageDivision, Integer age,
	        Double bodyWeight) {
		Boolean active = true;
		List<Category> findFiltered = findFiltered((String) null, gender, ageDivision, (AgeGroup) null, age, bodyWeight,
		        active, -1, -1);
		findFiltered = findFiltered.stream().filter(c -> c.getAgeGroup().isActive())
		        .sorted(new RegistrationPreferenceComparator()).collect(Collectors.toList());
		return findFiltered;
	}

	/**
	 * Find by name.
	 *
	 * @param string the string
	 * @return the category
	 */
	// @Deprecated
	// public static Category findByName(String string) {
	// return JPAService.runInTransaction(em -> {
	// return doFindByName(string, em);
	// });
	// }

	/**
	 * Find filtered.
	 *
	 * @param name        the last name
	 * @param ageDivision the age division
	 * @param active      if category is active
	 * @param offset      the offset
	 * @param limit       the limit
	 * @return the list
	 */
	public static List<Category> findFiltered(String name, Gender gender, Championship ageDivision, AgeGroup ageGroup,
	        Integer age, Double bodyWeight, Boolean active, int offset, int limit) {
		return JPAService.runInTransaction(em -> {
			List<Category> doFindFiltered = doFindFiltered(em, name, gender, ageDivision, ageGroup, age, bodyWeight,
			        active, offset, limit);
			// logger.trace("found {} searching for {} {} {} {} {}", doFindFiltered.size(),
			// gender, ageDivision, age,
			// bodyWeight, active);
			return doFindFiltered;
		});
	}

	/**
	 * Find all.
	 *
	 * @return the list
	 */
	@SuppressWarnings("unchecked")
	public static List<Category> findNullCodes() {
		return JPAService
		        .runInTransaction(
		                em -> em.createQuery("select c from Category c where c.code is null").getResultList());
	}

	public static void fixNullCodes(List<Category> nullCodeCategories) {
		JPAService.runInTransaction(em -> {
			for (Category c : nullCodeCategories) {
				// recomputes the code and the name
				save(c);
			}
			em.flush();
			return null;
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
	public static Category getById(Long id, EntityManager em) {
		Query query = em.createQuery("select u from Category u where u.id=:id");
		query.setParameter("id", id);

		return (Category) query.getResultList().stream().findFirst().orElse(null);
	}

	/**
	 * Save.
	 *
	 * @param category the category
	 * @return the category
	 */
	public static Category save(Category category) {
		return JPAService.runInTransaction(em -> {
			// code must match inside info for string-based matches in db.
			category.setCode(category.getComputedCode());
			category.setName(category.getDisplayName());
			return em.merge(category);
		});
	}

	private static String filteringJoins(AgeGroup ag, Integer age) {
		List<String> fromList = new LinkedList<>();
		// if (ag != null || age != null) {
		fromList.add("join c.ageGroup ag"); // group is via a relationship, join on id
		// }
		if (fromList.size() == 0) {
			return "";
		} else {
			return String.join(" ", fromList);
		}
	}

	private static String filteringSelection(String name, Gender gender, Championship ageDivision, AgeGroup ageGroup,
	        Integer age, Double bodyWeight, Boolean active) {
		String joins = filteringJoins(ageGroup, age);
		String where = filteringWhere(name, ageDivision, ageGroup, age, bodyWeight, gender, active);
		String selection = (joins != null ? " " + joins : "") + (where != null ? " where " + where : "");
		return selection;
	}

	private static String filteringWhere(String name, Championship ageDivision, AgeGroup ageGroup, Integer age,
	        Double bodyWeight, Gender gender, Boolean active) {
		List<String> whereList = new LinkedList<>();
		if (ageDivision != null) {
			whereList.add("((ag.ageDivision = :championshipName) or (ag.championshipName = :championshipName))");
		}
		if (name != null && name.trim().length() > 0) {
			whereList.add("lower(c.name) like :name");
		}
		if (active != null && active) {
			// must be active in an active age group
			// whereList.add("(c.active = :active) and (ag.active = :active)");
			whereList.add("(ag.active = :active)");
		}
		if (gender != null) {
			whereList.add("c.gender = :gender");
		}
		// because there is exactly one ageGroup following could be done with
		// c.ageGroup.id = :ageGroupId
		if (ageGroup != null) {
			whereList.add("ag.id = :ageGroupId"); // group is via a relationship, select the joined id.
		}
		// because there is exactly one ageGroup following could test on
		// c.ageGroup.minAge and maxAge
		if (age != null) {
			whereList.add("(ag.minAge <= :age) and (ag.maxAge >= :age)");
		}
		if (bodyWeight != null) {
			whereList.add("(c.minimumWeight < :bodyWeight) and (c.maximumWeight >= :bodyWeight)");
		}
		if (whereList.size() == 0) {
			return null;
		} else {
			String join = String.join(" and ", whereList);
			return join;
		}
	}

	private static void setFilteringParameters(String name, Gender gender, Championship championship, AgeGroup ageGroup,
	        Integer age, Double bodyWeight, Boolean active, Query query) {
		if (name != null && name.trim().length() > 0) {
			// starts with
			query.setParameter("name", "%" + name.toLowerCase() + "%");
		}
		if (ageGroup != null) {
			// group is via a relationship, we join and select on id
			query.setParameter("ageGroupId", ageGroup.getId());
		}
		if (active != null && active) {
			query.setParameter("active", active);
		}
		if (age != null) {
			query.setParameter("age", age);
		}
		if (bodyWeight != null) {
			query.setParameter("bodyWeight", bodyWeight);
		}
		if (championship != null) {
			query.setParameter("championshipName", championship.getName()); // ageDivision is a string
		}
		if (gender != null) {
			query.setParameter("gender", gender);
		}
	}

	public static void resetCodeMap() {
		synchronized (allCategories) {
			findActive().stream()
			//.peek(c -> logger.debug("adding {} + {}", c.getDisplayName(), c.getNameWithAgeGroup()))
			.forEach(c -> {
				allCategories.put(c.getDisplayName(), c);
				allCategories.put(c.getNameWithAgeGroup(), c);
			});
		}
	}

	public static Category codeFromName(String catName) {
		synchronized (allCategories) {
			return allCategories.get(catName.trim());
		}
	}
}
