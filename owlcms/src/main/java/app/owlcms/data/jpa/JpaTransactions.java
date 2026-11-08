/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.jpa;

import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

/** Transaction execution without application locking; failures propagate to the caller. */
public final class JpaTransactions {

	private JpaTransactions() {
	}

	/**
	 * Runs the work in one transaction. Unlike {@link JPAService#runInTransaction}, a failure is never swallowed:
	 * the transaction is rolled back and the exception propagates.
	 */
	public static <T> T inTransaction(Function<EntityManager, T> work) {
		EntityManager em = JPAService.getFactory().createEntityManager();
		EntityTransaction tx = em.getTransaction();
		try {
			tx.begin();
			T result = work.apply(em);
			tx.commit();
			return result;
		} catch (RuntimeException e) {
			try {
				if (tx.isActive()) {
					tx.rollback();
				}
			} catch (RuntimeException rollbackFailure) {
				e.addSuppressed(rollbackFailure);
			}
			throw e;
		} finally {
			em.close();
		}
	}

	/**
	 * Runs the work in a transaction that is always rolled back: a dry run reads consistent data and can exercise
	 * the real write path, yet nothing is written or bootstrapped.
	 */
	public static <T> T inRollbackOnlyTransaction(Function<EntityManager, T> work) {
		EntityManager em = JPAService.getFactory().createEntityManager();
		EntityTransaction tx = em.getTransaction();
		try {
			tx.begin();
			return work.apply(em);
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
			em.close();
		}
	}
}
