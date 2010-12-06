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
import org.exoplatform.services.jcr.config.QueryHandlerParams;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.ValueStorageEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.dataflow.serialization.ObjectWriter;
import org.exoplatform.services.jcr.ext.backup.BackupConfig;
import org.exoplatform.services.jcr.ext.backup.BackupOperationException;
import org.exoplatform.services.jcr.ext.backup.impl.AbstractFullBackupJob;
import org.exoplatform.services.jcr.ext.backup.impl.FileNameProducer;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.RdbmsWorkspaceInitializer;
import org.exoplatform.services.jcr.impl.core.query.SystemSearchManager;
import org.exoplatform.services.jcr.impl.dataflow.serialization.ObjectWriterImpl;
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
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Calendar;

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
    * Logger.
    */
   protected static Log log = ExoLogger.getLogger("exo.jcr.component.ext.FullBackupJob");

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
    * Indicates the way to get value thru getBinaryStream() method.
    */
   public static final int GET_BINARY_STREAM_METHOD = 0;

   /**
    * Indicates the way to get value thru getString() method.
    */
   public static final int GET_STRING_METHOD = 1;

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
      Integer transactionIsolation = null;
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

         // dump JCR data
         String[][] scripts;
         if (Boolean.parseBoolean(multiDb))
         {
            scripts =
               new String[][]{{"JCR_MVALUE", "select * from JCR_MVALUE"}, {"JCR_MREF", "select * from JCR_MREF"},
                  {"JCR_MITEM", "select * from JCR_MITEM"}};
         }
         else
         {
            scripts =
               new String[][]{
                  {
                     "JCR_SVALUE",
                     "select from JCR_SVALUE where exists(select * from JCR_SITEM where JCR_SITEM.ID=JCR_SVALUE.PROPERTY_ID and JCR_SITEM.CONTAINER_NAME="
                        + workspaceName + ")"},
                  {
                     "JCR_SREF",
                     "select from JCR_SREF where exists(select * from JCR_SITEM where JCR_SITEM.ID=JCR_SREF.PROPERTY_ID and JCR_SITEM.CONTAINER_NAME="
                        + workspaceName + ")"},
                  {"JCR_SITEM", "select from JCR_SITEM where CONTAINER_NAME=" + workspaceName}};
         }

         for (String script[] : scripts)
         {
            dumpTable(jdbcConn, script[0], script[1]);
         }

         // dump LOCK data
         scripts =
            new String[][]{
               {"JCR_LOCK_" + workspaceName.toUpperCase(), "select * from JCR_LOCK_" + workspaceName.toUpperCase()},
               {"JCR_LOCK_" + workspaceName.toUpperCase() + "_D",
                  "select * from JCR_LOCK_" + workspaceName.toUpperCase() + "_D"}};

         for (String script[] : scripts)
         {
            if (jdbcConn.getMetaData().getTables(null, null, script[0], new String[]{"TABLE"}).next())
            {
               dumpTable(jdbcConn, script[0], script[1]);
            }
         }

         backupValueStorage(workspaceEntry);
         backupIndex(workspaceEntry);
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
            File destDir = new File(getStorageURL().getFile(), RdbmsWorkspaceInitializer.INDEX_DIR);
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
               File destDir = new File(getStorageURL().getFile(), RdbmsWorkspaceInitializer.SYSTEM_INDEX_DIR);
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
               File destValuesDir = new File(getStorageURL().getFile(), RdbmsWorkspaceInitializer.VALUE_STORAGE_DIR);
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
      int getValueMethod = GET_BINARY_STREAM_METHOD;

      String dbDialect = DialectDetecter.detect(jdbcConn.getMetaData());
      if (dbDialect.equals(DBConstants.DB_DIALECT_HSQLDB))
      {
         getValueMethod = GET_STRING_METHOD;
      }

      ObjectWriter contentWriter = null;
      ObjectWriter contentLenWriter = null;
      PreparedStatement stmt = null;
      ResultSet rs = null;
      try
      {
         File contentFile =
            new File(getStorageURL().getFile(), tableName + RdbmsWorkspaceInitializer.CONTENT_FILE_SUFFIX);
         contentWriter = new ObjectWriterImpl(PrivilegedFileHelper.fileOutputStream(contentFile));

         File contentLenFile =
            new File(getStorageURL().getFile(), tableName + RdbmsWorkspaceInitializer.CONTENT_LEN_FILE_SUFFIX);
         contentLenWriter = new ObjectWriterImpl(PrivilegedFileHelper.fileOutputStream(contentLenFile));

         stmt = jdbcConn.prepareStatement(script);
         rs = stmt.executeQuery();
         ResultSetMetaData metaData = rs.getMetaData();

         int columnCount = metaData.getColumnCount();

         contentWriter.writeInt(columnCount);
         for (int i = 0; i < columnCount; i++)
         {
            contentWriter.writeInt(metaData.getColumnType(i + 1));
         }

         // Now we can output the actual data
         while (rs.next())
         {
            for (int i = 0; i < columnCount; i++)
            {
               InputStream value = getInputStream(rs, i + 1, getValueMethod);
               if (value == null)
               {
                  contentLenWriter.writeByte(NULL_LEN);
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
                  writeCompressedContentLen(contentLenWriter, len);
               }
            }
         }
      }
      finally
      {
         if (contentWriter != null)
         {
            contentWriter.close();
         }

         if (contentLenWriter != null)
         {
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
    * Get input stream from value.
    */
   private InputStream getInputStream(ResultSet rs, int columnIndex, int getValueMethod) throws SQLException,
      UnsupportedEncodingException, BackupOperationException
   {
      if (getValueMethod == GET_STRING_METHOD)
      {
         String str = rs.getString(columnIndex);
         return str == null ? null : new ByteArrayInputStream(str.getBytes(Constants.DEFAULT_ENCODING));
      }
      else if (getValueMethod == GET_BINARY_STREAM_METHOD)
      {
         return rs.getBinaryStream(columnIndex);
      }

      throw new BackupOperationException("There is no way get input stream from value");
   }

   /**
    * Write content length in output. 
    */
   private void writeCompressedContentLen(ObjectWriter out, long len) throws IOException
   {
      if (len < Byte.MAX_VALUE)
      {
         out.writeByte(BYTE_LEN);
         out.writeByte((byte)len);
      }
      else if (len < Integer.MAX_VALUE)
      {
         out.writeByte(INT_LEN);
         out.writeInt((int)len);
      }
      else
      {
         out.writeByte(LONG_LEN);
         out.writeLong(len);
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
}
