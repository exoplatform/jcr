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
package org.exoplatform.services.jcr.impl.core;

import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.AccessManager;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.nodetype.ItemDefinitionData;
import org.exoplatform.services.jcr.core.security.JCRRuntimePermissions;
import org.exoplatform.services.jcr.dataflow.DataManager;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.SharedDataManager;
import org.exoplatform.services.jcr.datamodel.IllegalPathException;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.itemfilters.QPathEntryFilter;
import org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeManagerImpl;
import org.exoplatform.services.jcr.impl.core.version.ChildVersionRemoveVisitor;
import org.exoplatform.services.jcr.impl.core.version.VersionHistoryImpl;
import org.exoplatform.services.jcr.impl.core.version.VersionImpl;
import org.exoplatform.services.jcr.impl.dataflow.ItemDataMoveVisitor;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.LocalWorkspaceDataManagerStub;
import org.exoplatform.services.jcr.impl.dataflow.session.SessionChangesLog;
import org.exoplatform.services.jcr.impl.dataflow.session.TransactionableDataManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

/**
 * Created by The eXo Platform SAS.<br>
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov </a>
 * @version $Id: SessionDataManager.java 14590 2008-05-22 08:51:29Z pnedonosko $
 */
public class SessionDataManager implements ItemDataConsumer
{

   public static final int MERGE_NODES = 1;

   public static final int MERGE_PROPS = 2;

   public static final int MERGE_ITEMS = 3;

   protected static Log log = ExoLogger.getLogger("exo.jcr.component.core.SessionDataManager");

   protected final SessionImpl session;

   protected final ItemReferencePool itemsPool;

   /**
    * Contains items was deleted but still not saved. i.e. deleted in session. The list will be
    * cleared by each session save call.
    */
   protected final List<ItemImpl> invalidated = new ArrayList<ItemImpl>();

   private final SessionChangesLog changesLog;

   protected final SessionItemFactory itemFactory;

   protected final AccessManager accessManager;

   protected final TransactionableDataManager transactionableManager;

   public SessionDataManager(SessionImpl session, LocalWorkspaceDataManagerStub dataManager) throws RepositoryException
   {
      this.session = session;
      this.changesLog = new SessionChangesLog(session);
      this.itemsPool = new ItemReferencePool();
      this.itemFactory = new SessionItemFactory();
      this.accessManager = session.getAccessManager();
      this.transactionableManager = new TransactionableDataManager(dataManager, session);
   }

   /**
    * @return Returns the workspDataManager.
    */
   public SharedDataManager getWorkspaceDataManager()
   {
      return transactionableManager.getStorageDataManager();
   }

   public String dump()
   {
      String d = "\nChanges:";
      d += changesLog.dump();
      d += "\nCache:";
      d += itemsPool.dump();
      return d;
   }

   /**
    * @return Returns the TransactionableDataManager
    */
   public TransactionableDataManager getTransactManager()
   {
      return transactionableManager;
   }

   /**
    * Return item data by internal <b>qpath</b> in this transient storage then in workspace
    * container.
    * 
    * @param path
    *          - absolute path
    * @return existed item data or null if not found
    * @throws RepositoryException
    * @see org.exoplatform.services.jcr.dataflow.ItemDataConsumer#getItemData(org.exoplatform.services.jcr.datamodel.QPath)
    */
   public ItemData getItemData(QPath path) throws RepositoryException
   {

      NodeData parent = (NodeData)getItemData(Constants.ROOT_UUID);

      if (path.equals(Constants.ROOT_PATH))
      {
         return parent;
      }

      QPathEntry[] relPathEntries = path.getRelPath(path.getDepth());

      return getItemData(parent, relPathEntries, ItemType.UNKNOWN);
   }

   /**
    * Return item data by parent NodeDada and relPathEntries If relpath is JCRPath.THIS_RELPATH = '.'
    * it return itself
    * 
    * @param parent
    * @param relPath
    *          - array of QPathEntry which represents the relation path to the searched item
    * @return existed item data or null if not found
    * @throws RepositoryException
    */
   @Deprecated
   public ItemData getItemData(NodeData parent, QPathEntry[] relPathEntries) throws RepositoryException
   {
      return getItemData(parent, relPathEntries, ItemType.UNKNOWN);
   }

   /**
    * Return item data by parent NodeDada and relPathEntries If relpath is JCRPath.THIS_RELPATH = '.'
    * it return itself
    * 
    * @param parent
    * @param relPath
    *          - array of QPathEntry which represents the relation path to the searched item
    * @param itemType
    *          - item type         
    * @return existed item data or null if not found
    * @throws RepositoryException
    */
   public ItemData getItemData(NodeData parent, QPathEntry[] relPathEntries, ItemType itemType)
      throws RepositoryException
   {
      ItemData item = parent;
      for (int i = 0; i < relPathEntries.length; i++)
      {
         if (i == relPathEntries.length - 1)
         {
            item = getItemData(parent, relPathEntries[i], itemType);
         }
         else
         {
            item = getItemData(parent, relPathEntries[i], ItemType.UNKNOWN);
         }

         if (item == null)
         {
            break;
         }

         if (item.isNode())
         {
            parent = (NodeData)item;
         }
         else if (i < relPathEntries.length - 1)
         {
            throw new IllegalPathException("Path can not contains a property as the intermediate element");
         }
      }
      return item;
   }

   /**
    * {@inheritDoc}
    */
   public ItemData getItemData(NodeData parentData, QPathEntry name) throws RepositoryException
   {
      return getItemData(parentData, name, ItemType.UNKNOWN);
   }

   /**
    * {@inheritDoc}
    */
   public ItemData getItemData(NodeData parent, QPathEntry name, ItemType itemType) throws RepositoryException
   {
      return getItemData(parent, name, false, itemType, true);
   }

   /**
    * {@inheritDoc}
    */
   public ItemData getItemData(NodeData parent, QPathEntry name, ItemType itemType, boolean createNullItemData)
      throws RepositoryException
   {
      return getItemData(parent, name, false, itemType, createNullItemData);
   }


   private ItemData getItemData(NodeData parent, QPathEntry name, boolean skipCheckInPersistence, ItemType itemType,
      boolean createNullItemData)
      throws RepositoryException
   {
      if (name.getName().equals(JCRPath.PARENT_RELPATH) && name.getNamespace().equals(Constants.NS_DEFAULT_URI))
      {
         if (parent.getIdentifier().equals(Constants.ROOT_UUID))
         {
            return null;
         }
         else
         {
            return getItemData(parent.getParentIdentifier());
         }
      }

      ItemData data = null;

      // 1. Try in transient changes
      ItemState state = changesLog.getItemState(parent, name, itemType);
      if (state == null)
      {
         // 2. Check if the parent node is a new node
         if (isNew(parent.getIdentifier()))
         {
            // The parent node is a new node so we know that the data doesn't exist in the database
            return null;
         }
         // 2. Try from txdatamanager
         if (!(skipCheckInPersistence))
         {
            data = transactionableManager.getItemData(parent, name, itemType, createNullItemData);
         }
      }
      else if (!state.isDeleted())
      {
         data = state.getData();
      }
      return data;
   }

   /**
    * Return item data by identifier in this transient storage then in workspace container.
    * 
    * @param identifier
    * @return existed item data or null if not found
    * @throws RepositoryException
    * @see org.exoplatform.services.jcr.dataflow.ItemDataConsumer#getItemData(java.lang.String)
    */
   public ItemData getItemData(String identifier) throws RepositoryException
   {
      ItemData data = null;
      // 1. Try in transient changes
      ItemState state = changesLog.getItemState(identifier);
      if (state == null)
      {
         // 2. Try from txdatamanager
         data = transactionableManager.getItemData(identifier);
      }
      else if (!state.isDeleted())
      {
         data = state.getData();
      }
      return data;
   }

   /**
    * Return Item by parent NodeDada and the name of searched item.
    * 
    * @param parent
    *          - parent of the searched item
    * @param name
    *          - item name
    * @param itemType
    *          - item type
    * @param pool
    *          - indicates does the item fall in pool
    * @return existed item or null if not found
    * @throws RepositoryException
    */
   @Deprecated
   public ItemImpl getItem(NodeData parent, QPathEntry name, boolean pool) throws RepositoryException
   {
      return getItem(parent, name, pool, ItemType.UNKNOWN);
   }

   /**
    * Return Item by parent NodeDada and the name of searched item.
    * 
    * @param parent
    *          - parent of the searched item
    * @param name
    *          - item name
    * @param itemType
    *          - item type
    * @param pool
    *          - indicates does the item fall in pool
    * @return existed item or null if not found
    * @throws RepositoryException
    */
   public ItemImpl getItem(NodeData parent, QPathEntry name, boolean pool, ItemType itemType)
      throws RepositoryException
   {
      return getItem(parent, name, pool, itemType, true);
   }

   /**
    * For internal use, required privileges. Return Item by parent NodeDada and the name of searched item.
    * 
    * @param parent
    *          - parent of the searched item
    * @param name
    *          - item name
    * @param itemType
    *          - item type
    * @param pool
    *          - indicates does the item fall in pool
    * @param apiRead 
    *          - if true will call postRead Action and check permissions              
    * @return existed item or null if not found
    * @throws RepositoryException
    */
   public ItemImpl getItem(NodeData parent, QPathEntry name, boolean pool, ItemType itemType, boolean apiRead)
      throws RepositoryException
   {
      return getItem(parent, name, pool, itemType, apiRead, true);
   }

