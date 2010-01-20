package org.exoplatform.services.jcr.impl.storage.jdbc;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import javax.jcr.InvalidItemStateException;
import javax.jcr.RepositoryException;

import org.exoplatform.container.StandaloneContainer;
import org.exoplatform.services.jcr.BaseStandaloneTest;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedNodeData;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedPropertyData;
import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.storage.WorkspaceDataContainer;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;

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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see&lt;http://www.gnu.org/licenses/&gt;.
 */

/**
 * This benchmark only works with mysql with the dump that you can find in src/test/resources/SQLBenchmark/exodb_data.sql.zip
 * 
 * 
 * Created by The eXo Platform SAS
 * Author : Nicolas Filotto 
 *          nicolas.filotto@exoplatform.com
 * 18 nov. 2009  
 */
public class SQLBenchmarkTest
{
/*   
   static
   {
      try
      {
         Class.forName("com.jdbmonitor.MonitorDriver");
         System.out.println("Driver Loaded");
      }
      catch (ClassNotFoundException e)
      {
         e.printStackTrace();
      }

   }
*/
   /**
    * @param args
    */
   public static void main(String[] args) throws Exception
   {
      String containerConf =
         BaseStandaloneTest.class.getResource("/conf/standalone/sql-benchmark-configuration.xml").toString();
      String loginConf = BaseStandaloneTest.class.getResource("/login.conf").toString();

      StandaloneContainer.addConfigurationURL(containerConf);

      StandaloneContainer container = StandaloneContainer.getInstance();

      if (System.getProperty("java.security.auth.login.config") == null)
         System.setProperty("java.security.auth.login.config", loginConf);

      benchmark(1, container);
      benchmark(10, container);
      benchmark(20, container);
      benchmark(50, container);
      benchmark(100, container);
   }

