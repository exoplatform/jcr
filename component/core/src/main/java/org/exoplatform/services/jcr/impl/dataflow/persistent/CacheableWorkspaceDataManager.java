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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.jcr.RepositoryException;

import org.exoplatform.services.jcr.dataflow.persistent.WorkspaceStorageCache;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.storage.SystemDataContainerHolder;
import org.exoplatform.services.jcr.storage.WorkspaceDataContainer;

/**
 * Created by The eXo Platform SAS. 
 * 
 * <br/>
 * Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 13.04.2006
 * 
 * @version $Id: CacheableWorkspaceDataManager.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class CacheableWorkspaceDataManager
   extends WorkspacePersistentDataManager
{

   /**
    * Items cache.
    */
   protected final WorkspaceStorageCache cache;

   /**
    * Requests cache.
    */
   protected final Map<Integer, DataRequest> requestCache;

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
         this.hcode = 31 * (31 + this.type) + this.id.hashCode();
      }

      /**
       * Find the same, and if found wait till the one will be finished.
       * 
       * WARNING. This method effective with cache use only!!! Without cache the database will control
       * requests performance/chaching process.
       * 
       * @return this data request
       */
      DataRequest waitSame()
      {
         DataRequest prev = null;
         synchronized (requestCache)
         {
            prev = requestCache.get(this.hashCode());
         }
         if (prev != null)
            prev.await();
         return this;
      }

      /**
       * Start the request, each same will wait till this will be finished
       */
      void start()
      {
         synchronized (requestCache)
         {
            requestCache.put(this.hashCode(), this);
         }
      }

      /**
       * Done the request. Must be called after the data request will be finished. This call allow
       * another same requests to be performed.
       */
      void done()
      {
         this.ready.countDown();
         synchronized (requestCache)
         {
            requestCache.remove(this.hashCode());
         }
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
      this.requestCache = new HashMap<Integer, DataRequest>();
      addItemPersistenceListener(cache);
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
         return getPersistedItemData(identifier);
      }
      return data;
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
         data = getPersistedItemData(parentData, name);
      }

      return data;
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
      return getChildPropertiesData(nodeData, false);
   }

   /**
    * {@inheritDoc}
    */
   public List<PropertyData> listChildPropertiesData(NodeData nodeData) throws RepositoryException
   {
      return listChildPropertiesData(nodeData, false);
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

      final DataRequest request = new DataRequest(nodeData.getIdentifier(), DataRequest.GET_NODES);

      List<NodeData> childNodes = null;
      if (!forcePersistentRead && cache.isEnabled())
      {
         request.waitSame();
         childNodes = cache.getChildNodes(nodeData);
         if (childNodes != null)
            return childNodes;
      }

      try
      {
         request.start();
         childNodes = super.getChildNodesData(nodeData);
         if (cache.isEnabled())
         {
            NodeData parentData = (NodeData) cache.get(nodeData.getIdentifier());
            if (parentData == null)
               parentData = (NodeData) super.getItemData(nodeData.getIdentifier());
            cache.addChildNodes(parentData, childNodes);
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

      final DataRequest request = new DataRequest(nodeData.getIdentifier(), DataRequest.GET_PROPERTIES);

      List<PropertyData> childProperties = null;
      if (!forcePersistentRead && cache.isEnabled())
      {
         request.waitSame();
         childProperties = cache.getChildProperties(nodeData);
         if (childProperties != null)
            return childProperties;
      }

      try
      {
         request.start();

         childProperties = super.getChildPropertiesData(nodeData);
         // TODO childProperties.size() > 0 for SDB
         if (childProperties.size() > 0 && cache.isEnabled())
         {
            NodeData parentData = (NodeData) cache.get(nodeData.getIdentifier());
            if (parentData == null)
               parentData = (NodeData) super.getItemData(nodeData.getIdentifier());
            cache.addChildProperties(parentData, childProperties);
         }
         return childProperties;
      }
      finally
      {
         request.done();
      }
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

      final DataRequest request = new DataRequest(nodeData.getIdentifier(), DataRequest.GET_PROPERTIES);

      List<PropertyData> propertiesList;
      if (!forcePersistentRead && cache.isEnabled())
      {
         request.waitSame(); // wait if getChildProp... working somewhere
         propertiesList = cache.listChildProperties(nodeData);
         if (propertiesList != null)
            return propertiesList;
      }

      propertiesList = super.listChildPropertiesData(nodeData);
      // TODO propertiesList.size() > 0 for SDB
      if (propertiesList.size() > 0 && cache.isEnabled())
      {
         NodeData parentData = (NodeData) cache.get(nodeData.getIdentifier());
         if (parentData == null)
            parentData = (NodeData) super.getItemData(nodeData.getIdentifier());
         cache.addChildPropertiesList(parentData, propertiesList);
      }
      return propertiesList;
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
    * Get Items Cache.
    * 
    * @return WorkspaceStorageCache
    */
   public WorkspaceStorageCache getCache()
   {
      return cache;
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

      ItemData data = null;
      data = super.getItemData(parentData, name);
      if (data != null && cache.isEnabled())
      {
         cache.put(data);
      }
      return data;
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
    * Call
    * {@link org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspacePersistentDataManager#getItemData(java.lang.String)
    * WorkspaceDataManager.getItemDataByIdentifier(java.lang.String)} and cache result if non null
    * returned.
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

}
