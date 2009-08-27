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

import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: PropertyDefinitionImpl.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class PropertyDefinitionImpl
   extends ItemDefinitionImpl
   implements PropertyDefinition
{

   private final int requiredType;

   private String[] valueConstraints;

   private Value[] defaultValues;

   private final boolean multiple;

   public PropertyDefinitionImpl(String name, NodeType declaringNodeType, int requiredType, String[] valueConstraints,
            Value[] defaultValues, boolean autoCreate, boolean mandatory, int onVersion, boolean readOnly,
            boolean multiple)
   {

      super(name, declaringNodeType, autoCreate, onVersion, readOnly, mandatory);

      this.requiredType = requiredType;
      this.valueConstraints = valueConstraints;
      this.defaultValues = defaultValues;
      this.multiple = multiple;

      int hk = 31 * this.hashCode + requiredType;
      hk = 31 * hk + valueConstraints.hashCode();
      hk = 31 * hk + defaultValues.hashCode();
      this.hashCode = 31 * hk + (multiple ? 0 : 1);
   }

   /**
    * {@inheritDoc}
    */
   public int getRequiredType()
   {
      return requiredType;
   }

   /**
    * {@inheritDoc}
    */
   public String[] getValueConstraints()
   {
      return valueConstraints;
   }

   /**
    * {@inheritDoc}
    */
   public Value[] getDefaultValues()
   {
      if (defaultValues != null && defaultValues.length > 0)
         return defaultValues;
      else
         return null;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isMultiple()
   {
      return multiple;
   }

   /**
    * @param defaultValues
    *          The defaultValues to set.
    */
   public void setDefaultValues(Value[] defaultValues)
   {
      this.defaultValues = defaultValues;
   }

   /**
    * @param valueConstraints
    *          The valueConstraints to set.
    */
   public void setValueConstraints(String[] valueConstraints)
   {
      this.valueConstraints = valueConstraints;
   }

   /**
    * Compare property definitions for equality by name, required type and miltiplicity flag. NOTE:
    * UNDEFINED is equals to UNDEFINED only. NOTE: PD without name is equals to PD without name
    */
   public boolean equals(Object obj)
   {
      if (obj == null)
         return false;
      if (super.equals(obj))
         return true;
      if (obj instanceof PropertyDefinitionImpl)
      {
         return obj.hashCode() == hashCode;
      }
      return false;
   }
}
