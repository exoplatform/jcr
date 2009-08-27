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
package org.exoplatform.services.jcr.ext.backup.server.bean;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date: 26.03.2009
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: RestoreBeen.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class RestoreBean
{

   /**
    * The backup identifier.
    */
   String backupId;

   /**
    * The workspace configuration.
    */
   String workspaceConfig;

   /**
    * RestoreBeen constructor.
    * 
    */
   public RestoreBean()
   {
   }

   /**
    * RestoreBeen constructor.
    * 
    * @param backupId
    *          String, the backup identifier
    * @param workspaceConfig
    *          the workspace configuration
    */
   public RestoreBean(String backupId, String workspaceConfig)
   {
      this.backupId = backupId;
      this.workspaceConfig = workspaceConfig;
   }

   /**
    * getBackupId.
    * 
    * @return String return the backup identifier
    */
   public String getBackupId()
   {
      return backupId;
   }

   /**
    * setBackupId.
    * 
    * @param backupId
    *          String, the backup identifier
    */
   public void setBackupId(String backupId)
   {
      this.backupId = backupId;
   }

   /**
    * getWorkspaceConfig.
    * 
    * @return String return the workspace configuration
    */
   public String getWorkspaceConfig()
   {
      return workspaceConfig;
   }

   /**
    * setWorkspaceConfig.
    * 
    * @param workspaceConfig
    *          String, the workspace configuration
    */
   public void setWorkspaceConfig(String workspaceConfig)
   {
      this.workspaceConfig = workspaceConfig;
   }
}
