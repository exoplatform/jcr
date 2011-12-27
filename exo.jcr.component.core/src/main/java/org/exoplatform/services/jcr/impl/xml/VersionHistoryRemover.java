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
package org.exoplatform.services.jcr.impl.xml;

import org.exoplatform.services.jcr.access.AccessManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.dataflow.ItemDataRemoveVisitor;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;

import java.io.IOException;
import java.util.List;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS. Helper class for removing version history.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public class VersionHistoryRemover
{
   /**
    * Class logger.
    */
   private final Log log = ExoLogger.getLogger("exo.jcr.component.core.VersionHistoryRemover");

   /**
    * Version history identifier.
    */
   private final String vhID;

   /**
    * ItemDataConsumer.
    */
   private final ItemDataConsumer dataManager;

   /**
    * NodeTypeManager.
    */
   private final NodeTypeDataManager ntManager;

   /**
    * Repository.
    */
   private final RepositoryImpl repository;

   /**
    * Current workspace name.
    */
   private final String currentWorkspaceName;

   /**
    * Containing history path.
    */
   private final QPath containingHistory;

   /**
    * Ancestor to save.
    */
   private final QPath ancestorToSave;

   /**
    * Changes log.
    */
   private final PlainChangesLogImpl transientChangesLog;

   /**
    * Access manager.
    */
   private final AccessManager accessManager;

   /**
    * User state.
    */
   private final ConversationState userState;

   /**
    * @param vhID - version history identifier.
    * @param dataManager - ItemDataConsumer.
    * @param ntManager - NodeTypeManagerImpl.
    * @param repository - RepositoryImpl.
    * @param currentWorkspaceName - current workspace name.
    * @param containingHistory - containingHistory.
    * @param ancestorToSave - ancestor to save.
    * @param transientChangesLog - changes log.
    * @param accessManager - access manager.
    * @param userState - user state.
    */
   public VersionHistoryRemover(String vhID, ItemDataConsumer dataManager, NodeTypeDataManager ntManager,
      RepositoryImpl repository, String currentWorkspaceName, QPath containingHistory, QPath ancestorToSave,
      PlainChangesLogImpl transientChangesLog, AccessManager accessManager, ConversationState userState)
   {
      super();
      this.vhID = vhID;
      this.dataManager = dataManager;
      this.ntManager = ntManager;
      this.repository = repository;
      this.currentWorkspaceName = currentWorkspaceName;
      this.containingHistory = containingHistory;
      this.ancestorToSave = ancestorToSave;
      this.transientChangesLog = transientChangesLog;
      this.accessManager = accessManager;
      this.userState = userState;
   }

   /**
    * Remove history.
    * 
    * @exception RepositoryException if an repository error occurs.
    */
   public void remove() throws RepositoryException
   {
      NodeData vhnode = (NodeData)dataManager.getItemData(vhID);

      if (vhnode == null)
      {
         ItemState vhState = null;
         List<ItemState> allStates = transientChangesLog.getAllStates();
         for (int i = allStates.size() - 1; i >= 0; i--)
         {
            ItemState state = allStates.get(i);
            if (state.getData().getIdentifier().equals(vhID))
               vhState = state;
         }
         if (vhState != null && vhState.isDeleted())
         {
            return;
         }

         throw new RepositoryException("Version history is not found. UUID: " + vhID
            + ". Context item (ancestor to save) " + ancestorToSave.getAsString());
      }

      // mix:versionable
      // we have to be sure that any versionable node somewhere in repository
      // doesn't refers to a VH of the node being deleted.
      for (String wsName : repository.getWorkspaceNames())
      {
         SessionImpl wsSession = repository.getSystemSession(wsName);
         try
         {
            for (PropertyData sref : wsSession.getTransientNodesManager().getReferencesData(vhID, false))
            {
               // Check if this VH isn't referenced from somewhere in workspace
               // or isn't contained in another one as a child history.
               // Ask ALL references incl. properties from version storage.
               if (sref.getQPath().isDescendantOf(Constants.JCR_VERSION_STORAGE_PATH))
               {
                  if (!sref.getQPath().isDescendantOf(vhnode.getQPath())
                     && (containingHistory != null ? !sref.getQPath().isDescendantOf(containingHistory) : true))
                     // has a reference to the VH in version storage,
                     // it's a REFERENCE property jcr:childVersionHistory of
                     // nt:versionedChild
                     // i.e. this VH is a child history in an another history.
                     // We can't remove this VH now.
                     return;
               }
               else if (!currentWorkspaceName.equals(wsName))
               {
                  // has a reference to the VH in traversed workspace,
                  // it's not a version storage, i.e. it's a property of versionable
                  // node somewhere in ws.
                  // We can't remove this VH now.
                  return;
               } // else -- if we has a references in workspace where the VH is being
               // deleted we can remove VH now.
            }
         }
         finally
         {
            wsSession.logout();
         }
      }

      // remove child versions from VH (if found)
      // ChildVersionRemoveVisitor cvremover = new
      // ChildVersionRemoveVisitor(session,
      // vhnode.getQPath(),
      // ancestorToSave);
      // vhnode.accept(cvremover);

      List<NodeData> childs = dataManager.getChildNodesData(vhnode);
      for (NodeData nodeData : childs)
      {
         if (ntManager.isNodeType(Constants.NT_VERSIONEDCHILD, vhnode.getPrimaryTypeName(), vhnode.getMixinTypeNames()))
         {
            PropertyData property =
               (PropertyData)dataManager.getItemData(nodeData, new QPathEntry(Constants.JCR_CHILDVERSIONHISTORY, 1),
                  ItemType.PROPERTY);

            if (property == null)
               throw new RepositoryException("Property " + Constants.JCR_CHILDVERSIONHISTORY.getAsString()
                  + " for node " + nodeData.getQPath().getAsString() + " not found");

            String childVhID;
            try
            {
               childVhID = new String(property.getValues().get(0).getAsByteArray());
            }
            catch (IOException e)
            {
               throw new RepositoryException("Child version history UUID read error " + e, e);
            }
            VersionHistoryRemover historyRemover =
               new VersionHistoryRemover(childVhID, dataManager, ntManager, repository, currentWorkspaceName,
                  containingHistory, ancestorToSave, transientChangesLog, accessManager, userState);
            historyRemover.remove();

         }

      }
      // remove VH
      ItemDataRemoveVisitor visitor = new ItemDataRemoveVisitor(dataManager, ancestorToSave);
      vhnode.accept(visitor);
      transientChangesLog.addAll(visitor.getRemovedStates());
   };

}
