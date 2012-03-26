/*
 * Copyright (C) 2010 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.services.jcr.infinispan;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.ispn.Utils;
import org.exoplatform.services.jcr.config.MappedParametrizedObjectEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.TemplateConfigurationHelper;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.Parser;
import org.infinispan.jmx.MBeanServerLookup;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.transaction.lookup.TransactionManagerLookup;

import java.io.IOException;
import java.io.InputStream;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.management.MBeanServer;
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

   private final ConfigurationManager configurationManager;

   private final TransactionManager transactionManager;

   private final TemplateConfigurationHelper configurationHelper;

   private static final Log LOG = ExoLogger//NOSONAR
      .getLogger("exo.jcr.component.core.impl.infinispan.v5.InfinispanCacheFactory");//NOSONAR

   /**
    * A Map that contains all the registered CacheManager order by cluster name.
    */
   private static Map<String, EmbeddedCacheManager> CACHE_MANAGERS = new HashMap<String, EmbeddedCacheManager>();

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
               Parser parser = new Parser(Thread.currentThread().getContextClassLoader());
               // Loads the configuration from the input stream
               ConfigurationBuilderHolder holder = parser.parse(configStream);
               GlobalConfigurationBuilder configBuilder = holder.getGlobalConfigurationBuilder();
               Utils.loadJGroupsConfig(configurationManager, configBuilder.build(), configBuilder);
               return getUniqueInstance(regionIdEscaped, holder, transactionManager);
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
            return manager.getCache(regionIdEscaped);
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
      ConfigurationBuilderHolder holder, final TransactionManager tm)
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
         manager = CACHE_MANAGERS.get(clusterName);
      }
      else
      {
         // Reset the manager before storing it into the map since the default config is used as
         // template to define a new configuration
         manager = new DefaultCacheManager(gc);
         CACHE_MANAGERS.put(clusterName, manager);
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
      Configuration conf = holder.getDefaultConfigurationBuilder().build();
      // Define the configuration of the cache
      manager.defineConfiguration(regionId, conf);
      if (LOG.isInfoEnabled())
      {
         LOG.info("The region " + regionId + " has been registered for the container "
            + container.getContext().getName());
      }
      return manager;
   }
}