   /**
    * For internal use. Return Item by parent NodeDada and the name of searched item.
    * 
    * @param parent
    *          - parent of the searched item
    * @param name
    *          - item name
    * @param itemType
    *          - item type
    * @param pool
    *          - indicates does the item fall in pool
    * @param apiRead 
    *          - if true will call postRead Action and check permissions              
    * @param createNullItemData
    *          - defines if there is a need to create NullItemData  
    *          
    * @return existed item or null if not found
    * @throws RepositoryException
    */
   public ItemImpl getItem(NodeData parent, QPathEntry name, boolean pool, ItemType itemType, boolean apiRead,
      boolean createNullItemData)
      throws RepositoryException
   {
      long start = 0;
      if (log.isDebugEnabled())
      {
         start = System.currentTimeMillis();
         log.debug("getItem(" + parent.getQPath().getAsString() + " + " + name.getAsString() + " ) >>>>>");
      }

      ItemImpl item = null;
      try
      {
         return item = readItem(getItemData(parent, name, itemType, createNullItemData), parent, pool, apiRead);
      }
      finally
      {
         if (log.isDebugEnabled())
         {
            log.debug("getItem(" + parent.getQPath().getAsString() + " + " + name.getAsString() + ") --> "
               + (item != null ? item.getPath() : "null") + " <<<<< " + ((System.currentTimeMillis() - start) / 1000d)
               + "sec");
         }
      }
   }

   /**
    * Return Item by parent NodeDada and the name of searched item.
    * 
    * @param parent
    *          - parent of the searched item
    * @param name
    *          - item name
    * @param pool
    *          - indicates does the item fall in pool
    * @param skipCheckInPersistence
    *          - skip getting Item from persistence if need
    * @return existed item or null if not found
    * @throws RepositoryException
    */
   @Deprecated
   public ItemImpl getItem(NodeData parent, QPathEntry name, boolean pool, boolean skipCheckInPersistence)
      throws RepositoryException
   {
      return getItem(parent, name, pool, skipCheckInPersistence, ItemType.UNKNOWN, true);
   }

   public ItemImpl getItem(NodeData parent, QPathEntry name, boolean pool, boolean skipCheckInPersistence,
      ItemType itemType) throws RepositoryException
   {
      return getItem(parent, name, pool, skipCheckInPersistence, itemType, true);
   }

   /**
    * Return Item by parent NodeDada and the name of searched item.
    * 
    * @param parent
    *          - parent of the searched item
    * @param name
    *          - item name
    * @param pool
    *          - indicates does the item fall in pool
    * @param skipCheckInPersistence
    *          - skip getting Item from persistence if need
    * @param itemType
    *          - item type
    * @return existed item or null if not found
    * @throws RepositoryException
    */
   public ItemImpl getItem(NodeData parent, QPathEntry name, boolean pool, boolean skipCheckInPersistence,
      ItemType itemType, boolean createNullItemData) throws RepositoryException
   {
      long start = 0;
      if (log.isDebugEnabled())
      {
         start = System.currentTimeMillis();
         log.debug("getItem(" + parent.getQPath().getAsString() + " + " + name.getAsString() + " ) >>>>>");
      }

      ItemImpl item = null;
      try
      {
         return item =
            readItem(getItemData(parent, name, skipCheckInPersistence, itemType, createNullItemData), parent, pool,
               true);
      }
      finally
      {
         if (log.isDebugEnabled())
         {
            log.debug("getItem(" + parent.getQPath().getAsString() + " + " + name.getAsString() + ") --> "
               + (item != null ? item.getPath() : "null") + " <<<<< " + ((System.currentTimeMillis() - start) / 1000d)
               + "sec");
         }
      }
   }

   /**
    * Return Item by parent NodeDada and array of QPathEntry which represent a relative path to the
    * searched item
    * 
    * @param parent
    *          - parent of the searched item
    * @param relPath
    *          - array of QPathEntry which represents the relation path to the searched item
    * @param pool
    *          - indicates does the item fall in pool
    * @return existed item or null if not found
    * @throws RepositoryException
    */
   @Deprecated
   public ItemImpl getItem(NodeData parent, QPathEntry[] relPath, boolean pool) throws RepositoryException
   {
      return getItem(parent, relPath, pool, ItemType.UNKNOWN);
   }

   /**
    * Return Item by parent NodeDada and array of QPathEntry which represent a relative path to the
    * searched item
    * 
    * @param parent
    *          - parent of the searched item
    * @param relPath
    *          - array of QPathEntry which represents the relation path to the searched item
    * @param pool
    *          - indicates does the item fall in pool
    * @param itemType
    *          - item type         
    * @return existed item or null if not found
    * @throws RepositoryException
    */
   public ItemImpl getItem(NodeData parent, QPathEntry[] relPath, boolean pool, ItemType itemType)
      throws RepositoryException
   {
      long start = 0;
      if (log.isDebugEnabled())
      {
         start = System.currentTimeMillis();
         String debugPath = "";
         for (QPathEntry rp : relPath)
         {
            debugPath += rp.getAsString();
         }
         log.debug("getItem(" + parent.getQPath().getAsString() + " + " + debugPath + " ) >>>>>");
      }

      ItemImpl item = null;
      try
      {
         return item = readItem(getItemData(parent, relPath, itemType), pool);
      }
      finally
      {
         if (log.isDebugEnabled())
         {
            String debugPath = "";
            for (QPathEntry rp : relPath)
            {
               debugPath += rp.getAsString();
            }
            log.debug("getItem(" + parent.getQPath().getAsString() + " + " + debugPath + ") --> "
               + (item != null ? item.getPath() : "null") + " <<<<< " + ((System.currentTimeMillis() - start) / 1000d)
               + "sec");
         }
      }
   }

   /**
    * Return item by absolute path in this transient storage then in workspace container.
    * 
    * @param path
    *          - absolute path to the searched item
    * @param pool
    *          - indicates does the item fall in pool
    * @return existed item or null if not found
    * @throws RepositoryException
    */
   public ItemImpl getItem(QPath path, boolean pool) throws RepositoryException
   {
      long start = 0;
      if (log.isDebugEnabled())
      {
         start = System.currentTimeMillis();
         log.debug("getItem(" + path.getAsString() + " ) >>>>>");
      }

      ItemImpl item = null;
      try
      {
         return item = readItem(getItemData(path), pool);
      }
      finally
      {
         if (log.isDebugEnabled())
         {
            log.debug("getItem(" + path.getAsString() + ") --> " + (item != null ? item.getPath() : "null") + " <<<<< "
               + ((System.currentTimeMillis() - start) / 1000d) + "sec");
         }
      }
   }

   /**
    * Read ItemImpl of given ItemData.
    * Will call postRead Action and check permissions.
    * 
    * @param itemData ItemData
    * @param pool boolean, if true will reload pooled ItemImpl
    * @return ItemImpl
    * @throws RepositoryException if errro occurs
    */
   protected ItemImpl readItem(ItemData itemData, boolean pool) throws RepositoryException
   {
      return readItem(itemData, null, pool, true);
   }

   /**
    * Create or reload pooled ItemImpl with the given ItemData.
    * 
    * @param itemData ItemData, data to create ItemImpl
    * @param parent NodeData, this item parent data, can be null. Not null used for getChildXXX() 
    * @param pool boolean, if true will reload pooled ItemImpl
    * @param apiRead boolean, if true will call postRead Action and check permissions 
    * @return ItemImpl
    * @throws RepositoryException if error occurs
    */
   protected ItemImpl readItem(ItemData itemData, NodeData parent, boolean pool, boolean apiRead)
      throws RepositoryException
   {
      if (!apiRead)
      {
         // Need privileges
         SecurityManager security = System.getSecurityManager();
         if (security != null)
         {
            security.checkPermission(JCRRuntimePermissions.INVOKE_INTERNAL_API_PERMISSION);
         }
      }

      if (itemData != null)
      {
         ItemImpl item;
         ItemImpl pooledItem;
         if (pool && (pooledItem = itemsPool.get(itemData, parent)) != null)
         {
            // use pooled & reloaded
            item = pooledItem;
         }
         else
         {
            // create new
            item = itemFactory.createItem(itemData, parent);
         }

         if (apiRead)
         {
            if (!item.hasPermission(PermissionType.READ))
            {
               throw new AccessDeniedException("Access denied " + itemData.getQPath().getAsString() + " for "
                  + session.getUserID());
            }
            session.getActionHandler().postRead(item);
         }

         return item;
      }
      else
      {
         return null;
      }
   }

   /**
    * Return item by identifier in this transient storage then in workspace container.
    * 
    * @param identifier
    *          - identifier of searched item
    * @param pool
    *          - indicates does the item fall in pool
    * @return existed item data or null if not found
    * @throws RepositoryException
    */
   public ItemImpl getItemByIdentifier(String identifier, boolean pool) throws RepositoryException
   {
      return getItemByIdentifier(identifier, pool, true);
   }

   /**
    * For internal use, required privileges. Return item by identifier in this transient storage then in workspace container.
    * 
    * @param identifier
    *          - identifier of searched item
    * @param pool
    *          - indicates does the item fall in pool
    * @param apiRead 
    *          - if true will call postRead Action and check permissions          
    * @return existed item data or null if not found
    * @throws RepositoryException
    */
   public ItemImpl getItemByIdentifier(String identifier, boolean pool, boolean apiRead) throws RepositoryException
   {
      long start = 0;
      if (log.isDebugEnabled())
      {
         start = System.currentTimeMillis();
         log.debug("getItemByIdentifier(" + identifier + " ) >>>>>");
      }

      ItemImpl item = null;
      try
      {
         return item = readItem(getItemData(identifier), null, pool, apiRead);
      }
      finally
      {
         if (log.isDebugEnabled())
         {
            log.debug("getItemByIdentifier(" + identifier + ") --> " + (item != null ? item.getPath() : "null")
               + "  <<<<< " + ((System.currentTimeMillis() - start) / 1000d) + "sec");
         }
      }
   }

   /**
    * Returns true if this Session holds pending (that is, unsaved) changes; otherwise returns false.
    * 
    * @param path
    *          to the node item
    * @return boolean
    */
   public boolean hasPendingChanges(QPath path)
   {
      return changesLog.getDescendantsChanges(path).size() > 0;
   }

