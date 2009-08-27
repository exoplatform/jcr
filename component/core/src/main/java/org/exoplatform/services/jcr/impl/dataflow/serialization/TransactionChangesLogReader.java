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

import java.io.IOException;

import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.dataflow.serialization.ObjectReader;
import org.exoplatform.services.jcr.dataflow.serialization.SerializationConstants;
import org.exoplatform.services.jcr.dataflow.serialization.UnknownClassIdException;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;

/**
 * Created by The eXo Platform SAS. <br/>Date:
 * 
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a>
 * @version $Id: TransactionChangesLogReader.java 111 2008-11-11 11:11:11Z serg $
 */
public class TransactionChangesLogReader
{

   private FileCleaner fileCleaner;

   private int maxBufferSize;

   private ReaderSpoolFileHolder holder;

   /**
    * Constructor.
    */
   public TransactionChangesLogReader(FileCleaner fileCleaner, int maxBufferSize, ReaderSpoolFileHolder holder)
   {
      this.fileCleaner = fileCleaner;
      this.maxBufferSize = maxBufferSize;
      this.holder = holder;
   }

   /**
    * Read and set TransactionChangesLog data.
    * 
    * @param in ObjectReader.
    * @return TransactionChangesLog object.
    * @throws UnknownClassIdException If read Class ID is not expected or do not
    *           exist.
    * @throws IOException If an I/O error has occurred.
    */
   public TransactionChangesLog read(ObjectReader in) throws UnknownClassIdException, IOException
   {

      // read id
      int key;
      if ((key = in.readInt()) != SerializationConstants.TRANSACTION_CHANGES_LOG)
      {
         throw new UnknownClassIdException("There is unexpected class [" + key + "]");
      }

      TransactionChangesLog log = new TransactionChangesLog();
      if (in.readByte() == SerializationConstants.NOT_NULL_DATA)
      {
         log.setSystemId(in.readString());
      }

      while (in.readByte() == SerializationConstants.NOT_NULL_DATA)
      {
         PlainChangesLogReader rdr = new PlainChangesLogReader(fileCleaner, maxBufferSize, holder);
         PlainChangesLog pl = rdr.read(in);
         log.addLog(pl);
      }

      return log;
   }

}
