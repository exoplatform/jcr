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
package org.exoplatform.services.jcr.impl.dataflow.persistent;

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.dataflow.SpoolConfig;
import org.exoplatform.services.jcr.impl.util.io.SpoolFile;
import org.exoplatform.services.jcr.impl.util.io.SwapFile;
import org.exoplatform.services.jcr.storage.value.ValueStorageURLStreamHandler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.PrivilegedExceptionAction;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id$
 */
public class StreamPersistedValueData extends FilePersistedValueData
{

   /**
    * Original stream.
    */
   protected InputStream stream;

   /**
    * Reserved file to spool.
    */
   protected SpoolFile tempFile;

   /**
    * The URL to the resource
    */
   protected URL url;

   /**
    * StreamPersistedValueData constructor for stream data.
    */
   public StreamPersistedValueData(int orderNumber, InputStream stream, SpoolConfig spoolConfig) throws IOException
   {
      this(orderNumber, stream, null, spoolConfig);
   }

   /**
    * StreamPersistedValueData  constructor for data spooled to temp file.
    */
   public StreamPersistedValueData(int orderNumber, SpoolFile tempFile, SpoolConfig spoolConfig) throws IOException
   {
      this(orderNumber, tempFile, null, spoolConfig);
   }

   /**
    * StreamPersistedValueData constructor for stream data with know destination file.
    * <p/>
    * Destination file reserved for use in internal impl.
   */
   public StreamPersistedValueData(int orderNumber, InputStream stream, File destFile, SpoolConfig spoolConfig)
      throws IOException
   {
      super(orderNumber, destFile, spoolConfig);
      this.tempFile = null;
      this.stream = stream;
   }

   /**
    * StreamPersistedValueData  constructor for data spooled to temp file with know destination file.
    * <p/>
    * Destination file reserved for use in internal impl.
    *
    * @param orderNumber int
    * @param tempFile File
    */
   public StreamPersistedValueData(int orderNumber, SpoolFile tempFile, File destFile, SpoolConfig spoolConfig)
      throws IOException
   {
      super(orderNumber, destFile, spoolConfig);
      this.tempFile = tempFile;
      this.stream = null;

      if (tempFile != null)
      {
         tempFile.acquire(this);
      }
   }

   /**
    * StreamPersistedValueData constructor.
    */
   public StreamPersistedValueData(int orderNumber, URL url, SpoolConfig spoolConfig) throws IOException
   {
      super(orderNumber, null, spoolConfig);
      this.url = url;
   }

   /**
    * StreamPersistedValueData empty constructor for serialization.
    */
   public StreamPersistedValueData() throws IOException
   {
      super();
   }

   /**
    * Return original data stream or null. <br/>
    * For persistent transformation from non-spooled TransientValueData to persistent layer.<br/>
    * WARN: after the stream will be consumed it will not contains data anymore.  
    * 
    * @return InputStream data stream or null
    * @throws IOException if error occurs
    */
   public InputStream getStream() throws IOException
   {
      return stream;
   }

   /**
    * Return temp file or null. For transport from spooled TransientValueData to persistent layer. <br/>
    * WARN: after the save the temp file will be removed. So, temp file actual only during the save from transient state.
    * 
    * @return File temporary file or null
    */
   public SpoolFile getTempFile()
   {
      return tempFile;
   }

   /**
    * Sets persistent file. Will reset (null) temp file and stream. This method should be called only from 
    * persistent layer (Value storage).
    * 
    * @param file File
    * @throws FileNotFoundException 
    */
   public void setPersistedFile(File file) throws FileNotFoundException
   {
      if (file instanceof SwapFile)
      {
         ((SwapFile)file).acquire(this);
      }

      this.file = file;

      // JCR-2326 Release the current ValueData from tempFile users before
      // setting its reference to null so it will be garbage collected.
      if (this.tempFile != null)
      {
         this.tempFile.release(this);
         this.tempFile = null;
      }

      this.stream = null;
   }

   /**
    * Sets persistent URL. Will reset (null) temp file and stream. This method should be called only from 
    * persistent layer (Value storage).
    * 
    * @param url the url to which the data has been persisted
    */
   public void setPersistedURL(URL url) throws FileNotFoundException
   {
      this.url = url;

      // JCR-2326 Release the current ValueData from tempFile users before
      // setting its reference to null so it will be garbage collected.
      if (this.tempFile != null)
      {
         this.tempFile.release(this);
         this.tempFile = null;
      }

      this.stream = null;
   }

