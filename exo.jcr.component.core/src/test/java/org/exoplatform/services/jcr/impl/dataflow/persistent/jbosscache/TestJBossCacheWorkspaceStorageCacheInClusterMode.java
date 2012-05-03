/*
 * Copyright (C) 2011 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.dataflow.persistent.jbosscache;

import org.exoplatform.container.configuration.ConfigurationManagerImpl;
import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.config.CacheEntry;
import org.exoplatform.services.jcr.config.SimpleParameterEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedNodeData;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedPropertyData;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.persistent.ByteArrayPersistedValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.CacheableWorkspaceDataManager;
import org.exoplatform.services.jcr.impl.storage.SystemDataContainerHolder;
import org.exoplatform.services.jcr.impl.storage.WorkspaceDataContainerBase;
import org.exoplatform.services.jcr.storage.WorkspaceDataContainer;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;
import org.exoplatform.services.transaction.TransactionService;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import javax.jcr.InvalidItemStateException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:nicolas.filotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 */
public class TestJBossCacheWorkspaceStorageCacheInClusterMode extends JcrImplBaseTest
{

   public JBossCacheWorkspaceStorageCache getCacheImpl() throws Exception
   {
      TransactionService transactionService =
         (TransactionService)container.getComponentInstanceOfType(TransactionService.class);

      ArrayList<SimpleParameterEntry> list = new ArrayList<SimpleParameterEntry>();
      list.add(new SimpleParameterEntry(JBossCacheWorkspaceStorageCache.JBOSSCACHE_CONFIG,
         "jar:/conf/standalone/cluster/test-jbosscache-data-no-mux.xml"));
      list.add(new SimpleParameterEntry("jbosscache-cluster-name", "TestJBossCacheWorkspaceStorageCacheInClusterMode"));
      
      CacheEntry entry = new CacheEntry(list);
      WorkspaceEntry workspaceEntry = new WorkspaceEntry();
      workspaceEntry.setCache(entry);
      return new JBossCacheWorkspaceStorageCache(workspaceEntry,
         transactionService == null ? null : transactionService, new ConfigurationManagerImpl());
   }

