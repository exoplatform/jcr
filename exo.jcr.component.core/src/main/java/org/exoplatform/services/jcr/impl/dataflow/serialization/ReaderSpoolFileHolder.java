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

package org.exoplatform.services.jcr.impl.dataflow.serialization;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>
 * Date:
 * 
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a>
 * @version $Id: ReaderSpoolFileHolder.java 111 2008-11-11 11:11:11Z serg $
 */
public class ReaderSpoolFileHolder
{

   /**
    * SpoolFile map.
    */
   private final ConcurrentHashMap<String, SerializationSpoolFile> map;

   /**
    * ReaderSpoolFileHolder constructor.
    */
   public ReaderSpoolFileHolder()
   {
      map = new ConcurrentHashMap<String, SerializationSpoolFile>();
   }

   /**
    * Get SpoolFile from holder.
    * 
    * @param key
    *          key
    * @return SpoolFile
    */
   public SerializationSpoolFile get(String key)
   {
      return map.get(key);
   }

   /**
    * Put file into holder.
    * 
    * @param key
    *          key
    * @param file
    *          SpoolFile
    */
   public void put(String key, SerializationSpoolFile file)
   {
      map.put(key, file);
   }

   /**
    * Remove file form holder.
    * 
    * @param key
    *          key
    */
   public void remove(String key)
   {
      map.remove(key);
   }

}
