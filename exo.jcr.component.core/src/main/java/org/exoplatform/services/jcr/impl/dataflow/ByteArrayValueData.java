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

import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.IllegalPathException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.persistent.ByteArrayPersistedValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.PersistedValueData;
import org.exoplatform.services.jcr.impl.util.JCRDateFormat;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Calendar;

import javax.jcr.ValueFormatException;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: ByteArrayValueData.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public abstract class ByteArrayValueData extends AbstractValueData
{

   /**
    * The value.
    */
   protected byte[] value;

   /**
    * ByteArrayValueData constructor.
    */
   protected ByteArrayValueData(int orderNumber, byte[] value)
   {
      super(orderNumber);
      this.value = value;
   }

   /**
    * {@inheritDoc}
    */
   protected boolean internalEquals(ValueData another)
   {
      if (another instanceof ByteArrayValueData)
      {
         return Arrays.equals(((ByteArrayValueData)another).value, value);
      }

      return false;
   }

   /**
    * {@inheritDoc}
    */
   protected byte[] spoolInternalValue()
   {
      return value;
   }

   /**
    * {@inheritDoc}
    */
   public PersistedValueData createPersistedCopy(int orderNumber) throws IOException
   {
      return new ByteArrayPersistedValueData(orderNumber, value);
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
   protected Long getLong() throws ValueFormatException
   {
      return Long.valueOf(getString());
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected Boolean getBoolean() throws ValueFormatException
   {
      return Boolean.valueOf(getString());
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected Double getDouble() throws ValueFormatException
   {
      return Double.valueOf(getString());
   }

   /**
    * {@inheritDoc}
    */
   protected String getString() throws ValueFormatException
   {
      try
      {
         return new String(value, Constants.DEFAULT_ENCODING);
      }
      catch (UnsupportedEncodingException e)
      {
         throw new ValueFormatException("Unsupported encoding " + Constants.DEFAULT_ENCODING, e);
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected Calendar getDate() throws ValueFormatException
   {
      return JCRDateFormat.parse(getString());
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected InternalQName getName() throws ValueFormatException, IllegalNameException
   {
      return InternalQName.parse(getString());
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected QPath getPath() throws ValueFormatException, IllegalPathException
   {
      return QPath.parse(getString());
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected String getReference() throws ValueFormatException
   {
      return getString();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected AccessControlEntry getPermission() throws ValueFormatException
   {
      return AccessControlEntry.parse(getString());
   }
}
