/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.data.jpa;

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

import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.platform.Platform;
import ch.qos.logback.classic.Logger;

/**
 * Class JPAService.
 */
public class JPAService {

	protected static final Logger logger = (Logger) LoggerFactory.getLogger(JPAService.class);

	protected static EntityManagerFactory factory;

	private static boolean memoryMode;

	/**
	 * @return true if running in memory
	 */
	public static boolean isMemoryMode() {
		return memoryMode;
	}

	/**
	 * Close.
	 */
	public static void close() {
		factory.close();
		factory = null;
	}

	/**
	 * Entity class names.
	 *
	 * @return the list
	 */
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

	/**
	 * @return the entity manager factory
	 */
	public static EntityManagerFactory getFactory() {
		if (factory == null) {
			init(isMemoryMode());
		}
		return factory;
	}

	/**
	 * Inits the database
	 *
	 * @param inMemory if true, start with in-memory database
	 */
	public static void init(boolean inMemory) {
		if (factory == null) {
			factory = getFactoryFromCode(inMemory);
		}
	}

	/**
	 * Gets the factory from code (without a persistance.xml file)
	 *
	 * @param memoryMode run from memory if true
	 * @return an entity manager factory
	 */
	private static EntityManagerFactory getFactoryFromCode(boolean testMode2) {
		PersistenceUnitInfo persistenceUnitInfo = new PersistenceUnitInfoImpl(
				JPAService.class.getSimpleName(),
				entityClassNames(),
				(memoryMode ? memoryProperties() : prodProperties()));
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
		props.put(JPA_JDBC_URL, "jdbc:h2:file:~/owlcms;DB_CLOSE_DELAY=-1;TRACE_LEVEL_FILE=4");
		props.put(JPA_JDBC_DRIVER, org.h2.Driver.class.getName());
		props.put(JPA_JDBC_USER, "sa");
		props.put(JPA_JDBC_PASSWORD, "");
		props.put("javax.persistence.schema-generation.database.action", "update");
		return props;
	}

	/**
	 * Test properties.
	 *
	 * @return the properties
	 */
	protected static Properties memoryProperties() {
		ImmutableMap<String, Object> vals = jpaProperties();
		Properties props = new Properties();
		props.putAll(vals);
		// keep the database even if all the connections have timed out
		props.put(JPA_JDBC_URL, "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
		props.put(JPA_JDBC_DRIVER, org.h2.Driver.class.getName());
		props.put(JPA_JDBC_USER, "sa");
		props.put(JPA_JDBC_PASSWORD, "");
		props.put("javax.persistence.schema-generation.database.action", "drop-and-create");
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
			.put("javax.persistence.sharedCache.mode", "ALL")
			.put("hibernate.c3p0.min_size", 5)
			.put("hibernate.c3p0.max_size", 20)
			.put("hibernate.c3p0.acquire_increment", 5)
			.put("hibernate.c3p0.timeout", 84200) //FIXME very high timeout value
			.put("hibernate.c3p0.preferredTestQuery","SELECT 1")
			.put("hibernate.c3p0.testConnectionOnCheckout",true)
			.put("hibernate.c3p0.idle_test_period",500)
			.build();
		return vals;
	}

	/**
	 * Run in transaction.
	 *
	 * @param <T> the generic type
	 * @param function the function
	 * @return the t
	 */
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


	/**
	 * Sets the in-memory mode.
	 *
	 * @param b the new test mode
	 */
	public static void setMemoryMode(boolean b) {
		memoryMode = b;
	}
}
