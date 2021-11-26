/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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

package org.exoplatform.services.jcr.impl.core;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.dataflow.session.SessionChangesLog;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: TestSessionDataManager.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestSessionDataManager extends JcrImplBaseTest
{

   private static String TEST_ROOT = "TestSessionDataManager";

   private SessionDataManager modificationManager;

   private NodeImpl testRoot;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      modificationManager = session.getTransientNodesManager();
      NodeImpl root = (NodeImpl)session.getRootNode();

      TransientNodeData data =
         TransientNodeData.createNodeData(root.nodeData(), new InternalQName(null, TEST_ROOT), new InternalQName(
            Constants.NS_NT_URI, "unstructured"));
      testRoot = (NodeImpl)modificationManager.update(ItemState.createAddedState(data), false);
      TransientPropertyData prop =
         TransientPropertyData.createPropertyData(data, new InternalQName(Constants.NS_JCR_URI, "primaryType"),
            PropertyType.NAME, false, new TransientValueData(new InternalQName(Constants.NS_NT_URI, "unstructured")));
      PropertyImpl prop1 = (PropertyImpl)modificationManager.update(ItemState.createAddedState(prop), false);

      assertEquals(2, modificationManager.getChangesLog().getSize());

      modificationManager.commit(data.getQPath());
      assertEquals(0, modificationManager.getChangesLog().getSize());
   }

   public void testItemReferencePool() throws Exception
   {

      SessionDataManager.ItemReferencePool pool = modificationManager.getItemsPool();
      System.gc();
      Thread.sleep(1000);

      assertEquals(1, pool.size()); // contains root node only

      NodeData parent = (NodeData)testRoot.getData();

      TransientNodeData data =
         TransientNodeData.createNodeData(parent, new InternalQName(null, "testItemReferencePool1"), new InternalQName(
            Constants.NS_NT_URI, "unstructured"));

      String uuid = data.getIdentifier();

      assertEquals(1, pool.size());
      NodeImpl node1 = (NodeImpl)modificationManager.update(ItemState.createAddedState(data), true);
      assertEquals(2, pool.size());
      assertEquals(uuid, node1.getInternalIdentifier());
      assertTrue(pool.contains(uuid));
      // return the same value
      assertEquals(node1, pool.get(node1.getData()));

      // add one more node
      data =
         TransientNodeData.createNodeData(parent, new InternalQName(null, "testItemReferencePool2"), new InternalQName(
            Constants.NS_NT_URI, "unstructured"));

      NodeImpl node2 = (NodeImpl)modificationManager.update(ItemState.createAddedState(data), true);

      List<NodeImpl> nodes = new ArrayList<NodeImpl>();
      nodes.add(node1);
      nodes.add(node2);

      List<NodeImpl> testNodes = getNodes(pool, nodes);
      assertEquals(2, testNodes.size());
      assertEquals(node1, testNodes.get(0));
      assertEquals(node2, testNodes.get(1));

      // ... add property
      TransientPropertyData prop =
         TransientPropertyData.createPropertyData(parent, new InternalQName(null, "testItemReferencePoolProp1"),
            PropertyType.STRING, false);
      PropertyImpl prop1 = (PropertyImpl)modificationManager.update(ItemState.createAddedState(prop), true);
      List<PropertyImpl> props = new ArrayList<PropertyImpl>();
      props.add(prop1);
      List<PropertyImpl> testProps = getProperties(pool, props);
      assertEquals(1, testProps.size());
      assertEquals(prop1, testProps.get(0));

      pool.remove(uuid);
      // in case for GC
      Thread.sleep(1000);
      // 1 node and 1 prop
      if (log.isDebugEnabled())
         log.debug(" >>>>> >>>> > " + modificationManager.dump());

      // /TestSessionDataManager/testItemReferencePoolProp1
      // /TestSessionDataManager
      // /TestSessionDataManager/testItemReferencePool2

      assertEquals(3, pool.size());
   }

   private List<NodeImpl> getNodes(SessionDataManager.ItemReferencePool pool, List<NodeImpl> nodes)
      throws RepositoryException
   {
      List<ItemImpl> items = (List<ItemImpl>)pool.getAll();
      List<NodeImpl> children = new ArrayList<NodeImpl>();
      for (NodeImpl node : nodes)
      {
         String id = node.getInternalIdentifier();

         NodeImpl pooled = null;
         for (ItemImpl item : items)
         {
            if (items.get(0).getData().getIdentifier().equals(id))
            {
               pooled = (NodeImpl)item;
               break;
            }
         }

         if (pooled == null)
         {
            children.add(node);
         }
         else
         {
            pooled.loadData(node.getData());
            children.add(pooled);
         }
      }
      return children;
   }

   private List<PropertyImpl> getProperties(SessionDataManager.ItemReferencePool pool, List<PropertyImpl> props)
      throws RepositoryException
   {
      List<ItemImpl> items = (List<ItemImpl>)pool.getAll();
      List<PropertyImpl> children = new ArrayList<PropertyImpl>();
      for (PropertyImpl prop : props)
      {
         String id = prop.getInternalIdentifier();

         PropertyImpl pooled = null;
         for (ItemImpl item : items)
         {
            if (items.get(0).getData().getIdentifier().equals(id))
            {
               pooled = (PropertyImpl)item;
               break;
            }
         }

         if (pooled == null)
         {
            children.add(prop);
         }
         else
         {
            pooled.loadData(prop.getData());
            children.add(pooled);
         }
      }
      return children;
   }

   public void testSessionChangesLog() throws Exception
   {
      SessionChangesLog changesLog = modificationManager.getChangesLog();

      assertEquals(0, changesLog.getAllStates().size());

      NodeData parent = (NodeData)testRoot.getData();

      TransientNodeData data =
         TransientNodeData.createNodeData(parent, new InternalQName(null, "testSessionChangesLogN1"),
            new InternalQName(Constants.NS_NT_URI, "unstructured"));

      NodeImpl node1 = (NodeImpl)modificationManager.update(ItemState.createAddedState(data), true);
      assertEquals(1, changesLog.getAllStates().size());
      assertNotNull(changesLog.getItemState(node1.getInternalIdentifier()));
      assertNotNull(changesLog.getItemState(node1.getInternalPath()));
      assertTrue(changesLog.getItemState(node1.getInternalIdentifier()).isAdded());

      // delete this node ... state should be DELETED
      modificationManager.delete(data);
      assertEquals(2, changesLog.getAllStates().size());
      List<ItemState> lst = changesLog.getItemStates(data.getIdentifier());
      assertEquals(2, lst.size());
      assertTrue(changesLog.getItemState(data.getIdentifier()).isDeleted());

      // add the same node ... state should be ADDED again
      node1 = (NodeImpl)modificationManager.update(ItemState.createAddedState(data), true);
      assertEquals(3, changesLog.getItemStates(data.getIdentifier()).size());
      assertTrue(changesLog.getItemState(data.getIdentifier()).isAdded());

      assertEquals(3, changesLog.getAllStates().size());

      // ... add property to the node1
      TransientPropertyData prop =
         TransientPropertyData.createPropertyData((NodeData)node1.getData(), new InternalQName(null,
            "testSessionChangesLogP1"), PropertyType.STRING, false, new TransientValueData(false));
      PropertyImpl prop1 = (PropertyImpl)modificationManager.update(ItemState.createAddedState(prop), true);

      assertTrue(changesLog.getItemState(node1.getInternalIdentifier()).isAdded());
      assertTrue(changesLog.getItemState(prop.getIdentifier()).isAdded());

      assertEquals(4, changesLog.getAllStates().size());

      // // 4 changes: 3 for node1, and 1- for prop1
      assertEquals(4, changesLog.getDescendantsChanges(node1.getInternalPath()).size());

   }

   public void testReadMethods() throws Exception
   {

      NodeData parent = (NodeData)testRoot.getData();
      TransientNodeData someData =
         TransientNodeData.createNodeData(parent, new InternalQName(null, "testReadMethodsN1"), new InternalQName(
            Constants.NS_NT_URI, "unstructured"));

      // add one more node
      TransientNodeData data =
         TransientNodeData.createNodeData(parent, new InternalQName(null, "testReadMethodsN2"), new InternalQName(
            Constants.NS_NT_URI, "unstructured"));
      NodeImpl node2 = (NodeImpl)modificationManager.update(ItemState.createAddedState(data), true);

      // ... add property
      TransientPropertyData prop =
         TransientPropertyData.createPropertyData(parent, new InternalQName(null, "testReadMethodsP1"),
            PropertyType.STRING, false, new TransientValueData(false));
      PropertyImpl prop1 = (PropertyImpl)modificationManager.update(ItemState.createAddedState(prop), true);

      assertNotNull(modificationManager.getItemData(data.getQPath()));
      assertEquals(data.getIdentifier(), modificationManager.getItemData(data.getQPath()).getIdentifier());

      assertEquals(node2, modificationManager.getItem(data.getQPath(), true));

      assertEquals(prop.getIdentifier(), modificationManager.getItemData(prop.getIdentifier()).getIdentifier());
      assertEquals(prop1, modificationManager.getItemByIdentifier(prop.getIdentifier(), true));

      assertTrue(modificationManager.hasPendingChanges(data.getQPath()));
      assertFalse(modificationManager.hasPendingChanges(someData.getQPath()));

      assertTrue(modificationManager.isNew(prop.getIdentifier()));
      assertFalse(modificationManager.isNew(parent.getIdentifier()));

      assertFalse(modificationManager.isModified(prop));
      modificationManager.update(ItemState.createUpdatedState(prop), true);
      assertTrue(modificationManager.isModified(prop));

      assertEquals(1, modificationManager.getChildNodesData(parent).size());
      assertEquals(2, modificationManager.getChildPropertiesData(parent).size());

      assertEquals(1, modificationManager.getChildNodesData(parent).size());
      assertEquals(2, modificationManager.getChildPropertiesData(parent).size());
   }

   public void testCommitAndRefresh() throws Exception
   {

      NodeData parent = (NodeData)testRoot.getData();
      TransientNodeData data1 =
         TransientNodeData.createNodeData(parent, new InternalQName(null, "testCommitAndRefreshN1"), new InternalQName(
            Constants.NS_NT_URI, "unstructured"));
      NodeImpl node1 = (NodeImpl)modificationManager.update(ItemState.createAddedState(data1), true);
      TransientPropertyData nt =
         TransientPropertyData.createPropertyData(data1, new InternalQName(Constants.NS_JCR_URI, "primaryType"),
            PropertyType.NAME, false, new TransientValueData(new InternalQName(Constants.NS_NT_URI, "unstructured")));
      modificationManager.update(ItemState.createAddedState(nt), true);
      assertEquals(1, modificationManager.getChildPropertiesData(data1).size());

      modificationManager.commit(data1.getQPath());

      // make sure changes are saved
      assertEquals(0, modificationManager.getChangesLog().getSize());

      assertNotNull(modificationManager.getItem(data1.getQPath(), true));
      
      assertEquals(1, modificationManager.getChildPropertiesData(parent).size());

      // ... add property
      TransientPropertyData prop =
         TransientPropertyData.createPropertyData(parent, new InternalQName(null, "testCommitAndRefreshP1"),
            PropertyType.STRING, false, new TransientValueData("test"));
      modificationManager.update(ItemState.createAddedState(prop), true);
      assertEquals("test", ((PropertyImpl)modificationManager.getItem(prop.getQPath(), true)).getString());
   }

   @SuppressWarnings("deprecation")
   public void testGetItem() throws RepositoryException, IllegalNameException
   {
      // get non-existent data by getItemData(NodeData parent, QPathEntry name)
      assertNull(modificationManager.getItemData((NodeData)((NodeImpl)root).getData(), new QPathEntry("",
         "testgetitemNode", 0), ItemType.NODE));
      // get non-existent data by ItemData getItemData(QPath path)
      assertNull(modificationManager.getItemData(QPath.makeChildPath(((NodeImpl)root).getData().getQPath(),
         new InternalQName("", "testgetitemNode"))));

      NodeImpl testNode = (NodeImpl)root.addNode("testgetitemNode");

      // get data by getItemData(NodeData parent, QPathEntry name)
      assertNotNull(modificationManager.getItemData((NodeData)((NodeImpl)root).getData(), new QPathEntry("",
         "testgetitemNode", 0), ItemType.NODE));
      // get data by ItemData getItemData(QPath path)
      assertNotNull(modificationManager.getItemData(QPath.makeChildPath(((NodeImpl)root).getData().getQPath(),
         new InternalQName("", "testgetitemNode"))));
   }

   public void testIsDeleted() throws ItemExistsException, PathNotFoundException, VersionException,
      ConstraintViolationException, LockException, RepositoryException
   {
      NodeImpl testNode = (NodeImpl)root.addNode("testNode");
      String identifier = testNode.getIdentifier();
      QPath itemPath = testNode.getData().getQPath();

      assertFalse(modificationManager.isDeleted(identifier));
      assertFalse(modificationManager.isDeleted(itemPath));

      testNode.remove();
      assertTrue(modificationManager.isDeleted(identifier));
      assertTrue(modificationManager.isDeleted(itemPath));
   }

   public void testGetACLWhenNodeIsRoot() throws RepositoryException
   {
      NodeImpl rootNode = (NodeImpl)root;
      assertEquals(rootNode.getACL(), modificationManager.getACL(rootNode.getData().getQPath()));
   }
   
   public void testUpdateWhenStateIsDeleted() throws ItemExistsException, PathNotFoundException, VersionException,
      ConstraintViolationException, LockException, RepositoryException
   {
      NodeImpl testNode = (NodeImpl)root.addNode("testNode");
      ItemState state = ItemState.createDeletedState(testNode.getData());

      try
      {
         modificationManager.update(state, false);
         fail();
      }
      catch (RepositoryException e)
      {
      }
   }

   public void testUpdateStateWhenStateIsDeleted() throws ItemExistsException, PathNotFoundException, VersionException,
      ConstraintViolationException, LockException, RepositoryException
   {
      NodeImpl testNode = (NodeImpl)root.addNode("testNode");
      ItemState state = ItemState.createDeletedState(testNode.getData());

      try
      {
         modificationManager.updateItemState(state);
         fail();
      }
      catch (RepositoryException e)
      {
      }
   }

   public void testRefreshWhenNodeHasModifiedStatus() throws ItemExistsException, PathNotFoundException,
      VersionException, ConstraintViolationException, LockException, RepositoryException
   {
      NodeImpl testNode = (NodeImpl)root.addNode("testNode");

      try
      {
         modificationManager.refresh(testNode.getData());
         fail();
      }
      catch (InvalidItemStateException e)
      {
      }
   }

   /*
    * This test is testing problem described in JCR-1864.
    * We add 2 node to the system node and we check lastOrderNum for system node.
    * Last orderNum from method SessionDataManager.getLastOrderNumber must be equal orderNum from last added node. 
    */
   public void testGetLastOrderNumFromSystemNode() throws RepositoryException
   {
      SessionImpl session2 = (SessionImpl)repository.login(credentials, "ws1");
      SessionDataManager dataManager = session2.getTransientNodesManager();

      Node systemNode = session2.getRootNode().getNode("jcr:system");
      systemNode.addNode("node1");
      NodeImpl node2 = (NodeImpl)systemNode.addNode("node2");

      session2.save();

      int lastOrderNum = dataManager.getLastOrderNumber((NodeData)dataManager.getItemData(Constants.SYSTEM_UUID));
      int orderNum = ((NodeData)dataManager.getItemData(node2.getIdentifier())).getOrderNumber();

      assertEquals(lastOrderNum, orderNum);
   }
}