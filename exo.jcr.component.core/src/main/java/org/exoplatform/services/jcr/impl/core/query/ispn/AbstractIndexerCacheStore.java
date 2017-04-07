/*
 * Copyright (C) 2011 eXo Platform SAS.
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

import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.impl.core.query.ChangesFilterListsWrapper;
import org.exoplatform.services.jcr.impl.core.query.Indexer;
import org.exoplatform.services.jcr.impl.core.query.IndexerIoModeHandler;
import org.exoplatform.services.jcr.impl.core.query.QueryHandler;
import org.exoplatform.services.jcr.impl.core.query.SearchManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.infinispan.Cache;
import org.infinispan.commons.util.concurrent.FutureListener;
import org.infinispan.context.Flag;
import org.infinispan.filter.KeyFilter;
import org.infinispan.marshall.core.MarshalledEntry;
import org.infinispan.persistence.spi.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

/**
 * Abstract Indexer Cache Loader defines default implementation of data processing received via cache.
 *
 */
public abstract class AbstractIndexerCacheStore implements AdvancedLoadWriteStore
{
   /**
    * A map of all the indexers that has been registered
    */
   protected final Map<String, Indexer> indexers = new HashMap<String, Indexer>();

   protected InitializationContext ctx;

   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.IndexerCacheLoader");//NOSONAR

   /**
    * This method will register a new Indexer according to the given parameters.
    *
    * @param searchManager
    * @param parentSearchManager
    * @param handler
    * @param parentHandler
    * @throws RepositoryConfigurationException
    */
   public void register(SearchManager searchManager, SearchManager parentSearchManager, QueryHandler handler,
                        QueryHandler parentHandler) throws RepositoryConfigurationException
   {
      indexers.put(searchManager.getWsId(), new Indexer(searchManager, parentSearchManager, handler, parentHandler));
      if (LOG.isDebugEnabled())
      {
         LOG.debug("Register " + searchManager.getWsId() + " " + this + " in " + indexers);
      }
   }

   /**
    * @return IndexerIoModeHandler instance
    */
   public abstract IndexerIoModeHandler getModeHandler();

   @Override
   public void init(InitializationContext ctx)
   {
      this.ctx= ctx;
   }

   @Override
   public void start()
   {
   }

   @Override
   public void stop()
   {
      indexers.clear();
   }

   @Override
   public MarshalledEntry load(Object key)
   {
      // This cacheStore only accepts data
      return null;
   }

   @Override
   public boolean contains(Object key)
   {
      return false;
   }

   @Override
   public void write(MarshalledEntry entry)
   {
      if (entry.getKey() instanceof ChangesKey && entry.getValue() instanceof ChangesFilterListsWrapper)
      {
         if (LOG.isDebugEnabled())
         {
            LOG.info("Received list wrapper, start indexing...");
         }
         // updating index
         ChangesFilterListsWrapper wrapper = (ChangesFilterListsWrapper)entry.getValue();
         final ChangesKey key = (ChangesKey)entry.getKey();
         final Cache cache = ctx.getCache();
         try
         {
            Indexer indexer = indexers.get(key.getWsId());
            if (indexer == null)
            {
               LOG.warn("No indexer could be found for the cache entry " + key.toString());
               if (LOG.isDebugEnabled())
               {
                  LOG.debug("The current content of the map of indexers is " + indexers);
               }
            }
            else if (wrapper.withChanges())
            {
               indexer.updateIndex(wrapper.getChanges(), wrapper.getParentChanges());
            }
            else
            {
               indexer.updateIndex(wrapper.getAddedNodes(), wrapper.getRemovedNodes(), wrapper.getParentAddedNodes(),
                       wrapper.getParentRemovedNodes());
            }
         }
         finally
         {
            // Purge the cache to prevent memory leak
            cache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL, Flag.IGNORE_RETURN_VALUES).removeAsync(key)
                    .attachListener(new FutureListener()
                    {
                       @Override
                       public void futureDone(Future future)
                       {
                          if (cache.containsKey(key))
                          {
                             LOG.debug("The entry was not removed properly, it will try to remove it once again");
                             cache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL, Flag.IGNORE_RETURN_VALUES)
                                     .remove(key);
                          }
                       }
                    });
         }
      }
   }

   @Override
   public boolean delete(Object key)
   {
      // This cacheStore only accepts data
      return true;
   }

   @Override
   public void clear() {
      throw new UnsupportedOperationException("This operation is not supported by this component.");
   }

   @Override
   public void purge(Executor threadPool, PurgeListener listener) {
      // This cacheStore only accepts data
   }

   @Override
   public void process(KeyFilter filter, CacheLoaderTask task, Executor executor, boolean fetchValue, boolean fetchMetadata) {
      // This cacheStore only accepts data
   }

   @Override
   public int size() {
      return 0;
   }
}