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
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionData;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.PropertyDefinition;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public class PropertyDefinitionImpl extends ItemDefinitionImpl implements PropertyDefinition
{

   /**
    * Class logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.PropertyDefinitionImpl");

   private final PropertyDefinitionData propertyDefinitionData;

   /**
    * @param propertyDefinitionData
    * @param nodeTypeDataManager
    * @param nodeTypeManager
    * @param locationFactory
    * @param valueFactory
    * @param dataManager
    */
   public PropertyDefinitionImpl(PropertyDefinitionData propertyDefinitionData,
      NodeTypeDataManager nodeTypeDataManager, ExtendedNodeTypeManager nodeTypeManager,
      LocationFactory locationFactory, ValueFactory valueFactory, ItemDataConsumer dataManager)
   {
      super(propertyDefinitionData, nodeTypeDataManager, nodeTypeManager, locationFactory, valueFactory, dataManager);
      this.propertyDefinitionData = propertyDefinitionData;
   }

   /**
    * {@inheritDoc}
    */
   public Value[] getDefaultValues()
   {
      String[] defaultValues = propertyDefinitionData.getDefaultValues();
      if (defaultValues == null)
         return null;
      Value[] vals = new Value[defaultValues.length];
      for (int i = 0; i < defaultValues.length; i++)
      {
         if (propertyDefinitionData.getRequiredType() == PropertyType.UNDEFINED)
         {
            vals[i] = valueFactory.createValue(defaultValues[i]);
         }
         else
            try
            {
               vals[i] = valueFactory.createValue(defaultValues[i], propertyDefinitionData.getRequiredType());
            }
            catch (ValueFormatException e)
            {
               LOG.error(e.getLocalizedMessage(), e);
            }
      }
      return vals;
   }

   public int getRequiredType()
   {
      return propertyDefinitionData.getRequiredType();
   }

   public String[] getValueConstraints()
   {
      return propertyDefinitionData.getValueConstraints();
   }

   public boolean isMultiple()
   {
      return propertyDefinitionData.isMultiple();
   }
   //
   //   /**
   //    * {@inheritDoc}
   //    */
   //   public String[] getAvailableQueryOperators()
   //   {
   //      return propertyDefinitionData.getAvailableQueryOperators();
   //   }
   //
   //   /**
   //    * {@inheritDoc}
   //    */
   //   public boolean isFullTextSearchable()
   //   {
   //      return propertyDefinitionData.isFullTextSearchable();
   //   }
   //
   //   /**
   //    * {@inheritDoc}
   //    */
   //   public boolean isQueryOrderable()
   //   {
   //      return propertyDefinitionData.isQueryOrderable();
   //   }

}
