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

import org.exoplatform.services.jcr.ext.backup.impl.BackupMessagesLog;
import org.exoplatform.services.jcr.ext.backup.impl.BackupScheduler;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by The eXo Platform SAS
 * 
 * Date: 05.12.2007
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestBackupScheduler.java 760 2008-02-07 15:08:07Z pnedonosko $
 */
public class TestBackupScheduler extends AbstractBackupTestCase
{

   private static final Log log = ExoLogger.getLogger(TestBackupScheduler.class);

   class BackupWaiter implements BackupJobListener
   {

      private int jobId;

      private int jobState;

      final BackupMessagesLog errors = new BackupMessagesLog();

      private CountDownLatch latch = null;

      public void onError(BackupJob job, String message, Throwable error)
      {
         errors.addError(message, error);
      }

      public void onStateChanged(BackupJob job)
      {
         if (job.getId() == jobId && job.getState() == jobState && latch != null)
            synchronized (latch)
            {
               latch.countDown(); // release waiter
            }
      }

      /**
       * @return true - ok, false - fail (tomeout expired)
       * @throws InterruptedException
       */
      boolean await(int jobId, int jobState, long timeout) throws InterruptedException
      {

         if (latch != null)
            synchronized (latch)
            {
               latch = new CountDownLatch(1);
            }
         else
            latch = new CountDownLatch(1);

         this.jobId = jobId;
         this.jobState = jobState;
         this.errors.clear();

         if (timeout > 0)
         {
            return this.latch.await(timeout, TimeUnit.MILLISECONDS);
         }
         else
         {
            this.latch.await();
            return true;
         }
      }
   }

