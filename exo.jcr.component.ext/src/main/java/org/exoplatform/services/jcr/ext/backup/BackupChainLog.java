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
package org.exoplatform.services.jcr.ext.backup;

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.RepositoryServiceConfiguration;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.ext.backup.server.WorkspaceRestoreExeption;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.util.JCRDateFormat;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.ws.frameworks.json.JsonHandler;
import org.exoplatform.ws.frameworks.json.JsonParser;
import org.exoplatform.ws.frameworks.json.impl.BeanBuilder;
import org.exoplatform.ws.frameworks.json.impl.JsonDefaultHandler;
import org.exoplatform.ws.frameworks.json.impl.JsonParserImpl;
import org.exoplatform.ws.frameworks.json.value.JsonValue;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.StartElement;

/**
 * Created by The eXo Platform SARL .<br/>
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */
public class BackupChainLog
{

   /**
    * Start for 1.1 version log will be stored relative paths. 
    */
   protected static String VERSION_LOG_1_1 = "1.1";

   protected static Log logger = ExoLogger.getLogger("exo.jcr.component.ext.BackupChainLog");

   public static final String PREFIX = "backup-";

   private static final String SUFFIX = ".xml";

   private File log;

   private String configInfo;

   private final List<JobEntryInfo> jobEntries;

   private LogWriter logWriter;

   private LogReader logReader;

   private BackupConfig config;

   private String backupId;

   private Calendar startedTime;

   private Calendar finishedTime;

   private boolean finalized;

   private WorkspaceEntry originalWorkspaceEntry;

   private final String versionLog;

   private File rootDir;

   private String fullBackupType;

   private String incrementalBackupType;

   /**
    * BackupChainLog  constructor.
    *
    * @param logDir
    *          File, the directory to logs
    * @param config
    *          BackupConfig, the backup config
    * @param fullBackupType
    *          Sting, FQN for full backup
    * @param incrementalBackupType
    *          Sting, FQN for incremental backup
    * @param backupId
    *          String, the identifier of backup
    * @param wEntry
    *           original workspace config
    * @throws BackupOperationException
    *           will be generate the exception BackupOperationException 
    */
   public BackupChainLog(File logDir, BackupConfig config, String fullBackupType, String incrementalBackupType,
            String backupId, RepositoryServiceConfiguration repositoryServiceConfiguration, File rootDir)
            throws BackupOperationException
   {
      try
      {
         this.finalized = false;
         this.versionLog = VERSION_LOG_1_1;
         this.log =
                  new File(PrivilegedFileHelper.getCanonicalPath(logDir) + File.separator
                           + (PREFIX + backupId + SUFFIX));
         PrivilegedFileHelper.createNewFile(this.log);
         this.rootDir = rootDir;
         this.backupId = backupId;
         this.fullBackupType = fullBackupType;
         this.incrementalBackupType = incrementalBackupType;
         this.config = config;
         this.jobEntries = new ArrayList<JobEntryInfo>();

         this.originalWorkspaceEntry = getWorkspaceEntry(config, repositoryServiceConfiguration);

         // write config info here
         logWriter = new LogWriter(log);
         logWriter.write(config, fullBackupType, incrementalBackupType);
         logWriter.writeWorkspaceEntry(originalWorkspaceEntry, repositoryServiceConfiguration);
      }
      catch (IOException e)
      {
         throw new BackupOperationException(e);
      }
      catch (XMLStreamException e)
      {
         throw new BackupOperationException(e);
      }
      catch (FactoryConfigurationError e)
      {
         throw new BackupOperationException(e);
      }
      catch (JiBXException e)
      {
         throw new BackupOperationException(e);
      }
      catch (RepositoryException e)
      {
         throw new BackupOperationException(e);
      }
      catch (RepositoryConfigurationException e)
      {
         throw new BackupOperationException(e);
      }
   }

   private WorkspaceEntry getWorkspaceEntry(BackupConfig config,
            RepositoryServiceConfiguration repositoryServiceConfiguration) throws BackupOperationException
   {
      RepositoryEntry repository = null;
      try
      {
         repository = repositoryServiceConfiguration.getRepositoryConfiguration(config.getRepository());
      }
      catch (RepositoryConfigurationException e)
      {
         throw new BackupOperationException("Can not get repository \"" + config.getRepository() + "\"", e);
      }

      WorkspaceEntry wEntry = null;

      for (WorkspaceEntry entry : repository.getWorkspaceEntries())
      {
         if (entry.getName().equals(config.getWorkspace()))
         {
            wEntry = entry;
            break;
         }
      }

      if (wEntry == null)
      {
         throw new BackupOperationException("Worksapce \"" + config.getWorkspace()
                  + "\" was not exsisted in repository \"" + repository.getName() + "\".");
      }

      return wEntry;
   }

