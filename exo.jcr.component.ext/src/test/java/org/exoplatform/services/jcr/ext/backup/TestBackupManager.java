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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;

import org.apache.commons.collections.map.HashedMap;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.backup.impl.JobRepositoryRestore;
import org.exoplatform.services.jcr.ext.backup.impl.JobWorkspaceRestore;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

/**
 * Created by The eXo Platform SAS.
 *  Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 05.12.2007
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestBackupManager.java 760 2008-02-07 15:08:07Z pnedonosko $
 */
public class TestBackupManager extends AbstractBackupTestCase
{

   public void testFullBackupRestore() throws Exception
   {
      // backup
      File backDir = new File("target/backup/ws1");
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getName());
      config.setWorkspace("ws1");
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      BackupChain bch = backup.findBackup(repository.getName(), "ws1");

      // wait till full backup will be stopped
      while (bch.getFullBackupState() != BackupJob.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // stop fullBackup

      if (bch != null)
         backup.stopBackup(bch);
      else
         fail("Can't get fullBackup chain");

      // restore
      RepositoryEntry re = (RepositoryEntry)ws1Session.getContainer().getComponentInstanceOfType(RepositoryEntry.class);
      WorkspaceEntry ws1back = makeWorkspaceEntry("ws1back", "jdbcjcr_backup_only_use_1");

      // BackupChainLog bchLog = new BackupChainLog(backDir, rconfig);
      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         BackupChainLog bchLog = new BackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restore(bchLog, re.getName(), ws1back, false);

         // check
         SessionImpl back1 = null;
         try
         {
            back1 = (SessionImpl)repository.login(credentials, "ws1back");
            Node ws1backTestRoot = back1.getRootNode().getNode("backupTest");
            assertEquals("Restored content should be same", "property-5", ws1backTestRoot.getNode("node_5")
               .getProperty("exo:data").getString());
         }
         catch (Exception e)
         {
            e.printStackTrace();
            fail(e.getMessage());
         }
         finally
         {
            if (back1 != null)
               back1.logout();
         }
      }
      else
         fail("There are no backup files in " + backDir.getAbsolutePath());
   }

   public void testIncrementalBackupRestore() throws Exception
   {
      // full backup & incremental
      File backDir = new File("target/backup/ws1.incr");
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getName());
      config.setWorkspace("ws1");
      config.setBackupType(BackupManager.FULL_AND_INCREMENTAL);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      BackupChain bch = backup.findBackup(repository.getName(), "ws1");

      // wait till full backup will be stopped
      while (bch.getFullBackupState() != BackupJob.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // add some changes which will be logged in incremental log
      ws1TestRoot.getNode("node_3").remove();
      ws1TestRoot.getNode("node_4").remove();
      ws1TestRoot.getNode("node_5").remove();
      ws1TestRoot.addNode("node #3").setProperty("exo:data", "property #3");
      ws1TestRoot.addNode("node #5").setProperty("exo:extraData", "property #5");

      ws1TestRoot.save(); // log here via listener

      // stop all
      if (bch != null)
         backup.stopBackup(bch);
      else
         fail("Can't get fullBackup chain");

      // restore
      RepositoryEntry re = (RepositoryEntry)ws1Session.getContainer().getComponentInstanceOfType(RepositoryEntry.class);
      WorkspaceEntry ws1back = makeWorkspaceEntry("ws1back.incr", "jdbcjcr_backup_only_use_2");

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         BackupChainLog bchLog = new BackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restore(bchLog, re.getName(), ws1back, false);

         // check
         SessionImpl back1 = null;
         try
         {
            back1 = (SessionImpl)repository.login(credentials, ws1back.getName());
            Node ws1backTestRoot = back1.getRootNode().getNode("backupTest");
            assertFalse("Node should be removed", ws1backTestRoot.hasNode("node_3"));
            assertFalse("Node should be removed", ws1backTestRoot.hasNode("node_4"));
            assertFalse("Node should be removed", ws1backTestRoot.hasNode("node_5"));

            assertEquals("Restored content should be same", "property #3", ws1backTestRoot.getNode("node #3")
               .getProperty("exo:data").getString());
            assertEquals("Restored content should be same", "property #5", ws1backTestRoot.getNode("node #5")
               .getProperty("exo:extraData").getString());

            assertFalse("Proeprty should be removed", ws1backTestRoot.getNode("node #5").hasProperty("exo:data"));
         }
         catch (Exception e)
         {
            e.printStackTrace();
            fail(e.getMessage());
         }
         finally
         {
            if (back1 != null)
               back1.logout();
         }
      }
      else
         fail("There are no backup files in " + backDir.getAbsolutePath());
   }

   /**
    * With BLOBs, locks, copy and move
    * 
    * @throws Exception
    */
   public void testIncrementalBackupRestore2() throws Exception
   {
      // full backup with BLOBs & incremental with BLOBs

      // BLOBs for full
      File tempf = createBLOBTempFile("testIncrementalBackupRestore2-", 5 * 1024); // 5M
      tempf.deleteOnExit();
      ws1TestRoot.addNode("node_101").setProperty("exo:data", new FileInputStream(tempf));
      ws1TestRoot.addNode("node_102").setProperty("exo:extraData", new FileInputStream(tempf));

      File backDir = new File("target/backup/ws1.incr2");
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getName());
      config.setWorkspace("ws1");
      config.setBackupType(BackupManager.FULL_AND_INCREMENTAL);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      BackupChain bch = backup.findBackup(repository.getName(), "ws1");

      // wait till full backup will be stopped
      while (bch.getFullBackupState() != BackupJob.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // add some changes which will be logged in incremental log
      ws1TestRoot.addNode("node #53").setProperty("exo:extraData", "property #53");
      ws1TestRoot.save(); // log here via listener

      // BLOBs for incr
      ws1TestRoot.getNode("node_1").setProperty("exo:extraData", new FileInputStream(tempf));
      ws1TestRoot.getNode("node_5").setProperty("exo:data", new FileInputStream(tempf));

      ws1TestRoot.addNode("node_101").setProperty("exo:data", new FileInputStream(tempf));
      ws1TestRoot.addNode("node_102").setProperty("exo:data", new FileInputStream(tempf));
      ws1TestRoot.save(); // log here via listener

      ws1TestRoot.getNode("node_2").setProperty("exo:data", (InputStream)null); // remove property
      ws1TestRoot.getNode("node_3").setProperty("exo:data", new ByteArrayInputStream("aaa".getBytes())); // set
      // aaa
      // bytes
      ws1TestRoot.getNode("node_4").remove(); // (*)
      ws1TestRoot.save(); // log here via listener

      ws1TestRoot.getNode("node_5").addMixin("mix:lockable");
      ws1TestRoot.save(); // log here via listener
      Lock n107lock = ws1TestRoot.getNode("node_5").lock(true, false);
      ws1TestRoot.getSession().move(ws1TestRoot.getNode("node #53").getPath(),
         ws1TestRoot.getNode("node_5").getPath() + "/node #53");
      ws1TestRoot.save(); // log here via listener

      ws1TestRoot.getNode("node_6").addMixin("mix:referenceable");
      String id6 = ws1TestRoot.getNode("node_6").getUUID();
      ws1TestRoot.save(); // log here via listener

      // before(*), log here via listener
      ws1TestRoot.getSession().getWorkspace().move(ws1TestRoot.getNode("node_6").getPath(),
         ws1TestRoot.getPath() + "/node_4"); // in place of
      // 4 removed

      // stop all
      if (bch != null)
         backup.stopBackup(bch);
      else
         fail("Can't get fullBackup chain");

      // restore
      RepositoryEntry re = (RepositoryEntry)ws1Session.getContainer().getComponentInstanceOfType(RepositoryEntry.class);
      WorkspaceEntry ws1back = makeWorkspaceEntry("ws1back.incr2", "jdbcjcr_backup_only_use_3");

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         BackupChainLog bchLog = new BackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restore(bchLog, re.getName(), ws1back, false);

         // check
         SessionImpl back1 = null;
         try
         {
            back1 = (SessionImpl)repository.login(credentials, ws1back.getName());
            Node ws1backTestRoot = back1.getRootNode().getNode("backupTest");

            assertTrue("Node should exists", ws1backTestRoot.getNode("node_5").hasNode("node #53"));
            assertTrue("Property should exists", ws1backTestRoot.getNode("node_5")
               .hasProperty("node #53/exo:extraData"));

            assertTrue("Node should exists", ws1backTestRoot.hasNode("node_7"));
            assertTrue("Property should exists", ws1backTestRoot.hasProperty("node_5/exo:data"));
            assertTrue("Property should exists", ws1backTestRoot.hasProperty("node_1/exo:extraData"));
            assertTrue("Node should exists", ws1backTestRoot.hasNode("node_102"));

            compareStream(new FileInputStream(tempf), ws1backTestRoot.getNode("node_5").getProperty("exo:data")
               .getStream());
            compareStream(new FileInputStream(tempf), ws1backTestRoot.getNode("node_1").getProperty("exo:extraData")
               .getStream());

            assertFalse("Property should be removed", ws1backTestRoot.getNode("node_2").hasProperty("exo:data"));

            compareStream(new ByteArrayInputStream("aaa".getBytes()), ws1backTestRoot.getNode("node_3").getProperty(
               "exo:data").getStream());

            assertTrue("Node should be mix:lockable ", ws1backTestRoot.getNode("node_5").isNodeType("mix:lockable"));
            assertFalse("Node should be not locked ", ws1backTestRoot.getNode("node_5").isLocked());

            assertEquals("Node should be mix:referenceable and UUID should be " + id6, id6, ws1backTestRoot.getNode(
               "node_4").getUUID());
         }
         catch (Exception e)
         {
            e.printStackTrace();
            fail(e.getMessage());
         }
         finally
         {
            if (back1 != null)
               back1.logout();
         }
      }
      else
         fail("There are no backup files in " + backDir.getAbsolutePath());
   }

   public void testFullBackupRestoreAsync() throws Exception
   {
      SessionImpl sessionWS1 = (SessionImpl)repository.login(credentials, "ws1");
      sessionWS1.getRootNode().addNode("backupTest").addNode("node_5").setProperty("exo:data",
         "Restored content should be same");
      sessionWS1.save();

      // backup
      File backDir = new File("target/backup/ws1_a");
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getName());
      config.setWorkspace("ws1");
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      BackupChain bch = backup.findBackup(repository.getName(), "ws1");

      // wait till full backup will be stopped
      while (bch.getFullBackupState() != BackupJob.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // stop fullBackup

      if (bch != null)
         backup.stopBackup(bch);
      else
         fail("Can't get fullBackup chain");

      // restore
      WorkspaceEntry ws1back = makeWorkspaceEntry("ws1back5", "jdbcjcr_backup_only_use_5");

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         BackupChainLog bchLog = new BackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restore(bchLog, repository.getName(), ws1back, true);

         while (backup.getLastRestore(repository.getName(), ws1back.getName()).getStateRestore() != JobWorkspaceRestore.RESTORE_SUCCESSFUL
            && backup.getLastRestore(repository.getName(), ws1back.getName()).getStateRestore() != JobWorkspaceRestore.RESTORE_FAIL)
         {
            Thread.sleep(50);
         }

         if (backup.getLastRestore(repository.getName(), ws1back.getName()).getStateRestore() == JobWorkspaceRestore.RESTORE_FAIL)
            throw (Exception)backup.getLastRestore(repository.getName(), ws1back.getName()).getRestoreException();

         // check
         SessionImpl back1 = null;
         try
         {
            back1 = (SessionImpl)repository.login(credentials, "ws1back5");
            Node ws1backTestRoot = back1.getRootNode().getNode("backupTest");
            assertEquals("Restored content should be same", "property-5", ws1backTestRoot.getNode("node_5")
               .getProperty("exo:data").getString());
         }
         catch (Exception e)
         {
            e.printStackTrace();
            fail(e.getMessage());
         }
         finally
         {
            if (back1 != null)
               back1.logout();
         }
      }
      else
         fail("There are no backup files in " + backDir.getAbsolutePath());
   }

   public void testAutoStopBackupFull() throws Exception
   {
      // backup
      File backDir = new File("target/backup/ws1_123");
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getName());
      config.setWorkspace("ws1");
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      BackupChain bch = backup.startBackup(config);

      Thread.sleep(11000);

      for (BackupChain chain : backup.getCurrentBackups())
         if (bch.getBackupId().equals(chain.getBackupId()))
            fail("The backup with id '" + chain.getBackupId() + "' should not be active");
   }

   public void testAutoStopBackupIncr() throws Exception
   {
      // backup
      File backDir = new File("target/backup/ws1_123_321");
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getName());
      config.setWorkspace("ws1");
      config.setBackupType(BackupManager.FULL_AND_INCREMENTAL);
      config.setBackupDir(backDir);
      config.setIncrementalJobPeriod(3);
      config.setIncrementalJobNumber(0);

      BackupChain bch = backup.startBackup(config);

      Thread.sleep(11000);

      boolean isFail = true;

      for (BackupChain chain : backup.getCurrentBackups())
         if (bch.getBackupId().equals(chain.getBackupId()))
            isFail = false;

      if (isFail)
         fail("The backup with id '" + bch.getBackupId() + "' should be active");
   }

   public void testAutoStopBackupIncrRepetion() throws Exception
   {
      // backup
      File backDir = new File("target/backup/ws1_123321");
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getName());
      config.setWorkspace("ws1");
      config.setBackupType(BackupManager.FULL_AND_INCREMENTAL);
      config.setBackupDir(backDir);
      config.setIncrementalJobPeriod(4);
      config.setIncrementalJobNumber(2);

      final BackupChain bch = backup.startBackup(config);

      Thread.sleep(20000);

      assertTrue(bch.isFinished());

      for (BackupChain chain : backup.getCurrentBackups())
         if (bch.getBackupId().equals(chain.getBackupId()))
            fail("The backup with id '" + chain.getBackupId() + "' should not be active");
   }

   public void testTwoRestores() throws Exception
   {
      {
         SessionImpl sessionWS1 = (SessionImpl)repository.login(credentials, "ws3");

         sessionWS1.getRootNode().addNode("asdasdasda", "nt:unstructured").setProperty("data",
            new FileInputStream(createBLOBTempFile(1024)));
         sessionWS1.save();

         // 1-st backup
         File backDir = new File("target/backup/ws1_restore_1");
         backDir.mkdirs();

         BackupConfig config = new BackupConfig();
         config.setRepository(repository.getName());
         config.setWorkspace("ws3");
         config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
         config.setBackupDir(backDir);

         BackupChain bch = backup.startBackup(config);

         // wait till full backup will be stopped
         while (!bch.isFinished())
         {
            Thread.yield();
            Thread.sleep(50);
         }

         // 1-st restore
         WorkspaceEntry ws1_restore_1 = makeWorkspaceEntry("ws1_restore_1", "jdbcjcr_backup_only_use_6");

         File backLog = new File(bch.getLogFilePath());
         if (backLog.exists())
         {
            BackupChainLog bchLog = new BackupChainLog(backLog);

            backup.restore(bchLog, repository.getName(), ws1_restore_1, false);

            // check
            SessionImpl back1 = (SessionImpl)repository.login(credentials, "ws1_restore_1");
            assertNotNull(back1.getRootNode().getNode("asdasdasda").getProperty("data"));

            // add date to restored workspace
            back1.getRootNode().addNode("gdfgrghfhf", "nt:unstructured").setProperty("data",
               new FileInputStream(createBLOBTempFile(1024)));
            back1.save();
         }
         else
            fail("There are no backup files in " + backDir.getAbsolutePath());
      }

      {
         // 2-st backup
         File backDir = new File("target/backup/ws1_restore_2");
         backDir.mkdirs();

         BackupConfig config = new BackupConfig();
         config.setRepository(repository.getName());
         config.setWorkspace("ws1_restore_1");
         config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
         config.setBackupDir(backDir);

         BackupChain bch = backup.startBackup(config);

         // wait till full backup will be stopped
         while (!bch.isFinished())
         {
            Thread.yield();
            Thread.sleep(50);
         }

         // 2-st restore
         WorkspaceEntry ws1_restore_2 = makeWorkspaceEntry("ws1_restore_2", "jdbcjcr_backup_only_use_7");

         File backLog = new File(bch.getLogFilePath());
         if (backLog.exists())
         {
            BackupChainLog bchLog = new BackupChainLog(backLog);

            backup.restore(bchLog, repository.getName(), ws1_restore_2, false);

            // check
            SessionImpl back2 = (SessionImpl)repository.login(credentials, "ws1_restore_2");
            assertNotNull(back2.getRootNode().getNode("gdfgrghfhf").getProperty("data"));
         }
         else
            fail("There are no backup files in " + backDir.getAbsolutePath());
      }
   }

   public void testStartFullBackupWIthJobPeriod() throws Exception
   {
      // backup
      File backDir = new File("target/backup/ws1_fwp");
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getName());
      config.setWorkspace("ws1");
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);
      config.setIncrementalJobPeriod(3600);

      backup.startBackup(config);

      BackupChain bch = backup.findBackup(repository.getName(), "ws1");

      // wait till full backup will be stopped
      while (bch.getFullBackupState() != BackupJob.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }
   }

   public void testNegativeIncremetalJobPeriod() throws Exception
   {
      // backup
      File backDir = new File("target/backup/ws1_negative_period");
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getName());
      config.setWorkspace("ws1");
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
         //ok
      }
   }

   public void testNegativeIncremetalJobNumber() throws Exception
   {
      // backup
      File backDir = new File("target/backup/ws1_negative_job_number");
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getName());
      config.setWorkspace("ws1");
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
         //ok
      }
   }

   public void testRestoreAfterFAilureRestore() throws Exception
   {
      // backup
      File backDir = new File("target/backup/ws1");
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getName());
      config.setWorkspace("ws1");
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      BackupChain bch = backup.findBackup(repository.getName(), "ws1");

      // wait till full backup will be stopped
      while (bch.getFullBackupState() != BackupJob.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // stop fullBackup

      if (bch != null)
         backup.stopBackup(bch);
      else
         fail("Can't get fullBackup chain");

      // restore
      RepositoryEntry re = (RepositoryEntry)ws1Session.getContainer().getComponentInstanceOfType(RepositoryEntry.class);
      WorkspaceEntry ws1back = makeWorkspaceEntry("ws1backt", "jdbcjcr_backup_only_use_8_NOT_EXIST");

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         BackupChainLog bchLog = new BackupChainLog(backLog);

         try
         {
            backup.restore(bchLog, re.getName(), ws1back, false);
            fail("The backup can not be restored.");
         }
         catch (Exception e)
         {
            //ok

            WorkspaceEntry ws1backTwo = makeWorkspaceEntry("ws1backt", "jdbcjcr_backup_only_use_8");

            backup.restore(bchLog, re.getName(), ws1backTwo, false);
         }

         // check
         SessionImpl back1 = null;
         try
         {
            back1 = (SessionImpl)repository.login(credentials, "ws1back");
            Node ws1backTestRoot = back1.getRootNode().getNode("backupTest");
            assertEquals("Restored content should be same", "property-5", ws1backTestRoot.getNode("node_5")
               .getProperty("exo:data").getString());
         }
         catch (Exception e)
         {
            e.printStackTrace();
            fail(e.getMessage());
         }
         finally
         {
            if (back1 != null)
               back1.logout();
         }
      }
      else
         fail("There are no backup files in " + backDir.getAbsolutePath());
   }

   public void testRepositoryFullBackupRestore() throws Exception
   {
      // backup
      File backDir = new File("target/backup");
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repository.getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      RepositoryBackupChain bch = backup.findRepositoryBackup(repository.getName());

      // wait till full backup will be stopped
      while (bch.getState() != RepositoryBackupChain.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // stop fullBackup

      backup.stopBackup(bch);

      // restore
      RepositoryEntry re = (RepositoryEntry)ws1Session.getContainer().getComponentInstanceOfType(RepositoryEntry.class);

      String newRepositoryName = "repo_restored_1";
      RepositoryEntry newRepositoryEntry =
         makeRepositoryEntry(newRepositoryName, re, "jdbcjcr_to_repository_restore_1", null);

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restore(bchLog, newRepositoryEntry, false);
         assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, backup.getLastRepositoryRestore(
            newRepositoryEntry.getName()).getStateRestore());

         // check
         ManageableRepository restoredRepository = repositoryService.getRepository(newRepositoryName);

         for (String wsName : restoredRepository.getWorkspaceNames())
         {
            SessionImpl back1 = null;
            try
            {
               back1 = (SessionImpl)restoredRepository.login(credentials, wsName);
               Node ws1backTestRoot = back1.getRootNode().getNode("backupTest");
               assertEquals("Restored content should be same", "property-5", ws1backTestRoot.getNode("node_5")
                  .getProperty("exo:data").getString());
            }
            catch (Exception e)
            {
               e.printStackTrace();
               fail(e.getMessage());
            }
            finally
            {
               if (back1 != null)
                  back1.logout();
            }
         }
      }
      else
         fail("There are no backup files in " + backDir.getAbsolutePath());
   }

   public void testRepositoryFullAndIncrementalBackupRestore() throws Exception
   {
      // backup
      File backDir = new File("target/backup");
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repository.getName());
      config.setBackupType(BackupManager.FULL_AND_INCREMENTAL);
      config.setIncrementalJobPeriod(1000);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      RepositoryBackupChain bch = backup.findRepositoryBackup(repository.getName());

      // wait till full backup will be stopped
      while (bch.getState() != RepositoryBackupChain.FULL_BACKUP_FINISHED_INCREMENTAL_BACKUP_WORKING)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // stop fullBackup

      backup.stopBackup(bch);

      // restore
      RepositoryEntry re = (RepositoryEntry)ws1Session.getContainer().getComponentInstanceOfType(RepositoryEntry.class);

      String newRepositoryName = "repo_restored_4";
      RepositoryEntry newRepositoryEntry =
         makeRepositoryEntry(newRepositoryName, re, "jdbcjcr_to_repository_restore_4", null);

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restore(bchLog, newRepositoryEntry, false);

         // check
         ManageableRepository restoredRepository = repositoryService.getRepository(newRepositoryName);

         for (String wsName : restoredRepository.getWorkspaceNames())
         {
            SessionImpl back1 = null;
            try
            {
               back1 = (SessionImpl)repository.login(credentials, wsName);
               Node ws1backTestRoot = back1.getRootNode().getNode("backupTest");
               assertEquals("Restored content should be same", "property-5", ws1backTestRoot.getNode("node_5")
                  .getProperty("exo:data").getString());
            }
            catch (Exception e)
            {
               e.printStackTrace();
               fail(e.getMessage());
            }
            finally
            {
               if (back1 != null)
                  back1.logout();
            }
         }
      }
      else
         fail("There are no backup files in " + backDir.getAbsolutePath());
   }

   public void testRepositoryFullBackupAsynchronusRestore() throws Exception
   {
      // backup
      File backDir = new File("target/backup");
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repository.getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      RepositoryBackupChain bch = backup.findRepositoryBackup(repository.getName());

      // wait till full backup will be stopped
      while (bch.getState() != RepositoryBackupChain.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // stop fullBackup

      backup.stopBackup(bch);

      // restore
      RepositoryEntry re = (RepositoryEntry)ws1Session.getContainer().getComponentInstanceOfType(RepositoryEntry.class);

      String newRepositoryName = "repo_restored_2";
      RepositoryEntry newRepositoryEntry =
         makeRepositoryEntry(newRepositoryName, re, "jdbcjcr_to_repository_restore_2", null);

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restore(bchLog, newRepositoryEntry, true);

         JobRepositoryRestore job = backup.getLastRepositoryRestore(newRepositoryName);

         while (job.getStateRestore() != JobRepositoryRestore.REPOSITORY_RESTORE_SUCCESSFUL
            || job.getStateRestore() == JobRepositoryRestore.REPOSITORY_RESTORE_FAIL)
         {
            Thread.yield();
            Thread.sleep(50);
         }

         // check
         ManageableRepository restoredRepository = repositoryService.getRepository(newRepositoryName);

         for (String wsName : restoredRepository.getWorkspaceNames())
         {
            SessionImpl back1 = null;
            try
            {
               back1 = (SessionImpl)restoredRepository.login(credentials, wsName);
               Node ws1backTestRoot = back1.getRootNode().getNode("backupTest");
               assertEquals("Restored content should be same", "property-5", ws1backTestRoot.getNode("node_5")
                  .getProperty("exo:data").getString());
            }
            catch (Exception e)
            {
               e.printStackTrace();
               fail(e.getMessage());
            }
            finally
            {
               if (back1 != null)
                  back1.logout();
            }
         }
      }
      else
         fail("There are no backup files in " + backDir.getAbsolutePath());
   }

   public void testRepositoryFullBackupAsynchronusRestoreWorkspaceMapping() throws Exception
   {
      // backup
      File backDir = new File("target/backup");
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repository.getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      RepositoryBackupChain bch = backup.findRepositoryBackup(repository.getName());

      // wait till full backup will be stopped
      while (bch.getState() != RepositoryBackupChain.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // stop fullBackup

      backup.stopBackup(bch);

      // restore
      RepositoryEntry re = (RepositoryEntry)ws1Session.getContainer().getComponentInstanceOfType(RepositoryEntry.class);

      String newRepositoryName = "repo_restored_3";
      RepositoryEntry newRepositoryEntry =
         makeRepositoryEntry(newRepositoryName, re, "jdbcjcr_to_repository_restore_3", null);

      // create workspace mappingS
      Map<String, String> workspaceMapping = new HashedMap();
      for (WorkspaceEntry we : newRepositoryEntry.getWorkspaceEntries())
      {
         workspaceMapping.put(we.getName(), we.getName() + "_mapped");
      }

      // Change workspaeNames
      for (WorkspaceEntry we : newRepositoryEntry.getWorkspaceEntries())
      {
         if (newRepositoryEntry.getSystemWorkspaceName().equals(we.getName()))
         {
            newRepositoryEntry.setSystemWorkspaceName(workspaceMapping.get(we.getName()));
            newRepositoryEntry.setDefaultWorkspaceName(workspaceMapping.get(we.getName()));
         }

         we.setName(workspaceMapping.get(we.getName()));
         we.setUniqueName(we.getUniqueName() + workspaceMapping.get(we.getName()));
      }

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restore(bchLog, newRepositoryEntry, workspaceMapping, true);

         JobRepositoryRestore job = backup.getLastRepositoryRestore(newRepositoryName);

         while (job.getStateRestore() != JobRepositoryRestore.REPOSITORY_RESTORE_SUCCESSFUL
            || job.getStateRestore() == JobRepositoryRestore.REPOSITORY_RESTORE_FAIL)
         {
            Thread.yield();
            Thread.sleep(50);
         }

         // check
         ManageableRepository restoredRepository = repositoryService.getRepository(newRepositoryName);

         for (String wsName : restoredRepository.getWorkspaceNames())
         {
            SessionImpl back1 = null;
            try
            {
               back1 = (SessionImpl)restoredRepository.login(credentials, workspaceMapping.get(wsName));
               Node ws1backTestRoot = back1.getRootNode().getNode("backupTest");
               assertEquals("Restored content should be same", "property-5", ws1backTestRoot.getNode("node_5")
                  .getProperty("exo:data").getString());
            }
            catch (Exception e)
            {
               e.printStackTrace();
               fail(e.getMessage());
            }
            finally
            {
               if (back1 != null)
                  back1.logout();
            }
         }
      }
      else
         fail("There are no backup files in " + backDir.getAbsolutePath());
   }

   public void testAutoStopRepositoryBackupIncrRepetion() throws Exception
   {
      // backup
      File backDir = new File("target/backup/" + repository.getName() + "_" + System.currentTimeMillis());
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repository.getName());
      config.setBackupType(BackupManager.FULL_AND_INCREMENTAL);
      config.setBackupDir(backDir);
      config.setIncrementalJobPeriod(4);
      config.setIncrementalJobNumber(2);

      final RepositoryBackupChain bch = backup.startBackup(config);

      Thread.sleep(20000);

      assertTrue(bch.isFinished());

      for (RepositoryBackupChain chain : backup.getCurrentRepositoryBackups())
         if (bch.getBackupId().equals(chain.getBackupId()))
            fail("The backup with id '" + chain.getBackupId() + "' should not be active");
   }

   public void testRepositoryRestoreFail() throws Exception
   {
      // restore
      RepositoryEntry re = (RepositoryEntry)ws1Session.getContainer().getComponentInstanceOfType(RepositoryEntry.class);

      String newRepositoryName = "repo_restored_5";
      RepositoryEntry newRepositoryEntry =
         makeRepositoryEntry(newRepositoryName, re, "jdbcjcr_to_repository_restore_5", null);

      //create broken system workspaceEntry
      newRepositoryEntry.getWorkspaceEntries().get(0).getQueryHandler().setType("gg");

      File backLog = new File(backup.getRepositoryBackupsLogs()[0].getLogFilePath());

      RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

      try
      {
         backup.restore(bchLog, newRepositoryEntry, false);
         fail("The backup " + backLog.getAbsolutePath() + "shoulde not restored.");
      }
      catch (RepositoryRestoreExeption e)
      {
         // ok.  
      }

      // check

      try
      {
         ManageableRepository restoredRepository = repositoryService.getRepository(newRepositoryName);
         fail("The repository " + newRepositoryName + "shoulde not exists.");
      }
      catch (RepositoryException e)
      {
         // ok.
      }
   }

   public void testIncrementalBackupRestoreEXOJCR_737() throws Exception
   {
      // full backup with BLOBs & incremental with BLOBs

      // BLOBs for full
      File tempf = createBLOBTempFile("testIncrementalBackupRestore737-", 5 * 1024); // 5M
      tempf.deleteOnExit();

      File backDir = new File("target/backup/ws1.incr737");
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getName());
      config.setWorkspace("ws1");
      config.setBackupType(BackupManager.FULL_AND_INCREMENTAL);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      BackupChain bch = backup.findBackup(repository.getName(), "ws1");

      // wait till full backup will be stopped
      while (bch.getFullBackupState() != BackupJob.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // add data
      ws1Session.getRootNode().addNode("node_101").setProperty("exo:data", new FileInputStream(tempf));
      ws1Session.getRootNode().addNode("node_102").setProperty("exo:extraData", new FileInputStream(tempf));
      ws1Session.getRootNode().save(); // log here via listener

      // stop backup
      if (bch != null)
         backup.stopBackup(bch);
      else
         fail("Can't get fullBackup chain");

      //remove data 
      ws1Session.getRootNode().getNode("node_101").remove();
      ws1Session.getRootNode().getNode("node_102").remove();
      ws1Session.getRootNode().save();

      // restore
      RepositoryEntry re = (RepositoryEntry)ws1Session.getContainer().getComponentInstanceOfType(RepositoryEntry.class);
      WorkspaceEntry ws1back = makeWorkspaceEntry("ws1back.incr737", "jdbcjcr25");

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         BackupChainLog bchLog = new BackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restore(bchLog, re.getName(), ws1back, false);

         // check
         SessionImpl back1 = null;
         try
         {
            back1 = (SessionImpl)repository.login(credentials, ws1back.getName());

            Node node_101 = back1.getRootNode().getNode("node_101");
            assertNotNull(node_101);
            assertEquals(tempf.length(), node_101.getProperty("exo:data").getStream().available());
            compareStream(new FileInputStream(tempf), node_101.getProperty("exo:data").getStream());

            Node node_102 = back1.getRootNode().getNode("node_102");
            assertNotNull(node_102);
            assertEquals(tempf.length(), node_102.getProperty("exo:extraData").getStream().available());
            compareStream(new FileInputStream(tempf), node_102.getProperty("exo:extraData").getStream());

         }
         catch (Exception e)
         {
            e.printStackTrace();
            fail(e.getMessage());
         }
         finally
         {
            if (back1 != null)
               back1.logout();
         }
      }
      else
         fail("There are no backup files in " + backDir.getAbsolutePath());
   }
   
   public void testExistedWorkspaceRestore() throws Exception
   {
      // backup
      File backDir = new File("target/backup/ws1");
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getName());
      config.setWorkspace("ws1");
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      BackupChain bch = backup.findBackup(repository.getName(), "ws1");

      // wait till full backup will be stopped
      while (bch.getFullBackupState() != BackupJob.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // stop fullBackup

      if (bch != null)
         backup.stopBackup(bch);
      else
         fail("Can't get fullBackup chain");

      // restore
      RepositoryEntry re = (RepositoryEntry)ws1Session.getContainer().getComponentInstanceOfType(RepositoryEntry.class);
      WorkspaceEntry ws1 = null;
      for (WorkspaceEntry we : re.getWorkspaceEntries())
      {
         if (ws1Session.getWorkspace().getName().equals(we.getName()))
         {
            ws1 = we;
            break;
         }
      }

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         BackupChainLog bchLog = new BackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restoreExistedWorkspace(bchLog, re.getName(), ws1, false);

         // check
         SessionImpl back1 = null;
         try
         {
            back1 = (SessionImpl)repository.login(credentials, "ws1");
            Node ws1backTestRoot = back1.getRootNode().getNode("backupTest");
            assertEquals("Restored content should be same", "property-5", ws1backTestRoot.getNode("node_5")
               .getProperty("exo:data").getString());
         }
         catch (Exception e)
         {
            e.printStackTrace();
            fail(e.getMessage());
         }
         finally
         {
            if (back1 != null)
               back1.logout();
         }
      }
      else
         fail("There are no backup files in " + backDir.getAbsolutePath());
   }
   
   public void testExistedWorkspaceRestoreAsync() throws Exception
   {
      // backup
      File backDir = new File("target/backup/ws1");
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getName());
      config.setWorkspace("ws1");
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      BackupChain bch = backup.findBackup(repository.getName(), "ws1");

      // wait till full backup will be stopped
      while (bch.getFullBackupState() != BackupJob.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // stop fullBackup

      if (bch != null)
         backup.stopBackup(bch);
      else
         fail("Can't get fullBackup chain");

      // restore
      RepositoryEntry re = (RepositoryEntry)ws1Session.getContainer().getComponentInstanceOfType(RepositoryEntry.class);
      WorkspaceEntry ws1 = null;
      for (WorkspaceEntry we : re.getWorkspaceEntries())
      {
         if (ws1Session.getWorkspace().getName().equals(we.getName()))
         {
            ws1 = we;
            break;
         }
      }

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         BackupChainLog bchLog = new BackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restoreExistedWorkspace(bchLog, re.getName(), ws1, true);
         
         while (backup.getLastRestore(repository.getName(), ws1.getName()).getStateRestore() != JobWorkspaceRestore.RESTORE_SUCCESSFUL
                  && backup.getLastRestore(repository.getName(), ws1.getName()).getStateRestore() != JobWorkspaceRestore.RESTORE_FAIL)
         {
            Thread.sleep(50);
         }

         // check
         SessionImpl back1 = null;
         try
         {
            back1 = (SessionImpl)repository.login(credentials, "ws1");
            Node ws1backTestRoot = back1.getRootNode().getNode("backupTest");
            assertEquals("Restored content should be same", "property-5", ws1backTestRoot.getNode("node_5")
               .getProperty("exo:data").getString());
         }
         catch (Exception e)
         {
            e.printStackTrace();
            fail(e.getMessage());
         }
         finally
         {
            if (back1 != null)
               back1.logout();
         }
      }
      else
         fail("There are no backup files in " + backDir.getAbsolutePath());
   }
   
   public void testExistedWorkspaceRestoreAsync2() throws Exception
   {
      // backup
      File backDir = new File("target/backup/ws1");
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getName());
      config.setWorkspace("ws1");
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      BackupChain bch = backup.findBackup(repository.getName(), "ws1");

      // wait till full backup will be stopped
      while (bch.getFullBackupState() != BackupJob.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // stop fullBackup

      if (bch != null)
         backup.stopBackup(bch);
      else
         fail("Can't get fullBackup chain");

      // restore
      RepositoryEntry re = (RepositoryEntry)ws1Session.getContainer().getComponentInstanceOfType(RepositoryEntry.class);
      WorkspaceEntry ws1 = null;
      for (WorkspaceEntry we : re.getWorkspaceEntries())
      {
         if (ws1Session.getWorkspace().getName().equals(we.getName()))
         {
            ws1 = we;
            break;
         }
      }

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         BackupChainLog bchLog = new BackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restoreExistedWorkspace(bchLog.getBackupId(), re.getName(), ws1, true);
         
         while (backup.getLastRestore(repository.getName(), ws1.getName()).getStateRestore() != JobWorkspaceRestore.RESTORE_SUCCESSFUL
                  && backup.getLastRestore(repository.getName(), ws1.getName()).getStateRestore() != JobWorkspaceRestore.RESTORE_FAIL)
         {
            Thread.sleep(50);
         }

         // check
         SessionImpl back1 = null;
         try
         {
            back1 = (SessionImpl)repository.login(credentials, "ws1");
            Node ws1backTestRoot = back1.getRootNode().getNode("backupTest");
            assertEquals("Restored content should be same", "property-5", ws1backTestRoot.getNode("node_5")
               .getProperty("exo:data").getString());
         }
         catch (Exception e)
         {
            e.printStackTrace();
            fail(e.getMessage());
         }
         finally
         {
            if (back1 != null)
               back1.logout();
         }
      }
      else
         fail("There are no backup files in " + backDir.getAbsolutePath());
   }
   
   public void testExistedRepositoryRestore() throws Exception
   {
      // backup
      File backDir = new File("target/backup");
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repository.getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      RepositoryBackupChain bch = backup.findRepositoryBackup(repository.getName());

      // wait till full backup will be stopped
      while (bch.getState() != RepositoryBackupChain.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // stop fullBackup

      backup.stopBackup(bch);

      // restore             
      RepositoryEntry baseRE = (RepositoryEntry)ws1Session.getContainer().getComponentInstanceOfType(RepositoryEntry.class);
      RepositoryEntry re = makeRepositoryEntry(baseRE.getName() , baseRE, null, null);

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restoreExistedRepository(bchLog, re, false);
         
         assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, backup.getLastRepositoryRestore(
            re.getName()).getStateRestore());

         // check
         ManageableRepository restoredRepository = repositoryService.getRepository(re.getName());

         for (String wsName : restoredRepository.getWorkspaceNames())
         {
            SessionImpl back1 = null;
            try
            {
               back1 = (SessionImpl)restoredRepository.login(credentials, wsName);
               Node ws1backTestRoot = back1.getRootNode().getNode("backupTest");
               assertEquals("Restored content should be same", "property-5", ws1backTestRoot.getNode("node_5")
                  .getProperty("exo:data").getString());
            }
            catch (Exception e)
            {
               e.printStackTrace();
               fail(e.getMessage());
            }
            finally
            {
               if (back1 != null)
                  back1.logout();
            }
         }
      }
      else
         fail("There are no backup files in " + backDir.getAbsolutePath());
   }
   
   public void testExistedRepositoryRestoreMultiDB() throws Exception
   {
      RepositoryImpl repositoryDB7 = (RepositoryImpl)repositoryService.getRepository("db7");

      for (String wsName : repositoryDB7.getWorkspaceNames())
      {
         SessionImpl sessionWS = (SessionImpl)repositoryDB7.login(credentials, wsName);
         
         Node wsTestRoot = sessionWS.getRootNode().addNode("backupTest");
         sessionWS.getRootNode().save();
         addContent(wsTestRoot, 1, 10, 1);
         sessionWS.getRootNode().save();
      }
      
      SessionImpl sessionWS = (SessionImpl)repositoryDB7.login(credentials, WS_NAME);
      
      // backup
      File backDir = new File("target/backup");
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repositoryDB7.getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      RepositoryBackupChain bch = backup.findRepositoryBackup(repositoryDB7.getName());

      // wait till full backup will be stopped
      while (bch.getState() != RepositoryBackupChain.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // stop fullBackup

      backup.stopBackup(bch);

      // restore             
      RepositoryEntry baseRE = (RepositoryEntry)sessionWS.getContainer().getComponentInstanceOfType(RepositoryEntry.class);
      RepositoryEntry re = makeRepositoryEntry(baseRE.getName() , baseRE, null, null);

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restoreExistedRepository(bchLog, re, false);
         
         assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, backup.getLastRepositoryRestore(
            re.getName()).getStateRestore());

         // check
         ManageableRepository restoredRepository = repositoryService.getRepository(re.getName());

         for (String wsName : restoredRepository.getWorkspaceNames())
         {
            SessionImpl back1 = null;
            try
            {
               back1 = (SessionImpl)restoredRepository.login(credentials, wsName);
               Node ws1backTestRoot = back1.getRootNode().getNode("backupTest");
               assertEquals("Restored content should be same", "property-5", ws1backTestRoot.getNode("node_5")
                  .getProperty("exo:data").getString());
            }
            catch (Exception e)
            {
               e.printStackTrace();
               fail(e.getMessage());
            }
            finally
            {
               if (back1 != null)
                  back1.logout();
            }
         }
      }
      else
         fail("There are no backup files in " + backDir.getAbsolutePath());
   }
   
   public void testExistedRepositoryRestoreAsync() throws Exception
   {
      // backup
      File backDir = new File("target/backup");
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repository.getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      RepositoryBackupChain bch = backup.findRepositoryBackup(repository.getName());

      // wait till full backup will be stopped
      while (bch.getState() != RepositoryBackupChain.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // stop fullBackup

      backup.stopBackup(bch);

      // restore
      RepositoryEntry baseRE = (RepositoryEntry)ws1Session.getContainer().getComponentInstanceOfType(RepositoryEntry.class);
      RepositoryEntry re = makeRepositoryEntry(baseRE.getName() , baseRE, null, null);

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restoreExistedRepository(bchLog, re, true);
         
         JobRepositoryRestore job = backup.getLastRepositoryRestore(re.getName());
         
         while (job.getStateRestore() != JobRepositoryRestore.REPOSITORY_RESTORE_SUCCESSFUL
                  && job.getStateRestore() != JobRepositoryRestore.REPOSITORY_RESTORE_FAIL)
         {
            Thread.sleep(50);
         }
         
         assertEquals(JobRepositoryRestore.REPOSITORY_RESTORE_SUCCESSFUL, backup.getLastRepositoryRestore(
            re.getName()).getStateRestore());

         // check
         ManageableRepository restoredRepository = repositoryService.getRepository(re.getName());

         for (String wsName : restoredRepository.getWorkspaceNames())
         {
            SessionImpl back1 = null;
            try
            {
               back1 = (SessionImpl)restoredRepository.login(credentials, wsName);
               Node ws1backTestRoot = back1.getRootNode().getNode("backupTest");
               assertEquals("Restored content should be same", "property-5", ws1backTestRoot.getNode("node_5")
                  .getProperty("exo:data").getString());
            }
            catch (Exception e)
            {
               e.printStackTrace();
               fail(e.getMessage());
            }
            finally
            {
               if (back1 != null)
                  back1.logout();
            }
         }
      }
      else
         fail("There are no backup files in " + backDir.getAbsolutePath());
   }
   
   public void testExistedRepositoryRestoreAsync2() throws Exception
   {
      // backup
      File backDir = new File("target/backup");
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repository.getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      RepositoryBackupChain bch = backup.findRepositoryBackup(repository.getName());

      // wait till full backup will be stopped
      while (bch.getState() != RepositoryBackupChain.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // stop fullBackup

      backup.stopBackup(bch);

      // restore
      RepositoryEntry baseRE = (RepositoryEntry)ws1Session.getContainer().getComponentInstanceOfType(RepositoryEntry.class);
      RepositoryEntry re = makeRepositoryEntry(baseRE.getName() , baseRE, null, null);

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restoreExistedRepository(bchLog.getBackupId(), re, true);
         
         JobRepositoryRestore job = backup.getLastRepositoryRestore(re.getName());
         
         while (job.getStateRestore() != JobRepositoryRestore.REPOSITORY_RESTORE_SUCCESSFUL
                  && job.getStateRestore() != JobRepositoryRestore.REPOSITORY_RESTORE_FAIL)
         {
            Thread.sleep(50);
         }
         
         assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, backup.getLastRepositoryRestore(
            re.getName()).getStateRestore());

         // check
         ManageableRepository restoredRepository = repositoryService.getRepository(re.getName());

         for (String wsName : restoredRepository.getWorkspaceNames())
         {
            SessionImpl back1 = null;
            try
            {
               back1 = (SessionImpl)restoredRepository.login(credentials, wsName);
               Node ws1backTestRoot = back1.getRootNode().getNode("backupTest");
               assertEquals("Restored content should be same", "property-5", ws1backTestRoot.getNode("node_5")
                  .getProperty("exo:data").getString());
            }
            catch (Exception e)
            {
               e.printStackTrace();
               fail(e.getMessage());
            }
            finally
            {
               if (back1 != null)
                  back1.logout();
            }
         }
      }
      else
         fail("There are no backup files in " + backDir.getAbsolutePath());
   }
   
   public void testExistedSystemWorkspaceRestore() throws Exception
   {
      String systemWS = repository.getSystemWorkspaceName();
      
      // backup
      File backDir = new File("target/backup/" + systemWS);
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getName());
      config.setWorkspace(systemWS);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      BackupChain bch = backup.findBackup(repository.getName(), systemWS);

      // wait till full backup will be stopped
      while (bch.getFullBackupState() != BackupJob.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // stop fullBackup

      if (bch != null)
         backup.stopBackup(bch);
      else
         fail("Can't get fullBackup chain");

      // restore
      RepositoryEntry re = (RepositoryEntry)ws1Session.getContainer().getComponentInstanceOfType(RepositoryEntry.class);
      WorkspaceEntry ws1 = null;
      for (WorkspaceEntry we : re.getWorkspaceEntries())
      {
         if (systemWS.equals(we.getName()))
         {
            ws1 = we;
            break;
         }
      }

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         BackupChainLog bchLog = new BackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restoreExistedWorkspace(bchLog, re.getName(), ws1, false);

         // check
         SessionImpl back1 = null;
         try
         {
            back1 = (SessionImpl)repository.login(credentials, systemWS);
            Node ws1backTestRoot = back1.getRootNode().getNode("backupTest");
            assertEquals("Restored content should be same", "property-5", ws1backTestRoot.getNode("node_5")
               .getProperty("exo:data").getString());
         }
         catch (Exception e)
         {
            e.printStackTrace();
            fail(e.getMessage());
         }
         finally
         {
            if (back1 != null)
               back1.logout();
         }
      }
      else
         fail("There are no backup files in " + backDir.getAbsolutePath());
   }
   
   public void testExistedSystemWorkspaceRestoreAsync() throws Exception
   {
      String systemWS = repository.getSystemWorkspaceName();
      
      // backup
      File backDir = new File("target/backup/" + systemWS);
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getName());
      config.setWorkspace(systemWS);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      BackupChain bch = backup.findBackup(repository.getName(), systemWS);

      // wait till full backup will be stopped
      while (bch.getFullBackupState() != BackupJob.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // stop fullBackup

      if (bch != null)
         backup.stopBackup(bch);
      else
         fail("Can't get fullBackup chain");

      // restore
      RepositoryEntry re = (RepositoryEntry)ws1Session.getContainer().getComponentInstanceOfType(RepositoryEntry.class);
      WorkspaceEntry ws1 = null;
      for (WorkspaceEntry we : re.getWorkspaceEntries())
      {
         if (systemWS.equals(we.getName()))
         {
            ws1 = we;
            break;
         }
      }

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         BackupChainLog bchLog = new BackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restoreExistedWorkspace(bchLog, re.getName(), ws1, true);
         
         while (backup.getLastRestore(repository.getName(), ws1.getName()).getStateRestore() != JobWorkspaceRestore.RESTORE_SUCCESSFUL
                  && backup.getLastRestore(repository.getName(), ws1.getName()).getStateRestore() != JobWorkspaceRestore.RESTORE_FAIL)
         {
            Thread.sleep(50);
         }

         // check
         SessionImpl back1 = null;
         try
         {
            back1 = (SessionImpl)repository.login(credentials, systemWS);
            Node ws1backTestRoot = back1.getRootNode().getNode("backupTest");
            assertEquals("Restored content should be same", "property-5", ws1backTestRoot.getNode("node_5")
               .getProperty("exo:data").getString());
         }
         catch (Exception e)
         {
            e.printStackTrace();
            fail(e.getMessage());
         }
         finally
         {
            if (back1 != null)
               back1.logout();
         }
      }
      else
         fail("There are no backup files in " + backDir.getAbsolutePath());
   }
   
   public void testExistedWorkspaceRestoreMultiDB() throws Exception
   {
      RepositoryImpl repositoryDB7 = (RepositoryImpl)repositoryService.getRepository("db7");

      SessionImpl sessionWS = (SessionImpl) repositoryDB7.login(credentials, "ws1");

      Node wsTestRoot = sessionWS.getRootNode().addNode("backupTest");
      sessionWS.getRootNode().save();
      addContent(wsTestRoot, 1, 10, 1);
      sessionWS.getRootNode().save();
      
      // backup
      File backDir = new File("target/backup/ws1");
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repositoryDB7.getName());
      config.setWorkspace("ws1");
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      BackupChain bch = backup.findBackup(repositoryDB7.getName(), "ws1");

      // wait till full backup will be stopped
      while (bch.getFullBackupState() != BackupJob.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // stop fullBackup

      if (bch != null)
         backup.stopBackup(bch);
      else
         fail("Can't get fullBackup chain");

      // restore
      RepositoryEntry re = (RepositoryEntry)sessionWS.getContainer().getComponentInstanceOfType(RepositoryEntry.class);
      WorkspaceEntry ws1 = null;
      for (WorkspaceEntry we : re.getWorkspaceEntries())
      {
         if (sessionWS.getWorkspace().getName().equals(we.getName()))
         {
            ws1 = we;
            break;
         }
      }

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         BackupChainLog bchLog = new BackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restoreExistedWorkspace(bchLog, re.getName(), ws1, false);

         // check
         SessionImpl back1 = null;
         try
         {
            back1 = (SessionImpl)repositoryDB7.login(credentials, "ws1");
            Node ws1backTestRoot = back1.getRootNode().getNode("backupTest");
            assertEquals("Restored content should be same", "property-5", ws1backTestRoot.getNode("node_5")
               .getProperty("exo:data").getString());
         }
         catch (Exception e)
         {
            e.printStackTrace();
            fail(e.getMessage());
         }
         finally
         {
            if (back1 != null)
               back1.logout();
         }
      }
      else
         fail("There are no backup files in " + backDir.getAbsolutePath());
   }
}
