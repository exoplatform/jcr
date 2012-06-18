/*
 * Copyright (C) 2012 eXo Platform SAS.
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
package org.exoplatform.services.jcr.api.core.query;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

import javax.jcr.Node;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: IndexingExcludeRuleTest.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class IndexingExcludeRuleTest extends JcrAPIBaseTest
{

   public void testExcludeByPath() throws Exception
   {
      SessionImpl session = (SessionImpl)repository.login(credentials, "ws1");
      Node testRoot = session.getRootNode().addNode("testRoot");
      Node exoTrash = testRoot.addNode("exo:trash");
      Node exoTrash2 = testRoot.addNode("exo:trash2");
      exoTrash.addNode("node1");
      exoTrash.addNode("node2");
      exoTrash2.addNode("node1");
      exoTrash2.addNode("node2");

      session.save();

      QueryManager qman = session.getWorkspace().getQueryManager();

      Query q = qman.createQuery("SELECT * FROM nt:base WHERE jcr:path LIKE '/testRoot/%'", Query.SQL);
      assertEquals(5, q.execute().getNodes().getSize());

      testRoot.remove();
      session.save();
   }

   public void testExcludeByNodeType() throws Exception
   {
      SessionImpl session = (SessionImpl)repository.login(credentials, "ws1");
      Node testRoot = session.getRootNode().addNode("testRoot");
      Node node1 = testRoot.addNode("node1");
      Node node2 = testRoot.addNode("node2");
      Node node1node1 = node1.addNode("node1");
      Node node1node2 = node1.addNode("node2");
      node2.addNode("node1");
      node2.addNode("node2");

      session.save();

      QueryManager qman = session.getWorkspace().getQueryManager();

      Query q = qman.createQuery("SELECT * FROM nt:base WHERE jcr:path LIKE '/testRoot/%'", Query.SQL);
      assertEquals(6, q.execute().getNodes().getSize());

      q = qman.createQuery("SELECT * FROM exo:hiddenable", Query.SQL);
      assertEquals(0, q.execute().getNodes().getSize());

      q = qman.createQuery("SELECT * FROM exo:nothiddenable", Query.SQL);
      assertEquals(0, q.execute().getNodes().getSize());

      node1node1.addMixin("exo:hiddenable");
      node1node2.addMixin("exo:nothiddenable");

      session.save();

      q = qman.createQuery("SELECT * FROM nt:base WHERE jcr:path LIKE '/testRoot/%'", Query.SQL);
      assertEquals(5, q.execute().getNodes().getSize());

      q = qman.createQuery("SELECT * FROM exo:hiddenable", Query.SQL);
      assertEquals(0, q.execute().getNodes().getSize());

      q = qman.createQuery("SELECT * FROM exo:nothiddenable", Query.SQL);
      assertEquals(1, q.execute().getNodes().getSize());

      testRoot.remove();
      session.save();
   }

   public void testExcludeByNodeTypeAndPath() throws Exception
   {
      SessionImpl session = (SessionImpl)repository.login(credentials, "ws1");
      Node testRoot = session.getRootNode().addNode("testRoot");
      Node node1 = testRoot.addNode("node1");
      Node node2 = testRoot.addNode("node2");
      Node node1node1 = node1.addNode("node1");
      Node node1node2 = node1.addNode("node2");
      node2.addNode("node1");
      node2.addNode("node2");

      session.save();

      QueryManager qman = session.getWorkspace().getQueryManager();

      Query q = qman.createQuery("SELECT * FROM nt:base WHERE jcr:path LIKE '/testRoot/%'", Query.SQL);
      assertEquals(6, q.execute().getNodes().getSize());

      q = qman.createQuery("SELECT * FROM exo:hiddenable2", Query.SQL);
      assertEquals(0, q.execute().getNodes().getSize());

      node1node1.addMixin("exo:hiddenable2");
      node1node2.addMixin("exo:hiddenable2");

      session.save();

      q = qman.createQuery("SELECT * FROM nt:base WHERE jcr:path LIKE '/testRoot/%'", Query.SQL);
      assertEquals(5, q.execute().getNodes().getSize());

      q = qman.createQuery("SELECT * FROM exo:hiddenable2", Query.SQL);
      assertEquals(1, q.execute().getNodes().getSize());

      testRoot.remove();
      session.save();
   }
}