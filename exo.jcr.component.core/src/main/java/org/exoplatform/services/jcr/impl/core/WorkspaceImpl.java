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
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedWorkspace;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeImpl;
import org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeManagerImpl;
import org.exoplatform.services.jcr.impl.core.query.QueryManagerFactory;
import org.exoplatform.services.jcr.impl.core.query.QueryManagerImpl;
import org.exoplatform.services.jcr.impl.core.version.VersionImpl;
import org.exoplatform.services.jcr.impl.dataflow.ItemDataCloneVisitor;
import org.exoplatform.services.jcr.impl.dataflow.ItemDataCopyVisitor;
import org.exoplatform.services.jcr.impl.dataflow.ItemDataMoveVisitor;
import org.exoplatform.services.jcr.impl.dataflow.session.SessionChangesLog;
import org.exoplatform.services.jcr.impl.dataflow.session.TransactionableDataManager;
import org.exoplatform.services.jcr.impl.dataflow.version.VersionHistoryDataHelper;
import org.exoplatform.services.jcr.impl.xml.ExportImportFactory;
import org.exoplatform.services.jcr.impl.xml.importing.ContentImporter;
import org.exoplatform.services.jcr.impl.xml.importing.StreamImporter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.xml.sax.ContentHandler;

import java.io.IOException;
import java.io.InputStream;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: WorkspaceImpl.java 14100 2008-05-12 10:53:47Z gazarenkov $
 */

public class WorkspaceImpl implements ExtendedWorkspace
{

   protected static Log log = ExoLogger.getLogger("exo.jcr.component.core.WorkspaceImpl");

   private final SessionImpl session;

   private final NamespaceRegistryImpl namespaceRegistry;

   private final NodeTypeDataManager nodeTypeManager;

   private final ObservationManager observationManager;

   private final QueryManagerImpl queryManager;

   private final String name;

   public WorkspaceImpl(String name, ExoContainer container, SessionImpl session, ObservationManager observationManager)
      throws RepositoryException
   {

      this.session = session;
      this.name = name;
      this.observationManager = observationManager;

      this.namespaceRegistry = (NamespaceRegistryImpl)container.getComponentInstanceOfType(NamespaceRegistry.class);
      this.nodeTypeManager = (NodeTypeDataManager)container.getComponentInstanceOfType(NodeTypeDataManager.class);

      QueryManagerFactory qf = (QueryManagerFactory)container.getComponentInstanceOfType(QueryManagerFactory.class);
      if (qf == null)
         this.queryManager = null;
      else
         this.queryManager = qf.getQueryManager(session);
   }

   /**
    * @see javax.jcr.Workspace#clone
    */
   public void clone(String srcWorkspace, String srcAbsPath, String destAbsPath, boolean removeExisting)
      throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException,
      PathNotFoundException, ItemExistsException, RepositoryException
   {

      SessionChangesLog changes = new SessionChangesLog(session.getId());

      clone(srcWorkspace, srcAbsPath, destAbsPath, removeExisting, changes);

      session.getTransientNodesManager().getTransactManager().save(changes);
   }

   /**
    * @see javax.jcr.Workspace#copy
    */
   public void copy(String srcPath, String destPath) throws ItemExistsException, VersionException,
      PathNotFoundException, ItemExistsException, ConstraintViolationException, RepositoryException
   {

      copy(getName(), srcPath, destPath);
   }

