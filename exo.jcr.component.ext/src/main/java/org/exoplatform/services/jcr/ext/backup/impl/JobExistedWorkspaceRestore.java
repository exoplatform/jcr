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
import org.exoplatform.services.jcr.impl.core.SessionRegistry;
import org.exoplatform.services.jcr.impl.util.jdbc.cleaner.DBCleanerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 2010
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id: JobExistedWorkspaceRestore.java 3709 2010-12-22 11:56:25Z tolusha $
 */
public class JobExistedWorkspaceRestore extends JobWorkspaceRestore
{
   /**
    * The logger.
    */
   private static Log log = ExoLogger.getLogger("exo.jcr.component.ext.JobExistedWorkspaceRestore");

   /**
    * Database cleaner.
    */
   private final DBCleanerService dbCleanerService;

   /**
    * Value storage cleaner.
    */
   private final ValueStorageCleanHelper valueStorageCleaner;

   /**
    * Index cleaner.
    */
   private final IndexCleanHelper indexCleanHelper;

   /**
    * JobExistedWorkspaceRestore constructor.
    */
   public JobExistedWorkspaceRestore(RepositoryService repositoryService, BackupManager backupManager,
      String repositoryName, BackupChainLog log, WorkspaceEntry wEntry)
   {
      super(repositoryService, backupManager, repositoryName, log, wEntry);

      this.dbCleanerService = new DBCleanerService();
      this.valueStorageCleaner = new ValueStorageCleanHelper();
      this.indexCleanHelper = new IndexCleanHelper();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void restore() throws WorkspaceRestoreException
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

         boolean isSystem =
            repositoryService.getRepository(repositoryName).getConfiguration().getSystemWorkspaceName()
               .equals(wEntry.getName());

         //close all session
         forceCloseSession(repositoryName, wEntry.getName());

         repositoryService.getRepository(repositoryName).removeWorkspace(wEntry.getName());

         //clean database
         dbCleanerService.cleanWorkspaceData(wEntry);

         //clean index
         indexCleanHelper.removeWorkspaceIndex(wEntry, isSystem);

         //clean value storage
         valueStorageCleaner.removeWorkspaceValueStorage(wEntry);

         super.restore();
      }
      catch (Throwable t)
      {
         throw new WorkspaceRestoreException("Workspace " + wEntry.getName() + " was not restored", t);
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
