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
package org.exoplatform.services.jcr.impl.dataflow;

import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.datamodel.Identifier;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.util.JCRDateFormat;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.impl.util.io.SpoolFile;

import java.io.ByteArrayInputStream;
import java.io.Externalizable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.Calendar;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.<br/>
 * 
 * @author Gennady Azarenkov
 * @version $Id: TransientValueData.java 35209 2009-08-07 15:32:27Z pnedonosko $
 */
public class TransientValueData extends AbstractValueData implements Externalizable
{

   private static final long serialVersionUID = -5280857006905550884L;

   protected byte[] data;

   protected InputStream tmpStream;

   protected File spoolFile;

   protected final boolean closeTmpStream;

   /**
    * User for read(...) method
    */
   protected FileChannel spoolChannel;

   protected FileCleaner fileCleaner;

   protected int maxBufferSize;

   protected File tempDirectory;

   protected boolean spooled = false;

   protected boolean isTransient = true;

   private boolean deleteSpoolFile;

   /**
    * Convert String into bytes array using default encoding.
    */
   protected static byte[] stringToBytes(final String value)
   {
      try
      {
         return value.getBytes(Constants.DEFAULT_ENCODING);
      }
      catch (UnsupportedEncodingException e)
      {
         throw new RuntimeException("FATAL ERROR Charset " + Constants.DEFAULT_ENCODING + " is not supported!");
      }
   }

   /**
    * Creates TransientValueData with incoming byte array.
    * 
    * @param value
    *          byte[]
    * @param orderNumber
    *          int
    */
   public TransientValueData(byte[] value, int orderNumber)
   {
      super(orderNumber);
      this.data = value;
      this.deleteSpoolFile = true;
      this.closeTmpStream = false;
   }

   /**
    * Creates TransientValueData with incoming input stream. the stream will be lazily spooled to
    * file or byte array depending on maxBufferSize.
    * 
    * @param stream
    *          InputStream
    * @param orderNumber
    *          int
    */
   protected TransientValueData(InputStream stream, int orderNumber)
   {
      super(orderNumber);
      this.tmpStream = stream;
      this.deleteSpoolFile = true;
      this.closeTmpStream = false;
   }

   /**
    * TransientValueData constructor.
    * 
    * @param orderNumber
    *          int
    * @param bytes
    *          byte[]
    * @param stream
    *          InputStream
    * @param spoolFile
    *          File
    * @param fileCleaner
    *          FileCleaner
    * @param maxBufferSize
    *          int
    * @param tempDirectory
    *          File
    * @param deleteSpoolFile
    *          boolean
    * @throws IOException
    *           if read error
    */
   public TransientValueData(int orderNumber, byte[] bytes, InputStream stream, File spoolFile,
      FileCleaner fileCleaner, int maxBufferSize, File tempDirectory, boolean deleteSpoolFile) throws IOException
   {

      super(orderNumber);
      this.data = bytes;
      this.tmpStream = stream;
      this.closeTmpStream = true;
      this.spoolFile = spoolFile;
      this.fileCleaner = fileCleaner;
      this.maxBufferSize = maxBufferSize;
      this.tempDirectory = tempDirectory;
      this.deleteSpoolFile = deleteSpoolFile;

      if (spoolFile != null)
      {
         if (spoolFile instanceof SpoolFile)
            ((SpoolFile)spoolFile).acquire(this);

         if (this.tmpStream != null)
         {
            this.tmpStream.close();
            this.tmpStream = null; // 05.02.2009 release stream if file exists
         }

         this.spooled = true;
      }
   }

   /**
    * TransientValueData constructor.
    * 
    * @param stream
    *          InputStream
    */
   public TransientValueData(InputStream stream)
   {
      this(stream, 0);
   }

   /**
    * Constructor for String value data.
    * 
    * @param value
    *          String
    */
   public TransientValueData(String value)
   {
      this(stringToBytes(value), 0);
   }

   /**
    * Constructor for boolean value data.
    * 
    * @param value
    *          boolean
    */
   public TransientValueData(boolean value)
   {
      this(Boolean.valueOf(value).toString().getBytes(), 0);
   }

   /**
    * Constructor for Calendar value data.
    * 
    * @param value
    *          Calendar
    */
   public TransientValueData(Calendar value)
   {
      this(new JCRDateFormat().serialize(value), 0);
   }

