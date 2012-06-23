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

import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.IllegalPathException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.PersistedValueData;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

import javax.jcr.Value;
import javax.jcr.ValueFormatException;

/**
 * Common class for all value data implementation.
 * 
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: AbstractValueData.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public abstract class AbstractValueData implements ValueData
{

   /**
    * Logger. 
    */
   protected final static Log LOG = ExoLogger.getLogger("exo.jcr.component.core.AbstractValueData");

   /**
    * Value data order number.
    */
   protected int orderNumber;

   /**
    * Constructor AbstractNewValueData.
    */
   protected AbstractValueData(int orderNumber)
   {
      this.orderNumber = orderNumber;
   }

   /**
    * {@inheritDoc}
    */
   public final int getOrderNumber()
   {
      return orderNumber;
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
      if (isByteArray())
      {
         return readFromByteArray(stream, length, position);
      }
      else
      {
         throw new IllegalStateException("Method is not supported for ValueData represented in array of bytes");
      }
   }

   /**
    * Reads data from array of bytes to {@link OutputStream}.
    */
   protected long readFromByteArray(OutputStream stream, long length, long position) throws IOException
   {
      byte[] data = getAsByteArray();
      length = validateAndAdjustLenght(length, position, data.length);

      stream.write(data, (int)position, (int)length);

      return length;
   }

   /**
    * Validate parameters. <code>Length</code> and <code>position</code> should not be negative and 
    * <code>length</code> should not be greater than <code>dataLength</code>
    * 
    * @return adjusted length of byte to read. Should not be possible to exceed array border. 
    */
   protected long validateAndAdjustLenght(long length, long position, long dataLength) throws IOException
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

      if (position + length >= dataLength)
      {
         return dataLength - position;
      }

      return length;
   }

   /**
    * {@inheritDoc}
    */
   public boolean equals(Object obj)
   {
      if (obj instanceof ValueData)
      {
         return this.equals((ValueData)obj);
      }

      return false;
   }

   /**
    * {@inheritDoc}
    */
   public boolean equals(ValueData another)
   {
      ValueData valueData = another;
      if (another instanceof TransientValueData)
      {
         return another.equals(this);
      }
      else if (valueData instanceof AbstractValueData)
      {
         return this == valueData || internalEquals(valueData);
      }

      return false;
   }

   /**
    * Creates transient copy of value data. 
    */
   protected abstract TransientValueData createTransientCopy(int orderNumber) throws IOException;

   /**
    * Creates persisted copy of value data. 
    */
   protected abstract PersistedValueData createPersistedCopy(int orderNumber) throws IOException;

   /**
    * Represents internal value as array of bytes.
    */
   protected abstract byte[] spoolInternalValue();

   /**
    * Equals internal value. 
    */
   protected abstract boolean internalEquals(ValueData another);

   // ================================== Getting value methods

   /**
    * {@link Value#getLong()}
    */
   protected Long getLong() throws ValueFormatException
   {
      throw new ValueFormatException("Can't conver to Long. Wrong value type.");
   }

   /**
    * {@link Value#getBoolean()}
    */
   protected Boolean getBoolean() throws ValueFormatException
   {
      throw new ValueFormatException("Can't conver to Boolean. Wrong value type.");
   }

   /**
    * {@link Value#getDouble()}
    */
   protected Double getDouble() throws ValueFormatException
   {
      throw new ValueFormatException("Can't conver to Double. Wrong value type.");
   }

   /**
    * {@link Value#getDate()}
    */
   protected Calendar getDate() throws ValueFormatException
   {
      throw new ValueFormatException("Can't conver to Calendar. Wrong value type.");
   }

   /**
    * {@link Value#getStream()}
    */
   protected InputStream getStream() throws IOException
   {
      return new ByteArrayInputStream(spoolInternalValue());
   }

   /**
    * {@link Value#getString()}
    */
   protected InternalQName getName() throws IllegalNameException, ValueFormatException
   {
      throw new ValueFormatException("Can't conver to InternalQName. Wrong value type.");
   }

   /**
    * {@link Value#getString()}
    */
   protected QPath getPath() throws IllegalPathException, ValueFormatException
   {
      throw new ValueFormatException("Can't conver to QPath. Wrong value type.");
   }

   /**
    * {@link Value#getString()}
    */
   protected String getReference() throws ValueFormatException
   {
      throw new ValueFormatException("Can't conver to Identity. Wrong value type.");
   }

   /**
    * {@link Value#getString()}
    */
   protected AccessControlEntry getPermission() throws ValueFormatException
   {
      throw new ValueFormatException("Can't conver to AccessControlEntry. Wrong value type.");
   }

   /**
    * {@link Value#getString()}
    */
   protected abstract String getString() throws ValueFormatException;

}
