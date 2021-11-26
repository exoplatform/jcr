/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.infinispan;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.database.utils.JDBCUtils;
import org.exoplatform.services.ispn.Utils;
import org.exoplatform.services.jcr.config.MappedParametrizedObjectEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.TemplateConfigurationHelper;
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.StoreConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.jmx.MBeanServerLookup;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.persistence.jdbc.configuration.JdbcBinaryStoreConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfigurationBuilder;
import org.infinispan.transaction.lookup.TransactionManagerLookup;

import java.io.IOException;
import java.io.InputStream;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

import javax.jcr.RepositoryException;
import javax.management.MBeanServer;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;

/**
 * Factory that creates and starts pre-configured instances of Infinispan.
 * Path to Infinispan configuration or template and cache name should be 
 * provided as "infinispan-configuration" and "infinispan-cache-name" properties 
 * in parameterEntry instance respectively. 
 * 
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: InfinispanCacheFactory.java 3001 2010-08-30 06:56:05Z tolusha $
 *
 */
public class ISPNCacheFactory<K, V>
{

   public static final String INFINISPAN_CONFIG = "infinispan-configuration";

   public static final String STORE_JNDI_URL = "infinispan-cl-cache.jdbc.datasource";

   private final ConfigurationManager configurationManager;

   private final TransactionManager transactionManager;

   private final TemplateConfigurationHelper configurationHelper;

   private static final Log LOG = ExoLogger//NOSONAR
      .getLogger("exo.jcr.component.core.InfinispanCacheFactory");//NOSONAR

   /**
    * A Map that contains all the registered CacheManager order by cluster name.
    */
   private static Map<String, CacheManagerInstance> CACHE_MANAGERS = new HashMap<String, CacheManagerInstance>();

   private static final MBeanServerLookup MBEAN_SERVER_LOOKUP = new MBeanServerLookup()
   {
      public MBeanServer getMBeanServer(Properties properties)
      {
         return ExoContainerContext.getTopContainer().getMBeanServer();
      }
   };

   /**
    * Creates InfinispanCacheFactory with provided configuration transaction managers.
    * Transaction manager will later be injected to cache instance. 
    * 
    * @param configurationManager
    * @param transactionManager
    */
   public ISPNCacheFactory(ConfigurationManager configurationManager, TransactionManager transactionManager)
   {
      this.configurationManager = configurationManager;
      this.configurationHelper = new ISPNCacheHelper(configurationManager);
      this.transactionManager = transactionManager;
   }

   /**
    * Creates InfinispanCacheFactory with provided configuration transaction managers.
    * 
    * @param configurationManager
    */
   public ISPNCacheFactory(ConfigurationManager configurationManager)
   {
      this(configurationManager, null);
   }

   /**
   * Factory that creates and starts pre-configured instances of Infinispan.
   * Path to Infinispan configuration or template should be provided as 
   * "infinispan-configuration" property in parameterEntry instance. 
   * <br>
   * 
   * @param regionId the unique id of the cache region to create
   * @param parameterEntry
   * @return
   * @throws RepositoryConfigurationException
   */
   public Cache<K, V> createCache(final String regionId, MappedParametrizedObjectEntry parameterEntry)
      throws RepositoryConfigurationException
   {
      // get Infinispan configuration file path
      final String configurationPath = parameterEntry.getParameterValue(INFINISPAN_CONFIG);
      final String jndiUrl = parameterEntry.getParameterValue(STORE_JNDI_URL, null);
      LOG.info("Infinispan Cache configuration used: " + configurationPath);
      // avoid dashes in cache name. Some SQL servers doesn't allow dashes in table names
      final String regionIdEscaped = regionId.replace("-", "_");
      // prepare configuration
      final InputStream configStream;
      try
      {
         // fill template
         configStream = configurationHelper.fillTemplate(configurationPath, parameterEntry.getParameters());
      }
      catch (IOException e)
      {
         throw new RepositoryConfigurationException(e);
      }

      // create cache
      final EmbeddedCacheManager manager;
      try
      {
         // creating new CacheManager using SecurityHelper

         manager = SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<EmbeddedCacheManager>()
         {
            public EmbeddedCacheManager run() throws Exception
            {
               ParserRegistry parser = new ParserRegistry(Thread.currentThread().getContextClassLoader());
               // Loads the configuration from the input stream
               ConfigurationBuilderHolder holder = parser.parse(configStream);
               GlobalConfigurationBuilder configBuilder = holder.getGlobalConfigurationBuilder();
               Utils.loadJGroupsConfig(configurationManager, configBuilder.build(), configBuilder);
               return getUniqueInstance(regionIdEscaped, holder, transactionManager, jndiUrl);
            }
         });

      }
      catch (PrivilegedActionException pae)
      {
         Throwable cause = pae.getCause();
         throw new RepositoryConfigurationException(cause);
      }

