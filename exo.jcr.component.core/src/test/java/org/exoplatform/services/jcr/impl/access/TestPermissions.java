/*
 * Copyright (C) 2010 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.access;

import org.exoplatform.services.jcr.BaseStandaloneTest;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.access.SystemIdentity;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.version.VersionHistoryImpl;
import org.exoplatform.services.jcr.impl.core.version.VersionImpl;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.version.Version;

/**
 * @author <a href="anatoliy.bazko@exoplatform.org">Anatoliy Bazko</a>
 * @version $Id: TestRemoveSysteNode.java 111 2010-11-11 11:11:11Z tolusha $
 */
public class TestPermissions extends BaseStandaloneTest
{

   protected SessionImpl sessionMaryWS;

   protected SessionImpl sessionMaryWS1;

   protected SessionImpl sessionWS;

   protected SessionImpl sessionWS1;

   /**
    * {@inheritDoc}
    */
   @Override
   protected String getRepositoryName()
   {
      return null;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      Repository repository = repositoryService.getRepository("db2");
      Credentials credentials = new CredentialsImpl("admin", "admin".toCharArray());
      sessionWS = (SessionImpl)repository.login(credentials, "ws");
      sessionWS1 = (SessionImpl)repository.login(credentials, "ws1");

      repository = repositoryService.getRepository("db2");
      credentials = new CredentialsImpl("mary", "exo".toCharArray());
      sessionMaryWS = (SessionImpl)repository.login(credentials, "ws");
      sessionMaryWS1 = (SessionImpl)repository.login(credentials, "ws1");

      // add node with only read permission for mary
      NodeImpl node = (NodeImpl)sessionWS1.getRootNode().addNode("MARY-ReadOnly");
      node.addMixin("exo:privilegeable");
      node.addMixin("exo:owneable");
      node.setPermission("mary", new String[]{PermissionType.READ});
      node.setPermission("admin", PermissionType.ALL);
      node.removePermission(SystemIdentity.ANY);
      node.addNode("test");
      sessionWS1.save();

      sessionWS1.getRootNode().addNode("MARY-ReadWrite");
      sessionWS1.save();
   }

   @Override
   public void tearDown() throws Exception
   {
      List<SessionImpl> sessions = new ArrayList<SessionImpl>();

      sessions.add(sessionMaryWS);
      sessions.add(sessionMaryWS1);
      sessions.add(sessionWS);
      sessions.add(sessionWS1);

      for (SessionImpl session : sessions)
      {
         if (session != null)
         {
            Session sysSession = repository.getSystemSession(session.getWorkspace().getName());
            try
            {
               Node rootNode = sysSession.getRootNode();
               if (rootNode.hasNodes())
               {
                  // clean test root
                  for (NodeIterator children = rootNode.getNodes(); children.hasNext();)
                  {
                     Node node = children.nextNode();
                     if (!node.getPath().startsWith("/jcr:system"))
                     {
                        node.remove();
                     }
                  }
                  sysSession.save();
               }
            }
            catch (Exception e)
            {
               log.error("tearDown() ERROR " + getClass().getName() + "." + getName() + " " + e, e);
            }
            finally
            {
               sysSession.logout();
               session.logout();
            }
         }
      }

      super.tearDown();
   }

   /**
    * Test if Mary can read root node of system workspace.
    */
   public void testGetRootNodeWSFailed() throws Exception
   {
      try
      {
         sessionMaryWS.getRootNode();

         fail("Exception should be thrown.");
      }
      catch (AccessDeniedException e)
      {
      }
   }

   /**
    * Test if Mary can perform versions operations in workspace where she has all right
    * and in same time she has not rights in system workspace. 
    */
   public void testCheckinCheckoutWS1Success() throws Exception
   {
      NodeImpl node = (NodeImpl)sessionMaryWS1.getRootNode().getNode("MARY-ReadWrite");
      node.addMixin("mix:versionable");
      sessionMaryWS1.save();

      node.checkin();
      node.checkout();

      Version version = node.getVersionHistory().getVersion("1");
      version.getPredecessors();
      version.getSuccessors();
      version.getContainingHistory();

      node.restore("1", true);

      node.remove();
      sessionMaryWS1.save();
   }

