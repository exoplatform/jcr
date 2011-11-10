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
package org.exoplatform.services.jcr.ext.backup.usecase;

import org.apache.commons.collections.map.HashedMap;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.ext.backup.BackupChain;
import org.exoplatform.services.jcr.ext.backup.BackupChainLog;
import org.exoplatform.services.jcr.ext.backup.BackupConfig;
import org.exoplatform.services.jcr.ext.backup.BackupJob;
import org.exoplatform.services.jcr.ext.backup.BackupManager;
import org.exoplatform.services.jcr.ext.backup.ExtendedBackupManager;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupChain;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupChainLog;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupConfig;
import org.exoplatform.services.jcr.ext.backup.impl.BackupManagerImpl;
import org.exoplatform.services.jcr.ext.backup.impl.JobExistingRepositoryRestore;
import org.exoplatform.services.jcr.ext.backup.impl.JobExistingRepositorySameConfigRestore;
import org.exoplatform.services.jcr.ext.backup.impl.JobExistingWorkspaceRestore;
import org.exoplatform.services.jcr.ext.backup.impl.JobExistingWorkspaceSameConfigRestore;
import org.exoplatform.services.jcr.ext.backup.impl.JobRepositoryRestore;
import org.exoplatform.services.jcr.ext.backup.impl.JobWorkspaceRestore;
import org.exoplatform.services.jcr.impl.backup.Backupable;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.SessionRegistry;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TestLoadBackup.java 111 2008-11-11 11:11:11Z serg $
 */
public class TestBackupRestore extends BaseStandaloneBackupRestoreTest
{

   public void testBackupRestoreExistingRepositorySingleDB() throws Exception
   {
      repositoryBackupRestore("db1", 1);
      repositoryBackupRestore("db1", 2);
   }

   public void testBackupRestoreExistingRepositoryMultiDB() throws Exception
   {
      repositoryBackupRestore("db2", 3);
      repositoryBackupRestore("db2", 4);
   }

   public void testBackupRestoreExistingWorkspaceSingleDB() throws Exception
   {
      workspaceBackupRestore("db1", 5);
      workspaceBackupRestore("db1", 6);
   }

   public void testBackupRestoreExistingWorkspaceMultiDB() throws Exception
   {
      workspaceBackupRestore("db2", 7);
      workspaceBackupRestore("db2", 8);
   }

