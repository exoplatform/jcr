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

import org.exoplatform.services.jcr.core.nodetype.NodeDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataImpl;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionData;
import org.exoplatform.services.jcr.dataflow.DataManager;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

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
    * @throws IOException
    * @throws IllegalNameException
    * @throws RepositoryException
    * @throws UnsupportedEncodingException
    */
   public NodeTypeData readNow(NodeData nodeData) throws RepositoryException

   {

      InternalQName name = readMandatoryName(nodeData, Constants.JCR_NODETYPENAME);

      InternalQName primaryItemName = readName(nodeData, Constants.JCR_PRIMARYITEMNAME);

      boolean mixin = readMandatoryBoolean(nodeData, Constants.JCR_ISMIXIN);

      boolean hasOrderableChildNodes = readMandatoryBoolean(nodeData, Constants.JCR_HASORDERABLECHILDNODES);
      // TODO fix to mandatory
      //      boolean isAbstract = readMandatoryBoolean(nodeData, Constants.JCR_ISABSTRACT);
      //
      //      boolean isQueryable = readBoolean(nodeData, Constants.JCR_ISQUERYABLE);

      InternalQName[] declaredSupertypeNames = readNames(nodeData, Constants.JCR_SUPERTYPES);

      List<PropertyDefinitionData> propertyDefinitionDataList = new ArrayList<PropertyDefinitionData>();
      List<NodeDefinitionData> nodeDefinitionDataList = new ArrayList<NodeDefinitionData>();

      List<NodeData> childDefinitions = dataManager.getChildNodesData(nodeData);
      for (NodeData childDefinition : childDefinitions)
      {
         if (Constants.NT_PROPERTYDEFINITION.equals(childDefinition.getPrimaryTypeName()))
         {
            propertyDefinitionDataList.add(propertyDefinitionAccessProvider.read(childDefinition, name));
         }
         else if (Constants.NT_CHILDNODEDEFINITION.equals(childDefinition.getPrimaryTypeName()))
         {
            nodeDefinitionDataList.add(nodeDefinitionAccessProvider.read(childDefinition, name));
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
    * @throws UnsupportedEncodingException
    * @throws RepositoryException
    * @throws IllegalNameException
    * @throws IOException
    */
   public NodeDefinitionData[] readNodeDefinitions(NodeData nodeData) throws NodeTypeReadException, RepositoryException
   {
      InternalQName name = null;
      List<NodeDefinitionData> nodeDefinitionDataList;

      List<NodeData> childDefinitions = dataManager.getChildNodesData(nodeData);

      if (childDefinitions.size() > 0)
         name = readMandatoryName(nodeData, Constants.JCR_NODETYPENAME);
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
    * @throws UnsupportedEncodingException
    * @throws RepositoryException
    * @throws IllegalNameException
    * @throws IOException
    */
   public PropertyDefinitionData[] readPropertyDefinitions(NodeData nodeData) throws NodeTypeReadException,
      RepositoryException
   {

      List<PropertyDefinitionData> propertyDefinitionDataList;

      List<NodeData> childDefinitions = dataManager.getChildNodesData(nodeData);
      InternalQName name = null;
      if (childDefinitions.size() > 0)
         name = readMandatoryName(nodeData, Constants.JCR_NODETYPENAME);
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
    * @param nodeData
    * @param nodeTypeData
    * @return
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
