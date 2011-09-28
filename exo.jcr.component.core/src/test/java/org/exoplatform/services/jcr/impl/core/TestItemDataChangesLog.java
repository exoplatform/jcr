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
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.session.SessionChangesLog;

import java.util.Collection;
import java.util.List;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: TestItemDataChangesLog.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestItemDataChangesLog extends JcrImplBaseTest
{

   private TransientNodeData data1;

   private TransientNodeData data2;

   private TransientPropertyData data3;

   public void setUp() throws Exception
   {
      super.setUp();
      QPath path1 = QPath.makeChildPath(Constants.ROOT_PATH, new InternalQName(null, "testBasicOperations1"));
      QPath path2 = QPath.makeChildPath(Constants.ROOT_PATH, new InternalQName(null, "testBasicOperations2"));
      QPath path3 = QPath.makeChildPath(Constants.ROOT_PATH, new InternalQName(null, "testBasicOperations3"));

      data1 =
         new TransientNodeData(path1, "1", 0, new InternalQName(Constants.NS_NT_URI, "unstructured"),
            new InternalQName[0], 0, Constants.ROOT_UUID, new AccessControlList());
      data2 =
         new TransientNodeData(path2, "2", 0, new InternalQName(Constants.NS_NT_URI, "unstructured"),
            new InternalQName[0], 0, Constants.ROOT_UUID, new AccessControlList());
      data3 = new TransientPropertyData(path3, "3", 0, PropertyType.STRING, Constants.ROOT_UUID, false);

   }

   public void testBasicOperations() throws Exception
   {

      SessionChangesLog cLog = new SessionChangesLog(new DummySession("s1"));
      cLog.add(ItemState.createAddedState(data1));
      cLog.add(ItemState.createAddedState(data2));
      cLog.add(ItemState.createDeletedState(data2));
      cLog.add(ItemState.createAddedState(data3));
      cLog.add(ItemState.createUpdatedState(data3));

      assertEquals(5, cLog.getSize());
      assertTrue(cLog.getItemState(data1.getQPath()).isAdded());
      assertTrue(cLog.getItemState(data2.getQPath()).isDeleted());
      assertTrue(cLog.getItemState("1").isAdded());
      assertTrue(cLog.getItemState("2").isDeleted());

      List<ItemState> states = cLog.getItemStates("3");
      assertEquals(2, states.size());
      assertTrue(states.get(0).isAdded());
      assertTrue(states.get(1).isUpdated());
   }

   public void testSessionOperations() throws Exception
   {

      SessionChangesLog cLog = new SessionChangesLog(new DummySession("s1"));

      TransientNodeData d1 =
         TransientNodeData.createNodeData(data1, new InternalQName(null, "testSessionOperations"), new InternalQName(
            Constants.NS_NT_URI, "unstructured"), "d1");

      // test remove
      cLog.add(ItemState.createAddedState(data1));
      cLog.add(ItemState.createAddedState(d1));
      assertEquals(2, cLog.getSize());
      cLog.remove(data1.getQPath());
      assertEquals(0, cLog.getSize());

      // test getChanges
      cLog.add(ItemState.createAddedState(data1));
      cLog.add(ItemState.createAddedState(data2));
      cLog.add(ItemState.createAddedState(d1));
      assertEquals(2, cLog.getDescendantsChanges(data1.getQPath()).size());
      assertEquals(1, cLog.getDescendantsChanges(data2.getQPath()).size());

      // test pushLog
      PlainChangesLog newLog = cLog.pushLog(data1.getQPath());
      assertEquals(2, newLog.getSize());
      assertEquals(1, cLog.getSize());
      cLog.remove(data2.getQPath());

      // test getLastStates
      cLog.add(ItemState.createAddedState(data1));
      cLog.add(ItemState.createAddedState(data2));
      cLog.add(ItemState.createAddedState(d1));

      Collection<ItemState> nodeStates = cLog.getLastChildrenStates(data1, true);
      assertEquals(1, nodeStates.size());
      ItemState s = nodeStates.iterator().next();
      assertTrue(d1.getQPath().equals(s.getData().getQPath()));
      assertTrue(s.isAdded());

      cLog.add(ItemState.createDeletedState(d1));
      nodeStates = cLog.getLastChildrenStates(data1, true);
      s = nodeStates.iterator().next();
      assertEquals(1, nodeStates.size());
      // System.out.println("log   ----- "+);
      assertTrue(s.isDeleted());

   }

   public void testMixinsAddTransient() throws Exception
   {

      String[] mixins = new String[]{"mix:referenceable", "mix:lockable"};
      String[] finalMixins = new String[]{"mix:lockable"};

      NodeImpl testNode = (NodeImpl)session.getRootNode().addNode("mixin_transient_test");
      NodeImpl node1 = (NodeImpl)testNode.addNode("node-1");
      session.save();

      node1.addMixin(mixins[0]);
      node1.addMixin(mixins[1]);

      PropertyImpl uuid = (PropertyImpl)node1.getProperty("jcr:uuid");

      try
      {
         NodeImpl sameNode1 = (NodeImpl)session.getNodeByUUID(uuid.getString());
         checkMixins(mixins, sameNode1);

         assertEquals("Nodes must be same", node1, sameNode1);
      }
      catch (RepositoryException e)
      {
         fail("Transient node must be accessible by uuid. " + e);
      }

      try
      {
         NodeImpl sameNode1 = (NodeImpl)session.getItem(node1.getPath());
         checkMixins(mixins, sameNode1);

         assertEquals("Nodes must be same", node1, sameNode1);
      }
      catch (RepositoryException e)
      {
         fail("Transient node must be accessible by path. " + e);
      }

      testNode.save();

      node1.removeMixin(mixins[0]);
      testNode.save();

      checkMixins(finalMixins, node1);

      // tear down - testNode will be deleted in tearDown()
   }

}
