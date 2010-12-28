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
package org.exoplatform.services.jcr.ext.backup.load;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.jcr.config.ContainerEntry;
import org.exoplatform.services.jcr.config.QueryHandlerEntry;
import org.exoplatform.services.jcr.config.QueryHandlerParams;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.SimpleParameterEntry;
import org.exoplatform.services.jcr.config.ValueStorageEntry;
import org.exoplatform.services.jcr.config.ValueStorageFilterEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.ext.BaseStandaloneTest;
import org.exoplatform.services.jcr.ext.backup.BackupChain;
import org.exoplatform.services.jcr.ext.backup.BackupChainLog;
import org.exoplatform.services.jcr.ext.backup.BackupConfig;
import org.exoplatform.services.jcr.ext.backup.BackupJob;
import org.exoplatform.services.jcr.ext.backup.BackupManager;
import org.exoplatform.services.jcr.ext.backup.ExtendedBackupManager;
import org.exoplatform.services.jcr.ext.backup.impl.BackupManagerImpl;
import org.exoplatform.services.jcr.ext.backup.impl.JobWorkspaceRestore;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.PropertyImpl;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
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
public class TestLoadBackup extends BaseStandaloneTest
{
   protected final String REPOSITORY_NAME = "db7";

   protected final String WORKSPACE_NAME = "ws1";

   protected final int WRITER_COUNT = 100;

   protected final String FULL_BACKUP_TYPE = "org.exoplatform.services.jcr.ext.backup.impl.rdbms.FullBackupJob";

   protected final int BACKUP_TYPE = BackupManager.FULL_BACKUP_ONLY;

   /**
    * Writer.
    */
   class TreeWriterThread extends Thread
   {
      private Session session;

      private final String nodeName;
      
      public TreeWriterThread(Session session, String nodeName) throws RepositoryException
      {
         this.session = session;
         this.nodeName = nodeName;
      }

      @Override
      public void run()
      {
         try
         {
            while (true)
            {
               Node rootChild = session.getRootNode().addNode(nodeName + System.currentTimeMillis());
               session.save();

               addChilds(session, rootChild, 0);
            }
         }
         catch (Exception e)
         {
            e.printStackTrace();
         }
      }
   }

   /**
    * Test Backup/Restore.
    * 
    * @throws Exception
    */
   public void testBackupRestore() throws Exception
   {
      BackupManagerImpl backupManagerImpl = (BackupManagerImpl)getBackupManager();

      List<TreeWriterThread> threads = new ArrayList<TreeWriterThread>();
      List<Session> sessions = new ArrayList<Session>();

      //writers
      for (int i = 0; i < WRITER_COUNT; i++)
      {
         Session writerSession = repositoryService.getRepository(REPOSITORY_NAME).login(credentials, WORKSPACE_NAME);
         TreeWriterThread writer = new TreeWriterThread(writerSession, "subnode" + i);
         writer.start();
         threads.add(writer);
         sessions.add(writerSession);
      }

      Thread.sleep(10 * 1000);

      System.out.println(" ============ BACKUP START ============");

      // backup
      File backDir = new File("target/backup/ws1");
      backDir.mkdirs();
      BackupChain bch = null;

      backupManagerImpl.start();

      BackupConfig config = new BackupConfig();
      config.setRepository(REPOSITORY_NAME);
      config.setWorkspace(WORKSPACE_NAME);
      config.setBackupType(BACKUP_TYPE);
      config.setBackupDir(backDir);

      backupManagerImpl.startBackup(config);

      bch = backupManagerImpl.findBackup(REPOSITORY_NAME, WORKSPACE_NAME);

      // wait till full backup will be stopped
      while (bch.getFullBackupState() != BackupJob.FINISHED)
      {
         Thread.yield();
         Thread.sleep(30);
      }

      System.out.println(" ============ FULL BACKUP FINISHED ============");

      if (BACKUP_TYPE == BackupManager.FULL_AND_INCREMENTAL)
      {
         Thread.sleep(5 * 1000);
      }

      //      for (Thread thread : threads)
      //      {
      //         thread.interrupt();
      //      }

      if (BACKUP_TYPE == BackupManager.FULL_AND_INCREMENTAL)
      {
         Thread.sleep(5 * 1000);
      }

      // stop backup
      if (bch != null)
      {
         backupManagerImpl.stopBackup(bch);
      }
      else
      {
         fail("Can't get fullBackup chain");
      }
      Thread.sleep(5 * 1000);

      System.out.println(" ============ BACKUP FINISHED ============");

      // restore
      WorkspaceEntry ws1back = makeWorkspaceEntry("ws1back", "jdbcjcr3");

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         BackupChainLog bchLog = new BackupChainLog(backLog);

         System.out.println(" ============ RESTORE START ============");

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backupManagerImpl.restore(bchLog, REPOSITORY_NAME, ws1back, false);

         JobWorkspaceRestore restore = backupManagerImpl.getLastRestore(REPOSITORY_NAME, ws1back.getName());
         if (restore != null)
         {
            while (restore.getStateRestore() == JobWorkspaceRestore.RESTORE_FAIL
               || restore.getStateRestore() == JobWorkspaceRestore.RESTORE_SUCCESSFUL)
            {
               Thread.sleep(1000);
            }

            if (restore.getStateRestore() == JobWorkspaceRestore.RESTORE_FAIL)
            {
               restore.getRestoreException().printStackTrace();
               fail(restore.getRestoreException().getMessage());
            }
         }
      }
      else
      {
         fail("There are no backup files in " + backDir.getAbsolutePath());
      }

