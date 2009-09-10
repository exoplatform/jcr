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
/**
 * 
 */
/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.dataflow.persistent;

import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;

import java.io.File;
import java.io.IOException;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Class for Persistent ValueData in Session (transient) level. Methods getSpoolFile(),
 * setSpoolFile() should don't get/set persistent file. Method createTransientCopy() returns this
 * object.
 * 
 * <br/>
 * Date: 09.06.2009
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: FileStreamTransientValueData.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class FileStreamTransientValueData extends TransientValueData
{

   /**
    * FileStreamTransientValueData constructor.
    * 
    * @param file
    *          File from Value storage
    * @param orderNumber
    *          int
    * @throws IOException
    *           if error occurs
    */
   FileStreamTransientValueData(File file, int orderNumber) throws IOException
   {
      super(orderNumber, null, null, file, null, -1, null, false);
   }

   /**
    * FileStreamTransientValueData constructor for swap files. Swap file it's a temp file used by
    * container for BLOBs read from database or any remote storage.
    * 
    * @param file
    *          File from storage
    * @param orderNumber
    *          int
    * @param fileCleaner
    *          FileCleaner
    * @param deleteFile
    *          boolean, if true file will be deleted after the FileStreamTransientValueData will be
    *          GCed.
    * @throws IOException
    *           if error occurs
    */
   FileStreamTransientValueData(File file, int orderNumber, FileCleaner fileCleaner, boolean deleteFile)
      throws IOException
   {
      super(orderNumber, null, null, file, fileCleaner, -1, null, deleteFile);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public TransientValueData createTransientCopy() throws RepositoryException
   {
      return this;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public File getSpoolFile()
   {
      return null;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public long getLength()
   {
      return spoolFile.length();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void setPersistedFile(File spoolFile)
   {
      assert !true : "Set of persistet file is out of contract.";
   }

   /**
    * {@inheritDoc}
    */
   public boolean isTransient()
   {
      return false;
   }

}
