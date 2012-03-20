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
package org.exoplatform.services.jcr.impl.core.version;

import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.core.nodetype.NodeDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionData;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.SessionDataManager;
import org.exoplatform.services.jcr.impl.core.value.BaseValue;
import org.exoplatform.services.jcr.impl.dataflow.AbstractItemDataCopyVisitor;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.OnParentVersionAction;
import javax.jcr.version.VersionException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: FrozenNodeInitializer.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class FrozenNodeInitializer extends AbstractItemDataCopyVisitor
{

   private static Log log = ExoLogger.getLogger("exo.jcr.component.core.FrozenNodeInitializer");

   private final Stack<NodeData> contextNodes;

   private final NodeTypeDataManager ntManager;

   private final PlainChangesLog changesLog;

   private final SessionDataManager dataManager;

   private final ValueFactory valueFactory;

   public FrozenNodeInitializer(NodeData frozen, SessionDataManager dataManager, NodeTypeDataManager ntManager,
      PlainChangesLog changesLog, ValueFactory valueFactory) throws RepositoryException
   {
      super(dataManager);
      this.dataManager = dataManager;
      this.ntManager = ntManager;
      this.changesLog = changesLog;
      this.valueFactory = valueFactory;
      this.contextNodes = new Stack<NodeData>();
      this.contextNodes.push(frozen);
   }

   /**
    * {@inheritDoc}
    */
   protected void visitChildNodes(NodeData node) throws RepositoryException
   {
      // It is not necessary to traverse child nodes since parent is null (OnParentVersion=IGNORE case)
      if (currentNode() == null)
      {
         return;
      }

      super.visitChildNodes(node);
   }

   @Override
   protected void entering(PropertyData property, int level) throws RepositoryException
   {

      if (log.isDebugEnabled())
      {
         log.debug("Entering property " + property.getQPath().getAsString());
      }

      if (currentNode() == null)
      {
         // skip if no parent - parent is COMPUTE, INITIALIZE
         return;
      }

      PropertyData frozenProperty = null;
      InternalQName qname = property.getQPath().getName();

      List<ValueData> values = copyValues(property);

      boolean mv = property.isMultiValued();

      if (qname.equals(Constants.JCR_PRIMARYTYPE) && level == 1)
      {
         frozenProperty =
            TransientPropertyData.createPropertyData(currentNode(), Constants.JCR_FROZENPRIMARYTYPE, PropertyType.NAME,
               mv, values);
      }
      else if (qname.equals(Constants.JCR_UUID) && level == 1)
      {
         frozenProperty =
            TransientPropertyData.createPropertyData(currentNode(), Constants.JCR_FROZENUUID, PropertyType.STRING, mv,
               values);
      }
      else if (qname.equals(Constants.JCR_MIXINTYPES) && level == 1)
      {
         frozenProperty =
            TransientPropertyData.createPropertyData(currentNode(), Constants.JCR_FROZENMIXINTYPES, PropertyType.NAME,
               mv, values);
      }
      else
      {
         NodeData parent = (NodeData)dataManager.getItemData(property.getParentIdentifier());

         PropertyDefinitionData pdef =
            ntManager.getPropertyDefinitions(qname, parent.getPrimaryTypeName(), parent.getMixinTypeNames())
               .getAnyDefinition();

         int action = pdef.getOnParentVersion();

         if (action == OnParentVersionAction.IGNORE)
         {
            return;
         }
         else if (action == OnParentVersionAction.ABORT)
         {
            throw new VersionException("Property is aborted " + property.getQPath().getAsString());
         }
         else if (action == OnParentVersionAction.COPY || action == OnParentVersionAction.VERSION
            || action == OnParentVersionAction.COMPUTE)
         {
            frozenProperty =
               TransientPropertyData.createPropertyData(currentNode(), qname, property.getType(), mv, values);
         }
         else if (action == OnParentVersionAction.INITIALIZE)
         {
            // 8.2.11.3 INITIALIZE
            // On checkin of N, a new P will be created and placed in version
            // storage as a child of VN. The new P will be initialized just as it
            // would
            // be if created normally in a workspace
            if (pdef.isAutoCreated())
            {
               if (pdef.getDefaultValues() != null && pdef.getDefaultValues().length > 0)
               {
                  // to use default values
                  values.clear();
                  for (String defValue : pdef.getDefaultValues())
                  {
                     ValueData defData;
                     if (PropertyType.UNDEFINED == pdef.getRequiredType())
                     {
                        defData = ((BaseValue)valueFactory.createValue(defValue)).getInternalData();
                     }
                     else
                     {
                        defData =
                           ((BaseValue)valueFactory.createValue(defValue, pdef.getRequiredType())).getInternalData();
                     }
                     // TransientValueData defData = ((BaseValue)
                     // defValue).getInternalData();
                     // values.add(defData.createTransientCopy());
                     values.add(defData);
                  }
               }
               else if (ntManager.isNodeType(Constants.NT_HIERARCHYNODE, parent.getPrimaryTypeName(), parent
                  .getMixinTypeNames())
                  && qname.equals(Constants.JCR_CREATED))
               {
                  // custom logic for nt:hierarchyNode jcr:created
                  values.clear();
                  values.add(new TransientValueData(dataManager.getTransactManager().getStorageDataManager()
                     .getCurrentTime()));
               }
            } // else... just as it would be if created normally in a workspace
            // (sure with value data)
            frozenProperty =
               TransientPropertyData.createPropertyData(currentNode(), qname, property.getType(), mv, values);
         }
         else
            throw new RepositoryException("Unknown OnParentVersion value " + action);
      }

      changesLog.add(ItemState.createAddedState(frozenProperty));
   }

   @Override
   protected void entering(NodeData node, int level) throws RepositoryException
   {
      // this node is not taken in account
      if (level == 0)
      {
         if (log.isDebugEnabled())
            log.debug("Entering node " + node.getQPath().getAsString() + ", level=0");
         return;
      }

      // ignored parent
      if (currentNode() == null)
      {
         contextNodes.push(null);
         if (log.isDebugEnabled())
            log.debug("Entering node " + node.getQPath().getAsString() + ", HAS NULL PARENT");
         return;
      }

      InternalQName qname = node.getQPath().getName();

      NodeData parent = (NodeData)dataManager.getItemData(node.getParentIdentifier());
      NodeDefinitionData ndef =
         ntManager.getChildNodeDefinition(qname, node.getPrimaryTypeName(), parent.getPrimaryTypeName(),
            parent.getMixinTypeNames());
      if (ndef == null)
      {
         throw new ConstraintViolationException("Definition not found for " + qname.getAsString());
      }
      int action = ndef.getOnParentVersion();

      if (log.isDebugEnabled())
         log.debug("Entering node " + node.getQPath().getAsString() + ", "
            + OnParentVersionAction.nameFromValue(action));

      NodeData frozenNode = null;
      if (action == OnParentVersionAction.IGNORE)
      {
         contextNodes.push(null);
      }
      else if (action == OnParentVersionAction.ABORT)
      {
         throw new VersionException("Node is aborted " + node.getQPath().getAsString());
      }
      else if (action == OnParentVersionAction.COPY)
      {
         AccessControlList acl = currentNode().getACL();

         boolean isPrivilegeable =
            ntManager.isNodeType(Constants.EXO_PRIVILEGEABLE, node.getPrimaryTypeName(), node.getMixinTypeNames());

         boolean isOwneable =
            ntManager.isNodeType(Constants.EXO_OWNEABLE, node.getPrimaryTypeName(), node.getMixinTypeNames());

         if (isPrivilegeable || isOwneable)
         {
            List<AccessControlEntry> permissionEntries = new ArrayList<AccessControlEntry>();
            permissionEntries.addAll((isPrivilegeable ? node.getACL() : currentNode().getACL()).getPermissionEntries());

            String owner = isOwneable ? node.getACL().getOwner() : currentNode().getACL().getOwner();

            acl = new AccessControlList(owner, permissionEntries);
         }

         QPath frozenPath = QPath.makeChildPath(currentNode().getQPath(), qname, node.getQPath().getIndex());
         frozenNode =
            new TransientNodeData(frozenPath, IdGenerator.generate(), node.getPersistedVersion(),
               node.getPrimaryTypeName(), node.getMixinTypeNames(), node.getOrderNumber(), currentNode()
                  .getIdentifier(), acl);

         contextNodes.push(frozenNode);
         changesLog.add(ItemState.createAddedState(frozenNode));
      }
      else if (action == OnParentVersionAction.VERSION)
      {
         if (ntManager.isNodeType(Constants.MIX_VERSIONABLE, node.getPrimaryTypeName(), node.getMixinTypeNames()))
         {
            frozenNode =
               TransientNodeData.createNodeData(currentNode(), qname, Constants.NT_VERSIONEDCHILD, node.getQPath()
                  .getIndex());

            PropertyData pt =
               TransientPropertyData.createPropertyData(frozenNode, Constants.JCR_PRIMARYTYPE, PropertyType.NAME,
                  false, new TransientValueData(Constants.NT_VERSIONEDCHILD));

            ValueData vh =
               ((PropertyData)dataManager.getItemData(node, new QPathEntry(Constants.JCR_VERSIONHISTORY, 0),
                  ItemType.PROPERTY)).getValues().get(0);

            PropertyData pd =
               TransientPropertyData.createPropertyData(frozenNode, Constants.JCR_CHILDVERSIONHISTORY,
                  PropertyType.REFERENCE, false, vh);

            contextNodes.push(null);
            changesLog.add(ItemState.createAddedState(frozenNode));
            changesLog.add(ItemState.createAddedState(pt));
            changesLog.add(ItemState.createAddedState(pd));
         }
         else
         { // behaviour of COPY
            AccessControlList acl = currentNode().getACL();

            boolean isPrivilegeable =
               ntManager.isNodeType(Constants.EXO_PRIVILEGEABLE, node.getPrimaryTypeName(), node.getMixinTypeNames());

            boolean isOwneable =
               ntManager.isNodeType(Constants.EXO_OWNEABLE, node.getPrimaryTypeName(), node.getMixinTypeNames());

            if (isPrivilegeable || isOwneable)
            {
               List<AccessControlEntry> accessList = new ArrayList<AccessControlEntry>();
               accessList.addAll((isPrivilegeable ? node.getACL() : currentNode().getACL())
                  .getPermissionEntries());

               String owner = isOwneable ? node.getACL().getOwner() : currentNode().getACL().getOwner();

               acl = new AccessControlList(owner, accessList);
            }

            QPath frozenPath = QPath.makeChildPath(currentNode().getQPath(), qname, node.getQPath().getIndex());
            frozenNode =
               new TransientNodeData(frozenPath, IdGenerator.generate(), node.getPersistedVersion(),
                  node.getPrimaryTypeName(), node.getMixinTypeNames(), node.getOrderNumber(), currentNode()
                     .getIdentifier(), acl);

            contextNodes.push(frozenNode);
            changesLog.add(ItemState.createAddedState(frozenNode));
         }

      }
      else if (action == OnParentVersionAction.INITIALIZE)
      {
         // 8.2.11.3 INITIALIZE
         // On checkin of N, a new node C will be created and placed in version
         // storage as a child of VN. This new C will be initialized just as it
         // would be if created normally in a workspace. No state information
         // of the current C in the workspace is preserved.
         frozenNode =
            TransientNodeData.createNodeData(currentNode(), qname, node.getPrimaryTypeName(), node.getQPath()
               .getIndex());
         contextNodes.push(null);
         changesLog.add(ItemState.createAddedState(frozenNode));
      }
      else if (action == OnParentVersionAction.COMPUTE)
      {
         // 8.2.11.4 COMPUTE
         // On checkin of N, a new node C will be created and placed in version
         // storage as a child of VN. This new C will be initialized by some
         // procedure defined for that type of child node.
         // [PN] 10.04.06 Creating simply as new node with same name and same node
         // type
         frozenNode =
            TransientNodeData.createNodeData(currentNode(), qname, node.getPrimaryTypeName(), node.getQPath()
               .getIndex());
         contextNodes.push(null);
         changesLog.add(ItemState.createAddedState(frozenNode));
      }
      else
      {
         throw new RepositoryException("Unknown onParentVersion type " + action);
      }
   }

   @Override
   protected void leaving(PropertyData property, int level) throws RepositoryException
   {
   }

   @Override
   protected void leaving(NodeData node, int level) throws RepositoryException
   {
      contextNodes.pop();
   }

   private NodeData currentNode()
   {
      return contextNodes.peek();
   }

   @Override
   public SessionDataManager getDataManager()
   {
      return dataManager;
   }

}