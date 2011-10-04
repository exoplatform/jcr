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

import org.exoplatform.services.jcr.ext.backup.impl.rdbms.FullBackupJob;

import java.io.File;
import java.net.URL;
import java.util.Calendar;

/**
 * @author <a href="mailto:anatoliy.bazko@gmail.com">Anatoliy Bazko</a>
 * @version $Id: TestFullBackupJob.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class TestRdbmsFullBackupJob
   extends BaseRDBMSBackupTest
{

   public void testRDBMSFullBackupJobSystemWorkspace() throws Exception
   {
      FullBackupJob job = new FullBackupJob();
      BackupConfig config = new BackupConfig();
      config.setRepository("db1");
      config.setWorkspace("ws");
      config.setBackupDir(new File("target/backup/testJob"));

      Calendar calendar = Calendar.getInstance();

      job.init(repositoryService.getRepository("db1"), "ws", config, calendar);
      job.run();

      URL url = job.getStorageURL();
      assertNotNull(url);

      assertTrue(new File(url.getFile(), "values-draft.zip").exists());
      assertTrue(new File(url.getFile(), "index.zip").exists());
      assertTrue(new File(url.getFile(), "dump.zip").exists());
      assertTrue(new File(url.getFile(), "dump-len.zip").exists());
   }

   public void testRDBMSFullBackupJob() throws Exception
   {
      FullBackupJob job = new FullBackupJob();
      BackupConfig config = new BackupConfig();
      config.setRepository("db1");
      config.setWorkspace("ws1");
      config.setBackupDir(new File("target/backup/testJob"));
      
      Calendar calendar = Calendar.getInstance();

      job.init(repositoryService.getRepository("db1"), "ws1", config, calendar);
      job.run();

      URL url = job.getStorageURL();
      assertNotNull(url);

      assertFalse(new File(url.getFile(), "values-draft.zip").exists());
      assertTrue(new File(url.getFile(), "index.zip").exists());
      assertFalse(new File(url.getFile(), "index_system.zip").exists());
      assertTrue(new File(url.getFile(), "dump.zip").exists());
      assertTrue(new File(url.getFile(), "dump-len.zip").exists());
   }
}
