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
import org.exoplatform.services.jcr.impl.clean.rdbms.DBCleanException;
import org.exoplatform.services.jcr.impl.clean.rdbms.DBCleanService;
import org.exoplatform.services.jcr.impl.clean.rdbms.DBCleanerTool;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.SessionRegistry;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializerHelper;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.naming.InitialContext;
import javax.sql.DataSource;

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

   public int index = 0;

   public void testBackupRestoreExistingRepositorySingleDB() throws Exception
   {
      repositoryBackupRestore("db3");
      repositoryBackupRestore("db3");
   }

   public void testBackupRestoreExistingRepositoryMultiDB() throws Exception
   {
      repositoryBackupRestore("db4");
      repositoryBackupRestore("db4");
   }

   public void testBackupRestoreExistingWorkspaceSingleDB() throws Exception
   {
      workspaceBackupRestore("db3");
      workspaceBackupRestore("db3");
   }

   public void testBackupRestoreExistingWorkspaceMultiDB() throws Exception
   {
      workspaceBackupRestore("db4");
      workspaceBackupRestore("db4");
   }

   public void testJobExistingRepositorySameConfigRestoreSingleDB() throws Exception
   {
      repositoryBackupRestoreDirectlyOverJobExistingRepositorySameConfigRestore("db3");
      repositoryBackupRestoreDirectlyOverJobExistingRepositorySameConfigRestore("db3");
   }

   public void testJobExistingRepositorySameConfigRestoreMultiDB() throws Exception
   {
      repositoryBackupRestoreDirectlyOverJobExistingRepositorySameConfigRestore("db4");
      repositoryBackupRestoreDirectlyOverJobExistingRepositorySameConfigRestore("db4");
   }

   public void testJobExistingRepositoryRestoreSingleDB() throws Exception
   {
      repositoryBackupRestoreDirectlyOverJobExistingRepositoryRestore("db3");
      repositoryBackupRestoreDirectlyOverJobExistingRepositoryRestore("db3");
   }

   public void testJobExistingRepositoryRestoreMultiDB() throws Exception
   {
      repositoryBackupRestoreDirectlyOverJobExistingRepositoryRestore("db4");
      repositoryBackupRestoreDirectlyOverJobExistingRepositoryRestore("db4");
   }

   public void testJobRepositoryRestoreSingleDB() throws Exception
   {
      repositoryBackupRestoreDirectlyOverJobRepositoryRestore("db3");
      repositoryBackupRestoreDirectlyOverJobRepositoryRestore("db3");
   }

   public void testJobRepositoryRestoreMultiDB() throws Exception
   {
      repositoryBackupRestoreDirectlyOverJobRepositoryRestore("db4");
      repositoryBackupRestoreDirectlyOverJobRepositoryRestore("db4");
   }

   public void testJobExistingWorkspaceSameConfigRestoreSingleDB() throws Exception
   {
      workspaceBackupRestoreDirectlyOverJobExistingWorkspaceSameConfigRestore("db3");
      workspaceBackupRestoreDirectlyOverJobExistingWorkspaceSameConfigRestore("db3");
   }

   public void testJobExistingWorkspaceSameConfigRestoreMultiDB() throws Exception
   {
      workspaceBackupRestoreDirectlyOverJobExistingWorkspaceSameConfigRestore("db4");
      workspaceBackupRestoreDirectlyOverJobExistingWorkspaceSameConfigRestore("db4");
   }

   public void testJobExistingWorkspaceRestoreSingleDB() throws Exception
   {
      workspaceBackupRestoreDirectlyOverJobExistingWorkspaceRestore("db3");
      workspaceBackupRestoreDirectlyOverJobExistingWorkspaceRestore("db3");
   }

   public void testJobExistingWorkspaceRestoreMultiDB() throws Exception
   {
      workspaceBackupRestoreDirectlyOverJobExistingWorkspaceRestore("db4");
      workspaceBackupRestoreDirectlyOverJobExistingWorkspaceRestore("db4");
   }

   public void testJobWorkspaceRestoreSingleDB() throws Exception
   {
      workspaceBackupRestoreDirectlyOverJobWorkspaceRestore("db3");
      workspaceBackupRestoreDirectlyOverJobWorkspaceRestore("db3");
   }

   public void testJobWorkspaceRestoreMultiDB() throws Exception
   {
      workspaceBackupRestoreDirectlyOverJobWorkspaceRestore("db4");
      workspaceBackupRestoreDirectlyOverJobWorkspaceRestore("db4");
   }

   protected void repositoryBackupRestoreDirectlyOverJobExistingRepositorySameConfigRestore(String repositoryName)
      throws Exception
   {
      addConent(repositoryName);

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

      Map<String, BackupChainLog> workspacesMapping = new HashMap<String, BackupChainLog>();
      Map<String, BackupChainLog> backups = new HashMap<String, BackupChainLog>();

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

      checkConent(repositoryName);
   }

   protected void repositoryBackupRestoreDirectlyOverJobExistingRepositoryRestore(String repositoryName)
      throws Exception
   {
      addConent(repositoryName);

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

      Map<String, BackupChainLog> workspacesMapping = new HashMap<String, BackupChainLog>();
      Map<String, BackupChainLog> backups = new HashMap<String, BackupChainLog>();

      for (String path : rblog.getWorkspaceBackupsInfo())
      {
         BackupChainLog bLog = new BackupChainLog(new File(path));
         backups.put(bLog.getBackupConfig().getWorkspace(), bLog);
      }

      for (WorkspaceEntry wsEntry : rblog.getOriginalRepositoryEntry().getWorkspaceEntries())
      {
         workspacesMapping.put(wsEntry.getName(), backups.get(wsEntry.getName()));
      }

      JobExistingRepositoryRestore job =
         new JobExistingRepositoryRestore(repositoryService, backupManagerImpl, rblog.getOriginalRepositoryEntry(),
            workspacesMapping, rblog);

      job.run();

      assertEquals(JobRepositoryRestore.REPOSITORY_RESTORE_SUCCESSFUL, job.getStateRestore());

      checkConent(repositoryName);
   }

   private int forceCloseSession(String repositoryName, String workspaceName) throws RepositoryException,
      RepositoryConfigurationException
   {
      ManageableRepository mr = repositoryService.getRepository(repositoryName);
      WorkspaceContainerFacade wc = mr.getWorkspaceContainer(workspaceName);

      SessionRegistry sessionRegistry = (SessionRegistry)wc.getComponent(SessionRegistry.class);

      return sessionRegistry.closeSessions(workspaceName);
   }

   protected void repositoryBackupRestoreDirectlyOverJobRepositoryRestore(String repositoryName)
      throws Exception
   {
      addConent(repositoryName);

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

      Map<String, BackupChainLog> workspacesMapping = new HashMap<String, BackupChainLog>();
      Map<String, BackupChainLog> backups = new HashMap<String, BackupChainLog>();

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

      checkConent(repositoryName);
   }

   protected void workspaceBackupRestore(String repositoryName) throws Exception
   {
      addConent(repositoryName);
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
      }
      else
      {
         fail("There are no backup files in " + backDir.getAbsolutePath());
      }

      checkConent(repositoryName);
   }

   protected void workspaceBackupRestoreDirectlyOverJobExistingWorkspaceSameConfigRestore(String repositoryName)
      throws Exception
   {
      addConent(repositoryName);
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

      checkConent(repositoryName);
   }

   /**
    * JobExistingWorkspaseRrestore is not support restore system workspace, 
    * because repository is not allowed removing system workspace.  
    */
   protected void workspaceBackupRestoreDirectlyOverJobExistingWorkspaceRestore(String repositoryName) throws Exception
   {
      addConent(repositoryName);

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

      checkConent(repositoryName);
   }

   /**
    * JobWorkspaseRrestore is not support restore system workspace, 
    * because repository is not allowed removing system workspace.  
    */
   protected void workspaceBackupRestoreDirectlyOverJobWorkspaceRestore(String repositoryName)
      throws Exception
   {
      addConent(repositoryName);

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

      checkConent(repositoryName);
   }

   protected void repositoryBackupRestore(String repositoryName) throws Exception
   {
      // prepare content
      addConent(repositoryName);

      // prepare backupManager
      BackupManagerImpl backupManagerImpl = (BackupManagerImpl)getBackupManager();
      backupManagerImpl.start();

      // backup
      File backDir = new File("target/backup/" + repositoryName);
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repositoryName);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      RepositoryBackupChain bch = backupManagerImpl.startBackup(config);

      // wait end of backup
      while (bch.getState() != BackupJob.FINISHED)
      {
         Thread.yield();
         Thread.sleep(30);
      }

      if (bch != null)
      {
         backupManagerImpl.stopBackup(bch);
      }

      // clean repository via DBCleanService
      DBCleanService.cleanRepositoryData(repositoryService.getRepository(repositoryName).getConfiguration());
      checkEmptyTables(repositoryName);

      // restore
      restore(repositoryName, bch, backupManagerImpl);
      checkConent(repositoryName);

      // clean every workspace via DBCleanService
      ManageableRepository repository = repositoryService.getRepository(repositoryName);
      for (WorkspaceEntry wsEntry : repository.getConfiguration().getWorkspaceEntries())
      {
         DBCleanService.cleanWorkspaceData(wsEntry);
      }
      checkEmptyTables(repositoryName);

      // restore
      restore(repositoryName, bch, backupManagerImpl);
      checkConent(repositoryName);

      // clean again via DBCleaner
      repository = repositoryService.getRepository(repositoryName);

      WorkspaceEntry entry = repository.getConfiguration().getWorkspaceEntries().get(0);
      DataSource ds =
         (DataSource)new InitialContext().lookup(entry.getContainer().getParameterValue(
            JDBCWorkspaceDataContainer.SOURCE_NAME));

      Connection conn = ds.getConnection();
      conn.setAutoCommit(false);

      try
      {
         if (repositoryName.equals("db3"))
         {
            DBCleanerTool repositoryDBCleaner =
               DBCleanService.getRepositoryDBCleaner(conn, repository.getConfiguration());

            // clean and rollback first
            repositoryDBCleaner.clean();

            conn.rollback();

            repositoryDBCleaner.rollback();

            conn.commit();

            checkConent(repositoryName);

            // clean
            repositoryDBCleaner.clean();
            repositoryDBCleaner.commit();

            conn.commit();
         }
         else
         {
            try
            {
               DBCleanerTool repositoryDBCleaner =
                  DBCleanService.getRepositoryDBCleaner(conn, repository.getConfiguration());
               fail("Exception should be thrown");
            }
            catch (DBCleanException e)
            {
            }
         }
      }
      finally
      {
         conn.close();
      }

      if (repositoryName.equals("db3"))
      {
         checkEmptyTables(repositoryName);
      }

      // restore
      restore(repositoryName, bch, backupManagerImpl);
      checkConent(repositoryName);

      // clean every workspace again via DBCleaner
      repository = repositoryService.getRepository(repositoryName);
      for (WorkspaceEntry wsEntry : repository.getConfiguration().getWorkspaceEntries())
      {
         ds =
            (DataSource)new InitialContext().lookup(wsEntry.getContainer().getParameterValue(
               JDBCWorkspaceDataContainer.SOURCE_NAME));

         conn = ds.getConnection();
         conn.setAutoCommit(false);

         DBCleanerTool workspaceDBCleaner = DBCleanService.getWorkspaceDBCleaner(conn, wsEntry);

         try
         {
            // clean and rollback first
            workspaceDBCleaner.clean();

            conn.rollback();

            workspaceDBCleaner.rollback();

            conn.commit();

            checkConent(repositoryName);

            // clean
            workspaceDBCleaner.clean();
            workspaceDBCleaner.commit();

            conn.commit();
         }
         finally
         {
            conn.close();
         }
      }

      checkEmptyTables(repositoryName);

      // restore
      restore(repositoryName, bch, backupManagerImpl);
      checkConent(repositoryName);
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

   protected void addConent(String repositoryName) throws Exception
   {
      index++;

      ManageableRepository repository = repositoryService.getRepository(repositoryName);
      for (String wsName : repository.getWorkspaceNames())
      {
         SessionImpl session = (SessionImpl)repository.login(credentials, wsName);
         try
         {
            Node rootNode = session.getRootNode().addNode("test" + index);

            rootNode.addNode("node1").setProperty("prop1", "value1");
            session.save();
         }
         finally
         {
            session.logout();
         }
      }
   }

   protected void checkConent(String repositoryName) throws Exception
   {
      ManageableRepository repository = repositoryService.getRepository(repositoryName);
      for (String wsName : repository.getWorkspaceNames())
      {
         SessionImpl session = (SessionImpl)repository.login(credentials, wsName);
         try
         {
            Node rootNode = session.getRootNode().getNode("test" + index);
            assertEquals(rootNode.getNode("node1").getProperty("prop1").getString(), "value1");
         }
         finally
         {
            session.logout();
         }
      }
   }

   private void restore(String repositoryName, RepositoryBackupChain bch, BackupManagerImpl backupManagerImpl)
      throws Exception
   {
      // restore
      File backLog = new File(bch.getLogFilePath());
      assertTrue(backLog.exists());

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

   private void checkEmptyTables(String repositoryName) throws Exception
   {
      ManageableRepository repository = repositoryService.getRepository(repositoryName);
      for (WorkspaceEntry wsEntry : repository.getConfiguration().getWorkspaceEntries())
      {
         String itemTableName = DBInitializerHelper.getItemTableName(wsEntry);
         String valueTableName = DBInitializerHelper.getValueTableName(wsEntry);
         String refTableName = DBInitializerHelper.getRefTableName(wsEntry);

         DataSource ds =
            (DataSource)new InitialContext().lookup(wsEntry.getContainer().getParameterValue(
               JDBCWorkspaceDataContainer.SOURCE_NAME));
         Connection conn = ds.getConnection();
         try
         {
            ResultSet result = conn.createStatement().executeQuery("SELECT COUNT(*) FROM " + itemTableName);
            try
            {
               assertTrue(result.next());
               assertEquals(1, result.getInt(1));
            }
            finally
            {
               result.close();
            }

            result = conn.createStatement().executeQuery("SELECT COUNT(*) FROM " + valueTableName);
            try
            {
               assertTrue(result.next());
               assertEquals(0, result.getInt(1));
            }
            finally
            {
               result.close();
            }

            result = conn.createStatement().executeQuery("SELECT COUNT(*) FROM " + refTableName);
            try
            {
               assertTrue(result.next());
               assertEquals(0, result.getInt(1));
            }
            finally
            {
               result.close();
            }
         }
         finally
         {
            conn.close();
         }
      }
   }
}
