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

import org.exoplatform.services.jcr.dataflow.serialization.ObjectReader;
import org.exoplatform.services.jcr.dataflow.serialization.SerializationConstants;
import org.exoplatform.services.jcr.dataflow.serialization.UnknownClassIdException;
import org.exoplatform.services.jcr.datamodel.IllegalPathException;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by The eXo Platform SAS. <br/>Date:
 * 
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a>
 * @version $Id: TransientpropertyDataReader.java 111 2008-11-11 11:11:11Z serg $
 */
public class TransientPropertyDataReader
{

   private FileCleaner fileCleaner;

   private int maxBufferSize;

   private final ReaderSpoolFileHolder holder;

   /**
    * Constructor.
    * 
    * @param fileCleaner - FileCleaner.
    * @param maxBufferSize - MaxBufferSize.
    */
   public TransientPropertyDataReader(FileCleaner fileCleaner, int maxBufferSize, ReaderSpoolFileHolder holder)
   {
      this.fileCleaner = fileCleaner;
      this.maxBufferSize = maxBufferSize;
      this.holder = holder;
   }

   /**
    * Read and set TransientPropertyData object data.
    * 
    * @param in ObjectReader.
    * @return TransientPropertyData object.
    * @throws UnknownClassIdException If read Class ID is not expected or do not
    *           exist.
    * @throws IOException If an I/O error has occurred.
    */
   public TransientPropertyData read(ObjectReader in) throws UnknownClassIdException, IOException
   {
      // read id
      int key;
      if ((key = in.readInt()) != SerializationConstants.TRANSIENT_PROPERTY_DATA)
      {
         throw new UnknownClassIdException("There is unexpected class [" + key + "]");
      }

      // TransientItemData
      QPath qpath;
      try
      {
         String sQPath = in.readString();
         qpath = QPath.parse(sQPath);
      }
      catch (final IllegalPathException e)
      {
         throw new IOException("Deserialization error. " + e)
         {

            /**
             * {@inheritDoc}
             */
            @Override
            public Throwable getCause()
            {
               return e;
            }
         };
      }

      String identifier = in.readString();

      String parentIdentifier = null;
      if (in.readByte() == SerializationConstants.NOT_NULL_DATA)
      {
         parentIdentifier = in.readString();
      }

      int persistedVersion = in.readInt();
      // --------------

      int type = in.readInt();
      boolean multiValued = in.readBoolean();

      TransientPropertyData prop =
         new TransientPropertyData(qpath, identifier, persistedVersion, type, parentIdentifier, multiValued);

      if (in.readByte() == SerializationConstants.NOT_NULL_DATA)
      {

         int listSize = in.readInt();
         List<ValueData> values = new ArrayList<ValueData>();
         TransientValueDataReader rdr = new TransientValueDataReader(fileCleaner, maxBufferSize, holder);
         for (int i = 0; i < listSize; i++)
         {
            values.add(rdr.read(in));
         }
         prop.setValues(values);
      }

      return prop;
   }

}
