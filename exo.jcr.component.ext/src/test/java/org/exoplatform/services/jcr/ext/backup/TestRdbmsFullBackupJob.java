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
import org.exoplatform.services.jcr.impl.core.query.SystemSearchManager;

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

      File valuesDir = new File(url.getFile(), "values");
      assertTrue(valuesDir.exists());
      String values[] = valuesDir.list();

      assertEquals(values.length, 1);
      assertTrue(new File(valuesDir, values[0]).isDirectory());

      File indexesDir = new File(url.getFile(), "index");
      assertTrue(indexesDir.exists());

      indexesDir = new File(url.getFile(), "index" + "_" + SystemSearchManager.INDEX_DIR_SUFFIX);
      assertTrue(indexesDir.exists());

      assertTrue(new File(url.getFile(), "JCR_MITEM.dump").exists());
      assertTrue(new File(url.getFile(), "JCR_MITEM.len").exists());

      assertTrue(new File(url.getFile(), "JCR_MVALUE.dump").exists());
      assertTrue(new File(url.getFile(), "JCR_MVALUE.len").exists());

      assertTrue(new File(url.getFile(), "JCR_MREF.dump").exists());
      assertTrue(new File(url.getFile(), "JCR_MREF.len").exists());

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

      File valuesDir = new File(url.getFile(), "values");
      assertFalse(valuesDir.exists());

      File indexesDir = new File(url.getFile(), "index");
      assertTrue(indexesDir.exists());

      indexesDir = new File(url.getFile(), "index" + "_" + SystemSearchManager.INDEX_DIR_SUFFIX);
      assertFalse(indexesDir.exists());

      assertTrue(new File(url.getFile(), "JCR_MITEM.dump").exists());
      assertTrue(new File(url.getFile(), "JCR_MITEM.len").exists());

      assertTrue(new File(url.getFile(), "JCR_MVALUE.dump").exists());
      assertTrue(new File(url.getFile(), "JCR_MVALUE.len").exists());

      assertTrue(new File(url.getFile(), "JCR_MREF.dump").exists());
      assertTrue(new File(url.getFile(), "JCR_MREF.len").exists());

   }
}
