/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.platform;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.LoggerFactory;

import app.owlcms.data.group.Group;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.monitors.MQTTMonitor;
import app.owlcms.spreadsheet.RGroup;
import ch.qos.logback.classic.Logger;

/**
 * PlatformRepository.
 *
 */
public class PlatformRepository {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(PlatformRepository.class);

	public static void checkPlatforms() {
		List<Platform> platforms = PlatformRepository.findAll();
		if (platforms.isEmpty()) {
			JPAService.runInTransaction(em -> {
				Platform np = new Platform("A");
				em.persist(np);
				return np;
			});
		} else {
			logger.debug("to be kept {}", platforms.stream().map(Platform::getName).collect(Collectors.toSet()));

			for (Platform platform : platforms) {
				String normalizedName = normalizeName(platform.getName());
				platform.setName(normalizedName);
				if (normalizedName == null || normalizedName.isBlank()) {
					PlatformRepository.delete(platform);
					logger.info("removing invalid entry for platform {}", platform.getId());
				}
			}

			fixDuplicates();

			if (PlatformRepository.findAll().isEmpty()) {
				JPAService.runInTransaction(em -> {
					Platform np = new Platform("A");
					em.persist(np);
					return np;
				});
			}
		}

	}

	public static void fixDuplicates() {
		JPAService.runInTransaction(em -> {
			@SuppressWarnings("unchecked")
			List<Platform> platforms = em.createQuery("select c from Platform c order by c.id").getResultList();
			Map<String, Platform> canonicalByName = new LinkedHashMap<>();
			for (Platform platform : platforms) {
				String normalizedName = normalizeName(platform.getName());
				if (normalizedName == null || normalizedName.isBlank()) {
					continue;
				}

				String normalizedKey = normalizeLookupKey(normalizedName);
				Platform canonical = canonicalByName.get(normalizedKey);
				if (canonical == null) {
					platform.setName(normalizedName);
					canonicalByName.put(normalizedKey, platform);
					continue;
				}

				logger.info("collapsing duplicate platform '{}' ({}) into canonical platform '{}' ({})",
				        platform.getName(), platform.getId(), canonical.getName(), canonical.getId());
				reassignGroups(em, platform, canonical);
				em.remove(platform);
			}
			return null;
		});
	}

	public static List<Platform> fixDuplicates(List<Platform> importedPlatforms, List<Group> importedGroups) {
		if (importedPlatforms == null) {
			return null;
		}

		Map<String, Platform> canonicalByName = new LinkedHashMap<>();
		List<Platform> canonicalPlatforms = new ArrayList<>();
		for (Platform platform : importedPlatforms) {
			String normalizedName = normalizeName(platform.getName());
			platform.setName(normalizedName);
			if (normalizedName == null || normalizedName.isBlank()) {
				canonicalPlatforms.add(platform);
				continue;
			}

			String normalizedKey = normalizeLookupKey(normalizedName);
			Platform canonical = canonicalByName.get(normalizedKey);
			if (canonical == null) {
				canonicalByName.put(normalizedKey, platform);
				canonicalPlatforms.add(platform);
			} else if (!Objects.equals(canonical.getId(), platform.getId())) {
				logger.warn("duplicate imported platform '{}' found with ids {} and {}; keeping {}",
				        normalizedName, canonical.getId(), platform.getId(), canonical.getId());
			}
		}

		if (importedGroups != null) {
			for (Group group : importedGroups) {
				Platform groupPlatform = group.getPlatform();
				if (groupPlatform == null) {
					continue;
				}
				String normalizedKey = normalizeLookupKey(groupPlatform.getName());
				Platform canonical = normalizedKey != null ? canonicalByName.get(normalizedKey) : null;
				if (canonical != null && canonical != groupPlatform) {
					logger.warn("remapping imported group '{}' from duplicate platform id {} to canonical id {}",
					        group.getName(), groupPlatform.getId(), canonical.getId());
					group.setPlatform(canonical);
				}
			}
		}

		return canonicalPlatforms;
	}

