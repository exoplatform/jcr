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
package org.exoplatform.services.jcr.ext.backup.impl;

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.ext.backup.BackupChain;
import org.exoplatform.services.jcr.ext.backup.BackupConfig;
import org.exoplatform.services.jcr.ext.backup.BackupConfigurationException;
import org.exoplatform.services.jcr.ext.backup.BackupJobListener;
import org.exoplatform.services.jcr.ext.backup.BackupOperationException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import javax.jcr.RepositoryException;
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
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: BackupScheduler.java 760 2008-02-07 15:08:07Z pnedonosko $
 */
public class BackupScheduler
{

   public enum TaskStatus {
      VIRGIN, EXECUTED, FINISHED
   };

   protected Log log = ExoLogger.getLogger("exo.jcr.component.ext.BackupScheduler");

   private final BackupManagerImpl backup;

   private final BackupMessagesLog messages;

   private final Timer timer;

   private final List<WeakReference<SchedulerTask>> tasks = new ArrayList<WeakReference<SchedulerTask>>();

   private class TaskConfig
   {

      private final DateFormat datef = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

      BackupConfig backupConfig;

      Date startTime;

      Date stopTime;

      long chainPeriod;

      long incrPeriod;

      TaskConfig(BackupConfig backupConfig, Date startTime, Date stopTime, long chainPeriod, long incrPeriod)
      {
         this.backupConfig = backupConfig;
         this.startTime = startTime;
         this.stopTime = stopTime;
         this.chainPeriod = chainPeriod;
         this.incrPeriod = incrPeriod;
      }

      TaskConfig(File taskFile) throws IOException, XMLStreamException, FactoryConfigurationError, ParseException
      {
         read(taskFile);
      }

      Date parseDate(String dateString) throws ParseException
      {
         if ("null".equals(dateString))
            return null;
         else
            return datef.parse(dateString);
      }

      String formatDate(Date date)
      {
         return date == null ? "null" : datef.format(date);
      }

      void save(File taskFile) throws IOException, XMLStreamException, FactoryConfigurationError
      {
         TaskConfigWriter w = new TaskConfigWriter(taskFile);
         w.writeBackupConfig(backupConfig);
         w.writeSchedulerConfig(startTime, stopTime, chainPeriod, incrPeriod);
         w.writeEndLog();
      }

      void read(File taskFile) throws IOException, XMLStreamException, FactoryConfigurationError, ParseException
      {
         TaskConfigReader r = new TaskConfigReader(taskFile);
         r.readLogFile();

         // done
         this.backupConfig = r.backupConfig;
         this.startTime = r.startTime;
         this.stopTime = r.stopTime;
         this.chainPeriod = r.chainPeriod;
         this.incrPeriod = r.incrPeriod;
      }

      @Deprecated
      void read_old(File taskFile) throws IOException, ParseException, BackupSchedulerException
      {
         char[] cbuf = new char[1024];
         StringBuilder content = new StringBuilder();

         // read file
         FileReader fr = new FileReader(taskFile);
         int r = 0;
         while ((r = fr.read(cbuf)) >= 0)
         {
            content.append(cbuf, 0, r);
         };
         fr.close();

         // parse file
         String[] fc = content.toString().trim().split("\n");
         if (fc.length > 1)
         {
            File _backupLog = null;
            Date _startTime = null, _stopTime = null;
            long _chainPeriod = -1, _incrPeriod = -1;

            String[] terms = fc[fc.length - 1].split(",");
            for (int i = 0; i < terms.length; i++)
            {
               if (i == 0)
               {
                  _backupLog = new File(terms[i]);
               }
               else if (i == 1)
               {
                  _startTime = datef.parse(terms[i]);
               }
               else if (i == 2)
               {
                  String t = terms[i];
                  if (!"null".equals(t))
                     _stopTime = datef.parse(t);
               }
               else if (i == 3)
               {
                  _chainPeriod = Long.parseLong(terms[i]);
               }
               else if (i == 4)
               {
                  _incrPeriod = Long.parseLong(terms[i]);
               }
            }

            if (PrivilegedFileHelper.exists(_backupLog))
            {
               // this.backupLog = _backupLog;
               this.startTime = _startTime;
               this.stopTime = _stopTime;
               this.chainPeriod = _chainPeriod;
               this.incrPeriod = _incrPeriod;
            }
            else
               throw new BackupSchedulerException(
                  "Scheduler task skipped due to the error. Backup log file not exists "
                     + PrivilegedFileHelper.getAbsolutePath(_backupLog) + ". Task file "
                     + PrivilegedFileHelper.getAbsolutePath(taskFile));
         }
         else
            throw new BackupSchedulerException("Scheduler task skipped due to bad configured task file "
               + PrivilegedFileHelper.getAbsolutePath(taskFile) + ". File doesn't contains configuration line.");
      }

