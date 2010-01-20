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

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.document.DocumentReaderService;
import org.exoplatform.services.jcr.config.QueryHandlerEntry;
import org.exoplatform.services.jcr.config.QueryHandlerParams;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.persistent.MandatoryItemsPersistenceListener;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.NamespaceRegistryImpl;
import org.exoplatform.services.jcr.impl.core.SessionDataManager;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.query.lucene.FieldNames;
import org.exoplatform.services.jcr.impl.core.query.lucene.LuceneVirtualTableResolver;
import org.exoplatform.services.jcr.impl.core.query.lucene.QueryHits;
import org.exoplatform.services.jcr.impl.core.query.lucene.ScoreNode;
import org.exoplatform.services.jcr.impl.core.query.lucene.SearchIndex;
import org.exoplatform.services.jcr.impl.core.value.NameValue;
import org.exoplatform.services.jcr.impl.core.value.PathValue;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspacePersistentDataManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.jboss.cache.factories.annotations.NonVolatile;
import org.picocontainer.Startable;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: SearchManager.java 1008 2009-12-11 15:14:51Z nzamosenchuk $
 */
@NonVolatile
public class SearchManager implements Startable, MandatoryItemsPersistenceListener
{

   /**
    * Logger instance for this class
    */
   private static final Log log = ExoLogger.getLogger(SearchManager.class);

   protected final QueryHandlerEntry config;

   /**
    * Text extractor for extracting text content of binary properties.
    */
   protected final DocumentReaderService extractor;

   /**
    * QueryHandler where query execution is delegated to
    */

   protected QueryHandler handler;

   /**
    * The shared item state manager instance for the workspace.
    */
   protected final ItemDataConsumer itemMgr;

   /**
    * The namespace registry of the repository.
    */
   protected final NamespaceRegistryImpl nsReg;

   /**
    * The node type registry.
    */
   protected final NodeTypeDataManager nodeTypeDataManager;

   /**
    * QueryHandler of the parent search manager or <code>null</code> if there
    * is none.
    */
   protected final SearchManager parentSearchManager;

   protected IndexingTree indexingTree;

   private final ConfigurationManager cfm;

   protected LuceneVirtualTableResolver virtualTableResolver;

   protected IndexerChangesFilter changesFilter;

   /**
    * Creates a new <code>SearchManager</code>.
    * 
    * @param config
    *            the search configuration.
    * @param nsReg
    *            the namespace registry.
    * @param ntReg
    *            the node type registry.
    * @param itemMgr
    *            the shared item state manager.
    * @param rootNodeId
    *            the id of the root node.
    * @param parentMgr
    *            the parent search manager or <code>null</code> if there is no
    *            parent search manager.
    * @param excludedNodeId
    *            id of the node that should be excluded from indexing. Any
    *            descendant of that node will also be excluded from indexing.
    * @throws RepositoryException
    *             if the search manager cannot be initialized
    * @throws RepositoryConfigurationException
    */

   public SearchManager(QueryHandlerEntry config, NamespaceRegistryImpl nsReg, NodeTypeDataManager ntReg,
      WorkspacePersistentDataManager itemMgr, SystemSearchManagerHolder parentSearchManager,
      DocumentReaderService extractor, ConfigurationManager cfm, final RepositoryIndexSearcherHolder indexSearcherHolder)
      throws RepositoryException, RepositoryConfigurationException
   {

      this.extractor = extractor;
      indexSearcherHolder.addIndexSearcher(this);
      this.config = config;
      this.nodeTypeDataManager = ntReg;
      this.nsReg = nsReg;
      this.itemMgr = itemMgr;
      this.cfm = cfm;
      this.virtualTableResolver = new LuceneVirtualTableResolver(nodeTypeDataManager, nsReg);
      this.parentSearchManager = parentSearchManager != null ? parentSearchManager.get() : null;
      if (parentSearchManager != null)
      {
         ((WorkspacePersistentDataManager)this.itemMgr).addItemPersistenceListener(this);
      }
   }

   public void createNewOrAdd(String key, ItemState state, Map<String, List<ItemState>> updatedNodes)
   {
      List<ItemState> list = updatedNodes.get(key);
      if (list == null)
      {
         list = new ArrayList<ItemState>();
         updatedNodes.put(key, list);
      }
      list.add(state);

   }

