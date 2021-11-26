/*
 * Copyright (C) 2003-2020 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.ext.index.persistent.filter;

import java.util.Set;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.jcr.config.QueryHandlerEntry;
import org.exoplatform.services.jcr.ext.index.persistent.api.JCRIndexingService;
import org.exoplatform.services.jcr.impl.core.query.*;

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

    getJcrIndexingQueueService().init(handler, config);

    super.init();
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
      getJcrIndexingQueueService().processIndexingQueue();
    } catch (Exception e) {
      throw new IllegalStateException("An error occurred while indexing from queue before applying Index changes", e);
    }
    super.doUpdateIndex(removedNodes, addedNodes, parentRemovedNodes, parentAddedNodes);
    getJcrIndexingQueueService().applyIndexChangesOnQueue(removedNodes,
                                                          addedNodes,
                                                          parentRemovedNodes,
                                                          parentAddedNodes,
                                                          getSearchManager().getWsId());
  }

  @Override
  protected void doUpdateIndex(final ChangesFilterListsWrapper changes) {
    try {
      getJcrIndexingQueueService().processIndexingQueue();
    } catch (Exception e) {
      throw new IllegalStateException("An error occurred while indexing from queue before applying Index changes", e);
    }
    super.doUpdateIndex(changes);
    getJcrIndexingQueueService().applyIndexChangesOnQueue(changes, getSearchManager().getWsId());
  }

  private JCRIndexingService getJcrIndexingQueueService() {
    if (jcrIndexingQueueService == null) {
      jcrIndexingQueueService = CommonsUtils.getService(JCRIndexingService.class);
    }
    return jcrIndexingQueueService;
  }

}
