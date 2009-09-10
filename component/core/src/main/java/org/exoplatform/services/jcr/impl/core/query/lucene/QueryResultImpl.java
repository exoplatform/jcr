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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFactory;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;

import org.exoplatform.services.log.Log;
import org.apache.lucene.search.Query;

import org.exoplatform.services.jcr.access.AccessManager;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.SessionDataManager;
import org.exoplatform.services.log.ExoLogger;

/**
 * Implements the <code>javax.jcr.query.QueryResult</code> interface.
 */
public class QueryResultImpl implements QueryResult
{

   /**
    * The logger instance for this class
    */
   private static final Log log = ExoLogger.getLogger(QueryResultImpl.class);

   /**
    * The search index to execute the query.
    */
   private final SearchIndex index;

   /**
    * The item manager of the session executing the query
    */
   private final SessionDataManager itemMgr;

   /**
    * The name and path resolver of the session executing the query
    */
   protected final LocationFactory resolver;

   /**
    * The access manager of the session that executes the query.
    */
   private final AccessManager accessMgr;

   /**
    * The query instance which created this query result.
    */
   protected final AbstractQueryImpl queryImpl;

   /**
    * The lucene query to execute.
    */
   protected final Query query;

   /**
    * The spell suggestion or <code>null</code> if not available.
    */
   protected final SpellSuggestion spellSuggestion;

   /**
    * The select properties
    */
   protected final InternalQName[] selectProps;

   /**
    * The names of properties to use for ordering the result set.
    */
   protected final InternalQName[] orderProps;

   /**
    * The order specifier for each of the order properties.
    */
   protected final boolean[] orderSpecs;

   /**
    * The result nodes including their score. This list is populated on a lazy
    * basis while a client iterates through the results.
    */
   private final List<ScoreNode> resultNodes = new ArrayList<ScoreNode>();

   /**
    * This is the raw number of results that matched the query. This number also
    * includes matches which will not be returned due to access restrictions.
    * This value is set when the query is executed the first time.
    */
   private int numResults = -1;

   /**
    * The number of results that are invalid, either because a node does not
    * exist anymore or because the session does not have access to the node.
    */
   private int invalid = 0;

   /**
    * If <code>true</code> nodes are returned in document order.
    */
   private final boolean docOrder;

   /**
    * The excerpt provider or <code>null</code> if none was created yet.
    */
   private ExcerptProvider excerptProvider;

   /**
    * The offset in the total result set
    */
   private final long offset;

   /**
    * The maximum size of this result if limit > 0
    */
   private final long limit;

   private final String userId;

   private final ValueFactory vFactory;

   /**
    * Creates a new query result.
    * 
    * @param index the search index where the query is executed.
    * @param itemMgr the item manager of the session executing the query.
    * @param resolver the namespace resolver of the session executing the query.
    * @param accessMgr the access manager of the session executiong the query.
    * @param queryImpl the query instance which created this query result.
    * @param query the lucene query to execute on the index.
    * @param spellSuggestion the spell suggestion or <code>null</code> if none is
    *          available.
    * @param selectProps the select properties of the query.
    * @param orderProps the names of the order properties.
    * @param orderSpecs the order specs, one for each order property name.
    * @param documentOrder if <code>true</code> the result is returned in
    *          document order.
    * @param limit the maximum result size
    * @param offset the offset in the total result set
    */
   public QueryResultImpl(SearchIndex index, SessionDataManager itemMgr, LocationFactory resolver,
      ValueFactory vFactory, AccessManager accessMgr, String userId, AbstractQueryImpl queryImpl, Query query,
      SpellSuggestion spellSuggestion, InternalQName[] selectProps, InternalQName[] orderProps, boolean[] orderSpecs,
      boolean documentOrder, long offset, long limit) throws RepositoryException
   {

      this.index = index;
      this.itemMgr = itemMgr;
      this.userId = userId;
      this.resolver = resolver;
      this.vFactory = vFactory;
      this.accessMgr = accessMgr;
      this.queryImpl = queryImpl;
      this.query = query;
      this.spellSuggestion = spellSuggestion;
      this.selectProps = selectProps;
      this.orderProps = orderProps;
      this.orderSpecs = orderSpecs;
      this.docOrder = orderProps.length == 0 && documentOrder;
      this.offset = offset;
      this.limit = limit;
      // if document order is requested get all results right away
      getResults(docOrder ? Integer.MAX_VALUE : index.getQueryHandlerConfig().getResultFetchSize());
   }

