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
package org.exoplatform.services.jcr.core.nodetype;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

/**
 * Created by The eXo Platform SAS<br>
 *
 * The PropertyDefinitionValue interface extends ItemDefinitionValue with the addition of writing methods,
 * enabling the characteristics of a child property definition to be set,
 * after that the PropertyDefinitionValue is added to a NodeTypeValue.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: PropertyDefinitionValue.java 11907 2008-03-13 15:36:21Z ksm $
 * @LevelAPI Unsupported
 */

public final class PropertyDefinitionValue extends ItemDefinitionValue
{
   private int requiredType;

   private List<String> valueConstraints;

   private List<String> defaultValueStrings;

   private boolean multiple;

   public PropertyDefinitionValue()
   {
   }

   /**
    * @param autoCreate
    * @param mandatory
    * @param name
    * @param onVersion
    * @param readOnly
    * @param defaultValueStrings
    * @param multiple
    * @param requiredType
    * @param valueConstraints
    */
   public PropertyDefinitionValue(String name, boolean autoCreate, boolean mandatory, int onVersion, boolean readOnly,
      List<String> defaultValueStrings, boolean multiple, int requiredType, List<String> valueConstraints)
   {
      super(name, autoCreate, mandatory, onVersion, readOnly);
      this.defaultValueStrings = defaultValueStrings;
      this.multiple = multiple;
      this.requiredType = requiredType;
      this.valueConstraints = valueConstraints;
   }

   public PropertyDefinitionValue(PropertyDefinition propertyDefinition) throws RepositoryException
   {
      super(propertyDefinition);
      this.defaultValueStrings = convert(propertyDefinition.getDefaultValues());
      this.multiple = propertyDefinition.isMultiple();
      this.requiredType = propertyDefinition.getRequiredType();
      this.valueConstraints =
         propertyDefinition.getValueConstraints() != null ? Arrays.asList(propertyDefinition.getValueConstraints())
            : new ArrayList<String>();;
   }

   /**
    * @return Returns the defaultValues.
    */
   public List<String> getDefaultValueStrings()
   {
      return defaultValueStrings;
   }

   /**
    * @param defaultValues The defaultValues to set.
    */
   public void setDefaultValueStrings(List<String> defaultValues)
   {
      this.defaultValueStrings = defaultValues;
   }

   /**
    * @return Returns the multiple.
    */
   public boolean isMultiple()
   {
      return multiple;
   }

   /**
    * @param multiple The multiple to set.
    */
   public void setMultiple(boolean multiple)
   {
      this.multiple = multiple;
   }

   /**
    * @return Returns the requiredType.
    */
   public int getRequiredType()
   {
      return requiredType;
   }

   /**
    * @param requiredType The requiredType to set.
    */
   public void setRequiredType(int requiredType)
   {
      this.requiredType = requiredType;
   }

   /**
    * @return Returns the valueConstraints.
    */
   public List<String> getValueConstraints()
   {
      return valueConstraints;
   }

   /**
    * @param valueConstraints The valueConstraints to set.
    */
   public void setValueConstraints(List<String> valueConstraints)
   {
      this.valueConstraints = valueConstraints;
   }

   private List<String> convert(Value[] values) throws RepositoryException
   {
      List<String> result = new ArrayList<String>(values.length);
      for (int i = 0; i < values.length; i++)
      {
         result.add(values[i].getString());
      }
      return result;
   }
}
