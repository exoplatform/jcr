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
/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id$
 */
package org.exoplatform.services.jcr.impl.core.nodetype.registration;

import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.AccessControlPolicy;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ExtendedPropertyType;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.dataflow.DataManager;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.ItemDataRemoveVisitor;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id$
 */
public class JCRNodeTypeDataPersister implements NodeTypeDataPersister
{

   protected final Log log = ExoLogger.getLogger("exo.jcr.component.core.JCRNodeTypeDataPersister");

   private final DataManager dataManager;

   private NodeData nodeTypeStorageRoot;

   private final NodeTypeDefinitionAccessProvider definitionAccessProvider;

   private final boolean addACL;

   private boolean started = false;

   /**
    * @param dataManager
    * @throws RepositoryException
    */
   public JCRNodeTypeDataPersister(DataManager dataManager, boolean addACL) throws RepositoryException
   {
      super();
      this.dataManager = dataManager;
      this.addACL = addACL;

      this.definitionAccessProvider = new NodeTypeDefinitionAccessProvider(dataManager);
   }

   /**
    * @param dataManager
    * @throws RepositoryException
    */
   public JCRNodeTypeDataPersister(DataManager dataManager, NodeData nodeTypeStorageRoot) throws RepositoryException
   {
      super();
      this.dataManager = dataManager;
      this.nodeTypeStorageRoot = nodeTypeStorageRoot;
      this.definitionAccessProvider = new NodeTypeDefinitionAccessProvider(dataManager);
      this.addACL = true;

   }

   /**
    * @param dataManager
    * @throws RepositoryException
    */
   public JCRNodeTypeDataPersister(DataManager dataManager, RepositoryEntry repConfig) throws RepositoryException
   {
      this(dataManager, !repConfig.getAccessControl().equals(AccessControlPolicy.DISABLE));
   }

