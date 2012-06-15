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

import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.dataflow.ValueDataUtil;
import org.exoplatform.services.jcr.impl.util.JCRDateFormat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

/**
 * a date value implementation.
 * 
 * @author Gennady Azarenkov
 */
public class DateValue extends BaseValue
{

   public static final int TYPE = PropertyType.DATE;

   /**
    * Constructs a <code>DateValue</code> object representing a date.
    * 
    * @param date
    *          the date this <code>DateValue</code> should represent s
    */
   public DateValue(Calendar date) throws IOException
   {
      super(TYPE, new TransientValueData(date));
   }

   /**
    * Constructs a <code>DateValue</code> object representing a date.
    */
   DateValue(ValueData data) throws IOException
   {
      super(TYPE, data);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public long getLong() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      validateByteArrayMethodInvoking();

      Calendar date = ValueDataUtil.getDate(getInternalData());

      if (date != null)
      {
         return date.getTimeInMillis();
      }
      else
      {
         throw new ValueFormatException("empty value");
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean getBoolean() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      throw new ValueFormatException("cannot convert date to boolean");
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   public String getString() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      validateByteArrayMethodInvoking();

      return getInternalString();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public double getDouble() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      validateByteArrayMethodInvoking();

      Calendar date = ValueDataUtil.getDate(getInternalData());

      if (date != null)
      {
         long ms = date.getTimeInMillis();
         if (ms <= Double.MAX_VALUE)
         {
            return ms;
         }
         throw new ValueFormatException("conversion from date to double failed: inconvertible types");
      }
      else
      {
         throw new ValueFormatException("empty value");
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public long getLength()
   {
      try
      {
         return getString().length();
      }
      catch (ValueFormatException e)
      {
         return super.getLength();
      }
      catch (RepositoryException e)
      {
         return super.getLength();
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected InputStream getAsStream() throws IOException, ValueFormatException, IllegalStateException,
      RepositoryException
   {
      return new ByteArrayInputStream(getInternalString().getBytes());
   }

   /**
    * Returns {@link Calendar} represented in String format.
    */
   private String getInternalString() throws RepositoryException
   {
      Calendar date = ValueDataUtil.getDate(internalData);
      if (date != null)
      {
         return JCRDateFormat.format(date);
      }
      else
      {
         throw new ValueFormatException("empty value");
      }
   }

}
