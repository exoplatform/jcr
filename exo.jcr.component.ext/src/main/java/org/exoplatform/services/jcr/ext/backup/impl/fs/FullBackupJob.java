/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.ext.backup.impl.fs;

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.backup.BackupConfig;
import org.exoplatform.services.jcr.ext.backup.impl.AbstractFullBackupJob;
import org.exoplatform.services.jcr.ext.backup.impl.FileNameProducer;
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

   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.ext.FullBackupJob");

   private String pathBackupFile;

   @Override
   protected URL createStorage() throws FileNotFoundException, IOException
   {

      FileNameProducer fnp =
         new FileNameProducer(config.getRepository(), config.getWorkspace(),
            PrivilegedFileHelper.getAbsolutePath(config.getBackupDir()), super.timeStamp, true);

      return new URL("file:" + PrivilegedFileHelper.getAbsolutePath(fnp.getNextFile()));
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
         LOG.error("Full backup initialization failed ", e);
         notifyError("Full backup initialization failed ", e);
      }
      catch (IOException e)
      {
         LOG.error("Full backup initialization failed ", e);
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
            FileOutputStream fos = PrivilegedFileHelper.fileOutputStream(pathBackupFile);
            session.exportWorkspaceSystemView(fos, false, false);
         }
         finally
         {
            session.logout();
         }
      }
      catch (RepositoryException e)
      {
         LOG.error("Full backup failed " + getStorageURL().getPath(), e);
         notifyError("Full backup failed", e);
      }
      catch (IOException e)
      {
         LOG.error("Full backup failed " + getStorageURL().getPath(), e);
         notifyError("Full backup failed", e);
      }

      state = FINISHED;

      notifyListeners();
   }

   public void stop()
   {
      LOG.info("Stop requested " + getStorageURL().getPath());
   }

}
