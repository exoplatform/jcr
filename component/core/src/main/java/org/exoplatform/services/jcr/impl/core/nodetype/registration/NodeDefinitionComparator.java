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
package org.exoplatform.services.jcr.impl.core.nodetype.registration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;

import org.exoplatform.services.log.Log;

import org.exoplatform.services.jcr.core.nodetype.NodeDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.dataflow.DataManager;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.log.ExoLogger;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public class NodeDefinitionComparator
   extends AbstractDefinitionComparator<NodeDefinitionData>
{

   /**
    * Class logger.
    */
   private static final Log LOG = ExoLogger.getLogger(NodeDefinitionComparator.class);

   /**
    * @param nodeTypeDataManager
    * @param persister
    */
   public NodeDefinitionComparator(NodeTypeDataManager nodeTypeDataManager, DataManager persister)
   {
      super(nodeTypeDataManager, persister);
   }

   public PlainChangesLog compare(NodeTypeData registeredNodeType, NodeDefinitionData[] ancestorDefinition,
            NodeDefinitionData[] recipientDefinition) throws ConstraintViolationException, RepositoryException
   {

      List<NodeDefinitionData> sameDefinitionData = new ArrayList<NodeDefinitionData>();
      List<RelatedDefinition<NodeDefinitionData>> changedDefinitionData =
               new ArrayList<RelatedDefinition<NodeDefinitionData>>();
      List<NodeDefinitionData> newDefinitionData = new ArrayList<NodeDefinitionData>();
      List<NodeDefinitionData> removedDefinitionData = new ArrayList<NodeDefinitionData>();
      init(ancestorDefinition, recipientDefinition, sameDefinitionData, changedDefinitionData, newDefinitionData,
               removedDefinitionData);
      // create changes log
      PlainChangesLog changesLog = new PlainChangesLogImpl();
      // check removed

      validateRemoved(registeredNodeType, removedDefinitionData, recipientDefinition);

      Set<String> nodes = nodeTypeDataManager.getNodes(registeredNodeType.getName());

      validateAdded(registeredNodeType.getName(), newDefinitionData, nodes, recipientDefinition);
      // changed
      validateChanged(registeredNodeType.getName(), changedDefinitionData, nodes, recipientDefinition);

      //
      doAdd(newDefinitionData, changesLog, nodes, registeredNodeType);

      return changesLog;

   }

   /**
    * @param nodes
    * @param nodeDefinitionData
    * @throws RepositoryException
    * @throws ConstraintViolationException
    */
   private void checkMandatoryItems(Set<String> nodes, NodeDefinitionData nodeDefinitionData)
            throws RepositoryException, ConstraintViolationException
   {
      for (String uuid : nodes)
      {
         NodeData nodeData = (NodeData) persister.getItemData(uuid);
         ItemData child = persister.getItemData(nodeData, new QPathEntry(nodeDefinitionData.getName(), 0));
         if (child == null || !child.isNode())
         {
            throw new ConstraintViolationException("Fail to  add mandatory and not auto-created "
                     + "child node definition " + nodeDefinitionData.getName().getAsString() + " for "
                     + nodeDefinitionData.getDeclaringNodeType().getAsString() + " because node "
                     + nodeData.getQPath().getAsString() + " doesn't contains child node with name "
                     + nodeDefinitionData.getName().getAsString());

         }
      }
   }

   /**
    * @param registeredNodeType
    * @param nodes
    * @param ancestorDefinitionData
    * @param recipientDefinitionData
    * @throws RepositoryException
    */
   private void checkRequiredPrimaryType(InternalQName registeredNodeType, Set<String> nodes,
            InternalQName[] ancestorRequiredPrimaryTypes, NodeDefinitionData recipientDefinitionData,
            NodeDefinitionData[] allRecipientDefinition) throws RepositoryException
   {
      // Required type change
      InternalQName[] requiredPrimaryTypes = recipientDefinitionData.getRequiredPrimaryTypes();

      for (String uuid : nodes)
      {
         NodeData nodeData = (NodeData) persister.getItemData(uuid);
         if (recipientDefinitionData.getName().equals(Constants.JCR_ANY_NAME))
         {
            List<NodeData> childs = persister.getChildNodesData(nodeData);
            for (NodeData child : childs)
            {
               if (isResidualMatch(child.getQPath().getName(), allRecipientDefinition))
               {
                  for (int i = 0; i < requiredPrimaryTypes.length; i++)
                  {
                     if (!nodeTypeDataManager.isNodeType(requiredPrimaryTypes[i], child.getPrimaryTypeName(), child
                              .getMixinTypeNames()))
                     {
                        StringBuffer buffer = new StringBuffer();
                        buffer.append("Fail to change ");
                        buffer.append(recipientDefinitionData.getName().getAsString());
                        buffer.append(" node definition for ");
                        buffer.append(registeredNodeType.getAsString());
                        buffer.append("node type from ");
                        if (ancestorRequiredPrimaryTypes != null)
                           buffer.append(Arrays.toString(ancestorRequiredPrimaryTypes));
                        else
                           buffer.append(" '' ");
                        buffer.append(" to ");
                        buffer.append(Arrays.toString(recipientDefinitionData.getRequiredPrimaryTypes()));
                        buffer.append(" because ");
                        buffer.append(child.getQPath().getAsString());
                        buffer.append(" doesn't much ");
                        buffer.append(requiredPrimaryTypes[i].getAsString());
                        buffer.append(" as required primary type");
                        throw new ConstraintViolationException(buffer.toString());

                     }
                  }
               }
            }
         }
         else
         {
            List<NodeData> childs = persister.getChildNodesData(nodeData);
            for (NodeData child : childs)
            {
               if (child.getQPath().getName().equals(recipientDefinitionData.getName()))
               {
                  for (int i = 0; i < requiredPrimaryTypes.length; i++)
                  {
                     if (!nodeTypeDataManager.isNodeType(requiredPrimaryTypes[i], child.getPrimaryTypeName(), child
                              .getMixinTypeNames()))
                     {
                        StringBuffer buffer = new StringBuffer();
                        buffer.append("Fail to change ");
                        buffer.append(recipientDefinitionData.getName().getAsString());
                        buffer.append(" node definition for ");
                        buffer.append(registeredNodeType.getAsString());
                        buffer.append("node type from ");
                        if (ancestorRequiredPrimaryTypes != null)
                           buffer.append(Arrays.toString(ancestorRequiredPrimaryTypes));
                        else
                           buffer.append(" '' ");
                        buffer.append(" to ");
                        buffer.append(Arrays.toString(recipientDefinitionData.getRequiredPrimaryTypes()));
                        buffer.append(" because ");
                        buffer.append(child.getQPath().getAsString());
                        buffer.append("doesn't much ");
                        buffer.append(requiredPrimaryTypes[i].getAsString());
                        buffer.append(" as required primary type");
                        throw new ConstraintViolationException(buffer.toString());
                     }
                  }
               }
            }
         }
      }
   }

   /**
    * @param registeredNodeType
    * @param nodes
    * @param allRecipientDefinition
    * @param ancestorDefinitionData
    * @param recipientDefinitionData
    * @throws RepositoryException
    */
   private void checkSameNameSibling(InternalQName registeredNodeType, Set<String> nodes, InternalQName recipientName,
            NodeDefinitionData[] allRecipientDefinition) throws RepositoryException
   {

      for (String uuid : nodes)
      {
         NodeData nodeData = (NodeData) persister.getItemData(uuid);

         if (recipientName.equals(Constants.JCR_ANY_NAME))
         {
            // child of node type
            List<NodeData> childs = persister.getChildNodesData(nodeData);
            for (NodeData child : childs)
            {
               if (isResidualMatch(child.getQPath().getName(), allRecipientDefinition))
               {
                  List<NodeData> childs2 = persister.getChildNodesData(child);

                  for (NodeData child2 : childs2)
                  {
                     if (child2.getQPath().getIndex() > 1)
                     {
                        StringBuffer buffer = new StringBuffer();
                        buffer.append("Fail to change ");
                        buffer.append(recipientName.getAsString());
                        buffer.append(" node definition for ");
                        buffer.append(registeredNodeType.getAsString());
                        buffer.append("node type from AllowsSameNameSiblings = true to AllowsSameNameSiblings = false");
                        buffer.append(" because ");
                        buffer.append(child.getQPath().getAsString());
                        buffer.append(" contains more then one child with name");
                        buffer.append(child2.getQPath().getName().getAsString());
                        throw new ConstraintViolationException(buffer.toString());
                     }
                  }
               }
            }
         }
         else
         {

            // child of node type
            List<NodeData> childs = persister.getChildNodesData(nodeData);
            for (NodeData child : childs)
            {
               if (child.getQPath().getName().equals(recipientName))
               {
                  List<NodeData> childs2 = persister.getChildNodesData(child);

                  for (NodeData child2 : childs2)
                  {
                     if (child2.getQPath().getIndex() > 1)
                     {
                        StringBuffer buffer = new StringBuffer();
                        buffer.append("Fail to change ");
                        buffer.append(recipientName.getAsString());
                        buffer.append(" node definition for ");
                        buffer.append(registeredNodeType.getAsString());
                        buffer.append("node type from AllowsSameNameSiblings = true to AllowsSameNameSiblings = false");
                        buffer.append(" because ");
                        buffer.append(child.getQPath().getAsString());
                        buffer.append(" contains more then one child with name");
                        buffer.append(child2.getQPath().getName().getAsString());
                        throw new ConstraintViolationException(buffer.toString());
                     }
                  }
               }

            }
         }

      }

   }

   /**
    * @param toAddList
    * @param changesLog
    * @param nodes
    * @param registeredNodeType
    * @throws RepositoryException
    */
   private void doAdd(List<NodeDefinitionData> toAddList, PlainChangesLog changesLog, Set<String> nodes,
            NodeTypeData registeredNodeType) throws RepositoryException
   {

      for (String uuid : nodes)
      {
         NodeData nodeData = (NodeData) persister.getItemData(uuid);

         // added properties
         for (NodeDefinitionData newNodeDefinitionData : toAddList)
         {
            if (!newNodeDefinitionData.getName().equals(Constants.JCR_ANY_NAME)
                     && newNodeDefinitionData.isAutoCreated())
               changesLog.addAll(nodeTypeDataManager.makeAutoCreatedNodes(nodeData, registeredNodeType.getName(),
                        new NodeDefinitionData[]
                        {newNodeDefinitionData}, persister, nodeData.getACL().getOwner()).getAllStates());
         }
      }
   }

   /**
    * @param nodeTypeName
    * @param newDefinitionData
    * @param nodes
    * @param recipientDefinition
    * @throws RepositoryException
    */
   private void validateAdded(InternalQName nodeTypeName, List<NodeDefinitionData> newDefinitionData,
            Set<String> nodes, NodeDefinitionData[] recipientDefinition) throws RepositoryException
   {

      for (NodeDefinitionData nodeDefinitionData : newDefinitionData)
      {

         if (nodeDefinitionData.getName().equals(Constants.JCR_ANY_NAME))
         {

            checkRequiredPrimaryType(nodeTypeName, nodes, null, nodeDefinitionData, recipientDefinition);

            checkSameNameSibling(nodeTypeName, nodes, nodeDefinitionData.getName(), recipientDefinition);

         }
         else
         {
            // check existed nodes for new constraint
            checkRequiredPrimaryType(nodeTypeName, nodes, null, nodeDefinitionData, recipientDefinition);
            checkSameNameSibling(nodeTypeName, nodes, nodeDefinitionData.getName(), recipientDefinition);

            // try to add mandatory or auto-created properties for
            // for already addded nodes.
            if (nodeDefinitionData.isMandatory() && !nodeDefinitionData.isAutoCreated())
            {
               checkMandatoryItems(nodes, nodeDefinitionData);
            }
         }
      }

   }

   /**
    * @param registeredNodeType
    * @param changedDefinitionData
    * @param nodes
    * @param allRecipientDefinition
    * @throws RepositoryException
    */
   private void validateChanged(InternalQName registeredNodeType,
            List<RelatedDefinition<NodeDefinitionData>> changedDefinitionData, Set<String> nodes,
            NodeDefinitionData[] allRecipientDefinition) throws RepositoryException
   {
      for (RelatedDefinition<NodeDefinitionData> changedDefinitions : changedDefinitionData)
      {
         NodeDefinitionData ancestorDefinitionData = changedDefinitions.getAncestorDefinition();
         NodeDefinitionData recipientDefinitionData = changedDefinitions.getRecepientDefinition();
         // change from mandatory=false to mandatory = true
         // TODO residual
         if (!ancestorDefinitionData.isMandatory() && recipientDefinitionData.isMandatory())
         {
            for (String uuid : nodes)
            {
               NodeData nodeData = (NodeData) persister.getItemData(uuid);
               ItemData child = persister.getItemData(nodeData, new QPathEntry(recipientDefinitionData.getName(), 0));
               if (child == null || !child.isNode())
               {
                  String message =
                           "Can not change " + recipientDefinitionData.getName().getAsString()
                                    + " node definition for " + registeredNodeType.getAsString() + " node type "
                                    + " from mandatory=false to mandatory = true , because " + " node "
                                    + nodeData.getQPath().getAsString() + " doesn't have child node with name "
                                    + recipientDefinitionData.getName().getAsString();
                  throw new RepositoryException(message);
               }
            }
         }

         // change from Protected=false to Protected = true
         if (!ancestorDefinitionData.isProtected() && recipientDefinitionData.isProtected())
         {
            // TODO residual
            for (String uuid : nodes)
            {
               NodeData nodeData = (NodeData) persister.getItemData(uuid);
               ItemData child = persister.getItemData(nodeData, new QPathEntry(recipientDefinitionData.getName(), 0));
               if (child == null || !child.isNode())
               {
                  String message =
                           "Fail to  change " + recipientDefinitionData.getName().getAsString()
                                    + " node definition for " + registeredNodeType.getAsString()
                                    + " node type  from rotected=false to Protected = true , because " + " node "
                                    + nodeData.getQPath().getAsString() + " doesn't have child node with name "
                                    + recipientDefinitionData.getName().getAsString();
                  throw new RepositoryException(message);
               }
            }
         }
         if (!Arrays.deepEquals(ancestorDefinitionData.getRequiredPrimaryTypes(), recipientDefinitionData
                  .getRequiredPrimaryTypes()))
         {
            checkRequiredPrimaryType(registeredNodeType, nodes, ancestorDefinitionData.getRequiredPrimaryTypes(),
                     recipientDefinitionData, allRecipientDefinition);
         }
         // check sibling
         if (ancestorDefinitionData.isAllowsSameNameSiblings() && !recipientDefinitionData.isAllowsSameNameSiblings())
         {
            checkSameNameSibling(registeredNodeType, nodes, recipientDefinitionData.getName(), allRecipientDefinition);
         }
      }
   }

   /**
    * @param registeredNodeType
    * @param removedDefinitionData
    * @param recipientDefinition
    * @throws ConstraintViolationException
    * @throws RepositoryException
    */
   private void validateRemoved(NodeTypeData registeredNodeType, List<NodeDefinitionData> removedDefinitionData,
            NodeDefinitionData[] recipientDefinition) throws ConstraintViolationException, RepositoryException
   {

      for (NodeDefinitionData removeNodeDefinitionData : removedDefinitionData)
      {
         Set<String> nodes;
         if (removeNodeDefinitionData.getName().equals(Constants.JCR_ANY_NAME))
         {
            nodes = nodeTypeDataManager.getNodes(registeredNodeType.getName());
            for (String uuid : nodes)
            {
               NodeData nodeData = (NodeData) persister.getItemData(uuid);
               List<NodeData> childs = persister.getChildNodesData(nodeData);
               // more then mixin and primary type
               // TODO it could be possible, check add definitions
               if (childs.size() > 0)
               {
                  for (NodeData nodeData2 : childs)
                  {
                     if (!isNonResidualMatch(nodeData2.getQPath().getName(), recipientDefinition))
                     {
                        String msg =
                                 "Can't remove node definition " + removeNodeDefinitionData.getName().getAsString()
                                          + "  for " + registeredNodeType.getName().getAsString()
                                          + " node type because node " + nodeData.getQPath().getAsString() + " "
                                          + " countains child nodes with name "
                                          + nodeData2.getQPath().getName().getAsString();
                        throw new ConstraintViolationException(msg);
                     }
                  }
               }
            }
         }
         else
         {
            if (!isResidualMatch(removeNodeDefinitionData.getName(), recipientDefinition))
            {
               nodes = nodeTypeDataManager.getNodes(registeredNodeType.getName());
               for (String uuid : nodes)
               {
                  NodeData nodeData = (NodeData) persister.getItemData(uuid);
                  ItemData child =
                           persister.getItemData(nodeData, new QPathEntry(removeNodeDefinitionData.getName(), 0));
                  if (child != null && child.isNode())
                  {
                     throw new ConstraintViolationException("Can't remove node definition "
                              + removeNodeDefinitionData.getName().getAsString() + "  for "
                              + registeredNodeType.getName().getAsString() + " node type because node "
                              + nodeData.getQPath().getAsString() + " " + " countains child node with name "
                              + child.getQPath().getName().getAsString());

                  }
               }
            }
         }
      }
   }
}
