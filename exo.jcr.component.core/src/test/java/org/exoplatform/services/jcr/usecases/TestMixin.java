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
package org.exoplatform.services.jcr.usecases;

import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.impl.core.NodeImpl;

import java.io.ByteArrayInputStream;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TestMixin.java 111 2008-11-11 11:11:11Z serg $
 */
public class TestMixin extends BaseUsecasesTest
{

   /**
    * There is usecase when changes in NodeImpl fetched with getParent() do not 
    * cause on other NodeImpl objects.
    * 
    * @throws Exception
    */
   public void testMixin() throws Exception
   {
      Node a = root.addNode("a");
      a.addMixin("mix:referenceable");

      Property p = a.setProperty("prop", "string");
      Node a1 = p.getParent();

      a1.addMixin("mix:lockable");
      a.addMixin("mix:versionable");
      session.save();

      //check result
      Node a2 = root.getNode("a");
      assertTrue(a2.isNodeType("mix:referenceable"));
      assertTrue(a2.isNodeType("mix:versionable"));
      assertTrue(a2.isNodeType("mix:lockable"));
   }

   /**
    * User without ADD_NODE permission should be able to add mixin 
    * without auto created nodes.
    */
   public void testAllowSetMixinWithoutHavingAddNodePermissions() throws Exception
   {
      NodeImpl testNode = (NodeImpl)session.getRootNode().addNode("testNode");

      testNode.addMixin("exo:privilegeable");
      testNode.setPermission("john", new String[]{PermissionType.READ, PermissionType.SET_PROPERTY,
         PermissionType.REMOVE});
      testNode.removePermission("any");

      session.save();

      Session johnSession = repository.login(new CredentialsImpl("john", "exo".toCharArray()));

      try
      {
         johnSession.getRootNode().getNode("testNode").addMixin("mix:lockable");
         johnSession.save();
      }
      catch (AccessDeniedException e)
      {
         fail("Should have ability to set mixin without having ADD_NODE permission");
      }

      try
      {
         johnSession.getRootNode().getNode("testNode").addMixin("mix:versionable");
         johnSession.save();
      }
      catch (AccessDeniedException e)
      {
         fail("Should have ability to set mixin without having ADD_NODE permission");
      }

      try
      {
         johnSession.getRootNode().getNode("testNode").addMixin("exo:EXOJCR1812");
         johnSession.save();

         fail("Should not have ability to set mixin without having ADD_NODE permission");
      }
      catch (AccessDeniedException e)
      {
      }

      johnSession.logout();
   }

   /**
    * This usecase is checking correctness of remove mixin which is declared
    * extend another node type (primary or mixin one).
    * 
    * @throws RepositoryException
    */
   public void testMixinExtendedNtBase() throws RepositoryException
   {
      ExtendedNodeTypeManager manager = (ExtendedNodeTypeManager)workspace.getNodeTypeManager();

      String cnd = "<nodeTypes>" //
         + "<nodeType name=\"test:my\" isMixin=\"true\" hasOrderableChildNodes=\"false\" primaryItemName=\"\">" //
         + "   <supertypes>" //
         + "     <supertype>nt:base</supertype>" // main configuration part for the test
         + "   </supertypes>" //
         + "   <propertyDefinitions>" //
         + "   </propertyDefinitions>" //
         + "</nodeType>" //
         + "</nodeTypes>";

      manager.registerNodeTypes(new ByteArrayInputStream(cnd.getBytes()), ExtendedNodeTypeManager.IGNORE_IF_EXISTS,
         NodeTypeDataManager.TEXT_XML);

      Node folder = root.addNode("testRemoveMixin", "nt:folder");
      folder.addMixin("test:my");
      session.save();
      folder.removeMixin("test:my");
      try
      {
         session.save();
      }
      catch (Exception e)
      {
         fail("Shouldn't be removed a property definition, there is existed another node type (primary or mixin) which has the property definition. \n"
            + e.getMessage());
      }
   }
}
