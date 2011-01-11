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
package org.exoplatform.services.jcr.impl.storage.jdbc.backup;

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.serialization.ObjectZipWriterImpl;
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.services.jcr.impl.storage.jdbc.DialectDetecter;

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
import java.util.zip.ZipEntry;

/**
 * @author <a href="mailto:anatoliy.bazko@gmail.com">Anatoliy Bazko</a>
 * @version $Id: DumpTable.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class DumpTable
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
    * Generic dialect.
    */
   public static final int DB_DIALECT_GENERIC = DBConstants.DB_DIALECT_GENERIC.hashCode();

   /**
    * HSQLDB dialect.
    */
   public static final int DB_DIALECT_HSQLDB = DBConstants.DB_DIALECT_HSQLDB.hashCode();

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
    * Dump table.
    * 
    * @throws IOException 
    * @throws SQLException 
    */
   public static void dump(Connection jdbcConn, String tableName, String script, File storageDir)
      throws IOException, SQLException
   {
      // Need privileges
      SecurityManager security = System.getSecurityManager();
      if (security != null)
      {
         security.checkPermission(Backupable.BACKUP_RESTORE_PERMISSION);
      }

      int dialect = DialectDetecter.detect(jdbcConn.getMetaData()).hashCode();

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
               if (dialect == DB_DIALECT_HSQLDB)
               {
                  if (columnType[i] == Types.VARBINARY)
                  {
                     value = rs.getBinaryStream(i + 1);
                  }
                  else
                  {
                     String str = rs.getString(i + 1);
                     value = str == null ? null : new ByteArrayInputStream(str.getBytes(Constants.DEFAULT_ENCODING));
                  }
               }
               else
               {
                  value = rs.getBinaryStream(i + 1);
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
