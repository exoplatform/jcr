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
package org.exoplatform.services.jcr.impl.core.value;

import org.exoplatform.services.jcr.core.value.ExtendedValue;
import org.exoplatform.services.jcr.core.value.ReadableBinaryValue;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.AbstractValueData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.util.JCRDateFormat;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

/**
 * This class is the superclass of the type-specific classes implementing the <code>Value</code>
 * interfaces.
 * 
 * @author Gennady Azarenkov
 * 
 * @version $Id: BaseValue.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public abstract class BaseValue implements ExtendedValue, ReadableBinaryValue
{

   protected static Log log = ExoLogger.getLogger("jcr.BinaryValue");

   protected final int type;

   protected LocalTransientValueData data;

   protected TransientValueData internalData;

   /**
    * Package-private default constructor.
    * 
    * @param type
    *          int
    * @param data
    *          TransientValueData
    */
   BaseValue(int type, TransientValueData data)
   {
      this.type = type;
      this.internalData = data;
   }

   /**
    * Return Session scope ValueData.
    * 
    * @param asStream
    *          boolean
    * @return LocalTransientValueData
    * @throws IOException
    *           if error
    */
   protected LocalTransientValueData getLocalData(boolean asStream) throws IOException
   {
      if (data == null)
         data = new LocalTransientValueData(asStream);

      return data;
   }

   /**
    * Returns the internal string representation of this value without modifying the value state.
    * 
    * @return String the internal string representation
    * @throws ValueFormatException
    *           if the value can not be represented as a <code>String</code> or if the value is
    *           <code>null</code>.
    * @throws RepositoryException 
    *           if another error occurs.
    */
   protected String getInternalString() throws ValueFormatException, RepositoryException
   {
      try
      {
         return new String(getLocalData(false).getAsByteArray(), Constants.DEFAULT_ENCODING);
      }
      catch (UnsupportedEncodingException e)
      {
         throw new RepositoryException(Constants.DEFAULT_ENCODING + " not supported on this platform", e);
      }
      catch (IOException e)
      {
         throw new ValueFormatException("conversion to string failed: " + e.getMessage(), e);
      }
   }

   /**
    * Return the internal calendar.
    * 
    * @return Calendar
    * @throws ValueFormatException
    *           if formatter error
    * @throws RepositoryException
    *           if other error
    */
   protected Calendar getInternalCalendar() throws ValueFormatException, RepositoryException
   {
      try
      {
         if (type == PropertyType.DATE)
            return new JCRDateFormat().deserialize(new String(getLocalData(false).getAsByteArray(),
               Constants.DEFAULT_ENCODING));

         return JCRDateFormat.parse(new String(getLocalData(false).getAsByteArray(), Constants.DEFAULT_ENCODING));
      }
      catch (UnsupportedEncodingException e)
      {
         throw new RepositoryException(Constants.DEFAULT_ENCODING + " not supported on this platform", e);
      }
      catch (IOException e)
      {
         throw new ValueFormatException("conversion to date failed: " + e.getMessage(), e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public final int getType()
   {
      return type;
   }

   /**
    * {@inheritDoc}
    */
   public Calendar getDate() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      Calendar cal = getInternalCalendar();

      if (cal == null)
      {
         throw new ValueFormatException("not a valid date format " + getInternalString());
      }
      else
      {
         return cal;
      }
   }

   /**
    * {@inheritDoc}
    */
   public long getLong() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      try
      {
         return Long.parseLong(getInternalString());
      }
      catch (NumberFormatException e)
      {
         throw new ValueFormatException("conversion to long failed", e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean getBoolean() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      return Boolean.valueOf(getInternalString()).booleanValue();
   }

   /**
    * {@inheritDoc}
    */
   public double getDouble() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      try
      {
         return Double.parseDouble(getInternalString());
      }
      catch (NumberFormatException e)
      {
         throw new ValueFormatException("conversion to double failed", e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public InputStream getStream() throws ValueFormatException, RepositoryException
   {
      try
      {
         return getLocalData(true).getAsStream();
      }
      catch (IOException e)
      {
         throw new RepositoryException(e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public String getString() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      return getInternalString();
   }

   /**
    * {@inheritDoc}
    */
   public String getReference() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      throw new ValueFormatException("Can not convert " + PropertyType.nameFromValue(type) + " to Reference");
   }

   /**
    * @return Returns the data TransientValueData.
    */
   public TransientValueData getInternalData()
   {
      return internalData;
   }

   /**
    * {@inheritDoc}
    */
   public long getLength()
   {
      try
      {
         return getLocalData(type == PropertyType.BINARY).getLength();
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean equals(Object obj)
   {
      if (this == obj)
      {
         return true;
      }
      if (!obj.getClass().equals(getClass()))
         return false;
      if (obj instanceof BaseValue)
      {
         BaseValue other = (BaseValue)obj;
         return getInternalData().equals(other.getInternalData());
      }
      return false;
   }

   /**
    * {@inheritDoc}
    */
   public int getOrderNumber()
   {
      try
      {
         return getLocalData(type == PropertyType.BINARY).getOrderNumber();
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void setOrderNumber(int order)
   {
      try
      {
         getLocalData(type == PropertyType.BINARY).setOrderNumber(order);
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public long read(OutputStream stream, long length, long position) throws IOException, RepositoryException
   {
      return getInternalData().read(stream, length, position);
   }

   /**
    * Session scope ValueData.
    */
   protected class LocalTransientValueData extends AbstractValueData
   {

      protected InputStream stream;

      protected byte[] bytes;

      protected final long length;

      /**
       * Create new Value data.
       * 
       * @param asStream
       *          boolean
       * @throws IOException
       *           if error
       */
      public LocalTransientValueData(boolean asStream) throws IOException
      {
         super(getInternalData().getOrderNumber());
         TransientValueData idata = getInternalData();
         if (!asStream)
         {
            bytes = idata.getAsByteArray();
            stream = null;
         }
         else
         {
            stream = idata.getAsStream();
            bytes = null;
         }
         length = idata.getLength();
      }

      /**
       * {@inheritDoc}
       */
      public byte[] getAsByteArray() throws IllegalStateException, IOException
      {
         if (streamConsumed())
            throw new IllegalStateException("stream value has already been consumed");
         return bytes;
      }

      /**
       * {@inheritDoc}
       */
      public InputStream getAsStream() throws IOException, IllegalStateException
      {
         if (bytesConsumed())
            throw new IllegalStateException("non-stream value has already been consumed");
         return stream;
      }

      /**
       * {@inheritDoc}
       */
      public long getLength()
      {
         return length;
      }

      /**
       * {@inheritDoc}
       */
      public boolean isByteArray()
      {
         return bytes != null;
      }

      private boolean streamConsumed()
      {
         return stream != null;
      }

      private boolean bytesConsumed()
      {
         return bytes != null;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public TransientValueData createTransientCopy()
      {
         throw new RuntimeException("LocalTransientValueData.createTransientCopy() is out of contract");
      }

      /**
       * {@inheritDoc}
       */
      public boolean isTransient()
      {
         return true;
      }
   }
}