   /**
    * @see javax.jcr.Workspace#copy
    */
   public void copy(String srcWorkspace, String srcAbsPath, String destAbsPath) throws NoSuchWorkspaceException,
      ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException,
      ItemExistsException, RepositoryException
   {

      // get source session
      SessionImpl srcSession = null;
      if (getName() != srcWorkspace)
      {
         srcSession = repository().internalLogin(session.getUserState(), srcWorkspace);
      }
      else
      {
         srcSession = session;
      }

      // get destination node
      JCRPath destNodePath = session.getLocationFactory().parseAbsPath(destAbsPath);
      if (destNodePath.isIndexSetExplicitly())
         throw new RepositoryException("The path provided must not have an index on its final element. "
            + destNodePath.getAsString(false));
      // get source node
      JCRPath srcNodePath = srcSession.getLocationFactory().parseAbsPath(srcAbsPath);

      NodeImpl srcNode = (NodeImpl)srcSession.getTransientNodesManager().getItem(srcNodePath.getInternalPath(), false);

      // get dst parent node
      NodeImpl destParentNode =
         (NodeImpl)session.getTransientNodesManager().getItem(destNodePath.makeParentPath().getInternalPath(), true);

      if (srcNode == null || destParentNode == null)
      {
         throw new PathNotFoundException("No node exists at " + srcAbsPath + " or no node exists one level above "
            + destAbsPath);
      }
      try
      {
         destParentNode.checkPermission(PermissionType.ADD_NODE);
      }
      catch (AccessControlException e)
      {
         throw new AccessDeniedException(e.getMessage());
      }
      destParentNode.validateChildNode(destNodePath.getName().getInternalName(), ((NodeTypeImpl)srcNode
         .getPrimaryNodeType()).getQName());

      NodeImpl destNode =
         (NodeImpl)session.getTransientNodesManager().getItem((NodeData)destParentNode.getData(),
            new QPathEntry(destNodePath.getInternalPath().getName(), 0), false);

      if (destNode != null)
      {

         if (!destNode.getDefinition().allowsSameNameSiblings())
         {
            throw new ItemExistsException("A node with name (" + destAbsPath + ") is already exists.");
         }
      }

      ItemDataCopyVisitor initializer =
         new ItemDataCopyVisitor((NodeData)destParentNode.getData(), destNodePath.getName().getInternalName(),
            nodeTypeManager, srcSession.getTransientNodesManager(), false);
      srcNode.getData().accept(initializer);

      PlainChangesLogImpl changesLog = new PlainChangesLogImpl(initializer.getItemAddStates(), session.getId());

      session.getTransientNodesManager().getTransactManager().save(changesLog);
   }

   /**
    * {@inheritDoc}
    */
   public String[] getAccessibleWorkspaceNames() throws RepositoryException
   {
      RepositoryImpl rep = (RepositoryImpl)session.getRepository();
      return rep.getWorkspaceNames();
   }

   /**
    * {@inheritDoc}
    */
   public ContentHandler getImportContentHandler(String parentAbsPath, int uuidBehavior) throws PathNotFoundException,
      ConstraintViolationException, VersionException, RepositoryException
   {
      NodeImpl node = (NodeImpl)session.getItem(parentAbsPath);
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
      return new ExportImportFactory().getImportHandler(((NodeData)node.getData()), uuidBehavior, session
         .getTransientNodesManager().getTransactManager(), session.getTransientNodesManager().getTransactManager(),
         nodeTypeManager, session.getLocationFactory(), session.getValueFactory(), getNamespaceRegistry(), session
            .getAccessManager(), session.getUserState(), context, (RepositoryImpl)session.getRepository(), name);
   }

   /**
    * {@inheritDoc}
    */
   public String getName()
   {
      return name;
   }

   /**
    * {@inheritDoc}
    */
   public NamespaceRegistry getNamespaceRegistry() throws RepositoryException
   {
      return namespaceRegistry;
   }

   /**
    * {@inheritDoc}
    */
   public NodeTypeManager getNodeTypeManager() throws RepositoryException
   {
      // incl Session mapping
      return new NodeTypeManagerImpl(session.getLocationFactory(), session.getValueFactory(), nodeTypeManager, session
         .getTransientNodesManager());
   }

   /**
    * {@inheritDoc}
    */
   public ObservationManager getObservationManager() throws UnsupportedRepositoryOperationException,
      RepositoryException
   {
      return observationManager;
   }

   /**
    * {@inheritDoc}
    */
   public QueryManager getQueryManager() throws RepositoryException
   {
      if (queryManager == null)
         throw new RepositoryException("Query Manager Factory not found. Check configuration.");
      return queryManager;
   }

