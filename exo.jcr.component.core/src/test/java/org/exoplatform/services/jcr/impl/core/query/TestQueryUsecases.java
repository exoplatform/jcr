/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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

import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TestQueryUsecases.java 111 2008-11-11 11:11:11Z serg $
 */
public class TestQueryUsecases extends BaseQueryTest
{

   private final Log log = ExoLogger.getLogger("exo.jcr.component.core.TestQueryUsecases");

   /**
    * Get all nodes from repository.
    * 
    * @throws Exception
    */
   public void testSimpleGetAll() throws Exception
   {
      root.addNode("simplenode", "nt:unstructured");

      Node doc1 = root.addNode("document1", "nt:file");
      NodeImpl cont = (NodeImpl)doc1.addNode("jcr:content", "nt:resource");
      cont.setProperty("jcr:mimeType", "text/plain");
      cont.setProperty("jcr:lastModified", Calendar.getInstance());
      cont.setProperty("jcr:encoding", "UTF-8");
      cont.setProperty("jcr:data", new ByteArrayInputStream("".getBytes()));

      Node doc2 = root.addNode("document2", "nt:file");
      cont = (NodeImpl)doc2.addNode("jcr:content", "nt:resource");
      cont.setProperty("jcr:mimeType", "text/plain");
      cont.setProperty("jcr:lastModified", Calendar.getInstance());
      cont.setProperty("jcr:encoding", "UTF-8");
      cont.setProperty("jcr:data", new ByteArrayInputStream("".getBytes()));

      session.save();

      // make SQL query
      QueryManager qman = this.workspace.getQueryManager();

      Query q = qman.createQuery("SELECT * FROM nt:base ", Query.SQL);
      QueryResult res = q.execute();
      long sqlsize = res.getNodes().getSize();
      assertTrue(3 < sqlsize);

      //make XPath query

      Query xq = qman.createQuery("//element(*,nt:base)", Query.XPATH);
      QueryResult xres = xq.execute();
      long xpathsize = xres.getNodes().getSize();
      assertTrue(3 < xpathsize);

      assertEquals(sqlsize, xpathsize);
   }

   /**
    * Get all files.
    * 
    * @throws Exception
    */
   public void testGetNodesByNodeType() throws Exception
   {
      root.addNode("simplenode", "nt:unstructured");

      Node doc1 = root.addNode("document1", "nt:file");
      NodeImpl cont = (NodeImpl)doc1.addNode("jcr:content", "nt:resource");
      cont.setProperty("jcr:mimeType", "text/plain");
      cont.setProperty("jcr:lastModified", Calendar.getInstance());
      cont.setProperty("jcr:encoding", "UTF-8");
      cont.setProperty("jcr:data", new ByteArrayInputStream("".getBytes()));

      Node doc2 = root.addNode("document2", "nt:file");
      cont = (NodeImpl)doc2.addNode("jcr:content", "nt:resource");
      cont.setProperty("jcr:mimeType", "text/plain");
      cont.setProperty("jcr:lastModified", Calendar.getInstance());
      cont.setProperty("jcr:encoding", "UTF-8");
      cont.setProperty("jcr:data", new ByteArrayInputStream("".getBytes()));

      session.save();

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
   }

   public void testGetNodesByMixinType() throws Exception
   {
      Node doc1 = root.addNode("document1", "nt:unstructured");
      doc1.addMixin("mix:title");
      doc1.setProperty("jcr:title", "First document");

      Node doc2 = root.addNode("document1", "nt:file");
      doc2.addMixin("mix:lockable");
      NodeImpl cont = (NodeImpl)doc2.addNode("jcr:content", "nt:resource");
      cont.setProperty("jcr:mimeType", "text/plain");
      cont.setProperty("jcr:lastModified", Calendar.getInstance());
      cont.setProperty("jcr:encoding", "UTF-8");
      cont.setProperty("jcr:data", new ByteArrayInputStream("".getBytes()));

      Node doc3 = root.addNode("document2", "nt:file");
      doc3.addMixin("mix:title");
      doc3.setProperty("jcr:title", "Second document");
      cont = (NodeImpl)doc3.addNode("jcr:content", "nt:resource");
      cont.setProperty("jcr:mimeType", "text/plain");
      cont.setProperty("jcr:lastModified", Calendar.getInstance());
      cont.setProperty("jcr:encoding", "UTF-8");
      cont.setProperty("jcr:data", new ByteArrayInputStream("".getBytes()));

      session.save();

      // make SQL query
      QueryManager qman = this.workspace.getQueryManager();

      Query q = qman.createQuery("SELECT * FROM mix:title ", Query.SQL);
      QueryResult res = q.execute();
      long sqlsize = res.getNodes().getSize();
      assertEquals(2, sqlsize);
      checkResult(res, new Node[]{doc1, doc3});

      //make XPath query

      Query xq = qman.createQuery("//element(*,mix:title)", Query.XPATH);
      QueryResult xres = xq.execute();
      long xpathsize = xres.getNodes().getSize();
      assertEquals(2, xpathsize);
      checkResult(xres, new Node[]{doc1, doc3});
   }

   /**
    * Get Collumns.
    * 
    * @throws Exception
    */
   public void testGetColumns() throws Exception
   {
      root.addNode("simplenode", "nt:unstructured");

      Node doc1 = root.addNode("document1", "nt:unstructured");
      doc1.addMixin("mix:title");
      doc1.setProperty("jcr:title", "Star wars");
      doc1.setProperty("jcr:description", "Dart rules!!");

      Node node2 = root.addNode("document2", "nt:file");
      node2.addMixin("mix:title");
      node2.setProperty("jcr:title", "Prison break");
      node2.setProperty("jcr:description", "Run, Forest, run ))");

      NodeImpl cont = (NodeImpl)node2.addNode("jcr:content", "nt:resource");
      cont.setProperty("jcr:mimeType", "text/plain");
      cont.setProperty("jcr:lastModified", Calendar.getInstance());
      cont.setProperty("jcr:data", "text");

      session.save();

      // make SQL query
      QueryManager qman = this.workspace.getQueryManager();

      Query q = qman.createQuery("SELECT jcr:title, jcr:description FROM mix:title ", Query.SQL);
      QueryResult res = q.execute();
      long sqlsize = res.getNodes().getSize();
      assertEquals(2, sqlsize);
      checkResult(res, new Node[]{doc1, node2});
      String[] expectedColumns = new String[]{"jcr:title", "jcr:description"};
      String[][] expectedRows = new String[][]{{"Star wars", "Dart rules!!"}, {"Prison break", "Run, Forest, run ))"}};
      checkColumns(res, expectedColumns, expectedRows, true);
      //make XPath query

      Query xq = qman.createQuery("//element(*,mix:title)/(@jcr:title | @jcr:description)", Query.XPATH);
      QueryResult xres = xq.execute();
      long xpathsize = xres.getNodes().getSize();
      assertEquals(2, xpathsize);
      checkResult(xres, new Node[]{doc1, node2});
      checkColumns(res, expectedColumns, expectedRows, true);
   }

