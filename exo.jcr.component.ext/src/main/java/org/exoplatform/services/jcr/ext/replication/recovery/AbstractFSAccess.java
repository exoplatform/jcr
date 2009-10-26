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
package org.exoplatform.services.jcr.ext.replication.recovery;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: AbstractFSAccess.java 34445 2009-07-24 07:51:18Z dkatayev $
 */

public class AbstractFSAccess
{
   /**
    * Definition the name for data folder.
    */
   public static final String DATA_DIR_NAME = "data";

   /**
    * Definition the prefix string for removed data.
    */
   public static final String PREFIX_REMOVED_DATA = "---";

   /**
    * Definition the prefix char.
    */
   public static final String PREFIX_CHAR = "-";

   /**
    * Definition the suffix string for removed files.
    */
   public static final String REMOVED_SUFFIX = ".remove";

   /**
    * Definition the constant for 1k buffer.
    */
   protected static final int BUFFER_1KB = 1024;

   /**
    * Definition the constant for 8k buffer.
    */
   protected static final int BUFFER_8X = 8;

   /**
    * Definition the constant for 20k buffer.
    */
   protected static final int BUFFER_20X = 20;

   /**
    * getAsFile. The input stream will be spooled to file.
    * 
    * @param is
    *          the InputStream
    * @return File return the spooled file
    * @throws IOException
    *           will be generated the IOExceprion
    */
   protected File getAsFile(InputStream is) throws IOException
   {
      byte[] buf = new byte[BUFFER_1KB * BUFFER_20X];

      File tempFile = File.createTempFile("" + System.currentTimeMillis(), "" + System.nanoTime());
      FileOutputStream fos = new FileOutputStream(tempFile);
      int len;

      while ((len = is.read(buf)) > 0)
         fos.write(buf, 0, len);

      fos.flush();
      fos.close();

      return tempFile;
   }

   /**
    * getAsFile.
    * 
    * @param ois
    *          the ObjectInputStream
    * @param fileSize
    *          will be read 'fileSize' bytes from stream
    * @return File return the file with data
    * @throws IOException
    *           will be generated the IOException
    */
   protected File getAsFile(ObjectInputStream ois, long fileSize) throws IOException
   {
      int bufferSize = BUFFER_1KB * BUFFER_8X;
      byte[] buf = new byte[bufferSize];

      File tempFile = File.createTempFile("" + System.currentTimeMillis(), "" + System.nanoTime());
      FileOutputStream fos = new FileOutputStream(tempFile);
      long readBytes = fileSize;

      while (readBytes > 0)
      {
         if (readBytes >= bufferSize)
         {
            ois.readFully(buf);
            fos.write(buf);
         }
         else if (readBytes < bufferSize)
         {
            ois.readFully(buf, 0, (int)readBytes);
            fos.write(buf, 0, (int)readBytes);
         }
         readBytes -= bufferSize;
      }

      fos.flush();
      fos.close();

      return tempFile;
   }
}
