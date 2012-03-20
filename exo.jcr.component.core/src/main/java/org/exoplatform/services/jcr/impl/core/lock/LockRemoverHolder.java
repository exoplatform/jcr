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

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.impl.proccess.WorkerService;
import org.picocontainer.Startable;

/**
 * LockRemoverHolder holds is a single per-repository LockRemover container.
 * 
 * @author <a href="mailto:karpenko.sergiy@gmail.com">Karpenko Sergiy</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 2009-07-22 23:58:59Z aheritier $
 */
public class LockRemoverHolder implements Startable
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
    * Constructor.
    * @param entry - RepositoryEntry that may contain lock-remover-max-threads parameter.
    */
   public LockRemoverHolder(RepositoryEntry entry)
   {
      this(null, entry);
   }
   
   /**
    * Constructor.
    * @param ctx - The {@link ExoContainerContext} in which the {@link LockRemoverHolder}
    * is registered
    * @param entry - RepositoryEntry that may contain lock-remover-max-threads parameter.
    */
   public LockRemoverHolder(ExoContainerContext ctx, RepositoryEntry entry)
   {
      int threadCount = DEFAULT_THREAD_COUNT;
      if (entry != null && entry.getLockRemoverThreadsCount() > 0)
      {
         threadCount = entry.getLockRemoverThreadsCount();
      }
      workerService =
               new WorkerService(threadCount, "Lock Remover "
                        + (ctx != null ? ctx.getName() : (entry == null ? "" : entry.getName())));
   }

   /**
    * Returns LockRemover object that removes expired locks from LockManager. Default timeout used.
    * 
    * @param lockManager - LockManager that going to be cleaned with returned LockRemover.
    * @return LockRemover
    */
   public LockRemover getLockRemover(WorkspaceLockManager lockManager)
   {
      return new LockRemover(workerService, lockManager);
   }

   /**
    * Returns LockRemover object that removes expired locks from LockManager.
    * 
    * @param lockManager - LockManager that going to be cleaned with returned LockRemover.
    * @param timeout - LockRemover will check LockManager with delay setted in timeout parameter
    * @return LockRemover
    */
   public LockRemover getLockRemover(WorkspaceLockManager lockManager, long timeout)
   {
      return new LockRemover(workerService, lockManager, timeout);
   }

   /**
    * @see org.picocontainer.Startable#start()
    */
   public void start()
   {
   }

   /**
    * @see org.picocontainer.Startable#stop()
    */
   public void stop()
   {
      workerService.stop();
   }
}
