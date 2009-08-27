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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.exoplatform.services.log.Log;

import org.exoplatform.services.jcr.core.value.EditableBinaryValue;
import org.exoplatform.services.jcr.impl.dataflow.EditableValueData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.log.ExoLogger;

/**
 * a binary value implementation.
 * 
 * @author Gennady Azarenkov
 */
public class BinaryValue
   extends BaseValue
   implements EditableBinaryValue
{

   public static final int TYPE = PropertyType.BINARY;

   protected EditableValueData changedData = null;

   protected boolean changed = false;

   protected static Log log = ExoLogger.getLogger("jcr.BinaryValue");

   /**
    * @param text
    * @throws IOException
    */
   public BinaryValue(String text) throws IOException
   {
      super(TYPE, new TransientValueData(text));
   }

   /**
    * BinaryValue constructor.
    * 
    * @param stream
    *          InputStream
    * @param fileCleaner
    *          FileCleaner
    * @param tempDirectory
    *          File
    * @param maxFufferSize
    *          int
    * @throws IOException
    *           if error
    */
   public BinaryValue(InputStream stream, FileCleaner fileCleaner, File tempDirectory, int maxFufferSize)
            throws IOException
   {
      this(new TransientValueData(stream), fileCleaner, tempDirectory, maxFufferSize);
   }

   BinaryValue(TransientValueData data) throws IOException
   {
      super(TYPE, data);
   }

   /** used in ValueFactory.loadValue */
   BinaryValue(TransientValueData data, FileCleaner fileCleaner, File tempDirectory, int maxFufferSize)
            throws IOException
   {
      super(TYPE, data);
      internalData.setFileCleaner(fileCleaner);
      internalData.setTempDirectory(tempDirectory);
      internalData.setMaxBufferSize(maxFufferSize);
   }

   @Override
   public TransientValueData getInternalData()
   {
      if (changedData != null)
         return changedData;

      return super.getInternalData();
   }

   @Override
   protected LocalTransientValueData getLocalData(boolean asStream) throws IOException
   {

      if (this.changed)
      {
         // reset to be recreated with new stream/bytes
         this.data = null;
         this.changed = false;
      }

      return super.getLocalData(asStream);
   }

   public String getReference() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      return getInternalString();
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
      if (changedData == null)
      {
         changedData = this.getInternalData().createEditableCopy();
      }

      this.changedData.update(stream, length, position);

      this.changed = true;
   }

   /**
    * Truncates binary value to <code> size </code>
    * 
    * @param size
    * @throws IOException
    */
   public void setLength(long size) throws IOException, RepositoryException
   {
      if (changedData == null)
      {
         changedData = this.getInternalData().createEditableCopy();
      }

      this.changedData.setLength(size);

      this.changed = true;
   }

}
