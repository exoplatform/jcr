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
package org.exoplatform.services.jcr.impl.core.nodetype;

import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.RepositoryException;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeType;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public class NodeDefinitionImpl extends ItemDefinitionImpl implements ExtendedNodeDefinition
{

   /**
    * Class logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.NodeDefinitionImpl");

   private final NodeDefinitionData nodeDefinitionData;

   /**
    * @param itemDefinitionData
    * @param nodeTypeDataManager
    * @param nodeTypeManager
    * @param locationFactory
    * @param valueFactory
    */
   public NodeDefinitionImpl(NodeDefinitionData nodeDefinitionData, NodeTypeDataManager nodeTypeDataManager,
      ExtendedNodeTypeManager nodeTypeManager, LocationFactory locationFactory, ValueFactory valueFactory,
      ItemDataConsumer dataManager)
   {
      super(nodeDefinitionData, nodeTypeDataManager, nodeTypeManager, locationFactory, valueFactory, dataManager);
      this.nodeDefinitionData = nodeDefinitionData;
   }

   /**
    * @param itemDefinitionData
    * @param nodeTypeDataManager
    * @param locationFactory
    * @param name
    */

   /**
    * {@inheritDoc}
    */
   public boolean allowsSameNameSiblings()
   {
      return nodeDefinitionData.isAllowsSameNameSiblings();
   }

   /**
    * {@inheritDoc}
    */
   public NodeType getDefaultPrimaryType()
   {
      if (nodeDefinitionData.getDefaultPrimaryType() == null)
         return null;
      return new NodeTypeImpl(nodeTypeDataManager.getNodeType(nodeDefinitionData.getDefaultPrimaryType()),
         nodeTypeDataManager, nodeTypeManager, locationFactory, valueFactory, dataManager);
   }

   /**
    * {@inheritDoc}
    */
   public String getDefaultPrimaryTypeName()
   {
      String result = null;
      if (nodeDefinitionData.getDefaultPrimaryType() != null)
      {
         try
         {
            result = locationFactory.createJCRName(nodeDefinitionData.getDefaultPrimaryType()).getAsString();
         }
         catch (RepositoryException e)
         {
            LOG.error(e.getLocalizedMessage(), e);
         }
      }
      return result;
   }

   /**
    * {@inheritDoc}
    */
   public InternalQName getDefaultPrimaryTypeQName()
   {
      return nodeDefinitionData.getDefaultPrimaryType();
   }

   /**
    * {@inheritDoc}
    */
   public String[] getRequiredPrimaryTypeNames()
   {
      InternalQName[] requiredPrimaryTypes = nodeDefinitionData.getRequiredPrimaryTypes();
      String[] result = new String[requiredPrimaryTypes.length];
      try
      {
         for (int i = 0; i < requiredPrimaryTypes.length; i++)
         {
            result[i] = locationFactory.createJCRName(requiredPrimaryTypes[i]).getAsString();
         }

      }
      catch (RepositoryException e)
      {
         LOG.error(e.getLocalizedMessage(), e);
      }
      return result;
   }

   /**
    * {@inheritDoc}
    */

   public InternalQName[] getRequiredPrimaryTypeQNames()
   {
      return nodeDefinitionData.getRequiredPrimaryTypes();
   }

   /**
    * {@inheritDoc}
    */
   public NodeType[] getRequiredPrimaryTypes()
   {
      InternalQName[] requiredPrimaryTypes = nodeDefinitionData.getRequiredPrimaryTypes();
      NodeType[] result = new NodeType[requiredPrimaryTypes.length];
      for (int i = 0; i < requiredPrimaryTypes.length; i++)
      {
         NodeTypeData ntData = nodeTypeDataManager.getNodeType(requiredPrimaryTypes[i]);
         if (ntData == null)
         {
            LOG.error("NODE TYPE NOT FOUND " + requiredPrimaryTypes[i].getAsString());
         }
         else
            result[i] =
               new NodeTypeImpl(ntData, nodeTypeDataManager, nodeTypeManager, locationFactory, valueFactory,
                  dataManager);
      }
      return result;
   }
}