   /**
    * Find all mix:title nodes with title 'Prison break'.
    * 
    * @throws Exception
    */
   public void testPropertyComparison() throws Exception
   {
      root.addNode("simplenode", "nt:unstructured");

      Node doc1 = root.addNode("document1", "nt:unstructured");
      doc1.addMixin("mix:title");
      doc1.setProperty("jcr:title", "War and peace");
      doc1.setProperty("jcr:description", "roman");
      doc1.setProperty("jcr:pagecount", 1000);

      Node doc2 = root.addNode("document2", "nt:file");
      doc2.addMixin("mix:title");
      doc2.setProperty("jcr:title", "Cinderella");
      doc2.setProperty("jcr:description", "fairytale");
      doc2.setProperty("jcr:pagecount", 100);

      NodeImpl cont = (NodeImpl)doc2.addNode("jcr:content", "nt:resource");
      cont.setProperty("jcr:mimeType", "text/plain");
      cont.setProperty("jcr:lastModified", Calendar.getInstance());
      cont.setProperty("jcr:data", "text");

      Node doc3 = root.addNode("document3", "nt:unstructured");
      doc3.addMixin("mix:title");
      doc3.setProperty("jcr:title", "Puss in Boots");
      doc3.setProperty("jcr:description", "fairytale");
      doc3.setProperty("jcr:pagecount", 60);

      session.save();

      // make SQL query
      QueryManager qman = this.workspace.getQueryManager();

      Query q = qman.createQuery("SELECT jcr:title FROM mix:title WHERE jcr:pagecount < 90", Query.SQL);
      QueryResult res = q.execute();
      long sqlsize = res.getNodes().getSize();
      assertEquals(1, sqlsize);
      checkResult(res, new Node[]{doc3});

      //make XPath query
      Query xq = qman.createQuery("//element(*,mix:title)[@jcr:pagecount < 90]/@jcr:title", Query.XPATH);
      QueryResult xres = xq.execute();
      long xpathsize = xres.getNodes().getSize();
      assertEquals(1, xpathsize);
      checkResult(xres, new Node[]{doc3});
   }

   public void testAndedConstraints() throws Exception
   {
      root.addNode("simplenode", "nt:unstructured");

      Node doc1 = root.addNode("document1", "nt:unstructured");
      doc1.addMixin("mix:title");
      doc1.setProperty("jcr:title", "War and peace");
      doc1.setProperty("jcr:description", "roman");
      doc1.setProperty("jcr:pagecount", 1000);

      Node doc2 = root.addNode("document2", "nt:file");
      doc2.addMixin("mix:title");
      doc2.setProperty("jcr:title", "Cinderella");
      doc2.setProperty("jcr:description", "fairytale");
      doc2.setProperty("jcr:pagecount", 100);

      NodeImpl cont = (NodeImpl)doc2.addNode("jcr:content", "nt:resource");
      cont.setProperty("jcr:mimeType", "text/plain");
      cont.setProperty("jcr:lastModified", Calendar.getInstance());
      cont.setProperty("jcr:data", "text");

      Node doc3 = root.addNode("document3", "nt:unstructured");
      doc3.addMixin("mix:title");
      doc3.setProperty("jcr:title", "Puss in Boots");
      doc3.setProperty("jcr:description", "fairytale");
      doc3.setProperty("jcr:pagecount", 60);

      session.save();

      // make SQL query
      QueryManager qman = this.workspace.getQueryManager();

      Query q =
         qman.createQuery("SELECT * FROM mix:title WHERE jcr:description = 'fairytale' AND jcr:pagecount > 90",
            Query.SQL);
      QueryResult res = q.execute();
      long sqlsize = res.getNodes().getSize();
      assertEquals(1, sqlsize);
      checkResult(res, new Node[]{doc2});

      //make XPath query
      Query xq =
         qman.createQuery("//element(*,mix:title)[@jcr:description='fairytale' and @jcr:pagecount > 90]", Query.XPATH);
      QueryResult xres = xq.execute();
      long xpathsize = xres.getNodes().getSize();
      assertEquals(1, xpathsize);
      checkResult(xres, new Node[]{doc2});
   }

   public void testORedConstraints() throws Exception
   {
      root.addNode("simplenode", "nt:unstructured");

      Node doc1 = root.addNode("document1", "nt:unstructured");
      doc1.addMixin("mix:title");
      doc1.setProperty("jcr:title", "War and peace");
      doc1.setProperty("jcr:description", "roman");
      doc1.setProperty("jcr:pagecount", 1000);

      Node doc2 = root.addNode("document2", "nt:file");
      doc2.addMixin("mix:title");
      doc2.setProperty("jcr:title", "Cinderella");
      doc2.setProperty("jcr:description", "fairytale");
      doc2.setProperty("jcr:pagecount", 100);

      NodeImpl cont = (NodeImpl)doc2.addNode("jcr:content", "nt:resource");
      cont.setProperty("jcr:mimeType", "text/plain");
      cont.setProperty("jcr:lastModified", Calendar.getInstance());
      cont.setProperty("jcr:data", "text");

      Node doc3 = root.addNode("document3", "nt:unstructured");
      doc3.addMixin("mix:title");
      doc3.setProperty("jcr:title", "Puss in Boots");
      doc3.setProperty("jcr:description", "fairytale");
      doc3.setProperty("jcr:pagecount", 60);

      session.save();

      // make SQL query
      QueryManager qman = this.workspace.getQueryManager();

      Query q =
         qman.createQuery("SELECT * FROM mix:title WHERE jcr:title = 'Cinderella' OR jcr:description = 'roman'",
            Query.SQL);
      QueryResult res = q.execute();
      long sqlsize = res.getNodes().getSize();
      assertEquals(2, sqlsize);
      checkResult(res, new Node[]{doc1, doc2});

      //make XPath query
      Query xq =
         qman.createQuery("//element(*,mix:title)[@jcr:title='Cinderella' or @jcr:description = 'roman']", Query.XPATH);
      QueryResult xres = xq.execute();
      long xpathsize = xres.getNodes().getSize();
      assertEquals(2, xpathsize);
      checkResult(xres, new Node[]{doc1, doc2});
   }

