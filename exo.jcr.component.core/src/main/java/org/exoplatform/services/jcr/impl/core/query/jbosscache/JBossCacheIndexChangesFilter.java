/*
 * Copyright (C) 2009 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.core.query.jbosscache;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.jcr.config.QueryHandlerEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.impl.core.query.ChangesFilterListsWrapper;
import org.exoplatform.services.jcr.impl.core.query.IndexerChangesFilter;
import org.exoplatform.services.jcr.impl.core.query.IndexerIoMode;
import org.exoplatform.services.jcr.impl.core.query.IndexerIoModeHandler;
import org.exoplatform.services.jcr.impl.core.query.IndexingTree;
import org.exoplatform.services.jcr.impl.core.query.QueryHandler;
import org.exoplatform.services.jcr.impl.core.query.SearchManager;
import org.exoplatform.services.jcr.jbosscache.ExoJBossCacheFactory;
import org.exoplatform.services.jcr.jbosscache.ExoJBossCacheFactory.CacheType;
import org.exoplatform.services.jcr.jbosscache.PrivilegedJBossCacheHelper;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.jboss.cache.Cache;
import org.jboss.cache.CacheSPI;
import org.jboss.cache.Fqn;
import org.jboss.cache.config.CacheLoaderConfig;
import org.jboss.cache.config.CacheLoaderConfig.IndividualCacheLoaderConfig;
import org.jboss.cache.config.CacheLoaderConfig.IndividualCacheLoaderConfig.SingletonStoreConfig;
import org.jboss.cache.jmx.JmxRegistrationManager;

import java.io.IOException;
import java.io.Serializable;
import java.security.PrivilegedAction;
import java.util.Properties;

import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@exoplatform.org">Sergey Kabashnyuk</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 2009-07-22 23:58:59Z ksm $
 *
 */
public class JBossCacheIndexChangesFilter extends IndexerChangesFilter
{
   /**
    * Logger instance for this class
    */
   private final Log log = ExoLogger.getLogger("exo.jcr.component.core.JBossCacheIndexChangesFilter");

   public static final String PARAM_JBOSSCACHE_CONFIGURATION = "jbosscache-configuration";

   public static final String PARAM_JBOSSCACHE_PUSHSTATE = "jbosscache-sscl-push.state.enabled";

   public static final String PARAM_JBOSSCACHE_PUSHSTATE_TIMEOUT = "jbosscache-sscl-push.state.timeout";

   /**
    * Indicate whether the JBoss Cache instance used can be shared with other caches
    */
   public static final String PARAM_JBOSSCACHE_SHAREABLE = "jbosscache-shareable";

   public static final Boolean PARAM_JBOSSCACHE_SHAREABLE_DEFAULT = Boolean.FALSE;

   private final Cache<Serializable, Object> cache;

   private final Fqn<String> rootFqn;
   
   private final JmxRegistrationManager jmxManager;

   public static final String LISTWRAPPER = "$lists".intern();

