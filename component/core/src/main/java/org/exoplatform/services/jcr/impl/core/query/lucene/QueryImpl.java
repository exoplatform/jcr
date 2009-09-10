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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.QueryResult;

import org.exoplatform.services.log.Log;
import org.apache.lucene.search.Query;

import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionData;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.SessionDataManager;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.query.AndQueryNode;
import org.exoplatform.services.jcr.impl.core.query.DefaultQueryNodeVisitor;
import org.exoplatform.services.jcr.impl.core.query.ExecutableQuery;
import org.exoplatform.services.jcr.impl.core.query.LocationStepQueryNode;
import org.exoplatform.services.jcr.impl.core.query.NodeTypeQueryNode;
import org.exoplatform.services.jcr.impl.core.query.OrderQueryNode;
import org.exoplatform.services.jcr.impl.core.query.PathQueryNode;
import org.exoplatform.services.jcr.impl.core.query.PropertyTypeRegistry;
import org.exoplatform.services.jcr.impl.core.query.QueryNodeFactory;
import org.exoplatform.services.jcr.impl.core.query.QueryParser;
import org.exoplatform.services.jcr.impl.core.query.QueryRootNode;
import org.exoplatform.services.log.ExoLogger;

/**
 * Implements the {@link ExecutableQuery} interface.
 */
public class QueryImpl extends AbstractQueryImpl
{

   /**
    * The logger instance for this class
    */
   private static final Log log = ExoLogger.getLogger(QueryImpl.class);

   /**
    * Represents a query that selects all nodes. E.g. in XPath: //*
    */
   protected final QueryRootNode allNodesQueryNode;

   /**
    * The root node of the query tree
    */
   protected final QueryRootNode root;

   /**
    * Creates a new query instance from a query string.
    * 
    * @param session the session of the user executing this query.
    * @param itemMgr the item manager of the session executing this query.
    * @param index the search index.
    * @param propReg the property type registry.
    * @param statement the query statement.
    * @param language the syntax of the query statement.
    * @param factory the query node factory.
    * @throws InvalidQueryException if the query statement is invalid according
    *           to the specified <code>language</code>.
    */
   public QueryImpl(SessionImpl session, SessionDataManager itemMgr, SearchIndex index, PropertyTypeRegistry propReg,
      String statement, String language, QueryNodeFactory factory) throws InvalidQueryException
   {
      super(session, itemMgr, index, propReg);
      // parse query according to language
      // build query tree using the passed factory
      this.root = QueryParser.parse(statement, language, session.getLocationFactory(), factory);
      allNodesQueryNode = createMatchAllNodesQuery(factory);
   }

   /**
    * Executes this query and returns a <code>{@link QueryResult}</code>.
    * 
    * @param offset the offset in the total result set
    * @param limit the maximum result size
    * @return a <code>QueryResult</code>
    * @throws RepositoryException if an error occurs
    */
   public QueryResult execute(long offset, long limit) throws RepositoryException
   {
      if (log.isDebugEnabled())
      {
         log.debug("Executing query: \n" + root.dump());
      }

      // check for special query
      if (allNodesQueryNode.equals(root))
      {
         return new WorkspaceTraversalResult(session, new InternalQName[]{Constants.JCR_PRIMARYTYPE,
            Constants.JCR_PATH, Constants.JCR_SCORE}, session.getLocationFactory());
      }

      // build lucene query
      Query query =
         LuceneQueryBuilder.createQuery(root, session, index.getContext().getItemStateManager(), index
            .getNamespaceMappings(), index.getContext().getNodeTypeDataManager(), index.getTextAnalyzer(), propReg,
            index.getSynonymProvider(), index.getIndexFormatVersion());

      OrderQueryNode orderNode = root.getOrderNode();

      OrderQueryNode.OrderSpec[] orderSpecs;
      if (orderNode != null)
      {
         orderSpecs = orderNode.getOrderSpecs();
      }
      else
      {
         orderSpecs = new OrderQueryNode.OrderSpec[0];
      }
      InternalQName[] orderProperties = new InternalQName[orderSpecs.length];
      boolean[] ascSpecs = new boolean[orderSpecs.length];
      for (int i = 0; i < orderSpecs.length; i++)
      {
         orderProperties[i] = orderSpecs[i].getProperty();
         ascSpecs[i] = orderSpecs[i].isAscending();
      }

      return new QueryResultImpl(index, itemMgr, session.getLocationFactory(), session.getValueFactory(), session
         .getAccessManager(), session.getUserID(), this, query, new SpellSuggestion(index.getSpellChecker(), root),
         getSelectProperties(), orderProperties, ascSpecs, getRespectDocumentOrder(), offset, limit);
   }

