/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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

import org.exoplatform.services.database.creator.DBCreator;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.repository.creation.RepositoryCreationService;
import org.exoplatform.services.jcr.ext.repository.creation.RepositoryCreationServiceImpl;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.naming.InitialContextInitializer;

import java.io.File;

import javax.jcr.Node;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TestRDBMSRepositoryRestore.java 111 2011-03-16 11:11:11Z serg $
 */
public class TestRDBMSRepositoryRestore extends BaseRDBMSBackupTest
{
   public void testCreateRepositoryFromBackup() throws Exception
   {
      // backup
      File backDir = new File("target/backup");
      backDir.mkdirs();

      RepositoryImpl repository = getReposityToBackup();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repository.getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      backup.startBackup(config);

      RepositoryBackupChain bch = backup.findRepositoryBackup(repository.getName());

      backup.getRepositoryBackupsLogs();

      // wait till full backup will be stopped
      while (bch.getState() != RepositoryBackupChain.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }

      // stop fullBackup
      if (bch != null)
         backup.stopBackup(bch);
      else
         fail("Can't get fullBackup chain");

      DBCreator dbCreator = (DBCreator)container.getComponentInstanceOfType(DBCreator.class);
      InitialContextInitializer initialContextInitializer =
         (InitialContextInitializer)container.getComponentInstanceOfType(InitialContextInitializer.class);

      RepositoryCreationService creatorService =
         new RepositoryCreationServiceImpl(repositoryService, backup, dbCreator, initialContextInitializer);

      String tenantName = "new_rdbms_repository";

      String repoToken = creatorService.reserveRepositoryName(tenantName);

      // restore             
      RepositoryEntry baseRE =
         (RepositoryEntry)ws1Session.getContainer().getComponentInstanceOfType(RepositoryEntry.class);

      RepositoryEntry rEntry = makeRepositoryEntry(tenantName, baseRE, "new_rdbms_repository_source", null);

      creatorService.createRepository(bch.getBackupId(), rEntry, repoToken);

      // check
      ManageableRepository restoredRepository = repositoryService.getRepository(tenantName);
      assertNotNull(restoredRepository);

      for (String wsName : restoredRepository.getWorkspaceNames())
      {
         SessionImpl back = null;
         try
         {
            back = (SessionImpl)restoredRepository.login(credentials, wsName);
            Node ws1backTestRoot = back.getRootNode().getNode("backupTest");
            assertEquals("Restored content should be same", "property-5", ws1backTestRoot.getNode("node_5")
               .getProperty("exo:data").getString());
         }
         catch (Exception e)
         {
            e.printStackTrace();
            fail(e.getMessage());
         }
         finally
         {
            if (back != null)
               back.logout();
         }
      }
      //check repositoryConfiguration
      RepositoryService repoService = (RepositoryService)this.container.getComponentInstance(RepositoryService.class);
      assertNotNull(repoService.getConfig().getRepositoryConfiguration(tenantName));
   }
}
