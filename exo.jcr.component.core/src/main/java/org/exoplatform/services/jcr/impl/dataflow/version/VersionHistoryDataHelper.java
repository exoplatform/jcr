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
package org.exoplatform.services.jcr.impl.dataflow.version;

import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.datamodel.Identifier;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.util.JCRDateFormat;
import org.exoplatform.services.jcr.util.IdGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.version.VersionException;

/**
 * Created by The eXo Platform SAS 19.12.2006 Helper class. Contains some
 * functions for a version history operations. Actually it's a wrapper for
 * NodeData with additional methods. For use instead a VersionHistoryImpl.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter
 *         Nedonosko</a>
 * @version $Id: VersionHistoryDataHelper.java 17564 2007-07-06 15:26:07Z
 *          peterit $
 */
public class VersionHistoryDataHelper extends TransientNodeData
{

   protected final ItemDataConsumer dataManager;

   protected final NodeTypeDataManager ntManager;

   private final String versionHistoryIdentifier;

   private final String baseVersionIdentifier;

   /**
    * Create helper using existed version history node data
    * 
    * @param source - existed version history node data
    * @param dataManager
    * @param ntManager
    */
   public VersionHistoryDataHelper(NodeData source, ItemDataConsumer dataManager, NodeTypeDataManager ntManager)
   {
      super(source.getQPath(), source.getIdentifier(), source.getPersistedVersion(), source.getPrimaryTypeName(),
         source.getMixinTypeNames(), source.getOrderNumber(), source.getParentIdentifier(), source.getACL());

      this.dataManager = dataManager;
      this.ntManager = ntManager;
      this.versionHistoryIdentifier = IdGenerator.generate();
      this.baseVersionIdentifier = IdGenerator.generate();
   }

   /**
    * Create helper as we create a new version history. All changes will be
    * placed into changes log. No persisted changes will be performed.
    * 
    * @param versionable - mix:versionable node data
    * @param changes - changes log
    * @param dataManager
    * @param ntManager
    * @throws RepositoryException
    */
   public VersionHistoryDataHelper(NodeData versionable, PlainChangesLog changes, ItemDataConsumer dataManager,
      NodeTypeDataManager ntManager) throws RepositoryException
   {

      this(versionable, changes, dataManager, ntManager, IdGenerator.generate(), IdGenerator.generate());
   }

   /**
    * Create helper as we create a new version history. All changes will be
    * placed into changes log. No persisted changes will be performed.
    * 
    * @param versionable - mix:versionable node data
    * @param changes - changes log
    * @param dataManager
    * @param ntManager
    * @throws RepositoryException
    */
   public VersionHistoryDataHelper(NodeData versionable, PlainChangesLog changes, ItemDataConsumer dataManager,
      NodeTypeDataManager ntManager, String versionHistoryIdentifier, String baseVersionIdentifier)
      throws RepositoryException
   {
      this.dataManager = dataManager;
      this.ntManager = ntManager;
      this.versionHistoryIdentifier = versionHistoryIdentifier;
      this.baseVersionIdentifier = baseVersionIdentifier;

      TransientNodeData vh = init(versionable, changes);

      // TransientItemData
      this.parentIdentifier = vh.getParentIdentifier().intern();
      this.identifier = vh.getIdentifier().intern();
      this.qpath = vh.getQPath();
      this.persistedVersion = vh.getPersistedVersion();

      // TransientNodeData
      this.primaryTypeName = vh.getPrimaryTypeName();
      this.mixinTypeNames = vh.getMixinTypeNames();
      this.orderNum = vh.getOrderNumber();
      this.acl = vh.getACL();
   }

   public List<NodeData> getAllVersionsData() throws RepositoryException
   {

      NodeData vData = (NodeData)dataManager.getItemData(getIdentifier());

      NodeData rootVersion =
         (NodeData)dataManager.getItemData(vData, new QPathEntry(Constants.JCR_ROOTVERSION, 0), ItemType.NODE);

      List<NodeData> vChilds = new ArrayList<NodeData>();

      // should be first in list
      vChilds.add(rootVersion);

      for (NodeData cnd : dataManager.getChildNodesData(vData))
      {
         if (!cnd.getQPath().getName().equals(Constants.JCR_ROOTVERSION)
            && ntManager.isNodeType(Constants.NT_VERSION, cnd.getPrimaryTypeName()))
            vChilds.add(cnd);
      }

      return vChilds;
   }

