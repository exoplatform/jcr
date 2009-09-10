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

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.query.lucene.IndexingConfigurationImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.Node;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: TestIndexingConfig.java 12051 2008-03-18 13:47:22Z serg $
 */
public class TestIndexingConfig extends BaseQueryTest
{
   public final String testString1 = "The quick brown fox jumped over the lazy dogs";

   public final String testString2 = "XY&Z Corporation - xyz@example.com";

   public final String simple = "simpleAnalyzer";

   public final String whitespace = "whitespaceAnalyzer";

   public final String stop = "stopAnalyzer";

   public final String def = "defaultAnalyzer"; // there might

   // be standard
   // analyzer

   Node testRoot = null;

   /**
    * Class logger.
    */
   private final Log log = ExoLogger.getLogger("jcr.TestIndexingConfig");

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      IndexingConfigurationImpl indexingConfigurationImpl =
         (IndexingConfigurationImpl)defaultSearchIndex.getIndexingConfig();
      indexingConfigurationImpl.setPropertyAnalyzer("FULL:" + simple, new SimpleAnalyzer());
      indexingConfigurationImpl.setPropertyAnalyzer("FULL:" + whitespace, new WhitespaceAnalyzer());
      indexingConfigurationImpl.setPropertyAnalyzer("FULL:" + stop, new StopAnalyzer());
      testRoot = root.addNode("testrootAnalyzers");
      root.save();
   }

   @Override
   public void tearDown() throws Exception
   {
      testRoot.remove();
      root.save();
      super.tearDown();
   }

   public void testSimplePropertyAnalyzer() throws Exception
   {
      try
      {
         NodeImpl testNode1 = (NodeImpl)testRoot.addNode("node1");
         testNode1.setProperty(simple, testString1);

         Node testNode2 = testRoot.addNode("node2");
         testNode2.setProperty(simple, testString2);

         root.save();

         // Test is there are all terms
         // There must be [the] [quick] [brown] [fox] [jumped] [over] [the] [lazy] [dogs]
         // in Node1

         Document doc = this.getDocument(testNode1.getInternalIdentifier(), false);
         System.out.println(doc);

         TermQuery the = new TermQuery(new Term("FULL:" + simple, "the"));
         TermQuery quick = new TermQuery(new Term("FULL:" + simple, "quick"));
         TermQuery brown = new TermQuery(new Term("FULL:" + simple, "brown"));
         TermQuery fox = new TermQuery(new Term("FULL:" + simple, "fox"));
         TermQuery jumped = new TermQuery(new Term("FULL:" + simple, "jumped"));
         TermQuery over = new TermQuery(new Term("FULL:" + simple, "over"));
         TermQuery lazy = new TermQuery(new Term("FULL:" + simple, "lazy"));
         TermQuery dogs = new TermQuery(new Term("FULL:" + simple, "dogs"));

         BooleanQuery compl = new BooleanQuery();
         compl.add(the, Occur.MUST);
         compl.add(quick, Occur.MUST);
         compl.add(brown, Occur.MUST);
         compl.add(fox, Occur.MUST);
         compl.add(jumped, Occur.MUST);
         compl.add(over, Occur.MUST);
         compl.add(lazy, Occur.MUST);
         compl.add(dogs, Occur.MUST);

         IndexReader ir = defaultSearchIndex.getIndexReader(false);
         IndexSearcher is = new IndexSearcher(ir);

         Hits hits = is.search(compl);
         assertEquals(1, hits.length());

         // Test is there are all terms
         // There must be [xy] [z] [corporation] [xyz] [example] [com]
         // in Node2
         TermQuery xy = new TermQuery(new Term("FULL:" + simple, "xy"));
         TermQuery z = new TermQuery(new Term("FULL:" + simple, "z"));
         TermQuery corporation = new TermQuery(new Term("FULL:" + simple, "corporation"));
         TermQuery xyz = new TermQuery(new Term("FULL:" + simple, "xyz"));
         TermQuery example = new TermQuery(new Term("FULL:" + simple, "example"));
         TermQuery com = new TermQuery(new Term("FULL:" + simple, "com"));

         compl = new BooleanQuery();
         compl.add(xy, Occur.MUST);
         compl.add(z, Occur.MUST);
         compl.add(corporation, Occur.MUST);
         compl.add(xyz, Occur.MUST);
         compl.add(example, Occur.MUST);
         compl.add(com, Occur.MUST);

         hits = is.search(compl);
         assertEquals(1, hits.length());

         ir.close();
         is.close();
      }
      catch (Exception e)
      {
         e.printStackTrace();
         throw e;
      }
   }

   public void testWhitespacePropertyAnalyzer() throws Exception
   {
      try
      {

         NodeImpl testNode1 = (NodeImpl)testRoot.addNode("node1");
         testNode1.setProperty(whitespace, testString1);

         Node testNode2 = testRoot.addNode("node2");
         testNode2.setProperty(whitespace, testString2);

         root.save();

         // Test is there are all terms
         // There must be [The] [quick] [brown] [fox] [jumped] [over] [the] [lazy] [dogs]
         // in Node1

         TermQuery The = new TermQuery(new Term("FULL:" + whitespace, "The"));
         TermQuery quick = new TermQuery(new Term("FULL:" + whitespace, "quick"));
         TermQuery brown = new TermQuery(new Term("FULL:" + whitespace, "brown"));
         TermQuery fox = new TermQuery(new Term("FULL:" + whitespace, "fox"));
         TermQuery jumped = new TermQuery(new Term("FULL:" + whitespace, "jumped"));
         TermQuery over = new TermQuery(new Term("FULL:" + whitespace, "over"));
         TermQuery the = new TermQuery(new Term("FULL:" + whitespace, "the"));
         TermQuery lazy = new TermQuery(new Term("FULL:" + whitespace, "lazy"));
         TermQuery dogs = new TermQuery(new Term("FULL:" + whitespace, "dogs"));

         BooleanQuery compl = new BooleanQuery();
         compl.add(The, Occur.MUST);
         compl.add(quick, Occur.MUST);
         compl.add(brown, Occur.MUST);
         compl.add(fox, Occur.MUST);
         compl.add(jumped, Occur.MUST);
         compl.add(over, Occur.MUST);
         compl.add(the, Occur.MUST);
         compl.add(lazy, Occur.MUST);
         compl.add(dogs, Occur.MUST);

         IndexReader ir = defaultSearchIndex.getIndexReader(false);
         IndexSearcher is = new IndexSearcher(ir);

         Hits hits = is.search(compl);
         assertEquals(1, hits.length());

         // Test is there are all terms
         // There must be [XY&Z] [Corporation] [-] [xyz@example.com]
         // in Node2
         TermQuery XYandZ = new TermQuery(new Term("FULL:" + whitespace, "XY&Z"));
         TermQuery corporation = new TermQuery(new Term("FULL:" + whitespace, "Corporation"));
         TermQuery defiz = new TermQuery(new Term("FULL:" + whitespace, "-"));
         TermQuery example = new TermQuery(new Term("FULL:" + whitespace, "xyz@example.com"));

         compl = new BooleanQuery();
         compl.add(XYandZ, Occur.MUST);
         compl.add(corporation, Occur.MUST);
         compl.add(defiz, Occur.MUST);
         compl.add(example, Occur.MUST);

         hits = is.search(compl);
         assertEquals(1, hits.length());

         ir.close();
         is.close();
      }
      catch (Exception e)
      {
         e.printStackTrace();
         throw e;
      }
   }

   public void testStopPropertyAnalyzer() throws Exception
   {
      try
      {
         NodeImpl testNode1 = (NodeImpl)testRoot.addNode("node1");
         testNode1.setProperty(stop, testString1);

         Node testNode2 = testRoot.addNode("node2");
         testNode2.setProperty(stop, testString2);

         root.save();

         // Test is there are all terms
         // There must be [quick] [brown] [fox] [jumped] [over] [lazy] [dogs]
         // in Node1

         TermQuery quick = new TermQuery(new Term("FULL:" + stop, "quick"));
         TermQuery brown = new TermQuery(new Term("FULL:" + stop, "brown"));
         TermQuery fox = new TermQuery(new Term("FULL:" + stop, "fox"));
         TermQuery jumped = new TermQuery(new Term("FULL:" + stop, "jumped"));
         TermQuery over = new TermQuery(new Term("FULL:" + stop, "over"));
         TermQuery lazy = new TermQuery(new Term("FULL:" + stop, "lazy"));
         TermQuery dogs = new TermQuery(new Term("FULL:" + stop, "dogs"));

         BooleanQuery compl = new BooleanQuery();
         compl.add(quick, Occur.MUST);
         compl.add(brown, Occur.MUST);
         compl.add(fox, Occur.MUST);
         compl.add(jumped, Occur.MUST);
         compl.add(over, Occur.MUST);
         compl.add(lazy, Occur.MUST);
         compl.add(dogs, Occur.MUST);

         IndexReader ir = defaultSearchIndex.getIndexReader(false);
         IndexSearcher is = new IndexSearcher(ir);

         Hits hits = is.search(compl);
         assertEquals(1, hits.length());

         // Test is there are all terms
         // There must be [xy] [z] [corporation] [xyz] [example] [com]
         // in Node2
         TermQuery xy = new TermQuery(new Term("FULL:" + stop, "xy"));
         TermQuery z = new TermQuery(new Term("FULL:" + stop, "z"));
         TermQuery corporation = new TermQuery(new Term("FULL:" + stop, "corporation"));
         TermQuery xyz = new TermQuery(new Term("FULL:" + stop, "xyz"));
         TermQuery example = new TermQuery(new Term("FULL:" + stop, "example"));
         TermQuery com = new TermQuery(new Term("FULL:" + stop, "com"));

         compl = new BooleanQuery();
         compl.add(xy, Occur.MUST);
         compl.add(z, Occur.MUST);
         compl.add(corporation, Occur.MUST);
         compl.add(xyz, Occur.MUST);
         compl.add(example, Occur.MUST);
         compl.add(com, Occur.MUST);

         hits = is.search(compl);
         assertEquals(1, hits.length());

         ir.close();
         is.close();
      }
      catch (Exception e)
      {
         e.printStackTrace();
         throw e;
      }
   }

   public void testDefaultPropertyAnalyzer() throws Exception
   {
      try
      {
         // StandardAnalyzer used for default

         NodeImpl testNode1 = (NodeImpl)testRoot.addNode("node1");
         testNode1.setProperty(def, testString1);

         Node testNode2 = testRoot.addNode("node2");
         testNode2.setProperty(def, testString2);

         root.save();

         // Test is there are all terms
         // There must be [quick] [brown] [fox] [jumped] [over] [lazy] [dogs]
         // in Node1
         TermQuery the = new TermQuery(new Term("FULL:" + def, "the"));
         TermQuery quick = new TermQuery(new Term("FULL:" + def, "quick"));
         TermQuery brown = new TermQuery(new Term("FULL:" + def, "brown"));
         TermQuery fox = new TermQuery(new Term("FULL:" + def, "fox"));
         TermQuery jumped = new TermQuery(new Term("FULL:" + def, "jumped"));
         TermQuery over = new TermQuery(new Term("FULL:" + def, "over"));
         TermQuery lazy = new TermQuery(new Term("FULL:" + def, "lazy"));
         TermQuery dogs = new TermQuery(new Term("FULL:" + def, "dogs"));

         BooleanQuery compl = new BooleanQuery();

         compl.add(the, Occur.MUST);
         compl.add(quick, Occur.MUST);
         compl.add(brown, Occur.MUST);
         compl.add(fox, Occur.MUST);
         compl.add(jumped, Occur.MUST);
         compl.add(over, Occur.MUST);
         compl.add(lazy, Occur.MUST);
         compl.add(dogs, Occur.MUST);

         IndexReader ir = defaultSearchIndex.getIndexReader(false);
         IndexSearcher is = new IndexSearcher(ir);

         Hits hits = is.search(compl);
         assertEquals(1, hits.length());

         // Test is there are all terms
         // Terms [xy&z] [corporation] [xyz@example] [com] - it's a default
         // lucene StandardAnalyzer with own stop words set.
         // In our case, there are StandardAnalyzer with empty stop words set, so
         // there must be terms : [corporation] [xy&z] [xyz@example.com]

         TermQuery xy = new TermQuery(new Term("FULL:" + def, "xy&z"));
         TermQuery corporation = new TermQuery(new Term("FULL:" + def, "corporation"));
         TermQuery com = new TermQuery(new Term("FULL:" + def, "xyz@example.com"));

         compl = new BooleanQuery();
         compl.add(xy, Occur.MUST);
         compl.add(corporation, Occur.MUST);
         compl.add(com, Occur.MUST);

         hits = is.search(compl);
         assertEquals(1, hits.length());

         ir.close();
         is.close();
      }
      catch (Exception e)
      {
         e.printStackTrace();
         throw e;
      }
   }

}
