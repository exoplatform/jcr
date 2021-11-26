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

package org.exoplatform.services.jcr.impl.core.version;

import org.exoplatform.services.jcr.dataflow.DataManager;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.datamodel.*;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.dataflow.ValueDataUtil;
import org.exoplatform.services.jcr.impl.dataflow.session.SessionChangesLog;
import org.exoplatform.services.jcr.impl.dataflow.version.VersionHistoryDataHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: VersionImpl.java 12311 2008-03-24 12:30:51Z pnedonosko $
 */
public class VersionImpl extends VersionStorageDescendantNode implements Version
{

   public VersionImpl(NodeData data, SessionImpl session) throws PathNotFoundException, RepositoryException
   {
      super(data, session);

      if (!this.isNodeType(Constants.NT_VERSION))
      {
         throw new RepositoryException("Node " + getLocation().getAsString(true) + " is not nt:version type");
      }
   }

   /* needed for VersionHistoryImpl.removeVersion */
   @Override
   protected void invalidate()
   {
      super.invalidate();
   }

   /**
    * {@inheritDoc}
    */
   public Calendar getCreated() throws RepositoryException
   {
      checkValid();

      PropertyData pdata =
         (PropertyData)dataManager.getItemData(nodeData(), new QPathEntry(Constants.JCR_CREATED, 0), ItemType.PROPERTY);

      if (pdata == null)
      {
         throw new VersionException("jcr:created property is not found for version " + getPath());
      }

      Value created = session.getValueFactory().loadValue(pdata.getValues().get(0), pdata.getType());
      return created.getDate();
   }

   /**
    * {@inheritDoc}
    */
   public Version[] getSuccessors() throws RepositoryException
   {
      checkValid();

      PropertyData successorsData =
         (PropertyData)dataManager.getItemData(nodeData(), new QPathEntry(Constants.JCR_SUCCESSORS, 0),
            ItemType.PROPERTY);

      if (successorsData == null)
      {
         return new Version[0];
      }

      List<ValueData> successorsValues = successorsData.getValues();
      Version[] successors = new Version[successorsValues.size()];

      for (int i = 0; i < successorsValues.size(); i++)
      {
         String videntifier = ValueDataUtil.getString(successorsValues.get(i));
         VersionImpl version = (VersionImpl)dataManager.getItemByIdentifier(videntifier, true);
         if (version != null)
         {
            successors[i] = version;
         }
         else
         {
            throw new RepositoryException("Successor version is not found " + videntifier + ", this version "
               + getPath());
         }
      }

      return successors;
   }

   /**
    * {@inheritDoc}
    */
   public Version[] getPredecessors() throws RepositoryException
   {
      checkValid();

      PropertyData predecessorsData =
         (PropertyData)dataManager.getItemData(nodeData(), new QPathEntry(Constants.JCR_PREDECESSORS, 0),
            ItemType.PROPERTY);

      if (predecessorsData == null)
      {
         return new Version[0];
      }

      List<ValueData> predecessorsValues = predecessorsData.getValues();
      Version[] predecessors = new Version[predecessorsValues.size()];

      for (int i = 0; i < predecessorsValues.size(); i++)
      {
         String videntifier = ValueDataUtil.getString(predecessorsValues.get(i));
         VersionImpl version = (VersionImpl)dataManager.getItemByIdentifier(videntifier, false, false);
         if (version != null)
         {
            predecessors[i] = version;
         }
         else
         {
            throw new RepositoryException("Predecessor version is not found " + videntifier + ", this version "
               + getPath());
         }
      }
      return predecessors;
   }

