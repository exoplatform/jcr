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
package org.exoplatform.services.jcr.impl.dataflow.persistent;

import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.SharedDataManager;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.Calendar;
import java.util.List;

import javax.jcr.InvalidItemStateException;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS. Data Manager supported ACL Inheritance
 * 
 * @author Gennady Azarenkov
 * @version $Id: ACLInheritanceSupportedWorkspaceDataManager.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class ACLInheritanceSupportedWorkspaceDataManager implements SharedDataManager
{

   private static Log LOG = ExoLogger.getLogger("exo.jcr.component.core.ACLInheritanceSupportedWorkspaceDataManager");

   protected final CacheableWorkspaceDataManager persistentManager;

   public ACLInheritanceSupportedWorkspaceDataManager(CacheableWorkspaceDataManager persistentManager)
   {
      this.persistentManager = persistentManager;
   }

   /**
    * Traverse items parents in persistent storage for ACL containing parent. Same work is made in
    * SessionDataManager.getItemData(NodeData, QPathEntry[]) but for session scooped items.
    * 
    * @param node
    *          - item
    * @return - parent or null
    * @throws RepositoryException
    */
   private AccessControlList getNearestACAncestorAcl(NodeData node) throws RepositoryException
   {

      if (node.getParentIdentifier() != null)
      {
         NodeData parent = (NodeData)getItemData(node.getParentIdentifier());
         while (parent != null)
         {
            if (parent.getACL() != null)
            {
               // has an AC parent
               return parent.getACL();
            }
            // going up to the root
            parent = (NodeData)getItemData(parent.getParentIdentifier());
         }
      }
      return new AccessControlList();
   }

   /**
    * @param parent
    *          - a parent, can be null (get item by id)
    * @param data
    *          - an item data
    * @return - an item data with ACL was initialized
    * @throws RepositoryException
    */
   private ItemData initACL(NodeData parent, NodeData node) throws RepositoryException
   {
      if (node != null)
      {
         AccessControlList acl = node.getACL();
         if (acl == null)
         {
            if (parent != null)
            {
               // use parent ACL
               node =
                  new TransientNodeData(node.getQPath(), node.getIdentifier(), node.getPersistedVersion(), node
                     .getPrimaryTypeName(), node.getMixinTypeNames(), node.getOrderNumber(),
                     node.getParentIdentifier(), parent.getACL());
            }
            else
            {
               // use nearest ancestor ACL... case of get by id
               node =
                  new TransientNodeData(node.getQPath(), node.getIdentifier(), node.getPersistedVersion(), node
                     .getPrimaryTypeName(), node.getMixinTypeNames(), node.getOrderNumber(),
                     node.getParentIdentifier(), getNearestACAncestorAcl(node));
            }
         }
         else if (!acl.hasPermissions())
         {
            // use nearest ancestor permissions
            AccessControlList ancestorAcl = getNearestACAncestorAcl(node);

            node =
               new TransientNodeData(node.getQPath(), node.getIdentifier(), node.getPersistedVersion(), node
                  .getPrimaryTypeName(), node.getMixinTypeNames(), node.getOrderNumber(), node.getParentIdentifier(),
                  new AccessControlList(acl.getOwner(), ancestorAcl.getPermissionEntries()));
         }
         else if (!acl.hasOwner())
         {
            // use nearest ancestor owner
            AccessControlList ancestorAcl = getNearestACAncestorAcl(node);

            node =
               new TransientNodeData(node.getQPath(), node.getIdentifier(), node.getPersistedVersion(), node
                  .getPrimaryTypeName(), node.getMixinTypeNames(), node.getOrderNumber(), node.getParentIdentifier(),
                  new AccessControlList(ancestorAcl.getOwner(), acl.getPermissionEntries()));

         }
      }

      return node;
   }

   /**
    * {@inheritDoc}
    */
   // ------------ ItemDataConsumer impl ------------
   public List<NodeData> getChildNodesData(NodeData parent) throws RepositoryException
   {
      final List<NodeData> nodes = persistentManager.getChildNodesData(parent);
      for (int i = 0; i < nodes.size(); i++)
      {
         nodes.set(i, (NodeData)initACL(parent, nodes.get(i)));
      }
      return nodes;
   }

   /**
    * {@inheritDoc}
    */
   public int getLastOrderNumber(final NodeData parent) throws RepositoryException
   {
      return persistentManager.getLastOrderNumber(parent);
   }

   /**
    * {@inheritDoc}
    */
   public int getChildNodesCount(final NodeData parent) throws RepositoryException
   {
      return persistentManager.getChildNodesCount(parent);
   }

   /**
    * {@inheritDoc}
    */
   public ItemData getItemData(NodeData parent, QPathEntry name) throws RepositoryException
   {
      return getItemData(parent, name, ItemType.UNKNOWN);
   }

   /**
    * {@inheritDoc}
    */
   public ItemData getItemData(NodeData parent, QPathEntry name, ItemType itemType) throws RepositoryException
   {
      final ItemData item = persistentManager.getItemData(parent, name, itemType);
      return item != null && item.isNode() ? initACL(parent, (NodeData)item) : item;
   }

   /**
    * {@inheritDoc}
    */
   public ItemData getItemData(String identifier) throws RepositoryException
   {
      final ItemData item = persistentManager.getItemData(identifier);
      return item != null && item.isNode() ? initACL(null, (NodeData)item) : item;
   }

   /**
    * {@inheritDoc}
    */
   public List<PropertyData> getChildPropertiesData(NodeData parent) throws RepositoryException
   {
      return persistentManager.getChildPropertiesData(parent);
   }

   /**
    * {@inheritDoc}
    */
   public List<PropertyData> listChildPropertiesData(NodeData parent) throws RepositoryException
   {
      return persistentManager.listChildPropertiesData(parent);
   }

   /**
    * {@inheritDoc}
    */
   public List<PropertyData> getReferencesData(String identifier, boolean skipVersionStorage)
      throws RepositoryException
   {
      return persistentManager.getReferencesData(identifier, skipVersionStorage);
   }

   // ------------ SharedDataManager ----------------------

   /**
    * {@inheritDoc}
    */
   public void save(ItemStateChangesLog changes) throws InvalidItemStateException, UnsupportedOperationException,
      RepositoryException
   {
      persistentManager.save(changes);
   }

   /**
    * {@inheritDoc}
    */
   public Calendar getCurrentTime()
   {
      return persistentManager.getCurrentTime();
   }
}
