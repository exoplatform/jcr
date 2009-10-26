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
package org.exoplatform.services.jcr.impl.util;

import org.exoplatform.services.jcr.dataflow.DataManager;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.ValueData;

import java.util.HashMap;
import java.util.List;

import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

/**
 * Created by The eXo Platform SAS 15.05.2006
 * 
 * PropertyData bulk reader.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: PropertyDataReader.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class PropertyDataReader extends ItemDataReader
{

   private HashMap<InternalQName, PropertyInfo> propeties = new HashMap<InternalQName, PropertyInfo>();

   private class PropertyInfo
   {
      private InternalQName propertyName = null;

      private boolean multiValued = false;

      private List<ValueData> mValueData = null;

      private List<Value> mValue = null;

      private ValueData valueData = null;

      private int type = PropertyType.STRING;

      PropertyInfo(InternalQName propertyName, int type)
      {
         this.propertyName = propertyName;
         this.type = type;
      }

      public InternalQName getPropertyName()
      {
         return propertyName;
      }

      public boolean isMultiValued()
      {
         return multiValued;
      }

      public void setMultiValued(boolean multiValued)
      {
         this.multiValued = multiValued;
      }

      public List<ValueData> getValues() throws ValueFormatException, PathNotFoundException
      {
         if (multiValued)
         {
            if (mValueData != null)
            {
               return mValueData;
            }
         }
         else if (valueData != null)
         {
            throw new ValueFormatException("Property " + parent.getQPath().getAsString() + propertyName.getAsString()
               + " is multi-valued");
         }
         throw new PathNotFoundException("Property " + parent.getQPath().getAsString() + propertyName.getAsString()
            + " not found (multi-valued)");
      }

      public void setValueDatas(List<ValueData> mValue)
      {
         this.mValueData = mValue;
         this.multiValued = true;
      }

      public ValueData getValueData() throws ValueFormatException, PathNotFoundException
      {
         if (!multiValued)
         {
            if (valueData != null)
            {
               return valueData;
            }
         }
         else if (mValueData != null)
         {
            throw new ValueFormatException("Property " + parent.getQPath().getAsString() + propertyName.getAsString()
               + " is single-valued");
         }
         throw new PathNotFoundException("Property " + parent.getQPath().getAsString() + propertyName.getAsString()
            + " not found (single-valued)");
      }

      public void setValueData(ValueData value)
      {
         this.valueData = value;
         this.multiValued = false;
      }

      public int getType()
      {
         return type;
      }
   }

   public PropertyDataReader(NodeData parent, DataManager dataManager)
   {
      super(parent, dataManager);
   }

   public PropertyDataReader forProperty(InternalQName name, int type)
   {
      propeties.put(name, new PropertyInfo(name, type));
      return this;
   }

   public List<ValueData> getPropertyValues(InternalQName name) throws ValueFormatException, PathNotFoundException
   {
      return propeties.get(name).getValues();
   }

   public ValueData getPropertyValue(InternalQName name) throws ValueFormatException, PathNotFoundException
   {
      return propeties.get(name).getValueData();
   }

   public void read() throws RepositoryException
   {
      List<PropertyData> ndProps = dataManager.getChildPropertiesData(parent);
      for (PropertyData prop : ndProps)
      {
         PropertyInfo propInfo = propeties.get(prop.getQPath().getName());
         if (propInfo != null)
         {
            List<ValueData> valueDataList = prop.getValues();
            if (prop.isMultiValued())
            {
               propInfo.setValueDatas(valueDataList);
            }
            else
            {
               if (valueDataList.size() > 0)
                  propInfo.setValueData(valueDataList.get(0));
            }
         }
      }
   }
}
