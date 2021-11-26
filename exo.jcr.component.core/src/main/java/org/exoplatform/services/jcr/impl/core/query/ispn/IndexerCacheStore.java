/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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

import org.exoplatform.services.jcr.impl.core.query.ChangesFilterListsWrapper;
import org.exoplatform.services.jcr.impl.core.query.IndexerIoMode;
import org.exoplatform.services.jcr.impl.core.query.IndexerIoModeHandler;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.infinispan.container.DataContainer;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStarted;
import org.infinispan.notifications.cachemanagerlistener.annotation.Merged;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.Event;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;
import org.infinispan.persistence.spi.InitializationContext;
import org.infinispan.remoting.transport.Address;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Implements Cache Store for clustered environment. It gives control of Index for coordinator and
 * adds failover mechanisms when it changes.
 * 
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: IndexCacheLoader.java 34360 2009-07-22 23:58:59Z nzamosenchuk $
 *
 */
public class IndexerCacheStore extends AbstractIndexerCacheStore
{
   protected volatile IndexerIoModeHandler modeHandler;

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

   protected EmbeddedCacheManager cacheManager;


   @Override
   public void init(InitializationContext ctx)
   {
      super.init(ctx);
      this.cacheManager = ctx.getCache().getCacheManager();
      listener = new CacheListener();
      cacheManager.addListener(listener);
   }

   /**
    * Get the mode handler
    */
   public IndexerIoModeHandler getModeHandler()
   {
      if (modeHandler == null)
      {

         if (ctx.getCache().getStatus() != ComponentStatus.RUNNING)
         {
            throw new IllegalStateException("The cache should be started first");
         }
         synchronized (this)
         {
            if (modeHandler == null)
            {
               this.modeHandler =
                  new IndexerIoModeHandler(cacheManager.isCoordinator()
                     || ctx.getCache().getAdvancedCache().getRpcManager() == null ? IndexerIoMode.READ_WRITE
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
   protected void activeStatusChanged(final boolean newActiveState)
   {
      // originally came from EXOJCR-1345. 
      // Deadlock occurs inside JGroups, if calling some operations inside the same thread,
      // invoking ViewChanged. That's why, need to perform operation in separated async tread.
      new Thread(new Runnable()
      {
         public void run()
         {
            coordinator = newActiveState;

            getModeHandler().setMode(coordinator ? IndexerIoMode.READ_WRITE : IndexerIoMode.READ_ONLY);
            LOG.info("Set indexer io mode to:" + (coordinator ? IndexerIoMode.READ_WRITE : IndexerIoMode.READ_ONLY));

            if (coordinator)
            {
               doPushState();
            }
         }
      }, "JCR Indexer ActiveStatusChanged-handler").start();
   }

   /**
    *  Flushes all cache content to underlying CacheStore
    */
   @SuppressWarnings("rawtypes")
   protected void doPushState()
   {
      final boolean debugEnabled = LOG.isDebugEnabled();

      if (debugEnabled)
      {
         LOG.debug("start pushing in-memory state to cache cacheLoader collection");
      }

      Map<String, ChangesFilterListsWrapper> changesMap = new HashMap<String, ChangesFilterListsWrapper>();
      List<ChangesKey> processedItemKeys = new ArrayList<ChangesKey>();

      DataContainer dc = ctx.getCache().getAdvancedCache().getDataContainer();
      Set keys = dc.keySet();
      InternalCacheEntry entry;
      // collect all cache entries into the following map:
      // <WS ID> : <Contracted lists of added/removed nodes>
      for (Object k : keys)
      {
         if ((entry = dc.get(k)) != null)
         {
            if (entry.getValue() instanceof ChangesFilterListsWrapper && entry.getKey() instanceof ChangesKey)
            {
               if (debugEnabled)
               {
                  LOG.debug("Received list wrapper, start indexing...");
               }
               // get stale List that was not processed
               ChangesFilterListsWrapper staleListIncache = (ChangesFilterListsWrapper)entry.getValue();
               ChangesKey key = (ChangesKey)entry.getKey();
               // get newly created wrapper instance
               ChangesFilterListsWrapper listToPush = changesMap.get(key.getWsId());
               if (listToPush == null)
               {
                  listToPush =
                     new ChangesFilterListsWrapper(new HashSet<String>(), new HashSet<String>(), new HashSet<String>(),
                        new HashSet<String>());
                  changesMap.put(key.getWsId(), listToPush);
               }
               // copying lists into the new wrapper
               if (staleListIncache.getParentAddedNodes() != null)
                  listToPush.getParentAddedNodes().addAll(staleListIncache.getParentAddedNodes());
               if (staleListIncache.getParentRemovedNodes() != null)
                  listToPush.getParentRemovedNodes().addAll(staleListIncache.getParentRemovedNodes());

               if (staleListIncache.getAddedNodes() != null)
                  listToPush.getAddedNodes().addAll(staleListIncache.getAddedNodes());
               if (staleListIncache.getRemovedNodes() != null)
                  listToPush.getRemovedNodes().addAll(staleListIncache.getRemovedNodes());
               processedItemKeys.add(key);
            }
         }

      }

      // process all lists for each workspace
      for (Entry<String, ChangesFilterListsWrapper> changesEntry : changesMap.entrySet())
      {
         // create key based on wsId and generated id
         ChangesKey changesKey = new ChangesKey(changesEntry.getKey(), IdGenerator.generate());
         ctx.getCache().putAsync(changesKey, changesEntry.getValue());
      }

      for (ChangesKey key : processedItemKeys)
      {
         ctx.getCache().removeAsync(key);
      }

      if (debugEnabled)
      {
         LOG.debug("in-memory state passed to cache cacheStore successfully");
      }
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
      @Merged
      public void viewChange(ViewChangedEvent event)
      {
         LOG.info("The intercepted EventType is : " + event.getType());
         LOG.info("The old list of members : " + event.getOldMembers());
         LOG.info("The new list of members: : " + event.getNewMembers());

         boolean tmp = isCoordinator(event.getNewMembers(), event.getLocalAddress());

         if (coordinator != tmp || (tmp && event.isMergeView()))
         {
            activeStatusChanged(tmp);
         }
      }

   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void stop()
   {
      cacheManager.removeListener(listener);
      super.stop();
   }

}
