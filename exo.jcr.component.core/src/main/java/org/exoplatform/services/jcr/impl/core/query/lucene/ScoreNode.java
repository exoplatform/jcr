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

import java.io.IOException;

/**
 * <code>ScoreNode</code> implements a simple container which holds a mapping
 * of NodeId to a score value.
 */
public final class ScoreNode {

    /**
     * The id of a node.
     */
    private final String id;

    /**
     * The score of the node.
     */
    private final float score;

    /**
     * The lucene document number for this score node. Set to <code>-1</code> if
     * unknown.
     */
    private final int doc;

    /**
     * Creates a new <code>ScoreNode</code>.
     *
     * @param id    the node id.
     * @param score the score value.
     */
    public ScoreNode(String id, float score) {
        this(id, score, -1);
    }

    /**
     * Creates a new <code>ScoreNode</code>.
     *
     * @param id    the node id.
     * @param score the score value.
     * @param doc   the document number.
     */
    public ScoreNode(String id, float score, int doc) {
        this.id = id;
        this.score = score;
        this.doc = doc;
    }

    /**
     * @return the node id for this <code>ScoreNode</code>.
     */
    public String getNodeId() {
        return id;
    }

    /**
     * @return the score for this <code>ScoreNode</code>.
     */
    public float getScore() {
        return score;
    }

    /**
     * Returns the document number for this score node.
     *
     * @param reader the current index reader to look up the document if
     *               needed.
     * @return the document number.
     * @throws IOException if an error occurs while reading from the index or
     *                     the node is not present in the index.
     */
    public int getDoc(IndexReader reader) throws IOException {
        if (doc == -1) {
            TermDocs docs = reader.termDocs(new Term(FieldNames.UUID, id.toString()));
            try {
                if (docs.next()) {
                    return docs.doc();
                } else {
                    throw new IOException("Node with id " + id + " not found in index");
                }
            } finally {
                docs.close();
            }
        } else {
            return doc;
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(id.toString());
        sb.append("(");
        if (doc != -1) {
            sb.append(doc);
        } else {
            sb.append("?");
        }
        sb.append(")");
        return sb.toString();
    }
}
