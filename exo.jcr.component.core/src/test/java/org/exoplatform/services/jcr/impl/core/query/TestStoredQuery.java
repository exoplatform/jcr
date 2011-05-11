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

package org.exoplatform.services.jcr.impl.core.query;

import javax.jcr.Node;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

/**
 * Created by The eXo Platform SAS Author : Sergey Karpenko <sergey.karpenko@exoplatform.com.ua>
 * 
 * @version $Id: $
 */

public class TestStoredQuery extends BaseQueryTest
{

   public void setUp() throws Exception
   {
      super.setUp();
      root.addNode("vassya");
      root.save();
   }

   public void tearDown() throws Exception
   {
      root.getNode("vassya").remove();
      super.tearDown();
   }

   public void testSaveQueryAsNode() throws Exception
   {

      QueryManager qman = this.workspace.getQueryManager();
      Query q = qman.createQuery("SELECT * FROM nt:unstructured WHERE jcr:path LIKE '/vassya'", Query.SQL);

      QueryResult res = q.execute();

      assertEquals(1, res.getNodes().getSize());

      Node node = q.storeAsNode("/stored_query");
      this.root.save();
      assertNotNull(node);
   }

   public void testGetQueryFromNode() throws Exception
   {
      QueryManager qman = this.workspace.getQueryManager();
      Query q = qman.createQuery("SELECT * FROM nt:unstructured WHERE jcr:path LIKE '/vassya'", Query.SQL);

      QueryResult res = q.execute();

      assertEquals(1, res.getNodes().getSize());

      Node node = q.storeAsNode("/stored_query");
      this.root.save();
      assertNotNull(node);

      Query stor_query = qman.getQuery(node);
      assertNotNull(stor_query);

      res = stor_query.execute();
      assertEquals(1, res.getNodes().getSize());
   }

   public void testInvalidNodeQuery() throws Exception
   {
      try
      {
         Node node = root.addNode("invalid_node");
         root.save();
         QueryManager qman = this.workspace.getQueryManager();
         Query stor_query = qman.getQuery(node);
         fail();
      }
      catch (InvalidQueryException e)
      {
      }
   }

}
