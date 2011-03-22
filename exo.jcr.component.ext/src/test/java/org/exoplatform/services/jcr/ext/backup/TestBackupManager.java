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

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.jcr.ext.backup.impl.BackupManagerImpl;


/**
 * Created by The eXo Platform SAS.
 *  Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 05.12.2007
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestBackupManager.java 760 2008-02-07 15:08:07Z pnedonosko $
 */
public class TestBackupManager
   extends AbstractBackupUseCasesTest
{

   private BackupManagerImpl backup;

   @Override
   protected ExtendedBackupManager getBackupManager()
   {
      if (backup == null)
      {

         InitParams initParams = new InitParams();
         PropertiesParam pps = new PropertiesParam();
         pps.setProperty(BackupManagerImpl.FULL_BACKUP_TYPE,
            "org.exoplatform.services.jcr.ext.backup.impl.fs.FullBackupJob");
         pps.setProperty(BackupManagerImpl.INCREMENTAL_BACKUP_TYPE,
            "org.exoplatform.services.jcr.ext.backup.impl.fs.IncrementalBackupJob");
         pps.setProperty(BackupManagerImpl.BACKUP_DIR, "target/backup_testBackupManager");
         pps.setProperty(BackupManagerImpl.DEFAULT_INCREMENTAL_JOB_PERIOD, "3600");

         initParams.put(BackupManagerImpl.BACKUP_PROPERTIES, pps);

         backup = new BackupManagerImpl(initParams, repositoryService);
         backup.start();
      }

      return backup;
   }
}
