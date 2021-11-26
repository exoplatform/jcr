/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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

package org.exoplatform.services.jcr.impl.core.query.ispn;

import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.jcr.config.QueryHandlerEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.impl.core.query.ChangesFilterListsWrapper;
import org.exoplatform.services.jcr.impl.core.query.IndexerChangesFilter;
import org.exoplatform.services.jcr.impl.core.query.IndexerIoModeHandler;
import org.exoplatform.services.jcr.impl.core.query.IndexingTree;
import org.exoplatform.services.jcr.impl.core.query.LocalIndexMarker;
import org.exoplatform.services.jcr.impl.core.query.QueryHandler;
import org.exoplatform.services.jcr.impl.core.query.SearchManager;
import org.exoplatform.services.jcr.infinispan.ISPNCacheFactory;
import org.exoplatform.services.jcr.infinispan.PrivilegedISPNCacheHelper;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.infinispan.persistence.manager.PersistenceManager;

import java.io.IOException;
import java.io.Serializable;
import java.util.Set;

import javax.jcr.RepositoryException;

/**
 * This type of ChangeFilter offers an ability for each cluster instance to have own
 * local index (stack of indexes, from persistent to volatile). It uses ISPN cache for
 * Lucene Documents and UUIDs delivery. Each node works in ReadWrite mode, so manages 
 * it own volatile, merger, local list of persisted indexes and stand-alone 
 * UpdateInProgressMonitor implementation. 
 * This implementation is similar to ISPNIndexChangesFilter but does not use
 * ISPNIndexInfos and ISPNIndexUpdateMonitor classes.
 *
 * @author <a href="mailto:anatoliy.bazko@gmail.com">Anatoliy Bazko</a>
 * @version $Id: LocalIndexChangesFilter.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class LocalIndexChangesFilter extends IndexerChangesFilter implements LocalIndexMarker
{
   /**
    * Logger instance for this class
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.LocalIndexChangesFilter");//NOSONAR

   public static final String PARAM_INFINISPAN_CACHESTORE_CLASS = "infinispan-cachestore-classname";

   private final Cache<Serializable, Object> cache;

   private final String wsId;

   /**
    * LocalIndexChangesFilter constructor.
    */
   public LocalIndexChangesFilter(SearchManager searchManager, SearchManager parentSearchManager,
      QueryHandlerEntry config, IndexingTree indexingTree, IndexingTree parentIndexingTree, QueryHandler handler,
      QueryHandler parentHandler, ConfigurationManager cfm) throws IOException, RepositoryException,
      RepositoryConfigurationException
   {
      super(searchManager, parentSearchManager, config, indexingTree, parentIndexingTree, handler, parentHandler, cfm);

      this.wsId = searchManager.getWsId();
      ISPNCacheFactory<Serializable, Object> factory = new ISPNCacheFactory<Serializable, Object>(cfm);
      config.putParameterValue(PARAM_INFINISPAN_CACHESTORE_CLASS, LocalIndexCacheStore.class.getName());
      this.cache = factory.createCache("Indexer_" + searchManager.getWsId(), config);

      PersistenceManager persistenceManager = cache.getAdvancedCache().getComponentRegistry().getComponent(PersistenceManager.class);
      Set<LocalIndexCacheStore> stores = persistenceManager.getStores(LocalIndexCacheStore.class);
      LocalIndexCacheStore cacheStore = null;
      if(!stores.isEmpty()){
         cacheStore = stores.iterator().next();
      }

      cacheStore.register(searchManager, parentSearchManager, handler, parentHandler);
      IndexerIoModeHandler modeHandler = cacheStore.getModeHandler();
      handler.setIndexerIoModeHandler(modeHandler);
      parentHandler.setIndexerIoModeHandler(modeHandler);

      // using default updateMonitor and default 
      if (!parentHandler.isInitialized())
      {
         parentHandler.init();
      }
      if (!handler.isInitialized())
      {
         handler.init();
      }
   }

   protected Log getLogger()
   {
      return LOG;
   }

   protected void doUpdateIndex(ChangesFilterListsWrapper changes)
   {
      ChangesKey changesKey = new ChangesKey(wsId, IdGenerator.generate());
      try
      {
         cache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).put(changesKey, changes);
      }
      finally
      {
         // Purge the cache to prevent memory leak
         cache.getAdvancedCache()
            .withFlags(Flag.CACHE_MODE_LOCAL, Flag.IGNORE_RETURN_VALUES)
            .remove(changesKey);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void close()
   {
      PrivilegedISPNCacheHelper.stop(cache);
      ISPNCacheFactory.releaseUniqueInstance(cache.getCacheManager());
   }
}
