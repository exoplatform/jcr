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
 * @version $Id: TestQueryMoveNode.java 111 2011-28-01 11:11:11Z serg $
 */
public class TestQueryMoveNode extends BaseUsecasesTest
{

   public void testReordering() throws Exception
   {
      Node testRoot = this.root.addNode("testSameNameSiblingDelete");

      Node subNode_1 = testRoot.addNode("node", "nt:unstructured"); // 1
      subNode_1.addMixin("mix:referenceable");
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

      // move
      testRoot.addNode("folder");
      session.save();
      session.move("/testSameNameSiblingDelete/node", "/testSameNameSiblingDelete/folder/node");
      session.save();

      sqlQuery = "SELECT * FROM nt:unstructured WHERE jcr:path = '/testSameNameSiblingDelete/node[1]'";
      query = qm.createQuery(sqlQuery, Query.SQL);
      queryResult = query.execute();
      iterator = queryResult.getNodes();
      assertTrue("There must be no node ", iterator.getSize() == 0);

      sqlQuery = "SELECT * FROM nt:unstructured WHERE jcr:path = '/testSameNameSiblingDelete/folder/node[1]'";
      query = qm.createQuery(sqlQuery, Query.SQL);
      queryResult = query.execute();
      iterator = queryResult.getNodes();
      assertTrue("Node expected ", iterator.getSize() == 1);
      node = iterator.nextNode();
      assertEquals("Wrong id ", subNode_1.getUUID(), node.getUUID());
      assertEquals("Wrong path ", subNode_1.getPath(), node.getPath());
   }

}
