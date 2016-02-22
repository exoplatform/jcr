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

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.services.jcr.dataflow.serialization.ObjectReader;
import org.exoplatform.services.jcr.dataflow.serialization.SerializationConstants;
import org.exoplatform.services.jcr.impl.Constants;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by The eXo Platform SAS. <br>
 * Date: 15.04.2009
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: FileObjectReaderImpl.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class FileObjectReaderImpl implements ObjectReader
{

   /**
    * The file channel to reading.
    */
   private final FileChannel channel;

   /**
    * FileObjectReaderImpl constructor.
    * 
    * @param file
    *          Source file to reading
    * @throws FileNotFoundException
    *           if file does not exist
    */
   public FileObjectReaderImpl(File file) throws FileNotFoundException
   {
      this.channel = PrivilegedFileHelper.fileInputStream(file).getChannel();
   }

   /**
    * {@inheritDoc}
    */
   public void close() throws IOException
   {
      channel.close();
   }

   /**
    * Reads a sequence of bytes from file into the given buffer.
    * 
    * @param dst
    *          buffer
    * @throws IOException
    *           if any Exception is occurred
    */
   private void readFully(ByteBuffer dst) throws IOException
   {
      int r = channel.read(dst);

      if (r < 0)
         throw new EOFException();
      if (r < dst.capacity() && r > 0)
         throw new StreamCorruptedException("Unexpected EOF in middle of data block.");
   }

   /**
    * {@inheritDoc}
    */
   public boolean readBoolean() throws IOException
   {
      int v = readInt();
      if (v < 0)
         throw new EOFException();

      return v != 0;
   }

   /**
    * {@inheritDoc}
    */
   public byte readByte() throws IOException
   {

      ByteBuffer dst = ByteBuffer.allocate(1);
      readFully(dst);
      return dst.get();
   }

   /**
    * {@inheritDoc}
    */
   public void readFully(byte[] b) throws IOException
   {
      ByteBuffer dst = ByteBuffer.wrap(b);
      readFully(dst);
   }

   /**
    * {@inheritDoc}
    */
   public int readInt() throws IOException
   {
      ByteBuffer dst = ByteBuffer.allocate(4);
      readFully(dst);
      return dst.asIntBuffer().get();
   }

   /**
    * {@inheritDoc}
    */
   public long readLong() throws IOException
   {
      ByteBuffer dst = ByteBuffer.allocate(8);
      readFully(dst);
      return dst.asLongBuffer().get();
   }

   /**
    * {@inheritDoc}
    */
   public String readString() throws IOException
   {
      ByteBuffer dst = ByteBuffer.allocate(readInt());
      readFully(dst);
      return new String(dst.array(), Constants.DEFAULT_ENCODING);
   }

   /**
    * {@inheritDoc}
    */
   public long skip(long n) throws IOException
   {
      if (n <= 0)
         return 0;

      channel.position(channel.position() + n);
      return n;
   }

   /**
    * {@inheritDoc}
    */
   public long read(OutputStream stream, long length) throws IOException
   {
      if (stream instanceof FileOutputStream)
      {
         // use NIO
         return channel.transferTo(0, length, ((FileOutputStream)stream).getChannel());
      }
      else
      {
         // bytes copy
         ByteBuffer buff = ByteBuffer.allocate(SerializationConstants.INTERNAL_BUFFER_SIZE);

         int r;
         int readed = 0;
         while ((r = channel.read(buff)) <= 0)
         {
            stream.write(buff.array(), 0, r);
            buff.rewind();
            readed += r;
         }
         return readed;
      }
   }

}
