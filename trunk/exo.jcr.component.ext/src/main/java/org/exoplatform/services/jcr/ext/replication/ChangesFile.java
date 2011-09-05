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
package org.exoplatform.services.jcr.ext.replication;

import org.exoplatform.commons.utils.PrivilegedFileHelper;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: FileDescriptor.java 111 2008-11-11 11:11:11Z rainf0x $
 */

public class ChangesFile implements Comparable<ChangesFile>
{
   /**
    * file. The File object to file.
    */
   private File file;

   /**
    * randomAccessFile. The RandomAccessFile object to file.
    */
   private RandomAccessFile randomAccessFile;

   /**
    * systemId. The system identification String.
    */
   private final String systemId;

   /**
    * totalPacketCount. The total packets.
    */
   private final long totalPacketCount;

   /**
    * Current count.
    */
   private long count = 0;

   /**
    * FileDescriptor constructor.
    * 
    * @param f
    *          the File object
    * @param systemId
    *          The system identification String
    * @param totalPacketCount
    *          the packet count.
    */
   public ChangesFile(File f, String systemId, long totalPacketCount)
   {
      this.file = f;
      this.systemId = systemId;
      this.totalPacketCount = totalPacketCount;
   }

   /**
    * getFile.
    * 
    * @return File return the File object
    */
   public File getFile()
   {
      return file;
   }

   /**
    * getSystemId.
    * 
    * @return String return the system identification String
    */
   public String getSystemId()
   {
      return systemId;
   }

   /**
    * {@inheritDoc}
    */
   public int compareTo(ChangesFile o)
   {
      return file.getName().compareTo(o.getFile().getName());
   }

   /**
    * Write data to file by offset.
    * 
    * @param offset
    *          - offset in file to store data.
    * @param data
    *          - byte[].
    * @throws IOException
    *           if IO exception occurs.
    */
   public synchronized void write(long offset, byte[] data) throws IOException
   {
      if (randomAccessFile == null)
      {
         randomAccessFile = PrivilegedFileHelper.randomAccessFile(file, "rw");
      }

      randomAccessFile.seek(offset);
      randomAccessFile.write(data);

      count++;
      if (isStored())
      {
         randomAccessFile.close();
      }

   }

   /**
    * Is file complete.
    * 
    * @return True if file completely written.
    */
   public boolean isStored()
   {
      return (count == totalPacketCount);
   }

}