   public void testRaceConditionsNConsistency() throws Exception
   {
      JBossCacheWorkspaceStorageCache cache1 = null, cache2 = null;
      try
      {
         MyWorkspaceSC con = new MyWorkspaceSC();
         WorkspaceDataContainer wdc = new MyWorkspaceDataContainer(con);
         CacheableWorkspaceDataManager cwdmNode1 =
            new CacheableWorkspaceDataManager(wdc, cache1 = getCacheImpl(), new SystemDataContainerHolder(wdc));
         CacheableWorkspaceDataManager cwdmNode2 =
            new CacheableWorkspaceDataManager(wdc, cache2 = getCacheImpl(), new SystemDataContainerHolder(wdc));
         NodeData parentNode = new PersistedNodeData("parent-id", QPath.makeChildPath(Constants.ROOT_PATH, new InternalQName(null, "parent-node")), Constants.ROOT_UUID, 1, 0,
            Constants.NT_UNSTRUCTURED, new InternalQName[0], null);
         // Test getChildNodesData
         Action readAction = new Action(cwdmNode2)
         {
            @Override
            public void execute(NodeData parentNode) throws Exception
            {
               cwdm.getChildNodesData(parentNode);
            }
         };
         Action writeAction = new Action(cwdmNode1)
         {
            @Override
            public void execute(NodeData parentNode) throws Exception
            {
               PlainChangesLog chlog = new PlainChangesLogImpl();            
               cwdm.getChildNodesData(parentNode);
               chlog.add(ItemState.createAddedState(new PersistedNodeData("id-node" + parentNode.getIdentifier(), QPath.makeChildPath(parentNode.getQPath(), new InternalQName(null, "node")), parentNode.getIdentifier(), 1, 0,
                  Constants.NT_UNSTRUCTURED, new InternalQName[0], null)));
               cwdm.save(chlog);            
            }
         };
         executeConcurrentReadNWrite(con, readAction, writeAction, Mode.READ_FIRST, parentNode);
         assertNotNull(cwdmNode1.getChildNodesData(parentNode));
         assertEquals(2, cwdmNode1.getChildNodesData(parentNode).size());
         assertNotNull(cwdmNode2.getChildNodesData(parentNode));
         assertEquals(2, cwdmNode2.getChildNodesData(parentNode).size());
         parentNode = new PersistedNodeData("parent-id2", QPath.makeChildPath(Constants.ROOT_PATH, new InternalQName(null, "parent-node2")), Constants.ROOT_UUID, 1, 0,
            Constants.NT_UNSTRUCTURED, new InternalQName[0], null);
         executeConcurrentReadNWrite(con, readAction, writeAction, Mode.WRITE_FIRST, parentNode);
         assertNotNull(cwdmNode1.getChildNodesData(parentNode));
         assertEquals(2, cwdmNode1.getChildNodesData(parentNode).size());
         assertNotNull(cwdmNode2.getChildNodesData(parentNode));
         assertEquals(2, cwdmNode2.getChildNodesData(parentNode).size());
         // Test getChildPropertiesData
         readAction = new Action(cwdmNode2)
         {
            @Override
            public void execute(NodeData parentNode) throws Exception
            {
               cwdm.getChildPropertiesData(parentNode);
            }
         };
         writeAction = new Action(cwdmNode1)
         {
            @Override
            public void execute(NodeData parentNode) throws Exception
            {
               PlainChangesLog chlog = new PlainChangesLogImpl();            
               cwdm.getChildPropertiesData(parentNode);
               chlog.add(ItemState.createAddedState(new PersistedPropertyData("id-property" + parentNode.getIdentifier(), QPath.makeChildPath(
                  parentNode.getQPath(), new InternalQName(null, "property")), parentNode.getIdentifier(), 0,
                  PropertyType.STRING, false, Arrays.asList((ValueData)new ByteArrayPersistedValueData(0, "some data".getBytes("UTF-8"))))));
               cwdm.save(chlog);
            }
         };      
         parentNode = new PersistedNodeData("parent-id3", QPath.makeChildPath(Constants.ROOT_PATH, new InternalQName(null, "parent-node3")), Constants.ROOT_UUID, 1, 0,
            Constants.NT_UNSTRUCTURED, new InternalQName[0], null);      
         executeConcurrentReadNWrite(con, readAction, writeAction, Mode.READ_FIRST, parentNode);
         assertNotNull(cwdmNode1.getChildPropertiesData(parentNode));
         assertEquals(2, cwdmNode1.getChildPropertiesData(parentNode).size());
         assertNotNull(cwdmNode2.getChildPropertiesData(parentNode));
         assertEquals(2, cwdmNode2.getChildPropertiesData(parentNode).size());
         parentNode = new PersistedNodeData("parent-id4", QPath.makeChildPath(Constants.ROOT_PATH, new InternalQName(null, "parent-node4")), Constants.ROOT_UUID, 1, 0,
            Constants.NT_UNSTRUCTURED, new InternalQName[0], null);
         executeConcurrentReadNWrite(con, readAction, writeAction, Mode.WRITE_FIRST, parentNode);
         assertNotNull(cwdmNode1.getChildPropertiesData(parentNode));
         assertEquals(2, cwdmNode1.getChildPropertiesData(parentNode).size());
         assertNotNull(cwdmNode2.getChildPropertiesData(parentNode));
         assertEquals(2, cwdmNode2.getChildPropertiesData(parentNode).size());
         // Test getReferencesData
         readAction = new Action(cwdmNode2)
         {
            @Override
            public void execute(NodeData parentNode) throws Exception
            {
               cwdm.getReferencesData(parentNode.getIdentifier(), false);
            }
         };
         writeAction = new Action(cwdmNode1)
         {
            @Override
            public void execute(NodeData parentNode) throws Exception
            {
               PlainChangesLog chlog = new PlainChangesLogImpl();            
               cwdm.getReferencesData(parentNode.getIdentifier(), false);
               chlog.add(ItemState.createAddedState(new PersistedPropertyData("id-reference" + parentNode.getIdentifier(), QPath.makeChildPath(
                  parentNode.getQPath(), new InternalQName(null, "reference")), parentNode.getIdentifier(), 0,
                  PropertyType.REFERENCE, false, Arrays.asList((ValueData)new ByteArrayPersistedValueData(0, parentNode.getIdentifier().getBytes("UTF-8"))))));
               cwdm.save(chlog);
            }
         };      
         parentNode = new PersistedNodeData("parent-id5", QPath.makeChildPath(Constants.ROOT_PATH, new InternalQName(null, "parent-node5")), Constants.ROOT_UUID, 1, 0,
            Constants.NT_UNSTRUCTURED, new InternalQName[0], null);      
         executeConcurrentReadNWrite(con, readAction, writeAction, Mode.READ_FIRST, parentNode);
         assertNotNull(cwdmNode1.getReferencesData(parentNode.getIdentifier(), false));
         assertEquals(2, cwdmNode1.getReferencesData(parentNode.getIdentifier(), false).size());
         assertNotNull(cwdmNode2.getReferencesData(parentNode.getIdentifier(), false));
         assertEquals(2, cwdmNode2.getReferencesData(parentNode.getIdentifier(), false).size());
         parentNode = new PersistedNodeData("parent-id6", QPath.makeChildPath(Constants.ROOT_PATH, new InternalQName(null, "parent-node6")), Constants.ROOT_UUID, 1, 0,
            Constants.NT_UNSTRUCTURED, new InternalQName[0], null);
         executeConcurrentReadNWrite(con, readAction, writeAction, Mode.WRITE_FIRST, parentNode);
         assertNotNull(cwdmNode1.getReferencesData(parentNode.getIdentifier(), false));
         assertEquals(2, cwdmNode1.getReferencesData(parentNode.getIdentifier(), false).size());
         assertNotNull(cwdmNode2.getReferencesData(parentNode.getIdentifier(), false));
         assertEquals(2, cwdmNode2.getReferencesData(parentNode.getIdentifier(), false).size()); 
         
         // Test getItemData by Id
         readAction = new Action(cwdmNode2)
         {
            @Override
            public void execute(NodeData parentNode) throws Exception
            {
               cwdm.getItemData(parentNode.getIdentifier());
            }
         };
         writeAction = new Action(cwdmNode1)
         {
            @Override
            public void execute(NodeData parentNode) throws Exception
            {
               PlainChangesLog chlog = new PlainChangesLogImpl();
               cwdm.getItemData(parentNode.getIdentifier());
               chlog.add(ItemState.createUpdatedState(new PersistedNodeData(parentNode.getIdentifier(), parentNode.getQPath(), Constants.ROOT_UUID, 2, 1,
                  Constants.NT_UNSTRUCTURED, new InternalQName[0], null)));
               cwdm.save(chlog);
            }
         };      
         parentNode = new PersistedNodeData("parent-id7", QPath.makeChildPath(Constants.ROOT_PATH, new InternalQName(null, "parent-node7")), Constants.ROOT_UUID, 1, 0,
            Constants.NT_UNSTRUCTURED, new InternalQName[0], null);      
         executeConcurrentReadNWrite(con, readAction, writeAction, Mode.READ_FIRST, parentNode);
         assertNotNull(cwdmNode1.getItemData(parentNode.getIdentifier()));
         assertEquals(2, cwdmNode1.getItemData(parentNode.getIdentifier()).getPersistedVersion());
         assertNotNull(cwdmNode2.getItemData(parentNode.getIdentifier()));
         assertEquals(2, cwdmNode2.getItemData(parentNode.getIdentifier()).getPersistedVersion());
         parentNode = new PersistedNodeData("parent-id8", QPath.makeChildPath(Constants.ROOT_PATH, new InternalQName(null, "parent-node8")), Constants.ROOT_UUID, 1, 0,
            Constants.NT_UNSTRUCTURED, new InternalQName[0], null);
         executeConcurrentReadNWrite(con, readAction, writeAction, Mode.WRITE_FIRST, parentNode);
         assertNotNull(cwdmNode1.getItemData(parentNode.getIdentifier()));
         assertEquals(2, cwdmNode1.getItemData(parentNode.getIdentifier()).getPersistedVersion());
         assertNotNull(cwdmNode2.getItemData(parentNode.getIdentifier()));
         assertEquals(2, cwdmNode2.getItemData(parentNode.getIdentifier()).getPersistedVersion()); 
              
         // Test getItemData by Path
         final QPathEntry qpe = new QPathEntry(null, "my-property", 1);
         readAction = new Action(cwdmNode2)
         {
            @Override
            public void execute(NodeData parentNode) throws Exception
            {
               cwdm.getItemData(parentNode, qpe, ItemType.PROPERTY);
            }
         };
         writeAction = new Action(cwdmNode1)
         {
            @Override
            public void execute(NodeData parentNode) throws Exception
            {
               PlainChangesLog chlog = new PlainChangesLogImpl();
               cwdm.getItemData(parentNode, qpe, ItemType.PROPERTY);
               chlog.add(ItemState.createUpdatedState(new PersistedPropertyData("property-by-path"
                  + parentNode.getIdentifier(), QPath.makeChildPath(parentNode.getQPath(), qpe), parentNode
                  .getIdentifier(), 2, PropertyType.STRING, false, Arrays
                  .asList((ValueData)new ByteArrayPersistedValueData(0, "some new data".getBytes("UTF-8"))))));
               cwdm.save(chlog);
            }
         };      
         parentNode = new PersistedNodeData("parent-id9", QPath.makeChildPath(Constants.ROOT_PATH, new InternalQName(null, "parent-node9")), Constants.ROOT_UUID, 1, 0,
            Constants.NT_UNSTRUCTURED, new InternalQName[0], null);      
         executeConcurrentReadNWrite(con, readAction, writeAction, Mode.READ_FIRST, parentNode);
         assertNotNull(cwdmNode1.getItemData(parentNode, qpe, ItemType.PROPERTY));
         assertEquals(2, cwdmNode1.getItemData(parentNode, qpe, ItemType.PROPERTY).getPersistedVersion());
         assertNotNull(cwdmNode2.getItemData(parentNode, qpe, ItemType.PROPERTY));
         assertEquals(2, cwdmNode2.getItemData(parentNode, qpe, ItemType.PROPERTY).getPersistedVersion());
         parentNode = new PersistedNodeData("parent-id10", QPath.makeChildPath(Constants.ROOT_PATH, new InternalQName(null, "parent-node10")), Constants.ROOT_UUID, 1, 0,
            Constants.NT_UNSTRUCTURED, new InternalQName[0], null);
         executeConcurrentReadNWrite(con, readAction, writeAction, Mode.WRITE_FIRST, parentNode);
         assertNotNull(cwdmNode1.getItemData(parentNode, qpe, ItemType.PROPERTY));
         assertEquals(2, cwdmNode1.getItemData(parentNode, qpe, ItemType.PROPERTY).getPersistedVersion());
         assertNotNull(cwdmNode2.getItemData(parentNode, qpe, ItemType.PROPERTY));
         assertEquals(2, cwdmNode2.getItemData(parentNode, qpe, ItemType.PROPERTY).getPersistedVersion());

         // testConsistency
         con = new MyWorkspaceSC(true);
         wdc = new MyWorkspaceDataContainer(con);
         cwdmNode1 = new CacheableWorkspaceDataManager(wdc, cache1, new SystemDataContainerHolder(wdc));
         cwdmNode2 = new CacheableWorkspaceDataManager(wdc, cache2, new SystemDataContainerHolder(wdc));
         parentNode =
            new PersistedNodeData("parent2-id", QPath.makeChildPath(Constants.ROOT_PATH, new InternalQName(null,
               "parent2-node")), Constants.ROOT_UUID, 1, 0, Constants.NT_UNSTRUCTURED, new InternalQName[0], null);

         // Test getChildNodesData
         con.setParentNode(parentNode);
         cwdmNode2.getChildNodesData(parentNode);
         PlainChangesLog chlog = new PlainChangesLogImpl();
         chlog.add(ItemState.createAddedState(new PersistedNodeData("id-node" + parentNode.getIdentifier(), QPath
            .makeChildPath(parentNode.getQPath(), new InternalQName(null, "node")), parentNode.getIdentifier(), 1, 0,
            Constants.NT_UNSTRUCTURED, new InternalQName[0], null)));
         cwdmNode1.save(chlog);
         assertNotNull(cwdmNode1.getChildNodesData(parentNode));
         assertEquals(2, cwdmNode1.getChildNodesData(parentNode).size());
         assertNotNull(cwdmNode2.getChildNodesData(parentNode));
         assertEquals(2, cwdmNode2.getChildNodesData(parentNode).size());
         parentNode =
            new PersistedNodeData("parent2-id2", QPath.makeChildPath(Constants.ROOT_PATH, new InternalQName(null,
               "parent2-node2")), Constants.ROOT_UUID, 1, 0, Constants.NT_UNSTRUCTURED, new InternalQName[0], null);
         con.setParentNode(parentNode);
         cwdmNode2.getChildNodesData(parentNode);
         chlog = new PlainChangesLogImpl();
         chlog.add(ItemState.createDeletedState(new PersistedNodeData("id-node2" + parentNode.getIdentifier(), QPath
            .makeChildPath(parentNode.getQPath(), new InternalQName(null, "node2")), parentNode.getIdentifier(), 1, 0,
            Constants.NT_UNSTRUCTURED, new InternalQName[0], null)));
         cwdmNode1.save(chlog);
         assertNotNull(cwdmNode1.getChildNodesData(parentNode));
         assertEquals(0, cwdmNode1.getChildNodesData(parentNode).size());
         assertNotNull(cwdmNode2.getChildNodesData(parentNode));
         assertEquals(0, cwdmNode2.getChildNodesData(parentNode).size());

         // Test getChildPropertiesData
         parentNode =
            new PersistedNodeData("parent2-id3", QPath.makeChildPath(Constants.ROOT_PATH, new InternalQName(null,
               "parent2-node3")), Constants.ROOT_UUID, 1, 0, Constants.NT_UNSTRUCTURED, new InternalQName[0], null);
         con.setParentNode(parentNode);
         cwdmNode2.getChildPropertiesData(parentNode);
         chlog = new PlainChangesLogImpl();
         chlog.add(ItemState.createAddedState(new PersistedPropertyData("id-property" + parentNode.getIdentifier(),
            QPath.makeChildPath(parentNode.getQPath(), new InternalQName(null, "property")),
            parentNode.getIdentifier(), 0, PropertyType.STRING, false, Arrays
               .asList((ValueData)new ByteArrayPersistedValueData(0, "some data".getBytes("UTF-8"))))));
         cwdmNode1.save(chlog);
         assertNotNull(cwdmNode1.getChildPropertiesData(parentNode));
         assertEquals(2, cwdmNode1.getChildPropertiesData(parentNode).size());
         assertNotNull(cwdmNode2.getChildPropertiesData(parentNode));
         assertEquals(2, cwdmNode2.getChildPropertiesData(parentNode).size());
         parentNode =
            new PersistedNodeData("parent2-id4", QPath.makeChildPath(Constants.ROOT_PATH, new InternalQName(null,
               "parent2-node4")), Constants.ROOT_UUID, 1, 0, Constants.NT_UNSTRUCTURED, new InternalQName[0], null);
         con.setParentNode(parentNode);
         cwdmNode2.getChildPropertiesData(parentNode);
         chlog = new PlainChangesLogImpl();
         chlog.add(ItemState.createDeletedState(new PersistedPropertyData("id-property2" + parentNode.getIdentifier(),
            QPath.makeChildPath(parentNode.getQPath(), new InternalQName(null, "property2")), parentNode
               .getIdentifier(), 0, PropertyType.STRING, false, Arrays
               .asList((ValueData)new ByteArrayPersistedValueData(0, "some data".getBytes("UTF-8"))))));
         cwdmNode1.save(chlog);
         assertNotNull(cwdmNode1.getChildPropertiesData(parentNode));
         assertEquals(0, cwdmNode1.getChildPropertiesData(parentNode).size());
         assertNotNull(cwdmNode2.getChildPropertiesData(parentNode));
         assertEquals(0, cwdmNode2.getChildPropertiesData(parentNode).size());

         // Test getReferencesData
         parentNode =
            new PersistedNodeData("parent2-id5", QPath.makeChildPath(Constants.ROOT_PATH, new InternalQName(null,
               "parent2-node5")), Constants.ROOT_UUID, 1, 0, Constants.NT_UNSTRUCTURED, new InternalQName[0], null);
         con.setParentNode(parentNode);
         cwdmNode2.getReferencesData(parentNode.getIdentifier(), false);
         chlog = new PlainChangesLogImpl();
         chlog.add(ItemState.createAddedState(new PersistedPropertyData("id-reference" + parentNode.getIdentifier(),
            QPath.makeChildPath(parentNode.getQPath(), new InternalQName(null, "reference")), parentNode
               .getIdentifier(), 0, PropertyType.REFERENCE, false, Arrays
               .asList((ValueData)new ByteArrayPersistedValueData(0, parentNode.getIdentifier().getBytes("UTF-8"))))));
         cwdmNode1.save(chlog);
         assertNotNull(cwdmNode1.getReferencesData(parentNode.getIdentifier(), false));
         assertEquals(2, cwdmNode1.getReferencesData(parentNode.getIdentifier(), false).size());
         assertNotNull(cwdmNode2.getReferencesData(parentNode.getIdentifier(), false));
         assertEquals(2, cwdmNode2.getReferencesData(parentNode.getIdentifier(), false).size());
         parentNode =
            new PersistedNodeData("parent2-id6", QPath.makeChildPath(Constants.ROOT_PATH, new InternalQName(null,
               "parent2-node6")), Constants.ROOT_UUID, 1, 0, Constants.NT_UNSTRUCTURED, new InternalQName[0], null);
         con.setParentNode(parentNode);
         cwdmNode2.getReferencesData(parentNode.getIdentifier(), false);
         chlog = new PlainChangesLogImpl();
         chlog.add(ItemState.createDeletedState(new PersistedPropertyData("id-reference2" + parentNode.getIdentifier(),
            QPath.makeChildPath(parentNode.getQPath(), new InternalQName(null, "reference2")), parentNode
               .getIdentifier(), 0, PropertyType.REFERENCE, false, Arrays
               .asList((ValueData)new ByteArrayPersistedValueData(0, parentNode.getIdentifier().getBytes("UTF-8"))))));
         cwdmNode1.save(chlog);
         assertNotNull(cwdmNode1.getReferencesData(parentNode.getIdentifier(), false));
         assertEquals(0, cwdmNode1.getReferencesData(parentNode.getIdentifier(), false).size());
         assertNotNull(cwdmNode2.getReferencesData(parentNode.getIdentifier(), false));
         assertEquals(0, cwdmNode2.getReferencesData(parentNode.getIdentifier(), false).size());
      }
      finally
      {
         if (cache1 != null)
         {
            try
            {
               cache1.cache.stop();
            }
            catch (Exception e)
            {
               // ignore me
            }
         }
         if (cache2 != null)
         {
            try
            {
               cache2.cache.stop();
            }
            catch (Exception e)
            {
               // ignore me
            }
         }
      }
   }

