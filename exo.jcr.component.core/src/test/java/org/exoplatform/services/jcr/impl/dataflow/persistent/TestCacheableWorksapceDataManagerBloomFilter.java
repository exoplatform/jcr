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
package org.exoplatform.services.jcr.impl.dataflow.persistent;

import org.exoplatform.commons.utils.QName;
import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedNodeData;
import org.exoplatform.services.jcr.dataflow.persistent.WorkspaceStorageCache;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.storage.SystemDataContainerHolder;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:skarpenko@exoplatform.com">Sergiy Karpenko</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 18.08.2011 skarpenko $
 *
 */
public class TestCacheableWorksapceDataManagerBloomFilter extends JcrImplBaseTest
{
   private MyWorkspaceDataContainer dataContainer;

   private CacheableWorkspaceDataManager mgr;

   public void setUp() throws Exception
   {
      super.setUp();
      dataContainer = new MyWorkspaceDataContainer();
      WorkspaceStorageCache cache = new MyWorkspaceStorageCache();
      WorkspaceContainerFacade wsc = repository.getWorkspaceContainer("ws");
      WorkspaceEntry wconf = (WorkspaceEntry)wsc.getComponent(WorkspaceEntry.class);
      mgr =
         new CacheableWorkspaceDataManager(wconf, dataContainer, cache, new SystemDataContainerHolder(dataContainer));
   }

   protected void tearDown() throws Exception
   {
      dataContainer.clear();
      dataContainer = null;
      mgr.clear();
      mgr = null;
      super.tearDown();
   }

   public void testGetItemWithACL() throws Exception
   {
      QPath path;
      dataContainer.add(new PersistedNodeData("2", path = new QPath(new QPathEntry[]{new QPathEntry("", "2", 1, "2")}),
         "3", 1, 1, null, null, new AccessControlList("owner", new ArrayList<AccessControlEntry>())));
      dataContainer.add(new PersistedNodeData("1", path = QPath.makeChildPath(path, new QName("", "1"), 1, "1"), "2",
         1, 1, null, null, new AccessControlList("owner", new ArrayList<AccessControlEntry>())));
      mgr.reloadFilters();
      mgr.getItemData("1");
      assertEquals(1, dataContainer.getTotalCalls());
      dataContainer.clear();
   }

   public void testGetItemWithoutACL() throws Exception
   {
      QPath path;
      dataContainer.add(new PersistedNodeData("3", path = new QPath(new QPathEntry[]{new QPathEntry("", "3", 1, "3")}),
         "4", 1, 1, null, null, new AccessControlList("owner", new ArrayList<AccessControlEntry>())));
      dataContainer.add(new PersistedNodeData("2", path = QPath.makeChildPath(path, new QName("", "2"), 1, "2"), "3",
         1, 1, null, null, new AccessControlList("owner", new ArrayList<AccessControlEntry>())));
      dataContainer.add(new PersistedNodeData("1", path = QPath.makeChildPath(path, new QName("", "1"), 1, "1"), "2",
         1, 1, null, null, null));
      mgr.reloadFilters();
      mgr.getItemData("1");
      assertEquals(2, dataContainer.getTotalCalls());
      dataContainer.clear();
   }

   public void testGetItemParentWithSingleOwner() throws Exception
   {
      QPath path;
      dataContainer.add(new PersistedNodeData("4", path = new QPath(new QPathEntry[]{new QPathEntry("", "4", 1, "4")}),
         "5", 1, 1, null, null, new AccessControlList("owner", new ArrayList<AccessControlEntry>())));
      dataContainer.add(new PersistedNodeData("3", path = QPath.makeChildPath(path, new QName("", "3"), 1, "3"), "4",
         1, 1, null, null, new AccessControlList("owner", new ArrayList<AccessControlEntry>())));
      dataContainer.add(new PersistedNodeData("2", path = QPath.makeChildPath(path, new QName("", "2"), 1, "2"), "3",
         1, 1, null, null, new AccessControlList("owner", null)));
      dataContainer.add(new PersistedNodeData("1", path = QPath.makeChildPath(path, new QName("", "1"), 1, "1"), "2",
         1, 1, null, null, null));
      mgr.reloadFilters();
      mgr.getItemData("1");
      assertEquals(3, dataContainer.getTotalCalls());
      dataContainer.clear();
   }

