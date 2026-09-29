package app.owlcms.data.account;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.slf4j.LoggerFactory;

import app.owlcms.access.Role;
import app.owlcms.data.jpa.JPAService;
import ch.qos.logback.classic.Logger;

public final class UserAccountRepository {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(UserAccountRepository.class);

	// null means stale; reloaded on next use so per-navigation checks do not hit the database
	private static volatile Map<Long, UserAccount> cache;

	private UserAccountRepository() {
	}

	/** @return copies, safe to edit */
	public static List<UserAccount> findAll() {
		List<UserAccount> result = new ArrayList<>();
		for (UserAccount account : cached().values()) {
			result.add(account.copy());
		}
		result.sort((a, b) -> a.getUsername().compareTo(b.getUsername()));
		return result;
	}

	/** @return the cached account: read it, do not modify it */
	public static UserAccount findById(Long id) {
		return id == null ? null : cached().get(id);
	}

	/** @return the cached account: read it, do not modify it */
	public static UserAccount findByUsername(String username) {
		String key = UserAccount.normalizeUsername(username);
		if (key == null || key.isEmpty()) {
			return null;
		}
		for (UserAccount account : cached().values()) {
			if (key.equals(account.getUsername())) {
				return account;
			}
		}
		return null;
	}

	public static UserAccount save(UserAccount account) {
		UserAccount saved = JPAService.runInTransaction(em -> em.merge(account));
		invalidate();
		return saved;
	}

	public static void delete(UserAccount account) {
		JPAService.runInTransaction(em -> {
			UserAccount managed = em.find(UserAccount.class, account.getId());
			if (managed != null) {
				em.remove(managed);
			}
			return null;
		});
		invalidate();
	}

	/** Replaces all accounts as part of an account-bearing competition import. */
	public static void replaceAll(EntityManager em, List<UserAccount> accounts) {
		List<UserAccount> existing = em.createQuery("select a from UserAccount a", UserAccount.class).getResultList();
		for (UserAccount account : existing) {
			em.remove(account);
		}
		em.flush();
		for (UserAccount account : accounts) {
			em.merge(account);
		}
		invalidate();
	}

	public static void renamePlatform(String oldName, String newName) {
		updateGrants(grant -> Objects.equals(grant.getPlatformName(), oldName)
		        ? new RoleGrant(grant.getRole(), newName)
		        : grant);
	}

	public static void removePlatform(String name) {
		updateGrants(grant -> Objects.equals(grant.getPlatformName(), name) ? null : grant);
	}

	public static boolean adminHasPassword() {
		UserAccount admin = findByUsername(UserAccount.BUILT_IN_ADMIN);
		return admin != null && admin.hasPassword();
	}

	/** Creates or repairs the built-in admin account. */
	public static void ensureBuiltInAdmin() {
		UserAccount admin = findByUsername(UserAccount.BUILT_IN_ADMIN);
		if (admin == null) {
			UserAccount created = new UserAccount();
			created.setUsername(UserAccount.BUILT_IN_ADMIN);
			created.setDisplayName(UserAccount.BUILT_IN_ADMIN);
			created.getGrants().add(new RoleGrant(Role.ADMIN, null));
			save(created);
			logger.info("built-in admin account created (no password)");
			return;
		}
		boolean holdsAdmin = admin.getGrants().stream()
		        .anyMatch(g -> g.getRole() == Role.ADMIN && g.getPlatformName() == null);
		if (!admin.isEnabled() || !holdsAdmin) {
			UserAccount repaired = admin.copy();
			repaired.setEnabled(true);
			if (!holdsAdmin) {
				repaired.getGrants().add(new RoleGrant(Role.ADMIN, null));
			}
			save(repaired);
			logger.warn("built-in admin account repaired");
		}
	}

	public static void invalidate() {
		cache = null;
	}

	private static void updateGrants(Function<RoleGrant, RoleGrant> change) {
		JPAService.runInTransaction(em -> {
			List<UserAccount> all = em.createQuery("select a from UserAccount a", UserAccount.class).getResultList();
			for (UserAccount account : all) {
				List<RoleGrant> updated = new ArrayList<>();
				for (RoleGrant grant : account.getGrants()) {
					RoleGrant result = change.apply(grant);
					if (result != null) {
						updated.add(result);
					}
				}
				if (!updated.equals(account.getGrants())) {
					account.getGrants().clear();
					account.getGrants().addAll(updated);
				}
			}
			return null;
		});
		invalidate();
	}

	private static Map<Long, UserAccount> cached() {
		Map<Long, UserAccount> current = cache;
		if (current == null) {
			synchronized (UserAccountRepository.class) {
				current = cache;
				if (current == null) {
					List<UserAccount> all = JPAService.runInTransaction(
					        em -> em.createQuery("select a from UserAccount a", UserAccount.class).getResultList());
					current = Collections.unmodifiableMap(all.stream()
					        .collect(Collectors.toMap(UserAccount::getId, a -> a)));
					cache = current;
				}
			}
		}
		return current;
	}
}
