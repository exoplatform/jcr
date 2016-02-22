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
package org.exoplatform.services.jcr.ext.backup;

import java.util.Calendar;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>Date: 2010
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id$
 */
public interface RepositoryBackupChain
{
   
   /**
    * State of backup : full backup was finished and incremental backup is working.
    */
   public static final int FULL_BACKUP_FINISHED_INCREMENTAL_BACKUP_WORKING = 16;
   
   /**
    * State of backup WORKING.
    */
   public static final int WORKING = 8;
   
   /**
    * State of backup FINISHED.
    */
   public static final int FINISHED = 4;
   
   /**
    * Sate of backup INITIALIZED.
    */
   public static final int INITIALIZED = 0;
   
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
   
   /**
    * Getting repository backup config.
    *
    * @return RepositoryBackupConfig
    *           return the repository backup config
    */
   RepositoryBackupConfig getBackupConfig();
   
}
