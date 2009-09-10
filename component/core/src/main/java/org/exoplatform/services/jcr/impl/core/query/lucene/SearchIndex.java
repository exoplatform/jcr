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

import org.apache.commons.collections.iterators.AbstractIteratorDecorator;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.document.DocumentReaderService;
import org.exoplatform.services.jcr.config.QueryHandlerEntry;
import org.exoplatform.services.jcr.config.QueryHandlerEntryWrapper;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.SessionDataManager;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.query.DefaultQueryNodeFactory;
import org.exoplatform.services.jcr.impl.core.query.ErrorLog;
import org.exoplatform.services.jcr.impl.core.query.ExecutableQuery;
import org.exoplatform.services.jcr.impl.core.query.QueryHandler;
import org.exoplatform.services.jcr.impl.core.query.QueryHandlerContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;

/**
 * Implements a {@link org.apache.jackrabbit.core.query.QueryHandler} using
 * Lucene.
 */
public class SearchIndex implements QueryHandler
{

   private static final DefaultQueryNodeFactory DEFAULT_QUERY_NODE_FACTORY = new DefaultQueryNodeFactory();

   /** The logger instance for this class */
   private static final Log log = ExoLogger.getLogger(SearchIndex.class);

   /**
    * Name of the file to persist search internal namespace mappings.
    */
   private static final String NS_MAPPING_FILE = "ns_mappings.properties";

   /**
    * Default name of the error log file
    */
   private static final String ERROR_LOG = "error.log";

   /**
    * Indicates if this <code>SearchIndex</code> is closed and cannot be used
    * anymore.
    */
   private boolean closed = false;

   private QueryHandlerContext context;

   /**
    * Text extractor for extracting text content of binary properties.
    */
   private DocumentReaderService extractor;

   /**
    * The actual index
    */
   private MultiIndex index;

   /**
    * Indicates the index format version which is relevant to a <b>query</b>.
    * This value may be different from what
    * {@link MultiIndex#getIndexFormatVersion()} returns because queries may be
    * executed on two physical indexes with different formats. Index format
    * versions are considered backward compatible. That is, the lower version of
    * the two physical indexes is used for querying.
    */
   private IndexFormatVersion indexFormatVersion;

   /**
    * The indexing configuration.
    */
   private IndexingConfiguration indexingConfig;

   /**
    * The name and path resolver used internally.
    */
   private LocationFactory npResolver;

   /**
    * The namespace mappings used internally.
    */
   private NamespaceMappings nsMappings;

   private final QueryHandlerEntryWrapper queryHandlerConfig;

   /**
    * The spell checker for this query handler or <code>null</code> if none is
    * configured.
    */
   private SpellChecker spellChecker;

   /**
    * The currently set synonym provider.
    */
   private SynonymProvider synProvider;

   private File indexDirectory;

   /**
    * The ErrorLog of this <code>MultiIndex</code>. All changes that must be in
    * index but interrupted by IOException are here.
    */
   private ErrorLog errorLog;

   private final ConfigurationManager cfm;

   public SearchIndex(QueryHandlerEntry queryHandlerConfig, ConfigurationManager cfm)
   {
      this.queryHandlerConfig = new QueryHandlerEntryWrapper(queryHandlerConfig);
      this.cfm = cfm;
   }

   /**
    * Adds the <code>node</code> to the search index.
    * 
    * @param node the node to add.
    * @throws RepositoryException if an error occurs while indexing the node.
    * @throws IOException if an error occurs while adding the node to the index.
    */
   public void addNode(NodeData node) throws RepositoryException, IOException
   {
      throw new UnsupportedOperationException("addNode");
   }

   /**
    * Creates an excerpt provider for the given <code>query</code>.
    * 
    * @param query the query.
    * @return an excerpt provider for the given <code>query</code>.
    * @throws IOException if the provider cannot be created.
    */
   public ExcerptProvider createExcerptProvider(Query query) throws IOException
   {
      ExcerptProvider ep = queryHandlerConfig.createExcerptProvider(query);
      ep.init(query, this);
      return ep;
   }

