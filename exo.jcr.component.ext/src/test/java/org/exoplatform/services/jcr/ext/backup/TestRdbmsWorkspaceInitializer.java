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

import org.exoplatform.services.jcr.config.SimpleParameterEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.config.WorkspaceInitializerEntry;
import org.exoplatform.services.jcr.ext.backup.impl.rdbms.FullBackupJob;
import org.exoplatform.services.jcr.ext.backup.impl.rdbms.RdbmsWorkspaceInitializer;
import org.exoplatform.services.jcr.impl.core.SysViewWorkspaceInitializer;
import org.exoplatform.services.jcr.impl.core.TesterRdbmsWorkspaceInitializer;
import org.exoplatform.services.jcr.impl.core.query.SystemSearchManager;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.exoplatform.services.jcr.impl.util.io.FileCleanerHolder;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.jcr.util.TesterConfigurationHelper;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * @author <a href="mailto:anatoliy.bazko@gmail.com">Anatoliy Bazko</a>
 * @version $Id: TestFullBackupJob.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class TestRdbmsWorkspaceInitializer extends BaseRDBMSBackupTest
{
   TesterConfigurationHelper helper = TesterConfigurationHelper.getInstence();

   public void testRDBMSInitializerRestoreTablesMultiDB() throws Exception
   {
      FullBackupJob job = new FullBackupJob();
      BackupConfig config = new BackupConfig();
      config.setRepository("db1");
      config.setWorkspace("ws1");
      config.setBackupDir(new File("target/backup/testJob/testRDBMSInitializerRestoreTablesMultiDB"));

      Calendar calendar = Calendar.getInstance();

      job.init(repositoryService.getRepository("db1"), "ws1", config, calendar);
      job.run();

      URL url = job.getStorageURL();

      for (WorkspaceEntry workspaceEntry : repositoryService.getRepository("db1").getConfiguration()
         .getWorkspaceEntries())
      {
         if (workspaceEntry.getName().equals("ws1"))
         {
            String newValueStoragePath = "target/temp/values/" + IdGenerator.generate();
            String newIndexPath = "target/temp/index/" + IdGenerator.generate();

            String dsName = helper.getNewDataSource("");

            // set the initializer
            WorkspaceEntry newEntry =
               helper.getNewWs("ws" + System.currentTimeMillis(), true, dsName, newValueStoragePath, newIndexPath,
                  workspaceEntry.getContainer(), workspaceEntry.getContainer().getValueStorages());

            WorkspaceInitializerEntry wiEntry = new WorkspaceInitializerEntry();
            wiEntry.setType(RdbmsWorkspaceInitializer.class.getCanonicalName());

            List<SimpleParameterEntry> wieParams = new ArrayList<SimpleParameterEntry>();
            wieParams.add(new SimpleParameterEntry(SysViewWorkspaceInitializer.RESTORE_PATH_PARAMETER, new File(url
               .getFile()).getParent()));

            wiEntry.setParameters(wieParams);

            newEntry.setInitializer(wiEntry);

            repositoryService.getRepository("db1").configWorkspace(newEntry);
            repositoryService.getRepository("db1").createWorkspace(newEntry.getName());

            dsName = helper.getNewDataSource("");

            newValueStoragePath = "target/temp/values/" + IdGenerator.generate();
            newIndexPath = "target/temp/index/" + IdGenerator.generate();

            // set the initializer
            newEntry =
               helper.getNewWs("ws" + System.currentTimeMillis(), true, dsName, newValueStoragePath, newIndexPath,
                  workspaceEntry.getContainer(),
                  workspaceEntry.getContainer().getValueStorages());

            wiEntry = new WorkspaceInitializerEntry();
            wiEntry.setType(RdbmsWorkspaceInitializer.class.getCanonicalName());

            wieParams = new ArrayList<SimpleParameterEntry>();
            wieParams.add(new SimpleParameterEntry(SysViewWorkspaceInitializer.RESTORE_PATH_PARAMETER, new File(url
               .getFile()).getParent()));

            wiEntry.setParameters(wieParams);

            newEntry.setInitializer(wiEntry);

            repositoryService.getRepository("db1").configWorkspace(newEntry);
            repositoryService.getRepository("db1").createWorkspace(newEntry.getName());

            assertFalse(new File(newValueStoragePath).exists());
            assertTrue(new File(newIndexPath).list().length > 0);
            assertFalse(new File(newIndexPath + "_" + SystemSearchManager.INDEX_DIR_SUFFIX).exists());
         }

         break;
      }
   }

   public void testRDBMSInitializerRestoreTablesSingleDB() throws Exception
   {
      FullBackupJob job = new FullBackupJob();
      BackupConfig config = new BackupConfig();
      config.setRepository("db3");
      config.setWorkspace("ws");
      config.setBackupDir(new File("target/backup/testJob/testRDBMSInitializerRestoreTablesSingleDB"));

      Calendar calendar = Calendar.getInstance();

      job.init(repositoryService.getRepository("db7"), "ws1", config, calendar);
      job.run();

      URL url = job.getStorageURL();

      for (WorkspaceEntry workspaceEntry : repositoryService.getRepository("db7").getConfiguration()
         .getWorkspaceEntries())
      {
         if (workspaceEntry.getName().equals("ws1"))
         {
            String newValueStoragePath = "target/temp/values/" + IdGenerator.generate();
            String newIndexPath = "target/temp/index/" + IdGenerator.generate();

            String dsName = helper.getNewDataSource("");

            // set the initializer
            WorkspaceEntry newEntry =
               helper.getNewWs("ws" + System.currentTimeMillis(), true, dsName, newValueStoragePath, newIndexPath,
                  workspaceEntry.getContainer(), workspaceEntry.getContainer().getValueStorages());

            WorkspaceInitializerEntry wiEntry = new WorkspaceInitializerEntry();
            wiEntry.setType(RdbmsWorkspaceInitializer.class.getCanonicalName());

            List<SimpleParameterEntry> wieParams = new ArrayList<SimpleParameterEntry>();
            wieParams.add(new SimpleParameterEntry(SysViewWorkspaceInitializer.RESTORE_PATH_PARAMETER, new File(url
               .getFile()).getParent()));

            wiEntry.setParameters(wieParams);

            newEntry.setInitializer(wiEntry);

            TesterRdbmsWorkspaceInitializer initializer =
               new TesterRdbmsWorkspaceInitializer(newEntry, repositoryService.getRepository("db3").getConfiguration(),
                  cacheableDataManager, null, null, null, (ValueFactoryImpl)valueFactory, null, repositoryService, new FileCleanerHolder());

            // restore single -> multi
            repositoryService.getRepository("db1").configWorkspace(newEntry);
            repositoryService.getRepository("db1").createWorkspace(newEntry.getName());

            dsName = helper.getNewDataSource("");

            newValueStoragePath = "target/temp/values/" + IdGenerator.generate();
            newIndexPath = "target/temp/index/" + IdGenerator.generate();

            // set the initializer
            newEntry =
               helper.getNewWs("ws" + System.currentTimeMillis(), true, dsName, newValueStoragePath, newIndexPath,
                  workspaceEntry.getContainer(), workspaceEntry.getContainer().getValueStorages());

            wiEntry = new WorkspaceInitializerEntry();
            wiEntry.setType(RdbmsWorkspaceInitializer.class.getCanonicalName());

            wieParams = new ArrayList<SimpleParameterEntry>();
            wieParams.add(new SimpleParameterEntry(SysViewWorkspaceInitializer.RESTORE_PATH_PARAMETER, new File(url
               .getFile()).getParent()));

            wiEntry.setParameters(wieParams);

            newEntry.setInitializer(wiEntry);

            repositoryService.getRepository("db1").configWorkspace(newEntry);
            repositoryService.getRepository("db1").createWorkspace(newEntry.getName());

            assertFalse(new File(newValueStoragePath).exists());
            assertTrue(new File(newIndexPath).list().length > 0);
            assertFalse(new File(newIndexPath + "_" + SystemSearchManager.INDEX_DIR_SUFFIX).exists());
         }

         break;
      }
   }
}
