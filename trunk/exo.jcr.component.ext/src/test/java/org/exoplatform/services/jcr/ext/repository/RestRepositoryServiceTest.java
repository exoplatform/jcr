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

import org.exoplatform.services.jcr.config.ContainerEntry;
import org.exoplatform.services.jcr.config.QueryHandlerEntry;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.SimpleParameterEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.BaseStandaloneTest;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.app.ThreadLocalSessionProviderService;
import org.exoplatform.services.jcr.ext.backup.ContainerRequestUserRole;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.util.TesterConfigurationHelper;
import org.exoplatform.services.rest.RequestHandler;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.InputHeadersMap;
import org.exoplatform.services.rest.impl.MultivaluedMapImpl;
import org.exoplatform.services.rest.tools.ByteArrayContainerResponseWriter;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.ws.frameworks.json.JsonHandler;
import org.exoplatform.ws.frameworks.json.JsonParser;
import org.exoplatform.ws.frameworks.json.impl.BeanBuilder;
import org.exoplatform.ws.frameworks.json.impl.JsonDefaultHandler;
import org.exoplatform.ws.frameworks.json.impl.JsonGeneratorImpl;
import org.exoplatform.ws.frameworks.json.impl.JsonParserImpl;
import org.exoplatform.ws.frameworks.json.value.JsonValue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;

