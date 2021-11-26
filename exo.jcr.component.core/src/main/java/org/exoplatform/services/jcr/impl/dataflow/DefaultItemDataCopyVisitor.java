/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.impl.dataflow;

import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.SessionDataManager;
import org.exoplatform.services.jcr.util.IdGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.jcr.RepositoryException;

/**
 * The class visits each node, all subnodes and all of them properties. It transfer as parameter of
 * a method <code>ItemData.visits()</code>. During visiting the class forms the <b>itemAddStates</b>
 * list of <code>List&lt;ItemState&gt;</code> for copying new nodes and their properties.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id$
 */

public abstract class DefaultItemDataCopyVisitor extends AbstractItemDataCopyVisitor
{

   /**
    * Destination data manager.
    */
   protected SessionDataManager dstDataManager;

   /**
    * Destination node name
    */
   protected InternalQName destNodeName;

   /**
    * The stack. In the top it contains a parent node.
    */
   protected Stack<NodeData> parents;

   /**
    * The list of added item states
    */
   protected List<ItemState> itemAddStates = new ArrayList<ItemState>();

   /**
    * The variable shows necessity of preservation <code>Identifier</code>, not generate new one, at
    * transformation of <code>Item</code>.
    */
   protected boolean keepIdentifiers;

   /**
    * The NodeTypeManager
    */
   protected NodeTypeDataManager ntManager;

   protected QPath ancestorToSave = null;

   /**
    * Creates an instance of this class.
    * 
    * @param parent
    *          - The parent node
    * @param destNodeName
    *          - Destination node name
    * @param nodeTypeManager
    *          - The NodeTypeManager
    * @param srcDataManager
    *          - Source data manager
    * @param keepIdentifiers
    *          - Is it necessity to keep <code>Identifier</code>
    */

   public DefaultItemDataCopyVisitor(NodeData parent, InternalQName destNodeName, NodeTypeDataManager nodeTypeManager,
      SessionDataManager srcDataManager, SessionDataManager dstDataManager, boolean keepIdentifiers)
   {
      super(srcDataManager);

      this.dstDataManager = dstDataManager;
      this.keepIdentifiers = keepIdentifiers;
      this.ntManager = nodeTypeManager;
      this.destNodeName = destNodeName;

      this.parents = new Stack<NodeData>();
      this.parents.add(parent);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void entering(PropertyData property, int level) throws RepositoryException
   {

      InternalQName qname = property.getQPath().getName();

      List<ValueData> values;

      if (ntManager.isNodeType(Constants.MIX_REFERENCEABLE, curParent().getPrimaryTypeName(), curParent()
         .getMixinTypeNames())
         && qname.equals(Constants.JCR_UUID))
      {
         values = new ArrayList<ValueData>(1);
         values.add(new TransientValueData(curParent().getIdentifier()));
      }
      else
      {
         values = copyValues(property);
      }

      TransientPropertyData newProperty =
         new TransientPropertyData(QPath.makeChildPath(curParent().getQPath(), qname), keepIdentifiers ? property
            .getIdentifier() : IdGenerator.generate(), -1, property.getType(), curParent().getIdentifier(), property
            .isMultiValued(), values);

      itemAddStates.add(new ItemState(newProperty, ItemState.ADDED, true, ancestorToSave, level != 0));
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void entering(NodeData node, int level) throws RepositoryException
   {

      if (ancestorToSave == null)
      {
         ancestorToSave = curParent().getQPath();
      }

      NodeData parent = curParent();
      QPath qpath = calculateNewNodePath(node, level);
      // Calc order number if parent supports orderable nodes...
      // If ordering is supported by the node type of the parent node of the new
      // location, then the newly moved node is appended to the end of the child node list.
      int orderNum = 0;
      if (level == 0
         && ntManager.isOrderableChildNodesSupported(parent.getPrimaryTypeName(), parent.getMixinTypeNames()))
      {
         orderNum = calculateNewNodeOrderNumber();
      }
      else
      {
         orderNum = node.getOrderNumber(); // has no matter
      }
      String id = keepIdentifiers ? node.getIdentifier() : IdGenerator.generate();

      AccessControlList acl = parent.getACL();

      boolean isPrivilegeable =
         ntManager.isNodeType(Constants.EXO_PRIVILEGEABLE, node.getPrimaryTypeName(), node.getMixinTypeNames());

      boolean isOwneable =
         ntManager.isNodeType(Constants.EXO_OWNEABLE, node.getPrimaryTypeName(), node.getMixinTypeNames());

      if (isPrivilegeable || isOwneable)
      {
         List<AccessControlEntry> permissionEntries = new ArrayList<AccessControlEntry>();
         permissionEntries.addAll((isPrivilegeable ? node.getACL() : parent.getACL()).getPermissionEntries());

         String owner = isOwneable ? node.getACL().getOwner() : parent.getACL().getOwner();

         acl = new AccessControlList(owner, permissionEntries);
      }

      TransientNodeData newNode =
         new TransientNodeData(qpath, id, -1, node.getPrimaryTypeName(), node.getMixinTypeNames(), orderNum,
            parent.getIdentifier(), acl);

      parents.push(newNode);

      // ancestorToSave is a parent node
      // if level == 0 set internal createt as false for validating on save
      itemAddStates.add(new ItemState(newNode, ItemState.ADDED, true, ancestorToSave, level != 0));
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void leaving(PropertyData property, int level) throws RepositoryException
   {

   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void leaving(NodeData node, int level) throws RepositoryException
   {
      parents.pop();
   }

   /**
    * Returns the current parent node.
    */
   protected NodeData curParent()
   {
      return parents.peek();
   }

   /**
    * Returns the list of item add states.
    */
   public List<ItemState> getItemAddStates()
   {
      return itemAddStates;
   }

   /**
    * Find item states.
    *
    * @param itemPath item path
    * @return List of states
    */
   protected List<ItemState> findItemStates(QPath itemPath)
   {
      List<ItemState> istates = new ArrayList<ItemState>();
      for (ItemState istate : itemAddStates)
      {
         if (istate.getData().getQPath().equals(itemPath))
            istates.add(istate);
      }
      return istates;
   }

   /**
    * Find last ItemState.
    *
    * @param itemPath item path
    * @return ItemState
    */
   protected ItemState findLastItemState(QPath itemPath)
   {
      for (int i = itemAddStates.size() - 1; i >= 0; i--)
      {
         ItemState istate = itemAddStates.get(i);
         if (istate.getData().getQPath().equals(itemPath))
            return istate;
      }
      return null;
   }

   protected int calculateNewNodeOrderNumber() throws RepositoryException
   {
      return dstDataManager.getLastOrderNumber(curParent()) + 1;
   }

   protected QPath calculateNewNodePath(NodeData node, int level) throws RepositoryException
   {
      NodeData parent = curParent();

      InternalQName qname = null;

      List<NodeData> existedChilds = dstDataManager.getChildNodesData(parent);
      int newIndex = 1;
      if (level == 0)
      {
         qname = destNodeName;
         // Calculate SNS index for dest root
         for (NodeData child : existedChilds)
         {
            if (child.getQPath().getName().equals(qname))
            {
               newIndex++; // next sibling index
            }
         }
      }
      else
      {
         qname = node.getQPath().getName();
         newIndex = node.getQPath().getIndex();
      }
      return QPath.makeChildPath(parent.getQPath(), qname, newIndex);
   }
}
