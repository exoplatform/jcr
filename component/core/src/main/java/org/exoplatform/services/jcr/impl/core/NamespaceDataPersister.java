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
import org.exoplatform.services.jcr.impl.dataflow.AbstractValueData;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.InvalidItemStateException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: NamespaceDataPersister.java 13962 2008-05-07 16:00:48Z pnedonosko $
 */

public class NamespaceDataPersister
{

   public static Log log = ExoLogger.getLogger("jcr.NamespaceDataPersister");

   private DataManager dataManager;

   private PlainChangesLog changesLog;

   private NodeData nsRoot;

   public NamespaceDataPersister(DataManager dataManager)
   {
      this.dataManager = dataManager;
      this.changesLog = new PlainChangesLogImpl();
      try
      {
         NodeData jcrSystem = (NodeData)dataManager.getItemData(Constants.SYSTEM_UUID);
         if (jcrSystem != null)
            this.nsRoot = (NodeData)dataManager.getItemData(jcrSystem, new QPathEntry(Constants.EXO_NAMESPACES, 1));
      }
      catch (RepositoryException e)
      {
         log.warn("Namespace storage (/jcr:system/exo:namespaces node) is not initialized");
      }
   }

   DataManager getDataManager()
   {
      return dataManager;
   }

   /**
    * Creates namespaces storage and fill it with given namespaces.
    * 
    * @param nsSystem
    * @param addACL
    * @param namespaces
    * @throws RepositoryException
    */
   public void initStorage(NodeData nsSystem, boolean addACL, Map<String, String> namespaces)
      throws RepositoryException
   {

      TransientNodeData exoNamespaces =
         TransientNodeData.createNodeData(nsSystem, Constants.EXO_NAMESPACES, Constants.NT_UNSTRUCTURED);

      TransientPropertyData primaryType =
         TransientPropertyData.createPropertyData(exoNamespaces, Constants.JCR_PRIMARYTYPE, PropertyType.NAME, false);
      primaryType.setValue(new TransientValueData(exoNamespaces.getPrimaryTypeName()));

      changesLog.add(ItemState.createAddedState(exoNamespaces)).add(ItemState.createAddedState(primaryType));

      if (addACL)
      {
         AccessControlList acl = new AccessControlList();

         InternalQName[] mixins = new InternalQName[]{Constants.EXO_OWNEABLE, Constants.EXO_PRIVILEGEABLE};
         exoNamespaces.setMixinTypeNames(mixins);

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

      nsRoot = exoNamespaces;

      Iterator<String> i = namespaces.keySet().iterator();
      while (i.hasNext())
      {
         String nsKey = i.next();
         if (nsKey != null)
         {
            if (log.isDebugEnabled())
               log.debug("Namespace " + nsKey + " " + namespaces.get(nsKey));
            addNamespace(nsKey, namespaces.get(nsKey));
            if (log.isDebugEnabled())
               log.debug("Namespace " + nsKey + " is initialized.");
         }
         else
         {
            log.warn("Namespace is " + nsKey + " " + namespaces.get(nsKey));
         }
      }
      saveChanges();
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

      if (!isInialized())
      {
         log.warn("Namespace storage (/jcr:system/exo:namespaces node) is not initialized");
         return;
      }

      TransientNodeData nsNode =
         TransientNodeData.createNodeData(nsRoot, new InternalQName("", prefix), Constants.EXO_NAMESPACE);

      TransientPropertyData primaryType =
         TransientPropertyData.createPropertyData(nsNode, Constants.JCR_PRIMARYTYPE, PropertyType.NAME, false);
      primaryType.setValue(new TransientValueData(nsNode.getPrimaryTypeName()));

      TransientPropertyData exoUri =
         TransientPropertyData.createPropertyData(nsNode, Constants.EXO_URI_NAME, PropertyType.STRING, false);
      exoUri.setValue(new TransientValueData(uri));

      TransientPropertyData exoPrefix =
         TransientPropertyData.createPropertyData(nsNode, Constants.EXO_PREFIX, PropertyType.STRING, false);
      exoPrefix.setValue(new TransientValueData(prefix));

      changesLog.add(ItemState.createAddedState(nsNode)).add(ItemState.createAddedState(primaryType)).add(
         ItemState.createAddedState(exoUri)).add(ItemState.createAddedState(exoPrefix));

   }

   public void removeNamespace(String prefix) throws RepositoryException
   {

      if (!isInialized())
      {
         log.warn("Namespace storage (/jcr:system/exo:namespaces node) is not initialized");
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

   void saveChanges() throws RepositoryException, InvalidItemStateException
   {
      dataManager.save(new TransactionChangesLog(changesLog));
      changesLog = new PlainChangesLogImpl();
   }

   private boolean isInialized()
   {
      return nsRoot != null;
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

      // make a copy
      TransientPropertyData newData =
         new TransientPropertyData(prop.getQPath(), prop.getIdentifier(), prop.getPersistedVersion(), prop.getType(),
            prop.getParentIdentifier(), prop.isMultiValued());

      List<ValueData> values = null;
      // null is possible for deleting items
      if (prop.getValues() != null)
      {
         values = new ArrayList<ValueData>();
         for (ValueData val : prop.getValues())
         {
            values.add(((AbstractValueData)val).createTransientCopy());
         }
      }
      newData.setValues(values);
      return newData;
   }
}
