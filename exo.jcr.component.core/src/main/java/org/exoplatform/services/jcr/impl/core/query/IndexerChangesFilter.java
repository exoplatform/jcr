/*
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.services.jcr.impl.core.query;

import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.jcr.config.QueryHandlerEntry;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.persistent.ItemsPersistenceListener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@exoplatform.org">Sergey Kabashnyuk</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 2009-07-22 23:58:59Z ksm $
 *
 */
public abstract class IndexerChangesFilter implements ItemsPersistenceListener
{
   /**
    * Logger instance for this class
    */
   private static final Log log = ExoLogger.getLogger("exo.jcr.component.core.DefaultChangesFilter");

   protected final SearchManager searchManager;

   protected final QueryHandlerEntry config;

   protected final QueryHandler handler;

   protected final QueryHandler parentHandler;

   protected final IndexingTree indexingTree;

   protected final SearchManager parentSearchManager;

   protected final IndexingTree parentIndexingTree;

   /**
    * @param searchManager
    * @param isSystem 
    * @param handler
    * @param indexingTree
    */
   public IndexerChangesFilter(SearchManager searchManager, SearchManager parentSearchManager,
      QueryHandlerEntry config, IndexingTree indexingTree, IndexingTree parentIndexingTree, QueryHandler handler,
      QueryHandler parentHandler, ConfigurationManager cfm)
   {
      super();
      this.searchManager = searchManager;
      this.parentSearchManager = parentSearchManager;
      this.config = config;
      this.parentIndexingTree = parentIndexingTree;
      this.indexingTree = indexingTree;
      this.handler = handler;
      this.parentHandler = parentHandler;
   }

   /**
    * @return the handler
    */
   public QueryHandler getHandler()
   {
      return handler;
   }

   /**
    * @return the indexingTree
    */
   public IndexingTree getIndexingTree()
   {
      return indexingTree;
   }

   /**
    * @return the searchManager
    */
   public SearchManager getSearchManager()
   {
      return searchManager;
   }

   /**
    * @see org.exoplatform.services.jcr.dataflow.persistent.ItemsPersistenceListener#onSaveItems(org.exoplatform.services.jcr.dataflow.ItemStateChangesLog)
    */
   public void onSaveItems(ItemStateChangesLog itemStates)
   {
      // nodes that need to be removed from the index.
      final Set<String> removedNodes = new HashSet<String>();
      // nodes that need to be added to the index.
      final Set<String> addedNodes = new HashSet<String>();
      //updated
      final Map<String, List<ItemState>> updatedNodes = new HashMap<String, List<ItemState>>();

      final Set<String> parentRemovedNodes = new HashSet<String>();
      // nodes that need to be added to the index.
      final Set<String> parentAddedNodes = new HashSet<String>();
      //updated
      final Map<String, List<ItemState>> parentUpdatedNodes = new HashMap<String, List<ItemState>>();

      for (Iterator<ItemState> iter = itemStates.getAllStates().iterator(); iter.hasNext();)
      {
         ItemState itemState = iter.next();

         if (itemState.isPersisted())
         {
            if (!indexingTree.isExcluded(itemState))
            {
               acceptChanges(removedNodes, addedNodes, updatedNodes, itemState);
            }
            else if (parentIndexingTree != null && !parentIndexingTree.isExcluded(itemState))
            {
               acceptChanges(parentRemovedNodes, parentAddedNodes, parentUpdatedNodes, itemState);
            }
         }
      }

      for (String uuid : updatedNodes.keySet())
      {
         removedNodes.add(uuid);
         addedNodes.add(uuid);
      }

      for (String uuid : parentUpdatedNodes.keySet())
      {
         parentRemovedNodes.add(uuid);
         parentAddedNodes.add(uuid);
      }

      doUpdateIndex(removedNodes, addedNodes, parentRemovedNodes, parentAddedNodes);
   }

   /**
    * @param removedNodes
    * @param addedNodes
    * @param updatedNodes
    * @param itemState
    */
   private void acceptChanges(final Set<String> removedNodes, final Set<String> addedNodes,
      final Map<String, List<ItemState>> updatedNodes, ItemState itemState)
   {
      String uuid =
         itemState.isNode() ? itemState.getData().getIdentifier() : itemState.getData().getParentIdentifier();

      if (itemState.isNode())
      {
         if (itemState.isAdded())
         {
            addedNodes.add(uuid);
         }
         else if (itemState.isRenamed() || itemState.isUpdated() || itemState.isMixinChanged())
         {
            createNewOrAdd(uuid, itemState, updatedNodes);
         }
         else if (itemState.isDeleted())
         {
            addedNodes.remove(uuid);
            removedNodes.add(uuid);

            // remove all changes after node remove
            updatedNodes.remove(uuid);
         }
      }
      else
      {
         if (itemState.isAdded() || itemState.isUpdated() || itemState.isDeleted())
         {
            if (!addedNodes.contains(uuid) && !removedNodes.contains(uuid) && !updatedNodes.containsKey(uuid))
            {
               createNewOrAdd(uuid, itemState, updatedNodes);
            }
         }
      }
   }

   /**
    * Update index.
    * @param removedNodes
    * @param addedNodes
    */
   protected abstract void doUpdateIndex(Set<String> removedNodes, Set<String> addedNodes,
      Set<String> parentRemovedNodes, Set<String> parentAddedNodes);

   private void createNewOrAdd(String key, ItemState state, Map<String, List<ItemState>> updatedNodes)
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
    * {@inheritDoc}
    */
   public boolean isTXAware()
   {
      return true;
   }
}
