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
package org.exoplatform.services.jcr.impl.dataflow.persistent;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedNodeData;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedPropertyData;
import org.exoplatform.services.jcr.dataflow.persistent.WorkspaceStorageCache;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.util.IdGenerator;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: WorkspaceStorageCacheBaseCase.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public abstract class WorkspaceStorageCacheBaseCase extends JcrImplBaseTest
{

   private QPath nodePath1 =
      QPath.makeChildPath(Constants.ROOT_PATH, new InternalQName(Constants.NS_EXO_PREFIX, "node 1"));

   private QPath nodePath2 =
      QPath.makeChildPath(Constants.ROOT_PATH, new InternalQName(Constants.NS_EXO_PREFIX, "node 2"));

   private QPath nodePath21 = QPath.makeChildPath(nodePath2, new InternalQName(Constants.NS_EXO_PREFIX, "node 2.1"));

   private QPath nodePath22 = QPath.makeChildPath(nodePath2, new InternalQName(Constants.NS_EXO_PREFIX, "node 2.2"));

   private QPath nodePath3 =
      QPath.makeChildPath(Constants.ROOT_PATH, new InternalQName(Constants.NS_EXO_PREFIX, "node 3"));

   private QPath nodePath31 = QPath.makeChildPath(nodePath3, new InternalQName(Constants.NS_EXO_PREFIX, "node 3.1"));

   private QPath nodePath32 = QPath.makeChildPath(nodePath3, new InternalQName(Constants.NS_EXO_PREFIX, "node 3.2"));

   private QPath propertyPath11 =
      QPath.makeChildPath(nodePath1, new InternalQName(Constants.NS_EXO_PREFIX, "property 1.1"));

   private QPath propertyPath12 =
      QPath.makeChildPath(nodePath1, new InternalQName(Constants.NS_EXO_PREFIX, "property 1.2"));

   private QPath propertyPath21 =
      QPath.makeChildPath(nodePath2, new InternalQName(Constants.NS_EXO_PREFIX, "property 2.1"));

   private QPath propertyPath22 =
      QPath.makeChildPath(nodePath2, new InternalQName(Constants.NS_EXO_PREFIX, "property 2.2"));

   private QPath propertyPath311 =
      QPath.makeChildPath(nodePath31, new InternalQName(Constants.NS_EXO_PREFIX, "property 3.1.1"));

   private QPath propertyPath312 =
      QPath.makeChildPath(nodePath31, new InternalQName(Constants.NS_EXO_PREFIX, "property 3.1.2"));

   private String rootUuid;

   private String nodeUuid1;

   private String nodeUuid2;

   private String nodeUuid21;

   private String nodeUuid22;

   private String nodeUuid3;

   private String nodeUuid31;

   private String nodeUuid32;

   private String propertyUuid11;

   private String propertyUuid12;

   private String propertyUuid21;

   private String propertyUuid22;

   private String propertyUuid311;

   private String propertyUuid312;

   private NodeData nodeData1;

   private NodeData nodeData2;

   private NodeData nodeData21;

   private NodeData nodeData22;

   private NodeData nodeData3;

   private NodeData nodeData31;

   private NodeData nodeData32;

   private PropertyData propertyData11;

   private PropertyData propertyData12;

   private PropertyData propertyData21;

   private PropertyData propertyData22;

   private PropertyData propertyData311;

   private PropertyData propertyData312;

   private WorkspaceStorageCache cache;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      rootUuid = IdGenerator.generate();

      nodeUuid1 = IdGenerator.generate();
      nodeUuid2 = IdGenerator.generate();
      nodeUuid21 = IdGenerator.generate();
      nodeUuid22 = IdGenerator.generate();
      nodeUuid3 = IdGenerator.generate();
      nodeUuid31 = IdGenerator.generate();
      nodeUuid32 = IdGenerator.generate();

      propertyUuid11 = IdGenerator.generate();
      propertyUuid12 = IdGenerator.generate();
      propertyUuid21 = IdGenerator.generate();
      propertyUuid22 = IdGenerator.generate();

      propertyUuid311 = IdGenerator.generate();
      propertyUuid312 = IdGenerator.generate();

      WorkspaceStorageCache cacheProbe =
         (WorkspaceStorageCache)session.getContainer().getComponentInstanceOfType(WorkspaceStorageCache.class);
      assertNotNull("Cache is unaccessible (check access denied or configuration)", cacheProbe);
      assertTrue("Cache is disabled", cacheProbe.isEnabled());

      cache = getCacheImpl();
      assertNotNull("Cache is disabled ", cache);
   }

   public abstract WorkspaceStorageCache getCacheImpl() throws Exception;

   private void initNodesData() throws RepositoryException
   {

      AccessControlList acl = ((NodeImpl)root).getACL();

      nodeData1 = new PersistedNodeData(nodeUuid1, nodePath1, rootUuid, 1, 0, Constants.NT_UNSTRUCTURED, null, acl);

      nodeData2 = new PersistedNodeData(nodeUuid2, nodePath2, rootUuid, 1, 1, Constants.NT_UNSTRUCTURED, null, acl);
      nodeData21 = new PersistedNodeData(nodeUuid21, nodePath21, nodeUuid2, 1, 0, Constants.NT_UNSTRUCTURED, null, acl);
      nodeData22 = new PersistedNodeData(nodeUuid22, nodePath22, nodeUuid2, 1, 1, Constants.NT_UNSTRUCTURED, null, acl);

      nodeData3 = new PersistedNodeData(nodeUuid3, nodePath3, rootUuid, 1, 2, Constants.NT_UNSTRUCTURED, null, acl);
      nodeData31 = new PersistedNodeData(nodeUuid31, nodePath31, nodeUuid3, 1, 0, Constants.NT_UNSTRUCTURED, null, acl);
      nodeData32 = new PersistedNodeData(nodeUuid32, nodePath32, nodeUuid3, 1, 1, Constants.NT_UNSTRUCTURED, null, acl);
   }

   private void initDataAsPersisted()
   {
      List<ValueData> stringData = new ArrayList<ValueData>();
      stringData.add(new ByteArrayPersistedValueData(0, "property data 1".getBytes()));
      stringData.add(new ByteArrayPersistedValueData(1, "property data 2".getBytes()));
      stringData.add(new ByteArrayPersistedValueData(2, "property data 3".getBytes()));
      propertyData11 =
         new PersistedPropertyData(propertyUuid11, propertyPath11, nodeUuid1, 1, PropertyType.STRING, false, stringData);

      List<ValueData> binData = new ArrayList<ValueData>();
      binData.add(new ByteArrayPersistedValueData(0, "property data bin 1".getBytes()));
      propertyData12 =
         new PersistedPropertyData(propertyUuid12, propertyPath12, nodeUuid1, 1, PropertyType.BINARY, false, binData);

      List<ValueData> stringData1 = new ArrayList<ValueData>();
      stringData1.add(new ByteArrayPersistedValueData(0, "property data 1".getBytes()));
      stringData1.add(new ByteArrayPersistedValueData(1, "property data 2".getBytes()));
      stringData1.add(new ByteArrayPersistedValueData(2, "property data 3".getBytes()));
      propertyData21 =
         new PersistedPropertyData(propertyUuid21, propertyPath21, nodeUuid2, 1, PropertyType.STRING, true, stringData);

      List<ValueData> booleanData = new ArrayList<ValueData>();
      booleanData.add(new ByteArrayPersistedValueData(0, "true".getBytes()));
      propertyData22 =
         new PersistedPropertyData(propertyUuid22, propertyPath22, nodeUuid2, 1, PropertyType.BOOLEAN, false,
            booleanData);

      List<ValueData> longData = new ArrayList<ValueData>();
      longData.add(new ByteArrayPersistedValueData(0, new Long(123456).toString().getBytes()));
      propertyData311 =
         new PersistedPropertyData(propertyUuid311, propertyPath311, nodeUuid31, 1, PropertyType.LONG, false, longData);

      List<ValueData> refData = new ArrayList<ValueData>();
      refData.add(new ByteArrayPersistedValueData(0, nodeUuid1.getBytes()));
      refData.add(new ByteArrayPersistedValueData(1, nodeUuid2.getBytes()));
      refData.add(new ByteArrayPersistedValueData(2, nodeUuid3.getBytes()));
      propertyData312 =
         new PersistedPropertyData(propertyUuid312, propertyPath312, nodeUuid31, 1, PropertyType.REFERENCE, true,
            refData);
   }

   public void testGetItem_Persisted() throws Exception
   {

      initNodesData();
      initDataAsPersisted();

      try
      {
         cache.beginTransaction();
         cache.put(nodeData1);
         cache.put(nodeData2);
         cache.put(propertyData12);
         cache.commitTransaction();
      }
      catch (Exception e)
      {
         cache.rollbackTransaction();
         throw e;
      }
      assertEquals("Cached node " + nodeData1.getQPath().getAsString() + " is not equals", cache.get(rootUuid,
         nodePath1.getEntries()[nodePath1.getEntries().length - 1], ItemType.NODE), nodeData1);
      assertEquals("Cached node " + nodeData2.getQPath().getAsString() + " is not equals", cache.get(rootUuid,
         nodePath2.getEntries()[nodePath2.getEntries().length - 1], ItemType.NODE), nodeData2);

      assertEquals("Cached node " + nodeData1.getIdentifier() + " is not equals", cache.get(nodeUuid1), nodeData1);
      assertEquals("Cached node " + nodeData2.getIdentifier() + " is not equals", cache.get(nodeUuid2), nodeData2);

      assertEquals("Cached property " + propertyPath12.getAsString() + " is not equals", cache.get(nodeUuid1,
         propertyPath12.getEntries()[propertyPath12.getEntries().length - 1], ItemType.PROPERTY), propertyData12);
      assertEquals("Cached property " + propertyData12.getIdentifier() + " is not equals", cache.get(propertyUuid12),
         propertyData12);
   }

   public void testGetItems_Persisted() throws Exception
   {

      initNodesData();
      initDataAsPersisted();

      List<NodeData> nodes = new ArrayList<NodeData>();
      nodes.add(nodeData31);
      nodes.add(nodeData32);
      try
      {
         cache.beginTransaction();
         cache.addChildNodes(nodeData3, nodes);

         cache.put(nodeData1);
         cache.put(nodeData2);
         cache.put(propertyData12);

         List<PropertyData> properties2 = new ArrayList<PropertyData>();
         properties2.add(propertyData21);
         properties2.add(propertyData22);
         cache.addChildProperties(nodeData2, properties2);

         List<PropertyData> properties1 = new ArrayList<PropertyData>();
         properties1.add(propertyData11);
         properties1.add(propertyData12);
         cache.addChildProperties(nodeData1, properties1);
         cache.commitTransaction();
      }
      catch (Exception e)
      {
         cache.rollbackTransaction();
         throw e;
      }
      // prev stuff
      assertEquals("Cached " + nodeData1.getQPath().getAsString() + " is not equals", cache.get(rootUuid, nodePath1
         .getEntries()[nodePath1.getEntries().length - 1], ItemType.NODE), nodeData1);
      assertEquals("Cached " + nodeData2.getQPath().getAsString() + " is not equals", cache.get(rootUuid, nodePath2
         .getEntries()[nodePath2.getEntries().length - 1], ItemType.NODE), nodeData2);
      assertEquals("Cached " + propertyData12.getQPath().getAsString() + " is not equals", cache.get(nodeUuid1,
         propertyPath12.getEntries()[propertyPath12.getEntries().length - 1], ItemType.PROPERTY), propertyData12);

      // childs...
      // nodes
      assertEquals("Cached child node " + nodeData31.getQPath().getAsString() + " is not equals", cache.get(nodeUuid3,
         nodePath31.getEntries()[nodePath31.getEntries().length - 1], ItemType.NODE), nodeData31);
      assertEquals("Cached child node " + nodeData31.getIdentifier() + " is not equals", cache.get(nodeUuid31),
         nodeData31);

      assertEquals("Cached child node " + nodeData32.getQPath().getAsString() + " is not equals", cache.get(nodeUuid3,
         nodePath32.getEntries()[nodePath32.getEntries().length - 1], ItemType.NODE), nodeData32);
      assertEquals("Cached child node " + nodeData32.getIdentifier() + " is not equals", cache.get(nodeUuid32),
         nodeData32);

      assertTrue("Cached child node " + nodeData31.getQPath().getAsString() + " is not in the childs list", cache
         .getChildNodes(nodeData3).contains(nodeData31));
      assertTrue("Cached child node " + nodeData32.getQPath().getAsString() + " is not in the childs list", cache
         .getChildNodes(nodeData3).contains(nodeData32));

      assertEquals("Cached child nodes count is wrong", cache.getChildNodes(nodeData3).size(), 2);

      // props
      assertEquals("Cached child property " + propertyData11.getQPath().getAsString() + " is not equals", cache.get(
         nodeUuid1, propertyPath11.getEntries()[propertyPath11.getEntries().length - 1], ItemType.PROPERTY),
         propertyData11);
      assertEquals("Cached child property " + propertyData11.getIdentifier() + " is not equals", cache
         .get(propertyUuid11), propertyData11);
      assertEquals("Cached child property " + propertyData12.getQPath().getAsString() + " is not equals", cache.get(
         nodeUuid1, propertyPath12.getEntries()[propertyPath12.getEntries().length - 1], ItemType.PROPERTY),
         propertyData12);
      assertEquals("Cached child property " + propertyData12.getIdentifier() + " is not equals", cache
         .get(propertyUuid12), propertyData12);

      assertEquals("Cached child property " + propertyData21.getQPath().getAsString() + " is not equals", cache.get(
         nodeUuid2, propertyPath21.getEntries()[propertyPath21.getEntries().length - 1], ItemType.PROPERTY),
         propertyData21);
      assertEquals("Cached child property " + propertyData21.getIdentifier() + " is not equals", cache
         .get(propertyUuid21), propertyData21);
      assertEquals("Cached child property " + propertyData22.getQPath().getAsString() + " is not equals", cache.get(
         nodeUuid2, propertyPath22.getEntries()[propertyPath22.getEntries().length - 1], ItemType.PROPERTY),
         propertyData22);
      assertEquals("Cached child property " + propertyData22.getIdentifier() + " is not equals", cache
         .get(propertyUuid22), propertyData22);

      assertTrue("Cached child property " + propertyData11.getQPath().getAsString() + " is not in the childs list",
         cache.getChildProperties(nodeData1).contains(propertyData11));
      assertTrue("Cached child property " + propertyData12.getQPath().getAsString() + " is not in the childs list",
         cache.getChildProperties(nodeData1).contains(propertyData12));
      assertTrue("Cached child property " + propertyData21.getQPath().getAsString() + " is not in the childs list",
         cache.getChildProperties(nodeData2).contains(propertyData21));
      assertTrue("Cached child property " + propertyData22.getQPath().getAsString() + " is not in the childs list",
         cache.getChildProperties(nodeData2).contains(propertyData22));

      assertEquals("Cached child properties count is wrong", cache.getChildProperties(nodeData1).size(), 2);
      assertEquals("Cached child properties count is wrong", cache.getChildProperties(nodeData2).size(), 2);
   }

   public void testGetProperty_Persisted() throws Exception
   {

      initNodesData();
      initDataAsPersisted();

      List<NodeData> nodes = new ArrayList<NodeData>();
      nodes.add(nodeData31);
      nodes.add(nodeData32);
      try
      {
         cache.beginTransaction();
         cache.addChildNodes(nodeData3, nodes);

         cache.put(nodeData1);
         cache.put(nodeData2);
         cache.put(propertyData12);

         List<PropertyData> properties2 = new ArrayList<PropertyData>();
         properties2.add(propertyData21);
         properties2.add(propertyData22);
         cache.addChildProperties(nodeData2, properties2);

         List<PropertyData> properties1 = new ArrayList<PropertyData>();
         properties1.add(propertyData11);
         properties1.add(propertyData12);
         cache.addChildProperties(nodeData1, properties1);
         cache.commitTransaction();
      }
      catch (Exception e)
      {
         cache.rollbackTransaction();
         throw e;
      }
      // props, prev stuff
      assertEquals("Cached child property " + propertyData11.getQPath().getAsString() + " is not equals", cache.get(
         nodeUuid1, propertyPath11.getEntries()[propertyPath11.getEntries().length - 1], ItemType.PROPERTY),
         propertyData11);
      assertEquals("Cached child property " + propertyData11.getIdentifier() + " is not equals", cache
         .get(propertyUuid11), propertyData11);
      assertEquals("Cached child property " + propertyData12.getQPath().getAsString() + " is not equals", cache.get(
         nodeUuid1, propertyPath12.getEntries()[propertyPath12.getEntries().length - 1], ItemType.PROPERTY),
         propertyData12);
      assertEquals("Cached child property " + propertyData12.getIdentifier() + " is not equals", cache
         .get(propertyUuid12), propertyData12);

      assertEquals("Cached child property " + propertyData21.getQPath().getAsString() + " is not equals", cache.get(
         nodeUuid2, propertyPath21.getEntries()[propertyPath21.getEntries().length - 1], ItemType.PROPERTY),
         propertyData21);
      assertEquals("Cached child property " + propertyData21.getIdentifier() + " is not equals", cache
         .get(propertyUuid21), propertyData21);
      assertEquals("Cached child property " + propertyData22.getQPath().getAsString() + " is not equals", cache.get(
         nodeUuid2, propertyPath22.getEntries()[propertyPath22.getEntries().length - 1], ItemType.PROPERTY),
         propertyData22);
      assertEquals("Cached child property " + propertyData22.getIdentifier() + " is not equals", cache
         .get(propertyUuid22), propertyData22);

      assertTrue("Cached child property " + propertyData11.getQPath().getAsString() + " is not in the childs list",
         cache.getChildProperties(nodeData1).contains(propertyData11));
      assertTrue("Cached child property " + propertyData12.getQPath().getAsString() + " is not in the childs list",
         cache.getChildProperties(nodeData1).contains(propertyData12));
      assertTrue("Cached child property " + propertyData21.getQPath().getAsString() + " is not in the childs list",
         cache.getChildProperties(nodeData2).contains(propertyData21));
      assertTrue("Cached child property " + propertyData22.getQPath().getAsString() + " is not in the childs list",
         cache.getChildProperties(nodeData2).contains(propertyData22));

      assertEquals("Cached child properties count is wrong", cache.getChildProperties(nodeData1).size(), 2);
      assertEquals("Cached child properties count is wrong", cache.getChildProperties(nodeData2).size(), 2);

      // remove
      cache.beginTransaction();
      cache.remove(propertyData12);
      cache.commitTransaction();

      // check
      assertEquals("Cached child property " + propertyData11.getQPath().getAsString() + " is not equals", cache.get(
         nodeUuid1, propertyPath11.getEntries()[propertyPath11.getEntries().length - 1], ItemType.PROPERTY),
         propertyData11);
      assertEquals("Cached child property " + propertyData11.getIdentifier() + " is not equals", cache
         .get(propertyUuid11), propertyData11);

      // here
      assertNull("Child property " + propertyData12.getQPath().getAsString() + " is not in the cache", cache.get(
         nodeUuid1, propertyPath12.getEntries()[propertyPath12.getEntries().length - 1], ItemType.PROPERTY));
      assertNull("Child property " + propertyData12.getQPath().getAsString() + " is not in the cache", cache
         .get(propertyUuid12));

      assertEquals("Cached child property " + propertyData21.getQPath().getAsString() + " is not equals", cache.get(
         nodeUuid2, propertyPath21.getEntries()[propertyPath21.getEntries().length - 1], ItemType.PROPERTY),
         propertyData21);
      assertEquals("Cached child property " + propertyData21.getIdentifier() + " is not equals", cache
         .get(propertyUuid21), propertyData21);
      assertEquals("Cached child property " + propertyData22.getQPath().getAsString() + " is not equals", cache.get(
         nodeUuid2, propertyPath22.getEntries()[propertyPath22.getEntries().length - 1], ItemType.PROPERTY),
         propertyData22);
      assertEquals("Cached child property " + propertyData22.getIdentifier() + " is not equals", cache
         .get(propertyUuid22), propertyData22);

      assertTrue("Cached child property " + propertyData11.getQPath().getAsString() + " is not in the childs list",
         cache.getChildProperties(nodeData1).contains(propertyData11));

      // here
      assertFalse("Cached child property " + propertyData12.getQPath().getAsString() + " is not in the childs list",
         cache.getChildProperties(nodeData1).contains(propertyData12));

      assertTrue("Cached child property " + propertyData21.getQPath().getAsString() + " is not in the childs list",
         cache.getChildProperties(nodeData2).contains(propertyData21));
      assertTrue("Cached child property " + propertyData22.getQPath().getAsString() + " is not in the childs list",
         cache.getChildProperties(nodeData2).contains(propertyData22));

      // and here
      assertEquals("Cached child properties count is wrong", cache.getChildProperties(nodeData1).size(), 1);

      assertEquals("Cached child properties count is wrong", cache.getChildProperties(nodeData2).size(), 2);
   }

   public void testRemoveChildNodes() throws Exception
   {

      initNodesData();
      initDataAsPersisted();
      try
      {
         cache.beginTransaction();
         // the case here
         cache.put(nodeData3);
         List<NodeData> n3childNodes = new ArrayList<NodeData>();
         n3childNodes.add(nodeData31);
         n3childNodes.add(nodeData32);
         cache.addChildNodes(nodeData3, n3childNodes);

         // any stuff
         cache.put(nodeData1);
         cache.put(nodeData2);
         cache.put(propertyData12);

         List<PropertyData> properties2 = new ArrayList<PropertyData>();
         properties2.add(propertyData21);
         properties2.add(propertyData22);
         cache.addChildProperties(nodeData2, properties2);

         List<PropertyData> properties1 = new ArrayList<PropertyData>();
         properties1.add(propertyData11);
         properties1.add(propertyData12);
         cache.addChildProperties(nodeData1, properties1);

         // remove
         cache.remove(nodeData3); // remove node3 and its childs (31, 32)
         cache.commitTransaction();
      }
      catch (Exception e)
      {
         cache.rollbackTransaction();
         throw e;
      }
      // check
      assertNull("Node " + nodeData3.getQPath().getAsString() + " in the cache", cache.get(nodeUuid3));
      assertNull("Node " + nodeData3.getQPath().getAsString() + " childs in the cache", cache.getChildNodes(nodeData3));
   }

   public void testRemoveChildProperties() throws Exception
   {

      initNodesData();
      initDataAsPersisted();

      // the case here
      List<PropertyData> properties2 = new ArrayList<PropertyData>();
      properties2.add(propertyData21);
      properties2.add(propertyData22);
      try
      {
         cache.beginTransaction();
         cache.addChildProperties(nodeData2, properties2);

         // any stuff
         cache.put(nodeData3);
         cache.put(nodeData31);

         List<PropertyData> properties1 = new ArrayList<PropertyData>();
         properties1.add(propertyData11);
         properties1.add(propertyData12);
         cache.addChildProperties(nodeData1, properties1);

         // remove
         cache.remove(nodeData2); // remove node2 and its childs (21, 22)
         cache.commitTransaction();
      }
      catch (Exception e)
      {
         cache.rollbackTransaction();
         throw e;
      }
      // check
      assertNull("Node " + nodeData2.getQPath().getAsString() + " in the cache", cache.get(nodeUuid2));
      assertNull("Node " + nodeData2.getQPath().getAsString() + " properties in the cache", cache
         .getChildProperties(nodeData2));
   }

   public void testRemoveChilds() throws Exception
   {

      initNodesData();
      initDataAsPersisted();

      // the case here
      List<PropertyData> properties2 = new ArrayList<PropertyData>();
      properties2.add(propertyData21);
      properties2.add(propertyData22);
      try
      {
         cache.addChildProperties(nodeData2, properties2);

         List<NodeData> nodes2 = new ArrayList<NodeData>();
         nodes2.add(nodeData21);
         nodes2.add(nodeData22);
         cache.addChildNodes(nodeData2, nodes2);

         // any stuff
         cache.put(nodeData3);
         cache.put(nodeData31);
         cache.put(nodeData32);

         List<PropertyData> properties1 = new ArrayList<PropertyData>();
         properties1.add(propertyData11);
         properties1.add(propertyData12);
         cache.addChildProperties(nodeData1, properties1);

         // remove
         PlainChangesLog chlog = new PlainChangesLogImpl();
         chlog.add(ItemState.createDeletedState(propertyData21));
         chlog.add(ItemState.createDeletedState(propertyData22));
         chlog.add(ItemState.createDeletedState(nodeData21));
         chlog.add(ItemState.createDeletedState(nodeData22));
         chlog.add(ItemState.createDeletedState(nodeData2));
         // cache.remove(nodeData2); // remove node2 and its childs and properties (21, 22)
         cache.onSaveItems(chlog);

      }
      catch (Exception e)
      {
         cache.rollbackTransaction();
         throw e;
      }
      // check
      assertNull("Node " + nodeData2.getQPath().getAsString() + " in the cache", cache.get(nodeUuid2));

      // check if childs areremoved as they are cached in lists of childs too
      assertNull("Node " + nodeData2.getQPath().getAsString() + " child nodes in the cache", cache
         .getChildNodes(nodeData2));
      assertNull("Node " + nodeData21.getQPath().getAsString() + " in the cache", cache.get(nodeUuid21));
      assertNull("Node " + nodeData22.getQPath().getAsString() + " in the cache", cache.get(nodeUuid22));

      assertNull("Node " + nodeData2.getQPath().getAsString() + " properties in the cache", cache
         .getChildProperties(nodeData2));
      assertNull("Property " + propertyData21.getQPath().getAsString() + " in the cache", cache.get(propertyUuid21));
      assertNull("Property " + propertyData22.getQPath().getAsString() + " in the cache", cache.get(propertyUuid22));
   }
}
