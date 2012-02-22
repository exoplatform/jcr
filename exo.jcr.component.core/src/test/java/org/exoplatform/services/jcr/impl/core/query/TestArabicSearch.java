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

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.query.lucene.FieldNames;
import org.exoplatform.services.jcr.impl.core.query.lucene.Util;

import java.io.FileInputStream;
import java.net.URL;
import java.util.Calendar;

import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

/**
 * Created by The eXo Platform SAS Author : Sergey Karpenko <sergey.karpenko@exoplatform.com.ua>
 * 
 * @version $Id: $
 */

public class TestArabicSearch extends BaseQueryTest
{

   public static String fileName = "testArabicUTF8";

   public void testSearchWithEncodingParameter() throws Exception
   {

      URL url = TestArabicSearch.class.getResource("/ArabicUTF8.txt");
      assertNotNull("ArabicUTF8.txt not found", url);

      FileInputStream fis = new FileInputStream(url.getFile());

      NodeImpl node = (NodeImpl)root.addNode(fileName, "nt:file");
      NodeImpl cont = (NodeImpl)node.addNode("jcr:content", "nt:resource");
      cont.setProperty("jcr:mimeType", "text/plain");
      cont.setProperty("jcr:lastModified", Calendar.getInstance());
      cont.setProperty("jcr:encoding", "UTF-8");

      cont.setProperty("jcr:data", fis);
      root.save();

      // Arabic word
      String word = "\u0627\u0644\u0644\u0627\u062a\u064a\u0646\u064a\u0629";

      // Check is node indexed
      ScoreDoc doc = getDocument(cont.getInternalIdentifier(), false);
      assertNotNull("Node is not indexed", doc);

      IndexReader reader = defaultSearchIndex.getIndexReader();
      IndexSearcher is = new IndexSearcher(reader);
      TermQuery query = new TermQuery(new Term(FieldNames.FULLTEXT, word));
      TopDocs search = is.search(query, null, Integer.MAX_VALUE);
      assertEquals(1, search.totalHits);

      QueryManager qman = this.workspace.getQueryManager();

      Query q = qman.createQuery("SELECT * FROM nt:resource " + " WHERE  CONTAINS(., '" + word + "')", Query.SQL);
      QueryResult res = q.execute();
      assertEquals(1, res.getNodes().getSize());

      is.close();
      Util.closeOrRelease(reader);
   }

   /**
    * This test needs <name>defaultEncoding</name> <description>description</description>
    * <value>UTF-8</value> parameter in exo.core.component.document conf.portal.configuration.xml
    * <component-plugin> <name>TPdocument.reader</name>..
    * 
    * @throws Exception
    */
   /*
    * public void testSearchWithDefaultReaderEncoding() throws Exception{ File file = new
    * File("src/test/resources/ArabicUTF8.txt");
    * assertTrue("/test/resources/ArabicUTF8.txt not found",file.exists()); FileInputStream fis = new
    * FileInputStream(file); NodeImpl node = (NodeImpl)root.addNode(fileName+"sec","nt:file");
    * NodeImpl cont = (NodeImpl)node.addNode("jcr:content","nt:resource");
    * cont.setProperty("jcr:mimeType", "text/plain"); cont.setProperty("jcr:lastModified",
    * Calendar.getInstance()); // cont.setProperty("jcr:encoding","UTF-8");
    * cont.setProperty("jcr:data", fis); root.save(); // Arabic word String word =
    * "\u0627\u0644\u0644\u0627\u062a\u064a\u0646\u064a\u0629"; //Check is node indexed Document doc
    * = getDocument(cont.getInternalIdentifier(), false); assertNotNull("Node is not indexed",doc);
    * IndexReader reader = defaultSearchIndex.getIndexReader(false); IndexSearcher is = new
    * IndexSearcher(reader); TermQuery query = new TermQuery(new Term(FieldNames.FULLTEXT, word ));
    * Hits result = is.search(query); assertEquals(1,result.length()); QueryManager qman =
    * this.workspace.getQueryManager(); Query q = qman.createQuery("SELECT * FROM nt:resource " +
    * " WHERE  CONTAINS(., '" + word + "')", Query.SQL); QueryResult res = q.execute();
    * assertEquals(1,res.getNodes().getSize()); }
    */
}
