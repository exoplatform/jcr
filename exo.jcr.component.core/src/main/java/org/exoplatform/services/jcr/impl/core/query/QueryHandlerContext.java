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

import org.exoplatform.services.document.DocumentReaderService;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.impl.core.NamespaceRegistryImpl;
import org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeDataManagerImpl;
import org.exoplatform.services.jcr.impl.core.query.lucene.LuceneVirtualTableResolver;

/**
 * Acts as an argument for the {@link QueryHandler} to keep the interface
 * stable. This class provides access to the environment where the query handler
 * is running in.
 */
public class QueryHandlerContext
{
   /**
    * The persistent <code>ItemStateManager</code>
    */
   private final ItemDataConsumer stateMgr;

   /**
    * The node type registry of the repository
    */
   private final NodeTypeDataManager nodeTypeDataManager;

   /**
    * The namespace registry of the repository.
    */
   private final NamespaceRegistryImpl nsRegistry;

   /**
    * The id of the root node.
    */
   private final IndexingTree indexingTree;

   /**
    * PropertyType registry to look up the type of a property with a given
    * name.
    */
   private final PropertyTypeRegistry propRegistry;

   /**
    * The query handler for the jcr:system tree
    */
   private final QueryHandler parentHandler;

   /**
    * Text extractor for extracting text content of binary properties.
    */
   private final DocumentReaderService extractor;

   private final String indexDirectory;

   private final boolean createInitialIndex;

   private final LuceneVirtualTableResolver virtualTableResolver;

   /**
    * Workspace container.
    */
   private final WorkspaceContainerFacade container;

   /** 
    * The class responsible for index retrieving from other place. 
    */
   private final IndexRecovery indexRecovery;
   
   /**
    * Creates a new context instance.
    * 
    * @param fs
    *            a this <code>QueryHandler</code> may use to store its index.
    *            If no <code>FileSystem</code> has been configured
    *            <code>fs</code> is <code>null</code>.
    * @param stateMgr
    *            provides persistent item states.
    * @param rootId
    *            the id of the root node.
    * @param ntRegistry
    *            the node type registry.
    * @param nsRegistry
    *            the namespace registry.
    * @param parentHandler
    *            the parent query handler or <code>null</code> it there is no
    *            parent handler.
    * @param virtualTableResolver
    * @param excludedNodeId
    *            id of the node that should be excluded from indexing. Any
    *            descendant of that node is also excluded from indexing.
    * @param indexRecovery
    *            the index retriever from other place     
    */
   public QueryHandlerContext(WorkspaceContainerFacade container, ItemDataConsumer stateMgr, IndexingTree indexingTree,
      NodeTypeDataManager nodeTypeDataManager, NamespaceRegistryImpl nsRegistry, QueryHandler parentHandler,
      String indexDirectory, DocumentReaderService extractor, boolean createInitialIndex,
      LuceneVirtualTableResolver virtualTableResolver, IndexRecovery indexRecovery)
   {
      this.indexRecovery = indexRecovery;
      this.container = container;
      this.stateMgr = stateMgr;
      this.indexingTree = indexingTree;
      this.nodeTypeDataManager = nodeTypeDataManager;
      this.nsRegistry = nsRegistry;
      this.indexDirectory = indexDirectory;
      this.extractor = extractor;
      this.createInitialIndex = createInitialIndex;
      this.virtualTableResolver = virtualTableResolver;
      this.propRegistry = new PropertyTypeRegistry(nodeTypeDataManager);
      this.parentHandler = parentHandler;
      ((NodeTypeDataManagerImpl)this.nodeTypeDataManager).addListener(propRegistry);
   }

   /**
    * @return the workspace container
    */
   public WorkspaceContainerFacade getContainer()
   {
      return container;
   }

   /**
    * @return the virtualTableResolver
    */
   public LuceneVirtualTableResolver getVirtualTableResolver()
   {
      return virtualTableResolver;
   }

   /**
    * @return the createInitialIndex
    */
   public boolean isCreateInitialIndex()
   {
      return createInitialIndex;
   }

   /**
    * Returns the persistent {@link ItemStateManager} of the workspace this
    * <code>QueryHandler</code> is based on.
    * 
    * @return the persistent <code>ItemStateManager</code> of the current
    *         workspace.
    */
   public ItemDataConsumer getItemStateManager()
   {
      return stateMgr;
   }

   /**
    * Returns the id of the root node.
    * 
    * @return the idof the root node.
    */
   public IndexingTree getIndexingTree()
   {
      return indexingTree;
   }

   /**
    * Returns the PropertyTypeRegistry for this repository.
    * 
    * @return the PropertyTypeRegistry for this repository.
    */
   public PropertyTypeRegistry getPropertyTypeRegistry()
   {
      return propRegistry;
   }

   /**
    * Returns the NodeTypeRegistry for this repository.
    * 
    * @return the NodeTypeRegistry for this repository.
    */
   public NodeTypeDataManager getNodeTypeDataManager()
   {
      return nodeTypeDataManager;
   }

   /**
    * Returns the NamespaceRegistryImpl for this repository.
    * 
    * @return the NamespaceRegistryImpl for this repository.
    */
   public NamespaceRegistryImpl getNamespaceRegistry()
   {
      return nsRegistry;
   }

   /**
    * Returns the parent query handler.
    * 
    * @return the parent query handler.
    */
   public QueryHandler getParentHandler()
   {
      return parentHandler;
   }

   /**
    * Destroys this context and releases resources.
    */
   public void destroy()
   {
      ((NodeTypeDataManagerImpl)this.nodeTypeDataManager).removeListener(propRegistry);
   }

   public DocumentReaderService getExtractor()
   {
      return extractor;
   }

   public String getIndexDirectory()
   {
      return indexDirectory;
   }

   public IndexRecovery getIndexRecovery()
   {
      return indexRecovery;
   }

}
