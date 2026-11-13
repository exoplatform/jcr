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
import static org.junit.Assert.assertFalse;
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
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.junit.Test;

import org.exoplatform.services.jcr.config.ContainerEntry;
import org.exoplatform.services.jcr.config.SimpleParameterEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.TableManipulationConfiguration;

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

   @Test
   public void testOracleAndDb2AreNotGuarded() throws Exception
   {
      // Infinispan looks the table up in the schema of the user on Oracle, in the current one on DB2: a table of
      // another schema is not taken
      assertFalse(ISPNLockUpgrade.lookedUpWithoutSchema(DBConstants.DB_DIALECT_ORACLE));
      assertFalse(ISPNLockUpgrade.lookedUpWithoutSchema(DBConstants.DB_DIALECT_DB2));
      assertTrue(ISPNLockUpgrade.lookedUpWithoutSchema(DBConstants.DB_DIALECT_MYSQL));
      assertTrue(ISPNLockUpgrade.lookedUpWithoutSchema(DBConstants.DB_DIALECT_PGSQL));
      assertTrue(ISPNLockUpgrade.lookedUpWithoutSchema(DBConstants.DB_DIALECT_MSSQL));
      assertTrue(ISPNLockUpgrade.lookedUpWithoutSchema(DBConstants.DB_DIALECT_HSQLDB));
   }

   @Test
   public void testAnOracleWorkspaceStartsBesideTheLockStoreOfAnotherSchema() throws Exception
   {
      Connection connection = mock(Connection.class);
      DatabaseMetaData metaData = mock(DatabaseMetaData.class);
      when(connection.getMetaData()).thenReturn(metaData);
      when(connection.getSchema()).thenReturn("PLF");
      when(metaData.getDatabaseProductName()).thenReturn("Oracle");
      when(metaData.getSearchStringEscape()).thenReturn("\\");
      ResultSet none = mock(ResultSet.class);
      when(metaData.getTables(any(), eq("PLF"), anyString(), any())).thenReturn(none);
      // The table another platform created in its own schema, which a lookup with no schema finds
      ResultSet other = mock(ResultSet.class);
      when(other.next()).thenReturn(true, false);
      when(other.getString("TABLE_SCHEM")).thenReturn("OTHER");
      when(other.getString("TABLE_NAME")).thenReturn("lk_Lws");
      when(metaData.getTables(any(), isNull(), anyString(), any())).thenReturn(other);
      DataSource dataSource = mock(DataSource.class);
      when(dataSource.getConnection()).thenReturn(connection);

      WorkspaceEntry workspace = new WorkspaceEntry();
      workspace.setName("ws");
      workspace.setUniqueName("repo_ws");
      workspace.setContainer(new ContainerEntry(JDBCWorkspaceDataContainer.class.getName(),
         new ArrayList<>(List.of(new SimpleParameterEntry(JDBCWorkspaceDataContainer.DB_STRUCTURE_TYPE, "single")))));
      TableManipulationConfiguration table = new ConfigurationBuilder().persistence()
                                                                       .addStore(JdbcStringBasedStoreConfigurationBuilder.class)
                                                                       .table()
                                                                       .tableNamePrefix("lk")
                                                                       .idColumnName("id")
                                                                       .dataColumnName("data")
                                                                       .timestampColumnName("timestamp")
                                                                       .segmentColumnName("segment_id")
                                                                       .create();

      // Infinispan looks the table up in the user's schema on Oracle: nothing to upgrade, no refusal
      assertFalse(new ISPNLockUpgrade(workspace, dataSource, dataSource, table, nodes -> 0, 300, 20).run());
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
