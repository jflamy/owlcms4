/*****************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 ******************************************************************************/
package app.owlcms.data.coach;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.slf4j.LoggerFactory;

import app.owlcms.data.jpa.JPAService;
import ch.qos.logback.classic.Logger;

public class CoachRepository {

    @SuppressWarnings("unused")
    final private static Logger logger = (Logger) LoggerFactory.getLogger(CoachRepository.class);

    public static void delete(Coach c) {
        JPAService.runInTransaction(em -> {
            em.remove(em.contains(c) ? c : em.merge(c));
            return null;
        });
    }

    public static List<Coach> findAll() {
        return JPAService
                .runInTransaction(em -> em.createQuery("select c from Coach c order by c.lastName, c.firstName", Coach.class)
                        .getResultList());
    }

    public static Coach findByName(String string) {
        String[] t = string.split("[, ]+");
        String lastName = t[0];
        String firstName = t.length > 1 ? t[1] : "";
        return JPAService.runInTransaction(em -> {
            TypedQuery<Coach> query = em.createQuery("select c from Coach c where (lower(lastName) = lower(:lastName) and lower(firstName) = lower(:firstName))", Coach.class);
            query.setParameter("lastName", lastName);
            query.setParameter("firstName", firstName);
            List<Coach> resultList = query.getResultList();
            return resultList.isEmpty() ? null : resultList.get(0);
        });
    }

    public static Coach safeFindByName(String string) {
        Coach c = findByName(string);
        if (c == null) {
            c = new Coach();
            c.setLastName(string);
        }
        return c;
    }

    public static Coach getById(Long id, EntityManager em) {
        TypedQuery<Coach> query = em.createQuery("select u from Coach u where u.id=:id", Coach.class);
        query.setParameter("id", id);

        return query.getResultList().stream().findFirst().orElse(null);
    }

    public static Coach save(Coach coach) {
        Coach nCoach = JPAService.runInTransaction(em -> em.merge(coach));
        return nCoach;
    }

    public static void deleteAll(EntityManager em) {
        em.createQuery("delete from Coach").executeUpdate();
    }
}
