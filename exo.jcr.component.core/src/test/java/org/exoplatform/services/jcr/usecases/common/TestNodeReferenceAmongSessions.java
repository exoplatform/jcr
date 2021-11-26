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

import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: TestNodeReferenceAmongSessions.java 11907 2008-03-13 15:36:21Z ksm $
 * 
 *          Items under /jcr:system
 */

public class TestNodeReferenceAmongSessions extends BaseUsecasesTest
{

   /**
    * Check If Jcr System is referenceable from any Workspace NOTE: THIS is an implementation feature
    * and NOT specified by JSR-170!
    * 
    * Sample test. An example how to make it
    * 
    * @throws Exception
    */
   public void testNodeReferenceAmongSessions() throws Exception
   {

      String[] wss = repository.getWorkspaceNames();
      if (wss.length < 2)
      {
         fail("2 or more workspaces required");
      }
      assertFalse(wss[0].equals(wss[1]));

      Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()), wss[0]);
      Node systemRefNode = null;
      try
      {
         systemRefNode = (Node)session.getItem("/jcr:system/cms");
         fail("There should not be /jcr:system/cms");
      }
      catch (PathNotFoundException e)
      {
      }
      // sessin1 (wss[0]) adds node
      systemRefNode = session.getRootNode().addNode("jcr:system/cms");
      systemRefNode.addMixin("mix:referenceable");
      String uuid = systemRefNode.getUUID();
      session.save();

      // check if it is possible to make a reference
      Session session3 = repository.login(new SimpleCredentials("admin", "admin".toCharArray()), wss[0]);
      // [PN] 05.07.06 Bug fix. Use session3 instead session.
      Node node3 = session3.getRootNode().addNode("NodeInWS1");
      node3.setProperty("ref", systemRefNode);
      session3.save();
      try
      {
         Node node = session.getRootNode().getNode("NodeInWS1");
      }
      catch (Exception e)
      {
         fail("Session hasn't this node");
      }

      Session session4 = repository.login(new SimpleCredentials("admin", "admin".toCharArray()), wss[1]);
      Node node4 = session4.getRootNode().addNode("NodeInWS2");
      node4.setProperty("ref", systemRefNode);
      session4.save();

      // get reference node from ws1(wss[0])
      Session refSession1 = repository.login(new SimpleCredentials("admin", "admin".toCharArray()), wss[0]);
      Node ws1RefNodeIn = refSession1.getNodeByUUID(uuid);
      assertEquals(1, ws1RefNodeIn.getReferences().getSize());

      // get reference node from ws2
      Session refSession2 = repository.login(new SimpleCredentials("admin", "admin".toCharArray()), wss[1]);
      Node ws2RefNode = refSession2.getNodeByUUID(uuid);
      assertEquals(1, ws2RefNode.getReferences().getSize());
      // clean

      node3.remove();
      node4.remove();
      systemRefNode.remove();
      session3.save();
      session4.save();
      session.save();

      session.logout();
      session3.logout();
      session4.logout();
      refSession1.logout();
      refSession2.logout();
   }
}
