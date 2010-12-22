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
package org.exoplatform.services.jcr.impl.core;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.jcr.util.TesterConfigurationHelper;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NoSuchNodeTypeException;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: TestRepositoryManagement.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestRepositoryManagement extends JcrImplBaseTest
{

   public static int BINDED_DS_COUNT = 100;

   private static boolean isBinded = false;

   private final int lastDS = 0;

   private WorkspaceEntry wsEntry;

   private boolean isDefaultWsMultiDb;

   private final TesterConfigurationHelper helper;

   public TestRepositoryManagement()
   {
      super();
      this.helper = TesterConfigurationHelper.getInstence();
   }

   // TODO remove this method
   public void createDafaultRepository(String repoName, String defaultWs) throws Exception
   {

      RepositoryEntry repositoryEntry = new RepositoryEntry();

      repositoryEntry.setName(repoName);
      repositoryEntry.setSessionTimeOut(3600000);
      repositoryEntry.setAuthenticationPolicy("org.exoplatform.services.jcr.impl.core.access.JAASAuthenticator");
      repositoryEntry.setSecurityDomain("exo-domain");
      repositoryEntry.setSystemWorkspaceName(defaultWs);
      repositoryEntry.setDefaultWorkspaceName(defaultWs);

      WorkspaceEntry workspaceEntry =
         helper.getNewWs(defaultWs, isDefaultWsMultiDb, null, "target/temp/values/" + IdGenerator.generate(), wsEntry
            .getContainer());

      repositoryEntry.addWorkspace(workspaceEntry);

      WorkspaceEntry secondWs =
         helper.getNewWs(defaultWs + IdGenerator.generate(), isDefaultWsMultiDb, isDefaultWsMultiDb ? null
            : workspaceEntry.getContainer().getParameterValue("sourceName"), "target/temp/values/"
            + IdGenerator.generate(), wsEntry.getContainer());

      repositoryEntry.addWorkspace(secondWs);

      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);

      service.createRepository(repositoryEntry);
   }

   @Override
   public void setUp() throws Exception
   {
      // bindDs();
      super.setUp();
      wsEntry = (WorkspaceEntry)session.getContainer().getComponentInstanceOfType(WorkspaceEntry.class);
      if ("true".equals(wsEntry.getContainer().getParameterValue("multi-db")))
      {
         isDefaultWsMultiDb = true;
      }

   }

   public void testAddNewRepository() throws Exception
   {
      RepositoryEntry repositoryEntry = new RepositoryEntry();

      repositoryEntry.setName("repo4TestCreateRepository");
      repositoryEntry.setSessionTimeOut(3600000);
      repositoryEntry.setAuthenticationPolicy("org.exoplatform.services.jcr.impl.core.access.JAASAuthenticator");
      repositoryEntry.setSecurityDomain("exo-domain");
      repositoryEntry.setSystemWorkspaceName("ws4TestCreateRepository");
      repositoryEntry.setDefaultWorkspaceName("ws4TestCreateRepository");
      //
      // List params = new ArrayList();
      // params.add(new SimpleParameterEntry("sourceName", getNewDs()));
      // params.add(new SimpleParameterEntry("db-type", "generic"));
      // params.add(new SimpleParameterEntry("multi-db", "false"));
      // params.add(new SimpleParameterEntry("update-storage", "true"));
      // params.add(new SimpleParameterEntry("max-buffer-size", "204800"));
      // params.add(new SimpleParameterEntry("swap-directory",
      // "target/temp/swap/ws"));
      //
      // ContainerEntry containerEntry = new
      // ContainerEntry("org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer",
      // (ArrayList) params);
      // containerEntry.setParameters(params);
      //
      // WorkspaceEntry workspaceEntry = new
      // WorkspaceEntry("ws4TestCreateRepository", "nt:unstructured");
      //    

      WorkspaceEntry workspaceEntry =
         helper.getNewWs("ws4TestCreateRepository", isDefaultWsMultiDb, wsEntry.getContainer().getParameterValue(
            JDBCWorkspaceDataContainer.SOURCE_NAME), "target/temp/values/" + IdGenerator.generate(), wsEntry
            .getContainer());

      repositoryEntry.addWorkspace(workspaceEntry);

      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);

      service.createRepository(repositoryEntry);

      RepositoryImpl newRtepository = (RepositoryImpl)service.getRepository("repo4TestCreateRepository");
      try
      {

         Session sess = newRtepository.getSystemSession(workspaceEntry.getName());

         Node root = sess.getRootNode();
         assertNotNull(root);

         assertNotNull(root.getNode("jcr:system"));

         assertNotNull(root.getNode("jcr:system/exo:namespaces"));
         root.addNode("testNode");
         sess.save();
         Node testNode = root.getNode("testNode");
         assertNotNull(testNode);
         sess.logout();
      }
      catch (RepositoryException e)
      {
         fail();
      }
      RepositoryImpl defRep = (RepositoryImpl)service.getDefaultRepository();
      Session sess = null;
      try
      {

         sess = defRep.getSystemSession();

         Node root = sess.getRootNode();
         assertNotNull(root);

         assertNotNull(root.getNode("jcr:system"));

         assertNotNull(root.getNode("jcr:system/exo:namespaces"));
         // root.addNode("testNode");
         // sess.save();
         Node testNode = root.getNode("testNode");

      }
      catch (PathNotFoundException e)
      {
         // Ok
      }
      finally
      {
         if (sess != null)
            sess.logout();
      }
      service.removeRepository("repo4TestCreateRepository");
   }

   public void testAddNewRepositoryWithSameName() throws Exception
   {

      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);

      RepositoryEntry repositoryEntry = new RepositoryEntry();

      repositoryEntry.setName(service.getConfig().getDefaultRepositoryName());
      repositoryEntry.setSessionTimeOut(3600000);
      repositoryEntry.setAuthenticationPolicy("org.exoplatform.services.jcr.impl.core.access.JAASAuthenticator");
      repositoryEntry.setSecurityDomain("exo-domain");
      repositoryEntry.setSystemWorkspaceName("ws4testAddNewRepositoryWithSameName");
      repositoryEntry.setDefaultWorkspaceName("ws4testAddNewRepositoryWithSameName");

      // List params = new ArrayList();
      // params.add(new SimpleParameterEntry("sourceName", getNewDs()));
      // params.add(new SimpleParameterEntry("db-type", "generic"));
      // params.add(new SimpleParameterEntry("multi-db", "false"));
      // params.add(new SimpleParameterEntry("update-storage", "true"));
      // params.add(new SimpleParameterEntry("max-buffer-size", "204800"));
      // params.add(new SimpleParameterEntry("swap-directory",
      // "target/temp/swap/ws"));
      //
      // ContainerEntry containerEntry = new
      // ContainerEntry("org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer",
      // (ArrayList) params);
      // containerEntry.setParameters(params);

      // WorkspaceEntry workspaceEntry = new
      // WorkspaceEntry("ws4TestCreateRepository", "nt:unstructured");
      // workspaceEntry.setContainer(containerEntry);

      WorkspaceEntry workspaceEntry =
         helper.getNewWs("ws4testAddNewRepositoryWithSameName", isDefaultWsMultiDb, null, "target/temp/values/"
            + IdGenerator.generate(), wsEntry.getContainer());
      repositoryEntry.addWorkspace(workspaceEntry);

      try
      {
         service.createRepository(repositoryEntry);
         fail();
      }
      catch (RepositoryConfigurationException e)
      {
         // ok
      }

   }

   public void testCanRemove() throws Exception
   {
      RepositoryEntry repositoryEntry = new RepositoryEntry();

      repositoryEntry.setName("repo4testCanRemove");
      repositoryEntry.setSessionTimeOut(3600000);
      repositoryEntry.setAuthenticationPolicy("org.exoplatform.services.jcr.impl.core.access.JAASAuthenticator");
      repositoryEntry.setSecurityDomain("exo-domain");
      repositoryEntry.setSystemWorkspaceName("ws4testCanRemove");
      repositoryEntry.setDefaultWorkspaceName("ws4testCanRemove");

      WorkspaceEntry workspaceEntry =
         helper.getNewWs("ws4testCanRemove", isDefaultWsMultiDb, null, "target/temp/values/" + IdGenerator.generate(),
            wsEntry.getContainer());

      repositoryEntry.addWorkspace(workspaceEntry);

      WorkspaceEntry secondWs =
         helper.getNewWs("ws4testCanRemove2", isDefaultWsMultiDb, isDefaultWsMultiDb ? null : wsEntry.getContainer()
            .getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME), "target/temp/values/" + IdGenerator.generate(),
            wsEntry.getContainer());
      // WorkspaceEntry secondWs = TestWorkspaceManagement.getNewWs(null, false,
      // dsName,null);
      repositoryEntry.addWorkspace(secondWs);

      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);

      service.createRepository(repositoryEntry);

      RepositoryImpl newRtepository = (RepositoryImpl)service.getRepository("repo4testCanRemove");
      try
      {

         Session sess = newRtepository.getSystemSession();

         Node root = sess.getRootNode();
         assertNotNull(root);
         sess.logout();

         Session sess2 = newRtepository.getSystemSession(secondWs.getName());

         Node root2 = sess2.getRootNode();
         assertNotNull(root2);
         assertFalse(service.canRemoveRepository("repo4testCanRemove"));
         sess2.logout();
         assertTrue(service.canRemoveRepository("repo4testCanRemove"));
         service.removeRepository("repo4testCanRemove");
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail();
      }

   }

   public void testInitNameSpaces() throws Exception
   {
      // Test initialization of common node types
      String REPONAME = "testInitNameSpaces";
      String WSNAME = "ws4" + REPONAME;

      createDafaultRepository(REPONAME, WSNAME);
      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);

      RepositoryImpl newRepository = (RepositoryImpl)service.getRepository(REPONAME);
      Session sess = newRepository.getSystemSession(WSNAME);

      assertEquals("http://www.apache.org/jackrabbit/test", sess.getNamespaceURI("test"));
      assertEquals("http://www.exoplatform.org/jcr/test/1.0", sess.getNamespaceURI("exojcrtest"));

      try
      {
         sess.getNamespaceURI("blabla");
         fail();
      }
      catch (NamespaceException e)
      {
         // ok;
      }

   }

   public void testInitNodeTypes() throws Exception
   {

      // Test initialization of common node types
      String REPONAME = "testInitNodeTypesCommonRepository";
      String WSNAME = "ws4testInitNodeTypes";

      createDafaultRepository(REPONAME, WSNAME);
      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);

      RepositoryImpl newRepository = (RepositoryImpl)service.getRepository(REPONAME);
      Session sess = newRepository.getSystemSession(WSNAME);
      Node newRoot = sess.getRootNode();
      try
      {

         assertNotNull(newRoot);

         assertNotNull(newRoot.getNode("jcr:system"));

         assertNotNull(newRoot.getNode("jcr:system/exo:namespaces"));
         newRoot.addNode("testNode", "exojcrtest:sub1");
         sess.save();
         Node testNode = newRoot.getNode("testNode");
         assertNotNull(testNode);
      }
      catch (RepositoryException e)
      {
         fail();
      }
      try
      {
         newRoot.addNode("testNode2", "exojcrtest:sub2");
         fail();
      }
      catch (NoSuchNodeTypeException e)
      {
         // ok
      }
      try
      {
         newRoot.addNode("testNode2", "exojcrtest:test2");
         fail();
      }
      catch (NoSuchNodeTypeException e)
      {
         // ok
      }

      sess.logout();

      assertTrue(service.canRemoveRepository(REPONAME));

      service.removeRepository(REPONAME);

      // test initialization node types only for one repository
      REPONAME = "testInitNodeTypesRepository";
      createDafaultRepository(REPONAME, WSNAME);

      newRepository = (RepositoryImpl)service.getRepository(REPONAME);
      sess = newRepository.getSystemSession(WSNAME);
      newRoot = sess.getRootNode();

      try
      {

         assertNotNull(newRoot);

         assertNotNull(newRoot.getNode("jcr:system"));

         assertNotNull(newRoot.getNode("jcr:system/exo:namespaces"));
         newRoot.addNode("testNode2", "exojcrtest:sub2");
         sess.save();
         Node testNode = newRoot.getNode("testNode2");
         assertNotNull(testNode);
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail();
      }
      try
      {
         newRoot.addNode("testNode3", "exojcrtest:test2");
         fail();
      }
      catch (NoSuchNodeTypeException e)
      {
         // ok
      }
      sess.logout();

      assertTrue(service.canRemoveRepository(REPONAME));

      service.removeRepository(REPONAME);

      // test initialization node types only for one repository
      REPONAME = "testInitNodeTypesRepositoryTest2";
      createDafaultRepository(REPONAME, WSNAME);

      newRepository = (RepositoryImpl)service.getRepository(REPONAME);
      sess = newRepository.getSystemSession(WSNAME);
      newRoot = sess.getRootNode();

      try
      {

         assertNotNull(newRoot);

         assertNotNull(newRoot.getNode("jcr:system"));

         assertNotNull(newRoot.getNode("jcr:system/exo:namespaces"));
         newRoot.addNode("testNode4", "exojcrtest:test2");
         sess.save();
         Node testNode = newRoot.getNode("testNode4");
         assertNotNull(testNode);
      }
      catch (RepositoryException e)
      {
         fail();
      }
      try
      {
         newRoot.addNode("testNode5", "exojcrtest:sub2");
         fail();
      }
      catch (NoSuchNodeTypeException e)
      {
         // ok
      }
      sess.logout();

      assertTrue(service.canRemoveRepository(REPONAME));

      service.removeRepository(REPONAME);

   }

   public void testRemove() throws Exception
   {
      RepositoryEntry repositoryEntry = new RepositoryEntry();

      repositoryEntry.setName("repo4testRemove");
      repositoryEntry.setSessionTimeOut(3600000);
      repositoryEntry.setAuthenticationPolicy("org.exoplatform.services.jcr.impl.core.access.JAASAuthenticator");
      repositoryEntry.setSecurityDomain("exo-domain");
      repositoryEntry.setSystemWorkspaceName("ws4testRemove");
      repositoryEntry.setDefaultWorkspaceName("ws4testRemove");

      WorkspaceEntry workspaceEntry =
         helper.getNewWs("ws4testRemove", isDefaultWsMultiDb, null, "target/temp/values/" + IdGenerator.generate(),
            wsEntry.getContainer());

      repositoryEntry.addWorkspace(workspaceEntry);

      WorkspaceEntry secondWs =
         helper.getNewWs("ws4testRemove2", isDefaultWsMultiDb, isDefaultWsMultiDb ? null : wsEntry.getContainer()
            .getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME), "target/temp/values/" + IdGenerator.generate(),
            wsEntry.getContainer());
      repositoryEntry.addWorkspace(secondWs);

      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);

      service.createRepository(repositoryEntry);

      RepositoryImpl newRtepository = (RepositoryImpl)service.getRepository("repo4testRemove");
      assertTrue(service.canRemoveRepository("repo4testRemove"));

      service.removeRepository("repo4testRemove");
   }

   public void testRemoveOtherThread() throws Exception
   {
      RepositoryEntry repositoryEntry = new RepositoryEntry();

      repositoryEntry.setName("repo4RemoveOtherThread");
      repositoryEntry.setSessionTimeOut(3600000);
      repositoryEntry.setAuthenticationPolicy("org.exoplatform.services.jcr.impl.core.access.JAASAuthenticator");
      repositoryEntry.setSecurityDomain("exo-domain");
      repositoryEntry.setSystemWorkspaceName("ws4RemoveOtherThread");
      repositoryEntry.setDefaultWorkspaceName("ws4RemoveOtherThread");

      WorkspaceEntry workspaceEntry =
         helper.getNewWs("ws4RemoveOtherThread", isDefaultWsMultiDb, null, "target/temp/values/"
            + IdGenerator.generate(), wsEntry.getContainer());

      repositoryEntry.addWorkspace(workspaceEntry);

      WorkspaceEntry secondWs =
         helper.getNewWs("ws4RemoveOtherThread2", isDefaultWsMultiDb, isDefaultWsMultiDb ? null : wsEntry
            .getContainer().getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME), "target/temp/values/"
            + IdGenerator.generate(), wsEntry.getContainer());

      repositoryEntry.addWorkspace(secondWs);

      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);

      service.createRepository(repositoryEntry);

      // RepositoryImpl newRepository = (RepositoryImpl)
      // service.getRepository("repo4RemoveOtherThread");
      assertTrue(service.canRemoveRepository("repo4RemoveOtherThread"));

      RepositoryRemover remover = new RepositoryRemover("repo4RemoveOtherThread", service);
      remover.start();
      Thread.sleep(1000 * 10);// 10 sec
      try
      {
         service.getRepository("repo4RemoveOtherThread");
         fail();
      }
      catch (RepositoryException e)
      {
         // ok
      }
   }

   private class RepositoryRemover extends Thread
   {
      private final String repoName;

      private final RepositoryService service;

      RepositoryRemover(String repoName, RepositoryService service)
      {
         this.repoName = repoName;
         this.service = service;

      }

      @Override
      public void run()
      {
         try
         {
            if (service.canRemoveRepository(repoName))
               service.removeRepository(repoName);
         }
         catch (RepositoryException e)
         {
            e.printStackTrace();
         }
      }
   }
}
