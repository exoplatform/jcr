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
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.ByteArrayPersistedValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.FilePersistedValueData;
import org.exoplatform.services.jcr.impl.storage.value.ValueDataResourceHolder;
import org.exoplatform.services.jcr.impl.util.io.PrivilegedFileHelper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id$
 */

public class TestFileValueIO extends TestCase
{

   static class FileValueIOUtil extends FileIOChannel
   {

      FileValueIOUtil()
      {
         super(null, null, "Test #1", new ValueDataResourceHolder());
      }

      @Override
      protected String makeFilePath(String propertyId, int orderNumber)
      {
         return null;
      }

      @Override
      protected File getFile(String propertyId, int orderNumber)
      {
         return null;
      }

      @Override
      protected File[] getFiles(String propertyId)
      {
         return null;
      }

      static public ValueData testReadValue(File file, int orderNum, int maxBufferSize) throws IOException
      {

         return new FileValueIOUtil().readValue(file, orderNum, maxBufferSize);
      }

      static public void testWriteValue(File file, ValueData value) throws IOException
      {
         new FileValueIOUtil().writeValue(file, value);
      }
   }

   public void testReadByteArrayValueData() throws Exception
   {

      byte[] buf = "0123456789".getBytes();
      File file = new File("target/testReadByteArrayValueData");
      if (PrivilegedFileHelper.exists(file))
         PrivilegedFileHelper.delete(file);
      FileOutputStream out = PrivilegedFileHelper.fileOutputStream(file);
      out.write(buf);
      out.close();

      // max buffer size = 50 - so ByteArray will be created
      ValueData vd = FileValueIOUtil.testReadValue(file, 0, 50);

      assertTrue(vd instanceof ByteArrayPersistedValueData);
      assertTrue(vd.isByteArray());
      assertEquals(10, vd.getLength());
      assertEquals(0, vd.getOrderNumber());
      assertEquals(10, vd.getAsByteArray().length);
      assertTrue(vd.getAsStream() instanceof ByteArrayInputStream);
   }

   public void testReadFileValueData() throws Exception
   {

      byte[] buf = "0123456789".getBytes();
      File file = new File("target/testReadFileValueData");
      if (PrivilegedFileHelper.exists(file))
         PrivilegedFileHelper.delete(file);
      FileOutputStream out = PrivilegedFileHelper.fileOutputStream(file);
      out.write(buf);
      out.close();

      // max buffer size = 5 - so File will be created
      ValueData vd = FileValueIOUtil.testReadValue(file, 0, 5);

      assertTrue(vd instanceof FilePersistedValueData);
      assertFalse(vd.isByteArray());
      assertEquals(10, vd.getLength());
      assertEquals(0, vd.getOrderNumber());
      try
      {
         vd.getAsByteArray();
      }
      catch (IllegalStateException e)
      {
         fail("IllegalStateException should not have been thrown!");
      }
      assertTrue(vd.getAsStream() instanceof FileInputStream);
   }

   public void testWriteFileValueData() throws Exception
   {

      byte[] buf = "0123456789".getBytes();
      File file = new File("target/testWriteFileValueData");
      if (PrivilegedFileHelper.exists(file))
         PrivilegedFileHelper.delete(file);

      TransientValueData vd = new TransientValueData(0, buf, null, null, null, 1024, null, false);

      FileValueIOUtil.testWriteValue(file, vd);

      // max buffer size = 5 - so File will be created
      ValueData vd1 = FileValueIOUtil.testReadValue(file, 0, 5);

      assertFalse(vd1.isByteArray());
      assertEquals(10, vd1.getLength());
      assertEquals(0, vd1.getOrderNumber());
      assertTrue(vd1.getAsStream() instanceof FileInputStream);
   }
}
