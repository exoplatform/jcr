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
package org.exoplatform.services.jcr.impl.core.lock;

import org.exoplatform.services.jcr.impl.proccess.WorkerService;

import java.util.concurrent.ScheduledFuture;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: LockRemover.java 11987 2008-03-17 09:06:06Z ksm $
 */
public class LockRemover
{
   public static final long DEFAULT_THREAD_TIMEOUT = 30000; // 30 sec

   private final WorkerService workerService;

   private final WorkspaceLockManager lockManager;

   private final long timeout;

   private ScheduledFuture lockRemoverTask = null;

   class LockRemoverTask implements Runnable
   {
      private final WorkspaceLockManager lockManager;

      LockRemoverTask(WorkspaceLockManager lockManager)
      {
         this.lockManager = lockManager;
      }

      public void run()
      {
         lockManager.removeExpired();
      }
   }

   protected LockRemover(WorkerService workerService, WorkspaceLockManager lockManager)
   {
      this(workerService, lockManager, DEFAULT_THREAD_TIMEOUT);
   }

   protected LockRemover(WorkerService workerService, WorkspaceLockManager lockManager, long timeout)
   {
      this.workerService = workerService;
      this.lockManager = lockManager;
      this.timeout = timeout;
   }

   public void start()
   {
      if (lockRemoverTask != null)
      {
         stop();
      }
      lockRemoverTask = workerService.executePeriodically(new LockRemoverTask(lockManager), timeout);
   }

   public void stop()
   {
      lockRemoverTask.cancel(false);
      lockRemoverTask = null;
   }

}
