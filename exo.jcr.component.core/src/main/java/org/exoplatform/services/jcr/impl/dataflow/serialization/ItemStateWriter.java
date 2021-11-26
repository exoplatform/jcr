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

package org.exoplatform.services.jcr.impl.dataflow.serialization;

import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedNodeData;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedPropertyData;
import org.exoplatform.services.jcr.dataflow.serialization.ObjectWriter;
import org.exoplatform.services.jcr.dataflow.serialization.SerializationConstants;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.impl.Constants;

import java.io.IOException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>
 * Date:
 * 
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a>
 * @version $Id: ItemStateWriter.java 111 2008-11-11 11:11:11Z serg $
 */
public class ItemStateWriter
{

   /**
    * Write item state into file.
    * 
    * @param out
    *          ObjectWriter
    * @param itemState
    *          ItemState
    * @throws IOException
    *           if any Exception is occurred
    */
   public void write(ObjectWriter out, ItemState itemState) throws IOException
   {
      // write id
      out.writeInt(SerializationConstants.ITEM_STATE);

      out.writeInt(itemState.getState());
      out.writeBoolean(itemState.isPersisted());
      out.writeBoolean(itemState.isEventFire());

      if (itemState.getOldPath() == null)
      {
         out.writeInt(SerializationConstants.NULL_DATA);
      }
      else
      {
         out.writeInt(SerializationConstants.NOT_NULL_DATA);
         byte[] buf = itemState.getOldPath().getAsString().getBytes(Constants.DEFAULT_ENCODING);
         out.writeInt(buf.length);
         out.write(buf);
      }

      // write flag isNodeData and ItemData
      ItemData data = itemState.getData();

      boolean isNodeData = (data instanceof PersistedNodeData);
      out.writeBoolean(isNodeData);
      if (isNodeData)
      {
         PersistedNodeDataWriter wr = new PersistedNodeDataWriter();
         wr.write(out, (PersistedNodeData)data);
      }
      else
      {
         PersistedPropertyDataWriter wr = new PersistedPropertyDataWriter();
         wr.write(out, (PersistedPropertyData)data);
      }
   }

}
