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

import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.impl.util.io.PrivilegedFileHelper;
import org.exoplatform.services.jcr.impl.util.io.SpoolFile;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

public class EditableValueData extends TransientValueData
{

   protected final static Log LOG = ExoLogger.getLogger("exo.jcr.component.core.EditableValueData");

   protected class NewEditableValueData extends NewValueData
   {

      protected final int maxIOBuffSize;

      public NewEditableValueData(byte[] bytes, int orderNumber, FileCleaner fileCleaner, int maxBufferSize,
         File tempDirectory) throws IOException
      {

         // send bytes to super.<init>
         super(orderNumber, bytes, null, null, fileCleaner, maxBufferSize, tempDirectory, true, true);

         this.maxIOBuffSize = calcMaxIOSize();

         this.spooled = true;
      }

      // TODO use InputStream instead of spoolFile and use Channel.transferFrom.
      public NewEditableValueData(SpoolFile spoolFile, int orderNumber, FileCleaner fileCleaner, int maxBufferSize,
         File tempDirectory) throws IOException
      {

         // don't send any data there (no stream, no bytes)
         super(orderNumber, null, null, null, fileCleaner, maxBufferSize, tempDirectory, true, true);

         this.maxIOBuffSize = calcMaxIOSize();

         SpoolFile sf = null;
         FileChannel sch = null;
         try
         {
            sf = SpoolFile.createTempFile("jcrvdedit", null, tempDirectory);

            sch = new RandomAccessFile(sf, "rw").getChannel();

            FileChannel sourceCh = PrivilegedFileHelper.fileInputStream(spoolFile).getChannel();
            try
            {
               sch.transferFrom(sourceCh, 0, sourceCh.size());
            }
            finally
            {
               sourceCh.close();
            }
         }
         catch (final IOException e)
         {
            try
            {
               sch.close();
               PrivilegedFileHelper.delete(sf);
            }
            catch (Exception e1)
            {
            }
            throw new IOException("init error " + e.getMessage())
            {
               @Override
               public Throwable getCause()
               {
                  return e;
               }
            };
         }

         this.data = null;

         this.spoolFile = sf;
         this.spoolChannel = sch;

         this.spooled = true;
      }

      public NewEditableValueData(InputStream stream, int orderNumber, FileCleaner fileCleaner, int maxBufferSize,
         File tempDirectory) throws IOException
      {

         // don't send any data there (no stream, no bytes)
         super(orderNumber, null, null, null, fileCleaner, maxBufferSize, tempDirectory, true, true);

         this.maxIOBuffSize = calcMaxIOSize();

         SpoolFile sf = SpoolFile.createTempFile("jcrvdedit", null, tempDirectory);
         OutputStream sfout = PrivilegedFileHelper.fileOutputStream(sf);
         try
         {
            byte[] tmpBuff = new byte[2048];
            int read = 0;
            int len = 0;

            while ((read = stream.read(tmpBuff)) >= 0)
            {
               sfout.write(tmpBuff, 0, read);
               len += read;
            }
         }
         catch (final IOException e)
         {
            try
            {
               sfout.close();
               PrivilegedFileHelper.delete(sf);
            }
            catch (Exception e1)
            {
            }
            throw new IOException("init error " + e.getMessage())
            {
               @Override
               public Throwable getCause()
               {
                  return e;
               }
            };
         }

         this.data = null;

         this.spoolFile = sf;
         this.spoolChannel = new RandomAccessFile(sf, "rw").getChannel();

         this.spooled = true;
      }

      protected int calcMaxIOSize()
      {
         return maxBufferSize < 1024 ? 1024 : maxBufferSize < (250 * 1024) ? maxBufferSize : 250 * 1024;
      }

      protected int calcBuffSize(long length)
      {
         int buffSize = (int)(length > maxIOBuffSize ? maxIOBuffSize : length / 4);
         buffSize = buffSize < 1024 ? 256 : buffSize;
         return buffSize;
      }

