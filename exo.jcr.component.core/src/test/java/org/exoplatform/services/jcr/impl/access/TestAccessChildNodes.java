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
package org.exoplatform.services.jcr.impl.access;

import org.exoplatform.services.jcr.BaseStandaloneTest;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.access.SystemIdentity;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.impl.core.NodeImpl;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TestAccessChildNodes.java 111 2008-11-11 11:11:11Z serg $
 */
public class TestAccessChildNodes extends BaseStandaloneTest
{

   @Override
   public String getRepositoryName()
   {
      return "db1";
   }

   public void setUp() throws Exception
   {
      super.setUp();
      //create nodes with "john" user
      Session sessJohn = repository.login(new CredentialsImpl("john", "exo".toCharArray()));
      Node testRoot = sessJohn.getRootNode().addNode("testRoot");
      testRoot.addMixin("exo:privilegeable");
      testRoot.setProperty("prop", "value");
      sessJohn.save();
      sessJohn.logout();
   }

   public void tearDown() throws Exception
   {
      Session sysSession = this.repository.getSystemSession(session.getWorkspace().getName());
      if (sysSession.getRootNode().hasNode("testRoot"))
      {
         Node testRoot = sysSession.getRootNode().getNode("testRoot");
         testRoot.remove();
         sysSession.save();
      }
      super.tearDown();
   }

   /**
    * Remove the parent and child node with a user that can remove the parent node but not the sub node,
    * and check that an AccessDeniedException occurs.
    * @throws Exception
    */
   public void testUserCanRemoveParentButCanNotRemoveChild() throws Exception
   {
      // login as Mary and create subNode
      Session sessMary = repository.login(new CredentialsImpl("mary", "exo".toCharArray()));
      Node testRoot = sessMary.getRootNode().getNode("testRoot");
      NodeImpl subNode = (NodeImpl)testRoot.addNode("subNode");
      subNode.addMixin("exo:privilegeable");
      sessMary.save();

      subNode.setPermission("mary", PermissionType.ALL);
      subNode.removePermission("john");
      subNode.removePermission(SystemIdentity.ANY);
      sessMary.save();
      sessMary.logout();

      // login as John and try remove subnode
      Session sessJohn = repository.login(new CredentialsImpl("john", "exo".toCharArray()));
      try
      {
         subNode = (NodeImpl)sessJohn.getRootNode().getNode("testRoot").getNode("subNode");
         subNode.remove();
         sessJohn.save();
         fail("There must be AccessDeniedException");
      }
      catch (AccessDeniedException e)
      {
         //Ok
      }

      // try to remove parent node node
      sessJohn.refresh(false);
      try
      {
         testRoot = sessJohn.getRootNode().getNode("testRoot");
         testRoot.remove();
         sessJohn.save();
         fail("There must be AccessDeniedException");
      }
      catch (AccessDeniedException e)
      {
         //Ok
      }
      finally
      {
         sessJohn.logout();
      }

      // now try with all permissions
      sessMary = repository.login(new CredentialsImpl("mary", "exo".toCharArray()));
      testRoot = sessMary.getRootNode().getNode("testRoot");
      subNode = (NodeImpl)testRoot.getNode("subNode");
      subNode.setPermission(SystemIdentity.ANY, PermissionType.ALL);
      sessMary.save();
      sessMary.logout();

      sessJohn = repository.login(new CredentialsImpl("john", "exo".toCharArray()));
      testRoot = sessJohn.getRootNode().getNode("testRoot");
      testRoot.remove();
      sessJohn.save();
      sessJohn.logout();
   }

   /**
    * Remove the sub node with a user that can remove the sub node (but not the parent node),
    * and check that the remove operation was successful.
    * @throws Exception
    */
   public void testUserCanNotRemoveParentButCanRemoveChild() throws Exception
   {
      // login as Mary and create subNode
      Session sessMary = repository.login(new CredentialsImpl("mary", "exo".toCharArray()));
      NodeImpl testRoot = (NodeImpl)sessMary.getRootNode().getNode("testRoot");
      NodeImpl subNode = (NodeImpl)testRoot.addNode("subNode");
      subNode.addMixin("exo:privilegeable");
      sessMary.save();

      //set permissions
      subNode.setPermission("mary", PermissionType.ALL);
      subNode.removePermission("john");
      subNode.removePermission(SystemIdentity.ANY);
      sessMary.save();

      testRoot.setPermission("john", PermissionType.ALL);
      testRoot.removePermission("mary");
      testRoot.setPermission("mary", new String[]{PermissionType.READ});
      testRoot.removePermission(SystemIdentity.ANY);
      sessMary.save();
      sessMary.logout();

      //try to remove parent as Mary - must fail
      sessMary = repository.login(new CredentialsImpl("mary", "exo".toCharArray()));
      try
      {
         testRoot = (NodeImpl)sessMary.getRootNode().getNode("testRoot");
         testRoot.remove();
         sessMary.save();
         fail("There must be AccessDeniedException");
      }
      catch (AccessDeniedException e)
      {
         //Ok
      }
      sessMary.refresh(false);

      // remove subnode as mary 
      subNode = (NodeImpl)sessMary.getRootNode().getNode("testRoot").getNode("subNode");
      subNode.remove();
      sessMary.save();
      assertFalse(sessMary.getRootNode().getNode("testRoot").hasNode("subNode"));
      sessMary.logout();
   }

   /**
    * Remove the property with a user that cannot remove the parent node, and check that an AccessDeniedException occurs
    * @throws Exception
    */
   public void testRemovePropertyWithoutPermissionOnParent() throws Exception
   {
      // login as Mary and set permissions on parent node
      Session sessMary = repository.login(new CredentialsImpl("mary", "exo".toCharArray()));
      NodeImpl testRoot = (NodeImpl)sessMary.getRootNode().getNode("testRoot");

      testRoot.removePermission("mary");
      testRoot.setPermission("mary", new String[]{PermissionType.READ});
      testRoot.removePermission(SystemIdentity.ANY);
      sessMary.save();
      sessMary.logout();

      //try to remove parent's property as Mary - must fail
      sessMary = repository.login(new CredentialsImpl("mary", "exo".toCharArray()));
      try
      {
         testRoot = (NodeImpl)sessMary.getRootNode().getNode("testRoot");
         testRoot.getProperty("prop").remove();
         sessMary.save();
         fail("There must be AccessDeniedException");
      }
      catch (AccessDeniedException e)
      {
         //Ok
      }
      finally
      {
         sessMary.logout();
      }
   }

   /**
    * Remove the property with a user that can remove the parent node, and check that the remove operation was successful.
    * @throws Exception
    */
   public void testRemovePropertyWithPermissionOnParent() throws Exception
   {
      // login as Mary and set permissions on parent node
      Session sessMary = repository.login(new CredentialsImpl("mary", "exo".toCharArray()));
      NodeImpl testRoot = (NodeImpl)sessMary.getRootNode().getNode("testRoot");

      testRoot.removePermission("mary");
      testRoot.setPermission("mary", PermissionType.ALL);
      testRoot.removePermission(SystemIdentity.ANY);
      sessMary.save();
      sessMary.logout();

      //try to remove parent's property as Mary - must fail
      sessMary = repository.login(new CredentialsImpl("mary", "exo".toCharArray()));

      testRoot = (NodeImpl)sessMary.getRootNode().getNode("testRoot");
      testRoot.getProperty("prop").remove();
      sessMary.save();

      assertFalse(sessMary.getRootNode().getNode("testRoot").hasProperty("prop"));
      sessMary.logout();
   }

}
