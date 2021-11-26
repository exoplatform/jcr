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

package org.exoplatform.services.jcr.impl.core.nodetype.registration;

import org.exoplatform.services.jcr.core.nodetype.NodeDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataImpl;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionData;
import org.exoplatform.services.jcr.dataflow.DataManager;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public class NodeTypeDefinitionAccessProvider extends AbstractItemDefinitionAccessProvider
{

   private PropertyDefinitionAccessProvider propertyDefinitionAccessProvider;

   private NodeDefinitionAccessProvider nodeDefinitionAccessProvider;

   /**
    * @param dataManager
    */
   public NodeTypeDefinitionAccessProvider(DataManager dataManager)
   {
      super(dataManager);
      this.propertyDefinitionAccessProvider = new PropertyDefinitionAccessProvider(dataManager);
      this.nodeDefinitionAccessProvider = new NodeDefinitionAccessProvider(dataManager);
   }

   public NodeTypeData read(NodeData nodeData) throws RepositoryException

   {

      //return new LazyLoadNodeTypeData(nodeData, this);
      return readNow(nodeData);
   }

   /**
    * @param nodeData
    * @return
    * @throws RepositoryException
    */
   public NodeTypeData readNow(NodeData nodeData) throws RepositoryException

   {
      List<PropertyData> props = dataManager.getChildPropertiesData(nodeData);
      Map<InternalQName, PropertyData> mapProps =new HashMap<InternalQName, PropertyData>();

      for (final PropertyData propertyData : props)
      {
         mapProps.put(propertyData.getQPath().getName(), propertyData);
      }
      InternalQName name = readMandatoryName(nodeData, mapProps.get(Constants.JCR_NODETYPENAME), Constants.JCR_NODETYPENAME);
      InternalQName primaryItemName = readName(nodeData, mapProps.get(Constants.JCR_PRIMARYITEMNAME), Constants.JCR_PRIMARYITEMNAME);
      boolean mixin = readMandatoryBoolean(nodeData, mapProps.get(Constants.JCR_ISMIXIN), Constants.JCR_ISMIXIN);
      boolean hasOrderableChildNodes = readMandatoryBoolean(nodeData, mapProps.get(Constants.JCR_HASORDERABLECHILDNODES), Constants.JCR_HASORDERABLECHILDNODES);
      InternalQName[] declaredSupertypeNames = readNames(nodeData, mapProps.get(Constants.JCR_SUPERTYPES), Constants.JCR_SUPERTYPES);

      List<PropertyDefinitionData> propertyDefinitionDataList = new ArrayList<PropertyDefinitionData>();
      List<NodeDefinitionData> nodeDefinitionDataList = new ArrayList<NodeDefinitionData>();

      List<NodeData> childDefinitions = dataManager.getChildNodesData(nodeData);
      for (NodeData childDefinition : childDefinitions)
      {
         List<PropertyData> childrenProps = dataManager.getChildPropertiesData(childDefinition);
         if (Constants.NT_PROPERTYDEFINITION.equals(childDefinition.getPrimaryTypeName()))
         {
            propertyDefinitionDataList.add(propertyDefinitionAccessProvider.read(childDefinition,childrenProps, name));
         }
         else if (Constants.NT_CHILDNODEDEFINITION.equals(childDefinition.getPrimaryTypeName()))
         {
            nodeDefinitionDataList.add(nodeDefinitionAccessProvider.read(childDefinition,childrenProps, name));
         }
      }

      return new NodeTypeDataImpl(name, primaryItemName, mixin, hasOrderableChildNodes, declaredSupertypeNames,
         propertyDefinitionDataList.toArray(new PropertyDefinitionData[propertyDefinitionDataList.size()]),
         nodeDefinitionDataList.toArray(new NodeDefinitionData[nodeDefinitionDataList.size()]));
   }

   /**
    * Read node NodeDefinitionData[]  of node type
    * @param nodeData
    * @return
    * @throws RepositoryException 
    * @throws NodeTypeReadException 
    * @throws RepositoryException
    */
   public NodeDefinitionData[] readNodeDefinitions(NodeData nodeData) throws NodeTypeReadException, RepositoryException
   {
      InternalQName name = null;
      List<NodeDefinitionData> nodeDefinitionDataList;

      List<NodeData> childDefinitions = dataManager.getChildNodesData(nodeData);

      if (childDefinitions.size() > 0)
         name = readMandatoryName(nodeData, null, Constants.JCR_NODETYPENAME);
      else
         return new NodeDefinitionData[0];

      nodeDefinitionDataList = new ArrayList<NodeDefinitionData>();
      for (NodeData childDefinition : childDefinitions)
      {
         if (Constants.NT_CHILDNODEDEFINITION.equals(childDefinition.getPrimaryTypeName()))
         {
            nodeDefinitionDataList.add(nodeDefinitionAccessProvider.read(childDefinition, name));
         }
      }

      return nodeDefinitionDataList.toArray(new NodeDefinitionData[nodeDefinitionDataList.size()]);
   }

   /**
    * Read PropertyDefinitionData of node type. 
    * @param nodeData
    * @return
    * @throws RepositoryException 
    * @throws NodeTypeReadException 
    * @throws RepositoryException
    */
   public PropertyDefinitionData[] readPropertyDefinitions(NodeData nodeData) throws NodeTypeReadException,
      RepositoryException
   {

      List<PropertyDefinitionData> propertyDefinitionDataList;

      List<NodeData> childDefinitions = dataManager.getChildNodesData(nodeData);
      InternalQName name = null;
      if (childDefinitions.size() > 0)
         name = readMandatoryName(nodeData, null, Constants.JCR_NODETYPENAME);
      else
         return new PropertyDefinitionData[0];
      propertyDefinitionDataList = new ArrayList<PropertyDefinitionData>();

      for (NodeData childDefinition : childDefinitions)
      {
         if (Constants.NT_PROPERTYDEFINITION.equals(childDefinition.getPrimaryTypeName()))
         {
            propertyDefinitionDataList.add(propertyDefinitionAccessProvider.read(childDefinition, name));
         }
      }

      return propertyDefinitionDataList.toArray(new PropertyDefinitionData[propertyDefinitionDataList.size()]);
   }

   /**
    * @param changesLog
    * @param nodeTypeStorageRoot
    * @param nodeType
    * @throws RepositoryException
    * @throws NodeTypeReadException 
    */
   public void write(PlainChangesLog changesLog, NodeData nodeTypeStorageRoot, NodeTypeData nodeType)
      throws NodeTypeReadException, RepositoryException
   {

      NodeData ntNode =
         TransientNodeData.createNodeData(nodeTypeStorageRoot, nodeType.getName(), Constants.NT_NODETYPE);

      changesLog.add(ItemState.createAddedState(ntNode));

      writeName(changesLog, ntNode, Constants.JCR_PRIMARYTYPE, ntNode.getPrimaryTypeName());

      // jcr:nodeTypeName
      writeName(changesLog, ntNode, Constants.JCR_NODETYPENAME, nodeType.getName());

      // jcr:isMixin
      writeBoolean(changesLog, ntNode, Constants.JCR_ISMIXIN, nodeType.isMixin());

      //      // jcr:isAbstract
      //      writeBoolean(changesLog, ntNode, Constants.JCR_ISABSTRACT, nodeType.isAbstract());
      //
      //      // jcr:isQueryable
      //      writeBoolean(changesLog, ntNode, Constants.JCR_ISQUERYABLE, nodeType.isQueryable());

      // jcr:hasOrderableChildNodes
      writeBoolean(changesLog, ntNode, Constants.JCR_HASORDERABLECHILDNODES, nodeType.hasOrderableChildNodes());

      if (nodeType.getPrimaryItemName() != null)
      {
         // jcr:primaryItemName
         writeName(changesLog, ntNode, Constants.JCR_PRIMARYITEMNAME, nodeType.getPrimaryItemName());
      }

      if (nodeType.getDeclaredSupertypeNames() != null && nodeType.getDeclaredSupertypeNames().length > 0)
      {
         // jcr:supertypes
         writeNames(changesLog, ntNode, Constants.JCR_SUPERTYPES, nodeType.getDeclaredSupertypeNames());
      }

      for (int i = 0; i < nodeType.getDeclaredPropertyDefinitions().length; i++)
      {

         propertyDefinitionAccessProvider
            .write(changesLog, ntNode, nodeType.getDeclaredPropertyDefinitions()[i], i + 1);

      }

      for (int i = 0; i < nodeType.getDeclaredChildNodeDefinitions().length; i++)
      {

         nodeDefinitionAccessProvider.write(changesLog, ntNode, nodeType.getDeclaredChildNodeDefinitions()[i], i + 1);

      }

   }
}