   /**
    * Find all mix:title nodes which title begins from 'P' symbol.
    * 
    * @throws Exception
    */
   public void testLIKEConstraint() throws Exception
   {
      root.addNode("simplenode", "nt:unstructured");

      Node doc1 = root.addNode("document1", "nt:unstructured");
      doc1.addMixin("mix:title");
      doc1.setProperty("jcr:title", "Star wars");
      doc1.setProperty("jcr:description", "Dart rules!!");

      Node doc2 = root.addNode("document2", "nt:file");
      doc2.addMixin("mix:title");
      doc2.setProperty("jcr:title", "Prison break");
      doc2.setProperty("jcr:description", "Run, Forest, run ))");

      NodeImpl cont = (NodeImpl)doc2.addNode("jcr:content", "nt:resource");
      cont.setProperty("jcr:mimeType", "text/plain");
      cont.setProperty("jcr:lastModified", Calendar.getInstance());
      cont.setProperty("jcr:data", "text");

      Node doc3 = root.addNode("document3", "nt:unstructured");
      doc3.addMixin("mix:title");
      doc3.setProperty("jcr:title", "Panopticum");
      doc3.setProperty("jcr:description", "It's imagine film )");

      session.save();

      // make SQL query
      QueryManager qman = this.workspace.getQueryManager();

      Query q = qman.createQuery("SELECT * FROM mix:title WHERE jcr:title LIKE 'P%'", Query.SQL);
      QueryResult res = q.execute();
      long sqlsize = res.getNodes().getSize();
      assertEquals(2, sqlsize);
      checkResult(res, new Node[]{doc2, doc3});

      //make XPath query
      Query xq = qman.createQuery("//element(*,mix:title)[jcr:like(@jcr:title, 'P%')]", Query.XPATH);
      QueryResult xres = xq.execute();
      long xpathsize = xres.getNodes().getSize();
      assertEquals(2, xpathsize);
      checkResult(xres, new Node[]{doc2, doc3});
   }

   public void testUPPERConstraint() throws Exception
   {
      root.addNode("simplenode", "nt:unstructured");

      Node doc1 = root.addNode("document1", "nt:unstructured");
      doc1.addMixin("mix:title");
      doc1.setProperty("jcr:title", "CaseSensitive");

      Node doc2 = root.addNode("document2", "nt:unstructured");
      doc2.addMixin("mix:title");
      doc2.setProperty("jcr:title", "casesensitive");

      Node doc3 = root.addNode("document3", "nt:unstructured");
      doc3.addMixin("mix:title");
      doc3.setProperty("jcr:title", "caseSENSITIVE");
      session.save();

      // make SQL query
      QueryManager qman = this.workspace.getQueryManager();

      Query q = qman.createQuery("SELECT * FROM mix:title WHERE UPPER(jcr:title) = 'CASESENSITIVE'", Query.SQL);
      QueryResult res = q.execute();
      long sqlsize = res.getNodes().getSize();
      assertEquals(3, sqlsize);
      checkResult(res, new Node[]{doc1, doc2, doc3});

      q = qman.createQuery("SELECT * FROM mix:title WHERE LOWER(jcr:title) = 'casesensitive'", Query.SQL);
      res = q.execute();
      sqlsize = res.getNodes().getSize();
      assertEquals(3, sqlsize);
      checkResult(res, new Node[]{doc1, doc2, doc3});

      //make XPath query
      Query xq = qman.createQuery("//element(*,mix:title)[fn:upper-case(@jcr:title)='CASESENSITIVE']", Query.XPATH);
      QueryResult xres = xq.execute();
      long xpathsize = xres.getNodes().getSize();
      assertEquals(3, xpathsize);
      checkResult(xres, new Node[]{doc1, doc2, doc3});

      xq = qman.createQuery("//element(*,mix:title)[fn:lower-case(@jcr:title)='casesensitive']", Query.XPATH);
      xres = xq.execute();
      xpathsize = xres.getNodes().getSize();
      assertEquals(3, xpathsize);
      checkResult(xres, new Node[]{doc1, doc2, doc3});
   }

   /**
    * Find all mix:title nodes which title begins from 'P' symbol.
    * 
    * @throws Exception
    */
   public void testLikeWithEscapeSymbol() throws Exception
   {
      root.addNode("simplenode", "nt:unstructured");

      Node doc1 = root.addNode("document1", "nt:unstructured");
      doc1.addMixin("mix:title");
      doc1.setProperty("jcr:title", "Porison break");//setProperty("jcr:title", "Star wars");
      doc1.setProperty("jcr:description", "Dart rules!!");

      Node doc2 = root.addNode("document2", "nt:file");
      doc2.addMixin("mix:title");
      doc2.setProperty("jcr:title", "P%rison break");
      doc2.setProperty("jcr:description", "Run, Forest, run ))");

      NodeImpl cont = (NodeImpl)doc2.addNode("jcr:content", "nt:resource");
      cont.setProperty("jcr:mimeType", "text/plain");
      cont.setProperty("jcr:lastModified", Calendar.getInstance());
      cont.setProperty("jcr:data", "text");

      Node doc3 = root.addNode("document3", "nt:unstructured");
      doc3.addMixin("mix:title");
      doc3.setProperty("jcr:title", "Panopticum");
      doc3.setProperty("jcr:description", "It's imagine film )");

      session.save();

      // make SQL query
      QueryManager qman = this.workspace.getQueryManager();

      Query q = qman.createQuery("SELECT * FROM mix:title WHERE jcr:title LIKE 'P#%ri%' ESCAPE '#'", Query.SQL);
      QueryResult res = q.execute();
      long sqlsize = res.getNodes().getSize();
      assertEquals(1, sqlsize);
      checkResult(res, new Node[]{doc2});

      //make XPath query
      Query xq = qman.createQuery("//element(*,mix:title)[jcr:like(@jcr:title, 'P\\%ri%')]", Query.XPATH);
      QueryResult xres = xq.execute();
      long xpathsize = xres.getNodes().getSize();
      assertEquals(1, xpathsize);
      checkResult(xres, new Node[]{doc2});
   }

