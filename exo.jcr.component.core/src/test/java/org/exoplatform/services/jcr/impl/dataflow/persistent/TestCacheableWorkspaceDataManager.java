/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see&lt;http://www.gnu.org/licenses/&gt;.
 */
package org.exoplatform.services.jcr.impl.dataflow.persistent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.databene.contiperf.PerfTest;
import org.databene.contiperf.junit.ContiPerfRule;
import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedNodeData;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedPropertyData;
import org.exoplatform.services.jcr.dataflow.persistent.WorkspaceStorageCache;
import org.exoplatform.services.jcr.dataflow.persistent.WorkspaceStorageCacheListener;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.core.itemfilters.QPathEntryFilter;
import org.exoplatform.services.jcr.impl.storage.SystemDataContainerHolder;
import org.exoplatform.services.jcr.impl.storage.WorkspaceDataContainerBase;
import org.exoplatform.services.jcr.storage.WorkspaceDataContainer;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jcr.InvalidItemStateException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS
 * Author : Nicolas Filotto 
 *          nicolas.filotto@exoplatform.com
 * 29 mars 2010  
 */
public class TestCacheableWorkspaceDataManager
{
   private static final int TOTAL_THREADS = 100;
   private static final int TIMES = 20;
   private static final int TOTAL_TIMES = TIMES * TOTAL_THREADS;

   // We need it as a variable since to be compatible with JUnit 4, the class must not extend TestCase so we
   // cannot extend BaseVersionTest. But as we need all the logic inside, we use it as a variable
   private JcrImplBaseTest test;

   @Rule
   public ContiPerfRule rule = new ContiPerfRule();

   private final AtomicInteger step = new AtomicInteger();
   private CyclicBarrier startSignal = new CyclicBarrier(TOTAL_THREADS);

   private CacheableWorkspaceDataManager cwdm;

   private WorkspaceDataContainer wdc;

   private MyWorkspaceStorageConnection con;
   private NodeData nodeData;

   @Before
   public void setUp() throws Exception
   {
      this.test = new JcrImplBaseTest();
      test.setUp();
      this.con = new MyWorkspaceStorageConnection();
      this.wdc = new MyWorkspaceDataContainer(con);
      WorkspaceContainerFacade wsc = test.getRepository().getWorkspaceContainer("ws");
      WorkspaceEntry wconf = (WorkspaceEntry)wsc.getComponent(WorkspaceEntry.class);
      this.cwdm =
         new CacheableWorkspaceDataManager(wconf, wdc, new MyWorkspaceStorageCache(),
            new SystemDataContainerHolder(wdc));
   }

   @After
   public void tearDown() throws Exception
   {
      this.con = null;
      this.wdc = null;
      this.cwdm = null;
      test.after();
   }

   @Test
   @PerfTest(invocations = TOTAL_THREADS, threads = TOTAL_THREADS)
   public void testGetItemById() throws Exception
   {
      assertEquals(0, con.getItemDataByIdCalls.get());
      startSignal.await();
      for (int i = 0; i < TIMES; i++)
      {
         ItemData item = cwdm.getItemData("getItemData");
         assertNotNull(item);
      }
      assertEquals(1, con.getItemDataByIdCalls.get());
   }

   @Test
   @PerfTest(invocations = TOTAL_THREADS, threads = TOTAL_THREADS)
   public void testGetItemDataByNodeDataNQPathEntry() throws Exception
   {
      if (step.compareAndSet(0, 1))
      {
         nodeData = new PersistedNodeData("getItemData", new QPath(new QPathEntry[]{}), null, 0, 1, null, null, null);
      }
      assertEquals(0, con.getItemDataByNodeDataNQPathEntryCalls.get());
      startSignal.await();
      for (int i = 0; i < TIMES; i++)
      {
         ItemData item = cwdm.getItemData(nodeData, new QPathEntry("http://www.foo.com", "foo", 0), ItemType.NODE);
         assertNotNull(item);
      }
      assertEquals(1, con.getItemDataByNodeDataNQPathEntryCalls.get());
   }

