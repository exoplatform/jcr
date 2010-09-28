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

import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 2010
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id$
 */
public interface ExtendedBackupManager extends BackupManager
{
   /**
    * Restoration over an existing repository.
    * Will be deleted old data.
    * Get status of repository restore was necessary use JobRepositoryRestore BackupManager.getLastRestore(String repositoryName). 
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
    */
   void restoreExistedRepository(RepositoryBackupChainLog log, RepositoryEntry repositoryEntry, boolean asynchronous)  throws BackupOperationException, BackupConfigurationException;
   
   /**
    * Restoration over an existing workspace.
    * Will be deleted old data.
    * Get status of workspace restore was necessary use JobWorkspaceRestore BackupManager.getLastRestore(String repositoryName, String workspaceName).
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
    */
   void restoreExistedWorkspace(BackupChainLog log, String repositoryName, WorkspaceEntry workspaceEntry, boolean asynchronous)  throws BackupOperationException, BackupConfigurationException;
   
   /**
    * Restoration over an existing repository.
    * Will be deleted old data.
    * Get status of repository restore was necessary use JobRepositoryRestore BackupManager.getLastRestore(String repositoryName). 
    * 
    * @param repositoryBackupIdentifier
    *          String, identifier of repository backup
    * @param repositoryEntry
    *          RepositoryEntry, the repository entry
    * @param asynchronous
    *          boolean, in 'true' then asynchronous restore.   
    * @throws BackupOperationException
    *           will be generate the exception BackupOperationException 
    * @throws BackupConfigurationException
    *           will be generate the exception BackupConfigurationException
    */
   void restoreExistedRepository(String  repositoryBackupIdentifier, RepositoryEntry repositoryEntry, boolean asynchronous)  throws BackupOperationException, BackupConfigurationException;
   
   /**
    * Restoration over an existing workspace.
    * Will be deleted old data.
    * Get status of workspace restore was necessary use JobWorkspaceRestore BackupManager.getLastRestore(String repositoryName, String workspaceName).
    * 
    * @param workspaceBackupIdentifier
    *          String, identifier of workspace backup
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
    */
   void restoreExistedWorkspace(String workspaceBackupIdentifier, String repositoryName, WorkspaceEntry workspaceEntry, boolean asynchronous)  throws BackupOperationException, BackupConfigurationException;
}
