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
import org.exoplatform.services.jcr.impl.core.query.IndexerIoMode;
import org.exoplatform.services.jcr.impl.core.query.IndexerIoModeHandler;
import org.exoplatform.services.jcr.impl.core.query.QueryHandler;
import org.exoplatform.services.jcr.impl.core.query.SearchManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.context.Flag;
import org.infinispan.loaders.AbstractCacheStore;
import org.infinispan.loaders.AbstractCacheStoreConfig;
import org.infinispan.loaders.CacheLoaderConfig;
import org.infinispan.loaders.CacheLoaderException;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Abstract Indexer Cache Loader defines default implementation of data processing received via cache.
 * 
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: AbstractInputCacheStore.java 34360 2009-07-22 23:58:59Z nzamosenchuk $
 *
 */
public abstract class AbstractIndexerCacheStore extends AbstractCacheStore
{

   /**
    * A map of all the indexers that has been registered
    */
   protected final Map<String, Indexer> indexers = new HashMap<String, Indexer>();

   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.impl.infinispan.v5.IndexerCacheLoader");//NOSONAR

   /**
    * Executor used to remove all the entries that have already been treated. We
    * need it because starting from ISPN 5.1 even changes applied in auto commit
    * mode are done within a transaction so if we try to drop the entries
    * directly we get an IllegalStateException because the operation is done
    * during a commit and if we try to execute it outside the transaction we
    * create a deadlock as the lock on the entry is hold by the suspended transaction
    */
   private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactory()
   {
      private final ThreadGroup group;
      {
         SecurityManager s = System.getSecurityManager();
         group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
      }

      public Thread newThread(Runnable r)
      {
         Thread t = new Thread(group, r, "IndexerCacheStoreCleaner", 0);
         t.setDaemon(true);
         return t;
      }
   });

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
    * @see org.infinispan.loaders.CacheStore#store(org.infinispan.container.entries.InternalCacheEntry)
    */
   public void store(InternalCacheEntry entry) throws CacheLoaderException
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
            if (getModeHandler().getMode() == IndexerIoMode.READ_WRITE)
            {
               // remove the data from the cache
               EXECUTOR.submit(new Runnable()
               {
                  public void run()
                  {
                     cache.getAdvancedCache()
                        .withFlags(Flag.SKIP_LOCKING, Flag.FORCE_ASYNCHRONOUS, Flag.SKIP_REMOTE_LOOKUP)
                        .removeAsync(key);
                  }
               });
            }
         }
      }
   }

   /**
   * {@inheritDoc}
   */
   @Override
   public void stop() throws CacheLoaderException
   {
      indexers.clear();
      super.stop();
   }

   /**
    * @return IndexerIoModeHandler instance
    */
   public abstract IndexerIoModeHandler getModeHandler();

   // ===================================================

   /**
    * @see org.infinispan.loaders.CacheLoader#getConfigurationClass()
    */
   public Class<? extends CacheLoaderConfig> getConfigurationClass()
   {
      return AbstractCacheStoreConfig.class;
   }

   /**
    * @see org.infinispan.loaders.CacheStore#fromStream(java.io.ObjectInput)
    */
   public void fromStream(ObjectInput inputStream) throws CacheLoaderException
   {
      throw new UnsupportedOperationException("This operation is not supported by this component.");
   }

   /**
    * @see org.infinispan.loaders.CacheStore#toStream(java.io.ObjectOutput)
    */
   public void toStream(ObjectOutput outputStream) throws CacheLoaderException
   {
      throw new UnsupportedOperationException("This operation is not supported by this component.");
   }

   /**
    * @see org.infinispan.loaders.AbstractCacheStore#purgeInternal()
    */
   @Override
   protected void purgeInternal() throws CacheLoaderException
   {
      // This cacheStore only accepts data
   }

   /**
    * @see org.infinispan.loaders.CacheStore#clear()
    */
   public void clear() throws CacheLoaderException
   {
      throw new UnsupportedOperationException("This operation is not supported by this component.");
   }

   /**
    * @see org.infinispan.loaders.CacheStore#remove(java.lang.Object)
    */
   public boolean remove(Object key) throws CacheLoaderException
   {
      // This cacheStore only accepts data
      return true;
   }

   /**
    * @see org.infinispan.loaders.CacheLoader#load(java.lang.Object)
    */
   public InternalCacheEntry load(Object key) throws CacheLoaderException
   {
      // This cacheStore only accepts data
      return null;
   }

   /**
    * @see org.infinispan.loaders.CacheLoader#load(int)
    */
   public Set<InternalCacheEntry> load(int numEntries) throws CacheLoaderException
   {
      // This cacheStore only accepts data
      return null;
   }

   /**
    * @see org.infinispan.loaders.CacheLoader#loadAll()
    */
   public Set<InternalCacheEntry> loadAll() throws CacheLoaderException
   {
      // This cacheStore only accepts data
      return null;
   }

   /**
    * @see org.infinispan.loaders.CacheLoader#loadAllKeys(java.util.Set)
    */
   public Set<Object> loadAllKeys(Set<Object> keysToExclude) throws CacheLoaderException
   {
      // This cacheStore only accepts data
      return null;
   }

}