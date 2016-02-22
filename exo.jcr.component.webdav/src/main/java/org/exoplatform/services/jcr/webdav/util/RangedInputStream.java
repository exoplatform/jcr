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
package org.exoplatform.services.jcr.webdav.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by The eXo Platform SAS.
 * @author Vitaly Guly - gavrikvetal@gmail.com
 * 
 * @version $Id: $
 */

public class RangedInputStream extends InputStream
{

   /**
    * Input Stream.
    */
   private InputStream nativeInputStream;

   /**
    * End of range.
    */
   private long endRange;

   /**
    * Position.
    */
   private long position = 0;

   /**
    * @param nativeInputStream Input Stream.
    * @param startRange range start
    * @param endRange range end
    * @throws IOException {@link IOException}
    */
   public RangedInputStream(InputStream nativeInputStream, long startRange, long endRange) throws IOException
   {
      this.nativeInputStream = nativeInputStream;
      this.endRange = endRange;

      byte[] buff = new byte[0x1000];

      while (position < (startRange - 1))
      {
         long needToRead = buff.length;
         if (needToRead > (startRange - position))
         {
            needToRead = startRange - position;
         }

         long readed = nativeInputStream.read(buff, 0, (int)needToRead);

         if (readed < 0)
         {
            break;
         }

         position += readed;
      }
   }

   /**
    * {@inheritDoc}
    */
   public int read() throws IOException
   {
      if (position > endRange)
      {
         return -1;
      }

      int curReaded = nativeInputStream.read();
      if (curReaded >= 0)
      {
         position++;
      }
      return curReaded;
   }

   /**
    * {@inheritDoc}
    */
   public int read(byte[] buffer) throws IOException
   {
      return read(buffer, 0, buffer.length);
   }

   /**
    * {@inheritDoc}
    */
   public int read(byte[] buffer, int offset, int size) throws IOException
   {
      long needsToRead = size;

      if (needsToRead > (endRange - position + 1))
      {
         needsToRead = endRange - position + 1;
      }

      if (needsToRead == 0)
      {
         return -1;
      }

      int curReaded = nativeInputStream.read(buffer, offset, (int)needsToRead);
      position += curReaded;
      return curReaded;
   }

   /**
    * {@inheritDoc}
    */
   public long skip(long skipVal) throws IOException
   {
      return nativeInputStream.skip(skipVal);
   }

   /**
    * {@inheritDoc}
    */
   public int available() throws IOException
   {
      return nativeInputStream.available();
   }

   /**
    * {@inheritDoc}
    */
   public void close() throws IOException
   {
      nativeInputStream.close();
   }

   /**
    * {@inheritDoc}
    */
   public synchronized void mark(int markVal)
   {
      nativeInputStream.mark(markVal);
   }

   /**
    * {@inheritDoc}
    */
   public synchronized void reset() throws IOException
   {
      nativeInputStream.reset();
   }

   /**
    * {@inheritDoc}
    */
   public boolean markSupported()
   {
      return nativeInputStream.markSupported();
   }

}