      @Deprecated
      File save_old(File taskFile) throws IOException
      {
         // File taskFile = new File(PrivilegedFileHelper.getAbsolutePath(backupLog) + ".task");
         if (!PrivilegedFileHelper.exists(taskFile))
         {
            FileWriter fw = new FileWriter(taskFile);
            fw.append("LogPath,StartTime,StopTime,ChainPeriod,IncrPeriod\n");
            fw.append(PrivilegedFileHelper.getAbsolutePath(taskFile) + "," + datef.format(startTime) + ","
               + (stopTime != null ? datef.format(stopTime) : "null") + "," + chainPeriod + "," + incrPeriod);
            fw.close();
            return taskFile;
         }

         return null;
      }

      class TaskConfigWriter
      {

         Log logger = ExoLogger.getLogger("exo.jcr.component.ext.TaskConfigWriter");

         final FileOutputStream logFile;

         final XMLStreamWriter writer;

         TaskConfigWriter(final File logFile) throws FileNotFoundException, XMLStreamException,
            FactoryConfigurationError
         {
            this.logFile = PrivilegedFileHelper.fileOutputStream(logFile);

            try
            {
               writer = SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<XMLStreamWriter>()
               {
                  public XMLStreamWriter run() throws Exception
                  {
                     return XMLOutputFactory.newInstance().createXMLStreamWriter(new FileOutputStream(logFile));
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
            writer.writeStartElement("backup-task-config");
            writer.flush();
         }

         void writeBackupConfig(BackupConfig config) throws XMLStreamException
         {
            writer.writeStartElement("backup-config");

            writer.writeStartElement("full-backup-type");
            writer.writeCharacters(backup.getFullBackupType());
            writer.writeEndElement();

            writer.writeStartElement("incremental-backup-type");
            writer.writeCharacters(backup.getIncrementalBackupType());
            writer.writeEndElement();

            if (config.getBackupDir() != null)
            {
               writer.writeStartElement("backup-dir");
               writer.writeCharacters(PrivilegedFileHelper.getAbsolutePath(config.getBackupDir()));
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

            writer.writeEndElement();

            writer.flush();
         }

         void writeSchedulerConfig(Date startTime, Date stopTime, long chainPeriod, long incrPeriod)
            throws XMLStreamException
         {
            writer.writeStartElement("scheduler-config");

            writer.writeStartElement("start-time");
            writer.writeCharacters(formatDate(startTime));
            writer.writeEndElement();

            writer.writeStartElement("stop-time");
            writer.writeCharacters(formatDate(stopTime));
            writer.writeEndElement();

            writer.writeStartElement("chain-period");
            writer.writeCharacters(String.valueOf(chainPeriod));
            writer.writeEndElement();

            writer.writeStartElement("incr-period");
            writer.writeCharacters(String.valueOf(incrPeriod));
            writer.writeEndElement();

            writer.writeEndElement();

            writer.flush();
         }

         void writeEndLog() throws XMLStreamException, IOException
         {
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();

            logFile.close();
         }
      }

      class TaskConfigReader
      {
         Log logger = ExoLogger.getLogger("exo.jcr.component.ext.TaskConfigReader");

         final FileInputStream logFile;

         final XMLStreamReader reader;

         BackupConfig backupConfig;

         Date startTime;

         Date stopTime;

         long chainPeriod;

         long incrPeriod;

         TaskConfigReader(File logFile) throws FileNotFoundException, XMLStreamException, FactoryConfigurationError
         {
            this.logFile = PrivilegedFileHelper.fileInputStream(logFile);

            this.reader = XMLInputFactory.newInstance().createXMLStreamReader(this.logFile);
         }

         void readLogFile() throws XMLStreamException, ParseException, IOException
         {
            try
            {
               while (true)
               {
                  int eventCode = reader.next();
                  switch (eventCode)
                  {

                     case StartElement.START_ELEMENT :
                        String name = reader.getLocalName();

                        if (name.equals("backup-config"))
                           readBackupConfig();

                        if (name.equals("scheduler-config"))
                           readTaskConfig();

                        break;

                     case StartElement.END_DOCUMENT :
                        return;
                  }
               }
            }
            finally
            {
               logFile.close();
            }
         }

         private void readTaskConfig() throws XMLStreamException, MalformedURLException, ParseException
         {
            boolean endJobEntryInfo = false;

            while (!endJobEntryInfo)
            {
               int eventCode = reader.next();
               switch (eventCode)
               {

                  case StartElement.START_ELEMENT :
                     String name = reader.getLocalName();

                     if (name.equals("start-time"))
                        startTime = parseDate(readContent());

                     if (name.equals("stop-time"))
                        stopTime = parseDate(readContent());

                     if (name.equals("chain-period"))
                        chainPeriod = Long.valueOf(readContent()).longValue();

                     if (name.equals("incr-period"))
                        incrPeriod = Long.valueOf(readContent()).longValue();

                     break;

                  case StartElement.END_ELEMENT :
                     String tagName = reader.getLocalName();

                     if (tagName.equals("scheduler-config"))
                        endJobEntryInfo = true;
                     break;
               }
            }
         }

         private void readBackupConfig() throws XMLStreamException
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
                        conf.setIncrementalJobPeriod(Long.valueOf(readContent()));

                     break;

                  case StartElement.END_ELEMENT :
                     String tagName = reader.getLocalName();

                     if (tagName.equals("backup-config"))
                        endBackupConfig = true;
                     break;
               }
            }

            backupConfig = conf;
         }

         // Read CHARACTERS
         private String readContent() throws XMLStreamException
         {
            String content = null;

            int eventCode = reader.next();

            if (eventCode == StartElement.CHARACTERS)
               content = reader.getText();

            return content;
         }
      }
   }

