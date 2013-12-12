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

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.FilterIndexReader;
import org.apache.lucene.index.IndexReader;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Wraps an <code>IndexReader</code> and allows to commit changes without
 * closing the reader.
 */
class CommittableIndexReader extends FilterIndexReader {

    /**
     * A modification count on this index reader. Initialized with
     * {@link IndexReader#getVersion()} and incremented with every call to
     * {@link #doDelete(int)}.
     */
    private final AtomicLong modCount;

    /**
     * If reader is created with flag transientDeletions, then reader 
     * deleted documents are stored in the memory buffer and not applied to underlying
     * index reader.
     */
    private final boolean transientDeletions;

    private final Set<Integer> deletedDocs;

    /**
     * Creates a new <code>CommittableIndexReader</code> based on <code>in</code>.
     *
     * @param in the <code>IndexReader</code> to wrap.
     * @param transientDeletions If reader is created with flag transientDeletions, then reader 
     *        deleted documents are stored in the memory buffer and not applied to underlying
     *        index reader.
     */
    CommittableIndexReader(IndexReader in, boolean transientDeletions) {
        super(in);
        modCount = new AtomicLong(in.getVersion());
        this.transientDeletions = transientDeletions;
        // no need to initialize Set if transientDeletions = false
        this.deletedDocs = transientDeletions? new CopyOnWriteArraySet<Integer>() : null;
    }

    //------------------------< FilterIndexReader >-----------------------------

    /**
     * {@inheritDoc}
     * <p/>
     * Increments the modification count.
     */
    protected void doDelete(int n) throws CorruptIndexException, IOException {
        super.doDelete(n);
        modCount.incrementAndGet();
    }

    // TODO: TO FIX
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public void deleteDocument(int docNum) throws StaleReaderException, CorruptIndexException,
//       LockObtainFailedException, IOException {
//       // skip acquiring write lock
//       if (transientDeletions)
//       {
//           deletedDocs.add(docNum);
//           modCount.incrementAndGet(); // doDelete won't be executed, so incrementing modCount
//       }
//       else
//       {
//           super.deleteDocument(docNum);
//       }
//    }

    @Override
    public boolean isDeleted(int n) {
       if (transientDeletions)
       {
           return deletedDocs.contains(n);
       }
       else
       {
           return super.isDeleted(n);
       }
    }

    //------------------------< additional methods >----------------------------

    /**
     * @return the modification count of this index reader.
     */
    long getModificationCount() {
        return modCount.get();
    }
}
