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

import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.value.PathValue;
import org.exoplatform.services.jcr.impl.dataflow.persistent.PersistedValueData;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.jcr.Value;
import javax.jcr.ValueFormatException;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: ReferenceeValueData.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public abstract class PathValueData extends AbstractValueData
{

   /**
    * The value.
    */
   protected QPath value;

   /**
    * NameValueData constructor.
    */
   protected PathValueData(int orderNumber, QPath value)
   {
      super(orderNumber);
      this.value = value;
   }

   /**
    * {@inheritDoc}
    */
   protected boolean internalEquals(ValueData another)
   {
      if (another instanceof PathValueData)
      {
         return ((PathValueData)another).value.equals(value);
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
      return new PathPersistedValueData(orderNumber, value);
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
    * Take in account {@link PathValue#getString()} uses {@link LocationFactory} to create
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
   protected InternalQName getName() throws ValueFormatException
   {
      if (value.getDepth() == 0 && !value.isAbsolute())
      {
         QPathEntry entry = value.getEntries()[0];
         return new InternalQName(entry.getNamespace(), entry.getName());
      }

      throw new ValueFormatException("Can't conver to InternalQName. Wrong value type.");
   }

   /**
    * {@link Value#getString()}
    */
   protected QPath getPath()
   {
      return value;
   }
}
