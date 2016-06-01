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

import org.exoplatform.services.jcr.core.ExtendedPropertyType;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionData;
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
 * @version $Id$
 */
public class PropertyDefinitionAccessProvider extends AbstractItemDefinitionAccessProvider
{

   public PropertyDefinitionAccessProvider(DataManager dataManager)
   {
      super(dataManager);
   }

   public PropertyDefinitionData read(NodeData nodeData, InternalQName declaringNodeType) throws NodeTypeReadException,
      RepositoryException
   {
      if (Constants.NT_PROPERTYDEFINITION.equals(nodeData.getPrimaryTypeName()))
      {
         // null if residual;
         InternalQName name = readName(nodeData, null, Constants.JCR_NAME);
         boolean protectedItem = readMandatoryBoolean(nodeData, null, Constants.JCR_PROTECTED);
         boolean autoCreated = readMandatoryBoolean(nodeData, null, Constants.JCR_AUTOCREATED);
         boolean mandatory = readMandatoryBoolean(nodeData, null, Constants.JCR_MANDATORY);
         int onParentVersion =
            OnParentVersionAction.valueFromName(readMandatoryString(nodeData, Constants.JCR_ONPARENTVERSION));

         int requiredType =
            ExtendedPropertyType.valueFromName(readMandatoryString(nodeData, Constants.JCR_REQUIREDTYPE));

         boolean multiple = readMandatoryBoolean(nodeData, null, Constants.JCR_MULTIPLE);

         String[] valueConstraints = readStrings(nodeData, null, Constants.JCR_VALUECONSTRAINTS);
         String[] defaultValues = readStrings(nodeData, null, Constants.JCR_DEFAULTVALUES);

         return new PropertyDefinitionData(name, declaringNodeType, autoCreated, mandatory, onParentVersion,
            protectedItem, requiredType, valueConstraints, defaultValues, multiple);
      }
      return null;
   }

   public PropertyDefinitionData read(NodeData nodeData, List<PropertyData> props, InternalQName declaringNodeType) throws NodeTypeReadException,
      RepositoryException
   {
      Map<InternalQName, PropertyData> mapProps = new HashMap<InternalQName, PropertyData>();

      for (final PropertyData propertyData : props)
      {
         mapProps.put(propertyData.getQPath().getName(), propertyData);
      }

      if (Constants.NT_PROPERTYDEFINITION.equals(nodeData.getPrimaryTypeName()))
      {
         InternalQName name = readName(nodeData, mapProps.get(Constants.JCR_NAME), Constants.JCR_NAME);
         boolean protectedItem = readMandatoryBoolean(nodeData, mapProps.get(Constants.JCR_PROTECTED), Constants.JCR_PROTECTED);
         boolean autoCreated = readMandatoryBoolean(nodeData, mapProps.get(Constants.JCR_AUTOCREATED), Constants.JCR_AUTOCREATED);
         boolean mandatory = readMandatoryBoolean(nodeData, mapProps.get(Constants.JCR_MANDATORY), Constants.JCR_MANDATORY);
         int onParentVersion =
            OnParentVersionAction.valueFromName(readMandatoryString(nodeData, mapProps.get(Constants.JCR_ONPARENTVERSION), Constants.JCR_ONPARENTVERSION));
         int requiredType =
            ExtendedPropertyType.valueFromName(readMandatoryString(nodeData, mapProps.get(Constants.JCR_REQUIREDTYPE), Constants.JCR_REQUIREDTYPE));
         boolean multiple = readMandatoryBoolean(nodeData, mapProps.get(Constants.JCR_MULTIPLE), Constants.JCR_MULTIPLE);
         String[] valueConstraints = readStrings(nodeData, mapProps.get(Constants.JCR_VALUECONSTRAINTS), Constants.JCR_VALUECONSTRAINTS);
         String[] defaultValues = readStrings(nodeData, mapProps.get(Constants.JCR_DEFAULTVALUES), Constants.JCR_DEFAULTVALUES);

         return new PropertyDefinitionData(name, declaringNodeType, autoCreated, mandatory, onParentVersion,
            protectedItem, requiredType, valueConstraints, defaultValues, multiple);
      }
      return null;
   }

   public void write(PlainChangesLog changesLog, NodeData declaredNodeType,
      PropertyDefinitionData propertyDefinitionData, int index)
   {
      NodeData propertyDefinition =
         TransientNodeData.createNodeData(declaredNodeType, Constants.JCR_PROPERTYDEFINITION,
            Constants.NT_PROPERTYDEFINITION, index);
      changesLog.add(ItemState.createAddedState(propertyDefinition));

      writeItemDefinition(changesLog, propertyDefinition, propertyDefinitionData);

      writeName(changesLog, propertyDefinition, Constants.JCR_PRIMARYTYPE, propertyDefinition.getPrimaryTypeName());

      writeString(changesLog, propertyDefinition, Constants.JCR_REQUIREDTYPE, ExtendedPropertyType
         .nameFromValue(propertyDefinitionData.getRequiredType()));

      writeBoolean(changesLog, propertyDefinition, Constants.JCR_MULTIPLE, propertyDefinitionData.isMultiple());

      //      writeBoolean(changesLog, propertyDefinition, Constants.JCR_QUERYORDERABLE, propertyDefinitionData
      //         .isQueryOrderable());
      //      writeStrings(changesLog, propertyDefinition, Constants.JCR_AVAILABLEQUERYOPERATORS, propertyDefinitionData
      //         .getAvailableQueryOperators());
      //      writeBoolean(changesLog, propertyDefinition, Constants.JCR_ISFULLTEXTSEARCHABLE, propertyDefinitionData
      //         .isFullTextSearchable());

      if (propertyDefinitionData.getValueConstraints() != null
         && propertyDefinitionData.getValueConstraints().length != 0)
      {

         writeStrings(changesLog, propertyDefinition, Constants.JCR_VALUECONSTRAINTS, propertyDefinitionData
            .getValueConstraints());
      }

      if (propertyDefinitionData.getDefaultValues() != null && propertyDefinitionData.getDefaultValues().length != 0)
      {

         writeStrings(changesLog, propertyDefinition, Constants.JCR_DEFAULTVALUES, propertyDefinitionData
            .getDefaultValues());
      }
   }
}
