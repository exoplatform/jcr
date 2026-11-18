/* 
 * Copyright (C) 2003-2020 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/ .
 */
package org.exoplatform.services.jcr.ext.index.persistent.filter;

import java.util.Set;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.jcr.config.QueryHandlerEntry;
import org.exoplatform.services.jcr.ext.index.persistent.api.JCRIndexingService;
import org.exoplatform.services.jcr.impl.core.query.*;
import org.exoplatform.services.jcr.impl.core.query.lucene.SearchIndex;

public class PersistentIndexChangesFilter extends DefaultChangesFilter implements LocalIndexMarker {

  private JCRIndexingService jcrIndexingQueueService;

  public PersistentIndexChangesFilter(SearchManager searchManager,
                                      SearchManager parentSearchManager,
                                      QueryHandlerEntry config,
                                      IndexingTree indexingTree,
                                      IndexingTree parentIndexingTree,
                                      QueryHandler handler,
                                      QueryHandler parentHandler,
                                      ConfigurationManager cfm)
      throws Exception {
    super(searchManager, parentSearchManager, config, indexingTree, parentIndexingTree, handler, parentHandler, cfm, false);
    this.jcrIndexingQueueService= ExoContainerContext.getService(JCRIndexingService.class);
    this.jcrIndexingQueueService.init(handler, config);

    super.init();
    if (!keepsTheQueueMarks(handler)) {
      // The index is ready, and may have been copied from another cluster node
      IndexRecovery indexRecovery = handler.getContext().getIndexRecovery();
      this.jcrIndexingQueueService.registerWorkspaceIndex(searchManager.getWsId(),
                                                          handler.getContext().getIndexDirectory(),
                                                          () -> indexRecovery != null && indexRecovery.isPreparingACopy());
    }
  }

  /**
   * An index keeps the queue marks as they are when its copy can lack what its
   * marker names, or can land after this registration: the rsync recovery
   * strategies copy the index files as the other node last flushed them, and an
   * asynchronous reindexing of an empty index copies it in the background. The
   * search index does not tell, at this point, whether its background
   * reindexing runs, so every index with asynchronous reindexing keeps its
   * marks, even one copied before this registration.
   */
  static boolean keepsTheQueueMarks(QueryHandler handler) {
    if (handler instanceof SearchIndex searchIndex) {
      String strategy = searchIndex.getIndexRecoveryStrategy();
      return searchIndex.isAsyncReindexing()
          || SearchIndex.INDEX_RECOVERY_RSYNC_STRATEGY.equals(strategy)
          || SearchIndex.INDEX_RECOVERY_RSYNC_WITH_DELETE_STRATEGY.equals(strategy);
    }
    return false;
  }

  /**
   * Update JCR indexes and add JCR indexing operations in QUEUE {@inheritDoc}
   */
  @Override
  protected void doUpdateIndex(Set<String> removedNodes,
                               Set<String> addedNodes,
                               Set<String> parentRemovedNodes,
                               Set<String> parentAddedNodes) {
    try {
      jcrIndexingQueueService.processIndexingQueue();
    } catch (Exception e) {
      throw new IllegalStateException("An error occurred while indexing from queue before applying Index changes", e);
    }
    super.doUpdateIndex(removedNodes, addedNodes, parentRemovedNodes, parentAddedNodes);
    jcrIndexingQueueService.applyIndexChangesOnQueue(removedNodes,
                                                          addedNodes,
                                                          parentRemovedNodes,
                                                          parentAddedNodes,
                                                          getSearchManager().getWsId());
  }

  @Override
  protected void doUpdateIndex(final ChangesFilterListsWrapper changes) {
    try {
      jcrIndexingQueueService.processIndexingQueue();
    } catch (Exception e) {
      throw new IllegalStateException("An error occurred while indexing from queue before applying Index changes", e);
    }
    super.doUpdateIndex(changes);
    jcrIndexingQueueService.applyIndexChangesOnQueue(changes, getSearchManager().getWsId());
  }


}
