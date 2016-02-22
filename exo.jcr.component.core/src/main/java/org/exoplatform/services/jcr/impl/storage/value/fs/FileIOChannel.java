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
package org.exoplatform.services.jcr.impl.storage.value.fs;

import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.dataflow.SpoolConfig;
import org.exoplatform.services.jcr.impl.dataflow.ValueDataUtil;
import org.exoplatform.services.jcr.impl.dataflow.ValueDataUtil.ValueDataWrapper;
import org.exoplatform.services.jcr.impl.dataflow.persistent.ChangedSizeHandler;
import org.exoplatform.services.jcr.impl.storage.value.ValueDataNotFoundException;
import org.exoplatform.services.jcr.impl.storage.value.ValueDataResourceHolder;
import org.exoplatform.services.jcr.impl.storage.value.ValueOperation;
import org.exoplatform.services.jcr.impl.storage.value.fs.operations.DeleteValues;
import org.exoplatform.services.jcr.impl.storage.value.fs.operations.ValueFileIOHelper;
import org.exoplatform.services.jcr.impl.storage.value.fs.operations.WriteValue;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.storage.value.ValueIOChannel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: FileIOChannel.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public abstract class FileIOChannel extends ValueFileIOHelper implements ValueIOChannel
{

   /**
    * Temporary directory. Used for I/O transaction operations and locks.
    */
   protected final File tempDir;

   /**
    * Storage root dir.
    */
   protected final File rootDir;

   /**
    * File cleaner used to clean swapped files.
    */
   protected final FileCleaner cleaner;

   /**
    * Concurrent access support for VS files.
    */
   protected final ValueDataResourceHolder resources;

   /**
    * Storage Id.
    */
   protected final String storageId;

   /**
    * Changes to be saved on commit or rolled back on rollback.
    */
   protected final List<ValueOperation> changes = new ArrayList<ValueOperation>();

   /**
    * FileIOChannel constructor.
    * 
    * @param rootDir
    *          root directory
    * @param cleaner
    *          FileCleaner
    * @param storageId
    *          Storage ID
    */
   public FileIOChannel(File rootDir, FileCleaner cleaner, String storageId, ValueDataResourceHolder resources)
   {
      this.rootDir = rootDir;
      this.cleaner = cleaner;
      this.storageId = storageId;
      this.resources = resources;

      this.tempDir = new File(rootDir, FileValueStorage.TEMP_DIR_NAME);
   }

   /**
    * {@inheritDoc}
    */
   public void write(String propertyId, ValueData value, ChangedSizeHandler sizeHandler) throws IOException
   {
      WriteValue o =
         new WriteValue(getFile(propertyId, value.getOrderNumber()), value, resources, cleaner, tempDir, sizeHandler);
      o.execute();
      changes.add(o);
   }

   /**
    * {@inheritDoc}
    */
   public void delete(String propertyId) throws IOException
   {
      DeleteValues o = new DeleteValues(getFiles(propertyId), resources, cleaner, tempDir);
      o.execute();
      changes.add(o);
   }

   /**
    * {@inheritDoc}
    */
   public void prepare() throws IOException
   {
      for (ValueOperation vo : changes)
         vo.prepare();
   }
   
   /**
    * {@inheritDoc}
    */
   public void commit() throws IOException
   {
      try
      {
         for (ValueOperation vo : changes)
            vo.commit();
      }
      finally
      {
         changes.clear();
      }
   }

   /**
    * {@inheritDoc}
    */
   public void twoPhaseCommit() throws IOException
   {
      try
      {
         for (ValueOperation vo : changes)
            vo.twoPhaseCommit();
      }
      finally
      {
         changes.clear();
      }
   }

   /**
    * {@inheritDoc}
    */
   public void rollback() throws IOException
   {
      try
      {
         for (int p = changes.size() - 1; p >= 0; p--)
            changes.get(p).rollback();
      }
      finally
      {
         changes.clear();
      }
   }

   /**
    * {@inheritDoc}
    */
   public void close()
   {
   }

   /**
    * {@inheritDoc}
    */
   public ValueDataWrapper read(String propertyId, int orderNumber, int type, SpoolConfig spoolConfig) throws IOException
   {
      File file = getFile(propertyId, orderNumber);
      return ValueDataUtil.readValueData(type, orderNumber, file, spoolConfig);
   }

   /**
    * {@inheritDoc}
    */
   public void checkValueData(String propertyId, int orderNumber) throws ValueDataNotFoundException, IOException
   {
      File f = getFile(propertyId, orderNumber);
      if (!f.exists())
      {
         throw new ValueDataNotFoundException("Value data corresponding to property with [id=" + propertyId
            + ", ordernum=" + orderNumber + "] does not exist.");
      }
   }

   /**
    * {@inheritDoc}
    */
   public void repairValueData(String propertyId, int orderNumber) throws IOException
   {
      File f = getFile(propertyId, orderNumber);
      if (!f.createNewFile())
      {
         throw new IOException("Can not create empty file " + f.getAbsolutePath());
      }
   }

   /**
    * {@inheritDoc}
    */
   public long getValueSize(String propertyId, int orderNumber) throws IOException
   {
      File f = getFile(propertyId, orderNumber);
      return f.exists() ? f.length() : 0;
   }

   /**
    * {@inheritDoc}
    */
   public long getValueSize(String propertyId) throws IOException
   {
      long size = 0;
      for (File file : getFiles(propertyId))
      {
         size += file.length();
      }

      return size;
   }

   /**
    * Makes storage file path by propertyId and order number.<br>
    * 
    * @param propertyId
    *          String
    * @param orderNumber
    *          int
    * @return String with path
    */
   protected abstract String makeFilePath(String propertyId, int orderNumber);

   /**
    * Creates storage file by propertyId and order number.<br>
    * 
    * File used for read/write operations.
    * 
    * @param propertyId
    *          String
    * @param orderNumber
    *          int
    * @return actual file on file system related to given parameters
    */
   protected abstract File getFile(String propertyId, int orderNumber) throws IOException;

   /**
    * Creates storage files list by propertyId.<br>
    * 
    * NOTE: Files list used for <strong>delete</strong> operation.
    * 
    * @param propertyId
    *          String
    * @return actual files on file system related to given propertyId
    */
   protected abstract File[] getFiles(String propertyId) throws IOException;

   /**
    * {@inheritDoc}
    */
   public String getStorageId()
   {
      return storageId;
   }
}
