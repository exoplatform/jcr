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

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by The eXo Platform SAS. <br/>
 * Date: 19.12.2008
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: ChangesFile.java 27525 2009-01-28 00:01:58Z pnedonosko $
 */
public class RandomChangesFile implements ChangesFile
{

   /**
    * The logger.
    */
   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.ext.RandomChangesFile");

   /**
    * Checksum to file (set in constructor).
    */
   private final byte[] crc;

   /**
    * Checksum calculated form content of file.
    */
   private byte[] crcCalc = null;

   /**
    * Time stamp to ChangesLog.
    */
   private final long id;

   /**
    * The file with data.
    */
   private final File file;

   /**
    * The resources holder.
    */
   private final ResourcesHolder resHolder;

   /**
    * The random access file.
    */
   private RandomAccessFile fileAccessor;

   /**
    * MessageDigest will be used for calculating MD5 checksum.
    */
   private MessageDigest digest;

   /**
    * Create ChangesFile with already formed file.
    * 
    * @param file
    *          changes file
    * @param crc
    *          checksum
    * @param id
    *          the id to changes file
    * @param resHolder
    *          ResourcesHolde, the resources holder.
    * @throws NoSuchAlgorithmException
    *           will be generated the exception NoSuchAlgorithmException
    */
   public RandomChangesFile(File file, byte[] crc, long id, ResourcesHolder resHolder) throws NoSuchAlgorithmException
   {
      this.crc = crc;
      this.id = id;
      this.file = file;
      this.resHolder = resHolder;
      this.digest = MessageDigest.getInstance("MD5");
   }

   /**
    * {@inheritDoc}
    */
   public byte[] getChecksum()
   {
      return crc;
   }

   /**
    * {@inheritDoc}
    */
   public InputStream getInputStream() throws IOException
   {
      finishWrite();

      InputStream in = new FileInputStream(file);
      resHolder.add(in);
      return in;
   }

   /**
    * Write data to file.
    * 
    * @param data
    *          byte buffer
    * @param position
    *          to write
    * @throws IOException
    *           will be generated the exception IOException 
    */
   public void writeData(byte[] data, long position) throws IOException
   {
      checkFileAccessor();
      synchronized (fileAccessor)
      {
         // write to file
         fileAccessor.seek(position);
         fileAccessor.write(data);
         // update digest
         digest.update(data);
      }
   }

   /**
    * Say internal writer that file write stopped.
    * 
    * @throws IOException
    *           error on file accessor close.
    */
   public void finishWrite() throws IOException
   {
      if (fileAccessor != null)
      {
         // close writer
         fileAccessor.close();
         fileAccessor = null;

         crcCalc = digest.digest();
         digest = null; // set to null - to prevent write to file again
      }
   }

   /**
    * Check is file accessor created. Create if not.
    * 
    * @throws IOException
    *           error on file accessor creation.
    */
   private void checkFileAccessor() throws IOException
   {
      if (fileAccessor == null)
      {
         fileAccessor = new RandomAccessFile(file, "rwd");

         resHolder.add(fileAccessor);

         // if (file.length() > 0) {
         // doTruncate = true;
         // }
         fileAccessor.seek(file.length());

         // LOG.info("checkFileAccessor - seek on " + file.length());
      }
   }

   /**
    * Delete file and its file-system storage.
    * 
    * @return boolean, true if delete successful.
    * @see java.io.File.delete()
    * @throws IOException
    *           on error
    */
   public boolean delete() throws IOException
   {
      finishWrite();
      return file.delete();
   }

   /**
    * {@inheritDoc}
    */
   public long getId()
   {
      return id;
   }

   /**
    * {@inheritDoc}
    */
   public String toString()
   {
      return file.getAbsolutePath();
   }

   /**
    * {@inheritDoc}
    */
   public long getLength()
   {
      return file.length();
   }

   /**
    * {@inheritDoc}
    */
   public void validate() throws InvalidChecksumException
   {
      if (crc == null || crc.length == 0)
      {
         throw new InvalidChecksumException("File checksum is null or empty.");
      }

      if (!java.util.Arrays.equals(crc, crcCalc))
      {
         throw new InvalidChecksumException("File content isn't match checksum.");
      }
   }

}
