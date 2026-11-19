package app.owlcms.data.access;

import java.time.Instant;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import app.owlcms.access.RememberedBrowser.Kind;
import app.owlcms.data.jpa.JPAService;

public final class RememberedLoginRepository {

	private RememberedLoginRepository() {
	}

	public static RememberedLogin find(String selector) {
		return JPAService.runInTransaction(em -> em.find(RememberedLogin.class, selector));
	}

	public static RememberedLogin save(RememberedLogin rememberedLogin) {
		RememberedLogin saved = JPAService.runInTransaction(em -> em.merge(rememberedLogin));
		if (saved == null) {
			throw new IllegalStateException("Remembered login could not be saved");
		}
		return saved;
	}

	public static RememberedLogin updateLastUse(String selector, Instant now, String clientIp, String userAgent) {
		return JPAService.runInTransaction(em -> {
			RememberedLogin rememberedLogin = em.find(RememberedLogin.class, selector);
			if (rememberedLogin == null) {
				return null;
			}
			rememberedLogin.setLastUsedAt(now);
			rememberedLogin.setClientIp(clientIp);
			rememberedLogin.setUserAgent(userAgent);
			return rememberedLogin;
		});
	}

	public static void delete(String selector) {
		Boolean deleted = JPAService.runInTransaction(em -> {
			RememberedLogin rememberedLogin = em.find(RememberedLogin.class, selector);
			if (rememberedLogin == null) {
				return true;
			}
			em.remove(rememberedLogin);
			return true;
		});
		if (deleted == null) {
			throw new IllegalStateException("Remembered login could not be deleted");
		}
	}

	public static int deleteAll() {
		Integer deleted = JPAService.runInTransaction(RememberedLoginRepository::deleteAll);
		if (deleted == null) {
			throw new IllegalStateException("Remembered logins could not be deleted");
		}
		return deleted;
	}

	public static int deleteAll(EntityManager em) {
		return em.createQuery("delete from RememberedLogin").executeUpdate();
	}

	public static int deleteExpired(Instant now) {
		Integer deleted = JPAService.runInTransaction(em -> em
		        .createQuery("delete from RememberedLogin r where r.expiresAt <= :now")
		        .setParameter("now", now).executeUpdate());
		if (deleted == null) {
			throw new IllegalStateException("Expired remembered logins could not be deleted");
		}
		return deleted;
	}

	public static void evictBeyond(Kind kind, Long accountId, int maximum, String protectedSelector) {
		Boolean evicted = JPAService.runInTransaction(em -> {
			String where = kind == Kind.ACCOUNT ? "r.kind = :kind and r.accountId = :accountId" : "r.kind = :kind";
			TypedQuery<RememberedLogin> query = em
			        .createQuery("select r from RememberedLogin r where " + where
			                + " and r.selector <> :protected order by r.issuedAt desc",
			                RememberedLogin.class)
			        .setParameter("kind", kind).setParameter("protected", protectedSelector);
			if (kind == Kind.ACCOUNT) {
				query.setParameter("accountId", accountId);
			}
			List<RememberedLogin> rows = query.getResultList();
			if (rows.size() >= maximum) {
				rows.subList(maximum - 1, rows.size()).forEach(em::remove);
			}
			return true;
		});
		if (evicted == null) {
			throw new IllegalStateException("Old remembered logins could not be evicted");
		}
	}
}
