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

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.collection.TransformedCollection;
import org.apache.commons.collections.iterators.TransformIterator;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortComparatorSource;
import org.apache.lucene.search.SortField;
import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.commons.utils.PrivilegedSystemHelper;
import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.document.DocumentReaderService;
import org.exoplatform.services.jcr.config.QueryHandlerEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.SessionDataManager;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.query.AbstractQueryHandler;
import org.exoplatform.services.jcr.impl.core.query.DefaultQueryNodeFactory;
import org.exoplatform.services.jcr.impl.core.query.ErrorLog;
import org.exoplatform.services.jcr.impl.core.query.ExecutableQuery;
import org.exoplatform.services.jcr.impl.core.query.IndexerIoMode;
import org.exoplatform.services.jcr.impl.core.query.IndexerIoModeListener;
import org.exoplatform.services.jcr.impl.core.query.QueryHandler;
import org.exoplatform.services.jcr.impl.core.query.QueryHandlerContext;
import org.exoplatform.services.jcr.impl.core.query.SearchIndexConfigurationHelper;
import org.exoplatform.services.jcr.impl.core.query.lucene.directory.DirectoryManager;
import org.exoplatform.services.jcr.impl.core.query.lucene.directory.FSDirectoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Implements a {@link org.apache.jackrabbit.core.query.QueryHandler} using
 * Lucene.
 */
public class SearchIndex extends AbstractQueryHandler implements IndexerIoModeListener
{

   private static final DefaultQueryNodeFactory DEFAULT_QUERY_NODE_FACTORY = new DefaultQueryNodeFactory();

   /** The logger instance for this class */
   private static final Logger log = LoggerFactory.getLogger("exo.jcr.component.core.SearchIndex");

   /**
    * Name of the file to persist search internal namespace mappings.
    */
   private static final String NS_MAPPING_FILE = "ns_mappings.properties";

   /**
    * The default value for property {@link #minMergeDocs}.
    */
   public static final int DEFAULT_MIN_MERGE_DOCS = 100;

   /**
    * The default value for property {@link #maxMergeDocs}.
    */
   public static final int DEFAULT_MAX_MERGE_DOCS = Integer.MAX_VALUE;

   /**
    * the default value for property {@link #mergeFactor}.
    */
   public static final int DEFAULT_MERGE_FACTOR = 10;

   /**
    * the default value for property {@link #maxFieldLength}.
    */
   public static final int DEFAULT_MAX_FIELD_LENGTH = 10000;

   /**
    * The default value for property {@link #extractorPoolSize}.
    * 
    * @deprecated this value is not used anymore. Instead the default value is
    *             calculated as follows: 2 *
    *             Runtime.getRuntime().availableProcessors().
    */
   @Deprecated
   public static final int DEFAULT_EXTRACTOR_POOL_SIZE = 0;

   /**
    * The default value for property {@link #extractorBackLog}.
    */
   public static final int DEFAULT_EXTRACTOR_BACK_LOG = Integer.MAX_VALUE;

   /**
    * The default timeout in milliseconds which is granted to the text
    * extraction process until fulltext indexing is deferred to a background
    * thread.
    */
   public static final long DEFAULT_EXTRACTOR_TIMEOUT = 100;

   /**
    * The maximum volatile index size in bytes until it is written to disk. The
    * default value is 1048576 (1MB).
    */
   public static final long DEFAULT_MAX_VOLATILE_INDEX_SIZE = 1024 * 1024;

   /**
    * volatileTime config parameter.
    * Max age of volatile. It is guaranteed that in any case volatile index will 
    * be flush to FS not later then in maxVolatileTime seconds.
    * Default is (-1) that means no maximal timeout set
    */
   public static final int DEFAULT_MAX_VOLATILE_TIME = -1;

   /**
    * The default value for {@link #termInfosIndexDivisor}.
    */
   public static final int DEFAULT_TERM_INFOS_INDEX_DIVISOR = 1;

   /**
    * Default name of the error log file
    */
   private static final String ERROR_LOG = "error.log";

   /**
    * The actual index
    */
   private MultiIndex index;

   /**
    * The analyzer we use for indexing.
    */
   private JcrStandartAnalyzer analyzer;

   /**
    * The namespace mappings used internally.
    */
   private NamespaceMappings nsMappings;

   /**
    * The location of the search index.
    * <p/>
    * Note: This is a <b>mandatory</b> parameter!
    */
   private String path;

   /**
    * minMergeDocs config parameter.
    */
   private int minMergeDocs = DEFAULT_MIN_MERGE_DOCS;

   /**
    * The maximum volatile index size in bytes until it is written to disk. The
    * default value is 1048576 (1MB).
    */
   private long maxVolatileIndexSize = DEFAULT_MAX_VOLATILE_INDEX_SIZE;

   /**
    * volatileTime config parameter.
    * Max age of volatile. It is guaranteed that in any case volatile index will 
    * be flush to FS not later then in maxVolatileTime seconds.
    */
   private int maxVolatileTime = DEFAULT_MAX_VOLATILE_TIME;

   /**
    * volatileIdleTime config parameter.
    */
   private int volatileIdleTime = 3;

   /**
    * maxMergeDocs config parameter
    */
   private int maxMergeDocs = DEFAULT_MAX_MERGE_DOCS;

   /**
    * mergeFactor config parameter
    */
   private int mergeFactor = DEFAULT_MERGE_FACTOR;

   /**
    * maxFieldLength config parameter
    */
   private int maxFieldLength = DEFAULT_MAX_FIELD_LENGTH;

   /**
    * extractorPoolSize config parameter
    */
   private int extractorPoolSize = 2 * Runtime.getRuntime().availableProcessors();

   /**
    * extractorBackLog config parameter
    */
   private int extractorBackLog = DEFAULT_EXTRACTOR_BACK_LOG;

   /**
    * extractorTimeout config parameter
    */
   private long extractorTimeout = DEFAULT_EXTRACTOR_TIMEOUT;

   /**
    * Number of documents that are buffered before they are added to the index.
    */
   private int bufferSize = 10;

   /**
    * Compound file flag
    */
   private boolean useCompoundFile = true;

   /**
    * Flag indicating whether document order is enabled as the default
    * ordering.
    * <p/>
    * Default value is: <code>false</code>.
    */
   private boolean documentOrder = true;

   /**
    * If set <code>true</code> the index is checked for consistency on startup.
    * If <code>false</code> a consistency check is only performed when there
    * are entries in the redo log on startup.
    * <p/>
    * Default value is: <code>false</code>.
    */
   private boolean forceConsistencyCheck = false;

   /**
    * If set <code>true</code> the index is checked for consistency depending
    * on the {@link #forceConsistencyCheck} parameter. If set to
    * <code>false</code>, no consistency check is performed, even if the redo
    * log had been applied on startup.
    * <p/>
    * Default value is: <code>false</code>.
    */
   private boolean consistencyCheckEnabled = false;

   /**
    * If set <code>true</code> errors detected by the consistency check are
    * repaired. If <code>false</code> the errors are only reported in the log.
    * <p/>
    * Default value is: <code>true</code>.
    */
   private boolean autoRepair = true;

   /**
    * The uuid resolver cache size.
    * <p/>
    * Default value is: <code>1000</code>.
    */
   private int cacheSize = 1000;

   /**
    * The number of documents that are pre fetched when a query is executed.
    * <p/>
    * Default value is: {@link Integer#MAX_VALUE}.
    */
   private int resultFetchSize = Integer.MAX_VALUE;

   /**
    * If set to <code>true</code> the fulltext field is stored and and a term
    * vector is created with offset information.
    * <p/>
    * Default value is: <code>false</code>.
    */
   private boolean supportHighlighting = false;

   /**
    * The excerpt provider class. Implements {@link ExcerptProvider}.
    */
   private Class<? extends ExcerptProvider> excerptProviderClass = DefaultHTMLExcerpt.class;

   /**
    * The path to the indexing configuration file.
    */
   private String indexingConfigPath;

   /**
    * The DOM with the indexing configuration or <code>null</code> if there is
    * no such configuration.
    */
   private Element indexingConfiguration;