   private class TaskThread extends Thread
   {
      private final CountDownLatch latch = new CountDownLatch(1);

      public TaskThread(String name)
      {
         super(name);
      }

      void markReady()
      {
         latch.countDown();
      }

      void await() throws InterruptedException
      {
         latch.await();
      }
   }

   class CleanupTasksListTask extends TimerTask
   {

      static final int PERIOD = 60000 * 30; // 30 min

      @Override
      public void run()
      {
         // pack tasks list if an empty references found
         synchronized (tasks)
         {
            for (Iterator<WeakReference<SchedulerTask>> ti = tasks.iterator(); ti.hasNext();)
            {
               if (ti.next().get() == null)
                  // remove reference
                  ti.remove();
            }
         }
      }
   }

   private abstract class SchedulerTask extends TimerTask
   {

      protected final BackupConfig config;

      protected BackupChain chain;

      protected TaskStatus status = TaskStatus.VIRGIN;

      protected final BackupJobListener listener;

      SchedulerTask(BackupConfig config, BackupJobListener listener)
      {
         this.config = config;
         this.listener = listener;
      }

      @Override
      public String toString()
      {
         return super.toString() + "-" + getChainName();
      }

      public String getChainName()
      {
         return config.getRepository() + "@" + config.getWorkspace();
      }

