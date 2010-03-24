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
import org.exoplatform.services.jcr.impl.core.query.lucene.IndexUpdateMonitor;
import org.exoplatform.services.jcr.impl.core.query.lucene.IndexUpdateMonitorListener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.jboss.cache.Cache;
import org.jboss.cache.CacheException;
import org.jboss.cache.Fqn;
import org.jboss.cache.Node;
import org.jboss.cache.notifications.annotation.CacheListener;
import org.jboss.cache.notifications.annotation.NodeModified;
import org.jboss.cache.notifications.event.NodeModifiedEvent;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@exoplatform.org">Sergey Kabashnyuk</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 2009-07-22 23:58:59Z ksm $
 *
 */
@CacheListener
public class JBossCacheIndexUpdateMonitor implements IndexUpdateMonitor, IndexerIoModeListener
{
   /**
    * Logger instance for this class
    */
   private final Log log = ExoLogger.getLogger(JBossCacheIndexUpdateMonitor.class);

   private final Cache<Serializable, Object> cache;

   private boolean localUpdateInProgress = false;

   private static final String INDEX_PARAMETERS = "$index_parameters".intern();

   private static final String SYSINDEX_PARAMETERS = "$sysindex_parameters".intern();

   private final static String PARAMETER_NAME = "index-update-in-progress";

   public final IndexerIoModeHandler modeHandler;

   /**
    * The list of all the listeners
    */
   private final List<IndexUpdateMonitorListener> listeners;

   /**
    * This FQN points to cache node, where list of indexes for this {@link IndexInfos} instance is stored.
    */
   private final Fqn parametersFqn;

   /**
    * @param cache instance of JbossCache that is used to deliver index names
    */
   public JBossCacheIndexUpdateMonitor(Cache<Serializable, Object> cache, boolean system,
      IndexerIoModeHandler modeHandler)
   {
      this.cache = cache;
      this.modeHandler = modeHandler;
      this.listeners = new CopyOnWriteArrayList<IndexUpdateMonitorListener>();
      // store parsed FQN to avoid it's parsing each time cache event is generated
      this.parametersFqn = Fqn.fromString(system ? INDEX_PARAMETERS : SYSINDEX_PARAMETERS);
      modeHandler.addIndexerIoModeListener(this);
      Node<Serializable, Object> cacheRoot = cache.getRoot();

      // prepare cache structures
      if (!cacheRoot.hasChild(parametersFqn))
      {
         cache.getInvocationContext().getOptionOverrides().setCacheModeLocal(true);
         cacheRoot.addChild(parametersFqn).setResident(true);
      }
      else
      {
         cache.getNode(parametersFqn).setResident(true);
      }

      if (IndexerIoMode.READ_WRITE == modeHandler.getMode())
      {
         // global, replicated set
         setUpdateInProgress(false, true);
      }
      else
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
         // In READ_WRITE, the value of UpdateInProgress is changed locally so no need to listen
         // to the cache
         cache.removeCacheListener(this);
      }
      else
      {
         // In READ_ONLY, the value of UpdateInProgress will be changed remotely, so we have
         // no need but to listen to the cache to be notified when the value changes
         cache.addCacheListener(this);
      }
   }

   /**
    * @see org.exoplatform.services.jcr.impl.core.query.lucene.IndexUpdateMonitor#getUpdateInProgress()
    */
   public boolean getUpdateInProgress()
   {
      if (IndexerIoMode.READ_ONLY == modeHandler.getMode())
      {
         Object value = cache.get(parametersFqn, PARAMETER_NAME);
         return value != null ? (Boolean)value : false;
      }
      else
      {
         // this node is read-write, so must read local value.
         // Local value is updated every time, but remote cache value is skipped is volatile changes are performed 
         return localUpdateInProgress;
      }
   }

   /**
    * @see org.exoplatform.services.jcr.impl.core.query.lucene.IndexUpdateMonitor#setUpdateInProgress(boolean, boolean)
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
            cache.put(parametersFqn, PARAMETER_NAME, new Boolean(updateInProgress));

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
    * @see org.exoplatform.services.jcr.impl.core.query.lucene.IndexUpdateMonitor#addIndexUpdateMonitorListener(org.exoplatform.services.jcr.impl.core.query.lucene.IndexUpdateMonitorListener)
    */
   public void addIndexUpdateMonitorListener(IndexUpdateMonitorListener listener)
   {
      listeners.add(listener);
   }

   /**
    * Called when a node of the cache has been modified. It will be used to trigger events
    * when the value of <code>updateInProgress</code> has been changed remotely
    * @param event the event
    */
   @NodeModified
   public void cacheNodeModified(NodeModifiedEvent event)
   {
      if (!event.isPre() && event.getFqn().equals(parametersFqn))
      {
         Object value = null;
         Map<?, ?> data = event.getData();
         if (data == null)
         {
            log.warn("The data map is empty");
         }
         else
         {
            value = data.get(PARAMETER_NAME);
         }
         if (value == null)
         {
            log.warn("The data cannot be found, we will try to get it from the cache");
            value = cache.get(parametersFqn, PARAMETER_NAME);
         }
         boolean updateInProgress = value != null ? (Boolean)value : false;
         for (IndexUpdateMonitorListener listener : listeners)
         {
            listener.onUpdateInProgressChange(updateInProgress);
         }
      }
   }
}
