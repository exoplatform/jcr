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
package org.exoplatform.services.jcr.impl.dataflow.persistent.infinispan;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.services.ispn.DistributedCacheManager;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.config.CacheEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedNodeData;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedPropertyData;
import org.exoplatform.services.jcr.dataflow.persistent.WorkspaceStorageCache;
import org.exoplatform.services.jcr.dataflow.persistent.WorkspaceStorageCacheListener;
import org.exoplatform.services.jcr.datamodel.IllegalPathException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.NullItemData;
import org.exoplatform.services.jcr.datamodel.NullNodeData;
import org.exoplatform.services.jcr.datamodel.NullPropertyData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.backup.BackupException;
import org.exoplatform.services.jcr.impl.backup.Backupable;
import org.exoplatform.services.jcr.impl.backup.DataRestore;
import org.exoplatform.services.jcr.impl.backup.rdbms.DataRestoreContext;
import org.exoplatform.services.jcr.impl.core.itemfilters.QPathEntryFilter;
import org.exoplatform.services.jcr.impl.dataflow.ValueDataUtil;
import org.exoplatform.services.jcr.impl.dataflow.persistent.SimplePersistedSize;
import org.exoplatform.services.jcr.infinispan.AbstractMapper;
import org.exoplatform.services.jcr.infinispan.CacheKey;
import org.exoplatform.services.jcr.infinispan.ISPNCacheFactory;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.transaction.ActionNonTxAware;
import org.exoplatform.services.transaction.TransactionService;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.context.Flag;
import org.infinispan.distexec.mapreduce.Collector;
import org.infinispan.distexec.mapreduce.MapReduceTask;
import org.infinispan.distexec.mapreduce.Reducer;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.picocontainer.Startable;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

/**
 * Created by The eXo Platform SAS.<p/>
 * 
 * Cache based on Infinispan.<p/>
 *
 * <ul>
 * <li>cache transparent: or item cached or not, we should not generate "not found" Exceptions </li>
 * 
 * This cache implementation stores items by UUID and parent UUID with QPathEntry.
 * Except this cache stores list of children UUID.
 * 
 * <p/>
 * Current state notes (subject of change):
 * <ul>
 * <li>cache implements WorkspaceStorageCache, without any stuff about references and locks</li>
 * <li>transaction style implemented via batches, do with JTA (i.e. via exo's TransactionService + JBoss TM)</li>
 * </ul>
 * 
 * @author <a href="anatoliy.bazko@exoplatform.org">Anatoliy Bazko</a>
 * @version $Id: ISPNCacheWorkspaceStorageCache.java 3514 2010-11-22 16:14:36Z nzamosenchuk $
 */
public class ISPNCacheWorkspaceStorageCache implements WorkspaceStorageCache, Backupable, Startable
{
   private static final Log LOG = ExoLogger//NOSONAR
      .getLogger("exo.jcr.component.core.impl.infinispan.v5.ISPNCacheWorkspaceStorageCache");//NOSONAR

   /**
    * Name of the cache in case of the distributed cache
    */
   private static final String CACHE_NAME = "JCRCache";

   /**
    * This id will be the unique identifier of the workspace in case the
    * distributed mode is enabled as the cache will be then shared so we
    * need this id to prevent mixing data of different workspace. In case
    * the workspace is not distributed the value of this variable will be
    * null to avoid consuming more memory for nothing 
    */
   protected final String ownerId;

   private final boolean enabled;
   
   protected final BufferedISPNCache cache;
   
   private final GlobalOperationCaller caller;

   /**
    * The list of all the listeners
    */
   private final List<WorkspaceStorageCacheListener> listeners =
      new CopyOnWriteArrayList<WorkspaceStorageCacheListener>();

   private final CacheActionNonTxAware<Void, Void> commitTransaction = new CacheActionNonTxAware<Void, Void>()
   {
      @Override
      protected Void execute(Void arg)
      {
         cache.commitTransaction();
         return null;
      }
   };

   private final CacheActionNonTxAware<ItemData, String> getFromCacheById =
      new CacheActionNonTxAware<ItemData, String>()
      {
         @Override
         protected ItemData execute(String id)
         {
            return id == null ? null : (ItemData)cache.get(new CacheId(getOwnerId(), id));
         }
      };

   private final CacheActionNonTxAware<ItemData, String> getFromBufferedCacheById =
      new CacheActionNonTxAware<ItemData, String>()
      {
         @Override
         protected ItemData execute(String id)
         {
            return id == null ? null : (ItemData)cache.getFromBuffer(new CacheId(getOwnerId(), id));
         }
      };

   private final CacheActionNonTxAware<List<NodeData>, NodeData> getChildNodes =
      new CacheActionNonTxAware<List<NodeData>, NodeData>()
      {
         @Override
         protected List<NodeData> execute(NodeData parent)
         {
            // get list of children uuids
            final Set<String> set = (Set<String>)cache.get(new CacheNodesId(getOwnerId(), parent.getIdentifier()));

            if (set != null)
            {
               if (set instanceof FakeValueSet)
               {
                  return null;
               }
               final List<NodeData> childs = new ArrayList<NodeData>();

               for (String childId : set)
               {
                  NodeData child = (NodeData)cache.get(new CacheId(getOwnerId(), childId));
                  if (child == null)
                  {
                     return null;
                  }

                  childs.add(child);
               }

               // order children by orderNumber, as HashSet returns children in other order
               Collections.sort(childs, new NodesOrderComparator<NodeData>());
               return childs;
            }
            else
            {
               return null;
            }
         }
      };

   private final CacheActionNonTxAware<ItemData, Object> getFromCacheByPath =
      new CacheActionNonTxAware<ItemData, Object>()
      {
         @Override
         protected ItemData execute(Object... args)
         {
            String parentIdentifier = (String)args[0];
            QPathEntry name = (QPathEntry)args[1];
            ItemType itemType = (ItemType)args[2];
            String itemId = null;

            if (itemType == ItemType.UNKNOWN)
            {
               // Try as node first.
               itemId = (String)cache.get(new CacheQPath(getOwnerId(), parentIdentifier, name, ItemType.NODE));

               if (itemId == null || itemId.equals(NullItemData.NULL_ID))
               {
                  // node with such a name is not found or marked as not-exist, so check the properties
                  String propId =
                     (String)cache.get(new CacheQPath(getOwnerId(), parentIdentifier, name, ItemType.PROPERTY));
                  if (propId != null)
                  {
                     itemId = propId;
                  }
               }
            }
            else if (itemType == ItemType.NODE)
            {
               itemId = (String)cache.get(new CacheQPath(getOwnerId(), parentIdentifier, name, ItemType.NODE));;
            }
            else
            {
               itemId = (String)cache.get(new CacheQPath(getOwnerId(), parentIdentifier, name, ItemType.PROPERTY));;
            }

            if (itemId != null)
            {
               if (itemId.equals(NullItemData.NULL_ID))
               {
                  if (itemType == ItemType.UNKNOWN || itemType == ItemType.NODE)
                  {
                     return new NullNodeData();
                  }
                  else
                  {
                     return new NullPropertyData();
                  }
               }
               else
               {
                  return get(itemId);
               }
            }
            return null;
         }
      };

   private final CacheActionNonTxAware<Integer, NodeData> getChildNodesCount =
      new CacheActionNonTxAware<Integer, NodeData>()
      {
         @Override
         protected Integer execute(NodeData parent)
         {
            Set<String> list = (Set<String>)cache.get(new CacheNodesId(getOwnerId(), parent.getIdentifier()));
            return list != null ? list.size() : -1;
         }
      };

   private final CacheActionNonTxAware<List<PropertyData>, Object> getChildProps =
      new CacheActionNonTxAware<List<PropertyData>, Object>()
      {
         @Override
         protected List<PropertyData> execute(Object... args)
         {
            String parentId = (String)args[0];
            boolean withValue = (Boolean)args[1];
            // get list of children uuids
            final Set<String> set = (Set<String>)cache.get(new CachePropsId(getOwnerId(), parentId));
            if (set != null)
            {
               final List<PropertyData> childs = new ArrayList<PropertyData>();

               for (String childId : set)
               {
                  PropertyData child = (PropertyData)cache.get(new CacheId(getOwnerId(), childId));

                  if (child == null)
                  {
                     return null;
                  }
                  if (withValue && child.getValues().size() <= 0)
                  {
                     return null;
                  }
                  childs.add(child);
               }
               return childs;
            }
            else
            {
               return null;
            }
         }
      };

