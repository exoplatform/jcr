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
import org.exoplatform.services.jcr.config.LockManagerEntry;
import org.exoplatform.services.jcr.config.QueryHandlerParams;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.ValueStorageEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.backup.BackupConfig;
import org.exoplatform.services.jcr.ext.backup.BackupOperationException;
import org.exoplatform.services.jcr.ext.backup.impl.AbstractFullBackupJob;
import org.exoplatform.services.jcr.ext.backup.impl.FileNameProducer;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.lock.cacheable.AbstractCacheableLockManager;
import org.exoplatform.services.jcr.impl.core.query.SystemSearchManager;
import org.exoplatform.services.jcr.impl.dataflow.serialization.ObjectZipWriterImpl;
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.services.jcr.impl.storage.jdbc.DialectDetecter;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.jcr.impl.storage.value.fs.FileValueStorage;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Calendar;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * Created by The eXo Platform SARL Author : Alex Reshetnyak alex.reshetnyak@exoplatform.com.ua Nov
 * 21, 2007
 */
public class FullBackupJob extends AbstractFullBackupJob
{
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
    * Logger.
    */
   protected static Log log = ExoLogger.getLogger("exo.jcr.component.ext.FullBackupJob");

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
    * {@inheritDoc}
    */
   @Override
   protected URL createStorage() throws FileNotFoundException, IOException
   {
      FileNameProducer fnp =
         new FileNameProducer(config.getRepository(), config.getWorkspace(),
            PrivilegedFileHelper.getAbsolutePath(config.getBackupDir()), super.timeStamp, true, true);

      return new URL("file:" + PrivilegedFileHelper.getAbsolutePath(fnp.getNextFile()));
   }