   /**
    * Test if Mary can add mixin on node with only read permission.
    */
   public void testAddMixinWS1Failed() throws Exception
   {
      NodeImpl node = (NodeImpl)sessionMaryWS1.getRootNode().getNode("MARY-ReadOnly").getNode("test");

      try
      {
         node.addMixin("mix:versionable");
         sessionMaryWS1.save();

         fail("Exception should be thrown.");
      }
      catch (AccessDeniedException e)
      {
      }
   }

   /**
    * Test if Mary can remove mixin on node with only read permission.
    */
   public void testRemoveMixinWS1Failed() throws Exception
   {
      NodeImpl node = (NodeImpl)sessionWS1.getRootNode().getNode("MARY-ReadOnly").getNode("test");
      node.addMixin("mix:versionable");
      sessionWS1.save();

      node = (NodeImpl)sessionMaryWS1.getRootNode().getNode("MARY-ReadOnly").getNode("test");

      try
      {
         node.removeMixin("mix:versionable");
         sessionMaryWS1.save();

         fail("Exception should be thrown.");
      }
      catch (AccessDeniedException e)
      {
      }
   }

   /**
    * Test if Mary can checkin on node with only read permission.
    */
   public void testCheckinWS1Failed() throws Exception
   {
      Node node = sessionWS1.getRootNode().getNode("MARY-ReadOnly").getNode("test");
      node.addMixin("mix:versionable");
      sessionWS1.save();

      node = sessionMaryWS1.getRootNode().getNode("MARY-ReadOnly").getNode("test");

      try
      {
         node.checkin();
         fail("Exception should be thrown.");
      }
      catch (AccessDeniedException e)
      {
      }
   }

   /**
    * Test if Mary can checkout on node with only read permission.
    */
   public void testCheckoutWS1Failed() throws Exception
   {
      Node node = sessionWS1.getRootNode().getNode("MARY-ReadOnly").getNode("test");
      node.addMixin("mix:versionable");
      sessionWS1.save();

      node.checkin();

      node = sessionMaryWS1.getRootNode().getNode("MARY-ReadOnly").getNode("test");

      try
      {
         node.checkout();
         fail("Exception should be thrown.");
      }
      catch (AccessDeniedException e)
      {
      }
   }

   /**
    * Test if Mary can restore on node with only read permission.
    */
   public void testRestoreWS1Failed() throws Exception
   {
      Node node = sessionWS1.getRootNode().getNode("MARY-ReadOnly").getNode("test");
      node.addMixin("mix:versionable");
      sessionWS1.save();

      node.checkin();
      node.checkout();

      node = sessionMaryWS1.getRootNode().getNode("MARY-ReadOnly").getNode("test");

      try
      {
         node.restore("1", true);
         fail("Exception should be thrown.");
      }
      catch (AccessDeniedException e)
      {
      }
   }

   public void testAccessPermission() throws Exception
   {
      // At creation time
      NodeImpl node = (NodeImpl)sessionWS1.getRootNode().addNode("testAccessPermission");
      node.addMixin("mix:versionable");
      sessionWS1.save();
      node.addMixin("exo:privilegeable");
      node.getSession().save();

      node.setPermission("admin", new String[]{"read", "add_node", "set_property", "remove"});
      node.removePermission(SystemIdentity.ANY);
      NodeImpl subNode = (NodeImpl)node.addNode("subNode");
      node.getSession().save();

      node.checkin();
      node.setPermission(SystemIdentity.ANY, new String[]{"read"});
      node.getSession().save();

      Credentials credentials = new CredentialsImpl("john", "exo".toCharArray());
      SessionImpl sessionJohnWS1 = (SessionImpl)repositoryService.getRepository("db2").login(credentials, "ws1");

      Credentials anonCredentials = new CredentialsImpl(SystemIdentity.ANONIM, "".toCharArray());
      SessionImpl anonSession = (SessionImpl)repositoryService.getRepository("db2").login(anonCredentials, "ws1");
      try
      {

         NodeImpl vNode = (NodeImpl)sessionJohnWS1.getRootNode().getNode("testAccessPermission");
         assertNotNull(vNode);
         VersionHistoryImpl vHist = (VersionHistoryImpl)vNode.getVersionHistory();
         assertEquals(vHist.getACL().getPermissions("admin").size(), 0);
         assertEquals(vHist.getACL().getPermissions("any").size(), 1); // there is a workaround in ScratchWorkspaceInitializer

         vNode = (NodeImpl)vHist.getVersion("1");
         assertEquals(vNode.getACL().getPermissions("admin").size(), 4);
         assertEquals(vNode.getACL().getPermissions("any").size(), 1); // there is a workaround in ScratchWorkspaceInitializer

         assertNotNull(vNode);
         vNode = (NodeImpl)vNode.getNode("jcr:frozenNode");
         assertNotNull(vNode);
         assertNotNull(vNode.getNode("subNode"));

         assertEquals(vNode.getACL().getPermissions("admin").size(), 4);
         assertEquals(vNode.getACL().getPermissions("any").size(), 1); // there is a workaround in ScratchWorkspaceInitializer

         //         try
         //         {
         //            anonSession.getNodeByUUID(vNode.getUUID());
         //            fail("Anonim shoul not have permission to node");
         //         }
         //         catch (Exception e)
         //         {
         //         }
      }
      finally
      {
         if (anonSession != null)
         {
            anonSession.logout();
         }

         if (sessionJohnWS1 != null)
         {
            sessionJohnWS1.logout();
         }
      }
   }

