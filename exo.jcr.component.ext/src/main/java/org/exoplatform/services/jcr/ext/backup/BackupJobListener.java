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

/**
 * Created by The eXo Platform SAS.
 * 
 * Date: 05.02.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: BackupJobListener.java 760 2008-02-07 15:08:07Z pnedonosko $
 */
public interface BackupJobListener
{

   /**
    * onStateChanged.
    *
    * @param job
    *          BackupJob, the backup job
    */
   void onStateChanged(BackupJob job);

   /**
    * onError.
    *
    * @param job
    *          BackupJob, the backup job 
    * @param message
    *          String, the error message
    * @param error
    *          Throwable,  the cause exception
    */
   void onError(BackupJob job, String message, Throwable error);

}
