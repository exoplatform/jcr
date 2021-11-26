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

import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.dataflow.serialization.ObjectReader;
import org.exoplatform.services.jcr.dataflow.serialization.SerializationConstants;
import org.exoplatform.services.jcr.dataflow.serialization.UnknownClassIdException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by The eXo Platform SAS. <br>Date:
 * 
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a>
 * @version $Id: ACLReader.java 111 2008-11-11 11:11:11Z serg $
 */
public class ACLReader
{

   /**
    * Read and set AccessControlList data.
    * 
    * @param in ObjectReader.
    * @return AccessControlList object.
    * @throws UnknownClassIdException If read Class ID is not expected or do not
    *           exist.
    * @throws IOException If an I/O error has occurred.
    */
   public AccessControlList read(ObjectReader in) throws UnknownClassIdException, IOException
   {
      // read id
      int key;
      if ((key = in.readInt()) != SerializationConstants.ACCESS_CONTROL_LIST)
      {
         throw new UnknownClassIdException("There is unexpected class [" + key + "]");
      }

      // reading owner
      String owner;
      if (in.readByte() == SerializationConstants.NOT_NULL_DATA)
      {
         owner = in.readString();
      }
      else
      {
         owner = null;
      }

      List<AccessControlEntry> accessList = new ArrayList<AccessControlEntry>();
      // reading access control entrys size
      int listSize = in.readInt();
      for (int i = 0; i < listSize; i++)
      {
         // reading access control entrys identity
         String ident = in.readString();
         // reading permission
         String perm = in.readString();

         accessList.add(new AccessControlEntry(ident, perm));
      }

      return new AccessControlList(owner, accessList);
   }

}