   /**
    * Returns true if the item with <code>identifier</code> is a new item, meaning that it exists
    * only in transient storage on the Session and has not yet been saved. Within a transaction,
    * isNew on an Item may return false (because the item has been saved) even if that Item is not in
    * persistent storage (because the transaction has not yet been committed).
    * 
    * @param identifier
    *          of the item
    * @return boolean
    */
   public boolean isNew(String identifier)
   {

      ItemState lastState = changesLog.getItemState(identifier);

      if (lastState == null || lastState.isDeleted())
      {
         return false;
      }

      return changesLog.getItemState(identifier, ItemState.ADDED) != null;
   }

   /**
    * Returns true if the item with <code>identifier</code> was deleted in this session. Within a transaction,
    * isDelete on an Item may return false (because the item has been saved) even if that Item is not in
    * persistent storage (because the transaction has not yet been committed).
    * 
    * @param identifier
    *          of the item
    * @return boolean, true if the item was deleted
    */
   public boolean isDeleted(String identifier)
   {

      ItemState lastState = changesLog.getItemState(identifier);

      if (lastState != null && lastState.isDeleted())
      {
         return true;
      }

      return false;
   }

   /**
    * Returns true if the item with <code>itemPath</code> was deleted in this session. Within a transaction,
    * isDelete on an Item may return false (because the item has been saved) even if that Item is not in
    * persistent storage (because the transaction has not yet been committed).
    * 
    * @param itemPath QPath, path of the item
    * @return boolean, true if the item was deleted
    */
   public boolean isDeleted(QPath itemPath)
   {

      ItemState lastState = changesLog.getItemState(itemPath);

      if (lastState != null && lastState.isDeleted())
      {
         return true;
      }

      return false;
   }

   /**
    * Returns true if this Item has been saved but has subsequently been modified through the current
    * session and therefore the state of this item as recorded in the session differs from the state
    * of this item as saved. Within a transaction, isModified on an Item may return false (because
    * the Item has been saved since the modification) even if the modification in question is not in
    * persistent storage (because the transaction has not yet been committed).
    * 
    * @param item
    *          ItemData
    * @return boolean
    */
   public boolean isModified(ItemData item)
   {

      if (item.isNode())
      {
         // this node and child changes only
         Collection<ItemState> nodeChanges = changesLog.getLastModifyStates((NodeData)item);
         return nodeChanges.size() > 0;
      }

      List<ItemState> states = changesLog.getItemStates(item.getIdentifier());
      if (states.size() > 0)
      {
         ItemState lastState = states.get(states.size() - 1);
         if (lastState.isAdded() || lastState.isDeleted())
         {
            return false;
         }

         return true;
      }

      return false;
   }

   /**
    * Returns saved only references (allowed by specs).
    * 
    * @param identifier
    *          String
    * @return List of PropertyImpl
    * @throws RepositoryException
    *           if error
    * 
    * @see javax.jcr.Node#getReferences
    */
   public List<PropertyImpl> getReferences(String identifier) throws RepositoryException
   {
      List<PropertyData> refDatas = transactionableManager.getReferencesData(identifier, true);
      List<PropertyImpl> refs = new ArrayList<PropertyImpl>(refDatas.size());
      for (int i = 0, length = refDatas.size(); i < length; i++)
      {
         PropertyData data = refDatas.get(i);
         ItemState state = changesLog.getItemState(data.getIdentifier());
         if (state != null)
         {
            if (state.isDeleted())
            {
               // if the Property was deleted skip it for now
               continue;
            }

            // otherwise use transient data
            data = (PropertyData)state.getData();
         }

         NodeData parent = (NodeData)getItemData(data.getParentIdentifier());
         // if parent exists check for read permissions, otherwise the parent was deleted in another session.
         if (parent != null)
         {
            // skip not permitted
            if (accessManager.hasPermission(parent.getACL(), new String[]{PermissionType.READ}, session.getUserState()
               .getIdentity()))
            {
               PropertyImpl item = (PropertyImpl)readItem(data, parent, true, false);

               refs.add(item);
               session.getActionHandler().postRead(item);
            }
         }
      }
      return refs;
   }

   /**
    * Return list with properties, for the parent node, for which user have access permeations.
    * 
    * @param parent
    *          NodeData
    * @param pool
    *          boolean, if true list of childs will be refreshed in Items pool
    * @return List of NodeImpl
    * @throws RepositoryException
    *           if error occurs
    * @throws AccessDeniedException
    *           if it's no permissions for childs listing
    */
   @Deprecated
   public List<NodeImpl> getChildNodes(NodeData parent, boolean pool) throws RepositoryException, AccessDeniedException
   {

      long start = 0;
      if (log.isDebugEnabled())
      {
         start = System.currentTimeMillis();
         log.debug("getChildNodes(" + parent.getQPath().getAsString() + ") >>>>>");
      }

      try
      {
         // merge data from changesLog with data from txManager
         List<NodeData> nodeDatas = getChildNodesData(parent);
         List<NodeImpl> nodes = new ArrayList<NodeImpl>(nodeDatas.size());

         for (int i = 0, length = nodeDatas.size(); i < length; i++)
         {
            NodeData data = nodeDatas.get(i);
            if (accessManager.hasPermission(data.getACL(), new String[]{PermissionType.READ}, session.getUserState()
               .getIdentity()))
            {
               NodeImpl item = (NodeImpl)readItem(data, parent, pool, false);
               session.getActionHandler().postRead(item);
               nodes.add(item);
            }
         }
         return nodes;
      }
      finally
      {
         if (log.isDebugEnabled())
         {
            log.debug("getChildNodes(" + parent.getQPath().getAsString() + ") <<<<< "
               + ((System.currentTimeMillis() - start) / 1000d) + "sec");
         }
      }
   }

