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

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.impl.core.query.lucene.FieldNames;
import org.exoplatform.services.jcr.impl.core.query.lucene.SearchIndex;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: BaseQueryTest.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class BaseQueryTest extends JcrImplBaseTest
{
   /**
    * Class logger.
    */
   private final Log log = ExoLogger.getLogger("jcr.BaseQueryTest");

   protected SearchManager defaultSearchManager;

   protected SystemSearchManager systemSearchManager;

   protected SearchIndex defaultSearchIndex;

   protected Document getDocument(String nodeIdentifer, boolean includeSystemIndex) throws IOException,
      RepositoryException
   {
      IndexReader reader = defaultSearchIndex.getIndexReader(includeSystemIndex);
      IndexSearcher is = new IndexSearcher(reader);
      TermQuery query = new TermQuery(new Term(FieldNames.UUID, nodeIdentifer));

      Hits result = is.search(query);

      if (result.length() == 1)
         return result.doc(0);
      else if (result.length() > 1)
         throw new RepositoryException("Results more then one");

      return null;
   }

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      this.defaultSearchManager = (SearchManager)session.getContainer().getComponentInstanceOfType(SearchManager.class);
      this.systemSearchManager =
         (SystemSearchManager)session.getContainer().getComponentInstanceOfType(SystemSearchManager.class);
      this.defaultSearchIndex = (SearchIndex)defaultSearchManager.getHandler();
   }
}