   public void testGetItemParentWithSinglePermission() throws Exception
   {
      QPath path;
      dataContainer.add(new PersistedNodeData("4", path = new QPath(new QPathEntry[]{new QPathEntry("", "4", 1, "4")}),
         "5", 1, 1, null, null, new AccessControlList("owner", new ArrayList<AccessControlEntry>())));
      dataContainer.add(new PersistedNodeData("3", path = QPath.makeChildPath(path, new QName("", "3"), 1, "3"), "4",
         1, 1, null, null, new AccessControlList("owner", new ArrayList<AccessControlEntry>())));
      dataContainer.add(new PersistedNodeData("2", path = QPath.makeChildPath(path, new QName("", "2"), 1, "2"), "3",
         1, 1, null, null, new AccessControlList(null, new ArrayList<AccessControlEntry>())));
      dataContainer.add(new PersistedNodeData("1", path = QPath.makeChildPath(path, new QName("", "1"), 1, "1"), "2",
         1, 1, null, null, null));
      mgr.reloadFilters();
      mgr.getItemData("1");
      assertEquals(3, dataContainer.getTotalCalls());
      dataContainer.clear();
   }

   public void testGetItemParentsWithCrossEmptyACL() throws Exception
   {
      QPath path;
      dataContainer.add(new PersistedNodeData("4", path = new QPath(new QPathEntry[]{new QPathEntry("", "4", 1, "4")}),
         "5", 1, 1, null, null, new AccessControlList("owner", new ArrayList<AccessControlEntry>())));
      dataContainer.add(new PersistedNodeData("3", path = QPath.makeChildPath(path, new QName("", "3"), 1, "3"), "4",
         1, 1, null, null, new AccessControlList(null, new ArrayList<AccessControlEntry>())));
      dataContainer.add(new PersistedNodeData("2", path = QPath.makeChildPath(path, new QName("", "2"), 1, "2"), "3",
         1, 1, null, null, new AccessControlList("owner", null)));
      dataContainer.add(new PersistedNodeData("1", path = QPath.makeChildPath(path, new QName("", "1"), 1, "1"), "2",
         1, 1, null, null, null));
      mgr.reloadFilters();
      mgr.getItemData("1");
      assertEquals(3, dataContainer.getTotalCalls());
      dataContainer.clear();
   }

   public void testGetItemParentsWithCrossEmptyACL2() throws Exception
   {
      QPath path;
      dataContainer.add(new PersistedNodeData("4", path = new QPath(new QPathEntry[]{new QPathEntry("", "4", 1, "4")}),
         "5", 1, 1, null, null, new AccessControlList("owner", new ArrayList<AccessControlEntry>())));
      dataContainer.add(new PersistedNodeData("3", path = QPath.makeChildPath(path, new QName("", "3"), 1, "3"), "4",
         1, 1, null, null, new AccessControlList("owner", null)));
      dataContainer.add(new PersistedNodeData("2", path = QPath.makeChildPath(path, new QName("", "2"), 1, "2"), "3",
         1, 1, null, null, new AccessControlList(null, new ArrayList<AccessControlEntry>())));
      dataContainer.add(new PersistedNodeData("1", path = QPath.makeChildPath(path, new QName("", "1"), 1, "1"), "2",
         1, 1, null, null, null));
      mgr.reloadFilters();
      mgr.getItemData("1");
      assertEquals(3, dataContainer.getTotalCalls());
      dataContainer.clear();
   }

   public void testGetItemParentsWithSameOwnerlessACL() throws Exception
   {
      QPath path;
      dataContainer.add(new PersistedNodeData("4", path = new QPath(new QPathEntry[]{new QPathEntry("", "4", 1, "4")}),
         "5", 1, 1, null, null, new AccessControlList("owner", new ArrayList<AccessControlEntry>())));
      dataContainer.add(new PersistedNodeData("3", path = QPath.makeChildPath(path, new QName("", "3"), 1, "3"), "4",
         1, 1, null, null, new AccessControlList("owner", null)));
      dataContainer.add(new PersistedNodeData("2", path = QPath.makeChildPath(path, new QName("", "2"), 1, "2"), "3",
         1, 1, null, null, new AccessControlList(null, new ArrayList<AccessControlEntry>())));
      dataContainer.add(new PersistedNodeData("1", path = QPath.makeChildPath(path, new QName("", "1"), 1, "1"), "2",
         1, 1, null, null, new AccessControlList(null, new ArrayList<AccessControlEntry>())));
      mgr.reloadFilters();
      mgr.getItemData("1");
      assertEquals(2, dataContainer.getTotalCalls());
      dataContainer.clear();
   }

