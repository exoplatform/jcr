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
package org.exoplatform.services.jcr.impl.dataflow.session;

import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.datamodel.IllegalPathException;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.TransientItemData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by The eXo Platform SAS.<br/> Responsible for managing session changes log. Relying on
 * fact that ItemData inside ItemState SHOULD be TransientItemData
 * 
 * @author Gennady Azarenkov
 * @version $Id: SessionChangesLog.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public final class SessionChangesLog extends PlainChangesLogImpl
{

   /**
    * ItemState index storage. Used in getItemState() by id and path.
    */
   protected Map<Object, ItemState> index = new HashMap<Object, ItemState>();

   /**
    * ItemState index storage. Used to store last nodes states. 
    */
   protected Map<String, Map<String, ItemState>> lastChildNodeStates = new HashMap<String, Map<String, ItemState>>();

   /**
    * ItemState index storage. Used to store last properties states.  
    */
   protected Map<String, Map<String, ItemState>> lastChildPropertyStates =
      new HashMap<String, Map<String, ItemState>>();

   /**
    * Stores persisted child nodes count.  
    */
   protected Map<String, int[]> childNodesCount = new HashMap<String, int[]>();

   /**
    * Create empty ChangesLog.
    * 
    * @param sessionId
    */
   public SessionChangesLog(String sessionId)
   {
      super(sessionId);
   }

   /**
    * Create ChangesLog and populate with given items changes.
    * 
    * @param items
    * @param sessionId
    */
   public SessionChangesLog(List<ItemState> items, String sessionId)
   {
      super(items, sessionId);

      for (ItemState change : items)
         addItem(change);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public PlainChangesLog add(ItemState change)
   {
      super.add(change);
      addItem(change);

      return this;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public PlainChangesLog addAll(List<ItemState> changes)
   {
      super.addAll(changes);
      for (ItemState change : changes)
         addItem(change);

      return this;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void clear()
   {
      super.clear();
      index.clear();
      lastChildNodeStates.clear();
      lastChildPropertyStates.clear();
      childNodesCount.clear();
   }

   /**
    * Removes the item at the rootPath and all descendants from the log
    * 
    * @param root
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
            items.remove(i);
            index.remove(item.getData().getIdentifier());
            index.remove(item.getData().getQPath());
            index.remove(new ParentIDQPathBasedKey(item));
            index.remove(new IDStateBasedKey(item.getData().getIdentifier(), item.getState()));
            childNodesCount.remove(item.getData().getIdentifier());
            lastChildNodeStates.remove(item.getData().getIdentifier());
            lastChildPropertyStates.remove(item.getData().getIdentifier());

            if (item.isNode() && item.isPersisted())
            {
               int childCount[] = childNodesCount.get(item.getData().getParentIdentifier());
               if (childCount != null)
               {
                  if (item.isDeleted())
                     ++childCount[0];
                  else if (item.isAdded())
                     --childCount[0];

                  childNodesCount.put(item.getData().getParentIdentifier(), childCount);
               }
            }

            if (item.getData().isNode())
            {
               Map<String, ItemState> children = lastChildNodeStates.get(item.getData().getParentIdentifier());
               if (children != null)
                  children.remove(item.getData().getIdentifier());
            }
            else
            {
               Map<String, ItemState> children = lastChildPropertyStates.get(item.getData().getParentIdentifier());
               if (children != null)
                  children.remove(item.getData().getIdentifier());
            }
         }
      }
   }

   /**
    * Returns list with changes of this node and its descendants. NOTE: this operation may cost more
    * than use of getDescendantsChanges() by path
    * 
    * @param rootIdentifier
    */
   public List<ItemState> getDescendantsChanges(String rootIdentifier)
   {
      List<ItemState> changesList = new ArrayList<ItemState>();

      traverseChangesByIdentifier(rootIdentifier, changesList);

      return changesList;
   }

   private void traverseChangesByIdentifier(String identifier, List<ItemState> changesList)
   {
      for (ItemState item : items)
      {
         if (item.getData().getIdentifier().equals(identifier))
         {
            changesList.add(item);
         }
         else if (item.getData().getParentIdentifier().equals(identifier))
         {
            traverseChangesByIdentifier(item.getData().getIdentifier(), changesList);
         }
      }
   }

   /**
    * An example of use: transient changes of item added and removed in same session. These changes
    * must not fire events in observation.
    * 
    * @param identifier
    */
   public void eraseEventFire(String identifier)
   {
      for (ItemState item : items)
      {
         if (item.getData().getIdentifier().equals(identifier))
         {
            // erase flag
            item.eraseEventFire();
         }
         else if (item.getData().getParentIdentifier().equals(identifier))
         {
            eraseEventFire(item.getData().getIdentifier());
         }
      }
   }

   /**
    * @param rootPath
    * @return item state at the rootPath and its descendants
    */
   public List<ItemState> getDescendantsChanges(QPath rootPath)
   {
      List<ItemState> list = new ArrayList<ItemState>();
      for (int i = 0; i < items.size(); i++)
      {
         if (items.get(i).isDescendantOf(rootPath))
         {
            list.add(items.get(i));
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
      for (ItemState state : getAllStates())
      {
         if (state.getData().getIdentifier().equals(itemIdentifier))
         {
            states.add(state);
         }
      }
      return states;
   }

   /**
    * Creates new changes log with rootPath and its descendants of this one and removes those
    * entries.
    * 
    * @param rootPath
    * @return ItemDataChangesLog
    */
   public PlainChangesLog pushLog(QPath rootPath)
   {
      PlainChangesLog cLog = new PlainChangesLogImpl(sessionId);

      if (rootPath.equals(Constants.ROOT_PATH))
      {
         cLog.addAll(items);
         clear();
      }
      else
      {
         cLog.addAll(getDescendantsChanges(rootPath));
         remove(rootPath);
      }

      return cLog;
   }

   /**
    * Get ItemState by parent and item name.
    * 
    * @param parentData
    * @param name
    * @return
    * @throws IllegalPathException
    */
   public ItemState getItemState(NodeData parentData, QPathEntry name) throws IllegalPathException
   {
      return index.get(new ParentIDQPathBasedKey(parentData.getIdentifier(), name));
   }

   /**
    * Get ItemState by identifier.
    * 
    * NOTE: Uses index HashMap.
    * 
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
    * Get ItemState by identifier and state.
    * 
    * NOTE: Uses index HashMap.
    * 
    * @param itemIdentifier
    * @param sate
    * @return
    */
   public ItemState getItemState(String itemIdentifier, int state)
   {
      return index.get(new IDStateBasedKey(itemIdentifier, state));
   }

   /**
    * Collect changes of all item direct childs (only). Including the item itself.
    * 
    * @param rootIdentifier
    * @return
    */
   public List<ItemState> getChildrenChanges(String rootIdentifier)
   {
      List<ItemState> list = new ArrayList<ItemState>();
      for (int i = 0; i < items.size(); i++)
      {
         ItemData item = items.get(i).getData();
         if (item.getParentIdentifier().equals(rootIdentifier) || item.getIdentifier().equals(rootIdentifier))
            list.add(items.get(i));
      }
      return list;
   }

   public int getChildNodesCount(String rootIdentifier)
   {
      int[] childCount = childNodesCount.get(rootIdentifier);
      return childCount == null ? 0 : childCount[0];
   }

   /**
    * Collect last in ChangesLog order item child changes.
    * 
    * @param rootData
    *          - a item root of the changes scan
    * @param forNodes
    *          retrieves nodes' ItemStates is true, or properties' otherwice
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
    * Collect last in ChangesLog order node (and direct childs) changes.
    * 
    * @param rootData
    *          - a item root of the changes scan
    * @param forNodes
    *          retrieves nodes' ItemStates is true, or properties' otherwice
    * @return this item (!) and child items last modify states (i.e. updates, not adds or deletes)
    */
   public Collection<ItemState> getLastModifyStates(NodeData rootData)
   {
      HashMap<String, ItemState> changes = new HashMap<String, ItemState>();

      for (int i = 0; i < items.size(); i++)
      {
         TransientItemData item = (TransientItemData)items.get(i).getData();
         if (item.getIdentifier().equals(rootData.getIdentifier()))
         {
            // the node
            if (items.get(i).isAdded())
               // if a new item - no modify changes can be
               return new ArrayList<ItemState>();

            if (!items.get(i).isDeleted())
               changes.put(item.getIdentifier(), items.get(i));
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
    * EXPERIMENTAL. NOT USED. Find a rename operation pair of states by path of DELETED item. Search
    * from the end of log for DELETED state first. Then repeat the search for RENAMED state.
    * 
    * @param deletedPath
    *          - target node path
    * @return - the pair of states of item (or its ancestors), ItemState[] {DELETED, RENAMED} or null
    *         if renaming is not detected.
    * @throws IllegalPathException
    */
   public ItemState[] findRenamed(QPath deletedPath) throws IllegalPathException
   {
      List<ItemState> allStates = getAllStates();
      // search from the end for DELETED state.
      // RENAMED comes after the DELETED in the log immediately (in back order)
      for (int i = allStates.size() - 1; i >= 0; i--)
      {
         ItemState state = allStates.get(i);
         if (state.getState() == ItemState.DELETED
            && !state.isPersisted()
            && (deletedPath.isDescendantOf(state.getData().getQPath()) || deletedPath
               .equals(state.getData().getQPath())))
         {
            // 1. if it's an item or ancestor of logged data
            try
            {
               ItemState delete = state;
               ItemState rename = allStates.get(i + 1);

               if (rename.getState() == ItemState.RENAMED && rename.isPersisted()
                  && rename.getData().getIdentifier().equals(delete.getData().getIdentifier()))
               {

                  // 2. search of most fresh state for searched rename state
                  for (int bi = allStates.size() - 1; bi >= i + 2; bi--)
                  {
                     state = allStates.get(bi);
                     if (state.getState() == ItemState.RENAMED && state.isPersisted()
                        && state.getData().getIdentifier().equals(rename.getData().getIdentifier()))
                     {
                        // got much fresh
                        rename = state;
                        delete = allStates.get(i - 1); // try the fresh delete state
                        if (delete.getData().getIdentifier().equals(rename.getData().getIdentifier()))
                           return new ItemState[]{delete, rename}; // 3. ok, got it
                     }
                  }

                  return new ItemState[]{delete, rename}; // 4. ok, there are no
                  // more fresh we have
                  // found before p.2
               } // else, it's not a rename, search deeper
            }
            catch (IndexOutOfBoundsException e)
            {
               // the pair not found
               return null;
            }
         }
      }
      return null;
   }

   /**
    * NOT USED. Search for an item state of item with given path (or its ancestor) and filter
    * parameters.
    * 
    * @param rootPath
    *          - item path (root path)
    * @param states
    *          - filter only the given list states, or all if it's null
    * @param isPersisted
    *          - filter only persisted/not persisted, or all if it's null
    * @param orAncestor
    *          - may return the item ancestor if true and the ancestor was changed last, or only item
    *          with given path if it's null
    * @return - filtered {@link ItemState}
    * @throws IllegalPathException
    */
   public ItemState findItemState(QPath rootPath, Boolean isPersisted, Boolean orAncestor, int... states)
      throws IllegalPathException
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
            byState = true;
         if (byState
            && (isPersisted != null ? istate.isPersisted() == isPersisted : true)
            && ((orAncestor != null && orAncestor ? rootPath.isDescendantOf(istate.getData().getQPath()) : true) || rootPath
               .equals(istate.getData().getQPath())))
         {
            return istate;
         }
      }
      return null;
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
    * @param orAncestor
    *          - may return the item ancestor if true and the ancestor was changed last, or only item
    *          with given path if it's null
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
            byState = true;
         if (byState && (isPersisted != null ? istate.isPersisted() == isPersisted : true)
            && istate.getData().getIdentifier().equals(id))
         {
            return istate;
         }
      }
      return null;
   }

   /**
    * Adds item to the changes log.
    * 
    * @param item
    *          the item
    */
   private void addItem(ItemState item)
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
      }

      if (item.isNode() && item.isPersisted())
      {
         int[] childCount = childNodesCount.get(item.getData().getParentIdentifier());
         if (childCount == null)
            childCount = new int[1];

         if (item.isDeleted())
            --childCount[0];
         else if (item.isAdded())
            ++childCount[0];

         childNodesCount.put(item.getData().getParentIdentifier(), childCount);
      }
   }

   /**
    * This class is used as a key for index map.
    */
   private class IDStateBasedKey
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
      IDStateBasedKey(String identifier, int state)
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
            return true;
         if (obj == null)
            return false;
         if (getClass() != obj.getClass())
            return false;
         IDStateBasedKey other = (IDStateBasedKey)obj;

         if (identifier == null)
         {
            if (other.identifier != null)
               return false;
         }
         else if (!identifier.equals(other.identifier))
            return false;
         if (state != other.state)
            return false;
         return true;
      }
   }

   /**
    * This class is used as a key for index map.
    */
   private class ParentIDQPathBasedKey
   {
      /**
       * Item name.
       */
      private final QPathEntry name;

      /**
       * Parent identifier.
       */
      private final String parentIdentifier;

      /**
       * KeyParentUUIDQPath  constructor.
       *
       * @param item
       *          the item
       */
      ParentIDQPathBasedKey(ItemState item)
      {
         this.name = item.getData().getQPath().getEntries()[item.getData().getQPath().getEntries().length - 1];
         this.parentIdentifier = item.getData().getParentIdentifier();
      }

      /**
       * KeyParentUUIDQPath  constructor.
       *
       * @param parentIdentifier
       *          the parent identifier
       * @param name
       *          item name
       */
      ParentIDQPathBasedKey(String parentIdentifier, QPathEntry name)
      {
         this.name = name;
         this.parentIdentifier = parentIdentifier;
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
         result = prime * result + parentIdentifier.hashCode();

         return result;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public boolean equals(Object obj)
      {
         if (this == obj)
            return true;
         if (obj == null)
            return false;
         if (getClass() != obj.getClass())
            return false;
         ParentIDQPathBasedKey other = (ParentIDQPathBasedKey)obj;

         if (name == null)
         {
            if (other.name != null)
               return false;
         }
         else if (!name.getName().equals(other.name.getName())
            || !name.getNamespace().equals(other.name.getNamespace()) || name.getIndex() != other.name.getIndex())
            return false;
         if (parentIdentifier == null)
         {
            if (other.parentIdentifier != null)
               return false;
         }
         else if (!parentIdentifier.equals(other.parentIdentifier))
            return false;
         return true;
      }
   }
}
