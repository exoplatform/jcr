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

import org.apache.commons.collections.map.LRUMap;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.FilterIndexReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Implements an <code>IndexReader</code> that maintains caches to resolve
 * {@link #getParent(int, BitSet)} calls efficiently.
 * <br>
 */
class CachingIndexReader extends FilterIndexReader
{

   /**
    * The logger instance for this class.
    */
   private static final Logger log = LoggerFactory.getLogger("exo.jcr.component.core.CachingIndexReader");

   /**
    * The current value of the global creation tick counter.
    */
   private static long currentTick;

   /**
    * BitSet where bits that correspond to document numbers are set for
    * sharable nodes.
    */
   private final BitSet shareableNodes;

   /**
    * Cache of nodes parent relation. If an entry in the array is not null,
    * that means the node with the document number = array-index has the node
    * with <code>DocId</code> as parent.
    */
   private final DocId[] parents;

   /**
    * Initializes the {@link #parents} cache.
    */
   private CacheInitializer cacheInitializer;

   /**
    * Tick when this index reader was created.
    */
   private final long creationTick = getNextCreationTick();

   /**
    * Document number cache if available. May be <code>null</code>.
    */
   private final DocNumberCache cache;

   /**
    * Maps document number to node UUID.
    */
   private final Map<Integer, String> docNumber2uuid;

   /**
    * A cache of TermDocs that are regularly read from the index.
    */
   private final TermDocsCache termDocsCache;

   /**
    * Creates a new <code>CachingIndexReader</code> based on
    * <code>delegate</code>
    *
    * @param delegatee the base <code>IndexReader</code>.
    * @param cache     a document number cache, or <code>null</code> if not
    *                  available to this reader.
    * @param initCache if the {@link #parents} cache should be initialized
    *                  when this index reader is constructed.
    * @throws IOException if an error occurs while reading from the index.
    */
   @SuppressWarnings("unchecked")
   CachingIndexReader(IndexReader delegatee, DocNumberCache cache, boolean initCache) throws IOException
   {
      super(delegatee);
      this.cache = cache;
      this.parents = new DocId[delegatee.maxDoc()];
      this.shareableNodes = new BitSet();
      TermDocs tDocs = delegatee.termDocs(new Term(FieldNames.SHAREABLE_NODE, ""));
      try
      {
         while (tDocs.next())
         {
            shareableNodes.set(tDocs.doc());
         }
      }
      finally
      {
         tDocs.close();
      }
      this.cacheInitializer = new CacheInitializer(delegatee);
      if (initCache)
      {
         cacheInitializer.run();
      }
      // limit cache to 1% of maxDoc(), but at least 10.
      this.docNumber2uuid =
         (Map<Integer, String>)Collections.synchronizedMap(new LRUMap(Math.max(10, delegatee.maxDoc() / 100)));
      this.termDocsCache = new TermDocsCache(delegatee, FieldNames.PROPERTIES);
   }

   /**
    * Returns the <code>DocId</code> of the parent of <code>n</code> or
    * {@link DocId#NULL} if <code>n</code> does not have a parent
    * (<code>n</code> is the root node).
    *
    * @param n the document number.
    * @param deleted the documents that should be regarded as deleted.
    * @return the <code>DocId</code> of <code>n</code>'s parent.
    * @throws IOException if an error occurs while reading from the index.
    */
   DocId getParent(int n, BitSet deleted) throws IOException
   {
      DocId parent;
      boolean existing = false;
      parent = parents[n];

      if (parent != null)
      {
         existing = true;

         // check if valid and reset if necessary
         if (!parent.isValid(deleted))
         {
            if (log.isDebugEnabled())
            {
               log.debug(parent + " not valid anymore.");
            }
            parent = null;
         }
      }

      if (parent == null)
      {
         Document doc = document(n, FieldSelectors.UUID_AND_PARENT);
         String[] parentUUIDs = doc.getValues(FieldNames.PARENT);
         if (parentUUIDs.length == 0 || parentUUIDs[0].length() == 0)
         {
            // root node
            parent = DocId.NULL;
         }
         else
         {
            if (shareableNodes.get(n))
            {
               parent = DocId.create(parentUUIDs);
            }
            else
            {
               if (!existing)
               {
                  Term id = new Term(FieldNames.UUID, parentUUIDs[0]);
                  TermDocs docs = termDocs(id);
                  try
                  {
                     while (docs.next())
                     {
                        if (!deleted.get(docs.doc()))
                        {
                           parent = DocId.create(docs.doc());
                           break;
                        }
                     }
                  }
                  finally
                  {
                     docs.close();
                  }
               }
               // if still null, then parent is not in this index, or existing
               // DocId was invalid. thus, only allowed to create DocId from uuid
               if (parent == null)
               {
                  parent = DocId.create(parentUUIDs[0]);
               }
            }
         }

         // finally put to cache
         parents[n] = parent;
      }
      return parent;
   }

   /**
    * Returns the tick value when this reader was created.
    *
    * @return the creation tick for this reader.
    */
   public long getCreationTick()
   {
      return creationTick;
   }

   //--------------------< FilterIndexReader overwrites >----------------------

   /**
    * Uses the {@link #docNumber2uuid} cache for document lookups that are only
    * interested in the {@link FieldSelectors#UUID}.
    *
    * @param n the document number.
    * @param fieldSelector the field selector.
    * @return the document.
    * @throws CorruptIndexException if the index is corrupt.
    * @throws IOException if an error occurs while reading from the index.
    */
   public Document document(int n, FieldSelector fieldSelector) throws CorruptIndexException, IOException
   {
      if (fieldSelector == FieldSelectors.UUID)
      {
         Integer docNum = new Integer(n);
         Document doc;
         String uuid = docNumber2uuid.get(docNum);
         if (uuid == null)
         {
            doc = super.document(n, fieldSelector);
            uuid = doc.get(FieldNames.UUID);
            docNumber2uuid.put(docNum, uuid);
         }
         else
         {
            doc = new Document();
            doc.add(new Field(FieldNames.UUID, uuid.toString(), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
         }
         return doc;
      }
      else
      {
         return super.document(n, fieldSelector);
      }
   }

   /**
    * If the field of <code>term</code> is {@link FieldNames#UUID} this
    * <code>CachingIndexReader</code> returns a <code>TermDocs</code> instance
    * with a cached document id. If <code>term</code> has any other field
    * the call is delegated to the base <code>IndexReader</code>.<br>
    * If <code>term</code> is for a {@link FieldNames#UUID} field and this
    * <code>CachingIndexReader</code> does not have such a document,
    * {@link EmptyTermDocs#INSTANCE} is returned.
    *
    * @param term the term to start the <code>TermDocs</code> enumeration.
    * @return a TermDocs instance.
    * @throws IOException if an error occurs while reading from the index.
    */
   public TermDocs termDocs(Term term) throws IOException
   {
      if (term!=null && term.field() == FieldNames.UUID)
      {
         // check cache if we have one
         if (cache != null)
         {
            DocNumberCache.Entry e = cache.get(term.text());
            if (e != null)
            {
               // check if valid
               // the cache may contain entries from a different reader
               // with the same uuid. that happens when a node is updated
               // and is reindexed. the node 'travels' from an older index
               // to a newer one. the cache will still contain a cache
               // entry from the old until it is overwritten by the
               // newer index.
               if (e.creationTick == creationTick && !isDeleted(e.doc))
               {
                  return new SingleTermDocs(e.doc);
               }
            }

            // not in cache or invalid
            TermDocs docs = in.termDocs(term);
            try
            {
               if (docs.next())
               {
                  // put to cache
                  cache.put(term.text(), this, docs.doc());
                  // and return
                  return new SingleTermDocs(docs.doc());
               }
               else
               {
                  return EmptyTermDocs.INSTANCE;
               }
            }
            finally
            {
               docs.close();
            }
         }
      }
      return termDocsCache.termDocs(term);
   }

   /**
    * {@inheritDoc}
    */
   protected void doClose() throws IOException
   {
      try
      {
         cacheInitializer.waitUntilStopped();
      }
      catch (InterruptedException e)
      {
         // ignore
      }
      super.doClose();
   }

   //----------------------< internal >----------------------------------------

   /**
    * Returns the next creation tick value.
    *
    * @return the next creation tick value.
    */
   private static long getNextCreationTick()
   {
      synchronized (CachingIndexReader.class)
      {
         return currentTick++;
      }
   }

   /**
    * Initializes the {@link CachingIndexReader#parents} cache.
    */
   private class CacheInitializer implements Runnable
   {

      /**
       * From where to read.
       */
      private final IndexReader reader;

      /**
       * Set to <code>true</code> while this initializer does its work.
       */
      private boolean running = false;

      /**
       * Set to <code>true</code> when this index reader is about to be closed.
       */
      private volatile boolean stopRequested = false;

      /**
       * Creates a new initializer with the given <code>reader</code>.
       *
       * @param reader an index reader.
       */
      public CacheInitializer(IndexReader reader)
      {
         this.reader = reader;
      }

      /**
       * Initializes the cache.
       */
      public void run()
      {
         synchronized (this)
         {
            running = true;
         }
         try
         {
            if (stopRequested)
            {
               // immediately return when stop is requested
               return;
            }
            initializeParents(reader);
         }
         catch (IOException e)
         {
            // only log warn message during regular operation
            if (!stopRequested)
            {
               log.warn("Error initializing parents cache.", e);
            }
         }
         finally
         {
            synchronized (this)
            {
               running = false;
               notifyAll();
            }
         }
      }

      /**
       * Waits until this cache initializer is stopped.
       *
       * @throws InterruptedException if the current thread is interrupted.
       */
      public void waitUntilStopped() throws InterruptedException
      {
         stopRequested = true;
         synchronized (this)
         {
            while (running)
            {
               wait();
            }
         }
      }

      /**
       * Initializes the {@link CachingIndexReader#parents} <code>DocId</code>
       * array.
       *
       * @param reader the underlying index reader.
       * @throws IOException if an error occurs while reading from the index.
       */
      private void initializeParents(IndexReader reader) throws IOException
      {
         long time = 0;
         if (log.isDebugEnabled())
         {
            time = System.currentTimeMillis();
         }
         final Map<Object, NodeInfo> docs = new HashMap<Object, NodeInfo>();
         // read UUIDs
         collectTermDocs(reader, new Term(FieldNames.UUID, ""), new TermDocsCollector()
         {
            public void collect(Term term, TermDocs tDocs) throws IOException
            {
               String uuid = term.text();
               while (tDocs.next())
               {
                  int doc = tDocs.doc();
                  // skip sharable nodes
                  if (!shareableNodes.get(doc))
                  {
                     NodeInfo info = new NodeInfo(doc, uuid);
                     docs.put(new Integer(doc), info);
                  }
               }
            }
         });

         // read PARENTs
         collectTermDocs(reader, new Term(FieldNames.PARENT, "0"), new TermDocsCollector()
         {
            public void collect(Term term, TermDocs tDocs) throws IOException
            {
               String uuid = term.text();
               while (tDocs.next())
               {
                  Integer docId = new Integer(tDocs.doc());
                  NodeInfo info = docs.get(docId);
                  if (info == null)
                  {
                     // sharable node, see above
                  }
                  else
                  {
                     info.parent = uuid;
                     docs.remove(docId);
                     docs.put(info.uuid, info);
                  }
               }
            }
         });

         if (stopRequested)
         {
            return;
         }

         double foreignParents = 0;
         Iterator<NodeInfo> it = docs.values().iterator();
         while (it.hasNext())
         {
            NodeInfo info = it.next();
            NodeInfo parent = docs.get(info.parent);
            if (parent != null)
            {
               parents[info.docId] = DocId.create(parent.docId);
            }
            else if (info.parent != null)
            {
               foreignParents++;
               parents[info.docId] = DocId.create(info.parent);
            }
            else if (shareableNodes.get(info.docId))
            {
               Document doc = reader.document(info.docId, FieldSelectors.UUID_AND_PARENT);
               parents[info.docId] = DocId.create(doc.getValues(FieldNames.PARENT));
            }
            else
            {
               // no parent -> root node
               parents[info.docId] = DocId.NULL;
            }
         }
         if (log.isDebugEnabled())
         {
            NumberFormat nf = NumberFormat.getPercentInstance();
            nf.setMaximumFractionDigits(1);
            time = System.currentTimeMillis() - time;
            if (parents.length > 0)
            {
               foreignParents /= parents.length;
            }
            log.debug("initialized {} DocIds in {} ms, {} foreign parents", new Object[]{new Integer(parents.length),
               new Long(time), nf.format(foreignParents)});
         }
      }

      /**
       * Collects term docs for a given start term. All terms with the same
       * field as <code>start</code> are enumerated.
       *
       * @param reader the index reader.
       * @param start the term where to start the term enumeration.
       * @param collector collects the term docs for each term.
       * @throws IOException if an error occurs while reading from the index.
       */
      private void collectTermDocs(IndexReader reader, Term start, TermDocsCollector collector) throws IOException
      {
         TermDocs tDocs = reader.termDocs();
         try
         {
            TermEnum terms = reader.terms(start);
            try
            {
               int count = 0;
               do
               {
                  Term t = terms.term();
                  if (t != null && t.field() == start.field())
                  {
                     tDocs.seek(terms);
                     collector.collect(t, tDocs);
                  }
                  else
                  {
                     break;
                  }
                  // once in a while check if we should quit
                  if (++count % 10000 == 0)
                  {
                     if (stopRequested)
                     {
                        break;
                     }
                  }
               }
               while (terms.next());
            }
            finally
            {
               terms.close();
            }
         }
         finally
         {
            tDocs.close();
         }
      }
   }

   /**
    * Simple interface to collect a term and its term docs.
    */
   private interface TermDocsCollector
   {

      /**
       * Called for each term encountered.
       *
       * @param term the term.
       * @param tDocs the term docs of <code>term</code>.
       * @throws IOException if an error occurs while reading from the index.
       */
      void collect(Term term, TermDocs tDocs) throws IOException;
   }

   private final static class NodeInfo
   {

      final int docId;

      final String uuid;

      String parent;

      public NodeInfo(int docId, String uuid)
      {
         this.docId = docId;
         this.uuid = uuid;
      }
   }
}
