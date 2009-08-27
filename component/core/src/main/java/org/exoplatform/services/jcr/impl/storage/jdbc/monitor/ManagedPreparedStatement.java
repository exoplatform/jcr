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

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 2-sep-08
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: ManagedPreparedStatement.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class ManagedPreparedStatement
   extends ManagedStatement
   implements PreparedStatement
{

   protected final PreparedStatement jdbcStmt;

   protected final String sqlStmt;

   ManagedPreparedStatement(PreparedStatement jdbcStmt, String sqlStmt, int interest, Log log)
   {
      super(jdbcStmt, interest, log);
      this.jdbcStmt = jdbcStmt;
      this.sqlStmt = sqlStmt;
   }

   public void addBatch() throws SQLException
   {
      jdbcStmt.addBatch();
   }

   public void clearParameters() throws SQLException
   {
      jdbcStmt.clearParameters();
   }

   public boolean execute() throws SQLException
   {
      return jdbcStmt.execute();
   }

   public ResultSet executeQuery() throws SQLException
   {
      final long start = System.currentTimeMillis();
      try
      {
         return jdbcStmt.executeQuery();
      }
      finally
      {
         if ((interest & ManagedConnection.EXECUTE_INTREST) != 0)
            log.info(ManagedConnection.EXECUTE_INTEREST_NAME + " " + sqlStmt + " - "
                     + (System.currentTimeMillis() - start) + "ms");
      }
   }

   public int executeUpdate() throws SQLException
   {
      final long start = System.currentTimeMillis();
      try
      {
         return jdbcStmt.executeUpdate();
      }
      finally
      {
         if ((interest & ManagedConnection.EXECUTE_INTREST) != 0)
            log.info(ManagedConnection.EXECUTE_INTEREST_NAME + " " + sqlStmt + " - "
                     + (System.currentTimeMillis() - start) + "ms");
      }
   }

   public ResultSetMetaData getMetaData() throws SQLException
   {
      return jdbcStmt.getMetaData();
   }

   public ParameterMetaData getParameterMetaData() throws SQLException
   {
      return jdbcStmt.getParameterMetaData();
   }

   public void setArray(int i, Array x) throws SQLException
   {
      jdbcStmt.setArray(i, x);
   }

   public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException
   {
      jdbcStmt.setAsciiStream(parameterIndex, x, length);
   }

   public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException
   {
      jdbcStmt.setBigDecimal(parameterIndex, x);
   }

   public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException
   {
      jdbcStmt.setBinaryStream(parameterIndex, x, length);
   }

   public void setBlob(int i, Blob x) throws SQLException
   {
      jdbcStmt.setBlob(i, x);
   }

   public void setBoolean(int parameterIndex, boolean x) throws SQLException
   {
      jdbcStmt.setBoolean(parameterIndex, x);
   }

   public void setByte(int parameterIndex, byte x) throws SQLException
   {
      jdbcStmt.setByte(parameterIndex, x);
   }

   public void setBytes(int parameterIndex, byte[] x) throws SQLException
   {
      jdbcStmt.setBytes(parameterIndex, x);
   }

   public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException
   {
      jdbcStmt.setCharacterStream(parameterIndex, reader, length);
   }

   public void setClob(int i, Clob x) throws SQLException
   {
      jdbcStmt.setClob(i, x);
   }

   public void setDate(int parameterIndex, Date x) throws SQLException
   {
      jdbcStmt.setDate(parameterIndex, x);
   }

   public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException
   {
      jdbcStmt.setDate(parameterIndex, x, cal);
   }

   public void setDouble(int parameterIndex, double x) throws SQLException
   {
      jdbcStmt.setDouble(parameterIndex, x);
   }

   public void setFloat(int parameterIndex, float x) throws SQLException
   {
      jdbcStmt.setFloat(parameterIndex, x);
   }

   public void setInt(int parameterIndex, int x) throws SQLException
   {
      jdbcStmt.setInt(parameterIndex, x);
   }

   public void setLong(int parameterIndex, long x) throws SQLException
   {
      jdbcStmt.setLong(parameterIndex, x);
   }

   public void setNull(int parameterIndex, int sqlType) throws SQLException
   {
      jdbcStmt.setNull(parameterIndex, sqlType);
   }

   public void setNull(int paramIndex, int sqlType, String typeName) throws SQLException
   {
      jdbcStmt.setNull(paramIndex, sqlType, typeName);
   }

   public void setObject(int parameterIndex, Object x) throws SQLException
   {
      jdbcStmt.setObject(parameterIndex, x);
   }

   public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException
   {
      jdbcStmt.setObject(parameterIndex, x, targetSqlType);
   }

   public void setObject(int parameterIndex, Object x, int targetSqlType, int scale) throws SQLException
   {
      jdbcStmt.setObject(parameterIndex, x, targetSqlType, scale);
   }

   public void setRef(int i, Ref x) throws SQLException
   {
      jdbcStmt.setRef(i, x);
   }

   public void setShort(int parameterIndex, short x) throws SQLException
   {
      jdbcStmt.setShort(parameterIndex, x);
   }

   public void setString(int parameterIndex, String x) throws SQLException
   {
      jdbcStmt.setString(parameterIndex, x);
   }

   public void setTime(int parameterIndex, Time x) throws SQLException
   {
      jdbcStmt.setTime(parameterIndex, x);
   }

   public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException
   {
      jdbcStmt.setTime(parameterIndex, x, cal);
   }

   public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException
   {
      jdbcStmt.setTimestamp(parameterIndex, x);
   }

   public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException
   {
      jdbcStmt.setTimestamp(parameterIndex, x, cal);
   }

   public void setURL(int parameterIndex, URL x) throws SQLException
   {
      jdbcStmt.setURL(parameterIndex, x);
   }

   public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException
   {
      jdbcStmt.setUnicodeStream(parameterIndex, x, length);
   }

}
