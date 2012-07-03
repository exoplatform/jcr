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
import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.commons.utils.PrivilegedSystemHelper;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.SimpleParameterEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.backup.impl.JobRepositoryRestore;
import org.exoplatform.services.jcr.ext.backup.impl.JobWorkspaceRestore;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig.DatabaseStructureType;
import org.exoplatform.services.jcr.util.IdGenerator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

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
      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.MULTI, null);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(DatabaseStructureType.MULTI, null);
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
      WorkspaceEntry newWS = helper.createWorkspaceEntry(DatabaseStructureType.MULTI, null);

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
      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.MULTI, null);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(DatabaseStructureType.MULTI, null);
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
      WorkspaceEntry newWS = helper.createWorkspaceEntry(DatabaseStructureType.MULTI, null);

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
      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.MULTI, null);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(DatabaseStructureType.MULTI, null);
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
      WorkspaceEntry newWS = helper.createWorkspaceEntry(DatabaseStructureType.MULTI, null);

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
      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.MULTI, null);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(DatabaseStructureType.MULTI, null);
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
      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.MULTI, null);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(DatabaseStructureType.MULTI, null);
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
      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.MULTI, null);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(DatabaseStructureType.MULTI, null);
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
      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.MULTI, null);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(DatabaseStructureType.MULTI, null);
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
      WorkspaceEntry newWS = helper.createWorkspaceEntry(DatabaseStructureType.MULTI, "NOT_EXISTED_DS");

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

      newWS = helper.createWorkspaceEntry(DatabaseStructureType.MULTI, null);
      backup.restore(bchLog, config.getRepository(), newWS, false);

      checkConent(repository, newWS.getName());
   }

   public void testRepositoryFullBackupRestore() throws Exception
   {
      testRepositoryFullBackupRestoreWithSpecifiedDbTypes(DatabaseStructureType.MULTI, DatabaseStructureType.MULTI);
   }

   public void testRepositoryFullBackupRestoreSingleToMulti() throws Exception
   {
      testRepositoryFullBackupRestoreWithSpecifiedDbTypes(DatabaseStructureType.SINGLE, DatabaseStructureType.MULTI);
   }

   public void testRepositoryFullBackupRestoreSingleToIsolated() throws Exception
   {
      testRepositoryFullBackupRestoreWithSpecifiedDbTypes(DatabaseStructureType.SINGLE, DatabaseStructureType.ISOLATED);
   }

   public void testRepositoryFullBackupRestoreMultiToIsolated() throws Exception
   {
      testRepositoryFullBackupRestoreWithSpecifiedDbTypes(DatabaseStructureType.MULTI, DatabaseStructureType.ISOLATED);
   }

   public void testRepositoryFullBackupRestoreMultiToSingle() throws Exception
   {
      testRepositoryFullBackupRestoreWithSpecifiedDbTypes(DatabaseStructureType.MULTI, DatabaseStructureType.SINGLE);
   }

   public void testRepositoryFullBackupRestoreIsolatedToSingle() throws Exception
   {
      testRepositoryFullBackupRestoreWithSpecifiedDbTypes(DatabaseStructureType.ISOLATED, DatabaseStructureType.SINGLE);
   }

   public void testRepositoryFullBackupRestoreIsolatedToMulti() throws Exception
   {
      testRepositoryFullBackupRestoreWithSpecifiedDbTypes(DatabaseStructureType.ISOLATED, DatabaseStructureType.MULTI);
   }
   
   public void testRepositoryFullBackupRestoreBackupSingleToIsolatedOnIsolated() throws Exception
   {
      // prepare
      ManageableRepository repositorySourceBackup = helper.createRepository(container, DatabaseStructureType.SINGLE, null);
      addConent(repositorySourceBackup, repositorySourceBackup.getConfiguration().getSystemWorkspaceName());

      // backup
      File backDir = new File("target/backup/" + IdGenerator.generate());
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repositorySourceBackup.getConfiguration().getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      RepositoryBackupChain bch = backup.startBackup(config);
      waitEndOfBackup(bch);
      backup.stopBackup(bch);
      
      
      RepositoryEntry re = helper.createRepositoryEntry(DatabaseStructureType.ISOLATED, repositorySourceBackup.getWorkspaceNames()[0], null);
      repositoryService.createRepository(re);  
      ManageableRepository repository = repositoryService.getRepository(re.getName());
      addConent(repository, repository.getConfiguration().getSystemWorkspaceName());

      // restore
      RepositoryEntry newRE =
         helper.copyRepositoryEntry(repository.getConfiguration());

      File backLog = new File(bch.getLogFilePath());
      assertTrue(backLog.exists());

      RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

      assertNotNull(bchLog.getStartedTime());
      assertNotNull(bchLog.getFinishedTime());

      backup.restoreExistingRepository(bchLog, newRE, false);
      checkConent(repositoryService.getRepository(newRE.getName()), newRE.getSystemWorkspaceName());
   }

   private void testRepositoryFullBackupRestoreWithSpecifiedDbTypes(DatabaseStructureType srcDbStructureType,
      DatabaseStructureType dstDbStructureType) throws Exception
   {
      // prepare
      ManageableRepository repository = helper.createRepository(container, srcDbStructureType, null);
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
         helper.createRepositoryEntry(dstDbStructureType, repository.getConfiguration()
            .getSystemWorkspaceName(), null);

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
      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.MULTI, null);
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
         helper.createRepositoryEntry(DatabaseStructureType.MULTI, repository.getConfiguration().getSystemWorkspaceName(), null);

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
      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.MULTI, null);
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
         helper.createRepositoryEntry(DatabaseStructureType.MULTI, repository.getConfiguration().getSystemWorkspaceName(), null);

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
      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.MULTI, null);
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
      RepositoryEntry newRE = helper.createRepositoryEntry(DatabaseStructureType.MULTI, null, null);

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
      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.MULTI, null);
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
      RepositoryEntry newRE = helper.createRepositoryEntry(DatabaseStructureType.MULTI, null, null);

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
      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.MULTI, null);
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
      RepositoryEntry newRE = helper.createRepositoryEntry(DatabaseStructureType.MULTI, null, null);
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
      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.MULTI, null);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(DatabaseStructureType.MULTI, null);
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

      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.SINGLE, dsName);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(DatabaseStructureType.SINGLE, dsName);
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
      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.MULTI, null);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(DatabaseStructureType.MULTI, null);
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
      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.MULTI, dsName);
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
         helper.createRepositoryEntry(DatabaseStructureType.MULTI, repository.getConfiguration().getSystemWorkspaceName(), dsName);
      newRE.setName(repository.getConfiguration().getName());

      backup.restoreExistingRepository(bchLog, newRE, false);
      checkConent(repositoryService.getRepository(newRE.getName()), newRE.getSystemWorkspaceName());
   }

   public void testExistedRepositoryRestoreSingelDB() throws Exception
   {
      // prepare
      String dsName = helper.createDatasource();
      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.SINGLE, dsName);
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
         helper.createRepositoryEntry(DatabaseStructureType.SINGLE, repository.getConfiguration().getSystemWorkspaceName(), dsName);
      newRE.setName(repository.getConfiguration().getName());

      backup.restoreExistingRepository(bchLog, newRE, false);
      checkConent(repositoryService.getRepository(newRE.getName()), newRE.getSystemWorkspaceName());
   }

   public void testExistedRepositoryRestoreAsync() throws Exception
   {
      // prepare
      String dsName = helper.createDatasource();
      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.SINGLE, dsName);
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
         helper.createRepositoryEntry(DatabaseStructureType.SINGLE, repository.getConfiguration().getSystemWorkspaceName(), dsName);
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
      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.MULTI, null);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(DatabaseStructureType.MULTI, null);
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
      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.MULTI, dsName);
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
         helper.createRepositoryEntry(DatabaseStructureType.MULTI, repository.getConfiguration().getSystemWorkspaceName(), dsName);
      newRE.setName(repository.getConfiguration().getName());

      backup.restoreExistingRepository(bchLog.getBackupId(), false);
      checkConent(repositoryService.getRepository(newRE.getName()), newRE.getSystemWorkspaceName());
   }

   public void testWorkspaceRestoreWithConfig() throws Exception
   {
      // prepare
      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.MULTI, null);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(DatabaseStructureType.MULTI, null);
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
      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.MULTI, null);
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
      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.MULTI, null);
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
      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.MULTI, null);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(DatabaseStructureType.MULTI, null);
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
      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.MULTI, null);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(DatabaseStructureType.MULTI, null);
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
      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.MULTI, null);
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
      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.MULTI, null);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(DatabaseStructureType.MULTI, null);
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
      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.MULTI, null);
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
   
   public void testEnvironmentVariablesToBackupDir() throws Exception
   {
      // prepare stage #1
      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.MULTI, null);
      addConent(repository, repository.getConfiguration().getSystemWorkspaceName());

      String tempDir = PrivilegedSystemHelper.getProperty("java.io.tmpdir");

      // backup
      File backDir = new File(tempDir);

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repository.getConfiguration().getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      RepositoryBackupChain bch = backup.startBackup(config);
      waitEndOfBackup(bch);
      backup.stopBackup(bch);

      // prepare stage #2
      String repositoryBackupChainLogPath = bch.getLogFilePath();

      String backupDitEnv = backDir.getCanonicalPath();

      String relativePath = bch.getBackupConfig().getBackupDir().getCanonicalPath().replace(backupDitEnv, "");
      String relativePathForWrite = relativePath;
      if (File.separator.equals("\\"))
      {
         relativePathForWrite = relativePath.replaceAll("\\\\", "\\\\\\\\");
      }

      String newBackupDir = "\\${java.io.tmpdir}" + relativePathForWrite;

      File dest = new File(repositoryBackupChainLogPath + ".xml");
      dest.createNewFile();

      RepositoryBackupChainLog newRepositoryBackupChainLog = null;
      try
      {
         String sConfig =
            setNewBackupDirInRepositoryBackupChainLog(new File(repositoryBackupChainLogPath), dest, newBackupDir);

         assertTrue(sConfig.contains("${java.io.tmpdir}" + relativePath));

         // check
         newRepositoryBackupChainLog = new RepositoryBackupChainLog(dest);

         assertEquals(bch.getBackupConfig().getBackupDir().getCanonicalPath(),
                  newRepositoryBackupChainLog.getBackupConfig().getBackupDir().getCanonicalPath());

      }
      finally
      {
         newRepositoryBackupChainLog = null;
         dest.delete();
         
         deleteFolder(bch.getBackupConfig().getBackupDir());
      }
   }

   public void testRelativeBackupDir() throws Exception
   {
      // prepare stage #1
      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.MULTI, null);
      addConent(repository, repository.getConfiguration().getSystemWorkspaceName());

      // backup
      File backDir = new File("target");

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repository.getConfiguration().getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      RepositoryBackupChain bch = backup.startBackup(config);
      waitEndOfBackup(bch);
      backup.stopBackup(bch);

      // prepare stage #2
      String repositoryBackupChainLogPath = bch.getLogFilePath();

      String relativePrefixBackupDir = backDir.getCanonicalFile().getParent() + File.separator;

      String newBackupDir =
         bch.getBackupConfig().getBackupDir().getCanonicalPath().replace(relativePrefixBackupDir, "");
      String newBackupDirForWrite = newBackupDir;
      if (File.separator.equals("\\"))
      {
         newBackupDirForWrite = newBackupDir.replaceAll("\\\\", "\\\\\\\\");
      }

      File dest = new File(repositoryBackupChainLogPath + ".xml");
      dest.createNewFile();

      RepositoryBackupChainLog newRepositoryBackupChainLog = null;

      String sConfig =
         setNewBackupDirInRepositoryBackupChainLog(new File(repositoryBackupChainLogPath), dest, newBackupDirForWrite);

      assertTrue(sConfig.contains(newBackupDir));

      // check
      newRepositoryBackupChainLog = new RepositoryBackupChainLog(dest);

      assertEquals(bch.getBackupConfig().getBackupDir().getCanonicalPath(), newRepositoryBackupChainLog
               .getBackupConfig().getBackupDir().getCanonicalPath());
   }

   /**
     * Use JobExistingWorkspaceSameConfigRestore to restore.
     */
   
   
   
   
   
   public void testExistedWorkspaceRestoreSingleDBTwoWS() throws Exception
   {
   // prepare
   String dsName1 = helper.createDatasource();
   String dsName2 = helper.createDatasource();

   ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.SINGLE, dsName1);
   WorkspaceEntry wsEntry1 = helper.createWorkspaceEntry(DatabaseStructureType.SINGLE, dsName1);
   helper.addWorkspace(repository, wsEntry1);
   addConent(repository, wsEntry1.getName());

   WorkspaceEntry wsEntry2 = helper.createWorkspaceEntry(DatabaseStructureType.SINGLE, dsName2);
   helper.addWorkspace(repository, wsEntry2);
   addConent(repository, wsEntry2.getName());

   // backup
   File backDir = new File("target/backup/" + IdGenerator.generate());
   backDir.mkdirs();

   BackupConfig config = new BackupConfig();
   config.setRepository(repository.getConfiguration().getName());
   config.setWorkspace(wsEntry1.getName());
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
   checkConent(repository, repository.getConfiguration().getWorkspaceEntries().get(2).getName());
   }

   /**
     * Use JobExistingWorkspaceRestore to restore.
     */
   
   
   
   
   
   public void testExistedWorkspaceRestoreSingleDBTwoWSWithDiffConfig() throws Exception
   {
   // prepare
   String dsName1 = helper.createDatasource();
   String dsName2 = helper.createDatasource();

   ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.SINGLE, dsName1);
   WorkspaceEntry wsEntry1 = helper.createWorkspaceEntry(DatabaseStructureType.SINGLE, dsName1);
   helper.addWorkspace(repository, wsEntry1);
   addConent(repository, wsEntry1.getName());

   WorkspaceEntry wsEntry2 = helper.createWorkspaceEntry(DatabaseStructureType.SINGLE, dsName2);
   helper.addWorkspace(repository, wsEntry2);
   addConent(repository, wsEntry2.getName());

   // backup
   File backDir = new File("target/backup/" + IdGenerator.generate());
   backDir.mkdirs();

   BackupConfig config = new BackupConfig();
   config.setRepository(repository.getConfiguration().getName());
   config.setWorkspace(wsEntry1.getName());
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
   
   // change  cofig
   WorkspaceEntry wsEntry = helper.copyWorkspaceEntry(repository.getConfiguration().getWorkspaceEntries().get(1));

   List<SimpleParameterEntry> params = wsEntry.getContainer().getParameters();
   params.set(2, new SimpleParameterEntry("max-buffer-size", "307200"));

   wsEntry.getContainer().setParameters(params);

   backup.restoreExistingWorkspace(bchLog, repository.getConfiguration().getName(), wsEntry, false);
   checkConent(repository, repository.getConfiguration().getWorkspaceEntries().get(1).getName());
   checkConent(repository, repository.getConfiguration().getWorkspaceEntries().get(2).getName());
   }

   public void testExistedRepositoryRestoreSingelDBSameConfig() throws Exception
   {
      // prepare
      String dsName = helper.createDatasource();
      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.SINGLE, dsName);
      addConent(repository, repository.getConfiguration().getSystemWorkspaceName());

      WorkspaceEntry wsEntry1 = helper.createWorkspaceEntry(DatabaseStructureType.SINGLE, dsName);
      helper.addWorkspace(repository, wsEntry1);
      addConent(repository, wsEntry1.getName());

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
      backup.restoreExistingRepository(bchLog, helper.copyRepositoryEntry(repository.getConfiguration()), false);

      checkConent(repositoryService.getRepository(repository.getConfiguration().getName()), repository
               .getConfiguration().getSystemWorkspaceName());
   }
   
   public void testCreateRepositoryAfterFailRestoreOnSystemWS() throws Exception
   {
      // create repository
      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);
      String dsName = helper.createDatasource();
      RepositoryEntry repoEntry = helper.createRepositoryEntry(DatabaseStructureType.SINGLE, null, dsName);
      service.createRepository(repoEntry);

      ManageableRepository repository = service.getRepository(repoEntry.getName());

      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(DatabaseStructureType.SINGLE, dsName);
      helper.addWorkspace(repository, wsEntry);
      service.getConfig().retain();

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

      // copy repository entry and set new ata source 
      RepositoryEntry newRepoEntry = helper.copyRepositoryEntry(repoEntry);
      String newDSName = helper.createDatasource();
      for (WorkspaceEntry ws : newRepoEntry.getWorkspaceEntries())
      {
         for (int i = 0; i < ws.getContainer().getParameters().size(); i++)
         {
            SimpleParameterEntry spe = ws.getContainer().getParameters().get(i);

            if (spe.getName().equals("source-name"))
            {
               ws.getContainer().getParameters().set(i, new SimpleParameterEntry(spe.getName(), newDSName));
               break;
            }
         }
      }

      // create repository entry with name of data source name is wrong in system workspace 
      RepositoryEntry wrongRepoEntry = helper.copyRepositoryEntry(repoEntry);
      for (WorkspaceEntry ws : wrongRepoEntry.getWorkspaceEntries())
      {
         if (repository.getConfiguration().getSystemWorkspaceName().equals(ws.getName()))
         {
            for (int i = 0; i < ws.getContainer().getParameters().size(); i++)
            {
               SimpleParameterEntry spe = ws.getContainer().getParameters().get(i);

               if (spe.getName().equals("source-name"))
               {
                  ws.getContainer().getParameters().set(i,
                     new SimpleParameterEntry(spe.getName(), spe.getValue() + "_wrong"));
                  break;
               }
            }
            break;
         }
      }

      // remove existed repository
      removeRepositoryFully(repository.getConfiguration().getName());

      // wrong restore
      try
      {
         backup.restore(new RepositoryBackupChainLog(backLog), wrongRepoEntry, false);
         fail();
      }
      catch (Exception e)
      {
         //ok
      }

      // restore
      backup.restore(new RepositoryBackupChainLog(backLog), newRepoEntry, false);

      checkConent(repositoryService.getRepository(newRepoEntry.getName()), newRepoEntry.getSystemWorkspaceName());
   }

   public void testCreateRepositoryAfterFailRestoreOnNonSystemWS() throws Exception
   {
      // create repository
      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);
      String dsName = helper.createDatasource();
      RepositoryEntry repoEntry = helper.createRepositoryEntry(DatabaseStructureType.SINGLE, null, dsName);
      service.createRepository(repoEntry);

      ManageableRepository repository = service.getRepository(repoEntry.getName());

      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(DatabaseStructureType.SINGLE, dsName);
      helper.addWorkspace(repository, wsEntry);
      service.getConfig().retain();

      addConent(repository, wsEntry.getName());

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

      // copy repository entry and set new ata source 
      RepositoryEntry newRepoEntry = helper.copyRepositoryEntry(repoEntry);
      String newDSName = helper.createDatasource();
      for (WorkspaceEntry ws : newRepoEntry.getWorkspaceEntries())
      {
         for (int i = 0; i < ws.getContainer().getParameters().size(); i++)
         {
            SimpleParameterEntry spe = ws.getContainer().getParameters().get(i);

            if (spe.getName().equals("source-name"))
            {
               ws.getContainer().getParameters().set(i, new SimpleParameterEntry(spe.getName(), newDSName));
               break;
            }
         }
      }

      // create repository entry with name of data source name is wrong in second workspace 
      RepositoryEntry wrongRepoEntry = helper.copyRepositoryEntry(repoEntry);
      for (WorkspaceEntry ws : wrongRepoEntry.getWorkspaceEntries())
      {
         if (wsEntry.getName().equals(ws.getName()))
         {
            for (int i = 0; i < ws.getContainer().getParameters().size(); i++)
            {
               SimpleParameterEntry spe = ws.getContainer().getParameters().get(i);

               if (spe.getName().equals("source-name"))
               {
                  ws.getContainer().getParameters().set(i,
                     new SimpleParameterEntry(spe.getName(), spe.getValue() + "_wrong"));
                  break;
               }
            }
            break;
         }
      }

      // remove existed repository
      removeRepositoryFully(repository.getConfiguration().getName());

      // wrong restore
      try
      {
         backup.restore(new RepositoryBackupChainLog(backLog), wrongRepoEntry, false);
         fail();
      }
      catch (Exception e)
      {
         //ok
      }

      // restore
      backup.restore(new RepositoryBackupChainLog(backLog), newRepoEntry, false);

      checkConent(repositoryService.getRepository(newRepoEntry.getName()), wsEntry.getName());
   }

   public void testCreateRepositoryAfterFailRestoreWithFailConfiguration() throws Exception
   {
      // create repository
      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);
      String dsName = helper.createDatasource();
      RepositoryEntry repoEntry = helper.createRepositoryEntry(DatabaseStructureType.SINGLE, null, dsName);
      service.createRepository(repoEntry);

      ManageableRepository repository = service.getRepository(repoEntry.getName());

      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(DatabaseStructureType.SINGLE, dsName);
      helper.addWorkspace(repository, wsEntry);
      service.getConfig().retain();

      addConent(repository, wsEntry.getName());

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

      // copy repository entry and set new data source 
      RepositoryEntry newRepoEntry = helper.copyRepositoryEntry(repoEntry);
      String newDSName = helper.createDatasource();
      for (WorkspaceEntry ws : newRepoEntry.getWorkspaceEntries())
      {
         for (int i = 0; i < ws.getContainer().getParameters().size(); i++)
         {
            SimpleParameterEntry spe = ws.getContainer().getParameters().get(i);

            if (spe.getName().equals("source-name"))
            {
               ws.getContainer().getParameters().set(i, new SimpleParameterEntry(spe.getName(), newDSName));
               break;
            }
         }
      }

      // create repository entry with name of data source name is wrong in second workspace 
      RepositoryEntry wrongRepoEntry = helper.copyRepositoryEntry(repoEntry);
      for (WorkspaceEntry ws : wrongRepoEntry.getWorkspaceEntries())
      {
         if (wsEntry.getName().equals(ws.getName()))
         {
            ws.getContainer().setType("wrong parameter");
         }
      }

      // remove existed repository
      removeRepositoryFully(repository.getConfiguration().getName());

      // wrong restore
      try
      {
         backup.restore(new RepositoryBackupChainLog(backLog), wrongRepoEntry, false);
         fail();
      }
      catch (Exception e)
      {
         //ok  
      }

      // restore
      backup.restore(new RepositoryBackupChainLog(backLog), newRepoEntry, false);

      checkConent(repositoryService.getRepository(newRepoEntry.getName()), wsEntry.getName());
   }
   
   public void testRepositoryFullBackupWithQueryInBackgroudOneThread() throws Exception
   {
      // prepare
      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.MULTI, null);
      addConent(repository, repository.getConfiguration().getSystemWorkspaceName());
      
      SessionImpl session = (SessionImpl)repository.login(credentials, repository.getConfiguration().getSystemWorkspaceName());
      final Query q = session.getWorkspace().getQueryManager().createQuery(
                  "select * from nt:unstructured where jcr:path like '/test/%'", Query.SQL);

      QueryResult res = q.execute();
      assertEquals("Wrong nodes count in result set", 3, res.getNodes().getSize());
      
      ThreadQueryExecuter tQueryExecuter = new ThreadQueryExecuter(q);
      tQueryExecuter.start();
      
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

      tQueryExecuter.close();
      assertNull("Can not execute query.", tQueryExecuter.getException());
   }
      
   class ThreadQueryExecuter extends Thread
   {
      private AtomicBoolean closed = new AtomicBoolean(false);
      
      private Exception exception;

      private Query query;

      public ThreadQueryExecuter(Query q)
      {
         setName("QueryExecuter" + getName());
         this.query = q;
      }

      public Exception getException()
      {
         return exception;
      }
      
      public void close()
      {
         closed.set(true);
      }

      @Override
      public void run()
      {
         while (!closed.get())
         {
            try
            {
               QueryResult result = query.execute();
               result.getNodes().getSize();
            }
            catch (Exception re)
            {
               log.error("Can not execute query.", re);
               exception = re;
            }
         }
      }
   }
   
   public void testPullJobRepositoryRestore() throws Exception
   {
      // prepare
      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.MULTI, null);
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
      
      assertNotNull(backup.getLastRepositoryRestore(config.getRepository()));

      assertNotNull(backup.pullJobRepositoryRestore(config.getRepository()));

      assertNull(backup.getLastRepositoryRestore(config.getRepository()));
   }
   
   
   /**
    * Set new backup directory in RepositoryBackupChainLog
    * 
    * @param src
    *          source file of RepositoryBackupChainLog
    * @param dest
    *          destination file of RepositoryBackupChainLog
    * @param newBackupDir
    * @return  String
    *            the content of file destination  
    * @throws IOException 
    */
   protected String setNewBackupDirInRepositoryBackupChainLog(File src, File dest, String newBackupDir)
            throws IOException
   {
      InputStream in = PrivilegedFileHelper.fileInputStream(src);
      OutputStream out = PrivilegedFileHelper.fileOutputStream(dest);

      byte[] buf = new byte[(int) (PrivilegedFileHelper.length(src))];
      in.read(buf);

      String sConfig = new String(buf, Constants.DEFAULT_ENCODING);
      sConfig = sConfig.replaceAll("<backup-dir>.+</backup-dir>", "<backup-dir>" + newBackupDir + "</backup-dir>");

      out.write(sConfig.getBytes(Constants.DEFAULT_ENCODING));

      in.close();
      out.close();

      return sConfig;
   }

   protected void deleteFolder(File f)
   {
      if (f.isDirectory())
      {
         for (File file : f.listFiles())
         {
            deleteFolder(file);
         }

         f.delete();
      }
      else
      {
         f.delete();
      }
   }
   
}
