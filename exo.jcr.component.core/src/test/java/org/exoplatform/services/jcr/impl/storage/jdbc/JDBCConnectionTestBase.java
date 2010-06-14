/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.storage.jdbc;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;

import java.io.ByteArrayInputStream;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.sql.DataSource;

/**
 * Created by The eXo Platform SAS
 * 
 * 01.07.2009
 * 
 * @author <a href="mailto:dezder@bk.ru">Denis Grebenyuk</a>
 * @version $Id:$ 
 */
abstract public class JDBCConnectionTestBase extends JcrAPIBaseTest
{

   protected JDBCStorageConnection jdbcConn = null;

   protected String tableType = null;

   private Connection connect = null;

   @Override
   protected void tearDown() throws Exception
   {

      connect.close();
      super.tearDown();
   }

   public Connection getJNDIConnection() throws Exception
   {

      final DataSource ds = (DataSource)new InitialContext().lookup("jdbcjcrtest");

      PrivilegedExceptionAction<Connection> action = new PrivilegedExceptionAction<Connection>()
      {
         public Connection run() throws Exception
         {
            return ds.getConnection();
         }
      };
      try
      {
         connect = AccessController.doPrivileged(action);
      }
      catch (PrivilegedActionException pae)
      {
         Throwable cause = pae.getCause();
         if (cause instanceof IllegalArgumentException)
         {
            throw (IllegalArgumentException)cause;
         }
         else if (cause instanceof SQLException)
         {
            throw (SQLException)cause;
         }
         else if (cause instanceof RuntimeException)
         {
            throw (RuntimeException)cause;
         }
         else
         {
            throw new RuntimeException(cause);
         }
      }

      return connect;
   }

   private NodeData setNode() throws Exception
   {
      InternalQName[] iqn = {InternalQName.parse("[]DbJDBCConnectionTest")};
      TransientNodeData tnd =
         new TransientNodeData(Constants.JCR_NODETYPES_PATH, "Z", 9, Constants.SV_NODE_NAME, iqn, 8, null, null);
      return tnd;
   }

   private NodeData giveNodeForRename() throws Exception
   {

      InternalQName[] iqn = {InternalQName.parse("[]DbJDBCConnectionTest")};
      TransientNodeData tnd =
         new TransientNodeData(Constants.JCR_NODETYPES_PATH, "B", 9, Constants.SV_NODE_NAME, iqn, 8, null, null);
      return tnd;
   }

   public void testAddNode() throws Exception
   {

      jdbcConn.add(setNode());
      ResultSet rs =
         connect.createStatement()
            .executeQuery("select * from " + "JCR_" + tableType + "ITEM" + " where N_ORDER_NUM=8");
      rs.next();
      assertEquals("[http://www.jcp.org/jcr/1.0]nodetypes", rs.getString("NAME"));
   }

   public void testAddValueData() throws Exception
   {

      byte data[] = {5};
      ByteArrayInputStream bas = new ByteArrayInputStream(data);
      jdbcConn.addValueData("C", 2, bas, 2, "J");
      ResultSet rs =
         connect.createStatement().executeQuery(
            "select * from " + "JCR_" + tableType + "VALUE" + " where PROPERTY_ID='C'");
      rs.next();
      assertEquals("05", rs.getString("DATA"));
   }

   public void testRenameNode() throws Exception
   {

      jdbcConn.renameNode(giveNodeForRename());
      ResultSet rs =
         connect.createStatement()
            .executeQuery("select * from " + "JCR_" + tableType + "ITEM" + " where N_ORDER_NUM=8");
      rs.next();
      assertEquals("[http://www.jcp.org/jcr/1.0]nodetypes", rs.getString("NAME"));
   }

   public void testUpdateNodeByIdentifier() throws Exception
   {

      jdbcConn.updateNodeByIdentifier(200923, 4512, 20, "B");
      ResultSet rs =
         connect.createStatement().executeQuery("select * from " + "JCR_" + tableType + "ITEM" + " where ID='B'");
      rs.next();
      assertEquals(20, rs.getInt("N_ORDER_NUM"));
   }

   public void testUpdatePropertyByIdentifier() throws Exception
   {

      jdbcConn.updatePropertyByIdentifier(200923, 4512, "C");
      ResultSet rs =
         connect.createStatement().executeQuery("select * from " + "JCR_" + tableType + "ITEM" + " where ID='C'");
      rs.next();
      assertEquals(4512, rs.getInt("P_TYPE"));
   }

   public void testDeleteReference() throws Exception
   {

      jdbcConn.deleteReference("A");
      ResultSet rs =
         connect.createStatement().executeQuery(
            "select * from " + "JCR_" + tableType + "REF" + " where PROPERTY_ID='A'");
      assertEquals(false, rs.next());
   }

