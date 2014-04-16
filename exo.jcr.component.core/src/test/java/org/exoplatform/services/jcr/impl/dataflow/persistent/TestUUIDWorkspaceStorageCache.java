/*
 * Copyright (C) 2014 eXo Platform SAS.
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

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.dataflow.persistent.WorkspaceStorageCache;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.itemfilters.PatternQPathEntry;
import org.exoplatform.services.jcr.impl.core.itemfilters.PatternQPathEntryFilter;
import org.exoplatform.services.jcr.impl.core.itemfilters.QPathEntryFilter;
import org.exoplatform.services.jcr.impl.storage.SystemDataContainerHolder;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.jcr.storage.WorkspaceDataContainer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.jcr.RepositoryException;


/**
 * Created by The eXo Platform SAS
 * Author : Aymen Boughzela
 *          aymen.boughzela@exoplatform.com
 * 15 Avril 2014
 */

public class TestUUIDWorkspaceStorageCache extends JcrAPIBaseTest
{
   private CacheableWorkspaceDataManager cwdm;

   private WorkspaceDataContainer wdc;

   private NodeImpl testNode;

   private NodeImpl refNode;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      WorkspaceContainerFacade wscf = repository.getWorkspaceContainer(repository.getSystemWorkspaceName());
      WorkspaceEntry wconf = (WorkspaceEntry)wscf.getComponent(WorkspaceEntry.class);
      wdc = (JDBCWorkspaceDataContainer)session.getContainer()
         .getComponentInstanceOfType(JDBCWorkspaceDataContainer.class);
      WorkspaceStorageCache wsc = (WorkspaceStorageCache)session.getContainer()
         .getComponentInstanceOfType(WorkspaceStorageCache.class);
      this.cwdm =
         new CacheableWorkspaceDataManager(wconf, wdc, wsc,
            new SystemDataContainerHolder(wdc));
      testNode = (NodeImpl)root.addNode("testNode");
      refNode = (NodeImpl)root.addNode("refNode");
      String mixReferenceable = session.getNamespacePrefix("http://www.jcp.org/jcr/mix/1.0") + ":referenceable";
      testNode.addMixin(mixReferenceable);
      testNode.addNode("node1");
      testNode.addNode("node2");
      testNode.addNode("node3");
      refNode.setProperty("ref", testNode);
      session.save();
   }

   @Override
   public void tearDown() throws Exception
   {
      testNode.remove();
      refNode.remove();
      session.save();
      super.tearDown();
      wdc = null;
      cwdm = null;
   }


   public void testUUIDGetChildNodesData() throws RepositoryException
   {
      NodeData rootData = (NodeData)cwdm.getItemData(testNode.getIdentifier());
      List<NodeData> list = cwdm.getChildNodesData(rootData);
      checkNodeUUID(list);
   }

   public void testUUIDGetChildNodesDataByPage() throws RepositoryException
   {
      NodeData rootData = (NodeData)cwdm.getItemData(testNode.getIdentifier());
      List<NodeData> childs = new ArrayList<NodeData>();
      cwdm.getChildNodesDataByPage(rootData, 0, 1, 10, childs);
      checkNodeUUID(childs);

   }

   public void testUUIDGetChildNodesDataPatternFilters() throws RepositoryException
   {
      NodeData rootData = (NodeData)cwdm.getItemData(testNode.getIdentifier());
      final List<QPathEntryFilter> nodePatterns =
         Collections.singletonList((QPathEntryFilter)new PatternQPathEntryFilter(new PatternQPathEntry("",
            "node")));
      List<NodeData> list = cwdm.getChildNodesData(rootData, nodePatterns);
      checkNodeUUID(list);

   }


   public void testUUIDGetChildPropertiesData() throws RepositoryException
   {
      NodeData rootData = (NodeData)cwdm.getItemData(testNode.getIdentifier());
      List<PropertyData> list = cwdm.getChildPropertiesData(rootData);
      checkPropertyUUID(list);
   }

   public void testUUIDGetChildPropertiesDataPatternFilters() throws RepositoryException
   {
      NodeData rootData = (NodeData)cwdm.getItemData(testNode.getIdentifier());
      final List<QPathEntryFilter> nodePatterns =
         Collections.singletonList((QPathEntryFilter)new PatternQPathEntryFilter(new PatternQPathEntry("",
            "node")));
      List<PropertyData> list = cwdm.getChildPropertiesData(rootData, nodePatterns);
      checkPropertyUUID(list);

   }

   public void testUUIDGetItemData() throws Exception
   {
      ItemData item = cwdm.getItemData(testNode.getIdentifier());
      assertFalse(item.getIdentifier().startsWith(wdc.getName()));


   }

   public void testUUIDGetReferencesData() throws RepositoryException
   {
      List<PropertyData> list = cwdm.getReferencesData(testNode.getIdentifier(), true);
      checkPropertyUUID(list);

   }

   public void testUUIDListChildPropertiesData() throws RepositoryException
   {
      NodeData rootData = (NodeData)cwdm.getItemData(testNode.getIdentifier());
      List<PropertyData> list = cwdm.listChildPropertiesData(rootData);
      checkPropertyUUID(list);

   }


   public void testUUIDGetItemData1() throws Exception
   {
      NodeData rootData = (NodeData)cwdm.getItemData(Constants.ROOT_UUID);
      InternalQName nodeName = InternalQName.parse("[]testNode");
      ItemData item = cwdm.getItemData(rootData, new QPathEntry(nodeName, 1), ItemType.NODE);
      assertFalse(item.getIdentifier().startsWith(wdc.getName()));


   }

   public void testUUIDGetACLHolders() throws RepositoryException
   {
      List<ACLHolder> holders = cwdm.getACLHolders();
      for (int i = 0, length = holders.size(); i < length; i++)
      {
         assertFalse(holders.get(i).getId().startsWith(wdc.getName()));
      }
   }

   private void checkNodeUUID(List<NodeData> list)
   {
      for (NodeData i : list)
      {
         assertFalse(i.getIdentifier().startsWith(wdc.getName()));
      }
   }

   private void checkPropertyUUID(List<PropertyData> list)
   {
      for (PropertyData i : list)
      {
         assertFalse(i.getIdentifier().startsWith(wdc.getName()));
      }
   }

}
