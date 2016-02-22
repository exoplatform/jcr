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

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Weight;

import java.util.Set;

/**
 * Specialized query that returns / scores all pages in the search index.
 * <p>Use this Query to perform a match '*'.
 */
class MatchAllQuery extends Query {

   /**
    * The serial version UID
    */
   private static final long serialVersionUID = -2400976814962542982L;

   private final String field;

   /**
    * Creates a new <code>MatchAllQuery</code> .
    * <br>
    *
    * @param field the field name.
    * @throws IllegalArgumentException if <code>field</code> is null.
    */
    MatchAllQuery(String field) throws IllegalArgumentException {
        if (field == null) {
            throw new IllegalArgumentException("parameter field cannot be null");
        }
        this.field = field.intern();
    }

    /**
     * Returns the <code>Weight</code> for this Query.
     *
     * @param searcher the current searcher.
     * @return the <code>Weight</code> for this Query.
     */
    @Override
   public Weight createWeight(Searcher searcher) {
        return new MatchAllWeight(this, searcher, field);
   }

    /**
     * Returns the String "%".
     *
     * @param field default field for the query.
     * @return the String "%".
     */
    @Override
   public String toString(String field) {
        return "%";
   }

    /**
     * Does nothing but simply returns. There are no terms to extract.
     */
    @Override
   public void extractTerms(Set<Term> terms) {
   }
}
