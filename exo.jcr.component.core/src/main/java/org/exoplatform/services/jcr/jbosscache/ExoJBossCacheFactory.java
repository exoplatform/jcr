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
package org.exoplatform.services.jcr.jbosscache;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.jcr.config.MappedParametrizedObjectEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.TemplateConfigurationHelper;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.jboss.cache.Cache;
import org.jboss.cache.CacheFactory;
import org.jboss.cache.DefaultCacheFactory;
import org.jboss.cache.Fqn;
import org.jboss.cache.Region;
import org.jboss.cache.config.Configuration;
import org.jboss.cache.config.EvictionConfig;
import org.jboss.cache.config.EvictionRegionConfig;
import org.jgroups.JChannelFactory;

import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import javax.transaction.TransactionManager;

/**
 * Factory that creates pre-configured instances of JBossCache, without starting it.
 * Path to JBossCache configuration or template should be provided as 
 * "jbosscache-configuration" property in parameterEntry instance. If
 * transaction manager is configure in ExoJBossCacheFactory, then it
 * is injected into the cache instance. 
 * <br>
 * If parameterEntry has "jgroups-multiplexer-stack" (=true) and 
 * "jgroups-configuration" parameters then Multiplexing stack is enabled
 * in JBossCache (this is highly recommended by RH specialists).
 * 
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: ExoCacheFactoryImpl.java 34360 2009-07-22 23:58:59Z nzamosenchuk $
 *
 */
public class ExoJBossCacheFactory<K, V>
{

   public static final String JBOSSCACHE_CONFIG = "jbosscache-configuration";

   public static final String JGROUPS_CONFIG = "jgroups-configuration";

   public static final String JGROUPS_MUX_ENABLED = "jgroups-multiplexer-stack";

   /**
    * A Map that contains all the registered JBC instances, ordered by
    * {@link ExoContainer} instances, {@link CacheType} and JBC Configuration.
    */
   private static Map<ExoContainer, Map<CacheType, Map<Configuration, Cache>>> CACHES =
      new HashMap<ExoContainer, Map<CacheType, Map<Configuration, Cache>>>();

   private final TemplateConfigurationHelper configurationHelper;

   private final TransactionManager transactionManager;

   private ConfigurationManager configurationManager;

   private static final Log log = ExoLogger.getLogger("exo.jcr.component.core.ExoJBossCacheFactory");

   /**
    * Creates ExoJbossCacheFactory with provided configuration transaction managers.
    * Transaction manager will later be injected to cache instance. 
    * 
    * @param configurationManager
    * @param transactionManager
    */
   public ExoJBossCacheFactory(ConfigurationManager configurationManager, TransactionManager transactionManager)
   {
      this.configurationManager = configurationManager;
      this.configurationHelper = TemplateConfigurationHelper.createJBossCacheHelper(configurationManager);
      this.transactionManager = transactionManager;
   }

   /**
    * Creates ExoJbossCacheFactory with provided configuration manager and without transaction manager. 
    * 
    * @param configurationManager
    */
   public ExoJBossCacheFactory(ConfigurationManager configurationManager)
   {
      this(configurationManager, null);
   }

