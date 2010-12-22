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
package org.exoplatform.services.jcr.impl.util.jdbc;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.util.jdbc.cleaner.DBCleanerService;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.jcr.util.TesterConfigurationHelper;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.naming.InitialContext;
import javax.sql.DataSource;

/**
 * This test uses "testdbcleaner" datasource to create own test repository with workspace.
 * So, please, check test-configuration.xml or test-configuration-sjdbc.xml does such datasource binded.
 * 
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TestRemoveWorkspace.java 111 2008-11-11 11:11:11Z serg $
 */
public class TestDBCleanerService extends JcrImplBaseTest
{
   private final static String DS_NAME = "testdbcleaner";

   private final TesterConfigurationHelper helper;

   private WorkspaceEntry wsEntry;

   public TestDBCleanerService()
   {
      super();
      this.helper = TesterConfigurationHelper.getInstence();
   }

   @SuppressWarnings("deprecation")
   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      wsEntry = (WorkspaceEntry)session.getContainer().getComponentInstanceOfType(WorkspaceEntry.class);
   }

   @Override
   public void tearDown() throws Exception
   {
      // drop any table  
      DataSource ds = (DataSource)new InitialContext().lookup(DS_NAME);
      Connection conn = ds.getConnection();
      Statement statement = conn.createStatement();
      try
      {
         statement.executeUpdate("drop table JCR_SREF");
      }
      catch (SQLException e)
      {
      }
      try
      {
         statement.executeUpdate("drop table JCR_SVALUE");
      }
      catch (SQLException e)
      {
      }
      try
      {
         statement.executeUpdate("drop table JCR_SITEM");
      }
      catch (SQLException e)
      {
      }
      try
      {
         statement.executeUpdate("drop table JCR_MREF");
      }
      catch (SQLException e)
      {
      }
      try
      {
         statement.executeUpdate("drop table JCR_MVALUE");
      }
      catch (SQLException e)
      {
      }
      try
      {
         statement.executeUpdate("drop table JCR_MITEM");
      }
      catch (SQLException e)
      {
      }
      try
      {
         statement.executeUpdate("drop table JCR_MCONTAINER");
      }
      catch (SQLException e)
      {
      }
      try
      {
         statement.executeUpdate("drop table JCR_SCONTAINER");
      }
      catch (SQLException e)
      {
      }
      if (statement != null)
      {
         try
         {
            statement.close();
         }
         catch (SQLException e)
         {
         }
      }
      super.tearDown();
   }

   public void testRemoveRepositoryMultiDB() throws Exception
   {
      String repositoryName = "repoTestRemoveMulti";

      RepositoryEntry repositoryEntry = createMultiDB(repositoryName);

      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);
      RepositoryImpl newRepository = (RepositoryImpl)service.getRepository(repositoryName);
      assertTrue(service.canRemoveRepository(repositoryName));

      String wsName = repositoryEntry.getWorkspaceEntries().get(0).getName();
      SessionImpl sess = newRepository.getSystemSession(wsName);

      // add nodes to workspaces and check it via datasource      
      NodeImpl node = (NodeImpl)sess.getRootNode().addNode("testNode");
      String id = node.getData().getIdentifier();
      sess.save();
      sess.logout();

      DataSource ds = (DataSource)new InitialContext().lookup(DS_NAME);
      Connection conn = ds.getConnection();
      Statement statement = conn.createStatement();
      ResultSet res = statement.executeQuery("select * from JCR_MITEM where ID='" + id + "'");
      assertTrue(res.next());

      // remove repository;
      new DBCleanerService().cleanRepositoryData(repositoryEntry);

      // check - does JCR_SITEM become empty
      try
      {
         res = statement.executeQuery("select * from JCR_MITEM where ID='" + id + "'");
         fail();
      }
      catch (SQLException e)
      {
         //ok
      }
      statement.close();

      service.removeRepository(repositoryName);
   }

   public void testRemoveRepositorySingleDB() throws Exception
   {
      String repositoryName = "repoTestRemoveSingle";

      RepositoryEntry repositoryEntry = createSingleDB(repositoryName);

      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);
      RepositoryImpl newRepository = (RepositoryImpl)service.getRepository(repositoryName);
      assertTrue(service.canRemoveRepository(repositoryName));

      String wsName = repositoryEntry.getWorkspaceEntries().get(0).getName();
      SessionImpl sess = newRepository.getSystemSession(wsName);

      // now add nodes to workspaces and check it via datasource      
      NodeImpl node = (NodeImpl)sess.getRootNode().addNode("testNode");
      String id = node.getData().getIdentifier();
      sess.save();
      sess.logout();

      DataSource ds = (DataSource)new InitialContext().lookup(DS_NAME);
      Connection conn = ds.getConnection();
      Statement statement = conn.createStatement();
      ResultSet res = statement.executeQuery("select * from JCR_SITEM where ID='" + wsName + id + "'");
      assertTrue(res.next());

      // remove repository content
      new DBCleanerService().cleanRepositoryData(repositoryEntry);

      // check - does JCR_SITEM become empty
      res = statement.executeQuery("select * from JCR_SITEM where ID='" + wsName + id + "'");
      assertFalse(res.next());
      statement.close();

      service.removeRepository(repositoryName);
   }

   public void testRemoveWorkspaceMultiDB() throws Exception
   {
      String repositoryName = "repoTestRemoveMulti";

      RepositoryEntry repositoryEntry = createMultiDB(repositoryName);

      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);
      RepositoryImpl newRepository = (RepositoryImpl)service.getRepository(repositoryName);
      assertTrue(service.canRemoveRepository(repositoryName));

      String wsName = repositoryEntry.getWorkspaceEntries().get(0).getName();
      SessionImpl sess = newRepository.getSystemSession(wsName);

      // now add nodes to workspaces and check it via datasource      
      NodeImpl node = (NodeImpl)sess.getRootNode().addNode("testNode");
      String id = node.getData().getIdentifier();
      sess.save();
      sess.logout();

      DataSource ds = (DataSource)new InitialContext().lookup(DS_NAME);
      Connection conn = ds.getConnection();
      Statement statement = conn.createStatement();
      ResultSet res = statement.executeQuery("select * from JCR_MITEM where ID='" + id + "'");
      assertTrue(res.next());

      // remove workspace data from database
      new DBCleanerService().cleanWorkspaceData(repositoryEntry.getWorkspaceEntries().get(0));

      // check - does JCR_SITEM become empty
      try
      {
         res = statement.executeQuery("select * from JCR_MITEM where ID='" + id + "'");
         fail();
      }
      catch (SQLException e)
      {
         //ok
      }
      statement.close();

      service.removeRepository(repositoryName);
   }

   public void testRemoveWorkspaceSingleDB() throws Exception
   {
      String repositoryName = "repoTestRemoveSingle";

      RepositoryEntry repositoryEntry = createSingleDB(repositoryName);

      WorkspaceEntry workspaceEntry = repositoryEntry.getWorkspaceEntries().get(0);

      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);
      Session sess = ((RepositoryImpl)service.getRepository(repositoryName)).getSystemSession(workspaceEntry.getName());
      Node root2 = sess.getRootNode();
      assertNotNull(root2);

      NodeImpl n = (NodeImpl)root2.addNode("node1");
      assertTrue(root2.hasNode("node1"));

      String id = n.getData().getIdentifier();

      sess.save();

      n.setProperty("prop", "some value");
      n.setProperty("prop2", "some value two");
      sess.save();

      Node n2 = n.addNode("subnode");
      n2.setProperty("prop", "some value");
      n2.addNode("subnode1");
      n2.addNode("subnode2");
      n2.addNode("subnode2");
      sess.save();
      sess.logout();

      DataSource ds = (DataSource)new InitialContext().lookup(DS_NAME);
      Connection conn = ds.getConnection();
      Statement statement = conn.createStatement();
      ResultSet res =
         statement.executeQuery("select * from JCR_SITEM where ID='" + workspaceEntry.getName() + id + "'");
      assertTrue(res.next());

      // remove workspace data from database
      new DBCleanerService().cleanWorkspaceData(workspaceEntry);

      // check - does JCR_SITEM become empty
      res = statement.executeQuery("select * from JCR_SITEM where ID='" + workspaceEntry.getName() + id + "'");
      assertFalse(res.next());
      statement.close();

      service.removeRepository(repositoryName);
   }

   private RepositoryEntry createMultiDB(String repositoryName) throws Exception
   {
      RepositoryEntry repositoryEntry = new RepositoryEntry();

      repositoryEntry.setName(repositoryName);
      repositoryEntry.setSessionTimeOut(3600000);
      repositoryEntry.setAuthenticationPolicy("org.exoplatform.services.jcr.impl.core.access.JAASAuthenticator");
      repositoryEntry.setSecurityDomain("exo-domain");
      repositoryEntry.setSystemWorkspaceName(repositoryName + "ws");
      repositoryEntry.setDefaultWorkspaceName(repositoryName + "ws");

      WorkspaceEntry workspaceEntry =
         helper.getNewWs(repositoryName + "ws", true, DS_NAME, "target/temp/values/" + IdGenerator.generate(),
            wsEntry.getContainer(), false);

      repositoryEntry.addWorkspace(workspaceEntry);

      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);

      service.createRepository(repositoryEntry);
      return repositoryEntry;
   }

   private RepositoryEntry createSingleDB(String repositoryName) throws Exception
   {
      RepositoryEntry repositoryEntry = new RepositoryEntry();

      repositoryEntry.setName(repositoryName);
      repositoryEntry.setSessionTimeOut(3600000);
      repositoryEntry.setAuthenticationPolicy("org.exoplatform.services.jcr.impl.core.access.JAASAuthenticator");
      repositoryEntry.setSecurityDomain("exo-domain");
      repositoryEntry.setSystemWorkspaceName(repositoryName + "ws");
      repositoryEntry.setDefaultWorkspaceName(repositoryName + "ws");

      WorkspaceEntry workspaceEntry =
         helper.getNewWs(repositoryName + "ws", false, DS_NAME, "target/temp/values/" + IdGenerator.generate(),
            wsEntry.getContainer(), false);

      repositoryEntry.addWorkspace(workspaceEntry);

      WorkspaceEntry secondWs =
         helper.getNewWs(repositoryName + "ws2", false, DS_NAME, "target/temp/values/" + IdGenerator.generate(),
            wsEntry.getContainer(), false);
      repositoryEntry.addWorkspace(secondWs);

      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);

      service.createRepository(repositoryEntry);
      return repositoryEntry;
   }
}
