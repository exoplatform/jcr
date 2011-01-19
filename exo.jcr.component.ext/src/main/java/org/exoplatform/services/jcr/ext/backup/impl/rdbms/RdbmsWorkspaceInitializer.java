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

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.AccessManager;
import org.exoplatform.services.jcr.config.QueryHandlerParams;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.ValueStorageEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.ext.backup.impl.IndexCleanHelper;
import org.exoplatform.services.jcr.ext.backup.impl.ValueStorageCleanHelper;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.BackupWorkspaceInitializer;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.NamespaceRegistryImpl;
import org.exoplatform.services.jcr.impl.core.SessionRegistry;
import org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeManagerImpl;
import org.exoplatform.services.jcr.impl.core.query.SystemSearchManager;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.exoplatform.services.jcr.impl.dataflow.persistent.CacheableWorkspaceDataManager;
import org.exoplatform.services.jcr.impl.storage.jdbc.backup.Backupable;
import org.exoplatform.services.jcr.impl.storage.jdbc.backup.CleanException;
import org.exoplatform.services.jcr.impl.storage.jdbc.backup.RestoreException;
import org.exoplatform.services.jcr.impl.storage.value.fs.FileValueStorage;
import org.exoplatform.services.jcr.impl.util.io.FileCleanerHolder;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipInputStream;

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
   protected static final Log log = ExoLogger.getLogger("exo.jcr.component.core.RdbmsWorkspaceInitializer");

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
            AccessManager accessManager, RepositoryService repositoryService, FileCleanerHolder cleanerHolder)
            throws RepositoryConfigurationException,
      PathNotFoundException, RepositoryException
   {
      super(config, repConfig, dataManager, namespaceRegistry, locationFactory, nodeTypeManager, valueFactory,
               accessManager, cleanerHolder);

      this.repositoryService = repositoryService;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public NodeData initWorkspace() throws RepositoryException
   {
      if (isWorkspaceInitialized())
      {
         return (NodeData)dataManager.getItemData(Constants.ROOT_UUID);
      }

      long start = System.currentTimeMillis();

      try
      {
         fullRdbmsRestore();
      }
      catch (Throwable e)
      {
         try
         {
            rollback();
         }
         catch (RepositoryConfigurationException e1)
         {
            log.error("Can't rollback changes", e1);
         }
         catch (CleanException e1)
         {
            log.error("Can't rollback changes", e1);
         }
         catch (IOException e1)
         {
            log.error("Can't rollback changes", e1);
         }
         throw new RepositoryException(e);
      }

      final NodeData root = (NodeData)dataManager.getItemData(Constants.ROOT_UUID);

      log.info("Workspace [" + workspaceName + "] restored from storage " + restorePath + " in "
         + (System.currentTimeMillis() - start) * 1d / 1000 + "sec");

      return root;
   }

   /**
    * Restore from full rdbms backup.
    */
   protected void fullRdbmsRestore() throws RepositoryException
   {
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
      
      try
      {
         // restore all components
         for (Backupable component : backupableComponents)
         {
            component.restore(new File(restorePath));
         }

         restoreValueStorage();
         restoreIndex();
      }
      catch (RestoreException e)
      {
         throw new RepositoryException(e);
      }
      catch (RepositoryConfigurationException e)
      {
         throw new RepositoryException(e);
      }
      catch (IOException e)
      {
         throw new RepositoryException(e);
      }
   }

   /**
    * Restore index from backup.
    */
   protected void restoreIndex() throws RepositoryConfigurationException, IOException
   {
      File indexDir = new File(restorePath, FullBackupJob.INDEX_DIR);
      File systemIndexDir = new File(restorePath, FullBackupJob.SYSTEM_INDEX_DIR);

      if (workspaceEntry.getQueryHandler() != null)
      {
         if (!PrivilegedFileHelper.exists(indexDir))
         {
            throw new RepositoryConfigurationException("Can't restore index. Directory " + indexDir.getName()
               + " doesn't exists");
         }
         else
         {
            File destDir =
               new File(workspaceEntry.getQueryHandler().getParameterValue(QueryHandlerParams.PARAM_INDEX_DIR));
            copyDirectory(indexDir, destDir);
         }

         // try to restore system index
         if (repositoryEntry.getSystemWorkspaceName().equals(workspaceName))
         {
            if (!PrivilegedFileHelper.exists(systemIndexDir))
            {
               throw new RepositoryConfigurationException("Can't restore system index. Directory "
                  + systemIndexDir.getName() + " doesn't exists");
            }
            else
            {
               File destDir =
                  new File(workspaceEntry.getQueryHandler().getParameterValue(QueryHandlerParams.PARAM_INDEX_DIR) + "_"
                     + SystemSearchManager.INDEX_DIR_SUFFIX);
               copyDirectory(systemIndexDir, destDir);
            }
         }
         else if (PrivilegedFileHelper.exists(systemIndexDir))
         {
            throw new RepositoryConfigurationException("Workspace [" + workspaceName
               + "] is not a system in repository configuration but system index backup files exist");
         }
      }
      else
      {
         if (PrivilegedFileHelper.exists(indexDir) || PrivilegedFileHelper.exists(systemIndexDir))
         {
            throw new RepositoryConfigurationException("Query handler didn't configure in workspace [" + workspaceName
               + "] configuration but index backup files exist");
         }
      }
   }

   /**
    * Rollback changes due to errors.
    * 
    * @throws RepositoryConfigurationException 
    * @throws RepositoryException 
    * @throws CleanException 
    * @throws IOException 
    */
   protected void rollback() throws RepositoryException, RepositoryConfigurationException, CleanException,
      IOException
   {
      boolean isSystem =
         repositoryService.getRepository(repositoryEntry.getName()).getConfiguration().getSystemWorkspaceName()
            .equals(workspaceEntry.getName());

      //close all session
      forceCloseSession(repositoryEntry.getName(), workspaceEntry.getName());

      List<Backupable> backupable =
         repositoryService.getRepository(repositoryEntry.getName()).getWorkspaceContainer(workspaceEntry.getName())
            .getComponentInstancesOfType(Backupable.class);

      //clean database
      for (Backupable component : backupable)
      {
         component.getDataCleaner().clean();
      }

      //clean index
      new IndexCleanHelper().removeWorkspaceIndex(workspaceEntry, isSystem);

      //clean value storage
      new ValueStorageCleanHelper().removeWorkspaceValueStorage(workspaceEntry);
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

   /**
    * Restoring value storage from backup.
    */
   protected void restoreValueStorage() throws RepositoryConfigurationException, IOException
   {
      File backupValueStorageDir = new File(restorePath, FullBackupJob.VALUE_STORAGE_DIR);
      if (workspaceEntry.getContainer().getValueStorages() != null)
      {
         List<ValueStorageEntry> valueStorages = workspaceEntry.getContainer().getValueStorages();
         String[] valueStoragesFiles = PrivilegedFileHelper.list(backupValueStorageDir);

         if ((valueStoragesFiles == null && valueStorages.size() != 0)
            || (valueStoragesFiles != null && valueStoragesFiles.length != valueStorages.size()))
         {
            throw new RepositoryConfigurationException("Workspace configuration [" + workspaceName
               + "] has a different amount of value storages than exist in backup");
         }

         for (ValueStorageEntry valueStorage : valueStorages)
         {
            File srcDir = new File(backupValueStorageDir, valueStorage.getId());
            if (!PrivilegedFileHelper.exists(srcDir))
            {
               throw new RepositoryConfigurationException("Can't restore value storage. Directory " + srcDir.getName()
                  + " doesn't exists");
            }
            else
            {
               File destDir = new File(valueStorage.getParameterValue(FileValueStorage.PATH));

               copyDirectory(srcDir, destDir);
            }
         }
      }
      else
      {
         if (PrivilegedFileHelper.exists(backupValueStorageDir))
         {
            throw new RepositoryConfigurationException("Value storage didn't configure in workspace [" + workspaceName
               + "] configuration but value storage backup files exist");
         }
      }
   }

   /**
    * Copy directory.
    * 
    * @param srcPath
    *          source path
    * @param dstPath
    *          destination path
    * @throws IOException
    *          if any exception occurred
    */
   private void copyDirectory(File srcPath, File dstPath) throws IOException
   {
      if (PrivilegedFileHelper.isDirectory(srcPath))
      {
         if (!PrivilegedFileHelper.exists(dstPath))
         {
            PrivilegedFileHelper.mkdirs(dstPath);
         }

         String files[] = PrivilegedFileHelper.list(srcPath);
         for (int i = 0; i < files.length; i++)
         {
            copyDirectory(new File(srcPath, files[i]), new File(dstPath, files[i]));
         }
      }
      else
      {
         ZipInputStream in = null;
         OutputStream out = null;

         try
         {
            in = PrivilegedFileHelper.zipInputStream(srcPath);
            in.getNextEntry();
            out = PrivilegedFileHelper.fileOutputStream(dstPath);

            // Transfer bytes from in to out
            byte[] buf = new byte[2048];

            int len;

            while ((len = in.read(buf)) > 0)
            {
               out.write(buf, 0, len);
            }
         }
         finally
         {
            if (in != null)
            {
               in.close();
            }

            if (out != null)
            {
               out.close();
            }
         }
      }
   }
}
