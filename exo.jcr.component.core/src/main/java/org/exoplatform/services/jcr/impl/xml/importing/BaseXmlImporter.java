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
package org.exoplatform.services.jcr.impl.xml.importing;

import org.exoplatform.services.jcr.access.AccessManager;
import org.exoplatform.services.jcr.core.nodetype.NodeDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.datamodel.IllegalPathException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.exoplatform.services.jcr.impl.dataflow.ItemDataRemoveVisitor;
import org.exoplatform.services.jcr.impl.dataflow.version.VersionHistoryDataHelper;
import org.exoplatform.services.jcr.impl.storage.JCRItemExistsException;
import org.exoplatform.services.jcr.impl.xml.VersionHistoryRemover;
import org.exoplatform.services.jcr.impl.xml.importing.dataflow.ImportItemData;
import org.exoplatform.services.jcr.impl.xml.importing.dataflow.ImportNodeData;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: BaseXmlImporter.java 14221 2008-05-14 08:27:41Z ksm $
 */
public abstract class BaseXmlImporter implements ContentImporter
{
   private static final String SESSION_ID = "00base0xml0importer0session0id00";

   protected final AccessManager accessManager;

   protected QPath ancestorToSave;

   protected final PlainChangesLogImpl changesLog;

   protected final Map<String, Object> context;

   protected final String currentWorkspaceName;

   protected final ItemDataConsumer dataConsumer;

   protected boolean isNeedReloadAncestorToSave;

   protected final LocationFactory locationFactory;

   protected final NamespaceRegistry namespaceRegistry;

   protected final NodeTypeDataManager nodeTypeDataManager;

   protected final RepositoryImpl repository;

   protected final Stack<NodeData> tree;

   protected final ConversationState userState;

   protected final int uuidBehavior;

   protected final ValueFactoryImpl valueFactory;

   private final Log log = ExoLogger.getLogger("exo.jcr.component.core.ImporterBase");

   public BaseXmlImporter(NodeData parent, QPath ancestorToSave, int uuidBehavior, ItemDataConsumer dataConsumer,
      NodeTypeDataManager ntManager, LocationFactory locationFactory, ValueFactoryImpl valueFactory,
      NamespaceRegistry namespaceRegistry, AccessManager accessManager, ConversationState userState,
      Map<String, Object> context, RepositoryImpl repository, String currentWorkspaceName)
   {

      this.dataConsumer = dataConsumer;
      this.valueFactory = valueFactory;
      this.namespaceRegistry = namespaceRegistry;
      this.accessManager = accessManager;
      this.userState = userState;

      this.context = context;

      this.nodeTypeDataManager = ntManager;
      this.locationFactory = locationFactory;
      this.uuidBehavior = uuidBehavior;
      this.repository = repository;
      this.currentWorkspaceName = currentWorkspaceName;
      this.tree = new Stack<NodeData>();
      this.tree.push(parent);
      this.changesLog = new PlainChangesLogImpl(SESSION_ID);
      this.ancestorToSave = ancestorToSave;
      this.isNeedReloadAncestorToSave = false;
   }

   /**
    * @return
    */
   public QPath getAncestorToSave()
   {
      return ancestorToSave;
   }

   /*
    * (non-Javadoc)
    * @see
    * org.exoplatform.services.jcr.impl.xml.importing.ContentImporter#getChanges
    * ()
    */
   public PlainChangesLog getChanges()
   {

      Collections.sort(changesLog.getAllStates(), new PathSorter());

      if (log.isDebugEnabled())
      {
         String str = "";
         for (int i = 0; i < changesLog.getAllStates().size(); i++)
            str +=
               " " + ItemState.nameFromValue(changesLog.getAllStates().get(i).getState()) + "\t\t"
                  + changesLog.getAllStates().get(i).getData().getIdentifier() + "\t" + "isPersisted="
                  + changesLog.getAllStates().get(i).isPersisted() + "\t" + "isEventFire="
                  + changesLog.getAllStates().get(i).isEventFire() + "\t" + "isInternallyCreated="
                  + changesLog.getAllStates().get(i).isInternallyCreated() + "\t"
                  + changesLog.getAllStates().get(i).getData().getQPath().getAsString() + "\n";
         log.debug(str);
      }
      if (isNeedReloadAncestorToSave)
      {
         PlainChangesLogImpl newChangesLog = new PlainChangesLogImpl();
         for (ItemState itemState : changesLog.getAllStates())
         {
            newChangesLog.add(new ItemState(itemState.getData(), itemState.getState(), itemState.isEventFire(),
               ancestorToSave, itemState.isInternallyCreated(), itemState.isPersisted(), itemState.getOldPath()));
         }
         changesLog.clear();
         changesLog.addAll(newChangesLog.getAllStates());
         isNeedReloadAncestorToSave = false;
      }
      return changesLog;
   }