   /**
    * Creates a new query by specifying the query statement itself and the
    * language in which the query is stated. If the query statement is
    * syntactically invalid, given the language specified, an
    * InvalidQueryException is thrown. <code>language</code> must specify a query
    * language string from among those returned by
    * QueryManager.getSupportedQueryLanguages(); if it is not then an
    * <code>InvalidQueryException</code> is thrown.
    * 
    * @param session the session of the current user creating the query object.
    * @param itemMgr the item manager of the current user.
    * @param statement the query statement.
    * @param language the syntax of the query statement.
    * @throws InvalidQueryException if statement is invalid or language is
    *           unsupported.
    * @return A <code>Query</code> object.
    */
   public ExecutableQuery createExecutableQuery(SessionImpl session, SessionDataManager itemMgr, String statement,
      String language) throws InvalidQueryException
   {
      QueryImpl query =
         new QueryImpl(session, itemMgr, this, getContext().getPropertyTypeRegistry(), statement, language,
            getQueryNodeFactory());
      query.setRespectDocumentOrder(queryHandlerConfig.getDocumentOrder());
      return query;
   }

   public org.exoplatform.services.jcr.impl.core.query.AbstractQueryImpl createQueryInstance()
      throws RepositoryException
   {
      try
      {
         Object obj = Class.forName(queryHandlerConfig.getQueryClass()).newInstance();
         if (obj instanceof org.exoplatform.services.jcr.impl.core.query.AbstractQueryImpl)
         {
            return (org.exoplatform.services.jcr.impl.core.query.AbstractQueryImpl)obj;
         }
         throw new IllegalArgumentException(queryHandlerConfig.getQueryClass() + " is not of type "
            + AbstractQueryImpl.class.getName());

      }
      catch (Throwable t)
      {
         throw new RepositoryException("Unable to create query: " + t.toString());
      }
   }

   /**
    * Removes the node with <code>uuid</code> from the search index.
    * 
    * @param id the id of the node to remove from the index.
    * @throws IOException if an error occurs while removing the node from the
    *           index.
    */
   public void deleteNode(String id) throws IOException
   {
      throw new UnsupportedOperationException("deleteNode");
   }

   public QueryHits executeQuery(Query query, boolean needsSystemTree, InternalQName[] orderProps, boolean[] orderSpecs)
      throws IOException
   {
      checkOpen();
      SortField[] sortFields = createSortFields(orderProps, orderSpecs);

      IndexReader reader = getIndexReader(needsSystemTree);
      IndexSearcher searcher = new IndexSearcher(reader);
      Hits hits;
      if (sortFields.length > 0)
      {
         hits = searcher.search(query, new Sort(sortFields));
      }
      else
      {
         hits = searcher.search(query);
      }
      return new QueryHits(hits, reader);
   }

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
    * Returns the index format version that this search index is able to support
    * when a query is executed on this index.
    * 
    * @return the index format version for this search index.
    */
   public IndexFormatVersion getIndexFormatVersion()
   {
      if (indexFormatVersion == null)
      {
         if (getContext().getParentHandler() instanceof SearchIndex)
         {
            SearchIndex parent = (SearchIndex)getContext().getParentHandler();
            if (parent.getIndexFormatVersion().getVersion() < index.getIndexFormatVersion().getVersion())
            {
               indexFormatVersion = parent.getIndexFormatVersion();
            }
            else
            {
               indexFormatVersion = index.getIndexFormatVersion();
            }
         }
         else
         {
            indexFormatVersion = index.getIndexFormatVersion();
         }
      }
      return indexFormatVersion;
   }

   /**
    * @return the indexing configuration or <code>null</code> if there is none.
    */
   public IndexingConfiguration getIndexingConfig()
   {
      return indexingConfig;
   }

