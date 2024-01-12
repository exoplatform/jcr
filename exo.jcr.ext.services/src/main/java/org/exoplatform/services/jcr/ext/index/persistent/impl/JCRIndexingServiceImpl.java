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
package org.exoplatform.services.jcr.ext.index.persistent.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.picocontainer.Startable;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;
import org.exoplatform.container.RootContainer.PortalContainerPostInitTask;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.QueryHandlerEntry;
import org.exoplatform.services.jcr.config.QueryHandlerParams;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.ext.index.persistent.JCRIndexingOperationType;
import org.exoplatform.services.jcr.ext.index.persistent.api.JCRIndexingQueueDAO;
import org.exoplatform.services.jcr.ext.index.persistent.api.JCRIndexingService;
import org.exoplatform.services.jcr.ext.index.persistent.api.TransientQueueEntrySet;
import org.exoplatform.services.jcr.ext.index.persistent.entity.JCRIndexQueueEntity;
import org.exoplatform.services.jcr.impl.core.query.ChangesFilterListsWrapper;
import org.exoplatform.services.jcr.impl.core.query.IndexRecovery;
import org.exoplatform.services.jcr.impl.core.query.QueryHandler;
import org.exoplatform.services.jcr.impl.core.query.SearchManager;
import org.exoplatform.services.jcr.impl.core.query.lucene.ChangesHolder;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rpc.RPCService;

import jakarta.servlet.ServletContext;

public class JCRIndexingServiceImpl implements JCRIndexingService, Startable {

  public static final String                                  LAST_OPERATION_FILE_NAME              = "lastOperationID";

  private static final Log                                    LOG                                   =
                                                                  ExoLogger.getLogger(JCRIndexingServiceImpl.class);

  private static final String                                 CLUSTER_NODE_NAME_PARAMETER           = "cluster.node.name";

  private static final String                                 BATCH_NUMBER_PARAM                    = "batch.number";

  private static final String                                 QUEUE_PROCESSING_PERIOD_SECONDS_PARAM = "queue.periodicity.seconds";

  private final JCRIndexingQueueDAO                           indexingQueueDAO;

  private final RepositoryService                             repositoryService;

  private final EntityManagerService                          entityManagerService;

  private String                                              repositoryName;

  private String                                              clusterNodeName;

  private ThreadLocal<Boolean>                                isIndexingLocally                     = new ThreadLocal<>();

  private int                                                 queueProcessingPeriod                 = 5;

  private final ConcurrentLinkedQueue<TransientQueueEntrySet> transientQueueToPersist               =
                                                                                      new ConcurrentLinkedQueue<>();

  private ScheduledExecutorService                            filePersisterThread                   =
                                                                                  Executors.newSingleThreadScheduledExecutor();

  private ScheduledExecutorService                            queueProducingJobService              =
                                                                                       Executors.newSingleThreadScheduledExecutor();

  private ScheduledExecutorService                            queueConsumingJobService              =
                                                                                       Executors.newSingleThreadScheduledExecutor();

  private AtomicBoolean                                       indexingOperationIsRunning            = new AtomicBoolean();

  private AtomicLong                                          storedLastOperationId                 = new AtomicLong(0);

  private AtomicLong                                          localLastOperationId                  = null;

  private File                                                localLastOperationFile                = null;

  private int                                                 batchNumber                           = 100;

  private ExoContainer                                        container;

  private boolean                                             initialized;

