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
package org.exoplatform.services.jcr.impl.dataflow.serialization;

import org.exoplatform.services.jcr.dataflow.serialization.ObjectReader;
import org.exoplatform.services.jcr.dataflow.serialization.ObjectWriter;
import org.exoplatform.services.jcr.impl.util.io.PrivilegedFileHelper;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

/**
 * Created by The eXo Platform SAS. <br/>Date:
 * 
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a>
 * @version $Id: TestEOFException.java 111 2008-11-11 11:11:11Z serg $
 */
public class TestEOFException extends JcrImplSerializationBaseTest
{

   public void testReadFully() throws Exception
   {
      final byte[] buffer = createBLOBTempData(45);

      File test = PrivilegedFileHelper.createTempFile("testEOF", "");
      ObjectWriter ow = new ObjectWriterImpl(PrivilegedFileHelper.fileOutputStream(test));

      ow.write(buffer);
      ow.close();

      ObjectReader or = new ObjectReaderImpl(PrivilegedFileHelper.fileInputStream(test));

      byte[] buf = new byte[buffer.length];
      try
      {
         or.readFully(buf);
         or.readFully(buf);
         fail();
      }
      catch (EOFException e)
      {
         // ok
      }
      finally
      {
         or.close();
      }

      test.delete();
   }

   public void testReadBoolean() throws Exception
   {

      File test = PrivilegedFileHelper.createTempFile("testEOF", "");
      ObjectWriter ow = new ObjectWriterImpl(PrivilegedFileHelper.fileOutputStream(test));

      ow.writeBoolean(true);
      ow.close();

      ObjectReader or = new ObjectReaderImpl(PrivilegedFileHelper.fileInputStream(test));

      boolean b = or.readBoolean();
      try
      {
         b = or.readBoolean();
         fail();
      }
      catch (EOFException e)
      {
         // ok
      }
      finally
      {
         or.close();
      }

      test.delete();
   }

   public void testReadInt() throws Exception
   {

      File test = PrivilegedFileHelper.createTempFile("testEOF", "");
      ObjectWriter ow = new ObjectWriterImpl(PrivilegedFileHelper.fileOutputStream(test));

      ow.writeInt(24);
      ow.close();

      ObjectReader or = new ObjectReaderImpl(PrivilegedFileHelper.fileInputStream(test));

      int b = or.readInt();
      assertEquals(24, b);
      try
      {
         b = or.readInt();
         fail();
      }
      catch (EOFException e)
      {
         // ok
      }
      finally
      {
         or.close();
      }

      test.delete();
   }

   public void testReadLong() throws Exception
   {

      File test = PrivilegedFileHelper.createTempFile("testEOF", "");
      ObjectWriter ow = new ObjectWriterImpl(PrivilegedFileHelper.fileOutputStream(test));

      ow.writeLong(24);
      ow.close();

      ObjectReader or = new ObjectReaderImpl(PrivilegedFileHelper.fileInputStream(test));

      long b = or.readLong();
      assertEquals(24, b);
      try
      {
         b = or.readLong();
         fail();
      }
      catch (EOFException e)
      {
         // ok
      }
      finally
      {
         or.close();
      }

      test.delete();
   }

   protected byte[] createBLOBTempData(int size) throws IOException
   {
      byte[] data = new byte[size]; // 1Kb
      Random random = new Random();
      random.nextBytes(data);
      return data;
   }

}