import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date: 26.08.2009
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: RestRepositoryServiceTest.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class RestRepositoryServiceTest extends BaseStandaloneTest
{
   private String REST_REPOSITORY_SERVICE_PATH = RestRepositoryService.Constants.BASE_URL;

   protected TesterConfigurationHelper helper = TesterConfigurationHelper.getInstance();

   private RequestHandler handler;

   public void setUp() throws Exception
   {
      super.setUp();

      handler = (RequestHandler)container.getComponentInstanceOfType(RequestHandler.class);

      SessionProviderService sessionProviderService =
         (SessionProviderService)container.getComponentInstanceOfType(ThreadLocalSessionProviderService.class);
      assertNotNull(sessionProviderService);
      sessionProviderService.setSessionProvider(null, new SessionProvider(new ConversationState(new Identity("root"))));
   }

   public void testRepositoriesList() throws Exception
   {
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      ContainerRequestUserRole creq =
         new ContainerRequestUserRole("GET", new URI(REST_REPOSITORY_SERVICE_PATH
            + RestRepositoryService.Constants.OperationType.REPOSITORIES_LIST), new URI(""), null, new InputHeadersMap(
            headers));

      ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
      ContainerResponse cres = new ContainerResponse(responseWriter);
      handler.handleRequest(creq, cres);

      assertEquals(200, cres.getStatus());

      NamesList repositories = (NamesList)getObject(NamesList.class, responseWriter.getBody());

      assertNotNull(repositories);
      assertEquals(repositoryService.getConfig().getRepositoryConfigurations().size(), repositories.getNames().size());
   }

   public void testWorkspacesList() throws Exception
   {
      String repoName = repositoryService.getConfig().getDefaultRepositoryName();

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      ContainerRequestUserRole creq =
         new ContainerRequestUserRole("GET", new URI(REST_REPOSITORY_SERVICE_PATH
            + RestRepositoryService.Constants.OperationType.WORKSPACES_LIST + "/" + repoName + "/"), new URI(""), null,
            new InputHeadersMap(headers));

      ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
      ContainerResponse cres = new ContainerResponse(responseWriter);
      handler.handleRequest(creq, cres);

      assertEquals(200, cres.getStatus());

      NamesList workspaces = (NamesList)getObject(NamesList.class, responseWriter.getBody());

      assertNotNull(workspaces);
      assertEquals(repositoryService.getConfig().getRepositoryConfiguration(repoName).getWorkspaceEntries().size(),
         workspaces.getNames().size());
   }

   public void testGetDefaultWorkspaceConfig() throws Exception
   {
      String repoName = repositoryService.getConfig().getDefaultRepositoryName();

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      ContainerRequestUserRole creq =
         new ContainerRequestUserRole("GET", new URI(REST_REPOSITORY_SERVICE_PATH
            + RestRepositoryService.Constants.OperationType.DEFAULT_WS_CONFIG + "/" + repoName + "/"), new URI(""),
            null, new InputHeadersMap(headers));

      ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
      ContainerResponse cres = new ContainerResponse(responseWriter);
      handler.handleRequest(creq, cres);

      assertEquals(200, cres.getStatus());

      WorkspaceEntry workspaceEntry = (WorkspaceEntry)getObject(WorkspaceEntry.class, responseWriter.getBody());

      assertNotNull(workspaceEntry);
      assertEquals(repositoryService.getConfig().getRepositoryConfiguration(repoName).getDefaultWorkspaceName(),
         workspaceEntry.getName());
   }

   public void testGetRepositoryServiceConfiguration() throws Exception
   {
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      ContainerRequestUserRole creq =
         new ContainerRequestUserRole("GET", new URI(REST_REPOSITORY_SERVICE_PATH
            + RestRepositoryService.Constants.OperationType.REPOSITORY_SERVICE_CONFIGURATION), new URI(""), null,
            new InputHeadersMap(headers));

      ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
      ContainerResponse cres = new ContainerResponse(responseWriter);
      handler.handleRequest(creq, cres);

      assertEquals(200, cres.getStatus());

      RepositoryServiceConf repositoryConf =
         (RepositoryServiceConf)getObject(RepositoryServiceConf.class, responseWriter.getBody());

      assertNotNull(repositoryConf);
      assertEquals(repositoryService.getConfig().getDefaultRepositoryName(), repositoryConf.getDefaultRepositoryName());
      assertEquals(repositoryService.getConfig().getRepositoryConfigurations().size(), repositoryConf.getRepositories()
         .size());
   }

   public void testCreateRepository() throws Exception
   {
      String wsName = "ws_over_rest_1";
      String rName = "repo_over_rest";

      RepositoryEntry rDefault =
         repositoryService.getConfig().getRepositoryConfiguration(
            repositoryService.getConfig().getDefaultRepositoryName());

      RepositoryEntry rEntry = new RepositoryEntry();

      rEntry.setName(rName);
      rEntry.setSessionTimeOut(3600000);
      rEntry.setAuthenticationPolicy("org.exoplatform.services.jcr.impl.core.access.JAASAuthenticator");
      rEntry.setSecurityDomain("exo-domain");
      rEntry.setSystemWorkspaceName(wsName);
      rEntry.setDefaultWorkspaceName(wsName);

      WorkspaceEntry wEntry =
         makeWorkspaceEntry(rDefault.getWorkspaceEntries().get(0), rName, wsName, "jdbcjcr_to_rest_repo_1", true);
      rEntry.addWorkspace(wEntry);

      JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
      JsonValue json = generatorImpl.createJsonObject(rEntry);

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.putSingle("Content-Type", "application/json; charset=UTF-8");

      ContainerRequestUserRole creq =
         new ContainerRequestUserRole("POST", new URI(REST_REPOSITORY_SERVICE_PATH
            + RestRepositoryService.Constants.OperationType.CREATE_REPOSITORY), new URI(""), new ByteArrayInputStream(
            json.toString().getBytes("UTF-8")), new InputHeadersMap(headers));

      System.out.print("testCreateRepository  : " + json.toString());

      ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
      ContainerResponse cres = new ContainerResponse(responseWriter);
      handler.handleRequest(creq, cres);

      assertEquals(200, cres.getStatus());

      Session session =
         repositoryService.getRepository(rName).login(new CredentialsImpl("root", "exo".toCharArray()), wsName);
      assertNotNull(session);
      assertNotNull(session.getRootNode());
      session.logout();
   }

   public void testCreateRepositoryWithInvalidChars() throws Exception
   {
      String wsName = "ws_over:?//\\__rest!!_1";
      String rName = "repo:?//\\_over:_re??st";

      RepositoryEntry rDefault =
         repositoryService.getConfig().getRepositoryConfiguration(
            repositoryService.getConfig().getDefaultRepositoryName());

      RepositoryEntry rEntry = new RepositoryEntry();

      rEntry.setName(rName);
      rEntry.setSessionTimeOut(3600000);
      rEntry.setAuthenticationPolicy("org.exoplatform.services.jcr.impl.core.access.JAASAuthenticator");
      rEntry.setSecurityDomain("exo-domain");
      rEntry.setSystemWorkspaceName(wsName);
      rEntry.setDefaultWorkspaceName(wsName);

      WorkspaceEntry wEntry =
         makeWorkspaceEntry(rDefault.getWorkspaceEntries().get(0), rName, wsName, "jdbcjcr_to_rest_repo_1", true);
      rEntry.addWorkspace(wEntry);

      JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
      JsonValue json = generatorImpl.createJsonObject(rEntry);

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.putSingle("Content-Type", "application/json; charset=UTF-8");

      ContainerRequestUserRole creq =
         new ContainerRequestUserRole("POST", new URI(REST_REPOSITORY_SERVICE_PATH
            + RestRepositoryService.Constants.OperationType.CREATE_REPOSITORY), new URI(""), new ByteArrayInputStream(
            json.toString().getBytes("UTF-8")), new InputHeadersMap(headers));

      System.out.print("testCreateRepository  : " + json.toString());

      ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
      ContainerResponse cres = new ContainerResponse(responseWriter);
      handler.handleRequest(creq, cres);

      assertEquals(200, cres.getStatus());

      Session session =
         repositoryService.getRepository(rName).login(new CredentialsImpl("root", "exo".toCharArray()), wsName);
      assertNotNull(session);
      assertNotNull(session.getRootNode());
      session.logout();
   }

   public void testCreateWorkspace() throws Exception
   {
      String wsName = "ws_over_rest_2";
      String rName = "repo_over_rest";

      WorkspaceEntry wEntry =
         makeWorkspaceEntry(repositoryService.getDefaultRepository().getConfiguration().getWorkspaceEntries().get(0),
            rName, wsName, "jdbcjcr_to_rest_repo_2", true);
      wEntry.setAccessManager(null);

      JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
      JsonValue json = generatorImpl.createJsonObject(wEntry);

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
      headers.putSingle("Content-Type", "application/json; charset=UTF-8");

      ContainerRequestUserRole creq =
         new ContainerRequestUserRole("POST", new URI(REST_REPOSITORY_SERVICE_PATH
            + RestRepositoryService.Constants.OperationType.CREATE_WORKSPACE + "/" + rName), new URI(""),
            new ByteArrayInputStream(json.toString().getBytes("UTF-8")), new InputHeadersMap(headers));

      System.out.print("testCreateWorkspace  : " + json.toString());

      ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
      ContainerResponse cres = new ContainerResponse(responseWriter);
      handler.handleRequest(creq, cres);

      assertEquals(200, cres.getStatus());

      Session session =
         repositoryService.getRepository(rName).login(new CredentialsImpl("root", "exo".toCharArray()), wsName);
      assertNotNull(session);
      assertNotNull(session.getRootNode());

      session.logout();
   }

   public void testRemoveWorkspace() throws Exception
   {
      String wsName = "ws_over_rest_2";
      String rName = "repo_over_rest";
      Session session =
         repositoryService.getRepository(rName).login(new CredentialsImpl("root", "exo".toCharArray()), wsName);
      assertNotNull(session);
      assertNotNull(session.getRootNode());

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();

      ContainerRequestUserRole creq =
         new ContainerRequestUserRole("POST", new URI(REST_REPOSITORY_SERVICE_PATH
            + RestRepositoryService.Constants.OperationType.REMOVE_WORKSPACE + "/" + rName + "/" + wsName + "/false/"),
            new URI(""), null, new InputHeadersMap(headers));

      ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
      ContainerResponse cres = new ContainerResponse(responseWriter);
      handler.handleRequest(creq, cres);

      assertEquals(409, cres.getStatus());

      //remove with prepare close sessions
      creq =
         new ContainerRequestUserRole("POST", new URI(REST_REPOSITORY_SERVICE_PATH
            + RestRepositoryService.Constants.OperationType.REMOVE_WORKSPACE + "/" + rName + "/" + wsName + "/true/"),
            new URI(""), null, new InputHeadersMap(headers));

      responseWriter = new ByteArrayContainerResponseWriter();
      cres = new ContainerResponse(responseWriter);
      handler.handleRequest(creq, cres);

      assertEquals(200, cres.getStatus());

      try
      {
         repositoryService.getRepository(rName).login(new CredentialsImpl("root", "exo".toCharArray()), wsName);
         fail("The workspace /" + rName + "/" + wsName + "should be removed. ");
      }
      catch (NoSuchWorkspaceException e)
      {
         //ok.
      }
   }

   public void testRemoveRepository() throws Exception
   {
      ManageableRepository repository = helper.createRepository(container, true, null);

      String wsName = repository.getConfiguration().getSystemWorkspaceName();
      String repoName = repository.getConfiguration().getName();

      Session session =
         repositoryService.getRepository(repoName).login(new CredentialsImpl("root", "exo".toCharArray()), wsName);
      assertNotNull(session);
      assertNotNull(session.getRootNode());

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();

      ContainerRequestUserRole creq =
         new ContainerRequestUserRole("GET", new URI(REST_REPOSITORY_SERVICE_PATH
            + RestRepositoryService.Constants.OperationType.REMOVE_REPOSITORY + "/" + repoName + "/false/"),
            new URI(""), null, new InputHeadersMap(headers));

      ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
      ContainerResponse cres = new ContainerResponse(responseWriter);
      handler.handleRequest(creq, cres);

      assertEquals(409, cres.getStatus());

      //remove with prepare close sessions
      creq =
         new ContainerRequestUserRole("GET", new URI(REST_REPOSITORY_SERVICE_PATH
            + RestRepositoryService.Constants.OperationType.REMOVE_REPOSITORY + "/" + repoName + "/true/"),
            new URI(""), null, new InputHeadersMap(headers));

      responseWriter = new ByteArrayContainerResponseWriter();
      cres = new ContainerResponse(responseWriter);
      handler.handleRequest(creq, cres);

      assertEquals(200, cres.getStatus());

      try
      {
         repositoryService.getRepository(repoName);
         fail("The repository /" + repoName + "should be removed. ");
      }
      catch (RepositoryException e)
      {
         //ok.
      }
   }

   /**
    * Will be created the Object from JSON binary data.
    * 
    * @param cl
    *          Class
    * @param data
    *          binary data (JSON)
    * @return Object
    * @throws Exception
    *           will be generated Exception
    */
   private Object getObject(Class cl, byte[] data) throws Exception
   {
      JsonHandler jsonHandler = new JsonDefaultHandler();
      JsonParser jsonParser = new JsonParserImpl();
      InputStream inputStream = new ByteArrayInputStream(data);
      jsonParser.parse(inputStream, jsonHandler);
      JsonValue jsonValue = jsonHandler.getJsonObject();

      return new BeanBuilder().createObject(cl, jsonValue);
   }

   protected WorkspaceEntry makeWorkspaceEntry(WorkspaceEntry defWEntry, String repoNmae, String wsName,
      String sourceName, boolean multiDb)
   {
      WorkspaceEntry ws1back = new WorkspaceEntry();
      ws1back.setName(wsName);
      ws1back.setUniqueName(repoNmae + "_" + wsName);

      ws1back.setAccessManager(defWEntry.getAccessManager());
      ws1back.setAutoInitializedRootNt(defWEntry.getAutoInitializedRootNt());
      ws1back.setAutoInitPermissions(defWEntry.getAutoInitPermissions());
      ws1back.setCache(defWEntry.getCache());
      ws1back.setLockManager(defWEntry.getLockManager());

      // Indexer
      ArrayList qParams = new ArrayList();
      qParams.add(new SimpleParameterEntry("indexDir", "target" + File.separator + skipInvalidCharacters(wsName)));
      QueryHandlerEntry qEntry = new QueryHandlerEntry(defWEntry.getQueryHandler().getType(), qParams);

      ws1back.setQueryHandler(qEntry);

      ArrayList params = new ArrayList();
      for (Iterator i = defWEntry.getContainer().getParameters().iterator(); i.hasNext();)
      {
         SimpleParameterEntry p = (SimpleParameterEntry)i.next();
         SimpleParameterEntry newp = new SimpleParameterEntry(p.getName(), p.getValue());

         if (newp.getName().equals("source-name"))
            newp.setValue(sourceName);
         else if (newp.getName().equals("swap-directory"))
            newp.setValue("target/temp/swap/" + skipInvalidCharacters(wsName));
         else if (newp.getName().equals("multi-db"))
            newp.setValue(Boolean.toString(multiDb));

         params.add(newp);
      }

      ContainerEntry ce = new ContainerEntry(defWEntry.getContainer().getType(), params);
      ws1back.setContainer(ce);

      return ws1back;
   }

   private String skipInvalidCharacters(String s)
   {
      if (File.separator.equals("\\"))
      {
         return s.replaceAll("[:,?]", "_");
      }
      else
      {
         return s;
      }

   }
}