   /**
    * @param parentData
    * @return next child order number.
    */
   public int getNextChildOrderNum(NodeData parentData)
   {
      int max = -1;

      for (ItemState itemState : changesLog.getAllStates())
      {
         ItemData stateData = itemState.getData();
         if (isParent(stateData, parentData) && stateData.isNode())
         {
            int cur = ((NodeData)stateData).getOrderNumber();
            if (cur > max)
               max = cur;
         }
      }
      return ++max;
   }

   /**
    * Return new node index.
    * 
    * @param parentData
    * @param name
    * @param skipIdentifier
    * @return
    * @throws PathNotFoundException
    * @throws IllegalPathException
    * @throws RepositoryException
    */
   public int getNodeIndex(NodeData parentData, InternalQName name, String skipIdentifier)
      throws PathNotFoundException, IllegalPathException, RepositoryException
   {

      int newIndex = 1;

      NodeDefinitionData nodedef =
         nodeTypeDataManager.getChildNodeDefinition(name, parentData.getPrimaryTypeName(), parentData
            .getMixinTypeNames());

      ItemData sameNameNode = null;
      try
      {
         sameNameNode = dataConsumer.getItemData(parentData, new QPathEntry(name, 0), ItemType.NODE, false);
      }
      catch (PathNotFoundException e)
      {
         // Ok no same name node;
         return newIndex;
      }

      List<ItemState> transientAddChilds = getItemStatesList(parentData, name, ItemState.ADDED, skipIdentifier);
      List<ItemState> transientDeletedChilds =
         getItemStatesList(parentData, new QPathEntry(name, 0), ItemState.DELETED, null);

      if (!nodedef.isAllowsSameNameSiblings() && ((sameNameNode != null) || (transientAddChilds.size() > 0)))
      {
         if ((sameNameNode != null) && (transientDeletedChilds.size() < 1))
         {
            throw new ItemExistsException("The node  already exists in " + sameNameNode.getQPath().getAsString()
               + " and same name sibling is not allowed ");
         }
         if (transientAddChilds.size() > 0)
         {
            throw new ItemExistsException("The node  already exists in add state "
               + "  and same name sibling is not allowed ");

         }
      }

      newIndex += transientAddChilds.size();

      List<NodeData> existedChilds = dataConsumer.getChildNodesData(parentData);

      // Calculate SNS index for dest root
      for (NodeData child : existedChilds)
      {
         // skeep deleted items
         if (transientDeletedChilds.size() != 0)
         {
            continue;
         }

         if (child.getQPath().getName().equals(name))
         {
            newIndex++; // next sibling index
         }

      }

      // searching
      return newIndex;
   }

   /*
    * (non-Javadoc)
    * @see
    * org.exoplatform.services.jcr.impl.xml.importing.Importer#registerNamespace
    * (java.lang.String, java.lang.String)
    */
   public void registerNamespace(String prefix, String uri)
   {
      try
      {
         namespaceRegistry.getPrefix(uri);
      }
      catch (NamespaceException e)
      {
         try
         {
            namespaceRegistry.registerNamespace(prefix, uri);
         }
         catch (NamespaceException e1)
         {
            throw new RuntimeException(e1);
         }
         catch (RepositoryException e1)
         {
            throw new RuntimeException(e1);
         }
      }
      catch (RepositoryException e)
      {
         throw new RuntimeException(e);
      }

   }

