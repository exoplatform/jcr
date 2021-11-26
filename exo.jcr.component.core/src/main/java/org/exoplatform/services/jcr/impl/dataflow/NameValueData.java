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

import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.value.NameValue;
import org.exoplatform.services.jcr.impl.dataflow.persistent.PersistedValueData;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: ReferenceeValueData.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public abstract class NameValueData extends AbstractValueData
{

   /**
    * The value.
    */
   protected InternalQName value;

   /**
    * NameValueData constructor.
    */
   protected NameValueData(int orderNumber, InternalQName value)
   {
      super(orderNumber);
      this.value = value;
   }

   /**
    * {@inheritDoc}
    */
   protected boolean internalEquals(ValueData another)
   {
      if (another instanceof NameValueData)
      {
         return ((NameValueData)another).value.equals(value);
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
         return value.getAsString().getBytes(Constants.DEFAULT_ENCODING);
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
      return value.getAsString();
   }

   /**
    * {@inheritDoc}
    */
   public PersistedValueData createPersistedCopy(int orderNumber) throws IOException
   {
      return new NamePersistedValueData(orderNumber, value);
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
    * 
    * Take in account {@link NameValue#getString()} uses {@link LocationFactory} to create
    * proper <code>String</code> value.
    */
   protected String getString()
   {
      return value.getAsString();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected InternalQName getName()
   {
      return value;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected QPath getPath()
   {
      QPathEntry entry = new QPathEntry(value, 0);
      return new QPath(new QPathEntry[]{entry});
   }
}