   public void addSuccessor(String successorIdentifier, PlainChangesLog changesLog) throws RepositoryException
   {
      checkValid();

      ValueData successorRef = new TransientValueData(new Identifier(successorIdentifier));

      PropertyData successorsProp =
         (PropertyData)dataManager.getItemData(nodeData(), new QPathEntry(Constants.JCR_SUCCESSORS, 0),
            ItemType.PROPERTY);

      if (successorsProp == null)
      {
         // create a property now
         List<ValueData> successors = new ArrayList<ValueData>();
         successors.add(successorRef);
         successorsProp =
            TransientPropertyData.createPropertyData(nodeData(), Constants.JCR_SUCCESSORS, PropertyType.REFERENCE,
               true, successors);
         changesLog.add(ItemState.createAddedState(successorsProp));
      }
      else
      {
         // add successor 
         List<ValueData> newSuccessorsValue = new ArrayList<ValueData>();
         try
         {
            for (ValueData svd : successorsProp.getValues())
            {
               newSuccessorsValue.add(ValueDataUtil.createTransientCopy(svd));
            }
         }
         catch (IOException e)
         {
            throw new RepositoryException("jcr:successors read error " + e, e);
         }

         newSuccessorsValue.add(successorRef);

         TransientPropertyData newSuccessorsProp =
            new TransientPropertyData(successorsProp.getQPath(), successorsProp.getIdentifier(), successorsProp
               .getPersistedVersion(), successorsProp.getType(), successorsProp.getParentIdentifier(), successorsProp
               .isMultiValued(), newSuccessorsValue);

         changesLog.add(ItemState.createUpdatedState(newSuccessorsProp));
      }
   }

   public void addPredecessor(String predeccessorIdentifier, PlainChangesLog changesLog) throws RepositoryException
   {
      checkValid();

      ValueData predeccessorRef = new TransientValueData(new Identifier(predeccessorIdentifier));

      PropertyData predeccessorsProp =
         (PropertyData)dataManager.getItemData(nodeData(), new QPathEntry(Constants.JCR_PREDECESSORS, 0),
            ItemType.PROPERTY);

      if (predeccessorsProp == null)
      {
         List<ValueData> predeccessors = new ArrayList<ValueData>();
         predeccessors.add(predeccessorRef);
         predeccessorsProp =
            TransientPropertyData.createPropertyData(nodeData(), Constants.JCR_PREDECESSORS, PropertyType.REFERENCE,
               true, predeccessors);
         changesLog.add(ItemState.createAddedState(predeccessorsProp));
      }
      else
      {
         // add predeccessor
         List<ValueData> newPredeccessorValue = new ArrayList<ValueData>();
         try
         {
            for (ValueData svd : predeccessorsProp.getValues())
            {
               newPredeccessorValue.add(ValueDataUtil.createTransientCopy(svd));
            }
         }
         catch (IOException e)
         {
            throw new RepositoryException("jcr:predecessors read error " + e, e);
         }

         newPredeccessorValue.add(predeccessorRef);

         TransientPropertyData newPredeccessorsProp =
            new TransientPropertyData(predeccessorsProp.getQPath(), predeccessorsProp.getIdentifier(),
               predeccessorsProp.getPersistedVersion(), predeccessorsProp.getType(), predeccessorsProp
                  .getParentIdentifier(), predeccessorsProp.isMultiValued(), newPredeccessorValue);

         changesLog.add(ItemState.createUpdatedState(newPredeccessorsProp));
      }
   }

   void removeSuccessor(String successorIdentifier, PlainChangesLog changesLog) throws RepositoryException
   {
      PropertyData successorsProp =
         (PropertyData)dataManager.getItemData(nodeData(), new QPathEntry(Constants.JCR_SUCCESSORS, 0),
            ItemType.PROPERTY);
      if (successorsProp != null)
      {
         List<ValueData> newSuccessors = new ArrayList<ValueData>();

         try
         {
            for (ValueData sdata : successorsProp.getValues())
            {
               if (!successorIdentifier.equals(ValueDataUtil.getString(sdata)))
               {
                  newSuccessors.add(ValueDataUtil.createTransientCopy(sdata));
               }
            }
         }
         catch (IOException e)
         {
            throw new RepositoryException("A jcr:successors property read error " + e, e);
         }

         TransientPropertyData newSuccessorsProp =
            new TransientPropertyData(QPath.makeChildPath(nodeData().getQPath(), Constants.JCR_SUCCESSORS,
               successorsProp.getQPath().getIndex()), successorsProp.getIdentifier(), successorsProp
               .getPersistedVersion(), PropertyType.REFERENCE, nodeData().getIdentifier(), true, newSuccessors);

         if(! newSuccessors.isEmpty()) {
            changesLog.add(ItemState.createUpdatedState(newSuccessorsProp));
         }
         else
         {
            //No successor , remove reference property
            ItemData successorProp =
                    dataManager.getItemData(nodeData(), new QPathEntry(Constants.JCR_SUCCESSORS, 0), ItemType.PROPERTY);

            changesLog.add(ItemState.createDeletedState(successorProp));
         }
      }
      else
      {
         throw new RepositoryException("A jcr:successors property is not found, version " + getPath());
      }
   }

