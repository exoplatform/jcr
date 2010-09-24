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

import org.exoplatform.container.ExoContainer;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.AccessManager;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.core.NamespaceAccessor;
import org.exoplatform.services.jcr.core.SessionLifecycleListener;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.lock.SessionLockManager;
import org.exoplatform.services.jcr.impl.core.lock.WorkspaceLockManager;
import org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeImpl;
import org.exoplatform.services.jcr.impl.core.observation.ObservationManagerImpl;
import org.exoplatform.services.jcr.impl.core.observation.ObservationManagerRegistry;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.exoplatform.services.jcr.impl.dataflow.ItemDataMoveVisitor;
import org.exoplatform.services.jcr.impl.dataflow.persistent.LocalWorkspaceDataManagerStub;
import org.exoplatform.services.jcr.impl.ext.action.SessionActionCatalog;
import org.exoplatform.services.jcr.impl.ext.action.SessionActionInterceptor;
import org.exoplatform.services.jcr.impl.util.io.WorkspaceFileCleanerHolder;
import org.exoplatform.services.jcr.impl.xml.ExportImportFactory;
import org.exoplatform.services.jcr.impl.xml.ItemDataKeeperAdapter;
import org.exoplatform.services.jcr.impl.xml.XmlMapping;
import org.exoplatform.services.jcr.impl.xml.exporting.BaseXmlExporter;
import org.exoplatform.services.jcr.impl.xml.importing.ContentImporter;
import org.exoplatform.services.jcr.impl.xml.importing.StreamImporter;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.observation.ObservationManager;
import javax.jcr.version.VersionException;
import javax.xml.stream.XMLStreamException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov </a>
 * @version $Id: SessionImpl.java 14244 2008-05-14 11:44:54Z ksm $ The
 *          implementation supported CredentialsImpl
 */
public class SessionImpl implements ExtendedSession, NamespaceAccessor
{

   public static final int DEFAULT_LAZY_READ_THRESHOLD = 100;

   private final RepositoryImpl repository;

   private final ConversationState userState;

   private final WorkspaceImpl workspace;

   private final Map<String, String> namespaces;

   private final Map<String, String> prefixes;

   private final AccessManager accessManager;

   private final LocationFactory locationFactory;

   private final ValueFactoryImpl valueFactory;

   private final ExoContainer container;

   private final LocationFactory systemLocationFactory;

   private final SessionLockManager lockManager;

   protected final String workspaceName;

   private boolean live;

   private final List<SessionLifecycleListener> lifecycleListeners;

   private final String id;

   private final SessionActionInterceptor actionHandler;

   private long lastAccessTime;

   private final int lazyReadThreshold;

   private final SessionRegistry sessionRegistry;

   protected final SessionDataManager dataManager;

   protected final NodeTypeDataManager nodeTypeManager;

