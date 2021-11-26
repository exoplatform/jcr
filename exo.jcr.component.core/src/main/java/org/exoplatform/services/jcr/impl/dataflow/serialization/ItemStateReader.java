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
import org.exoplatform.services.jcr.dataflow.serialization.ObjectReader;
import org.exoplatform.services.jcr.dataflow.serialization.SerializationConstants;
import org.exoplatform.services.jcr.dataflow.serialization.UnknownClassIdException;
import org.exoplatform.services.jcr.datamodel.IllegalPathException;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.SpoolConfig;

import java.io.EOFException;
import java.io.IOException;
import java.io.StreamCorruptedException;

/**
 * Created by The eXo Platform SAS. <br>
 * Date:
 * 
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a>
 * @version $Id: ItemStateReader.java 111 2008-11-11 11:11:11Z serg $
 */
public class ItemStateReader
{

   /**
    * SpoolConfig.
    */
   private SpoolConfig spoolConfig;

   /**
    * Spool file holder.
    */
   private ReaderSpoolFileHolder holder;

   /**
    * ItemStateReader constructor.
    */
   public ItemStateReader(ReaderSpoolFileHolder holder, SpoolConfig spoolConfig)
   {
      this.spoolConfig = spoolConfig;
      this.holder = holder;
   }

   /**
    * Read and set ItemState data.
    * 
    * @param in
    *          ObjectReader.
    * @return ItemState object.
    * @throws UnknownClassIdException
    *           If read Class ID is not expected or do not exist.
    * @throws IOException
    *           If an I/O error has occurred.
    */
   public ItemState read(ObjectReader in) throws UnknownClassIdException, IOException
   {
      // read id
      int key;
      if ((key = in.readInt()) != SerializationConstants.ITEM_STATE)
      {
         throw new UnknownClassIdException("There is unexpected class [" + key + "]");
      }

      ItemState is = null;
      try
      {
         int state = in.readInt();
         boolean isPersisted = in.readBoolean();
         boolean eventFire = in.readBoolean();

         QPath oldPath = null;
         if (in.readInt() == SerializationConstants.NOT_NULL_DATA)
         {
            byte[] buf = new byte[in.readInt()];
            in.readFully(buf);
            oldPath = QPath.parse(new String(buf, Constants.DEFAULT_ENCODING));
         }

         boolean isNodeData = in.readBoolean();
         if (isNodeData)
         {
            PersistedNodeDataReader rdr = new PersistedNodeDataReader();
            is = new ItemState(rdr.read(in), state, eventFire, null, false, isPersisted, oldPath);
         }
         else
         {
            PersistedPropertyDataReader rdr = new PersistedPropertyDataReader(holder, spoolConfig);
            is = new ItemState(rdr.read(in), state, eventFire, null, false, isPersisted, oldPath);
         }
         return is;
      }
      catch (EOFException e)
      {
         throw new StreamCorruptedException("Unexpected EOF in middle of data block.");
      }
      catch (IllegalPathException e)
      {
         throw new IOException("Data corrupted", e);
      }
   }
}
