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

import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.ext.backup.impl.BackupMessage;
import org.exoplatform.services.jcr.ext.backup.impl.BackupScheduler;
import org.exoplatform.services.jcr.ext.backup.impl.JobRepositoryRestore;
import org.exoplatform.services.jcr.ext.backup.impl.JobWorkspaceRestore;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SARL .<br/>
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */

public interface BackupManager
{

   /**
    * The full backup only the type of backup.
    */
   static final int FULL_BACKUP_ONLY = 0;

   /**
    * The full and incremental backup the type of backup.
    */
   static final int FULL_AND_INCREMENTAL = 1;

   /**
    * Getting current backups.
    *
    * @return Set
    *           return the set of current backups 
    */
   Set<BackupChain> getCurrentBackups();

   /**
    * Getting current repository backups.
    *
    * @return Set
    *           return the set of current backups 
    */
   Set<RepositoryBackupChain> getCurrentRepositoryBackups();

   /**
    * Getting list of restores.
    *
    * @return List
    *           return the list of backups
    */
   List<JobWorkspaceRestore> getRestores();

   /**
    * Getting last restore by repository and workspace name.
    *
    * @param repositoryName
    *          String,  the repository name
    * @param workspaceName
    *          String, the workspace name
    * @return JobWorkspaceRestore
    *           return the job to restore
    */
   JobWorkspaceRestore getLastRestore(String repositoryName, String workspaceName);

   /**
    * Getting list of repository restores.
    *
    * @return List
    *           return the list of backups
    */
   List<JobRepositoryRestore> getRepositoryRestores();

   /**
    * Getting last repository restore by repository name.
    *
    * @param repositoryName
    *          String,  the repository name
    * @return JobWorkspaceRestore
    *           return the job to restore
    */
   JobRepositoryRestore getLastRepositoryRestore(String repositoryName);

   /**
    * Getting all backup logs .
    *
    * @return BackupChainLog[]
    *           return the all backup logs
    */
   BackupChainLog[] getBackupsLogs();

   /**
    * Getting all repository backup logs.
    *
    * @return RepositoryBackupChainLog[]
    *           return the all repository backup logs
    */
   RepositoryBackupChainLog[] getRepositoryBackupsLogs();

   /**
    * Starting backup.
    *
    * @param config
    *          BackupConfig, the backup configuration
    * @return BackupChain
    *           return the backup chain
    * @throws BackupOperationException BackupOperationException
    *           will be generate the exception BackupOperationException
    * @throws BackupConfigurationException
    *           will be generate the exception BackupConfigurationException
    * @throws RepositoryException
    *           will be generate the exception RepositoryException
    * @throws RepositoryConfigurationException
    *           will be generate the exception RepositoryConfigurationException
    */
   BackupChain startBackup(BackupConfig config) throws BackupOperationException, BackupConfigurationException,
      RepositoryException, RepositoryConfigurationException;

   /**
    * Stop backup.
    *
    * @param backup
    *          BackupChain, the backup chain 
    */
   void stopBackup(BackupChain backup);

   /**
    * Finding current backup by repository and workspace.
    *
    * @param reposytore
    *          String, the repository name
    * @param workspace
    *          String, the workspace name
    * @return BackupChain
    *           return the current backup 
    */
   BackupChain findBackup(String reposytore, String workspace);

   /**
    * Finding current backup by identifier.
    *
    * @param backupId
    *          String the backup identifier
    * @return BackupChain
    *           return the current backup
    */
   BackupChain findBackup(String backupId);

   /**
    * Restore from backup.
    *
    * @param log
    *          BackupChainLog, the backup log
    * @param repositoryName
    *          String, repository name
    * @param workspaceEntry
    *          WorkspaceEntry, the workspace entry
    * @throws BackupOperationException
    *           will be generate the exception BackupOperationException 
    * @throws BackupConfigurationException
    *           will be generate the exception BackupConfigurationException 
    * @throws RepositoryException
    *           will be generate the exception RepositoryException 
    * @throws RepositoryConfigurationException
    *           will be generate the exception RepositoryConfigurationException 
    */
   @Deprecated
   void restore(BackupChainLog log, String repositoryName, WorkspaceEntry workspaceEntry)
      throws BackupOperationException, BackupConfigurationException, RepositoryException,
      RepositoryConfigurationException;

