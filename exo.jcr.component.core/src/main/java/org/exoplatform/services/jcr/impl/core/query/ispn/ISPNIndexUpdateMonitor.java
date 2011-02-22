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

import org.exoplatform.services.jcr.impl.core.query.IndexerIoMode;
import org.exoplatform.services.jcr.impl.core.query.IndexerIoModeHandler;
import org.exoplatform.services.jcr.impl.core.query.IndexerIoModeListener;
import org.exoplatform.services.jcr.impl.core.query.lucene.IndexUpdateMonitor;
import org.exoplatform.services.jcr.impl.core.query.lucene.IndexUpdateMonitorListener;
import org.exoplatform.services.jcr.infinispan.PrivilegedISPNCacheHelper;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.infinispan.Cache;
import org.infinispan.CacheException;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by The eXo Platform SAS.
 *
 * Date: 22.02.2011
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: ISPNIndexUpdateMonitor.java 34360 2010-11-11 11:11:11Z tolusha $
 */
@Listener
public class ISPNIndexUpdateMonitor implements IndexUpdateMonitor, IndexerIoModeListener
{
   /**
    * Logger instance for this class
    */
   private final Log log = ExoLogger.getLogger("exo.jcr.component.core.ISPNIndexUpdateMonitor");

   /**
    * ISPN cache.
    */
   private final Cache<Serializable, Object> cache;

   private boolean localUpdateInProgress = false;

   private static final String INDEX_PARAMETERS = "$index_parameters".intern();

   private static final String SYSINDEX_PARAMETERS = "$sysindex_parameters".intern();

   public final IndexerIoModeHandler modeHandler;

   /**
    * The list of all the listeners
    */
   private final List<IndexUpdateMonitorListener> listeners;

   /**
    * Cache key for sending notifications.
    */
   private final IndexUpdateKey updateKey;

   /**
    * 
    * @param wsId
    * @param cache
    * @param system
    * @param modeHandler
    */
   public ISPNIndexUpdateMonitor(String wsId, Cache<Serializable, Object> cache, boolean system,
      IndexerIoModeHandler modeHandler)
   {
      this.cache = cache;
      this.modeHandler = modeHandler;
      this.listeners = new CopyOnWriteArrayList<IndexUpdateMonitorListener>();
      this.modeHandler.addIndexerIoModeListener(this);
      this.updateKey = new IndexUpdateKey(wsId + (system ? SYSINDEX_PARAMETERS : INDEX_PARAMETERS));

      if (IndexerIoMode.READ_WRITE == modeHandler.getMode())
      {
         // global, replicated set
         setUpdateInProgress(false, true);
      }
      else
      {
         // Currently READ_ONLY is set, so new lists should be fired to multiIndex.
         cache.addListener(this);
         Object value = cache.get(updateKey);
         localUpdateInProgress = value != null ? (Boolean)value : false;
      }
   }

   /**
    * {@inheritDoc}
    */
   public void onChangeMode(IndexerIoMode mode)
   {
      if (mode == IndexerIoMode.READ_WRITE)
      {
         // In READ_WRITE, the value of UpdateInProgress is changed locally so no need to listen
         // to the cache
         cache.removeListener(this);
      }
      else
      {
         // In READ_ONLY, the value of UpdateInProgress will be changed remotely, so we have
         // no need but to listen to the cache to be notified when the value changes
         cache.addListener(this);
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean getUpdateInProgress()
   {
      return localUpdateInProgress;
   }

   /**
    * {@inheritDoc}
    */
   public void setUpdateInProgress(boolean updateInProgress, boolean persitentUpdate)
   {
      if (IndexerIoMode.READ_ONLY == modeHandler.getMode())
      {
         throw new IllegalStateException("Unable to set updateInProgress value in IndexerIoMode.READ_ONLY mode");
      }
      try
      {
         // anyway set local update in progress
         localUpdateInProgress = updateInProgress;
         if (persitentUpdate)
         {
            PrivilegedISPNCacheHelper.put(cache, updateKey, new Boolean(updateInProgress));
         }
         for (IndexUpdateMonitorListener listener : listeners)
         {
            listener.onUpdateInProgressChange(updateInProgress);
         }

      }
      catch (CacheException e)
      {
         log.error("Fail to change updateInProgress mode to " + updateInProgress, e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void addIndexUpdateMonitorListener(IndexUpdateMonitorListener listener)
   {
      listeners.add(listener);
   }

   /**
    * Method will be invoked when a cache entry has been modified only in READ_ONLY mode.
    * 
    * @param event
    *          CacheEntryModifiedEvent
    */
   @CacheEntryModified
   public void cacheEntryModified(CacheEntryModifiedEvent event)
   {
      if (!event.isPre() && event.getKey().equals(updateKey))
      {
         Object value = event.getValue();
         localUpdateInProgress = value != null ? (Boolean)value : false;

         for (IndexUpdateMonitorListener listener : listeners)
         {
            listener.onUpdateInProgressChange(localUpdateInProgress);
         }
      }
   }
}
