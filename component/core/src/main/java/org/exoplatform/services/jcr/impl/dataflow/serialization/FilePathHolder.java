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
package org.exoplatform.services.jcr.impl.dataflow.serialization;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.exoplatform.services.jcr.impl.util.io.FileCleaner;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date: 19.02.2009
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: FilePathHolder.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class FilePathHolder
{

   /**
    * The file cleaner.
    */
   private FileCleaner cleaner;

   /**
    * File path map.
    */
   private ConcurrentHashMap<String, String> filePathMap = new ConcurrentHashMap<String, String>();

   /**
    * Get file path from map.
    * 
    * @param parentPropertyDataId
    *          id
    * @return File path if present and null in other case
    */
   public String getPath(String parentPropertyDataId)
   {
      if (filePathMap.containsKey(parentPropertyDataId))
      {
         return filePathMap.get(parentPropertyDataId);
      }
      else
         return null;
   }

   /**
    * Put file path in map.
    * 
    * @param parentPropertyDataId
    *          id
    * @param filePath
    *          file path
    */
   public void putPath(String parentPropertyDataId, String filePath)
   {
      filePathMap.put(parentPropertyDataId, filePath);
   }

   /**
    * Add all files to file cleaner.
    */
   public void clean()
   {
      List<String> ls = new ArrayList<String>(filePathMap.values());

      for (String fPath : ls)
         cleaner.addFile(new File(fPath));
   }

}