   /**
    * Constructor for double value data.
    * 
    * @param value
    *          double
    */
   public TransientValueData(double value)
   {
      this(Double.valueOf(value).toString().getBytes(), 0);
   }

   /**
    * Constructor for long value data.
    * 
    * @param value
    *          long
    */
   public TransientValueData(long value)
   {
      this(Long.valueOf(value).toString().getBytes(), 0);
   }

   /**
    * Constructor for Name value data.
    * 
    * @param value
    *          InternalQName
    */
   public TransientValueData(InternalQName value)
   {
      this(stringToBytes(value.getAsString()), 0);
   }

   /**
    * Constructor for Path value data.
    * 
    * @param value
    *          QPath
    */
   public TransientValueData(QPath value)
   {
      this(stringToBytes(value.getAsString()), 0);
   }

   /**
    * Constructor for Reference value data.
    * 
    * @param value
    *          Identifier
    */
   public TransientValueData(Identifier value)
   {
      this(value.getString().getBytes(), 0);
   }

   /**
    * Constructor for Permission value data.
    * 
    * @param value
    *          AccessControlEntry
    */
   public TransientValueData(AccessControlEntry value)
   {
      this(stringToBytes(value.getAsString()), 0);
   }

   /**
    * {@inheritDoc}
    */
   public byte[] getAsByteArray() throws IOException
   {
      if (isByteArrayAfterSpool())
      {
         // TODO JCR-992 don't copy bytes
         // byte[] bytes = new byte[data.length];
         // System.arraycopy(data, 0, bytes, 0, data.length);
         return data;
      }
      else
      {
         return fileToByteArray();
      }
   }