   /**
    * Returns the select properties for this query.
    * 
    * @return array of select property names.
    * @throws RepositoryException if an error occurs.
    */
   protected InternalQName[] getSelectProperties() throws RepositoryException
   {
      // get select properties
      List<InternalQName> selectProps = new ArrayList<InternalQName>();
      selectProps.addAll(Arrays.asList(root.getSelectProperties()));
      if (selectProps.size() == 0)
      {
         // use node type constraint
         LocationStepQueryNode[] steps = root.getLocationNode().getPathSteps();
         final InternalQName[] ntName = new InternalQName[1];
         steps[steps.length - 1].acceptOperands(new DefaultQueryNodeVisitor()
         {

            public Object visit(AndQueryNode node, Object data)
            {
               return node.acceptOperands(this, data);
            }

            public Object visit(NodeTypeQueryNode node, Object data)
            {
               ntName[0] = node.getValue();
               return data;
            }
         }, null);
         if (ntName[0] == null)
         {
            ntName[0] = Constants.NT_BASE;
         }
         NodeTypeData nt = propReg.getNodeTypeDataManager().findNodeType(ntName[0]);

         PropertyDefinitionData[] propDefs = nt.getDeclaredPropertyDefinitions();
         for (int i = 0; i < propDefs.length; i++)
         {
            PropertyDefinitionData propDef = propDefs[i];
            if (!propDef.isResidualSet() && !propDef.isMultiple())
            {
               selectProps.add(propDef.getName());
            }
         }
      }

      // add jcr:path and jcr:score if not selected already
      if (!selectProps.contains(Constants.JCR_PATH))
      {
         selectProps.add(Constants.JCR_PATH);
      }
      if (!selectProps.contains(Constants.JCR_SCORE))
      {
         selectProps.add(Constants.JCR_SCORE);
      }

      return (InternalQName[])selectProps.toArray(new InternalQName[selectProps.size()]);
   }

   /**
    * Returns <code>true</code> if this query node needs items under /jcr:system
    * to be queried.
    * 
    * @return <code>true</code> if this query node needs content under
    *         /jcr:system to be queried; <code>false</code> otherwise.
    */
   public boolean needsSystemTree()
   {
      return this.root.needsSystemTree();
   }

   // ----------------------------< internal >----------------------------------

   /**
    * Creates an abstract query tree that matches all nodes. XPath example:
    * //element(*, nt:base)
    * 
    * @param factory the query node factory.
    * @return the abstract query tree.
    */
   private static QueryRootNode createMatchAllNodesQuery(QueryNodeFactory factory)
   {
      QueryRootNode allNodesQueryNode = factory.createQueryRootNode();
      PathQueryNode pathNode = factory.createPathQueryNode(allNodesQueryNode);
      LocationStepQueryNode lsNode = factory.createLocationStepQueryNode(pathNode);
      lsNode.setNameTest(null);
      lsNode.setIncludeDescendants(true);
      pathNode.addPathStep(lsNode);
      pathNode.setAbsolute(true);
      allNodesQueryNode.setLocationNode(pathNode);
      return allNodesQueryNode;
   }
}