   /**
    * Returns an index reader for this search index. The caller of this method is
    * responsible for closing the index reader when he is finished using it.
    * 
    * @return an index reader for this search index.
    * @throws IOException the index reader cannot be obtained.
    */
   public IndexReader getIndexReader() throws IOException
   {
      return getIndexReader(true);
   }

   // --------------------------< properties >----------------------------------

   /**
    * Returns an index reader for this search index. The caller of this method is
    * responsible for closing the index reader when he is finished using it.
    * 
    * @param includeSystemIndex if <code>true</code> the index reader will cover
    *          the complete workspace. If <code>false</code> the returned index
    *          reader will not contains any nodes under /jcr:system.
    * @return an index reader for this search index.
    * @throws IOException the index reader cannot be obtained.
    */
   public IndexReader getIndexReader(boolean includeSystemIndex) throws IOException
   {
      QueryHandler parentHandler = getContext().getParentHandler();
      CachingMultiIndexReader parentReader = null;
      if (parentHandler instanceof SearchIndex && includeSystemIndex)
      {
         parentReader = ((SearchIndex)parentHandler).index.getIndexReader();
      }

      IndexReader reader;
      if (parentReader != null)
      {
         CachingMultiIndexReader[] readers = {index.getIndexReader(), parentReader};
         reader = new CombinedIndexReader(readers);
      }
      else
      {
         reader = index.getIndexReader();
      }
      return new JackrabbitIndexReader(reader);

   }

   /**
    * Returns the namespace mappings for the internal representation.
    * 
    * @return the namespace mappings for the internal representation.
    */
   public NamespaceMappings getNamespaceMappings()
   {
      return nsMappings;
   }

   /**
    * @return the spell checker of this search index. If none is configured this
    *         method returns <code>null</code>.
    */
   public SpellChecker getSpellChecker()
   {
      return spellChecker;
   }

   /**
    * @return the synonym provider of this search index. If none is set for this
    *         search index the synonym provider of the parent handler is returned
    *         if there is any.
    */
   public SynonymProvider getSynonymProvider()
   {
      if (synProvider != null)
      {
         return synProvider;
      }
      QueryHandler handler = getContext().getParentHandler();
      if (handler instanceof SearchIndex)
         return ((SearchIndex)handler).getSynonymProvider();
      return null;

   }

   /**
    * Returns the analyzer in use for indexing.
    * 
    * @return the analyzer in use for indexing.
    */
   public Analyzer getTextAnalyzer()
   {
      return queryHandlerConfig.getAnalyzer();
   }

   /**
    * Initializes this query handler by setting all properties in this class with
    * appropriate parameter values.
    * 
    * @param context the context for this query handler.
    */
   public final void setContext(QueryHandlerContext queryHandlerContext) throws IOException
   {
      this.context = queryHandlerContext;
   }

