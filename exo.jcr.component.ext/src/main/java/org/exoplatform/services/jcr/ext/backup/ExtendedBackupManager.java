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


/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 2010
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id: ExtendedBackupManager.java 3545 2010-11-24 15:35:31Z areshetnyak $
 */
public interface ExtendedBackupManager
   extends BackupManager
{

   //   TODO Will be uncommented after fix issue JCR-1054   
   //   /**
   //    * Restore existing workspace. Previous data will be deleted.
   //    * For getting status of workspace restore can use 
   //    * BackupManager.getLastRestore(String repositoryName, String workspaceName) method
   //    * WorkspaceEntry for restore should be contains in BackupChainLog. 
   //    * 
   //    * @param workspaceBackupIdentifier
   //    *          identifier to workspace backup. 
   //    * @param asynchronous
   //    *          if 'true' restore will be in asynchronous mode (i.e. in separated thread) 
   //    * @throws BackupOperationException
   //    *           if backup operation exception occurred 
   //    * @throws BackupConfigurationException
   //    *           if configuration exception occurred 
   //    */
   //   void restoreExistingWorkspace(String workspaceBackupIdentifier, boolean asynchronous)
   //            throws BackupOperationException, BackupConfigurationException;
   //
   //   /**
   //    * Restore existing repository. Previous data will be deleted.
   //    * For getting status of repository restore can use 
   //    * BackupManager.getLastRestore(String repositoryName) method.
   //    * ReprositoryEntry for restore should be contains in BackupChainLog. 
   //    * 
   //    * @param repositoryBackupIdentifier
   //    *          identifier to repository backup.   
   //    * @param asynchronous
   //    *          if 'true' restore will be in asynchronous mode (i.e. in separated thread)
   //    * @throws BackupOperationException
   //    *           if backup operation exception occurred 
   //    * @throws BackupConfigurationException
   //    *           if configuration exception occurred
   //    */
   //   void restoreExistingRepository(String repositoryBackupIdentifier, boolean asynchronous)
   //            throws BackupOperationException, BackupConfigurationException;

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
}