   public void testAccessPermissionForAny() throws Exception
   {
      // At creation time
      NodeImpl node = (NodeImpl)sessionWS1.getRootNode().addNode("testAccessPermissionAny");
      node.addMixin("mix:versionable");
      sessionWS1.save();
      node.addMixin("exo:privilegeable");
      node.getSession().save();

      node.clearACL();
      node.setPermission("admin", new String[]{"read", "add_node", "set_property", "remove"});
      node.setPermission(SystemIdentity.ANY, new String[]{"read"});

      NodeImpl subNode = (NodeImpl)node.addNode("subNode");
      node.getSession().save();

      Version version = node.checkin();

      Credentials credentials = new CredentialsImpl("john", "exo".toCharArray());
      SessionImpl sessionJohnWS1 = (SessionImpl)repositoryService.getRepository("db2").login(credentials, "ws1");

      Credentials anonCredentials = new CredentialsImpl(SystemIdentity.ANONIM, "".toCharArray());
      SessionImpl anonSession = (SessionImpl)repositoryService.getRepository("db2").login(anonCredentials, "ws1");
      try
      {
         NodeImpl vNode = (NodeImpl)sessionJohnWS1.getRootNode().getNode("testAccessPermissionAny");
         assertNotNull(vNode);
         VersionHistoryImpl vHist = (VersionHistoryImpl)vNode.getVersionHistory();
         assertEquals(vHist.getACL().getPermissions("admin").size(), 0);
         assertEquals(vHist.getACL().getPermissions("any").size(), 1); // there is a workaround in ScratchWorkspaceInitializer

         vNode = (NodeImpl)vHist.getVersion("1");
         assertEquals(vNode.getACL().getPermissions("admin").size(), 4);
         assertEquals(vNode.getACL().getPermissions("any").size(), 1);

         assertNotNull(vNode);
         vNode = (NodeImpl)vNode.getNode("jcr:frozenNode");
         assertNotNull(vNode);
         assertNotNull(vNode.getNode("subNode"));

         assertEquals(vNode.getACL().getPermissions("admin").size(), 4);
         assertEquals(vNode.getACL().getPermissions("any").size(), 1);

         vNode = (NodeImpl)anonSession.getRootNode().getNode("testAccessPermissionAny");
         assertNotNull(vNode);
         vHist = (VersionHistoryImpl)vNode.getVersionHistory();
         assertEquals(vHist.getACL().getPermissions("admin").size(), 0);
         assertEquals(vHist.getACL().getPermissions("any").size(), 1); // there is a workaround in ScratchWorkspaceInitializer

         vNode = (NodeImpl)vHist.getVersion("1");
         assertEquals(vNode.getACL().getPermissions("admin").size(), 4);
         assertEquals(vNode.getACL().getPermissions("any").size(), 1);

         assertNotNull(vNode);
         vNode = (NodeImpl)vNode.getNode("jcr:frozenNode");
         assertNotNull(vNode);
         assertNotNull(vNode.getNode("subNode"));

         assertEquals(vNode.getACL().getPermissions("admin").size(), 4);
         assertEquals(vNode.getACL().getPermissions("any").size(), 1);

         vNode = (NodeImpl)anonSession.getNodeByUUID(vNode.getUUID());
         assertNotNull(vNode);
         assertNotNull(vNode.getNode("subNode"));
      }
      finally
      {
         if (anonSession != null)
         {
            anonSession.logout();
         }

         if (sessionJohnWS1 != null)
         {
            sessionJohnWS1.logout();
         }
      }
   }

