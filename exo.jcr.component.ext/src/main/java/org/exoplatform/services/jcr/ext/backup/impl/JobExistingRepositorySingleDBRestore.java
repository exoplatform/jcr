package org.exoplatform.services.jcr.ext.backup.impl;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.ext.backup.BackupChainLog;
import org.exoplatform.services.jcr.ext.backup.RepositoryRestoreExeption;
import org.exoplatform.services.jcr.impl.backup.BackupException;
import org.exoplatform.services.jcr.impl.backup.Backupable;
import org.exoplatform.services.jcr.impl.backup.DataRestore;
import org.exoplatform.services.jcr.impl.backup.JCRRestore;
import org.exoplatform.services.jcr.impl.backup.rdbms.DataRestoreContext;
import org.exoplatform.services.jcr.impl.clean.rdbms.DBCleanService;
import org.exoplatform.services.jcr.impl.clean.rdbms.DBCleanerTool;
import org.exoplatform.services.jcr.impl.clean.rdbms.DummyDBCleanerTool;
import org.exoplatform.services.jcr.impl.core.SessionRegistry;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializerHelper;

import java.io.File;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.sql.DataSource;

/**
 * Created by The eXo Platform SAS.
 * <p/>
 * <br/>Date: 2013
 *
 * @author <a href="mailto:aboughzela@exoplatform.com.ua">Aymen Boughzela</a>
 * @version $Id$
 */
public class JobExistingRepositorySingleDBRestore extends JobExistingRepositorySameConfigRestore {

    /**
     * JobExistingRepositorySingleDBRestore constructor.
     */
    public JobExistingRepositorySingleDBRestore(RepositoryService repoService, BackupManagerImpl backupManagerImpl,
                                                RepositoryEntry repositoryEntry, Map<String, File> workspacesMapping,
                                                File backupChainLogFile) {
        this(repoService, backupManagerImpl, repositoryEntry, workspacesMapping, backupChainLogFile, false);
    }

    /**
     * JobExistingRepositorySingleDBRestore constructor.
     */
    public JobExistingRepositorySingleDBRestore(RepositoryService repoService, BackupManagerImpl backupManagerImpl,
                                                RepositoryEntry repositoryEntry, Map<String, File> workspacesMapping,
                                                File backupChainLogFile, boolean removeJobOnceOver) {
        super(repoService, backupManagerImpl, repositoryEntry, workspacesMapping, backupChainLogFile, removeJobOnceOver);
    }


    /**
     * {@inheritDoc}
     */
    @Override
  protected void restoreData(List<DataRestore> dataRestorer,List<WorkspaceContainerFacade> workspacesWaits4Resume) throws RepositoryRestoreExeption{
      try{

          for (DataRestore restorer : dataRestorer) {
              restorer.commit();
          }
          ArrayList<WorkspaceEntry> workspaceList = new ArrayList<WorkspaceEntry>();
          workspaceList.addAll(repositoryEntry.getWorkspaceEntries());

          //close all session
          LOG.info("Trying to close all the current sessions of all the workspaces of the repository");

          for (WorkspaceEntry wEntry : workspaceList) {
              forceCloseSession(repositoryEntry.getName(), wEntry.getName());
          }
          //remove repository
          LOG.info("Trying to remove the repository '" + repositoryEntry.getName() + "'");
          repositoryService.removeRepository(repositoryEntry.getName());

          super.restoreData();


      }
      catch (Throwable t) {
          throw new RepositoryRestoreExeption("Repository " + repositoryEntry.getName() + " was not restored", t);
      }
  }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void removeRepository(RepositoryService repositoryService, String repositoryName)
            throws RepositoryException, RepositoryConfigurationException {
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
            RepositoryConfigurationException {
        ManageableRepository mr = repositoryService.getRepository(repositoryName);
        WorkspaceContainerFacade wc = mr.getWorkspaceContainer(workspaceName);

        SessionRegistry sessionRegistry = (SessionRegistry) wc.getComponent(SessionRegistry.class);

        return sessionRegistry.closeSessions(workspaceName);
    }
}

