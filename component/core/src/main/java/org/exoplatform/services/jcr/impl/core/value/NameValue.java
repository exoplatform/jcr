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

import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.impl.core.JCRName;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;

import java.io.IOException;
import java.util.Calendar;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

/**
 * a <code>NAME</code> value impl (a string that is namespace-qualified).
 * 
 * @author Gennady Azarenkov
 */
public class NameValue extends BaseValue
{

   public static final int TYPE = PropertyType.NAME;

   private final LocationFactory locationFactory;

   public NameValue(InternalQName name, LocationFactory locationFactory) throws IOException
   {
      super(TYPE, new TransientValueData(name));
      this.locationFactory = locationFactory;
   }

   public NameValue(TransientValueData data, LocationFactory locationFactory) throws IOException
   {
      super(TYPE, data);
      this.locationFactory = locationFactory;
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.impl.core.value.BaseValue#getString()
    */
   public String getString() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      JCRName name = locationFactory.createJCRName(getQName());
      return name.getAsString();
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
    * @see org.exoplatform.services.jcr.impl.core.value.BaseValue#getBoolean()
    */
   public boolean getBoolean() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      throw new ValueFormatException("conversion to boolean failed: inconvertible types");
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.impl.core.value.BaseValue#getDouble()
    */
   public double getDouble() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      throw new ValueFormatException("conversion to double failed: inconvertible types");
   }

   /**
    * @return qname
    * @throws ValueFormatException
    * @throws IllegalStateException
    * @throws RepositoryException
    */
   public InternalQName getQName() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      try
      {
         return InternalQName.parse(getInternalString());
      }
      catch (IllegalNameException e)
      {
         throw new RepositoryException(e);
      }
   }

}
