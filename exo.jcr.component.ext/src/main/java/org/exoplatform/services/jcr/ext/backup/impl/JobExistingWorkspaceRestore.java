/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.ext.backup.impl;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.ext.backup.BackupChainLog;
import org.exoplatform.services.jcr.ext.backup.BackupManager;
import org.exoplatform.services.jcr.ext.backup.WorkspaceRestoreException;
import org.exoplatform.services.jcr.impl.backup.Backupable;
import org.exoplatform.services.jcr.impl.core.SessionRegistry;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.List;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 2010
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id$
 */
public class JobExistingWorkspaceRestore extends JobWorkspaceRestore
{
   /**
    * The logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.ext.JobExistedWorkspaceRestore");

   /**
    * JobExistingWorkspaceRestore constructor.
    */
   public JobExistingWorkspaceRestore(RepositoryService repositoryService, BackupManager backupManager,
      String repositoryName, BackupChainLog log, WorkspaceEntry wEntry)
   {
      super(repositoryService, backupManager, repositoryName, log, wEntry);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void restoreWorkspace() throws WorkspaceRestoreException
   {
      try
      {
         // get current workspace configuration
         WorkspaceEntry wEntry = null;;
         for (WorkspaceEntry entry : repositoryService.getRepository(repositoryName).getConfiguration()
            .getWorkspaceEntries())
         {
            if (entry.getName().equals(this.wEntry.getName()))
            {
               wEntry = entry;
               break;
            }
         }

         if (wEntry == null)
         {
            throw new WorkspaceRestoreException("Workspace " + this.wEntry.getName()
               + " did not found in current repository " + repositoryName + " configuration");
         }

         // get all backupable components
         List<Backupable> backupable =
            repositoryService.getRepository(repositoryName).getWorkspaceContainer(wEntry.getName())
               .getComponentInstancesOfType(Backupable.class);

         // close all session
         forceCloseSession(repositoryName, wEntry.getName());

         repositoryService.getRepository(repositoryName).removeWorkspace(wEntry.getName());

         // clean
         for (Backupable component : backupable)
         {
            component.clean();
         }

         super.restoreWorkspace();
      }
      catch (Throwable t) //NOSONAR
      {
         throw new WorkspaceRestoreException("Workspace " + wEntry.getName() + " was not restored", t);
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void removeWorkspace(ManageableRepository mr, String workspaceName) throws RepositoryException
   {
   }

   /**
    * Close sessions on specific workspace.
    * 
    * @param repositoryName
    *          repository name
    * @param workspaceName
    *          workspace name
    * @return int return the how many sessions was closed
    * @throws RepositoryConfigurationException
    *           will be generate RepositoryConfigurationException
    * @throws RepositoryException
    *           will be generate RepositoryException
    */
   private int forceCloseSession(String repositoryName, String workspaceName) throws RepositoryException,
      RepositoryConfigurationException
   {
      ManageableRepository mr = repositoryService.getRepository(repositoryName);
      WorkspaceContainerFacade wc = mr.getWorkspaceContainer(workspaceName);

      SessionRegistry sessionRegistry = (SessionRegistry)wc.getComponent(SessionRegistry.class);

      return sessionRegistry.closeSessions(workspaceName);
   }
}
