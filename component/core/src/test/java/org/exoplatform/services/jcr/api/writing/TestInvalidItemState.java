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

import javax.jcr.InvalidItemStateException;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.services.jcr.JcrAPIBaseTest;

/**
 * Created by The eXo Platform SAS.
 * 
 * Date: 31.03.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestInvalidItemState.java 12967 2008-04-09 09:29:36Z pnedonosko $
 */
public class TestInvalidItemState
   extends JcrAPIBaseTest
{

   private Node testRoot;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      testRoot = root.addNode("invalidTest");
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

   public void testSessionRemainChanges() throws Exception
   {
      final Node testRootS1 = testRoot;

      Session s2 = repository.login(credentials, testRootS1.getSession().getWorkspace().getName());
      final Node testRootS2 = (Node) s2.getItem(testRootS1.getPath());

      // test
      Node nS1 = testRootS1.addNode("node");
      testRootS1.save();

      Node nS2 = testRootS2.getNode("node");
      nS2.remove();

      nS1.remove();
      testRootS1.save();

      try
      {
         testRootS2.save();
      }
      catch (InvalidItemStateException e)
      {
         // ok, check pending changes
         assertTrue("Failed session has to store pending changes", s2.hasPendingChanges());
      }
   }

   public void testAddRemoveRegularNode() throws LoginException, NoSuchWorkspaceException, RepositoryException
   {

      final Node testRootS1 = testRoot;

      Session s2 = repository.login(credentials, testRootS1.getSession().getWorkspace().getName());
      final Node testRootS2 = (Node) s2.getItem(testRootS1.getPath());

      // test

      Node nS1 = testRootS1.addNode("node1");
      testRootS1.save();

      testRootS2.addNode("node2");

      nS1.remove();
      testRootS1.save();

      try
      {
         testRootS2.save();
      }
      catch (InvalidItemStateException e)
      {
         e.printStackTrace();
         fail("InvalidItemStateException should not have been thrown, but  " + e);
      }
   }

   public void testAddRemoveSameNameSiblings() throws LoginException, NoSuchWorkspaceException, RepositoryException
   {

      final Node testRootS1 = testRoot;

      Session s2 = repository.login(credentials, testRootS1.getSession().getWorkspace().getName());
      final Node testRootS2 = (Node) s2.getItem(testRootS1.getPath());

      // test

      Node nS1 = testRootS1.addNode("node");
      testRootS1.save();

      testRootS2.addNode("node");

      nS1.remove();
      testRootS1.save();

      try
      {
         testRootS2.save();
         fail("InvalidItemStateException should have been thrown");
      }
      catch (InvalidItemStateException e)
      {
         // ok
      }
   }

   public void testAddSubtreeOfNodes() throws LoginException, NoSuchWorkspaceException, RepositoryException
   {

      final Node testRootS1 = testRoot;

      // test

      try
      {
         Node nS1 = testRootS1.addNode("node1");
         Node nS1_1 = nS1.addNode("childNode1");
         Node nS1_2 = nS1.addNode("childNode1");
         Node nS1_2_1 = nS1_2.addNode("childNode2");
         Node nS1_2_2 = nS1_2.addNode("childNode2");
         testRootS1.save();
      }
      catch (InvalidItemStateException e)
      {
         e.printStackTrace();
         fail("InvalidItemStateException should not have been thrown, but  " + e);
      }
   }

   /**
    * Test if remove of same-name siblings will not thorwn an exception. Nodes are removed by their
    * Ids.
    * 
    * @throws LoginException
    * @throws NoSuchWorkspaceException
    * @throws RepositoryException
    */
   public void testRemoveSameNameSiblings() throws LoginException, NoSuchWorkspaceException, RepositoryException
   {

      final Node testRootS1 = testRoot;

      Session s2 = repository.login(credentials, testRootS1.getSession().getWorkspace().getName());
      final Node testRootS2 = (Node) s2.getItem(testRootS1.getPath());

      Node nS1 = testRootS1.addNode("node"); // node[1]
      testRootS1.save();

      Node nS2marker = testRootS2.addNode("node"); // node[2]
      nS2marker.addMixin("mix:referenceable");
      String s2id = nS2marker.getUUID();
      Node nS2 = testRootS2.addNode("node"); // node[3]
      testRootS2.save();

      // test

      nS1.remove(); // 1. node[2] -> node[1]

      nS2.remove(); // 2. node[3] -> node[2] too

      testRootS1.save(); // save step 1

      try
      {
         testRootS2.save(); // save step 2
      }
      catch (InvalidItemStateException e)
      {
         fail("InvalidItemStateException should not have been thrown, but  " + e);
      }

      assertEquals("Wrong node UUID found ", s2id, testRootS2.getNode("node").getUUID());
   }

   public void testOrderSameNameSiblings() throws LoginException, NoSuchWorkspaceException, RepositoryException
   {

      final Node testRootS1 = testRoot;

      Session s2 = repository.login(credentials, testRootS1.getSession().getWorkspace().getName());
      final Node testRootS2 = (Node) s2.getItem(testRootS1.getPath());

      testRootS1.addNode("node"); // node[1]
      testRootS1.save();

      Node nS2_2 = testRootS2.addNode("node"); // node[2]
      nS2_2.addMixin("mix:referenceable");
      String s2_2_id = nS2_2.getUUID();
      testRootS2.addNode("node"); // node[3]
      testRootS2.save();

      // test

      testRootS2.orderBefore("node[3]", "node[1]");

      testRootS1.orderBefore("node[3]", "node[2]");

      testRootS1.save(); // save step 1

      try
      {
         testRootS2.save(); // save step 2
      }
      catch (InvalidItemStateException e)
      {
         fail("InvalidItemStateException should not have been thrown, but  " + e);
      }

      assertEquals("Wrong node UUID found ", s2_2_id, testRootS2.getNode("node[3]").getUUID());
   }

   /**
    * Test if orderBefore of same-name sibling nodes fails if one of them is deleted.
    * <p>
    * Check if refresh(true) and refresh(false) works ok with changes recorded on the Session.
    * 
    * @throws LoginException
    * @throws NoSuchWorkspaceException
    * @throws RepositoryException
    */
   public void testOrderRemoveSameNameSiblings() throws LoginException, NoSuchWorkspaceException, RepositoryException
   {

      final Node testRootS1 = testRoot;

      Session s2 = repository.login(credentials, testRootS1.getSession().getWorkspace().getName());
      final Node testRootS2 = (Node) s2.getItem(testRootS1.getPath());

      Node nS1_1 = testRootS1.addNode("node"); // node[1]
      testRootS1.save();
      nS1_1.addMixin("mix:referenceable");
      String s1_1_id = nS1_1.getUUID();
      testRootS1.save();

      Node nS2_2 = testRootS2.addNode("node"); // node[2]
      nS2_2.addMixin("mix:referenceable");
      String s2_2_id = nS2_2.getUUID();
      testRootS2.addNode("node"); // node[3]
      testRootS2.save();

      // test

      testRootS2.orderBefore("node[3]", "node[1]");// node[3] -> node[1], node[1] -> node[2]

      testRootS1.orderBefore("node[3]", "node[2]");// node[3] -> node[2], node[2] -> node[3]
      nS1_1.remove();// remove node[1]
      testRootS1.save(); // save step 1

      try
      {
         testRootS2.save(); // save step 2
         fail("InvalidItemStateException should have been thrown, node[1] was removed");
      }
      catch (InvalidItemStateException e)
      {
         // ok
      }

      try
      {
         testRootS2.refresh(true);
         testRootS2.save(); // save step 2
         fail("InvalidItemStateException should have been thrown, node[1] was removed");
      }
      catch (InvalidItemStateException e)
      {
         // ok
      }

      // in pending changes
      assertEquals("Wrong node UUID found ", s1_1_id, testRootS2.getNode("node[2]").getUUID());

      testRootS2.refresh(false);
      // in persistent storage
      assertEquals("Wrong node UUID found ", s2_2_id, testRootS2.getNode("node[2]").getUUID());
   }

   /**
    * Check if orderBefore of same-name sibling and regular nodes fails if the regular is deleted.
    * <p>
    * Check if refresh(true) and refresh(false) works ok with changes recorded on the Session.
    * 
    * @throws LoginException
    * @throws NoSuchWorkspaceException
    * @throws RepositoryException
    */
   public void testOrderSameNameSiblingsRemoveRegular() throws LoginException, NoSuchWorkspaceException,
            RepositoryException
   {

      final Node testRootS1 = testRoot;

      Session s2 = repository.login(credentials, testRootS1.getSession().getWorkspace().getName());
      final Node testRootS2 = (Node) s2.getItem(testRootS1.getPath());

      Node nS1_1 = testRootS1.addNode("node"); // node[1]
      testRootS1.save();
      nS1_1.addMixin("mix:referenceable");
      String s1_1_id = nS1_1.getUUID();
      testRootS1.save();

      testRootS2.addNode("my_node"); // my_node
      Node nS2_2 = testRootS2.addNode("node"); // node[2]
      nS2_2.addMixin("mix:referenceable");
      String s2_2_id = nS2_2.getUUID();
      testRootS2.save();

      // test

      testRootS2.orderBefore("my_node", "node[1]");

      testRootS1.orderBefore("my_node", "node[2]");
      nS1_1.remove();// remove node[1]
      testRootS1.save(); // save step 1

      try
      {
         testRootS2.save(); // save step 2
         fail("InvalidItemStateException should have been thrown, node[1] was removed");
      }
      catch (InvalidItemStateException e)
      {
         // ok
      }

      try
      {
         testRootS2.refresh(true);
         testRootS2.save(); // save step 2
         fail("InvalidItemStateException should have been thrown, node[1] was removed");
      }
      catch (InvalidItemStateException e)
      {
         // ok
      }

      // in pending changes, node[1] exists as an origialy created with index 1
      assertEquals("Wrong node UUID found ", s1_1_id, testRootS2.getNode("node[1]").getUUID());

      testRootS2.refresh(false);
      // in persistent storage, node[1] it's a node[2] originally
      assertEquals("Wrong node UUID found ", s2_2_id, testRootS2.getNode("node[1]").getUUID());
   }
}