   private final CacheActionNonTxAware<List<PropertyData>, String> getReferencedProperties =
      new CacheActionNonTxAware<List<PropertyData>, String>()
      {
         @Override
         protected List<PropertyData> execute(String identifier)
         {
            // get list of children uuids
            final Set<String> set = (Set<String>)cache.get(new CacheRefsId(getOwnerId(), identifier));
            if (set != null)
            {
               final List<PropertyData> props = new ArrayList<PropertyData>();

               for (String childId : set)
               {
                  PropertyData prop = (PropertyData)cache.get(new CacheId(getOwnerId(), childId));

                  if (prop == null || prop instanceof NullItemData)
                  {
                     return null;
                  }
                  // add property as many times as has referenced values 
                  List<ValueData> lData = prop.getValues();
                  for (int i = 0, length = lData.size(); i < length; i++)
                  {
                     ValueData vdata = lData.get(i);
                     try
                     {
                        if (ValueDataUtil.getString(vdata).equals(identifier))
                        {
                           props.add(prop);
                        }
                     }
                     catch (IllegalStateException e)
                     {
                        // property was not added, force read from lower layer
                        return null;
                     }
                     catch (RepositoryException e)
                     {
                        // property was not added, force read from lower layer
                        return null;
                     }
                  }
               }
               return props;
            }
            else
            {
               return null;
            }
         }
      };

   private final CacheActionNonTxAware<Long, Void> getSize = new CacheActionNonTxAware<Long, Void>()
   {
      @Override
      protected Long execute(Void arg)
      {
         return (long)caller.getCacheSize();
      }
   };

   /**
    * Node order comparator for getChildNodes().
    */
   class NodesOrderComparator<N extends NodeData> implements Comparator<NodeData>
   {
      /**
       * {@inheritDoc}
       */
      public int compare(NodeData n1, NodeData n2)
      {
         return n1.getOrderNumber() - n2.getOrderNumber();
      }
   }

   class ChildItemsIterator<T extends ItemData> implements Iterator<T>
   {

      private Iterator<String> childs;
      private Iterator<Entry<CacheKey, Object>> entries; 
      private String parentId;
      private Class<? extends ItemData> type;
      
      private T next;
      
      ChildItemsIterator(String parentId, Class<? extends ItemData> type, CacheKey key)
      {
         Set<String> set = (Set<String>)cache.get(key);
         if (set != null)
         {
            this.childs = ((Set<String>)cache.get(key)).iterator();
         }
         else
         {
            this.entries = cache.entrySet().iterator();
            this.parentId = parentId;
            this.type = type;
         }
         fetchNext();
      }

      protected void fetchNext()
      {
         if (childs != null)
         {
            // Case where all the children nodes have been loaded
            if (childs.hasNext())
            {
               // traverse to the first existing or the end of children
               T n = null;
               do
               {
                  n = (T)cache.get(new CacheId(getOwnerId(), childs.next()));
               }
               while (n == null && childs.hasNext());
               next = n;
            }
            else
            {
               next = null;
            }
         }
         else
         {
            // Case where all the children nodes have not been loaded
            if (entries.hasNext())
            {
               // traverse to the first children node or the end of the entry set
               T n = null;
               do
               {
                  Entry<CacheKey, Object> entry = entries.next();
                  if (entry.getKey() instanceof CacheId && type.isInstance(entry.getValue()))
                  {
                     ItemData item = (ItemData)entry.getValue();
                     if (parentId.equals(item.getParentIdentifier()))
                     {
                        n = (T)item;
                     }
                  }
               }
               while (n == null && entries.hasNext());
               next = n;
            }
            else
            {
               next = null;
            }
         }
      }

      public boolean hasNext()
      {
         return next != null;
      }

      public T next()
      {
         if (next == null)
         {
            throw new NoSuchElementException();
         }

         final T current = next;
         fetchNext();
         return current;
      }

      public void remove()
      {
         throw new IllegalArgumentException("Not implemented");
      }
   }

   class ChildNodesIterator<N extends NodeData> extends ChildItemsIterator<N>
   {
      ChildNodesIterator(String parentId)
      {
         super(parentId, NodeData.class, new CacheNodesId(getOwnerId(), parentId));
      }

      @Override
      public N next()
      {
         return super.next();
      }
   }

   class ChildPropertiesIterator<P extends PropertyData> extends ChildItemsIterator<P>
   {

      ChildPropertiesIterator(String parentId)
      {
         super(parentId, PropertyData.class, new CachePropsId(getOwnerId(), parentId));
      }

      @Override
      public P next()
      {
         return super.next();
      }
   }

   /**
    * Cache constructor.
    * 
    * @param wsConfig WorkspaceEntry workspace configuration
    * @param cfm The configuration manager
    * @throws RepositoryException if error of initialization
    * @throws RepositoryConfigurationException if error of configuration
    */
   public ISPNCacheWorkspaceStorageCache(WorkspaceEntry wsConfig, ConfigurationManager cfm) throws RepositoryException,
      RepositoryConfigurationException
   {
      this(null, wsConfig, cfm, null, null);
   }

   /**
    * Cache constructor.
    * 
    * @param ctx The {@link ExoContainerContext} that owns the current component
    * @param wsConfig WorkspaceEntry workspace configuration
    * @param cfm The configuration manager
    * @param ts TransactionService external transaction service
    * @throws RepositoryException if error of initialization
    * @throws RepositoryConfigurationException if error of configuration
    */
   public ISPNCacheWorkspaceStorageCache(ExoContainerContext ctx, WorkspaceEntry wsConfig, ConfigurationManager cfm,
      TransactionService ts) throws RepositoryException, RepositoryConfigurationException
   {
      this(ctx, wsConfig, cfm, null, ts);
   }

   /**
    * Cache constructor.
    * 
    * @param ctx The {@link ExoContainerContext} that owns the current component
    * @param wsConfig WorkspaceEntry workspace configuration
    * @param cfm The configuration manager
    * @param dcm The distributed cache manager
    * @throws RepositoryException if error of initialization
    * @throws RepositoryConfigurationException if error of configuration
    */
   public ISPNCacheWorkspaceStorageCache(ExoContainerContext ctx, WorkspaceEntry wsConfig, ConfigurationManager cfm,
      DistributedCacheManager dcm) throws RepositoryException, RepositoryConfigurationException
   {
      this(ctx, wsConfig, cfm, dcm, null);
   }

   /**
    * Cache constructor.
    * 
    * @param ctx The {@link ExoContainerContext} that owns the current component
    * @param wsConfig WorkspaceEntry workspace configuration
    * @param cfm The configuration manager
    * @param dcm The distributed cache manager
    * @param ts TransactionService external transaction service
    * @throws RepositoryException if error of initialization
    * @throws RepositoryConfigurationException if error of configuration
    */
   public ISPNCacheWorkspaceStorageCache(ExoContainerContext ctx, WorkspaceEntry wsConfig, ConfigurationManager cfm,
      DistributedCacheManager dcm, TransactionService ts) throws RepositoryException, RepositoryConfigurationException
   {
      if (wsConfig.getCache() == null)
      {
         throw new RepositoryConfigurationException("Cache configuration not found");
      }
      this.enabled = wsConfig.getCache().isEnabled();
      
      // create cache using custom factory
      ISPNCacheFactory<CacheKey, Object> factory =
         new ISPNCacheFactory<CacheKey, Object>(cfm, ts == null ? null : ts.getTransactionManager());

      // create parent Infinispan instance
      CacheEntry cacheEntry = wsConfig.getCache();
      boolean useDistributedCache = cacheEntry.getParameterBoolean("use-distributed-cache", false);
      Cache<CacheKey, Object> parentCache;
      if (useDistributedCache)
      {
                  // We expect a distributed cache
                 if (dcm == null)
                 {
                     throw new IllegalArgumentException("The DistributedCacheManager has not been defined in the configuration,"
                        + " please configure it at root container level if you want to use a distributed cache.");
                  }
                  parentCache = dcm.getCache(CACHE_NAME);
                  this.ownerId = ctx.getName();
                  if (LOG.isDebugEnabled())
                  {
                     LOG.debug("The distributed cache has been enabled for the workspace whose unique id is " + ownerId);
                  }
      }
      else
      {
         parentCache = factory.createCache("Data_" + wsConfig.getUniqueName(), cacheEntry);
         Configuration config = parentCache.getCacheConfiguration();
         if (config.clustering().cacheMode() == CacheMode.DIST_SYNC
            || config.clustering().cacheMode() == CacheMode.DIST_ASYNC)
         {
            throw new IllegalArgumentException("Cache configuration not allowed, if you want to use the distributed "
               + "cache please enable the parameter 'use-distributed-cache' and configure the DistributedCacheManager.");
         }
         this.ownerId = null;
      }
      Boolean allowLocalChanges =
         useDistributedCache ? cacheEntry.getParameterBoolean("allow-local-changes", Boolean.TRUE) : Boolean.TRUE;
      this.cache = new BufferedISPNCache(parentCache, allowLocalChanges);
      if (useDistributedCache)
      {
         this.caller = new DistributedOperationCaller();
      }
      else
      {
         this.caller = new GlobalOperationCaller();
         cache.addListener(new CacheEventListener());
      }

      this.cache.start();
   }

