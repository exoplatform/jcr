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
package org.exoplatform.services.jcr.impl.dataflow;

import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.util.JCRDateFormat;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;

import javax.jcr.ValueFormatException;

/**
 * Created by The eXo Platform SAS. <br/>
 * 
 * Helper to make ValueData conversion in one place.
 * 
 * Convert bytes to types
 * <ul>
 * <li>String</li>
 * <li>Long</li>
 * <li>Double</li>
 * <li>Calendar</li>
 * <li>Boolean</li>
 * </ul>
 * 
 * To make conversion to Name or Path use ValueFactory which covers conversion using
 * LocationFactory.
 * 
 * Candidate to ValueDataFactory.
 * 
 * Date: 13.05.2008 <br/>
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: ValueDataConvertor.java 14464 2008-05-19 11:05:20Z pnedonosko $
 */
public class ValueDataConvertor
{

   public static String readString(ValueData value) throws UnsupportedEncodingException, IllegalStateException,
      IOException
   {
      return new String(value.getAsByteArray(), Constants.DEFAULT_ENCODING);
   }

   public static Calendar readDate(ValueData value) throws UnsupportedEncodingException, IllegalStateException,
      IOException, ValueFormatException
   {
      return new JCRDateFormat().deserialize(new String(value.getAsByteArray(), Constants.DEFAULT_ENCODING));
   }

   public static long readLong(ValueData value) throws NumberFormatException, UnsupportedEncodingException,
      IllegalStateException, IOException
   {
      return Long.valueOf(new String(value.getAsByteArray(), Constants.DEFAULT_ENCODING)).longValue();
   }

   public static double readDouble(ValueData value) throws NumberFormatException, UnsupportedEncodingException,
      IllegalStateException, IOException
   {
      return Double.valueOf(new String(value.getAsByteArray(), Constants.DEFAULT_ENCODING)).doubleValue();
   }

   public static boolean readBoolean(ValueData value) throws UnsupportedEncodingException, IllegalStateException,
      IOException
   {
      return Boolean.valueOf(new String(value.getAsByteArray(), Constants.DEFAULT_ENCODING)).booleanValue();
   }

   public static InternalQName readQName(ValueData value) throws UnsupportedEncodingException, IllegalNameException,
      IOException
   {
      return InternalQName.parse(readString(value));
   }

}
