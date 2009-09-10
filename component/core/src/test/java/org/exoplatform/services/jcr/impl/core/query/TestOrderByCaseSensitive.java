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
   private final Log log = ExoLogger.getLogger("jcr.TestOrderByCaseSensitive");

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

   }

   public void testLowerCase() throws Exception
   {
      Calendar c1 = Calendar.getInstance();
      c1.set(2001, 4, 20, 14, 35, 14);
      Calendar c2 = Calendar.getInstance();
      c2.set(2002, 5, 20, 14, 35, 14);
      Calendar c3 = Calendar.getInstance();
      c3.set(2003, 4, 20, 14, 35, 13);

      Node testRoot = root.addNode("testRoot");
      Node node1 = testRoot.addNode("node1");
      node1.setProperty("date", c1);
      Node node2 = testRoot.addNode("node2");
      node2.setProperty("date", c2);
      Node node3 = testRoot.addNode("node3");
      node3.setProperty("date", c3);
      root.save();
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
      Calendar c1 = Calendar.getInstance();
      c1.set(2001, 4, 20, 14, 35, 14);
      Calendar c2 = Calendar.getInstance();
      c2.set(2002, 5, 20, 14, 35, 14);
      Calendar c3 = Calendar.getInstance();
      c3.set(2003, 4, 20, 14, 35, 13);

      Node testRoot = root.addNode("testRoot");
      Node node1 = testRoot.addNode("node1");
      node1.setProperty("date", c1);
      Node node2 = testRoot.addNode("node2");
      node2.setProperty("date", c2);
      Node node3 = testRoot.addNode("node3");
      node3.setProperty("date", c3);
      root.save();
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
      Calendar c1 = Calendar.getInstance();
      c1.set(2001, 4, 20, 14, 35, 14);
      Calendar c2 = Calendar.getInstance();
      c2.set(2002, 5, 20, 14, 35, 14);
      Calendar c3 = Calendar.getInstance();
      c3.set(2003, 4, 20, 14, 35, 13);

      Node testRoot = root.addNode("testRoot");
      Node node1 = testRoot.addNode("node1");
      node1.setProperty("date", c1);
      Node node2 = testRoot.addNode("node2");
      node2.setProperty("date", c2);
      Node node3 = testRoot.addNode("node3");
      node3.setProperty("date", c3);
      root.save();
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
}