   public NodeData getLastVersionData() throws RepositoryException
   {
      List<NodeData> versionsData = getAllVersionsData();

      NodeData lastVersionData = null;
      Calendar lastCreated = null;
      for (NodeData vd : versionsData)
      {

         PropertyData createdData =
            (PropertyData)dataManager.getItemData(vd, new QPathEntry(Constants.JCR_CREATED, 0), ItemType.PROPERTY);

         if (createdData == null)
            throw new VersionException("jcr:created is not found, version: " + vd.getQPath().getAsString());

         Calendar created = null;
         try
         {
            created = new JCRDateFormat().deserialize(new String(createdData.getValues().get(0).getAsByteArray()));
         }
         catch (IOException e)
         {
            throw new RepositoryException(e);
         }

         if (lastVersionData == null || created.after(lastCreated))
         {
            lastCreated = created;
            lastVersionData = vd;
         }
      }
      return lastVersionData;
   }

   public NodeData getVersionData(InternalQName versionQName) throws VersionException, RepositoryException
   {
      return (NodeData)dataManager.getItemData(this, new QPathEntry(versionQName, 0), ItemType.NODE);
   }

   public NodeData getVersionLabelsData() throws VersionException, RepositoryException
   {
      return (NodeData)dataManager.getItemData(this, new QPathEntry(Constants.JCR_VERSIONLABELS, 0), ItemType.NODE);
   }

   public List<PropertyData> getVersionLabels() throws VersionException, RepositoryException
   {
      List<PropertyData> labelsList = dataManager.getChildPropertiesData(getVersionLabelsData());

      return labelsList;
   }

   public NodeData getVersionDataByLabel(InternalQName labelQName) throws VersionException, RepositoryException
   {

      List<PropertyData> labelsList = getVersionLabels();
      for (PropertyData prop : labelsList)
      {
         if (prop.getQPath().getName().equals(labelQName))
         {
            // label found
            try
            {
               String versionIdentifier = new String(prop.getValues().get(0).getAsByteArray());
               return (NodeData)dataManager.getItemData(versionIdentifier);
            }
            catch (IllegalStateException e)
            {
               throw new RepositoryException("Version label data error: " + e.getMessage(), e);
            }
            catch (IOException e)
            {
               throw new RepositoryException("Version label data reading error: " + e.getMessage(), e);
            }
         }
      }

      return null;
   }

