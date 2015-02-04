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
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.PropertyImpl;
import org.exoplatform.services.jcr.impl.core.itemfilters.ExactQPathEntryFilter;
import org.exoplatform.services.jcr.impl.core.itemfilters.PatternQPathEntryFilter;
import org.exoplatform.services.jcr.impl.core.itemfilters.QPathEntryFilter;
import org.exoplatform.services.jcr.impl.storage.SystemDataContainerHolder;
import org.exoplatform.services.jcr.impl.storage.WorkspaceDataContainerBase;
import org.exoplatform.services.jcr.storage.WorkspaceDataContainer;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS
 * Author : Nicolas Filotto 
 *          nicolas.filotto@exoplatform.com
 * 29 mars 2010  
 */
public class TestCacheableWorkspaceDataManager extends JcrImplBaseTest
{

   private static final int READER = 100;

   private static final int TIMES = 20;

   private CacheableWorkspaceDataManager cwdm;

   private WorkspaceDataContainer wdc;

   private MyWorkspaceStorageConnection con;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      this.con = new MyWorkspaceStorageConnection();
      this.wdc = new MyWorkspaceDataContainer(con);
      WorkspaceContainerFacade wsc = repository.getWorkspaceContainer("ws");
      WorkspaceEntry wconf = (WorkspaceEntry)wsc.getComponent(WorkspaceEntry.class);
      this.cwdm =
         new CacheableWorkspaceDataManager(wconf, wdc, new MyWorkspaceStorageCache(),
            new SystemDataContainerHolder(wdc));
   }

   @Override
   protected void tearDown() throws Exception
   {
      this.con = null;
      this.wdc = null;
      this.cwdm = null;
      super.tearDown();
   }

   private void multiThreadingTest(final MyTask task) throws Exception
   {
      final CountDownLatch startSignal = new CountDownLatch(1);
      final CountDownLatch doneSignal = new CountDownLatch(READER);
      final List<Exception> errors = Collections.synchronizedList(new ArrayList<Exception>());
      for (int i = 0; i < READER; i++)
      {
         Thread thread = new Thread()
         {
            @Override
            public void run()
            {
               try
               {
                  startSignal.await();
                  for (int i = 0; i < TIMES; i++)
                  {
                     task.execute();
                  }
               }
               catch (Exception e)
               {
                  errors.add(e);
               }
               finally
               {
                  doneSignal.countDown();
               }
            }
         };
         thread.start();
      }
      startSignal.countDown();
      doneSignal.await();
      if (!errors.isEmpty())
      {
         for (Exception e : errors)
         {
            e.printStackTrace();
         }
         throw errors.get(0);
      }
   }

   public void testGetItemById() throws Exception
   {
      assertEquals(0, con.getItemDataByIdCalls.get());
      MyTask task = new MyTask()
      {
         public void execute() throws Exception
         {
            ItemData item = cwdm.getItemData("getItemData");
            assertNotNull(item);
         }
      };
      multiThreadingTest(task);
      assertEquals(1, con.getItemDataByIdCalls.get());
   }

   public void testGetItemDataByNodeDataNQPathEntry() throws Exception
   {
      final NodeData nodeData =
         new PersistedNodeData("getItemData", new QPath(new QPathEntry[]{}), null, 0, 1, null, null, null);
      assertEquals(0, con.getItemDataByNodeDataNQPathEntryCalls.get());
      MyTask task = new MyTask()
      {
         public void execute() throws Exception
         {
            ItemData item = cwdm.getItemData(nodeData, new QPathEntry("http://www.foo.com", "foo", 0), ItemType.NODE);
            assertNotNull(item);
         }
      };
      multiThreadingTest(task);
      assertEquals(1, con.getItemDataByNodeDataNQPathEntryCalls.get());
   }

   public void testGetChildPropertiesData() throws Exception
   {
      final NodeData nodeData = new PersistedNodeData("getChildPropertiesData", null, null, 0, 1, null, null, null);
      assertEquals(0, con.getChildPropertiesDataCalls.get());
      MyTask task = new MyTask()
      {
         public void execute() throws Exception
         {
            List<PropertyData> properties = cwdm.getChildPropertiesData(nodeData);
            assertNotNull(properties);
            assertFalse(properties.isEmpty());
         }
      };
      multiThreadingTest(task);
      assertEquals(1, con.getChildPropertiesDataCalls.get());
      task = new MyTask()
      {
         public void execute() throws Exception
         {
            List<PropertyData> properties = cwdm.getChildPropertiesData(nodeData, true);
            assertNotNull(properties);
            assertFalse(properties.isEmpty());
         }
      };
      multiThreadingTest(task);
      assertEquals(1 + READER * TIMES, con.getChildPropertiesDataCalls.get());
   }

   public void testListChildPropertiesData() throws Exception
   {
      final NodeData nodeData = new PersistedNodeData("listChildPropertiesData", null, null, 0, 1, null, null, null);
      assertEquals(0, con.listChildPropertiesDataCalls.get());
      MyTask task = new MyTask()
      {
         public void execute() throws Exception
         {
            List<PropertyData> properties = cwdm.listChildPropertiesData(nodeData);
            assertNotNull(properties);
            assertFalse(properties.isEmpty());
         }
      };
      multiThreadingTest(task);
      assertEquals(1, con.listChildPropertiesDataCalls.get());
      task = new MyTask()
      {
         public void execute() throws Exception
         {
            List<PropertyData> properties = cwdm.listChildPropertiesData(nodeData, true);
            assertNotNull(properties);
            assertFalse(properties.isEmpty());
         }
      };
      multiThreadingTest(task);
      assertEquals(1 + READER * TIMES, con.listChildPropertiesDataCalls.get());
   }

   public void testGetChildNodes() throws Exception
   {
      final NodeData nodeData = new PersistedNodeData("getChildNodes", null, null, 0, 1, null, null, null);
      assertEquals(0, con.getChildNodesDataCalls.get());
      MyTask task = new MyTask()
      {
         public void execute() throws Exception
         {
            List<NodeData> nodes = cwdm.getChildNodesData(nodeData);
            assertNotNull(nodes);
            assertFalse(nodes.isEmpty());
         }
      };
      multiThreadingTest(task);
      assertEquals(1, con.getChildNodesDataCalls.get());
      task = new MyTask()
      {
         public void execute() throws Exception
         {
            List<NodeData> nodes = cwdm.getChildNodesData(nodeData, true);
            assertNotNull(nodes);
            assertFalse(nodes.isEmpty());
         }
      };
      multiThreadingTest(task);
      assertEquals(1 + READER * TIMES, con.getChildNodesDataCalls.get());
   }

   public void testGetChildNodesCount() throws Exception
   {
      final NodeData nodeData = new PersistedNodeData("getChildNodesCount", null, null, 0, 1, null, null, null);
      assertEquals(0, con.getChildNodesCountCalls.get());
      MyTask task = new MyTask()
      {
         public void execute() throws Exception
         {
            int result = cwdm.getChildNodesCount(nodeData);
            assertEquals(1, result);
         }
      };
      multiThreadingTest(task);
      assertEquals(1, con.getChildNodesCountCalls.get());
   }

   public void testBigFileWithCacheEnabledById() throws Exception
   {
      WorkspaceContainerFacade wsc = repository.getWorkspaceContainer("ws");
      CacheableWorkspaceDataManager cwdm = (CacheableWorkspaceDataManager)wsc.getComponent(CacheableWorkspaceDataManager.class);
      if (!cwdm.cache.isEnabled())
         return;
      Node testLocalBigFiles = root.addNode("testBigFileWithCacheEnabledById");
      // 300 Kb
      String path = createBLOBTempFile(300).getAbsolutePath();

      Node localBigFile = testLocalBigFiles.addNode("bigFile", "nt:file");
      Node contentNode = localBigFile.addNode("jcr:content", "nt:resource");
      InputStream is = new FileInputStream(path);
      PropertyImpl data = (PropertyImpl)contentNode.setProperty("jcr:data", is);
      contentNode.setProperty("jcr:mimeType", "application/octet-stream ");
      contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
      session.save();
      String id = data.getInternalIdentifier();
      PropertyData pData = (PropertyData)cwdm.getCachedItemData(id);
      assertNotNull(pData);
      List<ValueData> vals = pData.getValues();
      assertTrue(vals != null);
      assertFalse(vals.isEmpty());
      assertTrue(vals.get(0) instanceof StreamPersistedValueData);
      StreamPersistedValueData fpvd = (StreamPersistedValueData)vals.get(0);
      assertNotNull(fpvd.getFile());
      assertTrue(cwdm.getItemData(id) == pData);

      // Simulate cases where the file is null such as during a replication of StreamPersistedValueData with a spool file as file
      // on all other cluster nodes the spool file doesn't exist so file will be set to null
      fpvd.setPersistedFile(null);

      assertFalse(cwdm.getItemData(id) == pData);
      assertTrue(cwdm.getItemData(id) == cwdm.getCachedItemData(id));
   }

   public void testBigFileWithCacheEnabledByPath() throws Exception
   {
      WorkspaceContainerFacade wsc = repository.getWorkspaceContainer("ws");
      CacheableWorkspaceDataManager cwdm = (CacheableWorkspaceDataManager)wsc.getComponent(CacheableWorkspaceDataManager.class);
      if (!cwdm.cache.isEnabled())
         return;
      Node testLocalBigFiles = root.addNode("testBigFileWithCacheEnabledByPath");
      // 300 Kb
      String path = createBLOBTempFile(300).getAbsolutePath();

      Node localBigFile = testLocalBigFiles.addNode("bigFile", "nt:file");
      Node contentNode = localBigFile.addNode("jcr:content", "nt:resource");
      InputStream is = new FileInputStream(path);
      PropertyImpl data = (PropertyImpl)contentNode.setProperty("jcr:data", is);
      contentNode.setProperty("jcr:mimeType", "application/octet-stream ");
      contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
      session.save();
      QPathEntry qpeProp = new QPathEntry(Constants.NS_JCR_URI, "data", 0);
      PropertyData pData = (PropertyData)cwdm.getCachedItemData(data.parentData(), qpeProp, ItemType.PROPERTY);
      assertNotNull(pData);
      List<ValueData> vals = pData.getValues();
      assertTrue(vals != null);
      assertFalse(vals.isEmpty());
      assertTrue(vals.get(0) instanceof StreamPersistedValueData);
      StreamPersistedValueData fpvd = (StreamPersistedValueData)vals.get(0);
      assertNotNull(fpvd.getFile());
      assertTrue(cwdm.getItemData(data.parentData(), qpeProp, ItemType.PROPERTY) == pData);

      // Simulate cases where the file is null such as during a replication of StreamPersistedValueData with a spool file as file
      // on all other cluster nodes the spool file doesn't exist so file will be set to null
      fpvd.setPersistedFile(null);

      assertFalse(cwdm.getItemData(data.parentData(), qpeProp, ItemType.PROPERTY) == pData);
      assertTrue(cwdm.getItemData(data.parentData(), qpeProp, ItemType.PROPERTY) == cwdm.getCachedItemData(
         data.parentData(), qpeProp, ItemType.PROPERTY));
   }

   public void testBigFileWithCacheEnabledByProps() throws Exception
   {
      WorkspaceContainerFacade wsc = repository.getWorkspaceContainer("ws");
      CacheableWorkspaceDataManager cwdm = (CacheableWorkspaceDataManager)wsc.getComponent(CacheableWorkspaceDataManager.class);
      if (!cwdm.cache.isEnabled())
         return;
      Node testLocalBigFiles = root.addNode("testBigFileWithCacheEnabledByProps");
      // 300 Kb
      String path = createBLOBTempFile(300).getAbsolutePath();

      Node localBigFile = testLocalBigFiles.addNode("bigFile", "nt:file");
      Node contentNode = localBigFile.addNode("jcr:content", "nt:resource");
      InputStream is = new FileInputStream(path);
      PropertyImpl data = (PropertyImpl)contentNode.setProperty("jcr:data", is);
      contentNode.setProperty("jcr:mimeType", "application/octet-stream ");
      contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
      session.save();
      // Load cache first
      cwdm.getChildPropertiesData(data.parentData());
      List<PropertyData> props = cwdm.cache.getChildProperties(data.parentData());
      PropertyData pData = null;
      for (PropertyData pd : props)
      {
         if (pd.getIdentifier().equals(data.getInternalIdentifier()))
         {
            pData = pd;
            break;
         }
      }
      assertNotNull(pData);
      List<ValueData> vals = pData.getValues();
      assertTrue(vals != null);
      assertFalse(vals.isEmpty());
      assertTrue(vals.get(0) instanceof StreamPersistedValueData);
      StreamPersistedValueData fpvd = (StreamPersistedValueData)vals.get(0);
      assertNotNull(fpvd.getFile());
      PropertyData pData2 = null;
      for (PropertyData pd : cwdm.getChildPropertiesData(data.parentData()))
      {
         if (pd.getIdentifier().equals(data.getInternalIdentifier()))
         {
            pData2 = pd;
            break;
         }
      }
      assertTrue(pData2 == pData);

      // Simulate cases where the file is null such as during a replication of StreamPersistedValueData with a spool file as file
      // on all other cluster nodes the spool file doesn't exist so file will be set to null
      fpvd.setPersistedFile(null);

      pData2 = null;
      for (PropertyData pd : cwdm.getChildPropertiesData(data.parentData()))
      {
         if (pd.getIdentifier().equals(data.getInternalIdentifier()))
         {
            pData2 = pd;
            break;
         }
      }
      assertFalse(pData2 == pData);
      pData2 = null;
      for (PropertyData pd : cwdm.getChildPropertiesData(data.parentData()))
      {
         if (pd.getIdentifier().equals(data.getInternalIdentifier()))
         {
            pData2 = pd;
            break;
         }
      }
      PropertyData pData3 = null;
      for (PropertyData pd : cwdm.cache.getChildProperties(data.parentData()))
      {
         if (pd.getIdentifier().equals(data.getInternalIdentifier()))
         {
            pData3 = pd;
            break;
         }
      }
      assertTrue(pData2 == pData3);
   }

   /**
    * Using getChildPropertiesData with parentData
    */
   public void testBigFileWithCacheEnabledByPattern1() throws Exception
   {
      WorkspaceContainerFacade wsc = repository.getWorkspaceContainer("ws");
      CacheableWorkspaceDataManager cwdm = (CacheableWorkspaceDataManager)wsc.getComponent(CacheableWorkspaceDataManager.class);
      if (!cwdm.cache.isEnabled())
         return;
      Node testLocalBigFiles = root.addNode("testBigFileWithCacheEnabledByPattern1");
      // 300 Kb
      String path = createBLOBTempFile(300).getAbsolutePath();

      Node localBigFile = testLocalBigFiles.addNode("bigFile", "nt:file");
      Node contentNode = localBigFile.addNode("jcr:content", "nt:resource");
      InputStream is = new FileInputStream(path);
      PropertyImpl data = (PropertyImpl)contentNode.setProperty("jcr:data", is);
      contentNode.setProperty("jcr:mimeType", "application/octet-stream ");
      contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
      session.save();
      // Load cache first
      cwdm.getChildPropertiesData(data.parentData());
      List<PropertyData> props = cwdm.cache.getChildProperties(data.parentData());
      PropertyData pData = null;
      for (PropertyData pd : props)
      {
         if (pd.getIdentifier().equals(data.getInternalIdentifier()))
         {
            pData = pd;
            break;
         }
      }
      assertNotNull(pData);
      List<ValueData> vals = pData.getValues();
      assertTrue(vals != null);
      assertFalse(vals.isEmpty());
      assertTrue(vals.get(0) instanceof StreamPersistedValueData);
      StreamPersistedValueData fpvd = (StreamPersistedValueData)vals.get(0);
      assertNotNull(fpvd.getFile());
      PropertyData pData2 = null;
      List<QPathEntryFilter> itemDataFilters =
         Collections.singletonList((QPathEntryFilter)new PatternQPathEntryFilter(new QPathEntry("*", "*", 0)));
      for (PropertyData pd : cwdm.getChildPropertiesData(data.parentData(), itemDataFilters))
      {
         if (pd.getIdentifier().equals(data.getInternalIdentifier()))
         {
            pData2 = pd;
            break;
         }
      }
      assertTrue(pData2 == pData);

      // Simulate cases where the file is null such as during a replication of StreamPersistedValueData with a spool file as file
      // on all other cluster nodes the spool file doesn't exist so file will be set to null
      fpvd.setPersistedFile(null);

      pData2 = null;
      for (PropertyData pd : cwdm.getChildPropertiesData(data.parentData(), itemDataFilters))
      {
         if (pd.getIdentifier().equals(data.getInternalIdentifier()))
         {
            pData2 = pd;
            break;
         }
      }
      assertFalse(pData2 == pData);
      pData2 = null;
      for (PropertyData pd : cwdm.getChildPropertiesData(data.parentData(), itemDataFilters))
      {
         if (pd.getIdentifier().equals(data.getInternalIdentifier()))
         {
            pData2 = pd;
            break;
         }
      }
      PropertyData pData3 = null;
      for (PropertyData pd : cwdm.cache.getChildProperties(data.parentData()))
      {
         if (pd.getIdentifier().equals(data.getInternalIdentifier()))
         {
            pData3 = pd;
            break;
         }
      }
      assertTrue(pData2 == pData3);
   }

   /**
    * Using getChildPropertiesData with parentData and pattern
    */
   public void testBigFileWithCacheEnabledByPattern2() throws Exception
   {
      WorkspaceContainerFacade wsc = repository.getWorkspaceContainer("ws");
      CacheableWorkspaceDataManager cwdm = (CacheableWorkspaceDataManager)wsc.getComponent(CacheableWorkspaceDataManager.class);
      if (!cwdm.cache.isEnabled())
         return;
      Node testLocalBigFiles = root.addNode("testBigFileWithCacheEnabledByPattern2");
      // 300 Kb
      String path = createBLOBTempFile(300).getAbsolutePath();

      Node localBigFile = testLocalBigFiles.addNode("bigFile", "nt:file");
      Node contentNode = localBigFile.addNode("jcr:content", "nt:resource");
      InputStream is = new FileInputStream(path);
      PropertyImpl data = (PropertyImpl)contentNode.setProperty("jcr:data", is);
      contentNode.setProperty("jcr:mimeType", "application/octet-stream ");
      contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
      session.save();
      // Load cache first
      QPathEntryFilter filter = new PatternQPathEntryFilter(new QPathEntry("*", "*", 0));
      List<QPathEntryFilter> itemDataFilters = Collections.singletonList((QPathEntryFilter)filter);
      cwdm.getChildPropertiesData(data.parentData(), itemDataFilters);
      List<PropertyData> props = cwdm.cache.getChildProperties(data.parentData(), filter);
      PropertyData pData = null;
      for (PropertyData pd : props)
      {
         if (pd.getIdentifier().equals(data.getInternalIdentifier()))
         {
            pData = pd;
            break;
         }
      }
      assertNotNull(pData);
      List<ValueData> vals = pData.getValues();
      assertTrue(vals != null);
      assertFalse(vals.isEmpty());
      assertTrue(vals.get(0) instanceof StreamPersistedValueData);
      StreamPersistedValueData fpvd = (StreamPersistedValueData)vals.get(0);
      assertNotNull(fpvd.getFile());
      PropertyData pData2 = null;
      for (PropertyData pd : cwdm.getChildPropertiesData(data.parentData(), itemDataFilters))
      {
         if (pd.getIdentifier().equals(data.getInternalIdentifier()))
         {
            pData2 = pd;
            break;
         }
      }
      assertTrue(pData2 == pData);

      // Simulate cases where the file is null such as during a replication of StreamPersistedValueData with a spool file as file
      // on all other cluster nodes the spool file doesn't exist so file will be set to null
      fpvd.setPersistedFile(null);

      pData2 = null;
      for (PropertyData pd : cwdm.getChildPropertiesData(data.parentData(), itemDataFilters))
      {
         if (pd.getIdentifier().equals(data.getInternalIdentifier()))
         {
            pData2 = pd;
            break;
         }
      }
      assertFalse(pData2 == pData);
      pData2 = null;
      for (PropertyData pd : cwdm.getChildPropertiesData(data.parentData(), itemDataFilters))
      {
         if (pd.getIdentifier().equals(data.getInternalIdentifier()))
         {
            pData2 = pd;
            break;
         }
      }
      PropertyData pData3 = null;
      for (PropertyData pd : cwdm.cache.getChildProperties(data.parentData(), filter))
      {
         if (pd.getIdentifier().equals(data.getInternalIdentifier()))
         {
            pData3 = pd;
            break;
         }
      }
      assertTrue(pData2 == pData3);
   }

   /**
    * Using get with parentData, QPathEntry and index
    */
   public void testBigFileWithCacheEnabledByPattern3() throws Exception
   {
      WorkspaceContainerFacade wsc = repository.getWorkspaceContainer("ws");
      CacheableWorkspaceDataManager cwdm = (CacheableWorkspaceDataManager)wsc.getComponent(CacheableWorkspaceDataManager.class);
      if (!cwdm.cache.isEnabled())
         return;
      Node testLocalBigFiles = root.addNode("testBigFileWithCacheEnabledByPattern3");
      // 300 Kb
      String path = createBLOBTempFile(300).getAbsolutePath();

      Node localBigFile = testLocalBigFiles.addNode("bigFile", "nt:file");
      Node contentNode = localBigFile.addNode("jcr:content", "nt:resource");
      InputStream is = new FileInputStream(path);
      PropertyImpl data = (PropertyImpl)contentNode.setProperty("jcr:data", is);
      contentNode.setProperty("jcr:mimeType", "application/octet-stream ");
      contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
      session.save();
      // Load cache first
      QPathEntry qpeProp = new QPathEntry(Constants.NS_JCR_URI, "data", 0);
      ExactQPathEntryFilter filter = new ExactQPathEntryFilter(qpeProp);
      List<QPathEntryFilter> itemDataFilters = Collections.singletonList((QPathEntryFilter)filter);
      cwdm.getChildPropertiesData(data.parentData(), itemDataFilters);
      PropertyData pData = (PropertyData)cwdm.getCachedItemData(data.parentData(), qpeProp, ItemType.PROPERTY);

      assertNotNull(pData);
      List<ValueData> vals = pData.getValues();
      assertTrue(vals != null);
      assertFalse(vals.isEmpty());
      assertTrue(vals.get(0) instanceof StreamPersistedValueData);
      StreamPersistedValueData fpvd = (StreamPersistedValueData)vals.get(0);
      assertNotNull(fpvd.getFile());
      PropertyData pData2 = null;
      for (PropertyData pd : cwdm.getChildPropertiesData(data.parentData(), itemDataFilters))
      {
         if (pd.getIdentifier().equals(data.getInternalIdentifier()))
         {
            pData2 = pd;
            break;
         }
      }
      assertTrue(pData2 == pData);

      // Simulate cases where the file is null such as during a replication of StreamPersistedValueData with a spool file as file
      // on all other cluster nodes the spool file doesn't exist so file will be set to null
      fpvd.setPersistedFile(null);

      pData2 = null;
      for (PropertyData pd : cwdm.getChildPropertiesData(data.parentData(), itemDataFilters))
      {
         if (pd.getIdentifier().equals(data.getInternalIdentifier()))
         {
            pData2 = pd;
            break;
         }
      }
      assertFalse(pData2 == pData);
      pData2 = null;
      for (PropertyData pd : cwdm.getChildPropertiesData(data.parentData(), itemDataFilters))
      {
         if (pd.getIdentifier().equals(data.getInternalIdentifier()))
         {
            pData2 = pd;
            break;
         }
      }
      assertTrue(pData2 == cwdm.getCachedItemData(data.parentData(), qpeProp, ItemType.PROPERTY));
   }

   private static interface MyTask
   {
      void execute() throws Exception;
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

      public void remove(String identifier, ItemData item)
      {
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

      public void add(PropertyData data) throws RepositoryException, UnsupportedOperationException,
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

      public void delete(PropertyData data) throws RepositoryException, UnsupportedOperationException,
         InvalidItemStateException, IllegalStateException
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
         return Arrays
            .asList((PropertyData)new PersistedPropertyData("getChildPropertiesData", null, null, 0,
               PropertyType.STRING, false,
               Arrays.asList((ValueData)new ByteArrayPersistedValueData(1, "foo".getBytes()))));
      }

      public List<PropertyData> getChildPropertiesData(NodeData parent, List<QPathEntryFilter> pattern)
         throws RepositoryException, IllegalStateException
      {
         getChildPropertiesDataCalls.incrementAndGet();
         return Arrays
            .asList((PropertyData)new PersistedPropertyData("getChildPropertiesDataByPattern", null, null, 0,
               PropertyType.STRING, false, Arrays
                  .asList((ValueData)new ByteArrayPersistedValueData(1, "foo".getBytes()))));
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
         return Arrays
            .asList((PropertyData)new PersistedPropertyData("getReferencesData", null, null, 0, PropertyType.STRING,
               false, Arrays.asList((ValueData)new ByteArrayPersistedValueData(1, "foo".getBytes()))));
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
         return Arrays
            .asList((PropertyData)new PersistedPropertyData("listChildPropertiesData", null, null, 0,
               PropertyType.STRING, false,
               Arrays.asList((ValueData)new ByteArrayPersistedValueData(1, "foo".getBytes()))));
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

      public void update(PropertyData data) throws RepositoryException, UnsupportedOperationException,
         InvalidItemStateException, IllegalStateException
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

      public boolean getChildNodesDataByPage(NodeData parent, int fromOrderNum, int offset, int pageSize, List<NodeData> childs)
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
