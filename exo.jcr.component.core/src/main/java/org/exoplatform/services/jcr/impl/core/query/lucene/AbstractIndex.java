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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogMergePolicy;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TieredMergePolicy;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import org.exoplatform.services.jcr.impl.core.query.IndexerIoMode;
import org.exoplatform.services.jcr.impl.core.query.IndexerIoModeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.BitSet;

/**
 * Implements common functionality for a lucene index.
 * <p/>
 * Note on synchronization: This class is not entirely thread-safe. Certain
 * concurrent access is however allowed. Read-only access on this index using
 * {@link #getReadOnlyIndexReader()} is thread-safe. That is, multiple threads
 * my call that method concurrently and use the returned IndexReader at the same
 * time.<br/>
 * Modifying threads must be synchronized externally in a way that only one
 * thread is using the returned IndexReader and IndexWriter instances returned
 * by {@link #getIndexReader()} and {@link #getIndexWriter()} at a time.<br/>
 * Concurrent access by <b>one</b> modifying thread and multiple read-only
 * threads is safe!
 */
abstract class AbstractIndex
{

   /** The logger instance for this class */
   private static final Logger log = LoggerFactory.getLogger("exo.jcr.component.core.AbstractIndex");

   /** PrintStream that pipes all calls to println(String) into log.info() */
   private static final LoggingPrintStream STREAM_LOGGER = new LoggingPrintStream();

   /** The currently set IndexWriter or <code>null</code> if none is set */
   private IndexWriter indexWriter;

   /** The currently set IndexReader or <code>null</code> if none is set */
   private CommittableIndexReader indexReader;

   /** The underlying Directory where the index is stored */
   private Directory directory;

   /** Analyzer we use to tokenize text */
   private Analyzer analyzer;

   /** The similarity in use for indexing and searching. */
   private final Similarity similarity;

   /** Compound file flag */
   private boolean useCompoundFile = true;

   /** termInfosIndexDivisor config parameter */
   private int termInfosIndexDivisor = SearchIndex.DEFAULT_TERM_INFOS_INDEX_DIVISOR;

   /**
    * The document number cache if this index may use one.
    */
   private DocNumberCache cache;

   /** The shared IndexReader for all read-only IndexReaders */
   private SharedIndexReader sharedReader;

   /**
    * The most recent read-only reader if there is any.
    */
   private ReadOnlyIndexReader readOnlyReader;

   /**
    * Flag that indicates whether there was an index present in the directory
    * when this AbstractIndex was created.
    */
   private boolean isExisting;

   protected final IndexerIoModeHandler modeHandler;

