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

import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.backup.BackupConfig;
import org.exoplatform.services.jcr.ext.backup.impl.AbstractFullBackupJob;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Calendar;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SARL Author : Alex Reshetnyak alex.reshetnyak@exoplatform.com.ua Nov
 * 21, 2007
 */
public class FullBackupJob extends AbstractFullBackupJob
{

   protected static Log log = ExoLogger.getLogger("ext.FullBackupJob");

   private String pathBackupFile;

   protected URL createStorage() throws FileNotFoundException, IOException
   {

      FileNameProducer fnp =
         new FileNameProducer(config.getRepository(), config.getWorkspace(), config.getBackupDir().getAbsolutePath(),
            super.timeStamp, true);

      return new URL("file:" + fnp.getNextFile().getAbsolutePath());
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
         log.error("Full backup initialization failed ", e);
         notifyError("Full backup initialization failed ", e);
      }
      catch (IOException e)
      {
         log.error("Full backup initialization failed ", e);
         notifyError("Full backup initialization failed ", e);
      }
   }

   public void run()
   {

      try
      {
         pathBackupFile = getStorageURL().getFile();

         SessionImpl session = (SessionImpl)repository.getSystemSession(workspaceName);

         try
         {
            notifyListeners();
            FileOutputStream fos = new FileOutputStream(pathBackupFile);
            session.exportWorkspaceSystemView(fos, false, false);
         }
         finally
         {
            session.logout();
         }
      }
      catch (RepositoryException e)
      {
         log.error("Full backup failed " + getStorageURL().getPath(), e);
         notifyError("Full backup failed", e);
      }
      catch (IOException e)
      {
         log.error("Full backup failed " + getStorageURL().getPath(), e);
         notifyError("Full backup failed", e);
      }

      state = FINISHED;

      notifyListeners();
   }

   public void stop()
   {
      log.info("Stop requested " + getStorageURL().getPath());
   }

}
