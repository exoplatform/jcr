/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.core.lock.cacheable;

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.dataflow.ChangesLogIterator;
import org.exoplatform.services.jcr.dataflow.CompositeChangesLog;
import org.exoplatform.services.jcr.dataflow.DataManager;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.dataflow.persistent.ItemsPersistenceListener;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.backup.BackupException;
import org.exoplatform.services.jcr.impl.backup.Backupable;
import org.exoplatform.services.jcr.impl.backup.DataRestore;
import org.exoplatform.services.jcr.impl.backup.DummyDataRestore;
import org.exoplatform.services.jcr.impl.backup.rdbms.DBBackup;
import org.exoplatform.services.jcr.impl.core.SessionDataManager;
import org.exoplatform.services.jcr.impl.core.lock.LockRemover;
import org.exoplatform.services.jcr.impl.core.lock.LockRemoverHolder;
import org.exoplatform.services.jcr.impl.core.lock.SessionLockManager;
import org.exoplatform.services.jcr.impl.dataflow.TransientItemData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspacePersistentDataManager;
import org.exoplatform.services.jcr.impl.storage.JCRInvalidItemStateException;
import org.exoplatform.services.jcr.observation.ExtendedEvent;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.transaction.ActionNonTxAware;
import org.picocontainer.Startable;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.transaction.TransactionManager;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: AbstractCacheableLockManagerImpl.java 2806 2010-07-21 08:00:15Z tolusha $
 */
