/*
 * Copyright (C) 2011 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.checker;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.annotations.ManagedName;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.impl.AbstractRepositorySuspender;
import org.exoplatform.services.jcr.impl.core.lock.cacheable.AbstractCacheableLockManager;
import org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeDataManagerImpl;
import org.exoplatform.services.jcr.impl.core.query.SearchManager;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainerChecker;
import org.exoplatform.services.jcr.storage.value.ValueStoragePluginProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

import java.io.IOException;
import java.security.PrivilegedAction;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jcr.RepositoryException;

/**
 * Repository check controller allows check jcr repository consistency:
 * <ul>
 *  <li>Check DB consistency</li>
 *  <li>Check value storage</li>
 *  <li>Check index</li>
 * </ul>
 * 
 * @author <a href="mailto:skarpenko@exoplatform.com">Sergiy Karpenko</a>
 * @version $Id: RepositoryCheckController.java 34360 3.10.2011 skarpenko $
 */
@Managed
@NameTemplate(@Property(key = "service", value = "RepositoryCheckController"))
public class RepositoryCheckController extends AbstractRepositorySuspender implements Startable
{
   /**
    * Logger.
    */
   protected static Log LOG = ExoLogger.getLogger("exo.jcr.component.core.RepositoryCheckController");

   public static final String REPORT_CONSISTENT_MESSAGE = "Repository data is consistent";

   public static final String REPORT_NOT_CONSISTENT_MESSAGE = "Repository data is NOT consistent";

   public static final String EXCEPTION_DURING_CHECKING_MESSAGE = "Exception occured during consistency checking";

   public static final String CONFIRMATION_FAILED_MESSAGE =
      "For starting auto-repair function please enter \"YES\" as method parameter";

   /**
    * The list of available storages for checking.
    */
   public enum DataStorage {
      DB, VALUE_STORAGE, LUCENE_INDEX
   };

   /**
    * Store the results of last checking.
    */
   protected InspectionReport lastReport;

   /**
    * RepositoryCheckController constructor.
    */
   public RepositoryCheckController(ManageableRepository repository)
   {
      super(repository);
   }

   /**
    * This method will make next steps:
    * <ul>
    *  <li>Suspend repository</li>
    *  <li>Check DB consistency</li>
    *  <li>Check value storage</li>
    *  <li>Check index</li>
    *  <li>Resume repository</li>
    * </ul>
    * 
    * @return String check consistency report
    */
   @Managed
   @ManagedDescription("Check repository data consistency. DB data, value storage and lucene index will be checked.")
   public String checkAll()
   {
      return checkAll(1);
   }

   @Managed
   @ManagedDescription("Check repository data consistency. DB data, value storage and lucene index will be checked. "
      + "Set nThreads parameter to configure the number of threads.")
   public String checkAll(@ManagedName("nThreads") int nThreads)
   {
      return checkAndRepair(new DataStorage[]{DataStorage.DB, DataStorage.VALUE_STORAGE, DataStorage.LUCENE_INDEX},
         false, nThreads);
   }

   @Managed
   @ManagedDescription("Check repository database consistency.")
   public String checkDataBase()
   {
      return checkDataBase(1);
   }

   @Managed
   @ManagedDescription("Check repository database consistency. "
      + "Set nThreads parameter to configure the number of threads.")
   public String checkDataBase(@ManagedName("nThreads") int nThreads)
   {
      return checkAndRepair(new DataStorage[]{DataStorage.DB}, false, nThreads);
   }

   @Managed
   @ManagedDescription("Check repository value storage consistency.")
   public String checkValueStorage()
   {
      return checkValueStorage(1);
   }

   @Managed
   @ManagedDescription("Check repository value storage consistency. "
      + "Set nThreads parameter to configure the number of threads")
   public String checkValueStorage(@ManagedName("nThreads") int nThreads)
   {
      return checkAndRepair(new DataStorage[]{DataStorage.VALUE_STORAGE}, false, nThreads);
   }

   @Managed
   @ManagedDescription("Check repository search index consistency.")
   public String checkIndex()
   {
      return checkIndex(1);
   }

   @Managed
   @ManagedDescription("Check repository search index consistency. "
      + "Set nThreads parameter to configure the number of threads")
   public String checkIndex(@ManagedName("nThreads") int nThreads)
   {
      return checkAndRepair(new DataStorage[]{DataStorage.LUCENE_INDEX}, false, nThreads);
   }

