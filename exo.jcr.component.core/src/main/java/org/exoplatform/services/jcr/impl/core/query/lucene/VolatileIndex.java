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

import org.apache.commons.collections.map.LinkedMap;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.store.RAMDirectory;

import java.io.IOException;
import java.util.Map;

/**
 * Implements an in-memory index with a pending buffer.
 */
class VolatileIndex extends AbstractIndex
{

   /**
    * Default value for {@link #bufferSize}.
    */
   private static final int DEFAULT_BUFFER_SIZE = 10;

   /**
    * Map of pending documents to add to the index
    */
   @SuppressWarnings("unchecked")
   private final Map<String, Document> pending = new LinkedMap();

   /**
    * Map of pending documents related to AggregatedNodeIndexes
    */
   @SuppressWarnings("unchecked")
   private final Map<String, Document> aggregateIndexes = new LinkedMap();

   /**
    * Number of documents that are buffered before they are added to the index.
    */
   private int bufferSize = DEFAULT_BUFFER_SIZE;

   /**
    * The number of documents in this index.
    */
   private int numDocs = 0;

   /**
    * Creates a new <code>VolatileIndex</code> using an <code>analyzer</code>.
    *
    * @param analyzer the analyzer to use.
    * @param similarity the similarity implementation.
    * @throws IOException if an error occurs while opening the index.
    */
   VolatileIndex(Analyzer analyzer, Similarity similarity) throws IOException
   {
      super(analyzer, similarity, new RAMDirectory(), null, null);
   }

   /**
    * Overwrites the default implementation by adding the documents to a
    * pending list and commits the pending list if needed.
    *
    * @param docs the documents to add to the index.
    * @throws IOException if an error occurs while writing to the index.
    */
   @Override
   void addDocuments(Document[] docs) throws IOException
   {
      for (int i = 0; i < docs.length; i++)
      {
         Document old = pending.put(docs[i].get(FieldNames.UUID), docs[i]);
         if (old != null)
         {
            Util.disposeDocument(old);
         }
         if (pending.size() >= bufferSize)
         {
            commitPending();
         }
         numDocs++;
      }
      invalidateSharedReader();
   }

   /***
    * @param uuid : the uuid term of the document.
    * @return document of the specific uuid
    */
   Document getAggregateIndexes(String uuid)
   {
      return aggregateIndexes.get(uuid);
   }

   /***
    * @param doc : the document to add to the temp  map aggregateIndexes.
    * @return
    */
   void addAggregateIndexes(Document doc)
   {
      Document old=aggregateIndexes.put(doc.get(FieldNames.UUID), doc);
      if (old != null)
      {
         Util.disposeDocument(old);
      }
   }


   /**
    * Overwrites the default implementation to remove the document from the
    * pending list if it is present or simply calls <code>super.removeDocument()</code>.
    *
    * @param idTerm the uuid term of the document to remove.
    * @return the number of deleted documents
    * @throws IOException if an error occurs while removing the document from
    *                     the index.
    */
   @Override
   int removeDocument(Term idTerm) throws IOException
   {
      Document doc = pending.remove(idTerm.text());
      int num;
      if (doc != null)
      {
         Util.disposeDocument(doc);
         // pending document has been removed
         num = 1;
      }
      else
      {
         // remove document from index
         num = super.getIndexReader().deleteDocuments(idTerm);
      }
      numDocs -= num;
      return num;
   }

   /**
    * Returns the number of valid documents in this index.
    *
    * @return the number of valid documents in this index.
    */
   int getNumDocuments()
   {
      return numDocs;
   }

   /**
    * Overwrites the implementation in {@link AbstractIndex} to trigger
    * commit of pending documents to index.
    * @return the index reader for this index.
    * @throws IOException if an error occurs building a reader.
    */
   @Override
   protected synchronized CommittableIndexReader getIndexReader() throws IOException
   {
      commitPending();
      return super.getIndexReader();
   }

   /**
    * Overwrites the implementation in {@link AbstractIndex} to commit
    * pending documents.
    * @param optimize if <code>true</code> the index is optimized after the
    *                 commit.
    */
   @Override
   protected synchronized void commit(boolean optimize) throws IOException
   {
      commitPending();
      super.commit(optimize);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   long getRamSizeInBytes()
   {
      return super.getRamSizeInBytes() + ((RAMDirectory)getDirectory()).sizeInBytes();
   }

   /**
    * Sets a new buffer size for pending documents to add to the index.
    * Higher values consume more memory, but help to avoid multiple index
    * cycles when a node is changed / saved multiple times.
    *
    * @param size the new buffer size.
    */
   void setBufferSize(int size)
   {
      bufferSize = size;
   }

   /**
    * Commits pending documents to the index.
    *
    * @throws IOException if committing pending documents fails.
    */
   private void commitPending() throws IOException
   {
      if (pending.isEmpty())
      {
         return;
      }
      super.addDocuments((Document[])pending.values().toArray(new Document[pending.size()]));
      pending.clear();
      aggregateIndexes.clear();
   }
}
