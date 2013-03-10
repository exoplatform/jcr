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
public class JobExistingRepositorySingleDBRestore extends JobRepositoryRestore {

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

    @Override
    /**
     * {@inheritDoc}
     */
    protected void restoreRepository() throws RepositoryRestoreExeption {

        // list of components to clean
        List<Backupable> backupable;

        // list of data restorers
        List<DataRestore> dataRestorer = new ArrayList<DataRestore>();

        // define one common connection for all restores and cleaners for single db case
        Connection jdbcConn = null;

        // define one common database cleaner for all restores for single db case
        DBCleanerTool dbCleaner = null;

        try {

            WorkspaceEntry wsEntry = repositoryService.getRepository(this.repositoryEntry.getName()).getConfiguration().getWorkspaceEntries().get(0);

            JDBCDataContainerConfig.DatabaseStructureType dbType = DBInitializerHelper.getDatabaseType(wsEntry);

            if (dbType.isShareSameDatasource()) {
                String dsName = wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME);

                final DataSource ds = (DataSource) new InitialContext().lookup(dsName);
                if (ds == null) {
                    throw new NameNotFoundException("Data source " + dsName + " not found");
                }

                jdbcConn = SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<Connection>() {
                    public Connection run() throws Exception {
                        return ds.getConnection();

                    }
                });
                jdbcConn.setAutoCommit(false);

                dbCleaner = DBCleanService.getRepositoryDBCleaner(jdbcConn, repositoryService.getRepository(this.repositoryEntry.getName()).getConfiguration());

            }
            ManageableRepository repository = repositoryService.getRepository(this.repositoryEntry.getName());
            for (String wsName : repository.getWorkspaceNames()) {
                LOG.info("Trying to suspend workspace '" + wsName + "'");
                WorkspaceContainerFacade wsContainer = repository.getWorkspaceContainer(wsName);
                wsContainer.setState(ManageableRepository.SUSPENDED);
            }

            boolean isSharedDbCleaner = false;
            for (WorkspaceEntry wEntry : repositoryEntry.getWorkspaceEntries()) {
                // get all backupable components
                backupable =
                        repositoryService.getRepository(this.repositoryEntry.getName()).getWorkspaceContainer(wEntry.getName())
                                .getComponentInstancesOfType(Backupable.class);

                File fullBackupDir =
                        JCRRestore.getFullBackupFile(new BackupChainLog(workspacesMapping.get(wEntry.getName()))
                                .getBackupConfig().getBackupDir());

                DataRestoreContext context;

                if (jdbcConn != null) {
                    context = new DataRestoreContext(
                            new String[]{
                                    DataRestoreContext.STORAGE_DIR,
                                    DataRestoreContext.DB_CONNECTION,
                                    DataRestoreContext.DB_CLEANER},
                            new Object[]{
                                    fullBackupDir,
                                    jdbcConn,
                                    isSharedDbCleaner ? new DummyDBCleanerTool() : dbCleaner});

                    isSharedDbCleaner = true;

                } else {
                    context = new DataRestoreContext(
                            new String[]{DataRestoreContext.STORAGE_DIR},
                            new Object[]{fullBackupDir});
                }

                for (Backupable component : backupable) {
                    dataRestorer.add(component.getDataRestorer(context));
                }
            }


            ArrayList<WorkspaceEntry> workspaceList = new ArrayList<WorkspaceEntry>();
            workspaceList.addAll(repositoryEntry.getWorkspaceEntries());

            //close all session
            LOG.info("Trying to close all the current sessions of all the workspaces of the repository");

            for (WorkspaceEntry wEntry : workspaceList) {
                forceCloseSession(repositoryEntry.getName(), wEntry.getName());
            }


            for (DataRestore restorer : dataRestorer) {
                restorer.clean();
            }
            for (DataRestore restorer : dataRestorer) {
                restorer.commit();
            }
            //remove repository
            LOG.info("Trying to remove the repository '" + repositoryEntry.getName() + "'");
            repositoryService.removeRepository(repositoryEntry.getName());

            super.restoreRepository();
        } catch (Throwable t) {
            LOG.info("Trying to roll back the changes");
            for (DataRestore restorer : dataRestorer) {
                try {
                    restorer.rollback();
                } catch (BackupException e) {
                    LOG.error("Can't rollback changes", e);
                }
            }
            throw new RepositoryRestoreExeption("Repository " + repositoryEntry.getName() + " was not restored", t);
        } finally {
            for (DataRestore restorer : dataRestorer) {
                try {
                    restorer.close();
                } catch (BackupException e) {
                    LOG.error("Can't close restorer", e);
                }
            }

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

