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

import java.io.*;

public class MultiDbJDBCConnectionEditTest
   extends JDBCConnectionBaseTest
{
   public void testAddNode() throws Exception
   {
      mdbc.add(setNode());
      makeFindDB("select * from JCR_MITEM where N_ORDER_NUM=8");
      assertEquals(8, rs.getInt("N_ORDER_NUM"));
   }

   public void testAddValueData() throws Exception
   {
      byte data[] =
      {Byte.parseByte("2")};
      ByteArrayInputStream bas = new ByteArrayInputStream(data);
      mdbc.addValueData("45", 2, bas, 13, "000");
      makeFindDB("select * from JCR_MVALUE where PROPERTY_ID='45'");
      assertEquals("45", rs.getString("PROPERTY_ID"));
   }

   public void testRenameNode() throws Exception
   {
      mdbc.renameNode(giveNode());
      makeFindDB("select * from JCR_MITEM where ID='12345'");
      assertEquals("123456", rs.getString("PARENT_ID"));
   }

   public void testUpdateNodeByIdentifier() throws Exception
   {
      mdbc.updateNodeByIdentifier(200923, 4512, 20, "12345");
      makeFindDB("select * from JCR_MITEM where ID='12345'");
      assertEquals(4512, rs.getInt("I_INDEX"));
   }

   public void testUpdatePropertyByIdentifier() throws Exception
   {
      mdbc.updatePropertyByIdentifier(200923, 4512, "12345");
      makeFindDB("select * from JCR_MITEM where ID='12345'");
      assertEquals(200923, rs.getInt("VERSION"));
   }

   public void testDeleteReference() throws Exception
   {
      mdbc.deleteReference("00214");
      ifDeleted("select * from JCR_MREF where PROPERTY_ID='00214'");
      assertEquals(false, rs.next());
   }

   public void testDeleteItemByIdentifier() throws Exception
   {
      mdbc.deleteItemByIdentifier("123");
      ifDeleted("select * from JCR_MITEM where ID='123'");
      assertEquals(false, rs.next());
   }

   public void testDeleteValueData() throws Exception
   {
      mdbc.deleteValueData("12345");
      ifDeleted("select * from JCR_MVALUE where PROPERTY_ID='12345'");
      assertEquals(false, rs.next());
   }
}