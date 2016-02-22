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
package org.exoplatform.services.jcr.dataflow;

import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.datamodel.IllegalPathException;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.Constants;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id$
 *          Stores collection of ItemStates
 */
public class PlainChangesLogImpl implements Externalizable, PlainChangesLog
{
   /**
    * Constant null value
    */
   private static final int NULL_VALUE = -1;

   /**
    * Constant not null value
    */
   private static final int NOT_NULL_VALUE = 1;

   /**
    * Constant serial Version UID
    */
   private static final long serialVersionUID = 5624550860372364084L;

   /**
    * Collect of all item 
    */
   protected List<ItemState> items;

   /**
    * Value session id
    */
   protected String sessionId;

   /**
    * Value event type
    */
   protected int eventType;

   /**
    * Value session
    */
   protected ExtendedSession session;

   /**
    * ItemState index storage. Used in getItemState() by id and path.
    */
   protected Map<Object, ItemState> index = new HashMap<Object, ItemState>();

   /**
    * ItemState index storage. Used to store last child nodes states. 
    */
   protected Map<String, Map<String, ItemState>> lastChildNodeStates = new HashMap<String, Map<String, ItemState>>();

   /**
    * ItemState index storage. Used to store last child properties states.  
    */
   protected Map<String, Map<String, ItemState>> lastChildPropertyStates =
      new HashMap<String, Map<String, ItemState>>();

   /**
    * ItemState index storage. Used to store child nodes states.
    */
   protected Map<String, List<ItemState>> childNodeStates = new HashMap<String, List<ItemState>>();

   /**
    * ItemState index storage. Used to store child properties states.
    */
   protected Map<String, List<ItemState>> childPropertyStates = new HashMap<String, List<ItemState>>();

   /**
    * Stores info for persisted child nodes by parent identifier. 
    * <br>Index in array points to: 
    * <br>0 - child nodes count. 
    * <br>1 - last child order number 
    */
   protected Map<String, int[]> childNodesInfo = new HashMap<String, int[]>();

   /**
    * The list of states corresponding to path changed
    */
   protected List<ItemState> allPathsChanged;

   /** 
    * Index in <code>childNodesInfo</code> value array to store child nodes count.
   */
   protected final int CHILD_NODES_COUNT = 0;

   /** 
    * Index in <code>childNodesInfo</code> value array to store last child order number.
    */
   protected final int CHILD_NODES_LAST_ORDER_NUMBER = 1;

   /**
    * Identifier of system and non-system logs pair. Null if no pair found. 
    */
   protected String pairId;

   /**
    * Constructor.
    * 
    * @param items List of ItemState
    * @param session Session 
    * @param eventType int
    */
   public PlainChangesLogImpl(List<ItemState> items, ExtendedSession session, int eventType)
   {
      this(items, session.getId(), eventType, null, session);
   }

   /**
    * Constructor.
    * 
    * @param items List of ItemState
    * @param sessionId String 
    * @param eventType int
    */
   public PlainChangesLogImpl(List<ItemState> items, String sessionId, int eventType)
   {
      this(items, sessionId, eventType, null, null);
   }

   /**
    * Constructor with undefined event type.
    * 
    * @param items List of ItemState
    * @param session Session 
    */
   public PlainChangesLogImpl(List<ItemState> items, ExtendedSession session)
   {
      this(items, session, -1);
   }

   /**
    * PlainChangesLogImpl constructor with an empty log.
    * 
    * @param session Session 
    */
   public PlainChangesLogImpl(ExtendedSession session)
   {
      this(new ArrayList<ItemState>(), session);
   }

   /**
    * PlainChangesLogImpl constructor with an empty log.
    * 
    * @param sessionId String
    */
   public PlainChangesLogImpl(String sessionId)
   {
      this(new ArrayList<ItemState>(), sessionId, -1);
   }

   /**
    * Default PlainChangesLogImpl constructor with an empty log. constructor (for externalizable mainly)
    */
   public PlainChangesLogImpl()
   {
      this(new ArrayList<ItemState>(), (String)null, -1);
   }

   /**
    * {@inheritDoc}
    */
   public List<ItemState> getAllStates()
   {
      return items;
   }

   /**
    * {@inheritDoc}
    */
   public int getSize()
   {
      return items.size();
   }

   /**
    * {@inheritDoc}
    */
   public int getEventType()
   {
      return eventType;
   }

   /**
    * {@inheritDoc}
    */
   public String getSessionId()
   {
      return sessionId;
   }

