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

import org.exoplatform.services.jcr.impl.util.JCRDateFormat;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

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

   protected static Log logger = ExoLogger.getLogger("ext.BackupChainLog");

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
    * @throws BackupOperationException
    *           will be generate the exception BackupOperationException 
    */
   public BackupChainLog(File logDir, BackupConfig config, String fullBackupType, String incrementalBackupType,
      String backupId) throws BackupOperationException
   {
      try
      {
         this.finalized = false;
         this.log = new File(logDir.getCanonicalPath() + File.separator + (PREFIX + backupId + SUFFIX));
         this.log.createNewFile();
         this.backupId = backupId;
         this.config = config;
         this.jobEntries = new ArrayList<JobEntryInfo>();

         // write config info here
         logWriter = new LogWriter(log);
         logWriter.write(config, fullBackupType, incrementalBackupType);
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

         this.config = logReader.getBackupConfig();
         this.startedTime = logReader.getBeginTime();
         this.finishedTime = logReader.getEndTime();
         this.jobEntries = logReader.getJobEntryInfoNormalizeList();

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

         logWriter.write(info);
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
      return log.getAbsolutePath();
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

   private class LogReader
   {
      protected Log logger = ExoLogger.getLogger("ext.LogWriter");

      private File logFile;

      private XMLStreamReader reader;

      private BackupConfig config;

      private List<JobEntryInfo> jobEntries;

      private List<JobEntryInfo> jobEntriesNormalize;

      public LogReader(File logFile) throws FileNotFoundException, XMLStreamException, FactoryConfigurationError
      {
         this.logFile = logFile;
         jobEntries = new ArrayList<JobEntryInfo>();

         reader = XMLInputFactory.newInstance().createXMLStreamReader(new FileInputStream(this.logFile));
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

      public void readLogFile() throws XMLStreamException, MalformedURLException, ValueFormatException
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

                  break;

               case StartElement.END_DOCUMENT :
                  endDocument = true;
                  break;
            }
         }
      }

      private JobEntryInfo readJobEntryInfo() throws XMLStreamException, MalformedURLException, ValueFormatException
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
                     info.setURL(new URL(readContent()));

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

      private BackupConfig readBackupConfig() throws XMLStreamException
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
                     conf.setBackupDir(new File(readContent()));

                  if (name.equals("repository"))
                     conf.setRepository(readContent());

                  if (name.equals("workspace"))
                     conf.setWorkspace(readContent());

                  if (name.equals("incremental-job-period"))
                     conf.setIncrementalJobPeriod(Long.valueOf(readContent()).longValue());

                  if (name.equals("incremental-job-number"))
                     conf.setIncrementalJobNumber(Integer.valueOf(readContent()).intValue());

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

      protected Log logger = ExoLogger.getLogger("ext.LogWriter");

      private File logFile;

      XMLStreamWriter writer;

      public LogWriter(File logFile) throws FileNotFoundException, XMLStreamException, FactoryConfigurationError
      {
         this.logFile = logFile;

         writer = XMLOutputFactory.newInstance().createXMLStreamWriter(new FileOutputStream(this.logFile));

         writer.writeStartDocument();
         writer.writeStartElement("backup-cain-log");
         writer.flush();
      }

      public synchronized void write(BackupConfig config, String fullBackupType, String incrementalBackupType)
         throws XMLStreamException
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
            writer.writeCharacters(config.getBackupDir().getAbsolutePath());
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

      public synchronized void write(JobEntryInfo info) throws XMLStreamException
      {
         writer.writeStartElement("job-entry-info");

         writer.writeStartElement("type");
         writer.writeCharacters((info.getType() == BackupJob.FULL ? "FULL" : "INCREMENTAL"));
         writer.writeEndElement();

         writer.writeStartElement("state");
         writer.writeCharacters(getState(info.getState()));
         writer.writeEndElement();

         writer.writeStartElement("url");
         writer.writeCharacters(info.getURL().toString());
         writer.writeEndElement();

         writer.writeStartElement("date");
         writer.writeCharacters(JCRDateFormat.format(info.getDate()));
         writer.writeEndElement();

         writer.writeEndElement();

         writer.flush();
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
}
