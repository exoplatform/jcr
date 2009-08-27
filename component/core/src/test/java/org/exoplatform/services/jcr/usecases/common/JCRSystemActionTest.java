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
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;

public class JCRSystemActionTest
   extends BaseUsecasesTest
{

   protected void tearDown() throws Exception
   {

      // [PN] Clean it!!! As BaseUsecasesTest.tearDown() don't touch jcr:system descendants
      try
      {
         Node jcrSystem = session.getRootNode().getNode("jcr:system");
         jcrSystem.getNode("cms").remove();
         session.save();
      }
      catch (RepositoryException e)
      {
         log.error("Error of tearDown " + e.getMessage());
      }
      try
      {
         Node jcrSystem = session.getRootNode().getNode("jcr:system");
         jcrSystem.getNode("ecm").remove();
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
      Session session = repository.getSystemSession(workspaceName);
      // ----------- Use addmin session
      // Session session= repository.login(new SimpleCredentials("admin", "admin".toCharArray()),
      // workspaceName);

      assertTrue("session is not nulll", session != null);
      Node cmsNode = null;
      try
      {
         cmsNode = (Node) session.getItem("/jcr:system/cms");
         fail("There should not be /jcr:system/cms");
      }
      catch (PathNotFoundException e)
      {
      }
      cmsNode = session.getRootNode().addNode("jcr:system/cms");
      session.save();

      Node ecmNode = null;
      try
      {
         ecmNode = (Node) session.getItem("/jcr:system/ecm");
         fail("There should not be /jcr:system/ecm");
      }
      catch (PathNotFoundException e)
      {
      }
      ecmNode = session.getRootNode().addNode("jcr:system/ecm");
      session.save();

      Node copyNode = null;
      try
      {
         copyNode = (Node) session.getItem("/jcr:system/cms/copyNode");
         fail("There should not be /jcr:system/cms/copyNode");
      }
      catch (PathNotFoundException e)
      {
      }
      copyNode = session.getRootNode().addNode("jcr:system/cms/copyNode");
      session.save();

      Node cutNode = null;
      try
      {
         cutNode = (Node) session.getItem("/jcr:system/cms/cutNode");
         fail("There should not be /jcr:system/cms/cutNode");
      }
      catch (Exception e)
      {
      }
      cutNode = session.getRootNode().addNode("jcr:system/cms/cutNode");
      session.save();
      Workspace workspace = session.getWorkspace();
      assertTrue("workspace is not null", workspace != null);

      // copy CopyNode form cms/copyNode to ecm

      // [PN] JCR-170 7.1.7 Moving and Copying
      // The destAbsPath provided must not have an index
      // on its final element. If it does, then a
      // RepositoryException is thrown. Strictly speaking,
      // the destAbsPath parameter is actually an absolute
      // path to the parent node of the new location,
      // appended with the new name desired for the copied
      // node.
      workspace.copy(copyNode.getPath(), ecmNode.getPath() + "/" + copyNode.getName());
      session.save();
      session.refresh(false);
      try
      {
         Node node = (Node) session.getItem("/jcr:system/ecm/copyNode");
         assertTrue("expect copyNode is found:", node != null);

         // [PN] It's not are new node! You save it before.
         // assertTrue("expect copyNode is new state", node.isNew()) ;
         // session.save() ; // ??? What a reason?

         node = (Node) session.getItem("/jcr:system/ecm/copyNode");
         assertTrue("expect copyNode is not new state", !node.isNew());
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }

      workspace.move(cutNode.getPath(), ecmNode.getPath() + "/" + copyNode.getName());
      try
      {
         Node node = (Node) session.getItem("/jcr:system/cms/cutNode");
         fail("Node is not cut on source node");
      }
      catch (Exception e)
      {
      }
      try
      {
         Node node = (Node) session.getItem("/jcr:system/cms/cutNode");
         assertTrue("Node is found", node != null);
      }
      catch (Exception e)
      {
         // e.printStackTrace() ;
      }

      cmsNode.remove();
      ecmNode.remove();
      session.save();
   }
}
