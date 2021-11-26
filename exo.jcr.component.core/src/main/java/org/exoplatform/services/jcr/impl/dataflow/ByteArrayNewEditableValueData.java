/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.impl.dataflow;

import org.exoplatform.services.jcr.core.value.EditableBinaryValue;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.RepositoryException;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: ByteArrayNewEditableValueData.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class ByteArrayNewEditableValueData extends ByteArrayNewValueData implements EditableBinaryValue
{

   /**
    * ByteArrayNewEditableValueData constructor.
    */
   public ByteArrayNewEditableValueData(int orderNumber, byte[] bytes) throws IOException
   {
      super(orderNumber, bytes);
   }

   /**
    * {@inheritDoc}
    */
   public void update(InputStream stream, long length, long position) throws IOException, RepositoryException
   {
      validateAndAdjustLenght(length, position, Integer.MAX_VALUE);

      long newSize = Math.max(length + position, value.length);
      byte[] newBytes = new byte[(int)newSize];

      if (position > 0)
      {
         // begin from the existed bytes
         System.arraycopy(value, 0, newBytes, 0, (int)(position < value.length ? position : value.length));
      }

      // write new data
      long read;
      boolean doRead = true;
      byte[] buff = new byte[2048];

      while (doRead && (read = stream.read(buff)) >= 0)
      {
         if (position + read > newBytes.length)
         {
            // given length reached
            read = newBytes.length - position;
            doRead = false;
         }
         System.arraycopy(buff, 0, newBytes, (int)position, (int)read);
         position += read;
      }

      if (position < value.length)
      {
         // write the rest of existed data
         System.arraycopy(value, (int)position, newBytes, (int)position, (int)(value.length - position));
      }

      value = newBytes;
   }

   /**
    * {@inheritDoc}
    */
   public void setLength(long size) throws IOException, RepositoryException
   {
      if (size < 0)
      {
         throw new IOException("Size must be higher or equals 0. But given " + size);
      }

      byte[] newBytes = new byte[(int)size];
      System.arraycopy(value, 0, newBytes, 0, Math.min(value.length, newBytes.length));

      value = newBytes;
   }
}
