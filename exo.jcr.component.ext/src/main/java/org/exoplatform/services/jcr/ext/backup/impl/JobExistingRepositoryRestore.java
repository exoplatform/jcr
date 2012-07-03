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
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.ext.backup.BackupChainLog;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupChainLog;
import org.exoplatform.services.jcr.ext.backup.RepositoryRestoreExeption;
import org.exoplatform.services.jcr.impl.backup.Backupable;
import org.exoplatform.services.jcr.impl.core.SessionRegistry;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 2010
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id$
 */
public class JobExistingRepositoryRestore extends JobRepositoryRestore
{

   /**
    * JobExistingRepositoryRestore constructor.
    */
   public JobExistingRepositoryRestore(RepositoryService repoService, BackupManagerImpl backupManagerImpl,
      RepositoryEntry repositoryEntry, Map<String, File> workspacesMapping,
      File backupChainLogFile)
   {
      super(repoService, backupManagerImpl, repositoryEntry, workspacesMapping, backupChainLogFile);
   }

   @Override
   /**
    * {@inheritDoc}
    */
   protected void restoreRepository() throws RepositoryRestoreExeption
   {
      // list of components to clean
      List<Backupable> backupable = new ArrayList<Backupable>();

      try
      {
         // get current repository configuration
         RepositoryEntry repositoryEntry =
            repositoryService.getConfig().getRepositoryConfiguration(this.repositoryEntry.getName());

         if (repositoryEntry == null)
         {
            throw new RepositoryRestoreExeption("Current repository configuration " + this.repositoryEntry.getName()
               + " did not found");
         }

         boolean isDefault =
            repositoryService.getDefaultRepository().getConfiguration().getName().equals(repositoryEntry.getName());

         String systemWsName =
            repositoryService.getRepository(this.repositoryEntry.getName()).getConfiguration().getSystemWorkspaceName();

         //Create local copy of WorkspaceEntry for all workspaces
         ArrayList<WorkspaceEntry> workspaceList = new ArrayList<WorkspaceEntry>();
         workspaceList.addAll(repositoryEntry.getWorkspaceEntries());

         // get all backupable components
         for (WorkspaceEntry wEntry : workspaceList)
         {
            backupable.addAll(repositoryService.getRepository(this.repositoryEntry.getName())
               .getWorkspaceContainer(wEntry.getName()).getComponentInstancesOfType(Backupable.class));
         }

         //close all session
         for (WorkspaceEntry wEntry : workspaceList)
         {
            forceCloseSession(repositoryEntry.getName(), wEntry.getName());
         }

         //remove repository
         repositoryService.removeRepository(repositoryEntry.getName());

         // clean
         for (Backupable component : backupable)
         {
            component.clean();
         }

         super.restoreRepository();
      }
      catch (Throwable t) //NOSONAR
      {
         throw new RepositoryRestoreExeption("Repository " + repositoryEntry.getName() + " was not restored", t);
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void removeRepository(RepositoryService repositoryService, String repositoryName)
      throws RepositoryException, RepositoryConfigurationException
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
