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

public class JCRAddSystemNodeTest
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

   public void testActionsOnJcrSystem() throws Exception
   {
      String workspaceName = repository.getSystemWorkspaceName();
      // ---------------use System sestion
      Session session2 = repository.getSystemSession(workspaceName);
      // ----------- Use addmin session
      Node jcrSystem = session2.getRootNode().getNode("jcr:system");
      jcrSystem.addNode("Node1", "nt:unstructured");
      jcrSystem.save();
      session2.save();
      session2 = repositoryService.getRepository().getSystemSession(repository.getSystemWorkspaceName());
      assertTrue(session2.getRootNode().getNode("jcr:system").hasNodes());
      Node node1 = (Node) session2.getItem("/jcr:system/Node1");
      jcrSystem.save();
      session2.save();
      jcrSystem.addNode("Node1/testNode");
      node1 = (Node) session2.getItem("/jcr:system/Node1");
      node1.save();
      jcrSystem.save();
      session2.save();
      Node testNode = (Node) session2.getItem("/jcr:system/Node1/testNode");
      assertNotNull(testNode);
      node1 = (Node) session2.getItem("/jcr:system/Node1");
      assertTrue(node1.hasNodes());

   }
}