   /**
    * The indexing configuration.
    */
   private IndexingConfiguration indexingConfig;

   /**
    * The name and path resolver used internally.
    */
   private LocationFactory npResolver;

   /**
    * The indexing configuration class. Implements
    * {@link IndexingConfiguration}.
    */
   private Class<? extends IndexingConfiguration> indexingConfigurationClass = IndexingConfigurationImpl.class;

   /**
    * The class that implements {@link SynonymProvider}.
    */
   private Class<? extends SynonymProvider> synonymProviderClass;

   /**
    * The currently set synonym provider.
    */
   private SynonymProvider synProvider;

   /**
    * The configuration path for the synonym provider.
    */
   private String synonymProviderConfigPath;

   /**
    * The FileSystem for the synonym if the query handler context does not
    * provide one.
    */
   private InputStream synonymProviderConfigFs;

   /**
    * Indicates the index format version which is relevant to a <b>query</b>.
    * This value may be different from what
    * {@link MultiIndex#getIndexFormatVersion()} returns because queries may be
    * executed on two physical indexes with different formats. Index format
    * versions are considered backward compatible. That is, the lower version
    * of the two physical indexes is used for querying.
    */
   private IndexFormatVersion indexFormatVersion;

   /**
    * The class that implements {@link SpellChecker}.
    */
   private Class<? extends SpellChecker> spellCheckerClass;

   /**
    * The spell checker for this query handler or <code>null</code> if none is
    * configured.
    */
   private SpellChecker spellChecker;

   /**
    * Return most popular results.
    */
   private boolean spellCheckerMorePopular = true;

   /**
    * Minimal distance between spell checked word and proposed word. 
    */
   private float spellCheckerMinDistance = 0.55f;

   /**
    * The similarity in use for indexing and searching.
    */
   private Similarity similarity = Similarity.getDefault();

   /**
    * The name of the directory manager class implementation.
    */
   private String directoryManagerClass = FSDirectoryManager.class.getName();

   /**
    * The directory manager.
    */
   private DirectoryManager directoryManager;

   /**
    * The termInfosIndexDivisor.
    */
   private int termInfosIndexDivisor = DEFAULT_TERM_INFOS_INDEX_DIVISOR;

   /**
    * The sort comparator source for indexed properties.
    */
   private SortComparatorSource scs;

   /**
    * Flag that indicates whether the hierarchy cache should be initialized
    * immediately on startup.
    */
   private boolean initializeHierarchyCache = true;

   /**
    * Indicates if this <code>SearchIndex</code> is closed and cannot be used
    * anymore.
    */
   private boolean closed = false;

   /**
    * Text extractor for extracting text content of binary properties.
    */
   private DocumentReaderService extractor;

   public static final int DEFAULT_ERRORLOG_FILE_SIZE = 50; // Kb

   private int errorLogfileSize = DEFAULT_ERRORLOG_FILE_SIZE;

   /**
    * The ErrorLog of this <code>MultiIndex</code>. All changes that must be in
    * index but interrupted by IOException are here.
    */
   private ErrorLog errorLog;

   private final ConfigurationManager cfm;

   /**
    * Working constructor.
    * 
    * @throws RepositoryConfigurationException
    * @throws IOException
    */
   public SearchIndex(QueryHandlerEntry queryHandlerConfig, ConfigurationManager cfm) throws IOException,
      RepositoryConfigurationException
   {
      this.analyzer = new JcrStandartAnalyzer();
      // this.queryHandlerConfig = new QueryHandlerEntryWrapper(
      // queryHandlerConfig);
      this.cfm = cfm;
      SearchIndexConfigurationHelper searchIndexConfigurationHelper = new SearchIndexConfigurationHelper(this);
      searchIndexConfigurationHelper.init(queryHandlerConfig);
   }

   /**
    * For test constructor.
    */
   public SearchIndex()
   {
      this.analyzer = new JcrStandartAnalyzer();
      // this.queryHandlerConfig = null;
      this.cfm = null;
   }

