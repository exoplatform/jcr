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

import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.core.itemfilters.QPathEntryFilter;

import java.util.List;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.<br/>
 * 
 *          Basic (Level 1) data flow inmemory operations<br/>
 * 
 *          Common Rule for Read : If there is some storage in this manager try to get the data
 *          from here first, if not found call super.someMethod
 *          
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id$
 */
public interface ItemDataConsumer
{
   /**
    * Checks if Item by parent (id) and name (with path index) of define type exists.
    * 
    * @param parent 
    *          NodeData
    * @param name 
    *          item name
    * @param itemType 
    *          itemType
    * @return true if Item exists and false otherwise
    * @throws RepositoryException
    */
   boolean hasItemData(NodeData parent, QPathEntry name, ItemType itemType) throws RepositoryException;

   /**
    * Find Item by parent (id) and name (with path index) of a given type.
    * 
    * @param parent 
    *          NodeData
    * @param name 
    *          item name
    * @param itemType 
    *          itemType
    * @return ItemData, data by parent and name
    * @throws RepositoryException
    */
   ItemData getItemData(NodeData parent, QPathEntry name, ItemType itemType) throws RepositoryException;

   /**
     * Find Item by parent (id) and name (with path index) of a given type and create
     * or not (defined by createNullItemData) null item data.
     * 
     * @param parent 
     *          NodeData
     * @param name 
     *          item name
     * @param itemType 
     *          itemType
     * @param createNullItemData 
     *          defines if NullItemData should be created          
     * @return ItemData, data by parent and name
     * @throws RepositoryException
     */
   ItemData getItemData(NodeData parent, QPathEntry name, ItemType itemType, boolean createNullItemData)
      throws RepositoryException;

   /**
    * Find Item by identifier.
    * 
    * @param String identifier
    * @return ItemData, data by identifier
    */
   ItemData getItemData(String identifier) throws RepositoryException;

   /**
    * Get child Nodes of the parent node.
    * 
    * @param parent NodeData
    * @return List of children Nodes
    */
   List<NodeData> getChildNodesData(NodeData parent) throws RepositoryException;

   /**
    * Get child Nodes of the parent node whose value of order number is between fromOrderNum and toOrderNum.
    * 
    * @param parent 
    *          the parent data
    * @param fromOrderNum
    *          the returned list of child nodes should not contain the node with order number 
    *          less than <code>fromOrderNum</code>
    * @param toOrderNum   
    *          the returned list of child nodes should not contain the node with order number 
    *          more than <code>toOrderNum</code>            
    * @param childs
    *          will contain the resulted child nodes
    * @return true if there are data to retrieve for next request and false in other case 
    */
   boolean getChildNodesDataByPage(NodeData parent, int fromOrderNum, int offset, int pageSize, List<NodeData> childs)
      throws RepositoryException;

   /**
    * Get child Nodes of the parent node.ItemDataFilter used to reduce count of returned items. 
    * But not guarantee that only items matching filter will be returned.
    * 
    * @param parent NodeData
    * @param patternFilters
    * @return List of children Nodes
    */
   List<NodeData> getChildNodesData(NodeData parent, List<QPathEntryFilter> patternFilters) throws RepositoryException;

   /**
    * Get children nodes count of the parent node. 
    * @param parent NodeData
    * @return int, child nodes count
    */
   int getChildNodesCount(NodeData parent) throws RepositoryException;
   
   /**
    * Get order number of parent's last child node.
    * 
    * @param parent node
    * @return int Returns last child nodes order number or -1 if there is no subnodes.
    * @throws RepositoryException
    */
   int getLastOrderNumber(NodeData parent) throws RepositoryException;

   /**
    * Get child Properties of the parent node.
    * 
    * @param parent NodeData
    * @return List of children Properties
    */
   List<PropertyData> getChildPropertiesData(NodeData parent) throws RepositoryException;

   /**
    * Get child Properties of the parent node. ItemDataFilter used to reduce count of returned items. 
    * But not guarantee that only items matching filter will be returned.
    * 
    * @param parent NodeData
    * @param itemDataFilters String
    * 
    * @return List of children Properties
    */
   List<PropertyData> getChildPropertiesData(NodeData parent, List<QPathEntryFilter> itemDataFilters) throws RepositoryException;

   /**
    * List child Properties, returned list will contains Properties without actual Values.
    *
    * @param parent NodeData 
    * @return List of PropertyData 
    * @throws RepositoryException
    */
   List<PropertyData> listChildPropertiesData(final NodeData parent) throws RepositoryException;

   /**
    * Get Referenced properties.
    * 
    * @param identifier
    *          - referenceable id
    * @param skipVersionStorage
    *          - if true references will be returned according the JSR-170 spec, without items from
    *          version storage
    * @return - list of REFERENCE properties
    * @throws RepositoryException
    */
   List<PropertyData> getReferencesData(String identifier, boolean skipVersionStorage) throws RepositoryException;
}
