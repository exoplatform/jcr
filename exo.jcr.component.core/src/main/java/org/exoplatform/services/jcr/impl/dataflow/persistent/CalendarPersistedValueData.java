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

package org.exoplatform.services.jcr.impl.dataflow.persistent;

import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.CalendarValueData;
import org.exoplatform.services.jcr.impl.util.JCRDateFormat;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Calendar;

import javax.jcr.ValueFormatException;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: CalendarPersistedValueData.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class CalendarPersistedValueData extends CalendarValueData implements PersistedValueData
{
   /**
    * Empty constructor for serialization.
    */
   public CalendarPersistedValueData()
   {
      super(0, null);
   }

   /**
    * CalendarPersistedValueData constructor.
    */
   public CalendarPersistedValueData(int orderNumber, Calendar value)
   {
      super(orderNumber, value);
   }

   /**
    * {@inheritDoc}
    */
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      orderNumber = in.readInt();

      byte[] data = new byte[in.readInt()];
      if (data.length > 0)
      {
         try
         {
            in.readFully(data);
            value = new JCRDateFormat().deserialize(new String(data, Constants.DEFAULT_ENCODING));
         }
         catch (ValueFormatException e)
         {
            throw new IOException("Deserialization data error", e);
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void writeExternal(ObjectOutput out) throws IOException
   {
      out.writeInt(orderNumber);

      byte[] data = new JCRDateFormat().serialize(value);
      out.writeInt(data.length);
      out.write(data);
   }
}
