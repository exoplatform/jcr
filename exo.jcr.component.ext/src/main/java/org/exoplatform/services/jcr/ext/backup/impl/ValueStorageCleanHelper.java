/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
package org.exoplatform.services.jcr.ext.backup.impl;

import org.exoplatform.services.jcr.config.ContainerEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.ValueStorageEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.storage.value.fs.FileValueStorage;

import java.io.File;
import java.io.IOException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 2010
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id: ValueStorageCleanerService.java 3219 2010-09-30 07:40:51Z areshetnyak $
 */
public class ValueStorageCleanHelper
{

   /**
    * Clear workspace value storage.
    * 
    * @param wEntry
    *          workspace configuration
    * @throws RepositoryConfigurationException
    * @throws IOException
    */
   public void removeWorkspaceValueStorage(WorkspaceEntry wEntry) throws RepositoryConfigurationException, IOException
   {
      ContainerEntry containerEntry = wEntry.getContainer();

      if (containerEntry.getValueStorages() != null)
      {
         for (ValueStorageEntry valueStorageEntry : containerEntry.getValueStorages())
         {
            String valueStorageDirName = valueStorageEntry.getParameterValue(FileValueStorage.PATH);
            
            File valueStorageDir = new File(valueStorageDirName);
            if (valueStorageDir.exists())
            {
               removeFolder(valueStorageDir);
            }
         }
      }
   }
   
   /**
    * Remove folder
    */
   private void removeFolder(File dir) throws IOException
   {
      if (dir.isDirectory())
      {  
         for (File subFile : dir.listFiles())
         {
            removeFolder(subFile);
         }
         
         if (!dir.delete())
         {
            throw new IOException("Value storage folder was not deleted : "
               + dir.getCanonicalPath());
         }
      }
      else
      {
         if (!dir.delete())
         {
            throw new IOException("Value storage file was not deleted : " + dir.getCanonicalPath());
         }
      }
   }

}
