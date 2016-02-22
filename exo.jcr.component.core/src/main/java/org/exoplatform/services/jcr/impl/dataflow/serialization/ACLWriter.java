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
package org.exoplatform.services.jcr.impl.dataflow.serialization;

import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.dataflow.serialization.ObjectWriter;
import org.exoplatform.services.jcr.dataflow.serialization.SerializationConstants;

import java.io.IOException;
import java.util.List;

/**
 * Created by The eXo Platform SAS. <br>
 * Date:
 * 
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a>
 * @version $Id: ACLWriter.java 111 2008-11-11 11:11:11Z serg $
 */
public class ACLWriter
{

   /**
    * Write AccessControlList data.
    * 
    * @param out
    *          The ObjectWriter
    * @param acl
    *          The AccessControlList object
    * @throws IOException
    *           If an I/O error has occurred.
    */
   public void write(ObjectWriter out, AccessControlList acl) throws IOException
   {
      // write id
      out.writeInt(SerializationConstants.ACCESS_CONTROL_LIST);

      // Writing owner
      String owner = acl.getOwner();
      if (owner != null)
      {
         out.writeByte(SerializationConstants.NOT_NULL_DATA);
         out.writeString(owner);
      }
      else
      {
         out.writeByte(SerializationConstants.NULL_DATA);
      }

      // writing access control entries size
      List<AccessControlEntry> accessList = acl.getPermissionEntries();
      out.writeInt(accessList.size());

      for (AccessControlEntry entry : accessList)
      {
         // writing access control entrys identity
         out.writeString(entry.getIdentity());
         // writing permission
         out.writeString(entry.getPermission());
      }
   }

}
