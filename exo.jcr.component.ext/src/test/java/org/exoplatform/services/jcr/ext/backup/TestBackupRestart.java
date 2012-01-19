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
