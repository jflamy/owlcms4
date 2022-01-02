/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.competition;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.LoggerFactory;

import app.owlcms.data.jpa.JPAService;
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
        deletedCount = em.createQuery("DELETE FROM CompetitionGroup").executeUpdate();
        logger.info("deleted {} groups", deletedCount);
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
     * Save.
     *
     * @param competition the competition
     * @return the competition
     */
    public static Competition save(Competition competition) {
        JPAService.runInTransaction(em -> {
            Competition nc = em.merge(competition);
            // needed because some classes get competition parameters from getCurrent()
            Competition.setCurrent(nc);
            return nc;
        });

        Competition current = Competition.getCurrent();
        return current;
    }

}
