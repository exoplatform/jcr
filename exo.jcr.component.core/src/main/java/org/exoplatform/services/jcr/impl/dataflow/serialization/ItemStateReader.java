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
import org.exoplatform.services.jcr.dataflow.serialization.ObjectReader;
import org.exoplatform.services.jcr.dataflow.serialization.SerializationConstants;
import org.exoplatform.services.jcr.dataflow.serialization.UnknownClassIdException;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;

import java.io.EOFException;
import java.io.IOException;
import java.io.StreamCorruptedException;

/**
 * Created by The eXo Platform SAS. <br/>
 * Date:
 * 
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a>
 * @version $Id: ItemStateReader.java 111 2008-11-11 11:11:11Z serg $
 */
public class ItemStateReader
{

   /**
    * File cleaner.
    */
   private FileCleaner fileCleaner;

   /**
    * Maximum buffer size.
    */
   private int maxBufferSize;

   /**
    * Spool file holder.
    */
   private ReaderSpoolFileHolder holder;

   /**
    * ItemStateReader constructor.
    * 
    * @param fileCleaner
    * @param maxBufferSize
    * @param holder
    */
   public ItemStateReader(FileCleaner fileCleaner, int maxBufferSize, ReaderSpoolFileHolder holder)
   {
      this.fileCleaner = fileCleaner;
      this.maxBufferSize = maxBufferSize;
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

         boolean isNodeData = in.readBoolean();

         if (isNodeData)
         {
            PresistedNodeDataReader rdr = new PresistedNodeDataReader();
            is = new ItemState(rdr.read(in), state, eventFire, null, false, isPersisted);
         }
         else
         {
            PersistedPropertyDataReader rdr = new PersistedPropertyDataReader(fileCleaner, maxBufferSize, holder);
            is = new ItemState(rdr.read(in), state, eventFire, null, false, isPersisted);
         }
         return is;
      }
      catch (EOFException e)
      {
         throw new StreamCorruptedException("Unexpected EOF in middle of data block.");
      }
   }
}