   /**
    * BackupChainLog  constructor.
    *
    * @param log
    *          File, the backup log
    * @throws BackupOperationException
    *           will be generate  the BackupOperationException 
    */
   public BackupChainLog(File log) throws BackupOperationException
   {
      this.log = log;
      this.backupId = log.getName().replaceAll(PREFIX, "").replaceAll(SUFFIX, "");

      try
      {
         logReader = new LogReader(log);
         logReader.readLogFile();
         logReader.jobEntrysNormalize();

         this.versionLog = logReader.getVersionLog();
         this.config = logReader.getBackupConfig();
         this.startedTime = logReader.getBeginTime();
         this.finishedTime = logReader.getEndTime();
         this.jobEntries = logReader.getJobEntryInfoNormalizeList();
         this.originalWorkspaceEntry = logReader.getOriginalWorkspaceEntry();
         this.fullBackupType = logReader.getFullBackupType();
         this.incrementalBackupType = logReader.getIncrementalBackupType();

         for (JobEntryInfo info : jobEntries)
         {
            if (info.getType() == BackupJob.INCREMENTAL)
            {
               config.setBackupType(BackupManager.FULL_AND_INCREMENTAL);
               break;
            }
         }
      }
      catch (FileNotFoundException e)
      {
         throw new BackupOperationException(e);
      }
      catch (XMLStreamException e)
      {
         throw new BackupOperationException(e);
      }
      catch (FactoryConfigurationError e)
      {
         throw new BackupOperationException(e);
      }
      catch (MalformedURLException e)
      {
         throw new BackupOperationException(e);
      }
      catch (ValueFormatException e)
      {
         throw new BackupOperationException(e);
      }
      catch (Exception e)
      {
         throw new BackupOperationException(e);
      }
   }

