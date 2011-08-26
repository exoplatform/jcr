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
 * Created by The eXo Platform SAS. Originally came from https://issues.jboss.org/browse/EXOJCR-1474
 * 
 * @author dongpd@exoplatform.com
 * @version $Id: $
 */
public class TestFuzzySearch extends JcrImplBaseTest
{
   private final Log log = ExoLogger.getLogger("exo.jcr.component.core.TestFuzzySearch");

   public void testFuzzySearch1() throws Exception
   {
      try
      {
         executeSQL("select * from nt:base where contains(.,'aaaaa~0.8')  and jcr:path like '/%'", 1);
      }
      catch (Exception e)
      {
         log.error(e);
         fail("Exception thrown.");
      }
   }

   public void testFuzzySearch2() throws Exception
   {
      try
      {
         executeSQL("select * from nt:base where contains(.,'user~0.8')  and jcr:path like '/%'", 0);
      }
      catch (Exception e)
      {
         log.error(e);
         fail("Exception thrown.");
      }
   }

   public void testFuzzySearch3() throws Exception
   {
      try
      {
         executeSQL("select * from nt:base where contains(.,'pharse~0.8') and jcr:path like '/%'", 0);
      }
      catch (Exception e)
      {
         log.error(e);
         fail("Exception thrown.");
      }
   }

   public void testFuzzySearch4() throws Exception
   {
      try
      {
         executeSQL("select * from nt:base where contains(.,'recherce~0.8') and jcr:path like '/%'", 0);
      }
      catch (Exception e)
      {
         log.error(e);
         fail("Exception thrown.");
      }
   }

   /**
    * Execute a Jcr SQL and show node names of resultset
    * 
    * @param param
    * @throws RepositoryException
    */
   private void executeSQL(String sql, int expectedCount) throws RepositoryException
   {
      // Build Jcr Query
      QueryManager qManager = session.getWorkspace().getQueryManager();
      Query query = qManager.createQuery(sql, Query.SQL);

      // Execute Jcr Query
      QueryResult result = query.execute();
      long totalCount = result.getNodes().getSize();
      assertEquals("Wrong result set size.", expectedCount, totalCount);
   }

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

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
   }

}