   /**
    * Get input stream.
    * 
    * @param needSpool
    *          spool input stream if true
    * @return input stream
    * @throws IOException
    *           if any Exception is occurred
    */
   public InputStream getAsStream(boolean needSpool) throws IOException
   {
      if (needSpool)
      {
         if (isByteArrayAfterSpool())
         {
            return new ByteArrayInputStream(data); // from bytes
         }
         else
         {
            if (spoolFile != null)
            {
               return new FileInputStream(spoolFile); // from spool file
            }
            else
            {
               throw new NullPointerException("Stream already consumed");
            }
         }
      }
      else
      {
         if (data != null)
         {
            return new ByteArrayInputStream(data); // from bytes
         }
         else if (spoolFile != null)
         {
            return new FileInputStream(spoolFile); // from spool file if initialized
         }
         else if (tmpStream != null)
         {
            return tmpStream;
         }
         else
         {
            throw new NullPointerException("Null Stream data ");
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public InputStream getAsStream() throws IOException
   {
      return getAsStream(true);
   }

   /**
    * {@inheritDoc}
    */
   public long getLength()
   {
      if (isByteArrayAfterSpool())
      {
         if (log.isDebugEnabled())
            log.debug("getLength data : " + data.length);
         return data.length;
      }
      else
      {
         if (log.isDebugEnabled())
            log.debug("getLength spoolFile : " + spoolFile.length());
         return spoolFile.length();
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean isByteArray()
   {
      return data != null;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public TransientValueData createTransientCopy() throws RepositoryException
   {
      if (isByteArray())
      {
         // bytes, make a copy of real data
         // TODO JCR-992 don't copy bytes
         // byte[] newBytes = new byte[data.length];
         // System.arraycopy(data, 0, newBytes, 0, newBytes.length);

         try
         {
            return new TransientValueData(orderNumber, data, // TODO JCR-992
               null, null, fileCleaner, maxBufferSize, tempDirectory, deleteSpoolFile);
         }
         catch (IOException e)
         {
            throw new RepositoryException(e);
         }
      }
      else
      {
         // stream (or file) based , i.e. shared across sessions
         return this;
      }
   }

   /**
    * Create editable ValueData copy.
    * 
    * @return EditableValueData
    * @throws RepositoryException
    *           if error occurs
    */
   public EditableValueData createEditableCopy() throws RepositoryException
   {
      if (isByteArrayAfterSpool())
      {
         // bytes, make a copy of real data
         byte[] newBytes = new byte[data.length];
         System.arraycopy(data, 0, newBytes, 0, newBytes.length);

         try
         {
            return new EditableValueData(newBytes, orderNumber, fileCleaner, maxBufferSize, tempDirectory);
         }
         catch (IOException e)
         {
            throw new RepositoryException(e);
         }
      }
      else
      {
         // edited BLOB file, make a copy
         try
         {
            EditableValueData copy =
               new EditableValueData(spoolFile, orderNumber, fileCleaner, maxBufferSize, tempDirectory);
            return copy;
         }
         catch (FileNotFoundException e)
         {
            throw new RepositoryException("Create transient copy error. " + e, e);
         }
         catch (IOException e)
         {
            throw new RepositoryException("Create transient copy error. " + e, e);
         }
      }
   }

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
    *           if read/write error occurs
    */
   public long read(OutputStream stream, long length, long position) throws IOException
   {

      if (position < 0)
         throw new IOException("Position must be higher or equals 0. But given " + position);

      if (length < 0)
         throw new IOException("Length must be higher or equals 0. But given " + length);

      if (isByteArrayAfterSpool())
      {
         // validation
         if (position >= data.length && position > 0)
            throw new IOException("Position " + position + " out of value size " + data.length);

         if (position + length >= data.length)
            length = data.length - position;

         stream.write(data, (int)position, (int)length);

         return length;
      }
      else
      {
         if (spoolChannel == null)
            spoolChannel = new FileInputStream(spoolFile).getChannel();

         // validation
         if (position >= spoolChannel.size() && position > 0)
            throw new IOException("Position " + position + " out of value size " + spoolChannel.size());

         if (position + length >= spoolChannel.size())
            length = spoolChannel.size() - position;

         MappedByteBuffer bb = spoolChannel.map(FileChannel.MapMode.READ_ONLY, position, length);

         WritableByteChannel ch = Channels.newChannel(stream); // TODO don't use Channels.newChannel
         ch.write(bb);
         ch.close();

         return length;
      }
   }

   /**
    * Return spool file. Actual for BLOBs only.
    * 
    * @return File spool file or null
    */
   public File getSpoolFile()
   {
      return spoolFile;
   }

   /**
    * Set spool as persisted file. It's means ValueData has its data stored to a External Value
    * Storage. And it's a file with the content which cannot be deleted or moved outside Value
    * Storage.
    * 
    * @param persistedFile
    *          File
    * @throws IOException
    *           if any Exception is occurred
    */
   public void setPersistedFile(File persistedFile) throws IOException
   {
      if (isTransient())
      {
         deleteCurrentSpoolFile();
      }

      this.spoolFile = persistedFile;
      this.deleteSpoolFile = false;
      this.spooled = true;

      this.tmpStream = null;
      this.data = null;

      this.isTransient = false;
   }

   /**
    * Helper method to simplify operations that requires stringified data.
    * 
    * @return String
    * @throws IOException
    *           if read error
    */
   public String getString() throws IOException
   {
      if (log.isDebugEnabled())
         log.debug("getString");

      return new String(getAsByteArray(), Constants.DEFAULT_ENCODING);
   }

   // ///////////////////////////////////
   /**
    * Make sense for stream storage only.
    * 
    * @param cleaner
    *          FileCleaner
    */
   public void setFileCleaner(FileCleaner cleaner)
   {
      this.fileCleaner = cleaner;
   }

   /**
    * @param tempDirectory
    */
   public void setTempDirectory(File tempDirectory)
   {
      this.tempDirectory = tempDirectory;
   }

   /**
    * @param maxBufferSize
    */
   public void setMaxBufferSize(int maxBufferSize)
   {
      this.maxBufferSize = maxBufferSize;
   }

   /**
    * {@inheritDoc}
    */
   protected void finalize() throws Throwable
   {
      deleteCurrentSpoolFile();
   }

   /**
    * {@inheritDoc}
    */
   public boolean equals(Object obj)
   {

      if (this == obj)
      {
         return true;
      }

      if (obj instanceof TransientValueData)
      {

         TransientValueData other = (TransientValueData)obj;
         if (isByteArray() != other.isByteArray())
            return false;
         try
         {
            if (isByteArray())
            {
               return Arrays.equals(getAsByteArray(), other.getAsByteArray());
            }
            else
               return getSpoolFile().equals(other.getSpoolFile());
         }
         catch (IOException e)
         {
            log.error("Read error", e);
            return false;
         }
      }
      return false;
   }

   // ///////////////////////////////////

   /**
    * Spool ValueData InputStream to a temp File.
    */
   protected void spoolInputStreamAlways()
   {

      if (spooled || tmpStream == null) // already spooled
         return;

      byte[] tmpBuff = new byte[2048];
      OutputStream sfout = null;
      int read = 0;

      try
      {
         SpoolFile sf = SpoolFile.createTempFile("jcrvd", null, tempDirectory);
         sf.acquire(this);
         sfout = new FileOutputStream(sf);

         while ((read = tmpStream.read(tmpBuff)) >= 0)
            sfout.write(tmpBuff, 0, read);

         this.spoolChannel = null;
         this.spoolFile = sf;

         this.data = null;
         this.spooled = true;
      }
      catch (IOException e)
      {
         throw new IllegalStateException(e);
      }
      finally
      {
         try
         {
            if (sfout != null)
               sfout.close();
         }
         catch (IOException e)
         {
            log.error("Error of spool output close.", e);
         }

         if (this.closeTmpStream)
            try
            {
               this.tmpStream.close();
            }
            catch (IOException e)
            {
               log.error("Error of source input close.", e);
            }
         this.tmpStream = null;
      }
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
         return;

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
            else if (len + read > maxBufferSize && fileCleaner != null)
            {
               // threshold for keeping data in memory exceeded,
               // if have a fileCleaner create temp file and spool buffer contents.
               sf = SpoolFile.createTempFile("jcrvd", null, tempDirectory);
               sf.acquire(this);

               sfout = new FileOutputStream(sf);
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
         throw new IllegalStateException(e);
      }
      finally
      {
         try
         {
            if (sfout != null)
               sfout.close();
         }
         catch (IOException e)
         {
            log.error("Error of spool output close.", e);
         }

         if (this.closeTmpStream)
            try
            {
               this.tmpStream.close();
            }
            catch (IOException e)
            {
               log.error("Error of source input close.", e);
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
      FileChannel fch = new FileInputStream(spoolFile).getChannel();

      if (log.isDebugEnabled() && fch.size() > maxBufferSize)
      {
         log.debug("Potential lack of memory due to call getAsByteArray() on stream data exceeded " + fch.size()
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
   private void deleteCurrentSpoolFile() throws IOException
   {
      if (spoolChannel != null)
         spoolChannel.close();

      if (spoolFile != null)
      {

         if (spoolFile instanceof SpoolFile)
            ((SpoolFile)spoolFile).release(this);

         if (deleteSpoolFile && spoolFile.exists())
         {
            if (!spoolFile.delete())
            {
               if (fileCleaner != null)
               {
                  fileCleaner.addFile(spoolFile);

                  if (log.isDebugEnabled())
                  {
                     log.debug("Could not remove file. Add to fileCleaner " + spoolFile.getAbsolutePath());
                  }
               }
               else
               {
                  log.warn("Could not remove temporary file on finalize " + spoolFile.getAbsolutePath());
               }
            }
         }
      }
   }

   // ------------- Serializable

   /**
    * TransientValueData empty constructor. Used for Replication serialization (java).
    * 
    */
   public TransientValueData()
   {
      super(0);
      this.deleteSpoolFile = true;
      this.closeTmpStream = true;
   }

   /**
    * {@inheritDoc}
    */
   public void writeExternal(ObjectOutput out) throws IOException
   {
      if (this.isByteArray())
      {
         out.writeInt(1);
         int f = data.length;
         out.writeInt(f);
         out.write(data);
      }
      else
      {
         out.writeInt(2);
      }
      out.writeInt(orderNumber);
      out.writeInt(maxBufferSize);
   }

   /**
    * {@inheritDoc}
    */
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      int type = in.readInt();

      if (type == 1)
      {
         data = new byte[in.readInt()];
         in.readFully(data);
      }
      orderNumber = in.readInt();
      maxBufferSize = in.readInt();
   }

   /**
    * Set data Stream from outside. FOR Synchronouis replicatiojn only!
    * 
    * @param in
    *          InputStream
    */
   public void setStream(InputStream in)
   {
      this.spooled = false;
      this.tmpStream = in;

      this.data = null;

      this.spoolFile = null;
      this.spoolChannel = null;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isTransient()
   {
      return isTransient;
   }
}
