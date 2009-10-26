/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.services.jcr.usecases.query;

import org.exoplatform.services.jcr.JcrAPIBaseTest;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: TestQueryReferenceNode.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestQueryReferenceNode extends JcrAPIBaseTest
{

   public void testGetReferences() throws Exception
   {

      String sqlQuery = "select * from nt:unstructured where jcr:path like '/queryNode/%' ";
      // Session session = repository.getSystemSession(repository.getSystemWorkspaceName()) ;
      Node rootNode = session.getRootNode();
      Node queryNode = rootNode.addNode("queryNode", "nt:unstructured");
      rootNode.save();
      // make sure database is clean
      QueryManager manager = session.getWorkspace().getQueryManager();
      Query query = manager.createQuery(sqlQuery, Query.SQL);
      QueryResult queryResult = query.execute();
      NodeIterator iter = queryResult.getNodes();
      assertTrue(iter.getSize() == 0);

      Node testNode = queryNode.addNode("testGetReferences", "nt:unstructured");;
      Node n1 = queryNode.addNode("n1", "nt:unstructured");
      Node n2 = queryNode.addNode("n2", "nt:unstructured");
      queryNode.save();

      // before make reference
      queryResult = query.execute();
      iter = queryResult.getNodes();
      System.out.println("SIZE: " + iter.getSize());

      assertTrue(iter.getSize() == 3);

      // After make reference
      testNode.addMixin("mix:referenceable");
      n1.setProperty("p1", testNode);
      queryNode.save();

      // /////////
      Session session1 =
         repository.login(new SimpleCredentials("admin", "admin".toCharArray()), repository.getSystemWorkspaceName());
      manager = session1.getWorkspace().getQueryManager();
      query = manager.createQuery(sqlQuery, Query.SQL);
      // /////////

      queryResult = query.execute();
      iter = queryResult.getNodes();

      System.out.println(" " + iter.nextNode().getPath());

      assertEquals(3, iter.getSize());
      // clean database
      queryNode.remove();
      session.save();

   }
}