      public BackupChain getChain()
      {
         return chain;
      }

      protected TaskThread stop()
      {
         TaskThread stopper = new TaskThread("BackupScheduler_Task_" + getChainName() + "-stop")
         {

            @Override
            public void run()
            {
               try
               {
                  synchronized (config)
                  {
                     backup.stopBackup(chain);
                     if (log.isDebugEnabled())
                        log.debug("Chain stopped " + chain.getLogFilePath());
                  }
               }
               finally
               {
                  markReady();
               }
            }
         };
         stopper.start();

         return stopper;
      }

      protected TaskThread start()
      {
         TaskThread starter = new TaskThread("BackupScheduler_Task_" + getChainName() + "-start")
         {

            @Override
            public void run()
            {
               try
               {
                  synchronized (config)
                  {
                     chain = backup.startBackup(config, listener);
                     if (log.isDebugEnabled())
                        log.debug("Chain satarted " + chain.getLogFilePath());
                  }
               }
               catch (BackupOperationException e)
               {
                  postError(getChainName() + " start", e);
               }
               catch (BackupConfigurationException e)
               {
                  postError(getChainName() + " start", e);
               }
               catch (RepositoryException e)
               {
                  postError(getChainName() + " start", e);
               }
               catch (RepositoryConfigurationException e)
               {
                  postError(getChainName() + " start", e);
               }
               finally
               {
                  markReady();
               }
            }
         };
         starter.start();

         return starter;
      }

      protected void postError(String message, Throwable e)
      {
         messages.addError(message, e);
         log.error(message, e);
         if (listener != null)
            listener.onError(null, message, e);
      }

      /**
       * Backup task done, i.e. stopped and unscheduled
       */
      protected TaskThread done()
      {
         TaskThread done = new TaskThread("BackupScheduler_Task_" + getChainName() + "-done")
         {
            @Override
            public void run()
            {
               try
               {
                  synchronized (config)
                  {
                     // cancel timer
                     cancel();

                     // stop the backup chain
                     backup.stopBackup(chain);

                     // remove task file config
                     removeTaskConfig();
                     if (log.isDebugEnabled())
                        log.debug("Task done (stopped and scheduler canceled) "
                           + (chain != null ? chain.getLogFilePath() : "[not started]"));
                  }
               }
               finally
               {
                  markReady();
               }
            }
         };
         done.start();

         return done;
      }

      protected void removeTaskConfig()
      {
         // remove task file config
         File taskFile =
            new File(PrivilegedFileHelper.getAbsolutePath(backup.getLogsDirectory()) + File.separator
               + config.getRepository() + "-" + config.getWorkspace() + ".task");
         if (PrivilegedFileHelper.exists(taskFile))
         {
            PrivilegedFileHelper.delete(taskFile);
            if (log.isDebugEnabled())
               log.debug("Remove scheduler task " + PrivilegedFileHelper.getAbsolutePath(taskFile));
         }
      }

      @Override
      public boolean cancel()
      {
         status = TaskStatus.FINISHED;
         if (log.isDebugEnabled())
            log.debug("Task scheduling canceled " + (chain != null ? chain.getLogFilePath() : "[not started]"));
         return super.cancel();
      }
   }

   // impl of p.3,4
   private class PeriodTask extends SchedulerTask
   {

      PeriodTask(BackupConfig config, BackupJobListener listener)
      {
         super(config, listener);
      }

      @Override
      public void run()
      {
         if (status == TaskStatus.VIRGIN)
         {
            // start
            start();
            status = TaskStatus.EXECUTED;
         }
         else if (status == TaskStatus.EXECUTED)
         {
            // stop backup and cancel scheduling
            done();
         }
         else
         {
            // do nothing or warn (shouldn't occurs)
            log.warn("Chain already task finished " + getChainName() + ", " + this);
         }
      }
   }

   // impl of p.5,6
   // &
   // impl of p7,8 (stopTime == null)
   private class PeriodicTask extends SchedulerTask
   {

