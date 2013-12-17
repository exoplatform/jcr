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

import org.exoplatform.services.jcr.access.AccessManager;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.core.SessionDataManager;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.security.IdentityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;

/**
 * Implements the <code>QueryResult</code> interface.
 */
public abstract class QueryResultImpl implements QueryResult
{

   /**
    * The logger instance for this class
    */
   private static final Logger LOG = LoggerFactory.getLogger("exo.jcr.component.core.QueryResultImpl");

   /**
    * The search index to execute the query.
    */
   protected final SearchIndex index;

   /**
    * The item manager of the session executing the query
    */
   protected final SessionDataManager itemMgr;

   /**
    * The session executing the query
    */
   protected final SessionImpl session;

   /**
    * The access manager of the session that executes the query.
    */
   protected final AccessManager accessMgr;

   /**
    * The query instance which created this query result.
    */
   protected final AbstractQueryImpl queryImpl;

   /**
    * The spell suggestion or <code>null</code> if not available.
    */
   protected final SpellSuggestion spellSuggestion;

   /**
    * The select properties
    */
   protected final InternalQName[] selectProps;

   /**
    * The relative paths of properties to use for ordering the result set.
    */
   protected final QPath[] orderProps;

   /**
    * The order specifier for each of the order properties.
    */
   protected final boolean[] orderSpecs;

   /**
    * The result nodes including their score. This list is populated on a lazy
    * basis while a client iterates through the results.
    * <p/>
    * The exact type is: <code>List&lt;ScoreNode[]></code>
    */
   private final List resultNodes = new ArrayList();

   /**
    * This is the raw number of results that matched the query. This number
    * also includes matches which will not be returned due to access
    * restrictions. This value is set whenever hits are obtained.
    */
   private int numResults = -1;

   /**
    * The selector names associated with the score nodes. The selector names
    * are set when the query is executed via {@link #getResults(long)}.
    */
   private InternalQName[] selectorNames;

   /**
    * The number of results that are invalid, either because a node does not
    * exist anymore or because the session does not have access to the node.
    */
   private int invalid = 0;

   /**
    * If <code>true</code> nodes are returned in document order.
    */
   protected final boolean docOrder;

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

   /**
    * If <code>true</code>, it means we're using a System session.
    */
   private final boolean isSystemSession;

