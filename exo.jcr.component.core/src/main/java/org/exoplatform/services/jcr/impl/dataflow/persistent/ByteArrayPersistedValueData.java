/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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

import org.exoplatform.services.jcr.impl.dataflow.ByteArrayValueData;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id$
 */
public class ByteArrayPersistedValueData extends ByteArrayValueData implements PersistedValueData
{
   /**
    * Empty constructor for serialization.
    */
   public ByteArrayPersistedValueData()
   {
      super(0, null);
   }

   /**
    * ByteArrayPersistedValueData constructor.
    */
   public ByteArrayPersistedValueData(int orderNumber, byte[] value)
   {
      super(orderNumber, value);
   }

   /**
    * {@inheritDoc}
    */
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      orderNumber = in.readInt();
      
      value = new byte[in.readInt()];
      if (value.length > 0)
      {
         in.readFully(value);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void writeExternal(ObjectOutput out) throws IOException
   {
      out.writeInt(orderNumber);
      out.writeInt(value.length);

      if (value.length > 0)
      {
         out.write(value);
      }
   }
}
