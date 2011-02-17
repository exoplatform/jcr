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
import org.exoplatform.services.jcr.dataflow.DataManager;
import org.exoplatform.services.jcr.ext.backup.BackupChainLog;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupChainLog;
import org.exoplatform.services.jcr.ext.backup.RepositoryRestoreExeption;
import org.exoplatform.services.jcr.impl.backup.BackupException;
import org.exoplatform.services.jcr.impl.backup.Backupable;
import org.exoplatform.services.jcr.impl.backup.DataRestor;
import org.exoplatform.services.jcr.impl.backup.JCRRestor;
import org.exoplatform.services.jcr.impl.backup.JdbcBackupable;
import org.exoplatform.services.jcr.impl.backup.ResumeException;
import org.exoplatform.services.jcr.impl.backup.Suspendable;
import org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspacePersistentDataManager;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.jcr.impl.util.io.FileCleanerHolder;

import java.io.File;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.sql.DataSource;

/**
 * Created by The eXo Platform SAS.
 *
 * Date: 24 01 2011
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: JobExistedRepositoryRestoreSameConfig.java 34360 2010-11-11 11:11:11Z tolusha $
 */
public class JobExistedRepositoryRestoreSameConfig extends JobRepositoryRestore
{

   /**
    * JobExistedRepositoryRestoreSameConfig constructor.
    */
   public JobExistedRepositoryRestoreSameConfig(RepositoryService repoService, BackupManagerImpl backupManagerImpl,
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
      List<DataRestor> dataRestorer = new ArrayList<DataRestor>();

      // the list of components to resume
      List<Suspendable> resumeComponents = new ArrayList<Suspendable>();

      try
      {
         WorkspaceEntry wsEntry = repositoryEntry.getWorkspaceEntries().get(0);

         // define one common connection for all restores and cleaners for single db case
         Connection jdbcConn = null;
         if (!Boolean.parseBoolean(wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.MULTIDB)))
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
         }

         // suspend all components
         for (WorkspaceEntry wEntry : repositoryEntry.getWorkspaceEntries())
         {
            List<Suspendable> suspendableComponents =
               repositoryService.getRepository(this.repositoryEntry.getName()).getWorkspaceContainer(wEntry.getName())
                  .getComponentInstancesOfType(Suspendable.class);

            for (Suspendable component : suspendableComponents)
            {
               component.suspend(Suspendable.SUSPEND_COMPONENT_ON_ALL_NODES);
               resumeComponents.add(component);
            }
         }

         // collect all restorers
         for (WorkspaceEntry wEntry : repositoryEntry.getWorkspaceEntries())
         {
            // get all backupable components
            List<Backupable> backupable =
               repositoryService.getRepository(this.repositoryEntry.getName()).getWorkspaceContainer(wEntry.getName())
                  .getComponentInstancesOfType(Backupable.class);

            File fullBackupDir =
               JCRRestor.getFullBackupFile(workspacesMapping.get(wEntry.getName()).getBackupConfig().getBackupDir());

            for (Backupable component : backupable)
            {
               if (component instanceof JdbcBackupable && jdbcConn != null)
               {
                  dataRestorer.add(((JdbcBackupable)component).getDataRestorer(fullBackupDir, jdbcConn));
               }
               else
               {
                  dataRestorer.add(component.getDataRestorer(fullBackupDir));
               }
            }
         }

         for (DataRestor restorer : dataRestorer)
         {
            restorer.clean();
         }

         for (DataRestor restorer : dataRestorer)
         {
            restorer.restore();
         }

         for (DataRestor restorer : dataRestorer)
         {
            restorer.commit();
         }

         // resume components
         for (int i = 0; i < resumeComponents.size() - 1; i++)
         {
            try
            {
               resumeComponents.remove(i).resume();
            }
            catch (ResumeException e)
            {
               log.error("Can't resume component", e);
            }
         }

         // incremental restore
         for (WorkspaceEntry wEntry : repositoryEntry.getWorkspaceEntries())
         {
            repositoryService.getRepository(this.repositoryEntry.getName()).getWorkspaceContainer(wEntry.getName())
               .getComponentInstancesOfType(Backupable.class);

            DataManager dataManager =
               (WorkspacePersistentDataManager)repositoryService.getRepository(this.repositoryEntry.getName())
                  .getWorkspaceContainer(wEntry.getName()).getComponent(WorkspacePersistentDataManager.class);

            FileCleanerHolder fileCleanHolder =
               (FileCleanerHolder)repositoryService.getRepository(this.repositoryEntry.getName())
                  .getWorkspaceContainer(wEntry.getName()).getComponent(FileCleanerHolder.class);

            File storageDir =
               JCRRestor.getFullBackupFile(workspacesMapping.get(wEntry.getName()).getBackupConfig().getBackupDir());

            JCRRestor restorer = new JCRRestor(dataManager, fileCleanHolder.getFileCleaner());
            for (File incrBackupFile : JCRRestor.getIncrementalFiles(storageDir))
            {
               restorer.incrementalRestore(incrBackupFile);
            }
         }
      }
      catch (Throwable t)
      {
         // rollback
         for (DataRestor restorer : dataRestorer)
         {
            try
            {
               restorer.rollback();
            }
            catch (BackupException e)
            {
               log.error("Can't rollback changes", e);
            }
         }

         throw new RepositoryRestoreExeption("Repository " + repositoryEntry.getName() + " was not restored", t);
      }
      finally
      {
         // close
         for (DataRestor restorer : dataRestorer)
         {
            try
            {
               restorer.close();
            }
            catch (BackupException e)
            {
               log.error("Can't close restorer", e);
            }
         }

         for (Suspendable component : resumeComponents)
         {
            try
            {
               component.resume();
            }
            catch (ResumeException e)
            {
               log.error("Can't resume component " + component.getClass(), e);
            }
         }
      }
   }
}
