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

import org.exoplatform.services.jcr.ext.backup.BackupConfig;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupConfig;
import org.exoplatform.services.jcr.ext.backup.server.bean.response.BackupJobConfig;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date: 26.03.2009
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: BackupConfigBeen.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class BackupConfigBean
{

   /**
    * The backup type (full or full+incremental).
    */
   private Integer backupType;

   /**
    * The incremental job period.
    */
   private Long incrementalJobPeriod;

   /**
    * The incremental repetition number.
    */
   private Integer incrementalRepetitionNumber = 0;

   /**
    * The BackupJobConfig to full backup.
    */
   private BackupJobConfig fullBackupJobConfig = new BackupJobConfig();

   /**
    * The BackupJobConfig to incremental backup.
    */
   private BackupJobConfig incrementalBackupJobConfig = new BackupJobConfig();

   /**
    * The folder for backup data.
    */
   private String backupDir;

   /**
    * BackupConfigBeen constructor. Empty constructor for JSON.
    * 
    */
   public BackupConfigBean()
   {
   }

   /**
    * BackupConfigBeen constructor. Constructor for full backup.
    * 
    * @param backupType
    *          Integer, backup tyeps
    * @param backupDir
    *          String, path to backup folder
    */
   public BackupConfigBean(Integer backupType, String backupDir)
   {
      this(backupType, backupDir, (long)0);
   }

   /**
    * BackupConfigBeen constructor. Constructor for full + incremental backup.
    * 
    * @param backupType
    *          Integer, backup tyeps
    * @param backupDir
    *          String, path to backup folder
    * @param incrementalJobPeriod
    *          Long, incremental job period
    */
   public BackupConfigBean(Integer backupType, String backupDir, Long incrementalJobPeriod)
   {
      this.backupType = backupType;
      this.backupDir = backupDir;
      this.incrementalJobPeriod = incrementalJobPeriod;
   }

   /**
    * BackupConfigBeen constructor. Constructor for full + incremental backup.
    * 
    * @param backupType
    *          Integer, backup tyeps
    * @param backupDir
    *          String, path to backup folder
    * @param incrementalJobPeriod
    *          Long, incremental job period
    * @param incrementalRepetitionNumber
    *          Integer, incremental repetition number
    */
   public BackupConfigBean(Integer backupType, String backupDir, Long incrementalJobPeriod,
      Integer incrementalRepetitionNumber)
   {
      this.backupType = backupType;
      this.backupDir = backupDir;
      this.incrementalJobPeriod = incrementalJobPeriod;
      this.incrementalRepetitionNumber = incrementalRepetitionNumber;
   }

   /**
    * BackupConfigBeen constructor.
    * 
    * @param config
    *          the backup config
    */
   public BackupConfigBean(BackupConfig config)
   {
      this(config.getBackupType(), config.getBackupDir().getAbsolutePath(), config
         .getIncrementalJobPeriod());
      this.incrementalRepetitionNumber = config.getIncrementalJobNumber();
   }

   /**
    * BackupConfigBeen constructor.
    * 
    * @param config
    *          the backup config
    */
   public BackupConfigBean(RepositoryBackupConfig config)
   {
      this(config.getBackupType(), config.getBackupDir().getAbsolutePath(), config
         .getIncrementalJobPeriod());
      this.incrementalRepetitionNumber = config.getIncrementalJobNumber();
   }

   /**
    * getIncrementalJobPeriod.
    * 
    * @return Long return the incremental job period
    */
   public Long getIncrementalJobPeriod()
   {
      return incrementalJobPeriod;
   }

   /**
    * setIncrementalJobPeriod.
    * 
    * @param incrementalJobPeriod
    *          Long, the incremental job period
    */
   public void setIncrementalJobPeriod(Long incrementalJobPeriod)
   {
      this.incrementalJobPeriod = incrementalJobPeriod;
   }

   /**
    * getBackupDir.
    * 
    * @return String return path to backup folder
    */
   public String getBackupDir()
   {
      return backupDir;
   }

   /**
    * setBackupDir.
    * 
    * @param backupDir
    *          String, path to backup folder
    */
   public void setBackupDir(String backupDir)
   {
      this.backupDir = backupDir;
   }

   /**
    * getBackupType.
    * 
    * @return Integer return the backup type
    */
   public Integer getBackupType()
   {
      return backupType;
   }

   /**
    * setBackupType.
    * 
    * @param backupType
    *          Integer, the backup type
    */
   public void setBackupType(Integer backupType)
   {
      this.backupType = backupType;
   }

   /**
    * getIncrementalRepetitionNumber.
    * 
    * @return Integer return the incremental repetition number
    */
   public Integer getIncrementalRepetitionNumber()
   {
      return incrementalRepetitionNumber;
   }

   /**
    * setIncrementalRepetitionNumber.
    * 
    * @param incrementalRepetitionNumber
    *          Integer, incremental repetition number
    */
   public void setIncrementalRepetitionNumber(Integer incrementalRepetitionNumber)
   {
      this.incrementalRepetitionNumber = incrementalRepetitionNumber;
   }

   /**
    * getFullBackupJobConfig.
    * 
    * @return BackupJobConfig return the backup job configuration to full backup
    */
   public BackupJobConfig getFullBackupJobConfig()
   {
      return fullBackupJobConfig;
   }

   /**
    * setFullBackupJobConfig.
    * 
    * @param fullBackupJobConfig
    *          BackupJobConfig the backup job configuration to full backup
    */
   public void setFullBackupJobConfig(BackupJobConfig fullBackupJobConfig)
   {
      this.fullBackupJobConfig = fullBackupJobConfig;
   }

   /**
    * getIncrementalBackupJobConfig.
    * 
    * @return BackupJobConfig return the backup job configuration to incremental backup
    */
   public BackupJobConfig getIncrementalBackupJobConfig()
   {
      return incrementalBackupJobConfig;
   }

   /**
    * setIncrementalBackupJobConfig.
    * 
    * @param incrementalBackupJobConfig
    *          BackupJobConfig the backup job configuration to incremental backup
    */
   public void setIncrementalBackupJobConfig(BackupJobConfig incrementalBackupJobConfig)
   {
      this.incrementalBackupJobConfig = incrementalBackupJobConfig;
   }

}