   @Test
   @PerfTest(invocations = TOTAL_THREADS, threads = TOTAL_THREADS)
   public void testGetChildPropertiesData() throws Exception
   {
      if (step.compareAndSet(0, 1))
      {
         nodeData = new PersistedNodeData("getChildPropertiesData", null, null, 0, 1, null, null, null);
      }
      assertEquals(0, con.getChildPropertiesDataCalls.get());
      startSignal.await();
      for (int i = 0; i < TIMES; i++)
      {
         List<PropertyData> properties = cwdm.getChildPropertiesData(nodeData);
         assertNotNull(properties);
         assertFalse(properties.isEmpty());
      }
      assertEquals(1, con.getChildPropertiesDataCalls.get());
      startSignal.await();
      for (int i = 0; i < TIMES; i++)
      {
         List<PropertyData> properties = cwdm.getChildPropertiesData(nodeData, true);
         assertNotNull(properties);
         assertFalse(properties.isEmpty());
      }
      startSignal.await();
      assertEquals(1 + TOTAL_TIMES, con.getChildPropertiesDataCalls.get());
   }

   @Test
   @PerfTest(invocations = TOTAL_THREADS, threads = TOTAL_THREADS)
   public void testListChildPropertiesData() throws Exception
   {
      if (step.compareAndSet(0, 1))
      {
         nodeData = new PersistedNodeData("listChildPropertiesData", null, null, 0, 1, null, null, null);
      }
      assertEquals(0, con.listChildPropertiesDataCalls.get());
      startSignal.await();
      for (int i = 0; i < TIMES; i++)
      {
         List<PropertyData> properties = cwdm.listChildPropertiesData(nodeData);
         assertNotNull(properties);
         assertFalse(properties.isEmpty());
      }
      assertEquals(1, con.listChildPropertiesDataCalls.get());
      startSignal.await();
      for (int i = 0; i < TIMES; i++)
      {
         List<PropertyData> properties = cwdm.listChildPropertiesData(nodeData, true);
         assertNotNull(properties);
         assertFalse(properties.isEmpty());
      }
      startSignal.await();
      assertEquals(1 + TOTAL_TIMES, con.listChildPropertiesDataCalls.get());
   }

   @Test
   @PerfTest(invocations = TOTAL_THREADS, threads = TOTAL_THREADS)
   public void testGetChildNodes() throws Exception
   {
      if (step.compareAndSet(0, 1))
      {
         nodeData = new PersistedNodeData("getChildNodes", null, null, 0, 1, null, null, null);
      }
      assertEquals(0, con.getChildNodesDataCalls.get());
      startSignal.await();
      for (int i = 0; i < TIMES; i++)
      {
         List<NodeData> nodes = cwdm.getChildNodesData(nodeData);
         assertNotNull(nodes);
         assertFalse(nodes.isEmpty());
      }
      assertEquals(1, con.getChildNodesDataCalls.get());
      startSignal.await();
      for (int i = 0; i < TIMES; i++)
      {
         List<NodeData> nodes = cwdm.getChildNodesData(nodeData, true);
         assertNotNull(nodes);
         assertFalse(nodes.isEmpty());
      }
      startSignal.await();
      assertEquals(1 + TOTAL_TIMES, con.getChildNodesDataCalls.get());
   }

   @Test
   @PerfTest(invocations = TOTAL_THREADS, threads = TOTAL_THREADS)
   public void testGetChildNodesCount() throws Exception
   {
      if (step.compareAndSet(0, 1))
      {
         nodeData = new PersistedNodeData("getChildNodesCount", null, null, 0, 1, null, null, null);
      }
      assertEquals(0, con.getChildNodesCountCalls.get());
      startSignal.await();
      for (int i = 0; i < TIMES; i++)
      {
         int result = cwdm.getChildNodesCount(nodeData);
         assertEquals(1, result);
      }
      assertEquals(1, con.getChildNodesCountCalls.get());
   }

