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

import org.exoplatform.services.jcr.datamodel.IllegalPathException;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.persistent.PersistedValueData;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: ReferencePersistedValueData.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class PathPersistedValueData extends PathValueData implements PersistedValueData
{
   /**
    * Empty constructor for serialization.
    */
   public PathPersistedValueData()
   {
      super(0, null);
   }

   /**
    * PathPersistedValueData constructor.
    */
   public PathPersistedValueData(int orderNumber, QPath value)
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
         in.readFully(data);

         try
         {
            value = QPath.parse(new String(data, Constants.DEFAULT_ENCODING));
         }
         catch (IllegalPathException e)
         {
            throw new IOException(e.getMessage(), e);
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void writeExternal(ObjectOutput out) throws IOException
   {
      out.writeInt(orderNumber);

      byte[] data = value.getAsString().getBytes(Constants.DEFAULT_ENCODING);
      out.writeInt(data.length);

      if (data.length > 0)
      {
         out.write(data);
      }
   }
}
