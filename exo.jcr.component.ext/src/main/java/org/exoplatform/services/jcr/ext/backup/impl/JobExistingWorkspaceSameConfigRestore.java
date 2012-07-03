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
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.dataflow.DataManager;
import org.exoplatform.services.jcr.ext.backup.BackupChainLog;
import org.exoplatform.services.jcr.ext.backup.BackupManager;
import org.exoplatform.services.jcr.ext.backup.WorkspaceRestoreException;
import org.exoplatform.services.jcr.impl.backup.BackupException;
import org.exoplatform.services.jcr.impl.backup.Backupable;
import org.exoplatform.services.jcr.impl.backup.DataRestore;
import org.exoplatform.services.jcr.impl.backup.JCRRestore;
import org.exoplatform.services.jcr.impl.backup.rdbms.DataRestoreContext;
import org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspacePersistentDataManager;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.jcr.impl.util.io.FileCleanerHolder;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 *
 * Date: 24 01 2011
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: JobExistingWorkspaceSameConfigRestore.java 34360 2010-11-11 11:11:11Z tolusha $
 */
public class JobExistingWorkspaceSameConfigRestore extends JobWorkspaceRestore
{
   /**
    * The logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.ext.JobExistingWorkspaceSameConfigRestore");

   /**
    * JobExistingWorkspaceSameConfigRestore constructor.
    */
   public JobExistingWorkspaceSameConfigRestore(RepositoryService repositoryService, BackupManager backupManager,
      String repositoryName, File logFile, WorkspaceEntry wEntry)
   {
      super(repositoryService, backupManager, repositoryName, logFile, wEntry);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void restoreWorkspace() throws WorkspaceRestoreException
   {
      // list of data restorers
      List<DataRestore> dataRestorer = new ArrayList<DataRestore>();

      List<WorkspaceContainerFacade> workspacesWaits4Resume = new ArrayList<WorkspaceContainerFacade>();
      try
      {
         ManageableRepository repository = repositoryService.getRepository(repositoryName);

         if (!JDBCWorkspaceDataContainer.getDatabaseType(wEntry).isMultiDatabase())
         {
            for (String wsName : repository.getWorkspaceNames())
            {
               WorkspaceContainerFacade wsContainer = repository.getWorkspaceContainer(wsName);
               wsContainer.setState(ManageableRepository.SUSPENDED);

               workspacesWaits4Resume.add(wsContainer);
            }
         }
         else
         {
            WorkspaceContainerFacade wsContainer = repository.getWorkspaceContainer(wEntry.getName());

            wsContainer.setState(ManageableRepository.SUSPENDED);
            workspacesWaits4Resume.add(wsContainer);
         }

         // get all restorers
         List<Backupable> backupable =
                  repository.getWorkspaceContainer(wEntry.getName())
               .getComponentInstancesOfType(Backupable.class);

         BackupChainLog backupChainLog = new BackupChainLog(backupChainLogFile);
         File storageDir = backupChainLog.getBackupConfig().getBackupDir();
         File fullBackupDir = JCRRestore.getFullBackupFile(storageDir);

         DataRestoreContext context = new DataRestoreContext(
                  new String[] {DataRestoreContext.STORAGE_DIR}, 
                  new Object[] {fullBackupDir});

         for (Backupable component : backupable)
         {
            dataRestorer.add(component.getDataRestorer(context));
         }

         for (DataRestore restorer : dataRestorer)
         {
            restorer.clean();
         }

         for (DataRestore restorer : dataRestorer)
         {
            restorer.restore();
         }
         
         for (DataRestore restorer : dataRestorer)
         {
            restorer.commit();
         }

         for (WorkspaceContainerFacade wsContainer : workspacesWaits4Resume)
         {
            wsContainer.setState(ManageableRepository.ONLINE);
         }

         // incremental restore
         DataManager dataManager =
                  (WorkspacePersistentDataManager) repository.getWorkspaceContainer(wEntry.getName()).getComponent(
                           WorkspacePersistentDataManager.class);

         FileCleanerHolder cleanerHolder =
            (FileCleanerHolder)repository.getWorkspaceContainer(wEntry.getName()).getComponent(FileCleanerHolder.class);

         JCRRestore restorer = new JCRRestore(dataManager, cleanerHolder.getFileCleaner());
         for (File incrBackupFile : JCRRestore.getIncrementalFiles(storageDir))
         {
            restorer.incrementalRestore(incrBackupFile);
         }
      }
      catch (Throwable t) //NOSONAR
      {
         // rollback
         for (DataRestore restorer : dataRestorer)
         {
            try
            {
               restorer.rollback();
            }
            catch (BackupException e)
            {
               LOG.error("Can't rollback changes", e);
            }
         }

         throw new WorkspaceRestoreException("Workspace " + wEntry.getName() + " was not restored", t);
      }
      finally
      {
         // close
         for (DataRestore restorer : dataRestorer)
         {
            try
            {
               restorer.close();
            }
            catch (BackupException e)
            {
               LOG.error("Can't close restorer", e);
            }
         }

         try
         {
            for (WorkspaceContainerFacade wsContainer : workspacesWaits4Resume)
            {
               wsContainer.setState(ManageableRepository.ONLINE);
            }
         }
         catch (RepositoryException e)
         {
            LOG.error("Can't resume component", e);
         }
      }
   }
}