   private static class MyWorkspaceStorageCache implements WorkspaceStorageCache
   {

      private volatile List<NodeData> childNodes;

      public void addChildNodes(NodeData parent, List<NodeData> childNodes)
      {
         this.childNodes = childNodes;
      }

      private volatile List<PropertyData> childProperties;

      public void addChildProperties(NodeData parent, List<PropertyData> childProperties)
      {
         this.childProperties = childProperties;
      }

      private volatile List<PropertyData> childPropertiesList;

      public void addChildPropertiesList(NodeData parent, List<PropertyData> childProperties)
      {
         this.childPropertiesList = childProperties;
      }

      public void beginTransaction()
      {
      }

      public void commitTransaction()
      {
      }

      private volatile ItemData itemData;

      public ItemData get(String parentIdentifier, QPathEntry name, ItemType itemType)
      {
         if (itemData != null && itemType.isSuitableFor(itemData))
         {
            return itemData;
         }

         return null;
      }

      public ItemData get(String identifier)
      {
         return itemData;
      }

      public List<NodeData> getChildNodes(NodeData parent)
      {
         return childNodes;
      }

      public List<PropertyData> getChildProperties(NodeData parent)
      {
         return childProperties;
      }

      public long getSize()
      {
         return 0;
      }

      public boolean isEnabled()
      {
         return true;
      }

      /**
       * {@inheritDoc}
       */
      public boolean isPatternSupported()
      {
         return false;
      }
      
      /**
       * {@inheritDoc}
       */
      public boolean isChildNodesByPageSupported()
      {
         return false;
      }

      public List<PropertyData> listChildProperties(NodeData parentData)
      {
         return childPropertiesList;
      }

      public void put(ItemData item)
      {
         this.itemData = item;
      }

      public void remove(ItemData item)
      {
         this.itemData = null;
      }

      public void rollbackTransaction()
      {
      }

      public boolean isTXAware()
      {
         return true;
      }

      public void onSaveItems(ItemStateChangesLog itemStates)
      {
      }

      public int getChildNodesCount(NodeData parent)
      {
         return childNodes != null ? childNodes.size() : count;
      }

      public List<PropertyData> getReferencedProperties(String identifier)
      {
         return null;
      }

      public void addReferencedProperties(String identifier, List<PropertyData> refProperties)
      {
      }

      public void addChildProperties(NodeData parent, QPathEntryFilter pattern, List<PropertyData> childProperties)
      {
      }

      public List<PropertyData> getChildProperties(NodeData parent, QPathEntryFilter pattern)
      {
         return null;
      }

      public void addChildNodes(NodeData parent, QPathEntryFilter pattern, List<NodeData> childNodes)
      {
      }

      public List<NodeData> getChildNodes(NodeData parent, QPathEntryFilter pattern)
      {
         return null;
      }

      public List<NodeData> getChildNodesByPage(NodeData parent, int fromOrderNum)
      {
         return null;
      }

      public void addChildNodesByPage(NodeData parent, List<NodeData> childs, int fromOrderNum)
      {
      }

      /**
       * @see org.exoplatform.services.jcr.dataflow.persistent.WorkspaceStorageCache#addListener(org.exoplatform.services.jcr.dataflow.persistent.WorkspaceStorageCacheListener)
       */
      public void addListener(WorkspaceStorageCacheListener listener) throws UnsupportedOperationException
      {
      }

      /**
       * @see org.exoplatform.services.jcr.dataflow.persistent.WorkspaceStorageCache#removeListener(org.exoplatform.services.jcr.dataflow.persistent.WorkspaceStorageCacheListener)
       */
      public void removeListener(WorkspaceStorageCacheListener listener) throws UnsupportedOperationException
      {
      }

