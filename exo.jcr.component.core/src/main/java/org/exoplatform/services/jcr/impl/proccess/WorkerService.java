package org.exoplatform.services.jcr.impl.proccess;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
         this.namePrefix = namePrefix + "-" + poolNumber.getAndIncrement() + "-thread-";
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
      executor = new ScheduledThreadPoolExecutor(threadCount);
   }

   /**
    * Constructor.
    * 
    * @param threadCount - max thread count that executor may use
    * @param threadNamePrefix - thread name prefix
    */
   public WorkerService(int threadCount, String threadNamePrefix)
   {
      executor = new ScheduledThreadPoolExecutor(threadCount, new WorkerThreadFactory(threadNamePrefix));
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
