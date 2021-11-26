/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.impl.core;

import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.AccessControlPolicy;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ExtendedPropertyType;
import org.exoplatform.services.jcr.dataflow.DataManager;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.CacheableWorkspaceDataManager;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.IdentityConstants;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS. <br> Default workspace intializer. <br>
 * Can be configured with root-nodetype and root-permissions parameters. If
 * root-nodetype and root-permissions are empty then <br> root-nodetype =
 * nt:unstructured <br> root-permissions = ACL default <br> values will be
 * applied.
 * 
 * @author Gennady Azarenkov
 * @version $Id: ScratchWorkspaceInitializer.java 13986 2008-05-08 10:48:43Z
 *          pnedonosko $
 */

public class ScratchWorkspaceInitializer implements WorkspaceInitializer
{

   protected static final Log log = ExoLogger.getLogger("exo.jcr.component.core.ScratchWorkspaceInitializer");

   private final String systemWorkspaceName;

   private final String workspaceName;

   private final DataManager dataManager;

   private final String accessControlType;

   // private final NamespaceDataPersister nsPersister;

   private final String rootPermissions;

   private final InternalQName rootNodeType;

   public ScratchWorkspaceInitializer(WorkspaceEntry config, RepositoryEntry repConfig,
      CacheableWorkspaceDataManager dataManager, LocationFactory locationFactory)
      throws RepositoryConfigurationException, PathNotFoundException, RepositoryException
   {

      this.systemWorkspaceName = repConfig.getSystemWorkspaceName();
      this.accessControlType = repConfig.getAccessControl();
      this.workspaceName = config.getName();

      // workspace root params
      String rootPermissions = null;
      String rootNodeType = null;
      if (config.getInitializer() != null)
      {
         // use user configuration for initializer
         rootPermissions =
            config.getInitializer().getParameterValue(WorkspaceInitializer.ROOT_PERMISSIONS_PARAMETER, null);
         rootNodeType = config.getInitializer().getParameterValue(WorkspaceInitializer.ROOT_NODETYPE_PARAMETER, null);
      }

      // default behaviour root-nodetype=nt:unstructured, root-permissions will be
      // managed by
      // AccessControlList class
      this.rootPermissions = rootPermissions;
      this.rootNodeType =
         rootNodeType != null ? locationFactory.parseJCRName(rootNodeType).getInternalName()
            : Constants.NT_UNSTRUCTURED;

      this.dataManager = dataManager;
   }

   public NodeData initWorkspace() throws RepositoryException
   {

      if (isWorkspaceInitialized())
      {
         return (NodeData)dataManager.getItemData(Constants.ROOT_UUID);
      }

      NodeData root = initRootNode(rootNodeType);

      if (log.isDebugEnabled())
         log.debug("Root node for " + workspaceName + " initialized. NodeType: " + rootNodeType + " system workspace: "
            + systemWorkspaceName);

      // Init system workspace
      if (workspaceName.equals(systemWorkspaceName))
      {
         // initialize /jcr:system
         NodeData sys = initJcrSystemNode(root);
      }

      return root;
   }

   /**
    * Workspace jobs. Will start after the repository initialization.
    */
   public void start()
   {
   }

   public boolean isWorkspaceInitialized() throws RepositoryException
   {
      try
      {
         return dataManager.getItemData(Constants.ROOT_UUID) != null;
      }
      catch (RepositoryException e)
      {
         throw new RepositoryException("Cannot check if the workspace '" + workspaceName
            + "' has already been initialized", e);
      }
   }

