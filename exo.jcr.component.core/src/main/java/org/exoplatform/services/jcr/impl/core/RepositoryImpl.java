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
package org.exoplatform.services.jcr.impl.core;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.container.spi.ComponentAdapter;
import org.exoplatform.services.jcr.access.AuthenticationPolicy;
import org.exoplatform.services.jcr.access.DynamicIdentity;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.SystemParametersPersistenceConfigurator;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.security.JCRRuntimePermissions;
import org.exoplatform.services.jcr.dataflow.PersistentDataManager;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.dataflow.persistent.ItemsPersistenceListener;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.RepositoryContainer;
import org.exoplatform.services.jcr.impl.WorkspaceContainer;
import org.exoplatform.services.jcr.impl.dataflow.ItemDataRemoveVisitor;
import org.exoplatform.services.jcr.impl.dataflow.session.TransactionableDataManager;
import org.exoplatform.services.jcr.impl.xml.ExportImportFactory;
import org.exoplatform.services.jcr.impl.xml.importing.ContentImporter;
import org.exoplatform.services.jcr.impl.xml.importing.StreamImporter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.MembershipEntry;

import java.io.IOException;
import java.io.InputStream;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.LoginException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS.<br/>
 * Implementation of javax.jcr.Repository
 *
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov </a>
 * @version $Id: RepositoryImpl.java 14487 2008-05-20 07:08:40Z gazarenkov $
 */
public class RepositoryImpl implements ManageableRepository
{

   /**
    * Repository descriptors.
    */
   private static HashMap<String, String> descriptors = new HashMap<String, String>();

   /**
    * SYSTEM credentials.
    */
   private static final CredentialsImpl SYSTEM_CREDENTIALS = new CredentialsImpl(IdentityConstants.SYSTEM,
      "".toCharArray());
   
   /**
    * Logger.
    */
   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.RepositoryImpl");

   static
   {
      descriptors.put(SPEC_VERSION_DESC, "1.0");
      descriptors.put(SPEC_NAME_DESC, "Content Repository Java Technology API");
      descriptors.put(REP_VENDOR_DESC, "eXo Platform SAS");
      descriptors.put(REP_VENDOR_URL_DESC, "http://www.exoplatform.com");
      descriptors.put(REP_NAME_DESC, "eXo Java Content Repository");
      descriptors.put(REP_VERSION_DESC, "1.7.1");
      descriptors.put(LEVEL_1_SUPPORTED, "true");
      descriptors.put(LEVEL_2_SUPPORTED, "true");
      descriptors.put(OPTION_TRANSACTIONS_SUPPORTED, "true");
      descriptors.put(OPTION_VERSIONING_SUPPORTED, "true");
      descriptors.put(OPTION_OBSERVATION_SUPPORTED, "true");
      descriptors.put(OPTION_LOCKING_SUPPORTED, "true");
      descriptors.put(OPTION_QUERY_SQL_SUPPORTED, "true");
      descriptors.put(QUERY_XPATH_POS_INDEX, "true");
      descriptors.put(QUERY_XPATH_DOC_ORDER, "true");
   }

   /**
    * Repository Container.
    */
   private final RepositoryContainer repositoryContainer;

   /**
    * Ssystem Workspace Name.
    */
   private final String systemWorkspaceName;

   /**
    * Repository name.
    */
   private final String name;

   /**
    * Repository configuration.
    */
   private final RepositoryEntry config;

   /**
    * List of {@link WorkspaceManagingListener}.
    */
   private final List<WorkspaceManagingListener> workspaceListeners = new ArrayList<WorkspaceManagingListener>();

   /**
    * Repository authentication policy.
    */
   private final AuthenticationPolicy authenticationPolicy;

   /**
    * Repository state. OFFLINE by default.
    */
   private boolean isOffline = true;

   /**
    * RepositoryImpl constructor.
    *
    * @param container Repository container
    * @throws RepositoryException error of initialization
    * @throws RepositoryConfigurationException error of configuration
    */
   public RepositoryImpl(RepositoryContainer container) throws RepositoryException, RepositoryConfigurationException
   {

      config = (RepositoryEntry)container.getComponentInstanceOfType(RepositoryEntry.class);

      authenticationPolicy = (AuthenticationPolicy)container.getComponentInstanceOfType(AuthenticationPolicy.class);

      this.name = config.getName();
      this.systemWorkspaceName = config.getSystemWorkspaceName();
      this.repositoryContainer = container;
   }

