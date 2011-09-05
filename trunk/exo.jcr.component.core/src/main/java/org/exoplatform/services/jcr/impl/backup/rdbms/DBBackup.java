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
package org.exoplatform.services.jcr.impl.backup.rdbms;

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.services.database.utils.ExceptionManagementHelper;
import org.exoplatform.services.jcr.core.security.JCRRuntimePermissions;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.backup.BackupException;
import org.exoplatform.services.jcr.impl.dataflow.serialization.ObjectZipWriterImpl;
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;

/**
 * @author <a href="mailto:anatoliy.bazko@gmail.com">Anatoliy Bazko</a>
 * @version $Id: DBBackup.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class DBBackup
{
   /**
    * Suffix for content file.
    */
   public static final String CONTENT_FILE_SUFFIX = ".dump";

   /**
    * Suffix for content length file.
    */
   public static final String CONTENT_LEN_FILE_SUFFIX = ".len";

   /**
    * MySQL dialect.
    */
   public static final int DB_DIALECT_MYSQL = DBConstants.DB_DIALECT_MYSQL.hashCode();

   /**
    * MySQL-UTF8 dialect.
    */
   public static final int DB_DIALECT_MYSQL_UTF8 = DBConstants.DB_DIALECT_MYSQL_UTF8.hashCode();

   /**
    * DB2 dialect.
    */
   public static final int DB_DIALECT_DB2 = DBConstants.DB_DIALECT_DB2.hashCode();

   /**
    * DB2V8 dialect.
    */
   public static final int DB_DIALECT_DB2V8 = DBConstants.DB_DIALECT_DB2V8.hashCode();

   /**
    * PGSQL dialect.
    */
   public static final int DB_DIALECT_PGSQL = DBConstants.DB_DIALECT_PGSQL.hashCode();

   /**
    * SYBASE dialect.
    */
   public static final int DB_DIALECT_SYBASE = DBConstants.DB_DIALECT_SYBASE.hashCode();

   /**
    * Backup tables.
    * 
    * @param storageDir
    *          the directory to store data
    * @param jdbcConn
    *          the connection to database
    * @param scripts
    *          map which contains table name and respective SQL query to get data
    * @throws BackupException
    *        if any exception occurred
    */
   public static void backup(File storageDir, Connection jdbcConn, Map<String, String> scripts) throws BackupException
   {
      try
      {
         for (Entry<String, String> entry : scripts.entrySet())
         {
            dumpTable(jdbcConn, entry.getKey(), entry.getValue(), storageDir);
         }
      }
      catch (IOException e)
      {
         throw new BackupException(e);
      }
      catch (SQLException e)
      {
         throw new BackupException("SQL Exception: " + ExceptionManagementHelper.getFullSQLExceptionMessage(e), e);
      }
      finally
      {
         if (jdbcConn != null)
         {
            try
            {
               jdbcConn.close();
            }
            catch (SQLException e)
            {
               throw new BackupException(e);
            }
         }
      }
   }

   /**
    * Dump table.
    * 
    * @throws IOException 
    * @throws SQLException 
    */
   private static void dumpTable(Connection jdbcConn, String tableName, String script, File storageDir)
      throws IOException, SQLException
   {
      // Need privileges
      SecurityManager security = System.getSecurityManager();
      if (security != null)
      {
         security.checkPermission(JCRRuntimePermissions.MANAGE_REPOSITORY_PERMISSION);
      }

      ObjectZipWriterImpl contentWriter = null;
      ObjectZipWriterImpl contentLenWriter = null;
      PreparedStatement stmt = null;
      ResultSet rs = null;
      try
      {
         File contentFile = new File(storageDir, tableName + CONTENT_FILE_SUFFIX);
         contentWriter = new ObjectZipWriterImpl(PrivilegedFileHelper.zipOutputStream(contentFile));
         contentWriter.putNextEntry(new ZipEntry(tableName));

         File contentLenFile = new File(storageDir, tableName + CONTENT_LEN_FILE_SUFFIX);
         contentLenWriter = new ObjectZipWriterImpl(PrivilegedFileHelper.zipOutputStream(contentLenFile));
         contentLenWriter.putNextEntry(new ZipEntry(tableName));

         stmt = jdbcConn.prepareStatement(script);
         rs = stmt.executeQuery();
         ResultSetMetaData metaData = rs.getMetaData();

         int columnCount = metaData.getColumnCount();
         int[] columnType = new int[columnCount];

         contentWriter.writeInt(columnCount);
         for (int i = 0; i < columnCount; i++)
         {
            columnType[i] = metaData.getColumnType(i + 1);
            contentWriter.writeInt(columnType[i]);
            contentWriter.writeString(metaData.getColumnName(i + 1));
         }

         // Now we can output the actual data
         while (rs.next())
         {
            for (int i = 0; i < columnCount; i++)
            {
               InputStream value;
               if (columnType[i] == Types.VARBINARY || columnType[i] == Types.LONGVARBINARY
                  || columnType[i] == Types.BLOB || columnType[i] == Types.BINARY || columnType[i] == Types.OTHER)
               {
                  value = rs.getBinaryStream(i + 1);
               }
               else
               {
                  String str = rs.getString(i + 1);
                  value = str == null ? null : new ByteArrayInputStream(str.getBytes(Constants.DEFAULT_ENCODING));
               }

               if (value == null)
               {
                  contentLenWriter.writeLong(-1);
               }
               else
               {
                  long len = 0;
                  int read = 0;
                  byte[] tmpBuff = new byte[2048];

                  while ((read = value.read(tmpBuff)) >= 0)
                  {
                     contentWriter.write(tmpBuff, 0, read);
                     len += read;
                  }
                  contentLenWriter.writeLong(len);
               }
            }
         }
      }
      finally
      {
         if (contentWriter != null)
         {
            contentWriter.closeEntry();
            contentWriter.close();
         }

         if (contentLenWriter != null)
         {
            contentLenWriter.closeEntry();
            contentLenWriter.close();
         }

         if (rs != null)
         {
            rs.close();
         }

         if (stmt != null)
         {
            stmt.close();
         }
      }
   }

}
