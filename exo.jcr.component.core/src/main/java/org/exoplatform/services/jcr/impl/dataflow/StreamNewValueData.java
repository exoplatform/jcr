/*
 * Copyright (C) 2012 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.dataflow;

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.FilePersistedValueData;
import org.exoplatform.services.jcr.impl.util.io.SpoolFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: NewValueData.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class StreamNewValueData extends AbstractNewValueData
{

   protected InputStream tmpStream;

   protected SpoolFile spoolFile;

   protected final boolean closeTmpStream;

   protected final SpoolConfig spoolConfig;

   /**
    * User for read(...) method
    */
   protected FileChannel spoolChannel;

   protected boolean spooled = false;

   protected byte[] data;

   /**
    * StreamNewValueData constructor.
    */
   protected StreamNewValueData(int orderNumber, InputStream stream, SpoolFile spoolFile, SpoolConfig spoolConfig,
      boolean closeTmpStream) throws IOException
   {
      super(orderNumber);
      this.tmpStream = stream;
      this.closeTmpStream = closeTmpStream;
      this.spoolFile = spoolFile;
      this.spoolConfig = spoolConfig;

      if (spoolFile != null)
      {
         spoolFile.acquire(this);

         if (this.tmpStream != null)
         {
            this.tmpStream.close();
            this.tmpStream = null;
         }

         this.spooled = true;
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public byte[] getAsByteArray() throws IOException
   {
      if (isByteArrayAfterSpool())
      {
         return data;
      }
      else
      {
         return fileToByteArray();
      }
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public InputStream getAsStream() throws IOException
   {
      if (isByteArrayAfterSpool())
      {
         return new ByteArrayInputStream(data); // from bytes
      }
      else
      {
         if (spoolFile != null)
         {
            return PrivilegedFileHelper.fileInputStream(spoolFile); // from spool file
         }
         else
         {
            throw new IllegalArgumentException("Stream already consumed");
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public long getLength()
   {
      if (isByteArrayAfterSpool())
      {
         return data.length;
      }
      else
      {
         return PrivilegedFileHelper.length(spoolFile);
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isByteArray()
   {
      return data != null;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public long read(OutputStream stream, long length, long position) throws IOException
   {
      if (isByteArrayAfterSpool())
      {
         return super.read(stream, length, position);
      }
      else
      {
         if (spoolChannel == null)
         {
            spoolChannel = PrivilegedFileHelper.fileInputStream(spoolFile).getChannel();
         }

         validate(length, position, spoolChannel.size());
         length = adjustReadLength(length, position, spoolChannel.size());

         MappedByteBuffer bb = spoolChannel.map(FileChannel.MapMode.READ_ONLY, position, length);

         WritableByteChannel ch = Channels.newChannel(stream);
         ch.write(bb);
         ch.close();

         return length;
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void finalize() throws Throwable
   {
      removeSpoolFile();
   }

   /**
    * Tell is this Value backed by bytes array before or after spooling.
    */
   private boolean isByteArrayAfterSpool()
   {
      if (data != null)
      {
         return true;
      }
      else
      {
         spoolInputStream();

         return data != null;
      }
   }

   /**
    * Spool ValueData temp InputStream to a temp File.
    */
   protected void spoolInputStream()
   {
      if (spooled || tmpStream == null) // already spooled
      {
         return;
      }

      byte[] buffer = new byte[0];
      byte[] tmpBuff = new byte[2048];
      int read = 0;
      int len = 0;
      SpoolFile sf = null;
      OutputStream sfout = null;

      try
      {
         while ((read = tmpStream.read(tmpBuff)) >= 0)
         {
            if (sfout != null)
            {
               // spool to temp file
               sfout.write(tmpBuff, 0, read);
               len += read;
            }
            else if (len + read > spoolConfig.maxBufferSize)
            {
               // threshold for keeping data in memory exceeded,
               // if have a fileCleaner create temp file and spool buffer contents.
               sf = SpoolFile.createTempFile("jcrvd", null, spoolConfig.tempDirectory);
               sf.acquire(this);

               sfout = PrivilegedFileHelper.fileOutputStream(sf);

               sfout.write(buffer, 0, len);
               sfout.write(tmpBuff, 0, read);
               buffer = null;
               len += read;
            }
            else
            {
               // reallocate new buffer and spool old buffer contents
               byte[] newBuffer = new byte[len + read];
               System.arraycopy(buffer, 0, newBuffer, 0, len);
               System.arraycopy(tmpBuff, 0, newBuffer, len, read);
               buffer = newBuffer;
               len += read;
            }
         }

         if (sf != null)
         {
            // spooled to file
            this.spoolChannel = null;
            this.spoolFile = sf;
            this.data = null;
         }
         else
         {
            // ...bytes
            this.spoolChannel = null;
            this.spoolFile = null;
            this.data = buffer;
         }

         this.spooled = true;
      }
      catch (IOException e)
      {
         if (sf != null)
         {
            try
            {
               sf.release(this);
               spoolConfig.fileCleaner.addFile(sf);
            }
            catch (FileNotFoundException ex)
            {
               if (LOG.isDebugEnabled())
               {
                  LOG.debug("Could not remove temporary file : " + sf.getAbsolutePath());
               }
            }
         }

         throw new IllegalStateException("Error of spooling to temp file from " + tmpStream, e);
      }
      finally
      {
         try
         {
            if (sfout != null)
            {
               sfout.close();
            }
         }
         catch (IOException e)
         {
            LOG.error("Error of spool output close.", e);
         }

         if (this.closeTmpStream)
         {
            try
            {
               this.tmpStream.close();
            }
            catch (IOException e)
            {
               LOG.error("Error of source input close.", e);
            }
         }
         this.tmpStream = null;
      }
   }

   /**
    * Convert File to byte array. <br/>
    * WARNING: Potential lack of memory due to call getAsByteArray() on stream data.
    * 
    * @return byte[] bytes array
    */
   private byte[] fileToByteArray() throws IOException
   {
      FileChannel fch = PrivilegedFileHelper.fileInputStream(spoolFile).getChannel();

      if (LOG.isDebugEnabled() && fch.size() > spoolConfig.maxBufferSize)
      {
         LOG.debug("Potential lack of memory due to call getAsByteArray() on stream data exceeded " + fch.size()
            + " bytes");
      }

      try
      {
         ByteBuffer bb = ByteBuffer.allocate((int)fch.size());
         fch.read(bb);
         if (bb.hasArray())
         {
            return bb.array();
         }
         else
         {
            // impossible code in most cases, as we use heap backed buffer
            byte[] tmpb = new byte[bb.capacity()];
            bb.get(tmpb);
            return tmpb;
         }
      }
      finally
      {
         fch.close();
      }
   }

   /**
    * Delete current spool file.
    * 
    * @throws IOException
    *           if error
    */
   private void removeSpoolFile() throws IOException
   {
      if (spoolChannel != null)
      {
         spoolChannel.close();
      }

      if (spoolFile != null)
      {
         if (spoolFile instanceof SpoolFile)
         {
            (spoolFile).release(this);
         }

         if (PrivilegedFileHelper.exists(spoolFile))
         {
            if (!PrivilegedFileHelper.delete(spoolFile))
            {
               spoolConfig.fileCleaner.addFile(spoolFile);

               if (LOG.isDebugEnabled())
               {
                  LOG.debug("Could not remove file. Add to fileCleaner "
                     + PrivilegedFileHelper.getAbsolutePath(spoolFile));
               }
            }
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   protected byte[] spoolInternalValue()
   {
      throw new UnsupportedOperationException("Method is not supported");
   }

   /**
    * {@inheritDoc}
    */
   protected boolean internalEquals(ValueData another)
   {
      if (another instanceof StreamNewValueData)
      {
         StreamNewValueData streamValue = (StreamNewValueData)another;

         if (isByteArray() == another.isByteArray())
         {
            if (isByteArray())
            {
               return Arrays.equals(streamValue.data, data);
            }
            else
            {
               if (streamValue.tmpStream == tmpStream)
               {
                  return true;
               }

               return spoolFile != null ? spoolFile.equals(streamValue.spoolFile) : false;
            }
         }
      }
      else if (another instanceof FilePersistedValueData)
      {
         File dataFile = ((FilePersistedValueData)another).getFile();
         return spoolFile != null ? spoolFile.equals(dataFile) : false;
      }

      return false;
   }
}

