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
import org.exoplatform.services.jcr.dataflow.serialization.SerializationConstants;
import org.exoplatform.services.jcr.impl.Constants;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;

/**
 * Created by The eXo Platform SAS. <br>
 * Date: 13.02.2009
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: JCRObjectInputImpl.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class ObjectReaderImpl implements ObjectReader
{

   /**
    * Reader stream.
    */
   private InputStream in;

   /**
    * File stream. Can be null.
    */
   private final FileInputStream fileIn;

   /**
    * ObjectReaderImpl constructor.
    * 
    * @param in
    *          original InputStream
    */
   public ObjectReaderImpl(InputStream in)
   {
      this.in = new BufferedInputStream(in, SerializationConstants.INTERNAL_BUFFER_SIZE);

      if (in instanceof FileInputStream)
         this.fileIn = (FileInputStream)in;
      else
         this.fileIn = null;
   }

   /**
    * {@inheritDoc}
    */
   public void close() throws IOException
   {
      in.close();
   }

   /**
    * {@inheritDoc}
    */
   public boolean readBoolean() throws IOException
   {
      int v = in.read();
      if (v < 0)
         throw new EOFException();

      return v != 0;
   }

   /**
    * {@inheritDoc}
    */
   public byte readByte() throws IOException
   {
      int v = in.read();
      if (v < 0)
         throw new EOFException();

      return (byte)v;
   }

   /**
    * {@inheritDoc}
    */
   public void readFully(byte[] b) throws IOException
   {
      int l = in.read(b);

      if (l < 0)
         throw new EOFException();
      if (l < b.length && l > 0)
         throw new StreamCorruptedException("Unexpected EOF in middle of data block.");
   }

   /**
    * {@inheritDoc}
    */
   public synchronized long read(OutputStream stream, long length) throws IOException
   {
      if (true)
         throw new IOException("Not implemented");

      boolean recreateBuffer = true;
      try
      {
         if (fileIn != null && stream instanceof FileOutputStream)
         {
            // use NIO

            return fileIn.getChannel().transferTo(0, length, ((FileOutputStream)stream).getChannel());
         }
         else
         {
            // bytes copy

            // choose which kind of stream to use
            // if this input stream contains enough available bytes we think it's
            // large content - use
            // fileIn
            // if not - use buffered write
            InputStream readIn;

            if (fileIn != null && fileIn.available() >= SerializationConstants.INTERNAL_BUFFER_SIZE)
            {
               readIn = fileIn; // and use File stream
            }
            else
            {
               readIn = this.in;
               recreateBuffer = false;
            }

            byte[] buf = new byte[SerializationConstants.INTERNAL_BUFFER_SIZE];
            int r;
            int readed = 0;
            while ((r = readIn.read(buf)) <= 0)
            {
               stream.write(buf, 0, r);
               readed += r;
            }
            return readed;
         }
      }
      finally
      {
         if (recreateBuffer)
            // we cannot use existing buffered stream anymore, create one new
            this.in = new BufferedInputStream(in, SerializationConstants.INTERNAL_BUFFER_SIZE);
      }
   }

   /**
    * {@inheritDoc}
    */
   public int readInt() throws IOException
   {
      byte[] readBuffer = new byte[4];
      readFully(readBuffer);
      return ((readBuffer[0] & 255) << 24) + ((readBuffer[1] & 255) << 16) + ((readBuffer[2] & 255) << 8)
         + ((readBuffer[3] & 255));
   }

   /**
    * {@inheritDoc}
    */
   public long readLong() throws IOException
   {
      byte[] readBuffer = new byte[8];

      readFully(readBuffer);
      return (((long)readBuffer[0] << 56) + ((long)(readBuffer[1] & 255) << 48) + ((long)(readBuffer[2] & 255) << 40)
         + ((long)(readBuffer[3] & 255) << 32) + ((long)(readBuffer[4] & 255) << 24) + ((readBuffer[5] & 255) << 16)
         + ((readBuffer[6] & 255) << 8) + ((readBuffer[7] & 255) << 0));
   }

   /**
    * {@inheritDoc}
    */
   public long skip(long n) throws IOException
   {
      if (n <= 0)
         return 0;

      long remaining = n;
      int nr;
      byte[] skipBuffer = new byte[SerializationConstants.INTERNAL_BUFFER_SIZE];
      while (remaining > 0)
      {
         nr = in.read(skipBuffer, 0, (int)Math.min(SerializationConstants.INTERNAL_BUFFER_SIZE, remaining));
         if (nr < 0)
         {
            break;
         }
         remaining -= nr;
      }
      return n - remaining;
   }

   /**
    * {@inheritDoc}
    */
   public String readString() throws IOException
   {

      int length = readInt();
      byte[] buf = new byte[length];
      readFully(buf);
      return new String(buf, Constants.DEFAULT_ENCODING);
   }
}
