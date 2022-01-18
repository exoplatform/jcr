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
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.Weight;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.query.LocationStepQueryNode;
import org.exoplatform.services.jcr.impl.core.query.lucene.hits.AbstractHitCollector;
import org.exoplatform.services.jcr.impl.core.query.lucene.hits.AdaptingHits;
import org.exoplatform.services.jcr.impl.core.query.lucene.hits.Hits;
import org.exoplatform.services.jcr.impl.core.query.lucene.hits.ScorerHits;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;

/**
 * Implements a lucene <code>Query</code> which returns the child nodes of the
 * nodes selected by another <code>Query</code>.
 */
class ChildAxisQuery extends Query implements JcrQuery
{

   /**
    * The serial version UID
    */
   private static final long serialVersionUID = -5244160386770496131L;

   /**
    * The logger instance for this class.
    */
   private static final Log        LOG                    = ExoLogger.getLogger("exo.jcr.component.core.ChildAxisQuery");

   /**
    * Threshold when children calculation is switched to
    * {@link HierarchyResolvingChildrenCalculator}.
    */
   private static int CONTEXT_SIZE_THRESHOLD = 10;

   /**
    * The item state manager containing persistent item states.
    */
   private final ItemDataConsumer itemMgr;

   /**
    * The context query
    */
   private Query contextQuery;

   /**
    * The nameTest to apply on the child axis, or <code>null</code> if all
    * child nodes should be selected.
    */
   private final InternalQName nameTest;

   /**
    * The context position for the selected child node, or
    * {@link LocationStepQueryNode#NONE} if no position is specified.
    */
   private final int position;

   /**
    * The index format version.
    */
   private final IndexFormatVersion version;

   /**
    * The internal namespace mappings.
    */
   private final NamespaceMappings nsMappings;

   /**
    * The scorer of the context query
    */
   private Scorer contextScorer;

   /**
    * The scorer of the name test query
    */
   private Scorer nameTestScorer;

   private IndexingConfiguration indexConfig;

   /**
    * Creates a new <code>ChildAxisQuery</code> based on a <code>context</code>
    * query.
    *
    * @param itemMgr the item state manager.
    * @param context the context for this query.
    * @param nameTest a name test or <code>null</code> if any child node is
    * selected.
    * @param version the index format version.
    * @param nsMappings the internal namespace mappings.
    * @param indexConfig
    */
   ChildAxisQuery(ItemDataConsumer itemMgr, Query context, InternalQName nameTest, IndexFormatVersion version,
      NamespaceMappings nsMappings, IndexingConfiguration indexConfig)
   {
      this(itemMgr, context, nameTest, LocationStepQueryNode.NONE, version, nsMappings, indexConfig);
   }

   /**
    * Creates a new <code>ChildAxisQuery</code> based on a <code>context</code>
    * query.
    *
    * @param itemMgr the item state manager.
    * @param context the context for this query.
    * @param nameTest a name test or <code>null</code> if any child node is
    * selected.
    * @param position the context position of the child node to select. If
    * <code>position</code> is {@link LocationStepQueryNode#NONE}, the context
    * position of the child node is not checked.
    * @param version the index format version.
    * @param nsMapping the internal namespace mappings.
    * @param indexConfig
    */
   ChildAxisQuery(ItemDataConsumer itemMgr, Query context, InternalQName nameTest, int position,
      IndexFormatVersion version, NamespaceMappings nsMapping, IndexingConfiguration indexConfig)
   {
      this.itemMgr = itemMgr;
      this.contextQuery = context;
      this.nameTest = nameTest;
      this.position = position;
      this.version = version;
      this.nsMappings = nsMapping;
      this.indexConfig = indexConfig;
   }

   /**
    * @return the context query of this child axis query.
    */
   Query getContextQuery()
   {
      return contextQuery;
   }

   /**
    * @return <code>true</code> if this child axis query matches any child
    *         node; <code>false</code> otherwise.
    */
   boolean matchesAnyChildNode()
   {
      return nameTest == null && position == LocationStepQueryNode.NONE;
   }

   /**
    * @return the name test or <code>null</code> if none was specified.
    */
   InternalQName getNameTest()
   {
      return nameTest;
   }

