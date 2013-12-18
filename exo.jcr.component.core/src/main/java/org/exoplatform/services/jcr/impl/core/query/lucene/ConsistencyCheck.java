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

import org.apache.lucene.document.Document;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.RepositoryException;

/**
 * Implements a consistency check on the search index. Currently the following
 * checks are implemented:
 * <ul>
 * <li>Does not node exist in the ItemStateManager? If it does not exist
 * anymore the node is deleted from the index.</li>
 * <li>Is the parent of a node also present in the index? If it is not present it
 * will be indexed.</li>
 * <li>Is a node indexed multiple times? If that is the case, all occurrences
 * in the index for such a node are removed, and the node is re-indexed.</li>
 * </ul>
 */
class ConsistencyCheck
{

   /**
    * Logger instance for this class
    */
   private static final Logger log = LoggerFactory.getLogger("exo.jcr.component.core.ConsistencyCheck");

   /**
    * The ItemStateManager of the workspace.
    */
   private final ItemDataConsumer stateMgr;

   /**
    * The index to check.
    */
   private final MultiIndex index;

   /**
    * All the document UUIDs within the index.
    */
   private Set<String> documentUUIDs;

   /**
    * List of all errors.
    */
   private final List<ConsistencyCheckError> errors = new ArrayList<ConsistencyCheckError>();

   /**
    * Private constructor.
    */
   private ConsistencyCheck(MultiIndex index, ItemDataConsumer mgr)
   {
      this.index = index;
      this.stateMgr = mgr;
   }

   /**
    * Runs the consistency check on <code>index</code>.
    *
    * @param index the index to check.
    * @param mgr   the ItemStateManager from where to load content.
    * @return the consistency check with the results.
    * @throws IOException if an error occurs while checking.
   * @throws RepositoryException 
    */
   static ConsistencyCheck run(MultiIndex index, ItemDataConsumer mgr) throws IOException, RepositoryException
   {
      ConsistencyCheck check = new ConsistencyCheck(index, mgr);
      check.run();
      return check;
   }

   /**
    * Repairs detected errors during the consistency check.
    * @param ignoreFailure if <code>true</code> repair failures are ignored,
    *   the repair continues without throwing an exception. If
    *   <code>false</code> the repair procedure is aborted on the first
    *   repair failure.
    * @throws IOException if a repair failure occurs.
    */
   void repair(boolean ignoreFailure) throws IOException
   {
      if (errors.size() == 0)
      {
         log.info("No errors found.");
         return;
      }
      int notRepairable = 0;
      for (Iterator<ConsistencyCheckError> it = errors.iterator(); it.hasNext();)
      {
         final ConsistencyCheckError error = it.next();
         try
         {
            if (error.repairable())
            {
               // running in privileged mode
               error.repair();
            }
            else
            {
               log.warn("Not repairable: " + error);
               notRepairable++;
            }
         }
         catch (IOException e)
         {
            if (ignoreFailure)
            {
               log.warn("Exception while reparing: " + e);
            }
            else
            {
               throw e;
            }
         }
         catch (Exception e)
         {
            if (ignoreFailure)
            {
               log.warn("Exception while reparing: " + e);
            }
            else
            {
               throw new IOException(e.getMessage(), e);
            }
         }
      }
      log.info("Repaired " + (errors.size() - notRepairable) + " errors.");
      if (notRepairable > 0)
      {
         log.warn("" + notRepairable + " error(s) not repairable.");
      }
   }

   /**
    * Returns the errors detected by the consistency check.
    * @return the errors detected by the consistency check.
    */
   List<ConsistencyCheckError> getErrors()
   {
      return new ArrayList<ConsistencyCheckError>(errors);
   }

   /**
    * Runs the consistency check.
    * @throws IOException if an error occurs while running the check.
   * @throws RepositoryException 
    */
   private void run() throws IOException, RepositoryException
   {
      // UUIDs of multiple nodes in the index
      Set<String> multipleEntries = new HashSet<String>();
      // collect all documents UUIDs
      documentUUIDs = new HashSet<String>();
      CachingMultiIndexReader reader = index.getIndexReader();
      try
      {
         for (int i = 0; i < reader.maxDoc(); i++)
         {
            if (i > 10 && i % (reader.maxDoc() / 5) == 0)
            {
               long progress = Math.round((100.0 * i) / (reader.maxDoc() * 2f));
               log.info("progress: " + progress + "%");
            }
            if (reader.isDeleted(i))
            {
               continue;
            }
            final int currentIndex = i;
            Document d = reader.document(currentIndex, FieldSelectors.UUID);
            String uuid = d.get(FieldNames.UUID);
            if (stateMgr.getItemData(uuid) != null)
            {
               if (!documentUUIDs.add(uuid))
               {
                  multipleEntries.add(uuid);
               }
            }
            else
            {
               errors.add(new NodeDeleted(uuid));
            }
         }
      }
      finally
      {
         reader.release();
      }

      // create multiple entries errors
      for (Iterator<String> it = multipleEntries.iterator(); it.hasNext();)
      {
         errors.add(new MultipleEntries(it.next()));
      }

      reader = index.getIndexReader();
      try
      {
         // run through documents again and check parent
         for (int i = 0; i < reader.maxDoc(); i++)
         {
            if (i > 10 && i % (reader.maxDoc() / 5) == 0)
            {
               long progress = Math.round((100.0 * i) / (reader.maxDoc() * 2f));
               log.info("progress: " + (progress + 50) + "%");
            }
            if (reader.isDeleted(i))
            {
               continue;
            }
            final int currentIndex = i;
            Document d = reader.document(currentIndex, FieldSelectors.UUID_AND_PARENT);
            String uuid = d.get(FieldNames.UUID);
            String parentUUIDString = d.get(FieldNames.PARENT);

            if (parentUUIDString == null || documentUUIDs.contains(parentUUIDString))
            {
               continue;
            }

            // parent is missing
            //NodeId parentId = new NodeId(parentUUID);
            if (stateMgr.getItemData(parentUUIDString) != null)
            {
               errors.add(new MissingAncestor(uuid, parentUUIDString));
            }
            else
            {
               errors.add(new UnknownParent(uuid, parentUUIDString));
            }
         }
      }
      finally
      {
         reader.release();
      }
   }

