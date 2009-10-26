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

import org.exoplatform.services.jcr.dataflow.ChangesLogIterator;
import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.dataflow.serialization.ObjectWriter;
import org.exoplatform.services.jcr.dataflow.serialization.SerializationConstants;

import java.io.IOException;

/**
 * Created by The eXo Platform SAS. <br/>Date:
 * 
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a>
 * @version $Id: TransactionChangesLogWriter.java 111 2008-11-11 11:11:11Z serg $
 */
public class TransactionChangesLogWriter
{

   public void write(ObjectWriter out, TransactionChangesLog tcl) throws IOException
   {
      // write id
      out.writeInt(SerializationConstants.TRANSACTION_CHANGES_LOG);

      if (tcl.getSystemId() != null)
      {
         out.writeByte(SerializationConstants.NOT_NULL_DATA);
         out.writeString(tcl.getSystemId());
      }
      else
      {
         out.writeByte(SerializationConstants.NULL_DATA);
      }

      ChangesLogIterator it = tcl.getLogIterator();
      PlainChangesLogWriter wr = new PlainChangesLogWriter();
      while (it.hasNextLog())
      {
         out.writeByte(SerializationConstants.NOT_NULL_DATA);
         wr.write(out, it.nextLog());
      }
      out.writeByte(SerializationConstants.NULL_DATA);
   }

}
