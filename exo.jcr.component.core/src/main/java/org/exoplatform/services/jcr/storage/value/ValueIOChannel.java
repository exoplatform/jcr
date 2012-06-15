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
package org.exoplatform.services.jcr.storage.value;

import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.dataflow.SpoolConfig;
import org.exoplatform.services.jcr.impl.storage.value.ValueDataNotFoundException;

import java.io.IOException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: ValueIOChannel.java 14100 2008-05-12 10:53:47Z gazarenkov $
 */

public interface ValueIOChannel
{

   /**
    * Read Property value.
    * 
    * @param propertyId
    *          - Property ID
    * @param orderNumber
    *          value data order number
    * @param type
    *          property type                  
    * @param SpoolConfig
    *          spool configuration
    * @return ValueData
    * @throws IOException
    *           if error occurs
    */
   ValueData read(String propertyId, int orderNumber, int type, SpoolConfig spoolConfig)
      throws IOException;

   /**
    * Inspects whether corresponding file exists in value storage or not.
    * 
    * @param propertyId 
    *          Property ID
    * @param orderNumber 
    *          Property order number
    * @throws ValueDataNotFoundException is thrown if file not exist
    * @throws IOException is thrown if another IO error is occurred
    */
   void checkValueData(String propertyId, int orderNumber) throws ValueDataNotFoundException, IOException;

   /**
    * Repair value data by creation new corresponding empty file.
    * 
    * @param propertyId 
    *          Property ID
    * @param orderNumber 
    *          Property order number
    * @throws IOException is thrown if can not create new empty file
    */
   void repairValueData(String propertyId, int orderNumber) throws IOException;

   /**
    * Add or update Property value.
    * 
    * @param propertyId
    *          - Property ID
    * @param data
    *          - ValueData
    * @throws IOException
    *           if error occurs
    */
   void write(String propertyId, ValueData data) throws IOException;

   /**
    * Delete Property all values.
    * 
    * @param propertyId
    *          - Property ID
    * @throws IOException
    *           if error occurs
    */
   void delete(String propertyId) throws IOException;

   /**
    * Closes channel.
    */
   void close();

   /**
    * Return this value storage id.
    */
   String getStorageId();

   /**
    * Prepare channel changes.
    * 
    * @throws IOException
    *           if error occurs
    */
   void prepare() throws IOException;
   
   /**
    * Commit channel changes (one phase).
    * 
    * @throws IOException
    *           if error occurs
    */
   void commit() throws IOException;

   /**
    * Commit channel changes (two phases).
    * 
    * @throws IOException
    *           if error occurs
    */
   void twoPhaseCommit() throws IOException;

   /**
    * Rollback channel changes.
    * 
    * @throws IOException
    *           if error occurs
    */
   void rollback() throws IOException;
}
