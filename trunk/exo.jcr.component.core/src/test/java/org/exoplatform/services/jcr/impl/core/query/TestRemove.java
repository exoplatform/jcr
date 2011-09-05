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

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public class TestRemove extends BaseQueryTest
{
   /**
    * Class logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.TestRemove");

   public void testRemoveProperty() throws Exception
   {
      Node testNode = root.addNode("testNode");
      Property p = testNode.setProperty("p", "p");
      session.save();
      QueryManager qman = workspace.getQueryManager();
      Query q = qman.createQuery("SELECT * FROM nt:unstructured WHERE jcr:path LIKE '/testNode'", Query.SQL);

      QueryResult res = q.execute();

      NodeIterator nodes = res.getNodes();
      assertEquals(1, nodes.getSize());
      assertEquals(testNode, nodes.nextNode());
      p.remove();
      session.save();

      res = q.execute();
      nodes = res.getNodes();
      assertEquals(1, nodes.getSize());
      assertEquals(testNode, nodes.nextNode());

   }
}
