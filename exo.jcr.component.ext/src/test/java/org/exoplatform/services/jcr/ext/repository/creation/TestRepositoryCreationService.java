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

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.backup.AbstractBackupTestCase;
import org.exoplatform.services.jcr.ext.backup.BackupManager;
import org.exoplatform.services.jcr.ext.backup.ExtendedBackupManager;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupChain;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupConfig;
import org.exoplatform.services.jcr.util.IdGenerator;

import java.io.File;

import javax.jcr.RepositoryException;

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

   public void testCreateRepositoryMultiDB() throws Exception
   {
      // prepare
      ManageableRepository repository = helper.createRepository(container, true, null);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(true, null);
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

      String tenantName = "new_repository_mutli-db";
      String repoToken = creatorService.reserveRepositoryName(tenantName);

      // restore             
      RepositoryEntry newRE =
         helper.createRepositoryEntry(true, repository.getConfiguration().getSystemWorkspaceName(),
            IdGenerator.generate());
      newRE.setName(tenantName);

      WorkspaceEntry newWSEntry = helper.createWorkspaceEntry(true, IdGenerator.generate());
      newWSEntry.setName(wsEntry.getName());
      newRE.addWorkspace(newWSEntry);

      creatorService.createRepository(bch.getBackupId(), newRE, repoToken);

      // check
      ManageableRepository restoredRepository = repositoryService.getRepository(tenantName);
      assertNotNull(restoredRepository);
      
      checkConent(restoredRepository, wsEntry.getName());

      //check repositoryConfiguration
      RepositoryService repoService = (RepositoryService)this.container.getComponentInstance(RepositoryService.class);
      assertNotNull(repoService.getConfig().getRepositoryConfiguration(tenantName));

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
   }

   /* TODO
   public void testCreateRepositorySingleDB() throws Exception
   {
      // prepare
      String dsName = helper.createDatasource();
      ManageableRepository repository = helper.createRepository(container, false, dsName);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(false, dsName);
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

      String tenantName = "new_repository_single-db";
      String repoToken = creatorService.reserveRepositoryName(tenantName);

      // restore             
      String newDSName = IdGenerator.generate();

      RepositoryEntry newRE =
         helper.createRepositoryEntry(false, repository.getConfiguration().getSystemWorkspaceName(), newDSName);
      newRE.setName(tenantName);

      WorkspaceEntry newWSEntry = helper.createWorkspaceEntry(false, newDSName);
      newWSEntry.setName(wsEntry.getName());
      newRE.addWorkspace(newWSEntry);

      creatorService.createRepository(bch.getBackupId(), newRE, repoToken);

      // check
      ManageableRepository restoredRepository = repositoryService.getRepository(tenantName);
      assertNotNull(restoredRepository);

      checkConent(restoredRepository, wsEntry.getName());

      //check repositoryConfiguration
      RepositoryService repoService = (RepositoryService)this.container.getComponentInstance(RepositoryService.class);
      assertNotNull(repoService.getConfig().getRepositoryConfiguration(tenantName));

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
   }

   public void testCreateRepositorySingleDBWithSpecificCreationProps() throws Exception
   {
      Map<String, String> connProps = new HashMap<String, String>();
      connProps.put("driverClassName", "org.hsqldb.jdbcDriver");
      connProps.put("username", "sa");
      connProps.put("password", "");

      DBCreationProperties creationProps =
         new DBCreationProperties("jdbc:hsqldb:file:target/temp/data_2/", connProps, "src/test/resources/test.sql",
            "sa", "");

      // prepare
      String dsName = helper.createDatasource();
      ManageableRepository repository = helper.createRepository(container, false, dsName);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(false, dsName);
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

      String tenantName = "new_repository_single-db-specific-props";
      String repoToken = creatorService.reserveRepositoryName(tenantName);

      // restore             
      String newDSName = IdGenerator.generate();

      RepositoryEntry newRE =
         helper.createRepositoryEntry(false, repository.getConfiguration().getSystemWorkspaceName(), newDSName);
      newRE.setName(tenantName);

      WorkspaceEntry newWSEntry = helper.createWorkspaceEntry(false, newDSName);
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
   }*/

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

      // prepare
      ManageableRepository repository = helper.createRepository(container, true, null);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(true, null);
      helper.addWorkspace(repository, wsEntry);
      addConent(repository, wsEntry.getName());

      RepositoryCreationService creatorService =
         (RepositoryCreationService)container.getComponentInstanceOfType(RepositoryCreationService.class);

      // 1) try to create with unregistered token
      try
      {
         creatorService.createRepository("nomatter", repository.getConfiguration(), "any_name");
         fail("There must be RepositoryCreationException.");
      }
      catch (RepositoryCreationException e)
      {
         //ok
      }
   }

   public void testCreateRepositoryMultiDBExistingDS() throws Exception
   {
      // prepare
      ManageableRepository repository = helper.createRepository(container, true, null);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(true, null);
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

      String tenantName = "new_repository_mutli-db_existing_ds";
      String repoToken = creatorService.reserveRepositoryName(tenantName);

      // restore             
      RepositoryEntry newRE =
         helper.createRepositoryEntry(true, repository.getConfiguration().getSystemWorkspaceName(), null);
      newRE.setName(tenantName);

      WorkspaceEntry newWSEntry = helper.createWorkspaceEntry(true, null);
      newWSEntry.setName(wsEntry.getName());
      newRE.addWorkspace(newWSEntry);

      try
      {
         creatorService.createRepository(bch.getBackupId(), newRE, repoToken);
         fail("Exception should be thrown");
      }
      catch (RepositoryConfigurationException e)
      {
         // ok
      }
   }

   @Override
   protected ExtendedBackupManager getBackupManager()
   {
      return getRDBMSBackupManager();
   }
}