   /**
    * Find all mix:title nodes which title not begins from 'P' symbol.
    * 
    * @throws Exception
    */
   public void testNotConstraint() throws Exception
   {
      root.addNode("simplenode", "nt:unstructured");

      Node doc1 = root.addNode("document1", "nt:unstructured");
      doc1.addMixin("mix:title");
      doc1.setProperty("jcr:title", "Star wars");
      doc1.setProperty("jcr:description", "Dart rules!!");

      Node doc2 = root.addNode("document2", "nt:file");
      doc2.addMixin("mix:title");
      doc2.setProperty("jcr:title", "Prison break");
      doc2.setProperty("jcr:description", "Run, Forest, run ))");

      NodeImpl cont = (NodeImpl)doc2.addNode("jcr:content", "nt:resource");
      cont.setProperty("jcr:mimeType", "text/plain");
      cont.setProperty("jcr:lastModified", Calendar.getInstance());
      cont.setProperty("jcr:data", "text");

      session.save();

      // make SQL query
      QueryManager qman = this.workspace.getQueryManager();

      Query q = qman.createQuery("SELECT * FROM mix:title WHERE NOT jcr:title LIKE 'P%'", Query.SQL);
      QueryResult res = q.execute();
      long sqlsize = res.getNodes().getSize();
      assertEquals(1, sqlsize);
      checkResult(res, new Node[]{doc1});

      //make XPath query
      Query xq = qman.createQuery("//element(*,mix:title)[not(jcr:like(@jcr:title, 'P%'))]", Query.XPATH);
      QueryResult xres = xq.execute();
      long xpathsize = xres.getNodes().getSize();
      assertEquals(1, xpathsize);
      checkResult(xres, new Node[]{doc1});
   }

   /**
    * Find all documents do not contains description.
    * 
    * @throws Exception
    */
   public void testPropertyNotExist() throws Exception
   {
      Node doc1 = root.addNode("document1", "nt:unstructured");
      doc1.addMixin("mix:title");
      doc1.setProperty("jcr:title", "Star wars");
      doc1.setProperty("jcr:description", "Dart rules!!");

      Node doc2 = root.addNode("document2", "nt:file");
      doc2.addMixin("mix:title");
      doc2.setProperty("jcr:title", "Prison break");
      doc2.setProperty("jcr:description", "Run, Forest, run ))");

      NodeImpl cont = (NodeImpl)doc2.addNode("jcr:content", "nt:resource");
      cont.setProperty("jcr:mimeType", "text/plain");
      cont.setProperty("jcr:lastModified", Calendar.getInstance());
      cont.setProperty("jcr:data", "text");

      Node doc3 = root.addNode("document1", "nt:unstructured");
      doc3.addMixin("mix:title");
      doc3.setProperty("jcr:title", "Titanic");

      session.save();

      // make SQL query
      QueryManager qman = this.workspace.getQueryManager();

      Query q = qman.createQuery("SELECT * FROM mix:title WHERE jcr:description IS NULL", Query.SQL);
      QueryResult res = q.execute();
      long sqlsize = res.getNodes().getSize();
      assertEquals(1, sqlsize);
      checkResult(res, new Node[]{doc3});

      //make XPath query
      Query xq = qman.createQuery("//element(*,mix:title)[not(@jcr:description)]", Query.XPATH);
      QueryResult xres = xq.execute();
      long xpathsize = xres.getNodes().getSize();
      assertEquals(1, xpathsize);
      checkResult(xres, new Node[]{doc3});
   }

   /**
    * Find all documents contains description.
    * 
    * @throws Exception
    */
   public void testPropertyExist() throws Exception
   {
      Node doc1 = root.addNode("document1", "nt:unstructured");
      doc1.addMixin("mix:title");
      doc1.setProperty("jcr:title", "Star wars");
      doc1.setProperty("jcr:description", "Dart rules!!");

      Node doc2 = root.addNode("document2", "nt:file");
      doc2.addMixin("mix:title");
      doc2.setProperty("jcr:title", "Prison break");
      doc2.setProperty("jcr:description", "Run, Forest, run ))");

      NodeImpl cont = (NodeImpl)doc2.addNode("jcr:content", "nt:resource");
      cont.setProperty("jcr:mimeType", "text/plain");
      cont.setProperty("jcr:lastModified", Calendar.getInstance());
      cont.setProperty("jcr:data", "text");

      Node doc3 = root.addNode("document3", "nt:unstructured");
      doc3.addMixin("mix:title");
      doc3.setProperty("jcr:title", "Titanic");

      session.save();

      // make SQL query
      QueryManager qman = this.workspace.getQueryManager();

      Query q = qman.createQuery("SELECT * FROM mix:title WHERE jcr:description IS NOT NULL", Query.SQL);
      QueryResult res = q.execute();
      long sqlsize = res.getNodes().getSize();
      assertEquals(2, sqlsize);
      checkResult(res, new Node[]{doc1, doc2});

      //make XPath query
      Query xq = qman.createQuery("//element(*,mix:title)[@jcr:description]", Query.XPATH);
      QueryResult xres = xq.execute();
      long xpathsize = xres.getNodes().getSize();
      assertEquals(2, xpathsize);
      checkResult(xres, new Node[]{doc1, doc2});
   }

