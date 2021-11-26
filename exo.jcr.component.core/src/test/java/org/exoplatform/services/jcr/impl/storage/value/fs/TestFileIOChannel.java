/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.impl.storage.value.fs;

import junit.framework.TestCase;

import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.dataflow.SpoolConfig;
import org.exoplatform.services.jcr.impl.dataflow.TesterTransientValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.ChangedSizeHandler;
import org.exoplatform.services.jcr.impl.dataflow.persistent.SimpleChangedSizeHandler;
import org.exoplatform.services.jcr.impl.storage.value.ValueDataResourceHolder;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.PropertyType;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id$
 */

public class TestFileIOChannel extends TestCase
{

   private File rootDir;

   private FileCleaner cleaner;

   private ValueDataResourceHolder resources = new ValueDataResourceHolder();

   private TesterTransientValueData testerTransientValueData = new TesterTransientValueData();

   /**
    * {@inheritDoc}
    */
   @Override
   protected void setUp() throws Exception
   {
      super.setUp();

      cleaner = new FileCleaner(2000);

      rootDir = new File(new File("target"), "vs1");
      rootDir.mkdirs();

      new File(rootDir, FileValueStorage.TEMP_DIR_NAME).mkdirs();

      if (!rootDir.exists())
      {
         throw new Exception("Folder does not exist " + rootDir.getAbsolutePath());
      }

   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void tearDown() throws Exception
   {
      cleaner.halt();
   }

   public void testRead() throws Exception
   {

      byte[] buf = "0123456789".getBytes();
      File file = new File(rootDir, "testReadFromIOChannel0");
      file.deleteOnExit();
      if (file.exists())
      {
         file.delete();
      }
      FileOutputStream out = new FileOutputStream(file);
      out.write(buf);
      out.close();

      buf = "01234567890123456789".getBytes();

      file = new File(rootDir, "testReadFromIOChannel1");
      if (file.exists())
      {
         file.delete();
      }
      out = new FileOutputStream(file);
      out.write(buf);
      out.close();

      FileIOChannel channel = new SimpleFileIOChannel(rootDir, cleaner, "#1", resources);

      // first value - to buffer (length`=10), second - file (length`=20)
      // List <ValueData> values = channel.read("testReadFromIOChannel", 11);

      // assertEquals(2, values.size());
      ValueData v0 =
         channel.read("testReadFromIOChannel", 0, PropertyType.BINARY, SpoolConfig.getDefaultSpoolConfig()).value;
      assertEquals(10, v0.getLength());
      assertEquals(0, v0.getOrderNumber());
      assertEquals(10, v0.getAsByteArray().length);
      assertTrue(v0.isByteArray());
      assertNotNull(v0.getAsStream());

      SpoolConfig spoolConfig = SpoolConfig.getDefaultSpoolConfig();
      spoolConfig.maxBufferSize = 11;

      ValueData v1 = channel.read("testReadFromIOChannel", 1, PropertyType.BINARY, spoolConfig).value;
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

      ChangedSizeHandler sizeHandler = new SimpleChangedSizeHandler();
      for (ValueData valueData : values)
      {
         channel.write("testWriteToIOChannel", valueData, sizeHandler);
      }
      channel.commit();

      assertTrue(new File(rootDir, "testWriteToIOChannel0").exists());
      assertTrue(new File(rootDir, "testWriteToIOChannel1").exists());
      assertTrue(new File(rootDir, "testWriteToIOChannel2").exists());

      assertEquals(10, new File(rootDir, "testWriteToIOChannel0").length());
      assertEquals(30, sizeHandler.getChangedSize());

      channel.delete("testWriteToIOChannel");
      channel.commit();
   }

   protected void writeUpdate(FileIOChannel channel) throws Exception
   {
      byte[] buf = "0123456789".getBytes();
      channel
.write("testWriteUpdate", testerTransientValueData.getTransientValueData(buf, 0),
         new SimpleChangedSizeHandler());
      channel.commit();

      File f = channel.getFile("testWriteUpdate", 0);
      assertTrue(f.exists());
      assertEquals(10, f.length());

      byte[] buf1 = "qwerty".getBytes();
      channel.write("testWriteUpdate", testerTransientValueData.getTransientValueData(buf1, 0),
         new SimpleChangedSizeHandler());
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
         channel.write("testDeleteFromIOChannel", valueData, new SimpleChangedSizeHandler());
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
         channel.write("testConcurrentReadFromIOChannel", valueData, new SimpleChangedSizeHandler());
      }
      channel.commit();

      File f = new File(rootDir, "testConcurrentReadFromIOChannel0");
      if (!f.exists())
      {
         throw new Exception("File does not exist " + f.getAbsolutePath());
      }

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
         channel.write("testDeleteLockedFileFromIOChannel", valueData, new SimpleChangedSizeHandler());
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
      assertTrue(f.exists());
      assertTrue(f.canRead());
      assertTrue(f.canWrite());
      assertEquals(48, new FileInputStream(f).read());

      // new Probe(f).start();
   }
}