   /**
    * @param con
    * @param cwdm
    * @param mode
    * @param idNode
    * @throws InterruptedException
    */
   private void executeConcurrentReadNWrite(final MyWorkspaceSC con, final Action readAction,
      final Action writeAction, final Mode mode, final NodeData parentNode) throws InterruptedException
   {
      final CountDownLatch goSignal = con.initCountDownLatch();
      con.setParentNode(parentNode);
      final AtomicReference<Exception> ex = new AtomicReference<Exception>();
      final CountDownLatch startSignal = new CountDownLatch(1);
      final CountDownLatch doneSignal = new CountDownLatch(2);
      Thread writer = new Thread()
      {
         @Override
         public void run()
         {
            try
            {
               startSignal.await();
               con.wait.set(mode != Mode.WRITE_FIRST);
               writeAction.execute(parentNode);
            }
            catch (Exception e)
            {
               e.printStackTrace();               
               ex.set(e);
            }
            finally
            {
               if (mode == Mode.WRITE_FIRST) goSignal.countDown();
               doneSignal.countDown();

               con.wait.remove();
            }
         }
      };
      writer.start();
      Thread reader = new Thread()
      {
         @Override
         public void run()
         {
            try
            {
               startSignal.await();
               con.wait.set(mode != Mode.READ_FIRST);
               readAction.execute(parentNode);
            }
            catch (Exception e)
            {
               e.printStackTrace();
               ex.set(e);
            }
            finally
            {
               if (mode == Mode.READ_FIRST) goSignal.countDown();
               doneSignal.countDown();

               con.wait.remove();
            }            
         }
      };
      reader.start();
      startSignal.countDown();
      doneSignal.await();
      assertNull(ex.get());
   }   