   /**
    * Initializes this <code>QueryHandler</code>. This implementation requires
    * that a path parameter is set in the configuration. If this condition is not
    * met, a <code>IOException</code> is thrown.
   * @throws IOException 
    * 
    * @throws IOException if an error occurs while initializing this handler.
   * @throws RepositoryException 
   * @throws RepositoryConfigurationException 
    */
   public void init() throws IOException, RepositoryException, RepositoryConfigurationException
   {
      //try {
      String indexDir = context.getIndexDirectory();
      if (indexDir != null)
      {
         indexDir = indexDir.replace("${java.io.tmpdir}", System.getProperty("java.io.tmpdir"));
         indexDirectory = new File(indexDir);
         if (!indexDirectory.exists())
            if (!indexDirectory.mkdirs())
               throw new RepositoryException("fail to create index dir " + indexDir);
      }
      else
      {
         throw new IOException("SearchIndex requires 'path' parameter in configuration!");
      }

      extractor = context.getExtractor();
      synProvider = queryHandlerConfig.createSynonymProvider(cfm);
      // File indexDirFile = context.getFileSystem();

      if (context.getParentHandler() instanceof SearchIndex)
      {
         // use system namespace mappings
         SearchIndex sysIndex = (SearchIndex)context.getParentHandler();
         nsMappings = sysIndex.getNamespaceMappings();
      }
      else
      {
         // read local namespace mappings
         File mapFile = new File(indexDirectory, NS_MAPPING_FILE);
         if (mapFile.exists())
         {
            // be backward compatible and use ns_mappings.properties from
            // index folder
            nsMappings = new FileBasedNamespaceMappings(mapFile);
         }
         else
         {
            // otherwise use repository wide stable index prefix from
            // namespace registry
            nsMappings = new NSRegistryBasedNamespaceMappings(context.getNamespaceRegistry());
         }
      }
      npResolver = new LocationFactory(nsMappings);

      indexingConfig = queryHandlerConfig.createIndexingConfiguration(nsMappings, context, cfm);

      queryHandlerConfig.getAnalyzer().setIndexingConfig(indexingConfig);

      index = new MultiIndex(indexDirectory, this/* , excludedIDs */, nsMappings);

      if (index.numDocs() == 0)
      {
         index.createInitialIndex(context.getItemStateManager(), context.getRootNodeIdentifer());
      }
      if (queryHandlerConfig.isConsistencyCheckEnabled()
         && (index.getRedoLogApplied() || queryHandlerConfig.isForceConsistencyCheck()))
      {
         log.info("Running consistency check... ");

         ConsistencyCheck check = ConsistencyCheck.run(index, context.getItemStateManager());
         if (queryHandlerConfig.getAutoRepair())
         {
            check.repair(true);
         }
         else
         {
            List<ConsistencyCheckError> errors = check.getErrors();
            if (errors.size() == 0)
            {
               log.info("No errors detected.");
            }
            for (Iterator<ConsistencyCheckError> it = errors.iterator(); it.hasNext();)
            {
               ConsistencyCheckError err = it.next();
               log.info(err.toString());
            }
         }
      }

      // initialize spell checker
      spellChecker = queryHandlerConfig.createSpellChecker(this);

      log.info("Index initialized: " + queryHandlerConfig.getIndexDir() + " Version: " + index.getIndexFormatVersion()
         + "");

      File file = new File(indexDir, ERROR_LOG);
      errorLog = new ErrorLog(file, queryHandlerConfig.getErrorLogSize());
      // reprocess any notfinished notifies;
      recoverErrorLog(errorLog);

      //    } catch (IOException e) {
      //      log.error(e.getLocalizedMessage());
      //      throw new RuntimeException(e);
      //    } catch (RepositoryException e) {
      //      log.error(e.getLocalizedMessage());
      //      throw new RuntimeException(e);
      //    } catch (RepositoryConfigurationException e) {
      //      log.error(e.getLocalizedMessage());
      //      throw new RuntimeException(e);
      //    }
   }

   private void recoverErrorLog(ErrorLog errlog) throws IOException, RepositoryException
   {
      final Set<String> rem = new HashSet<String>();
      final Set<String> add = new HashSet<String>();

      errlog.readChanges(rem, add);

      // check is any notifies in log
      if (rem.isEmpty() && add.isEmpty())
      {
         // there is no sense to continue
         return;
      }

      Iterator<String> removedStates = rem.iterator();

      // make a new iterator;
      Iterator<NodeData> addedStates = new Iterator<NodeData>()
      {
         private final Iterator<String> iter = add.iterator();

         public boolean hasNext()
         {
            return iter.hasNext();
         }

         public NodeData next()
         {
            String id;
            // we have to iterrate through items till will meet ones existing in
            // workspace
            while (iter.hasNext())
            {
               id = iter.next();

               try
               {
                  ItemData item = context.getItemStateManager().getItemData(id);
                  if (item != null)
                  {
                     if (item.isNode())
                     {
                        return (NodeData)item; // return node here
                     }
                     else
                        log.warn("Node expected but property found with id " + id + ". Skipping "
                           + item.getQPath().getAsString());
                  }
                  else
                  {
                     log.warn("Unable to recovery node index " + id + ". Node not found.");
                  }
               }
               catch (RepositoryException e)
               {
                  log.error("ErrorLog recovery error. Item id " + id + ". " + e, e);
               }
            }

            return null;
         }

         public void remove()
         {
            throw new UnsupportedOperationException();
         }
      };

      updateNodes(removedStates, addedStates);

      errlog.clear();
   }