   /**
    * Constructs an index with an <code>analyzer</code> and a
    * <code>directory</code>.
    *
    * @param analyzer      the analyzer for text tokenizing.
    * @param similarity    the similarity implementation.
    * @param directory     the underlying directory.
    * @param cache         the document number cache if this index should use
    *                      one; otherwise <code>cache</code> is
    *                      <code>null</code>.
    * @throws IOException if the index cannot be initialized.
    */
   AbstractIndex(final Analyzer analyzer, Similarity similarity, final Directory directory, DocNumberCache cache,
      IndexerIoModeHandler modeHandler) throws IOException
   {
      this.analyzer = analyzer;
      this.similarity = similarity;
      this.directory = directory;
      this.cache = cache;
      this.modeHandler = modeHandler;

      AbstractIndex.this.isExisting = IndexReader.indexExists(directory);

      if (!isExisting)
      {
         IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_36, analyzer);
         indexWriter = new IndexWriter(directory, config);
         // immediately close, now that index has been created
         indexWriter.close();
         indexWriter = null;
      }
   }

   /**
    * Default implementation returns the same instance as passed
    * in the constructor.
    *
    * @return the directory instance passed in the constructor
    */
   Directory getDirectory()
   {
      return directory;
   }

   /**
    * Returns <code>true</code> if this index was opened on a directory with
    * an existing index in it; <code>false</code> otherwise.
    *
    * @return <code>true</code> if there was an index present when this index
    *          was created; <code>false</code> otherwise.
    */
   boolean isExisting()
   {
      return isExisting;
   }

   /**
    * Adds documents to this index and invalidates the shared reader.
    *
    * @param docs the documents to add.
    * @throws IOException if an error occurs while writing to the index.
    */
   void addDocuments(final Document[] docs) throws IOException
   {
      final IndexWriter writer = getIndexWriter();

      IOException ioExc = null;
      try
      {
         for (Document doc : docs)
         {
            try
            {
               writer.addDocument(doc);
            }
            catch (Throwable e) //NOSONAR
            {
               if (ioExc == null)
               {
                  if (e instanceof IOException)
                  {
                     ioExc = (IOException)e;
                  }
                  else
                  {
                     ioExc = Util.createIOException(e);
                  }
               }

               log.warn("Exception while inverting document", e);
            }
         }
      }
      finally
      {
         invalidateSharedReader();
      }

      if (ioExc != null)
      {
         throw ioExc;
      }
   }

   /**
    * Removes the document from this index. This call will not invalidate
    * the shared reader. If a subclass wishes to do so, it should overwrite
    * this method and call {@link #invalidateSharedReader()}.
    *
    * @param idTerm the id term of the document to remove.
    * @throws IOException if an error occurs while removing the document.
    * @return number of documents deleted
    */
   int removeDocument(final Term idTerm) throws IOException
   {
      return getIndexReader().deleteDocuments(idTerm);
   }

   /**
    * Returns an <code>IndexReader</code> on this index. This index reader
    * may be used to delete documents.
    *
    * @return an <code>IndexReader</code> on this index.
    * @throws IOException if the reader cannot be obtained.
    */
   protected synchronized CommittableIndexReader getIndexReader() throws IOException
   {
      if (indexWriter != null)
      {
         indexWriter.close();
         log.debug("closing IndexWriter.");
         indexWriter = null;
      }

      if (indexReader == null || !indexReader.isCurrent())
      {
         IndexReader reader = IndexReader.open(getDirectory(), null, false, termInfosIndexDivisor);
         // if modeHandler != null and mode==READ_ONLY, then reader should be with transient deletions.
         // This is used to transiently update reader in clustered environment when some documents have 
         // been deleted. If index reader not null and already contains some transient deletions, but it
         // is no more current, it will be re-created loosing deletions. They will already be applied by
         // coordinator node in the cluster. And there is no need to inject them into the new reader  

         indexReader =
            new CommittableIndexReader(reader, modeHandler != null && modeHandler.getMode() == IndexerIoMode.READ_ONLY);
      }
      return indexReader;
   }

   /**
    * Returns a read-only index reader, that can be used concurrently with
    * other threads writing to this index. The returned index reader is
    * read-only, that is, any attempt to delete a document from the index
    * will throw an <code>UnsupportedOperationException</code>.
    *
    * @param initCache if the caches in the index reader should be initialized
    *          before the index reader is returned.
    * @return a read-only index reader.
    * @throws IOException if an error occurs while obtaining the index reader.
    */
   synchronized ReadOnlyIndexReader getReadOnlyIndexReader(final boolean initCache) throws IOException
   {
      // get current modifiable index reader
      CommittableIndexReader modifiableReader = getIndexReader();
      long modCount = modifiableReader.getModificationCount();
      if (readOnlyReader != null)
      {
         if (readOnlyReader.getDeletedDocsVersion() == modCount)
         {
            // reader up-to-date
            readOnlyReader.acquire();
            return readOnlyReader;
         }
         else
         {
            // reader outdated
            if (readOnlyReader.getRefCounter() == 1)
            {
               // not in use, except by this index
               // update the reader
               readOnlyReader.updateDeletedDocs(modifiableReader);
               readOnlyReader.acquire();
               return readOnlyReader;
            }
            else
            {
               // cannot update reader, it is still in use
               // need to create a new instance
               readOnlyReader.release();
               readOnlyReader = null;
            }
         }
      }
      // if we get here there is no up-to-date read-only reader
      // capture snapshot of deleted documents
      BitSet deleted = new BitSet(modifiableReader.maxDoc());
      for (int i = 0; i < modifiableReader.maxDoc(); i++)
      {
         if (modifiableReader.isDeleted(i))
         {
            deleted.set(i);
         }
      }
      if (sharedReader == null)
      {
         // create new shared reader
         IndexReader reader = IndexReader.open(getDirectory(), termInfosIndexDivisor);
         CachingIndexReader cr = new CachingIndexReader(reader, cache, initCache);
         sharedReader = new SharedIndexReader(cr);
      }
      readOnlyReader = new ReadOnlyIndexReader(sharedReader, deleted, modCount);
      readOnlyReader.acquire();
      return readOnlyReader;
   }

   /**
    * Returns a read-only index reader, that can be used concurrently with
    * other threads writing to this index. The returned index reader is
    * read-only, that is, any attempt to delete a document from the index
    * will throw an <code>UnsupportedOperationException</code>.
    *
    * @return a read-only index reader.
    * @throws IOException if an error occurs while obtaining the index reader.
    */
   protected ReadOnlyIndexReader getReadOnlyIndexReader() throws IOException
   {
      return getReadOnlyIndexReader(false);
   }

   /**
    * Returns an <code>IndexWriter</code> on this index.
    * @return an <code>IndexWriter</code> on this index.
    * @throws IOException if the writer cannot be obtained.
    */
   protected synchronized IndexWriter getIndexWriter() throws IOException
   {
      if (indexReader != null)
      {
         indexReader.close();
         log.debug("closing IndexReader.");
         indexReader = null;
      }
      if (indexWriter == null)
      {
         IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_36, analyzer);
         config.setSimilarity(similarity);
         if (config.getMergePolicy() instanceof LogMergePolicy)
         {
            ((LogMergePolicy)config.getMergePolicy()).setUseCompoundFile(useCompoundFile);
         }
         else if (config.getMergePolicy() instanceof TieredMergePolicy)
         {
            ((TieredMergePolicy)config.getMergePolicy()).setUseCompoundFile(useCompoundFile);
         }
         else
         {
            log.error("Can't set \"UseCompoundFile\". Merge policy is not an instance of LogMergePolicy. ");
         }
         indexWriter = new IndexWriter(directory, config);
         setUseCompoundFile(useCompoundFile);
         indexWriter.setInfoStream(STREAM_LOGGER);
      }
      return indexWriter;
   }

   /**
    * Commits all pending changes to the underlying <code>Directory</code>.
    * @throws IOException if an error occurs while commiting changes.
    */
   protected void commit() throws IOException
   {
      commit(false);
   }

   /**
    * Commits all pending changes to the underlying <code>Directory</code>.
    *
    * @param optimize if <code>true</code> the index is optimized after the
    *                 commit.
    * @throws IOException if an error occurs while commiting changes.
    */
   protected synchronized void commit(final boolean optimize) throws IOException
   {
      if (indexReader != null)
      {
         log.debug("committing IndexReader.");
         indexReader.flush();
      }
      if (indexWriter != null)
      {
         log.debug("committing IndexWriter.");
         indexWriter.commit();
      }
      // optimize if requested
      if (optimize)
      {
         IndexWriter writer = getIndexWriter();
         writer.forceMerge(1, true);
         writer.close();
         indexWriter = null;
      }
   }

   /**
    * Closes this index, releasing all held resources.
    */
   synchronized void close()
   {
      releaseWriterAndReaders();
      if (directory != null)
      {
         try
         {
            directory.close();
         }
         catch (IOException e)
         {
            directory = null;
         }
      }
   }

   /**
    * Releases all potentially held index writer and readers.
    */
   protected void releaseWriterAndReaders()
   {
      if (indexWriter != null)
      {
         try
         {
            indexWriter.close();
         }
         catch (IOException e)
         {
            log.warn("Exception closing index writer: " + e.toString());
         }
         indexWriter = null;
      }
      if (indexReader != null)
      {
         try
         {
            indexReader.close();
         }
         catch (IOException e)
         {
            log.warn("Exception closing index reader: " + e.toString());
         }
         indexReader = null;
      }
      if (readOnlyReader != null)
      {
         try
         {
            readOnlyReader.release();
         }
         catch (IOException e)
         {
            log.warn("Exception closing index reader: " + e.toString());
         }
         readOnlyReader = null;
      }
      if (sharedReader != null)
      {
         try
         {
            sharedReader.release();
         }
         catch (IOException e)
         {
            log.warn("Exception closing index reader: " + e.toString());
         }
         sharedReader = null;
      }
   }

   /**
    * @return the number of bytes this index occupies in memory.
    */
   synchronized long getRamSizeInBytes()
   {
      if (indexWriter != null)
      {
         return indexWriter.ramSizeInBytes();
      }
      else
      {
         return 0;
      }
   }

   /**
    * Closes the shared reader.
    *
    * @throws IOException if an error occurs while closing the reader.
    */
   protected synchronized void invalidateSharedReader() throws IOException
   {
      // also close the read-only reader
      if (readOnlyReader != null)
      {
         readOnlyReader.release();
         readOnlyReader = null;
      }
      // invalidate shared reader
      if (sharedReader != null)
      {
         sharedReader.release();
         sharedReader = null;
      }
   }

   //-------------------------< properties >-----------------------------------

   /**
    * The lucene index writer property: useCompountFile
    */
   void setUseCompoundFile(boolean b)
   {
      useCompoundFile = b;
      if (indexWriter != null)
      {
         IndexWriterConfig config = indexWriter.getConfig();
         if (config.getMergePolicy() instanceof LogMergePolicy)
         {
            ((LogMergePolicy)config.getMergePolicy()).setUseCompoundFile(useCompoundFile);
            ((LogMergePolicy)config.getMergePolicy()).setNoCFSRatio(1.0);
         }
         else if (config.getMergePolicy() instanceof TieredMergePolicy)
         {
            ((TieredMergePolicy)config.getMergePolicy()).setUseCompoundFile(useCompoundFile);
            ((TieredMergePolicy)config.getMergePolicy()).setNoCFSRatio(1.0);
         }
         else
         {
            log.error("Can't set \"UseCompoundFile\". Merge policy is not an instance of LogMergePolicy. ");
         }
      }
   }

   /**
    * @return the current value for termInfosIndexDivisor.
    */
   public int getTermInfosIndexDivisor()
   {
      return termInfosIndexDivisor;
   }

   /**
    * Sets a new value for termInfosIndexDivisor.
    *
    * @param termInfosIndexDivisor the new value.
    */
   public void setTermInfosIndexDivisor(int termInfosIndexDivisor)
   {
      this.termInfosIndexDivisor = termInfosIndexDivisor;
   }

   //------------------------------< internal >--------------------------------

   /**
    * Adapter to pipe info messages from lucene into log messages.
    */
   private static final class LoggingPrintStream extends PrintStream
   {

      /** Buffer print calls until a newline is written */
      private StringBuffer buffer = new StringBuffer();

      public LoggingPrintStream()
      {
         super(new OutputStream()
         {
            @Override
            public void write(int b)
            {
               // do nothing
            }
         });
      }

      @Override
      public void print(String s)
      {
         buffer.append(s);
      }

      @Override
      public void println(String s)
      {
         buffer.append(s);
         log.debug(buffer.toString());
         buffer.setLength(0);
      }
   }
}
