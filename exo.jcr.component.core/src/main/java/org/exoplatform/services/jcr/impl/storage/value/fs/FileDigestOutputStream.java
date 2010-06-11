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
package org.exoplatform.services.jcr.impl.storage.value.fs;

import org.exoplatform.services.jcr.impl.util.io.PrivilegedFileHelper;

import java.io.File;
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
      super(PrivilegedFileHelper.fileOutputStream(file), digest);
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
