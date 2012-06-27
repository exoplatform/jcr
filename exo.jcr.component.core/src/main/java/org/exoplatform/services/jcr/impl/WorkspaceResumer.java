/*
 * Copyright (C) 2012 eXo Platform SAS.
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

import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rpc.RPCException;
import org.exoplatform.services.rpc.RPCService;
import org.exoplatform.services.rpc.RemoteCommand;
import org.exoplatform.services.rpc.TopologyChangeEvent;
import org.exoplatform.services.rpc.TopologyChangeListener;
import org.picocontainer.Startable;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:aplotnikov@exoplatform.com">Andrey Plotnikov</a>
 * @version $Id: WorkspaceResumer.java 34360 25.05.2012 andrew.plotnikov $
 */
public class WorkspaceResumer implements Startable, TopologyChangeListener
{

   /**
    * The logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.WorkspaceResumer");

   /**
    * The RPC Service is using to communicate with other nodes.
    */
   private final RPCService rpcService;

   /**
    * Request to all nodes to check if there is someone who responsible for resuming.
    */
   private RemoteCommand requestForResponsibleForResuming;

   /**
    * Workspace configuration.
    */
   private final WorkspaceEntry wsEntry;

   /**
    * Related repository.
    */
   private final RepositoryImpl reposistory;

   /**
    * Indicates that node keep responsible for resuming.
    */
   public final AtomicBoolean responsibleForResuming = new AtomicBoolean(false);

   /**
    * WorkspaceResumer constructor.
    */
   public WorkspaceResumer(final RPCService rpcService, RepositoryImpl reposistory, WorkspaceEntry wsEntry)
   {
      this.rpcService = rpcService;
      this.wsEntry = wsEntry;
      this.reposistory = reposistory;
   }

   /**
    * Unregister remote commands.
    */
   private void unregisterRemoteCommands()
   {
      rpcService.unregisterCommand(requestForResponsibleForResuming);
      rpcService.unregisterTopologyChangeListener(this);
   }

   /**
    * Register remote commands.
    */
   private void registerRemoteCommands()
   {
      requestForResponsibleForResuming = rpcService.registerCommand(new RemoteCommand()
      {
         public String getId()
         {
            return this.getClass().getName() + "-requestForResponsibilityForResuming-" + wsEntry.getUniqueName();
         }

         public Serializable execute(Serializable[] args) throws Throwable
         {
            return responsibleForResuming.get();
         }
      });

      rpcService.registerTopologyChangeListener(this);
   }

   /**
    * {@inheritDoc}
    */
   public void start()
   {
      registerRemoteCommands();
   }

   /**
    * {@inheritDoc}
    */
   public void stop()
   {
      unregisterRemoteCommands();
   }

   /**
    * Is invoked when suspend on workspace has been acted.
    */
   public void onSuspend()
   {
      LOG.info("Suspending workspace " + wsEntry.getUniqueName());
      responsibleForResuming.set(true);
   }

   /**
    * Is invoked when resume on workspace has been acted.
    */
   public void onResume()
   {
      LOG.info("Setting workspace " + wsEntry.getUniqueName() + " online");
      responsibleForResuming.set(false);
   }

   /**
    * {@inheritDoc}
    */
   public void onChange(TopologyChangeEvent event)
   {
      try
      {
         if (rpcService.isCoordinator())
         {
            final WorkspaceContainerFacade wsContainer = reposistory.getWorkspaceContainer(wsEntry.getName());

            if (wsContainer.getState() == ManageableRepository.SUSPENDED)
            {
               new Thread()
               {
                  @Override
                  public void run()
                  {
                     try
                     {
                        List<Object> results =
                           rpcService.executeCommandOnAllNodes(requestForResponsibleForResuming, true);
                        for (Object result : results)
                        {
                           if (!(result instanceof Throwable) && (Boolean)result)
                           {
                              return;
                           }
                        }

                        wsContainer.setState(ManageableRepository.ONLINE);
                     }
                     catch (SecurityException e)
                     {
                        LOG.error("You haven't privileges to execute remote command", e);
                     }
                     catch (RepositoryException e)
                     {
                        LOG.error("Can't resume workspace", e);
                     }
                     catch (RPCException e)
                     {
                        LOG.error("Can't resume workspace", e);
                     }
                  }
               }.start();
            }
         }
      }
      catch (RPCException e)
      {
         LOG.error("Exception during command execution", e);
      }
   }
}
