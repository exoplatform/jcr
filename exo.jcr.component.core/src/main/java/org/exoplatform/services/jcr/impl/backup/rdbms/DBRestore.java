/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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

package org.exoplatform.services.jcr.impl.backup.rdbms;

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.commons.utils.PrivilegedSystemHelper;
import org.exoplatform.services.database.utils.DialectConstants;
import org.exoplatform.services.database.utils.DialectDetecter;
import org.exoplatform.services.database.utils.JDBCUtils;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.security.JCRRuntimePermissions;
import org.exoplatform.services.jcr.dataflow.serialization.ObjectReader;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.backup.BackupException;
import org.exoplatform.services.jcr.impl.backup.DataRestore;
import org.exoplatform.services.jcr.impl.clean.rdbms.DBCleanException;
import org.exoplatform.services.jcr.impl.clean.rdbms.DBCleanerTool;
import org.exoplatform.services.jcr.impl.dataflow.serialization.ZipObjectReader;
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializerHelper;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.naming.NamingException;

/**
 * Created by The eXo Platform SAS.
 *
 * Date: 22 01 2011
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: DBRestore.java 34360 2010-11-11 11:11:11Z tolusha $
 */
public class DBRestore implements DataRestore
{
   /**
    * The maximum possible batch size.
    */
   private final int MAXIMUM_BATCH_SIZE = 1000;

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
   private final File tempDir = new File(PrivilegedSystemHelper.getProperty("java.io.tmpdir"));

   /**
    * Maximum buffer size.
    */
   private final int maxBufferSize;

   /**
    * Logger.
    */
   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.DBRestor");

   /**
    * Connection to database.
    */
   protected final Connection jdbcConn;

   /**
    * Directory for dumps.
    */
   private final File storageDir;

   /**
    * Restore table rules.
    */
   protected final Map<String, TableTransformationRule> tables;

   /**
    * Database cleaner.
    */
   private final DBCleanerTool dbCleaner;

   /**
    * Database dialect.
    */
   protected final String dialect;

   protected final boolean useSequence;

   protected final String itemTableName;

   /**
    * Contains object names which executed queries.   
    */
   protected List<String> successfulExecuted;

   protected boolean dbCleanerInAutoCommit;

   /**
    * Constructor DBRestore.
    * 
    * @throws NamingException 
    * @throws SQLException 
    * @throws RepositoryConfigurationException 
    */
   public DBRestore(File storageDir, Connection jdbcConn, Map<String, TableTransformationRule> tables,
      WorkspaceEntry wsConfig, FileCleaner fileCleaner, DBCleanerTool dbCleaner) throws NamingException,
      SQLException, RepositoryConfigurationException
   {
      this.jdbcConn = jdbcConn;
      this.fileCleaner = fileCleaner;
      this.maxBufferSize =
         wsConfig.getContainer().getParameterInteger(JDBCWorkspaceDataContainer.MAXBUFFERSIZE_PROP,
            JDBCWorkspaceDataContainer.DEF_MAXBUFFERSIZE);

      this.storageDir = storageDir;
      this.tables = tables;
      this.dbCleaner = dbCleaner;
      this.dialect = DialectDetecter.detect(jdbcConn.getMetaData());
      this.dbCleanerInAutoCommit = dialect.startsWith(DialectConstants.DB_DIALECT_SYBASE);
      this.itemTableName = DBInitializerHelper.getItemTableName(wsConfig);
      this.useSequence= DBInitializerHelper.useSequenceForOrderNumber(wsConfig,this.dialect);
   }

