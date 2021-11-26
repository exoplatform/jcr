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

import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.LongPersistedValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.PersistedValueData;

import java.io.IOException;
import java.util.Calendar;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: LongValueData.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public abstract class LongValueData extends AbstractValueData
{

   /**
    * The value.
    */
   protected long value;

   /**
    * LongNewValueData constructor.
    */
   protected LongValueData(int orderNumber, long value)
   {
      super(orderNumber);
      this.value = value;
   }

   /**
    * {@inheritDoc}
    */
   protected boolean internalEquals(ValueData another)
   {
      if (another instanceof LongValueData)
      {
         return ((LongValueData)another).value == value;
      }

      return false;
   }

   /**
    * {@inheritDoc}
    */
   protected byte[] spoolInternalValue()
   {
      return Long.toString(value).getBytes();
   }

   /**
    * {@inheritDoc}
    */
   public String toString()
   {
      return Long.valueOf(value).toString();
   }

   /**
    * {@inheritDoc}
    */
   public PersistedValueData createPersistedCopy(int orderNumber) throws IOException
   {
      return new LongPersistedValueData(orderNumber, value);
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
   @Override
   protected Long getLong()
   {
      return value;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected Double getDouble()
   {
      return (double)value;
   }

   /**
    * {@inheritDoc}
    */
   protected String getString()
   {
      return Long.toString(value);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected Calendar getDate()
   {
      Calendar calendar = Calendar.getInstance();
      calendar.setTimeInMillis(value);

      return calendar;
   }
}