   /**
    * Return status of persisted state.
    * 
    * @return boolean, true if the ValueData was persisted to a storage, false otherwise.
    */
   public boolean isPersisted()
   {
      return file != null || url != null;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public long getLength()
   {
      if (file != null)
      {
         return PrivilegedFileHelper.length(file);
      }
      else if (tempFile != null)
      {
         return PrivilegedFileHelper.length(tempFile);
      }
      else if (stream instanceof FileInputStream)
      {
         try
         {
            return ((FileInputStream)stream).getChannel().size();
         }
         catch (IOException e)
         {
            return -1;
         }
      }
      else if (url != null)
      {
         try
         {
            URLConnection connection = url.openConnection();
            return connection.getContentLength();
         }
         catch (IOException e)
         {
            return -1;
         }
      }
      else
      {
         try
         {
            return stream.available();
         }
         catch (IOException e)
         {
            return -1;
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void finalize() throws Throwable
   {
      try
      {
         if (file instanceof SwapFile)
         {
            ((SwapFile)file).release(this);
         }

         if (tempFile != null)
         {
            tempFile.release(this);
         }
      }
      finally
      {
         super.finalize();
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected boolean internalEquals(ValueData another)
   {
      if (another instanceof StreamPersistedValueData)
      {
         StreamPersistedValueData streamValue = (StreamPersistedValueData)another;

         if (file != null && file.equals(streamValue.file))
         {
            return  true;
         }
         else if (tempFile != null && tempFile.equals(streamValue.tempFile))
         {
            return true;
         }
         else if (stream != null && stream == streamValue.stream)
         {
            return true;
         }
         else if (url != null && streamValue.url != null && url.getFile().equals(streamValue.url.getFile()))
         {
            return true;
         }
      }

      return false;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public InputStream getAsStream() throws IOException
   {
      if (url != null)
      {
         return url.openStream();
      }
      return super.getAsStream();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public byte[] getAsByteArray() throws IllegalStateException, IOException
   {
      if (url != null)
      {
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         InputStream is = url.openStream();
         try
         {
            byte[] bytes = new byte[1024];
            int length;
            while ((length = is.read(bytes)) != -1)
            {
               baos.write(bytes, 0, length);
            }
            return baos.toByteArray();
         }
         finally
         {
            is.close();
         }
      }
      return super.getAsByteArray();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public long read(OutputStream stream, long length, long position) throws IOException
   {
      if (url != null)
      {
         InputStream is = url.openStream();
         try
         {
            is.skip(position);
            byte[] bytes = new byte[(int)length];
            int lg = is.read(bytes);
            if (lg > 0)
               stream.write(bytes, 0, lg);
            return lg;
         }
         finally
         {
            is.close();
            stream.close();
         }
      }
      return super.read(stream, length, position);
   }

   /**
    * {@inheritDoc}
    */
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      orderNumber = in.readInt();

      // read canonical file path
      int size = in.readInt();
      if (size > 0)
      {
         byte[] buf = new byte[size];
         in.readFully(buf);
         final String path = new String(buf, "UTF-8");
         File f = new File(path);
         // validate if exists
         if (PrivilegedFileHelper.exists(f))
         {
            file = f;
         }
         else if (path.startsWith(ValueStorageURLStreamHandler.PROTOCOL + ":/"))
         {
            url = SecurityHelper.doPrivilegedMalformedURLExceptionAction(new PrivilegedExceptionAction<URL>()
            {
               public URL run() throws Exception
               {
                  return new URL(null, path, ValueStorageURLStreamHandler.INSTANCE);
               }
            });
         }
         else
         {
            file = null;
         }
      }
      else
      {
         // should not occurs but since we have a way to recover, it should not be
         // an issue
         file = null;
      }
   }

   /**
    * {@inheritDoc}
    */
   public void writeExternal(ObjectOutput out) throws IOException
   {
      if (url == null)
      {
         super.writeExternal(out);
         return;
      }
      out.writeInt(orderNumber);

      // write the path
      byte[] buf = url.toString().getBytes("UTF-8");
      out.writeInt(buf.length);
      out.write(buf);
   }

   /**
    * {@inheritDoc}
    */
   public PersistedValueData createPersistedCopy(int orderNumber) throws IOException
   {
      if (url != null)
         return new StreamPersistedValueData(orderNumber, url, spoolConfig);
      return super.createPersistedCopy(orderNumber);
   }

   /**
    * @return the url
    */
   public URL getUrl()
   {
      return url;
   }
}