   private NodeData initRootNode(InternalQName rootNodeType) throws RepositoryException
   {

      boolean addACL = !accessControlType.equals(AccessControlPolicy.DISABLE);

      PlainChangesLog changesLog = new PlainChangesLogImpl();
      TransientNodeData rootNode;

      if (addACL)
      {
         AccessControlList acl = new AccessControlList();

         if (rootPermissions != null)
         {
            acl.removePermissions(IdentityConstants.ANY);
            acl.addPermissions(rootPermissions);
         }

         InternalQName[] mixins = new InternalQName[]{Constants.EXO_OWNEABLE, Constants.EXO_PRIVILEGEABLE};

         rootNode =
            new TransientNodeData(Constants.ROOT_PATH, Constants.ROOT_UUID, -1, rootNodeType, mixins, 0, null, acl);
         changesLog.add(new ItemState(rootNode, ItemState.ADDED, false, null));

         TransientPropertyData primaryType =
            new TransientPropertyData(QPath.makeChildPath(rootNode.getQPath(), Constants.JCR_PRIMARYTYPE), IdGenerator
               .generate(), -1, PropertyType.NAME, rootNode.getIdentifier(), false,
               new TransientValueData(rootNodeType));

         changesLog.add(new ItemState(primaryType, ItemState.ADDED, false, null)); // 

         // jcr:mixinTypes
         List<ValueData> mixValues = new ArrayList<ValueData>();
         for (InternalQName mixin : mixins)
         {
            mixValues.add(new TransientValueData(mixin));
         }
         TransientPropertyData exoMixinTypes =
            TransientPropertyData.createPropertyData(rootNode, Constants.JCR_MIXINTYPES, PropertyType.NAME, true,
               mixValues);

         TransientPropertyData exoOwner =
            TransientPropertyData.createPropertyData(rootNode, Constants.EXO_OWNER, PropertyType.STRING, false,
               new TransientValueData(acl.getOwner()));

         List<ValueData> permsValues = new ArrayList<ValueData>();
         for (int i = 0; i < acl.getPermissionEntries().size(); i++)
         {
            AccessControlEntry entry = acl.getPermissionEntries().get(i);
            permsValues.add(new TransientValueData(entry));
         }
         TransientPropertyData exoPerms =
            TransientPropertyData.createPropertyData(rootNode, Constants.EXO_PERMISSIONS,
               ExtendedPropertyType.PERMISSION, true, permsValues);

         changesLog.add(ItemState.createAddedState(exoMixinTypes)).add(ItemState.createAddedState(exoOwner)).add(
            ItemState.createAddedState(exoPerms));
         changesLog.add(new ItemState(rootNode, ItemState.MIXIN_CHANGED, false, null));
      }
      else
      {
         rootNode =
            new TransientNodeData(Constants.ROOT_PATH, Constants.ROOT_UUID, -1, rootNodeType, new InternalQName[0], 0,
               null, new AccessControlList());
         changesLog.add(new ItemState(rootNode, ItemState.ADDED, false, null));

         TransientPropertyData primaryType =
            new TransientPropertyData(QPath.makeChildPath(rootNode.getQPath(), Constants.JCR_PRIMARYTYPE), IdGenerator
               .generate(), -1, PropertyType.NAME, rootNode.getIdentifier(), false,
               new TransientValueData(rootNodeType));
         changesLog.add(new ItemState(primaryType, ItemState.ADDED, false, null)); // 
      }

      dataManager.save(new TransactionChangesLog(changesLog));
      return rootNode;
   }

