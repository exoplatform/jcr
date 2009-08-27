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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;

/**
 * Created by The eXo Platform SAS Author : Anh Nguyen ntuananh.vn@gmail.com Nov
 * 15, 2007
 */

public class TestI18NSQLQuery
   extends BaseUsecasesTest
{

   private static String[] input;

   static
   {
      input = new String[]
      {"ngocanh's o'clock"};
   }

   public void testI18NQueryPath() throws Exception
   {

      // Create node
      try
      {
         String content = input[0];
         root.addNode(content, "nt:unstructured");
         root.save();
      }
      catch (RepositoryException e)
      {
         // OK there must be exception - path is illegal

      }

      // String sqlQuery = "select * from nt:unstructured where jcr:path like
      // '/ngocanh''s o''clock' ";

   }

   public void testI18NQueryProperty() throws Exception
   {

      // We have problem with unicode chars, in Vietnamese or French, the result
      // alway empty

      // Create nodes
      String content = input[0];

      Node childNode = root.addNode("testNode", "nt:unstructured");
      childNode.setProperty("exo:testi18n", content);

      root.save();

      // Do Query by properties

      String sqlQuery = "select * from nt:unstructured where exo:testi18n like 'ngocanh''s o''clock' ";

      QueryManager manager = session.getWorkspace().getQueryManager();
      Query query = manager.createQuery(sqlQuery, Query.SQL);
      QueryResult queryResult = query.execute();

      NodeIterator iter = queryResult.getNodes();
      assertEquals(1, iter.getSize());
   }

}
