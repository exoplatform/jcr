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

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

/**
 * a double value impl.
 * 
 * @author Gennady Azarenkov
 */
public class DoubleValue extends BaseValue
{

   public static final int TYPE = PropertyType.DOUBLE;

   /**
    * DoubleValue constructor.
    */
   public DoubleValue(double dbl) throws IOException
   {
      super(TYPE, new TransientValueData(dbl));
   }

   /**
    * DoubleValue constructor.
    */
   DoubleValue(ValueData data) throws IOException
   {
      super(TYPE, data);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Calendar getDate() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      validateByteArrayMethodInvoking();

      Double doubleNumber = ValueDataUtil.getDouble(getInternalData());

      if (doubleNumber != null)
      {
         // loosing timezone information...
         Calendar cal = Calendar.getInstance();
         cal.setTime(new Date(doubleNumber.longValue()));
         return cal;
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
   public long getLong() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      validateByteArrayMethodInvoking();

      Double doubleNumber = ValueDataUtil.getDouble(getInternalData());

      if (doubleNumber != null)
      {
         return doubleNumber.longValue();
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
      throw new ValueFormatException("conversion to boolean failed: inconvertible types");
   }
}