   @Managed
   @ManagedDescription("Auto-repair inconsistencies for value storage. "
      + "Don't forget to backup your data first. Set confirmation parameter to \"YES\" for enabling auto-repair feature.")
   public String repairValueStorage(@ManagedName("confirmation") String confirmation)
   {
      return repairValueStorage(confirmation,1);
   }

   @Managed
   @ManagedDescription("Auto-repair inconsistencies for value storage. "
      + "Don't forget to backup your data first. Set confirmation parameter to \"YES\" for enabling auto-repair feature. "
      + "Set nThreads parameter to configure the number of threads")
   public String repairValueStorage(@ManagedName("confirmation") String confirmation, @ManagedName("nThreads") int nThreads)
   {
      if (confirmation.equalsIgnoreCase("YES"))
      {
         return checkAndRepair(new DataStorage[]{DataStorage.VALUE_STORAGE}, true, nThreads);
      }
      else
      {
         return CONFIRMATION_FAILED_MESSAGE;
      }
   }

   @Managed
   @ManagedDescription("Auto-repair inconsistencies for database. "
      + "Don't forget to backup your data first. Set confirmation parameter to \"YES\" for enabling auto-repair feature.")
   public String repairDataBase(@ManagedName("confirmation") String confirmation)
   {
      return repairDataBase(confirmation, 1);
   }

   @Managed
   @ManagedDescription("Auto-repair inconsistencies for database. "
      + "Don't forget to backup your data first. Set confirmation parameter to \"YES\" for enabling auto-repair feature. "
      + "Set nThreads parameter to configure the number of threads.")
   public String repairDataBase(@ManagedName("confirmation") String confirmation, @ManagedName("nThreads") int nThreads)
   {
      if (confirmation.equalsIgnoreCase("YES"))
      {
         return checkAndRepair(new DataStorage[]{DataStorage.DB}, true, nThreads);
      }
      else
      {
         return CONFIRMATION_FAILED_MESSAGE;
      }
   }

   public String checkAndRepair(final DataStorage[] storages, final boolean autoRepair, final int nThreads)
   {
      return SecurityHelper.doPrivilegedAction(new PrivilegedAction<String>()
      {
         public String run()
         {
            return checkAndRepairAction(storages, autoRepair, nThreads);
         }
      });
   }

   /**
    * @return absolute path to report or null if it doesn't exist.
    */
   public String getLastReportPath()
   {
      return lastReport != null ? lastReport.getReportPath() : null;
   }

   protected String checkAndRepairAction(DataStorage[] storages, boolean autoRepair, int nThreads)
   {
      try
      {
         createNewReport();
      }
      catch (IOException e)
      {
         return getExceptionDuringCheckingMessage(e);
      }

      try
      {
         suspendRepository();
         if(nThreads > 1)
         {
            try
            {
               MultithreadedChecking checking = new MultithreadedChecking(storages, autoRepair, nThreads);
               return checking.startThreads();
            }
            catch (IOException e)
            {
               return getExceptionDuringCheckingMessage(e);
            }
         }
         else
         {
            lastReport.init(false);
            return doCheckAndRepair(storages, autoRepair);
         }
      }
      catch (RepositoryException e)
      {
         return getExceptionDuringCheckingMessage(e);
      }
      finally
      {
         resumeRepository();
         closeReport();
      }
   }

   /**
    * {@inheritDoc}
    */
   protected void resumeRepository()
   {
      try
      {
         super.resumeRepository();
      }
      catch (RepositoryException e)
      {
         LOG.error("Can not resume repository. Error: " + e.getMessage(), e);
      }
   }

   private String doCheckAndRepair(DataStorage[] storages, boolean autoRepair)
   {
      try
      {
         doCheckAndRepair(storages, autoRepair, null);
         return logAndGetCheckingResultMessage();
      }
      catch (Throwable e) //NOSONAR
      {
         return logAndGetExceptionDuringCheckingMessage(e);
      }
   }

   private void doCheckAndRepair(DataStorage[] storages, boolean autoRepair, Queue<Callable<Void>> tasks) throws IOException, RepositoryException
   {
      for (DataStorage storage : storages)
      {
         switch (storage)
         {
            case DB :
               doCheckDataBase(autoRepair,tasks);
               break;

            case VALUE_STORAGE :
               doCheckValueStorage(autoRepair,tasks);
               break;

            case LUCENE_INDEX :
               doCheckIndex(autoRepair,tasks);
               break;
         }
      }
   }