   /**
    * Creates a query object from a node that can be executed on the workspace.
    * 
    * @param session
    *            the session of the user executing the query.
    * @param itemMgr
    *            the item manager of the user executing the query. Needed to
    *            return <code>Node</code> instances in the result set.
    * @param node
    *            a node of type nt:query.
    * @return a <code>Query</code> instance to execute.
    * @throws InvalidQueryException
    *             if <code>absPath</code> is not a valid persisted query (that
    *             is, a node of type nt:query)
    * @throws RepositoryException
    *             if any other error occurs.
    */
   public Query createQuery(SessionImpl session, SessionDataManager sessionDataManager, Node node)
      throws InvalidQueryException, RepositoryException
   {
      AbstractQueryImpl query = createQueryInstance();
      query.init(session, sessionDataManager, handler, node);
      return query;
   }

   /**
    * Creates a query object that can be executed on the workspace.
    * 
    * @param session
    *            the session of the user executing the query.
    * @param itemMgr
    *            the item manager of the user executing the query. Needed to
    *            return <code>Node</code> instances in the result set.
    * @param statement
    *            the actual query statement.
    * @param language
    *            the syntax of the query statement.
    * @return a <code>Query</code> instance to execute.
    * @throws InvalidQueryException
    *             if the query is malformed or the <code>language</code> is
    *             unknown.
    * @throws RepositoryException
    *             if any other error occurs.
    */
   public Query createQuery(SessionImpl session, SessionDataManager sessionDataManager, String statement,
      String language) throws InvalidQueryException, RepositoryException
   {
      AbstractQueryImpl query = createQueryInstance();
      query.init(session, sessionDataManager, handler, statement, language);
      return query;
   }

   /**
    * {@inheritDoc}
    */
   public Set<String> getFieldNames() throws IndexException
   {
      final Set<String> fildsSet = new HashSet<String>();
      if (handler instanceof SearchIndex)
      {
         IndexReader reader = null;
         try
         {
            reader = ((SearchIndex)handler).getIndexReader();
            final Collection fields = reader.getFieldNames(IndexReader.FieldOption.ALL);
            for (final Object field : fields)
            {
               fildsSet.add((String)field);
            }
         }
         catch (IOException e)
         {
            throw new IndexException(e.getLocalizedMessage(), e);
         }
         finally
         {
            try
            {
               if (reader != null)
                  reader.close();
            }
            catch (IOException e)
            {
               throw new IndexException(e.getLocalizedMessage(), e);
            }
         }

      }
      return fildsSet;
   }

   /**
    * just for test use only
    */
   public QueryHandler getHandler()
   {

      return handler;
   }

   public Set<String> getNodesByNodeType(final InternalQName nodeType) throws RepositoryException
   {

      return getNodes(virtualTableResolver.resolve(nodeType, true));
   }

   /**
    * Return set of uuid of nodes. Contains in names prefixes maped to the
    * given uri
    * 
    * @param prefix
    * @return
    * @throws RepositoryException
    */
   public Set<String> getNodesByUri(final String uri) throws RepositoryException
   {
      Set<String> result;
      final int defaultClauseCount = BooleanQuery.getMaxClauseCount();
      try
      {

         // final LocationFactory locationFactory = new
         // LocationFactory(this);
         final ValueFactoryImpl valueFactory = new ValueFactoryImpl(new LocationFactory(nsReg));
         BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE);
         BooleanQuery query = new BooleanQuery();

         final String prefix = nsReg.getNamespacePrefixByURI(uri);
         query.add(new WildcardQuery(new Term(FieldNames.LABEL, prefix + ":*")), Occur.SHOULD);
         // name of the property
         query.add(new WildcardQuery(new Term(FieldNames.PROPERTIES_SET, prefix + ":*")), Occur.SHOULD);

         result = getNodes(query);

         // value of the property

         try
         {
            final Set<String> props = getFieldNames();

            query = new BooleanQuery();
            for (final String fieldName : props)
            {
               if (!FieldNames.PROPERTIES_SET.equals(fieldName))
               {
                  query.add(new WildcardQuery(new Term(fieldName, "*" + prefix + ":*")), Occur.SHOULD);
               }
            }
         }
         catch (final IndexException e)
         {
            throw new RepositoryException(e.getLocalizedMessage(), e);
         }

         final Set<String> propSet = getNodes(query);
         // Manually check property values;
         for (final String uuid : propSet)
         {
            if (isPrefixMatch(valueFactory, uuid, prefix))
            {
               result.add(uuid);
            }
         }
      }
      finally
      {
         BooleanQuery.setMaxClauseCount(defaultClauseCount);
      }

