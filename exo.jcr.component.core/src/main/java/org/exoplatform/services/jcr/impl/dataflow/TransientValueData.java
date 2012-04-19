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

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.datamodel.Identifier;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.persistent.FilePersistedValueData;
import org.exoplatform.services.jcr.impl.util.JCRDateFormat;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.impl.util.io.SpoolFile;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
 * @author <a href="mailto:peter.nedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id$
 */
public class TransientValueData implements ValueData
{

   protected final static Log LOG = ExoLogger.getLogger("exo.jcr.component.core.TransientValueData");

   protected ValueData delegate;

   protected class NewValueData extends AbstractSessionValueData
   {

      protected byte[] data;

      protected InputStream tmpStream;

      protected SpoolFile spoolFile;

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
       * Creates TransientValueData with incoming byte array.
       * @param orderNumber
       *          int
       * @param value
       *          byte[]
       */
      private NewValueData(int orderNumber, byte[] value)
      {
         super(orderNumber);
         this.data = value;
         this.deleteSpoolFile = true;
         this.closeTmpStream = false;
      }

      /**
       * Creates TransientValueData with incoming input stream. the stream will be lazily spooled to
       * file or byte array depending on maxBufferSize.
       * @param orderNumber
       *          int
       * @param stream
       *          InputStream
       */
      private NewValueData(int orderNumber, InputStream stream)
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
       * @param closeTmpStream
       *          boolean         
       * @throws IOException
       *           if read error
       */
      protected NewValueData(int orderNumber, byte[] bytes, InputStream stream, SpoolFile spoolFile,
         FileCleaner fileCleaner, int maxBufferSize, File tempDirectory, boolean deleteSpoolFile, boolean closeTmpStream)
         throws IOException
      {

         super(orderNumber);
         this.data = bytes;
         this.tmpStream = stream;
         this.closeTmpStream = closeTmpStream;
         this.spoolFile = spoolFile;
         this.fileCleaner = fileCleaner;
         this.maxBufferSize = maxBufferSize;
         this.tempDirectory = tempDirectory;
         this.deleteSpoolFile = deleteSpoolFile;

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
       * Get input stream.
       * 
       * @param needSpool
       *          spool input stream if true
       * @return input stream
       * @throws IOException
       *           if any Exception is occurred
       */
      private InputStream getAsStream(boolean needSpool) throws IOException
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
                  return PrivilegedFileHelper.fileInputStream(spoolFile); // from spool file
               }
               else
               {
                  throw new IllegalArgumentException("Stream already consumed");
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
               return PrivilegedFileHelper.fileInputStream(spoolFile); // from spool file if initialized
            }
            else if (tmpStream != null)
            {
               return tmpStream;
            }
            else
            {
               throw new IllegalArgumentException("Null Stream data ");
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
      public boolean isByteArray()
      {
         return data != null;
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
       * {@inheritDoc}
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
               spoolChannel = PrivilegedFileHelper.fileInputStream(spoolFile).getChannel();

            // validation
            if (position >= spoolChannel.size() && position > 0)
               throw new IOException("Position " + position + " out of value size " + spoolChannel.size());

            if (position + length >= spoolChannel.size())
               length = spoolChannel.size() - position;

            MappedByteBuffer bb = spoolChannel.map(FileChannel.MapMode.READ_ONLY, position, length);

            WritableByteChannel ch = Channels.newChannel(stream);
            ch.write(bb);
            ch.close();

            return length;
         }
      }

      /**
       * Helper method to simplify operations that requires stringified data.
       * 
       * @return String
       * @throws IOException
       *           if read error
       */
      @Deprecated
      public String getString() throws IOException
      {
         return new String(getAsByteArray(), Constants.DEFAULT_ENCODING);
      }

      /**
       * {@inheritDoc}
       */
      @Override
      protected void finalize() throws Throwable
      {
         deleteCurrentSpoolFile();
      }

