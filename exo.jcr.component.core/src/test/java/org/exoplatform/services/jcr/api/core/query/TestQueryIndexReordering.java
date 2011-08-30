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
package org.exoplatform.services.jcr.api.core.query;

import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TestIndexReordering.java 111 2008-11-11 11:11:11Z serg $
 */
public class TestQueryIndexReordering extends BaseUsecasesTest
{

   public void testReordering() throws Exception
   {
      Node testRoot = this.root.addNode("testSameNameSiblingDelete");

      Node subNode_1 = testRoot.addNode("node", "nt:unstructured"); // 1
      subNode_1.addMixin("mix:referenceable");
      subNode_1.setProperty("prop", "data 1");
      Node subNode_2 = testRoot.addNode("node", "nt:unstructured"); // 2
      subNode_2.addMixin("mix:referenceable");
      subNode_2.setProperty("prop", "data 2");
      Node subNode_3 = testRoot.addNode("node", "nt:unstructured"); // 3
      subNode_3.addMixin("mix:referenceable");
      subNode_3.setProperty("prop", "data 3");
      session.save();

      //check the index
      String sqlQuery;
      Query query;
      QueryResult queryResult;
      NodeIterator iterator;
      Node node;
      QueryManager qm = session.getWorkspace().getQueryManager();

      sqlQuery = "SELECT * FROM nt:unstructured WHERE jcr:path = '/testSameNameSiblingDelete/node[1]'";
      query = qm.createQuery(sqlQuery, Query.SQL);
      queryResult = query.execute();
      iterator = queryResult.getNodes();
      assertTrue("Node expected ", iterator.getSize() == 1);
      node = iterator.nextNode();
      assertEquals("Wrong id ", subNode_1.getUUID(), node.getUUID());
      assertEquals("Wrong path ", subNode_1.getPath(), node.getPath());

      sqlQuery = "SELECT * FROM nt:unstructured WHERE jcr:path = '/testSameNameSiblingDelete/node[2]'";
      query = qm.createQuery(sqlQuery, Query.SQL);
      queryResult = query.execute();
      iterator = queryResult.getNodes();
      assertTrue("Node expected ", iterator.getSize() == 1);
      node = iterator.nextNode();
      assertEquals("Wrong id ", subNode_2.getUUID(), node.getUUID());
      assertEquals("Wrong path ", subNode_2.getPath(), node.getPath());

      sqlQuery = "SELECT * FROM nt:unstructured WHERE jcr:path = '/testSameNameSiblingDelete/node[3]'";
      query = qm.createQuery(sqlQuery, Query.SQL);
      queryResult = query.execute();
      iterator = queryResult.getNodes();
      assertTrue("Node expected ", iterator.getSize() == 1);
      node = iterator.nextNode();
      assertEquals("Wrong id ", subNode_3.getUUID(), node.getUUID());
      assertEquals("Wrong path ", subNode_3.getPath(), node.getPath());

      // usecase - order to the end,
      // i.e. /testSameNameSiblingDelete/node[2] will be UPDATED to
      // /testSameNameSiblingDelete/node[1]
      // i.e. /testSameNameSiblingDelete/node[3] will be UPDATED to
      // /testSameNameSiblingDelete/node[2]
      subNode_1.getParent().orderBefore("node", null);
      session.save();

      sqlQuery = "SELECT * FROM nt:unstructured WHERE jcr:path = '/testSameNameSiblingDelete/node[1]'";
      query = qm.createQuery(sqlQuery, Query.SQL);
      queryResult = query.execute();
      iterator = queryResult.getNodes();
      assertTrue("Node expected ", iterator.getSize() == 1);
      node = iterator.nextNode();
      assertEquals("Wrong id ", subNode_2.getUUID(), node.getUUID());
      assertEquals("Wrong path ", subNode_2.getPath(), node.getPath());

      sqlQuery = "SELECT * FROM nt:unstructured WHERE jcr:path = '/testSameNameSiblingDelete/node[2]'";
      query = qm.createQuery(sqlQuery, Query.SQL);
      queryResult = query.execute();
      iterator = queryResult.getNodes();
      assertTrue("Node expected ", iterator.getSize() == 1);
      node = iterator.nextNode();
      assertEquals("Wrong id ", subNode_3.getUUID(), node.getUUID());
      assertEquals("Wrong path ", subNode_3.getPath(), node.getPath());

      sqlQuery = "SELECT * FROM nt:unstructured WHERE jcr:path = '/testSameNameSiblingDelete/node[3]'";
      query = qm.createQuery(sqlQuery, Query.SQL);
      queryResult = query.execute();
      iterator = queryResult.getNodes();
      assertTrue("Node expected ", iterator.getSize() == 1);
      node = iterator.nextNode();
      assertEquals("Wrong id ", subNode_1.getUUID(), node.getUUID());
      assertEquals("Wrong path ", subNode_1.getPath(), node.getPath());
   }

}