   private TransientNodeData init(NodeData versionable, PlainChangesLog changes) throws RepositoryException
   {

      // ----- VERSION STORAGE nodes -----
      // ----- version history -----
      NodeData rootItem = (NodeData)dataManager.getItemData(Constants.SYSTEM_UUID);

      NodeData versionStorageData =
         (NodeData)dataManager.getItemData(rootItem, new QPathEntry(Constants.JCR_VERSIONSTORAGE, 1), ItemType.NODE); // Constants
      // Make versionStorageData transient
      if (!(versionStorageData instanceof TransientNodeData))
         versionStorageData =
            new TransientNodeData(versionStorageData.getQPath(), versionStorageData.getIdentifier(), versionStorageData
               .getPersistedVersion(), versionStorageData.getPrimaryTypeName(), versionStorageData.getMixinTypeNames(),
               versionStorageData.getOrderNumber(), versionStorageData.getParentIdentifier(), versionStorageData
                  .getACL());
      // .
      // JCR_VERSION_STORAGE_PATH

      InternalQName vhName = new InternalQName(null, versionHistoryIdentifier);

      TransientNodeData versionHistory =
         TransientNodeData.createNodeData(versionStorageData, vhName, Constants.NT_VERSIONHISTORY,
            versionHistoryIdentifier);

      // jcr:primaryType
      TransientPropertyData vhPrimaryType =
         TransientPropertyData.createPropertyData(versionHistory, Constants.JCR_PRIMARYTYPE, PropertyType.NAME, false,
            new TransientValueData(versionHistory.getPrimaryTypeName()));

      // jcr:uuid
      TransientPropertyData vhUuid =
         TransientPropertyData.createPropertyData(versionHistory, Constants.JCR_UUID, PropertyType.STRING, false,
            new TransientValueData(versionHistoryIdentifier));

      // jcr:versionableUuid
      TransientPropertyData vhVersionableUuid =
         TransientPropertyData
         // [PN] 10.04.07 VERSIONABLEUUID isn't referenceable!!!
            .createPropertyData(versionHistory, Constants.JCR_VERSIONABLEUUID, PropertyType.STRING, false,
               new TransientValueData(new Identifier(versionable.getIdentifier())));

      // ------ jcr:versionLabels ------
      NodeData vhVersionLabels =
         TransientNodeData.createNodeData(versionHistory, Constants.JCR_VERSIONLABELS, Constants.NT_VERSIONLABELS);

      // jcr:primaryType
      TransientPropertyData vlPrimaryType =
         TransientPropertyData.createPropertyData(vhVersionLabels, Constants.JCR_PRIMARYTYPE, PropertyType.NAME, false,
            new TransientValueData(vhVersionLabels.getPrimaryTypeName()));

      // ------ jcr:rootVersion ------
      NodeData rootVersionData =
         TransientNodeData.createNodeData(versionHistory, Constants.JCR_ROOTVERSION, Constants.NT_VERSION,
            baseVersionIdentifier);

      // jcr:primaryType
      TransientPropertyData rvPrimaryType =
         TransientPropertyData.createPropertyData(rootVersionData, Constants.JCR_PRIMARYTYPE, PropertyType.NAME, false,
            new TransientValueData(rootVersionData.getPrimaryTypeName()));

      // jcr:uuid
      TransientPropertyData rvUuid =
         TransientPropertyData.createPropertyData(rootVersionData, Constants.JCR_UUID, PropertyType.STRING, false,
            new TransientValueData(baseVersionIdentifier));

      // jcr:mixinTypes
      TransientPropertyData rvMixinTypes =
         TransientPropertyData.createPropertyData(rootVersionData, Constants.JCR_MIXINTYPES, PropertyType.NAME, true,
            new TransientValueData(Constants.MIX_REFERENCEABLE));

      // jcr:created
      // TODO Current time source was
      // rvCreated.setValue(new
      // TransientValueData(dataManager.getTransactManager().getStorageDataManager
      // ().getCurrentTime()));
      TransientPropertyData rvCreated =
         TransientPropertyData.createPropertyData(rootVersionData, Constants.JCR_CREATED, PropertyType.DATE, false,
            new TransientValueData(Calendar.getInstance()));

      // ----- VERSIONABLE properties -----
      // jcr:versionHistory
      TransientPropertyData vh =
         TransientPropertyData.createPropertyData(versionable, Constants.JCR_VERSIONHISTORY, PropertyType.REFERENCE,
            false, new TransientValueData(new Identifier(versionHistoryIdentifier)));

      // jcr:baseVersion
      TransientPropertyData bv =
         TransientPropertyData.createPropertyData(versionable, Constants.JCR_BASEVERSION, PropertyType.REFERENCE,
            false, new TransientValueData(new Identifier(baseVersionIdentifier)));

      // jcr:predecessors
      TransientPropertyData pd =
         TransientPropertyData.createPropertyData(versionable, Constants.JCR_PREDECESSORS, PropertyType.REFERENCE,
            true, new TransientValueData(new Identifier(baseVersionIdentifier)));

      // update all
      QPath vpath = versionable.getQPath();
      changes.add(new ItemState(versionHistory, ItemState.ADDED, true, vpath));
      changes.add(new ItemState(vhPrimaryType, ItemState.ADDED, true, vpath));
      changes.add(new ItemState(vhUuid, ItemState.ADDED, true, vpath));
      changes.add(new ItemState(vhVersionableUuid, ItemState.ADDED, true, vpath));

      changes.add(new ItemState(vhVersionLabels, ItemState.ADDED, true, vpath));
      changes.add(new ItemState(vlPrimaryType, ItemState.ADDED, true, vpath));

      changes.add(new ItemState(rootVersionData, ItemState.ADDED, true, vpath));
      changes.add(new ItemState(rvPrimaryType, ItemState.ADDED, true, vpath));
      changes.add(new ItemState(rvMixinTypes, ItemState.ADDED, true, vpath));
      changes.add(new ItemState(rvUuid, ItemState.ADDED, true, vpath));
      changes.add(new ItemState(rvCreated, ItemState.ADDED, true, vpath));

      changes.add(ItemState.createAddedState(vh));
      changes.add(ItemState.createAddedState(bv));
      changes.add(ItemState.createAddedState(pd));

      return versionHistory;
   }
}
