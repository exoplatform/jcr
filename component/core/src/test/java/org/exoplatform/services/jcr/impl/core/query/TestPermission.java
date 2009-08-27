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

import java.util.HashMap;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.services.log.Log;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.query.lucene.TestRemapping;
import org.exoplatform.services.log.ExoLogger;

/**
 * Created by The eXo Platform SAS Author : Sergey Karpenko <sergey.karpenko@exoplatform.com.ua>
 * 
 * @version $Id: TestPermission.java 11908 2008-03-13 16:00:12Z ksm $
 */

public class TestPermission
   extends JcrImplBaseTest
{

   public static final Log logger = ExoLogger.getLogger(TestRemapping.class);

   public final String TEST_NAME = "test_name";

   public void setUp() throws Exception
   {
      super.setUp();
      // create test document
      NodeImpl node = (NodeImpl) root.addNode(TEST_NAME);
      node.addMixin("exo:privilegeable");

      HashMap<String, String[]> perm = new HashMap<String, String[]>();
      perm.put("admin", PermissionType.ALL);
      node.setPermissions(perm);

      root.save();
   }

   public void tearDown() throws Exception
   {
      Credentials cred = new CredentialsImpl("exo", "exo".toCharArray());
      Session sess = repository.login(cred, "ws");

      Node node = (Node) sess.getItem("/" + TEST_NAME);
      node.remove();
      sess.save();
      sess.logout();
      super.tearDown();
   }

   public void testPermissionXPATH() throws Exception
   {
      NodeImpl node = (NodeImpl) session.getItem("/" + TEST_NAME);

      QueryManager qManager = session.getWorkspace().getQueryManager();
      QueryResult res = qManager.createQuery(TEST_NAME, Query.XPATH).execute();
      assertEquals(1, getActualSize(res));

      // Search in other jcr - session
      Credentials cred = new CredentialsImpl("exo", "exo".toCharArray());
      Session sess = repository.login(cred, "ws");

      qManager = sess.getWorkspace().getQueryManager();
      res = qManager.createQuery(TEST_NAME, Query.XPATH).execute();
      assertEquals(0, getActualSize(res));

      // add permission for "exo" user
      HashMap<String, String[]> perm = new HashMap<String, String[]>();
      perm.put("exo", PermissionType.ALL);
      node.setPermissions(perm);
      root.save();

      // search in "admin" session XPATH
      qManager = session.getWorkspace().getQueryManager();
      res = qManager.createQuery(TEST_NAME, Query.XPATH).execute();
      assertEquals(0, getActualSize(res));

      // search in "exo" session XPATH
      qManager = sess.getWorkspace().getQueryManager();
      res = qManager.createQuery(TEST_NAME, Query.XPATH).execute();
      assertEquals(1, getActualSize(res));
   }

   public void testPermissionSQL() throws Exception
   {
      NodeImpl node = (NodeImpl) session.getItem("/" + TEST_NAME);

      QueryManager qManager = session.getWorkspace().getQueryManager();
      QueryResult res =
               qManager.createQuery(
                        "SELECT * FROM " + node.getPrimaryNodeType().getName() + " WHERE jcr:path LIKE '/" + TEST_NAME
                                 + "'", Query.SQL).execute();
      assertEquals(1, getActualSize(res));

      // Search in other jcr - session
      Credentials cred = new CredentialsImpl("exo", "exo".toCharArray());
      Session sess = repository.login(cred, "ws");

      qManager = sess.getWorkspace().getQueryManager();
      res =
               qManager.createQuery(
                        "SELECT * FROM " + node.getPrimaryNodeType().getName() + " WHERE jcr:path LIKE '/" + TEST_NAME
                                 + "'", Query.SQL).execute();
      assertEquals(0, getActualSize(res));

      // add permission for "exo" user
      HashMap<String, String[]> perm = new HashMap<String, String[]>();
      perm.put("exo", PermissionType.ALL);
      node.setPermissions(perm);
      root.save();

      // search in "admin" session SQL
      qManager = session.getWorkspace().getQueryManager();
      res =
               qManager.createQuery(
                        "SELECT * FROM " + node.getPrimaryNodeType().getName() + " WHERE jcr:path LIKE '/" + TEST_NAME
                                 + "'", Query.SQL).execute();
      assertEquals(0, getActualSize(res));

      // search in "exo" session SQL
      qManager = sess.getWorkspace().getQueryManager();
      res =
               qManager.createQuery(
                        "SELECT * FROM " + node.getPrimaryNodeType().getName() + " WHERE jcr:path LIKE '/" + TEST_NAME
                                 + "'", Query.SQL).execute();
      assertEquals(1, getActualSize(res));
   }

   private int getActualSize(QueryResult result) throws RepositoryException
   {
      int resultCount = 0;
      NodeIterator nodes = result.getNodes();
      while (nodes.hasNext())
      {
         nodes.nextNode();
         resultCount++;
      }
      return resultCount;
   }
}