   private NodeData initJcrSystemNode(NodeData root) throws RepositoryException
   {
      boolean addACL = !accessControlType.equals(AccessControlPolicy.DISABLE);

      PlainChangesLog changesLog = new PlainChangesLogImpl();
      TransientNodeData jcrSystem;

      if (addACL)
      {
         InternalQName[] mixins = new InternalQName[]{Constants.EXO_OWNEABLE, Constants.EXO_PRIVILEGEABLE};

         jcrSystem =
            TransientNodeData.createNodeData(root, Constants.JCR_SYSTEM, Constants.NT_UNSTRUCTURED, mixins,
               Constants.SYSTEM_UUID);

         AccessControlList acl = jcrSystem.getACL();

         TransientPropertyData primaryType =
            TransientPropertyData.createPropertyData(jcrSystem, Constants.JCR_PRIMARYTYPE, PropertyType.NAME, false,
               new TransientValueData(jcrSystem.getPrimaryTypeName()));

         changesLog.add(ItemState.createAddedState(jcrSystem)).add(ItemState.createAddedState(primaryType));

         // jcr:mixinTypes
         List<ValueData> mixValues = new ArrayList<ValueData>();
         for (InternalQName mixin : mixins)
         {
            mixValues.add(new TransientValueData(mixin));
         }
         TransientPropertyData exoMixinTypes =
            TransientPropertyData.createPropertyData(jcrSystem, Constants.JCR_MIXINTYPES, PropertyType.NAME, true,
               mixValues);

         TransientPropertyData exoOwner =
            TransientPropertyData.createPropertyData(jcrSystem, Constants.EXO_OWNER, PropertyType.STRING, false,
               new TransientValueData(acl.getOwner()));

         List<ValueData> permsValues = new ArrayList<ValueData>();
         for (int i = 0; i < acl.getPermissionEntries().size(); i++)
         {
            AccessControlEntry entry = acl.getPermissionEntries().get(i);
            permsValues.add(new TransientValueData(entry));
         }
         TransientPropertyData exoPerms =
            TransientPropertyData.createPropertyData(jcrSystem, Constants.EXO_PERMISSIONS,
               ExtendedPropertyType.PERMISSION, true, permsValues);

         changesLog.add(ItemState.createAddedState(exoMixinTypes)).add(ItemState.createAddedState(exoOwner)).add(
            ItemState.createAddedState(exoPerms));
         changesLog.add(new ItemState(jcrSystem, ItemState.MIXIN_CHANGED, false, null));
      }
      else
      {
         jcrSystem =
            TransientNodeData.createNodeData(root, Constants.JCR_SYSTEM, Constants.NT_UNSTRUCTURED,
               Constants.SYSTEM_UUID);

         TransientPropertyData primaryType =
            TransientPropertyData.createPropertyData(jcrSystem, Constants.JCR_PRIMARYTYPE, PropertyType.NAME, false,
               new TransientValueData(jcrSystem.getPrimaryTypeName()));

         changesLog.add(ItemState.createAddedState(jcrSystem)).add(ItemState.createAddedState(primaryType));
      }

      // init version storage
      AccessControlList acl = new AccessControlList();
      acl.removePermissions(IdentityConstants.ANY);
      acl.addPermissions(IdentityConstants.ANY, new String[]{PermissionType.READ});

      for (AccessControlEntry entry : jcrSystem.getACL().getPermissionEntries())
      {
         String identity = entry.getIdentity();
         String permission = entry.getPermission();

         if (!identity.equals(IdentityConstants.ANY) || !permission.equals(PermissionType.READ))
         {
            acl.addPermissions(identity, new String[]{permission});
         }
      }

      TransientNodeData versionStorageNodeData =
         TransientNodeData.createNodeData(jcrSystem, Constants.JCR_VERSIONSTORAGE, Constants.EXO_VERSIONSTORAGE,
            Constants.VERSIONSTORAGE_UUID, acl);

      TransientPropertyData vsPrimaryType =
         TransientPropertyData.createPropertyData(versionStorageNodeData, Constants.JCR_PRIMARYTYPE, PropertyType.NAME,
            false, new TransientValueData(versionStorageNodeData.getPrimaryTypeName()));

      TransientPropertyData exoMixinTypes =
         TransientPropertyData.createPropertyData(versionStorageNodeData, Constants.JCR_MIXINTYPES, PropertyType.NAME,
            true, new TransientValueData(Constants.EXO_PRIVILEGEABLE));

      List<ValueData> permsValues = new ArrayList<ValueData>();
      for (int i = 0; i < acl.getPermissionEntries().size(); i++)
      {
         AccessControlEntry entry = acl.getPermissionEntries().get(i);
         permsValues.add(new TransientValueData(entry));
      }
      TransientPropertyData exoPerms =
         TransientPropertyData.createPropertyData(versionStorageNodeData, Constants.EXO_PERMISSIONS,
            ExtendedPropertyType.PERMISSION, true, permsValues);

      changesLog.add(ItemState.createAddedState(versionStorageNodeData));
      changesLog.add(ItemState.createAddedState(vsPrimaryType));
      changesLog.add(ItemState.createAddedState(exoMixinTypes));
      changesLog.add(ItemState.createAddedState(exoPerms));
      changesLog.add(new ItemState(versionStorageNodeData, ItemState.MIXIN_CHANGED, false, null));

      dataManager.save(new TransactionChangesLog(changesLog));

      return jcrSystem;
   }

   public void stop()
   {
   }
}