      PrivilegedAction<Cache<K, V>> action = new PrivilegedAction<Cache<K, V>>()
      {
         public Cache<K, V> run()
         {
            Cache<K, V> cache = manager.getCache(regionIdEscaped);
            if (cache.getStatus() == ComponentStatus.TERMINATED)
            {
               cache.start();
               LOG.info("The cache corresponding to the region {} was in state Terminated, so it has been restarted",
                  regionIdEscaped);
            }
            return cache;
         }
      };
      Cache<K, V> cache = SecurityHelper.doPrivilegedAction(action);

      return cache;
   }

   /**
    * Try to find if a {@link EmbeddedCacheManager} of the same type (i.e. the cluster names are equal)
    * has already been registered for the same current container.
    * If no cache manager has been registered, we register the given cache manager otherwise we
    * use the previously registered cache manager and we define a dedicated region for the related cache.
    * @param regionId the unique id of the cache region to create
    * @param holder the configuration holder of the the cache to create
    * @param tm the transaction manager to put into the configuration of the cache
    * @return the given cache manager if it has not been registered otherwise the cache manager of the same
    * type that has already been registered..
    */
   private static synchronized EmbeddedCacheManager getUniqueInstance(String regionId,
      ConfigurationBuilderHolder holder, final TransactionManager tm, String jndiUrl)
   {
      GlobalConfigurationBuilder configBuilder = holder.getGlobalConfigurationBuilder();
      GlobalConfiguration gc = configBuilder.build();
      ExoContainer container = ExoContainerContext.getCurrentContainer();
      // Ensure that the cluster name won't be used between 2 ExoContainers
      configBuilder.transport().clusterName(gc.transport().clusterName() + "_" + container.getContext().getName())
         .globalJmxStatistics()
         .cacheManagerName(gc.globalJmxStatistics().cacheManagerName() + "_" + container.getContext().getName()).
         // Configure the MBeanServerLookup
         mBeanServerLookup(MBEAN_SERVER_LOOKUP);
      EmbeddedCacheManager manager;
      gc = configBuilder.build();
      String clusterName = gc.transport().clusterName();
      if (CACHE_MANAGERS.containsKey(clusterName))
      {
         CacheManagerInstance cacheManagerInstance = CACHE_MANAGERS.get(clusterName);
         cacheManagerInstance.acquire();
         manager = cacheManagerInstance.cacheManager;
      }
      else
      {
         // Reset the manager before storing it into the map since the default config is used as
         // template to define a new configuration
         manager = new DefaultCacheManager(gc);
         CacheManagerInstance cacheManagerInstance = new CacheManagerInstance(manager);
         cacheManagerInstance.acquire();
         CACHE_MANAGERS.put(clusterName, cacheManagerInstance);
         if (LOG.isInfoEnabled())
         {
            LOG.info("A new ISPN Cache Manager instance has been registered for the region " + regionId
               + " and the container " + container.getContext().getName());
         }
      }
      ConfigurationBuilder confBuilder = holder.getDefaultConfigurationBuilder();
      if (tm != null)
      {
         TransactionManagerLookup tml = new TransactionManagerLookup()
         {
            public TransactionManager getTransactionManager() throws Exception
            {
               return tm;
            }
         };
         confBuilder.transaction().transactionManagerLookup(tml);
      }


      ConfigurationBuilder configurationBuilder = holder.getDefaultConfigurationBuilder();

      //Configure ManagedConnectionFactory
      List storeConfigurationBuilderList = configurationBuilder.persistence().stores();
      if(storeConfigurationBuilderList != null && ! storeConfigurationBuilderList.isEmpty())
      {
         for (StoreConfigurationBuilder storeBuilder : configurationBuilder.persistence().stores())
         {
            if(storeBuilder instanceof JdbcBinaryStoreConfigurationBuilder)
            {
               JdbcBinaryStoreConfigurationBuilder storeConfigurationBuilder =
                       (JdbcBinaryStoreConfigurationBuilder) storeConfigurationBuilderList.get(0);
               storeConfigurationBuilder.connectionFactory(ManagedConnectionFactoryConfigurationBuilder.class).jndiUrl(jndiUrl);
            }
            else if( storeBuilder instanceof JdbcStringBasedStoreConfigurationBuilder)
            {
               JdbcStringBasedStoreConfigurationBuilder storeConfigurationBuilder =
                       (JdbcStringBasedStoreConfigurationBuilder) storeConfigurationBuilderList.get(0);
               storeConfigurationBuilder.connectionFactory(ManagedConnectionFactoryConfigurationBuilder.class).jndiUrl(jndiUrl);
            }
         }
      }
      
      Configuration conf = configurationBuilder.build();
      // Define the configuration of the cache
      manager.defineConfiguration(regionId, new ConfigurationBuilder().read(conf).build());
      if (LOG.isInfoEnabled())
      {
         LOG.info("The region " + regionId + " has been registered for the container "
            + container.getContext().getName());
      }
      return manager;
   }

   public static synchronized void releaseUniqueInstance(EmbeddedCacheManager cacheManager)
   {
      Iterator<Entry<String, CacheManagerInstance>> iterator = CACHE_MANAGERS.entrySet().iterator();
      while (iterator.hasNext())
      {
         Entry<String, CacheManagerInstance> next = iterator.next();
         if (next.getValue().isSame(cacheManager))
         {
            CacheManagerInstance cacheManagerInstance = next.getValue();
            cacheManagerInstance.release();
            if (!cacheManagerInstance.hasReferences())
            {
               cacheManagerInstance.cacheManager.stop();
               iterator.remove();
            }
            return;
         }
      }
   }

   /**
    * If a cache store is used, then fills-in column types. If column type configured from jcr-configuration file,
    * then nothing is overridden. Parameters are injected into the given parameterEntry.
    */
   public static void configureCacheStore(MappedParametrizedObjectEntry parameterEntry,
                                          String dataSourceParamName, String dataColumnParamName, String idColumnParamName, String timeColumnParamName, String dialectParamName)
      throws RepositoryException
   {
      String dataSourceName = parameterEntry.getParameterValue(dataSourceParamName, null);

      String dialect = parameterEntry.getParameterValue(dialectParamName, DBConstants.DB_DIALECT_AUTO).toUpperCase();

      // if data source is defined, then inject correct data-types.
      // Also it cans be not defined and nothing should be injected
      // (i.e. no cache loader is used (possibly pattern is changed, to used another cache loader))
      DataSource dataSource;
      try
      {
         dataSource = (DataSource)new InitialContext().lookup(dataSourceName);
      }
      catch (NamingException e)
      {
         throw new RepositoryException(e.getMessage(), e);
      }

      String blobType;
      String charType;
      String timeStampType;
      try
      {
         blobType = JDBCUtils.getAppropriateBlobType(dataSource);
         if (dialect.startsWith(DBConstants.DB_DIALECT_MYSQL) && dialect.endsWith("-UTF8"))
         {
            charType = "VARCHAR(255)";
         }
         else
         {
            charType = JDBCUtils.getAppropriateCharType(dataSource);
         }
         timeStampType = JDBCUtils.getAppropriateTimestamp(dataSource);
      }
      catch (SQLException e)
      {
         throw new RepositoryException(e.getMessage(), e);
      }

      // set parameters if not defined
      // if parameter is missing in configuration, then
      // getParameterValue(INFINISPAN_JDBC_CL_DATA_COLUMN, INFINISPAN_JDBC_CL_AUTO)
      // will return INFINISPAN_JDBC_CL_AUTO. If parameter is present in configuration and
      //equals to "auto", then it should be replaced
      // with correct value for given database
      if (parameterEntry.getParameterValue(dataColumnParamName, "auto").equalsIgnoreCase("auto"))
      {
         parameterEntry.putParameterValue(dataColumnParamName, blobType);
      }

      if (parameterEntry.getParameterValue(idColumnParamName, "auto").equalsIgnoreCase("auto"))
      {
         parameterEntry.putParameterValue(idColumnParamName, charType);
      }

      if (parameterEntry.getParameterValue(timeColumnParamName, "auto").equalsIgnoreCase("auto"))
      {
         parameterEntry.putParameterValue(timeColumnParamName, timeStampType);
      }
   }

   /**
    * This class is used to store the actual amount of times cache was used.
    */
   private static class CacheManagerInstance
   {
      private final EmbeddedCacheManager cacheManager;

      private int references;

      public CacheManagerInstance(EmbeddedCacheManager cache)
      {
         this.cacheManager = cache;
      }

      private void acquire()
      {
         references++;
      }

      private void release()
      {
         references--;
      }

      private boolean hasReferences()
      {
         return references > 0;
      }

      private boolean isSame(EmbeddedCacheManager cacheManager)
      {
         return this.cacheManager == cacheManager;
      }
   }
}
