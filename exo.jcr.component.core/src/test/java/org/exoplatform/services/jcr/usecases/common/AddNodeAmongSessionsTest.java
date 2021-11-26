/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.usecases.common;

import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

public class AddNodeAmongSessionsTest extends BaseUsecasesTest
{
   public void testAddNodeAmongSession() throws Exception
   {
      String workspaceName = repository.getSystemWorkspaceName();
      // Session systemSession =
      // repositoryService.getRepository().getSystemSession(workspaceName) ;
      Session systemSession = repository.login(new SimpleCredentials("admin", "admin".toCharArray()), workspaceName);

      Session addminSession = repository.login(new SimpleCredentials("admin", "admin".toCharArray()), workspaceName);

      Node jcrSystem = systemSession.getRootNode().getNode("jcr:system");
      try
      {
         systemSession.getItem("/jcr:system/Node1");
         fail("Node1 is exsited");
      }
      catch (Exception e)
      {
         jcrSystem.addNode("Node1", "nt:unstructured");
         systemSession.save();
      }
      Node node1 = (Node)addminSession.getItem("/jcr:system/Node1");
      assertTrue("Node1 is found", node1 != null);
      assertFalse(node1.hasNodes());

      node1.addNode("testNode");
      addminSession.save();

      node1 = addminSession.getRootNode().getNode("jcr:system/Node1");
      assertTrue("node1 has child Node", node1.hasNodes());

      // we can get testNode directly
      Node testNode1 = (Node)systemSession.getItem("/jcr:system/Node1/testNode");
      assertTrue(testNode1.getName().equals("testNode"));
      // Or we can get it via root Node

      // systemSession.refresh(true);

      Node rootNode = systemSession.getRootNode();
      assertTrue(rootNode.hasNode("jcr:system/Node1/testNode"));

      Node jcrSystemNode = rootNode.getNode("jcr:system");
      assertTrue(jcrSystemNode.hasNode("Node1/testNode"));
      // but we can't get it via its parent
      Node parentNode = jcrSystemNode.getNode("Node1");
      List perms = ((NodeImpl)parentNode).getACL().getPermissionEntries();

      assertTrue(parentNode.getNodes().getSize() > 0);
      assertTrue(parentNode.hasNode("testNode"));

      node1.remove();
      addminSession.save();
   }
}
