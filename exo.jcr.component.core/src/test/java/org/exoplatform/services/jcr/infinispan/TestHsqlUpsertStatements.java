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
package org.exoplatform.services.jcr.infinispan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.persistence.jdbc.common.DatabaseType;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfigurationBuilder;
import org.infinispan.persistence.jdbc.impl.table.AbstractTableManager;
import org.infinispan.persistence.jdbc.impl.table.DbMetaData;
import org.infinispan.persistence.jdbc.impl.table.TableManagerFactory;
import org.junit.Test;

/**
 * Runs the upsert Infinispan prepares for a JDBC store on HSQLDB, as the lock manager's store does it: the table and the
 * statement come from Infinispan's table manager.
 */
public class TestHsqlUpsertStatements
{
   @Test
   public void testUpsertOfASegmentedTableReplacesTheEntryOfAnId() throws Exception
   {
      upsert(true);
   }

   @Test
   public void testUpsertOfATableWithoutSegmentReplacesTheEntryOfAnId() throws Exception
   {
      upsert(false);
   }

   private static void upsert(boolean segmented) throws Exception
   {
      try (Connection hsqldb = DriverManager.getConnection("jdbc:hsqldb:mem:" + UUID.randomUUID(), "sa", ""))
      {
         JdbcStringBasedStoreConfigurationBuilder store =
            new ConfigurationBuilder().persistence().addStore(JdbcStringBasedStoreConfigurationBuilder.class);
         store.table()
              .tableNamePrefix("lk")
              .idColumnName("id")
              .idColumnType("VARCHAR(255)")
              .dataColumnName("data")
              .dataColumnType("VARBINARY(65535)")
              .timestampColumnName("ts")
              .timestampColumnType("BIGINT")
              .segmentColumnName("segment_id")
              .segmentColumnType("INTEGER");
         store.connectionFactory(ManagedConnectionFactoryConfigurationBuilder.class).jndiUrl("unused");
         AbstractTableManager<?, ?> tables = (AbstractTableManager<?, ?>)TableManagerFactory.getManager(
            new DbMetaData(DatabaseType.HSQL, null, null, 128, false, false, !segmented), null, null, store.create(),
            "ws");
         tables.createDataTable(hsqldb);
         Connection connection = HsqlUpsertStatements.wrap(hsqldb, new HashMap<>());

         try (PreparedStatement upsert = connection.prepareStatement(tables.getUpsertRowSql()))
         {
            bind(upsert, "lock1", "first", 1, segmented);
            bind(upsert, "lock1", "second", 2, segmented);
            bind(upsert, "lock2", "other", 3, segmented);
            upsert.executeBatch();
         }
         try (ResultSet count = connection.createStatement()
                                          .executeQuery("SELECT COUNT(*) FROM " + tables.getDataTableName()))
         {
            assertTrue(count.next());
            assertEquals(2, count.getInt(1));
         }
         assertEquals("second", data(connection, tables, "lock1"));
         assertEquals("other", data(connection, tables, "lock2"));
      }
   }

   /**
    * Binds the parameters as Infinispan does: the data, the timestamp, the id and the segment.
    */
   private static void bind(PreparedStatement upsert, String id, String data, long timestamp, boolean segmented)
      throws SQLException
   {
      byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
      upsert.setBinaryStream(1, new ByteArrayInputStream(bytes), bytes.length);
      upsert.setLong(2, timestamp);
      upsert.setString(3, id);
      if (segmented)
      {
         upsert.setInt(4, 7);
      }
      upsert.addBatch();
   }

   private static String data(Connection connection, AbstractTableManager<?, ?> tables, String id) throws SQLException
   {
      try (PreparedStatement select = connection.prepareStatement(
         "SELECT data FROM " + tables.getDataTableName() + " WHERE id = ?"))
      {
         select.setString(1, id);
         try (ResultSet row = select.executeQuery())
         {
            assertTrue(row.next());
            return new String(row.getBytes(1), StandardCharsets.UTF_8);
         }
      }
   }
}