   public void testAccessPermissionDuringMove1() throws Exception
   {
      NodeImpl node = (NodeImpl)sessionWS1.getRootNode().addNode("srcNode");
      sessionWS1.save();

      assertEquals(node.getACL().getPermissions("mary").size(), 0);
      assertEquals(node.getACL().getOwner(), SystemIdentity.SYSTEM);

      // destination node has its own permissions and owner
      node = (NodeImpl)sessionWS1.getRootNode().addNode("dstNode");
      node.addMixin("exo:privilegeable");
      node.addMixin("exo:owneable");
      node.setPermission("mary", new String[]{"read", "add_node", "set_property", "remove"});
      sessionWS1.save();

      assertEquals(node.getACL().getPermissions("mary").size(), 4);
      assertEquals(node.getACL().getOwner(), "admin");

      // move node to new destination with new ACL
      sessionWS1.move("/srcNode", "/dstNode/newSrc");
      sessionWS1.save();

      node = (NodeImpl)sessionWS1.getRootNode().getNode("dstNode/newSrc");

      // acl should be changed
      node = (NodeImpl)sessionWS1.getRootNode().getNode("dstNode/newSrc");
      assertEquals(node.getACL().getPermissions("mary").size(), 4);
      assertEquals(node.getACL().getOwner(), "admin");
   }

   public void testAccessPermissionDuringMove2() throws Exception
   {
      NodeImpl node = (NodeImpl)sessionWS1.getRootNode().addNode("srcNode");
      node.addMixin("exo:privilegeable");
      node.addMixin("exo:owneable");
      node.setPermission("mary", new String[]{"read", "add_node", "set_property", "remove"});
      sessionWS1.save();

      assertEquals(node.getACL().getPermissions("mary").size(), 4);
      assertEquals(node.getACL().getPermissions("admin").size(), 0);
      assertEquals(node.getACL().getOwner(), "admin");

      node = (NodeImpl)sessionWS1.getRootNode().addNode("dstNode");
      node.addMixin("exo:privilegeable");
      node.setPermission("admin", new String[]{"read", "add_node", "set_property", "remove"});
      sessionWS1.save();

      assertEquals(node.getACL().getPermissions("admin").size(), 4);
      assertEquals(node.getACL().getOwner(), SystemIdentity.SYSTEM);

      // move node to new destination with new ACL
      sessionWS1.move("/srcNode", "/dstNode/newSrc");
      sessionWS1.save();

      node = (NodeImpl)sessionWS1.getRootNode().getNode("dstNode/newSrc");

      // acl should not be changed
      node = (NodeImpl)sessionWS1.getRootNode().getNode("dstNode/newSrc");
      assertEquals(node.getACL().getPermissions("mary").size(), 4);
      assertEquals(node.getACL().getPermissions("admin").size(), 0);
      assertEquals(node.getACL().getOwner(), "admin");
   }

   public void testAccessPermissionDuringCopy1() throws Exception
   {
      NodeImpl node = (NodeImpl)sessionWS1.getRootNode().addNode("srcNode");
      sessionWS1.save();

      assertEquals(node.getACL().getPermissions("mary").size(), 0);
      assertEquals(node.getACL().getOwner(), SystemIdentity.SYSTEM);

      // destination node has its own permissions and owner
      node = (NodeImpl)sessionWS1.getRootNode().addNode("dstNode");
      node.addMixin("exo:privilegeable");
      node.addMixin("exo:owneable");
      node.setPermission("mary", new String[]{"read", "add_node", "set_property", "remove"});
      sessionWS1.save();

      assertEquals(node.getACL().getPermissions("mary").size(), 4);
      assertEquals(node.getACL().getOwner(), "admin");

      // move node to new destination with new ACL
      sessionWS1.getWorkspace().copy("/srcNode", "/dstNode/newSrc");

      node = (NodeImpl)sessionWS1.getRootNode().getNode("dstNode/newSrc");

      // acl should be changed
      node = (NodeImpl)sessionWS1.getRootNode().getNode("dstNode/newSrc");
      assertEquals(node.getACL().getPermissions("mary").size(), 4);
      assertEquals(node.getACL().getOwner(), "admin");
   }

