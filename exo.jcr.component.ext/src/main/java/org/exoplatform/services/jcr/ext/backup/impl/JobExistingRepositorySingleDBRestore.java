package org.exoplatform.services.jcr.ext.backup.impl;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.ext.backup.RepositoryRestoreExeption;
import org.exoplatform.services.jcr.impl.backup.DataRestore;
import org.exoplatform.services.jcr.impl.clean.rdbms.DBCleanException;
import org.exoplatform.services.jcr.impl.clean.rdbms.DBCleanService;
import org.exoplatform.services.jcr.impl.clean.rdbms.DBCleanerTool;
import org.exoplatform.services.jcr.impl.core.SessionRegistry;

import java.io.File;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;


/**
 * Created by The eXo Platform SAS.
 * <p/>
 * <br/>Date: 2013
 *
 * @author <a href="mailto:aboughzela@exoplatform.com.ua">Aymen Boughzela</a>
 * @version $Id$
 */
public class JobExistingRepositorySingleDBRestore extends JobExistingRepositorySameConfigRestore
{

   /**
    * JobExistingRepositorySingleDBRestore constructor.
    */
   public JobExistingRepositorySingleDBRestore(RepositoryService repoService, BackupManagerImpl backupManagerImpl,
                                               RepositoryEntry repositoryEntry, Map<String, File> workspacesMapping,
                                               File backupChainLogFile)
   {
      this(repoService, backupManagerImpl, repositoryEntry, workspacesMapping, backupChainLogFile, false);
   }

   /**
    * JobExistingRepositorySingleDBRestore constructor.
    */
   public JobExistingRepositorySingleDBRestore(RepositoryService repoService, BackupManagerImpl backupManagerImpl,
                                               RepositoryEntry repositoryEntry, Map<String, File> workspacesMapping,
                                               File backupChainLogFile, boolean removeJobOnceOver)
   {
      super(repoService, backupManagerImpl, repositoryEntry, workspacesMapping, backupChainLogFile, removeJobOnceOver);
   }


   /**
    * {@inheritDoc}
    */
   @Override
   protected void restoreData(List<DataRestore> dataRestorer, List<WorkspaceContainerFacade> workspacesWaits4Resume) throws RepositoryRestoreExeption
   {
      try
      {

         for (DataRestore restorer : dataRestorer)
         {
            restorer.commit();
         }
         ArrayList<WorkspaceEntry> workspaceList = new ArrayList<WorkspaceEntry>();
         workspaceList.addAll(repositoryEntry.getWorkspaceEntries());

         //close all session
         LOG.info("Trying to close all the current sessions of all the workspaces of the repository");

         for (WorkspaceEntry wEntry : workspaceList)
         {
            forceCloseSession(repositoryEntry.getName(), wEntry.getName());
         }
         //remove repository
         LOG.info("Trying to remove the repository '" + repositoryEntry.getName() + "'");
         repositoryService.removeRepository(repositoryEntry.getName());

         super.restoreData();


      }
      catch (Throwable t) //NOSONAR
      {
         throw new RepositoryRestoreExeption("Repository " + repositoryEntry.getName() + " was not restored", t);
      }
   }


   /**
    * {@inheritDoc}
    */
   @Override
   protected void removeRepository(RepositoryService repositoryService, String repositoryName)
      throws RepositoryException, RepositoryConfigurationException
   {
   }

   /**
    * Close sessions on specific workspace.
    *
    * @param repositoryName repository name
    * @param workspaceName  workspace name
    * @return int return the how many sessions was closed
    * @throws RepositoryConfigurationException
    *                             will be generate RepositoryConfigurationException
    * @throws RepositoryException will be generate RepositoryException
    */
   private int forceCloseSession(String repositoryName, String workspaceName) throws RepositoryException,
      RepositoryConfigurationException
   {
      ManageableRepository mr = repositoryService.getRepository(repositoryName);
      WorkspaceContainerFacade wc = mr.getWorkspaceContainer(workspaceName);

      SessionRegistry sessionRegistry = (SessionRegistry)wc.getComponent(SessionRegistry.class);

      return sessionRegistry.closeSessions(workspaceName);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected WorkspaceEntry getWorkspaceConfig() throws RepositoryConfigurationException, RepositoryException
   {
      return repositoryService.getRepository(this.repositoryEntry.getName()).getConfiguration().getWorkspaceEntries().get(0);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected DBCleanerTool getDBCleaner(Connection jdbcConn) throws RepositoryRestoreExeption, DBCleanException, RepositoryConfigurationException, RepositoryException
   {
      return DBCleanService.getRepositoryDBCleaner(jdbcConn, repositoryService.getRepository(this.repositoryEntry.getName()).getConfiguration());
   }
}

