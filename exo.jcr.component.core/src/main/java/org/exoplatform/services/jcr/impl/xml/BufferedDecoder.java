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
package org.exoplatform.services.jcr.impl.xml;

import org.apache.ws.commons.util.Base64;
import org.exoplatform.services.jcr.impl.util.io.PrivilegedFileHelper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: BufferedDecoder.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class BufferedDecoder extends Base64.Decoder
{
   /**
    * Default buffer size.
    */
   private static final int DEFAULT_BUFFER_SIZE = 4096;

   /**
    * 
    */
   private static final int DEFAULT_READ_BUFFER_SIZE = 4096;

   /**
    * Buffer size.
    */
   private final int bufferSize;

   /**
    * Buffer file.
    */
   private File fileBuffer;

   /**
    * Output stream.
    */
   private OutputStream out;

   /**
    * Default constructor.
    */
   public BufferedDecoder()
   {
      super(DEFAULT_BUFFER_SIZE);
      this.bufferSize = DEFAULT_BUFFER_SIZE;
      this.out = new ByteArrayOutputStream(DEFAULT_BUFFER_SIZE);
   }

   /**
    * @param bufferSize
    *          - buffer size.
    */
   public BufferedDecoder(int bufferSize)
   {
      super(bufferSize);
      this.bufferSize = bufferSize;
      this.out = new ByteArrayOutputStream(DEFAULT_BUFFER_SIZE);
   }

   /**
    * @return - input stream.
    * @throws IOException
    *           - unknown output stream.
    */
   public InputStream getInputStream() throws IOException
   {
      flush();
      if (out instanceof ByteArrayOutputStream)
      {
         return new ByteArrayInputStream(((ByteArrayOutputStream)out).toByteArray());
      }
      else if (out instanceof BufferedOutputStream)
      {

         out.close();
         return new BufferedInputStream(PrivilegedFileHelper.fileInputStream(fileBuffer));
      }
      else
      {
         throw new IOException("unexpected change of buffer");
      }
   }

   /**
    * Remove buffer.
    * 
    * @throws IOException
    *           if file cannot be removed.
    */
   public void remove() throws IOException
   {
      if ((fileBuffer != null) && PrivilegedFileHelper.exists(fileBuffer))
      {
         if (!PrivilegedFileHelper.delete(fileBuffer))
         {
            throw new IOException("Cannot remove file " + PrivilegedFileHelper.getAbsolutePath(fileBuffer)
               + " Close all streams.");
         }
      }
   }

   /**
    * @return string representation for buffer
    */
   @Override
   public String toString()
   {
      if (out instanceof ByteArrayOutputStream)
      {
         return ((ByteArrayOutputStream)out).toString();
      }
      else if (out instanceof BufferedOutputStream)
      {
         try
         {
            out.close();
            BufferedInputStream is = new BufferedInputStream(PrivilegedFileHelper.fileInputStream(fileBuffer));

            StringBuffer fileData = new StringBuffer(DEFAULT_READ_BUFFER_SIZE);

            byte[] buf = new byte[bufferSize];
            int numRead = 0;
            while ((numRead = is.read(buf)) != -1)
            {

               fileData.append(new String(buf, 0, numRead));

            }
            is.close();
            return fileData.toString();
         }
         catch (IOException e)
         {
            return null;
         }

      }
      else
      {
         return null;
      }
   }

   /**
    * Swap in-memory buffer with file.
    * 
    * @exception IOException
    *              if an I/O error occurs.
    */
   private void swapBuffers() throws IOException
   {
      byte[] data = ((ByteArrayOutputStream)out).toByteArray();
      fileBuffer = PrivilegedFileHelper.createTempFile("decoderBuffer", ".tmp");
      PrivilegedFileHelper.deleteOnExit(fileBuffer);
      out = new BufferedOutputStream(PrivilegedFileHelper.fileOutputStream(fileBuffer), bufferSize);
      out.write(data);
   }

   /**
    * Writes <code>length</code> bytes from the specified byte array starting at offset
    * <code>start</code> to this byte array output stream.
    * 
    * @param buffer
    *          the data.
    * @param start
    *          the start offset in the data.
    * @param length
    *          the number of bytes to write.
    * @exception IOException
    *              if an I/O error occurs.
    */
   @Override
   protected void writeBuffer(byte[] buffer, int start, int length) throws IOException
   {
      if (out instanceof ByteArrayOutputStream)
      {
         if (((ByteArrayOutputStream)out).size() + length > bufferSize)
         {
            swapBuffers();
         }
      }
      out.write(buffer, start, length);
   }

}
