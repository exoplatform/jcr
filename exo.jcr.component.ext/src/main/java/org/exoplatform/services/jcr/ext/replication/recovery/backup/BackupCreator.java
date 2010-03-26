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
package org.exoplatform.services.jcr.ext.replication.recovery.backup;

import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.replication.recovery.FileNameFactory;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: BackupCreator.java 34445 2009-07-24 07:51:18Z dkatayev $
 */

public class BackupCreator implements Runnable
{
   /**
    * The apache logger.
    */
   private static Log log = ExoLogger.getLogger("exo.jcr.component.ext.BackupCreator");

   /**
    * The thread to BackupCreator.
    */
   private Thread backupCreatorThread;

   /**
    * The delay timeout.
    */
   private long delayTime;

   /**
    * The workspace name.
    */
   private String workspaceName;

   /**
    * The backup directory.
    */
   private File backupDir;

   /**
    * The ManageableRepository will be needed to thread naming.
    */
   private ManageableRepository manageableRepository;

   /**
    * The FileNameFactory will be created file names.
    */
   private FileNameFactory fileNameFactory;

   /**
    * BackupCreator constructor.
    * 
    * @param delayTime
    *          the delay timeout
    * @param workspaceName
    *          the workspace name
    * @param backupDir
    *          the backup directory
    * @param manageableRepository
    *          the ManageableReposirory
    */
   public BackupCreator(long delayTime, String workspaceName, File backupDir, ManageableRepository manageableRepository)
   {
      this.delayTime = delayTime;
      this.workspaceName = workspaceName;
      this.backupDir = backupDir;
      this.manageableRepository = manageableRepository;

      fileNameFactory = new FileNameFactory();

      backupCreatorThread =
         new Thread(this, "BackupCreatorThread@" + manageableRepository.getConfiguration().getName() + ":"
            + workspaceName);
      backupCreatorThread.start();
   }

   /**
    * {@inheritDoc}
    */
   public void run()
   {
      try
      {
         Thread.yield();
         Thread.sleep(delayTime);

         log.info("The backup has been started : " + manageableRepository.getConfiguration().getName() + "@"
            + workspaceName);

         SessionImpl session = (SessionImpl)manageableRepository.getSystemSession(workspaceName);

         Calendar backupTime = Calendar.getInstance();
         String fileName =
            manageableRepository.getConfiguration().getName() + "_" + workspaceName + "_"
               + fileNameFactory.getStrDate(backupTime) + "_" + fileNameFactory.getStrTime(backupTime) + ".xml";

         File backupFile = new File(backupDir.getCanonicalPath() + File.separator + fileName);

         if (backupFile.createNewFile())
         {

            session.exportWorkspaceSystemView(new FileOutputStream(backupFile), false, false);

            log.info("The backup has been finished : " + manageableRepository.getConfiguration().getName() + "@"
               + workspaceName);
         }
         else
            throw new IOException("Can't create file : " + backupFile.getCanonicalPath());

      }
      catch (InterruptedException ie)
      {
         log.error("The InterruptedExeption", ie);
      }
      catch (RepositoryException e)
      {
         log.error("The RepositoryException", e);
      }
      catch (IOException e)
      {
         log.error("The IOException", e);
      }

   }
}