   public void testGetItemParentsWithSamePermissionlessACL() throws Exception
   {
      QPath path;
      dataContainer.add(new PersistedNodeData("4", path = new QPath(new QPathEntry[]{new QPathEntry("", "4", 1, "4")}),
         "5", 1, 1, null, null, new AccessControlList("owner", new ArrayList<AccessControlEntry>())));
      dataContainer.add(new PersistedNodeData("3", path = QPath.makeChildPath(path, new QName("", "3"), 1, "3"), "4",
         1, 1, null, null, new AccessControlList(null, new ArrayList<AccessControlEntry>())));
      dataContainer.add(new PersistedNodeData("2", path = QPath.makeChildPath(path, new QName("", "2"), 1, "2"), "3",
         1, 1, null, null, new AccessControlList("owner", null)));
      dataContainer.add(new PersistedNodeData("1", path = QPath.makeChildPath(path, new QName("", "1"), 1, "1"), "2",
         1, 1, null, null, new AccessControlList("owner", null)));
      mgr.reloadFilters();
      mgr.getItemData("1");
      assertEquals(2, dataContainer.getTotalCalls());
      dataContainer.clear();
   }

   class MyWorkspaceDataContainer extends TestWorkspaceDataContainer
   {

      protected Map<String, NodeData> nodesById = new HashMap<String, NodeData>();

      protected List<ACLHolder> holders = new ArrayList<ACLHolder>();

      private int totalCalls;

      protected synchronized void incrementCalls()
      {
         totalCalls++;
      }

      public WorkspaceStorageConnection openConnection() throws RepositoryException
      {
         return new MyWorkspaceStorageConnection(this);
      }

      public WorkspaceStorageConnection openConnection(boolean readOnly) throws RepositoryException
      {
         return openConnection();
      }

      public void clear()
      {
         nodesById.clear();
         holders.clear();
         totalCalls = 0;
      }

      public void add(NodeData node)
      {
         nodesById.put(node.getIdentifier(), node);
         AccessControlList acl = node.getACL();
         if (acl != null)
         {
            ACLHolder holder = new ACLHolder(node.getIdentifier());
            holder.setOwner(acl.hasOwner());
            holder.setPermissions(acl.hasPermissions());
            holders.add(holder);
         }
      }

      /**
       * @return the totalCalls
       */
      public int getTotalCalls()
      {
         return totalCalls;
      }

   }

   class MyWorkspaceStorageConnection extends TestWorkspaceStorageConnection
   {
      private final MyWorkspaceDataContainer container;

      public MyWorkspaceStorageConnection(MyWorkspaceDataContainer myWorkspaceDataContainer)
      {
         container = myWorkspaceDataContainer;
      }

      /**
       * @see org.exoplatform.services.jcr.impl.dataflow.persistent.CacheableWorkspaceDataManager#getItemData(java.lang.String)
       */
      @Override
      public ItemData getItemData(String identifier) throws RepositoryException
      {
         container.incrementCalls();
         return container.nodesById.get(identifier);
      }

      /**
       * @see org.exoplatform.services.jcr.impl.dataflow.persistent.CacheableWorkspaceDataManager#getACLHolders()
       */
      @Override
      public List<ACLHolder> getACLHolders() throws RepositoryException
      {
         return container.holders;
      }
   }

   class MyWorkspaceStorageCache extends LinkedWorkspaceStorageCacheImpl
   {
      public MyWorkspaceStorageCache() throws RepositoryConfigurationException
      {
         super("", false, 0, 0, 0, 0, false, false, 0, false);
      }

      public boolean isEnabled()
      {
         return false;
      }
   }

}
