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

import org.exoplatform.services.jcr.config.ContainerEntry;
import org.exoplatform.services.jcr.config.QueryHandlerEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.SimpleParameterEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.ext.BaseStandaloneTest;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

/**
 * Created by The eXo Platform SAS Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 04.02.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: AbstractBackupTestCase.java 760 2008-02-07 15:08:07Z pnedonosko $
 */
public class AbstractBackupTestCase extends BaseStandaloneTest
{

   protected SessionImpl ws1Session;

   protected Node ws1TestRoot;

   protected SessionImpl ws2Session;

   protected BackupManager backup;

   class LogFilter implements FileFilter
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

      // RepositoryContainer rcontainer = (RepositoryContainer)
      // container.getComponentInstanceOfType(RepositoryContainer.class);
      backup = (BackupManager)container.getComponentInstanceOfType(BackupManager.class);

      if (backup == null)
         throw new Exception("There are no BackupManagerImpl in configuration");

      // ws1
      
      for (String wsName : repository.getWorkspaceNames())
      {
         if ("ws1".equals(wsName))
         {
            ws1Session = (SessionImpl) repository.login(credentials, "ws1");
            ws1TestRoot = ws1Session.getRootNode().addNode("backupTest");
            ws1Session.save();
            addContent(ws1TestRoot, 1, 10, 1);
            
         }
         else
         {
            SessionImpl ws = (SessionImpl) repository.login(credentials, wsName);
            Node wsTestRoot = ws.getRootNode().addNode("backupTest");
            ws.save();
            addContent(wsTestRoot, 1, 10, 1);
         }
      }

      // ws2
      ws2Session = (SessionImpl)repository.login(credentials, "ws2");
   }

   @Override
   protected void tearDown() throws Exception
   {

      for (String wsName : repository.getWorkspaceNames())
      {
         if ("ws1".equals(wsName))
         {
            ws1Session = (SessionImpl) repository.login(credentials, "ws1");
            ws1Session.getRootNode().getNode("backupTest").remove();
            ws1Session.save();
         }
         else
         {
            SessionImpl ws = (SessionImpl)repository.login(credentials, wsName);
            ws.getRootNode().getNode("backupTest").remove();
            ws.save();
         }
      }

      super.tearDown();
   }

   protected WorkspaceEntry makeWorkspaceEntry(String name, String sourceName)
   {
      WorkspaceEntry ws1e = (WorkspaceEntry)ws1Session.getContainer().getComponentInstanceOfType(WorkspaceEntry.class);

      WorkspaceEntry ws1back = new WorkspaceEntry();
      ws1back.setName(name);
      // RepositoryContainer rcontainer = (RepositoryContainer)
      // container.getComponentInstanceOfType(RepositoryContainer.class);
      ws1back.setUniqueName(((RepositoryImpl)ws1Session.getRepository()).getName() + "_" + ws1back.getName()); // EXOMAN

      ws1back.setAccessManager(ws1e.getAccessManager());
      ws1back.setCache(ws1e.getCache());
      ws1back.setContainer(ws1e.getContainer());
      ws1back.setLockManager(ws1e.getLockManager());
      ws1back.setInitializer(ws1e.getInitializer());

      // Indexer
      ArrayList qParams = new ArrayList();
      // qParams.add(new SimpleParameterEntry("indexDir", "target" + File.separator+ "temp" +
      // File.separator +"index" + name));
      qParams.add(new SimpleParameterEntry("indexDir", "target" + File.separator + name + System.currentTimeMillis()));
      QueryHandlerEntry qEntry =
         new QueryHandlerEntry("org.exoplatform.services.jcr.impl.core.query.lucene.SearchIndex", qParams);

      ws1back.setQueryHandler(qEntry); // EXOMAN

      ArrayList params = new ArrayList();
      for (Iterator i = ws1back.getContainer().getParameters().iterator(); i.hasNext();)
      {
         SimpleParameterEntry p = (SimpleParameterEntry)i.next();
         SimpleParameterEntry newp = new SimpleParameterEntry(p.getName(), p.getValue());

         if (newp.getName().equals("source-name"))
            newp.setValue(sourceName);
         else if (newp.getName().equals("swap-directory"))
            newp.setValue("target/temp/swap/" + name + System.currentTimeMillis());

         params.add(newp);
      }

      ContainerEntry ce =
         new ContainerEntry("org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer", params);
      ws1back.setContainer(ce);

      return ws1back;
   }
   
   protected RepositoryEntry makeRepositoryEntry(String repoName, RepositoryEntry baseRepoEntry, String sourceName, Map<String, String> workspaceMapping)
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
      
      newRepositoryEntry.setSystemWorkspaceName(workspaceMapping == null ? baseRepoEntry.getSystemWorkspaceName() : workspaceMapping.get(baseRepoEntry.getSystemWorkspaceName()));
      newRepositoryEntry.setAccessControl(baseRepoEntry.getAccessControl());
      newRepositoryEntry.setAuthenticationPolicy(baseRepoEntry.getAuthenticationPolicy());
      newRepositoryEntry.setDefaultWorkspaceName(workspaceMapping == null ? baseRepoEntry.getDefaultWorkspaceName() : workspaceMapping.get(baseRepoEntry.getDefaultWorkspaceName()));
      newRepositoryEntry.setName(repoName);
      newRepositoryEntry.setSecurityDomain(baseRepoEntry.getSecurityDomain());
      newRepositoryEntry.setSessionTimeOut(baseRepoEntry.getSessionTimeOut());
      
      newRepositoryEntry.setWorkspaceEntries(wsEntries);
      
      return newRepositoryEntry;
   }
   
   protected WorkspaceEntry makeWorkspaceEntry(WorkspaceEntry baseWorkspaceEntry, String wsName, String repoName, String sourceName)
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
      ArrayList qParams = new ArrayList();
      qParams.add(new SimpleParameterEntry("indexDir", "target" + File.separator + repoName + "_" + wsName));
      QueryHandlerEntry qEntry =
         new QueryHandlerEntry("org.exoplatform.services.jcr.impl.core.query.lucene.SearchIndex", qParams);

      ws1back.setQueryHandler(qEntry); 
      
      ArrayList params = new ArrayList();
      for (Iterator i = ws1back.getContainer().getParameters().iterator(); i.hasNext();)
      {
         SimpleParameterEntry p = (SimpleParameterEntry)i.next();
         SimpleParameterEntry newp = new SimpleParameterEntry(p.getName(), p.getValue());

         if (newp.getName().equals("source-name"))
            newp.setValue(sourceName);
         else if (newp.getName().equals("swap-directory"))
            newp.setValue("target/temp/swap/"  + repoName + "_" + wsName);
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
      RepositoryEntry re = (RepositoryEntry)ws1Session.getContainer().getComponentInstanceOfType(RepositoryEntry.class);
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
            back1 = (SessionImpl)repository.login(credentials, ws1back.getName());
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

   public void testname() throws Exception
   {
      assertEquals(true, true);
   }
}
