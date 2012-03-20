/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.exoplatform.services.jcr.impl.core.query.lucene;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;

import java.io.IOException;

/**
 * Wraps a lucene query result and adds a close method that allows to release
 * resources after a query has been executed and the results have been read
 * completely.
 */
public class LuceneQueryHits implements QueryHits
{

   /**
    * The IndexReader in use by the lucene hits.
    */
   private final IndexReader reader;

   /**
    * The scorer for the query.
    */
   private final Scorer scorer;

   /**
    * Release IndexReader on LuceneQueryHits.close().
    */
   private final boolean releaseReaderOnClose;

   public LuceneQueryHits(IndexReader reader, IndexSearcher searcher, Query query) throws IOException
   {
      this(reader, searcher, query, false);
   }

   /**
    * Constructor.
    * 
    * @param reader IndexReader
    * @param searcher IndexSearcher
    * @param query Query
    * @param releaseReaderOnClose - release IndexReader on LuceneQueryHits.close().
    * @throws IOException
    */
   public LuceneQueryHits(IndexReader reader, IndexSearcher searcher, Query query, boolean releaseReaderOnClose)
      throws IOException
   {
      this.reader = reader;
      this.scorer = query.weight(searcher).scorer(reader, true, false);
      this.releaseReaderOnClose = releaseReaderOnClose;
   }

   /**
    * {@inheritDoc}
    */
   public ScoreNode nextScoreNode() throws IOException
   {
      if (scorer == null)
      {
         return null;
      }
      int doc = scorer.nextDoc();
      if (doc == DocIdSetIterator.NO_MORE_DOCS)
      {
         return null;
      }
      String uuid = reader.document(doc).get(FieldNames.UUID);
      return new ScoreNode(uuid, scorer.score(), doc);
   }

   /**
    * {@inheritDoc}
    */
   public void close() throws IOException
   {
      if (scorer != null)
      {
         // make sure scorer frees resources
         scorer.advance(Integer.MAX_VALUE);
      }

      if (releaseReaderOnClose && reader != null && reader instanceof ReleaseableIndexReader)
      {
         ((ReleaseableIndexReader)reader).release();
      }
   }

   /**
    * @return always -1.
    */
   public int getSize()
   {
      return -1;
   }

   /**
    * {@inheritDoc}
    */
   public void skip(int n) throws IOException
   {
      while (n-- > 0)
      {
         if (nextScoreNode() == null)
         {
            return;
         }
      }
   }
}
