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
package org.exoplatform.services.jcr.ext.index.persistent.api;

import java.util.Set;
import java.util.function.BooleanSupplier;

import org.exoplatform.services.jcr.config.QueryHandlerEntry;
import org.exoplatform.services.jcr.impl.core.query.ChangesFilterListsWrapper;
import org.exoplatform.services.jcr.impl.core.query.QueryHandler;

public interface JCRIndexingService {

  /**
   * 1/ Read from Queue <br>
   * 2/ Index in JCR <br>
   * 3/ Update Queue to mark operation as executed for this node
   * 
   * @throws Exception
   */
  void processIndexingQueue() throws Exception; // NOSONAR

  /**
   * JCR Index Changes applied on JCR to propagate for other nodes through Queue
   * 
   * @param changes
   * @param workspaceId REPOSITORY_WORKSPACE
   */
  void applyIndexChangesOnQueue(ChangesFilterListsWrapper changes, String workspaceId);

  /**
   * JCR Index Changes applied on JCR to propagate for other nodes through Queue
   * 
   * @param removedNodes removed nodes JCR UUID
   * @param addedNodes added nodes JCR UUID
   * @param parentRemovedNodes removed Parent JCR nodes UUID
   * @param parentAddedNodes added Parent JCR nodes UUID
   * @param workspaceId REPOSITORY_WORKSPACE
   */
  void applyIndexChangesOnQueue(Set<String> removedNodes,
                                Set<String> addedNodes,
                                Set<String> parentRemovedNodes,
                                Set<String> parentAddedNodes,
                                String workspaceId);

  /**
   * Initialize JCR Indexing Service
   * 
   * @param handler
   * @param config
   */
  void init(QueryHandler handler, QueryHandlerEntry config);

  /**
   * Called once a workspace's index is ready at startup. When the index was
   * copied from another cluster node, the queue operations of the workspace are
   * marked as that copy holds them, so that this node applies what the copy
   * lacks.
   *
   * @param workspaceId the workspace of the index, as its queue operations name
   *          it
   * @param indexDirectory the index's directory
   * @param preparingACopy true while this node prepares a copy of the index for
   *          another node: the index's marker is not written meanwhile
   */
  default void registerWorkspaceIndex(String workspaceId, String indexDirectory, BooleanSupplier preparingACopy) {
    // Nothing to mark without a queue
  }

}
