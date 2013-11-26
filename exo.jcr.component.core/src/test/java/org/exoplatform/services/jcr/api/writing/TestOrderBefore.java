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

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.util.EntityCollection;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.VersionException;

/**
 * Created by The eXo Platform SAS 27.12.2006
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter
 *         Nedonosko</a>
 * @version $Id: TestOrderBefore.java 11962 2008-03-16 16:31:14Z gazarenkov $
 */
public class TestOrderBefore extends JcrAPIBaseTest
{

   protected final String TEST_ROOT = "order_test";

   private Node testBase = null;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      testBase = root.addNode(TEST_ROOT);
      root.save();
   }

   private void initSimpleCase() throws Exception
   {
      testBase.addNode("n1");
      testBase.addNode("n2");
      testBase.addNode("n3");
      testBase.addNode("n4");

      root.save();
   }

   private void initSimpleCase5() throws Exception
   {
      testBase.addNode("n2");
      testBase.addNode("n3");
      testBase.addNode("n1");
      testBase.addNode("n4");
      testBase.addNode("n5");

      root.save();
   }

   private void initSNSCase1() throws Exception
   {
      testBase.addNode("n1");
      testBase.addNode("n2");
      testBase.addNode("n2");
      testBase.addNode("n3");

      root.save();
   }

   private void initSNSCase2() throws Exception
   {
      testBase.addNode("n1");
      testBase.addNode("n1");
      Node n3 = testBase.addNode("n1");
      n3.addMixin("mix:referenceable");
      testBase.addNode("n1");

      root.save();
   }

   private void initSNSCase3() throws Exception
   {
      testBase.addNode("n1");
      testBase.addNode("n1");
      Node n3 = testBase.addNode("n1");
      n3.addMixin("mix:referenceable");
      testBase.addNode("n1");
      testBase.addNode("n2");

      root.save();
   }

   private void initSNSCase4() throws Exception
   {
      testBase.addNode("n1");
      testBase.addNode("n1");
      Node n21 = testBase.addNode("n2");
      n21.addMixin("mix:referenceable");
      testBase.addNode("n2");
      Node n13 = testBase.addNode("n1");
      n13.addMixin("mix:referenceable");
      testBase.addNode("n3");

      root.save();
   }

   private void initCustom(String[] names) throws ItemExistsException, PathNotFoundException, VersionException,
      ConstraintViolationException, LockException, RepositoryException
   {
      for (int i = 0; i < names.length; i++)
      {
         testBase.addNode(names[i]);
      }
      root.save();
   }

   private void initSNSCaseLargeArray() throws Exception
   {
      for (int i = 1; i <= 100; i++)
      {
         Node node = testBase.addNode("n1");
         if (i == 40)
         {
            node.addMixin("mix:referenceable");
         }
      }
      root.save();

      for (int i = 1; i <= 30; i++)
      {
         testBase.addNode("n_" + i);
      }
      root.save();
   }

   @Override
   protected void tearDown() throws Exception
   {
      try
      {
         testBase.remove();
         root.save();
      }
      catch (Throwable e)
      {
         log.error("TEAR DOWN ERROR. " + getName() + ". " + e.getMessage(), e);
      }

      super.tearDown();
   }

   /**
    * nodes and positions must be of same length
    * 
    * @param nodes
    */
   private void checkOrder(Node testRoot, String[] nodes) throws Exception
   {

      if (log.isDebugEnabled())
         log.debug(">>>> CHECK ORDER >>>>");

      NodeIterator childs = testRoot.getNodes();
      int orderPos = -1;
      while (childs.hasNext())
      {
         orderPos++;
         Node next = childs.nextNode();
         String nodeName = nodes[orderPos];
         if (!next.getPath().endsWith(nodeName))
         {
            String failMsg =
               "Nodes order is invalid. Expected: " + nodeName + ". Found: " + next.getPath() + ". Position: "
                  + orderPos;
            log.error(failMsg);
            fail(failMsg);
         }
         String mixins = "";
         for (NodeType nt : next.getMixinNodeTypes())
         {
            mixins += nt.getName() + " ";
         }
         mixins = mixins.trim();
         if (log.isDebugEnabled())
            log.debug(">> " + next.getPath() + ", " + next.getPrimaryNodeType().getName() + " " + mixins);

      }
      if (log.isDebugEnabled())
         log.debug("<<<< CHECK ORDER <<<<");
   }

   /**
    * nodes and positions must be of same length
    * 
    * @param nodes
    */
   private void checkOrderAnotherSession(String[] nodes) throws Exception
   {

      Session newSession = repository.login(this.credentials, session.getWorkspace().getName());
      Node testRoot = newSession.getRootNode().getNode(TEST_ROOT);
      checkOrder(testRoot, nodes);
      newSession.logout();
   }

   /**
    * nodes and positions must be of same length
    * 
    * @param nodes
    */
   private void checkOrder(String[] nodes) throws Exception
   {

      checkOrder(testBase, nodes);
   }

   // -------------- Tests ---------------

   // -------------- Simple use-case: child nodes n1, n2, n3, n4 ---------------

   public void testOrderUp() throws Exception
   {
      initSimpleCase();

      testBase.orderBefore("n4", "n3");

      String[] order = new String[]{"n1", "n2", "n4", "n3"};

      checkOrder(order);

      testBase.save();

      checkOrderAnotherSession(order);
   }

   public void testOrderUpStepOver() throws Exception
   {
      initSimpleCase5();

      // was n2,n3,n1,n4,n5
      testBase.orderBefore("n4", "n3");

      String[] order = new String[]{"n2", "n4", "n3", "n1", "n5"};

      checkOrder(order);

      testBase.save();

      checkOrderAnotherSession(order);
   }

   public void testOrderBegin() throws Exception
   {
      initSimpleCase();

      testBase.orderBefore("n3", "n1");

      String[] order = new String[]{"n3", "n1", "n2", "n4"};

      checkOrder(order);

      testBase.save();

      checkOrderAnotherSession(order);
   }

   public void testOrderDown() throws Exception
   {
      initSimpleCase();

      testBase.orderBefore("n2", "n4");

      String[] order = new String[]{"n1", "n3", "n2", "n4"};

      checkOrder(order);

      testBase.save();

      checkOrderAnotherSession(order);
   }

   public void testOrderEnd() throws Exception
   {
      initSimpleCase();

      testBase.orderBefore("n2", null);

      String[] order = new String[]{"n1", "n3", "n4", "n2"};

      checkOrder(order);

      testBase.save();

      checkOrderAnotherSession(order);
   }

   // ----- Same-name sibling use-case: child nodes n1, n2[1], n2[2], n3 -----

   public void testOrderUp_SNS1() throws Exception
   {
      initSNSCase1();

      testBase.orderBefore("n3", "n2");

      String[] order = new String[]{"n1", "n3", "n2", "n2[2]"};

      checkOrder(order);

      testBase.save();

      checkOrderAnotherSession(order);
   }

   public void testOrderBegin_SNS1() throws Exception
   {
      initSNSCase1();

      testBase.orderBefore("n2[2]", "n1");

      String[] order = new String[]{"n2", "n1", "n2[2]", "n3"};

      checkOrder(order);

      testBase.save();

      checkOrderAnotherSession(order);
   }

   public void testOrderDown_SNS1() throws Exception
   {
      initSNSCase1();

      testBase.orderBefore("n1", "n2[2]");

      String[] order = new String[]{"n2", "n1", "n2[2]", "n3"};

      checkOrder(order);

      testBase.save();

      checkOrderAnotherSession(order);
   }

   /**
    * A childs order map is unchanged after the order
    */
   public void testOrderDown1_SNS1() throws Exception
   {
      initSNSCase1();

      testBase.orderBefore("n2", "n3");

      String[] order = new String[]{"n1", "n2", "n2[2]", "n3"};

      checkOrder(order);

      testBase.save();

      checkOrderAnotherSession(order);
   }

   public void testOrderEnd_SNS1() throws Exception
   {
      initSNSCase1();

      testBase.orderBefore("n2[2]", null);

      String[] order = new String[]{"n1", "n2", "n3", "n2[2]"};

      checkOrder(order);

      testBase.save();

      checkOrderAnotherSession(order);
   }

   // ----- Same-name sibling use-case: child nodes n1, n1[2], n1[3], n1[4].
   // n1[3] -
   // mix:referenceable -----

   public void testOrderBegin_SNS2() throws Exception
   {
      initSNSCase2();

      String n3uuid = testBase.getNode("n1[3]").getUUID();

      testBase.orderBefore("n1[3]", "n1");

      String[] order = new String[]{"n1", "n1[2]", "n1[3]", "n1[4]"}; // n1[3]
      // -> n1

      checkOrder(order);

      try
      {
         String n1uuid = testBase.getNode("n1").getUUID();
         assertEquals("A UUIDs must be equals after order node to another position", n3uuid, n1uuid);
      }
      catch (RepositoryException e)
      {
         fail("A node is not mix:referenceable after order to another position, " + testBase.getNode("n1").getPath()
            + ". " + e);
      }

      testBase.save();

      checkOrderAnotherSession(order);

      try
      {
         String n1uuid = testBase.getNode("n1").getUUID();
         assertEquals("A UUIDs must be equals after order node to another position", n3uuid, n1uuid);
      }
      catch (RepositoryException e)
      {
         fail("A node is not mix:referenceable after order to another position, " + testBase.getNode("n1").getPath()
            + ". " + e);
      }
   }

   public void testOrderUp_SNS2() throws Exception
   {
      initSNSCase2();

      String n3uuid = testBase.getNode("n1[3]").getUUID();

      testBase.orderBefore("n1[4]", "n1[2]");

      String[] order = new String[]{"n1", "n1[2]", "n1[3]", "n1[4]"}; // n1[3]
      // ->
      // n1[4]

      checkOrder(order);

      try
      {
         String n4uuid = testBase.getNode("n1[4]").getUUID();
         assertEquals("A UUIDs must be equals after order node to another position", n3uuid, n4uuid);
      }
      catch (RepositoryException e)
      {
         fail("A node is not mix:referenceable after order to another position, " + testBase.getNode("n1[4]").getPath()
            + ". " + e);
      }

      testBase.save();

      checkOrderAnotherSession(order);

      try
      {
         String n4uuid = testBase.getNode("n1[4]").getUUID();
         assertEquals("A UUIDs must be equals after order node to another position", n3uuid, n4uuid);
      }
      catch (RepositoryException e)
      {
         fail("A node is not mix:referenceable after order to another position, " + testBase.getNode("n1[4]").getPath()
            + ". " + e);
      }
   }

   public void testOrderDown_SNS2() throws Exception
   {
      initSNSCase2();

      String n3uuid = testBase.getNode("n1[3]").getUUID();

      testBase.orderBefore("n1", "n1[3]");

      String[] order = new String[]{"n1", "n1[2]", "n1[3]", "n1[4]"}; // n1[3]
      // is
      // unchanged
      // in
      // location

      checkOrder(order);

      try
      {
         String n3uuid_same = testBase.getNode("n1[3]").getUUID();
         assertEquals("A UUIDs must be unchanged after order an other node", n3uuid, n3uuid_same);
      }
      catch (RepositoryException e)
      {
         fail("A node is not mix:referenceable after order of an other node, " + testBase.getNode("n1[3]").getPath()
            + ". " + e);
      }

      testBase.save();

      checkOrderAnotherSession(order);

      try
      {
         String n3uuid_same = testBase.getNode("n1[3]").getUUID();
         assertEquals("A UUIDs must be equals after order node to another position", n3uuid, n3uuid_same);
      }
      catch (RepositoryException e)
      {
         fail("A node is not mix:referenceable after order to another position, " + testBase.getNode("n1[3]").getPath()
            + ". " + e);
      }
   }

   /**
    * A childs order map is unchanged after the order
    */
   public void testOrderDown1_SNS2() throws Exception
   {
      initSNSCase2();

      String n3uuid = testBase.getNode("n1[3]").getUUID();

      testBase.orderBefore("n1[2]", "n1[4]");

      String[] order = new String[]{"n1", "n1[2]", "n1[3]", "n1[4]"}; // n1[3]
      // ->
      // n1[2]

      checkOrder(order);

      try
      {
         String n2uuid = testBase.getNode("n1[2]").getUUID();
         assertEquals("A UUIDs must be equals after order node to another position", n3uuid, n2uuid);
      }
      catch (RepositoryException e)
      {
         fail("A node is not mix:referenceable after order to another position, " + testBase.getNode("n1[2]").getPath()
            + ". " + e);
      }

      testBase.save();

      checkOrderAnotherSession(order);

      try
      {
         String n2uuid = testBase.getNode("n1[2]").getUUID();
         assertEquals("A UUIDs must be equals after order node to another position", n3uuid, n2uuid);
      }
      catch (RepositoryException e)
      {
         fail("A node is not mix:referenceable after order to another position, " + testBase.getNode("n1[2]").getPath()
            + ". " + e);
      }
   }

   public void testOrderEnd_SNS2() throws Exception
   {
      initSNSCase2();

      String n3uuid = testBase.getNode("n1[3]").getUUID();

      testBase.orderBefore("n1[2]", null);

      String[] order = new String[]{"n1", "n1[2]", "n1[3]", "n1[4]"}; // n1[3]
      // ->
      // n1[2]

      checkOrder(order);

      try
      {
         String n2uuid = testBase.getNode("n1[2]").getUUID();
         assertEquals("A UUIDs must be equals after order node to another position", n3uuid, n2uuid);
      }
      catch (RepositoryException e)
      {
         fail("A node is not mix:referenceable after order to another position, " + testBase.getNode("n1[2]").getPath()
            + ". " + e);
      }

      testBase.save();

      checkOrderAnotherSession(order);

      try
      {
         String n2uuid = testBase.getNode("n1[2]").getUUID();
         assertEquals("A UUIDs must be equals after order node to another position", n3uuid, n2uuid);
      }
      catch (RepositoryException e)
      {
         fail("A node is not mix:referenceable after order to another position, " + testBase.getNode("n1[2]").getPath()
            + ". " + e);
      }
   }

   // ----- Same-name sibling use-case: child nodes n1, n1[2], n1[3], n1[4], n2.
   // n1[3] -
   // mix:referenceable -----

   public void testOrderBegin_SNS3() throws Exception
   {
      initSNSCase3();

      String n3uuid = testBase.getNode("n1[3]").getUUID();

      testBase.orderBefore("n2", "n1");

      String[] order = new String[]{"n2", "n1", "n1[2]", "n1[3]", "n1[4]"}; // n1
      // [
      // 3
      // ]
      // unchanged

      checkOrder(order);

      try
      {
         String n3uuid_same = testBase.getNode("n1[3]").getUUID();
         assertEquals("A UUIDs must be unchanged after order an other node", n3uuid, n3uuid_same);
      }
      catch (RepositoryException e)
      {
         fail("A node is not mix:referenceable after order of an other node, " + testBase.getNode("n1[3]").getPath()
            + ". " + e);
      }

      testBase.save();

      checkOrderAnotherSession(order);

      try
      {
         String n3uuid_same = testBase.getNode("n1[3]").getUUID();
         assertEquals("A UUIDs must be unchanged after order an other node", n3uuid, n3uuid_same);
      }
      catch (RepositoryException e)
      {
         fail("A node is not mix:referenceable after order of an other node, " + testBase.getNode("n1[3]").getPath()
            + ". " + e);
      }
   }

   public void testOrderUp_SNS3() throws Exception
   {
      initSNSCase3();

      String n3uuid = testBase.getNode("n1[3]").getUUID();

      testBase.orderBefore("n2", "n1[3]");

      String[] order = new String[]{"n1", "n1[2]", "n2", "n1[3]", "n1[4]"}; // n1
      // [
      // 3
      // ]
      // unchanged

      checkOrder(order);

      try
      {
         String n3uuid_same = testBase.getNode("n1[3]").getUUID();
         assertEquals("A UUIDs must be unchanged after order an other node", n3uuid, n3uuid_same);
      }
      catch (RepositoryException e)
      {
         fail("A node is not mix:referenceable after order of an other node, " + testBase.getNode("n1[3]").getPath()
            + ". " + e);
      }

      testBase.save();

      checkOrderAnotherSession(order);

      try
      {
         String n3uuid_same = testBase.getNode("n1[3]").getUUID();
         assertEquals("A UUIDs must be unchanged after order an other node", n3uuid, n3uuid_same);
      }
      catch (RepositoryException e)
      {
         fail("A node is not mix:referenceable after order of an other node, " + testBase.getNode("n1[3]").getPath()
            + ". " + e);
      }
   }

   public void testOrderDown_SNS3() throws Exception
   {
      initSNSCase3();

      String n3uuid = testBase.getNode("n1[3]").getUUID();

      testBase.orderBefore("n1", "n2");

      String[] order = new String[]{"n1", "n1[2]", "n1[3]", "n1[4]", "n2"}; // n1
      // [
      // 3
      // ]
      // -
      // >
      // n1
      // [
      // 2
      // ]

      checkOrder(order);

      try
      {
         String n2uuid = testBase.getNode("n1[2]").getUUID();
         assertEquals("A UUIDs must be equals after order node to another position", n3uuid, n2uuid);
      }
      catch (RepositoryException e)
      {
         fail("A node is not mix:referenceable after order to another position, " + testBase.getNode("n1[2]").getPath()
            + ". " + e);
      }

      testBase.save();

      checkOrderAnotherSession(order);

      try
      {
         String n2uuid = testBase.getNode("n1[2]").getUUID();
         assertEquals("A UUIDs must be equals after order node to another position", n3uuid, n2uuid);
      }
      catch (RepositoryException e)
      {
         fail("A node is not mix:referenceable after order to another position, " + testBase.getNode("n1[2]").getPath()
            + ". " + e);
      }
   }

   /**
    * A childs order map is unchanged after the order
    */
   public void testOrderDown1_SNS3() throws Exception
   {
      initSNSCase3();

      String n3uuid = testBase.getNode("n1[3]").getUUID();

      testBase.orderBefore("n1[2]", "n1[4]");

      String[] order = new String[]{"n1", "n1[2]", "n1[3]", "n1[4]", "n2"}; // n1
      // [
      // 3
      // ]
      // -
      // >
      // n1
      // [
      // 2
      // ]

      checkOrder(order);

      try
      {
         String n2uuid = testBase.getNode("n1[2]").getUUID();
         assertEquals("A UUIDs must be equals after order node to another position", n3uuid, n2uuid);
      }
      catch (RepositoryException e)
      {
         fail("A node is not mix:referenceable after order to another position, " + testBase.getNode("n1[2]").getPath()
            + ". " + e);
      }

      testBase.save();

      checkOrderAnotherSession(order);

      try
      {
         String n2uuid = testBase.getNode("n1[2]").getUUID();
         assertEquals("A UUIDs must be equals after order node to another position", n3uuid, n2uuid);
      }
      catch (RepositoryException e)
      {
         fail("A node is not mix:referenceable after order to another position, " + testBase.getNode("n1[2]").getPath()
            + ". " + e);
      }
   }

   public void testOrderEnd_SNS3() throws Exception
   {
      initSNSCase3();

      String n3uuid = testBase.getNode("n1[3]").getUUID();

      testBase.orderBefore("n1[3]", null);

      String[] order = new String[]{"n1", "n1[2]", "n1[3]", "n2", "n1[4]"}; // n1
      // [3]->n1[4]

      checkOrder(order);

      try
      {
         String n4uuid = testBase.getNode("n1[4]").getUUID();
         assertEquals("A UUIDs must be equals after order node to another position", n3uuid, n4uuid);
      }
      catch (RepositoryException e)
      {
         fail("A node is not mix:referenceable after order to another position, " + testBase.getNode("n1[4]").getPath()
            + ". " + e);
      }

      testBase.save();

      checkOrderAnotherSession(order);

      try
      {
         String n4uuid = testBase.getNode("n1[4]").getUUID();
         assertEquals("A UUIDs must be equals after order node to another position", n3uuid, n4uuid);
      }
      catch (RepositoryException e)
      {
         fail("A node is not mix:referenceable after order to another position, " + testBase.getNode("n1[4]").getPath()
            + ". " + e);
      }
   }

   // ================= Large arrays of nodes ================
   /**
    * Test of case when an index text length in the item path is differs from one
    * new creted by reorder. E.g. n1[2] -> n1[100]
    */
   public void testLargeNodesArray() throws Exception
   {
      initSNSCaseLargeArray();

      // String n40uuid = testBase.getNode("n1[40]").getUUID();

      Node n1__2 = testBase.getNode("n1[2]");
      Node n_21 = testBase.getNode("n_21");
      Node n_24 = testBase.getNode("n_24");

      // === step 1 ===

      // n1[2] -> n1[100] pos:120; n_21 = pos:121; ... n1[3] -> pos:2; n1[99] ->
      // n1[98] pos:98;
      // n1[100] -> n1[99] pos:99
      testBase.orderBefore("n1[2]", "n_21");

      EntityCollection nodes = getEntityCollection(testBase.getNodes());

      assertEquals("Nodes must be equals ", n1__2, nodes.getList().get(119)); // pos
      // :
      // 120
      assertEquals("Nodes must be equals ", n1__2, testBase.getNode("n1[100]"));

      assertEquals("Nodes must be equals ", n_21, nodes.getList().get(120)); //pos:
      // 121
      assertEquals("Nodes must be equals ", n_21, testBase.getNode("n_21"));

      assertTrue("Node must exists ", testBase.hasNode("n1[2]"));

      testBase.save();

      nodes = getEntityCollection(testBase.getNodes());

      assertEquals("Nodes must be equals ", n1__2, nodes.getList().get(119)); // pos
      // :
      // 120
      assertEquals("Nodes must be equals ", n1__2, testBase.getNode("n1[100]"));

      assertEquals("Nodes must be equals ", n_21, nodes.getList().get(120)); //pos:
      // 121
      assertEquals("Nodes must be equals ", n_21, testBase.getNode("n_21"));

      assertTrue("Node must exists ", testBase.hasNode("n1[2]"));

      // === step 2 ===

      Node n1__100 = testBase.getNode("n1[100]");

      // n_24 -> pos:120; n1[100] -> pos:121;
      testBase.orderBefore("n_24", "n1[100]");

      nodes = getEntityCollection(testBase.getNodes());

      assertEquals("Nodes must be equals ", n1__100, nodes.getList().get(120)); // pos
      // :
      // 121
      assertEquals("Nodes must be equals ", n1__100, testBase.getNode("n1[100]"));

      assertEquals("Nodes must be equals ", n_24, nodes.getList().get(119)); //pos:
      // 120
      assertEquals("Nodes must be equals ", n_24, testBase.getNode("n_24"));

      assertEquals("Nodes must be equals ", n_21, nodes.getList().get(121)); //pos:
      // 122
      assertEquals("Nodes must be equals ", n_21, testBase.getNode("n_21"));

      testBase.save();

      assertEquals("Nodes must be equals ", n1__100, nodes.getList().get(120)); // pos
      // :
      // 121
      assertEquals("Nodes must be equals ", n1__100, testBase.getNode("n1[100]"));

      assertEquals("Nodes must be equals ", n_24, nodes.getList().get(119)); //pos:
      // 120
      assertEquals("Nodes must be equals ", n_24, testBase.getNode("n_24"));

      assertEquals("Nodes must be equals ", n_21, nodes.getList().get(121)); //pos:
      // 122
      assertEquals("Nodes must be equals ", n_21, testBase.getNode("n_21"));
   }

   public void testOrderTwice() throws Exception
   {

      String[] order = new String[]{"n1", "n2", "n3", "n4", "n5"};
      initCustom(order);
      checkOrder(testBase, order);

      testBase.orderBefore("n1", "n4");

      order = new String[]{"n2", "n3", "n1", "n4", "n5"};

      checkOrder(order);

      testBase.save();

      checkOrder(order);

      checkOrderAnotherSession(order);

      testBase.orderBefore("n4", "n3");

      order = new String[]{"n2", "n4", "n3", "n1", "n5"};

      checkOrder(order);

      testBase.save();
      checkOrder(order);
      checkOrderAnotherSession(order);

   }

   public void testAddMixinOrderBefore() throws Exception
   {
      Session session = repository.login(credentials, "ws");
      Node testBase = session.getRootNode().addNode("test");
      session.save();

      testBase.addNode("n1");
      root.save();

      Node n2 = testBase.addNode("n2");
      testBase.orderBefore("n2", "n1");
      n2.addMixin("exo:datetime");
      testBase.save();

      Node n3 = testBase.addNode("n3");
      testBase.orderBefore("n3", "n2");
      n3.addMixin("exo:datetime");
      testBase.save();

      Node n4 = testBase.addNode("n4");
      testBase.orderBefore("n4", "n3");
      n4.addMixin("exo:datetime");
      testBase.save();

      NodeIterator it = testBase.getNodes();
      NodeImpl node1 = (NodeImpl)it.nextNode();
      NodeImpl node2 = (NodeImpl)it.nextNode();
      NodeImpl node3 = (NodeImpl)it.nextNode();
      NodeImpl node4 = (NodeImpl)it.nextNode();

      assertEquals("n4", node1.getName());
      assertEquals("n3", node2.getName());
      assertEquals("n2", node3.getName());
      assertEquals("n1", node4.getName());

      assertTrue(((NodeData)node1.getData()).getOrderNumber() < ((NodeData)node2.getData()).getOrderNumber());
      assertTrue(((NodeData)node2.getData()).getOrderNumber() < ((NodeData)node3.getData()).getOrderNumber());
      assertTrue(((NodeData)node3.getData()).getOrderNumber() < ((NodeData)node4.getData()).getOrderNumber());

      session.logout();
   }

   public void testDeleteOrderBefore() throws Exception
   {
      Session session = repository.login(credentials, "ws");
      session.getRootNode().addNode("a");
      session.save();
      session.logout();

      session = repository.login(credentials, "ws");
      Node a = session.getRootNode().getNode("a"); // We suppose it already exists
      a.addNode("n");
      a.addNode("n");
      a.addNode("n");
      a.addNode("n");
      session.save();
      session.logout();

      session = repository.login(credentials, "ws");
      a = session.getRootNode().getNode("a");
      NodeIterator i = a.getNodes();
      i.nextNode().remove();
      i.nextNode().remove();
      i.nextNode().remove();
      session.save();
      session.logout();

      session = repository.login(credentials, "ws");
      a = session.getRootNode().getNode("a");
      a.addNode("n");
      a.orderBefore("n", null); // NPE happens here
      session.save();
   }

   public void testDeleteOrderBefore_SNS() throws Exception
   {
      Session session = repository.login(credentials, "ws");
      session.getRootNode().addNode("a");
      session.save();
      session.logout();

      session = repository.login(credentials, "ws");
      Node a = session.getRootNode().getNode("a"); // We suppose it already exist
      Node n1 = a.addNode("n");
      n1.addMixin("mix:referenceable");
      Node n2 = a.addNode("n");
      n2.addMixin("mix:referenceable");
      Node n3 = a.addNode("n");
      n3.addMixin("mix:referenceable");
      session.save();
      String n1id = n1.getUUID();
      String n2id = n2.getUUID();
      String n3id = n3.getUUID();
      session.logout();

      session = repository.login(credentials, "ws");
      a = session.getRootNode().getNode("a");
      a.getNode("n[2]").remove();
      a.save();
      session.save();
      session.logout();

      session = repository.login(credentials, "ws");
      a = session.getRootNode().getNode("a");

      try
      {
         session.getNodeByUUID(n2id);
         fail("Node with id " + n2id + " is deleted");
      }
      catch (ItemNotFoundException e)
      {
         // ok
      }
   }

   public void testOrderBeforeAfterMove() throws Exception
   {
      SessionImpl session = (SessionImpl)repository.login(credentials, WORKSPACE);
      Node list = session.getRootNode().addNode("list2", "list");
      assertEquals("list", list.getPrimaryNodeType().getName());
      assertTrue(list.getPrimaryNodeType().hasOrderableChildNodes());

      String path = list.addNode("1").getPath();
      list.addNode("2");
      session.save();
      session.logout();

      session = (SessionImpl)repository.login(credentials, WORKSPACE);
      list = session.getRootNode().getNode("list2");
      session.move(path, list.getPath() + "/3");
      list.orderBefore("3", "2");
      session.save();
      session.logout();

      session = (SessionImpl)repository.login(credentials, WORKSPACE);
      NodeIterator it = session.getRootNode().getNode("list2").getNodes();
      NodeImpl node1 = (NodeImpl)it.nextNode();
      NodeImpl node2 = (NodeImpl)it.nextNode();
      assertEquals("3", node1.getName());
      assertEquals("2", node2.getName());
      assertTrue(((NodeData)node1.getData()).getOrderNumber() < ((NodeData)node2.getData()).getOrderNumber());

      session.logout();
   }

   public void testOrderBeforeAfterMove2() throws Exception
   {
      SessionImpl session = (SessionImpl)repository.login(credentials, WORKSPACE);
      Node list = session.getRootNode().addNode("list2", "list");
      assertEquals("list", list.getPrimaryNodeType().getName());
      assertTrue(list.getPrimaryNodeType().hasOrderableChildNodes());

      list.addNode("1");
      list.addNode("2");
      list.addNode("3");
      list.addNode("4");
      session.save();
      session.logout();

      session = (SessionImpl)repository.login(credentials, WORKSPACE);
      list = session.getRootNode().getNode("list2");
      session.move(list.getPath() + "/2", list.getPath() + "/5");
      list.orderBefore("5", "1");
      session.save();
      session.logout();

      session = (SessionImpl)repository.login(credentials, WORKSPACE);
      NodeIterator it = session.getRootNode().getNode("list2").getNodes();
      NodeImpl node1 = (NodeImpl)it.nextNode();
      NodeImpl node2 = (NodeImpl)it.nextNode();
      NodeImpl node3 = (NodeImpl)it.nextNode();
      NodeImpl node4 = (NodeImpl)it.nextNode();

      assertEquals("5", node1.getName());
      assertEquals("1", node2.getName());
      assertEquals("3", node3.getName());
      assertEquals("4", node4.getName());

      assertTrue(((NodeData)node1.getData()).getOrderNumber() < ((NodeData)node2.getData()).getOrderNumber());
      assertTrue(((NodeData)node2.getData()).getOrderNumber() < ((NodeData)node3.getData()).getOrderNumber());
      assertTrue(((NodeData)node3.getData()).getOrderNumber() < ((NodeData)node4.getData()).getOrderNumber());
      
      session.logout();
   }

   private EntityCollection getEntityCollection(NodeIterator nodes)
   {
      List result = new ArrayList();
      while (nodes.hasNext())
      {
         result.add(nodes.next());
      }

      return new EntityCollection(result);
   }
}