   /**
    * Creates a new query result. The concrete sub class is responsible for
    * calling {@link #getResults(long)} after this constructor had been called.
    *
    * @param index           the search index where the query is executed.
    * @param itemMgr         the item manager of the session executing the
    *                        query.
    * @param session         the session executing the query.
    * @param accessMgr       the access manager of the session executiong the
    *                        query.
    * @param queryImpl       the query instance which created this query
    *                        result.
    * @param spellSuggestion the spell suggestion or <code>null</code> if none
    *                        is available.
    * @param selectProps     the select properties of the query.
    * @param orderProps      the relative paths of the order properties.
    * @param orderSpecs      the order specs, one for each order property
    *                        name.
    * @param documentOrder   if <code>true</code> the result is returned in
    *                        document order.
    * @param limit           the maximum result size
    * @param offset          the offset in the total result set
    * @throws RepositoryException if an error occurs while reading from the
    *                             repository.
    */
   public QueryResultImpl(SearchIndex index, SessionDataManager itemMgr, SessionImpl session, AccessManager accessMgr,
      AbstractQueryImpl queryImpl, SpellSuggestion spellSuggestion, InternalQName[] selectProps, QPath[] orderProps,
      boolean[] orderSpecs, boolean documentOrder, long offset, long limit) throws RepositoryException
   {
      this.index = index;
      this.itemMgr = itemMgr;
      this.session = session;
      this.accessMgr = accessMgr;
      this.queryImpl = queryImpl;
      this.spellSuggestion = spellSuggestion;
      this.selectProps = selectProps;
      this.orderProps = orderProps;
      this.orderSpecs = orderSpecs;
      this.docOrder = orderProps.length == 0 && documentOrder;
      this.offset = offset;
      this.limit = limit;
      this.isSystemSession = IdentityConstants.SYSTEM.equals(session.getUserID());
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
            propNames[i] = session.getLocationFactory().createJCRName(selectProps[i]).getAsString();
         }
         return propNames;
      }
      catch (NamespaceException npde)
      {
         String msg = "encountered invalid property name";
         LOG.debug(msg);
         throw new RepositoryException(msg, npde);
      }
   }

   /**
    * {@inheritDoc}
    */
   public NodeIterator getNodes() throws RepositoryException
   {
      return new NodeIteratorImpl(itemMgr, getScoreNodes(), 0);
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
            excerptProvider = createExcerptProvider();
         }
         catch (IOException e)
         {
            throw new RepositoryException(e);
         }
      }
      return new RowIteratorImpl(getScoreNodes(), selectProps, selectorNames, itemMgr, session.getLocationFactory(),
         excerptProvider, spellSuggestion, index.getContext().getCleanerHolder());
   }

   /**
    * Executes the query for this result and returns hits. The caller must
    * close the query hits when he is done using it.
    *
    * @param resultFetchHint a hint on how many results should be fetched.
    * @return hits for this query result.
    * @throws IOException if an error occurs while executing the query.
    * @throws RepositoryException 
    */
   protected abstract MultiColumnQueryHits executeQuery(long resultFetchHint) throws IOException, RepositoryException;

   /**
    * Creates an excerpt provider for this result set.
    *
    * @return an excerpt provider.
    * @throws IOException if an error occurs.
    */
   protected abstract ExcerptProvider createExcerptProvider() throws IOException;

   //--------------------------------< internal >------------------------------

   /**
    * Creates a {@link ScoreNodeIterator} over the query result.
    *
    * @return a {@link ScoreNodeIterator} over the query result.
    */
   private ScoreNodeIterator getScoreNodes()
   {
      if (docOrder)
      {
         return new DocOrderScoreNodeIterator(itemMgr, resultNodes, 0);
      }
      else
      {
         return new LazyScoreNodeIteratorImpl();
      }
   }

   /**
    * Attempts to get <code>size</code> results and puts them into {@link
    * #resultNodes}. If the size of {@link #resultNodes} is less than
    * <code>size</code> then there are no more than <code>resultNodes.size()</code>
    * results for this query.
    *
    * @param size the number of results to fetch for the query.
    * @throws RepositoryException if an error occurs while executing the
    *                             query.
    */
   protected void getResults(long size) throws RepositoryException
   {
      if (LOG.isDebugEnabled())
      {
         LOG.debug("getResults({}) limit={}", new Long(size), new Long(limit));
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
      MultiColumnQueryHits result = null;
      try
      {
         long time = 0;
         if (LOG.isDebugEnabled())
         {
            time = System.currentTimeMillis();
         }
         result = executeQuery(maxResultSize);
         if (LOG.isDebugEnabled())
         {
            LOG.debug("query executed in {} ms", new Long(System.currentTimeMillis() - time));
         }
         // set selector names
         selectorNames = result.getSelectorNames();

         if (resultNodes.isEmpty() && offset > 0)
         {
            // collect result offset into dummy list
            collectScoreNodes(result, new ArrayList(), offset, true);
         }
         else
         {
            int start = resultNodes.size() + invalid + (int)offset;
            result.skip(start);
         }

         if (LOG.isDebugEnabled())
         {
            time = System.currentTimeMillis();
         }
         collectScoreNodes(result, resultNodes, maxResultSize,false);
         if (LOG.isDebugEnabled())
         {
            LOG.debug("retrieved ScoreNodes in {} ms", new Long(System.currentTimeMillis() - time));
         }

         // update numResults
         numResults = result.getSize();
      }
      catch (IndexOfflineIOException e)
      {
         throw new IndexOfflineRepositoryException(e.getMessage(), e);
      }
      catch (IOException e)
      {
         LOG.error("Exception while executing query: ", e);
         // todo throw?
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
               LOG.warn("Unable to close query result: " + e);
            }
         }
      }
   }

   /**
    * Collect score nodes from <code>hits</code> into the <code>collector</code>
    * list until the size of <code>collector</code> reaches <code>maxResults</code>
    * or there are not more results.
    *
    * @param hits the raw hits.
    * @param collector where the access checked score nodes are collected.
    * @param maxResults the maximum number of results in the collector.
    * @param isOffset if true the access is checked for the offset result.
    * @throws IOException if an error occurs while reading from hits.
    * @throws RepositoryException if an error occurs while checking access rights.
    */
   private void collectScoreNodes(MultiColumnQueryHits hits, List collector, long maxResults, boolean isOffset) throws IOException,
      RepositoryException
   {
      while (collector.size() < maxResults)
      {
         ScoreNode[] sn = hits.nextScoreNodes();
         if (sn == null)
         {
            // no more results
            break;
         }
         // check access
         if ((!docOrder && !isOffset ) || isAccessGranted(sn))
         {
            collector.add(sn);
         }
         else
         {
            invalid++;
         }
      }
   }

   /**
    * Checks if access is granted to all <code>nodes</code>.
    *
    * @param nodes the nodes to check.
    * @return <code>true</code> if read access is granted to all
    *         <code>nodes</code>.
    * @throws RepositoryException if an error occurs while checking access
    *                             rights.
    */
   private boolean isAccessGranted(ScoreNode[] nodes) throws RepositoryException
   {
      if (isSystemSession)
      {
         return true;
      }
      for (int i = 0; i < nodes.length; i++)
      {
         try
         {
            //if (nodes[i] != null && !accessMgr.isGranted(nodes[i].getNodeId(), PermissionType.READ)) {
            if (nodes[i] != null)
            {
               NodeData nodeData = (NodeData)itemMgr.getItemData(nodes[i].getNodeId());
               if (nodeData == null
                  || !accessMgr.hasPermission(nodeData.getACL(), PermissionType.READ, session.getUserState()
                     .getIdentity()))
               {
                  return false;
               }
            }
         }
         catch (ItemNotFoundException e)
         {
            if (LOG.isTraceEnabled())
            {
               LOG.trace("An exception occurred: " + e.getMessage());
            }
         }
      }
      return true;
   }

   /**
    * Returns the total number of hits. This is the number of results you
    * will get get if you don't set any limit or offset. Keep in mind that this
    * number may get smaller if nodes are found in the result set which the
    * current session has no permission to access. This method may return
    * <code>-1</code> if the total size is unknown.
    *
    * @return the total number of hits.
    */
   public int getTotalSize()
   {
      if (numResults == -1)
      {
         return -1;
      }
      else
      {
         return numResults - invalid;
      }
   }

   private final class LazyScoreNodeIteratorImpl implements ScoreNodeIterator
   {

      private int position = -1;

      private boolean initialized = false;

      private ScoreNode[] next;

      public ScoreNode[] nextScoreNodes()
      {
         initialize();
         if (next == null)
         {
            throw new NoSuchElementException();
         }
         ScoreNode[] sn = next;
         fetchNext();
         return sn;
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
         if (skipNum == 0)
         {
            // do nothing
         }
         else
         {
            // attempt to get enough results
            long expectedPosition = position + skipNum;
            while (position < expectedPosition)
            {
               fetchNext();
               if (next == null)
               {
                  // not enough results after getResults()
                  throw new NoSuchElementException();
               }
            }
         }
      }

      /**
       * 
       * @see org.exoplatform.services.jcr.impl.core.query.lucene.TwoWayRangeIterator#skipBack(long)
       */
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
       * {@inheritDoc}
       * <p/>
       * This value may shrink when the query result encounters non-existing
       * nodes or the session does not have access to a node.
       */
      public long getSize()
      {
         int total = getTotalSize();
         if (total == -1)
         {
            return -1;
         }
         long size = total - offset;
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
         return nextScoreNodes();
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
       * Fetches the next node to return by this iterator. If this method
       * returns and {@link #next} is <code>null</code> then there is no next
       * node.
       */
      private void fetchNext()
      {
         next = null;
         int nextPos = position + 1;
         while (next == null)
         {
            if (nextPos >= resultNodes.size())
            {
               // quick check if there are more results at all
               // this check is only possible if we have numResults
               if (numResults != -1 && (nextPos + invalid) >= numResults)
               {
                  break;
               }

               // fetch more results
               try
               {
                  int num;
                  if (resultNodes.size() == 0)
                  {
                     num = index.getResultFetchSize();
                  }
                  else
                  {
                     num = resultNodes.size() * 2;
                  }
                  getResults(num);
               }
               catch (RepositoryException e)
               {
                  LOG.warn("Exception getting more results: " + e);
               }
               // check again
               if (nextPos >= resultNodes.size())
               {
                  // no more valid results
                  break;
               }
            }
            next = (ScoreNode[])resultNodes.get(nextPos);
            try
            {
               if (!isAccessGranted(next))
               {
                  next = null;
                  invalid++;
                  resultNodes.remove(nextPos);
                  if (LOG.isDebugEnabled())
                  {
                     LOG.debug("The node is invalid since we don't have sufficient rights to access it, "
                        + "it will be removed from the results set");
                  }
               }
            }
            catch (RepositoryException e)
            {
               LOG.error("Could not check access permission", e);
               break;
            }
         }
         position++;
      }
   }
}