   /**
    * Initializes this <code>QueryHandler</code>. This implementation requires
    * that a path parameter is set in the configuration. If this condition is
    * not met, a <code>IOException</code> is thrown.
    * 
    * @throws IOException
    *             if an error occurs while initializing this handler.
    * @throws RepositoryException
    */
   @Override
   public void doInit() throws IOException, RepositoryException
   {
      QueryHandlerContext context = getContext();
      setPath(context.getIndexDirectory());
      if (path == null)
      {
         throw new IOException("SearchIndex requires 'path' parameter in configuration!");
      }

      final File indexDirectory;
      if (path != null)
      {
         indexDirectory = new File(path);

         try
         {
            AccessController.doPrivileged((new PrivilegedExceptionAction<Object>()
            {
               public Object run() throws Exception
               {
                  if (!indexDirectory.exists())
                  {
                     if (!indexDirectory.mkdirs())
                     {
                        throw new RepositoryException("fail to create index dir " + path);
                     }
                  }
                  return null;
               }
            }));
         }
         catch (PrivilegedActionException pae)
         {
            Throwable cause = pae.getCause();
            if (cause instanceof RepositoryException)
            {
               throw (RepositoryException)cause;
            }
            else if (cause instanceof RuntimeException)
            {
               throw (RuntimeException)cause;
            }
            else
            {
               throw new RuntimeException(cause);
            }
         }
      }
      else
      {
         throw new IOException("SearchIndex requires 'path' parameter in configuration!");
      }
      log.info("path=" + path);

      // Set excludedIDs = new HashSet();
      // if (context.getExcludedNodeId() != null)
      // {
      // excludedIDs.add(context.getExcludedNodeId());
      // }

      extractor = context.getExtractor();
      synProvider = createSynonymProvider();//queryHandlerConfig.createSynonymProvider(cfm);
      directoryManager = createDirectoryManager();

      if (context.getParentHandler() instanceof SearchIndex)
      {
         // use system namespace mappings
         SearchIndex sysIndex = (SearchIndex)context.getParentHandler();
         nsMappings = sysIndex.getNamespaceMappings();
      }
      else
      {
         // read local namespace mappings
         final File mapFile = new File(indexDirectory, NS_MAPPING_FILE);
         boolean fileExists = SecurityHelper.doPriviledgedAction(new PrivilegedAction<Boolean>()
         {
            public Boolean run()
            {
               return mapFile.exists();
            }
         });
         if (fileExists)
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

      scs = new SharedFieldSortComparator(FieldNames.PROPERTIES, context.getItemStateManager(), nsMappings);
      npResolver = new LocationFactory(nsMappings);

      indexingConfig = createIndexingConfiguration(nsMappings);
      analyzer.setIndexingConfig(indexingConfig);

      index = new MultiIndex(this, context.getIndexingTree(), modeHandler, getIndexInfos(), getIndexUpdateMonitor());
      // if RW mode, create initial index and start check
      if (modeHandler.getMode() == IndexerIoMode.READ_WRITE)
      {
         if (index.numDocs() == 0 && context.isCreateInitialIndex())
         {
            index.createInitialIndex(context.getItemStateManager());
         }
         if (consistencyCheckEnabled && (index.getRedoLogApplied() || forceConsistencyCheck))
         {
            log.info("Running consistency check...");
            try
            {
               ConsistencyCheck check = ConsistencyCheck.run(index, context.getItemStateManager());
               if (autoRepair)
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
            catch (Exception e)
            {
               log.warn("Failed to run consistency check on index: " + e);
            }
         }
      }
      // initialize spell checker
      spellChecker = createSpellChecker();

      log.info("Index initialized: {} Version: {}", new Object[]{path, index.getIndexFormatVersion()});
      if (!index.getIndexFormatVersion().equals(getIndexFormatVersion()))
      {
         log.warn("Using Version {} for reading. Please re-index version " + "storage for optimal performance.",
            new Integer(getIndexFormatVersion().getVersion()));
      }

      File file = new File(indexDirectory, ERROR_LOG);
      errorLog = new ErrorLog(file, errorLogfileSize);
      // reprocess any notfinished notifies;
      if (modeHandler.getMode() == IndexerIoMode.READ_WRITE)
      {
         recoverErrorLog(errorLog);
      }

      modeHandler.addIndexerIoModeListener(this);
   }

   /**
    * @return the errorLogfileSize
    */
   public int getErrorLogfileSize()
   {
      return errorLogfileSize;
   }

   /**
    * @param errorLogfileSize
    *            the errorLogfileSize to set
    */
   public void setErrorLogfileSize(int errorLogfileSize)
   {
      this.errorLogfileSize = errorLogfileSize;
   }

   /**
    * Adds the <code>node</code> to the search index.
    * 
    * @param node
    *            the node to add.
    * @throws RepositoryException
    *             if an error occurs while indexing the node.
    * @throws IOException
    *             if an error occurs while adding the node to the index.
    */
   public void addNode(NodeData node) throws RepositoryException, IOException
   {
      throw new UnsupportedOperationException("addNode");
   }

   /**
    * Removes the node with <code>uuid</code> from the search index.
    * 
    * @param id
    *            the id of the node to remove from the index.
    * @throws IOException
    *             if an error occurs while removing the node from the index.
    */
   public void deleteNode(String id) throws IOException
   {
      throw new UnsupportedOperationException("deleteNode");
   }

   /**
    * This implementation forwards the call to
    * {@link MultiIndex#update(Collection, Collection)} and transforms the two
    * iterators to the required types.
    * 
    * @param remove
    *            uuids of nodes to remove.
    * @param add
    *            NodeStates to add. Calls to <code>next()</code> on this
    *            iterator may return <code>null</code>, to indicate that a node
    *            could not be indexed successfully.
    * @throws RepositoryException
    *             if an error occurs while indexing a node.
    * @throws IOException
    *             if an error occurs while updating the index.
    */
   @Override
   public void updateNodes(final Iterator<String> remove, final Iterator<NodeData> add) throws RepositoryException,
      IOException
   {
      checkOpen();
      final Map<String, NodeData> aggregateRoots = new HashMap<String, NodeData>();
      final Set<String> removedNodeIds = new HashSet<String>();
      final Set<String> addedNodeIds = new HashSet<String>();

      index.update(IteratorUtils.toList(new TransformIterator(remove, new Transformer()
      {
         public Object transform(Object input)
         {
            String uuid = ((String)input);
            removedNodeIds.add(uuid);
            return uuid;
         }
      })), IteratorUtils.toList(new TransformIterator(add, new Transformer()
      {
         public Object transform(Object input)
         {
            NodeData state = (NodeData)input;
            if (state == null)
            {
               return null;
            }
            String uuid = state.getIdentifier();
            addedNodeIds.add(uuid);
            removedNodeIds.remove(uuid);
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
      })));

      // remove any aggregateRoot nodes that are new
      // and therefore already up-to-date
      aggregateRoots.keySet().removeAll(addedNodeIds);

      // based on removed UUIDs get affected aggregate root nodes
      retrieveAggregateRoot(removedNodeIds, aggregateRoots);

      // update aggregates if there are any affected
      if (aggregateRoots.size() > 0)
      {
         Collection modified = TransformedCollection.decorate(new ArrayList(), new Transformer()
         {
            public Object transform(Object input)
            {
               NodeData state = (NodeData)input;
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
         modified.addAll(aggregateRoots.values());
         index.update(aggregateRoots.keySet(), modified);
      }
   }

   /**
    * Creates a new query by specifying the query statement itself and the
    * language in which the query is stated. If the query statement is
    * syntactically invalid, given the language specified, an
    * InvalidQueryException is thrown. <code>language</code> must specify a
    * query language string from among those returned by
    * QueryManager.getSupportedQueryLanguages(); if it is not then an
    * <code>InvalidQueryException</code> is thrown.
    * 
    * @param session
    *            the session of the current user creating the query object.
    * @param itemMgr
    *            the item manager of the current user.
    * @param statement
    *            the query statement.
    * @param language
    *            the syntax of the query statement.
    * @throws InvalidQueryException
    *             if statement is invalid or language is unsupported.
    * @return A <code>Query</code> object.
    */
   public ExecutableQuery createExecutableQuery(SessionImpl session, SessionDataManager itemMgr, String statement,
      String language) throws InvalidQueryException
   {
      QueryImpl query =
         new QueryImpl(session, itemMgr, this, getContext().getPropertyTypeRegistry(), statement, language,
            getQueryNodeFactory());
      query.setRespectDocumentOrder(documentOrder);
      return query;
   }

   // /**
   // * Creates a new query by specifying the query object model. If the query
   // * object model is considered invalid for the implementing class, an
   // * InvalidQueryException is thrown.
   // *
   // * @param session the session of the current user creating the query
   // * object.
   // * @param itemMgr the item manager of the current user.
   // * @param qomTree query query object model tree.
   // * @return A <code>Query</code> object.
   // * @throws javax.jcr.query.InvalidQueryException
   // * if the query object model tree is invalid.
   // * @see QueryHandler#createExecutableQuery(SessionImpl, ItemManager,
   // QueryObjectModelTree)
   // */
   // public ExecutableQuery createExecutableQuery(SessionImpl session,
   // SessionDataManager itemMgr, QueryObjectModelTree qomTree)
   // throws InvalidQueryException
   // {
   // QueryObjectModelImpl query =
   // new QueryObjectModelImpl(session, itemMgr, this,
   // getContext().getPropertyTypeRegistry(), qomTree);
   // query.setRespectDocumentOrder(documentOrder);
   // return query;
   // }

   /**
    * This method returns the QueryNodeFactory used to parse Queries. This
    * method may be overridden to provide a customized QueryNodeFactory
    */
   protected DefaultQueryNodeFactory getQueryNodeFactory()
   {
      return DEFAULT_QUERY_NODE_FACTORY;
   }

   /**
    * Closes this <code>QueryHandler</code> and frees resources attached to
    * this handler.
    */
   public void close()
   {
      if (synonymProviderConfigFs != null)
      {
         try
         {
            synonymProviderConfigFs.close();
         }
         catch (IOException e)
         {
            log.warn("Exception while closing FileSystem", e);
         }
      }
      if (spellChecker != null)
      {
         spellChecker.close();
      }
      errorLog.close();
      index.close();
      getContext().destroy();
      closed = true;
      log.info("Index closed: " + path);
   }

   /**
    * Executes the query on the search index.
    * 
    * @param session
    *            the session that executes the query.
    * @param queryImpl
    *            the query impl.
    * @param query
    *            the lucene query.
    * @param orderProps
    *            name of the properties for sort order.
    * @param orderSpecs
    *            the order specs for the sort order properties.
    *            <code>true</code> indicates ascending order,
    *            <code>false</code> indicates descending.
    * @param resultFetchHint
    *            a hint on how many results should be fetched.
    * @return the query hits.
    * @throws IOException
    *             if an error occurs while searching the index.
    * @throws RepositoryException
    */
   public MultiColumnQueryHits executeQuery(SessionImpl session, AbstractQueryImpl queryImpl, Query query,
      QPath[] orderProps, boolean[] orderSpecs, long resultFetchHint) throws IOException, RepositoryException
   {
      checkOpen();

      Sort sort = new Sort(createSortFields(orderProps, orderSpecs));

      final IndexReader reader = getIndexReader(queryImpl.needsSystemTree());
      JcrIndexSearcher searcher = new JcrIndexSearcher(session, reader, getContext().getItemStateManager());
      searcher.setSimilarity(getSimilarity());
      return new FilterMultiColumnQueryHits(searcher.execute(query, sort, resultFetchHint,
         QueryImpl.DEFAULT_SELECTOR_NAME))
      {
         @Override
         public void close() throws IOException
         {
            try
            {
               super.close();
            }
            finally
            {
               PerQueryCache.getInstance().dispose();
               Util.closeOrRelease(reader);
            }
         }
      };
   }

   /**
    * Executes the query on the search index.
    * 
    * @param session
    *            the session that executes the query.
    * @param query
    *            the query.
    * @param orderProps
    *            name of the properties for sort order.
    * @param orderSpecs
    *            the order specs for the sort order properties.
    *            <code>true</code> indicates ascending order,
    *            <code>false</code> indicates descending.
    * @param resultFetchHint
    *            a hint on how many results should be fetched.
    * @return the query hits.
    * @throws IOException
    *             if an error occurs while searching the index.
    * @throws RepositoryException
    */
   public MultiColumnQueryHits executeQuery(SessionImpl session, MultiColumnQuery query, QPath[] orderProps,
      boolean[] orderSpecs, long resultFetchHint) throws IOException, RepositoryException
   {
      checkOpen();

      Sort sort = new Sort(createSortFields(orderProps, orderSpecs));

      final IndexReader reader = getIndexReader();
      JcrIndexSearcher searcher = new JcrIndexSearcher(session, reader, getContext().getItemStateManager());
      searcher.setSimilarity(getSimilarity());
      return new FilterMultiColumnQueryHits(query.execute(searcher, sort, resultFetchHint))
      {
         @Override
         public void close() throws IOException
         {
            try
            {
               super.close();
            }
            finally
            {
               PerQueryCache.getInstance().dispose();
               Util.closeOrRelease(reader);
            }
         }
      };
   }

   /**
    * Creates an excerpt provider for the given <code>query</code>.
    * 
    * @param query
    *            the query.
    * @return an excerpt provider for the given <code>query</code>.
    * @throws IOException
    *             if the provider cannot be created.
    */
   public ExcerptProvider createExcerptProvider(Query query) throws IOException
   {
      ExcerptProvider ep;
      try
      {
         ep = excerptProviderClass.newInstance();
      }
      catch (Exception e)
      {
         throw Util.createIOException(e);
      }
      ep.init(query, this);
      return ep;
   }

   /**
    * Returns the analyzer in use for indexing.
    * 
    * @return the analyzer in use for indexing.
    */
   public Analyzer getTextAnalyzer()
   {
      return analyzer;
   }

   // /**
   // * Returns the text extractor in use for indexing.
   // *
   // * @return the text extractor in use for indexing.
   // */
   // public TextExtractor getTextExtractor()
   // {
   // return extractor;
   // }

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
    * @return the indexing configuration or <code>null</code> if there is none.
    */
   public IndexingConfiguration getIndexingConfig()
   {
      return indexingConfig;
   }

   /**
    * @return the synonym provider of this search index. If none is set for
    *         this search index the synonym provider of the parent handler is
    *         returned if there is any.
    */
   public SynonymProvider getSynonymProvider()
   {
      if (synProvider != null)
      {
         return synProvider;
      }
      else
      {
         QueryHandler handler = getContext().getParentHandler();
         if (handler instanceof SearchIndex)
         {
            return ((SearchIndex)handler).getSynonymProvider();
         }
         else
         {
            return null;
         }
      }
   }

   /**
    * @return the spell checker of this search index. If none is configured
    *         this method returns <code>null</code>.
    */
   public SpellChecker getSpellChecker()
   {
      return spellChecker;
   }

   /**
    * @return the similarity, which should be used for indexing and searching.
    */
   public Similarity getSimilarity()
   {
      return similarity;
   }

   /**
    * Returns an index reader for this search index. The caller of this method
    * is responsible for closing the index reader when he is finished using it.
    * 
    * @return an index reader for this search index.
    * @throws IOException
    *             the index reader cannot be obtained.
    */
   public IndexReader getIndexReader() throws IOException
   {
      return getIndexReader(true);
   }

   /**
    * Returns the index format version that this search index is able to
    * support when a query is executed on this index.
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
    * @return the directory manager for this search index.
    */
   public DirectoryManager getDirectoryManager()
   {
      return directoryManager;
   }

   /**
    * Returns an index reader for this search index. The caller of this method
    * is responsible for closing the index reader when he is finished using it.
    * 
    * @param includeSystemIndex
    *            if <code>true</code> the index reader will cover the complete
    *            workspace. If <code>false</code> the returned index reader
    *            will not contains any nodes under /jcr:system.
    * @return an index reader for this search index.
    * @throws IOException
    *             the index reader cannot be obtained.
    */
   protected IndexReader getIndexReader(boolean includeSystemIndex) throws IOException
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
      return new JcrIndexReader(reader);
   }

   /**
    * Creates the SortFields for the order properties.
    * 
    * @param orderProps
    *            the order properties.
    * @param orderSpecs
    *            the order specs for the properties.
    * @return an array of sort fields
    * @throws RepositoryException
    */
   protected SortField[] createSortFields(QPath[] orderProps, boolean[] orderSpecs) throws RepositoryException
   {
      List<SortField> sortFields = new ArrayList<SortField>();
      for (int i = 0; i < orderProps.length; i++)
      {
         if (orderProps[i].getEntries().length == 1 && Constants.JCR_SCORE.equals(orderProps[i].getName()))
         {
            // order on jcr:score does not use the natural order as
            // implemented in lucene. score ascending in lucene means that
            // higher scores are first. JCR specs that lower score values
            // are first.
            sortFields.add(new SortField(null, SortField.SCORE, orderSpecs[i]));
         }
         else
         {
            path = npResolver.createJCRPath(orderProps[i]).getAsString(false);
            sortFields.add(new SortField(path, scs, !orderSpecs[i]));
         }
      }
      return sortFields.toArray(new SortField[sortFields.size()]);
   }

   /**
    * Creates a lucene <code>Document</code> for a node state using the
    * namespace mappings <code>nsMappings</code>.
    * 
    * @param node
    *            the node state to index.
    * @param nsMappings
    *            the namespace mappings of the search index.
    * @param indexFormatVersion
    *            the index format version that should be used to index the
    *            passed node state.
    * @return a lucene <code>Document</code> that contains all properties of
    *         <code>node</code>.
    * @throws RepositoryException
    *             if an error occurs while indexing the <code>node</code>.
    */
   protected Document createDocument(NodeData node, NamespaceMappings nsMappings, IndexFormatVersion indexFormatVersion)
      throws RepositoryException
   {
      NodeIndexer indexer = new NodeIndexer(node, getContext().getItemStateManager(), nsMappings, extractor);
      indexer.setSupportHighlighting(supportHighlighting);
      indexer.setIndexingConfiguration(indexingConfig);
      indexer.setIndexFormatVersion(indexFormatVersion);
      Document doc = indexer.createDoc();
      mergeAggregatedNodeIndexes(node, doc);
      return doc;
   }

   /**
    * Returns the actual index.
    * 
    * @return the actual index.
    */
   public MultiIndex getIndex()
   {
      return index;
   }

   /**
    * @return the sort comparator source for this index.
    */
   protected SortComparatorSource getSortComparatorSource()
   {
      return scs;
   }

   // /**
   // * Factory method to create the <code>TextExtractor</code> instance.
   // *
   // * @return the <code>TextExtractor</code> instance this index should use.
   // */
   // protected TextExtractor createTextExtractor()
   // {
   // TextExtractor txtExtr = new JackrabbitTextExtractor(textFilterClasses);
   // if (extractorPoolSize > 0)
   // {
   // // wrap with pool
   // txtExtr = new PooledTextExtractor(txtExtr, extractorPoolSize,
   // extractorBackLog, extractorTimeout);
   // }
   // return txtExtr;
   // }

   /**
    * @param namespaceMappings
    *            The namespace mappings
    * @return the fulltext indexing configuration or <code>null</code> if there
    *         is no configuration.
    */
   protected IndexingConfiguration createIndexingConfiguration(NamespaceMappings namespaceMappings)
   {
      Element docElement = getIndexingConfigurationDOM();
      if (docElement == null)
      {
         return null;
      }
      try
      {
         IndexingConfiguration idxCfg = indexingConfigurationClass.newInstance();
         idxCfg.init(docElement, getContext(), namespaceMappings);
         return idxCfg;
      }
      catch (Exception e)
      {
         log.warn("Exception initializing indexing configuration from: " + indexingConfigPath, e);
      }
      log.warn(indexingConfigPath + " ignored.");
      return null;
   }

   /**
    * @return the configured synonym provider or <code>null</code> if none is
    *         configured or an error occurs.
    */
   protected SynonymProvider createSynonymProvider()
   {
      SynonymProvider sp = null;
      if (synonymProviderClass != null)
      {
         try
         {
            sp = synonymProviderClass.newInstance();
            sp.initialize(createSynonymProviderConfigResource());
         }
         catch (Exception e)
         {
            log.warn("Exception initializing synonym provider: " + synonymProviderClass, e);
            sp = null;
         }
      }
      return sp;
   }

   /**
    * @return an initialized {@link DirectoryManager}.
    * @throws IOException
    *             if the directory manager cannot be instantiated or an
    *             exception occurs while initializing the manager.
    */
   protected DirectoryManager createDirectoryManager() throws IOException
   {
      try
      {
         Class clazz = Class.forName(directoryManagerClass);
         if (!DirectoryManager.class.isAssignableFrom(clazz))
         {
            throw new IOException(directoryManagerClass + " is not a DirectoryManager implementation");
         }
         DirectoryManager df = (DirectoryManager)clazz.newInstance();
         df.init(this);
         return df;
      }
      catch (IOException e)
      {
         throw e;
      }
      catch (Exception e)
      {
         IOException ex = new IOException();
         ex.initCause(e);
         throw ex;
      }
   }

   /**
    * Creates a file system resource to the synonym provider configuration.
    *
    * @return a file system resource or <code>null</code> if no path was
    *         configured.
    * @throws FileSystemException if an exception occurs accessing the file
    *                             system.
    */
   protected InputStream createSynonymProviderConfigResource() throws IOException
   {
      if (synonymProviderConfigPath != null)
      {
         InputStream fsr;
         // simple sanity check
         String separator = PrivilegedSystemHelper.getProperty("file.separator");
         if (synonymProviderConfigPath.endsWith(PrivilegedSystemHelper.getProperty("file.separator")))
         {
            throw new IOException("Invalid synonymProviderConfigPath: " + synonymProviderConfigPath);
         }

         if (cfm == null)
         {
            int lastSeparator = synonymProviderConfigPath.lastIndexOf(separator);
            if (lastSeparator != -1)
            {
               File root = new File(path, synonymProviderConfigPath.substring(0, lastSeparator));
               fsr =
                  new BufferedInputStream(PrivilegedFileHelper.fileInputStream(new File(root, synonymProviderConfigPath
                     .substring(lastSeparator + 1))));
            }
            else
            {
               fsr = new BufferedInputStream(PrivilegedFileHelper.fileInputStream(new File(synonymProviderConfigPath)));

            }
            synonymProviderConfigFs = fsr;
         }
         else
         {
            try
            {
               fsr = cfm.getInputStream(synonymProviderConfigPath);
            }
            catch (Exception e)
            {
               throw new IOException(e.getLocalizedMessage());
            }
         }
         return fsr;
      }
      else
      {
         // path not configured
         return null;
      }
   }

   /**
    * Creates a spell checker for this query handler.
    * 
    * @return the spell checker or <code>null</code> if none is configured or
    *         an error occurs.
    */
   protected SpellChecker createSpellChecker()
   {
      // spell checker config
      SpellChecker spCheck = null;
      if (spellCheckerClass != null)
      {
         try
         {
            spCheck = SecurityHelper.doPriviledgedIOExceptionAction(new PrivilegedExceptionAction<SpellChecker>()
            {
               public SpellChecker run() throws Exception
               {
                  SpellChecker spCheck = spellCheckerClass.newInstance();
                  spCheck.init(SearchIndex.this, spellCheckerMinDistance, spellCheckerMorePopular);
                  return spCheck;
               }
            });
         }
         catch (Exception e)
         {
            log.warn("Exception initializing spell checker: " + spellCheckerClass, e);
         }
      }
      return spCheck;
   }

   /**
    * Returns the document element of the indexing configuration or
    * <code>null</code> if there is no indexing configuration.
    * 
    * @return the indexing configuration or <code>null</code> if there is none.
    */
   protected Element getIndexingConfigurationDOM()
   {
      if (indexingConfiguration == null)
      {
         if (indexingConfigPath != null)
         {

            // File config = PrivilegedFileHelper.file(indexingConfigPath);
            SecurityHelper.doPriviledgedAction(new PrivilegedAction<Object>()
            {
               public Object run()
               {
                  InputStream is = SearchIndex.class.getResourceAsStream(indexingConfigPath);
                  if (is == null)
                  {
                     try
                     {
                        is = cfm.getInputStream(indexingConfigPath);
                     }
                     catch (Exception e1)
                     {
                        log.warn("Unable to load configuration " + indexingConfigPath);
                     }
                  }

                  try
                  {
                     DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                     DocumentBuilder builder = factory.newDocumentBuilder();
                     builder.setEntityResolver(new IndexingConfigurationEntityResolver());
                     indexingConfiguration = builder.parse(is).getDocumentElement();
                  }
                  catch (ParserConfigurationException e)
                  {
                     log.warn("Unable to create XML parser", e);
                  }
                  catch (IOException e)
                  {
                     log.warn("Exception parsing " + indexingConfigPath, e);
                  }
                  catch (SAXException e)
                  {
                     log.warn("Exception parsing " + indexingConfigPath, e);
                  }
                  return null;
               }
            });
         }
      }
      return indexingConfiguration;
   }

   /**
    * Merges the fulltext indexed fields of the aggregated node states into
    * <code>doc</code>.
    * 
    * @param state
    *            the node state on which <code>doc</code> was created.
    * @param doc
    *            the lucene document with index fields from <code>state</code>.
    */
   protected void mergeAggregatedNodeIndexes(NodeData state, Document doc)
   {
      if (indexingConfig != null)
      {
         AggregateRule[] aggregateRules = indexingConfig.getAggregateRules();
         if (aggregateRules == null)
         {
            return;
         }
         try
         {
            ItemDataConsumer ism = getContext().getItemStateManager();
            for (int i = 0; i < aggregateRules.length; i++)
            {
               boolean ruleMatched = false;
               // node includes
               NodeData[] aggregates = aggregateRules[i].getAggregatedNodeStates(state);
               if (aggregates != null)
               {
                  ruleMatched = true;
                  for (int j = 0; j < aggregates.length; j++)
                  {
                     Document aDoc =
                        createDocument(aggregates[j], getNamespaceMappings(), index.getIndexFormatVersion());
                     // transfer fields to doc if there are any
                     Fieldable[] fulltextFields = aDoc.getFieldables(FieldNames.FULLTEXT);
                     if (fulltextFields != null)
                     {
                        for (int k = 0; k < fulltextFields.length; k++)
                        {
                           doc.add(fulltextFields[k]);
                        }
                        doc.add(new Field(FieldNames.AGGREGATED_NODE_UUID, aggregates[j].getIdentifier(),
                           Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
                     }
                  }
               }
               // property includes
               PropertyData[] propStates = aggregateRules[i].getAggregatedPropertyStates(state);
               if (propStates != null)
               {
                  ruleMatched = true;
                  for (int j = 0; j < propStates.length; j++)
                  {
                     PropertyData propState = propStates[j];
                     String namePrefix =
                        FieldNames.createNamedValue(getNamespaceMappings()
                           .translateName(propState.getQPath().getName()), "");
                     NodeData parent = (NodeData)ism.getItemData(propState.getParentIdentifier());
                     Document aDoc = createDocument(parent, getNamespaceMappings(), getIndex().getIndexFormatVersion());
                     try
                     {
                        // find the right fields to transfer
                        Fieldable[] fields = aDoc.getFieldables(FieldNames.PROPERTIES);
                        Token t = new Token();
                        for (int k = 0; k < fields.length; k++)
                        {
                           Fieldable field = fields[k];
                           // assume properties fields use
                           // SingleTokenStream
                           t = field.tokenStreamValue().next(t);
                           String value = new String(t.termBuffer(), 0, t.termLength());
                           if (value.startsWith(namePrefix))
                           {
                              // extract value
                              value = value.substring(namePrefix.length());
                              // create new named value
                              QPath p = getRelativePath(state, propState);
                              String path = getNamespaceMappings().translatePath(p);
                              value = FieldNames.createNamedValue(path, value);
                              t.setTermBuffer(value);
                              doc.add(new Field(field.name(), new SingletonTokenStream(t)));
                              doc.add(new Field(FieldNames.AGGREGATED_NODE_UUID, parent.getIdentifier(),
                                 Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
                           }
                        }
                     }
                     finally
                     {
                        Util.disposeDocument(aDoc);
                     }
                  }
               }

               // only use first aggregate definition that matches
               if (ruleMatched)
               {
                  break;
               }
            }
         }
         catch (Exception e)
         {
            // do not fail if aggregate cannot be created
            log
               .warn("Exception while building indexing aggregate for" + " node with UUID: " + state.getIdentifier(), e);
         }
      }
   }

   /**
    * Returns the relative path from <code>nodeState</code> to
    * <code>propState</code>.
    * 
    * @param nodeState
    *            a node state.
    * @param propState
    *            a property state.
    * @return the relative path.
    * @throws RepositoryException
    *             if an error occurs while resolving paths.
    * @throws ItemStateException
    *             if an error occurs while reading item states.
    */
   protected QPath getRelativePath(NodeData nodeState, PropertyData propState) throws RepositoryException

   {

      QPath nodePath = nodeState.getQPath();
      QPath propPath = propState.getQPath();
      throw new RepositoryException();
      // Path p = nodePath.computeRelativePath(propPath);
      // // make sure it does not contain indexes
      // boolean clean = true;
      // Path.Element[] elements = p.getElements();
      // for (int i = 0; i < elements.length; i++)
      // {
      // if (elements[i].getIndex() != 0)
      // {
      // elements[i] = PATH_FACTORY.createElement(elements[i].getName());
      // clean = false;
      // }
      // }
      // if (!clean)
      // {
      // p = PATH_FACTORY.create(elements);
      // }

      // return p;
   }

   /**
    * Retrieves the root of the indexing aggregate for <code>state</code> and
    * puts it into <code>map</code>.
    * 
    * @param state
    *            the node state for which we want to retrieve the aggregate
    *            root.
    * @param map
    *            aggregate roots are collected in this map. Key=UUID,
    *            value=NodeState.
    */
   protected void retrieveAggregateRoot(NodeData state, Map<String, NodeData> map)
   {
      if (indexingConfig != null)
      {
         AggregateRule[] aggregateRules = indexingConfig.getAggregateRules();
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
    * <code>removedUUIDs</code> and puts it into <code>map</code>.
    * 
    * @param removedUUIDs
    *            the UUIDs of removed nodes.
    * @param map
    *            aggregate roots are collected in this map. Key=UUID,
    *            value=NodeState.
    */
   protected void retrieveAggregateRoot(final Set<String> removedNodeIds, final Map<String, NodeData> map)

   {
      if (indexingConfig != null)
      {
         AggregateRule[] aggregateRules = indexingConfig.getAggregateRules();
         if (aggregateRules == null)
         {
            return;
         }
         long time = System.currentTimeMillis();
         int found = SecurityHelper.doPriviledgedAction(new PrivilegedAction<Integer>()
         {
            public Integer run()
            {
               int found = 0;
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
                              Document doc = reader.document(tDocs.doc(), FieldSelectors.UUID);
                              String uuid = doc.get(FieldNames.UUID);
                              ItemData itd = ism.getItemData(uuid);
                              if (itd == null)
                              {
                                 continue;
                              }
                              if (!itd.isNode())
                              {
                                 throw new RepositoryException("Item with id:" + uuid + " is not a node");
                              }
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
               return found;
            }
         });
         time = System.currentTimeMillis() - time;
         log.debug("Retrieved {} aggregate roots in {} ms.", new Integer(found), new Long(time));
      }
   }

   // ----------------------------< internal
   // >----------------------------------

   /**
    * Combines multiple {@link CachingMultiIndexReader} into a
    * <code>MultiReader</code> with {@link HierarchyResolver} support.
    */
   protected static final class CombinedIndexReader extends MultiReader implements HierarchyResolver, MultiIndexReader
   {

      /**
       * The sub readers.
       */
      private final CachingMultiIndexReader[] subReaders;

      /**
       * Doc number starts for each sub reader
       */
      private int[] starts;

      public CombinedIndexReader(CachingMultiIndexReader[] indexReaders)
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
       * @inheritDoc
       */
      public int[] getParents(int n, int[] docNumbers) throws IOException
      {
         int i = readerIndex(n);
         DocId id = subReaders[i].getParentDocId(n - starts[i]);
         id = id.applyOffset(starts[i]);
         return id.getDocumentNumbers(this, docNumbers);
      }

      // -------------------------< MultiIndexReader
      // >-------------------------

      /**
       * {@inheritDoc}
       */
      public IndexReader[] getIndexReaders()
      {
         IndexReader[] readers = new IndexReader[subReaders.length];
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

      // ---------------------------< internal
      // >-------------------------------

      /**
       * Returns the reader index for document <code>n</code>. Implementation
       * copied from lucene MultiReader class.
       * 
       * @param n
       *            document number.
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

      @Override
      public boolean equals(Object obj)
      {
         if (obj instanceof CombinedIndexReader)
         {
            CombinedIndexReader other = (CombinedIndexReader)obj;
            return Arrays.equals(subReaders, other.subReaders);
         }
         return false;
      }

      @Override
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
               return realDoc + starts[i];
            }
         }
         return -1;
      }
   }

   // --------------------------< properties
   // >----------------------------------

   /**
    * Sets the analyzer in use for indexing. The given analyzer class name must
    * satisfy the following conditions:
    * <ul>
    * <li>the class must exist in the class path</li>
    * <li>the class must have a public default constructor</li>
    * <li>the class must be a Lucene Analyzer</li>
    * </ul>
    * <p>
    * If the above conditions are met, then a new instance of the class is set
    * as the analyzer. Otherwise a warning is logged and the current analyzer
    * is not changed.
    * <p>
    * This property setter method is normally invoked by the Jackrabbit
    * configuration mechanism if the "analyzer" parameter is set in the search
    * configuration.
    * 
    * @param analyzerClassName
    *            the analyzer class name
    */
   public void setAnalyzer(String analyzerClassName)
   {
      try
      {
         Class analyzerClass = Class.forName(analyzerClassName);
         analyzer.setDefaultAnalyzer((Analyzer)analyzerClass.newInstance());
      }
      catch (Exception e)
      {
         log.warn("Invalid Analyzer class: " + analyzerClassName, e);
      }
   }

   /**
    * Returns the class name of the analyzer that is currently in use.
    * 
    * @return class name of analyzer in use.
    */
   public String getAnalyzer()
   {
      return analyzer.getClass().getName();
   }

   /**
    * Sets the location of the search index.
    * 
    * @param path
    *            the location of the search index.
    * @throws IOException
    */
   public void setPath(String path)
   {

      this.path = path.replace("${java.io.tmpdir}", PrivilegedSystemHelper.getProperty("java.io.tmpdir"));

   }

   /**
    * Returns the location of the search index. Returns <code>null</code> if
    * not set.
    * 
    * @return the location of the search index.
    */
   public String getPath()
   {
      return path;
   }

   /**
    * The lucene index writer property: useCompoundFile
    */
   public void setUseCompoundFile(boolean b)
   {
      useCompoundFile = b;
   }

   /**
    * Returns the current value for useCompoundFile.
    * 
    * @return the current value for useCompoundFile.
    */
   public boolean getUseCompoundFile()
   {
      return useCompoundFile;
   }

   /**
    * The lucene index writer property: minMergeDocs
    */
   public void setMinMergeDocs(int minMergeDocs)
   {
      this.minMergeDocs = minMergeDocs;
   }

   /**
    * Returns the current value for minMergeDocs.
    * 
    * @return the current value for minMergeDocs.
    */
   public int getMinMergeDocs()
   {
      return minMergeDocs;
   }

   /**
    * Sets the property: volatileIdleTime
    * 
    * @param volatileIdleTime
    *            idle time in seconds
    */
   public void setVolatileIdleTime(int volatileIdleTime)
   {
      this.volatileIdleTime = volatileIdleTime;
   }

   /**
    * Returns the current value for volatileIdleTime.
    * 
    * @return the current value for volatileIdleTime.
    */
   public int getVolatileIdleTime()
   {
      return volatileIdleTime;
   }

   /**
    * The lucene index writer property: maxMergeDocs
    */
   public void setMaxMergeDocs(int maxMergeDocs)
   {
      this.maxMergeDocs = maxMergeDocs;
   }

   /**
    * Returns the current value for maxMergeDocs.
    * 
    * @return the current value for maxMergeDocs.
    */
   public int getMaxMergeDocs()
   {
      return maxMergeDocs;
   }

   /**
    * The lucene index writer property: mergeFactor
    */
   public void setMergeFactor(int mergeFactor)
   {
      this.mergeFactor = mergeFactor;
   }

   /**
    * Returns the current value for the merge factor.
    * 
    * @return the current value for the merge factor.
    */
   public int getMergeFactor()
   {
      return mergeFactor;
   }

   /**
    * @see VolatileIndex#setBufferSize(int)
    */
   public void setBufferSize(int size)
   {
      bufferSize = size;
   }

   /**
    * Returns the current value for the buffer size.
    * 
    * @return the current value for the buffer size.
    */
   public int getBufferSize()
   {
      return bufferSize;
   }

   public void setRespectDocumentOrder(boolean docOrder)
   {
      documentOrder = docOrder;
   }

   public boolean getRespectDocumentOrder()
   {
      return documentOrder;
   }

   public void setForceConsistencyCheck(boolean b)
   {
      forceConsistencyCheck = b;
   }

   public boolean getForceConsistencyCheck()
   {
      return forceConsistencyCheck;
   }

   public void setAutoRepair(boolean b)
   {
      autoRepair = b;
   }

   public boolean getAutoRepair()
   {
      return autoRepair;
   }

   public void setCacheSize(int size)
   {
      cacheSize = size;
   }

   public int getCacheSize()
   {
      return cacheSize;
   }

   public void setMaxFieldLength(int length)
   {
      maxFieldLength = length;
   }

   public int getMaxFieldLength()
   {
      return maxFieldLength;
   }

   //
   // /**
   // * Sets the list of text extractors (and text filters) to use for
   // * extracting text content from binary properties. The list must be
   // * comma (or whitespace) separated, and contain fully qualified class
   // * names of the {@link TextExtractor} (and {@link
   // org.apache.jackrabbit.core.query.TextFilter}) classes
   // * to be used. The configured classes must all have a public default
   // * constructor.
   // *
   // * @param filterClasses comma separated list of class names
   // */
   // public void setTextFilterClasses(String filterClasses)
   // {
   // this.textFilterClasses = filterClasses;
   // }

   // /**
   // * Returns the fully qualified class names of the text filter instances
   // * currently in use. The names are comma separated.
   // *
   // * @return class names of the text filters in use.
   // */
   // public String getTextFilterClasses()
   // {
   // return textFilterClasses;
   // }

   /**
    * Tells the query handler how many result should be fetched initially when
    * a query is executed.
    * 
    * @param size
    *            the number of results to fetch initially.
    */
   public void setResultFetchSize(int size)
   {
      resultFetchSize = size;
   }

   /**
    * @return the number of results the query handler will fetch initially when
    *         a query is executed.
    */
   public int getResultFetchSize()
   {
      return resultFetchSize;
   }

   /**
    * The number of background threads for the extractor pool.
    * 
    * @param numThreads
    *            the number of threads.
    */
   public void setExtractorPoolSize(int numThreads)
   {
      if (numThreads < 0)
      {
         numThreads = 0;
      }
      extractorPoolSize = numThreads;
   }

   /**
    * @return the size of the thread pool which is used to run the text
    *         extractors when binary content is indexed.
    */
   public int getExtractorPoolSize()
   {
      return extractorPoolSize;
   }

   /**
    * The number of extractor jobs that are queued until a new job is executed
    * with the current thread instead of using the thread pool.
    * 
    * @param backLog
    *            size of the extractor job queue.
    */
   public void setExtractorBackLogSize(int backLog)
   {
      extractorBackLog = backLog;
   }

   /**
    * @return the size of the extractor queue back log.
    */
   public int getExtractorBackLogSize()
   {
      return extractorBackLog;
   }

   /**
    * The timeout in milliseconds which is granted to the text extraction
    * process until fulltext indexing is deferred to a background thread.
    * 
    * @param timeout
    *            the timeout in milliseconds.
    */
   public void setExtractorTimeout(long timeout)
   {
      extractorTimeout = timeout;
   }

   /**
    * @return the extractor timeout in milliseconds.
    */
   public long getExtractorTimeout()
   {
      return extractorTimeout;
   }

   /**
    * If set to <code>true</code> additional information is stored in the index
    * to support highlighting using the rep:excerpt pseudo property.
    * 
    * @param b
    *            <code>true</code> to enable highlighting support.
    */
   public void setSupportHighlighting(boolean b)
   {
      supportHighlighting = b;
   }

   /**
    * @return <code>true</code> if highlighting support is enabled.
    */
   public boolean getSupportHighlighting()
   {
      return supportHighlighting;
   }

   /**
    * Sets the class name for the {@link ExcerptProvider} that should be used
    * for the rep:excerpt pseudo property in a query.
    * 
    * @param className
    *            the name of a class that implements {@link ExcerptProvider}.
    */
   public void setExcerptProviderClass(String className)
   {
      try
      {
         Class clazz = Class.forName(className);
         if (ExcerptProvider.class.isAssignableFrom(clazz))
         {
            excerptProviderClass = clazz;
         }
         else
         {
            log.warn("Invalid value for excerptProviderClass, {} does " + "not implement ExcerptProvider interface.",
               className);
         }
      }
      catch (ClassNotFoundException e)
      {
         log.warn("Invalid value for excerptProviderClass, class {} not found.", className);
      }
   }

   /**
    * @return the class name of the excerpt provider implementation.
    */
   public String getExcerptProviderClass()
   {
      return excerptProviderClass.getName();
   }

   /**
    * Sets the path to the indexing configuration file.
    * 
    * @param path
    *            the path to the configuration file.
    */
   public void setIndexingConfiguration(String path)
   {
      indexingConfigPath = path;
   }

   /**
    * @return the path to the indexing configuration file.
    */
   public String getIndexingConfiguration()
   {
      return indexingConfigPath;
   }

   /**
    * Sets the name of the class that implements {@link IndexingConfiguration}.
    * The default value is
    * <code>org.apache.jackrabbit.core.query.lucene.IndexingConfigurationImpl</code>
    * .
    * 
    * @param className
    *            the name of the class that implements
    *            {@link IndexingConfiguration}.
    */
   public void setIndexingConfigurationClass(String className)
   {
      try
      {
         Class clazz = Class.forName(className);
         if (IndexingConfiguration.class.isAssignableFrom(clazz))
         {
            indexingConfigurationClass = clazz;
         }
         else
         {
            log.warn("Invalid value for indexingConfigurationClass, {} "
               + "does not implement IndexingConfiguration interface.", className);
         }
      }
      catch (ClassNotFoundException e)
      {
         log.warn("Invalid value for indexingConfigurationClass, class {} not found.", className);
      }
   }

   /**
    * @return the class name of the indexing configuration implementation.
    */
   public String getIndexingConfigurationClass()
   {
      return indexingConfigurationClass.getName();
   }

   /**
    * Sets the name of the class that implements {@link SynonymProvider}. The
    * default value is <code>null</code> (none set).
    * 
    * @param className
    *            name of the class that implements {@link SynonymProvider}.
    */
   public void setSynonymProviderClass(String className)
   {
      try
      {
         Class clazz = Class.forName(className);
         if (SynonymProvider.class.isAssignableFrom(clazz))
         {
            synonymProviderClass = clazz;
         }
         else
         {
            log.warn("Invalid value for synonymProviderClass, {} " + "does not implement SynonymProvider interface.",
               className);
         }
      }
      catch (ClassNotFoundException e)
      {
         log.warn("Invalid value for synonymProviderClass, class {} not found.", className);
      }
   }

   /**
    * @return the class name of the synonym provider implementation or
    *         <code>null</code> if none is set.
    */
   public String getSynonymProviderClass()
   {
      if (synonymProviderClass != null)
      {
         return synonymProviderClass.getName();
      }
      else
      {
         return null;
      }
   }

   /**
    * Sets the name of the class that implements {@link SpellChecker}. The
    * default value is <code>null</code> (none set).
    * 
    * @param className
    *            name of the class that implements {@link SpellChecker}.
    */
   public void setSpellCheckerClass(String className)
   {
      try
      {
         Class clazz = Class.forName(className);
         if (SpellChecker.class.isAssignableFrom(clazz))
         {
            spellCheckerClass = clazz;
         }
         else
         {
            log.warn("Invalid value for spellCheckerClass, {} " + "does not implement SpellChecker interface.",
               className);
         }
      }
      catch (ClassNotFoundException e)
      {
         log.warn("Invalid value for spellCheckerClass," + " class {} not found.", className);
      }
   }

   /**
    * Set SpellChecker morePopular parameter.
    * @param morePopular boolean
    */
   public void setSpellCheckerMorePopuar(boolean morePopular)
   {
      spellCheckerMorePopular = morePopular;
   }

   /**
    * Set SpellChecker minimal word distance.
    * @param minDistance float
    */
   public void setSpellCheckerMinDistance(float minDistance)
   {
      spellCheckerMinDistance = minDistance;
   }

   /**
    * @return the class name of the spell checker implementation or
    *         <code>null</code> if none is set.
    */
   public String getSpellCheckerClass()
   {
      if (spellCheckerClass != null)
      {
         return spellCheckerClass.getName();
      }
      else
      {
         return null;
      }
   }

   /**
    * Enables or disables the consistency check on startup. Consistency checks
    * are disabled per default.
    * 
    * @param b
    *            <code>true</code> enables consistency checks.
    * @see #setForceConsistencyCheck(boolean)
    */
   public void setEnableConsistencyCheck(boolean b)
   {
      this.consistencyCheckEnabled = b;
   }

   /**
    * @return <code>true</code> if consistency checks are enabled.
    */
   public boolean getEnableConsistencyCheck()
   {
      return consistencyCheckEnabled;
   }

   /**
    * Sets the configuration path for the synonym provider.
    * 
    * @param path
    *            the configuration path for the synonym provider.
    */
   public void setSynonymProviderConfigPath(String path)
   {
      synonymProviderConfigPath = path;
   }

   /**
    * @return the configuration path for the synonym provider. If none is set
    *         this method returns <code>null</code>.
    */
   public String getSynonymProviderConfigPath()
   {
      return synonymProviderConfigPath;
   }

   /**
    * Sets the similarity implementation, which will be used for indexing and
    * searching. The implementation must extend {@link Similarity}.
    * 
    * @param className
    *            a {@link Similarity} implementation.
    */
   public void setSimilarityClass(String className)
   {
      try
      {
         Class similarityClass = Class.forName(className);
         similarity = (Similarity)similarityClass.newInstance();
      }
      catch (Exception e)
      {
         log.warn("Invalid Similarity class: " + className, e);
      }
   }

   /**
    * @return the name of the similarity class.
    */
   public String getSimilarityClass()
   {
      return similarity.getClass().getName();
   }

   /**
    * Sets a new maxVolatileIndexSize value.
    * 
    * @param maxVolatileIndexSize
    *            the new value.
    */
   public void setMaxVolatileIndexSize(long maxVolatileIndexSize)
   {
      this.maxVolatileIndexSize = maxVolatileIndexSize;
   }

   /**
    * @return the maxVolatileIndexSize in bytes.
    */
   public long getMaxVolatileIndexSize()
   {
      return maxVolatileIndexSize;
   }

   /**
    * @return maxVolatileTime in seconds
    */
   public int getMaxVolatileTime()
   {
      return maxVolatileTime;
   }

   /**
    * @param maxVolatileTime in seconds
    */
   public void setMaxVolatileTime(int maxVolatileTime)
   {
      this.maxVolatileTime = maxVolatileTime;
   }

   /**
    * @return the name of the directory manager class.
    */
   public String getDirectoryManagerClass()
   {
      return directoryManagerClass;
   }

   /**
    * Sets name of the directory manager class. The class must implement
    * {@link DirectoryManager}.
    * 
    * @param className
    *            the name of the class that implements directory manager.
    */
   public void setDirectoryManagerClass(String className)
   {
      this.directoryManagerClass = className;
   }

   /**
    * @return the current value for termInfosIndexDivisor.
    */
   public int getTermInfosIndexDivisor()
   {
      return termInfosIndexDivisor;
   }

   /**
    * Sets a new value for termInfosIndexDivisor.
    * 
    * @param termInfosIndexDivisor
    *            the new value.
    */
   public void setTermInfosIndexDivisor(int termInfosIndexDivisor)
   {
      this.termInfosIndexDivisor = termInfosIndexDivisor;
   }

   /**
    * @return <code>true</code> if the hierarchy cache should be initialized
    *         immediately on startup.
    */
   public boolean isInitializeHierarchyCache()
   {
      return initializeHierarchyCache;
   }

   /**
    * Whether the hierarchy cache should be initialized immediately on startup.
    * 
    * @param initializeHierarchyCache
    *            <code>true</code> if the cache should be initialized
    *            immediately.
    */
   public void setInitializeHierarchyCache(boolean initializeHierarchyCache)
   {
      this.initializeHierarchyCache = initializeHierarchyCache;
   }

   // ----------------------------< internal
   // >----------------------------------

   /**
    * Checks if this <code>SearchIndex</code> is open, otherwise throws an
    * <code>IOException</code>.
    * 
    * @throws IOException
    *             if this <code>SearchIndex</code> had been closed.
    */
   private void checkOpen() throws IOException
   {
      if (closed)
      {
         throw new IOException("query handler closed and cannot be used anymore.");
      }
   }

   /**
    * Log unindexed changes into error.log
    * 
    * @param removed
    *            set of removed node uuids
    * @param added
    *            map of added node states and uuids
    * @throws IOException
    */
   public void logErrorChanges(Set<String> removed, Set<String> added) throws IOException
   {
      // backup the remove and add iterators
      errorLog.writeChanges(removed, added);
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
            // we have to iterrate through items till will meet ones
            // existing in
            // workspace
            while (iter.hasNext())
            {
               id = iter.next();

               try
               {
                  ItemData item = getContext().getItemStateManager().getItemData(id);
                  if (item != null)
                  {
                     if (item.isNode())
                     {
                        return (NodeData)item; // return node here
                     }
                     else
                     {
                        log.warn("Node expected but property found with id " + id + ". Skipping "
                           + item.getQPath().getAsString());
                     }
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
    * @see org.exoplatform.services.jcr.impl.core.query.QueryHandler#executeQuery(org.apache.lucene.search.Query,
    *      boolean, org.exoplatform.services.jcr.datamodel.InternalQName[],
    *      boolean[])
    */
   public QueryHits executeQuery(Query query) throws IOException
   {
      checkOpen();

      IndexReader reader = getIndexReader(true);
      IndexSearcher searcher = new IndexSearcher(reader);
      searcher.setSimilarity(getSimilarity());

      return new LuceneQueryHits(reader, searcher, query);
   }

   /**
    * {@inheritDoc}
    */
   public void onChangeMode(IndexerIoMode mode)
   {
      try
      {
         if (mode == IndexerIoMode.READ_WRITE)
         {
            // reprocess any notfinished notifies;
            log.info("Proceessing eroor log ...");
            recoverErrorLog(errorLog);
         }
      }
      catch (IOException e)
      {
         log.error("Can not recover error log. On changed mode " + mode, e);
      }
      catch (RepositoryException e)
      {
         log.error("Can not recover error log.", e);
      }
   }
}
