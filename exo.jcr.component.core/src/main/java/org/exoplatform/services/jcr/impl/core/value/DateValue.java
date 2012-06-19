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

import java.io.IOException;
import java.util.Calendar;

import javax.jcr.PropertyType;

/**
 * a date value implementation.
 * 
 * @author Gennady Azarenkov
 */
public class DateValue extends BaseValue
{

   public static final int TYPE = PropertyType.DATE;

   /**
    * Constructs a <code>DateValue</code> object representing a date.
    * 
    * @param date
    *          the date this <code>DateValue</code> should represent s
    */
   public DateValue(Calendar date) throws IOException
   {
      super(TYPE, new TransientValueData(date));
   }

   /**
    * Constructs a <code>DateValue</code> object representing a date.
    */
   DateValue(ValueData data) throws IOException
   {
      super(TYPE, data);
   }
}
