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

import java.util.List;

/**
 * Created by The eXo Platform SAS.
 *  Author : Alex Reshetnyak alex.reshetnyak@exoplatform.com.ua
 * 09.01.2008
 */
public interface BackupChain extends RepositoryBackupChain 
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
    * Getting the state of incremental backup.
    *
    * @return int
    *           return state of full backup
    */
   int getIncrementalBackupState();

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
}
