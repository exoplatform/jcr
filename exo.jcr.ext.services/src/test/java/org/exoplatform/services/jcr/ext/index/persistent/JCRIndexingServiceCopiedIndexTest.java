/*
 * Copyright (C) 2026 eXo Platform SAS.
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
package org.exoplatform.services.jcr.ext.index.persistent;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.jcr.ext.BaseStandaloneTest;
import org.exoplatform.services.jcr.ext.index.persistent.api.JCRIndexingQueueDAO;
import org.exoplatform.services.jcr.ext.index.persistent.entity.JCRIndexQueueEntity;
import org.exoplatform.services.jcr.ext.index.persistent.impl.JCRIndexingQueueDAOImpl;
import org.exoplatform.services.jcr.ext.index.persistent.impl.JCRIndexingServiceImpl;

/**
 * A node whose workspace index was copied from another cluster node at startup
 * holds what the copy holds, whatever the queue marks of its own index said:
 * the queue then gives it back every operation the copy lacks.
 */
public class JCRIndexingServiceCopiedIndexTest extends BaseStandaloneTest {

  private static final String     THIS_NODE   = "ClusterNodeName";

  private static final String     COORDINATOR = "coordinator";

  private JCRIndexingServiceImpl  jcrIndexingService;

  private JCRIndexingQueueDAOImpl indexingQueueDAO;

  private String                  copiedWorkspace;

  private String                  keptWorkspace;

