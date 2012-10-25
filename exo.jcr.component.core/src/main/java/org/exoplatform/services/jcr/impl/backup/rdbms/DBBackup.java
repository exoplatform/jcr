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
import org.exoplatform.services.database.utils.JDBCUtils;
import org.exoplatform.services.jcr.core.security.JCRRuntimePermissions;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.backup.BackupException;
import org.exoplatform.services.jcr.impl.dataflow.serialization.ZipObjectWriter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

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
    * Logger.
    */
   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.DBBackup");

   /**
    * Suffix for content file.
    */
   public static final String CONTENT_FILE_SUFFIX = ".dump";

   /**
    * Suffix for content length file.
    */
   public static final String CONTENT_LEN_FILE_SUFFIX = ".len";

   /**
    * Suffix for content file.
    */
   public static final String CONTENT_ZIP_FILE = "dump.zip";

   /**
    * Suffix for content length file.
    */
   public static final String CONTENT_LEN_ZIP_FILE = "dump-len.zip";

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
      Exception exc = null;

      ZipObjectWriter contentWriter = null;
      ZipObjectWriter contentLenWriter = null;

      try
      {
         contentWriter =
            new ZipObjectWriter(PrivilegedFileHelper.zipOutputStream(new File(storageDir, CONTENT_ZIP_FILE)));

         contentLenWriter =
            new ZipObjectWriter(PrivilegedFileHelper.zipOutputStream(new File(storageDir, CONTENT_LEN_ZIP_FILE)));

         for (Entry<String, String> entry : scripts.entrySet())
         {
            dumpTable(jdbcConn, entry.getKey(), entry.getValue(), storageDir, contentWriter, contentLenWriter);
         }
      }
      catch (IOException e)
      {
         exc = e;
         throw new BackupException(e);
      }
      catch (SQLException e)
      {
         exc = e;
         throw new BackupException("SQL Exception: " + JDBCUtils.getFullMessage(e), e);
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
               if (exc != null)
               {
                  LOG.error("Can't close connection", e);
                  throw new BackupException(exc);
               }
               else
               {
                  throw new BackupException(e);
               }
            }
         }

         try
         {
            if (contentWriter != null)
            {
               contentWriter.close();
            }

            if (contentLenWriter != null)
            {
               contentLenWriter.close();
            }
         }
         catch (IOException e)
         {
            if (exc != null)
            {
               LOG.error("Can't close zip", e);
               throw new BackupException(exc);
            }
            else
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
   private static void dumpTable(Connection jdbcConn, String tableName, String script, File storageDir,
      ZipObjectWriter contentWriter, ZipObjectWriter contentLenWriter) throws IOException, SQLException
   {
      // Need privileges
      SecurityManager security = System.getSecurityManager();
      if (security != null)
      {
         security.checkPermission(JCRRuntimePermissions.MANAGE_REPOSITORY_PERMISSION);
      }

      PreparedStatement stmt = null;
      ResultSet rs = null;
      try
      {
         contentWriter.putNextEntry(new ZipEntry(tableName));
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

         byte[] tmpBuff = new byte[2048];

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

                  while ((read = value.read(tmpBuff)) >= 0)
                  {
                     contentWriter.write(tmpBuff, 0, read);
                     len += read;
                  }
                  contentLenWriter.writeLong(len);
               }
            }
         }

         contentWriter.closeEntry();
         contentLenWriter.closeEntry();
      }
      finally
      {
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
