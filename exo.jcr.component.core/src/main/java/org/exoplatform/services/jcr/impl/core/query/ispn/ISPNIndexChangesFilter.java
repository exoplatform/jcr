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

import org.apache.regexp.RE;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.jcr.config.QueryHandlerEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.impl.core.query.*;
import org.exoplatform.services.jcr.impl.core.query.lucene.IndexInfos;
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
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.ISPNIndexChangesFilter");//NOSONAR

   public static final String PARAM_INFINISPAN_CACHESTORE_CLASS = "infinispan-cachestore-classname";

   /**
    * ISPN cache.
    */
   private final Cache<Serializable, Object> cache;

   /**
    * Unique workspace identifier.
    */
   private final String wsId;

   /**
    * ISPNIndexChangesFilter constructor.
    */
   public ISPNIndexChangesFilter(SearchManager searchManager, SearchManager parentSearchManager,
      QueryHandlerEntry config, IndexingTree indexingTree, IndexingTree parentIndexingTree, QueryHandler handler,
      QueryHandler parentHandler, ConfigurationManager cfm) throws IOException, RepositoryException,
      RepositoryConfigurationException
   {
      super(searchManager, parentSearchManager, config, indexingTree, parentIndexingTree, handler, parentHandler, cfm);

      this.wsId = searchManager.getWsId();

      ISPNCacheFactory<Serializable, Object> factory = new ISPNCacheFactory<Serializable, Object>(cfm);
      config.putParameterValue(PARAM_INFINISPAN_CACHESTORE_CLASS, IndexerCacheStore.class.getName());
      this.cache = factory.createCache("Indexer_" + searchManager.getWsId(), config);
      PrivilegedISPNCacheHelper.start(this.cache);

      PersistenceManager persistenceManager = cache.getAdvancedCache().getComponentRegistry().getComponent(PersistenceManager.class);
      Set<IndexerCacheStore> stores = persistenceManager.getStores(IndexerCacheStore.class);
      IndexerCacheStore cacheStore = null;
      if(!stores.isEmpty()){
         cacheStore = stores.iterator().next();
      }

      cacheStore.register(searchManager, parentSearchManager, handler, parentHandler);
      IndexerIoModeHandler modeHandler = cacheStore.getModeHandler();
      handler.setIndexerIoModeHandler(modeHandler);
      parentHandler.setIndexerIoModeHandler(modeHandler);

      if (!parentHandler.isInitialized())
      {
         parentHandler.setIndexInfos(createIndexInfos(true, modeHandler, config, parentHandler));
         parentHandler.setIndexUpdateMonitor(new ISPNIndexUpdateMonitor(searchManager.getWsId(), cache, true,
            modeHandler));
         parentHandler.init();
      }
      if (!handler.isInitialized())
      {
         handler.setIndexInfos(createIndexInfos(false, modeHandler, config, handler));
         handler.setIndexUpdateMonitor(new ISPNIndexUpdateMonitor(searchManager.getWsId(), cache, false, modeHandler));
         handler.init();
      }
   }
   

   /**
    * Factory method for creating corresponding IndexInfos class. RSyncIndexInfos created if RSync configured
    * and ISPNIndexInfos otherwise
    * 
    * @param system
    * @param modeHandler
    * @param config
    * @param handler
    * @return
    * @throws RepositoryConfigurationException
    */
   private IndexInfos createIndexInfos(Boolean system, IndexerIoModeHandler modeHandler, QueryHandlerEntry config,
      QueryHandler handler) throws RepositoryConfigurationException
   {
      try
      {
         // read RSYNC configuration
         RSyncConfiguration rSyncConfiguration = new RSyncConfiguration(config);
         // rsync configured
         if (rSyncConfiguration.getRsyncEntryName() != null)
         {
            return new RsyncIndexInfos(wsId, cache, system, modeHandler, handler.getContext()
                    .getIndexDirectory(), rSyncConfiguration);
         }
         else
         {
            return new ISPNIndexInfos(wsId, cache, true, modeHandler);
         }
      }
      catch (RepositoryConfigurationException e)
      {
         return new ISPNIndexInfos(wsId, cache, true, modeHandler);
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
    * @see org.exoplatform.services.jcr.impl.core.query.IndexerChangesFilter#isShared()
    */
   public boolean isShared()
   {
      return true;
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
