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
import org.exoplatform.services.jcr.impl.core.query.Indexer;
import org.exoplatform.services.jcr.impl.core.query.IndexerIoMode;
import org.exoplatform.services.jcr.impl.core.query.IndexerIoModeHandler;
import org.exoplatform.services.jcr.impl.core.query.QueryHandler;
import org.exoplatform.services.jcr.impl.core.query.SearchManager;
import org.exoplatform.services.jcr.impl.core.query.jbosscache.ChangesFilterListsWrapper;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.infinispan.Cache;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.loaders.CacheLoaderConfig;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.marshall.StreamingMarshaller;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStarted;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.Event;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;
import org.infinispan.remoting.transport.Address;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: IndexCacheLoader.java 34360 2009-07-22 23:58:59Z nzamosenchuk $
 *
 */
public class IndexerCacheStore extends AbstractInputCacheStore
{

   protected CacheListener listener;

   /**
    * Address instance that allows SingletonStore to find out whether it became the coordinator of the cluster, or
    * whether it stopped being it. This dictates whether the SingletonStore is active or not.
    */
   private Address localAddress;

   /**
    * Whether the the current cache is the coordinator and therefore SingletonStore is active. Being active means
    * delegating calls to the underlying cache loader.
    */
   private volatile boolean coordinator;

   EmbeddedCacheManager cacheManager;

   protected volatile IndexerIoModeHandler modeHandler;

   /**
    * A map of all the indexers that has been registered
    */
   private final Map<Integer, Indexer> indexers = new HashMap<Integer, Indexer>();

