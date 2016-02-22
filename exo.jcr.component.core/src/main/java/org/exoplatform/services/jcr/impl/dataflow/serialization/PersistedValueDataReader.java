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

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.services.jcr.dataflow.serialization.ObjectReader;
import org.exoplatform.services.jcr.dataflow.serialization.SerializationConstants;
import org.exoplatform.services.jcr.dataflow.serialization.UnknownClassIdException;
import org.exoplatform.services.jcr.impl.dataflow.SpoolConfig;
import org.exoplatform.services.jcr.impl.dataflow.ValueDataUtil;
import org.exoplatform.services.jcr.impl.dataflow.persistent.PersistedValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.StreamPersistedValueData;
import org.exoplatform.services.jcr.impl.util.io.SpoolFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by The eXo Platform SAS. <br>
 * Date:
 * 
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a>
 * @version $Id$
 */
public class PersistedValueDataReader
{

   /**
    * ReadedSpoolFile holder,
    */
   private final ReaderSpoolFileHolder holder;

   private final SpoolConfig spoolConfig;

   /**
    * Constructor.
    * 
    * @param holder
    *          ReaderSpoolFileHolder
    */
   public PersistedValueDataReader(ReaderSpoolFileHolder holder, SpoolConfig spoolConfig)
   {
      this.holder = holder;
      this.spoolConfig = spoolConfig;
   }

   /**
    * Read and set PersistedValueData object data.
    * 
    * @param in
    *          ObjectReader.
    * @param type
    *          property type         
    * @return PersistedValueData object.
    * @throws UnknownClassIdException
    *           If read Class ID is not expected or do not exist.
    * @throws IOException
    *           If an I/O error has occurred.
    */
   public PersistedValueData read(ObjectReader in, int type) throws UnknownClassIdException, IOException
   {
      File tempDirectory = new File(SerializationConstants.TEMP_DIR);
      PrivilegedFileHelper.mkdirs(tempDirectory);

      // read id
      int key;
      if ((key = in.readInt()) != SerializationConstants.PERSISTED_VALUE_DATA)
      {
         throw new UnknownClassIdException("There is unexpected class [" + key + "]");
      }

      int orderNumber = in.readInt();

      boolean isByteArray = in.readBoolean();

      if (isByteArray)
      {
         byte[] data = new byte[in.readInt()];
         in.readFully(data);

         return ValueDataUtil.createValueData(type, orderNumber, data);
      }
      else
      {
         // read file id - used for reread data optimization
         String id = in.readString();
         // read file length
         long length = in.readLong();

         SerializationSpoolFile sf = holder.get(id);
         if (sf == null)
         {
            // Deleted ItemState usecase 
            if (length == SerializationConstants.NULL_FILE)
            {
               return new StreamPersistedValueData(orderNumber, (SerializationSpoolFile)null, spoolConfig);
            }
            sf = new SerializationSpoolFile(tempDirectory, id, holder);
            writeToFile(in, sf, length);
            holder.put(id, sf);
            return new StreamPersistedValueData(orderNumber, sf, spoolConfig);
         }
         else
         {
            sf.acquire(this); // workaround for AsyncReplication test
            try
            {
               PersistedValueData vd = new StreamPersistedValueData(orderNumber, sf, spoolConfig);

               // skip data in input stream
               if (in.skip(length) != length)
               {
                  throw new IOException("Content isn't skipped correctly.");
               }

               return vd;
            }
            finally
            {
               sf.release(this);
            }
         }
      }
   }

   private void writeToFile(ObjectReader src, SpoolFile dest, long length) throws IOException
   {
      // write data to file
      FileOutputStream sfout = PrivilegedFileHelper.fileOutputStream(dest);
      int bSize = SerializationConstants.INTERNAL_BUFFER_SIZE;
      try
      {
         byte[] buff = new byte[bSize];
         for (; length >= bSize; length -= bSize)
         {
            src.readFully(buff);
            sfout.write(buff);
         }

         if (length > 0)
         {
            buff = new byte[(int)length];
            src.readFully(buff);
            sfout.write(buff);
         }
      }
      finally
      {
         sfout.close();
      }
   }
}