   /**
    * Creates pre-configured instance of JBossCache, without starting it.
    * Path to JBossCache configuration or template should be provided as 
    * "jbosscache-configuration" property in parameterEntry instance. If
    * transaction manager is configure in ExoJBossCacheFactory, then it
    * is injected into the cache instance. 
    * <br>
    * If parameterEntry has "jgroups-multiplexer-stack" (=true) and 
    * "jgroups-configuration" parameters then Multiplexing stack is enabled
    * in JBossCache (this is highly recommended by RH specialists).
    * 
    * @param parameterEntry
    * @return
    * @throws RepositoryConfigurationException
    */
   public Cache<K, V> createCache(MappedParametrizedObjectEntry parameterEntry) throws RepositoryConfigurationException
   {
      // get JBossCache configuration file path
      String jBossCacheConfigurationPath = parameterEntry.getParameterValue(JBOSSCACHE_CONFIG);
      log.info("JBoss Cache configuration used: " + jBossCacheConfigurationPath);

      // prepare configuration
      InputStream configStream;
      try
      {
         // fill template
         configStream = configurationHelper.fillTemplate(jBossCacheConfigurationPath, parameterEntry.getParameters());
      }
      catch (IOException e)
      {
         throw new RepositoryConfigurationException(e);
      }

      // create cache
      final CacheFactory<K, V> factory = new DefaultCacheFactory<K, V>();
      final InputStream stream = configStream;

      PrivilegedAction<Cache<K, V>> action = new PrivilegedAction<Cache<K, V>>()
      {
         public Cache<K, V> run()
         {
            return factory.createCache(stream, false);
         }
      };
      Cache<K, V> cache = AccessController.doPrivileged(action);

      // inject transaction manager if defined
      if (transactionManager != null)
      {
         cache.getConfiguration().getRuntimeConfig().setTransactionManager(transactionManager);
      }

      // JGroups multiplexer configuration if enabled
      if (parameterEntry.getParameterBoolean(JGROUPS_MUX_ENABLED, false))
      {
         try
         {
            // Get path to JGroups configuration
            String jgroupsConfigurationFilePath = parameterEntry.getParameterValue(JGROUPS_CONFIG);
            if (jgroupsConfigurationFilePath != null)
            {
               // Create and inject multiplexer factory
               JChannelFactory muxFactory = new JChannelFactory();
               muxFactory.setMultiplexerConfig(configurationManager.getResource(jgroupsConfigurationFilePath));

               cache.getConfiguration().getRuntimeConfig().setMuxChannelFactory(muxFactory);
               log.info("Multiplexer stack successfully enabled for the cache.");
            }
         }
         catch (Exception e)
         {
            // exception occurred setting mux factory
            e.printStackTrace();
            throw new RepositoryConfigurationException("Error setting multiplexer configuration.", e);
         }
      }
      else
      {
         // Multiplexer is not enabled. If jGroups configuration preset it is applied
         String jgroupsConfigurationFilePath = parameterEntry.getParameterValue(JGROUPS_CONFIG, null);
         if (jgroupsConfigurationFilePath != null)
         {
            try
            {
               cache.getConfiguration().setJgroupsConfigFile(
                  configurationManager.getResource(jgroupsConfigurationFilePath));
               log.info("Custom JGroups configuration set:"
                  + configurationManager.getResource(jgroupsConfigurationFilePath));
            }
            catch (Exception e)
            {
               throw new RepositoryConfigurationException("Error setting JGroups configuration.", e);
            }
         }
      }
      return cache;
   }

   /**
    * Add a region to the given cache
    * @param fqn the roof fqn of the region to add
    * @param cache the cache to which we want to add the region
    * @param cfg the configuration from which the Eviction Algorithm Config must be extracted
    */
   private static <K, V> void addEvictionRegion(Fqn<String> fqn, Cache<K, V> cache, Configuration cfg)
   {
      EvictionConfig ec = cfg.getEvictionConfig();
      // Create the region and set the config
      Region region = cache.getRegion(fqn, true);
      if (ec != null && ec.getDefaultEvictionRegionConfig() != null
         && ec.getDefaultEvictionRegionConfig().getEvictionAlgorithmConfig() != null)
      {
         EvictionRegionConfig erc =
            new EvictionRegionConfig(fqn, ec.getDefaultEvictionRegionConfig().getEvictionAlgorithmConfig());
         region.setEvictionRegionConfig(erc);
      }
   }

   /**
    * Try to find if a Cache of the same type (i.e. their {@link Configuration} are equals)
    * has already been registered for the same current container and the same {@link CacheType}.
    * If no cache has been registered, we register the given cache otherwise we
    * use the previously registered cache and we create a dedicated region to the shared cache
    * for the given cache.
    * @param cacheType The type of the target cache
    * @param rootFqn the rootFq 
    * @param cache the cache to register
    * @return the unique instance of the same cache registered
    * @throws RepositoryConfigurationException
    */
   @SuppressWarnings("unchecked")
   public static synchronized <K, V> Cache<K, V> getUniqueInstance(CacheType cacheType, Fqn<String> rootFqn,
      Cache<K, V> cache) throws RepositoryConfigurationException
   {
      ExoContainer container = ExoContainerContext.getCurrentContainer();
      Map<CacheType, Map<Configuration, Cache>> allCacheTypes = CACHES.get(container);
      if (allCacheTypes == null)
      {
         allCacheTypes = new HashMap<CacheType, Map<Configuration, Cache>>();
         CACHES.put(container, allCacheTypes);
      }
      Map<Configuration, Cache> caches = allCacheTypes.get(cacheType);
      if (caches == null)
      {
         caches = new HashMap<Configuration, Cache>();
         allCacheTypes.put(cacheType, caches);
      }
      Configuration cfg;
      try
      {
         cfg = cache.getConfiguration().clone();
      }
      catch (CloneNotSupportedException e)
      {
         throw new RepositoryConfigurationException("Cannot clone the configuration.", e);
      }
      if (caches.containsKey(cfg))
      {
         cache = caches.get(cfg);
         if (log.isInfoEnabled())
            log.info("The region " + rootFqn + " has been registered for a cache of type " + cacheType
               + " and the container " + container.getContext().getName());
      }
      else
      {
         caches.put(cfg, cache);
      }
      addEvictionRegion(rootFqn, cache, cfg);
      return cache;
   }

   /**
    * All the known cache types
    */
   public enum CacheType {
      JCR_CACHE, INDEX_CACHE, LOCK_CACHE
   };
}
