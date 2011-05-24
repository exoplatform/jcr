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
import org.exoplatform.services.jcr.datamodel.NodeData;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 13.11.2009
 *
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a> 
 * @version $Id$
 */
public class TestNodeOrder extends JcrImplBaseTest
{

   public void testOrder() throws Exception
   {
      Node testNode = root.addNode("testNode");
      session.save();
      testNode.addNode("z");
      session.save();
      testNode.addNode("a");
      session.save();
      testNode.addNode("b");
      session.save();
      testNode.addNode("c");

      // check
      NodeIterator nodes = testNode.getNodes();
      assertEquals(4, nodes.getSize());

      int order = 0;
      for (; nodes.hasNext();)
      {
         NodeImpl n = (NodeImpl)nodes.nextNode();
         int orderNumb = ((NodeData)n.getData()).getOrderNumber();
         log.info(orderNumb + ": " + n.getPath());

         assertEquals(order++, orderNumb);
      }
   }

   public void testOrderInSession() throws Exception
   {
      Node testNode = root.addNode("testNode");
      session.save();

      testNode.addNode("a");
      testNode.addNode("a");
      testNode.addNode("z");
      testNode.addNode("a");

      // check
      NodeIterator nodes = testNode.getNodes();
      assertEquals(4, nodes.getSize());

      int order = 0;
      for (; nodes.hasNext();)
      {
         NodeImpl n = (NodeImpl)nodes.nextNode();
         int orderNumb = ((NodeData)n.getData()).getOrderNumber();
         log.info(orderNumb + ": " + n.getPath());

         assertEquals(order++, orderNumb);
      }
   }

   public void testOrderCombined() throws Exception
   {
      Node testNode = root.addNode("testNode");
      session.save();

      testNode.addNode("a");
      testNode.addNode("a");
      testNode.addNode("z");
      testNode.addNode("a");
      session.save();

      testNode.addNode("a");
      testNode.addNode("y");
      session.save();

      testNode.addNode("c");
      testNode.addNode("a");
      session.save();

      // check
      NodeIterator nodes = testNode.getNodes();
      assertEquals(8, nodes.getSize());

      int order = 0;
      for (; nodes.hasNext();)
      {
         NodeImpl n = (NodeImpl)nodes.nextNode();
         int orderNumb = ((NodeData)n.getData()).getOrderNumber();
         log.info(orderNumb + ": " + n.getPath());

         assertEquals(order++, orderNumb);
      }
   }

   public void testOrderWithRefreshDiscard() throws Exception
   {
      Node testNode = root.addNode("testNode");
      session.save();

      testNode.addNode("a");
      testNode.addNode("a");
      testNode.addNode("z");
      testNode.addNode("a");
      session.save();

      testNode.addNode("a");
      testNode.addNode("y");
      root.refresh(false); // discard added a and y

      testNode.addNode("c");
      testNode.addNode("a");
      session.save();

      // check
      NodeIterator nodes = testNode.getNodes();
      assertEquals(6, nodes.getSize());

      int order = 0;
      for (; nodes.hasNext();)
      {
         NodeImpl n = (NodeImpl)nodes.nextNode();
         int orderNumb = ((NodeData)n.getData()).getOrderNumber();
         log.info(orderNumb + ": " + n.getPath());

         assertEquals(order++, orderNumb);
      }
   }

   public void testOrderWithInvalidation() throws Exception
   {
      Node testNode = root.addNode("testNode");
      session.save();

      testNode.addNode("a");
      testNode.addNode("a");
      testNode.addNode("z");
      testNode.addNode("a");
      session.save();

      testNode.addNode("a");
      testNode.addNode("y");

      // add some in another session
      Session another = repository.login(credentials, root.getSession().getWorkspace().getName());
      Node anotherTest = another.getRootNode().getNode("testNode");
      another.save();
      anotherTest.addNode("a");
      anotherTest.addNode("y");
      another.save();
      another.logout();

      try
      {
         session.save();
         fail("Nodes already added in another session, ItemExistsException should be thrown");
      }
      catch (ItemExistsException e)
      {
         // ok
         root.refresh(false); // discard a and y
      }

      testNode.addNode("c");
      testNode.addNode("a");
      session.save();

      // check
      NodeIterator nodes = testNode.getNodes();
      assertEquals(8, nodes.getSize());

      int order = 0;
      for (; nodes.hasNext();)
      {
         NodeImpl n = (NodeImpl)nodes.nextNode();
         int orderNumb = ((NodeData)n.getData()).getOrderNumber();
         log.info(orderNumb + ": " + n.getPath());

         assertEquals(order++, orderNumb);
      }
   }

   public void testOrderWithRefreshKeep() throws Exception
   {
      Node testNode = root.addNode("testNode");
      session.save();

      testNode.addNode("a");
      testNode.addNode("a");
      testNode.addNode("z");
      testNode.addNode("a");
      session.save();

      // add some in another session
      Session another = repository.login(credentials, root.getSession().getWorkspace().getName());
      Node anotherTest = another.getRootNode().getNode("testNode");
      another.save();
      anotherTest.addNode("a");
      anotherTest.addNode("y");
      another.save();
      another.logout();

      //root.refresh(true); // refresh to see another Session
      testNode.addNode("a");
      testNode.addNode("y");
      session.save();

      // check
      NodeIterator nodes = testNode.getNodes();
      assertEquals(8, nodes.getSize());

      int order = 0;
      for (; nodes.hasNext();)
      {
         NodeImpl n = (NodeImpl)nodes.nextNode();
         int orderNumb = ((NodeData)n.getData()).getOrderNumber();
         log.info(orderNumb + ": " + n.getPath());

         assertEquals(order++, orderNumb);
      }
   }

   public void testRemoveOrder() throws Exception
   {
      Node list = session.getRootNode().addNode("list");
      assertTrue(list.getPrimaryNodeType().hasOrderableChildNodes());
      list.addNode("foo", "nt:unstructured");
      list.addNode("bar", "nt:unstructured");
      list.addNode("juu", "nt:unstructured");
      session.save();

      list = session.getRootNode().getNode("list");
      list.getNode("bar").remove();
      session.save();

      list = session.getRootNode().getNode("list");
      list.addNode("daa", "nt:unstructured");
      session.save();

      //check order numbers
      NodeImpl foo = (NodeImpl)list.getNode("foo");
      assertEquals(0, ((NodeData)foo.getData()).getOrderNumber());
      NodeImpl juu = (NodeImpl)list.getNode("juu");
      assertEquals(2, ((NodeData)juu.getData()).getOrderNumber());
      NodeImpl daa = (NodeImpl)list.getNode("daa");
      assertEquals(3, ((NodeData)daa.getData()).getOrderNumber());

      //     list.orderBefore("daa", null);
      NodeIterator it = list.getNodes();
      foo = (NodeImpl)it.nextNode();
      assertEquals("foo", foo.getName());
      juu = (NodeImpl)it.nextNode();
      assertEquals("juu", juu.getName());
      daa = (NodeImpl)it.nextNode();
      assertEquals("daa", daa.getName());
      assertFalse(it.hasNext());
      session.save();
   }

}
