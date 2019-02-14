package org.ledocte.owlcms.data.jpa;

import static org.hibernate.cfg.AvailableSettings.CACHE_REGION_FACTORY;
import static org.hibernate.cfg.AvailableSettings.DIALECT;
import static org.hibernate.cfg.AvailableSettings.GENERATE_STATISTICS;
import static org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_DRIVER;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_PASSWORD;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_URL;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_USER;
import static org.hibernate.cfg.AvailableSettings.QUERY_STARTUP_CHECKING;
import static org.hibernate.cfg.AvailableSettings.SHOW_SQL;
import static org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE;
import static org.hibernate.cfg.AvailableSettings.USE_QUERY_CACHE;
import static org.hibernate.cfg.AvailableSettings.USE_REFLECTION_OPTIMIZER;
import static org.hibernate.cfg.AvailableSettings.USE_SECOND_LEVEL_CACHE;
import static org.hibernate.cfg.AvailableSettings.USE_STRUCTURED_CACHE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.ledocte.owlcms.data.athlete.Athlete;
import org.ledocte.owlcms.data.category.Category;
import org.ledocte.owlcms.data.category.CategoryRepository;
import org.ledocte.owlcms.data.competition.Competition;
import org.ledocte.owlcms.data.group.Group;
import org.ledocte.owlcms.data.platform.Platform;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import ch.qos.logback.classic.Logger;

public class JPAService {

	@WebListener
	public static class ContextListener implements ServletContextListener {

		@Override
		public void contextDestroyed(ServletContextEvent sce) {
			close();
		}

		@Override
		public void contextInitialized(ServletContextEvent sce) {
			init(Boolean.getBoolean("testMode"));
		}
	}

	protected static final Logger logger = (Logger) LoggerFactory.getLogger(JPAService.class);

	protected static EntityManagerFactory factory;

	private static boolean testMode;

	/**
	 * @return the testMode
	 */
	public static boolean isTestMode() {
		return testMode;
	}

	public static void close() {
		factory.close();
	}

	protected static void createInitialData() {
		logger.info("Creating initial data. {}", (isTestMode() ? "(test mode)" : ""));
		CategoryRepository.insertStandardCategories();
		TestData.insertInitialData(5, true);
	}

	protected static List<String> entityClassNames() {
		ImmutableList<String> vals = new ImmutableList.Builder<String>()
			.add(Group.class.getName())
			.add(Category.class.getName())
			.add(Athlete.class.getName())
			.add(Platform.class.getName())
			.add(Competition.class.getName())
			.build();
		return vals;
	}

	public static EntityManagerFactory getFactory() {
		if (factory == null) {
			init(isTestMode());
		}
		return factory;
	}

	public static void init(boolean testMode2) {
		if (factory == null) {
			factory = getFactoryFromCode(testMode2);
			createInitialData();
		}
	}

	public static EntityManagerFactory getFactoryFromCode(boolean testMode2) {
		PersistenceUnitInfo persistenceUnitInfo = new PersistenceUnitInfoImpl(
				JPAService.class.getSimpleName(),
				entityClassNames(),
				(testMode ? testProperties() : prodProperties()));
		Map<String, Object> configuration = new HashMap<>();

		factory = new EntityManagerFactoryBuilderImpl(
				new PersistenceUnitInfoDescriptor(persistenceUnitInfo),
				configuration).build();
		return factory;
	}

	private static Properties prodProperties() {
		ImmutableMap<String, Object> vals = jpaProperties();
		Properties props = new Properties();
		props.putAll(vals);
		props.put(JPA_JDBC_URL, "jdbc:h2:mem:test");
		props.put(JPA_JDBC_DRIVER, org.h2.Driver.class.getName());
		props.put(JPA_JDBC_USER, "sa");
		props.put(JPA_JDBC_PASSWORD, "");
		return props;
	}

	protected static Properties testProperties() {
		ImmutableMap<String, Object> vals = jpaProperties();
		Properties props = new Properties();
		props.putAll(vals);
		props.put(JPA_JDBC_URL, "jdbc:h2:mem:test");
		props.put(JPA_JDBC_DRIVER, org.h2.Driver.class.getName());
		props.put(JPA_JDBC_USER, "sa");
		props.put(JPA_JDBC_PASSWORD, "");
		return props;
	}

	private static ImmutableMap<String, Object> jpaProperties() {
		ImmutableMap<String, Object> vals = new ImmutableMap.Builder<String, Object>()
			.put(DIALECT, H2Dialect.class.getName())
			.put(HBM2DDL_AUTO, "update")
			.put(SHOW_SQL, false)
			.put(QUERY_STARTUP_CHECKING, false)
			.put(GENERATE_STATISTICS, false)
			.put(USE_REFLECTION_OPTIMIZER, false)
			.put(USE_SECOND_LEVEL_CACHE, true)
			.put(USE_QUERY_CACHE, false)
			.put(USE_STRUCTURED_CACHE, false)
			.put(STATEMENT_BATCH_SIZE, 20)
			.put(CACHE_REGION_FACTORY, "org.hibernate.cache.jcache.JCacheRegionFactory")
			.put("hibernate.javax.cache.provider", "org.ehcache.jsr107.EhcacheCachingProvider")
			.put("hibernate.javax.cache.missing_cache_strategy", "create")
			.put("javax.persistence.schema-generation.database.action", "update")
			.put("javax.persistence.sharedCache.mode", "ALL")
			.build();
		return vals;
	}

	public static <T> T runInTransaction(Function<EntityManager, T> function) {
		EntityManager entityManager = null;

		try {
			entityManager = factory.createEntityManager();
			entityManager.getTransaction()
				.begin();

			T result = function.apply(entityManager);

			entityManager.getTransaction()
				.commit();
			return result;

		} finally {
			if (entityManager != null) {
				entityManager.close();
			}
		}
	}

	public static void setTestMode(boolean b) {
		// TODO Auto-generated method stub

	}
}