   /**
    * Closes this <code>QueryHandler</code> and frees resources attached to this
    * handler.
    */
   public void close()
   {
      if (spellChecker != null)
      {
         spellChecker.close();
      }
      index.close();
      getContext().destroy();
      closed = true;

      log.info("Index closed: " + indexDirectory.getAbsolutePath());
   }

   /**
    * This implementation forwards the call to
    * {@link MultiIndex#update(java.util.Iterator, java.util.Iterator)} and
    * transforms the two iterators to the required types.
    * 
    * @param remove uuids of nodes to remove.
    * @param add NodeStates to add. Calls to <code>next()</code> on this iterator
    *          may return <code>null</code>, to indicate that a node could not be
    *          indexed successfully.
    * @throws RepositoryException if an error occurs while indexing a node.
    * @throws IOException if an error occurs while updating the index.
    */
   public void updateNodes(final Iterator<String> remove, final Iterator<NodeData> add) throws RepositoryException,
      IOException
   {

      checkOpen();

      final Map<String, NodeData> aggregateRoots = new HashMap<String, NodeData>();
      final Set<String> removedNodeIds = new HashSet<String>();
      final Set<String> addedNodeIds = new HashSet<String>();

      index.update(new AbstractIteratorDecorator(remove)
      {
         public Object next()
         {
            String nodeId = (String)super.next();
            removedNodeIds.add(nodeId);
            return nodeId;
         }
      }, new AbstractIteratorDecorator(add)
      {
         public Object next()
         {
            NodeData state = (NodeData)super.next();
            if (state == null)
            {
               return null;
            }
            addedNodeIds.add(state.getIdentifier());
            removedNodeIds.remove(state.getIdentifier());
            Document doc = null;
            try
            {
               doc = createDocument(state, getNamespaceMappings(), index.getIndexFormatVersion());
               retrieveAggregateRoot(state, aggregateRoots);
            }
            catch (RepositoryException e)
            {
               log
                  .warn("Exception while creating document for node: " + state.getIdentifier() + ": " + e.toString(), e);

            }
            return doc;
         }
      });

      // remove any aggregateRoot nodes that are new
      // and therefore already up-to-date
      aggregateRoots.keySet().removeAll(addedNodeIds);

      // based on removed NodeIds get affected aggregate root nodes
      retrieveAggregateRoot(removedNodeIds, aggregateRoots);

      // update aggregates if there are any affected
      if (aggregateRoots.size() > 0)
      {
         index.update(new AbstractIteratorDecorator(aggregateRoots.keySet().iterator())
         {
            public Object next()
            {
               return super.next();
            }
         }, new AbstractIteratorDecorator(aggregateRoots.values().iterator())
         {
            public Object next()
            {
               NodeData state = (NodeData)super.next();
               try
               {
                  return createDocument(state, getNamespaceMappings(), index.getIndexFormatVersion());
               }
               catch (RepositoryException e)
               {
                  log
                     .warn("Exception while creating document for node: " + state.getIdentifier() + ": " + e.toString());
               }
               return null;
            }
         });
      }

   }

