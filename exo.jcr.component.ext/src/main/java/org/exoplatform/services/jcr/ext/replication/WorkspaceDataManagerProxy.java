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
package org.exoplatform.services.jcr.ext.replication;

import org.exoplatform.services.jcr.dataflow.ItemDataKeeper;
import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.persistent.ItemsPersistenceListener;
import org.exoplatform.services.jcr.impl.core.lock.WorkspaceLockManager;
import org.exoplatform.services.jcr.impl.core.query.SearchManager;
import org.exoplatform.services.jcr.impl.dataflow.persistent.CacheableWorkspaceDataManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.InvalidItemStateException;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.<br/>
 * Proxy of WorkspaceDataManager for "proxy" mode of replication to let replicator not to make
 * persistent changes but replicate cache, indexes etc instead. This is the case if persistent
 * replication is done with some external way (by repliucation enabled RDB or AS etc)
 * 
 * @author Gennady Azarenkov
 * @version $Id: WorkspaceDataManagerProxy.java 34445 2009-07-24 07:51:18Z dkatayev $
 */

public class WorkspaceDataManagerProxy implements ItemDataKeeper
{

   /**
    * The apache logger.
    */
   private static Log log = ExoLogger.getLogger("exo.jcr.component.ext.WorkspaceDataManagerProxy");

   /**
    * The ItemsPersistenceListeners.
    */
   private List<ItemsPersistenceListener> listeners;

   /**
    * WorkspaceDataManagerProxy constructor.
    * 
    * @param dataManager
    *          the CacheableWorkspaceDataManager
    * @param searchIndex
    *          the SearchManager
    * @param lockManager
    *          the LockManagerImpl
    */
   public WorkspaceDataManagerProxy(CacheableWorkspaceDataManager dataManager, SearchManager searchIndex,
      WorkspaceLockManager lockManager)
   {
      this.listeners = new ArrayList<ItemsPersistenceListener>();
      listeners.add(dataManager.getCache());
      if (searchIndex != null)
      {
         listeners.add(searchIndex);
      }

      if (lockManager != null)
      {
         listeners.add((ItemsPersistenceListener)lockManager);
      }

      log.info("WorkspaceDataManagerProxy is instantiated");
   }

   /**
    * calls onSaveItems on all registered listeners.
    * 
    * @param changesLog
    *          the ChangesLog with data
    *
    * @throws InvalidItemStateException
    *           will be generate the exception InvalidItemStateException           
    * @throws UnsupportedOperationException
    *           will be generate the exception UnsupportedOperationException
    * @throws RepositoryException
    *           will be generate the exception RepositoryException
    */
   public void save(ItemStateChangesLog changesLog) throws InvalidItemStateException, UnsupportedOperationException,
      RepositoryException
   {
      for (ItemsPersistenceListener listener : listeners)
      {
         listener.onSaveItems(changesLog);
      }
      
      if (log.isDebugEnabled())
      {
         log.debug("ChangesLog sent to " + listeners);
      }
   }
}
