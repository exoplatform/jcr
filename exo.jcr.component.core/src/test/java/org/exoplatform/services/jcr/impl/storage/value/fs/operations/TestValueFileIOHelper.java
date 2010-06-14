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
package org.exoplatform.services.jcr.impl.storage.value.fs.operations;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.dataflow.serialization.ObjectReader;
import org.exoplatform.services.jcr.dataflow.serialization.ObjectWriter;
import org.exoplatform.services.jcr.impl.dataflow.serialization.ObjectReaderImpl;
import org.exoplatform.services.jcr.impl.dataflow.serialization.ObjectWriterImpl;
import org.exoplatform.services.jcr.impl.util.io.PrivilegedFileHelper;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date: 28.05.2009
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id$
 */
public class TestValueFileIOHelper extends JcrImplBaseTest
{

   private final static int BLOCK_COUNT = 5000;

   private ValueFileIOHelper io;

   private File testDir;

   private File dest;

   private static File src;

   private static File srcSerialization;

   private ByteBuffer buf4 = ByteBuffer.allocate(4);

   private ByteBuffer buf8 = ByteBuffer.allocate(8);

   private ByteBuffer buf40 = ByteBuffer.allocate(40);

   private ByteBuffer buf2048 = ByteBuffer.allocate(2048);

   private byte buf[] = new byte[2048];

   private String str = new String("0123456789012345678901234567890123456789");

   private FileChannel infch;

   private FileChannel outfch;

   private ReadableByteChannel inch;

   private WritableByteChannel outch;

   private boolean inFile;

   private boolean outFile;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      io = new ValueFileIOHelper();

      if (src == null || !src.exists())
      {
         src = createBLOBTempFile(7 * 1024); // 7M
         src.deleteOnExit();
      }

      if (srcSerialization == null || !srcSerialization.exists())
      {
         srcSerialization = File.createTempFile("srcSerialization", ".tmp");
         srcSerialization.deleteOnExit();

         OutputStream out = PrivilegedFileHelper.fileOutputStream(srcSerialization);
         ObjectWriter ow = new ObjectWriterImpl(out);
         for (int i = 0; i < BLOCK_COUNT; i++)
         {
            ow.writeInt(1024);
            ow.writeInt(1024);
            ow.writeInt(1024);
            ow.writeLong(1024);
            ow.writeLong(1024);
            ow.writeLong(1024);
            ow.writeString(str);
            ow.writeString(str);
            ow.write(buf);
         }

         out.flush();
         out.close();
      }

      testDir = PrivilegedFileHelper.file("target/TestValueFileIOHelper");
      testDir.mkdirs();

