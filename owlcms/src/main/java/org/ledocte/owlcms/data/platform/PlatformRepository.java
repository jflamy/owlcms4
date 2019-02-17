/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Affero GNU License amended with the
 * Commons Clause.
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.data.platform;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.ledocte.owlcms.data.jpa.JPAService;

/**
 * The Class PlatformRepository.
 *
 * @author Alejandro Duarte
 */
public class PlatformRepository {

	/**
	 * Gets the by id.
	 *
	 * @param id the id
	 * @param em the em
	 * @return the by id
	 */
	@SuppressWarnings("unchecked")
	public static Platform getById(Long id, EntityManager em) {
		Query query = em.createQuery("select u from Platform u where u.id=:id");
		query.setParameter("id", id);

		return (Platform) query.getResultList()
			.stream()
			.findFirst()
			.orElse(null);
	}

	/**
	 * Save.
	 *
	 * @param Platform the platform
	 * @return the platform
	 */
	public static Platform save(Platform Platform) {
		return JPAService.runInTransaction(em -> em.merge(Platform));
	}

	/**
	 * Delete.
	 *
	 * @param Platform the platform
	 */
	public static void delete(Platform Platform) {
		JPAService.runInTransaction(em -> {
			em.remove(getById(Platform.getId(), em));
			return null;
		});
	}

	/**
	 * Find all.
	 *
	 * @return the list
	 */
	@SuppressWarnings("unchecked")
	public static List<Platform> findAll() {
		return JPAService.runInTransaction(em -> em.createQuery("select c from Platform c")
			.getResultList());
	}

	
	/**
	 * Find by name.
	 *
	 * @param string the string
	 * @return the platform
	 */
	@SuppressWarnings("unchecked")
	public static Platform findByName(String string) {
		return JPAService.runInTransaction(em -> {
			Query query = em.createQuery("select c from Platform c where lower(name) = lower(:string)");
			query.setParameter("string", string);
			List<Platform> resultList = query.getResultList();
			return resultList.get(0);
		});
	}
}
