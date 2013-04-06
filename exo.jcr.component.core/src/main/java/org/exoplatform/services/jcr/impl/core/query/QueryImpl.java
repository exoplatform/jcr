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

import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.JCRPath;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.SessionDataManager;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.nodetype.ItemAutocreator;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionException;

/**
 * Provides the default implementation for a JCR query.
 * @LevelAPI Experimental
 */
public class QueryImpl extends AbstractQueryImpl
{

   /**
    * The logger instance for this class
    */
   private static final Logger log = LoggerFactory.getLogger("exo.jcr.component.core.QueryImpl");

   /**
    * A string constant representing the JCR-SQL2 query language.
    *
    * @since JCR 2.0
    */
   public static final String JCR_SQL2 = "JCR-SQL2";

   /**
    * A string constant representing the JCR-JQOM query language.
    *
    * @since JCR 2.0
    */
   public static final String JCR_JQOM = "JCR-JQOM";

   /**
    * The session of the user executing this query
    */
   protected SessionImpl session;

   /**
    * The query statement
    */
   protected String statement;

   /**
    * The syntax of the query statement
    */
   protected String language;

   /**
    * The actual query implementation that can be executed
    */
   protected ExecutableQuery query;

   /**
    * The node where this query is persisted. Only set when this is a persisted
    * query.
    */
   protected Node node;

   /**
    * The query handler for this query.
    */
   protected QueryHandler handler;

   /**
    * Flag indicating whether this query is initialized.
    */
   private boolean initialized = false;

   /**
    * The maximum result size.
    */
   private long limit;

   /**
    * The offset in the total result set.
    */
   private long offset;

   /**
    * Indicates does we should use case insensitive sorting by field in order by clause or not.
    */
   private boolean caseInsensitiveOrder;

   /**
    * @inheritDoc
    */
   public void init(SessionImpl session, SessionDataManager itemMgr, QueryHandler handler, String statement,
      String language) throws InvalidQueryException
   {
      checkNotInitialized();
      this.session = session;
      this.statement = statement;
      this.language = language;
      this.handler = handler;
      this.query = handler.createExecutableQuery(session, itemMgr, statement, language);
      setInitialized();
   }

   /**
    * @inheritDoc
    */
   public void init(SessionImpl session, SessionDataManager itemMgr, QueryHandler handler, Node node)
      throws InvalidQueryException, RepositoryException
   {
      checkNotInitialized();
      this.session = session;
      this.node = node;
      this.handler = handler;

      if (!((ExtendedNode)node).isNodeType(Constants.NT_QUERY))
      {
         throw new InvalidQueryException("node is not of type nt:query");
      }
      statement = node.getProperty("jcr:statement").getString();
      language = node.getProperty("jcr:language").getString();
      query = handler.createExecutableQuery(session, itemMgr, statement, language);
      setInitialized();
   }

   /**
    * This method simply forwards the <code>execute</code> call to the
    * {@link ExecutableQuery} object returned by
    * {@link QueryHandler#createExecutableQuery}.
    * {@inheritDoc}
    */
   public QueryResult execute() throws RepositoryException
   {
      checkInitialized();
      long time = 0;
      if (log.isDebugEnabled())
      {
         time = System.currentTimeMillis();
      }

      QueryResult result = query.execute(offset, limit, caseInsensitiveOrder);

      if (log.isDebugEnabled())
      {
         time = System.currentTimeMillis() - time;
         NumberFormat format = NumberFormat.getNumberInstance();
         format.setMinimumFractionDigits(2);
         format.setMaximumFractionDigits(2);
         String seconds = format.format((double)time / 1000);
         log.debug("executed in " + seconds + " s. (" + statement + ")");
      }
      return result;
   }

   /**
    * {@inheritDoc}
    */
   public String getStatement()
   {
      checkInitialized();
      return statement;
   }

   /**
    * {@inheritDoc}
    */
   public String getLanguage()
   {
      checkInitialized();
      return language;
   }

   /**
    * {@inheritDoc}
    */
   public String getStoredQueryPath() throws ItemNotFoundException, RepositoryException
   {
      checkInitialized();
      if (node == null)
      {
         throw new ItemNotFoundException("not a persistent query");
      }
      return node.getPath();
   }

