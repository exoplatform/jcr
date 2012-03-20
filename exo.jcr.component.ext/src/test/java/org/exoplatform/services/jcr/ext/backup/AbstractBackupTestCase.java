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
import org.exoplatform.services.jcr.config.QueryHandlerParams;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.ValueStorageEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.ext.BaseStandaloneTest;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.app.ThreadLocalSessionProviderService;
import org.exoplatform.services.jcr.ext.backup.impl.BackupManagerImpl;
import org.exoplatform.services.jcr.ext.backup.impl.JobRepositoryRestore;
import org.exoplatform.services.jcr.ext.backup.impl.JobWorkspaceRestore;
import org.exoplatform.services.jcr.ext.backup.server.HTTPBackupAgent;
import org.exoplatform.services.jcr.ext.backup.server.HTTPBackupAgentTest;
import org.exoplatform.services.jcr.ext.backup.server.bean.response.DetailedInfo;
import org.exoplatform.services.jcr.ext.backup.server.bean.response.ShortInfo;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.clean.rdbms.DBCleanService;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.SessionRegistry;
import org.exoplatform.services.jcr.impl.core.query.SystemSearchManager;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig.DatabaseStructureType;
import org.exoplatform.services.jcr.impl.storage.value.fs.FileValueStorage;
import org.exoplatform.services.jcr.impl.util.io.DirectoryHelper;
import org.exoplatform.services.jcr.util.TesterConfigurationHelper;
import org.exoplatform.services.rest.ContainerResponseWriter;
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
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.jcr.ItemExistsException;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Created by The eXo Platform SAS Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 04.02.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: AbstractBackupTestCase.java 760 2008-02-07 15:08:07Z pnedonosko $
 */
public abstract class AbstractBackupTestCase extends BaseStandaloneTest
{

   protected TesterConfigurationHelper helper = TesterConfigurationHelper.getInstance();

   protected File blob;

   protected ExtendedBackupManager backup;

   protected File backupDir;

   protected RequestHandler handler;

   /**
    * {@inheritDoc}
    */
   public void setUp() throws Exception
   {
      super.setUp();// this

      backup = getBackupManager();
      blob = createBLOBTempFile(300);

      backupDir = new File("target/temp/backup/" + System.currentTimeMillis());
      backupDir.mkdirs();

      handler = (RequestHandler)container.getComponentInstanceOfType(RequestHandler.class);

      SessionProviderService sessionProviderService =
         (SessionProviderService)container.getComponentInstanceOfType(ThreadLocalSessionProviderService.class);
      assertNotNull(sessionProviderService);
      sessionProviderService.setSessionProvider(null, new SessionProvider(new ConversationState(new Identity("root"))));
   }

   /**
    * {@inheritDoc}
    */
   protected void tearDown() throws Exception
   {
      super.tearDown();

      blob.delete();
   }

   protected abstract ExtendedBackupManager getBackupManager();

   protected ExtendedBackupManager getJCRBackupManager()
   {
      if (backup == null)
      {
         InitParams initParams = new InitParams();
         PropertiesParam pps = new PropertiesParam();
         pps.setProperty(BackupManagerImpl.FULL_BACKUP_TYPE,
            "org.exoplatform.services.jcr.ext.backup.impl.fs.FullBackupJob");
         pps.setProperty(BackupManagerImpl.INCREMENTAL_BACKUP_TYPE,
            "org.exoplatform.services.jcr.ext.backup.impl.fs.IncrementalBackupJob");
         pps.setProperty(BackupManagerImpl.BACKUP_DIR, "target/backup");
         pps.setProperty(BackupManagerImpl.DEFAULT_INCREMENTAL_JOB_PERIOD, "3600");

         initParams.put(BackupManagerImpl.BACKUP_PROPERTIES, pps);

         BackupManagerImpl backup = new BackupManagerImpl(initParams, repositoryService);
         backup.start();

         return backup;
      }

      return backup;
   }

