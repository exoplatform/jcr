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

import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.persistent.WorkspaceStorageCache;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.jbosscache.JBossCacheWorkspaceStorageCache;
import org.exoplatform.services.jcr.impl.storage.SystemDataContainerHolder;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCStorageConnection;
import org.exoplatform.services.jcr.storage.WorkspaceDataContainer;
import org.exoplatform.services.transaction.TransactionService;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;

import javax.jcr.RepositoryException;
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
public class CacheableWorkspaceDataManager extends WorkspacePersistentDataManager
{

   /**
    * Items cache.
    */
   protected final WorkspaceStorageCache cache;

   /**
    * Requests cache.
    */
   protected final ConcurrentMap<Integer, DataRequest> requestCache;

   private TransactionManager transactionManager;

   /**
    * ItemData request, used on get operations.
    * 
    */
   protected class DataRequest
   {
      /**
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

   protected class SaveInTransaction extends TxIsolatedOperation
   {
      final ItemStateChangesLog changes;

      SaveInTransaction(ItemStateChangesLog changes)
      {
         super(transactionManager);
         this.changes = changes;
      }

      @Override
      protected void action() throws RepositoryException
      {
         CacheableWorkspaceDataManager.super.save(changes);
      }

      @Override
      protected void txAction() throws RepositoryException
      {
         super.txAction();

         // notify listeners after transaction commit but before the current resume!
         try
         {
            notifySaveItems(changes, false);
         }
         catch (Throwable th)
         {
            // TODO XA layer can throws runtime exceptions
            throw new RepositoryException(th);
         }
      }
   }

   /**
    * CacheableWorkspaceDataManager constructor.
    * 
    * @param dataContainer
    *          Workspace data container (persistent level)
    * @param cache
    *          Items cache
    * @param systemDataContainerHolder
    *          System Workspace data container (persistent level)
    * @param transactionService TransactionService         
    */
   public CacheableWorkspaceDataManager(WorkspaceDataContainer dataContainer, WorkspaceStorageCache cache,
      SystemDataContainerHolder systemDataContainerHolder, TransactionService transactionService)
   {
      super(dataContainer, systemDataContainerHolder);
      this.cache = cache;

      this.requestCache = new ConcurrentHashMap<Integer, DataRequest>();
      addItemPersistenceListener(cache);

      transactionManager = transactionService.getTransactionManager();
   }

   /**
    * CacheableWorkspaceDataManager constructor.
    * 
    * @param dataContainer
    *          Workspace data container (persistent level)
    * @param cache
    *          Items cache
    * @param systemDataContainerHolder
    *          System Workspace data container (persistent level)
    */
   public CacheableWorkspaceDataManager(WorkspaceDataContainer dataContainer, WorkspaceStorageCache cache,
      SystemDataContainerHolder systemDataContainerHolder)
   {
      super(dataContainer, systemDataContainerHolder);
      this.cache = cache;

      this.requestCache = new ConcurrentHashMap<Integer, DataRequest>();
      addItemPersistenceListener(cache);

      if (cache instanceof JBossCacheWorkspaceStorageCache)
      {
         transactionManager = ((JBossCacheWorkspaceStorageCache)cache).getTransactionManager();
      }
      else
      {
         transactionManager = null;
      }
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
   public int getChildNodesCount(NodeData parent) throws RepositoryException
   {
      if (cache.isEnabled())
      {
         List<NodeData> childNodes = cache.getChildNodes(parent);
         if (childNodes != null)
         {
            return childNodes.size();
         }
      }

      return super.getChildNodesCount(parent);
   }

   /**
    * {@inheritDoc}
    */
   public List<NodeData> getChildNodesData(NodeData nodeData) throws RepositoryException
   {
      return getChildNodesData(nodeData, false);
   }

   /**
    * {@inheritDoc}
    */
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
   public ItemData getItemData(NodeData parentData, QPathEntry name) throws RepositoryException
   {

      // 1. Try from cache
      ItemData data = getCachedItemData(parentData, name);

      // 2. Try from container
      if (data == null)
      {
         final DataRequest request = new DataRequest(parentData.getIdentifier(), name);
         
         try
         {
            request.start();
            // Try first to get the value from the cache since a
            // request could have been launched just before
            data = getCachedItemData(parentData, name);
            if (data == null)
            {
               data = getPersistedItemData(parentData, name);               
            }
         }
         finally
         {
            request.done();
         }
      }
      else if (!data.isNode())
      {
         fixPropertyValues((PropertyData)data);
      }

      return data;
   }

   /**
    * {@inheritDoc}
    */
   public ItemData getItemData(String identifier) throws RepositoryException
   {
      // 2. Try from cache
      ItemData data = getCachedItemData(identifier);

      // 3. Try from container
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
               data = getPersistedItemData(identifier);               
            }
         }
         finally
         {
            request.done();
         }
      }
      else if (!data.isNode())
      {
         fixPropertyValues((PropertyData)data);
      }