      private final Date stopTime;

      PeriodicTask(BackupConfig config, Date stopTime, BackupJobListener listener)
      {
         super(config, listener);
         this.stopTime = stopTime;
      }

      @Override
      public void run()
      {
         if (chain != null && chain.isFinished())
         {
            // cancel scheduling
            cancel();
            // remove task file config
            removeTaskConfig();
         }
         else if (stopTime != null && new Date().after(stopTime))
         {
            // stop backup and cancel scheduling
            done();
         }
         else if (status == TaskStatus.VIRGIN)
         {
            // start
            start();
            status = TaskStatus.EXECUTED;
         }
         else if (status == TaskStatus.EXECUTED)
         {
            // stop current
            try
            {
               stop().await(); // wait if stop finished
            }
            catch (InterruptedException e)
            {
               postError("Can't stop task for periodic rotation ", e);
            }

            // start next
            start();
         }
         else
         {
            // do nothing or warn (shouldn't occurs)
            log.warn("Chain already task finished " + getChainName() + ", " + this);
         }
      }
   }

   // impl of p.1,2
   private class RunOnceTask extends SchedulerTask
   {

      RunOnceTask(BackupConfig config, BackupJobListener listener)
      {
         super(config, listener);
      }

      @Override
      public void run()
      {
         // start
         start();
         // remove task file config
         removeTaskConfig();
      }
   }

   BackupScheduler(BackupManagerImpl backup, BackupMessagesLog messages)
   {
      this.backup = backup;
      this.timer =
         new Timer("BackupScheduler_Timer_" + new SimpleDateFormat("yyyyMMdd.HHmmss.SSS").format(new Date()), true);

      // tasks list cleanup task
      this.timer.schedule(new CleanupTasksListTask(), CleanupTasksListTask.PERIOD, CleanupTasksListTask.PERIOD);

      this.messages = messages;

      // don't ask timer to cancel tasks
      // registerShutdownHook();
   }

   public BackupMessage[] getErrors()
   {
      return messages.getMessages();
   }

   /**
    * Restore scheduler after system shutdown etc. Called from manager at start.
    * 
    * Should restore the scheduler tasks if start and stop time are correct and will be occured in
    * future. Or should restore and to continue the tasks if start time reached but stop time in
    * future or periodic scheduling enabled for the task (including incremental configuration too).
    * 
    * @param taskFile
    * @throws ParseException
    * @throws BackupSchedulerException
    * @throws BackupOperationException
    * @throws RepositoryConfigurationException
    * @throws RepositoryException
    * @throws BackupConfigurationException
    * @throws IOException
    */
   void restore(final File taskFile) throws BackupSchedulerException, BackupOperationException,
      BackupConfigurationException, RepositoryException, RepositoryConfigurationException
   {
      try
      {
         TaskConfig tconf = new TaskConfig(taskFile);

         // check if the task is not expired
         final Date now = new Date();
         // by start time and periodic parameters
         if ((tconf.stopTime != null && tconf.stopTime.after(now)) || tconf.chainPeriod > 0 || tconf.incrPeriod > 0)
         {

            // by stop time
            // Restore without scheduler now. Add task search capabilities to the scheduler and add
            // listener to a task
            schedule(tconf.backupConfig, tconf.startTime, tconf.stopTime, tconf.chainPeriod, tconf.incrPeriod, null);
         } // else - the start time in past and no periodic configuration found

      }
      catch (IOException e)
      {
         throw new BackupSchedulerException("Can't restore scheduler from task file "
            + PrivilegedFileHelper.getAbsolutePath(taskFile), e);
      }
      catch (ParseException e)
      {
         throw new BackupSchedulerException("Can't restore scheduler from task file "
            + PrivilegedFileHelper.getAbsolutePath(taskFile), e);
      }
      catch (XMLStreamException e)
      {
         throw new BackupSchedulerException("Can't restore scheduler from task file "
            + PrivilegedFileHelper.getAbsolutePath(taskFile), e);
      }
      catch (FactoryConfigurationError e)
      {
         throw new BackupSchedulerException("Can't restore scheduler from task file "
            + PrivilegedFileHelper.getAbsolutePath(taskFile), e);
      }
   }

