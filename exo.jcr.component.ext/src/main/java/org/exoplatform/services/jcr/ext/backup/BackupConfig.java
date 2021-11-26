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

package org.exoplatform.services.jcr.ext.backup;

/**
 * Created by The eXo Platform SARL .<br>
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */

public class BackupConfig extends RepositoryBackupConfig
{
   /**
    * The workspace name. 
    */
   private String workspace;

   /**
    * Getting the workspace name.
    *
    * @return String
    *           return the workspace name
    */
   public String getWorkspace()
   {
      return workspace;
   }

   /**
    * Setting the workspace name.
    *
    * @param workspace
    *          String, the workspace name
    */
   public void setWorkspace(String workspace)
   {
      this.workspace = workspace;
   }
}