   /**
    * {@inheritDoc}
    */
   public Session getSession()
   {
      return session;
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
    * org.exoplatform.services.jcr.core.ExtendedWorkspace#importXML(java.lang
    * .String, java.io.InputStream, int, boolean)
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
      NodeImpl node = (NodeImpl)session.getItem(parentAbsPath);
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
         new ExportImportFactory().getStreamImporter(((NodeData)node.getData()), uuidBehavior, session
            .getTransientNodesManager().getTransactManager(), session.getTransientNodesManager().getTransactManager(),
            nodeTypeManager, session.getLocationFactory(), session.getValueFactory(), getNamespaceRegistry(), session
               .getAccessManager(), session.getUserState(), context, (RepositoryImpl)session.getRepository(), name);
      importer.importStream(in);

   }

   /**
    * {@inheritDoc}
    */
   public void move(String srcAbsPath, String destAbsPath) throws ConstraintViolationException, VersionException,
      AccessDeniedException, PathNotFoundException, ItemExistsException, RepositoryException
   {

      // get destination node
      JCRPath destNodePath = session.getLocationFactory().parseAbsPath(destAbsPath);
      if (destNodePath.isIndexSetExplicitly())
         throw new RepositoryException("The path provided must not have an index on its final element. "
            + destNodePath.getAsString(false));
      // get source node
      JCRPath srcNodePath = session.getLocationFactory().parseAbsPath(srcAbsPath);

      NodeImpl srcNode = (NodeImpl)session.getTransientNodesManager().getItem(srcNodePath.getInternalPath(), false);

      // get dst parent node
      NodeImpl destParentNode =
         (NodeImpl)session.getTransientNodesManager().getItem(destNodePath.makeParentPath().getInternalPath(), true);

      if (srcNode == null || destParentNode == null)
      {
         throw new PathNotFoundException("No node exists at " + srcAbsPath + " or no node exists one level above "
            + destAbsPath);
      }
      try
      {
         destParentNode.checkPermission(PermissionType.ADD_NODE);
         srcNode.checkPermission(PermissionType.REMOVE);
      }
      catch (AccessControlException e)
      {
         throw new AccessDeniedException(e.getMessage());
      }
      destParentNode.validateChildNode(destNodePath.getName().getInternalName(), ((NodeTypeImpl)srcNode
         .getPrimaryNodeType()).getQName());

      // Check for node with destAbsPath name in session
      NodeImpl destNode =
         (NodeImpl)session.getTransientNodesManager().getItem((NodeData)destParentNode.getData(),
            new QPathEntry(destNodePath.getInternalPath().getName(), 0), false);

      if (destNode != null)
      {
         if (!destNode.getDefinition().allowsSameNameSiblings())
         {
            // Throw exception
            String msg = "A node with name (" + destAbsPath + ") is already exists.";
            throw new ItemExistsException(msg);
         }
      }

      // Check if versionable ancestor is not checked-in
      if (!srcNode.checkedOut())
         throw new VersionException("Source parent node " + srcNode.getPath()
            + " or its nearest ancestor is checked-in");
      // Check locking
      if (!srcNode.checkLocking())
         throw new LockException("Source parent node " + srcNode.getPath() + " is     locked ");

      ItemDataMoveVisitor initializer =
         new ItemDataMoveVisitor((NodeData)destParentNode.getData(), destNodePath.getName().getInternalName(),
            nodeTypeManager, session.getTransientNodesManager(), true);
      srcNode.getData().accept(initializer);

      PlainChangesLog changes = new PlainChangesLogImpl(session.getId());
      changes.addAll(initializer.getAllStates());

      session.getTransientNodesManager().getTransactManager().save(changes);
   }

   /**
    * @see javax.jcr.Workspace#restore
    */
   public void restore(Version[] versions, boolean removeExisting) throws UnsupportedRepositoryOperationException,
      VersionException, RepositoryException, InvalidItemStateException
   {

      restoreVersions(versions, removeExisting);
   }

   protected void clone(String srcWorkspace, String srcAbsPath, String destAbsPath, boolean removeExisting,
      SessionChangesLog changes) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException,
      AccessDeniedException, PathNotFoundException, ItemExistsException, RepositoryException
   {

      if (srcWorkspace.equals(getName()))
         throw new RepositoryException("Source and destination workspace are equals " + name);

      // make dest node path
      JCRPath destNodePath = session.getLocationFactory().parseAbsPath(destAbsPath);

      if (destNodePath.isIndexSetExplicitly())
         throw new RepositoryException("DestPath should not contain an index " + destAbsPath);

      // find src node
      SessionImpl srcSession = repository().internalLogin(session.getUserState(), srcWorkspace);

      // get source node
      JCRPath srcNodePath = srcSession.getLocationFactory().parseAbsPath(srcAbsPath);

      NodeImpl srcNode = (NodeImpl)srcSession.getTransientNodesManager().getItem(srcNodePath.getInternalPath(), false);

      NodeImpl destParentNode =
         (NodeImpl)session.getTransientNodesManager().getItem(destNodePath.makeParentPath().getInternalPath(), true);

      if (srcNode == null || destParentNode == null)
      {
         throw new PathNotFoundException("No node exists at source path " + srcAbsPath
            + " or no node exists one level above on destenation " + destAbsPath);
      }

      try
      {
         destParentNode.checkPermission(PermissionType.ADD_NODE);
      }
      catch (AccessControlException e)
      {
         throw new AccessDeniedException(e.getMessage());
      }

      destParentNode.validateChildNode(destNodePath.getName().getInternalName(), ((NodeTypeImpl)srcNode
         .getPrimaryNodeType()).getQName());

      // Check for node with destAbsPath name in session
      NodeImpl destNode =
         (NodeImpl)session.getTransientNodesManager().getItem((NodeData)destParentNode.getData(),
            new QPathEntry(destNodePath.getInternalPath().getName(), 0), false);

      ItemState changesItemState = null;

      if (changes != null)
      {
         changesItemState = changes.getItemState(destNodePath.getInternalPath());
      }

      if (destNode != null && !(changesItemState != null && changesItemState.isDeleted()))
      {
         if (!destNode.getDefinition().allowsSameNameSiblings())
         {
            // Throw exception
            String msg = "A node with name (" + destAbsPath + ") is already exists.";
            throw new ItemExistsException(msg);
         }
      }

      ItemDataCloneVisitor initializer =
         new ItemDataCloneVisitor((NodeData)destParentNode.getData(), destNodePath.getName().getInternalName(),
            nodeTypeManager, srcSession.getTransientNodesManager(), session.getTransientNodesManager(), removeExisting,
            changes);

      srcNode.getData().accept(initializer);

      // removeing existing nodes and properties
      if (removeExisting && initializer.getItemDeletedExistingStates(false).size() > 0)
      {
         changes.addAll(initializer.getItemDeletedExistingStates(true));
      }

      changes.addAll(initializer.getItemAddStates());
   }

   protected void restoreVersions(Version[] versions, boolean removeExisting)
      throws UnsupportedRepositoryOperationException, VersionException, RepositoryException, InvalidItemStateException
   {

      if (session.hasPendingChanges())
         throw new InvalidItemStateException("Session has pending changes ");

      // for restore operation
      List<String> existedIdentifiers = new ArrayList<String>(); // InWorkspace
      List<VersionImpl> notExistedVersions = new ArrayList<VersionImpl>();
      LinkedHashMap<VersionImpl, NodeData> existedVersions = new LinkedHashMap<VersionImpl, NodeData>();

      TransactionableDataManager dataManager = session.getTransientNodesManager().getTransactManager();

      for (Version v : versions)
      {
         String versionableIdentifier = v.getContainingHistory().getVersionableUUID();
         NodeData node = (NodeData)dataManager.getItemData(versionableIdentifier);
         if (node != null)
         {
            existedVersions.put((VersionImpl)v, node);
            existedIdentifiers.add(versionableIdentifier);
         }
         else
         {
            // not found, looking for parent
            // SPEC: For every version V in S that corresponds to a missing node in
            // the workspace, there must also be a parent of V in S
            // =========================================
            // Trying search for corresponding node,
            // If we have a corr node and her parent in the existed list - all ok,
            // otherwise exception will be thrown.
            NodeData corrNode = null;
            String versionableParentIdentifier = null;
            if (!v.getSession().getWorkspace().getName().equals(session.getWorkspace().getName()))
            {
               TransactionableDataManager vDataManager =
                  ((SessionImpl)v.getSession()).getTransientNodesManager().getTransactManager();
               corrNode = (NodeData)vDataManager.getItemData(versionableIdentifier);
               if (corrNode != null)
                  versionableParentIdentifier = corrNode.getParentIdentifier();
               else
                  log.warn("Workspace.restore(). Correspondent node is not found " + versionableIdentifier);
            }
            if (versionableParentIdentifier != null && existedIdentifiers.contains(versionableParentIdentifier))
            {
               notExistedVersions.add((VersionImpl)v);
               continue;
            }
            throw new VersionException(
               "No such node (for version, from the array of versions, "
                  + "that corresponds to a missing node in the workspace, there must also be a parent in the array). UUID: "
                  + versionableIdentifier);

         }
      }

      SessionChangesLog changesLog = new SessionChangesLog(session.getId());

      for (VersionImpl v : existedVersions.keySet())
      {
         try
         {
            NodeData node = existedVersions.get(v);

            NodeData destParent = (NodeData)dataManager.getItemData(node.getParentIdentifier());
            NodeData vh = (NodeData)dataManager.getItemData(v.getParentIdentifier()); // version
            // parent
            // it's a VH
            VersionHistoryDataHelper historyHelper =
               new VersionHistoryDataHelper((NodeData)vh, dataManager, nodeTypeManager);

            changesLog.addAll(v.restoreLog(destParent, node.getQPath().getName(), historyHelper, session,
               removeExisting, changesLog).getAllStates());
         }
         catch (ItemExistsException e)
         {
            throw new ItemExistsException("Workspace restore. Can't restore a node. "
               + v.getContainingHistory().getVersionableUUID() + ". " + e.getMessage(), e);
         }
         catch (RepositoryException e)
         {
            throw new RepositoryException("Workspace restore. Can't restore a node. "
               + v.getContainingHistory().getVersionableUUID() + ". Repository error: " + e.getMessage(), e);
         }
      }

      for (VersionImpl v : notExistedVersions)
      {
         String versionableIdentifier = v.getContainingHistory().getVersionableUUID();
         try
         {
            NodeData node = null;
            for (ItemState change : changesLog.getAllStates())
            {
               if (change.isNode() && change.isAdded()
                  && ((NodeData)change.getData()).getIdentifier().equals(versionableIdentifier))
               {
                  node = (NodeData)change.getData();
                  break;
               }
            }
            if (node != null)
            {
               NodeData destParent = (NodeData)dataManager.getItemData(node.getParentIdentifier());
               // version parent it's a VH
               NodeData vh = (NodeData)dataManager.getItemData(v.getParentIdentifier());
               VersionHistoryDataHelper historyHelper =
                  new VersionHistoryDataHelper((NodeData)vh, dataManager, nodeTypeManager);

               changesLog.addAll(v.restoreLog(destParent, node.getQPath().getName(), historyHelper, session,
                  removeExisting, changesLog).getAllStates());
            }
            else
            {
               throw new VersionException("No such node restored before (for version, from the array of versions, "
                  + "that corresponds to a missing node in the workspace, "
                  + "there must also be a parent in the array). UUID: " + versionableIdentifier);
            }
         }
         catch (ItemExistsException e)
         {
            throw new ItemExistsException("Workspace restore. Can't restore a node not existed before. "
               + versionableIdentifier + ". " + e.getMessage(), e);
         }
         catch (RepositoryException e)
         {
            throw new RepositoryException("Workspace restore. Can't restore a node not existed before. "
               + versionableIdentifier + ". Repository error: " + e.getMessage(), e);
         }
      }

      dataManager.save(changesLog);
   }

   private RepositoryImpl repository()
   {
      return (RepositoryImpl)session.getRepository();
   }

   public NodeTypeDataManager getNodeTypesHolder() throws RepositoryException
   {
      return nodeTypeManager;
   }

}