   /**
    * {@inheritDoc}
    */
   public void addItemPersistenceListener(String workspaceName, ItemsPersistenceListener listener)
   {
      SecurityHelper.validateSecurityPermission(JCRRuntimePermissions.MANAGE_REPOSITORY_PERMISSION);

      PersistentDataManager pmanager =
         (PersistentDataManager)repositoryContainer.getWorkspaceContainer(workspaceName).getComponentInstanceOfType(
            PersistentDataManager.class);

      pmanager.addItemPersistenceListener(listener);
   }

   /**
    * {@inheritDoc}
    */
   public boolean canRemoveWorkspace(String workspaceName) throws NoSuchWorkspaceException
   {
      if (repositoryContainer.getWorkspaceEntry(workspaceName) == null)
      {
         throw new NoSuchWorkspaceException("No such workspace " + workspaceName);
      }

      if (workspaceName.equals(config.getSystemWorkspaceName()))
      {
         return false;
      }

      SessionRegistry sessionRegistry =
         (SessionRegistry)repositoryContainer.getComponentInstance(SessionRegistry.class);

      return sessionRegistry != null && !sessionRegistry.isInUse(workspaceName);
   }

   /**
    * {@inheritDoc}
    */
   public void configWorkspace(final WorkspaceEntry wsConfig) throws RepositoryConfigurationException,
      RepositoryException
   {
      SecurityHelper.validateSecurityPermission(JCRRuntimePermissions.MANAGE_REPOSITORY_PERMISSION);

      if (isWorkspaceInitialized(wsConfig.getName()))
      {
         throw new RepositoryConfigurationException("Workspace '" + wsConfig.getName()
            + "' is presumably initialized. config canceled");
      }

      try
      {
         repositoryContainer.initWorkspaceComponentEntries((SystemParametersPersistenceConfigurator)repositoryContainer
            .getComponentInstanceOfType(SystemParametersPersistenceConfigurator.class), wsConfig);

         repositoryContainer.registerWorkspace(wsConfig);
      }
      catch (RepositoryConfigurationException e)
      {
         final WorkspaceContainer workspaceContainer = repositoryContainer.getWorkspaceContainer(wsConfig.getName());
         SecurityHelper.doPrivilegedAction(new PrivilegedAction<Void>()
         {
            public Void run()
            {
               repositoryContainer.unregisterComponent(wsConfig.getName());
               return null;
            }
         });
         throw new RepositoryConfigurationException(e);
      }
      catch (RepositoryException e)
      {
         final WorkspaceContainer workspaceContainer = repositoryContainer.getWorkspaceContainer(wsConfig.getName());
         SecurityHelper.doPrivilegedAction(new PrivilegedAction<Void>()
         {
            public Void run()
            {
               repositoryContainer.unregisterComponent(wsConfig.getName());
               return null;
            }
         });
         throw new RepositoryException(e);
      }
   }

   /**
    * Creation contains three steps. First
    * <code>configWorkspace(WorkspaceEntry wsConfig)</code> - registration a new
    * configuration in RepositoryContainer and create WorkspaceContainer.
    * Second, the main step, is
    * <code>initWorkspace(String workspaceName, String rootNodeType)</code> -
    * initializing workspace by name and root nodetype. Third, final step,
    * starting all components of workspace. Before creation workspace <b>must be
    * configured</b>
    *
    * @see org.exoplatform.services.jcr.core.RepositoryImpl#configWorkspace(org.exoplatform.services.jcr.config.WorkspaceEntry
    *      )
    * @see org.exoplatform.services.jcr.core.RepositoryImpl#initWorkspace(java.lang.String,java.lang.String)
    * @param workspaceName - Creates a new Workspace with the specified name
    * @throws RepositoryException
    */
   public synchronized void createWorkspace(String workspaceName) throws RepositoryException
   {

      SecurityHelper.validateSecurityPermission(JCRRuntimePermissions.MANAGE_REPOSITORY_PERMISSION);
      
      final WorkspaceContainer wsContainer = repositoryContainer.getWorkspaceContainer(workspaceName);

      if (wsContainer == null)
      {
         throw new RepositoryException("Workspace " + workspaceName
            + " is not configured. Use RepositoryImpl.configWorkspace() method");
      }

      WorkspaceInitializer workspaceInitializer =
         repositoryContainer.getWorkspaceContainer(workspaceName).getWorkspaceInitializer();

      SystemParametersPersistenceConfigurator sppc =
         (SystemParametersPersistenceConfigurator)repositoryContainer
            .getComponentInstanceOfType(SystemParametersPersistenceConfigurator.class);

      if (sppc != null)
      {
         WorkspaceEntry workspaceEntry = repositoryContainer.getWorkspaceEntry(workspaceName);

         repositoryContainer.setInitializerAndValidateOverriddenParameters(workspaceEntry, workspaceInitializer);
      }

      if (isWorkspaceInitialized(workspaceName))
      {
         LOG.warn("Workspace '" + workspaceName + "' is presumably initialized. config canceled");
         return;
      }

      workspaceInitializer.initWorkspace();
      SecurityHelper.doPrivilegedAction(new PrivilegedAction<Void>()
      {
         public Void run()
         {
            wsContainer.start();
            return null;
         }
      });
      LOG.info("Workspace " + workspaceName + "@" + this.name + " is initialized");
   }

