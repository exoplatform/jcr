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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.jcr.RepositoryException;

import org.exoplatform.services.jcr.impl.dataflow.AbstractValueData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: FileStreamPersistedValueData.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class FileStreamPersistedValueData
   extends AbstractValueData
{

   protected final File file;

   /**
    * FileStreamPersistedValueData  constructor.
    *
    * @param file File
    * @param orderNumber int
    */
   public FileStreamPersistedValueData(File file, int orderNumber)
   {
      super(orderNumber);
      this.file = file;
   }

   /**
    * {@inheritDoc}
    */
   public InputStream getAsStream() throws IOException
   {
      return new FileInputStream(file);
   }

   /**
    * {@inheritDoc}
    */
   public byte[] getAsByteArray() throws IllegalStateException
   {
      throw new IllegalStateException(
               "It is illegal to call on FileStreamPersistedValueData due to potential lack of memory");
   }

   /**
    * {@inheritDoc}
    */
   public long getLength()
   {
      return file.length();
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
   public TransientValueData createTransientCopy() throws RepositoryException
   {
      try
      {
         return new FileStreamTransientValueData(file, orderNumber);
      }
      catch (IOException e)
      {
         throw new RepositoryException(e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean isTransient()
   {
      return false;
   }

   // TODO cleanup
   //  protected void finalize() throws Throwable {
   //    try {
   //      if (temp && !file.delete())
   //        log.warn("FilePersistedValueData could not remove temporary file on finalize "
   //            + file.getAbsolutePath());
   //    } finally {
   //      super.finalize();
   //    }
   //  }
}
