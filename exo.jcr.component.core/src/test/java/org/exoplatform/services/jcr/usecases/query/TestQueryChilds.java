/*
 * Copyright (C) 2011 eXo Platform SAS.
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
package org.exoplatform.services.jcr.usecases.query;

import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

/**
 * @author <a href="mailto:skarpenko@exoplatform.com">Sergiy Karpenko</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 16 ????. 2011 skarpenko $
 *
 */
public class TestQueryChilds extends BaseUsecasesTest
{
   public void testGetChilds() throws Exception
   {
      Node testRoot = this.root.addNode("testSameNameSiblingDelete");

      Node subNode_1 = testRoot.addNode("node", "nt:unstructured"); // 1
      Node subNode_1_1 = subNode_1.addNode("node1", "nt:unstructured");
      Node subNode_1_2 = subNode_1.addNode("node2", "nt:unstructured");
      Node subNode_1_1_1 = subNode_1_1.addNode("node11", "nt:unstructured");
      //Node subNode_1_1_2 = subNode_1_1.addNode("node12", "nt:unstructured");
      Node subNode_2 = testRoot.addNode("node", "nt:unstructured"); // 2
      Node subNode_2_1 = subNode_2.addNode("node3", "nt:unstructured");
      Node subNode_2_2 = subNode_2.addNode("node4", "nt:unstructured");

      session.save();

      //check the index
      String sqlQuery;
      Query query;
      QueryResult queryResult;
      NodeIterator iterator;
      Node node;
      QueryManager qm = session.getWorkspace().getQueryManager();

      sqlQuery = "SELECT * FROM nt:unstructured WHERE jcr:path LIKE '/testSameNameSiblingDelete/node/%'";
      query = qm.createQuery(sqlQuery, Query.SQL);
      queryResult = query.execute();
      iterator = queryResult.getNodes();
      assertTrue("Node expected ", iterator.getSize() == 3);
      //      node = iterator.nextNode();
      //      assertEquals("Wrong id ", subNode_1.getUUID(), node.getUUID());
      //      assertEquals("Wrong path ", subNode_1.getPath(), node.getPath());
      testNames(iterator, new String[]{"node1", "node2", "node11"}); //, "node3", "node4" 

      //      // move
      //      testRoot.addNode("folder");
      //      session.save();
      //      session.move("/testSameNameSiblingDelete/node", "/testSameNameSiblingDelete/folder/node");
      //      session.save();
      //
      //      sqlQuery = "SELECT * FROM nt:unstructured WHERE jcr:path = '/testSameNameSiblingDelete/node[1]'";
      //      query = qm.createQuery(sqlQuery, Query.SQL);
      //      queryResult = query.execute();
      //      iterator = queryResult.getNodes();
      //      assertTrue("There must be no node ", iterator.getSize() == 0);
      //
      //      sqlQuery = "SELECT * FROM nt:unstructured WHERE jcr:path = '/testSameNameSiblingDelete/folder/node[1]'";
      //      query = qm.createQuery(sqlQuery, Query.SQL);
      //      queryResult = query.execute();
      //      iterator = queryResult.getNodes();
      //      assertTrue("Node expected ", iterator.getSize() == 1);
      //      node = iterator.nextNode();
      //      assertEquals("Wrong id ", subNode_1.getUUID(), node.getUUID());
      //      assertEquals("Wrong path ", subNode_1.getPath(), node.getPath());
   }

}