   /**
    * {@inheritDoc}
    */
   public String[] getColumnNames() throws RepositoryException
   {
      try
      {
         String[] propNames = new String[selectProps.length];
         for (int i = 0; i < selectProps.length; i++)
         {
            propNames[i] = resolver.createJCRName(selectProps[i]).getAsString();
         }
         return propNames;
      }
      catch (NamespaceException npde)
      {
         String msg = "encountered invalid property name";
         log.debug(msg);
         throw new RepositoryException(msg, npde);
      }
   }

   /**
    * {@inheritDoc}
    */
   public NodeIterator getNodes() throws RepositoryException
   {
      return getNodeIterator();
   }

   /**
    * {@inheritDoc}
    */
   public RowIterator getRows() throws RepositoryException
   {
      if (excerptProvider == null)
      {
         try
         {
            excerptProvider = index.createExcerptProvider(query);
         }
         catch (IOException e)
         {
            throw new RepositoryException(e);
         }
      }
      return new RowIteratorImpl(getNodeIterator(), selectProps, resolver, excerptProvider, spellSuggestion, vFactory);
   }

   /**
    * Executes the query for this result and returns hits. The caller must close
    * the query hits when he is done using it.
    * 
    * @return hits for this query result.
    * @throws IOException if an error occurs while executing the query.
    */
   protected QueryHits executeQuery() throws IOException
   {
      return index.executeQuery(query, queryImpl.needsSystemTree(), orderProps, orderSpecs);
   }

   // --------------------------------< internal >------------------------------

   /**
    * Creates a node iterator over the result nodes.
    * 
    * @return a node iterator over the result nodes.
    */
   private ScoreNodeIterator getNodeIterator()
   {
      if (docOrder)
      {
         return new DocOrderNodeIteratorImpl(itemMgr, accessMgr, userId, resultNodes);
      }
      return new LazyScoreNodeIterator();

   }

   /**
    * Attempts to get <code>size</code> results and puts them into
    * {@link #resultNodes}. If the size of {@link #resultNodes} is less than
    * <code>size</code> then there are no more than
    * <code>resultNodes.size()</code> results for this query.
    * 
    * @param size the number of results to fetch for the query.
    * @throws RepositoryException if an error occurs while executing the query.
    */
   private void getResults(long size) throws RepositoryException
   {
      if (log.isDebugEnabled())
      {
         log.debug("getResults(" + size + ")");
      }

      long maxResultSize = size;

      // is there any limit?
      if (limit > 0)
      {
         maxResultSize = limit;
      }

      if (resultNodes.size() >= maxResultSize)
      {
         // we already have them all
         return;
      }

      // execute it
      QueryHits result = null;
      try
      {
         result = executeQuery();

         // set num results with the first query execution
         if (numResults == -1)
         {
            numResults = result.length();
         }

         int start = resultNodes.size() + invalid + (int)offset;
         int max = Math.min(result.length(), numResults);
         for (int i = start; i < max && resultNodes.size() < maxResultSize; i++)
         {
            String id = result.getFieldContent(i, FieldNames.UUID);
            // check access
            resultNodes.add(new ScoreNode(id, result.score(i)));
         }

      }
      catch (IOException e)
      {
         log.error("Exception while executing query: ", e);
      }
      finally
      {
         if (result != null)
         {
            try
            {
               result.close();
            }
            catch (IOException e)
            {
               log.warn("Unable to close query result: " + e);
            }
         }
      }
   }

   /**
    * Returns the total number of hits. This is the number of results you will
    * get get if you don't set any limit or offset. Keep in mind that this number
    * may get smaller if nodes are found in the result set which the current
    * session has no permission to access.
    */
   public int getTotalSize()
   {
      return numResults - invalid;
   }

   private final class LazyScoreNodeIterator implements TwoWayRangeIterator, ScoreNodeIterator
   {

      private int position = -1;

      private boolean initialized = false;

      private NodeImpl next;

