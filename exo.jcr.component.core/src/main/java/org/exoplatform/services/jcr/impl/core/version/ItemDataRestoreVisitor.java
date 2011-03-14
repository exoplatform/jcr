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
package org.exoplatform.services.jcr.impl.core.version;

import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.SystemIdentity;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.dataflow.AbstractItemDataCopyVisitor;
import org.exoplatform.services.jcr.impl.dataflow.ItemDataCopyVisitor;
import org.exoplatform.services.jcr.impl.dataflow.ItemDataRemoveVisitor;
import org.exoplatform.services.jcr.impl.dataflow.TransientItemData;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.dataflow.session.SessionChangesLog;
import org.exoplatform.services.jcr.impl.dataflow.version.VersionHistoryDataHelper;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import javax.jcr.ItemExistsException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.version.OnParentVersionAction;

/**
 * Created by The eXo Platform SAS. 
 * 
 * 14.12.2006
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: ItemDataRestoreVisitor.java 14100 2008-05-12 10:53:47Z gazarenkov $
 */
public class ItemDataRestoreVisitor extends AbstractItemDataCopyVisitor
{

   private final Log log = ExoLogger.getLogger("exo.jcr.component.core.ItemDataRestoreVisitor");

   protected final boolean removeExisting;

   protected final Stack<NodeDataContext> parents = new Stack<NodeDataContext>();

   protected final NodeData context;

   protected final NodeData history;

   protected final InternalQName destName;

   protected NodeData restored;

   protected final SessionImpl userSession;

   protected final SessionChangesLog changes;

   /**
    * Node paths for updating instead of deleting.
    */
   protected final Set<QPath> updatingPath = new HashSet<QPath>();

   /**
    * Usecase of Workspace.restore(Version[], boolean), for not existing
    * versionable nodes in the target workspace.
    */
   protected final SessionChangesLog delegatedChanges;

   private NodeTypeDataManager nodeTypeDataManager;

   protected class NodeDataContext
   {

      private final NodeData node;

      private final boolean existing;

      protected NodeDataContext(NodeData node)
      {
         this.node = node;
         this.existing = false;
      }

      protected NodeDataContext(NodeData node, boolean existing)
      {
         this.node = node;
         this.existing = existing;
      }

      protected NodeData getNode()
      {
         return this.node;
      }

      public boolean isExisting()
      {
         return existing;
      }
   }

   /**
    * Prepare item states for removing subtree before restoring. Accordingly to JCR specification 
    * some nodes will remain and not be deleted. For this case visitor skips such nodes and stores
    * its paths. For nodes with OnParentVersion attribute IGNORE ItemDataCopyIgnoredVisitor will be used
    * to copy children/properties to the restored node.
    */
   protected class RemoveVisitor extends ItemDataRemoveVisitor
   {
      /**
       * The stack. In the top it contains a parent node.
       */
      protected Stack<NodeData> parents = new Stack<NodeData>();

      /**
       * Last met node path during traversing which will remain during restore and not be deleted.
       */
      private QPath remainedNode;

      /**
       * Node path for updating instead of deleting. All ancestors of remained nodes 
       * not be deleted since updating will be applied later.
       */
      private final Set<QPath> updatingPath = new HashSet<QPath>();

      /**
       * RemoveVisitor constructor.
       * 
       * @throws RepositoryException
       */
      RemoveVisitor() throws RepositoryException
      {
         super(userSession.getTransientNodesManager(), null, nodeTypeDataManager, userSession.getAccessManager(),
            userSession.getUserState());
      }

      /**
       * {@inheritDoc}
       */
      @Override
      protected void validateReferential(NodeData node) throws RepositoryException
      {
         // no REFERENCE validation here
      }

