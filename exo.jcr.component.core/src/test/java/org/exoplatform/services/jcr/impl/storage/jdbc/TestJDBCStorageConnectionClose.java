/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/ .
 */
package org.exoplatform.services.jcr.impl.storage.jdbc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;

import org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db.MySQLSingleDbJDBCConnection;

import junit.framework.TestCase;

public class TestJDBCStorageConnectionClose extends TestCase {

  public void testAReadOnlyCloseEndsTheTransactionItsReadsOpened() throws Exception {
    // A pooled connection that a writer left with autocommit off
    Connection connection = connection(false);

    new MySQLSingleDbJDBCConnection(connection, true, config()).close();

    // Its REPEATABLE READ snapshot must not go back to the pool for the next borrower
    verify(connection).rollback();
    verify(connection).close();
  }

  public void testAReadOnlyCloseInAutocommitRollsNothingBack() throws Exception {
    Connection connection = connection(true);

    new MySQLSingleDbJDBCConnection(connection, true, config()).close();

    verify(connection, never()).rollback();
    verify(connection).close();
  }

  public void testAReadOnlyCloseOfAConnectionStillUsedRollsNothingBack() throws Exception {
    Connection connection = connection(false);
    JDBCStorageConnection storageConnection = new MySQLSingleDbJDBCConnection(connection, true, config());
    // Another storage connection, a writer's, uses the same JDBC connection
    storageConnection.share();

    storageConnection.close();

    verify(connection, never()).rollback();
    verify(connection, never()).close();
  }

  private static Connection connection(boolean autoCommit) throws SQLException {
    Connection connection = mock(Connection.class);
    when(connection.getAutoCommit()).thenReturn(autoCommit);
    when(connection.getTransactionIsolation()).thenReturn(Connection.TRANSACTION_REPEATABLE_READ);
    return connection;
  }

  private static JDBCDataContainerConfig config() {
    JDBCDataContainerConfig config = new JDBCDataContainerConfig();
    config.containerName = "ws";
    config.dbDialect = DBConstants.DB_DIALECT_MYSQL;
    config.dbStructureType = JDBCDataContainerConfig.DatabaseStructureType.SINGLE;
    return config;
  }
}
