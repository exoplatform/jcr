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

package org.exoplatform.services.jcr.impl.storage.value.fs.operations;

import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.StreamPersistedValueData;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>
 * Date: 03.04.2009
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: ValueFileIOHelper.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class ValueFileIOHelper
{

   /**
    * I/O buffer size for internal VS operations (32K).
    */
   public static final int IOBUFFER_SIZE = 32 * 1024; // 32K

   /**
    * Helper logger.
    */
   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.ValueFileIOHelper");

   /**
    * Write value to a file.
    * 
    * @param file
    *          File
    * @param value
    *          ValueData
    * @return size of wrote content
    * @throws IOException
    *           if error occurs
    */
   protected long writeValue(File file, ValueData value) throws IOException
   {
      if (value.isByteArray())
      {
         return writeByteArrayValue(file, value);
      }
      else
      {
         return writeStreamedValue(file, value);
      }
   }

   /**
    * Write value array of bytes to a file.
    * 
    * @param file
    *          File
    * @param value
    *          ValueData
    * @return size of wrote content
    * @throws IOException
    *           if error occurs
    */
   protected long writeByteArrayValue(File file, ValueData value) throws IOException
   {
      OutputStream out = new FileOutputStream(file);
      try
      {
         byte[] data = value.getAsByteArray();
         out.write(data);

         return data.length;
      }
      finally
      {
         out.close();
      }
   }

   /**
    * Write streamed value to a file.
    * 
    * @param file
    *          File
    * @param value
    *          ValueData
    * @return size of wrote content
    * @throws IOException
    *           if error occurs
    */
   protected long writeStreamedValue(File file, ValueData value) throws IOException
   {
      long size;

      // stream Value
      if (value instanceof StreamPersistedValueData)
      {
         StreamPersistedValueData streamed = (StreamPersistedValueData)value;

         if (streamed.isPersisted())
         {
            // already persisted in another Value, copy it to this Value
            size = copyClose(streamed.getAsStream(), new FileOutputStream(file));
         }
         else
         {
            // the Value not yet persisted, i.e. or in client stream or spooled to a temp file
            File tempFile;
            if ((tempFile = streamed.getTempFile()) != null)
            {
               // it's spooled Value, try move its file to VS
               if (!tempFile.renameTo(file))
               {
                  // not succeeded - copy bytes, temp file will be deleted by transient ValueData
                  if (LOG.isDebugEnabled())
                  {
                     LOG.debug("Value spool file move (rename) to Values Storage is not succeeded. "
                        + "Trying bytes copy. Spool file: " + tempFile.getAbsolutePath() + ". Destination: "
                        + file.getAbsolutePath());
                  }

                  size = copyClose(new FileInputStream(tempFile), new FileOutputStream(file));
               }
               else
               {
                  size = file.length();
               }
            }
            else
            {
               // not spooled, use client InputStream
               size = copyClose(streamed.getStream(), new FileOutputStream(file));
            }

            // link this Value to file in VS
            streamed.setPersistedFile(file);
         }
      }
      else
      {
         // copy from Value stream to the file, e.g. from FilePersistedValueData to this Value
         size = copyClose(value.getAsStream(), new FileOutputStream(file));
      }

      return size;
   }

   /**
    * Stream value data to the output.
    * 
    * @param out
    *          OutputStream
    * @param value
    *          ValueData
    * @throws IOException
    *           if error occurs
    */
   protected long writeOutput(OutputStream out, ValueData value) throws IOException
   {
      if (value.isByteArray())
      {
         byte[] buff = value.getAsByteArray();
         out.write(buff);

         return buff.length;
      }
      else
      {
         InputStream in;
         if (value instanceof StreamPersistedValueData)
         {

            StreamPersistedValueData streamed = (StreamPersistedValueData)value;
            if (streamed.isPersisted())
            {
               // already persisted in another Value, copy it to this Value
               in = streamed.getAsStream();
            }
            else
            {
               in = streamed.getStream();
               if (in == null)
               {
                  in = new FileInputStream(streamed.getTempFile());
               }
            }
         }
         else
         {
            in = value.getAsStream();
         }

         try
         {
            return copy(in, out);
         }
         finally
         {
            in.close();
         }
      }
   }

   /**
    * Copy input to output data using NIO.
    * 
    * @param in
    *          InputStream
    * @param out
    *          OutputStream
    * @return The number of bytes, possibly zero, that were actually copied
    * @throws IOException
    *           if error occurs
    */
   protected long copy(InputStream in, OutputStream out) throws IOException
   {
      // compare classes as in Java6 Channels.newChannel(), Java5 has a bug in newChannel().
      boolean inFile = in instanceof FileInputStream && FileInputStream.class.equals(in.getClass());
      boolean outFile = out instanceof FileOutputStream && FileOutputStream.class.equals(out.getClass());
      if (inFile && outFile)
      {
         // it's user file
         FileChannel infch = ((FileInputStream)in).getChannel();
         FileChannel outfch = ((FileOutputStream)out).getChannel();

         long size = 0;
         long r = 0;
         do
         {
            r = outfch.transferFrom(infch, r, infch.size());
            size += r;
         }
         while (r < infch.size());
         return size;
      }
      else
      {
         // it's user stream (not a file)
         ReadableByteChannel inch = inFile ? ((FileInputStream)in).getChannel() : Channels.newChannel(in);
         WritableByteChannel outch = outFile ? ((FileOutputStream)out).getChannel() : Channels.newChannel(out);

         long size = 0;
         int r = 0;
         ByteBuffer buff = ByteBuffer.allocate(IOBUFFER_SIZE);
         buff.clear();
         while ((r = inch.read(buff)) >= 0)
         {
            buff.flip();

            // copy all
            do
            {
               outch.write(buff);
            }
            while (buff.hasRemaining());

            buff.clear();
            size += r;
         }

         if (outFile)
            ((FileChannel)outch).force(true); // force all data to FS

         return size;
      }
   }

   /**
    * Copy input to output data using NIO. Input and output streams will be closed after the
    * operation.
    * 
    * @param in
    *          InputStream
    * @param out
    *          OutputStream
    * @return The number of bytes, possibly zero, that were actually copied
    * @throws IOException
    *           if error occurs
    */
   protected long copyClose(InputStream in, OutputStream out) throws IOException
   {
      try
      {
         try
         {
            return copy(in, out);
         }
         finally
         {
            in.close();
         }
      }
      finally
      {
         out.close();
      }
   }
}