   public void testDeleteItemByIdentifier() throws Exception
   {

      jdbcConn.deleteItemByIdentifier("C");
      ResultSet rs =
         connect.createStatement().executeQuery("select * from " + "JCR_" + tableType + "ITEM" + " where ID='C'");
      assertEquals(false, rs.next());
   }

   public void testDeleteValueData() throws Exception
   {

      jdbcConn.deleteValueData("A");
      ResultSet rs =
         connect.createStatement().executeQuery(
            "select * from " + "JCR_" + tableType + "VALUE" + " where PROPERTY_ID='A'");
      assertEquals(false, rs.next());
   }

   public void testFindItemByIdentifier() throws Exception
   {

      ResultSet rsRemote = jdbcConn.findItemByIdentifier("A");
      rsRemote.next();
      ResultSet rs =
         connect.createStatement().executeQuery("select * from " + "JCR_" + tableType + "ITEM" + " where ID='A'");
      rs.next();
      assertEquals(rsRemote.getString("PARENT_ID"), rs.getString("PARENT_ID"));
   }

   public void testFindPropertyByName() throws Exception
   {

      ResultSet rsRemote = jdbcConn.findPropertyByName("A", "test1");
      rsRemote.next();
      ResultSet rs =
         connect
            .createStatement()
            .executeQuery(
               "select V.DATA from JCR_"
                  + tableType
                  + "ITEM I, JCR_"
                  + tableType
                  + "VALUE V "
                  + "where I.I_CLASS=2 and I.PARENT_ID='A' and I.NAME='test1' and I.ID=V.PROPERTY_ID order by V.ORDER_NUM");
      rs.next();
      assertEquals(rsRemote.getString("DATA"), rs.getString("DATA"));
   }

   public void testFindItemByName() throws Exception
   {

      ResultSet rsRemote = jdbcConn.findItemByName("A", "test1", 1233);
      rsRemote.next();
      ResultSet rs =
         connect.createStatement().executeQuery(
            "select * from JCR_" + tableType + "ITEM"
               + " where PARENT_ID='A' and NAME='test1' and I_INDEX=1233 order by I_CLASS, VERSION DESC");
      rs.next();
      assertEquals(rsRemote.getString("ID"), rs.getString("ID"));
   }

   public void testFindChildNodesByParentIdentifier() throws Exception
   {

      ResultSet rsRemote = jdbcConn.findChildNodesByParentIdentifier("A");
      rsRemote.next();
      ResultSet rs =
         connect.createStatement().executeQuery(
            "select * from " + "JCR_" + tableType + "ITEM" + " where I_CLASS=1 and PARENT_ID='A'");
      rs.next();
      assertEquals(rsRemote.getString("NAME"), rs.getString("NAME"));
   }

   public void testFindChildPropertiesByParentIdentifier() throws Exception
   {
      ResultSet rsRemote = jdbcConn.findChildPropertiesByParentIdentifier("A");
      rsRemote.next();
      ResultSet rs =
         connect.createStatement().executeQuery(
            "select * from JCR_" + tableType + "ITEM" + " where I_CLASS=2 and PARENT_ID='A' order by ID");
      rs.next();
      assertEquals(rsRemote.getString("NAME"), rs.getString("NAME"));
   }

   public void testFindReferences() throws Exception
   {

      ResultSet rsRemote = jdbcConn.findReferences("D");
      rsRemote.next();
      ResultSet rs =
         connect.createStatement().executeQuery(
            "select P.ID, P.PARENT_ID, P.VERSION, P.P_TYPE, P.P_MULTIVALUED, P.NAME" + " from JCR_" + tableType
               + "REF R, JCR_" + tableType + "ITEM P" + " where R.NODE_ID='D' and P.ID=R.PROPERTY_ID and P.I_CLASS=2");
      rs.next();
      assertEquals(rsRemote.getString("ID"), rs.getString("ID"));
   }

   public void testFindValuesByPropertyId() throws Exception
   {

      ResultSet rsRemote = jdbcConn.findValuesByPropertyId("A");
      rsRemote.next();
      ResultSet rs =
         connect.createStatement().executeQuery(
            "select PROPERTY_ID, ORDER_NUM, STORAGE_DESC from " + "JCR_" + tableType + "VALUE"
               + " where PROPERTY_ID='A' order by ORDER_NUM");
      rs.next();
      assertEquals(rsRemote.getString("STORAGE_DESC"), rs.getString("STORAGE_DESC"));
   }

   public void testFindValueByPropertyIdOrderNumber() throws Exception
   {

      ResultSet rsRemote = jdbcConn.findValueByPropertyIdOrderNumber("A", 16);
      rsRemote.next();
      ResultSet rs =
         connect.createStatement().executeQuery(
            "select DATA from " + "JCR_" + tableType + "VALUE" + " where PROPERTY_ID='A' and ORDER_NUM=16");
      rs.next();
      assertEquals(rsRemote.getString("DATA"), rs.getString("DATA"));
   }
}
