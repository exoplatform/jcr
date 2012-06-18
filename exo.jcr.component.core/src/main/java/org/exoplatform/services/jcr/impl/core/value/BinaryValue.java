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
package org.exoplatform.services.jcr.impl.core.value;

import org.exoplatform.services.jcr.core.value.EditableBinaryValue;
import org.exoplatform.services.jcr.core.value.ReadableBinaryValue;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.dataflow.EditableValueData;
import org.exoplatform.services.jcr.impl.dataflow.SpoolConfig;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

/**
 * a binary value implementation.
 * 
 * @author Gennady Azarenkov
 */
public class BinaryValue extends BaseValue implements EditableBinaryValue, ReadableBinaryValue
{

   public static final int TYPE = PropertyType.BINARY;

   protected boolean changed;

   protected final SpoolConfig spoolConfig;

   /**
   * BinaryValue constructor.
   * 
   * @param stream
   *          InputStream
   * @throws IOException
   *           if error
   */
   public BinaryValue(InputStream stream, SpoolConfig spoolConfig)
      throws IOException
   {
      this(new TransientValueData(0, stream, null, spoolConfig), spoolConfig);
   }

   /** 
    * For ValueFactory.loadValue(). 
    */
   BinaryValue(ValueData data, SpoolConfig spoolConfig)
   {
      super(TYPE, data);
      this.spoolConfig = spoolConfig;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getReference() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      return getString();
   }

   /**
    * Update with <code>length</code> bytes from the specified InputStream <code>stream</code> to
    * this binary value at <code>position</code>
    * 
    * @param stream
    *          the data.
    * @param length
    *          the number of bytes from buffer to write.
    * @param position
    *          position in file to write data
    * */
   public void update(InputStream stream, long length, long position) throws IOException, RepositoryException
   {
      if (!changed)
      {
         internalData = createEditableCopy(this.getInternalData());
      }

      ((EditableValueData)internalData).update(stream, length, position);
      invalidateStream();
   }

   /**
    * Truncates binary value to <code> size </code>
    * 
    * @param size
    * @throws IOException
    */
   public void setLength(long size) throws IOException, RepositoryException
   {
      if (!changed)
      {
         internalData = createEditableCopy(this.getInternalData());
      }

      ((EditableValueData)internalData).setLength(size);
      invalidateStream();
   }

   /**
    * Create editable ValueData copy.
    * 
    * @return EditableValueData
    * @throws RepositoryException
    *           if error occurs
    * @throws IOException 
    * @throws IllegalStateException 
    */
   private EditableValueData createEditableCopy(ValueData oldValue) throws RepositoryException, IllegalStateException,
      IOException
   {
      if (oldValue.isByteArray())
      {
         // bytes, make a copy of real data
         byte[] oldBytes = oldValue.getAsByteArray();
         byte[] newBytes = new byte[oldBytes.length];
         System.arraycopy(oldBytes, 0, newBytes, 0, newBytes.length);

         try
         {
            return new EditableValueData(newBytes, oldValue.getOrderNumber());
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
            return new EditableValueData(oldValue.getAsStream(), oldValue.getOrderNumber(), spoolConfig);
         }
         catch (FileNotFoundException e)
         {
            throw new RepositoryException("Create editable copy error. " + e, e);
         }
         catch (IOException e)
         {
            throw new RepositoryException("Create editable copy error. " + e, e);
         }
      }
   }
}
