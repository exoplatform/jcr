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

import org.exoplatform.frameworks.ftpclient.FtpConst;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.io.OutputStream;

public class FtpSlowOutputStream extends OutputStream
{

   private static Log log = ExoLogger.getLogger(FtpConst.FTP_PREFIX + "FtpSlowOutputStream");

   private OutputStream nativeOutputStream;

   private int bytesPerSec;

   private int blockSize;

   private int writed = 0;

   public FtpSlowOutputStream(OutputStream nativeOutputStream, int bytesPerSec)
   {
      this.nativeOutputStream = nativeOutputStream;
      this.bytesPerSec = bytesPerSec;

      blockSize = bytesPerSec / 10;
   }

   protected void tryWaiting()
   {
      if (writed >= blockSize)
      {
         try
         {
            Thread.sleep(100);
         }
         catch (Exception exc)
         {
            log.info("Unhandled exception. " + exc.getMessage(), exc);
         }
         writed = 0;
      }
   }

   public void write(int dataByte) throws IOException
   {
      tryWaiting();
      nativeOutputStream.write(dataByte);
      writed++;
   }

   public void write(byte[] dataBytes) throws IOException
   {
      write(dataBytes, 0, dataBytes.length);
   }

   public void write(byte[] dataBytes, int offset, int len) throws IOException
   {
      int allWrited = 0;
      int curOffset = offset;

      while (allWrited < len)
      {
         tryWaiting();

         int curBlockSize = blockSize - writed;
         if ((curBlockSize + allWrited) > len)
         {
            curBlockSize = len - allWrited;
         }

         nativeOutputStream.write(dataBytes, curOffset, curBlockSize);

         allWrited += curBlockSize;
         writed += curBlockSize;
         curOffset += curBlockSize;
      }
   }

   public void flush() throws IOException
   {
      nativeOutputStream.flush();
   }

   public void close() throws IOException
   {
      nativeOutputStream.close();
   }

}
