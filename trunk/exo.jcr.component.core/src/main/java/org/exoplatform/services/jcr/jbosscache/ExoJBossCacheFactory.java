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

import org.exoplatform.commons.utils.SecurityHelper;
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
import org.jboss.cache.jmx.JmxRegistrationManager;
import org.jgroups.JChannelFactory;

import java.io.IOException;
import java.io.InputStream;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import javax.management.ObjectName;
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
    * Keep only one instance of the {@link JChannelFactory} to prevent creating several times the
    * same multiplexer stack
    */
   private static final JChannelFactory CHANNEL_FACTORY =
      SecurityHelper.doPrivilegedAction(new PrivilegedAction<JChannelFactory>()
      {
         public JChannelFactory run()
         {
            return new JChannelFactory();
         }
      });

   /**
    * A Map that contains all the registered JBC instances, ordered by
    * {@link ExoContainer} instances, {@link CacheType} and JBC Configuration.
    */
   private static Map<ExoContainer, Map<CacheType, Map<ConfigurationKey, Cache>>> CACHES =
      new HashMap<ExoContainer, Map<CacheType, Map<ConfigurationKey, Cache>>>();

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
      this.configurationHelper = new JBossCacheHelper(configurationManager);
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
      final CacheFactory<K, V> factory = SecurityHelper.doPrivilegedAction(new PrivilegedAction<CacheFactory<K, V>>()
      {
         public CacheFactory<K, V> run()
         {
            return new DefaultCacheFactory<K, V>();
         }
      });

      final InputStream stream = configStream;

      PrivilegedAction<Cache<K, V>> action = new PrivilegedAction<Cache<K, V>>()
      {
         public Cache<K, V> run()
         {
            return factory.createCache(stream, false);
         }
      };
      Cache<K, V> cache = SecurityHelper.doPrivilegedAction(action);

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
               CHANNEL_FACTORY.setMultiplexerConfig(configurationManager.getResource(jgroupsConfigurationFilePath));
               cache.getConfiguration().getRuntimeConfig().setMuxChannelFactory(CHANNEL_FACTORY);
               log.info("Multiplexer stack successfully enabled for the cache.");
            }
         }
         catch (Exception e)
         {
            // exception occurred setting mux factory
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
      if (ec != null && ec.getDefaultEvictionRegionConfig() != null)
      {
         EvictionRegionConfig erc = new EvictionRegionConfig(fqn);
         erc.setDefaults(ec.getDefaultEvictionRegionConfig());
         region.setEvictionRegionConfig(erc);
      }
   }

   /**
    * Try to find if a Cache of the same type (i.e. their {@link Configuration} are equals)
    * has already been registered for the same current container and the same {@link CacheType}.
    * If no cache has been registered, we register the given cache otherwise we
    * use the previously registered cache and we create a dedicated region into the shared cache
    * for the given cache.
    * @param cacheType The type of the target cache
    * @param rootFqn the rootFqn corresponding to root of the region 
    * @param cache the cache to register
    * @param shareable indicates whether the cache is shareable or not. 
    * @return the given cache if has not been registered otherwise the cache of the same
    * type that has already been registered. If the cache is not sharable, the given cache
    * will be returned.
    * @throws RepositoryConfigurationException
    */
   @SuppressWarnings("unchecked")
   public static synchronized <K, V> Cache<K, V> getUniqueInstance(CacheType cacheType, Fqn<String> rootFqn,
      Cache<K, V> cache, boolean shareable) throws RepositoryConfigurationException
   {
      if (!shareable)
      {
         // The cache is not shareable         
         // Avoid potential naming collision by changing the cluster name
         cache.getConfiguration().setClusterName(
            cache.getConfiguration().getClusterName() + rootFqn.toString().replace('/', '-'));
         return cache;
      }
      ExoContainer container = ExoContainerContext.getCurrentContainer();
      Map<CacheType, Map<ConfigurationKey, Cache>> allCacheTypes = CACHES.get(container);
      if (allCacheTypes == null)
      {
         allCacheTypes = new HashMap<CacheType, Map<ConfigurationKey, Cache>>();
         CACHES.put(container, allCacheTypes);
      }
      Map<ConfigurationKey, Cache> caches = allCacheTypes.get(cacheType);
      if (caches == null)
      {
         caches = new HashMap<ConfigurationKey, Cache>();
         allCacheTypes.put(cacheType, caches);
      }
      Configuration cfg = cache.getConfiguration();
      ConfigurationKey key;
      try
      {
         key = new ConfigurationKey(cfg);
      }
      catch (CloneNotSupportedException e)
      {
         throw new RepositoryConfigurationException("Cannot clone the configuration.", e);
      }
      if (caches.containsKey(key))
      {
         cache = caches.get(key);
      }
      else
      {
         caches.put(key, cache);
         if (log.isInfoEnabled())
         {
            log.info("A new JBoss Cache instance has been registered for the region " + rootFqn + ", a cache of type "
               + cacheType + " and the container " + container.getContext().getName());
         }
      }
      addEvictionRegion(rootFqn, cache, cfg);
      if (log.isInfoEnabled())
      {
         log.info("The region " + rootFqn + " has been registered for a cache of type " + cacheType
            + " and the container " + container.getContext().getName());
      }
      return cache;
   }

   /**
    * Gives the {@link JmxRegistrationManager} instance corresponding to the given context
    */
   public static JmxRegistrationManager getJmxRegistrationManager(ExoContainerContext ctx, Cache<?, ?> parentCache,
      CacheType cacheType)
   {
      try
      {
         ObjectName containerObjectName = ctx.getContainer().getScopingObjectName();
         final String objectNameBase = containerObjectName.toString() + ",cache-type=" + cacheType;
         return new JmxRegistrationManager(ctx.getContainer().getMBeanServer(), parentCache, objectNameBase)
         {
            public String getObjectName(String resourceName)
            {
               return objectNameBase + JMX_RESOURCE_KEY + resourceName;
            }
         };
      }
      catch (Exception e)
      {
         log.error("Could not create the JMX Manager", e);
      }
      return null;
   }
   
   /**
    * All the known cache types
    */
   public enum CacheType {
      JCR_CACHE, INDEX_CACHE, LOCK_CACHE
   }

   /**
    * This class is used to make {@link Configuration} being usable as a Key in an HashMap since
    * some variables such as <code>jgroupsConfigFile</code> are not managed as expected in the
    * methods equals and hashCode. Moreover two cache with same config except the EvictionConfig
    * are considered as identical
    */
   private static class ConfigurationKey
   {
      private final String jgroupsConfigFile;

      private final Configuration conf;

      public ConfigurationKey(Configuration initialConf) throws CloneNotSupportedException
      {
         // Clone it first since it will be modified
         this.conf = initialConf.clone();
         this.jgroupsConfigFile = (conf.getJGroupsConfigFile() == null ? null : conf.getJGroupsConfigFile().toString());
         // remove the jgroupsConfigFile from the conf
         conf.setJgroupsConfigFile(null);
         // remove the EvictionConfig to ignore it
         conf.setEvictionConfig(null);
      }

      @Override
      public int hashCode()
      {
         final int prime = 31;
         int result = 1;
         result = prime * result + ((conf == null) ? 0 : conf.hashCode());
         result = prime * result + ((jgroupsConfigFile == null) ? 0 : jgroupsConfigFile.hashCode());
         return result;
      }

      @Override
      public boolean equals(Object obj)
      {
         if (this == obj)
         {
            return true;
         }
         if (obj == null)
         {
            return false;
         }
         if (getClass() != obj.getClass())
         {
            return false;
         }
         ConfigurationKey other = (ConfigurationKey)obj;
         if (conf == null)
         {
            if (other.conf != null)
            {
               return false;
            }
         }
         else if (!conf.equals(other.conf))
         {
            return false;
         }
         if (jgroupsConfigFile == null)
         {
            if (other.jgroupsConfigFile != null)
            {
               return false;
            }
         }
         else if (!jgroupsConfigFile.equals(other.jgroupsConfigFile))
         {
            return false;
         }
         return true;
      }
   }
}
