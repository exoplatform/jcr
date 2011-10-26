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
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.backup.BackupJob;
import org.exoplatform.services.jcr.ext.backup.BackupManager;
import org.exoplatform.services.jcr.ext.backup.ExtendedBackupManager;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupChain;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupChainLog;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupConfig;
import org.exoplatform.services.jcr.ext.backup.impl.BackupManagerImpl;
import org.exoplatform.services.jcr.ext.backup.impl.JobRepositoryRestore;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

import java.io.File;

import javax.jcr.Node;

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
      repositoryBackupRestore("db1");
   }

   public void testBackupRestoreExistingRepositoryMultiDB() throws Exception
   {
      repositoryBackupRestore("db2");
   }

   protected void repositoryBackupRestore(String repositoryName) throws Exception
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
      ManageableRepository repository = repositoryService.getRepository(repositoryName);
      String wsName = repository.getConfiguration().getSystemWorkspaceName();

      SessionImpl session = (SessionImpl)repository.login(credentials, wsName);
      try
      {
         Node rootNode = session.getRootNode().addNode("test");

         rootNode.addNode("node1").setProperty("prop1", "value1");
         session.save();
      }
      finally
      {
         session.logout();
      }
   }

   protected void checkConent(String repositoryName) throws Exception
   {
      ManageableRepository repository = repositoryService.getRepository(repositoryName);
      String wsName = repository.getConfiguration().getSystemWorkspaceName();

      SessionImpl session = (SessionImpl)repository.login(credentials, wsName);
      try
      {
         Node rootNode = session.getRootNode().getNode("test");
         assertEquals(rootNode.getNode("node1").getProperty("prop1").getString(), "value1");
      }
      finally
      {
         session.logout();
      }
   }
}
