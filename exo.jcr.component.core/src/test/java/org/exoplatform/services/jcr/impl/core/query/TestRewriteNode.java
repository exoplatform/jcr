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
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.query.lucene.FieldNames;
import org.exoplatform.services.jcr.impl.core.query.lucene.Util;

import java.util.Calendar;

/**
 * Created by The eXo Platform SAS Author : Sergey Karpenko <sergey.karpenko@exoplatform.com.ua>
 * 
 * @version $Id: $
 */

public class TestRewriteNode extends BaseQueryTest
{

   private static final String fileName = "FileToRewrite";

   public void testRewriteNode() throws Exception
   {

      NodeImpl node = (NodeImpl)root.addNode(fileName, "nt:file");
      NodeImpl cont = (NodeImpl)node.addNode("jcr:content", "nt:resource");
      cont.setProperty("jcr:mimeType", "text/plain");
      cont.setProperty("jcr:lastModified", Calendar.getInstance());
      // cont.setProperty("jcr:encoding","UTF-8");

      cont.setProperty("jcr:data", "The Quick brown fox jumped over the lazy dog");
      root.save();

      IndexReader reader = defaultSearchIndex.getIndexReader();
      IndexSearcher is = new IndexSearcher(reader);
      TermQuery query = new TermQuery(new Term(FieldNames.FULLTEXT, "fox"));
      TopDocs topDocs = is.search(query, null, Integer.MAX_VALUE);
      assertEquals(1, topDocs.totalHits);

      cont.setProperty("jcr:data", "Bahama mama");
      root.save();

      reader = defaultSearchIndex.getIndexReader();
      is = new IndexSearcher(reader);
      query = new TermQuery(new Term(FieldNames.FULLTEXT, "mama"));
      topDocs = is.search(query, null, Integer.MAX_VALUE);
      assertEquals(1, topDocs.totalHits);

      reader = defaultSearchIndex.getIndexReader();
      is = new IndexSearcher(reader);
      query = new TermQuery(new Term(FieldNames.FULLTEXT, "fox"));
      topDocs = is.search(query, null, Integer.MAX_VALUE);
      assertEquals(0, topDocs.totalHits);

      is.close();
      Util.closeOrRelease(reader);

   }

}
