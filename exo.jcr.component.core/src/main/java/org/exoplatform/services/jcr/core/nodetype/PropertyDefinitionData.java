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

import org.exoplatform.services.jcr.datamodel.InternalQName;

import java.util.Arrays;

/**
 * Created by The eXo Platform SAS. <br/>Date: 25.11.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter
 *         Nedonosko</a>
 * @version $Id: PropertyDefinitionData.java 23992 2008-11-27 16:58:34Z
 *          pnedonosko $
 */
public class PropertyDefinitionData extends ItemDefinitionData
{

   protected final int requiredType;

   protected final String[] valueConstraints;

   protected final String[] defaultValues;

   protected final boolean multiple;

   public PropertyDefinitionData(InternalQName name, InternalQName declaringNodeType, boolean autoCreated,
      boolean mandatory, int onParentVersion, boolean protectedItem, int requiredType, String[] valueConstraints,
      String[] defaultValues, boolean multiple)
   {
      super(name, declaringNodeType, autoCreated, mandatory, onParentVersion, protectedItem);
      this.requiredType = requiredType;
      this.valueConstraints = valueConstraints;
      this.defaultValues = defaultValues;
      this.multiple = multiple;
   }

   public int getRequiredType()
   {
      return requiredType;
   }

   public String[] getValueConstraints()
   {
      return valueConstraints;
   }

   public String[] getDefaultValues()
   {
      return defaultValues;
   }

   public boolean isMultiple()
   {
      return multiple;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
         return true;
      if ((obj == null) || (obj.getClass() != this.getClass()))
         return false;
      PropertyDefinitionData test = (PropertyDefinitionData)obj;
      return requiredType == test.requiredType && multiple == test.multiple && super.equals(test)
         && Arrays.equals(this.valueConstraints, test.valueConstraints)
         && Arrays.equals(this.defaultValues, test.defaultValues);
   }
}
