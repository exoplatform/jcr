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
package org.exoplatform.services.jcr.load.perf;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.util.IdGenerator;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;

import javax.jcr.Node;

/**
 * Created by The eXo Platform SAS Author : Vitaliy Obmanjuk vitaliy.obmanjuk@exoplatform.com.ua
 * 20.07.2006
 * 
 * @version $Id: TestPerformance.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class TestPerformance extends JcrAPIBaseTest
{

   private Node testRoot = null;

   private int NODES_COUNT_SHORT_SESSION = 30;

   private int NODES_COUNT_LONG_SESSION = 300;

   private static String TEST_FILE = null;

   public void setUp() throws Exception
   {
      super.setUp();
      session.refresh(false);
      if (!(root.hasNode("testRoot")))
      {
         testRoot = root.addNode("testRoot", "nt:unstructured");
         for (int i = 1; i < NODES_COUNT_LONG_SESSION; i++)
         {
            testRoot.addNode("setUpNode" + i, "nt:base");
         }
         root.save();
         log.info("" + NODES_COUNT_LONG_SESSION + " nodes added");
      }
      else
      {
         testRoot = root.getNode("testRoot");
      }
   }

   public void tearDown() throws Exception
   {
   }

   // defaults:
   // NODES_COUNT_SHORT_SESSION
   // nt:unstructured
   public void testAddNodeOfTypeNtBaseShortSession() throws Exception
   {
      float time = 0;
      Node testAddNodeOfTypeNtBase = testRoot.addNode("testAddNodeOfTypeNtBase", "nt:unstructured");
      long startTime = System.currentTimeMillis();
      for (int i = 1; i < NODES_COUNT_SHORT_SESSION; i++)
      {
         testAddNodeOfTypeNtBase.addNode("NodeOfTypeNtBase#" + i, "nt:base");
      }
      long endTime = System.currentTimeMillis();
      time += ((float)((endTime - startTime)) / NODES_COUNT_SHORT_SESSION);
      testAddNodeOfTypeNtBase.remove();
      session.save();
      log.info("[1.1 addNode short session                   ] average time: " + time + "ms");
   }

   public void testSetPropertyShortSession() throws Exception
   {
      float time = 0;
      Node testSetProperty = testRoot.addNode("testSetProperty", "nt:unstructured");
      long startTime = System.currentTimeMillis();
      for (int i = 1; i < NODES_COUNT_SHORT_SESSION; i++)
      {
         testSetProperty.setProperty("testProperty" + i, "1234567890");
      }
      long endTime = System.currentTimeMillis();
      time += ((float)((endTime - startTime)) / NODES_COUNT_SHORT_SESSION);
      testSetProperty.remove();
      session.save();
      log.info("[2.1 setProperty short session               ] average time: " + time + "ms");
   }

   public void testAddMixReferenceableToNodeOfTypeNtBaseShortSession() throws Exception
   {
      float time = 0;
      Node testAddMixReferenceableToNodeOfTypeNtBase =
         testRoot.addNode("testAddMixReferenceableToNodeOfTypeNtBase", "nt:unstructured");
      ArrayList<Node> nodesList = new ArrayList<Node>();
      for (int i = 1; i < NODES_COUNT_SHORT_SESSION; i++)
      {
         Node tmpNode = testAddMixReferenceableToNodeOfTypeNtBase.addNode("NodeOfTypeNtBase#" + i, "nt:base");
         nodesList.add(tmpNode);
      }
      long startTime = System.currentTimeMillis();
      for (Node tmpNode : nodesList)
      {
         tmpNode.addMixin("mix:referenceable");
      }
      long endTime = System.currentTimeMillis();
      time += ((float)((endTime - startTime)) / NODES_COUNT_SHORT_SESSION);
      testAddMixReferenceableToNodeOfTypeNtBase.remove();
      session.save();
      log.info("[3.1 add mix:referenceable short session     ] average time: " + time + "ms");
   }

   public void testSaveNodesShortSession() throws Exception
   {
      float time = 0;
      Node testSaveNodesShortSession = testRoot.addNode("testSaveNodesShortSession", "nt:unstructured");
      for (int i = 1; i < NODES_COUNT_SHORT_SESSION; i++)
      {
         testSaveNodesShortSession.addNode("NodeOfTypeNtBase#" + i, "nt:base");
      }
      long startTime = System.currentTimeMillis();
      session.save();
      long endTime = System.currentTimeMillis();
      time += ((float)((endTime - startTime)) / NODES_COUNT_SHORT_SESSION);

      testSaveNodesShortSession.remove();
      session.save();
      log.info("[4.1 nodes saving short session.save         ] average time: " + time + "ms");
   }

   public void testSaveNodesLongSession() throws Exception
   {
      float time = 0;
      Node testSaveNodesLongSession = testRoot.addNode("testSaveNodesLongSession", "nt:unstructured");
      for (int i = 1; i < NODES_COUNT_LONG_SESSION; i++)
      {
         testSaveNodesLongSession.addNode("NodeOfTypeNtBase#" + i, "nt:base");
      }
      long startTime = System.currentTimeMillis();
      session.save();
      long endTime = System.currentTimeMillis();
      time += ((float)((endTime - startTime)) / NODES_COUNT_LONG_SESSION);
      testSaveNodesLongSession.remove();
      session.save();
      log.info("[4.2 nodes saving long session.save          ] average time: " + time + "ms");
   }

   public void testSavePropertiesShortSession() throws Exception
   {
      float time = 0;
      Node testSavePropertiesShortSession = testRoot.addNode("testSavePropertiesShortSession", "nt:unstructured");
      for (int i = 1; i < NODES_COUNT_SHORT_SESSION; i++)
      {
         testSavePropertiesShortSession.setProperty("testProperty" + i, IdGenerator.generate());
      }
      long startTime = System.currentTimeMillis();
      session.save();
      long endTime = System.currentTimeMillis();
      time += ((float)((endTime - startTime)) / NODES_COUNT_SHORT_SESSION);
      testSavePropertiesShortSession.remove();
      session.save();
      log.info("[5.1 properties saving short session.save    ] average time: " + time + "ms");
   }

   public void testSavePropertiesLongSession() throws Exception
   {
      float time = 0;
      Node testSavePropertiesLongSession = testRoot.addNode("testSavePropertiesLongSession", "nt:unstructured");
      for (int i = 1; i < NODES_COUNT_LONG_SESSION; i++)
      {
         testSavePropertiesLongSession.setProperty("testProperty" + i, IdGenerator.generate());
      }
      long startTime = System.currentTimeMillis();
      session.save();
      long endTime = System.currentTimeMillis();
      time += ((float)((endTime - startTime)) / NODES_COUNT_LONG_SESSION);
      testSavePropertiesLongSession.remove();
      session.save();
      log.info("[5.2 properties saving long session.save     ] average time: " + time + "ms");
   }

   public void testAddVersionableMixinShortSession() throws Exception
   {
      float time = 0;
      Node testAddVersionableMixin = testRoot.addNode("testAddVersionableMixin", "nt:unstructured");
      ArrayList<Node> nodesList = new ArrayList<Node>();
      for (int i = 1; i < NODES_COUNT_SHORT_SESSION; i++)
      {
         Node tmpNode = testAddVersionableMixin.addNode("NodeOfTypeNtBase#" + i, "nt:base");
         nodesList.add(tmpNode);
      }
      long startTime = System.currentTimeMillis();
      for (Node tmpNode : nodesList)
      {
         tmpNode.addMixin("mix:versionable");
      }
      long endTime = System.currentTimeMillis();
      time += ((float)((endTime - startTime)) / NODES_COUNT_SHORT_SESSION);
      testAddVersionableMixin.remove();
      session.save();
      log.info("[6.1 add mix:versionable short session       ] average time: " + time + "ms");
   }

   public void testCheckinShortSession() throws Exception
   {
      float time = 0;
      Node testCheckin = testRoot.addNode("testCheckin", "nt:unstructured");
      ArrayList<Node> nodesList = new ArrayList<Node>();
      for (int i = 1; i < NODES_COUNT_SHORT_SESSION; i++)
      {
         Node tmpNode = testRoot.addNode("NodeOfTypeNtUnstructured#" + i, "nt:unstructured");
         tmpNode.addMixin("mix:versionable");
         nodesList.add(tmpNode);
      }
      session.save();
      long startTime = System.currentTimeMillis();
      for (Node tmpNode : nodesList)
      {
         tmpNode.checkin();
      }
      long endTime = System.currentTimeMillis();
      time += ((float)((endTime - startTime)) / NODES_COUNT_SHORT_SESSION);
      // to make node removeable
      for (Node tmpNode : nodesList)
      {
         tmpNode.checkout();
      }
      testCheckin.remove();
      session.save();
      log.info("[7.1 checkin short session                   ] average time: " + time + "ms");
   }

   public void testCheckoutShortSession() throws Exception
   {
      float time = 0;
      Node testCheckout = testRoot.addNode("testCheckout", "nt:unstructured");
      ArrayList<Node> nodesList = new ArrayList<Node>();
      for (int i = 1; i < NODES_COUNT_SHORT_SESSION; i++)
      {
         Node tmpNode = testCheckout.addNode("NodeOfTypeNtUnstructured#" + i, "nt:unstructured");
         tmpNode.addMixin("mix:versionable");
         session.save();
         tmpNode.checkin();
         nodesList.add(tmpNode);
      }
      session.save();
      long startTime = System.currentTimeMillis();
      for (Node tmpNode : nodesList)
      {
         tmpNode.checkout();
      }
      long endTime = System.currentTimeMillis();
      time += ((float)((endTime - startTime)) / NODES_COUNT_SHORT_SESSION);
      testCheckout.remove();
      session.save();
      log.info("[8.1 checkout short session                  ] average time: " + time + "ms");
   }

   public void testRemoveNodesShortSession() throws Exception
   {
      float time = 0;
      Node testRemoveNodes = testRoot.addNode("testRemoveNodes", "nt:unstructured");
      ArrayList<Node> nodesList = new ArrayList<Node>();
      for (int i = 1; i < NODES_COUNT_SHORT_SESSION; i++)
      {
         Node tmpNode = testRemoveNodes.addNode("NodeOfTypeNtUnstructured#" + i, "nt:unstructured");
         nodesList.add(tmpNode);
      }
      // session.save();
      long startTime = System.currentTimeMillis();
      for (Node tmpNode : nodesList)
      {
         tmpNode.remove();
      }
      long endTime = System.currentTimeMillis();
      time += ((float)((endTime - startTime)) / NODES_COUNT_SHORT_SESSION);
      testRemoveNodes.remove();
      session.save();
      log.info("[9.1 remove nodes short session              ] average time: " + time + "ms");
   }

   public void testRemovePropertiesShortSession() throws Exception
   {
      float time = 0;
      Node testRemoveProperties = testRoot.addNode("testRemoveProperties", "nt:unstructured");
      ArrayList<Node> nodesList = new ArrayList<Node>();
      for (int i = 1; i < NODES_COUNT_SHORT_SESSION; i++)
      {
         Node tmpNode = testRemoveProperties.addNode("NodeOfTypeNtUnstructured#" + i, "nt:unstructured");
         tmpNode.setProperty("testProperty", IdGenerator.generate());
         nodesList.add(tmpNode);
      }
      // session.save();
      long startTime = System.currentTimeMillis();
      for (Node tmpNode : nodesList)
      {
         tmpNode.getProperty("testProperty").remove();
      }
      long endTime = System.currentTimeMillis();
      time += ((float)((endTime - startTime)) / NODES_COUNT_SHORT_SESSION);
      testRemoveProperties.remove();
      session.save();
      log.info("[9.2 remove properties short session         ] average time: " + time + "ms");
   }

   public void testSaveRemovedNodesShortSession() throws Exception
   {
      float time = 0;
      Node testSaveRemovedNodes = testRoot.addNode("testSaveRemovedNodes", "nt:unstructured");
      ArrayList<Node> nodesList = new ArrayList<Node>();
      for (int i = 1; i < NODES_COUNT_SHORT_SESSION; i++)
      {
         Node tmpNode = testSaveRemovedNodes.addNode("NodeOfTypeNtUnstructured#" + i, "nt:unstructured");
         nodesList.add(tmpNode);
      }
      session.save();
      for (Node tmpNode : nodesList)
      {
         tmpNode.remove();
      }
      long startTime = System.currentTimeMillis();
      session.save();
      long endTime = System.currentTimeMillis();
      time += ((float)((endTime - startTime)) / NODES_COUNT_SHORT_SESSION);
      testSaveRemovedNodes.remove();
      session.save();
      log.info("[10.1 save removed nodes short session       ] average time: " + time + "ms");
   }

   public void testSaveRemovedPropertiesShortSession() throws Exception
   {
      float time = 0;
      Node testSaveRemovedNodes = testRoot.addNode("testSaveRemovedNodes", "nt:unstructured");
      ArrayList<Node> nodesList = new ArrayList<Node>();
      for (int i = 1; i < NODES_COUNT_SHORT_SESSION; i++)
      {
         Node tmpNode = testSaveRemovedNodes.addNode("NodeOfTypeNtUnstructured#" + i, "nt:unstructured");
         tmpNode.setProperty("testProperty", IdGenerator.generate());
         nodesList.add(tmpNode);
      }
      session.save();
      for (Node tmpNode : nodesList)
      {
         tmpNode.getProperty("testProperty").remove();
      }
      long startTime = System.currentTimeMillis();
      session.save();
      long endTime = System.currentTimeMillis();
      time += ((float)((endTime - startTime)) / NODES_COUNT_SHORT_SESSION);
      testSaveRemovedNodes.remove();
      session.save();
      log.info("[10.2 save removed properties short session  ] average time: " + time + "ms");
   }

   public void testLockShortSession() throws Exception
   {
      float time = 0;
      Node testLock = testRoot.addNode("testLock", "nt:unstructured");
      ArrayList<Node> nodesList = new ArrayList<Node>();
      for (int i = 1; i < NODES_COUNT_SHORT_SESSION; i++)
      {
         Node tmpNode = testLock.addNode("NodeOfTypeNtUnstructuredLock#" + i, "nt:unstructured");
         tmpNode.addMixin("mix:lockable");
         nodesList.add(tmpNode);
      }
      session.save();
      long startTime = System.currentTimeMillis();
      for (Node tmpNode : nodesList)
      {
         tmpNode.lock(true, true);
      }
      long endTime = System.currentTimeMillis();
      time += ((float)((endTime - startTime)) / NODES_COUNT_SHORT_SESSION);
      // to make node removeable
      for (Node tmpNode : nodesList)
      {
         tmpNode.unlock();
      }
      testLock.remove();
      session.save();
      log.info("[11.1 lock short session                     ] average time: " + time + "ms");
   }

   public void testUnlockShortSession() throws Exception
   {
      float time = 0;
      Node testUnlock = testRoot.addNode("testUnlock", "nt:unstructured");
      ArrayList<Node> nodesList = new ArrayList<Node>();
      for (int i = 1; i < NODES_COUNT_SHORT_SESSION; i++)
      {
         Node tmpNode = testUnlock.addNode("NodeOfTypeNtUnstructuredUnLock#" + i, "nt:unstructured");
         tmpNode.addMixin("mix:lockable");
         session.save();
         tmpNode.lock(true, true);
         nodesList.add(tmpNode);
      }
      session.save();
      long startTime = System.currentTimeMillis();
      for (Node tmpNode : nodesList)
      {
         tmpNode.unlock();
      }
      long endTime = System.currentTimeMillis();
      time += ((float)((endTime - startTime)) / NODES_COUNT_SHORT_SESSION);
      testUnlock.remove();
      session.save();
      log.info("[12.1 unlock short session                   ] average time: " + time + "ms");
   }

   public void testComplexOperationsAddNtFilePlusNtResource() throws Exception
   {
      // variables for the execution time
      int FILE_SIZE = 100;// 100 K
      Node testAddNtFilePlusNtResource = testRoot.addNode("testAddNtFilePlusNtResource", "nt:unstructured");
      TEST_FILE = createBLOBTempFile(FILE_SIZE).getAbsolutePath();
      long startTime = System.currentTimeMillis();
      for (int i = 0; i < NODES_COUNT_SHORT_SESSION; i++)
      {
         String name = new String("nnn-" + i);
         Node cool = testAddNtFilePlusNtResource.addNode(name, "nt:file");
         Node contentNode = cool.addNode("jcr:content", "nt:resource");
         contentNode.setProperty("jcr:encoding", "UTF-8");
         InputStream is = new FileInputStream(TEST_FILE);
         contentNode.setProperty("jcr:data", is);
         contentNode.setProperty("jcr:mimeType", "text/plain");
         contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));
         is.close();
      }
      long endTime = System.currentTimeMillis();
      log.info("[13.1 adding nt:file                         ] average time: "
         + ((float)((endTime - startTime)) / NODES_COUNT_SHORT_SESSION) + "ms");
      try
      {
         startTime = System.currentTimeMillis();
         session.save();
         endTime = System.currentTimeMillis();
         log.info("[13.2 saving nt:file                         ] average time: "
            + ((float)((endTime - startTime)) / NODES_COUNT_SHORT_SESSION) + "ms");
      }
      catch (Exception e)
      {
         log.error(e);
         fail("Error Save!!!");
      }
      testAddNtFilePlusNtResource.remove();
      session.save();
   }

   public void testGetNodeOfTypeNtBaseShortSession() throws Exception
   {
      float time = 0;
      Node testAddNodeOfTypeNtBase = testRoot.addNode("testAddNodeOfTypeNtBase", "nt:unstructured");
      for (int i = 1; i < NODES_COUNT_SHORT_SESSION; i++)
      {
         testAddNodeOfTypeNtBase.addNode("NodeOfTypeNtBase#" + i, "nt:base");
      }
      long startTime = System.currentTimeMillis();
      for (int i = 1; i < NODES_COUNT_SHORT_SESSION; i++)
      {
         testAddNodeOfTypeNtBase.getNode("NodeOfTypeNtBase#" + i);
      }
      long endTime = System.currentTimeMillis();
      time += ((float)((endTime - startTime)) / NODES_COUNT_SHORT_SESSION);
      testAddNodeOfTypeNtBase.remove();
      session.save();
      log.info("[14.1 getNode short session                  ] average time: " + time + "ms");
   }

   public void testGetPropertyShortSession() throws Exception
   {
      float time = 0;
      Node testSetProperty = testRoot.addNode("testSetProperty", "nt:unstructured");
      for (int i = 1; i < NODES_COUNT_SHORT_SESSION; i++)
      {
         testSetProperty.setProperty("testProperty" + i, "1234567890");
      }
      long startTime = System.currentTimeMillis();
      for (int i = 1; i < NODES_COUNT_SHORT_SESSION; i++)
      {
         testSetProperty.getProperty("testProperty" + i);
      }
      long endTime = System.currentTimeMillis();
      time += ((float)((endTime - startTime)) / NODES_COUNT_SHORT_SESSION);
      testSetProperty.remove();
      session.save();
      log.info("[15.1 getProperty short session              ] average time: " + time + "ms");
   }

   // jcr 1.5 case
   // protected File createBLOBTempFile(int sizeInKb) throws IOException {
   // return createBLOBTempFile("exo_jcr_test_temp_file_", sizeInKb);
   // }
   //
   // protected File createBLOBTempFile(String prefix, int sizeInKb) throws IOException {
   // // create test file
   // int BUFFER_SIZE = 1024; // 1KB
   // byte SYMBOL = 65; // symbol A
   // byte[] data = new byte[BUFFER_SIZE]; // 1KB
   // Arrays.fill(data, (byte) SYMBOL); // symbol A
   // File testFile = File.createTempFile(prefix, ".tmp");
   // FileOutputStream tempOut = PrivilegedFileHelper.fileOutputStream(testFile);
   // for (int i = 0; i < sizeInKb; i++) {
   // tempOut.write(data);
   // }
   // tempOut.close();
   // testFile.deleteOnExit(); // delete on test exit
   // log.info("Temp file created: " + testFile.getAbsolutePath() + " size: " + testFile.length());
   // return testFile;
   // }

}
