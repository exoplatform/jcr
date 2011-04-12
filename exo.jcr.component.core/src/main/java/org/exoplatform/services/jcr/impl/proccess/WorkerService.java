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
package org.exoplatform.services.jcr.impl.proccess;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WorkerService.
 * 
 * @author <a href="mailto:karpenko.sergiy@gmail.com">Karpenko Sergiy</a>
 * @version $Id: WorkerService.java 34361 2010-08-24 23:58:59Z aheritier $
 */
public class WorkerService
{
   /**
    * Executor that process assigned command periodically.
    */
   private final ScheduledThreadPoolExecutor executor;

   static class WorkerThreadFactory implements ThreadFactory
   {
      static final AtomicInteger poolNumber = new AtomicInteger(1);

      final ThreadGroup group;

      final AtomicInteger threadNumber = new AtomicInteger(1);

      final String namePrefix;

      final boolean isDaemon;

      WorkerThreadFactory(String namePrefix)
      {
         this(namePrefix, false);
      }

      WorkerThreadFactory(String namePrefix, boolean isDaemon)
      {
         this.isDaemon = isDaemon;
         SecurityManager s = System.getSecurityManager();
         group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
         this.namePrefix = (namePrefix == null || namePrefix.isEmpty()) ? "pool-" + poolNumber.getAndIncrement() + "-thread-" : namePrefix + " ";
      }

      public Thread newThread(Runnable r)
      {
         Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
         t.setDaemon(isDaemon);
         if (t.getPriority() != Thread.NORM_PRIORITY)
            t.setPriority(Thread.NORM_PRIORITY);
         return t;
      }
   }

   /**
    * Constructor.
    * 
    * @param threadCount - max thread count that executor may use
    */
   public WorkerService(int threadCount)
   {
      this(threadCount, null);
   }

   /**
    * Constructor.
    * 
    * @param threadCount - max thread count that executor may use
    * @param threadNamePrefix - thread name prefix
    */
   public WorkerService(int threadCount, String threadNamePrefix)
   {
      this(threadCount, threadNamePrefix, true);
   }

   /**
    * Constructor.
    * 
    * @param threadCount - max thread count that executor may use
    * @param threadNamePrefix - thread name prefix
    * @param makeThreadsDaemon - does thread created by Service must be daemon
    */
   public WorkerService(int threadCount, String threadNamePrefix, boolean makeThreadsDaemon)
   {
      executor =
         new ScheduledThreadPoolExecutor(threadCount, new WorkerThreadFactory(threadNamePrefix, makeThreadsDaemon));
   }

   /**
    * Execute specified <code>command</code> periodically with <code>delay</code>.
    * 
    * @param command - command that must be executed
    * @param delay - delay between each command execution 
    * @return
    */
   public ScheduledFuture<?> executePeriodically(Runnable command, long delay)
   {
      return executor.scheduleWithFixedDelay(command, 0, delay, TimeUnit.MILLISECONDS);
   }

   /**
    * Initiates an orderly shutdown in which previously submitted tasks are executed, but no new tasks will be accepted. 
    */
   public void stop()
   {
      executor.shutdown();
   }
}
