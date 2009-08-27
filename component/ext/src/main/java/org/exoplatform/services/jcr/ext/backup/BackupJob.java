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
package org.exoplatform.services.jcr.ext.backup;

import org.exoplatform.services.jcr.core.ManageableRepository;

import java.net.URL;
import java.util.Calendar;

/**
 * Created by The eXo Platform SARL .<br/>
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */

public interface BackupJob extends Runnable
{

   /**
    * The full backup type of job.
    */
   public static final int FULL = 1;

   /**
    * THe incremental backup type of job.
    */
   public static final int INCREMENTAL = 2;

   /**
    * The STARTING state of job.
    */
   public static final int STARTING = 0;

   /**
    * The WAITING state of job..
    */
   public static final int WAITING = 1;

   /**
    * The WORKING state of job.
    */
   public static final int WORKING = 2;

   /**
    * The FINISHED state of job..
    */
   public static final int FINISHED = 4;

   /**
    * Getting the type of job.
    * 
    * @return int
    *           return the type of job
    */
   int getType();

   /**
    * Getting state of job.
    *
    * @return int
    *           return the state of job
    */
   int getState();

   /**
    * Getting the id of job.
    *
    * @return int
    *           return id
    */
   int getId();

   /**
    * Getting storage URL.
    *
    * @return URL
    *           return the storage URL
    * @throws BackupOperationException
    *           will be generate the exception BackupOperationException 
    */
   URL getStorageURL() throws BackupOperationException;

   /**
    * Call stop of job.
    *
    */
   void stop();

   /**
    * Initialize.
    *
    * @param repository
    *          ManageableRepository, the manageable repository
    * @param workspaceName
    *          String, the workspace name 
    * @param config
    *          BackupConfig, the backup config
    * @param timeStamp
    *          Calendar,  the time stamp
    */
   void init(ManageableRepository repository, String workspaceName, BackupConfig config, Calendar timeStamp);

   /**
    * Adding listener.
    *
    * @param listener
    *          BackupJobListener, the job listener
    */
   void addListener(BackupJobListener listener);

   /**
    * Remove listener.
    *
    * @param listener
    *          BackupJobListener, the job listener
    */
   void removeListener(BackupJobListener listener);

}
