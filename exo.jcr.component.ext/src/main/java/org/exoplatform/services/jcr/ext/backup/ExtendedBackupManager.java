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
import org.exoplatform.services.jcr.ext.backup.impl.JobRepositoryRestore;

import java.io.File;

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
    * Restore existing workspace. Previous data will be deleted.
    * For getting status of workspace restore can use 
    * BackupManager.getLastRestore(String repositoryName, String workspaceName) method 
    * 
    * @param workspaceBackupIdentifier
    *          backup identifier
    * @param workspaceEntry
    *          new workspace configuration
    * @param asynchronous
    *          if 'true' restore will be in asynchronous mode (i.e. in separated thread)
    * @throws BackupOperationException
    *           if backup operation exception occurred 
    * @throws BackupConfigurationException
    *           if configuration exception occurred
    */
   void restoreExistingWorkspace(String workspaceBackupIdentifier, String repositoryName, WorkspaceEntry workspaceEntry,
      boolean asynchronous) throws BackupOperationException, BackupConfigurationException;

   /**
    * Restore existing workspace. Previous data will be deleted.
    * For getting status of workspace restore can use 
    * BackupManager.getLastRestore(String repositoryName, String workspaceName) method 
    * 
    * @param log
    *          workspace backup log
    * @param workspaceEntry
    *          new workspace configuration
    * @param asynchronous
    *          if 'true' restore will be in asynchronous mode (i.e. in separated thread)
    * @throws BackupOperationException
    *           if backup operation exception occurred 
    * @throws BackupConfigurationException
    *           if configuration exception occurred
    */
   void restoreExistingWorkspace(BackupChainLog log, String repositoryName, WorkspaceEntry workspaceEntry,
            boolean asynchronous) throws BackupOperationException, BackupConfigurationException;

   /**
    * Restore existing repository. Previous data will be deleted.
    * For getting status of repository restore can use 
    * BackupManager.getLastRestore(String repositoryName) method 
    * 
    * @param repositoryBackupIdentifier
    *          backup identifier
    * @param repositoryEntry
    *          new repository configuration
    * @param asynchronous
    *          if 'true' restore will be in asynchronous mode (i.e. in separated thread)
    * @throws BackupOperationException
    *           if backup operation exception occurred 
    * @throws BackupConfigurationException
    *           if configuration exception occurred
    */
   void restoreExistingRepository(String repositoryBackupIdentifier, RepositoryEntry repositoryEntry,
            boolean asynchronous) throws BackupOperationException, BackupConfigurationException;

   /**
    * Restore existing repository. Previous data will be deleted.
    * For getting status of repository restore can use 
    * BackupManager.getLastRestore(String repositoryName) method 
    * 
    * @param log
    *          repository backup log
    * @param repositoryEntry
    *          new repository configuration
    * @param asynchronous
    *          if 'true' restore will be in asynchronous mode (i.e. in separated thread)
    * @throws BackupOperationException
    *           if backup operation exception occurred 
    * @throws BackupConfigurationException
    *           if configuration exception occurred
    */
   void restoreExistingRepository(RepositoryBackupChainLog log, RepositoryEntry repositoryEntry, boolean asynchronous)
      throws BackupOperationException, BackupConfigurationException;

   /**
    * Restore existing workspace. Previous data will be deleted.
    * For getting status of workspace restore can use 
    * BackupManager.getLastRestore(String repositoryName, String workspaceName) method
    * WorkspaceEntry for restore should be contains in BackupChainLog. 
    * 
    * @param workspaceBackupIdentifier
    *          identifier to workspace backup. 
    * @param asynchronous
    *          if 'true' restore will be in asynchronous mode (i.e. in separated thread) 
    * @throws BackupOperationException
    *           if backup operation exception occurred 
    * @throws BackupConfigurationException
    *           if configuration exception occurred 
    */
   void restoreExistingWorkspace(String workspaceBackupIdentifier, boolean asynchronous)
            throws BackupOperationException,
            BackupConfigurationException;

   /**
    * Restore existing repository. Previous data will be deleted.
    * For getting status of repository restore can use 
    * BackupManager.getLastRestore(String repositoryName) method.
    * ReprositoryEntry for restore should be contains in BackupChainLog. 
    * 
    * @param repositoryBackupIdentifier
    *          identifier to repository backup.   
    * @param asynchronous
    *          if 'true' restore will be in asynchronous mode (i.e. in separated thread)
    * @throws BackupOperationException
    *           if backup operation exception occurred 
    * @throws BackupConfigurationException
    *           if configuration exception occurred
    */
   void restoreExistingRepository(String repositoryBackupIdentifier, boolean asynchronous)
            throws BackupOperationException,
            BackupConfigurationException;

   /**
    * WorkspaceEntry for restore should be contains in BackupChainLog. 
    * 
    * @param workspaceBackupIdentifier
    *          identifier to workspace backup. 
    * @param asynchronous
    *          if 'true' restore will be in asynchronous mode (i.e. in separated thread) 
    * @throws BackupOperationException
    *           if backup operation exception occurred 
    * @throws BackupConfigurationException
    *           if configuration exception occurred 
    */
   void restoreWorkspace(String workspaceBackupIdentifier, boolean asynchronous) throws BackupOperationException,
            BackupConfigurationException;

   /**
    * ReprositoryEntry for restore should be contains in BackupChainLog. 
    * 
    * @param repositoryBackupIdentifier
    *          identifier to repository backup.   
    * @param asynchronous
    *          if 'true' restore will be in asynchronous mode (i.e. in separated thread)
    * @throws BackupOperationException
    *           if backup operation exception occurred 
    * @throws BackupConfigurationException
    *           if configuration exception occurred
    */
   void restoreRepository(String repositoryBackupIdentifier, boolean asynchronous) throws BackupOperationException,
            BackupConfigurationException;

   /**
    * Restore existing workspace. Previous data will be deleted.
    * For getting status of workspace restore can use 
    * BackupManager.getLastRestore(String repositoryName, String workspaceName) method
    * WorkspaceEntry for restore should be contains in BackupChainLog. 
    * 
    * @param workspaceBackupSetDir
    *          the directory with backup set  
    * @param asynchronous
    *          if 'true' restore will be in asynchronous mode (i.e. in separated thread) 
    * @throws BackupOperationException
    *           if backup operation exception occurred 
    * @throws BackupConfigurationException
    *           if configuration exception occurred 
    */
   void restoreExistingWorkspace(File workspaceBackupSetDir, boolean asynchronous)
            throws BackupOperationException, BackupConfigurationException;

   /**
    * Restore existing repository. Previous data will be deleted.
    * For getting status of repository restore can use 
    * BackupManager.getLastRestore(String repositoryName) method.
    * ReprositoryEntry for restore should be contains in BackupChainLog. 
    * 
    * @param repositoryBackupSetDir
    *          the directory with backup set     
    * @param asynchronous
    *          if 'true' restore will be in asynchronous mode (i.e. in separated thread)
    * @throws BackupOperationException
    *           if backup operation exception occurred 
    * @throws BackupConfigurationException
    *           if configuration exception occurred
    */
   void restoreExistingRepository(File repositoryBackupSetDir, boolean asynchronous)
            throws BackupOperationException, BackupConfigurationException;

   /**
    * WorkspaceEntry for restore should be contains in BackupChainLog. 
    * 
    * @param workspaceBackupSetDir
    *          the directory with backup set 
    * @param asynchronous
    *          if 'true' restore will be in asynchronous mode (i.e. in separated thread) 
    * @throws BackupOperationException
    *           if backup operation exception occurred 
    * @throws BackupConfigurationException
    *           if configuration exception occurred 
    */
   void restoreWorkspace(File workspaceBackupSetDir, boolean asynchronous) throws BackupOperationException,
            BackupConfigurationException;

   /**
    * ReprositoryEntry for restore should be contains in BackupChainLog. 
    * 
    * @param repositoryBackupSetDir
    *          the directory with backup set   
    * @param asynchronous
    *          if 'true' restore will be in asynchronous mode (i.e. in separated thread)
    * @throws BackupOperationException
    *           if backup operation exception occurred 
    * @throws BackupConfigurationException
    *           if configuration exception occurred
    */
   void restoreRepository(File repositoryBackupSetDir, boolean asynchronous) throws BackupOperationException,
            BackupConfigurationException;
}
