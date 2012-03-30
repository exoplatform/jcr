/*
 * Copyright (C) 2011 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.services.jcr.impl.core.query.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Similarity;
import org.exoplatform.services.jcr.impl.core.query.IndexerIoModeHandler;
import org.exoplatform.services.jcr.impl.core.query.lucene.directory.DirectoryManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class extends {@link PersistentIndex} and designed to be used while Index is not yet started
 * due to long running jobs, but since it is launched in clustered environment some concurrent 
 * repository operations can be performed (add, delete).   
 * 
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: OfflinePersistentIndex.java 34360 2009-07-22 23:58:59Z nzamosenchuk $
 *
 */
public class OfflinePersistentIndex extends PersistentIndex
{
   public static final String NAME = "offline";

   private List<String> processedIDs;

   /**
    * Creates a new <code>OfflinePersistentIndex</code>.
    *
    * @param analyzer the analyzer for text tokenizing.
    * @param similarity the similarity implementation.
    * @param cache the document number cache
    * @param directoryManager the directory manager.
    * @throws IOException if an error occurs while opening / creating the
    *  index.
    */
   OfflinePersistentIndex(Analyzer analyzer, Similarity similarity, DocNumberCache cache,
      DirectoryManager directoryManager, IndexerIoModeHandler modeHandler) throws IOException
   {
      super(NAME, analyzer, similarity, cache, directoryManager, modeHandler);
      this.processedIDs = new ArrayList<String>();
   }

   @Override
   int getNumDocuments() throws IOException
   {
      return super.getNumDocuments();
   }

   @Override
   int removeDocument(Term idTerm) throws IOException
   {
      int count = super.removeDocument(idTerm);
      processedIDs.add(idTerm.text());
      return count;
   }

   @Override
   void addDocuments(Document[] docs) throws IOException
   {
      super.addDocuments(docs);
      for (Document doc : docs)
      {
         processedIDs.add(doc.get(FieldNames.UUID));
      }
   }

   @Override
   synchronized void close()
   {
      processedIDs.clear();
      super.close();
   }

   /**
    * @return the list of UUIDs that where processed by this index. They are both added and removed nodes.
    */
   public List<String> getProcessedIDs()
   {
      return Collections.unmodifiableList(processedIDs);
   }

}
