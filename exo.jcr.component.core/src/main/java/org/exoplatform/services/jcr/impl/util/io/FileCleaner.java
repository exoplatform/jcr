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
package org.exoplatform.services.jcr.impl.util.io;

import org.exoplatform.services.jcr.impl.proccess.WorkerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: FileCleaner.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class FileCleaner
{

   protected static final long DEFAULT_TIMEOUT = 30000;

   protected static Log log = ExoLogger.getLogger("exo.jcr.component.core.FileCleaner");

   /**
    * FileCleanerTask is a task that might be executed with WorkerService. This task 
    * tries to remove single file. If remove is failed it register itself to execute again. 
    */
   class FileCleanerTask implements Runnable
   {
      protected final File file;

      private final WorkerService executor;

      FileCleanerTask(WorkerService workerService, File file)
      {
         this.file = file;
         this.executor = workerService;
      }

      /**
       * {@inheritDoc} 
       */
      public void run()
      {
         if (PrivilegedFileHelper.exists(file))
         {
            if (!PrivilegedFileHelper.delete(file))
            {
               if (log.isDebugEnabled())
                  log.debug("Could not delete " + (file.isDirectory() ? "directory" : "file")
                     + ". Will try next time: " + PrivilegedFileHelper.getAbsolutePath(file));
               // delete is failed, so execute this task again 
               executor.executeDelay(this, timeout);
            }
            else if (log.isDebugEnabled())
            {
               log.debug((file.isDirectory() ? "Directory" : "File") + " deleted : "
                  + PrivilegedFileHelper.getAbsolutePath(file));
            }
         }
      }
   }

   final long timeout;

   final WorkerService workerService;

   /**
    * TODO this constructor used only in ext project. Clean it.
    * 
    * @param timeout
    */
   public FileCleaner(long timeout)
   {
      this(new WorkerService(1), DEFAULT_TIMEOUT);
   }

   public FileCleaner(WorkerService workerService)
   {
      this(workerService, DEFAULT_TIMEOUT);
   }

   public FileCleaner(WorkerService workerService, long timeout)
   {
      this.timeout = timeout;
      this.workerService = workerService;
   }

   /**
    * Add file to special removing queue.  
    * 
    * @param file - file that must be removed
    */
   public void addFile(File file)
   {
      if (PrivilegedFileHelper.exists(file))
      {
         workerService.executeDelay(new FileCleanerTask(workerService, file), timeout);
      }
   }

}
