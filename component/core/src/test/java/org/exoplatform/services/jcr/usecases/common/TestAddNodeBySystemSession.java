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
package org.exoplatform.services.jcr.usecases.common;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;

public class TestAddNodeBySystemSession
   extends BaseUsecasesTest
{

   protected void tearDown() throws Exception
   {

      // [PN] Clean it!!! As BaseUsecasesTest.tearDown() don't touch jcr:system descendants
      try
      {
         session.refresh(false);
         Node jcrSystem = session.getRootNode().getNode("jcr:system");
         jcrSystem.getNode("Node1").remove();
         session.save();
      }
      catch (RepositoryException e)
      {
         log.error("Error of tearDown " + e.getMessage());
      }

      super.tearDown();
   }

   public void testAddNodeBySystemSession() throws Exception
   {
      String workspaceName = repository.getSystemWorkspaceName();
      Session session2 = repository.getSystemSession(workspaceName);
      Node jcrSystem = session2.getRootNode().getNode("jcr:system");
      // Add node1 on jcr:system
      try
      {
         jcrSystem.getNode("Node1");
         fail("There is Node 1");
      }
      catch (Exception e)
      {
         jcrSystem.addNode("Node1", "nt:unstructured");
         jcrSystem.save();
         session2.save();
      }
      Node node1 = (Node) session2.getItem("/jcr:system/Node1");
      assertFalse("Node1 hasn't got childnode", node1.hasNodes());
      // add Node/testNode on jcr:system
      try
      {
         jcrSystem.getNode("Node1/testNode");
         fail("There is Node1/testNode");
      }
      catch (Exception e)
      {
         jcrSystem.addNode("Node1/testNode");
         jcrSystem.save();
         session2.save();
      }
      // we can get testNode directly
      Node testNode = (Node) session2.getItem("/jcr:system/Node1/testNode");
      assertNotNull("testNode is found", testNode);
      // but we can't get testNode by Node1.getNode()
      node1 = (Node) session2.getItem("/jcr:system/Node1");
      assertTrue("==========> Node1 has got childNode", node1.hasNodes());
   }
}
