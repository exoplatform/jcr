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

import org.exoplatform.services.jcr.dataflow.DataManager;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.datamodel.Identifier;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
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

   @Override
   /* needed for VersionHistoryImpl.removeVersion */
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

      PropertyData pdata = (PropertyData)dataManager.getItemData(nodeData(), new QPathEntry(Constants.JCR_CREATED, 0));

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
         (PropertyData)dataManager.getItemData(nodeData(), new QPathEntry(Constants.JCR_SUCCESSORS, 0));

      if (successorsData == null)
      {
         return new Version[0];
      }

      List<ValueData> successorsValues = successorsData.getValues();
      Version[] successors = new Version[successorsValues.size()];

      try
      {
         for (int i = 0; i < successorsValues.size(); i++)
         {
            String videntifier = new String(successorsValues.get(i).getAsByteArray());
            VersionImpl version = (VersionImpl)dataManager.getItemByIdentifier(videntifier, true);
            if (version != null)
               successors[i] = version;
            else
               throw new RepositoryException("Successor version is not found " + videntifier + ", this version "
                  + getPath());
         }
      }
      catch (IOException e)
      {
         throw new RepositoryException("Successor value read error " + e, e);
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
         (PropertyData)dataManager.getItemData(nodeData(), new QPathEntry(Constants.JCR_PREDECESSORS, 0));

      if (predecessorsData == null)
         return new Version[0];

      List<ValueData> predecessorsValues = predecessorsData.getValues();
      Version[] predecessors = new Version[predecessorsValues.size()];

      try
      {
         for (int i = 0; i < predecessorsValues.size(); i++)
         {
            String videntifier = new String(predecessorsValues.get(i).getAsByteArray());
            VersionImpl version = (VersionImpl)dataManager.getItemByIdentifier(videntifier, false);
            if (version != null)
               predecessors[i] = version;
            else
               throw new RepositoryException("Predecessor version is not found " + videntifier + ", this version "
                  + getPath());
         }
      }
      catch (IOException e)
      {
         throw new RepositoryException("Predecessor value read error " + e, e);
      }

      return predecessors;
   }

   public void addSuccessor(String successorIdentifier, PlainChangesLog changesLog) throws RepositoryException
   {
      ValueData successorRef = new TransientValueData(new Identifier(successorIdentifier));

      PropertyData successorsProp =
         (PropertyData)dataManager.getItemData(nodeData(), new QPathEntry(Constants.JCR_SUCCESSORS, 0));

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
         for (ValueData svd : successorsProp.getValues())
         {
            newSuccessorsValue.add(svd);
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
      ValueData predeccessorRef = new TransientValueData(new Identifier(predeccessorIdentifier));

      PropertyData predeccessorsProp =
         (PropertyData)dataManager.getItemData(nodeData(), new QPathEntry(Constants.JCR_PREDECESSORS, 0));

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
         for (ValueData svd : predeccessorsProp.getValues())
         {
            newPredeccessorValue.add(svd);
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
         (PropertyData)dataManager.getItemData(nodeData(), new QPathEntry(Constants.JCR_SUCCESSORS, 0));
      if (successorsProp != null)
      {
         List<ValueData> newSuccessors = new ArrayList<ValueData>();

         try
         {
            for (ValueData sdata : successorsProp.getValues())
            {
               if (!successorIdentifier.equals(new String(sdata.getAsByteArray())))
               {
                  newSuccessors.add(sdata);
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

         changesLog.add(ItemState.createUpdatedState(newSuccessorsProp));
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
         (PropertyData)dataManager.getItemData(nodeData(), new QPathEntry(Constants.JCR_SUCCESSORS, 0));

      if (successorsProp != null)
      {
         List<ValueData> newSuccessors = new ArrayList<ValueData>();

         try
         {
            for (ValueData sdata : successorsProp.getValues())
            {
               if (!removedSuccessorIdentifier.equals(new String(sdata.getAsByteArray())))
               {
                  newSuccessors.add(sdata);
               }
            }
         }
         catch (IOException e)
         {
            throw new RepositoryException("A jcr:successors property read error " + e, e);
         }

         newSuccessors.add(new TransientValueData(new Identifier(addedSuccessorIdentifier)));

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
         (PropertyData)dataManager.getItemData(nodeData(), new QPathEntry(Constants.JCR_PREDECESSORS, 0));

      if (predeccessorsProp != null)
      {
         List<ValueData> newPredeccessors = new ArrayList<ValueData>();

         try
         {
            for (ValueData sdata : predeccessorsProp.getValues())
            {
               if (!predecessorIdentifier.equals(new String(sdata.getAsByteArray())))
               {
                  newPredeccessors.add(sdata);
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
         (PropertyData)dataManager.getItemData(nodeData(), new QPathEntry(Constants.JCR_PREDECESSORS, 0));

      if (predeccessorsProp != null)
      {
         List<ValueData> newPredeccessors = new ArrayList<ValueData>();

         try
         {
            for (ValueData sdata : predeccessorsProp.getValues())
            {
               if (!removedPredecessorIdentifier.equals(new String(sdata.getAsByteArray())))
               {
                  newPredeccessors.add(sdata);
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
         (VersionHistoryImpl)dataManager.getItemByIdentifier(nodeData().getParentIdentifier(), true);

      if (vhistory == null)
      {
         throw new VersionException("Version history item is not found for version " + getPath());
      }

      return vhistory;
   }

   public SessionChangesLog restoreLog(NodeData destParent, InternalQName name, VersionHistoryDataHelper historyData,
      SessionImpl restoreSession, boolean removeExisting, SessionChangesLog delegatedLog) throws RepositoryException
   {

      if (LOG.isDebugEnabled())
         LOG.debug("Restore on parent " + destParent.getQPath().getAsString() + " as " + name.getAsString()
            + ", removeExisting=" + removeExisting);

      DataManager dmanager = restoreSession.getTransientNodesManager().getTransactManager();

      NodeData frozenData = (NodeData)dmanager.getItemData(nodeData(), new QPathEntry(Constants.JCR_FROZENNODE, 1));

      ItemDataRestoreVisitor restoreVisitor =
         new ItemDataRestoreVisitor(destParent, name, historyData, restoreSession, removeExisting, delegatedLog);

      frozenData.accept(restoreVisitor);

      return restoreVisitor.getRestoreChanges();
   }

   public void restore(SessionImpl restoreSession, NodeData destParent, InternalQName name, boolean removeExisting)
      throws RepositoryException
   {

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
            return true;
      }
      return false;
   }

}
