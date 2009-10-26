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
package org.exoplatform.services.jcr.ext.replication.lock;

import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.ext.replication.ReplicationService;
import org.exoplatform.services.jcr.impl.core.lock.FileSystemLockPersister;
import org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspacePersistentDataManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: ReplicationLockPersister.java 111 2008-11-11 11:11:11Z rainf0x $
 */

public class ReplicationLockPersister extends FileSystemLockPersister
{
   /**
    * The RrplicationService.
    */
   private final ReplicationService replicationService;

   /**
    * The apache logger.
    */
   private static Log log = ExoLogger.getLogger("ext.ReplicationLockPersister");

   /**
    * The definition start timeout.
    */
   private static final int START_TIMEOUT = 250;

   /**
    * Delay thread. The thread wait RrplicationService is successful start and call super.start();
    */
   private Thread delayStarterThread;

   /**
    * ReplicationLockPersister constructor.
    * 
    * @param dataManager
    *          the WorkspacePersistentDataManager
    * @param config
    *          the configuration to workspace
    * @param service
    *          the ReplicationService
    * @throws RepositoryConfigurationException
    *           will be generated RepositoryConfigurationException
    * @throws RepositoryException
    *           will be generated RepositoryException
    */
   public ReplicationLockPersister(WorkspacePersistentDataManager dataManager, WorkspaceEntry config,
      ReplicationService service) throws RepositoryConfigurationException, RepositoryException
   {
      super(dataManager, config);
      log.info("init");

      replicationService = service;
   }

   /**
    * {@inheritDoc}
    */
   public void start()
   {
      log.info("start");

      delayStarterThread = new Thread(new DelaySatrter());
      delayStarterThread.start();
   }

   /**
    * superStart. will be called super.start()
    */
   private void superStart()
   {
      super.start();
   }

   /**
    * {@inheritDoc}
    */
   public void stop()
   {
      log.info("stop");
   }

   /**
    * DelaySatrter. The DelaySatrter wait RrplicationService is successful start and call
    * super.start();
    */
   class DelaySatrter implements Runnable
   {
      /**
       * {@inheritDoc}
       */
      public void run()
      {
         try
         {
            while (!replicationService.isStarted())
               Thread.sleep(START_TIMEOUT);

            superStart();
         }
         catch (InterruptedException ie)
         {
            log.error("Can not start", ie);
         }
      }
   }
}
