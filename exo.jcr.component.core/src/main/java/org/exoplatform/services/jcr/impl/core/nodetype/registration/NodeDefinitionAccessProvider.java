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
import org.exoplatform.services.jcr.dataflow.DataManager;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.version.OnParentVersionAction;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public class NodeDefinitionAccessProvider extends AbstractItemDefinitionAccessProvider
{
   /**
    * @param dataManager
    */
   public NodeDefinitionAccessProvider(DataManager dataManager)
   {
      super(dataManager);

   }

   /**
    * @param childDefinition
    * @param declaringNodeType
    * @return
    * @throws RepositoryException 
    * @throws NodeTypeReadException 
    * @throws RepositoryException
    */
   public NodeDefinitionData read(NodeData childDefinition, InternalQName declaringNodeType)
      throws NodeTypeReadException, RepositoryException

   {
      // null if residual
      InternalQName name = readName(childDefinition, null, Constants.JCR_NAME);

      boolean autoCreated = readMandatoryBoolean(childDefinition, null, Constants.JCR_AUTOCREATED);

      boolean mandatory = readMandatoryBoolean(childDefinition, null, Constants.JCR_MANDATORY);

      int onParentVersion =
         OnParentVersionAction.valueFromName(readMandatoryString(childDefinition, Constants.JCR_ONPARENTVERSION));

      boolean protectedItem = readMandatoryBoolean(childDefinition, null, Constants.JCR_PROTECTED);

      InternalQName[] requiredPrimaryTypes = readNames(childDefinition, null, Constants.JCR_REQUIREDPRIMARYTYPES);

      InternalQName defaultPrimaryType = readName(childDefinition, null, Constants.JCR_DEFAULTPRIMNARYTYPE);

      boolean allowsSameNameSiblings = readMandatoryBoolean(childDefinition, null, Constants.JCR_SAMENAMESIBLINGS);

      return new NodeDefinitionData(name, declaringNodeType, autoCreated, mandatory, onParentVersion, protectedItem,
         requiredPrimaryTypes, defaultPrimaryType, allowsSameNameSiblings);
   }

   /**
    * @param childDefinition
    * @param declaringNodeType
    * @param props
    * @return
    * @throws RepositoryException
    * @throws NodeTypeReadException
    * @throws RepositoryException
    */
   public NodeDefinitionData read(NodeData childDefinition, List<PropertyData> props, InternalQName declaringNodeType)
      throws NodeTypeReadException, RepositoryException
   {
      Map<InternalQName, PropertyData> mapProps = new HashMap<InternalQName, PropertyData>();

      for (final PropertyData propertyData : props)
      {
         mapProps.put(propertyData.getQPath().getName(), propertyData);
      }

      InternalQName name = readName(childDefinition, mapProps.get(Constants.JCR_NAME), Constants.JCR_NAME);
      boolean autoCreated = readMandatoryBoolean(childDefinition, mapProps.get(Constants.JCR_AUTOCREATED), Constants.JCR_AUTOCREATED);
      boolean mandatory = readMandatoryBoolean(childDefinition, mapProps.get(Constants.JCR_MANDATORY), Constants.JCR_MANDATORY);
      int onParentVersion =
         OnParentVersionAction.valueFromName(readMandatoryString(childDefinition, mapProps.get(Constants.JCR_ONPARENTVERSION), Constants.JCR_ONPARENTVERSION));
      boolean protectedItem = readMandatoryBoolean(childDefinition, mapProps.get(Constants.JCR_PROTECTED), Constants.JCR_PROTECTED);
      InternalQName[] requiredPrimaryTypes = readNames(childDefinition, mapProps.get(Constants.JCR_REQUIREDPRIMARYTYPES), Constants.JCR_REQUIREDPRIMARYTYPES);
      InternalQName defaultPrimaryType = readName(childDefinition, mapProps.get(Constants.JCR_DEFAULTPRIMNARYTYPE), Constants.JCR_DEFAULTPRIMNARYTYPE);
      boolean allowsSameNameSiblings = readMandatoryBoolean(childDefinition, mapProps.get(Constants.JCR_SAMENAMESIBLINGS), Constants.JCR_SAMENAMESIBLINGS);

      return new NodeDefinitionData(name, declaringNodeType, autoCreated, mandatory, onParentVersion, protectedItem,
         requiredPrimaryTypes, defaultPrimaryType, allowsSameNameSiblings);
   }

   public void write(PlainChangesLog changesLog, NodeData ntNode, NodeDefinitionData nodeDefinitionData, int index)
   {

      NodeData childNodesDefinition =
         TransientNodeData.createNodeData(ntNode, Constants.JCR_CHILDNODEDEFINITION, Constants.NT_CHILDNODEDEFINITION,
            index);

      changesLog.add(ItemState.createAddedState(childNodesDefinition));

      writeItemDefinition(changesLog, childNodesDefinition, nodeDefinitionData);

      writeName(changesLog, childNodesDefinition, Constants.JCR_PRIMARYTYPE, childNodesDefinition.getPrimaryTypeName());

      writeBoolean(changesLog, childNodesDefinition, Constants.JCR_SAMENAMESIBLINGS, nodeDefinitionData
         .isAllowsSameNameSiblings());

      if (nodeDefinitionData.getDefaultPrimaryType() != null)
      { // Mandatory false

         writeName(changesLog, childNodesDefinition, Constants.JCR_DEFAULTPRIMNARYTYPE, nodeDefinitionData
            .getDefaultPrimaryType());
      }

      if (nodeDefinitionData.getRequiredPrimaryTypes() != null
         && nodeDefinitionData.getRequiredPrimaryTypes().length != 0)
      {
         writeNames(changesLog, childNodesDefinition, Constants.JCR_REQUIREDPRIMARYTYPES, nodeDefinitionData
            .getRequiredPrimaryTypes());
      }
   }

}