   /**
    * {@inheritDoc}
    */
   public RepositoryEntry getConfiguration()
   {
      SecurityHelper.validateSecurityPermission(JCRRuntimePermissions.MANAGE_REPOSITORY_PERMISSION);

      return config;
   }

   /**
    * {@inheritDoc}
    */
   public String getDescriptor(String key)
   {
      return descriptors.get(key);
   }

   // / -------- ManageableRepository impl -----------------------------

   /**
    * {@inheritDoc}
    */
   public String[] getDescriptorKeys()
   {
      String[] keys = new String[descriptors.size()];
      Iterator<String> decriptorsList = descriptors.keySet().iterator();
      int i = 0;
      while (decriptorsList.hasNext())
         keys[i++] = decriptorsList.next();
      return keys;
   }

   /**
    * @return default location factory
    */
   public LocationFactory getLocationFactory()
   {
      return repositoryContainer.getLocationFactory();
   }

   /**
    * @return the repository name as it configured in jcr configuration
    */
   public String getName()
   {
      return name;
   }

   /**
    * {@inheritDoc}
    */
   public NamespaceRegistry getNamespaceRegistry()
   {
      return repositoryContainer.getNamespaceRegistry();
   }

   /**
    * {@inheritDoc}
    */
   public ExtendedNodeTypeManager getNodeTypeManager()
   {
      return repositoryContainer.getNodeTypeManager();
   }

   /**
    * @return system session belongs to system workspace
    * @throws RepositoryException
    */
   public SessionImpl getSystemSession() throws RepositoryException
   {
      return getSystemSession(systemWorkspaceName);
   }

   /**
    * {@inheritDoc}
    */
   public SessionImpl getSystemSession(String workspaceName) throws RepositoryException
   {

      if (getState() == OFFLINE)
      {
         LOG.warn("Repository " + getName() + " is OFFLINE.");
      }

      WorkspaceContainer workspaceContainer = repositoryContainer.getWorkspaceContainer(workspaceName);
      if (workspaceContainer == null || !workspaceContainer.getWorkspaceInitializer().isWorkspaceInitialized())
      {
         throw new RepositoryException("Workspace " + workspaceName + " not found or workspace is not initialized");
      }

      SessionFactory sessionFactory = workspaceContainer.getSessionFactory();

      return sessionFactory.createSession(authenticationPolicy.authenticate(SYSTEM_CREDENTIALS));
   }

   /**
    * {@inheritDoc}
    */
   public SessionImpl getDynamicSession(String workspaceName, Collection<MembershipEntry> membershipEntries)
            throws RepositoryException
   {
      if (getState() == OFFLINE)
      {
         LOG.warn("Repository " + getName() + " is OFFLINE.");
      }

      WorkspaceContainer workspaceContainer = repositoryContainer.getWorkspaceContainer(workspaceName);
      if (workspaceContainer == null || !workspaceContainer.getWorkspaceInitializer().isWorkspaceInitialized())
      {
         throw new RepositoryException("Workspace " + workspaceName + " not found or workspace is not initialized");
      }

      SessionFactory sessionFactory = workspaceContainer.getSessionFactory();

      Identity id = new Identity(DynamicIdentity.DYNAMIC, membershipEntries);

      return sessionFactory.createSession(new ConversationState(id));
   }

   /**
    * @return system workspace name as it configured in jcr configuration
    */
   public String getSystemWorkspaceName()
   {
      return systemWorkspaceName;
   }

   /**
    * {@inheritDoc}
    */
   public String[] getWorkspaceNames()
   {

      List<ComponentAdapter<WorkspaceContainer>> adapters = repositoryContainer.getComponentAdaptersOfType(WorkspaceContainer.class);
      List<String> workspaceNames = new ArrayList<String>();
      for (int i = 0; i < adapters.size(); i++)
      {
         ComponentAdapter<WorkspaceContainer> adapter = adapters.get(i);
         String workspaceName = new String((String)adapter.getComponentKey());

         try
         {
            if (repositoryContainer.getWorkspaceContainer(workspaceName).getWorkspaceInitializer()
               .isWorkspaceInitialized())
               workspaceNames.add(workspaceName);
         }
         catch (RuntimeException e)
         {
            LOG.warn(e.getLocalizedMessage());
         }

      }
      return workspaceNames.toArray(new String[workspaceNames.size()]);
   }

