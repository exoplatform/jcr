/*
 * Copyright (C) 2012 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.quota;

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.commons.utils.PrivilegedSystemHelper;
import org.exoplatform.services.jcr.impl.backup.BackupException;
import org.exoplatform.services.jcr.impl.backup.Backupable;
import org.exoplatform.services.jcr.impl.backup.DataRestore;
import org.exoplatform.services.jcr.impl.backup.rdbms.DBBackup;
import org.exoplatform.services.jcr.impl.backup.rdbms.DataRestoreContext;
import org.exoplatform.services.jcr.impl.dataflow.serialization.ZipObjectReader;
import org.exoplatform.services.jcr.impl.dataflow.serialization.ZipObjectWriter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.io.IOException;

/**
 * {@link DataRestore} implementation for quota.
 *
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: WorkspaceQuotaRestore.java Aug 13, 2012 tolusha $
 */
public class WorkspaceQuotaRestore implements DataRestore
{

   /**
    * Logger.
    */
   protected final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.WorkspaceQuotaRestore");

   /**
    * File name for backuped data.
    */
   protected static final String BACKUP_FILE_NAME = "quota";

   /**
    * File where temporary data are stored will be restored at rollback.
    */
   private final File tempFile;

   /**
    * File where backuped data are stored.
    */
   private final File backupFile;

   /**
    * {@link WorkspaceQuotaManager} instance.
    */
   private final WorkspaceQuotaManager wqm;

   /**
    * Workspace name.
    */
   private final String wsName;

   /**
    * Repository name.
    */
   private final String rName;

   /**
    * {@link QuotaPersister}
    */
   private final QuotaPersister quotaPersister;

   /**
    * WorkspaceQuotaRestore constructor.
    */
   WorkspaceQuotaRestore(WorkspaceQuotaManager wqm, DataRestoreContext context)
   {
      this(wqm, (File)context.getObject(DataRestoreContext.STORAGE_DIR));
   }

   /**
    * WorkspaceQuotaRestore constructor.
    */
   WorkspaceQuotaRestore(WorkspaceQuotaManager wqm, File storageDir)
   {
      this.wqm = wqm;
      this.backupFile = new File(storageDir, BACKUP_FILE_NAME + DBBackup.CONTENT_FILE_SUFFIX);

      File tempDir = new File(PrivilegedSystemHelper.getProperty("java.io.tmpdir"));
      this.tempFile = new File(tempDir, "temp.dump");

      this.wsName = wqm.getContext().wsName;
      this.rName = wqm.getContext().rName;
      this.quotaPersister = wqm.getContext().quotaPersister;
   }

   /**
    * {@inheritDoc}
    */
   public void clean() throws BackupException
   {
      doBackup(tempFile);
      doClean();
   }

   /**
    * {@inheritDoc}
    */
   public void restore() throws BackupException
   {
      doRestore(backupFile);
   }

   /**
    * {@inheritDoc}
    */
   public void commit() throws BackupException
   {
   }

   /**
    * {@inheritDoc}
    */
   public void rollback() throws BackupException
   {
      doClean();
      doRestore(tempFile);
   }

   /**
    * {@inheritDoc}
    */
   public void close() throws BackupException
   {
      PrivilegedFileHelper.delete(tempFile);
   }

   /**
    * {@link Backupable#backup(File)}
    */
   public void backup() throws BackupException
   {
      doBackup(backupFile);
   }

   /**
    * Restores content.
    */
   protected void doRestore(File backupFile) throws BackupException
   {
      if (!PrivilegedFileHelper.exists(backupFile))
      {
         LOG.warn("Nothing to restore for quotas");
         return;
      }

      ZipObjectReader in = null;
      try
      {
         in = new ZipObjectReader(PrivilegedFileHelper.zipInputStream(backupFile));
         quotaPersister.restoreWorkspaceData(rName, wsName, in);
      }
      catch (IOException e)
      {
         throw new BackupException(e);
      }
      finally
      {
         if (in != null)
         {
            try
            {
               in.close();
            }
            catch (IOException e)
            {
               LOG.error("Can't close input stream", e);
            }
         }
      }

      repairDataSize();
   }

   /**
    * After workspace data size being restored, need also to update
    * repository and global data size on respective value.
    */
   private void repairDataSize()
   {
      try
      {
         long dataSize = quotaPersister.getWorkspaceDataSize(rName, wsName);

         ChangesItem changesItem = new ChangesItem();
         changesItem.updateWorkspaceChangedSize(dataSize);

         quotaPersister.setWorkspaceDataSize(rName, wsName, 0); // workaround

         Runnable task = new ApplyPersistedChangesTask(wqm.getContext(), changesItem);
         task.run();
      }
      catch (UnknownDataSizeException e)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace(e.getMessage(), e);
         }
      }
   }

   /**
    * Backups data to define file.
    */
   protected void doBackup(File backupFile) throws BackupException
   {
      ZipObjectWriter out = null;
      try
      {
         out = new ZipObjectWriter(PrivilegedFileHelper.zipOutputStream(backupFile));
         quotaPersister.backupWorkspaceData(rName, wsName, out);
      }
      catch (IOException e)
      {
         throw new BackupException(e);
      }
      finally
      {
         if (out != null)
         {
            try
            {
               out.close();
            }
            catch (IOException e)
            {
               LOG.error("Can't close output stream", e);
            }
         }
      }
   }

   /**
    * Clean workspace data. Also decrease repository and global data size.
    */
   protected void doClean() throws BackupException
   {
      try
      {
         long dataSize = wqm.quotaPersister.getWorkspaceDataSize(rName, wsName);

         ChangesItem changesItem = new ChangesItem();
         changesItem.updateWorkspaceChangedSize(-dataSize);

         Runnable task = new ApplyPersistedChangesTask(wqm.getContext(), changesItem);
         task.run();
      }
      catch (UnknownDataSizeException e)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace(e.getMessage(), e);
         }
      }

      quotaPersister.clearWorkspaceData(wqm.rName, wqm.wsName);
   }
}
