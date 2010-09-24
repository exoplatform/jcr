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
package org.exoplatform.services.jcr.dataflow.persistent;

import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;

import java.util.List;

/**
 * Created by The eXo Platform SAS.
 * 
 * Defines storage cache contract
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: WorkspaceStorageCache.java 13869 2008-05-05 08:40:10Z pnedonosko $
 */
public interface WorkspaceStorageCache extends MandatoryItemsPersistenceListener
{

   public static final String MAX_SIZE_PARAMETER_NAME = "max-size";

   public static final String LIVE_TIME_PARAMETER_NAME = "live-time";

   /**
    * Get item by parent identifier and name +index.
    * 
    * @param parentIdentifier
    *          parent identifier
    * @param name
    *          item name
    * @return itemData by parent Identifier and item name with index or null in other case
    */
   @Deprecated
   ItemData get(String parentIdentifier, QPathEntry name);

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
    * @param identifier
    * @return ItemData by Identifier or null if not found
    */
   ItemData get(String identifier);

   /**
    * @param parent
    * @return child nodes for parent if found; empty list if no items found; null if no items
    *         initialized
    */
   List<NodeData> getChildNodes(NodeData parent);

   /**
    * @param parent
    * @return child nodes count for parent if found; 0 if no items found; -1 if no items
    *         initialized
    */
   int getChildNodesCount(NodeData parent);

   /**
    * Get node child properties.<br/>
    * 
    * @param parent
    * @return child properties for parent if found; empty list if no items found; null if no items
    *         initialized
    */
   List<PropertyData> getChildProperties(NodeData parent);

   /**
    * List node child properties.<br/> A difference from {@link getChildProperties()} it's that the
    * method may return list of node properties (PropertyData) which contains no data
    * (ValueData).<br/> Used for Node.hasProperties(), NodeIndexer.createDoc().
    * 
    * @param parent
    * @return child properties for parent if found; null if no items initialized
    */
   List<PropertyData> listChildProperties(final NodeData parentData);

   /**
    * Adds (or updates if found) ItemData.
    * 
    * @param item
    */
   void put(ItemData item);

   /**
    * Adds (update should not be the case!) list of child nodes. The list can be empty. If list is
    * null the operation is ignored.
    * 
    * @param parent
    * @param childNodes
    */
   void addChildNodes(NodeData parent, List<NodeData> childNodes);

   /**
    * Adds (update should not be the case!) list of child properties. The list can be empty. If list
    * is null the operation is ignored.
    * 
    * @param parent
    * @param childNodes
    */
   void addChildProperties(NodeData parent, List<PropertyData> childProperties);

   /**
    * Adds (update should not be the case!) list of child properties with empty values. The list can
    * be empty. If list is null the operation is ignored.
    * 
    * @param parent
    * @param childNodes
    */
   void addChildPropertiesList(NodeData parent, List<PropertyData> childProperties);

   /**
    * Removes data and its children from cache.
    * 
    * @param item
    */
   void remove(ItemData item);

   /**
    * 
    * @return enabled status flag, if true then cache is enabled
    */
   boolean isEnabled();

   /**
    * Cache size.
    * 
    * @return long value
    */
   long getSize();

   /**
    * Start buffering process.
    */
   public void beginTransaction();

   /**
    * Sort changes and commit data to the cache.
    */
   public void commitTransaction();

   /**
    * Forget about changes
    */
   public void rollbackTransaction();

}
