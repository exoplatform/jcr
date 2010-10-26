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

import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.impl.proccess.WorkerService;

/**
 * Created by The eXo Platform SAS. <br/> per workspace container file cleaner holder object
 * 
 * @author Gennady Azarenkov
 * @version $Id: WorkspaceFileCleanerHolder.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class FileCleanerHolder
{
   /**
    * Default amount of thread that may be used by WorkerService to serve LockRemovers.
    */
   public final int DEFAULT_THREAD_COUNT = 1;

   /**
    * WorkerService that executed LockRemover.
    */
   private final WorkerService workerService;

   /**
    * Constructor. Used in tests.
    * 
    * @param threadCount - how mane threads can serve FileCleaner tasks
    */
   public FileCleanerHolder(int threadCount)
   {
      workerService = new WorkerService(threadCount, "file-cleaner-");
   }

   /**
    * Constructor.
    * @param entry - RepositoryEntry that may contain lock-remover-max-threads parameter.
    */
   public FileCleanerHolder(RepositoryEntry entry)
   {
      int threadCount = DEFAULT_THREAD_COUNT;
      if (entry != null)
      {
         if (entry.getLockRemoverThreadsCount() > 0)
         {
            threadCount = entry.getLockRemoverThreadsCount();
         }
      }
      workerService = new WorkerService(threadCount, "file-cleaner-" + entry.getName());
   }

   public FileCleaner getFileCleaner()
   {
      return new FileCleaner(workerService);
   }

   public FileCleaner getFileCleaner(long timeout)
   {
      return new FileCleaner(workerService, timeout);
   }

   public void stop()
   {
      this.workerService.stop();
   }

}
