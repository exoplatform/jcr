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
import org.exoplatform.services.jcr.impl.RepositoryServiceImpl;
import org.exoplatform.services.jcr.impl.core.SessionRegistry;
import org.exoplatform.services.jcr.impl.util.jdbc.cleaner.DBCleanerService;

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
public class JobExistedRepositoryRestore extends JobRepositoryRestore
{

   /**
    * Database cleaner.
    */
   private final DBCleanerService dbCleanerService;

   public JobExistedRepositoryRestore(RepositoryService repoService, BackupManagerImpl backupManagerImpl,
      RepositoryEntry repositoryEntry, Map<String, BackupChainLog> workspacesMapping,
      RepositoryBackupChainLog backupChainLog)
   {
      super(repoService, backupManagerImpl, repositoryEntry, workspacesMapping, backupChainLog);
      this.dbCleanerService = new DBCleanerService();
   }

   @Override
   /**
    * {@inheritDoc}
    */
   protected void restoreRepository() throws RepositoryRestoreExeption
   {
      try
      {
         RepositoryEntry repositoryEntry =
            repositoryService.getConfig().getRepositoryConfiguration(this.repositoryEntry.getName());

         if (repositoryEntry == null)
         {
            throw new RepositoryRestoreExeption("Repository " + this.repositoryEntry.getName()
               + " did not found configuration");
         }

         boolean isDefault =
            repositoryService.getDefaultRepository().getConfiguration().getName().equals(repositoryEntry.getName());

         //close all session
         for (WorkspaceEntry wEntry : repositoryEntry.getWorkspaceEntries())
         {
            forceCloseSession(repositoryEntry.getName(), wEntry.getName());
         }

         //remove repository
         if (isDefault)
         {
            ((RepositoryServiceImpl)repositoryService).removeDefaultRepository();
         }
         else
         {
            repositoryService.removeRepository(repositoryEntry.getName());
         }

         //clean database
         dbCleanerService.cleanRepositoryData(repositoryEntry);

         //clean index
         for (WorkspaceEntry wEntry : repositoryEntry.getWorkspaceEntries())
         {
            IndexCleanerService.removeWorkspaceIndex(wEntry,
               repositoryEntry.getSystemWorkspaceName().equals(wEntry.getName()));
         }

         //clean value storage
         for (WorkspaceEntry wEntry : repositoryEntry.getWorkspaceEntries())
         {
            ValueStorageCleanerService.removeWorkspaceValueStorage(wEntry);
         }

         super.restoreRepository();
      }
      catch (Throwable t)
      {
         throw new RepositoryRestoreExeption("Repository " + repositoryEntry.getName() + " was not restored", t);
      }
   }

   /**
    * forceCloseSession. Close sessions on specific workspace.
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
