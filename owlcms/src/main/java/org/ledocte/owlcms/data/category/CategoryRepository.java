package org.ledocte.owlcms.data.category;

import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.ledocte.owlcms.data.AgeDivision;
import org.ledocte.owlcms.data.Gender;
import org.ledocte.owlcms.data.jpa.JPAService;

/**
 * @author Alejandro Duarte
 */
public class CategoryRepository {

	@SuppressWarnings("unchecked")
	public static List<Category> findAll() {
		return JPAService.runInTransaction(em -> em.createQuery("select c from Category c")
			.getResultList());
	}

	public static Category save(Category Category) {
		return JPAService.runInTransaction(em -> em.merge(Category));
	}

	public static void delete(Category Category) {
		JPAService.runInTransaction(em -> {
			em.remove(getById(Category.getId(), em));
			return null;
		});
	}



	static void insertKidsCategories(AgeDivision curAG, boolean active) {
		save(new Category(0.0, 35.0, Gender.F, active, curAG, 0));
		save(new Category(35.0, 40.0, Gender.F, active, curAG, 0));
		save(new Category(40.0, 45.0, Gender.F, active, curAG, 0));
		save(new Category(45.0, 49.0, Gender.F, active, curAG, 0));
		save(new Category(49.0, 55.0, Gender.F, active, curAG, 0));
		save(new Category(55.0, 59.0, Gender.F, active, curAG, 0));
		save(new Category(59.0, 64.0, Gender.F, active, curAG, 0));
		save(new Category(64.0, 71.0, Gender.F, active, curAG, 0));
		save(new Category(71.0, 76.0, Gender.F, active, curAG, 0));
		save(new Category(76.0, 999.0, Gender.F, active, curAG, 0));

		save(new Category(0.0, 44.0, Gender.M, active, curAG, 0));
		save(new Category(44.0, 49.0, Gender.M, active, curAG, 0));
		save(new Category(49.0, 55.0, Gender.M, active, curAG, 0));
		save(new Category(55.0, 61.0, Gender.M, active, curAG, 0));
		save(new Category(61.0, 67.0, Gender.M, active, curAG, 0));
		save(new Category(67.0, 73.0, Gender.M, active, curAG, 0));
		save(new Category(73.0, 81.0, Gender.M, active, curAG, 0));
		save(new Category(81.0, 89.0, Gender.M, active, curAG, 0));
		save(new Category(89.0, 96.0, Gender.M, active, curAG, 0));
		save(new Category(96.0, 999.0, Gender.M, active, curAG, 0));
	}

	private static void insertNewCategories(AgeDivision curAG, boolean active) {
		save(new Category(0.0, 45.0, Gender.F, active, curAG, 191));
		save(new Category(45.0, 49.0, Gender.F, active, curAG, 203));
		save(new Category(49.0, 55.0, Gender.F, active, curAG, 221));
		save(new Category(55.0, 59.0, Gender.F, active, curAG, 232));
		save(new Category(59.0, 64.0, Gender.F, active, curAG, 245));
		save(new Category(64.0, 71.0, Gender.F, active, curAG, 261));
		save(new Category(71.0, 76.0, Gender.F, active, curAG, 272));
		save(new Category(76.0, 81.0, Gender.F, active, curAG, 283));
		save(new Category(81.0, 87.0, Gender.F, active, curAG, 294));
		save(new Category(87.0, 999.0, Gender.F, active, curAG, 320));

		save(new Category(0.0, 55.0, Gender.M, active, curAG, 293));
		save(new Category(55.0, 61.0, Gender.M, active, curAG, 312));
		save(new Category(61.0, 67.0, Gender.M, active, curAG, 331));
		save(new Category(67.0, 73.0, Gender.M, active, curAG, 348));
		save(new Category(73.0, 81.0, Gender.M, active, curAG, 368));
		save(new Category(81.0, 89.0, Gender.M, active, curAG, 387));
		save(new Category(89.0, 96.0, Gender.M, active, curAG, 401));
		save(new Category(96.0, 102.0, Gender.M, active, curAG, 412));
		save(new Category(102.0, 109.0, Gender.M, active, curAG, 424));
		save(new Category(109.0, 999.0, Gender.M, active, curAG, 453));
	}

	public static void insertStandardCategories() {
		if (findAll().size() == 0) {
			insertNewCategories(AgeDivision.DEFAULT, true);
			insertNewCategories(AgeDivision.SENIOR, false);
			insertNewCategories(AgeDivision.JUNIOR, false);
			insertYouthCategories(AgeDivision.YOUTH, false);
			insertYouthCategories(AgeDivision.KIDS, false);
			insertTraditionalCategories(AgeDivision.TRADITIONAL, false);
		}
	}