   /**
    * @return the position check or {@link LocationStepQueryNode#NONE} is none
    *         was specified.
    */
   int getPosition()
   {
      return position;
   }

   /**
    * Creates a <code>Weight</code> instance for this query.
    *
    * @param searcher the <code>Searcher</code> instance to use.
    * @return a <code>ChildAxisWeight</code>.
    */
   @Override
   public Weight createWeight(Searcher searcher)
   {
      return new ChildAxisWeight(searcher);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void extractTerms(Set<Term> terms)
   {
      contextQuery.extractTerms(terms);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Query rewrite(IndexReader reader) throws IOException
   {
      Query cQuery = contextQuery.rewrite(reader);
      // only try to compact if no position is specified
      if (position == LocationStepQueryNode.NONE)
      {
         if (cQuery instanceof DescendantSelfAxisQuery)
         {
            DescendantSelfAxisQuery dsaq = (DescendantSelfAxisQuery)cQuery;
            if (dsaq.subQueryMatchesAll())
            {
               Query sub;
               if (nameTest == null)
               {
                  sub = new MatchAllDocsQuery(indexConfig);
               }
               else
               {
                  sub = new NameQuery(nameTest, version, nsMappings);
               }
               return new DescendantSelfAxisQuery(dsaq.getContextQuery(), sub, dsaq.getMinLevels() + 1, indexConfig)
                  .rewrite(reader);
            }
         }
      }

      // if we get here we could not compact the query
      if (cQuery == contextQuery) // NOSONAR
      {
         return this;
      }
      else
      {
         return new ChildAxisQuery(itemMgr, cQuery, nameTest, position, version, nsMappings, indexConfig);
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String toString(String field)
   {
      StringBuilder sb = new StringBuilder();
      sb.append("ChildAxisQuery(");
      sb.append(contextQuery);
      sb.append(", ");
      sb.append(nameTest.getAsString());
      if (position != LocationStepQueryNode.NONE)
      {
         sb.append(", ");
         sb.append(position);
      }
      sb.append(")");
      return sb.toString();
   }

   //-------------------< JackrabbitQuery >------------------------------------

   /**
    * {@inheritDoc}
    */
   public QueryHits execute(JcrIndexSearcher searcher, SessionImpl session, Sort sort) throws IOException
   {
      if (sort.getSort().length == 0 && matchesAnyChildNode())
      {
         Query context = getContextQuery();
         return new ChildNodesQueryHits(searcher.evaluate(context), session, indexConfig);
      }
      else
      {
         return null;
      }
   }

   //-------------------< ChildAxisWeight >------------------------------------

   /**
    * The <code>Weight</code> implementation for this <code>ChildAxisQuery</code>.
    */
   private class ChildAxisWeight extends Weight
   {

      private static final long serialVersionUID = -2558140386233461135L;

      /**
       * The searcher in use
       */
      private final Searcher searcher;

      /**
       * Creates a new <code>ChildAxisWeight</code> instance using
       * <code>searcher</code>.
       *
       * @param searcher a <code>Searcher</code> instance.
       */
      private ChildAxisWeight(Searcher searcher)
      {
         this.searcher = searcher;
      }

      /**
       * Returns this <code>ChildAxisQuery</code>.
       *
       * @return this <code>ChildAxisQuery</code>.
       */
      @Override
      public Query getQuery()
      {
         return ChildAxisQuery.this;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public float getValue()
      {
         return 1.0f;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public float sumOfSquaredWeights() throws IOException
      {
         return 1.0f;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public void normalize(float norm)
      {
      }

      /**
       * Creates a scorer for this <code>ChildAxisQuery</code>.
       *
       * @param reader a reader for accessing the index.
       * @return a <code>ChildAxisScorer</code>.
       * @throws IOException if an error occurs while reading from the index.
       */
      @Override
      public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder, boolean topScorer) throws IOException
      {
         contextScorer = contextQuery.weight(searcher).scorer(reader, scoreDocsInOrder, topScorer);
         if (nameTest != null)
         {
            nameTestScorer =
               new NameQuery(nameTest, version, nsMappings).weight(searcher)
                  .scorer(reader, scoreDocsInOrder, topScorer);
         }
         return new ChildAxisScorer(searcher.getSimilarity(), reader, (HierarchyResolver)reader);
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public Explanation explain(IndexReader reader, int doc) throws IOException
      {
         return new Explanation();
      }
   }

   //----------------------< ChildAxisScorer >---------------------------------

   /**
    * Implements a <code>Scorer</code> for this <code>ChildAxisQuery</code>.
    */
   private class ChildAxisScorer extends Scorer
   {

      /**
       * An <code>IndexReader</code> to access the index.
       */
      private final IndexReader reader;

      /**
       * The <code>HierarchyResolver</code> of the index.
       */
      private final HierarchyResolver hResolver;

      /**
       * The next document id to return
       */
      private int nextDoc = -1;

      /**
       * A <code>Hits</code> instance containing all hits
       */
      private Hits hits;

      /**
       * Creates a new <code>ChildAxisScorer</code>.
       *
       * @param similarity the <code>Similarity</code> instance to use.
       * @param reader     for index access.
       * @param hResolver  the hierarchy resolver of <code>reader</code>.
       */
      protected ChildAxisScorer(Similarity similarity, IndexReader reader, HierarchyResolver hResolver)
      {
         super(similarity);
         this.reader = reader;
         this.hResolver = hResolver;
      }

      @Override
      public int nextDoc() throws IOException
      {
         if (nextDoc == NO_MORE_DOCS)
         {
            return nextDoc;
         }

         calculateChildren();
         do
         {
            nextDoc = hits.next();
         }
         while (nextDoc > -1 && !indexIsValid(nextDoc));

         if (nextDoc < 0)
         {
            nextDoc = NO_MORE_DOCS;
         }
         return nextDoc;
      }

      @Override
      public int docID()
      {
         return nextDoc;
      }

      @Override
      public float score() throws IOException
      {
         return 1.0f;
      }

      @Override
      public int advance(int target) throws IOException
      {
         if (nextDoc == NO_MORE_DOCS)
         {
            return nextDoc;
         }

         calculateChildren();
         nextDoc = hits.skipTo(target);
         while (nextDoc > -1 && !indexIsValid(nextDoc))
         {
            nextDoc();
         }
         if (nextDoc < 0)
         {
            nextDoc = NO_MORE_DOCS;
         }
         return nextDoc;
      }

      private void calculateChildren() throws IOException
      {
         if (hits == null)
         {

            final ChildrenCalculator[] calc = new ChildrenCalculator[1];
            if (nameTestScorer == null)
            {
               // always use simple in that case
               calc[0] = new SimpleChildrenCalculator(reader, hResolver);
               contextScorer.score(new AbstractHitCollector()
               {
                  @Override
                  protected void collect(int doc, float score)
                  {
                     calc[0].collectContextHit(doc);
                  }
               });
            }
            else
            {
               // start simple but switch once threshold is reached
               calc[0] = new SimpleChildrenCalculator(reader, hResolver);
               contextScorer.score(new AbstractHitCollector()
               {

                  private List<Integer> docIds = new ArrayList<Integer>();

                  @Override
                  protected void collect(int doc, float score)
                  {
                     calc[0].collectContextHit(doc);
                     if (docIds != null)
                     {
                        docIds.add(doc);
                        if (docIds.size() > CONTEXT_SIZE_THRESHOLD)
                        {
                           // switch
                           calc[0] = new HierarchyResolvingChildrenCalculator(reader, hResolver);
                           for (int docId : docIds)
                           {
                              calc[0].collectContextHit(docId);
                           }
                           // indicate that we switched
                           docIds = null;
                        }
                     }
                  }
               });
            }

            hits = calc[0].getHits();
         }
      }

      private boolean indexIsValid(int i) throws IOException
      {
         if (position != LocationStepQueryNode.NONE)
         {
            Document node = reader.document(i, FieldSelectors.UUID_AND_PARENT_AND_INDEX);
            String parentId = node.get(FieldNames.PARENT);
            String id = node.get(FieldNames.UUID);
            try
            {
               if (nameTest == null)
               {
                  NodeData state = (NodeData)itemMgr.getItemData(parentId);
                  // only select this node if it is the child at
                  // specified position
                  if (position == LocationStepQueryNode.LAST)
                  {
                     // only select last
                     List<NodeData> childNodes = itemMgr.getChildNodesData(state);
                     if (childNodes.size() == 0 || !childNodes.get(childNodes.size() - 1).getIdentifier().equals(id))
                     {
                        return false;
                     }
                  }
                  else
                  {
                     List<NodeData> childNodes = itemMgr.getChildNodesData(state);
                     if (position < 1 || childNodes.size() < position
                        || !childNodes.get(position - 1).getIdentifier().equals(id))
                     {
                        return false;
                     }
                  }
               }
               else
               {
                  // select the node when its index is equal to
                  // specified position
                  if (position == LocationStepQueryNode.LAST)
                  {
                     NodeData state = (NodeData)itemMgr.getItemData(parentId);
                     // only select last

                     if (state == null)
                     {
                        // no such child node, probably deleted meanwhile
                        return false;
                     }
                     else
                     {
                        // only use the last one
                        List<NodeData> childNodes = itemMgr.getChildNodesData(state);
                        if (childNodes.size() == 0 || !childNodes.get(childNodes.size() - 1).getIdentifier().equals(id))
                        {
                           return false;
                        }
                     }
                  }
                  else if (version.getVersion() >= IndexFormatVersion.V4.getVersion())
                  {
                     if (Integer.valueOf(node.get(FieldNames.INDEX)) != position)
                        return false;
                  }
                  else
                  {
                     NodeData nodeData = (NodeData)itemMgr.getItemData(id);
                     if (nodeData == null)
                     {
                        // no such child node, probably has been deleted meanwhile
                        return false;
                     }
                     else
                     {
                        if (nodeData.getQPath().getIndex() != position)
                        {
                           return false;
                        }
                     }
                  }
               }

            }
            catch (RepositoryException e)
            {
               // ignore this node, probably has been deleted meanwhile
               return false;
            }
         }
         return true;
      }
   }

   /**
    * Base class to calculate the children for a context query.
    */
   private abstract class ChildrenCalculator
   {

      /**
       * The current index reader.
       */
      protected final IndexReader reader;

      /**
       * The current hierarchy resolver.
       */
      protected final HierarchyResolver hResolver;

      /**
       * Creates a new children calculator with the given index reader and
       * hierarchy resolver.
       *
       * @param reader the current index reader.
       * @param hResolver the current hierarchy resolver.
       */
      public ChildrenCalculator(IndexReader reader, HierarchyResolver hResolver)
      {
         this.reader = reader;
         this.hResolver = hResolver;
      }

      /**
       * Collects a context hit.
       *
       * @param doc the lucene document number of the context hit.
       */
      protected abstract void collectContextHit(int doc);

      /**
       * @return the hits that contains the children.
       * @throws IOException if an error occurs while reading from the index.
       */
      public abstract Hits getHits() throws IOException;
   }

   /**
    * An implementation of a children calculator using the item state manager.
    */
   private final class SimpleChildrenCalculator extends ChildrenCalculator
   {

      /**
       * The context hits.
       */
      private final Hits contextHits = new AdaptingHits();

      /**
       * Creates a new simple children calculator.
       *
       * @param reader the current index reader.
       * @param hResolver the current hierarchy resolver.
       */
      public SimpleChildrenCalculator(IndexReader reader, HierarchyResolver hResolver)
      {
         super(reader, hResolver);
      }

      /**
       * {@inheritDoc}
       */
      @Override
      protected void collectContextHit(int doc)
      {
         contextHits.set(doc);
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public Hits getHits() throws IOException
      {
         // read the uuids of the context nodes
         Map<Integer, String> uuids = new HashMap<Integer, String>();
         for (int i = contextHits.next(); i > -1; i = contextHits.next())
         {
            String uuid = reader.document(i, FieldSelectors.UUID).get(FieldNames.UUID);
            uuids.put(new Integer(i), uuid);
         }

         // get child node entries for each hit
         Hits childrenHits = new AdaptingHits();
         for (Iterator<String> it = uuids.values().iterator(); it.hasNext();)
         {
            String uuid = it.next();
            try
            {
               if (nameTest != null && version.getVersion() >= IndexFormatVersion.V4.getVersion())
               {
                  StringBuilder path = new StringBuilder(256);
                  path.append(uuid == null ? "" : uuid).append('/').append(nameTest.getAsString());
                  TermDocs docs = reader.termDocs(new Term(FieldNames.PATH, path.toString()));
                  try
                  {
                     while (docs.next())
                     {
                        childrenHits.set(docs.doc());
                     }

                  }
                  finally
                  {
                     docs.close();
                  }
               }
               else
               {
                  long time = System.currentTimeMillis();
                  NodeData state = (NodeData)itemMgr.getItemData(uuid);
                  time = System.currentTimeMillis() - time;
                  LOG.debug("got NodeState with id {} in {} ms.", uuid, new Long(time));
                  Iterator<NodeData> entries;
                  if (nameTest != null)
                  {
                     List<NodeData> datas = new ArrayList<NodeData>();
                     if (indexConfig == null || !indexConfig.isExcluded(state))
                     {
                        List<NodeData> childs = itemMgr.getChildNodesData(state);

                        if (childs != null)
                        {
                           for (NodeData nodeData : childs)
                           {
                              if (nameTest.equals(nodeData.getQPath().getName()))
                                 datas.add(nodeData);
                           }
                        }
                     }
                     entries = datas.iterator();
                  }
                  else
                  {
                     // get all children
                     entries = itemMgr.getChildNodesData(state).iterator();
                  }
                  while (entries.hasNext())
                  {
                     NodeData nodeData = entries.next();
                     if (indexConfig == null || !indexConfig.isExcluded(nodeData))
                     {
                        String childId = nodeData.getIdentifier();
                        Term uuidTerm = new Term(FieldNames.UUID, childId);
                        TermDocs docs = reader.termDocs(uuidTerm);
                        try
                        {
                           if (docs.next())
                           {
                              childrenHits.set(docs.doc());
                           }
                        }
                        finally
                        {
                           docs.close();
                        }
                     }
                  }
               }
            }
            catch (RepositoryException e)
            {
               if (LOG.isTraceEnabled())
               {
                  LOG.trace("An exception occurred: " + e.getMessage());
               }
            }
         }
         return childrenHits;
      }
   }

   /**
    * An implementation of a children calculator that uses the hierarchy
    * resolver. This implementation requires that
    * {@link ChildAxisQuery#nameTestScorer} is non null.
    */
   private final class HierarchyResolvingChildrenCalculator extends ChildrenCalculator
   {

      /**
       * The document numbers of the context hits.
       */
      private final Set<Integer> docIds = new HashSet<Integer>();

      /**
       * Creates a new hierarchy resolving children calculator.
       *
       * @param reader the current index reader.
       * @param hResolver the current hierarchy resolver.
       */
      public HierarchyResolvingChildrenCalculator(IndexReader reader, HierarchyResolver hResolver)
      {
         super(reader, hResolver);
      }

      /**
       * {@inheritDoc}
       */
      @Override
      protected void collectContextHit(int doc)
      {
         docIds.add(new Integer(doc));
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public Hits getHits() throws IOException
      {
         long time = 0;
         if (LOG.isDebugEnabled())
         {
            time = System.currentTimeMillis();
         }
         Hits childrenHits = new AdaptingHits();
         Hits nameHits = new ScorerHits(nameTestScorer);
         int[] docs = new int[1];
         for (int h = nameHits.next(); h > -1; h = nameHits.next())
         {
            docs = hResolver.getParents(h, docs);
            if (docs.length == 1)
            {
               // optimize single value
               if (docIds.contains(new Integer(docs[0])))
               {
                  childrenHits.set(h);
               }
            }
            else
            {
               for (int i = 0; i < docs.length; i++)
               {
                  if (docIds.contains(new Integer(docs[i])))
                  {
                     childrenHits.set(h);
                  }
               }
            }
         }
         if (LOG.isDebugEnabled())
         {
            time = System.currentTimeMillis() - time;
            LOG.debug("Filtered hits in {} ms.", new Long(time));
         }
         return childrenHits;
      }
   }
}