   /*public void testJobExistingRepositorySameConfigRestoreSingleDB() throws Exception
   {
      repositoryBackupRestoreDirectlyOverJobExistingRepositorySameConfigRestore("db1", 9);
      repositoryBackupRestoreDirectlyOverJobExistingRepositorySameConfigRestore("db1", 10);
   }

   public void testJobExistingRepositorySameConfigRestoreMultiDB() throws Exception
   {
      repositoryBackupRestoreDirectlyOverJobExistingRepositorySameConfigRestore("db2", 10);
      repositoryBackupRestoreDirectlyOverJobExistingRepositorySameConfigRestore("db2", 11);
   }

   public void testJobExistingRepositoryRestoreSingleDB() throws Exception
   {
      repositoryBackupRestoreDirectlyOverJobExistingRepositoryRestore("db1", 12);
      repositoryBackupRestoreDirectlyOverJobExistingRepositoryRestore("db1", 13);
   }

   public void testJobExistingRepositoryRestoreMultiDB() throws Exception
   {
      repositoryBackupRestoreDirectlyOverJobExistingRepositoryRestore("db2", 14);
      repositoryBackupRestoreDirectlyOverJobExistingRepositoryRestore("db2", 15);
   }

   public void testJobRepositoryRestoreSingleDB() throws Exception
   {
      repositoryBackupRestoreDirectlyOverJobRepositoryRestore("db1", 17);
      repositoryBackupRestoreDirectlyOverJobRepositoryRestore("db1", 18);
   }

   public void testJobRepositoryRestoreMultiDB() throws Exception
   {
      repositoryBackupRestoreDirectlyOverJobRepositoryRestore("db2", 19);
      repositoryBackupRestoreDirectlyOverJobRepositoryRestore("db2", 20);
   }

   public void testJobExistingWorkspaceSameConfigRestoreSingleDB() throws Exception
   {
      workspaceBackupRestoreDirectlyOverJobExistingWorkspaceSameConfigRestore("db1", 21);
      workspaceBackupRestoreDirectlyOverJobExistingWorkspaceSameConfigRestore("db1", 22);
   }

   public void testJobExistingWorkspaceSameConfigRestoreMultiDB() throws Exception
   {
      workspaceBackupRestoreDirectlyOverJobExistingWorkspaceSameConfigRestore("db2", 23);
      workspaceBackupRestoreDirectlyOverJobExistingWorkspaceSameConfigRestore("db2", 24);
   }

   public void testJobExistingWorkspaceRestoreSingleDB() throws Exception
   {
      workspaceBackupRestoreDirectlyOverJobExistingWorkspaceRestore("db1", 25);
      workspaceBackupRestoreDirectlyOverJobExistingWorkspaceRestore("db1", 26);
   }

   public void testJobExistingWorkspaceRestoreMultiDB() throws Exception
   {
      workspaceBackupRestoreDirectlyOverJobExistingWorkspaceRestore("db2", 27);
      workspaceBackupRestoreDirectlyOverJobExistingWorkspaceRestore("db2", 28);
   }

   public void testJobWorkspaceRestoreSingleDB() throws Exception
   {
      workspaceBackupRestoreDirectlyOverJobWorkspaceRestore("db1", 29);
      workspaceBackupRestoreDirectlyOverJobWorkspaceRestore("db1", 30);
   }

   public void testJobWorkspaceRestoreMultiDB() throws Exception
   {
      workspaceBackupRestoreDirectlyOverJobWorkspaceRestore("db2", 31);
      workspaceBackupRestoreDirectlyOverJobWorkspaceRestore("db2", 32);
   }*/

   protected void repositoryBackupRestoreDirectlyOverJobExistingRepositorySameConfigRestore(String repositoryName,
      int number) throws Exception
   {
      addConent(repositoryName, number);

      BackupManagerImpl backupManagerImpl = (BackupManagerImpl)getBackupManager();
      backupManagerImpl.start();

      File backDir = new File("target/backup/" + repositoryName);
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repositoryName);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      RepositoryBackupChain bch = backupManagerImpl.startBackup(config);

      // wait till full backup will stop
      while (bch.getState() != BackupJob.FINISHED)
      {
         Thread.yield();
         Thread.sleep(30);
      }

      if (bch != null)
      {
         backupManagerImpl.stopBackup(bch);
      }

      // restore
      RepositoryBackupChainLog rblog = new RepositoryBackupChainLog(new File(bch.getLogFilePath()));

      Map<String, BackupChainLog> workspacesMapping = new HashedMap();
      Map<String, BackupChainLog> backups = new HashedMap();

      for (String path : rblog.getWorkspaceBackupsInfo())
      {
         BackupChainLog bLog = new BackupChainLog(new File(path));
         backups.put(bLog.getBackupConfig().getWorkspace(), bLog);
      }

      for (WorkspaceEntry wsEntry : rblog.getOriginalRepositoryEntry().getWorkspaceEntries())
      {
         workspacesMapping.put(wsEntry.getName(), backups.get(wsEntry.getName()));
      }

      JobExistingRepositorySameConfigRestore job =
         new JobExistingRepositorySameConfigRestore(repositoryService, backupManagerImpl, rblog
            .getOriginalRepositoryEntry(), workspacesMapping, rblog);

      job.run();
      assertEquals(JobRepositoryRestore.REPOSITORY_RESTORE_SUCCESSFUL, job.getStateRestore());

