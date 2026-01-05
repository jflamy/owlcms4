/*******************************************************************************
 * @author Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0 (NPOSL-3.0)
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.technicalofficial;

import java.util.List;

import javax.persistence.EntityManager;

import app.owlcms.data.group.Group;

/**
 * Repository for TechnicalOfficialsTimetable entities.
 * 
 * Provides CRUD operations and queries for timetable entries that map
 * technical officials to sessions and roles.
 */
public class TechnicalOfficialsTimetableRepository {

    public static List<TechnicalOfficialsTimetable> findAll(EntityManager em) {
        return em.createQuery("SELECT t FROM TechnicalOfficialsTimetable t ORDER BY t.id", TechnicalOfficialsTimetable.class)
                .getResultList();
    }

    public static TechnicalOfficialsTimetable findById(EntityManager em, Long id) {
        return em.find(TechnicalOfficialsTimetable.class, id);
    }

    public static List<TechnicalOfficialsTimetable> findByGroup(EntityManager em, Group group) {
        if (group == null) {
            return List.of();
        }
        return em.createQuery(
                "SELECT t FROM TechnicalOfficialsTimetable t WHERE t.group = :group ORDER BY t.roleCategory, t.teamNumber",
                TechnicalOfficialsTimetable.class)
                .setParameter("group", group)
                .getResultList();
    }

    public static List<TechnicalOfficialsTimetable> findByGroupAndRole(EntityManager em, Group group, OfficialRole role) {
        if (group == null || role == null) {
            return List.of();
        }
        return em.createQuery(
                "SELECT t FROM TechnicalOfficialsTimetable t WHERE t.group = :group AND t.roleCategory = :role ORDER BY t.teamNumber",
                TechnicalOfficialsTimetable.class)
                .setParameter("group", group)
                .setParameter("role", role)
                .getResultList();
    }

    public static TechnicalOfficialsTimetable save(EntityManager em, TechnicalOfficialsTimetable timetable) {
        if (timetable.getId() == null) {
            em.persist(timetable);
            return timetable;
        } else {
            return em.merge(timetable);
        }
    }

    public static void delete(EntityManager em, TechnicalOfficialsTimetable timetable) {
        if (timetable != null && timetable.getId() != null) {
            em.remove(em.merge(timetable));
        }
    }

    public static void deleteAll(EntityManager em) {
        em.createQuery("DELETE FROM TechnicalOfficialsTimetable").executeUpdate();
    }

    public static void deleteByGroup(EntityManager em, Group group) {
        if (group != null) {
            em.createQuery("DELETE FROM TechnicalOfficialsTimetable t WHERE t.group = :group")
                    .setParameter("group", group)
                    .executeUpdate();
        }
    }

}
