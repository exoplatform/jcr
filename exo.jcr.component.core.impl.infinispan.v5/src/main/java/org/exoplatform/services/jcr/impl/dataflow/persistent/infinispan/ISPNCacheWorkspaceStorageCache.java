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

import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.config.CacheEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
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
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.infinispan.ISPNCacheFactory;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.transaction.ActionNonTxAware;
import org.infinispan.Cache;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
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
public class ISPNCacheWorkspaceStorageCache implements WorkspaceStorageCache, Backupable
{
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.impl.infinispan.v5.ISPNCacheWorkspaceStorageCache");

   private final boolean enabled;
   
   protected final BufferedISPNCache cache;
   
   /**
    * The list of all the listeners
    */
   private final List<WorkspaceStorageCacheListener> listeners = new CopyOnWriteArrayList<WorkspaceStorageCacheListener>();

   private final CacheActionNonTxAware<Void, Void> commitTransaction = new CacheActionNonTxAware<Void, Void>()
   {
      @Override
      protected Void execute(Void arg) throws RuntimeException
      {
         cache.commitTransaction();
         return null;
      }
   };

   private final CacheActionNonTxAware<ItemData, String> getFromCacheById =
      new CacheActionNonTxAware<ItemData, String>()
      {
         @Override
         protected ItemData execute(String id) throws RuntimeException
         {
            return id == null ? null : (ItemData)cache.get(new CacheId(id));
         }
      };

