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

package org.exoplatform.services.jcr.core.value;

import java.io.IOException;
import java.io.OutputStream;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: ReadableBinaryValue.java 11907 2008-03-13 15:36:21Z ksm $
 */
public interface ReadableBinaryValue extends ExtendedValue
{

   /**
    * Read <code>length</code> bytes from the binary value at <code>position</code> to the
    * <code>stream</code>.
    * 
    * @param stream
    *          - destenation OutputStream
    * @param length
    *          - data length to be read
    * @param position
    *          - position in value data from which the read will be performed
    * @return - The number of bytes, possibly zero, that were actually transferred
    * @throws IOException
    * @throws RepositoryException
    */
   long read(OutputStream stream, long length, long position) throws IOException, RepositoryException;

}
