/*
 * Copyright (C) 2012 eXo Platform SAS.
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
      validate(length, position, Integer.MAX_VALUE);

      long newSize = Math.max(length + position, data.length);
      byte[] newBytes = new byte[(int)newSize];

      if (position > 0)
      {
         // begin from the existed bytes
         System.arraycopy(data, 0, newBytes, 0, (int)(position < data.length ? position : data.length));
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

      if (position < data.length)
      {
         // write the rest of existed data
         System.arraycopy(data, (int)position, newBytes, (int)position, (int)(data.length - position));
      }

      data = newBytes;
   }

   /**
    * {@inheritDoc}
    */
   public void setLength(long size) throws IOException, RepositoryException
   {
      validate(size);

      byte[] newBytes = new byte[(int)size];
      System.arraycopy(data, 0, newBytes, 0, Math.min(data.length, newBytes.length));

      data = newBytes;
   }
}
