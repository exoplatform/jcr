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

import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: JcrSystemTest.java 11907 2008-03-13 15:36:21Z ksm $
 * 
 *          Items under /jcr:system
 */

public class JcrSystemTest extends BaseUsecasesTest
{
   /**
    * Check If Jcr System is referenceable from any Workspace NOTE: THIS is an implementation feature
    * and NOT specified by JSR-170!
    * 
    * Sample test. An example how to make it
    * 
    * @throws Exception
    */
   public void testIfJcrSystemItemsSharesBetweenWorkspaces() throws Exception
   {

      String[] wss = repository.getWorkspaceNames();
      if (wss.length < 2)
      {
         fail("2 or more workspaces required");
      }

      assertFalse(wss[0].equals(wss[1]));

      Session session1 = repository.login(new SimpleCredentials("admin", "admin".toCharArray()), wss[0]);
      Session session2 = repository.login(new SimpleCredentials("admin", "admin".toCharArray()), wss[1]);
      Node testNode1 = null;
      try
      {
         testNode1 = (Node)session2.getItem("/jcr:system/cms");
         fail("There should not be /jcr:system/cms");
      }
      catch (PathNotFoundException e)
      {
      }

      // sessin1 (wss[0]) adds node
      testNode1 = session1.getRootNode().addNode("jcr:system/cms");
      testNode1.addMixin("mix:referenceable");
      String uuid = testNode1.getUUID();
      session1.save();

      // is node reachable from sessin2 (wss[1]) ?
      Node testNode2 = null;
      try
      {
         Node testNode3 = (Node)session2.getItem("/jcr:system/cms");
         testNode2 = session2.getNodeByUUID(uuid);

         assertTrue(testNode3.isSame(testNode2));
         assertTrue(testNode1.isSame(testNode2));

      }
      catch (PathNotFoundException e)
      {
         e.printStackTrace();
         fail("/jcr:system/cms is not reachable!");
      }

      // check if it is possible to make a reference
      Node subRoot1 = session1.getRootNode().addNode("testIfJcrSystemItemsSharesBetweenWorkspaces");
      subRoot1.setProperty("ref", testNode1);
      session1.save();

      assertEquals(1, testNode1.getReferences().getSize());

      Node subRoot2 = session2.getRootNode().addNode("testIfJcrSystemItemsSharesBetweenWorkspaces");
      subRoot2.setProperty("ref", testNode2);
      session2.save();

      assertEquals(1, testNode2.getReferences().getSize());

      // clean
      subRoot1.remove();
      subRoot2.remove();
      testNode1.remove();
      session1.save();
      session2.save();

   }

}
