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
package org.exoplatform.services.jcr.impl.core.query;

import org.apache.lucene.search.Query;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.impl.checker.InspectionReport;
import org.exoplatform.services.jcr.impl.core.SessionDataManager;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.query.lucene.ChangesHolder;
import org.exoplatform.services.jcr.impl.core.query.lucene.IndexInfos;
import org.exoplatform.services.jcr.impl.core.query.lucene.IndexUpdateMonitor;
import org.exoplatform.services.jcr.impl.core.query.lucene.MultiIndex;
import org.exoplatform.services.jcr.impl.core.query.lucene.QueryHits;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;

/**
 * Defines an interface for the actual node indexing and query execution.
 * The goal is to allow different implementations based on the persistent
 * manager in use. Some persistent model might allow to execute a query
 * in an optimized manner, e.g. database persistence.
 * @LevelAPI Unsupported
 */
public interface QueryHandler
{

   /**
    * Returns the query handler context that passed in {@link
    * #setContext(QueryHandlerContext)}.
    *
    * @return the query handler context.
    */
   QueryHandlerContext getContext();

   /**
    * Adds a <code>Node</code> to the search index.
    * @param node the NodeState to add.
    * @throws RepositoryException if an error occurs while indexing the node.
    * @throws IOException if an error occurs while adding the node to the index.
    */
   void addNode(NodeData node) throws RepositoryException, IOException;

   /**
    * Deletes the Node with <code>id</code> from the search index.
    * @param id the <code>id</code> of the node to delete.
    * @throws IOException if an error occurs while deleting the node.
    */
   void deleteNode(String id) throws IOException;

   /**
    * Updates the index in an atomic operation. Some nodes may be removed and
    * added again in the same updateNodes() call, which is equivalent to an
    * node update.
    *
    * @param remove Iterator of <code>NodeIds</code> of nodes to delete
    * @param add    Iterator of <code>NodeState</code> instance to add to the
    *               index.
    * @throws RepositoryException if an error occurs while indexing a node.
    * @throws IOException if an error occurs while updating the index.
    */
   void updateNodes(Iterator<String> remove, Iterator<NodeData> add) throws RepositoryException, IOException;

   /**
    * Extracts all the changes and returns them as a {@link ChangesHolder} instance
    * @param remove Iterator of <code>NodeIds</code> of nodes to delete
    * @param add    Iterator of <code>NodeState</code> instance to add to the
    *               index.
    * @return a {@link ChangesHolder} instance that contains all the changes
    */
   ChangesHolder getChanges(Iterator<String> remove, Iterator<NodeData> add);

   /**
    * Applies the given changes to the indes in an atomic operation
    * @param changes the changes to apply
    * @throws RepositoryException if an error occurs while indexing a node.
    * @throws IOException if an error occurs while updating the index.
    */
   void apply(ChangesHolder changes) throws RepositoryException, IOException;

   /**
    * Closes this <code>QueryHandler</code> and frees resources attached
    * to this handler.
    */
   void close();

   /**
    * Sets QueryHandlerContext
    * @param context
    */
   void setContext(QueryHandlerContext context);

   /**
    * 
    * initializes QueryHandler
    *
    * @throws IOException
    * @throws RepositoryException
    * @throws RepositoryConfigurationException
    */
   void init() throws IOException, RepositoryException, RepositoryConfigurationException;

   /**
    * Checks whether QueryHandler is initialized or not
    */
   boolean isInitialized();

   /**
    * Creates a new query by specifying the query statement itself and the
    * language in which the query is stated.  If the query statement is
    * syntactically invalid, given the language specified, an
    * InvalidQueryException is thrown. <code>language</code> must specify a query language
    * string from among those returned by QueryManager.getSupportedQueryLanguages(); if it is not
    * then an <code>InvalidQueryException</code> is thrown.
    *
    * @param session the session of the current user creating the query object.
    * @param itemMgr the item manager of the current user.
    * @param statement the query statement.
    * @param language the syntax of the query statement.
    * @throws InvalidQueryException if statement is invalid or language is unsupported.
    * @return A <code>Query</code> object.
    */
   ExecutableQuery createExecutableQuery(SessionImpl session, SessionDataManager itemMgr, String statement,
      String language) throws InvalidQueryException;

   /**
    * Log unindexed changes into error.log
    * 
    * @param removed set of removed node uuids
    * @param added map of added node states and uuids
    * @throws IOException
    */
   void logErrorChanges(Set<String> removed, Set<String> added) throws IOException;

   void setIndexerIoModeHandler(IndexerIoModeHandler handler) throws IOException;

   IndexerIoModeHandler getIndexerIoModeHandler();

   /**
    * @return the name of the query class to use.
    */
   String getQueryClass();

   /**
    * Executes the query on the search index.
    *
    * @param query the lucene query.
    * @return the lucene Hits object.
    * @throws IOException if an error occurs while searching the index.
    */
   QueryHits executeQuery(Query query) throws IOException;

   /**
    * Sets {@link IndexInfos} instance into QueryHandler, which is later passed to {@link MultiIndex}.
    * 
    * @param indexInfos
    */
   void setIndexInfos(IndexInfos indexInfos);

   /**
    * Returns {@link IndexInfos} instance that was set into QueryHandler.
    * @return Index info instance
    */
   IndexInfos getIndexInfos();

   /**
    * @return the indexUpdateMonitor
    */
   IndexUpdateMonitor getIndexUpdateMonitor();

   /**
    * @param indexUpdateMonitor the indexUpdateMonitor to set
    */
   void setIndexUpdateMonitor(IndexUpdateMonitor indexUpdateMonitor);

   /**
    * Switches index into corresponding ONLINE or OFFLINE mode. Offline mode means that new indexing data is
    * collected but index is guaranteed to be unmodified during offline state. Passing the allowQuery flag, can
    * allow or deny performing queries on index during offline mode. AllowQuery is not used when setting index
    * back online. When dropStaleIndexes is set, indexes present on the moment of switching index offline will be
    * marked as stale and removed on switching it back online.
    * 
    * @param isOnline
    * @param allowQuery
    *          doesn't matter, when switching index to online
    * @param dropStaleIndexes
    *          doesn't matter, when switching index to online
    * @throws IOException
    */
   void setOnline(boolean isOnline, boolean allowQuery, boolean dropStaleIndexes) throws IOException;

   /**
    * Offline mode means that new indexing data is collected but index is guaranteed to be unmodified during
    * offline state.
    * 
    * @return the state of index.
    */
   boolean isOnline();

   /**
    * Check index consistency. Iterator goes through index documents and check, does each document have
    * according jcr-node. If <b>autoRepair</b> is true - all broken index-documents will be reindexed,
    * and documents that do not have corresponding jcr-node will be removed.
    * 
    * @param itemStateManager
    * @param isSystem
    * @param report
    * @throws RepositoryException
    * @throws IOException
    */
   void checkIndex(ItemDataConsumer itemStateManager, boolean isSystem, InspectionReport report)
      throws RepositoryException, IOException;
}
