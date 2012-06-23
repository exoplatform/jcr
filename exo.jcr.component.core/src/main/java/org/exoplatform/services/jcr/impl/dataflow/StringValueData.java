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
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.persistent.PersistedValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.StringPersistedValueData;
import org.exoplatform.services.jcr.impl.util.JCRDateFormat;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;

import javax.jcr.ValueFormatException;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: StringValueData.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public abstract class StringValueData extends AbstractValueData
{
   /**
    * The value.
    */
   protected String value;

   /**
    * StringValueData constructor.
    */
   protected StringValueData(int orderNumber, String value)
   {
      super(orderNumber);
      this.value = value;
   }

   /**
    * {@inheritDoc}
    */
   protected boolean internalEquals(ValueData another)
   {
      if (another instanceof StringValueData)
      {
         return ((StringValueData)another).value.equals(value);
      }

      return false;
   }

   /**
    * {@inheritDoc}
    */
   protected byte[] spoolInternalValue()
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
    * {@inheritDoc}
    */
   public String toString()
   {
      return value;
   }

   /**
    * {@inheritDoc}
    */
   public PersistedValueData createPersistedCopy(int orderNumber) throws IOException
   {
      return new StringPersistedValueData(orderNumber, value);
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
      return Long.valueOf(value);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected Boolean getBoolean()
   {
      return Boolean.valueOf(value);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected Double getDouble()
   {
      return Double.valueOf(value);
   }

   /**
    * {@inheritDoc}
    */
   protected String getString()
   {
      return value;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected Calendar getDate() throws ValueFormatException
   {
      return JCRDateFormat.parse(value);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected InternalQName getName() throws IllegalNameException
   {
      return InternalQName.parse(value);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected QPath getPath() throws IllegalPathException
   {
      return QPath.parse(value);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected String getReference()
   {
      return value;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected AccessControlEntry getPermission()
   {
      return AccessControlEntry.parse(value);
   }
}