   /**
    * {@inheritDoc}
    */
   public Node storeAsNode(String absPath) throws ItemExistsException, PathNotFoundException, VersionException,
      ConstraintViolationException, LockException, UnsupportedRepositoryOperationException, RepositoryException
   {

      checkInitialized();
      JCRPath path = session.getLocationFactory().parseAbsPath(absPath);
      QPath qpath = path.getInternalPath();
      NodeImpl parent = (NodeImpl)session.getTransientNodesManager().getItem(qpath.makeParentPath(), false);
      if (parent == null)
         throw new PathNotFoundException("Parent not found for " + path.getAsString(false));

      // validate as on parent child node
      parent.validateChildNode(qpath.getName(), Constants.NT_QUERY);

      NodeData queryData =
         TransientNodeData.createNodeData((NodeData)parent.getData(), qpath.getName(), Constants.NT_QUERY);
      NodeImpl queryNode =
         (NodeImpl)session.getTransientNodesManager().update(ItemState.createAddedState(queryData), false);

      NodeTypeDataManager ntmanager = session.getWorkspace().getNodeTypesHolder();

      ItemAutocreator itemAutocreator =
         new ItemAutocreator(ntmanager, session.getValueFactory(), session.getTransientNodesManager(), false);

      PlainChangesLog changes =
         itemAutocreator.makeAutoCreatedItems((NodeData)queryNode.getData(), Constants.NT_QUERY, session
            .getTransientNodesManager(), session.getUserID());

      for (ItemState autoCreatedState : changes.getAllStates())
      {
         session.getTransientNodesManager().update(autoCreatedState, false);
      }
      // queryNode.addAutoCreatedItems(Constants.NT_QUERY);
      // set properties
      TransientValueData value = new TransientValueData(language);
      TransientPropertyData jcrLanguage =
         TransientPropertyData.createPropertyData(queryData, Constants.JCR_LANGUAGE, PropertyType.STRING, false, value);
      session.getTransientNodesManager().update(ItemState.createAddedState(jcrLanguage), false);

      value = new TransientValueData(statement);
      TransientPropertyData jcrStatement =
         TransientPropertyData
            .createPropertyData(queryData, Constants.JCR_STATEMENT, PropertyType.STRING, false, value);
      session.getTransientNodesManager().update(ItemState.createAddedState(jcrStatement), false);

      // NOTE: for save stored node need save() on parent or on session (6.6.10
      // The Query Object)
      node = queryNode;
      return node;
   }

   /**
    * Binds the given <code>value</code> to the variable named
    * <code>varName</code>.
    *
    * @param varName name of variable in query
    * @param value   value to bind
    * @throws IllegalArgumentException      if <code>varName</code> is not a
    *                                       valid variable in this query.
    * @throws javax.jcr.RepositoryException if an error occurs.
    */
   public void bindValue(String varName, Value value) throws IllegalArgumentException, RepositoryException
   {
      checkInitialized();
      query.bindValue(session.getLocationFactory().parseJCRName(varName).getInternalName(), value);
   }

   /**
    * Sets the maximum size of the result set.
    *
    * @param limit new maximum size of the result set
    */
   public void setLimit(long limit)
   {
      this.limit = limit;
   }

   /**
    * Sets the start offset of the result set.
    *
    * @param offset new start offset of the result set
    */
   public void setOffset(long offset)
   {
      this.offset = offset;
   }

   /**
    * Setter for {@link #caseInsensitiveOrder} field.
    */
   public void setCaseInsensitiveOrder(boolean caseInsensitiveOrder)
   {
      this.caseInsensitiveOrder = caseInsensitiveOrder;
   }

   //-----------------------------< internal >---------------------------------

   /**
    * Sets the initialized flag.
    */
   protected void setInitialized()
   {
      initialized = true;
   }

   /**
    * Checks if this query is not yet initialized and throws an
    * <code>IllegalStateException</code> if it is already initialized.
    */
   protected void checkNotInitialized()
   {
      if (initialized)
      {
         throw new IllegalStateException("already initialized");
      }
   }

   /**
    * Checks if this query is initialized and throws an
    * <code>IllegalStateException</code> if it is not yet initialized.
    */
   protected void checkInitialized()
   {
      if (!initialized)
      {
         throw new IllegalStateException("not initialized");
      }
   }

}
