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
import org.exoplatform.services.jcr.impl.dataflow.persistent.ByteArrayPersistedValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.StreamPersistedValueData;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: AbstractNewValueData.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public abstract class AbstractNewValueData extends AbstractSessionValueData
{

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
   public byte[] getAsByteArray() throws IOException
   {
      return spoolInternalValue();
   }

   /**
    * {@inheritDoc}
    */
   public InputStream getAsStream() throws IOException
   {
      return new ByteArrayInputStream(getAsByteArray());
   }

   /**
    * {@inheritDoc}
    */
   public long getLength()
   {
      try
      {
         return getAsByteArray().length;
      }
      catch (IOException e)
      {
         throw new IllegalStateException("Can't calculate the length of value", e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public long read(OutputStream stream, long length, long position) throws IOException
   {
      byte[] data = getAsByteArray();

      validate(length, position, data.length);
      length = adjustReadLength(length, position, data.length);

      stream.write(data, (int)position, (int)length);

      return length;
   }

   /**
    *  Adjusts length of byte to read. Should not be possible to exceed array border. 
    */
   protected long adjustReadLength(long length, long position, long dataLength)
   {
      if (position + length >= dataLength)
      {
         return dataLength - position;
      }

      return length;
   }

   /**
    * Validate parameters. <code>Length</code> and <code>position</code> should not be negative and 
    * <code>length</code> should not be greater than <code>dataLength</code>
    */
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

   /**
    * Validate <code>size</code> parameter, should not be negative.
    */
   protected void validate(long size) throws IOException
   {
      if (size < 0)
      {
         throw new IOException("Size must be higher or equals 0. But given " + size);
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean equals(ValueData another)
   {
      ValueData valueData = another;
      if (another instanceof TransientValueData)
      {
         valueData = ((TransientValueData)another).delegate;
      }

      if (valueData instanceof AbstractNewValueData)
      {
         return this == valueData || internalEquals(valueData);
      }
      else if (valueData instanceof StreamPersistedValueData)
      {
         return internalEquals(valueData);
      }
      else if (valueData instanceof ByteArrayPersistedValueData)
      {
         try
         {
            return Arrays.equals(valueData.getAsByteArray(), getAsByteArray());
         }
         catch (IllegalStateException e)
         {
            LOG.error("Error in comparing values", e);
         }
         catch (IOException e)
         {
            LOG.error("Error in comparing values", e);
         }
      }

      return false;
   }

   /**
    * Represents internal value as array of bytes.
    */
   protected abstract byte[] spoolInternalValue();

   /**
    * Equals internal value. 
    */
   protected abstract boolean internalEquals(ValueData another);

}
