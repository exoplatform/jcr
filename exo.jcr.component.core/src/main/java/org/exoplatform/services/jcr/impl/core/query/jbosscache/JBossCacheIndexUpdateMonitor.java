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
import org.exoplatform.services.jcr.impl.core.query.lucene.IndexUpdateMonitor;
import org.exoplatform.services.jcr.impl.core.query.lucene.IndexUpdateMonitorListener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.jboss.cache.Cache;
import org.jboss.cache.CacheException;
import org.jboss.cache.CacheSPI;
import org.jboss.cache.Fqn;
import org.jboss.cache.Node;
import org.jboss.cache.lock.LockManager;
import org.jboss.cache.lock.LockType;
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

   private final static Fqn PARAMETER_ROOT = Fqn.fromString("INDEX_UPDATE_MONITOR");

   private final static String PARAMETER_NAME = "index-update-in-progress";

   public final IndexerIoModeHandler modeHandler;

   /**
    * The list of all the listeners
    */
   private final List<IndexUpdateMonitorListener> listeners;

   /**
    * @param cache instance of JbossCache that is used to deliver index names
    */
   public JBossCacheIndexUpdateMonitor(Cache<Serializable, Object> cache, IndexerIoModeHandler modeHandler)
   {
      this.cache = cache;
      this.modeHandler = modeHandler;
      this.listeners = new CopyOnWriteArrayList<IndexUpdateMonitorListener>();
      modeHandler.addIndexerIoModeListener(this);
      Node<Serializable, Object> cacheRoot = cache.getRoot();

      // prepare cache structures
      if (!cacheRoot.hasChild(PARAMETER_ROOT))
      {
         cache.getInvocationContext().getOptionOverrides().setCacheModeLocal(true);
         cacheRoot.addChild(PARAMETER_ROOT).setResident(true);
      }
      else
      {
         cache.getNode(PARAMETER_ROOT).setResident(true);
      }

      if (IndexerIoMode.READ_WRITE == modeHandler.getMode())
      {
         setUpdateInProgress(false);
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
      Object value = cache.get(PARAMETER_ROOT, PARAMETER_NAME);
      return value != null ? (Boolean)value : false;
   }

   /**
    *  Returns true if the node is locked (either for reading or writing) by anyone, and false otherwise.
    * @param name
    * @return
    */
   public boolean isLocked(String name)
   {
      LockManager lm = ((CacheSPI<Serializable, Object>)cache).getComponentRegistry().getComponent(LockManager.class);
      return lm.isLocked(Fqn.fromRelativeFqn(PARAMETER_ROOT, Fqn.fromString(name)));
   }

   /**
    *  Acquires a lock of type lockType, for a given owner
    * @param name
    * @param lockType
    * @return
    * @throws InterruptedException
    */
   public boolean lock(String name, LockType lockType)
   {

      LockManager lm = ((CacheSPI<Serializable, Object>)cache).getComponentRegistry().getComponent(LockManager.class);
      try
      {
         return lm.lock(Fqn.fromRelativeFqn(PARAMETER_ROOT, Fqn.fromString(name)), lockType, Integer.MAX_VALUE);
      }
      catch (InterruptedException e)
      {
         log.warn("An error occurs while tryning to lock the node " + name, e);
      }
      return false;
   }

   /**
    * @see org.exoplatform.services.jcr.impl.core.query.lucene.IndexUpdateMonitor#setUpdateInProgress(boolean)
    */
   public void setUpdateInProgress(boolean updateInProgress)
   {
      if (IndexerIoMode.READ_ONLY == modeHandler.getMode())
      {
         throw new IllegalStateException("Unable to set updateInProgress value in IndexerIoMode.READ_ONLY mode");
      }
      try
      {
         cache.put(PARAMETER_ROOT, PARAMETER_NAME, new Boolean(updateInProgress));
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
    * Releases the lock passed in
    * @param name
    */
   public void unlock(String name)
   {
      LockManager lm = ((CacheSPI<Serializable, Object>)cache).getComponentRegistry().getComponent(LockManager.class);
      lm.unlock(Fqn.fromRelativeFqn(PARAMETER_ROOT, Fqn.fromString(name)), cache.getInvocationContext()
         .getGlobalTransaction());
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
      if (!event.isPre() && event.getFqn().equals(PARAMETER_ROOT))
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
            value = cache.get(PARAMETER_ROOT, PARAMETER_NAME);
         }
         boolean updateInProgress = value != null ? (Boolean)value : false;
         for (IndexUpdateMonitorListener listener : listeners)
         {
            listener.onUpdateInProgressChange(updateInProgress);
         }
      }
   }
}
