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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.impl.storage;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.core.PropertyImpl;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.SimpleChangedSizeHandler;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig.DatabaseStructureType;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCStorageConnection;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.jcr.storage.WorkspaceDataContainer;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;

import java.sql.Statement;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.version.Version;

/**
 * Created by The eXo Platform SAS Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 26.09.2006 VARNING! This test change data container database data directly in tables
 * JCR_XCONTAINER: version switched to 1.0 value. And then try update container to actual data in
 * StorageUpdateManager.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: StorageUpdateTest.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class StorageUpdateTest extends JcrImplBaseTest
{

   protected static final String REFERENCEABLE_NAME = "node_R__nt_file";

   protected static final String VERSIONABLE_NAME = "node_V__nt_file";

   private JDBCWorkspaceDataContainer dataContainer = null;

   private Node testNode = null;

   private Version version1_node_V = null;

   private Version version2_node_V = null;

   private Version version3_node_V = null;

   private Version version1_node_R = null;

   private Version version2_node_R = null;

   private DatabaseStructureType dbStructureType;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      ExoContainer exoContainer = session.getContainer();
      if (exoContainer == null)
         fail("This test can be executed by user with ADMIN rights only");

      dataContainer = (JDBCWorkspaceDataContainer)exoContainer.getComponentInstanceOfType(WorkspaceDataContainer.class);
      if (dataContainer == null)
         fail("This test can be executed on JDBCWorkspaceDataContainer instance only");

      WorkspaceEntry wsEntry = (WorkspaceEntry)session.getContainer()

      .getComponentInstanceOfType(WorkspaceEntry.class);

      dbStructureType = DatabaseStructureType.valueOf(wsEntry.getContainer().getParameterValue(
				JDBCWorkspaceDataContainer.DB_STRUCTURE_TYPE));

      testNode = session.getRootNode().addNode("test_node", "nt:unstructured");
      // create referenceable node
      Node node_R = testNode.addNode(REFERENCEABLE_NAME, "nt:file");
      Node content = node_R.addNode("jcr:content", "nt:unstructured");
      content.setProperty("anyDate", Calendar.getInstance());
      content.setProperty("anyString", "11111111111111<=Any string=>11111111111111111");
      content.setProperty("anyNumb", 123.321d);

      content.setProperty("anyBinary", "11111111111111<=Any binary=>11111111111111111", PropertyType.BINARY);

      content.addNode("anyNode1").setProperty("_some_double", 1234.4321d);
      content.addNode("anyNode2").setProperty("_some_long", 123456789L);

      session.save();

      if (node_R.canAddMixin("mix:versionable"))
      {
         node_R.addMixin("mix:versionable");
         node_R.save();
      }
      else
      {
         fail("Can't add mixin mix:versionable");
      }

      session.save();

      // show ref node jcr:uuid
      log.info("node_R node uuid: " + node_R.getUUID() + ", jcr:uuid: " + node_R.getProperty("jcr:uuid").getString());

      Node node11 = testNode.addNode("node1").addNode("node11");

      session.save();

      session.getWorkspace().copy(node_R.getPath(), node11.getPath() + "/" + VERSIONABLE_NAME);

      // show ver node jcr:uuid
      Node node_V = node11.getNode(VERSIONABLE_NAME);
      log.info("node_V node uuid: " + node_V.getUUID() + ", jcr:uuid: " + node_V.getProperty("jcr:uuid").getString());

      // =============================================================
      // !!!!!!!!!!! add bug from Workspace.copy() ver.1.0 !!!!!!!!!!!
      // =============================================================
      PropertyData jcrUuid = (PropertyData)((PropertyImpl)node_V.getProperty("jcr:uuid")).getData();

      // Set a uuid of source node in Workspace.copy()
      TransientPropertyData bugData =
         new TransientPropertyData(jcrUuid.getQPath(), jcrUuid.getIdentifier(), jcrUuid.getPersistedVersion(), jcrUuid
            .getType(), jcrUuid.getParentIdentifier(), jcrUuid.isMultiValued(), new TransientValueData(node_R
            .getProperty("jcr:uuid").getString()));

      WorkspaceStorageConnection conn = dataContainer.openConnection();
      if (conn instanceof JDBCStorageConnection)
      {
         JDBCStorageConnection jdbcConn = (JDBCStorageConnection)conn;

         conn.update(bugData, new SimpleChangedSizeHandler());
         jdbcConn.getJdbcConnection().commit();

         NodeData parent =
            (NodeData)session.getTransientNodesManager().getTransactManager()
               .getItemData(jcrUuid.getParentIdentifier());
         QPathEntry[] qentry = bugData.getQPath().getEntries();
         PropertyData persistedBugData =
            (PropertyData)conn.getItemData(parent, qentry[qentry.length - 1], ItemType.PROPERTY);
         log.info("node_V node BUG uuid: " + node_V.getUUID() + ", jcr:uuid: "
            + new String(persistedBugData.getValues().get(0).getAsByteArray()));

         // =================== remove version record ===================
         Statement smnt = jdbcConn.getJdbcConnection().createStatement();
         log.info("Update container version records: "
            + smnt.executeUpdate("update JCR_" + (dbStructureType.isMultiDatabase() ? "M" : "S") + "CONTAINER set VERSION='1.0'"));
         jdbcConn.getJdbcConnection().commit();

         conn.commit();
      }
      else
      {
         conn.rollback();

         fail("This test require WorkspaceStorageConnection instance of JDBCStorageConnection class only");
      }
      // =============================================================

      // perform some JCR life operations... versions like,
      // so bad uuid will be stored in jcr:frozenUuid props (fix will be
      // implemented in 1.1)

      // node_V life...
      version1_node_V = node_V.checkin();
      node_V.checkout();

      node_V.getNode("jcr:content").addNode("node111");
      node_V.save();

      version2_node_V = node_V.checkin();
      node_V.checkout();

      Node n111 = node_V.getNode("jcr:content").getNode("node111");
      if (n111.canAddMixin("mix:versionable"))
      {
         n111.addMixin("mix:versionable");
         n111.save();

         n111.checkin();
         n111.checkout();

         n111.addNode("node111-1").setProperty("doublee111-1", 222D);
         n111.save();

         n111.checkin();
         n111.checkout();
      }
      else
      {
         fail("Can't add mixin mix:versionable");
      }

      node_V.getNode("jcr:content").getNode("node111").setProperty("prop111", "111111111111");
      node_V.save();

      version3_node_V = node_V.checkin();
      node_V.checkout();

      // node_R life...
      version1_node_R = node_R.checkin();
      node_R.checkout();

      node_R.getNode("jcr:content").addNode("node222").setProperty("prop222", Calendar.getInstance());
      node_R.save();

      version2_node_R = node_R.checkin();
      node_R.checkout();
   }

   public void testUpdate10() throws Exception
   {
      // TODO dataContainer.checkVersion(true);
      // As for now... check fixes manualy (in SQL tool) etc.
   }

}