   private final CacheActionNonTxAware<List<NodeData>, NodeData> getChildNodes =
      new CacheActionNonTxAware<List<NodeData>, NodeData>()
      {
         @Override
         protected List<NodeData> execute(NodeData parent) throws RuntimeException
         {
            // get list of children uuids
            final Set<String> set = (Set<String>)cache.get(new CacheNodesId(parent.getIdentifier()));

            if (set != null)
            {
               final List<NodeData> childs = new ArrayList<NodeData>();

               for (String childId : set)
               {
                  NodeData child = (NodeData)cache.get(new CacheId(childId));
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
         protected ItemData execute(Object... args) throws RuntimeException
         {
            String parentIdentifier = (String)args[0];
            QPathEntry name = (QPathEntry)args[1];
            ItemType itemType = (ItemType)args[2];
            String itemId = null;

            if (itemType == ItemType.UNKNOWN)
            {
               // Try as node first.
               itemId = (String)cache.get(new CacheQPath(parentIdentifier, name, ItemType.NODE));

               if (itemId == null || itemId.equals(NullItemData.NULL_ID))
               {
                  // node with such a name is not found or marked as not-exist, so check the properties
                  String propId = (String)cache.get(new CacheQPath(parentIdentifier, name, ItemType.PROPERTY));
                  if (propId != null)
                  {
                     itemId = propId;
                  }
               }
            }
            else if (itemType == ItemType.NODE)
            {
               itemId = (String)cache.get(new CacheQPath(parentIdentifier, name, ItemType.NODE));;
            }
            else
            {
               itemId = (String)cache.get(new CacheQPath(parentIdentifier, name, ItemType.PROPERTY));;
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
         protected Integer execute(NodeData parent) throws RuntimeException
         {
            Set<String> list = (Set<String>)cache.get(new CacheNodesId(parent.getIdentifier()));
            return list != null ? list.size() : -1;
         }
      };

   private final CacheActionNonTxAware<List<PropertyData>, Object> getChildProps =
      new CacheActionNonTxAware<List<PropertyData>, Object>()
      {
         @Override
         protected List<PropertyData> execute(Object... args) throws RuntimeException
         {
            String parentId = (String)args[0];
            boolean withValue = (Boolean)args[1];
            // get list of children uuids
            final Set<String> set = (Set<String>)cache.get(new CachePropsId(parentId));
            if (set != null)
            {
               final List<PropertyData> childs = new ArrayList<PropertyData>();

               for (String childId : set)
               {
                  PropertyData child = (PropertyData)cache.get(new CacheId(childId));

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
         protected List<PropertyData> execute(String identifier) throws RuntimeException
         {
            // get list of children uuids
            final Set<String> set = (Set<String>)cache.get(new CacheRefsId(identifier));
            if (set != null)
            {
               final List<PropertyData> props = new ArrayList<PropertyData>();

               for (String childId : set)
               {
                  PropertyData prop = (PropertyData)cache.get(new CacheId(childId));

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
                        if (new String(vdata.getAsByteArray(), Constants.DEFAULT_ENCODING).equals(identifier))
                        {
                           props.add(prop);
                        }
                     }
                     catch (IllegalStateException e)
                     {
                        // property was not added, force read from lower layer
                        return null;
                     }
                     catch (IOException e)
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
      protected Long execute(Void arg) throws RuntimeException
      {
         return (long)cache.size();
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

      final Iterator<String> childs;

      T next;

      ChildItemsIterator(CacheKey key)
      {
         Set<String> set = (Set<String>)cache.get(key);
         if (set != null)
         {
            childs = ((Set<String>)cache.get(key)).iterator();
            fetchNext();
         }
         else
         {
            childs = null;
            next = null;
         }
      }

      protected void fetchNext()
      {
         if (childs.hasNext())
         {
            // traverse to the first existing or the end of children
            T n = null;
            do
            {
               n = (T)cache.get(new CacheId(childs.next()));
            }
            while (n == null && childs.hasNext());
            next = n;
         }
         else
         {
            next = null;
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
         super(new CacheNodesId(parentId));
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
         super(new CachePropsId(parentId));
      }

      @Override
      public P next()
      {
         return super.next();
      }
   }

   /**
    * Cache constructor with eXo TransactionService support.
    * 
    * @param wsConfig WorkspaceEntry workspace config
    * @throws RepositoryException if error of initialization
    * @throws RepositoryConfigurationException if error of configuration
    */
   public ISPNCacheWorkspaceStorageCache(WorkspaceEntry wsConfig, ConfigurationManager cfm) throws RepositoryException,
      RepositoryConfigurationException
   {
      if (wsConfig.getCache() == null)
      {
         throw new RepositoryConfigurationException("Cache configuration not found");
      }
      this.enabled = wsConfig.getCache().isEnabled();
      
      // create cache using custom factory
      ISPNCacheFactory<CacheKey, Object> factory = new ISPNCacheFactory<CacheKey, Object>(cfm);

      // create parent Infinispan instance
      CacheEntry cacheEntry = wsConfig.getCache();
      Cache<CacheKey, Object> parentCache = factory.createCache("Data_" + wsConfig.getUniqueName(), cacheEntry);

      Boolean allowLocalChanges = null;
      try
      {
         allowLocalChanges = cacheEntry.getParameterBoolean("allow-local-changes");
      }
      catch (RepositoryConfigurationException e)
      {
         // do n't nothing
      }
      this.cache = new BufferedISPNCache(parentCache, allowLocalChanges);
      cache.addListener(new CacheEventListener());
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
               removeItem(state.getData());
            }
            else if (state.isRenamed())
            {
               putItem(state.getData());
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

         CacheNodesByPageId cacheId = new CacheNodesByPageId(parent.getIdentifier());
         Map<Integer, Set<String>> pages = (Map<Integer, Set<String>>)cache.get(cacheId);
         if (pages == null)
         {
            pages = new HashMap<Integer, Set<String>>();
         }

         if (childs.size() > 0)
         {
            Set<String> set = new HashSet<String>();
            for (NodeData child : childs)
            {
               putNode(child, ModifyChildOption.NOT_MODIFY);
               set.add(child.getIdentifier());
            }

            pages.put(fromOrderNum, set);
            cache.put(cacheId, pages);
         }
         else
         {
            // cache fact of empty childs list
            pages.put(fromOrderNum, new HashSet<String>());
            cache.put(cacheId, pages);
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
         if (childs.size() > 0)
         {
            Set<Object> set = new HashSet<Object>();
            for (NodeData child : childs)
            {
               putNode(child, ModifyChildOption.NOT_MODIFY);
               set.add(child.getIdentifier());
            }
            cache.putIfAbsent(new CacheNodesId(parent.getIdentifier()), set);
         }
         else
         {
            // cache fact of empty childs list
            cache.putIfAbsent(new CacheNodesId(parent.getIdentifier()), new HashSet<Object>());
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

         CachePatternNodesId cacheId = new CachePatternNodesId(parent.getIdentifier());
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
            cache.putIfAbsent(new CachePropsId(parent.getIdentifier()), set);

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

            CachePatternPropsId cacheId = new CachePatternPropsId(parent.getIdentifier());
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
      // TODO not implemented, will force read from DB
   }

   /**
    * {@inheritDoc}
    */
   public ItemData get(String parentId, QPathEntry name)
   {
      return get(parentId, name, ItemType.UNKNOWN);
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
   public List<NodeData> getChildNodesByPage(final NodeData parent, final int fromOrderNum)
   {
      // get list of children uuids
      final Map<Integer, Set<String>> pages =
         (Map<Integer, Set<String>>)cache.get(new CacheNodesByPageId(parent.getIdentifier()));

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
         NodeData child = (NodeData)cache.get(new CacheId(childId));
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
         (Map<QPathEntryFilter, Set<String>>)cache.get(new CachePatternNodesId(parent.getIdentifier()));

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
         NodeData child = (NodeData)cache.get(new CacheId(childId));
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
         (Map<QPathEntryFilter, Set<String>>)cache.get(new CachePatternPropsId(parent.getIdentifier()));

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
         PropertyData child = (PropertyData)cache.get(new CacheId(childId));

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
            cache.putIfAbsent(new CacheQPath(node.getParentIdentifier(), node.getQPath(), ItemType.NODE), node
                     .getIdentifier());
         }
         else
         {
            cache.put(new CacheQPath(node.getParentIdentifier(), node.getQPath(), ItemType.NODE), node.getIdentifier());
         }

         // if MODIFY and List present OR FORCE_MODIFY, then write
         if (modifyListsOfChild != ModifyChildOption.NOT_MODIFY)
         {
            cache.addToPatternList(new CachePatternNodesId(node.getParentIdentifier()), node);
            cache.addToList(new CacheNodesId(node.getParentIdentifier()), node.getIdentifier(),
               modifyListsOfChild == ModifyChildOption.FORCE_MODIFY);

            cache.remove(new CacheNodesByPageId(node.getParentIdentifier()));
         }
      }

      if (modifyListsOfChild == ModifyChildOption.NOT_MODIFY)
      {
         return (ItemData)cache.putIfAbsent(new CacheId(node.getIdentifier()), node);
      }
      else
      {
         return (ItemData)cache.put(new CacheId(node.getIdentifier()), node, true);
      }
   }

   protected ItemData putNodeInBufferedCache(NodeData node, ModifyChildOption modifyListsOfChild)
   {
      if (node.getParentIdentifier() != null)
      {
         cache.put(new CacheQPath(node.getParentIdentifier(), node.getQPath(), ItemType.NODE), node.getIdentifier());

         // if MODIFY and List present OR FORCE_MODIFY, then write
         if (modifyListsOfChild != ModifyChildOption.NOT_MODIFY)
         {
            cache.addToList(new CacheNodesId(node.getParentIdentifier()), node.getIdentifier(),
               modifyListsOfChild == ModifyChildOption.FORCE_MODIFY);
         }
      }

      // NullNodeData must never be returned inside internal cache operations. 
      ItemData itemData = (ItemData)cache.putInBuffer(new CacheId(node.getIdentifier()), node);
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
            cache.putIfAbsent(new CacheId(item.getIdentifier()), item);
         }
         else if (item.getName() != null && item.getParentIdentifier() != null)
         {
            cache.putIfAbsent(new CacheQPath(item.getParentIdentifier(), item.getName(), ItemType.getItemType(item)),
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
         cache.addToPatternList(new CachePatternPropsId(prop.getParentIdentifier()), prop);
         cache.addToList(new CachePropsId(prop.getParentIdentifier()), prop.getIdentifier(),
            modifyListsOfChild == ModifyChildOption.FORCE_MODIFY);
      }
      if (modifyListsOfChild == ModifyChildOption.NOT_MODIFY)
      {
         cache.putIfAbsent(new CacheQPath(prop.getParentIdentifier(), prop.getQPath(), ItemType.PROPERTY), prop.getIdentifier());
      }
      else
      {
         cache.put(new CacheQPath(prop.getParentIdentifier(), prop.getQPath(), ItemType.PROPERTY), prop.getIdentifier());
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
               nodeIdentifier = new String(vdata.getAsByteArray(), Constants.DEFAULT_ENCODING);
            }
            catch (IllegalStateException e)
            {
               // Do nothing. Never happens.
            }
            catch (IOException e)
            {
               // Do nothing. Never happens.
            }
            cache.addToList(new CacheRefsId(nodeIdentifier), prop.getIdentifier(),
               modifyListsOfChild == ModifyChildOption.FORCE_MODIFY);
         }
      }
      // NullItemData must never be returned inside internal cache operations. 
      PropertyData propData;
      if (modifyListsOfChild == ModifyChildOption.NOT_MODIFY)
      {
         propData = (PropertyData)cache.putIfAbsent(new CacheId(prop.getIdentifier()), prop);
      }
      else
      {
         propData = (PropertyData)cache.put(new CacheId(prop.getIdentifier()), prop, true);
      }
      
      return (propData instanceof NullPropertyData) ? null : propData;
   }

   protected void removeItem(ItemData item)
   {
      cache.remove(new CacheId(item.getIdentifier()));
      cache.remove(new CacheQPath(item.getParentIdentifier(), item.getQPath(), ItemType.getItemType(item)));

      if (item.isNode())
      {
         if (item.getParentIdentifier() != null)
         {
            cache.removeFromPatternList(new CachePatternNodesId(item.getParentIdentifier()), item);
            cache.removeFromList(new CacheNodesId(item.getParentIdentifier()), item.getIdentifier());
            cache.remove(new CacheNodesByPageId(item.getParentIdentifier()));
         }

         cache.remove(new CacheNodesId(item.getIdentifier()));
         cache.remove(new CachePropsId(item.getIdentifier()));
         cache.remove(new CacheNodesByPageId(item.getIdentifier()));
         cache.remove(new CachePatternNodesId(item.getIdentifier()));
         cache.remove(new CachePatternPropsId(item.getIdentifier()));
         cache.remove(new CacheRefsId(item.getIdentifier()));
      }
      else
      {
         cache.removeFromPatternList(new CachePatternPropsId(item.getParentIdentifier()), item);
         cache.removeFromList(new CachePropsId(item.getParentIdentifier()), item.getIdentifier());
      }
   }

   /**
    * Update Node's mixin and ACL.
    *
    * @param node NodeData
    */
   protected void updateMixin(NodeData node)
   {
      NodeData prevData = (NodeData)cache.put(new CacheId(node.getIdentifier()), node, true);
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
      CacheQPath prevKey = new CacheQPath(node.getParentIdentifier(), prevNode.getQPath(), ItemType.NODE);
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
      boolean inheritACL = acl != null;

      // check all ITEMS in cache 
      Iterator<CacheKey> keys = cache.keySet().iterator();

      while (keys.hasNext())
      {
         CacheKey key = keys.next();
         if (key instanceof CacheId)
         {
            ItemData data = (ItemData)cache.get(key);

            if (data != null)
            {
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
                  }

                  // make new path - no matter  node or property
                  QPath newPath = QPath.makeChildPath(newRootPath, relativePath);

                  if (data.isNode())
                  {
                     // update node
                     NodeData prevNode = (NodeData)data;

                     TransientNodeData newNode =
                        new TransientNodeData(newPath, prevNode.getIdentifier(), prevNode.getPersistedVersion(),
                           prevNode.getPrimaryTypeName(), prevNode.getMixinTypeNames(), prevNode.getOrderNumber(),
                           prevNode.getParentIdentifier(), inheritACL ? acl : prevNode.getACL());

                     // update this node
                     cache.put(new CacheId(newNode.getIdentifier()), newNode);
                  }
                  else
                  {
                     //update property
                     PropertyData prevProp = (PropertyData)data;

                     if (inheritACL
                        && (prevProp.getQPath().getName().equals(Constants.EXO_PERMISSIONS) || prevProp.getQPath()
                           .getName().equals(Constants.EXO_OWNER)))
                     {
                        inheritACL = false;
                     }

                     TransientPropertyData newProp =
                        new TransientPropertyData(newPath, prevProp.getIdentifier(), prevProp.getPersistedVersion(),
                           prevProp.getType(), prevProp.getParentIdentifier(), prevProp.isMultiValued(), prevProp
                              .getValues());

                     // update this property
                     cache.put(new CacheId(newProp.getIdentifier()), newProp);
                  }
               }
            }
         }
      }
   }

   /**
    * Update child Nodes ACLs.
    *
    * @param parentId String - root node id of JCR subtree.
    * @param acl AccessControlList
    */
   protected void updateChildsACL(final String parentId, final AccessControlList acl)
   {
      loop: for (Iterator<NodeData> iter = new ChildNodesIterator<NodeData>(parentId); iter.hasNext();)
      {
         NodeData prevNode = iter.next();

         // is ACL changes on this node (i.e. ACL inheritance brokes)
         for (InternalQName mixin : prevNode.getMixinTypeNames())
         {
            if (mixin.equals(Constants.EXO_PRIVILEGEABLE) || mixin.equals(Constants.EXO_OWNEABLE))
            {
               continue loop;
            }
         }

         // recreate with new path for child Nodes only
         TransientNodeData newNode =
            new TransientNodeData(prevNode.getQPath(), prevNode.getIdentifier(), prevNode.getPersistedVersion(),
               prevNode.getPrimaryTypeName(), prevNode.getMixinTypeNames(), prevNode.getOrderNumber(), prevNode
                  .getParentIdentifier(), acl);

         // update this node
         cache.put(new CacheId(newNode.getIdentifier()), newNode);

         // update childs recursive
         updateChildsACL(newNode.getIdentifier(), acl);
      }
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
         cache.putIfAbsent(new CacheRefsId(identifier), set);
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
         cache.clear();
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
            cache.clear();
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
      listeners.add(listener);
   }

   /**
    * {@inheritDoc}
    */
   public void removeListener(WorkspaceStorageCacheListener listener)
   {
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
         catch (Exception e)
         {
            LOG.warn("The method onCacheEntryUpdated fails for the listener " + listener.getClass(), e);
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
}