   /**
    * Find all documents contains description.
    * 
    * @throws Exception
    */
   public void testFulltextAllNodes() throws Exception
   {
      Node doc1 = root.addNode("document1", "nt:file");
      NodeImpl cont1 = (NodeImpl)doc1.addNode("jcr:content", "nt:resource");
      cont1.setProperty("jcr:mimeType", "text/plain");
      cont1.setProperty("jcr:lastModified", Calendar.getInstance());
      cont1.setProperty("jcr:data", "The quick brown fox jump over the lazy dog");
      session.save();

      Node doc2 = root.addNode("document2", "nt:file");
      NodeImpl cont2 = (NodeImpl)doc2.addNode("jcr:content", "nt:resource");
      cont2.setProperty("jcr:mimeType", "text/plain");
      cont2.setProperty("jcr:lastModified", Calendar.getInstance());
      cont2.setProperty("jcr:data", "Dogs do not like cats.");

      Node doc3 = root.addNode("document3", "nt:file");
      NodeImpl cont3 = (NodeImpl)doc3.addNode("jcr:content", "nt:resource");
      cont3.setProperty("jcr:mimeType", "text/plain");
      cont3.setProperty("jcr:lastModified", Calendar.getInstance());
      cont3.setProperty("jcr:data", "Cats jumping high.");
      session.save();

      // make SQL query
      QueryManager qman = this.workspace.getQueryManager();

      Query q = qman.createQuery("SELECT * FROM nt:resource WHERE CONTAINS(*,'do')", Query.SQL);
      QueryResult res = q.execute();
      long sqlsize = res.getNodes().getSize();
      assertEquals(1, sqlsize);
      checkResult(res, new Node[]{cont2});

      //make XPath query
      Query xq = qman.createQuery("//element(*,nt:resource)[jcr:contains(.,'cats')]", Query.XPATH);
      QueryResult xres = xq.execute();
      long xpathsize = xres.getNodes().getSize();
      assertEquals(2, xpathsize);
      checkResult(xres, new Node[]{cont2, cont3});
   }

   public void testFulltextAllNodes2() throws Exception
   {
      Node doc1 = root.addNode("document1", "nt:unstructured");
      doc1.addMixin("mix:title");
      doc1.setProperty("jcr:title", "Star wars");
      doc1.setProperty("jcr:description", "Dart rules!!");

      Node doc2 = root.addNode("document2", "nt:file");
      doc2.addMixin("mix:title");
      doc2.setProperty("jcr:title", "Prison break.");
      doc2.setProperty("jcr:description", "Run, Forest, run ))");

      NodeImpl cont = (NodeImpl)doc2.addNode("jcr:content", "nt:resource");
      cont.setProperty("jcr:mimeType", "text/plain");
      cont.setProperty("jcr:lastModified", Calendar.getInstance());
      cont.setProperty("jcr:data", "text");

      Node doc3 = root.addNode("document3", "nt:unstructured");
      doc3.addMixin("mix:title");
      doc3.setProperty("jcr:title", "Titanic");
      doc3.setProperty("jcr:description", "The aisberg break ship.");
      session.save();

      // make SQL query
      QueryManager qman = this.workspace.getQueryManager();

      //TODO can't make working query
      Query q = qman.createQuery("SELECT * FROM mix:title WHERE CONTAINS(*,'break')", Query.SQL);
      QueryResult res = q.execute();
      long sqlsize = res.getNodes().getSize();
      assertEquals(2, sqlsize);
      checkResult(res, new Node[]{doc2, doc3});

      //make XPath query
      Query xq = qman.createQuery("//element(*,mix:title)[jcr:contains(.,'break')]", Query.XPATH);
      QueryResult xres = xq.execute();
      long xpathsize = xres.getNodes().getSize();
      assertEquals(2, xpathsize);
      checkResult(xres, new Node[]{doc2, doc3});
   }

   public void testFulltextByProperty() throws Exception
   {
      root.addNode("simplenode", "nt:unstructured");

      Node doc1 = root.addNode("document1", "nt:unstructured");
      doc1.addMixin("mix:title");
      doc1.setProperty("jcr:title", "Star wars");
      doc1.setProperty("jcr:description", "Quick brown fox jumps over the lazy dog.");

      Node doc2 = root.addNode("document2", "nt:file");
      doc2.addMixin("mix:title");
      doc2.setProperty("jcr:title", "Prison break");
      doc2.setProperty("jcr:description", "Brown fox live in forest.");

      NodeImpl cont = (NodeImpl)doc2.addNode("jcr:content", "nt:resource");
      cont.setProperty("jcr:mimeType", "text/plain");
      cont.setProperty("jcr:lastModified", Calendar.getInstance());
      cont.setProperty("jcr:data", "text");

      Node doc3 = root.addNode("document3", "nt:unstructured");
      doc3.addMixin("mix:title");
      doc3.setProperty("jcr:title", "Star wars");
      doc3.setProperty("jcr:description", "fox is a nice animal.");

      Node doc4 = root.addNode("document4", "nt:unstructured");
      doc4.setProperty("jcr:title", "Star wars");
      doc4.setProperty("jcr:description", "There is forest word too.");

      session.save();

      // make SQL query
      QueryManager qman = this.workspace.getQueryManager();

      Query q = qman.createQuery("SELECT * FROM mix:title WHERE CONTAINS(jcr:description, 'forest')", Query.SQL);
      QueryResult res = q.execute();
      long sqlsize = res.getNodes().getSize();
      assertEquals(1, sqlsize);
      checkResult(res, new Node[]{doc2});
      // Check order

      //make XPath query

      Query xq = qman.createQuery("//element(*,mix:title)[jcr:contains(@jcr:description, 'forest')]", Query.XPATH);
      QueryResult xres = xq.execute();
      long xpathsize = xres.getNodes().getSize();
      assertEquals(1, xpathsize);
      checkResult(xres, new Node[]{doc2});

   }

