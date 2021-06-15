package org.exoplatform.services.jcr.ext;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import static org.exoplatform.services.jcr.ext.ActivityTypeUtils.EXO_ACTIVITY_ID;
import static org.exoplatform.services.jcr.ext.ActivityTypeUtils.EXO_ACTIVITY_INFO;

public class TestActivityTypeUtils extends BaseStandaloneTest {

  public void testAttachActivityId() throws RepositoryException {
    Node sampleNode = root.addNode("testNode");
    ActivityTypeUtils.attachActivityId(sampleNode, "5");
    assertTrue(sampleNode.isNodeType(EXO_ACTIVITY_INFO));
    assertEquals(ActivityTypeUtils.getActivityId(sampleNode), "5");
    if(sampleNode.canAddMixin("mix:versionable")) {
      sampleNode.addMixin("mix:versionable");
    }
    sampleNode.checkout();
    root.save();
    ActivityTypeUtils.attachActivityId(sampleNode, "10");
    sampleNode.save();
    assertEquals(ActivityTypeUtils.getActivityId(sampleNode), "10");
    sampleNode.checkin();
    ActivityTypeUtils.attachActivityId(sampleNode, "20");
    assertEquals(ActivityTypeUtils.getActivityId(sampleNode), "20");
  }
  public void testRemoveAttchAtivityId() throws RepositoryException{
    Node sampleNode = root.addNode("testNode");
    ActivityTypeUtils.attachActivityId(sampleNode, "5");
    assertEquals(sampleNode.getProperty(EXO_ACTIVITY_ID).getString(), "5");
    ActivityTypeUtils.removeAttchAtivityId(sampleNode);
    assertEquals(sampleNode.getProperty(EXO_ACTIVITY_ID).getString(), "");
  }
}
