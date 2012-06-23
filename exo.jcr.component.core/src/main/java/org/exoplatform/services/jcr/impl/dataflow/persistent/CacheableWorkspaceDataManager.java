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
package org.exoplatform.services.jcr.impl.dataflow.persistent;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.persistent.MandatoryItemsPersistenceListener;
import org.exoplatform.services.jcr.dataflow.persistent.WorkspaceStorageCache;
import org.exoplatform.services.jcr.dataflow.persistent.WorkspaceStorageCacheListener;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.NullItemData;
import org.exoplatform.services.jcr.datamodel.NullNodeData;
import org.exoplatform.services.jcr.datamodel.NullPropertyData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.backup.ResumeException;
import org.exoplatform.services.jcr.impl.backup.SuspendException;
import org.exoplatform.services.jcr.impl.backup.Suspendable;
import org.exoplatform.services.jcr.impl.core.itemfilters.QPathEntryFilter;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.dataflow.session.TransactionableResourceManager;
import org.exoplatform.services.jcr.impl.dataflow.session.TransactionableResourceManagerListener;
import org.exoplatform.services.jcr.impl.storage.SystemDataContainerHolder;
import org.exoplatform.services.jcr.storage.WorkspaceDataContainer;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;
import org.exoplatform.services.rpc.RPCException;
import org.exoplatform.services.rpc.RPCService;
import org.exoplatform.services.rpc.RemoteCommand;
import org.exoplatform.services.rpc.TopologyChangeEvent;
import org.exoplatform.services.rpc.TopologyChangeListener;
import org.exoplatform.services.transaction.TransactionService;
import org.picocontainer.Startable;

import java.io.Serializable;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.jcr.RepositoryException;
import javax.transaction.Status;
import javax.transaction.TransactionManager;

/**
 * Created by The eXo Platform SAS. 
 * 
 * <br/>
 * Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 13.04.2006
 * 
 * @version $Id$
 */