   /**
    * Creates a lucene <code>Document</code> for a node state using the namespace
    * mappings <code>nsMappings</code>.
    * 
    * @param node the node state to index.
    * @param nsMappings the namespace mappings of the search index.
    * @param indexFormatVersion the index format version that should be used to
    *          index the passed node state.
    * @return a lucene <code>Document</code> that contains all properties of
    *         <code>node</code>.
    * @throws RepositoryException if an error occurs while indexing the
    *           <code>node</code>.
    */
   protected Document createDocument(NodeData node, NamespaceMappings nsMappings, IndexFormatVersion indexFormatVersion)
      throws RepositoryException
   {
      NodeIndexer indexer = new NodeIndexer(node, getContext().getItemStateManager(), nsMappings, extractor);
      indexer.setSupportHighlighting(queryHandlerConfig.getSupportHighlighting());
      indexer.setIndexingConfiguration(indexingConfig);
      indexer.setIndexFormatVersion(indexFormatVersion);
      Document doc = indexer.createDoc();
      mergeAggregatedNodeIndexes(node, doc);
      return doc;
   }

   // ----------------------------< internal >----------------------------------

   /**
    * Creates the SortFields for the order properties.
    * 
    * @param orderProps the order properties.
    * @param orderSpecs the order specs for the properties.
    * @return an array of sort fields
    */
   protected SortField[] createSortFields(InternalQName[] orderProps, boolean[] orderSpecs)
   {
      List<SortField> sortFields = new ArrayList<SortField>();
      for (int i = 0; i < orderProps.length; i++)
      {
         String prop = null;
         if (Constants.JCR_SCORE.equals(orderProps[i]))
         {
            // order on jcr:score does not use the natural order as
            // implemented in lucene. score ascending in lucene means that
            // higher scores are first. JCR specs that lower score values
            // are first.
            sortFields.add(new SortField(null, SortField.SCORE, orderSpecs[i]));
         }
         else
         {
            try
            {
               prop = npResolver.createJCRName(orderProps[i]).getAsString();
            }
            catch (RepositoryException e)
            {
               e.printStackTrace();
               // will never happen
            }
            sortFields.add(new SortField(prop, SharedFieldSortComparator.PROPERTIES, !orderSpecs[i]));
         }
      }
      return sortFields.toArray(new SortField[sortFields.size()]);
   }

   /**
    * Returns the actual index.
    * 
    * @return the actual index.
    */
   protected MultiIndex getIndex()
   {
      return index;
   }

   /**
    * This method returns the QueryNodeFactory used to parse Queries. This method
    * may be overridden to provide a customized QueryNodeFactory
    */
   protected DefaultQueryNodeFactory getQueryNodeFactory()
   {
      return DEFAULT_QUERY_NODE_FACTORY;
   }

   /**
    * Merges the fulltext indexed fields of the aggregated node states into
    * <code>doc</code>.
    * 
    * @param state the node state on which <code>doc</code> was created.
    * @param doc the lucene document with index fields from <code>state</code>.
    */
   protected void mergeAggregatedNodeIndexes(NodeData state, Document doc)
   {
      if (indexingConfig != null)
      {
         AggregateRule aggregateRules[] = indexingConfig.getAggregateRules();
         if (aggregateRules == null)
         {
            return;
         }
         try
         {
            for (int i = 0; i < aggregateRules.length; i++)
            {
               NodeData[] aggregates = aggregateRules[i].getAggregatedNodeStates(state);
               if (aggregates == null)
               {
                  continue;
               }
               for (int j = 0; j < aggregates.length; j++)
               {
                  Document aDoc = createDocument(aggregates[j], getNamespaceMappings(), index.getIndexFormatVersion());
                  // transfer fields to doc if there are any
                  Field[] fulltextFields = aDoc.getFields(FieldNames.FULLTEXT);
                  if (fulltextFields != null)
                  {
                     for (int k = 0; k < fulltextFields.length; k++)
                     {
                        doc.add(fulltextFields[k]);
                     }
                     doc.add(new Field(FieldNames.AGGREGATED_NODE_UUID, aggregates[j].getIdentifier().toString(),
                        Field.Store.NO, Field.Index.NO_NORMS));
                  }
               }
               // only use first aggregate definition that matches
               break;
            }
         }
         catch (Exception e)
         {
            // do not fail if aggregate cannot be created
            log
               .warn("Exception while building indexing aggregate for " + "node with UUID: " + state.getIdentifier(), e);
         }
      }
   }