   private abstract class Action
   {
    
      protected final CacheableWorkspaceDataManager cwdm;

      public Action(CacheableWorkspaceDataManager cwdm)
      {
         this.cwdm = cwdm;
      }
      protected abstract void execute(NodeData parentNode) throws Exception;
   }

   private static enum Mode
   {
      READ_FIRST, WRITE_FIRST;
   }

   public static class MyWorkspaceSC implements WorkspaceStorageConnection
   {
      public ThreadLocal<Boolean> wait = new ThreadLocal<Boolean>();

      private NodeData parentNode;

      private CountDownLatch goSignal;
      
      private ItemData itemAdded;

      private boolean canModify;

      private boolean itemDeleted;

      public MyWorkspaceSC()
      {
      }

      public MyWorkspaceSC(boolean canModify)
      {
         this.canModify = canModify;
      }

      /** 
       * @param canModify the canModify to set 
       */
      public void setCanModify(boolean canModify)
      {
         this.canModify = canModify;
      }

      public CountDownLatch initCountDownLatch()
      {
         return this.goSignal = new CountDownLatch(1);
      }
      
      public void setParentNode(NodeData parentNode)
      {
         this.parentNode = parentNode;
         this.itemAdded = null;
         this.itemDeleted = false;
      }
      
      public void add(NodeData data) throws RepositoryException, UnsupportedOperationException,
         InvalidItemStateException, IllegalStateException
      {
         this.itemAdded = data;
      }

