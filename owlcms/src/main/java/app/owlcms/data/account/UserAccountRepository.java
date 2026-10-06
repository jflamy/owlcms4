package app.owlcms.data.account;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.slf4j.LoggerFactory;

import app.owlcms.access.Role;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.platform.Platform;
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
			if (key.equals(UserAccount.normalizeUsername(account.getUsername()))) {
				return account;
			}
		}
		return null;
	}

	public static UserAccount save(UserAccount account) {
		UserAccount saved = JPAService.runInTransaction(em -> {
			List<UserAccount> matching = em.createQuery(
			        "select a from UserAccount a where lower(trim(a.username)) = :username", UserAccount.class)
			        .setParameter("username", UserAccount.normalizeUsername(account.getUsername())).getResultList();
			if (matching.stream().anyMatch(a -> !Objects.equals(a.getId(), account.getId()))) {
				logger./**/warn("duplicate account username refused username={}", account.getUsername());
				throw new IllegalArgumentException("Account username already exists: " + account.getUsername());
			}
			return em.merge(account);
		});
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
		accountsByUsername(accounts);
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

	public static synchronized void ensureRoleAccounts() {
		initializeRoleAccounts(false);
	}

	public static synchronized List<UserAccount> resetPlatformAccounts() {
		initializeRoleAccounts(true);
		return findAll();
	}

	private static void initializeRoleAccounts(boolean resetPlatforms) {
		JPAService.runInTransaction(em -> {
			List<UserAccount> existing = em.createQuery("select a from UserAccount a", UserAccount.class).getResultList();
			List<String> platforms = em.createQuery("select p from Platform p order by p.id", Platform.class)
			        .getResultList().stream().map(Platform::getName).toList();
			if (resetPlatforms) {
				for (UserAccount account : existing.stream()
				        .filter(UserAccountRepository::isConventionalPlatformAccount).toList()) {
					em.remove(account);
					logger.info("platform role account reset username={}", account.getUsername());
				}
				em.flush();
				existing = existing.stream().filter(a -> !isConventionalPlatformAccount(a)).toList();
			}
			for (UserAccount account : missingRoleAccounts(existing, platforms)) {
				em.persist(account);
				logger.info("role account created username={} (password required)", account.getUsername());
			}
			return null;
		});
		invalidate();
	}

	static boolean isConventionalPlatformAccount(UserAccount account) {
		if (account.isBuiltInAdmin() || account.getGrants().size() != 1) {
			return false;
		}
		RoleGrant grant = account.getGrants().get(0);
		if (!grant.getRole().isPlatformScopable() || grant.getPlatformName() == null) {
			return false;
		}
		String roleName = grant.getRole().name().toLowerCase(Locale.ROOT);
		String username = UserAccount.normalizeUsername(account.getUsername());
		return roleName.equals(username)
		        || UserAccount.normalizeUsername(roleName + "-" + grant.getPlatformName()).equals(username);
	}

	static List<UserAccount> missingRoleAccounts(List<UserAccount> existing, List<String> platforms) {
		Map<String, UserAccount> byUsername = accountsByUsername(existing);
		List<UserAccount> missing = new ArrayList<>();
		for (Role role : Role.values()) {
			if (!role.isGrantable()) {
				continue;
			}
			List<String> scopes = role.isPlatformScopable() ? platforms : Collections.singletonList(null);
			for (String platform : scopes) {
				String username = role.name().toLowerCase(Locale.ROOT)
				        + (platform == null || platforms.size() == 1 ? "" : "-" + platform);
				String key = UserAccount.normalizeUsername(username);
				if (byUsername.containsKey(key)) {
					UserAccount account = byUsername.get(key);
					if (!account.getGrants().contains(new RoleGrant(role, platform))) {
						logger./**/warn("role account username={} already exists with different grants; left unchanged",
						        username);
					}
					continue;
				}
				UserAccount account = new UserAccount();
				account.setUsername(username);
				account.setDisplayName(username);
				account.getGrants().add(new RoleGrant(role, platform));
				missing.add(account);
				byUsername.put(key, account);
			}
		}
		return missing;
	}

	private static Map<String, UserAccount> accountsByUsername(List<UserAccount> accounts) {
		return accounts.stream().collect(Collectors.toMap(
		        a -> UserAccount.normalizeUsername(a.getUsername()), Function.identity(), (first, duplicate) -> {
			        logger./**/warn("duplicate account username refused username={}", duplicate.getUsername());
			        throw new IllegalArgumentException("Duplicate account username: " + duplicate.getUsername());
		        }));
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