   protected ExtendedBackupManager getRDBMSBackupManager()
   {
      if (backup == null)
      {
         InitParams initParams = new InitParams();
         PropertiesParam pps = new PropertiesParam();
         pps.setProperty(BackupManagerImpl.FULL_BACKUP_TYPE,
            "org.exoplatform.services.jcr.ext.backup.impl.rdbms.FullBackupJob");
         pps.setProperty(BackupManagerImpl.INCREMENTAL_BACKUP_TYPE,
            "org.exoplatform.services.jcr.ext.backup.impl.fs.IncrementalBackupJob");
         pps.setProperty(BackupManagerImpl.BACKUP_DIR, "target/backup");
         pps.setProperty(BackupManagerImpl.DEFAULT_INCREMENTAL_JOB_PERIOD, "3600");

         initParams.put(BackupManagerImpl.BACKUP_PROPERTIES, pps);

         BackupManagerImpl backup = new BackupManagerImpl(initParams, repositoryService);
         backup.start();

         return backup;
      }

      return backup;
   }

   protected void addContent(Node node, int startIndex, int stopIndex, long sleepTime) throws ValueFormatException,
            VersionException, LockException, ConstraintViolationException, ItemExistsException, PathNotFoundException,
            RepositoryException, InterruptedException
   {
      for (int i = startIndex; i <= stopIndex; i++)
      {
         node.addNode("node_" + i).setProperty("exo:data", "property-" + i);
         Thread.sleep(sleepTime);
         if (i % 10 == 0)
            node.save(); // log here via listener
      }
      node.save();
   }

   protected void waitTime(Date time) throws InterruptedException
   {
      while (Calendar.getInstance().getTime().before(time))
      {
         Thread.yield();
         Thread.sleep(50);
      }
      Thread.sleep(250);
   }

   protected void removeWorkspaceFully(String repositoryName, String workspaceName) throws Exception
   {
      // get current workspace configuration
      WorkspaceEntry wEntry = null;;
      for (WorkspaceEntry entry : repositoryService.getRepository(repositoryName).getConfiguration()
         .getWorkspaceEntries())
      {
         if (entry.getName().equals(workspaceName))
         {
            wEntry = entry;
            break;
         }
      }

      if (wEntry == null)
      {
         throw new WorkspaceRestoreException("Workspace " + workspaceName + " did not found in current repository "
            + repositoryName + " configuration");
      }

      boolean isSystem =
         repositoryService.getRepository(repositoryName).getConfiguration().getSystemWorkspaceName()
            .equals(wEntry.getName());

      // remove workspace 
      forceCloseSession(repositoryName, wEntry.getName());
      repositoryService.getRepository(repositoryName).removeWorkspace(wEntry.getName());

      // clean db
      DBCleanService.cleanWorkspaceData(wEntry);

      // clean value storage
      if (wEntry.getContainer().getValueStorages() != null)
      {
         for (ValueStorageEntry valueStorage : wEntry.getContainer().getValueStorages())
         {
            DirectoryHelper.removeDirectory(new File(valueStorage.getParameterValue(FileValueStorage.PATH)));
         }
      }

      // clean index
      if (wEntry.getQueryHandler() != null)
      {
         DirectoryHelper.removeDirectory(new File(wEntry.getQueryHandler().getParameterValue(
            QueryHandlerParams.PARAM_INDEX_DIR, null)));
         if (isSystem)
         {
            DirectoryHelper.removeDirectory(new File(wEntry.getQueryHandler().getParameterValue(
               QueryHandlerParams.PARAM_INDEX_DIR,
               null)
               + "_" + SystemSearchManager.INDEX_DIR_SUFFIX));
         }
      }
   }

   protected void removeWorkspaceFullySingleDB(String repositoryName, String workspaceName) throws Exception
   {
      // get current workspace configuration
      WorkspaceEntry wEntry = null;;
      for (WorkspaceEntry entry : repositoryService.getRepository(repositoryName).getConfiguration()
         .getWorkspaceEntries())
      {
         if (entry.getName().equals(workspaceName))
         {
            wEntry = entry;
            break;
         }
      }

      if (wEntry == null)
      {
         throw new WorkspaceRestoreException("Workspace " + workspaceName + " did not found in current repository "
            + repositoryName + " configuration");
      }

      boolean isSystem =
         repositoryService.getRepository(repositoryName).getConfiguration().getSystemWorkspaceName()
            .equals(wEntry.getName());

      //close all session
      forceCloseSession(repositoryName, wEntry.getName());

      repositoryService.getRepository(repositoryName).removeWorkspace(wEntry.getName());

      DBCleanService.cleanWorkspaceData(wEntry);

      if (wEntry.getContainer().getValueStorages() != null)
      {
         for (ValueStorageEntry valueStorage : wEntry.getContainer().getValueStorages())
         {
            DirectoryHelper.removeDirectory(new File(valueStorage.getParameterValue(FileValueStorage.PATH)));
         }
      }

      if (wEntry.getQueryHandler() != null)
      {
         DirectoryHelper.removeDirectory(new File(wEntry.getQueryHandler().getParameterValue(
            QueryHandlerParams.PARAM_INDEX_DIR, null)));
         if (isSystem)
         {
            DirectoryHelper.removeDirectory(new File(wEntry.getQueryHandler().getParameterValue(
               QueryHandlerParams.PARAM_INDEX_DIR,
               null)
               + "_" + SystemSearchManager.INDEX_DIR_SUFFIX));
         }
      }
   }

