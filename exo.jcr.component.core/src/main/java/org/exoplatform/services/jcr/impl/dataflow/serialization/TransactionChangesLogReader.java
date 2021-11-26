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

import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.dataflow.serialization.ObjectReader;
import org.exoplatform.services.jcr.dataflow.serialization.SerializationConstants;
import org.exoplatform.services.jcr.dataflow.serialization.UnknownClassIdException;
import org.exoplatform.services.jcr.impl.dataflow.SpoolConfig;

import java.io.IOException;

/**
 * Created by The eXo Platform SAS. <br>Date:
 * 
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a>
 * @version $Id: TransactionChangesLogReader.java 111 2008-11-11 11:11:11Z serg $
 */
public class TransactionChangesLogReader
{

   /**
    * Spool config.
    */
   private final SpoolConfig spoolConfig;

   private ReaderSpoolFileHolder holder;

   /**
    * TransactionChangesLogReader constructor.
    */
   public TransactionChangesLogReader(SpoolConfig spoolConfig, ReaderSpoolFileHolder holder)
   {
      this.spoolConfig = spoolConfig;
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
         PlainChangesLogReader rdr = new PlainChangesLogReader(holder, spoolConfig);
         PlainChangesLog pl = rdr.read(in);
         log.addLog(pl);
      }

      return log;
   }

}
