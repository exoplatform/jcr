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
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.dataflow.SpoolConfig;
import org.exoplatform.services.jcr.impl.dataflow.StreamValueData;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id$
 */
public class FilePersistedValueData extends StreamValueData implements PersistedValueData
{

   /**
    * Persisted file is located in value storage.
    */
   protected File file;

   /**
    * Empty constructor to serialization.
    */
   public FilePersistedValueData() throws IOException
   {
      this(0, null, SpoolConfig.getDefaultSpoolConfig());
   }

   /**
    * FilePersistedValueData constructor.
    */
   public FilePersistedValueData(int orderNumber, File file, SpoolConfig spoolConfig) throws IOException
   {
      super(orderNumber, null, null, spoolConfig);
      this.file = file;
   }

   /**
    * Returns persisted file represents value data. 
    */
   public File getFile()
   {
      return file;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public InputStream getAsStream() throws IOException
   {
      return PrivilegedFileHelper.fileInputStream(file);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public byte[] getAsByteArray() throws IllegalStateException, IOException
   {
      return fileToByteArray(file);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public long getLength()
   {
      return PrivilegedFileHelper.length(file);
   }

   /**
    * {@inheritDoc}
    */
   public boolean isByteArray()
   {
      return false;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public long read(OutputStream stream, long length, long position) throws IOException
   {
      return readFromFile(stream, file, length, position);
   }

   /**
    * {@inheritDoc}
    */
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      orderNumber = in.readInt();

      // read canonical file path
      int size = in.readInt();
      if (size >= 0)
      {
         byte[] buf = new byte[size];
         in.readFully(buf);

         File f = new File(new String(buf, "UTF-8"));
         // validate if exists
         if (PrivilegedFileHelper.exists(f))
         {
            file = f;
         }
         else
         {
            file = null;
         }
      }
      else
      {
         // should not occurs
         throw new IOException("readExternal: Persisted ValueData with null file found");
      }
   }

   /**
    * {@inheritDoc}
    */
   public void writeExternal(ObjectOutput out) throws IOException
   {
      out.writeInt(orderNumber);

      // write canonical file path
      if (file != null)
      {
         byte[] buf = PrivilegedFileHelper.getCanonicalPath(file).getBytes("UTF-8");
         out.writeInt(buf.length);
         out.write(buf);
      }
      else
      {
         throw new IOException("writeExternal: Persisted ValueData with null file found");
      }
   }

   /**
    * {@inheritDoc}
    */
   protected boolean internalEquals(ValueData another)
   {
      if (another instanceof FilePersistedValueData)
      {
         return file.equals(((FilePersistedValueData)another).file);
      }

      return false;
   }

   /**
    * {@inheritDoc}
    */
   public PersistedValueData createPersistedCopy(int orderNumber) throws IOException
   {
      return new FilePersistedValueData(orderNumber, file, spoolConfig);
   }
}
