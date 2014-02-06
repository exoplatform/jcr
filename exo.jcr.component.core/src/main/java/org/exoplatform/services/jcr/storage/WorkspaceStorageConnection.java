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
package org.exoplatform.services.jcr.storage;

import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.core.itemfilters.QPathEntryFilter;
import org.exoplatform.services.jcr.impl.dataflow.persistent.ACLHolder;
import org.exoplatform.services.jcr.impl.dataflow.persistent.ChangedSizeHandler;

import java.util.List;

import javax.jcr.InvalidItemStateException;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS<br>
 * 
 * Includes methods for direct read (by identifier and qPath), node child traversing, reference
 * access as well as write single item methods (add, update, delete) and batch operations support.
 * Valid (workable) connection state is "opened" (isOpened() == true). Newly created connection
 * should have "opened" state. The connection becomes "closed" (invalid for using) after calling
 * commit() or rollback() methods. In this case methods calling will cause an IllegalStateException
 * 
 * Connection object intends to be as "light" as possible i.e. connection creation SHOULD NOT be
 * expensive operation, so better NOT to open/close potentially EXPENSIVE resources using by
 * Connection (WorkspaceDataContainer should be responsible for that). The Connection IS NOT a
 * thread-safe object and normally SHOULD NOT be pooled/cached.
 * 
 * @author Gennady Azarenkov
 * @version $Id: WorkspaceStorageConnection.java 11907 2008-03-13 15:36:21Z ksm $
 * @LevelAPI Unsupported
 */
public interface WorkspaceStorageConnection
{

   /**
    * Checks if <code>ItemData</code> exists into the storage using item's parent and name relative the parent
    * location of define type.
    * 
    * @param parentData
    *          - the item's parent NodeData
    * @param name
    *          - item's path entry (QName + index)
    * @param itemType
    *             item type         
    * @return - true if ItemData exists and false otherwise
    * @throws RepositoryException
    *           if some exception occurred
    * @throws IllegalStateException
    *           if connection is closed
    */
   boolean hasItemData(NodeData parentData, QPathEntry name, ItemType itemType) throws RepositoryException,
      IllegalStateException;

   /**
    * Reads <code>ItemData</code> from the storage using item's parent and name relative the parent
    * location of a given type.
    * 
    * @param parentData
    *          - the item's parent NodeData
    * @param name
    *          - item's path entry (QName + index)
    * @param itemType
    *             item type         
    * @return - stored ItemData which has exact the same path Entry (name+index) inside the parent; or
    *         null if not such an item data found
    * @throws RepositoryException
    *           if some exception occurred
    * @throws IllegalStateException
    *           if connection is closed
    */
   ItemData getItemData(NodeData parentData, QPathEntry name, ItemType itemType) throws RepositoryException,
      IllegalStateException;

   /**
    * Reads <code>ItemData</code> from the storage by item identifier.
    * 
    * @param identifier
    *          - Item identifier
    * @return stored ItemData or null if no item found with given id. Basically used for
    *         Session.getNodeByUUID but not necessarily refers to jcr:uuid property (In fact, this
    *         identifier should not necessary be equal of referenceable node's UUID if any) thereby
    *         can return NodeData for not referenceable node data or PropertyData.
    * @throws RepositoryException
    *           if some exception occurred
    * @throws IllegalStateException
    *           if connection is closed
    */
   ItemData getItemData(String identifier) throws RepositoryException, IllegalStateException;

   /**
    * Reads <code>List</code> of <code>NodeData</code> from the storage using item's parent location.
    * 
    * @param parent
    *          NodeData
    * @return child nodes data or empty <code>List</code>
    * @throws RepositoryException
    *           if some exception occurred
    * @throws IllegalStateException
    *           if connection is closed
    */
   List<NodeData> getChildNodesData(NodeData parent) throws RepositoryException, IllegalStateException;

   /**
    * Reads <code>List</code> of <code>NodeData</code> from the storage using item's parent location, and name filter.
    * 
    * @param parent
    *          NodeData
    * @param pattern  - list of QPathEntryFilters
    * @return child nodes data or empty <code>List</code>
    * @throws RepositoryException
    *           if some exception occurred
    * @throws IllegalStateException
    *           if connection is closed
    */
   List<NodeData> getChildNodesData(NodeData parent, List<QPathEntryFilter> pattern) throws RepositoryException,
      IllegalStateException;

   /**
    * Reads count of <code>parent<code/> child nodes.
    *
    * @param parent NodeData
    * @return long, childs count
    * @throws RepositoryException if error occurs
    */
   int getChildNodesCount(NodeData parent) throws RepositoryException;

   /**
    * Reads order number of last <code>parent<code/> child nodes.
    *
    * @param parent NodeData
    * @return long, order number of last parent's child node.
    * @throws RepositoryException if error occurs
    */
   int getLastOrderNumber(NodeData parent) throws RepositoryException;
   
