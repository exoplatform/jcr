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
package org.exoplatform.services.jcr.datamodel;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: ValueData.java 34445 2009-07-24 07:51:18Z dkatayev $
 */
public interface ValueData
{

   /**
    * Set Value order number.
    * 
    * @param orderNum
    *          int, Value order number
    */
   void setOrderNumber(int orderNum);

   /**
    * Return Value order number.
    * 
    * @return number of this value (values should be ordered)
    */
   int getOrderNumber();

   /**
    * Tell is this Value backed by bytes array.
    * 
    * @return true if data rendered as byte array, false otherwise
    */
   boolean isByteArray();

   /**
    * Renders this value data as array of bytes.
    * 
    * @return byte[], this value data as array of bytes
    * @throws IllegalStateException
    *           if cannot return this Value as array of bytes
    * @throws IOException
    *           if I/O error occurs
    */
   byte[] getAsByteArray() throws IllegalStateException, IOException;

   /**
    * Renders this value data as stream of bytes NOTE: client is responsible for closing this stream,
    * else IllegalStateException occurs in close().
    * 
    * @return InputStream, this value data as stream of bytes
    * @throws IOException
    *           if I/O error occurs
    */
   InputStream getAsStream() throws IOException;

   /**
    * Return this data length in bytes.
    * 
    * @return long
    */
   long getLength();

   /**
    * Tell if this ValueData is transient (not saved).<br/>
    * It means this ValueData instance of <code>TransientValueData<code/>.
    * 
    * @return boolean, true if ValueData is transient, false - otherwise
    */
   boolean isTransient();
}
