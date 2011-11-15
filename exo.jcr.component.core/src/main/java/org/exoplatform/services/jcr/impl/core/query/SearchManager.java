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
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.WildcardQuery;
import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.services.document.DocumentReaderService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.QueryHandlerEntry;
import org.exoplatform.services.jcr.config.QueryHandlerParams;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
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
import org.exoplatform.services.jcr.impl.InspectionLog;
import org.exoplatform.services.jcr.impl.backup.BackupException;
import org.exoplatform.services.jcr.impl.backup.Backupable;
import org.exoplatform.services.jcr.impl.backup.DataRestore;
import org.exoplatform.services.jcr.impl.backup.ResumeException;
import org.exoplatform.services.jcr.impl.backup.SuspendException;
import org.exoplatform.services.jcr.impl.backup.Suspendable;
import org.exoplatform.services.jcr.impl.backup.rdbms.DataRestoreContext;
import org.exoplatform.services.jcr.impl.backup.rdbms.DirectoryRestore;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.NamespaceRegistryImpl;
import org.exoplatform.services.jcr.impl.core.SessionDataManager;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.query.lucene.ChangesHolder;
import org.exoplatform.services.jcr.impl.core.query.lucene.FieldNames;
import org.exoplatform.services.jcr.impl.core.query.lucene.IndexOfflineIOException;
import org.exoplatform.services.jcr.impl.core.query.lucene.IndexOfflineRepositoryException;
import org.exoplatform.services.jcr.impl.core.query.lucene.LuceneVirtualTableResolver;
import org.exoplatform.services.jcr.impl.core.query.lucene.QueryHits;
import org.exoplatform.services.jcr.impl.core.query.lucene.ScoreNode;
import org.exoplatform.services.jcr.impl.core.query.lucene.SearchIndex;
import org.exoplatform.services.jcr.impl.core.value.NameValue;
import org.exoplatform.services.jcr.impl.core.value.PathValue;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspacePersistentDataManager;
import org.exoplatform.services.jcr.impl.util.io.DirectoryHelper;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rpc.RPCException;
import org.exoplatform.services.rpc.RPCService;
import org.exoplatform.services.rpc.RemoteCommand;
import org.exoplatform.services.rpc.TopologyChangeEvent;
import org.exoplatform.services.rpc.TopologyChangeListener;
import org.jboss.cache.factories.annotations.NonVolatile;
import org.picocontainer.Startable;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
@Managed
@NameTemplate(@Property(key = "service", value = "SearchManager"))
public class SearchManager implements Startable, MandatoryItemsPersistenceListener, Suspendable, Backupable,
   TopologyChangeListener
{

   /**
    * Logger instance for this class
    */
   private static final Log log = ExoLogger.getLogger("exo.jcr.component.core.SearchManager");

   /**
    * Used to display date and time for JMX components 
    */
   private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

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
    * The Repository name.
    */
   protected final String repositoryName;

   /**
    * The Repository name.
    */
   protected final String workspaceName;

   /**
    * The repository service.
    */
   protected final RepositoryService rService;

   /**
    * The unique name of the related workspace
    */
   protected final String wsId;

   /**
    * The unique name of the workspace container
    */
   protected final String wsContainerId;

   /**
    * Component responsible for executing commands in cluster nodes.
    */
   protected final RPCService rpcService;

   /**
    * Indicates if component suspended or not.
    */
   protected boolean isSuspended = false;

   /**
    * Indicates that node keep responsible for resuming.
    */
   protected Boolean isResponsibleForResuming = false;

   /**
    * Suspend remote command.
    */
   private RemoteCommand suspend;

   /**
    * Resume remote command.
    */
   private RemoteCommand resume;

   /**
    * Index recovery.
    */
   private final IndexRecovery indexRecovery;

   /**
    * Request to all nodes to check if there is someone who responsible for resuming.
    */
   private RemoteCommand requestForResponsibleForResuming;

   /**
    * Switches index between online and offline modes
    */
   private RemoteCommand changeIndexState;

   private final ExoContainerContext ctx;

   private String hotReindexingState = "not stated";

   public SearchManager(ExoContainerContext ctx, WorkspaceEntry wEntry, RepositoryEntry rEntry,
      RepositoryService rService, QueryHandlerEntry config, NamespaceRegistryImpl nsReg, NodeTypeDataManager ntReg,
      WorkspacePersistentDataManager itemMgr, SystemSearchManagerHolder parentSearchManager,
      DocumentReaderService extractor, ConfigurationManager cfm, final RepositoryIndexSearcherHolder indexSearcherHolder)
      throws RepositoryException, RepositoryConfigurationException
   {
      this(ctx, wEntry, rEntry, rService, config, nsReg, ntReg, itemMgr, parentSearchManager, extractor, cfm,
         indexSearcherHolder, null);
   }

   /**
    * Creates a new <code>SearchManager</code>.
    * 
    * @param ctx
    *            The eXo Container context in which the SearchManager is registered
    * @param rEntry
    *            repository configuration
    * @param rService
    *            repository service  
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
    * @param rpcService
    *             the service for executing commands on all nodes of cluster           
    * @throws RepositoryException
    *             if the search manager cannot be initialized
    * @throws RepositoryConfigurationException
    */
   public SearchManager(ExoContainerContext ctx, WorkspaceEntry wEntry, RepositoryEntry rEntry,
      RepositoryService rService, QueryHandlerEntry config, NamespaceRegistryImpl nsReg, NodeTypeDataManager ntReg,
      WorkspacePersistentDataManager itemMgr, SystemSearchManagerHolder parentSearchManager,
      DocumentReaderService extractor, ConfigurationManager cfm,
      final RepositoryIndexSearcherHolder indexSearcherHolder, RPCService rpcService) throws RepositoryException,
      RepositoryConfigurationException
   {
      this.ctx = ctx;
      this.wsContainerId = ctx.getName();
      this.rpcService = rpcService;
      this.repositoryName = rEntry.getName();
      this.workspaceName = wEntry.getName();
      this.rService = rService;
      this.wsId = wEntry.getUniqueName();
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

      if (rpcService == null)
      {
         this.indexRecovery = null;
      }
      else
      {
         doInitRemoteCommands();

         this.indexRecovery = new IndexRecoveryImpl(rpcService, this);
         rpcService.registerTopologyChangeListener(this);
         rpcService.registerTopologyChangeListener((TopologyChangeListener)indexRecovery);
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
    * Check index consistency. Iterator goes through index documents and check, does each document have
    * according jcr-node. If index is suspended then it will be temporary resumed, while check is running and suspended afterwards.
	 */
   public void checkIndex(final InspectionLog inspectionLog, final boolean isSystem) throws RepositoryException, IOException
   { 
	  
      if (isSuspended)
      {
         try
         {
            SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<Object>()
            {
               public Object run() throws RepositoryException, IOException
               {
                  // try resuming the workspace
                  try
                  {
                     if (isSystem && parentSearchManager != null && parentSearchManager.isSuspended)
                     {
                        parentSearchManager.resume();
                     }
                     resume();
                     
                     handler.checkIndex(itemMgr, isSystem, inspectionLog);
                     return null;
                  }
                  catch (ResumeException e)
                  {
                     throw new RepositoryException("Can not resume SearchManager for inspection purposes.", e);
                  }
                  finally
                  {
                     // safely return the state of the workspace
                     try
                     {
                        suspend();
                        if (isSystem && parentSearchManager != null && !parentSearchManager.isSuspended)
                        {
                           parentSearchManager.suspend();
                        }
                     }
                     catch (SuspendException e)
                     {
                        log.error(e.getMessage(), e);
                     }
                  }
               }
            });
         }
         catch (PrivilegedActionException e)
         {
            Throwable ex = e.getCause();
            if (ex instanceof RepositoryException)
            {
               throw (RepositoryException)ex;
            }
            else if (ex instanceof IOException)
            {
               throw (IOException)ex;
            }
            else
            {
               throw new RepositoryException(ex.getMessage(), ex);
            }
         }
      }
      else
      {
         // simply run checkIndex, if not suspended
         handler.checkIndex(itemMgr, isSystem, inspectionLog);
      }
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
            final Collection<?> fields = reader.getFieldNames(IndexReader.FieldOption.ALL);
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
               {
                  reader.close();
               }
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
    * {@inheritDoc}
    */
   public void onSaveItems(final ItemStateChangesLog itemStates)
   {
      //skip empty
      if (itemStates.getSize() > 0)
      {
         //Check if SearchManager started and filter configured
         if (changesFilter != null && parentSearchManager!=null)
         {
            changesFilter.onSaveItems(itemStates);
         }
      }
   }

   public void start()
   {

      if (log.isDebugEnabled())
      {
         log.debug("start");
      }
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
                     {
                        excludedPath.add(excludeData.getQPath());
                     }
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
                  {
                     indexingRootData = (NodeData)indexingRootDataItem;
                  }
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
      // ChangesFiler instance is one for both SearchManagers and close() must be invoked only once,  
      if (parentSearchManager != null)
      {
         changesFilter.close();
      }
      log.info("Search manager stopped");
   }

   /**
    * {@inheritDoc}
    */
   public void updateIndex(final Set<String> removedNodes, final Set<String> addedNodes) throws RepositoryException,
      IOException
   {
      final ChangesHolder changes = getChanges(removedNodes, addedNodes);
      apply(changes);
   }

   public void apply(ChangesHolder changes) throws RepositoryException, IOException
   {
      if (handler != null && changes != null && (!changes.getAdd().isEmpty() || !changes.getRemove().isEmpty()))
      {
         handler.apply(changes);
      }
   }

   /**
    * Extracts all the changes and returns them as a {@link ChangesHolder} instance
    */
   public ChangesHolder getChanges(final Set<String> removedNodes, final Set<String> addedNodes)
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
                           {
                              return (NodeData)item;
                           }
                        }
                        else
                        {
                           log.warn("Node not found, but property " + id + ", " + item.getQPath().getAsString()
                              + " found. ");
                        }
                     }
                     else
                     {
                        log.warn("Unable to index node with id " + id + ", node does not exist.");
                     }

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
            return handler.getChanges(removedIds, addedStates);
         }
      }
      return null;
   }

   protected QueryHandlerContext createQueryHandlerContext(QueryHandler parentHandler)
      throws RepositoryConfigurationException
   {
      WorkspaceContainerFacade container;
      try
      {
         container = rService.getRepository(repositoryName).getWorkspaceContainer(workspaceName);
      }
      catch (RepositoryException e)
      {
         throw new RepositoryConfigurationException(e);
      }

      // Recovery Filters are 
      String changesFilterClassName = config.getParameterValue(QueryHandlerParams.PARAM_CHANGES_FILTER_CLASS, null);
      boolean recoveryFilterUsed = false;

      if (changesFilterClassName != null)
      {
         try
         {
            Class<?> changesFilterClass = Class.forName(changesFilterClassName);
            // Set recoveryFilterUsed, if changes filter implements LocalIndex strategy 
            if (changesFilterClass != null)
            {
               recoveryFilterUsed = LocalIndexMarker.class.isAssignableFrom(changesFilterClass);
            }
         }
         catch (ClassNotFoundException e)
         {
            throw new RepositoryConfigurationException(e.getMessage(), e);
         }
      }

      QueryHandlerContext context =
         new QueryHandlerContext(container, itemMgr, indexingTree, nodeTypeDataManager, nsReg, parentHandler,
            PrivilegedFileHelper.getAbsolutePath(getIndexDirectory()), extractor, true, recoveryFilterUsed,
            virtualTableResolver, indexRecovery, rpcService, repositoryName, wsId);

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

   /**^
    * Returns "index-dir" parameter from configuration. 
    */
   protected String getIndexDirParam() throws RepositoryConfigurationException
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
    * @return the ctx
    */
   public ExoContainerContext getExoContainerContext()
   {
      return ctx;
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
               IndexingTree.class, IndexingTree.class, QueryHandler.class, QueryHandler.class,
               ConfigurationManager.class);
         if (parentSearchManager != null)
         {
            newChangesFilter =
               constuctor.newInstance(this, parentSearchManager, config, indexingTree, parentSearchManager
                  .getIndexingTree(), handler, parentSearchManager.getHandler(), cfm);
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
      {
         throw new RepositoryConfigurationException("Content hanler       configuration fail");
      }

      try
      {
         Class<?> qHandlerClass = Class.forName(className, true, this.getClass().getClassLoader());
         try
         {
            // We first try a constructor with the workspace id
            Constructor<?> constuctor =
               qHandlerClass.getConstructor(String.class, QueryHandlerEntry.class, ConfigurationManager.class);
            handler = (QueryHandler)constuctor.newInstance(wsContainerId, config, cfm);
         }
         catch (NoSuchMethodException e)
         {
            // No constructor with the workspace id can be found so we use the default constructor
            Constructor<?> constuctor =
               qHandlerClass.getConstructor(QueryHandlerEntry.class, ConfigurationManager.class);
            handler = (QueryHandler)constuctor.newInstance(config, cfm);
         }
         QueryHandler parentHandler = (this.parentSearchManager != null) ? parentSearchManager.getHandler() : null;
         QueryHandlerContext context = createQueryHandlerContext(parentHandler);
         handler.setContext(context);

         if (parentSearchManager != null)
         {
            changesFilter = initializeChangesFilter();
            parentSearchManager.setChangesFilter(changesFilter);
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
    * Inserts the instance of {@link IndexerChangesFilter} into the {@link SearchManager}. 
    * Used to set instance for {@link SystemSearchManager}.
    * 
    * @param changesFilter
    */
   protected void setChangesFilter(IndexerChangesFilter changesFilter)
   {
      if (this.changesFilter == null)
      {
         this.changesFilter = changesFilter;
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
      QueryHits hits = null;
      try
      {
         hits = handler.executeQuery(query);

         ScoreNode sn;
         while ((sn = hits.nextScoreNode()) != null)
         {
            result.add(sn.getNodeId());
         }
         return result;
      }
      catch (IndexOfflineIOException e)
      {
         throw new IndexOfflineRepositoryException(e.getMessage(), e);
      }
      catch (IOException e)
      {
         throw new RepositoryException(e.getLocalizedMessage(), e);
      }
      finally
      {
         if (hits != null)
         {
            try
            {
               hits.close();
            }
            catch (IOException e)
            {
               log.error("Can not close QueryHits.", e);
            }
         }
      }
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

   public String getWsId()
   {
      return wsId;
   }

   /**
    * {@inheritDoc}
    */
   public void suspend() throws SuspendException
   {
      isResponsibleForResuming = true;

      if (rpcService != null)
      {
         try
         {
            rpcService.executeCommandOnAllNodes(suspend, true);
         }
         catch (SecurityException e)
         {
            throw new SuspendException(e);
         }
         catch (RPCException e)
         {
            throw new SuspendException(e);
         }
      }
      else
      {
         suspendLocally();
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean isSuspended()
   {
      return isSuspended;
   }

   /**
    * Switches index into corresponding ONLINE or OFFLINE mode. Offline mode means that new indexing data is
    * collected but index is guaranteed to be unmodified during offline state. Passing the allowQuery flag, can
    * allow or deny performing queries on index during offline mode. AllowQuery is not used when setting index
    * back online. When dropStaleIndexes is set, indexes present on the moment of switching index offline will be
    * marked as stale and removed on switching it back online.
    * 
    * @param isOnline
    * @param allowQuery
    *          doesn't matter, when switching index to online
    * @param dropStaleIndexes
    *          doesn't matter, when switching index to online
    * @throws IOException
    */
   public void setOnline(boolean isOnline, boolean allowQuery, boolean dropStaleIndexes) throws IOException
   {
      handler.setOnline(isOnline, allowQuery, dropStaleIndexes);
   }

   public boolean isOnline()
   {
      return handler.isOnline();
   }

   /**
    * {@inheritDoc}
    */
   public void resume() throws ResumeException
   {
      if (rpcService != null)
      {
         try
         {
            rpcService.executeCommandOnAllNodes(resume, true);
         }
         catch (SecurityException e)
         {
            throw new ResumeException(e);
         }
         catch (RPCException e)
         {
            throw new ResumeException(e);
         }
      }
      else
      {
         resumeLocally();
      }

      isResponsibleForResuming = false;
   }

   /**
    * Public method, designed to be called via JMX, to perform "HOT" reindexing of the workspace
    * 
    * @throws IOException
    * @throws IllegalStateException
    */
   @Managed
   @ManagedDescription("Starts hot async reindexing")
   public void reindex(final boolean dropExisting) throws IllegalStateException
   {
      // checks
      if (handler == null || handler.getIndexerIoModeHandler() == null || changesFilter == null)
      {
         throw new IllegalStateException("Index might have not been initialized yet.");
      }
      if (handler.getIndexerIoModeHandler().getMode() != IndexerIoMode.READ_WRITE)
      {
         throw new IllegalStateException(
            "Index is not in READ_WRITE mode and reindexing can't be launched. Please start reindexing on coordinator node.");
      }
      if (isSuspended || !handler.isOnline())
      {
         throw new IllegalStateException("Can't start reindexing while index is "
            + ((isSuspended) ? "SUSPENDED." : "already OFFLINE (it means that reindexing is in progress).") + ".");
      }

      log.info("Starting hot reindexing on the " + handler.getContext().getRepositoryName() + "/"
         + handler.getContext().getContainer().getWorkspaceName() + ", with" + (dropExisting ? "" : "out")
         + " dropping the existing indexes.");
      // starting new thread, releasing JMX call
      new Thread(new Runnable()
      {
         public void run()
         {
            boolean successful = false;
            hotReindexingState = "Running. Started at " + sdf.format(Calendar.getInstance().getTime());
            try
            {
               isResponsibleForResuming = true;
               // set offline cluster wide (will make merger disposed and volatile flushed)
               if (rpcService != null && changesFilter.isShared())
               {
                  rpcService.executeCommandOnAllNodes(changeIndexState, true, false, !dropExisting);
               }
               else
               {
                  handler.setOnline(false, !dropExisting, true);
               }
               // launch reindexing thread safely, resume nodes if any exception occurs
               if (handler instanceof SearchIndex)
               {
                  ((SearchIndex)handler).getIndex().reindex(itemMgr);
                  successful = true;
               }
               else
               {
                  log.error("This kind of QuerHandler class doesn't support hot reindxing.");
               }
            }
            catch (RepositoryException e)
            {
               log.error("Error while reindexing the workspace", e);
            }
            catch (SecurityException e)
            {
               log.error("Can't change state to offline.", e);
            }
            catch (RPCException e)
            {
               log.error("Can't change state to offline.", e);
            }
            catch (IOException e)
            {
               log.error("Erroe while reindexing the workspace", e);
            }
            // safely change state back
            finally
            {
               // finish, setting indexes back online
               if (rpcService != null && changesFilter.isShared())
               {
                  try
                  {
                     // if dropExisting, then queries are no allowed
                     rpcService.executeCommandOnAllNodes(changeIndexState, true, true, true);
                  }
                  catch (SecurityException e)
                  {
                     log.error("Error setting index back online in a cluster", e);
                  }
                  catch (RPCException e)
                  {
                     log.error("Error setting index back online in a cluster", e);
                  }
               }
               else
               {
                  try
                  {
                     handler.setOnline(true, true, true);
                  }
                  catch (IOException e)
                  {
                     log.error("Error setting index back online locally");
                  }
               }
               if (successful)
               {
                  hotReindexingState = "Finished at " + sdf.format(Calendar.getInstance().getTime());
                  log.info("Reindexing finished successfully.");
               }
               else
               {
                  hotReindexingState = "Stopped with errors at " + sdf.format(Calendar.getInstance().getTime());
                  log.info("Reindexing halted with errors.");
               }
               isResponsibleForResuming = false;
            }
         }
      }, "HotReindexing-" + handler.getContext().getRepositoryName() + "-"
         + handler.getContext().getContainer().getWorkspaceName()).start();
   }

   @Managed
   @ManagedDescription("Hot async reindexing state")
   public String getHotReindexingState()
   {
      return hotReindexingState;
   }

   @Managed
   @ManagedDescription("Index IO mode (READ_ONLY/READ_WRITE)")
   public String getIOMode()
   {
      if (handler == null || handler.getIndexerIoModeHandler() == null)
      {
         return "not initialized";
      }
      return (handler.getIndexerIoModeHandler().getMode() == IndexerIoMode.READ_WRITE) ? "READ_WRITE" : "READ_ONLY";
   }

   @Managed
   @ManagedDescription("Index state (Online/Offline(indexing))")
   public String getState()
   {
      if (handler == null)
      {
         return "not initialized";
      }
      return handler.isOnline() ? "Online" : "Offline (indexing)";
   }

   @Managed
   @ManagedDescription("QueryHandler class")
   public String getQuerHandlerClass()
   {
      if (handler != null)
      {
         return handler.getClass().getCanonicalName();
      }
      else
      {
         return "not initialized";
      }
   }

   @Managed
   @ManagedDescription("ChangesFilter class")
   public String getChangesFilterClass()
   {
      if (changesFilter != null)
      {
         return changesFilter.getClass().getCanonicalName();
      }
      else
      {
         return "not initialized";
      }
   }

   /**
    * Register remote commands.
    */
   private void doInitRemoteCommands()
   {
      // register commands
      suspend = rpcService.registerCommand(new RemoteCommand()
      {

         public String getId()
         {
            return "org.exoplatform.services.jcr.impl.core.query.SearchManager-suspend-" + wsId + "-"
               + (parentSearchManager == null);
         }

         public Serializable execute(Serializable[] args) throws Throwable
         {
            suspendLocally();
            return null;
         }
      });

      resume = rpcService.registerCommand(new RemoteCommand()
      {

         public String getId()
         {
            return "org.exoplatform.services.jcr.impl.core.query.SearchManager-resume-" + wsId + "-"
               + (parentSearchManager == null);
         }

         public Serializable execute(Serializable[] args) throws Throwable
         {
            resumeLocally();
            return null;
         }
      });

      requestForResponsibleForResuming = rpcService.registerCommand(new RemoteCommand()
      {

         public String getId()
         {
            return "org.exoplatform.services.jcr.impl.core.query.SearchManager-requestForResponsibilityForResuming-"
               + wsId + "-" + (parentSearchManager == null);
         }

         public Serializable execute(Serializable[] args) throws Throwable
         {
            return isResponsibleForResuming;
         }
      });

      changeIndexState = rpcService.registerCommand(new RemoteCommand()
      {
         public String getId()
         {
            return "org.exoplatform.services.jcr.impl.core.query.SearchManager-changeIndexerState-" + wsId + "-"
               + (parentSearchManager == null);
         }

         public Serializable execute(Serializable[] args) throws Throwable
         {
            boolean isOnline = (Boolean)args[0];
            boolean allowQuery = (args.length == 2) ? (Boolean)args[1] : false;
            SearchManager.this.setOnline(isOnline, allowQuery, true);
            return null;
         }
      });

   }

   protected void suspendLocally() throws SuspendException
   {
      if (!handler.isOnline())
      {
         throw new SuspendException("Can't suspend index, while reindexing in progeress.");
      }
      if (isSuspended)
      {
         throw new SuspendException("Component already suspended.");
      }

      if (handler instanceof Suspendable)
      {
         ((Suspendable)handler).suspend();
      }

      isSuspended = true;
   }

   protected void resumeLocally() throws ResumeException
   {
      if (!isSuspended)
      {
         throw new ResumeException("Component is not suspended.");
      }

      if (handler instanceof Suspendable)
      {
         ((Suspendable)handler).resume();
      }

      isSuspended = false;
   }

   /**
    * {@inheritDoc}
    */
   public void onChange(TopologyChangeEvent event)
   {
      if (isSuspended)
      {
         new Thread()
         {
            @Override
            public synchronized void run()
            {
               try
               {
                  List<Object> results = rpcService.executeCommandOnAllNodes(requestForResponsibleForResuming, true);

                  for (Object result : results)
                  {
                     if ((Boolean)result)
                     {
                        return;
                     }
                  }

                  // node which was responsible for resuming leave the cluster, so resume component
                  try
                  {
                     resumeLocally();
                  }
                  catch (ResumeException e)
                  {
                     log.error("Can not resume component", e);
                  }
               }
               catch (SecurityException e1)
               {
                  log.error("You haven't privileges to execute remote command", e1);
               }
               catch (RPCException e1)
               {
                  log.error("Exception during command execution", e1);
               }
            }
         }.start();
      }
   }

   /**
    * {@inheritDoc}}
    */
   public void clean() throws BackupException
   {
      try
      {
         DirectoryHelper.removeDirectory(getIndexDirectory());
      }
      catch (IOException e)
      {
         throw new BackupException(e);
      }
      catch (RepositoryConfigurationException e)
      {
         throw new BackupException(e);
      }
   }

   /**
    * {@inheritDoc}}
    */
   public void backup(File storageDir) throws BackupException
   {
      try
      {
         File indexDir = getIndexDirectory();

         if (!PrivilegedFileHelper.exists(indexDir))
         {
            throw new BackupException("Can't backup index. Directory "
               + PrivilegedFileHelper.getCanonicalPath(indexDir) + " doesn't exists");
         }
         else
         {
            File destZip = new File(storageDir, getStorageName() + ".zip");
            DirectoryHelper.compressDirectory(indexDir, destZip);
         }
      }
      catch (RepositoryConfigurationException e)
      {
         throw new BackupException(e);
      }
      catch (IOException e)
      {
         throw new BackupException(e);
      }
   }

   /**
    * Returns the index directory.
    * 
    * @return File
    * @throws RepositoryConfigurationException
    */
   protected File getIndexDirectory() throws RepositoryConfigurationException
   {
      return new File(getIndexDirParam());
   }

   /**
    * Returns storage name of index.
    * 
    * @return String
    */
   protected String getStorageName()
   {
      return "index";
   }

   /**
    * {@inheritDoc}}
    */
   public DataRestore getDataRestorer(DataRestoreContext context) throws BackupException
   {
      try
      {
         File zipFile = new File((File)context.getObject(DataRestoreContext.STORAGE_DIR), getStorageName() + ".zip");

         if (PrivilegedFileHelper.exists(zipFile))
         {
            return new DirectoryRestore(getIndexDirectory(), zipFile);
         }
         else
         {
            // try to check if we have deal with old backup format
            zipFile = new File((File)context.getObject(DataRestoreContext.STORAGE_DIR), getStorageName());
            if (PrivilegedFileHelper.exists(zipFile))
            {
               return new DirectoryRestore(getIndexDirectory(), zipFile);
            }
            else
            {
               throw new BackupException("There is no backup data for index");
            }
         }
      }
      catch (RepositoryConfigurationException e)
      {
         throw new BackupException(e);
      }
   }
   
   /**
    * {@inheritDoc}
    */
   public int getPriority()
   {
      return PRIORITY_NORMAL;
   }
}
