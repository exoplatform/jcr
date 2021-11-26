/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>Date: 2011
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id: SybaseJDBCConnectionHelper.java 111 2011-11-11 11:11:11Z rainf0x $
 */
public class SybaseJDBCConnectionHelper
{

   /**
    * The name for temporary table. 
    */
   public final static String TEMP_A_TABLE_NAME = "#tempA";

   /**
    * The name for temporary table. 
    */
   public final static String TEMP_B_TABLE_NAME = "#tempB";

   /**
    * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
    * @version $Id: SybaseJDBCConnectionHelper.java 111 2011-11-11 11:11:11Z rainf0x $
    *
    * The class EmptyResultSet will be used if need return empty result set for query. 
    */
   static public class EmptyResultSet
      implements ResultSet
   {

      public boolean absolute(int row) throws SQLException
      {
         return false;
      }

      public void afterLast() throws SQLException
      {
      }

      public void beforeFirst() throws SQLException
      {
      }

      public void cancelRowUpdates() throws SQLException
      {
      }

      public void clearWarnings() throws SQLException
      {
      }

      public void close() throws SQLException
      {
      }

      public void deleteRow() throws SQLException
      {
      }

      public int findColumn(String columnLabel) throws SQLException
      {
         return 0;
      }

      public boolean first() throws SQLException
      {
         return false;
      }

      public Array getArray(int columnIndex) throws SQLException
      {
         return null;
      }

      public Array getArray(String columnLabel) throws SQLException
      {
         return null;
      }

      public InputStream getAsciiStream(int columnIndex) throws SQLException
      {
         return null;
      }

      public InputStream getAsciiStream(String columnLabel) throws SQLException
      {
         return null;
      }

      public BigDecimal getBigDecimal(int columnIndex) throws SQLException
      {
         return null;
      }

      public BigDecimal getBigDecimal(String columnLabel) throws SQLException
      {
         return null;
      }

      public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException
      {
         return null;
      }

      public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException
      {
         return null;
      }

      public InputStream getBinaryStream(int columnIndex) throws SQLException
      {
         return null;
      }

      public InputStream getBinaryStream(String columnLabel) throws SQLException
      {
         return null;
      }

      public Blob getBlob(int columnIndex) throws SQLException
      {
         return null;
      }

      public Blob getBlob(String columnLabel) throws SQLException
      {
         return null;
      }

      public boolean getBoolean(int columnIndex) throws SQLException
      {
         return false;
      }

      public boolean getBoolean(String columnLabel) throws SQLException
      {
         return false;
      }

      public byte getByte(int columnIndex) throws SQLException
      {
         return 0;
      }

      public byte getByte(String columnLabel) throws SQLException
      {
         return 0;
      }

      public byte[] getBytes(int columnIndex) throws SQLException
      {
         return null;
      }

      public byte[] getBytes(String columnLabel) throws SQLException
      {
         return null;
      }

      public Reader getCharacterStream(int columnIndex) throws SQLException
      {
         return null;
      }

      public Reader getCharacterStream(String columnLabel) throws SQLException
      {
         return null;
      }

      public Clob getClob(int columnIndex) throws SQLException
      {
         return null;
      }

      public Clob getClob(String columnLabel) throws SQLException
      {
         return null;
      }

      public int getConcurrency() throws SQLException
      {
         return 0;
      }

      public String getCursorName() throws SQLException
      {
         return null;
      }

      public Date getDate(int columnIndex) throws SQLException
      {
         return null;
      }

      public Date getDate(String columnLabel) throws SQLException
      {
         return null;
      }

      public Date getDate(int columnIndex, Calendar cal) throws SQLException
      {
         return null;
      }

      public Date getDate(String columnLabel, Calendar cal) throws SQLException
      {
         return null;
      }

      public double getDouble(int columnIndex) throws SQLException
      {
         return 0;
      }

      public double getDouble(String columnLabel) throws SQLException
      {
         return 0;
      }

      public int getFetchDirection() throws SQLException
      {
         return 0;
      }

      public int getFetchSize() throws SQLException
      {
         return 0;
      }

      public float getFloat(int columnIndex) throws SQLException
      {
         return 0;
      }

      public float getFloat(String columnLabel) throws SQLException
      {
         return 0;
      }

      public int getHoldability() throws SQLException
      {
         return 0;
      }

      public int getInt(int columnIndex) throws SQLException
      {
         return 0;
      }

      public int getInt(String columnLabel) throws SQLException
      {
         return 0;
      }

      public long getLong(int columnIndex) throws SQLException
      {
         return 0;
      }

      public long getLong(String columnLabel) throws SQLException
      {
         return 0;
      }

      public ResultSetMetaData getMetaData() throws SQLException
      {
         return null;
      }

      public Reader getNCharacterStream(int columnIndex) throws SQLException
      {
         return null;
      }

      public Reader getNCharacterStream(String columnLabel) throws SQLException
      {
         return null;
      }

      public NClob getNClob(int columnIndex) throws SQLException
      {
         return null;
      }

      public NClob getNClob(String columnLabel) throws SQLException
      {
         return null;
      }

      public String getNString(int columnIndex) throws SQLException
      {
         return null;
      }

      public String getNString(String columnLabel) throws SQLException
      {
         return null;
      }

      public Object getObject(int columnIndex) throws SQLException
      {
         return null;
      }

      public Object getObject(String columnLabel) throws SQLException
      {
         return null;
      }

      public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException
      {
         return null;
      }

      public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException
      {
         return null;
      }

      public Ref getRef(int columnIndex) throws SQLException
      {
         return null;
      }

      public Ref getRef(String columnLabel) throws SQLException
      {
         return null;
      }

      public int getRow() throws SQLException
      {
         return 0;
      }

      public RowId getRowId(int columnIndex) throws SQLException
      {
         return null;
      }

      public RowId getRowId(String columnLabel) throws SQLException
      {
         return null;
      }

      public SQLXML getSQLXML(int columnIndex) throws SQLException
      {
         return null;
      }

      public SQLXML getSQLXML(String columnLabel) throws SQLException
      {
         return null;
      }

      public short getShort(int columnIndex) throws SQLException
      {
         return 0;
      }

      public short getShort(String columnLabel) throws SQLException
      {
         return 0;
      }

      public Statement getStatement() throws SQLException
      {
         return null;
      }

      public String getString(int columnIndex) throws SQLException
      {
         return null;
      }

      public String getString(String columnLabel) throws SQLException
      {
         return null;
      }

      public Time getTime(int columnIndex) throws SQLException
      {
         return null;
      }

      public Time getTime(String columnLabel) throws SQLException
      {
         return null;
      }

      public Time getTime(int columnIndex, Calendar cal) throws SQLException
      {
         return null;
      }

      public Time getTime(String columnLabel, Calendar cal) throws SQLException
      {
         return null;
      }

      public Timestamp getTimestamp(int columnIndex) throws SQLException
      {
         return null;
      }

      public Timestamp getTimestamp(String columnLabel) throws SQLException
      {
         return null;
      }

      public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException
      {
         return null;
      }

      public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException
      {
         return null;
      }

      public int getType() throws SQLException
      {
         return 0;
      }

      public URL getURL(int columnIndex) throws SQLException
      {
         return null;
      }

      public URL getURL(String columnLabel) throws SQLException
      {
         return null;
      }

      public InputStream getUnicodeStream(int columnIndex) throws SQLException
      {
         return null;
      }

      public InputStream getUnicodeStream(String columnLabel) throws SQLException
      {
         return null;
      }

      public SQLWarning getWarnings() throws SQLException
      {
         return null;
      }

      public void insertRow() throws SQLException
      {

      }

      public boolean isAfterLast() throws SQLException
      {
         return false;
      }

      public boolean isBeforeFirst() throws SQLException
      {
         return false;
      }

      public boolean isClosed() throws SQLException
      {
         return false;
      }

      public boolean isFirst() throws SQLException
      {
         return false;
      }

      public boolean isLast() throws SQLException
      {
         return false;
      }

      public boolean last() throws SQLException
      {
         return false;
      }

      public void moveToCurrentRow() throws SQLException
      {

      }

      public void moveToInsertRow() throws SQLException
      {

      }

      public boolean next() throws SQLException
      {
         return false;
      }

      public boolean previous() throws SQLException
      {
         return false;
      }

      public void refreshRow() throws SQLException
      {

      }

      public boolean relative(int rows) throws SQLException
      {
         return false;
      }

      public boolean rowDeleted() throws SQLException
      {
         return false;
      }

      public boolean rowInserted() throws SQLException
      {
         return false;
      }

      public boolean rowUpdated() throws SQLException
      {
         return false;
      }

      public void setFetchDirection(int direction) throws SQLException
      {
      }

      public void setFetchSize(int rows) throws SQLException
      {
      }

      public void updateArray(int columnIndex, Array x) throws SQLException
      {
      }

      public void updateArray(String columnLabel, Array x) throws SQLException
      {
      }

      public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException
      {
      }

      public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException
      {
      }

      public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException
      {
      }

      public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException
      {
      }

      public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException
      {
      }

      public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException
      {
      }

      public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException
      {
      }

      public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException
      {
      }

      public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException
      {
      }

      public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException
      {
      }

      public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException
      {
      }

      public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException
      {
      }

      public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException
      {
      }

      public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException
      {
      }

      public void updateBlob(int columnIndex, Blob x) throws SQLException
      {
      }

      public void updateBlob(String columnLabel, Blob x) throws SQLException
      {
      }

      public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException
      {
      }

      public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException
      {
      }

      public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException
      {
      }

      public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException
      {
      }

      public void updateBoolean(int columnIndex, boolean x) throws SQLException
      {
      }

      public void updateBoolean(String columnLabel, boolean x) throws SQLException
      {
      }

      public void updateByte(int columnIndex, byte x) throws SQLException
      {
      }

      public void updateByte(String columnLabel, byte x) throws SQLException
      {
      }

      public void updateBytes(int columnIndex, byte[] x) throws SQLException
      {
      }

      public void updateBytes(String columnLabel, byte[] x) throws SQLException
      {
      }

      public void updateCharacterStream(int columnIndex, Reader x) throws SQLException
      {
      }

      public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException
      {
      }

      public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException
      {
      }

      public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException
      {
      }

      public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException
      {
      }

      public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException
      {
      }

      public void updateClob(int columnIndex, Clob x) throws SQLException
      {
      }

      public void updateClob(String columnLabel, Clob x) throws SQLException
      {
      }

      public void updateClob(int columnIndex, Reader reader) throws SQLException
      {
      }

      public void updateClob(String columnLabel, Reader reader) throws SQLException
      {
      }

      public void updateClob(int columnIndex, Reader reader, long length) throws SQLException
      {
      }

      public void updateClob(String columnLabel, Reader reader, long length) throws SQLException
      {
      }

      public void updateDate(int columnIndex, Date x) throws SQLException
      {
      }

      public void updateDate(String columnLabel, Date x) throws SQLException
      {
      }

      public void updateDouble(int columnIndex, double x) throws SQLException
      {
      }

      public void updateDouble(String columnLabel, double x) throws SQLException
      {
      }

      public void updateFloat(int columnIndex, float x) throws SQLException
      {
      }

      public void updateFloat(String columnLabel, float x) throws SQLException
      {
      }

      public void updateInt(int columnIndex, int x) throws SQLException
      {
      }

      public void updateInt(String columnLabel, int x) throws SQLException
      {
      }

      public void updateLong(int columnIndex, long x) throws SQLException
      {
      }

      public void updateLong(String columnLabel, long x) throws SQLException
      {
      }

      public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException
      {
      }

      public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException
      {
      }

      public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException
      {
      }

      public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException
      {
      }

      public void updateNClob(int columnIndex, NClob nClob) throws SQLException
      {
      }

      public void updateNClob(String columnLabel, NClob nClob) throws SQLException
      {
      }

      public void updateNClob(int columnIndex, Reader reader) throws SQLException
      {
      }

      public void updateNClob(String columnLabel, Reader reader) throws SQLException
      {
      }

      public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException
      {
      }

      public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException
      {
      }

      public void updateNString(int columnIndex, String nString) throws SQLException
      {
      }

      public void updateNString(String columnLabel, String nString) throws SQLException
      {
      }

      public void updateNull(int columnIndex) throws SQLException
      {
      }

      public void updateNull(String columnLabel) throws SQLException
      {
      }

      public void updateObject(int columnIndex, Object x) throws SQLException
      {
      }

      public void updateObject(String columnLabel, Object x) throws SQLException
      {
      }

      public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException
      {
      }

      public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException
      {
      }

      public void updateRef(int columnIndex, Ref x) throws SQLException
      {
      }

      public void updateRef(String columnLabel, Ref x) throws SQLException
      {
      }

      public void updateRow() throws SQLException
      {
      }

      public void updateRowId(int columnIndex, RowId x) throws SQLException
      {
      }

      public void updateRowId(String columnLabel, RowId x) throws SQLException
      {
      }

      public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException
      {
      }

      public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException
      {
      }

      public void updateShort(int columnIndex, short x) throws SQLException
      {
      }

      public void updateShort(String columnLabel, short x) throws SQLException
      {
      }

      public void updateString(int columnIndex, String x) throws SQLException
      {
      }

      public void updateString(String columnLabel, String x) throws SQLException
      {
      }

      public void updateTime(int columnIndex, Time x) throws SQLException
      {
      }

      public void updateTime(String columnLabel, Time x) throws SQLException
      {
      }

      public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException
      {
      }

      public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException
      {
      }

      public boolean wasNull() throws SQLException
      {
         return false;
      }

      public boolean isWrapperFor(Class<?> iface) throws SQLException
      {
         return false;
      }

      public <T> T unwrap(Class<T> iface) throws SQLException
      {
         return null;
      }

      public <T> T getObject(int columnIndex, Class<T> type) throws SQLException
      {
         return null;
      }

      public <T> T getObject(String columnLabel, Class<T> type) throws SQLException
      {
         return null;
      }
   }
}