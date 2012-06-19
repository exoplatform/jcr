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
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.dataflow.ValueDataUtil;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
 * @version $Id$
 */
public abstract class BaseValue implements ExtendedValue, ReadableBinaryValue
{

   /**
    * Logger.
    */
   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.BaseValue");

   /**
    * Value type.
    */
   protected final int type;

   /**
    * Internal value data.
    */
   protected ValueData internalData;

   /**
    * Indicates that stream consuming method already was invoked.
    */
   protected boolean streamConsumed;

   /**
    * Indicates that bytes consuming method already was invoked. 
    */
   protected boolean bytesConsumed;

   /**
    * Store stream value. Should returns same stream instance for different
    * invoking {@link BaseValue#getStream()} method.
    */
   protected InputStream stream;

   /**
    * Package-private default constructor.
    * 
    * @param type
    *          int
    * @param data
    *          TransientValueData
    */
   BaseValue(int type, ValueData data)
   {
      this.type = type;
      this.internalData = data;
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
      validateByteArrayMethodInvoking();

      return ValueDataUtil.getDate(getInternalData());
   }

   /**
    * {@inheritDoc}
    */
   public long getLong() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      validateByteArrayMethodInvoking();

      try
      {
         return ValueDataUtil.getLong(getInternalData());
      }
      catch (NumberFormatException e)
      {
         throw new ValueFormatException("Can't convert to Long value");
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean getBoolean() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      validateByteArrayMethodInvoking();

      return ValueDataUtil.getBoolean(getInternalData());
   }

   /**
    * {@inheritDoc}
    */
   public double getDouble() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      validateByteArrayMethodInvoking();

      try
      {
         return ValueDataUtil.getDouble(getInternalData());
      }
      catch (NumberFormatException e)
      {
         throw new ValueFormatException("Can't convert to Long value");
      }
   }

   /**
    * {@inheritDoc}
    */
   public InputStream getStream() throws ValueFormatException, RepositoryException
   {
      validateStreamMethodInvoking();

      try
      {
         if (stream == null)
         {
            stream = getAsStream();
         }

         return stream;
      }
      catch (IOException e)
      {
         throw new RepositoryException(e.getMessage(), e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public String getString() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      validateByteArrayMethodInvoking();

      return ValueDataUtil.getString(getInternalData());
   }

   /**
    * {@inheritDoc}
    */
   public String getReference() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      validateByteArrayMethodInvoking();

      return ValueDataUtil.getReference(getInternalData());
   }

   /**
    * {@inheritDoc}
    */
   public long getLength()
   {
      return getInternalData().getLength();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
      {
         return true;
      }
      else if (!obj.getClass().equals(getClass()))
      {
         return false;
      }
      else if (obj instanceof BaseValue)
      {
         return getInternalData().equals(((BaseValue)obj).getInternalData());
      }
      return false;
   }

   /**
    * {@inheritDoc}
    */
   public int getOrderNumber()
   {
      return getInternalData().getOrderNumber();
   }

   /**
    * {@inheritDoc}
    */
   public long read(OutputStream stream, long length, long position) throws IOException, RepositoryException
   {
      return getInternalData().read(stream, length, position);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String toString()
   {
      String info;

      String typeName;
      try
      {
         typeName = PropertyType.nameFromValue(type);
      }
      catch (IllegalArgumentException e)
      {
         // Value has abnormal type
         typeName = String.valueOf(type);
      }

      if (type == PropertyType.BINARY)
      {
         info = "size: " + ((getInternalData() == null) ? "undefined" : (getInternalData().getLength() + " bytes"));
      }
      else
      {
         try
         {
            info = "value: '" + getString() + "'";
         }
         catch (IllegalStateException e)
         {
            info = "can't retrieve value";
         }
         catch (RepositoryException e)
         {
            info = "can't retrieve value";
         }
      }

      return String.format("Value {\n type: %s;\n data-class: %s;\n %s\n}", typeName, getInternalData() == null ? null
         : internalData.getClass().getName(), info);
   }

   /**
    * Returns internal value data.
    */
   public ValueData getInternalData()
   {
      return internalData;
   }

   protected InputStream getAsStream() throws IOException, ValueFormatException, IllegalStateException,
      RepositoryException
   {
      return getInternalData().getAsStream();
   }

   protected void validateStreamMethodInvoking() throws IllegalStateException
   {
      if (streamConsumed)
      {
         throw new IllegalStateException("non-stream value has already been consumed");
      }

      bytesConsumed = true;
   }

   protected void validateByteArrayMethodInvoking() throws IllegalStateException
   {
      if (bytesConsumed)
      {
         throw new IllegalStateException("non-stream value has already been consumed");
      }

      streamConsumed = true;
   }

   protected void invalidateStream()
   {
      this.stream = null;
   }

}