   /**
    * {@inheritDoc}
    */
   public ExtendedSession getSession()
   {
      return session;
   }

   /**
    * {@inheritDoc}
    */
   public PlainChangesLog add(ItemState change)
   {
      items.add(change);
      addItem(change);

      return this;
   }

   /**
    * {@inheritDoc}
    */
   public PlainChangesLog addAll(List<ItemState> changes)
   {
      items.addAll(changes);
      for (int i = 0, length = changes.size(); i < length; i++)
      {
         addItem(changes.get(i));
      }

      return this;
   }

   /**
    * {@inheritDoc}
    */
   public void clear()
   {
      items.clear();
      index.clear();
      lastChildNodeStates.clear();
      lastChildPropertyStates.clear();
      childNodeStates.clear();
      childPropertyStates.clear();
      childNodesInfo.clear();
      allPathsChanged = null;
   }

   /**
    * {@inheritDoc}
    */
   public String getPairId()
   {
      return pairId;
   }

   /**
    * {@inheritDoc}
    */
   public String dump()
   {
      StringBuilder str = new StringBuilder("ChangesLog: \n");
      for (int i = 0; i < items.size(); i++)
      {
         str.append(" ").append(ItemState.nameFromValue(items.get(i).getState())).append("\t")
            .append(items.get(i).getData().getIdentifier());
         str.append("\t").append("isPersisted=").append(items.get(i).isPersisted()).append("\t").append("isEventFire=");
         str.append(items.get(i).isEventFire()).append("\t").append("isInternallyCreated=")
            .append(items.get(i).isInternallyCreated()).append("\t");
         str.append(items.get(i).getData().getQPath().getAsString()).append("\n");
      }

      return str.toString();
   }

   /**
    * Full qualified constructor.
    * 
    * @param items List of ItemState
    * @param sessionId String 
    * @param eventType int
    * @param pairId String
    */
   protected PlainChangesLogImpl(List<ItemState> items, String sessionId, int eventType, String pairId,
      ExtendedSession session)
   {
      this.session = session;
      this.sessionId = sessionId;
      this.eventType = eventType;
      this.pairId = pairId;
      this.items = new ArrayList<ItemState>();
      addAll(items);
   }

   /**
    * Creates a new instance of {@link PlainChangesLogImpl} by copying metadata from originalLog 
    * instance with Items provided.
    * 
    * @param items
    * @param originalLog
    * @return
    */
   public static PlainChangesLogImpl createCopy(List<ItemState> items, PlainChangesLog originalLog)
   {
      return createCopy(items, originalLog.getPairId(), originalLog);
   }

   /**
    * Creates a new instance of {@link PlainChangesLogImpl} by copying metadata from originalLog 
    * instance with Items and PairID provided. Metadata will be copied excluding PairID.
    * 
    * @param items
    * @param originalLog
    * @return
    */
   public static PlainChangesLogImpl createCopy(List<ItemState> items, String pairId, PlainChangesLog originalLog)
   {
      if (originalLog.getSession() != null)
      {
         return new PlainChangesLogImpl(items, originalLog.getSession().getId(), originalLog.getEventType(), pairId,
            originalLog.getSession());
      }
      return new PlainChangesLogImpl(items, originalLog.getSessionId(), originalLog.getEventType(), pairId, null);
   }
   
   /**
    * Removes the property or node and all descendants from the log
    * 
    * @param item
    *          item
    */
   public void remove(ItemState item)
   {
      if (item.isNode())
      {
         remove(item.getData().getQPath());
      }
      else 
      {
         removeProperty(item, -1);
      }
   }

