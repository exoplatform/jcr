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
package org.exoplatform.services.jcr.usecases.version;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.version.Version;

import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;

/**
 * Created by The eXo Platform SARL Author : Anh Nguyen ntuananh.vn@gmail.com Dec 24, 2007
 */
public class ErrorsRelateToRestoreVersionTest
   extends BaseUsecasesTest
{

   private boolean runTest = false;

   public void testImportVersionableNodeThenRestore() throws Exception
   {
      // Case 2 in ECM JIRA
      // http://jira.exoplatform.org/browse/ECM-1160

      runTest = false;
      if (!runTest)
         return;

      Node node1 = root.addNode("Node1", "nt:unstructured");
      node1.addMixin("mix:versionable");
      root.save();
      // Create 1 versions
      Version node1ver1 = node1.checkin();
      node1.checkout();
      // export node
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      session.exportDocumentView(node1.getPath(), bos, false, false);

      // create new node
      Node node2 = root.addNode("Node2", "nt:unstructured");
      // import docview of node1 to new node
      ByteArrayInputStream xmlInputStream = new ByteArrayInputStream(bos.toByteArray());
      session.importXML(node2.getPath(), xmlInputStream, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
      session.save();
      // Create 1 version for node2
      Node node1clone = node2.getNode("Node1");
      Version node2ver1 = node1clone.checkin();
      node1clone.checkout();
      // Create 1 version for node1
      Version node1newver = node1.checkin();
      node1.checkout();

      System.out.println("node1 BaseVersionNode Name:" + node1.getBaseVersion().getName());
      // Resore Node1 to version 2
      Version node1ver2 = node1.getVersionHistory().getVersion("2");
      // Error
      node1.restore(node1ver2, true);
      // Remove node1
      node1.remove();
      root.save();
   }

   public void testCase9() throws Exception
   {
      System.out.println("////Case 9"); // Not JCR Bugs
      runTest = true;
      if (!runTest)
         return;

      Node testNode = root.addNode("Test", "nt:unstructured");
      root.save();

      // Create sub node
      Node doc1 = testNode.addNode("Doc1", "nt:unstructured");
      root.save();

      // Create 1 versions for Doc1 Node
      doc1.addMixin("mix:versionable");
      doc1.save();
      root.save();

      // Create 2 version
      Version ver1doc = doc1.checkin();
      doc1.checkout();

      Version ver2doc = doc1.checkin();
      doc1.checkout();
      root.save();

      // Restore ver 1
      doc1.restore(ver1doc, true);

      // Create 1 versions for Test Node
      testNode.addMixin("mix:versionable");
      testNode.save();
      root.save();

      // Create 2 version
      Version ver1test = testNode.checkin();
      testNode.checkout();

      Version ver2test = testNode.checkin();
      testNode.checkout();
      root.save();

      // Restore ver 1
      testNode.restore(ver1test, true);

      // Test rename testNode
      String newName = "_new";
      try
      {
         // Error here
         session.move(testNode.getPath(), testNode.getPath() + newName);
      }
      catch (Exception e)
      {
         e.printStackTrace();
         fail(e.getMessage());
      }
   }

   public void testVersionMovedSubnode() throws Exception
   {
      // Case 5 in ECM JIRA
      // http://jira.exoplatform.org/browse/ECM-1160
      runTest = false;
      if (!runTest)
         return;

      Node testNode = root.addNode("Test", "nt:unstructured");
      root.save();
      // Create 1 versions for Test Node
      testNode.addMixin("mix:versionable");
      testNode.save();
      root.save();

      // Create version 1
      testNode.checkin();
      testNode.checkout();
      root.save();

      // Create sub node
      Node doc2 = testNode.addNode("Doc2", "nt:unstructured");
      Node doc1 = testNode.addNode("Doc1", "nt:unstructured");
      root.save();

      // Create Versions 2,3 for Test Node
      testNode.checkin();
      testNode.checkout();

      Version ver3 = testNode.checkin();
      testNode.checkout();
      root.save();

      // Test rename testNode
      String testName = "_test";
      try
      {
         // OK
         session.move(testNode.getPath(), testNode.getPath() + testName);
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }

      // Move subnodes
      Node tempNode = root.addNode("Temp");
      root.save();

      try
      {
         session.getWorkspace().move(doc1.getPath(), tempNode.getPath() + "new");
         session.getWorkspace().move(doc2.getPath(), tempNode.getPath() + "new");
         session.save();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }

      // Create version 4
      Version ver4 = testNode.checkin();
      testNode.checkout();
      root.save();

      // Restore testNode to version3
      try
      {
         testNode.restore(ver3, true);
         root.save();
         session.save();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }

      // Test rename testNode
      String newName = "_new";
      try
      {
         // Error here
         session.move(testNode.getPath(), testNode.getPath() + newName);
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }

   }

   public void testActiveVersionSubnode() throws Exception
   {
      // Case 10 in ECM JIRA
      // http://jira.exoplatform.org/browse/ECM-1160
      runTest = false;
      if (!runTest)
         return;

      Node testNode = root.addNode("Test", "nt:unstructured");
      root.save();

      // Create sub node
      Node doc1 = testNode.addNode("Doc1", "nt:unstructured");
      root.save();

      // Create 1 versions for Test Node
      testNode.addMixin("mix:versionable");
      testNode.save();
      Version testNodVer1 = testNode.checkin();
      testNode.checkout();
      root.save();

      assertFalse(doc1.isNodeType("mix:versionable"));
      assertTrue(doc1.canAddMixin("mix:versionable"));

      // Create 2 versions for Sub Node
      doc1.addMixin("mix:versionable");
      doc1.save();

      doc1.checkin();
      doc1.checkout();

      doc1.checkin();
      doc1.checkout();
      root.save();

      // Create version 2 for Test Node
      Version testNodVer2 = testNode.checkin();
      testNode.checkout();

      assertTrue(doc1.isNodeType("mix:versionable"));
      assertFalse(doc1.canAddMixin("mix:versionable"));

      // Restore testNode to version1
      testNode.restore(testNodVer1, true);
      root.save();
      session.save();

      doc1 = root.getNode("Test").getNode("Doc1");

      assertFalse(doc1.isNodeType("mix:versionable"));
      assertTrue(doc1.canAddMixin("mix:versionable"));
      // Add mix:versionable for this node
      // doc1.addMixin("mix:versionable");

      // Clean
      testNode.remove();
      root.save();
   }

   public void testImportNodeThenRestore() throws Exception
   {
      // Case 11 in ECM JIRA
      // http://jira.exoplatform.org/browse/ECM-1160

      runTest = false;
      if (!runTest)
         return;

      Node node1 = root.addNode("Node1", "nt:unstructured");
      // Active vesrion to node
      node1.addMixin("mix:versionable");
      root.save();

      // Create some nodes to node1
      node1.addNode("Node1_1", "nt:unstructured");
      node1.addNode("Node1_2", "nt:unstructured");
      node1.save();

      // Create 1 versions
      Version ver1 = node1.checkin();
      node1.checkout();
      node1.save();
      // export node
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      session.exportDocumentView(node1.getPath(), bos, false, false);

      // import docview of node1
      ByteArrayInputStream xmlInputStream = new ByteArrayInputStream(bos.toByteArray());
      session.importXML(node1.getPath(), xmlInputStream, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
      session.save();

      // Create version 2 for node1
      Version ver2 = node1.checkin();
      node1.checkout();
      node1.save();

      assertNotNull(node1.getNode("Node1"));
      assertNotNull(node1.getNode("Node1").getNode("Node1_1"));
      assertNotNull(node1.getNode("Node1").getNode("Node1_2"));

      // Resore Node1 to version 1
      node1.restore(ver1, true);
      // Resore Node1 to version 2
      node1.restore(ver2, true);

      assertNotNull(node1.getNode("Node1"));
      assertNotNull(node1.getNode("Node1").getNode("Node1_1"));
      assertNotNull(node1.getNode("Node1").getNode("Node1_2"));

      // Clean
      node1.remove();
      root.save();

   }
}
