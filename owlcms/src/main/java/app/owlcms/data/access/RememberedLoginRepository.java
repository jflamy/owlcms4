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
		return JPAService.runInTransaction(em -> em.merge(rememberedLogin));
	}

	public static void delete(String selector) {
		JPAService.runInTransaction(em -> {
			delete(em, selector);
			return null;
		});
	}

	public static int deleteAll() {
		return JPAService.runInTransaction(RememberedLoginRepository::deleteAll);
	}

	public static int deleteAll(EntityManager em) {
		return em.createQuery("delete from RememberedLogin").executeUpdate();
	}

	public static int deleteExpired(Instant now) {
		return JPAService.runInTransaction(em -> em
		        .createQuery("delete from RememberedLogin r where r.expiresAt <= :now")
		        .setParameter("now", now).executeUpdate());
	}

	public static void evictBeyond(Kind kind, Long accountId, int maximum) {
		JPAService.runInTransaction(em -> {
			String where = kind == Kind.ACCOUNT ? "r.kind = :kind and r.accountId = :accountId" : "r.kind = :kind";
			TypedQuery<RememberedLogin> query = em
			        .createQuery("select r from RememberedLogin r where " + where + " order by r.issuedAt desc",
			                RememberedLogin.class)
			        .setParameter("kind", kind);
			if (kind == Kind.ACCOUNT) {
				query.setParameter("accountId", accountId);
			}
			List<RememberedLogin> rows = query.getResultList();
			if (rows.size() > maximum) {
				rows.subList(maximum, rows.size()).forEach(em::remove);
			}
			return null;
		});
	}

	private static void delete(EntityManager em, String selector) {
		RememberedLogin rememberedLogin = em.find(RememberedLogin.class, selector);
		if (rememberedLogin != null) {
			em.remove(rememberedLogin);
		}
	}

}