   void removeAddSuccessor(String removedSuccessorIdentifier, String addedSuccessorIdentifier,
      PlainChangesLog changesLog) throws RepositoryException
   {

      PropertyData successorsProp =
         (PropertyData)dataManager.getItemData(nodeData(), new QPathEntry(Constants.JCR_SUCCESSORS, 0),
            ItemType.PROPERTY);

      if (successorsProp != null)
      {
         List<ValueData> newSuccessors = new ArrayList<ValueData>();

         try
         {
            for (ValueData sdata : successorsProp.getValues())
            {
               if (!removedSuccessorIdentifier.equals(ValueDataUtil.getString(sdata)))
               {
                  newSuccessors.add(ValueDataUtil.createTransientCopy(sdata));
               }
            }
         }
         catch (IOException e)
         {
            throw new RepositoryException("A jcr:successors property read error " + e, e);
         }

         if(addedSuccessorIdentifier != null)
         {
            newSuccessors.add(new TransientValueData(new Identifier(addedSuccessorIdentifier)));
         }

         TransientPropertyData newSuccessorsProp =
            new TransientPropertyData(QPath.makeChildPath(nodeData().getQPath(), Constants.JCR_SUCCESSORS,
               successorsProp.getQPath().getIndex()), successorsProp.getIdentifier(), successorsProp
               .getPersistedVersion(), PropertyType.REFERENCE, nodeData().getIdentifier(), true, newSuccessors);

         changesLog.add(ItemState.createUpdatedState(newSuccessorsProp));
      }
      else
      {
         throw new RepositoryException("A jcr:successors property is not found, version " + getPath());
      }
   }

   void removePredecessor(String predecessorIdentifier, PlainChangesLog changesLog) throws RepositoryException
   {
      PropertyData predeccessorsProp =
         (PropertyData)dataManager.getItemData(nodeData(), new QPathEntry(Constants.JCR_PREDECESSORS, 0),
            ItemType.PROPERTY);

      if (predeccessorsProp != null)
      {
         List<ValueData> newPredeccessors = new ArrayList<ValueData>();

         try
         {
            for (ValueData sdata : predeccessorsProp.getValues())
            {
               if (!predecessorIdentifier.equals(ValueDataUtil.getString(sdata)))
               {
                  newPredeccessors.add(ValueDataUtil.createTransientCopy(sdata));
               }
            }
         }
         catch (IOException e)
         {
            throw new RepositoryException("A jcr:predecessors property read error " + e, e);
         }

         TransientPropertyData newPredecessorsProp =
            new TransientPropertyData(QPath.makeChildPath(nodeData().getQPath(), Constants.JCR_PREDECESSORS,
               predeccessorsProp.getQPath().getIndex()), predeccessorsProp.getIdentifier(), predeccessorsProp
               .getPersistedVersion(), PropertyType.REFERENCE, nodeData().getIdentifier(), true, newPredeccessors);

         changesLog.add(ItemState.createUpdatedState(newPredecessorsProp));
      }
      else
      {
         throw new RepositoryException("A jcr:predecessors property is not found, version " + getPath());
      }
   }

