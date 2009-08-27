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
package org.exoplatform.services.jcr.api.search;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.exoplatform.services.jcr.JcrAPIBaseTest;

/**
 * Created by The eXo Platform SAS.
 * 
 * Date: 17.04.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestSQLQuery.java 13418 2008-04-18 14:09:08Z pnedonosko $
 */
public class TestSQLQuery
   extends JcrAPIBaseTest
{

   private Node testRoot;

   private String prefixPath;

   /**
    * Create nodes structure.
    * 
    * <p>
    * /testSqlQuery/files
    * <p>
    * /testSqlQuery/files/draft
    * <p>
    * /testSqlQuery/files/draft/content1
    * <p>
    * /testSqlQuery/files/myFile1
    * <p>
    * /testSqlQuery/files/myFile1/jcr:content
    * <p>
    * /testSqlQuery/files/myFile1/jcr:content/myFile1
    * <p>
    * /testSqlQuery/files/myFile1/jcr:content/myFile1/jcr:content
    * <p>
    * /testSqlQuery/files/myFile2
    * <p>
    * /testSqlQuery/files/myFile2/jcr:content
    * <p>
    * /testSqlQuery/files/myFile2/jcr:content/myFile1
    * <p>
    * /testSqlQuery/files/myFile2/jcr:content/myFile1/jcr:content
    * 
    * <p>
    * /testSqlQuery/data
    * <p>
    * /testSqlQuery/data/draft
    * <p>
    * /testSqlQuery/data/draft/content1
    * <p>
    * /testSqlQuery/data/draft/content2
    * <p>
    * /testSqlQuery/data/myFile1
    * <p>
    * /testSqlQuery/data/myFile1/jcr:content
    * <p>
    * /testSqlQuery/data/myFile2
    * <p>
    * /testSqlQuery/data/myFile2/jcr:content
    * 
    */
   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      testRoot = root.addNode("testSqlQuery");
      root.save();

      prefixPath = testRoot.getPath();

      // files
      Node subnode = testRoot.addNode("files");

      Node sdata = subnode.addNode("draft");
      sdata.addNode("content1");

      Node file = subnode.addNode("myFile1", "nt:file");
      Node cfile = file.addNode("jcr:content", "nt:unstructured").addNode("myFile1", "nt:file");
      cfile.addNode("jcr:content", "nt:base");

      file = subnode.addNode("myFile2", "nt:file");
      cfile = file.addNode("jcr:content", "nt:unstructured").addNode("myFile1", "nt:file");
      cfile.addNode("jcr:content", "nt:base");

      // data
      subnode = testRoot.addNode("data");

      sdata = subnode.addNode("draft");
      sdata.addNode("content1");
      sdata.addNode("content2");

      Node data = subnode.addNode("myData1", "nt:file");
      data.addNode("jcr:content", "nt:unstructured");

      data = subnode.addNode("myData2", "nt:file");
      data.addNode("jcr:content", "nt:unstructured");

      testRoot.save();
   }

   @Override
   protected void tearDown() throws Exception
   {

      testRoot.remove();
      root.save();

      super.tearDown();
   }

   /**
    * Select all descendant nodes.
    * 
    * select * from nt:file where jcr:path like '/testSqlQuery/files/%'
    * 
    * @throws RepositoryException
    */
   public void testPathLike_Descendants() throws RepositoryException
   {

      try
      {
         Query q =
                  session.getWorkspace().getQueryManager().createQuery(
                           "select * from nt:file where jcr:path like '/testSqlQuery/files/%'", Query.SQL);

         QueryResult res = q.execute();
         assertEquals("Wrong nodes count in result set", 4, res.getNodes().getSize());
      }
      catch (InvalidQueryException e)
      {
         e.printStackTrace();
         fail("The query must be valid, but error found " + e);
      }
   }

   /**
    * Select all descendant nodes with part of path.
    * <p>
    * select * from nt:file where jcr:path like '%/files/%'
    * 
    * @throws RepositoryException
    */
   public void testPathLike_Descendants1() throws RepositoryException
   {

      try
      {
         Query q =
                  session.getWorkspace().getQueryManager().createQuery(
                           "select * from nt:file where jcr:path like '%/files/%'", Query.SQL);

         QueryResult res = q.execute();
         assertEquals("Wrong nodes count in result set", 4, res.getNodes().getSize());
      }
      catch (InvalidQueryException e)
      {
         e.printStackTrace();
         fail("The query must be valid, but error found " + e);
      }
   }

   /**
    * Select all descendant nodes with part of path from two locations in the workspace.
    * /testSqlQuery/files/draft /testSqlQuery/data/draft
    * 
    * <p>
    * select * from nt:unstructured where jcr:path like '%/draft/%'
    * 
    * @throws InvalidQueryException
    * @throws RepositoryException
    */
   public void testPathLike_FromDifferentLocations() throws InvalidQueryException, RepositoryException
   {

      Query q =
               session.getWorkspace().getQueryManager().createQuery(
                        "select * from nt:unstructured where jcr:path like '%/draft/%'", Query.SQL);

      QueryResult res = q.execute();
      assertEquals("Wrong nodes count in result set", 3, res.getNodes().getSize());

      for (NodeIterator nodes = res.getNodes(); nodes.hasNext();)
      {
         log.info(nodes.nextNode().getPath());
      }
   }

   /**
    * Select all descendant nodes from two locations in the workspace. /testSqlQuery/files/draft
    * /testSqlQuery/data/draft
    * 
    * <p>
    * select * from nt:unstructured where jcr:path like '/testSqlQuery/%/draft/%'
    * 
    * @throws InvalidQueryException
    * @throws RepositoryException
    */
   public void testPathLike_FromDifferentLocations1() throws InvalidQueryException, RepositoryException
   {

      Query q =
               session.getWorkspace().getQueryManager().createQuery(
                        "select * from nt:unstructured where jcr:path like '/testSqlQuery/%/draft/%'", Query.SQL);

      QueryResult res = q.execute();
      assertEquals("Wrong nodes count in result set", 3, res.getNodes().getSize());

      for (NodeIterator nodes = res.getNodes(); nodes.hasNext();)
      {
         log.info(nodes.nextNode().getPath());
      }
   }

   /**
    * Test query for child nodes only (without descendants).
    * <p>
    * select * from nt:file where jcr:path like '/testSqlQuery/files/%' and not jcr:path like
    * '/testSqlQuery/files/%/%'
    * 
    * @throws RepositoryException
    */
   public void testPathLike_ChildsOnly() throws RepositoryException
   {

      try
      {
         Query q =
                  session.getWorkspace().getQueryManager().createQuery(
                           "select * from nt:file where jcr:path like '/testSqlQuery/files/%' and not "
                                    + "jcr:path like '/testSqlQuery/files/%/%'", Query.SQL);

         QueryResult res = q.execute();
         assertEquals("Wrong nodes count in result set", 2, res.getNodes().getSize());
      }
      catch (InvalidQueryException e)
      {
         e.printStackTrace();
         fail("The query must be valid, but error found " + e);
      }
   }

   /**
    * Test if we can select nodes by path pattern and by exact path.
    * <p>
    * select * from nt:file where jcr:path like '/testSqlQuery/files/%/myFile1' or jcr:path =
    * '/testSqlQuery/files/myFile1'
    * 
    * @throws RepositoryException
    */
   public void testPathLike_DescendantsOrWithPath() throws RepositoryException
   {

      try
      {
         Query q =
                  session.getWorkspace().getQueryManager().createQuery(
                           "select * from nt:file where jcr:path like '/testSqlQuery/files/%/myFile1' or "
                                    + " jcr:path = '/testSqlQuery/files/myFile1'", Query.SQL);

         QueryResult res = q.execute();
         assertEquals("Wrong nodes count in result set", 3, res.getNodes().getSize());
      }
      catch (InvalidQueryException e)
      {
         e.printStackTrace();
         fail("The query must be valid, but error found " + e);
      }
   }

}