   private static void benchmark(int threads, StandaloneContainer container) throws Exception
   {
      RepositoryService repositoryService =
         (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);
      RepositoryImpl repository1 = (RepositoryImpl)repositoryService.getRepository("repository1");
      WorkspaceContainerFacade wsc1 = repository1.getWorkspaceContainer("collaboration");
      final WorkspaceDataContainer dataContainer1 =
         (WorkspaceDataContainer)wsc1.getComponent(WorkspaceDataContainer.class);
      WorkspaceContainerFacade wsc1s = repository1.getWorkspaceContainer("system");
      final WorkspaceDataContainer dataContainer1s =
         (WorkspaceDataContainer)wsc1s.getComponent(WorkspaceDataContainer.class);
      RepositoryImpl repository2 = (RepositoryImpl)repositoryService.getRepository("repository2");
      WorkspaceContainerFacade wsc2 = repository2.getWorkspaceContainer("collaboration");
      final WorkspaceDataContainer dataContainer2 =
         (WorkspaceDataContainer)wsc2.getComponent(WorkspaceDataContainer.class);
      WorkspaceContainerFacade wsc2s = repository2.getWorkspaceContainer("system");
      final WorkspaceDataContainer dataContainer2s =
         (WorkspaceDataContainer)wsc2s.getComponent(WorkspaceDataContainer.class);

      System.out.println("########################################");

      int totalTimes;
      long time;
      NodeData parent;

      totalTimes = 5000 / threads;
      QPath path1 = null;
      Task<QPath> traverseQPath = new Task<QPath>()
      {
         public QPath execute(Object... args) throws Exception
         {
            return traverseQPath((WorkspaceDataContainer)args[0], (String)args[1]);
         }

      };
      Result<QPath> rTraverseQPath =
         executeTask(traverseQPath, totalTimes, threads, dataContainer1, Constants.ROOT_UUID);
      path1 = rTraverseQPath.getResult();
      time = rTraverseQPath.getTime();
      System.out.println("traverseQPath with deep 0, thread " + threads
         + ": Total time with the old strategy (n queries) = " + time + ", avg = " + (time / (threads * totalTimes)));
      QPath path2 = null;
      rTraverseQPath = executeTask(traverseQPath, totalTimes, threads, dataContainer2, Constants.ROOT_UUID);
      path2 = rTraverseQPath.getResult();
      time = rTraverseQPath.getTime();
      System.out.println("traverseQPath with deep 0, thread " + threads + ": Total time with the new strategy 0 = "
         + time + ", avg = " + (time / (threads * totalTimes)));
      System.out.println("path1 == path2 = " + equals(path1, path2));

      rTraverseQPath =
         executeTask(traverseQPath, totalTimes, threads, dataContainer1, "dfcbd34bc0a8010b006357806c7f108d");
      path1 = rTraverseQPath.getResult();
      time = rTraverseQPath.getTime();
      System.out.println("traverseQPath with deep 1, thread " + threads
         + ": Total time with the old strategy (n queries) = " + time + ", avg = " + (time / (threads * totalTimes)));

      rTraverseQPath =
         executeTask(traverseQPath, totalTimes, threads, dataContainer2, "dfcbd34bc0a8010b006357806c7f108d");
      path2 = rTraverseQPath.getResult();
      time = rTraverseQPath.getTime();
      System.out.println("traverseQPath with deep 1, thread " + threads
         + ": Total time with the new strategy (n/2 queries) = " + time + ", avg = " + (time / (threads * totalTimes)));
      System.out.println("path1 == path2 = " + equals(path1, path2));

      rTraverseQPath =
         executeTask(traverseQPath, totalTimes, threads, dataContainer1, "dfcbe240c0a8010b00ff024d54e46b9f");
      path1 = rTraverseQPath.getResult();
      time = rTraverseQPath.getTime();
      System.out.println("traverseQPath with deep 2, thread " + threads
         + ": Total time with the old strategy (n queries) = " + time + ", avg = " + (time / (threads * totalTimes)));

      rTraverseQPath =
         executeTask(traverseQPath, totalTimes, threads, dataContainer2, "dfcbe240c0a8010b00ff024d54e46b9f");
      path2 = rTraverseQPath.getResult();
      time = rTraverseQPath.getTime();
      System.out.println("traverseQPath with deep 2, thread " + threads
         + ": Total time with the new strategy (n/2 queries) = " + time + ", avg = " + (time / (threads * totalTimes)));
      System.out.println("path1 == path2 = " + equals(path1, path2));

      rTraverseQPath =
         executeTask(traverseQPath, totalTimes, threads, dataContainer1, "dfcbffaec0a8010b00ed7dad7cb43540");
      path1 = rTraverseQPath.getResult();
      time = rTraverseQPath.getTime();
      System.out.println("traverseQPath with deep 5, thread " + threads
         + ": Total time with the old strategy (n queries) = " + time + ", avg = " + (time / (threads * totalTimes)));

      rTraverseQPath =
         executeTask(traverseQPath, totalTimes, threads, dataContainer2, "dfcbffaec0a8010b00ed7dad7cb43540");
      path2 = rTraverseQPath.getResult();
      time = rTraverseQPath.getTime();
      System.out.println("traverseQPath with deep 5, thread " + threads
         + ": Total time with the new strategy (n/2 queries) = " + time + ", avg = " + (time / (threads * totalTimes)));
      System.out.println("path1 == path2 = " + equals(path1, path2));

      rTraverseQPath =
         executeTask(traverseQPath, totalTimes, threads, dataContainer1, "83cb7ebeac1b00a400bf3596e43c8f18");
      path1 = rTraverseQPath.getResult();
      time = rTraverseQPath.getTime();
      System.out.println("traverseQPath with deep 9, thread " + threads
         + ": Total time with the old strategy (n queries) = " + time + ", avg = " + (time / (threads * totalTimes)));

      rTraverseQPath =
         executeTask(traverseQPath, totalTimes, threads, dataContainer2, "83cb7ebeac1b00a400bf3596e43c8f18");
      path2 = rTraverseQPath.getResult();
      time = rTraverseQPath.getTime();
      System.out.println("traverseQPath with deep 9, thread " + threads
         + ": Total time with the new strategy (n/2 queries) = " + time + ", avg = " + (time / (threads * totalTimes)));
      System.out.println("path1 == path2 = " + equals(path1, path2));

      totalTimes = 1;
      parent =
         new PersistedNodeData("83cb2a36ac1b00a400bdbe4f3f4f6e0e", Constants.ROOT_PATH, null, 0, 0, null, null, null);

      Task<List<NodeData>> getChildNodesData = new Task<List<NodeData>>()
      {
         public List<NodeData> execute(Object... args) throws Exception
         {
            return getChildNodesData((WorkspaceDataContainer)args[0], (NodeData)args[1]);
         }

      };
      List<NodeData> nodesData1 = null;

      Result<List<NodeData>> rGetChildNodesData =
         executeTask(getChildNodesData, totalTimes, threads, dataContainer1, parent);
      nodesData1 = rGetChildNodesData.getResult();
      time = rGetChildNodesData.getTime();
      System.out.println("getChildNodesData with 1034 subnodes, thread " + threads
         + ": Total time with the old strategy (4*n + 1 queries) = " + time + ", avg = "
         + (time / (threads * totalTimes)));
      List<NodeData> nodesData2 = null;
      rGetChildNodesData = executeTask(getChildNodesData, totalTimes, threads, dataContainer2, parent);
      nodesData2 = rGetChildNodesData.getResult();
      time = rGetChildNodesData.getTime();
      System.out.println("getChildNodesData with 1034 subnodes, thread " + threads
         + ": Total time with the new strategy (1 query) = " + time + ", avg = " + (time / (threads * totalTimes)));
      try
      {
         System.out.println("length = " + nodesData1.size());
         System.out.println("nodesData1 == nodesData2 = " + equals(nodesData1, nodesData2) + " length = "
            + nodesData1.size());
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
      totalTimes = 100 / threads;
      parent =
         new PersistedNodeData("83c6e36cac1b00a400688aeb844539b2", Constants.ROOT_PATH, null, 0, 0, null, null, null);
      rGetChildNodesData = executeTask(getChildNodesData, totalTimes, threads, dataContainer1, parent);
      nodesData1 = rGetChildNodesData.getResult();
      time = rGetChildNodesData.getTime();
      System.out.println("getChildNodesData with 4 subnodes, thread " + threads
         + ": Total time with the old strategy (4*n + 1 queries) = " + time + ", avg = "
         + (time / (threads * totalTimes)));

      rGetChildNodesData = executeTask(getChildNodesData, totalTimes, threads, dataContainer2, parent);
      nodesData2 = rGetChildNodesData.getResult();
      time = rGetChildNodesData.getTime();
      System.out.println("getChildNodesData with 4 subnodes, thread " + threads
         + ": Total time with the new strategy (1 query) = " + time + ", avg = " + (time / (threads * totalTimes)));
      System.out.println("nodesData1 == nodesData2 = " + equals(nodesData1, nodesData2) + " length = "
         + nodesData1.size());

      totalTimes = 1000 / threads;

      Task<ItemData> getItemData = new Task<ItemData>()
      {
         public ItemData execute(Object... args) throws Exception
         {
            return getItemData((WorkspaceDataContainer)args[0], (String)args[1]);
         }

      };
      PersistedNodeData nodeData1 = null;
      Result<ItemData> rGetItemData =
         executeTask(getItemData, totalTimes, threads, dataContainer1, "83c6e36cac1b00a400688aeb844539b2");
      nodeData1 = (PersistedNodeData)rGetItemData.getResult();
      time = rGetItemData.getTime();
      System.out.println("getItemData by Id for a node, thread " + threads
         + ": Total time with the old strategy (5 queries) = " + time + ", avg = " + (time / (threads * totalTimes)));

      PersistedNodeData nodeData2 = null;
      rGetItemData = executeTask(getItemData, totalTimes, threads, dataContainer2, "83c6e36cac1b00a400688aeb844539b2");
      nodeData2 = (PersistedNodeData)rGetItemData.getResult();
      time = rGetItemData.getTime();
      System.out.println("getItemData by Id for a node, thread " + threads
         + ": Total time with the new strategy (2 queries) = " + time + ", avg = " + (time / (threads * totalTimes)));
      System.out.println("nodeData1 == nodeData2 = " + equals(nodeData1, nodeData2));

      PersistedPropertyData propertyData1 = null;
      rGetItemData = executeTask(getItemData, totalTimes, threads, dataContainer1, "83c6e36cac1b00a40038e9e950ecff39");
      propertyData1 = (PersistedPropertyData)rGetItemData.getResult();
      time = rGetItemData.getTime();
      System.out.println("getItemData by Id for a property, thread " + threads
         + ": Total time with the old strategy (2 queries) = " + time + ", avg = " + (time / (threads * totalTimes)));

      PersistedPropertyData propertyData2 = null;
      rGetItemData = executeTask(getItemData, totalTimes, threads, dataContainer2, "83c6e36cac1b00a40038e9e950ecff39");
      propertyData2 = (PersistedPropertyData)rGetItemData.getResult();
      time = rGetItemData.getTime();
      System.out.println("getItemData by Id for a property, thread " + threads
         + ": Total time with the new strategy (2 queries) (=old strategy) = " + time + ", avg = "
         + (time / (threads * totalTimes)));
      System.out.println("propertyData1 == propertyData2 = " + equals(propertyData1, propertyData2));

      NodeData parent2 =
         new PersistedNodeData("00exo0jcr0root0uuid0000000000000", Constants.ROOT_PATH, null, 0, 0, null, null, null);
      QPathEntry name2 = new QPathEntry(null, "Documents", 1);

      Task<ItemData> getItemData2 = new Task<ItemData>()
      {
         public ItemData execute(Object... args) throws Exception
         {
            return getItemData((WorkspaceDataContainer)args[0], (NodeData)args[1], (QPathEntry)args[2]);
         }

      };
      rGetItemData = executeTask(getItemData2, totalTimes, threads, dataContainer1, parent2, name2);
      nodeData1 = (PersistedNodeData)rGetItemData.getResult();
      time = rGetItemData.getTime();
      System.out.println("getItemData by QPathEntry for a node, thread " + threads
         + ": Total time with the old strategy = " + time + ", avg = " + (time / (threads * totalTimes)));

      rGetItemData = executeTask(getItemData2, totalTimes, threads, dataContainer2, parent2, name2);
      nodeData2 = (PersistedNodeData)rGetItemData.getResult();
      time = rGetItemData.getTime();
      System.out.println("getItemData by QPathEntry for a node, thread " + threads
         + ": Total time with the new strategy = " + time + ", avg = " + (time / (threads * totalTimes)));
      System.out.println("nodeData1 == nodeData2 = " + equals(nodeData1, nodeData2));

      NodeData parent3 = nodeData1;
      QPathEntry name3 = new QPathEntry("http://www.exoplatform.com/jcr/exo/1.0", "permissions", 1);

      rGetItemData = executeTask(getItemData2, totalTimes, threads, dataContainer1, parent3, name3);
      propertyData1 = (PersistedPropertyData)rGetItemData.getResult();
      time = rGetItemData.getTime();
      System.out.println("getItemData by QPathEntry for a property, thread " + threads
         + ": Total time with the old strategy = " + time + ", avg = " + (time / (threads * totalTimes)));

      rGetItemData = executeTask(getItemData2, totalTimes, threads, dataContainer2, parent3, name3);
      propertyData2 = (PersistedPropertyData)rGetItemData.getResult();
      time = rGetItemData.getTime();
      System.out.println("getItemData by QPathEntry for a property, thread " + threads
         + ": Total time with the new strategy = " + time + ", avg = " + (time / (threads * totalTimes)));
      System.out.println("propertyData1 == propertyData2 = " + equals(propertyData1, propertyData2));

      parent =
         new PersistedNodeData("83c7507eac1b00a400cf6d951b948e23", Constants.ROOT_PATH, null, 0, 0, null, null, null);

      totalTimes = 100 / threads;
      Task<List<PropertyData>> getChildPropertiesData = new Task<List<PropertyData>>()
      {
         public List<PropertyData> execute(Object... args) throws Exception
         {
            return getChildPropertiesData((WorkspaceDataContainer)args[0], (NodeData)args[1]);
         }

      };
      List<PropertyData> propertiesData1 = null;
      Result<List<PropertyData>> rGetChildPropertiesData =
         executeTask(getChildPropertiesData, totalTimes, threads, dataContainer1, parent);
      propertiesData1 = rGetChildPropertiesData.getResult();
      time = rGetChildPropertiesData.getTime();
      System.out.println("getChildPropertiesData with 20 properties, thread " + threads
         + ": Total time with the old strategy (n + 1 queries) = " + time + ", avg = "
         + (time / (threads * totalTimes)));
      List<PropertyData> propertiesData2 = null;
      rGetChildPropertiesData = executeTask(getChildPropertiesData, totalTimes, threads, dataContainer2, parent);
      propertiesData2 = rGetChildPropertiesData.getResult();
      time = rGetChildPropertiesData.getTime();
      System.out.println("getChildPropertiesData with 20 properties, thread " + threads
         + ": Total time with the new strategy (1 query) = " + time + ", avg = " + (time / (threads * totalTimes)));
      try
      {
         System.out.println("propertiesData1 == propertiesData2 = " + equalsP(propertiesData1, propertiesData2)
            + " length = " + propertiesData1.size());
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
      totalTimes = 100 / threads;
      parent =
         new PersistedNodeData("83c6e36cac1b00a400688aeb844539b2", Constants.ROOT_PATH, null, 0, 0, null, null, null);
      rGetChildPropertiesData = executeTask(getChildPropertiesData, totalTimes, threads, dataContainer1, parent);
      propertiesData1 = rGetChildPropertiesData.getResult();
      time = rGetChildPropertiesData.getTime();
      System.out.println("getChildPropertiesData with 6 properties, thread " + threads
         + ": Total time with the old strategy (n + 1 queries) = " + time + ", avg = "
         + (time / (threads * totalTimes)));

      rGetChildPropertiesData = executeTask(getChildPropertiesData, totalTimes, threads, dataContainer2, parent);
      propertiesData2 = rGetChildPropertiesData.getResult();
      time = rGetChildPropertiesData.getTime();
      System.out.println("getChildPropertiesData with 6 properties, thread " + threads
         + ": Total time with the new strategy (1 query) = " + time + ", avg = " + (time / (threads * totalTimes)));
      System.out.println("propertiesData1 == propertiesData2 = " + equalsP(propertiesData1, propertiesData2)
         + " length = " + propertiesData1.size());

      totalTimes = 100 / threads;
      Task<List<PropertyData>> getReferencesData = new Task<List<PropertyData>>()
      {
         public List<PropertyData> execute(Object... args) throws Exception
         {
            return getReferencesData((WorkspaceDataContainer)args[0], (String)args[1]);
         }

      };
      Result<List<PropertyData>> rGetReferencesData =
         executeTask(getReferencesData, totalTimes, threads, dataContainer1s, "dfcbf3cfc0a8010b00a3f5f3b962c76a");
      propertiesData1 = rGetReferencesData.getResult();
      time = rGetReferencesData.getTime();
      System.out.println("getReferencesData with 3 properties, thread " + threads
         + ": Total time with the old strategy (n + 1 queries) = " + time + ", avg = "
         + (time / (threads * totalTimes)));

      rGetReferencesData =
         executeTask(getReferencesData, totalTimes, threads, dataContainer2s, "dfcbf3cfc0a8010b00a3f5f3b962c76a");
      propertiesData2 = rGetReferencesData.getResult();
      time = rGetReferencesData.getTime();
      System.out.println("getReferencesData with 3 properties, thread " + threads
         + ": Total time with the new strategy (1 query) = " + time + ", avg = " + (time / (threads * totalTimes)));
      try
      {
         System.out.println("propertiesData1 == propertiesData2 = " + equalsP(propertiesData1, propertiesData2)
            + " length = " + propertiesData1.size());
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
      totalTimes = 100 / threads;
      rGetReferencesData =
         executeTask(getReferencesData, totalTimes, threads, dataContainer1, "dfcbe9d6c0a8010b004ccac41a161c5d");
      propertiesData1 = rGetReferencesData.getResult();
      time = rGetReferencesData.getTime();
      System.out.println("getReferencesData with 1 property, thread " + threads
         + ": Total time with the old strategy (n + 1 queries) = " + time + ", avg = "
         + (time / (threads * totalTimes)));

      rGetReferencesData =
         executeTask(getReferencesData, totalTimes, threads, dataContainer2, "dfcbe9d6c0a8010b004ccac41a161c5d");
      propertiesData2 = rGetReferencesData.getResult();
      time = rGetReferencesData.getTime();
      System.out.println("getReferencesData with 1 property, thread " + threads
         + ": Total time with the new strategy (1 query) = " + time + ", avg = " + (time / (threads * totalTimes)));
      System.out.println("propertiesData1 == propertiesData2 = " + equalsP(propertiesData1, propertiesData2)
         + " length = " + propertiesData1.size());
   }

   private static boolean equals(PersistedNodeData nodeData1, PersistedNodeData nodeData2)
   {
      return nodeData1.getACL().equals(nodeData2.getACL())
         && nodeData1.getIdentifier().equals(nodeData2.getIdentifier())
         && Arrays.equals(nodeData1.getMixinTypeNames(), nodeData2.getMixinTypeNames())
         && nodeData1.getOrderNumber() == nodeData2.getOrderNumber()
         && nodeData1.getParentIdentifier().equals(nodeData2.getParentIdentifier())
         && nodeData1.getPersistedVersion() == nodeData2.getPersistedVersion()
         && nodeData1.getPrimaryTypeName().equals(nodeData2.getPrimaryTypeName())
         && nodeData1.getQPath().equals(nodeData2.getQPath()) && nodeData1.isNode() == nodeData2.isNode();
   }

   private static boolean equals(PersistedPropertyData propertyData1, PersistedPropertyData propertyData2)
   {
      boolean result =
         propertyData1.isMultiValued() == propertyData2.isMultiValued()
            && propertyData1.isNode() == propertyData2.isNode()
            && propertyData1.getIdentifier().equals(propertyData2.getIdentifier())
            && propertyData1.getParentIdentifier().equals(propertyData2.getParentIdentifier())
            && propertyData1.getPersistedVersion() == propertyData2.getPersistedVersion()
            && propertyData1.getQPath().equals(propertyData2.getQPath())
            && propertyData1.getType() == propertyData2.getType();
      if (!result)
      {
         return false;
      }
      List<ValueData> values1 = propertyData1.getValues();
      List<ValueData> values2 = propertyData2.getValues();
      if (values1 == null)
      {
         return values2 == null;
      }
      else if (values2 == null || values1.size() != values2.size())
      {
         return false;
      }
      else
      {
         for (int i = 0; i < values1.size(); i++)
         {
            ValueData value1 = values1.get(i);
            ValueData value2 = values2.get(i);
            result =
               value1.isByteArray() == value2.isByteArray() && value1.getLength() == value2.getLength()
                  && value1.getOrderNumber() == value2.getOrderNumber();
            if (!result)
            {
               return false;
            }
         }
      }
      return true;
   }

   private static boolean equals(QPath path1, QPath path2)
   {
      return path1.equals(path2);
   }

   private static boolean equalsP(List<PropertyData> propertiesData1, List<PropertyData> propertiesData2)
   {
      if (propertiesData1 == null)
      {
         return propertiesData2 == null;
      }
      else if (propertiesData2 == null || propertiesData1.size() != propertiesData2.size())
      {
         return false;
      }
      else
      {
         for (int i = 0; i < propertiesData1.size(); i++)
         {
            PersistedPropertyData propertyData1 = (PersistedPropertyData)propertiesData1.get(i);
            PersistedPropertyData propertyData2 = (PersistedPropertyData)propertiesData2.get(i);
            if (!equals(propertyData1, propertyData2))
            {
               return false;
            }
         }
      }
      return true;
   }

   private static boolean equals(List<NodeData> nodesData1, List<NodeData> nodesData2)
   {
      if (nodesData1 == null)
      {
         return nodesData2 == null;
      }
      else if (nodesData2 == null || nodesData1.size() != nodesData2.size())
      {
         return false;
      }
      else
      {
         for (int i = 0; i < nodesData1.size(); i++)
         {
            PersistedNodeData nodeData1 = (PersistedNodeData)nodesData1.get(i);
            PersistedNodeData nodeData2 = (PersistedNodeData)nodesData2.get(i);
            if (!equals(nodeData1, nodeData2))
            {
               return false;
            }
         }
      }
      return true;
   }

   public static ItemData getItemData(WorkspaceDataContainer dataContainer, NodeData parentData, QPathEntry name)
      throws RepositoryException, IllegalStateException
   {
      final WorkspaceStorageConnection con = dataContainer.openConnection();
      try
      {
         return con.getItemData(parentData, name);
      }
      finally
      {
         con.close();
      }
   }

   private static ItemData getItemData(WorkspaceDataContainer dataContainer, final String identifier)
      throws RepositoryException
   {
      final WorkspaceStorageConnection con = dataContainer.openConnection();
      try
      {
         return con.getItemData(identifier);
      }
      finally
      {
         con.close();
      }
   }

   private static List<NodeData> getChildNodesData(WorkspaceDataContainer dataContainer, final NodeData parent)
      throws RepositoryException
   {
      final WorkspaceStorageConnection con = dataContainer.openConnection();
      try
      {
         return con.getChildNodesData(parent);
      }
      finally
      {
         con.close();
      }
   }

   private static List<PropertyData> getChildPropertiesData(WorkspaceDataContainer dataContainer, final NodeData parent)
      throws RepositoryException
   {
      final WorkspaceStorageConnection con = dataContainer.openConnection();
      try
      {
         return con.getChildPropertiesData(parent);
      }
      finally
      {
         con.close();
      }
   }

   private static List<PropertyData> getReferencesData(WorkspaceDataContainer dataContainer, final String identifier)
      throws RepositoryException
   {
      final WorkspaceStorageConnection con = dataContainer.openConnection();
      try
      {
         return con.getReferencesData(identifier);
      }
      finally
      {
         con.close();
      }
   }

   private static QPath traverseQPath(WorkspaceDataContainer dataContainer, final String identifier)
      throws RepositoryException, SQLException, InvalidItemStateException, IllegalNameException
   {
      final JDBCStorageConnection con = (JDBCStorageConnection)dataContainer.openConnection();
      try
      {
         return con.traverseQPath(con.getInternalId(identifier));
      }
      finally
      {
         con.close();
      }
   }

   private static interface Task<R>
   {
      R execute(Object... args) throws Exception;
   }

   private static class Result<R>
   {
      private final R result;

      private final long time;

      public Result(R result, long time)
      {
         this.result = result;
         this.time = time;
      }

      public R getResult()
      {
         return result;
      }

      public long getTime()
      {
         return time;
      }
   }

   private static <R> Result<R> executeTask(final Task<R> task, final int totalTimes, int threads, final Object... args)
      throws InterruptedException
   {
      final CountDownLatch startSignal = new CountDownLatch(1);
      final CountDownLatch doneSignal = new CountDownLatch(threads);
      final AtomicReference<R> result = new AtomicReference<R>();
      for (int i = 0; i < threads; i++)
      {
         Thread t = new Thread()
         {
            public void run()
            {
               try
               {
                  startSignal.await();
                  for (int i = 0; i < totalTimes; i++)
                  {
                     result.set(task.execute(args));
                  }
               }
               catch (Exception e)
               {
                  e.printStackTrace();
               }
               finally
               {
                  doneSignal.countDown();
               }
            }
         };
         t.start();
      }
      long time = System.currentTimeMillis();
      startSignal.countDown();
      doneSignal.await();
      return new Result<R>(result.get(), System.currentTimeMillis() - time);
   }
}