      dest = File.createTempFile("vdftest", "", testDir);
   }

   @Override
   public void tearDown() throws Exception
   {
      dest.delete();

      super.tearDown();
   }

   public void testCopySerialization() throws Exception
   {

      System.out.println("=== test Serialization, file size:  " + srcSerialization.length());

      // copy via InputStream
      long start = System.currentTimeMillis();

      InputStream in = PrivilegedFileHelper.fileInputStream(srcSerialization);
      OutputStream out = PrivilegedFileHelper.fileOutputStream(dest);

      ObjectReader or = new ObjectReaderImpl(in);
      ObjectWriter ow = new ObjectWriterImpl(out);

      for (int i = 0; i < BLOCK_COUNT; i++)
      {
         ow.writeInt(or.readInt());
         ow.writeInt(or.readInt());
         ow.writeInt(or.readInt());
         ow.writeLong(or.readLong());
         ow.writeLong(or.readLong());
         ow.writeLong(or.readLong());
         ow.writeString(or.readString());
         ow.writeString(or.readString());
         or.readFully(buf);
         ow.write(buf);
      }

      in.close();
      out.flush();
      out.close();

      // print time
      System.out.println("\t=== IO time  " + (System.currentTimeMillis() - start));

      // clean and recreate file
      dest.delete();
      dest = File.createTempFile("vdftest", "", testDir);

      // copy via NIO
      start = System.currentTimeMillis();

      in = new BufferedInputStream(PrivilegedFileHelper.fileInputStream(srcSerialization));
      out = PrivilegedFileHelper.fileOutputStream(dest);
      openChannel(in, out);

      long pos = 0;
      for (int i = 0; i < BLOCK_COUNT; i++)
      {
         pos = copyBytes(pos, 4);
         pos = copyBytes(pos, 4);
         pos = copyBytes(pos, 4);
         pos = copyBytes(pos, 8);
         pos = copyBytes(pos, 8);
         pos = copyBytes(pos, 8);
         pos = copyBytes(pos, 44);
         pos = copyBytes(pos, 44);
         pos = copyBytes(pos, 2048);
      }

      in.close();
      out.close();

      System.out.println("\t=== NIO  (inFile=" + inFile + " outFile=" + outFile + ") time "
         + (System.currentTimeMillis() - start));

      // check length
      assertEquals(srcSerialization.length(), dest.length());
   }

   public void testCopyFileToFile() throws Exception
   {

      io.copyClose(PrivilegedFileHelper.fileInputStream(src), PrivilegedFileHelper.fileOutputStream(dest));

      // check length
      assertEquals(src.length(), dest.length());

      // check content
      // InputStream srcin = PrivilegedFileHelper.fileInputStream(src);
      // InputStream destin = PrivilegedFileHelper.fileInputStream(dest);
      // try {
      // compareStream(srcin, destin);
      // } finally {
      // srcin.close();
      // destin.close();
      // }
   }

   public void testCopyBytesToFile() throws Exception
   {

      System.out.println("=== test copyBytesToFile, file size:  " + src.length());

      // copy via InputStream
      long start = System.currentTimeMillis();

      InputStream in = PrivilegedFileHelper.fileInputStream(src);
      // InputStream in = new URL("http://jboss1.exoua-int:8089/browser/02.zip").openStream();
      OutputStream out = PrivilegedFileHelper.fileOutputStream(dest);
      try
      {
         int r = 0;
         byte[] buff = new byte[ValueFileIOHelper.IOBUFFER_SIZE];
         while ((r = in.read(buff)) >= 0)
         {
            out.write(buff, 0, r);
         }
         out.flush();
      }
      finally
      {
         in.close();
         out.close();
      }
      // print time
      System.out.println("\t=== IO time  " + (System.currentTimeMillis() - start));

      // clean and recreate file
      dest.delete();
      dest = File.createTempFile("vdftest", "", testDir);

      // copy via NIO
      start = System.currentTimeMillis();
      io.copyClose(new BufferedInputStream(PrivilegedFileHelper.fileInputStream(src)), PrivilegedFileHelper.fileOutputStream(dest));
      // io.copyClose(new URL("http://jboss1.exoua-int:8089/browser/02.zip").openStream(), new
      // FileOutputStream(dest));
      System.out.println("\t=== NIO time " + (System.currentTimeMillis() - start));

      // check length
      assertEquals(src.length(), dest.length());

      // check content
      // InputStream srcin = PrivilegedFileHelper.fileInputStream(src);
      // InputStream destin = PrivilegedFileHelper.fileInputStream(dest);
      // try {
      // compareStream(srcin, destin);
      // } finally {
      // srcin.close();
      // destin.close();
      // }
   }

   /**
    * Open channels.
    */
   private void openChannel(InputStream in, OutputStream out)
   {
      inFile = in instanceof FileInputStream && FileInputStream.class.equals(in.getClass());
      outFile = out instanceof FileOutputStream && FileOutputStream.class.equals(out.getClass());

      if (inFile && outFile)
      {
         // it's user file
         infch = ((FileInputStream)in).getChannel();
         outfch = ((FileOutputStream)out).getChannel();
      }
      else
      {
         inch = inFile ? ((FileInputStream)in).getChannel() : Channels.newChannel(in);
         outch = outFile ? ((FileOutputStream)out).getChannel() : Channels.newChannel(out);
      }
   }

   private long copyBytes(long pos, int len) throws IOException
   {
      if (inFile && outFile)
      {
         long size = outfch.transferFrom(infch, pos, len);
         return pos + size;
      }
      else
      {
         long size = 0;
         int r = 0;

         ByteBuffer buff = ByteBuffer.allocate(len);
         buff.clear();
         if ((r = inch.read(buff)) >= 0)
         {
            buff.flip();

            // copy all
            do
            {
               outch.write(buff);
            }
            while (buff.hasRemaining());

            size += r;
         }
         return pos + size;
      }
   }
}
