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
package org.exoplatform.services.jcr.impl.backup;

import org.exoplatform.services.jcr.impl.backup.rdbms.DataRestoreContext;

import java.io.File;

/**
 * @author <a href="mailto:anatoliy.bazko@gmail.com">Anatoliy Bazko</a>
 * @version $Id: Backupable.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public interface Backupable
{
   /**
    * Backup data.
    * 
    * @param storageDir
    *          the directory to store backup
    * @throws BackupException
    *          if any exception occurred
    */
   void backup(File storageDir) throws BackupException;

   /**
    * Clean data.
    * 
    * @throws BackupException
    *          if any exception occurred
    */
   void clean() throws BackupException;

   /**
    * Get data restorer to support atomic restore.
    * 
    * @param context
    *          the context
    * @throws BackupException
    *          if any exception occurred
    */
   DataRestore getDataRestorer(DataRestoreContext context) throws BackupException;

}