  private File                    indexDirectory;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    indexingQueueDAO = ExoContainerContext.getService(JCRIndexingQueueDAOImpl.class);
    jcrIndexingService = ExoContainerContext.getService(JCRIndexingServiceImpl.class);
    // The queue keeps 30 characters of a workspace name
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    copiedWorkspace = "copied-" + suffix;
    keptWorkspace = "kept-" + suffix;
    indexDirectory = Files.createTempDirectory("copied-index").toFile();
    RequestLifeCycle.begin(container);
    // The marker's id depends on every operation of the queue
    indexingQueueDAO.deleteAll();
  }

  @Override
  protected void tearDown() throws Exception {
    RequestLifeCycle.end();
    Files.deleteIfExists(pendingMarkerFile().toPath());
    for (File file : indexDirectory.listFiles()) {
      Files.delete(file.toPath());
    }
    Files.delete(indexDirectory.toPath());
    super.tearDown();
  }

  public void testACopiedIndexGetsBackTheOperationsTheCopyLacks() throws Exception {
    // Queued by this node, which indexed it: the coordinator never applied it
    JCRIndexQueueEntity ownChange = operation(copiedWorkspace, THIS_NODE);
    // Queued by the coordinator, applied by both
    JCRIndexQueueEntity appliedByBoth = operation(copiedWorkspace, COORDINATOR, THIS_NODE);
    // Queued by the coordinator after the id its index carries, applied by this node
    JCRIndexQueueEntity afterTheCopy = operation(copiedWorkspace, COORDINATOR, THIS_NODE);
    // Another workspace, whose index this node kept
    JCRIndexQueueEntity keptIndex = operation(keptWorkspace, COORDINATOR, THIS_NODE);
    // The copy brought the coordinator's marker: the last operation its index holds
    copiedMarker(appliedByBoth.getId(), COORDINATOR);

    jcrIndexingService.registerWorkspaceIndex(copiedWorkspace, indexDirectory.getAbsolutePath(), () -> false);

    assertEquals(Set.of(ownChange.getId(), afterTheCopy.getId()), notAppliedByThisNode(copiedWorkspace));
    assertTrue(notAppliedByThisNode(keptWorkspace).isEmpty());
    assertTrue(indexingQueueDAO.find(keptIndex.getId()).getNodes().contains(THIS_NODE));
    // From now on, the index is this node's: its marker says so
    assertEquals(THIS_NODE, Files.readString(markerFile().toPath(), StandardCharsets.UTF_8).split(";")[1]);
  }

  public void testACopiedIndexGetsBackTheParentChangesTheCopyLacks() throws Exception {
    JCRIndexQueueEntity ownParentChange = operation(copiedWorkspace, true, THIS_NODE);
    JCRIndexQueueEntity parentChangeAppliedByBoth = operation(copiedWorkspace, true, COORDINATOR, THIS_NODE);
    copiedMarker(parentChangeAppliedByBoth.getId(), COORDINATOR);

    jcrIndexingService.registerWorkspaceIndex(copiedWorkspace, indexDirectory.getAbsolutePath(), () -> false);

    assertEquals(Set.of(ownParentChange.getId()), notAppliedByThisNode(copiedWorkspace));
  }

  public void testACopiedIndexGetsBackTheOperationsQueuedShortlyBeforeItsMarker() throws Exception {
    // Applied by the coordinator, maybe after its copy: its transaction can have
    // committed after the coordinator read its marker's id
    JCRIndexQueueEntity queuedJustBefore = operation(copiedWorkspace, COORDINATOR, THIS_NODE);
    marker(queuedJustBefore.getId() + ";" + COORDINATOR + ";" + System.currentTimeMillis());

    jcrIndexingService.registerWorkspaceIndex(copiedWorkspace, indexDirectory.getAbsolutePath(), () -> false);

    assertEquals(Set.of(queuedJustBefore.getId()), notAppliedByThisNode(copiedWorkspace));
  }

  public void testACopiedIndexTakesItsMarksBatchByBatch() throws Exception {
    JCRIndexingServiceImpl service = new JCRIndexingServiceImpl(null,
                                                                ExoContainerContext.getService(EntityManagerService.class),
                                                                ExoContainerContext.getService(RepositoryService.class),
                                                                indexingQueueDAO,
                                                                thisNodeParams("2"));
    Set<Long> ownChanges = new HashSet<>();
    long lastAppliedByBoth = 0;
    for (int i = 0; i < 5; i++) {
      ownChanges.add(operation(copiedWorkspace, THIS_NODE).getId());
      lastAppliedByBoth = operation(copiedWorkspace, COORDINATOR, THIS_NODE).getId();
    }
    copiedMarker(lastAppliedByBoth, COORDINATOR);

    service.registerWorkspaceIndex(copiedWorkspace, indexDirectory.getAbsolutePath(), () -> false);

    assertEquals(ownChanges, notAppliedByThisNode(copiedWorkspace));
  }

  public void testAnIndexWhoseMarksCannotBeTakenKeepsTheCopiedMarker() throws Exception {
    JCRIndexingQueueDAO failingDAO = mock(JCRIndexingQueueDAO.class);
    when(failingDAO.findOperationsOfWorkspaceExecutedByClusterNode(anyString(), anyString(), anyInt()))
                                                                                                      .thenThrow(new IllegalStateException("database down"));
    JCRIndexingServiceImpl service = new JCRIndexingServiceImpl(null,
                                                                mock(EntityManagerService.class),
                                                                mock(RepositoryService.class),
                                                                failingDAO,
                                                                thisNodeParams());
    marker("5;" + COORDINATOR + ";1700000000000");

    service.registerWorkspaceIndex(copiedWorkspace, indexDirectory.getAbsolutePath(), () -> false);

    // The next start takes the marks from the copied marker again; a copy of
    // the index meanwhile gets an unreadable marker, which gives everything back
    assertEquals("5;" + COORDINATOR + ";1700000000000", Files.readString(pendingMarkerFile().toPath(), StandardCharsets.UTF_8));
    assertFalse(Files.readString(markerFile().toPath(), StandardCharsets.UTF_8).contains(COORDINATOR));
  }

  public void testTheNextStartTakesTheMarksThatCouldNotBeTaken() throws Exception {
    JCRIndexQueueEntity ownChange = operation(copiedWorkspace, THIS_NODE);
    JCRIndexQueueEntity appliedByBoth = operation(copiedWorkspace, COORDINATOR, THIS_NODE);
    Files.writeString(pendingMarkerFile().toPath(),
                      appliedByBoth.getId() + ";" + COORDINATOR + ";"
                          + (System.currentTimeMillis() + 2 * JCRIndexingServiceImpl.IN_FLIGHT_OPERATION_MARGIN));
    marker("marks pending on " + THIS_NODE);

    jcrIndexingService.registerWorkspaceIndex(copiedWorkspace, indexDirectory.getAbsolutePath(), () -> false);

    assertEquals(Set.of(ownChange.getId()), notAppliedByThisNode(copiedWorkspace));
    assertFalse(pendingMarkerFile().exists());
    assertEquals(THIS_NODE, Files.readString(markerFile().toPath(), StandardCharsets.UTF_8).split(";")[1]);
  }

  public void testAnIndexCopiedAgainSinceItsMarksWereNotTakenTakesTheMarksOfTheNewCopy() throws Exception {
    JCRIndexQueueEntity ownChange = operation(copiedWorkspace, THIS_NODE);
    JCRIndexQueueEntity appliedByTheOldSource = operation(copiedWorkspace, "old-source", THIS_NODE);
    // Left by a start whose marks failed, then a new copy came from the coordinator
    Files.writeString(pendingMarkerFile().toPath(),
                      appliedByTheOldSource.getId() + ";old-source;"
                          + (System.currentTimeMillis() + 2 * JCRIndexingServiceImpl.IN_FLIGHT_OPERATION_MARGIN));
    copiedMarker(ownChange.getId() - 1, COORDINATOR);

    jcrIndexingService.registerWorkspaceIndex(copiedWorkspace, indexDirectory.getAbsolutePath(), () -> false);

    // The new copy's marker names none of them: both are applied again
    assertEquals(Set.of(ownChange.getId(), appliedByTheOldSource.getId()), notAppliedByThisNode(copiedWorkspace));
    assertFalse(pendingMarkerFile().exists());
  }

  public void testAnIndexCopiedFromANodeWhoseMarksArePendingGetsBackEveryOperation() throws Exception {
    JCRIndexQueueEntity ownChange = operation(copiedWorkspace, THIS_NODE);
    JCRIndexQueueEntity appliedByTheOldSource = operation(copiedWorkspace, "old-source", THIS_NODE);
    // Left by this node's start whose marks failed, then a copy came from a
    // node whose own marks are pending
    Files.writeString(pendingMarkerFile().toPath(),
                      appliedByTheOldSource.getId() + ";old-source;"
                          + (System.currentTimeMillis() + 2 * JCRIndexingServiceImpl.IN_FLIGHT_OPERATION_MARGIN));
    marker("marks pending on " + COORDINATOR);

    jcrIndexingService.registerWorkspaceIndex(copiedWorkspace, indexDirectory.getAbsolutePath(), () -> false);

    // The copy's marker is unreadable: both are applied again
    assertEquals(Set.of(ownChange.getId(), appliedByTheOldSource.getId()), notAppliedByThisNode(copiedWorkspace));
    assertFalse(pendingMarkerFile().exists());
  }

  public void testTheMarkerIsNotWrittenWhileTheIndexIsPreparedForACopy() throws Exception {
    AtomicBoolean preparingACopy = new AtomicBoolean(true);

    jcrIndexingService.registerWorkspaceIndex(copiedWorkspace, indexDirectory.getAbsolutePath(), preparingACopy::get);

    assertFalse(markerFile().exists());
    // The same check as the one the scheduled marker writes go through
    preparingACopy.set(false);
    jcrIndexingService.registerWorkspaceIndex(copiedWorkspace, indexDirectory.getAbsolutePath(), preparingACopy::get);
    assertEquals(THIS_NODE, Files.readString(markerFile().toPath(), StandardCharsets.UTF_8).split(";")[1]);
  }

  public void testAnIndexWithAnUnreadableMarkerGetsBackEveryOperationOfItsWorkspace() throws Exception {
    JCRIndexQueueEntity ownChange = operation(copiedWorkspace, THIS_NODE);
    JCRIndexQueueEntity appliedByBoth = operation(copiedWorkspace, COORDINATOR, THIS_NODE);
    marker("not a marker");

    jcrIndexingService.registerWorkspaceIndex(copiedWorkspace, indexDirectory.getAbsolutePath(), () -> false);

    // Applied again rather than lost
    assertEquals(Set.of(ownChange.getId(), appliedByBoth.getId()), notAppliedByThisNode(copiedWorkspace));
  }

  public void testThisNodesOwnIndexKeepsItsMarks() throws Exception {
    JCRIndexQueueEntity ownChange = operation(copiedWorkspace, THIS_NODE);
    copiedMarker(ownChange.getId(), THIS_NODE);

    jcrIndexingService.registerWorkspaceIndex(copiedWorkspace, indexDirectory.getAbsolutePath(), () -> false);

    assertTrue(notAppliedByThisNode(copiedWorkspace).isEmpty());
  }

  public void testAnIndexWithNoMarkerKeepsItsMarksAndGetsOne() {
    operation(copiedWorkspace, THIS_NODE);

    jcrIndexingService.registerWorkspaceIndex(copiedWorkspace, indexDirectory.getAbsolutePath(), () -> false);

    assertTrue(notAppliedByThisNode(copiedWorkspace).isEmpty());
    assertTrue(markerFile().exists());
  }

  public void testTheMarkerFollowsTheLastOperationIdThisNodeStores() throws Exception {
    jcrIndexingService.registerWorkspaceIndex(copiedWorkspace, indexDirectory.getAbsolutePath(), () -> false);
    // A queue write moves this node's last operation id forward
    jcrIndexingService.index(UUID.randomUUID().toString(), copiedWorkspace, false);
    String lastOperation = notBlankLastOperation();

    jcrIndexingService.persistLastOperationId();

    // A copy of the index taken now carries what the index holds
    assertTrue(Files.readString(markerFile().toPath(), StandardCharsets.UTF_8).startsWith(notBlankLastOperation() + ";"));
    assertFalse(lastOperation.isBlank());
  }

  public void testTheMarkerStopsBeforeTheFirstOperationThisNodeDidNotApply() throws Exception {
    // Queued by the coordinator, not applied here yet
    JCRIndexQueueEntity notApplied = operation(copiedWorkspace, COORDINATOR);
    jcrIndexingService.index(UUID.randomUUID().toString(), copiedWorkspace, false);
    notBlankLastOperation();

    jcrIndexingService.registerWorkspaceIndex(copiedWorkspace, indexDirectory.getAbsolutePath(), () -> false);

    // A copy taken now gets every operation from that one on
    assertTrue(Files.readString(markerFile().toPath(), StandardCharsets.UTF_8)
                    .startsWith((notApplied.getId() - 1) + ";" + THIS_NODE + ";"));
  }

  private String notBlankLastOperation() {
    jcrIndexingService.persistLastOperationId();
    return jcrIndexingService.getLastExecutedOperation();
  }

  private JCRIndexQueueEntity operation(String workspace, String creator, String... appliedBy) {
    return operation(workspace, false, creator, appliedBy);
  }

  private JCRIndexQueueEntity operation(String workspace, boolean parentChange, String creator, String... appliedBy) {
    JCRIndexQueueEntity entity = new JCRIndexQueueEntity(UUID.randomUUID().toString(),
                                                         workspace,
                                                         JCRIndexingOperationType.CREATE,
                                                         Calendar.getInstance(),
                                                         parentChange,
                                                         creator);
    for (String node : appliedBy) {
      entity.addNode(node);
    }
    return indexingQueueDAO.create(entity);
  }

  private Set<Long> notAppliedByThisNode(String workspace) {
    Set<Long> ids = new HashSet<>();
    int offset = 0;
    List<JCRIndexQueueEntity> page;
    do {
      page = indexingQueueDAO.findAllOperationNotExecutedByClusterNode(offset, 100, THIS_NODE);
      ids.addAll(page.stream()
                     .filter(entity -> workspace.equals(entity.getWorkspace()))
                     .map(JCRIndexQueueEntity::getId)
                     .collect(Collectors.toSet()));
      offset += page.size();
    } while (page.size() == 100);
    return ids;
  }

  private static InitParams thisNodeParams() {
    return thisNodeParams(null);
  }

  private static InitParams thisNodeParams(String batchNumber) {
    InitParams params = new InitParams();
    if (batchNumber != null) {
      ValueParam batch = new ValueParam();
      batch.setName("batch.number");
      batch.setValue(batchNumber);
      params.addParameter(batch);
    }
    ValueParam nodeName = new ValueParam();
    nodeName.setName("cluster.node.name");
    nodeName.setValue(THIS_NODE);
    params.addParameter(nodeName);
    return params;
  }

  /**
   * A marker written by the other node well after the operations it names were
   * queued
   */
  private void copiedMarker(long lastOperationId, String clusterNode) throws Exception {
    long writtenLater = System.currentTimeMillis() + 2 * JCRIndexingServiceImpl.IN_FLIGHT_OPERATION_MARGIN;
    marker(lastOperationId + ";" + clusterNode + ";" + writtenLater);
  }

  private File pendingMarkerFile() {
    return new File(indexDirectory.getAbsoluteFile().getParentFile(),
                    indexDirectory.getName() + "-" + JCRIndexingServiceImpl.WORKSPACE_LAST_OPERATION_FILE_NAME);
  }

  private void marker(String content) throws Exception {
    Files.writeString(markerFile().toPath(), content, StandardCharsets.UTF_8);
  }

  private File markerFile() {
    return new File(indexDirectory, JCRIndexingServiceImpl.WORKSPACE_LAST_OPERATION_FILE_NAME);
  }
}