      checkConent(repositoryName, number);
   }

   protected void repositoryBackupRestoreDirectlyOverJobExistingRepositoryRestore(String repositoryName, int number)
      throws Exception
   {
      addConent(repositoryName, number);

      BackupManagerImpl backupManagerImpl = (BackupManagerImpl)getBackupManager();
      backupManagerImpl.start();

      File backDir = new File("target/backup/" + repositoryName);
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repositoryName);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      RepositoryBackupChain bch = backupManagerImpl.startBackup(config);

      // wait till full backup will stop
      while (bch.getState() != BackupJob.FINISHED)
      {
         Thread.yield();
         Thread.sleep(30);
      }

      if (bch != null)
      {
         backupManagerImpl.stopBackup(bch);
      }

      // restore
      RepositoryBackupChainLog rblog = new RepositoryBackupChainLog(new File(bch.getLogFilePath()));

      Map<String, BackupChainLog> workspacesMapping = new HashedMap();
      Map<String, BackupChainLog> backups = new HashedMap();

      for (String path : rblog.getWorkspaceBackupsInfo())
      {
         BackupChainLog bLog = new BackupChainLog(new File(path));
         backups.put(bLog.getBackupConfig().getWorkspace(), bLog);
      }

      for (WorkspaceEntry wsEntry : rblog.getOriginalRepositoryEntry().getWorkspaceEntries())
      {
         workspacesMapping.put(wsEntry.getName(), backups.get(wsEntry.getName()));
      }

      //TODO 
      /*List<WorkspaceContainerFacade> workspacesWaits4Resume = new ArrayList<WorkspaceContainerFacade>();
      ManageableRepository repository = repositoryService.getRepository(repositoryName);
      for (String wsName : repository.getWorkspaceNames())
      {
         WorkspaceContainerFacade wsContainer = repository.getWorkspaceContainer(wsName);
         wsContainer.setState(ManageableRepository.SUSPENDED);

         workspacesWaits4Resume.add(wsContainer);
      }*/
      //

      JobExistingRepositoryRestore job =
         new JobExistingRepositoryRestore(repositoryService, backupManagerImpl, rblog.getOriginalRepositoryEntry(),
            workspacesMapping, rblog);

      job.run();

      //TODO resume components
      /*for (WorkspaceContainerFacade wsContainer : workspacesWaits4Resume)
      {
         wsContainer.setState(ManageableRepository.ONLINE);
      }
      //
      */
      assertEquals(JobRepositoryRestore.REPOSITORY_RESTORE_SUCCESSFUL, job.getStateRestore());

      checkConent(repositoryName, number);
   }

   private int forceCloseSession(String repositoryName, String workspaceName) throws RepositoryException,
      RepositoryConfigurationException
   {
      ManageableRepository mr = repositoryService.getRepository(repositoryName);
      WorkspaceContainerFacade wc = mr.getWorkspaceContainer(workspaceName);

      SessionRegistry sessionRegistry = (SessionRegistry)wc.getComponent(SessionRegistry.class);

      return sessionRegistry.closeSessions(workspaceName);
   }

   protected void repositoryBackupRestoreDirectlyOverJobRepositoryRestore(String repositoryName, int number)
      throws Exception
   {
      addConent(repositoryName, number);

      BackupManagerImpl backupManagerImpl = (BackupManagerImpl)getBackupManager();
      backupManagerImpl.start();

      File backDir = new File("target/backup/" + repositoryName);
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repositoryName);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      RepositoryBackupChain bch = backupManagerImpl.startBackup(config);

      // wait till full backup will stop
      while (bch.getState() != BackupJob.FINISHED)
      {
         Thread.yield();
         Thread.sleep(30);
      }

      if (bch != null)
      {
         backupManagerImpl.stopBackup(bch);
      }

      // restore
      RepositoryBackupChainLog rblog = new RepositoryBackupChainLog(new File(bch.getLogFilePath()));

      // clean existing repository
      // list of components to clean
      List<Backupable> backupable = new ArrayList<Backupable>();

      //Create local copy of WorkspaceEntry for all workspaces
      ArrayList<WorkspaceEntry> workspaceList = new ArrayList<WorkspaceEntry>();
      workspaceList.addAll(rblog.getOriginalRepositoryEntry().getWorkspaceEntries());

      // get all backupable components
      for (WorkspaceEntry wEntry : workspaceList)
      {
         backupable.addAll(repositoryService.getRepository(rblog.getOriginalRepositoryEntry().getName())
            .getWorkspaceContainer(wEntry.getName()).getComponentInstancesOfType(Backupable.class));
      }

      //close all session
      for (WorkspaceEntry wEntry : workspaceList)
      {
         forceCloseSession(rblog.getOriginalRepositoryEntry().getName(), wEntry.getName());
      }

      //remove repository
      repositoryService.removeRepository(rblog.getOriginalRepositoryEntry().getName());

      // clean
      for (Backupable component : backupable)
      {
         component.clean();
      }

      Map<String, BackupChainLog> workspacesMapping = new HashedMap();
      Map<String, BackupChainLog> backups = new HashedMap();

      for (String path : rblog.getWorkspaceBackupsInfo())
      {
         BackupChainLog bLog = new BackupChainLog(new File(path));
         backups.put(bLog.getBackupConfig().getWorkspace(), bLog);
      }

      for (WorkspaceEntry wsEntry : rblog.getOriginalRepositoryEntry().getWorkspaceEntries())
      {
         workspacesMapping.put(wsEntry.getName(), backups.get(wsEntry.getName()));
      }

      JobRepositoryRestore job =
         new JobRepositoryRestore(repositoryService, backupManagerImpl, rblog.getOriginalRepositoryEntry(),
            workspacesMapping, rblog);

      job.run();
      assertEquals(JobRepositoryRestore.REPOSITORY_RESTORE_SUCCESSFUL, job.getStateRestore());

      checkConent(repositoryName, number);
   }

   protected void workspaceBackupRestore(String repositoryName, int number) throws Exception
   {
      addConent(repositoryName, number);
      String workspaceName =
         repositoryService.getRepository(repositoryName).getConfiguration().getSystemWorkspaceName();

      WorkspaceEntry wsEntry = null;
      for (WorkspaceEntry entry : repositoryService.getRepository(repositoryName).getConfiguration()
         .getWorkspaceEntries())
      {
         if (entry.getName().equals(workspaceName))
         {
            wsEntry = entry;
            break;
         }
      }

      BackupManagerImpl backupManagerImpl = (BackupManagerImpl)getBackupManager();
      backupManagerImpl.start();

      File backDir = new File("target/backup/" + repositoryName);
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repositoryName);
      config.setWorkspace(workspaceName);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      BackupChain bch = backupManagerImpl.startBackup(config);

      // wait till full backup will stop
      while (bch.getFullBackupState() != BackupChain.FINISHED)
      {
         Thread.yield();
         Thread.sleep(30);
      }

      if (bch != null)
      {
         backupManagerImpl.stopBackup(bch);
      }

      // restore
      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         BackupChainLog bchLog = new BackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backupManagerImpl.restoreExistingWorkspace(bchLog, repositoryName, wsEntry, false);

         JobWorkspaceRestore restore = backupManagerImpl.getLastRestore(repositoryName, workspaceName);
         assertNotNull(restore);
         //         if (restore != null)
         //         {
         //            if (restore.getStateRestore() != JobWorkspaceRestore.RESTORE_SUCCESSFUL)
         //            {
         //               fail(restore.getRestoreException().getMessage());
         //            }
         //         }
      }
      else
      {
         fail("There are no backup files in " + backDir.getAbsolutePath());
      }

      checkConent(repositoryName, number);
   }

   protected void workspaceBackupRestoreDirectlyOverJobExistingWorkspaceSameConfigRestore(String repositoryName,
      int number) throws Exception
   {
      addConent(repositoryName, number);
      String workspaceName =
         repositoryService.getRepository(repositoryName).getConfiguration().getSystemWorkspaceName();

      WorkspaceEntry wsEntry = null;
      for (WorkspaceEntry entry : repositoryService.getRepository(repositoryName).getConfiguration()
         .getWorkspaceEntries())
      {
         if (entry.getName().equals(workspaceName))
         {
            wsEntry = entry;
            break;
         }
      }

      BackupManagerImpl backupManagerImpl = (BackupManagerImpl)getBackupManager();
      backupManagerImpl.start();

      File backDir = new File("target/backup/" + repositoryName);
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repositoryName);
      config.setWorkspace(workspaceName);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      BackupChain bch = backupManagerImpl.startBackup(config);

      // wait till full backup will stop
      while (bch.getFullBackupState() != BackupChain.FINISHED)
      {
         Thread.yield();
         Thread.sleep(30);
      }

      if (bch != null)
      {
         backupManagerImpl.stopBackup(bch);
      }

      // restore
      BackupChainLog bclog = new BackupChainLog(new File(bch.getLogFilePath()));

      JobExistingWorkspaceSameConfigRestore job =
         new JobExistingWorkspaceSameConfigRestore(repositoryService, backupManagerImpl, repositoryName, bclog, bclog
            .getOriginalWorkspaceEntry());

      job.run();
      assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, job.getStateRestore());

      checkConent(repositoryName, number);
   }

   /**
    * JobExistingWorkspaseRrestore is not support restore system workspace, 
    * because repository is not allowed removing system workspace.  
    */
   protected void workspaceBackupRestoreDirectlyOverJobExistingWorkspaceRestore(String repositoryName,
      int number) throws Exception
   {
      addConent(repositoryName, number);

      WorkspaceEntry wsEntry = null;
      for (WorkspaceEntry entry : repositoryService.getRepository(repositoryName).getConfiguration()
         .getWorkspaceEntries())
      {
         if (!entry.getName().equals(
            repositoryService.getRepository(repositoryName).getConfiguration().getSystemWorkspaceName()))
         {
            wsEntry = entry;
            break;
         }
      }

      BackupManagerImpl backupManagerImpl = (BackupManagerImpl)getBackupManager();
      backupManagerImpl.start();

      File backDir = new File("target/backup/" + repositoryName);
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repositoryName);
      config.setWorkspace(wsEntry.getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      BackupChain bch = backupManagerImpl.startBackup(config);

      // wait till full backup will stop
      while (bch.getFullBackupState() != BackupChain.FINISHED)
      {
         Thread.yield();
         Thread.sleep(30);
      }

      if (bch != null)
      {
         backupManagerImpl.stopBackup(bch);
      }

      // restore
      BackupChainLog bclog = new BackupChainLog(new File(bch.getLogFilePath()));

      JobExistingWorkspaceRestore job =
         new JobExistingWorkspaceRestore(repositoryService, backupManagerImpl, repositoryName, bclog, bclog
            .getOriginalWorkspaceEntry());

      job.run();
      assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, job.getStateRestore());

      checkConent(repositoryName, number);
   }

   /**
    * JobWorkspaseRrestore is not support restore system workspace, 
    * because repository is not allowed removing system workspace.  
    */
   protected void workspaceBackupRestoreDirectlyOverJobWorkspaceRestore(String repositoryName, int number)
      throws Exception
   {
      addConent(repositoryName, number);

      WorkspaceEntry wsEntry = null;
      for (WorkspaceEntry entry : repositoryService.getRepository(repositoryName).getConfiguration()
         .getWorkspaceEntries())
      {
         if (!entry.getName().equals(
            repositoryService.getRepository(repositoryName).getConfiguration().getSystemWorkspaceName()))
         {
            wsEntry = entry;
            break;
         }
      }

      String workspaceName = wsEntry.getName();

      BackupManagerImpl backupManagerImpl = (BackupManagerImpl)getBackupManager();
      backupManagerImpl.start();

      File backDir = new File("target/backup/" + repositoryName);
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repositoryName);
      config.setWorkspace(workspaceName);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      BackupChain bch = backupManagerImpl.startBackup(config);

      // wait till full backup will stop
      while (bch.getFullBackupState() != BackupChain.FINISHED)
      {
         Thread.yield();
         Thread.sleep(30);
      }

      if (bch != null)
      {
         backupManagerImpl.stopBackup(bch);
      }

      // clean existed workspace
      // get all backupable components
      List<Backupable> backupable =
         repositoryService.getRepository(repositoryName).getWorkspaceContainer(wsEntry.getName())
            .getComponentInstancesOfType(Backupable.class);

      // close all session
      forceCloseSession(repositoryName, wsEntry.getName());

      repositoryService.getRepository(repositoryName).removeWorkspace(wsEntry.getName());

      // clean
      for (Backupable component : backupable)
      {
         component.clean();
      }

      // restore
      BackupChainLog bclog = new BackupChainLog(new File(bch.getLogFilePath()));

      JobWorkspaceRestore job =
         new JobWorkspaceRestore(repositoryService, backupManagerImpl, repositoryName, bclog, bclog
            .getOriginalWorkspaceEntry());

      job.run();
      assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, job.getStateRestore());

      checkConent(repositoryName, number);
   }

   protected void repositoryBackupRestore(String repositoryName, int number) throws Exception
   {
      addConent(repositoryName, number);

      BackupManagerImpl backupManagerImpl = (BackupManagerImpl)getBackupManager();
      backupManagerImpl.start();

      File backDir = new File("target/backup/" + repositoryName);
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repositoryName);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      RepositoryBackupChain bch = backupManagerImpl.startBackup(config);

      // wait till full backup will stop
      while (bch.getState() != BackupJob.FINISHED)
      {
         Thread.yield();
         Thread.sleep(30);
      }

      if (bch != null)
      {
         backupManagerImpl.stopBackup(bch);
      }

      // restore
      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backupManagerImpl.restoreExistingRepository(bchLog, repositoryService.getRepository(repositoryName)
            .getConfiguration(), false);

         JobRepositoryRestore restore = backupManagerImpl.getLastRepositoryRestore(repositoryName);
         if (restore != null)
         {
            if (restore.getStateRestore() != JobRepositoryRestore.REPOSITORY_RESTORE_SUCCESSFUL)
            {
               fail(restore.getRestoreException().getMessage());
            }
         }
      }
      else
      {
         fail("There are no backup files in " + backDir.getAbsolutePath());
      }

      checkConent(repositoryName, number);
   }

   protected ExtendedBackupManager getBackupManager()
   {
      InitParams initParams = new InitParams();
      PropertiesParam pps = new PropertiesParam();

      pps.setProperty(BackupManagerImpl.FULL_BACKUP_TYPE,
         "org.exoplatform.services.jcr.ext.backup.impl.rdbms.FullBackupJob");
      pps.setProperty(BackupManagerImpl.BACKUP_DIR, "target/backup");
      initParams.put(BackupManagerImpl.BACKUP_PROPERTIES, pps);

      return new BackupManagerImpl(initParams, repositoryService);
   }

   protected void addConent(String repositoryName, int number) throws Exception
   {
      ManageableRepository repository = repositoryService.getRepository(repositoryName);
      for (String wsName : repository.getWorkspaceNames())
      {
         SessionImpl session = (SessionImpl)repository.login(credentials, wsName);
         try
         {
            Node rootNode = session.getRootNode().addNode("test" + number);

            rootNode.addNode("node1").setProperty("prop1", "value1");
            session.save();
         }
         finally
         {
            session.logout();
         }
      }
   }

   protected void checkConent(String repositoryName, int number) throws Exception
   {
      ManageableRepository repository = repositoryService.getRepository(repositoryName);
      for (String wsName : repository.getWorkspaceNames())
      {
         SessionImpl session = (SessionImpl)repository.login(credentials, wsName);
         try
         {
            Node rootNode = session.getRootNode().getNode("test" + number);
            assertEquals(rootNode.getNode("node1").getProperty("prop1").getString(), "value1");
         }
         finally
         {
            session.logout();
         }
      }
   }
}