   /**
    * Set new ancestorToSave.
    * 
    * @param ancestorToSave
    */
   public void setAncestorToSave(QPath newAncestorToSave)
   {
      if (!ancestorToSave.equals(newAncestorToSave))
      {
         isNeedReloadAncestorToSave = true;
      }
      this.ancestorToSave = newAncestorToSave;
   }

   /**
    * Create new version history.
    * 
    * @param nodeData
    * @throws RepositoryException
    */
   protected void createVersionHistory(ImportNodeData nodeData) throws RepositoryException
   {
      // Generate new VersionHistoryIdentifier and BaseVersionIdentifier
      // if uuid changed after UC
      boolean newVersionHistory = nodeData.isNewIdentifer() || !nodeData.isContainsVersionhistory();
      if (newVersionHistory)
      {
         nodeData.setVersionHistoryIdentifier(IdGenerator.generate());
         nodeData.setBaseVersionIdentifier(IdGenerator.generate());
      }

      PlainChangesLogImpl changes = new PlainChangesLogImpl();
      // using VH helper as for one new VH, all changes in changes log
      new VersionHistoryDataHelper(nodeData, changes, dataConsumer, nodeTypeDataManager,
         nodeData.getVersionHistoryIdentifier(), nodeData.getBaseVersionIdentifier());

      if (!newVersionHistory)
      {
         for (ItemState state : changes.getAllStates())
         {
            if (!state.getData().getQPath().isDescendantOf(Constants.JCR_SYSTEM_PATH))
            {
               changesLog.add(state);
            }
         }
      }
      else
      {
         changesLog.addAll(changes.getAllStates());
      }
   }

   /**
    * @return parent node.
    */
   protected NodeData getParent()
   {
      return tree.peek();
   }

   /**
    * Check uuid collision. If collision happen reload path information.
    * 
    * @param currentNodeInfo
    * @param olUuid
    * @throws RepositoryException
    */
   protected void checkReferenceable(ImportNodeData currentNodeInfo, String olUuid) throws RepositoryException
   {
      // if node is in version storrage - do not assign new id from jcr:uuid
      // property
      if (Constants.JCR_VERSION_STORAGE_PATH.getDepth() + 3 <= currentNodeInfo.getQPath().getDepth()
         && currentNodeInfo.getQPath().getEntries()[Constants.JCR_VERSION_STORAGE_PATH.getDepth() + 3]
            .equals(Constants.JCR_FROZENNODE)
         && currentNodeInfo.getQPath().isDescendantOf(Constants.JCR_VERSION_STORAGE_PATH))
      {
         return;
      }

      String identifier = validateUuidCollision(olUuid);

      if (identifier != null)
      {
         reloadChangesInfoAfterUC(currentNodeInfo, identifier);
      }
      else
      {
         currentNodeInfo.setIsNewIdentifer(true);
      }
      if (uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING)
         currentNodeInfo.setParentIdentifer(getParent().getIdentifier());

   }

   /**
    * Reload path information after uuid collision
    * 
    * @param currentNodeInfo
    * @param identifier
    * @throws PathNotFoundException
    * @throws IllegalPathException
    * @throws RepositoryException
    */
   protected void reloadChangesInfoAfterUC(ImportNodeData currentNodeInfo, String identifier)
      throws PathNotFoundException, IllegalPathException, RepositoryException
   {
      boolean reloadSNS =
         uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING
            || uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING;
      QPath newPath = null;
      if (reloadSNS)
      {
         NodeData currentParentData = getParent();
         // current node already in list
         int nodeIndex = getNodeIndex(currentParentData, currentNodeInfo.getQName(), currentNodeInfo.getIdentifier());
         newPath = QPath.makeChildPath(currentParentData.getQPath(), currentNodeInfo.getQName(), nodeIndex);
         currentNodeInfo.setQPath(newPath);
      }

      String oldIdentifer = currentNodeInfo.getIdentifier();
      // update parentIdentifer
      for (ItemState state : changesLog.getAllStates())
      {
         ItemData data = state.getData();
         if (data.getParentIdentifier() != null && data.getParentIdentifier().equals(oldIdentifer))
         {
            ((ImportItemData)data).setParentIdentifer(identifier);
            if (reloadSNS)
               ((ImportItemData)data).setQPath(QPath.makeChildPath(newPath, data.getQPath().getName()));

         }

      }

      currentNodeInfo.setIdentifier(identifier);
   }


