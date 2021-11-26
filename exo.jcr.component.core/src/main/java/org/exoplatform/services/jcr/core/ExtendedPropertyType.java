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

package org.exoplatform.services.jcr.core;

import javax.jcr.PropertyType;

/**
 * Created by The eXo Platform SAS.<br> Extension for JSR-170 Property Types
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: ExtendedPropertyType.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class ExtendedPropertyType
{

   /**
    * Additional property type for exo permission properties.
    */
   public static final int PERMISSION = 100;

   public static String nameFromValue(int type)
   {
      if (type == PERMISSION)
         return "Permission";
      return PropertyType.nameFromValue(type);
   }

   public static int valueFromName(String name)
   {
      if (name.equals("Permission"))
         return PERMISSION;
      return PropertyType.valueFromName(name);
   }
}
