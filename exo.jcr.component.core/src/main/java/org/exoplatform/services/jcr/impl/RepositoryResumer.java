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

import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
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

import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:aplotnikov@exoplatform.com">Andrey Plotnikov</a>
 * @version $Id: RepositoryResumer.java 34360 25.05.2012 andrew.plotnikov $
 *
 */
public class RepositoryResumer implements Startable, TopologyChangeListener
{

   /**
    * The logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.RepositoryResumer");

   /**
    * The RPC Service used to communicate with other nodes
    */
   private final RPCService rpcService;

   private final WorkspaceContainerFacade workspaceContainer;
   
   /**
    * Request to all nodes to check if there is someone who responsible for resuming.
    */
   private RemoteCommand requestForResponsibleForResuming;


   public RepositoryResumer(final RPCService rpcService, WorkspaceContainerFacade workspaceContainer)
   {
      this.rpcService = rpcService;
      this.workspaceContainer = workspaceContainer;
   }

   public RepositoryResumer(WorkspaceContainerFacade workspaceContainer)
   {
      this.rpcService = null;
      this.workspaceContainer = workspaceContainer;
   }

   /**
    * Unregister remote commands.
    */
   private void unregisterRemoteCommands()
   {
      if (rpcService != null)
      {
         rpcService.unregisterCommand(requestForResponsibleForResuming);
      }
   }

   /**
    * Register remote commands.
    */
   private void registerRemoteCommands()
   {
      if (rpcService != null)
      {
         requestForResponsibleForResuming = rpcService.registerCommand(new RemoteCommand()
         {

            public String getId()
            {
               return "org.exoplatform.services.jcr.impl.RepositoryResumer" + "-requestForResponsibilityForResuming-"
                  + workspaceContainer.getWorkspaceName();
            }

            public Serializable execute(Serializable[] args) throws Throwable
            {
               return workspaceContainer.getResponsibleForResuming();
            }
         });
      }
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
    * {@inheritDoc}
    */
   public void onChange(TopologyChangeEvent event)
   {
      try
      {
         if (rpcService.isCoordinator())
         {
            if (workspaceContainer.getState() == ManageableRepository.SUSPENDED)
            {
               try
               {
                  List<Object> results = rpcService.executeCommandOnAllNodes(requestForResponsibleForResuming, true);
                  for (Object result : results)
                  {
                     if ((Boolean)result)
                     {
                        return;
                     }
                  }

                  workspaceContainer.setState(ManageableRepository.ONLINE);
               }
               catch (SecurityException e)
               {
                  LOG.error("You haven't privileges to execute remote command", e);
               }
               catch (RepositoryException e)
               {
                  LOG.error("Can't resume component:", e);
               }
            }
         }
      }
      catch (RPCException e)
      {
         LOG.error("Exception during command execution", e);
      }
   }
}
