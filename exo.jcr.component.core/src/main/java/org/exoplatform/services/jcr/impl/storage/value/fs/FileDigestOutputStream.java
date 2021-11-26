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

package org.exoplatform.services.jcr.impl.storage.value.fs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.DigestOutputStream;
import java.security.MessageDigest;

/**
 * Created by The eXo Platform SAS
 * 
 * Date: 14.07.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: FileDigestOutputStream.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class FileDigestOutputStream extends DigestOutputStream
{

   protected final File file;

   private boolean closed = false;

   private String digestHash = null;

   FileDigestOutputStream(File file, MessageDigest digest) throws IOException
   {
      super(new FileOutputStream(file), digest);
      this.file = file;
   }

   public File getFile()
   {
      return file;
   }

   public String getDigestHash()
   {
      if (digestHash == null)
      {
         StringBuilder hash = new StringBuilder();
         for (byte b : digest.digest())
         {
            int i = b & 0x000000FF;
            String hs = Integer.toHexString(i);
            if (hs.length() < 2)
               hash.append('0'); // pad with zero

            hash.append(hs);
         }
         if (closed)
            // save and use after if was closed
            return digestHash = hash.toString();
         else
            return hash.toString();
      }

      return digestHash;
   }

   @Override
   public void close() throws IOException
   {
      super.close();
      closed = true;
   }

}