   /**
    * {@inheritDoc}
    */
   public void importWorkspace(String wsName, InputStream xmlStream) throws RepositoryException, IOException
   {
      createWorkspace(wsName);
      SessionImpl sysSession = getSystemSession(wsName);

      try
      {
         Map<String, Object> context = new HashMap<String, Object>();
         context.put(ContentImporter.RESPECT_PROPERTY_DEFINITIONS_CONSTRAINTS, true);

         NodeData rootData = ((NodeData)((NodeImpl)sysSession.getRootNode()).getData());
         TransactionableDataManager dataManager = sysSession.getTransientNodesManager().getTransactManager();
         
         cleanWorkspace(dataManager);

         StreamImporter importer =
            new ExportImportFactory().getWorkspaceImporter(rootData, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW,
               dataManager, dataManager, sysSession.getWorkspace().getNodeTypesHolder(),
               sysSession.getLocationFactory(), sysSession.getValueFactory(), getNamespaceRegistry(),
               sysSession.getAccessManager(), sysSession.getUserState(), context, this, wsName);
         importer.importStream(xmlStream);
      }
      finally
      {
         sysSession.logout();
      }
   }

   private void cleanWorkspace(TransactionableDataManager dataManager)
      throws RepositoryException
   {
      NodeData rootData = (NodeData)dataManager.getItemData(Constants.ROOT_UUID);

      ItemDataRemoveVisitor removeVisitor = new ItemDataRemoveVisitor(dataManager, null);
      rootData.accept(removeVisitor);

      PlainChangesLogImpl changesLog = new PlainChangesLogImpl();
      changesLog.addAll(removeVisitor.getRemovedStates());

      dataManager.save(changesLog);
   }