   /**
    * Check if item with uuid=identifier exists. If no item exist return same
    * identifier. If same uuid item exist and depend on uuidBehavior do:
    * <ol>
    * <li>IMPORT_UUID_CREATE_NEW - return null. Caller will create new
    * identifier.</li>
    * <li>IMPORT_UUID_COLLISION_REMOVE_EXISTING - Remove same uuid item and his
    * subtree. Also if item MIX_VERSIONABLE, remove version history</li>
    * <li>IMPORT_UUID_COLLISION_REPLACE_EXISTING - Remove same uuid item and his
    * subtree. Also if item MIX_VERSIONABLE, remove version history</li>
    * <li>IMPORT_UUID_COLLISION_THROW - throw new ItemExistsException</li>
    * </ol>
    * 
    * @param identifier
    * @return
    * @throws RepositoryException
    */
   protected String validateUuidCollision(final String identifier) throws RepositoryException
   {
      String newIdentifer = identifier;
      if (identifier != null)
      {
         try
         {
            NodeData sameUuidItem = (NodeData)dataConsumer.getItemData(identifier);
            ItemState lastState = getLastItemState(identifier);

            if (sameUuidItem != null && (lastState == null || !lastState.isDeleted()))
            {
               boolean isMixVersionable =
                  nodeTypeDataManager.isNodeType(Constants.MIX_VERSIONABLE, sameUuidItem.getMixinTypeNames());

               switch (uuidBehavior)
               {
                  case ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW :
                     // Incoming referenceable nodes are assigned newly created UUIDs
                     // upon addition to the workspace. As a result UUID collisions
                     // never occur.

                     // reset UUID and it will be autocreated in session
                     newIdentifer = null;
                     break;
                  case ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING :

                     // remove version history before removing item
                     if (isMixVersionable)
                     {
                        removeVersionHistory(sameUuidItem);
                     }
                     removeExisted(sameUuidItem);
                     break;
                  case ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING :
                     // remove version history before removing item
                     if (isMixVersionable)
                     {
                        removeVersionHistory(sameUuidItem);
                     }
                     removeExisted(sameUuidItem);
                     ItemData parentOfsameUuidItem = dataConsumer.getItemData(sameUuidItem.getParentIdentifier());
                     tree.push(ImportNodeData.createCopy((NodeData)parentOfsameUuidItem));
                     break;
                  case ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW :
                     // If an incoming referenceable node has the same UUID as a node
                     // already existing in the workspace then a SAXException is thrown
                     // by the ContentHandler during deserialization.
                     throw new JCRItemExistsException("An incoming referenceable node has the same "
                        + "UUID as a node already existing in the workspace! UUID:" + identifier, identifier);
                  default :
               }
            }
         }
         catch (ItemNotFoundException e)
         {
            // node not found, it's ok - willing create one new
         }

      }
      return newIdentifer;
   }

   /**
    * Return list of changes for item.
    * 
    * @param parentData - parent item
    * @param name - item name
    * @param state - state
    * @param skipIdentifier - skipped identifier.
    * @return
    */
   private List<ItemState> getItemStatesList(NodeData parentData, InternalQName name, int state, String skipIdentifier)
   {
      List<ItemState> states = new ArrayList<ItemState>();
      for (ItemState itemState : changesLog.getAllStates())
      {
         ItemData stateData = itemState.getData();
         if (isParent(stateData, parentData) && stateData.getQPath().getName().equals(name))
         {
            if ((state != 0) && (state != itemState.getState()) || stateData.getIdentifier().equals(skipIdentifier))
            {
               continue;
            }
            states.add(itemState);

         }
      }
      return states;
   }

   /**
    * Return last item state in changes log. If no state exist return null.
    * 
    * @param identifer
    * @return
    */
   protected ItemState getLastItemState(String identifer)
   {
      List<ItemState> allStates = changesLog.getAllStates();
      for (int i = allStates.size() - 1; i >= 0; i--)
      {
         ItemState state = allStates.get(i);
         if (state.getData().getIdentifier().equals(identifer))
            return state;
      }
      return null;
   }

