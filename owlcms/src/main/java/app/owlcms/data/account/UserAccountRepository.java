package app.owlcms.data.account;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.slf4j.LoggerFactory;

import app.owlcms.access.AccessMode;
import app.owlcms.access.Role;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.audit.AuthAudit;
import app.owlcms.data.config.Config;
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

	/**
	 * @param tokenHash SHA-256 of a presented token
	 * @return the cached account holding a token with that hash, or null: read it, do not modify it
	 */
	public static UserAccount findByTokenHash(String tokenHash) {
		if (tokenHash == null) {
			return null;
		}
		for (UserAccount account : cached().values()) {
			if (account.getTokens().stream().anyMatch(t -> tokenHash.equals(t.getTokenHash()))) {
				return account;
			}
		}
		return null;
	}

	public static UserAccount save(UserAccount account, AuditOrigin origin) {
		UserAccount cachedBefore = findById(account.getId());
		UserAccount before = cachedBefore != null ? cachedBefore.copy() : null;
		boolean admin = account.isBuiltInAdmin() || (before != null && before.isBuiltInAdmin());
		AccessMode modeBefore = admin ? modeInForce() : null;
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
		audit(origin, List.of(new Change(before, saved.copy())));
		// the admin password decides whether an audit key brings accounts mode into force
		AccessMode modeAfter = admin ? modeInForce() : null;
		if (modeBefore != null && modeAfter != null && modeBefore != modeAfter) {
			AuthAudit.accessModeChanged(origin, modeBefore.name(), modeAfter.name());
		}
		return saved;
	}

	public static void delete(UserAccount account, AuditOrigin origin) {
		UserAccount cachedBefore = findById(account.getId());
		UserAccount before = cachedBefore != null ? cachedBefore.copy() : account.copy();
		boolean removed = JPAService.runInTransaction(em -> {
			UserAccount managed = em.find(UserAccount.class, account.getId());
			if (managed != null) {
				em.remove(managed);
				return true;
			}
			return false;
		});
		invalidate();
		if (removed) {
			audit(origin, List.of(new Change(before, null)));
		}
	}

	/**
	 * Replaces all accounts as part of an account-bearing competition import. Imports are not audited account by
	 * account; the import itself is the audited event.
	 */
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

	public static void renamePlatform(String oldName, String newName, AuditOrigin origin) {
		updateGrants(grant -> Objects.equals(grant.getPlatformName(), oldName)
		        ? new RoleGrant(grant.getRole(), newName)
		        : grant, origin);
	}

	public static void removePlatform(String name, AuditOrigin origin) {
		updateGrants(grant -> Objects.equals(grant.getPlatformName(), name) ? null : grant, origin);
	}

	public static boolean adminHasPassword() {
		UserAccount admin = findByUsername(UserAccount.BUILT_IN_ADMIN);
		return admin != null && admin.hasPassword();
	}

	public static synchronized void ensureRoleAccounts(AuditOrigin origin) {
		initializeRoleAccounts(false, origin);
	}

	/**
	 * Brings the generic accounts in line with the current platforms, keeping passwords: an account that still
	 * fits a role and platform is kept and renamed if needed, missing accounts are created, and generic accounts
	 * that fit nothing are deleted. The built-in admin and user accounts are never touched.
	 */
	public static synchronized List<UserAccount> recomputeGenericAccounts(AuditOrigin origin) {
		initializeRoleAccounts(true, origin);
		return findAll();
	}

	private static void initializeRoleAccounts(boolean removeUnmatched, AuditOrigin origin) {
		List<Change> changes = JPAService.runInTransaction(em -> {
			List<UserAccount> existing = em.createQuery("select a from UserAccount a", UserAccount.class).getResultList();
			Map<Long, UserAccount> before = existing.stream()
			        .collect(Collectors.toMap(UserAccount::getId, UserAccount::copy));
			List<Change> result = new ArrayList<>();
			List<String> platforms = em.createQuery("select p from Platform p order by p.id", Platform.class)
			        .getResultList().stream().map(Platform::getName).toList();
			List<UserAccount> unmatched = removeUnmatched ? unmatchedGenericAccounts(existing, platforms) : List.of();
			for (UserAccount account : unmatched) {
				em.remove(account);
				result.add(new Change(before.get(account.getId()), null));
				logger.info("generic account removed username={}", account.getUsername());
			}
			em.flush();
			List<UserAccount> remaining = existing.stream().filter(a -> !unmatched.contains(a)).toList();
			List<UserAccount> missing = missingRoleAccounts(remaining, platforms);
			for (UserAccount account : remaining) {
				result.add(new Change(before.get(account.getId()), account.copy()));
			}
			// renames first, so that a new account may take a name an existing one just gave up
			em.flush();
			for (UserAccount account : missing) {
				em.persist(account);
				result.add(new Change(null, account.copy()));
				logger.info("role account created username={} (password required)", account.getUsername());
			}
			return result;
		});
		invalidate();
		audit(origin, changes);
	}

	/** One generic account per role and platform scope; generated names depend on the number of platforms. */
	private record ExpectedAccount(RoleGrant grant, String username) {
	}

	private static List<ExpectedAccount> expectedAccounts(List<String> platforms) {
		List<ExpectedAccount> expected = new ArrayList<>();
		for (Role role : Role.values()) {
			// the rules-defined Competition Director often does not use the system; grant it by hand when needed
			if (!role.isGrantable() || role == Role.COMPETITION_DIRECTOR) {
				continue;
			}
			List<String> scopes = role.isPlatformScopable() ? platforms : Collections.singletonList(null);
			for (String platform : scopes) {
				String username = role.name().toLowerCase(Locale.ROOT)
				        + (platform == null || platforms.size() == 1 ? "" : "-" + platform);
				expected.add(new ExpectedAccount(new RoleGrant(role, platform), username));
			}
		}
		return expected;
	}

	/** Generic accounts (other than the admin) whose single grant matches the expected role and platform. */
	private static Map<RoleGrant, UserAccount> genericByGrant(List<UserAccount> accounts) {
		Map<RoleGrant, UserAccount> result = new HashMap<>();
		for (UserAccount account : accounts) {
			if (account.isGeneric() && !account.isBuiltInAdmin() && account.getGrants().size() == 1) {
				result.putIfAbsent(account.getGrants().get(0), account);
			}
		}
		return result;
	}

	static List<UserAccount> unmatchedGenericAccounts(List<UserAccount> existing, List<String> platforms) {
		Map<RoleGrant, UserAccount> byGrant = genericByGrant(existing);
		List<UserAccount> matched = expectedAccounts(platforms).stream().map(e -> byGrant.get(e.grant()))
		        .filter(Objects::nonNull).toList();
		return existing.stream()
		        .filter(a -> a.isGeneric() && !a.isBuiltInAdmin() && !matched.contains(a))
		        .toList();
	}

	/**
	 * Renames the generic accounts whose generated name changed (a platform was added, removed or renamed), keeping
	 * their password, and returns the accounts to create.
	 */
	static List<UserAccount> missingRoleAccounts(List<UserAccount> existing, List<String> platforms) {
		Map<String, UserAccount> byUsername = accountsByUsername(existing);
		Map<RoleGrant, UserAccount> byGrant = genericByGrant(existing);
		List<UserAccount> missing = new ArrayList<>();
		for (ExpectedAccount expected : expectedAccounts(platforms)) {
			String username = expected.username();
			String key = UserAccount.normalizeUsername(username);
			UserAccount holder = byUsername.get(key);
			UserAccount generic = byGrant.get(expected.grant());
			if (generic != null) {
				String oldKey = UserAccount.normalizeUsername(generic.getUsername());
				if (!oldKey.equals(key)) {
					if (holder == null) {
						logger.info("generic account renamed {} -> {}", generic.getUsername(), username);
						byUsername.remove(oldKey);
						generic.setUsername(username);
						byUsername.put(key, generic);
					} else {
						logger./**/warn("generic account {} not renamed: username {} is taken", generic.getUsername(),
						        username);
					}
				}
				continue;
			}
			if (holder != null) {
				if (!holder.getGrants().contains(expected.grant())) {
					logger./**/warn("role account username={} already exists with different grants; left unchanged",
					        username);
				}
				continue;
			}
			UserAccount account = new UserAccount();
			account.setUsername(username);
			account.setGeneric(true);
			account.getGrants().add(expected.grant());
			missing.add(account);
			byUsername.put(key, account);
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
	public static void ensureBuiltInAdmin(AuditOrigin origin) {
		UserAccount admin = findByUsername(UserAccount.BUILT_IN_ADMIN);
		if (admin == null) {
			UserAccount created = new UserAccount();
			created.setUsername(UserAccount.BUILT_IN_ADMIN);
			created.setGeneric(true);
			created.getGrants().add(new RoleGrant(Role.ADMIN, null));
			save(created, origin);
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
			save(repaired, origin);
			logger.warn("built-in admin account repaired");
		}
	}

	public static void invalidate() {
		cache = null;
	}

	private static void updateGrants(Function<RoleGrant, RoleGrant> change, AuditOrigin origin) {
		List<Change> changes = JPAService.runInTransaction(em -> {
			List<Change> result = new ArrayList<>();
			List<UserAccount> all = em.createQuery("select a from UserAccount a", UserAccount.class).getResultList();
			for (UserAccount account : all) {
				List<RoleGrant> updated = new ArrayList<>();
				for (RoleGrant grant : account.getGrants()) {
					RoleGrant changed = change.apply(grant);
					if (changed != null) {
						updated.add(changed);
					}
				}
				if (!updated.equals(account.getGrants())) {
					UserAccount before = account.copy();
					account.getGrants().clear();
					account.getGrants().addAll(updated);
					result.add(new Change(before, account.copy()));
				}
			}
			return result;
		});
		invalidate();
		audit(origin, changes);
	}

	/** One account before and after a change; null before for a creation, null after for a deletion. */
	private record Change(UserAccount before, UserAccount after) {
	}

	private static void audit(AuditOrigin origin, List<Change> changes) {
		for (Change change : changes) {
			String detail = UserAccount.auditDetail(change.before(), change.after());
			if (detail != null) {
				String action = change.before() == null ? "added" : change.after() == null ? "deleted" : "updated";
				AuthAudit.accountChanged(origin, action, detail);
			}
		}
	}

	private static AccessMode modeInForce() {
		try {
			return Config.getCurrent().getParamAccessMode();
		} catch (RuntimeException e) {
			// no configuration yet (early startup, some tests): nothing to compare
			return null;
		}
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
