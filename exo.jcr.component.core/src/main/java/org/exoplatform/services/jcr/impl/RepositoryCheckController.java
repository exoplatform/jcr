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
package org.exoplatform.services.jcr.impl;

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.impl.core.query.SearchManager;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainerChecker;
import org.exoplatform.services.jcr.storage.value.ValueStoragePluginProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

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
 * @version $Id: exo-jboss-codetemplates.xml 34360 3.10.2011 skarpenko $
 */
@Managed
@NameTemplate(@Property(key = "service", value = "RepositoryCheckController"))
public class RepositoryCheckController extends AbstractRepositorySuspender implements Startable
{
   /**
    * Logger.
    */
   protected static Log LOG = ExoLogger.getLogger("exo.jcr.component.core.RepositorySuspendController");

   protected static final String FILE_NAME = "report";

   protected enum DataStorage {
      DB, VALUE_STORAGE, LUCENE_INDEX
   };

   private File inspectionLogFile = null;

   private String lastResult = null;

   /**
    * The current repository.
    */
   private final ManageableRepository repository;

   /**
    * RepositoryCheckController constructor.
    */
   public RepositoryCheckController(ManageableRepository repository)
   {
      super(repository);
      this.repository = repository;
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
   public String checkRepositoryDataConsistency()
   {
      return checkRepositoryDataConsistency(new DataStorage[]{DataStorage.DB, DataStorage.VALUE_STORAGE,
         DataStorage.LUCENE_INDEX});
   }

   @Managed
   @ManagedDescription("Check repository database consistency.")
   public String checkRepositoryDataBaseConsistency()
   {
      return checkRepositoryDataConsistency(new DataStorage[]{DataStorage.DB});
   }

   @Managed
   @ManagedDescription("Check repository value storage consistency.")
   public String checkRepositoryValueStorageConsistency()
   {
      return checkRepositoryDataConsistency(new DataStorage[]{DataStorage.VALUE_STORAGE});
   }

   @Managed
   @ManagedDescription("Check repository search index consistency.")
   public String checkRepositorySearchIndexConsistency()
   {
      return checkRepositoryDataConsistency(new DataStorage[]{DataStorage.LUCENE_INDEX});
   }

   protected String checkRepositoryDataConsistency(DataStorage[] checkData)
   {
      inspectionLogFile = null;
      try
      {
         if (getRepositoryState() == ManageableRepository.SUSPENDED)
         {
            return "Can not check data consistency. Repository is already suspended.";
         }

         Writer reportWriter = null;
         try
         {
            try
            {
               suspendRepository();
            }
            catch (RepositoryException e)
            {
               return "Can not check data consistency. Repository was not suspended. Error: " + e.getMessage();
            }

            // DO CHECK 
            inspectionLogFile =
               new File(FILE_NAME + "-" + repository.getConfiguration().getName() + "-"
                  + new SimpleDateFormat("dd-MMM-yy-HH-mm").format(new Date()) + ".txt");
            if (!PrivilegedFileHelper.exists(inspectionLogFile)
               && !PrivilegedFileHelper.createNewFile(inspectionLogFile))
            {
               LOG.error("Inspection log file was not created. "
                  + PrivilegedFileHelper.getAbsolutePath(inspectionLogFile));
               return "Can not check data consistency. Inspection log file was not created. "
                  + PrivilegedFileHelper.getAbsolutePath(inspectionLogFile);
            }

            reportWriter =
               new BufferedWriter(new OutputStreamWriter(PrivilegedFileHelper.fileOutputStream(inspectionLogFile)));
            InspectionLog report = new InspectionLogWriter(reportWriter);
            for (DataStorage cd : checkData)
            {
               switch (cd)
               {
                  case DB :
                     try
                     {
                        checkDB(report);
                     }
                     catch (RepositoryException e)
                     {
                        report.logException("RepositoryException occures during DB consistency check.", e);
                        return "RepositoryException occures during DB consistency check. Error: " + e.getMessage()
                           + ". See log here: " + PrivilegedFileHelper.getAbsolutePath(inspectionLogFile);
                     }
                     catch (IOException e)
                     {
                        report.logException("IOException occures during DB consistency check.", e);
                        return "IOException occures during DB consistency check. Error: " + e.getMessage()
                           + ". See log here: " + PrivilegedFileHelper.getAbsolutePath(inspectionLogFile);
                     }
                     break;
                  case VALUE_STORAGE :
                     try
                     {
                        checkVS(report);
                     }
                     catch (RepositoryException e)
                     {
                        report.logException("RepositoryException occures during ValueStorage consistency check.", e);
                        return "RepositoryException occures during ValueStorage consistency check. Error: "
                           + e.getMessage() + ". See log here: "
                           + PrivilegedFileHelper.getAbsolutePath(inspectionLogFile);
                     }
                     catch (IOException e)
                     {
                        report.logException("IOException occures during ValueStorage consistency check.", e);
                        return "IOException occures during ValueStorage consistency check. Error: " + e.getMessage()
                           + ". See log here: " + PrivilegedFileHelper.getAbsolutePath(inspectionLogFile);
                     }
                     break;
                  case LUCENE_INDEX :
                     try
                     {
                        checkLuceneIndex(report);
                     }
                     catch (RepositoryException e)
                     {
                        report.logException("RepositoryException occures during SearchIndex consistency check.", e);
                        return "RepositoryException occures during SearchIndex consistency check. Error: "
                           + e.getMessage() + ". See log here: "
                           + PrivilegedFileHelper.getAbsolutePath(inspectionLogFile);
                     }
                     catch (IOException e)
                     {
                        report.logException("IOException occures during SearchIndex consistency check.", e);
                        return "IOException occures during SearchIndex consistency check. Error: " + e.getMessage()
                           + ". See log here: " + PrivilegedFileHelper.getAbsolutePath(inspectionLogFile);
                     }
                     break;
               }
            }

            if (report.hasInconsistency())
            {
               report.logComment("Repository data is NOT consistent.");
               return "Repository data is inconsistent. See full report by path "
                  + PrivilegedFileHelper.getAbsolutePath(inspectionLogFile);
            }
            else if (report.hasWarnings())
            {
               report.logComment("Repository data is consistent, except some warnings.");
               return "Repository data is consistent, except some warnings. See full report by path "
                  + PrivilegedFileHelper.getAbsolutePath(inspectionLogFile);
            }
            else
            {
               report.logComment("Repository data is consistent");
               return "Repository data is consistent. See full report by path "
                  + PrivilegedFileHelper.getAbsolutePath(inspectionLogFile);
            }
         }
         finally
         {
            if (reportWriter != null)
            {
               try
               {
                  reportWriter.flush();
                  reportWriter.close();
               }
               catch (IOException e)
               {
                  LOG.error("Can not close file " + PrivilegedFileHelper.getAbsolutePath(inspectionLogFile), e);
               }
            }

            //resume repository
            try
            {
               resumeRepository();
            }
            catch (RepositoryException e)
            {
               LOG.error("Can not resume repository. Error: " + e.getMessage(), e);
            }
            if (getRepositoryState() != ManageableRepository.ONLINE)
            {
               LOG.error("Repository was not resumed and now it is OFFLINE");
            }
         }
      }
      catch (Throwable e)
      {
         LOG.error(e.getMessage(), e);
         return "Exception thrown during repository data validation: "
            + e
            + (inspectionLogFile != null ? " See log by path "
               + PrivilegedFileHelper.getAbsolutePath(inspectionLogFile) : "");
      }
   }

   private void checkDB(InspectionLog inspectionLog) throws RepositoryException, IOException,
      RepositoryConfigurationException
   {
      String[] wsNames = repository.getWorkspaceNames();
      for (String wsName : wsNames)
      {
         inspectionLog.logComment("Check DB consistency. Workspace " + wsName);
         JDBCWorkspaceDataContainer dataContainer =
            (JDBCWorkspaceDataContainer)repository.getWorkspaceContainer(wsName).getComponent(
               JDBCWorkspaceDataContainer.class);
         JDBCWorkspaceDataContainerChecker.checkDB(dataContainer, inspectionLog);
      }
   }

   private void checkVS(InspectionLog inspectionLog) throws RepositoryException, IOException
   {
      String[] wsNames = repository.getWorkspaceNames();
      for (String wsName : wsNames)
      {
         inspectionLog.logComment("Check ValueStorage consistency. Workspace " + wsName);

         JDBCWorkspaceDataContainer dataContainer =
            (JDBCWorkspaceDataContainer)repository.getWorkspaceContainer(wsName).getComponent(
               JDBCWorkspaceDataContainer.class);

         ValueStoragePluginProvider vsPlugin =
            (ValueStoragePluginProvider)repository.getWorkspaceContainer(wsName).getComponent(
               ValueStoragePluginProvider.class);

         JDBCWorkspaceDataContainerChecker.checkValueStorage(dataContainer, vsPlugin, inspectionLog);
      }
   }

   private void checkLuceneIndex(InspectionLog inspectionLog) throws RepositoryException, IOException
   {
      final String[] wsNames = repository.getWorkspaceNames();
      final String systemWS = repository.getConfiguration().getSystemWorkspaceName();
      for (String wsName : wsNames)
      {
         inspectionLog.logComment("Check SearchIndex consistency. Workspace " + wsName);
         SearchManager searchManager =
            (SearchManager)repository.getWorkspaceContainer(wsName).getComponent(SearchManager.class);
         searchManager.checkIndex(inspectionLog, systemWS.equals(wsName));
      }
   }

   /**
    * For test purposes.
    * @return
    */
   protected File getLastLogFile()
   {
      return inspectionLogFile;
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
