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
package org.exoplatform.services.jcr.ext.repository;

import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.ext.backup.AbstractBackupTestCase;
import org.exoplatform.services.jcr.ext.backup.ExtendedBackupManager;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig.DatabaseStructureType;

import java.net.URI;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date: 26.08.2009
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: RestRepositoryServiceTest.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class RestRepositoryServiceTest extends AbstractBackupTestCase
{
   private String REST_REPOSITORY_SERVICE_PATH = RestRepositoryService.Constants.BASE_URL;

   public void testRepositoriesList() throws Exception
   {
      TesterContainerResponce cres =
         makeGetRequest(new URI(REST_REPOSITORY_SERVICE_PATH
            + RestRepositoryService.Constants.OperationType.REPOSITORIES_LIST));

      assertEquals(200, cres.getStatus());

      NamesList repositories = (NamesList)getObject(NamesList.class, cres.responseWriter.getBody());

      assertNotNull(repositories);
      assertEquals(repositoryService.getConfig().getRepositoryConfigurations().size(), repositories.getNames().size());
   }

   public void testWorkspacesList() throws Exception
   {
      String repoName = repositoryService.getConfig().getDefaultRepositoryName();

      TesterContainerResponce cres =
         makeGetRequest(new URI(REST_REPOSITORY_SERVICE_PATH
            + RestRepositoryService.Constants.OperationType.WORKSPACES_LIST + "/" + repoName + "/"));

      assertEquals(200, cres.getStatus());

      NamesList workspaces = (NamesList)getObject(NamesList.class, cres.responseWriter.getBody());

      assertNotNull(workspaces);
      assertEquals(repositoryService.getConfig().getRepositoryConfiguration(repoName).getWorkspaceEntries().size(),
         workspaces.getNames().size());
   }

   public void testGetDefaultWorkspaceConfig() throws Exception
   {
      String repoName = repositoryService.getConfig().getDefaultRepositoryName();

      TesterContainerResponce cres =
         makeGetRequest(new URI(REST_REPOSITORY_SERVICE_PATH
            + RestRepositoryService.Constants.OperationType.DEFAULT_WS_CONFIG + "/" + repoName + "/"));

      assertEquals(200, cres.getStatus());

      WorkspaceEntry workspaceEntry = (WorkspaceEntry)getObject(WorkspaceEntry.class, cres.responseWriter.getBody());

      assertNotNull(workspaceEntry);
      assertEquals(repositoryService.getConfig().getRepositoryConfiguration(repoName).getDefaultWorkspaceName(),
         workspaceEntry.getName());
   }

   public void testGetRepositoryServiceConfiguration() throws Exception
   {
      TesterContainerResponce cres =
         makeGetRequest(new URI(REST_REPOSITORY_SERVICE_PATH
            + RestRepositoryService.Constants.OperationType.REPOSITORY_SERVICE_CONFIGURATION));

      assertEquals(200, cres.getStatus());

      RepositoryServiceConf repositoryConf =
         (RepositoryServiceConf)getObject(RepositoryServiceConf.class, cres.responseWriter.getBody());

      assertNotNull(repositoryConf);
      assertEquals(repositoryService.getConfig().getDefaultRepositoryName(), repositoryConf.getDefaultRepositoryName());
      assertEquals(repositoryService.getConfig().getRepositoryConfigurations().size(), repositoryConf.getRepositories()
         .size());
   }

   public void testCreateRepository() throws Exception
   {
      RepositoryEntry rEntry = helper.createRepositoryEntry(DatabaseStructureType.MULTI, null, null);

      TesterContainerResponce cres =
         makePostRequest(new URI(REST_REPOSITORY_SERVICE_PATH
            + RestRepositoryService.Constants.OperationType.CREATE_REPOSITORY), rEntry);

      assertEquals(200, cres.getStatus());
      assertTrue(isRepositoryExists(rEntry.getName()));
   }

   public void testCreateRepositoryWithInvalidChars() throws Exception
   {
      String wsName = "ws_over:?//\\__rest!!_1";
      String rName = "repo:?//\\_over:_re??st";

      RepositoryEntry rEntry = helper.createRepositoryEntry(DatabaseStructureType.MULTI, wsName, null);
      rEntry.setName(rName);

      TesterContainerResponce cres =
         makePostRequest(new URI(REST_REPOSITORY_SERVICE_PATH
            + RestRepositoryService.Constants.OperationType.CREATE_REPOSITORY), rEntry);

      assertEquals(200, cres.getStatus());
      assertTrue(isRepositoryExists(rEntry.getName()));
   }

   public void testCreateWorkspace() throws Exception
   {
      WorkspaceEntry wEntry = helper.createWorkspaceEntry(DatabaseStructureType.MULTI, null);

      TesterContainerResponce cres =
         makePostRequest(new URI(REST_REPOSITORY_SERVICE_PATH
            + RestRepositoryService.Constants.OperationType.CREATE_WORKSPACE + "/db1"), wEntry);

      assertEquals(200, cres.getStatus());
      assertTrue(isWorkspaceExists("db1", wEntry.getName()));
   }

   public void testRemoveWorkspace() throws Exception
   {
      RepoInfo rInfo = createRepositoryAndGetSession();

      TesterContainerResponce cres =
         makePostRequest(new URI(REST_REPOSITORY_SERVICE_PATH
            + RestRepositoryService.Constants.OperationType.REMOVE_WORKSPACE + "/" + rInfo.rName + "/" + rInfo.wsName
            + "/false/"), "");

      assertEquals(409, cres.getStatus());

      cres =
         makePostRequest(new URI(REST_REPOSITORY_SERVICE_PATH
            + RestRepositoryService.Constants.OperationType.REMOVE_WORKSPACE + "/" + rInfo.rName + "/" + rInfo.wsName
            + "/true/"), "");
      assertEquals(200, cres.getStatus());

      assertFalse(isWorkspaceExists(rInfo.rName, rInfo.wsName));
   }

   public void testRemoveRepository() throws Exception
   {
      RepoInfo rInfo = createRepositoryAndGetSession();


      TesterContainerResponce cres =
         makeGetRequest(new URI(REST_REPOSITORY_SERVICE_PATH
            + RestRepositoryService.Constants.OperationType.REMOVE_REPOSITORY + "/" + rInfo.rName + "/false/"));

      assertEquals(409, cres.getStatus());

      cres =
         makeGetRequest(new URI(REST_REPOSITORY_SERVICE_PATH
            + RestRepositoryService.Constants.OperationType.REMOVE_REPOSITORY + "/" + rInfo.rName + "/true/"));

      assertEquals(200, cres.getStatus());
      assertFalse(isRepositoryExists(rInfo.rName));
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ExtendedBackupManager getBackupManager()
   {
      return getJCRBackupManager();
   }
}