   /**
    * Retrieves the root of the indexing aggregate for <code>state</code> and
    * puts it into <code>map</code>.
    * 
    * @param state the node state for which we want to retrieve the aggregate
    *          root.
    * @param map aggregate roots are collected in this map. Key=NodeId,
    *          value=NodeState.
    */
   protected void retrieveAggregateRoot(NodeData state, Map<String, NodeData> map)
   {
      if (indexingConfig != null)
      {
         AggregateRule aggregateRules[] = indexingConfig.getAggregateRules();
         if (aggregateRules == null)
         {
            return;
         }
         try
         {
            for (int i = 0; i < aggregateRules.length; i++)
            {
               NodeData root = aggregateRules[i].getAggregateRoot(state);
               if (root != null)
               {
                  map.put(root.getIdentifier(), root);
                  break;
               }
            }
         }
         catch (Exception e)
         {
            log.warn("Unable to get aggregate root for " + state.getIdentifier(), e);
         }
      }
   }

   /**
    * Retrieves the root of the indexing aggregate for
    * <code>removedNodeIds</code> and puts it into <code>map</code>.
    * 
    * @param removedNodeIds the ids of removed nodes.
    * @param map aggregate roots are collected in this map. Key=NodeId,
    *          value=NodeState.
    */
   protected void retrieveAggregateRoot(Set<String> removedNodeIds, Map<String, NodeData> map)
   {
      if (indexingConfig != null)
      {
         AggregateRule aggregateRules[] = indexingConfig.getAggregateRules();
         if (aggregateRules == null)
         {
            return;
         }
         int found = 0;
         long time = System.currentTimeMillis();
         try
         {
            CachingMultiIndexReader reader = index.getIndexReader();
            try
            {
               Term aggregateUUIDs = new Term(FieldNames.AGGREGATED_NODE_UUID, "");
               TermDocs tDocs = reader.termDocs();
               try
               {
                  ItemDataConsumer ism = getContext().getItemStateManager();
                  for (Iterator<String> it = removedNodeIds.iterator(); it.hasNext();)
                  {
                     String id = it.next();
                     aggregateUUIDs = aggregateUUIDs.createTerm(id);
                     tDocs.seek(aggregateUUIDs);
                     while (tDocs.next())
                     {
                        Document doc = reader.document(tDocs.doc());
                        String uuid = doc.get(FieldNames.UUID);
                        ItemData itd = ism.getItemData(uuid);
                        if (itd == null)
                           continue;
                        if (!itd.isNode())
                           throw new RepositoryException("Item with id:" + uuid + " is not a node");
                        map.put(uuid, (NodeData)itd);
                        found++;
                     }
                  }
               }
               finally
               {
                  tDocs.close();
               }
            }
            finally
            {
               reader.release();
            }
         }
         catch (Exception e)
         {
            log.warn("Exception while retrieving aggregate roots", e);
         }
         time = System.currentTimeMillis() - time;
         log.debug("Retrieved " + new Integer(found) + " aggregate roots in " + new Long(time) + " ms.");
      }
   }

   /**
    * Checks if this <code>SearchIndex</code> is open, otherwise throws an
    * <code>IOException</code>.
    * 
    * @throws IOException if this <code>SearchIndex</code> had been closed.
    */
   private void checkOpen() throws IOException
   {
      if (closed)
      {
         throw new IOException("query handler closed and cannot be used anymore.");
      }
   }

   /**
    * Combines multiple {@link CachingMultiIndexReader} into a
    * <code>MultiReader</code> with {@link HierarchyResolver} support.
    */
   protected static final class CombinedIndexReader extends MultiReader implements HierarchyResolver, MultiIndexReader
   {

      /**
       * Doc number starts for each sub reader
       */
      private int[] starts;