   protected void removeRepositoryFully(String repositoryName) throws Exception
   {
      // get current repository configuration
      RepositoryEntry repositoryEntry = repositoryService.getConfig().getRepositoryConfiguration(repositoryName);

      if (repositoryEntry == null)
      {
         throw new RepositoryRestoreExeption("Current repository configuration " + repositoryName + " did not found");
      }

      //Create local copy of WorkspaceEntry for all workspaces
      ArrayList<WorkspaceEntry> workspaceList = new ArrayList<WorkspaceEntry>();
      workspaceList.addAll(repositoryEntry.getWorkspaceEntries());

      //close all session
      for (WorkspaceEntry wEntry : workspaceList)
      {
         forceCloseSession(repositoryEntry.getName(), wEntry.getName());
      }

      String systemWorkspaceName =
         repositoryService.getRepository(repositoryName).getConfiguration().getSystemWorkspaceName();

      //remove repository
      repositoryService.removeRepository(repositoryEntry.getName());

      // clean data
      for (WorkspaceEntry wEntry : workspaceList)
      {
         DBCleanService.cleanWorkspaceData(wEntry);

         if (wEntry.getContainer().getValueStorages() != null)
         {
            for (ValueStorageEntry valueStorage : wEntry.getContainer().getValueStorages())
            {
               DirectoryHelper.removeDirectory(new File(valueStorage.getParameterValue(FileValueStorage.PATH)));
            }
         }

         boolean isSystem = systemWorkspaceName.equals(wEntry.getName());

         if (wEntry.getQueryHandler() != null)
         {
            DirectoryHelper.removeDirectory(new File(wEntry.getQueryHandler().getParameterValue(
               QueryHandlerParams.PARAM_INDEX_DIR, null)));
            if (isSystem)
            {
               DirectoryHelper.removeDirectory(new File(wEntry.getQueryHandler().getParameterValue(
                  QueryHandlerParams.PARAM_INDEX_DIR, null)
                  + "_" + SystemSearchManager.INDEX_DIR_SUFFIX));
            }
         }
      }
   }

   /**
    * forceCloseSession. Close sessions on specific workspace.
    * 
    * @param repositoryName
    *          repository name
    * @param workspaceName
    *          workspace name
    * @return int return the how many sessions was closed
    * @throws RepositoryConfigurationException
    *           will be generate RepositoryConfigurationException
    * @throws RepositoryException
    *           will be generate RepositoryException
    */
   private int forceCloseSession(String repositoryName, String workspaceName) throws RepositoryException,
            RepositoryConfigurationException
   {
      ManageableRepository mr = repositoryService.getRepository(repositoryName);
      WorkspaceContainerFacade wc = mr.getWorkspaceContainer(workspaceName);

      SessionRegistry sessionRegistry = (SessionRegistry) wc.getComponent(SessionRegistry.class);

      return sessionRegistry.closeSessions(workspaceName);
   }

   public void waitEndOfBackup(BackupChain bch) throws Exception
   {
      while (bch.getFullBackupState() != BackupChain.FINISHED)
      {
         Thread.yield();
         Thread.sleep(50);
      }
   }

   public void waitEndOfBackup(RepositoryBackupChain bch) throws Exception
   {
      while (bch.getState() != RepositoryBackupChain.FINISHED
         && bch.getState() != RepositoryBackupChain.FULL_BACKUP_FINISHED_INCREMENTAL_BACKUP_WORKING)
      {
         Thread.yield();
         Thread.sleep(50);
      }
   }

