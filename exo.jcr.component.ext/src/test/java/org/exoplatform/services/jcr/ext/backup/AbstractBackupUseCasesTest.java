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

import org.apache.commons.collections.map.HashedMap;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.backup.impl.JobRepositoryRestore;
import org.exoplatform.services.jcr.ext.backup.impl.JobWorkspaceRestore;
import org.exoplatform.services.jcr.util.IdGenerator;

import java.io.File;
import java.util.Map;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 2010
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id$
 */
public abstract class AbstractBackupUseCasesTest extends AbstractBackupTestCase
{
   
   public void testFullBackupRestore() throws Exception
   {
      // prepare
      ManageableRepository repository = helper.createRepository(container, true, null);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(true, null);
      helper.addWorkspace(repository, wsEntry);
      addConent(repository, wsEntry.getName());

      // backup
      File backDir = new File("target/backup/" + IdGenerator.generate());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getConfiguration().getName());
      config.setWorkspace(wsEntry.getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      BackupChain bch = backup.startBackup(config);
      waitEndOfBackup(bch);
      backup.stopBackup(bch);

      // restore
      WorkspaceEntry newWS = helper.createWorkspaceEntry(true, null);

      File backLog = new File(bch.getLogFilePath());
      assertTrue(backLog.exists());

      BackupChainLog bchLog = new BackupChainLog(backLog);

      assertNotNull(bchLog.getStartedTime());
      assertNotNull(bchLog.getFinishedTime());

      backup.restore(bchLog, config.getRepository(), newWS, false);
      checkConent(repository, newWS.getName());
   }

   public void testIncrementalBackupRestore() throws Exception
   {
      // prepare
      ManageableRepository repository = helper.createRepository(container, true, null);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(true, null);
      helper.addWorkspace(repository, wsEntry);
      addConent(repository, wsEntry.getName());

      // backup
      File backDir = new File("target/backup/" + IdGenerator.generate());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getConfiguration().getName());
      config.setWorkspace(wsEntry.getName());
      config.setBackupType(BackupManager.FULL_AND_INCREMENTAL);
      config.setBackupDir(backDir);

      BackupChain bch = backup.startBackup(config);
      waitEndOfBackup(bch);

      addIncrementalConent(repository, wsEntry.getName());

      backup.stopBackup(bch);

      // restore
      WorkspaceEntry newWS = helper.createWorkspaceEntry(true, null);

      File backLog = new File(bch.getLogFilePath());
      assertTrue(backLog.exists());

      BackupChainLog bchLog = new BackupChainLog(backLog);

      assertNotNull(bchLog.getStartedTime());
      assertNotNull(bchLog.getFinishedTime());