   /**
    * Restore from backup.
    *
    * @param log
    *          BackupChainLog, the backup log
    * @param repositoryName
    *          String, repository name
    * @param workspaceEntry
    *          WorkspaceEntry, the workspace entry
    * @param asynchronous
    *          boolean, in 'true' then asynchronous restore.   
    * @throws BackupOperationException
    *           will be generate the exception BackupOperationException 
    * @throws BackupConfigurationException
    *           will be generate the exception BackupConfigurationException 
    * @throws RepositoryException
    *           will be generate the exception RepositoryException 
    * @throws RepositoryConfigurationException
    *           will be generate the exception RepositoryConfigurationException 
    */
   void restore(BackupChainLog log, String repositoryName, WorkspaceEntry workspaceEntry, boolean asynchronous)
      throws BackupOperationException, BackupConfigurationException, RepositoryException,
      RepositoryConfigurationException;

   /**
    * Getting the scheduler.
    *
    * @return BackupScheduler
    *           return the BackupScheduler 
    */
   BackupScheduler getScheduler();

   /**
    * Getting the backup messages.
    *
    * @return BackupMessage[]
    *           return the backup messages
    */
   BackupMessage[] getMessages();

   /**
    * Getting backup directory.
    *
    * @return File
    *           return the backup directory
    */
   File getBackupDirectory();

   /**
    * Getting full backup type.
    *
    * @return Sting
    *           return FQN to full backup type 
    */
   String getFullBackupType();

   /**
    * Getting incremental backup type.
    *
    * @return String
    *         return FQN to full backup type
    */
   String getIncrementalBackupType();

   /**
    * Getting default incremental job period.
    *
    * @return long
    *           return the default incremental job period 
    */
   long getDefaultIncrementalJobPeriod();

   /**
    * Starting repository backup.
    *
    * @param config
    *          RepositoryBackupConfig, the backup configuration to repository
    * @return RepositoryBackupChain
    *           return the repository backup chain
    * @throws BackupOperationException BackupOperationException
    *           will be generate the exception BackupOperationException
    * @throws BackupConfigurationException
    *           will be generate the exception BackupConfigurationException
    * @throws RepositoryException
    *           will be generate the exception RepositoryException
    * @throws RepositoryConfigurationException
    *           will be generate the exception RepositoryConfigurationException
    */
   RepositoryBackupChain startBackup(RepositoryBackupConfig config) throws BackupOperationException,
      BackupConfigurationException, RepositoryException, RepositoryConfigurationException;

   /**
    * Stop backup.
    *
    * @param backup
    *          RepositoryBackupChain, the repositroy backup chain 
    */
   void stopBackup(RepositoryBackupChain backup);

   /**
    * Repository restore from backup.
    *
    * @param log
    *          RepositoryBackupChainLog, the repository backup log
    * @param repositoryEntry
    *          RepositoryEntry, the repository entry
    * @param workspaceNamesCorrespondMap
    *          Map<String, String>, the map with correspondence workspace name in RepositoryEntry and RepositoryBackupChainLog.
    * @param asynchronous
    *          boolean, in 'true' then asynchronous restore.   
    * @throws BackupOperationException
    *           will be generate the exception BackupOperationException 
    * @throws BackupConfigurationException
    *           will be generate the exception BackupConfigurationException 
    * @throws RepositoryException
    *           will be generate the exception RepositoryException 
    * @throws RepositoryConfigurationException
    *           will be generate the exception RepositoryConfigurationException 
    */
   void restore(RepositoryBackupChainLog log, RepositoryEntry repositoryEntry,
      Map<String, String> workspaceNamesCorrespondMap, boolean asynchronous) throws BackupOperationException,
      BackupConfigurationException, RepositoryException, RepositoryConfigurationException;

   /**
    * Repository restore from backup.
    *
    * @param log
    *          RepositoryBackupChainLog, the repository backup log
    * @param repositoryEntry
    *          RepositoryEntry, the repository entry
    * @param asynchronous
    *          boolean, in 'true' then asynchronous restore.   
    * @throws BackupOperationException
    *           will be generate the exception BackupOperationException 
    * @throws BackupConfigurationException
    *           will be generate the exception BackupConfigurationException 
    * @throws RepositoryException
    *           will be generate the exception RepositoryException 
    * @throws RepositoryConfigurationException
    *           will be generate the exception RepositoryConfigurationException 
    */
   void restore(RepositoryBackupChainLog log, RepositoryEntry repositoryEntry, boolean asynchronous)
      throws BackupOperationException, BackupConfigurationException, RepositoryException,
      RepositoryConfigurationException;

   /**
    * Finding current backup by repository.
    *
    * @param reposytore
    *          String, the repository name
    * @return RepositoryBackupChain
    *           return the current backup to repository 
    */
   RepositoryBackupChain findRepositoryBackup(String repository);
   
   /**
    * Finding current backup by id.
    *
    * @param reposytore
    *          String, the repository name
    * @return RepositoryBackupChain
    *           return the current backup to repository 
    */
   RepositoryBackupChain findRepositoryBackupId(String id);
   
}
