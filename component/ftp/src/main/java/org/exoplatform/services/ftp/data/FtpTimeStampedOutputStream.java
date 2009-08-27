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
package org.exoplatform.services.ftp.data;

import org.exoplatform.services.ftp.FtpConst;
import org.exoplatform.services.ftp.client.FtpClientSession;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by The eXo Platform SAS Author : Vitaly Guly <gavrik-vetal@ukr.net/mail.ru>
 * 
 * @version $Id: $
 */

public class FtpTimeStampedOutputStream extends OutputStream
{

   private OutputStream nativeOutputStream;

   private FtpClientSession clientSession;

   public FtpTimeStampedOutputStream(OutputStream nativeOutputStream, FtpClientSession clientSession)
   {
      this.nativeOutputStream = nativeOutputStream;
      this.clientSession = clientSession;
   }

   public void write(int dataByte) throws IOException
   {
      clientSession.refreshTimeOut();
      nativeOutputStream.write(dataByte);
   }

   public void write(byte[] dataBytes) throws IOException
   {
      write(dataBytes, 0, dataBytes.length);
   }

   public void write(byte[] dataBytes, int offset, int len) throws IOException
   {
      int allWrited = 0;
      int curOffset = offset;

      clientSession.refreshTimeOut();

      while (allWrited < len)
      {
         int curBlockSize = FtpConst.FTP_TIMESTAMPED_BLOCK_SIZE;
         if ((curBlockSize + allWrited) > len)
         {
            curBlockSize = len - allWrited;
         }

         nativeOutputStream.write(dataBytes, curOffset, curBlockSize);
         clientSession.refreshTimeOut();

         allWrited += curBlockSize;
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
