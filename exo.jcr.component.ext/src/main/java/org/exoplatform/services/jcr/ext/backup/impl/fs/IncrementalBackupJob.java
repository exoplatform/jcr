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
package org.exoplatform.services.jcr.ext.backup.impl.fs;

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.dataflow.ChangesLogIterator;
import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.ext.backup.BackupConfig;
import org.exoplatform.services.jcr.ext.backup.impl.AbstractIncrementalBackupJob;
import org.exoplatform.services.jcr.ext.backup.impl.FileNameProducer;
import org.exoplatform.services.jcr.ext.backup.impl.FixupStream;
import org.exoplatform.services.jcr.ext.backup.impl.PendingChangesLog;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.impl.util.io.FileCleanerHolder;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.List;

/**
 * Created by The eXo Platform SARL Author : Alex Reshetnyak alex.reshetnyak@exoplatform.com.ua Nov
 * 20, 2007
 */
public class IncrementalBackupJob extends AbstractIncrementalBackupJob
{

   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.ext.IncrementalBackupJob");

   private ObjectOutputStream oosFileData;

   private FileCleaner fileCleaner = FileCleanerHolder.getFileCleaner();

   public IncrementalBackupJob()
   {
   }

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
         LOG.error("Incremental backup initialization failed ", e);
         notifyError("Incremental backup initialization failed ", e);
      }
      catch (IOException e)
      {
         LOG.error("Incremental backup initialization failed ", e);
         notifyError("Incremental backup initialization failed ", e);
      }
   }

   public void stop()
   {
      state = FINISHED;
      LOG.info("Stop requested " + getStorageURL().getPath());

      notifyListeners();
   }

   @Override
   protected URL createStorage() throws FileNotFoundException, IOException
   {
      FileNameProducer fnp =
         new FileNameProducer(config.getRepository(), config.getWorkspace(),
            PrivilegedFileHelper.getAbsolutePath(config.getBackupDir()), super.timeStamp, false);

      File backupFileData = fnp.getNextFile();

      oosFileData = new ObjectOutputStream(PrivilegedFileHelper.fileOutputStream(backupFileData));

      return new URL("file:" + backupFileData.getAbsoluteFile());
   }

   @Override
   protected void save(ItemStateChangesLog persistentLog) throws IOException
   {

      TransactionChangesLog changesLog = (TransactionChangesLog)persistentLog;

      if (changesLog != null && !isSessionNull(changesLog))
      {
         long start = System.currentTimeMillis();

         writeExternal(oosFileData, changesLog, fileCleaner);

         long total = System.currentTimeMillis() - start;

         if (LOG.isDebugEnabled())
            LOG.debug("Time : " + total + " ms" + "    Itemstates count : " + changesLog.getAllStates().size());
      }
   }

   public void writeExternal(ObjectOutputStream out, TransactionChangesLog changesLog, FileCleaner fileCleaner)
      throws IOException
   {

      PendingChangesLog pendingChangesLog = new PendingChangesLog(changesLog, fileCleaner);

      synchronized (out)
      {
         if (pendingChangesLog.getConteinerType() == PendingChangesLog.Type.CHANGESLOG_WITH_STREAM)
         {

            out.writeInt(PendingChangesLog.Type.CHANGESLOG_WITH_STREAM);
            out.writeObject(changesLog);

            // Write FixupStream
            List<FixupStream> listfs = pendingChangesLog.getFixupStreams();
            out.writeInt(listfs.size());

            for (int i = 0; i < listfs.size(); i++)
            {
               listfs.get(i).writeExternal(out);
            }

            // write stream data
            List<InputStream> listInputList = pendingChangesLog.getInputStreams();

            // write file count
            out.writeInt(listInputList.size());

            for (int i = 0; i < listInputList.size(); i++)
            {
               File tempFile = getAsFile(listInputList.get(i));
               FileInputStream fis = PrivilegedFileHelper.fileInputStream(tempFile);

               // write file size
               out.writeLong(PrivilegedFileHelper.length(tempFile));

               // write file content
               writeContent(fis, out);

               fis.close();
               fileCleaner.addFile(tempFile);
            }
         }
         else
         {
            out.writeInt(PendingChangesLog.Type.CHANGESLOG_WITHOUT_STREAM);
            out.writeObject(changesLog);
         }

         out.flush();
      }
   }

   private File getAsFile(InputStream is) throws IOException
   {
      byte[] buf = new byte[1024 * 20];

      File tempFile = PrivilegedFileHelper.createTempFile("" + System.currentTimeMillis(), "" + System.nanoTime());
      FileOutputStream fos = PrivilegedFileHelper.fileOutputStream(tempFile);
      int len;

      while ((len = is.read(buf)) > 0)
         fos.write(buf, 0, len);

      fos.flush();
      fos.close();

      return tempFile;
   }

   private void writeContent(InputStream is, ObjectOutputStream oos) throws IOException
   {
      byte[] buf = new byte[1024 * 8];
      int len;

      int size = 0;

      while ((len = is.read(buf)) > 0)
      {
         oos.write(buf, 0, len);
         size += len;
      }

      oos.flush();
   }

   private boolean isSessionNull(TransactionChangesLog changesLog)
   {
      boolean isSessionNull = false;

      ChangesLogIterator logIterator = changesLog.getLogIterator();
      while (logIterator.hasNextLog())
         if (logIterator.nextLog().getSessionId() == null)
         {
            isSessionNull = true;
            break;
         }

      return isSessionNull;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isTXAware()
   {
      return false;
   }

}
