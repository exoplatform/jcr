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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;

/**
 * Extends a <code>MultiReader</code> with support for cached <code>TermDocs</code>
 * on {@link FieldNames#UUID} field.
 */
public final class CachingMultiIndexReader
        extends MultiReader
        implements HierarchyResolver, MultiIndexReader {

    /**
     * The sub readers.
     */
    private ReadOnlyIndexReader[] subReaders;

    /**
     * Map of {@link OffsetReader}s, identified by creation tick.
     */
    private final Map readersByCreationTick = new HashMap();

    /**
     * Document number cache if available. May be <code>null</code>.
     */
    private final DocNumberCache cache;

    /**
     * Doc number starts for each sub reader
     */
    private int[] starts;

    /**
     * Reference count. Every time close is called refCount is decremented. If
     * refCount drops to zero the underlying readers are closed as well.
     */
    private int refCount = 1;

    /**
     * Creates a new <code>CachingMultiIndexReader</code> based on sub readers.
     *
     * @param subReaders the sub readers.
     * @param cache the document number cache.
     */
    public CachingMultiIndexReader(ReadOnlyIndexReader[] subReaders,
                                   DocNumberCache cache) {
        super(subReaders);
        this.cache = cache;
        this.subReaders = subReaders;
        starts = new int[subReaders.length + 1];
        int maxDoc = 0;
        for (int i = 0; i < subReaders.length; i++) {
            starts[i] = maxDoc;
            maxDoc += subReaders[i].maxDoc();
            OffsetReader offsetReader = new OffsetReader(subReaders[i], starts[i]);
            readersByCreationTick.put(new Long(subReaders[i].getCreationTick()), offsetReader);
        }
        starts[subReaders.length] = maxDoc;
    }

    /**
     * {@inheritDoc}
     */
    public int[] getParents(int n, int[] docNumbers) throws IOException {
        DocId id = getParentDocId(n);
        return id.getDocumentNumbers(this, docNumbers);
    }

    /**
     * Returns the DocId of the parent of <code>n</code> or {@link DocId#NULL}
     * if <code>n</code> does not have a parent (<code>n</code> is the root
     * node).
     *
     * @param n the document number.
     * @return the DocId of <code>n</code>'s parent.
     * @throws IOException if an error occurs while reading from the index.
     */
    public DocId getParentDocId(int n) throws IOException {
        int i = readerIndex(n);
        DocId result = DocId.NULL;
        if(i >= 0 && starts[i] >= 0 && (n - starts[i]) >= 0) {
           DocId id = subReaders[i].getParent(n - starts[i]);
           result  =  id.applyOffset(starts[i]);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public TermDocs termDocs(Term term) throws IOException {
        if (term!=null && term.field() == FieldNames.UUID) {
            // check cache
            DocNumberCache.Entry e = cache.get(term.text());
            if (e != null) {
                // check if valid:
                // 1) reader must be in the set of readers
                // 2) doc must not be deleted
                OffsetReader offsetReader = (OffsetReader) readersByCreationTick.get(
                        new Long(e.creationTick));
                if (offsetReader != null && !offsetReader.reader.isDeleted(e.doc)) {
                    return new SingleTermDocs(e.doc + offsetReader.offset);
                }
            }

            // if we get here, entry is either invalid or did not exist
            // search through readers
            for (int i = 0; i < subReaders.length; i++) {
                TermDocs docs = subReaders[i].termDocs(term);
                try {
                    if (docs.next()) {
                        return new SingleTermDocs(docs.doc() + starts[i]);
                    }
                } finally {
                    docs.close();
                }
            }
        }

        return super.termDocs(term);
    }

    /**
     * Increments the reference count of this reader. Each call to this method
     * must later be acknowledged by a call to {@link #release()}.
     */
    synchronized void acquire() {
        refCount++;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized final void release() throws IOException {
        if (--refCount == 0) {
            close();
        }
    }

    /**
     * {@inheritDoc}
     */
    protected synchronized void doClose() throws IOException {
        for (int i = 0; i < subReaders.length; i++) {
            subReaders[i].release();
        }
       subReaders = null;
       readersByCreationTick.clear();
    }

    //-------------------------< MultiIndexReader >-----------------------------

    /**
     * {@inheritDoc}
     */
    public IndexReader[] getIndexReaders() {
        IndexReader[] readers = new IndexReader[subReaders.length];
        System.arraycopy(subReaders, 0, readers, 0, subReaders.length);
        return readers;
    }

    /**
     * {@inheritDoc}
     */
    public ForeignSegmentDocId createDocId(String uuid) throws IOException {
        Term id = new Term(FieldNames.UUID,uuid);
        int doc;
        long tick;
        for (int i = 0; i < subReaders.length; i++) {
            TermDocs docs = subReaders[i].termDocs(id);
            try {
                if (docs.next()) {
                    doc = docs.doc();
                    tick = subReaders[i].getCreationTick();
                    return new ForeignSegmentDocId(doc, tick);
                }
            } finally {
                docs.close();
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public int getDocumentNumber(ForeignSegmentDocId docId) {
        OffsetReader r = (OffsetReader) readersByCreationTick.get(
                new Long(docId.getCreationTick()));
        if (r != null && !r.reader.isDeleted(docId.getDocNumber())) {
            return r.offset + docId.getDocNumber();
        }
        return -1;
    }

    /**
     * Returns the reader index for document <code>n</code>.
     * Implementation copied from lucene MultiReader class.
     *
     * @param n document number.
     * @return the reader index.
     */
    private int readerIndex(int n) {
        int lo = 0;                                      // search starts array
        int hi = subReaders.length - 1;                  // for first element less

        while (hi >= lo) {
            int mid = (lo + hi) >> 1;
            int midValue = starts[mid];
            if (n < midValue) {
                hi = mid - 1;
            } else if (n > midValue) {
                lo = mid + 1;
            } else {                                      // found a match
                while (mid + 1 < subReaders.length && starts[mid + 1] == midValue) {
                    mid++;                                  // scan to last match
                }
                return mid;
            }
        }
        return hi;
    }

    //-----------------------< OffsetTermDocs >---------------------------------

    /**
     * Simple helper struct that associates an offset with an IndexReader.
     */
    private static final class OffsetReader {

        /**
         * The index reader.
         */
        private final ReadOnlyIndexReader reader;

        /**
         * The reader offset in this multi reader instance.
         */
        private final int offset;

        /**
         * Creates a new <code>OffsetReader</code>.
         *
         * @param reader the index reader.
         * @param offset the reader offset in a multi reader.
         */
        OffsetReader(ReadOnlyIndexReader reader, int offset) {
            this.reader = reader;
            this.offset = offset;
        }
    }
}
