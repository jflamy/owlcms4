/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.competition;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.LoggerFactory;

import app.owlcms.audit.AuditOrigin;
import app.owlcms.audit.SettingsAudit;
import app.owlcms.data.athleteSort.RankingConfig;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.jpa.JpaTransactions;
import app.owlcms.data.jpa.SingletonSaves;
import app.owlcms.data.jpa.StaleSingletonException;
import app.owlcms.data.jpa.StaleSingletonException.Kind;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * CompetitionRepository.
 *
 */
public class CompetitionRepository {
	private final static Logger logger = (Logger) LoggerFactory.getLogger(CompetitionRepository.class);
	static {
		logger.setLevel(Level.INFO);
	}

	/**
	 * Delete.
	 *
	 * @param Competition the competition
	 */
	public static void delete(Competition Competition) {
		JPAService.runInTransaction(em -> {
			em.remove(getById(Competition.getId(), em));
			return null;
		});
	}

	public static void doRemoveAll(EntityManager em) {
		int deletedCount;
		deletedCount = em.createQuery("DELETE FROM Participation").executeUpdate();
		logger.info("deleted {} participations", deletedCount);
		deletedCount = em.createQuery("DELETE FROM Athlete").executeUpdate();
		logger.info("deleted {} athletes", deletedCount);
		deletedCount = em.createQuery("DELETE FROM Coach").executeUpdate();
		logger.info("deleted {} coaches", deletedCount);
		deletedCount = em.createQuery("DELETE FROM TechnicalOfficialsTimetable").executeUpdate();
		logger.info("deleted {} technical officials timetable entries", deletedCount);
		deletedCount = em.createQuery("DELETE FROM CompetitionGroup").executeUpdate();
		logger.info("deleted {} sessions", deletedCount);
		deletedCount = em.createQuery("DELETE FROM Category").executeUpdate();
		logger.info("deleted {} categories", deletedCount);
		deletedCount = em.createQuery("DELETE FROM AgeGroup").executeUpdate();
		logger.info("deleted {} age groups", deletedCount);
		deletedCount = em.createQuery("DELETE FROM RecordEvent").executeUpdate();
		logger.info("deleted {} records", deletedCount);
		deletedCount = em.createQuery("DELETE FROM Platform").executeUpdate();
		logger.info("deleted {} platforms", deletedCount);
		deletedCount = em.createQuery("DELETE FROM Config").executeUpdate();
		logger.info("deleted {} config", deletedCount);
		deletedCount = em.createQuery("DELETE FROM Competition").executeUpdate();
		logger.info("deleted {} competition", deletedCount);

		em.flush();
		em.clear();
	}

	/**
	 * Find all.
	 *
	 * @return the list
	 */
	@SuppressWarnings("unchecked")
	public static List<Competition> findAll() {
		return JPAService.runInTransaction(em -> em.createQuery("select c from Competition c").getResultList());
	}

	/**
	 * Gets Competition by id.
	 *
	 * @param id the id
	 * @param em the em
	 * @return the by id
	 */
	@SuppressWarnings("unchecked")
	public static Competition getById(Long id, EntityManager em) {
		Query query = em.createQuery("select u from Competition u where u.id=:id");
		query.setParameter("id", id);

		return (Competition) query.getResultList().stream().findFirst().orElse(null);
	}

	public static void removeAll() {
		JPAService.runInTransaction(em -> {
			doRemoveAll(em);
			return null;
		});
	}

	/**
	 * A fresh copy of the stored competition for a form: never the published instance. It carries the stamp it was
	 * loaded with, which {@link #saveEdited} compares with the row.
	 */
	public static Competition loadForEdit() {
		Competition published = Competition.getCurrent();
		Competition draft = JpaTransactions.inTransaction(
		        em -> SingletonSaves.findFresh(em, Competition.class, published.getId()));
		if (draft == null) {
			throw new IllegalStateException("no stored Competition");
		}
		return draft;
	}

