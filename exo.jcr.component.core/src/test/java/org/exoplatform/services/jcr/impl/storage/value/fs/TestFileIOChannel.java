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
package org.exoplatform.services.jcr.impl.storage.value.fs;

import junit.framework.TestCase;

import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.dataflow.TesterTransientValueData;
import org.exoplatform.services.jcr.impl.storage.value.ValueDataResourceHolder;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id$
 */

public class TestFileIOChannel extends TestCase
{

   private File rootDir;

   private FileCleaner cleaner = new FileCleaner(2000);

   private ValueDataResourceHolder resources = new ValueDataResourceHolder();

   private TesterTransientValueData testerTransientValueData = new TesterTransientValueData();

   /**
    * {@inheritDoc}
    */
   @Override
   protected void setUp() throws Exception
   {
      super.setUp();

      rootDir = new File(new File("target"), "vs1");
      rootDir.mkdirs();

      new File(rootDir, FileValueStorage.TEMP_DIR_NAME).mkdirs();

      if (!rootDir.exists())
         throw new Exception("Folder does not exist " + rootDir.getAbsolutePath());

   }

   public void testRead() throws Exception
   {

      byte[] buf = "0123456789".getBytes();
      File file = new File(rootDir, "testReadFromIOChannel0");
      file.deleteOnExit();
      if (file.exists())
         file.delete();
      FileOutputStream out = new FileOutputStream(file);
      out.write(buf);
      out.close();

      buf = "01234567890123456789".getBytes();

      file = new File(rootDir, "testReadFromIOChannel1");
      if (file.exists())
         file.delete();
      out = new FileOutputStream(file);
      out.write(buf);
      out.close();

      FileIOChannel channel = new SimpleFileIOChannel(rootDir, cleaner, "#1", resources);

      // first value - to buffer (length`=10), second - file (length`=20)
      // List <ValueData> values = channel.read("testReadFromIOChannel", 11);

      // assertEquals(2, values.size());
      ValueData v0 = channel.read("testReadFromIOChannel", 0, 11);
      assertEquals(10, v0.getLength());
      assertEquals(0, v0.getOrderNumber());
      assertEquals(10, v0.getAsByteArray().length);
      assertTrue(v0.isByteArray());
      assertNotNull(v0.getAsStream());

      ValueData v1 = channel.read("testReadFromIOChannel", 1, 11);
      assertEquals(20, v1.getLength());
      assertEquals(1, v1.getOrderNumber());
      assertFalse(v1.isByteArray());
      assertNotNull(v1.getAsStream());

      try
      {
         v1.getAsByteArray();
      }
      catch (IllegalStateException e)
      {
         fail("IllegalStateException should not have been thrown");
      }
      channel.delete("testReadFromIOChannel");
      channel.commit();
   }

   public void testWriteAdd() throws Exception
   {
      FileIOChannel channel = new SimpleFileIOChannel(rootDir, cleaner, "#1", resources);

      byte[] buf = "0123456789".getBytes();
      List<ValueData> values = new ArrayList<ValueData>();
      values.add(testerTransientValueData.getTransientValueData(buf, 0));
      values.add(testerTransientValueData.getTransientValueData(buf, 1));
      values.add(testerTransientValueData.getTransientValueData(buf, 2));

      for (ValueData valueData : values)
      {
         channel.write("testWriteToIOChannel", valueData);
      }
      channel.commit();

      assertTrue(new File(rootDir, "testWriteToIOChannel0").exists());
      assertTrue(new File(rootDir, "testWriteToIOChannel1").exists());
      assertTrue(new File(rootDir, "testWriteToIOChannel2").exists());

      assertEquals(10, new File(rootDir, "testWriteToIOChannel0").length());

      channel.delete("testWriteToIOChannel");
      channel.commit();
      // try to read
      // values = channel.read("testWriteToIOChannel", 5);
      // assertEquals(3, values.size());
   }

   protected void writeUpdate(FileIOChannel channel) throws Exception
   {

      byte[] buf = "0123456789".getBytes();
      channel.write("testWriteUpdate", testerTransientValueData.getTransientValueData(buf, 0));
      channel.commit();

      File f = channel.getFile("testWriteUpdate", 0);
      assertTrue(f.exists());
      assertEquals(10, f.length());

      byte[] buf1 = "qwerty".getBytes();
      channel.write("testWriteUpdate", testerTransientValueData.getTransientValueData(buf1, 0));
      channel.commit();

      f = channel.getFile("testWriteUpdate", 0);
      assertTrue(f.exists());
      assertEquals(6, f.length());

      channel.delete("testWriteUpdate");
      channel.commit();
   }

