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
package org.exoplatform.frameworks.ftpclient.data;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.io.InputStream;

public class FtpSlowInputStream1 extends InputStream
{

   private static final Log LOG = ExoLogger.getLogger("exo.jcr.framework.command.FtpSlowInputStream");

   protected InputStream nativeInputStream;

   protected int bytesPerSec;

   protected int blockSize = 0;

   protected int readed = 0;

   public FtpSlowInputStream1(InputStream nativeInputStream, int bytesPerSec)
   {
      this.nativeInputStream = nativeInputStream;
      this.bytesPerSec = bytesPerSec;
      blockSize = bytesPerSec / 10;
   }

   protected void tryWaiting()
   {
      if (readed >= blockSize)
      {
         try
         {
            Thread.sleep(100);
         }
         catch (Exception exc)
         {
            LOG.info("Unhandled exception until Thread.sleep(...). " + exc.getMessage(), exc);
         }
         readed = 0;
      }
   }

   public int read() throws IOException
   {
      tryWaiting();
      int curReaded = nativeInputStream.read();
      if (curReaded >= 0)
      {
         readed++;
      }
      return curReaded;
   }

   public int read(byte[] buffer) throws IOException
   {
      return read(buffer, 0, buffer.length);
   }

   public int read(byte[] buffer, int offset, int size) throws IOException
   {
      tryWaiting();

      int curBlockSize = blockSize - readed;
      if (curBlockSize > size)
      {
         curBlockSize = size;
      }

      int curReaded = nativeInputStream.read(buffer, offset, curBlockSize);

      readed += curReaded;
      return curReaded;
   }

   public long skip(long skipVal) throws IOException
   {
      return nativeInputStream.skip(skipVal);
   }

   public int available() throws IOException
   {
      return nativeInputStream.available();
   }

   public void close() throws IOException
   {
      nativeInputStream.close();
   }

   public synchronized void mark(int markVal)
   {
      nativeInputStream.mark(markVal);
   }

   public synchronized void reset() throws IOException
   {
      nativeInputStream.reset();
   }

   public boolean markSupported()
   {
      return nativeInputStream.markSupported();
   }

}