   /**
    * {@inheritDoc}
    */
   public void addNodeType(NodeTypeData nodeType) throws RepositoryException
   {
      if (!started)
      {
         log.warn("Unable save nodetype " + nodeType.getName().getAsString()
            + " in to the storage. Storage not initialized");
         return;
      }

      PlainChangesLog changesLog = new PlainChangesLogImpl();
      definitionAccessProvider.write(changesLog, nodeTypeStorageRoot, nodeType);
      dataManager.save(new TransactionChangesLog(changesLog));
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasNodeType(InternalQName nodeTypeName) throws RepositoryException
   {
      if (!validatate())
      {
         return false;
      }

      NodeData nodeTypeData =
         (NodeData)dataManager.getItemData(nodeTypeStorageRoot, new QPathEntry(nodeTypeName, 1), ItemType.NODE);

      return nodeTypeData != null;
   }

   public NodeData initNodetypesRoot(NodeData nsSystem, boolean addACL) throws RepositoryException
   {
      PlainChangesLog changesLog = new PlainChangesLogImpl();
      TransientNodeData jcrNodetypes;

      long start = System.currentTimeMillis();

      if (addACL)
      {
         AccessControlList acl = new AccessControlList();
         InternalQName[] mixins = new InternalQName[]{Constants.EXO_OWNEABLE, Constants.EXO_PRIVILEGEABLE};

         jcrNodetypes =
            TransientNodeData.createNodeData(nsSystem, Constants.JCR_NODETYPES, Constants.NT_UNSTRUCTURED, mixins,
               Constants.NODETYPESROOT_UUID);

         TransientPropertyData primaryType =
            TransientPropertyData.createPropertyData(jcrNodetypes, Constants.JCR_PRIMARYTYPE, PropertyType.NAME, false,
               new TransientValueData(jcrNodetypes.getPrimaryTypeName()));

         changesLog.add(ItemState.createAddedState(jcrNodetypes)).add(ItemState.createAddedState(primaryType));

         // jcr:mixinTypes
         List<ValueData> mixValues = new ArrayList<ValueData>();
         for (InternalQName mixin : mixins)
         {
            mixValues.add(new TransientValueData(mixin));
         }
         TransientPropertyData exoMixinTypes =
            TransientPropertyData.createPropertyData(jcrNodetypes, Constants.JCR_MIXINTYPES, PropertyType.NAME, true,
               mixValues);

         TransientPropertyData exoOwner =
            TransientPropertyData.createPropertyData(jcrNodetypes, Constants.EXO_OWNER, PropertyType.STRING, false,
               new TransientValueData(acl.getOwner()));

         List<ValueData> permsValues = new ArrayList<ValueData>();
         for (int i = 0; i < acl.getPermissionEntries().size(); i++)
         {
            AccessControlEntry entry = acl.getPermissionEntries().get(i);
            permsValues.add(new TransientValueData(entry));
         }
         TransientPropertyData exoPerms =
            TransientPropertyData.createPropertyData(jcrNodetypes, Constants.EXO_PERMISSIONS,
               ExtendedPropertyType.PERMISSION, true, permsValues);

         changesLog.add(ItemState.createAddedState(exoMixinTypes)).add(ItemState.createAddedState(exoOwner)).add(
            ItemState.createAddedState(exoPerms));
         changesLog.add(new ItemState(jcrNodetypes, ItemState.MIXIN_CHANGED, false, null));
      }
      else
      {
         jcrNodetypes =
            TransientNodeData.createNodeData(nsSystem, Constants.JCR_NODETYPES, Constants.NT_UNSTRUCTURED,
               Constants.NODETYPESROOT_UUID);

         TransientPropertyData primaryType =
            TransientPropertyData.createPropertyData(jcrNodetypes, Constants.JCR_PRIMARYTYPE, PropertyType.NAME, false,
               new TransientValueData(jcrNodetypes.getPrimaryTypeName()));

         changesLog.add(ItemState.createAddedState(jcrNodetypes)).add(ItemState.createAddedState(primaryType));
      }

      if (log.isDebugEnabled())
      {
         log.debug("/jcr:system/jcr:nodetypes is created, creation time: " + (System.currentTimeMillis() - start)
            + " ms");
      }

      dataManager.save(new TransactionChangesLog(changesLog));

      return jcrNodetypes;

   }

   /**
    * {@inheritDoc}
    */
   public boolean isStorageFilled()
   {
      if (nodeTypeStorageRoot == null)
      {
         log.warn(" Storage not initialized");
         return false;
      }
      try
      {
         List<NodeData> storageContent = dataManager.getChildNodesData(nodeTypeStorageRoot);
         return storageContent.size() > 0;
      }
      catch (RepositoryException e)
      {
         log.error(e.getLocalizedMessage(), e);

      }
      return false;
   }

   /**
    * {@inheritDoc}
    */
   public void addNodeTypes(List<NodeTypeData> nodeTypes) throws RepositoryException
   {
      if (!validatate())
      {
         return;
      }

      PlainChangesLog changesLog = new PlainChangesLogImpl();
      for (NodeTypeData nodeTypeData : nodeTypes)
      {
         definitionAccessProvider.write(changesLog, nodeTypeStorageRoot, nodeTypeData);
      }

      dataManager.save(new TransactionChangesLog(changesLog));

   }

   /**
    * {@inheritDoc}
    */
   public void removeNodeType(NodeTypeData nodeType) throws RepositoryException
   {
      if (!validatate())
      {
         return;
      }

      validatate();
      NodeData nodeTypeData =
         (NodeData)dataManager.getItemData(nodeTypeStorageRoot, new QPathEntry(nodeType.getName(), 1), ItemType.NODE);
      ItemDataRemoveVisitor removeVisitor = new ItemDataRemoveVisitor(dataManager, nodeTypeStorageRoot.getQPath());
      nodeTypeData.accept(removeVisitor);

      PlainChangesLog changesLog = new PlainChangesLogImpl();
      changesLog.addAll(removeVisitor.getRemovedStates());
      dataManager.save(new TransactionChangesLog(changesLog));
   }

   public void start()
   {
      if (!started)
      {
         try
         {
            NodeData jcrSystem = (NodeData)dataManager.getItemData(Constants.SYSTEM_UUID);
            if (jcrSystem != null)
            {
               NodeData jcrNodetypes =
                  (NodeData)dataManager.getItemData(jcrSystem, new QPathEntry(Constants.JCR_NODETYPES, 1),
                     ItemType.NODE);
               if (jcrNodetypes == null)
               {
                  this.nodeTypeStorageRoot = initNodetypesRoot(jcrSystem, addACL);
               }
               else
               {
                  this.nodeTypeStorageRoot = jcrNodetypes;
               }
            }
            else
            {
               throw new RuntimeException("Nodetypes storage (/jcr:systemnode) is not initialized.");
            }
         }
         catch (RepositoryException e)
         {
            throw new RuntimeException(e.getLocalizedMessage(), e);
         }
         started = true;
      }
   }

   public void stop()
   {
   }

   /**
    * @see org.exoplatform.services.jcr.impl.core.nodetype.registration.NodeTypeDataPersister#unmarshall(org.exoplatform.services.jcr.datamodel.InternalQName, java.util.Set)
    */
   public NodeTypeData getNodeType(InternalQName nodeTypeName) throws RepositoryException, NoSuchNodeTypeException
   {
      if (nodeTypeStorageRoot == null)
      {
         log.warn(" Storage not initialized");
         return null;
      }
      //Searching nodeType root
      ItemData nodeType = dataManager.getItemData(nodeTypeStorageRoot, new QPathEntry(nodeTypeName, 1), ItemType.NODE);
      if (nodeType == null)
         throw new NoSuchNodeTypeException("Node type definition " + nodeTypeName.getAsString() + "not found");
      if (!nodeType.isNode())
         throw new RepositoryException("Unexpected property found " + nodeType.getQPath().getAsString()
            + ". Should be node.");

      NodeData nodeTypeRoot = (NodeData)nodeType;

      if (!Constants.NT_NODETYPE.equals(nodeTypeRoot.getPrimaryTypeName()))
         throw new RepositoryException("Unexpected node type of NodeData found "
            + nodeTypeRoot.getPrimaryTypeName().getAsString() + ". Should be " + Constants.NT_NODETYPE.getAsString());

      return definitionAccessProvider.read(nodeTypeRoot);
   }

   /**
    * @see org.exoplatform.services.jcr.impl.core.nodetype.registration.NodeTypeDataPersister#getNodeType(java.util.Set)
    */
   public List<NodeTypeData> getAllNodeTypes() throws RepositoryException
   {
      if (!validatate())
      {
         return new ArrayList<NodeTypeData>();
      }

      validatate();
      List<NodeData> nodeTypes = dataManager.getChildNodesData(nodeTypeStorageRoot);
      List<NodeTypeData> result = new ArrayList<NodeTypeData>();
      for (NodeData nodeData : nodeTypes)
      {
         if (Constants.NT_NODETYPE.equals(nodeData.getPrimaryTypeName()))
         {
            result.add(definitionAccessProvider.read(nodeData));
         }
      }
      return result;
   }

   /**
    * {@inheritDoc}
    */
   public void update(List<NodeTypeData> nodeTypes, UpdateNodeTypeObserver observer) throws RepositoryException
   {

      PlainChangesLog changesLog = new PlainChangesLogImpl();

      for (NodeTypeData nodeTypeData : nodeTypes)
      {

         if (observer != null)
         {
            if (observer.shouldSkip(nodeTypeData, changesLog))
            {
               continue;
            }
            observer.beforeUpdate(nodeTypeData, changesLog);
         }
         if (!validatate())
         {
            continue;
         }
         // remove first
         NodeData removeNodeTypeData =
            (NodeData)dataManager.getItemData(nodeTypeStorageRoot, new QPathEntry(nodeTypeData.getName(), 1),
               ItemType.NODE);
         if (removeNodeTypeData != null)
         {
            ItemDataRemoveVisitor removeVisitor =
               new ItemDataRemoveVisitor(dataManager, nodeTypeStorageRoot.getQPath());
            removeNodeTypeData.accept(removeVisitor);

            changesLog.addAll(removeVisitor.getRemovedStates());
         }
         // add
         definitionAccessProvider.write(changesLog, nodeTypeStorageRoot, nodeTypeData);
         if (observer != null)
         {
            observer.afterUpdate(nodeTypeData, changesLog);
         }
      }
      //made changes if needed
      if (changesLog.getSize() > 0)
      {
         dataManager.save(new TransactionChangesLog(changesLog));
      }
   }

   private boolean validatate()
   {
      if (this.nodeTypeStorageRoot == null)
      {
         if (log.isDebugEnabled())
            log.debug(" Storage not initialized");
         return false;
      }
      return true;
   }
}