      /**
       * Update with <code>length</code> bytes from the specified <code>stream</code> to this value data
       * at <code>position</code>.
       * 
       * If <code>position</code> is lower 0 the IOException exception will be thrown.
       * 
       * If <code>position</code> is higher of current Value length the Value length will be increased
       * before to <code>position</code> size and <code>length</code> bytes will be added after the
       * <code>position</code>.
       * 
       * @param stream
       *          the data.
       * @param length
       *          the number of bytes from buffer to write.
       * @param position
       *          position in file to write data
       * 
       * @throws IOException
       */
      public void update(InputStream stream, long length, long position) throws IOException
      {

         if (position < 0)
            throw new IOException("Position must be higher or equals 0. But given " + position);

         if (length < 0)
            throw new IOException("Length must be higher or equals 0. But given " + length);

         if (isByteArray())
         {
            // edit bytes
            // ...check length
            long updateSize = position + length;

            long newSize = updateSize > data.length ? updateSize : data.length;
            if ((newSize <= maxBufferSize && newSize <= Integer.MAX_VALUE) || maxBufferSize <= 0
               || tempDirectory == null)
            {
               // bytes
               byte[] newBytes = new byte[(int)newSize];

               int newIndex = 0; // first pos to write

               if ((newIndex = (int)position) > 0)
               {
                  // begin from the existed bytes
                  System.arraycopy(data, 0, newBytes, 0, newIndex < data.length ? newIndex : data.length);
               }

               // write new data
               int i = -1;
               boolean doRead = true;
               byte[] buff = new byte[calcBuffSize(length)];
               while (doRead && (i = stream.read(buff)) >= 0)
               {
                  if (newIndex + i > newBytes.length)
                  {
                     // given length reached
                     i = newBytes.length - newIndex;
                     doRead = false;
                  }
                  System.arraycopy(buff, 0, newBytes, newIndex, i);
                  newIndex += i;
               }

               if (newIndex < data.length)
                  // write the rest of existed data
                  System.arraycopy(data, newIndex, newBytes, newIndex, data.length - newIndex);

               this.data = newBytes;

               this.spoolFile = null;
               this.spoolChannel = null;

            }
            else
            {

               // switch from bytes to file/channel
               SpoolFile chf = null;
               FileChannel chch = null;
               long newIndex = 0; // first pos to write

               try
               {
                  chf = SpoolFile.createTempFile("jcrvdedit", null, tempDirectory);
                  chch = new RandomAccessFile(chf, "rw").getChannel();

                  // allocate the space for whole file
                  MappedByteBuffer bb = chch.map(FileChannel.MapMode.READ_WRITE, position + length, 0);
                  bb.force();
                  bb = null;

                  ReadableByteChannel bch = Channels.newChannel(new ByteArrayInputStream(this.data));

                  if ((newIndex = (int)position) > 0)
                  {
                     // begin from the existed bytes
                     chch.transferFrom(bch, 0, newIndex < data.length ? newIndex : data.length);
                     bch.close();
                  }

                  // write update data
                  // TODO don't use Channels.newChannel in Java5
                  ReadableByteChannel sch = Channels.newChannel(stream);
                  chch.transferFrom(sch, newIndex, length);
                  sch.close();
                  newIndex += length;

                  if (newIndex < data.length)
                     // write the rest of existed data
                     chch.transferFrom(bch, newIndex, data.length - newIndex);

                  bch.close();
               }
               catch (final IOException e)
               {
                  try
                  {
                     chch.close();
                     PrivilegedFileHelper.delete(chf);
                  }
                  catch (Exception e1)
                  {
                  }
                  throw new IOException("update error " + e.getMessage())
                  {
                     @Override
                     public Throwable getCause()
                     {
                        return e;
                     }
                  };
               }
               this.spoolFile = chf;
               this.spoolChannel = chch;
               this.data = null;
            }
         }
         else
         {
            MappedByteBuffer bb = spoolChannel.map(FileChannel.MapMode.READ_WRITE, position, length);

            ReadableByteChannel ch = Channels.newChannel(stream);
            ch.read(bb);
            ch.close();

            bb.force();
         }
      }

