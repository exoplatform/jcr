/*
 * Copyright (C) 2011 eXo Platform SAS.
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
package org.exoplatform.services.jcr.ext.hierarchy;

import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.ext.BaseStandaloneTest;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.impl.NewGroupListener;
import org.exoplatform.services.jcr.ext.hierarchy.impl.NewUserListener;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.impl.GroupImpl;
import org.exoplatform.services.organization.impl.UserImpl;
import org.exoplatform.services.security.ConversationState;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;

/**
 * @author <a href="mailto:nicolas.filotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public class TestNodeHierarchyCreator extends BaseStandaloneTest
{
   private NodeHierarchyCreator creator;
   private SessionProvider sessionProvider;
   private NewUserListener userListener;
   private NewGroupListener groupListener;
   private Session session;
   
   /**
    * @see org.exoplatform.services.jcr.ext.BaseStandaloneTest#setUp()
    */
   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      creator = (NodeHierarchyCreator)container.getComponentInstanceOfType(NodeHierarchyCreator.class);
      userListener = (NewUserListener)container.getComponentInstanceOfType(NewUserListener.class);
      groupListener = (NewGroupListener)container.getComponentInstanceOfType(NewGroupListener.class);
      sessionProvider = new SessionProvider(ConversationState.getCurrent());
      session = sessionProvider.getSession("ws1", repository);
   }

   /**
    * @see org.exoplatform.services.jcr.ext.BaseStandaloneTest#tearDown()
    */
   @Override
   protected void tearDown() throws Exception
   {
      creator = null;
      userListener = null;
      groupListener = null;
      if (session != null)
      {
         session.logout();
         session = null;
      }
      if (sessionProvider != null)
      {
         sessionProvider.close();
         sessionProvider = null;
      }
      super.tearDown();
   }
   
   public void testGetJcrPath() throws Exception
   {
      assertEquals("/exo:applications", creator.getJcrPath("eXoApplications"));
      assertEquals("/exo:services", creator.getJcrPath("eXoServices"));
      assertEquals("/Users", creator.getJcrPath("usersPath"));
      assertEquals("/Groups/Path/Home", creator.getJcrPath("groupsPath"));
   }
   
   public void testInit() throws Exception
   {
      Node node = (Node)session.getItem("/exo:services");
      assertNotNull(node);
      assertTrue(node.isNodeType("nt:folder"));
      assertTrue(node.canAddMixin("mix:referenceable"));
      assertTrue(node.canAddMixin("exo:privilegeable"));
      
      node = (Node)session.getItem("/exo:applications");
      assertNotNull(node);
      assertFalse(node.isNodeType("nt:folder"));
      assertFalse(node.canAddMixin("mix:referenceable"));
      assertTrue(node.canAddMixin("exo:privilegeable"));
      
      node = (Node)session.getItem("/Users");
      assertNotNull(node);
      assertFalse(node.isNodeType("nt:folder"));
      assertTrue(node.canAddMixin("mix:referenceable"));
      assertFalse(node.canAddMixin("exo:privilegeable"));
      assertTrue(((ExtendedNode)node).getACL().hasPermissions());
      assertNotNull(((ExtendedNode)node).getACL().getPermissions("*:/platform/administrators"));
      
      node = (Node)session.getItem("/Groups/Path/Home");
      assertNotNull(node);
      assertTrue(node.isNodeType("nt:unstructured"));
      assertFalse(node.canAddMixin("mix:referenceable"));
      assertFalse(node.canAddMixin("exo:privilegeable"));
      assertTrue(((ExtendedNode)node).getACL().hasPermissions());
      assertNotNull(((ExtendedNode)node).getACL().getPermissions("*:/platform/administrators"));
      assertTrue(node.getParent().isNodeType("nt:unstructured"));
      assertFalse(node.getParent().canAddMixin("mix:referenceable"));
      assertFalse(node.getParent().canAddMixin("exo:privilegeable"));
      assertTrue(((ExtendedNode)node.getParent()).getACL().hasPermissions());
      assertNotNull(((ExtendedNode)node.getParent()).getACL().getPermissions("*:/platform/administrators"));
      assertTrue(node.getParent().getParent().isNodeType("nt:unstructured"));
      assertFalse(node.getParent().getParent().canAddMixin("mix:referenceable"));
      assertFalse(node.getParent().getParent().canAddMixin("exo:privilegeable"));
      assertTrue(((ExtendedNode)node.getParent().getParent()).getACL().hasPermissions());
      assertNotNull(((ExtendedNode)node.getParent().getParent()).getACL().getPermissions("*:/platform/administrators"));
   }   
   
   public void testGetUserNode() throws Exception
   {      
      Node node = creator.getUserNode(sessionProvider, "foo");
      assertNotNull(node);
      assertTrue(node.getPath().startsWith("/Users"));
   }
   
   public void testGetUserApplicationNode() throws Exception
   {
      User user = new UserImpl("foo");
      userListener.preSave(user, true);
      Node node = creator.getUserApplicationNode(sessionProvider, "foo");
      assertNotNull(node);
      assertTrue(node.isNodeType("nt:folder"));
      assertFalse(node.canAddMixin("mix:referenceable"));
      assertFalse(node.canAddMixin("exo:privilegeable"));
      assertTrue(((ExtendedNode)node).getACL().hasPermissions());
      assertNotNull(((ExtendedNode)node).getACL().getPermissions("*:/platform/administrators"));      
      Node parentNode = creator.getUserNode(sessionProvider, "foo");
      assertNotNull(parentNode);
      assertTrue(node.getPath().startsWith(parentNode.getPath()));
      assertNotNull(session.getItem(node.getPath()));
      userListener.preDelete(user);
      try
      {
         session.getItem(node.getPath());
         fail("A PathNotFoundException is expected");
      }
      catch (PathNotFoundException e)
      {
         // ignore me
      }
   }
   
   public void testGroupsNode() throws Exception
   {
      GroupImpl group1 = new GroupImpl("platform");
      GroupImpl group2 = new GroupImpl();
      group2.setId("/platform/users");
      GroupImpl group3 = new GroupImpl("my-group-name2");
      group3.setParentId("/platform");
      
      groupListener.preSave(group1, true);
      Node node = (Node)session.getItem("/Groups/Path/Home/" + group1.getGroupName());
      assertNotNull(node);
      node = node.getNode("ApplicationData");
      assertTrue(node.isNodeType("nt:folder"));
      assertFalse(node.canAddMixin("mix:referenceable"));
      assertFalse(node.canAddMixin("exo:privilegeable"));
      assertTrue(((ExtendedNode)node).getACL().hasPermissions());
      assertNotNull(((ExtendedNode)node).getACL().getPermissions("*:/platform/administrators"));      
           
      groupListener.preSave(group2, true);
      node = (Node)session.getItem("/Groups/Path/Home" + group2.getId());
      assertNotNull(node);
      node = node.getNode("ApplicationData");
      assertTrue(node.isNodeType("nt:folder"));
      assertFalse(node.canAddMixin("mix:referenceable"));
      assertFalse(node.canAddMixin("exo:privilegeable"));
      assertTrue(((ExtendedNode)node).getACL().hasPermissions());
      assertNotNull(((ExtendedNode)node).getACL().getPermissions("*:/platform/administrators"));      
      groupListener.preDelete(group2);
      try
      {
         session.getItem("/Groups/Path/Home" + group2.getId());
         fail("A PathNotFoundException is expected");
      }
      catch (PathNotFoundException e)
      {
         // ignore me
      }      
      groupListener.preSave(group3, true);
      node = (Node)session.getItem("/Groups/Path/Home" + group3.getParentId() + "/" + group3.getGroupName());
      assertNotNull(node);
      node = node.getNode("ApplicationData");
      assertTrue(node.isNodeType("nt:folder"));
      assertFalse(node.canAddMixin("mix:referenceable"));
      assertFalse(node.canAddMixin("exo:privilegeable"));
      assertTrue(((ExtendedNode)node).getACL().hasPermissions());
      assertNotNull(((ExtendedNode)node).getACL().getPermissions("*:/platform/administrators"));      
      groupListener.preDelete(group3);
      try
      {
         session.getItem("/Groups/Path/Home" + group3.getParentId() + "/" + group3.getGroupName());
         fail("A PathNotFoundException is expected");
      }
      catch (PathNotFoundException e)
      {
         // ignore me
      }      
      groupListener.preDelete(group1);
      try
      {
         session.getItem("/Groups/Path/Home/" + group1.getGroupName());
         fail("A PathNotFoundException is expected");
      }
      catch (PathNotFoundException e)
      {
         // ignore me
      }      
   }
}
