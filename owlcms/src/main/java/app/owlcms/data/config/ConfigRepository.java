/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.config;

import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.function.Consumer;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.LoggerFactory;

import app.owlcms.access.AccessMode;
import app.owlcms.access.AccessStartup;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.audit.AuthAudit;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.jpa.SingletonSaves;
import app.owlcms.data.jpa.StaleSingletonException;
import app.owlcms.data.jpa.StaleSingletonException.Kind;
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

	private static final Long DEFAULT_ID = 1L;

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
		List<Config> configList = JPAService
		        .runInTransaction(em -> em.createQuery("select c from Config c").getResultList());
		if (configList.size() < 1) {
			logger.debug("found {} config", configList.size());
		} else if (configList.size() > 1) {
			logger.error("found {} configs", configList.size());
		}
		return configList;
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
	 * A fresh copy of the stored settings for a form: never the published instance. It carries the stamp it was
	 * loaded with, which {@link #saveEdited} compares with the row.
	 */
	public static Config loadForEdit() {
		Config published = Config.getCurrent();
		Config draft = SingletonSaves.inTransaction(em -> SingletonSaves.findFresh(em, Config.class, published.getId()));
		if (draft == null) {
			throw new IllegalStateException("no stored Config");
		}
		draft.copyRuntimeStateFrom(published, true);
		return draft;
	}

	/**
	 * Saves a form's copy, unless the stored settings were saved after the copy was loaded.
	 *
	 * @throws StaleSingletonException when the settings were saved since; nothing is written
	 */
	public static Config saveEdited(Config draft, AuditOrigin origin) {
		return write(draft, origin, Mode.EDIT);
	}

	/**
	 * Immediate change (no form): loads the stored row under the save lock, applies the change, saves and publishes.
	 * The change re-checks its own preconditions on the row it is given.
	 */
	public static Config apply(Consumer<Config> change, AuditOrigin origin) {
		SaveResult result;
		synchronized (SingletonSaves.LOCK) {
			Config published = Config.peekCurrent();
			Long id = published != null ? published.getId() : DEFAULT_ID;
			result = SingletonSaves.inTransaction(em -> {
				Config row = SingletonSaves.findFresh(em, Config.class, id);
				if (row == null) {
					throw new IllegalStateException("no stored Config");
				}
				AccessMode previousStored = row.getAccessMode();
				change.accept(row);
				row.prepareForSave();
				row.stamp(origin, row.getLastSavedAt());
				return new SaveResult(row, true, previousStored);
			});
			result.config().copyRuntimeStateFrom(published, true);
			Config.publish(result.config());
		}
		afterCommit(result, origin);
		return result.config();
	}

	/** Stores settings when there are none (startup, initial data). */
	public static Config create(Config fresh, AuditOrigin origin) {
		return write(fresh, origin, Mode.CREATE);
	}

	/**
	 * DATABASE IMPORT ONLY. Replaces the stored settings with the imported ones, without the stale-save check: an
	 * import is a faithful replacement, whatever was saved before. Pages opened before the import are still refused
	 * afterwards, since this save is stamped like any other.
	 */
	public static Config replaceForImport(Config imported, AuditOrigin origin) {
		return replace(imported, origin);
	}

	/** Unchecked replacement, shared by {@link #replaceForImport} and {@link Config#setCurrentTesting}. */
	static Config replace(Config config, AuditOrigin origin) {
		return write(config, origin, Mode.REPLACE);
	}

	private enum Mode {
		EDIT,
		CREATE,
		REPLACE
	}

	private static Config write(Config config, AuditOrigin origin, Mode mode) {
		SaveResult result;
		synchronized (SingletonSaves.LOCK) {
			Config published = Config.peekCurrent();
			config.prepareForSave();
			preserveLocalOverride(config);
			result = SingletonSaves.inTransaction(em -> {
				// raw values only: the effective mode needs the account cache, which must not be read in here
				Config row = SingletonSaves.findFresh(em, Config.class, config.getId());
				if (mode == Mode.EDIT && (row == null || !Objects.equals(row.getLastSavedAt(), config.getLastSavedAt()))) {
					throw new StaleSingletonException(Kind.CONFIG, row != null ? row.getLastSavedBy() : null);
				}
				if (mode == Mode.CREATE && row != null) {
					throw new IllegalStateException("Config already stored");
				}
				AccessMode previousStored = row != null ? row.getAccessMode() : null;
				config.stamp(origin, row != null ? row.getLastSavedAt() : null);
				Config nc = em.merge(config);
				return new SaveResult(nc, row != null, previousStored);
			});
			// the copy that was written carries the forwarding activity edited with it
			result.config().copyRuntimeStateFrom(published, false);
			result.config().setManagedForwardingActivity(config.getManagedForwardingActivity());
			Config.publish(result.config());
		}
		afterCommit(result, origin);
		return result.config();
	}

	/**
	 * Hibernate cannot merge a Blob whose stream has already been consumed: extract the bytes (from the in-memory
	 * BlobProxy, or from the database), then rehydrate with a fresh BlobProxy; clear when there are none.
	 */
	private static void preserveLocalOverride(Config config) {
		byte[] localZipBytes = null;
		try {
			localZipBytes = config.getLocalZipBlobBytes();
		} catch (Exception e) {
			logger.debug("No in-memory BLOB to preserve: {}", e.getMessage());
		}
		if (localZipBytes != null && localZipBytes.length > 0) {
			config.setLocalZipBlob(localZipBytes);
		} else {
			config.clearLocalOverride();
			config.setClearZip(false);
		}
	}

	/** Side effects of a committed save, outside the save lock. */
	private static void afterCommit(SaveResult result, AuditOrigin origin) {
		TimeZone tz = result.config().getTimeZone();
		if (tz != null) {
			logger.info("setting time zone to {}", tz);
			TimeZone.setDefault(tz);
		}
		AccessMode modeBefore = result.hadPrevious()
		        ? Config.effectiveAccessMode(result.previousStored())
		        : null;
		AccessMode modeAfter = result.config().getParamAccessMode();
		if (modeBefore != null && modeBefore != modeAfter) {
			AuthAudit.accessModeChanged(origin, modeBefore.name(), modeAfter.name());
		}
		boolean initializeAccounts = modeAfter == AccessMode.ACCOUNTS && modeBefore != AccessMode.ACCOUNTS;
		if (initializeAccounts) {
			AccessStartup.initializeRoleAccountsInBackground(origin);
		}
	}

	private record SaveResult(Config config, boolean hadPrevious, AccessMode previousStored) {
	}

}
