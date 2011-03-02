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
import org.exoplatform.services.jcr.config.MappedParametrizedObjectEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.TemplateConfigurationHelper;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.infinispan.Cache;
import org.infinispan.config.Configuration;
import org.infinispan.config.GlobalConfiguration;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;

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

   private final TemplateConfigurationHelper configurationHelper;

   private static final Log log = ExoLogger.getLogger("exo.jcr.component.core.InfinispanCacheFactory");

   /**
    * A Map that contains all the registered CacheManager order by {@link ExoContainer} 
    * instances and {@link GlobalConfiguration}.
    */
   private static Map<GlobalConfiguration, EmbeddedCacheManager> CACHE_MANAGERS =
      new HashMap<GlobalConfiguration, EmbeddedCacheManager>();

   /**
    * Creates InfinispanCacheFactory with provided configuration transaction managers.
    * Transaction manager will later be injected to cache instance. 
    * 
    * @param configurationManager
    */
   public ISPNCacheFactory(ConfigurationManager configurationManager)
   {
      this.configurationHelper = new ISPNCacheHelper(configurationManager);
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
      log.info("Infinispan Cache configuration used: " + configurationPath);
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

         manager = SecurityHelper.doPrivilegedIOExceptionAction(new PrivilegedExceptionAction<EmbeddedCacheManager>()
         {
            public EmbeddedCacheManager run() throws IOException
            {
               return getUniqueInstance(regionIdEscaped, new DefaultCacheManager(configStream));
            }
         });

      }
      catch (IOException e)
      {
         throw new RepositoryConfigurationException(e);
      }

      PrivilegedAction<Cache<K, V>> action = new PrivilegedAction<Cache<K, V>>()
      {
         public Cache<K, V> run()
         {
            return manager.getCache(regionIdEscaped);
         }
      };
      Cache<K, V> cache = AccessController.doPrivileged(action);

      return cache;
   }

   /**
    * Try to find if a {@link EmbeddedCacheManager} of the same type (i.e. their {@link GlobalConfiguration} are equals)
    * has already been registered for the same current container.
    * If no cache manager has been registered, we register the given cache manager otherwise we
    * use the previously registered cache manager and we define a dedicated region for the related cache.
    * @param regionId the unique id of the cache region to create
    * @param manager the current cache manager of the cache to create
    * @return the given cache manager if it has not been registered otherwise the cache manager of the same
    * type that has already been registered..
    */
   private static synchronized EmbeddedCacheManager getUniqueInstance(String regionId, EmbeddedCacheManager manager)
   {
      GlobalConfiguration gc = manager.getGlobalConfiguration();
      ExoContainer container = ExoContainerContext.getCurrentContainer();
      // Ensure that the cluster name won't be used between 2 ExoContainers
      gc.setClusterName(gc.getClusterName() + "_" + container.getContext().getName());
      Configuration conf = manager.getDefaultConfiguration();
      if (CACHE_MANAGERS.containsKey(gc))
      {
         manager = CACHE_MANAGERS.get(gc);
      }
      else
      {
         CACHE_MANAGERS.put(gc, manager);
         if (log.isInfoEnabled())
         {
            log.info("A new JBoss Cache instance has been registered for the region " + regionId
               + " and the container " + container.getContext().getName());
         }
      }
      // Define the configuration of the cache
      manager.defineConfiguration(regionId, conf);
      if (log.isInfoEnabled())
      {
         log.info("The region " + regionId + " has been registered for the container "
            + container.getContext().getName());
      }
      return manager;
   }
}