      public void add(PropertyData data) throws RepositoryException, UnsupportedOperationException,
         InvalidItemStateException, IllegalStateException
      {
         this.itemAdded = data;
      }

      public void close() throws IllegalStateException, RepositoryException
      {
      }

      public void commit() throws IllegalStateException, RepositoryException
      {
         if (wait.get() != null && wait.get())
         {
            try
            {
               goSignal.await();
            }
            catch (InterruptedException e)
            {
               Thread.currentThread().interrupt();
            }            
         }
      }

      public void delete(NodeData data) throws RepositoryException, UnsupportedOperationException,
         InvalidItemStateException, IllegalStateException
      {
         this.itemDeleted = true;
      }

      public void delete(PropertyData data) throws RepositoryException, UnsupportedOperationException,
         InvalidItemStateException, IllegalStateException
      {
         this.itemDeleted = true;
      }

      public int getChildNodesCount(NodeData parent) throws RepositoryException
      {
         return -1;
      }

      public List<NodeData> getChildNodesData(NodeData parent) throws RepositoryException, IllegalStateException
      {
         if (wait.get() != null && wait.get())
         {
            try
            {
               goSignal.await();
            }
            catch (InterruptedException e)
            {
               Thread.currentThread().interrupt();
            }            
         }
         List<NodeData> children = new ArrayList<NodeData>();
         if (!canModify || !itemDeleted)
         {
            children.add(new PersistedNodeData("id-node2" + parentNode.getIdentifier(), QPath.makeChildPath(
               parent.getQPath(), new InternalQName(null, "node2")), parent.getIdentifier(), 1, 0,
               Constants.NT_UNSTRUCTURED, new InternalQName[0], null));
         }
         if (canModify && itemAdded != null)
         {
            children.add((NodeData)itemAdded);
         }

         return children;
      }

