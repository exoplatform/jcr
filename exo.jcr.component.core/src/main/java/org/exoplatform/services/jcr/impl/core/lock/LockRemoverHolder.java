package org.exoplatform.services.jcr.impl.core.lock;

import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.impl.proccess.WorkerService;

/**
 * LockRemoverHolder holds is a single per-repository LockRemover container.
 * @author <a href="mailto:foo@bar.org">Foo Bar</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 2009-07-22 23:58:59Z aheritier $
 *
 */
public class LockRemoverHolder
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
      int threadCount = DEFAULT_THREAD_COUNT;
      if (entry != null)
      {
         if (entry.getLockRemoverThreadsCount() > 0)
         {
            threadCount = entry.getLockRemoverThreadsCount();
         }
      }
      workerService = new WorkerService(threadCount, "lock-remover");
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

}
