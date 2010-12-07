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
package org.exoplatform.services.jcr.impl.core;

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.access.AccessManager;
import org.exoplatform.services.jcr.config.QueryHandlerParams;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.ValueStorageEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.dataflow.serialization.ObjectReader;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeManagerImpl;
import org.exoplatform.services.jcr.impl.core.query.SystemSearchManager;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.exoplatform.services.jcr.impl.dataflow.persistent.CacheableWorkspaceDataManager;
import org.exoplatform.services.jcr.impl.dataflow.serialization.ObjectReaderImpl;
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.services.jcr.impl.storage.jdbc.DialectDetecter;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.jcr.impl.storage.value.fs.FileValueStorage;
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
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * @author <a href="mailto:anatoliy.bazko@gmail.com">Anatoliy Bazko</a>
 * @version $Id: RdbmsWorkspaceInitializer.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class RdbmsWorkspaceInitializer extends BackupWorkspaceInitializer
{
   /**
    * Logger.
    */
   protected static final Log log = ExoLogger.getLogger("exo.jcr.component.core.RdbmsWorkspaceInitializer");

   /**
    * Index directory in full backup storage.
    */
   public static final String INDEX_DIR = "index";

   /**
    * System index directory in full backup storage.
    */
   public static final String SYSTEM_INDEX_DIR = INDEX_DIR + "_" + SystemSearchManager.INDEX_DIR_SUFFIX;

   /**
    * Value storage directory in full backup storage.
    */
   public static final String VALUE_STORAGE_DIR = "values";

   /**
    * Suffix for content file.
    */
   public static final String CONTENT_FILE_SUFFIX = ".dump";

   /**
    * Suffix for content length file.
    */
   public static final String CONTENT_LEN_FILE_SUFFIX = ".len";

   /**
    * Content is absent.
    */
   public static final byte NULL_LEN = 0;

   /**
    * Content length value has byte type.
    */
   public static final byte BYTE_LEN = 1;

   /**
    * Content length value has integer type.
    */
   public static final byte INT_LEN = 2;

   /**
    * Content length value has long type.
    */
   public static final byte LONG_LEN = 3;

   /**
    * List of temporary files.
    */
   protected List<File> spoolFileList = new ArrayList<File>();

   /**
    * Constructor RdbmsWorkspaceInitializer.
    */
   public RdbmsWorkspaceInitializer(WorkspaceEntry config, RepositoryEntry repConfig,
      CacheableWorkspaceDataManager dataManager, NamespaceRegistryImpl namespaceRegistry,
      LocationFactory locationFactory, NodeTypeManagerImpl nodeTypeManager, ValueFactoryImpl valueFactory,
      AccessManager accessManager) throws RepositoryConfigurationException, PathNotFoundException, RepositoryException
   {
      super(config, repConfig, dataManager, namespaceRegistry, locationFactory, nodeTypeManager, valueFactory,
         accessManager);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public NodeData initWorkspace() throws RepositoryException
   {
      if (isWorkspaceInitialized())
      {
         return (NodeData)dataManager.getItemData(Constants.ROOT_UUID);
      }

      Connection jdbcConn = null;
      Integer transactionIsolation = null;
      Statement st = null;
      try
      {
         long start = System.currentTimeMillis();

         String dsName = workspaceEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME);
         if (dsName == null)
         {
            throw new RepositoryConfigurationException("Data source name not found in workspace configuration "
               + workspaceName);
         }

         String multiDb = workspaceEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.MULTIDB);
         if (multiDb == null)
         {
            throw new RepositoryConfigurationException(JDBCWorkspaceDataContainer.MULTIDB
               + " parameter not found in workspace " + workspaceName + " configuration");
         }

         final DataSource ds = (DataSource)new InitialContext().lookup(dsName);
         if (ds == null)
         {
            throw new NameNotFoundException("Data source " + dsName + " not found");
         }

         jdbcConn = SecurityHelper.doPriviledgedSQLExceptionAction(new PrivilegedExceptionAction<Connection>()
         {
            public Connection run() throws Exception
            {
               return ds.getConnection();

               }
         });

         String dbDialect = DialectDetecter.detect(jdbcConn.getMetaData());

         transactionIsolation = jdbcConn.getTransactionIsolation();
         jdbcConn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

         jdbcConn.setAutoCommit(false);

         // restore JCR data
         String[] tables;
         if (Boolean.parseBoolean(multiDb))
         {
            tables = new String[]{"JCR_MITEM", "JCR_MVALUE", "JCR_MREF"};
         }
         else
         {
            tables = new String[]{"JCR_SITEM", "JCR_SVALUE", "JCR_SREF"};
         }

         // resolve constraint name depends on database
         String constraintName;
         if (dbDialect.equals(DBConstants.DB_DIALECT_DB2) || dbDialect.equals(DBConstants.DB_DIALECT_DB2V8))
         {
            constraintName = "JCR_FK_" + (Boolean.parseBoolean(multiDb) ? "M" : "S") + "ITEM_PAREN";
         }
         else
         {
            constraintName = "JCR_FK_" + (Boolean.parseBoolean(multiDb) ? "M" : "S") + "ITEM_PARENT";
         }
         String constraint =
            "CONSTRAINT " + constraintName + " FOREIGN KEY(PARENT_ID) REFERENCES " + tables[0] + "(ID)";

         for (String table : tables)
         {
            if (table.equals("JCR_MITEM") || table.equals("JCR_SITEM"))
            {
               st = jdbcConn.createStatement();
               st.execute("ALTER TABLE " + table + " DROP CONSTRAINT " + constraintName);
               jdbcConn.commit();

               restoreTable(jdbcConn, table);

               st = jdbcConn.createStatement();
               st.execute("ALTER TABLE " + table + " ADD CONSTRAINT " + constraint);
               jdbcConn.commit();
            }
            else
            {
               restoreTable(jdbcConn, table);
            }
         }

         // restore LOCK data
         tables =
            new String[]{"JCR_LOCK_" + workspaceName.toUpperCase(), "JCR_LOCK_" + workspaceName.toUpperCase() + "_D"};

         for (String table : tables)
         {
            if (PrivilegedFileHelper.exists(new File(restorePath, table + CONTENT_FILE_SUFFIX)))
            {
               restoreTable(jdbcConn, table);
            }
         }

         restoreValueStorage();
         restoreIndex();

         // restore from incremental backup
         incrementalRead();

         final NodeData root = (NodeData)dataManager.getItemData(Constants.ROOT_UUID);

         log.info("Workspace [" + workspaceName + "] restored from storage " + restorePath + " in "
            + (System.currentTimeMillis() - start) * 1d / 1000 + "sec");

         return root;

      }
      catch (RepositoryConfigurationException e)
      {
         throw new RepositoryException(e);
      }
      catch (NamingException e)
      {
         throw new RepositoryException(e);
      }
      catch (IOException e)
      {
         throw new RepositoryException(e);
      }
      catch (SQLException e)
      {
         if (jdbcConn != null)
         {
            try
            {
               jdbcConn.rollback();
            }
            catch (SQLException e1)
            {
               log.error("Rollback error", e1);
            }
         }

         SQLException next = e.getNextException();
         String errorTrace = "";
         while (next != null)
         {
            errorTrace += next.getMessage() + "; ";
            next = next.getNextException();
         }

         Throwable cause = e.getCause();
         String msg = "SQL Exception: " + errorTrace + (cause != null ? " (Cause: " + cause.getMessage() + ")" : "");

         throw new RepositoryException(msg, e);
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
               throw new RepositoryException(e);
            }
         }

         if (jdbcConn != null)
         {
            try
            {
               if (transactionIsolation != null)
               {
                  jdbcConn.setTransactionIsolation(transactionIsolation);
               }

               jdbcConn.close();
            }
            catch (SQLException e)
            {
               throw new RepositoryException(e);
            }
         }
      }
   }

   /**
    * Restore index from backup.
    */
   protected void restoreIndex() throws RepositoryConfigurationException, IOException
   {
      File indexDir = new File(restorePath, INDEX_DIR);
      File systemIndexDir = new File(restorePath, SYSTEM_INDEX_DIR);

      if (workspaceEntry.getQueryHandler() != null)
      {
         if (!PrivilegedFileHelper.exists(indexDir))
         {
            throw new RepositoryConfigurationException("Can't restore index. Directory " + indexDir.getName()
               + " doesn't exists");
         }
         else
         {
            File destDir =
               new File(workspaceEntry.getQueryHandler().getParameterValue(QueryHandlerParams.PARAM_INDEX_DIR));
            copyDirectory(indexDir, destDir);
         }

         // try to restore system index
         if (repositoryEntry.getSystemWorkspaceName().equals(workspaceName))
         {
            if (!PrivilegedFileHelper.exists(systemIndexDir))
            {
               throw new RepositoryConfigurationException("Can't restore system index. Directory "
                  + systemIndexDir.getName() + " doesn't exists");
            }
            else
            {
               File destDir =
                  new File(workspaceEntry.getQueryHandler().getParameterValue(QueryHandlerParams.PARAM_INDEX_DIR) + "_"
                     + SystemSearchManager.INDEX_DIR_SUFFIX);
               copyDirectory(systemIndexDir, destDir);
            }
         }
         else if (PrivilegedFileHelper.exists(systemIndexDir))
         {
            throw new RepositoryConfigurationException("Workspace [" + workspaceName
               + "] is not a system in repository configuration but system index backup files exist");
         }
      }
      else
      {
         if (PrivilegedFileHelper.exists(indexDir) || PrivilegedFileHelper.exists(systemIndexDir))
         {
            throw new RepositoryConfigurationException("Query handler didn't configure in workspace [" + workspaceName
               + "] configuration but index backup files exist");
         }
      }
   }

   /**
    * Restoring value storage from backup.
    */
   protected void restoreValueStorage() throws RepositoryConfigurationException, IOException
   {
      File backupValueStorageDir = new File(restorePath, VALUE_STORAGE_DIR);
      if (workspaceEntry.getContainer().getValueStorages() != null)
      {
         List<ValueStorageEntry> valueStorages = workspaceEntry.getContainer().getValueStorages();
         String[] valueStoragesFiles = PrivilegedFileHelper.list(backupValueStorageDir);

         if ((valueStoragesFiles == null && valueStorages.size() != 0)
            || (valueStoragesFiles != null && valueStoragesFiles.length != valueStorages.size()))
         {
            throw new RepositoryConfigurationException("Workspace configuration [" + workspaceName
               + "] has a different amount of value storages than exist in backup");
         }

         for (ValueStorageEntry valueStorage : valueStorages)
         {
            File srcDir = new File(backupValueStorageDir, valueStorage.getId());
            if (!PrivilegedFileHelper.exists(srcDir))
            {
               throw new RepositoryConfigurationException("Can't restore value storage. Directory " + srcDir.getName()
                  + " doesn't exists");
            }
            else
            {
               File destDir = new File(valueStorage.getParameterValue(FileValueStorage.PATH));

               copyDirectory(srcDir, destDir);
            }
         }
      }
      else
      {
         if (PrivilegedFileHelper.exists(backupValueStorageDir))
         {
            throw new RepositoryConfigurationException("Value storage didn't configure in workspace [" + workspaceName
               + "] configuration but value storage backup files exist");
         }
      }
   }

   /**
    * Copy directory.
    * 
    * @param srcPath
    *          source path
    * @param dstPath
    *          destination path
    * @throws IOException
    *          if any exception occurred
    */
   private void copyDirectory(File srcPath, File dstPath) throws IOException
   {
      if (PrivilegedFileHelper.isDirectory(srcPath))
      {
         if (!PrivilegedFileHelper.exists(dstPath))
         {
            PrivilegedFileHelper.mkdirs(dstPath);
         }

         String files[] = PrivilegedFileHelper.list(srcPath);
         for (int i = 0; i < files.length; i++)
         {
            copyDirectory(new File(srcPath, files[i]), new File(dstPath, files[i]));
         }
      }
      else
      {
         InputStream in = null;
         OutputStream out = null;

         try
         {
            in = PrivilegedFileHelper.fileInputStream(srcPath);
            out = PrivilegedFileHelper.fileOutputStream(dstPath);

            // Transfer bytes from in to out
            byte[] buf = new byte[2048];

            int len;

            while ((len = in.read(buf)) > 0)
            {
               out.write(buf, 0, len);
            }
         }
         finally
         {
            if (in != null)
            {
               in.close();
            }

            if (out != null)
            {
               out.close();
            }
         }
      }
   }

   /**
    * Restore table.
    */
   protected void restoreTable(Connection jdbcConn, String tableName) throws IOException, SQLException
   {
      String insertNodeQuery = null;

      ObjectReader contentReader = null;
      ObjectReader contentLenReader = null;

      PreparedStatement insertNode = null;

      try
      {
         contentReader =
            new ObjectReaderImpl(PrivilegedFileHelper.fileInputStream(new File(restorePath, tableName
               + CONTENT_FILE_SUFFIX)));

         contentLenReader =
            new ObjectReaderImpl(PrivilegedFileHelper.fileInputStream(new File(restorePath, tableName
               + CONTENT_LEN_FILE_SUFFIX)));

         int columnCount = contentReader.readInt();
         int[] columnType = new int[columnCount];

         for (int i = 0; i < columnCount; i++)
         {
            columnType[i] = contentReader.readInt();
         }

         for (int i = 0; i < columnCount; i++)
         {
            if (i == 0)
            {
               insertNodeQuery = "INSERT INTO " + tableName + " VALUES(?";
            }
            else
            {
               insertNodeQuery += ",?";
            }

            if (i == columnCount - 1)
            {
               insertNodeQuery += ")";
            }
         }

         insertNode = jdbcConn.prepareStatement(insertNodeQuery);
         outer : while (true)
         {
            for (int i = 0; i < columnCount; i++)
            {
               long len;
               try
               {
                  len = readCompressedContentLen(contentLenReader);
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

               if (len != NULL_LEN)
               {
                  InputStream stream = spoolInputStream(contentReader, len);

                  if (columnType[i] == Types.INTEGER || columnType[i] == Types.BIGINT
                     || columnType[i] == Types.SMALLINT || columnType[i] == Types.TINYINT)
                  {
                     ByteArrayInputStream ba = (ByteArrayInputStream)stream;
                     byte[] readBuffer = new byte[ba.available()];
                     ba.read(readBuffer);

                     String value = new String(readBuffer, Constants.DEFAULT_ENCODING);
                     insertNode.setLong(i + 1, Integer.parseInt(value));
                  }
                  else if (columnType[i] == Types.BOOLEAN || columnType[i] == Types.BIT)
                  {
                     ByteArrayInputStream ba = (ByteArrayInputStream)stream;
                     byte[] readBuffer = new byte[ba.available()];
                     ba.read(readBuffer);

                     String value = new String(readBuffer);
                     insertNode.setBoolean(i + 1, value.equals("t"));
                  }
                  else
                  {
                     insertNode.setBinaryStream(i + 1, stream, (int)len);
                  }
               }
               else
               {
                  insertNode.setNull(i + 1, columnType[i]);
               }
            }
            insertNode.addBatch();
         }

         insertNode.executeBatch();
         jdbcConn.commit();
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
      }
   }

   /**
    * Write content length in output. 
    */
   private long readCompressedContentLen(ObjectReader in) throws IOException
   {
      byte lenType = in.readByte();

      if (lenType == NULL_LEN)
      {
         return lenType;
      }
      else if (lenType == BYTE_LEN)
      {
         return in.readByte();
      }
      else if (lenType == INT_LEN)
      {
         return in.readInt();
      }
      else
      {
         return in.readLong();
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
            else if (readLen + needToRead > maxBufferSize)
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