   public void testAccessPermissionDuringCopy2() throws Exception
   {
      NodeImpl node = (NodeImpl)sessionWS1.getRootNode().addNode("srcNode");
      node.addMixin("exo:privilegeable");
      node.addMixin("exo:owneable");
      node.setPermission("mary", new String[]{"read", "add_node", "set_property", "remove"});
      sessionWS1.save();

      assertEquals(node.getACL().getPermissions("mary").size(), 4);
      assertEquals(node.getACL().getPermissions("admin").size(), 0);
      assertEquals(node.getACL().getOwner(), "admin");

      node = (NodeImpl)sessionWS1.getRootNode().addNode("dstNode");
      node.addMixin("exo:privilegeable");
      node.setPermission("admin", new String[]{"read", "add_node", "set_property", "remove"});
      sessionWS1.save();

      assertEquals(node.getACL().getPermissions("admin").size(), 4);
      assertEquals(node.getACL().getOwner(), SystemIdentity.SYSTEM);

      // move node to new destination with new ACL
      sessionWS1.getWorkspace().copy("/srcNode", "/dstNode/newSrc");
      sessionWS1.save();

      node = (NodeImpl)sessionWS1.getRootNode().getNode("dstNode/newSrc");

      // acl should not be changed
      node = (NodeImpl)sessionWS1.getRootNode().getNode("dstNode/newSrc");
      assertEquals(node.getACL().getPermissions("mary").size(), 4);
      assertEquals(node.getACL().getPermissions("admin").size(), 0);
      assertEquals(node.getACL().getOwner(), "admin");
   }