   void removeAddPredecessor(String removedPredecessorIdentifier, String addedPredecessorIdentifier,
      PlainChangesLog changesLog) throws RepositoryException
   {

      PropertyData predeccessorsProp =
         (PropertyData)dataManager.getItemData(nodeData(), new QPathEntry(Constants.JCR_PREDECESSORS, 0),
            ItemType.PROPERTY);

      if (predeccessorsProp != null)
      {
         List<ValueData> newPredeccessors = new ArrayList<ValueData>();

         try
         {
            for (ValueData sdata : predeccessorsProp.getValues())
            {
               if (!removedPredecessorIdentifier.equals(ValueDataUtil.getString(sdata)))
               {
                  newPredeccessors.add(ValueDataUtil.createTransientCopy(sdata));
               }
            }
         }
         catch (IOException e)
         {
            throw new RepositoryException("A jcr:predecessors property read error " + e, e);
         }

         newPredeccessors.add(new TransientValueData(new Identifier(addedPredecessorIdentifier)));

         TransientPropertyData newPredecessorsProp =
            new TransientPropertyData(QPath.makeChildPath(nodeData().getQPath(), Constants.JCR_PREDECESSORS,
               predeccessorsProp.getQPath().getIndex()), predeccessorsProp.getIdentifier(), predeccessorsProp
               .getPersistedVersion(), PropertyType.REFERENCE, nodeData().getIdentifier(), true, newPredeccessors);

         changesLog.add(ItemState.createUpdatedState(newPredecessorsProp));
      }
      else
      {
         throw new RepositoryException("A jcr:predecessors property is not found, version " + getPath());
      }
   }

   /**
    * {@inheritDoc}
    */
   public VersionHistoryImpl getContainingHistory() throws RepositoryException
   {
      checkValid();

      VersionHistoryImpl vhistory =
         (VersionHistoryImpl)dataManager.getItemByIdentifier(nodeData().getParentIdentifier(), true, false);

      if (vhistory == null)
      {
         throw new VersionException("Version history item is not found for version " + getPath());
      }

      return vhistory;
   }

   public SessionChangesLog restoreLog(NodeData destParent, InternalQName name, VersionHistoryDataHelper historyData,
      SessionImpl restoreSession, boolean removeExisting, SessionChangesLog delegatedLog) throws RepositoryException
   {
      checkValid();

      if (LOG.isDebugEnabled())
      {
         LOG.debug("Restore on parent " + destParent.getQPath().getAsString() + " as " + name.getAsString()
            + ", removeExisting=" + removeExisting);
      }

      DataManager dmanager = restoreSession.getTransientNodesManager().getTransactManager();

      NodeData frozenData =
         (NodeData)dmanager.getItemData(nodeData(), new QPathEntry(Constants.JCR_FROZENNODE, 1), ItemType.NODE);

      ItemDataRestoreVisitor restoreVisitor =
         new ItemDataRestoreVisitor(destParent, name, historyData, restoreSession, removeExisting, delegatedLog);

      frozenData.accept(restoreVisitor);

      return restoreVisitor.getRestoreChanges();
   }

   public void restore(SessionImpl restoreSession, NodeData destParent, InternalQName name, boolean removeExisting)
      throws RepositoryException
   {
      checkValid();

      DataManager dmanager = restoreSession.getTransientNodesManager().getTransactManager();

      NodeData vh = (NodeData)dmanager.getItemData(nodeData().getParentIdentifier()); // version
      // parent it's a VH
      VersionHistoryDataHelper historyHelper =
         new VersionHistoryDataHelper(vh, dmanager, session.getWorkspace().getNodeTypesHolder());

      SessionChangesLog changesLog = restoreLog(destParent, name, historyHelper, restoreSession, removeExisting, null);
      dmanager.save(changesLog);
   }

   public boolean isSuccessorOrSameOf(VersionImpl anotherVersion) throws RepositoryException
   {
      Version[] prds = getPredecessors();
      for (int i = 0; i < prds.length; i++)
      {
         if (prds[i].getUUID().equals(anotherVersion.getUUID())
            || ((VersionImpl)prds[i]).isSuccessorOrSameOf(anotherVersion))
         {
            return true;
         }
      }
      return false;
   }

}
