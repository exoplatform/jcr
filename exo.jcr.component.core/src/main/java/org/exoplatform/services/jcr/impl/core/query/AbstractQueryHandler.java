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

import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.impl.core.query.lucene.DefaultIndexUpdateMonitor;
import org.exoplatform.services.jcr.impl.core.query.lucene.IndexInfos;
import org.exoplatform.services.jcr.impl.core.query.lucene.IndexUpdateMonitor;
import org.exoplatform.services.jcr.impl.core.query.lucene.MultiIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;

import javax.jcr.RepositoryException;

/**
 * Implements default behaviour for some methods of {@link QueryHandler}.
 */
public abstract class AbstractQueryHandler implements QueryHandler
{

   /**
    * Logger instance for this class
    */
   private static final Logger log = LoggerFactory.getLogger("exo.jcr.component.core.AbstractQueryHandler");

   /**
    * The context for this query handler.
    */
   private QueryHandlerContext context;

   protected boolean initialized = false;

   /**
    * The {@link OnWorkspaceInconsistency} handler. Defaults to 'fail'.
    */
   private OnWorkspaceInconsistency owi = OnWorkspaceInconsistency.FAIL;

   /**
    * The name of a class that extends {@link AbstractQueryImpl}.
    */
   private String queryClass = QueryImpl.class.getName();

   /**
    * The max idle time for this query handler until it is stopped. This
    * property is actually not used anymore.
    */
   private String idleTime;

   /**
    * The handler of the Indexer io mode
    */
   protected IndexerIoModeHandler modeHandler;

   /**
    * {@link IndexInfos} instance that is passed to {@link MultiIndex}
    */
   protected IndexInfos indexInfos;

   protected IndexUpdateMonitor indexUpdateMonitor;

   public boolean isInitialized()
   {
      return initialized;
   }

   /**
    * {@inheritDoc}
    */
   public void setIndexerIoModeHandler(IndexerIoModeHandler modeHandler) throws IOException
   {
      this.modeHandler = modeHandler;
   }

   /**
    * @see org.exoplatform.services.jcr.impl.core.query.QueryHandler#getIndexerIoModeHandler()
    */
   public IndexerIoModeHandler getIndexerIoModeHandler()
   {
      return modeHandler;
   }

   /**
    * {@inheritDoc}
    */
   public void setContext(QueryHandlerContext context)
   {
      this.context = context;
   }

   /**
    * Initializes QueryHandler with given IoMode (RW/RO)
    */
   public void init() throws IOException, RepositoryException, RepositoryConfigurationException
   {
      doInit();
      initialized = true;
   }

   /**
    * This method must be implemented by concrete sub classes and will be
    * called from {@link #init}.
    *
    * @throws IOException If an error occurs.
    * @throws RepositoryException 
    */
   protected abstract void doInit() throws IOException, RepositoryException;

   /**
    * Returns the context for this query handler.
    *
    * @return the <code>QueryHandlerContext</code> instance for this
    *         <code>QueryHandler</code>.
    */
   public QueryHandlerContext getContext()
   {
      return context;
   }

   /**
    * This default implementation calls the individual {@link #deleteNode(org.apache.jackrabbit.core.NodeId)}
    * and {@link #addNode(org.apache.jackrabbit.core.state.NodeState)} methods
    * for each entry in the iterators. First the nodes to remove are processed
    * then the nodes to add.
    *
    * @param remove uuids of nodes to remove.
    * @param add NodeStates to add.
    * @throws RepositoryException if an error occurs while indexing a node.
    * @throws IOException if an error occurs while updating the index.
    */
   public void updateNodes(Iterator<String> remove, Iterator<NodeData> add) throws RepositoryException, IOException
   {
      while (remove.hasNext())
      {
         deleteNode(remove.next());
      }
      while (add.hasNext())
      {
         addNode(add.next());
      }
   }

   /**
    * @return the {@link OnWorkspaceInconsistency} handler.
    */
   public OnWorkspaceInconsistency getOnWorkspaceInconsistencyHandler()
   {
      return owi;
   }

   //--------------------------< properties >----------------------------------

   /**
     * Sets the {@link OnWorkspaceInconsistency} handler with the given name.
     * Currently the only valid name is:
     * <ul>
     * <li><code>fail</code></li>
     * </ul>
     *
     * @param name the name of a {@link OnWorkspaceInconsistency} handler.
     */
   public void setOnWorkspaceInconsistency(String name)
   {
      owi = OnWorkspaceInconsistency.fromString(name);
   }

   /**
    * @return the name of the currently set {@link OnWorkspaceInconsistency}.
    */
   public String getOnWorkspaceInconsistency()
   {
      return owi.getName();
   }

   /**
    * Sets the name of the query class to use.
    *
    * @param queryClass the name of the query class to use.
    */
   public void setQueryClass(String queryClass)
   {
      this.queryClass = queryClass;
   }

   /**
    * @return the name of the query class to use.
    */
   public String getQueryClass()
   {
      return queryClass;
   }

   /**
    * @return the query handler idle time.
    */
   public String getIdleTime()
   {
      return idleTime;
   }

   /**
    * {@inheritDoc}
    */
   public void setIndexInfos(IndexInfos indexInfos)
   {
      this.indexInfos = indexInfos;
   }

   /**
    * @see org.exoplatform.services.jcr.impl.core.query.QueryHandler#getIndexInfos()
    */
   public IndexInfos getIndexInfos()
   {
      return indexInfos == null ? new IndexInfos() : indexInfos;
   }

   /**
    * @return the indexUpdateMonitor
    */
   public IndexUpdateMonitor getIndexUpdateMonitor()
   {
      return indexUpdateMonitor == null ? new DefaultIndexUpdateMonitor() : indexUpdateMonitor;
   }

   /**
    * @param indexUpdateMonitor the indexUpdateMonitor to set
    */
   public void setIndexUpdateMonitor(IndexUpdateMonitor indexUpdateMonitor)
   {
      this.indexUpdateMonitor = indexUpdateMonitor;
   }
}