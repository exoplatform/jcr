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

import org.apache.lucene.search.ScoreDoc;
import org.exoplatform.services.jcr.impl.core.NodeImpl;

import javax.jcr.PropertyType;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

/**
 * Created by The eXo Platform SAS Author : Sergey Karpenko <sergey.karpenko@exoplatform.com.ua>
 */

public class TestMultiValueSearch extends BaseQueryTest
{

   public void testString() throws Exception
   {
      NodeImpl node = (NodeImpl)root.addNode("String", "nt:unstructured");
      node.setProperty("jcr:data", new String[]{"First", "Second"});
      root.save();

      // Check is node indexed
      ScoreDoc doc = getDocument(node.getInternalIdentifier(), false);
      assertNotNull("Node is not indexed", doc);

      QueryManager qman = this.workspace.getQueryManager();

      // Check first value
      Query q = qman.createQuery("SELECT * FROM nt:unstructured " + " WHERE jcr:data = 'First'", Query.SQL);
      QueryResult res = q.execute();
      assertEquals("First value isnt found.", 1, res.getNodes().getSize());

      // Check second value
      q = qman.createQuery("SELECT * FROM nt:unstructured " + " WHERE jcr:data = 'Second'", Query.SQL);
      res = q.execute();
      assertEquals("Second value isnt found.", 1, res.getNodes().getSize());
   }

   public void testBinary() throws Exception
   {
      NodeImpl node = (NodeImpl)root.addNode("Binary", "nt:unstructured");
      NodeImpl cont = (NodeImpl)node.addNode("jcr:content");
      cont.setProperty("jcr:mimeType", "text/plain");
      cont.setProperty("jcr:data", new String[]{"FirstB", "SecondB"}, PropertyType.BINARY);
      root.save();

      // Check is node indexed
      ScoreDoc doc = getDocument(node.getInternalIdentifier(), false);
      assertNotNull("Node is not indexed", doc);

      QueryManager qman = this.workspace.getQueryManager();

      // Check first value
      Query q = qman.createQuery("SELECT * FROM nt:unstructured " + " WHERE  CONTAINS(., 'FirstB')", Query.SQL);
      QueryResult res = q.execute();
      assertEquals("First value isnt found.", 1, res.getNodes().getSize());

      // Check second value
      q = qman.createQuery("SELECT * FROM nt:unstructured " + " WHERE  CONTAINS(., 'SecondB')", Query.SQL);
      res = q.execute();
      assertEquals("Second value isnt found.", 1, res.getNodes().getSize());
   }

}