   /**
    * Schedule backup task with given configuration and scheduler parameters. The behaviour of a task
    * vary depending on scheduler parameters. If specified
    * <ul>
    * <li>1. startTime only - run once forever</li>
    * <li>2. startTime + incrementalPeriod - run once forever (with incremental backup)</li>
    * <li>3. startTime, endTime - run during given period</li>
    * <li>4. startTime, endTime + incrementalPeriod - run during given period (with incremental
    * backup)</li>
    * <li>5. startTime, endTime, chainPeriod - run periodic during given period</li>
    * <li>6. startTime, endTime, chainPeriod + incrementalPeriod - run periodic during given period
    * (with incremental backup)</li>
    * <li>7. startTime, chainPeriod - run periodic forever</li>
    * <li>8. startTime, chainPeriod + incrementalPeriod - run periodic forever (with incremental
    * backup)</li>
    * </ul>
    * 
    * @param config
    * @param startTime
    *          - task start time
    * @param stopTime
    *          - task stop time, may be null i.e. the task will be executed forever
    * @param chainPeriod
    *          - task chain period, means periodic execution of the configured backup chain
    * @param incrementalPeriod
    *          - incr period
    * @param listener
    *          - listener for each job produced by an each backup chain
    * @return
    * @throws BackupSchedulerException
    */
   public void schedule(BackupConfig config, Date startTime, Date stopTime, long chainPeriod, long incrementalPeriod)
      throws BackupSchedulerException
   {
      schedule(config, startTime, stopTime, chainPeriod, incrementalPeriod, null);
   }

