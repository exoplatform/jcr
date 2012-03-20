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
package org.exoplatform.services.jcr.impl.access;

import org.exoplatform.services.jcr.BaseStandaloneTest;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.AccessManager;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;

import java.io.InputStream;
import java.security.AccessControlException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS.<br/>
 * Prerequisite: enable access control i.e.
 * <access-control>optional</access-control>
 * 
 * @author Gennady Azarenkov
 * @version $Id: TestAccess.java 14515 2008-05-20 11:45:21Z ksm $
 */

public class TestAccess extends BaseStandaloneTest
{

   private ExtendedNode accessTestRoot;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      if (!session.getRootNode().hasNode("accessTestRoot"))
      {
         accessTestRoot = (ExtendedNode)session.getRootNode().addNode("accessTestRoot");
         session.save();
      }
      else
      {
         accessTestRoot = (ExtendedNode)session.getRootNode().getNode("accessTestRoot");
      }
   }

   @Override
   protected void tearDown() throws Exception
   {
      // accessTestRoot.remove();
      // session.save();
      super.tearDown();
   }

   @Override
   public String getRepositoryName()
   {
      return "db1";
   }

   /**
    * tests default permission (if node is not exo:accessControllable)
    * 
    * @throws Exception
    */
   public void testNoAccessControllable() throws Exception
   {
      AccessControlList acl = ((ExtendedNode)root).getACL();
      assertEquals(IdentityConstants.SYSTEM, acl.getOwner());
      assertEquals(PermissionType.ALL.length, acl.getPermissionEntries().size());
      assertEquals(PermissionType.ALL[0], acl.getPermissionEntries().get(0).getPermission());
   }

   /**
    * tests default permission for exo:owneable node
    * 
    * @throws Exception
    */
   public void testOwneable() throws Exception
   {
      ExtendedNode node = (ExtendedNode)session.getRootNode().addNode("testACNode");
      node.addMixin("exo:owneable");
      AccessControlList acl = node.getACL();
      assertEquals(session.getUserID(), acl.getOwner());
      assertEquals(PermissionType.ALL.length, acl.getPermissionEntries().size());
      assertEquals(PermissionType.ALL[0], acl.getPermissionEntries().get(0).getPermission());
   }

   /**
    * tests default permission for exo:privilegeable node
    * 
    * @throws Exception
    */
   public void testPrivilegeable() throws Exception
   {
      ExtendedNode node = (ExtendedNode)session.getRootNode().addNode("testACNode");
      node.addMixin("exo:privilegeable");
      AccessControlList acl = node.getACL();
      assertEquals(IdentityConstants.SYSTEM, acl.getOwner());
      assertEquals(PermissionType.ALL.length, acl.getPermissionEntries().size());
      assertEquals(PermissionType.ALL[0], acl.getPermissionEntries().get(0).getPermission());
   }

   /**
    * test permission for default exo:accessControllable node - i.e. if just
    * node.addMixin("exo:accessControllable");
    * 
    * @throws Exception
    */
   public void testDefaultAccessControllable() throws Exception
   {
      ExtendedNode node = (ExtendedNode)session.getRootNode().addNode("testACNode");
      // node.addMixin("exo:accessControllable");
      node.addMixin("exo:owneable");
      node.addMixin("exo:privilegeable");

      AccessControlList acl = node.getACL();
      assertEquals(session.getUserID(), acl.getOwner());

      assertEquals(PermissionType.ALL.length, acl.getPermissionEntries().size());
      assertEquals(PermissionType.ALL[0], acl.getPermissionEntries().get(0).getPermission());

      // the same after save() and re-retrieve
      session.save();
      node = (ExtendedNode)session.getRootNode().getNode("testACNode");

      Session session1 = repository.login(new CredentialsImpl("mary", "exo".toCharArray()));
      session1.getRootNode().getNode("testACNode");

      acl = node.getACL();
      assertEquals(session.getUserID(), acl.getOwner());

      assertEquals(PermissionType.ALL.length, acl.getPermissionEntries().size());
      assertEquals(PermissionType.ALL[0], acl.getPermissionEntries().get(0).getPermission());
   }

   /**
    * tests if persmission are saved permanently
    * 
    * @throws Exception
    */
   public void testIfPermissionSaved() throws Exception
   {
      NodeImpl node = (NodeImpl)accessTestRoot.addNode("testIfPermissionSaved");
      // node.addMixin("exo:accessControllable");
      node.addMixin("exo:owneable");
      node.addMixin("exo:privilegeable");

      session.save();
      if (log.isDebugEnabled())
      {
         log.debug("NODE PERM 1 >>> " + node.getACL().dump());
      }
      // change permission
      HashMap<String, String[]> perm = new HashMap<String, String[]>();
      perm.put("john", new String[]{PermissionType.ADD_NODE, PermissionType.READ});
      node.setPermissions(perm);

      // showPermissions("accessTestRoot/testIfPermissionSaved");

      if (log.isDebugEnabled())
      {
         log.debug("NODE PERM 1 >>> " + node.getACL().dump());
      }

      session.save();

      if (log.isDebugEnabled())
      {
         log.debug("NODE PERM 2 >>> " + node.getACL().dump());
      }

      // get node in new session
      NodeImpl testNode =
         (NodeImpl)repository.getSystemSession().getRootNode().getNode("accessTestRoot/testIfPermissionSaved");

      if (log.isDebugEnabled())
      {
         log.debug("NODE PERM 4 >>> " + node.getACL().dump());
      }

      if (log.isDebugEnabled())
      {
         log.debug("TEST PERM >>> " + testNode.getACL().dump());
      }

      showPermissions("accessTestRoot/testIfPermissionSaved");

      AccessControlList acl = testNode.getACL();

      // ACL should be:
      // Owner = exo
      // ADD_NODE and READ permissions for john
      assertEquals(session.getUserID(), acl.getOwner());

      assertEquals(2, acl.getPermissionEntries().size());
      List<AccessControlEntry> entries = acl.getPermissionEntries();
      assertEquals("john", entries.get(0).getIdentity());
      assertEquals(PermissionType.ADD_NODE, entries.get(0).getPermission());
      assertEquals(PermissionType.READ, entries.get(1).getPermission());
   }

   /**
    * tests child-parent permission inheritance
    * 
    * @throws Exception
    */
   public void testPermissionInheritance() throws Exception
   {
      NodeImpl node = (NodeImpl)accessTestRoot.addNode("testPermissionInheritance");
      // node.addMixin("exo:accessControllable");
      node.addMixin("exo:owneable");
      node.addMixin("exo:privilegeable");

      // change permission
      HashMap<String, String[]> perm = new HashMap<String, String[]>();
      perm.put("john", new String[]{PermissionType.ADD_NODE, PermissionType.READ});
      node.setPermissions(perm);
      // AccessControlList acl = node.getACL();

      // add child node and test if acl is equal to parent
      NodeImpl node1 = (NodeImpl)node.addNode("node1");
      // AccessControlList acl = node1.getACL();
      assertEquals(node.getACL(), node1.getACL());

      // add grandchild node and test if acl is equal to grandparent
      NodeImpl node2 = (NodeImpl)node1.addNode("node1");
      assertEquals(node.getACL(), node2.getACL());
   }

   /**
    * tests session.checkPermission() method
    * 
    * @throws Exception
    */
   public void testSessionCheckPermission() throws Exception
   {
      NodeImpl node = (NodeImpl)accessTestRoot.addNode("testSessionCheckPermission");
      // node.addMixin("exo:accessControllable");
      node.addMixin("exo:owneable");
      node.addMixin("exo:privilegeable");

      // change permission
      HashMap<String, String[]> perm = new HashMap<String, String[]>();
      perm.put("john", new String[]{PermissionType.ADD_NODE, PermissionType.READ});
      node.setPermissions(perm);
      session.save();

      // showPermissions("accessTestRoot/testSessionCheckPermission");

      // ACL is:
      // Owner = exo
      // ADD_NODE and READ permissions for john
      // check permission for john - ADD_NODE and READ allowed
      Session session1 = repository.login(new CredentialsImpl("john", "exo".toCharArray()));
      session1.checkPermission("/accessTestRoot/testSessionCheckPermission", PermissionType.READ);
      try
      {
         session1.checkPermission("/accessTestRoot/testSessionCheckPermission", PermissionType.SET_PROPERTY);
         fail("AccessControlException should have been thrown ");
      }
      catch (AccessControlException e)
      {
      }

      // check permission for exo2 - nothing allowed
      Session session2 = repository.login(new CredentialsImpl("mary", "exo".toCharArray()));
      try
      {
         session2.checkPermission("/accessTestRoot/testSessionCheckPermission", PermissionType.READ);
         fail("AccessControlException should have been thrown ");
      }
      catch (AccessControlException e)
      {
      }
   }

   /**
    * tests READ permission
    * 
    * @throws Exception
    */
   public void testRead() throws Exception
   {
      NodeImpl node = (NodeImpl)accessTestRoot.addNode("testRead");
      // node.addMixin("exo:accessControllable");
      node.addMixin("exo:owneable");
      node.addMixin("exo:privilegeable");

      // change permission
      HashMap<String, String[]> perm = new HashMap<String, String[]>();
      perm.put("john", new String[]{PermissionType.READ});
      node.setPermissions(perm);
      node.addNode("node1");
      session.save();

      // ACL is:
      // Owner = exo
      // READ permissions for john

      // check permission for john - READ allowed
      Session session1 = repository.login(new CredentialsImpl("john", "exo".toCharArray()));
      session1.getItem("/accessTestRoot/testRead");
      session1.getItem("/accessTestRoot/testRead/jcr:primaryType");
      session1.getItem("/accessTestRoot/testRead/node1");
      // primartType, mixinTypes, permissions, owner
      assertEquals(4, ((Node)session1.getItem("/accessTestRoot/testRead")).getProperties().getSize());

      Node n1 = (Node)session1.getItem("/accessTestRoot");
      assertEquals(1, n1.getNodes().getSize());

      // check permission for exo2 - nothing allowed
      Session session2 = repository.login(new CredentialsImpl("mary", "exo".toCharArray()));
      try
      {
         session2.getItem("/accessTestRoot/testRead");
         fail("AccessDeniedException should have been thrown ");
      }
      catch (AccessDeniedException e)
      {
      }
      Node n2 = (Node)session2.getItem("/accessTestRoot");
      assertEquals(0, n2.getNodes().getSize());

      // ... test inheritanse
      try
      {
         session2.getItem("/accessTestRoot/testRead/node1");
         fail("AccessDeniedException should have been thrown ");
      }
      catch (AccessDeniedException e)
      {
      }
   }

   public void testAddNode() throws Exception
   {
      // ExtendedNode node =
      // (ExtendedNode)session.getRootNode().addNode("testAddNode");
      ExtendedNode node = (ExtendedNode)accessTestRoot.addNode("testAddNode");
      // node.addMixin("exo:accessControllable");
      node.addMixin("exo:owneable");
      node.addMixin("exo:privilegeable");

      HashMap<String, String[]> perm = new HashMap<String, String[]>();
      // perm.put("john", PermissionType.ALL);
      perm.put("john", new String[]{PermissionType.ADD_NODE, PermissionType.READ});
      perm.put("mary", new String[]{PermissionType.READ});
      node.setPermissions(perm);
      session.save();

      // ACL is:
      // Owner = exo
      // READ, ADD_NODE permissions for john
      // READ permissions for exo2

      // [PN] 19.06.07 owner it's by whom session was open
      // assertEquals("exo",((ExtendedNode)accessTestRoot.getNode("testAddNode")).
      // getACL().getOwner());
      assertEquals(credentials.getUserID(), ((ExtendedNode)accessTestRoot.getNode("testAddNode")).getACL().getOwner());

      accessTestRoot.getNode("testAddNode").addNode("ownersNode");
      session.save();

      Session session1 = repository.login(new CredentialsImpl("mary", "exo".toCharArray()));

      session1.getRootNode().getNode("accessTestRoot/testAddNode").addNode("illegal");

      try
      {
         session1.save();
         fail("AccessDeniedException should have been thrown ");
      }
      catch (AccessDeniedException e)
      {
         session1.refresh(false);
      }

      session1 = repository.login(new CredentialsImpl("john", "exo".toCharArray()));
      session1.getRootNode().getNode("accessTestRoot/testAddNode").addNode("legal");
      session1.save();

      NodeImpl addNode = (NodeImpl)session1.getRootNode().getNode("accessTestRoot/testAddNode");
      addNode.setProperty("illegal", "test");
      try
      {
         session1.save();
         fail("AccessDeniedException should have been thrown ");
      }
      catch (AccessDeniedException e)
      {
         session1.refresh(false);
      }
   }

   public void testModifyAndReadItem() throws Exception
   {
      ExtendedNode node = (ExtendedNode)accessTestRoot.addNode("testModifyAndReadNode");
      // node.addMixin("exo:accessControllable");
      node.addMixin("exo:owneable");
      node.addMixin("exo:privilegeable");

      HashMap<String, String[]> perm = new HashMap<String, String[]>();
      perm.put("john", PermissionType.ALL);
      perm.put("mary", new String[]{PermissionType.READ});
      node.setPermissions(perm);
      session.save();

      // ACL is:
      // Owner = exo
      // ALL permissions for john
      // READ permissions for exo2

      assertEquals(credentials.getUserID(), ((ExtendedNode)session.getRootNode().getNode(
         "accessTestRoot/testModifyAndReadNode")).getACL().getOwner());
      session.getRootNode().getNode("accessTestRoot/testModifyAndReadNode").addNode("ownersNode");
      session.save();

      Session session1 = repository.login(new CredentialsImpl("john", "exo".toCharArray()));

      session1 = repository.login(new CredentialsImpl("john", "exo".toCharArray()));
      session1.getRootNode().getNode("accessTestRoot/testModifyAndReadNode").setProperty("legal", "test");
      session1.getRootNode().getNode("accessTestRoot/testModifyAndReadNode").setProperty("illegal", "test");
      session1.save();

      // session.getItem("/accessTestRoot/testModifyAndReadNode/legal");

      session1.getRootNode().getProperty("accessTestRoot/testModifyAndReadNode/legal").remove();
      session1.save();

      // john
      session1.getRootNode().getNode("accessTestRoot/testModifyAndReadNode").getProperty("illegal");
      assertEquals(1, session1.getRootNode().getNode("accessTestRoot/testModifyAndReadNode").getProperties("illegal")
         .getSize());

      Session session2 = repository.login(new CredentialsImpl("mary", "exo".toCharArray()));
      session2.getRootNode().getNode("accessTestRoot/testModifyAndReadNode").getProperty("illegal").remove();

      try
      {
         // exo2
         session2.save();
         fail("PathNotFoundException or AccessDenied should have been thrown ");
      }
      catch (AccessDeniedException e)
      {
         session2.refresh(false);
         session1.save();
      }

      session2.getRootNode().getNode("accessTestRoot/testModifyAndReadNode").setProperty("illegal2", "test");
      try
      {
         session2.save();
         fail("PathNotFoundException or AccessDenied should have been thrown ");
      }
      catch (AccessDeniedException e)
      {
         session2.refresh(false);
      }
   }

   public void testCheckAndCleanPermissions() throws Exception
   {
      ExtendedNode node = (ExtendedNode)accessTestRoot.addNode("testCheckAndCleanPermissions");
      // node.addMixin("exo:accessControllable");
      node.addMixin("exo:owneable");
      node.addMixin("exo:privilegeable");

      HashMap<String, String[]> perm = new HashMap<String, String[]>();
      perm.put("john", PermissionType.ALL);
      perm.put("mary", new String[]{PermissionType.READ});
      node.setPermissions(perm);
      session.save();

      // ACL is:
      // Owner = exo
      // ALL permissions for john
      // READ permissions for exo2

      Session session1 = repository.login(new CredentialsImpl("john", "exo".toCharArray()));
      Session session2 = repository.login(new CredentialsImpl("mary", "exo".toCharArray()));

      session1.checkPermission("/accessTestRoot/testCheckAndCleanPermissions", PermissionType.ADD_NODE);

      try
      {
         session2.checkPermission("/accessTestRoot/testCheckAndCleanPermissions", PermissionType.ADD_NODE);
         fail("AccessControlException should have been thrown ");
      }
      catch (AccessControlException e)
      {
      }
      session2.checkPermission("/accessTestRoot/testCheckAndCleanPermissions", PermissionType.READ);

      // try to re set permissions
      ExtendedNode node2 = (ExtendedNode)session2.getRootNode().getNode("accessTestRoot/testCheckAndCleanPermissions");

      try
      {
         // no set_property permission
         node2.setPermissions(perm);
         session2.save();
         fail("AccessControlException should have been thrown ");
      }
      catch (AccessControlException e)
      {
         session2.refresh(false);
      }

      // get current permissions
      AccessControlList acl = node2.getACL();
      assertEquals(credentials.getUserID(), acl.getOwner());
      assertEquals(5, acl.getPermissionEntries().size());

      try
      {
         // clean acl
         node2.clearACL();
         session2.save();
         fail("AccessControlException should have been thrown ");
      }
      catch (AccessControlException e)
      {
         session2.refresh(false);
      }

      ExtendedNode node1 = (ExtendedNode)session1.getRootNode().getNode("accessTestRoot/testCheckAndCleanPermissions");
      node1.clearACL();
      session1.save();
      // default
      acl = node1.getACL();
      assertEquals(credentials.getUserID(), acl.getOwner());
      assertEquals(PermissionType.ALL.length, acl.getPermissionEntries().size());
      assertEquals(PermissionType.ALL[0], acl.getPermissionEntries().get(0).getPermission());
   }

   public void testPrivilegeableAddNode() throws Exception
   {
      Node node = session.getRootNode().addNode("testACNode");
      node.addMixin("exo:privilegeable");
      try
      {
         node.addNode("privilegeable");
         session.save();
      }
      catch (AccessControlException e)
      {
         fail("AccessControlException should not have been thrown ");
      }
      try
      {
         session.getRootNode().getNode("testACNode/privilegeable");

      }
      catch (RepositoryException e)
      {
         fail("PathNotFoundException or AccessDenied  should not have been thrown ");
      }
   }

   public void testAddSaveAndRead() throws Exception
   {
      ExtendedNode node = (ExtendedNode)accessTestRoot.addNode("testSetAndRemovePermission");
      node.addMixin("exo:privilegeable");
      node.setPermission("john", PermissionType.ALL);
      String owner = node.getACL().getOwner();
      assertEquals(8, node.getACL().getPermissionEntries().size());
      accessTestRoot.save();
      Session session1 = repository.login(new CredentialsImpl("john", "exo".toCharArray()));
      NodeImpl node1 = (NodeImpl)session1.getRootNode().getNode("accessTestRoot/testSetAndRemovePermission");
      assertEquals(8, node1.getACL().getPermissionEntries().size());
      assertEquals(node1.getACL().getOwner(), owner);
   }

   public void testSetAndRemovePermission() throws Exception
   {
      ExtendedNode node = (ExtendedNode)accessTestRoot.addNode("testSetAndRemovePermission");
      // node.addMixin("exo:accessControllable");
      node.addMixin("exo:owneable");
      node.addMixin("exo:privilegeable");

      node.setPermission("john", PermissionType.ALL);
      assertEquals(PermissionType.ALL.length * 2, node.getACL().getPermissionEntries().size());

      // ("Access contr " +
      // node.isNodeType("exo:accessControllable"));
      node.setPermission("mary", new String[]{PermissionType.READ});
      assertEquals(PermissionType.ALL.length * 2 + 1, node.getACL().getPermissionEntries().size());

      node.removePermission("john");
      assertEquals(PermissionType.ALL.length + 1, node.getACL().getPermissionEntries().size());
   }

   /**
    * check if the setPermission(String identity, String[] permission) completely
    * replace permissions of the identity.
    * 
    * @throws Exception
    */
   public void testReplacePermission() throws Exception
   {
      ExtendedNode node = (ExtendedNode)accessTestRoot.addNode("testReplacePermission");
      // node.addMixin("exo:accessControllable");
      node.addMixin("exo:owneable");
      node.addMixin("exo:privilegeable");

      node.setPermission("john", PermissionType.ALL);
      assertEquals(PermissionType.ALL.length * 2, node.getACL().getPermissionEntries().size());

      // System.out.println("Access contr " +
      // node.isNodeType("exo:accessControllable"));

      node.setPermission("john", new String[]{PermissionType.READ});
      assertEquals(PermissionType.ALL.length + 1, node.getACL().getPermissionEntries().size());

      node.removePermission("john");
      assertEquals(PermissionType.ALL.length, node.getACL().getPermissionEntries().size());
   }

   /**
    * check if the removePermission(String identity, String permission) remove
    * specified permissions of the identity.
    * 
    * @throws Exception
    */
   public void testRemoveSpecified() throws Exception
   {
      AccessManager accessManager = ((SessionImpl)accessTestRoot.getSession()).getAccessManager();

      ExtendedNode node = (ExtendedNode)accessTestRoot.addNode("testRemoveSpecified");
      // node.addMixin("exo:accessControllable");
      node.addMixin("exo:owneable");
      node.addMixin("exo:privilegeable");

      node.setPermission("john", PermissionType.ALL);
      assertEquals(PermissionType.ALL.length * 2, node.getACL().getPermissionEntries().size());

      node.setPermission("mary", PermissionType.ALL);
      accessTestRoot.save();

      Session session1 = repository.login(new CredentialsImpl("mary", "exo".toCharArray()));
      ExtendedNode testRemoveSpecifiedNode =
         (ExtendedNode)session1.getRootNode().getNode("accessTestRoot").getNode("testRemoveSpecified");
      testRemoveSpecifiedNode.removePermission(IdentityConstants.ANY);

      assertTrue(accessManager.hasPermission(testRemoveSpecifiedNode.getACL(), PermissionType.READ,
         new Identity("john")));

      testRemoveSpecifiedNode.removePermission("john", PermissionType.READ);
      assertTrue(accessManager.hasPermission(testRemoveSpecifiedNode.getACL(), PermissionType.SET_PROPERTY,
         new Identity("john")));

      assertFalse(accessManager.hasPermission(testRemoveSpecifiedNode.getACL(), PermissionType.READ, new Identity(
         "john")));

      assertTrue(accessManager.hasPermission(testRemoveSpecifiedNode.getACL(), PermissionType.READ,
         new Identity("mary")));

      // assertFalse(accessManager.hasPermission(testRemoveSpecifiedNode.getACL(),
      // PermissionType.READ,
      // SystemIdentity.ANY));

      testRemoveSpecifiedNode.remove();
      session1.save();
   }

   public void testOperationsByOwner() throws Exception
   {
      Session session1 = repository.login(new CredentialsImpl("john", "exo".toCharArray()));
      Node accessTestRoot1 = session1.getRootNode().getNode("accessTestRoot");

      accessTestRoot1.addMixin("exo:privilegeable");

      Node testByOwnerNode = accessTestRoot1.addNode("testByOwnerNode");
      testByOwnerNode.addMixin("exo:owneable");
      testByOwnerNode.addMixin("exo:privilegeable");

      session1.save();
      session1.logout();

      accessTestRoot = (ExtendedNode)session.getRootNode().getNode("accessTestRoot");

      accessTestRoot.setPermission(accessTestRoot.getSession().getUserID(), PermissionType.ALL);
      accessTestRoot.removePermission("john");
      accessTestRoot.removePermission(IdentityConstants.ANY);
      accessTestRoot.setPermission("john", new String[]{PermissionType.READ});

      ExtendedNode testByOwnerNodeSystem = (ExtendedNode)accessTestRoot.getNode("testByOwnerNode");
      testByOwnerNodeSystem.setPermission(accessTestRoot.getSession().getUserID(), PermissionType.ALL);
      testByOwnerNodeSystem.removePermission("john");
      testByOwnerNodeSystem.removePermission(IdentityConstants.ANY);
      testByOwnerNodeSystem.setPermission("john", new String[]{PermissionType.READ});

      session.save();

      session1 = repository.login(new CredentialsImpl("john", "exo".toCharArray()));
      accessTestRoot1 = session1.getRootNode().getNode("accessTestRoot");
      testByOwnerNode = accessTestRoot1.getNode("testByOwnerNode");
      try
      {
         Property prop = testByOwnerNode.setProperty("prop1", "val1");
         session1.save();
         prop.remove();
         session1.save();
         Node test2 = testByOwnerNode.addNode("test2");
         session1.save();
         test2.remove();
         session1.save();
      }
      catch (AccessControlException e)
      {
         fail("AccessControlException should not have been thrown ");
      }

      //john is node owner so he can remove no matter what permission are assigned to node
      testByOwnerNode.remove();
      session1.save();
   }

   public void testRemoveExoOwnable() throws Exception
   {
      ExtendedNode testRoot = (ExtendedNode)accessTestRoot.addNode("testRemoveExoOwnable");
      testRoot.addMixin("exo:privilegeable");
      testRoot.setPermission("john", new String[]{PermissionType.READ, PermissionType.ADD_NODE,
         PermissionType.SET_PROPERTY});
      testRoot.setPermission(accessTestRoot.getSession().getUserID(), PermissionType.ALL);
      testRoot.removePermission(IdentityConstants.ANY);

      ExtendedNode subRoot = (ExtendedNode)testRoot.addNode("subroot");
      accessTestRoot.getSession().save();

      Session session1 = repository.login(new CredentialsImpl("john", "exo".toCharArray()));
      Node accessTestRoot1 = session1.getRootNode().getNode("accessTestRoot");
      Node testRoot1 = accessTestRoot1.getNode("testRemoveExoOwnable");

      ExtendedNode subRoot1 = (ExtendedNode)testRoot1.getNode("subroot");
      subRoot1.addMixin("exo:owneable");
      assertEquals("john", subRoot1.getProperty("exo:owner").getString());
      assertEquals("john", subRoot1.getACL().getOwner());
      Node testNode = subRoot1.addNode("node");
      session1.save();
      session1.logout();

      Session session2 = repository.login(new CredentialsImpl("john", "exo".toCharArray()));

      ExtendedNode subRoot2 =
         (ExtendedNode)session2.getRootNode().getNode("accessTestRoot/testRemoveExoOwnable/subroot");
      assertEquals("john", subRoot2.getProperty("exo:owner").getString());
      assertEquals("john", subRoot2.getACL().getOwner());
      Node testNode2 = subRoot2.getNode("node");
      testNode2.remove();

      session2.save();

      testRoot.remove();
      accessTestRoot.getSession().save();
   }

   public void testAnonim() throws RepositoryException
   {
      ExtendedNode testNode = (ExtendedNode)accessTestRoot.addNode("testAnonim");
      // node.addMixin("exo:accessControllable");
      testNode.addMixin("exo:owneable");
      testNode.addMixin("exo:privilegeable");
      session.save();

      Session anonimSession = repository.login(new CredentialsImpl(IdentityConstants.ANONIM, "".toCharArray()));
      // try {
      // anonimSession.checkPermission(testNode.getPath(), PermissionType.READ);
      // anonimSession.getRootNode().getNode("."+testNode.getPath());
      // anonimSession.checkPermission(testNode.getPath(), PermissionType.REMOVE);
      // fail();
      // } catch (AccessControlException e) {
      // //ok
      // }

      testNode.setPermission(testNode.getSession().getUserID(), PermissionType.ALL);
      testNode.removePermission(IdentityConstants.ANY);
      session.save();
      try
      {
         anonimSession.checkPermission("." + testNode.getPath(), PermissionType.READ);
         fail();
      }
      catch (AccessControlException e)
      {

      }

      testNode.setPermission(IdentityConstants.ANY, new String[]{PermissionType.READ});
      session.save();

      try
      {
         anonimSession.checkPermission(testNode.getPath(), PermissionType.READ);
         anonimSession.getRootNode().getNode("." + testNode.getPath());
         anonimSession.checkPermission(testNode.getPath(), PermissionType.REMOVE);
         fail();
      }
      catch (AccessControlException e)
      {

      }
      testNode.removePermission(IdentityConstants.ANY);
      session.save();
      try
      {
         anonimSession.checkPermission("." + testNode.getPath(), PermissionType.READ);
         fail();
      }
      catch (AccessControlException e)
      {

      }
      testNode.setPermission(IdentityConstants.ANY, new String[]{PermissionType.READ, PermissionType.SET_PROPERTY,
         PermissionType.REMOVE});
      session.save();

      try
      {
         anonimSession.checkPermission(testNode.getPath(), PermissionType.READ);
         anonimSession.getRootNode().getNode("." + testNode.getPath());
         anonimSession.checkPermission(testNode.getPath(), PermissionType.SET_PROPERTY);
         anonimSession.checkPermission(testNode.getPath(), PermissionType.REMOVE);
         anonimSession.checkPermission(testNode.getPath(), PermissionType.ADD_NODE);
         fail();
      }
      catch (AccessControlException e)
      {

      }

      try
      {
         anonimSession.checkPermission(testNode.getPath(), PermissionType.READ + "," + PermissionType.ADD_NODE);
         fail();
      }
      catch (AccessControlException e)
      {

      }
      try
      {
         anonimSession.checkPermission(testNode.getPath(), PermissionType.CHANGE_PERMISSION);
         fail();
      }
      catch (AccessControlException e)
      {

      }
   }

   public void testDualCheckPermissions() throws Exception
   {
      ExtendedNode testRoot = (ExtendedNode)accessTestRoot.addNode("DualCheckPermissions");

      testRoot.addMixin("exo:privilegeable");
      testRoot.setPermission("john", new String[]{PermissionType.READ, PermissionType.ADD_NODE,
         PermissionType.SET_PROPERTY});
      testRoot.setPermission(accessTestRoot.getSession().getUserID(), PermissionType.ALL);
      testRoot.removePermission(IdentityConstants.ANY);
      accessTestRoot.save();

      AccessManager accessManager = ((SessionImpl)accessTestRoot.getSession()).getAccessManager();

      SessionImpl session1 = (SessionImpl)repository.login(new CredentialsImpl("john", "exo".toCharArray()));

      assertTrue(accessManager.hasPermission(testRoot.getACL(), new String[]{PermissionType.READ,
         PermissionType.ADD_NODE}, session1.getUserState().getIdentity()));

      assertTrue(accessManager.hasPermission(testRoot.getACL(), new String[]{PermissionType.READ,
         PermissionType.SET_PROPERTY}, session1.getUserState().getIdentity()));

      assertTrue(accessManager.hasPermission(testRoot.getACL(), new String[]{PermissionType.ADD_NODE,
         PermissionType.SET_PROPERTY}, session1.getUserState().getIdentity()));

      assertFalse(accessManager.hasPermission(testRoot.getACL(), new String[]{PermissionType.READ,
         PermissionType.REMOVE}, session1.getUserState().getIdentity()));
   }

   /**
    * Test possibility of removing of all permissions.
    * 
    * @throws Exception
    */
   public void testEmptyPermissions() throws Exception
   {
      ExtendedNode testRoot = (ExtendedNode)accessTestRoot.addNode("testEmptyPermissions");
      testRoot.addMixin("exo:privilegeable");
      session.save();

      testRoot.removePermission(IdentityConstants.ANY);

      try
      {
         session.checkPermission(testRoot.getPath(), PermissionType.READ);
         fail();
      }
      catch (AccessControlException e1)
      {
         // ok
      }

      try
      {
         session.save();
         fail();
      }
      catch (RepositoryException e)
      {
         // ok
      }

      session.refresh(false);

      ExtendedNode testRoot2 = (ExtendedNode)accessTestRoot.addNode("testEmptyPermissions2");
      testRoot2.addMixin("exo:privilegeable");
      session.save();

      testRoot2.setPermission(IdentityConstants.ANY, new String[]{});
      try
      {
         session.save();
         fail();
      }
      catch (RepositoryException e)
      {
         // ok
      }
   }

   /**
    * Testing of correct parsing of string representation of permissions values.
    * 
    * @throws Exception
    */
   public void testPerseEntries() throws Exception
   {
      ExtendedNode testRoot = (ExtendedNode)accessTestRoot.addNode("testPerseEntries");
      testRoot.addMixin("exo:privilegeable");
      session.save();

      testRoot.setPermission(IdentityConstants.ANY, PermissionType.ALL);
      session.save();

      session.checkPermission(testRoot.getPath(), PermissionType.ADD_NODE);
      session.checkPermission(testRoot.getPath(), PermissionType.SET_PROPERTY);
      session.checkPermission(testRoot.getPath(), PermissionType.READ);
      session.checkPermission(testRoot.getPath(), PermissionType.REMOVE);

      try
      {
         session.checkPermission(testRoot.getPath(), "bla-bla");
      }
      catch (AccessControlException e)
      {

      }
      try
      {
         session.checkPermission(testRoot.getPath(), "");
      }
      catch (AccessControlException e)
      {

      }
   }

   /**
    * Test possibility of removing of all user permissions.
    * 
    * @throws Exception
    */
   public void testRemoveAllPermissions() throws Exception
   {
      Session session1 = repository.login(new CredentialsImpl("john", "exo".toCharArray()));
      ExtendedNode testNode = (ExtendedNode)session1.getRootNode().addNode("testNode");
      testNode.addMixin("exo:privilegeable");
      session1.save();
      Map<String, String[]> permissions = new HashMap<String, String[]>();
      permissions.put("admin", PermissionType.ALL);

      testNode.setPermissions(permissions);

      try
      {
         testNode.addNode("d");
         fail();
      }
      catch (AccessDeniedException e)
      {
         // ok
      }
      session1.save();
      try
      {
         testNode.addNode("d");
         fail();
      }
      catch (AccessDeniedException e)
      {
         // ok
      }

      Node testNodeAdmin = session.getRootNode().getNode("testNode");
      testNodeAdmin.remove();
   }

   /**
    * Check permission after import
    * 
    * @throws Exception
    */
   public void testPermissionAfterImport() throws Exception
   {
      Session session1 = repository.login(new CredentialsImpl("root", "exo".toCharArray()));
      InputStream importStream = BaseStandaloneTest.class.getResourceAsStream("/import-export/testPermdocview.xml");
      session1.importXML("/", importStream, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
      session1.save();
      // After import
      ExtendedNode testNode = (ExtendedNode)session1.getItem("/a");
      List<AccessControlEntry> permsList = testNode.getACL().getPermissionEntries();
      int permsListTotal = 0;
      for (AccessControlEntry ace : permsList)
      {
         String id = ace.getIdentity();
         String permission = ace.getPermission();
         if (id.equals("*:/platform/administrators") || id.equals("root"))
         {
            assertTrue(permission.equals(PermissionType.READ) || permission.equals(PermissionType.REMOVE)
               || permission.equals(PermissionType.SET_PROPERTY) || permission.equals(PermissionType.ADD_NODE));
            permsListTotal++;
         }
         else if (id.equals("validator:/platform/users"))
         {
            assertTrue(permission.equals(PermissionType.READ) || permission.equals(PermissionType.SET_PROPERTY));
            permsListTotal++;
         }
      }
      assertEquals(10, permsListTotal);
      testNode.remove();
      session1.save();
   }

   public void testAccessControlEntryEquals() throws PathNotFoundException, RepositoryException
   {
      root.addNode("testNode");
      ExtendedNode testNode = (ExtendedNode)session.getItem("/testNode");
      AccessControlEntry perm = testNode.getACL().getPermissionEntries().get(0);

      assertTrue(perm.equals(perm));
      assertFalse(perm.equals(new Object()));
   }

   private void showPermissions(String path) throws RepositoryException
   {
      NodeImpl node = (NodeImpl)this.repository.getSystemSession().getRootNode().getNode(path);
      AccessControlList acl = node.getACL();
      if (log.isDebugEnabled())
      {
         log.debug("DUMP: " + acl.dump());
      }
   }
}