      public List<PropertyData> getChildPropertiesData(NodeData parent) throws RepositoryException,
         IllegalStateException
      {
         if (wait.get() != null && wait.get())
         {
            try
            {
               goSignal.await();
            }
            catch (InterruptedException e)
            {
               Thread.currentThread().interrupt();
            }            
         }
         List<PropertyData> children = new ArrayList<PropertyData>();
         try
         {
            if (!canModify || !itemDeleted)
            {
               children.add(new PersistedPropertyData("id-property2" + parentNode.getIdentifier(), QPath.makeChildPath(
                  parentNode.getQPath(), new InternalQName(null, "property2")), parentNode.getIdentifier(), 0,
                  PropertyType.STRING, false, Arrays.asList((ValueData)new ByteArrayPersistedValueData(0, "some data"
                     .getBytes("UTF-8")))));
            }
            if (canModify && itemAdded != null)
            {
               children.add((PropertyData)itemAdded);
            }
         }
         catch (UnsupportedEncodingException e)
         {
            e.printStackTrace();
         }
         return children;
      }

      /**
       * {@inheritDoc}
       */
      public ItemData getItemData(NodeData parentData, QPathEntry name) throws RepositoryException,
         IllegalStateException
      {
         return getItemData(parentData, name, ItemType.UNKNOWN);
      }