	/**
	 * Saves a form's copy, unless the stored competition was saved after the copy was loaded.
	 *
	 * @throws StaleSingletonException when the competition was saved since; nothing is written
	 */
	public static Competition saveEdited(Competition draft, AuditOrigin origin) {
		return write(draft, origin, Mode.EDIT);
	}

	/**
	 * Immediate change (no form): loads the stored row under the save lock, applies the change, saves and publishes.
	 */
	public static Competition apply(Consumer<Competition> change, AuditOrigin origin) {
		WriteResult result;
		synchronized (SingletonSaves.LOCK) {
			Competition published = Competition.getCurrent();
			result = JpaTransactions.inTransaction(em -> {
				Competition row = SingletonSaves.findFresh(em, Competition.class, published.getId());
				if (row == null) {
					throw new StaleSingletonException(Kind.COMPETITION, null);
				}
				Integer[] oldBreaks = { row.getShorterBreakDuration(), row.getLongerBreakDuration() };
				change.accept(row);
				row.stamp(origin, row.getLastSavedAt());
				return new WriteResult(row, oldBreaks);
			});
			publish(result.competition(), published);
		}
		afterCommit(result);
		return result.competition();
	}

	/** Stores a new competition (startup, initial data). */
	public static Competition create(Competition fresh, AuditOrigin origin) {
		return write(fresh, origin, Mode.CREATE);
	}

	/**
	 * TEST SETUP ONLY. Saves without the stale-save check: a page that loaded the competition earlier is not
	 * detected. Application code must use {@link #saveEdited} (forms), {@link #apply} (immediate changes) or
	 * {@link #create} (first creation).
	 */
	public static Competition saveTesting(Competition competition) {
		return write(competition, AuditOrigin.system("test"), Mode.REPLACE);
	}

	private enum Mode {
		EDIT,
		CREATE,
		REPLACE
	}

	private static Competition write(Competition competition, AuditOrigin origin, Mode mode) {
		WriteResult result;
		synchronized (SingletonSaves.LOCK) {
			Competition published = Competition.peekCurrent();
			result = JpaTransactions.inTransaction(em -> {
				Competition row = SingletonSaves.findFresh(em, Competition.class, competition.getId());
				if (mode == Mode.EDIT && (row == null
				        || !Objects.equals(row.getLastSavedAt(), competition.getLastSavedAt()))) {
					// a missing row must not fall back to an insert: that would create a second competition
					throw new StaleSingletonException(Kind.COMPETITION, row != null ? row.getLastSavedBy() : null);
				}
				if (mode == Mode.CREATE && row != null) {
					throw new IllegalStateException("Competition already stored");
				}
				Integer[] oldBreaks = row == null ? new Integer[2]
				        : new Integer[] { row.getShorterBreakDuration(), row.getLongerBreakDuration() };
				competition.stamp(origin, row != null ? row.getLastSavedAt() : null);
				return new WriteResult(em.merge(competition), oldBreaks);
			});
			publish(result.competition(), published);
		}
		afterCommit(result);
		return result.competition();
	}

	/** Under the save lock, after the commit. */
	private static void publish(Competition saved, Competition previous) {
		saved.copyRuntimeStateFrom(previous);
		// needed because some classes get competition parameters from getCurrent()
		Competition.setCurrent(saved);
	}

	private static void afterCommit(WriteResult result) {
		Competition current = result.competition();
		SettingsAudit.change("competition", "competition.change", "shorterBreakDuration", result.oldBreaks()[0],
		        current.getShorterBreakDuration(), null);
		SettingsAudit.change("competition", "competition.change", "longerBreakDuration", result.oldBreaks()[1],
		        current.getLongerBreakDuration(), null);
		// Recompute mustCompute rankings based on updated Competition and age groups
		RankingConfig.updateMustCompute();
	}

	private record WriteResult(Competition competition, Integer[] oldBreaks) {
	}

}