      /**
       * {@inheritDoc}
       */
      public float getScore()
      {
         initialize();
         if (!hasNext())
         {
            throw new NoSuchElementException();
         }
         return resultNodes.get(position).getScore();
      }

      /**
       * {@inheritDoc}
       */
      public NodeImpl nextNodeImpl()
      {
         initialize();
         if (next == null)
         {
            throw new NoSuchElementException();
         }
         NodeImpl n = next;
         fetchNext();
         return n;
      }

      /**
       * {@inheritDoc}
       */
      public Node nextNode()
      {
         return nextNodeImpl();
      }

      /**
       * {@inheritDoc}
       */
      public void skip(long skipNum)
      {
         initialize();
         if (skipNum < 0)
         {
            throw new IllegalArgumentException("skipNum must not be negative");
         }
         if ((position + invalid + skipNum) > numResults)
         {
            throw new NoSuchElementException();
         }
         if (skipNum == 0)
         {
            // do nothing
         }
         else
         {
            // attempt to get enough results
            try
            {
               getResults(position + invalid + (int)skipNum);
               if (resultNodes.size() >= position + skipNum)
               {
                  // skip within already fetched results
                  position += skipNum - 1;
                  fetchNext();
               }
               else
               {
                  // not enough results after getResults()
                  throw new NoSuchElementException();
               }
            }
            catch (RepositoryException e)
            {
               throw new NoSuchElementException(e.getMessage());
            }
         }
      }

      public void skipBack(long skipNum)
      {
         initialize();
         if (skipNum < 0)
         {
            throw new IllegalArgumentException("skipNum must not be negative");
         }
         if ((position - skipNum) < 0)
         {
            throw new NoSuchElementException();
         }
         if (skipNum == 0)
         {
            // do nothing
         }
         else
         {
            position -= skipNum + 1;
            fetchNext();
         }
      }

      /**
       * {@inheritDoc} <p/> This value may shrink when the query result encounters
       * non-existing nodes or the session does not have access to a node.
       */
      public long getSize()
      {
         long size = getTotalSize() - offset;
         if (limit > 0 && size > limit)
         {
            return limit;
         }
         else
         {
            return size;
         }
      }

      /**
       * {@inheritDoc}
       */
      public long getPosition()
      {
         initialize();
         return position;
      }

      /**
       * @throws UnsupportedOperationException always.
       */
      public void remove()
      {
         throw new UnsupportedOperationException("remove");
      }

      /**
       * {@inheritDoc}
       */
      public boolean hasNext()
      {
         initialize();
         return next != null;
      }

      /**
       * {@inheritDoc}
       */
      public Object next()
      {
         return nextNodeImpl();
      }

      /**
       * Initializes this iterator but only if it is not yet initialized.
       */
      private void initialize()
      {
         if (!initialized)
         {
            fetchNext();
            initialized = true;
         }
      }

      /**
       * Fetches the next node to return by this iterator. If this method returns
       * and {@link #next} is <code>null</code> then there is no next node.
       */
      private void fetchNext()
      {
         next = null;
         int nextPos = position + 1;
         while (next == null && (nextPos + invalid) < numResults)
         {
            if (nextPos >= resultNodes.size())
            {
               // fetch more results
               try
               {
                  int num;
                  if (resultNodes.size() == 0)
                  {
                     num = index.getQueryHandlerConfig().getResultFetchSize();
                  }
                  else
                  {
                     num = resultNodes.size() * 2;
                  }
                  getResults(num);
               }
               catch (RepositoryException e)
               {
                  log.warn("Exception getting more results: " + e);
               }
               // check again
               if (nextPos >= resultNodes.size())
               {
                  // no more valid results
                  return;
               }
            }
            ScoreNode sn = resultNodes.get(nextPos);
            try
            {
               next = (NodeImpl)itemMgr.getItemByIdentifier(sn.getNodeId(), true);
               if (next == null)
               {
                  resultNodes.remove(nextPos);
                  invalid++;
               }
            }
            catch (RepositoryException e)
            {
               log.warn("Exception retrieving Node with UUID: " + sn.getNodeId() + ": " + e.toString());
               // remove score node and try next
               resultNodes.remove(nextPos);
               invalid++;
            }
         }
         position++;
      }
   }
}
