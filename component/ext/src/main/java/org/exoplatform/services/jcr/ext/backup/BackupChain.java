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
package org.exoplatform.services.jcr.ext.backup;

import java.util.Calendar;
import java.util.List;

/**
 * Created by The eXo Platform SAS.
 *  Author : Alex Reshetnyak alex.reshetnyak@exoplatform.com.ua
 * 09.01.2008
 */
public interface BackupChain
{
   /**
    * The full backup state.
    */
   public static final int FULL_WORKING = 1;

   /**
    * The full + incremental backup state.
    */
   public static final int INCREMENTAL_WORKING = 2;

   /**
    * State of backup FINISHED.
    */
   public static final int FINISHED = 4;

   /**
    * Sate of backup INITIALIZED.
    */
   public static final int INITIALIZED = 0;

   /**
    *  The timeout.
    */
   public long TIMEOUT = 10000;

   /**
    * Getting current backup jobs.
    *
    * @return List
    *           return list of current backup jobs.
    */
   List<BackupJob> getBackupJobs();

   /**
    * Call start backup.
    *
    */
   void startBackup();

   /**
    * Call stop backup.
    *
    */
   void stopBackup();

   /**
    * Getting backup config.
    *
    * @return BackupConfig
    *           return the backup config
    */
   BackupConfig getBackupConfig();

   /**
    * Getting the state of full backup.
    *
    * @return int
    *           return state of full backup
    */
   int getFullBackupState();

   /**
    * Getting state of backup.
    *
    * @return int
    *           return  state of backup
    */
   int getState();

   /**
    * isFinished.
    *
    * @return boolean
    *           return 'true' if backup was finished.
    */
   boolean isFinished();

   /**
    * Getting path to backup log.
    *
    * @return String
    *           return path to backup log
    */
   String getLogFilePath();

   /**
    * Add listener to all existing and will be created in future jobs.
    * 
    * @param listener
    *          BackupJobListener, the backup job listener
    */
   void addListener(BackupJobListener listener);

   /**
    * Remove listener from all existing and don't add it to a created in future jobs.
    * 
    * @param listener
    *          BackupJobListener, the backup job listener
    */
   void removeListener(BackupJobListener listener);

   /**
    * Getting identifier of backup.
    *
    * @return String 
    *           return identifier of backup
    */
   String getBackupId();

   /**
    * Getting started time of backup.
    *
    * @return Calendar
    *           return started time of backup
    */
   Calendar getStartedTime();

}