      return data;
   }

   /**
    * {@inheritDoc}
    */
   public List<PropertyData> getReferencesData(String identifier, boolean skipVersionStorage)
      throws RepositoryException
   {
      return super.getReferencesData(identifier, skipVersionStorage);
   }

   /**
    * {@inheritDoc}
    */
   public List<PropertyData> listChildPropertiesData(NodeData nodeData) throws RepositoryException
   {
      return listChildPropertiesData(nodeData, false);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void save(final ItemStateChangesLog changesLog) throws RepositoryException
   {
      if (isTxAware())
      {
         // save in dedicated XA transaction
         new SaveInTransaction(changesLog).perform();
      }
      else
      {
         // save normaly 
         super.save(changesLog);

         // notify listeners after storage commit
         notifySaveItems(changesLog, false);
      }
   }

   /**
    * Get cached ItemData.
    * 
    * @param parentData
    *          parent
    * @param name
    *          Item name
    * @return ItemData
    * @throws RepositoryException
    *           error
    */
   protected ItemData getCachedItemData(NodeData parentData, QPathEntry name) throws RepositoryException
   {
      return cache.get(parentData.getIdentifier(), name);
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
      return cache.get(identifier);
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
   protected List<NodeData> getChildNodesData(NodeData nodeData, boolean forcePersistentRead)
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
         childNodes = super.getChildNodesData(nodeData);
         if (cache.isEnabled())
         {
            NodeData parentData = (NodeData)cache.get(nodeData.getIdentifier());
            if (parentData == null)
            {
               parentData = (NodeData)super.getItemData(nodeData.getIdentifier());
            }

            if (parentData != null)
            {
               cache.addChildNodes(parentData, childNodes);
            }
         }
         return childNodes;
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
   protected List<PropertyData> getChildPropertiesData(NodeData nodeData, boolean forcePersistentRead)
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

         childProperties = super.getChildPropertiesData(nodeData);
         // TODO childProperties.size() > 0 for SDB
         if (childProperties.size() > 0 && cache.isEnabled())
         {
            NodeData parentData = (NodeData)cache.get(nodeData.getIdentifier());
            if (parentData == null)
            {
               parentData = (NodeData)super.getItemData(nodeData.getIdentifier());
            }

            if (parentData != null)
            {
               cache.addChildProperties(parentData, childProperties);
            }
         }
         return childProperties;
      }
      finally
      {
         request.done();
      }
   }

   /**
    * Get persisted ItemData.
    * 
    * @param parentData
    *          parent
    * @param name
    *          Item name
    * @return ItemData
    * @throws RepositoryException
    *           error
    */
   protected ItemData getPersistedItemData(NodeData parentData, QPathEntry name) throws RepositoryException
   {
      ItemData data = super.getItemData(parentData, name);
      if (data != null && cache.isEnabled())
      {
         cache.put(data);
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
      if (data != null && cache.isEnabled())
      {
         cache.put(data);
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
   protected List<PropertyData> listChildPropertiesData(NodeData nodeData, boolean forcePersistentRead)
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
         propertiesList = super.listChildPropertiesData(nodeData);
         // TODO propertiesList.size() > 0 for SDB
         if (propertiesList.size() > 0 && cache.isEnabled())
         {
            NodeData parentData = (NodeData)cache.get(nodeData.getIdentifier());
            if (parentData == null)
            {
               parentData = (NodeData)super.getItemData(nodeData.getIdentifier());
            }

            if (parentData != null)
            {
               cache.addChildPropertiesList(parentData, propertiesList);
            }
         }
         return propertiesList;
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
    * Fix Property BLOB Values if someone has null file (swap actually) by reading the content from the storage (VS or JDBC no matter).
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
               // need read from storage
               ValueData svd = getPropertyValue(prop.getIdentifier(), vd.getOrderNumber(), prop.getPersistedVersion());

               if (svd == null)
               {
                  // error, value not found
                  throw new RepositoryException("Value cannot be found in storage for cached Property "
                     + prop.getQPath().getAsString() + ", orderNumb:" + vd.getOrderNumber() + ", pversion:"
                     + prop.getPersistedVersion());
               }

               vals.set(i, svd);
            }
         }
      }
   }

   /**
    * Fill Property Value from persistent storage.
    * 
    * @param prop PropertyData, original Property data
    * @return PropertyData
    * @throws IllegalStateException
    * @throws RepositoryException 
    */
   protected ValueData getPropertyValue(String propertyId, int orderNumb, int persistedVersion)
      throws IllegalStateException, RepositoryException
   {
      // TODO use interface not JDBC
      JDBCStorageConnection conn = (JDBCStorageConnection)dataContainer.openConnection();
      try
      {
         return conn.getValue(propertyId, orderNumb, persistedVersion);
      }
      finally
      {
         conn.close();
      }
   }
}