      return result;
   }

   /**
    * @see org.exoplatform.services.jcr.dataflow.persistent.ItemsPersistenceListener#onSaveItems(org.exoplatform.services.jcr.dataflow.ItemStateChangesLog)
    */
   public void onSaveItems(ItemStateChangesLog itemStates)
   {
      //skip empty
      if (itemStates.getSize() > 0)
      {
         //Check if SearchManager started and filter configured
         if (changesFilter != null)
         {
            changesFilter.onSaveItems(itemStates);
         }
      }
   }

   public void start()
   {

      if (log.isDebugEnabled())
         log.debug("start");
      try
      {
         if (indexingTree == null)
         {
            List<QPath> excludedPath = new ArrayList<QPath>();
            // Calculating excluded node identifiers
            excludedPath.add(Constants.JCR_SYSTEM_PATH);

            //if (config.getExcludedNodeIdentifers() != null)
            String excludedNodeIdentifer =
               config.getParameterValue(QueryHandlerParams.PARAM_EXCLUDED_NODE_IDENTIFERS, null);
            if (excludedNodeIdentifer != null)
            {
               StringTokenizer stringTokenizer = new StringTokenizer(excludedNodeIdentifer);
               while (stringTokenizer.hasMoreTokens())
               {

                  try
                  {
                     ItemData excludeData = itemMgr.getItemData(stringTokenizer.nextToken());
                     if (excludeData != null)
                        excludedPath.add(excludeData.getQPath());
                  }
                  catch (RepositoryException e)
                  {
                     log.warn(e.getLocalizedMessage());
                  }
               }
            }

            NodeData indexingRootData = null;
            String rootNodeIdentifer = config.getParameterValue(QueryHandlerParams.PARAM_ROOT_NODE_ID, null);
            if (rootNodeIdentifer != null)
            {
               try
               {
                  ItemData indexingRootDataItem = itemMgr.getItemData(rootNodeIdentifer);
                  if (indexingRootDataItem != null && indexingRootDataItem.isNode())
                     indexingRootData = (NodeData)indexingRootDataItem;
               }
               catch (RepositoryException e)
               {
                  log.warn(e.getLocalizedMessage() + " Indexing root set to " + Constants.ROOT_PATH.getAsString());

               }

            }
            else
            {
               try
               {
                  indexingRootData = (NodeData)itemMgr.getItemData(Constants.ROOT_UUID);
                  //               indexingRootData =
                  //                  new TransientNodeData(Constants.ROOT_PATH, Constants.ROOT_UUID, 1, Constants.NT_UNSTRUCTURED,
                  //                     new InternalQName[0], 0, null, new AccessControlList());
               }
               catch (RepositoryException e)
               {
                  log.error("Fail to load root node data");
               }
            }

            indexingTree = new IndexingTree(indexingRootData, excludedPath);
         }
         initializeQueryHandler();

      }
      catch (RepositoryException e)
      {
         log.error(e.getLocalizedMessage());
         handler = null;
         throw new RuntimeException(e.getLocalizedMessage(), e.getCause());
      }
      catch (RepositoryConfigurationException e)
      {
         log.error(e.getLocalizedMessage());
         handler = null;
         throw new RuntimeException(e.getLocalizedMessage(), e.getCause());
      }
   }

   public void stop()
   {
      handler.close();
      log.info("Search manager stopped");
   }

   /**
    * {@inheritDoc}
    */
   public void updateIndex(final Set<String> removedNodes, final Set<String> addedNodes) throws RepositoryException,
      IOException
   {
      if (handler != null)
      {
         Iterator<NodeData> addedStates = new Iterator<NodeData>()
         {
            private final Iterator<String> iter = addedNodes.iterator();

            public boolean hasNext()
            {
               return iter.hasNext();
            }

            public NodeData next()
            {

               // cycle till find a next or meet the end of set
               do
               {
                  String id = iter.next();
                  try
                  {
                     ItemData item = itemMgr.getItemData(id);
                     if (item != null)
                     {
                        if (item.isNode())
                        {
                           if (!indexingTree.isExcluded(item))
                              return (NodeData)item;
                        }
                        else
                           log.warn("Node not found, but property " + id + ", " + item.getQPath().getAsString()
                              + " found. ");
                     }
                     else
                        log.warn("Unable to index node with id " + id + ", node does not exist.");

                  }
                  catch (RepositoryException e)
                  {
                     log.error("Can't read next node data " + id, e);
                  }
               }
               while (iter.hasNext()); // get next if error or node not found

               return null; // we met the end of iterator set
            }

            public void remove()
            {
               throw new UnsupportedOperationException();
            }
         };

         Iterator<String> removedIds = new Iterator<String>()
         {
            private final Iterator<String> iter = removedNodes.iterator();

            public boolean hasNext()
            {
               return iter.hasNext();
            }

            public String next()
            {
               return nextNodeId();
            }

            public String nextNodeId() throws NoSuchElementException
            {
               return iter.next();
            }

            public void remove()
            {
               throw new UnsupportedOperationException();

            }
         };

         if (removedNodes.size() > 0 || addedNodes.size() > 0)
         {
            handler.updateNodes(removedIds, addedStates);
         }
      }

   }

   protected QueryHandlerContext createQueryHandlerContext(QueryHandler parentHandler)
      throws RepositoryConfigurationException
   {

      QueryHandlerContext context =
         new QueryHandlerContext(itemMgr, indexingTree, nodeTypeDataManager, nsReg, parentHandler, getIndexDir(),
            extractor, true, virtualTableResolver);
      return context;
   }

   /**
    * Creates a new instance of an {@link AbstractQueryImpl} which is not
    * initialized.
    * 
    * @return an new query instance.
    * @throws RepositoryException
    *             if an error occurs while creating a new query instance.
    */
   protected AbstractQueryImpl createQueryInstance() throws RepositoryException
   {
      try
      {
         String queryImplClassName = handler.getQueryClass();
         Object obj = Class.forName(queryImplClassName).newInstance();
         if (obj instanceof AbstractQueryImpl)
         {
            return (AbstractQueryImpl)obj;
         }
         else
         {
            throw new IllegalArgumentException(queryImplClassName + " is not of type "
               + AbstractQueryImpl.class.getName());
         }
      }
      catch (Throwable t)
      {
         throw new RepositoryException("Unable to create query: " + t.toString(), t);
      }
   }

   protected String getIndexDir() throws RepositoryConfigurationException
   {
      String dir = config.getParameterValue(QueryHandlerParams.PARAM_INDEX_DIR, null);
      if (dir == null)
      {
         log.warn(QueryHandlerParams.PARAM_INDEX_DIR + " parameter not found. Using outdated parameter name "
            + QueryHandlerParams.OLD_PARAM_INDEX_DIR);
         dir = config.getParameterValue(QueryHandlerParams.OLD_PARAM_INDEX_DIR);
      }
      return dir;
   }

   /**
    * @return the indexingTree
    */
   protected IndexingTree getIndexingTree()
   {
      return indexingTree;
   }

   /**
    * Initialize changes filter.
    * @throws RepositoryException
    * @throws RepositoryConfigurationException
    * @throws ClassNotFoundException 
    * @throws NoSuchMethodException 
    * @throws SecurityException 
    */
   protected IndexerChangesFilter initializeChangesFilter() throws RepositoryException,
      RepositoryConfigurationException

   {
      IndexerChangesFilter newChangesFilter = null;
      Class<? extends IndexerChangesFilter> changesFilterClass = DefaultChangesFilter.class;
      String changesFilterClassName = config.getParameterValue(QueryHandlerParams.PARAM_CHANGES_FILTER_CLASS, null);
      try
      {
         if (changesFilterClassName != null)
         {
            changesFilterClass =
               (Class<? extends IndexerChangesFilter>)Class.forName(changesFilterClassName, true, this.getClass()
                  .getClassLoader());
         }
         Constructor<? extends IndexerChangesFilter> constuctor =
            changesFilterClass.getConstructor(SearchManager.class, SearchManager.class, QueryHandlerEntry.class,
               IndexingTree.class, IndexingTree.class, QueryHandler.class, QueryHandler.class);
         if (parentSearchManager != null)
         {
            newChangesFilter =
               constuctor.newInstance(this, parentSearchManager, config, indexingTree, parentSearchManager
                  .getIndexingTree(), handler, parentSearchManager.getHandler());
         }
      }
      catch (SecurityException e)
      {
         throw new RepositoryException(e.getMessage(), e);
      }
      catch (IllegalArgumentException e)
      {
         throw new RepositoryException(e.getMessage(), e);
      }
      catch (ClassNotFoundException e)
      {
         throw new RepositoryException(e.getMessage(), e);
      }
      catch (NoSuchMethodException e)
      {
         throw new RepositoryException(e.getMessage(), e);
      }
      catch (InstantiationException e)
      {
         throw new RepositoryException(e.getMessage(), e);
      }
      catch (IllegalAccessException e)
      {
         throw new RepositoryException(e.getMessage(), e);
      }
      catch (InvocationTargetException e)
      {
         throw new RepositoryException(e.getMessage(), e);
      }
      return newChangesFilter;
   }

   /**
    * Initializes the query handler.
    * 
    * @throws RepositoryException
    *             if the query handler cannot be initialized.
    * @throws RepositoryConfigurationException
    * @throws ClassNotFoundException
    */
   protected void initializeQueryHandler() throws RepositoryException, RepositoryConfigurationException
   {
      // initialize query handler
      String className = config.getType();
      if (className == null)
         throw new RepositoryConfigurationException("Content hanler       configuration fail");

      try
      {
         Class qHandlerClass = Class.forName(className, true, this.getClass().getClassLoader());
         Constructor constuctor = qHandlerClass.getConstructor(QueryHandlerEntry.class, ConfigurationManager.class);
         handler = (QueryHandler)constuctor.newInstance(config, cfm);
         QueryHandler parentHandler = (this.parentSearchManager != null) ? parentSearchManager.getHandler() : null;
         QueryHandlerContext context = createQueryHandlerContext(parentHandler);
         handler.setContext(context);

         if (parentSearchManager != null)
         {
            changesFilter = initializeChangesFilter();
         }
      }
      catch (SecurityException e)
      {
         throw new RepositoryException(e.getMessage(), e);
      }
      catch (IllegalArgumentException e)
      {
         throw new RepositoryException(e.getMessage(), e);
      }
      catch (ClassNotFoundException e)
      {
         throw new RepositoryException(e.getMessage(), e);
      }
      catch (NoSuchMethodException e)
      {
         throw new RepositoryException(e.getMessage(), e);
      }
      catch (InstantiationException e)
      {
         throw new RepositoryException(e.getMessage(), e);
      }
      catch (IllegalAccessException e)
      {
         throw new RepositoryException(e.getMessage(), e);
      }
      catch (InvocationTargetException e)
      {
         throw new RepositoryException(e.getMessage(), e);
      }
   }

   /**
    * @param query
    * @return
    * @throws RepositoryException
    */
   private Set<String> getNodes(final org.apache.lucene.search.Query query) throws RepositoryException
   {
      Set<String> result = new HashSet<String>();
      try
      {
         QueryHits hits = handler.executeQuery(query);

         ScoreNode sn;

         while ((sn = hits.nextScoreNode()) != null)
         {
            // Node node = session.getNodeById(sn.getNodeId());
            result.add(sn.getNodeId());
         }
      }
      catch (IOException e)
      {
         throw new RepositoryException(e.getLocalizedMessage(), e);
      }
      return result;
   }

   private boolean isPrefixMatch(final InternalQName value, final String prefix) throws RepositoryException
   {
      return value.getNamespace().equals(nsReg.getNamespaceURIByPrefix(prefix));
   }

   private boolean isPrefixMatch(final QPath value, final String prefix) throws RepositoryException
   {
      for (int i = 0; i < value.getEntries().length; i++)
      {
         if (isPrefixMatch(value.getEntries()[i], prefix))
         {
            return true;
         }
      }
      return false;
   }

   /**
    * @param valueFactory
    * @param dm
    * @param uuid
    * @param prefix
    * @throws RepositoryException
    */
   private boolean isPrefixMatch(final ValueFactoryImpl valueFactory, final String uuid, final String prefix)
      throws RepositoryException
   {

      final ItemData node = itemMgr.getItemData(uuid);
      if (node != null && node.isNode())
      {
         final List<PropertyData> props = itemMgr.getChildPropertiesData((NodeData)node);
         for (final PropertyData propertyData : props)
         {
            if (propertyData.getType() == PropertyType.PATH || propertyData.getType() == PropertyType.NAME)
            {
               for (final ValueData vdata : propertyData.getValues())
               {
                  final Value val = valueFactory.loadValue(vdata, propertyData.getType());
                  if (propertyData.getType() == PropertyType.PATH)
                  {
                     if (isPrefixMatch(((PathValue)val).getQPath(), prefix))
                     {
                        return true;
                     }
                  }
                  else if (propertyData.getType() == PropertyType.NAME)
                  {
                     if (isPrefixMatch(((NameValue)val).getQName(), prefix))
                     {
                        return true;
                     }
                  }
               }
            }
         }
      }
      return false;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isTXAware()
   {
      return false;
   }

}
