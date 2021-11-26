/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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

package org.exoplatform.services.jcr.ext.backup;

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.container.xml.Deserializer;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.RepositoryServiceConfiguration;
import org.exoplatform.services.jcr.ext.backup.server.RepositoryRestoreExeption;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.util.JCRDateFormat;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.StartElement;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>Date: 2010
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id$
 */
public class RepositoryBackupChainLog
{
   private class LogWriter
   {

      protected Log logger = ExoLogger.getLogger("exo.jcr.component.ext.LogWriter");

      private File logFile;

      XMLStreamWriter writer;

      public LogWriter(File file) throws FileNotFoundException, XMLStreamException, FactoryConfigurationError
      {
         this.logFile = file;

         try
         {
            writer = SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<XMLStreamWriter>()
            {
               public XMLStreamWriter run() throws Exception
               {
                  return XMLOutputFactory.newInstance().createXMLStreamWriter(new FileOutputStream(logFile),
                           Constants.DEFAULT_ENCODING);
               }
            });
         }
         catch (PrivilegedActionException pae)
         {
            Throwable cause = pae.getCause();
            if (cause instanceof FileNotFoundException)
            {
               throw (FileNotFoundException)cause;
            }
            else if (cause instanceof XMLStreamException)
            {
               throw (XMLStreamException)cause;
            }
            else if (cause instanceof FactoryConfigurationError)
            {
               throw (FactoryConfigurationError)cause;
            }
            else if (cause instanceof RuntimeException)
            {
               throw (RuntimeException)cause;
            }
            else
            {
               throw new RuntimeException(cause);
            }
         };

         writer.writeStartDocument();
         writer.writeStartElement("repository-backup-chain-log");

         writer.writeStartElement("version-log");
         writer.writeCharacters(versionLog);
         writer.writeEndElement();

         writer.writeStartElement("start-time");
         writer.writeCharacters(JCRDateFormat.format(startedTime));
         writer.writeEndElement();

         writer.flush();
      }

      public void writeSystemWorkspaceName(String wsName) throws XMLStreamException
      {
         writer.writeStartElement("system-workspace");
         writer.writeCharacters(wsName);
         writer.writeEndElement();
         writer.flush();
      }

      public void writeBackupsPath(List<String> wsLogFilePathList, RepositoryBackupConfig config)
               throws XMLStreamException,
               IOException
      {
         writer.writeStartElement("workspaces-backup-info");

         for (String path : wsLogFilePathList)
         {
            writer.writeStartElement("url");
            writer.writeCharacters(RepositoryChainLogPathHelper.getRelativePath(path, PrivilegedFileHelper.getCanonicalPath(config
                     .getBackupDir())));
            writer.writeEndElement();
         }

         writer.writeEndElement();

         writer.flush();
      }

      public synchronized void write(RepositoryBackupConfig config, String fullBackupType,
               String incrementalBackupType, File serviceBackupDir)
               throws XMLStreamException, IOException
      {
         writer.writeStartElement("repository-backup-config");

         writer.writeStartElement("backup-type");
         writer.writeCharacters(String.valueOf(config.getBackupType()));
         writer.writeEndElement();

         writer.writeStartElement("full-backup-type");
         writer.writeCharacters(fullBackupType);
         writer.writeEndElement();

         writer.writeStartElement("incremental-backup-type");
         writer.writeCharacters(incrementalBackupType);
         writer.writeEndElement();

         if (config.getBackupDir() != null)
         {
            String backupDir = PrivilegedFileHelper.getCanonicalPath(config.getBackupDir());
            String serviceBackupDirPath = PrivilegedFileHelper.getCanonicalPath(serviceBackupDir);
            if (backupDir.startsWith(serviceBackupDirPath))
            {

               backupDir = "." + backupDir.replace(serviceBackupDirPath, "");

               if (File.separator.equals("\\"))
               {
                  backupDir = backupDir.replaceAll("\\\\", "/");
               }
            }
            
            writer.writeStartElement("backup-dir");
            writer.writeCharacters(backupDir);
            writer.writeEndElement();
         }

         if (config.getRepository() != null)
         {
            writer.writeStartElement("repository");
            writer.writeCharacters(config.getRepository());
            writer.writeEndElement();
         }

         writer.writeStartElement("incremental-job-period");
         writer.writeCharacters(Long.toString(config.getIncrementalJobPeriod()));
         writer.writeEndElement();

         writer.writeStartElement("incremental-job-number");
         writer.writeCharacters(Integer.toString(config.getIncrementalJobNumber()));
         writer.writeEndElement();

         writer.writeEndElement();

         writer.flush();
      }

