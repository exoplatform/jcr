/*
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.services.jcr.api.version;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

/**
 * Created by The eXo Platform SAS 07.05.2006
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter
 *         Nedonosko</a>
 * @version $Id: TestVersionHistory.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class TestVersionHistory extends BaseVersionTest
{

   private Node testRoot = null;

   private Node testVersionable = null;

   public void setUp() throws Exception
   {
      super.setUp();

      testRoot = root.addNode("testRoot");

      testVersionable = testRoot.addNode("testVersionable", "nt:unstructured");
      testVersionable.addMixin("mix:versionable");
      root.save();
   }

   protected void tearDown() throws Exception
   {
      try
      {
         testVersionable.checkout();
         testRoot.remove();
         root.save();
      }
      catch (RepositoryException e)
      {
         log.error("tear down error: " + e, e);
      }

      super.tearDown();
   }

   /**
    * Scenario (script): Creating nodes: n1, n2 making: ver.1 Creating nodes:
    * n1[2], n1[3], n1[4], n3 making: ver.2 Creating node: n4 making: ver.3
    * Creating node: n5 making: ver.4 Removing nodes: n2, n4 Restoring ver.1:
    * (n1, n2) Creating node: n6 making: ver.1.1 Removing nodes: n1, n6 Restoring
    * ver.3: (n1, n1[2], n1[3], n1[4], n2, n3, n4) Creating node: n2[2] making:
    * ver.3.1 making: ver.3.1.1 Restoring ver.2 (n1, n1[2], n1[3], n1[4], n2, n3)
    * Creating node: n2[2] again!!! making: ver.2.1
    * ------------------------------------------------- [PN] 08.05.06 Play in
    * same play as above. There are some problems in CMS Web UI ('Manage Version'
    * menu) if we will look on the version history of the /testVersionable node.
    * After ver.3 was restored we create n2 (n2[2]) and make a new version
    * ver.3.1. Then we go to 'Manage Version' menu item and will not see the base
    * version (ver.3.1, name is '6'). Same case history if we restore ver.1.1
    * (named '5'), then add a node in the /testVersionable and make checkin.
    */
   public void testVersionHistoryTree() throws Exception
   {

      // testVersionable = versionableNode; // it's nt:folder

      VersionHistory vHistory = testVersionable.getVersionHistory();

      // Creating nodes: n1, n2
      Node n1 = testVersionable.addNode("n1");
      Node n2 = testVersionable.addNode("n2");
      testVersionable.save();
      Version ver1 = testVersionable.checkin(); // v1
      vHistory.addVersionLabel(ver1.getName(), "ver.1", false);
      testVersionable.checkout();

      // sameNameSibs nodes
      // Creating nodes: n1[2], n1[3], n1[4]
      Node snsN1_2 = testVersionable.addNode("n1");
      Node snsN1_3 = testVersionable.addNode("n1");
      Node snsN1_4 = testVersionable.addNode("n1");

      // Creating node: n3
      Node n3 = testVersionable.addNode("n3");
      testVersionable.save();
      Version ver2 = testVersionable.checkin(); // v2
      vHistory.addVersionLabel(ver2.getName(), "ver.2", false);
      testVersionable.checkout();

      // Creating node: n4
      Node n4 = testVersionable.addNode("n4");
      testVersionable.save();
      Version ver3 = testVersionable.checkin(); // v3
      vHistory.addVersionLabel(ver3.getName(), "ver.3", false);
      vHistory.addVersionLabel(ver3.getName(), "version 3.0", false);
      testVersionable.checkout();

      // Creating node: n5
      Node n5 = testVersionable.addNode("n5");
      testVersionable.save();
      Version ver4 = testVersionable.checkin(); // v4
      vHistory.addVersionLabel(ver4.getName(), "ver.4", false);
      testVersionable.checkout();

      if (log.isDebugEnabled())
         log.debug("===== init =====");
      checkItemsExisted(new String[]{n1.getPath(), snsN1_2.getPath(), snsN1_3.getPath(), snsN1_4.getPath(),
         n2.getPath(), n3.getPath(), n4.getPath(), n5.getPath()}, null);
      checkVersionHistory(testVersionable, 4);

      // removing nodes: n2, n4
      n2.remove();
      n4.remove();
      testVersionable.save();
      if (log.isDebugEnabled())
         log.debug("===== ver.1 before restore =====");
      checkItemsExisted(new String[]{n1.getPath(), snsN1_2.getPath(), snsN1_3.getPath(), snsN1_4.getPath(),
         n3.getPath(), n5.getPath()}, new String[]{n2.getPath(), n4.getPath()});
      checkVersionHistory(testVersionable, 4);

      // RESTORE ver.1 and n1, n2 will be restored
      testVersionable.restore(ver1, true);
      if (log.isDebugEnabled())
         log.debug("===== ver.1 after restore =====");
      checkItemsExisted(new String[]{n1.getPath(), n2.getPath()}, new String[]{snsN1_2.getPath(), snsN1_3.getPath(),
         snsN1_4.getPath(), n3.getPath(), n4.getPath(), n5.getPath()});
      checkVersionHistory(testVersionable, 4);

      testVersionable.checkout();
      // adding nodes: n6
      Node n6 = testVersionable.addNode("n6");
      testVersionable.save();
      Version ver11 = testVersionable.checkin(); // v1.1
      vHistory.addVersionLabel(ver11.getName(), "ver.1.1", false);

      if (log.isDebugEnabled())
         log.debug("===== ver.1.1 =====");
      checkItemsExisted(new String[]{n1.getPath(), n2.getPath(), n6.getPath()}, new String[]{snsN1_2.getPath(),
         snsN1_3.getPath(), snsN1_4.getPath(), n3.getPath(), n4.getPath(), n5.getPath()});
      checkVersionHistory(testVersionable, 5); // has five versions in history

      testVersionable.checkout();
      // removing nodes: n1, n6
      n1.remove();
      n6.remove();
      testVersionable.save();
      if (log.isDebugEnabled())
         log.debug("===== ver.3 before restore =====");
      checkItemsExisted(new String[]{n2.getPath()}, new String[]{n1.getPath(), snsN1_2.getPath(), snsN1_3.getPath(),
         snsN1_4.getPath(), n2.getPath(), n3.getPath(), n4.getPath(), n5.getPath(), n6.getPath()});
      checkVersionHistory(testVersionable, 5);

      // RESTORE ver.3 and n1, n1_2, n1_3, n1_4, n2, n3, n4 will be restored
      testVersionable.restore(ver3, true);
      if (log.isDebugEnabled())
         log.debug("===== ver.3 after restore =====");
      checkItemsExisted(new String[]{n1.getPath(), snsN1_2.getPath(), snsN1_3.getPath(), snsN1_4.getPath(),
         n2.getPath(), n3.getPath(), n4.getPath()}, new String[]{n5.getPath(), n6.getPath()});
      checkVersionHistory(testVersionable, 5);

      testVersionable.checkout();
      // adding node: n2[2]
      Node snsN2_2 = testVersionable.addNode("n2");
      testVersionable.save();
      Version ver31 = testVersionable.checkin(); // v3.1
      vHistory.addVersionLabel(ver31.getName(), "ver.3.1", false);

      if (log.isDebugEnabled())
         log.debug("===== ver.3.1 =====");
      checkItemsExisted(new String[]{n1.getPath(), snsN1_2.getPath(), snsN1_3.getPath(), snsN1_4.getPath(),
         n2.getPath(), snsN2_2.getPath(), n3.getPath(), n4.getPath()}, new String[]{n5.getPath(), n6.getPath()});
      checkVersionHistory(testVersionable, 6);

      testVersionable.checkout();
      Version ver311 = testVersionable.checkin(); // v3.1.1
      vHistory.addVersionLabel(ver311.getName(), "ver.3.1.1", false);

      if (log.isDebugEnabled())
         log.debug("===== ver.2 before restore =====");
      checkItemsExisted(new String[]{n1.getPath(), snsN1_2.getPath(), snsN1_3.getPath(), snsN1_4.getPath(),
         n2.getPath(), snsN2_2.getPath(), n3.getPath(), n4.getPath()}, new String[]{n5.getPath(), n6.getPath()});
      checkVersionHistory(testVersionable, 7);

      // RESTORE ver.2 and n1, n1_2, n1_3, n1_4, n2, n3 will be restored
      testVersionable.restore(ver2, true);
      if (log.isDebugEnabled())
         log.debug("===== ver.2 after restore =====");
      // the node snsN2_2 points to a node with index 1 (result of reindex), i.e.
      // n2 (n2[1])
      checkItemsExisted(new String[]{n1.getPath(), snsN1_2.getPath(), snsN1_3.getPath(), snsN1_4.getPath(),
         n2.getPath(), n3.getPath()}, new String[]{testVersionable.getPath() + "/n2[2]", n4.getPath(), n5.getPath(),
         n6.getPath()});
      checkVersionHistory(testVersionable, 7);

      if (log.isDebugEnabled())
         log.debug("===== ver.2.1 =====");
      testVersionable.checkout();
      // adding node: n2[2] again
      Node snsN2_2_Other = testVersionable.addNode("n2"); // testVersionable.
      // getNodes()
      snsN1_3.refresh(true);
      snsN1_3.remove();
      // testVersionable.getNode(snsN1_3.getName() + "[3]").remove();
      testVersionable.save(); // reindex: n1[4] -> n1[3]
      Version ver21 = testVersionable.checkin(); // v2.1
      vHistory.addVersionLabel(ver21.getName(), "ver.2.1", false);

      // The node snsN2_2 has no actual node in the repository, it's phantom node
      // with old state.
      // But node with same path already created (snsN2_2_Other)!
      //
      // The node snsN1_3 has no actual node in the repository, it's phantom node
      // with old state.
      // But reindex done: n1[4] -> n1[3] and n1[3] already exists
      //
      checkItemsExisted(new String[]{n1.getPath(), snsN1_2.getPath(), snsN1_3.getPath(), n2.getPath(),
         snsN2_2_Other.getPath(), n3.getPath()}, new String[]{n4.getPath(), n5.getPath(), n6.getPath()});
      checkVersionHistory(testVersionable, 8);
   }

   private VersionHistory prepareHistory() throws Exception
   {
      VersionHistory vHistory = testVersionable.getVersionHistory();

      if (log.isDebugEnabled())
         log.debug("===== prepare =====");
      showVersionable(testVersionable);

      // Creating nodes: n1, n2
      Node n1 = testVersionable.addNode("n1");
      Node n2 = testVersionable.addNode("n2");
      testVersionable.save();
      Version ver1 = testVersionable.checkin(); // v1
      vHistory.addVersionLabel(ver1.getName(), "ver.1", false);
      if (log.isDebugEnabled())
         log.debug("===== ver.1 checkin =====");
      showVersionable(testVersionable);
      testVersionable.checkout();
      if (log.isDebugEnabled())
         log.debug("===== ver.1 checkout =====");
      showVersionable(testVersionable);

      // sameNameSibs nodes
      // Creating nodes: n1[2], n1[3], n1[4]
      Node snsN1_2 = testVersionable.addNode("n1");
      Node snsN1_3 = testVersionable.addNode("n1");
      Node snsN1_4 = testVersionable.addNode("n1");

      // Creating node: n3
      Node n3 = testVersionable.addNode("n3");
      testVersionable.save();
      Version ver2 = testVersionable.checkin(); // v2
      vHistory.addVersionLabel(ver2.getName(), "ver.2", false);
      if (log.isDebugEnabled())
         log.debug("===== ver.2 checkin =====");
      showVersionable(testVersionable);
      testVersionable.checkout();
      if (log.isDebugEnabled())
         log.debug("===== ver.2 checkout =====");
      showVersionable(testVersionable);

      // Creating node: n4
      Node n4 = testVersionable.addNode("n4");
      testVersionable.save();
      Version ver3 = testVersionable.checkin(); // v3
      vHistory.addVersionLabel(ver3.getName(), "ver.3", false);
      vHistory.addVersionLabel(ver3.getName(), "version 3.0", false);
      if (log.isDebugEnabled())
         log.debug("===== ver.3 checkin =====");
      showVersionable(testVersionable);
      testVersionable.checkout();
      if (log.isDebugEnabled())
         log.debug("===== ver.3 checkout =====");
      showVersionable(testVersionable);

      // Creating node: n5
      Node n5 = testVersionable.addNode("n5");
      testVersionable.save();
      Version ver4 = testVersionable.checkin(); // v4
      vHistory.addVersionLabel(ver4.getName(), "ver.4", false);
      if (log.isDebugEnabled())
         log.debug("===== ver.4 checkin =====");
      showVersionable(testVersionable);
      testVersionable.checkout();
      if (log.isDebugEnabled())
         log.debug("===== ver.4 checkout =====");
      showVersionable(testVersionable);

      return vHistory;
   }

   public void testVersionRemove() throws Exception
   {

      // -------- prepare the case --------

      VersionHistory vHistory = prepareHistory();

      if (log.isDebugEnabled())
         log.debug("===== init =====");
      checkItemsExisted(new String[]{"/testRoot/testVersionable/n1", "/testRoot/testVersionable/n1[2]",
         "/testRoot/testVersionable/n1[3]", "/testRoot/testVersionable/n1[4]", "/testRoot/testVersionable/n2",
         "/testRoot/testVersionable/n3", "/testRoot/testVersionable/n4", "/testRoot/testVersionable/n5"}, null);
      showVersionable(testVersionable);
      checkVersionHistory(testVersionable, 4);

      // -------- play with version remove --------

      // we have v4 as base version, REMOVE v1 from history
      vHistory.removeVersion(vHistory.getVersionByLabel("ver.1").getName());
      if (log.isDebugEnabled())
         log.debug("===== ver.1 removed =====");
      showVersionable(testVersionable);
      checkVersionHistory(testVersionable, 3);
      try
      {
         vHistory.getVersion("1");
         fail("ver.1 must be not existed");
      }
      catch (VersionException e)
      {
         // ok
      }

      // removing nodes: n2, n4
      testVersionable.getNode("n2").remove();
      testVersionable.getNode("n4").remove();
      testVersionable.save();
      if (log.isDebugEnabled())
         log.debug("===== ver.3 before restore =====");
      checkItemsExisted(new String[]{"/testRoot/testVersionable/n1", "/testRoot/testVersionable/n1[2]",
         "/testRoot/testVersionable/n1[3]", "/testRoot/testVersionable/n1[4]", "/testRoot/testVersionable/n3",
         "/testRoot/testVersionable/n5"}, new String[]{"/testRoot/testVersionable/n2", "/testRoot/testVersionable/n4"});
      showVersionable(testVersionable);
      checkVersionHistory(testVersionable, 3);

      // RESTORE ver.3 and n1, n4 will be restored
      testVersionable.restore(vHistory.getVersionByLabel("ver.3"), true);
      if (log.isDebugEnabled())
         log.debug("===== ver.3 after restore =====");
      checkItemsExisted(new String[]{"/testRoot/testVersionable/n1", "/testRoot/testVersionable/n1[2]",
         "/testRoot/testVersionable/n1[3]", "/testRoot/testVersionable/n1[4]", "/testRoot/testVersionable/n3",
         "/testRoot/testVersionable/n4"}, new String[]{"/testRoot/testVersionable/n5"});
      showVersionable(testVersionable);
      checkVersionHistory(testVersionable, 3);

      // testVersionable.checkout();
      // // adding nodes: n6
      // Node n6 = testVersionable.addNode("n6");
      // testVersionable.save();
      // Version ver11 = testVersionable.checkin(); // v1.1
      // vHistory.addVersionLabel(ver11.getName(), "ver.1.1", false);
      //
      // log.info("===== ver.1.1 =====");
      // checkItemsExisted(new String[] {n1.getPath(), n2.getPath(),
      // n6.getPath()},
      // new String[] {snsN1_2.getPath(), snsN1_3.getPath(), snsN1_4.getPath(),
      // n3.getPath(),
      // n4.getPath(), n5.getPath()});
      // checkVersionHistory(testVersionable, 5); // has five versions in history
      //
      // testVersionable.checkout();
      // // removing nodes: n1, n6
      // n1.remove();
      // n6.remove();
      // testVersionable.save();
      // log.info("===== ver.3 before restore =====");
      // checkItemsExisted(new String[] {n2.getPath()},
      // new String[] {n1.getPath(), snsN1_2.getPath(), snsN1_3.getPath(),
      // snsN1_4.getPath(),
      // n2.getPath(), n3.getPath(), n4.getPath(), n5.getPath(), n6.getPath()});
      // checkVersionHistory(testVersionable, 5);
   }

   private void checkVersionableCopy(Node versionable1, Node versionable2) throws RepositoryException
   {
      String v1_VH_UUID = versionable1.getProperty("jcr:versionHistory").getString();
      String v1_BV_UUID = versionable1.getProperty("jcr:baseVersion").getString();

      try
      {

         String v2_VH_UUID = versionable2.getProperty("jcr:versionHistory").getString();
         String v2_BV_UUID = versionable2.getProperty("jcr:baseVersion").getString();

         assertNotSame("Copied node must has a new version history ", v1_VH_UUID, v2_VH_UUID);
         assertNotSame("Copied node must has a new base version ", v1_BV_UUID, v2_BV_UUID);

         // add new version to the source
         versionable1.checkin();
         versionable1.checkout();

         v1_BV_UUID = versionable1.getProperty("jcr:baseVersion").getString();
         v2_BV_UUID = versionable2.getProperty("jcr:baseVersion").getString();
         assertNotSame("Copied node and its source versionable node must has a different version graphs (1) ",
            v1_BV_UUID, v2_BV_UUID);

         // add new version to the another versionable
         versionable2.checkin();
         versionable2.checkout();

         v2_BV_UUID = versionable2.getProperty("jcr:baseVersion").getString();
         assertNotSame("Copied node and its source versionable node must has a different version graphs (2) ",
            v1_BV_UUID, v2_BV_UUID);

      }
      catch (RepositoryException e)
      {
         fail("Versionable node was not copied properly. " + e);
      }
   }

   public void testCopyVersionable() throws Exception
   {

      // Node source = testRoot.addNode("node1");
      session.getWorkspace().copy(testVersionable.getPath(), testRoot.getPath() + "/newVersionable");

      checkVersionableCopy(testVersionable, testRoot.getNode("newVersionable"));
   }

   public void testCopyVersionableInAnotherWorkspace() throws Exception
   {

      Session ws1 = repository.login(credentials, "ws1");

      ws1.getWorkspace().copy("ws", testVersionable.getPath(), "/newVersionable");

      checkVersionableCopy(testVersionable, ws1.getRootNode().getNode("newVersionable"));
   }
}
