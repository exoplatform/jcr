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

import org.exoplatform.services.jcr.core.nodetype.ItemDefinitionData;
import org.exoplatform.services.jcr.dataflow.DataManager;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.dataflow.ValueDataUtil;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.version.OnParentVersionAction;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: AbstractItemDefinitionAccessProvider.java 549 2009-11-10 15:25:10Z skabashnyuk $
 */
public abstract class AbstractItemDefinitionAccessProvider
{

   protected final DataManager dataManager;

   public AbstractItemDefinitionAccessProvider(DataManager dataManager)
   {
      super();
      this.dataManager = dataManager;
   }

   /**
    * Load values.
    * 
    * @param parentNode
    * @param propertyName
    * @return
    * @throws RepositoryException
    */
   protected List<ValueData> loadPropertyValues(NodeData parentNode, ItemData property , InternalQName propertyName)
      throws RepositoryException
   {
      if(property == null)
      {
          property = dataManager.getItemData(parentNode, new QPathEntry(propertyName, 1), ItemType.PROPERTY);
      }
      if (property != null)
      {
         if (property.isNode())
            throw new RepositoryException("Fail to load property " + propertyName + "not found for "
               + parentNode.getQPath().getAsString());
         return ((PropertyData)property).getValues();
      }
      return null;
   }

   public Boolean readBoolean(NodeData parentNode, PropertyData propertyData, InternalQName propertyName) throws RepositoryException,
      NodeTypeReadException
   {
      List<ValueData> values = loadPropertyValues(parentNode, propertyData, propertyName);
      if (values != null)
      {
         if (values.size() == 1)
         {
            try
            {
               return ValueDataUtil.getBoolean(values.get(0));
            }
            catch (IllegalStateException e)
            {
               throw new NodeTypeReadException(e.getLocalizedMessage(), e.getCause());
            }
         }
      }
      return new Boolean(false);
   }


   public Long readLong(NodeData parentNode, InternalQName propertyName) throws RepositoryException,
      NodeTypeReadException
   {
      List<ValueData> values = loadPropertyValues(parentNode, null, propertyName);
      if (values != null)
      {
         if (values.size() == 1)
         {
            try
            {
               return ValueDataUtil.getLong(values.get(0));
            }
            catch (NumberFormatException e)
            {
               throw new NodeTypeReadException(e.getLocalizedMessage(), e.getCause());
            }
            catch (IllegalStateException e)
            {
               throw new NodeTypeReadException(e.getLocalizedMessage(), e.getCause());
            }
         }
      }
      return null;
   }

   public Boolean readMandatoryBoolean(NodeData parentNode,PropertyData propertyData, InternalQName propertyName) throws RepositoryException,
      NodeTypeReadException
   {
      Boolean name = readBoolean(parentNode, propertyData, propertyName);
      if (name == null)
         throw new RepositoryException("Mandatory item " + propertyName + "not found for "
            + parentNode.getQPath().getAsString());
      return name;
   }

   public Long readMandatoryLong(NodeData parentNode, InternalQName propertyName) throws RepositoryException,
      NodeTypeReadException
   {
      Long name = readLong(parentNode, propertyName);
      if (name == null)
         throw new RepositoryException("Mandatory item " + propertyName + "not found for "
            + parentNode.getQPath().getAsString());
      return name;
   }

   public InternalQName readMandatoryName(NodeData parentNode, PropertyData propertyData, InternalQName propertyName) throws RepositoryException,
      NodeTypeReadException
   {
      InternalQName name = readName(parentNode, propertyData, propertyName);
      if (name == null)
         throw new RepositoryException("Mandatory item " + propertyName + "not found for "
            + parentNode.getQPath().getAsString());
      return name;
   }

   public InternalQName[] readMandatoryNames(NodeData parentNode, InternalQName propertyName)
      throws RepositoryException, NodeTypeReadException
   {
      InternalQName[] names = readNames(parentNode, null, propertyName);
      if (names == null)
         throw new RepositoryException("Mandatory item " + propertyName + "not found for "
            + parentNode.getQPath().getAsString());
      return names;
   }

   public String readMandatoryString(NodeData parentNode, InternalQName propertyName) throws RepositoryException,
      NodeTypeReadException
   {
      String name = readString(parentNode, null, propertyName);
      if (name == null)
         throw new RepositoryException("Mandatory item " + propertyName + "not found for "
            + parentNode.getQPath().getAsString());
      return name;
   }

   public String readMandatoryString(NodeData parentNode,PropertyData propertyData, InternalQName propertyName) throws RepositoryException,
      NodeTypeReadException
   {
      String name = readString(parentNode, propertyData, propertyName);
      if (name == null)
         throw new RepositoryException("Mandatory item " + propertyName + "not found for "
            + parentNode.getQPath().getAsString());
      return name;
   }

   public InternalQName readName(NodeData parentNode, PropertyData propertyData, InternalQName propertyName) throws RepositoryException,
      NodeTypeReadException
   {
      List<ValueData> values = loadPropertyValues(parentNode, propertyData, propertyName);
      if (values != null)
      {
         if (values.size() == 1)
         {
            try
            {
               return InternalQName.parse(ValueDataUtil.getString(values.get(0)));
            }
            catch (IllegalNameException e)
            {
               throw new NodeTypeReadException(e.getLocalizedMessage(), e.getCause());
            }
         }
      }
      return null;
   }

