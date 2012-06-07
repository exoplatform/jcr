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

import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: AbstractNewValueData.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public abstract class AbstractNewValueData extends AbstractSessionValueData
{

   /**
    * Internal value is represented as array of bytes.
    */
   protected byte data[];

   /**
    * Constructor AbstractNewValueData.
    */
   protected AbstractNewValueData(int orderNumber)
   {
      super(orderNumber);
   }

   /**
    * {@inheritDoc}
    */
   public boolean isByteArray()
   {
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public byte[] getAsByteArray() throws IllegalStateException, IOException
   {
      spoolToByteArray();

      return data;
   }

   /**
    * {@inheritDoc}
    */
   public InputStream getAsStream() throws IOException
   {
      spoolToByteArray();

      return new ByteArrayInputStream(data);
   }

   /**
    * {@inheritDoc}
    */
   public long getLength()
   {
      spoolToByteArray();

      return data.length;
   }

   /**
    * {@inheritDoc}
    */
   public long read(OutputStream stream, long length, long position) throws IOException
   {
      spoolToByteArray();

      validate(length, position, data.length);
      length = adjustLength(length, position, data.length);

      stream.write(data, (int)position, (int)length);

      return length;
   }

   /**
    *  Adjusts length of byte to read. Should not be possible to exceed array border. 
    */
   protected long adjustLength(long length, long position, long dataLength)
   {
      if (position + length >= dataLength)
      {
         return dataLength - position;
      }

      return length;
   }

   protected void validate(long length, long position, long dataLength) throws IOException
   {
      if (position < 0)
      {
         throw new IOException("Position must be higher or equals 0. But given " + position);
      }

      if (length < 0)
      {
         throw new IOException("Length must be higher or equals 0. But given " + length);
      }

      if (position >= dataLength && position > 0)
      {
         throw new IOException("Position " + position + " out of value size " + dataLength);
      }
   }

   protected void validate(long size) throws IOException
   {
      if (size < 0)
      {
         throw new IOException("Size must be higher or equals 0. But given " + size);
      }
   }

   /**
    * Convert String into bytes array using default encoding.
    */
   protected byte[] stringToBytes(final String value)
   {
      try
      {
         return value.getBytes(Constants.DEFAULT_ENCODING);
      }
      catch (UnsupportedEncodingException e)
      {
         throw new RuntimeException("FATAL ERROR Charset " + Constants.DEFAULT_ENCODING + " is not supported!");
      }
   }

   /**
    * Spools data to array.
    */
   private void spoolToByteArray()
   {
      if (data == null)
      {
         data = spoolInternalValue();
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean equals(ValueData another)
   {
      try
      {
         return Arrays.equals(getAsByteArray(), another.getAsByteArray());
      }
      catch (IOException e)
      {
         LOG.error("Read error", e);
         return false;
      }
   }

   /**
    * Represents internal value as array of bytes.
    */
   protected abstract byte[] spoolInternalValue();

}
