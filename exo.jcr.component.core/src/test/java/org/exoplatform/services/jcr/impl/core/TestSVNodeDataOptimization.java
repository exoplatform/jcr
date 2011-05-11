/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.core;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.config.CacheEntry;
import org.exoplatform.services.jcr.config.ContainerEntry;
import org.exoplatform.services.jcr.config.LockManagerEntry;
import org.exoplatform.services.jcr.config.LockPersisterEntry;
import org.exoplatform.services.jcr.config.QueryHandlerEntry;
import org.exoplatform.services.jcr.config.SimpleParameterEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.config.WorkspaceInitializerEntry;
import org.exoplatform.services.jcr.datamodel.NodeData;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date: 02.11.2009
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: TestSVNodeDataOptimization.java 3381 2010-11-02 15:51:38Z tolusha $
 */
public class TestSVNodeDataOptimization
   extends JcrImplBaseTest
{
   
   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      SessionImpl ses = (SessionImpl) repository.login(credentials, "ws1");
      if (ses != null)
      {
         try
         {
            ses.refresh(false);
            Node rootNode = ses.getRootNode();
            if (rootNode.hasNodes())
            {
               // clean test root
               for (NodeIterator children = rootNode.getNodes(); children.hasNext();)
               {
                  Node node = children.nextNode();
                  if (!node.getPath().startsWith("/jcr:system"))
                  {
                     // log.info("DELETing ------------- "+node.getPath());
                     node.remove();
                  }
               }
               ses.save();
            }
         }
         catch (Exception e)
         {
            log.error("tearDown() ERROR " + getClass().getName() + "." + getName() + " " + e, e);
         }
         finally
         {
            ses.logout();
         }
      }
   }

   @Override
   protected void tearDown() throws Exception
   {
      SessionImpl ses = (SessionImpl) repository.login(credentials, "ws1");
      if (ses != null)
      {
         try
         {
            ses.refresh(false);
            Node rootNode = ses.getRootNode();
            if (rootNode.hasNodes())
            {
               // clean test root
               for (NodeIterator children = rootNode.getNodes(); children.hasNext();)
               {
                  Node node = children.nextNode();
                  if (!node.getPath().startsWith("/jcr:system"))
                  {
                     // log.info("DELETing ------------- "+node.getPath());
                     node.remove();
                  }
               }
               ses.save();
            }
         }
         catch (Exception e)
         {
            log.error("tearDown() ERROR " + getClass().getName() + "." + getName() + " " + e, e);
         }
         finally
         {
            ses.logout();
         }
      }
      super.tearDown();
   }

   public void testSVNodeData() throws Exception
   {

      //ADD content
      SessionImpl ses = (SessionImpl) repository.login(credentials, "ws1");

      //Add node with sub name sibling
      Node nodeSNS = ses.getRootNode().addNode("node_with_sns");
      for (int i = 0; i < 100; i++)
      {
         nodeSNS.addNode("node_sns_");
      }

      //Add node without sub name sibling
      Node nodeWOSNS = ses.getRootNode().addNode("node_without_sns");
      for (int i = 0; i < 100; i++)
      {
         nodeSNS.addNode("node_" + i);
      }

      ses.save();

      // Cereate backup
      File backup = File.createTempFile("full-backup", ".xml");
      backup.deleteOnExit();

      ses.exportWorkspaceSystemView(new FileOutputStream(backup), false, false);

      // restore to ws1_restored
      WorkspaceEntry ws1_restore =
               makeWorkspaceEntry("ws1_restored", isMultiDB(session) ? "jdbcjcr2export3" : "jdbcjcr", backup, ses);
      repository.configWorkspace(ws1_restore);
      repository.createWorkspace(ws1_restore.getName());

      // check
      SessionImpl backupSession = (SessionImpl) repository.login(credentials, "ws1_restored");

      assertNotNull(backupSession);

      checkEquals(ses, backupSession);
   }

   private void checkEquals(SessionImpl expected, SessionImpl actual) throws Exception
   {
      NodeImpl srcNode = (NodeImpl) expected.getRootNode();
      NodeImpl destNode = (NodeImpl) actual.getRootNode();

      checkNodeEquals(srcNode, destNode);
   }

   private void checkNodeEquals(NodeImpl src, NodeImpl dest) throws Exception
   {
      assertTrue(dest.equals(src));
      assertEquals(src.getIndex(), dest.getIndex());
      assertEquals(((NodeData) src.getData()).getOrderNumber(), ((NodeData) dest.getData()).getOrderNumber());

      NodeIterator srcIterator = src.getNodes();
      NodeIterator destIterator = dest.getNodes();

      assertEquals(srcIterator.getSize(), destIterator.getSize());

      while (srcIterator.hasNext())
         checkNodeEquals((NodeImpl) srcIterator.nextNode(), (NodeImpl) destIterator.nextNode());
   }

   private WorkspaceEntry makeWorkspaceEntry(String name, String sourceName, File sysViewFile, SessionImpl ses)
   {
      WorkspaceEntry ws1e = (WorkspaceEntry) ses.getContainer().getComponentInstanceOfType(WorkspaceEntry.class);

      WorkspaceEntry ws1back = new WorkspaceEntry();
      ws1back.setName(name);
      ws1back.setUniqueName(((RepositoryImpl) ses.getRepository()).getName() + "_" + ws1back.getName());

      ws1back.setAccessManager(ws1e.getAccessManager());
      ws1back.setAutoInitializedRootNt(ws1e.getAutoInitializedRootNt());
      ws1back.setAutoInitPermissions(ws1e.getAutoInitPermissions());
      
      CacheEntry cacheConfig = new CacheEntry(new ArrayList(ws1e.getCache().getParameters()));
      if (cacheConfig.getParameterValue("jbosscache-cluster-name", null) != null)
      {
         // Ensure that the cluster name is unique
         cacheConfig.putParameterValue("jbosscache-cluster-name", "JCR-cluster-" + ws1back.getUniqueName());
      }
      cacheConfig.setEnabled(ws1e.getCache().getEnabled());
      cacheConfig.setType(ws1e.getCache().getType());
      ws1back.setCache(cacheConfig);
      ws1back.setContainer(ws1e.getContainer());
      LockManagerEntry lockManagerConfig = new LockManagerEntry();
      lockManagerConfig.setParameters(new ArrayList(ws1e.getLockManager().getParameters()));
      if (lockManagerConfig.getParameterValue("jbosscache-cluster-name", null) != null)
      {
         // Ensure that the cluster name is unique
         lockManagerConfig.putParameterValue("jbosscache-cluster-name", "JCR-cluster-locks-" + ws1back.getUniqueName());
      }
      lockManagerConfig.setType(ws1e.getLockManager().getType());
      lockManagerConfig.setTimeout(ws1e.getLockManager().getTimeout());
      if (ws1e.getLockManager().getPersister() != null)
      {
         LockPersisterEntry LockPersisterConfig = new LockPersisterEntry();
         LockPersisterConfig.setParameters(new ArrayList(ws1e.getLockManager().getPersister().getParameters()));
         LockPersisterConfig.setType(ws1e.getLockManager().getPersister().getType());
         lockManagerConfig.setPersister(LockPersisterConfig);         
      }
      ws1back.setLockManager(lockManagerConfig);

      // Initializer
      WorkspaceInitializerEntry wiEntry = new WorkspaceInitializerEntry();
      wiEntry.setType(SysViewWorkspaceInitializer.class.getCanonicalName());

      List<SimpleParameterEntry> wieParams = new ArrayList<SimpleParameterEntry>();
      wieParams
               .add(new SimpleParameterEntry(SysViewWorkspaceInitializer.RESTORE_PATH_PARAMETER, sysViewFile.getPath()));

      wiEntry.setParameters(wieParams);

      ws1back.setInitializer(wiEntry);

      // Indexer
      ArrayList qParams = new ArrayList();
      qParams.add(new SimpleParameterEntry("indexDir", "target" + File.separator + name));
      QueryHandlerEntry qEntry =
               new QueryHandlerEntry("org.exoplatform.services.jcr.impl.core.query.lucene.SearchIndex", qParams);

      ws1back.setQueryHandler(qEntry);

      ArrayList params = new ArrayList();
      for (Iterator i = ws1back.getContainer().getParameters().iterator(); i.hasNext();)
      {
         SimpleParameterEntry p = (SimpleParameterEntry) i.next();
         SimpleParameterEntry newp = new SimpleParameterEntry(p.getName(), p.getValue());

         if (isMultiDB(ses) && newp.getName().equals("source-name"))
            newp.setValue(sourceName);
         else if (newp.getName().equals("swap-directory"))
            newp.setValue("target/temp/swap/" + name);
         else if (isMultiDB(ses) && newp.getName().equals("dialect"))
            newp.setValue("hsqldb");

         params.add(newp);
      }

      ContainerEntry ce =
               new ContainerEntry("org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer", params);
      ws1back.setContainer(ce);

      return ws1back;
   }

   private boolean isMultiDB(SessionImpl session)
   {
      WorkspaceEntry ws1e = (WorkspaceEntry) session.getContainer().getComponentInstanceOfType(WorkspaceEntry.class);

      for (Iterator i = ws1e.getContainer().getParameters().iterator(); i.hasNext();)
      {
         SimpleParameterEntry p = (SimpleParameterEntry) i.next();
         SimpleParameterEntry newp = new SimpleParameterEntry(p.getName(), p.getValue());

         if (newp.getName().equals("multi-db"))
            return Boolean.valueOf(newp.getValue());
      }

      throw new RuntimeException("Can not get property 'multi-db' in configuration on workspace '" + ws1e.getName()
               + "'");
   }

}