   private boolean isDistributedMode()
   {
      return ownerId != null;
   }

   private String getOwnerId()
   {
      return ownerId;
   }

   /**
    * Return TransactionManager used by ISPN backing the JCR cache.
    * 
    * @return TransactionManager
    */
   public TransactionManager getTransactionManager()
   {
      return cache.getTransactionManager();
   }

   /**
    * {@inheritDoc}
    */
   public void put(ItemData item)
   {
      // There is different commit processing for NullNodeData and ordinary ItemData.
      if (item instanceof NullItemData)
      {
         putNullItem((NullItemData)item);
         return;
      }

      boolean inTransaction = cache.isTransactionActive();
      try
      {
         if (!inTransaction)
         {
            cache.beginTransaction();
         }
         cache.setLocal(true);
         if (item.isNode())
         {
            putNode((NodeData)item, ModifyChildOption.NOT_MODIFY);
         }
         else
         {
            putProperty((PropertyData)item, ModifyChildOption.NOT_MODIFY);
         }
      }
      finally
      {
         cache.setLocal(false);
         if (!inTransaction)
         {
            dedicatedTxCommit();
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void remove(ItemData item)
   {
      removeItem(item);
   }

   /**
    * {@inheritDoc}
    */
   public void onSaveItems(final ItemStateChangesLog itemStates)
   {
      //  if something happen we will rollback changes
      boolean rollback = true;
      try
      {
         ItemState lastDelete = null;

         cache.beginTransaction();
         for (ItemState state : itemStates.getAllStates())
         {
            if (state.isAdded())
            {
               if (state.isPersisted())
               {
                  putItem(state.getData());
               }
            }
            else if (state.isUpdated())
            {
               if (state.isPersisted())
               {
                  // There was a problem with removing a list of samename siblings in on transaction,
                  // so putItemInBufferedCache(..) and updateInBufferedCache(..) used instead put(..) and update (..) methods.
                  ItemData prevItem = putItemInBufferedCache(state.getData());
                  if (prevItem != null && state.isNode())
                  {
                     // nodes reordered, if previous is null it's InvalidItemState case
                     updateInBuffer((NodeData)state.getData(), (NodeData)prevItem);
                  }
               }
            }
            else if (state.isDeleted())
            {
               if (state.isPersisted())
               {
                  removeItem(state.getData());
               }
            }
            else if (state.isRenamed())
            {
               renameItem(state, lastDelete);
            }
            else if (state.isPathChanged())
            {
               updateTreePath(state.getOldPath(), state.getData().getQPath(), null);
            }
            else if (state.isMixinChanged())
            {
               if (state.isPersisted())
               {
                  // update subtree ACLs
                  updateMixin((NodeData)state.getData());
               }
            }

            if (state.isDeleted())
            {
               lastDelete = state;
            }
         }

         cache.commitTransaction();
         rollback = false;
      }
      finally
      {
         if (rollback)
         {
            cache.rollbackTransaction();
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void addChildNodesByPage(NodeData parent, List<NodeData> childs, int fromOrderNum)
   {
      boolean inTransaction = cache.isTransactionActive();
      try
      {
         if (!inTransaction)
         {
            cache.beginTransaction();
         }

         cache.setLocal(true);

         CacheNodesByPageId cacheId = new CacheNodesByPageId(getOwnerId(), parent.getIdentifier());
         Map<Integer, Set<String>> pages = (Map<Integer, Set<String>>)cache.get(cacheId);
         if (pages == null)
         {
            pages = new HashMap<Integer, Set<String>>();
         }

         Set<String> set = new HashSet<String>();
         for (NodeData child : childs)
         {
            putNode(child, ModifyChildOption.NOT_MODIFY);
            set.add(child.getIdentifier());
         }

         pages.put(fromOrderNum, set);
         cache.put(cacheId, pages);
      }
      finally
      {
         cache.setLocal(false);
         if (!inTransaction)
         {
            dedicatedTxCommit();
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void addChildNodes(NodeData parent, List<NodeData> childs)
   {
      boolean inTransaction = cache.isTransactionActive();
      try
      {
         if (!inTransaction)
         {
            cache.beginTransaction();
         }

         cache.setLocal(true);
         Set<Object> set = new HashSet<Object>();
         for (NodeData child : childs)
         {
            putNode(child, ModifyChildOption.NOT_MODIFY);
            set.add(child.getIdentifier());
         }
         cache.putIfAbsent(new CacheNodesId(getOwnerId(), parent.getIdentifier()), set);
      }
      finally
      {
         cache.setLocal(false);
         if (!inTransaction)
         {
            dedicatedTxCommit();
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void addChildNodes(NodeData parent, QPathEntryFilter pattern, List<NodeData> childs)
   {
      boolean inTransaction = cache.isTransactionActive();
      try
      {
         if (!inTransaction)
         {
            cache.beginTransaction();
         }

         cache.setLocal(true);
         Set<String> set = new HashSet<String>();

         for (NodeData child : childs)
         {
            putNode(child, ModifyChildOption.NOT_MODIFY);
            set.add(child.getIdentifier());
         }

         CachePatternNodesId cacheId = new CachePatternNodesId(getOwnerId(), parent.getIdentifier());
         Map<QPathEntryFilter, Set<String>> patterns = (Map<QPathEntryFilter, Set<String>>)cache.get(cacheId);
         if (patterns == null)
         {
            patterns = new HashMap<QPathEntryFilter, Set<String>>();
         }
         patterns.put(pattern, set);
         cache.put(cacheId, patterns);
      }
      finally
      {
         cache.setLocal(false);
         if (!inTransaction)
         {
            dedicatedTxCommit();
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void addChildProperties(NodeData parent, List<PropertyData> childs)
   {
      boolean inTransaction = cache.isTransactionActive();
      try
      {
         if (!inTransaction)
         {
            cache.beginTransaction();
         }
         cache.setLocal(true);
         if (childs.size() > 0)
         {
            // add all new
            Set<Object> set = new HashSet<Object>();
            for (PropertyData child : childs)
            {
               putProperty(child, ModifyChildOption.NOT_MODIFY);
               set.add(child.getIdentifier());
            }
            cache.putIfAbsent(new CachePropsId(getOwnerId(), parent.getIdentifier()), set);

         }
         else
         {
            LOG.warn("Empty properties list cached " + (parent != null ? parent.getQPath().getAsString() : parent));
         }
      }
      finally
      {
         cache.setLocal(false);
         if (!inTransaction)
         {
            dedicatedTxCommit();
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void addChildProperties(NodeData parent, QPathEntryFilter pattern, List<PropertyData> childs)
   {
      boolean inTransaction = cache.isTransactionActive();
      try
      {
         if (!inTransaction)
         {
            cache.beginTransaction();
         }
         cache.setLocal(true);
         if (childs.size() > 0)
         {
            // add all new
            Set<String> set = new HashSet<String>();
            for (PropertyData child : childs)
            {
               putProperty(child, ModifyChildOption.NOT_MODIFY);
               set.add(child.getIdentifier());
            }

            CachePatternPropsId cacheId = new CachePatternPropsId(getOwnerId(), parent.getIdentifier());
            Map<QPathEntryFilter, Set<String>> patterns = (Map<QPathEntryFilter, Set<String>>)cache.get(cacheId);
            if (patterns == null)
            {
               patterns = new HashMap<QPathEntryFilter, Set<String>>();
            }
            patterns.put(pattern, set);
            cache.put(cacheId, patterns);
         }
      }
      finally
      {
         cache.setLocal(false);
         if (!inTransaction)
         {
            dedicatedTxCommit();
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void addChildPropertiesList(NodeData parent, List<PropertyData> childProperties)
   {

   }

   /**
    * {@inheritDoc}
    */
   public ItemData get(String parentIdentifier, QPathEntry name, ItemType itemType)
   {
      return getFromCacheByPath.run(parentIdentifier, name, itemType);
   }

   /**
    * {@inheritDoc}
    */
   public ItemData get(String id)
   {
      return getFromCacheById.run(id);
   }

   /**
    * {@inheritDoc}
    */
   public List<NodeData> getChildNodes(final NodeData parent)
   {
      return getChildNodes.run(parent);
   }

   /**
    * {@inheritDoc}
    */
   public void addChildNodesCount(NodeData parent, int count)
   {
      boolean inTransaction = cache.isTransactionActive();
      try
      {
         if (!inTransaction)
         {
            cache.beginTransaction();
         }

         cache.setLocal(true);
         cache.putIfAbsent(new CacheNodesId(getOwnerId(), parent.getIdentifier()), new FakeValueSet(count));
      }
      finally
      {
         cache.setLocal(false);
         if (!inTransaction)
         {
            dedicatedTxCommit();
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public List<NodeData> getChildNodesByPage(final NodeData parent, final int fromOrderNum)
   {
      // get list of children uuids
      final Map<Integer, Set<String>> pages =
         (Map<Integer, Set<String>>)cache.get(new CacheNodesByPageId(getOwnerId(), parent.getIdentifier()));

      if (pages == null)
      {
         return null;
      }

      Set<String> set = pages.get(fromOrderNum);
      if (set == null)
      {
         return null;
      }

      final List<NodeData> childs = new ArrayList<NodeData>();
      for (String childId : set)
      {
         NodeData child = (NodeData)cache.get(new CacheId(getOwnerId(), childId));
         if (child == null)
         {
            return null;
         }

         childs.add(child);
      }

      // order children by orderNumber, as HashSet returns children in other order
      Collections.sort(childs, new NodesOrderComparator<NodeData>());
      return childs;
   }

   /**
    * {@inheritDoc}
    */
   public List<NodeData> getChildNodes(final NodeData parent, final QPathEntryFilter pattern)
   {
      // get list of children uuids
      final Map<QPathEntryFilter, Set<String>> patterns =
         (Map<QPathEntryFilter, Set<String>>)cache.get(new CachePatternNodesId(getOwnerId(), parent.getIdentifier()));

      if (patterns == null)
      {
         return null;
      }

      Set<String> set = patterns.get(pattern);
      if (set == null)
      {
         return null;
      }

      final List<NodeData> childs = new ArrayList<NodeData>();

      for (String childId : set)
      {
         NodeData child = (NodeData)cache.get(new CacheId(getOwnerId(), childId));
         if (child == null)
         {
            return null;
         }

         childs.add(child);
      }

      // order children by orderNumber, as HashSet returns children in other order
      Collections.sort(childs, new NodesOrderComparator<NodeData>());
      return childs;

   }

   /**
    * {@inheritDoc}
    */
   public int getChildNodesCount(NodeData parent)
   {
      return getChildNodesCount.run(parent);
   }

   /**
    * {@inheritDoc}
    */
   public List<PropertyData> getChildProperties(NodeData parent)
   {
      return getChildProps(parent.getIdentifier(), true);
   }

   public List<PropertyData> getChildProperties(NodeData parent, QPathEntryFilter pattern)
   {
      // get list of children uuids
      final Map<QPathEntryFilter, Set<String>> patterns =
         (Map<QPathEntryFilter, Set<String>>)cache.get(new CachePatternPropsId(getOwnerId(), parent.getIdentifier()));

      if (patterns == null)
      {
         return null;
      }

      Set<String> set = patterns.get(pattern);
      if (set == null)
      {
         return null;
      }

      final List<PropertyData> childs = new ArrayList<PropertyData>();

      for (String childId : set)
      {
         PropertyData child = (PropertyData)cache.get(new CacheId(getOwnerId(), childId));

         if (child == null)
         {
            return null;
         }
         if (child.getValues().size() <= 0)
         {
            return null;
         }
         childs.add(child);
      }
      return childs;

   }

   /**
    * {@inheritDoc}
    */
   public List<PropertyData> listChildProperties(NodeData parent)
   {
      return getChildProps(parent.getIdentifier(), false);
   }

   /**
    * Internal get child properties.
    *
    * @param parentId String
    * @param withValue boolean, if true only "full" Propeties can be returned
    * @return List of PropertyData
    */
   protected List<PropertyData> getChildProps(String parentId, boolean withValue)
   {
      return getChildProps.run(parentId, withValue);
   }

   /**
    * {@inheritDoc}
    */
   public long getSize()
   {
      return getSize.run();
   }

   /**
    * {@inheritDoc}
    */
   public boolean isEnabled()
   {
      return enabled;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isPatternSupported()
   {
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isChildNodesByPageSupported()
   {
      return true;
   }

   /**
    * Internal put Item.
    *
    * @param item ItemData, new data to put in the cache
    * @return ItemData, previous data or null
    */
   protected ItemData putItem(ItemData item)
   {
      if (item.isNode())
      {
         return putNode((NodeData)item, ModifyChildOption.MODIFY);
      }
      else
      {
         return putProperty((PropertyData)item, ModifyChildOption.MODIFY);
      }
   }

   protected ItemData putItemInBufferedCache(ItemData item)
   {
      if (item.isNode())
      {
         return putNodeInBufferedCache((NodeData)item, ModifyChildOption.MODIFY);
      }
      else
      {
         return putProperty((PropertyData)item, ModifyChildOption.MODIFY);
      }

   }

   /**
    * Internal put Node.
    *
    * @param node, NodeData, new data to put in the cache
    * @return NodeData, previous data or null
    */
   protected ItemData putNode(NodeData node, ModifyChildOption modifyListsOfChild)
   {
      if (node.getParentIdentifier() != null)
      {
         if (modifyListsOfChild == ModifyChildOption.NOT_MODIFY)
         {
            cache.putIfAbsent(new CacheQPath(getOwnerId(), node.getParentIdentifier(), node.getQPath(), ItemType.NODE),
               node.getIdentifier());
         }
         else
         {
            cache.put(new CacheQPath(getOwnerId(), node.getParentIdentifier(), node.getQPath(), ItemType.NODE),
               node.getIdentifier());
         }

         // if MODIFY and List present OR FORCE_MODIFY, then write
         if (modifyListsOfChild != ModifyChildOption.NOT_MODIFY)
         {
            cache.addToPatternList(new CachePatternNodesId(getOwnerId(), node.getParentIdentifier()), node);
            cache.addToList(new CacheNodesId(getOwnerId(), node.getParentIdentifier()), node.getIdentifier(),
               modifyListsOfChild == ModifyChildOption.FORCE_MODIFY);

            cache.remove(new CacheNodesByPageId(getOwnerId(), node.getParentIdentifier()));
         }
      }

      if (modifyListsOfChild == ModifyChildOption.NOT_MODIFY)
      {
         return (ItemData)cache.putIfAbsent(new CacheId(getOwnerId(), node.getIdentifier()), node);
      }
      else
      {
         return (ItemData)cache.put(new CacheId(getOwnerId(), node.getIdentifier()), node, true);
      }
   }

   protected ItemData putNodeInBufferedCache(NodeData node, ModifyChildOption modifyListsOfChild)
   {
      if (node.getParentIdentifier() != null)
      {
         cache.put(new CacheQPath(getOwnerId(), node.getParentIdentifier(), node.getQPath(), ItemType.NODE),
            node.getIdentifier());

         // if MODIFY and List present OR FORCE_MODIFY, then write
         if (modifyListsOfChild != ModifyChildOption.NOT_MODIFY)
         {
            cache.addToList(new CacheNodesId(getOwnerId(), node.getParentIdentifier()), node.getIdentifier(),
               modifyListsOfChild == ModifyChildOption.FORCE_MODIFY);
         }
      }

      // NullNodeData must never be returned inside internal cache operations. 
      ItemData itemData = (ItemData)cache.putInBuffer(new CacheId(getOwnerId(), node.getIdentifier()), node);
      return (itemData instanceof NullItemData) ? null : itemData;
   }

   /**
    * Internal put NullNode.
    *
    * @param item, NullItemData, new data to put in the cache
    */
   protected void putNullItem(NullItemData item)
   {
      boolean inTransaction = cache.isTransactionActive();
      try
      {
         if (!inTransaction)
         {
            cache.beginTransaction();
         }
         cache.setLocal(true);

         if (!item.getIdentifier().equals(NullItemData.NULL_ID))
         {
            cache.putIfAbsent(new CacheId(getOwnerId(), item.getIdentifier()), item);
         }
         else if (item.getName() != null && item.getParentIdentifier() != null)
         {
            cache.putIfAbsent(
               new CacheQPath(getOwnerId(), item.getParentIdentifier(), item.getName(), ItemType.getItemType(item)),
               NullItemData.NULL_ID);
         }
      }
      finally
      {
         cache.setLocal(false);
         if (!inTransaction)
         {
            dedicatedTxCommit();
         }
      }
   }

   /**
    * Internal put Property.
    *
    * @param node, PropertyData, new data to put in the cache
    * @return PropertyData, previous data or null
    */
   protected PropertyData putProperty(PropertyData prop, ModifyChildOption modifyListsOfChild)
   {
      // if MODIFY and List present OR FORCE_MODIFY, then write
      if (modifyListsOfChild != ModifyChildOption.NOT_MODIFY)
      {
         cache.addToPatternList(new CachePatternPropsId(getOwnerId(), prop.getParentIdentifier()), prop);
         cache.addToList(new CachePropsId(getOwnerId(), prop.getParentIdentifier()), prop.getIdentifier(),
            modifyListsOfChild == ModifyChildOption.FORCE_MODIFY);
      }
      if (modifyListsOfChild == ModifyChildOption.NOT_MODIFY)
      {
         cache.putIfAbsent(
            new CacheQPath(getOwnerId(), prop.getParentIdentifier(), prop.getQPath(), ItemType.PROPERTY),
            prop.getIdentifier());
      }
      else
      {
         cache.put(new CacheQPath(getOwnerId(), prop.getParentIdentifier(), prop.getQPath(), ItemType.PROPERTY),
            prop.getIdentifier());
      }

      // add referenced property
      if (modifyListsOfChild != ModifyChildOption.NOT_MODIFY && prop.getType() == PropertyType.REFERENCE)
      {
         List<ValueData> lData = prop.getValues();
         for (int i = 0, length = lData.size(); i < length; i++)
         {
            ValueData vdata = lData.get(i);
            String nodeIdentifier = null;
            try
            {
               nodeIdentifier = ValueDataUtil.getString(vdata);
            }
            catch (IllegalStateException e)
            {
               if (LOG.isTraceEnabled())
               {
                  LOG.trace("An exception occurred: " + e.getMessage());
               }
            }
            catch (RepositoryException e)
            {
               if (LOG.isTraceEnabled())
               {
                  LOG.trace("An exception occurred: " + e.getMessage());
               }
            }
            cache.addToList(new CacheRefsId(getOwnerId(), nodeIdentifier), prop.getIdentifier(),
               modifyListsOfChild == ModifyChildOption.FORCE_MODIFY);
         }
      }
      // NullItemData must never be returned inside internal cache operations. 
      PropertyData propData;
      if (modifyListsOfChild == ModifyChildOption.NOT_MODIFY)
      {
         propData = (PropertyData)cache.putIfAbsent(new CacheId(getOwnerId(), prop.getIdentifier()), prop);
      }
      else
      {
         propData = (PropertyData)cache.put(new CacheId(getOwnerId(), prop.getIdentifier()), prop, true);
      }

      return (propData instanceof NullPropertyData) ? null : propData;
   }

   protected void removeItem(ItemData item)
   {
      cache.remove(new CacheId(getOwnerId(), item.getIdentifier()));
      cache
         .remove(new CacheQPath(getOwnerId(), item.getParentIdentifier(), item.getQPath(), ItemType.getItemType(item)));

      if (item.isNode())
      {
         if (item.getParentIdentifier() != null)
         {
            cache.removeFromPatternList(new CachePatternNodesId(getOwnerId(), item.getParentIdentifier()), item);
            cache.removeFromList(new CacheNodesId(getOwnerId(), item.getParentIdentifier()), item.getIdentifier());
            cache.remove(new CacheNodesByPageId(getOwnerId(), item.getParentIdentifier()));
         }

         cache.remove(new CacheNodesId(getOwnerId(), item.getIdentifier()));
         cache.remove(new CachePropsId(getOwnerId(), item.getIdentifier()));
         cache.remove(new CacheNodesByPageId(getOwnerId(), item.getIdentifier()));
         cache.remove(new CachePatternNodesId(getOwnerId(), item.getIdentifier()));
         cache.remove(new CachePatternPropsId(getOwnerId(), item.getIdentifier()));
         cache.remove(new CacheRefsId(getOwnerId(), item.getIdentifier()));
      }
      else
      {
         cache.removeFromPatternList(new CachePatternPropsId(getOwnerId(), item.getParentIdentifier()), item);
         cache.removeFromList(new CachePropsId(getOwnerId(), item.getParentIdentifier()), item.getIdentifier());
      }
   }

   /**
    * Update Node's mixin and ACL.
    *
    * @param node NodeData
    */
   protected void updateMixin(NodeData node)
   {
      NodeData prevData = (NodeData)cache.put(new CacheId(getOwnerId(), node.getIdentifier()), node, true);
      // prevent update NullNodeData
      if (!(prevData instanceof NullNodeData))
      {
         if (prevData != null)
         {
            // do update ACL if needed
            if (prevData.getACL() == null || !prevData.getACL().equals(node.getACL()))
            {
               updateChildsACL(node.getIdentifier(), node.getACL());
            }
         }
         else if (LOG.isDebugEnabled())
         {
            LOG.debug("Previous NodeData not found for mixin update " + node.getQPath().getAsString());
         }
      }
   }

   /**
    * Update Node hierachy in case of same-name siblings reorder.
    * Assumes the new (updated) nodes already putted in the cache. Previous name of updated nodes will be calculated
    * and that node will be deleted (if has same id as the new node). Childs paths will be updated to a new node path.
    *
    * @param node NodeData
    * @param prevNode NodeData
    */
   protected void updateInBuffer(final NodeData node, final NodeData prevNode)
   {
      // I expect that NullNodeData will never update existing NodeData.
      CacheQPath prevKey = new CacheQPath(getOwnerId(), node.getParentIdentifier(), prevNode.getQPath(), ItemType.NODE);
      if (node.getIdentifier().equals(cache.getFromBuffer(prevKey)))
      {
         cache.remove(prevKey);
      }

      // update childs paths if index changed
      int nodeIndex = node.getQPath().getEntries()[node.getQPath().getEntries().length - 1].getIndex();
      int prevNodeIndex = prevNode.getQPath().getEntries()[prevNode.getQPath().getEntries().length - 1].getIndex();
      if (nodeIndex != prevNodeIndex)
      {
         // its a samename reordering
         updateTreePath(prevNode.getQPath(), node.getQPath(), null); // don't change ACL, it's same parent
      }
   }

   /**
    * Check all items in cache - is it descendant of prevRootPath, and update path according newRootPath.
    * 
    * @param prevRootPath
    * @param newRootPath
    * @param acl
    */
   protected void updateTreePath(final QPath prevRootPath, final QPath newRootPath, final AccessControlList acl)
   {
      caller.updateTreePath(prevRootPath, newRootPath, acl);
   }

   /**
    * Apply rename operation on cache. Parent node will be re-added into the cache since
    * parent or name might changing. For other children only item data will be replaced.
    */
   protected void renameItem(final ItemState state, final ItemState lastDelete)
   {
      ItemData data = state.getData();
      ItemData prevData = getFromBufferedCacheById.run(data.getIdentifier());

      if (data.isNode())
      {
         if (state.isPersisted())
         {
            // it is state where name can be changed by rename operation, so re-add node
            removeItem(lastDelete.getData());
            putItem(state.getData());
         }
         else
         {
            // update item data with new name only
            cache.put(new CacheId(getOwnerId(), data.getIdentifier()), data, false);
         }
      }
      else
      {
         PropertyData prop = (PropertyData)data;

         if (prevData != null && !(prevData instanceof NullItemData))
         {
            PropertyData newProp =
               new PersistedPropertyData(prop.getIdentifier(), prop.getQPath(), prop.getParentIdentifier(),
                  prop.getPersistedVersion(), prop.getType(), prop.isMultiValued(),
                  ((PropertyData)prevData).getValues(), new SimplePersistedSize(
                     ((PersistedPropertyData)prevData).getPersistedSize()));
            
            // update item data with new name and old values only
            cache.put(new CacheId(getOwnerId(), newProp.getIdentifier()), newProp, false);
         }
         else
         {
            // remove item to avoid inconsistency in cluster mode since we have not old values
            cache.remove(new CacheId(getOwnerId(), data.getIdentifier()));
         }
      }
   }

   /**
    * Update child Nodes ACLs.
    *
    * @param parentId String - root node id of JCR subtree.
    * @param acl AccessControlList
    */
   protected void updateChildsACL(String parentId, AccessControlList acl)
   {
      caller.updateChildsACL(parentId, acl);
   }

   public void beginTransaction()
   {
      cache.beginTransaction();
   }

   public void commitTransaction()
   {
      cache.commitTransaction();
   }

   public void rollbackTransaction()
   {
      cache.rollbackTransaction();
   }

   /**
    * {@inheritDoc}
    */
   public boolean isTXAware()
   {
      return true;
   }

   /**
    * <li>NOT_MODIFY - node(property) is not added to the parent's list 
    * (no persistent changes performed, cache used as cache)</li>
    * <li>MODIFY - node(property) is added to the parent's list if parent in the cache 
    * (new item is added to persistent, add to list if it is present)</li>
    * <li>FORCE_MODIFY - node(property) is added to the parent's list anyway (when list is read from DB, forcing write)</li>
    */
   private enum ModifyChildOption {
      NOT_MODIFY, MODIFY, FORCE_MODIFY
   }

   /**
    * Allows to commit the cache changes in a dedicated XA Tx in order to avoid potential
    * deadlocks
    */
   private void dedicatedTxCommit()
   {
      commitTransaction.run();
   }

   /**
    * {@inheritDoc}
    */
   public void addReferencedProperties(String identifier, List<PropertyData> refProperties)
   {
      boolean inTransaction = cache.isTransactionActive();
      try
      {
         if (!inTransaction)
         {
            cache.beginTransaction();
         }
         cache.setLocal(true);

         Set<Object> set = new HashSet<Object>();
         for (PropertyData prop : refProperties)
         {
            putProperty(prop, ModifyChildOption.NOT_MODIFY);
            set.add(prop.getIdentifier());
         }
         cache.putIfAbsent(new CacheRefsId(getOwnerId(), identifier), set);
      }
      finally
      {
         cache.setLocal(false);
         if (!inTransaction)
         {
            dedicatedTxCommit();
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public List<PropertyData> getReferencedProperties(String identifier)
   {
      return getReferencedProperties.run(identifier);
   }

   /**
    * {@inheritDoc}
    */
   public void backup(File storageDir) throws BackupException
   {
   }

   /**
    * {@inheritDoc}
    */
   @Managed
   @ManagedDescription("Remove all the existing items from the cache")
   public void clean() throws BackupException
   {
      if (cache.getStatus() == ComponentStatus.RUNNING)
      {
         caller.clearCache();
      }
   }

   /**
    * {@inheritDoc}
    */
   public DataRestore getDataRestorer(DataRestoreContext context) throws BackupException
   {
      return new DataRestore()
      {
         /**
          * {@inheritDoc}
          */
         public void clean() throws BackupException
         {
            caller.clearCache();
         }

         /**
          * {@inheritDoc}
          */
         public void restore() throws BackupException
         {

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
         }

         /**
          * {@inheritDoc}
          */
         public void close() throws BackupException
         {
         }
      };
   }

   /**
    * {@inheritDoc}
    */
   public void addListener(WorkspaceStorageCacheListener listener)
   {
      if (isDistributedMode())
      {
         throw new UnsupportedOperationException("The cache listeners are not supported by the "
            + "ISPNCacheWorkspaceStorageCache in case of the distributed mode");
      }
      listeners.add(listener);
   }

   /**
    * {@inheritDoc}
    */
   public void removeListener(WorkspaceStorageCacheListener listener)
   {
      if (isDistributedMode())
      {
         throw new UnsupportedOperationException("The cache listeners are not supported by the "
            + "ISPNCacheWorkspaceStorageCache in case of the distributed mode");
      }
      listeners.remove(listener);
   }

   /**
    * Called when a cache entry corresponding to the given node has item updated
    * @param data the item corresponding to the updated cache entry
    */
   private void onCacheEntryUpdated(ItemData data)
   {
      if (data == null || data instanceof NullItemData)
      {
         return;
      }
      for (WorkspaceStorageCacheListener listener : listeners)
      {
         try
         {
            listener.onCacheEntryUpdated(data);
         }
         catch (RuntimeException e) //NOSONAR
         {
            LOG.warn("The method onCacheEntryUpdated fails for the listener " + listener.getClass(), e);
         }
      }
   }

   private static void updateTreePath(Cache<CacheKey, Object> cache, String ownerId, ItemData data, QPath prevRootPath,
      QPath newRootPath, AccessControlList acl)
   {
      if (data == null)
      {
         return;
      }

      boolean inheritACL = acl != null;
      // check is this descendant of prevRootPath
      QPath nodeQPath = data.getQPath();
      if (nodeQPath != null && nodeQPath.isDescendantOf(prevRootPath))
      {
         //make relative path
         QPathEntry[] relativePath = null;
         try
         {
            relativePath = nodeQPath.getRelPath(nodeQPath.getDepth() - prevRootPath.getDepth());
         }
         catch (IllegalPathException e)
         {
            // Do nothing. Never happens.
            if (LOG.isTraceEnabled())
            {
               LOG.trace("An exception occurred: " + e.getMessage());
            }
         }

         // make new path - no matter  node or property
         QPath newPath = QPath.makeChildPath(newRootPath, relativePath);

         if (data.isNode())
         {
            // update node
            NodeData prevNode = (NodeData)data;

            PersistedNodeData newNode =
               new PersistedNodeData(prevNode.getIdentifier(), newPath, prevNode.getParentIdentifier(),
                  prevNode.getPersistedVersion(), prevNode.getOrderNumber(), prevNode.getPrimaryTypeName(),
                  prevNode.getMixinTypeNames(), inheritACL ? acl : prevNode.getACL());

            // update this node
            cache.put(new CacheId(ownerId, newNode.getIdentifier()), newNode);
         }
         else
         {
            //update property
            PropertyData prevProp = (PropertyData)data;

            if (inheritACL
               && (prevProp.getQPath().getName().equals(Constants.EXO_PERMISSIONS) || prevProp.getQPath().getName()
                  .equals(Constants.EXO_OWNER)))
            {
               inheritACL = false;
            }

            PersistedPropertyData newProp =
               new PersistedPropertyData(prevProp.getIdentifier(), newPath, prevProp.getParentIdentifier(),
                  prevProp.getPersistedVersion(), prevProp.getType(), prevProp.isMultiValued(), prevProp.getValues(),
                  new SimplePersistedSize(((PersistedPropertyData)prevProp).getPersistedSize()));

            // update this property
            cache.put(new CacheId(ownerId, newProp.getIdentifier()), newProp);
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
   protected abstract class CacheActionNonTxAware<R, A> extends ActionNonTxAware<R, A, RuntimeException>
   {
      /**
       * {@inheritDoc}
       */
      protected TransactionManager getTransactionManager()
      {
         return ISPNCacheWorkspaceStorageCache.this.getTransactionManager();
      }
   }

   @SuppressWarnings("rawtypes")
   @Listener
   public class CacheEventListener
   {

      @CacheEntryModified
      public void cacheEntryModified(CacheEntryModifiedEvent evt)
      {
         if (!evt.isPre() && evt.getKey() instanceof CacheId)
         {
            final ItemData value = (ItemData)evt.getValue();
            onCacheEntryUpdated(value);
         }
      }
   }

   /**
    * This class defines all the methods that could change between the replicated and the distributed mode.
    * By default it implements the methods for the local and replicated mode.
    *
    */
   private class GlobalOperationCaller
   {
      protected int getCacheSize()
      {
         return cache.size();
      }

      protected void clearCache()
      {
         cache.clear();
      }

      /**
       * Update child Nodes ACLs.
       *
       * @param parentId String - root node id of JCR subtree.
       * @param acl AccessControlList
       */
      protected void updateChildsACL(String parentId, AccessControlList acl)
      {
         loop : for (Iterator<NodeData> iter = new ChildNodesIterator<NodeData>(parentId); iter.hasNext();)
         {
            NodeData prevNode = iter.next();

            // is ACL changes on this node (i.e. ACL inheritance brokes)
            boolean hasExoPrivilegeable = false;
            boolean hasExoOwneable = false;
            for (InternalQName mixin : prevNode.getMixinTypeNames())
            {
               if (mixin.equals(Constants.EXO_PRIVILEGEABLE))
               {
                  hasExoPrivilegeable = true;
                  if (hasExoOwneable)
                  {
                     continue loop;
                  }
               }
               else if (mixin.equals(Constants.EXO_OWNEABLE))
               {
                  hasExoOwneable = true;
                  if (hasExoPrivilegeable)
                  {
                     continue loop;
                  }
               }
            }
            AccessControlList newAcl = null;
            if (hasExoOwneable)
            {
               newAcl = new AccessControlList(prevNode.getACL().getOwner(), acl.getPermissionEntries());
            }
            else if (hasExoPrivilegeable)
            {
               newAcl = new AccessControlList(acl.getOwner(), prevNode.getACL().getPermissionEntries());
            }
            if (newAcl != null)
            {
               if (newAcl.equals(prevNode.getACL()))
               {
                  // No need to keep traversing the cache since the acl is the same
                  continue loop;
               }
               acl = newAcl;
            }
            // recreate with new path for child Nodes only
            PersistedNodeData newNode =
               new PersistedNodeData(prevNode.getIdentifier(), prevNode.getQPath(), prevNode.getParentIdentifier(),
                  prevNode.getPersistedVersion(), prevNode.getOrderNumber(), prevNode.getPrimaryTypeName(),
                  prevNode.getMixinTypeNames(), acl);

            // update this node
            cache.put(new CacheId(getOwnerId(), newNode.getIdentifier()), newNode);

            // update childs recursive
            updateChildsACL(newNode.getIdentifier(), acl);
         }
      }

      /**
       * Check all items in cache - is it descendant of prevRootPath, and update path according newRootPath.
       * 
       * @param prevRootPath
       * @param newRootPath
       * @param acl
       */
      protected void updateTreePath(QPath prevRootPath, QPath newRootPath, AccessControlList acl)
      {

         // check all ITEMS in cache 
         Iterator<CacheKey> keys = cache.keySet().iterator();

         while (keys.hasNext())
         {
            CacheKey key = keys.next();
            if (key instanceof CacheId)
            {
               ItemData data = (ItemData)cache.get(key);
               ISPNCacheWorkspaceStorageCache.updateTreePath(cache, getOwnerId(), data, prevRootPath, newRootPath, acl);
            }
         }
      }
   }

   /**
    * This class implements all the global operations for the distributed mode
    *
    */
   private class DistributedOperationCaller extends GlobalOperationCaller
   {

      /**
       * {@inheritDoc}
       */
      @Override
      protected int getCacheSize()
      {
         Map<String, Integer> map = SecurityHelper.doPrivilegedAction(new PrivilegedAction<Map<String, Integer>>()
         {
            public Map<String, Integer> run()
            {
               MapReduceTask<CacheKey, Object, String, Integer> task =
                  new MapReduceTask<CacheKey, Object, String, Integer>(cache);
               task.mappedWith(new GetSizeMapper(getOwnerId())).reducedWith(new GetSizeReducer<String>());
               return task.execute();
            }

         });
         int sum = 0;
         for (Integer i : map.values())
         {
            sum += i;
         }
         return sum;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      protected void clearCache()
      {
         SecurityHelper.doPrivilegedAction(new PrivilegedAction<Void>()
         {
            public Void run()
            {
               MapReduceTask<CacheKey, Object, Void, Void> task =
                  new MapReduceTask<CacheKey, Object, Void, Void>(cache);
               task.mappedWith(new ClearCacheMapper(getOwnerId())).reducedWith(new IdentityReducer());
               task.execute();
               return null;
            }
         });
      }

      /**
       * {@inheritDoc}
       */
      @Override
      protected void updateTreePath(final QPath prevRootPath, final QPath newRootPath, final AccessControlList acl)
      {
         final TransactionManager tm = getTransactionManager();
         if (tm != null)
         {
            try
            {
               // Add the action out of the current transaction to avoid deadlocks
               tm.getTransaction().registerSynchronization(new Synchronization()
               {

                  public void beforeCompletion()
                  {
                  }

                  public void afterCompletion(int status)
                  {
                     if (status == Status.STATUS_COMMITTED)
                     {
                        try
                        {
                           // Since the tx is successfully committed we can call components non tx aware

                           // The listeners will need to be executed outside the current tx so we suspend
                           // the current tx we can face enlistment issues on product like ISPN
                           tm.suspend();
                           _updateTreePath(prevRootPath, newRootPath, acl);
                        }
                        catch (SystemException e)
                        {
                           LOG.warn("Cannot suspend the transaction", e);
                        }
                     }
                  }
               });
               return;
            }
            catch (Exception e) //NOSONAR
            {
               if (LOG.isDebugEnabled())
               {
                  LOG.debug("Cannot register the synchronization to the current transaction in order to update"
                     + " the path out of the transaction", e);
               }
            }
         }
         _updateTreePath(prevRootPath, newRootPath, acl);
      }

      private void _updateTreePath(final QPath prevRootPath, final QPath newRootPath, final AccessControlList acl)
      {
         SecurityHelper.doPrivilegedAction(new PrivilegedAction<Void>()
         {
            public Void run()
            {
               MapReduceTask<CacheKey, Object, Void, Void> task =
                  new MapReduceTask<CacheKey, Object, Void, Void>(cache);
               task.mappedWith(new UpdateTreePathMapper(getOwnerId(), prevRootPath, newRootPath, acl)).reducedWith(
                  new IdentityReducer());
               task.execute();
               return null;
            }
         });
      }

      /**
       * {@inheritDoc}
       */
      @Override
      protected void updateChildsACL(String parentId, final AccessControlList acl)
      {
         ItemData parentItem = get(parentId);
         if (!(parentItem instanceof NodeData))
         {
            return;
         }
         final QPath parentPath = ((NodeData)parentItem).getQPath();
         SecurityHelper.doPrivilegedAction(new PrivilegedAction<Void>()
         {
            public Void run()
            {
               MapReduceTask<CacheKey, Object, Void, Void> task =
                  new MapReduceTask<CacheKey, Object, Void, Void>(cache);
               task.mappedWith(new UpdateChildsACLMapper(getOwnerId(), parentPath, acl)).reducedWith(
                  new IdentityReducer());
               task.execute();
               return null;
            }
         });
      }
   }

   public static class GetSizeMapper extends AbstractMapper<String, Integer>
   {

      public GetSizeMapper()
      {
      }

      public GetSizeMapper(String ownerId)
      {
         super(ownerId);
      }

      /**
       * {@inheritDoc}
       */
      @Override
      protected void _map(CacheKey key, Object value, Collector<String, Integer> collector)
      {
         collector.emit("total", Integer.valueOf(1));
      }
   }

   public static class GetSizeReducer<K> implements Reducer<K, Integer>
   {

      /**
       * The serial version UID
       */
      private static final long serialVersionUID = 7877781449514234007L;

      /**
       * @see org.infinispan.distexec.mapreduce.Reducer#reduce(java.lang.Object, java.util.Iterator)
       */
      public Integer reduce(K reducedKey, Iterator<Integer> iter)
      {
         int sum = 0;
         while (iter.hasNext())
         {
            Integer i = iter.next();
            sum += i;
         }
         return sum;
      }
   }

   public static class ClearCacheMapper extends AbstractMapper<Void, Void>
   {
      public ClearCacheMapper()
      {
      }

      public ClearCacheMapper(String ownerId)
      {
         super(ownerId);
      }

      /**
       * {@inheritDoc}
       */
      @Override
      protected void _map(CacheKey key, Object value, Collector<Void, Void> collector)
      {
         ExoContainer container = ExoContainerContext.getTopContainer();
         if (container == null)
         {
            LOG.error("The top container could not be found");
            return;
         }
         DistributedCacheManager dcm =
            (DistributedCacheManager)container.getComponentInstanceOfType(DistributedCacheManager.class);
         if (dcm == null)
         {
            LOG.error("The DistributedCacheManager could not be found at top container level, please configure it.");
            return;
         }
         Cache<CacheKey, Object> cache = dcm.getCache(CACHE_NAME);
         cache.getAdvancedCache().withFlags(Flag.SKIP_REMOTE_LOOKUP, Flag.FAIL_SILENTLY).remove(key);
      }
   }

   public static class IdentityReducer implements Reducer<Void, Void>
   {

      /**
       * The serial version UID
       */
      private static final long serialVersionUID = -6193360351201912040L;

      /**
       * @see org.infinispan.distexec.mapreduce.Reducer#reduce(java.lang.Object, java.util.Iterator)
       */
      public Void reduce(Void reducedKey, Iterator<Void> iter)
      {
         return null;
      }
   }

   public static class UpdateTreePathMapper extends AbstractMapper<Void, Void>
   {
      private QPath prevRootPath, newRootPath;

      private AccessControlList acl;

      public UpdateTreePathMapper()
      {
      }

      public UpdateTreePathMapper(String ownerId, QPath prevRootPath, QPath newRootPath, AccessControlList acl)
      {
         super(ownerId);
         this.prevRootPath = prevRootPath;
         this.newRootPath = newRootPath;
         this.acl = acl;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      protected boolean isValid(CacheKey key)
      {
         return super.isValid(key) && key instanceof CacheId;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public void writeExternal(ObjectOutput out) throws IOException
      {
         super.writeExternal(out);
         byte[] buf = prevRootPath.getAsString().getBytes(Constants.DEFAULT_ENCODING);
         out.writeInt(buf.length);
         out.write(buf);

         buf = newRootPath.getAsString().getBytes(Constants.DEFAULT_ENCODING);
         out.writeInt(buf.length);
         out.write(buf);

         out.writeBoolean(acl != null);
         if (acl != null)
         {
            acl.writeExternal(out);
         }
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
      {
         super.readExternal(in);
         byte[] buf;

         try
         {
            buf = new byte[in.readInt()];
            in.readFully(buf);
            String sQPath = new String(buf, Constants.DEFAULT_ENCODING);
            prevRootPath = QPath.parse(sQPath);
            buf = new byte[in.readInt()];
            in.readFully(buf);
            sQPath = new String(buf, Constants.DEFAULT_ENCODING);
            newRootPath = QPath.parse(sQPath);
         }
         catch (IllegalPathException e)
         {
            throw new IOException("Deserialization error. ", e);
         }
         if (in.readBoolean())
         {
            this.acl = new AccessControlList();
            acl.readExternal(in);
         }
      }

      /**
       * {@inheritDoc}
       */
      @Override
      protected void _map(CacheKey key, Object value, Collector<Void, Void> collector)
      {
         ExoContainer container = ExoContainerContext.getTopContainer();
         if (container == null)
         {
            LOG.error("The top container could not be found");
            return;
         }
         DistributedCacheManager dcm =
            (DistributedCacheManager)container.getComponentInstanceOfType(DistributedCacheManager.class);
         if (dcm == null)
         {
            LOG.error("The DistributedCacheManager could not be found at top container level, please configure it.");
            return;
         }
         Cache<CacheKey, Object> cache = dcm.getCache(CACHE_NAME);
         ISPNCacheWorkspaceStorageCache.updateTreePath(cache.getAdvancedCache().withFlags(Flag.SKIP_REMOTE_LOOKUP),
            ownerId, (ItemData)value, prevRootPath, newRootPath, acl);
      }
   }

   public static class UpdateChildsACLMapper extends AbstractMapper<Void, Void>
   {
      private QPath parentPath;

      private AccessControlList acl;

      public UpdateChildsACLMapper()
      {
      }

      public UpdateChildsACLMapper(String ownerId, QPath parentPath, AccessControlList acl)
      {
         super(ownerId);
         this.parentPath = parentPath;
         this.acl = acl;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      protected boolean isValid(CacheKey key)
      {
         return super.isValid(key) && key instanceof CacheId;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public void writeExternal(ObjectOutput out) throws IOException
      {
         super.writeExternal(out);
         byte[] buf = parentPath.getAsString().getBytes(Constants.DEFAULT_ENCODING);
         out.writeInt(buf.length);
         out.write(buf);

         out.writeBoolean(acl != null);
         if (acl != null)
         {
            acl.writeExternal(out);
         }
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
      {
         super.readExternal(in);
         byte[] buf;

         try
         {
            buf = new byte[in.readInt()];
            in.readFully(buf);
            String sQPath = new String(buf, Constants.DEFAULT_ENCODING);
            parentPath = QPath.parse(sQPath);
         }
         catch (IllegalPathException e)
         {
            throw new IOException("Deserialization error. ", e);
         }
         if (in.readBoolean())
         {
            this.acl = new AccessControlList();
            acl.readExternal(in);
         }
      }

      /**
       * {@inheritDoc}
       */
      @Override
      protected void _map(CacheKey key, Object value, Collector<Void, Void> collector)
      {
         if (!(value instanceof NodeData))
         {
            return;
         }
         NodeData prevNode = (NodeData)value;
         // check is this descendant of parentPath
         QPath nodeQPath = prevNode.getQPath();
         if (nodeQPath == null || !nodeQPath.isDescendantOf(parentPath))
         {
            return;
         }

         // is ACL changes on this node (i.e. ACL inheritance brokes)
         boolean hasExoPrivilegeable = false;
         boolean hasExoOwneable = false;
         for (InternalQName mixin : prevNode.getMixinTypeNames())
         {
            if (mixin.equals(Constants.EXO_PRIVILEGEABLE))
            {
               hasExoPrivilegeable = true;
               if (hasExoOwneable)
               {
                  return;
               }
            }
            else if (mixin.equals(Constants.EXO_OWNEABLE))
            {
               hasExoOwneable = true;
               if (hasExoPrivilegeable)
               {
                  return;
               }
            }
         }
         ExoContainer container = ExoContainerContext.getTopContainer();
         if (container == null)
         {
            LOG.error("The top container could not be found");
            return;
         }
         DistributedCacheManager dcm =
            (DistributedCacheManager)container.getComponentInstanceOfType(DistributedCacheManager.class);
         if (dcm == null)
         {
            LOG.error("The DistributedCacheManager could not be found at top container level, please configure it.");
            return;
         }
         Cache<CacheKey, Object> cache = dcm.getCache(CACHE_NAME);
         // we force the reloading
         cache.getAdvancedCache().withFlags(Flag.SKIP_REMOTE_LOOKUP, Flag.FAIL_SILENTLY).remove(key);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void start()
   {
   }

   /**
    * {@inheritDoc}
    */
   public void stop()
   {
      cache.stop();
   }

   public static class FakeValueSet extends HashSet<String>
   {
      /**
       * Serial Version UID
       */
      private static final long serialVersionUID = 6163005084471981227L;

      public FakeValueSet() {}

      public FakeValueSet(int size)
      {
         for (int i = 0; i < size; i++)
         {
            // We put only fake ids to store the size and to force a reloading in case getChildNodes
            // is called
            add(Integer.toString(i));
         }
      }
   }
}
