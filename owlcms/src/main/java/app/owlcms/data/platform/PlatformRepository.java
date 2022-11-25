/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.platform;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.LoggerFactory;

import app.owlcms.data.group.Group;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.spreadsheet.RGroup;
import ch.qos.logback.classic.Logger;

/**
 * PlatformRepository.
 *
 */
public class PlatformRepository {

    final private static Logger logger = (Logger) LoggerFactory.getLogger(PlatformRepository.class);

    public static void checkPlatforms() {
        Set<String> checkPlatforms = PlatformRepository.findAll().stream().map(Platform::getName)
                .collect(Collectors.toSet());
        if (checkPlatforms.isEmpty()) {
            JPAService.runInTransaction(em -> {
                Platform np = new Platform("A");
                em.persist(np);
                return np;
            });
        } else {
            logger.debug("to be kept {}", checkPlatforms);

            Set<String> seen = new HashSet<>();
            // delete all unused platforms
            for (Platform pl : PlatformRepository.findAll()) {
                String name = pl.getName();
                if (name == null || name.isBlank() || seen.contains(name)) {
                    // we have already seen a platform with this name
                    // group will be connected with the first platform created with that name
                    PlatformRepository.delete(pl);
                    logger.info("removing duplicate or invalid entry for platform {}", name);
                } else {
                    seen.add(name);
                }
            }
        }
    }

    public static void createMissingPlatforms(List<RGroup> groups) {
        final Set<String> checkPlatforms = PlatformRepository.findAll().stream().map(p -> p.getName())
                .collect(Collectors.toSet());
        logger.debug("platforms after cleanup {}", checkPlatforms);

        // create missing platforms
        JPAService.runInTransaction(em -> {
            groups.stream().forEach(g -> {
                String platformName = g.getPlatform();
                Group group = g.getGroup();
                if (platformName != null && !checkPlatforms.contains(platformName)) {
                    Platform np = new Platform();
                    np.setName(platformName);
                    group.setPlatform(np);
                    // make sure we don't add twice.
                    checkPlatforms.add(platformName);
                    logger.info("adding platform '{}'", np.getName());
                    em.persist(np);
                }
            });
            em.flush();
            return null;
        });
    }

    /**
     * Delete.
     *
     * @param Platform the platform
     */
    /**
     * @param Platform
     */
    public static void delete(Platform platform) {
        JPAService.runInTransaction(em -> {
            // this is the only case where platform needs to know its groups, so we do a
            // query instead of adding a relationship.
            Long pId = platform.getId();
            // group is illegal as a table name; query uses the configured table name for entity.
            Query gQ = em.createQuery("select g from CompetitionGroup g join g.platform p where p.id = :platformId");
            gQ.setParameter("platformId", pId);
            @SuppressWarnings("unchecked")
            List<Group> gL = gQ.getResultList();
            for (Group g : gL) {
                g.setPlatform(null);
            }
            em.remove(em.contains(platform) ? platform : em.merge(platform));
            OwlcmsFactory.unregisterFOP(platform);
            return null;
        });
        OwlcmsFactory.setFirstFOPAsDefault();
    }

    public static void deleteUnusedPlatforms(Set<String> futurePlatforms) {
        Set<String> preCheckPlatforms = PlatformRepository.findAll().stream().map(p -> p.getName())
                .collect(Collectors.toSet());
        logger.info("platforms before cleanup {}", preCheckPlatforms);

        // delete all unused platforms
        for (Platform pl : PlatformRepository.findAll()) {
            if (!futurePlatforms.contains(pl.getName())) {
                logger.info("removing platform {}", pl.getName());
                PlatformRepository.delete(pl);
            } else {
            }
        }
    }

    /**
     * Find all.
     *
     * @return the list
     */
    @SuppressWarnings("unchecked")
    public static List<Platform> findAll() {
        return JPAService
                .runInTransaction(em -> em.createQuery("select c from Platform c order by c.id").getResultList());
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
            return resultList.isEmpty() ? null : resultList.get(0);
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
            } else {
                OwlcmsFactory.registerEmptyFOP(nPlatform);
            }
        }
        return nPlatform;
    }
}
