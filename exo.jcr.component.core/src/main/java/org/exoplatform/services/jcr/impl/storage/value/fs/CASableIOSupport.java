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

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by The eXo Platform SAS.
 * 
 * CAS IO support covers some work will be produced in target FileIOChannels to make them
 * CASeable.<br/> - add value - delete value -
 * 
 * Date: 15.07.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: CASableIOSupport.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class CASableIOSupport
{

   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.CASeableIOSupport");

   public static final int HASHFILE_ORDERNUMBER = 0;

   protected final FileIOChannel channel;

   protected final String digestAlgo;

   CASableIOSupport(FileIOChannel channel, String digestAlgo)
   {
      this.channel = channel;
      this.digestAlgo = digestAlgo;
   }

   /**
    * Open digester output.<br/> Digester output will write into given file and calc hash for a
    * content.
    * 
    * @param file
    *          - destenation file
    * @return - digester output stream
    * @throws IOException
    */
   public FileDigestOutputStream openFile(File file) throws IOException
   {
      MessageDigest md;
      try
      {
         md = MessageDigest.getInstance(digestAlgo);
      }
      catch (NoSuchAlgorithmException e)
      {
         LOG.error("Can't wriet using " + digestAlgo + " algorithm, " + e, e);
         throw new IOException(e.getMessage(), e);
      }

      return new FileDigestOutputStream(file, md);
   }

   /**
    * Construct file name of given hash.
    * 
    * @param hash String
    *          - digester hash
    */
   public File getFile(String hash)
   {
      // work with digest
      return new File(channel.rootDir, channel.makeFilePath(hash, 0));
   }
}
