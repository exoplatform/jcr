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
package org.exoplatform.services.jcr.ext.backup.server.bean.response;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>
 * Date: 27.03.2009
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: BackupServiceInfoBeen.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class BackupServiceInfoBean
{

   /**
    * The type of full backup.
    */
   private String fullBackupType;

   /**
    * The type of incremental backup.
    */
   private String incrementalBackupType;

   /**
    * The path to backup log folder.
    */
   private String backupLogDir;

   /**
    * The default incremental job period.
    */
   private Long defaultIncrementalJobPeriod;

   /**
    * BackupServiceInfoBeen constructor.
    * 
    */
   public BackupServiceInfoBean()
   {
   }

   /**
    * BackupServiceInfoBean constructor.
    * 
    * @param fullBackupType
    *          String, the type of full backup
    * @param incrementalBackupType
    *          String, the type of incremental backup
    * @param backupLogDir
    *          String, the backup folder
    * @param defaultIncrementalJobPeriod
    *          Long, the default incremental job period
    */
   public BackupServiceInfoBean(String fullBackupType, String incrementalBackupType, String backupLogDir,
      Long defaultIncrementalJobPeriod)
   {

      this.fullBackupType = fullBackupType;
      this.incrementalBackupType = incrementalBackupType;
      this.backupLogDir = backupLogDir;
      this.defaultIncrementalJobPeriod = defaultIncrementalJobPeriod;
   }

   /**
    * getFullBackupType.
    * 
    * @return String return the type of full backup
    */
   public String getFullBackupType()
   {
      return fullBackupType;
   }

   /**
    * setFullBackupType.
    * 
    * @param fullBackupType
    *          String, the type of full backup
    */
   public void setFullBackupType(String fullBackupType)
   {
      this.fullBackupType = fullBackupType;
   }

   /**
    * getIncrementalBackupType.
    * 
    * @return String return the type of incremental backup
    */
   public String getIncrementalBackupType()
   {
      return incrementalBackupType;
   }

   /**
    * setIncrementalBackupType.
    * 
    * @param incrementalBackupType
    *          String, the type of incremental backup
    */
   public void setIncrementalBackupType(String incrementalBackupType)
   {
      this.incrementalBackupType = incrementalBackupType;
   }

   /**
    * getBackupLogDir.
    * 
    * @return String return the path to backup log folder
    */
   public String getBackupLogDir()
   {
      return backupLogDir;
   }

   /**
    * setBackupLogDir.
    * 
    * @param backupLogDir
    *          String, the path to backup log folder
    */
   public void setBackupLogDir(String backupLogDir)
   {
      this.backupLogDir = backupLogDir;
   }

   /**
    * getDefaultIncrementalJobPeriod.
    * 
    * @return Long return the default incremental job period
    */
   public Long getDefaultIncrementalJobPeriod()
   {
      return defaultIncrementalJobPeriod;
   }

   /**
    * setDefaultIncrementalJobPeriod.
    * 
    * @param defaultIncrementalJobPeriod
    *          Long, the default incremental job period
    */
   public void setDefaultIncrementalJobPeriod(Long defaultIncrementalJobPeriod)
   {
      this.defaultIncrementalJobPeriod = defaultIncrementalJobPeriod;
   }

}
