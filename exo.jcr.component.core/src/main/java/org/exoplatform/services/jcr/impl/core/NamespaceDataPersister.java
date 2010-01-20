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

import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.AccessControlPolicy;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ComponentPersister;
import org.exoplatform.services.jcr.core.ExtendedPropertyType;
import org.exoplatform.services.jcr.dataflow.DataManager;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.TransientItemData;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.dataflow.ValueDataConvertor;
import org.exoplatform.services.jcr.impl.util.NodeDataReader;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady
 *         Azarenkov</a>
 * @version $Id: NamespaceDataPersister.java 13962 2008-05-07 16:00:48Z
 *          pnedonosko $
 */

public class NamespaceDataPersister implements ComponentPersister
{

   public static Log log = ExoLogger.getLogger(NamespaceDataPersister.class.getName());

   private final DataManager dataManager;

   private NodeData nsRoot;

   private final RepositoryEntry repConfig;

   private boolean started = false;

   public NamespaceDataPersister(DataManager dataManager, RepositoryEntry repConfig) throws RepositoryException
   {
      this.dataManager = dataManager;
      this.repConfig = repConfig;
   }

   /**
    * Add new namespace.
    * 
    * @param prefix
    *          NS prefix
    * @param uri
    *          NS URI
    * @throws RepositoryException
    *           Repository error
    */
   public void addNamespace(String prefix, String uri) throws RepositoryException
   {
      if (!started)
      {
         if (log.isDebugEnabled())
            log.debug("Unable save namespace " + uri + "=" + prefix + " in to the storage. Storage not initialized");
         return;
      }
      PlainChangesLog changesLog = new PlainChangesLogImpl();
      internallAdd(changesLog, prefix, uri);

      dataManager.save(new TransactionChangesLog(changesLog));

   }

   private PlainChangesLog internallAdd(PlainChangesLog changesLog, String prefix, String uri)
   {
      TransientNodeData nsNode =
         TransientNodeData.createNodeData(nsRoot, new InternalQName("", prefix), Constants.EXO_NAMESPACE);

      TransientPropertyData primaryType =
         TransientPropertyData.createPropertyData(nsNode, Constants.JCR_PRIMARYTYPE, PropertyType.NAME, false,
            new TransientValueData(nsNode.getPrimaryTypeName()));

      TransientPropertyData exoUri =
         TransientPropertyData.createPropertyData(nsNode, Constants.EXO_URI_NAME, PropertyType.STRING, false,
            new TransientValueData(uri));

      TransientPropertyData exoPrefix =
         TransientPropertyData.createPropertyData(nsNode, Constants.EXO_PREFIX, PropertyType.STRING, false,
            new TransientValueData(prefix));

      changesLog.add(ItemState.createAddedState(nsNode)).add(ItemState.createAddedState(primaryType)).add(
         ItemState.createAddedState(exoUri)).add(ItemState.createAddedState(exoPrefix));
      return changesLog;
   }

   /**
    * Add new namespace.
    * 
    * @param prefix NS prefix
    * @param uri NS URI
    * @throws RepositoryException Repository error
    */
   public void addNamespaces(Map<String, String> namespaceMap) throws RepositoryException
   {
      if (!started)
      {
         log.warn("Unable save namespaces in to the storage. Storage not initialized");
         return;
      }

      PlainChangesLog changesLog = new PlainChangesLogImpl();
      for (Map.Entry<String, String> entry : namespaceMap.entrySet())
      {
         String prefix = entry.getKey();
         String uri = entry.getValue();

         if (prefix != null)
         {
            if (log.isDebugEnabled())
               log.debug("Namespace " + uri + ":" + prefix);
            internallAdd(changesLog, prefix, uri);
         }
      }
      dataManager.save(new TransactionChangesLog(changesLog));

   }

   public boolean isStorageFilled()
   {
      try
      {
         List<NodeData> storageContent = dataManager.getChildNodesData(nsRoot);
         return storageContent.size() > 0;
      }
      catch (RepositoryException e)
      {
         log.error(e.getLocalizedMessage(), e);

      }
      return false;
   }

