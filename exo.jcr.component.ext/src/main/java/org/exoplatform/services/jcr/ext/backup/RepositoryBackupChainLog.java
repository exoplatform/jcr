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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.ext.backup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jcr.ValueFormatException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.StartElement;

import org.exoplatform.services.jcr.impl.util.JCRDateFormat;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 2010
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id$
 */
public class RepositoryBackupChainLog
{
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
         writer.writeStartElement("repository-backup-cain-log");
         writer.flush();
         
         writer.writeStartElement("start-time");
         writer.writeCharacters(JCRDateFormat.format(startedTime));
         writer.writeEndElement();
      }

      public void writeSystemWorkspaceName(String wsName) throws XMLStreamException
      {
         writer.writeStartElement("system-workspace");
         writer.writeCharacters(wsName);
         writer.writeEndElement();
         writer.flush();
      }

      public void writeBackupsPath(List<String> wsLogFilePathList) throws XMLStreamException
      {
         writer.writeStartElement("workspaces-backup-info");
         
         for (String path : wsLogFilePathList)
         {
            writer.writeStartElement("url");
            writer.writeCharacters(path);
            writer.writeEndElement();
         }
         
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
         catch (Exception e)
         {
            logger.error("Can't write end log", e);
         }
      }
   }
   
   private class LogReader
   {
      protected Log logger = ExoLogger.getLogger("ext.LogWriter");

      private File logFile;

      private XMLStreamReader reader;

      public LogReader(File logFile) throws FileNotFoundException, XMLStreamException, FactoryConfigurationError
      {
         this.logFile = logFile;
         reader = XMLInputFactory.newInstance().createXMLStreamReader(new FileInputStream(logFile));
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

                  if (name.equals("system-workspace"))
                     workspaceSystem = readContent();

                  if (name.equals("workspaces-backup-info"))
                     workspaceBackupsInfo = readWorkspaceBackupInfo();
                  
                  if (name.equals("start-time"))
                     startedTime = JCRDateFormat.parse(readContent());
                  
                  if (name.equals("finish-time"))
                     finishedTime = JCRDateFormat.parse(readContent());

                  break;

               case StartElement.END_DOCUMENT :
                  endDocument = true;
                  break;
            }
         }
      }
      
      private List<String> readWorkspaceBackupInfo() throws XMLStreamException
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
                     wsBackupInfo.add(readContent());

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

      private String readContent() throws XMLStreamException
      {
         String content = null;

         int eventCode = reader.next();

         if (eventCode == StartElement.CHARACTERS)
            content = reader.getText();

         return content;
      }
   }

   protected static Log logger = ExoLogger.getLogger("ext.BackupChainLog");

   private static final String PREFIX = "repository-backup-";

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

   /**
    * @param logDirectory
    * @param config
    * @param systemWorkspace
    * @param wsLogFilePathList
    * @param backupId
    * @param startTime
    * @throws BackupOperationException
    */
   public RepositoryBackupChainLog(File logDirectory, RepositoryBackupConfig config, String systemWorkspace, List<String> wsLogFilePathList,
            String backupId, Calendar startTime) throws BackupOperationException
   {
      try
      {
         this.finalized = false;
         this.log = new File(logDirectory.getCanonicalPath() + File.separator + (PREFIX + backupId + SUFFIX));
         this.log.createNewFile();
         this.backupId = backupId;
         this.config = config;
         this.startedTime = Calendar.getInstance();

         logWriter = new LogWriter(log);
         logWriter.writeSystemWorkspaceName(systemWorkspace);
         logWriter.writeBackupsPath(wsLogFilePathList);
         
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
      }
      catch (FileNotFoundException e)
      {
         throw new BackupOperationException("Can not read RepositoryBackupChainLog from file ...", e);
      }
      catch (XMLStreamException e)
      {
         throw new BackupOperationException("Can not read RepositoryBackupChainLog from file ...", e);
      }
      catch (FactoryConfigurationError e)
      {
         throw new BackupOperationException("Can not read RepositoryBackupChainLog from file ...", e);
      }
      catch (MalformedURLException e)
      {
         throw new BackupOperationException("Can not read RepositoryBackupChainLog from file ...", e);
      }
      catch (ValueFormatException e)
      {
         throw new BackupOperationException("Can not read RepositoryBackupChainLog from file ...", e);
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
      return log.getAbsolutePath();
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
   public void endLog()
   {
      finishedTime = Calendar.getInstance();
      finalized = true;
      logWriter.writeEndLog();
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

}