   /**
    * Returns the path for <code>node</code>. If an error occurs this method
    * returns the uuid of the node.
    *
    * @param node the node to retrieve the path from
    * @return the path of the node or its uuid.
    */
   private String getPath(NodeData node)
   {
      // remember as fallback
      return node.getQPath().getAsString();
   }

   //-------------------< ConsistencyCheckError classes >----------------------

   /**
    * One or more ancestors of an indexed node are not available in the index.
    */
   private class MissingAncestor extends ConsistencyCheckError
   {

      private final String parentUUID;

      private MissingAncestor(String uuid, String parentUUID)
      {
         super("Parent of " + uuid + " missing in index. Parent: " + parentUUID, uuid);
         this.parentUUID = parentUUID;
      }

      /**
       * Returns <code>true</code>.
       * @return <code>true</code>.
       */
      @Override
      public boolean repairable()
      {
         return true;
      }

      /**
       * Repairs the missing node by indexing the missing ancestors.
       * @throws IOException if an error occurs while repairing.
       */
      @Override
      public void repair() throws IOException
      {
         String parentId = parentUUID;
         while (parentId != null && !documentUUIDs.contains(parentId))
         {
            try
            {
               NodeData n = (NodeData)stateMgr.getItemData(parentId);
               log.info("Reparing missing node " + getPath(n));
               Document d = index.createDocument(n);
               index.addDocument(d);
               documentUUIDs.add(n.getIdentifier());
               parentId = n.getParentIdentifier();
            }
            catch (RepositoryException e)
            {
               throw new IOException(e.toString());
            }
         }
      }
   }

   /**
    * The parent of a node is not available through the ItemStateManager.
    */
   private class UnknownParent extends ConsistencyCheckError
   {

      private UnknownParent(String uuid, String parentUUID)
      {
         super("Node " + uuid + " has unknown parent: " + parentUUID, uuid);
      }

      /**
       * Not reparable (yet).
       * @return <code>false</code>.
       */
      @Override
      public boolean repairable()
      {
         return false;
      }

      /**
       * No operation.
       */
      @Override
      public void repair() throws IOException
      {
         log.warn("Unknown parent for " + uuid + " cannot be repaired");
      }
   }

   /**
    * A node is present multiple times in the index.
    */
   private class MultipleEntries extends ConsistencyCheckError
   {

      MultipleEntries(String uuid)
      {
         super("Multiple entries found for node " + uuid, uuid);
      }

      /**
       * Returns <code>true</code>.
       * @return <code>true</code>.
       */
      @Override
      public boolean repairable()
      {
         return true;
      }

      /**
       * Removes the nodes with the identical uuids from the index and
       * re-index the node.
       * @throws IOException if an error occurs while repairing.
       */
      @Override
      public void repair() throws IOException
      {
         // first remove all occurrences
         index.removeAllDocuments(uuid);
         // then re-index the node
         try
         {
            NodeData node = (NodeData)stateMgr.getItemData(uuid);
            log.info("Re-indexing duplicate node occurrences in index: " + getPath(node));
            Document d = index.createDocument(node);
            index.addDocument(d);
            documentUUIDs.add(node.getIdentifier());
         }
         catch (RepositoryException e)
         {
            throw new IOException(e.toString(), e);
         }
      }
   }

   /**
    * Indicates that a node has been deleted but is still in the index.
    */
   private class NodeDeleted extends ConsistencyCheckError
   {

      NodeDeleted(String uuid)
      {
         super("Node " + uuid + " does not longer exist.", uuid);
      }

      /**
       * Returns <code>true</code>.
       * @return <code>true</code>.
       */
      @Override
      public boolean repairable()
      {
         return true;
      }

      /**
       * Deletes the nodes from the index.
       * @throws IOException if an error occurs while repairing.
       */
      @Override
      public void repair() throws IOException
      {
         log.info("Removing deleted node from index: " + uuid);
         index.removeDocument(uuid);
      }
   }
}
