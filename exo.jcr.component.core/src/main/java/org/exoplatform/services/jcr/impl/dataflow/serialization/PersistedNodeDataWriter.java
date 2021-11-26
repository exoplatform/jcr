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

import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedNodeData;
import org.exoplatform.services.jcr.dataflow.serialization.ObjectWriter;
import org.exoplatform.services.jcr.dataflow.serialization.SerializationConstants;
import org.exoplatform.services.jcr.datamodel.InternalQName;

import java.io.IOException;

/**
 * Created by The eXo Platform SAS. <br>Date:
 * 
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a>
 * @version $Id$
 */
public class PersistedNodeDataWriter
{

   /**
    * Write to stream all necessary object data.
    * 
    * @param out SerializationOutputStream.
    * @throws IOException If an I/O error has occurred.
    */
   public void write(ObjectWriter out, PersistedNodeData nodeData) throws IOException
   {
      // write id
      out.writeInt(SerializationConstants.PERSISTED_NODE_DATA);

      // TransientItemData
      out.writeString(nodeData.getQPath().getAsString());
      out.writeString(nodeData.getIdentifier());

      if (nodeData.getParentIdentifier() != null)
      {
         out.writeByte(SerializationConstants.NOT_NULL_DATA);
         out.writeString(nodeData.getParentIdentifier());
      }
      else
         out.writeByte(SerializationConstants.NULL_DATA);

      out.writeInt(nodeData.getPersistedVersion());
      // -------------------

      out.writeInt(nodeData.getOrderNumber());

      // primary type
      out.writeString(nodeData.getPrimaryTypeName().getAsString());

      // mixins
      InternalQName[] mixinNames = nodeData.getMixinTypeNames();

      if (mixinNames != null)
      {
         out.writeByte(SerializationConstants.NOT_NULL_DATA);
         out.writeInt(mixinNames.length);
         for (int i = 0; i < mixinNames.length; i++)
         {
            out.writeString(mixinNames[i].getAsString());
         }
      }
      else
      {
         out.writeByte(SerializationConstants.NULL_DATA);
      }

      ACLWriter wr = new ACLWriter();

      AccessControlList acl = nodeData.getACL();
      if (acl != null)
      {
         out.writeByte(SerializationConstants.NOT_NULL_DATA);
         wr.write(out, acl);
      }
      else
      {
         out.writeByte(SerializationConstants.NULL_DATA);
      }
   }

}