      System.out.println(" ============ CHECKING INTEGRITY ============");

      checkIntegrity((NodeImpl)repositoryService.getRepository(REPOSITORY_NAME).login(credentials, "ws1back")
         .getRootNode());
   }

   protected void addChilds(Session session, Node root, int layer) throws Exception
   {
      if (layer == 4)
      {
         return;
      }

      for (int i = 0; i < 10; i++)
      {
         Node n = root.addNode("testNode" + i);
         n.addMixin("mix:referenceable");
         n.setProperty("long", i);
         n.setProperty("ref", n);
         n.setProperty("string", new String[]{"test" + System.currentTimeMillis()});
         n.setProperty("stream", new FileInputStream(createBLOBTempFile(1 * 1024)));

         session.save();

         //         System.out.println("Child has been added");

         Thread.sleep(50);

         addChilds(session, n, layer + 1);
      }
   }

   /**
    * {@inheritDoc}
    */
   protected ExtendedBackupManager getBackupManager()
   {
      InitParams initParams = new InitParams();
      PropertiesParam pps = new PropertiesParam();
      pps.setProperty(BackupManagerImpl.FULL_BACKUP_TYPE, FULL_BACKUP_TYPE);
      pps.setProperty(BackupManagerImpl.INCREMENTAL_BACKUP_TYPE,
         "org.exoplatform.services.jcr.ext.backup.impl.fs.IncrementalBackupJob");
      pps.setProperty(BackupManagerImpl.BACKUP_DIR, "target/backup");
      pps.setProperty(BackupManagerImpl.DEFAULT_INCREMENTAL_JOB_PERIOD, "3600");

      initParams.put(BackupManagerImpl.BACKUP_PROPERTIES, pps);

      return new BackupManagerImpl(initParams, repositoryService);
   }

   /**
    * Check integrity. Get all values from all properties.
    * 
    * @param node
    * @throws RepositoryException
    */
   protected void checkIntegrity(NodeImpl node) throws RepositoryException
   {
      Iterator<NodeImpl> iterator = node.getNodes();
      while (iterator.hasNext())
      {
         NodeImpl newNode = iterator.next();
         if (newNode.isNodeType(Constants.MIX_VERSIONABLE)
            && !newNode.getData().getQPath().isDescendantOf(Constants.JCR_VERSION_STORAGE_PATH))
         {
            VersionHistory verHist = newNode.getVersionHistory();

            NodeImpl verNode = null;
            try
            {
               verNode = (NodeImpl)verHist.getVersion("1");
               checkIntegrity(verNode);
            }
            catch (VersionException e)
            {
            }
         }

         checkIntegrity(newNode);

         Iterator<PropertyImpl> propIt = newNode.getProperties();
         while (propIt.hasNext())
         {
            PropertyImpl prop = propIt.next();
            if (prop.isMultiValued())
            {
               for (Value value : prop.getValues())
               {
                  value.toString();
               }
            }
            else
            {
               prop.getValue().toString();
            }
         }
      }
   }
   
   public void _testTableLock() throws Exception
   {
      Session writerSession = repositoryService.getRepository(REPOSITORY_NAME).login(credentials, WORKSPACE_NAME);
      //      TreeWriterThread writer = new TreeWriterThread(writerSession, "subnode");
      //      writer.start();

      DataSource ds = (DataSource)new InitialContext().lookup("jdbcjcr_to_repository_restore_singel_db");
      Connection conn = ds.getConnection();

      Thread.sleep(2 * 1000);

      Statement st = conn.createStatement();
      st.executeQuery("SET PROPERTY \"readonly\" TRUE");
      System.out.println("LOCK TABLES JCR_SITEM WRITE");
      Thread.sleep(5 * 1000);

      //      st.executeQuery("SET READONLY FALSE");
      //      System.out.println("UNLOCK TABLES");
      //      Thread.sleep(5 * 1000);

      st.close();
      conn.close();
      System.out.println("Con closed");
      Thread.sleep(5 * 1000);

      addChilds(writerSession, writerSession.getRootNode(), 0);
   }

   protected WorkspaceEntry makeWorkspaceEntry(String name, String sourceName) throws LoginException,
      NoSuchWorkspaceException, RepositoryException, RepositoryConfigurationException
   {
      SessionImpl session =
         (SessionImpl)repositoryService.getRepository(REPOSITORY_NAME).login(credentials, WORKSPACE_NAME);

      WorkspaceEntry ws1e = (WorkspaceEntry)session.getContainer().getComponentInstanceOfType(WorkspaceEntry.class);

      WorkspaceEntry ws1back = new WorkspaceEntry();
      ws1back.setName(name);
      // RepositoryContainer rcontainer = (RepositoryContainer)
      // container.getComponentInstanceOfType(RepositoryContainer.class);
      ws1back.setUniqueName(((RepositoryImpl)session.getRepository()).getName() + "_" + ws1back.getName()); // EXOMAN

      ws1back.setAccessManager(ws1e.getAccessManager());
      ws1back.setCache(ws1e.getCache());
      //      ws1back.setContainer(ws1e.getContainer());
      ws1back.setLockManager(ws1e.getLockManager());
      ws1back.setInitializer(ws1e.getInitializer());

      // Indexer
      ArrayList qParams = new ArrayList();
      // qParams.add(new SimpleParameterEntry("indexDir", "target" + File.separator+ "temp" +
      // File.separator +"index" + name));
      qParams.add(new SimpleParameterEntry(QueryHandlerParams.PARAM_INDEX_DIR, "target/temp/index/" + name
         + System.currentTimeMillis()));
      QueryHandlerEntry qEntry =
         new QueryHandlerEntry("org.exoplatform.services.jcr.impl.core.query.lucene.SearchIndex", qParams);

      ws1back.setQueryHandler(qEntry); // EXOMAN

      ArrayList params = new ArrayList();
      for (Iterator i = ws1e.getContainer().getParameters().iterator(); i.hasNext();)
      {
         SimpleParameterEntry p = (SimpleParameterEntry)i.next();
         SimpleParameterEntry newp = new SimpleParameterEntry(p.getName(), p.getValue());

         if (newp.getName().equals("source-name") && sourceName != null)
            newp.setValue(sourceName);
         else if (newp.getName().equals("swap-directory"))
            newp.setValue("target/temp/swap/" + name + System.currentTimeMillis());

         params.add(newp);
      }

      ContainerEntry ce =
         new ContainerEntry("org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer", params);

      ArrayList<ValueStorageEntry> list = new ArrayList<ValueStorageEntry>();

      // value storage
      ArrayList<ValueStorageFilterEntry> vsparams = new ArrayList<ValueStorageFilterEntry>();
      ValueStorageFilterEntry filterEntry = new ValueStorageFilterEntry();
      filterEntry.setPropertyType("Binary");
      vsparams.add(filterEntry);

      ValueStorageEntry valueStorageEntry =
         new ValueStorageEntry("org.exoplatform.services.jcr.impl.storage.value.fs.TreeFileValueStorage", vsparams);
      ArrayList<SimpleParameterEntry> spe = new ArrayList<SimpleParameterEntry>();
      spe.add(new SimpleParameterEntry("path", "target/temp/values/" + name + "_" + System.currentTimeMillis()));
      valueStorageEntry.setId("draft");
      valueStorageEntry.setParameters(spe);
      valueStorageEntry.setFilters(vsparams);

      // containerEntry.setValueStorages();
      list.add(valueStorageEntry);
      ce.setValueStorages(list);

      ws1back.setContainer(ce);

      return ws1back;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void setUp() throws Exception
   {
      super.setUp();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void tearDown() throws Exception
   {
   }
}