@Managed
@NameTemplate(@Property(key = "service", value = "DataManager"))
public class CacheableWorkspaceDataManager extends WorkspacePersistentDataManager implements Suspendable,
   TopologyChangeListener, Startable, WorkspaceStorageCacheListener
{

   private final static double ACL_BF_FALSE_PROPBABILITY_DEFAULT = 0.1d;

   private final static int ACL_BF_ELEMENTS_NUMBER_DEFAULT = 1000000;

   /**
    * Items cache.
    */
   protected final WorkspaceStorageCache cache;

   /**
    * Requests cache.
    */
   protected final ConcurrentMap<Integer, DataRequest> requestCache;

   /**
    * The resource manager
    */
   private final TransactionableResourceManager txResourceManager;

   private final AtomicBoolean filtersEnabled = new AtomicBoolean();

   private final AtomicBoolean filtersSupported = new AtomicBoolean(true);

   /**
    * Bloom filter parameters.
    */
   private final double bfProbability;

   private final int bfElementNumber;

   private volatile BloomFilter<String> filterPermissions;

   private volatile BloomFilter<String> filterOwner;

   /**
    * The service for executing commands on all nodes of cluster.
    */
   protected final RPCService rpcService;

   /**
    * The amount of current working threads.
    */
   protected AtomicInteger workingThreads = new AtomicInteger();

   /**
    * Indicates if component suspended or not.
    */
   protected final AtomicBoolean isSuspended = new AtomicBoolean(false);

   /**
    * Indicates if component stopped or not.
    */
   protected final AtomicBoolean isStopped = new AtomicBoolean(false);

   /**
    * Allows to make all threads waiting until resume. 
    */
   protected final AtomicReference<CountDownLatch> latcher = new AtomicReference<CountDownLatch>();

   /**
    * Indicates that node keep responsible for resuming.
    */
   protected final AtomicBoolean isResponsibleForResuming = new AtomicBoolean(false);

   /**
    * Request to all nodes to check if there is someone who responsible for resuming.
    */
   private RemoteCommand requestForResponsibleForResuming;

   /**
    * Suspend remote command.
    */
   private RemoteCommand suspend;

   /**
    * Resume remote command.
    */
   private RemoteCommand resume;

   /**
    * ItemData request, used on get operations.
    * 
    */
   protected class DataRequest
   {
      /**
       * GET_NODES type.
       */
      static public final int GET_NODES = 1;

      /**
       * GET_PROPERTIES type.
       */
      static public final int GET_PROPERTIES = 2;

      /**
       * GET_ITEM_ID type.
       */
      static private final int GET_ITEM_ID = 3;

      /**
       * GET_ITEM_NAME type.
       */
      static private final int GET_ITEM_NAME = 4;

      /**
       * GET_LIST_PROPERTIES type.
       */
      static private final int GET_LIST_PROPERTIES = 5;

      /**
       * GET_REFERENCES type.
       */
      static public final int GET_REFERENCES = 6;

      /**
       * Request type.
       */
      protected final int type;

      /**
       * Item parentId.
       */
      protected final String parentId;

      /**
       * Item id.
       */
      protected final String id;

      /**
       * Item name.
       */
      protected final QPathEntry name;

      /**
       * Hash code.
       */
      protected final int hcode;

      /**
       * Readiness latch.
       */
      protected CountDownLatch ready = new CountDownLatch(1);

      /**
       * DataRequest constructor.
       * 
       * @param parentId
       *          parent id
       * @param type
       *          request type
       */
      DataRequest(String parentId, int type)
      {
         this.parentId = parentId;
         this.name = null;
         this.id = null;
         this.type = type;

         // hashcode
         this.hcode = 31 * (31 + this.type) + this.parentId.hashCode();
      }

      /**
       * DataRequest constructor.
       * 
       * @param parentId
       *          parent id
       * @param name
       *          Item name
       */
      DataRequest(String parentId, QPathEntry name)
      {
         this.parentId = parentId;
         this.name = name;
         this.id = null;
         this.type = GET_ITEM_NAME;

         // hashcode
         int hc = 31 * (31 + this.type) + this.parentId.hashCode();
         this.hcode = 31 * hc + this.name.hashCode();
      }

      /**
       * DataRequest constructor.
       * 
       * @param id
       *          Item id
       */
      DataRequest(String id)
      {
         this.parentId = null;
         this.name = null;
         this.id = id;
         this.type = GET_ITEM_ID;

         // hashcode
         this.hcode = 31 * (31 + this.type) + (this.id == null ? 0 : this.id.hashCode());
      }

      /**
       * Start the request, each same will wait till this will be finished
       */
      void start()
      {
         DataRequest request = requestCache.putIfAbsent(this.hashCode(), this);
         if (request != null)
         {
            request.await();
         }
      }

      /**
       * Done the request. Must be called after the data request will be finished. This call allow
       * another same requests to be performed.
       */
      void done()
      {
         this.ready.countDown();
         requestCache.remove(this.hashCode(), this);
      }

      /**
       * Await this thread for another one running same request.
       * 
       */
      void await()
      {
         try
         {
            this.ready.await();
         }
         catch (InterruptedException e)
         {
            LOG.warn("Can't wait for same request process. " + e, e);
         }
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public boolean equals(Object obj)
      {
         return this.hcode == obj.hashCode();
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public int hashCode()
      {
         return hcode;
      }
   }

   /**
   * This class is a decorator on the top of the {@link WorkspaceStorageCache} to manage the case
   * where the cache is disabled at the beginning then potentially enabled later
   */
   private class CacheItemsPersistenceListener implements MandatoryItemsPersistenceListener
   {
      /**
       * {@inheritDoc}
      */
      public boolean isTXAware()
      {
         return cache.isTXAware();
      }

      /**
       * {@inheritDoc}
       */
      public void onSaveItems(ItemStateChangesLog itemStates)
      {
         if (cache.isEnabled())
         {
            cache.onSaveItems(itemStates);
         }
      }
   }

   /**
    * CacheableWorkspaceDataManager constructor.
    * 
    * @param wsConfig 
    *          WorkspaceEntry used to fetch bloom filter parameters
    * @param dataContainer
    *          Workspace data container (persistent level)
    * @param cache
    *          Items cache
    * @param systemDataContainerHolder
    *          System Workspace data container (persistent level)
    * @param txResourceManager
    *          the resource manager used to manage the whole tx
    * @param transactionService 
    *          TransactionService  
    * @param rpcService
    *          the service for executing commands on all nodes of cluster
    */
   public CacheableWorkspaceDataManager(WorkspaceEntry wsConfig, WorkspaceDataContainer dataContainer,
      WorkspaceStorageCache cache, SystemDataContainerHolder systemDataContainerHolder,
      TransactionableResourceManager txResourceManager, TransactionService transactionService, RPCService rpcService)
   {
      super(dataContainer, systemDataContainerHolder, txResourceManager, transactionService.getTransactionManager());

      bfProbability =
         wsConfig.getContainer().getParameterDouble(WorkspaceDataContainer.ACL_BF_FALSE_PROPBABILITY,
            ACL_BF_FALSE_PROPBABILITY_DEFAULT);
      if (bfProbability < 0 || bfProbability > 1)
      {
         throw new IllegalArgumentException("Parameter " + WorkspaceDataContainer.ACL_BF_FALSE_PROPBABILITY
            + " is invalid, must be between 0 and 1.");
      }

      bfElementNumber =
         wsConfig.getContainer().getParameterInteger(WorkspaceDataContainer.ACL_BF_ELEMENTS_NUMBER,
            ACL_BF_ELEMENTS_NUMBER_DEFAULT);
      if (bfElementNumber <= 0)
      {
         throw new IllegalArgumentException("Parameter " + WorkspaceDataContainer.ACL_BF_ELEMENTS_NUMBER
            + " is invalid, can not be less then 1.");
      }

      this.cache = cache;

      this.requestCache = new ConcurrentHashMap<Integer, DataRequest>();
      addItemPersistenceListener(new CacheItemsPersistenceListener());

      this.rpcService = rpcService;
      this.txResourceManager = txResourceManager;
   }

   /**
    * CacheableWorkspaceDataManager constructor.
    * 
    * @param wsConfig 
    *          WorkspaceEntry used to fetch bloom filter parameters
    * @param dataContainer
    *          Workspace data container (persistent level)
    * @param cache
    *          Items cache
    * @param systemDataContainerHolder
    *          System Workspace data container (persistent level)
    * @param txResourceManager
    *          the resource manager used to manage the whole tx
    * @param transactionService TransactionService         
    */
   public CacheableWorkspaceDataManager(WorkspaceEntry wsConfig, WorkspaceDataContainer dataContainer,
      WorkspaceStorageCache cache, SystemDataContainerHolder systemDataContainerHolder,
      TransactionableResourceManager txResourceManager, TransactionService transactionService)
   {
      this(wsConfig, dataContainer, cache, systemDataContainerHolder, txResourceManager, transactionService, null);
   }

   /**
    * CacheableWorkspaceDataManager constructor.
    * 
    * @param wsConfig 
    *          WorkspaceEntry used to fetch bloom filter parameters
    * @param dataContainer
    *          Workspace data container (persistent level)
    * @param cache
    *          Items cache
    * @param systemDataContainerHolder
    *          System Workspace data container (persistent level)
    * @param txResourceManager
    *          the resource manager used to manage the whole tx
    */
   public CacheableWorkspaceDataManager(WorkspaceEntry wsConfig, WorkspaceDataContainer dataContainer,
      WorkspaceStorageCache cache, SystemDataContainerHolder systemDataContainerHolder,
      TransactionableResourceManager txResourceManager, RPCService rpcService)
   {
      super(dataContainer, systemDataContainerHolder, txResourceManager, getTransactionManagerFromCache(cache));

      bfProbability =
         wsConfig.getContainer().getParameterDouble(WorkspaceDataContainer.ACL_BF_FALSE_PROPBABILITY,
            ACL_BF_FALSE_PROPBABILITY_DEFAULT);
      if (bfProbability < 0 || bfProbability > 1)
      {
         throw new IllegalArgumentException("Parameter " + WorkspaceDataContainer.ACL_BF_FALSE_PROPBABILITY
            + " is invalid, must be between 0 and 1.");
      }

      bfElementNumber =
         wsConfig.getContainer().getParameterInteger(WorkspaceDataContainer.ACL_BF_ELEMENTS_NUMBER,
            ACL_BF_ELEMENTS_NUMBER_DEFAULT);
      if (bfElementNumber <= 0)
      {
         throw new IllegalArgumentException("Parameter " + WorkspaceDataContainer.ACL_BF_ELEMENTS_NUMBER
            + " is invalid, can not be less then 1.");
      }

      this.cache = cache;

      this.requestCache = new ConcurrentHashMap<Integer, DataRequest>();
      addItemPersistenceListener(new CacheItemsPersistenceListener());

      this.rpcService = rpcService;
      this.txResourceManager = txResourceManager;
   }

   /**
    * CacheableWorkspaceDataManager constructor.
    * 
    * @param wsConfig 
    *          WorkspaceEntry used to fetch bloom filter parameters
    * @param dataContainer
    *          Workspace data container (persistent level)
    * @param cache
    *          Items cache
    * @param systemDataContainerHolder
    *          System Workspace data container (persistent level)
    * @param txResourceManager
    *          the resource manager used to manage the whole tx
    */
   public CacheableWorkspaceDataManager(WorkspaceEntry wsConfig, WorkspaceDataContainer dataContainer,
      WorkspaceStorageCache cache, SystemDataContainerHolder systemDataContainerHolder,
      TransactionableResourceManager txResourceManager)
   {
      this(wsConfig, dataContainer, cache, systemDataContainerHolder, txResourceManager, (RPCService)null);
   }

   /**
    * CacheableWorkspaceDataManager constructor.
    * 
    * @param wsConfig 
    *          WorkspaceEntry used to fetch bloom filter parameters
    * @param dataContainer
    *          Workspace data container (persistent level)
    * @param cache
    *          Items cache
    * @param systemDataContainerHolder
    *          System Workspace data container (persistent level)
    */
   public CacheableWorkspaceDataManager(WorkspaceEntry wsConfig, WorkspaceDataContainer dataContainer,
      WorkspaceStorageCache cache, SystemDataContainerHolder systemDataContainerHolder)
   {
      this(wsConfig, dataContainer, cache, systemDataContainerHolder, null, (RPCService)null);
   }

   /**
    * Try to get the TransactionManager from the cache by calling by reflection
    * getTransactionManager() on the cache instance, by default it will return null
    */
   private static TransactionManager getTransactionManagerFromCache(WorkspaceStorageCache cache)
   {
      try
      {
         return (TransactionManager)cache.getClass().getMethod("getTransactionManager", (Class<?>[])null)
            .invoke(cache, (Object[])null);
      }
      catch (Exception e)
      {
         LOG.debug("Could not get the transaction manager from the cache", e);
      }
      return null;
   }
   
   /**
    * Get Items Cache.
    * 
    * @return WorkspaceStorageCache
    */
   public WorkspaceStorageCache getCache()
   {
      return cache;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int getChildNodesCount(final NodeData parent) throws RepositoryException
   {
      if (cache.isEnabled())
      {
         int childCount = cache.getChildNodesCount(parent);
         if (childCount >= 0)
         {
            return childCount;
         }
      }
      return executeAction(new PrivilegedExceptionAction<Integer>()
      {
         public Integer run() throws RepositoryException
         {
            return CacheableWorkspaceDataManager.super.getChildNodesCount(parent);
         }
      });
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<NodeData> getChildNodesData(NodeData nodeData) throws RepositoryException
   {
      return getChildNodesData(nodeData, false);
   }

   /**
    * {@inheritDoc}
    */
   public boolean getChildNodesDataByPage(final NodeData nodeData, final int fromOrderNum, final int toOrderNum,
      final List<NodeData> childs) throws RepositoryException
   {
      // if child nodes lazy iteration feature not supported by cache
      // then call old-style getChildNodes method
      if (!cache.isChildNodesByPageSupported())
      {
         childs.addAll(getChildNodesData(nodeData));
         return false;
      }
      // if child nodes by page iteration supported, then do it
      List<NodeData> childNodes = null;
      if (cache.isEnabled())
      {
         childNodes = cache.getChildNodes(nodeData);
         if (childNodes != null)
         {
            childs.addAll(childNodes);
            return false;
         }
         else
         {
            childNodes = cache.getChildNodesByPage(nodeData, fromOrderNum);
            if (childNodes != null)
            {
               childs.addAll(childNodes);
               return true;
            }
         }
      }
      final DataRequest request = new DataRequest(nodeData.getIdentifier(), DataRequest.GET_NODES);

      try
      {
         request.start();
         if (cache.isEnabled())
         {
            // Try first to get the value from the cache since a
            // request could have been launched just before
            childNodes = cache.getChildNodes(nodeData);
            if (childNodes != null)
            {
               childs.addAll(childNodes);
               return false;
            }
            else
            {
               childNodes = cache.getChildNodesByPage(nodeData, fromOrderNum);
               if (childNodes != null)
               {
                  childs.addAll(childNodes);
                  return true;
               }
            }
         }

         return executeAction(new PrivilegedExceptionAction<Boolean>()
         {
            public Boolean run() throws RepositoryException
            {
               boolean hasNext =
                  CacheableWorkspaceDataManager.super.getChildNodesDataByPage(nodeData, fromOrderNum, toOrderNum,
                     childs);

               if (cache.isEnabled())
               {
                  cache.addChildNodesByPage(nodeData, childs, fromOrderNum);
               }

               return hasNext;
            }
         });
      }
      finally
      {
         request.done();
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<NodeData> getChildNodesData(NodeData parentData, List<QPathEntryFilter> patternFilters)
      throws RepositoryException
   {
      return getChildNodesDataByPattern(parentData, patternFilters);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<PropertyData> getChildPropertiesData(NodeData nodeData) throws RepositoryException
   {
      List<PropertyData> childs = getChildPropertiesData(nodeData, false);
      for (PropertyData prop : childs)
      {
         fixPropertyValues(prop);
      }

      return childs;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<PropertyData> getChildPropertiesData(NodeData nodeData, List<QPathEntryFilter> itemDataFilters)
      throws RepositoryException
   {
      List<PropertyData> childs = getChildPropertiesDataByPattern(nodeData, itemDataFilters);
      for (PropertyData prop : childs)
      {
         fixPropertyValues(prop);
      }

      return childs;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public ItemData getItemData(NodeData parentData, QPathEntry name) throws RepositoryException
   {
      return getItemData(parentData, name, ItemType.UNKNOWN);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public ItemData getItemData(final NodeData parentData, final QPathEntry name, final ItemType itemType)
      throws RepositoryException
   {
      return getItemData(parentData, name, itemType, true);
   }

   /**
    * {@inheritDoc}
    */
   public ItemData getItemData(final NodeData parentData, final QPathEntry name, final ItemType itemType,
      final boolean createNullItemData)
      throws RepositoryException
   {
      if (cache.isEnabled())
      {
         // 1. Try from cache
         ItemData data = getCachedItemData(parentData, name, itemType);

         // 2. Try from container
         if (data == null)
         {
            final DataRequest request = new DataRequest(parentData.getIdentifier(), name);

            try
            {
               request.start();
               // Try first to get the value from the cache since a
               // request could have been launched just before
               data = getCachedItemData(parentData, name, itemType);
               if (data == null)
               {
                  data = executeAction(new PrivilegedExceptionAction<ItemData>()
                  {
                     public ItemData run() throws RepositoryException
                     {
                        return getPersistedItemData(parentData, name, itemType, createNullItemData);
                     }
                  });
               }
            }
            finally
            {
               request.done();
            }
         }

         if (data instanceof NullItemData)
         {
            return null;
         }

         if (data != null && !data.isNode())
         {
            fixPropertyValues((PropertyData)data);
         }

         return data;
      }
      else
      {
         return executeAction(new PrivilegedExceptionAction<ItemData>()
         {
            public ItemData run() throws RepositoryException
            {
               ItemData item = CacheableWorkspaceDataManager.super.getItemData(parentData, name, itemType);
               return item != null && item.isNode() ? initACL(parentData, (NodeData)item) : item;
            }
         });
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public ItemData getItemData(String identifier) throws RepositoryException
   {
      return getItemData(identifier, true);
   }

   /**
    * Do the same thing as getItemData(identifier), but ACL initialization can be specified.
    * If doInitACL is true (default value for getItemData(identifier)) ACL will be initialized.
    * 
    * @param identifier
    * @param doInitACL
    * @return
    * @throws RepositoryException
    */
   private ItemData getItemData(final String identifier, final boolean doInitACL) throws RepositoryException
   {
      if (cache.isEnabled())
      {
         // 1. Try from cache
         ItemData data = getCachedItemData(identifier);

         // 2 Try from container
         if (data == null)
         {
            final DataRequest request = new DataRequest(identifier);

            try
            {
               request.start();
               // Try first to get the value from the cache since a
               // request could have been launched just before
               data = getCachedItemData(identifier);
               if (data == null)
               {
                  data = executeAction(new PrivilegedExceptionAction<ItemData>()
                  {
                     public ItemData run() throws RepositoryException
                     {
                        return getPersistedItemData(identifier);
                     }
                  });
               }
            }
            finally
            {
               request.done();
            }
         }

         if (data instanceof NullItemData)
         {
            return null;
         }

         if (data != null && !data.isNode())
         {
            fixPropertyValues((PropertyData)data);
         }

         return data;
      }
      else
      {
         return executeAction(new PrivilegedExceptionAction<ItemData>()
         {
            public ItemData run() throws RepositoryException
            {
               ItemData item = CacheableWorkspaceDataManager.super.getItemData(identifier);
               if (item != null && item.isNode() && doInitACL)
               {
                  return initACL(null, (NodeData)item);
               }
               else
               {
                  return item;
               }
            }
         });
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<PropertyData> getReferencesData(String identifier, boolean skipVersionStorage)
      throws RepositoryException
   {
      List<PropertyData> props = getReferencedPropertiesData(identifier);

      if (skipVersionStorage)
      {
         List<PropertyData> result = new ArrayList<PropertyData>();
         for (int i = 0, length = props.size(); i < length; i++)
         {
            PropertyData prop = props.get(i);
            if (!prop.getQPath().isDescendantOf(Constants.JCR_VERSION_STORAGE_PATH))
            {
               result.add(prop);
            }
         }

         return result;
      }

      return props;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<PropertyData> listChildPropertiesData(NodeData nodeData) throws RepositoryException
   {
      return listChildPropertiesData(nodeData, false);
   }

   /**
    * {@inheritDoc}
    */
   public void save(final ItemStateChangesLog changesLog) throws RepositoryException
   {
      if (isSuspended.get())
      {
         try
         {
            latcher.get().await();
         }
         catch (InterruptedException e)
         {
            throw new RepositoryException(e);
         }
      }

      workingThreads.incrementAndGet();
      try
      {
         SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<Void>()
         {
            public Void run() throws Exception
            {
               doSave(changesLog);
               return null;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         Throwable cause = e.getCause();
         if (cause instanceof RepositoryException)
         {
            throw (RepositoryException)cause;
         }
         else if (cause instanceof RuntimeException)
         {
            throw (RuntimeException)cause;
         }
         else
         {
            throw new RuntimeException(cause);
         }
      }
      finally
      {
         workingThreads.decrementAndGet();

         if (isSuspended.get() && workingThreads.get() == 0)
         {
            synchronized (workingThreads)
            {
               workingThreads.notifyAll();
            }
         }
      }
   }

   private void doSave(final ItemStateChangesLog changesLog) throws RepositoryException
   {
      if (isStopped.get())
      {
         throw new RepositoryException("Data container is stopped");
      }
      
      ChangesLogWrapper logWrapper = new ChangesLogWrapper(changesLog);

      if (isTxAware())
      {
         if (txResourceManager != null && txResourceManager.isGlobalTxActive())
         {
            super.save(logWrapper);
            registerListener(logWrapper);
         }
         else
         {
            doBegin();
            try
            {
               super.save(logWrapper);
            }
            catch (RepositoryException e)
            {
               doRollback();
               throw e;
            }
            catch (Exception e)
            {
               doRollback();
               throw new RepositoryException("Could not save the changes", e);
            }
            doCommit();
            // notify listeners after storage commit
            notifySaveItems(logWrapper.getChangesLog(), false);
         }
      }
      else
      {
         // save normally 
         super.save(logWrapper);

         // notify listeners after storage commit
         notifySaveItems(logWrapper.getChangesLog(), false);
      }
   }

   /**
    * Commits the tx
    * @throws RepositoryException if the tx could not be committed.
    */
   private void doCommit() throws RepositoryException
   {
      try
      {
         transactionManager.commit();
      }
      catch (Exception e)
      {
         throw new RepositoryException("Could not commit the changes", e);
      }
   }

   /**
    * Starts a new Tx
    * @throws RepositoryException if the tx could not be created
    */
   private void doBegin() throws RepositoryException
   {
      try
      {
         transactionManager.begin();
      }
      catch (Exception e)
      {
         throw new RepositoryException("Could not create a new Tx", e);
      }
   }

   /**
    * Performs rollback of the action.
    */
   private void doRollback()
   {
      try
      {
         transactionManager.rollback();
      }
      catch (Exception e)
      {
         LOG.error("Rollback error ", e);
      }
   }

   /**
    * This will allow to notify listeners that are not TxAware once the Tx is committed
    * @param logWrapper
    * @throws RepositoryException if any error occurs
    */
   private void registerListener(final ChangesLogWrapper logWrapper) throws RepositoryException
   {
      try
      {
         // Why calling the listeners non tx aware has been done like this:
         // 1. If we call them in the commit phase and we use Arjuna with ISPN, we get:
         //       ActionStatus.COMMITTING > is not in a valid state to be invoking cache operations on.
         //       at org.infinispan.interceptors.TxInterceptor.enlist(TxInterceptor.java:195)
         //       at org.infinispan.interceptors.TxInterceptor.enlistReadAndInvokeNext(TxInterceptor.java:167)
         //       at org.infinispan.interceptors.TxInterceptor.visitGetKeyValueCommand(TxInterceptor.java:162)
         //       at org.infinispan.commands.read.GetKeyValueCommand.acceptVisitor(GetKeyValueCommand.java:64)
         //    This is due to the fact that ISPN enlist the cache even for a read access and enlistments are not 
         //    allowed in the commit phase
         // 2. If we call them in the commit phase, we use Arjuna with ISPN and we suspend the current tx,
         //    we get deadlocks because we try to acquire locks on cache entries that have been locked by the main tx.
         // 3. If we call them in the afterComplete, we use JOTM with ISPN and we suspend and resume the current tx, we get:
         //       jotm: resume: Invalid Transaction Status:STATUS_COMMITTED (Current.java, line 743) 
         //       javax.transaction.InvalidTransactionException: Invalid resume org.objectweb.jotm.TransactionImpl
         //       at org.objectweb.jotm.Current.resume(Current.java:744)
         //    This is due to the fact that it is not allowed to resume a tx when its status is STATUS_COMMITED

         txResourceManager.addListener(new TransactionableResourceManagerListener()
         {
            public void onCommit(boolean onePhase) throws Exception
            {
            }

            public void onAfterCompletion(int status) throws Exception
            {
               if (status == Status.STATUS_COMMITTED)
               {
                  // Since the tx is successfully committed we can call components non tx aware

                  // The listeners will need to be executed outside the current tx so we suspend
                  // the current tx we can face enlistment issues on product like ISPN
                  transactionManager.suspend();

                  SecurityHelper.doPrivilegedAction(new PrivilegedAction<Void>()
                  {
                     public Void run()
                     {
                        notifySaveItems(logWrapper.getChangesLog(), false);
                        return null;
                     }
                  });
                  // Since the resume method could cause issue with some TM at this stage, we don't resume the tx
               }
            }

            public void onAbort() throws Exception
            {
            }
         });
      }
      catch (Exception e)
      {
         throw new RepositoryException("The listener for the components not tx aware could not be added", e);
      }
   }

   /**
    * Get cached ItemData.
    * 
    * @param parentData
    *          parent
    * @param name
    *          Item name
    * @param itemType
    *          item type          
    * @return ItemData
    * @throws RepositoryException
    *           error
    */
   protected ItemData getCachedItemData(NodeData parentData, QPathEntry name, ItemType itemType)
      throws RepositoryException
   {
      return cache.isEnabled() ? cache.get(parentData.getIdentifier(), name, itemType) : null;
   }

   /**
    * Returns an item from cache by Identifier or null if the item don't cached.
    * 
    * @param identifier
    *          Item id
    * @return ItemData
    * @throws RepositoryException
    *           error
    */
   protected ItemData getCachedItemData(String identifier) throws RepositoryException
   {
      return cache.isEnabled() ? cache.get(identifier) : null;
   }

   /**
    * Get child NodesData.
    * 
    * @param nodeData
    *          parent
    * @param forcePersistentRead
    *          true if persistent read is required (without cache)
    * @return List<NodeData>
    * @throws RepositoryException
    *           Repository error
    */
   protected List<NodeData> getChildNodesData(final NodeData nodeData, boolean forcePersistentRead)
      throws RepositoryException
   {

      List<NodeData> childNodes = null;
      if (!forcePersistentRead && cache.isEnabled())
      {
         childNodes = cache.getChildNodes(nodeData);
         if (childNodes != null)
         {
            return childNodes;
         }
      }
      final DataRequest request = new DataRequest(nodeData.getIdentifier(), DataRequest.GET_NODES);

      try
      {
         request.start();
         if (!forcePersistentRead && cache.isEnabled())
         {
            // Try first to get the value from the cache since a
            // request could have been launched just before
            childNodes = cache.getChildNodes(nodeData);
            if (childNodes != null)
            {
               return childNodes;
            }
         }
         return executeAction(new PrivilegedExceptionAction<List<NodeData>>()
         {
            public List<NodeData> run() throws RepositoryException
            {
               List<NodeData> childNodes = CacheableWorkspaceDataManager.super.getChildNodesData(nodeData);
               if (cache.isEnabled())
               {
                  cache.addChildNodes(nodeData, childNodes);
               }

               return childNodes;
            }
         });
      }
      finally
      {
         request.done();
      }
   }

   protected List<NodeData> getChildNodesDataByPattern(final NodeData parentData,
      final List<QPathEntryFilter> patternFilters) throws RepositoryException
   {
      if (!cache.isEnabled())
      {
         return executeAction(new PrivilegedExceptionAction<List<NodeData>>()
         {
            public List<NodeData> run() throws RepositoryException
            {
               List<NodeData> childNodes =
                  CacheableWorkspaceDataManager.super.getChildNodesData(parentData, patternFilters);

               // ini ACL
               for (int i = 0; i < childNodes.size(); i++)
               {
                  childNodes.set(i, (NodeData)initACL(parentData, childNodes.get(i)));
               }

               return childNodes;
            }
         });
      }

      if (!cache.isPatternSupported())
      {
         return getChildNodesData(parentData);
      }

      // 1. check cache - outside data request

      List<NodeData> childNodesList = cache.getChildNodes(parentData);
      if (childNodesList != null)
      {
         return childNodesList;
      }

      final Map<String, NodeData> childNodesMap = new HashMap<String, NodeData>();

      final Set<QPathEntryFilter> uncachedPatterns = new HashSet<QPathEntryFilter>();
      for (int i = 0; i < patternFilters.size(); i++)
      {
         if (patternFilters.get(i).isExactName())
         {
            ItemData data = getCachedItemData(parentData, patternFilters.get(i).getQPathEntry(), ItemType.NODE);
            if (data != null)
            {
               if (!(data instanceof NullItemData))
               {
                  childNodesMap.put(data.getIdentifier(), (NodeData)data);
               }
            }
            else
            {
               uncachedPatterns.add(patternFilters.get(i));
            }
         }
         else
         {
            // get nodes list by pattern
            List<NodeData> cachedItemList = cache.getChildNodes(parentData, patternFilters.get(i));
            if (cachedItemList != null)
            {
               //merge results
               for (int j = 0, length = cachedItemList.size(); j < length; j++)
               {
                  childNodesMap.put(cachedItemList.get(j).getIdentifier(), cachedItemList.get(j));
               }
            }
            else
            {
               uncachedPatterns.add(patternFilters.get(i));
            }
         }
      }

      // 2. check cache - inside data requests
      if (!uncachedPatterns.isEmpty())
      {
         List<DataRequest> requests = new ArrayList<DataRequest>();
         try
         {
            final DataRequest request = new DataRequest(parentData.getIdentifier(), DataRequest.GET_NODES);
            request.start();
            requests.add(request);
            // Try first to get the value from the cache since a
            // request could have been launched just before
            childNodesList = cache.getChildNodes(parentData);
            if (childNodesList != null)
            {
               return childNodesList;
            }

            Iterator<QPathEntryFilter> patternIterator = uncachedPatterns.iterator();
            while (patternIterator.hasNext())
            {
               QPathEntryFilter pattern = patternIterator.next();
               if (pattern.isExactName())
               {
                  DataRequest exactNameRequest = new DataRequest(parentData.getIdentifier(), pattern.getQPathEntry());
                  exactNameRequest.start();
                  requests.add(exactNameRequest);

                  ItemData data = getCachedItemData(parentData, pattern.getQPathEntry(), ItemType.NODE);
                  if (data != null)
                  {
                     if (!(data instanceof NullItemData))
                     {
                        childNodesMap.put(data.getIdentifier(), (NodeData)data);
                     }
                     patternIterator.remove();
                  }
               }
               else
               {
                  // get node list by pattern
                  List<NodeData> cachedItemList = cache.getChildNodes(parentData, pattern);
                  if (cachedItemList != null)
                  {
                     //merge results
                     for (int j = 0, length = cachedItemList.size(); j < length; j++)
                     {
                        childNodesMap.put(cachedItemList.get(j).getIdentifier(), cachedItemList.get(j));
                     }
                     patternIterator.remove();
                  }
               }
            }
            patternIterator = null;

            // execute all patterns and put result in cache
            if (!uncachedPatterns.isEmpty())
            {
               executeAction(new PrivilegedExceptionAction<Void>()
               {
                  public Void run() throws RepositoryException
                  {
                     List<NodeData> persistedItemList =
                        CacheableWorkspaceDataManager.super.getChildNodesData(parentData,
                           new ArrayList<QPathEntryFilter>(uncachedPatterns));

                     if (persistedItemList.size() > 0)
                     {
                        NodeData parent = (NodeData)getItemData(parentData.getIdentifier());
                        if (parent != null)
                        {
                           // filter nodes list for each exact name
                           Iterator<QPathEntryFilter> patternIterator = uncachedPatterns.iterator();
                           while (patternIterator.hasNext())
                           {
                              QPathEntryFilter pattern = patternIterator.next();
                              @SuppressWarnings("unchecked")
                              List<NodeData> persistedNodeData = (List<NodeData>)pattern.accept(persistedItemList);
                              if (pattern.isExactName())
                              {
                                 if (persistedNodeData.isEmpty())
                                 {
                                    cache.put(new NullNodeData(parentData, pattern.getQPathEntry()));
                                 }
                                 else
                                 {
                                    cache.put(persistedNodeData.get(0));
                                 }
                              }
                              else
                              {
                                 cache.addChildNodes(parent, pattern, persistedNodeData);
                              }
                              for (NodeData node : persistedItemList)
                              {
                                 childNodesMap.put(node.getIdentifier(), node);
                              }
                           }
                        }
                     }
                     return null;
                  }
               });
            }
         }
         finally
         {
            for (DataRequest rq : requests)
            {
               rq.done();
            }
            requests.clear();
         }
      }

      return new ArrayList<NodeData>(childNodesMap.values());
   }

   /**
    * Get referenced properties data.
    * 
    * @param identifier
    *          referenceable identifier
    * @return List<PropertyData>
    * @throws RepositoryException
    *           Repository error
    */
   protected List<PropertyData> getReferencedPropertiesData(final String identifier) throws RepositoryException
   {
      List<PropertyData> refProps = null;
      if (cache.isEnabled())
      {
         refProps = cache.getReferencedProperties(identifier);
         if (refProps != null)
         {
            return refProps;
         }
      }
      final DataRequest request = new DataRequest(identifier, DataRequest.GET_REFERENCES);

      try
      {
         request.start();
         if (cache.isEnabled())
         {
            // Try first to get the value from the cache since a
            // request could have been launched just before
            refProps = cache.getReferencedProperties(identifier);
            if (refProps != null)
            {
               return refProps;
            }
         }
         return executeAction(new PrivilegedExceptionAction<List<PropertyData>>()
         {
            public List<PropertyData> run() throws RepositoryException
            {
               List<PropertyData> refProps = CacheableWorkspaceDataManager.super.getReferencesData(identifier, false);
               if (cache.isEnabled())
               {
                  cache.addReferencedProperties(identifier, refProps);
               }
               return refProps;
            }
         });
      }
      finally
      {
         request.done();
      }
   }

   /**
    * Get child PropertyData.
    * 
    * @param nodeData
    *          parent
    * @param forcePersistentRead
    *          true if persistent read is required (without cache)
    * @return List<PropertyData>
    * @throws RepositoryException
    *           Repository error
    */
   protected List<PropertyData> getChildPropertiesData(final NodeData nodeData, boolean forcePersistentRead)
      throws RepositoryException
   {

      List<PropertyData> childProperties = null;
      if (!forcePersistentRead && cache.isEnabled())
      {
         childProperties = cache.getChildProperties(nodeData);
         if (childProperties != null)
         {
            return childProperties;
         }
      }
      final DataRequest request = new DataRequest(nodeData.getIdentifier(), DataRequest.GET_PROPERTIES);

      try
      {
         request.start();
         if (!forcePersistentRead && cache.isEnabled())
         {
            // Try first to get the value from the cache since a
            // request could have been launched just before
            childProperties = cache.getChildProperties(nodeData);
            if (childProperties != null)
            {
               return childProperties;
            }
         }
         return executeAction(new PrivilegedExceptionAction<List<PropertyData>>()
         {
            public List<PropertyData> run() throws RepositoryException
            {
               List<PropertyData> childProperties =
                  CacheableWorkspaceDataManager.super.getChildPropertiesData(nodeData);

               if (childProperties.size() > 0 && cache.isEnabled())
               {
                  cache.addChildProperties(nodeData, childProperties);
               }
               return childProperties;
            }
         });
      }
      finally
      {
         request.done();
      }
   }

   protected List<PropertyData> getChildPropertiesDataByPattern(final NodeData nodeData,
      final List<QPathEntryFilter> patternFilters) throws RepositoryException
   {
      if (!cache.isEnabled())
      {
         return executeAction(new PrivilegedExceptionAction<List<PropertyData>>()
         {
            public List<PropertyData> run() throws RepositoryException
            {
               return CacheableWorkspaceDataManager.super.getChildPropertiesData(nodeData, patternFilters);
            }
         });
      }

      if (!cache.isPatternSupported())
      {
         return getChildPropertiesData(nodeData);
      }

      // 1. check cache - outside data request
      List<PropertyData> childPropsList = cache.getChildProperties(nodeData);
      if (childPropsList != null)
      {
         return childPropsList;
      }

      final Map<String, PropertyData> childPropsMap = new HashMap<String, PropertyData>();

      final Set<QPathEntryFilter> uncachedPatterns = new HashSet<QPathEntryFilter>();
      for (int i = 0; i < patternFilters.size(); i++)
      {
         if (patternFilters.get(i).isExactName())
         {
            ItemData data = getCachedItemData(nodeData, patternFilters.get(i).getQPathEntry(), ItemType.PROPERTY);
            if (data != null)
            {
               if (!(data instanceof NullPropertyData))
               {
                  childPropsMap.put(data.getIdentifier(), (PropertyData)data);
               }
            }
            else
            {
               uncachedPatterns.add(patternFilters.get(i));
            }
         }
         else
         {

            // get property list by pattern
            List<PropertyData> cachedItemList = cache.getChildProperties(nodeData, patternFilters.get(i));
            if (cachedItemList != null)
            {
               //merge results
               for (int j = 0, length = cachedItemList.size(); j < length; j++)
               {
                  childPropsMap.put(cachedItemList.get(j).getIdentifier(), cachedItemList.get(j));
               }
            }
            else
            {
               uncachedPatterns.add(patternFilters.get(i));
            }
         }
      }

      // 2. check cache - inside data requests
      if (!uncachedPatterns.isEmpty())
      {
         List<DataRequest> requests = new ArrayList<DataRequest>();
         try
         {

            final DataRequest request = new DataRequest(nodeData.getIdentifier(), DataRequest.GET_PROPERTIES);
            request.start();
            requests.add(request);

            // Try first to get the value from the cache since a
            // request could have been launched just before
            childPropsList = cache.getChildProperties(nodeData);
            if (childPropsList != null)
            {
               return childPropsList;
            }

            Iterator<QPathEntryFilter> patternIterator = uncachedPatterns.iterator();
            while (patternIterator.hasNext())
            {
               QPathEntryFilter pattern = patternIterator.next();
               if (pattern.isExactName())
               {
                  DataRequest exactNameRequest = new DataRequest(nodeData.getIdentifier(), pattern.getQPathEntry());
                  exactNameRequest.start();
                  requests.add(exactNameRequest);

                  ItemData data = getCachedItemData(nodeData, pattern.getQPathEntry(), ItemType.PROPERTY);
                  if (data != null)
                  {
                     if (!(data instanceof NullPropertyData))
                     {
                        childPropsMap.put(data.getIdentifier(), (PropertyData)data);
                     }
                     patternIterator.remove();
                  }
               }
               else
               {
                  // get properties list by pattern
                  List<PropertyData> cachedItemList = cache.getChildProperties(nodeData, pattern);
                  if (cachedItemList != null)
                  {
                     //merge results
                     for (int j = 0, length = cachedItemList.size(); j < length; j++)
                     {
                        childPropsMap.put(cachedItemList.get(j).getIdentifier(), cachedItemList.get(j));
                     }
                     patternIterator.remove();
                  }
               }
            }
            patternIterator = null;

            // execute all patterns and put result in cache
            if (!uncachedPatterns.isEmpty())
            {
               executeAction(new PrivilegedExceptionAction<Void>()
               {
                  public Void run() throws RepositoryException
                  {
                     List<PropertyData> persistedItemList =
                        CacheableWorkspaceDataManager.super.getChildPropertiesData(nodeData,
                           new ArrayList<QPathEntryFilter>(uncachedPatterns));

                     if (persistedItemList.size() > 0)
                     {
                        NodeData parent = (NodeData)getItemData(nodeData.getIdentifier());
                        if (parent != null)
                        {
                           // filter properties list for each exact name
                           Iterator<QPathEntryFilter> patternIterator = uncachedPatterns.iterator();
                           while (patternIterator.hasNext())
                           {
                              QPathEntryFilter pattern = patternIterator.next();
                              @SuppressWarnings("unchecked")
                              List<PropertyData> persistedPropData =
                                 (List<PropertyData>)pattern.accept(persistedItemList);
                              if (pattern.isExactName())
                              {
                                 if (persistedPropData.isEmpty())
                                 {
                                    cache.put(new NullPropertyData(parent, pattern.getQPathEntry()));
                                 }
                                 else
                                 {
                                    cache.put(persistedPropData.get(0));
                                 }
                              }
                              else
                              {
                                 cache.addChildProperties(parent, pattern, persistedPropData);
                              }

                              for (PropertyData node : persistedItemList)
                              {
                                 childPropsMap.put(node.getIdentifier(), node);
                              }
                           }
                        }
                     }
                     return null;
                  }
               });
            }
         }
         finally
         {
            for (DataRequest rq : requests)
            {
               rq.done();
            }
            requests.clear();
         }
      }

      return new ArrayList<PropertyData>(childPropsMap.values());
   }

   /**
    * Get persisted ItemData.
    * 
    * @param parentData
    *          parent
    * @param name
    *          Item name
    * @param itemType
    *          item type         
    * @return ItemData
    * @throws RepositoryException
    *           error
    */
   protected ItemData getPersistedItemData(NodeData parentData, QPathEntry name, ItemType itemType)
      throws RepositoryException
   {
      ItemData data = super.getItemData(parentData, name, itemType);
      if (cache.isEnabled())
      {
         if (data == null)
         {
            if (itemType == ItemType.NODE || itemType == ItemType.UNKNOWN)
            {
               cache.put(new NullNodeData(parentData, name));
            }
            else
            {
               cache.put(new NullPropertyData(parentData, name));
            }
         }
         else
         {
            cache.put(data);
         }
      }
      return data;
   }

   /**
    * Get persisted ItemData.
    * 
    * @param parentData
    *          parent
    * @param name
    *          Item name
    * @param itemType
    *          item type
    * @param createNullItemData
    *          indicates if nullItem should be created     
    * @return ItemData
    * @throws RepositoryException
    *           error
    */
   protected ItemData getPersistedItemData(NodeData parentData, QPathEntry name, ItemType itemType,
      boolean createNullItemData) throws RepositoryException
   {
      ItemData data = super.getItemData(parentData, name, itemType);
      if (cache.isEnabled())
      {
         if (data != null)
         {
            cache.put(data);
         }
         else if (createNullItemData)
         {
            if (itemType == ItemType.NODE || itemType == ItemType.UNKNOWN)
            {
               cache.put(new NullNodeData(parentData, name));
            }
            else
            {
               cache.put(new NullPropertyData(parentData, name));
            }
         }
      }
      return data;
   }

   /**
    * Call
    * {@link org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspacePersistentDataManager#getItemData(java.lang.String)
    * WorkspaceDataManager.getItemDataByIdentifier(java.lang.String)} and cache result if non null returned.
    * 
    * @see org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspacePersistentDataManager#getItemData(java.lang.String)
    */
   protected ItemData getPersistedItemData(String identifier) throws RepositoryException
   {
      ItemData data = super.getItemData(identifier);

      // set ACL

      if (data != null && data.isNode())
      {
         data = initACL(null, (NodeData)data);
      }

      if (cache.isEnabled())
      {
         if (data != null)
         {
            cache.put(data);
         }
         else if (identifier != null)
         {
            // no matter does property or node expected - store NullNodeData
            cache.put(new NullNodeData(identifier));
         }
      }
      return data;
   }

   /**
    * Get child PropertyData list (without ValueData).
    * 
    * @param nodeData
    *          parent
    * @param forcePersistentRead
    *          true if persistent read is required (without cache)
    * @return List<PropertyData>
    * @throws RepositoryException
    *           Repository error
    */
   protected List<PropertyData> listChildPropertiesData(final NodeData nodeData, boolean forcePersistentRead)
      throws RepositoryException
   {

      List<PropertyData> propertiesList;
      if (!forcePersistentRead && cache.isEnabled())
      {
         propertiesList = cache.listChildProperties(nodeData);
         if (propertiesList != null)
         {
            return propertiesList;
         }
      }

      final DataRequest request = new DataRequest(nodeData.getIdentifier(), DataRequest.GET_LIST_PROPERTIES);
      try
      {
         request.start();
         if (!forcePersistentRead && cache.isEnabled())
         {
            // Try first to get the value from the cache since a
            // request could have been launched just before
            propertiesList = cache.listChildProperties(nodeData);
            if (propertiesList != null)
            {
               return propertiesList;
            }
         }
         return executeAction(new PrivilegedExceptionAction<List<PropertyData>>()
         {
            public List<PropertyData> run() throws RepositoryException
            {
               List<PropertyData> propertiesList =
                  CacheableWorkspaceDataManager.super.listChildPropertiesData(nodeData);

               if (propertiesList.size() > 0 && cache.isEnabled())
               {
                  cache.addChildPropertiesList(nodeData, propertiesList);
               }
               return propertiesList;
            }
         });
      }
      finally
      {
         request.done();
      }
   }

   protected boolean isTxAware()
   {
      return transactionManager != null;
   }

   /**
    * Fix Property BLOB Values if someone has null file (swap actually) 
    * by reading the content from the storage (VS or JDBC no matter).
    * 
    * @param prop PropertyData
    * @throws RepositoryException
    */
   protected void fixPropertyValues(PropertyData prop) throws RepositoryException
   {
      final List<ValueData> vals = prop.getValues();
      for (int i = 0; i < vals.size(); i++)
      {
         ValueData vd = vals.get(i);
         if (!vd.isByteArray())
         {
            // check if file is correct
            FilePersistedValueData fpvd = (FilePersistedValueData)vd;
            if (fpvd.getFile() == null)
            {
               // error, value not found
               throw new RepositoryException("Value cannot be found in storage for cached Property "
                  + prop.getQPath().getAsString() + ", orderNumb:" + vd.getOrderNumber() + ", pversion:"
                  + prop.getPersistedVersion());
            }
         }
      }
   }


   /**
    * {@inheritDoc}
    */
   public void suspend() throws SuspendException
   {
      if (rpcService != null)
      {
         isResponsibleForResuming.set(true);

         try
         {
            rpcService.executeCommandOnAllNodes(suspend, true);
         }
         catch (SecurityException e)
         {
            throw new SuspendException(e);
         }
         catch (RPCException e)
         {
            throw new SuspendException(e);
         }
      }
      else
      {
         suspendLocally();
      }
   }

   /**
    * {@inheritDoc}
    */
   public void resume() throws ResumeException
   {
      if (rpcService != null)
      {
         try
         {
            rpcService.executeCommandOnAllNodes(resume, true);
         }
         catch (SecurityException e)
         {
            throw new ResumeException(e);
         }
         catch (RPCException e)
         {
            throw new ResumeException(e);
         }

         isResponsibleForResuming.set(false);
      }
      else
      {
         resumeLocally();
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean isSuspended()
   {
      return isSuspended.get();
   }

   private void suspendLocally() throws SuspendException
   {
      if (!isSuspended.get())
      {
         latcher.set(new CountDownLatch(1));
         isSuspended.set(true);

         if (workingThreads.get() > 0)
         {
            synchronized (workingThreads)
            {
               while (workingThreads.get() > 0)
               {
                  try
                  {
                     workingThreads.wait();
                  }
                  catch (InterruptedException e)
                  {
                     if (LOG.isTraceEnabled())
                     {
                        LOG.trace(e.getMessage(), e);
                     }
                  }
               }
            }
         }
      }
   }

   private void resumeLocally()
   {
      if (isSuspended.get())
      {
         latcher.get().countDown();
         isSuspended.set(false);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void onChange(TopologyChangeEvent event)
   {
      if (isSuspended.get())
      {
         new Thread()
         {
            @Override
            public synchronized void run()
            {
               try
               {
                  List<Object> results = rpcService.executeCommandOnAllNodes(requestForResponsibleForResuming, true);

                  for (Object result : results)
                  {
                     if ((Boolean)result)
                     {
                        return;
                     }
                  }

                  // node which was responsible for resuming leave the cluster, so resume component
                  resumeLocally();
               }
               catch (SecurityException e1)
               {
                  LOG.error("You haven't privileges to execute remote command", e1);
               }
               catch (RPCException e1)
               {
                  LOG.error("Exception during command execution", e1);
               }
            }
         }.start();
      }
   }

   /**
    * Initialization remote commands.
    */
   private void initRemoteCommands()
   {
      if (rpcService != null)
      {
         // register commands
         suspend = rpcService.registerCommand(new RemoteCommand()
         {

            public String getId()
            {
               return "org.exoplatform.services.jcr.impl.dataflow.persistent.CacheableWorkspaceDataManager-suspend-"
                  + dataContainer.getUniqueName();
            }

            public Serializable execute(Serializable[] args) throws Throwable
            {
               suspendLocally();
               return null;
            }
         });

         resume = rpcService.registerCommand(new RemoteCommand()
         {

            public String getId()
            {
               return "org.exoplatform.services.jcr.impl.dataflow.persistent.CacheableWorkspaceDataManager-resume-"
                  + dataContainer.getUniqueName();
            }

            public Serializable execute(Serializable[] args) throws Throwable
            {
               resumeLocally();
               return null;
            }
         });

         requestForResponsibleForResuming = rpcService.registerCommand(new RemoteCommand()
         {

            public String getId()
            {
               return "org.exoplatform.services.jcr.impl.dataflow.persistent.CacheableWorkspaceDataManager"
                  + "-requestForResponsibilityForResuming-" + dataContainer.getUniqueName();
            }

            public Serializable execute(Serializable[] args) throws Throwable
            {
               return isResponsibleForResuming.get();
            }
         });

         rpcService.registerTopologyChangeListener(this);
      }
   }

   private <T> T executeAction(PrivilegedExceptionAction<T> action) throws RepositoryException
   {
      try
      {
         return SecurityHelper.doPrivilegedExceptionAction(action);
      }
      catch (PrivilegedActionException pae)
      {
         Throwable cause = pae.getCause();
         if (cause instanceof RepositoryException)
         {
            throw (RepositoryException)cause;
         }
         else if (cause instanceof RuntimeException)
         {
            throw (RuntimeException)cause;
         }
         else
         {
            throw new RuntimeException(cause);
         }
      }
   }

   /**
    * Init ACL of the node.
    * @param parent
    *          - a parent, can be null (get item by id)
    * @param node
    *          - an item data
    * @return - an item data with ACL was initialized
    * @throws RepositoryException
    */
   private ItemData initACL(NodeData parent, NodeData node) throws RepositoryException
   {
      return initACL(parent, node, null);
   }

   /**
    * @param parent
    *          - a parent, can be null (get item by id)
    * @param node
    *          - an node data
    * @param search
    *          - indicates what we are looking for
    * @return - an node data with ACL was initialized
    * @throws RepositoryException
    */
   private NodeData initACL(NodeData parent, NodeData node, ACLSearch search) throws RepositoryException
   {
      if (node != null)
      {
         AccessControlList acl = node.getACL();
         if (acl == null)
         {
            if (parent != null)
            {
               // use parent ACL
               node =
                  new TransientNodeData(node.getQPath(), node.getIdentifier(), node.getPersistedVersion(),
                     node.getPrimaryTypeName(), node.getMixinTypeNames(), node.getOrderNumber(),
                     node.getParentIdentifier(), parent.getACL());
            }
            else
            {
               if (search == null)
               {
                  search = new ACLSearch(null, null);
               }
               // use nearest ancestor ACL... case of get by id
               node =
                  new TransientNodeData(node.getQPath(), node.getIdentifier(), node.getPersistedVersion(),
                     node.getPrimaryTypeName(), node.getMixinTypeNames(), node.getOrderNumber(),
                     node.getParentIdentifier(), getNearestACAncestorAcl(node, search));
            }
         }
         else if (!acl.hasPermissions())
         {
            // use nearest ancestor permissions
            if (search == null)
            {
               search = new ACLSearch(acl.getOwner(), null);
            }
            else
            {
               search.setOwner(acl.getOwner());
               if (search.found())
               {
                  return new TransientNodeData(node.getQPath(), node.getIdentifier(), node.getPersistedVersion(),
                     node.getPrimaryTypeName(), node.getMixinTypeNames(), node.getOrderNumber(),
                     node.getParentIdentifier(), new AccessControlList(acl.getOwner(), null));
               }
            }
            AccessControlList ancestorAcl =
               parent != null && parent.getACL() != null && parent.getACL().hasPermissions() ? parent.getACL()
                  : getNearestACAncestorAcl(node, search);

            node =
               new TransientNodeData(node.getQPath(), node.getIdentifier(), node.getPersistedVersion(), node
                  .getPrimaryTypeName(), node.getMixinTypeNames(), node.getOrderNumber(), node.getParentIdentifier(),
                  new AccessControlList(acl.getOwner(), ancestorAcl.getPermissionEntries()));
         }
         else if (!acl.hasOwner())
         {
            if (search == null)
            {
               search = new ACLSearch(null, acl.getPermissionEntries());
            }
            else
            {
               search.setPermissions(acl.getPermissionEntries());
               if (search.found())
               {
                  return new TransientNodeData(node.getQPath(), node.getIdentifier(), node.getPersistedVersion(),
                     node.getPrimaryTypeName(), node.getMixinTypeNames(), node.getOrderNumber(),
                     node.getParentIdentifier(), new AccessControlList(null, acl.getPermissionEntries()));
               }
            }
            // use nearest ancestor owner
            AccessControlList ancestorAcl =
               parent != null && parent.getACL() != null && parent.getACL().hasOwner() ? parent.getACL()
                  : getNearestACAncestorAcl(node, search);

            node =
               new TransientNodeData(node.getQPath(), node.getIdentifier(), node.getPersistedVersion(), node
                  .getPrimaryTypeName(), node.getMixinTypeNames(), node.getOrderNumber(), node.getParentIdentifier(),
                  new AccessControlList(ancestorAcl.getOwner(), acl.getPermissionEntries()));

         }
      }

      return node;
   }

   /**
    * Traverse items parents in persistent storage for ACL containing parent. Same work is made in
    * SessionDataManager.getItemData(NodeData, QPathEntry[]) but for session scooped items.
    * 
    * @param node
    *          - item
    * @param search
    *          - indicates what we are looking for
    * @return - parent or null
    * @throws RepositoryException
    */
   private AccessControlList getNearestACAncestorAcl(NodeData node, ACLSearch search) throws RepositoryException
   {
      String id = node.getParentIdentifier();
      if (id != null)
      {
         boolean filtersEnabled = this.filtersEnabled.get();
         BloomFilter<String> filterPermissions = this.filterPermissions;
         BloomFilter<String> filterOwner = this.filterOwner;
         if (filtersEnabled && filterOwner != null && filterPermissions != null)
         {
            QPathEntry[] entries = node.getQPath().getEntries();
            for (int i = entries.length - 2; i >= 0; i--)
            {
               QPathEntry entry = entries[i];
               String currentId = entry.getId();
               if (currentId == null)
               {
                  // the path doesn't contain any id so we do a normal call
                  break;
               }
               else if ((!search.hasOwner() && filterOwner.contains(currentId))
                  || (!search.hasPermissions() && filterPermissions.contains(currentId)))
               {
                  id = currentId;
                  break;
               }
               else
               {
                  id = currentId;
               }
            }
         }
         NodeData parent = getACL(id, search);
         if (parent != null && parent.getACL() != null)
         {
            // has an AC parent
            return parent.getACL();
         }
      }
      return new AccessControlList();
   }

   /**
    * Find Item by identifier to get the missing ACL information.
    * 
    * @param identifier the id of the node that we are looking for to fill the ACL research
    * @param search the ACL search describing what we are looking for
    * @return NodeData, data by identifier
    */
   private NodeData getACL(String identifier, ACLSearch search) throws RepositoryException
   {
      final ItemData item = getItemData(identifier, false);
      return item != null && item.isNode() ? initACL(null, (NodeData)item, search) : null;
   }

   /**
    * Gets the list of all the ACL holders
    * @throws RepositoryException if an error occurs
    */
   public List<ACLHolder> getACLHolders() throws RepositoryException
   {
      WorkspaceStorageConnection conn = dataContainer.openConnection();
      try
      {
         return conn.getACLHolders();
      }
      finally
      {
         conn.close();
      }
   }

   /**
    * Reloads the bloom filters
    * @return <code>true</code> if the filters could be reloaded successfully, <code>false</code> otherwise.
    */
   @Managed
   @ManagedDescription("Reloads the bloom filters used to efficiently manage the ACLs")
   public boolean reloadFilters()
   {
      return loadFilters(false);
   }

   /**
    * Clears the bloom filters
    */
   protected void clear()
   {
      this.filterPermissions = null;
      this.filterOwner = null;
   }

   /**
    * @see org.picocontainer.Startable#start()
    */
   public void start()
   {
      initRemoteCommands();
      isStopped.set(false);

      try
      {
         this.cache.addListener(this);
      }
      catch (UnsupportedOperationException e)
      {
         filtersSupported.set(false);
         if (LOG.isDebugEnabled())
         {
            LOG.debug("The bloom filters are disabled as they are not supported by the cache implementation "
               + cache.getClass().getName());
         }
         return;
      }

      loadFilters(true);
   }

   /**
    * Loads the bloom filters
    * @param cleanOnFail clean everything if an error occurs
    * @return <code>true</code> if the filters could be loaded successfully, <code>false</code> otherwise.
    */
   protected boolean loadFilters(boolean cleanOnFail)
   {
      if (!filtersSupported.get())
      {
         if (LOG.isWarnEnabled())
         {
            LOG.warn("The bloom filters are not supported therefore they cannot be reloaded");
         }
         return false;
      }
      filtersEnabled.set(false);
      this.filterPermissions = new BloomFilter<String>(bfProbability, bfElementNumber);
      this.filterOwner = new BloomFilter<String>(bfProbability, bfElementNumber);
      boolean fails = true;
      List<ACLHolder> holders = null;
      try
      {
         LOG.info("Getting all the ACL Holders from the persistence layer");
         holders = getACLHolders();
         fails = false;
      }
      catch (UnsupportedOperationException e)
      {
         filtersSupported.set(false);
         if (LOG.isDebugEnabled())
         {
            LOG.debug("The method getACLHolders is not supported", e);
         }
      }
      catch (RepositoryException e)
      {
         LOG.error("Could not load all the ACL loaders", e);
      }
      finally
      {
         if (fails)
         {
            if (cleanOnFail)
            {
               clear();
               cache.removeListener(this);
            }
            return false;
         }
         else if (holders != null && !holders.isEmpty())
         {
            LOG.info("Adding all the ACL Holders found into the BloomFilters");
            for (int i = 0, length = holders.size(); i < length; i++)
            {
               ACLHolder holder = holders.get(i);
               if (holder == null)
               {
                  continue;
               }
               if (holder.hasOwner())
               {
                  filterOwner.add(holder.getId());
               }
               if (holder.hasPermissions())
               {
                  filterPermissions.add(holder.getId());
               }
            }
         }         
      }
      filtersEnabled.set(true);
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public void stop()
   {
      if (filtersEnabled.get())
      {
         cache.removeListener(this);
      }
      if (rpcService!=null)
      {
         rpcService.unregisterTopologyChangeListener(this);
         
         rpcService.unregisterCommand(requestForResponsibleForResuming);
         rpcService.unregisterCommand(resume);
         rpcService.unregisterCommand(suspend);
      }

      isStopped.set(true);
      resumeLocally();
   }

   /**
    * {@inheritDoc}
    */
   public void onCacheEntryAdded(ItemData data)
   {
      onCacheEntryUpdated(data);
   }

   /**
    * {@inheritDoc}
    */
   public void onCacheEntryUpdated(ItemData data)
   {
      if (data instanceof NodeData)
      {
         NodeData node = (NodeData)data;
         AccessControlList acl = node.getACL();
         if (acl == null)
         {
            return;
         }
         if (acl.hasOwner())
         {
            filterOwner.add(node.getIdentifier());
         }
         if (acl.hasPermissions())
         {
            filterPermissions.add(node.getIdentifier());
         }
      }
   }
   
   /**
    * {@inheritDoc}
    */
   public int getPriority()
   {
      return PRIORITY_HIGH;
   }

   /**
    * Defines what we are really looking for
    */
   private static class ACLSearch
   {
      private String owner;

      private List<AccessControlEntry> permissions;

      ACLSearch(String owner, List<AccessControlEntry> permissions)
      {
         this.owner = owner;
         this.permissions = permissions;
      }

      /**
       * @return <code>true</code> if the owner and the permission have been found, <code>false</code>
       * otherwise
       */
      public boolean found()
      {
         return owner != null && permissions != null;
      }

      /**
       * @param owner the owner to set
       */
      public void setOwner(String owner)
      {
         if (this.owner == null)
         {
            this.owner = owner;
         }
      }

      /**
       * @param permissions the permissions to set
       */
      public void setPermissions(List<AccessControlEntry> permissions)
      {
         if (this.permissions == null)
         {
            this.permissions = permissions;
         }
      }

      /**
       * @return the owner
       */
      public boolean hasOwner()
      {
         return owner != null;
      }

      /**
       * @return the permissions
       */
      public boolean hasPermissions()
      {
         return permissions != null;
      }
   }
}