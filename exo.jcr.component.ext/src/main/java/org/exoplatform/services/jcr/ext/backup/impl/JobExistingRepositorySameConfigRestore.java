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

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.dataflow.DataManager;
import org.exoplatform.services.jcr.ext.backup.BackupChainLog;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupChainLog;
import org.exoplatform.services.jcr.ext.backup.RepositoryRestoreExeption;
import org.exoplatform.services.jcr.impl.backup.BackupException;
import org.exoplatform.services.jcr.impl.backup.Backupable;
import org.exoplatform.services.jcr.impl.backup.DataRestore;
import org.exoplatform.services.jcr.impl.backup.JCRRestore;
import org.exoplatform.services.jcr.impl.backup.rdbms.DataRestoreContext;
import org.exoplatform.services.jcr.impl.clean.rdbms.DBCleanService;
import org.exoplatform.services.jcr.impl.clean.rdbms.DBCleanerTool;
import org.exoplatform.services.jcr.impl.clean.rdbms.DummyDBCleanerTool;
import org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspacePersistentDataManager;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig.DatabaseStructureType;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.jcr.impl.util.io.FileCleanerHolder;

import java.io.File;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.sql.DataSource;

/**
 * Created by The eXo Platform SAS.
 *
 * Date: 24 01 2011
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: JobExistingRepositorySameConfigRestore.java 34360 2010-11-11 11:11:11Z tolusha $
 */
public class JobExistingRepositorySameConfigRestore extends JobRepositoryRestore
{

   /**
    * JobExistingRepositorySameConfigRestore constructor.
    */
   public JobExistingRepositorySameConfigRestore(RepositoryService repoService, BackupManagerImpl backupManagerImpl,
      RepositoryEntry repositoryEntry, Map<String, BackupChainLog> workspacesMapping,
      RepositoryBackupChainLog backupChainLog)
   {
      super(repoService, backupManagerImpl, repositoryEntry, workspacesMapping, backupChainLog);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void restoreRepository() throws RepositoryRestoreExeption
   {
      // list of data restorers
      List<DataRestore> dataRestorer = new ArrayList<DataRestore>();

      List<WorkspaceContainerFacade> workspacesWaits4Resume = new ArrayList<WorkspaceContainerFacade>();
      try
      {
         WorkspaceEntry wsEntry = repositoryEntry.getWorkspaceEntries().get(0);

         // define one common connection for all restores and cleaners for single db case
         Connection jdbcConn = null;
         
         // define one common database cleaner for all restores for single db case
         DBCleanerTool dbCleaner = null;
         
         DatabaseStructureType dbType = JDBCWorkspaceDataContainer.getDatabaseType(wsEntry);

         if (dbType.isShareSameDatasource())
         {
            String dsName = wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME);

            final DataSource ds = (DataSource)new InitialContext().lookup(dsName);
            if (ds == null)
            {
               throw new NameNotFoundException("Data source " + dsName + " not found");
            }

            jdbcConn = SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<Connection>()
            {
               public Connection run() throws Exception
               {
                  return ds.getConnection();

               }
            });
            jdbcConn.setAutoCommit(false);

            if (dbType == DatabaseStructureType.SINGLE)
            {
               dbCleaner = DBCleanService.getRepositoryDBCleaner(jdbcConn, repositoryEntry);
            }
         }

         ManageableRepository repository = repositoryService.getRepository(this.repositoryEntry.getName());
         for (String wsName : repository.getWorkspaceNames())
         {
            WorkspaceContainerFacade wsContainer = repository.getWorkspaceContainer(wsName);
            wsContainer.setState(ManageableRepository.SUSPENDED);

            workspacesWaits4Resume.add(wsContainer);
         }

         boolean isSharedDbCleaner = false;

         // collect all restorers
         for (WorkspaceEntry wEntry : repositoryEntry.getWorkspaceEntries())
         {
            // get all backupable components
            List<Backupable> backupable =
               repositoryService.getRepository(this.repositoryEntry.getName()).getWorkspaceContainer(wEntry.getName())
                  .getComponentInstancesOfType(Backupable.class);

            File fullBackupDir =
               JCRRestore.getFullBackupFile(workspacesMapping.get(wEntry.getName()).getBackupConfig().getBackupDir());
            
            DataRestoreContext context;

            if (jdbcConn != null)
            {
               if (dbType == DatabaseStructureType.SINGLE)
               {
                  context = new DataRestoreContext(
                                 new String[]{
                                    DataRestoreContext.STORAGE_DIR, 
                                    DataRestoreContext.DB_CONNECTION, 
                                    DataRestoreContext.DB_CLEANER}, 
                                 new Object[]{
                                    fullBackupDir, 
                                    jdbcConn, 
                                    isSharedDbCleaner ? new DummyDBCleanerTool() : dbCleaner});
   
                  isSharedDbCleaner = true;
               }
               else
               {
                  context = new DataRestoreContext(
                     new String[]{
                        DataRestoreContext.STORAGE_DIR, 
                        DataRestoreContext.DB_CONNECTION}, 
                     new Object[]{
                        fullBackupDir, 
                        jdbcConn});
               }
            }
            else
            {
               context = new DataRestoreContext(
                        new String[] {DataRestoreContext.STORAGE_DIR}, 
                        new Object[] {fullBackupDir});
            }

            for (Backupable component : backupable)
            {
               dataRestorer.add(component.getDataRestorer(context));
            }
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
         
         // resume components
         for (WorkspaceContainerFacade wsContainer : workspacesWaits4Resume)
         {
            wsContainer.setState(ManageableRepository.ONLINE);
         }

         // incremental restore
         for (WorkspaceEntry wEntry : repositoryEntry.getWorkspaceEntries())
         {
            repositoryService.getRepository(this.repositoryEntry.getName()).getWorkspaceContainer(wEntry.getName())
               .getComponentInstancesOfType(Backupable.class);

            DataManager dataManager =
               (WorkspacePersistentDataManager)repositoryService.getRepository(this.repositoryEntry.getName())
                  .getWorkspaceContainer(wEntry.getName()).getComponent(WorkspacePersistentDataManager.class);

            File storageDir =
               JCRRestore.getFullBackupFile(workspacesMapping.get(wEntry.getName()).getBackupConfig().getBackupDir());

            FileCleanerHolder cleanerHolder =
               (FileCleanerHolder)repositoryService.getRepository(this.repositoryEntry.getName())
                  .getWorkspaceContainer(wEntry.getName()).getComponent(FileCleanerHolder.class);

            JCRRestore restorer = new JCRRestore(dataManager, cleanerHolder.getFileCleaner());
            for (File incrBackupFile : JCRRestore.getIncrementalFiles(storageDir))
            {
               restorer.incrementalRestore(incrBackupFile);
            }
         }
      }
      catch (Throwable t)
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

         throw new RepositoryRestoreExeption("Repository " + repositoryEntry.getName() + " was not restored", t);
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
            LOG.error("Can't resume repository", e);
         }
      }
   }
}
