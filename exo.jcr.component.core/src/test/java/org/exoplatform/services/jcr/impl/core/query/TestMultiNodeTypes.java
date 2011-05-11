/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public class TestMultiNodeTypes extends BaseQueryTest
{
   /**
    * Class logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.TestMultiNodeTypes");

   public void testSelect2NtSQL() throws Exception
   {
      Node test1 = root.addNode("test1");
      test1.addMixin("mix:referenceable");
      test1.setProperty("p1", "1");
      Node test2 = root.addNode("test2");
      session.save();
      QueryResult res = null;
      try
      {
         QueryManager qman = this.workspace.getQueryManager();
         Query q = qman.createQuery("SELECT * FROM nt:unstructured, mix:referenceable where p1='1'", Query.SQL);

         res = q.execute();

      }
      catch (InvalidQueryException e)
      {
         fail();
      }
      assertNotNull(res);
      assertEquals(1, res.getNodes().getSize());
   }

   public void testSelect2PropertiesSQL() throws Exception
   {
      Node test1 = root.addNode("test1");
      test1.addMixin("mix:referenceable");
      test1.setProperty("p1", "1");
      Node test2 = root.addNode("test2");
      test2.setProperty("p1", "1");
      Node test3 = root.addNode("test3");
      test3.setProperty("p1", "2");

      session.save();
      String uuid = test1.getUUID();
      QueryResult res = null;
      try
      {
         QueryManager qman = this.workspace.getQueryManager();
         Query q =
            qman.createQuery("SELECT * FROM nt:base, mix:referenceable  where  p1='1' and jcr:uuid ='" + uuid + "'",
               Query.SQL);

         res = q.execute();

      }
      catch (InvalidQueryException e)
      {
         fail();
      }
      assertNotNull(res);
      NodeIterator nodes = res.getNodes();
      assertEquals(1, nodes.getSize());
      assertEquals(test1, nodes.nextNode());
   }

   public void testSelect2PropertiesXpath() throws Exception
   {
      Node test1 = root.addNode("test1");
      test1.setProperty("title1", "blah1");
      test1.setProperty("title2", "blabla1");
      Node test2 = root.addNode("test2");
      test2.setProperty("title1", "blah2");
      test2.setProperty("title2", "blabla2");
      Node test3 = root.addNode("test3");
      test3.setProperty("title1", "blah3");
      test3.setProperty("title2", "blabla3");

      session.save();

      QueryResult res = null;
      try
      {
         QueryManager qman = this.workspace.getQueryManager();
         Query q = qman.createQuery("//*[@title1= 'blah1' and @title2 = 'blabla1']", Query.XPATH);

         res = q.execute();

      }
      catch (InvalidQueryException e)
      {
         fail();
      }
      assertNotNull(res);
      NodeIterator nodes = res.getNodes();
      assertEquals(1, nodes.getSize());
      assertEquals(test1, nodes.nextNode());
   }
}
