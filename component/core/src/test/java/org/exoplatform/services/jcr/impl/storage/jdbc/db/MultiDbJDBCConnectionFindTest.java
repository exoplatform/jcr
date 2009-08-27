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

public class MultiDbJDBCConnectionFindTest
   extends JDBCConnectionBaseTest
{
   public void testFindItemByIdentifier() throws Exception
   {
      ResultSet rsRemote = mdbc.findItemByIdentifier("12345");
      rsRemote.next();
      makeFindDB("select * from JCR_MITEM where ID='12345'");
      assertEquals(rs.getString("ID"), rsRemote.getString("ID"));
   }

   public void testFindPropertyByName() throws Exception
   {
      ResultSet rsRemote = mdbc.findPropertyByName("123456", "Sam");
      rsRemote.next();
      makeFindDB("select V.DATA"
               + " from JCR_MITEM I,JCR_MVALUE V"
               + " where I.I_CLASS=2 and I.PARENT_ID='123456' and I.Name='Sam' and I.ID=V.PROPERTY_ID order by V.ORDER_NUM");
      assertEquals(rs.getString("DATA"), rsRemote.getString("DATA"));
   }

   public void testFindItemByName() throws Exception
   {
      ResultSet rsRemote = mdbc.findItemByName("123456", "Sam", 1233);
      rsRemote.next();
      makeFindDB("select * from JCR_MITEM where PARENT_ID='123456' and NAME='Sam' and I_INDEX=1233");
      assertTrue(rs.getInt("I_INDEX") == rsRemote.getInt("I_INDEX"));
   }

   public void testFindChildNodesByParentIdentifier() throws Exception
   {
      ResultSet rsRemote = mdbc.findChildNodesByParentIdentifier("1235");
      rsRemote.next();
      makeFindDB("select * from JCR_MITEM where I_CLASS=1 and PARENT_ID='1235'");
      assertEquals(rs.getString("PARENT_ID"), rsRemote.getString("PARENT_ID"));
   }

   public void testFindChildPropertiesByParentIdentifier() throws Exception
   {
      ResultSet rsRemote = mdbc.findChildPropertiesByParentIdentifier("123456");
      rsRemote.next();
      makeFindDB("select * from JCR_MITEM where I_CLASS=2 and PARENT_ID='123456' order by ID");
      assertEquals(rs.getString("PARENT_ID"), rsRemote.getString("PARENT_ID"));
   }

   public void testFindReferences() throws Exception
   {
      ResultSet rsRemote = mdbc.findReferences("45as1");
      rsRemote.next();
      makeFindDB("select P.ID, P.PARENT_ID, P.VERSION, P.P_TYPE, P.P_MULTIVALUED, P.NAME"
               + " from JCR_MREF R, JCR_MITEM P" + " where R.NODE_ID='45as1' and P.ID=R.PROPERTY_ID and P.I_CLASS=2");
      assertEquals(rs.getString("ID"), rsRemote.getString("ID"));
   }

   public void testFindValuesByPropertyId() throws Exception
   {
      ResultSet rsRemote = mdbc.findValuesByPropertyId("12345");
      rsRemote.next();
      makeFindDB("select PROPERTY_ID, ORDER_NUM, STORAGE_DESC from JCR_MVALUE where PROPERTY_ID='12345' order by ORDER_NUM");
      assertEquals(rs.getString("PROPERTY_ID"), rsRemote.getString("PROPERTY_ID"));
   }

   public void testFindValueByPropertyIdOrderNumber() throws Exception
   {
      ResultSet rsRemote = mdbc.findValueByPropertyIdOrderNumber("12345", 16);
      rsRemote.next();
      makeFindDB("select DATA from JCR_MVALUE where PROPERTY_ID='12345' and ORDER_NUM=16");
      assertEquals(rs.getString("DATA"), rsRemote.getString("DATA"));
   }
}