public abstract class AbstractCacheableLockManager implements CacheableLockManager, ItemsPersistenceListener,
   Startable, Backupable
{
   /**
    *  The name to property time out.  
    */
   public static final String TIME_OUT = "time-out";

   /**
    * Default lock time out. 30min
    */
   public static final long DEFAULT_LOCK_TIMEOUT = 1000 * 60 * 30;

   /**
    * Data manager.
    */
   protected final DataManager dataManager;

   /**
    * Run time lock time out.
    */
   protected long lockTimeOut;

   /**
    * Lock remover thread.
    */
   protected LockRemover lockRemover;

   /**
    * Workspace configuration;
    */
   protected final WorkspaceEntry config;

   /**
    * SessionLockManagers that uses this LockManager.
    */
   protected Map<String, CacheableSessionLockManager> sessionLockManagers;

   /**
    * The current Transaction Manager
    */
   protected TransactionManager tm;

   /**
    * Logger
    */
   protected Log LOG = ExoLogger.getLogger("exo.jcr.component.core.AbstractCacheableLockManager");

   protected LockActionNonTxAware<Integer, Object> getNumLocks;

   protected LockActionNonTxAware<Boolean, Object> hasLocks;

   protected LockActionNonTxAware<Boolean, String> isLockLive;

   protected LockActionNonTxAware<Object, LockData> refresh;

   protected LockActionNonTxAware<Boolean, String> lockExist;

   protected LockActionNonTxAware<LockData, String> getLockDataById;

   protected LockActionNonTxAware<List<LockData>, Object> getLockList;

   /**
    * Constructor.
    * 
    * @param dataManager - workspace persistent data manager
    * @param config - workspace entry
    * @param transactionManager 
    *          the transaction manager
    * @throws RepositoryConfigurationException 
    */
   public AbstractCacheableLockManager(WorkspacePersistentDataManager dataManager, WorkspaceEntry config,
      TransactionManager transactionManager, LockRemoverHolder lockRemoverHolder)
      throws RepositoryConfigurationException
   {
      if (config.getLockManager() != null)
      {
         if (config.getLockManager().getParameters() != null
            && config.getLockManager().getParameterValue(TIME_OUT, null) != null)
         {
            long timeOut = config.getLockManager().getParameterTime(TIME_OUT);
            lockTimeOut = timeOut > 0 ? timeOut : DEFAULT_LOCK_TIMEOUT;
         }
         else
         {
            lockTimeOut =
               config.getLockManager().getTimeout() > 0 ? config.getLockManager().getTimeout() : DEFAULT_LOCK_TIMEOUT;
         }
      }
      else
      {
         lockTimeOut = DEFAULT_LOCK_TIMEOUT;
      }

      this.config = config;
      this.dataManager = dataManager;
      this.sessionLockManagers = new ConcurrentHashMap<String, CacheableSessionLockManager>();
      this.tm = transactionManager;
      this.lockRemover = lockRemoverHolder.getLockRemover(this);
      dataManager.addItemPersistenceListener(this);
   }

   /**
    * Returns the number of active locks.
    */
   @Managed
   @ManagedDescription("The number of active locks")
   public int getNumLocks()
   {
      try
      {
         return getNumLocks.run();
      }
      catch (LockException e)
      {
         // ignore me will never occur
      }
      return -1;
   }

   /**
    * Indicates if some locks have already been created.
    */
   protected boolean hasLocks()
   {
      try
      {
         return hasLocks.run();
      }
      catch (LockException e)
      {
         // ignore me will never occur
      }
      return true;
   }

   /**
    * Check is LockManager contains lock. No matter it is in pending or persistent state.
    */
   public boolean isLockLive(String nodeId) throws LockException
   {
      try
      {
         return isLockLive.run(nodeId);
      }
      catch (LockException e)
      {
         // ignore me will never occur
      }
      return false;
   }

   /**
    * Refreshed lock data in cache
    */
   public void refreshLockData(LockData newLockData) throws LockException
   {
      refresh.run(newLockData);
   }

   /**
    * Check is LockManager contains lock. 
    */
   public boolean lockExist(String nodeId)
   {
      try
      {
         return lockExist.run(nodeId);
      }
      catch (LockException e)
      {
         // ignore me will never occur
      }
      return false;
   }

   /**
    * Returns lock data by node identifier.
    */
   protected LockData getLockDataById(String nodeId)
   {
      try
      {
         return getLockDataById.run(nodeId);
      }
      catch (LockException e)
      {
         // ignore me will never occur
      }
      return null;
   }

   /**
    * Returns all locks.
    */
   protected synchronized List<LockData> getLockList()
   {
      try
      {
         return getLockList.run();
      }
      catch (LockException e)
      {
         // ignore me will never occur
      }
      return null;
   }

   @Managed
   @ManagedDescription("Remove the expired locks")
   public void cleanExpiredLocks()
   {
      removeExpired();
   }

   public long getDefaultLockTimeOut()
   {
      return lockTimeOut;
   }

   /**
    * Return new instance of session lock manager.
    */
   public SessionLockManager getSessionLockManager(String sessionId, SessionDataManager transientManager)
   {
      CacheableSessionLockManager sessionManager = new CacheableSessionLockManager(sessionId, this, transientManager);
      sessionLockManagers.put(sessionId, sessionManager);
      return sessionManager;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isTXAware()
   {
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public void onSaveItems(ItemStateChangesLog changesLog)
   {
      List<PlainChangesLog> chengesLogList = new ArrayList<PlainChangesLog>();
      if (changesLog instanceof TransactionChangesLog)
      {
         ChangesLogIterator logIterator = ((TransactionChangesLog)changesLog).getLogIterator();

         while (logIterator.hasNextLog())
         {
            chengesLogList.add(logIterator.nextLog());
         }
      }
      else if (changesLog instanceof PlainChangesLog)
      {
         chengesLogList.add((PlainChangesLog)changesLog);
      }
      else if (changesLog instanceof CompositeChangesLog)
      {
         for (ChangesLogIterator iter = ((CompositeChangesLog)changesLog).getLogIterator(); iter.hasNextLog();)
         {
            chengesLogList.add(iter.nextLog());
         }
      }

      List<LockOperationContainer> containers = new ArrayList<LockOperationContainer>();

      for (PlainChangesLog currChangesLog : chengesLogList)
      {
         String sessionId = currChangesLog.getSessionId();

         String nodeIdentifier;
         try
         {
            switch (currChangesLog.getEventType())
            {
               case ExtendedEvent.LOCK :
                  if (currChangesLog.getSize() < 2)
                  {
                     LOG.error("Incorrect changes log  of type ExtendedEvent.LOCK size=" + currChangesLog.getSize()
                        + "<2 \n" + currChangesLog.dump());
                     break;
                  }
                  nodeIdentifier = currChangesLog.getAllStates().get(0).getData().getParentIdentifier();

                  CacheableSessionLockManager session = sessionLockManagers.get(sessionId);
                  if (session != null && session.containsPendingLock(nodeIdentifier))
                  {
                     containers.add(new LockOperationContainer(nodeIdentifier, currChangesLog.getSessionId(),
                        ExtendedEvent.LOCK));
                  }
                  else
                  {
                     LOG.error("Lock must exist in pending locks.");
                  }
                  break;
               case ExtendedEvent.UNLOCK :
                  if (currChangesLog.getSize() < 2)
                  {
                     LOG.error("Incorrect changes log  of type ExtendedEvent.UNLOCK size=" + currChangesLog.getSize()
                        + "<2 \n" + currChangesLog.dump());
                     break;
                  }

                  containers.add(new LockOperationContainer(currChangesLog.getAllStates().get(0).getData()
                     .getParentIdentifier(), currChangesLog.getSessionId(), ExtendedEvent.UNLOCK));
                  break;
               default :
                  HashSet<String> removedLock = new HashSet<String>();
                  for (ItemState itemState : currChangesLog.getAllStates())
                  {
                     // this is a node and node is locked
                     if (itemState.getData().isNode() && lockExist(itemState.getData().getIdentifier()))
                     {
                        nodeIdentifier = itemState.getData().getIdentifier();
                        if (itemState.isDeleted())
                        {
                           removedLock.add(nodeIdentifier);
                        }
                        else if (itemState.isAdded() || itemState.isRenamed() || itemState.isUpdated())
                        {
                           removedLock.remove(nodeIdentifier);
                        }
                     }
                  }
                  for (String identifier : removedLock)
                  {
                     containers.add(new LockOperationContainer(identifier, currChangesLog.getSessionId(),
                        ExtendedEvent.UNLOCK));
                  }
                  break;
            }
         }
         catch (IllegalStateException e)
         {
            LOG.error(e.getLocalizedMessage(), e);
         }
      }

      // sort locking and unlocking operations to avoid deadlocks
      Collections.sort(containers);
      for (LockOperationContainer container : containers)
      {
         try
         {
            container.apply();
         }
         catch (LockException e)
         {
            LOG.error(e.getMessage(), e);
         }
      }
   }

   /**
    * Class containing operation type (LOCK or UNLOCK) and all the needed information like node uuid and session id.
    */
   private class LockOperationContainer implements Comparable<LockOperationContainer>
   {

      private String identifier;

      private String sessionId;

      private int type;

      /**
       * @param identifier node identifier 
       * @param sessionId id of session
       * @param type ExtendedEvent type specifying the operation (LOCK or UNLOCK)
       */
      public LockOperationContainer(String identifier, String sessionId, int type)
      {
         super();
         this.identifier = identifier;
         this.sessionId = sessionId;
         this.type = type;
      }

      /**
       * @return node identifier
       */
      public String getIdentifier()
      {
         return identifier;
      }

      public void apply() throws LockException
      {
         // invoke internalLock in LOCK operation
         if (type == ExtendedEvent.LOCK)
         {
            internalLock(sessionId, identifier);
         }
         // invoke internalUnLock in UNLOCK operation
         else if (type == ExtendedEvent.UNLOCK)
         {
            internalUnLock(sessionId, identifier);
         }
      }

      /**
       * @see java.lang.Comparable#compareTo(java.lang.Object)
       */
      public int compareTo(LockOperationContainer o)
      {
         return identifier.compareTo(o.getIdentifier());
      }
   }

   /**
    * Remove expired locks. Used from LockRemover.
    */
   public synchronized void removeExpired()
   {
      final List<String> removeLockList = new ArrayList<String>();

      for (LockData lock : getLockList())
      {
         if (!lock.isSessionScoped() && lock.getTimeToDeath() < 0)
         {
            removeLockList.add(lock.getNodeIdentifier());
         }
      }

      Collections.sort(removeLockList);

      for (String rLock : removeLockList)
      {
         removeLock(rLock);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void start()
   {
      lockRemover.start();
   }

   /**
    * {@inheritDoc}
    */
   public void stop()
   {
      lockRemover.stop();
      sessionLockManagers.clear();
   }

   /**
    * Copy <code>PropertyData prop<code> to new TransientItemData
    * 
    * @param prop
    * @return
    * @throws RepositoryException
    */
   protected TransientItemData copyItemData(PropertyData prop) throws RepositoryException
   {
      if (prop == null)
      {
         return null;
      }

      // make a copy, value may be null for deleting items
      TransientPropertyData newData =
         new TransientPropertyData(prop.getQPath(), prop.getIdentifier(), prop.getPersistedVersion(), prop.getType(),
            prop.getParentIdentifier(), prop.isMultiValued(), prop.getValues());

      return newData;
   }

   /**
    * Internal lock
    * 
    * @param nodeIdentifier
    * @throws LockException
    */
   protected abstract void internalLock(String sessionId, String nodeIdentifier) throws LockException;

   /**
    * Internal unlock.
    * 
    * @param sessionId
    * @param nodeIdentifier
    * @throws LockException
    */
   protected abstract void internalUnLock(String sessionId, String nodeIdentifier) throws LockException;


   /**
    * {@inheritDoc}
    */
   public String getLockTokenHash(String token)
   {
      String hash = "";
      try
      {
         MessageDigest m = MessageDigest.getInstance("MD5");
         m.update(token.getBytes(), 0, token.length());
         hash = new BigInteger(1, m.digest()).toString(16);
      }
      catch (NoSuchAlgorithmException e)
      {
         LOG.error("Can't get instanse of MD5 MessageDigest!", e);
      }
      return hash;
   }

   /**
    * {@inheritDoc}
    */
   public LockData getExactNodeOrCloseParentLock(NodeData node) throws RepositoryException
   {
      return getExactNodeOrCloseParentLock(node, true);
   }

   private LockData getExactNodeOrCloseParentLock(NodeData node, boolean checkHasLocks) throws RepositoryException
   {

      if (node == null || (checkHasLocks && !hasLocks()))
      {
         return null;
      }
      LockData retval = null;
      retval = getLockDataById(node.getIdentifier());
      if (retval == null)
      {
         NodeData parentData = (NodeData)dataManager.getItemData(node.getParentIdentifier());
         if (parentData != null)
         {
            retval = getExactNodeOrCloseParentLock(parentData, false);
         }
      }
      return retval;
   }

   /**
    * {@inheritDoc}
    */
   public LockData getExactNodeLock(NodeData node) throws RepositoryException
   {
      if (node == null || !hasLocks())
      {
         return null;
      }

      return getLockDataById(node.getIdentifier());
   }

   /**
    * {@inheritDoc}
    */
   public LockData getClosedChild(NodeData node) throws RepositoryException
   {
      return getClosedChild(node, true);
   }

   private LockData getClosedChild(NodeData node, boolean checkHasLocks) throws RepositoryException
   {

      if (node == null || (checkHasLocks && !hasLocks()))
      {
         return null;
      }
      LockData retval = null;

      List<NodeData> childData = dataManager.getChildNodesData(node);
      for (NodeData nodeData : childData)
      {
         retval = getLockDataById(nodeData.getIdentifier());
         if (retval != null)
         {
            return retval;
         }
      }
      // child not found try to find dipper
      for (NodeData nodeData : childData)
      {
         retval = getClosedChild(nodeData, false);
         if (retval != null)
         {
            return retval;
         }
      }
      return retval;
   }



   /**
    * Remove lock, used by Lock remover.
    * 
    * @param nodeIdentifier String
    */
   protected void removeLock(String nodeIdentifier)
   {
      try
      {
         NodeData nData = (NodeData)dataManager.getItemData(nodeIdentifier);

         //TODO EXOJCR-412, should be refactored in future.
         //Skip removing, because that node was removed in other node of cluster.  
         if (nData == null)
         {
            return;
         }

         PlainChangesLog changesLog =
            new PlainChangesLogImpl(new ArrayList<ItemState>(), IdentityConstants.SYSTEM, ExtendedEvent.UNLOCK);

         ItemData lockOwner =
            copyItemData((PropertyData)dataManager.getItemData(nData, new QPathEntry(Constants.JCR_LOCKOWNER, 1),
               ItemType.PROPERTY));

         //TODO EXOJCR-412, should be refactored in future.
         //Skip removing, because that lock was removed in other node of cluster.  
         if (lockOwner == null)
         {
            return;
         }

         changesLog.add(ItemState.createDeletedState(lockOwner));

         ItemData lockIsDeep =
            copyItemData((PropertyData)dataManager.getItemData(nData, new QPathEntry(Constants.JCR_LOCKISDEEP, 1),
               ItemType.PROPERTY));

         //TODO EXOJCR-412, should be refactored in future.
         //Skip removing, because that lock was removed in other node of cluster.  
         if (lockIsDeep == null)
         {
            return;
         }

         changesLog.add(ItemState.createDeletedState(lockIsDeep));

         // lock probably removed by other thread
         if (lockOwner == null && lockIsDeep == null)
         {
            return;
         }

         dataManager.save(new TransactionChangesLog(changesLog));
      }
      catch (JCRInvalidItemStateException e)
      {
         //TODO EXOJCR-412, should be refactored in future.
         //Skip property not found in DB, because that lock property was removed in other node of cluster.
         if (LOG.isDebugEnabled())
         {
            LOG.debug("The propperty was removed in other node of cluster.", e);
         }

      }
      catch (RepositoryException e)
      {
         LOG.error("Error occur during removing lock" + e.getLocalizedMessage(), e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void closeSessionLockManager(String sessionID)
   {
      sessionLockManagers.remove(sessionID);
   }

   /**
    * Actions that are not supposed to be called within a transaction
    * 
    * Created by The eXo Platform SAS
    * Author : Nicolas Filotto 
    *          nicolas.filotto@exoplatform.com
    * 21 janv. 2010
    */
   protected abstract class LockActionNonTxAware<R, A> extends ActionNonTxAware<R, A, LockException>
   {
      /**
       * @see org.exoplatform.services.transaction.ActionNonTxAware#getTransactionManager()
       */
      protected TransactionManager getTransactionManager()
      {
         return tm;
      }
   }

   /**
    * {@inheritDoc}
    */
   public void clean() throws BackupException
   {
      cleanCacheDirectly();
   }

   /**
    * {@inheritDoc}
    */
   public void backup(File storageDir) throws BackupException
   {
      ObjectOutputStream out = null;
      try
      {
         File contentFile = new File(storageDir, "CacheLocks" + DBBackup.CONTENT_FILE_SUFFIX);
         out = new ObjectOutputStream(new BufferedOutputStream(PrivilegedFileHelper.fileOutputStream(contentFile)));
         
         List<LockData> locks = getLockList();

         out.writeInt(locks.size());
         for (LockData lockData : locks)
         {
            lockData.writeExternal(out);
         }
      }
      catch (FileNotFoundException e)
      {
         throw new BackupException(e);
      }
      catch (IOException e)
      {
         throw new BackupException(e);
      }
      finally
      {
         if (out != null)
         {
            try
            {
               out.flush();
               out.close();
            }
            catch (IOException e)
            {
               LOG.error("Can't close output stream", e);
            }
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public DataRestore getDataRestorer(File storageDir) throws BackupException
   {
      List<LockData> locks = new ArrayList<LockData>();

      ObjectInputStream in = null;
      try
      {
         File contentFile = new File(storageDir, "CacheLocks" + DBBackup.CONTENT_FILE_SUFFIX);

         // it is possible that backup was created on configuration without Backupable WorkspaceLockManager class
         if (!PrivilegedFileHelper.exists(contentFile))
         {
            return new DummyDataRestore();
         }

         in = new ObjectInputStream(PrivilegedFileHelper.fileInputStream(contentFile));

         int count = in.readInt();
         for (int i = 0; i < count; i++)
         {
            LockData lockData = new LockData();
            lockData.readExternal(in);

            locks.add(lockData);
         }
      }
      catch (FileNotFoundException e)
      {
         throw new BackupException(e);
      }
      catch (IOException e)
      {
         throw new BackupException(e);
      }
      catch (ClassNotFoundException e)
      {
         throw new BackupException(e);
      }
      finally
      {
         if (in != null)
         {
            try
            {
               in.close();
            }
            catch (IOException e)
            {
               LOG.error("Can't close output stream", e);
            }
         }
      }

      return new CacheLocksRestore(locks);
   }

   /**
    * Cache restorer.
    */
   protected class CacheLocksRestore implements DataRestore
   {

      final private List<LockData> backupLocks = new ArrayList<LockData>();

      private List<LockData> actualLocks = new ArrayList<LockData>();

      CacheLocksRestore(final List<LockData> backupLocks)
      {
         this.backupLocks.addAll(backupLocks);
      }

      /**
       * {@inheritDoc}
       */
      public void clean() throws BackupException
      {
         actualLocks.addAll(getLockList());
         cleanCacheDirectly();
      }

      /**
       * {@inheritDoc}
       */
      public void restore() throws BackupException
      {
         for (LockData lockData : backupLocks)
         {
            putDirectly(lockData);
         }
      }

      /**
       * {@inheritDoc}
       */
      public void commit() throws BackupException
      {
      }

      /**
       * {@inheritDoc}
       */
      public void rollback() throws BackupException
      {
         cleanCacheDirectly();
         for (LockData lockData : actualLocks)
         {
            putDirectly(lockData);
         }
      }

      /**
       * {@inheritDoc}
       */
      public void close() throws BackupException
      {
         backupLocks.clear();
         actualLocks.clear();
      }
   }

   /**
    * Puts lock data directly into cache.
    * 
    * @param lockData
    *          the lock data to put
    */
   protected abstract void putDirectly(LockData lockData);

   /**
    * Clean cache directly.
    */
   protected abstract void cleanCacheDirectly();

}