   public void removeNamespace(String prefix) throws RepositoryException
   {

      if (!started)
      {
         log.warn("Unable remove namspace " + prefix + " from the storage. Storage not initialized");
         return;
      }

      PlainChangesLogImpl plainChangesLogImpl = new PlainChangesLogImpl();
      ItemData prefData = dataManager.getItemData(nsRoot, new QPathEntry(new InternalQName("", prefix), 0));

      if (prefData != null && prefData.isNode())
      {
         List<PropertyData> childs = dataManager.getChildPropertiesData((NodeData)prefData);
         for (PropertyData propertyData : childs)
         {
            plainChangesLogImpl.add(ItemState.createDeletedState(copyPropertyData(propertyData), true));
         }
         prefData =
            new TransientNodeData(prefData.getQPath(), prefData.getIdentifier(), prefData.getPersistedVersion(),
               ((NodeData)prefData).getPrimaryTypeName(), ((NodeData)prefData).getMixinTypeNames(),
               ((NodeData)prefData).getOrderNumber(), ((NodeData)prefData).getParentIdentifier(), ((NodeData)prefData)
                  .getACL());
         plainChangesLogImpl.add(ItemState.createDeletedState(prefData, true));

      }

      dataManager.save(new TransactionChangesLog(plainChangesLogImpl));

   }