   /**
    * Test restore of exo:privilegeable.
    */
   public void testPrivilegeable() throws Exception
   {
      final String TESTNODE_NAME = "testRestorePrivilegeable";
      final String CHILD_TESTNODE_NAME1 = "childTestRestorePrivilegeable1";
      final String CHILD_TESTNODE_NAME2 = "childTestRestorePrivilegeable2";
      final String CHILD_TESTNODE_NAME3 = "childTestRestorePrivilegeable3";
      final String CHILD_TESTNODE_NAME4 = "childTestRestorePrivilegeable4";

      Credentials johnCredentials = new CredentialsImpl("john", "exo".toCharArray());
      SessionImpl johnSession = (SessionImpl)repositoryService.getRepository("db2").login(johnCredentials, "ws1");

      Credentials anonCredentials = new CredentialsImpl(SystemIdentity.ANONIM, "".toCharArray());
      SessionImpl anonSession = (SessionImpl)repositoryService.getRepository("db2").login(anonCredentials, "ws1");

      NodeImpl node = (NodeImpl)sessionWS1.getRootNode().addNode(TESTNODE_NAME);
      node.addMixin("exo:privilegeable");
      node.addMixin("exo:owneable");
      node.addMixin("mix:versionable");
      node.setPermission("*:/platform/administrators", PermissionType.ALL);
      node.setPermission("mary",
         new String[]{PermissionType.READ, PermissionType.SET_PROPERTY, PermissionType.ADD_NODE});
      node.removePermission(SystemIdentity.ANY);
      sessionWS1.save();

      // child node exo:privilegeable & exo:owneable
      NodeImpl childNode1 = (NodeImpl)node.addNode(CHILD_TESTNODE_NAME1);
      childNode1.addMixin("exo:privilegeable");
      childNode1.addMixin("exo:owneable");
      childNode1.setPermission("*:/platform/administrators", PermissionType.ALL);
      childNode1.setPermission("mary", new String[]{PermissionType.READ, PermissionType.SET_PROPERTY});
      childNode1.removePermission(SystemIdentity.ANY);
      sessionWS1.save();

      // child node all inherited from parent
      NodeImpl childNode2 = (NodeImpl)node.addNode(CHILD_TESTNODE_NAME2);
      sessionWS1.save();

      // child node exo:owneable
      node = (NodeImpl)johnSession.getRootNode().getNode(TESTNODE_NAME);
      NodeImpl childNode3 = (NodeImpl)node.addNode(CHILD_TESTNODE_NAME3);
      childNode3.addMixin("exo:owneable");
      johnSession.save();

      node = (NodeImpl)sessionWS1.getRootNode().getNode(TESTNODE_NAME);

      // child node exo:privilegeable
      NodeImpl childNode4 = (NodeImpl)node.addNode(CHILD_TESTNODE_NAME4);
      childNode4.addMixin("exo:privilegeable");
      childNode4.setPermission("*:/platform/administrators", PermissionType.ALL);
      childNode4.setPermission("mary", new String[]{PermissionType.READ, PermissionType.SET_PROPERTY});
      childNode4.removePermission(SystemIdentity.ANY);
      sessionWS1.save();

      // check what we have 
      NodeImpl marysNode = (NodeImpl)sessionMaryWS1.getRootNode().getNode(TESTNODE_NAME);
      assertTrue(marysNode.hasPermission(PermissionType.READ));
      assertTrue(marysNode.hasPermission(PermissionType.SET_PROPERTY));
      assertTrue(marysNode.hasPermission(PermissionType.ADD_NODE));
      assertFalse(marysNode.hasPermission(PermissionType.REMOVE));
      assertEquals(((NodeData)marysNode.getData()).getACL().getOwner(), "admin");

      NodeImpl marysChildNode1 = (NodeImpl)marysNode.getNode(CHILD_TESTNODE_NAME1);
      assertTrue(marysChildNode1.hasPermission(PermissionType.READ));
      assertTrue(marysChildNode1.hasPermission(PermissionType.SET_PROPERTY));
      assertFalse(marysChildNode1.hasPermission(PermissionType.ADD_NODE));
      assertEquals(((NodeData)marysChildNode1.getData()).getACL().getOwner(), "admin");

      NodeImpl marysChildNode2 = (NodeImpl)marysNode.getNode(CHILD_TESTNODE_NAME2);
      assertTrue(marysNode.hasPermission(PermissionType.READ));
      assertTrue(marysNode.hasPermission(PermissionType.SET_PROPERTY));
      assertTrue(marysNode.hasPermission(PermissionType.ADD_NODE));
      assertFalse(marysNode.hasPermission(PermissionType.REMOVE));;
      assertEquals(((NodeData)marysChildNode2.getData()).getACL().getOwner(), "admin");
      
      NodeImpl marysChildNode3 = (NodeImpl)marysNode.getNode(CHILD_TESTNODE_NAME3);
      assertTrue(marysChildNode3.hasPermission(PermissionType.READ));
      assertTrue(marysChildNode3.hasPermission(PermissionType.SET_PROPERTY));
      assertTrue(marysChildNode3.hasPermission(PermissionType.ADD_NODE));
      assertFalse(marysChildNode3.hasPermission(PermissionType.REMOVE));
      assertEquals(((NodeData)marysChildNode3.getData()).getACL().getOwner(), "john");

      NodeImpl marysChildNode4 = (NodeImpl)marysNode.getNode(CHILD_TESTNODE_NAME4);
      assertTrue(marysChildNode4.hasPermission(PermissionType.READ));
      assertTrue(marysChildNode4.hasPermission(PermissionType.SET_PROPERTY));
      assertFalse(marysChildNode4.hasPermission(PermissionType.REMOVE));;
      assertEquals(((NodeData)marysChildNode2.getData()).getACL().getOwner(), "admin");

      // for __anonim
      try
      {
         anonSession.getRootNode().getNode(TESTNODE_NAME);
      }
      catch (AccessDeniedException e)
      {
         // ok
      }

      // v1
      VersionImpl version = (VersionImpl)node.checkin();
      node.checkout();

      // check frozen node and its children nodes
      NodeImpl frozenNode =
         (NodeImpl)sessionMaryWS1.getRootNode().getNode(TESTNODE_NAME).getVersionHistory().getVersion("1")
            .getNode("jcr:frozenNode");

      assertTrue(frozenNode.hasPermission(PermissionType.READ));
      assertTrue(frozenNode.hasPermission(PermissionType.SET_PROPERTY));
      assertTrue(frozenNode.hasPermission(PermissionType.ADD_NODE));
      assertFalse(frozenNode.hasPermission(PermissionType.REMOVE));
      assertEquals(((NodeData)frozenNode.getData()).getACL().getOwner(), "admin");

      marysChildNode1 = (NodeImpl)frozenNode.getNode(CHILD_TESTNODE_NAME1);
      assertTrue(marysChildNode1.hasPermission(PermissionType.READ));
      assertTrue(marysChildNode1.hasPermission(PermissionType.SET_PROPERTY));
      assertFalse(marysChildNode1.hasPermission(PermissionType.ADD_NODE));
      assertEquals(((NodeData)marysChildNode1.getData()).getACL().getOwner(), "admin");

      marysChildNode2 = (NodeImpl)frozenNode.getNode(CHILD_TESTNODE_NAME2);
      assertTrue(marysNode.hasPermission(PermissionType.READ));
      assertTrue(marysNode.hasPermission(PermissionType.SET_PROPERTY));
      assertTrue(marysNode.hasPermission(PermissionType.ADD_NODE));
      assertFalse(marysNode.hasPermission(PermissionType.REMOVE));;
      assertEquals(((NodeData)marysChildNode2.getData()).getACL().getOwner(), "admin");

      marysChildNode3 = (NodeImpl)frozenNode.getNode(CHILD_TESTNODE_NAME3);
      assertTrue(marysChildNode3.hasPermission(PermissionType.READ));
      assertTrue(marysChildNode3.hasPermission(PermissionType.SET_PROPERTY));
      assertTrue(marysChildNode3.hasPermission(PermissionType.ADD_NODE));
      assertFalse(marysChildNode3.hasPermission(PermissionType.REMOVE));
      assertEquals(((NodeData)marysChildNode3.getData()).getACL().getOwner(), "john");

      marysChildNode4 = (NodeImpl)frozenNode.getNode(CHILD_TESTNODE_NAME4);
      assertTrue(marysChildNode4.hasPermission(PermissionType.READ));
      assertTrue(marysChildNode4.hasPermission(PermissionType.SET_PROPERTY));
      assertFalse(marysChildNode4.hasPermission(PermissionType.REMOVE));;
      assertEquals(((NodeData)marysChildNode2.getData()).getACL().getOwner(), "admin");

      try
      {
         // restore v1
         node.restore("1", true);
      }
      catch (AccessDeniedException e)
      {
         fail("Restore should succeed");
      }

      // check what we have after restore
      marysNode = (NodeImpl)sessionMaryWS1.getRootNode().getNode(TESTNODE_NAME);
      assertTrue(marysNode.hasPermission(PermissionType.READ));
      assertTrue(marysNode.hasPermission(PermissionType.SET_PROPERTY));
      assertTrue(marysNode.hasPermission(PermissionType.ADD_NODE));
      assertFalse(marysNode.hasPermission(PermissionType.REMOVE));
      assertEquals(((NodeData)marysNode.getData()).getACL().getOwner(), "admin");

      marysChildNode1 = (NodeImpl)marysNode.getNode(CHILD_TESTNODE_NAME1);
      assertTrue(marysChildNode1.hasPermission(PermissionType.READ));
      assertTrue(marysChildNode1.hasPermission(PermissionType.SET_PROPERTY));
      assertFalse(marysChildNode1.hasPermission(PermissionType.ADD_NODE));
      assertEquals(((NodeData)marysChildNode1.getData()).getACL().getOwner(), "admin");

      marysChildNode2 = (NodeImpl)marysNode.getNode(CHILD_TESTNODE_NAME2);
      assertTrue(marysNode.hasPermission(PermissionType.READ));
      assertTrue(marysNode.hasPermission(PermissionType.SET_PROPERTY));
      assertTrue(marysNode.hasPermission(PermissionType.ADD_NODE));
      assertFalse(marysNode.hasPermission(PermissionType.REMOVE));
      assertEquals(((NodeData)marysChildNode2.getData()).getACL().getOwner(), "admin");;

      marysChildNode3 = (NodeImpl)marysNode.getNode(CHILD_TESTNODE_NAME3);
      assertTrue(marysChildNode3.hasPermission(PermissionType.READ));
      assertTrue(marysChildNode3.hasPermission(PermissionType.SET_PROPERTY));
      assertTrue(marysChildNode3.hasPermission(PermissionType.ADD_NODE));
      assertFalse(marysChildNode3.hasPermission(PermissionType.REMOVE));
      assertEquals(((NodeData)marysChildNode3.getData()).getACL().getOwner(), "john");

      marysChildNode4 = (NodeImpl)marysNode.getNode(CHILD_TESTNODE_NAME4);
      assertTrue(marysChildNode4.hasPermission(PermissionType.READ));
      assertTrue(marysChildNode4.hasPermission(PermissionType.SET_PROPERTY));
      assertFalse(marysChildNode4.hasPermission(PermissionType.REMOVE));;
      assertEquals(((NodeData)marysChildNode2.getData()).getACL().getOwner(), "admin");

      // for __anonim
      try
      {
         anonSession.getRootNode().getNode(TESTNODE_NAME);
      }
      catch (AccessDeniedException e)
      {
         // ok
      }
      finally
      {
         anonSession.logout();
      }

      johnSession.logout();
   }