      /**
       * {@inheritDoc}
       */
      public boolean equals(ValueData another)
      {
         if (this == another)
         {
            return true;
         }

         if (isByteArray() == another.isByteArray())
         {
            // by content
            try
            {
               if (isByteArray())
               {
                  // compare bytes
                  return Arrays.equals(getAsByteArray(), another.getAsByteArray());
               }
               else
               {
                  // it's comparison of BLOB values 
                  // they can be equal in theory, but we will not check BLOB files content due to performance
                  // check only if it's not a same file or stream (not real) backed both

                  File dataFile;
                  if (another instanceof TransientValueData)
                  {
                     // if both transient... or stream of file can be equal
                     TransientValueData transnt = (TransientValueData)another;

                     if (transnt.delegate instanceof NewValueData)
                     {
                        // if both are transient... check stream or file
                        NewValueData otherVd = (NewValueData)transnt.delegate;
                        if (this.tmpStream == otherVd.tmpStream)
                        {
                           return true;
                        }
                        dataFile = otherVd.spoolFile;
                     }
                     else if (transnt.delegate instanceof FilePersistedValueData)
                     {
                        // if other persistent as delegated - check file
                        dataFile = ((FilePersistedValueData)transnt.delegate).getFile();
                     }
                     else
                     {
                        return false;
                     }
                  }
                  else if (another instanceof FilePersistedValueData)
                  {
                     FilePersistedValueData persisted = (FilePersistedValueData)another;

                     // if other persistent - check file
                     dataFile = persisted.getFile();
                  }
                  else
                  {
                     return false;
                  }

                  return spoolFile != null ? spoolFile.equals(dataFile) : false;
               }
            }
            catch (IOException e)
            {
               LOG.error("Read error", e);
               return false;
            }
         }
         return false;
      }

      /**
       * Spool ValueData InputStream to a temp File.
       */
      @Deprecated
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
            sfout = PrivilegedFileHelper.fileOutputStream(sf);

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
               LOG.error("Error of spool output close.", e);
            }