   /**
    * 1. startTime only - run once forever
    */
   public void testScheduler_p1() throws Exception
   {
      Date startTime;

      Calendar calendar = Calendar.getInstance();
      calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) + 10);
      startTime = calendar.getTime();

      // full backup & incremental

      File backDir = new File("target/backup/ws1.incr5");
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getName());
      config.setWorkspace("ws1");
      config.setBackupType(BackupManager.FULL_AND_INCREMENTAL);
      config.setBackupDir(backDir);

      BackupScheduler scheduler = backup.getScheduler();

      scheduler.schedule(config, startTime, null, 0, 0);

      // wait till backup will be started
      while (Calendar.getInstance().getTime().before(startTime))
         Thread.sleep(100);

      Thread.sleep(100); // to know the full is started
      BackupChain bch = backup.getCurrentBackups().iterator().next();

      // wait till full backup will be stopped
      while (bch.getFullBackupState() != BackupJob.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // play with incremental
      addContent(ws1TestRoot, 1, 20, 1);

      scheduler.unschedule(config);

      log.info("-----------------[ restore ]-------------------------");
      // restore
      restoreAndCheck("ws1back.incr5", "jdbcjcr6", bch.getLogFilePath(), backDir, 1, 20);
   }

   /**
    * 2. startTime + incrementalPeriod - run once forever (with incremental backup)
    * 
    */
   public void testScheduler_p2() throws Exception
   {
      Date startTime;

      Calendar calendar = Calendar.getInstance();
      calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) + 10);
      startTime = calendar.getTime();

      // full backup & incremental

      File backDir = new File("target/backup/ws1.incr6");
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getName());
      config.setWorkspace("ws1");
      config.setBackupType(BackupManager.FULL_AND_INCREMENTAL);
      config.setBackupDir(backDir);

      BackupScheduler scheduler = backup.getScheduler();

      scheduler.schedule(config, startTime, null, 0, 10); // 10 sec incremental period

      // wait till backup will be started
      waitTime(startTime);

      Thread.sleep(100); // to know the full is started
      BackupChain bch = backup.getCurrentBackups().iterator().next();

      // wait till full backup will be stopped
      while (bch.getFullBackupState() != BackupJob.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // play with incremental, each node will be added after 500ms
      addContent(ws1TestRoot, 1, 20, 1100); // 20 * 1100 = 220000 ms = 22sec

      scheduler.unschedule(config);

      log.info("-----------------[ restore ]-------------------------");
      // restore
      restoreAndCheck("ws1back.incr6", "jdbcjcr7", bch.getLogFilePath(), backDir, 1, 20);
   }

   /**
    * 3. startTime, endTime - run during a given period
    */
   public void testScheduler_p3() throws Exception
   {
      Date startTime;
      Date stopTime;

      Calendar calendar = Calendar.getInstance();
      calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) + 10);
      startTime = calendar.getTime();

      calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) + 40); // 30 sec to stop
      stopTime = calendar.getTime();

      // full backup & incremental forever (without log rotate)

      File backDir = new File("target/backup/ws1.incr4");
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getName());
      config.setWorkspace("ws1");
      config.setBackupType(BackupManager.FULL_AND_INCREMENTAL);
      config.setBackupDir(backDir);

      BackupScheduler scheduler = backup.getScheduler();

      scheduler.schedule(config, startTime, stopTime, 0, 0);

      // wait till backup will be started
      waitTime(startTime);

      BackupChain bch = backup.getCurrentBackups().iterator().next();

      // wait till full backup will be stopped
      while (bch.getFullBackupState() != BackupJob.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // play with incremental during 20 sec
      addContent(ws1TestRoot, 1, 20, 1000);

      // wait till backup will be stopped
      waitTime(stopTime);

      // wait a bit more
      Thread.sleep(5000);

      log.info("-----------------[ restore ]-------------------------");
      // restore
      restoreAndCheck("ws1back.incr4", "jdbcjcr8", bch.getLogFilePath(), backDir, 1, 20);
   }

   /**
    * 4. startTime, endTime + incrementalPeriod - run during a given period (with incremental backup)
    */
   public void testScheduler_p4() throws Exception
   {
      Date startTime;
      Date stopTime;

      Calendar calendar = Calendar.getInstance();
      calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) + 10);
      startTime = calendar.getTime();

      calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) + 60); // 60 sec to stop
      stopTime = calendar.getTime();

      // full backup & incremental with log rotation 20sec

      File backDir = new File("target/backup/ws1.incr3");
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getName());
      config.setWorkspace("ws1");
      config.setBackupType(BackupManager.FULL_AND_INCREMENTAL);
      config.setIncrementalJobPeriod(20); // incrementalPeriod = 20 sec*
      config.setBackupDir(backDir);

      BackupScheduler scheduler = backup.getScheduler();

      scheduler.schedule(config, startTime, stopTime, 0, 0); // incrementalPeriod = 20sec* (see
      // before)

      // wait till backup will be started
      waitTime(startTime);

      BackupChain bch = backup.getCurrentBackups().iterator().next();

      // wait till full backup will be stopped
      while (bch.getFullBackupState() != BackupJob.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // play with incremental during 50 sec
      addContent(ws1TestRoot, 1, 50, 1000);

      // wait till backup will be stopped
      waitTime(stopTime);

      // wait a bit more
      Thread.sleep(5000);

      log.info("-----------------[ restore ]-------------------------");
      // restore
      restoreAndCheck("ws1back.incr3", "jdbcjcr9", bch.getLogFilePath(), backDir, 1, 50);
   }

   /**
    * 5. startTime, endTime, chainPeriod - run periodic at given period
    * 
    * test with timing - listener to know we have jobs stared in required time
    */
   public void testScheduler_p5_timing() throws Exception
   {
      Date startTime;
      Date stopTime;

      Calendar calendar = Calendar.getInstance();
      calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) + 10);
      startTime = calendar.getTime();

      calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) + 61);
      stopTime = calendar.getTime();

      // full backup each 20sec
      File backDir = new File("target/backup/ws1.incr7");
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getName());
      config.setWorkspace("ws1");
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);;
      config.setBackupDir(backDir);

      BackupScheduler scheduler = backup.getScheduler();

      BackupWaiter waiter = new BackupWaiter();
      scheduler.schedule(config, startTime, stopTime, 20, 0, waiter); // 20 sec chain period

      // wait till backup #1 will be started
      assertTrue("Full backup #1 start expired", waiter.await(0, BackupJob.STARTING, 20500)); // 20.5sec
      // to
      // start

      BackupChain bch1 = backup.getCurrentBackups().iterator().next();
      log.info("full #1 " + bch1.getLogFilePath());

      final int nodesCount = 20;
      final int nodeTimeout = 500;
      addContent(ws1TestRoot, 11, 10 + nodesCount, nodeTimeout);

      // wait till full backup #2 will be started
      assertTrue("Full backup #2 start expired", waiter.await(0, BackupJob.STARTING, 20500)); // 20.5sec
      // for
      // next

      BackupChain bch2 = backup.getCurrentBackups().iterator().next();
      log.info("full #2 " + bch2.getLogFilePath());

      addContent(ws1TestRoot, 31, 30 + nodesCount, nodeTimeout);

      // wait till full backup #3 will be started
      assertTrue("Full backup #3 start expired", waiter.await(0, BackupJob.STARTING, 20500)); // 20.5sec
      // for
      // next

      BackupChain bch3 = backup.getCurrentBackups().iterator().next();
      log.info("full #3 " + bch3.getLogFilePath());

      // wait till backup will be stopped
      waitTime(stopTime);

      Thread.sleep(20000); // wait 20 sec if next chain was started before the stop

      log.info("-----------------[ restore #1 ]-------------------------");
      restoreAndCheck("ws1back.incr7", "jdbcjcr10", bch1.getLogFilePath(), backDir, 1, 10);

      log.info("-----------------[ restore #2 ]-------------------------");
      restoreAndCheck("ws1back.incr8", "jdbcjcr11", bch2.getLogFilePath(), backDir, 11, 30);

      log.info("-----------------[ restore #3 ]-------------------------");
      restoreAndCheck("ws1back.incr9", "jdbcjcr12", bch3.getLogFilePath(), backDir, 31, 50);
   }

   // 6. startTime, endTime, chainPeriod + incrementalPeriod - run periodic at // given period (with
   // incremental backup)
   public void testScheduler_p6() throws Exception
   {

      Date startTime;
      Date stopTime;

      Calendar calendar = Calendar.getInstance();
      calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) + 10);
      startTime = calendar.getTime();

      calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) + 55);
      stopTime = calendar.getTime();
      // full backup & incremental

      File backDir = new File("target/backup/ws1.incr10");
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getName());
      config.setWorkspace("ws1");
      config.setBackupType(BackupManager.FULL_AND_INCREMENTAL);
      config.setBackupDir(backDir);

      BackupScheduler scheduler = backup.getScheduler();

      scheduler.schedule(config, startTime, stopTime, 25, 10);

      // wait till backup will be started
      waitTime(startTime);

      BackupChain bch = backup.getCurrentBackups().iterator().next();

      // wait till full backup will be stopped
      while (bch.getFullBackupState() != BackupJob.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      BackupChain bch1 = backup.getCurrentBackups().iterator().next();
      log.info(" #1 " + bch1.getLogFilePath());
      // BackupChain bch1 = backup.findBackup(config.getRepository(), config.getWorkspace());

      // incr works, 15sec+
      addContent(ws1TestRoot, 1, 20, 750);

      // wait till next backup chain will be started
      Thread.sleep(10000);

      BackupChain bch2 = backup.getCurrentBackups().iterator().next();
      log.info(" #2 " + bch2.getLogFilePath());
      // BackupChain bch2 = backup.findBackup(config.getRepository(), config.getWorkspace());

      addContent(ws1TestRoot, 21, 40, 750);

      // wait till backup will be stopped
      waitTime(stopTime);

      Thread.sleep(25000); // wait 25 sec if next chain was started before the stop

      log.info(" restore #1 " + bch1.getLogFilePath());
      restoreAndCheck("ws1back.incr10", "jdbcjcr13", bch1.getLogFilePath(), backDir, 1, 20);

      log.info(" restore #2 " + bch2.getLogFilePath());
      restoreAndCheck("ws1back.incr11", "jdbcjcr14", bch2.getLogFilePath(), backDir, 21, 40);
   }

   /**
    * 7. startTime, chainPeriod - run periodic forever
    */
   public void testScheduler_p7() throws Exception
   {
      Date startTime;

      Calendar calendar = Calendar.getInstance();
      calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) + 10);

      startTime = calendar.getTime();
      // full backup & incremental

      File backDir = new File("target/backup/ws1.incr11");
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getName());
      config.setWorkspace("ws1");
      config.setBackupType(BackupManager.FULL_AND_INCREMENTAL);
      config.setBackupDir(backDir);

      BackupScheduler scheduler = backup.getScheduler();

      scheduler.schedule(config, startTime, null, 10, 0);

      // wait till backup will be started
      waitTime(startTime);

      BackupChain bch1 = backup.getCurrentBackups().iterator().next();
      // BackupChain bch1 = backup.findBackup(config.getRepository(), config.getWorkspace());

      addContent(ws1TestRoot, 1, 20, 10);
      // wait till next backup will be started
      Thread.sleep(11000);

      BackupChain bch2 = backup.getCurrentBackups().iterator().next();
      // BackupChain bch2 = backup.findBackup(config.getRepository(), config.getWorkspace());

      addContent(ws1TestRoot, 21, 40, 1);

      // Stop backup
      Thread.sleep(10000); // for last started chain
      scheduler.unschedule(config);

      log.info("-----------------[ restore #1 ]-------------------------");
      restoreAndCheck("ws1back.incr12", "jdbcjcr15", bch1.getLogFilePath(), backDir, 1, 20);

      log.info("-----------------[ restore #2 ]-------------------------");
      restoreAndCheck("ws1back.incr13", "jdbcjcr16", bch2.getLogFilePath(), backDir, 21, 40);
   }

   // 8. startTime, chainPeriod + incrementalPeriod - run periodic forever (with incremental backup)
   public void testScheduler_p8() throws Exception
   {

      Date startTime;

      Calendar calendar = Calendar.getInstance();
      calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) + 10);
      startTime = calendar.getTime();
      // full backup & incremental

      File backDir = new File("target/backup/ws1.incr13");
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getName());
      config.setWorkspace("ws1");
      config.setBackupType(BackupManager.FULL_AND_INCREMENTAL);
      config.setBackupDir(backDir);

      BackupScheduler scheduler = backup.getScheduler();

      scheduler.schedule(config, startTime, null, 10, 6);

      // wait till backup will be started
      waitTime(startTime);

      BackupChain bch1 = backup.getCurrentBackups().iterator().next();
      // BackupChain bch1 = backup.findBackup(config.getRepository(), config.getWorkspace());

      addContent(ws1TestRoot, 1, 20, 15);
      // wait till next backup will be started
      Thread.sleep(11000);

      BackupChain bch2 = backup.getCurrentBackups().iterator().next();
      // BackupChain bch2 = backup.findBackup(config.getRepository(), config.getWorkspace());

      addContent(ws1TestRoot, 21, 40, 15);

      // Stop backup
      Thread.sleep(10000); // for last started chain
      scheduler.unschedule(config);

      log.info("-----------------[ restore #1 ]-------------------------");
      restoreAndCheck("ws1back.incr14", "jdbcjcr17", bch1.getLogFilePath(), backDir, 1, 20);

      log.info("-----------------[ restore #2 ]-------------------------");
      restoreAndCheck("ws1back.incr15", "jdbcjcr18", bch2.getLogFilePath(), backDir, 21, 40);
   }

}