   /**
    * Test restore of exo:privilegeable.
    */
   public void testPrivilegeable2() throws Exception
   {
      final String TESTNODE_NAME = "testRestorePrivilegeable2";

      NodeImpl node = (NodeImpl)sessionWS1.getRootNode().addNode(TESTNODE_NAME);
      node.addMixin("exo:privilegeable");
      node.addMixin("mix:versionable");
      node.setPermission("*:/platform/administrators", PermissionType.ALL);
      node.setPermission("mary",
         new String[]{PermissionType.READ, PermissionType.SET_PROPERTY, PermissionType.ADD_NODE});
      node.removePermission(SystemIdentity.ANY);
      sessionWS1.save();

      // check what we have 
      NodeImpl marysNode = (NodeImpl)sessionMaryWS1.getRootNode().getNode(TESTNODE_NAME);
      assertTrue(marysNode.hasPermission(PermissionType.READ));
      assertTrue(marysNode.hasPermission(PermissionType.SET_PROPERTY));
      assertTrue(marysNode.hasPermission(PermissionType.ADD_NODE));
      assertFalse(marysNode.hasPermission(PermissionType.REMOVE));
      assertEquals(((NodeData)marysNode.getData()).getACL().getOwner(), SystemIdentity.SYSTEM);

      // v1
      node.checkin();
      node.checkout();

      try
      {
         // restore v1
         node.restore("1", true);
      }
      catch (AccessDeniedException e)
      {
         fail("Restore should succeed");
      }

      // check what we have after restore
      marysNode = (NodeImpl)sessionMaryWS1.getRootNode().getNode(TESTNODE_NAME);
      assertTrue(marysNode.hasPermission(PermissionType.READ));
      assertTrue(marysNode.hasPermission(PermissionType.SET_PROPERTY));
      assertTrue(marysNode.hasPermission(PermissionType.ADD_NODE));
      assertFalse(marysNode.hasPermission(PermissionType.REMOVE));
      assertEquals(((NodeData)marysNode.getData()).getACL().getOwner(), SystemIdentity.SYSTEM);
   }

