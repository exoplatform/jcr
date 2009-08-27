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
package org.exoplatform.services.jcr.impl.storage.jdbc.db;

import java.sql.*;

public class SingleDbJDBCConnectionFindTest
   extends JDBCConnectionBaseTest
{
   public void testFindItemByIdentifier() throws Exception
   {
      ResultSet rsRemote = sdbc.findItemByIdentifier("12345");
      rsRemote.next();
      makeFindDB("select * from JCR_SITEM where ID='12345'");
      assertEquals(rs.getString("ID"), rsRemote.getString("ID"));
   }

   public void testFindPropertyByName() throws Exception
   {
      ResultSet rsRemote = sdbc.findPropertyByName("123456", "Sam");
      rsRemote.next();
      makeFindDB("select V.DATA"
               + " from JCR_SITEM I, JCR_SVALUE V"
               + " where I.I_CLASS=2 and I.CONTAINER_NAME='mycontainer' and I.PARENT_ID='123456' and I.NAME='Sam' and I.ID=V.PROPERTY_ID order by V.ORDER_NUM");
      assertEquals(rs.getString("DATA"), rsRemote.getString("DATA"));
   }

   public void testFindItemByName() throws Exception
   {
      ResultSet rsRemote = sdbc.findItemByName("123456", "Sam", 1233);
      rsRemote.next();
      makeFindDB("select * from JCR_SITEM"
               + " where CONTAINER_NAME='mycontainer' and PARENT_ID='123456' and NAME='Sam' and I_INDEX=1233 order by I_CLASS, VERSION DESC");
      assertTrue(rs.getInt("I_INDEX") == rsRemote.getInt("I_INDEX"));
   }

   public void testFindChildNodesByParentIdentifier() throws Exception
   {
      ResultSet rsRemote = sdbc.findChildNodesByParentIdentifier("1235");
      rsRemote.next();
      makeFindDB("select * from JCR_SITEM" + " where I_CLASS=1 and CONTAINER_NAME='myContainer' and PARENT_ID='1235'"
               + " order by N_ORDER_NUM");
      assertEquals(rs.getString("PARENT_ID"), rsRemote.getString("PARENT_ID"));
   }

   public void testFindChildPropertiesByParentIdentifier() throws Exception
   {
      ResultSet rsRemote = sdbc.findChildPropertiesByParentIdentifier("123456");
      rsRemote.next();
      makeFindDB("select * from JCR_SITEM" + " where I_CLASS=2 and CONTAINER_NAME='mycontainer' and PARENT_ID='123456'"
               + " order by ID");
      assertEquals(rs.getString("PARENT_ID"), rsRemote.getString("PARENT_ID"));
   }

   public void testFindReferences() throws Exception
   {
      ResultSet rsRemote = sdbc.findReferences("45as1");
      rsRemote.next();
      makeFindDB("select P.ID, P.PARENT_ID, P.VERSION, P.P_TYPE, P.P_MULTIVALUED, P.NAME"
               + " from JCR_SREF R, JCR_SITEM P"
               + " where R.NODE_ID='45as1' and P.CONTAINER_NAME='mycontainer' and P.ID=R.PROPERTY_ID and P.I_CLASS=2");
      assertEquals(rs.getString("ID"), rsRemote.getString("ID"));
   }

   public void testFindValuesByPropertyId() throws Exception
   {
      ResultSet rsRemote = sdbc.findValuesByPropertyId("12345");
      rsRemote.next();
      makeFindDB("select PROPERTY_ID, ORDER_NUM, STORAGE_DESC from JCR_SVALUE where PROPERTY_ID='12345' order by ORDER_NUM");
      assertEquals(rs.getString("PROPERTY_ID"), rsRemote.getString("PROPERTY_ID"));
   }

   public void testFindValueByPropertyIdOrderNumber() throws Exception
   {
      ResultSet rsRemote = sdbc.findValueByPropertyIdOrderNumber("12345", 16);
      rsRemote.next();
      makeFindDB("select DATA from JCR_SVALUE where PROPERTY_ID='12345' and ORDER_NUM=16");
      assertEquals(rs.getString("DATA"), rsRemote.getString("DATA"));
   }
}