   /**
    * Adds item to the changes log.
    * 
    * @param item
    *          the item
    */
   protected void addItem(ItemState item)
   {
      index.put(item.getData().getIdentifier(), item);
      index.put(item.getData().getQPath(), item);
      index.put(new ParentIDQPathBasedKey(item), item);
      index.put(new IDStateBasedKey(item.getData().getIdentifier(), item.getState()), item);

      if (item.getData().isNode())
      {
         Map<String, ItemState> children = lastChildNodeStates.get(item.getData().getParentIdentifier());
         if (children == null)
         {
            children = new HashMap<String, ItemState>();
            lastChildNodeStates.put(item.getData().getParentIdentifier(), children);
         }
         children.put(item.getData().getIdentifier(), item);

         List<ItemState> listItemState = childNodeStates.get(item.getData().getParentIdentifier());
         if (listItemState == null)
         {
            listItemState = new ArrayList<ItemState>();
            childNodeStates.put(item.getData().getParentIdentifier(), listItemState);
         }
         listItemState.add(item);
         if (item.isPathChanged())
         {
            if (allPathsChanged == null)
            {
               allPathsChanged = new ArrayList<ItemState>();
            }
            allPathsChanged.add(item);
         }
      }
      else
      {
         Map<String, ItemState> children = lastChildPropertyStates.get(item.getData().getParentIdentifier());
         if (children == null)
         {
            children = new HashMap<String, ItemState>();
            lastChildPropertyStates.put(item.getData().getParentIdentifier(), children);
         }
         children.put(item.getData().getIdentifier(), item);

         List<ItemState> listItemState = childPropertyStates.get(item.getData().getParentIdentifier());
         if (listItemState == null)
         {
            listItemState = new ArrayList<ItemState>();
            childPropertyStates.put(item.getData().getParentIdentifier(), listItemState);
         }
         listItemState.add(item);
      }

      if (item.isNode() && item.isPersisted())
      {
         int[] childInfo = childNodesInfo.get(item.getData().getParentIdentifier());
         if (childInfo == null)
         {
            childInfo = new int[2];
         }

         if (item.isDeleted())
         {
            --childInfo[CHILD_NODES_COUNT];
         }
         else if (item.isAdded())
         {
            ++childInfo[CHILD_NODES_COUNT];
            childInfo[CHILD_NODES_LAST_ORDER_NUMBER] = ((NodeData)item.getData()).getOrderNumber();

         }
         childNodesInfo.put(item.getData().getParentIdentifier(), childInfo);
      }
   }

   /**
    * @return the allPathsChanged
    */
   public List<ItemState> getAllPathsChanged()
   {
      return allPathsChanged;
   }

   public int getChildNodesCount(String rootIdentifier)
   {
      int[] childInfo = childNodesInfo.get(rootIdentifier);
      return childInfo == null ? 0 : childInfo[CHILD_NODES_COUNT];
   }

   public int getLastChildOrderNumber(String rootIdentifier)
   {
      int[] childInfo = childNodesInfo.get(rootIdentifier);
      return childInfo == null ? -1 : childInfo[CHILD_NODES_LAST_ORDER_NUMBER];
   }

   /**
    * Removes the item at the rootPath and all descendants from the log
    * 
    * @param rootPath
    *          path
    */
   public void remove(QPath rootPath)
   {
      for (int i = items.size() - 1; i >= 0; i--)
      {
         ItemState item = items.get(i);

         QPath qPath = item.getData().getQPath();
         if (qPath.isDescendantOf(rootPath) || item.getAncestorToSave().isDescendantOf(rootPath)
            || item.getAncestorToSave().equals(rootPath) || qPath.equals(rootPath))
         {
            
            if (item.isNode())
            {
               removeNode(item, i);
            }
            else
            {
               removeProperty(item, i);
            }
         }
      }
   }
   
   /**
    * Removes the node from the log
    * 
    * @param item
    *          ItemState
    */
   private void removeNode(ItemState item, int indexItem)
   {

      items.remove(indexItem);
      index.remove(item.getData().getIdentifier());
      index.remove(item.getData().getQPath());
      index.remove(new ParentIDQPathBasedKey(item));
      index.remove(new IDStateBasedKey(item.getData().getIdentifier(), item.getState()));
      childNodesInfo.remove(item.getData().getIdentifier());
      lastChildNodeStates.remove(item.getData().getIdentifier());
      childNodeStates.remove(item.getData().getIdentifier());
      if (allPathsChanged != null && item.isPathChanged())
      {
         allPathsChanged.remove(item);
         if (allPathsChanged.isEmpty())
            allPathsChanged = null;
      }

      if (item.isPersisted())
      {

         int childInfo[] = childNodesInfo.get(item.getData().getParentIdentifier());

         if (childInfo != null)
         {
            if (item.isDeleted())
            {
               ++childInfo[CHILD_NODES_COUNT];
            }
            else if (item.isAdded())
            {
               --childInfo[CHILD_NODES_COUNT];
            }

            childNodesInfo.put(item.getData().getParentIdentifier(), childInfo);
         }
      }

      Map<String, ItemState> children = lastChildNodeStates.get(item.getData().getParentIdentifier());
      if (children != null)
      {
         children.remove(item.getData().getIdentifier());
         if (children.isEmpty())
         {
            lastChildNodeStates.remove(item.getData().getParentIdentifier());
         }
      }

      List<ItemState> listItemStates = childNodeStates.get(item.getData().getParentIdentifier());
      if (listItemStates != null)
      {
         listItemStates.remove(item);
         if (listItemStates.isEmpty())
         {
            childNodeStates.remove(item.getData().getParentIdentifier());
         }
      }
      if ((children == null || children.isEmpty()) && (listItemStates == null || listItemStates.isEmpty()))
      {
         childNodesInfo.remove(item.getData().getParentIdentifier());
      }
   }
   
