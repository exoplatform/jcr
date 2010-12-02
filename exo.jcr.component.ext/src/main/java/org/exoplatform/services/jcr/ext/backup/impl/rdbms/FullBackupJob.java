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
import org.exoplatform.services.jcr.ext.backup.impl.AbstractFullBackupJob;
import org.exoplatform.services.jcr.ext.backup.impl.FileNameProducer;
import org.exoplatform.services.jcr.impl.core.query.SystemSearchManager;
import org.exoplatform.services.jcr.impl.dataflow.serialization.ObjectWriterImpl;
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

   public static final String CONTENT_FILE_SUFFIX = "dump";

   public static final String CONTENT_LEN_FILE_SUFFIX = "len";

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
            throw new RepositoryConfigurationException("Source name not found in workspace configuration "
               + workspaceName);
         }

         final DataSource ds = (DataSource)new InitialContext().lookup(dsName);
         if (ds == null)
         {
            throw new NameNotFoundException("Data source name " + dsName + " not found");
         }

         jdbcConn =
            SecurityHelper.doPriviledgedSQLExceptionAction(new PrivilegedExceptionAction<Connection>()
            {
               public Connection run() throws Exception
               {
                  return ds.getConnection();

               }
            });
            
         
         // dump JCR data
         Boolean multiDb = Boolean.parseBoolean(workspaceEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.MULTIDB));

         String[][] scripts;
         if (multiDb)
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
                        + workspaceEntry.getName() + ")"},
                  {
                     "JCR_SREF",
                     "select from JCR_SREF where exists(select * from JCR_SITEM where JCR_SITEM.ID=JCR_SREF.PROPERTY_ID and JCR_SITEM.CONTAINER_NAME="
                        + workspaceEntry.getName() + ")"},
                  {"JCR_SITEM", "select from JCR_SITEM where CONTAINER_NAME=" + workspaceEntry.getName()}};
         }

         for (String script[] : scripts)
         {
            dumpTable(jdbcConn, script[0], script[1]);
         }

         // copy value storage directory
         for (ValueStorageEntry valueStorage : workspaceEntry.getContainer().getValueStorages())
         {
            File srcDir = new File(valueStorage.getParameterValue(FileValueStorage.PATH));
            if (!PrivilegedFileHelper.exists(srcDir))
            {
               throw new FileNotFoundException("File or directory " + srcDir.getName() + " doesn't exists");
            }

            File destValuesDir = new File(getStorageURL().getFile(), VALUE_STORAGE_DIR);
            File destDir = new File(destValuesDir, valueStorage.getId());

            copyDirectory(srcDir, destDir);
         }

         // copy index directory 
         File srcDir = new File(workspaceEntry.getQueryHandler().getParameterValue(QueryHandlerParams.PARAM_INDEX_DIR));
         if (!PrivilegedFileHelper.exists(srcDir))
         {
            throw new FileNotFoundException("File or directory " + srcDir.getName() + " doesn't exists");
         }

         File destDir = new File(getStorageURL().getFile(), INDEX_DIR);
         copyDirectory(srcDir, destDir);

         if (repository.getConfiguration().getSystemWorkspaceName().equals(workspaceName))
         {
            srcDir = new File(PrivilegedFileHelper.getCanonicalPath(srcDir) + "_" + SystemSearchManager.INDEX_DIR_SUFFIX);
            if (!PrivilegedFileHelper.exists(srcDir))
            {
               throw new FileNotFoundException("File or directory " + srcDir.getName() + " doesn't exists");
            }

            destDir = new File(getStorageURL().getFile(), SYSTEM_INDEX_DIR);
         }
         copyDirectory(srcDir, destDir);
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
               log.error("Full backup failed " + getStorageURL().getPath(), e);
               notifyError("Full backup failed", e);
            }
         }
      }

      state = FINISHED;
      notifyListeners();
   }

   /**
    * Dump table.
    */
   private void dumpTable(Connection jdbcConn, String tableName, String script) throws SQLException, IOException
   {
      ObjectWriter contentWriter = null;
      ObjectWriter contentLenWriter = null;
      PreparedStatement stmt = null;
      try
      {
         File contentFile = new File(getStorageURL().getFile(), tableName + "." + CONTENT_FILE_SUFFIX);
         contentWriter = new ObjectWriterImpl(PrivilegedFileHelper.fileOutputStream(contentFile));

         File contentLenFile = new File(getStorageURL().getFile(), tableName + "." + CONTENT_LEN_FILE_SUFFIX);
         contentLenWriter = new ObjectWriterImpl(PrivilegedFileHelper.fileOutputStream(contentLenFile));

         stmt = jdbcConn.prepareStatement(script);
         ResultSet rs = stmt.executeQuery();
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
               String str = rs.getString(i + 1);
               InputStream value = str == null ? null : new ByteArrayInputStream(str.getBytes());
               if (value == null)
               {
                  contentLenWriter.writeByte((byte)0);
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
         rs.close();
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

         if (stmt != null)
         {
            stmt.close();
         }
      }
   }

   /**
    * @throws IOException 
    */
   private void writeCompressedContentLen(ObjectWriter out, long len) throws IOException
   {
      if (len < Byte.MAX_VALUE)
      {
         out.writeByte((byte)1);
         out.writeByte((byte)len);
      }
      else if (len < Integer.MAX_VALUE)
      {
         out.writeByte((byte)2);
         out.writeInt((int)len);
      }
      else
      {
         out.writeByte((byte)3);
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
