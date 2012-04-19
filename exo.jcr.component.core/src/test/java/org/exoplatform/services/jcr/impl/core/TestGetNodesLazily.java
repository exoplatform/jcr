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
package org.exoplatform.services.jcr.impl.core;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.transaction.TransactionService;

import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RangeIterator;
import javax.jcr.RepositoryException;
import javax.transaction.UserTransaction;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:nzamosenchuk@exoplatform.com">Nikolay Zamosenchuk</a>
 * @version $Id: TestGetNode.java 111 2009-11-11 11:11:11Z nzamosenchuk $
 */
public class TestGetNodesLazily extends JcrImplBaseTest
{
   private static String INDEX_PROPERTY = "indexNumber";

   private NodeImpl testRoot;

   private int nodesCount;

   private TransactionService txService;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      testRoot = (NodeImpl)session.getRootNode().addNode("TestGetNodesLazily");

      // add first 150 child nodes
      nodesCount = 350;
      for (int i = 0; i < nodesCount; i++)
      {
         Node newNode = testRoot.addNode("child" + i);
         newNode.setProperty(INDEX_PROPERTY, i);
         newNode.addMixin("exo:owneable");
      }
      session.save();
      txService = (TransactionService)container.getComponentInstanceOfType(TransactionService.class);
   }

   /**
    * Children nodes counting checking.
    */
   public void testGetNodesCount() throws Exception
   {
      assertEquals(nodesCount, testRoot.getNodesCount());
   }

   /**
    * Simple check, session log empty.
    */
   public void testGetNodesLazilyBasicUsecase() throws Exception
   {
      // 150 nodes added in setup
      assertChildNodes(testRoot, nodesCount);
   }

   /**
    * All child they reordered one by one and though must be returned in same order 
    */
   public void testGetNodesLazilyReordered() throws Exception
   {
      // 150 nodes added in setup
      for (int i = 0; i < nodesCount; i++)
      {
         NodeImpl node = (NodeImpl)testRoot.getNode("child" + i);
         session.move(node.getPath(), node.getPath());
      }
      session.save();
      assertChildNodes(testRoot, nodesCount);
   }

   /**
    * All child they reordered one by one and though must be returned in same order 
    */
   public void testGetNodesLazilyReorderedBackwards() throws Exception
   {
      // 150 nodes added in setup
      // TODO : testcase fails
      for (int i = nodesCount - 1; i >= 0; i--)
      {
         NodeImpl node = (NodeImpl)testRoot.getNode("child" + i);
         session.move(node.getPath(), node.getPath());
      }
      session.save();
      assertChildNodes(testRoot, nodesCount, true);
   }

   /**
    * Session move with save
    */
   public void testGetNodesLazilyRenamedParent() throws Exception
   {
      // 150 nodes added in setup
      // renaming parent
      String newName = "new Name";
      session.move("/" + testRoot.getName(), "/" + newName);
      NodeImpl newTestRoot = (NodeImpl)root.getNode(newName);
      session.save();
      assertChildNodes(newTestRoot, nodesCount);
   }

   //=============== Tests with non-empty changesLog =============== 

   /**
    * New nodes added into session log and not save
    */
   public void testGetNodesLazilySessionAddedNodes() throws Exception
   {
      // 150 nodes added in setup
      // adding 10 more nodes without save, so it is missing in persistent layer but exist in session changes log
      for (int i = 0; i < 10; i++)
      {
         testRoot.addNode("child" + nodesCount).setProperty(INDEX_PROPERTY, nodesCount);
         nodesCount++;
      }

      assertChildNodes(testRoot, nodesCount);
   }

   /**
    * New nodes moved into session log and not save
    */
   public void testGetNodesLazilySessionMovedNodes() throws Exception
   {
      session.getRootNode().addNode("child" + nodesCount).setProperty(INDEX_PROPERTY, nodesCount);
      session.save();

      nodesCount++;

      String newNodeName = "child" + (nodesCount - 1);

      session.move("/" + newNodeName, testRoot.getPath() + "/" + newNodeName);
      assertChildNodes(testRoot, nodesCount);

      testRoot.getNode(newNodeName).remove();
      nodesCount--;

      assertChildNodes(testRoot, nodesCount);

      session.save();
      assertChildNodes(testRoot, nodesCount);
   }

   /**
    * New nodes moved into session log and not save
    */
   public void testGetNodesLazilySessionUpdatedNodes() throws Exception
   {
      testRoot.orderBefore("child110", "child0");
      RangeIterator iterator = testRoot.getNodesLazily();

      NodeImpl next = (NodeImpl)iterator.next();
      assertEquals(next.getName(), "child110");
      assertEquals(((NodeData)next.getData()).getOrderNumber(), 0);

      next = (NodeImpl)iterator.next();
      assertEquals(next.getName(), "child0");
      assertEquals(((NodeData)next.getData()).getOrderNumber(), 1);

      iterator.skip(108);
      next = (NodeImpl)iterator.next();
      assertEquals(next.getName(), "child109");
      assertEquals(((NodeData)next.getData()).getOrderNumber(), 110);
   }

   /**
    * Change mixin in node.
    */
   public void testGetNodesLazilySessionMixinChanged() throws Exception
   {
      testRoot.getNode("child0").addMixin("mix:lockable");

      NodeIterator iterator = testRoot.getNodesLazily();
      NodeImpl node = (NodeImpl)iterator.nextNode();

      assertTrue(node.isNodeType(Constants.MIX_LOCKABLE));
   }

   /**
    * New nodes added into session log and not save
    */
   public void testGetNodesLazilySessionNewAddedNodes() throws Exception
   {
      // adding 150 new nodes
      NodeImpl localRoot = (NodeImpl)testRoot.addNode("localRoot");
      for (int i = 0; i < nodesCount; i++)
      {
         localRoot.addNode("child" + i).setProperty(INDEX_PROPERTY, i);
      }
      assertChildNodes(localRoot, nodesCount);
   }

   /**
    * Last 10 nodes removed. Session log has removed states for them
    */
   public void testGetNodesLazilySessionRemovedNodes() throws Exception
   {
      // 150 nodes added in setup
      // removing 10 nodes without save
      for (int i = 0; i < 10; i++)
      {
         nodesCount--;
         testRoot.getNode("child" + nodesCount).remove();
      }
      assertChildNodes(testRoot, nodesCount);
   }

   /**
    * Session move without save
    */
   public void testGetNodesLazilySessionRenamedParent() throws Exception
   {
      // 150 nodes added in setup
      // renaming parent
      String newName = "new Name";
      session.move("/" + testRoot.getName(), "/" + newName);
      NodeImpl newTestRoot = (NodeImpl)root.getNode(newName);
      assertChildNodes(newTestRoot, nodesCount);
   }

   /**
    * All child nodes are in session changes log and both in persisted layer. All they reordered one by one and though must 
    * be returned in same order
    */
   public void testGetNodesLazilySessionReordered() throws Exception
   {
      // 150 nodes added in setup
      // TODO : testcase fails
      for (int i = nodesCount - 1; i >= 0; i--)
      {
         NodeImpl node = (NodeImpl)testRoot.getNode("child" + i);
         session.move(node.getPath(), node.getPath());
      }
      assertChildNodes(testRoot, nodesCount, true);
   }

   //=============== transactions related ===============

   public void testGetNodesLazilyTransaction() throws Exception
   {
      assertNotNull(txService);
      UserTransaction ut = txService.getUserTransaction();
      ut.begin();

      NodeImpl localRoot = (NodeImpl)testRoot.addNode("localRoot");
      for (int i = 0; i < nodesCount; i++)
      {
         localRoot.addNode("child" + i).setProperty(INDEX_PROPERTY, i);
      }
      // assert within session changes log
      assertChildNodes(localRoot, nodesCount);
      session.save();
      // assert within transaction changes log
      assertChildNodes(localRoot, nodesCount);
      ut.commit();
      // assert within persistent layer
      assertChildNodes(localRoot, nodesCount);
   }

   public void testGetNodesLazilyTransactionRollbackAdded() throws Exception
   {
      assertNotNull(txService);
      UserTransaction ut = txService.getUserTransaction();
      ut.begin();

      int txNodesCount = nodesCount;
      // 150 nodes added in setup
      // adding 10 more nodes without save, so it is missing in persistent layer but exist in session changes log
      for (int i = 0; i < 10; i++)
      {
         testRoot.addNode("child" + txNodesCount).setProperty(INDEX_PROPERTY, txNodesCount);
         txNodesCount++;
      }
      session.save();
      // assert within transaction changes log
      assertChildNodes(testRoot, txNodesCount);
      ut.rollback();
      // assert within persistent layer, nodes should be rolled back
      assertChildNodes(testRoot, nodesCount);
   }

   public void testGetNodesLazilyTransactionRollbackRemoved() throws Exception
   {
      assertNotNull(txService);
      UserTransaction ut = txService.getUserTransaction();
      ut.begin();

      // 150 nodes added in setup
      // removing 10 nodes
      int txNodesCount = nodesCount;
      for (int i = 0; i < 10; i++)
      {
         txNodesCount--;
         testRoot.getNode("child" + txNodesCount).remove();
      }
      session.save();

      // assert within transaction changes log
      assertChildNodes(testRoot, txNodesCount);
      ut.rollback();
      // assert within persistent layer, nodes should be rolled back
      assertChildNodes(testRoot, nodesCount);
   }

   //=============== test iterator ===============

   public void testGetNodesLazilyIterator() throws Exception
   {
      RangeIterator iterator = testRoot.getNodesLazily();

      // there are 150 node, so it must have next
      assertTrue(iterator.hasNext());
      // position is before first node 
      assertEquals(0, iterator.getPosition());

      // fetch first one (/child0)
      iterator.next();
      assertEquals(1, iterator.getPosition());

      // fetch second one (/child1)
      iterator.next();
      assertEquals(2, iterator.getPosition());

      // skip to /child12
      iterator.skip(10);
      NodeImpl next = (NodeImpl)iterator.next();
      assertEquals(13, iterator.getPosition());
      assertEquals(12, next.getProperty(INDEX_PROPERTY).getLong());

      iterator.skip(1);
      next = (NodeImpl)iterator.next();
      assertEquals(15, iterator.getPosition());
      assertEquals(14, next.getProperty(INDEX_PROPERTY).getLong());

      iterator.skip(100);
      next = (NodeImpl)iterator.next();
      assertEquals(116, iterator.getPosition());
      assertEquals(115, next.getProperty(INDEX_PROPERTY).getLong());

      iterator = testRoot.getNodesLazily();
      long size = iterator.getSize();
      iterator.skip(size);

      try
      {
         iterator.next();
         fail("Exception should be thrown");
      }
      catch (NoSuchElementException e)
      {
      }

      // remove nodes from 31..60 to make gap in interval of order numbers
      iterator = testRoot.getNodesLazily();
      iterator.skip(30);
      for (int i = 0; i < 30; i++)
      {
         ((NodeImpl)iterator.next()).remove();
      }
      testRoot.save();

      iterator = testRoot.getNodesLazily(10);
      size = 0;
      while (iterator.hasNext())
      {
         size++;
         iterator.next();
      }
      
      assertEquals(320, size);
   }

   //=============== stuff ===============

   /**
    * Performs complex assert, that retrieves child nodes in a lazy way and asserts it's order, content
    * and quantity 
    */
   private void assertChildNodes(ExtendedNode testNode, int expectedSize) throws RepositoryException
   {
      assertChildNodes(testNode, expectedSize, false);
   }

   /**
    * Performs complex assert, that retrieves child nodes in a lazy way and asserts it's order, content
    * and quantity 
    */
   private void assertChildNodes(ExtendedNode testNode, int expectedSize, boolean backwardOrder)
      throws RepositoryException
   {
      NodeIterator lazyIterator = testNode.getNodesLazily();

      // getSize shouldn't return actual size, since it works in a lazy manner and nodes are retrieved "on demand"
      //      int actualSize = (int)lazyIterator.getSize();
      //      assertEquals("getSize should return -1, but returned " + actualSize, -1, actualSize);

      if (expectedSize == 0)
      {
         assertFalse(lazyIterator.hasNext());
      }
      else
      {
         int number = 0;
         if (!backwardOrder)
         {
            int i = 0;
            while (lazyIterator.hasNext())
            {
               Node node = lazyIterator.nextNode();

               long actualNodeIndex = node.getProperty(INDEX_PROPERTY).getLong();
               assertEquals("Iterator must return nodes ordered by \"order num\". Occurred at: child" + actualNodeIndex
                  + " expecting <" + i + ">", i, actualNodeIndex);
               i++;
               number++;
            }
         }
         else
         {
            int i = expectedSize - 1;
            while (lazyIterator.hasNext())
            {
               NodeImpl node = (NodeImpl)lazyIterator.nextNode();
               long actualNodeIndex = node.getProperty(INDEX_PROPERTY).getLong();
               assertEquals("Iterator must return nodes ordered by \"order num\". Occurred at: child" + actualNodeIndex
                  + " expecting <" + i + ">", i, actualNodeIndex);
               i--;
               number++;
            }
         }

         // assert all returned
         assertEquals(
            "Iterator returned wrong number of nodes. Expected: " + expectedSize + ", but returned " + number,
            expectedSize, number);
      }
   }

   @Override
   protected void tearDown() throws Exception
   {
      session.refresh(false);
      testRoot.remove();
      session.save();
      super.tearDown();
   }

}