      /**
       * {@inheritDoc}
       */
      @Override
      protected void entering(NodeData node, int level) throws RepositoryException
      {
         if (level == 0)
         {
            removedRoot = node;
         }

         parents.push(node);
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public void visit(NodeData node) throws RepositoryException
      {
         if (currentLevel > 0)
         {
            NodeData parent = parents.peek();
            int onParentVersion =
               nodeTypeDataManager.getChildNodeDefinition(node.getQPath().getName(), parent.getPrimaryTypeName(),
                  parent.getMixinTypeNames()).getOnParentVersion();

            if (onParentVersion == OnParentVersionAction.VERSION
               && nodeTypeDataManager.isNodeType(Constants.MIX_VERSIONABLE, node.getPrimaryTypeName(),
                  node.getMixinTypeNames()))
            {
               remainedNode = node.getQPath();

               // node and its children will remain and not be removed
               return;
            }
         }

         super.visit(node);
      }

      /**
       * {@inheritDoc}
       */
      @Override
      protected void leaving(NodeData node, int level) throws RepositoryException
      {
         if (validate)
         {
            validate(node);
         }

         if (remainedNode != null && remainedNode.isDescendantOf(node.getQPath()))
         {
            updatingPath.add(node.getQPath());
         }
         else
         {
            if (!(node instanceof TransientItemData))
            {
               node = (NodeData)copyItemDataDelete(node);
            }

            ItemState state =
               new ItemState(node, ItemState.DELETED, true, ancestorToSave != null ? ancestorToSave
                  : removedRoot.getQPath());

            itemRemovedStates.add(state);
         }

         parents.pop();
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public List<ItemState> getRemovedStates()
      {
         return itemRemovedStates;
      }

      /**
       * Return nodes paths for updating.
       * 
       * @return Set
       */
      public Set<QPath> getUpdatingPath()
      {
         return updatingPath;
      }

   };

   ItemDataRestoreVisitor(NodeData context, InternalQName restoringName, NodeData history, SessionImpl userSession,
      boolean removeExisting) throws RepositoryException
   {
      this(context, restoringName, history, userSession, removeExisting, null);
   }

   ItemDataRestoreVisitor(NodeData context, InternalQName destName, NodeData history, SessionImpl userSession,
      boolean removeExisting, SessionChangesLog delegatedChanges) throws RepositoryException
   {
      super(userSession.getTransientNodesManager().getTransactManager());

      this.userSession = userSession;
      this.changes = new SessionChangesLog(userSession.getId());
      this.context = context;
      this.destName = destName;
      this.history = history;
      this.parents.push(new NodeDataContext(context));
      this.removeExisting = removeExisting;
      this.nodeTypeDataManager = userSession.getWorkspace().getNodeTypesHolder();
      this.delegatedChanges = delegatedChanges;
   }

   private NodeData currentNode()
   {
      return parents.peek().getNode();
   }

   private NodeData pushCurrent(NodeData node)
   {
      return parents.push(new NodeDataContext(node)).getNode();
   }

   private ItemData findDelegated(String identifier)
   {
      if (delegatedChanges != null)
      {
         for (ItemState state : delegatedChanges.getAllStates())
         {
            if (state.getData().getIdentifier().equals(identifier))
               return state.getData();
         }
      }

      return null;
   }

   private ItemData findDelegated(QPath path)
   {
      if (delegatedChanges != null)
      {
         for (ItemState state : delegatedChanges.getAllStates())
         {
            if (state.getData().getQPath().equals(path))
               return state.getData();
         }
      }

      return null;
   }

   private void deleteDelegated(QPath path)
   {
      if (delegatedChanges != null)
      {
         List<ItemState> removed = new ArrayList<ItemState>();
         for (ItemState state : delegatedChanges.getAllStates())
         {
            if (state.getData().getQPath().equals(path) || state.getData().getQPath().isDescendantOf(path))
               removed.add(state);
         }

         for (ItemState state : removed)
         {
            delegatedChanges.remove(state.getData().getQPath());
         }
      }
   }

   protected void initRestoreRoot(NodeData parentData, InternalQName name, NodeData frozen) throws RepositoryException
   {

      // WARNING: path with index=1
      QPath nodePath = QPath.makeChildPath(parentData.getQPath(), name);

      if (log.isDebugEnabled())
         log.debug("Restore: " + nodePath.getAsString() + ", removeExisting=" + removeExisting);

      PropertyData frozenIdentifier =
         (PropertyData)dataManager.getItemData(frozen, new QPathEntry(Constants.JCR_FROZENUUID, 1), ItemType.PROPERTY);

      String fidentifier = null;
      NodeData existing = null;
      // make new node from frozen
      try
      {
         fidentifier = new String(frozenIdentifier.getValues().get(0).getAsByteArray());
         NodeData sameIdentifierNodeRestored = (NodeData)findDelegated(fidentifier);
         if (sameIdentifierNodeRestored != null)
         {
            // already restored from delegated call, remove it as we interested in
            // this version state
            deleteDelegated(sameIdentifierNodeRestored.getQPath());
         }
         else
         {
            NodeData sameIdentifierNode = (NodeData)dataManager.getItemData(fidentifier);
            if (sameIdentifierNode != null)
            {
               QPath sameIdentifierPath = sameIdentifierNode.getQPath();
               if (sameIdentifierPath.makeParentPath().equals(nodePath.makeParentPath()) && // same
                  // parent
                  sameIdentifierPath.getName().equals(nodePath.getName()))
               { // same
                 // name

                  if (sameIdentifierPath.getIndex() != nodePath.getIndex())
                     // but different index, see below... fix it
                     nodePath = QPath.makeChildPath(parentData.getQPath(), name, sameIdentifierPath.getIndex());

                  // if it's a target node
                  existing = sameIdentifierNode;

                  // remove existed node, with validation
                  RemoveVisitor removeVisitor = new RemoveVisitor();
                  removeVisitor.visit(existing);

                  changes.addAll(removeVisitor.getRemovedStates());
                  updatingPath.addAll(removeVisitor.getUpdatingPath());
               }
               else if (!sameIdentifierPath.isDescendantOf(nodePath))
               {
                  if (removeExisting)
                  {
                     final QPath restorePath = nodePath;
                     // remove same uuid node, with validation
                     class RemoveVisitor extends ItemDataRemoveVisitor
                     {
                        RemoveVisitor() throws RepositoryException
                        {
                           super(userSession.getTransientNodesManager(), null, nodeTypeDataManager, userSession
                              .getAccessManager(), userSession.getUserState());
                        }

                        @Override
                        protected boolean isRemoveDescendant(ItemData item) throws RepositoryException
                        {
                           return item.getQPath().isDescendantOf(removedRoot.getQPath())
                              || item.getQPath().isDescendantOf(restorePath);
                        }
                     };

                     ItemDataRemoveVisitor removeVisitor = new RemoveVisitor();
                     removeVisitor.visit(sameIdentifierNode);

                     changes.addAll(removeVisitor.getRemovedStates());
                  }
                  else
                  {
                     throw new ItemExistsException("Item with the same UUID as restored node " + nodePath.getAsString()
                        + " already exists and removeExisting=false. Existed "
                        + userSession.getLocationFactory().createJCRPath(sameIdentifierPath).getAsString(false) + " "
                        + sameIdentifierNode.getIdentifier());
                  }
               }
            }
         }
      }
      catch (IllegalStateException e)
      {
         throw new RepositoryException("jcr:frozenUuid, error of data read "
            + userSession.getLocationFactory().createJCRPath(frozenIdentifier.getQPath()).getAsString(false), e);
      }
      catch (IOException e)
      {
         throw new RepositoryException("jcr:frozenUuid, error of data read "
            + userSession.getLocationFactory().createJCRPath(frozenIdentifier.getQPath()).getAsString(false), e);
      }

      PropertyData frozenPrimaryType =
         (PropertyData)dataManager.getItemData(frozen, new QPathEntry(Constants.JCR_FROZENPRIMARYTYPE, 0),
            ItemType.PROPERTY);

      PropertyData frozenMixinTypes =
         (PropertyData)dataManager.getItemData(frozen, new QPathEntry(Constants.JCR_FROZENMIXINTYPES, 0),
            ItemType.PROPERTY);

      List<AccessControlEntry> accessList =
         new ArrayList<AccessControlEntry>(parentData.getACL().getPermissionEntries());

      String owner = parentData.getACL().getOwner();

      InternalQName[] mixins = null;
      if (frozenMixinTypes != null)
      {
         try
         {
            List<ValueData> mvs = frozenMixinTypes.getValues();
            mixins = new InternalQName[mvs.size()];
            for (int i = 0; i < mvs.size(); i++)
            {
               ValueData mvd = mvs.get(i);
               mixins[i] = InternalQName.parse(new String(mvd.getAsByteArray()));

               if (mixins[i].equals(Constants.EXO_PRIVILEGEABLE))
               {
                  PropertyData aclData =
                     (PropertyData)dataManager.getItemData(frozen, new QPathEntry(Constants.EXO_PERMISSIONS, 0),
                        ItemType.PROPERTY);

                  AccessControlList acl = new AccessControlList();
                  acl.removePermissions(SystemIdentity.ANY);

                  for (ValueData value : aclData.getValues())
                  {
                     acl.addPermissions(new String(value.getAsByteArray(), Constants.DEFAULT_ENCODING));
                  }

                  accessList = acl.getPermissionEntries();
               }
               else if (mixins[i].equals(Constants.EXO_OWNEABLE))
               {
                  PropertyData ownerData =
                     (PropertyData)dataManager.getItemData(frozen, new QPathEntry(Constants.EXO_OWNER, 0),
                        ItemType.PROPERTY);

                  owner = new String(ownerData.getValues().get(0).getAsByteArray(), Constants.DEFAULT_ENCODING);
               }
            }
         }
         catch (IllegalNameException e)
         {
            throw new RepositoryException("jcr:frozenMixinTypes, error of data read "
               + userSession.getLocationFactory().createJCRPath(frozenMixinTypes.getQPath()).getAsString(false), e);
         }
         catch (IllegalStateException e)
         {
            throw new RepositoryException("jcr:frozenMixinTypes, error of data read "
               + userSession.getLocationFactory().createJCRPath(frozenMixinTypes.getQPath()).getAsString(false), e);
         }
         catch (IOException e)
         {
            throw new RepositoryException("jcr:frozenMixinTypes, error of data read "
               + userSession.getLocationFactory().createJCRPath(frozenMixinTypes.getQPath()).getAsString(false), e);
         }
      }

      AccessControlList acl = new AccessControlList(owner, accessList);

      InternalQName ptName = null;
      try
      {
         ptName = InternalQName.parse(new String(frozenPrimaryType.getValues().get(0).getAsByteArray()));
      }
      catch (IllegalNameException e)
      {
         throw new RepositoryException("jcr:frozenPrimaryType, error of data read "
            + userSession.getLocationFactory().createJCRPath(frozenPrimaryType.getQPath()).getAsString(false), e);
      }
      catch (IllegalStateException e)
      {
         throw new RepositoryException("jcr:frozenPrimaryType, error of data read "
            + userSession.getLocationFactory().createJCRPath(frozenPrimaryType.getQPath()).getAsString(false), e);
      }
      catch (IOException e)
      {
         throw new RepositoryException("jcr:frozenPrimaryType, error of data read "
            + userSession.getLocationFactory().createJCRPath(frozenPrimaryType.getQPath()).getAsString(false), e);
      }

      // create restored version of the node
      NodeData restoredData =
         new TransientNodeData(nodePath, fidentifier, (existing != null ? existing.getPersistedVersion() : -1), ptName,
            mixins == null ? new InternalQName[0] : mixins, 0, parentData.getIdentifier(), acl);

      if (updatingPath.contains(nodePath))
      {
         changes.add(ItemState.createUpdatedState(restoredData));
      }
      else
      {
         changes.add(ItemState.createAddedState(restoredData));
      }

      pushCurrent(restoredData);
   }

   @Override
   protected void entering(NodeData frozen, int level) throws RepositoryException
   {

      if (frozen == null)
      {
         return;
      }

      if (log.isDebugEnabled())
      {
         log.debug("Visit node " + frozen.getQPath().getAsString() + ", HAS NULL FROZEN NODE");
      }

      InternalQName qname = frozen.getQPath().getName();

      if (qname.equals(Constants.JCR_FROZENNODE) && level == 0)
      {
         // child props/nodes will be restored
         if (log.isDebugEnabled())
         {
            log.debug("jcr:frozenNode " + frozen.getQPath().getAsString());
         }

         // init destenation node
         initRestoreRoot(currentNode(), destName, frozen);

         restored = currentNode();

      }
      else if (nodeTypeDataManager.isNodeType(Constants.NT_VERSIONEDCHILD, frozen.getPrimaryTypeName()))
      {

         QPath cvhpPropPath = QPath.makeChildPath(frozen.getQPath(), Constants.JCR_CHILDVERSIONHISTORY);

         if (log.isDebugEnabled())
         {
            log.debug("Versioned child node " + cvhpPropPath.getAsString());
         }

         VersionHistoryDataHelper childHistory = null;
         try
         {

            String vhIdentifier =
               new String(((PropertyData)dataManager.getItemData(frozen, new QPathEntry(
                  Constants.JCR_CHILDVERSIONHISTORY, 0), ItemType.PROPERTY)).getValues().get(0).getAsByteArray());

            NodeData cHistory = null;
            if ((cHistory = (NodeData)dataManager.getItemData(vhIdentifier)) == null)
               throw new RepositoryException("Version history is not found with uuid " + vhIdentifier);

            childHistory = new VersionHistoryDataHelper(cHistory, dataManager, nodeTypeDataManager);
         }
         catch (IllegalStateException e)
         {
            throw new RepositoryException("jcr:childVersionHistory, error of data read "
               + userSession.getLocationFactory().createJCRPath(cvhpPropPath).getAsString(false), e);
         }
         catch (IOException e)
         {
            throw new RepositoryException("jcr:childVersionHistory, error of data read "
               + userSession.getLocationFactory().createJCRPath(cvhpPropPath).getAsString(false), e);
         }

         String versionableIdentifier = null;
         try
         {
            versionableIdentifier =
               new String(((PropertyData)dataManager.getItemData(childHistory, new QPathEntry(
                  Constants.JCR_VERSIONABLEUUID, 0), ItemType.PROPERTY)).getValues().get(0).getAsByteArray());

         }
         catch (IOException e)
         {
            throw new RepositoryException("jcr:childVersionHistory, error of data read "
               + userSession.getLocationFactory().createJCRPath(cvhpPropPath).getAsString(false), e);
         }

         NodeData versionable = (NodeData)dataManager.getItemData(versionableIdentifier);
         if (versionable != null)
         {
            // exists,
            // On restore of VN, if the workspace currently has an already
            // existing node corresponding to C’s version history and the
            // removeExisting flag of the restore is set to true, then that
            // instance of C becomes the child of the restored N.
            if (!removeExisting)
            {
               throw new ItemExistsException("Item with the same UUID " + versionableIdentifier
                  + " as versionable child node "
                  + userSession.getLocationFactory().createJCRPath(versionable.getQPath()).getAsString(false)
                  + " already exists and removeExisting=false");
            }
            // else - leaving existed unchanged
         }
         else
         {
            // not found, gets last version (by time of creation) and restore it
            NodeData lastVersionData = childHistory.getLastVersionData();
            NodeData cvFrozen =
               (NodeData)dataManager.getItemData(lastVersionData, new QPathEntry(Constants.JCR_FROZENNODE, 1),
                  ItemType.NODE);

            ItemDataRestoreVisitor restoreVisitor =
               new ItemDataRestoreVisitor(currentNode(), qname, childHistory, userSession, removeExisting, changes);
            cvFrozen.accept(restoreVisitor);
            changes.addAll(restoreVisitor.getRestoreChanges().getAllStates());
         }
         pushCurrent(null); // skip any childs of that node
      }
      else if (currentNode() != null)
      {
         // ordinary node for copy under nt:frozenNode
         // [PN] 10.04.06 In case of COPY - copy node, otherwise we don't
         // 8.2.11.3 INITIALIZE; 8.2.11.4 COMPUTE
         // On restore of VN, the C stored as its child will be ignored, and the
         // current C in the workspace will be left unchanged.

         int action =
            nodeTypeDataManager.getChildNodeDefinition(qname, currentNode().getPrimaryTypeName(),
               currentNode().getMixinTypeNames()).getOnParentVersion();

         if (log.isDebugEnabled())
         {
            log.debug("Stored node " + frozen.getQPath().getAsString() + ", "
               + OnParentVersionAction.nameFromValue(action));
         }

         if (action == OnParentVersionAction.COPY || action == OnParentVersionAction.VERSION)
         {
            // copy
            QPath restoredPath =
               QPath.makeChildPath(currentNode().getQPath(), frozen.getQPath().getName(), frozen.getQPath().getIndex());

            // jcr:uuid
            String jcrUuid = null;
            NodeData existing = null;
            if (nodeTypeDataManager.isNodeType(Constants.MIX_REFERENCEABLE, frozen.getPrimaryTypeName(),
               frozen.getMixinTypeNames()))
            {
               // copy uuid from frozen state of mix:referenceable,
               // NOTE: mix:referenceable stored in frozen state with genereted ID
               // (JCR_XITEM PK) as UUID must be unique,
               // but jcr:uuid property containts real UUID.
               QPath jcrUuidPath = QPath.makeChildPath(frozen.getQPath(), Constants.JCR_UUID);
               try
               {
                  jcrUuid =
                     new String(((PropertyData)dataManager.getItemData(frozen, new QPathEntry(Constants.JCR_UUID, 0),
                        ItemType.PROPERTY)).getValues().get(0).getAsByteArray());

               }
               catch (IOException e)
               {
                  throw new RepositoryException("jcr:uuid, error of data read "
                     + userSession.getLocationFactory().createJCRPath(jcrUuidPath).getAsString(false), e);
               }
               existing = (NodeData)dataManager.getItemData(jcrUuid);
            }
            else
            {
               // try to use existing node uuid, otherwise to generate one new
               existing =
                  (NodeData)dataManager.getItemData(currentNode(), new QPathEntry(frozen.getQPath().getName(), frozen
                     .getQPath().getIndex()), ItemType.NODE);
               if (existing != null)
               {
                  jcrUuid = existing.getIdentifier();
               }
               else
               {
                  jcrUuid = IdGenerator.generate();
               }
            }

            if (existing != null && !existing.getQPath().isDescendantOf(restored.getQPath()))
            {
               NodeData existingDelegared = (NodeData)findDelegated(existing.getQPath());
               if (existingDelegared != null)
               {
                  // was restored by previous restore (Workspace.restore(...)), remove
                  // it from delegated log
                  deleteDelegated(existing.getQPath());
               }
               else
               {
                  // exists in workspace
                  if (removeExisting)
                  {
                     if (changes.getItemState(existing.getIdentifier(), ItemState.DELETED) == null)
                     {
                        // remove existed node, with validation (same as for restored
                        // root)
                        RemoveVisitor removeVisitor = new RemoveVisitor();
                        removeVisitor.visit(existing);

                        changes.addAll(removeVisitor.getRemovedStates());
                        updatingPath.addAll(removeVisitor.getUpdatingPath());
                     }
                  }
                  else
                  {
                     throw new ItemExistsException("Node with the same UUID as restored child node "
                        + userSession.getLocationFactory().createJCRPath(restoredPath).getAsString(false)
                        + " already exists and removeExisting=false. Existed "
                        + userSession.getLocationFactory().createJCRPath(existing.getQPath()).getAsString(false) + " "
                        + existing.getIdentifier());
                  }
               }
            }

            AccessControlList acl = currentNode().getACL();

            boolean isPrivilegeable =
               nodeTypeDataManager.isNodeType(Constants.EXO_PRIVILEGEABLE, frozen.getPrimaryTypeName(),
                  frozen.getMixinTypeNames());

            boolean isOwneable =
               nodeTypeDataManager.isNodeType(Constants.EXO_OWNEABLE, frozen.getPrimaryTypeName(),
                  frozen.getMixinTypeNames());

            if (isPrivilegeable || isOwneable)
            {
               List<AccessControlEntry> permissionEntries = new ArrayList<AccessControlEntry>();
               permissionEntries.addAll((isPrivilegeable ? frozen.getACL() : currentNode().getACL())
                  .getPermissionEntries());

               String owner = isOwneable ? frozen.getACL().getOwner() : currentNode().getACL().getOwner();

               acl = new AccessControlList(owner, permissionEntries);
            }

            NodeData restoredData =
               new TransientNodeData(restoredPath, jcrUuid, frozen.getPersistedVersion(), frozen.getPrimaryTypeName(),
                  frozen.getMixinTypeNames(), frozen.getOrderNumber(), currentNode().getIdentifier(), acl);

            if (updatingPath.contains(restoredPath))
            {
               changes.add(ItemState.createUpdatedState(restoredData));
            }
            else
            {
               changes.add(ItemState.createAddedState(restoredData));
            }

            pushCurrent(restoredData);
         }
         else if (action == OnParentVersionAction.INITIALIZE || action == OnParentVersionAction.COMPUTE)
         {
            // current C in the workspace will be left unchanged,
            NodeData existed =
               (NodeData)dataManager.getItemData(currentNode(), new QPathEntry(frozen.getQPath().getName(), 0),
                  ItemType.NODE);

            if (existed != null)
            {
               // copy existed - i.e. left unchanged
               ItemDataCopyVisitor copyVisitor = new ItemDataCopyVisitor(currentNode(), frozen.getQPath().getName(),
               // node,
                  nodeTypeDataManager, userSession.getTransientNodesManager(), true);
               existed.accept(copyVisitor);
               changes.addAll(copyVisitor.getItemAddStates());
            } // else - nothing to do, i.e. left unchanged

            pushCurrent(null); // JCR-193, skip any childs of that node now
         }
      }
   }

   @Override
   protected void entering(PropertyData property, int level) throws RepositoryException
   {

      // TODO what to do if REFERENCE property target doesn't exists in workspace
      if (currentNode() != null)
      {
         NodeData frozenParent = (NodeData)dataManager.getItemData(property.getParentIdentifier());

         InternalQName qname = property.getQPath().getName();

         if (nodeTypeDataManager.isNodeType(Constants.NT_FROZENNODE, frozenParent.getPrimaryTypeName()))
            if (qname.equals(Constants.JCR_FROZENPRIMARYTYPE))
            {
               qname = Constants.JCR_PRIMARYTYPE;
            }
            else if (qname.equals(Constants.JCR_FROZENUUID))
            {
               qname = Constants.JCR_UUID;
            }
            else if (qname.equals(Constants.JCR_FROZENMIXINTYPES))
            {
               qname = Constants.JCR_MIXINTYPES;
            }
            else if (qname.equals(Constants.JCR_PRIMARYTYPE) || qname.equals(Constants.JCR_UUID)
               || qname.equals(Constants.JCR_MIXINTYPES))
            {
               // skip these props, as they are a nt:frozenNode special props
               return;
            }

         int action =
            nodeTypeDataManager
               .getPropertyDefinitions(qname, currentNode().getPrimaryTypeName(), currentNode().getMixinTypeNames())
               .getAnyDefinition().getOnParentVersion();

         if (log.isDebugEnabled())
         {
            log.debug("Visit property " + property.getQPath().getAsString() + " "
               + currentNode().getQPath().getAsString() + " " + OnParentVersionAction.nameFromValue(action));
         }

         if (action == OnParentVersionAction.COPY || action == OnParentVersionAction.VERSION
            || action == OnParentVersionAction.INITIALIZE || action == OnParentVersionAction.COMPUTE)
         {
            // In case of COPY, VERSION - copy property

            PropertyData tagetProperty = null;
            if (qname.equals(Constants.JCR_PREDECESSORS))
            {
               tagetProperty =
                  TransientPropertyData.createPropertyData(currentNode(), qname, property.getType(),
                     property.isMultiValued(), new ArrayList<ValueData>());
            }
            else
            {
               tagetProperty =
                  TransientPropertyData.createPropertyData(currentNode(), qname, property.getType(),
                     property.isMultiValued(), copyValues(property));
            }

            changes.add(ItemState.createAddedState(tagetProperty));
         }
         else if (log.isDebugEnabled())
         {
            // else - nothing to do, i.e. left unchanged
            log.debug("Visit property " + property.getQPath().getAsString() + " HAS "
               + OnParentVersionAction.nameFromValue(action) + " action");
         }
      }
      else if (log.isDebugEnabled())
      {
         log.debug("Visit property " + property.getQPath().getAsString()
            + " HAS NULL PARENT. Restore of this property is impossible.");
      }
   }

   @Override
   protected void leaving(NodeData frozen, int level) throws RepositoryException
   {
      InternalQName qname = frozen.getQPath().getName();

      if (qname.equals(Constants.JCR_FROZENNODE) && level == 0)
      {

         if (log.isDebugEnabled())
         {
            log.debug("leaving jcr:frozenNode " + frozen.getQPath().getAsString());
         }

         // post init of a restored node

         PropertyData baseVersion =
            TransientPropertyData.createPropertyData(restored, Constants.JCR_BASEVERSION, PropertyType.REFERENCE,
               false, new TransientValueData(frozen.getParentIdentifier()));

         PropertyData isCheckedOut =
            TransientPropertyData.createPropertyData(restored, Constants.JCR_ISCHECKEDOUT, PropertyType.BOOLEAN, false,
               new TransientValueData(false));

         NodeData existing = (NodeData)dataManager.getItemData(restored.getIdentifier());
         if (existing != null && !existing.getQPath().isDescendantOf(Constants.JCR_VERSION_STORAGE_PATH))
         {
            // copy childs/properties with OnParentVersionAction.IGNORE to the
            // restored node
            ItemDataCopyIgnoredVisitor copyIgnoredVisitor =
               new ItemDataCopyIgnoredVisitor((NodeData)dataManager.getItemData(restored.getParentIdentifier()),
                  restored.getQPath().getName(), nodeTypeDataManager, userSession.getTransientNodesManager(), changes);

            existing.accept(copyIgnoredVisitor);
            changes.addAll(copyIgnoredVisitor.getItemAddStates());
         }

         changes.add(ItemState.createAddedState(baseVersion));
         changes.add(ItemState.createAddedState(isCheckedOut));
      }

      if (parents.size() <= 0)
      {
         log.error("Empty parents stack");
      }

      parents.pop();
   }

   @Override
   protected void leaving(PropertyData property, int level) throws RepositoryException
   {
   }

   public SessionChangesLog getRestoreChanges()
   {
      return changes;
   }

   public NodeData getRestoreRoot()
   {
      return restored;
   }
}
