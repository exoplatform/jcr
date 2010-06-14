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
import org.exoplatform.services.jcr.impl.core.value.BinaryValue;
import org.exoplatform.services.jcr.impl.core.value.BooleanValue;
import org.exoplatform.services.jcr.impl.core.value.DoubleValue;
import org.exoplatform.services.jcr.impl.core.value.LongValue;
import org.exoplatform.services.jcr.impl.core.value.StringValue;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.impl.util.io.PrivilegedFileHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: TestValueImpl.java 34801 2009-07-31 15:44:50Z dkatayev $
 */

public class TestValueImpl extends TestCase
{

   private File tempDirectory = new File(System.getProperty("java.io.tmpdir"));

   private int maxFufferSize = 10;

   public void testNewBinaryValue() throws Exception
   {

      byte[] buf = "012345678901234567890123456789".getBytes();
      File file = new File("target/testNewBinaryValue");
      if (PrivilegedFileHelper.exists(file))
         file.delete();
      FileOutputStream out = PrivilegedFileHelper.fileOutputStream(file);
      out.write(buf);
      out.close();

      FileInputStream fs1 = PrivilegedFileHelper.fileInputStream(file);
      BinaryValue val = new BinaryValue(fs1, new FileCleaner(), tempDirectory, maxFufferSize);
      InputStream str1 = val.getStream();
      assertNotNull(str1);

      // obj returned by getStream() is not the same as incoming stream
      assertNotSame(str1, fs1);

      // streams returned by subsequent call of val.getStream() are equals
      assertEquals(str1, val.getStream());

      // another one value using the same string
      BinaryValue val2 = new BinaryValue(fs1, new FileCleaner(), tempDirectory, maxFufferSize);
      InputStream str2 = val2.getStream();

      // are not the same although created from same Stream
      assertNotSame(str1, str2);

      // stream already consumed
      try
      {
         val.getString();
         fail("IllegalStateException should have been thrown");
      }
      catch (IllegalStateException e)
      {
      }
      // System.out.println(" >>>>>>>>STRING >>> "+);
   }

   public void testNewBinaryValueFromString() throws Exception
   {

      BinaryValue val = new BinaryValue("string");
      InputStream str1 = val.getStream();
      assertNotNull(str1);
      // stream already consumed
      try
      {
         val.getString();
         fail("IllegalStateException should have been thrown");
      }
      catch (IllegalStateException e)
      {
      }

      BinaryValue val2 = new BinaryValue("stream");
      String str2 = val2.getString();
      assertNotNull(str2);
      // string already consumed
      try
      {
         val2.getStream();
         fail("IllegalStateException should have been thrown");
      }
      catch (IllegalStateException e)
      {
      }
   }

   public void testNewStringValue() throws Exception
   {
      StringValue sv = new StringValue("string");
      assertEquals(6, sv.getLength());
      assertEquals("string", sv.getString());
      // string already consumed
      try
      {
         sv.getStream();
         fail("IllegalStateException should have been thrown");
      }
      catch (IllegalStateException e)
      {
      }

      // default encoded string (utf-8)
      StringValue sv2 = new StringValue("����");
      // assertEquals(8, sv2.getLength());
      sv2.getStream();

   }

   public void testNewBooleanValue() throws Exception
   {
      BooleanValue bv = new BooleanValue(true);
      assertTrue(bv.getBoolean());
   }

   //
   // public void testNewDateValueData() throws Exception {
   //    
   // Calendar cal = Calendar.getInstance();
   // long time = cal.getTimeInMillis();
   // //String str = ISO8601.format(cal);
   // TransientValueData vd = new TransientValueData(cal);
   // /// ????????????????
   // //assertEquals(time, ISO8601.parse(new String(vd.getAsByteArray())).getTimeInMillis());
   // fail("How to compare Date Value ?????");
   //    
   // }
   //
   public void testNewDoubleValueData() throws Exception
   {
      DoubleValue dv = new DoubleValue(3.14);
      assertEquals(3.14, dv.getDouble());
   }

   public void testNewLongValueData() throws Exception
   {
      LongValue lv = new LongValue(314);
      assertEquals(314, lv.getLong());
   }

   // public void testNewPathValueData() throws Exception {
   // InternalQPath path = InternalQPath.parse("[]:1[]test:1");
   // PathValue pv = new PathValue(path);
   // assertEquals(path, pv.getPath());
   // }
   //  
   // public void testNewNameValueData() throws Exception {
   // InternalQName name = InternalQName.parse("[]test");
   // NameValue nv = new NameValue(name, );
   // assertEquals(name, nv.getQName());
   // }

   public void testNewUuidValueData() throws Exception
   {

      TransientValueData vd = new TransientValueData(new Identifier("1234"));
      assertEquals("1234", new String(vd.getAsByteArray()));
   }

}
