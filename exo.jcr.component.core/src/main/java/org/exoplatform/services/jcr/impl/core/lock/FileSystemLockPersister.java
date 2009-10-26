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
package org.exoplatform.services.jcr.impl.core.lock;

import org.exoplatform.services.jcr.access.SystemIdentity;
import org.exoplatform.services.jcr.config.LockPersisterEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.query.SearchManager;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspacePersistentDataManager;
import org.exoplatform.services.jcr.observation.ExtendedEvent;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;

/**
 * Store information about locks on file system. After start and before stop it remove all lock
 * properties in repository
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: FileSystemLockPersister.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class FileSystemLockPersister implements LockPersister
{
   /**
    * Name of the parameter.
    */
   private static final String PARAM_ROOT_DIR = "path";

   /**
    * logger.
    */
   private final Log log = ExoLogger.getLogger("jcr.lock.LockPersister");

   /**
    * The directory which stores information of the locks.
    */
   private File rootDir;

   /**
    * Data manager.
    */
   private final WorkspacePersistentDataManager dataManager;

   /**
    * Lock persister configuration.
    */
   private final LockPersisterEntry config;

   /**
    * @param dataManager
    * @param config
    * @throws RepositoryConfigurationException
    * @throws RepositoryException
    */
   public FileSystemLockPersister(WorkspacePersistentDataManager dataManager, WorkspaceEntry config)
      throws RepositoryConfigurationException, RepositoryException
   {
      this.dataManager = dataManager;
      this.config = config.getLockManager().getPersister();
      init();
   }

   /**
    * @param dataManager
    * @param config
    * @param searchManager
    * @throws RepositoryConfigurationException
    * @throws RepositoryException
    */
   public FileSystemLockPersister(WorkspacePersistentDataManager dataManager, WorkspaceEntry config,
      SearchManager searchManager) throws RepositoryConfigurationException, RepositoryException
   {
      this.dataManager = dataManager;
      this.config = config.getLockManager().getPersister();
      init();
   }

   /*
    * (non-Javadoc)
    * @see
    * org.exoplatform.services.jcr.impl.core.lock.LockPersister#add(org.exoplatform.services.jcr.
    * impl.core.lock.LockData)
    */
   public void add(LockData lock) throws LockException
   {
      log.debug("add event fire");
      File lockFile = new File(rootDir, lock.getNodeIdentifier());

      if (lockFile.exists())
      {
         throw new LockException("Persistent lock information already exists");
      }

      try
      {
         lockFile.createNewFile();
      }
      catch (IOException e)
      {
         throw new LockException(e);
      }
   }

   /*
    * (non-Javadoc)
    * @see
    * org.exoplatform.services.jcr.impl.core.lock.LockPersister#remove(org.exoplatform.services.jcr
    * .impl.core.lock.LockData)
    */
   public void remove(LockData lock) throws LockException
   {
      log.debug("remove event fire");
      File lockFile = new File(rootDir, lock.getNodeIdentifier());
      if (!lockFile.exists())
      {
         // throw new LockException("Persistent lock information not exists");
         log.warn("Persistent lock information  for node " + lock.getNodeIdentifier() + " doesn't exists");
         return;
      }
      if (!lockFile.delete())
         throw new LockException("Fail to remove lock information");

   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.impl.core.lock.LockPersister#removeAll()
    */
   public void removeAll() throws LockException
   {
      if (log.isDebugEnabled())
      {
         log.debug("Removing all locks");
      }

      TransactionChangesLog transactionChangesLog = new TransactionChangesLog();

      String[] list = rootDir.list();

      try
      {
         for (int i = 0; i < list.length; i++)
         {
            PlainChangesLog plainChangesLog =
               new PlainChangesLogImpl(new ArrayList<ItemState>(), SystemIdentity.SYSTEM, ExtendedEvent.UNLOCK);

            NodeData lockedNodeData = (NodeData)dataManager.getItemData(list[i]);
            // No item no problem
            if (lockedNodeData != null)
            {
               PropertyData dataLockIsDeep =
                  (PropertyData)dataManager.getItemData(lockedNodeData, new QPathEntry(Constants.JCR_LOCKISDEEP, 0));

               if (dataLockIsDeep != null)
               {
                  plainChangesLog.add(ItemState.createDeletedState(new TransientPropertyData(QPath.makeChildPath(
                     lockedNodeData.getQPath(), Constants.JCR_LOCKISDEEP), dataLockIsDeep.getIdentifier(), 0,
                     dataLockIsDeep.getType(), dataLockIsDeep.getParentIdentifier(), dataLockIsDeep.isMultiValued())));
               }

               PropertyData dataLockOwner =
                  (PropertyData)dataManager.getItemData(lockedNodeData, new QPathEntry(Constants.JCR_LOCKOWNER, 0));
               if (dataLockOwner != null)
                  plainChangesLog.add(ItemState.createDeletedState(new TransientPropertyData(QPath.makeChildPath(
                     lockedNodeData.getQPath(), Constants.JCR_LOCKOWNER), dataLockOwner.getIdentifier(), 0,
                     dataLockOwner.getType(), dataLockOwner.getParentIdentifier(), dataLockOwner.isMultiValued())));

               if (plainChangesLog.getSize() > 0)
               {
                  transactionChangesLog.addLog(plainChangesLog);
               }
            }
         }

         if (transactionChangesLog.getSize() > 0)
         {
            dataManager.save(transactionChangesLog);
         }

         // remove files
         for (int i = 0; i < list.length; i++)
         {
            File lockFile = new File(rootDir, list[i]);
            if (!lockFile.exists())
            {
               log.warn("Persistent lock information for node id " + list[i] + " doesn't exists");
            }
            if (!lockFile.delete())
               throw new LockException("Fail to remove lock information");
         }
      }
      catch (RepositoryException e)
      {
         log.error("Unable to remove lock files due to error " + e, e);
         throw new LockException(e);
      }
   }

   /*
    * (non-Javadoc)
    * @see org.picocontainer.Startable#start()
    */
   public void start()
   {
      if (log.isDebugEnabled())
      {
         log.debug("Starting FileSystemLockPersister");
      }
      try
      {
         removeAll();
      }
      catch (LockException e)
      {
         log.error(e.getLocalizedMessage(), e);
      }

   }

   /*
    * (non-Javadoc)
    * @see org.picocontainer.Startable#stop()
    */
   public void stop()
   {
      if (log.isDebugEnabled())
      {
         log.debug("Stoping FileSystemLockPersister");
      }
      try
      {
         removeAll();
      }
      catch (LockException e)
      {
         log.error(e.getLocalizedMessage(), e);
      }
      if (log.isDebugEnabled())
      {
         log.debug("FileSystemLockPersister stoped");
      }
   }

   /**
    * Initialize directory tree for storing lock information.
    * 
    * @throws RepositoryConfigurationException
    * @throws RepositoryException
    */
   private void init() throws RepositoryConfigurationException, RepositoryException
   {
      String root = config.getParameterValue(PARAM_ROOT_DIR);

      if (root == null)
         throw new RepositoryConfigurationException("Repository service configuration." + " Source name ("
            + PARAM_ROOT_DIR + ") is expected");
      rootDir = new File(root);
      if (rootDir.exists())
      {
         if (!rootDir.isDirectory())
         {
            throw new RepositoryConfigurationException("'" + root + "' is not a directory");
         }
      }
      else
      {
         if (!rootDir.mkdirs())
            throw new RepositoryException("Can't create dir" + root);
      }
   }
}
