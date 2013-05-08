/*
 * Copyright (C) 2012 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.dataflow;

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.core.ExtendedPropertyType;
import org.exoplatform.services.jcr.datamodel.Identifier;
import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.IllegalPathException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.persistent.BooleanPersistedValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.ByteArrayPersistedValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.CalendarPersistedValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.CleanableFilePersistedValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.DoublePersistedValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.FilePersistedValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.LongPersistedValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.PersistedValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.StringPersistedValueData;
import org.exoplatform.services.jcr.impl.storage.value.fs.operations.ValueFileIOHelper;
import org.exoplatform.services.jcr.impl.util.JCRDateFormat;
import org.exoplatform.services.jcr.impl.util.io.SwapFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

/**
 * Utility class for managing value data.
 * 
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: PersistedValueDataFactory.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class ValueDataUtil
{
   /**
    * Read value data from stream.
    * 
    * @param cid
    *          property identifier
    * @param type
    *          property type, {@link PropertyType}
    * @param orderNum
    *          value data order number
    * @param version
    *          property persisted version    
    * @param content
    *          value data represented in stream     
    * @param SpoolConfig
    *          contains threshold for spooling
    * @return PersistedValueData
    * @throws IOException
    *           if any error is occurred
    */
   public static ValueDataWrapper readValueData(String cid, int type, int orderNumber, int version,
      final InputStream content, SpoolConfig spoolConfig) throws IOException
   {
      ValueDataWrapper vdDataWrapper = new ValueDataWrapper();

      byte[] buffer = new byte[0];
      byte[] spoolBuffer = new byte[ValueFileIOHelper.IOBUFFER_SIZE];
      
      int read;
      int len = 0;

      OutputStream out = null;
      SwapFile swapFile = null;

      try
      {
         // stream from database
         if (content != null)
         {
            while ((read = content.read(spoolBuffer)) >= 0)
            {
               if (out != null)
               {
                  // spool to temp file
                  out.write(spoolBuffer, 0, read);
               }
               else if (len + read > spoolConfig.maxBufferSize)
               {
                  // threshold for keeping data in memory exceeded;
                  // create temp file and spool buffer contents
                  swapFile = SwapFile.get(spoolConfig.tempDirectory, cid + orderNumber + "." + version,spoolConfig.fileCleaner);
                  if (swapFile.isSpooled())
                  {
                     // break, value already spooled
                     buffer = null;
                     break;
                  }
                  out = PrivilegedFileHelper.fileOutputStream(swapFile);
                  out.write(buffer, 0, len);
                  out.write(spoolBuffer, 0, read);
                  buffer = null;
               }
               else
               {
                  // reallocate new buffer and spool old buffer contents
                  byte[] newBuffer = new byte[len + read];
                  System.arraycopy(buffer, 0, newBuffer, 0, len);
                  System.arraycopy(spoolBuffer, 0, newBuffer, len, read);
                  buffer = newBuffer;
               }
               len += read;
            }
         }
      }
      finally
      {
         if (out != null)
         {
            out.close();
            swapFile.spoolDone();
         }
      }

      vdDataWrapper.size = len;
      if (swapFile != null)
      {
         vdDataWrapper.value = new CleanableFilePersistedValueData(orderNumber, swapFile, spoolConfig);
      }
      else
      {
         vdDataWrapper.value = createValueData(type, orderNumber, buffer);
      }

      return vdDataWrapper;
   }

   /**
    * Read value data from file.
    * 
    * @param type
    *          property type, {@link PropertyType}
    * @param file
    *          File
    * @param orderNum
    *          value data order number
    * @param SpoolConfig
    *          contains threshold for spooling
    * @return PersistedValueData
    * @throws IOException
    *           if any error is occurred
    */
   public static ValueDataWrapper readValueData(int type, int orderNumber, File file, SpoolConfig spoolConfig)
      throws IOException
   {
      ValueDataWrapper vdDataWrapper = new ValueDataWrapper();

      long fileSize = file.length();
      vdDataWrapper.size = fileSize;

      if (fileSize > spoolConfig.maxBufferSize)
      {
         vdDataWrapper.value = new FilePersistedValueData(orderNumber, file, spoolConfig);
      }
      else
      {
         FileInputStream is = new FileInputStream(file);
         try
         {
            byte[] data = new byte[(int)fileSize];
            byte[] buff =
               new byte[ValueFileIOHelper.IOBUFFER_SIZE > fileSize ? ValueFileIOHelper.IOBUFFER_SIZE : (int)fileSize];

            int rpos = 0;
            int read;

            while ((read = is.read(buff)) >= 0)
            {
               System.arraycopy(buff, 0, data, rpos, read);
               rpos += read;
            }

            vdDataWrapper.value = createValueData(type, orderNumber, data);
         }
         finally
         {
            is.close();
         }
      }

      return vdDataWrapper;
   }

   /**
    * Creates value data depending on its type. It avoids storing unnecessary bytes in memory 
    * every time.
    * 
    * @param type
    *          property data type, can be either {@link PropertyType} or {@link ExtendedPropertyType}
    * @param orderNumber
    *          value data order number
    * @param data
    *          value data represented in array of bytes
    */
   public static PersistedValueData createValueData(int type, int orderNumber, byte[] data) throws IOException
   {
      switch (type)
      {
         case PropertyType.BINARY :
         case PropertyType.UNDEFINED :
            return new ByteArrayPersistedValueData(orderNumber, data);

         case PropertyType.BOOLEAN :
            return new BooleanPersistedValueData(orderNumber, Boolean.valueOf(getString(data)));

         case PropertyType.DATE :
            try
            {
               return new CalendarPersistedValueData(orderNumber, JCRDateFormat.parse(getString(data)));
            }
            catch (ValueFormatException e)
            {
               throw new IOException("Can't create Calendar value", e);
            }

         case PropertyType.DOUBLE :
            return new DoublePersistedValueData(orderNumber, Double.valueOf(getString(data)));

         case PropertyType.LONG :
            return new LongPersistedValueData(orderNumber, Long.valueOf(getString(data)));

         case PropertyType.NAME :
            try
            {
               return new NamePersistedValueData(orderNumber, InternalQName.parse(getString(data)));
            }
            catch (IllegalNameException e)
            {
               throw new IOException(e.getMessage(), e);
            }

         case PropertyType.PATH :
            try
            {
               return new PathPersistedValueData(orderNumber, QPath.parse(getString(data)));
            }
            catch (IllegalPathException e)
            {
               throw new IOException(e.getMessage(), e);
            }

         case PropertyType.REFERENCE :
            return new ReferencePersistedValueData(orderNumber, new Identifier(data));

         case PropertyType.STRING :
            return new StringPersistedValueData(orderNumber, getString(data));

         case ExtendedPropertyType.PERMISSION :
            return new PermissionPersistedValueData(orderNumber, AccessControlEntry.parse(getString(data)));

         default :
            throw new IllegalStateException("Unknown property type " + type);
      }
   }

   /**
    * {@link AbstractValueData#createTransientCopy(int)}
    */
   public static TransientValueData createTransientCopy(ValueData valueData) throws IOException
   {
      return createTransientCopy(valueData, 0);
   }

   /**
    * {@link AbstractValueData#createTransientCopy(int)}
    */
   public static TransientValueData createTransientCopy(ValueData valueData, int orderNumber) throws IOException
   {
      if (valueData instanceof TransientValueData)
      {
         return createTransientCopy(((TransientValueData)valueData).delegate, orderNumber);
      }
      else if (valueData instanceof AbstractValueData)
      {
         return ((AbstractValueData)valueData).createTransientCopy(orderNumber);
      }
      else
      {
         return new TransientValueData(orderNumber, valueData.getAsByteArray());
      }
   }

   /**
    * Returns <code>Long</code> value.
    */
   public static Long getLong(ValueData valueData) throws RepositoryException
   {
      if (valueData instanceof TransientValueData)
      {
         return getLong(((TransientValueData)valueData).delegate);
      }
      
      return ((AbstractValueData) valueData).getLong();
   }

   /**
    * Returns <code>Double</code> value.
    */
   public static Double getDouble(ValueData valueData) throws RepositoryException
   {
      if (valueData instanceof TransientValueData)
      {
         return getDouble(((TransientValueData)valueData).delegate);
      }

      return ((AbstractValueData)valueData).getDouble();
   }

   /**
    * Returns <code>Date</code> value.
    */
   public static Calendar getDate(ValueData valueData) throws RepositoryException
   {
      if (valueData instanceof TransientValueData)
      {
         return getDate(((TransientValueData)valueData).delegate);
      }

      return ((AbstractValueData)valueData).getDate();
   }

   /**
    * Returns <code>Boolean</code> value.
    */
   public static Boolean getBoolean(ValueData valueData) throws RepositoryException
   {
      if (valueData instanceof TransientValueData)
      {
         return getBoolean(((TransientValueData)valueData).delegate);
      }

      return ((AbstractValueData)valueData).getBoolean();
   }

   /**
    * Returns <code>String</code> value.
    */
   public static String getString(ValueData valueData) throws RepositoryException
   {
      if (valueData instanceof TransientValueData)
      {
         return getString(((TransientValueData)valueData).delegate);
      }

      return ((AbstractValueData)valueData).getString();
   }

   /**
    * Returns <code>String</code> value.
    */
   public static InternalQName getName(ValueData valueData) throws RepositoryException
   {
      if (valueData instanceof TransientValueData)
      {
         return getName(((TransientValueData)valueData).delegate);
      }
      
      try
      {
         return ((AbstractValueData)valueData).getName();
      }
      catch (IllegalNameException e)
      {
         throw new RepositoryException(e.getMessage(), e);
      }
   }

   /**
    * Returns <code>String</code> value.
    */
   public static QPath getPath(ValueData valueData) throws RepositoryException
   {
      if (valueData instanceof TransientValueData)
      {
         return getPath(((TransientValueData)valueData).delegate);
      }

      return ((AbstractValueData)valueData).getPath();
   }

   /**
    * Returns <code>Reference</code> value.
    */
   public static String getReference(ValueData valueData) throws RepositoryException
   {
      if (valueData instanceof TransientValueData)
      {
         return getReference(((TransientValueData)valueData).delegate);
      }

      return ((AbstractValueData)valueData).getReference();
   }

   /**
    * Returns <code>AccessControlEntry</code> value.
    */
   public static AccessControlEntry getPermission(ValueData valueData) throws RepositoryException
   {
      if (valueData instanceof TransientValueData)
      {
         return getPermission(((TransientValueData)valueData).delegate);
      }

      return ((AbstractValueData)valueData).getPermission();
   }

   /**
    * Returns String data represented in array of bytes.
    */
   private static String getString(byte[] data) throws UnsupportedEncodingException
   {
      return new String(data, Constants.DEFAULT_ENCODING);
   }

   /**
    * Simply wraps {@link ValueData} and its size at storage.
    */
   public static class ValueDataWrapper
   {
      public long size;

      public PersistedValueData value;

      private ValueDataWrapper()
      {
      }
   }
}