      public ItemData getItemData(NodeData parentData, QPathEntry name, ItemType itemType) throws RepositoryException,
         IllegalStateException
      {
         if (wait.get() != null && wait.get())
         {
            try
            {
               goSignal.await();
            }
            catch (InterruptedException e)
            {
               Thread.currentThread().interrupt();
            }            
         }
         if (itemType == ItemType.NODE)
         {
            return new PersistedNodeData("my-node" + parentNode.getIdentifier(), QPath.makeChildPath(parentNode.getQPath(), name), Constants.ROOT_UUID, 1, 1,
               Constants.NT_UNSTRUCTURED, new InternalQName[0], null);
         }
         try
         {
            return new PersistedPropertyData("property-by-path"
               + parentNode.getIdentifier(), QPath.makeChildPath(parentNode.getQPath(), name), parentNode
               .getIdentifier(), 1, PropertyType.STRING, false, Arrays
               .asList((ValueData)new ByteArrayPersistedValueData(0, "some new data".getBytes("UTF-8"))));
         }
         catch (UnsupportedEncodingException e)
         {
            e.printStackTrace();
         }
         return null;
      }

      public ItemData getItemData(String identifier) throws RepositoryException, IllegalStateException
      {
         if (wait.get() != null && wait.get())
         {
            try
            {
               goSignal.await();
            }
            catch (InterruptedException e)
            {
               Thread.currentThread().interrupt();
            }            
         }
         return parentNode;
      }

