/*
 * Copyright (C) 2026 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.core.lock.infinispan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

import org.junit.Test;

/**
 * The lookup of the lock store tables on a connection that names its schema, and on one that cannot.
 */
public class TestISPNLockUpgradeSchema
{
   @Test
   public void testATableIsLookedUpInTheSchemaOfTheConnection() throws Exception
   {
      Connection connection = connection();
      when(connection.getSchema()).thenReturn("exo");

      assertTrue(ISPNLockUpgrade.tableExists(connection, "\"JCR_LOCK_WS\""));

      verify(connection.getMetaData()).getTables(any(), eq("exo"), anyString(), isNull());
   }

   @Test
   public void testAConnectionOfAPoolWrittenBeforeJdbc41IsLookedUpInEverySchema() throws Exception
   {
      // Commons DBCP 1 wraps the connection in a class that has no getSchema
      Connection connection = connection();
      when(connection.getSchema()).thenThrow(new AbstractMethodError("getSchema"));

      assertTrue(ISPNLockUpgrade.tableExists(connection, "\"JCR_LOCK_WS\""));

      verify(connection.getMetaData()).getTables(any(), isNull(), anyString(), isNull());
   }

   @Test
   public void testATableOfAnotherMySQLDatabaseIsFoundElsewhere() throws Exception
   {
      // MySQL Connector/J names the database as the catalog, and has no schema
      Connection connection = mock(Connection.class);
      DatabaseMetaData metaData = mock(DatabaseMetaData.class);
      ResultSet found = mock(ResultSet.class);
      when(connection.getMetaData()).thenReturn(metaData);
      when(connection.getCatalog()).thenReturn("plf");
      when(metaData.getTables(isNull(), isNull(), eq("JCR_LOCK_Lws"), any())).thenReturn(found);
      when(found.next()).thenReturn(true, true, false);
      when(found.getString("TABLE_CAT")).thenReturn("plf", "other_plf");
      when(found.getString("TABLE_NAME")).thenReturn("jcr_lock_lws", "JCR_LOCK_Lws");

      assertEquals("other_plf.JCR_LOCK_Lws", ISPNLockUpgrade.findElsewhere(connection, "`JCR_LOCK_Lws`"));
   }

   @Test
   public void testATableOfTheDatabaseOfTheConnectionIsNotFoundElsewhere() throws Exception
   {
      Connection connection = mock(Connection.class);
      DatabaseMetaData metaData = mock(DatabaseMetaData.class);
      ResultSet found = mock(ResultSet.class);
      when(connection.getMetaData()).thenReturn(metaData);
      when(connection.getCatalog()).thenReturn("plf");
      when(metaData.getTables(isNull(), isNull(), eq("JCR_LOCK_Lws"), any())).thenReturn(found);
      when(found.next()).thenReturn(true, false);
      when(found.getString("TABLE_CAT")).thenReturn("PLF");
      when(found.getString("TABLE_NAME")).thenReturn("jcr_lock_lws");

      assertNull(ISPNLockUpgrade.findElsewhere(connection, "`JCR_LOCK_Lws`"));
   }

   private static Connection connection() throws Exception
   {
      Connection connection = mock(Connection.class);
      DatabaseMetaData metaData = mock(DatabaseMetaData.class);
      ResultSet found = mock(ResultSet.class);
      when(connection.getMetaData()).thenReturn(metaData);
      when(metaData.getSearchStringEscape()).thenReturn("\\");
      when(metaData.getTables(any(), any(), anyString(), any())).thenReturn(found);
      when(found.next()).thenReturn(true);
      return connection;
   }
}
