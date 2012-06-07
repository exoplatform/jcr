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

import java.io.ByteArrayInputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id$
 */
public class ByteArrayPersistedValueData extends AbstractPersistedValueData implements Externalizable
{

   /**
    * The serialVersionUID.
    */
   private static final long serialVersionUID = -9131328056670315388L;
   
   protected byte[] data;
   
   /**
    * Empty constructor to serialization.
    */
   public ByteArrayPersistedValueData()
   {
      super(0);
   }

   /**
    * ByteArrayPersistedValueData constructor.
    * @param orderNumber
    *          int
    * @param data
    *          byte[]
    */
   public ByteArrayPersistedValueData(int orderNumber, byte[] data)
   {
      super(orderNumber);
      this.data = data;
   }

   /**
    * {@inheritDoc}
    */
   public InputStream getAsStream() throws IOException
   {
      return new ByteArrayInputStream(data);
   }

   /**
    * {@inheritDoc}
    */
   public byte[] getAsByteArray()
   {
      return data;
   }

   /**
    * {@inheritDoc}
    */
   public long getLength()
   {
      return data.length;
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

      // validation
      if (position >= data.length && position > 0)
         throw new IOException("Position " + position + " out of value size " + data.length);

      if (position + length >= data.length)
         length = data.length - position;

      stream.write(data, (int)position, (int)length);

      return length;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isByteArray()
   {
      return true;
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

      if (isByteArray() && another.isByteArray())
      {
         // by content
         try
         {
            return Arrays.equals(getAsByteArray(), another.getAsByteArray());
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
    * {@inheritDoc}
    */
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      orderNumber = in.readInt();
      
      data = new byte[in.readInt()];
      if (data.length > 0) in.readFully(data);
   }

   /**
    * {@inheritDoc}
    */
   public void writeExternal(ObjectOutput out) throws IOException
   {
      out.writeInt(orderNumber);
      
      out.writeInt(data.length);
      out.write(data);
   }

}
