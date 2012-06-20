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

import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.BooleanPersistedValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.PersistedValueData;

import java.io.IOException;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: BooleanValueData.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public abstract class BooleanValueData extends AbstractValueData
{
   /**
    * The value.
    */
   protected boolean value;

   /**
    * BooleanValueData constructor.
    */
   protected BooleanValueData(int orderNumber, boolean value)
   {
      super(orderNumber);
      this.value = value;
   }

   /**
    * {@inheritDoc}
    */
   protected boolean internalEquals(ValueData another)
   {
      if (another instanceof BooleanValueData)
      {
         return ((BooleanValueData)another).value == value;
      }

      return false;
   }

   /**
    * {@inheritDoc}
    */
   protected byte[] spoolInternalValue()
   {
      return Boolean.toString(value).getBytes();
   }

   /**
    * {@inheritDoc}
    */
   public String toString()
   {
      return Boolean.valueOf(value).toString();
   }

   /**
    * {@inheritDoc}
    */
   public PersistedValueData createPersistedCopy(int orderNumber) throws IOException
   {
      return new BooleanPersistedValueData(orderNumber, value);
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
   protected Boolean getBoolean()
   {
      return value;
   }

   /**
    * {@inheritDoc}
    */
   protected String getString()
   {
      return Boolean.toString(value);
   }
}
