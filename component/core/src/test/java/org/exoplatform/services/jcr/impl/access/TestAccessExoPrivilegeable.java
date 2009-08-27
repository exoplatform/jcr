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

import java.security.AccessControlException;

import javax.jcr.AccessDeniedException;
import javax.jcr.Property;
import javax.jcr.Session;

import org.exoplatform.services.jcr.BaseStandaloneTest;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.impl.core.NodeImpl;

/**
 * Created by The eXo Platform SAS.<br/> Prerequisite: enable access control i.e.
 * <access-control>optional</access-control>
 * 
 * @author Gennady Azarenkov
 * @version $Id:TestAccessExoPrivilegeable.java 12535 2007-02-02 15:39:26Z peterit $
 */

public class TestAccessExoPrivilegeable
   extends BaseStandaloneTest
{

   private ExtendedNode accessTestRoot;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      accessTestRoot = (ExtendedNode) session.getRootNode().addNode("accessTestRoot");
      session.save();
   }

   public String getRepositoryName()
   {
      return "db1";
   }

   /**
    * tests session.checkPermission() method
    * 
    * @throws Exception
    */
   public void testSessionCheckPermission() throws Exception
   {
      NodeImpl node = null;
      node = (NodeImpl) accessTestRoot.addNode("testSessionCheckPermission");

      node.addMixin("exo:owneable");
      node.addMixin("exo:privilegeable");

      // good style of set permission
      // 1. set for me
      // 2. set for others
      // 3. remove for any
      node.setPermission("exo", PermissionType.ALL);
      node.setPermission("john", new String[]
      {PermissionType.READ});
      node.removePermission("any");
      session.save();

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

   public void testSubNodePermissions() throws Exception
   {
      NodeImpl newNode = (NodeImpl) accessTestRoot.addNode("node1");
      newNode.addMixin("exo:privilegeable");
      newNode.setPermission("exo", new String[]
      {PermissionType.READ});
      newNode.setPermission("*:/platform/administrators", PermissionType.ALL);
      newNode.removePermission("any");
      accessTestRoot.save();

      NodeImpl subnode = (NodeImpl) newNode.addNode("subnode");
      subnode.addMixin("exo:privilegeable");
      newNode.save();

      Session session1 = repository.login(new CredentialsImpl("exo", "exo".toCharArray()));
      try
      {
         subnode = (NodeImpl) session1.getItem(subnode.getPath());

         assertEquals("User 'exo' permissions are wrong", PermissionType.READ, subnode.getACL().getPermissions("exo")
                  .get(0));
         assertEquals("User 'exo' permissions are wrong", "exo " + PermissionType.READ, subnode.getProperty(
                  "exo:permissions").getValues()[0].getString());
      }
      finally
      {
         session1.logout();
      }
   }

   public void testSubNodeInheritedPermissions() throws Exception
   {
      NodeImpl newNode = (NodeImpl) accessTestRoot.addNode("node1");
      newNode.addMixin("exo:privilegeable");
      newNode.setPermission("exo", new String[]
      {PermissionType.READ});
      newNode.setPermission("*:/platform/administrators", PermissionType.ALL);
      newNode.removePermission("any");
      accessTestRoot.save();

      NodeImpl subnode = (NodeImpl) newNode.addNode("subnode");
      subnode.addMixin("exo:owneable");
      newNode.save();

      Session session1 = repository.login(new CredentialsImpl("exo", "exo".toCharArray()));
      try
      {
         subnode = (NodeImpl) session1.getItem(subnode.getPath());
         assertEquals("User 'exo' permissions are wrong", PermissionType.READ, subnode.getACL().getPermissions("exo")
                  .get(0));
      }
      finally
      {
         session1.logout();
      }
   }

   public void testGetNodeWithoutParentREAD() throws Exception
   {
      NodeImpl newNode = (NodeImpl) accessTestRoot.addNode("node1");
      newNode.addMixin("exo:privilegeable");
      newNode.setPermission("exo", new String[]
      {PermissionType.SET_PROPERTY});
      newNode.setPermission("*:/platform/administrators", PermissionType.ALL);
      newNode.removePermission("any");
      Property p = newNode.setProperty("property", "property");
      NodeImpl n = (NodeImpl) newNode.addNode("subnode");
      Property np = n.setProperty("property1", "property1");
      n.addMixin("exo:privilegeable");
      n.setPermission("exo", new String[]
      {PermissionType.READ, PermissionType.SET_PROPERTY});

      accessTestRoot.save();

      // user exo will try set property
      Session session1 = repository.login(new CredentialsImpl("exo", "exo".toCharArray()));
      NodeImpl acr = (NodeImpl) session1.getItem(accessTestRoot.getPath());
      try
      {
         acr.getNode("node1");
         fail("Node " + newNode.getPath() + " has no permissions for read by 'exo'");
      }
      catch (AccessDeniedException e)
      {
         // ok
      }

      try
      {
         assertNotNull("Node should be accessible", acr.getNode("node1/subnode"));
      }
      catch (AccessDeniedException e)
      {
         e.printStackTrace();
         fail("User 'exo' shoould be able to get the node " + n.getPath());
      }

      try
      {
         assertNotNull("Property should be accessible", acr.getProperty("node1/subnode/property1"));
      }
      catch (AccessDeniedException e)
      {
         e.printStackTrace();
         fail("User 'exo' shoould be able to get the property " + np.getPath());
      }

      try
      {
         assertNotNull("Node should be accessible", session1.getItem(n.getPath()));
      }
      catch (AccessDeniedException e)
      {
         e.printStackTrace();
         fail("User 'exo' shoould be able to get the node " + n.getPath());
      }

      try
      {
         assertNotNull("Property should be accessible", session1.getItem(np.getPath()));
      }
      catch (AccessDeniedException e)
      {
         e.printStackTrace();
         fail("User 'exo' shoould be able to get the property " + np.getPath());
      }
   }

   public void testGetPropertyWithoutParentREAD() throws Exception
   {
      NodeImpl newNode = (NodeImpl) accessTestRoot.addNode("node1");
      newNode.addMixin("exo:privilegeable");
      newNode.setPermission("exo", new String[]
      {PermissionType.ADD_NODE});
      newNode.setPermission("*:/platform/administrators", PermissionType.ALL);
      newNode.removePermission("any");
      Property p = newNode.setProperty("property", "property");

      accessTestRoot.save();

      // user exo will try set property
      Session session1 = repository.login(new CredentialsImpl("exo", "exo".toCharArray()));
      NodeImpl acr = (NodeImpl) session1.getItem(accessTestRoot.getPath());

      // property it's a node rights issue
      try
      {
         acr.getProperty("node1/property").getString();
         fail("User 'exo' hasn't rights to get property " + p.getPath());
      }
      catch (AccessDeniedException e)
      {
         // ok
      }
   }
}
