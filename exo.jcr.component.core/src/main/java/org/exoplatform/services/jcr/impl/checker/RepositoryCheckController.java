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
      return checkAndRepair(new DataStorage[]{DataStorage.DB, DataStorage.VALUE_STORAGE, DataStorage.LUCENE_INDEX},
         false);
   }

   @Managed
   @ManagedDescription("Check repository database consistency.")
   public String checkDataBase()
   {
      return checkAndRepair(new DataStorage[]{DataStorage.DB}, false);
   }

   @Managed
   @ManagedDescription("Check repository value storage consistency.")
   public String checkValueStorage()
   {
      return checkAndRepair(new DataStorage[]{DataStorage.VALUE_STORAGE}, false);
   }

   @Managed
   @ManagedDescription("Check repository search index consistency.")
   public String checkIndex()
   {
      return checkAndRepair(new DataStorage[]{DataStorage.LUCENE_INDEX}, false);
   }

   @Managed
   @ManagedDescription("Auto-repair inconsistencies for value storage. "
      + "Don't forget to backup your data first. Set parameter to \"YES\" for enabling auto-repair feature")
   public String repairValueStorage(String confirmation)
   {
      if (confirmation.equalsIgnoreCase("YES"))
      {
         return checkAndRepair(new DataStorage[]{DataStorage.VALUE_STORAGE}, true);
      }
      else
      {
         return CONFIRMATION_FAILED_MESSAGE;
      }
   }

   @Managed
   @ManagedDescription("Auto-repair inconsistencies for database. "
      + "Don't forget to backup your data first. Set parameter to \"YES\" for enabling auto-repair feature")
   public String repairDataBase(String confirmation)
   {
      if (confirmation.equalsIgnoreCase("YES"))
      {
         return checkAndRepair(new DataStorage[]{DataStorage.DB}, true);
      }
      else
      {
         return CONFIRMATION_FAILED_MESSAGE;
      }
   }

   public String checkAndRepair(final DataStorage[] storages, final boolean autoRepair)
   {
      return SecurityHelper.doPrivilegedAction(new PrivilegedAction<String>()
      {
         public String run()
         {
            return checkAndRepairAction(storages, autoRepair);
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

   protected String checkAndRepairAction(DataStorage[] storages, boolean autoRepair)
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

         return doCheckAndRepair(storages, autoRepair);
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
         for (DataStorage storage : storages)
         {
            switch (storage)
            {
               case DB :
                  doCheckDataBase(autoRepair);
                  break;

               case VALUE_STORAGE :
                  doCheckValueStorage(autoRepair);
                  break;

               case LUCENE_INDEX :
                  doCheckIndex(autoRepair);
                  break;
            }
         }

         return logAndGetCheckingResultMessage();
      }
      catch (Throwable e)
      {
         return logAndGetExceptionDuringCheckingMessage(e);
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

   private void doCheckDataBase(boolean autoRepair)
   {
      for (String wsName : repository.getWorkspaceNames())
      {
         logComment("Check DB consistency. Workspace " + wsName);

         JDBCWorkspaceDataContainerChecker jdbcChecker = getJDBCChecker(wsName);
         jdbcChecker.checkDataBase(autoRepair);
         jdbcChecker.checkLocksInDataBase(autoRepair);
      }
   }

   private void doCheckValueStorage(boolean autoRepair)
   {
      for (String wsName : repository.getWorkspaceNames())
      {
         logComment("Check ValueStorage consistency. Workspace " + wsName);
         getJDBCChecker(wsName).checkValueStorage(autoRepair);
      }
   }

   private void doCheckIndex(boolean autoRepair) throws RepositoryException, IOException
   {
      final String systemWS = repository.getConfiguration().getSystemWorkspaceName();
      for (String wsName : repository.getWorkspaceNames())
      {
         logComment("Check SearchIndex consistency. Workspace " + wsName);

         SearchManager searchManager = (SearchManager)getComponent(SearchManager.class, wsName);

         searchManager.checkIndex(lastReport, systemWS.equals(wsName));
      }
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
}