   /**
    * Removes the property from the log
    * 
    * @param item
    *          ItemState
    */
   private void removeProperty(ItemState item, int indexItem)
   {
      if (indexItem == -1)
      {
         items.remove(item);
      }
      else
      {
         items.remove(indexItem);
      }
      index.remove(item.getData().getIdentifier());
      index.remove(item.getData().getQPath());
      index.remove(new ParentIDQPathBasedKey(item));
      index.remove(new IDStateBasedKey(item.getData().getIdentifier(), item.getState()));
      lastChildPropertyStates.remove(item.getData().getIdentifier());
      childPropertyStates.remove(item.getData().getIdentifier());

      Map<String, ItemState> children = lastChildPropertyStates.get(item.getData().getParentIdentifier());
      if (children != null)
      {
         children.remove(item.getData().getIdentifier());
         if (children.isEmpty())
         {
            lastChildPropertyStates.remove(item.getData().getParentIdentifier());
         }
      }

      List<ItemState> listItemStates = childPropertyStates.get(item.getData().getParentIdentifier());
      if (listItemStates != null)
      {
         listItemStates.remove(item);
         if (listItemStates.isEmpty())
         {
            childPropertyStates.remove(item.getData().getParentIdentifier());
         }
      }
   }

   /**
    * Collect last in ChangesLog order item child changes.
    * 
    * @param rootData
    *          - a item root of the changes scan
    * @param forNodes
    *          retrieves nodes' ItemStates is true, or properties' otherwise
    * @return child items states
    */
   public Collection<ItemState> getLastChildrenStates(ItemData rootData, boolean forNodes)
   {
      Map<String, ItemState> children =
         forNodes ? lastChildNodeStates.get(rootData.getIdentifier()) : lastChildPropertyStates.get(rootData
            .getIdentifier());

      return children == null ? new ArrayList<ItemState>() : children.values();
   }

   /**
    * Return the last item state from ChangesLog.
    *  
    * @param item 
    *          an item data the last state which need to be taken
    * @param forNode 
    *          retrieves nodes' ItemStates is true, or properties' otherwise
    * @return the last item state
    */
   public ItemState getLastState(ItemData item, boolean forNode)
   {
      Map<String, ItemState> children =
         forNode ? lastChildNodeStates.get(item.getParentIdentifier()) : lastChildPropertyStates.get(item
            .getParentIdentifier());

      return children == null ? null : children.get(item.getIdentifier());
   }
   /**
    * Collect changes of all item direct childs. Including the item itself.
    * @param rootIdentifier root identifier
    * @param forNodes must be returned nodes or properties
    * @return Collect changes of all item direct childs
    */
   public List<ItemState> getChildrenChanges(String rootIdentifier, boolean forNodes)
   {
      List<ItemState> children =
         forNodes ? childNodeStates.get(rootIdentifier) : childPropertyStates.get(rootIdentifier);

      return children == null ? new ArrayList<ItemState>() : children;
   }

   /**
    * Get ItemState by identifier and state.
    * 
    * NOTE: Uses index HashMap.
    * 
    * @param itemIdentifier
    * @param state
    * @return
    */
   public ItemState getItemState(String itemIdentifier, int state)
   {
      return index.get(new IDStateBasedKey(itemIdentifier, state));
   }

   /**
    * Get ItemState by identifier
    * @param itemIdentifier
    * @return
    */
   public ItemState getItemState(String itemIdentifier)
   {
      return index.get(itemIdentifier);
   }

   /**
    * Get ItemState by absolute path.
    * 
    * NOTE: Uses index HashMap.
    * 
    * @param itemPath
    * @return
    */
   public ItemState getItemState(QPath itemPath)
   {
      return index.get(itemPath);
   }

