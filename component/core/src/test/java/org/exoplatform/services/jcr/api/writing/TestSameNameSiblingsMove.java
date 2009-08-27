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
package org.exoplatform.services.jcr.api.writing;

import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.exoplatform.services.jcr.JcrAPIBaseTest;

/**
 * Created by The eXo Platform SAS.
 * 
 * Date: 31.03.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestSameNameSiblingsMove.java 12992 2008-04-09 14:52:34Z pnedonosko $
 */
public class TestSameNameSiblingsMove
   extends JcrAPIBaseTest
{

   private Node testRoot;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      testRoot = root.addNode("snsMoveTest");
      root.save();
   }

   @Override
   protected void tearDown() throws Exception
   {
      root.refresh(false);
      testRoot.remove();
      root.save();

      super.tearDown();
   }

   /**
    * Move node[1] to node[3], node[3] reordered to node[2], node[2] to node[1].
    * 
    * @throws LoginException
    * @throws NoSuchWorkspaceException
    * @throws RepositoryException
    */
   public void testMoveFirst() throws LoginException, NoSuchWorkspaceException, RepositoryException
   {

      final Node testRootS1 = testRoot;
      testRootS1.addMixin("exo:owneable");
      testRootS1.addMixin("exo:privilegeable");
      testRootS1.save();

      Node nS1_1 = testRootS1.addNode("node"); // node[1]
      testRootS1.save();
      nS1_1.addMixin("mix:referenceable");
      nS1_1.addMixin("exo:owneable");
      // nS1_1.setProperty("exo:owner", "root");
      String s1_1_id = nS1_1.getUUID();
      testRootS1.save();

      Node nS1_2 = testRootS1.addNode("node"); // node[2]
      Node nS1_3 = testRootS1.addNode("node"); // node[3]

      testRootS1.save();

      // test
      // for (NodeIterator iter = testRootS1.getNodes(); iter.hasNext();) {
      // Node n = iter.nextNode();
      // log.info("Node: " + n.getPath() + " " + ((NodeImpl)n).getInternalIdentifier());
      // }
      try
      {
         // move node[1] to node[3], node[3] reordered to node[2], node[2] to node[1]
         testRootS1.getSession().move(testRootS1.getPath() + "/node", testRootS1.getPath() + "/node");
         // for (NodeIterator iter = testRootS1.getNodes(); iter.hasNext();) {
         // Node n = iter.nextNode();
         // log.info("Node: " + n.getPath() + " " + ((NodeImpl)n).getInternalIdentifier());
         // }
         testRootS1.save(); // save

      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail("RepositoryException should not have been thrown, but " + e);
      }

      int index = 0;
      for (NodeIterator iter = testRootS1.getNodes(); iter.hasNext();)
      {
         index++;
         Node n = iter.nextNode();
         // log.info("Node: " + n.getPath());
         assertEquals("Wrong index found ", index, n.getIndex());
      }

      // check reordering
      assertEquals("Wrong node UUID found ", s1_1_id, testRootS1.getNode("node[3]").getUUID());
   }

   /**
    * Move node[2] to node[3], node[3] reordered to node[2].
    * 
    * @throws LoginException
    * @throws NoSuchWorkspaceException
    * @throws RepositoryException
    */
   public void testMoveMiddle() throws LoginException, NoSuchWorkspaceException, RepositoryException
   {

      final Node testRootS1 = testRoot;

      Node nS1_1 = testRootS1.addNode("node"); // node[1]
      testRootS1.save();
      nS1_1.addMixin("mix:referenceable");
      String s1_1_id = nS1_1.getUUID();
      testRootS1.save();

      Node nS1_2 = testRootS1.addNode("node"); // node[2]
      Node nS1_3 = testRootS1.addNode("node"); // node[3]
      testRootS1.save();

      // test
      try
      {
         // move node[2] to node[3], node[3] reordered to node[2]
         testRootS1.getSession().move(testRootS1.getPath() + "/node[2]", testRootS1.getPath() + "/node");
         testRootS1.save(); // save
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail("RepositoryException should not have been thrown, but " + e);
      }

      int index = 0;
      for (NodeIterator iter = testRootS1.getNodes(); iter.hasNext();)
      {
         index++;
         Node n = iter.nextNode();
         // log.info("Node: " + n.getPath());
         assertEquals("Wrong index found ", index, n.getIndex());
      }

      // check reordering
      assertEquals("Wrong node UUID found ", s1_1_id, testRootS1.getNode("node").getUUID());
   }

   /**
    * Move SNS node to itself, move node[3] to node[3].
    * 
    * @throws LoginException
    * @throws NoSuchWorkspaceException
    * @throws RepositoryException
    */
   public void testMoveLast() throws LoginException, NoSuchWorkspaceException, RepositoryException
   {

      final Node testRootS1 = testRoot;

      Node nS1_1 = testRootS1.addNode("node"); // node[1]
      testRootS1.save();
      nS1_1.addMixin("mix:referenceable");
      String s1_1_id = nS1_1.getUUID();
      testRootS1.save();

      Node nS1_2 = testRootS1.addNode("node"); // node[2]
      Node nS1_3 = testRootS1.addNode("node"); // node[3]
      testRootS1.save();

      // test
      try
      {
         // move to itself, move node[3] to node[3]
         testRootS1.getSession().move(testRootS1.getPath() + "/node[3]", testRootS1.getPath() + "/node");
         testRootS1.save(); // save
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail("RepositoryException should not have been thrown, but " + e);
      }

      int index = 0;
      for (NodeIterator iter = testRootS1.getNodes(); iter.hasNext();)
      {
         index++;
         Node n = iter.nextNode();
         // log.info("Node: " + n.getPath());
         assertEquals("Wrong index found ", index, n.getIndex());
      }

      // check reordering
      assertEquals("Wrong node UUID found ", s1_1_id, testRootS1.getNode("node").getUUID());
   }

   /**
    * Move SNS node to different location with SNS too, move /snsMoveTest/node1/node[2] to /snsMoveTest/node2/node[3].
    * 
    * @throws LoginException
    * @throws NoSuchWorkspaceException
    * @throws RepositoryException
    */
   public void testMoveToDiffLocation() throws LoginException, NoSuchWorkspaceException, RepositoryException
   {

      final Node testRootS1 = testRoot;
      final Node testNode1 = testRootS1.addNode("node1");
      final Node testNode2 = testRootS1.addNode("node1");
      testRootS1.save();

      Node n1_1 = testNode1.addNode("node"); // node[1]
      Node n1_2 = testNode1.addNode("node"); // node[2]
      testNode1.save();
      n1_2.addMixin("mix:referenceable");
      String n1_2_id = n1_2.getUUID();
      testNode1.save();

      Node n1_3 = testNode1.addNode("node"); // node[3]
      Node n1_4 = testNode1.addNode("node"); // node[4]
      testNode1.save();

      Node n2_1 = testNode2.addNode("node"); // node[1]
      Node n2_2 = testNode2.addNode("node"); // node[2]
      testNode2.save();

      // test
      try
      {
         // move /snsMoveTest/node1/node[2] to /snsMoveTest/node2/node[3]
         testNode1.getSession().move(testNode1.getPath() + "/node[2]", testNode2.getPath() + "/node");
         testNode1.getSession().save(); // save
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail("RepositoryException should not have been thrown, but " + e);
      }

      int index = 0;
      for (NodeIterator iter = testNode1.getNodes(); iter.hasNext();)
      {
         index++;
         Node n = iter.nextNode();
         assertEquals("Wrong index found ", index, n.getIndex());
      }

      index = 0;
      for (NodeIterator iter = testNode2.getNodes(); iter.hasNext();)
      {
         index++;
         Node n = iter.nextNode();
         assertEquals("Wrong index found ", index, n.getIndex());
      }

      // check reordering
      assertEquals("Wrong node UUID found ", n1_2_id, testNode2.getNode("node[3]").getUUID());
   }
}
