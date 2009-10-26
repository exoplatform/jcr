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

package org.exoplatform.services.jcr.impl.core.query.lucene;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

/**
 * Created by The eXo Platform SAS Author : Sergey Karpenko <sergey.karpenko@exoplatform.com.ua>
 * 
 * @version $Id: TestRemapping.java 11908 2008-03-13 16:00:12Z ksm $
 */

public class TestRemapping extends JcrImplBaseTest
{

   public static final Log logger = ExoLogger.getLogger(TestRemapping.class);

   public final String ORIGINAL_PREFIX = "test_remap";

   public final String NEW_PREFIX = "new_remap";

   public final String URI = "http://www.test_my.org/test_remap";

   public final String TEST_NAME = "test_name";

   public void setUp() throws Exception
   {
      super.setUp();

      boolean isOriginal = false;
      boolean isNew = false;

      String[] prefs = session.getWorkspace().getNamespaceRegistry().getPrefixes();
      for (int i = 0; i < prefs.length; i++)
      {
         if (prefs[i].equalsIgnoreCase(ORIGINAL_PREFIX))
            isOriginal = true;
         if (prefs[i].equalsIgnoreCase(NEW_PREFIX))
            isNew = true;
      }

      if (!isOriginal)
      {
         session.getWorkspace().getNamespaceRegistry().registerNamespace(ORIGINAL_PREFIX, URI);
         session.save();
      }
      // create test document
      root.addNode(ORIGINAL_PREFIX + ":" + TEST_NAME);
      root.save();

      // remapping
      if (!isNew)
      {
         session.setNamespacePrefix(NEW_PREFIX, URI);
         root.save();
      }
   }

   public void tearDown() throws Exception
   {
      Node node = (Node)session.getItem("/" + ORIGINAL_PREFIX + ":" + TEST_NAME);
      node.remove();
      session.save();

      super.tearDown();
   }

   public void testRemappingXPath() throws Exception
   {

      QueryManager qManager = session.getWorkspace().getQueryManager();

      QueryResult res = qManager.createQuery(ORIGINAL_PREFIX + ":" + TEST_NAME, Query.XPATH).execute();

      assertEquals(1, res.getNodes().getSize());

      res = qManager.createQuery(NEW_PREFIX + ":" + TEST_NAME, Query.XPATH).execute();
      assertEquals(1, res.getNodes().getSize());

      // Check hits

      NodeIterator nIt = res.getNodes();
      Node n = (Node)nIt.next();
      assertEquals(NEW_PREFIX + ":" + TEST_NAME, n.getName());

      // Search in other jcr - session
      Credentials cred = new CredentialsImpl("exo", "exo".toCharArray());
      Session sess = repository.login(cred, "ws");
      qManager = sess.getWorkspace().getQueryManager();
      res = qManager.createQuery(ORIGINAL_PREFIX + ":" + TEST_NAME, Query.XPATH).execute();

      assertEquals(1, res.getNodes().getSize());

      boolean itsOK = false;
      try
      {
         res = qManager.createQuery(NEW_PREFIX + ":" + TEST_NAME, Query.XPATH).execute();
      }
      catch (javax.jcr.query.InvalidQueryException e)
      {
         // OK
         itsOK = true;
      }
      assertTrue(itsOK);
   }

   public void testRemappingSQL() throws Exception
   {

      QueryManager qManager = session.getWorkspace().getQueryManager();

      QueryResult res =
         qManager
            .createQuery(
               "SELECT * FROM nt:unstructured WHERE jcr:path LIKE '/" + ORIGINAL_PREFIX + ":" + TEST_NAME + "'",
               Query.SQL).execute();

      assertEquals(1, res.getNodes().getSize());
      res =
         qManager.createQuery(
            "SELECT * FROM nt:unstructured WHERE jcr:path LIKE '/" + NEW_PREFIX + ":" + TEST_NAME + "'", Query.SQL)
            .execute();
      assertEquals(1, res.getNodes().getSize());

      // Check hits

      NodeIterator nIt = res.getNodes();
      Node n = (Node)nIt.next();
      assertEquals(NEW_PREFIX + ":" + TEST_NAME, n.getName());

      // Search in other jcr - session
      Credentials cred = new CredentialsImpl("exo", "exo".toCharArray());
      Session sess = repository.login(cred, "ws");
      qManager = sess.getWorkspace().getQueryManager();
      res =
         qManager
            .createQuery(
               "SELECT * FROM nt:unstructured WHERE jcr:path LIKE '/" + ORIGINAL_PREFIX + ":" + TEST_NAME + "'",
               Query.SQL).execute();

      assertEquals(1, res.getNodes().getSize());

      boolean itsOK = false;
      try
      {
         res =
            qManager.createQuery(
               "SELECT * FROM nt:unstructured WHERE jcr:path LIKE '/" + NEW_PREFIX + ":" + TEST_NAME + "'", Query.SQL)
               .execute();
      }
      catch (javax.jcr.query.InvalidQueryException e)
      {
         // OK
         itsOK = true;
      }
      assertTrue(itsOK);
   }

}
