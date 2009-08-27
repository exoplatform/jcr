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

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: PropertyDefinitions.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class PropertyDefinitionDatas
{

   private PropertyDefinitionData multiDef = null;

   private PropertyDefinitionData singleDef = null;

   public void setDefinition(final PropertyDefinitionData def)
   {
      final boolean residual = def.isResidualSet();
      if (def.isMultiple())
      {
         if ((residual && multiDef == null) || !residual)
            multiDef = def;
      }
      else
      {
         if ((residual && singleDef == null) || !residual)
            singleDef = def;
      }
   }

   public PropertyDefinitionData getDefinition(final boolean multiple)
   {

      refresh();

      if (multiple && multiDef != null)
         return multiDef;
      if (!multiple && singleDef != null)
         return singleDef;

      return null;
   }

   public PropertyDefinitionData getAnyDefinition()
   {

      refresh();

      if (multiDef != null)
         return multiDef;
      if (singleDef != null)
         return singleDef;

      return null;
   }

   private void refresh()
   {
      // if both defined should be both either residual or not
      if (multiDef != null && singleDef != null)
      {
         if (multiDef.isResidualSet() && !singleDef.isResidualSet())
            multiDef = null;
         if (singleDef.isResidualSet() && !multiDef.isResidualSet())
            singleDef = null;
      }
   }

   public String dump()
   {
      return "Definitions single: " + ((singleDef == null) ? "N/D" : singleDef.getName()) + ", multiple: "
               + ((multiDef == null) ? "N/D" : multiDef.getName());
   }

}
