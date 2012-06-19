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
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.DoublePersistedValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.PersistedValueData;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.ValueFormatException;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: DoubleValueData.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public abstract class DoubleValueData extends AbstractValueData
{

   /**
    * The value.
    */
   protected double value;

   /**
    * DoubleValueData constructor.
    */
   protected DoubleValueData(int orderNumber, double value)
   {
      super(orderNumber);
      this.value = value;
   }

   /**
    * {@inheritDoc}
    */
   protected boolean internalEquals(ValueData another)
   {
      if (another instanceof DoubleValueData)
      {
         return ((DoubleValueData)another).value == value;
      }

      return false;
   }

   /**
    * {@inheritDoc}
    */
   protected byte[] spoolInternalValue()
   {
      return Double.toString(value).getBytes();
   }

   /**
    * {@inheritDoc}
    */
   public String toString()
   {
      return Double.toString(value);
   }

   /**
    * {@inheritDoc}
    */
   public PersistedValueData createPersistedCopy(int orderNumber) throws IOException
   {
      return new DoublePersistedValueData(orderNumber, value);
   }

   /**
    * {@inheritDoc}
    */
   public TransientValueData createTransientCopy(int orderNumber) throws IOException
   {
      return new TransientValueData(orderNumber, value);
   }

   /**
    * {@inheritDoc}
    */
   protected Long getLong()
   {
      return new Double(value).longValue();
   }

   /**
    * {@inheritDoc}
    */
   protected Boolean getBoolean() throws ValueFormatException
   {
      throw new ValueFormatException("Can't conver to Boolean. Wrong value type.");
   }

   /**
    * {@inheritDoc}
    */
   protected Double getDouble()
   {
      return value;
   }

   /**
    * {@inheritDoc}
    */
   protected String getString()
   {
      return new Double(value).toString();
   }

   /**
    * {@inheritDoc}
    */
   protected Calendar getDate()
   {
      Calendar calendar = Calendar.getInstance();
      calendar.setTimeInMillis(new Double(value).longValue());

      return calendar;
   }

   /**
    * {@inheritDoc}
    */
   protected InputStream getStream()
   {
      return new ByteArrayInputStream(spoolInternalValue());
   }

   /**
    * {@inheritDoc}
    */
   protected InternalQName getName() throws ValueFormatException
   {
      throw new ValueFormatException("Can't conver to InternalQName. Wrong value type.");
   }

   /**
    * {@inheritDoc}
    */
   protected QPath getPath() throws ValueFormatException
   {
      throw new ValueFormatException("Can't conver to QPath. Wrong value type.");
   }

   /**
    * {@inheritDoc}
    */
   protected String getReference() throws ValueFormatException
   {
      throw new ValueFormatException("Can't conver to Identity. Wrong value type.");
   }

   /**
    * {@inheritDoc}
    */
   protected AccessControlEntry getPermission() throws ValueFormatException
   {
      throw new ValueFormatException("Can't conver to AccessControlEntry. Wrong value type.");
   }

}
