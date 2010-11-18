/*
 * Copyright (C) 2010 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl;

import org.exoplatform.container.BaseContainerLifecyclePlugin;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rpc.RPCException;
import org.exoplatform.services.rpc.RPCService;
import org.exoplatform.services.rpc.RemoteCommand;
import org.exoplatform.services.rpc.TopologyChangeEvent;
import org.exoplatform.services.rpc.TopologyChangeListener;

import java.io.Serializable;
import java.util.concurrent.CountDownLatch;

/**
 * This class is used to be able to synchronize the cluster nodes to avoid initializing
 * several times the same repository which could cause exceptions and prevent the nodes to start.
 * 
 * @author <a href="mailto:nicolas.filotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 */
public class RepositoryCreationSynchronizer
{

   /**
    * The logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.RepositoryCreationSynchronizer");

   /**
    * The RPC Service used to communicate with other nodes
    */
   private final RPCService rpcService;

   /**
    * The {@link RemoteCommand} used to indicate to the nodes that at least one workspace needs
    * to be initialized
    */
   private RemoteCommand needToInitWorkspaceCommand;

   /**
    * The {@link RemoteCommand} used to know whether the node have to wait or
    * not
    */
   private RemoteCommand shouldIWaitCommand;

   /**
    * Indicates whether at least one workspace has to be initialized
    */
   private boolean needToInitWorkspace;

   /**
    * This lock is used to synchronize the start process of all the nodes
    */
   private final CountDownLatch lock = new CountDownLatch(1);

   /**
    * Indicates whether this component has to be disabled or not
    */
   private final boolean disabled;

   /**
    * The default constructor that should be used in a non cluster environment
    */
   public RepositoryCreationSynchronizer(ExoContainerContext ctx)
   {
      this(null, ctx, null);
   }

   /**
    * The default constructor that should be used in a cluster environment
    */
   public RepositoryCreationSynchronizer(final RPCService rpcService, ExoContainerContext ctx, InitParams params)
   {
      this.rpcService = rpcService;
      this.disabled =
         rpcService == null
            || (params != null && params.getValueParam("disabled") != null && Boolean.valueOf(params.getValueParam(
               "disabled").getValue()));
      if (disabled && LOG.isDebugEnabled())
      {
         LOG.debug("The RepositoryCreationSynchronizer has been disabled");
      }
      if (rpcService != null)
      {
         shouldIWaitCommand = rpcService.registerCommand(new RemoteCommand()
         {

            public String getId()
            {
               return "exo.jcr.component.core.RepositoryCreationSynchronizer-shouldIWaitCommand";
            }

            public Serializable execute(Serializable[] args) throws Throwable
            {
               return shouldIWait();
            }
         });
         needToInitWorkspaceCommand = rpcService.registerCommand(new RemoteCommand()
         {

            public String getId()
            {
               return "exo.jcr.component.core.RepositoryCreationSynchronizer-needToInitWorkspaceCommand";
            }

            public Serializable execute(Serializable[] args) throws Throwable
            {
               needToInitWorkspace();
               return null;
            }
         });
         final RemoteCommand releaseCommand = rpcService.registerCommand(new RemoteCommand()
         {

            public String getId()
            {
               return "exo.jcr.component.core.RepositoryCreationSynchronizer-releaseCommand";
            }

            public Serializable execute(Serializable[] args) throws Throwable
            {
               release();
               return null;
            }
         });

         // Used to release all the nodes if needed once the current node is fully started
         // We need to wait for a full container start since upper applications could also not
         // support concurrent JCR initialization
         ctx.getContainer().addContainerLifecylePlugin(new BaseContainerLifecyclePlugin()
         {
            @Override
            public void startContainer(ExoContainer container) throws Exception
            {
               if (needToInitWorkspace)
               {
                  needToInitWorkspace = false;
                  try
                  {
                     if (LOG.isDebugEnabled())
                     {
                        LOG.debug("Release the other cluster nodes if needed.");
                     }
                     rpcService.executeCommandOnAllNodes(releaseCommand, false);
                  }
                  catch (Exception e)
                  {
                     LOG.error("Could not release all the nodes", e);
                  }
               }
            }
         });
         // Used to release the coordinator
         rpcService.registerTopologyChangeListener(new TopologyChangeListener()
         {
            public void onChange(TopologyChangeEvent event)
            {
               if (event.isCoordinator())
               {
                  release();
               }
            }
         });
      }
   }

   /**
    * Make the current thread wait until the {@link RepositoryCreationSynchronizer}
    * allows to proceed
    */
   public void waitForApproval(boolean isWorkspaceInitialized)
   {

      if (disabled)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace("The RepositoryCreationSynchronizer has been disabled so no need to make it wait.");
         }
         return;
      }
      if (rpcService == null)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace("No RPCService has been defined so we assume that we are in"
               + " a non cluster environment so no need to make it wait.");
         }
         return;
      }

      // The RPC Service has been defined so we assume that we are in a cluster environment
      if (!isWorkspaceInitialized)
      {
         // Set locally the value
         needToInitWorkspace();
         // A workspace has not been initialized, we need to notify everybody
         if (LOG.isDebugEnabled())
         {
            LOG.debug("Notify all the nodes that there is at least one workspace to initialize.");
         }
         try
         {
            rpcService.executeCommandOnAllNodes(needToInitWorkspaceCommand, false);
         }
         catch (Exception e)
         {
            LOG.warn("Could not notify all the nodes that there is at least one workspace to initialize.", e);
         }
      }
      if (lock.getCount() <= 0)
      {
         // We already have been released so no need to wait
         if (LOG.isTraceEnabled())
         {
            LOG.trace("We already have been released so no need to make it wait.");
         }
         return;
      }
      try
      {
         if (!needToInitWorkspace && isWorkspaceInitialized)
         {
            // Asking the coordinator if we have to wait since the workspace could be currently
            // initialized by the coordinator
            if (LOG.isDebugEnabled())
            {
               LOG.debug("Ask the coordinator if the local node needs to wait.");
            }
            Object result = rpcService.executeCommandOnCoordinator(shouldIWaitCommand, 0);
            if (LOG.isDebugEnabled())
            {
               LOG.debug("The response from the coordinator was " + result);
            }
            if (result instanceof Boolean && (Boolean)result)
            {
               // We wait to be release by the coordinator
               waitForCoordinator();
            }
         }
         else
         {
            // We wait to be release by the coordinator
            waitForCoordinator();
         }
      }
      catch (RPCException e)
      {
         LOG.warn("An error occured while executing the method waitForApproval()", e);
      }
   }

   /**
    * Make the current node wait until being released by the coordinator
    */
   private void waitForCoordinator()
   {
      LOG.info("Waiting to be released by the coordinator");
      try
      {
         lock.await();
      }
      catch (InterruptedException e)
      {
         Thread.currentThread().interrupt();
      }
   }

   /**
    * Used to know if the nodes need to wait
    */
   private boolean shouldIWait()
   {
      return needToInitWorkspace;
   }

   /**
    * Indicates that there is at least one workspace to initialize
    */
   private void needToInitWorkspace()
   {
      this.needToInitWorkspace = true;
   }

   /**
    * Releases the local node if needed
    */
   private void release()
   {
      if (lock.getCount() > 0)
      {
         LOG.info("The local node has been released.");
         lock.countDown();
      }
   }
}