   /**
    * Schedule backup task with given configuration and scheduler parameters. The behaviour of a task
    * vary depending on scheduler parameters. If specified
    * <ul>
    * <li>1. startTime only - run once forever</li>
    * <li>2. startTime + incrementalPeriod - run once forever (with incremental backup)</li>
    * <li>3. startTime, endTime - run during given period</li>
    * <li>4. startTime, endTime + incrementalPeriod - run during given period (with incremental
    * backup)</li>
    * <li>5. startTime, endTime, chainPeriod - run periodic during given period</li>
    * <li>6. startTime, endTime, chainPeriod + incrementalPeriod - run periodic during given period
    * (with incremental backup)</li>
    * <li>7. startTime, chainPeriod - run periodic forever</li>
    * <li>8. startTime, chainPeriod + incrementalPeriod - run periodic forever (with incremental
    * backup)</li>
    * </ul>
    * 
    * The method will return immediate, a task will be scheduled and started as independent thread.
    * 
    * @param config
    * @param startTime
    *          - task start time
    * @param stopTime
    *          - task stop time, may be null i.e. the task will be executed forever
    * @param chainPeriod
    *          - task chain period, means periodic execution of the configured backup chain
    * @param incrementalPeriod
    *          - incr period
    * @param listener
    *          - listener for each job produced by an each backup chain
    * @return
    * @throws BackupSchedulerException
    */
   public void schedule(BackupConfig config, Date startTime, Date stopTime, long chainPeriod, long incrementalPeriod,
      BackupJobListener listener) throws BackupSchedulerException
   {

      long chainPeriodMilliseconds = chainPeriod * 1000;

      if (incrementalPeriod > 0)
         config.setIncrementalJobPeriod(incrementalPeriod); // override ones from config

      SchedulerTask ctask;

      if (stopTime != null)
      {
         if (stopTime.after(startTime))
         {
            if (chainPeriodMilliseconds > 0)
            {
               ctask = new PeriodicTask(config, stopTime, listener);
               // the task will be executed each time chainPeriod exceeded and stopped at stopTime
               timer.schedule(ctask, startTime, chainPeriodMilliseconds);
            }
            else
            {
               long stopPeriod = stopTime.getTime() - startTime.getTime();
               ctask = new PeriodTask(config, listener);
               // the task will be executed twice, second execution will means stop
               timer.schedule(ctask, startTime, stopPeriod);
            }
         }
         else
            throw new BackupSchedulerException("Stop time (" + stopTime + ") should be after the start time ("
               + startTime + ")");
      }
      else
      {
         if (chainPeriodMilliseconds > 0)
         {
            ctask = new PeriodicTask(config, null, listener);
            // the task will be executed each time chainPeriod exceeded and never stopped there
            timer.schedule(ctask, startTime, chainPeriodMilliseconds);
         }
         else
         {
            ctask = new RunOnceTask(config, listener);
            // the task will executed once at given startTime
            timer.schedule(ctask, startTime);
         }
      }

      synchronized (tasks)
      {
         tasks.add(new WeakReference<SchedulerTask>(ctask));
      }

      TaskConfig tc = new TaskConfig(config, startTime, stopTime, chainPeriod, incrementalPeriod);
      try
      {
         // PrivilegedFileHelper.getAbsolutePath(backup.getLogsDirectory())
         File taskFile =
            new File(PrivilegedFileHelper.getAbsolutePath(backup.getLogsDirectory()) + File.separator
               + config.getRepository() + "-" + config.getWorkspace() + ".task");

         if (PrivilegedFileHelper.exists(taskFile))
         {
            throw new BackupSchedulerException("Task for repository '" + config.getRepository() + "' workspace '"
               + config.getWorkspace() + "' already exists. File " + PrivilegedFileHelper.getAbsolutePath(taskFile));
         }
         tc.save(taskFile); // save task config
      }
      catch (IOException e)
      {
         throw new BackupSchedulerException("Can't save scheduler task file " + e, e);
      }
      catch (XMLStreamException e)
      {
         throw new BackupSchedulerException("Can't save scheduler task file " + e, e);
      }
      catch (FactoryConfigurationError e)
      {
         throw new BackupSchedulerException("Can't save scheduler task file " + e, e);
      }
   }

   /**
    * Search task by repository and workspace names
    * 
    * @param repository
    *          - name string
    * @param workspace
    *          - name string
    * 
    * @return SchedulerTask
    */
   public SchedulerTask findTask(String repository, String workspace)
   {
      synchronized (tasks)
      {
         for (Iterator<WeakReference<SchedulerTask>> ti = tasks.iterator(); ti.hasNext();)
         {
            WeakReference<SchedulerTask> tr = ti.next();
            SchedulerTask task = tr.get();
            if (task != null && task.config.getRepository().equals(repository)
               && task.config.getWorkspace().equals(workspace))
            {
               return task;
            }
         }
      }

      return null;
   }

   /**
    * Unshedule the task scheduled before with the given configuration. The method will waits till
    * the task will be stopped.
    * 
    * @param config
    *          - configuration used for a task search
    * @return - true if task was searched and stopped ok
    * @throws BackupSchedulerException
    */
   public boolean unschedule(BackupConfig config) throws BackupSchedulerException
   {
      synchronized (tasks)
      {
         for (Iterator<WeakReference<SchedulerTask>> ti = tasks.iterator(); ti.hasNext();)
         {
            WeakReference<SchedulerTask> tr = ti.next();
            SchedulerTask task = tr.get();
            if (task != null && task.config.getRepository().equals(config.getRepository())
               && task.config.getWorkspace().equals(config.getWorkspace()))
            {
               // remove task
               try
               {
                  task.done().await();
               }
               catch (InterruptedException e)
               {
                  throw new BackupSchedulerException("Task stop operation fails " + e, e);
               }
               ti.remove();
               return true;
            }
         }
      }

      return false;
   }

   /**
    * Simple method to release the thread used by timer used in scheduler
    */
   public void cancelTimer()
   {
      timer.cancel();
   }
}
