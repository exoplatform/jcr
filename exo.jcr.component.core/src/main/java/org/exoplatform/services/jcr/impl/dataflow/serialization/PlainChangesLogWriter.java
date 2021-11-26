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
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.serialization.ObjectWriter;
import org.exoplatform.services.jcr.dataflow.serialization.SerializationConstants;

import java.io.IOException;
import java.util.List;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>
 * Date:
 * 
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a>
 * @version $Id: PlainChangesLogWriter.java 111 2008-11-11 11:11:11Z serg $
 */
public class PlainChangesLogWriter
{

   /**
    * Write PlainChangesLog data.
    * 
    * @param out
    *          ObjectWriter
    * @param pcl
    *          PlainChangesLog ojbect
    * @throws IOException
    *           if any Exception is occurred
    */
   public void write(ObjectWriter out, PlainChangesLog pcl) throws IOException
   {
      // write id
      out.writeInt(SerializationConstants.PLAIN_CHANGES_LOG_IMPL);

      out.writeInt(pcl.getEventType());
      out.writeString(pcl.getSessionId());

      List<ItemState> list = pcl.getAllStates();

      int listSize = list.size();
      out.writeInt(listSize);
      ItemStateWriter wr = new ItemStateWriter();
      for (int i = 0; i < listSize; i++)
      {
         wr.write(out, list.get(i));
      }

   }

}