   /**
    * {@inheritDoc}
    */
   public void start()
   {
      if (!started)
      {
         try
         {
            NodeData jcrSystem = (NodeData)dataManager.getItemData(Constants.SYSTEM_UUID);
            if (jcrSystem != null)
            {
               NodeData exoNamespaces =
                  (NodeData)dataManager.getItemData(jcrSystem, new QPathEntry(Constants.EXO_NAMESPACES, 1));
               if (exoNamespaces == null)
               {
                  initStorage(jcrSystem, !repConfig.getAccessControl().equals(AccessControlPolicy.DISABLE));
                  this.nsRoot =
                     (NodeData)dataManager.getItemData(jcrSystem, new QPathEntry(Constants.EXO_NAMESPACES, 1));
               }
               else
               {
                  this.nsRoot = exoNamespaces;
               }
            }
            else
            {
               throw new RepositoryException("Nodetypes storage (/jcr:systemnode) is not initialized.");

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

   @Deprecated
   DataManager getDataManager()
   {
      return dataManager;
   }

   void loadNamespaces(Map<String, String> namespacesMap, Map<String, String> urisMap) throws RepositoryException
   {

      if (!isInialized())
      {
         NodeData jcrSystem = (NodeData)dataManager.getItemData(Constants.SYSTEM_UUID);
         if (jcrSystem != null)
            this.nsRoot = (NodeData)dataManager.getItemData(jcrSystem, new QPathEntry(Constants.EXO_NAMESPACES, 1));
         else
            throw new RepositoryException(
               "/jcr:system is not found. Possible the workspace is not initialized properly");
      }

      if (isInialized())
      {
         NodeDataReader nsReader = new NodeDataReader(nsRoot, dataManager);
         nsReader.setRememberSkiped(true);
         nsReader.forNodesByType(Constants.EXO_NAMESPACE);
         nsReader.read();

         List<NodeDataReader> nsData = nsReader.getNodesByType(Constants.EXO_NAMESPACE);
         for (NodeDataReader nsr : nsData)
         {
            nsr.forProperty(Constants.EXO_URI_NAME, PropertyType.STRING).forProperty(Constants.EXO_PREFIX,
               PropertyType.STRING);
            nsr.read();

            try
            {
               String exoUri = ValueDataConvertor.readString(nsr.getPropertyValue(Constants.EXO_URI_NAME));
               String exoPrefix = ValueDataConvertor.readString(nsr.getPropertyValue(Constants.EXO_PREFIX));
               namespacesMap.put(exoPrefix, exoUri);
               urisMap.put(exoUri, exoPrefix);

               if (log.isDebugEnabled())
                  log.debug("Namespace " + exoPrefix + " is loaded");
            }
            catch (IOException e)
            {
               throw new RepositoryException("Namespace load error " + e, e);
            }
         }

         for (NodeData skipedNs : nsReader.getSkiped())
         {
            log.warn("Namespace node " + skipedNs.getQPath().getName().getAsString() + " (primary type '"
               + skipedNs.getPrimaryTypeName().getAsString()
               + "') is not supported for loading. Nodes with 'exo:namespace' node type is supported only now.");
         }
      }
      else
         log.warn("Namespace storage (/jcr:system/exo:namespaces node) is not initialized. No namespaces loaded.");
   }

   /**
    * Copy <code>PropertyData prop<code> to new TransientItemData
    * 
    * @param prop
    * @return
    * @throws RepositoryException
    */
   private TransientItemData copyPropertyData(PropertyData prop) throws RepositoryException
   {

      if (prop == null)
         return null;

      // make a copy, value may be null for deleting items
      TransientPropertyData newData =
         new TransientPropertyData(prop.getQPath(), prop.getIdentifier(), prop.getPersistedVersion(), prop.getType(),
            prop.getParentIdentifier(), prop.isMultiValued(), prop.getValues());

      return newData;
   }

   /**
    * Creates namespaces storage and fill it with given namespaces.
    * 
    * @param nsSystem
    * @param addACL
    * @param namespaces
    * @throws RepositoryException
    */
   private void initStorage(NodeData nsSystem, boolean addACL) throws RepositoryException
   {
      PlainChangesLog changesLog = new PlainChangesLogImpl();
      TransientNodeData exoNamespaces;

      if (addACL)
      {
         AccessControlList acl = new AccessControlList();
         InternalQName[] mixins = new InternalQName[]{Constants.EXO_OWNEABLE, Constants.EXO_PRIVILEGEABLE};

         exoNamespaces =
            TransientNodeData.createNodeData(nsSystem, Constants.EXO_NAMESPACES, Constants.NT_UNSTRUCTURED, mixins);

         TransientPropertyData primaryType =
            TransientPropertyData.createPropertyData(exoNamespaces, Constants.JCR_PRIMARYTYPE, PropertyType.NAME,
               false, new TransientValueData(exoNamespaces.getPrimaryTypeName()));

         changesLog.add(ItemState.createAddedState(exoNamespaces)).add(ItemState.createAddedState(primaryType));

         // jcr:mixinTypes
         List<ValueData> mixValues = new ArrayList<ValueData>();
         for (InternalQName mixin : mixins)
         {
            mixValues.add(new TransientValueData(mixin));
         }
         TransientPropertyData exoMixinTypes =
            TransientPropertyData.createPropertyData(exoNamespaces, Constants.JCR_MIXINTYPES, PropertyType.NAME, true,
               mixValues);

         TransientPropertyData exoOwner =
            TransientPropertyData.createPropertyData(exoNamespaces, Constants.EXO_OWNER, PropertyType.STRING, false,
               new TransientValueData(acl.getOwner()));

         List<ValueData> permsValues = new ArrayList<ValueData>();
         for (int i = 0; i < acl.getPermissionEntries().size(); i++)
         {
            AccessControlEntry entry = acl.getPermissionEntries().get(i);
            permsValues.add(new TransientValueData(entry));
         }
         TransientPropertyData exoPerms =
            TransientPropertyData.createPropertyData(exoNamespaces, Constants.EXO_PERMISSIONS,
               ExtendedPropertyType.PERMISSION, true, permsValues);

         changesLog.add(ItemState.createAddedState(exoMixinTypes)).add(ItemState.createAddedState(exoOwner)).add(
            ItemState.createAddedState(exoPerms));
         changesLog.add(new ItemState(exoNamespaces, ItemState.MIXIN_CHANGED, false, null));
      }
      else
      {
         exoNamespaces =
            TransientNodeData.createNodeData(nsSystem, Constants.EXO_NAMESPACES, Constants.NT_UNSTRUCTURED);

         TransientPropertyData primaryType =
            TransientPropertyData.createPropertyData(exoNamespaces, Constants.JCR_PRIMARYTYPE, PropertyType.NAME,
               false, new TransientValueData(exoNamespaces.getPrimaryTypeName()));

         changesLog.add(ItemState.createAddedState(exoNamespaces)).add(ItemState.createAddedState(primaryType));
      }

      nsRoot = exoNamespaces;
      dataManager.save(new TransactionChangesLog(changesLog));
   }

   private boolean isInialized()
   {
      return nsRoot != null;
   }
}