      private volatile int count = -1;
      /**
       * @see org.exoplatform.services.jcr.dataflow.persistent.WorkspaceStorageCache#addChildNodesCount(org.exoplatform.services.jcr.datamodel.NodeData, int)
       */
      public void addChildNodesCount(NodeData parent, int count)
      {
         this.count = count;
      }
   }

   private static class MyWorkspaceStorageConnection implements WorkspaceStorageConnection
   {

      public void add(NodeData data) throws RepositoryException, UnsupportedOperationException,
         InvalidItemStateException, IllegalStateException
      {
      }

      public void add(PropertyData data, ChangedSizeHandler sizeHandler) throws RepositoryException,
         UnsupportedOperationException,
         InvalidItemStateException, IllegalStateException
      {
      }

      public void close() throws IllegalStateException, RepositoryException
      {
      }

      public void commit() throws IllegalStateException, RepositoryException
      {
      }

      public void prepare() throws IllegalStateException, RepositoryException
      {
      }

      public void delete(NodeData data) throws RepositoryException, UnsupportedOperationException,
         InvalidItemStateException, IllegalStateException
      {
      }

      public void delete(PropertyData data, ChangedSizeHandler sizeHandler) throws RepositoryException,
         UnsupportedOperationException, InvalidItemStateException, IllegalStateException
      {
      }

      public AtomicInteger getChildNodesCountCalls = new AtomicInteger();

      public int getChildNodesCount(NodeData parent) throws RepositoryException
      {
         getChildNodesCountCalls.incrementAndGet();
         return 1;
      }

      public AtomicInteger getChildNodesDataCalls = new AtomicInteger();

      public List<NodeData> getChildNodesData(NodeData parent) throws RepositoryException, IllegalStateException
      {
         getChildNodesDataCalls.incrementAndGet();
         return Arrays.asList((NodeData)new PersistedNodeData("getChildNodesData", null, null, 0, 1, null, null, null));
      }

      public AtomicInteger getChildPropertiesDataCalls = new AtomicInteger();

      public List<PropertyData> getChildPropertiesData(NodeData parent) throws RepositoryException,
         IllegalStateException
      {
         getChildPropertiesDataCalls.incrementAndGet();
         return Arrays.asList((PropertyData)new PersistedPropertyData("getChildPropertiesData", null, null, 0,
            PropertyType.STRING, false, Arrays.asList((ValueData)new ByteArrayPersistedValueData(1, "foo".getBytes())),
            new SimplePersistedSize(0)));
      }

      public List<PropertyData> getChildPropertiesData(NodeData parent, List<QPathEntryFilter> pattern)
         throws RepositoryException, IllegalStateException
      {
         getChildPropertiesDataCalls.incrementAndGet();
         return Arrays.asList((PropertyData)new PersistedPropertyData("getChildPropertiesDataByPattern", null, null, 0,
            PropertyType.STRING, false, Arrays.asList((ValueData)new ByteArrayPersistedValueData(1, "foo".getBytes())),
            new SimplePersistedSize(0)));
      }

      public AtomicInteger getItemDataByNodeDataNQPathEntryCalls = new AtomicInteger();

      public ItemData getItemData(NodeData parentData, QPathEntry name, ItemType itemType) throws RepositoryException,
         IllegalStateException
      {
         getItemDataByNodeDataNQPathEntryCalls.incrementAndGet();
         if (itemType != ItemType.PROPERTY)
         {
            return new PersistedNodeData("getItemData", null, null, 0, 1, null, null, null);
         }

         return null;
      }

      public AtomicInteger getItemDataByIdCalls = new AtomicInteger();

      public ItemData getItemData(String identifier) throws RepositoryException, IllegalStateException
      {
         getItemDataByIdCalls.incrementAndGet();
         return new PersistedNodeData("getItemData", null, null, 0, 1, null, null, null);
      }

      public AtomicInteger getReferencesDataCalls = new AtomicInteger();

