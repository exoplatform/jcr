/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.services.jcr.api.nodetypes;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

import java.util.GregorianCalendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.Version;

/**
 * @author <a href="mailto:anatoliy.bazko@gmail.com">Anatoliy Bazko</a>
 * @version $Id: TestNodeDefinitionWCM.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class TestNodeDefinition extends JcrAPIBaseTest
{

   public void testNodeTypeWCM1() throws Exception
   {
      NodeImpl someNode = (NodeImpl)session.getRootNode().addNode("someNode");
      someNode.addMixin("mix:referenceable");

      NodeImpl exoWebContent = (NodeImpl)session.getRootNode().addNode("exoWebContent", "exo:webContent");
      exoWebContent.setProperty("exo:title", "tit");
      session.save();

      // add nt:file
      NodeImpl ntFile1 = (NodeImpl)exoWebContent.addNode("ntFile-1", "nt:file");
      NodeImpl jcrContent = (NodeImpl)ntFile1.addNode("jcr:content", "nt:resource");
      jcrContent.setProperty("jcr:data", "");
      jcrContent.setProperty("jcr:mimeType", "");
      jcrContent.setProperty("jcr:lastModified", new GregorianCalendar());
      session.save();

      exoWebContent.addMixin("exo:actionable");
      exoWebContent.setProperty("exo:actions", someNode);
      session.save();

      // add exo:actionStorage
      NodeImpl actionStorage = (NodeImpl)exoWebContent.addNode("actionStorage", "exo:actionStorage");
      session.save();

      // add nt:file
      NodeImpl ntFile2 = (NodeImpl)exoWebContent.addNode("ntFile-2", "nt:file");
      jcrContent = (NodeImpl)ntFile2.addNode("jcr:content", "nt:resource");
      jcrContent.setProperty("jcr:data", "");
      jcrContent.setProperty("jcr:mimeType", "");
      jcrContent.setProperty("jcr:lastModified", new GregorianCalendar());
      session.save();

      SessionImpl session = (SessionImpl)repository.login(credentials, WORKSPACE);

      ntFile1 = (NodeImpl)session.getRootNode().getNode("exoWebContent").getNode("ntFile-1");
      assertEquals(ntFile1.getDefinition().getRequiredPrimaryTypes()[0].getName(), "nt:base");

      ntFile2 = (NodeImpl)session.getRootNode().getNode("exoWebContent").getNode("ntFile-2");
      assertEquals(ntFile2.getDefinition().getRequiredPrimaryTypes()[0].getName(), "nt:base");

      actionStorage = (NodeImpl)session.getRootNode().getNode("exoWebContent").getNode("actionStorage");
      assertEquals(actionStorage.getDefinition().getRequiredPrimaryTypes()[0].getName(), "exo:actionStorage");
   }

   public void testNodeTypeWCM2() throws Exception
   {
      Node parent = session.getRootNode().addNode("parent", "exo:newsletterCategory");
      parent.addNode("child", "nt:unstructured");
      parent.setProperty("exo:newsletterCategoryTitle", "title");
      session.save();
   }

   public void testNTVersionedChild() throws Exception
   {
      Node folder1 = session.getRootNode().addNode("folder1", "nt:folder");
      Node folder2 = folder1.addNode("folder2", "nt:folder");
      Node folder3 = folder2.addNode("folder3", "nt:folder");
      folder1.addMixin("mix:versionable");
      folder3.addMixin("mix:versionable");
      session.save();

      Version ver1 = folder1.checkin();
      folder1.checkout();

      folder3 =
         (NodeImpl)session.getItem("/jcr:system/jcr:versionStorage/" + ver1.getParent().getUUID()
            + "/1/jcr:frozenNode/folder2/folder3");

      try
      {
         folder3.getDefinition();
      }
      catch (RepositoryException e)
      {
         fail();
      }
   }

   public void testNodeTypeNotDeterminedNtFolder() throws Exception
   {
      Node root = session.getRootNode();
      Node folder = root.addNode("abc", "nt:folder");

      try
      {
         folder.addNode("def");
         fail("Should be throw ConstraintViolationException, because node type for node \"def\" "
                  + "was not defined and default primary type for child nodes in \"nt:folder\" node type is not define.");
      }
      catch (ConstraintViolationException e)
      {
         // ok
      }
   }

   public void testNodeTypeNotDeterminedNtFile2() throws Exception
   {
      Node root = session.getRootNode();
      Node folder = root.addNode("abc", "nt:file");

      try
      {
         folder.addNode("jcr:content");
         fail("Should be throw ConstraintViolationException, because node type for node \"jcr:content\" "
                  + "was not defined and default primary type for child nodes in \"nt:file\" node type is not define.");
      }
      catch (ConstraintViolationException e)
      {
         // ok
      }
   }
}
