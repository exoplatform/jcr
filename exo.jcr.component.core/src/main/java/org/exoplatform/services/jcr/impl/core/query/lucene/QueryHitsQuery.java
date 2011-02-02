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
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.Weight;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * <code>QueryHitsQuery</code> exposes a {@link QueryHits} implementation again
 * as a Lucene Query.
 */
public class QueryHitsQuery extends Query implements JcrQuery{

    /**
     * The underlying query hits.
     */
    private final QueryHits hits;

    /**
     * Creates a new query based on {@link QueryHits}.
     *
     * @param hits the query hits.
     */
    public QueryHitsQuery(QueryHits hits) {
        this.hits = hits;
    }

    /**
     * {@inheritDoc}
     */
    @Override
   public Weight createWeight(Searcher searcher) throws IOException {
        return new QueryHitsQueryWeight(searcher.getSimilarity());
    }

    /**
     * {@inheritDoc}
     */
    @Override
   public String toString(String field) {
        return "QueryHitsQuery";
    }

    /**
     * {@inheritDoc}
     */
    @Override
   public void extractTerms(Set terms) {
        // no terms
    }

    //-----------------------< JackrabbitQuery >--------------------------------

    /**
     * {@inheritDoc}
     */
    public QueryHits execute(JcrIndexSearcher searcher,
                             SessionImpl session,
                             Sort sort) throws IOException {
        if (sort.getSort().length == 0) {
            return hits;
        } else {
            return null;
        }
    }

    //------------------------< QueryHitsQueryWeight >--------------------------

    /**
     * The Weight implementation for this query.
     */
    public class QueryHitsQueryWeight extends Weight {

        /**
         * The similarity.
         */
        private final Similarity similarity;

        /**
         * Creates a new weight with the given <code>similarity</code>.
         *
         * @param similarity the similarity.
         */
        public QueryHitsQueryWeight(Similarity similarity) {
            this.similarity = similarity;
        }

        /**
         * {@inheritDoc}
         */
        @Override
      public Query getQuery() {
            return QueryHitsQuery.this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
      public float getValue() {
            return 1.0f;
        }

        /**
         * {@inheritDoc}
         */
        @Override
      public float sumOfSquaredWeights() throws IOException {
            return 1.0f;
        }

        /**
         * {@inheritDoc}
         */
        @Override
      public void normalize(float norm) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
      public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
           // TODO: use arguments!
            return new QueryHitsQueryScorer(reader, similarity);
        }

        /**
         * {@inheritDoc}
         */
        @Override
      public Explanation explain(IndexReader reader, int doc) throws IOException {
            return new Explanation();
        }
    }

    //-------------------< QueryHitsQueryScorer >-------------------------------

    /**
     * the scorer implementation for this query.
     */
    public class QueryHitsQueryScorer extends Scorer {

        /**
         * Iterator over <code>Integer</code> instances identifying the
         * lucene documents. Document numbers are iterated in ascending order.
         */
        private final Iterator docs;

        /**
         * Maps <code>Integer</code> document numbers to <code>Float</code>
         * scores.
         */
        private final Map scores = new HashMap();

        /**
         * The current document number.
         */
        private Integer currentDoc = null;

        /**
         * Creates a new scorer.
         *
         * @param reader     the index reader.
         * @param similarity the similarity implementation.
         * @throws IOException if an error occurs while reading from the index.
         */
        protected QueryHitsQueryScorer(IndexReader reader,
                                       Similarity similarity)
                throws IOException {
            super(similarity);
            ScoreNode node;
            Set sortedDocs = new TreeSet();
            try {
                while ((node = hits.nextScoreNode()) != null) {
                    String uuid = node.getNodeId();
                    Term id = new Term(FieldNames.UUID, uuid);
                    TermDocs tDocs = reader.termDocs(id);
                    try {
                        if (tDocs.next()) {
                            Integer doc = new Integer(tDocs.doc());
                            sortedDocs.add(doc);
                            scores.put(doc, new Float(node.getScore()));
                        }
                    } finally {
                        tDocs.close();
                    }
                }
            } finally {
                hits.close();
            }
            docs = sortedDocs.iterator();
        }

        /**
         * {@inheritDoc}
         */
        @Override
      public boolean next() throws IOException {
            if (docs.hasNext()) {
                currentDoc = (Integer) docs.next();
                return true;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
      public int doc() {
            return currentDoc.intValue();
        }

        /**
         * {@inheritDoc}
         */
        @Override
      public float score() throws IOException {
            return ((Float) scores.get(currentDoc)).floatValue();
        }

        /**
         * {@inheritDoc}
         */
        @Override
      public boolean skipTo(int target) throws IOException {
            do {
                if (!next()) {
                    return false;
                }
            } while (target > doc());
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
      public Explanation explain(int doc) throws IOException {
            return new Explanation();
        }
    }
}