   /**
    * Reads <code>List</code> of <code>PropertyData</code> from the storage using item's parent
    * location.
    * 
    * @param parent
    *          NodeData
    * @return child properties data or empty <code>List</code>
    * @throws RepositoryException
    *           if some exception occurred
    * @throws IllegalStateException
    *           if connection is closed
    */
   List<PropertyData> getChildPropertiesData(NodeData parent) throws RepositoryException, IllegalStateException;

   /**
    * Reads <code>List</code> of <code>PropertyData</code> from the storage using item's parent
    * location.
    * 
    * @param parent
    *          NodeData
    * @param pattern
    *          String[] list of wild card names 
    * @return child properties data or empty <code>List</code>
    * @throws RepositoryException
    *           if some exception occurred
    * @throws IllegalStateException
    *           if connection is closed
    */
   List<PropertyData> getChildPropertiesData(NodeData parent, List<QPathEntryFilter> pattern) throws RepositoryException,
      IllegalStateException;

   /**
    * Reads <code>List</code> of <code>PropertyData</code> with empty <code>ValueData</code> from the
    * storage using item's parent location.
    * 
    * <br/>
    * This method specially dedicated for non-content modification operations (e.g. Items delete).
    * 
    * @param parent
    *          NodeData
    * @return child properties data (with empty data) or empty <code>List</code>
    * @throws RepositoryException
    *           if some exception occurred
    * @throws IllegalStateException
    *           if connection is closed
    */
   List<PropertyData> listChildPropertiesData(NodeData parent) throws RepositoryException, IllegalStateException;

   /**
    * Reads <code>List</code> of <code>PropertyData</code> from the storage using item's parent
    * location.
    * 
    * <br/>
    * It's REFERENCE type Properties referencing Node with given <code>nodeIdentifier</code>.
    * 
    * See more {@link javax.jcr.Node#getReferences()}
    * 
    * @param nodeIdentifier
    *          of referenceable Node
    * @return list of referenced property data or empty <code>List</code>
    * @throws RepositoryException
    *           if some exception occurred
    * @throws IllegalStateException
    *           if connection is closed
    * @throws UnsupportedOperationException
    *           if operation is not supported
    */
   List<PropertyData> getReferencesData(String nodeIdentifier) throws RepositoryException, IllegalStateException,
      UnsupportedOperationException;

   /**
    * Get child Nodes of the parent node.
    * 
    * @param parent 
    *          the parent data
    * @param fromOrderNum
    *          the returned list of child nodes should not contain the node with order number 
    *          less than <code>fromOrderNum</code>
    * @param offset
    *          the position of the first element to retrieve.
    * @param pageSize
    *          the total amount of element expected per page.
    * @param childs
    *          will contain the resulted child nodes
    * @return true if there are data to retrieve for next request and false in other case 
    */
   boolean getChildNodesDataByPage(NodeData parent, int fromOrderNum, int offset, int pageSize, List<NodeData> childs)
      throws RepositoryException;
   
   /**
    * Adds single <code>NodeData</code>.
    * 
    * @param data
    *          - the new data
    * @throws InvalidItemStateException
    *           if the item already exists
    * @throws UnsupportedOperationException
    *           if operation is not supported (it is container for level 1)
    * @throws RepositoryException
    *           if some exception occurred
    * @throws IllegalStateException
    *           if connection is closed
    */
   void add(NodeData data) throws RepositoryException, UnsupportedOperationException, InvalidItemStateException,
      IllegalStateException;

   /**
    * Adds single <code>PropertyData</code>.
    * 
    * @param data
    *          - the new data
    * @param sizeHandler
    *          accumulate changed size
    * @throws InvalidItemStateException
    *           if the item already exists
    * @throws UnsupportedOperationException
    *           if operation is not supported (it is container for level 1)
    * @throws RepositoryException
    *           if some exception occurred
    * @throws IllegalStateException
    *           if connection is closed
    */
   void add(PropertyData data, ChangedSizeHandler sizeHandler) throws RepositoryException,
      UnsupportedOperationException, InvalidItemStateException, IllegalStateException;

   /**
    * Updates <code>NodeData</code>.
    * 
    * @param data
    *          - the new data
    * @throws InvalidItemStateException
    *           (1)if the data is already updated, i.e. persisted version value of persisted data >=
    *           of new data's persisted version value (2) if the persisted data is not NodeData (i.e.
    *           it is PropertyData). It means that some other process deleted original data and
    *           replace it with other type of data.
    * @throws UnsupportedOperationException
    *           if operation is not supported (it is container for level 1)
    * @throws RepositoryException
    *           if some exception occurred
    * @throws IllegalStateException
    *           if connection is closed
    */
   void update(NodeData data) throws RepositoryException, UnsupportedOperationException, InvalidItemStateException,
      IllegalStateException;

