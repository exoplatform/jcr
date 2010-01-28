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
package org.exoplatform.services.jcr.impl.core.lock.jbosscache;

import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.services.jcr.access.SystemIdentity;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.SimpleParameterEntry;
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
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.lock.AbstractLockManager;
import org.exoplatform.services.jcr.impl.core.lock.LockRemover;
import org.exoplatform.services.jcr.impl.core.lock.SessionLockManager;
import org.exoplatform.services.jcr.impl.dataflow.TransientItemData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspacePersistentDataManager;
import org.exoplatform.services.jcr.impl.storage.JCRInvalidItemStateException;
import org.exoplatform.services.jcr.jbosscache.ExoJBossCacheFactory;
import org.exoplatform.services.jcr.observation.ExtendedEvent;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.naming.InitialContextInitializer;
import org.exoplatform.services.transaction.TransactionService;
import org.jboss.cache.Cache;
import org.jboss.cache.Fqn;
import org.jboss.cache.Node;
import org.jboss.cache.loader.CacheLoader;
import org.picocontainer.Startable;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: CacheableLockManager.java 111 2008-11-11 11:11:11Z serg $
 */
@Managed
@NameTemplate(@Property(key = "service", value = "lockmanager"))
public class CacheableLockManager extends AbstractLockManager implements ItemsPersistenceListener, Startable
{
   /**
    *  The name to property time out. 
    */
   public static final String TIME_OUT = "time-out";

   /**
    *  The name to property cache configuration. 
    */
   public static final String JBOSSCACCHE_CONFIG = "jbosscache-configuration";

   /**
    * Default lock time out. 30min
    */
   public static final long DEFAULT_LOCK_TIMEOUT = 1000 * 60 * 30;

   // Search constants
   /**
    * The exact lock token.
    */
   protected static final int SEARCH_EXECMATCH = 1;

   /**
    * Lock token of closed parent
    */
   protected static final int SEARCH_CLOSEDPARENT = 2;

   /**
    * Lock token of closed child
    */
   protected static final int SEARCH_CLOSEDCHILD = 4;

   /**
    * Name of lock root in jboss-cache.
    */
   public static final String LOCKS = "$LOCKS";

   /**
    * Attribute name where LockData will be stored.
    */
   public static final String LOCK_DATA = "$LOCK_DATA";

   /**
    * Logger
    */
   private final Log log = ExoLogger.getLogger("jcr.lock.CacheableLockManager");

   /**
    * Data manager.
    */
   private final DataManager dataManager;

   /**
    * Map NodeIdentifier -- lockData
    */
   private final Map<String, LockData> pendingLocks;

   /**
    * Context recall is a workaround of JDBCCacheLoader starting. 
    */
   private final InitialContextInitializer context;

   /**
    * Run time lock time out.
    */
   private long lockTimeOut;

   /**
    * Lock remover thread.
    */
   private LockRemover lockRemover;

   /**
    * The current Transaction Manager
    */
   private TransactionManager tm;

   private Cache<Serializable, Object> cache;

   private final Fqn<String> lockRoot;

   private Map<String, CacheableSessionLockManager> sessionLockManagers;

   /**
    * Constructor.
    * 
    * @param dataManager - workspace persistent data manager
    * @param config - workspace entry
    * @param context InitialContextInitializer, needed to reload context after JBoss cache creation
    * @param transactionService 
    *          the transaction service
    * @throws RepositoryConfigurationException
    */
   public CacheableLockManager(WorkspacePersistentDataManager dataManager, WorkspaceEntry config,
      InitialContextInitializer context, TransactionService transactionService, ConfigurationManager cfm)
      throws RepositoryConfigurationException
   {
      this(dataManager, config, context, transactionService.getTransactionManager(), cfm);
   }

   /**
    * Constructor.
    * 
    * @param dataManager - workspace persistent data manager
    * @param config - workspace entry
    * @param context InitialContextInitializer, needed to reload context after JBoss cache creation
    * @throws RepositoryConfigurationException
    */
   public CacheableLockManager(WorkspacePersistentDataManager dataManager, WorkspaceEntry config,
      InitialContextInitializer context, ConfigurationManager cfm) throws RepositoryConfigurationException
   {
      this(dataManager, config, context, (TransactionManager)null, cfm);
   }