            if (this.closeTmpStream)
               try
               {
                  this.tmpStream.close();
               }
               catch (IOException e)
               {
                  LOG.error("Error of source input close.", e);
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
                  fileCleaner.addFile(sf);
               }
               catch (FileNotFoundException ex)
               {
                  if (LOG.isDebugEnabled())
                  {
                     LOG.debug("Could not remove temporary file : " + sf.getAbsolutePath());
                  }
               }
            }
            
            throw new IllegalStateException("Error of spooling to temp file from " + tmpStream
               + ". Check if stream is not consumed or is not closed.", e);
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

         if (LOG.isDebugEnabled() && fch.size() > maxBufferSize)
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
      private void deleteCurrentSpoolFile() throws IOException
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

            if (deleteSpoolFile && PrivilegedFileHelper.exists(spoolFile))
            {
               if (!PrivilegedFileHelper.delete(spoolFile))
               {
                  if (fileCleaner != null)
                  {
                     fileCleaner.addFile(spoolFile);

                     if (LOG.isDebugEnabled())
                     {
                        LOG.debug("Could not remove file. Add to fileCleaner "
                           + PrivilegedFileHelper.getAbsolutePath(spoolFile));
                     }
                  }
                  else
                  {
                     LOG.warn("Could not remove temporary file on finalize "
                        + PrivilegedFileHelper.getAbsolutePath(spoolFile));
                  }
               }
            }
         }
      }
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
    * @param closeTmpStream
    *          boolean
    * @throws IOException
    *           if read error
    */
   public TransientValueData(int orderNumber, byte[] bytes, InputStream stream, SpoolFile spoolFile,
      FileCleaner fileCleaner, int maxBufferSize, File tempDirectory, boolean deleteSpoolFile, boolean closeTmpStream)
      throws IOException
   {
      this.delegate =
         new NewValueData(orderNumber, bytes, stream, spoolFile, fileCleaner, maxBufferSize, tempDirectory,
            deleteSpoolFile, closeTmpStream);
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
   public TransientValueData(int orderNumber, byte[] bytes, InputStream stream, SpoolFile spoolFile,
      FileCleaner fileCleaner, int maxBufferSize, File tempDirectory, boolean deleteSpoolFile) throws IOException
   {
      this.delegate =
         new NewValueData(orderNumber, bytes, stream, spoolFile, fileCleaner, maxBufferSize, tempDirectory,
            deleteSpoolFile, true);
   }

   /**
    * TransientValueData constructor for stream data.
    * 
    * @param orderNumber
    *          int
    * @param stream
    *          InputStream
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
   public TransientValueData(int orderNumber, InputStream stream, FileCleaner fileCleaner, int maxBufferSize,
      File tempDirectory, boolean deleteSpoolFile) throws IOException
   {
      this.delegate =
         new NewValueData(orderNumber, null, stream, null, fileCleaner, maxBufferSize, tempDirectory, deleteSpoolFile,
            true);
   }

   /**
    * TransientValueData constructor for file data.
    * 
    * @param orderNumber
    *          int
    * @param spoolFile
    *          File
    * @param fileCleaner
    *          FileCleaner         
    * @param deleteSpoolFile
    *          boolean
    * @throws IOException
    *           if read error
    */
   public TransientValueData(int orderNumber, SpoolFile spoolFile, FileCleaner fileCleaner, boolean deleteSpoolFile)
      throws IOException
   {
      this.delegate =
         new NewValueData(orderNumber, null, null, spoolFile, fileCleaner, -1, null, deleteSpoolFile, true);
   }

   /**
    * Creates TransientValueData with incoming byte array.
    * @param value
    *          byte[]
    */
   public TransientValueData(byte[] value)
   {
      this(0, value);
   }

   /**
    * Creates TransientValueData with incoming byte array.
    * @param orderNumber
    *          int
    * @param value
    *          byte[]
    */
   public TransientValueData(int orderNumber, byte[] value)
   {
      this.delegate = new NewValueData(orderNumber, value);
   }

   /**
    * TransientValueData constructor.
    * 
    * @param stream
    *          InputStream
    */
   public TransientValueData(InputStream stream)
   {
      this(0, stream);
   }

   /**
    * Creates TransientValueData with incoming input stream. the stream will be lazily spooled to
    * file or byte array depending on maxBufferSize.
    * 
    * @param orderNumber
    *          int
    * @param stream
    *          InputStream
    */
   public TransientValueData(int orderNumber, InputStream stream)
   {
      this.delegate = new NewValueData(orderNumber, stream);
   }

   /**
    * Constructor for String value data.
    * 
    * @param value
    *          String
    */
   public TransientValueData(String value)
   {
      this(0, value);
   }

   /**
    * Constructor for String value data.
    * 
    * @param orderNumber
    *          int 
    * @param value
    *          String
    */
   public TransientValueData(int orderNumber, String value)
   {
      this(orderNumber, stringToBytes(value));
   }

   /**
    * Constructor for boolean value data.
    * 
    * @param value
    *          boolean
    */
   public TransientValueData(boolean value)
   {
      this(0, value);
   }

   /**
    * Constructor for boolean value data.
    * 
    * @param orderNumber
    *          int
    * @param value
    *          boolean
    */
   public TransientValueData(int orderNumber, boolean value)
   {
      this(orderNumber, Boolean.valueOf(value).toString().getBytes());
   }

   /**
    * Constructor for Calendar value data.
    * 
    * @param value
    *          Calendar
    */
   public TransientValueData(Calendar value)
   {
      this(0, value);
   }

   /**
    * Constructor for Calendar value data.
    * 
    * @param orderNumber
    *          int
    * @param value
    *          Calendar
    */
   public TransientValueData(int orderNumber, Calendar value)
   {
      this(orderNumber, new JCRDateFormat().serialize(value));
   }

   /**
    * Constructor for double value data.
    * 
    * @param value
    *          double
    */
   public TransientValueData(double value)
   {
      this(0, value);
   }

   /**
    * Constructor for double value data.
    * 
    * @param orderNumber
    *          int
    * @param value
    *          double
    */
   public TransientValueData(int orderNumber, double value)
   {
      this(orderNumber, Double.valueOf(value).toString().getBytes());
   }

   /**
    * Constructor for long value data.
    * 
    * @param value
    *          long
    */
   public TransientValueData(long value)
   {
      this(0, value);
   }

   /**
    * Constructor for long value data.
    * 
    * @param orderNumber
    *          int
    * @param value
    *          long
    */
   public TransientValueData(int orderNumber, long value)
   {
      this(orderNumber, Long.valueOf(value).toString().getBytes());
   }

   /**
    * Constructor for Name value data.
    * 
    * @param value
    *          InternalQName
    */
   public TransientValueData(InternalQName value)
   {
      this(0, value);
   }

   /**
    * Constructor for Name value data.
    *
    * @param orderNumber
    *          int
    * @param value
    *          InternalQName
    */
   public TransientValueData(int orderNumber, InternalQName value)
   {
      this(orderNumber, stringToBytes(value.getAsString()));
   }

   /**
    * Constructor for Path value data.
    * 
    * @param value
    *          QPath
    */
   public TransientValueData(QPath value)
   {
      this(0, value);
   }

   /**
    * Constructor for Path value data.
    * 
    * @param orderNumber
    *          int
    * @param value
    *          QPath
    */
   public TransientValueData(int orderNumber, QPath value)
   {
      this(orderNumber, stringToBytes(value.getAsString()));
   }

   /**
    * Constructor for Reference value data.
    * 
    * @param value
    *          Identifier
    */
   public TransientValueData(Identifier value)
   {
      this(0, value);
   }

   /**
    * Constructor for Reference value data.
    * 
    * @param orderNumber
    *          int
    * @param value
    *          Identifier
    */
   public TransientValueData(int orderNumber, Identifier value)
   {
      this(orderNumber, value.getString().getBytes());
   }

   /**
    * Constructor for Permission value data.
    * 
    * @param value
    *          AccessControlEntry
    */
   public TransientValueData(AccessControlEntry value)
   {
      this(0, value);
   }

   /**
    * Constructor for Permission value data.
    * 
    * @param orderNumber
    *          int
    * @param value
    *          AccessControlEntry
    */
   public TransientValueData(int orderNumber, AccessControlEntry value)
   {
      this(orderNumber, stringToBytes(value.getAsString()));
   }

   /**
    * Constructor for Persisted ValueData delegate (for transient copy).
    * 
    * @param persistent AbstractPersistedValueData
    */
   public TransientValueData(AbstractPersistedValueData persistent)
   {
      this.delegate = persistent;
   }

   /**
    * Constructor for Editable value data.
    * 
    * @param orderNumber
    *          int
    */
   protected TransientValueData()
   {
   }

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
    * {@inheritDoc}
    */
   public byte[] getAsByteArray() throws IllegalStateException, IOException
   {
      return delegate.getAsByteArray();
   }

   /**
    * {@inheritDoc}
    */
   public InputStream getAsStream() throws IOException
   {
      return delegate.getAsStream();
   }

   /**
    * {@inheritDoc}
    */
   public long getLength()
   {
      return delegate.getLength();
   }

   /**
    * {@inheritDoc}
    */
   public boolean isByteArray()
   {
      return delegate.isByteArray();
   }

   /**
    * {@inheritDoc}
    */
   public long read(OutputStream stream, long length, long position) throws IOException
   {
      return delegate.read(stream, length, position);
   }

   /**
    * {@inheritDoc}
    */
   public int getOrderNumber()
   {
      return delegate.getOrderNumber();
   }

   /**
    * Re-init this TransientValueData with another value.  
    * 
    * @param newValue ValueData
    */
   public void delegate(ValueData newValue)
   {
      this.delegate = newValue;
   }

   /**
    * Return temporary spool file. Can be null. For persistent operations on newly created data only.
    * 
    * @return File temp file
    */
   public SpoolFile getSpoolFile()
   {
      if (delegate instanceof NewValueData)
      {
         return ((NewValueData)delegate).spoolFile;
      }

      return null;
   }

   /**
    * Get original stream. Can be consumed or null. <p/>
    * WARN: method for persistent operations on modified ValueData only.
    * 
    * @return InputStream original stream
    */
   public InputStream getOriginalStream()
   {
      if (delegate instanceof NewValueData)
      {
         return ((NewValueData)delegate).tmpStream;
      }

      return null;
   }

   /**
    * {@inheritDoc}
    */
   public boolean equals(ValueData another)
   {

      if (this == another)
      {
         return true;
      }

      return delegate.equals(another);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean equals(Object obj)
   {
      if (obj instanceof ValueData)
      {
         return equals((ValueData)obj);
      }

      return false;
   }

   //   /**
   //    * TransientValueData empty constructor. Used for Replication serialization (java).
   //    * 
   //    */
   //   public TransientValueData()
   //   {
   //      super(0);
   //   }

   //   /**
   //    * {@inheritDoc}
   //    */
   //   public void writeExternal(ObjectOutput out) throws IOException
   //   {
   //      if (this.isByteArray())
   //      {
   //         out.writeInt(1);
   //         int f = (int) delegate.getLength();
   //         out.writeInt(f);
   //         out.write(delegate.getAsByteArray());
   //      }
   //      else
   //      {
   //         out.writeInt(2);
   //      }
   //      out.writeInt(orderNumber);
   //      out.writeInt(maxBufferSize);
   //   }

   //   /**
   //    * {@inheritDoc}
   //    */
   //   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   //   {
   //      int type = in.readInt();
   //
   //      if (type == 1)
   //      {
   //         data = new byte[in.readInt()];
   //         in.readFully(data);
   //      }
   //      orderNumber = in.readInt();
   //      maxBufferSize = in.readInt();
   //   }

   //   /**
   //    * Set data Stream from outside. FOR Synchronous replication only!
   //    * 
   //    * @param in
   //    *          InputStream
   //    */
   //   public void setStream(InputStream in)
   //   {
   //      this.spooled = false;
   //      this.tmpStream = in;
   //
   //      this.data = null;
   //
   //      this.spoolFile = null;
   //      this.spoolChannel = null;
   //   }
}