   /**
    * Internal Remove Workspace.
    *
    * @param workspaceName workspace name
    * @throws RepositoryException error of remove
    */
   public void internalRemoveWorkspace(final String workspaceName) throws RepositoryException
   {
      SecurityHelper.validateSecurityPermission(JCRRuntimePermissions.MANAGE_REPOSITORY_PERMISSION);

      final WorkspaceContainer workspaceContainer = repositoryContainer.getWorkspaceContainer(workspaceName);
      try
      {
         SecurityHelper.doPrivilegedAction(new PrivilegedAction<Void>()
         {
            public Void run()
            {
               workspaceContainer.stop();
               return null;
            }
         });
      }
      catch (Exception e)
      {
         throw new RepositoryException(e);
      }
      SecurityHelper.doPrivilegedAction(new PrivilegedAction<Void>()
      {
         public Void run()
         {
            repositoryContainer.unregisterComponent(workspaceName);
            return null;
         }
      });

      config.getWorkspaceEntries().remove(repositoryContainer.getWorkspaceEntry(workspaceName));

      for (WorkspaceManagingListener listener : workspaceListeners)
      {
         listener.onWorkspaceRemove(workspaceName);
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean isWorkspaceInitialized(String workspaceName)
   {
      try
      {
         return repositoryContainer.getWorkspaceContainer(workspaceName).getWorkspaceInitializer()
            .isWorkspaceInitialized();
      }
      catch (Exception e)
      {
         return false;
      }
   }

   // //////////////////////////////////////////////////////

   /**
    * {@inheritDoc}
    */
   public Session login() throws LoginException, NoSuchWorkspaceException, RepositoryException
   {
      return login(null, null);
   }

   /**
    * {@inheritDoc}
    */
   public Session login(Credentials credentials) throws LoginException, NoSuchWorkspaceException, RepositoryException
   {
      return login(credentials, null);
   }

   /**
    * {@inheritDoc}
    */
   public Session login(String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException
   {
      return login(null, workspaceName);
   }

   /**
    * {@inheritDoc}
    */
   public Session login(final Credentials credentials, String workspaceName) throws LoginException,
      NoSuchWorkspaceException, RepositoryException
   {

      if (getState() == OFFLINE)
      {
         LOG.warn("Repository " + getName() + " is OFFLINE.");
      }

      ConversationState state;

      PrivilegedExceptionAction<ConversationState> action = new PrivilegedExceptionAction<ConversationState>()
      {
         public ConversationState run() throws Exception
         {
            if (credentials != null)
            {
               return authenticationPolicy.authenticate(credentials);
            }
            else
            {
               return authenticationPolicy.authenticate();
            }
         }
      };
      try
      {
         state = SecurityHelper.doPrivilegedExceptionAction(action);
      }
      catch (PrivilegedActionException pae)
      {
         Throwable cause = pae.getCause();
         if (cause instanceof LoginException)
         {
            throw (LoginException)cause;
         }
         else if (cause instanceof RuntimeException)
         {
            throw (RuntimeException)cause;
         }
         else
         {
            throw new RuntimeException(cause);
         }
      }

      return internalLogin(state, workspaceName);
   }

   /**
    * Internal login.
    *
    * @param state ConversationState
    * @param workspaceName workspace name
    * @return SessionImpl
    * @throws LoginException error of logic
    * @throws NoSuchWorkspaceException if no workspace found with name
    * @throws RepositoryException Repository error
    */
   SessionImpl internalLogin(ConversationState state, String workspaceName) throws LoginException,
      NoSuchWorkspaceException, RepositoryException
   {

      if (workspaceName == null)
      {
         workspaceName = config.getDefaultWorkspaceName();
         if (workspaceName == null)
         {
            throw new NoSuchWorkspaceException("Both workspace and default-workspace name are null! ");
         }
      }

      if (!isWorkspaceInitialized(workspaceName))
      {
         throw new NoSuchWorkspaceException("Workspace '" + workspaceName + "' not found. "
            + "Probably is not initialized. If so either Initialize it manually or turn on the RepositoryInitializer");
      }

      SessionFactory sessionFactory = repositoryContainer.getWorkspaceContainer(workspaceName).getSessionFactory();
      return sessionFactory.createSession(state);
   }

   /**
    * {@inheritDoc}
    */
   public void removeWorkspace(String workspaceName) throws RepositoryException
   {
      if (!canRemoveWorkspace(workspaceName))

         throw new RepositoryException("Workspace " + workspaceName + " in use. If you want to "
            + " remove workspace close all open sessions");

      internalRemoveWorkspace(workspaceName);
   }

   /**
    * {@inheritDoc}
    */
   public WorkspaceContainerFacade getWorkspaceContainer(String workspaceName)
   {
      return new WorkspaceContainerFacade(workspaceName, repositoryContainer.getWorkspaceContainer(workspaceName));
   }

   /**
    * {@inheritDoc}
    */
   public int getState()
   {
      if (isOffline)
      {
         return OFFLINE;
      }
      
      Integer state = null;
      for (String workspaceName : getWorkspaceNames())
      {
         int workspaceState = getWorkspaceContainer(workspaceName).getState();
         if (state == null)
         {
            state = workspaceState;
         }
         else if (state != workspaceState)
         {
            return UNDEFINED;
         }
      }
      
      return state == null ? ONLINE : state;
   }

   /**
    * {@inheritDoc}
    */
   public void setState(int state) throws RepositoryException
   {
      if (getState() != ONLINE && !(state == ONLINE || state == OFFLINE))
      {
         throw new RepositoryException("First switch repository to ONLINE and then to needed state.\n" + toString());
      }

      String[] workspaces = getWorkspaceNames();
      if (workspaces.length > 0)
      {
         // set state for all workspaces
         for (String workspaceName : workspaces)
         {
            if (!workspaceName.equals(systemWorkspaceName))
            {
               getWorkspaceContainer(workspaceName).setState(state);
            }
         }
         getWorkspaceContainer(systemWorkspaceName).setState(state);
      }

      isOffline = state == OFFLINE;
   }

   /**
    * {@inheritDoc}
    */
   public String getStateTitle()
   {
      switch (getState())
      {
         case ONLINE :
            return "online";
         case OFFLINE :
            return "offline";
         case SUSPENDED :
            return "suspended";
         default :
            return "undefined";
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String toString()
   {
      String defaultWorkspaceName = config.getDefaultWorkspaceName();
      return String.format(
         "Repository {\n name: %s;\n system workspace: %s;\n default workspace: %s;\n workspaces: %s;\n state: %s \n}",
         name, systemWorkspaceName, defaultWorkspaceName, Arrays.toString(getWorkspaceNames()), getStateTitle());
   }

   /**
    * Adds {@link WorkspaceManagingListener}.
    */
   public void addWorkspaceManagingListener(WorkspaceManagingListener listener)
   {
      workspaceListeners.add(listener);
   }

   /**
    * Removes {@link WorkspaceManagingListener}.
    */
   public void removeWorkspaceManagingListener(WorkspaceManagingListener listener)
   {
      workspaceListeners.remove(listener);
   }
}
