/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.data.config;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.LoggerFactory;

import app.owlcms.data.jpa.JPAService;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * ConfigRepository.
 *
 */
public class ConfigRepository {
    private final static Logger logger = (Logger) LoggerFactory.getLogger(ConfigRepository.class);
    static {
        logger.setLevel(Level.INFO);
    }

    /**
     * Delete.
     *
     * @param Config the config
     */
    public static void delete(Config Config) {
        JPAService.runInTransaction(em -> {
            em.remove(getById(Config.getId(), em));
            return null;
        });
    }

    /**
     * Find all.
     *
     * @return the list
     */
    @SuppressWarnings("unchecked")
    public static List<Config> findAll() {
        return JPAService.runInTransaction(em -> em.createQuery("select c from Config c").getResultList());
    }

    /**
     * Gets Config by id.
     *
     * @param id the id
     * @param em the em
     * @return the by id
     */
    @SuppressWarnings("unchecked")
    public static Config getById(Long id, EntityManager em) {
        Query query = em.createQuery("select u from Config u where u.id=:id");
        query.setParameter("id", id);

        return (Config) query.getResultList().stream().findFirst().orElse(null);
    }

    /**
     * Save.
     *
     * @param config the config
     * @return the config
     */
    public static Config save(Config config) {
        JPAService.runInTransaction(em -> {
            Config nc = em.merge(config);
            // needed because some classes get config parameters from getCurrent()
            Config.setCurrent(nc);
            return nc;
        });

        Config current = Config.getCurrent();
        return current;
    }

}
