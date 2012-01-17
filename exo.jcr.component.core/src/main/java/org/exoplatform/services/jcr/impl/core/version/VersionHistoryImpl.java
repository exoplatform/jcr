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
import org.exoplatform.services.jcr.core.ExtendedPropertyType;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.JCRName;
import org.exoplatform.services.jcr.impl.core.JCRPath;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.dataflow.ItemDataRemoveVisitor;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.dataflow.ValueDataConvertor;
import org.exoplatform.services.jcr.impl.dataflow.session.SessionChangesLog;
import org.exoplatform.services.jcr.impl.dataflow.version.VersionHistoryDataHelper;
import org.exoplatform.services.jcr.impl.util.EntityCollection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: VersionHistoryImpl.java 14464 2008-05-19 11:05:20Z pnedonosko $
 */

public class VersionHistoryImpl extends VersionStorageDescendantNode implements VersionHistory
{

   // new impl
   public VersionHistoryImpl(NodeData data, SessionImpl session) throws PathNotFoundException, RepositoryException
   {

      super(data, session);

      if (!this.isNodeType(Constants.NT_VERSIONHISTORY))
      {
         throw new RepositoryException("Node " + getLocation().getAsString(true) + " is not nt:versionHistory type");
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void loadData(ItemData vhData, NodeData parent) throws RepositoryException, InvalidItemStateException,
      ConstraintViolationException
   {
      super.loadData(new VersionHistoryDataHelper((NodeData)vhData, session.getTransientNodesManager()
         .getTransactManager(), session.getWorkspace().getNodeTypesHolder()), parent);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public VersionHistoryDataHelper getData()
   {
      return (VersionHistoryDataHelper)super.getData();
   }

   /**
    * {@inheritDoc}
    */
   public String getVersionableUUID() throws RepositoryException
   {
      checkValid();

      PropertyData versionableUuid =
         (PropertyData)dataManager.getItemData(nodeData(), new QPathEntry(Constants.JCR_VERSIONABLEUUID, 0),
            ItemType.PROPERTY);

      if (versionableUuid != null)
      {
         try
         {
            return new String(versionableUuid.getValues().get(0).getAsByteArray());
         }
         catch (IllegalStateException e)
         {
            LOG.error("jcr:versionableUuid, error of read " + e + ". Version history " + getPath(), e);
         }
         catch (IOException e)
         {
            LOG.error("jcr:versionableUuid, error of read " + e + ". Version history " + getPath(), e);
         }
      }

      throw new ItemNotFoundException("A property jcr:versionableUuid is not found. Version history " + getPath());
   }

   /**
    * {@inheritDoc}
    */
   public Version getRootVersion() throws RepositoryException
   {
      checkValid();

      VersionImpl version =
         (VersionImpl)dataManager
            .getItem(nodeData(), new QPathEntry(Constants.JCR_ROOTVERSION, 0), true, ItemType.NODE);
      if (version == null)
      {
         throw new VersionException("There are no root version in the version history " + getPath());
      }

      return version;
   }

   /**
    * {@inheritDoc}
    */
   public VersionIterator getAllVersions() throws RepositoryException
   {
      checkValid();

      List<NodeData> versionsDataList = getData().getAllVersionsData();

      EntityCollection versions = new EntityCollection();

      for (NodeData vd : versionsDataList)
      {
         versions.add(new VersionImpl(vd, session));
      }

      return versions;

   }

   /**
    * {@inheritDoc}
    */
   public Version getVersion(String versionName) throws VersionException, RepositoryException
   {
      checkValid();

      return version(versionName, true);
   }

   /**
    * For internal use. Doesn't check InvalidItemStateException. May return
    * unpooled Version object.
    */
   public Version version(String versionName, boolean pool) throws VersionException, RepositoryException
   {
      JCRName jcrVersionName = locationFactory.parseJCRName(versionName);
      VersionImpl version =
         (VersionImpl)dataManager.getItem(nodeData(), new QPathEntry(jcrVersionName.getInternalName(), 1), pool,
            ItemType.NODE, false);
      if (version == null)
      {
         throw new VersionException("There are no version with name '" + versionName + "' in the version history "
            + getPath());
      }

      return version;
   }

   /**
    * {@inheritDoc}
    */
   public Version getVersionByLabel(String label) throws RepositoryException
   {
      checkValid();

      NodeData versionData = getVersionDataByLabel(label);
      if (versionData == null)
      {
         throw new RepositoryException("There are no label '" + label + "' in the version history " + getPath());
      }

      VersionImpl version = (VersionImpl)dataManager.getItemByIdentifier(versionData.getIdentifier(), true, false);

      if (version == null)
      {
         throw new VersionException("There are no version with label '" + label + "' in the version history "
            + getPath());
      }

      return version;

   }

   /**
    * {@inheritDoc}
    */
   public boolean hasVersionLabel(String label) throws RepositoryException
   {
      checkValid();

      if (this.getVersionDataByLabel(label) == null)
      {
         return false;
      }

      return true;
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasVersionLabel(Version version, String label) throws VersionException, RepositoryException
   {
      checkValid();

      NodeData versionData = getVersionDataByLabel(label);
      if (versionData != null && version.getUUID().equals(versionData.getIdentifier()))
      {
         return true;
      }

      return false;
   }

   /**
    * {@inheritDoc}
    */
   public String[] getVersionLabels() throws RepositoryException
   {
      checkValid();

      List<PropertyData> versionLabels = getData().getVersionLabels();
      String[] labelsStrs = new String[versionLabels.size()];
      for (int i = 0; i < versionLabels.size(); i++)
      {
         labelsStrs[i] = locationFactory.createJCRName(versionLabels.get(i).getQPath().getName()).getAsString();
      }

      return labelsStrs;
   }

   protected List<String> getVersionLabelsList(Version version) throws VersionException, RepositoryException
   {
      if (!isVersionBelongToThis(version))
      {
         throw new VersionException("There are no version '" + version.getPath() + "' in the version history "
            + getPath());
      }

      List<PropertyData> labelsList = getData().getVersionLabels();
      List<String> vlabels = new ArrayList<String>();

      try
      {
         for (PropertyData prop : labelsList)
         {
            String versionUuid = ValueDataConvertor.readString(prop.getValues().get(0));
            if (versionUuid.equals(((VersionImpl)version).getInternalIdentifier()))
            {
               vlabels.add(locationFactory.createJCRName(prop.getQPath().getName()).getAsString());
            }
         }
      }
      catch (IOException e)
      {
         throw new RepositoryException("Get version " + version.getPath() + " labels error " + e, e);
      }

      return vlabels;
   }

   /**
    * {@inheritDoc}
    */
   public String[] getVersionLabels(Version version) throws VersionException, RepositoryException
   {
      checkValid();

      List<String> vlabels = getVersionLabelsList(version);

      String[] res = new String[vlabels.size()];
      for (int i = 0; i < vlabels.size(); i++)
      {
         res[i] = vlabels.get(i);
      }
      return res;
   }

   /**
    * {@inheritDoc}
    */
   public void removeVersion(String versionName) throws ReferentialIntegrityException, AccessDeniedException,
      UnsupportedRepositoryOperationException, VersionException, RepositoryException
   {
      checkValid();
      // get version (pool it to be able to invalidate the version on final)
      VersionImpl version = (VersionImpl)version(versionName, true);

      // check references.
      // Note: References from /jcr:system/jcr:versionStorage never included to
      // getReferences!
      List<PropertyData> refs = dataManager.getReferencesData(version.getInternalIdentifier(), true);
      if (refs.size() > 0)
      {
         throw new ReferentialIntegrityException("There are Reference property pointed to this Version "
            + refs.get(0).getQPath().getAsString());
      }

      PlainChangesLog changes = new PlainChangesLogImpl(session);

      // remove labels first
      try
      {
         for (PropertyData vlabel : getData().getVersionLabels())
         {
            String versionUuid = new String(vlabel.getValues().get(0).getAsByteArray());
            if (versionUuid.equals(version.getInternalIdentifier()))
            {
               changes.add(ItemState.createDeletedState(vlabel));
            }
         }
      }
      catch (IOException e)
      {
         throw new RepositoryException("Get version " + version.getPath() + " labels error " + e, e);
      }

      // remove this version from successor anf predecessor list
      // and point successor to predecessor directly

      PropertyData successorsData =
         (PropertyData)dataManager.getItemData((NodeData)version.getData(),
            new QPathEntry(Constants.JCR_SUCCESSORS, 0), ItemType.PROPERTY);

      // jcr:predecessors
      PropertyData predecessorsData =
         (PropertyData)dataManager.getItemData((NodeData)version.getData(), new QPathEntry(Constants.JCR_PREDECESSORS,
            0), ItemType.PROPERTY);

      try
      {
         for (ValueData pvalue : predecessorsData.getValues())
         {
            String pidentifier = new String(pvalue.getAsByteArray());
            VersionImpl predecessor = (VersionImpl)dataManager.getItemByIdentifier(pidentifier, false, false);
            // actually predecessor is V2's successor
            if (predecessor != null)
            {// V2's successor
               if (successorsData != null)
               {// to redirect V2's successor
                  // case of VH graph merge
                  for (ValueData svalue : successorsData.getValues())
                  {
                     predecessor.removeAddSuccessor(version.getInternalIdentifier(),
                        new String(svalue.getAsByteArray()), changes);
                  }
               }
               else
               {
                  // case of VH last version remove
                  predecessor.removeSuccessor(version.getInternalIdentifier(), changes);
               }
            }
            else
            {
               throw new RepositoryException("A predecessor (" + pidentifier + ") of the version " + version.getPath()
                  + " is not found.");
            }
         }
      }
      catch (IOException e)
      {
         throw new RepositoryException("Get predecessor " + version.getPath() + " error " + e, e);
      }

      try
      {
         if (successorsData != null)
         {
            for (ValueData svalue : successorsData.getValues())
            {
               String sidentifier = new String(svalue.getAsByteArray());
               VersionImpl successor = (VersionImpl)dataManager.getItemByIdentifier(sidentifier, false, false);
               if (successor != null)
               {
                  // case of VH graph merge
                  for (ValueData pvalue : predecessorsData.getValues())
                  {
                     successor.removeAddPredecessor(version.getInternalIdentifier(),
                        new String(pvalue.getAsByteArray()), changes);
                  }
               }
               else
               {
                  throw new RepositoryException("A successor (" + sidentifier + ") of the version " + version.getPath()
                     + " is not found.");
               }
            }
         }
      }
      catch (IOException e)
      {
         throw new RepositoryException("Get successor " + version.getPath() + " error " + e, e);
      }

      ItemDataRemoveVisitor removeVisitor = new ItemDataRemoveVisitor(dataManager.getTransactManager(), null);
      version.getData().accept(removeVisitor);
      changes.addAll(removeVisitor.getRemovedStates());

      dataManager.getTransactManager().save(changes);

      version.invalidate();
   }

   protected NodeData getVersionData(String versionName) throws VersionException, RepositoryException
   {
      JCRPath jcrPath = locationFactory.createJCRPath(getLocation(), versionName);

      NodeData version = getData().getVersionData(jcrPath.getName().getInternalName());

      if (version == null)
      {
         throw new VersionException("Version is not found " + jcrPath.getAsString(false));
      }

      return version;
   }

   protected NodeData getVersionLabelsData() throws VersionException, RepositoryException
   {
      NodeData labels = getData().getVersionLabelsData();
      if (labels == null)
      {
         throw new VersionException("Mandatory node jcr:versionLabels is not found for version history " + getPath());
      }

      return labels;
   }

   protected NodeData getVersionDataByLabel(String labelName) throws VersionException, RepositoryException
   {
      JCRName jcrLabelName = locationFactory.parseJCRName(labelName);
      InternalQName labelQName = jcrLabelName.getInternalName();

      return getData().getVersionDataByLabel(labelQName);
   }

   protected NodeData getVersionDataByIdentifier(String versionIdentifier) throws VersionException, RepositoryException
   {
      NodeData version = (NodeData)dataManager.getItemData(versionIdentifier);
      if (version == null)
      {
         throw new VersionException("Version is not found, uuid: " + versionIdentifier);
      }

      return version;
   }

   /**
    * {@inheritDoc}
    */
   public void addVersionLabel(String versionName, String label, boolean moveLabel) throws VersionException,
      RepositoryException
   {
      checkValid();

      JCRName jcrLabelName = locationFactory.parseJCRName(label);
      InternalQName labelQName = jcrLabelName.getInternalName();

      NodeData labels = getVersionLabelsData();

      List<PropertyData> labelsList = dataManager.getChildPropertiesData(labels);
      for (PropertyData prop : labelsList)
      {
         if (prop.getQPath().getName().equals(labelQName))
         {
            // label is found
            if (moveLabel)
            {
               removeVersionLabel(label);
               break;
            }
            throw new VersionException("Label " + label + " is already exists and moveLabel=false");
         }
      }

      NodeData versionData = getVersionData(versionName);

      SessionChangesLog changesLog = new SessionChangesLog(session);

      PropertyData labelData =
         TransientPropertyData.createPropertyData(labels, labelQName, PropertyType.REFERENCE, false,
            new TransientValueData(versionData.getIdentifier()));
      changesLog.add(ItemState.createAddedState(labelData));

      dataManager.getTransactManager().save(changesLog);
   }

   /**
    * {@inheritDoc}
    */
   public void removeVersionLabel(String labelName) throws VersionException, RepositoryException
   {
      checkValid();

      JCRName jcrLabelName = locationFactory.parseJCRName(labelName);
      InternalQName labelQName = jcrLabelName.getInternalName();

      PropertyData vldata =
         (PropertyData)dataManager.getItemData(getData().getVersionLabelsData(), new QPathEntry(labelQName, 0),
            ItemType.PROPERTY);

      if (vldata != null)
      {
         PlainChangesLog changes = new PlainChangesLogImpl(session);
         changes.add(ItemState.createDeletedState(vldata));
         dataManager.getTransactManager().save(changes);
      }
      else
      {
         throw new VersionException("Label not found " + labelName);
      }

   }

   // //////////////// impl

   public void addVersion(NodeData versionableNodeData, String uuid, SessionChangesLog changesLog)
      throws RepositoryException
   {
      checkValid();

      NodeTypeDataManager ntManager = session.getWorkspace().getNodeTypesHolder();

      boolean isPrivilegeable =
         ntManager.isNodeType(Constants.EXO_PRIVILEGEABLE, versionableNodeData.getPrimaryTypeName(),
            versionableNodeData.getMixinTypeNames());

      boolean isOwneable =
         ntManager.isNodeType(Constants.EXO_OWNEABLE, versionableNodeData.getPrimaryTypeName(), versionableNodeData
            .getMixinTypeNames());

      List<InternalQName> mixinsList = new ArrayList<InternalQName>();
      mixinsList.add(Constants.MIX_REFERENCEABLE);

      Set<AccessControlEntry> accessList = new HashSet<AccessControlEntry>();
      accessList.addAll(nodeData().getACL().getPermissionEntries());

      String owner = nodeData().getACL().getOwner();

      if (isPrivilegeable)
      {
         accessList.addAll(versionableNodeData.getACL().getPermissionEntries());
         mixinsList.add(Constants.EXO_PRIVILEGEABLE);
      }

      if (isOwneable)
      {
         owner = versionableNodeData.getACL().getOwner();
         mixinsList.add(Constants.EXO_OWNEABLE);
      }

      AccessControlList acl = new AccessControlList(owner, new ArrayList<AccessControlEntry>(accessList));
      InternalQName[] mixins = mixinsList.toArray(new InternalQName[mixinsList.size()]);

      // nt:version
      NodeData versionData =
         TransientNodeData.createNodeData(nodeData(), new InternalQName(null, nextVersionName()), Constants.NT_VERSION,
            mixins, uuid, acl);
      changesLog.add(ItemState.createAddedState(versionData));

      // jcr:primaryType
      TransientPropertyData propData =
         TransientPropertyData.createPropertyData(versionData, Constants.JCR_PRIMARYTYPE, PropertyType.NAME, false,
            new TransientValueData(Constants.NT_VERSION));
      changesLog.add(ItemState.createAddedState(propData));

      // jcr:mixinTypes
      List<ValueData> mixValues = new ArrayList<ValueData>();
      for (InternalQName mixin : mixins)
      {
         mixValues.add(new TransientValueData(mixin));
      }
      TransientPropertyData exoMixinTypes =
         TransientPropertyData.createPropertyData(versionData, Constants.JCR_MIXINTYPES, PropertyType.NAME, true,
            mixValues);
      changesLog.add(ItemState.createAddedState(exoMixinTypes));

      // exo:owner
      if (isOwneable)
      {
         TransientPropertyData exoOwner =
            TransientPropertyData.createPropertyData(versionData, Constants.EXO_OWNER, PropertyType.STRING, false,
               new TransientValueData(acl.getOwner()));
         changesLog.add(ItemState.createAddedState(exoOwner));
      }

      // exo:permissions
      if (isPrivilegeable)
      {
         List<ValueData> permsValues = new ArrayList<ValueData>();
         for (AccessControlEntry entry : acl.getPermissionEntries())
         {
            permsValues.add(new TransientValueData(entry));
         }

         TransientPropertyData exoPerms =
            TransientPropertyData.createPropertyData(versionData, Constants.EXO_PERMISSIONS,
               ExtendedPropertyType.PERMISSION, true, permsValues);
         changesLog.add(ItemState.createAddedState(exoPerms));
      }

      // jcr:uuid
      propData =
         TransientPropertyData.createPropertyData(versionData, Constants.JCR_UUID, PropertyType.STRING, false,
            new TransientValueData(uuid));
      changesLog.add(ItemState.createAddedState(propData));

      // jcr:created
      propData =
         TransientPropertyData.createPropertyData(versionData, Constants.JCR_CREATED, PropertyType.DATE, false,
            new TransientValueData(session.getTransientNodesManager().getWorkspaceDataManager().getCurrentTime()));
      changesLog.add(ItemState.createAddedState(propData));

      // A reference to V is added to the jcr:successors property of
      // each of the versions identified in Vs jcr:predecessors property.
      List<ValueData> predecessors =
         ((PropertyData)dataManager.getItemData(versionableNodeData, new QPathEntry(Constants.JCR_PREDECESSORS, 0),
            ItemType.PROPERTY)).getValues();
      List<ValueData> predecessorsNew = new ArrayList<ValueData>();
      for (ValueData predecessorValue : predecessors)
      {
         byte[] pib;
         try
         {
            pib = predecessorValue.getAsByteArray();
         }
         catch (IOException e)
         {
            throw new RepositoryException(e);
         }
         VersionImpl predecessor = (VersionImpl)dataManager.getItemByIdentifier(new String(pib), false, false);
         predecessor.addSuccessor(versionData.getIdentifier(), changesLog);

         predecessorsNew.add(new TransientValueData(pib));
      }

      // jcr:predecessors
      propData =
         TransientPropertyData.createPropertyData(versionData, Constants.JCR_PREDECESSORS, PropertyType.REFERENCE,
            true, predecessorsNew);
      changesLog.add(ItemState.createAddedState(propData));

      // jcr:frozenNode
      NodeData frozenData =
         TransientNodeData.createNodeData(versionData, Constants.JCR_FROZENNODE, Constants.NT_FROZENNODE);
      changesLog.add(ItemState.createAddedState(frozenData));

      propData =
         TransientPropertyData.createPropertyData(frozenData, Constants.JCR_PRIMARYTYPE, PropertyType.NAME, false,
            new TransientValueData(Constants.NT_FROZENNODE));
      changesLog.add(ItemState.createAddedState(propData));

      propData =
         TransientPropertyData.createPropertyData(frozenData, Constants.JCR_MIXINTYPES, PropertyType.NAME, true,
            new TransientValueData(Constants.MIX_REFERENCEABLE));
      changesLog.add(ItemState.createAddedState(propData));

      propData =
         TransientPropertyData.createPropertyData(frozenData, Constants.JCR_UUID, PropertyType.STRING, false,
            new TransientValueData(frozenData.getIdentifier()));
      changesLog.add(ItemState.createAddedState(propData));

      FrozenNodeInitializer visitor =
         new FrozenNodeInitializer(frozenData, session.getTransientNodesManager(), session.getWorkspace()
            .getNodeTypesHolder(), changesLog, session.getValueFactory());

      if (LOG.isDebugEnabled())
      {
         LOG.debug("Before frozen visitor: " + changesLog.dump());
      }

      versionableNodeData.accept(visitor);

   }

   public boolean isVersionBelongToThis(Version version) throws RepositoryException
   {
      return ((VersionImpl)version).getLocation().isDescendantOf(getLocation(), false);
   }

   private String nextVersionName() throws RepositoryException
   {
      // make version name - 1-based (because of rootVersion) integer
      int vn = 0;
      for (VersionIterator allVersions = getAllVersions(); allVersions.hasNext();)
      {
         Version v = allVersions.nextVersion();
         try
         {
            int vi = Integer.parseInt(v.getName());
            if (vi > vn)
            {
               vn = vi;
            }
         }
         catch (NumberFormatException e)
         {
         }
      }
      return vn > 0 ? String.valueOf(vn + 1) : "1";
   }
}
