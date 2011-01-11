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
import org.exoplatform.services.jcr.config.QueryHandlerParams;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.ValueStorageEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.backup.BackupConfig;
import org.exoplatform.services.jcr.ext.backup.BackupOperationException;
import org.exoplatform.services.jcr.ext.backup.impl.AbstractFullBackupJob;
import org.exoplatform.services.jcr.ext.backup.impl.FileNameProducer;
import org.exoplatform.services.jcr.impl.core.query.SystemSearchManager;
import org.exoplatform.services.jcr.impl.storage.jdbc.backup.BackupException;
import org.exoplatform.services.jcr.impl.storage.jdbc.backup.Backupable;
import org.exoplatform.services.jcr.impl.storage.jdbc.backup.ResumeException;
import org.exoplatform.services.jcr.impl.storage.jdbc.backup.SuspendException;
import org.exoplatform.services.jcr.impl.storage.jdbc.backup.Suspendable;
import org.exoplatform.services.jcr.impl.storage.value.fs.FileValueStorage;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by The eXo Platform SARL Author : Alex Reshetnyak alex.reshetnyak@exoplatform.com.ua Nov
 * 21, 2007
 */
public class FullBackupJob extends AbstractFullBackupJob
{
   /**
    * Index directory in full backup storage.
    */
   public static final String INDEX_DIR = "index";

   /**
    * System index directory in full backup storage.
    */
   public static final String SYSTEM_INDEX_DIR = INDEX_DIR + "_" + SystemSearchManager.INDEX_DIR_SUFFIX;

   /**
    * Value storage directory in full backup storage.
    */
   public static final String VALUE_STORAGE_DIR = "values";

   /**
    * Logger.
    */
   protected static Log log = ExoLogger.getLogger("exo.jcr.component.ext.FullBackupJob");

   /**
    * {@inheritDoc}
    */
   @Override
   protected URL createStorage() throws FileNotFoundException, IOException
   {
      FileNameProducer fnp =
         new FileNameProducer(config.getRepository(), config.getWorkspace(),
            PrivilegedFileHelper.getAbsolutePath(config.getBackupDir()), super.timeStamp, true, true);

      return new URL("file:" + PrivilegedFileHelper.getAbsolutePath(fnp.getNextFile()));
   }