      public List<PropertyData> getReferencesData(String nodeIdentifier) throws RepositoryException,
         IllegalStateException, UnsupportedOperationException
      {
         getReferencesDataCalls.incrementAndGet();
         return Arrays.asList((PropertyData)new PersistedPropertyData("getReferencesData", null, null, 0,
            PropertyType.STRING, false, Arrays.asList((ValueData)new ByteArrayPersistedValueData(1, "foo".getBytes())),
            new SimplePersistedSize(0)));
      }

      public boolean isOpened()
      {
         return true;
      }

      public AtomicInteger listChildPropertiesDataCalls = new AtomicInteger();

      public List<PropertyData> listChildPropertiesData(NodeData parent) throws RepositoryException,
         IllegalStateException
      {
         listChildPropertiesDataCalls.incrementAndGet();
         return Arrays.asList((PropertyData)new PersistedPropertyData("listChildPropertiesData", null, null, 0,
            PropertyType.STRING, false, Arrays.asList((ValueData)new ByteArrayPersistedValueData(1, "foo".getBytes())),
            new SimplePersistedSize(0)));
      }

      public void rename(NodeData data) throws RepositoryException, UnsupportedOperationException,
         InvalidItemStateException, IllegalStateException
      {
      }

      public void rollback() throws IllegalStateException, RepositoryException
      {
      }

      public void update(NodeData data) throws RepositoryException, UnsupportedOperationException,
         InvalidItemStateException, IllegalStateException
      {
      }

      public void update(PropertyData data, ChangedSizeHandler sizeHandler) throws RepositoryException,
         UnsupportedOperationException, InvalidItemStateException, IllegalStateException
      {
      }

      public int getLastOrderNumber(NodeData parent) throws RepositoryException
      {
         return -1;
      }

      public List<NodeData> getChildNodesData(NodeData parent, List<QPathEntryFilter> pattern) throws RepositoryException,
         IllegalStateException
      {
         return null;
      }

      public boolean getChildNodesDataByPage(NodeData parent, int fromOrderNum, int toOrderNum, List<NodeData> childs)
         throws RepositoryException
      {
         return false;
      }
      
      /**
       * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#getACLHolders()
       */
      public List<ACLHolder> getACLHolders() throws RepositoryException, IllegalStateException,
         UnsupportedOperationException
      {
         return null;
      }

      /**
       * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#getNodesCount()
       */
      public long getNodesCount() throws RepositoryException
      {
         throw new UnsupportedOperationException();
      }

      public boolean hasItemData(NodeData parentData, QPathEntry name, ItemType itemType) throws RepositoryException,
         IllegalStateException
      {
         return getItemData(parentData, name, itemType) != null;
      }

      public long getWorkspaceDataSize() throws RepositoryException
      {
         return 0;
      }

      public long getNodeDataSize(String parentId) throws RepositoryException
      {
         return 0;
      }
   };

   private static class MyWorkspaceDataContainer extends WorkspaceDataContainerBase
   {

      private WorkspaceStorageConnection con;

      public MyWorkspaceDataContainer(WorkspaceStorageConnection con)
      {
         this.con = con;
      }

      public boolean isCheckSNSNewConnection()
      {
         return false;
      }

      public boolean isSame(WorkspaceDataContainer another)
      {
         return false;
      }

      public WorkspaceStorageConnection openConnection() throws RepositoryException
      {
         return con;
      }

      public WorkspaceStorageConnection openConnection(boolean readOnly) throws RepositoryException
      {
         return con;
      }

      public WorkspaceStorageConnection reuseConnection(WorkspaceStorageConnection original) throws RepositoryException
      {
         return con;
      }

      public String getInfo()
      {
         return "MyWorkspaceDataContainer";
      }

      public String getName()
      {
         return "MyWorkspaceDataContainer";
      }

      public String getStorageVersion()
      {
         return "0";
      }

      public String getUniqueName()
      {
         return "MyWorkspaceDataContainer";
      }

   };

}
