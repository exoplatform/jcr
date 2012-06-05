/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.core.query;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyType;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public class TestOrderByCaseSensitive extends BaseQueryTest
{
   /**
    * Class logger.
    */
   private final Log log = ExoLogger.getLogger("exo.jcr.component.core.TestOrderByCaseSensitive");

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      Calendar c1 = Calendar.getInstance();
      c1.set(2001, 4, 20, 14, 35, 14);
      Calendar c2 = Calendar.getInstance();
      c2.set(2002, 5, 20, 14, 35, 14);
      Calendar c3 = Calendar.getInstance();
      c3.set(2003, 4, 20, 14, 35, 13);

      Node testRoot = root.addNode("testRoot");

      Node node1 = testRoot.addNode("node1");
      node1.setProperty("date", c1);
      node1.setProperty("exo:desc", "CCC");
      node1.setProperty("exo:title", "AAA");
      node1.setProperty("exo:name", "exo:AAA", PropertyType.NAME);
      node1.setProperty("exo:path", "/A/A/A", PropertyType.PATH);

      Node node2 = testRoot.addNode("node2");
      node2.setProperty("date", c2);
      node2.setProperty("exo:desc", "AAA");
      node2.setProperty("exo:title", "XXX");
      node2.setProperty("exo:name", "exo:XXX", PropertyType.NAME);
      node2.setProperty("exo:path", "/X/X/X", PropertyType.PATH);

      Node node3 = testRoot.addNode("node3");
      node3.setProperty("date", c3);
      node3.setProperty("exo:desc", "AAA");
      node3.setProperty("exo:title", "bbb");
      node3.setProperty("exo:name", "exo:bbb", PropertyType.NAME);
      node3.setProperty("exo:path", "/b/b/b", PropertyType.PATH);

      root.save();
   }

   public void testLowerCase() throws Exception
   {
      String sql = "select * from nt:unstructured where jcr:path like '/testRoot/%' order by date desc";
      QueryManager qm = workspace.getQueryManager();
      Query query = qm.createQuery(sql, Query.SQL);
      QueryResult result = query.execute();
      NodeIterator nodes = result.getNodes();
      assertEquals(3, nodes.getSize());
      assertEquals("node3", nodes.nextNode().getName());
      assertEquals("node2", nodes.nextNode().getName());
      assertEquals("node1", nodes.nextNode().getName());

   }

   public void testOrderByUpperCase() throws Exception
   {
      String sql = "select * from nt:unstructured where jcr:path like '/testRoot/%' ORDER BY date desc";
      QueryManager qm = workspace.getQueryManager();
      Query query = qm.createQuery(sql, Query.SQL);
      QueryResult result = query.execute();
      NodeIterator nodes = result.getNodes();

      assertEquals(3, nodes.getSize());
      assertEquals("node3", nodes.nextNode().getName());
      assertEquals("node2", nodes.nextNode().getName());
      assertEquals("node1", nodes.nextNode().getName());
   }

   public void testDESCUpperCase() throws Exception
   {
      String sql = "select * from nt:unstructured where jcr:path like '/testRoot/%' order by date DESC";
      QueryManager qm = workspace.getQueryManager();
      Query query = qm.createQuery(sql, Query.SQL);
      QueryResult result = query.execute();
      NodeIterator nodes = result.getNodes();

      assertEquals(3, nodes.getSize());
      assertEquals("node3", nodes.nextNode().getName());
      assertEquals("node2", nodes.nextNode().getName());
      assertEquals("node1", nodes.nextNode().getName());
   }

   public void testCaseInsensitiveAscOrder() throws Exception
   {
      String sql = "select * from nt:unstructured where jcr:path like '/testRoot/%' order by exo:title asc";
      QueryManager qm = workspace.getQueryManager();
      QueryImpl query = (QueryImpl)qm.createQuery(sql, Query.SQL);
      query.setCaseInsensitiveOrder(true);

      QueryResult result = query.execute();
      NodeIterator nodes = result.getNodes();

      assertEquals(3, nodes.getSize());
      assertEquals("AAA", nodes.nextNode().getProperty("exo:title").getString());
      assertEquals("bbb", nodes.nextNode().getProperty("exo:title").getString());
      assertEquals("XXX", nodes.nextNode().getProperty("exo:title").getString());

      query.setCaseInsensitiveOrder(false);

      result = query.execute();
      nodes = result.getNodes();

      assertEquals(3, nodes.getSize());
      assertEquals("AAA", nodes.nextNode().getProperty("exo:title").getString());
      assertEquals("XXX", nodes.nextNode().getProperty("exo:title").getString());
      assertEquals("bbb", nodes.nextNode().getProperty("exo:title").getString());
   }

   public void testCaseInsensitiveDescOrder() throws Exception
   {
      String sql = "select * from nt:unstructured where jcr:path like '/testRoot/%' order by exo:title desc";
      QueryManager qm = workspace.getQueryManager();
      QueryImpl query = (QueryImpl)qm.createQuery(sql, Query.SQL);
      query.setCaseInsensitiveOrder(true);

      QueryResult result = query.execute();
      NodeIterator nodes = result.getNodes();

      assertEquals(3, nodes.getSize());
      assertEquals("XXX", nodes.nextNode().getProperty("exo:title").getString());
      assertEquals("bbb", nodes.nextNode().getProperty("exo:title").getString());
      assertEquals("AAA", nodes.nextNode().getProperty("exo:title").getString());

      query.setCaseInsensitiveOrder(false);

      result = query.execute();
      nodes = result.getNodes();

      assertEquals(3, nodes.getSize());
      assertEquals("bbb", nodes.nextNode().getProperty("exo:title").getString());
      assertEquals("XXX", nodes.nextNode().getProperty("exo:title").getString());
      assertEquals("AAA", nodes.nextNode().getProperty("exo:title").getString());
   }

   public void testCaseInsensitiveAscOrder2Props() throws Exception
   {
      String sql = "select * from nt:unstructured where jcr:path like '/testRoot/%' order by exo:desc, exo:title asc";
      QueryManager qm = workspace.getQueryManager();
      QueryImpl query = (QueryImpl)qm.createQuery(sql, Query.SQL);
      query.setCaseInsensitiveOrder(true);

      QueryResult result = query.execute();
      NodeIterator nodes = result.getNodes();

      assertEquals(3, nodes.getSize());

      Node node = nodes.nextNode();
      assertEquals("AAA", node.getProperty("exo:desc").getString());
      assertEquals("bbb", node.getProperty("exo:title").getString());

      node = nodes.nextNode();
      assertEquals("AAA", node.getProperty("exo:desc").getString());
      assertEquals("XXX", node.getProperty("exo:title").getString());

      node = nodes.nextNode();
      assertEquals("CCC", node.getProperty("exo:desc").getString());
      assertEquals("AAA", node.getProperty("exo:title").getString());

      query.setCaseInsensitiveOrder(false);

      result = query.execute();
      nodes = result.getNodes();

      assertEquals(3, nodes.getSize());

      node = nodes.nextNode();
      assertEquals("AAA", node.getProperty("exo:desc").getString());
      assertEquals("XXX", node.getProperty("exo:title").getString());

      node = nodes.nextNode();
      assertEquals("AAA", node.getProperty("exo:desc").getString());
      assertEquals("bbb", node.getProperty("exo:title").getString());

      node = nodes.nextNode();
      assertEquals("CCC", node.getProperty("exo:desc").getString());
      assertEquals("AAA", node.getProperty("exo:title").getString());
   }

   public void testCaseInsensitiveAscOrderNameProperty() throws Exception
   {
      String sql = "select * from nt:unstructured where jcr:path like '/testRoot/%' order by exo:name asc";
      QueryManager qm = workspace.getQueryManager();
      QueryImpl query = (QueryImpl)qm.createQuery(sql, Query.SQL);
      query.setCaseInsensitiveOrder(true);

      QueryResult result = query.execute();
      NodeIterator nodes = result.getNodes();

      assertEquals(3, nodes.getSize());
      assertEquals("exo:AAA", nodes.nextNode().getProperty("exo:name").getString());
      assertEquals("exo:bbb", nodes.nextNode().getProperty("exo:name").getString());
      assertEquals("exo:XXX", nodes.nextNode().getProperty("exo:name").getString());

      query.setCaseInsensitiveOrder(false);

      result = query.execute();
      nodes = result.getNodes();

      assertEquals(3, nodes.getSize());
      assertEquals("exo:AAA", nodes.nextNode().getProperty("exo:name").getString());
      assertEquals("exo:XXX", nodes.nextNode().getProperty("exo:name").getString());
      assertEquals("exo:bbb", nodes.nextNode().getProperty("exo:name").getString());
   }

   public void testCaseInsensitiveAscOrderPathProperty() throws Exception
   {
      String sql = "select * from nt:unstructured where jcr:path like '/testRoot/%' order by exo:path asc";
      QueryManager qm = workspace.getQueryManager();
      QueryImpl query = (QueryImpl)qm.createQuery(sql, Query.SQL);
      query.setCaseInsensitiveOrder(true);

      QueryResult result = query.execute();
      NodeIterator nodes = result.getNodes();

      assertEquals(3, nodes.getSize());
      assertEquals("/A/A/A", nodes.nextNode().getProperty("exo:path").getString());
      assertEquals("/b/b/b", nodes.nextNode().getProperty("exo:path").getString());
      assertEquals("/X/X/X", nodes.nextNode().getProperty("exo:path").getString());

      query.setCaseInsensitiveOrder(false);

      result = query.execute();
      nodes = result.getNodes();

      assertEquals(3, nodes.getSize());
      assertEquals("/A/A/A", nodes.nextNode().getProperty("exo:path").getString());
      assertEquals("/X/X/X", nodes.nextNode().getProperty("exo:path").getString());
      assertEquals("/b/b/b", nodes.nextNode().getProperty("exo:path").getString());
   }

   public void testCaseInsensitiveAscCompoundOrder() throws Exception
   {
      Node test = root.getNode("testRoot");
      test.getNode("node1").addNode("child").setProperty("exo:name", "AAA", PropertyType.NAME);
      test.getNode("node2").addNode("child").setProperty("exo:name", "XXX", PropertyType.NAME);
      test.getNode("node3").addNode("child").setProperty("exo:name", "bbb", PropertyType.NAME);
      root.save();

      String sql = "testRoot/* order by child/@exo:name";
      QueryManager qm = workspace.getQueryManager();
      QueryImpl query = (QueryImpl)qm.createQuery(sql, Query.XPATH);
      query.setCaseInsensitiveOrder(true);

      QueryResult result = query.execute();
      NodeIterator nodes = result.getNodes();

      assertEquals(3, nodes.getSize());
      assertEquals("AAA", nodes.nextNode().getNode("child").getProperty("exo:name").getString());
      assertEquals("bbb", nodes.nextNode().getNode("child").getProperty("exo:name").getString());
      assertEquals("XXX", nodes.nextNode().getNode("child").getProperty("exo:name").getString());

      query.setCaseInsensitiveOrder(false);

      result = query.execute();
      nodes = result.getNodes();

      assertEquals(3, nodes.getSize());
      assertEquals("AAA", nodes.nextNode().getNode("child").getProperty("exo:name").getString());
      assertEquals("XXX", nodes.nextNode().getNode("child").getProperty("exo:name").getString());
      assertEquals("bbb", nodes.nextNode().getNode("child").getProperty("exo:name").getString());
   }

}