      public synchronized void writeEndLog()
      {
         try
         {
            writer.writeStartElement("finish-time");
            writer.writeCharacters(JCRDateFormat.format(finishedTime));
            writer.writeEndElement();

            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
         }
         catch (XMLStreamException e)
         {
            logger.error("Can't write end log", e);
         }
      }

      public synchronized void writeRepositoryEntry(RepositoryEntry rEntry,
               RepositoryServiceConfiguration serviceConfiguration) throws XMLStreamException, IOException,
               JiBXException
      {
         File config =
                  new File(PrivilegedFileHelper.getCanonicalPath(RepositoryBackupChainLog.this.config.getBackupDir())
                     + File.separator + "original-repository-config.xml");
         PrivilegedFileHelper.createNewFile(config);
         OutputStream saveStream = PrivilegedFileHelper.fileOutputStream(config);
      
         ArrayList<RepositoryEntry> repositoryEntries = new ArrayList<RepositoryEntry>();
         repositoryEntries.add(rEntry);
      
         RepositoryServiceConfiguration newRepositoryServiceConfiguration =
                  new RepositoryServiceConfiguration(serviceConfiguration.getDefaultRepositoryName(), repositoryEntries);
      
         IBindingFactory bfact;
         try
         {
            bfact = SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<IBindingFactory>()
            {
               public IBindingFactory run() throws Exception
               {
                  return BindingDirectory.getFactory(RepositoryServiceConfiguration.class);
               }
            });
         }
         catch (PrivilegedActionException pae)
         {
            Throwable cause = pae.getCause();
            if (cause instanceof JiBXException)
            {
               throw (JiBXException) cause;
            }
            else if (cause instanceof RuntimeException)
            {
               throw (RuntimeException) cause;
            }
            else
            {
               throw new RuntimeException(cause);
            }
         }
         IMarshallingContext mctx = bfact.createMarshallingContext();
      
         mctx.marshalDocument(newRepositoryServiceConfiguration, "ISO-8859-1", null, saveStream);
         saveStream.close();
      