   /**
    * {@inheritDoc}
    */
   public void clean() throws BackupException
   {
      LOG.info("Start to clean JCR tables");
      try
      {
         dbCleaner.clean();
      }
      catch (DBCleanException e)
      {
         throw new BackupException(e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void restore() throws BackupException
   {
      try
      {
         for (Entry<String, TableTransformationRule> entry : tables.entrySet())
         {
            String tableName = entry.getKey();
            TableTransformationRule restoreRule = entry.getValue();

            restoreTable(storageDir, jdbcConn, tableName, restoreRule);
         }
      }
      catch (IOException e)
      {
         throw new BackupException(e);
      }
      catch (SQLException e)
      {
         throw new BackupException("SQL Exception: " + JDBCUtils.getFullMessage(e), e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void commit() throws BackupException
   {
      try
      {
         dbCleaner.commit();

         jdbcConn.commit();
      }
      catch (SQLException e)
      {
         throw new BackupException(e);
      }
      catch (DBCleanException e)
      {
         throw new BackupException(e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void rollback() throws BackupException
   {
      try
      {
         jdbcConn.rollback();

         dbCleaner.rollback();

         jdbcConn.commit();
      }
      catch (SQLException e)
      {
         throw new BackupException(e);
      }
      catch (DBCleanException e)
      {
         throw new BackupException(e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void close() throws BackupException
   {
      try
      {
         // in case for shared connection
         if (!jdbcConn.isClosed())
         {
            jdbcConn.close();
         }
      }
      catch (SQLException e)
      {
         throw new BackupException(e);
      }
   }

   /**
    * Restore table.
    */
   private void restoreTable(File storageDir, Connection jdbcConn, String tableName, TableTransformationRule restoreRule)
      throws IOException, SQLException
   {
      // Need privileges
      SecurityManager security = System.getSecurityManager();
      if (security != null)
      {
         security.checkPermission(JCRRuntimePermissions.MANAGE_REPOSITORY_PERMISSION);
      }

      ZipObjectReader contentReader = null;
      ZipObjectReader contentLenReader = null;

      PreparedStatement insertNode = null;
      ResultSet tableMetaData = null;
      Statement stmt = null;

      // switch table name to lower case
      if (dialect.startsWith(DBConstants.DB_DIALECT_PGSQL))
      {
         tableName = tableName.toLowerCase();
      }

      try
      {
         File contentFile = new File(storageDir, restoreRule.getSrcTableName() + DBBackup.CONTENT_FILE_SUFFIX);

         // check old style backup format, when for every table was dedicated zip file 
         if (PrivilegedFileHelper.exists(contentFile))
         {
            contentReader = new ZipObjectReader(PrivilegedFileHelper.zipInputStream(contentFile));
            contentReader.getNextEntry();

            File contentLenFile =
               new File(storageDir, restoreRule.getSrcTableName() + DBBackup.CONTENT_LEN_FILE_SUFFIX);

            contentLenReader = new ZipObjectReader(PrivilegedFileHelper.zipInputStream(contentLenFile));
            contentLenReader.getNextEntry();
         }
         else
         {
            contentFile = new File(storageDir, DBBackup.CONTENT_ZIP_FILE);
            contentReader = new ZipObjectReader(PrivilegedFileHelper.zipInputStream(contentFile));

            while (!contentReader.getNextEntry().getName().equals(restoreRule.getSrcTableName()));

            File contentLenFile = new File(storageDir, DBBackup.CONTENT_LEN_ZIP_FILE);
            contentLenReader = new ZipObjectReader(PrivilegedFileHelper.zipInputStream(contentLenFile));

            while (!contentLenReader.getNextEntry().getName().equals(restoreRule.getSrcTableName()));
         }

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
         if (restoreRule.getNewColumnIndex() != null)
         {
            targetColumnCount++;

            columnType.add(restoreRule.getNewColumnIndex(), restoreRule.getNewColumnType());

            String newColumnName =
               dialect.startsWith(DBConstants.DB_DIALECT_PGSQL) ? restoreRule.getNewColumnName().toLowerCase()
                  : restoreRule.getNewColumnName();
            columnName.add(restoreRule.getNewColumnIndex(), newColumnName);
         }

         // construct statement
         StringBuilder names = new StringBuilder();
         StringBuilder parameters = new StringBuilder();
         for (int i = 0; i < targetColumnCount; i++)
         {
            if (restoreRule.getSkipColumnIndex() != null && restoreRule.getSkipColumnIndex() == i)
            {
               continue;
            }
            names.append(columnName.get(i)).append(i == targetColumnCount - 1 ? "" : ",");
            parameters.append("?").append(i == targetColumnCount - 1 ? "" : ",");
         }

         int batchSize = 0;
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
                     if (dialect.startsWith(DBConstants.DB_DIALECT_PGSQL))
                     {
                        insertNode.setBoolean(targetIndex + 1, value.equalsIgnoreCase("t"));
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
                     insertNode.setBoolean(targetIndex + 1, value.equalsIgnoreCase("true"));
                  }
                  else if (columnType.get(i) == Types.VARBINARY || columnType.get(i) == Types.LONGVARBINARY
                     || columnType.get(i) == Types.BLOB || columnType.get(i) == Types.BINARY
                     || columnType.get(i) == Types.OTHER)
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

            // add statement to batch
            insertNode.addBatch();

            if (++batchSize == MAXIMUM_BATCH_SIZE)
            {
               insertNode.executeBatch();

               commitBatch();

               batchSize = 0;
            }
         }

         if (batchSize != 0)
         {
            insertNode.executeBatch();

            commitBatch();
         }
         if (useSequence)
         {
            batchSize = 0;
            String update = "DROP SEQUENCE " + tableName + "_seq";
            stmt = jdbcConn.createStatement();
            if ((dialect.startsWith(DBConstants.DB_DIALECT_MYSQL) || dialect.startsWith(DBConstants.DB_DIALECT_MSSQL)
               || dialect.startsWith(DBConstants.DB_DIALECT_SYBASE)) && tableName.equalsIgnoreCase(this.itemTableName))
            {
               boolean exist = checkEntry(jdbcConn, tableName + "_SEQ");
               if (exist)
               {
                  insertNode =
                     jdbcConn.prepareStatement("UPDATE " + tableName + "_SEQ  SET  nextVal=?  where name='LAST_N_ORDER_NUM'");
               }
               else
               {
                  insertNode =
                     jdbcConn.prepareStatement("INSERT INTO " + tableName + "_SEQ  (name, nextVal) VALUES ('LAST_N_ORDER_NUM', ?)");
               }
               insertNode.setInt(1, getStartValue(jdbcConn, tableName));
               insertNode.executeUpdate();
               batchSize++;

            }
            else if ((dialect.startsWith(DBConstants.DB_DIALECT_PGSQL) || dialect.startsWith(DBConstants.DB_DIALECT_DB2) || dialect.startsWith(DBConstants.DB_DIALECT_HSQLDB)
            ) && (tableName.equalsIgnoreCase(this.itemTableName)))

            {
               stmt.execute(update);
               update = "CREATE SEQUENCE " + tableName + "_seq  INCREMENT BY 1 MINVALUE -1 NO MAXVALUE  NO CYCLE START WITH " + (getStartValue(jdbcConn, tableName) + 1);
               stmt.execute(update);
               batchSize++;
            }
            else if (dialect.startsWith(DBConstants.DB_DIALECT_H2) && (tableName.equalsIgnoreCase(this.itemTableName)))
            {
               stmt.execute(update);
               update = "CREATE SEQUENCE " + tableName + "_seq  INCREMENT BY 1 START WITH " + (getStartValue(jdbcConn, tableName) + 1);
               stmt.execute(update);
               batchSize++;
            }
            else if (dialect.startsWith(DBConstants.DB_DIALECT_ORACLE) && tableName.equalsIgnoreCase(this.itemTableName))
            {
               stmt.execute(update);
               update = "CREATE SEQUENCE " + tableName + "_seq  INCREMENT BY 1 MINVALUE -1 NOMAXVALUE NOCACHE NOCYCLE START WITH " + (getStartValue(jdbcConn, tableName) + 1);
               stmt.execute(update);
               batchSize++;

            }
            if (batchSize != 0)
            {

               commitBatch();
            }
         }
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

         if (stmt != null)
         {
            stmt.close();
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
    * Committing changes from batch.
    */
   protected void commitBatch() throws SQLException
   {
      // commit every batch for sybase
      if (dialect.startsWith(DBConstants.DB_DIALECT_SYBASE))
      {
         jdbcConn.commit();
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

   /**
    * Init Start value for sequence.
    */
   private int getStartValue(Connection con, String table)
   {

      Statement stmt = null;
      ResultSet trs = null;
      try
      {
         String query = "select max(N_ORDER_NUM) from " + table;
         stmt = con.createStatement();
         trs = stmt.executeQuery(query);
         if (trs.next() && trs.getInt(1) >= 0)
         {
            return trs.getInt(1);
         }
         else
         {
            return -1;
         }

      }
      catch (SQLException e)
      {
         if (LOG.isDebugEnabled())
         {
            LOG.debug("SQLException occurred while calculate the sequence start value", e);
         }
         return -1;
      }
      finally
      {
         JDBCUtils.freeResources(trs, stmt, null);
      }
   }

   /**
    * Check if LAST_N_ORDER_NUM row exists.
    */
   private boolean checkEntry(Connection con, String table)
   {

      Statement stmt = null;
      ResultSet trs = null;
      try
      {
         String query = "select count(*) from " + table +"  where name ='LAST_N_ORDER_NUM'";
         stmt = con.createStatement();
         trs = stmt.executeQuery(query);
         if (trs.next() && trs.getInt(1) > 0)
         {
            return true;
         }
      }
      catch (SQLException e)
      {
         if (LOG.isDebugEnabled())
         {
            LOG.debug("SQLException occurred while check the table " + table +"  entry ", e);
         }
      }
      finally
      {
         JDBCUtils.freeResources(trs, stmt, null);
      }
      return false;
   }
}


