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
package org.exoplatform.services.jcr.impl.dataflow.persistent.cache.jbosscache;

import org.exoplatform.container.configuration.ConfigurationManagerImpl;
import org.exoplatform.services.jcr.config.CacheEntry;
import org.exoplatform.services.jcr.config.SimpleParameterEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedNodeData;
import org.exoplatform.services.jcr.dataflow.persistent.WorkspaceStorageCache;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.itemfilters.QPathEntryFilter;
import org.exoplatform.services.jcr.impl.dataflow.persistent.ACLHolder;
import org.exoplatform.services.jcr.impl.dataflow.persistent.CacheableWorkspaceDataManager;
import org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspaceStorageCacheBaseCase;
import org.exoplatform.services.jcr.impl.dataflow.persistent.jbosscache.JBossCacheWorkspaceStorageCache;
import org.exoplatform.services.jcr.impl.storage.SystemDataContainerHolder;
import org.exoplatform.services.jcr.impl.storage.WorkspaceDataContainerBase;
import org.exoplatform.services.jcr.storage.WorkspaceDataContainer;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;
import org.exoplatform.services.transaction.TransactionService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import javax.jcr.InvalidItemStateException;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id$
 */
public class TestJBossCacheWorkspaceStorageCache extends WorkspaceStorageCacheBaseCase
{

   @Override
   public WorkspaceStorageCache getCacheImpl() throws Exception
   {
      TransactionService transactionService =
         (TransactionService)container.getComponentInstanceOfType(TransactionService.class);

      ArrayList<SimpleParameterEntry> list = new ArrayList<SimpleParameterEntry>();
      list.add(new SimpleParameterEntry(JBossCacheWorkspaceStorageCache.JBOSSCACHE_CONFIG,
         "jar:/conf/standalone/test-jbosscache-config.xml"));

      CacheEntry entry = new CacheEntry(list);
      entry.setEnabled(true);
      WorkspaceEntry workspaceEntry = new WorkspaceEntry();
      workspaceEntry.setUniqueName("WS_UUID");
      workspaceEntry.setCache(entry);
      return new JBossCacheWorkspaceStorageCache(workspaceEntry,
         transactionService == null ? null : transactionService, new ConfigurationManagerImpl());
   }
   
   public void testRaceConditions() throws Exception
   {
      MyWorkspaceStorageConnection con = new MyWorkspaceStorageConnection();
      WorkspaceDataContainer wdc = new MyWorkspaceDataContainer(con);
      WorkspaceContainerFacade wsc = repository.getWorkspaceContainer("ws");
      WorkspaceEntry wconf = (WorkspaceEntry)wsc.getComponent(WorkspaceEntry.class);
      final CacheableWorkspaceDataManager cwdm =
         new CacheableWorkspaceDataManager(wconf, wdc, getCacheImpl(), new SystemDataContainerHolder(wdc));
      String idNode = "foo1";
      executeConcurrentReadNWrite(con, cwdm, Mode.READ_FIRST, idNode);
      assertNotNull(cwdm.getItemData(idNode));
      idNode = "foo2";
      executeConcurrentReadNWrite(con, cwdm, Mode.WRITE_FIRST, idNode);
      assertNotNull(cwdm.getItemData(idNode));
   }

   /**
    * @param con
    * @param cwdm
    * @param mode
    * @param idNode
    * @throws InterruptedException
    */
   private void executeConcurrentReadNWrite(MyWorkspaceStorageConnection con, final CacheableWorkspaceDataManager cwdm,
      final Mode mode, final String idNode) throws InterruptedException
   {
      final CountDownLatch goSignal = con.setMode(mode);
      final AtomicReference<Exception> ex = new AtomicReference<Exception>();
      final CountDownLatch startSignal = new CountDownLatch(1);
      final CountDownLatch doneSignal = new CountDownLatch(2);
      Thread writer = new Thread()
      {
         public void run()
         {
            try
            {
               startSignal.await();
               PlainChangesLog chlog = new PlainChangesLogImpl();
               chlog.add(ItemState.createAddedState(new PersistedNodeData(idNode, Constants.ROOT_PATH, "parent-id", 1, 0,
                  Constants.NT_UNSTRUCTURED, null, null)));

               if (mode == Mode.READ_FIRST)
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

               cwdm.save(chlog);
            }
            catch (Exception e)
            {
               ex.set(e);
            }
            finally
            {
               if (mode == Mode.WRITE_FIRST) goSignal.countDown();
               doneSignal.countDown();
            }
         }
      };
      writer.start();
      Thread reader = new Thread()
      {
         public void run()
         {
            try
            {
               startSignal.await();
               cwdm.getItemData(idNode);
            }
            catch (Exception e)
            {
               ex.set(e);
            }
            finally
            {
               if (mode == Mode.READ_FIRST) goSignal.countDown();
               doneSignal.countDown();
            }            
         }
      };
      reader.start();
      startSignal.countDown();
      doneSignal.await();
      assertNull(ex.get());
   }   

   private static enum Mode
   {
      READ_FIRST, WRITE_FIRST;
   }
   private static class MyWorkspaceStorageConnection implements WorkspaceStorageConnection
   {

      private Mode mode;
      private CountDownLatch goSignal;
      
      public CountDownLatch setMode(Mode mode)
      {
         this.mode = mode;
         this.goSignal = new CountDownLatch(1);
         return goSignal;
      }
      
      
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

      public void delete(NodeData data) throws RepositoryException, UnsupportedOperationException,
         InvalidItemStateException, IllegalStateException
      {
      }

      public void delete(PropertyData data) throws RepositoryException, UnsupportedOperationException,
         InvalidItemStateException, IllegalStateException
      {
      }

      public int getChildNodesCount(NodeData parent) throws RepositoryException
      {
         return -1;
      }

      public List<NodeData> getChildNodesData(NodeData parent) throws RepositoryException, IllegalStateException
      {
         return null;
      }

      public List<PropertyData> getChildPropertiesData(NodeData parent) throws RepositoryException,
         IllegalStateException
      {
         return null;
      }

      public List<PropertyData> getChildPropertiesData(NodeData parent, List<QPathEntryFilter> pattern)
         throws RepositoryException, IllegalStateException
      {
         return null;
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
         return null;
      }

      public ItemData getItemData(String identifier) throws RepositoryException, IllegalStateException
      {
         if (mode == Mode.WRITE_FIRST)
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
         return null;
      }

      public List<PropertyData> getReferencesData(String nodeIdentifier) throws RepositoryException,
         IllegalStateException, UnsupportedOperationException
      {
         return null;
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
         return getChildNodesData(parent);
      }

      public ValueData getValue(String propertyId, int orderNumb, int persistedVersion) throws IllegalStateException,
         RepositoryException
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