	public static List<Platform> canonicalizeImportedPlatforms(List<Platform> importedPlatforms, List<Group> importedGroups) {
		return fixDuplicates(importedPlatforms, importedGroups);
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
				if (platformName != null && !platformName.isBlank() && !checkPlatforms.contains(platformName)) {
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
		try {
			if (OwlcmsFactory.getFopByName() == null) {
				OwlcmsFactory.initFOPByName();
			}
			FieldOfPlay fop = OwlcmsFactory.getFOPByName(platform.getName());
			MQTTMonitor mm = fop != null ? fop.getMqttMonitor() : null;
			JPAService.runInTransaction(em -> {
				// this is the only case where platform needs to know its groups, so we do a
				// query instead of adding a relationship.
				Long pId = platform.getId();
				// group is illegal as a table name; query uses the configured table name for
				// entity.
				Query gQ = em.createQuery("select g from CompetitionGroup g join g.platform p where p.id = :platformId");
				gQ.setParameter("platformId", pId);
				@SuppressWarnings("unchecked")
				List<Group> gL = gQ.getResultList();
				for (Group g : gL) {
					g.setPlatform(null);
				}
				em.remove(em.contains(platform) ? platform : em.merge(platform));
				return null;
			});
			if (mm != null) {
				mm.publishMqttConfig();
			}
			OwlcmsFactory.setFirstFOPAsDefault();
		} catch (Throwable t) {
			t.printStackTrace();
		}
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
		String normalizedName = normalizeName(string);
		if (normalizedName == null || normalizedName.isBlank()) {
			return null;
		}
		return JPAService.runInTransaction(em -> {
			Query query = em.createQuery("select c from Platform c where lower(name) = lower(:string)");
			query.setParameter("string", normalizedName);
			List<Platform> resultList = query.getResultList();
			return resultList.isEmpty() ? null : resultList.get(0);
		});
	}

	public static boolean hasDuplicateName(Platform platform) {
		if (platform == null) {
			return false;
		}

		String normalizedKey = normalizeLookupKey(platform.getName());
		if (normalizedKey == null) {
			return false;
		}

		Long platformId = platform.getId();
		return findAll().stream()
		        .filter(existing -> !Objects.equals(existing.getId(), platformId))
		        .map(Platform::getName)
		        .map(PlatformRepository::normalizeLookupKey)
		        .anyMatch(normalizedKey::equals);
	}

	public static String normalizeName(String name) {
		return name == null ? null : name.trim();
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
		return save(platform, true);
	}

	/**
	 * Save with optional MQTT config publish.
	 *
	 * @param platform the platform
	 * @param publishMqtt whether to publish MQTT config after save
	 * @return the platform
	 */
	public static Platform save(Platform platform, boolean publishMqtt) {
		logger.debug("Saving platform {} publishMqtt={}", platform.getName(), publishMqtt);
		Platform nPlatform = JPAService.runInTransaction(em -> em.merge(platform));
		String name = nPlatform.getName();
		FieldOfPlay fop = null;
		if (name != null) {
			fop = OwlcmsFactory.getFOPByName(name);
			if (fop != null) {
				fop.setPlatform(nPlatform);
			} else {
				fop = OwlcmsFactory.registerEmptyFOP(nPlatform);
			}
			if (publishMqtt) {
				MQTTMonitor mm = fop.getMqttMonitor();
				if (mm != null) {
					mm.publishMqttConfig();
				}
			}
		}
		return nPlatform;
	}

	public static void syncFOPs() {
		Set<String> preCheckPlatforms = PlatformRepository.findAll().stream().map(p -> p.getName())
		        .collect(Collectors.toSet());
		Set<String> fops = OwlcmsFactory.getFOPs().stream().map(f -> f.getName()).collect(Collectors.toSet());

		// delete all unused FOPs
		for (String fopName : fops) {
			if (!preCheckPlatforms.contains(fopName)) {
				FieldOfPlay fop = OwlcmsFactory.getFopByName().get(fopName);
				fop.getFopEventBus().unregister(fop);
				OwlcmsFactory.getFopByName().remove(fopName);
			}
		}
	}

	private static String normalizeLookupKey(String name) {
		String normalizedName = normalizeName(name);
		if (normalizedName == null || normalizedName.isBlank()) {
			return null;
		}
		return normalizedName.toLowerCase(Locale.ROOT);
	}

	private static void reassignGroups(EntityManager em, Platform fromPlatform, Platform toPlatform) {
		Query groupQuery = em.createQuery("select g from CompetitionGroup g join g.platform p where p.id = :platformId");
		groupQuery.setParameter("platformId", fromPlatform.getId());
		@SuppressWarnings("unchecked")
		List<Group> groups = groupQuery.getResultList();
		for (Group group : groups) {
			group.setPlatform(toPlatform);
		}
	}
}
