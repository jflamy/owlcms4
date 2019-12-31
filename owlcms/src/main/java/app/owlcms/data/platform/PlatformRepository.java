/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.data.platform;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import app.owlcms.data.jpa.JPAService;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsFactory;

/**
 * PlatformRepository.
 *
 */
public class PlatformRepository {

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
        return JPAService.runInTransaction(em -> em.createQuery("select c from Platform c").getResultList());
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

        return (Platform) query.getResultList().stream().findFirst().orElse(null);
    }

    /**
     * Save. The 1:1 relationship with FOP is managed manually since FOP is not persisted.
     *
     * @param platform the platform
     * @return the platform
     */
    public static Platform save(Platform platform) {
        Platform nPlatform = JPAService.runInTransaction(em -> em.merge(platform));
        String name = nPlatform.getName();
        if (name != null) {
            FieldOfPlay fop = OwlcmsFactory.getFOPByName(name);
            if (fop != null) {
                fop.setPlatform(nPlatform);
            }
        }
        return nPlatform;
    }
}