      backup.restore(bchLog, config.getRepository(), newWS, false);
      checkConent(repository, newWS.getName());
      checkIncrementalConent(repository, newWS.getName());
   }

   public void testFullBackupRestoreAsync() throws Exception
   {
      // prepare
      ManageableRepository repository = helper.createRepository(container, true, null);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(true, null);
      helper.addWorkspace(repository, wsEntry);
      addConent(repository, wsEntry.getName());

      // backup
      File backDir = new File("target/backup/" + IdGenerator.generate());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getConfiguration().getName());
      config.setWorkspace(wsEntry.getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      BackupChain bch = backup.startBackup(config);
      waitEndOfBackup(bch);
      backup.stopBackup(bch);

      // restore
      WorkspaceEntry newWS = helper.createWorkspaceEntry(true, null);

      File backLog = new File(bch.getLogFilePath());
      assertTrue(backLog.exists());

      BackupChainLog bchLog = new BackupChainLog(backLog);

      assertNotNull(bchLog.getStartedTime());
      assertNotNull(bchLog.getFinishedTime());

      backup.restore(bchLog, config.getRepository(), newWS, true);
      waitEndOfRestore(config.getRepository(), newWS.getName());

      assertEquals(backup.getLastRestore(config.getRepository(), newWS.getName()).getStateRestore(),
         JobWorkspaceRestore.RESTORE_SUCCESSFUL);
      checkConent(repository, newWS.getName());
   }

   public void testAutoStopBackupFull() throws Exception
   {
      // prepare
      ManageableRepository repository = helper.createRepository(container, true, null);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(true, null);
      helper.addWorkspace(repository, wsEntry);

      // backup
      File backDir = new File("target/backup/" + IdGenerator.generate());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getConfiguration().getName());
      config.setWorkspace(wsEntry.getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      BackupChain bch = backup.startBackup(config);
      waitEndOfBackup(bch);

      Thread.sleep(30000);
      try
      {
         assertEquals(backup.getCurrentBackups().size(), 0);
      }
      finally
      {
         backup.stopBackup(bch);
      }
   }

   public void testAutoStopBackupIncr() throws Exception
   {
      // prepare
      ManageableRepository repository = helper.createRepository(container, true, null);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(true, null);
      helper.addWorkspace(repository, wsEntry);

      // backup
      File backDir = new File("target/backup/" + IdGenerator.generate());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getConfiguration().getName());
      config.setWorkspace(wsEntry.getName());
      config.setBackupType(BackupManager.FULL_AND_INCREMENTAL);
      config.setIncrementalJobPeriod(3);
      config.setIncrementalJobNumber(0);
      config.setBackupDir(backDir);

      BackupChain bch = backup.startBackup(config);
      waitEndOfBackup(bch);

      Thread.sleep(10000);
      try
      {
         assertEquals(backup.getCurrentBackups().size(), 1);
      }
      finally
      {
         backup.stopBackup(bch);
      }
   }

   public void _testAutoStopBackupIncrRepetion() throws Exception
   {
      // prepare
      ManageableRepository repository = helper.createRepository(container, true, null);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(true, null);
      helper.addWorkspace(repository, wsEntry);

      // backup
      File backDir = new File("target/backup/" + IdGenerator.generate());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getConfiguration().getName());
      config.setWorkspace(wsEntry.getName());
      config.setBackupType(BackupManager.FULL_AND_INCREMENTAL);
      config.setIncrementalJobPeriod(4);
      config.setIncrementalJobNumber(2);
      config.setBackupDir(backDir);

      BackupChain bch = backup.startBackup(config);
      waitEndOfBackup(bch);

      Thread.sleep(60000);
      try
      {
         assertEquals(backup.getCurrentBackups().size(), 0);
      }
      finally
      {
         backup.stopBackup(bch);
      }
   }

   public void testNegativeIncremetalJobPeriod() throws Exception
   {
      // backup
      File backDir = new File("target/backup/" + IdGenerator.generate());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository("fake");
      config.setWorkspace("fake");
      config.setBackupType(BackupManager.FULL_AND_INCREMENTAL);
      config.setIncrementalJobPeriod(-1000);
      config.setBackupDir(backDir);

      try
      {
         backup.startBackup(config);
         fail("The backup can not be started.");
      }
      catch (BackupConfigurationException e)
      {
      }
   }

   public void testNegativeIncremetalJobNumber() throws Exception
   {
      // backup
      File backDir = new File("target/backup/" + IdGenerator.generate());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository("fake");
      config.setWorkspace("fake");
      config.setBackupType(BackupManager.FULL_AND_INCREMENTAL);
      config.setIncrementalJobNumber(-5);
      config.setBackupDir(backDir);

      try
      {
         backup.startBackup(config);
         fail("The backup can not be started.");
      }
      catch (BackupConfigurationException e)
      {
      }
   }

   public void testRestoreAfterFailureRestore() throws Exception
   {
      // prepare
      ManageableRepository repository = helper.createRepository(container, true, null);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(true, null);
      helper.addWorkspace(repository, wsEntry);
      addConent(repository, wsEntry.getName());

      // backup
      File backDir = new File("target/backup/" + IdGenerator.generate());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getConfiguration().getName());
      config.setWorkspace(wsEntry.getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      BackupChain bch = backup.startBackup(config);
      waitEndOfBackup(bch);
      backup.stopBackup(bch);

      // restore
      WorkspaceEntry newWS = helper.createWorkspaceEntry(true, "NOT_EXISTED_DS");

      File backLog = new File(bch.getLogFilePath());
      assertTrue(backLog.exists());

      BackupChainLog bchLog = new BackupChainLog(backLog);

      assertNotNull(bchLog.getStartedTime());
      assertNotNull(bchLog.getFinishedTime());

      try
      {
         backup.restore(bchLog, config.getRepository(), newWS, false);
         fail("Exception should be thrown");
      }
      catch (Exception e)
      {
      }

      newWS = helper.createWorkspaceEntry(true, null);
      backup.restore(bchLog, config.getRepository(), newWS, false);

      checkConent(repository, newWS.getName());
   }

   public void testRepositoryFullBackupRestore() throws Exception
   {
      // prepare
      ManageableRepository repository = helper.createRepository(container, true, null);
      addConent(repository, repository.getConfiguration().getSystemWorkspaceName());

      // backup
      File backDir = new File("target/backup/" + IdGenerator.generate());
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repository.getConfiguration().getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      RepositoryBackupChain bch = backup.startBackup(config);
      waitEndOfBackup(bch);
      backup.stopBackup(bch);

      // restore
      RepositoryEntry newRE =
         helper.createRepositoryEntry(true, repository.getConfiguration().getSystemWorkspaceName(), null);

      File backLog = new File(bch.getLogFilePath());
      assertTrue(backLog.exists());

      RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

      assertNotNull(bchLog.getStartedTime());
      assertNotNull(bchLog.getFinishedTime());

      backup.restore(bchLog, newRE, false);
      checkConent(repositoryService.getRepository(newRE.getName()), newRE.getSystemWorkspaceName());
   }

   public void testRepositoryFullAndIncrementalBackupRestore() throws Exception
   {
      // prepare
      ManageableRepository repository = helper.createRepository(container, true, null);
      addConent(repository, repository.getConfiguration().getSystemWorkspaceName());

      // backup
      File backDir = new File("target/backup/" + IdGenerator.generate());
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repository.getConfiguration().getName());
      config.setBackupType(BackupManager.FULL_AND_INCREMENTAL);
      config.setBackupDir(backDir);

      RepositoryBackupChain bch = backup.startBackup(config);
      waitEndOfBackup(bch);

      addIncrementalConent(repository, repository.getConfiguration().getSystemWorkspaceName());

      backup.stopBackup(bch);

      // restore
      RepositoryEntry newRE =
         helper.createRepositoryEntry(true, repository.getConfiguration().getSystemWorkspaceName(), null);

      File backLog = new File(bch.getLogFilePath());
      assertTrue(backLog.exists());

      RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

      assertNotNull(bchLog.getStartedTime());
      assertNotNull(bchLog.getFinishedTime());

      backup.restore(bchLog, newRE, false);
      checkConent(repositoryService.getRepository(newRE.getName()), newRE.getSystemWorkspaceName());
      checkIncrementalConent(repositoryService.getRepository(newRE.getName()), newRE.getSystemWorkspaceName());
   }

   public void testRepositoryFullBackupAsynchronusRestore() throws Exception
   {
      // prepare
      ManageableRepository repository = helper.createRepository(container, true, null);
      addConent(repository, repository.getConfiguration().getSystemWorkspaceName());

      // backup
      File backDir = new File("target/backup/" + IdGenerator.generate());
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repository.getConfiguration().getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      RepositoryBackupChain bch = backup.startBackup(config);
      waitEndOfBackup(bch);
      backup.stopBackup(bch);

      // restore
      RepositoryEntry newRE =
         helper.createRepositoryEntry(true, repository.getConfiguration().getSystemWorkspaceName(), null);

      File backLog = new File(bch.getLogFilePath());
      assertTrue(backLog.exists());

      RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

      assertNotNull(bchLog.getStartedTime());
      assertNotNull(bchLog.getFinishedTime());

      backup.restore(bchLog, newRE, true);
      waitEndOfRestore(newRE.getName());

      checkConent(repositoryService.getRepository(newRE.getName()), newRE.getSystemWorkspaceName());
   }

   public void testRepositoryFullBackupAsynchronusRestoreWorkspaceMapping() throws Exception
   {
      // prepare
      ManageableRepository repository = helper.createRepository(container, true, null);
      addConent(repository, repository.getConfiguration().getSystemWorkspaceName());

      // backup
      File backDir = new File("target/backup/" + IdGenerator.generate());
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repository.getConfiguration().getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      RepositoryBackupChain bch = backup.startBackup(config);
      waitEndOfBackup(bch);
      backup.stopBackup(bch);

      // restore
      RepositoryEntry newRE = helper.createRepositoryEntry(true, null, null);

      // create workspace mappingS
      Map<String, String> workspaceMapping = new HashedMap();
      workspaceMapping.put(repository.getConfiguration().getSystemWorkspaceName(), newRE.getSystemWorkspaceName());

      File backLog = new File(bch.getLogFilePath());
      assertTrue(backLog.exists());

      RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

      assertNotNull(bchLog.getStartedTime());
      assertNotNull(bchLog.getFinishedTime());

      backup.restore(bchLog, newRE, workspaceMapping, true);
      waitEndOfRestore(newRE.getName());

      checkConent(repositoryService.getRepository(newRE.getName()), newRE.getSystemWorkspaceName());
   }

   public void _testAutoStopRepositoryBackupIncrRepetion() throws Exception
   {
      // prepare
      ManageableRepository repository = helper.createRepository(container, true, null);
      addConent(repository, repository.getConfiguration().getSystemWorkspaceName());

      // backup
      File backDir = new File("target/backup/" + IdGenerator.generate());
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repository.getConfiguration().getName());
      config.setBackupType(BackupManager.FULL_AND_INCREMENTAL);
      config.setIncrementalJobPeriod(4);
      config.setIncrementalJobNumber(2);
      config.setBackupDir(backDir);

      RepositoryBackupChain bch = backup.startBackup(config);
      waitEndOfBackup(bch);
      backup.stopBackup(bch);

      // restore
      RepositoryEntry newRE = helper.createRepositoryEntry(true, null, null);

      // create workspace mappingS
      Map<String, String> workspaceMapping = new HashedMap();
      workspaceMapping.put(repository.getConfiguration().getSystemWorkspaceName(), newRE.getSystemWorkspaceName());

      File backLog = new File(bch.getLogFilePath());
      assertTrue(backLog.exists());

      RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

      assertNotNull(bchLog.getStartedTime());
      assertNotNull(bchLog.getFinishedTime());

      backup.restore(bchLog, newRE, workspaceMapping, true);
      waitEndOfRestore(newRE.getName());

      Thread.sleep(60000);
      assertEquals(backup.getCurrentRepositoryBackups().size(), 0);
   }

   public void testRepositoryRestoreFail() throws Exception
   {
      // prepare
      ManageableRepository repository = helper.createRepository(container, true, null);
      addConent(repository, repository.getConfiguration().getSystemWorkspaceName());

      // backup
      File backDir = new File("target/backup/" + IdGenerator.generate());
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repository.getConfiguration().getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      RepositoryBackupChain bch = backup.startBackup(config);
      waitEndOfBackup(bch);
      backup.stopBackup(bch);

      // restore
      RepositoryEntry newRE = helper.createRepositoryEntry(true, null, null);
      newRE.getWorkspaceEntries().get(0).getQueryHandler().setType("gg");

      // create workspace mappingS
      Map<String, String> workspaceMapping = new HashedMap();
      workspaceMapping.put(repository.getConfiguration().getSystemWorkspaceName(), newRE.getSystemWorkspaceName());

      File backLog = new File(bch.getLogFilePath());
      assertTrue(backLog.exists());

      RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

      assertNotNull(bchLog.getStartedTime());
      assertNotNull(bchLog.getFinishedTime());

      try
      {
         backup.restore(bchLog, newRE, workspaceMapping, false);
         fail("Exception should be thrown");
      }
      catch (RepositoryRestoreExeption e)
      {
      }
   }

   public void testExistedWorkspaceRestoreMultiDB() throws Exception
   {
      // prepare
      ManageableRepository repository = helper.createRepository(container, true, null);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(true, null);
      helper.addWorkspace(repository, wsEntry);
      addConent(repository, wsEntry.getName());

      // backup
      File backDir = new File("target/backup/" + IdGenerator.generate());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getConfiguration().getName());
      config.setWorkspace(wsEntry.getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      BackupChain bch = backup.startBackup(config);
      waitEndOfBackup(bch);
      backup.stopBackup(bch);

      // restore
      File backLog = new File(bch.getLogFilePath());
      assertTrue(backLog.exists());

      BackupChainLog bchLog = new BackupChainLog(backLog);

      assertNotNull(bchLog.getStartedTime());
      assertNotNull(bchLog.getFinishedTime());

      backup.restoreExistingWorkspace(bchLog, repository.getConfiguration().getName(), repository.getConfiguration()
         .getWorkspaceEntries().get(1), false);
      checkConent(repository, repository.getConfiguration().getWorkspaceEntries().get(1).getName());
   }

   public void testExistedWorkspaceRestoreSingleDB() throws Exception
   {
      // prepare
      String dsName = helper.createDatasource();

      ManageableRepository repository = helper.createRepository(container, false, dsName);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(false, dsName);
      helper.addWorkspace(repository, wsEntry);
      addConent(repository, wsEntry.getName());

      // backup
      File backDir = new File("target/backup/" + IdGenerator.generate());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getConfiguration().getName());
      config.setWorkspace(wsEntry.getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      BackupChain bch = backup.startBackup(config);
      waitEndOfBackup(bch);
      backup.stopBackup(bch);

      // restore
      File backLog = new File(bch.getLogFilePath());
      assertTrue(backLog.exists());

      BackupChainLog bchLog = new BackupChainLog(backLog);

      assertNotNull(bchLog.getStartedTime());
      assertNotNull(bchLog.getFinishedTime());

      backup.restoreExistingWorkspace(bchLog, repository.getConfiguration().getName(), repository.getConfiguration()
         .getWorkspaceEntries().get(1), false);
      checkConent(repository, repository.getConfiguration().getWorkspaceEntries().get(1).getName());
   }

   public void testExistedWorkspaceRestoreAsync() throws Exception
   {
      // prepare
      ManageableRepository repository = helper.createRepository(container, true, null);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(true, null);
      helper.addWorkspace(repository, wsEntry);
      addConent(repository, wsEntry.getName());

      // backup
      File backDir = new File("target/backup/" + IdGenerator.generate());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getConfiguration().getName());
      config.setWorkspace(wsEntry.getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      BackupChain bch = backup.startBackup(config);
      waitEndOfBackup(bch);
      backup.stopBackup(bch);

      // restore
      File backLog = new File(bch.getLogFilePath());
      assertTrue(backLog.exists());

      BackupChainLog bchLog = new BackupChainLog(backLog);

      assertNotNull(bchLog.getStartedTime());
      assertNotNull(bchLog.getFinishedTime());

      backup.restoreExistingWorkspace(bchLog, repository.getConfiguration().getName(), repository.getConfiguration()
         .getWorkspaceEntries().get(1), true);
      waitEndOfRestore(repository.getConfiguration().getName(), repository.getConfiguration().getWorkspaceEntries()
         .get(1).getName());

      assertEquals(
         backup.getLastRestore(repository.getConfiguration().getName(),
            repository.getConfiguration().getWorkspaceEntries().get(1).getName()).getStateRestore(),
         JobWorkspaceRestore.RESTORE_SUCCESSFUL);

      checkConent(repository, repository.getConfiguration().getWorkspaceEntries().get(1).getName());
   }

   public void testExistedRepositoryRestoreMultiDB() throws Exception
   {
      // prepare
      String dsName = helper.createDatasource();
      ManageableRepository repository = helper.createRepository(container, true, dsName);
      addConent(repository, repository.getConfiguration().getSystemWorkspaceName());

      // backup
      File backDir = new File("target/backup/" + IdGenerator.generate());
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repository.getConfiguration().getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      RepositoryBackupChain bch = backup.startBackup(config);
      waitEndOfBackup(bch);
      backup.stopBackup(bch);


      File backLog = new File(bch.getLogFilePath());
      assertTrue(backLog.exists());

      RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

      assertNotNull(bchLog.getStartedTime());
      assertNotNull(bchLog.getFinishedTime());

      // restore
      RepositoryEntry newRE =
         helper.createRepositoryEntry(true, repository.getConfiguration().getSystemWorkspaceName(), dsName);
      newRE.setName(repository.getConfiguration().getName());

      backup.restoreExistingRepository(bchLog, newRE, false);
      checkConent(repositoryService.getRepository(newRE.getName()), newRE.getSystemWorkspaceName());
   }

   public void testExistedRepositoryRestoreSingelDB() throws Exception
   {
      // prepare
      String dsName = helper.createDatasource();
      ManageableRepository repository = helper.createRepository(container, false, dsName);
      addConent(repository, repository.getConfiguration().getSystemWorkspaceName());

      // backup
      File backDir = new File("target/backup/" + IdGenerator.generate());
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repository.getConfiguration().getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      RepositoryBackupChain bch = backup.startBackup(config);
      waitEndOfBackup(bch);
      backup.stopBackup(bch);


      File backLog = new File(bch.getLogFilePath());
      assertTrue(backLog.exists());

      RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

      assertNotNull(bchLog.getStartedTime());
      assertNotNull(bchLog.getFinishedTime());

      // restore
      RepositoryEntry newRE =
         helper.createRepositoryEntry(false, repository.getConfiguration().getSystemWorkspaceName(), dsName);
      newRE.setName(repository.getConfiguration().getName());

      backup.restoreExistingRepository(bchLog, newRE, false);
      checkConent(repositoryService.getRepository(newRE.getName()), newRE.getSystemWorkspaceName());
   }

   public void testExistedRepositoryRestoreAsync() throws Exception
   {
      // prepare
      String dsName = helper.createDatasource();
      ManageableRepository repository = helper.createRepository(container, false, dsName);
      addConent(repository, repository.getConfiguration().getSystemWorkspaceName());

      // backup
      File backDir = new File("target/backup/" + IdGenerator.generate());
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repository.getConfiguration().getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      RepositoryBackupChain bch = backup.startBackup(config);
      waitEndOfBackup(bch);
      backup.stopBackup(bch);

      File backLog = new File(bch.getLogFilePath());
      assertTrue(backLog.exists());

      RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

      assertNotNull(bchLog.getStartedTime());
      assertNotNull(bchLog.getFinishedTime());

      // restore
      RepositoryEntry newRE =
         helper.createRepositoryEntry(false, repository.getConfiguration().getSystemWorkspaceName(), dsName);
      newRE.setName(repository.getConfiguration().getName());

      backup.restoreExistingRepository(bchLog, newRE, true);

      waitEndOfRestore(repository.getConfiguration().getName());
      
      assertEquals(JobRepositoryRestore.REPOSITORY_RESTORE_SUCCESSFUL,
         backup.getLastRepositoryRestore(repository.getConfiguration().getName()).getStateRestore());

      checkConent(repositoryService.getRepository(newRE.getName()), newRE.getSystemWorkspaceName());
   }


   public void testExistedWorkspaceRestoreWithConfig() throws Exception
   {
      // prepare
      ManageableRepository repository = helper.createRepository(container, true, null);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(true, null);
      helper.addWorkspace(repository, wsEntry);
      addConent(repository, wsEntry.getName());

      // backup
      File backDir = new File("target/backup/" + IdGenerator.generate());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getConfiguration().getName());
      config.setWorkspace(wsEntry.getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      BackupChain bch = backup.startBackup(config);
      waitEndOfBackup(bch);
      backup.stopBackup(bch);

      // restore
      File backLog = new File(bch.getLogFilePath());
      assertTrue(backLog.exists());

      BackupChainLog bchLog = new BackupChainLog(backLog);

      assertNotNull(bchLog.getStartedTime());
      assertNotNull(bchLog.getFinishedTime());

      backup.restoreExistingWorkspace(bchLog.getBackupId(), false);
      checkConent(repository, repository.getConfiguration().getWorkspaceEntries().get(1).getName());
   }

   public void testExistedRepositoryRestoreWithConfig() throws Exception
   {
      // prepare
      String dsName = helper.createDatasource();
      ManageableRepository repository = helper.createRepository(container, true, dsName);
      addConent(repository, repository.getConfiguration().getSystemWorkspaceName());

      // backup
      File backDir = new File("target/backup/" + IdGenerator.generate());
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repository.getConfiguration().getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      RepositoryBackupChain bch = backup.startBackup(config);
      waitEndOfBackup(bch);
      backup.stopBackup(bch);

      File backLog = new File(bch.getLogFilePath());
      assertTrue(backLog.exists());

      RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

      assertNotNull(bchLog.getStartedTime());
      assertNotNull(bchLog.getFinishedTime());

      // restore
      RepositoryEntry newRE =
         helper.createRepositoryEntry(true, repository.getConfiguration().getSystemWorkspaceName(), dsName);
      newRE.setName(repository.getConfiguration().getName());

      backup.restoreExistingRepository(bchLog.getBackupId(), false);
      checkConent(repositoryService.getRepository(newRE.getName()), newRE.getSystemWorkspaceName());
   }

   public void testWorkspaceRestoreWithConfig() throws Exception
   {
      // prepare
      ManageableRepository repository = helper.createRepository(container, true, null);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(true, null);
      helper.addWorkspace(repository, wsEntry);
      addConent(repository, wsEntry.getName());

      // backup
      File backDir = new File("target/backup/" + IdGenerator.generate());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getConfiguration().getName());
      config.setWorkspace(wsEntry.getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      BackupChain bch = backup.startBackup(config);
      waitEndOfBackup(bch);
      backup.stopBackup(bch);

      removeWorkspaceFully(repository.getConfiguration().getName(), wsEntry.getName());

      // restore
      File backLog = new File(bch.getLogFilePath());
      assertTrue(backLog.exists());

      BackupChainLog bchLog = new BackupChainLog(backLog);

      assertNotNull(bchLog.getStartedTime());
      assertNotNull(bchLog.getFinishedTime());

      backup.restoreWorkspace(bchLog.getBackupId(), false);
      checkConent(repository, repository.getConfiguration().getWorkspaceEntries().get(1).getName());
   }

   public void testRepositoryRestoreWithConfig() throws Exception
   {
      // prepare
      ManageableRepository repository = helper.createRepository(container, true, null);
      addConent(repository, repository.getConfiguration().getSystemWorkspaceName());

      // backup
      File backDir = new File("target/backup/" + IdGenerator.generate());
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repository.getConfiguration().getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      RepositoryBackupChain bch = backup.startBackup(config);
      waitEndOfBackup(bch);
      backup.stopBackup(bch);

      // restore
      removeRepositoryFully(repository.getConfiguration().getName());

      File backLog = new File(bch.getLogFilePath());
      assertTrue(backLog.exists());

      RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

      assertNotNull(bchLog.getStartedTime());
      assertNotNull(bchLog.getFinishedTime());

      backup.restoreRepository(bchLog.getBackupId(), false);

      checkConent(repositoryService.getRepository(config.getRepository()),
         repositoryService.getRepository(config.getRepository()).getConfiguration().getSystemWorkspaceName());
   }

   public void testRepositoryRestoreWithNullConfig() throws Exception
   {
      // prepare
      ManageableRepository repository = helper.createRepository(container, true, null);
      addConent(repository, repository.getConfiguration().getSystemWorkspaceName());

      // backup
      File backDir = new File("target/backup/" + IdGenerator.generate());
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repository.getConfiguration().getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      RepositoryBackupChain bch = backup.startBackup(config);
      waitEndOfBackup(bch);
      backup.stopBackup(bch);

      // restore
      removeRepositoryFully(repository.getConfiguration().getName());

      File backLog = new File(bch.getLogFilePath());
      assertTrue(backLog.exists());

      RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

      assertNotNull(bchLog.getStartedTime());
      assertNotNull(bchLog.getFinishedTime());

      backup.restore(bchLog, null, false);
      checkConent(repositoryService.getRepository(config.getRepository()),
         repositoryService.getRepository(config.getRepository()).getConfiguration().getSystemWorkspaceName());
   }
   
   public void testWorkspaceRestoreWithNullConfig() throws Exception
   {
      // prepare
      ManageableRepository repository = helper.createRepository(container, true, null);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(true, null);
      helper.addWorkspace(repository, wsEntry);
      addConent(repository, wsEntry.getName());

      // backup
      File backDir = new File("target/backup/" + IdGenerator.generate());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getConfiguration().getName());
      config.setWorkspace(wsEntry.getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      BackupChain bch = backup.startBackup(config);
      waitEndOfBackup(bch);
      backup.stopBackup(bch);

      removeWorkspaceFully(repository.getConfiguration().getName(), wsEntry.getName());

      // restore
      File backLog = new File(bch.getLogFilePath());
      assertTrue(backLog.exists());

      BackupChainLog bchLog = new BackupChainLog(backLog);

      assertNotNull(bchLog.getStartedTime());
      assertNotNull(bchLog.getFinishedTime());

      try
      {
         backup.restore(bchLog, repository.getConfiguration().getName() + "not_exists", null, false);
         fail("Exception should be thrown");
      }
      catch (Exception e)
      {
      }

      backup.restore(bchLog, repository.getConfiguration().getName(), null, false);
      checkConent(repository, repository.getConfiguration().getWorkspaceEntries().get(1).getName());
   }

   public void testExistedWorkspaceRestoreWithConfigBackupSetDir() throws Exception
   {
      // prepare
      ManageableRepository repository = helper.createRepository(container, true, null);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(true, null);
      helper.addWorkspace(repository, wsEntry);
      addConent(repository, wsEntry.getName());

      // backup
      File backDir = new File("target/backup/" + IdGenerator.generate());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getConfiguration().getName());
      config.setWorkspace(wsEntry.getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      BackupChain bch = backup.startBackup(config);
      waitEndOfBackup(bch);
      backup.stopBackup(bch);

      // restore
      File backLog = new File(bch.getLogFilePath());
      assertTrue(backLog.exists());

      BackupChainLog bchLog = new BackupChainLog(backLog);

      assertNotNull(bchLog.getStartedTime());
      assertNotNull(bchLog.getFinishedTime());

      backup.restoreExistingWorkspace(bchLog.getBackupConfig().getBackupDir(), false);
      checkConent(repository, repository.getConfiguration().getWorkspaceEntries().get(1).getName());
   }
   
   public void testExistedRepositoryRestoreWithConfigBackupSetDir() throws Exception
   {
      // prepare
      ManageableRepository repository = helper.createRepository(container, true, null);
      addConent(repository, repository.getConfiguration().getSystemWorkspaceName());

      // backup
      File backDir = new File("target/backup/" + IdGenerator.generate());
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repository.getConfiguration().getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      RepositoryBackupChain bch = backup.startBackup(config);
      waitEndOfBackup(bch);
      backup.stopBackup(bch);

      File backLog = new File(bch.getLogFilePath());
      assertTrue(backLog.exists());

      RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

      assertNotNull(bchLog.getStartedTime());
      assertNotNull(bchLog.getFinishedTime());

      backup.restoreExistingRepository(bchLog.getBackupConfig().getBackupDir(), false);
      checkConent(repositoryService.getRepository(config.getRepository()),
         repositoryService.getRepository(config.getRepository()).getConfiguration().getSystemWorkspaceName());
   }

   public void testWorkspaceRestoreWithConfigBackupSetDir() throws Exception
   {
      // prepare
      ManageableRepository repository = helper.createRepository(container, true, null);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(true, null);
      helper.addWorkspace(repository, wsEntry);
      addConent(repository, wsEntry.getName());

      // backup
      File backDir = new File("target/backup/" + IdGenerator.generate());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getConfiguration().getName());
      config.setWorkspace(wsEntry.getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      BackupChain bch = backup.startBackup(config);
      waitEndOfBackup(bch);
      backup.stopBackup(bch);

      removeWorkspaceFully(repository.getConfiguration().getName(), wsEntry.getName());
      
      // restore
      File backLog = new File(bch.getLogFilePath());
      assertTrue(backLog.exists());

      BackupChainLog bchLog = new BackupChainLog(backLog);

      assertNotNull(bchLog.getStartedTime());
      assertNotNull(bchLog.getFinishedTime());

      backup.restoreWorkspace(bchLog.getBackupConfig().getBackupDir(), false);
      checkConent(repository, repository.getConfiguration().getWorkspaceEntries().get(1).getName());
   }

   public void testRepositoryRestoreWithConfigBackupSetDir() throws Exception
   {
      // prepare
      ManageableRepository repository = helper.createRepository(container, true, null);
      addConent(repository, repository.getConfiguration().getSystemWorkspaceName());

      // backup
      File backDir = new File("target/backup/" + IdGenerator.generate());
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repository.getConfiguration().getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      RepositoryBackupChain bch = backup.startBackup(config);
      waitEndOfBackup(bch);
      backup.stopBackup(bch);

      // restore
      removeRepositoryFully(repository.getConfiguration().getName());

      File backLog = new File(bch.getLogFilePath());
      assertTrue(backLog.exists());

      RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

      assertNotNull(bchLog.getStartedTime());
      assertNotNull(bchLog.getFinishedTime());

      backup.restoreRepository(bchLog.getBackupConfig().getBackupDir(), false);
      checkConent(repositoryService.getRepository(config.getRepository()),
         repositoryService.getRepository(config.getRepository()).getConfiguration().getSystemWorkspaceName());
   }
}
