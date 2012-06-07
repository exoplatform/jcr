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
import org.exoplatform.services.jcr.impl.core.query.SystemSearchManager;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig.DatabaseStructureType;
import org.exoplatform.services.jcr.util.TesterConfigurationHelper;
import org.exoplatform.services.jcr.util.TesterRdbmsWorkspaceInitializer;

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
   TesterConfigurationHelper helper = TesterConfigurationHelper.getInstance();

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
            String dsName = helper.createDatasource();
            WorkspaceEntry newEntry =
               helper.createWorkspaceEntry(DatabaseStructureType.MULTI, dsName,
                  helper.getValueStorageIds(workspaceEntry.getContainer().getValueStorages()));

            List<SimpleParameterEntry> wieParams = new ArrayList<SimpleParameterEntry>();
            wieParams.add(new SimpleParameterEntry(SysViewWorkspaceInitializer.RESTORE_PATH_PARAMETER, new File(url
               .getFile()).getParent()));

            WorkspaceInitializerEntry wiEntry = new WorkspaceInitializerEntry();
            wiEntry.setType(RdbmsWorkspaceInitializer.class.getCanonicalName());
            wiEntry.setParameters(wieParams);
            newEntry.setInitializer(wiEntry);

            // restore
            helper.addWorkspace(repositoryService.getRepository("db1"), newEntry);

            dsName = helper.createDatasource();
            newEntry =
               helper.createWorkspaceEntry(DatabaseStructureType.MULTI, dsName,
                  helper.getValueStorageIds(workspaceEntry.getContainer().getValueStorages()));

            wieParams = new ArrayList<SimpleParameterEntry>();
            wieParams.add(new SimpleParameterEntry(SysViewWorkspaceInitializer.RESTORE_PATH_PARAMETER, new File(url
               .getFile()).getParent()));

            wiEntry = new WorkspaceInitializerEntry();
            wiEntry.setType(RdbmsWorkspaceInitializer.class.getCanonicalName());
            wiEntry.setParameters(wieParams);
            newEntry.setInitializer(wiEntry);

            // restore
            helper.addWorkspace(repositoryService.getRepository("db1"), newEntry);

            String newIndexPath = newEntry.getQueryHandler().getParameterValue("index-dir");
            String newValueStoragePath = newEntry.getContainer().getValueStorages().get(0).getParameterValue("path");

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

      job.init(repositoryService.getRepository("db3"), "ws1", config, calendar);
      job.run();

      URL url = job.getStorageURL();

      for (WorkspaceEntry workspaceEntry : repositoryService.getRepository("db3").getConfiguration()
         .getWorkspaceEntries())
      {
         if (workspaceEntry.getName().equals("ws1"))
         {
            String dsName = helper.createDatasource();
            WorkspaceEntry newEntry =
               helper.createWorkspaceEntry(DatabaseStructureType.MULTI, dsName,
                  helper.getValueStorageIds(workspaceEntry.getContainer().getValueStorages()));

            List<SimpleParameterEntry> wieParams = new ArrayList<SimpleParameterEntry>();
            wieParams.add(new SimpleParameterEntry(SysViewWorkspaceInitializer.RESTORE_PATH_PARAMETER, new File(url
               .getFile()).getParent()));

            WorkspaceInitializerEntry wiEntry = new WorkspaceInitializerEntry();
            wiEntry.setType(RdbmsWorkspaceInitializer.class.getCanonicalName());
            wiEntry.setParameters(wieParams);
            newEntry.setInitializer(wiEntry);

            TesterRdbmsWorkspaceInitializer initializer =
               new TesterRdbmsWorkspaceInitializer(newEntry, repositoryService.getRepository("db3").getConfiguration(),
                  cacheableDataManager, null, null, null, (ValueFactoryImpl)valueFactory, null, repositoryService);

            // restore single -> multi
            helper.addWorkspace(repositoryService.getRepository("db1"), newEntry);

            dsName = helper.createDatasource();
            newEntry =
               helper.createWorkspaceEntry(DatabaseStructureType.MULTI, dsName,
                  helper.getValueStorageIds(workspaceEntry.getContainer().getValueStorages()));

            wieParams = new ArrayList<SimpleParameterEntry>();
            wieParams.add(new SimpleParameterEntry(SysViewWorkspaceInitializer.RESTORE_PATH_PARAMETER, new File(url
               .getFile()).getParent()));

            wiEntry = new WorkspaceInitializerEntry();
            wiEntry.setType(RdbmsWorkspaceInitializer.class.getCanonicalName());
            wiEntry.setParameters(wieParams);
            newEntry.setInitializer(wiEntry);

            helper.addWorkspace(repositoryService.getRepository("db1"), newEntry);

            String newIndexPath = newEntry.getQueryHandler().getParameterValue("index-dir");
            String newValueStoragePath = newEntry.getContainer().getValueStorages().get(0).getParameterValue("path");

            assertFalse(new File(newValueStoragePath).exists());
            assertTrue(new File(newIndexPath).list().length > 0);
            assertFalse(new File(newIndexPath + "_" + SystemSearchManager.INDEX_DIR_SUFFIX).exists());
         }

         break;
      }
   }
}
