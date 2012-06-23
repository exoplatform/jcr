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
import org.exoplatform.services.jcr.impl.core.query.ChangesFilterListsWrapper;
import org.exoplatform.services.jcr.impl.core.query.IndexerChangesFilter;
import org.exoplatform.services.jcr.impl.core.query.IndexerIoModeHandler;
import org.exoplatform.services.jcr.impl.core.query.IndexingTree;
import org.exoplatform.services.jcr.impl.core.query.QueryHandler;
import org.exoplatform.services.jcr.impl.core.query.SearchManager;
import org.exoplatform.services.jcr.infinispan.ISPNCacheFactory;
import org.exoplatform.services.jcr.infinispan.PrivilegedISPNCacheHelper;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.infinispan.loaders.CacheLoaderManager;

import java.io.IOException;
import java.io.Serializable;

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
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.impl.infinispan.v5.ISPNIndexChangesFilter");//NOSONAR

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

      CacheLoaderManager cacheLoaderManager =
         cache.getAdvancedCache().getComponentRegistry().getComponent(CacheLoaderManager.class);
      IndexerCacheStore cacheStore = (IndexerCacheStore)cacheLoaderManager.getCacheLoader();

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
   
   protected Log getLogger()
   {
      return LOG;
   }

   protected void doUpdateIndex(ChangesFilterListsWrapper changes)
   {
      ChangesKey changesKey = new ChangesKey(wsId, IdGenerator.generate());
      cache.getAdvancedCache().withFlags(Flag.SKIP_LOCKING).put(changesKey, changes);
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
