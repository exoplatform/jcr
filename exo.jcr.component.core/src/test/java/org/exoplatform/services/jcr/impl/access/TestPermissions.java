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
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.version.Version;

/**
 * Test is run on special repository db2.<br>
 * System workspace 'ws' has permissions configuration: <br>
 * <br>
 * *:/platform/administrators read;*:/platform/administrators add_node;
 * *:/platform/administrators set_property;*:/platform/administrators remove
 * <br><br>
 * Workspace 'ws1' has permissions configuration:<br>
 * <br>
 * any read;any add_node;any set_property;any remove
 * 
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
      if (sessionWS1.getRootNode().hasNode("MARY-ReadOnly"))
      {
         sessionWS1.getRootNode().getNode("MARY-ReadOnly").remove();
      }

      if (sessionWS1.getRootNode().hasNode("MARY-ReadWrite"))
      {
         sessionWS1.getRootNode().getNode("MARY-ReadWrite").remove();
      }
      sessionWS1.save();

      sessionMaryWS.logout();
      sessionMaryWS1.logout();
      sessionWS.logout();
      sessionWS1.logout();

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
      SessionImpl sessionJohnWS1 = null;

      try
      {
         Credentials credentials = new CredentialsImpl("john", "exo".toCharArray());
         sessionJohnWS1 = (SessionImpl)repositoryService.getRepository("db2").login(credentials, "ws1");
         Node vNode = sessionJohnWS1.getRootNode().getNode("testAccessPermission");
         assertNotNull(vNode);
         vNode = vNode.getVersionHistory().getVersion("1");
         assertNotNull(vNode);
         vNode = vNode.getNode("jcr:frozenNode");
         assertNotNull(vNode);
         assertNotNull(vNode.getNode("subNode"));
      }
      finally
      {
         if (sessionJohnWS1 != null)
         {
            sessionJohnWS1.logout();
         }
      }
   }
}
