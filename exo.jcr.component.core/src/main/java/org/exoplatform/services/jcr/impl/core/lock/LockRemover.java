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

import org.exoplatform.services.jcr.impl.proccess.WorkerThread;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: LockRemover.java 11987 2008-03-17 09:06:06Z ksm $
 */
public class LockRemover extends WorkerThread
{

   private final Log log = ExoLogger.getLogger("jcr.lock.LockRemover");

   public static final long DEFAULT_THREAD_TIMEOUT = 30000; // 30

   // sec

   private final WorkspaceLockManager lockManagerImpl;

   public LockRemover(WorkspaceLockManager lockManagerImpl)
   {
      this(lockManagerImpl, DEFAULT_THREAD_TIMEOUT);
   }

   private LockRemover(WorkspaceLockManager lockManagerImpl, long timeout)
   {
      super(timeout);
      this.lockManagerImpl = lockManagerImpl;
      setName("LockRemover " + getId());
      setPriority(Thread.MIN_PRIORITY);
      setDaemon(true);
      start();
      if (log.isDebugEnabled())
         log.debug("LockRemover instantiated name= " + getName() + " timeout= " + timeout);
   }

   @Override
   protected void callPeriodically() throws Exception
   {
      lockManagerImpl.removeExpired();
   }
}
