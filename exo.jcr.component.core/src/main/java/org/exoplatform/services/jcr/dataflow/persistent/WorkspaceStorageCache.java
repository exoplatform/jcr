/*
R * Copyright (C) 2009 eXo Platform SAS.
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
package org.exoplatform.services.jcr.dataflow.persistent;

import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.core.itemfilters.QPathEntryFilter;

import java.util.List;

/**
 * Created by The eXo Platform SAS</br>
 * 
 * Defines storage cache contract
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: WorkspaceStorageCache.java 13869 2008-05-05 08:40:10Z pnedonosko $
 * @LevelAPI Unsupported
 */
@Managed
@NameTemplate(@Property(key = "service", value = "Cache"))
public interface WorkspaceStorageCache extends MandatoryItemsPersistenceListener
{

   public static final String MAX_SIZE_PARAMETER_NAME = "max-size";

   public static final String LIVE_TIME_PARAMETER_NAME = "live-time";

   /**
    * Get item by parent identifier and name +index of define type.
    * 
    * @param parentIdentifier
    *          parent identifier
    * @param name
    *          item name
    * @param itemType
    *          item type
    * @return itemData by parent Identifier and item name with index of define type or null in other case
    */
   ItemData get(String parentIdentifier, QPathEntry name, ItemType itemType);

   /**
    * Get item by identifier.
    * 
    * @param identifier the Item Data identifier
    * @return ItemData by Identifier or null if not found
    */
   ItemData get(String identifier);

   /**
    * Get child nodes.
    * 
    * @param parent the parent node data
    * @return child nodes for parent if found; empty list if no items found; null if no items
    *         initialized
    */
   List<NodeData> getChildNodes(NodeData parent);

   /**
    * Get page of child nodes.
    * 
    * @param parent
    *          the parent node data
    * @param fromOrderNum
    *          the order number related to page of child nodes
    * @return child nodes for parent if found; empty list if no items found; null if no items
    *         initialized
    */
   List<NodeData> getChildNodesByPage(final NodeData parent, final int fromOrderNum);

   /**
    * Get child nodes by pattern.
    * 
    * @param parent the parent node data
    * @param pattern the filter to use
    * @return child nodes for parent if found; empty list if no items found; null if no items
    *         initialized
    */
   List<NodeData> getChildNodes(NodeData parent, QPathEntryFilter pattern);

   /**
    * Get child nodes count.
    * 
    * @param parent the parent node data
    * @return child nodes count for parent if found; 0 if no items found; -1 if no items
    *         initialized
    */
   int getChildNodesCount(NodeData parent);

   /**
    * Get node child properties.<br/>
    * 
    * @param parent the parent node data
    * @return child properties for parent if found; empty list if no items found; null if no items
    *         initialized
    */
   List<PropertyData> getChildProperties(NodeData parent);

   /**
    * Get node child properties by pattern.<br/>
    * 
    * @param parent the parent node data
    * @param pattern the filter to use
    * @return child properties for parent if found; empty list if no items found; null if no items
    *         initialized
    */
   List<PropertyData> getChildProperties(NodeData parent, QPathEntryFilter pattern);

   /**
    * List node child properties.<br/> A difference from {@link #getChildProperties(org.exoplatform.services.jcr.datamodel.NodeData)} it's that the
    * method may return list of node properties (PropertyData) which contains no data
    * (ValueData).<br/> Used for Node.hasProperties(), NodeIndexer.createDoc().
    * 
    * @param parentData the parent node data
    * @return child properties for parent if found; null if no items initialized
    */
   List<PropertyData> listChildProperties(final NodeData parentData);

   /**
    * Get referenced properties.
    * 
    * @param identifier
    *          referenceable id
    * @return
    *          list of REFERENCE properties 
    */
   List<PropertyData> getReferencedProperties(String identifier);

   /**
    * Add referenced properties.
    * 
    * @param identifier
    *          referenceable id
    * @param refProperties
    *          list of properties
    */
   void addReferencedProperties(String identifier, List<PropertyData> refProperties);

   /**
    * Adds (or updates if found) ItemData.
    * 
    * @param item the item data
    */
   void put(ItemData item);

   /**
    * Adds the total amount of children nodes.
    *
    * @param parent
    *          the parent node data
    * @param count
    *          the total amount of nodes
    */
   void addChildNodesCount(NodeData parent, int count);

   /**
    * Adds page of child nodes.
    * 
    * @param parent
    *          the parent node data
    * @param childs
    *          the list of child nodes
    * @param fromOrderNum
    *          the order number related to page of child nodes 
    */
   void addChildNodesByPage(NodeData parent, List<NodeData> childs, int fromOrderNum);

   /**
    * Adds (update should not be the case!) list of child nodes. The list can be empty. If list is
    * null the operation is ignored.
    * 
    * @param parent the parent node data
    * @param childNodes the list of child nodes
    */
   void addChildNodes(NodeData parent, List<NodeData> childNodes);

   /**
    * Adds (update should not be the case!) list of child nodes. The list can be empty. If list is
    * null the operation is ignored.
    * 
    * @param parent the parent node data
    * @param pattern the filter to use
    * @param childNodes the list of children nodes
    */
   void addChildNodes(NodeData parent, QPathEntryFilter pattern, List<NodeData> childNodes);

   /**
    * Adds (update should not be the case!) list of child properties. The list can be empty. If list
    * is null the operation is ignored.
    * 
    * @param parent the parent node data
    * @param childProperties the children properties
    */
   void addChildProperties(NodeData parent, List<PropertyData> childProperties);

   /**
    * Adds (update should not be the case!) list of child properties. The list can be empty. If list
    * is null the operation is ignored.
    * 
    * @param parent the parent node data
    * @param pattern the filter to use
    * @param childProperties the children properties
    */
   void addChildProperties(NodeData parent, QPathEntryFilter pattern, List<PropertyData> childProperties);

   /**
    * Adds (update should not be the case!) list of child properties with empty values. The list can
    * be empty. If list is null the operation is ignored.
    * 
    * @param parent the parent node data
    * @param childProperties the list of child properties
    */
   void addChildPropertiesList(NodeData parent, List<PropertyData> childProperties);

   /**
    * Removes data and its children from cache.
    * 
    * @param item the item data
    */
   void remove(ItemData item);

   /**
    * 
    * @return enabled status flag, if true then cache is enabled
    */
   @Managed
   @ManagedDescription("Indicates whether the cache is enabled or not")
   boolean isEnabled();

   /**
    * 
    * @return isPatternSupported status flag, if true then cache can store pattern results 
    */
   boolean isPatternSupported();

   /**
    * @return isPatternSupported status flag, if true then cache can store child lists grouped 
    * by pages
    */
   boolean isChildNodesByPageSupported();

   /**
    * Cache size.
    * 
    * @return long value
    */
   @Managed
   @ManagedDescription("Indicates the total amount of items into the cache")
   long getSize();

   /**
    * Start buffering process.
    */
   void beginTransaction();

   /**
    * Sort changes and commit data to the cache.
    */
   void commitTransaction();

   /**
    * Forget about changes
    */
   void rollbackTransaction();

   /**
    * Adds a new listener
    * @param listener the listener to register
    * @throws UnsupportedOperationException in case the listeners are not supported by the
    * implementation
    */
   void addListener(WorkspaceStorageCacheListener listener) throws UnsupportedOperationException;

   /**
    * Removes a listener
    * @param listener the listener to remove
    * @throws UnsupportedOperationException in case the listeners are not supported by the
    * implementation
    */
   void removeListener(WorkspaceStorageCacheListener listener) throws UnsupportedOperationException;
}