   public void testExactPath() throws Exception
   {
      Node r1 = root.addNode("root1");
      Node r2 = r1.addNode("root2");
      Node node1 = r2.addNode("node1", "nt:unstructured");
      Node node1_2 = r2.addNode("node1", "nt:unstructured");
      Node node2 = r2.addNode("node2", "nt:unstructured");
      session.save();

      // make SQL query
      QueryManager qman = this.workspace.getQueryManager();

      Query q = qman.createQuery("SELECT * FROM nt:unstructured  WHERE jcr:path = '/root1/root2/node1[1]'", Query.SQL);
      QueryResult res = q.execute();
      long sqlsize = res.getNodes().getSize();
      assertEquals(1, sqlsize);
      checkResult(res, new Node[]{node1});

      //make XPath query
      Query xq = qman.createQuery("/jcr:root/root1[1]/root2[1]/element(node1,nt:unstructured)[1]", Query.XPATH);
      QueryResult xres = xq.execute();
      long xpathsize = xres.getNodes().getSize();
      assertEquals(1, xpathsize);
      checkResult(xres, new Node[]{node1});

   }

   public void testFindAllSameName() throws Exception
   {
      Node r1 = root.addNode("root1");
      Node r2 = r1.addNode("root2");
      Node node1 = r2.addNode("node1", "nt:unstructured");
      Node node1_2 = r2.addNode("node1", "nt:unstructured");
      Node node2 = r2.addNode("node2", "nt:unstructured");
      session.save();

      // make SQL query
      QueryManager qman = this.workspace.getQueryManager();

      Query q =
         qman.createQuery("SELECT * FROM nt:unstructured  WHERE jcr:path LIKE '/root1/root2/node1[%]'", Query.SQL);
      QueryResult res = q.execute();
      long sqlsize = res.getNodes().getSize();
      assertEquals(2, sqlsize);
      checkResult(res, new Node[]{node1, node1_2});

      //make XPath query
      Query xq = qman.createQuery("/jcr:root/root1[1]/root2[1]/element(node1,nt:unstructured)", Query.XPATH);
      QueryResult xres = xq.execute();
      long xpathsize = xres.getNodes().getSize();
      assertEquals(2, xpathsize);
      checkResult(xres, new Node[]{node1, node1_2});
   }

   public void testChildNodes() throws Exception
   {
      Node r1 = root.addNode("root1", "nt:folder");
      Node r2 = r1.addNode("root2", "nt:folder");
      Node subdir1 = r2.addNode("subdir1", "nt:folder");
      Node node1 = subdir1.addNode("node1", "nt:folder");
      Node node2 = r2.addNode("node2", "nt:folder");
      session.save();

      // make SQL query
      QueryManager qman = this.workspace.getQueryManager();

      Query q =
         qman.createQuery(
            "SELECT * FROM nt:folder WHERE jcr:path LIKE '/root1/root2/%' AND NOT jcr:path LIKE '/root1/root2/%/%'",
            Query.SQL);
      QueryResult res = q.execute();
      long sqlsize = res.getNodes().getSize();
      assertEquals(2, sqlsize);
      checkResult(res, new Node[]{subdir1, node2});

      //make XPath query
      Query xq = qman.createQuery("/jcr:root/root1[1]/root2[1]/element(*,nt:folder)", Query.XPATH);
      QueryResult xres = xq.execute();
      long xpathsize = xres.getNodes().getSize();
      assertEquals(2, xpathsize);
      checkResult(xres, new Node[]{subdir1, node2});
   }

   public void testDescendantNodes() throws Exception
   {
      Node r1 = root.addNode("root1", "nt:folder");
      Node r2 = r1.addNode("root2", "nt:folder");
      Node subdir1 = r2.addNode("subdir1", "nt:folder");
      Node node1 = subdir1.addNode("node1", "nt:folder");
      Node node2 = r2.addNode("node2", "nt:folder");
      session.save();

      // make SQL query
      QueryManager qman = this.workspace.getQueryManager();

      Query q = qman.createQuery("SELECT * FROM nt:folder WHERE jcr:path LIKE '/root1/root2/%'", Query.SQL);
      QueryResult res = q.execute();
      long sqlsize = res.getNodes().getSize();
      assertEquals(3, sqlsize);
      checkResult(res, new Node[]{subdir1, node1, node2});

      //make XPath query
      Query xq = qman.createQuery("/jcr:root/root1[1]/root2[1]//element(*,nt:folder)", Query.XPATH);
      QueryResult xres = xq.execute();
      long xpathsize = xres.getNodes().getSize();
      assertEquals(3, xpathsize);
      checkResult(xres, new Node[]{subdir1, node1, node2});
   }

   /***
    * TODO seems working incorrect
    * 
    * @throws Exception
    */
   public void testDescendantOrSelf() throws Exception
   {
      Node r1 = root.addNode("root1", "nt:folder");
      Node r2 = r1.addNode("root2", "nt:folder");
      Node subdir1 = r2.addNode("subdir1", "nt:folder");
      Node node1 = subdir1.addNode("node1", "nt:folder");
      Node node2 = r2.addNode("node2", "nt:folder");
      session.save();

      // make SQL query
      QueryManager qman = this.workspace.getQueryManager();

      Query q =
         qman.createQuery("SELECT * FROM nt:folder WHERE jcr:path = '/root1/root2' OR jcr:path LIKE '/root1/root2/%'",
            Query.SQL);
      QueryResult res = q.execute();
      long sqlsize = res.getNodes().getSize();
      assertEquals(4, sqlsize);
      checkResult(res, new Node[]{r2, subdir1, node1, node2});

      //make XPath query
      Query xq = qman.createQuery("/jcr:root/root1[1]/root2[1]//element(*,nt:folder)", Query.XPATH);
      QueryResult xres = xq.execute();
      long xpathsize = xres.getNodes().getSize();
      assertEquals(3, xpathsize);
      checkResult(xres, new Node[]{subdir1, node1, node2});
   }

