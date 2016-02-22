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
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;

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
      InternalQName name = readName(childDefinition, Constants.JCR_NAME);

      boolean autoCreated = readMandatoryBoolean(childDefinition, Constants.JCR_AUTOCREATED);

      boolean mandatory = readMandatoryBoolean(childDefinition, Constants.JCR_MANDATORY);

      int onParentVersion =
         OnParentVersionAction.valueFromName(readMandatoryString(childDefinition, Constants.JCR_ONPARENTVERSION));

      boolean protectedItem = readMandatoryBoolean(childDefinition, Constants.JCR_PROTECTED);

      InternalQName[] requiredPrimaryTypes = readNames(childDefinition, Constants.JCR_REQUIREDPRIMARYTYPES);

      InternalQName defaultPrimaryType = readName(childDefinition, Constants.JCR_DEFAULTPRIMNARYTYPE);

      boolean allowsSameNameSiblings = readMandatoryBoolean(childDefinition, Constants.JCR_SAMENAMESIBLINGS);

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
