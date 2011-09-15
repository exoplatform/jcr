/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.backup.rdbms;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 2011
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id: DBRestoreContext.java 111 2011-11-11 11:11:11Z rainf0x $
 */
public class DataRestoreContext
{

   public static final String STORAGE_DIR = "storage-dir";

   public static final String DB_CONNECTION = "db-connection";

   public static final String DB_CLEANER = "db-cleaner";

   /**
    * Context objects.
    */
   private Map<String, Object> objects = new HashMap<String, Object>();

   /**
    * Constructor.
    *
    * @param names
    *          the array with names
    * @param objects
    *          the array with objects
    */
   public DataRestoreContext(String[] names, Object[] objects)
   {
      for (int i = 0; i < names.length; i++)
      {
         this.objects.put(names[i], objects[i]);
      }
   }

   /**
    * Getting object from context.
    * 
    * @param objectName
    *          String, name of object.
    * @return Object, if object is not contains in context will be throws RuntimeException.
    *     
    */
   public Object getObject(String objectName)
   {
      return objects.get(objectName);
   }
}