   public SessionImpl(String workspaceName, ConversationState userState, ExoContainer container)
      throws RepositoryException
   {
      this.workspaceName = workspaceName;
      this.container = container;
      this.live = true;
      this.id = IdGenerator.generate();
      this.userState = userState;

      this.repository = (RepositoryImpl)container.getComponentInstanceOfType(RepositoryImpl.class);
      this.systemLocationFactory = (LocationFactory)container.getComponentInstanceOfType(LocationFactory.class);

      this.accessManager = (AccessManager)container.getComponentInstanceOfType(AccessManager.class);
      WorkspaceEntry wsConfig = (WorkspaceEntry)container.getComponentInstanceOfType(WorkspaceEntry.class);

      this.lazyReadThreshold =
         wsConfig.getLazyReadThreshold() > 0 ? wsConfig.getLazyReadThreshold() : DEFAULT_LAZY_READ_THRESHOLD;

      WorkspaceFileCleanerHolder cleanerHolder =
         (WorkspaceFileCleanerHolder)container.getComponentInstanceOfType(WorkspaceFileCleanerHolder.class);

      this.locationFactory = new LocationFactory(this);
      this.valueFactory = new ValueFactoryImpl(locationFactory, wsConfig, cleanerHolder);

      this.namespaces = new LinkedHashMap<String, String>();
      this.prefixes = new LinkedHashMap<String, String>();

      // Observation manager per session
      ObservationManagerRegistry observationManagerRegistry =
         (ObservationManagerRegistry)container.getComponentInstanceOfType(ObservationManagerRegistry.class);
      ObservationManager observationManager = observationManagerRegistry.createObservationManager(this);

      LocalWorkspaceDataManagerStub workspaceDataManager =
         (LocalWorkspaceDataManagerStub)container.getComponentInstanceOfType(LocalWorkspaceDataManagerStub.class);

      this.dataManager = new SessionDataManager(this, workspaceDataManager);

      this.lockManager =
         ((WorkspaceLockManager)container.getComponentInstanceOfType(WorkspaceLockManager.class))
            .getSessionLockManager(id, dataManager);

      this.nodeTypeManager = (NodeTypeDataManager)container.getComponentInstanceOfType(NodeTypeDataManager.class);

      this.workspace = new WorkspaceImpl(workspaceName, container, this, observationManager);

      this.lifecycleListeners = new ArrayList<SessionLifecycleListener>();
      this.registerLifecycleListener((ObservationManagerImpl)observationManager);
      this.registerLifecycleListener(lockManager);

      SessionActionCatalog catalog =
         (SessionActionCatalog)container.getComponentInstanceOfType(SessionActionCatalog.class);
      actionHandler = new SessionActionInterceptor(catalog, container);

      sessionRegistry = (SessionRegistry)container.getComponentInstanceOfType(SessionRegistry.class);

      sessionRegistry.registerSession(this);
      this.lastAccessTime = System.currentTimeMillis();

   }

   /**
    * {@inheritDoc}
    */
   public void addLockToken(String lt)
   {
      getLockManager().addLockToken(lt);
   }

