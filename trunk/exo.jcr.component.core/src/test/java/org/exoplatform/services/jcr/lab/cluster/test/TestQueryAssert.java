/*
 * Copyright (C) 2009 eXo Platform SAS.
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
package org.exoplatform.services.jcr.lab.cluster.test;

import org.exoplatform.services.jcr.JcrAPIBaseTest;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

/**
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: TestQueryAssert.java 34360 2009-07-22 23:58:59Z nzamosenchuk $
 *
 */
public class TestQueryAssert extends JcrAPIBaseTest
{
   public void testAssert() throws RepositoryException, IOException
   {
      System.out.println("Start asserting....");

      Node doc1 = root.getNode("document1");
      Node doc2 = root.getNode("document2");

      // make SQL query
      QueryManager qman = this.workspace.getQueryManager();

      Query q = qman.createQuery("SELECT * FROM nt:file ", Query.SQL);
      QueryResult res = q.execute();
      long sqlsize = res.getNodes().getSize();
      assertEquals(2, sqlsize);
      checkResult(res, new Node[]{doc1, doc2});

      //make XPath query

      Query xq = qman.createQuery("//element(*,nt:file)", Query.XPATH);
      QueryResult xres = xq.execute();
      long xpathsize = xres.getNodes().getSize();
      assertEquals(2, xpathsize);
      checkResult(xres, new Node[]{doc1, doc2});
      System.out.println("Done!");
      try
      {
         Thread.sleep(60000);
      }
      catch (InterruptedException e)
      {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }

   protected void checkResult(QueryResult result, Node[] nodes) throws RepositoryException
   {
      // collect paths

      String[] columnNames = result.getColumnNames();
      String[][] vals = new String[(int)result.getNodes().getSize()][result.getColumnNames().length];

      RowIterator rit = result.getRows();
      int j = 0;
      while (rit.hasNext())
      {
         Row r = rit.nextRow();
         Value[] v = r.getValues();
         for (int i = 0; i < v.length; i++)
         {
            vals[j][i] = (v[i] != null) ? v[i].getString() : "null";
         }
         j++;
      }

      Set<String> expectedPaths = new HashSet<String>();
      for (int i = 0; i < nodes.length; i++)
      {
         expectedPaths.add(nodes[i].getPath());
      }
      Set<String> resultPaths = new HashSet<String>();
      for (NodeIterator it = result.getNodes(); it.hasNext();)
      {
         resultPaths.add(it.nextNode().getPath());
      }

      comparePaths(expectedPaths, resultPaths, false);
   }

   private void comparePaths(Set<String> expectedPaths, Set<String> resultPaths, boolean canContainMore)
   {
      // check if all expected are in result
      for (Iterator<String> it = expectedPaths.iterator(); it.hasNext();)
      {
         String path = it.next();
         assertTrue(path + " is not part of the result set", resultPaths.contains(path));
      }

      if (!canContainMore)
      {
         // check result does not contain more than expected

         for (Iterator<String> it = resultPaths.iterator(); it.hasNext();)
         {
            String path = it.next();
            assertTrue(path + " is not expected to be part of the result set. " + " Total size:" + resultPaths.size(),
               expectedPaths.contains(path));
         }
      }
   }

   @Override
   protected void tearDown() throws Exception
   {
      // do noting
   }
}
