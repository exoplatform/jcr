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
import org.exoplatform.services.jcr.core.nodetype.ItemDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.impl.Constants;
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
public class ItemDefinitionImpl implements ExtendedItemDefinition
{
   /**
    * Class logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.ItemDefinitionImpl");

   protected final NodeTypeDataManager nodeTypeDataManager;

   protected final LocationFactory locationFactory;

   protected final ExtendedNodeTypeManager nodeTypeManager;

   protected final ValueFactory valueFactory;

   private final ItemDefinitionData itemDefinitionData;

   /**
    * @param name
    * @param declaringNodeType
    * @param onParentVersion
    * @param autoCreated
    * @param mandatory
    * @param protectedItem
    */
   public ItemDefinitionImpl(ItemDefinitionData itemDefinitionData, NodeTypeDataManager nodeTypeDataManager,
      ExtendedNodeTypeManager nodeTypeManager, LocationFactory locationFactory, ValueFactory valueFactory)
   {

      this.itemDefinitionData = itemDefinitionData;
      this.nodeTypeDataManager = nodeTypeDataManager;
      this.nodeTypeManager = nodeTypeManager;
      this.locationFactory = locationFactory;
      this.valueFactory = valueFactory;
   }

   /**
    * {@inheritDoc}
    */
   public NodeType getDeclaringNodeType()
   {

      return new NodeTypeImpl(nodeTypeDataManager.getNodeType(itemDefinitionData.getDeclaringNodeType()),
         nodeTypeDataManager, nodeTypeManager, locationFactory, valueFactory);
   }

   /**
    * {@inheritDoc}
    */
   public String getName()
   {
      String result = "";
      try
      {
         result = locationFactory.createJCRName(itemDefinitionData.getName()).getAsString();
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
   public InternalQName getQName()
   {
      return itemDefinitionData.getName();
   }

   /**
    * {@inheritDoc}
    */
   public InternalQName getDeclaringNodeTypeQName()
   {
      return itemDefinitionData.getDeclaringNodeType();
   }

   /**
    * {@inheritDoc}
    */
   public int getOnParentVersion()
   {
      return itemDefinitionData.getOnParentVersion();
   }

   /**
    * {@inheritDoc}
    */
   public boolean isAutoCreated()
   {
      return itemDefinitionData.isAutoCreated();
   }

   /**
    * {@inheritDoc}
    */
   public boolean isMandatory()
   {
      return itemDefinitionData.isMandatory();
   }

   /**
    * {@inheritDoc}
    */
   public boolean isProtected()
   {
      return itemDefinitionData.isProtected();
   }

   /**
    * {@inheritDoc}
    */
   public boolean isResidualSet()
   {
      return getName().equals(Constants.JCR_ANY_NAME.getName());
   }
}
