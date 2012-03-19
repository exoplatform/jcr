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
package org.exoplatform.services.jcr.ext.repository.creation.cluster;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.backup.AbstractBackupTestCase;
import org.exoplatform.services.jcr.ext.backup.BackupManager;
import org.exoplatform.services.jcr.ext.backup.ExtendedBackupManager;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupChain;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupConfig;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionMode;
import org.exoplatform.services.jcr.ext.repository.creation.DBCreationProperties;
import org.exoplatform.services.jcr.ext.repository.creation.RepositoryCreationException;
import org.exoplatform.services.jcr.ext.repository.creation.RepositoryCreationService;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig.DatabaseStructureType;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TestRepositoryCreationService.java 111 2008-11-11 11:11:11Z serg $
 */
public class TestRepositoryCreationServiceInClusterNode1 extends AbstractBackupTestCase
{
   public void testCreateRepositorySingleDBWithSpecificCreationProps() throws Exception
   {
      log.info("Node1: Waits the second node");
      Thread.sleep(60000);
      
      Map<String, String> connProps = new HashMap<String, String>();
      connProps.put("driverClassName", "com.mysql.jdbc.Driver");
      connProps.put("username", "root");
      connProps.put("password", "24635457");

      DBCreationProperties creationProps =
         new DBCreationProperties("jdbc:mysql://localhost/", connProps, "src/test/resources/test-mysql.sql", "user3",
            "pass3");

      // prepare
      String dsName = helper.createDatasource();
      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.SINGLE, dsName);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(DatabaseStructureType.SINGLE, dsName);
      helper.addWorkspace(repository, wsEntry);
      addConent(repository, wsEntry.getName());

      // backup
      File backDir = new File("target/backup");
      backDir.mkdirs();

      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(repository.getConfiguration().getName());
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backDir);

      RepositoryBackupChain bch = backup.startBackup(config);
      waitEndOfBackup(bch);
      backup.stopBackup(bch);

      // restore with RepositoryCreatorService
      RepositoryCreationService creatorService =
         (RepositoryCreationService)container.getComponentInstanceOfType(RepositoryCreationService.class);
      assertNotNull(creatorService);

      String tenantName = "tenant_4";
      String repoToken = creatorService.reserveRepositoryName(tenantName);

      // restore             
      RepositoryEntry newRE =
         helper.createRepositoryEntry(DatabaseStructureType.SINGLE, repository.getConfiguration().getSystemWorkspaceName(), tenantName);
      newRE.setName(tenantName);

      WorkspaceEntry newWSEntry = helper.createWorkspaceEntry(DatabaseStructureType.SINGLE, tenantName);
      newWSEntry.setName(wsEntry.getName());
      newRE.addWorkspace(newWSEntry);

      creatorService.createRepository(bch.getBackupId(), newRE, repoToken, creationProps);

      // check
      ManageableRepository restoredRepository = repositoryService.getRepository(tenantName);
      assertNotNull(restoredRepository);

      checkConent(restoredRepository, wsEntry.getName());

      //check repositoryConfiguration
      RepositoryService repoService = (RepositoryService)this.container.getComponentInstance(RepositoryService.class);
      assertNotNull(repoService.getConfig().getRepositoryConfiguration(tenantName));

      log.info("Node1: Repository has been created");
      Thread.sleep(60000);
      
      // remove repository
      try
      {
         creatorService.removeRepository(tenantName, false);
         fail("Exception should be thrown");
      }
      catch (RepositoryCreationException e)
      {
         // repository in use
      }

      // remove repository
      creatorService.removeRepository(tenantName, true);

      try
      {
         repoService.getRepository(tenantName);
         fail("Exception should be thrown");
      }
      catch (RepositoryException e)
      {
         // expected behavior, repository should be missing 
      }
      
      log.info("Node1: Repository removed");
   }

   @Override
   protected ExtendedBackupManager getBackupManager()
   {
      return getRDBMSBackupManager();
   }
}
