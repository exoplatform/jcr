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
package org.exoplatform.services.jcr.impl.dataflow;

import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.AccessManager;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.dataflow.ItemDataTraversingVisitor;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS 15.12.2006
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id$
 */
public class ItemDataRemoveVisitor extends ItemDataTraversingVisitor
{

   private static Log log = ExoLogger.getLogger("exo.jcr.component.core.ItemDataRemoveVisitor");

   protected List<ItemState> itemRemovedStates = new ArrayList<ItemState>();

   protected List<ItemState> reversedItemRemovedStates = null;

   protected boolean validate;

   protected NodeData removedRoot = null;

   protected QPath ancestorToSave = null;

   private final NodeTypeDataManager nodeTypeManager;

   private final AccessManager accessManager;

   private final ConversationState userState;

   // a deletion without any validation
   public ItemDataRemoveVisitor(ItemDataConsumer dataManager, QPath ancestorToSave)
   {

      this(dataManager, ancestorToSave, null, null, null);
      this.validate = false;

   }

   public ItemDataRemoveVisitor(ItemDataConsumer dataManager, QPath ancestorToSave,
      NodeTypeDataManager nodeTypeManager, AccessManager accessManager, ConversationState userState)
   {
      super(dataManager);
      this.nodeTypeManager = nodeTypeManager;
      this.accessManager = accessManager;
      this.userState = userState;

      this.validate = true;
      this.ancestorToSave = ancestorToSave;
   }

   protected void validate(PropertyData property) throws RepositoryException
   {
      // 1. check AccessDeniedException
      validateAccessDenied(property);

      // 2. check ConstraintViolationException - PropertyDefinition for
      // mandatory/protected flags
      validateConstraints(property);

      // 3. check VersionException
      validateVersion(property);

      // 4. check LockException
      validateLock(property);
   }

   protected void validateAccessDenied(PropertyData property) throws RepositoryException
   {
      NodeData parent = (NodeData)dataManager.getItemData(property.getParentIdentifier());
      if (!accessManager.hasPermission(parent.getACL(), PermissionType.READ, userState.getIdentity()))
      {
         throw new AccessDeniedException("Access denied " + property.getQPath().getAsString() + " for "
            + userState.getIdentity().getUserId() + " (get item parent by id)");
      }
   }

   protected void validateConstraints(PropertyData property) throws RepositoryException
   {
   }

   protected void validateVersion(PropertyData property) throws RepositoryException
   {
   }

   protected void validateLock(PropertyData property) throws RepositoryException
   {
   }

   protected void validate(NodeData node) throws RepositoryException
   {
      // 1. check AccessDeniedException
      validateAccessDenied(node);

      // 2. check ReferentialIntegrityException - REFERENCE property target
      if (nodeTypeManager.isNodeType(Constants.MIX_REFERENCEABLE, node.getPrimaryTypeName(), node.getMixinTypeNames()))
      {
         validateReferential(node);
      }

      // 3. check ConstraintViolationException - NodeDefinition for
      // mandatory/protected flags
      validateConstraints(node);

      // 4. check VersionException
      validateVersion(node);

      // 5. check LockException
      validateLock(node);
   }

   protected void validateAccessDenied(NodeData node) throws RepositoryException
   {

      if (!accessManager.hasPermission(node.getACL(), PermissionType.READ, userState.getIdentity()))
      {
         throw new AccessDeniedException("Access denied " + node.getQPath().getAsString() + " for "
            + userState.getIdentity().getUserId() + " (get item by id)");

      }
   }

   protected void validateReferential(NodeData node) throws RepositoryException
   {

      List<PropertyData> refs = dataManager.getReferencesData(node.getIdentifier(), true);

      // A ReferentialIntegrityException will be thrown on save if this item or an
      // item in its subtree
      // is currently the target of a REFERENCE property located in this workspace
      // but outside
      // this item's subtree and the current Session has read access to that
      // REFERENCE property.

      // An AccessDeniedException will be thrown on save if this item or an item
      // in its subtree
      // is currently the target of a REFERENCE property located in this workspace
      // but outside
      // this item's subtree and the current Session does not have read access to
      // that REFERENCE property.

      for (PropertyData rpd : refs)
      {
         if (isRemoveDescendant(removedRoot))
         {
            // on the tree(s), we have to remove REFERENCE property before the node
            entering(rpd, currentLevel);
         }
         else
         {
            NodeData refParent = (NodeData)dataManager.getItemData(rpd.getParentIdentifier());
            if (!accessManager.hasPermission(refParent.getACL(), PermissionType.READ, userState.getIdentity()))
            {
               throw new AccessDeniedException("Access denied " + rpd.getQPath().getAsString() + " for "
                  + userState.getIdentity().getUserId() + " (get reference property parent by id)");
            }

            throw new ReferentialIntegrityException("This node " + node.getQPath().getAsString()
               + " is currently the target of a REFERENCE property " + rpd.getQPath().getAsString()
               + " located in this workspace. Session id: " + userState.getIdentity().getUserId());
         }
      }
   }