   /**
    * {@inheritDoc}
    */
   public void checkPermission(String absPath, String actions) throws AccessControlException
   {

      try
      {
         JCRPath jcrPath = locationFactory.parseAbsPath(absPath);
         AccessControlList acl = dataManager.getACL(jcrPath.getInternalPath());
         if (!accessManager.hasPermission(acl, actions, getUserState().getIdentity()))
            throw new AccessControlException("Permission denied " + absPath + " : " + actions);
      }
      catch (RepositoryException e)
      {
         throw new AccessControlException("Could not check permission for " + absPath + " " + e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void exportDocumentView(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse)
      throws InvalidSerializedDataException, PathNotFoundException, SAXException, RepositoryException
   {

      LocationFactory factory = new LocationFactory(((NamespaceRegistryImpl)repository.getNamespaceRegistry()));

      WorkspaceEntry wsConfig = (WorkspaceEntry)container.getComponentInstanceOfType(WorkspaceEntry.class);

      WorkspaceFileCleanerHolder cleanerHolder =
         (WorkspaceFileCleanerHolder)container.getComponentInstanceOfType(WorkspaceFileCleanerHolder.class);

      ValueFactoryImpl valueFactoryImpl = new ValueFactoryImpl(factory, wsConfig, cleanerHolder);

      try
      {
         BaseXmlExporter exporter =
            new ExportImportFactory().getExportVisitor(XmlMapping.DOCVIEW, contentHandler, skipBinary, noRecurse,
               getTransientNodesManager(), repository.getNamespaceRegistry(), valueFactoryImpl);

         JCRPath srcNodePath = getLocationFactory().parseAbsPath(absPath);
         ItemData srcItemData = dataManager.getItemData(srcNodePath.getInternalPath());

         if (srcItemData == null)
         {
            throw new PathNotFoundException("No node exists at " + absPath);
         }

         exporter.export((NodeData)srcItemData);

      }
      catch (XMLStreamException e)
      {
         throw new SAXException(e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void exportDocumentView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse)
      throws InvalidSerializedDataException, IOException, PathNotFoundException, RepositoryException
   {

      LocationFactory factory = new LocationFactory(((NamespaceRegistryImpl)repository.getNamespaceRegistry()));

      WorkspaceEntry wsConfig = (WorkspaceEntry)container.getComponentInstanceOfType(WorkspaceEntry.class);

      WorkspaceFileCleanerHolder cleanerHolder =
         (WorkspaceFileCleanerHolder)container.getComponentInstanceOfType(WorkspaceFileCleanerHolder.class);

      ValueFactoryImpl valueFactoryImpl = new ValueFactoryImpl(factory, wsConfig, cleanerHolder);

      try
      {
         BaseXmlExporter exporter =
            new ExportImportFactory().getExportVisitor(XmlMapping.DOCVIEW, out, skipBinary, noRecurse,
               getTransientNodesManager(), repository.getNamespaceRegistry(), valueFactoryImpl);

         JCRPath srcNodePath = getLocationFactory().parseAbsPath(absPath);
         ItemData srcItemData = dataManager.getItemData(srcNodePath.getInternalPath());

         if (srcItemData == null)
         {
            throw new PathNotFoundException("No node exists at " + absPath);
         }

         exporter.export((NodeData)srcItemData);
      }
      catch (XMLStreamException e)
      {
         throw new IOException(e.getLocalizedMessage());
      }
      catch (SAXException e)
      {
         throw new IOException(e.getLocalizedMessage());
      }
   }

   /**
    * {@inheritDoc}
    */
   public void exportWorkspaceSystemView(OutputStream out, boolean skipBinary, boolean noRecurse) throws IOException,
      PathNotFoundException, RepositoryException
   {
      LocationFactory factory = new LocationFactory(((NamespaceRegistryImpl)repository.getNamespaceRegistry()));

      WorkspaceEntry wsConfig = (WorkspaceEntry)container.getComponentInstanceOfType(WorkspaceEntry.class);

      WorkspaceFileCleanerHolder cleanerHolder =
         (WorkspaceFileCleanerHolder)container.getComponentInstanceOfType(WorkspaceFileCleanerHolder.class);

      ValueFactoryImpl valueFactoryImpl = new ValueFactoryImpl(factory, wsConfig, cleanerHolder);

      try
      {
         BaseXmlExporter exporter =
            new ExportImportFactory().getExportVisitor(XmlMapping.BACKUP, out, skipBinary, noRecurse,
               getTransientNodesManager(), repository.getNamespaceRegistry(), valueFactoryImpl);

         ItemData srcItemData = dataManager.getItemData(Constants.ROOT_UUID);
         if (srcItemData == null)
         {
            throw new PathNotFoundException("Root node not found");
         }

         exporter.export((NodeData)srcItemData);
      }
      catch (XMLStreamException e)
      {
         throw new IOException(e.getLocalizedMessage());
      }
      catch (SAXException e)
      {
         throw new IOException(e.getLocalizedMessage());
      }
   }

   /**
    * {@inheritDoc}
    */
   public void exportSystemView(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse)
      throws PathNotFoundException, SAXException, RepositoryException
   {
      LocationFactory factory = new LocationFactory(((NamespaceRegistryImpl)repository.getNamespaceRegistry()));

      WorkspaceEntry wsConfig = (WorkspaceEntry)container.getComponentInstanceOfType(WorkspaceEntry.class);

      WorkspaceFileCleanerHolder cleanerHolder =
         (WorkspaceFileCleanerHolder)container.getComponentInstanceOfType(WorkspaceFileCleanerHolder.class);

      ValueFactoryImpl valueFactoryImpl = new ValueFactoryImpl(factory, wsConfig, cleanerHolder);
      try
      {
         BaseXmlExporter exporter =
            new ExportImportFactory().getExportVisitor(XmlMapping.SYSVIEW, contentHandler, skipBinary, noRecurse,
               getTransientNodesManager(), repository.getNamespaceRegistry(), valueFactoryImpl);

         JCRPath srcNodePath = getLocationFactory().parseAbsPath(absPath);
         ItemData srcItemData = dataManager.getItemData(srcNodePath.getInternalPath());
         if (srcItemData == null)
         {
            throw new PathNotFoundException("No node exists at " + absPath);
         }

         exporter.export((NodeData)srcItemData);

      }
      catch (XMLStreamException e)
      {
         throw new SAXException(e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void exportSystemView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse)
      throws IOException, PathNotFoundException, RepositoryException
   {
      LocationFactory factory = new LocationFactory(((NamespaceRegistryImpl)repository.getNamespaceRegistry()));

      WorkspaceEntry wsConfig = (WorkspaceEntry)container.getComponentInstanceOfType(WorkspaceEntry.class);

      WorkspaceFileCleanerHolder cleanerHolder =
         (WorkspaceFileCleanerHolder)container.getComponentInstanceOfType(WorkspaceFileCleanerHolder.class);

      ValueFactoryImpl valueFactoryImpl = new ValueFactoryImpl(factory, wsConfig, cleanerHolder);
      try
      {
         BaseXmlExporter exporter =
            new ExportImportFactory().getExportVisitor(XmlMapping.SYSVIEW, out, skipBinary, noRecurse,
               getTransientNodesManager(), repository.getNamespaceRegistry(), valueFactoryImpl);

         JCRPath srcNodePath = getLocationFactory().parseAbsPath(absPath);
         ItemData srcItemData = dataManager.getItemData(srcNodePath.getInternalPath());

         if (srcItemData == null)
         {
            throw new PathNotFoundException("No node exists at " + absPath);
         }

         exporter.export((NodeData)srcItemData);
      }
      catch (XMLStreamException e)
      {
         throw new IOException(e.getLocalizedMessage());
      }
      catch (SAXException e)
      {
         throw new IOException(e.getLocalizedMessage());
      }
   }

   /**
    * @return Returns the accessManager.
    */
   public AccessManager getAccessManager()
   {
      return accessManager;
   }

   public SessionActionInterceptor getActionHandler()
   {
      return actionHandler;
   }

   /**
    * {@inheritDoc}
    */
   public String[] getAllNamespacePrefixes() throws RepositoryException
   {
      return getNamespacePrefixes();
   }

   /**
    * {@inheritDoc}
    */
   public Object getAttribute(String name)
   {
      return userState.getAttribute(name);
   }

   /**
    * {@inheritDoc}
    */
   public String[] getAttributeNames()
   {

      Set<String> attributes = userState.getAttributeNames();

      String[] names = new String[attributes.size()];
      int i = 0;
      for (String name : attributes)
         names[i++] = name;
      return names;
   }

   /**
    * For debug purpose! Can accessed by admin only, otherwise null will be
    * returned
    * 
    * @deprecated use WorkspaceContainerFacade instead of using container
    *             directly
    */
   public ExoContainer getContainer()
   {
      return container;
   }

   /**
    * {@inheritDoc}
    */
   public String getId()
   {
      return id;
   }

   /**
    * {@inheritDoc}
    */
   public ContentHandler getImportContentHandler(String parentAbsPath, int uuidBehavior) throws

   PathNotFoundException, ConstraintViolationException, VersionException, RepositoryException
   {
      NodeImpl node = (NodeImpl)getItem(parentAbsPath);
      // checked-in check
      if (!node.checkedOut())
      {
         throw new VersionException("Node " + node.getPath() + " or its nearest ancestor is checked-in");
      }

      // Check if node is not protected
      if (node.getDefinition().isProtected())
      {
         throw new ConstraintViolationException("Can't add protected node " + node.getName() + " to "
            + node.getParent().getPath());
      }

      // Check locking
      if (!node.checkLocking())
      {
         throw new LockException("Node " + node.getPath() + " is locked ");
      }

      Map<String, Object> context = new HashMap<String, Object>();
      context.put(ContentImporter.RESPECT_PROPERTY_DEFINITIONS_CONSTRAINTS, true);

      return new ExportImportFactory().getImportHandler(((NodeData)node.getData()), uuidBehavior,
         new ItemDataKeeperAdapter(getTransientNodesManager()), getTransientNodesManager(), nodeTypeManager,
         getLocationFactory(), getValueFactory(), getWorkspace().getNamespaceRegistry(), getAccessManager(), userState,
         context, (RepositoryImpl)getRepository(), getWorkspace().getName());
   }

   /**
    * {@inheritDoc}
    */
   public ItemImpl getItem(String absPath) throws PathNotFoundException, RepositoryException
   {

      JCRPath loc = locationFactory.parseAbsPath(absPath);

      ItemImpl item = dataManager.getItem(loc.getInternalPath(), true);
      if (item != null)
         return item;

      throw new PathNotFoundException("Item not found " + absPath + " in workspace " + workspaceName);
   }

   public long getLastAccessTime()
   {
      return lastAccessTime;
   }

   /**
    * {@inheritDoc}
    */
   public LocationFactory getLocationFactory()
   {
      return locationFactory;
   }

   public SessionLockManager getLockManager()
   {
      return lockManager;
   }

   /**
    * {@inheritDoc}
    */
   public String[] getLockTokens()
   {
      return getLockManager().getLockTokens();
   }

   /**
    * {@inheritDoc}
    */
   public String getNamespacePrefix(String uri) throws NamespaceException, RepositoryException
   {
      if (prefixes.containsKey(uri))
      {
         return prefixes.get(uri);
      }
      return workspace.getNamespaceRegistry().getPrefix(uri);
   }

   /**
    * {@inheritDoc}
    */
   public String getNamespacePrefixByURI(String uri) throws NamespaceException, RepositoryException
   {
      return getNamespacePrefix(uri);
   }

   /**
    * {@inheritDoc}
    */
   public String[] getNamespacePrefixes() throws RepositoryException
   {
      Collection<String> allPrefixes = new LinkedList<String>();
      allPrefixes.addAll(namespaces.keySet());

      String[] permanentPrefixes = workspace.getNamespaceRegistry().getPrefixes();

      for (int i = 0; i < permanentPrefixes.length; i++)
      {
         if (!prefixes.containsKey(workspace.getNamespaceRegistry().getURI(permanentPrefixes[i])))
         {
            allPrefixes.add(permanentPrefixes[i]);
         }
      }
      return allPrefixes.toArray(new String[allPrefixes.size()]);
   }

   /**
    * {@inheritDoc}
    */
   public String getNamespaceURI(String prefix) throws NamespaceException, RepositoryException
   {
      String uri = null;
      // look in session first
      if (namespaces.size() > 0)
      {
         uri = namespaces.get(prefix);
         if (uri != null)
            return uri;
      }

      return workspace.getNamespaceRegistry().getURI(prefix);
   }

   /**
    * {@inheritDoc}
    */
   public String getNamespaceURIByPrefix(String prefix) throws NamespaceException, RepositoryException
   {
      return getNamespaceURI(prefix);
   }

   /**
    * {@inheritDoc}
    */
   public Node getNodeByIdentifier(String identifier) throws ItemNotFoundException, RepositoryException
   {
      Item item = dataManager.getItemByIdentifier(identifier, true);
      if (item != null && item.isNode())
         return (Node)item;

      throw new ItemNotFoundException("Node not found " + identifier + " at " + workspaceName);
   }

   /**
    * {@inheritDoc}
    */
   public Node getNodeByUUID(String uuid) throws ItemNotFoundException, RepositoryException
   {
      Item item = dataManager.getItemByIdentifier(uuid, true);

      if (item != null && item.isNode())
      {
         NodeImpl node = (NodeImpl)item;
         node.getUUID(); // throws exception
         return node;
      }

      throw new ItemNotFoundException("Node not found " + uuid + " at " + workspaceName);

   }

   /**
    * {@inheritDoc}
    */
   public Repository getRepository()
   {
      return repository;
   }

   /**
    * {@inheritDoc}
    */
   public Node getRootNode() throws RepositoryException
   {
      Item item = dataManager.getItemByIdentifier(Constants.ROOT_UUID, true);
      if (item != null && item.isNode())
      {
         return (NodeImpl)item;
      }

      throw new ItemNotFoundException("Node not found " + JCRPath.ROOT_PATH + " at " + workspaceName);
   }

   public SessionDataManager getTransientNodesManager()
   {
      return this.dataManager;
   }

   /**
    * {@inheritDoc}
    */
   public String getUserID()
   {
      return userState.getIdentity().getUserId();
   }

   /**
    * {@inheritDoc}
    */
   public ValueFactoryImpl getValueFactory() throws UnsupportedRepositoryOperationException, RepositoryException
   {
      return valueFactory;
   }

   /**
    * {@inheritDoc}
    */
   public WorkspaceImpl getWorkspace()
   {
      return workspace;
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasPendingChanges() throws RepositoryException
   {
      return dataManager.hasPendingChanges(Constants.ROOT_PATH);
   }

   /**
    * {@inheritDoc}
    */
   public Session impersonate(Credentials credentials) throws LoginException, RepositoryException
   {

      String name;
      if (credentials instanceof CredentialsImpl)
      {
         name = ((CredentialsImpl)credentials).getUserID();
      }
      else if (credentials instanceof SimpleCredentials)
      {
         name = ((SimpleCredentials)credentials).getUserID();
      }
      else
         throw new LoginException(
            "Credentials for the authentication should be CredentialsImpl or SimpleCredentials type");

      SessionFactory sessionFactory = (SessionFactory)container.getComponentInstanceOfType(SessionFactory.class);

      ConversationState newState =
         new ConversationState(new Identity(name, userState.getIdentity().getMemberships(), userState.getIdentity()
            .getRoles()));
      return sessionFactory.createSession(newState);

   }

   /**
    * {@inheritDoc}
    */
   public void importXML(String parentAbsPath, InputStream in, int uuidBehavior) throws IOException,
      PathNotFoundException, ItemExistsException, ConstraintViolationException, InvalidSerializedDataException,
      RepositoryException
   {
      Map<String, Object> context = new HashMap<String, Object>();
      context.put(ContentImporter.RESPECT_PROPERTY_DEFINITIONS_CONSTRAINTS, true);

      importXML(parentAbsPath, in, uuidBehavior, context);
   }

   /*
    * (non-Javadoc)
    * @see
    * org.exoplatform.services.jcr.core.ExtendedSession#importXML(java.lang.String
    * , java.io.InputStream, int, boolean)
    */
   public void importXML(String parentAbsPath, InputStream in, int uuidBehavior,
      boolean respectPropertyDefinitionsConstraints) throws IOException, PathNotFoundException, ItemExistsException,
      ConstraintViolationException, InvalidSerializedDataException, RepositoryException
   {
      Map<String, Object> context = new HashMap<String, Object>();
      context.put(ContentImporter.RESPECT_PROPERTY_DEFINITIONS_CONSTRAINTS, respectPropertyDefinitionsConstraints);
      importXML(parentAbsPath, in, uuidBehavior, context);

   }

   /**
    * {@inheritDoc}
    */
   public void importXML(String parentAbsPath, InputStream in, int uuidBehavior, Map<String, Object> context)
      throws IOException, PathNotFoundException, ItemExistsException, ConstraintViolationException,
      InvalidSerializedDataException, RepositoryException
   {
      NodeImpl node = (NodeImpl)getItem(parentAbsPath);
      if (!node.checkedOut())
      {
         throw new VersionException("Node " + node.getPath() + " or its nearest ancestor is checked-in");
      }

      // Check if node is not protected
      if (node.getDefinition().isProtected())
      {
         throw new ConstraintViolationException("Can't add protected node " + node.getName() + " to "
            + node.getParent().getPath());
      }

      // Check locking
      if (!node.checkLocking())
      {
         throw new LockException("Node " + node.getPath() + " is locked ");
      }

      StreamImporter importer =
         new ExportImportFactory().getStreamImporter(((NodeData)node.getData()), uuidBehavior,
            new ItemDataKeeperAdapter(getTransientNodesManager()), getTransientNodesManager(), nodeTypeManager,
            getLocationFactory(), getValueFactory(), getWorkspace().getNamespaceRegistry(), getAccessManager(),
            userState, context, (RepositoryImpl)getRepository(), getWorkspace().getName());
      importer.importStream(in);
   }

   /**
    * {@inheritDoc}
    */
   public boolean isLive()
   {
      return live;
   }

   /**
    * {@inheritDoc}
    */
   public boolean itemExists(String absPath)
   {
      try
      {
         if (getItem(absPath) != null)
            return true;
      }
      catch (RepositoryException e)
      {
      }
      return false;
   }

   /**
    * {@inheritDoc}
    */
   public void logout()
   {
      for (int i = 0; i < lifecycleListeners.size(); i++)
      {
         lifecycleListeners.get(i).onCloseSession(this);
      }
      this.sessionRegistry.unregisterSession(getId());
      this.live = false;
   }

   /**
    * {@inheritDoc}
    */
   public void move(String srcAbsPath, String destAbsPath) throws ItemExistsException, PathNotFoundException,
      VersionException, LockException, RepositoryException
   {
      JCRPath srcNodePath = getLocationFactory().parseAbsPath(srcAbsPath);

      NodeImpl srcNode = (NodeImpl)dataManager.getItem(srcNodePath.getInternalPath(), false);
      JCRPath destNodePath = getLocationFactory().parseAbsPath(destAbsPath);
      if (destNodePath.isIndexSetExplicitly())
         throw new RepositoryException("The relPath provided must not have an index on its final element. "
            + destNodePath.getAsString(false));

      NodeImpl destParentNode = (NodeImpl)dataManager.getItem(destNodePath.makeParentPath().getInternalPath(), true);

      if (srcNode == null || destParentNode == null)
      {
         throw new PathNotFoundException("No node exists at " + srcAbsPath + " or no node exists one level above "
            + destAbsPath);
      }

      destParentNode.validateChildNode(destNodePath.getName().getInternalName(),
         ((NodeTypeImpl)srcNode.getPrimaryNodeType()).getQName());

      // Check for node with destAbsPath name in session
      NodeImpl destNode =
         (NodeImpl)dataManager.getItem((NodeData)destParentNode.getData(), new QPathEntry(destNodePath
            .getInternalPath().getName(), 0), false, ItemType.NODE);

      if (destNode != null)
      {
         if (!destNode.getDefinition().allowsSameNameSiblings())
         {
            throw new ItemExistsException("A node with this name (" + destAbsPath + ") is already exists. ");
         }
      }

      // Check if versionable ancestor is not checked-in
      if (!srcNode.parent().checkedOut())
         throw new VersionException("Parent or source Node or its nearest ancestor is checked-in");

      if (!srcNode.checkLocking())
         throw new LockException("Source parent node " + srcNode.getPath() + " is locked ");

      ItemDataMoveVisitor initializer =
         new ItemDataMoveVisitor((NodeData)destParentNode.getData(), destNodePath.getName().getInternalName(),
            nodeTypeManager, getTransientNodesManager(), true);

      getTransientNodesManager().rename((NodeData)srcNode.getData(), initializer);
   }

   /**
    * {@inheritDoc}
    */
   public void refresh(boolean keepChanges) throws RepositoryException
   {
      getRootNode().refresh(keepChanges);
   }

   /**
    * {@inheritDoc}
    */
   public void registerLifecycleListener(SessionLifecycleListener listener)
   {
      this.lifecycleListeners.add(listener);
   }

   /**
    * {@inheritDoc}
    */
   public void removeLockToken(String lt)
   {
      getLockManager().removeLockToken(lt);
   }

   /**
    * {@inheritDoc}
    */
   public void save() throws AccessDeniedException, LockException, ConstraintViolationException,
      InvalidItemStateException, RepositoryException
   {
      getRootNode().save();
   }

   /**
    * {@inheritDoc}
    */
   public void setNamespacePrefix(String prefix, String uri) throws NamespaceException, RepositoryException
   {
      NamespaceRegistryImpl nrg = (NamespaceRegistryImpl)workspace.getNamespaceRegistry();
      if (!nrg.isUriRegistered(uri))
         throw new NamespaceException("The specified uri:" + uri + " is not among "
            + "those registered in the NamespaceRegistry");
      if (nrg.isPrefixMaped(prefix))
         throw new NamespaceException("A prefix '" + prefix + "' is currently already mapped to " + nrg.getURI(prefix)
            + " URI persistently in the repository NamespaceRegistry "
            + "and cannot be remapped to a new URI using this method, since this would make any "
            + "content stored using the old URI unreadable.");
      if (namespaces.containsKey(prefix))
         throw new NamespaceException("A prefix '" + prefix + "' is currently already mapped to "
            + namespaces.get(prefix) + " URI transiently within this Session and cannot be "
            + "remapped to a new URI using this method, since this would make any "
            + "content stored using the old URI unreadable.");
      nrg.validateNamespace(prefix, uri);
      namespaces.put(prefix, uri);
      prefixes.put(uri, prefix);
   }

   public void updateLastAccessTime()
   {
      lastAccessTime = System.currentTimeMillis();
   }

   LocationFactory getSystemLocationFactory()
   {
      return systemLocationFactory;
   }

   public ConversationState getUserState()
   {
      return this.userState;
   }

   int getLazyReadThreshold()
   {
      return lazyReadThreshold;
   }

}
