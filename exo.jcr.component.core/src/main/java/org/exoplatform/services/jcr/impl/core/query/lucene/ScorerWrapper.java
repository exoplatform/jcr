/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Similarity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * WorkAround wrapper, used as bridge between Scorer.score(Collector) and DocIdSetIterator.
 * Some Scorers inside Lucene (BooleanScorer) doesn't support DocIdSetIterator interface, 
 * but required for JCR needs.
 * Consider getting rid of this solution.
 * 
 * @author <a href="mailto:nzamosenchuk@exoplatform.com">Nikolay Zamosenchuk</a>
 * @version $Id: ScorerWrapper.java 34360 2009-07-22 23:58:59Z nzamosenchuk $
 *
 */
public class ScorerWrapper extends Scorer
{

   List<DocData> docs = new ArrayList<DocData>();

   int index;

   DocData currentDocData = null;

   CollectorWrapper collectorWrapper;

   static class DocData
   {
      public int docID;

      public float freq;

      public float score;

      public DocData(int docID, float freq, float score)
      {
         super();
         this.docID = docID;
         this.freq = freq;
         this.score = score;
      }

   }

   class CollectorWrapper extends Collector
   {
      private Scorer subScrorer;

      @Override
      public void setScorer(Scorer scorer) throws IOException
      {
         this.subScrorer = scorer;
      }

      @Override
      public void collect(int doc) throws IOException
      {
         ScorerWrapper.this.docs.add(new DocData(doc, subScrorer.freq(), subScrorer.score()));
      }

      @Override
      public void setNextReader(IndexReader reader, int docBase) throws IOException
      {
      }

      @Override
      public boolean acceptsDocsOutOfOrder()
      {
         return true;
      }

   }

   /**
    * @param similarity
    */
   protected ScorerWrapper(Similarity similarity)
   {
      super(similarity);

      collectorWrapper = new CollectorWrapper();
   }

   /**
   * {@inheritDoc}
   */
   @Override
   public float score() throws IOException
   {
      if (currentDocData != null)
      {
         return currentDocData.score;
      }
      return 0;
   }

   /**
   * {@inheritDoc}
   */
   @Override
   public int docID()
   {
      if (currentDocData != null)
      {
         return currentDocData.docID;
      }
      return NO_MORE_DOCS;
   }

   /**
   * {@inheritDoc}
   */
   @Override
   public int nextDoc() throws IOException
   {
      if (index < docs.size())
      {
         currentDocData = docs.get(index);
         index++;
         return currentDocData.docID;
      }
      else
      {
         currentDocData = null;
         return NO_MORE_DOCS;
      }
   }

   /**
   * {@inheritDoc}
   */
   @Override
   public int advance(int target) throws IOException
   {
      int doc;
      while ((doc = nextDoc()) < target)
      {
         if (doc == NO_MORE_DOCS || doc == -1)
         {
            return NO_MORE_DOCS;
         }
      }
      return doc;
   }

   public Collector getCollector()
   {
      return collectorWrapper;
   }

}
