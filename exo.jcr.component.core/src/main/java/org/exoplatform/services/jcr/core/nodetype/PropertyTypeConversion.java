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

package org.exoplatform.services.jcr.core.nodetype;

import org.exoplatform.services.jcr.core.ExtendedPropertyType;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.PropertyType;

/**
 * Serialization/Deserialization for PropertyType beans. For JiBX binding process only.
 * 
 * @author <a href="mailto:peterit@rambler.ru">Petro Nedonosko</a>
 */
public class PropertyTypeConversion
{

   private static final Log LOG = ExoLogger
      .getLogger("org.exoplatform.services.jcr.core.nodetype.PropertyTypeConversion");

   public static String serializeType(int propertyType)
   {

      String r = PropertyType.TYPENAME_UNDEFINED;
      try
      {
         r = ExtendedPropertyType.nameFromValue(propertyType);
      }
      catch (IllegalArgumentException e)
      {
         // r = PropertyType.TYPENAME_UNDEFINED;
         if (LOG.isTraceEnabled())
         {
            LOG.trace("An exception occurred: " + e.getMessage());
         }
      }
      catch (Exception e)
      {
         // r = PropertyType.TYPENAME_UNDEFINED;
         if (LOG.isTraceEnabled())
         {
            LOG.trace("An exception occurred: " + e.getMessage());
         }
      }
      return r;
   }

   public static int deserializeType(String propertyTypeString)
   {

      int r = PropertyType.UNDEFINED;
      try
      {
         r = ExtendedPropertyType.valueFromName(propertyTypeString);
      }
      catch (IllegalArgumentException e)
      {
         // r = PropertyType.TYPENAME_UNDEFINED;
         if (LOG.isTraceEnabled())
         {
            LOG.trace("An exception occurred: " + e.getMessage());
         }
      }
      catch (Exception e)
      {
         // r = PropertyType.TYPENAME_UNDEFINED;
         if (LOG.isTraceEnabled())
         {
            LOG.trace("An exception occurred: " + e.getMessage());
         }
      }
      return r;
   }

}
