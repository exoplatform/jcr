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

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.MembershipEntry;

import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS
 * 
 * Date: 19.05.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestUserAccess.java 14464 2008-05-19 11:05:20Z pnedonosko $
 */
public class TestUserAccess extends JcrImplBaseTest
{

   private NodeImpl testRoot;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      testRoot = (NodeImpl)root.addNode("testUserAccess");
      root.save();
   }

   @Override
   protected void tearDown() throws Exception
   {
      Session sysSession = repository.getSystemSession(session.getWorkspace().getName());
      if (sysSession.getRootNode().hasNode("testUserAccess"))
      {
         Node testRoot = sysSession.getRootNode().getNode("testUserAccess");
         testRoot.remove();
         sysSession.save();
      }
      super.tearDown();
   }

   /**
    * Check if dedicated user has rights to a node with this user rights only.
    * 
    * @throws Exception
    */
   public void testUser() throws Exception
   {
      // Mary only node, Mary membership is '*:/exo', seems it's user
      NodeImpl maryNode = (NodeImpl) testRoot.addNode("mary");
      maryNode.addMixin("exo:privilegeable");
      if (!session.getUserID().equals("mary"))
      {
         maryNode.setPermission("mary", PermissionType.ALL);
         maryNode.removePermission(session.getUserID());
      }
      maryNode.removePermission(IdentityConstants.ANY);
      testRoot.save();

      try
      {
         Session marySession =
            repository.login(new CredentialsImpl("mary", "exo".toCharArray()), session.getWorkspace().getName());
         NodeImpl myNode = (NodeImpl)marySession.getItem(maryNode.getPath());
         Node test = myNode.addNode("test");
         test.setProperty("property", "any data");
         myNode.save();
         test.remove();
         myNode.save();
      }
      catch (AccessControlException e)
      {
         e.printStackTrace();
         fail(e.getMessage());
      }
   }

   /**
    * Check if admin user has rights to a node with this user rights only.
    * 
    * @throws Exception
    */
   public void testRoot() throws Exception
   {
      // root's only node, root membership is '*:/admin'
      NodeImpl rootNode = (NodeImpl)testRoot.addNode("root");
      rootNode.addMixin("exo:privilegeable");
      if (!session.getUserID().equals("root"))
      {
         rootNode.setPermission("root", PermissionType.ALL);
         rootNode.removePermission(session.getUserID());
      }
      rootNode.removePermission(IdentityConstants.ANY);
      testRoot.save();

      try
      {
         Session rootSession =
            repository.login(new CredentialsImpl("root", "exo".toCharArray()), session.getWorkspace().getName());
         NodeImpl myNode = (NodeImpl)rootSession.getItem(rootNode.getPath());
         Node test = myNode.addNode("test");
         test.setProperty("property", "any data");
         myNode.save();
         test.remove();
         myNode.save();
      }
      catch (AccessControlException e)
      {
         e.printStackTrace();
         fail(e.getMessage());
      }
   }

   /**
    * Check if root user has rights to a node with this user rights and rights for any to a read.
    * 
    * @throws Exception
    */
   public void testRootAndAnyRead() throws Exception
   {
      // root has all rights, any to read only
      NodeImpl rootNode = (NodeImpl)testRoot.addNode("root");
      rootNode.addMixin("exo:privilegeable");
      if (!session.getUserID().equals("root"))
         rootNode.setPermission("root", PermissionType.ALL);

      // set any to read only
      rootNode.setPermission(session.getUserID(), PermissionType.ALL); // temp all for current user
      rootNode.removePermission(IdentityConstants.ANY);
      rootNode.setPermission(IdentityConstants.ANY, new String[]{PermissionType.READ});
      rootNode.removePermission(session.getUserID()); // clean temp rights

      testRoot.save();

      try
      {
         Session rootSession =
            repository.login(new CredentialsImpl("root", "exo".toCharArray()), session.getWorkspace().getName());
         NodeImpl myNode = (NodeImpl)rootSession.getItem(rootNode.getPath());
         Node test = myNode.addNode("test");
         test.setProperty("property", "any data");
         myNode.save();
         test.remove();
         myNode.save();
      }
      catch (AccessControlException e)
      {
         e.printStackTrace();
         fail(e.getMessage());
      }
   }

   /**
    * Check if Dynamic user has rights to a node with user "mary".
    * 
    * @throws Exception
    */
   public void testDynamicUserRead() throws Exception
   {
      // Mary only node, Mary membership is '*:/platform/users', seems it's user
      NodeImpl maryNode = (NodeImpl) testRoot.addNode("mary_dynamic");
      maryNode.addMixin("exo:privilegeable");
      if (!session.getUserID().equals("mary"))
      {
         maryNode.setPermission("*:/platform/users", new String[] {PermissionType.READ});
         maryNode.setPermission("mary", PermissionType.ALL);
         maryNode.removePermission(session.getUserID());
      }
      maryNode.removePermission(IdentityConstants.ANY);
      testRoot.save();

      Session marySession =
                  repository.login(new CredentialsImpl("mary", "exo".toCharArray()), session.getWorkspace().getName());
      NodeImpl myNode = (NodeImpl) marySession.getItem(maryNode.getPath());
      Node test = myNode.addNode("test");
      test.setProperty("property", "any data");
      myNode.save();

      //Dynamic session fail read
      List<MembershipEntry> dynamicMembershipEntries = new ArrayList<MembershipEntry>();
      dynamicMembershipEntries.add(new MembershipEntry("/platform/administrators"));

      try
      {
         Session dynamicSession =
                  repository.getDynamicSession(session.getWorkspace().getName(), dynamicMembershipEntries);
         NodeImpl maryNodeDynamic = (NodeImpl) dynamicSession.getItem(maryNode.getPath());
         fail("Dynamic session with membership '*:/platform/users' should not read node with membership '*:/platform/users'");
      }
      catch (AccessDeniedException e)
      {
         //ok
      }

      //Dynamic session successful read
      dynamicMembershipEntries = new ArrayList<MembershipEntry>();
      dynamicMembershipEntries.add(new MembershipEntry("/platform/users"));

      //check get
      try
      {
         Session dynamicSession =
                  repository.getDynamicSession(session.getWorkspace().getName(), dynamicMembershipEntries);
         NodeImpl maryNodeDynamic = (NodeImpl) dynamicSession.getItem(maryNode.getPath());
         //ok
      }
      catch (AccessDeniedException e)
      {

         e.printStackTrace();
         fail("Dynamic session with membership '*:/platform/users' should read node with membership '*:/platform/users'. Exception message :"
                  + e.getMessage());
      }

      //check add
      try
      {
         Session dynamicSession =
                  repository.getDynamicSession(session.getWorkspace().getName(), dynamicMembershipEntries);
         NodeImpl maryNodeDynamic = (NodeImpl) dynamicSession.getItem(maryNode.getPath());

         maryNodeDynamic.addNode("test2");
         maryNodeDynamic.save();
         fail("Dynamic session with membership '*:/platform/users' should be not add child node with membership '*:/platform/users READ'");
      }
      catch (AccessDeniedException e)
      {
         //ok
      }

      //check remove
      try
      {
         Session dynamicSession =
                  repository.getDynamicSession(session.getWorkspace().getName(), dynamicMembershipEntries);
         NodeImpl maryNodeDynamic = (NodeImpl) dynamicSession.getItem(maryNode.getPath());

         maryNodeDynamic.getNode("test").remove();
         maryNodeDynamic.save();
         fail("Dynamic session with membership '*:/platform/users' should be not remove child node with membership '*:/platform/users READ'");
      }
      catch (AccessDeniedException e)
      {
         //ok
      }

   }

}
