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

import java.io.File;
import java.io.IOError;
import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.naming.NamingException;

import org.exoplatform.services.jcr.config.QueryHandlerParams;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.WorkspaceEntry;

/**
 * IndexCleanerService deliver tools for clean index data of workspace or repository.
 * 
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 2010
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id$
 */
public class IndexCleanerService
{
   /**
    * Remove all file of workspace index. 
    * 
    * @param wsConfig - workspace configuration.
    * @param isSystem - 'true' to clean system workspace. 
    * @throws RepositoryConfigurationException - exception on parsing workspace configuration
    * @throws IOException - exception on remove index folder
    */
   public static void removeWorkspaceIndex(WorkspaceEntry wsConfig, boolean isSystem) throws RepositoryConfigurationException, IOException
   {
      String indexDir = wsConfig.getQueryHandler().getParameterValue(QueryHandlerParams.PARAM_INDEX_DIR);
      
      removeFolder(new File(indexDir));
      
      if (isSystem)
      {
         removeFolder(new File(indexDir + "_system"));
      }
   }
   
   /**
    * Remove folder
    */
   private static void removeFolder(File dir) throws IOException 
   {
      if (dir.isDirectory())
      {  
         for (File subFile : dir.listFiles())
         {
            removeFolder(subFile);
         }
         
         if (!dir.delete())
         {
            throw new IOException("Index folder was not deleted : " + dir.getCanonicalPath());
         }
      }
      else
      {
         if (!dir.delete())
         {
            throw new IOException("Index file was not deleted : " + dir.getCanonicalPath());
         }
      }
   }
}
