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

import org.apache.lucene.search.Sort;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

import java.io.IOException;

import javax.jcr.RepositoryException;

/**
 * <code>MatchAllDocsQuery</code> extends the lucene <code>MatchAllDocsQuery</code>
 * and in addition implements {@link JcrQuery}.
 */
public class MatchAllDocsQuery
        extends org.apache.lucene.search.MatchAllDocsQuery
        implements JcrQuery {

   /**
    * The serial version UID
    */
   private static final long serialVersionUID = -5002771154307146872L;

   private IndexingConfiguration indexConfig;

   /**
    * @param index
    */
   public MatchAllDocsQuery(IndexingConfiguration indexConfig)
   {
      super();
      this.indexConfig = indexConfig;
   }

   /**
     * {@inheritDoc}
     */
    public QueryHits execute(JcrIndexSearcher searcher,
                             SessionImpl session,
                             Sort sort) throws IOException {
        if (sort.getSort().length == 0) {
            try {
                return new NodeTraversingQueryHits(
                        session.getRootNode(), true, indexConfig);
            } catch (RepositoryException e) {
                throw Util.createIOException(e);
            }
        } else {
            return null;
        }
    }
}