      /**
       * The sub readers.
       */
      final private CachingMultiIndexReader[] subReaders;

      public CombinedIndexReader(CachingMultiIndexReader[] indexReaders) throws IOException
      {
         super(indexReaders);
         this.subReaders = indexReaders;
         this.starts = new int[subReaders.length + 1];

         int maxDoc = 0;
         for (int i = 0; i < subReaders.length; i++)
         {
            starts[i] = maxDoc;
            maxDoc += subReaders[i].maxDoc();
         }
         starts[subReaders.length] = maxDoc;
      }

      /**
       * {@inheritDoc}
       */
      public ForeignSegmentDocId createDocId(String uuid) throws IOException
      {
         for (int i = 0; i < subReaders.length; i++)
         {
            CachingMultiIndexReader subReader = subReaders[i];
            ForeignSegmentDocId doc = subReader.createDocId(uuid);
            if (doc != null)
            {
               return doc;
            }
         }
         return null;
      }

      // -------------------------< MultiIndexReader >-------------------------

      public boolean equals(Object obj)
      {
         if (obj instanceof CombinedIndexReader)
         {
            CombinedIndexReader other = (CombinedIndexReader)obj;
            return Arrays.equals(subReaders, other.subReaders);
         }
         return false;
      }

      // ---------------------------< internal >-------------------------------

      /**
       * {@inheritDoc}
       */
      public int getDocumentNumber(ForeignSegmentDocId docId)
      {
         for (int i = 0; i < subReaders.length; i++)
         {
            CachingMultiIndexReader subReader = subReaders[i];
            int realDoc = subReader.getDocumentNumber(docId);
            if (realDoc >= 0)
            {
               return realDoc;
            }
         }
         return -1;
      }

      /**
       * {@inheritDoc}
       */
      public IndexReader[] getIndexReaders()
      {
         IndexReader readers[] = new IndexReader[subReaders.length];
         System.arraycopy(subReaders, 0, readers, 0, subReaders.length);
         return readers;
      }

      /**
       * {@inheritDoc}
       */
      public void release() throws IOException
      {
         for (int i = 0; i < subReaders.length; i++)
         {
            subReaders[i].release();
         }
      }

      /**
       * @inheritDoc
       */
      public int getParent(int n) throws IOException
      {
         int i = readerIndex(n);
         DocId id = subReaders[i].getParentDocId(n - starts[i]);
         id = id.applyOffset(starts[i]);
         return id.getDocumentNumber(this);
      }

      public int hashCode()
      {
         int hash = 0;
         for (int i = 0; i < subReaders.length; i++)
         {
            hash = 31 * hash + subReaders[i].hashCode();
         }
         return hash;
      }

      /**
       * Returns the reader index for document <code>n</code>. Implementation
       * copied from lucene MultiReader class.
       * 
       * @param n document number.
       * @return the reader index.
       */
      private int readerIndex(int n)
      {
         int lo = 0; // search starts array
         int hi = subReaders.length - 1; // for first element less

         while (hi >= lo)
         {
            int mid = (lo + hi) >> 1;
            int midValue = starts[mid];
            if (n < midValue)
            {
               hi = mid - 1;
            }
            else if (n > midValue)
            {
               lo = mid + 1;
            }
            else
            { // found a match
               while (mid + 1 < subReaders.length && starts[mid + 1] == midValue)
               {
                  mid++; // scan to last match
               }
               return mid;
            }
         }
         return hi;
      }
   }

   public QueryHandlerEntryWrapper getQueryHandlerConfig()
   {
      return queryHandlerConfig;
   }

   /**
    * Log unindexed changes into error.log
    * 
    * @param removed set of removed node uuids
    * @param added map of added node states and uuids
    * @throws IOException
    */
   public void logErrorChanges(Set<String> removed, Set<String> added) throws IOException
   {
      // backup the remove and add iterators
      errorLog.writeChanges(removed, added);
   }

}