   public InternalQName[] readNames(NodeData parentNode, InternalQName propertyName) throws RepositoryException,
      NodeTypeReadException
   {
      List<ValueData> values = loadPropertyValues(parentNode, null, propertyName);
      if (values != null)
      {
         InternalQName[] result = new InternalQName[values.size()];
         int i = 0;
         for (ValueData valueData : values)
         {
            try
            {
               result[i++] = InternalQName.parse(ValueDataUtil.getString(valueData));
            }
            catch (IllegalNameException e)
            {
               throw new NodeTypeReadException(e.getLocalizedMessage(), e.getCause());
            }
         }
         return result;
      }
      return new InternalQName[0];
   }

   public InternalQName[] readNames(NodeData parentNode, PropertyData propertyData, InternalQName propertyName) throws RepositoryException,
      NodeTypeReadException
   {
      List<ValueData> values = loadPropertyValues(parentNode, propertyData, propertyName);
      if (values != null)
      {
         InternalQName[] result = new InternalQName[values.size()];
         int i = 0;
         for (ValueData valueData : values)
         {
            try
            {
               result[i++] = InternalQName.parse(ValueDataUtil.getString(valueData));
            }
            catch (IllegalNameException e)
            {
               throw new NodeTypeReadException(e.getLocalizedMessage(), e.getCause());
            }
         }
         return result;
      }
      return new InternalQName[0];
   }

   public String readString(NodeData parentNode,PropertyData propertyData, InternalQName propertyName) throws RepositoryException,
      NodeTypeReadException
   {
      List<ValueData> values = loadPropertyValues(parentNode, propertyData, propertyName);
      if (values != null)
      {
         if (values.size() == 1)
         {
            return ValueDataUtil.getString(values.get(0));
         }
      }
      return null;
   }
   
   public String[] readStrings(NodeData parentNode,PropertyData propertyData, InternalQName propertyName) throws RepositoryException,
      NodeTypeReadException
   {
      List<ValueData> values = loadPropertyValues(parentNode, propertyData, propertyName);
      if (values != null)
      {
         if (values.size() > 0)
         {
            String[] result = new String[values.size()];
            int i = 0;
            for (ValueData valueData : values)
            {
               result[i++] = ValueDataUtil.getString(valueData);
            }

            return result;
         }
      }
      return new String[0];
   }


   protected void writeBoolean(PlainChangesLog changesLog, NodeData parentNode, InternalQName propertyName,
      boolean value)
   {
      TransientPropertyData propertyData =
         TransientPropertyData.createPropertyData(parentNode, propertyName, PropertyType.BOOLEAN, false,
            new TransientValueData(value));
      changesLog.add(ItemState.createAddedState(propertyData));
   }

   protected void writeItemDefinition(PlainChangesLog changesLog, NodeData itemDefinition,
      ItemDefinitionData nodeDefinitionData)
   {
      if (nodeDefinitionData.getName() != null)
      { // Mandatory false

         writeName(changesLog, itemDefinition, Constants.JCR_NAME, nodeDefinitionData.getName());
      }
      writeBoolean(changesLog, itemDefinition, Constants.JCR_PROTECTED, nodeDefinitionData.isProtected());

      writeBoolean(changesLog, itemDefinition, Constants.JCR_AUTOCREATED, nodeDefinitionData.isAutoCreated());

      writeBoolean(changesLog, itemDefinition, Constants.JCR_MANDATORY, nodeDefinitionData.isMandatory());

      writeString(changesLog, itemDefinition, Constants.JCR_ONPARENTVERSION,
         OnParentVersionAction.nameFromValue(nodeDefinitionData.getOnParentVersion()));
   }

   protected void writeName(PlainChangesLog changesLog, NodeData parentNode, InternalQName propertyName,
      InternalQName value)
   {
      TransientPropertyData propertyData =
         TransientPropertyData.createPropertyData(parentNode, propertyName, PropertyType.NAME, false,
            new TransientValueData(value));
      changesLog.add(ItemState.createAddedState(propertyData));
   }

   protected void writeNames(PlainChangesLog changesLog, NodeData parentNode, InternalQName propertyName,
      InternalQName[] value)
   {
      List<ValueData> parents = new ArrayList<ValueData>();
      for (InternalQName nt : value)
      {
         parents.add(new TransientValueData(nt));
      }

      TransientPropertyData propertyData =
         TransientPropertyData.createPropertyData(parentNode, propertyName, PropertyType.NAME, true, parents);

      changesLog.add(ItemState.createAddedState(propertyData));
   }

   protected void writeString(PlainChangesLog changesLog, NodeData parentNode, InternalQName propertyName, String value)
   {
      TransientPropertyData propertyData =
         TransientPropertyData.createPropertyData(parentNode, propertyName, PropertyType.STRING, false,
            new TransientValueData(value));
      changesLog.add(ItemState.createAddedState(propertyData));
   }

   protected void writeStrings(PlainChangesLog changesLog, NodeData parentNode, InternalQName propertyName,
      String[] value)
   {
      List<ValueData> valueDatas = new ArrayList<ValueData>();
      for (String vc : value)
      {
         if (vc != null)
         {
            valueDatas.add(new TransientValueData(vc));
         }
      }

      TransientPropertyData propertyData =
         TransientPropertyData.createPropertyData(parentNode, propertyName, PropertyType.STRING, true, valueDatas);

      changesLog.add(ItemState.createAddedState(propertyData));
   }
}
