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
import java.io.OutputStream;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: ValueData.java 34445 2009-07-24 07:51:18Z dkatayev $
 */
public interface ValueData
{
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
    * Renders this value data as stream of bytes. <br>NOTE: client is responsible for closing this stream,
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
    * Read <code>length</code> bytes from the binary value at <code>position</code> to the
    * <code>stream</code>.
    * 
    * @param stream
    *          - destination OutputStream
    * @param length
    *          - data length to be read
    * @param position
    *          - position in value data from which the read will be performed
    * @return - The number of bytes, possibly zero, that were actually transferred
    * @throws IOException
    *           if read/write error occurs
    */
   long read(OutputStream stream, long length, long position) throws IOException;

   /**
    * Indicates whether some other ValueData is "equals to" this ValueData. Return "true" if <code>valueData</code>  
    * can be treated as equal to this ValueData. Otherwise "false" returned.<br>
    * 
    * This method assumes that ValueData.equals(Object) implemented also and uses this method to perform the check.  
    * 
    * @see java.lang.Object#equals(Object)
    * 
    * @param another another ValueData
    * @return boolean, "true" if this ValueData equals to another ValueData
    */
   boolean equals(ValueData another);
}
