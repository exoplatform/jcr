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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.impl.storage.value.fs.operations;

import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.ChangedSizeHandler;
import org.exoplatform.services.jcr.impl.storage.value.ValueDataResourceHolder;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;

import java.io.File;
import java.io.IOException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>
 * Date: 03.04.2009
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: WriteValue.java 111 2008-11-11 11:11:11Z pnedonosko $
 */
public class WriteValue extends ValueFileOperation
{

   /**
    * Affected file.
    */
   protected final File file;

   /**
    * Value to save.
    */
   protected final ValueData value;

   /**
    * File lock.
    */
   protected ValueFileLock fileLock;

   /**
    * Accumulates content size.
    */
   protected final ChangedSizeHandler sizeHandler;

   /**
    * WriteValue  constructor.
    */
   public WriteValue(File file, ValueData value, ValueDataResourceHolder resources, FileCleaner cleaner, File tempDir,
      ChangedSizeHandler sizeHandler)
   {
      super(resources, cleaner, tempDir);

      this.file = file;
      this.value = value;
      this.sizeHandler = sizeHandler;
   }

   /**
    * {@inheritDoc}
    */
   public void execute() throws IOException
   {
      makePerformed();

      fileLock = new ValueFileLock(file);
      fileLock.lock();
   }

   public void prepare() throws IOException
   {
      if (fileLock != null)
      {
         // be sure the destination dir exists (case for Tree-style storage)
         file.getParentFile().mkdirs();
         if (file.exists())
         {
            // The file still exists so either it is a file that could not be removed
            // or it is a multi update use case, in both cases we will need
            // to prevent the file cleaner to remove it
            cleaner.removeFile(file);
         }
         // write value to the file
         long contentSize = writeValue(file, value);
         sizeHandler.accumulateNewSize(contentSize);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void rollback() throws IOException
   {
      if (fileLock != null)
      {
         try
         {
            if (file.exists() && !file.delete())
            {
               cleaner.addFile(file);
            }
         }
         finally
         {
            fileLock.unlock();
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void twoPhaseCommit() throws IOException
   {
      if (fileLock != null)
      {
         fileLock.unlock();
      }
   }
}
