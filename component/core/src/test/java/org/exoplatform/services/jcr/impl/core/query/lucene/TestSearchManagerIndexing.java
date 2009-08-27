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

import java.util.Properties;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.services.log.Log;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.query.SearchManager;
import org.exoplatform.services.log.ExoLogger;

/**
 * Created by The eXo Platform SAS Author : Sergey Karpenko <sergey.karpenko@exoplatform.com.ua>
 * 
 * @version $Id: TestSearchManagerIndexing.java 13111 2008-04-11 08:22:13Z serg $
 */

public class TestSearchManagerIndexing
   extends JcrImplBaseTest
{
   public static final Log logger = ExoLogger.getLogger(TestSearchManagerIndexing.class);

   private SearchManager manager;

   public void testAdditionNode() throws Exception
   {
      assertNotNull(manager);
      SearchIndex si = (SearchIndex) manager.getHandler();
      IndexReader ir = si.getIndex().getIndexReader();
      int docnum = ir.numDocs();
      ir.close();

      Node node = root.addNode("test");
      node.setProperty("prop", "string value");
      root.save();

      ir = si.getIndex().getIndexReader();
      assertEquals(docnum + 1, ir.numDocs());

      for (int i = 0; i < ir.numDocs(); i++)
      {
         if (!ir.isDeleted(i))
         {
            logger.info(" DOCUMENT [" + i + "] name [" + ir.document(i).getField(FieldNames.LABEL).stringValue() + "]");
         }

      }
      logger.info(" DOCUMENT [] name [" + getIndexPrefix(si, "") + "test" + "]");

      IndexSearcher is = new IndexSearcher(ir);

      // Test is next addings are in index
      TermQuery name = new TermQuery(new Term(FieldNames.LABEL, getIndexPrefix(si, "") + "test"));
      TermQuery prop1 =
               new TermQuery(new Term(FieldNames.PROPERTIES, getIndexPrefix(si, "") + "prop" + '\uFFFF'
                        + "string value"));
      TermQuery prop2 =
               new TermQuery(new Term(FieldNames.PROPERTIES, getIndexPrefix(si, "jcr") + "primaryType" + '\uFFFF'
                        + getIndexPrefix(si, "nt") + "unstructured"));
      TermQuery full1 = new TermQuery(new Term(FieldNames.FULLTEXT, "string"));
      TermQuery full2 = new TermQuery(new Term(FieldNames.FULLTEXT, "value"));
      TermQuery fullprop1 =
               new TermQuery(new Term(FieldNames.createFullTextFieldName(getIndexPrefix(si, "") + "prop"), "string"));
      TermQuery fullprop2 =
               new TermQuery(new Term(FieldNames.createFullTextFieldName(getIndexPrefix(si, "") + "prop"), "value"));

      BooleanQuery compl = new BooleanQuery();
      compl.add(name, Occur.MUST);
      compl.add(prop1, Occur.MUST);
      compl.add(prop2, Occur.MUST);
      compl.add(full1, Occur.MUST);
      compl.add(full2, Occur.MUST);
      compl.add(fullprop1, Occur.MUST);
      compl.add(fullprop2, Occur.MUST);

      Hits hits = is.search(compl);
      assertEquals(1, hits.length());

      ir.close();
      is.close();
   }

   public void testRemoveNode() throws Exception
   {
      assertNotNull(manager);

      SearchIndex si = (SearchIndex) manager.getHandler();
      IndexReader ir = si.getIndex().getIndexReader();

      int docnum = ir.numDocs();
      ir.close();

      // add node
      Node node = root.addNode("test");
      root.save();

      ir = si.getIndex().getIndexReader();
      assertEquals(docnum + 1, ir.numDocs());
      ir.close();

      node.remove();
      root.save();

      ir = si.getIndex().getIndexReader();
      assertEquals(docnum, ir.numDocs());
      ir.close();
   }

   /**
    * Here is test of correct indexing changes in repository.
    * 
    * @throws Exception
    */
   public void testMoveNode() throws Exception
   {
      assertNotNull(manager);
      SearchIndex si = (SearchIndex) manager.getHandler();
      IndexReader ir = si.getIndex().getIndexReader();
      int docnum = ir.numDocs();
      ir.close();

      Node mid = root.addNode("mid");
      Node node = mid.addNode("test");
      root.save();

      ir = si.getIndex().getIndexReader();
      assertEquals(docnum + 2, ir.numDocs());

      IndexSearcher is = new IndexSearcher(ir);
      NodeData data = (NodeData) ((NodeImpl) node).getData();

      TermQuery query = new TermQuery(new Term(FieldNames.UUID, data.getIdentifier()));
      Hits hits = is.search(query);
      assertEquals(1, hits.length());

      Document doc = hits.doc(0);
      // check that "node" parent is "mid"
      assertEquals(((NodeData) ((NodeImpl) mid).getData()).getIdentifier(), doc.getField(FieldNames.PARENT)
               .stringValue());

      ir.close();
      is.close();

      // move
      session.move("/mid/test", "/test");
      session.save();

      ir = si.getIndex().getIndexReader();
      assertEquals(docnum + 2, ir.numDocs());

      is = new IndexSearcher(ir);
      hits = is.search(query);
      assertEquals(1, hits.length());

      doc = hits.doc(0);
      // check that "node" parent is root-node
      assertEquals(((NodeData) ((NodeImpl) root).getData()).getIdentifier(), doc.getField(FieldNames.PARENT)
               .stringValue());

      ir.close();
      is.close();
   }

   public void testRenameNode() throws Exception
   {
      final String nodeName = "test";
      final String newNodeName = "newName";

      assertNotNull(manager);
      SearchIndex si = (SearchIndex) manager.getHandler();
      IndexReader ir = si.getIndex().getIndexReader();
      int docnum = ir.numDocs();
      ir.close();

      root.addNode(nodeName);
      root.save();

      ir = si.getIndex().getIndexReader();
      assertEquals(docnum + 1, ir.numDocs());

      IndexSearcher is = new IndexSearcher(ir);
      TermQuery query = new TermQuery(new Term(FieldNames.LABEL, getIndexPrefix(si, "") + nodeName));
      Hits hits = is.search(query);
      assertEquals(1, hits.length());

      ir.close();
      is.close();

      // rename
      session.move("/" + nodeName, "/" + newNodeName);
      session.save();

      ir = si.getIndex().getIndexReader();
      assertEquals(docnum + 1, ir.numDocs());

      is = new IndexSearcher(ir);
      query = new TermQuery(new Term(FieldNames.LABEL, getIndexPrefix(si, "") + newNodeName));
      hits = is.search(query);
      assertEquals(1, hits.length());

      ir.close();
      is.close();
   }

   public void testSameName() throws Exception
   {
      assertNotNull(manager);
      SearchIndex si = (SearchIndex) manager.getHandler();
      IndexReader ir = si.getIndex().getIndexReader();
      int docnum = ir.numDocs();
      ir.close();

      final String nodeName = "test";

      root.addNode(nodeName);
      root.addNode(nodeName);
      root.save();

      ir = si.getIndex().getIndexReader();

      assertEquals(docnum + 2, ir.numDocs());

      IndexSearcher is = new IndexSearcher(ir);
      TermQuery query = new TermQuery(new Term(FieldNames.LABEL, getIndexPrefix(si, "") + nodeName));
      Hits hits = is.search(query);
      assertEquals(2, hits.length());
   }

   public void testAddMixinType() throws Exception
   {
      assertNotNull(manager);
      SearchIndex si = (SearchIndex) manager.getHandler();
      IndexReader ir = si.getIndex().getIndexReader();
      int docnum = ir.numDocs();
      ir.close();
      final String nodeName = "test";

      Node node = root.addNode(nodeName);
      root.save();

      ir = si.getIndex().getIndexReader();
      assertEquals(docnum + 1, ir.numDocs());

      TermQuery name = new TermQuery(new Term(FieldNames.LABEL, getIndexPrefix(si, "") + nodeName));

      TermQuery prop1 =
               new TermQuery(new Term(FieldNames.PROPERTIES, getIndexPrefix(si, "jcr") + "primaryType" + '\uFFFF'
                        + getIndexPrefix(si, "nt") + "unstructured"));
      BooleanQuery compl = new BooleanQuery();
      compl.add(name, Occur.MUST);
      compl.add(prop1, Occur.MUST);

      IndexSearcher is = new IndexSearcher(ir);
      Hits hits = is.search(compl);
      assertEquals(1, hits.length());

      // add mixin
      node.addMixin("mix:referenceable");
      root.save();

      ir = si.getIndex().getIndexReader();
      assertEquals(docnum + 1, ir.numDocs());

      TermQuery prop2 =
               new TermQuery(new Term(FieldNames.PROPERTIES, getIndexPrefix(si, "jcr") + "uuid" + '\uFFFF'
                        + node.getUUID()));
      compl.add(prop2, Occur.MUST);

      is = new IndexSearcher(ir);
      hits = is.search(compl);
      assertEquals(1, hits.length());
   }

   public void setUp() throws Exception
   {
      super.setUp();
      manager = (SearchManager) this.session.getContainer().getComponentInstanceOfType(SearchManager.class);
   }

   private String getIndexPrefix(SearchIndex si, String stPref) throws RepositoryException, NamespaceException
   {

      Properties props = new Properties();
      props.setProperty("", "");
      props.setProperty("jcr", "http://www.jcp.org/jcr/1.0");
      props.setProperty("nt", "http://www.jcp.org/jcr/nt/1.0");
      props.setProperty("mix", "http://www.jcp.org/jcr/mix/1.0");
      props.setProperty("xml", "http://www.w3.org/XML/1998/namespace");
      props.setProperty("sv", "http://www.jcp.org/jcr/sv/1.0");
      props.setProperty("exo", "http://www.exoplatform.com/jcr/exo/1.0");
      props.setProperty("xs", "http://www.w3.org/2001/XMLSchema");
      props.setProperty("fn", "http://www.w3.org/2004/10/xpath-functions");

      String result = (si.getNamespaceMappings().getNamespacePrefixByURI(props.getProperty(stPref)) + ":");
      return (result.equals(":") ? "" : result);
   }
}