   public void waitEndOfRestore(String repositoryName) throws Exception
   {
      while (backup.getLastRepositoryRestore(repositoryName).getStateRestore() != JobRepositoryRestore.REPOSITORY_RESTORE_SUCCESSFUL
         && backup.getLastRepositoryRestore(repositoryName).getStateRestore() != JobRepositoryRestore.REPOSITORY_RESTORE_FAIL)
      {
         Thread.sleep(50);
      }
   }

   public void waitEndOfRestore(String repositoryName, String workspaceName) throws Exception
   {
      while (backup.getLastRestore(repositoryName, workspaceName).getStateRestore() != JobWorkspaceRestore.RESTORE_SUCCESSFUL
         && backup.getLastRestore(repositoryName, workspaceName).getStateRestore() != JobWorkspaceRestore.RESTORE_FAIL)
      {
         Thread.sleep(50);
      }
   }

   public void addIncrementalConent(ManageableRepository repository, String wsName) throws Exception
   {
      SessionImpl session = (SessionImpl)repository.login(credentials, wsName);
      Node rootNode = session.getRootNode().addNode("testIncremental");

      // add some changes which will be logged in incremental log
      rootNode.addNode("node1").setProperty("prop1", "value1");
      rootNode.addNode("node2").setProperty("prop2", new FileInputStream(blob));
      rootNode.addNode("node3").addMixin("mix:lockable");
      session.save();
   }

   public void addConent(ManageableRepository repository, String wsName) throws Exception
   {
      SessionImpl session = (SessionImpl)repository.login(credentials, wsName);
      Node rootNode = session.getRootNode().addNode("test");

      // add some changes which will be logged in incremental log
      rootNode.addNode("node1").setProperty("prop1", "value1");
      rootNode.addNode("node2").setProperty("prop2", new FileInputStream(blob));
      rootNode.addNode("node3").addMixin("mix:lockable");
      session.save();
   }

   public void checkConent(ManageableRepository repository, String wsName) throws Exception
   {
      SessionImpl session = (SessionImpl)repository.login(credentials, wsName);

      Node rootNode = session.getRootNode().getNode("test");
      assertEquals(rootNode.getNode("node1").getProperty("prop1").getString(), "value1");

      InputStream in = rootNode.getNode("node2").getProperty("prop2").getStream();
      try
      {
         compareStream(new FileInputStream(blob), in);
      }
      finally
      {
         in.close();
      }
   }

   public void checkIncrementalConent(ManageableRepository repository, String wsName) throws Exception
   {
      SessionImpl session = (SessionImpl)repository.login(credentials, wsName);

      Node rootNode = session.getRootNode().getNode("testIncremental");
      assertEquals(rootNode.getNode("node1").getProperty("prop1").getString(), "value1");

      InputStream in = rootNode.getNode("node2").getProperty("prop2").getStream();
      try
      {
         compareStream(new FileInputStream(blob), in);
      }
      finally
      {
         in.close();
      }
   }

   protected RepoInfo createRepositoryAndGetSession() throws Exception
   {
      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.MULTI, null);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(DatabaseStructureType.MULTI, null);
      helper.addWorkspace(repository, wsEntry);

      RepoInfo rInfo = new RepoInfo();
      rInfo.rName = repository.getConfiguration().getName();
      rInfo.wsName = wsEntry.getName();
      rInfo.sysWsName = repository.getConfiguration().getSystemWorkspaceName();
      rInfo.session = repositoryService.getRepository(rInfo.rName).login(credentials, rInfo.wsName);

