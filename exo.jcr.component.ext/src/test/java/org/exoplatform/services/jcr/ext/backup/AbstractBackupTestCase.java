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

import org.exoplatform.services.jcr.config.ContainerEntry;
import org.exoplatform.services.jcr.config.QueryHandlerEntry;
import org.exoplatform.services.jcr.config.QueryHandlerParams;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.SimpleParameterEntry;
import org.exoplatform.services.jcr.config.ValueStorageEntry;
import org.exoplatform.services.jcr.config.ValueStorageFilterEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.ext.BaseStandaloneTest;
import org.exoplatform.services.jcr.ext.backup.impl.IndexCleanHelper;
import org.exoplatform.services.jcr.ext.backup.impl.ValueStorageCleanHelper;
import org.exoplatform.services.jcr.impl.RepositoryServiceImpl;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.SessionRegistry;
import org.exoplatform.services.jcr.impl.util.jdbc.cleaner.DBCleanerService;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

/**
 * Created by The eXo Platform SAS Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 04.02.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: AbstractBackupTestCase.java 760 2008-02-07 15:08:07Z pnedonosko $
 */
public abstract class AbstractBackupTestCase
   extends BaseStandaloneTest
{

   protected SessionImpl ws1Session;

   protected Node ws1TestRoot;

   protected ExtendedBackupManager backup;

   protected String repositoryNameToBackup = "db8";

   protected String workspaceNameToBackup = "ws1";

   protected String dataSourceToWorkspaceRestore = "jdbcjcr_workspace_restore";

   protected String dataSourceToRepositoryRestore = "jdbcjcr_to_repository_restore";
   
   protected String dataSourceToRepositoryRestoreSingleDB = "jdbcjcr_to_repository_restore_singel_db";
   
   protected String repositoryNameToBackupSingleDB = "db7";

   protected String repositoryNameToRestore = "db8backup";

   protected String workspaceNameToRestore = "ws1backup";

   /**
    * Database cleaner.
    */
   private DBCleanerService dbCleanerService = new DBCleanerService();

   /**
    * Value storage cleaner.
    */
   private ValueStorageCleanHelper valueStorageCleanHelper = new ValueStorageCleanHelper();

   /**
    * Index storage cleaner.
    */
   private IndexCleanHelper indexCleanHelper = new IndexCleanHelper();

   class LogFilter
      implements FileFilter
   {

      public boolean accept(File pathname)
      {
         return pathname.getName().startsWith("backup-") && pathname.getName().endsWith(".xml");
      }
   }

   @Override
   public void setUp() throws Exception
   {
      super.setUp();// this

      backup = getBackupManager();

      if (backup == null)
         throw new Exception("There are no BackupManagerImpl in configuration");

      for (String wsName : getReposityToBackup().getWorkspaceNames())
      {
         SessionImpl ws = (SessionImpl) getReposityToBackup().login(credentials, wsName);
         Node wsTestRoot = ws.getRootNode().addNode("backupTest");
         ws.save();
         addContent(wsTestRoot, 1, 10, 1);

         if ("ws1".equals(wsName))
         {
            ws1Session = ws;
            ws1TestRoot = wsTestRoot;
         }
      }

      RepositoryImpl repositoryDB7 = (RepositoryImpl) repositoryService.getRepository(repositoryNameToBackupSingleDB);

      for (String wsName : repositoryDB7.getWorkspaceNames())
      {
         SessionImpl sessionWS = (SessionImpl) repositoryDB7.login(credentials, wsName);

         Node wsTestRoot = sessionWS.getRootNode().addNode("backupTest");
         sessionWS.getRootNode().save();
         addContent(wsTestRoot, 1, 10, 1);
         sessionWS.getRootNode().save();
      }

   }

   protected abstract ExtendedBackupManager getBackupManager();

   protected RepositoryImpl getReposityToBackup() throws RepositoryException, RepositoryConfigurationException
   {
      return (RepositoryImpl) repositoryService.getRepository(repositoryNameToBackup);
   }

   @Override
   protected void tearDown() throws Exception
   {

      for (String wsName : getReposityToBackup().getWorkspaceNames())
      {
         try
         {
            SessionImpl ws = (SessionImpl) getReposityToBackup().login(credentials, wsName);
            ws.getRootNode().getNode("backupTest").remove();
            ws.save();
         }
         catch (PathNotFoundException e)
         {
            //skip
         }
      }

      RepositoryImpl repositoryDB7 = (RepositoryImpl) repositoryService.getRepository(repositoryNameToBackupSingleDB);

      for (String wsName : repositoryDB7.getWorkspaceNames())
      {
         try
         {
            SessionImpl ws = (SessionImpl) repositoryDB7.login(credentials, wsName);
            ws.getRootNode().getNode("backupTest").remove();
            ws.save();
         }
         catch (PathNotFoundException e)
         {
            //skip
         }
      }

      for (String wsName : getReposityToBackup().getWorkspaceNames())
      {
         if (wsName.equals(workspaceNameToRestore))
         {
            removeWorkspaceFully(getReposityToBackup().getName(), workspaceNameToRestore);
         }
      }

      try 
      {
         repositoryService.getConfig().getRepositoryConfiguration(repositoryNameToRestore);
         removeRepositoryFully(repositoryNameToRestore);
      } 
      catch (RepositoryConfigurationException e)
      {
         //skip
      }

   }

   protected WorkspaceEntry makeWorkspaceEntry(String name, String sourceName)
   {
      WorkspaceEntry ws1e = (WorkspaceEntry) ws1Session.getContainer().getComponentInstanceOfType(WorkspaceEntry.class);

      WorkspaceEntry ws1back = new WorkspaceEntry();
      ws1back.setName(name);
      // RepositoryContainer rcontainer = (RepositoryContainer)
      // container.getComponentInstanceOfType(RepositoryContainer.class);
      ws1back.setUniqueName(((RepositoryImpl) ws1Session.getRepository()).getName() + "_" + ws1back.getName()); // EXOMAN

      ws1back.setAccessManager(ws1e.getAccessManager());
      ws1back.setCache(ws1e.getCache());
      ws1back.setContainer(ws1e.getContainer());
      ws1back.setLockManager(ws1e.getLockManager());
      ws1back.setInitializer(ws1e.getInitializer());

      // Indexer
      ArrayList qParams = new ArrayList();
      // qParams.add(new SimpleParameterEntry("indexDir", "target" + File.separator+ "temp" +
      // File.separator +"index" + name));
      qParams.add(new SimpleParameterEntry(QueryHandlerParams.PARAM_INDEX_DIR, "target" + File.separator + name
               + System.currentTimeMillis()));
      QueryHandlerEntry qEntry =
               new QueryHandlerEntry("org.exoplatform.services.jcr.impl.core.query.lucene.SearchIndex", qParams);

      ws1back.setQueryHandler(qEntry); // EXOMAN

      ArrayList params = new ArrayList();
      for (Iterator i = ws1back.getContainer().getParameters().iterator(); i.hasNext();)
      {
         SimpleParameterEntry p = (SimpleParameterEntry) i.next();
         SimpleParameterEntry newp = new SimpleParameterEntry(p.getName(), p.getValue());

         if (newp.getName().equals("source-name"))
            newp.setValue(sourceName);
         else if (newp.getName().equals("swap-directory"))
            newp.setValue("target/temp/swap/" + name + System.currentTimeMillis());

         params.add(newp);
      }

      //Value storage
      ArrayList<ValueStorageEntry> valueStorages = new ArrayList<ValueStorageEntry>();

      ValueStorageFilterEntry filterEntry = new ValueStorageFilterEntry();
      filterEntry.setPropertyType("Binary");

      ArrayList<ValueStorageFilterEntry> filterEntries = new ArrayList<ValueStorageFilterEntry>();
      filterEntries.add(filterEntry);
      

      ValueStorageEntry valueStorageEntry = new ValueStorageEntry();
      valueStorageEntry.setType("org.exoplatform.services.jcr.impl.storage.value.fs.TreeFileValueStorage");
      valueStorageEntry.setId("draft");
      valueStorageEntry.setFilters(filterEntries);
      
      ArrayList<SimpleParameterEntry> parameterEntries = new ArrayList<SimpleParameterEntry>();
      parameterEntries.add(new SimpleParameterEntry("path", "target/temp/values/" + ws1back.getName()));
      
      valueStorageEntry.setParameters(parameterEntries);

      valueStorages.add(valueStorageEntry);

      ContainerEntry ce =
               new ContainerEntry("org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer", params);
      ce.setValueStorages(valueStorages);

      ws1back.setContainer(ce);

      return ws1back;
   }

   protected RepositoryEntry makeRepositoryEntry(String repoName, RepositoryEntry baseRepoEntry, String sourceName,
            Map<String, String> workspaceMapping)
   {
      ArrayList<WorkspaceEntry> wsEntries = new ArrayList<WorkspaceEntry>();

      for (WorkspaceEntry wsEntry : baseRepoEntry.getWorkspaceEntries())
      {
         String newWorkspaceName = wsEntry.getName();
         if (workspaceMapping != null)
         {
            newWorkspaceName = workspaceMapping.get(wsEntry.getName());
         }

         WorkspaceEntry newWSEntry = makeWorkspaceEntry(wsEntry, newWorkspaceName, repoName, sourceName);

         wsEntries.add(newWSEntry);
      }

      RepositoryEntry newRepositoryEntry = new RepositoryEntry();

      newRepositoryEntry.setSystemWorkspaceName(workspaceMapping == null ? baseRepoEntry.getSystemWorkspaceName()
               : workspaceMapping.get(baseRepoEntry.getSystemWorkspaceName()));
      newRepositoryEntry.setAccessControl(baseRepoEntry.getAccessControl());
      newRepositoryEntry.setAuthenticationPolicy(baseRepoEntry.getAuthenticationPolicy());
      newRepositoryEntry.setDefaultWorkspaceName(workspaceMapping == null ? baseRepoEntry.getDefaultWorkspaceName()
               : workspaceMapping.get(baseRepoEntry.getDefaultWorkspaceName()));
      newRepositoryEntry.setName(repoName);
      newRepositoryEntry.setSecurityDomain(baseRepoEntry.getSecurityDomain());
      newRepositoryEntry.setSessionTimeOut(baseRepoEntry.getSessionTimeOut());

      newRepositoryEntry.setWorkspaceEntries(wsEntries);

      return newRepositoryEntry;
   }

   protected WorkspaceEntry makeWorkspaceEntry(WorkspaceEntry baseWorkspaceEntry, String wsName, String repoName,
            String sourceName)
   {
      WorkspaceEntry ws1back = new WorkspaceEntry();
      ws1back.setName(wsName);
      ws1back.setUniqueName(repoName + "_" + ws1back.getName());

      ws1back.setAccessManager(baseWorkspaceEntry.getAccessManager());
      ws1back.setCache(baseWorkspaceEntry.getCache());
      ws1back.setContainer(baseWorkspaceEntry.getContainer());
      ws1back.setLockManager(baseWorkspaceEntry.getLockManager());
      ws1back.setInitializer(baseWorkspaceEntry.getInitializer());

      // Indexer
      if (sourceName != null)
      {
         ArrayList qParams = new ArrayList();
         qParams.add(new SimpleParameterEntry(QueryHandlerParams.PARAM_INDEX_DIR, "target" + File.separator + repoName
                  + "_" + wsName));
         QueryHandlerEntry qEntry =
                  new QueryHandlerEntry("org.exoplatform.services.jcr.impl.core.query.lucene.SearchIndex", qParams);

         ws1back.setQueryHandler(qEntry);
      }
      else
      {
         ws1back.setQueryHandler(baseWorkspaceEntry.getQueryHandler());
      }

      ArrayList params = new ArrayList();
      for (Iterator i = ws1back.getContainer().getParameters().iterator(); i.hasNext();)
      {
         SimpleParameterEntry p = (SimpleParameterEntry) i.next();
         SimpleParameterEntry newp = new SimpleParameterEntry(p.getName(), p.getValue());

         if (newp.getName().equals("source-name"))
         {
            if (sourceName != null)
            {
               newp.setValue(sourceName);
            }
         }
         else if (newp.getName().equals("swap-directory"))
            newp.setValue("target/temp/swap/" + repoName + "_" + wsName);
         else if (newp.getName().equals("multi-db"))
            newp.setValue("false");

         params.add(newp);
      }

      ContainerEntry ce =
               new ContainerEntry("org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer", params);
      ws1back.setContainer(ce);

      return ws1back;
   }

   protected void restoreAndCheck(String workspaceName, String datasourceName, String backupLogFilePath, File backDir,
            int startIndex, int stopIndex) throws RepositoryConfigurationException, RepositoryException,
            BackupOperationException, BackupConfigurationException
   {
      // restore
      RepositoryEntry re =
               (RepositoryEntry) ws1Session.getContainer().getComponentInstanceOfType(RepositoryEntry.class);
      WorkspaceEntry ws1back = makeWorkspaceEntry(workspaceName, datasourceName);

      repository.configWorkspace(ws1back);

      File backLog = new File(backupLogFilePath);
      if (backLog.exists())
      {
         BackupChainLog bchLog = new BackupChainLog(backLog);
         backup.restore(bchLog, re.getName(), ws1back);

         // check
         SessionImpl back1 = null;
         try
         {
            back1 = (SessionImpl) repository.login(credentials, ws1back.getName());
            Node ws1backTestRoot = back1.getRootNode().getNode("backupTest");
            for (int i = startIndex; i < stopIndex; i++)
            {
               assertEquals("Restored content should be same", "property-" + i, ws1backTestRoot.getNode("node_" + i)
                        .getProperty("exo:data").getString());
            }
         }
         catch (Exception e)
         {
            e.printStackTrace();
            fail(e.getMessage());
         }
         finally
         {
            if (back1 != null)
               back1.logout();
         }
      }
      else
         fail("There are no backup files in " + backDir.getAbsolutePath());
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
               repositoryService.getRepository(repositoryName).getConfiguration().getSystemWorkspaceName().equals(
                        wEntry.getName());

      //close all session
      forceCloseSession(repositoryName, wEntry.getName());

      repositoryService.getRepository(repositoryName).removeWorkspace(wEntry.getName());

      //clean database
      dbCleanerService.cleanWorkspaceData(wEntry);

      //clean index
      indexCleanHelper.removeWorkspaceIndex(wEntry, isSystem);

      //clean value storage
      valueStorageCleanHelper.removeWorkspaceValueStorage(wEntry);
   }

   protected void removeRepositoryFully(String repositoryName) throws Exception
   {
      // get current repository configuration
      RepositoryEntry repositoryEntry = repositoryService.getConfig().getRepositoryConfiguration(repositoryName);

      if (repositoryEntry == null)
      {
         throw new RepositoryRestoreExeption("Current repository configuration " + repositoryName + " did not found");
      }

      boolean isDefault =
               repositoryService.getDefaultRepository().getConfiguration().getName().equals(repositoryEntry.getName());

      //Create local copy of WorkspaceEntry for all workspaces
      ArrayList<WorkspaceEntry> workspaceList = new ArrayList<WorkspaceEntry>();
      workspaceList.addAll(repositoryEntry.getWorkspaceEntries());

      //close all session
      for (WorkspaceEntry wEntry : workspaceList)
      {
         forceCloseSession(repositoryEntry.getName(), wEntry.getName());
      }

      //remove repository
      if (isDefault)
      {
         ((RepositoryServiceImpl) repositoryService).removeDefaultRepository();
      }
      else
      {
         repositoryService.removeRepository(repositoryEntry.getName());
      }

      //clean database
      RepositoryEntry re = new RepositoryEntry();
      re.setWorkspaceEntries(workspaceList);
      dbCleanerService.cleanRepositoryData(re);

      //clean index
      for (WorkspaceEntry wEntry : workspaceList)
      {
         indexCleanHelper.removeWorkspaceIndex(wEntry, repositoryEntry.getSystemWorkspaceName()
                  .equals(wEntry.getName()));
      }

      //clean value storage
      for (WorkspaceEntry wEntry : workspaceList)
      {
         valueStorageCleanHelper.removeWorkspaceValueStorage(wEntry);
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

}
