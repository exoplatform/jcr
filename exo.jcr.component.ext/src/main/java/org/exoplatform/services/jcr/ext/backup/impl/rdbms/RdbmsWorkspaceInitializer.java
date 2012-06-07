/*
 * Copyright (C) 2009 eXo Platform SAS.
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
package org.exoplatform.services.jcr.ext.backup.impl.rdbms;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.AccessManager;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.impl.backup.BackupException;
import org.exoplatform.services.jcr.impl.backup.Backupable;
import org.exoplatform.services.jcr.impl.backup.DataRestore;
import org.exoplatform.services.jcr.impl.backup.rdbms.DataRestoreContext;
import org.exoplatform.services.jcr.impl.core.BackupWorkspaceInitializer;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.NamespaceRegistryImpl;
import org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeManagerImpl;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.exoplatform.services.jcr.impl.dataflow.persistent.CacheableWorkspaceDataManager;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:anatoliy.bazko@gmail.com">Anatoliy Bazko</a>
 * @version $Id: RdbmsWorkspaceInitializer.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class RdbmsWorkspaceInitializer extends BackupWorkspaceInitializer
{
   /**
    * Logger.
    */
   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.RdbmsWorkspaceInitializer");

   /**
    * The repository service.
    */
   protected final RepositoryService repositoryService;

   /**
    * Constructor RdbmsWorkspaceInitializer.
    */
   public RdbmsWorkspaceInitializer(WorkspaceEntry config, RepositoryEntry repConfig,
      CacheableWorkspaceDataManager dataManager, NamespaceRegistryImpl namespaceRegistry,
      LocationFactory locationFactory, NodeTypeManagerImpl nodeTypeManager, ValueFactoryImpl valueFactory,
      AccessManager accessManager, RepositoryService repositoryService) throws RepositoryConfigurationException,
      PathNotFoundException, RepositoryException
   {
      super(config, repConfig, dataManager, namespaceRegistry, locationFactory, nodeTypeManager, valueFactory,
         accessManager);

      this.repositoryService = repositoryService;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void doRestore() throws Throwable
   {
      List<DataRestore> dataRestorers = new ArrayList<DataRestore>();

      ManageableRepository repository = null;
      try
      {
         repository = repositoryService.getRepository(repositoryEntry.getName());
      }
      catch (RepositoryConfigurationException e)
      {
         throw new RepositoryException(e);
      }

      List<Backupable> backupableComponents =
         repository.getWorkspaceContainer(workspaceName).getComponentInstancesOfType(Backupable.class);
      
      List<WorkspaceContainerFacade> workspacesWaits4Resume = new ArrayList<WorkspaceContainerFacade>();
      
      Throwable throwable = null;
      try
      {
         // set state SUSPENDED to other workspaces if singledb
         if (!JDBCWorkspaceDataContainer.getDatabaseType(workspaceEntry).isMultiDatabase())
         {
            for (WorkspaceEntry we : repositoryEntry.getWorkspaceEntries())
            {
               if (!we.getName().equals(workspaceEntry.getName()))
               {
                  WorkspaceContainerFacade wsContainer = repository.getWorkspaceContainer(we.getName());
                  wsContainer.setState(ManageableRepository.SUSPENDED);

                  workspacesWaits4Resume.add(wsContainer);
               }
            }
         }

         DataRestoreContext context = new DataRestoreContext(
                  new String[] {DataRestoreContext.STORAGE_DIR}, 
                  new Object[] {new File(restorePath)});

         // restore all components
         for (Backupable component : backupableComponents)
         {
            dataRestorers.add(component.getDataRestorer(context));
         }

         for (DataRestore restorer : dataRestorers)
         {
            restorer.clean();
         }

         for (DataRestore restorer : dataRestorers)
         {
            restorer.restore();
         }

         for (DataRestore restorer : dataRestorers)
         {
            restorer.commit();
         }
      }
      catch (Throwable e)
      {
         throwable = e;

         for (DataRestore restorer : dataRestorers)
         {
            try
            {
               restorer.rollback();
            }
            catch (BackupException e1)
            {
               LOG.error("Can't rollback restorer", e1);
            }
         }

         throw e;
      }
      finally
      {
         for (DataRestore restorer : dataRestorers)
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
            if (throwable == null)
            {
               LOG.error("Con not set ONLYNE state for repository " + repository.getConfiguration().getName(), e);
            }
            else
            {
               throw new RepositoryException("Con not set ONLINE state for repository "
                  + repository.getConfiguration().getName(), e);
            }
         }
      }
   }
}