   /**
    * Constructor.
    * 
    * @param dataManager - workspace persistent data manager
    * @param config - workspace entry
    * @param context InitialContextInitializer, needed to reload context after JBoss cache creation
    * @param transactionManager 
    *          the transaction manager
    * @throws RepositoryConfigurationException
    */
   public CacheableLockManager(WorkspacePersistentDataManager dataManager, WorkspaceEntry config,
      InitialContextInitializer context, TransactionManager transactionManager, ConfigurationManager cfm)
      throws RepositoryConfigurationException
   {
      lockRoot = Fqn.fromElements(LOCKS);

      List<SimpleParameterEntry> paramenerts = config.getLockManager().getParameters();

      this.dataManager = dataManager;
      if (config.getLockManager() != null)
      {
         if (paramenerts != null && config.getLockManager().getParameterValue(TIME_OUT, null) != null)
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

      pendingLocks = new HashMap<String, LockData>();
      sessionLockManagers = new HashMap<String, CacheableSessionLockManager>();
      this.context = context;

      dataManager.addItemPersistenceListener(this);

      // make cache
      if (config.getLockManager() != null)
      {
         this.tm = transactionManager;

         // create cache using custom factory
         ExoJBossCacheFactory<Serializable, Object> factory =
            new ExoJBossCacheFactory<Serializable, Object>(cfm, transactionManager);

         cache = factory.createCache(config.getLockManager());
         
         cache.create();
         cache.start();

         createStructuredNode(lockRoot);

         // Context recall is a workaround of JDBCCacheLoader starting. 
         context.recall();
      }
      else
      {
         throw new RepositoryConfigurationException("Cache configuration not found");
      }
   }

   /*
    * (non-Javadoc)
    * @see
    * org.exoplatform.services.jcr.impl.core.lock.LockManager#addPendingLock(org.exoplatform.services
    * .jcr.impl.core.NodeImpl, boolean, boolean, long)
    */
   public synchronized void addPendingLock(String nodeIdentifier, LockData lData)
   {
      pendingLocks.put(nodeIdentifier, lData);
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

   /*
    * (non-Javadoc)
    * @see
    * org.exoplatform.services.jcr.impl.core.lock.LockManager#getLock(org.exoplatform.services.jcr
    * .impl.core.NodeImpl)
    */
   public LockData getLockData(NodeImpl node) throws LockException, RepositoryException
   {

      LockData lData = getLockData((NodeData)node.getData(), SEARCH_EXECMATCH | SEARCH_CLOSEDPARENT);

      if (lData == null || (!node.getInternalIdentifier().equals(lData.getNodeIdentifier()) && !lData.isDeep()))
      {
         throw new LockException("Node not locked: " + node.getData().getQPath());
      }
      return lData;
   }

   private final LockActionNonTxAware<Integer, Object> getNumLocks = new LockActionNonTxAware<Integer, Object>()
   {
      public Integer execute(Object arg)
      {
         return cache.getChildrenNames(lockRoot).size();
      }
   };

   @Managed
   @ManagedDescription("The number of active locks")
   public int getNumLocks()
   {
      try
      {
         return executeLockActionNonTxAware(getNumLocks, null);
      }
      catch (LockException e)
      {
         // ignore me will never occur
      }
      return -1;
   }

   /**
    * Return new instance of session lock manager.
    */
   public SessionLockManager getSessionLockManager(String sessionId)
   {
      CacheableSessionLockManager sessionManager = new CacheableSessionLockManager(sessionId, this);
      sessionLockManagers.put(sessionId, sessionManager);
      return sessionManager;
   }

   private final LockActionNonTxAware<Boolean, String> isLockLive = new LockActionNonTxAware<Boolean, String>()
   {
      public Boolean execute(String nodeId)
      {
         if (pendingLocks.containsKey(nodeId) || cache.getRoot().hasChild(makeLockFqn(nodeId)))
         {
            return true;
         }

         return false;
      }
   };

   /**
    * Check is LockManager contains lock. No matter it is in pending or persistent state.
    * 
    * @param nodeId - locked node id
    * @return 
    */
   public boolean isLockLive(String nodeId)
   {
      try
      {
         return executeLockActionNonTxAware(isLockLive, nodeId);
      }
      catch (LockException e)
      {
         // ignore me will never occur
      }
      return false;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isTXAware()
   {
      return true;
   }

   /*
    * (non-Javadoc)
    * @seeorg.exoplatform.services.jcr.dataflow.persistent.ItemsPersistenceListener#onSaveItems(org.
    * exoplatform.services.jcr.dataflow.ItemStateChangesLog)
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
         String nodeIdentifier;
         try
         {
            switch (currChangesLog.getEventType())
            {
               case ExtendedEvent.LOCK :
                  if (currChangesLog.getSize() < 2)
                  {
                     log.error("Incorrect changes log  of type ExtendedEvent.LOCK size=" + currChangesLog.getSize()
                        + "<2 \n" + currChangesLog.dump());
                     break;
                  }
                  nodeIdentifier = currChangesLog.getAllStates().get(0).getData().getParentIdentifier();

                  if (pendingLocks.containsKey(nodeIdentifier))
                  {
                     containers.add(new LockOperationContainer(nodeIdentifier, currChangesLog.getSessionId(),
                        ExtendedEvent.LOCK));
                  }
                  else
                  {
                     log.error("Lock must exist in pending locks.");
                  }
                  break;
               case ExtendedEvent.UNLOCK :
                  if (currChangesLog.getSize() < 2)
                  {
                     log.error("Incorrect changes log  of type ExtendedEvent.UNLOCK size=" + currChangesLog.getSize()
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
            log.error(e.getLocalizedMessage(), e);
         }
      }

      // sort locking and unlocking operations to avoid deadlocks in JBossCache
      Collections.sort(containers);
      for (LockOperationContainer container : containers)
      {
         try
         {
            container.apply();
         }
         catch (LockException e)
         {
            log.error(e.getMessage(), e);
         }
      }
   }

   /**
    * Class containing operation type (LOCK or UNLOCK) and all the needed information like node uuid and session id 
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
            internalLock(identifier);
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

   private final LockActionNonTxAware<Object, LockData> refresh = new LockActionNonTxAware<Object, LockData>()
   {
      public Object execute(LockData newLockData) throws LockException
      {
         //first look pending locks
         if (pendingLocks.containsKey(newLockData.getNodeIdentifier()))
         {
            pendingLocks.put(newLockData.getNodeIdentifier(), newLockData);
         }
         else
         {
            Fqn<String> fqn = makeLockFqn(newLockData.getNodeIdentifier());
            if (cache.getRoot().hasChild(fqn))
            {
               cache.getRoot().addChild(fqn);
            }
            else
            {
               throw new LockException("Can't refresh lock for node " + newLockData.getNodeIdentifier()
                  + " since lock is not exist");
            }
         }
         return null;
      }
   };

   /**
    * Refreshed lock data in cache
    * 
    * @param newLockData
    */
   public void refresh(LockData newLockData) throws LockException
   {
      executeLockActionNonTxAware(refresh, newLockData);
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

   public void removePendingLock(String nodeId)
   {
      pendingLocks.remove(nodeId);
   }

   /*
    * (non-Javadoc)
    * @see org.picocontainer.Startable#start()
    */
   public void start()
   {
      lockRemover = new LockRemover(this);
   }

   /*
    * (non-Javadoc)
    * @see org.picocontainer.Startable#stop()
    */
   public void stop()
   {
      lockRemover.halt();
      lockRemover.interrupt();
      pendingLocks.clear();
      sessionLockManagers.clear();
      cache.stop();
   }

   /**
    * Copy <code>PropertyData prop<code> to new TransientItemData
    * 
    * @param prop
    * @return
    * @throws RepositoryException
    */
   private TransientItemData copyItemData(PropertyData prop) throws RepositoryException
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
   private synchronized void internalLock(String nodeIdentifier) throws LockException
   {
      LockData lockData = pendingLocks.get(nodeIdentifier);
      if (lockData != null)
      {
         Fqn<String> lockPath = makeLockFqn(lockData.getNodeIdentifier());

         // addChild will add if absent or return old if present
         Node<Serializable, Object> node = cache.getRoot().addChild(lockPath);

         // this will return null if success. And old data if something exists...
         LockData oldLockData = (LockData)node.putIfAbsent(LOCK_DATA, lockData);

         if (oldLockData != null)
         {
            throw new LockException("Unable to write LockData. Node [" + lockData.getNodeIdentifier()
               + "] already has LockData!");
         }
         pendingLocks.remove(nodeIdentifier);
      }
      else
      {
         throw new LockException("No lock in pending locks");
      }
   }

   /**
    * Internal unlock.
    * 
    * @param sessionId
    * @param nodeIdentifier
    * @throws LockException
    */
   private synchronized void internalUnLock(String sessionId, String nodeIdentifier) throws LockException
   {
      LockData lData = getLockDataById(nodeIdentifier);

      if (lData != null)
      {
         cache.removeNode(makeLockFqn(nodeIdentifier));

         CacheableSessionLockManager sessMgr = sessionLockManagers.get(sessionId);
         if (sessMgr != null)
         {
            sessMgr.notifyLockRemoved(nodeIdentifier);
         }
      }
   }

   private final LockActionNonTxAware<Boolean, String> lockExist = new LockActionNonTxAware<Boolean, String>()
   {
      public Boolean execute(String nodeId) throws LockException
      {
         return cache.getRoot().hasChild(makeLockFqn(nodeId));
      }
   };

   private boolean lockExist(String nodeId)
   {
      try
      {
         return executeLockActionNonTxAware(lockExist, nodeId);
      }
      catch (LockException e)
      {
         // ignore me will never occur
      }
      return false;
   }

   /**
    * Calculates md5 hash of string.
    * 
    * @param token
    * @return
    */
   protected String getHash(String token)
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
         log.error("Can't get instanse of MD5 MessageDigest!", e);
      }
      return hash;
   }

   /**
    * Search lock in maps.
    * 
    * @param data
    * @param searchType
    * @return
    */
   protected LockData getLockData(NodeData data, int searchType)
   {
      if (data == null)
         return null;
      LockData retval = null;
      try
      {
         if ((searchType & SEARCH_EXECMATCH) != 0)
         {
            retval = getLockDataById(data.getIdentifier());
         }
         if (retval == null && (searchType & SEARCH_CLOSEDPARENT) != 0)
         {

            NodeData parentData = (NodeData)dataManager.getItemData(data.getParentIdentifier());
            if (parentData != null)
            {
               retval = getLockDataById(parentData.getIdentifier());
               // parent not found try to fo upper
               if (retval == null)
               {
                  retval = getLockData(parentData, SEARCH_CLOSEDPARENT);
               }
            }
         }
         if (retval == null && (searchType & SEARCH_CLOSEDCHILD) != 0)
         {

            List<NodeData> childData = dataManager.getChildNodesData(data);
            for (NodeData nodeData : childData)
            {
               retval = getLockDataById(nodeData.getIdentifier());
               if (retval != null)
                  break;
            }
            if (retval == null)
            {
               // child not found try to find diper
               for (NodeData nodeData : childData)
               {
                  retval = getLockData(nodeData, SEARCH_CLOSEDCHILD);
                  if (retval != null)
                     break;
               }
            }
         }
      }
      catch (RepositoryException e)
      {
         return null;
      }

      return retval;
   }

   private final LockActionNonTxAware<LockData, String> getLockDataById = new LockActionNonTxAware<LockData, String>()
   {
      public LockData execute(String nodeId) throws LockException
      {
         return (LockData)cache.get(makeLockFqn(nodeId), LOCK_DATA);
      }
   };

   protected LockData getLockDataById(String nodeId)
   {
      try
      {
         return executeLockActionNonTxAware(getLockDataById, nodeId);
      }
      catch (LockException e)
      {
         // ignore me will never occur
      }
      return null;
   }

   private final LockActionNonTxAware<List<LockData>, Object> getLockList =
      new LockActionNonTxAware<List<LockData>, Object>()
      {
         public List<LockData> execute(Object arg) throws LockException
         {
            Set<Object> nodesId = cache.getChildrenNames(lockRoot);

            List<LockData> locksData = new ArrayList<LockData>();
            for (Object nodeId : nodesId)
            {
               LockData lockData = (LockData)cache.get(makeLockFqn((String)nodeId), LOCK_DATA);
               if (lockData != null)
               {
                  locksData.add(lockData);
               }
            }
            return locksData;
         }
      };

   protected synchronized List<LockData> getLockList()
   {
      try
      {
         return executeLockActionNonTxAware(getLockList, null);
      }
      catch (LockException e)
      {
         // ignore me will never occur
      }
      return null;
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
            new PlainChangesLogImpl(new ArrayList<ItemState>(), SystemIdentity.SYSTEM, ExtendedEvent.UNLOCK);

         ItemData lockOwner =
            copyItemData((PropertyData)dataManager.getItemData(nData, new QPathEntry(Constants.JCR_LOCKOWNER, 1)));

         //TODO EXOJCR-412, should be refactored in future.
         //Skip removing, because that lock was removed in other node of cluster.  
         if (lockOwner == null)
         {
            return;
         }

         changesLog.add(ItemState.createDeletedState(lockOwner));

         ItemData lockIsDeep =
            copyItemData((PropertyData)dataManager.getItemData(nData, new QPathEntry(Constants.JCR_LOCKISDEEP, 1)));

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
         if (log.isDebugEnabled())
         {
            log.debug("The propperty was removed in other node of cluster.", e);
         }

      }
      catch (RepositoryException e)
      {
         log.error("Error occur during removing lock" + e.getLocalizedMessage(), e);
      }
   }

   /**
    * Release all resources associated with CacheableSessionLockManager.
    * 
    * @param sessionID - session identifier
    */
   protected void closeSession(String sessionID)
   {
      sessionLockManagers.remove(sessionID);
   }

   /**
    * Make lock absolute Fqn, i.e. /$LOCKS/nodeID.
    *
    * @param itemId String
    * @return Fqn
    */
   private Fqn<String> makeLockFqn(String nodeId)
   {
      return Fqn.fromRelativeElements(lockRoot, nodeId);
   }

   /**
    *  Will be created structured node in cache, like /$LOCKS
    */
   private void createStructuredNode(Fqn fqn)
   {
      Node<Serializable, Object> node = cache.getRoot().getChild(fqn);
      if (node == null)
      {
         cache.getInvocationContext().getOptionOverrides().setCacheModeLocal(true);
         node = cache.getRoot().addChild(fqn);
      }
      node.setResident(true);
   }

   /**
    * Execute the given action outside a transaction. This is needed since the {@link Cache} used by {@link CacheableLockManager}
    * manages the persistence of its locks thanks to a {@link CacheLoader} and a {@link CacheLoader} lock the JBoss cache {@link Node}
    * even for read operations which cause deadlock issue when a XA {@link Transaction} is already opened
    * @throws LockException when a exception occurs
    */
   private <R, A> R executeLockActionNonTxAware(LockActionNonTxAware<R, A> action, A arg) throws LockException
   {
      Transaction tx = null;
      try
      {
         if (tm != null)
         {
            try
            {
               tx = tm.suspend();
            }
            catch (Exception e)
            {
               log.warn("Cannot suspend the current transaction", e);
            }
         }
         return action.execute(arg);
      }
      finally
      {
         if (tx != null)
         {
            try
            {
               tm.resume(tx);
            }
            catch (Exception e)
            {
               log.warn("Cannot resume the current transaction", e);
            }
         }
      }
   }

   /**
    * Actions that are not supposed to be called within a transaction
    * 
    * Created by The eXo Platform SAS
    * Author : Nicolas Filotto 
    *          nicolas.filotto@exoplatform.com
    * 21 janv. 2010
    */
   private static interface LockActionNonTxAware<R, A>
   {
      R execute(A arg) throws LockException;
   }
}
