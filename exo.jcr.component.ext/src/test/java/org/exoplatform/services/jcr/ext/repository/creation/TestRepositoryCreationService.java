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
package org.exoplatform.services.jcr.ext.repository.creation;

import java.io.File;

import javax.jcr.Node;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.backup.AbstractBackupTestCase;
import org.exoplatform.services.jcr.ext.backup.BackupManager;
import org.exoplatform.services.jcr.ext.backup.ExtendedBackupManager;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupChain;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupConfig;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TestRepositoryCreationService.java 111 2008-11-11 11:11:11Z serg $
 */
public class TestRepositoryCreationService extends AbstractBackupTestCase
{

   protected ExtendedBackupManager getBackupManager()
   {
      return (ExtendedBackupManager) container.getComponentInstanceOfType(BackupManager.class);
   }

   public void testCreateRepository() throws Exception
   {

      // backup
      File backDir = new File("target/backup");
      backDir.mkdirs();

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

      // restore with RepositoryCreatorService
      RepositoryCreationService creatorService =
         (RepositoryCreationService)container.getComponentInstanceOfType(RepositoryCreationService.class);

      String tenantName = "new_repository";

      String repoToken = creatorService.reserveRepositoryName(tenantName);

      // restore             
      RepositoryEntry baseRE =
         (RepositoryEntry)ws1Session.getContainer().getComponentInstanceOfType(RepositoryEntry.class);

      RepositoryEntry rEntry = makeRepositoryEntry(tenantName, baseRE, "source", null);

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

   public void testReserveRepositoryNameException() throws Exception
   {
      RepositoryCreationService creatorService =
         (RepositoryCreationService)container.getComponentInstanceOfType(RepositoryCreationService.class);

      // 1) check unexist repository same name
      String tenantName = "new_repository_2";

      String repoToken = creatorService.reserveRepositoryName(tenantName);
      assertNotNull(repoToken);

      try
      {
         creatorService.reserveRepositoryName(tenantName);
         fail("There must be RepositoryCreationException.");
      }
      catch (RepositoryCreationException e)
      {
         //ok
      }

      // 2)try to reserve already existing repository
      try
      {
         creatorService.reserveRepositoryName(this.repository.getName());
         fail("There must be RepositoryCreationException.");
      }
      catch (RepositoryCreationException e)
      {
         //ok
      }
   }

   public void testCreateRepositoryException() throws Exception
   {
      String tenantName = "new_repository_3";
      RepositoryEntry baseRE =
         (RepositoryEntry)ws1Session.getContainer().getComponentInstanceOfType(RepositoryEntry.class);

      RepositoryEntry rEntry = makeRepositoryEntry(tenantName, baseRE, "source2", null);

      RepositoryCreationService creatorService =
         (RepositoryCreationService)container.getComponentInstanceOfType(RepositoryCreationService.class);

      // 1) try to create with unregistered token
      try
      {
         creatorService.createRepository("nomatter", rEntry, "any_name");
         fail("There must be RepositoryCreationException.");
      }
      catch (RepositoryCreationException e)
      {
         //ok
      }

      String repoToken = creatorService.reserveRepositoryName(tenantName);
      // 2) test with malformed repository entry

      RepositoryEntry brokenRepositoryEntry = rEntry;

      brokenRepositoryEntry.getWorkspaceEntries().get(0).getContainer().getParameters().remove(0);
      brokenRepositoryEntry.getWorkspaceEntries().get(0).getContainer().getParameters().remove(0);

      try
      {
         creatorService.createRepository("nomatter", brokenRepositoryEntry, repoToken);
         fail("There must be RepositoryConfigurationException.");
      }
      catch (RepositoryConfigurationException e)
      {
         //ok
      }

      repoToken = creatorService.reserveRepositoryName(tenantName);
      // 3) test configuration with existing datasource
      RepositoryEntry rEntryWithRealDataSource = makeRepositoryEntry(tenantName, baseRE, null, null);
      try
      {
         creatorService.createRepository("nomatter", rEntryWithRealDataSource, repoToken);
         fail("There must be RepositoryConfigurationException.");
      }
      catch (RepositoryConfigurationException e)
      {
         //ok
      }
   }
}
