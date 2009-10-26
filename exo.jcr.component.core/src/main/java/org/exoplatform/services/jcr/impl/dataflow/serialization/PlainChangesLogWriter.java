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

import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.serialization.ObjectWriter;
import org.exoplatform.services.jcr.dataflow.serialization.SerializationConstants;

import java.io.IOException;
import java.util.List;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
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
