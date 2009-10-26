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
package org.exoplatform.services.jcr.impl.storage.jdbc.monitor;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.Map;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 2-sep-08
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: ManagedConnection.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class ManagedConnection implements Connection
{

   public static final String JCR_JDBC_CONNECTION_MONITOR = "org.exoplatform.jcr.monitor.jdbcMonitor";

   public static final String PREPARE_INTREST_NAME = "PREPARE";

   public static final String COMMIT_INTEREST_NAME = "COMMIT";

   public static final String CLOSE_INTEREST_NAME = "CLOSE";

   public static final String OPEN_INTEREST_NAME = "OPEN";

   public static final String EXECUTE_INTEREST_NAME = "EXECUTE";

   public static final int PREPARE_INTREST = 1;

   public static final int COMMIT_INTREST = 2;

   public static final int CLOSE_INTREST = 4;

   public static final int OPEN_INTREST = 8;

   public static final int EXECUTE_INTREST = 16;

   protected static final Log LOG = ExoLogger.getLogger("jcr.ManagedConnection");

   protected final Connection jdbcConn;

   protected int interest;

   public ManagedConnection(Connection jdbcConn, int interest)
   {
      this.jdbcConn = jdbcConn;
      this.interest = interest;

      if ((interest & ManagedConnection.OPEN_INTREST) != 0)
         LOG.info(ManagedConnection.OPEN_INTREST + " " + jdbcConn + " - " + System.currentTimeMillis());
   }

   public int getInterest()
   {
      return interest;
   }

   public void setInterest(int interest)
   {
      this.interest = interest;
   }

   public void clearWarnings() throws SQLException
   {
      jdbcConn.clearWarnings();
   }

   public void close() throws SQLException
   {
      jdbcConn.close();

      if ((interest & ManagedConnection.OPEN_INTREST) != 0)
         LOG.info(ManagedConnection.OPEN_INTREST + " - " + System.currentTimeMillis());
   }

   public void commit() throws SQLException
   {
      final long start = System.currentTimeMillis();

      jdbcConn.commit();

      if ((interest & ManagedConnection.COMMIT_INTREST) != 0)
         LOG.info(ManagedConnection.COMMIT_INTEREST_NAME + " - " + (System.currentTimeMillis() - start) + "ms");
   }

   public Statement createStatement() throws SQLException
   {
      return new ManagedStatement(jdbcConn.createStatement(), interest, LOG);
   }

   public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException
   {
      return new ManagedStatement(jdbcConn.createStatement(resultSetType, resultSetConcurrency), interest, LOG);
   }

   public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
      throws SQLException
   {
      return new ManagedStatement(jdbcConn.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability),
         interest, LOG);
   }

   public boolean getAutoCommit() throws SQLException
   {
      return jdbcConn.getAutoCommit();
   }

   public String getCatalog() throws SQLException
   {
      return jdbcConn.getCatalog();
   }

   public int getHoldability() throws SQLException
   {
      return jdbcConn.getHoldability();
   }

   public DatabaseMetaData getMetaData() throws SQLException
   {
      return jdbcConn.getMetaData();
   }

   public int getTransactionIsolation() throws SQLException
   {
      return jdbcConn.getTransactionIsolation();
   }

   public Map<String, Class<?>> getTypeMap() throws SQLException
   {
      return jdbcConn.getTypeMap();
   }

   public SQLWarning getWarnings() throws SQLException
   {
      return jdbcConn.getWarnings();
   }

   public boolean isClosed() throws SQLException
   {
      return jdbcConn.isClosed();
   }

   public boolean isReadOnly() throws SQLException
   {
      return jdbcConn.isReadOnly();
   }

   public String nativeSQL(String sql) throws SQLException
   {
      return jdbcConn.nativeSQL(sql);
   }

   public CallableStatement prepareCall(String sql) throws SQLException
   {
      return jdbcConn.prepareCall(sql);
   }

   public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
   {
      return jdbcConn.prepareCall(sql, resultSetType, resultSetConcurrency);
   }

   public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
      int resultSetHoldability) throws SQLException
   {
      return jdbcConn.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
   }

   public PreparedStatement prepareStatement(String sql) throws SQLException
   {
      return new ManagedPreparedStatement(jdbcConn.prepareStatement(sql), sql, interest, LOG);
   }

   public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException
   {
      return new ManagedPreparedStatement(jdbcConn.prepareStatement(sql, autoGeneratedKeys), sql, interest, LOG);
   }

   public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException
   {
      return new ManagedPreparedStatement(jdbcConn.prepareStatement(sql, columnIndexes), sql, interest, LOG);
   }

   public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException
   {
      return new ManagedPreparedStatement(jdbcConn.prepareStatement(sql, columnNames), sql, interest, LOG);
   }

   public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
      throws SQLException
   {
      return new ManagedPreparedStatement(jdbcConn.prepareStatement(sql, resultSetType, resultSetConcurrency), sql,
         interest, LOG);
   }

   public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
      int resultSetHoldability) throws SQLException
   {
      return new ManagedPreparedStatement(jdbcConn.prepareStatement(sql, resultSetType, resultSetConcurrency,
         resultSetHoldability), sql, interest, LOG);
   }

   public void releaseSavepoint(Savepoint savepoint) throws SQLException
   {
      jdbcConn.releaseSavepoint(savepoint);
   }

   public void rollback() throws SQLException
   {
      jdbcConn.rollback();
   }

   public void rollback(Savepoint savepoint) throws SQLException
   {
      jdbcConn.rollback(savepoint);
   }

   public void setAutoCommit(boolean autoCommit) throws SQLException
   {
      jdbcConn.setAutoCommit(autoCommit);
   }

   public void setCatalog(String catalog) throws SQLException
   {
      jdbcConn.setCatalog(catalog);
   }

   public void setHoldability(int holdability) throws SQLException
   {
      jdbcConn.setHoldability(holdability);
   }

   public void setReadOnly(boolean readOnly) throws SQLException
   {
      jdbcConn.setReadOnly(readOnly);
   }

   public Savepoint setSavepoint() throws SQLException
   {
      return jdbcConn.setSavepoint();
   }

   public Savepoint setSavepoint(String name) throws SQLException
   {
      return jdbcConn.setSavepoint(name);
   }

   public void setTransactionIsolation(int level) throws SQLException
   {
      jdbcConn.setTransactionIsolation(level);
   }

   public void setTypeMap(Map<String, Class<?>> map) throws SQLException
   {
      jdbcConn.setTypeMap(map);
   }

}
