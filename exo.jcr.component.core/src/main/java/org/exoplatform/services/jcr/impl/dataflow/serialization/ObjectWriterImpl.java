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

import org.exoplatform.services.jcr.dataflow.serialization.ObjectWriter;
import org.exoplatform.services.jcr.dataflow.serialization.SerializationConstants;
import org.exoplatform.services.jcr.impl.Constants;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

/**
 * Created by The eXo Platform SAS. <br/>
 * Date: 13.02.2009
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: JCRObjectOutputImpl.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class ObjectWriterImpl implements ObjectWriter
{

   /**
    * The output stream.
    */
   private final OutputStream out;

   /**
    * File output stream. Can be null.
    */
   private final FileOutputStream fileOut;

   /**
    * JCR ObjectOutputImpl constructor.
    * 
    * @param out
    *          the OutputStream.
    */
   public ObjectWriterImpl(OutputStream out)
   {
      this.out = new BufferedOutputStream(out, SerializationConstants.INTERNAL_BUFFER_SIZE);

      if (out instanceof FileOutputStream)
         this.fileOut = (FileOutputStream)out;
      else
         this.fileOut = null;
   }

   /**
    * {@inheritDoc}
    */
   public void close() throws IOException
   {
      flush();
      out.close();
   }

   /**
    * {@inheritDoc}
    */
   public void flush() throws IOException
   {
      out.flush();
   }

   /**
    * {@inheritDoc}
    */
   public void write(byte[] b) throws IOException
   {
      out.write(b);
   }

   /**
    * {@inheritDoc}
    */
   public void write(byte[] b, int off, int len) throws IOException
   {
      out.write(b, off, len);
   }

   /**
    * {@inheritDoc}
    */
   public void writeBoolean(boolean v) throws IOException
   {
      out.write(v ? 1 : 0);
   }

   /**
    * {@inheritDoc}
    */
   public void writeByte(byte b) throws IOException
   {
      out.write(b);
   }

   /**
    * {@inheritDoc}
    */
   public void writeInt(int v) throws IOException
   {
      out.write((v >>> 24) & 0xFF);
      out.write((v >>> 16) & 0xFF);
      out.write((v >>> 8) & 0xFF);
      out.write((v >>> 0) & 0xFF);
   }

   /**
    * {@inheritDoc}
    */
   public void writeLong(long v) throws IOException
   {

      byte[] writeBuffer = new byte[8];
      writeBuffer[0] = (byte)(v >>> 56);
      writeBuffer[1] = (byte)(v >>> 48);
      writeBuffer[2] = (byte)(v >>> 40);
      writeBuffer[3] = (byte)(v >>> 32);
      writeBuffer[4] = (byte)(v >>> 24);
      writeBuffer[5] = (byte)(v >>> 16);
      writeBuffer[6] = (byte)(v >>> 8);
      writeBuffer[7] = (byte)(v >>> 0);
      out.write(writeBuffer, 0, 8);
   }

   /**
    * {@inheritDoc}
    */
   public void writeString(String str) throws IOException
   {
      byte[] bytes = str.getBytes(Constants.DEFAULT_ENCODING);
      writeInt(bytes.length);
      write(bytes);
   }

   /**
    * {@inheritDoc}
    */
   public synchronized void writeStream(InputStream stream) throws IOException
   {

      if (true)
         throw new IOException("Not implemented");

      if (fileOut != null && stream instanceof FileInputStream)
      {
         out.flush(); // flush buffer

         // and use NIO on original stream for transfer
         FileChannel fin = ((FileInputStream)stream).getChannel();
         fileOut.getChannel().transferFrom(fin, 0, fin.size());
      }
      else
      {
         // bytes copy

         // choose which kind of stream to use
         // if input stream contains enough available bytes we think it's large content - use fileOut
         // if not - use buffered write
         OutputStream writeOut;

         if (fileOut != null && stream.available() >= SerializationConstants.INTERNAL_BUFFER_SIZE)
         {
            out.flush(); // flush buffer before the write
            writeOut = fileOut; // and use File stream
         }
         else
            writeOut = this.out;

         byte[] buf = new byte[SerializationConstants.INTERNAL_BUFFER_SIZE];
         int r;
         while ((r = stream.read(buf)) <= 0)
            writeOut.write(buf, 0, r);
      }
   }
}
