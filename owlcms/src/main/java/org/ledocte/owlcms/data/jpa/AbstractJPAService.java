package org.ledocte.owlcms.data.jpa;

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

import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.ledocte.owlcms.data.category.Category;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import ch.qos.logback.classic.Logger;

abstract public class AbstractJPAService {

	protected static EntityManagerFactory factory;
	protected static final Logger logger = (Logger) LoggerFactory.getLogger(AbstractJPAService.class);

	public static void init() {
		if (factory == null) {
			factory = getFactoryFromCode();
			createInitialData();
		}
	}

	protected static void createInitialData() {
	}
	
	public static void close() {
		factory.close();
	}

	public static EntityManagerFactory getFactory() {
		return factory;
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

	public static EntityManagerFactory getFactoryFromCode() {
	
		PersistenceUnitInfo persistenceUnitInfo = persistenceUnitInfo(
			JPAService.class.getSimpleName());
		Map<String, Object> configuration = new HashMap<>();
	
		factory = new EntityManagerFactoryBuilderImpl(
				new PersistenceUnitInfoDescriptor(persistenceUnitInfo),
				configuration).build();
		return factory;
	}

	protected static PersistenceUnitInfoImpl persistenceUnitInfo(String name) {
		return new PersistenceUnitInfoImpl(name, entityClassNames(), properties());
	}

	protected static Properties properties() {
		return null;
	}

	protected static List<String> entityClassNames() {
		ImmutableList<String> vals = new ImmutableList.Builder<String>()
			.add(Category.class.getName())
			.build();
		return vals;
	}


	

}