  public JCRIndexingServiceImpl(EntityManagerService entityManagerService,
                                RepositoryService repositoryService,
                                JCRIndexingQueueDAO indexingQueueDAO,
                                InitParams params) {
    this.indexingQueueDAO = indexingQueueDAO;
    this.repositoryService = repositoryService;
    this.entityManagerService = entityManagerService;

    if (params.containsKey(BATCH_NUMBER_PARAM)) {
      String batchNumberValue = params.getValueParam(BATCH_NUMBER_PARAM).getValue();
      try {
        batchNumber = Integer.parseInt(batchNumberValue);
      } catch (NumberFormatException e) {
        LOG.warn("Invalid parameter {} with value {}. default value will be used",
                 BATCH_NUMBER_PARAM,
                 batchNumberValue,
                 batchNumber);
      }
    }

    if (params.containsKey(QUEUE_PROCESSING_PERIOD_SECONDS_PARAM)) {
      String value = params.getValueParam(QUEUE_PROCESSING_PERIOD_SECONDS_PARAM).getValue();
      try {
        queueProcessingPeriod = Integer.parseInt(value);
      } catch (NumberFormatException e) {
        LOG.warn("Invalid parameter {} with value {}. default value will be used",
                 QUEUE_PROCESSING_PERIOD_SECONDS_PARAM,
                 value,
                 queueProcessingPeriod);
      }
    }
    clusterNodeName =
                    params.containsKey(CLUSTER_NODE_NAME_PARAMETER) ? params.getValueParam(CLUSTER_NODE_NAME_PARAMETER).getValue()
                                                                    : null;
    if (StringUtils.isBlank(clusterNodeName)) {
      throw new IllegalStateException(CLUSTER_NODE_NAME_PARAMETER + " parameter is empty");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void processIndexingQueue() throws Exception {
    if (!initialized || isIndexingInCurrentThread()) {
      return;
    }
    isIndexingLocally.set(true);
    computeCurrentContainer();
    RequestLifeCycle.begin(entityManagerService);
    try {
      applyIndexChangesOnJCR();
    } finally {
      isIndexingLocally.remove();
      RequestLifeCycle.end();
    }
  }

  @Override
  public void start() {
    if (container instanceof PortalContainer) {
      PortalContainer portalContainer = (PortalContainer) container;
      PortalContainer.addInitTask(portalContainer.getPortalContext(), new PortalContainerPostInitTask() {
        @Override
        public void execute(ServletContext context, PortalContainer portalContainer) {
          scheduleIndexTasks();
        }
      });
    }
  }

  @Override
  public void stop() {
    if (!queueProducingJobService.isShutdown() && !queueProducingJobService.isTerminated()) {
      queueProducingJobService.shutdownNow();
    }
    if (!queueConsumingJobService.isShutdown() && !queueConsumingJobService.isTerminated()) {
      queueConsumingJobService.shutdownNow();
    }
    if (!filePersisterThread.isShutdown() && !filePersisterThread.isTerminated()) {
      filePersisterThread.shutdownNow();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void applyIndexChangesOnQueue(ChangesFilterListsWrapper changes, String workspaceId) {
    applyIndexChangesOnQueue(changes.getRemovedNodes(),
                             changes.getAddedNodes(),
                             changes.getParentRemovedNodes(),
                             changes.getParentAddedNodes(),
                             workspaceId);
  }

  @Override
  public void applyIndexChangesOnQueue(final Set<String> removedNodes,
                                       final Set<String> addedNodes,
                                       final Set<String> parentRemovedNodes,
                                       final Set<String> parentAddedNodes,
                                       String workspaceId) {
    transientQueueToPersist.offer(new TransientQueueEntrySet(workspaceId,
                                                             removedNodes,
                                                             addedNodes,
                                                             parentRemovedNodes,
                                                             parentAddedNodes));
  }

  /**
   * Update Queue, JCR indexes and status file. {@inheritDoc}
   */
  @Override
  public synchronized void init(QueryHandler handler, QueryHandlerEntry config) {
    if (initialized) {
      return;
    }

    LOG.debug("Initialize JCR QUEUE Indexing Service");

    retrieveLocalLastOperationFile();
    if (this.localLastOperationFile == null || !this.localLastOperationFile.exists()) {
      try {
        // Retrieve file from coordinator if it doesn't exist locally
        // This means that the index folder is empty and we should copy indexes
        // from coordinator AND update Queue status
        String indexDirPath = config.getParameter(QueryHandlerParams.PARAM_INDEX_DIR).getValue();
        retrieveLastOperationIDFromCoordinator(handler, indexDirPath);
      } catch (Exception e) {
        LOG.warn("Unable to retrieve lastOperation file from coordinator", e);
      }
    }

    // After retrieving index from coordinator, or just starting up, force
    // update index status of indexes synchronously
    if (this.localLastOperationFile != null && this.localLastOperationFile.exists()) {
      // Update Queue with last status if the indexes was manually copied from
      // coordinator
      updateQueueSwitchStatusInFile();
    }
    initialized = true;
  }

  /**
   * Apply the changes coming from queue to JCR. The changes are identitifed by
   * JCR UUID and Operation type.
   * 
   * @param indexingQueueEntities {@link List} of {@link JCRIndexQueueEntity} to
   *          index
   * @throws Exception when an error happens while indexing the operation
   */
  @ExoTransactional
  public void applyIndexChangesOnJCR(List<JCRIndexQueueEntity> indexingQueueEntities) throws Exception { // NOSONAR
    if (indexingQueueEntities == null || indexingQueueEntities.isEmpty()) {
      return;
    }

    Map<String, List<JCRIndexQueueEntity>> opByWorkspace = convertEntitiesToMapByWorkspace(indexingQueueEntities);
    Map<String, Map<Boolean, Map<JCRIndexingOperationType, Set<String>>>> opByWorkspaceByType =
                                                                                              convertEntitiesToMapByWorkspaceByType(indexingQueueEntities);
    for (Map.Entry<String, Map<Boolean, Map<JCRIndexingOperationType, Set<String>>>> operationsByTypeEntry : opByWorkspaceByType.entrySet()) {
      String workspaceId = operationsByTypeEntry.getKey();
      SearchManager searchManager = getSearchManager(workspaceId);
      Map<Boolean, Map<JCRIndexingOperationType, Set<String>>> operationsByType = operationsByTypeEntry.getValue();

      Map<JCRIndexingOperationType, Set<String>> parentOpsByType = operationsByType.containsKey(true) ? operationsByType.get(true)
                                                                                                      : Collections.emptyMap();
      Map<JCRIndexingOperationType, Set<String>> opsByType = operationsByType.containsKey(false) ? operationsByType.get(false)
                                                                                                 : Collections.emptyMap();
      Set<String> parentAddedNodes = getJCRUUIDs(parentOpsByType, JCRIndexingOperationType.CREATE);
      Set<String> parentRemovedNodes = getJCRUUIDs(parentOpsByType, JCRIndexingOperationType.DELETE);

      Set<String> addedNodes = getJCRUUIDs(opsByType, JCRIndexingOperationType.CREATE);
      Set<String> removedNodes = getJCRUUIDs(opsByType, JCRIndexingOperationType.DELETE);
      if (LOG.isDebugEnabled()) {
        LOG.debug("APPLY index to JCR: '{}' parent create & '{}' parent remove & '{}' create & '{}' remove",
                  parentAddedNodes.size(),
                  parentRemovedNodes.size(),
                  addedNodes.size(),
                  removedNodes.size());
        for (String parentRemovedNode : parentRemovedNodes) {
          LOG.debug("-  APPLY index to JCR: parent delete UUID = {}", parentRemovedNode);
        }
        for (String removedNode : removedNodes) {
          LOG.debug("-  APPLY index to JCR: delete UUID = {}", removedNode);
        }
        for (String parentAddedNode : parentAddedNodes) {
          LOG.debug("+  APPLY index to JCR: parent create UUID = {}", parentAddedNode);
        }
        for (String addedNode : addedNodes) {
          LOG.debug("+  APPLY index to JCR: create UUID = {}", addedNode);
        }
      }

      ChangesHolder parentChanges = searchManager.getChanges(parentRemovedNodes, parentAddedNodes);
      ChangesHolder changes = searchManager.getChanges(removedNodes, addedNodes);

      searchManager.apply(parentChanges);
      searchManager.apply(changes);

      List<JCRIndexQueueEntity> operationsSucceeded = opByWorkspace.get(workspaceId);
      for (JCRIndexQueueEntity jcrIndexQueueEntity : operationsSucceeded) {
        jcrIndexQueueEntity.addNode(clusterNodeName);
        this.updateLastNewOperationId(jcrIndexQueueEntity.getId());
      }
      indexingQueueDAO.updateAll(operationsSucceeded);
    }
  }

  @ExoTransactional
  public List<JCRIndexQueueEntity> findNextOperations(int offset, int batchNumber) {
    return indexingQueueDAO.findAllOperationNotExecutedByClusterNode(offset, batchNumber, clusterNodeName);
  }

  @ExoTransactional
  public void index(String jcrUUID, String workspaceId, boolean parentChange) {
    if (isIndexingInCurrentThread()) {
      return;
    }
    checkIndexingArguments(jcrUUID, workspaceId);
    JCRIndexQueueEntity indexQueueEntity = addToIndexingQueue(jcrUUID,
                                                              workspaceId,
                                                              JCRIndexingOperationType.CREATE,
                                                              parentChange);
    updateLastNewOperationId(indexQueueEntity.getId());
  }

  @ExoTransactional
  public void reindex(String jcrUUID, String workspaceId, boolean parentChange) {
    if (isIndexingInCurrentThread()) {
      return;
    }
    checkIndexingArguments(jcrUUID, workspaceId);
    JCRIndexQueueEntity indexQueueEntity = addToIndexingQueue(jcrUUID,
                                                              workspaceId,
                                                              JCRIndexingOperationType.UPDATE,
                                                              parentChange);
    updateLastNewOperationId(indexQueueEntity.getId());
  }

  @ExoTransactional
  public void unindex(String jcrUUID, String workspaceId, boolean parentChange) {
    if (isIndexingInCurrentThread()) {
      return;
    }
    checkIndexingArguments(jcrUUID, workspaceId);
    JCRIndexQueueEntity indexQueueEntity = addToIndexingQueue(jcrUUID,
                                                              workspaceId,
                                                              JCRIndexingOperationType.DELETE,
                                                              parentChange);
    updateLastNewOperationId(indexQueueEntity.getId());
  }

  @ExoTransactional
  public void unindexAll(String workspaceId) {
    if (isIndexingInCurrentThread()) {
      return;
    }
    checkIndexingArgument("workspaceId", workspaceId);
    JCRIndexQueueEntity indexQueueEntity = addToIndexingQueue("-", workspaceId, JCRIndexingOperationType.DELETE_ALL, false);
    updateLastNewOperationId(indexQueueEntity.getId());
  }

  @ExoTransactional
  public void updateQueueSwitchStatusInFile() {
    try {
      String lastExecutedOperation = getLastExecutedOperation();
      if (StringUtils.isBlank(lastExecutedOperation)) {
        return;
      }

      String[] lastExecutedIdArray = lastExecutedOperation.split(";");
      String oldClusterNodeName = lastExecutedIdArray[1];
      String lastExecutedIdString = lastExecutedIdArray[0];
      if (!this.clusterNodeName.equals(oldClusterNodeName) && StringUtils.isNotBlank(lastExecutedIdString)) {
        LOG.debug("Start: Update Queue with last status because the indexes was manually copied");

        // If the last executed id was copied with index folder
        // from coordinator, the QUEUE has to be updated to reflect that
        // the current copied indexes are applied on currentClusterNode too
        // And operation id is less than id from file
        int offset = 0;
        int proceededOperations = 0;
        int totalProceededOperations = 0;
        long lastExecutedId = Long.parseLong(lastExecutedIdString);
        Long totalIndexingOperations = indexingQueueDAO.count();
        do {
          List<JCRIndexQueueEntity> indexingQueueEntities =
                                                          indexingQueueDAO.findAllOperationExecutedByClusterNode(oldClusterNodeName,
                                                                                                                 clusterNodeName,
                                                                                                                 lastExecutedId,
                                                                                                                 offset,
                                                                                                                 batchNumber);
          // Mark all operations as executed for node as well
          for (JCRIndexQueueEntity jcrIndexQueueEntity : indexingQueueEntities) {
            jcrIndexQueueEntity.addNode(clusterNodeName);
          }
          indexingQueueDAO.updateAll(indexingQueueEntities);
          proceededOperations = indexingQueueEntities.size();
          totalProceededOperations += proceededOperations;

          LOG.debug("Retrieved indexes update: {} / {}", totalProceededOperations, totalIndexingOperations);
        } while (proceededOperations == batchNumber);

        LOG.debug("End: Update Queue with last status because the indexes was manually copied. Total indexes updates: {} / {}",
                  totalProceededOperations,
                  totalIndexingOperations);

        updateLastNewOperationId(lastExecutedId);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Couldn't read file JCR queue indexing last operation id", e);
    }
  }

  public String getLastExecutedOperation() {
    if (localLastOperationFile == null || !localLastOperationFile.exists()) {
      return null;
    }
    try {
      return IOUtil.getFileContentAsString(localLastOperationFile);
    } catch (IOException e) {
      LOG.error("An error occurred while retrieving last operation id from file", e);
      return null;
    }
  }

  public int getQueueProcessingPeriod() {
    return queueProcessingPeriod;
  }

  public synchronized void persistQueueOperations() {
    computeCurrentContainer();
    while (transientQueueToPersist.peek() != null) {
      TransientQueueEntrySet queueEntrySet = transientQueueToPersist.poll();
      Set<String> addedNodes = queueEntrySet.getAddedNodes();
      Set<String> parentAddedNodes = queueEntrySet.getParentAddedNodes();
      Set<String> removedNodes = queueEntrySet.getRemovedNodes();
      Set<String> parentRemovedNodes = queueEntrySet.getParentRemovedNodes();
      String workspaceId = queueEntrySet.getWorkspace();

      RequestLifeCycle.begin(entityManagerService);
      try {
        if (LOG.isDebugEnabled()) {
          LOG.debug("PUSH to queue index: '{}' parent create & '{}' parent remove & '{}' create & '{}' remove",
                    parentAddedNodes.size(),
                    parentRemovedNodes.size(),
                    addedNodes.size(),
                    removedNodes.size());
          for (String parentRemovedNode : parentRemovedNodes) {
            LOG.debug("-  PUSH to queue index: parent delete UUID = {}", parentRemovedNode);
          }
          for (String removedNode : removedNodes) {
            LOG.debug("-  PUSH to queue index: delete UUID = {}", removedNode);
          }
          for (String parentAddedNode : parentAddedNodes) {
            LOG.debug("+  PUSH to queue index: parent create UUID = {}", parentAddedNode);
          }
          for (String addedNode : addedNodes) {
            LOG.debug("+  PUSH to queue index: create UUID = {}", addedNode);
          }
        }

        for (String removeNodeUUID : parentRemovedNodes) {
          if (!parentAddedNodes.contains(removeNodeUUID)) {
            // 1. First step: This is not an update, the node is deleted, thus
            // delete all
            // previous indexing Operations relative to this UUID
            LOG.debug("Delete all previous indexing operations with JCR UUID '{}' since the node was deleted", removeNodeUUID);
            indexingQueueDAO.deleteOperationsByJCRUUID(removeNodeUUID);
          }
          // 2. Second step: add a new entry in queue to requesting unindexing
          // this node with its parent
          unindex(removeNodeUUID, workspaceId, true);
        }
        for (String removeNodeUUID : removedNodes) {
          if (!addedNodes.contains(removeNodeUUID)) {
            // 1. First step: This is not an update, the node is deleted, thus
            // delete all
            // previous indexing Operations relative to this UUID
            LOG.debug("Delete all previous indexing operations with JCR UUID '{}' since the node was deleted", removeNodeUUID);
            indexingQueueDAO.deleteOperationsByJCRUUID(removeNodeUUID);
          }
          // 2. Second step: add a new entry in queue to requesting unindexing
          // this node without a change on its parent
          unindex(removeNodeUUID, workspaceId, false);
        }

        for (String addedNodeUUID : parentAddedNodes) {
          index(addedNodeUUID, workspaceId, true);
        }
        for (String addedNodeUUID : addedNodes) {
          index(addedNodeUUID, workspaceId, false);
        }
      } catch (Exception e) {
        LOG.error("Error while applying changes", e);
      } finally {
        RequestLifeCycle.end();
      }
    }
  }

  private void retrieveLastOperationIDFromCoordinator(QueryHandler handler, String indexDirPath) throws Exception { // NOSONAR
    IndexRecovery indexRecovery = handler.getContext().getIndexRecovery();

    File indexDirFile = new File(indexDirPath);
    File lastOperationFile = new File(indexDirFile.getParentFile(), LAST_OPERATION_FILE_NAME);
    if (lastOperationFile.exists()) {
      // If last indexing operation already exists, we will not retrieve the
      // index folder of workspace from coordinator. In fact, the file could be
      // already retrieved from coordinator by another workspace initialization
      this.localLastOperationFile = lastOperationFile;
      return;
    }

    if (!handler.getContext().isRecoveryFilterUsed() || handler.getContext().getIndexRecovery() == null) {
      // Can't retrieve from coordinator if RecoveryFilter is not configured
      return;
    }

    RPCService rpcService = handler.getContext().getRPCService();
    if (rpcService == null || rpcService.isCoordinator()) {
      // Can't retrieve from coordinator (master) if the started node is the
      // coordinator (master) itself
      return;
    }

    LOG.debug("Retriving last executed queue operation file from coordinator");
    // Retrieving only the file of last indexed operation
    InputStream indexFile = indexRecovery.getIndexFile("../" + LAST_OPERATION_FILE_NAME);
    try (FileOutputStream outputStream = new FileOutputStream(lastOperationFile)) {
      IOUtils.copy(indexFile, outputStream);
      outputStream.flush();
    }
  }

  private void applyIndexChangesOnJCR() throws Exception {
    int offset = 0;
    int proceededOperations = 0;
    do {
      List<JCRIndexQueueEntity> indexingQueueEntities = findNextOperations(offset, batchNumber);
      proceededOperations = indexingQueueEntities.size();
      applyIndexChangesOnJCR(indexingQueueEntities);
    } while (proceededOperations == batchNumber);
  }

  private void updateLastNewOperationId(final long id) {
    checkStoredLastOperationId();
    // Update it locally and let it be stored async in sheduled job
    this.localLastOperationId.getAndUpdate(value -> id > value ? id : value);
  }

  public void persistLastOperationId() {
    retrieveLocalLastOperationFile();
    if (this.localLastOperationFile == null) {
      LOG.error("JCR Index parent directory wasn't found. Can't save last indexed operation.");
      return;
    }

    checkStoredLastOperationId();
    long id = this.localLastOperationId.get();
    if (storedLastOperationId.get() >= id) {
      // No need to store again an id of index operation that was already
      // executed
      return;
    }

    LOG.debug("Persist last index operation id: {}", id);
    try (FileOutputStream outputStream = new FileOutputStream(this.localLastOperationFile)) {

      String fileContent = id + ";" + clusterNodeName;
      outputStream.write(fileContent.getBytes());
      outputStream.flush();

      this.storedLastOperationId.set(id);
    } catch (Exception e) {
      LOG.error("Error while writing last operation ID ", e);
    }
  }

  private void checkStoredLastOperationId() {
    if (this.localLastOperationId == null) {
      String lastExecutedOperation = getLastExecutedOperation();
      if (StringUtils.isNotBlank(lastExecutedOperation)) {
        String[] lastExecutedIdArray = lastExecutedOperation.split(";");
        String lastExecutedIdString = lastExecutedIdArray[0];
        this.localLastOperationId = new AtomicLong(Long.parseLong(lastExecutedIdString));
      } else {
        this.localLastOperationId = new AtomicLong(0);
      }
      this.storedLastOperationId.set(this.localLastOperationId.get());
    }
  }

  private JCRIndexQueueEntity addToIndexingQueue(String jcrUUID,
                                                 String workspaceId,
                                                 JCRIndexingOperationType operationType,
                                                 boolean parentChange) {
    if (operationType == null) {
      throw new IllegalArgumentException("Operation cannot be null");
    }
    JCRIndexQueueEntity indexingEntity = getIndexingEntity(jcrUUID, workspaceId, operationType, parentChange);
    indexingEntity = indexingQueueDAO.create(indexingEntity);
    return indexingEntity;
  }

  private boolean isIndexingInCurrentThread() {
    return this.isIndexingLocally.get() != null && this.isIndexingLocally.get();
  }

  private JCRIndexQueueEntity getIndexingEntity(String jcrUUID,
                                                String workspaceId,
                                                JCRIndexingOperationType operationType,
                                                boolean parentChange) {
    return new JCRIndexQueueEntity(jcrUUID, workspaceId, operationType, Calendar.getInstance(), parentChange, clusterNodeName);
  }

  private void retrieveLocalLastOperationFile() {
    if (this.localLastOperationFile != null) {
      return;
    }
    String workspaceIndexDirName = getSystemWorkspaceIndexDirectory();
    if (StringUtils.isBlank(workspaceIndexDirName)) {
      return;
    }
    File workspaceIndexFolder = new File(workspaceIndexDirName);
    File rootIndexFolder = workspaceIndexFolder.getParentFile();
    if (!rootIndexFolder.exists()) {
      rootIndexFolder.mkdirs();
    }
    this.localLastOperationFile = new File(rootIndexFolder, LAST_OPERATION_FILE_NAME);
  }

  private String getSystemWorkspaceIndexDirectory() {
    String indexDirName = null;
    try {
      ManageableRepository repository = repositoryService.getCurrentRepository();
      String systemWorkspaceName = repository.getConfiguration().getSystemWorkspaceName();
      WorkspaceContainerFacade workspaceContainer = repository.getWorkspaceContainer(systemWorkspaceName);
      WorkspaceEntry workspaceEntry = (WorkspaceEntry) workspaceContainer.getComponent(WorkspaceEntry.class);
      indexDirName = workspaceEntry.getQueryHandler().getParameterValue(QueryHandlerParams.PARAM_INDEX_DIR);
    } catch (Exception e) {
      LOG.error("Error while getting system workspace configuration");
    }
    return indexDirName;
  }

  private void checkIndexingArguments(String jcrUUID, String workspaceId) {
    checkIndexingArgument("workspaceId", workspaceId);
    checkIndexingArgument("jcrUUID", jcrUUID);
  }

  private void checkIndexingArgument(String name, String value) {
    if (StringUtils.isBlank(value)) {
      throw new IllegalArgumentException(name + " is null");
    }
  }

  private SearchManager getSearchManager(String workspaceId) throws RepositoryException {
    if (StringUtils.isBlank(workspaceId)) {
      throw new IllegalArgumentException("Workspace id is null");
    }
    if (!workspaceId.contains(getRepositoryName())) {
      throw new IllegalArgumentException("Workspace id '" + workspaceId + "' doesn't match pattern '" + getRepositoryName()
          + "_WORKSPACENAME' ");
    }
    String workspaceName = workspaceId.replace(getRepositoryName() + "_", "");

    WorkspaceContainerFacade workspaceContainer = repositoryService.getCurrentRepository().getWorkspaceContainer(workspaceName);
    if (workspaceContainer == null) {
      throw new IllegalStateException("Can't find workspace container with name " + workspaceName);
    }
    return (SearchManager) workspaceContainer.getComponent(SearchManager.class);
  }

  private String getRepositoryName() {
    if (this.repositoryName == null) {
      try {
        this.repositoryName = repositoryService.getConfig().getDefaultRepositoryName();
      } catch (Exception e) {
        throw new IllegalStateException("Can't get current repository name from repository service", e);
      }
    }
    return repositoryName;
  }

  private Set<String> getJCRUUIDs(Map<JCRIndexingOperationType, Set<String>> operationsByType,
                                  JCRIndexingOperationType operationType) {
    Set<String> nodesUUIDSet = operationsByType.containsKey(operationType) ? operationsByType.get(operationType) : null;
    if (nodesUUIDSet == null) {
      nodesUUIDSet = Collections.emptySet();
    }
    return nodesUUIDSet;
  }

  private Map<String, Map<Boolean, Map<JCRIndexingOperationType, Set<String>>>> convertEntitiesToMapByWorkspaceByType(List<JCRIndexQueueEntity> indexingQueueEntities) {
    return indexingQueueEntities.stream()
                                .collect(Collectors.groupingBy(JCRIndexQueueEntity::getWorkspace,
                                                               Collectors.groupingBy(JCRIndexQueueEntity::isParentChange,
                                                                                     Collectors.groupingBy(JCRIndexQueueEntity::getOperationType,
                                                                                                           Collectors.mapping(JCRIndexQueueEntity::getJcrUUID,
                                                                                                                              Collectors.toSet())))));
  }

  private Map<String, List<JCRIndexQueueEntity>> convertEntitiesToMapByWorkspace(List<JCRIndexQueueEntity> indexingQueueEntities) {
    return indexingQueueEntities.stream()
                                .collect(Collectors.groupingBy(JCRIndexQueueEntity::getWorkspace,
                                                               Collectors.toList()));
  }

  private void computeCurrentContainer() {
    if (container == null) {
      container = ExoContainerContext.getCurrentContainerIfPresent();
    }

    if (container instanceof RootContainer) {
      container = PortalContainer.getInstance();
    }
    ExoContainerContext.setCurrentContainer(container);
  }

  private void scheduleIndexTasks() {
    retrieveLocalLastOperationFile();
    checkStoredLastOperationId();

    // Job to index operations retrieved from queue and not indexed locally
    queueConsumingJobService.scheduleAtFixedRate(() -> {
      if (indexingOperationIsRunning.get()) {
        LOG.debug("Previous QUEUE indexing processing task is always executing, skip this execution");
        return;
      }
      indexingOperationIsRunning.set(true);
      computeCurrentContainer();
      RequestLifeCycle.begin(entityManagerService);
      try {
        LOG.debug("Running JCR Index from DB QUEUE Task");
        processIndexingQueue();
      } catch (Exception e) {
        LOG.error("Error while Running JCR nodes indexing operation", e);
      } finally {
        indexingOperationIsRunning.set(false);
        RequestLifeCycle.end();
      }
    }, 0, queueProcessingPeriod, TimeUnit.SECONDS);

    // Persist indexing operations made locally
    queueProducingJobService.scheduleAtFixedRate(() -> persistQueueOperations(), 0, queueProcessingPeriod, TimeUnit.SECONDS);

    // Persist last executed operation id when changed
    filePersisterThread.scheduleAtFixedRate(() -> persistLastOperationId(), 0, 10, TimeUnit.SECONDS);
  }

}