   /**
    * Return list with properties, for the parent node, for which user have access permeations.
    * 
    * @param parent
    *          NodeData
    * @param pool
    *          boolean, if true list of childs will be refreshed in Items pool
    * @return List of PropertyImpl
    * @throws RepositoryException
    *           if error occurs
    * @throws AccessDeniedException
    *           if it's no permissions for childs listing
    */
   @Deprecated
   public List<PropertyImpl> getChildProperties(NodeData parent, boolean pool) throws RepositoryException,
      AccessDeniedException
   {

      long start = 0;
      if (log.isDebugEnabled())
      {
         start = System.currentTimeMillis();
         log.debug("getChildProperties(" + parent.getQPath().getAsString() + ") >>>>>");
      }

      try
      {
         List<PropertyData> propDatas = getChildPropertiesData(parent);
         List<PropertyImpl> props = new ArrayList<PropertyImpl>(propDatas.size());
         for (int i = 0, length = propDatas.size(); i < length; i++)
         {
            PropertyData data = propDatas.get(i);
            if (accessManager.hasPermission(parent.getACL(), new String[]{PermissionType.READ}, session.getUserState()
               .getIdentity()))
            {
               ItemImpl item = readItem(data, parent, pool, false);
               session.getActionHandler().postRead(item);
               props.add((PropertyImpl)item);
            }
         }
         return props;
      }
      finally
      {
         if (log.isDebugEnabled())
         {
            log.debug("getChildProperties(" + parent.getQPath().getAsString() + ") <<<<< "
               + ((System.currentTimeMillis() - start) / 1000d) + "sec");
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean getChildNodesDataByPage(final NodeData parent, int fromOrderNum, int toOrderNum, List<NodeData> childs)
      throws RepositoryException
   {
      long start = 0;
      if (log.isDebugEnabled())
      {
         start = System.currentTimeMillis();
         log.debug("getChildNodesData(" + parent.getQPath().getAsString() + " , itemDataFilter) >>>>>");
      }

      try
      {
         boolean hasNext = false;
         if (!isNew(parent.getIdentifier()))
         {
            hasNext = transactionableManager.getChildNodesDataByPage(parent, fromOrderNum, toOrderNum, childs);
         }

         // merge data
         Collection<ItemState> transientDescendants = changesLog.getLastChildrenStates(parent, true);

         if (!transientDescendants.isEmpty())
         {
            // 2 get ALL persisted descendants
            Map<String, NodeData> descendants = new LinkedHashMap<String, NodeData>();
            for (int i = 0, length = childs.size(); i < length; i++)
            {
               NodeData childNode = childs.get(i);
               descendants.put(childNode.getIdentifier(), childNode);
            }
            
            int minOrderNum = childs.size() != 0 ? childs.get(0).getOrderNumber() : -1;
            int maxOrderNum = childs.size() != 0 ? childs.get(childs.size() - 1).getOrderNumber() : -1;

            // merge data
            for (ItemState state : transientDescendants)
            {
               NodeData data = (NodeData)state.getData();

               // we have only last states, so remove nodes first
               descendants.remove(data.getIdentifier());

               if ((state.isAdded() || state.isRenamed()) && !hasNext)
               {
                  descendants.put(data.getIdentifier(), data);
               }
               else if (state.isMixinChanged() || state.isUpdated())
               {
                  if (minOrderNum <= data.getOrderNumber() && data.getOrderNumber() <= maxOrderNum)
                  {
                     descendants.put(data.getIdentifier(), data);
                  }
               }
            }

            childs.clear();
            childs.addAll(descendants.values());
         }
         
         return hasNext;
      }
      finally
      {
         if (log.isDebugEnabled())
         {
            log.debug("getChildNodesData(" + parent.getQPath().getAsString() + ") <<<<< "
               + ((System.currentTimeMillis() - start) / 1000d) + "sec");
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public List<NodeData> getChildNodesData(NodeData parent) throws RepositoryException
   {
      long start = 0;
      if (log.isDebugEnabled())
      {
         start = System.currentTimeMillis();
         log.debug("getChildNodesData(" + parent.getQPath().getAsString() + ") >>>>>");
      }

      try
      {
         return (List<NodeData>)mergeNodes(parent, transactionableManager);
      }
      finally
      {
         if (log.isDebugEnabled())
         {
            log.debug("getChildNodesData(" + parent.getQPath().getAsString() + ") <<<<< "
               + ((System.currentTimeMillis() - start) / 1000d) + "sec");
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public List<NodeData> getChildNodesData(NodeData parent, List<QPathEntryFilter> patternFilters)
      throws RepositoryException
   {
      long start = 0;
      if (log.isDebugEnabled())
      {
         start = System.currentTimeMillis();
         log.debug("getChildNodesData(" + parent.getQPath().getAsString() + " , itemDataFilter) >>>>>");
      }

      try
      {
         List<NodeData> persistChildNodes =
            (isNew(parent.getIdentifier())) ? new ArrayList<NodeData>() : transactionableManager.getChildNodesData(
               parent, patternFilters);
         return (List<NodeData>)mergeNodes(parent, persistChildNodes);
      }
      finally
      {
         if (log.isDebugEnabled())
         {
            log.debug("getChildNodesData(" + parent.getQPath().getAsString() + ") <<<<< "
               + ((System.currentTimeMillis() - start) / 1000d) + "sec");
         }
      }

   }

   /**
    * {@inheritDoc}
    */
   public int getLastOrderNumber(NodeData parent) throws RepositoryException
   {
      int lastOrderNumber = changesLog.getLastChildOrderNumber(parent.getIdentifier());
      int lastPersistedNodeOrderNumber =
         isNew(parent.getIdentifier()) ? -1 : transactionableManager.getLastOrderNumber(parent);

      return Math.max(lastPersistedNodeOrderNumber, lastOrderNumber);
   }

   /**
    * {@inheritDoc}
    */
   public int getChildNodesCount(NodeData parent) throws RepositoryException
   {
      int childsCount =
         changesLog.getChildNodesCount(parent.getIdentifier())
            + (isNew(parent.getIdentifier()) ? 0 : transactionableManager.getChildNodesCount(parent));
      if (childsCount < 0)
      {
         throw new InvalidItemStateException("Node's child nodes were changed in another Session "
            + parent.getQPath().getAsString());
      }

      return childsCount;
   }

   /**
    * {@inheritDoc}
    */
   public List<PropertyData> getChildPropertiesData(NodeData parent) throws RepositoryException
   {
      long start = 0;
      if (log.isDebugEnabled())
      {
         start = System.currentTimeMillis();
         log.debug("getChildPropertiesData(" + parent.getQPath().getAsString() + ") >>>>>");
      }

      try
      {
         return (List<PropertyData>)mergeProps(parent, false, transactionableManager);
      }
      finally
      {
         if (log.isDebugEnabled())
         {
            log.debug("getChildPropertiesData(" + parent.getQPath().getAsString() + ") <<<<< "
               + ((System.currentTimeMillis() - start) / 1000d) + "sec");
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public List<PropertyData> getChildPropertiesData(NodeData parent, List<QPathEntryFilter> itemDataFilters)
      throws RepositoryException
   {
      long start = 0;
      if (log.isDebugEnabled())
      {
         start = System.currentTimeMillis();
         log.debug("getChildPropertiesData(" + parent.getQPath().getAsString() + ") >>>>>");
      }

      try
      {
         List<PropertyData> childProperties =
            (isNew(parent.getIdentifier())) ? new ArrayList<PropertyData>() : transactionableManager
               .getChildPropertiesData(parent, itemDataFilters);

         return (List<PropertyData>)mergeProps(parent, childProperties, transactionableManager);
      }
      finally
      {
         if (log.isDebugEnabled())
         {
            log.debug("getChildPropertiesData(" + parent.getQPath().getAsString() + ") <<<<< "
               + ((System.currentTimeMillis() - start) / 1000d) + "sec");
         }
      }

   }

   /**
    * {@inheritDoc}
    */
   public List<PropertyData> listChildPropertiesData(NodeData parent) throws RepositoryException
   {
      long start = 0;
      if (log.isDebugEnabled())
      {
         start = System.currentTimeMillis();
         log.debug("listChildPropertiesData(" + parent.getQPath().getAsString() + ") >>>>>");
      }

      try
      {
         return (List<PropertyData>)mergeProps(parent, true, transactionableManager);
      }
      finally
      {
         if (log.isDebugEnabled())
         {
            log.debug("listChildPropertiesData(" + parent.getQPath().getAsString() + ") <<<<< "
               + ((System.currentTimeMillis() - start) / 1000d) + "sec");
         }
      }
   }

   /**
    * Return the ACL of the location. A session pending changes will be searched too. Item path will
    * be traversed from the root node to a last existing item.
    * 
    * @param path
    *          - path of an ACL
    * @return - an item or its parent ancestor ACL
    * @throws RepositoryException
    */
   public AccessControlList getACL(QPath path) throws RepositoryException
   {
      long start = 0;
      if (log.isDebugEnabled())
      {
         start = System.currentTimeMillis();
         log.debug("getACL(" + path.getAsString() + " ) >>>>>");
      }

      try
      {
         NodeData parent = (NodeData)getItemData(Constants.ROOT_UUID);
         if (path.equals(Constants.ROOT_PATH))
         {
            return parent.getACL();
         }

         ItemData item = null;
         QPathEntry[] relPathEntries = path.getRelPath(path.getDepth());
         for (int i = 0; i < relPathEntries.length; i++)
         {
            if (i == relPathEntries.length - 1)
            {
               item = getItemData(parent, relPathEntries[i], ItemType.NODE);
            }
            else
            {
               item = getItemData(parent, relPathEntries[i], ItemType.UNKNOWN);
            }

            if (item == null)
            {
               break;
            }

            if (item.isNode())
            {
               parent = (NodeData)item;
            }
            else if (i < relPathEntries.length - 1)
            {
               throw new IllegalPathException("Get ACL. Path can not contains a property as the intermediate element");
            }
         }

         if (item != null && item.isNode())
         {
            // node ACL
            return ((NodeData)item).getACL();
         }
         else
         {
            // item not found or it's a property - return parent ACL
            return parent.getACL();
         }
      }
      finally
      {
         if (log.isDebugEnabled())
         {
            log.debug("getACL(" + path.getAsString() + ") <<<<< " + ((System.currentTimeMillis() - start) / 1000d)
               + "sec");
         }
      }
   }

   public AccessControlList getACL(NodeData parent, QPathEntry name) throws RepositoryException
   {
      long start = 0;
      if (log.isDebugEnabled())
      {
         start = System.currentTimeMillis();
         log.debug("getACL(" + parent.getQPath().getAsString() + " + " + name.getAsString() + " ) >>>>>");
      }

      try
      {
         ItemData item = getItemData(parent, name, ItemType.NODE);
         if (item != null && item.isNode())
         {
            // node ACL
            return ((NodeData)item).getACL();
         }
         else
         {
            // item not found or it's a property - return parent ACL
            return parent.getACL();
         }
      }
      finally
      {
         if (log.isDebugEnabled())
         {
            log.debug("getACL(" + parent.getQPath().getAsString() + " + " + name.getAsString() + ") <<<<< "
               + ((System.currentTimeMillis() - start) / 1000d) + "sec");
         }
      }
   }

   void reloadPool(ItemData fromItem) throws RepositoryException
   {
      Collection<ItemImpl> pooledItems = itemsPool.getAll();
      for (ItemImpl item : pooledItems)
      {
         if (item != null)
         {
            if (item.getInternalPath().isDescendantOf(fromItem.getQPath())
               || item.getInternalPath().equals(fromItem.getQPath()))
            {
               ItemData ri = getItemData(item.getInternalIdentifier());
               if (ri != null)
               {
                  itemsPool.reload(ri);
               }
               else
               {
                  // the item is invalid, case of version restore - the item from non
                  // current version
                  item.invalidate();
               }

               invalidated.add(item);
            }
         }
      }
   }

   /**
    * Reloads item in pool.
    *
    * @param data
    *          item data
    * @return
    * @throws RepositoryException
    */
   ItemImpl reloadItem(ItemData data) throws RepositoryException
   {
      return itemsPool.reload(data);
   }

   public void rename(NodeData srcData, ItemDataMoveVisitor initializer) throws RepositoryException
   {

      srcData.accept(initializer);

      changesLog.addAll(initializer.getAllStates());

      // reload items pool
      for (ItemState state : initializer.getItemAddStates())
      {
         if (state.isUpdated() || state.isRenamed())
         {
            ItemImpl item = reloadItem(state.getData());
            if (item != null)
            {
               invalidated.add(item);
            }
         }
      }
   }

   /**
    * Traverses all the descendants of incoming item and creates DELETED state for them Adds DELETED
    * incoming state of incoming and descendants to the changes log and removes corresponding items
    * from pool (if any)
    * 
    * @param itemState
    *          - incoming state
    * @throws RepositoryException
    */
   public void delete(ItemData itemData) throws RepositoryException
   {
      delete(itemData, itemData.getQPath(), false);
   }

   public void delete(ItemData itemData, QPath ancestorToSave) throws RepositoryException
   {
      delete(itemData, ancestorToSave, false);
   }

   protected void delete(ItemData itemData, QPath ancestorToSave, boolean isInternall) throws RepositoryException
   {

      List<? extends ItemData> list = mergeList(itemData, transactionableManager, true, MERGE_ITEMS);

      List<ItemState> deletes = new ArrayList<ItemState>();

      boolean fireEvent = !isNew(itemData.getIdentifier());

      NodeTypeManagerImpl ntManager = (NodeTypeManagerImpl)session.getWorkspace().getNodeTypeManager();

      // if node mix:versionable vs will be removed from Item.remove method.
      boolean checkRemoveChildVersionStorages = false;
      if (itemData.isNode())
      {
         checkRemoveChildVersionStorages =
            !session.getWorkspace().getNodeTypesHolder().isNodeType(Constants.NT_VERSIONHISTORY,
               ((NodeData)itemData).getPrimaryTypeName());
      }

      boolean rootAdded = false;
      for (ItemData data : list)
      {
         if (data.equals(itemData))
         {
            rootAdded = true;
         }

         deletes.add(new ItemState(data, ItemState.DELETED, fireEvent, ancestorToSave, isInternall));
         // if subnode contains JCR_VERSIONHISTORY property
         // we should remove version storage manually
         if (checkRemoveChildVersionStorages && !data.isNode()
            && Constants.JCR_VERSIONHISTORY.equals(data.getQPath().getName()))
         {
            try
            {
               PropertyData vhPropertyData = (PropertyData)getItemData(data.getIdentifier());
               removeVersionHistory(new String(vhPropertyData.getValues().get(0).getAsByteArray()), null,
                  ancestorToSave);
            }
            catch (IllegalStateException e)
            {
               throw new RepositoryException(e.getLocalizedMessage(), e);
            }
            catch (IOException e)
            {
               throw new RepositoryException(e.getLocalizedMessage(), e);
            }
         }
         ItemImpl pooled = itemsPool.remove(data.getIdentifier());

         if (pooled != null)
         {
            pooled.invalidate(); // invalidate immediate
            invalidated.add(pooled);
         }
      }

      // 4 add item itself if not added
      if (!rootAdded)
      {
         deletes.add(new ItemState(itemData, ItemState.DELETED, fireEvent, ancestorToSave, isInternall));

         ItemImpl pooled = itemsPool.remove(itemData.getIdentifier());
         if (pooled != null)
         {
            pooled.invalidate(); // invalidate immediate
            invalidated.add(pooled);
         }

         if (log.isDebugEnabled())
         {
            log.debug("deleted top item: " + itemData.getQPath().getAsString());
         }
      }

      // 6 sort items to delete
      Collections.sort(deletes, new PathSorter());

      if (!fireEvent)
      {
         // 7 erase evenFire flag if it's a new item
         changesLog.eraseEventFire(itemData.getIdentifier());
      }

      changesLog.addAll(deletes);
      // log.info(changesLog.dump())
      if (itemData.isNode())
      {
         // 8 reindex same-name siblings
         changesLog.addAll(reindexSameNameSiblings((NodeData)itemData, this));
      }
   }

   /**
    * Check when it's a Node and is versionable will a version history removed. Case of last version
    * in version history.
    * 
    * @throws RepositoryException
    * @throws ConstraintViolationException
    * @throws VersionException
    */
   public void removeVersionHistory(String vhID, QPath containingHistory, QPath ancestorToSave)
      throws RepositoryException, ConstraintViolationException, VersionException
   {

      NodeData vhnode = (NodeData)getItemData(vhID);

      if (vhnode == null)
      {
         ItemState vhState = changesLog.getItemState(vhID);
         if (vhState != null && vhState.isDeleted())
         {
            return;
         }

         throw new RepositoryException("Version history is not found. UUID: " + vhID
            + ". Context item (ancestor to save) " + ancestorToSave.getAsString());
      }

      // mix:versionable
      // we have to be sure that any versionable node somewhere in repository
      // doesn't refers to a VH of the node being deleted.
      RepositoryImpl rep = (RepositoryImpl)session.getRepository();
      for (String wsName : rep.getWorkspaceNames())
      {
         SessionImpl wsSession =
            session.getWorkspace().getName().equals(wsName) ? session : (SessionImpl)rep.getSystemSession(wsName);
         try
         {
            for (PropertyData sref : wsSession.getTransientNodesManager().getReferencesData(vhID, false))
            {
               // Check if this VH isn't referenced from somewhere in workspace
               // or isn't contained in another one as a child history.
               // Ask ALL references incl. properties from version storage.
               if (sref.getQPath().isDescendantOf(Constants.JCR_VERSION_STORAGE_PATH))
               {
                  if (!sref.getQPath().isDescendantOf(vhnode.getQPath())
                     && (containingHistory != null ? !sref.getQPath().isDescendantOf(containingHistory) : true))
                  {
                     // has a reference to the VH in version storage,
                     // it's a REFERENCE property jcr:childVersionHistory of
                     // nt:versionedChild
                     // i.e. this VH is a child history in an another history.
                     // We can't remove this VH now.
                     return;
                  }
               }
               else if (wsSession != session)
               {
                  // has a reference to the VH in traversed workspace,
                  // it's not a version storage, i.e. it's a property of versionable
                  // node somewhere in ws.
                  // We can't remove this VH now.
                  return;
               } // else -- if we has a references in workspace where the VH is being
               // deleted we can remove VH now.
            }
         }
         finally
         {
            if (wsSession != session)
            {
               wsSession.logout();
            }
         }
      }

      // remove child versions from VH (if found)

      ChildVersionRemoveVisitor cvremover =
         new ChildVersionRemoveVisitor(session.getTransientNodesManager(), session.getWorkspace().getNodeTypesHolder(),
            vhnode.getQPath(), ancestorToSave);
      vhnode.accept(cvremover);

      // remove VH
      delete(vhnode, ancestorToSave, true);
   }

   /**
    * Reindex same-name siblings of the node Reindex is actual for remove, move only. If node is
    * added then its index always is a last in list of childs.
    * 
    * @param node
    *          , a node caused reindexing, i.e. deleted or moved node.
    */
   protected List<ItemState> reindexSameNameSiblings(NodeData cause, ItemDataConsumer dataManager)
      throws RepositoryException
   {
      List<ItemState> changes = new ArrayList<ItemState>();

      NodeData parentNodeData = (NodeData)dataManager.getItemData(cause.getParentIdentifier());

      NodeData nextSibling =
         (NodeData)dataManager.getItemData(parentNodeData, new QPathEntry(cause.getQPath().getName(), cause.getQPath()
            .getIndex() + 1), ItemType.NODE);

      String reindexedId = null;
      // repeat till next sibling exists and it's not a caused Node (deleted or moved to) or just
      // reindexed
      while (nextSibling != null && !nextSibling.getIdentifier().equals(cause.getIdentifier())
         && !nextSibling.getIdentifier().equals(reindexedId))
      {
         // update with new index
         QPath siblingPath =
            QPath.makeChildPath(nextSibling.getQPath().makeParentPath(), nextSibling.getQPath().getName(), nextSibling
               .getQPath().getIndex() - 1);

         NodeData reindexed =
            new TransientNodeData(siblingPath, nextSibling.getIdentifier(), nextSibling.getPersistedVersion(),
               nextSibling.getPrimaryTypeName(), nextSibling.getMixinTypeNames(), nextSibling.getOrderNumber(),
               nextSibling.getParentIdentifier(), nextSibling.getACL());

         reindexedId = reindexed.getIdentifier();

         ItemState reindexedState = ItemState.createUpdatedState(reindexed);
         changes.add(reindexedState);

         itemsPool.reload(reindexed);

         // next...
         nextSibling =
            (NodeData)dataManager.getItemData(parentNodeData, new QPathEntry(nextSibling.getQPath().getName(),
               nextSibling.getQPath().getIndex() + 1), ItemType.NODE);
      }

      return changes;
   }

   /**
    * Updates (adds or modifies) item state in the session transient storage.
    * 
    * @param itemState
    *          - the state
    * @param pool
    *          - if true Manager force pooling this State so next calling will returna the same
    *          object Common rule: use pool = true if the Item supposed to be returned by JCR API
    *          (Node.addNode(), Node.setProperty() for ex) (NOTE: independently of pooling the
    *          Manager always return actual Item state)
    * @return
    * @throws RepositoryException
    */
   public ItemImpl update(ItemState itemState, boolean pool) throws RepositoryException
   {
      if (itemState.isDeleted())
      {
         throw new RepositoryException("Illegal state DELETED. Use delete(...) method");
      }

      changesLog.add(itemState);

      return readItem(itemState.getData(), null, pool, false);
   }

   /**
    * Updates (adds or modifies) item state in the session transient storage without creation ItemImpl.
    * 
    * @param itemState
    *          - the state
    * @param pool
    *          - if true Manager force pooling this State so next calling will returna the same
    *          object Common rule: use pool = true if the Item supposed to be returned by JCR API
    *          (Node.addNode(), Node.setProperty() for ex) (NOTE: independently of pooling the
    *          Manager always return actual Item state)
    * @return
    * @throws RepositoryException
    */
   public void updateItemState(ItemState itemState) throws RepositoryException
   {
      if (itemState.isDeleted())
      {
         throw new RepositoryException("Illegal state DELETED. Use delete(...) method");
      }

      changesLog.add(itemState);
   }

   /**
    * Commit changes
    * 
    * @param path
    * @throws RepositoryException
    * @throws AccessDeniedException
    * @throws ReferentialIntegrityException
    * @throws InvalidItemStateException
    */
   public void commit(QPath path) throws RepositoryException, AccessDeniedException, ReferentialIntegrityException,
      InvalidItemStateException, ItemExistsException
   {

      // validate all, throw an exception if validation failed
      validate(path);

      PlainChangesLog cLog = changesLog.pushLog(path);

      if (log.isDebugEnabled())
      {
         log.debug(" ----- commit -------- \n" + cLog.dump());
      }

      try
      {
         transactionableManager.save(cLog);
         invalidated.clear();
      }
      catch (AccessDeniedException e)
      {
         remainChangesBack(cLog);
         throw new AccessDeniedException(e);
      }
      catch (InvalidItemStateException e)
      {
         remainChangesBack(cLog);
         throw new InvalidItemStateException(e);
      }
      catch (ItemExistsException e)
      {
         remainChangesBack(cLog);
         throw new ItemExistsException(e);
      }
      catch (ReferentialIntegrityException e)
      {
         remainChangesBack(cLog);
         throw new ReferentialIntegrityException(e);
      }
      catch (RepositoryException e)
      {
         remainChangesBack(cLog);
         throw new RepositoryException(e);
      }
   }

   /**
    * Save changes log records back in the session changes log.
    * <p>
    * Case of Session.save error.
    * 
    * @param cLog
    */
   private void remainChangesBack(PlainChangesLog cLog)
   {
      changesLog.addAll(cLog.getAllStates());
      if (log.isDebugEnabled())
      {
         log.debug(" ----- rollback ----- \n" + cLog.dump());
      }
   }

   /**
    * Returns all REFERENCE properties that refer to this node.
    * 
    * @see org.exoplatform.services.jcr.dataflow.ItemDataConsumer#getReferencesData(java.lang.String)
    */
   public List<PropertyData> getReferencesData(String identifier, boolean skipVersionStorage)
      throws RepositoryException
   {
      List<PropertyData> persisted = transactionableManager.getReferencesData(identifier, skipVersionStorage);
      List<PropertyData> sessionTransient = new ArrayList<PropertyData>();
      for (PropertyData p : persisted)
      {
         sessionTransient.add(p);
      }
      return sessionTransient;
   }

   /**
    * Validate all user created changes saves like access permeations, mandatory items, value
    * constraint.
    * 
    * @param path
    * @throws RepositoryException
    * @throws AccessDeniedException
    * @throws ReferentialIntegrityException
    */
   private void validate(QPath path) throws RepositoryException, AccessDeniedException, ReferentialIntegrityException
   {

      List<ItemState> changes = changesLog.getAllStates();
      for (ItemState itemState : changes)
      {

         if (itemState.isInternallyCreated())
         {
            // skip internally created
            if (itemState.isMixinChanged())
            {
               // ...except of check of ACL correct size for internally created
               // items.
               // If no permissions in the list throw exception.
               if (itemState.isDescendantOf(path))
               {
                  if (((NodeData)itemState.getData()).getACL().getPermissionsSize() < 1)
                  {
                     throw new RepositoryException("Node " + itemState.getData().getQPath().getAsString()
                        + " has wrong formed ACL.");
                  }
               }
               validateMandatoryItem(itemState);
            }
         }
         else
         {
            if (itemState.isDescendantOf(path))
            {
               validateAccessPermissions(itemState);
               validateMandatoryItem(itemState);
            }

            if (path.isDescendantOf(itemState.getAncestorToSave()))
            {
               throw new ConstraintViolationException(path.getAsString()
                  + " is the same or descendant of either Session.move()'s destination or source node only "
                  + path.getAsString());
            }
         }
      }
   }

   /**
    * Validate ItemState for access permeations
    * 
    * @param changedItem
    * @throws RepositoryException
    * @throws AccessDeniedException
    */
   private void validateAccessPermissions(ItemState changedItem) throws RepositoryException, AccessDeniedException
   {
      if (changedItem.isDeleted())
      {
         validateRemoveAccessPermission(changedItem);
      }
      else if (changedItem.isMixinChanged())
      {
         validateMixinChangedPermission(changedItem);
      }
      else
      {
         NodeData parent = (NodeData)getItemData(changedItem.getData().getParentIdentifier());
         if (parent != null)
         {
            if (changedItem.getData().isNode())
            {
               // add node
               if (changedItem.isAdded())
               {
                  if (!accessManager.hasPermission(parent.getACL(), new String[]{PermissionType.ADD_NODE}, session
                     .getUserState().getIdentity()))
                  {
                     throw new AccessDeniedException("Access denied: ADD_NODE "
                        + changedItem.getData().getQPath().getAsString() + " for: " + session.getUserID()
                        + " item owner " + parent.getACL().getOwner());
                  }
               }
            }
            else if (changedItem.isAdded() || changedItem.isUpdated())
            {
               // add or update property
               if (!accessManager.hasPermission(parent.getACL(), new String[]{PermissionType.SET_PROPERTY}, session
                  .getUserState().getIdentity()))
               {
                  throw new AccessDeniedException("Access denied: SET_PROPERTY "
                     + changedItem.getData().getQPath().getAsString() + " for: " + session.getUserID() + " item owner "
                     + parent.getACL().getOwner());
               }
            }
         } // else - parent not found, deleted in this session or from another
      }
   }

   private void validateRemoveAccessPermission(ItemState changedItem) throws RepositoryException, AccessDeniedException
   {
      NodeData nodeData = null;
      // if changedItem is node - check its ACL, if property - check parent node ACL
      if (changedItem.isNode())
      {
         nodeData = (NodeData)changedItem.getData();
      }
      else
      {
         nodeData = (NodeData)getItemData(changedItem.getData().getParentIdentifier());
         if (nodeData == null)
         {
            return;
         }
      }

      if (!accessManager.hasPermission(nodeData.getACL(), new String[]{PermissionType.REMOVE}, session.getUserState()
         .getIdentity()))
      {
         throw new AccessDeniedException("Access denied: REMOVE " + changedItem.getData().getQPath().getAsString()
            + " for: " + session.getUserID() + " item owner " + nodeData.getACL().getOwner());
      }
   }

   private void validateMixinChangedPermission(ItemState changedItem) throws AccessDeniedException
   {
      if (!accessManager.hasPermission(((NodeData)changedItem.getData()).getACL(), new String[]{
         PermissionType.ADD_NODE, PermissionType.SET_PROPERTY}, session.getUserState().getIdentity()))
      {
         throw new AccessDeniedException("Access denied: ADD_NODE or SET_PROPERTY"
            + changedItem.getData().getQPath().getAsString() + " for: " + session.getUserID() + " item owner "
            + ((NodeData)changedItem.getData()).getACL().getOwner());
      }
   }

   /**
    * Validate ItemState which represents the add node, for it's all mandatory items
    * 
    * @param changedItem
    * @throws ConstraintViolationException
    * @throws AccessDeniedException
    */
   private void validateMandatoryItem(ItemState changedItem) throws ConstraintViolationException, AccessDeniedException
   {
      if (changedItem.getData().isNode() && (changedItem.isAdded() || changedItem.isMixinChanged())
         && !changesLog.getItemState(changedItem.getData().getQPath()).isDeleted())
      {
         // Node not in delete state. It might be a wrong
         if (!changesLog.getItemState(changedItem.getData().getIdentifier()).isDeleted())
         {
            NodeData nData = (NodeData)changedItem.getData();
            try
            {
               validateMandatoryChildren(nData);
            }
            catch (ConstraintViolationException e)
            {
               throw e;
            }
            catch (AccessDeniedException e)
            {
               throw e;
            }
            catch (RepositoryException e)
            {
               log.warn("Unexpected exception. Probable wrong data. Exception message:" + e.getLocalizedMessage());
            }
         }
      }
   }

   public void validateMandatoryChildren(NodeData nData) throws ConstraintViolationException, AccessDeniedException,
      RepositoryException
   {

      Collection<ItemDefinitionData> mandatoryItemDefs =
         session.getWorkspace().getNodeTypesHolder().getManadatoryItemDefs(nData.getPrimaryTypeName(),
            nData.getMixinTypeNames());
      for (ItemDefinitionData itemDefinitionData : mandatoryItemDefs)
      {
         if (getItemData(nData, new QPathEntry(itemDefinitionData.getName(), 0), ItemType.UNKNOWN) == null)
         {
            throw new ConstraintViolationException("Mandatory item " + itemDefinitionData.getName()
               + " not found. Node [" + nData.getQPath().getAsString() + " primary type: "
               + nData.getPrimaryTypeName().getAsString() + "]");
         }

      }
   }

   /**
    * Removes all pending changes of this item
    * 
    * @param item
    * @throws RepositoryException
    */
   void rollback(ItemData item) throws InvalidItemStateException, RepositoryException
   {

      // remove from changes log (Session pending changes)
      PlainChangesLog slog = changesLog.pushLog(item.getQPath());
      SessionChangesLog changes = new SessionChangesLog(slog.getAllStates(), session);

      String exceptions = "";

      for (Iterator<ItemImpl> removedIter = invalidated.iterator(); removedIter.hasNext();)
      {
         ItemImpl removed = removedIter.next();

         QPath removedPath = removed.getLocation().getInternalPath();
         ItemState rstate = changes.getItemState(removedPath);

         if (rstate == null)
         {
            exceptions +=
               "Can't find removed item " + removed.getLocation().getAsString(false) + " in changes for rollback.\n";
            continue;
         }

         if (rstate.isRenamed())
         {
            // find DELETED
            rstate = changes.findItemState(rstate.getData().getIdentifier(), false, new int[]{ItemState.DELETED});
            if (rstate == null)
            {
               exceptions +=
                  "Can't find removed item (of move operation) " + removed.getLocation().getAsString(false)
                     + " in changes for rollback.\n";
               continue;
            }
         }

         NodeData parent = (NodeData)transactionableManager.getItemData(rstate.getData().getParentIdentifier());
         if (parent != null)
         {
            ItemData persisted =
               transactionableManager.getItemData(parent, rstate.getData().getQPath().getEntries()[rstate.getData()
                  .getQPath().getEntries().length - 1], ItemType.getItemType(rstate.getData()));

            if (persisted != null)
            {
               // reload item data
               removed.loadData(persisted);
            }
         } // else it's transient item

         removedIter.remove();
      }

      if (exceptions.length() > 0 && log.isDebugEnabled())
      {
         log.warn(exceptions);
      }
   }

   /**
    * @see javax.jcr.Item#refresh(boolean)
    * @param item ItemData
    * @throws InvalidItemStateException
    * @throws RepositoryException
    */
   void refresh(ItemData item) throws InvalidItemStateException, RepositoryException
   {
      if (!isModified(item) && itemsPool.contains(item.getIdentifier()))
      {
         // if not modified but was pooled, load data from persistent storage
         ItemData persisted = transactionableManager.getItemData(item.getIdentifier());
         if (persisted == null)
         {
            // ...try by path
            NodeData parent = (NodeData)transactionableManager.getItemData(item.getParentIdentifier());
            if (parent != null)
            {
               QPathEntry[] path = item.getQPath().getEntries();
               persisted =
                  transactionableManager.getItemData(parent, path[path.length - 1], ItemType.getItemType(item));
            } // else, the item has an invalid state, will be throwed on save
         }

         if (persisted != null)
         {
            // the item
            itemsPool.reload(item.getIdentifier(), persisted);

            // the childs is acquired in the session.
            for (ItemImpl pooled : itemsPool.getDescendats(persisted.getQPath()))
            {
               persisted = transactionableManager.getItemData(pooled.getInternalIdentifier());
               if (persisted == null)
               {
                  // ...try by path
                  NodeData parent = (NodeData)transactionableManager.getItemData(pooled.getParentIdentifier());
                  if (parent != null)
                  {
                     QPathEntry[] path = pooled.getData().getQPath().getEntries();
                     persisted =
                        transactionableManager.getItemData(parent, path[path.length - 1], ItemType.getItemType(pooled
                           .getData()));
                  } // else, the item has an invalid state, will be throwed on save
               }
               if (persisted != null)
               {
                  itemsPool.reload(pooled.getInternalIdentifier(), persisted);
               }
            }
         }
         else
         {
            throw new InvalidItemStateException(
               "An item is transient only or removed (either by this session or another) "
                  + session.getLocationFactory().createJCRPath(item.getQPath()).getAsString(false));
         }
      }
   }

   // for testing only
   protected ItemReferencePool getItemsPool()
   {
      return this.itemsPool;
   }

   /**
    * @return sessionChangesLog
    */
   protected SessionChangesLog getChangesLog()
   {
      return this.changesLog;
   }

   /**
    * Merges incoming node with changes stored in this log i.e: 1. incoming data still not modified
    * if there are no corresponding changes 2. incoming data is refreshed with corresponding changes
    * if any 3. new data is added from changes 4. if changed data is marked as "deleted" it removes
    * from outgoing list WARN. THIS METHOD HAS SIBLING - mergeList, see below.
    * 
    * @param rootData 
    * @param persistChildNodes persisted child nodes, that will be merged with transient data
    * 
    * @return merged nodes list
    */
   protected List<? extends ItemData> mergeNodes(ItemData rootData, List<NodeData> persistChildNodes)
      throws RepositoryException
   {
      // 1 get all transient descendants
      Collection<ItemState> transientDescendants = changesLog.getLastChildrenStates(rootData, true);

      if (!transientDescendants.isEmpty())
      {
         // 2 get ALL persisted descendants
         Map<String, ItemData> descendants = new LinkedHashMap<String, ItemData>();
         for (int i = 0, length = persistChildNodes.size(); i < length; i++)
         {
            NodeData childNode = persistChildNodes.get(i);
            descendants.put(childNode.getIdentifier(), childNode);
         }

         // merge data
         for (ItemState state : transientDescendants)
         {
            ItemData data = state.getData();
            if (!state.isDeleted())
            {
               descendants.put(data.getIdentifier(), data);
            }
            else
            {
               descendants.remove(data.getIdentifier());
            }
         }
         Collection<ItemData> desc = descendants.values();
         return new ArrayList<ItemData>(desc);
      }
      else
      {
         return persistChildNodes;
      }
   }

   /**
    * Merges incoming node with changes stored in this log i.e: 1. incoming data still not modified
    * if there are no corresponding changes 2. incoming data is refreshed with corresponding changes
    * if any 3. new datas is added from changes 4. if chaged data is marked as "deleted" it removes
    * from outgoing list WARN. THIS METHOD HAS SIBLING - mergeList, see below.
    * 
    * @param rootData
    * @return
    */
   protected List<? extends ItemData> mergeNodes(ItemData rootData, DataManager dataManager) throws RepositoryException
   {
      // 1 get all transient descendants
      Collection<ItemState> transientDescendants = changesLog.getLastChildrenStates(rootData, true);

      if (!transientDescendants.isEmpty())
      {
         // 2 get ALL persisted descendants
         Map<String, ItemData> descendants = new LinkedHashMap<String, ItemData>();
         traverseStoredDescendants(rootData, dataManager, MERGE_NODES, descendants, false, transientDescendants);

         // merge data
         for (ItemState state : transientDescendants)
         {
            ItemData data = state.getData();
            if (!state.isDeleted())
            {
               descendants.put(data.getIdentifier(), data);
            }
            else
            {
               descendants.remove(data.getIdentifier());
            }
         }
         Collection<ItemData> desc = descendants.values();
         return new ArrayList<ItemData>(desc);
      }
      else
      {
         if (isNew(rootData.getIdentifier()))
         {
            return Collections.emptyList();
         }
         return dataManager.getChildNodesData((NodeData)rootData);
      }
   }

   /**
    * Merges incoming property data with changes stored in this log i.e: 1. incoming data still not modified
    * if there are no corresponding changes 2. incoming data is refreshed with corresponding changes
    * if any 3. new datas is added from changes 4. if chaged data is marked as "deleted" it removes
    * from outgoing list WARN. THIS METHOD HAS SIBLING - mergeList, see below.
    * 
    * @param rootData
    * @param listOnly 
    * @return
    */
   protected List<? extends ItemData> mergeProps(ItemData rootData, List<PropertyData> childProperties,
      DataManager dataManager) throws RepositoryException
   {
      // 1 get all transient descendants
      Collection<ItemState> transientDescendants = changesLog.getLastChildrenStates(rootData, false);

      if (!transientDescendants.isEmpty())
      {
         Map<String, ItemData> descendants = new LinkedHashMap<String, ItemData>();
         outer : for (int i = 0, length = childProperties.size(); i < length; i++)
         {
            ItemData childProp = childProperties.get(i);
            for (ItemState transientState : transientDescendants)
            {
               if (!transientState.isNode() && !transientState.isDeleted()
                  && transientState.getData().getQPath().getDepth() == childProp.getQPath().getDepth()
                  && transientState.getData().getQPath().getName().equals(childProp.getQPath().getName()))
               {
                  continue outer;
               }
            }
            descendants.put(childProp.getIdentifier(), childProp);
         }

         // merge data
         for (ItemState state : transientDescendants)
         {
            ItemData data = state.getData();
            if (!state.isDeleted())
            {
               descendants.put(data.getIdentifier(), data);
            }
            else
            {
               descendants.remove(data.getIdentifier());
            }
         }
         Collection<ItemData> desc = descendants.values();
         return new ArrayList<ItemData>(desc);
      }
      else
      {
         return childProperties;
      }
   }

   /**
    * Merges incoming property data with changes stored in this log i.e: 1. incoming data still not modified
    * if there are no corresponding changes 2. incoming data is refreshed with corresponding changes
    * if any 3. new datas is added from changes 4. if chaged data is marked as "deleted" it removes
    * from outgoing list WARN. THIS METHOD HAS SIBLING - mergeList, see below.
    * 
    * @param rootData
    * @param listOnly 
    * @return
    */
   protected List<? extends ItemData> mergeProps(ItemData rootData, boolean listOnly, DataManager dataManager)
      throws RepositoryException
   {
      // 1 get all transient descendants
      Collection<ItemState> transientDescendants = changesLog.getLastChildrenStates(rootData, false);

      if (!transientDescendants.isEmpty())
      {
         // 2 get ALL persisted descendants
         Map<String, ItemData> descendants = new LinkedHashMap<String, ItemData>();
         traverseStoredDescendants(rootData, dataManager, MERGE_PROPS, descendants, listOnly, transientDescendants);

         // merge data
         for (ItemState state : transientDescendants)
         {
            ItemData data = state.getData();
            if (!state.isDeleted())
            {
               descendants.put(data.getIdentifier(), data);
            }
            else
            {
               descendants.remove(data.getIdentifier());
            }
         }
         Collection<ItemData> desc = descendants.values();
         return new ArrayList<ItemData>(desc);
      }
      else
      {
         return dataManager.getChildPropertiesData((NodeData)rootData);
      }
   }

   /**
    * Merge a list of nodes and properties of root data. NOTE. Properties in the list will have empty
    * value data. I.e. for operations not changes properties content. USED FOR DELETE.
    * 
    * @param rootData
    * @param dataManager
    * @param deep
    * @param action
    * @return
    * @throws RepositoryException
    */
   protected List<? extends ItemData> mergeList(ItemData rootData, DataManager dataManager, boolean deep, int action)
      throws RepositoryException
   {

      // 1 get all transient descendants
      List<ItemState> transientDescendants = new ArrayList<ItemState>();
      traverseTransientDescendants(rootData, action, transientDescendants);

      if (deep || !transientDescendants.isEmpty())
      {
         // 2 get ALL persisted descendants
         Map<String, ItemData> descendants = new LinkedHashMap<String, ItemData>();
         traverseStoredDescendants(rootData, dataManager, action, descendants, true, transientDescendants);

         // merge data
         for (ItemState state : transientDescendants)
         {
            ItemData data = state.getData();
            if (!state.isDeleted())
            {
               descendants.put(data.getIdentifier(), data);
            }
            else
            {
               descendants.remove(data.getIdentifier());
            }
         }
         Collection<ItemData> desc = descendants.values();
         List<ItemData> retval;
         if (deep)
         {
            int size = desc.size();
            retval = new ArrayList<ItemData>(size < 10 ? 10 : size);

            for (ItemData itemData : desc)
            {
               retval.add(itemData);
               if (deep)
               {
                  retval.addAll(mergeList(itemData, dataManager, true, action));
               }
            }
         }
         else
         {
            retval = new ArrayList<ItemData>(desc);
         }
         return retval;
      }
      else
      {
         return getStoredDescendants(rootData, dataManager, action);
      }
   }

   /**
    * Calculate all stored descendants for the given parent node
    * 
    * @param parent
    * @param dataManager
    * @param deep
    * @param action
    * @param ret
    * @throws RepositoryException
    */
   private void traverseStoredDescendants(ItemData parent, DataManager dataManager, int action,
      Map<String, ItemData> ret, boolean listOnly, Collection<ItemState> transientDescendants)
      throws RepositoryException
   {

      if (parent.isNode() && !isNew(parent.getIdentifier()))
      {
         if (action != MERGE_PROPS)
         {
            List<NodeData> childNodes = dataManager.getChildNodesData((NodeData)parent);
            for (int i = 0, length = childNodes.size(); i < length; i++)
            {
               NodeData childNode = childNodes.get(i);
               ret.put(childNode.getIdentifier(), childNode);
            }
         }
         if (action != MERGE_NODES)
         {
            List<PropertyData> childProps =
               listOnly ? dataManager.listChildPropertiesData((NodeData)parent) : dataManager
                  .getChildPropertiesData((NodeData)parent);
            outer : for (int i = 0, length = childProps.size(); i < length; i++)
            {
               PropertyData childProp = childProps.get(i);
               for (ItemState transientState : transientDescendants)
               {
                  if (!transientState.isNode() && !transientState.isDeleted()
                     && transientState.getData().getQPath().getDepth() == childProp.getQPath().getDepth()
                     && transientState.getData().getQPath().getName().equals(childProp.getQPath().getName()))
                  {
                     continue outer;
                  }
               }
               ret.put(childProp.getIdentifier(), childProp);
            }
         }
      }
   }

   /**
    * Get all stored descendants for the given parent node
    * 
    * @param parent
    * @param dataManager
    * @param action
    * @throws RepositoryException
    */
   private List<? extends ItemData> getStoredDescendants(ItemData parent, DataManager dataManager, int action)
      throws RepositoryException
   {
      if (parent.isNode())
      {
         List<ItemData> childItems = null;

         List<NodeData> childNodes = dataManager.getChildNodesData((NodeData)parent);
         if (action != MERGE_NODES)
         {
            childItems = new ArrayList<ItemData>(childNodes);
         }
         else
         {
            return childNodes;
         }

         List<PropertyData> childProps = dataManager.getChildPropertiesData((NodeData)parent);
         if (action != MERGE_PROPS)
         {
            childItems.addAll(childProps);
         }
         else
         {
            return childProps;
         }

         return childItems;
      }
      return null;
   }

   /**
    * Calculate all transient descendants for the given parent node
    * 
    * @param parent
    * @param deep
    * @param action
    * @param ret
    * @throws RepositoryException
    */
   private void traverseTransientDescendants(ItemData parent, int action, List<ItemState> ret)
      throws RepositoryException
   {

      if (parent.isNode())
      {
         if (action != MERGE_PROPS)
         {
            Collection<ItemState> childNodes = changesLog.getLastChildrenStates(parent, true);
            for (ItemState childNode : childNodes)
            {
               ret.add(childNode);
            }
         }
         if (action != MERGE_NODES)
         {
            Collection<ItemState> childProps = changesLog.getLastChildrenStates(parent, false);
            for (ItemState childProp : childProps)
            {
               ret.add(childProp);
            }
         }
      }
   }

   /**
   * Pool for touched items.
   */
   protected final class ItemReferencePool
   {

      private WeakHashMap<String, WeakReference<ItemImpl>> items;

      ItemReferencePool()
      {
         items = new WeakHashMap<String, WeakReference<ItemImpl>>();
      }

      ItemImpl remove(String identifier)
      {
         WeakReference<ItemImpl> weakItem = items.remove(identifier);
         return weakItem != null ? weakItem.get() : null;
      }

      Collection<ItemImpl> getAll()
      {
         List<ItemImpl> list = new ArrayList<ItemImpl>();
         for (WeakReference<ItemImpl> weakItem : items.values())
         {
            if (weakItem != null)
            {
               list.add(weakItem.get());
            }
         }

         return list;
      }

      int size()
      {
         return items.size();
      }

      /**
       * @param identifier
       * @return true if item with given identifier is pooled
       * @throws RepositoryException
       */
      boolean contains(String identifier)
      {
         return items.containsKey(identifier);
      }

      /**
       * Get ItemImpl from the pool using given data.   
       * 
       * @param newData ItemData
       * @return ItemImpl item
       * @throws RepositoryException
       */
      ItemImpl get(final ItemData newData) throws RepositoryException
      {
         return get(newData, null);
      }

      /**
       * Get ItemImpl from the pool using given data.   
       * 
       * @param newData ItemData
       * @param parent nodeData
       * @return ItemImpl item
       * @throws RepositoryException
       */
      ItemImpl get(final ItemData newData, final NodeData parent) throws RepositoryException
      {
         final String identifier = newData.getIdentifier();

         WeakReference<ItemImpl> weakItem = items.get(identifier);
         ItemImpl item = weakItem != null ? weakItem.get() : null;

         if (item != null)
         {
            item.loadData(newData, parent);
         }
         else
         {
            item = itemFactory.createItem(newData, parent);
            items.put(item.getInternalIdentifier(), new WeakReference<ItemImpl>(item));
         }
         return item;
      }

      /**
       * Reload an existed item in the pool with given data
       * 
       * @param itemData
       *          - given data
       * @return an existed item of null if no item is pooled with a given data Identifier
       * @throws RepositoryException
       */
      ItemImpl reload(ItemData itemData) throws RepositoryException
      {
         return reload(itemData.getIdentifier(), itemData);
      }

      ItemImpl reload(String identifier, ItemData newItemData) throws RepositoryException
      {
         WeakReference<ItemImpl> weakItem = items.get(identifier);
         ItemImpl item = weakItem != null ? weakItem.get() : null;

         if (item != null)
         {
            item.loadData(newItemData);
            return item;
         }
         return null;
      }

      /**
       * Load nodes ti the pool USED FOR TEST PURPOSE ONLY
       * 
       * @param nodes
       * @return child nodes
       * @throws RepositoryException
       */
      @Deprecated
      List<NodeImpl> getNodes(List<NodeImpl> nodes) throws RepositoryException
      {
         List<NodeImpl> children = new ArrayList<NodeImpl>();
         for (NodeImpl node : nodes)
         {
            String id = node.getInternalIdentifier();

            WeakReference<ItemImpl> weakItem = items.get(id);
            NodeImpl pooled = weakItem != null ? (NodeImpl)weakItem.get() : null;

            if (pooled == null)
            {
               items.put(id, new WeakReference<ItemImpl>(node));
               children.add(node);
            }
            else
            {
               pooled.loadData(node.getData());
               children.add(pooled);
            }
         }
         return children;
      }

      /**
       * Load properties to the pool USED FOR TEST PURPOSE ONLY
       * 
       * @param props
       * @return child properties
       * @throws RepositoryException
       */
      @Deprecated
      List<PropertyImpl> getProperties(List<PropertyImpl> props) throws RepositoryException
      {
         List<PropertyImpl> children = new ArrayList<PropertyImpl>();
         for (PropertyImpl prop : props)
         {
            String id = prop.getInternalIdentifier();

            WeakReference<ItemImpl> weakItem = items.get(id);
            PropertyImpl pooled = weakItem != null ? (PropertyImpl)weakItem.get() : null;

            if (pooled == null)
            {
               items.put(id, new WeakReference<ItemImpl>(prop));
               children.add(prop);
            }
            else
            {
               pooled.loadData(prop.getData());
               children.add(pooled);
            }
         }
         return children;
      }

      /**
       * Search for all descendants of given parent path.
       * 
       * @parentPath parent path
       * @return - List of ItemImpl
       */
      List<ItemImpl> getDescendats(QPath parentPath)
      {
         List<ItemImpl> desc = new ArrayList<ItemImpl>();

         Collection<ItemImpl> snapshort = getAll();
         for (ItemImpl pitem : snapshort)
         {
            if (pitem != null)
            {
               if (pitem.getData().getQPath().isDescendantOf(parentPath))
               {
                  desc.add(pitem);
               }
            }
         }

         return desc;
      }

      String dump()
      {
         String str = "Items Pool: \n";
         try
         {
            for (ItemImpl item : getAll())
            {
               if (item != null)
               {
                  str +=
                     (item.isNode() ? "Node\t\t" : "Property\t") + "\t" + item.isValid() + "\t" + item.isNew() + "\t"
                        + item.getInternalIdentifier() + "\t" + item.getPath() + "\n";
               }
            }
         }
         catch (Exception e)
         {
            log.error(e.getLocalizedMessage(), e);
         }

         return str;
      }
   }

   /**
    * Class creates the Item from ItemData;
    */
   private class SessionItemFactory
   {

      private ItemImpl createItem(ItemData data, NodeData parent) throws RepositoryException
      {

         if (data.isNode())
         {
            return createNode((NodeData)data, parent);
         }
         else
         {
            return createProperty(data, parent);
         }
      }

      private NodeImpl createNode(NodeData data, NodeData parent) throws RepositoryException
      {
         NodeImpl node = new NodeImpl(data, parent, session);
         if (data.getPrimaryTypeName().equals(Constants.NT_VERSION))
         {
            return new VersionImpl(data, session);
         }
         else if (data.getPrimaryTypeName().equals(Constants.NT_VERSIONHISTORY))
         {
            return new VersionHistoryImpl(data, session);
         }
         else
         {
            return node;
         }
      }

      private PropertyImpl createProperty(ItemData data, NodeData parent) throws RepositoryException
      {
         return new PropertyImpl(data, parent, session);
      }
   }

   /**
    * Class helps on to sort nodes on deleting
    */
   private class PathSorter implements Comparator<ItemState>
   {

      public int compare(final ItemState i1, final ItemState i2)
      {
         return -i1.getData().getQPath().compareTo(i2.getData().getQPath());
      }
   }

}
