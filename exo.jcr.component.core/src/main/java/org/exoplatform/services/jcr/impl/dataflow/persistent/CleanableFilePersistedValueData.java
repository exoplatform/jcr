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
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.impl.util.io.SwapFile;

import java.io.IOException;

/**
 * Created by The eXo Platform SAS. Implementation of FileStream ValueData secures deleting file in
 * object finalization
 * 
 * @author Gennady Azarenkov
 * @version $Id: CleanableFilePersistedValueData.java 35209 2009-08-07 15:32:27Z pnedonosko $
 */
public class CleanableFilePersistedValueData extends FilePersistedValueData
{

   protected FileCleaner fileCleaner;

   /**
    * Empty constructor for serialization.
    */
   public CleanableFilePersistedValueData() throws IOException
   {
      super();
   }

   /**
    * CleanableFilePersistedValueData constructor.
    */
   public CleanableFilePersistedValueData(int orderNumber, SwapFile file, FileCleaner fileCleaner) throws IOException
   {
      super(orderNumber, file);
      this.fileCleaner = fileCleaner;

      file.acquire(this);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void finalize() throws Throwable
   {
      try
      {
         // release file
         ((SwapFile)file).release(this);

         if (!PrivilegedFileHelper.delete(file))
         {
            fileCleaner.addFile(file);

            if (LOG.isDebugEnabled())
            {
               LOG.debug("Could not remove temporary file on finalize: inUse=" + (((SwapFile)file).inUse()) + ", "
                  + PrivilegedFileHelper.getAbsolutePath(file));
            }
         }
      }
      finally
      {
         super.finalize();
      }
   }
}