   private String logAndGetCheckingResultMessage()
   {
      if (lastReport.hasInconsistency())
      {
         logComment(REPORT_NOT_CONSISTENT_MESSAGE);
         return REPORT_NOT_CONSISTENT_MESSAGE + getPathToReportMessage();
      }
      else
      {
         logComment(REPORT_CONSISTENT_MESSAGE);
         return REPORT_CONSISTENT_MESSAGE + getPathToReportMessage();
      }
   }

   private String logAndGetExceptionDuringCheckingMessage(Throwable e)
   {
      logExceptionAndSetInconsistency(EXCEPTION_DURING_CHECKING_MESSAGE, e);
      return getExceptionDuringCheckingMessage(e) + getPathToReportMessage();
   }

   private String getExceptionDuringCheckingMessage(Throwable e)
   {
      return EXCEPTION_DURING_CHECKING_MESSAGE + ": " + e.getMessage();
   }

   private void logComment(String message)
   {
      try
      {
         lastReport.logComment(message);
      }
      catch (IOException e)
      {
         LOG.error(e.getMessage(), e);
      }
   }

   private void logExceptionAndSetInconsistency(String message, Throwable e)
   {
      try
      {
         lastReport.logExceptionAndSetInconsistency(message, e);
      }
      catch (IOException e1)
      {
         LOG.error(e1.getMessage(), e1);
      }
   }

   private void createNewReport() throws IOException
   {
      lastReport = new InspectionReport(repository.getConfiguration().getName());
   }

   private void closeReport()
   {
      try
      {
         lastReport.close();
      }
      catch (IOException e)
      {
         LOG.error(e.getMessage(), e);
      }
   }

   private void doCheckDataBase(final boolean autoRepair,Queue<Callable<Void>> tasks) throws IOException
   {
      for (final String wsName : repository.getWorkspaceNames())
      {
         if(tasks != null)
         {
            Callable<Void> task = new Callable<Void>()
            {
               public Void call() throws Exception
               {
                  checkDatabase(autoRepair,wsName);
                  return null;
               }
            };
            tasks.offer(task);
         }
         else
         {
            checkDatabase(autoRepair, wsName);
         }
      }
   }

   private void checkDatabase(boolean autoRepair, String wsName)
   {
      logComment("Check DB consistency. Workspace " + wsName);
      JDBCWorkspaceDataContainerChecker jdbcChecker = getJDBCChecker(wsName);
      jdbcChecker.checkDataBase(autoRepair);
      jdbcChecker.checkLocksInDataBase(autoRepair);
   }

   private void doCheckValueStorage(final boolean autoRepair,Queue<Callable<Void>> tasks) throws IOException
   {
      for (final String wsName : repository.getWorkspaceNames())
      {
         if(tasks != null)
         {
            Callable<Void> task = new Callable<Void>()
            {
               public Void call() throws Exception
               {
                  checkValueStorage(autoRepair,wsName);
                  return null;
               }
            };
            tasks.offer(task);
         }
         else
         {
            checkValueStorage(autoRepair, wsName);
         }
      }
   }

   private void checkValueStorage(boolean autoRepair, String wsName)
   {
      logComment("Check ValueStorage consistency. Workspace " + wsName);
      getJDBCChecker(wsName).checkValueStorage(autoRepair);
   }

   private void doCheckIndex(boolean autoRepair,Queue<Callable<Void>> tasks) throws RepositoryException, IOException
   {
      for (final String wsName : repository.getWorkspaceNames())
      {
         if (tasks != null)
         {
            Callable<Void> task = new Callable<Void>()
            {
               public Void call() throws Exception
               {
                  checkIndex(wsName);
                  return null;
               }
            };
            tasks.offer(task);
         }
         else
         {
            checkIndex(wsName);
         }
      }
   }

   private void checkIndex(String wsName) throws IOException, RepositoryException
   {
      final String systemWS = repository.getConfiguration().getSystemWorkspaceName();
      logComment("Check SearchIndex consistency. Workspace " + wsName);
      SearchManager searchManager = (SearchManager)getComponent(SearchManager.class, wsName);
      searchManager.checkIndex(lastReport, systemWS.equals(wsName));
   }

