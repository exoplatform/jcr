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
package org.exoplatform.services.jcr.impl.storage.jdbc.backup.util;

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.dataflow.serialization.ObjectReader;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.serialization.ObjectZipReaderImpl;
import org.exoplatform.services.jcr.impl.storage.jdbc.DialectDetecter;
import org.exoplatform.services.jcr.impl.storage.jdbc.backup.Backupable;
import org.exoplatform.services.jcr.impl.storage.jdbc.backup.RestoreException;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * @author <a href="mailto:anatoliy.bazko@gmail.com">Anatoliy Bazko</a>
 * @version $Id: DumpTable.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class RestoreTables
{
   /**
    * List of temporary files.
    */
   private final List<File> spoolFileList = new ArrayList<File>();

   /**
    *  The file cleaner.
    */
   private final FileCleaner fileCleaner;

   /**
    * Temporary directory.
    */
   private final File tempDir;

   /**
    * Maximum buffer size.
    */
   private final int maxBufferSize;

   /**
    * Logger.
    */
   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.JDBCWorkspaceDataContainer");

   /**
    * Constructor RestoreTable.
    */
   public RestoreTables(FileCleaner fileCleaner, File tempDir, int maxBufferSize)
   {
      this.fileCleaner = fileCleaner;
      this.tempDir = tempDir;
      this.maxBufferSize = maxBufferSize;
   }

   /**
    * {@inheritDoc}
    */
   public void restore(File storageDir, String dsName, Map<String, RestoreTableRule> tables)
      throws RestoreException
   {
      Connection jdbcConn = null;
      Connection jdbcConn1 = null;
      Statement st = null;
      RestoreException exc = null;

      try
      {
         final DataSource ds = (DataSource)new InitialContext().lookup(dsName);
         if (ds == null)
         {
            throw new NameNotFoundException("Data source " + dsName + " not found");
         }

         jdbcConn = SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<Connection>()
         {
            public Connection run() throws Exception
            {
               return ds.getConnection();

            }
         });
         jdbcConn.setAutoCommit(false);

         int dialect = DialectDetecter.detect(jdbcConn.getMetaData()).hashCode();

         for (Entry<String, RestoreTableRule> entry : tables.entrySet())
         {
            String tableName = entry.getKey();
            RestoreTableRule restoreRule = entry.getValue();

            String constraint = null;
            if (tableName.equals("JCR_SITEM") || tableName.equals("JCR_MITEM"))
            {
               if (dialect != BackupTables.DB_DIALECT_MYSQL && dialect != BackupTables.DB_DIALECT_MYSQL_UTF8
                  && dialect != BackupTables.DB_DIALECT_SYBASE)
               {
                  // resolve constraint name depends on database
                  String constraintName;
                  if (dialect == BackupTables.DB_DIALECT_DB2 || dialect == BackupTables.DB_DIALECT_DB2V8)
                  {
                     constraintName = "JCR_FK_" + (restoreRule.getDstMultiDb() ? "M" : "S") + "ITEM_PAREN";
                  }
                  else
                  {
                     constraintName = "JCR_FK_" + (restoreRule.getDstMultiDb() ? "M" : "S") + "ITEM_PARENT";
                  }
                  constraint =
                     "CONSTRAINT " + constraintName + " FOREIGN KEY(PARENT_ID) REFERENCES " + tableName + "(ID)";

                  // drop constraint
                  st = jdbcConn.createStatement();
                  st.execute("ALTER TABLE " + tableName + " DROP CONSTRAINT " + constraintName);
               }
            }

            restore(storageDir, jdbcConn, tableName, restoreRule);

            if (constraint != null)
            {
               // add constraint
               st.execute("ALTER TABLE " + tableName + " ADD " + constraint);
            }
         }

         jdbcConn.commit();
      }
      catch (IOException e)
      {
         exc = new RestoreException(e);
         throw exc;
      }
      catch (SQLException e)
      {
         SQLException next = e.getNextException();
         String errorTrace = "";
         while (next != null)
         {
            errorTrace += next.getMessage() + "; ";
            next = next.getNextException();
         }

         Throwable cause = e.getCause();
         String msg = "SQL Exception: " + errorTrace + (cause != null ? " (Cause: " + cause.getMessage() + ")" : "");

         exc = new RestoreException(msg, e);
         throw exc;
      }
      catch (NamingException e)
      {
         exc = new RestoreException(e);
         throw exc;
      }
      finally
      {
         if (st != null)
         {
            try
            {
               st.close();
            }
            catch (SQLException e)
            {
               LOG.warn("Can't close statemnt", e);
            }
         }

         if (jdbcConn != null)
         {
            try
            {
               jdbcConn.rollback();
            }
            catch (SQLException e)
            {
               if (exc != null)
               {
                  throw new RestoreException(e);
               }
               else
               {
                  throw new RestoreException("Can't rollback connection", exc);
               }
            }
            finally
            {
               try
               {
                  jdbcConn.close();
               }
               catch (SQLException e)
               {
                  if (exc != null)
                  {
                     throw new RestoreException(e);
                  }
                  else
                  {
                     throw new RestoreException("Can't close connection", exc);
                  }
               }
            }
         }
      }
   }

   /**
    * Restore table.
    */
   private void restore(File storageDir, Connection jdbcConn, String tableName, RestoreTableRule restoreRule)
      throws IOException, SQLException
   {
      // Need privileges
      SecurityManager security = System.getSecurityManager();
      if (security != null)
      {
         security.checkPermission(Backupable.BACKUP_RESTORE_PERMISSION);
      }

      ObjectZipReaderImpl contentReader = null;
      ObjectZipReaderImpl contentLenReader = null;

      PreparedStatement insertNode = null;
      ResultSet tableMetaData = null;

      int dialect = DialectDetecter.detect(jdbcConn.getMetaData()).hashCode();

      // switch table name to lower case
      if (dialect == BackupTables.DB_DIALECT_PGSQL)
      {
         tableName = tableName.toLowerCase();
      }

      try
      {
         contentReader = new ObjectZipReaderImpl(PrivilegedFileHelper.zipInputStream(restoreRule.getContentFile()));
         contentReader.getNextEntry();

         contentLenReader =
            new ObjectZipReaderImpl(PrivilegedFileHelper.zipInputStream(restoreRule.getContentLenFile()));
         contentLenReader.getNextEntry();

         // get information about source table
         int sourceColumnCount = contentReader.readInt();

         List<Integer> columnType = new ArrayList<Integer>();
         List<String> columnName = new ArrayList<String>();

         for (int i = 0; i < sourceColumnCount; i++)
         {
            columnType.add(contentReader.readInt());
            columnName.add(contentReader.readString());
         }

         int targetColumnCount = sourceColumnCount;
         if (restoreRule.getDeleteColumnIndex() != null)
         {
            targetColumnCount--;
         }
         else if (restoreRule.getNewColumnIndex() != null)
         {
            targetColumnCount++;

            columnType.add(restoreRule.getNewColumnIndex(), restoreRule.getNewColumnType());

            String newColumnName =
               dialect == BackupTables.DB_DIALECT_PGSQL ? restoreRule.getNewColumnName().toLowerCase() : restoreRule
                  .getNewColumnName();
            columnName.add(restoreRule.getNewColumnIndex(), newColumnName);
         }

         // construct statement
         String names = "";
         String parameters = "";
         for (int i = 0; i < targetColumnCount; i++)
         {
            if (restoreRule.getSkipColumnIndex() != null && restoreRule.getSkipColumnIndex() == i)
            {
               continue;
            }
            names += columnName.get(i) + (i == targetColumnCount - 1 ? "" : ",");
            parameters += "?" + (i == targetColumnCount - 1 ? "" : ",");
         }
         insertNode =
            jdbcConn.prepareStatement("INSERT INTO " + tableName + " (" + names + ") VALUES(" + parameters + ")");

         // set data
         outer : while (true)
         {
            for (int i = 0, targetIndex = 0; i < columnType.size(); i++, targetIndex++)
            {
               InputStream stream;
               long len;

               if (restoreRule.getNewColumnIndex() != null && restoreRule.getNewColumnIndex() == i)
               {
                  stream =
                     new ByteArrayInputStream(restoreRule.getDstContainerName().getBytes(Constants.DEFAULT_ENCODING));
                  len = ((ByteArrayInputStream)stream).available();
               }
               else
               {
                  try
                  {
                     len = contentLenReader.readLong();
                  }
                  catch (EOFException e)
                  {
                     if (i == 0)
                     {
                        // content length file is empty check content file
                        try
                        {
                           contentReader.readByte();
                        }
                        catch (EOFException e1)
                        {
                           break outer;
                        }
                     }

                     throw new IOException("Content length file is empty but content still present", e);
                  }
                  stream = len == -1 ? null : spoolInputStream(contentReader, len);
               }

               if (restoreRule.getSkipColumnIndex() != null && restoreRule.getSkipColumnIndex() == i)
               {
                  targetIndex--;
                  continue;
               }
               else if (restoreRule.getDeleteColumnIndex() != null && restoreRule.getDeleteColumnIndex() == i)
               {
                  targetIndex--;
                  continue;
               }

               // set 
               if (stream != null)
               {
                  if (restoreRule.getConvertColumnIndex() != null && restoreRule.getConvertColumnIndex().contains(i))
                  {
                     // convert column value
                     ByteArrayInputStream ba = (ByteArrayInputStream)stream;
                     byte[] readBuffer = new byte[ba.available()];
                     ba.read(readBuffer);

                     String currentValue = new String(readBuffer, Constants.DEFAULT_ENCODING);
                     if (currentValue.equals(Constants.ROOT_PARENT_UUID))
                     {
                        stream = new ByteArrayInputStream(Constants.ROOT_PARENT_UUID.getBytes());
                     }
                     else
                     {
                        if (restoreRule.getDstMultiDb())
                        {
                           if (!restoreRule.getSrcMultiDb())
                           {
                              stream =
                                 new ByteArrayInputStream(new String(readBuffer, Constants.DEFAULT_ENCODING).substring(
                                    restoreRule.getSrcContainerName().length()).getBytes());
                           }
                        }
                        else
                        {
                           if (restoreRule.getSrcMultiDb())
                           {
                              StringBuilder builder = new StringBuilder();
                              builder.append(restoreRule.getDstContainerName());
                              builder.append(currentValue);

                              stream = new ByteArrayInputStream(builder.toString().getBytes());
                           }
                           else
                           {
                              StringBuilder builder = new StringBuilder();
                              builder.append(restoreRule.getDstContainerName());
                              builder.append(new String(readBuffer, Constants.DEFAULT_ENCODING).substring(restoreRule
                                 .getSrcContainerName().length()));

                              stream = new ByteArrayInputStream(builder.toString().getBytes());
                           }
                        }
                     }

                     len = ((ByteArrayInputStream)stream).available();
                  }

                  if (columnType.get(i) == Types.INTEGER || columnType.get(i) == Types.BIGINT
                     || columnType.get(i) == Types.SMALLINT || columnType.get(i) == Types.TINYINT)
                  {
                     ByteArrayInputStream ba = (ByteArrayInputStream)stream;
                     byte[] readBuffer = new byte[ba.available()];
                     ba.read(readBuffer);

                     String value = new String(readBuffer, Constants.DEFAULT_ENCODING);
                     insertNode.setLong(targetIndex + 1, Integer.parseInt(value));
                  }
                  else if (columnType.get(i) == Types.BIT)
                  {
                     ByteArrayInputStream ba = (ByteArrayInputStream)stream;
                     byte[] readBuffer = new byte[ba.available()];
                     ba.read(readBuffer);

                     String value = new String(readBuffer);
                     if (dialect == BackupTables.DB_DIALECT_PGSQL)
                     {
                        insertNode.setBoolean(targetIndex + 1, value.equals("t"));
                     }
                     else
                     {
                        insertNode.setBoolean(targetIndex + 1, value.equals("1"));
                     }
                  }
                  else if (columnType.get(i) == Types.BOOLEAN)
                  {
                     ByteArrayInputStream ba = (ByteArrayInputStream)stream;
                     byte[] readBuffer = new byte[ba.available()];
                     ba.read(readBuffer);

                     String value = new String(readBuffer);
                     insertNode.setBoolean(targetIndex + 1, value.equals("true"));
                  }
                  else if (columnType.get(i) == Types.VARBINARY || columnType.get(i) == Types.LONGVARBINARY
                     || columnType.get(i) == Types.BLOB || columnType.get(i) == Types.BINARY)
                  {
                     insertNode.setBinaryStream(targetIndex + 1, stream, (int)len);
                  }
                  else
                  {
                     byte[] readBuffer = new byte[(int)len];
                     stream.read(readBuffer);

                     insertNode.setString(targetIndex + 1, new String(readBuffer, Constants.DEFAULT_ENCODING));
                  }
               }
               else
               {
                  insertNode.setNull(targetIndex + 1, columnType.get(i));
               }
            }
            insertNode.addBatch();
         }

         insertNode.executeBatch();
      }
      finally
      {
         if (contentReader != null)
         {
            contentReader.close();
         }

         if (contentLenReader != null)
         {
            contentLenReader.close();
         }

         if (insertNode != null)
         {
            insertNode.close();
         }

         // delete all temporary files
         for (File file : spoolFileList)
         {
            if (!PrivilegedFileHelper.delete(file))
            {
               fileCleaner.addFile(file);
            }
         }

         if (tableMetaData != null)
         {
            tableMetaData.close();
         }
      }
   }

   /**
    * Spool input stream.
    */
   private InputStream spoolInputStream(ObjectReader in, long contentLen) throws IOException
   {
      byte[] buffer = new byte[0];
      byte[] tmpBuff;
      long readLen = 0;
      File sf = null;
      OutputStream sfout = null;

      try
      {
         while (true)
         {
            int needToRead = contentLen - readLen > 2048 ? 2048 : (int)(contentLen - readLen);
            tmpBuff = new byte[needToRead];

            if (needToRead == 0)
            {
               break;
            }

            in.readFully(tmpBuff);

            if (sfout != null)
            {
               sfout.write(tmpBuff);
            }
            else if (readLen + needToRead > maxBufferSize && fileCleaner != null)
            {
               sf = PrivilegedFileHelper.createTempFile("jcrvd", null, tempDir);
               sfout = PrivilegedFileHelper.fileOutputStream(sf);

               sfout.write(buffer);
               sfout.write(tmpBuff);
               buffer = null;
            }
            else
            {
               // reallocate new buffer and spool old buffer contents
               byte[] newBuffer = new byte[(int)(readLen + needToRead)];
               System.arraycopy(buffer, 0, newBuffer, 0, (int)readLen);
               System.arraycopy(tmpBuff, 0, newBuffer, (int)readLen, needToRead);
               buffer = newBuffer;
            }

            readLen += needToRead;
         }

         if (buffer != null)
         {
            return new ByteArrayInputStream(buffer);
         }
         else
         {
            return PrivilegedFileHelper.fileInputStream(sf);
         }
      }
      finally
      {
         if (sfout != null)
         {
            sfout.close();
         }

         if (sf != null)
         {
            spoolFileList.add(sf);
         }
      }
   }
}