      /**
       * Set length of the Value in bytes to the specified <code>size</code>.
       * 
       * If <code>size</code> is lower 0 the IOException exception will be thrown.
       * 
       * This operation can be used both for extend and for truncate the Value size.
       * 
       * This method used internally in update operation in case of extending the size to the given
       * position.
       * 
       * @param size
       * @throws IOException
       */
      public void setLength(long size) throws IOException
      {

         if (size < 0)
            throw new IOException("Size must be higher or equals 0. But given " + size);

         if (isByteArray())
         {
            if (size < maxBufferSize || maxBufferSize <= 0 || tempDirectory == null)
            {
               // use bytes
               byte[] newBytes = new byte[(int)size];
               System.arraycopy(data, 0, newBytes, 0, (data.length < newBytes.length) ? data.length : newBytes.length);
               this.data = newBytes;
            }
            else
            {
               // switch from bytes to file/channel
               SpoolFile chf = null;
               FileChannel chch = null;
               try
               {
                  chf = SpoolFile.createTempFile("jcrvdedit", null, tempDirectory);
                  chch = new RandomAccessFile(chf, "rw").getChannel();

                  ReadableByteChannel bch = Channels.newChannel(new ByteArrayInputStream(this.data));
                  chch.transferFrom(bch, 0, this.data.length); // get all
                  bch.close();

                  if (chch.size() < size)
                  {
                     // extend length
                     MappedByteBuffer bb = chch.map(FileChannel.MapMode.READ_WRITE, size, 0);
                     bb.force();
                  }
               }
               catch (final IOException e)
               {
                  try
                  {
                     chch.close();
                     PrivilegedFileHelper.delete(chf);
                  }
                  catch (Exception e1)
                  {
                  }
                  throw new IOException("setLength(" + size + ") error. " + e.getMessage())
                  {
                     @Override
                     public Throwable getCause()
                     {
                        return e;
                     }
                  };
               }
               this.spoolFile = chf;
               this.spoolChannel = chch;
               this.data = null;
            }
         }
         else if (size < maxBufferSize)
         {
            // switch to bytes
            ByteBuffer bb = ByteBuffer.allocate((int)size);
            spoolChannel.force(false);
            spoolChannel.position(0);
            spoolChannel.read(bb);

            byte[] tmpb = null;

            if (bb.hasArray())
            {
               tmpb = bb.array();
            }
            else
            {
               // impossible code in most cases, as we use heap backed buffer
               tmpb = new byte[bb.capacity()];
               bb.get(tmpb);
            }

            spoolChannel.close();

            // delete file
            if (!PrivilegedFileHelper.delete(spoolFile))
            {
               if (fileCleaner != null)
               {
                  LOG.info("Could not remove file. Add to fileCleaner " + spoolFile);
                  fileCleaner.addFile(spoolFile);
               }
               else
               {
                  LOG.warn("Could not remove temporary file on switch to bytes, fileCleaner not found. "
                     + PrivilegedFileHelper.getAbsolutePath(spoolFile));
               }
            }

            data = tmpb;
            spoolChannel = null;
            spoolFile = null;
         }
         else
         {
            if (spoolChannel.size() < size)
            {
               // extend file
               MappedByteBuffer bb = spoolChannel.map(FileChannel.MapMode.READ_WRITE, size, 0);
               bb.force();
            }
            else
            {
               // truncate file
               spoolChannel.truncate(size);
            }
         }
      }
   }

   public EditableValueData(byte[] bytes, int orderNumber, FileCleaner fileCleaner, int maxBufferSize,
      File tempDirectory) throws IOException
   {
      this.delegate = new NewEditableValueData(bytes, orderNumber, fileCleaner, maxBufferSize, tempDirectory);
   }

   public EditableValueData(SpoolFile spoolFile, int orderNumber, FileCleaner fileCleaner, int maxBufferSize,
      File tempDirectory) throws IOException
   {
      this.delegate = new NewEditableValueData(spoolFile, orderNumber, fileCleaner, maxBufferSize, tempDirectory);
   }

   public EditableValueData(InputStream stream, int orderNumber, FileCleaner fileCleaner, int maxBufferSize,
      File tempDirectory) throws IOException
   {
      this.delegate = new NewEditableValueData(stream, orderNumber, fileCleaner, maxBufferSize, tempDirectory);
   }

   public void update(InputStream stream, long length, long position) throws IOException
   {
      ((NewEditableValueData)this.delegate).update(stream, length, position);
   }

   public void setLength(long size) throws IOException
   {
      ((NewEditableValueData)this.delegate).setLength(size);
   }
}