   private JDBCWorkspaceDataContainerChecker getJDBCChecker(String wsName)
   {
      JDBCWorkspaceDataContainer dataContainer =
         (JDBCWorkspaceDataContainer)getComponent(JDBCWorkspaceDataContainer.class, wsName);

      AbstractCacheableLockManager lockManager =
         (AbstractCacheableLockManager)getComponent(AbstractCacheableLockManager.class, wsName);

      ValueStoragePluginProvider vsPlugin =
         (ValueStoragePluginProvider)getComponent(ValueStoragePluginProvider.class, wsName);

      WorkspaceEntry wsEntry = (WorkspaceEntry)getComponent(WorkspaceEntry.class, wsName);

      NodeTypeDataManagerImpl nodeTypeManager =
         (NodeTypeDataManagerImpl)getComponent(NodeTypeDataManagerImpl.class, wsName);

      return new JDBCWorkspaceDataContainerChecker(dataContainer, lockManager, vsPlugin, wsEntry, nodeTypeManager,
         lastReport);
   }

   private Object getComponent(Class forClass, String wsName)
   {
      return repository.getWorkspaceContainer(wsName).getComponent(forClass);
   }

   private String getPathToReportMessage()
   {
      return ". See full report by path " + lastReport.getReportPath();
   }

   /**
    * {@inheritDoc}
    */
   public void start()
   {
   }

   /**
    * {@inheritDoc}
    */
   public void stop()
   {
   }

   private class MultithreadedChecking
   {
      /**
       * The total amount of threads currently working
       */
      private final AtomicInteger runningThreads = new AtomicInteger();

      /**
       * The {@link java.util.concurrent.CountDownLatch} used to notify that the checking is over
       */
      private final CountDownLatch endSignal;

      /**
       * All the checking threads
       */
      private final Thread[] allCheckingThreads;

      /**
       * The list of checking tasks left to do
       */
      private Queue<Callable<Void>> tasks;

      /**
       * The task that all the checking threads have to execute
       */
      private final Runnable checkingTask = new Runnable()
      {
         public void run()
         {
            while (!Thread.currentThread().isInterrupted())
            {
               Callable<Void> task;
               while ((task = tasks.poll()) != null)
               {
                  try
                  {
                     lastReport.init(true);
                     task.call();
                  }
                  catch (InterruptedException e)
                  {
                     Thread.currentThread().interrupt();
                  }
                  catch (Exception e)
                  {
                     logAndGetExceptionDuringCheckingMessage(e);
                  }
                  finally
                  {
                     try
                     {
                        lastReport.flush();
                     }
                     catch (IOException e)
                     {
                        LOG.error(e.getMessage(), e);
                     }
                     synchronized (runningThreads)
                     {
                        runningThreads.decrementAndGet();
                        runningThreads.notifyAll();
                     }
                  }
               }
               synchronized (runningThreads)
               {
                  if (!Thread.currentThread().isInterrupted()  && (runningThreads.get() > 0))
                  {
                     try
                     {
                        runningThreads.wait();
                     }
                     catch (InterruptedException e)
                     {
                        Thread.currentThread().interrupt();
                     }
                  }
                  else
                  {
                     break;
                  }
               }
            }
            endSignal.countDown();
         }
      };

      /**
       * MultithreadedChecking constructor.
       */
      public MultithreadedChecking(final DataStorage[] storages, final boolean autoRepair, int nThreads) throws IOException, RepositoryException
      {
         endSignal = new CountDownLatch(nThreads);
         allCheckingThreads = new Thread[nThreads];

         tasks = new LinkedBlockingQueue<Callable<Void>>()
         {
            private static final long serialVersionUID = 1L;

            @Override
            public Callable<Void> poll()
            {
               Callable<Void> task;
               synchronized (runningThreads)
               {
                  if ((task = super.poll()) != null)
                  {
                     runningThreads.incrementAndGet();
                  }
               }
               return task;
            }

            @Override
            public boolean offer(Callable<Void> o)
            {
               if (super.offer(o))
               {
                  synchronized (runningThreads)
                  {
                     runningThreads.notifyAll();
                  }
                  return true;
               }
               return false;
            }
         };
         doCheckAndRepair(storages, autoRepair, tasks);
      }

      /**
       * Starts all the checking threads
       */
      public String startThreads() throws IOException, RepositoryException
      {
         for (int i = 0; i < allCheckingThreads.length; i++)
         {
            (allCheckingThreads[i] = new Thread(checkingTask, "checking Thread #" + (i + 1))).start();
         }
         try
         {
            endSignal.await();
         }
         catch (InterruptedException e)
         {
            Thread.currentThread().interrupt();
            return logAndGetExceptionDuringCheckingMessage(e);
         }
         return logAndGetCheckingResultMessage();
      }
   }
}