   /**
    * Test restore of exo:privilegeable.
    */
   public void testPrivilegeable3() throws Exception
   {
      final String TESTNODE_NAME = "testRestorePrivilegeable3";

      NodeImpl node = (NodeImpl)sessionWS1.getRootNode().addNode(TESTNODE_NAME);
      node.addMixin("exo:owneable");
      node.addMixin("mix:versionable");
      sessionWS1.save();

      // check what we have 
      NodeImpl marysNode = (NodeImpl)sessionMaryWS1.getRootNode().getNode(TESTNODE_NAME);
      assertEquals(marysNode.getACL().getPermissionsSize(), 4);
      assertEquals(marysNode.getACL().getPermissions(SystemIdentity.ANY).size(), 4);
      assertEquals(marysNode.getACL().getPermissions("mary").size(), 0);
      assertEquals(((NodeData)marysNode.getData()).getACL().getOwner(), "admin");

      // v1
      node.checkin();
      node.checkout();

      try
      {
         // restore v1
         node.restore("1", true);
      }
      catch (AccessDeniedException e)
      {
         fail("Restore should succeed");
      }

      // check what we have after restore
      marysNode = (NodeImpl)sessionMaryWS1.getRootNode().getNode(TESTNODE_NAME);
      assertEquals(marysNode.getACL().getPermissionsSize(), 4);
      assertEquals(marysNode.getACL().getPermissions(SystemIdentity.ANY).size(), 4);
      assertEquals(marysNode.getACL().getPermissions("mary").size(), 0);
      assertEquals(((NodeData)marysNode.getData()).getACL().getOwner(), "admin");
   }

}
