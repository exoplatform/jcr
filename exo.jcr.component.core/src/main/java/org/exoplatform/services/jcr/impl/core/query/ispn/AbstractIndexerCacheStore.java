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
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.commons.util.IntSet;
import org.infinispan.commons.util.concurrent.CompletableFutures;
import org.infinispan.context.Flag;
import org.infinispan.persistence.spi.InitializationContext;
import org.infinispan.persistence.spi.MarshallableEntry;
import org.infinispan.persistence.spi.NonBlockingStore;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Abstract Indexer Cache Loader defines default implementation of data processing received via cache.
 *
 */
public abstract class AbstractIndexerCacheStore implements NonBlockingStore<Object, Object>
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
   public CompletionStage<Void> start(InitializationContext ctx)
   {
      this.ctx= ctx;
      return CompletableFutures.completedNull();
   }

   @Override
   public CompletionStage<Void> stop()
   {
      indexers.clear();
      return CompletableFutures.completedNull();
   }

   /**
    * This cacheStore only accepts data: it is never read, and its segments hold nothing
    */
   @Override
   public Set<Characteristic> characteristics()
   {
      return EnumSet.of(Characteristic.WRITE_ONLY, Characteristic.SEGMENTABLE);
   }

   @Override
   public CompletionStage<MarshallableEntry<Object, Object>> load(int segment, Object key)
   {
      // This cacheStore only accepts data
      return CompletableFutures.completedNull();
   }

   /**
    * Indexing blocks, so it runs on a blocking thread, which the write of the entry waits for
    */
   @Override
   public CompletionStage<Void> write(int segment, MarshallableEntry<?, ?> entry)
   {
      if (entry.getKey() instanceof ChangesKey && entry.getValue() instanceof ChangesFilterListsWrapper)
      {
         ChangesKey key = (ChangesKey)entry.getKey();
         ChangesFilterListsWrapper wrapper = (ChangesFilterListsWrapper)entry.getValue();
         return ctx.getBlockingManager().runBlocking(() -> updateIndex(key, wrapper), key);
      }
      return CompletableFutures.completedNull();
   }

   private void updateIndex(final ChangesKey key, ChangesFilterListsWrapper wrapper)
   {
      if (LOG.isDebugEnabled())
      {
         LOG.info("Received list wrapper, start indexing...");
      }
      // updating index
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
         final AdvancedCache localCache =
            cache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL, Flag.IGNORE_RETURN_VALUES);
         localCache.removeAsync(key).whenComplete((value, error) -> {
            if (cache.containsKey(key))
            {
               LOG.debug("The entry was not removed properly, it will try to remove it once again");
               localCache.removeAsync(key);
            }
         });
      }
   }

   @Override
   public CompletionStage<Boolean> delete(int segment, Object key)
   {
      // This cacheStore only accepts data
      return CompletableFutures.completedTrue();
   }

   @Override
   public CompletionStage<Void> addSegments(IntSet segments)
   {
      // This cacheStore only accepts data
      return CompletableFutures.completedNull();
   }

   @Override
   public CompletionStage<Void> removeSegments(IntSet segments)
   {
      // This cacheStore only accepts data
      return CompletableFutures.completedNull();
   }

   @Override
   public CompletionStage<Void> clear() {
      return CompletableFuture.failedFuture(
         new UnsupportedOperationException("This operation is not supported by this component."));
   }
}