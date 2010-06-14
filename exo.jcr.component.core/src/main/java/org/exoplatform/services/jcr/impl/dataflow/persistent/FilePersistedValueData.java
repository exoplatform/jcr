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

import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.dataflow.AbstractPersistedValueData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.util.io.PrivilegedFileHelper;

import java.io.Externalizable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id$
 */

public class FilePersistedValueData extends AbstractPersistedValueData implements Externalizable
{

   /**
    * The serialVersionUID.
    */
   private static final long serialVersionUID = -8183328056670315388L;

   protected File file;

   protected FileChannel channel;

   /**
    *   Empty constructor to serialization.
    */
   public FilePersistedValueData()
   {
   }

   /**
    * FilePersistedValueData  constructor.
    * @param orderNumber int
    * @param file File
    */
   public FilePersistedValueData(int orderNumber, File file)
   {
      super(orderNumber);
      this.file = file;
   }

   public File getFile()
   {
      return file;
   }

   /**
    * {@inheritDoc}
    */
   public InputStream getAsStream() throws IOException
   {
      return PrivilegedFileHelper.fileInputStream(file);
   }

   /**
    * {@inheritDoc}
    * @throws IOException 
    */
   public byte[] getAsByteArray() throws IllegalStateException, IOException
   {
      return fileToByteArray();
   }

   /**
    * {@inheritDoc}
    */
   public long getLength()
   {
      return PrivilegedFileHelper.length(file);
   }

   /**
    * {@inheritDoc}
    */
   public long read(OutputStream stream, long length, long position) throws IOException
   {
      if (channel == null)
      {
         channel = PrivilegedFileHelper.fileInputStream(file).getChannel();
      }

      // validation
      if (position >= channel.size() && position > 0)
      {
         throw new IOException("Position " + position + " out of value size " + channel.size());
      }

      if (position + length >= channel.size())
      {
         length = channel.size() - position;
      }

      MappedByteBuffer bb = channel.map(FileChannel.MapMode.READ_ONLY, position, length);

      WritableByteChannel ch;
      if (stream instanceof FileOutputStream)
      {
         ch = ((FileOutputStream)stream).getChannel();
      }
      else
      {
         ch = Channels.newChannel(stream);
      }
      ch.write(bb);
      ch.close();

      return length;
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
   public boolean equals(ValueData another)
   {
      if (this == another)
      {
         return true;
      }

      if (!isByteArray() && !another.isByteArray())
      {
         // compare files 
         if (another instanceof TransientValueData)
         {
            // if another transient
            return file.equals(((TransientValueData)another).getSpoolFile());
         }
         else if (another instanceof FilePersistedValueData)
         {
            // both from peristent layer
            return file.equals(((FilePersistedValueData)another).getFile());
         }
      }
      return false;
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
    * Convert File to byte array. <br/>
    * WARNING: Potential lack of memory due to call getAsByteArray() on stream data.
    * 
    * @return byte[] bytes array
    */
   private byte[] fileToByteArray() throws IOException
   {
      // TODO do refactor of work with NIO and java6
      FileChannel fch = PrivilegedFileHelper.fileInputStream(file).getChannel();

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

         File f = PrivilegedFileHelper.file(new String(buf, "UTF-8"));
         // validate if exists
         if (f.exists())
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
         // TODO for tests byte[] buf = file.getPath().getBytes("UTF-8");
         byte[] buf = file.getCanonicalPath().getBytes("UTF-8");
         out.writeInt(buf.length);
         out.write(buf);
      }
      else
      {
         throw new IOException("writeExternal: Persisted ValueData with null file found");
      }
   }
}
