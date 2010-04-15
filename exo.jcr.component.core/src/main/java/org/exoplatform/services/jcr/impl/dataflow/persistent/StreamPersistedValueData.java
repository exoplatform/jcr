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

import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.util.io.SpoolFile;
import org.exoplatform.services.jcr.impl.util.io.SwapFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id$
 */
public class StreamPersistedValueData extends FilePersistedValueData
{

   protected InputStream stream;

   protected File tempFile;

   /**
    * StreamPersistedValueData  constructor for stream data.
    *
    * @param orderNumber int
    * @param stream InputStream
    */
   public StreamPersistedValueData(int orderNumber, InputStream stream)
   {
      this(orderNumber, stream, null);
   }

   /**
    * StreamPersistedValueData  constructor for data spooled to temp file.
    *
    * @param orderNumber int
    * @param tempFile File
    * @throws FileNotFoundException 
    */
   public StreamPersistedValueData(int orderNumber, File tempFile) throws FileNotFoundException
   {
      this(orderNumber, tempFile, null);
   }

   /**
    * StreamPersistedValueData constructor for stream data with know destenation file.
    * <p/>
    * Destenation file reserved for use in JBC impl.
    * 
    * @param orderNumber int
    * @param stream InputStream
    * @param destFile File
    */
   public StreamPersistedValueData(int orderNumber, InputStream stream, File destFile)
   {
      super(orderNumber, destFile);
      this.tempFile = null;
      this.stream = stream;
   }

   /**
    * StreamPersistedValueData  constructor for data spooled to temp file with know destenation file.
    * <p/>
    * Destenation file reserved for use in JBC impl.
    *
    * @param orderNumber int
    * @param tempFile File
    * @throws FileNotFoundException 
    */
   public StreamPersistedValueData(int orderNumber, File tempFile, File destFile) throws FileNotFoundException
   {
      super(orderNumber, destFile);
      this.tempFile = tempFile;
      this.stream = null;

      if (tempFile != null && tempFile instanceof SpoolFile)
      {
         ((SpoolFile)tempFile).acquire(this);
      }
   }

   /**
    * StreamPersistedValueData empty constructor for serialization.
    */
   public StreamPersistedValueData()
   {
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
   public File getTempFile()
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

      this.tempFile = null;
      this.stream = null;
   }

   /**
    * Return status of persisted state.
    * 
    * @return boolean, true if the ValueData was persisted to a storage, false otherwise.
    */
   public boolean isPersisted()
   {
      return file != null;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public InputStream getAsStream() throws IOException
   {
      // TODO check if file exists, wait a bit (for replication etc.)
      return super.getAsStream();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public long getLength()
   {
      if (file != null)
      {
         return file.length();
      }
      else if (tempFile != null)
      {
         return tempFile.length();
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
   public TransientValueData createTransientCopy() throws RepositoryException
   {
      return new TransientValueData(this);
   }

   /**
    * {@inheritDoc}
    */
   protected void finalize() throws Throwable
   {
      try
      {
         if (file != null && file instanceof SwapFile)
         {
            ((SwapFile)file).release(this);
         }

         if (tempFile != null && tempFile instanceof SpoolFile)
         {
            ((SpoolFile)tempFile).release(this);
         }
      }
      finally
      {
         super.finalize();
      }
   }
}