      return rInfo;
   }

   /**
    * Class for tests purpose only. To have ability to access to {@link ContainerResponseWriter}.
    */
   protected class TesterContainerResponce extends ContainerResponse
   {

      public ByteArrayContainerResponseWriter responseWriter;

      public TesterContainerResponce(ByteArrayContainerResponseWriter responseWriter)
      {
         super(responseWriter);
         this.responseWriter = responseWriter;
      }
   }

   /**
    * Aggregate info about newly created repository.
    */
   protected class RepoInfo
   {
      public String rName;

      public String wsName;

      public String sysWsName;

      public Session session;
   }

   protected boolean isRepositoryExists(String rName)
   {
      return isWorkspaceExists(rName, null);
   }

   protected boolean isWorkspaceExists(String rName, String wsName)
   {
      ManageableRepository repository = null;
      try
      {
         repository = repositoryService.getRepository(rName);
      }
      catch (RepositoryException e)
      {
         return false;
      }
      catch (RepositoryConfigurationException e)
      {
         return false;
      }

      try
      {
         repository.login(credentials, wsName);
      }
      catch (LoginException e)
      {
         return false;
      }
      catch (NoSuchWorkspaceException e)
      {
         return false;
      }
      catch (RepositoryException e)
      {
         return false;
      }

      return true;
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
   protected Object getObject(Class cl, byte[] data) throws Exception
   {
      JsonHandler jsonHandler = new JsonDefaultHandler();
      JsonParser jsonParser = new JsonParserImpl();
      InputStream inputStream = new ByteArrayInputStream(data);
      jsonParser.parse(inputStream, jsonHandler);
      JsonValue jsonValue = jsonHandler.getJsonObject();

      return new BeanBuilder().createObject(cl, jsonValue);
   }

   protected void waitWorkspaceRestore(String repoName, String wsName) throws Exception
   {
      while (true)
      {
         TesterContainerResponce cres =
            makeGetRequest(new URI(HTTPBackupAgentTest.HTTP_BACKUP_AGENT_PATH
               + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_WS + "/" + repoName + "/" + wsName));

         assertEquals(200, cres.getStatus());

         DetailedInfo info = (DetailedInfo)getObject(DetailedInfo.class, cres.responseWriter.getBody());

         if (info.getState().intValue() == JobWorkspaceRestore.RESTORE_SUCCESSFUL
            || info.getState().intValue() == JobWorkspaceRestore.RESTORE_FAIL)
         {
            break;
         }

         Thread.sleep(500);
      }
   }

   protected void waitRepositoryRestore(String repoName) throws Exception
   {
      while (true)
      {
         TesterContainerResponce cres =
            makeGetRequest(new URI(HTTPBackupAgentTest.HTTP_BACKUP_AGENT_PATH
               + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_REPOSITORY + "/" + repoName));

         assertEquals(200, cres.getStatus());

         DetailedInfo info = (DetailedInfo)getObject(DetailedInfo.class, cres.responseWriter.getBody());

         if (info.getState().intValue() == JobRepositoryRestore.REPOSITORY_RESTORE_SUCCESSFUL
            || info.getState().intValue() == JobRepositoryRestore.REPOSITORY_RESTORE_FAIL)
         {
            break;
         }

         Thread.sleep(500);
      }
   }

   protected ShortInfo getBackupInfo(List<ShortInfo> list, String rName)
   {
      for (ShortInfo info : list)
      {
         if (info.getRepositoryName().equals(rName))
         {
            return info;
         }
      }

      return null;
   }

   protected BackupChain backupWorkspace(RepoInfo rInfo) throws Exception
   {
      BackupConfig config = new BackupConfig();
      config.setRepository(rInfo.rName);
      config.setWorkspace(rInfo.wsName);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backupDir);

      BackupChain bch = backup.startBackup(config);
      waitEndOfBackup(bch);

      return bch;
   }

   protected RepositoryBackupChain backupRepository(RepoInfo rInfo) throws Exception
   {
      RepositoryBackupConfig config = new RepositoryBackupConfig();
      config.setRepository(rInfo.rName);
      config.setBackupType(BackupManager.FULL_BACKUP_ONLY);
      config.setBackupDir(backupDir);

      RepositoryBackupChain bch = backup.startBackup(config);
      waitEndOfBackup(bch);

      return bch;
   }

   protected TesterContainerResponce makeGetRequest(URI uri) throws Exception
   {
      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();

      ContainerRequestUserRole creq =
         new ContainerRequestUserRole("GET", uri, new URI(""), null, new InputHeadersMap(headers));

      ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
      TesterContainerResponce cres = new TesterContainerResponce(responseWriter);
      handler.handleRequest(creq, cres);

      return cres;
   }

   protected TesterContainerResponce makePostRequest(URI uri, Object object) throws Exception
   {
      JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
      JsonValue json = generatorImpl.createJsonObject(object);

      MultivaluedMap<String, String> headers = new MultivaluedMapImpl();

      headers.putSingle("Content-Type", "application/json; charset=UTF-8");
      ContainerRequestUserRole creq =
         new ContainerRequestUserRole("POST", uri, new URI(""), new ByteArrayInputStream(json.toString().getBytes(
            "UTF-8")), new InputHeadersMap(headers));

      ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
      TesterContainerResponce cres = new TesterContainerResponce(responseWriter);
      handler.handleRequest(creq, cres);

      return cres;
   }

}
