/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.usecases;

import org.exoplatform.services.jcr.impl.core.SessionImpl;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>Date:
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TestGetNodesByPattern.java 111 11 ����. 2011 serg $
 */
public class TestGetNodesByPattern extends BaseUsecasesTest
{
   public void testPatternWithEscapedSymbols() throws Exception
   {
      session = (SessionImpl)repository.login(credentials, WORKSPACE);
      Node root = session.getRootNode();
      Node node = root.getNode("childNode").addNode("testNode", "nt:unstructured");

      node.addNode("node.txt");
      node.addNode("node_txt");
      node.addNode("node3txt");
      root.save();

      NodeIterator iterator = node.getNodes("node.tx*");
      assertTrue(iterator.hasNext());
      assertEquals(iterator.nextNode().getName(), "node.txt");
      assertFalse(iterator.hasNext());

      iterator = node.getNodes("node_tx*");
      assertTrue(iterator.hasNext());
      assertEquals(iterator.nextNode().getName(), "node_txt");
      assertFalse(iterator.hasNext());

      iterator = node.getNodes("node_tx* | boo");
      assertTrue(iterator.hasNext());
      assertEquals(iterator.nextNode().getName(), "node_txt");
      assertFalse(iterator.hasNext());

      iterator = node.getNodes("node.txt");
      assertTrue(iterator.hasNext());
      assertEquals(iterator.nextNode().getName(), "node.txt");
      assertFalse(iterator.hasNext());

      iterator = node.getNodes("nodata.txt");
      assertFalse(iterator.hasNext());

      iterator = node.getNodes("nodata.t*t");
      assertFalse(iterator.hasNext());

   }

   public void testCaching() throws Exception
   {
      session = (SessionImpl)repository.login(credentials, WORKSPACE);
      Node root = session.getRootNode();
      Node node = root.getNode("childNode").addNode("testNode", "nt:unstructured");

      node.addNode("cassiopeia");
      node.addNode("casio");
      node.addNode("cassandra");
      node.addNode("libra");
      node.addNode("equilibrium");
      node.addNode("equality");
      root.save();
      for (int i = 0; i < 100; i++)
      {
         node.addNode("node" + i);
      }
      root.save();

      long executionTime = System.currentTimeMillis();
      NodeIterator iterator = node.getNodes("cass* | *lib*");
      executionTime = System.currentTimeMillis() - executionTime;
      assertTrue(iterator.hasNext());
      testNames(iterator, new String[]{"cassiopeia", "cassandra", "libra", "equilibrium"});

      long nextExecutionTime = System.currentTimeMillis();
      iterator = node.getNodes("cass* | *lib*");
      nextExecutionTime = System.currentTimeMillis() - nextExecutionTime;
      assertTrue(iterator.hasNext());
      testNames(iterator, new String[]{"cassiopeia", "cassandra", "libra", "equilibrium"});
   }

   public void testNamespaces() throws Exception
   {
      session = (SessionImpl)repository.login(credentials, WORKSPACE);
      Node root = session.getRootNode();
      Node node = root.getNode("childNode").addNode("testNode", "nt:unstructured");

      node.addNode("exo:cassiopeia");
      node.addNode("jcr:casio");
      node.addNode("nt:cassandra");
      node.addNode("exo:libra");
      node.addNode("jcr:equilibrium");
      node.addNode("exo:equality");
      root.save();
      for (int i = 0; i < 100; i++)
      {
         node.addNode("node" + i);
      }
      root.save();

      long executionTime = System.currentTimeMillis();
      NodeIterator iterator = node.getNodes("*:cass* | *:*lib*");
      executionTime = System.currentTimeMillis() - executionTime;
      assertTrue(iterator.hasNext());
      testNames(iterator, new String[]{"exo:cassiopeia", "nt:cassandra", "exo:libra", "jcr:equilibrium"});

      long nextExecutionTime = System.currentTimeMillis();
      iterator = node.getNodes("*:cass* | *:*lib*");
      nextExecutionTime = System.currentTimeMillis() - nextExecutionTime;
      assertTrue(iterator.hasNext());
      testNames(iterator, new String[]{"exo:cassiopeia", "nt:cassandra", "exo:libra", "jcr:equilibrium"});
   }

   public void testCacheUpdate() throws Exception
   {
      session = (SessionImpl)repository.login(credentials, WORKSPACE);
      Node root = session.getRootNode();
      Node node = root.getNode("childNode").addNode("testNode", "nt:unstructured");

      node.addNode("cassiopeia");
      node.addNode("casio");
      node.addNode("cassandra");
      node.addNode("libra");
      node.addNode("equilibrium");
      node.addNode("equality");
      root.save();
      for (int i = 0; i < 100; i++)
      {
         node.addNode("node" + i);
      }
      root.save();

      NodeIterator iterator = node.getNodes("cass* ");
      assertTrue(iterator.hasNext());
      testNames(iterator, new String[]{"cassiopeia", "cassandra"});

      // add new node 
      node.addNode("cassa");
      root.save();

      iterator = node.getNodes("cass*");
      assertTrue(iterator.hasNext());
      testNames(iterator, new String[]{"cassiopeia", "cassandra", "cassa"});

      // remove node
      node.getNode("cassiopeia").remove();
      root.save();

      iterator = node.getNodes("cass*");
      assertTrue(iterator.hasNext());
      testNames(iterator, new String[]{"cassandra", "cassa"});
   }

   public void setUp() throws Exception
   {
      super.setUp();
      Node root = session.getRootNode();
      root.addNode("childNode");
      root.save();
   }

   public void tearDown() throws Exception
   {
      Node root = session.getRootNode();
      Node node = root.getNode("childNode");
      node.remove();
      session.save();

      super.tearDown();
   }

}
