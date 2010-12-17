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
package org.exoplatform.services.jcr.ext.backup.impl.rdbms;

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.AccessManager;
import org.exoplatform.services.jcr.config.LockManagerEntry;
import org.exoplatform.services.jcr.config.QueryHandlerParams;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.ValueStorageEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.dataflow.serialization.ObjectReader;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.ext.backup.impl.IndexCleanHelper;
import org.exoplatform.services.jcr.ext.backup.impl.ValueStorageCleanHelper;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.BackupWorkspaceInitializer;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.NamespaceRegistryImpl;
import org.exoplatform.services.jcr.impl.core.SessionRegistry;
import org.exoplatform.services.jcr.impl.core.lock.cacheable.AbstractCacheableLockManager;
import org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeManagerImpl;
import org.exoplatform.services.jcr.impl.core.query.SystemSearchManager;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.exoplatform.services.jcr.impl.dataflow.persistent.CacheableWorkspaceDataManager;
import org.exoplatform.services.jcr.impl.dataflow.serialization.ObjectZipReaderImpl;
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.services.jcr.impl.storage.jdbc.DialectDetecter;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.jcr.impl.storage.value.fs.FileValueStorage;
import org.exoplatform.services.jcr.impl.util.jdbc.cleaner.DBCleanerException;
import org.exoplatform.services.jcr.impl.util.jdbc.cleaner.DBCleanerService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipInputStream;

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
    * The repository service.
    */
   protected final RepositoryService repositoryService;

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
      AccessManager accessManager, RepositoryService repositoryService) throws RepositoryConfigurationException,
      PathNotFoundException, RepositoryException
   {
      super(config, repConfig, dataManager, namespaceRegistry, locationFactory, nodeTypeManager, valueFactory,
         accessManager);

      this.repositoryService = repositoryService;
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

      long start = System.currentTimeMillis();

      try
      {
         fullRdbmsRestore();
      }
      catch (Throwable e)
      {
         try
         {
            rollback();
         }
         catch (RepositoryConfigurationException e1)
         {
            throw new RepositoryException("Can't rollback changes", e);
         }
         catch (DBCleanerException e1)
         {
            throw new RepositoryException("Can't rollback changes", e);
         }
         catch (IOException e1)
         {
            throw new RepositoryException("Can't rollback changes", e);
         }
         throw new RepositoryException(e);
      }

      final NodeData root = (NodeData)dataManager.getItemData(Constants.ROOT_UUID);

      log.info("Workspace [" + workspaceName + "] restored from storage " + restorePath + " in "
         + (System.currentTimeMillis() - start) * 1d / 1000 + "sec");

      return root;
   }

   /**
    * Restore from full rdbms backup.
    */
   protected void fullRdbmsRestore() throws RepositoryException
   {
      Connection jdbcConn = null;
      Integer transactionIsolation = null;
      Statement st = null;
      try
      {
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
         boolean isMultiDb = Boolean.parseBoolean(multiDb);

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

         transactionIsolation = jdbcConn.getTransactionIsolation();
         jdbcConn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

         jdbcConn.setAutoCommit(false);

         RDBMSBackupInfoReader backupInfo = new RDBMSBackupInfoReader(restorePath);

         // restore JCR data
         Integer[] tableTypes =
            new Integer[]{RestoreTableHelper.ITEM_TABLE, RestoreTableHelper.VALUE_TABLE, RestoreTableHelper.REF_TABLE};

         for (Integer tableType : tableTypes)
         {
            RestoreTableHelper helper = new RestoreTableHelper(tableType, isMultiDb, backupInfo);

            if (tableType == RestoreTableHelper.ITEM_TABLE)
            {
               // resolve constraint name depends on database
               String constraintName;
               String dbDialect = DialectDetecter.detect(jdbcConn.getMetaData());

               if (dbDialect.equals(DBConstants.DB_DIALECT_DB2) || dbDialect.equals(DBConstants.DB_DIALECT_DB2V8))
               {
                  constraintName = "JCR_FK_" + (Boolean.parseBoolean(multiDb) ? "M" : "S") + "ITEM_PAREN";
               }
               else
               {
                  constraintName = "JCR_FK_" + (Boolean.parseBoolean(multiDb) ? "M" : "S") + "ITEM_PARENT";
               }
               String constraint =
                  "CONSTRAINT " + constraintName + " FOREIGN KEY(PARENT_ID) REFERENCES " + helper.getTableName()
                     + "(ID)";

               // drop constraint
               st = jdbcConn.createStatement();
               st.execute("ALTER TABLE " + helper.getTableName() + " DROP CONSTRAINT " + constraintName);
               jdbcConn.commit();

               restoreTable(jdbcConn, helper);

               // add constraint
               st = jdbcConn.createStatement();
               st.execute("ALTER TABLE " + helper.getTableName() + " ADD " + constraint);
               jdbcConn.commit();
            }
            else
            {
               if (PrivilegedFileHelper.exists(helper.getContentFile()))
               {
                  restoreTable(jdbcConn, helper);
               }
               else
               {
                  throw new IOException("File " + PrivilegedFileHelper.getCanonicalPath(helper.getContentFile())
                     + " not found");
               }
            }
         }

         // restore Lock data
         LockManagerEntry lockEntry = workspaceEntry.getLockManager();
         if (lockEntry != null)
         {
            List<String> existedLockTablesNames = AbstractCacheableLockManager.getLockTableNames(lockEntry);
            if (existedLockTablesNames.size() != backupInfo.getLockTableNames().size())
            {
               throw new RepositoryException("The amount of existed lock tables differs from backup");
            }

            for (int i = 0; i < backupInfo.getLockTableNames().size(); i++)
            {
               RestoreTableHelper helper = new RestoreTableHelper(RestoreTableHelper.LOCK_TABLE, isMultiDb, backupInfo);

               helper.setContentFile(new File(restorePath, backupInfo.getLockTableNames().get(i)
                  + FullBackupJob.CONTENT_FILE_SUFFIX));
               helper.setContentLenFile(new File(restorePath, backupInfo.getLockTableNames().get(i)
                  + FullBackupJob.CONTENT_LEN_FILE_SUFFIX));
               helper.setTableName(existedLockTablesNames.get(i));

               if (PrivilegedFileHelper.exists(helper.contentFile))
               {
                  restoreTable(jdbcConn, helper);
               }
               else
               {
                  throw new IOException("File " + PrivilegedFileHelper.getCanonicalPath(helper.contentFile)
                     + " not found");
               }
            }
         }
         else if (backupInfo.getLockTableNames().size() != 0)
         {
            throw new RepositoryException("There are no lock tables for new workspace configuration [" + workspaceName
               + "] but backup lock data exist");
         }

         // restore value storage and index
         restoreValueStorage();
         restoreIndex();
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
      File indexDir = new File(restorePath, FullBackupJob.INDEX_DIR);
      File systemIndexDir = new File(restorePath, FullBackupJob.SYSTEM_INDEX_DIR);

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
    * Rollback changes due to errors.
    * 
    * @throws RepositoryConfigurationException 
    * @throws RepositoryException 
    * @throws DBCleanerException 
    * @throws IOException 
    */
   protected void rollback() throws RepositoryException, RepositoryConfigurationException, DBCleanerException,
      IOException
   {
      boolean isSystem =
         repositoryService.getRepository(repositoryEntry.getName()).getConfiguration().getSystemWorkspaceName()
            .equals(workspaceEntry.getName());

      //close all session
      forceCloseSession(repositoryEntry.getName(), workspaceEntry.getName());

      //clean database
      new DBCleanerService().cleanWorkspaceData(workspaceEntry);

      //clean index
      new IndexCleanHelper().removeWorkspaceIndex(workspaceEntry, isSystem);

      //clean value storage
      new ValueStorageCleanHelper().removeWorkspaceValueStorage(workspaceEntry);
   }

   /**
    * Close sessions on specific workspace.
    * 
    * @param repositoryName
    *          repository name
    * @param workspaceName
    *          workspace name
    * @return int return the how many sessions was closed
    * @throws RepositoryConfigurationException
    *           will be generate RepositoryConfigurationException
    * @throws RepositoryException
    *           will be generate RepositoryException
    */
   private int forceCloseSession(String repositoryName, String workspaceName) throws RepositoryException,
      RepositoryConfigurationException
   {
      ManageableRepository mr = repositoryService.getRepository(repositoryName);
      WorkspaceContainerFacade wc = mr.getWorkspaceContainer(workspaceName);

      SessionRegistry sessionRegistry = (SessionRegistry)wc.getComponent(SessionRegistry.class);

      return sessionRegistry.closeSessions(workspaceName);
   }

   /**
    * Restoring value storage from backup.
    */
   protected void restoreValueStorage() throws RepositoryConfigurationException, IOException
   {
      File backupValueStorageDir = new File(restorePath, FullBackupJob.VALUE_STORAGE_DIR);
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
         ZipInputStream in = null;
         OutputStream out = null;

         try
         {
            in = PrivilegedFileHelper.zipInputStream(srcPath);
            in.getNextEntry();
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
   protected void restoreTable(Connection jdbcConn, RestoreTableHelper helper)
      throws IOException, SQLException
   {
      String insertNodeQuery = null;

      ObjectZipReaderImpl contentReader = null;
      ObjectZipReaderImpl contentLenReader = null;

      PreparedStatement insertNode = null;
      ResultSet tableMetaData = null;

      int dialect = DialectDetecter.detect(jdbcConn.getMetaData()).hashCode();

      try
      {
         contentReader = new ObjectZipReaderImpl(PrivilegedFileHelper.zipInputStream(helper.getContentFile()));
         contentReader.getNextEntry();

         contentLenReader = new ObjectZipReaderImpl(PrivilegedFileHelper.zipInputStream(helper.getContentLenFile()));
         contentLenReader.getNextEntry();

         // get information about backup table
         int sourceColumnCount = contentReader.readInt();
         
         List<Integer> columnType = new ArrayList<Integer>();
         List<String> columnName = new ArrayList<String>();
         
         for (int i = 0; i < sourceColumnCount; i++)
         {
            columnType.add(contentReader.readInt());
            columnName.add(contentReader.readString());
         }

         // collect information about target table 
         List<Integer> newColumnType = new ArrayList<Integer>();
         List<String> newColumnName = new ArrayList<String>();

         tableMetaData = jdbcConn.getMetaData().getColumns(null, null, helper.tableName, "%");
         while (tableMetaData.next())
         {
            newColumnName.add(tableMetaData.getString("COLUMN_NAME"));
            newColumnType.add(tableMetaData.getInt("DATA_TYPE"));
         }

         // construct query
         int targetColumnCount = sourceColumnCount;
         if (helper.getDeleteColumnIndex() != null)
         {
            targetColumnCount--;
         }
         else if (helper.getNewColumnIndex() != null)
         {
            targetColumnCount++;
            columnType.add(helper.getNewColumnIndex(), newColumnType.get((helper.getNewColumnIndex())));
         }

         for (int i = 0; i < targetColumnCount; i++)
         {
            if (i == 0)
            {
               insertNodeQuery = "INSERT INTO " + helper.getTableName() + " VALUES(?";
            }
            else
            {
               insertNodeQuery += ",?";
            }

            if (i == targetColumnCount - 1)
            {
               insertNodeQuery += ")";
            }
         }
         insertNode = jdbcConn.prepareStatement(insertNodeQuery);

         // set data
         outer : while (true)
         {
            for (int i = 0, targetIndex = 0; i < columnType.size(); i++, targetIndex++)
            {
               InputStream stream;
               long len;

               if (helper.getNewColumnIndex() != null && helper.getNewColumnIndex() == i)
               {
                  stream = new ByteArrayInputStream(workspaceName.getBytes(Constants.DEFAULT_ENCODING));
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

               if (helper.getSkipColumnIndex() != null && helper.getSkipColumnIndex() == i)
               {
                  continue;
               }
               else if (helper.getDeleteColumnIndex() != null && helper.getDeleteColumnIndex() == i)
               {
                  targetIndex--;
                  continue;
               }

               // set 
               if (stream != null)
               {
                  if (helper.getConvertColumnIndexes().contains(i))
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
                        if (helper.isMultiDb)
                        {
                           if (!helper.isBackupMutliDb())
                           {
                              stream =
                                 new ByteArrayInputStream(new String(readBuffer, Constants.DEFAULT_ENCODING).substring(
                                    helper.getBackupWorkspaceName().length()).getBytes());
                           }
                        }
                        else
                        {
                           if (helper.isBackupMutliDb())
                           {
                              StringBuilder builder = new StringBuilder();
                              builder.append(workspaceName);
                              builder.append(currentValue);

                              stream = new ByteArrayInputStream(builder.toString().getBytes());
                           }
                           else
                           {
                              StringBuilder builder = new StringBuilder();
                              builder.append(workspaceName);
                              builder.append(new String(readBuffer, Constants.DEFAULT_ENCODING).substring(helper
                                 .getBackupWorkspaceName().length()));

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
                     insertNode.setBoolean(targetIndex + 1, value.equals("t"));
                  }
                  else if (columnType.get(i) == Types.BOOLEAN)
                  {
                     ByteArrayInputStream ba = (ByteArrayInputStream)stream;
                     byte[] readBuffer = new byte[ba.available()];
                     ba.read(readBuffer);

                     String value = new String(readBuffer);
                     insertNode.setBoolean(targetIndex + 1, value.equals("true"));
                  }
                  else
                  {
                     if (dialect == FullBackupJob.DB_DIALECT_HSQLDB)
                     {
                        if (columnType.get(i) == Types.VARBINARY)
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
                        insertNode.setBinaryStream(targetIndex + 1, stream, (int)len);
                     }
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

   /**
    * Class which helps to restore data. 
    */
   protected class RestoreTableHelper
   {
      public static final int ITEM_TABLE = 0;

      public static final int VALUE_TABLE = 1;

      public static final int REF_TABLE = 2;

      public static final int LOCK_TABLE = 3;

      private String tableName;

      private File contentFile;

      private File contentLenFile;

      private Integer deleteColumnIndex = null;

      private Integer skipColumnIndex = null;

      private Integer newColumnIndex = null;

      private Set<Integer> convertColumnIndex = new HashSet<Integer>();

      private final boolean isMultiDb;

      private final RDBMSBackupInfoReader backupInfo;

      public RestoreTableHelper(int tableType, boolean isMultiDb, RDBMSBackupInfoReader backupInfo)
         throws IOException
      {
         this.backupInfo = backupInfo;
         this.isMultiDb = isMultiDb;

         if (tableType == ITEM_TABLE)
         {
            contentFile = new File(restorePath, backupInfo.getItemTableName() + FullBackupJob.CONTENT_FILE_SUFFIX);
            contentLenFile =
               new File(restorePath, backupInfo.getItemTableName() + FullBackupJob.CONTENT_LEN_FILE_SUFFIX);

            tableName = "JCR_" + (isMultiDb ? "M" : "S") + "ITEM";

            if (isMultiDb)
            {
               tableName = "JCR_MITEM";
               if (!backupInfo.isMultiDb())
               {
                  // CONTAINER_NAME column index
                  deleteColumnIndex = 4;

                  // ID and PARENT_ID column indexes
                  convertColumnIndex.add(0);
                  convertColumnIndex.add(1);
               }
            }
            else
            {
               tableName = "JCR_SITEM";
               if (backupInfo.isMultiDb())
               {
                  // CONTAINER_NAME column index
                  newColumnIndex = 4;

                  // ID and PARENT_ID column indexes
                  convertColumnIndex.add(0);
                  convertColumnIndex.add(1);
               }
               else
               {
                  // ID and PARENT_ID and CONTAINER_NAME column indexes
                  convertColumnIndex.add(0);
                  convertColumnIndex.add(1);
                  convertColumnIndex.add(4);
               }
            }
         }
         else if (tableType == VALUE_TABLE)
         {
            contentFile = new File(restorePath, backupInfo.getValueTableName() + FullBackupJob.CONTENT_FILE_SUFFIX);
            contentLenFile =
               new File(restorePath, backupInfo.getValueTableName() + FullBackupJob.CONTENT_LEN_FILE_SUFFIX);

            tableName = "JCR_" + (isMultiDb ? "M" : "S") + "VALUE";

            // auto increment ID column
            skipColumnIndex = 0;

            if (!isMultiDb || !backupInfo.isMultiDb())
            {
               // PROPERTY_ID column index
               convertColumnIndex.add(3);
            }
         }
         else if (tableType == REF_TABLE)
         {
            contentFile = new File(restorePath, backupInfo.getRefTableName() + FullBackupJob.CONTENT_FILE_SUFFIX);
            contentLenFile =
               new File(restorePath, backupInfo.getRefTableName() + FullBackupJob.CONTENT_LEN_FILE_SUFFIX);

            tableName = "JCR_" + (isMultiDb ? "M" : "S") + "REF";

            if (!isMultiDb || !backupInfo.isMultiDb())
            {
               // NODE_ID and PROPERTY_ID column indexes
               convertColumnIndex.add(0);
               convertColumnIndex.add(1);
            }
         }
      }

      /**
       * Returns the table name for restore.
       * 
       * @return table name
       */
      public String getTableName()
      {
         return tableName;
      }

      /**
       * Returns the content file for restore.
       * 
       * @return file
       */
      public File getContentFile()
      {
         return contentFile;
      }

      /**
       * Returns the content length file for restore.
       * 
       * @return file
       */
      public File getContentLenFile()
      {
         return contentLenFile;
      }

      /**
       * Set table name for restore.
       */
      public void setTableName(String tableName)
      {
         this.tableName = tableName;
      }

      /**
       * Set content file for restore.
       */
      public void setContentFile(File file)
      {
         this.contentFile = file;
      }

      /**
       * Set content length file for restore.
       */
      public void setContentLenFile(File file)
      {
         this.contentLenFile = file;
      }

      /**
       * Returns index of column which should be skipped during restore.
       * 
       * @return Integer
       */
      public Integer getSkipColumnIndex()
      {
         return skipColumnIndex;
      }

      /**
       * Returns index of column which should be skipped during restore.
       * 
       * @return Integer
       */
      public Integer getDeleteColumnIndex()
      {
         return deleteColumnIndex;
      }


      /**
       * Returns index of column which should be added during restore.
       * 
       * @return Integer
       */
      public Integer getNewColumnIndex()
      {
         return newColumnIndex;
      }

      /**
       * Returns indexes of columns which should be converted during restore.
       * 
       * @return Integer
       */
      public Set<Integer> getConvertColumnIndexes()
      {
         return convertColumnIndex;
      }

      /**
       * Returns the target workspace name for restore.
       * 
       * @return workspace name
       */
      public boolean isMultiDb()
      {
         return isMultiDb;
      }

      /**
       * Returns the original workspace name where backup was performed.
       * 
       * @return workspace name
       */
      public String getBackupWorkspaceName()
      {
         return backupInfo.getWorkspaceName();
      }

      /**
       * Returns the original value of multi-db parameter of workspace from which backup was performed.
       * 
       * @return multi-db parameter 
       */
      public boolean isBackupMutliDb()
      {
         return backupInfo.isMultiDb();
      }
   }
}
