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
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.lock.Lock;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 2010
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id$
 */
public abstract class AbstractBackupUseCasesTest
   extends AbstractBackupTestCase
{

   private static volatile long uuIndex;

   protected static synchronized long getUUIndex()
   {
      return uuIndex++;
   }

   public void testFullBackupRestore() throws Exception
   {
      // backup
      File backDir = new File("target/backup/" + workspaceNameToBackup + getUUIndex());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repositoryNameToBackup);
      config.setWorkspace(workspaceNameToBackup);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      BackupChain bch = backup.findBackup(repositoryNameToBackup, workspaceNameToBackup);

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
      RepositoryEntry re =
               (RepositoryEntry) ws1Session.getContainer().getComponentInstanceOfType(RepositoryEntry.class);
      WorkspaceEntry ws1back = makeWorkspaceEntry(workspaceNameToRestore, dataSourceToWorkspaceRestore);

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
            back1 = (SessionImpl) getReposityToBackup().login(credentials, workspaceNameToRestore);
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
      File backDir = new File("target/backup/" + workspaceNameToBackup + getUUIndex());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repositoryNameToBackup);
      config.setWorkspace(workspaceNameToBackup);
      config.setBackupType(BackupManager.FULL_AND_INCREMENTAL);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      BackupChain bch = backup.findBackup(repositoryNameToBackup, workspaceNameToBackup);

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
      RepositoryEntry re =
               (RepositoryEntry) ws1Session.getContainer().getComponentInstanceOfType(RepositoryEntry.class);
      WorkspaceEntry ws1back = makeWorkspaceEntry(workspaceNameToRestore, dataSourceToWorkspaceRestore);

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         BackupChainLog bchLog = new BackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restore(bchLog, repositoryNameToBackup, ws1back, false);

         // check
         SessionImpl back1 = null;
         try
         {
            back1 = (SessionImpl) getReposityToBackup().login(credentials, workspaceNameToRestore);
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

   public void testIncrementalBackupRestore2() throws Exception
   {
      // full backup with BLOBs & incremental with BLOBs

      // BLOBs for full
      File tempf = createBLOBTempFile("testIncrementalBackupRestore2-", 5 * 1024); // 5M
      tempf.deleteOnExit();
      ws1TestRoot.addNode("node_101").setProperty("exo:data", new FileInputStream(tempf));
      ws1TestRoot.addNode("node_102").setProperty("exo:extraData", new FileInputStream(tempf));

      File backDir = new File("target/backup/" + workspaceNameToBackup + getUUIndex());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repositoryNameToBackup);
      config.setWorkspace(workspaceNameToBackup);
      config.setBackupType(BackupManager.FULL_AND_INCREMENTAL);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      BackupChain bch = backup.findBackup(repositoryNameToBackup, workspaceNameToBackup);

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

      ws1TestRoot.getNode("node_2").setProperty("exo:data", (Value) null); // remove property
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
      RepositoryEntry re =
               (RepositoryEntry) ws1Session.getContainer().getComponentInstanceOfType(RepositoryEntry.class);
      WorkspaceEntry ws1back = makeWorkspaceEntry(workspaceNameToRestore, dataSourceToWorkspaceRestore);

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         BackupChainLog bchLog = new BackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restore(bchLog, repositoryNameToBackup, ws1back, false);

         // check
         SessionImpl back1 = null;
         try
         {
            back1 = (SessionImpl) getReposityToBackup().login(credentials, workspaceNameToRestore);
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
      SessionImpl sessionWS1 = (SessionImpl) getReposityToBackup().login(credentials, workspaceNameToBackup);
      sessionWS1.getRootNode().getNode("backupTest").getNode("node_5").setProperty("exo:data",
               "Restored content should be same");
      sessionWS1.save();

      // backup
      File backDir = new File("target/backup/" + workspaceNameToBackup + getUUIndex());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repositoryNameToBackup);
      config.setWorkspace(workspaceNameToBackup);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      BackupChain bch = backup.findBackup(repositoryNameToBackup, workspaceNameToBackup);

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
      WorkspaceEntry ws1back = makeWorkspaceEntry(workspaceNameToRestore, dataSourceToWorkspaceRestore);

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         BackupChainLog bchLog = new BackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restore(bchLog, repositoryNameToBackup, ws1back, true);

         while (backup.getLastRestore(repositoryNameToBackup, workspaceNameToRestore).getStateRestore() != JobWorkspaceRestore.RESTORE_SUCCESSFUL
                  && backup.getLastRestore(repositoryNameToBackup, workspaceNameToRestore).getStateRestore() != JobWorkspaceRestore.RESTORE_FAIL)
         {
            Thread.sleep(50);
         }

         if (backup.getLastRestore(repositoryNameToBackup, workspaceNameToRestore).getStateRestore() == JobWorkspaceRestore.RESTORE_FAIL)
            throw (Exception) backup.getLastRestore(repositoryNameToBackup, workspaceNameToRestore)
                     .getRestoreException();

         // check
         SessionImpl back1 = null;
         try
         {
            back1 = (SessionImpl) getReposityToBackup().login(credentials, workspaceNameToRestore);
            Node ws1backTestRoot = back1.getRootNode().getNode("backupTest");
            assertEquals("Restored content should be same", "Restored content should be same", ws1backTestRoot.getNode(
                     "node_5").getProperty("exo:data").getString());
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
      File backDir = new File("target/backup/" + workspaceNameToBackup + getUUIndex());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repositoryNameToBackup);
      config.setWorkspace(workspaceNameToBackup);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      BackupChain bch = backup.startBackup(config);

      while (!bch.isFinished())
      {
         Thread.yield();
         Thread.sleep(50);
      }

      Thread.sleep(5000);

      for (BackupChain chain : backup.getCurrentBackups())
         if (bch.getBackupId().equals(chain.getBackupId()))
            fail("The backup with id '" + chain.getBackupId() + "' should not be active");
   }

   public void testAutoStopBackupIncr() throws Exception
   {
      // backup
      File backDir = new File("target/backup/" + workspaceNameToBackup + getUUIndex());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repositoryNameToBackup);
      config.setWorkspace(workspaceNameToBackup);
      config.setBackupType(BackupManager.FULL_AND_INCREMENTAL);
      config.setBackupDir(backDir);
      config.setIncrementalJobPeriod(3);
      config.setIncrementalJobNumber(0);

      BackupChain bch = backup.startBackup(config);

      while (bch.getFullBackupState() != BackupJob.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      boolean isFail = true;

      for (BackupChain chain : backup.getCurrentBackups())
         if (bch.getBackupId().equals(chain.getBackupId()))
            isFail = false;

      if (isFail)
         fail("The backup with id '" + bch.getBackupId() + "' should be active");

      backup.stopBackup(bch);
   }

   public void testAutoStopBackupIncrRepetion() throws Exception
   {
      // backup
      File backDir = new File("target/backup/" + workspaceNameToBackup + getUUIndex());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repositoryNameToBackup);
      config.setWorkspace(workspaceNameToBackup);
      config.setBackupType(BackupManager.FULL_AND_INCREMENTAL);
      config.setBackupDir(backDir);
      config.setIncrementalJobPeriod(4);
      config.setIncrementalJobNumber(2);

      final BackupChain bch = backup.startBackup(config);

      while (!bch.isFinished())
      {
         Thread.yield();
         Thread.sleep(50);
      }

      Thread.sleep(5000);

      for (BackupChain chain : backup.getCurrentBackups())
         if (bch.getBackupId().equals(chain.getBackupId()))
            fail("The backup with id '" + chain.getBackupId() + "' should not be active");
   }

   public void testTwoRestores() throws Exception
   {
      {
         SessionImpl sessionWS1 = (SessionImpl) getReposityToBackup().login(credentials, workspaceNameToBackup);

         sessionWS1.getRootNode().addNode("asdasdasda", "nt:unstructured").setProperty("data",
                  new FileInputStream(createBLOBTempFile(1024)));
         sessionWS1.save();

         // 1-st backup
         File backDir = new File("target/backup/" + workspaceNameToBackup + getUUIndex());
         backDir.mkdirs();

         BackupConfig config = new BackupConfig();
         config.setRepository(repositoryNameToBackup);
         config.setWorkspace(workspaceNameToBackup);
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
         WorkspaceEntry ws1_restore_1 = makeWorkspaceEntry(workspaceNameToRestore, dataSourceToWorkspaceRestore);

         File backLog = new File(bch.getLogFilePath());
         if (backLog.exists())
         {
            BackupChainLog bchLog = new BackupChainLog(backLog);

            backup.restore(bchLog, repositoryNameToBackup, ws1_restore_1, false);

            // check
            SessionImpl back1 = (SessionImpl) getReposityToBackup().login(credentials, workspaceNameToRestore);
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
         File backDir = new File("target/backup/" + workspaceNameToBackup + getUUIndex());
         backDir.mkdirs();

         BackupConfig config = new BackupConfig();
         config.setRepository(repositoryNameToBackup);
         config.setWorkspace(workspaceNameToRestore);
         config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
         config.setBackupDir(backDir);

         BackupChain bch = backup.startBackup(config);

         // wait till full backup will be stopped
         while (!bch.isFinished())
         {
            Thread.yield();
            Thread.sleep(50);
         }

         removeWorkspaceFully(repositoryNameToBackup, workspaceNameToRestore);

         // 2-st restore
         WorkspaceEntry ws1_restore_2 = makeWorkspaceEntry(workspaceNameToRestore, dataSourceToWorkspaceRestore);

         File backLog = new File(bch.getLogFilePath());
         if (backLog.exists())
         {
            BackupChainLog bchLog = new BackupChainLog(backLog);

            backup.restore(bchLog, repositoryNameToBackup, ws1_restore_2, false);

            // check
            SessionImpl back2 = (SessionImpl) getReposityToBackup().login(credentials, workspaceNameToRestore);
            assertNotNull(back2.getRootNode().getNode("gdfgrghfhf").getProperty("data"));
         }
         else
            fail("There are no backup files in " + backDir.getAbsolutePath());
      }
   }

   public void testStartFullBackupWIthJobPeriod() throws Exception
   {
      // backup
      File backDir = new File("target/backup" + workspaceNameToBackup + getUUIndex());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repositoryNameToBackup);
      config.setWorkspace(workspaceNameToBackup);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);
      config.setIncrementalJobPeriod(3600);

      backup.startBackup(config);

      BackupChain bch = backup.findBackup(repositoryNameToBackup, workspaceNameToBackup);

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
      File backDir = new File("target/backup/ws1_negative_period" + getUUIndex());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repositoryNameToBackup);
      config.setWorkspace(workspaceNameToBackup);
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
      File backDir = new File("target/backup/ws1_negative_job_number" + getUUIndex());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repositoryNameToBackup);
      config.setWorkspace(workspaceNameToBackup);
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
      File backDir = new File("target/backup/" + workspaceNameToBackup + getUUIndex());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repositoryNameToBackup);
      config.setWorkspace(workspaceNameToBackup);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      BackupChain bch = backup.findBackup(repositoryNameToBackup, workspaceNameToBackup);

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
      RepositoryEntry re =
               (RepositoryEntry) ws1Session.getContainer().getComponentInstanceOfType(RepositoryEntry.class);
      WorkspaceEntry ws1back = makeWorkspaceEntry(workspaceNameToRestore, dataSourceToWorkspaceRestore + "NOT_EXIST");

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         BackupChainLog bchLog = new BackupChainLog(backLog);

         try
         {
            backup.restore(bchLog, repositoryNameToBackup, ws1back, false);
            fail("The backup can not be restored.");
         }
         catch (Exception e)
         {
            //ok

            WorkspaceEntry ws1backTwo = makeWorkspaceEntry(workspaceNameToRestore, dataSourceToWorkspaceRestore);

            backup.restore(bchLog, repositoryNameToBackup, ws1backTwo, false);
         }

         // check
         SessionImpl back1 = null;
         try
         {
            back1 = (SessionImpl) getReposityToBackup().login(credentials, workspaceNameToRestore);
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
      File backDir = new File("target/backup/" + getUUIndex());
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repositoryNameToBackup);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      RepositoryBackupChain bch = backup.findRepositoryBackup(repositoryNameToBackup);

      // wait till full backup will be stopped
      while (bch.getState() != RepositoryBackupChain.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // stop fullBackup

      backup.stopBackup(bch);

      // restore
      RepositoryEntry newRepositoryEntry =
               makeRepositoryEntry(repositoryNameToRestore, getReposityToBackup().getConfiguration(),
                        dataSourceToRepositoryRestore, null);

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
         ManageableRepository restoredRepository = repositoryService.getRepository(repositoryNameToRestore);

         for (String wsName : restoredRepository.getWorkspaceNames())
         {
            SessionImpl back1 = null;
            try
            {
               back1 = (SessionImpl) restoredRepository.login(credentials, wsName);
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
      File backDir = new File("target/backup/" + getUUIndex());
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repositoryNameToBackup);
      config.setBackupType(BackupManager.FULL_AND_INCREMENTAL);
      config.setIncrementalJobPeriod(1000);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      RepositoryBackupChain bch = backup.findRepositoryBackup(repositoryNameToBackup);

      // wait till full backup will be stopped
      while (bch.getState() != RepositoryBackupChain.FULL_BACKUP_FINISHED_INCREMENTAL_BACKUP_WORKING)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // stop fullBackup

      backup.stopBackup(bch);

      // restore
      RepositoryEntry newRepositoryEntry =
               makeRepositoryEntry(repositoryNameToRestore, getReposityToBackup().getConfiguration(),
                        dataSourceToRepositoryRestore, null);

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restore(bchLog, newRepositoryEntry, false);

         // check
         ManageableRepository restoredRepository = repositoryService.getRepository(repositoryNameToRestore);

         for (String wsName : restoredRepository.getWorkspaceNames())
         {
            SessionImpl back1 = null;
            try
            {
               back1 = (SessionImpl) restoredRepository.login(credentials, wsName);
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
      File backDir = new File("target/backup/" + getUUIndex());
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repositoryNameToBackup);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      RepositoryBackupChain bch = backup.findRepositoryBackup(repositoryNameToBackup);

      // wait till full backup will be stopped
      while (bch.getState() != RepositoryBackupChain.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // stop fullBackup

      backup.stopBackup(bch);

      // restore
      RepositoryEntry newRepositoryEntry =
               makeRepositoryEntry(repositoryNameToRestore, getReposityToBackup().getConfiguration(),
                        dataSourceToRepositoryRestore, null);

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restore(bchLog, newRepositoryEntry, true);

         JobRepositoryRestore job = backup.getLastRepositoryRestore(repositoryNameToRestore);

         while (job.getStateRestore() != JobRepositoryRestore.REPOSITORY_RESTORE_SUCCESSFUL
                  || job.getStateRestore() == JobRepositoryRestore.REPOSITORY_RESTORE_FAIL)
         {
            Thread.yield();
            Thread.sleep(50);
         }

         // check
         ManageableRepository restoredRepository = repositoryService.getRepository(repositoryNameToRestore);

         for (String wsName : restoredRepository.getWorkspaceNames())
         {
            SessionImpl back1 = null;
            try
            {
               back1 = (SessionImpl) restoredRepository.login(credentials, wsName);
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
      File backDir = new File("target/backup/" + getUUIndex());
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repositoryNameToBackup);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      RepositoryBackupChain bch = backup.findRepositoryBackup(repositoryNameToBackup);

      // wait till full backup will be stopped
      while (bch.getState() != RepositoryBackupChain.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // stop fullBackup

      backup.stopBackup(bch);

      // restore
      RepositoryEntry newRepositoryEntry =
               makeRepositoryEntry(repositoryNameToRestore, getReposityToBackup().getConfiguration(),
                        dataSourceToRepositoryRestore, null);

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

         JobRepositoryRestore job = backup.getLastRepositoryRestore(repositoryNameToRestore);

         while (job.getStateRestore() != JobRepositoryRestore.REPOSITORY_RESTORE_SUCCESSFUL
                  || job.getStateRestore() == JobRepositoryRestore.REPOSITORY_RESTORE_FAIL)
         {
            Thread.yield();
            Thread.sleep(50);
         }

         // check
         ManageableRepository restoredRepository = repositoryService.getRepository(repositoryNameToRestore);

         for (String wsName : restoredRepository.getWorkspaceNames())
         {
            SessionImpl back1 = null;
            try
            {
               back1 = (SessionImpl) restoredRepository.login(credentials, workspaceMapping.get(wsName));
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
      File backDir = new File("target/backup/" + getUUIndex());
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repositoryNameToBackup);
      config.setBackupType(BackupManager.FULL_AND_INCREMENTAL);
      config.setBackupDir(backDir);
      config.setIncrementalJobPeriod(4);
      config.setIncrementalJobNumber(2);

      final RepositoryBackupChain bch = backup.startBackup(config);

      while (!bch.isFinished())
      {
         Thread.yield();
         Thread.sleep(50);
      }

      Thread.sleep(5000);

      for (RepositoryBackupChain chain : backup.getCurrentRepositoryBackups())
         if (bch.getBackupId().equals(chain.getBackupId()))
            fail("The backup with id '" + chain.getBackupId() + "' should not be active");
   }

   public void testRepositoryRestoreFail() throws Exception
   {
      // backup
      File backDir = new File("target/backup/" + getUUIndex());
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repositoryNameToBackup);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      RepositoryBackupChain bch = backup.findRepositoryBackup(repositoryNameToBackup);

      // wait till full backup will be stopped
      while (bch.getState() != RepositoryBackupChain.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // stop fullBackup
      backup.stopBackup(bch);

      Thread.sleep(5000);

      String repoName = repositoryNameToRestore + System.currentTimeMillis();

      // restore
      RepositoryEntry newRepositoryEntry =
               makeRepositoryEntry(repoName, getReposityToBackup().getConfiguration(), dataSourceToRepositoryRestore,
                        null);

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
         ManageableRepository restoredRepository = repositoryService.getRepository(repoName);
         fail("The repository " + repositoryNameToRestore + "shoulde not exists.");
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

      File backDir = new File("target/backup/" + getUUIndex());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repositoryNameToBackup);
      config.setWorkspace(workspaceNameToBackup);
      config.setBackupType(BackupManager.FULL_AND_INCREMENTAL);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      BackupChain bch = backup.findBackup(repositoryNameToBackup, workspaceNameToBackup);

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
      WorkspaceEntry ws1back = makeWorkspaceEntry(workspaceNameToRestore, dataSourceToWorkspaceRestore);

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         BackupChainLog bchLog = new BackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restore(bchLog, repositoryNameToBackup, ws1back, false);

         // check
         SessionImpl back1 = null;
         try
         {
            back1 = (SessionImpl) getReposityToBackup().login(credentials, workspaceNameToRestore);

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

   public void testExistedWorkspaceRestoreMultiDB() throws Exception
   {
      String repositoryNameToBackup = "db8";
      SessionImpl ws1Session =
         (SessionImpl)repositoryService.getRepository(repositoryNameToBackup).login(credentials, "ws1");

      ws1Session.getRootNode().addNode("TESTNODE");
      ws1Session.save();

      // backup
      File backDir = new File("target/backup/" + getUUIndex());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repositoryNameToBackup);
      config.setWorkspace(workspaceNameToBackup);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      BackupChain bch = backup.findBackup(repositoryNameToBackup, workspaceNameToBackup);

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
      RepositoryEntry re =
               (RepositoryEntry) ws1Session.getContainer().getComponentInstanceOfType(RepositoryEntry.class);
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

         backup.restoreExistingWorkspace(bchLog, repositoryNameToBackup, ws1, false);

         // check
         SessionImpl back1 = null;
         try
         {
            back1 = (SessionImpl)repositoryService.getRepository("db8").login(credentials, "ws1");
            Node ws1backTestRoot = back1.getRootNode().getNode("TESTNODE");
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
      SessionImpl ws1Session = (SessionImpl) repositoryService.getRepository("db7").login(credentials, "ws1");

      // backup
      File backDir = new File("target/backup/" + getUUIndex());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository("db7");
      config.setWorkspace(workspaceNameToBackup);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      BackupChain bch = backup.findBackup("db7", workspaceNameToBackup);

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
      RepositoryEntry re =
               (RepositoryEntry) ws1Session.getContainer().getComponentInstanceOfType(RepositoryEntry.class);
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

         backup.restoreExistingWorkspace(bchLog, "db7", ws1, false);

         // check
         SessionImpl back1 = null;
         try
         {
            back1 = (SessionImpl) repositoryService.getRepository("db7").login(credentials, "ws1");
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
      File backDir = new File("target/backup/" + getUUIndex());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repositoryNameToBackup);
      config.setWorkspace(workspaceNameToBackup);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      BackupChain bch = backup.findBackup(repositoryNameToBackup, workspaceNameToBackup);

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
      RepositoryEntry re =
               (RepositoryEntry) ws1Session.getContainer().getComponentInstanceOfType(RepositoryEntry.class);
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

         backup.restoreExistingWorkspace(bchLog, repositoryNameToBackup, ws1, true);

         while (backup.getLastRestore(repositoryNameToBackup, ws1.getName()).getStateRestore() != JobWorkspaceRestore.RESTORE_SUCCESSFUL
                  && backup.getLastRestore(repositoryNameToBackup, ws1.getName()).getStateRestore() != JobWorkspaceRestore.RESTORE_FAIL)
         {
            Thread.sleep(50);
         }

         // check
         SessionImpl back1 = null;
         try
         {
            back1 =
                     (SessionImpl) repositoryService.getRepository(repositoryNameToBackup).login(credentials,
                              workspaceNameToBackup);
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
      File backDir = new File("target/backup/" + getUUIndex());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repositoryNameToBackup);
      config.setWorkspace(workspaceNameToBackup);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      BackupChain bch = backup.findBackup(repositoryNameToBackup, workspaceNameToBackup);

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
      RepositoryEntry re =
               (RepositoryEntry) ws1Session.getContainer().getComponentInstanceOfType(RepositoryEntry.class);
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

         backup.restoreExistingWorkspace(bchLog.getBackupId(), repositoryNameToBackup, ws1, true);

         while (backup.getLastRestore(repositoryNameToBackup, ws1.getName()).getStateRestore() != JobWorkspaceRestore.RESTORE_SUCCESSFUL
                  && backup.getLastRestore(repositoryNameToBackup, ws1.getName()).getStateRestore() != JobWorkspaceRestore.RESTORE_FAIL)
         {
            Thread.sleep(50);
         }

         // check
         SessionImpl back1 = null;
         try
         {
            back1 =
                     (SessionImpl) repositoryService.getRepository(repositoryNameToBackup).login(credentials,
                              workspaceNameToBackup);
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
      File backDir = new File("target/backup/" + getUUIndex());
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repositoryNameToBackup);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      RepositoryBackupChain bch = backup.findRepositoryBackup(repositoryNameToBackup);

      // wait till full backup will be stopped
      while (bch.getState() != RepositoryBackupChain.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // stop fullBackup

      backup.stopBackup(bch);

      // restore             
      RepositoryEntry re =
               makeRepositoryEntry(repositoryNameToBackup, getReposityToBackup().getConfiguration(), null, null);

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restoreExistingRepository(bchLog, re, false);

         assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, backup.getLastRepositoryRestore(repositoryNameToBackup)
                  .getStateRestore());

         // check
         ManageableRepository restoredRepository = repositoryService.getRepository(repositoryNameToBackup);

         for (String wsName : restoredRepository.getWorkspaceNames())
         {
            SessionImpl back1 = null;
            try
            {
               back1 = (SessionImpl) restoredRepository.login(credentials, wsName);
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

   public void testExistedRepositoryRestoreSingelDB() throws Exception
   {
      RepositoryImpl repositoryDB7 = (RepositoryImpl) repositoryService.getRepository(repositoryNameToBackupSingleDB);
      SessionImpl sessionWS = (SessionImpl) repositoryDB7.login(credentials, workspaceNameToBackup);

      // backup
      File backDir = new File("target/backup/" + getUUIndex());
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repositoryNameToBackupSingleDB);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      RepositoryBackupChain bch = backup.findRepositoryBackup(repositoryNameToBackupSingleDB);

      // wait till full backup will be stopped
      while (bch.getState() != RepositoryBackupChain.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // stop fullBackup

      backup.stopBackup(bch);

      // restore             
      RepositoryEntry baseRE =
               (RepositoryEntry) sessionWS.getContainer().getComponentInstanceOfType(RepositoryEntry.class);
      RepositoryEntry re = makeRepositoryEntry(baseRE.getName(), baseRE, null, null);

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restoreExistingRepository(bchLog, re, false);

         assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, backup.getLastRepositoryRestore(re.getName())
                  .getStateRestore());

         // check
         ManageableRepository restoredRepository = repositoryService.getRepository(repositoryNameToBackupSingleDB);

         for (String wsName : restoredRepository.getWorkspaceNames())
         {
            SessionImpl back1 = null;
            try
            {
               back1 = (SessionImpl) restoredRepository.login(credentials, wsName);
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
      File backDir = new File("target/backup/" + getUUIndex());
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repositoryNameToBackup);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      RepositoryBackupChain bch = backup.findRepositoryBackup(repositoryNameToBackup);

      // wait till full backup will be stopped
      while (bch.getState() != RepositoryBackupChain.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // stop fullBackup

      backup.stopBackup(bch);

      // restore
      RepositoryEntry re =
               makeRepositoryEntry(repositoryNameToBackup, getReposityToBackup().getConfiguration(), null, null);

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restoreExistingRepository(bchLog, re, true);

         JobRepositoryRestore job = backup.getLastRepositoryRestore(repositoryNameToBackup);

         while (job.getStateRestore() != JobRepositoryRestore.REPOSITORY_RESTORE_SUCCESSFUL
                  && job.getStateRestore() != JobRepositoryRestore.REPOSITORY_RESTORE_FAIL)
         {
            Thread.sleep(50);
         }

         assertEquals(JobRepositoryRestore.REPOSITORY_RESTORE_SUCCESSFUL, backup.getLastRepositoryRestore(
                  repositoryNameToBackup).getStateRestore());

         // check
         ManageableRepository restoredRepository = repositoryService.getRepository(repositoryNameToBackup);

         for (String wsName : restoredRepository.getWorkspaceNames())
         {
            SessionImpl back1 = null;
            try
            {
               back1 = (SessionImpl) restoredRepository.login(credentials, wsName);
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
      config.setRepository(repositoryNameToBackup);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      RepositoryBackupChain bch = backup.findRepositoryBackup(repositoryNameToBackup);

      // wait till full backup will be stopped
      while (bch.getState() != RepositoryBackupChain.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // stop fullBackup

      backup.stopBackup(bch);

      // restore
      RepositoryEntry re =
               makeRepositoryEntry(repositoryNameToBackup, getReposityToBackup().getConfiguration(), null, null);

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restoreExistingRepository(bchLog.getBackupId(), re, true);

         JobRepositoryRestore job = backup.getLastRepositoryRestore(repositoryNameToBackup);

         while (job.getStateRestore() != JobRepositoryRestore.REPOSITORY_RESTORE_SUCCESSFUL
                  && job.getStateRestore() != JobRepositoryRestore.REPOSITORY_RESTORE_FAIL)
         {
            Thread.sleep(50);
         }

         assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, backup.getLastRepositoryRestore(repositoryNameToBackup)
                  .getStateRestore());

         // check
         ManageableRepository restoredRepository = repositoryService.getRepository(repositoryNameToBackup);

         for (String wsName : restoredRepository.getWorkspaceNames())
         {
            SessionImpl back1 = null;
            try
            {
               back1 = (SessionImpl) restoredRepository.login(credentials, wsName);
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

   public void testExistedWorkspaceRestoreSingelDB() throws Exception
   {
      RepositoryImpl repositoryDB7 = (RepositoryImpl) repositoryService.getRepository(repositoryNameToBackupSingleDB);
      SessionImpl sessionWS = (SessionImpl) repositoryDB7.login(credentials, workspaceNameToBackup);

      // backup
      File backDir = new File("target/backup/" + getUUIndex());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repositoryNameToBackupSingleDB);
      config.setWorkspace(workspaceNameToBackup);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      BackupChain bch = backup.findBackup(repositoryNameToBackupSingleDB, workspaceNameToBackup);

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
      RepositoryEntry re = (RepositoryEntry) sessionWS.getContainer().getComponentInstanceOfType(RepositoryEntry.class);
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

         backup.restoreExistingWorkspace(bchLog, repositoryNameToBackupSingleDB, ws1, false);

         // check
         SessionImpl back1 = null;
         try
         {
            repositoryDB7 = (RepositoryImpl) repositoryService.getRepository(repositoryNameToBackupSingleDB);
            back1 = (SessionImpl) repositoryDB7.login(credentials, workspaceNameToBackup);
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

   public void testExistedWorkspaceRestoreWithConfig() throws Exception
   {
      // backup
      File backDir = new File("target/backup/" + getUUIndex());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repositoryNameToBackup);
      config.setWorkspace(workspaceNameToBackup);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      BackupChain bch = backup.findBackup(repositoryNameToBackup, workspaceNameToBackup);

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

      super.tearDown();

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         BackupChainLog bchLog = new BackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restoreExistingWorkspace(bchLog.getBackupId(), false);

         // check
         SessionImpl back1 = null;
         try
         {
            back1 =
                     (SessionImpl) repositoryService.getRepository(repositoryNameToBackup).login(credentials,
                              workspaceNameToBackup);
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

   public void testExistedRepositoryRestoreWithConfig() throws Exception
   {
      // backup
      File backDir = new File("target/backup/" + getUUIndex());
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repositoryNameToBackup);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      RepositoryBackupChain bch = backup.findRepositoryBackup(repositoryNameToBackup);

      // wait till full backup will be stopped
      while (bch.getState() != RepositoryBackupChain.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // stop fullBackup
      backup.stopBackup(bch);

      // check
      super.tearDown();

      // restore             
      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restoreExistingRepository(bchLog.getBackupId(), false);

         assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, backup.getLastRepositoryRestore(repositoryNameToBackup)
                  .getStateRestore());

         // check
         ManageableRepository restoredRepository = repositoryService.getRepository(repositoryNameToBackup);

         for (String wsName : restoredRepository.getWorkspaceNames())
         {
            SessionImpl back1 = null;
            try
            {
               back1 = (SessionImpl) restoredRepository.login(credentials, wsName);
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

   public void testWorkspaceRestoreWithConfig() throws Exception
   {
      // backup
      File backDir = new File("target/backup/" + getUUIndex());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repositoryNameToBackup);
      config.setWorkspace(workspaceNameToBackup);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      BackupChain bch = backup.findBackup(repositoryNameToBackup, workspaceNameToBackup);

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

      //TODO
      //      super.tearDown();
      removeWorkspaceFullySingleDB(repositoryNameToBackup, workspaceNameToBackup);

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         BackupChainLog bchLog = new BackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restoreWorkspace(bchLog.getBackupId(), false);

         // check
         SessionImpl back1 = null;
         try
         {
            back1 = (SessionImpl) repositoryService.getRepository(repositoryNameToBackup).login(credentials, "ws1");
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

   public void testRepositoryRestoreWithConfig() throws Exception
   {
      // backup
      File backDir = new File("target/backup/" + getUUIndex());
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repositoryNameToBackup);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      RepositoryBackupChain bch = backup.findRepositoryBackup(repositoryNameToBackup);

      // wait till full backup will be stopped
      while (bch.getState() != RepositoryBackupChain.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // stop fullBackup

      backup.stopBackup(bch);

      //TODO
      super.tearDown();
      removeRepositoryFully(repositoryNameToBackup);

      // restore             
      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restoreRepository(bchLog.getBackupId(), false);

         assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, backup.getLastRepositoryRestore(repositoryNameToBackup)
                  .getStateRestore());

         // check
         ManageableRepository restoredRepository = repositoryService.getRepository(repositoryNameToBackup);

         for (String wsName : restoredRepository.getWorkspaceNames())
         {
            SessionImpl back1 = null;
            try
            {
               back1 = (SessionImpl) restoredRepository.login(credentials, wsName);
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

   public void testRepositoryRestoreWithNullConfig() throws Exception
   {
      // backup
      File backDir = new File((File.createTempFile("12123", "123")).getParent() + File.separator + getUUIndex());
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repositoryNameToBackup);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      RepositoryBackupChain bch = backup.findRepositoryBackup(repositoryNameToBackup);

      // wait till full backup will be stopped
      while (bch.getState() != RepositoryBackupChain.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // stop fullBackup

      backup.stopBackup(bch);

      //TODO
      super.tearDown();
      removeRepositoryFully(repositoryNameToBackup);

      // restore             
      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restore(bchLog, null, false);

         assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, backup.getLastRepositoryRestore(repositoryNameToBackup)
                  .getStateRestore());

         // check
         ManageableRepository restoredRepository = repositoryService.getRepository(repositoryNameToBackup);

         for (String wsName : restoredRepository.getWorkspaceNames())
         {
            SessionImpl back1 = null;
            try
            {
               back1 = (SessionImpl) restoredRepository.login(credentials, wsName);
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

   public void testWorkspaceRestoreWithNullConfig() throws Exception
   {
      // backup
      File backDir = new File("target/backup/" + getUUIndex());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repositoryNameToBackup);
      config.setWorkspace(workspaceNameToBackup);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      BackupChain bch = backup.findBackup(repositoryNameToBackup, workspaceNameToBackup);

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

      //TODO
      removeWorkspaceFullySingleDB(repositoryNameToBackup, workspaceNameToBackup);

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         BackupChainLog bchLog = new BackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         try
         {
            backup.restore(bchLog, repositoryNameToBackup + "not_exists", null, false);
            fail("Should be throw exception WorkspaceRestoreException");
         }
         catch (WorkspaceRestoreException e)
         {
            //ok
         }

         backup.restore(bchLog, repositoryNameToBackup, null, false);

         // check
         SessionImpl back1 = null;
         try
         {
            back1 = (SessionImpl) getReposityToBackup().login(credentials, workspaceNameToBackup);
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

   public void testExistedWorkspaceRestoreWithConfigBackupSetDir() throws Exception
   {
      // backup
      File backDir = new File("target/backup/" + getUUIndex());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repositoryNameToBackup);
      config.setWorkspace(workspaceNameToBackup);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      BackupChain bch = backup.findBackup(repositoryNameToBackup, workspaceNameToBackup);

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

      super.tearDown();

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         BackupChainLog bchLog = new BackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restoreExistingWorkspace(bchLog.getBackupConfig().getBackupDir(), false);

         // check
         SessionImpl back1 = null;
         try
         {
            back1 = (SessionImpl) getReposityToBackup().login(credentials, workspaceNameToBackup);
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

   public void testExistedRepositoryRestoreWithConfigBackupSetDir() throws Exception
   {
      // backup
      File backDir = new File("target/backup/" + getUUIndex());
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repositoryNameToBackup);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      RepositoryBackupChain bch = backup.findRepositoryBackup(repositoryNameToBackup);

      // wait till full backup will be stopped
      while (bch.getState() != RepositoryBackupChain.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // stop fullBackup
      backup.stopBackup(bch);

      // check
      super.tearDown();

      // restore             
      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restoreExistingRepository(bchLog.getBackupConfig().getBackupDir(), false);

         assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, backup.getLastRepositoryRestore(repositoryNameToBackup)
                  .getStateRestore());

         // check
         ManageableRepository restoredRepository = repositoryService.getRepository(repositoryNameToBackup);

         for (String wsName : restoredRepository.getWorkspaceNames())
         {
            SessionImpl back1 = null;
            try
            {
               back1 = (SessionImpl) restoredRepository.login(credentials, wsName);
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

   public void testWorkspaceRestoreWithConfigBackupSetDir() throws Exception
   {
      // backup
      File backDir = new File((File.createTempFile("12123", "123")).getParent() + File.separator + getUUIndex());
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repositoryNameToBackup);
      config.setWorkspace(workspaceNameToBackup);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      BackupChain bch = backup.findBackup(repositoryNameToBackup, workspaceNameToBackup);

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

      //TODO
      removeWorkspaceFullySingleDB(repositoryNameToBackup, workspaceNameToBackup);

      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         BackupChainLog bchLog = new BackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restoreWorkspace(bchLog.getBackupConfig().getBackupDir(), false);

         // check
         SessionImpl back1 = null;
         try
         {
            back1 = (SessionImpl) getReposityToBackup().login(credentials, "ws1");
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

   public void testRepositoryRestoreWithConfigBackupSetDir() throws Exception
   {
      // backup
      File backDir = new File((File.createTempFile("12123", "123")).getParent() + File.separator + getUUIndex());
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repositoryNameToBackup);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);

      config.setBackupDir(backDir);

      backup.startBackup(config);

      RepositoryBackupChain bch = backup.findRepositoryBackup(repositoryNameToBackup);

      // wait till full backup will be stopped
      while (bch.getState() != RepositoryBackupChain.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // stop fullBackup

      backup.stopBackup(bch);

      //TODO
      super.tearDown();
      removeRepositoryFully(repositoryNameToBackup);

      // restore             
      File backLog = new File(bch.getLogFilePath());
      if (backLog.exists())
      {
         RepositoryBackupChainLog bchLog = new RepositoryBackupChainLog(backLog);

         assertNotNull(bchLog.getStartedTime());
         assertNotNull(bchLog.getFinishedTime());

         backup.restoreRepository(bchLog.getBackupConfig().getBackupDir(), false);

         assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, backup.getLastRepositoryRestore(repositoryNameToBackup)
                  .getStateRestore());

         // check
         ManageableRepository restoredRepository = repositoryService.getRepository(repositoryNameToBackup);

         for (String wsName : restoredRepository.getWorkspaceNames())
         {
            SessionImpl back1 = null;
            try
            {
               back1 = (SessionImpl) restoredRepository.login(credentials, wsName);
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
}