   /**
    * Adding the the backup job.
    *
    * @param job 
    *          BackupJob, the backup job
    */
   public void addJobEntry(BackupJob job)
   {
      // jobEntries
      try
      {
         JobEntryInfo info = new JobEntryInfo();
         info.setDate(Calendar.getInstance());
         info.setType(job.getType());
         info.setState(job.getState());
         info.setURL(job.getStorageURL());

         logWriter.write(info, config);
      }
      catch (Exception e)
      {
         logger.error("Can't add job", e);
      }
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
    * Getting the config info.
    *
    * @return String 
    *           return the config info
    */
   public String getConfigInfo()
   {
      return configInfo;
   }

   /**
    * Getting the job informations.
    *
    * @return List
    *           return the job informations.
    */
   public List<JobEntryInfo> getJobEntryInfos()
   {
      return jobEntries;
   }

   /**
    * Finalize log.
    *
    */
   public void endLog()
   {
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
      catch (Exception e)
      {
         logger.error("Can't write log", e);
      }

   }

   /**
    * Getting the states for jobs.
    *
    * @return Collection
    *           return the collection of states for jobs
    * 
    */
   public Collection<JobEntryInfo> getJobEntryStates()
   {
      HashMap<Integer, JobEntryInfo> infos = new HashMap<Integer, JobEntryInfo>();
      for (JobEntryInfo jobEntry : jobEntries)
      {
         infos.put(jobEntry.getID(), jobEntry);
      }
      return infos.values();
   }

   /**
    * Getting backup config.
    *
    * @return BackupConfig
    *           return the backup config
    */
   public BackupConfig getBackupConfig()
   {
      return config;
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

   /**
    * Getting original workspace configuration
    * 
    * @return WorkspaceEntry
    *           return the original workspace configuration
    */
   public WorkspaceEntry getOriginalWorkspaceEntry()
   {
      return originalWorkspaceEntry;
   }

   private class LogReader
   {
      protected Log logger = ExoLogger.getLogger("exo.jcr.component.ext.LogReader");

      private File logFile;

      private XMLStreamReader reader;

      private BackupConfig config;

      private List<JobEntryInfo> jobEntries;

      private List<JobEntryInfo> jobEntriesNormalize;

      private WorkspaceEntry originalWorkspaceEntry;

      private String version;

      private String iBackupType;

      private String fBackupType;

      public LogReader(File logFile) throws FileNotFoundException, XMLStreamException, FactoryConfigurationError
      {
         this.logFile = logFile;
         jobEntries = new ArrayList<JobEntryInfo>();

         reader =
                  XMLInputFactory.newInstance().createXMLStreamReader(
                           PrivilegedFileHelper.fileInputStream(this.logFile), Constants.DEFAULT_ENCODING);
      }

      public String getIncrementalBackupType()
      {
         return iBackupType;
      }

      public String getFullBackupType()
      {
         return fBackupType;
      }

      public String getVersionLog()
      {
         return version;
      }

      public WorkspaceEntry getOriginalWorkspaceEntry()
      {
         return originalWorkspaceEntry;
      }

      public BackupConfig getBackupConfig()
      {
         return config;
      }

      public List<JobEntryInfo> getJobEntryInfoList()
      {
         return jobEntries;
      }

      public List<JobEntryInfo> getJobEntryInfoNormalizeList()
      {
         return jobEntriesNormalize;
      }

      public Calendar getBeginTime()
      {
         return jobEntries.get(0).getDate();
      }

      public Calendar getEndTime()
      {
         return jobEntries.get(jobEntries.size() - 1).getDate();
      }

      public void readLogFile() throws Exception
      {
         boolean endDocument = false;

         while (!endDocument)
         {
            int eventCode = reader.next();
            switch (eventCode)
            {

               case StartElement.START_ELEMENT :
                  String name = reader.getLocalName();

                  if (name.equals("backup-config"))
                     config = readBackupConfig();

                  if (name.equals("job-entry-info"))
                     jobEntries.add(readJobEntryInfo());

                  if (name.equals("original-workspace-config"))
                     this.originalWorkspaceEntry = readWorkspaceEntry();

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

      private WorkspaceEntry readWorkspaceEntry() throws Exception
      {
         String configName = readContent();

         File configFile =
                  new File(PrivilegedFileHelper.getCanonicalPath(getBackupConfig().getBackupDir()) + File.separator
                           + configName);

         if (!PrivilegedFileHelper.exists(configFile))
         {
            throw new WorkspaceRestoreExeption("The backup set is not contains original workspace configuration : "
                     + PrivilegedFileHelper.getCanonicalPath(getBackupConfig().getBackupDir()));
         }

         IBindingFactory factory = BindingDirectory.getFactory(RepositoryServiceConfiguration.class);
         IUnmarshallingContext uctx = factory.createUnmarshallingContext();
         RepositoryServiceConfiguration conf =
                  (RepositoryServiceConfiguration) uctx.unmarshalDocument(PrivilegedFileHelper
                           .fileInputStream(configFile), null);

         RepositoryEntry repositoryEntry = conf.getRepositoryConfiguration(getBackupConfig().getRepository());

         if (repositoryEntry.getWorkspaceEntries().size() != 1)
         {
            throw new WorkspaceRestoreExeption(
                     "The oririginal configuration should be contains only one workspace entry :"
                     + PrivilegedFileHelper.getCanonicalPath(configFile));
         }

         if (!repositoryEntry.getWorkspaceEntries().get(0).getName().equals(getBackupConfig().getWorkspace()))
         {
            throw new WorkspaceRestoreExeption(
                     "The oririginal configuration should be contains only one workspace entry with name \""
                              + getBackupConfig().getWorkspace() + "\" :"
                              + PrivilegedFileHelper.getCanonicalPath(configFile));
         }

         return repositoryEntry.getWorkspaceEntries().get(0);
      }

      /**
       * Will be created the Object from JSON binary data.
       * 
       * @param cl
       *          Class
       * @param data
       *          binary data (JSON)
       * @return Object
       * @throws Exception
       *           will be generated Exception
       */
      private Object getObject(Class cl, byte[] data) throws Exception
      {
         JsonHandler jsonHandler = new JsonDefaultHandler();
         JsonParser jsonParser = new JsonParserImpl();
         InputStream inputStream = new ByteArrayInputStream(data);
         jsonParser.parse(inputStream, jsonHandler);
         JsonValue jsonValue = jsonHandler.getJsonObject();

         return new BeanBuilder().createObject(cl, jsonValue);
      }

      private JobEntryInfo readJobEntryInfo() throws XMLStreamException, ValueFormatException, IOException
      {
         JobEntryInfo info = new JobEntryInfo();

         boolean endJobEntryInfo = false;

         while (!endJobEntryInfo)
         {
            int eventCode = reader.next();
            switch (eventCode)
            {

               case StartElement.START_ELEMENT :
                  String name = reader.getLocalName();

                  if (name.equals("type"))
                     info.setType(getType(readContent()));

                  if (name.equals("state"))
                     info.setState(getState(readContent()));

                  if (name.equals("url"))
                  {
                     if (version != null && version.equals(VERSION_LOG_1_1))
                     {
                        String path =
                                 readContent().replace(
                                          "file:",
                                          "file:" + PrivilegedFileHelper.getCanonicalPath(config.getBackupDir())
                                                   + File.separator);

                        info.setURL(new URL(path));
                     }
                     else
                     {
                        info.setURL(new URL(readContent()));
                     }
                  }

                  if (name.equals("date"))
                     info.setDate(JCRDateFormat.parse(readContent()));

                  break;

               case StartElement.END_ELEMENT :
                  String tagName = reader.getLocalName();

                  if (tagName.equals("job-entry-info"))
                     endJobEntryInfo = true;
                  break;
            }
         }

         return info;
      }

      private int getState(String content)
      {
         int state = -1;

         if (content.equals("FINISHED"))
            state = BackupJob.FINISHED;

         if (content.equals("STARTING"))
            state = BackupJob.STARTING;

         if (content.equals("WAITING"))
            state = BackupJob.WAITING;

         if (content.equals("WORKING"))
            state = BackupJob.WORKING;

         return state;
      }

      private int getType(String content)
      {
         int type = -1;

         if (content.equals("FULL"))
            type = BackupJob.FULL;

         if (content.equals("INCREMENTAL"))
            type = BackupJob.INCREMENTAL;

         return type;
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
                        else
                        {
                           conf.setBackupDir(new File(dir));
                        }
                     }
                     else
                     {
                        conf.setBackupDir(new File(readContent()));
                     }
                  }

                  if (name.equals("repository"))
                     conf.setRepository(readContent());

                  if (name.equals("workspace"))
                     conf.setWorkspace(readContent());

                  if (name.equals("incremental-job-period"))
                     conf.setIncrementalJobPeriod(Long.valueOf(readContent()).longValue());

                  if (name.equals("incremental-job-number"))
                     conf.setIncrementalJobNumber(Integer.valueOf(readContent()).intValue());

                  if (name.equals("full-backup-type"))
                     this.fBackupType = readContent();

                  if (name.equals("incremental-backup-type"))
                     this.iBackupType = readContent();

                  break;

               case StartElement.END_ELEMENT :
                  String tagName = reader.getLocalName();

                  if (tagName.equals("backup-config"))
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

      public void jobEntrysNormalize()
      {
         jobEntriesNormalize = new ArrayList<JobEntryInfo>();

         for (int i = 0; i < jobEntries.size(); i++)
         {
            JobEntryInfo entryInfo = jobEntries.get(i);

            boolean alreadyExist = false;

            for (int j = 0; j < jobEntriesNormalize.size(); j++)
               if (jobEntriesNormalize.get(j).getURL().toString().equals(entryInfo.getURL().toString()))
                  alreadyExist = true;

            if (!alreadyExist)
               jobEntriesNormalize.add(entryInfo);
         }
      }
   }

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
               throw (FileNotFoundException) cause;
            }
            else if (cause instanceof XMLStreamException)
            {
               throw (XMLStreamException) cause;
            }
            else if (cause instanceof FactoryConfigurationError)
            {
               throw (FactoryConfigurationError) cause;
            }
            else if (cause instanceof RuntimeException)
            {
               throw (RuntimeException) cause;
            }
            else
            {
               throw new RuntimeException(cause);
            }
         };

         writer.writeStartDocument();
         writer.writeStartElement("backup-chain-log");

         writer.writeStartElement("version-log");
         writer.writeCharacters(versionLog);
         writer.writeEndElement();

         writer.flush();
      }

      public void writeWorkspaceEntry(WorkspaceEntry originalWorkspaceEntry,
               RepositoryServiceConfiguration serviceConfiguration) throws XMLStreamException, IOException,
               JiBXException, RepositoryException, RepositoryConfigurationException
      {
         File config =
                  new File(PrivilegedFileHelper.getCanonicalPath(BackupChainLog.this.config.getBackupDir())
                           + File.separator + "original-workspace-config.xml");
         PrivilegedFileHelper.createNewFile(config);
         OutputStream saveStream = PrivilegedFileHelper.fileOutputStream(config);

         RepositoryEntry baseRepositoryEntry =
                  serviceConfiguration.getRepositoryConfiguration(BackupChainLog.this.config.getRepository());

         RepositoryEntry repositoryEntry = new RepositoryEntry();
         repositoryEntry.addWorkspace(originalWorkspaceEntry);
         repositoryEntry.setSystemWorkspaceName(baseRepositoryEntry.getSystemWorkspaceName());
         repositoryEntry.setAccessControl(baseRepositoryEntry.getAccessControl());
         repositoryEntry.setAuthenticationPolicy(baseRepositoryEntry.getAuthenticationPolicy());
         repositoryEntry.setDefaultWorkspaceName(baseRepositoryEntry.getDefaultWorkspaceName());
         repositoryEntry.setName(baseRepositoryEntry.getName());
         repositoryEntry.setSecurityDomain(baseRepositoryEntry.getSecurityDomain());
         repositoryEntry.setSessionTimeOut(baseRepositoryEntry.getSessionTimeOut());

         ArrayList<RepositoryEntry> repositoryEntries = new ArrayList<RepositoryEntry>();
         repositoryEntries.add(repositoryEntry);

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

         writer.writeStartElement("original-workspace-config");
         writer.writeCharacters(config.getName());
         writer.writeEndElement();
      }

      public synchronized void write(BackupConfig config, String fullBackupType, String incrementalBackupType)
               throws XMLStreamException, IOException
      {
         writer.writeStartElement("backup-config");

         writer.writeStartElement("full-backup-type");
         writer.writeCharacters(fullBackupType);
         writer.writeEndElement();

         writer.writeStartElement("incremental-backup-type");
         writer.writeCharacters(incrementalBackupType);
         writer.writeEndElement();

         if (config.getBackupDir() != null)
         {
            writer.writeStartElement("backup-dir");
            String path =
                     (isRootBackupManagerDir(logFile) ? PrivilegedFileHelper.getCanonicalPath(config.getBackupDir())
                              : ".");

            writer.writeCharacters(path);
            writer.writeEndElement();
         }

         if (config.getRepository() != null)
         {
            writer.writeStartElement("repository");
            writer.writeCharacters(config.getRepository());
            writer.writeEndElement();
         }

         if (config.getWorkspace() != null)
         {
            writer.writeStartElement("workspace");
            writer.writeCharacters(config.getWorkspace());
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

      public synchronized void write(JobEntryInfo info, BackupConfig config) throws XMLStreamException, IOException
      {
         writer.writeStartElement("job-entry-info");

         writer.writeStartElement("type");
         writer.writeCharacters((info.getType() == BackupJob.FULL ? "FULL" : "INCREMENTAL"));
         writer.writeEndElement();

         writer.writeStartElement("state");
         writer.writeCharacters(getState(info.getState()));
         writer.writeEndElement();

         writer.writeStartElement("url");
         writer.writeCharacters(getRelativeUrl(info.getURL(), config.getBackupDir()));
         writer.writeEndElement();

         writer.writeStartElement("date");
         writer.writeCharacters(JCRDateFormat.format(info.getDate()));
         writer.writeEndElement();

         writer.writeEndElement();

         writer.flush();
      }

      private String getRelativeUrl(URL url, File backupDir) throws IOException
      {
         String str = PrivilegedFileHelper.getCanonicalPath(new File(url.getFile()));

         return url.getProtocol() + ":"
                  + str.replace(PrivilegedFileHelper.getCanonicalPath(config.getBackupDir()) + File.separator, "");
      }

      private boolean isRootBackupManagerDir(File log) throws IOException
      {
         return (PrivilegedFileHelper.getCanonicalPath(log.getParentFile()).equals(PrivilegedFileHelper
                  .getCanonicalPath(rootDir)));
      }

      public synchronized void writeEndLog()
      {
         try
         {
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
         }
         catch (Exception e)
         {
            logger.error("Can't write log", e);
         }
      }

      private String getState(int iState)
      {
         String sState = "" + iState;
         switch (iState)
         {
            case BackupJob.FINISHED :
               sState = "FINISHED";
               break;
            case BackupJob.STARTING :
               sState = "STARTING";
               break;
            case BackupJob.WAITING :
               sState = "WAITING";
               break;
            case BackupJob.WORKING :
               sState = "WORKING";
               break;
         }

         return sState;
      }
   }

   public boolean isFinilized()
   {
      return finalized;
   }

   public String getFullBackupType()
   {
      return fullBackupType;
   }

   public String getIncrementalBackupType()
   {
      return incrementalBackupType;
   }
}
