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
package org.exoplatform.services.jcr.impl.core;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.config.SimpleParameterEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig.DatabaseStructureType;
import org.exoplatform.services.jcr.storage.WorkspaceDataContainer;
import org.exoplatform.services.jcr.util.TesterConfigurationHelper;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionException;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: TestMoveNode.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestMoveNode extends JcrImplBaseTest
{
   private static int FILES_COUNT = 20;

   public void testMove() throws Exception
   {
      Node node1 = root.addNode("node1");
      Node node2 = node1.addNode("node2");
      Node node3 = root.addNode("node3");
      session.save();
      session.move(node1.getPath(), node3.getPath() + "/node4");
      session.save();
      node3.remove();
      session.save();

      try
      {
         root.getNode("node3");
         fail();
      }
      catch (PathNotFoundException e)
      {
         // ok
      }
   }

   public void testMoveWithoutTriggering() throws Exception
   {
      Node node1 = root.addNode("nodeToBeRenamed");
      session.save();
      session.move(node1.getPath(), node1.getPath() + "-testing", false);
      session.save();

      try
      {
         root.getNode("nodeToBeRenamed");
         fail();
      }
      catch (PathNotFoundException e)
      {
         // ok
      }
   }

   public void testMoveAndRefreshTrue() throws Exception
   {
      testMoveAndRefreshTrue(true);
   }

   public void testMoveAndRefreshTrueWithoutTriggering() throws Exception
   {
      testMoveAndRefreshTrue(false);
   }

   private void testMoveAndRefreshTrue(boolean triggerEventsForDescendants) throws Exception
   {
      Node node1 = root.addNode("node1");
      Node node2 = node1.addNode("node2");
      Node node3 = root.addNode("node3");
      session.save();
      assertEquals("/node1", node1.getPath());
      assertEquals("/node1/node2", node2.getPath());
      assertEquals("/node1/node2/jcr:primaryType", node2.getProperty("jcr:primaryType").getPath());
      session.move(node1.getPath(), node3.getPath() + "/node4", triggerEventsForDescendants);
      assertEquals(node3.getPath() + "/node4", node1.getPath());
      assertEquals(node3.getPath() + "/node4/node2", node2.getPath());
      assertEquals(node3.getPath() + "/node4/node2/jcr:primaryType", node2.getProperty("jcr:primaryType").getPath());
      session.refresh(true);
      assertEquals(node3.getPath() + "/node4", node1.getPath());
      assertEquals(node3.getPath() + "/node4/node2", node2.getPath());
      assertEquals(node3.getPath() + "/node4/node2/jcr:primaryType", node2.getProperty("jcr:primaryType").getPath());

      try
      {
         root.getNode("node1");
         fail();
      }
      catch (PathNotFoundException e)
      {
         // ok
      }

      try
      {
         session.getItem("/node1/node2");
         fail();
      }
      catch (PathNotFoundException e)
      {
         // ok
      }

      try
      {
         session.getItem("/node1/jcr:primaryType");
         fail();
      }
      catch (PathNotFoundException e)
      {
         // ok
      }

      try
      {
         session.getItem("/node1/node2/jcr:primaryType");
         fail();
      }
      catch (PathNotFoundException e)
      {
         // ok
      }

      try
      {
         node3.getNode("node4");
      }
      catch (PathNotFoundException e1)
      {
         fail();
      }

      node3.remove();
      session.save();

      try
      {
         root.getNode("node3");
         fail();
      }
      catch (PathNotFoundException e)
      {
         // ok
      }
   }

   public void testMoveAndRefreshFalse() throws Exception
   {
      testMoveAndRefreshFalse(true);
   }

   public void testMoveAndRefreshFalseWithoutTriggering() throws Exception
   {
      testMoveAndRefreshFalse(false);
   }

   private void testMoveAndRefreshFalse(boolean triggerEventsForDescendants) throws Exception
   {
      Node node1 = root.addNode("node1");
      Node node2 = node1.addNode("node2");
      Node node3 = root.addNode("node3");
      session.save();
      assertEquals("/node1", node1.getPath());
      assertEquals("/node1/node2", node2.getPath());
      assertEquals("/node1/node2/jcr:primaryType", node2.getProperty("jcr:primaryType").getPath());
      session.move(node1.getPath(), node3.getPath() + "/node4", triggerEventsForDescendants);
      assertEquals(node3.getPath() + "/node4", node1.getPath());
      assertEquals(node3.getPath() + "/node4/node2", node2.getPath());
      assertEquals(node3.getPath() + "/node4/node2/jcr:primaryType", node2.getProperty("jcr:primaryType").getPath());
      session.refresh(false);
      assertEquals("/node1", node1.getPath());
      assertEquals("/node1/node2", node2.getPath());
      assertEquals("/node1/node2/jcr:primaryType", node2.getProperty("jcr:primaryType").getPath());
      try
      {
         node3.getNode("node4");
         fail();
      }
      catch (PathNotFoundException e1)
      {
         // ok
      }

      node3.remove();
      session.save();

      try
      {
         root.getNode("node3");
         fail();
      }
      catch (PathNotFoundException e)
      {
         // ok
      }
   }

   public void testIsMoveModifed() throws Exception
   {
      Node node1 = root.addNode("node1");
      Node node2 = node1.addNode("node2");

      Node node3 = root.addNode("node3");
      session.save();
      assertFalse(node2.isModified());
      node2.setProperty("test", "sdf");
      assertTrue(node2.isModified());
      session.move(node1.getPath(), node3.getPath() + "/node4");
      assertTrue(node2.isModified());
   }

   public void testMoveAndRefreshTrueNSave() throws Exception
   {
      testMoveAndRefreshTrueNSave(true);
   }

   public void testMoveAndRefreshTrueNSaveWithoutTriggering() throws Exception
   {
      testMoveAndRefreshTrueNSave(false);
   }

   private void testMoveAndRefreshTrueNSave(boolean triggerEventsForDescendants) throws Exception
   {
      Node node1 = root.addNode("node1");
      Node node2 = node1.addNode("node2");
      Node node3 = root.addNode("node3");
      session.save();
      assertEquals("/node1", node1.getPath());
      assertEquals("/node1/node2", node2.getPath());
      assertEquals("/node1/node2/jcr:primaryType", node2.getProperty("jcr:primaryType").getPath());
      session.move(node1.getPath(), node3.getPath() + "/node4", triggerEventsForDescendants);
      assertEquals(node3.getPath() + "/node4", node1.getPath());
      assertEquals(node3.getPath() + "/node4/node2", node2.getPath());
      assertEquals(node3.getPath() + "/node4/node2/jcr:primaryType", node2.getProperty("jcr:primaryType").getPath());
      session.refresh(true);
      session.save();
      assertEquals(node3.getPath() + "/node4", node1.getPath());
      assertEquals(node3.getPath() + "/node4/node2", node2.getPath());
      assertEquals(node3.getPath() + "/node4/node2/jcr:primaryType", node2.getProperty("jcr:primaryType").getPath());

      Query query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/node1'", Query.SQL);
      QueryResult result = query.execute();
      assertFalse(result.getNodes().hasNext());
      query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/node1/node2'", Query.SQL);
      result = query.execute();
      assertFalse(result.getNodes().hasNext());

      query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/node3/node4'", Query.SQL);
      result = query.execute();
      NodeIterator ni = result.getNodes();
      assertTrue(ni.hasNext());
      Node n = ni.nextNode();
      assertEquals("/node3/node4", n.getPath());
      assertFalse(ni.hasNext());
      query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/node3/node4/node2'", Query.SQL);
      result = query.execute();
      ni = result.getNodes();
      assertTrue(ni.hasNext());
      n = ni.nextNode();
      assertEquals("/node3/node4/node2", n.getPath());
      assertEquals("/node3/node4/node2/jcr:primaryType", n.getProperty("jcr:primaryType").getPath());
      assertFalse(ni.hasNext());

      try
      {
         root.getNode("node1");
         fail();
      }
      catch (PathNotFoundException e)
      {
         // ok
      }

      try
      {
         session.getItem("/node1/node2");
         fail();
      }
      catch (PathNotFoundException e)
      {
         // ok
      }

      try
      {
         session.getItem("/node1/jcr:primaryType");
         fail();
      }
      catch (PathNotFoundException e)
      {
         // ok
      }

      try
      {
         session.getItem("/node1/node2/jcr:primaryType");
         fail();
      }
      catch (PathNotFoundException e)
      {
         // ok
      }

      try
      {
         node3.getNode("node4");
      }
      catch (PathNotFoundException e1)
      {
         fail();
      }

      node3.remove();
      session.save();

      try
      {
         root.getNode("node3");
         fail();
      }
      catch (PathNotFoundException e)
      {
         // ok
      }
   }

   public void testMoveAndRefreshFalseNSave() throws Exception
   {
      testMoveAndRefreshFalseNSave(true);
   }

   public void testMoveAndRefreshFalseNSaveWithoutTriggering() throws Exception
   {
      testMoveAndRefreshFalseNSave(false);
   }

   private void testMoveAndRefreshFalseNSave(boolean triggerEventsForDescendants) throws Exception
   {
      Node node1 = root.addNode("node1");
      Node node2 = node1.addNode("node2");
      Node node3 = root.addNode("node3");
      session.save();
      assertEquals("/node1", node1.getPath());
      assertEquals("/node1/node2", node2.getPath());
      assertEquals("/node1/node2/jcr:primaryType", node2.getProperty("jcr:primaryType").getPath());
      session.move(node1.getPath(), node3.getPath() + "/node4", triggerEventsForDescendants);
      assertEquals(node3.getPath() + "/node4", node1.getPath());
      assertEquals(node3.getPath() + "/node4/node2", node2.getPath());
      assertEquals(node3.getPath() + "/node4/node2/jcr:primaryType", node2.getProperty("jcr:primaryType").getPath());
      session.refresh(false);
      session.save();
      assertEquals("/node1", node1.getPath());
      assertEquals("/node1/node2", node2.getPath());
      assertEquals("/node1/node2/jcr:primaryType", node2.getProperty("jcr:primaryType").getPath());

      Query query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/node1'", Query.SQL);
      QueryResult result = query.execute();
      NodeIterator ni = result.getNodes();
      assertTrue(ni.hasNext());
      Node n = ni.nextNode();
      assertEquals("/node1", n.getPath());
      assertFalse(ni.hasNext());
      query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/node1/node2'", Query.SQL);
      result = query.execute();
      ni = result.getNodes();
      assertTrue(ni.hasNext());
      n = ni.nextNode();
      assertEquals("/node1/node2", n.getPath());
      assertEquals("/node1/node2/jcr:primaryType", n.getProperty("jcr:primaryType").getPath());
      assertFalse(ni.hasNext());

      query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/node3/node4'", Query.SQL);
      result = query.execute();
      assertFalse(result.getNodes().hasNext());
      query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/node3/node4/node2'", Query.SQL);
      result = query.execute();
      assertFalse(result.getNodes().hasNext());

      try
      {
         node3.getNode("node4");
         fail();
      }
      catch (PathNotFoundException e1)
      {
         // ok
      }

      node3.remove();
      session.save();

      try
      {
         root.getNode("node3");
         fail();
      }
      catch (PathNotFoundException e)
      {
         // ok
      }
   }
   public void testMoveTwice() throws Exception
   {
      testMoveTwice(true);
   }

   public void testMoveTwiceWithoutTriggering() throws Exception
   {
      testMoveTwice(false);
   }

   private void testMoveTwice(boolean triggerEventsForDescendants) throws Exception
   {
      Node node1 = root.addNode("node1");
      Node node2 = node1.addNode("node2");
      node2.addMixin("mix:referenceable");
      node2.addNode("subnode2");
      Node node3 = root.addNode("node3");
      session.save();
      // root/node1/node2
      // root/node3
      session.move(node1.getPath(), node3.getPath() + "/node4", triggerEventsForDescendants);

      String id = node2.getUUID();
      // root/node3/node4/node2

      try
      {
         root.getNode("node1");
         fail();
      }
      catch (PathNotFoundException e)
      {
         // ok
      }

      try
      {
         session.getItem("/node1");
         fail();
      }
      catch (PathNotFoundException e)
      {
         // ok
      }

      try
      {
         session.getItem("/node1/jcr:primaryType");
         fail();
      }
      catch (PathNotFoundException e)
      {
         // ok
      }

      assertTrue(root.hasNode("node3/node4"));
      assertEquals("/node3/node4", root.getNode("node3/node4").getPath());
      assertEquals("/node3/node4/jcr:primaryType", root.getNode("node3/node4").getProperty("jcr:primaryType").getPath());
      assertTrue(root.hasNode("node3/node4/node2"));
      assertEquals("/node3/node4/node2", root.getNode("node3/node4/node2").getPath());
      assertEquals("/node3/node4/node2/jcr:primaryType", root.getNode("node3/node4/node2").getProperty("jcr:primaryType").getPath());
      assertEquals("/node3/node4/node2/subnode2", root.getNode("node3/node4/node2/subnode2").getPath());
      assertEquals("/node3/node4/node2/subnode2/jcr:primaryType", root.getNode("node3/node4/node2/subnode2").getProperty("jcr:primaryType").getPath());

      Node node = session.getNodeByUUID(id);
      assertEquals("/node3/node4/node2", node.getPath());
      assertEquals("/node3/node4/node2/jcr:primaryType", node.getProperty("jcr:primaryType").getPath());
      assertEquals("/node3/node4/node2/subnode2", node.getNode("subnode2").getPath());
      assertEquals("/node3/node4/node2/subnode2/jcr:primaryType", root.getNode("node3/node4/node2/subnode2").getProperty("jcr:primaryType").getPath());

      // root/node3/node4
      // root/node5
      session.move(node3.getPath() + "/node4/node2", root.getPath() + "node5", triggerEventsForDescendants);

      try
      {
         root.getNode("node3/node4/node2");
         fail();
      }
      catch (PathNotFoundException e)
      {
         // ok
      }

      try
      {
         session.getItem("/node3/node4/node2");
         fail();
      }
      catch (PathNotFoundException e)
      {
         // ok
      }

      try
      {
         session.getItem("/node3/node4/node2/jcr:primaryType");
         fail();
      }
      catch (PathNotFoundException e)
      {
         // ok
      }

      try
      {
         session.getItem("/node3/node4/node2/subnode2");
         fail();
      }
      catch (PathNotFoundException e)
      {
         // ok
      }

      try
      {
         session.getItem("/node3/node4/node2/subnode2/jcr:primaryType");
         fail();
      }
      catch (PathNotFoundException e)
      {
         // ok
      }

      Node node5 = root.getNode("node5");
      assertEquals("/node5", root.getNode("node5").getPath());
      assertEquals("/node5/jcr:primaryType", root.getNode("node5").getProperty("jcr:primaryType").getPath());
      assertEquals("/node5/subnode2", root.getNode("node5/subnode2").getPath());
      assertEquals("/node5/subnode2/jcr:primaryType", root.getNode("node5/subnode2").getProperty("jcr:primaryType").getPath());
      node = session.getNodeByUUID(id);
      assertEquals("/node5", node.getPath());
      assertEquals("/node5/jcr:primaryType", node.getProperty("jcr:primaryType").getPath());
      assertEquals("/node5/subnode2", node.getNode("subnode2").getPath());
      assertEquals("/node5/subnode2/jcr:primaryType", node.getNode("subnode2").getProperty("jcr:primaryType").getPath());

      assertEquals(QPath.makeChildPath(((NodeImpl)root).getData().getQPath(), new InternalQName("", "node5"),

      0).getAsString(), ((NodeImpl)node2).getData().getQPath().getAsString());

      session.save();

      assertEquals("/node5", root.getNode("node5").getPath());
      assertEquals("/node5/jcr:primaryType", root.getNode("node5").getProperty("jcr:primaryType").getPath());
      assertEquals("/node5/subnode2", root.getNode("node5/subnode2").getPath());
      assertEquals("/node5/subnode2/jcr:primaryType", root.getNode("node5/subnode2").getProperty("jcr:primaryType").getPath());
      node = session.getNodeByUUID(id);
      assertEquals("/node5", node.getPath());
      assertEquals("/node5/jcr:primaryType", node.getProperty("jcr:primaryType").getPath());
      assertEquals("/node5/subnode2", node.getNode("subnode2").getPath());
      assertEquals("/node5/subnode2/jcr:primaryType", node.getNode("subnode2").getProperty("jcr:primaryType").getPath());

      Query query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/node1'", Query.SQL);
      QueryResult result = query.execute();
      assertFalse(result.getNodes().hasNext());
      query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/node1/node2'", Query.SQL);
      result = query.execute();
      assertFalse(result.getNodes().hasNext());
      query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/node1/node2/subnode2'", Query.SQL);
      result = query.execute();
      assertFalse(result.getNodes().hasNext());
      query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/node3/node4/node2'", Query.SQL);
      result = query.execute();
      assertFalse(result.getNodes().hasNext());
      query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/node3/node4/node2/subnode2'", Query.SQL);
      result = query.execute();
      assertFalse(result.getNodes().hasNext());

      query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/node3/node4'", Query.SQL);
      result = query.execute();
      NodeIterator ni = result.getNodes();
      assertTrue(ni.hasNext());
      Node n = ni.nextNode();
      assertEquals("/node3/node4", n.getPath());
      assertFalse(ni.hasNext());
      query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/node5'", Query.SQL);
      result = query.execute();
      ni = result.getNodes();
      assertTrue(ni.hasNext());
      n = ni.nextNode();
      assertEquals("/node5", n.getPath());
      assertEquals("/node5/jcr:primaryType", n.getProperty("jcr:primaryType").getPath());
      assertFalse(ni.hasNext());
      query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/node5/subnode2'", Query.SQL);
      result = query.execute();
      ni = result.getNodes();
      assertTrue(ni.hasNext());
      n = ni.nextNode();
      assertEquals("/node5/subnode2", n.getPath());
      assertEquals("/node5/subnode2/jcr:primaryType", n.getProperty("jcr:primaryType").getPath());
      assertFalse(ni.hasNext());

      node5.remove();
      node3.remove();
      session.save();

      try
      {
         root.getNode("node3");
         fail();
      }
      catch (PathNotFoundException e)
      {
         // ok
      }

   }

   public void testRenameNodeWithBinaryProperty() throws Exception
   {
      Node parentNode = root.addNode("testRenameNodeWithBinaryProperty");
      String path = parentNode.getPath();
      String value = "my Property Value";

      parentNode.setProperty("myBinaryData", new ByteArrayInputStream(value.getBytes("UTF-8")));
      root.save();

      value = "my Property Value 2";
      parentNode.setProperty("myBinaryData", new ByteArrayInputStream(value.getBytes("UTF-8")));
      session.move(path, path + "2");

      value = "my Property Value 3";
      parentNode.setProperty("myBinaryData", new ByteArrayInputStream(value.getBytes("UTF-8")));
      session.move(path + "2", path + "3");
      session.save();

      Item i = session.getItem(path + "3/myBinaryData");

      assertTrue(i instanceof Property);

      Property p = (Property)i;
      InputStream is = p.getStream();
      byte[] bValue = new byte[is.available()];
      is.read(bValue);
      is.close();

      assertEquals(value, new String(bValue, "UTF-8"));
   }

   public void testRenameNodeWithBinaryProperty2() throws Exception
   {
      Node parentNode = root.addNode("testRenameNodeWithBinaryProperty");
      String path = parentNode.getPath();

      String value = "my Property Value";
      parentNode.setProperty("myBinaryData", new ByteArrayInputStream(value.getBytes("UTF-8")));
      session.move(path, path + "2");
      session.save();

      Item i = session.getItem(path + "2/myBinaryData");

      assertTrue(i instanceof Property);
      Property p = (Property)i;
      InputStream is = p.getStream();
      byte[] bValue = new byte[is.available()];
      is.read(bValue);
      is.close();

      assertEquals(value, new String(bValue, "UTF-8"));
   }

   public void testLocalBigFiles() throws Exception
   {
      Node testBinaryValue = root.addNode("testBinaryValue");
      Node testLocalBigFiles = testBinaryValue.addNode("testLocalBigFiles");
      long startTime, endTime;
      startTime = System.currentTimeMillis(); // to get the time of start

      List<String> filesList = new ArrayList<String>();
      Random random = new Random();
      String TEST_FILE = "";

      InputStream is[] = new InputStream[FILES_COUNT];

      for (int i = 0; i < FILES_COUNT; i++)
      {
         TEST_FILE = createBLOBTempFile("testMove", random.nextInt(1024)).getAbsolutePath();
         filesList.add(TEST_FILE);
         Node localBigFile = testLocalBigFiles.addNode("bigFile" + i, "nt:file");
         Node contentNode = localBigFile.addNode("jcr:content", "nt:resource");
         // contentNode.setProperty("jcr:encoding", "UTF-8");
         is[i] = new FileInputStream(TEST_FILE);
         contentNode.setProperty("jcr:data", is[i]);
         contentNode.setProperty("jcr:mimeType", "application/octet-stream ");
         if (log.isDebugEnabled())
            log.debug("Data is set: " + TEST_FILE);
         // contentNode.setProperty("jcr:mimeType", "video/avi");
         contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
      }

      if (log.isDebugEnabled())
         log.debug("Saving: " + TEST_FILE + " " + Runtime.getRuntime().freeMemory());
      session.save();
      if (log.isDebugEnabled())
         log.debug("Saved: " + TEST_FILE + " " + Runtime.getRuntime().freeMemory());

      // close streams
      for (int i = 0; i < FILES_COUNT; i++)
      {
         is[i].close();
      }

      endTime = System.currentTimeMillis();
      if (log.isDebugEnabled())
         log.debug("Execution time after adding and saving (local big):" + ((endTime - startTime) / 1000) + "s");

      Node dstNode = testLocalBigFiles.addNode("dst");
      try
      {

         for (int i = 0; i < FILES_COUNT; i++)
         {
            session.move(testLocalBigFiles.getPath() + "/bigFile" + i, dstNode.getPath() + "/bigFile" + i);
         }
         session.save();
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail();
      }

      for (int i = 0; i < FILES_COUNT; i++)
      {
         Node localBigFile = dstNode.getNode("bigFile" + i);
         Node contentNode = localBigFile.getNode("jcr:content");
         compareStream(new FileInputStream(filesList.get(i)), contentNode.getProperty("jcr:data").getStream());
      }
   }

   /**
    * We have A/B moved to C/B without generation events for B.
    * Will checked if B reloaded its data.
    */
   public void testMoveWithoutGenerationChangesForAllSubTree() throws Exception
   {
      TesterConfigurationHelper helper = TesterConfigurationHelper.getInstance();
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(DatabaseStructureType.MULTI, null);
      wsEntry.getContainer().getParameters()
         .add(new SimpleParameterEntry(WorkspaceDataContainer.TRIGGER_EVENTS_FOR_DESCENDANTS_ON_RENAME, "false"));

      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.MULTI, null);
      helper.addWorkspace(repository, wsEntry);

      testMoveForAllSubTree(wsEntry, repository);
   }

   /**
    * We have A/B moved to C/B without generation events for B.
    * Will checked if B reloaded its data.
    */
   public void testMoveWithoutGenerationChangesForAllSubTreeBadSpelled() throws Exception
   {
      TesterConfigurationHelper helper = TesterConfigurationHelper.getInstance();
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(DatabaseStructureType.MULTI, null);
      wsEntry.getContainer().getParameters()
         .add(new SimpleParameterEntry(WorkspaceDataContainer.TRIGGER_EVENTS_FOR_DESCENDENTS_ON_RENAME, "false"));

      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.MULTI, null);
      helper.addWorkspace(repository, wsEntry);

      testMoveForAllSubTree(wsEntry, repository);
   }

   /**
    * We have A/B moved to C/B without generation events for B.
    * Will checked if B reloaded its data.
    */
   public void testMoveWithoutGenerationChangesForAllSubTreeWithMaxDescParam() throws Exception
   {
      TesterConfigurationHelper helper = TesterConfigurationHelper.getInstance();
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(DatabaseStructureType.MULTI, null);
      wsEntry.getContainer().getParameters()
         .add(new SimpleParameterEntry(WorkspaceDataContainer.MAX_DESCENDANTS_NODES_ALLOWED_ON_MOVE, "2"));

      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.MULTI, null);
      helper.addWorkspace(repository, wsEntry);

      testMoveForAllSubTree(wsEntry, repository);
   }

   /**
    * We have A/B moved to C/B without generation events for B.
    * Will checked if B reloaded its data.
    */
   public void testMoveWithoutGenerationChangesForAllSubTreeWithMoveParam() throws Exception
   {
      TesterConfigurationHelper helper = TesterConfigurationHelper.getInstance();
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(DatabaseStructureType.MULTI, null);
      wsEntry.getContainer().getParameters()
         .add(new SimpleParameterEntry(WorkspaceDataContainer.TRIGGER_EVENTS_FOR_DESCENDANTS_ON_MOVE, "false"));

      ManageableRepository repository = helper.createRepository(container, DatabaseStructureType.MULTI, null);
      helper.addWorkspace(repository, wsEntry);

      testMoveForAllSubTree(wsEntry, repository);
   }

   private void testMoveForAllSubTree(WorkspaceEntry wsEntry, ManageableRepository repository) throws LoginException,
      NoSuchWorkspaceException, RepositoryException, ItemExistsException, PathNotFoundException, VersionException,
      ConstraintViolationException, LockException, AccessDeniedException, InvalidItemStateException,
      InvalidQueryException
   {
      SessionImpl session = (SessionImpl)repository.login(credentials, wsEntry.getName());

      Node nodeA = session.getRootNode().addNode("A");
      Node nodeB = nodeA.addNode("B");
      Node nodeB1 = nodeB.addNode("B1");
      Node nodeB2 = nodeB1.addNode("B2");
      session.save();

      assertEquals("/A", nodeA.getPath());
      assertEquals("/A/B", nodeB.getPath());
      assertEquals("/A/B/B1", nodeB1.getPath());
      assertEquals("/A/B/B1/B2", nodeB2.getPath());

      session.move("/A", "/C");

      assertEquals("/C", nodeA.getPath());
      assertEquals("/C/B", nodeB.getPath());
      assertEquals("/C/B/B1", nodeB1.getPath());
      assertEquals("/C/B/B1/B2", nodeB2.getPath());

      session.refresh(false);

      assertEquals("/A", nodeA.getPath());
      assertEquals("/A/B", nodeB.getPath());
      assertEquals("/A/B/B1", nodeB1.getPath());
      assertEquals("/A/B/B1/B2", nodeB2.getPath());

      session.move("/A", "/C");
      session.save();

      assertEquals("/C", nodeA.getPath());
      assertEquals("/C/B", nodeB.getPath());
      assertEquals("/C/B/B1", nodeB1.getPath());
      assertEquals("/C/B/B1/B2", nodeB2.getPath());

      Query query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/A'", Query.SQL);
      QueryResult result = query.execute();
      assertFalse(result.getNodes().hasNext());
      query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/A/B'", Query.SQL);
      result = query.execute();
      assertFalse(result.getNodes().hasNext());
      query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/A/B/B1'", Query.SQL);
      result = query.execute();
      assertFalse(result.getNodes().hasNext());
      query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/A/B/B1/B2'", Query.SQL);
      result = query.execute();
      assertFalse(result.getNodes().hasNext());

      query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/C'", Query.SQL);
      result = query.execute();
      NodeIterator ni = result.getNodes();
      assertTrue(ni.hasNext());
      Node n = ni.nextNode();
      assertEquals("/C", n.getPath());
      assertFalse(ni.hasNext());
      query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/C/B'", Query.SQL);
      result = query.execute();
      ni = result.getNodes();
      assertTrue(ni.hasNext());
      n = ni.nextNode();
      assertEquals("/C/B", n.getPath());
      assertFalse(ni.hasNext());
      query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/C/B/B1'", Query.SQL);
      result = query.execute();
      ni = result.getNodes();
      assertTrue(ni.hasNext());
      n = ni.nextNode();
      assertEquals("/C/B/B1", n.getPath());
      assertFalse(ni.hasNext());
      query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/C/B/B1/B2'", Query.SQL);
      result = query.execute();
      ni = result.getNodes();
      assertTrue(ni.hasNext());
      n = ni.nextNode();
      assertEquals("/C/B/B1/B2", n.getPath());
      assertFalse(ni.hasNext());
   }

   public void testMoveAndRemoveTree() throws Exception
   {
      testMoveAndRemoveTree(true);
   }

   public void testMoveAndRemoveTreeWithoutTriggering() throws Exception
   {
      testMoveAndRemoveTree(false);
   }

   private void testMoveAndRemoveTree(boolean triggerEventsForDescendants) throws Exception
   {
      Node testRoot = root.addNode("test");
      testRoot.addMixin("mix:referenceable");

      Node node1_1 = testRoot.addNode("node1");
      node1_1.addMixin("mix:referenceable");

      Node node1_2 = testRoot.addNode("node1");
      node1_2.addMixin("mix:referenceable");

      Node node1_3 = testRoot.addNode("node1");
      node1_3.addMixin("mix:referenceable");

      Node node2_1 = node1_3.addNode("node2_1");
      node2_1.addMixin("mix:referenceable");

      Node node3_1 = node2_1.addNode("node3_1");
      node3_1.addMixin("mix:referenceable");
      session.save();

      // move tree
      session.move("/test", "/newtest", triggerEventsForDescendants);
      session.save();

      Node testRootMoved = root.getNode("newtest");
      Property testRootPRMoved = testRootMoved.getProperty("jcr:primaryType");

      Node node1_1Moved = testRootMoved.getNode("node1[1]");
      Property node1_1PRMoved = node1_1Moved.getProperty("jcr:primaryType");

      Node node1_2Moved = testRootMoved.getNode("node1[2]");
      Property node1_2PRMoved = node1_2Moved.getProperty("jcr:primaryType");

      Node node1_3Moved = testRootMoved.getNode("node1[3]");
      Property node1_3PRMoved = node1_3Moved.getProperty("jcr:primaryType");

      Node node2_1Moved = node1_3Moved.getNode("node2_1");
      Property node2_1PRMoved = node2_1Moved.getProperty("jcr:primaryType");

      Node node3_1Moved = node2_1Moved.getNode("node3_1");
      Property node3_1PRMoved = node3_1Moved.getProperty("jcr:primaryType");

      assertEquals("/newtest", testRootMoved.getPath());
      assertEquals("/newtest/node1", node1_1Moved.getPath());
      assertEquals("/newtest/node1[2]", node1_2Moved.getPath());
      assertEquals("/newtest/node1[3]", node1_3Moved.getPath());
      assertEquals("/newtest/node1[3]/node2_1", node2_1Moved.getPath());
      assertEquals("/newtest/node1[3]/node2_1/node3_1", node3_1Moved.getPath());
      assertEquals("/newtest/jcr:primaryType", testRootPRMoved.getPath());
      assertEquals("/newtest/node1/jcr:primaryType", node1_1PRMoved.getPath());
      assertEquals("/newtest/node1[2]/jcr:primaryType", node1_2PRMoved.getPath());
      assertEquals("/newtest/node1[3]/jcr:primaryType", node1_3PRMoved.getPath());
      assertEquals("/newtest/node1[3]/node2_1/jcr:primaryType", node2_1PRMoved.getPath());
      assertEquals("/newtest/node1[3]/node2_1/node3_1/jcr:primaryType", node3_1PRMoved.getPath());

      // move sns node newtest/node1[1]
      session.move("/newtest/node1", "/newtest/node4", triggerEventsForDescendants);
      session.save();

      node1_2Moved = testRootMoved.getNode("node1[1]");
      node1_2PRMoved = node1_2Moved.getProperty("jcr:primaryType");

      node1_3Moved = testRootMoved.getNode("node1[2]");
      node1_3PRMoved = node1_3Moved.getProperty("jcr:primaryType");

      node2_1Moved = node1_3Moved.getNode("node2_1");
      node2_1PRMoved = node2_1Moved.getProperty("jcr:primaryType");

      node3_1Moved = node2_1Moved.getNode("node3_1");
      node3_1PRMoved = node3_1Moved.getProperty("jcr:primaryType");

      assertEquals("/newtest/node1", node1_2Moved.getPath());
      assertEquals("/newtest/node1[2]", node1_3Moved.getPath());
      assertEquals("/newtest/node1[2]/node2_1", node2_1Moved.getPath());
      assertEquals("/newtest/node1[2]/node2_1/node3_1", node3_1Moved.getPath());
      assertEquals("/newtest/node1/jcr:primaryType", node1_2PRMoved.getPath());
      assertEquals("/newtest/node1[2]/jcr:primaryType", node1_3PRMoved.getPath());
      assertEquals("/newtest/node1[2]/node2_1/jcr:primaryType", node2_1PRMoved.getPath());
      assertEquals("/newtest/node1[2]/node2_1/node3_1/jcr:primaryType", node3_1PRMoved.getPath());

      Query query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/test'", Query.SQL);
      QueryResult result = query.execute();
      assertFalse(result.getNodes().hasNext());
      query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/test/node1'", Query.SQL);
      result = query.execute();
      assertFalse(result.getNodes().hasNext());
      query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/test/node1[2]'", Query.SQL);
      result = query.execute();
      assertFalse(result.getNodes().hasNext());
      query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/test/node1[3]'", Query.SQL);
      result = query.execute();
      assertFalse(result.getNodes().hasNext());
      query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/test/node1[3]/node2_1'", Query.SQL);
      result = query.execute();
      assertFalse(result.getNodes().hasNext());
      query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/test/node1[3]/node2_1/node3_1'", Query.SQL);
      result = query.execute();
      assertFalse(result.getNodes().hasNext());
      query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/newtest/node1[3]'", Query.SQL);
      result = query.execute();
      assertFalse(result.getNodes().hasNext());

      query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/newtest/node1'", Query.SQL);
      result = query.execute();
      NodeIterator ni = result.getNodes();
      assertTrue(ni.hasNext());
      Node n = ni.nextNode();
      assertEquals("/newtest/node1", n.getPath());
      assertEquals("/newtest/node1/jcr:primaryType", n.getProperty("jcr:primaryType").getPath());
      assertFalse(ni.hasNext());
      query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/newtest/node1[2]'", Query.SQL);
      result = query.execute();
      ni = result.getNodes();
      assertTrue(ni.hasNext());
      n = ni.nextNode();
      assertEquals("/newtest/node1[2]", n.getPath());
      assertEquals("/newtest/node1[2]/jcr:primaryType", n.getProperty("jcr:primaryType").getPath());
      assertFalse(ni.hasNext());
      query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/newtest/node1[2]/node2_1'", Query.SQL);
      result = query.execute();
      ni = result.getNodes();
      assertTrue(ni.hasNext());
      n = ni.nextNode();
      assertEquals("/newtest/node1[2]/node2_1", n.getPath());
      assertEquals("/newtest/node1[2]/node2_1/jcr:primaryType", n.getProperty("jcr:primaryType").getPath());
      assertFalse(ni.hasNext());
      query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/newtest/node1[2]/node2_1/node3_1'", Query.SQL);
      result = query.execute();
      ni = result.getNodes();
      assertTrue(ni.hasNext());
      n = ni.nextNode();
      assertEquals("/newtest/node1[2]/node2_1/node3_1", n.getPath());
      assertEquals("/newtest/node1[2]/node2_1/node3_1/jcr:primaryType", n.getProperty("jcr:primaryType").getPath());
      assertFalse(ni.hasNext());
      query = session.getWorkspace().getQueryManager().createQuery("select * from nt:base where jcr:path = '/newtest/node4'", Query.SQL);
      result = query.execute();
      ni = result.getNodes();
      assertTrue(ni.hasNext());
      n = ni.nextNode();
      assertEquals("/newtest/node4", n.getPath());
      assertEquals("/newtest/node4/jcr:primaryType", n.getProperty("jcr:primaryType").getPath());
      assertFalse(ni.hasNext());
   }

   public void testSetPropertyAndMoveNode() throws Exception
   {
      testSetPropertyAndMoveNode(true);
   }

   public void testSetPropertyAndMoveNodeWithoutTriggering() throws Exception
   {
      testSetPropertyAndMoveNode(false);
   }

   /**
    * JCR-1960. Set property and move node. Reveals bug in cache, when actually property with old value
    * has been moved.
    */
   private void testSetPropertyAndMoveNode(boolean triggerEventsForDescendants) throws Exception
   {
      Node rootNode = session.getRootNode();
      Node aNode = rootNode.addNode("foo");
      aNode.setProperty("A", "B");
      session.save();

      rootNode = session.getRootNode();
      aNode = rootNode.getNode("foo");
      aNode.setProperty("A", "C");
      session.move("/foo", "/bar", triggerEventsForDescendants);
      session.save();

      try
      {
         root.getNode("foo");
         fail();
      }
      catch (PathNotFoundException e)
      {
         // ok
      }

      try
      {
         session.getItem("/foo");
         fail();
      }
      catch (PathNotFoundException e)
      {
         // ok
      }

      try
      {
         session.getItem("/foo/jcr:primaryType");
         fail();
      }
      catch (PathNotFoundException e)
      {
         // ok
      }

      try
      {
         session.getItem("/foo/A");
         fail();
      }
      catch (PathNotFoundException e)
      {
         // ok
      }

      assertEquals("C", aNode.getProperty("A").getString());
      assertEquals("/bar", rootNode.getNode("bar").getPath());
      assertEquals("/bar/jcr:primaryType", rootNode.getNode("bar").getProperty("jcr:primaryType").getPath());
      assertEquals("/bar/A", rootNode.getNode("bar").getProperty("A").getPath());
   }
}