   /**
    * {@inheritDoc}
    */
   public void init(ManageableRepository repository, String workspaceName, BackupConfig config, Calendar timeStamp)
   {
      this.repository = repository;
      this.workspaceName = workspaceName;
      this.config = config;
      this.timeStamp = timeStamp;

      try
      {
         url = createStorage();
      }
      catch (FileNotFoundException e)
      {
         log.error("Full backup initialization failed ", e);
         notifyError("Full backup initialization failed ", e);
      }
      catch (IOException e)
      {
         log.error("Full backup initialization failed ", e);
         notifyError("Full backup initialization failed ", e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void run()
   {
      notifyListeners();

      List<Backupable> backupableComponents =
         repository.getWorkspaceContainer(workspaceName).getComponentInstancesOfType(Backupable.class);

      List<Suspendable> suspendableComponents =
         repository.getWorkspaceContainer(workspaceName).getComponentInstancesOfType(Suspendable.class);

      try
      {
         WorkspaceEntry workspaceEntry = null;
         for (WorkspaceEntry entry : repository.getConfiguration().getWorkspaceEntries())
         {
            if (entry.getName().equals(workspaceName))
            {
               workspaceEntry = entry;
               break;
            }
         }

         if (workspaceEntry == null)
         {
            throw new RepositoryConfigurationException("Workpace name " + workspaceName
               + " not found in repository configuration");
         }

         // suspend all components
         for (Suspendable component : suspendableComponents)
         {
            component.suspend();
         }

         // backup all components
         for (Backupable component : backupableComponents)
         {
            component.backup(new File(getStorageURL().getFile()));
         }

         backupValueStorage(workspaceEntry);
         backupIndex(workspaceEntry);
      }
      catch (RepositoryConfigurationException e)
      {
         log.error("Full backup failed " + getStorageURL().getPath(), e);
         notifyError("Full backup failed", e);
      }
      catch (SuspendException e)
      {
         log.error("Full backup failed " + getStorageURL().getPath(), e);
         notifyError("Full backup failed", e);
      }
      catch (BackupException e)
      {
         log.error("Full backup failed " + getStorageURL().getPath(), e);
         notifyError("Full backup failed", e);
      }
      catch (BackupOperationException e)
      {
         log.error("Full backup failed " + getStorageURL().getPath(), e);
         notifyError("Full backup failed", e);
      }
      catch (IOException e)
      {
         log.error("Full backup failed " + getStorageURL().getPath(), e);
         notifyError("Full backup failed", e);
      }
      finally
      {
         for (Suspendable component : suspendableComponents)
         {
            try
            {
               component.resume();
            }
            catch (ResumeException e)
            {
               log.error("Full backup failed " + getStorageURL().getPath(), e);
               notifyError("Full backup failed", e);
            }
         }
      }

      state = FINISHED;
      notifyListeners();
   }

   /**
    * Backup index files.
    * 
    * @param workspaceEntry
    * @throws RepositoryConfigurationException
    * @throws BackupOperationException
    * @throws IOException
    */
   protected void backupIndex(WorkspaceEntry workspaceEntry) throws RepositoryConfigurationException,
      BackupOperationException, IOException
   {
      if (workspaceEntry.getQueryHandler() != null)
      {
         File srcDir = new File(workspaceEntry.getQueryHandler().getParameterValue(QueryHandlerParams.PARAM_INDEX_DIR));
         if (!PrivilegedFileHelper.exists(srcDir))
         {
            throw new BackupOperationException("Can't backup index. Directory " + srcDir.getName() + " doesn't exists");
         }
         else
         {
            File destDir = new File(getStorageURL().getFile(), INDEX_DIR);
            copyDirectory(srcDir, destDir);
         }

         if (repository.getConfiguration().getSystemWorkspaceName().equals(workspaceName))
         {
            srcDir =
               new File(PrivilegedFileHelper.getCanonicalPath(srcDir) + "_" + SystemSearchManager.INDEX_DIR_SUFFIX);
            if (!PrivilegedFileHelper.exists(srcDir))
            {
               throw new BackupOperationException("Can't backup system index. Directory " + srcDir.getName()
                  + " doesn't exists");
            }
            else
            {
               File destDir = new File(getStorageURL().getFile(), SYSTEM_INDEX_DIR);
               copyDirectory(srcDir, destDir);
            }
         }
      }
   }

   /**
    * Backup value storage files.
    * 
    * @param workspaceEntry
    * @throws RepositoryConfigurationException
    * @throws BackupOperationException
    * @throws IOException
    */
   protected void backupValueStorage(WorkspaceEntry workspaceEntry) throws RepositoryConfigurationException,
      BackupOperationException, IOException
   {
      if (workspaceEntry.getContainer().getValueStorages() != null)
      {
         for (ValueStorageEntry valueStorage : workspaceEntry.getContainer().getValueStorages())
         {
            File srcDir = new File(valueStorage.getParameterValue(FileValueStorage.PATH));
            if (!PrivilegedFileHelper.exists(srcDir))
            {
               throw new BackupOperationException("Can't backup value storage. Directory " + srcDir.getName()
                  + " doesn't exists");
            }
            else
            {
               File destValuesDir = new File(getStorageURL().getFile(), VALUE_STORAGE_DIR);
               File destDir = new File(destValuesDir, valueStorage.getId());

               copyDirectory(srcDir, destDir);
            }
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void stop()
   {
      log.info("Stop requested " + getStorageURL().getPath());
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
         InputStream in = null;
         ZipOutputStream out = null;

         try
         {
            in = PrivilegedFileHelper.fileInputStream(srcPath);
            out = PrivilegedFileHelper.zipOutputStream(dstPath);
            out.putNextEntry(new ZipEntry(srcPath.getName()));

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
               out.flush();
               out.closeEntry();
               out.close();
            }
         }
      }
   }
}
