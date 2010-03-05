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

import org.exoplatform.services.jcr.dataflow.serialization.ObjectWriter;
import org.exoplatform.services.jcr.dataflow.serialization.SerializationConstants;
import org.exoplatform.services.jcr.impl.dataflow.AbstractPersistedValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.FilePersistedValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.StreamPersistedValueData;
import org.exoplatform.services.jcr.util.IdGenerator;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by The eXo Platform SAS. <br/>
 * Date:
 * 
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a>
 * @version $Id$
 */
public class PersistedValueDataWriter
{

   /**
    * Write to stream all necessary object data.
    * 
    * @param out
    *          SerializationOutputStream.
    * @throws IOException
    *           If an I/O error has occurred.
    */
   public void write(ObjectWriter out, AbstractPersistedValueData vd) throws IOException
   {
      // write id
      out.writeInt(SerializationConstants.PERSISTED_VALUE_DATA);

      out.writeInt(vd.getOrderNumber());

      boolean isByteArray = vd.isByteArray();
      out.writeBoolean(isByteArray);

      if (isByteArray)
      {
         byte[] data = vd.getAsByteArray();
         int f = data.length;
         out.writeInt(f);
         out.write(data);
      }
      else
      {
         // write file content
         FilePersistedValueData streamed = (FilePersistedValueData)vd;

         InputStream in = null;
         
         if (streamed.getFile() == null && vd instanceof StreamPersistedValueData)
         {
            in = new FileInputStream(((StreamPersistedValueData)vd).getTempFile());
         }
         else
         {
           in = streamed.getAsStream();
         }

         // TODO optimize it, use channels
         if (streamed.getFile() instanceof SerializationSpoolFile)
         {
            SerializationSpoolFile ssf = (SerializationSpoolFile)streamed.getFile();
            out.writeString(ssf.getId());
            out.writeLong(0);
         }
         else
         {
            // write property id - used for reread data optimization
            String id = IdGenerator.generate();
            out.writeString(id);

            out.writeLong(vd.getLength());
            try
            {
               byte[] buf = new byte[SerializationConstants.INTERNAL_BUFFER_SIZE];
               int l = 0;
               while ((l = in.read(buf)) >= 0)
                  out.write(buf, 0, l);
            }
            finally
            {
               in.close();
            }
         }
      }
   }
}