	private static void insertTraditionalCategories(AgeDivision curAG, boolean active) {
		save(new Category(0.0, 40.0, Gender.F, false, curAG, 0));
		save(new Category(0.0, 44.0, Gender.F, false, curAG, 0));
		save(new Category(0.0, 48.0, Gender.F, active, curAG, 217));
		save(new Category(48.0, 53.0, Gender.F, active, curAG, 233));
		save(new Category(53.0, 58.0, Gender.F, active, curAG, 252));
		save(new Category(63.0, 69.0, Gender.F, active, curAG, 262));
		save(new Category(58.0, 63.0, Gender.F, active, curAG, 276));
		save(new Category(69.0, 75.0, Gender.F, active, curAG, 296));
		save(new Category(75.0, 90.0, Gender.F, active, curAG, 283));
		save(new Category(90.0, 999.0, Gender.F, active, curAG, 348));
		save(new Category(63.0, 999.0, Gender.F, false, curAG, 0));
		save(new Category(69.0, 999.0, Gender.F, false, curAG, 0));
		save(new Category(75.0, 999.0, Gender.F, false, curAG, 0));

		save(new Category(0.0, 46.0, Gender.M, false, curAG, 0));
		save(new Category(0.0, 51.0, Gender.M, false, curAG, 0));
		save(new Category(0.0, 56.0, Gender.M, active, curAG, 307));
		save(new Category(56.0, 62.0, Gender.M, active, curAG, 333));
		save(new Category(62.0, 69.0, Gender.M, active, curAG, 359));
		save(new Category(69.0, 77.0, Gender.M, active, curAG, 380));
		save(new Category(77.0, 85.0, Gender.M, active, curAG, 396));
		save(new Category(85.0, 94.0, Gender.M, active, curAG, 417));
		save(new Category(94.0, 105.0, Gender.M, active, curAG, 437));
		save(new Category(105.0, 999.0, Gender.M, active, curAG, 477));
		save(new Category(77.0, 999.0, Gender.M, false, curAG, 0));
		save(new Category(85.0, 999.0, Gender.M, false, curAG, 0));
		save(new Category(94.0, 999.0, Gender.M, false, curAG, 0));
	}

	static void insertYouthCategories(AgeDivision curAG, boolean active) {
		save(new Category(0.0, 40.0, Gender.F, active, curAG, 0));
		save(new Category(40.0, 45.0, Gender.F, active, curAG, 0));
		save(new Category(45.0, 49.0, Gender.F, active, curAG, 0));
		save(new Category(49.0, 55.0, Gender.F, active, curAG, 0));
		save(new Category(55.0, 59.0, Gender.F, active, curAG, 0));
		save(new Category(59.0, 64.0, Gender.F, active, curAG, 0));
		save(new Category(64.0, 71.0, Gender.F, active, curAG, 0));
		save(new Category(71.0, 76.0, Gender.F, active, curAG, 0));
		save(new Category(76.0, 81.0, Gender.F, active, curAG, 0));
		save(new Category(81.0, 999.0, Gender.F, active, curAG, 0));

		save(new Category(0.0, 49.0, Gender.M, active, curAG, 0));
		save(new Category(49.0, 55.0, Gender.M, active, curAG, 0));
		save(new Category(55.0, 61.0, Gender.M, active, curAG, 0));
		save(new Category(61.0, 67.0, Gender.M, active, curAG, 0));
		save(new Category(67.0, 73.0, Gender.M, active, curAG, 0));
		save(new Category(73.0, 81.0, Gender.M, active, curAG, 0));
		save(new Category(81.0, 89.0, Gender.M, active, curAG, 0));
		save(new Category(89.0, 96.0, Gender.M, active, curAG, 0));
		save(new Category(96.0, 102.0, Gender.M, active, curAG, 0));
		save(new Category(102.0, 999.0, Gender.M, active, curAG, 0));
	}

	@SuppressWarnings("unchecked")
	public static Category getById(Long id, EntityManager em) {
		Query query = em.createQuery("select u from Category u where u.id=:id");
		query.setParameter("id", id);

		return (Category) query.getResultList()
			.stream()
			.findFirst()
			.orElse(null);
	}
	
	private static String byAgeDivision = "from Category c where c.ageDivision = :division";

	@SuppressWarnings("unchecked")
	public static Collection<Category> findByAgeDivision(AgeDivision ageDivision, int offset, int limit) {
		if (ageDivision == null) {
			return JPAService.runInTransaction(em -> {
				Query query = em.createQuery("select c from Category c");
				query.setFirstResult(offset);
				query.setMaxResults(limit);
				return query.getResultList();
			});
		} else {
			return JPAService.runInTransaction(em -> {
				Query query = em.createQuery("select c " + byAgeDivision);
				query.setParameter("division", ageDivision);
				query.setFirstResult(offset);
				query.setMaxResults(limit);
				List<Category> resultList = query.getResultList();
				return resultList;
			});
		}
	}

	public static int countByAgeDivision(AgeDivision ageDivision) {
		if (ageDivision == null) {
			return JPAService.runInTransaction(em -> {
				Query query = em.createQuery("select count(c.id) from Category c");
				int i = ((Long) query.getSingleResult()).intValue();
				return i;
			});
		} else {
			return JPAService.runInTransaction(em -> {
				Query query = em.createQuery("select count(c.id) " + byAgeDivision);
				query.setParameter("division", ageDivision);
				int i = ((Long) query.getSingleResult()).intValue();
				return i;
			});
		}
	}

}
