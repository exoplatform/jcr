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
package org.exoplatform.services.jcr.impl.core.query.ispn;

import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.jcr.config.QueryHandlerEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.impl.core.query.IndexerChangesFilter;
import org.exoplatform.services.jcr.impl.core.query.IndexerIoModeHandler;
import org.exoplatform.services.jcr.impl.core.query.IndexingTree;
import org.exoplatform.services.jcr.impl.core.query.QueryHandler;
import org.exoplatform.services.jcr.impl.core.query.SearchManager;
import org.exoplatform.services.jcr.impl.core.query.jbosscache.ChangesFilterListsWrapper;
import org.exoplatform.services.jcr.impl.core.query.lucene.ChangesHolder;
import org.exoplatform.services.jcr.infinispan.ISPNCacheFactory;
import org.exoplatform.services.jcr.infinispan.PrivilegedISPNCacheHelper;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.infinispan.Cache;
import org.infinispan.CacheException;
import org.infinispan.config.Configuration.CacheMode;
import org.infinispan.loaders.CacheLoaderManager;

import java.io.IOException;
import java.io.Serializable;
import java.util.Set;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 *
 * Date: 23.02.2011
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: ISPNIndexChangesFilter.java 34360 2010-11-11 11:11:11Z tolusha $
 */
public class ISPNIndexChangesFilter extends IndexerChangesFilter
{
   /**
    * Logger instance for this class.
    */
   private final Log log = ExoLogger.getLogger("exo.jcr.component.core.ISPNIndexChangesFilter");

   public static final String PARAM_INFINISPAN_CACHESTORE_CLASS = "infinispan-cachestore-classname";

   /**
    * ISPN cache.
    */
   private final Cache<Serializable, Object> cache;

   /**
    * Unique workspace identifier.
    */
   private final int wsId;

   /**
    * ISPNIndexChangesFilter constructor.
    */
   public ISPNIndexChangesFilter(SearchManager searchManager, SearchManager parentSearchManager,
      QueryHandlerEntry config, IndexingTree indexingTree, IndexingTree parentIndexingTree, QueryHandler handler,
      QueryHandler parentHandler, ConfigurationManager cfm) throws IOException, RepositoryException,
      RepositoryConfigurationException
   {
      super(searchManager, parentSearchManager, config, indexingTree, parentIndexingTree, handler, parentHandler, cfm);

      this.wsId = searchManager.getWsId().hashCode();

      ISPNCacheFactory<Serializable, Object> factory = new ISPNCacheFactory<Serializable, Object>(cfm);
      config.putParameterValue(PARAM_INFINISPAN_CACHESTORE_CLASS, IndexerCacheStore.class.getName());
      this.cache = factory.createCache("Indexer-" + searchManager.getWsId(), config);

      CacheLoaderManager cacheLoaderManager =
         cache.getAdvancedCache().getComponentRegistry().getComponent(CacheLoaderManager.class);
      IndexerCacheStore cacheStore = (IndexerCacheStore)cacheLoaderManager.getCacheLoader();

      // This code make it possible to use the ISPNIndexChangesFilter in a non-cluster environment
      if (cache.getConfiguration().getCacheMode() == CacheMode.LOCAL)
      {
         cacheStore.activeStatusChanged(true);
      }

      cacheStore.register(searchManager, parentSearchManager, handler, parentHandler);
      IndexerIoModeHandler modeHandler = cacheStore.getModeHandler();
      handler.setIndexerIoModeHandler(modeHandler);
      parentHandler.setIndexerIoModeHandler(modeHandler);

      if (!parentHandler.isInitialized())
      {
         parentHandler.setIndexInfos(new ISPNIndexInfos(searchManager.getWsId(), cache, true, modeHandler));
         parentHandler.setIndexUpdateMonitor(new ISPNIndexUpdateMonitor(searchManager.getWsId(), cache, true,
            modeHandler));
         parentHandler.init();
      }
      if (!handler.isInitialized())
      {
         handler.setIndexInfos(new ISPNIndexInfos(searchManager.getWsId(), cache, false, modeHandler));
         handler.setIndexUpdateMonitor(new ISPNIndexUpdateMonitor(searchManager.getWsId(), cache, false, modeHandler));
         handler.init();
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void doUpdateIndex(Set<String> removedNodes, Set<String> addedNodes, Set<String> parentRemovedNodes,
      Set<String> parentAddedNodes)
   {
      ChangesHolder changes = searchManager.getChanges(removedNodes, addedNodes);
      ChangesHolder parentChanges = parentSearchManager.getChanges(parentRemovedNodes, parentAddedNodes);

      if (changes == null && parentChanges == null)
      {
         return;
      }

      ChangesKey changesKey = new ChangesKey(wsId, IdGenerator.generate());
      try
      {
         PrivilegedISPNCacheHelper.put(cache, changesKey, new ChangesFilterListsWrapper(changes, parentChanges));
      }
      catch (CacheException e)
      {
         log.error(e.getLocalizedMessage(), e);
         logErrorChanges(handler, removedNodes, addedNodes);
         logErrorChanges(parentHandler, parentRemovedNodes, parentAddedNodes);
      }
   }

   /**
    * Log errors.
    * 
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