   /**
    * Check if item <b>parent</b> is parent item of item <b>data</b>.
    * 
    * @param data - Possible child ItemData.
    * @param parent - Possible parent ItemData.
    * @return True if parent of both ItemData the same.
    */
   private boolean isParent(ItemData data, ItemData parent)
   {
      String id1 = data.getParentIdentifier();
      String id2 = parent.getIdentifier();
      if (id1 == id2)
         return true;
      if (id1 == null && id2 != null)
         return false;
      return id1.equals(id2);
   }

   /**
    * Remove existed item.
    * 
    * @param sameUuidItem
    * @throws RepositoryException
    * @throws ConstraintViolationException
    * @throws PathNotFoundException
    */
   private void removeExisted(NodeData sameUuidItem) throws RepositoryException, ConstraintViolationException,
      PathNotFoundException
   {

      if (!nodeTypeDataManager.isNodeType(Constants.MIX_REFERENCEABLE, sameUuidItem.getPrimaryTypeName(), sameUuidItem
         .getMixinTypeNames()))
      {
         throw new RepositoryException("An incoming referenceable node has the same "
            + " UUID as a identifier of non mix:referenceable" + " node already existing in the workspace!");
      }

      // If an incoming referenceable node has the same UUID as a node
      // already existing in the workspace then the already existing
      // node (and its subtree) is removed from wherever it may be in
      // the workspace before the incoming node is added. Note that this
      // can result in nodes disappearing from locations in the
      // workspace that are remote from the location to which the
      // incoming subtree is being written.
      // parentNodeData = (NodeData) sameUuidItem.getParent().getData();

      QPath sameUuidPath = sameUuidItem.getQPath();

      if (ancestorToSave.isDescendantOf(sameUuidPath) || ancestorToSave.equals(sameUuidPath))
      {
         throw new ConstraintViolationException("The imported document contains a element"
            + " with jcr:uuid attribute the same as the  parent of the import target.");
      }

      setAncestorToSave(QPath.getCommonAncestorPath(ancestorToSave, sameUuidPath));

      ItemDataRemoveVisitor visitor =
         new ItemDataRemoveVisitor(dataConsumer, getAncestorToSave(), nodeTypeDataManager, accessManager, userState);
      sameUuidItem.accept(visitor);

      changesLog.addAll(visitor.getRemovedStates());
   }

   /**
    * Remove version history of versionable node.
    * 
    * @param mixVersionableNode - node
    * @throws RepositoryException
    * @throws ConstraintViolationException
    * @throws VersionException
    */
   private void removeVersionHistory(NodeData mixVersionableNode) throws RepositoryException,
      ConstraintViolationException, VersionException
   {
      try
      {
         PropertyData vhpd =
            (PropertyData)dataConsumer.getItemData(mixVersionableNode, new QPathEntry(Constants.JCR_VERSIONHISTORY, 1),
               ItemType.PROPERTY);
         try
         {
            String vhID = new String(vhpd.getValues().get(0).getAsByteArray());
            VersionHistoryRemover historyRemover =
               new VersionHistoryRemover(vhID, dataConsumer, nodeTypeDataManager, repository, currentWorkspaceName,
                  null, ancestorToSave, changesLog, accessManager, userState);
            historyRemover.remove();
         }
         catch (IOException e)
         {
            throw new RepositoryException(e);
         }
      }
      catch (IllegalStateException e)
      {
         throw new RepositoryException(e);
      }
   }

   /**
    * Class helps sort ItemStates list. After sorting the delete states has to be
    * on top of the list
    */
   private class PathSorter implements Comparator<ItemState>
   {
      /*
       * (non-Javadoc)
       * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
       */
      public int compare(final ItemState i1, final ItemState i2)
      {
         int sign = 0;
         if (i1.getState() != i2.getState())
         {
            if (i2.isDeleted())
               sign = 1;
            else
               sign = -1;
         }
         return sign;
      }
   }
}
