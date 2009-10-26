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

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public class TestFuzzyQuery extends JcrImplBaseTest
{
   /**
    * Class logger.
    */
   private final Log log = ExoLogger.getLogger("jcr.TestFuzzyQuery");

   public void testFuzziness() throws Exception
   {
      Node testRoot = root.addNode("testRooot");
      Node testNode1 = testRoot.addNode("testNode1");
      testNode1.setProperty("field", "aaaaa");
      Node testNode2 = testRoot.addNode("testNode2");
      testNode2.setProperty("field", "aaaab");
      Node testNode3 = testRoot.addNode("testNode3");
      testNode3.setProperty("field", "aaabb");
      Node testNode4 = testRoot.addNode("testNode4");
      testNode4.setProperty("field", "aabbb");
      Node testNode5 = testRoot.addNode("testNode5");
      testNode5.setProperty("field", "abbbb");
      Node testNode6 = testRoot.addNode("testNode6");
      testNode6.setProperty("field", "bbbbb");
      Node testNode7 = testRoot.addNode("testNode7");
      testNode7.setProperty("field", "ddddd");
      root.save();

      assertEquals(3, executeSQLQuery("select * from nt:base where contains(field, 'aaaaa~')"));
      assertEquals(0, executeSQLQuery("select * from nt:base where contains(field, 'aaccc~')"));
      assertEquals(0, executeSQLQuery("select * from nt:base where contains(field, 'xxxxx~')"));
      assertEquals(3, executeSQLQuery("select * from nt:base where contains(field, 'aaaac~')"));
      assertEquals(1, executeSQLQuery("select * from nt:base where contains(field, 'ddddX~')"));

   }

   private long executeSQLQuery(String sql) throws RepositoryException
   {
      QueryManager qman = session.getWorkspace().getQueryManager();
      Query q = qman.createQuery(sql, Query.SQL);
      QueryResult res = q.execute();
      return res.getNodes().getSize();
   }
}