   public void testGetAllColumns() throws Exception
   {
      root.addNode("simplenode", "nt:unstructured");

      Node doc1 = root.addNode("document1", "nt:unstructured");
      doc1.addMixin("mix:title");
      doc1.setProperty("jcr:title", "Star wars");
      doc1.setProperty("jcr:description", "Dart rules!!");

      Node doc2 = root.addNode("document2", "nt:file");
      doc2.addMixin("mix:title");
      doc2.setProperty("jcr:title", "Prison break");
      doc2.setProperty("jcr:description", "Run, Forest, run ))");

      NodeImpl cont = (NodeImpl)doc2.addNode("jcr:content", "nt:resource");
      cont.setProperty("jcr:mimeType", "text/plain");
      cont.setProperty("jcr:lastModified", Calendar.getInstance());
      cont.setProperty("jcr:data", "text");

      Node doc3 = root.addNode("document3", "nt:unstructured");
      doc3.addMixin("mix:title");
      doc3.setProperty("jcr:title", "Panopticum");
      doc3.setProperty("jcr:description", "It's imagine film )");

      session.save();

      // make SQL query
      QueryManager qman = this.workspace.getQueryManager();

      Query q = qman.createQuery("SELECT * FROM mix:title", Query.SQL);
      QueryResult res = q.execute();
      long sqlsize = res.getNodes().getSize();
      assertEquals(3, sqlsize);
      checkResult(res, new Node[]{doc1, doc2, doc3});
      String[] expectedColumns = new String[]{"jcr:title", "jcr:description", "jcr:pagecount", "jcr:path"};
      String[][] expectedRows =
         new String[][]{{"Star wars", "Dart rules!!", null, "/document1"},
            {"Prison break", "Run, Forest, run ))", null, "/document2"},
            {"Panopticum", "It's imagine film )", null, "/document3"}};
      checkColumns(res, expectedColumns, expectedRows, true);

      //make XPath query
      Query xq = qman.createQuery("//element(*,mix:title)", Query.XPATH);
      QueryResult xres = xq.execute();
      long xpathsize = xres.getNodes().getSize();
      assertEquals(3, xpathsize);
      checkResult(xres, new Node[]{doc1, doc2, doc3});
   }

   public void testOrderByScore() throws Exception
   {
      root.addNode("simplenode", "nt:unstructured");

      Node doc1 = root.addNode("document1", "nt:unstructured");
      doc1.addMixin("mix:title");
      doc1.setProperty("jcr:title", "Star wars");
      doc1.setProperty("jcr:description", "Quick brown fox jumps over the lazy dog.");

      Node doc2 = root.addNode("document2", "nt:file");
      doc2.addMixin("mix:title");
      doc2.setProperty("jcr:title", "Prison break");
      doc2.setProperty("jcr:description", "Brown fox live in forest.");

      NodeImpl cont = (NodeImpl)doc2.addNode("jcr:content", "nt:resource");
      cont.setProperty("jcr:mimeType", "text/plain");
      cont.setProperty("jcr:lastModified", Calendar.getInstance());
      cont.setProperty("jcr:data", "text");

      Node doc3 = root.addNode("document3", "nt:unstructured");
      doc3.addMixin("mix:title");
      doc3.setProperty("jcr:title", "Star wars");
      doc3.setProperty("jcr:description", "fox is a nice animal.");

      session.save();

      // make SQL query
      QueryManager qman = this.workspace.getQueryManager();

      Query q =
         qman.createQuery(
            "SELECT * FROM mix:title WHERE CONTAINS(*, 'brown OR fox OR jumps') ORDER BY jcr:score() ASC", Query.SQL);
      QueryResult res = q.execute();
      long sqlsize = res.getNodes().getSize();
      assertEquals(3, sqlsize);
      checkOrder(res, new Node[]{doc3, doc2, doc1});

      //make XPath query
      Query xq =
         qman.createQuery("//element(*,mix:title)[jcr:contains(., 'brown OR fox OR jumps')] order by jcr:score()",
            Query.XPATH);
      QueryResult xres = xq.execute();
      long xpathsize = xres.getNodes().getSize();
      assertEquals(3, xpathsize);
      checkOrder(res, new Node[]{doc3, doc2, doc1});
   }

   public void testOrderByLongDesc() throws Exception
   {
      root.addNode("simplenode", "nt:unstructured");

      Node doc1 = root.addNode("document1", "nt:unstructured");
      doc1.addMixin("mix:title");
      doc1.setProperty("jcr:title", "Star wars");
      doc1.setProperty("jcr:description", "Quick brown fox jumps over the lazy dog.");
      doc1.setProperty("jcr:pagecount", 4);

      Node doc2 = root.addNode("document2", "nt:unstructured");
      doc2.addMixin("mix:title");
      doc2.setProperty("jcr:title", "Prison break");
      doc2.setProperty("jcr:description", "Brown fox live in forest.");
      doc2.setProperty("jcr:pagecount", 7);

      Node doc3 = root.addNode("document3", "nt:unstructured");
      doc3.addMixin("mix:title");
      doc3.setProperty("jcr:title", "Star wars");
      doc3.setProperty("jcr:description", "fox is a nice animal.");
      doc3.setProperty("jcr:pagecount", 1);

      session.save();

      QueryManager qman = this.workspace.getQueryManager();
      //make XPath query
      Query xq = qman.createQuery("//element(*,mix:title) order by jcr:pagecount descending", Query.XPATH);
      QueryResult xres = xq.execute();
      long xpathsize = xres.getNodes().getSize();
      assertEquals(3, xpathsize);
      checkResult(xres, new Node[]{doc1, doc2, doc3});
   }

   public void testOrderByProperty() throws Exception
   {
      root.addNode("simplenode", "nt:unstructured");

      Node doc1 = root.addNode("document1", "nt:unstructured");
      doc1.addMixin("mix:title");
      doc1.setProperty("jcr:title", "Star wars");
      doc1.setProperty("jcr:description", "Quick brown fox jumps over the lazy dog.");
      doc1.setProperty("jcr:pagecount", 4);

      Node doc2 = root.addNode("document2", "nt:unstructured");
      doc2.addMixin("mix:title");
      doc2.setProperty("jcr:title", "Prison break");
      doc2.setProperty("jcr:description", "Brown fox live in forest.");
      doc2.setProperty("jcr:pagecount", 7);

      Node doc3 = root.addNode("document3", "nt:unstructured");
      doc3.addMixin("mix:title");
      doc3.setProperty("jcr:title", "Star wars");
      doc3.setProperty("jcr:description", "fox is a nice animal.");
      doc3.setProperty("jcr:pagecount", 1);

      session.save();

      // make SQL query
      QueryManager qman = this.workspace.getQueryManager();

      Query q = qman.createQuery("SELECT * FROM mix:title ORDER BY jcr:pagecount ASC", Query.SQL);
      QueryResult res = q.execute();
      long sqlsize = res.getNodes().getSize();
      assertEquals(3, sqlsize);
      checkResult(res, new Node[]{doc1, doc2, doc3});
      checkOrder(res, new Node[]{doc3, doc1, doc2});

      //make XPath query

      Query xq = qman.createQuery("//element(*,mix:title) order by jcr:pagecount ascending", Query.XPATH);
      QueryResult xres = xq.execute();
      long xpathsize = xres.getNodes().getSize();
      assertEquals(3, xpathsize);
      //checkResult(xres, new Node[]{doc1, doc2, doc3});
      checkOrder(res, new Node[]{doc3, doc1, doc2});
   }

