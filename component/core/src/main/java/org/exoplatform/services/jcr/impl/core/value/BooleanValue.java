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

import java.io.IOException;
import java.util.Calendar;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;

/**
 * a boolean value implementation.
 * 
 * @author Gennady Azarenkov
 */
public class BooleanValue
   extends BaseValue
{

   public static final int TYPE = PropertyType.BOOLEAN;

   public BooleanValue(boolean bool) throws IOException
   {
      super(TYPE, new TransientValueData(bool));
   }

   public BooleanValue(TransientValueData data) throws IOException
   {
      super(TYPE, data);
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.impl.core.value.BaseValue#getDate()
    */
   public Calendar getDate() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      throw new ValueFormatException("conversion to date failed: inconvertible types");
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.impl.core.value.BaseValue#getLong()
    */
   public long getLong() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      throw new ValueFormatException("conversion to long failed: inconvertible types");
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.impl.core.value.BaseValue#getDouble()
    */
   public double getDouble() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      throw new ValueFormatException("conversion to double failed: inconvertible types");
   }
}