         writer.writeStartElement("original-repository-config");
         writer.writeCharacters(config.getName());
         writer.writeEndElement();
      }
   }

   private class LogReader
   {
      protected Log logger = ExoLogger.getLogger("exo.jcr.component.ext.LogReader");

      private File logFile;

      private XMLStreamReader reader;

      private String version;

      public LogReader(File logFile) throws FileNotFoundException, XMLStreamException, FactoryConfigurationError
      {
         this.logFile = logFile;
         reader =
                  XMLInputFactory.newInstance().createXMLStreamReader(PrivilegedFileHelper.fileInputStream(logFile),
                           Constants.DEFAULT_ENCODING);
      }

      public void readLogFile() throws UnsupportedEncodingException, Exception
      {
         boolean endDocument = false;

         while (!endDocument)
         {
            int eventCode = reader.next();
            switch (eventCode)
            {

               case StartElement.START_ELEMENT :
                  String name = reader.getLocalName();

                  if (name.equals("repository-backup-config"))
                     config = readBackupConfig();

                  if (name.equals("system-workspace"))
                     workspaceSystem = readContent();

                  if (name.equals("backup-config"))
                     config = readBackupConfig();

                  if (name.equals("workspaces-backup-info"))
                     workspaceBackupsInfo = readWorkspaceBackupInfo();

                  if (name.equals("start-time"))
                     startedTime = JCRDateFormat.parse(readContent());

                  if (name.equals("finish-time"))
                     finishedTime = JCRDateFormat.parse(readContent());

                  if (name.equals("original-repository-config"))
                     originalRepositoryEntry = readRepositoryEntry();

                  if (name.equals("version-log"))
                  {
                     this.version = readContent();
                  }


                  break;

               case StartElement.END_DOCUMENT :
                  endDocument = true;
                  break;
            }
         }
      }

      private RepositoryEntry readRepositoryEntry() throws UnsupportedEncodingException, Exception
      {
         String configName = readContent();

         File configFile =
                  new File(PrivilegedFileHelper.getCanonicalPath(getBackupConfig().getBackupDir()) + File.separator
                           + configName);

         if (!PrivilegedFileHelper.exists(configFile))
         {
            throw new RepositoryRestoreExeption("The backup set is not contains original repository configuration : "
                     + PrivilegedFileHelper.getCanonicalPath(getBackupConfig().getBackupDir()));
         }

         IBindingFactory factory;
         try
         {
            factory = SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<IBindingFactory>()
            {
               public IBindingFactory run() throws Exception
               {
                  return BindingDirectory.getFactory(RepositoryServiceConfiguration.class);
               }
            });
         }
         catch (PrivilegedActionException pae)
         {
            Throwable cause = pae.getCause();
            if (cause instanceof JiBXException)
            {
               throw (JiBXException)cause;
            }
            else if (cause instanceof RuntimeException)
            {
               throw (RuntimeException)cause;
            }
            else
            {
               throw new RuntimeException(cause);
            }
         }

         IUnmarshallingContext uctx = factory.createUnmarshallingContext();
         RepositoryServiceConfiguration conf =
                  (RepositoryServiceConfiguration) uctx.unmarshalDocument(PrivilegedFileHelper
                           .fileInputStream(configFile), null);

         if (conf.getRepositoryConfigurations().size() != 1)
         {
            throw new RepositoryRestoreExeption(
                     "The oririginal configuration should be contains only one repository entry :"
                     + PrivilegedFileHelper.getCanonicalPath(configFile));
         }

         if (!conf.getRepositoryConfiguration(getBackupConfig().getRepository()).getName().equals(
                  getBackupConfig().getRepository()))
         {
            throw new RepositoryRestoreExeption(
                     "The oririginal configuration should be contains only one repository entry with name \""
                              + getBackupConfig().getRepository() + "\" :"
                              + PrivilegedFileHelper.getCanonicalPath(configFile));
         }
         
         

         return conf.getRepositoryConfiguration(getBackupConfig().getRepository());
      }

      private List<String> readWorkspaceBackupInfo() throws XMLStreamException, IOException
      {
         List<String> wsBackupInfo = new ArrayList<String>();

         boolean endWorkspaceBackupInfo = false;

         while (!endWorkspaceBackupInfo)
         {
            int eventCode = reader.next();
            switch (eventCode)
            {

               case StartElement.START_ELEMENT :
                  String name = reader.getLocalName();

                  if (name.equals("url"))
                  {
                     if (version != null && version.equals(VERSION_LOG_1_1))
                     {
                        String path = readContent();
                        wsBackupInfo.add(RepositoryChainLogPathHelper.getPath(path, PrivilegedFileHelper.getCanonicalPath(config
                                 .getBackupDir())));
                     }
                     else
                     {
                        wsBackupInfo.add(readContent());
                     }
                  }

                  break;

               case StartElement.END_ELEMENT :
                  String tagName = reader.getLocalName();

                  if (tagName.equals("workspaces-backup-info"))
                     endWorkspaceBackupInfo = true;
                  break;
            }
         }

         return wsBackupInfo;
      }

      private BackupConfig readBackupConfig() throws XMLStreamException, IOException
      {
         BackupConfig conf = new BackupConfig();

         boolean endBackupConfig = false;

         while (!endBackupConfig)
         {
            int eventCode = reader.next();
            switch (eventCode)
            {

               case StartElement.START_ELEMENT :
                  String name = reader.getLocalName();

                  if (name.equals("backup-dir"))
                  {
                     if (version != null && version.equals(VERSION_LOG_1_1))
                     {
                        String dir = readContent();
                        if (dir.equals("."))
                        {
                           String path = PrivilegedFileHelper.getCanonicalPath(logFile.getParentFile());

                           conf.setBackupDir(new File(path));
                        }
                        else if (dir.startsWith("./"))
                        {
                           String path = PrivilegedFileHelper.getCanonicalPath(logFile.getParentFile());

                           dir = dir.replace("./", "/");
                           
                           if (File.separator.equals("\\"))
                           {
                              dir = dir.replaceAll("/", "\\\\");
                           }

                           conf.setBackupDir(new File(path + dir));
                        }
                        else
                        {
                           conf.setBackupDir(new File(Deserializer.resolveVariables(dir)));
                        }
                     }
                     else
                     {
                        conf.setBackupDir(new File(readContent()));
                     }
                  }

                  if (name.equals("backup-type"))
                     conf.setBackupType(Integer.valueOf(readContent()));

                  if (name.equals("repository"))
                     conf.setRepository(readContent());

                  if (name.equals("workspace"))
                     conf.setWorkspace(readContent());

                  if (name.equals("incremental-job-period"))
                     conf.setIncrementalJobPeriod(Long.valueOf(readContent()).longValue());

                  if (name.equals("incremental-job-number"))
                     conf.setIncrementalJobNumber(Integer.valueOf(readContent()).intValue());

                  if (name.equals("full-backup-type"))
                     fullBackupType = readContent();

                  if (name.equals("incremental-backup-type"))
                     increnetalBackupType = readContent();

                  break;

               case StartElement.END_ELEMENT :
                  String tagName = reader.getLocalName();

                  if (tagName.equals("repository-backup-config"))
                     endBackupConfig = true;
                  break;
            }
         }

         return conf;
      }

      private String readContent() throws XMLStreamException
      {
         String content = null;

         int eventCode = reader.next();

         if (eventCode == StartElement.CHARACTERS)
            content = reader.getText();

         return content;
      }

      public String getVersionLog()
      {
         return version;
      }
   }

   protected static Log logger = ExoLogger.getLogger("exo.jcr.component.ext.BackupChainLog");

   /**
    * Start for 1.1 version log will be stored relative paths. 
    */
   protected static String VERSION_LOG_1_1 = "1.1";

   public static final String PREFIX = "repository-backup-";

   private static final String SUFFIX = ".xml";

   private File log;

   private LogWriter logWriter;

   private LogReader logReader;

   private RepositoryBackupConfig config;

   private String backupId;

   private Calendar startedTime;

   private Calendar finishedTime;

   private boolean finalized;

   private List<String> workspaceBackupsInfo;

   private String workspaceSystem;

   private String fullBackupType;

   private String increnetalBackupType;

   private RepositoryEntry originalRepositoryEntry;

   private final String versionLog;

   /**
    * @param logDirectory
    * @param config
    * @param systemWorkspace
    * @param wsLogFilePathList
    * @param backupId
    * @param startTime
    * @param rEntry
    * @throws BackupOperationException
    */
   public RepositoryBackupChainLog(File logDirectory, RepositoryBackupConfig config, String fullBackupType,
            String incrementalBackupType, String systemWorkspace, List<String> wsLogFilePathList, String backupId,
            Calendar startTime, RepositoryEntry rEntry, RepositoryServiceConfiguration repositoryServiceConfiguration)
            throws BackupOperationException
   {
      try
      {
         this.finalized = false;
         this.versionLog = VERSION_LOG_1_1;
         this.log =
                  new File(PrivilegedFileHelper.getCanonicalPath(logDirectory) + File.separator
                           + (PREFIX + backupId + SUFFIX));
         PrivilegedFileHelper.createNewFile(this.log);
         this.backupId = backupId;
         this.config = config;
         this.startedTime = Calendar.getInstance();
         this.fullBackupType = fullBackupType;
         this.increnetalBackupType = incrementalBackupType;
         this.originalRepositoryEntry = rEntry;

         logWriter = new LogWriter(log);
         logWriter.write(config, fullBackupType, incrementalBackupType, logDirectory);
         logWriter.writeSystemWorkspaceName(systemWorkspace);
         logWriter.writeBackupsPath(wsLogFilePathList, config);
         logWriter.writeRepositoryEntry(rEntry, repositoryServiceConfiguration);

         this.workspaceBackupsInfo = wsLogFilePathList;
         this.workspaceSystem = systemWorkspace;
      }
      catch (IOException e)
      {
         throw new BackupOperationException("Can not create backup log ...", e);
      }
      catch (XMLStreamException e)
      {
         throw new BackupOperationException("Can not create backup log ...", e);
      }
      catch (FactoryConfigurationError e)
      {
         throw new BackupOperationException("Can not create backup log ...", e);
      }
      catch (JiBXException e)
      {
         throw new BackupOperationException("Can not create backup log ...", e);
      }
   }

   /**
    * @param log
    * @throws BackupOperationException
    */
   public RepositoryBackupChainLog(File log) throws BackupOperationException
   {
      this.log = log;
      this.backupId = log.getName().replaceAll(PREFIX, "").replaceAll(SUFFIX, "");

      try
      {
         logReader = new LogReader(log);
         logReader.readLogFile();
         this.versionLog = logReader.getVersionLog();
      }
      catch (FileNotFoundException e)
      {
         throw new BackupOperationException("Can not read RepositoryBackupChainLog from file :"
                  + PrivilegedFileHelper.getAbsolutePath(log), e);
      }
      catch (XMLStreamException e)
      {
         throw new BackupOperationException("Can not read RepositoryBackupChainLog from file :"
                  + PrivilegedFileHelper.getAbsolutePath(log), e);
      }
      catch (UnsupportedEncodingException e)
      {
         throw new BackupOperationException("Can not read RepositoryBackupChainLog from file :"
                  + PrivilegedFileHelper.getAbsolutePath(log), e);
      }
      catch (Exception e)
      {
         throw new BackupOperationException("Can not read RepositoryBackupChainLog from file :"
                  + PrivilegedFileHelper.getAbsolutePath(log), e);
      }
   }

   /**
    * Getting log file path.
    *
    * @return String
    *           return the path to backup log
    */
   public String getLogFilePath()
   {
      return PrivilegedFileHelper.getAbsolutePath(log);
   }

   /**
    * Getting repository backup configuration.
    *
    * @return ReposiotoryBackupConfig
    *           return the repository backup configuration
    */
   public RepositoryBackupConfig getBackupConfig()
   {
      return config;
   }

   /**
    * Getting the started time.
    *
    * @return Calendar
    *           return the started time
    */
   public Calendar getStartedTime()
   {
      return startedTime;
   }

   /**
    * Getting the finished time.
    *
    * @return Calendar
    *           return the finished time 
    */
   public Calendar getFinishedTime()
   {
      return finishedTime;
   }

   public boolean isFinilized()
   {
      return finalized;
   }

   /**
    * Finalize log.
    *
    */
   public synchronized void endLog()
   {
      if (!finalized)
      {
         finishedTime = Calendar.getInstance();
         finalized = true;
         logWriter.writeEndLog();

         //copy backup chain log file in into Backupset files itself for portability (e.g. on another server)
         try
         {
            InputStream in = PrivilegedFileHelper.fileInputStream(log);

            File dest = new File(config.getBackupDir() + File.separator + log.getName());
            if (!PrivilegedFileHelper.exists(dest))
            {
               OutputStream out = PrivilegedFileHelper.fileOutputStream(dest);

               byte[] buf = new byte[(int) (PrivilegedFileHelper.length(log))];
               in.read(buf);

               String sConfig = new String(buf, Constants.DEFAULT_ENCODING);
               sConfig = sConfig.replaceAll("<backup-dir>.+</backup-dir>", "<backup-dir>.</backup-dir>");

               out.write(sConfig.getBytes(Constants.DEFAULT_ENCODING));

               in.close();
               out.close();
            }
         }
         catch (PatternSyntaxException e)
         {
            logger.error("Can't write log", e);
         }
         catch (FileNotFoundException e)
         {
            logger.error("Can't write log", e);
         }
         catch (IOException e)
         {
            logger.error("Can't write log", e);
         }
      }
   }

   /**
    * Getting the system workspace name.
    * 
    * @return String
    *           return the system workspace name.
    */
   public String getSystemWorkspace()
   {
      return workspaceSystem;
   }

   /**
    * Getting the workspace backups info.
    * 
    * @return Collection
    *           return the list with path to backups.
    */
   public List<String> getWorkspaceBackupsInfo()
   {
      return workspaceBackupsInfo;
   }

   /**
    * Getting the backup id.
    *
    * @return int
    *           return the backup id
    */
   public String getBackupId()
   {
      return backupId;
   }

   /**
    * Getting original repository configuration
    * 
    * @return RepositoryEntry
    *           return the original repository configuration
    */
   public RepositoryEntry getOriginalRepositoryEntry()
   {
      return originalRepositoryEntry;
   }

}
