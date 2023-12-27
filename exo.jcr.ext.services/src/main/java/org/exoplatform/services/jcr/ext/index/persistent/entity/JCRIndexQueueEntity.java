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
package org.exoplatform.services.jcr.ext.index.persistent.entity;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import org.exoplatform.commons.api.persistence.ExoEntity;
import org.exoplatform.services.jcr.ext.index.persistent.JCRIndexingOperationType;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity(name = "JCRIndexingQueue")
@ExoEntity
@Table(name = "JCR_INDEXING_QUEUE")
@NamedQueries(
  {
      @NamedQuery(
          name = "JCRIndexingQueue.findAllOperationNotExecutedByClusterNode",
          query = "SELECT idx FROM JCRIndexingQueue idx"
              + " WHERE :clusterNodeName NOT MEMBER OF idx.nodes"
              + " ORDER BY idx.modificationDate ASC"
      ),
      @NamedQuery(
          name = "JCRIndexingQueue.findAllOperationNotExecutedByClusterNodePreceedingAnID",
          query = "SELECT idx FROM JCRIndexingQueue idx"
              + " WHERE idx.id < :lastExecutedId "
              + " AND :oldClusterNodeName MEMBER OF idx.nodes"
              + " AND :clusterNodeName NOT MEMBER OF idx.nodes"
              + " ORDER BY idx.id ASC"
      ),
      @NamedQuery(
          name = "JCRIndexingQueue.deleteAllIndexingOperationsSince",
          query = "Delete FROM JCRIndexingQueue idx WHERE idx.id < :id"
      ),
      @NamedQuery(
          name = "JCRIndexingQueue.deleteOperationsByJCRUUID",
          query = "Delete FROM JCRIndexingQueue idx WHERE idx.jcrUUID = :jcrUUID"
      )
  }
)
public class JCRIndexQueueEntity {
  @Id
  @Column(name = "INDEXING_QUEUE_ID")
  @SequenceGenerator(name = "SEQ_JCR_INDEXING_QUEUE", sequenceName = "SEQ_JCR_INDEXING_QUEUE", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_JCR_INDEXING_QUEUE")
  private long                     id;

  @Column(name = "JCR_UUID", nullable = false)
  private String                   jcrUUID;

  @Column(name = "WORKSPACE", nullable = false)
  private String                   workspace;

  @Column(name = "OPERATION_TYPE", nullable = false)
  private JCRIndexingOperationType operationType;

  @Column(name = "MODIFICATION_DATE", nullable = false)
  private Calendar                 modificationDate;

  @Column(name = "IS_PARENT_CHANGE", nullable = false)
  private boolean                  parentChange;

  @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
  @CollectionTable(name = "JCR_INDEXING_QUEUE_NODES", joinColumns = @JoinColumn(name = "INDEXING_QUEUE_ID"))
  @Column(name = "NODE_NAME", nullable = false)
  private Set<String>              nodes;

  public JCRIndexQueueEntity() {
  }

  public JCRIndexQueueEntity(String jcrUUID,
                             String workspaceName,
                             JCRIndexingOperationType operationType,
                             Calendar modificationDate,
                             boolean parentChange,
                             String nodeName) {
    this.jcrUUID = jcrUUID;
    this.workspace = workspaceName;
    this.operationType = operationType;
    this.modificationDate = modificationDate;
    this.parentChange = parentChange;
    addNode(nodeName);
  }

  public long getId() {
    return id;
  }

  public String getJcrUUID() {
    return jcrUUID;
  }

  public void setJcrUUID(String jcrUUID) {
    this.jcrUUID = jcrUUID;
  }

  public JCRIndexingOperationType getOperationType() {
    return operationType;
  }

  public void setOperationType(JCRIndexingOperationType operationType) {
    this.operationType = operationType;
  }

  public Calendar getModificationDate() {
    return modificationDate;
  }

  public void setModificationDate(Calendar modificationDate) {
    this.modificationDate = modificationDate;
  }

  public String getWorkspace() {
    return workspace;
  }

  public void setWorkspace(String workspace) {
    this.workspace = workspace;
  }

  public boolean isParentChange() {
    return parentChange;
  }

  public void setParentChange(boolean parentChange) {
    this.parentChange = parentChange;
  }

  public Set<String> getNodes() {
    return nodes;
  }

  public void addNode(String nodeName) {
    if (this.nodes == null) {
      this.nodes = new HashSet<>();
    }
    this.nodes.add(nodeName);
  }
}
