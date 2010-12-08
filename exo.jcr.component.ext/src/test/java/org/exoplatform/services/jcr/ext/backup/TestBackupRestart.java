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

import java.io.File;
import java.util.Calendar;
import java.util.Date;

import org.exoplatform.services.jcr.ext.backup.impl.BackupScheduler;

/**
 * Created by The eXo Platform SAS
 * 
 * Date: 27.02.2008
 * 
 * TODO Test should be run twice to check restored task 1. testPeriodicSchedulerPrepare() and stop
 * 2. restart and run testPeriodicSchedulerRestore()
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestBackupRestart.java 11395 2008-02-27 16:19:37Z pnedonosko $
 */
public class TestBackupRestart extends AbstractBackupTestCase
{

   @Override
   protected void tearDown() throws Exception
   {
      // empty to be able work after the JVM restart
   }

   protected ExtendedBackupManager getBackupManager()
   {
      return (ExtendedBackupManager) container.getComponentInstanceOfType(BackupManager.class);
   }

   /**
    * 4. startTime, endTime + incrementalPeriod - run during a given period (with incremental backup)
    */
   public void _testPeriodicSchedulerPrepare() throws Exception
   {
      Date startTime;
      Date stopTime;

      Calendar calendar = Calendar.getInstance();
      calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) + 10);
      startTime = calendar.getTime();

      calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) + 10); // 10 min to stop
      stopTime = calendar.getTime();

      File backDir = new File("target/backup/ws1.restored");
      backDir.mkdirs();

      BackupConfig config = new BackupConfig();
      config.setRepository(repository.getName());
      config.setWorkspace("ws1");
      config.setBackupType(BackupManager.FULL_AND_INCREMENTAL);
      config.setIncrementalJobPeriod(2 * 60); // incrementalPeriod = 2 min
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

      // play with incremental during 50 sec
      addContent(ws1TestRoot, 1, 50, 1000);

      // stop to be restarted
      Thread.sleep(1000);
   }

   public void _testPeriodicSchedulerRestore() throws Exception
   {
      BackupChain bch = backup.getCurrentBackups().iterator().next();
      File backDir = bch.getBackupConfig().getBackupDir();

      // wait till backup will be stopped
      while (!backup.getCurrentBackups().isEmpty())
      {
         Thread.yield();
         Thread.sleep(100);
      }

      // restore
      restoreAndCheck("ws1back.restored", "jdbcjcr9", bch.getLogFilePath(), backDir, 1, 50);
   }

}
