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
import org.exoplatform.services.jcr.impl.util.io.PrivilegedFileHelper;

import java.io.File;
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
      PrivilegedFileHelper.mkdirs(rootDir);

      PrivilegedFileHelper.mkdirs(new File(rootDir, FileValueStorage.TEMP_DIR_NAME));

      if (!PrivilegedFileHelper.exists(rootDir))
         throw new Exception("Folder does not exist " + PrivilegedFileHelper.getAbsolutePath(rootDir));

   }

   public void testRead() throws Exception
   {

      byte[] buf = "0123456789".getBytes();
      File file = new File(rootDir, "testReadFromIOChannel0");
      PrivilegedFileHelper.deleteOnExit(file);
      if (PrivilegedFileHelper.exists(file))
         PrivilegedFileHelper.delete(file);
      FileOutputStream out = PrivilegedFileHelper.fileOutputStream(file);
      out.write(buf);
      out.close();

      buf = "01234567890123456789".getBytes();

      file = new File(rootDir, "testReadFromIOChannel1");
      if (PrivilegedFileHelper.exists(file))
         PrivilegedFileHelper.delete(file);
      out = PrivilegedFileHelper.fileOutputStream(file);
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

      assertTrue(PrivilegedFileHelper.exists(new File(rootDir, "testWriteToIOChannel0")));
      assertTrue(PrivilegedFileHelper.exists(new File(rootDir, "testWriteToIOChannel1")));
      assertTrue(PrivilegedFileHelper.exists(new File(rootDir, "testWriteToIOChannel2")));

      assertEquals(10, PrivilegedFileHelper.length(new File(rootDir, "testWriteToIOChannel0")));

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
      assertTrue(PrivilegedFileHelper.exists(f));
      assertEquals(10, PrivilegedFileHelper.length(f));

      byte[] buf1 = "qwerty".getBytes();
      channel.write("testWriteUpdate", testerTransientValueData.getTransientValueData(buf1, 0));
      channel.commit();

      f = channel.getFile("testWriteUpdate", 0);
      assertTrue(PrivilegedFileHelper.exists(f));
      assertEquals(6, PrivilegedFileHelper.length(f));

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

      assertTrue(PrivilegedFileHelper.exists(new File(rootDir, "testDeleteFromIOChannel0")));
      assertTrue(PrivilegedFileHelper.exists(new File(rootDir, "testDeleteFromIOChannel1")));
      assertTrue(PrivilegedFileHelper.exists(new File(rootDir, "testDeleteFromIOChannel2")));

      channel.delete("testDeleteFromIOChannel");
      channel.commit();

      assertFalse(PrivilegedFileHelper.exists(new File(rootDir, "testDeleteFromIOChannel0")));
      assertFalse(PrivilegedFileHelper.exists(new File(rootDir, "testDeleteFromIOChannel1")));
      assertFalse(PrivilegedFileHelper.exists(new File(rootDir, "testDeleteFromIOChannel2")));

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
      if (!PrivilegedFileHelper.exists(f))
         throw new Exception("File does not exist " + PrivilegedFileHelper.getAbsolutePath(f));

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
      //Thread.sleep(1000);

      for (int i = 0; i < 10; i++)
      {
         p[i].join();
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
      Probe p = new Probe(f);
      p.start();

      p.join();
      f = null;

      // Thread.sleep(100);

      // removed by FileCleaner
      //Thread.sleep(3000);

      f = new File(rootDir, "testDeleteLockedFileFromIOChannel0");
      // assertFalse(f.exists());
      System.out.println(">>>>>>>>>>>>>" + f.canRead() + " " + PrivilegedFileHelper.exists(f) + " " + f.canWrite());
      System.out.println(">>>>>>>>>>>>>" + PrivilegedFileHelper.fileInputStream(f).read());

      // new Probe(f).start();
   }
}
