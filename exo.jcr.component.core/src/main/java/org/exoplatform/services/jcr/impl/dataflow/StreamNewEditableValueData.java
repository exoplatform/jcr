/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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

package org.exoplatform.services.jcr.impl.dataflow;

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.services.jcr.core.value.EditableBinaryValue;
import org.exoplatform.services.jcr.impl.util.io.DirectoryHelper;
import org.exoplatform.services.jcr.impl.util.io.SpoolFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: StreamNewEditableValueData.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class StreamNewEditableValueData extends StreamNewValueData implements EditableBinaryValue
{
   public StreamNewEditableValueData(InputStream stream, int orderNumber, SpoolConfig spoolConfig) throws IOException
   {
      // don't send any data there (no stream, no bytes)
      super(orderNumber, null, null, spoolConfig);

      SpoolFile sf = SpoolFile.createTempFile("jcrvdedit", null, spoolConfig.tempDirectory);
      OutputStream sfout = PrivilegedFileHelper.fileOutputStream(sf);
      try
      {
         DirectoryHelper.transfer(stream, sfout);
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
            if (LOG.isTraceEnabled())
            {
               LOG.trace("An exception occurred: " + e1.getMessage());
            }
         }
         throw new IOException("init error " + e.getMessage(), e);
      }
      finally
      {
         sfout.close();
         stream.close();
      }

      this.spoolFile = sf;
      this.channel = PrivilegedFileHelper.randomAccessFile(sf, "rw").getChannel();
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
      validateAndAdjustLenght(length, position, Integer.MAX_VALUE);

      MappedByteBuffer bb = channel.map(FileChannel.MapMode.READ_WRITE, position, length);

      ReadableByteChannel ch = Channels.newChannel(stream);
      ch.read(bb);
      ch.close();

      bb.force();
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
      {
         throw new IOException("Size must be higher or equals 0. But given " + size);
      }

      if (channel.size() < size)
      {
         // extend file
         MappedByteBuffer bb = channel.map(FileChannel.MapMode.READ_WRITE, size, 0);
         bb.force();
      }
      else
      {
         // truncate file
         channel.truncate(size);
      }
   }
}

