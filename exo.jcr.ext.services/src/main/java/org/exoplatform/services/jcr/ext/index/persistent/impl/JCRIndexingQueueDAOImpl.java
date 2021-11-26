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

package org.exoplatform.services.jcr.ext.index.persistent.impl;

import java.util.List;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.services.jcr.ext.index.persistent.api.JCRIndexingQueueDAO;
import org.exoplatform.services.jcr.ext.index.persistent.entity.JCRIndexQueueEntity;

public class JCRIndexingQueueDAOImpl extends GenericDAOJPAImpl<JCRIndexQueueEntity, Long> implements JCRIndexingQueueDAO {

  @Override
  @ExoTransactional
  public List<JCRIndexQueueEntity> findAllOperationNotExecutedByClusterNode(int offset, int limit, String clusterNodeName) {
    return getEntityManager().createNamedQuery("JCRIndexingQueue.findAllOperationNotExecutedByClusterNode",
                                               JCRIndexQueueEntity.class)
                             .setParameter("clusterNodeName", clusterNodeName)
                             .setFirstResult(offset)
                             .setMaxResults(limit)
                             .getResultList();
  }

  @Override
  public List<JCRIndexQueueEntity> findAllOperationExecutedByClusterNode(String oldClusterNodeName,
                                                                         String clusterNodeName,
                                                                         long lastExecutedId,
                                                                         int offset,
                                                                         int limit) {
    return getEntityManager().createNamedQuery("JCRIndexingQueue.findAllOperationNotExecutedByClusterNodePreceedingAnID",
                                               JCRIndexQueueEntity.class)
                             .setParameter("lastExecutedId", lastExecutedId)
                             .setParameter("oldClusterNodeName", oldClusterNodeName)
                             .setParameter("clusterNodeName", clusterNodeName)
                             .setFirstResult(offset)
                             .setMaxResults(limit)
                             .getResultList();
  }

  @Override
  @ExoTransactional
  public void deleteOperationsByJCRUUID(String removeNodeUUID) {
    getEntityManager().createNamedQuery("JCRIndexingQueue.deleteOperationsByJCRUUID")
                      .setParameter("jcrUUID", removeNodeUUID)
                      .executeUpdate();
  }

}