   /**
    * @param searchManager
    * @param config
    * @param indexingTree
    * @throws RepositoryConfigurationException 
    */
   public JBossCacheIndexChangesFilter(SearchManager searchManager, SearchManager parentSearchManager,
      QueryHandlerEntry config, IndexingTree indexingTree, IndexingTree parentIndexingTree, QueryHandler handler,
      QueryHandler parentHandler, ConfigurationManager cfm) throws IOException, RepositoryException,
      RepositoryConfigurationException
   {
      super(searchManager, parentSearchManager, config, indexingTree, parentIndexingTree, handler, parentHandler, cfm);
      // create cache using custom factory
      ExoJBossCacheFactory<Serializable, Object> factory = new ExoJBossCacheFactory<Serializable, Object>(cfm);
      Cache<Serializable, Object> initCache = factory.createCache(config);

      // initialize IndexerCacheLoader 
      IndexerCacheLoader indexerCacheLoader = new IndexerCacheLoader();
      // set SingltonStoreCacheLoader
      SingletonStoreConfig singletonStoreConfig = new SingletonStoreConfig();
      singletonStoreConfig.setSingletonStoreClass(IndexerSingletonStoreCacheLoader.class.getName());
      //singletonStoreConfig.setSingletonStoreClass(SingletonStoreCacheLoader.class.getName());
      Properties singletonStoreProperties = new Properties();

      // try to get pushState parameters, since they are set programmatically only
      Boolean pushState = config.getParameterBoolean(PARAM_JBOSSCACHE_PUSHSTATE, false);
      Long pushStateTimeOut = config.getParameterTime(PARAM_JBOSSCACHE_PUSHSTATE_TIMEOUT, 10000L);

      singletonStoreProperties.setProperty("pushStateWhenCoordinator", pushState.toString());
      singletonStoreProperties.setProperty("pushStateWhenCoordinatorTimeout", pushStateTimeOut.toString());
      singletonStoreConfig.setProperties(singletonStoreProperties);
      singletonStoreConfig.setSingletonStoreEnabled(true);
      // create CacheLoaderConfig
      IndividualCacheLoaderConfig individualCacheLoaderConfig = new IndividualCacheLoaderConfig();
      // set SingletonStoreConfig
      individualCacheLoaderConfig.setSingletonStoreConfig(singletonStoreConfig);
      // set CacheLoader
      individualCacheLoaderConfig.setCacheLoader(indexerCacheLoader);
      // set parameters
      individualCacheLoaderConfig.setFetchPersistentState(false);
      individualCacheLoaderConfig.setAsync(false);
      individualCacheLoaderConfig.setIgnoreModifications(false);
      individualCacheLoaderConfig.setPurgeOnStartup(false);
      // create CacheLoaderConfig
      CacheLoaderConfig cacheLoaderConfig = new CacheLoaderConfig();
      cacheLoaderConfig.setShared(false);
      cacheLoaderConfig.setPassivation(false);
      cacheLoaderConfig.addIndividualCacheLoaderConfig(individualCacheLoaderConfig);
      // insert CacheLoaderConfig
      initCache.getConfiguration().setCacheLoaderConfig(cacheLoaderConfig);
      this.rootFqn = Fqn.fromElements(searchManager.getWsId());
      this.cache =
         ExoJBossCacheFactory.getUniqueInstance(CacheType.INDEX_CACHE, rootFqn, initCache,
            config.getParameterBoolean(PARAM_JBOSSCACHE_SHAREABLE, PARAM_JBOSSCACHE_SHAREABLE_DEFAULT));

      PrivilegedJBossCacheHelper.create(cache);
      PrivilegedJBossCacheHelper.start(cache);
      
      this.jmxManager =
         ExoJBossCacheFactory.getJmxRegistrationManager(searchManager.getExoContainerContext(), cache,
            CacheType.INDEX_CACHE);
      if (jmxManager != null)
      {
         SecurityHelper.doPrivilegedAction(new PrivilegedAction<Void>()
         {
            public Void run()
            {
               jmxManager.registerAllMBeans();
               return null;
            }
         });
      }

      // start will invoke cache listener which will notify handler that mode is changed
      IndexerIoMode ioMode =
         ((CacheSPI)cache).getRPCManager().isCoordinator() ? IndexerIoMode.READ_WRITE : IndexerIoMode.READ_ONLY;

      // Could have change of cache
      IndexerSingletonStoreCacheLoader issCacheLoader =
         (IndexerSingletonStoreCacheLoader)((CacheSPI)cache).getCacheLoaderManager().getCacheLoader();

      indexerCacheLoader = (IndexerCacheLoader)issCacheLoader.getCacheLoader();

      indexerCacheLoader.register(searchManager, parentSearchManager, handler, parentHandler);
      IndexerIoModeHandler modeHandler = indexerCacheLoader.getModeHandler();
      handler.setIndexerIoModeHandler(modeHandler);
      parentHandler.setIndexerIoModeHandler(modeHandler);

      if (!parentHandler.isInitialized())
      {
         parentHandler.setIndexInfos(new JBossCacheIndexInfos(rootFqn, cache, true, modeHandler));
         parentHandler.setIndexUpdateMonitor(new JBossCacheIndexUpdateMonitor(rootFqn, cache, true, modeHandler));
         parentHandler.init();
      }
      if (!handler.isInitialized())
      {
         handler.setIndexInfos(new JBossCacheIndexInfos(rootFqn, cache, false, modeHandler));
         handler.setIndexUpdateMonitor(new JBossCacheIndexUpdateMonitor(rootFqn, cache, false, modeHandler));
         handler.init();
      }
   }

   protected Log getLogger()
   {
      return log;
   }

   protected void doUpdateIndex(ChangesFilterListsWrapper changes)
   {
      String id = IdGenerator.generate();
      cache.put(Fqn.fromRelativeElements(rootFqn, id), LISTWRAPPER, changes);
   }

   /**
    * @see org.exoplatform.services.jcr.impl.core.query.IndexerChangesFilter#isShared()
    */
   public boolean isShared()
   {
      return true;
   }

    /**
    * {@inheritDoc}
    */
   @Override
   public void close()
   {
      try
      {
         if (jmxManager != null)
         {
            SecurityHelper.doPrivilegedAction(new PrivilegedAction<Void>()
            {
               public Void run()
               {
                  jmxManager.unregisterAllMBeans();
                  return null;
               }
            });
         }
      }
      catch (Exception e) {
         log.warn("Not all JBoss Cache MBeans were unregistered.");
      }
   } 
}
