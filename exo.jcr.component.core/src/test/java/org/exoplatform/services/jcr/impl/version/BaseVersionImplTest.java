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
package org.exoplatform.services.jcr.impl.version;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.SessionDataManagerTestWrapper;
import org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeManagerImpl;
import org.exoplatform.services.jcr.impl.core.version.FrozenNodeInitializer;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.dataflow.session.SessionChangesLog;
import org.exoplatform.services.jcr.util.IdGenerator;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.PropertyType;

/**
 * Created by The eXo Platform SAS 07.12.2006
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter
 *         Nedonosko</a>
 * @version $Id: BaseVersionImplTest.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class BaseVersionImplTest extends JcrImplBaseTest
{

   protected static final int TESTCASE_ONPARENT_COPY = 0;

   protected static final int TESTCASE_ONPARENT_ABORT = 1;

   protected static final int TESTCASE_ONPARENT_IGNORE = 2;

   protected static final int TESTCASE_ONPARENT_VERSION = 3;

   protected String NS_TEST_IMPL_URI = "http://www.exoplatform.org/jcr/test/1.0";

   protected InternalQName NT_TEST_ONPARENT_ABORT = new InternalQName(NS_TEST_IMPL_URI, "onParentAbort");

   protected InternalQName PROPERTY_ABORTED = new InternalQName(NS_TEST_IMPL_URI, "abortedProperty");

   protected InternalQName NODE_ABOORTED = new InternalQName(NS_TEST_IMPL_URI, "abortedNode");

   protected InternalQName NT_TEST_ONPARENT_IGNORE = new InternalQName(NS_TEST_IMPL_URI, "onParentIgnore");

   protected InternalQName PROPERTY_IGNORED = new InternalQName(NS_TEST_IMPL_URI, "ignoredProperty");

   protected InternalQName NODE_IGNORED = new InternalQName(NS_TEST_IMPL_URI, "ignoredNode");

   protected InternalQName NT_TEST_ONPARENT_VERSION = new InternalQName(NS_TEST_IMPL_URI, "onParentVersion");

   protected InternalQName PROPERTY_VERSIONED = new InternalQName(NS_TEST_IMPL_URI, "versionedProperty");

   protected InternalQName NODE_VERSIONED = new InternalQName(NS_TEST_IMPL_URI, "versionedNode");

   protected static String TEST_ROOT = "testRoot";

   protected static String TEST_NODE = "versionableNode";

   protected static String TEST_FROZEN_ROOT = "frozenRoot";

   protected InternalQName[] mixVersionable = new InternalQName[]{Constants.MIX_VERSIONABLE};

   protected InternalQName testRootName = new InternalQName(Constants.NS_EXO_URI, TEST_ROOT);

   protected InternalQName frozenRootName = new InternalQName(Constants.NS_EXO_URI, TEST_FROZEN_ROOT);

   protected InternalQName nodeName1 = new InternalQName(Constants.NS_EXO_URI, "node 1");

   protected InternalQName nodeName2 = new InternalQName(Constants.NS_EXO_URI, "node 2");

   protected InternalQName nodeName3 = new InternalQName(Constants.NS_EXO_URI, "node 3");

   protected InternalQName nodeName4 = new InternalQName(Constants.NS_EXO_URI, "node 4");

   protected InternalQName nodeName5 = new InternalQName(Constants.NS_EXO_URI, "node 5");

   protected InternalQName propertyName1 = new InternalQName(Constants.NS_EXO_URI, "property 1");

   protected InternalQName propertyName2 = new InternalQName(Constants.NS_EXO_URI, "property 2");

   protected InternalQName propertyName3 = new InternalQName(Constants.NS_EXO_URI, "property 3");

   protected InternalQName propertyName4 = new InternalQName(Constants.NS_EXO_URI, "property 4");

   protected InternalQName propertyName5 = new InternalQName(Constants.NS_EXO_URI, "property 5");

   protected InternalQName propertyName6 = new InternalQName(Constants.NS_EXO_URI, "property 6");

   protected List<ValueData> stringDataMultivalued;

   protected List<ValueData> stringDataSinglevalued;

   protected List<ValueData> longDataSinglevalued;

   protected List<ValueData> binaryDataSinglevalued;

   protected List<ValueData> versionedVersionHistoryData;

   // protected InternalQPath nodePath3 =
   // InternalQPath.makeChildPath(Constants.ROOT_PATH, new
   // InternalQName(Constants.NS_EXO_URI,"node 3"));

   protected String rootUuid;

   protected String frozenUuid;

   protected String versionedVersionHistoryUuid; // used

   // for
   // OnParentCopy
   // =
   // VERSION
   // test

   protected String nodeUuid1;

   protected String nodeUuid2;

   protected String nodeUuid3;

   protected String nodeUuid31;

   protected String nodeUuid32;

   protected String propertyUuid11;

   protected String propertyUuid12;

   protected String propertyUuid21;

   protected String propertyUuid22;

   protected String propertyUuid311;

   protected String propertyUuid312;

   protected NodeData testRoot;

   protected NodeData frozenRoot;

   protected NodeData versionable;

   protected SessionChangesLog changesLog;

   protected SessionChangesLog versionableLog;

   protected NodeTypeManagerImpl ntManager;

   protected FrozenNodeInitializer visitor;

   protected class TestTransientValueData extends TransientValueData
   {
      protected TestTransientValueData(byte[] data, int orderNumb)
      {
         super(orderNumb, data);
      }
   }

   public void setUp() throws Exception
   {
      super.setUp();

      rootUuid = IdGenerator.generate();

      frozenUuid = IdGenerator.generate();

      versionedVersionHistoryUuid = IdGenerator.generate();

      nodeUuid1 = IdGenerator.generate();
      nodeUuid2 = IdGenerator.generate();
      nodeUuid3 = IdGenerator.generate();
      nodeUuid31 = IdGenerator.generate();
      nodeUuid32 = IdGenerator.generate();

      propertyUuid11 = IdGenerator.generate();
      propertyUuid12 = IdGenerator.generate();
      propertyUuid21 = IdGenerator.generate();
      propertyUuid22 = IdGenerator.generate();

      propertyUuid311 = IdGenerator.generate();
      propertyUuid312 = IdGenerator.generate();

      stringDataMultivalued = new ArrayList<ValueData>();
      stringDataMultivalued.add(new TestTransientValueData("property data 1".getBytes(), 0));
      stringDataMultivalued.add(new TestTransientValueData("property data 2".getBytes(), 1));
      stringDataMultivalued.add(new TestTransientValueData("property data 3".getBytes(), 2));

      stringDataSinglevalued = new ArrayList<ValueData>();
      stringDataSinglevalued.add(new TestTransientValueData("property data".getBytes(), 0));

      longDataSinglevalued = new ArrayList<ValueData>();
      longDataSinglevalued.add(new TestTransientValueData(new Long(123456l).toString().getBytes(), 0));

      binaryDataSinglevalued = new ArrayList<ValueData>();
      binaryDataSinglevalued.add(new TestTransientValueData("property binary data".getBytes(), 0));

      versionedVersionHistoryData = new ArrayList<ValueData>();
      versionedVersionHistoryData.add(new TestTransientValueData(versionedVersionHistoryUuid.getBytes(), 0));

      changesLog = new SessionChangesLog(session.getId());

      ntManager = (NodeTypeManagerImpl)session.getWorkspace().getNodeTypeManager();

      SessionChangesLog initChanges = new SessionChangesLog(session.getId());

      NodeData wsRoot = (NodeData)((NodeImpl)session.getRootNode()).getData();
      testRoot =
         TransientNodeData.createNodeData(wsRoot, new InternalQName(null, TEST_ROOT), Constants.NT_UNSTRUCTURED);
      initChanges.add(ItemState.createAddedState(testRoot));

      frozenRoot = TransientNodeData.createNodeData(testRoot, frozenRootName, Constants.NT_UNSTRUCTURED);
      initChanges.add(ItemState.createAddedState(frozenRoot));

      // session.getTransientNodesManager().getTransactManager().save(changesLog);
      SessionDataManagerTestWrapper managerWrapper =
         new SessionDataManagerTestWrapper(session.getTransientNodesManager());
      managerWrapper.getChangesLog().addAll(initChanges.getAllStates());

      visitor =
         new FrozenNodeInitializer(frozenRoot, session.getTransientNodesManager(), session.getWorkspace()
            .getNodeTypesHolder(),

         changesLog, session.getValueFactory());
   }

   public void tearDown() throws Exception
   {
      changesLog.clear();

      session.refresh(false);

      super.tearDown();
   }

   /**
    * Prepare in session log: /testRoot/node 1 /testRoot/node 1/property 1
    * /testRoot/node 1/property 2 /testRoot/node 2 /testRoot/node 2/property 3
    * /testRoot/node 2/property 4 /testRoot/node 2/node 3 /testRoot/node 2/node
    * 3/property 5
    */
   public void createVersionable(int testCase) throws Exception
   {

      versionableLog = new SessionChangesLog(session.getId());

      // target node
      versionable =
         TransientNodeData.createNodeData(testRoot, nodeName1, Constants.NT_UNSTRUCTURED, mixVersionable, nodeUuid1);
      versionableLog.add(ItemState.createAddedState(versionable));

      PropertyData vChildProperty1 =
         TransientPropertyData.createPropertyData(versionable, propertyName1, 0, true, stringDataMultivalued);
      versionableLog.add(ItemState.createAddedState(vChildProperty1));
      PropertyData vChildProperty2 =
         TransientPropertyData.createPropertyData(versionable, propertyName2, 0, false, longDataSinglevalued);
      versionableLog.add(ItemState.createAddedState(vChildProperty2));

      NodeData vChildNode1 = TransientNodeData.createNodeData(versionable, nodeName2, Constants.NT_UNSTRUCTURED);
      versionableLog.add(ItemState.createAddedState(vChildNode1));
      PropertyData vChildNode1_property1 =
         TransientPropertyData.createPropertyData(vChildNode1, propertyName3, 0, false, stringDataSinglevalued);
      versionableLog.add(ItemState.createAddedState(vChildNode1_property1));
      PropertyData vChildNode1_property2 =
         TransientPropertyData.createPropertyData(vChildNode1, propertyName4, 0, false, binaryDataSinglevalued);
      versionableLog.add(ItemState.createAddedState(vChildNode1_property2));

      NodeData vChildNode1_node3 = TransientNodeData.createNodeData(vChildNode1, nodeName3, Constants.NT_UNSTRUCTURED);
      versionableLog.add(ItemState.createAddedState(vChildNode1_node3));
      PropertyData vChildNode1_node3_property1 =
         TransientPropertyData.createPropertyData(vChildNode1_node3, propertyName5, 0, false, stringDataSinglevalued);
      versionableLog.add(ItemState.createAddedState(vChildNode1_node3_property1));

      NodeData vChildNode1_node4 = TransientNodeData.createNodeData(vChildNode1, nodeName4, Constants.NT_UNSTRUCTURED);
      versionableLog.add(ItemState.createAddedState(vChildNode1_node4));
      PropertyData vChildNode1_node4_property1 =
         TransientPropertyData.createPropertyData(vChildNode1_node4, propertyName1, 0, false, stringDataMultivalued);
      versionableLog.add(ItemState.createAddedState(vChildNode1_node4_property1));

      switch (testCase)
      {
         case (TESTCASE_ONPARENT_ABORT) : {
            NodeData vChildNode1_node5 =
               TransientNodeData.createNodeData(vChildNode1, nodeName5, NT_TEST_ONPARENT_ABORT);
            versionableLog.add(ItemState.createAddedState(vChildNode1_node5));
            PropertyData vChildNode1_node5_propertyAborted =
               TransientPropertyData.createPropertyData(vChildNode1_node5, PROPERTY_ABORTED, 0, false,
                  stringDataSinglevalued);
            versionableLog.add(ItemState.createAddedState(vChildNode1_node5_propertyAborted));
            NodeData vChildNode1_node5_nodeAborted =
               TransientNodeData.createNodeData(vChildNode1_node5, NODE_ABOORTED, Constants.NT_UNSTRUCTURED);
            versionableLog.add(ItemState.createAddedState(vChildNode1_node5_nodeAborted));
            PropertyData vChildNode1_node5_node1_property2 =
               TransientPropertyData.createPropertyData(vChildNode1_node5_nodeAborted, propertyName2, 0, false,
                  stringDataSinglevalued);
            versionableLog.add(ItemState.createAddedState(vChildNode1_node5_node1_property2));
            break;
         }

         case (TESTCASE_ONPARENT_IGNORE) : {
            NodeData vChildNode1_node5 =
               TransientNodeData.createNodeData(vChildNode1, nodeName5, NT_TEST_ONPARENT_IGNORE);
            versionableLog.add(ItemState.createAddedState(vChildNode1_node5));
            PropertyData vChildNode1_node5_propertyIgnored =
               TransientPropertyData.createPropertyData(vChildNode1_node5, PROPERTY_IGNORED, 0, false,
                  stringDataSinglevalued);
            versionableLog.add(ItemState.createAddedState(vChildNode1_node5_propertyIgnored));
            NodeData vChildNode1_node5_nodeIgnored =
               TransientNodeData.createNodeData(vChildNode1_node5, NODE_IGNORED, Constants.NT_UNSTRUCTURED);
            versionableLog.add(ItemState.createAddedState(vChildNode1_node5_nodeIgnored));
            PropertyData vChildNode1_node5_node1_property2 =
               TransientPropertyData.createPropertyData(vChildNode1_node5_nodeIgnored, propertyName2, 0, false,
                  stringDataSinglevalued);
            versionableLog.add(ItemState.createAddedState(vChildNode1_node5_node1_property2));
            break;
         }

         case (TESTCASE_ONPARENT_VERSION) : {
            NodeData vChildNode1_node5 =
               TransientNodeData.createNodeData(vChildNode1, nodeName5, NT_TEST_ONPARENT_VERSION);
            versionableLog.add(ItemState.createAddedState(vChildNode1_node5));
            PropertyData vChildNode1_node5_propertyVersioned =
               TransientPropertyData.createPropertyData(vChildNode1_node5, PROPERTY_VERSIONED, 0, false,
                  stringDataSinglevalued); // behaviour
            // of
            // COPY
            // will
            // be
            versionableLog.add(ItemState.createAddedState(vChildNode1_node5_propertyVersioned));

            NodeData vChildNode1_node5_nodeVersioned =
               TransientNodeData.createNodeData(vChildNode1_node5, NODE_VERSIONED, Constants.NT_UNSTRUCTURED,
                  mixVersionable);
            versionableLog.add(ItemState.createAddedState(vChildNode1_node5_nodeVersioned));
            PropertyData vChildNode1_node5_node1_property2 =
               TransientPropertyData.createPropertyData(vChildNode1_node5_nodeVersioned, propertyName2, 0, false,
                  stringDataSinglevalued);
            versionableLog.add(ItemState.createAddedState(vChildNode1_node5_node1_property2));

            // version history for store in jcr:childVersionHistory property during
            // visitor work
            PropertyData vChildNode1_node5_nodeVersioned_versionHistory =
               TransientPropertyData.createPropertyData(vChildNode1_node5_nodeVersioned, Constants.JCR_VERSIONHISTORY,
                  PropertyType.REFERENCE, false, versionedVersionHistoryData);
            versionableLog.add(ItemState.createAddedState(vChildNode1_node5_nodeVersioned_versionHistory));
            break;
         }
      }

      SessionDataManagerTestWrapper managerWrapper =
         new SessionDataManagerTestWrapper(session.getTransientNodesManager());
      managerWrapper.getChangesLog().addAll(versionableLog.getAllStates());
   }
}
