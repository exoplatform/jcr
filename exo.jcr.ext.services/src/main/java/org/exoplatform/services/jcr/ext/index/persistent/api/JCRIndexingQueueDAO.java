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

import java.util.Calendar;
import java.util.List;

import org.exoplatform.commons.api.persistence.GenericDAO;
import org.exoplatform.services.jcr.ext.index.persistent.entity.JCRIndexQueueEntity;

public interface JCRIndexingQueueDAO extends GenericDAO<JCRIndexQueueEntity, Long> {

  List<JCRIndexQueueEntity> findAllOperationNotExecutedByClusterNode(int offset, int limit, String clusterNodeName);

  List<JCRIndexQueueEntity> findAllOperationExecutedByClusterNode(String oldClusterNodeName,
                                                                  String clusterNodeName,
                                                                  long lastExecutedId,
                                                                  int offset,
                                                                  int limit);

  void deleteOperationsByJCRUUID(String removeNodeUUID);

  /**
   * @param clusterNodeName a cluster node
   * @return the lowest id of the operations the cluster node did not execute,
   *         or null when it executed them all
   */
  Long findLowestOperationIdNotExecutedByClusterNode(String clusterNodeName);

  /**
   * @param workspace the workspace of the operations
   * @param clusterNodeName the cluster node that executed them
   * @param limit the maximum number of operations returned
   * @return the operations of the workspace that the cluster node executed, in
   *         id order
   */
  List<JCRIndexQueueEntity> findOperationsOfWorkspaceExecutedByClusterNode(String workspace,
                                                                          String clusterNodeName,
                                                                          int limit);

  /**
   * @param workspace the workspace of the operations
   * @param otherClusterNodeName the cluster node that executed them
   * @param clusterNodeName the cluster node that did not execute them
   * @param lastExecutedId the greatest id returned
   * @param queuedBefore the operations queued from that time on are not
   *          returned
   * @param limit the maximum number of operations returned
   * @return the operations of the workspace, up to the id and queued before
   *         the time, that the other cluster node executed and the cluster
   *         node did not, in id order
   */
  List<JCRIndexQueueEntity> findOperationsOfWorkspaceExecutedByAnotherClusterNode(String workspace,
                                                                                 String otherClusterNodeName,
                                                                                 String clusterNodeName,
                                                                                 long lastExecutedId,
                                                                                 Calendar queuedBefore,
                                                                                 int limit);

}
