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
package org.exoplatform.services.jcr.ext.replication.storage;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by The eXo Platform SAS. <br/>
 * Date: 19.12.2008
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: ChangesFile.java 31768 2009-05-14 09:35:43Z pnedonosko $
 */
public interface ChangesFile
{

   /**
    * File checksum.
    * 
    * @return String return the check sum to file.
    */
   byte[] getChecksum();

   /**
    * Validate file CRC.
    * 
    * @throws InvalidChecksumException
    *           if orogonal and actual CRCis is not equals.
    */
   void validate() throws InvalidChecksumException;

   /**
    * getId.
    * 
    * @return long 
    *           return the id to changes file.
    */
   long getId();

   /**
    * getInputStream.
    *
    * @return InputStream
    *           return the input stream
    * @throws IOException
    *           will be generated IOException. 
    */
   InputStream getInputStream() throws IOException;

   /**
    * Delete file and its file-system storage.
    * 
    * @return boolean, true if delete successful.
    * @see java.io.File.delete()
    * @throws IOException
    *           on error
    */
   boolean delete() throws IOException;

   /**
    * getLength.
    *
    * @return long
    *           return the length of changes file 
    */
   long getLength();
}
