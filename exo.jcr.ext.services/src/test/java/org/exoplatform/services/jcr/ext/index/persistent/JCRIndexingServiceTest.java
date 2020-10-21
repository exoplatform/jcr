package org.exoplatform.services.jcr.ext.index.persistent;

import javax.jcr.Node;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.jcr.ext.BaseStandaloneTest;
import org.exoplatform.services.jcr.ext.index.persistent.impl.JCRIndexingQueueDAOImpl;
import org.exoplatform.services.jcr.ext.index.persistent.impl.JCRIndexingServiceImpl;

public class JCRIndexingServiceTest extends BaseStandaloneTest {

  private JCRIndexingServiceImpl  jcrIndexingService;

  private JCRIndexingQueueDAOImpl indexingQueueDAO;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    indexingQueueDAO = ExoContainerContext.getService(JCRIndexingQueueDAOImpl.class);
    jcrIndexingService = ExoContainerContext.getService(JCRIndexingServiceImpl.class);

    RequestLifeCycle.begin(container);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    RequestLifeCycle.end();
  }

  public void testCreateIndexEntity() throws Exception { // NOSONAR
    saveAndProcessQueue();

    Long initialCount = indexingQueueDAO.count();
    assertNotNull(initialCount);

    Node testRoot = session.getRootNode().addNode("testCreateIndexEntity");
    Node node = testRoot.addNode("exo:test");
    node.setProperty("jcr:prop", "property and node");
    saveAndProcessQueue();

    Long count = indexingQueueDAO.count();
    assertNotNull(count);
    assertEquals(initialCount.longValue() + 2, count.longValue());

    jcrIndexingService.persistLastOperationId();
    String lastExecutedOperation = jcrIndexingService.getLastExecutedOperation();
    assertTrue(lastExecutedOperation.contains(";ClusterNodeName"));
    lastExecutedOperation = lastExecutedOperation.replaceAll(";ClusterNodeName", "");
    assertTrue("Last executed queue operation '" + lastExecutedOperation
        + "' has to be greater or equals the number of elements in queue" + count,
               Long.parseLong(lastExecutedOperation) >= count);
  }

  public void testUpdateIndexEntity() throws Exception { // NOSONAR
    Node testRoot = session.getRootNode().addNode("testUpdateIndexEntity");
    Node node = testRoot.addNode("exo:test");
    node.setProperty("jcr:prop", "property and node");
    saveAndProcessQueue();

    Long initialCount = getQueueCount();
    assertNotNull(initialCount);

    node.setProperty("jcr:prop", "property and node 2");
    saveAndProcessQueue();

    Long count = indexingQueueDAO.count();
    assertNotNull(count);
    assertEquals(initialCount.longValue() + 2, count.longValue());

    jcrIndexingService.persistLastOperationId();
    String lastExecutedOperation = jcrIndexingService.getLastExecutedOperation();
    assertTrue(lastExecutedOperation.contains(";ClusterNodeName"));
    lastExecutedOperation = lastExecutedOperation.replaceAll(";ClusterNodeName", "");
    assertTrue(Long.parseLong(lastExecutedOperation) >= count);
  }

  public void testRemoveIndexEntity() throws Exception { // NOSONAR
    Node testRoot = session.getRootNode().addNode("testRemoveIndexEntity");
    Node node = testRoot.addNode("exo:test");
    node.setProperty("jcr:prop", "property and node");
    saveAndProcessQueue();

    Long initialCount = getQueueCount();
    assertNotNull(initialCount);

    node.remove();
    saveAndProcessQueue();

    Long count = getQueueCount();
    assertNotNull(count);
    // "1" (One) indexation operations representing the 'NODE_ADD' operation
    // will be removed from queue and then "1" (One) new unindex operation will
    // be added requesting to unindex the node
    assertEquals(initialCount.longValue() - 1 + 1, count.longValue());

    jcrIndexingService.persistLastOperationId();
    String lastExecutedOperation = jcrIndexingService.getLastExecutedOperation();
    assertTrue(lastExecutedOperation.contains(";ClusterNodeName"));
    lastExecutedOperation = lastExecutedOperation.replaceAll(";ClusterNodeName", "");
    assertTrue(Long.parseLong(lastExecutedOperation) >= count);
  }

  private long getQueueCount() {
    return indexingQueueDAO.count();
  }

  private void saveAndProcessQueue() throws Exception { // NOSONAR
    session.save();
    jcrIndexingService.persistQueueOperations();
  }

}