   public void testWriteUpdate() throws Exception
   {
      FileIOChannel channel = new SimpleFileIOChannel(rootDir, cleaner, "#1", resources);

      writeUpdate(channel);

      channel = new TreeFileIOChannel(rootDir, cleaner, "#1", resources);

      writeUpdate(channel);
   }

   public void testDelete() throws Exception
   {
      FileIOChannel channel = new SimpleFileIOChannel(rootDir, cleaner, "#1", resources);

      byte[] buf = "0123456789".getBytes();
      List<ValueData> values = new ArrayList<ValueData>();
      values.add(testerTransientValueData.getTransientValueData(buf, 0));
      values.add(testerTransientValueData.getTransientValueData(buf, 1));
      values.add(testerTransientValueData.getTransientValueData(buf, 2));

      for (ValueData valueData : values)
      {
         channel.write("testDeleteFromIOChannel", valueData);
      }
      channel.commit();

      assertTrue(new File(rootDir, "testDeleteFromIOChannel0").exists());
      assertTrue(new File(rootDir, "testDeleteFromIOChannel1").exists());
      assertTrue(new File(rootDir, "testDeleteFromIOChannel2").exists());

      channel.delete("testDeleteFromIOChannel");
      channel.commit();

      assertFalse(new File(rootDir, "testDeleteFromIOChannel0").exists());
      assertFalse(new File(rootDir, "testDeleteFromIOChannel1").exists());
      assertFalse(new File(rootDir, "testDeleteFromIOChannel2").exists());

      channel.delete("testDeleteFromIOChannel");
      channel.commit();
      // try to read
      // values = channel.read("testDeleteFromIOChannel", 5);
      // assertEquals(0, values.size());

   }

   public void testConcurrentRead() throws Exception
   {
      FileIOChannel channel = new SimpleFileIOChannel(rootDir, cleaner, "#1", resources);

      List<ValueData> values = new ArrayList<ValueData>();
      byte[] buf = new byte[100 * 100];
      for (int i = 0; i < buf.length; i++)
      {
         buf[i] = 48;
      }

      // approx. 10Kb file
      values.add(testerTransientValueData.getTransientValueData(buf, 0));
      for (ValueData valueData : values)
      {
         channel.write("testConcurrentReadFromIOChannel", valueData);
      }
      channel.commit();

      File f = new File(rootDir, "testConcurrentReadFromIOChannel0");
      if (!f.exists())
         throw new Exception("File does not exist " + f.getAbsolutePath());

      Probe[] p = new Probe[10];
      for (int i = 0; i < 10; i++)
      {
         p[i] = new Probe(f);
         p[i].start();
      }

      // // should be enough to start read but not finish all the threads
      // Thread.sleep(100);
      // channel.delete("testConcurrentReadFromIOChannel");

      // should be enough to finish all the threads
      Thread.sleep(1000);

      for (int i = 0; i < 10; i++)
      {
         assertEquals(100 * 100, p[i].getLen());
      }
      channel.delete("testConcurrentReadFromIOChannel");
      channel.commit();
   }

   public void testDeleteLockedFile() throws Exception
   {
      FileIOChannel channel = new SimpleFileIOChannel(rootDir, cleaner, "#1", resources);

      List<ValueData> values = new ArrayList<ValueData>();
      byte[] buf = new byte[100000];
      for (int i = 0; i < buf.length; i++)
      {
         buf[i] = 48;
      }

      // approx. 1Mb file
      values.add(testerTransientValueData.getTransientValueData(buf, 0));
      for (ValueData valueData : values)
      {
         channel.write("testDeleteLockedFileFromIOChannel", valueData);
      }
      channel.commit();

      File f = new File(rootDir, "testDeleteLockedFileFromIOChannel0");
      new Probe(f).start();

      f = null;

      Thread.sleep(100);

      // removed by FileCleaner
      Thread.sleep(3000);
      f = new File(rootDir, "testDeleteLockedFileFromIOChannel0");
      // assertFalse(f.exists());
      System.out.println(">>>>>>>>>>>>>" + f.canRead() + " " + f.exists() + " " + f.canWrite());
      System.out.println(">>>>>>>>>>>>>" + new FileInputStream(f).read());

      // new Probe(f).start();
   }
}
