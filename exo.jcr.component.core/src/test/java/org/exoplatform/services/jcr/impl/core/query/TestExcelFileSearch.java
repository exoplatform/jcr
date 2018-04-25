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
import org.exoplatform.services.document.DocumentReader;
import org.exoplatform.services.document.DocumentReaderService;
import org.exoplatform.services.document.impl.MSExcelDocumentReader;
import org.exoplatform.services.document.impl.tika.TikaDocumentReader;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.query.lucene.FieldNames;
import org.exoplatform.services.jcr.impl.core.query.lucene.Util;

import java.io.InputStream;
import java.util.Calendar;

/**
 * Created by The eXo Platform SAS Author : Sergey Karpenko <sergey.karpenko@exoplatform.com.ua>
 * 
 * @version $Id: $
 */

public class TestExcelFileSearch extends BaseQueryTest
{

   public void testFindFileContent() throws Exception
   {
      InputStream inputStream = TestExcelFileSearch.class.getResourceAsStream("/test.xls");
      assertNotNull("test.xls not found", inputStream);

      NodeImpl node = (NodeImpl)root.addNode("excelFile", "nt:file");
      NodeImpl cont = (NodeImpl)node.addNode("jcr:content", "nt:resource");
      cont.setProperty("jcr:mimeType", "application/excel");
      cont.setProperty("jcr:lastModified", Calendar.getInstance());
      // cont.setProperty("jcr:encoding","UTF-8");

      cont.setProperty("jcr:data", inputStream);
      root.save();

      inputStream.close();

      DocumentReaderService extr =
         (DocumentReaderService)session.getContainer().getComponentInstanceOfType(DocumentReaderService.class);

      DocumentReader dreader = extr.getDocumentReader("application/excel");
      assertNotNull(dreader);

      if (dreader instanceof MSExcelDocumentReader)
      {
         // OK
      }
      else if (dreader instanceof TikaDocumentReader)
      {
         String[] mimetypes = ((TikaDocumentReader)dreader).getMimeTypes();
         assertEquals("application/excel", mimetypes[0]);
      }
      else
      {
         fail("Wrong document reader");
      }

      // Arabic word
      String word = "eric";

      // Check is node indexed
      ScoreDoc doc = getDocument(cont.getInternalIdentifier(), false);
      assertNotNull("Node is not indexed", doc);

      IndexReader reader = defaultSearchIndex.getIndexReader();
      IndexSearcher is = new IndexSearcher(reader);
      TermQuery query = new TermQuery(new Term(FieldNames.FULLTEXT, word));
      TopDocs topDocs = is.search(query, null, Integer.MAX_VALUE);
      assertEquals(1, topDocs.totalHits);
      is.close();
      Util.closeOrRelease(reader);
   }

}