   /**
    * Get ItemState by parent and item name.
    * 
    * @param parentData
    *          parent
    * @param name
    *          item name
    * @param itemType
    *          item type
    * @return
    * @throws IllegalPathException
    */
   public ItemState getItemState(NodeData parentData, QPathEntry name, ItemType itemType) throws IllegalPathException
   {
      if (itemType != ItemType.UNKNOWN)
      {
         return index.get(new ParentIDQPathBasedKey(parentData.getIdentifier(), name, itemType));
      }
      else
      {
         ItemState state = index.get(new ParentIDQPathBasedKey(parentData.getIdentifier(), name, ItemType.NODE));
         if (state == null)
         {
            state = index.get(new ParentIDQPathBasedKey(parentData.getIdentifier(), name, ItemType.PROPERTY));
         }
         return state;
      }
   }

   /**
    * @param rootPath
    * @return item state at the rootPath and its descendants
    */
   public List<ItemState> getDescendantsChanges(QPath rootPath)
   {
      List<ItemState> list = new ArrayList<ItemState>();
      if (rootPath.equals(Constants.ROOT_PATH))
      {
         list.addAll(items);
      }
      else
      {
         for (int i = 0, length = items.size(); i < length; i++)
         {
            ItemState item = items.get(i);
            if (item.isDescendantOf(rootPath))
            {
               list.add(item);
            }
         }
      }

      return list;
   }

   /**
    * Gets items by identifier.
    *
    * @param itemIdentifier
    * @return
    */
   public List<ItemState> getItemStates(String itemIdentifier)
   {
      List<ItemState> states = new ArrayList<ItemState>();
      List<ItemState> currentStates = getAllStates();
      for (int i = 0, length = currentStates.size(); i < length; i++)
      {
         ItemState state = currentStates.get(i);
         if (state.getData().getIdentifier().equals(itemIdentifier))
         {
            states.add(state);
         }
      }
      return states;
   }

   /**
    * Collect last in ChangesLog order node (and direct childs) changes.
    * 
    * @param rootData
    *          - a item root of the changes scan
    * @return this item (!) and child items last modify states (i.e. updates, not adds or deletes)
    */
   public Collection<ItemState> getLastModifyStates(NodeData rootData)
   {
      HashMap<String, ItemState> changes = new HashMap<String, ItemState>();

      for (int i = 0; i < items.size(); i++)
      {
         ItemData item = items.get(i).getData();
         if (item.getIdentifier().equals(rootData.getIdentifier()))
         {
            // the node
            if (items.get(i).isAdded())
            {
               // if a new item - no modify changes can be
               return new ArrayList<ItemState>();
            }

            if (!items.get(i).isDeleted())
            {
               changes.put(item.getIdentifier(), items.get(i));
            }
         }
         else if (item.getParentIdentifier().equals(rootData.getIdentifier()))
         {
            // childs
            changes.put(item.getIdentifier(), items.get(i));
         }
      }

      return changes.values();
   }

   /**
    * Search for an item state of item with given id and filter parameters.
    * 
    * @param id
    *          - item id
    * @param states
    *          - filter only the given list states (ORed), or all if it's null
    * @param isPersisted
    *          - filter only persisted/not persisted, or all if it's null
    * @return - filtered {@link ItemState}
    * @throws IllegalPathException
    */
   public ItemState findItemState(String id, Boolean isPersisted, int... states) throws IllegalPathException
   {
      List<ItemState> allStates = getAllStates();
      // search from the end for state
      for (int i = allStates.size() - 1; i >= 0; i--)
      {
         ItemState istate = allStates.get(i);
         boolean byState = false;
         if (states != null)
         {
            for (int state : states)
            {
               if (istate.getState() == state)
               {
                  byState = true;
                  break;
               }
            }
         }
         else
         {
            byState = true;
         }
         if (byState && (isPersisted != null ? istate.isPersisted() == isPersisted : true)
            && istate.getData().getIdentifier().equals(id))
         {
            return istate;
         }
      }
      return null;
   }

   /**
    * This class is used as a key for index map.
    */
   protected class IDStateBasedKey
   {

      /**
       * Item identifier.
       */
      private final String identifier;

      /**
       * Item state.
       */
      private final int state;