   public void testSearchByName() throws Exception
   {
      Node doc1 = root.addNode("document1", "nt:file");
      NodeImpl cont1 = (NodeImpl)doc1.addNode("jcr:content", "nt:resource");
      cont1.setProperty("jcr:mimeType", "text/plain");
      cont1.setProperty("jcr:lastModified", Calendar.getInstance());
      cont1.setProperty("jcr:data", "The quick brown fox jump over the lazy dog");
      session.save();

      Node doc2 = root.addNode("document2", "nt:file");
      NodeImpl cont2 = (NodeImpl)doc2.addNode("jcr:content", "nt:resource");
      cont2.setProperty("jcr:mimeType", "text/plain");
      cont2.setProperty("jcr:lastModified", Calendar.getInstance());
      cont2.setProperty("jcr:data", "Dogs do not like cats.");

      Node doc3 = root.addNode("document1", "nt:file");
      NodeImpl cont3 = (NodeImpl)doc3.addNode("jcr:content", "nt:resource");
      cont3.setProperty("jcr:mimeType", "text/plain");
      cont3.setProperty("jcr:lastModified", Calendar.getInstance());
      cont3.setProperty("jcr:data", "Cats jumping high.");
      session.save();

      // make SQL query
      QueryManager qman = this.workspace.getQueryManager();

      Query q = qman.createQuery("SELECT * FROM nt:file WHERE fn:name() = 'document1'", Query.SQL);
      QueryResult res = q.execute();
      long sqlsize = res.getNodes().getSize();
      assertEquals(2, sqlsize);
      checkResult(res, new Node[]{doc1, doc3});

      //make XPath query
      Query xq = qman.createQuery("//element(*,nt:file)[fn:name() = 'document1']", Query.XPATH);
      QueryResult xres = xq.execute();
      long xpathsize = xres.getNodes().getSize();
      assertEquals(2, xpathsize);
      checkResult(xres, new Node[]{doc1, doc3});
   }

   public void testMultivalueProperty() throws Exception
   {
      Node doc1 = root.addNode("node1", "nt:unstructured");
      doc1.setProperty("multiprop", new String[]{"one", "two"});

      Node doc2 = root.addNode("node2", "nt:unstructured");
      doc2.setProperty("multiprop", new String[]{"one", "two", "three"});

      Node doc3 = root.addNode("node3", "nt:unstructured");
      doc3.setProperty("multiprop", new String[]{"one", "five"});
      session.save();

      // make SQL query
      QueryManager qman = this.workspace.getQueryManager();

      Query q =
         qman.createQuery("SELECT * FROM nt:unstructured WHERE multiprop = 'one' AND multiprop = 'two'", Query.SQL);
      QueryResult res = q.execute();
      long sqlsize = res.getNodes().getSize();
      assertEquals(2, sqlsize);
      checkResult(res, new Node[]{doc1, doc2});

      //make XPath query
      Query xq =
         qman.createQuery("//element(*,nt:unstructured)[@multiprop = 'one' and @multiprop = 'two']", Query.XPATH);
      QueryResult xres = xq.execute();
      long xpathsize = xres.getNodes().getSize();
      assertEquals(2, xpathsize);
      checkResult(xres, new Node[]{doc1, doc2});

   }

   /**
    * Checks if the result set contains exactly the <code>nodes</code>.
    * 
    * @param result the query result.
    * @param nodes the expected nodes in the result set.
    */
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

   private void checkOrder(QueryResult res, Node[] expectedNodes) throws RepositoryException
   {
      NodeIterator ni = res.getNodes();

      List<String> list = new ArrayList<String>();

      while (ni.hasNext())
      {
         list.add(ni.nextNode().getPath());
      }

      ni = res.getNodes();
      for (int i = 0; i < expectedNodes.length; i++)
      {
         Node expNode = expectedNodes[i];
         if (!ni.hasNext())
         {
            fail("Result do not contain node " + expNode.getName());
         }
         assertEquals("Node not found or not in expected order " + expNode.getPath(), expNode.getPath(), ni.nextNode()
            .getPath());
      }
      assertFalse("Node has more than expected nodes.", ni.hasNext());

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

   private void checkColumns(QueryResult res, String[] columns, String[][] values, boolean hasMoreColumns)
      throws RepositoryException
   {
      // check column list

      String[] rcol = res.getColumnNames();
      if (!hasMoreColumns)
      {
         assertEquals("Columns count not equal to expected set", rcol.length, columns.length);
      }
      else
      {
         assertTrue(rcol.length >= columns.length);
      }

      for (String colname : columns)
      {
         boolean finded = false;
         for (String rescolname : rcol)
         {
            if (colname.equals(rescolname))
            {
               finded = true;
            }
         }
         assertTrue("Column name not founded : " + colname, finded);
      }

      //check values
      RowIterator rit = res.getRows();
      for (String[] row : values)
      {
         if (!rit.hasNext())
         {
            fail("Expected row not exist.");
         }
         Row resrow = rit.nextRow();
         for (int j = 0; j < columns.length; j++)
         {
            Value val = resrow.getValue(columns[j]);
            assertEquals(row[j], (val != null) ? val.getString() : null);
         }
      }
      assertFalse("There is more rows than expected", rit.hasNext());

   }
}