   private static final Log log = ExoLogger.getLogger("exo.jcr.component.core.IndexerCacheLoader");

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
      indexers.put(searchManager.getWsId().hashCode(), new Indexer(searchManager, parentSearchManager, handler,
         parentHandler));
      if (log.isDebugEnabled())
      {
         log.debug("Register " + searchManager.getWsId() + " " + this + " in " + indexers);
      }
   }

   @Override
   public void init(CacheLoaderConfig config, Cache<?, ?> cache, StreamingMarshaller m) throws CacheLoaderException
   {
      super.init(config, cache, m);
      this.cacheManager = cache == null ? null : (EmbeddedCacheManager)cache.getCacheManager();
      listener = new CacheListener();
      cache.addListener(listener);
   }

   /**
    * @see org.infinispan.loaders.CacheStore#store(org.infinispan.container.entries.InternalCacheEntry)
    */
   public void store(InternalCacheEntry entry) throws CacheLoaderException
   {
      if (entry.getValue() instanceof ChangesFilterListsWrapper && entry.getKey() instanceof ChangesKey)
      {
         if (log.isDebugEnabled())
         {
            log.info("Received list wrapper, start indexing...");
         }
         // updating index
         ChangesFilterListsWrapper wrapper = (ChangesFilterListsWrapper)entry.getValue();
         ChangesKey key = (ChangesKey)entry.getKey();
         try
         {
            Indexer indexer = indexers.get(key.getWsId());
            if (indexer == null)
            {
               log.warn("No indexer could be found for the cache entry " + key.toString());
               if (log.isDebugEnabled())
               {
                  log.debug("The current content of the map of indexers is " + indexers);
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
            if (modeHandler.getMode() == IndexerIoMode.READ_WRITE)
            {
               // remove the data from the cache
               cache.remove(key);
            }
         }
      }
   }

   /**
    * Set the mode handler
    * @param modeHandler
    */
   IndexerIoModeHandler getModeHandler()
   {
      if (modeHandler == null)
      {

         if (cache.getStatus() != ComponentStatus.RUNNING)
         {
            throw new IllegalStateException("The cache should be started first");
         }
         synchronized (this)
         {
            if (modeHandler == null)
            {
               this.modeHandler =
                  new IndexerIoModeHandler(cacheManager.isCoordinator() ? IndexerIoMode.READ_WRITE
                     : IndexerIoMode.READ_ONLY);
            }
         }
      }
      return modeHandler;
   }

   /**
    * Indicates whether the current cache is the coordinator of the cluster.  This implementation assumes that the
    * coordinator is the first member in the list.
    *
    * @param newView View instance containing the new view of the cluster
    * @return whether the current cache is the coordinator or not.
    */
   private boolean isCoordinator(List<Address> newView, Address currentAddress)
   {
      if (!currentAddress.equals(localAddress))
      {
         localAddress = currentAddress;
      }
      if (localAddress != null)
      {
         return !newView.isEmpty() && localAddress.equals(newView.get(0));
      }
      else
      {
         /* Invalid new view, so previous value returned */
         return coordinator;
      }
   }

   /**
    * Method called when the cache either becomes the coordinator or stops being the coordinator. If it becomes the
    * coordinator, it can optionally start the in-memory state transfer to the underlying cache store.
    *
    * @param newActiveState true if the cache just became the coordinator, false if the cache stopped being the
    *                       coordinator.
    */
   protected void activeStatusChanged(boolean newActiveState)
   {
      coordinator = newActiveState;

      if (modeHandler != null)
      {
         modeHandler.setMode(coordinator ? IndexerIoMode.READ_WRITE : IndexerIoMode.READ_ONLY);
         log.info("Set indexer io mode to:" + (coordinator ? IndexerIoMode.READ_WRITE : IndexerIoMode.READ_ONLY));
      }

      if (coordinator)
      {
         doPushState();
      }
   }

   protected void doPushState()
   {
      //      final boolean debugEnabled = log.isDebugEnabled();
      //
      //      if (debugEnabled)
      //      {
      //         log.debug("start pushing in-memory state to cache cacheLoader collection");
      //      }
      //
      //      // merging all lists stored in memory
      //      Collection<NodeSPI> children = cache.getRoot().getChildren();
      //      for (NodeSPI wsChildren : children)
      //      {
      //         final Set<String> removedNodes = new HashSet<String>();
      //         final Set<String> addedNodes = new HashSet<String>();
      //         final Set<String> parentRemovedNodes = new HashSet<String>();
      //         final Set<String> parentAddedNodes = new HashSet<String>();
      //         Collection<NodeSPI> changes = wsChildren.getChildren();
      //         for (NodeSPI aChildren : changes)
      //         {
      //            Fqn<?> fqn = aChildren.getFqn();
      //            Object value = cache.get(fqn, JBossCacheIndexChangesFilter.LISTWRAPPER);
      //            if (value instanceof ChangesFilterListsWrapper)
      //            {
      //               // get wrapper object
      //               ChangesFilterListsWrapper listsWrapper = (ChangesFilterListsWrapper)value;
      //               if (listsWrapper.withChanges())
      //               {
      //                  if (listsWrapper.getChanges() != null)
      //                  {
      //                     // get search manager lists
      //                     addedNodes.addAll(listsWrapper.getChanges().getAddIds());
      //                     removedNodes.addAll(listsWrapper.getChanges().getRemove());
      //                  }
      //                  if (listsWrapper.getParentChanges() != null)
      //                  {
      //                     // parent search manager lists
      //                     parentAddedNodes.addAll(listsWrapper.getParentChanges().getAddIds());
      //                     parentRemovedNodes.addAll(listsWrapper.getParentChanges().getRemove());
      //                  }
      //               }
      //               else
      //               {
      //                  // get search manager lists
      //                  addedNodes.addAll(listsWrapper.getAddedNodes());
      //                  removedNodes.addAll(listsWrapper.getRemovedNodes());
      //                  // parent search manager lists
      //                  parentAddedNodes.addAll(listsWrapper.getParentAddedNodes());
      //                  parentRemovedNodes.addAll(listsWrapper.getParentAddedNodes());
      //               }
      //            }
      //         }
      //         String id = IdGenerator.generate();
      //         cache.put(Fqn.fromRelativeElements(wsChildren.getFqn(), id), JBossCacheIndexChangesFilter.LISTWRAPPER,
      //            new ChangesFilterListsWrapper(addedNodes, removedNodes, parentAddedNodes, parentRemovedNodes));
      //         // Once we put the merged changes into the cache we can remove other changes from the cache 
      //         for (NodeSPI aChildren : children)
      //         {
      //            // Remove the node from the cache and do it asynchronously 
      //            cache.getInvocationContext().getOptionOverrides().setForceAsynchronous(true);
      //            cache.removeNode(aChildren.getFqn());
      //         }
      //      }
      //      if (debugEnabled)
      //      {
      //         log.debug("in-memory state passed to cache cacheLoader successfully");
      //      }
      //      return null;
   }

   @Listener
   public class CacheListener
   {
      @CacheStarted
      public void cacheStarted(Event e)
      {
         localAddress = cacheManager.getAddress();
         coordinator = cacheManager.isCoordinator();
      }

      /**
       * The cluster formation changed, so determine whether the current cache stopped being the coordinator or became
       * the coordinator. This method can lead to an optional in memory to cache loader state push, if the current cache
       * became the coordinator. This method will report any issues that could potentially arise from this push.
       */
      @ViewChanged
      public void viewChange(ViewChangedEvent event)
      {
         boolean tmp = isCoordinator(event.getNewMembers(), event.getLocalAddress());

         if (coordinator != tmp)
         {
            activeStatusChanged(tmp);
         }
      }
   }

}
