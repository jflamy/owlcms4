/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.jpa;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.hibernate.jpa.HibernatePersistenceProvider;

/**
 * <code>AbstractPersistenceUnitInfo</code> - Base PersistenceUnitInfo.
 *
 * @author Vlad Mihalcea
 */
public class PersistenceUnitInfoImpl implements PersistenceUnitInfo {

    /**
     * New JTA persistence unit info.
     *
     * @param persistenceUnitName the persistence unit name
     * @param mappingFileNames    the mapping file names
     * @param properties          the properties
     * @param jtaDataSource       the jta data source
     * @return the persistence unit info impl
     */
    public static PersistenceUnitInfoImpl newJTAPersistenceUnitInfo(String persistenceUnitName,
            List<String> mappingFileNames, Properties properties, DataSource jtaDataSource) {
        PersistenceUnitInfoImpl persistenceUnitInfo = new PersistenceUnitInfoImpl(persistenceUnitName, mappingFileNames,
                properties);
        persistenceUnitInfo.jtaDataSource = jtaDataSource;
        persistenceUnitInfo.nonJtaDataSource = null;
        persistenceUnitInfo.transactionType = PersistenceUnitTransactionType.JTA;
        return persistenceUnitInfo;
    }

    private DataSource jtaDataSource;

    private final List<String> managedClassNames;

    private DataSource nonJtaDataSource;

    private final String persistenceUnitName;

    private final Properties properties;

    private PersistenceUnitTransactionType transactionType = PersistenceUnitTransactionType.RESOURCE_LOCAL;

    /**
     * Instantiates a new persistence unit info impl.
     *
     * @param persistenceUnitName the persistence unit name
     * @param managedClassNames   the managed class names
     * @param properties          the properties
     */
    public PersistenceUnitInfoImpl(String persistenceUnitName, List<String> managedClassNames, Properties properties) {
        this.persistenceUnitName = persistenceUnitName;
        this.managedClassNames = managedClassNames;
        this.properties = properties;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.persistence.spi.PersistenceUnitInfo#addTransformer(javax.persistence. spi.ClassTransformer)
     */
    @Override
    public void addTransformer(ClassTransformer transformer) {

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.persistence.spi.PersistenceUnitInfo#excludeUnlistedClasses()
     */
    @Override
    public boolean excludeUnlistedClasses() {
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.persistence.spi.PersistenceUnitInfo#getClassLoader()
     */
    @Override
    public ClassLoader getClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.persistence.spi.PersistenceUnitInfo#getJarFileUrls()
     */
    @Override
    public List<URL> getJarFileUrls() {
        return Collections.emptyList();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.persistence.spi.PersistenceUnitInfo#getJtaDataSource()
     */
    @Override
    public DataSource getJtaDataSource() {
        return jtaDataSource;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.persistence.spi.PersistenceUnitInfo#getManagedClassNames()
     */
    @Override
    public List<String> getManagedClassNames() {
        return managedClassNames;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.persistence.spi.PersistenceUnitInfo#getMappingFileNames()
     */
    @Override
    public List<String> getMappingFileNames() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.persistence.spi.PersistenceUnitInfo#getNewTempClassLoader()
     */
    @Override
    public ClassLoader getNewTempClassLoader() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.persistence.spi.PersistenceUnitInfo#getNonJtaDataSource()
     */
    @Override
    public DataSource getNonJtaDataSource() {
        return nonJtaDataSource;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.persistence.spi.PersistenceUnitInfo#getPersistenceProviderClassName()
     */
    @Override
    public String getPersistenceProviderClassName() {
        return HibernatePersistenceProvider.class.getName();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.persistence.spi.PersistenceUnitInfo#getPersistenceUnitName()
     */
    @Override
    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.persistence.spi.PersistenceUnitInfo#getPersistenceUnitRootUrl()
     */
    @Override
    public URL getPersistenceUnitRootUrl() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.persistence.spi.PersistenceUnitInfo#getPersistenceXMLSchemaVersion()
     */
    @Override
    public String getPersistenceXMLSchemaVersion() {
        return "2.1";
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.persistence.spi.PersistenceUnitInfo#getProperties()
     */
    @Override
    public Properties getProperties() {
        return properties;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.persistence.spi.PersistenceUnitInfo#getSharedCacheMode()
     */
    @Override
    public SharedCacheMode getSharedCacheMode() {
        return SharedCacheMode.UNSPECIFIED;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.persistence.spi.PersistenceUnitInfo#getTransactionType()
     */
    @Override
    public PersistenceUnitTransactionType getTransactionType() {
        return transactionType;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.persistence.spi.PersistenceUnitInfo#getValidationMode()
     */
    @Override
    public ValidationMode getValidationMode() {
        return ValidationMode.AUTO;
    }
}