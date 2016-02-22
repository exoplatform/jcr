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
import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.RepositoryException;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.PropertyDefinition;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public class NodeTypeDefinitionImpl
{
   /**
    * Class logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.NodeTypeDefinitionImpl");

   protected NodeTypeData nodeTypeData;

   protected final NodeTypeDataManager nodeTypeDataManager;

   protected final LocationFactory locationFactory;

   protected final ExtendedNodeTypeManager nodeTypeManager;

   protected final ValueFactory valueFactory;

   protected final ItemDataConsumer dataManager;

   /**
    * @param nodeTypeData
    * @param nodeTypeDataManager
    * @param nodeTypeManager
    * @param locationFactory
    * @param valueFactory
    * @param dataManager
    */
   public NodeTypeDefinitionImpl(NodeTypeData nodeTypeData, NodeTypeDataManager nodeTypeDataManager,
      ExtendedNodeTypeManager nodeTypeManager, LocationFactory locationFactory, ValueFactory valueFactory,
      ItemDataConsumer dataManager)
   {
      super();
      this.nodeTypeData = nodeTypeData;
      this.nodeTypeDataManager = nodeTypeDataManager;
      this.nodeTypeManager = nodeTypeManager;
      this.locationFactory = locationFactory;
      this.valueFactory = valueFactory;
      this.dataManager = dataManager;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
      {
         return true;
      }
      if (obj == null)
      {
         return false;
      }
      if (!(obj instanceof NodeTypeDefinitionImpl))
      {
         return false;
      }
      NodeTypeDefinitionImpl other = (NodeTypeDefinitionImpl)obj;
      if (nodeTypeData == null)
      {
         if (other.nodeTypeData != null)
         {
            return false;
         }
      }
      else if (!nodeTypeData.equals(other.nodeTypeData))
      {
         return false;
      }
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public NodeDefinition[] getDeclaredChildNodeDefinitions()
   {
      NodeDefinition[] result = new NodeDefinition[nodeTypeData.getDeclaredChildNodeDefinitions().length];
      for (int i = 0; i < nodeTypeData.getDeclaredChildNodeDefinitions().length; i++)
      {
         result[i] =
            new NodeDefinitionImpl(nodeTypeData.getDeclaredChildNodeDefinitions()[i], nodeTypeDataManager,
               nodeTypeManager, locationFactory, valueFactory, dataManager);
      }
      return result;
   }

   /**
    * {@inheritDoc}
    */
   public PropertyDefinition[] getDeclaredPropertyDefinitions()
   {
      PropertyDefinition[] result = new PropertyDefinition[nodeTypeData.getDeclaredPropertyDefinitions().length];
      for (int i = 0; i < nodeTypeData.getDeclaredPropertyDefinitions().length; i++)
      {
         result[i] =
            new PropertyDefinitionImpl(nodeTypeData.getDeclaredPropertyDefinitions()[i], nodeTypeDataManager,
               nodeTypeManager, locationFactory, valueFactory, dataManager);
      }
      return result;
   }

   /**
    * {@inheritDoc}
    */
   public String[] getDeclaredSupertypeNames()
   {
      String[] result = new String[nodeTypeData.getDeclaredSupertypeNames().length];
      try
      {
         for (int i = 0; i < nodeTypeData.getDeclaredSupertypeNames().length; i++)
         {
            result[i] = locationFactory.createJCRName(nodeTypeData.getDeclaredSupertypeNames()[i]).getAsString();
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
   public String getName()
   {
      String result = "";
      try
      {
         result = locationFactory.createJCRName(nodeTypeData.getName()).getAsString();
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
   public String getPrimaryItemName()
   {
      String result = "";
      try
      {
         result = locationFactory.createJCRName(nodeTypeData.getPrimaryItemName()).getAsString();
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
   @Override
   public int hashCode()
   {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((nodeTypeData == null) ? 0 : nodeTypeData.hashCode());
      return result;
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasOrderableChildNodes()
   {
      return nodeTypeData.hasOrderableChildNodes();
   }

   //   /**
   //    * {@inheritDoc}
   //    */
   //   public boolean isAbstract()
   //   {
   //      return nodeTypeData.isAbstract();
   //   }

   /**
    * {@inheritDoc}
    */
   public boolean isMixin()
   {
      return nodeTypeData.isMixin();
   }

   //   /**
   //    * {@inheritDoc}
   //    */
   //   public boolean isQueryable()
   //   {
   //      return nodeTypeData.isQueryable();
   //   }

}
