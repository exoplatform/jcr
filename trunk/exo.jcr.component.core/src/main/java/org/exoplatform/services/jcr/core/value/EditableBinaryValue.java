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
package org.exoplatform.services.jcr.core.value;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua Sep
 * 7, 2007
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: EditableBinaryValue.java 11907 2008-03-13 15:36:21Z ksm $
 */
public interface EditableBinaryValue extends ReadableBinaryValue
{
   /**
    * Update with <code>length</code> bytes from the specified InputStream <code>stream</code> to
    * this binary value at <code>position</code>
    * 
    * @param stream
    *          the data.
    * @param length
    *          the number of bytes from buffer to write.
    * @param position
    *          position in file to write data
    * */
   void update(InputStream stream, long length, long position) throws IOException, RepositoryException;

   /**
    * Truncates binary value to <code> size </code>
    * 
    * @param size
    * @throws IOException
    */
   void setLength(long size) throws IOException, RepositoryException;
}
