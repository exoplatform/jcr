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

import java.io.ByteArrayInputStream;

public class SingleDbJDBCConnectionEditTest
   extends JDBCConnectionBaseTest
{

   public void testAddNode() throws Exception
   {
      sdbc.add(setNode());
      makeFindDB("select * from JCR_SITEM where N_ORDER_NUM=8");
      assertEquals(8, rs.getInt("N_ORDER_NUM"));
   }

   public void testAddValueData() throws Exception
   {
      byte data[] =
      {Byte.parseByte("2")};
      ByteArrayInputStream bas = new ByteArrayInputStream(data);
      sdbc.addValueData("45", 2, bas, 13, "000");
      makeFindDB("select * from JCR_SVALUE where PROPERTY_ID='45'");
      assertEquals("45", rs.getString("PROPERTY_ID"));
   }

   public void testRenameNode() throws Exception
   {
      sdbc.renameNode(giveNode());
      makeFindDB("select * from JCR_SITEM where ID='myContainer123'");
      assertEquals("myContainer4512", rs.getString("PARENT_ID"));
   }

   public void testUpdateNodeByIdentifier() throws Exception
   {
      sdbc.updateNodeByIdentifier(200923, 4512, 20, "12345");
      makeFindDB("select * from JCR_SITEM where ID='12345'");
      assertEquals(20, rs.getInt("N_ORDER_NUM"));
   }

   public void testUpdatePropertyByIdentifier() throws Exception
   {
      sdbc.updatePropertyByIdentifier(200923, 4512, "12345");
      makeFindDB("select * from JCR_SITEM where ID='12345'");
      assertEquals(4512, rs.getInt("P_TYPE"));
   }

   public void testDeleteReference() throws Exception
   {
      sdbc.deleteReference("5987");
      ifDeleted("select * from JCR_SREF where PROPERTY_ID='5987'");
      assertEquals(false, rs.next());
   }

   public void testDeleteItemByIdentifier() throws Exception
   {
      sdbc.deleteItemByIdentifier("myContainer123");
      ifDeleted("select * from JCR_SREF where NODE_ID='myContainer123'");
      assertEquals(false, rs.next());
   }

   public void testDeleteValueData() throws Exception
   {
      sdbc.deleteValueData("12345");
      ifDeleted("select * from JCR_SVALUE where PROPERTY_ID='12345'");
      assertEquals(false, rs.next());
   }
}