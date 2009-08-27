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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;

import org.picocontainer.Startable;

import org.exoplatform.services.log.Log;

import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.document.DocumentReaderService;
import org.exoplatform.services.jcr.config.QueryHandlerEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.persistent.MandatoryItemsPersistenceListener;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.NamespaceRegistryImpl;
import org.exoplatform.services.jcr.impl.core.SessionDataManager;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspacePersistentDataManager;
import org.exoplatform.services.log.ExoLogger;

/**
 * Acts as a global entry point to execute queries and index nodes.
 */
public class SearchManager
   implements Startable, MandatoryItemsPersistenceListener
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
    * QueryHandler of the parent search manager or <code>null</code> if there is
    * none.
    */
   protected final SearchManager parentSearchManager;

   protected QPath indexingRoot;

   protected List<QPath> excludedPaths = new ArrayList<QPath>();

   private final ConfigurationManager cfm;

   /**
    * Creates a new <code>SearchManager</code>.
    * 
    * @param config the search configuration.
    * @param nsReg the namespace registry.
    * @param ntReg the node type registry.
    * @param itemMgr the shared item state manager.
    * @param rootNodeId the id of the root node.
    * @param parentMgr the parent search manager or <code>null</code> if there is
    *          no parent search manager.
    * @param excludedNodeId id of the node that should be excluded from indexing.
    *          Any descendant of that node will also be excluded from indexing.
    * @throws RepositoryException if the search manager cannot be initialized
    * @throws RepositoryConfigurationException
    */
   public SearchManager(QueryHandlerEntry config, NamespaceRegistryImpl nsReg, NodeTypeDataManager ntReg,
            WorkspacePersistentDataManager itemMgr, SystemSearchManagerHolder parentSearchManager,
            DocumentReaderService extractor, ConfigurationManager cfm) throws RepositoryException,
            RepositoryConfigurationException
   {

      this.extractor = extractor;

      this.config = config;
      this.nodeTypeDataManager = ntReg;
      this.nsReg = nsReg;
      this.itemMgr = itemMgr;
      this.cfm = cfm;

      this.parentSearchManager = parentSearchManager != null ? parentSearchManager.get() : null;
      itemMgr.addItemPersistenceListener(this);
      initializeQueryHandler();
   }

   /**
    * Creates a query object from a node that can be executed on the workspace.
    * 
    * @param session the session of the user executing the query.
    * @param itemMgr the item manager of the user executing the query. Needed to
    *          return <code>Node</code> instances in the result set.
    * @param node a node of type nt:query.
    * @return a <code>Query</code> instance to execute.
    * @throws InvalidQueryException if <code>absPath</code> is not a valid
    *           persisted query (that is, a node of type nt:query)
    * @throws RepositoryException if any other error occurs.
    */
   public Query createQuery(SessionImpl session, SessionDataManager sessionDataManager, Node node)
            throws InvalidQueryException, RepositoryException
   {
      AbstractQueryImpl query = handler.createQueryInstance();
      query.init(session, sessionDataManager, handler, node);
      return query;
   }

   /**
    * Creates a query object that can be executed on the workspace.
    * 
    * @param session the session of the user executing the query.
    * @param itemMgr the item manager of the user executing the query. Needed to
    *          return <code>Node</code> instances in the result set.
    * @param statement the actual query statement.
    * @param language the syntax of the query statement.
    * @return a <code>Query</code> instance to execute.
    * @throws InvalidQueryException if the query is malformed or the
    *           <code>language</code> is unknown.
    * @throws RepositoryException if any other error occurs.
    */
   public Query createQuery(SessionImpl session, SessionDataManager sessionDataManager, String statement,
            String language) throws InvalidQueryException, RepositoryException
   {
      AbstractQueryImpl query = handler.createQueryInstance();
      query.init(session, sessionDataManager, handler, statement, language);
      return query;
   }

   /**
    * just for test use only
    */
   public QueryHandler getHandler()
   {

      return handler;
   }

   public void onSaveItems(ItemStateChangesLog changesLog)
   {
      if (handler == null)
         return;

      long time = System.currentTimeMillis();

      // nodes that need to be removed from the index.
      final Set<String> removedNodes = new HashSet<String>();
      // nodes that need to be added to the index.
      final Set<String> addedNodes = new HashSet<String>();

      final Map<String, List<ItemState>> updatedNodes = new HashMap<String, List<ItemState>>();

      for (Iterator<ItemState> iter = changesLog.getAllStates().iterator(); iter.hasNext();)
      {
         ItemState itemState = iter.next();

         if (!isExcluded(itemState))
         {
            String uuid =
                     itemState.isNode() ? itemState.getData().getIdentifier() : itemState.getData()
                              .getParentIdentifier();

            if (itemState.isAdded())
            {
               if (itemState.isNode())
               {
                  addedNodes.add(uuid);
               }
               else
               {
                  if (!addedNodes.contains(uuid))
                  {
                     createNewOrAdd(uuid, itemState, updatedNodes);
                  }
               }
            }
            else if (itemState.isRenamed())
            {
               if (itemState.isNode())
               {
                  addedNodes.add(uuid);
               }
               else
               {
                  createNewOrAdd(uuid, itemState, updatedNodes);
               }
            }
            else if (itemState.isUpdated())
            {
               createNewOrAdd(uuid, itemState, updatedNodes);
            }
            else if (itemState.isMixinChanged())
            {
               createNewOrAdd(uuid, itemState, updatedNodes);
            }
            else if (itemState.isDeleted())
            {
               if (itemState.isNode())
               {
                  if (addedNodes.contains(uuid))
                  {
                     addedNodes.remove(uuid);
                     removedNodes.remove(uuid);
                  }
                  else
                  {
                     removedNodes.add(uuid);
                  }
                  // remove all changes after node remove
                  updatedNodes.remove(uuid);
               }
               else
               {
                  if (!removedNodes.contains(uuid) && !addedNodes.contains(uuid))
                  {
                     createNewOrAdd(uuid, itemState, updatedNodes);
                  }
               }
            }
         }
      }
      // TODO make quick changes
      for (String uuid : updatedNodes.keySet())
      {
         removedNodes.add(uuid);
         addedNodes.add(uuid);
      }

      // // property events
      // List<ItemState> propEvents = new ArrayList<ItemState>();
      // List<ItemState> itemStates = changesLog.getAllStates();
      //
      // final Set<String> allRemovedNodesId = new HashSet<String>();
      // final Set<String> allAddedNodesId = new HashSet<String>();
      // for (ItemState itemState : itemStates) {
      // if (!isExcluded(itemState)) {
      // if (itemState.isNode()) {
      // if (itemState.isAdded() || itemState.isRenamed()) {
      // addedNodes.add(itemState.getData().getIdentifier());
      // allAddedNodesId.add(itemState.getData().getIdentifier());
      // } else if (itemState.isDeleted()) {
      // // remove node from add list, and if node not in add list add it to
      // // removed list
      // if (!addedNodes.remove(itemState.getData().getIdentifier()))
      // removedNodes.add(itemState.getData().getIdentifier());
      // allRemovedNodesId.add(itemState.getData().getIdentifier());
      // } else if (itemState.isMixinChanged()) {
      // removedNodes.add(itemState.getData().getIdentifier());
      // addedNodes.add(itemState.getData().getIdentifier());
      // }
      // } else {
      // propEvents.add(itemState);
      // }
      // }
      // }
      //
      // // sort out property events
      // for (int i = 0; i < propEvents.size(); i++) {
      // ItemState event = propEvents.get(i);
      // String nodeId = event.getData().getParentIdentifier();
      // if (event.isAdded()) {
      // if (!addedNodes.contains(nodeId) && !allAddedNodesId.contains(nodeId)) {
      // // only property added
      // // need to re-index
      // addedNodes.add(nodeId);
      // removedNodes.add(nodeId);
      // } else {
      // // the node where this prop belongs to is also new
      // }
      // } else if (event.isRenamed() || event.isUpdated()) {
      // // need to re-index
      // addedNodes.add(nodeId);
      // removedNodes.add(nodeId);
      // } else if (event.isDeleted()) {
      // if (!allRemovedNodesId.contains(nodeId)) {
      // addedNodes.add(nodeId);
      // removedNodes.add(nodeId);
      // }
      // }
      // }

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
                        return (NodeData) item; // return node
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
         try
         {
            handler.updateNodes(removedIds, addedStates);
         }
         catch (RepositoryException e)
         {
            log.error("Error indexing changes " + e, e);
         }
         catch (IOException e)
         {
            log.error("Error indexing changes " + e, e);
            try
            {
               handler.logErrorChanges(removedNodes, addedNodes);
            }
            catch (IOException ioe)
            {
               log.warn("Exception occure when errorLog writed. Error log is not complete. " + ioe, ioe);
            }
         }
      }

      if (log.isDebugEnabled())
      {
         log.debug("onEvent: indexing finished in " + String.valueOf(System.currentTimeMillis() - time) + " ms.");
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

   public void start()
   {

      if (log.isDebugEnabled())
         log.debug("start");

      // Calculating excluded node identifiers
      excludedPaths.add(Constants.JCR_SYSTEM_PATH);

      if (config.getExcludedNodeIdentifers() != null)
      {
         StringTokenizer stringTokenizer = new StringTokenizer(config.getExcludedNodeIdentifers());
         while (stringTokenizer.hasMoreTokens())
         {

            try
            {
               ItemData excludeData = itemMgr.getItemData(stringTokenizer.nextToken());
               if (excludeData != null)
                  excludedPaths.add(excludeData.getQPath());
            }
            catch (RepositoryException e)
            {
               log.warn(e.getLocalizedMessage());
            }
         }
      }

      indexingRoot = Constants.ROOT_PATH;
      if (config.getRootNodeIdentifer() != null)
      {
         try
         {
            ItemData indexingRootData = itemMgr.getItemData(config.getRootNodeIdentifer());
            if (indexingRootData != null && indexingRootData.isNode())
               indexingRoot = indexingRootData.getQPath();
         }
         catch (RepositoryException e)
         {
            log.warn(e.getLocalizedMessage() + " Indexing root set to " + indexingRoot.getAsString());
         }

      }
      handler.init();
   }

   public void stop()
   {
      handler.close();
      log.info("Search manager stopped");
   }

   /**
    * Checks if the given event should be excluded based on the
    * {@link #excludePath} setting.
    * 
    * @param event observation event
    * @return <code>true</code> if the event should be excluded,
    *         <code>false</code> otherwise
    */
   protected boolean isExcluded(ItemState event)
   {

      for (QPath excludedPath : excludedPaths)
      {
         if (event.getData().getQPath().isDescendantOf(excludedPath) || event.getData().getQPath().equals(excludedPath))
            return true;
      }

      return !event.getData().getQPath().isDescendantOf(indexingRoot)
               && !event.getData().getQPath().equals(indexingRoot);
   }

   protected QueryHandlerContext createQueryHandlerContext(QueryHandler parentHandler)
            throws RepositoryConfigurationException
   {

      QueryHandlerContext context =
               new QueryHandlerContext(itemMgr, config.getRootNodeIdentifer() != null ? config.getRootNodeIdentifer()
                        : Constants.ROOT_UUID, nodeTypeDataManager, nsReg, parentHandler, config.getIndexDir(),
                        extractor);
      return context;
   }

   /**
    * Initializes the query handler.
    * 
    * @throws RepositoryException if the query handler cannot be initialized.
    * @throws RepositoryConfigurationException
    * @throws ClassNotFoundException
    */
   private void initializeQueryHandler() throws RepositoryException, RepositoryConfigurationException
   {
      // initialize query handler
      String className = config.getType();
      if (className == null)
         throw new RepositoryConfigurationException("Content hanler       configuration fail");

      try
      {
         Class qHandlerClass = Class.forName(className, true, this.getClass().getClassLoader());
         Constructor constuctor = qHandlerClass.getConstructor(QueryHandlerEntry.class, ConfigurationManager.class);
         handler = (QueryHandler) constuctor.newInstance(config, cfm);

         QueryHandler parentHandler = (this.parentSearchManager != null) ? parentSearchManager.getHandler() : null;
         QueryHandlerContext context = createQueryHandlerContext(parentHandler);
         handler.setContext(context);
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
      catch (IOException e)
      {
         throw new RepositoryException(e.getMessage(), e);
      }
   }

}