   /**
    * {@inheritDoc}
    */
   public void init(ManageableRepository repository, String workspaceName, BackupConfig config, Calendar timeStamp)
   {
      this.repository = repository;
      this.workspaceName = workspaceName;
      this.config = config;
      this.timeStamp = timeStamp;

      try
      {
         url = createStorage();
      }
      catch (FileNotFoundException e)
      {
         log.error("Full backup initialization failed ", e);
         notifyError("Full backup initialization failed ", e);
      }
      catch (IOException e)
      {
         log.error("Full backup initialization failed ", e);
         notifyError("Full backup initialization failed ", e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void run()
   {
      notifyListeners();

      Connection jdbcConn = null;
      try
      {
         WorkspaceEntry workspaceEntry = null;
         for (WorkspaceEntry entry : repository.getConfiguration().getWorkspaceEntries())
         {
            if (entry.getName().equals(workspaceName))
            {
               workspaceEntry = entry;
               break;
            }
         }
         if (workspaceEntry == null)
         {
            throw new RepositoryConfigurationException("Workpace name " + workspaceName
               + " not found in repository configuration");
         }

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

         jdbcConn = SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<Connection>()
         {
            public Connection run() throws Exception
            {
               return ds.getConnection();

            }
         });
         
         RDBMSBackupInfoWriter backupInfoWriter = new RDBMSBackupInfoWriter(getStorageURL().getFile());

         backupInfoWriter.setRepositoryName(repository.getConfiguration().getName());
         backupInfoWriter.setWorkspaceName(workspaceName);
         backupInfoWriter.setMultiDb(isMultiDb);

         // dump JCR data
         String[][] scripts;
         if (isMultiDb)
         {
            scripts =
               new String[][]{
                  {"JCR_MITEM", "select * from JCR_MITEM where JCR_MITEM.name <> '" + Constants.ROOT_PARENT_NAME + "'"},
                  {"JCR_MVALUE", "select * from JCR_MVALUE"}, {"JCR_MREF", "select * from JCR_MREF"}};
         }
         else
         {
            scripts =
               new String[][]{
                  {"JCR_SITEM", "select * from JCR_SITEM where CONTAINER_NAME='" + workspaceName + "'"},
                  {
                     "JCR_SVALUE",
                     "select * from JCR_SVALUE where exists(select * from JCR_SITEM where JCR_SITEM.ID=JCR_SVALUE.PROPERTY_ID and JCR_SITEM.CONTAINER_NAME='"
                        + workspaceName + "')"},
                  {
                     "JCR_SREF",
                     "select * from JCR_SREF where exists(select * from JCR_SITEM where JCR_SITEM.ID=JCR_SREF.PROPERTY_ID and JCR_SITEM.CONTAINER_NAME='"
                        + workspaceName + "')"}};
         }

         backupInfoWriter.setItemTableName(scripts[0][0]);
         backupInfoWriter.setValueTableName(scripts[1][0]);
         backupInfoWriter.setRefTableName(scripts[2][0]);

         // Lock tables
         ResultSet rs = null;
         Statement st = null;
         try
         {
            DatabaseMetaData metaData = jdbcConn.getMetaData();

            rs = metaData.getTables(null, null, "%", new String[]{"TABLE"});
            st = jdbcConn.createStatement();
            int dialect = DialectDetecter.detect(metaData).hashCode();

            while (rs.next())
            {
               String tableName = rs.getString("TABLE_NAME");
               if (dialect == DB_DIALECT_HSQLDB)
               {
                  st.execute("SET TABLE " + tableName + " READONLY TRUE");
               }
               else
               {
                  st.execute("LOCK TABLES " + tableName + " WRITE");
               }
            }
         }
         finally
         {
            if (rs != null)
            {
               rs.close();
            }

            if (st != null)
            {
               st.close();
            }
         }

         for (String script[] : scripts)
         {
            dumpTable(jdbcConn, script[0], script[1]);
         }

         // dump LOCK data
         LockManagerEntry lockEntry = workspaceEntry.getLockManager();
         if (lockEntry != null)
         {
            List<String> lockTableNames = AbstractCacheableLockManager.getLockTableNames(lockEntry);
            backupInfoWriter.setLockTableNames(lockTableNames);

            for (String tableName : lockTableNames)
            {
               dumpTable(jdbcConn, tableName, AbstractCacheableLockManager.getSelectScript(tableName));
            }
         }

         backupValueStorage(workspaceEntry);
         backupIndex(workspaceEntry);

         // write backup information
         backupInfoWriter.write();
      }
      catch (RepositoryConfigurationException e)
      {
         log.error("Full backup failed " + getStorageURL().getPath(), e);
         notifyError("Full backup failed", e);
      }
      catch (NameNotFoundException e)
      {
         log.error("Full backup failed " + getStorageURL().getPath(), e);
         notifyError("Full backup failed", e);
      }
      catch (NamingException e)
      {
         log.error("Full backup failed " + getStorageURL().getPath(), e);
         notifyError("Full backup failed", e);
      }
      catch (SQLException e)
      {
         log.error("Full backup failed " + getStorageURL().getPath(), e);
         notifyError("Full backup failed", e);
      }
      catch (IOException e)
      {
         log.error("Full backup failed " + getStorageURL().getPath(), e);
         notifyError("Full backup failed", e);
      }
      catch (BackupOperationException e)
      {
         log.error("Full backup failed " + getStorageURL().getPath(), e);
         notifyError("Full backup failed", e);
      }
      finally
      {
         if (jdbcConn != null)
         {
            // unlock tables
            try
            {
               ResultSet rs = null;
               Statement st = null;
               try
               {
                  DatabaseMetaData metaData = jdbcConn.getMetaData();
                  st = jdbcConn.createStatement();
                  int dialect = DialectDetecter.detect(metaData).hashCode();

                  if (dialect == DB_DIALECT_HSQLDB)
                  {
                     rs = metaData.getTables(null, null, "%", new String[]{"TABLE"});
                     while (rs.next())
                     {
                        String tableName = rs.getString("TABLE_NAME");
                        st.execute("SET TABLE " + tableName + " READONLY FALSE");
                     }
                  }
                  else
                  {
                     st.execute("UNLOCK TABLES");
                  }
               }
               finally
               {
                  if (rs != null)
                  {
                     rs.close();
                  }

                  if (st != null)
                  {
                     st.close();
                  }
               }

               jdbcConn.close();
            }
            catch (SQLException e)
            {
               log.error("Full backup failed " + getStorageURL().getPath(), e);
               notifyError("Full backup failed", e);
            }
         }
      }

      state = FINISHED;
      notifyListeners();
   }

   /**
    * Backup index files.
    * 
    * @param workspaceEntry
    * @throws RepositoryConfigurationException
    * @throws BackupOperationException
    * @throws IOException
    */
   protected void backupIndex(WorkspaceEntry workspaceEntry) throws RepositoryConfigurationException,
      BackupOperationException, IOException
   {
      if (workspaceEntry.getQueryHandler() != null)
      {
         File srcDir = new File(workspaceEntry.getQueryHandler().getParameterValue(QueryHandlerParams.PARAM_INDEX_DIR));
         if (!PrivilegedFileHelper.exists(srcDir))
         {
            throw new BackupOperationException("Can't backup index. Directory " + srcDir.getName() + " doesn't exists");
         }
         else
         {
            File destDir = new File(getStorageURL().getFile(), INDEX_DIR);
            copyDirectory(srcDir, destDir);
         }

         if (repository.getConfiguration().getSystemWorkspaceName().equals(workspaceName))
         {
            srcDir =
               new File(PrivilegedFileHelper.getCanonicalPath(srcDir) + "_" + SystemSearchManager.INDEX_DIR_SUFFIX);
            if (!PrivilegedFileHelper.exists(srcDir))
            {
               throw new BackupOperationException("Can't backup system index. Directory " + srcDir.getName()
                  + " doesn't exists");
            }
            else
            {
               File destDir = new File(getStorageURL().getFile(), SYSTEM_INDEX_DIR);
               copyDirectory(srcDir, destDir);
            }
         }
      }
   }

   /**
    * Backup value storage files.
    * 
    * @param workspaceEntry
    * @throws RepositoryConfigurationException
    * @throws BackupOperationException
    * @throws IOException
    */
   protected void backupValueStorage(WorkspaceEntry workspaceEntry) throws RepositoryConfigurationException,
      BackupOperationException, IOException
   {
      if (workspaceEntry.getContainer().getValueStorages() != null)
      {
         for (ValueStorageEntry valueStorage : workspaceEntry.getContainer().getValueStorages())
         {
            File srcDir = new File(valueStorage.getParameterValue(FileValueStorage.PATH));
            if (!PrivilegedFileHelper.exists(srcDir))
            {
               throw new BackupOperationException("Can't backup value storage. Directory " + srcDir.getName()
                  + " doesn't exists");
            }
            else
            {
               File destValuesDir = new File(getStorageURL().getFile(), VALUE_STORAGE_DIR);
               File destDir = new File(destValuesDir, valueStorage.getId());

               copyDirectory(srcDir, destDir);
            }
         }
      }
   }

   /**
    * Dump table.
    */
   protected void dumpTable(Connection jdbcConn, String tableName, String script) throws SQLException, IOException,
      BackupOperationException
   {
      int dialect = DialectDetecter.detect(jdbcConn.getMetaData()).hashCode();

      ObjectZipWriterImpl contentWriter = null;
      ObjectZipWriterImpl contentLenWriter = null;
      PreparedStatement stmt = null;
      ResultSet rs = null;
      try
      {
         File contentFile = new File(getStorageURL().getFile(), tableName + CONTENT_FILE_SUFFIX);
         contentWriter = new ObjectZipWriterImpl(PrivilegedFileHelper.zipOutputStream(contentFile));
         contentWriter.putNextEntry(new ZipEntry(tableName));

         File contentLenFile = new File(getStorageURL().getFile(), tableName + CONTENT_LEN_FILE_SUFFIX);
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
                     value = rs.getBinaryStream(i+1);
                  }
                  else
                  {
                     String str = rs.getString(i+1);
                     value = str == null ? null : new ByteArrayInputStream(str.getBytes(Constants.DEFAULT_ENCODING));
                  }
               }
               else
               {
                  value = rs.getBinaryStream(i+1);
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

   /**
    * {@inheritDoc}
    */
   public void stop()
   {
      log.info("Stop requested " + getStorageURL().getPath());
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
         ZipOutputStream out = null;

         try
         {
            in = PrivilegedFileHelper.fileInputStream(srcPath);
            out = PrivilegedFileHelper.zipOutputStream(dstPath);
            out.putNextEntry(new ZipEntry(srcPath.getName()));

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
               out.closeEntry();
               out.close();
            }
         }
      }
   }
}
