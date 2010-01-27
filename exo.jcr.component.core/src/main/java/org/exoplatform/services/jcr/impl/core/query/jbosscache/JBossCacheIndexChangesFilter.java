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

import org.exoplatform.services.jcr.config.QueryHandlerEntry;
import org.exoplatform.services.jcr.config.QueryHandlerParams;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.impl.core.query.IndexerChangesFilter;
import org.exoplatform.services.jcr.impl.core.query.IndexerIoMode;
import org.exoplatform.services.jcr.impl.core.query.IndexerIoModeHandler;
import org.exoplatform.services.jcr.impl.core.query.IndexingTree;
import org.exoplatform.services.jcr.impl.core.query.QueryHandler;
import org.exoplatform.services.jcr.impl.core.query.SearchManager;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.jboss.cache.Cache;
import org.jboss.cache.CacheException;
import org.jboss.cache.CacheFactory;
import org.jboss.cache.CacheSPI;
import org.jboss.cache.DefaultCacheFactory;
import org.jboss.cache.config.CacheLoaderConfig;
import org.jboss.cache.config.CacheLoaderConfig.IndividualCacheLoaderConfig;
import org.jboss.cache.config.CacheLoaderConfig.IndividualCacheLoaderConfig.SingletonStoreConfig;

import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;
import java.util.Set;

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
   private final Log log = ExoLogger.getLogger(JBossCacheIndexChangesFilter.class);

   private final Cache<Serializable, Object> cache;

   public static final String LISTWRAPPER = "$lists".intern();

   /**
    * @param searchManager
    * @param config
    * @param indexingTree
    * @throws RepositoryConfigurationException 
    */
   public JBossCacheIndexChangesFilter(SearchManager searchManager, SearchManager parentSearchManager,
      QueryHandlerEntry config, IndexingTree indexingTree, IndexingTree parentIndexingTree, QueryHandler handler,
      QueryHandler parentHandler) throws IOException, RepositoryException, RepositoryConfigurationException
   {
      super(searchManager, parentSearchManager, config, indexingTree, parentIndexingTree, handler, parentHandler);
      String jbcConfig = config.getParameterValue(QueryHandlerParams.PARAM_CHANGES_FILTER_CONFIG_PATH);
      CacheFactory<Serializable, Object> factory = new DefaultCacheFactory<Serializable, Object>();
      log.info("JBoss Cache configuration used: " + jbcConfig);
      this.cache = factory.createCache(jbcConfig, false);

      // initialize IndexerCacheLoader 
      IndexerCacheLoader indexerCacheLoader = new IndexerCacheLoader();
      // inject dependencies
      indexerCacheLoader.init(searchManager, parentSearchManager, handler, parentHandler);
      // set SingltonStoreCacheLoader
      SingletonStoreConfig singletonStoreConfig = new SingletonStoreConfig();
      singletonStoreConfig.setSingletonStoreClass(IndexerSingletonStoreCacheLoader.class.getName());
      //singletonStoreConfig.setSingletonStoreClass(SingletonStoreCacheLoader.class.getName());
      Properties singletonStoreProperties = new Properties();
      singletonStoreProperties.setProperty("pushStateWhenCoordinator", "false");
      singletonStoreProperties.setProperty("pushStateWhenCoordinatorTimeout", "10000");
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
      this.cache.getConfiguration().setCacheLoaderConfig(cacheLoaderConfig);
      this.cache.create();
      this.cache.start();
      // start will invoke cache listener which will notify handler that mode is changed
      IndexerIoMode ioMode =
         ((CacheSPI)cache).getRPCManager().isCoordinator() ? IndexerIoMode.READ_WRITE : IndexerIoMode.READ_ONLY;
      IndexerIoModeHandler modeHandler = indexerCacheLoader.getModeHandler();
      handler.setIndexerIoModeHandler(modeHandler);
      parentHandler.setIndexerIoModeHandler(modeHandler);

      if (!parentHandler.isInitialized())
      {
         // TODO: uncomment it, when JbossCacheIndexInfos is finished.
         parentHandler.setIndexInfos(new JBossCacheIndexInfos(cache, true, modeHandler));
         parentHandler.setIndexUpdateMonitor(new JBossCacheIndexUpdateMonitor(cache, modeHandler));
         parentHandler.init();
      }
      if (!handler.isInitialized())
      {
         // TODO: uncomment it, when JbossCacheIndexInfos is finished.
         handler.setIndexInfos(new JBossCacheIndexInfos(cache, false, modeHandler));
         handler.setIndexUpdateMonitor(new JBossCacheIndexUpdateMonitor(cache, modeHandler));
         handler.init();
      }

   }

   /**
    * @see org.exoplatform.services.jcr.impl.core.query.IndexerChangesFilter#doUpdateIndex(java.util.Set, java.util.Set, java.util.Set, java.util.Set)
    */
   @Override
   protected void doUpdateIndex(Set<String> removedNodes, Set<String> addedNodes, Set<String> parentRemovedNodes,
      Set<String> parentAddedNodes)
   {
      String id = IdGenerator.generate();
      try
      {
         cache.put(id, LISTWRAPPER, new ChangesFilterListsWrapper(addedNodes, removedNodes, parentAddedNodes,
            parentRemovedNodes));
      }
      catch (CacheException e)
      {
         logErrorChanges(handler, removedNodes, addedNodes);
         logErrorChanges(parentHandler, parentRemovedNodes, parentAddedNodes);
      }
   }

   /**
    * Log errors
    * @param logHandler
    * @param removedNodes
    * @param addedNodes
    */
   private void logErrorChanges(QueryHandler logHandler, Set<String> removedNodes, Set<String> addedNodes)
   {
      try
      {

         logHandler.logErrorChanges(addedNodes, removedNodes);
      }
      catch (IOException ioe)
      {
         log.warn("Exception occure when errorLog writed. Error log is not complete. " + ioe, ioe);
      }
   }
}