   protected boolean isRemoveDescendant(ItemData item) throws RepositoryException
   {
      return item.getQPath().isDescendantOf(removedRoot.getQPath());
   }

   protected void validateConstraints(NodeData node) throws RepositoryException
   {
   }

   protected void validateVersion(NodeData node) throws RepositoryException
   {
   }

   protected void validateLock(NodeData node) throws RepositoryException
   {
   }

   @Override
   protected void entering(PropertyData property, int level) throws RepositoryException
   {
      if (log.isDebugEnabled())
      {
         log.debug("Entering property " + property.getQPath().getAsString());
      }

      if (validate)
      {
         validate(property);
      }
      property = (PropertyData)copyItemDataDelete(property);
      ItemState state =
         new ItemState(property, ItemState.DELETED, true, ancestorToSave != null ? ancestorToSave : removedRoot
            .getQPath());

      if (!itemRemovedStates.contains(state))
      {
         itemRemovedStates.add(state);
      }
      else if (log.isDebugEnabled())
      {
         // REFERENCE props usecase, see validateReferential(NodeData)
         log.debug("A property " + property.getQPath().getAsString() + " is already listed for remove");
      }
   }

   @Override
   protected void entering(NodeData node, int level) throws RepositoryException
   {
      if (log.isDebugEnabled())
      {
         log.debug("Entering node " + node.getQPath().getAsString());
      }

      // this node is not taken in account
      if (level == 0)
      {
         removedRoot = node;
      }

      if (validate)
      {
         validate(node);
      }
      if (!(node instanceof TransientItemData))
      {
         node = (NodeData)copyItemDataDelete(node);
      }

      ItemState state =
         new ItemState(node, ItemState.DELETED, true, ancestorToSave != null ? ancestorToSave : removedRoot.getQPath());
      itemRemovedStates.add(state);
   }

   @Override
   protected void leaving(PropertyData property, int level) throws RepositoryException
   {
   }

   @Override
   protected void leaving(NodeData node, int level) throws RepositoryException
   {
   }

   public List<ItemState> getRemovedStates()
   {
      if (reversedItemRemovedStates == null)
      {
         Collections.reverse(itemRemovedStates);
         reversedItemRemovedStates = itemRemovedStates;
      }

      return reversedItemRemovedStates;
   }

   /**
    * Copy ItemData for Delete operation.
    * 
    * @param item
    *          ItemData
    * @return TransientItemData
    * @throws RepositoryException
    *           if error occurs
    */
   private TransientItemData copyItemDataDelete(final ItemData item) throws RepositoryException
   {

      if (item == null)
      {
         return null;
      }

      // make a copy
      if (item.isNode())
      {
         final NodeData node = (NodeData)item;

         // the node ACL can't be are null as ACL manager does care about this
         final AccessControlList acl = node.getACL();
         if (acl == null)
         {
            throw new RepositoryException("Node ACL is null. " + node.getQPath().getAsString() + " "
               + node.getIdentifier());
         }

         return new TransientNodeData(node.getQPath(), node.getIdentifier(), node.getPersistedVersion(), node
            .getPrimaryTypeName(), node.getMixinTypeNames(), node.getOrderNumber(), node.getParentIdentifier(), acl);
      }

      // else - property
      final PropertyData prop = (PropertyData)item;

      // make a copy, value wil be null for deleting items
      TransientPropertyData newData =
         new TransientPropertyData(prop.getQPath(), prop.getIdentifier(), prop.getPersistedVersion(), prop.getType(),
            prop.getParentIdentifier(), prop.isMultiValued());

      return newData;
   }
}