   /**
    * Updates <code>PropertyData</code>.
    * 
    * @param data
    *          - the new data
    * @param sizeHandler
    *          accumulates changed size
    * @throws InvalidItemStateException
    *           (1)if the data is already updated, i.e. persisted version value of persisted data >=
    *           of new data's persisted version value (2) if the persisted data is not PropertyData
    *           (i.e. it is NodeData). It means that some other process deleted original data and
    *           replace it with other type of data.
    * @throws UnsupportedOperationException
    *           if operation is not supported (it is container for level 1)
    * @throws RepositoryException
    *           if some exception occurred
    * @throws IllegalStateException
    *           if connection is closed
    */
   void update(PropertyData data, ChangedSizeHandler sizeHandler) throws RepositoryException,
      UnsupportedOperationException, InvalidItemStateException, IllegalStateException;

   /**
    * Renames <code>NodeData</code> using Node identifier and new name and index from the data.
    * 
    * @param data
    *          - NodeData to be renamed
    * @throws InvalidItemStateException
    *           (1)if the data is already updated, i.e. persisted version value of persisted data >=
    *           of new data's persisted version value (2) if the persisted data is not PropertyData
    *           (i.e. it is NodeData). It means that some other process deleted original data and
    *           replace it with other type of data.
    * @throws UnsupportedOperationException
    *           if operation is not supported (it is container for level 1)
    * @throws RepositoryException
    *           if some exception occurred
    * @throws IllegalStateException
    *           if connection is closed
    */
   void rename(NodeData data) throws RepositoryException, UnsupportedOperationException, InvalidItemStateException,
      IllegalStateException;

   /**
    * Deletes <code>NodeData</code>.
    * 
    * @param data
    *          that identifies data to be deleted
    * 
    * @throws InvalidItemStateException
    *           if the data is already deleted
    * @throws UnsupportedOperationException
    *           if operation is not supported (it is container for level 1)
    * @throws RepositoryException
    *           if some exception occurred
    * @throws IllegalStateException
    *           if connection is closed
    */
   void delete(NodeData data) throws RepositoryException, UnsupportedOperationException, InvalidItemStateException,
      IllegalStateException;

   /**
    * Deletes <code>PropertyData</code>.
    * 
    * @param data
    *          that identifies data to be deleted
    * @param sizeHandler
    *          accumulate changed size
    * @throws InvalidItemStateException
    *           if the data is already deleted
    * @throws UnsupportedOperationException
    *           if operation is not supported (it is container for level 1)
    * @throws RepositoryException
    *           if some exception occurred
    * @throws IllegalStateException
    *           if connection is closed
    */
   void delete(PropertyData data, ChangedSizeHandler sizeHandler) throws RepositoryException,
      UnsupportedOperationException, InvalidItemStateException, IllegalStateException;

   /**
    * Prepare the commit phase.
    * 
    * @throws IllegalStateException
    *           if connection is already closed
    * @throws RepositoryException
    *           if some exception occurred
    */
   void prepare() throws IllegalStateException, RepositoryException;
   
   /**
    * Persist changes and closes connection. It can be database transaction commit for instance etc.
    * 
    * @throws IllegalStateException
    *           if connection is already closed
    * @throws RepositoryException
    *           if some exception occurred
    */
   void commit() throws IllegalStateException, RepositoryException;

   /**
    * Refuses persistent changes and closes connection. It can be database transaction rollback for
    * instance etc.
    * 
    * @throws IllegalStateException
    *           if connection is already closed
    * @throws RepositoryException
    *           if some exception occurred
    */
   void rollback() throws IllegalStateException, RepositoryException;

   /**
    * Close connection.
    * 
    * @throws IllegalStateException
    *           if connection is already closed
    * @throws RepositoryException
    *           if some exception occurred
    */
   void close() throws IllegalStateException, RepositoryException;

   /**
    * Returns true if connection can be used.
    * 
    * @return boolean, true if connection is open and ready, false - otherwise
    */
   boolean isOpened();

   /**
    * Returns all the nodes that hold some ACL info like owner or permissions
    * 
    * @return a list of all the ACL holders for this workspace
    * @throws RepositoryException
    *           if some exception occurred
    * @throws IllegalStateException
    *           if connection is closed
    * @throws UnsupportedOperationException
    *           if operation is not supported
    */
   List<ACLHolder> getACLHolders() throws RepositoryException, IllegalStateException, UnsupportedOperationException;

   /**
    * Reads count of nodes in workspace.
    * 
    * @return
    *          nodes count 
    * @throws RepositoryException
    *           if a database access error occurs
    */
   public long getNodesCount() throws RepositoryException;

   /**
    * Calculate workspace data size.
    *
    * @throws RepositoryException
    */
   public long getWorkspaceDataSize() throws RepositoryException;

   /**
    * Calculate node data size.
    *
    * @param nodeIdentifier
    *          node identifier which size need to calculate
    * @throws RepositoryException
    */
   public long getNodeDataSize(String nodeIdentifier) throws RepositoryException;

}
