/*
 * Copyright (C) 2012 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.quota;

import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.dataflow.serialization.TesterItemsPersistenceListener;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.Value;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: TestContentSizeHandler.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class TestContentSizeHandler extends AbstractQuotaManagerTest
{
   private Node testRoot;

   private long testRootSize;

   private LocationFactory lFactory;

   public void setUp() throws Exception
   {
      super.setUp();

      testRoot = session.getRootNode().addNode("testRoot");
      session.save();

      testRootSize = wsQuotaManager.getNodeDataSizeDirectly(testRoot.getPath());
      lFactory = repository.getLocationFactory();
   }

   /**
    * Adds node and checks if content size was increased on correct value.
    * Single-value properties only.
    */
   public void testAddSingleValueProps() throws Exception
   {
      TesterItemsPersistenceListener pListener = new TesterItemsPersistenceListener(session);

      Node node1 = testRoot.addNode("node1");
      node1.setProperty("prop1", "value");
      node1.setProperty("prop2", true);
      node1.setProperty("prop4", Calendar.getInstance());
      node1.setProperty("prop5", new FileInputStream(createBLOBTempFile(1000)));

      testRoot.addNode("node2").addNode("node3");
      testRoot.save();

      List<TransactionChangesLog> logs = pListener.pushChanges();

      long expectedDelta = wsQuotaManager.getNodeDataSizeDirectly(testRoot.getPath()) - testRootSize;
      long measuredDelta = calcChangedSize(logs, testRoot.getPath());

      assertEquals(expectedDelta, measuredDelta);
   }

   /**
    * Adds node and checks if content size was increased on correct value.
    * Multi-value properties included.
    */
   public void testAddMultiValueProps() throws Exception
   {
      TesterItemsPersistenceListener pListener = new TesterItemsPersistenceListener(session);

      Value[] values =
         {valueFactory.createValue(new ByteArrayInputStream("binary string 1".getBytes())),
            valueFactory.createValue(new FileInputStream(createBLOBTempFile(1000)))};

      testRoot.setProperty("prop1", new String[]{"value1", "value2", "value3"});
      testRoot.setProperty("prop2", values, PropertyType.BINARY);

      testRoot.save();

      List<TransactionChangesLog> logs = pListener.pushChanges();

      long expectedDelta = wsQuotaManager.getNodeDataSizeDirectly(testRoot.getPath()) - testRootSize;
      long measuredDelta = calcChangedSize(logs, testRoot.getPath());

      assertEquals(expectedDelta, measuredDelta);
   }

   /**
    * Updating same property several times.
    */
   public void testUpdateTransientSingleValuePropSeveralTimes() throws Exception
   {
      TesterItemsPersistenceListener pListener = new TesterItemsPersistenceListener(session);

      testRoot.setProperty("prop1", "12345");
      testRoot.setProperty("prop1", "12345678910");
      testRoot.save();

      List<TransactionChangesLog> logs = pListener.pushChanges();

      long expectedDelta = wsQuotaManager.getNodeDataSizeDirectly(testRoot.getPath()) - testRootSize;
      long measuredDelta = calcChangedSize(logs, testRoot.getPath());

      assertEquals(expectedDelta, measuredDelta);
   }

   /**
    * Updating same property several times.
    */
   public void testUpdatePersistedSingleValuePropSeveralTimes() throws Exception
   {
      testRoot.setProperty("prop1", "1");
      testRoot.save();
      testRootSize = wsQuotaManager.getNodeDataSizeDirectly(testRoot.getPath());

      TesterItemsPersistenceListener pListener = new TesterItemsPersistenceListener(session);

      testRoot.setProperty("prop1", "12345");
      testRoot.setProperty("prop1", "12345678910");
      testRoot.save();

      List<TransactionChangesLog> logs = pListener.pushChanges();

      long expectedDelta = wsQuotaManager.getNodeDataSizeDirectly(testRoot.getPath()) - testRootSize;
      long measuredDelta = calcChangedSize(logs, testRoot.getPath());

      assertEquals(expectedDelta, measuredDelta);
   }

   /**
    * Updating same property several times.
    */
   public void testUpdatePersistedMultiValueProp() throws Exception
   {
      Value[] values =
         {valueFactory.createValue(new ByteArrayInputStream("binary string 1".getBytes())),
            valueFactory.createValue(new FileInputStream(createBLOBTempFile(1000)))};

      testRoot.setProperty("prop1", new String[]{"value1", "value2", "value3"});
      testRoot.setProperty("prop2", values, PropertyType.BINARY);

      testRoot.save();
      testRootSize = wsQuotaManager.getNodeDataSizeDirectly(testRoot.getPath());

      TesterItemsPersistenceListener pListener = new TesterItemsPersistenceListener(session);

      Value[] newValues = {valueFactory.createValue(new FileInputStream(createBLOBTempFile(1000)))};

      testRoot.setProperty("prop1", new String[]{"value11", "value22", "value33"});
      testRoot.setProperty("prop2", newValues);
      testRoot.save();

      List<TransactionChangesLog> logs = pListener.pushChanges();

      long expectedDelta = wsQuotaManager.getNodeDataSizeDirectly(testRoot.getPath()) - testRootSize;
      long measuredDelta = calcChangedSize(logs, testRoot.getPath());

      assertEquals(expectedDelta, measuredDelta);
   }

   /**
    * Delete node.
    */
   public void testDeleteNode() throws Exception
   {
      testRoot.addNode("node").setProperty("prop", new FileInputStream(createBLOBTempFile(1000)));
      testRoot.save();
      testRootSize = wsQuotaManager.getNodeDataSizeDirectly(testRoot.getPath());

      TesterItemsPersistenceListener pListener = new TesterItemsPersistenceListener(session);

      testRoot.getNode("node").remove();
      testRoot.save();

      List<TransactionChangesLog> logs = pListener.pushChanges();

      long expectedDelta = wsQuotaManager.getNodeDataSizeDirectly(testRoot.getPath()) - testRootSize;
      long measuredDelta = calcChangedSize(logs, testRoot.getPath());

      assertEquals(expectedDelta, measuredDelta);
   }

   /**
    * Add and delete node.
    */
   public void testAddDeleteNode() throws Exception
   {
      TesterItemsPersistenceListener pListener = new TesterItemsPersistenceListener(session);

      testRoot.addNode("node").remove();
      testRoot.save();

      List<TransactionChangesLog> logs = pListener.pushChanges();

      long expectedDelta = wsQuotaManager.getNodeDataSizeDirectly(testRoot.getPath()) - testRootSize;
      long measuredDelta = calcChangedSize(logs, testRoot.getPath());

      assertEquals(expectedDelta, measuredDelta);
   }

   /**
    * Add and delete node.
    */
   public void testAddDeleteAddNode() throws Exception
   {
      testRoot.addNode("node");
      testRoot.save();
      testRootSize = wsQuotaManager.getNodeDataSizeDirectly(testRoot.getPath());

      TesterItemsPersistenceListener pListener = new TesterItemsPersistenceListener(session);

      testRoot.getNode("node").remove();
      testRoot.addNode("node");
      testRoot.save();

      List<TransactionChangesLog> logs = pListener.pushChanges();

      long expectedDelta = wsQuotaManager.getNodeDataSizeDirectly(testRoot.getPath()) - testRootSize;
      long measuredDelta = calcChangedSize(logs, testRoot.getPath());

      assertEquals(expectedDelta, measuredDelta);
   }

   /**
    * Add and delete node.
    */
   public void testRenameNode() throws Exception
   {
      Node testRename = root.addNode("rename");
      session.save();
      long testRenameSize = wsQuotaManager.getNodeDataSizeDirectly(testRename.getPath());

      testRoot.addNode("node");
      testRoot.save();
      testRootSize = wsQuotaManager.getNodeDataSizeDirectly(testRoot.getPath());

      TesterItemsPersistenceListener pListener = new TesterItemsPersistenceListener(session);

      session.move("/testRoot/node", "/rename/node");
      session.save();

      List<TransactionChangesLog> logs = pListener.pushChanges();

      long expectedDelta = wsQuotaManager.getNodeDataSizeDirectly(testRoot.getPath()) - testRootSize;
      long measuredDelta = calcChangedSize(logs, testRoot.getPath());

      assertEquals(expectedDelta, measuredDelta);

      expectedDelta = wsQuotaManager.getNodeDataSizeDirectly(testRename.getPath()) - testRenameSize;
      measuredDelta = calcChangedSize(logs, testRename.getPath());

      assertEquals(expectedDelta, measuredDelta);
   }

   /**
    * Add and delete node.
    */
   public void testAddRenameRenameNode() throws Exception
   {
      Node testRename1 = root.addNode("rename1");
      Node testRename2 = root.addNode("rename2");
      session.save();
      long testRename1Size = wsQuotaManager.getNodeDataSizeDirectly(testRename1.getPath());
      long testRename2Size = wsQuotaManager.getNodeDataSizeDirectly(testRename2.getPath());

      TesterItemsPersistenceListener pListener = new TesterItemsPersistenceListener(session);

      testRoot.addNode("node");
      root.getNode("testRoot/node").setProperty("prop1", "012345");
      root.getNode("testRoot/node").setProperty("prop1", "0123456789");
      session.move("/testRoot/node", "/rename1/node");

      root.getNode("rename1/node").setProperty("prop2", "012345");
      root.getNode("rename1/node").setProperty("prop2", "0123456789");
      session.move("/rename1/node", "/rename2/node");
      session.save();

      List<TransactionChangesLog> logs = pListener.pushChanges();

      long expectedDelta = wsQuotaManager.getNodeDataSizeDirectly(testRoot.getPath()) - testRootSize;
      long measuredDelta = calcChangedSize(logs, testRoot.getPath());

      assertEquals(expectedDelta, measuredDelta);

      expectedDelta = wsQuotaManager.getNodeDataSizeDirectly(testRename1.getPath()) - testRename1Size;
      measuredDelta = calcChangedSize(logs, testRename1.getPath());

      assertEquals(expectedDelta, measuredDelta);

      expectedDelta = wsQuotaManager.getNodeDataSizeDirectly(testRename2.getPath()) - testRename2Size;
      measuredDelta = calcChangedSize(logs, testRename2.getPath());

      assertEquals(expectedDelta, measuredDelta);
   }

   private long calcChangedSize(List<TransactionChangesLog> logs, String nodePath) throws Exception
   {
      long delta = 0;

      for (TransactionChangesLog tLog : logs)
      {
         for (ItemState state : tLog.getAllStates())
         {
            if (!state.getData().isNode())
            {
               String path = lFactory.createJCRPath(state.getData().getQPath()).getAsString(false);
               if (path.startsWith(nodePath))
               {
                  delta += state.getChangedSize();
               }
            }
         }
      }

      return delta;
   }
}
