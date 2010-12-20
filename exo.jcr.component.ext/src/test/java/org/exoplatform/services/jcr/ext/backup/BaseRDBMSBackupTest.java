/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.ext.backup;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.jcr.ext.backup.impl.BackupManagerImpl;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 2010
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id$
 */
public class BaseRDBMSBackupTest
   extends AbstractBackupTestCase
{
   @Override
   protected ExtendedBackupManager getBackupManager()
   {
      InitParams initParams = new InitParams();
      PropertiesParam pps = new PropertiesParam();
      pps.setProperty(BackupManagerImpl.FULL_BACKUP_TYPE,
               "org.exoplatform.services.jcr.ext.backup.impl.rdbms.FullBackupJob");
      pps.setProperty(BackupManagerImpl.INCREMENTAL_BACKUP_TYPE,
               "org.exoplatform.services.jcr.ext.backup.impl.fs.IncrementalBackupJob");
      pps.setProperty(BackupManagerImpl.BACKUP_DIR, "target/backup_BaseRDBMSBackupTest");
      pps.setProperty(BackupManagerImpl.DEFAULT_INCREMENTAL_JOB_PERIOD, "3600");

      initParams.put(BackupManagerImpl.BACKUP_PROPERTIES, pps);

      BackupManagerImpl backupManagerImpl = new BackupManagerImpl(initParams, repositoryService);
      backupManagerImpl.start();

      return backupManagerImpl;
   }
}
