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

import junit.framework.TestCase;

import org.exoplatform.services.jcr.ext.backup.impl.FileNameProducer;

import java.io.File;
import java.util.Calendar;

/**
 * Created by The eXo Platform SARL Author : Alex Reshetnyak alex.reshetnyak@exoplatform.com.ua
 * reshetnyak.alex@exoplatform.com.ua Nov 20, 2007
 */
public class TestFileNameProduser extends TestCase
{
   private File tempDir;

   private String backupsetName;

   private Calendar calendar;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      backupsetName = String.valueOf(System.currentTimeMillis());
      calendar = Calendar.getInstance();
   }

   public void testGetNextNameJCRBackup() throws Exception
   {
      tempDir = new File("target" + File.separator + "temp" + File.separator + "fileProduser1");
      tempDir.mkdirs();

      FileNameProducer nameProducer =
         new FileNameProducer(backupsetName, tempDir.getAbsolutePath(), calendar, true, false);
      File file = nameProducer.getNextFile();

      assertTrue(file.isFile());
      assertTrue(file.getName().endsWith(".0"));

      nameProducer = new FileNameProducer(backupsetName, tempDir.getAbsolutePath(), calendar, false, false);
      file = nameProducer.getNextFile();

      assertTrue(file.isFile());
      assertTrue(file.getName().endsWith(".1"));
      assertTrue(nameProducer.getNextFile().getName().endsWith(".2"));
      assertTrue(nameProducer.getNextFile().getName().endsWith(".3"));
   }

   public void testGetNextNameRDBMSBackup() throws Exception
   {
      tempDir = new File("target" + File.separator + "temp" + File.separator + "fileProduser2");
      tempDir.mkdirs();

      FileNameProducer nameProducer =
         new FileNameProducer(backupsetName, tempDir.getAbsolutePath(), calendar, true, true);
      File file = nameProducer.getNextFile();

      assertTrue(file.isDirectory());
      assertTrue(file.getName().endsWith(".0"));

      nameProducer = new FileNameProducer(backupsetName, tempDir.getAbsolutePath(), calendar, false, false);
      file = nameProducer.getNextFile();

      assertTrue(file.isFile());
      assertTrue(file.getName().endsWith(".1"));
      assertTrue(nameProducer.getNextFile().getName().endsWith(".2"));
      assertTrue(nameProducer.getNextFile().getName().endsWith(".3"));
   }
}