      /**
       * KeyUUIDState  constructor.
       *
       * @param identifier
       *          item identifier
       * @param state
       *          item state
       */
      public IDStateBasedKey(String identifier, int state)
      {
         this.identifier = identifier;
         this.state = state;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public int hashCode()
      {
         final int prime = 31;
         int result = 1;
         result = prime * result + identifier.hashCode();
         result = prime * result + state;

         return result;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public boolean equals(Object obj)
      {
         if (this == obj)
         {
            return true;
         }
         if (obj == null)
         {
            return false;
         }
         if (getClass() != obj.getClass())
         {
            return false;
         }
         IDStateBasedKey other = (IDStateBasedKey)obj;

         if (identifier == null)
         {
            if (other.identifier != null)
            {
               return false;
            }
         }
         else if (!identifier.equals(other.identifier))
         {
            return false;
         }
         if (state != other.state)
         {
            return false;
         }
         return true;
      }
   }

   /**
    * This class is used as a key for index map.
    */
   protected class ParentIDQPathBasedKey
   {
      /**
       * Item name.
       */
      private final QPathEntry name;

      /**
       * Parent identifier.
       */
      private final String parentIdentifier;

      private final ItemType itemType;

      /**
       * KeyParentUUIDQPath  constructor.
       *
       * @param item
       *          the item
       */
      public ParentIDQPathBasedKey(ItemState item)
      {
         this.name = item.getData().getQPath().getEntries()[item.getData().getQPath().getEntries().length - 1];
         this.parentIdentifier = item.getData().getParentIdentifier();
         this.itemType = ItemType.getItemType(item.getData());
      }

      /**
       * KeyParentUUIDQPath  constructor.
       *
       * @param parentIdentifier
       *          the parent identifier
       * @param name
       *          item name
       */
      ParentIDQPathBasedKey(String parentIdentifier, QPathEntry name, ItemType itemType)
      {
         this.name = name;
         this.parentIdentifier = parentIdentifier;
         this.itemType = itemType;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public int hashCode()
      {
         final int prime = 31;
         int result = 1;
         result = prime * result + name.getName().hashCode();
         result = prime * result + name.getNamespace().hashCode();
         result = prime * result + name.getIndex();
         result = prime * result + (parentIdentifier == null ? 0 : parentIdentifier.hashCode());
         result = prime * result + itemType.ordinal();

         return result;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public boolean equals(Object obj)
      {
         if (this == obj)
         {
            return true;
         }
         if (obj == null)
         {
            return false;
         }
         if (getClass() != obj.getClass())
         {
            return false;
         }
         ParentIDQPathBasedKey other = (ParentIDQPathBasedKey)obj;

         if (name == null)
         {
            if (other.name != null)
            {
               return false;
            }
         }
         else if (!name.getName().equals(other.name.getName())
            || !name.getNamespace().equals(other.name.getNamespace()) || name.getIndex() != other.name.getIndex())
         {
            return false;
         }

         if (parentIdentifier == null)
         {
            if (other.parentIdentifier != null)
            {
               return false;
            }
         }
         else if (!parentIdentifier.equals(other.parentIdentifier))
         {
            return false;
         }

         if (itemType == null)
         {
            if (other.itemType != null)
            {
               return false;
            }
         }
         else if (!itemType.equals(other.itemType))
         {
            return false;
         }

         return true;
      }
   }

   // Need for Externalizable
   // ------------------ [ BEGIN ] ------------------

   public void writeExternal(ObjectOutput out) throws IOException
   {
      out.writeInt(eventType);

      byte[] buff = sessionId.getBytes(Constants.DEFAULT_ENCODING);
      out.writeInt(buff.length);
      out.write(buff);

      int listSize = items.size();
      out.writeInt(listSize);
      for (int i = 0; i < listSize; i++)
      {
         out.writeObject(items.get(i));
      }

      if (pairId != null)
      {
         out.writeInt(NOT_NULL_VALUE);
         buff = pairId.getBytes(Constants.DEFAULT_ENCODING);
         out.writeInt(buff.length);
         out.write(buff);
      }
      else
      {
         out.writeInt(NULL_VALUE);
      }
   }

   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      eventType = in.readInt();

      byte[] buf = new byte[in.readInt()];
      in.readFully(buf);
      sessionId = new String(buf, Constants.DEFAULT_ENCODING);

      int listSize = in.readInt();
      for (int i = 0; i < listSize; i++)
      {
         add((ItemState)in.readObject());
      }

      if (in.readInt() == NOT_NULL_VALUE)
      {
         buf = new byte[in.readInt()];
         in.readFully(buf);
         pairId = new String(buf, Constants.DEFAULT_ENCODING);
      }
   }

   // ------------------ [ END ] ------------------

}