      public List<PropertyData> getReferencesData(String nodeIdentifier) throws RepositoryException,
         IllegalStateException, UnsupportedOperationException
      {        
         if (wait.get() != null && wait.get())
         {
            try
            {
               goSignal.await();
            }
            catch (InterruptedException e)
            {
               Thread.currentThread().interrupt();
            }            
         }
         List<PropertyData> children = new ArrayList<PropertyData>();
         try
         {
            if (!canModify || !itemDeleted)
            {
               children
                  .add(new PersistedPropertyData("id-reference2" + parentNode.getIdentifier(), QPath.makeChildPath(
                     parentNode.getQPath(), new InternalQName(null, "reference2")), parentNode.getIdentifier(), 0,
                     PropertyType.REFERENCE, false, Arrays.asList((ValueData)new ByteArrayPersistedValueData(0,
                        parentNode.getIdentifier().getBytes("UTF-8")))));
            }
            if (canModify && itemAdded != null)
            {
               children.add((PropertyData)itemAdded);
            }
         }
         catch (UnsupportedEncodingException e)
         {
            e.printStackTrace();
         }
         return children;
      }

      public boolean isOpened()
      {
         return true;
      }

      public List<PropertyData> listChildPropertiesData(NodeData parent) throws RepositoryException,
         IllegalStateException
      {
         return null;
      }

      public void rename(NodeData data) throws RepositoryException, UnsupportedOperationException,
         InvalidItemStateException, IllegalStateException
      {
      }

      public void rollback() throws IllegalStateException, RepositoryException
      {
      }
      
      public void prepare() throws IllegalStateException, RepositoryException
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
   };   
}