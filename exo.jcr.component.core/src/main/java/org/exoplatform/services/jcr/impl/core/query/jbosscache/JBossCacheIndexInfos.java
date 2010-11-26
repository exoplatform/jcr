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

import org.exoplatform.services.jcr.impl.core.query.IndexerIoMode;
import org.exoplatform.services.jcr.impl.core.query.IndexerIoModeHandler;
import org.exoplatform.services.jcr.impl.core.query.IndexerIoModeListener;
import org.exoplatform.services.jcr.impl.core.query.lucene.IndexInfos;
import org.exoplatform.services.jcr.impl.core.query.lucene.MultiIndex;
import org.exoplatform.services.jcr.jbosscache.PrivilegedJBossCacheHelper;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.jboss.cache.Cache;
import org.jboss.cache.Fqn;
import org.jboss.cache.Node;
import org.jboss.cache.notifications.annotation.CacheListener;
import org.jboss.cache.notifications.annotation.NodeModified;
import org.jboss.cache.notifications.event.NodeModifiedEvent;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * List of indexes is stored in FS and all operations with it are wrapped by IndexInfos class. In
 * standalone mode index and so the list of indexes are managed by indexer and can't be changed 
 * externally. 
 * But in cluster environment all JCR Indexers are reading from shared file system and only one
 * cluster node is writing this index. So read-only cluster nodes should be notified when content
 * of index (actually list of index segments) is changed. 
 * This class is responsible for storing list of segments (indexes) in distributed JBoss Cache 
 * instance.
 * 
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: JbossCacheIndexInfos.java 34360 2009-07-22 23:58:59Z nzamosenchuk $
 *
 */
@CacheListener
public class JBossCacheIndexInfos extends IndexInfos implements IndexerIoModeListener
{

   private final Log log = ExoLogger.getLogger("exo.jcr.component.core.JBossCacheIndexInfos");

   private static final String INDEX_NAMES = "$names".intern();

   private static final String SYSINDEX_NAMES = "$sysNames".intern();

   private static final String LIST_KEY = "$listOfIndexes".intern();

   private final Cache<Serializable, Object> cache;

   /**
    * Flag notifies if this IndexInfos is from system search manager or not.
    */
   private boolean system;

   /**
    * Used to retrieve the current mode
    */
   private final IndexerIoModeHandler modeHandler;

   /**
    * This FQN points to cache node, where list of indexes for this {@link IndexInfos} instance is stored.
    */
   private final Fqn namesFqn;

   /**
    * @param cache instance of JbossCache that is used to deliver index names
    */
   public JBossCacheIndexInfos(Fqn<String> rootFqn, Cache<Serializable, Object> cache, boolean system,
      IndexerIoModeHandler modeHandler)
   {
      this(rootFqn, DEFALUT_NAME, cache, system, modeHandler);
   }

   /**
    * @param fileName where index names are stored.
    * @param cache instance of JbossCache that is used to deliver index names
    */
   public JBossCacheIndexInfos(Fqn<String> rootFqn, String fileName, Cache<Serializable, Object> cache, boolean system,
      IndexerIoModeHandler modeHandler)
   {
      super(fileName);
      this.cache = cache;
      this.modeHandler = modeHandler;
      modeHandler.addIndexerIoModeListener(this);
      // store parsed FQN to avoid it's parsing each time cache event is generated
      namesFqn = Fqn.fromRelativeElements(rootFqn, system ? SYSINDEX_NAMES : INDEX_NAMES);
      Node<Serializable, Object> cacheRoot = cache.getRoot();
      // prepare cache structures
      if (!cacheRoot.hasChild(namesFqn))
      {
         cache.getInvocationContext().getOptionOverrides().setCacheModeLocal(true);
         cacheRoot.addChild(namesFqn).setResident(true);
      }
      else
      {
         cache.getNode(namesFqn).setResident(true);
      }
      if (modeHandler.getMode() == IndexerIoMode.READ_ONLY)
      {
         // Currently READ_ONLY is set, so new lists should be fired to multiIndex.
         cache.addCacheListener(this);
      }
   }

   /**
    * @see org.exoplatform.services.jcr.impl.core.query.IndexerIoModeListener#onChangeMode(org.exoplatform.services.jcr.impl.core.query.IndexerIoMode)
    */
   public void onChangeMode(IndexerIoMode mode)
   {
      if (mode == IndexerIoMode.READ_WRITE)
      {
         // Now is read-write. Index list is actual and shouldn't be refreshed.
         // Remove listener to avoid asserting if ioMode is RO on each cache event 
         cache.removeCacheListener(this);
         // re-read from FS current actual list.
         try
         {
            super.read();
         }
         catch (IOException e)
         {
            log.error("Cannot read the list of indexe names", e);
         }
      }
      else
      {
         // Currently READ_ONLY is set, so new lists should be fired to multiIndex.
         cache.addCacheListener(this);
      }
   }

   /**
    * @see org.exoplatform.services.jcr.impl.core.query.lucene.IndexInfos#write()
    */
   @Override
   public void write() throws IOException
   {
      // if READ_WRITE and is dirty, then flush. 
      if (isDirty() && modeHandler.getMode() == IndexerIoMode.READ_WRITE)
      {
         // write to FS
         super.write();
         // write to cache
         PrivilegedJBossCacheHelper.put(cache, namesFqn, LIST_KEY, getNames());
      }
   }

   /**
    * CacheListener method, that accepts event, when cache node changed. This class is registered as cache listener, 
    * only in READ_ONLY mode.
    * @param event
    */
   @NodeModified
   public void cacheNodeModified(NodeModifiedEvent event)
   {
      if (!event.isPre() && event.getFqn().equals(namesFqn))
      {
         Set<String> set = null;
         Map<?, ?> data = event.getData();
         if (data == null)
         {
            log.warn("The data map is empty");
         }
         else
         {
            set = (Set<String>)data.get(LIST_KEY);
         }
         if (set == null)
         {
            log.warn("The data cannot be found, we will try to get it from the cache");
            // read from cache to update lists
            set = (Set<String>)cache.get(namesFqn, LIST_KEY);
         }
         if (set != null)
         {
            setNames(set);
            // callback multiIndex to refresh lists
            try
            {
               MultiIndex multiIndex = getMultiIndex();
               if (multiIndex != null)
               {
                  multiIndex.refreshIndexList();
               }
            }
            catch (IOException e)
            {
               log.error("Failed to update indexes! " + e.getMessage(), e);
            }
         }
      }
   }
}
