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

package org.exoplatform.services.jcr.impl.storage.value.fs;

import org.exoplatform.services.jcr.impl.storage.value.ValueDataResourceHolder;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

/**
 * Created by The eXo Platform SAS
 * 
 * @author Gennady Azarenkov
 * @version $Id: SimpleFileIOChannel.java 34801 2009-07-31 15:44:50Z dkatayev $
 */

public class SimpleFileIOChannel extends FileIOChannel
{

   protected class PropertyIDFilter implements FileFilter
   {

      private String propertyId;

      public PropertyIDFilter(String propertyId)
      {
         this.propertyId = propertyId;
      }

      public boolean accept(File file)
      {
         return file.getName().startsWith(propertyId);
      }
   }

   public SimpleFileIOChannel(File rootDir, FileCleaner cleaner, String storageId, ValueDataResourceHolder resources)
   {
      super(rootDir, cleaner, storageId, resources);
   }

   @Override
   protected String makeFilePath(String propertyId, int orderNumber)
   {
      return propertyId + orderNumber;
   }

   @Override
   protected File getFile(String propertyId, int orderNumber) throws IOException
   {
      return new File(rootDir, makeFilePath(propertyId, orderNumber));
   }

   @Override
   protected File[] getFiles(String propertyId) throws IOException
   {
      return rootDir.listFiles(new PropertyIDFilter(propertyId));
   }
}
