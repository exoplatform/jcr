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
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.impl.core.SessionDataManager;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.query.lucene.QueryHits;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;

/**
 * Defines an interface for the actual node indexing and query execution. The goal is to allow
 * different implementations based on the persistent manager in use. Some persistent model might
 * allow to execute a query in an optimized manner, e.g. database persistence.
 */
public interface QueryHandler
{

   /**
    * Initializes this query handler. This method is called after the <code>QueryHandler</code> is
    * instantiated.
    * 
    * @param context the context for this query handler.
    * @throws IOException if an error occurs during initialization.
    */
   void setContext(QueryHandlerContext context) throws IOException;

   /**
    * Returns the query handler context that passed in {@link #init(QueryHandlerContext)}.
    * 
    * @return the query handler context.
    */
   QueryHandlerContext getContext();

   /**
    * Adds a <code>Node</code> to the search index.
    * 
    * @param node the NodeState to add.
    * @throws RepositoryException if an error occurs while indexing the node.
    * @throws IOException if an error occurs while adding the node to the index.
    */
   void addNode(NodeData node) throws RepositoryException, IOException;

   /**
    * Deletes the Node with <code>id</code> from the search index.
    * 
    * @param id the <code>id</code> of the node to delete.
    * @throws IOException if an error occurs while deleting the node.
    */
   void deleteNode(String id) throws IOException;

   /**
    * Updates the index in an atomic operation. Some nodes may be removed and added again in the same
    * updateNodes() call, which is equivalent to an node update.
    * 
    * @param remove Iterator of <code>NodeIds</code> of nodes to delete
    * @param add Iterator of <code>NodeState</code> instance to add to the index.
    * @throws RepositoryException if an error occurs while indexing a node.
    * @throws IOException if an error occurs while updating the index.
    */
   void updateNodes(Iterator<String> remove, Iterator<NodeData> add) throws RepositoryException, IOException;

   /**
    * Closes this <code>QueryHandler</code> and frees resources attached to this handler.
    */
   void close();

   /**
    * Closes this <code>QueryHandler</code> and frees resources attached to this handler.
    * @throws IOException 
    * @throws RepositoryException 
    * @throws RepositoryConfigurationException 
    */
   void init() throws IOException, RepositoryException, RepositoryConfigurationException;

   /**
    * Creates a new query by specifying the query statement itself and the language in which the
    * query is stated. If the query statement is syntactically invalid, given the language specified,
    * an InvalidQueryException is thrown. <code>language</code> must specify a query language string
    * from among those returned by QueryManager.getSupportedQueryLanguages(); if it is not then an
    * <code>InvalidQueryException</code> is thrown.
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
    * Creates a new instance of an {@link AbstractQueryImpl} which is not initialized.
    * 
    * @return an new query instance.
    * @throws RepositoryException
    * @throws RepositoryException if an error occurs while creating a new query instance.
    */
   AbstractQueryImpl createQueryInstance() throws RepositoryException;

   /**
    * Log unindexed changes into error.log
    * 
    * @param removed set of removed node uuids
    * @param added map of added node states and uuids
    * @throws IOException
    */
   void logErrorChanges(Set<String> removed, Set<String> added) throws IOException;

   /**
    * Executes the query on the search index.
    * 
    * @param queryImpl the query impl.
    * @param query the lucene query.
    * @param orderProps name of the properties for sort order.
    * @param orderSpecs the order specs for the sort order properties. <code>true</code> indicates
    *          ascending order, <code>false</code> indicates descending.
    * @return the lucene Hits object.
    * @throws IOException if an error occurs while searching the index.
    */
   public QueryHits executeQuery(Query query, boolean needsSystemTree, InternalQName[] orderProps, boolean[] orderSpecs)
      throws IOException;

}
