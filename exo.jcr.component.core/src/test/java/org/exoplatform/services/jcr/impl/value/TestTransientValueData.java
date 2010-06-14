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
package org.exoplatform.services.jcr.impl.value;

import junit.framework.TestCase;

import org.exoplatform.services.jcr.datamodel.Identifier;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.util.JCRDateFormat;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.impl.util.io.PrivilegedFileHelper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Calendar;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: TestTransientValueData.java 34801 2009-07-31 15:44:50Z dkatayev $
 */

public class TestTransientValueData extends TestCase
{

   public void testCreateByteArrayTransientValueData() throws Exception
   {

      // byte[] buf = "0123456789".getBytes();
      TransientValueData vd = new TransientValueData("0123456789");
      // normal
      assertTrue(vd.isByteArray());
      assertEquals(10, vd.getLength());
      assertEquals(0, vd.getOrderNumber());
      assertEquals(10, vd.getAsByteArray().length);

      // as stream
      assertTrue(vd.getAsStream() instanceof ByteArrayInputStream);
      assertTrue(vd.isByteArray());

      // no spool file as spooled to byte array
      assertNull(vd.getSpoolFile());

   }

   public void testCreateFileStreamTransientValueData() throws Exception
   {

      byte[] buf = "0123456789".getBytes();
      File file = new File("target/testCreateFileStreamTransientValueData");
      if (PrivilegedFileHelper.exists(file))
         file.delete();
      FileOutputStream out = PrivilegedFileHelper.fileOutputStream(file);
      out.write(buf);
      out.close();

      FileInputStream fs1 = PrivilegedFileHelper.fileInputStream(file);
      TransientValueData vd =
         new TransientValueData(0, null, fs1, null, new FileCleaner(), 5, new File("target"), true);

      // spool to file
      InputStream fs2 = vd.getAsStream();
      assertEquals(10, vd.getLength());
      assertTrue(fs2 instanceof FileInputStream);

      // not the same object as new is is from spool file
      assertNotSame(fs1, fs2);
      // spooled to file so not a byte array
      assertFalse(vd.isByteArray());

      // next call return not the same object as well
      // (new stream every time)
      assertNotSame(vd.getAsStream(), fs2);
      assertEquals(10, vd.getLength());

      // gets as byte array
      assertEquals(10, vd.getAsByteArray().length);
      // but still spooled to file
      assertFalse(vd.isByteArray());

   }

   public void testIfTransientValueDataReturnsSameBytes() throws Exception
   {
      TransientValueData vd = new TransientValueData("0123456789");

      // same bytes object
      assertSame(vd.getAsByteArray(), vd.getAsByteArray());

      // but not same stream
      assertNotSame(vd.getAsStream(), vd.getAsStream());
   }

   public void testCreateTransientValueDataFromByteArray() throws Exception
   {
      // byte[] buf = "0123456789".getBytes();
      TransientValueData vd = new TransientValueData("0123456789");
      // TODO not influenced here as will be spooled to byte array anyway
      //vd.setMaxBufferSize(5);
      //vd.setFileCleaner(new FileCleaner());

      //
      InputStream fs2 = vd.getAsStream();
      assertEquals(10, vd.getLength());
      assertTrue(fs2 instanceof ByteArrayInputStream);
   }

   public void testNewStringValueData() throws Exception
   {
      TransientValueData vd = new TransientValueData("string");
      assertEquals(6, vd.getLength());
      assertEquals("string", new String(vd.getAsByteArray()));

      // default encoded string (utf-8)
      vd = new TransientValueData("H\u0158l\u1e37o \u1e84\u00F6r\u013b\u01fc");
      assertEquals(19, vd.getLength());
   }

   public void testNewBooleanValueData() throws Exception
   {
      TransientValueData vd = new TransientValueData(true);
      assertEquals("true", new String(vd.getAsByteArray()));
   }

   public void testNewDateValueData() throws Exception
   {

      Calendar cal = Calendar.getInstance();
      long time = cal.getTimeInMillis();
      TransientValueData vd = new TransientValueData(cal);
      assertEquals(time, new JCRDateFormat().deserialize(new String(vd.getAsByteArray())).getTimeInMillis());
   }

   public void testNewDoubleValueData() throws Exception
   {
      TransientValueData vd = new TransientValueData(3.14);
      assertEquals("3.14", new String(vd.getAsByteArray()));
   }

   public void testNewLongValueData() throws Exception
   {
      TransientValueData vd = new TransientValueData(314);
      assertEquals("314", new String(vd.getAsByteArray()));
   }

   public void testNewPathValueData() throws Exception
   {
      QPath path = QPath.parse("[]:1[]test:1");
      TransientValueData vd = new TransientValueData(path);
      assertEquals(path, QPath.parse(new String(vd.getAsByteArray())));
   }

   public void testNewNameValueData() throws Exception
   {
      InternalQName name = InternalQName.parse("[]test");
      TransientValueData vd = new TransientValueData(name);
      assertEquals(name, InternalQName.parse(new String(vd.getAsByteArray())));
   }

   public void testNewUuidValueData() throws Exception
   {

      TransientValueData vd = new TransientValueData(new Identifier("1234"));
      assertEquals("1234", new String(vd.getAsByteArray()));
   }

}
