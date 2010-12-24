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
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.naming.InitialContext;
import javax.sql.DataSource;

/**
 * @author <a href="mailto:anatoliy.bazko@gmail.com">Anatoliy Bazko</a>
 * @version $Id: TestFullBackupJob.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class TestRdbmsWorkspaceInitializer extends BaseRDBMSBackupTest
{
   TesterConfigurationHelper helper = TesterConfigurationHelper.getInstence();

   public void testRDBMSInitializerSystemWorkspace() throws Exception
   {
      FullBackupJob job = new FullBackupJob();
      BackupConfig config = new BackupConfig();
      config.setRepository("db1");
      config.setWorkspace("ws");
      config.setBackupDir(new File("target/backup/testJob/testRDBMSInitializerSystemWorkspace"));

      Calendar calendar = Calendar.getInstance();

      job.init(repositoryService.getRepository("db1"), "ws", config, calendar);
      job.run();

      URL url = job.getStorageURL();

      for (WorkspaceEntry workspaceEntry : repositoryService.getRepository("db1").getConfiguration()
         .getWorkspaceEntries())
      {
         if (workspaceEntry.getName().equals("ws"))
         {
            String newValueStoragePath = "target/temp/values/" + IdGenerator.generate();
            String newIndexPath = "target/temp/index/" + IdGenerator.generate();

            // set the initializer
            WorkspaceEntry newEntry =
               helper.getNewWs("ws", true, null, newValueStoragePath, newIndexPath, workspaceEntry.getContainer(),
                  workspaceEntry.getContainer().getValueStorages());

            WorkspaceInitializerEntry wiEntry = new WorkspaceInitializerEntry();
            wiEntry.setType(RdbmsWorkspaceInitializer.class.getCanonicalName());

            List<SimpleParameterEntry> wieParams = new ArrayList<SimpleParameterEntry>();
            wieParams.add(new SimpleParameterEntry(SysViewWorkspaceInitializer.RESTORE_PATH_PARAMETER, new File(url
               .getFile()).getParent()));

            wiEntry.setParameters(wieParams);

            newEntry.setInitializer(wiEntry);

            TesterRdbmsWorkspaceInitializer initializer =
               new TesterRdbmsWorkspaceInitializer(newEntry, repositoryService.getRepository("db1").getConfiguration(),
                  cacheableDataManager, null, null, null, (ValueFactoryImpl)valueFactory, null, repositoryService, new FileCleanerHolder());

            initializer.restoreValueFiles();
            assertTrue(new File(newValueStoragePath).list().length > 0);

            initializer.restoreIndexFiles();
            assertTrue(new File(newIndexPath).list().length > 0);
            assertTrue(new File(newIndexPath + "_" + SystemSearchManager.INDEX_DIR_SUFFIX).exists());
            assertTrue(new File(newIndexPath + "_" + SystemSearchManager.INDEX_DIR_SUFFIX).list().length > 0);
         }
      }
   }

   public void testRDBMSInitializer() throws Exception
   {
      FullBackupJob job = new FullBackupJob();
      BackupConfig config = new BackupConfig();
      config.setRepository("db1");
      config.setWorkspace("ws1");
      config.setBackupDir(new File("target/backup/testJob/testRDBMSInitializer"));

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

            // set the initializer
            WorkspaceEntry newEntry =
               helper.getNewWs("ws1", true, null, newValueStoragePath, newIndexPath, workspaceEntry.getContainer(),
                  workspaceEntry.getContainer().getValueStorages());

            WorkspaceInitializerEntry wiEntry = new WorkspaceInitializerEntry();
            wiEntry.setType(RdbmsWorkspaceInitializer.class.getCanonicalName());

            List<SimpleParameterEntry> wieParams = new ArrayList<SimpleParameterEntry>();
            wieParams.add(new SimpleParameterEntry(SysViewWorkspaceInitializer.RESTORE_PATH_PARAMETER, new File(url
               .getFile()).getParent()));

            wiEntry.setParameters(wieParams);

            newEntry.setInitializer(wiEntry);

            TesterRdbmsWorkspaceInitializer initializer =
               new TesterRdbmsWorkspaceInitializer(newEntry, repositoryService.getRepository("db1").getConfiguration(),
 cacheableDataManager, null, null, null,
                              (ValueFactoryImpl) valueFactory, null, repositoryService, new FileCleanerHolder());

            initializer.restoreValueFiles();
            assertFalse(new File(newValueStoragePath).exists());

            initializer.restoreIndexFiles();
            assertTrue(new File(newIndexPath).list().length > 0);
            assertFalse(new File(newIndexPath + "_" + SystemSearchManager.INDEX_DIR_SUFFIX).exists());
         }
      }
   }

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
            DataSource ds = (DataSource)new InitialContext().lookup(dsName);

            Connection conn = ds.getConnection();
            Statement st = conn.createStatement();
            st.execute("CREATE TABLE JCR_MITEM(ID VARCHAR(96) NOT NULL,PARENT_ID VARCHAR(96) NOT NULL,NAME VARCHAR(512) NOT NULL,VERSION INTEGER NOT NULL,I_CLASS INTEGER NOT NULL,I_INDEX INTEGER NOT NULL,N_ORDER_NUM INTEGER,P_TYPE INTEGER,P_MULTIVALUED  BOOLEAN,CONSTRAINT JCR_PK_MITEM PRIMARY KEY(ID))");
            conn.commit();

            st.execute("INSERT INTO JCR_MITEM VALUES(' ',' ','__root_parent',0,0,0,0,NULL,NULL)");
            conn.commit();

            st.execute("CREATE TABLE JCR_MVALUE(ID BIGINT generated by default as identity (START WITH 2, INCREMENT BY 1) NOT NULL, DATA VARBINARY(65535),ORDER_NUM INTEGER NOT NULL,PROPERTY_ID VARCHAR(96) NOT NULL,STORAGE_DESC VARCHAR(512),CONSTRAINT JCR_PK_MVALUE PRIMARY KEY(ID),CONSTRAINT JCR_FK_MVALUE_PROPERTY FOREIGN KEY(PROPERTY_ID) REFERENCES JCR_MITEM(ID))");
            conn.commit();

            st.execute("CREATE TABLE JCR_MREF(NODE_ID VARCHAR(96) NOT NULL, PROPERTY_ID VARCHAR(96) NOT NULL, ORDER_NUM INTEGER NOT NULL, CONSTRAINT JCR_PK_MREF PRIMARY KEY(NODE_ID, PROPERTY_ID, ORDER_NUM))");
            conn.commit();

            // set the initializer
            WorkspaceEntry newEntry =
               helper.getNewWs("ws1", true, dsName, newValueStoragePath, newIndexPath, workspaceEntry.getContainer(),
                  workspaceEntry.getContainer().getValueStorages());

            WorkspaceInitializerEntry wiEntry = new WorkspaceInitializerEntry();
            wiEntry.setType(RdbmsWorkspaceInitializer.class.getCanonicalName());

            List<SimpleParameterEntry> wieParams = new ArrayList<SimpleParameterEntry>();
            wieParams.add(new SimpleParameterEntry(SysViewWorkspaceInitializer.RESTORE_PATH_PARAMETER, new File(url
               .getFile()).getParent()));

            wiEntry.setParameters(wieParams);

            newEntry.setInitializer(wiEntry);

            TesterRdbmsWorkspaceInitializer initializer =
               new TesterRdbmsWorkspaceInitializer(newEntry, repositoryService.getRepository("db1").getConfiguration(),
 cacheableDataManager, null, null, null,
                              (ValueFactoryImpl) valueFactory, null, repositoryService, new FileCleanerHolder());

            // restore multi -> multi
            initializer.restoreTables(conn, 0, true, workspaceEntry.getLockManager(), url.getFile());
            initializer.restoreTables(conn, 1, true, workspaceEntry.getLockManager(), url.getFile());
            initializer.restoreTables(conn, 2, true, workspaceEntry.getLockManager(), url.getFile());

            st.execute("ALTER TABLE JCR_MITEM ADD CONSTRAINT JCR_FK_MITEM_PARENT FOREIGN KEY(PARENT_ID) REFERENCES JCR_MITEM(ID)");
            conn.commit();

            dsName = helper.getNewDataSource("");
            ds = (DataSource)new InitialContext().lookup(dsName);

            conn = ds.getConnection();
            st = conn.createStatement();
            st.execute("CREATE TABLE JCR_SITEM(ID VARCHAR(96) NOT NULL,PARENT_ID VARCHAR(96) NOT NULL,NAME VARCHAR(512) NOT NULL,VERSION INTEGER NOT NULL,CONTAINER_NAME VARCHAR(96) NOT NULL,I_CLASS INTEGER NOT NULL,I_INDEX INTEGER NOT NULL,N_ORDER_NUM INTEGER,P_TYPE INTEGER,P_MULTIVALUED  BOOLEAN,CONSTRAINT JCR_PK_SITEM PRIMARY KEY(ID))");
            conn.commit();

            st.execute("INSERT INTO JCR_SITEM VALUES(' ',' ','__root_parent',0,'__root_parent_container',0,0,0,NULL,NULL)");
            conn.commit();

            st.execute("CREATE TABLE JCR_SVALUE(ID BIGINT generated by default as identity (START WITH 2, INCREMENT BY 1) NOT NULL, DATA VARBINARY(65535),ORDER_NUM INTEGER NOT NULL,PROPERTY_ID VARCHAR(96) NOT NULL,STORAGE_DESC VARCHAR(512),CONSTRAINT JCR_PK_MVALUE PRIMARY KEY(ID),CONSTRAINT JCR_FK_SVALUE_PROPERTY FOREIGN KEY(PROPERTY_ID) REFERENCES JCR_SITEM(ID))");
            conn.commit();

            st.execute("CREATE TABLE JCR_SREF(NODE_ID VARCHAR(96) NOT NULL, PROPERTY_ID VARCHAR(96) NOT NULL, ORDER_NUM INTEGER NOT NULL, CONSTRAINT JCR_PK_SREF PRIMARY KEY(NODE_ID, PROPERTY_ID, ORDER_NUM))");
            conn.commit();

            // set the initializer
            newEntry =
               helper.getNewWs("ws1", true, dsName, newValueStoragePath, newIndexPath, workspaceEntry.getContainer(),
                  workspaceEntry.getContainer().getValueStorages());

            wiEntry = new WorkspaceInitializerEntry();
            wiEntry.setType(RdbmsWorkspaceInitializer.class.getCanonicalName());

            wieParams = new ArrayList<SimpleParameterEntry>();
            wieParams.add(new SimpleParameterEntry(SysViewWorkspaceInitializer.RESTORE_PATH_PARAMETER, new File(url
               .getFile()).getParent()));

            wiEntry.setParameters(wieParams);

            newEntry.setInitializer(wiEntry);

            initializer =
               new TesterRdbmsWorkspaceInitializer(newEntry, repositoryService.getRepository("db1").getConfiguration(),
                  cacheableDataManager, null, null, null, (ValueFactoryImpl)valueFactory, null, repositoryService, new FileCleanerHolder());

            // restore multi -> single
            initializer.restoreTables(conn, 0, false, workspaceEntry.getLockManager(), url.getFile());
            initializer.restoreTables(conn, 1, false, workspaceEntry.getLockManager(), url.getFile());
            initializer.restoreTables(conn, 2, false, workspaceEntry.getLockManager(), url.getFile());

            st.execute("ALTER TABLE JCR_SITEM ADD CONSTRAINT JCR_FK_SITEM_PARENT FOREIGN KEY(PARENT_ID) REFERENCES JCR_SITEM(ID)");
            conn.commit();

         }
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

      job.init(repositoryService.getRepository("db3"), "ws", config, calendar);
      job.run();

      URL url = job.getStorageURL();

      for (WorkspaceEntry workspaceEntry : repositoryService.getRepository("db3").getConfiguration()
         .getWorkspaceEntries())
      {
         if (workspaceEntry.getName().equals("ws"))
         {
            String newValueStoragePath = "target/temp/values/" + IdGenerator.generate();
            String newIndexPath = "target/temp/index/" + IdGenerator.generate();

            String dsName = helper.getNewDataSource("");
            DataSource ds = (DataSource)new InitialContext().lookup(dsName);

            Connection conn = ds.getConnection();
            Statement st = conn.createStatement();
            st.execute("CREATE TABLE JCR_MITEM(ID VARCHAR(96) NOT NULL,PARENT_ID VARCHAR(96) NOT NULL,NAME VARCHAR(512) NOT NULL,VERSION INTEGER NOT NULL,I_CLASS INTEGER NOT NULL,I_INDEX INTEGER NOT NULL,N_ORDER_NUM INTEGER,P_TYPE INTEGER,P_MULTIVALUED  BOOLEAN,CONSTRAINT JCR_PK_MITEM PRIMARY KEY(ID))");
            conn.commit();

            st.execute("INSERT INTO JCR_MITEM VALUES(' ',' ','__root_parent',0,0,0,0,NULL,NULL)");
            conn.commit();

            st.execute("CREATE TABLE JCR_MVALUE(ID BIGINT generated by default as identity (START WITH 2, INCREMENT BY 1) NOT NULL, DATA VARBINARY(65535),ORDER_NUM INTEGER NOT NULL,PROPERTY_ID VARCHAR(96) NOT NULL,STORAGE_DESC VARCHAR(512),CONSTRAINT JCR_PK_MVALUE PRIMARY KEY(ID),CONSTRAINT JCR_FK_MVALUE_PROPERTY FOREIGN KEY(PROPERTY_ID) REFERENCES JCR_MITEM(ID))");
            conn.commit();

            st.execute("CREATE TABLE JCR_MREF(NODE_ID VARCHAR(96) NOT NULL, PROPERTY_ID VARCHAR(96) NOT NULL, ORDER_NUM INTEGER NOT NULL, CONSTRAINT JCR_PK_MREF PRIMARY KEY(NODE_ID, PROPERTY_ID, ORDER_NUM))");
            conn.commit();

            // set the initializer
            WorkspaceEntry newEntry =
               helper.getNewWs("ws", true, dsName, newValueStoragePath, newIndexPath, workspaceEntry.getContainer(),
                  workspaceEntry.getContainer().getValueStorages());

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
            initializer.restoreTables(conn, 0, true, workspaceEntry.getLockManager(), url.getFile());
            initializer.restoreTables(conn, 1, true, workspaceEntry.getLockManager(), url.getFile());
            initializer.restoreTables(conn, 2, true, workspaceEntry.getLockManager(), url.getFile());

            st.execute("ALTER TABLE JCR_MITEM ADD CONSTRAINT JCR_FK_MITEM_PARENT FOREIGN KEY(PARENT_ID) REFERENCES JCR_MITEM(ID)");
            conn.commit();

            dsName = helper.getNewDataSource("");
            ds = (DataSource)new InitialContext().lookup(dsName);

            conn = ds.getConnection();
            st = conn.createStatement();
            st.execute("CREATE TABLE JCR_SITEM(ID VARCHAR(96) NOT NULL,PARENT_ID VARCHAR(96) NOT NULL,NAME VARCHAR(512) NOT NULL,VERSION INTEGER NOT NULL,CONTAINER_NAME VARCHAR(96) NOT NULL,I_CLASS INTEGER NOT NULL,I_INDEX INTEGER NOT NULL,N_ORDER_NUM INTEGER,P_TYPE INTEGER,P_MULTIVALUED  BOOLEAN,CONSTRAINT JCR_PK_SITEM PRIMARY KEY(ID))");
            conn.commit();

            st.execute("INSERT INTO JCR_SITEM VALUES(' ',' ','__root_parent',0,'__root_parent_container',0,0,0,NULL,NULL)");
            conn.commit();

            st.execute("CREATE TABLE JCR_SVALUE(ID BIGINT generated by default as identity (START WITH 2, INCREMENT BY 1) NOT NULL, DATA VARBINARY(65535),ORDER_NUM INTEGER NOT NULL,PROPERTY_ID VARCHAR(96) NOT NULL,STORAGE_DESC VARCHAR(512),CONSTRAINT JCR_PK_MVALUE PRIMARY KEY(ID),CONSTRAINT JCR_FK_SVALUE_PROPERTY FOREIGN KEY(PROPERTY_ID) REFERENCES JCR_SITEM(ID))");
            conn.commit();

            st.execute("CREATE TABLE JCR_SREF(NODE_ID VARCHAR(96) NOT NULL, PROPERTY_ID VARCHAR(96) NOT NULL, ORDER_NUM INTEGER NOT NULL, CONSTRAINT JCR_PK_SREF PRIMARY KEY(NODE_ID, PROPERTY_ID, ORDER_NUM))");
            conn.commit();

            // set the initializer
            newEntry =
               helper.getNewWs("ws", true, dsName, newValueStoragePath, newIndexPath, workspaceEntry.getContainer(),
                  workspaceEntry.getContainer().getValueStorages());

            wiEntry = new WorkspaceInitializerEntry();
            wiEntry.setType(RdbmsWorkspaceInitializer.class.getCanonicalName());

            wieParams = new ArrayList<SimpleParameterEntry>();
            wieParams.add(new SimpleParameterEntry(SysViewWorkspaceInitializer.RESTORE_PATH_PARAMETER, new File(url
               .getFile()).getParent()));

            wiEntry.setParameters(wieParams);

            newEntry.setInitializer(wiEntry);

            initializer =
               new TesterRdbmsWorkspaceInitializer(newEntry, repositoryService.getRepository("db3").getConfiguration(),
                  cacheableDataManager, null, null, null, (ValueFactoryImpl)valueFactory, null, repositoryService, new FileCleanerHolder());

            // restore single -> single
            initializer.restoreTables(conn, 0, false, workspaceEntry.getLockManager(), url.getFile());
            initializer.restoreTables(conn, 1, false, workspaceEntry.getLockManager(), url.getFile());
            initializer.restoreTables(conn, 2, false, workspaceEntry.getLockManager(), url.getFile());

            st.execute("ALTER TABLE JCR_SITEM ADD CONSTRAINT JCR_FK_SITEM_PARENT FOREIGN KEY(PARENT_ID) REFERENCES JCR_SITEM(ID)");
            conn.commit();
         }
      }
   }
}
