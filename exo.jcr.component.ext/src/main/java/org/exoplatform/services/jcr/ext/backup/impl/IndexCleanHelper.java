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

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.services.jcr.config.QueryHandlerParams;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.core.query.SystemSearchManager;

import java.io.File;
import java.io.IOException;

/**
 * IndexCleanerService deliver tools for clean index data of workspace or repository.
 * 
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 2010
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id: IndexCleanerService.java 3210 2010-09-28 12:01:50Z areshetnyak $
 */
public class IndexCleanHelper
{
   /**
    * Remove all file of workspace index. 
    * 
    * @param wsConfig - workspace configuration.
    * @param isSystem - 'true' to clean system workspace. 
    * @throws RepositoryConfigurationException - exception on parsing workspace configuration
    * @throws IOException - exception on remove index folder
    */
   public void removeWorkspaceIndex(WorkspaceEntry wsConfig, boolean isSystem) throws RepositoryConfigurationException, IOException
   {
      String indexDirName = wsConfig.getQueryHandler().getParameterValue(QueryHandlerParams.PARAM_INDEX_DIR);
      
      File indexDir = new File(indexDirName);
      if (PrivilegedFileHelper.exists(indexDir))
      {
         removeFolder(indexDir);
      }
      
      if (isSystem)
      {
         File systemIndexDir = new File(indexDirName + "_" + SystemSearchManager.INDEX_DIR_SUFFIX);
         if (PrivilegedFileHelper.exists(systemIndexDir))
         {
            removeFolder(systemIndexDir);
         }
      }
   }
   
   /**
    * Remove folder
    */
   private void removeFolder(File dir) throws IOException 
   {
      if (PrivilegedFileHelper.isDirectory(dir))
      {  
         for (File subFile : PrivilegedFileHelper.listFiles(dir))
         {
            removeFolder(subFile);
         }
         
         if (!PrivilegedFileHelper.delete(dir))
         {
            throw new IOException("Index folder was not deleted : " + PrivilegedFileHelper.getCanonicalPath(dir));
         }
      }
      else
      {
         if (!PrivilegedFileHelper.delete(dir))
         {
            throw new IOException("Index file was not deleted : " + PrivilegedFileHelper.getCanonicalPath(dir));
         }
      }
   }
}
