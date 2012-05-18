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
package org.exoplatform.services.jcr.util;

import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.datamodel.Identifier;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.PropertyImpl;
import org.exoplatform.services.jcr.impl.core.SessionDataManager;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.dataflow.ItemDataRemoveVisitor;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.dataflow.ValueDataConvertor;
import org.exoplatform.services.jcr.impl.xml.ItemDataKeeperAdapter;
import org.exoplatform.services.jcr.impl.xml.importing.ContentImporter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public class VersionHistoryImporter
{
   /**
    * Class logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.VersionHistoryImporter");

   /**
    * Versioned node.
    */
   private final NodeImpl versionableNode;

   /**
    * Version history data.
    */
   private final InputStream versionHistoryStream;

   /**
    * User session.
    */
   private final SessionImpl userSession;

   /**
    * Node-type data manager.
    */
   private final NodeTypeDataManager nodeTypeDataManager;

   /**
    * Data keeper.
    */
   private final ItemDataKeeperAdapter dataKeeper;

   /**
    * jcr:baseVersion - uuid.
    */
   private final String baseVersionUuid;

   /**
    * predecessors uuids.
    */
   private final String[] predecessors;

   /**
    * Version history - uuid.
    */
   private final String versionHistory;

   /**
    * Versionable node uuid.
    */
   private String uuid;

   /**
    * Versionable node path.
    */
   private String path;

   /**
    * VersionHistoryImporter constructor.
    * 
    * @param versionableNode - versionable node.
    * @param versionHistoryStream - Version history data.
    * @param baseVersionUuid - jcr:baseVersion - uuid.
    * @param predecessors - predecessors uuids.
    * @param versionHistory - Version history - uuid
    * @throws RepositoryException -if an error occurs while getting
    *           NodeTypesHolder.
    */
   public VersionHistoryImporter(NodeImpl versionableNode, InputStream versionHistoryStream, String baseVersionUuid,
      String[] predecessors, String versionHistory) throws RepositoryException
   {
      super();
      this.versionableNode = versionableNode;
      this.versionHistoryStream = versionHistoryStream;
      this.baseVersionUuid = baseVersionUuid;
      this.predecessors = predecessors;
      this.versionHistory = versionHistory;
      this.userSession = versionableNode.getSession();
      this.nodeTypeDataManager = userSession.getWorkspace().getNodeTypesHolder();
      this.dataKeeper = new ItemDataKeeperAdapter(userSession.getTransientNodesManager());
   }

   /**
    * Do import.
    * 
    * @throws RepositoryException -if an error occurs while importing.
    * @throws IOException -i f an error occurs while importing.
    */
   public void doImport() throws RepositoryException, IOException
   {
      try
      {
         uuid = versionableNode.getUUID();
         path = versionableNode.getVersionHistory().getParent().getPath();
         if (LOG.isDebugEnabled())
         {
            LOG.debug("Started: Import version history for node with path=" + path + " and UUID=" + uuid);
         }

         NodeData versionable = (NodeData)versionableNode.getData();
         // ----- VERSIONABLE properties -----
         // jcr:versionHistory
         TransientPropertyData vh =
            TransientPropertyData.createPropertyData(versionable, Constants.JCR_VERSIONHISTORY, PropertyType.REFERENCE,
               false, new TransientValueData(new Identifier(versionHistory)));

         // jcr:baseVersion
         TransientPropertyData bv =
            TransientPropertyData.createPropertyData(versionable, Constants.JCR_BASEVERSION, PropertyType.REFERENCE,
               false, new TransientValueData(new Identifier(baseVersionUuid)));

         // jcr:predecessors
         List<ValueData> values = new ArrayList<ValueData>();
         for (int i = 0; i < predecessors.length; i++)
         {
            values.add(new TransientValueData(new Identifier(predecessors[i])));
         }
         TransientPropertyData pd =
            TransientPropertyData.createPropertyData(versionable, Constants.JCR_PREDECESSORS, PropertyType.REFERENCE,
               true, values);

         PlainChangesLog changesLog = new PlainChangesLogImpl();
         RemoveVisitor rv = new RemoveVisitor();
         rv.visit((NodeData)((NodeImpl)versionableNode.getVersionHistory()).getData());
         changesLog.addAll(rv.getRemovedStates());
         changesLog.add(ItemState.createAddedState(vh));
         changesLog.add(ItemState.createAddedState(bv));
         changesLog.add(ItemState.createAddedState(pd));
         // remove version properties to avoid referential integrety check
         PlainChangesLog changesLogDelete = new PlainChangesLogImpl();

         changesLogDelete.add(ItemState.createDeletedState(((PropertyImpl)versionableNode
            .getProperty("jcr:versionHistory")).getData()));
         changesLogDelete.add(ItemState.createDeletedState(((PropertyImpl)versionableNode
            .getProperty("jcr:baseVersion")).getData()));
         changesLogDelete.add(ItemState.createDeletedState(((PropertyImpl)versionableNode
            .getProperty("jcr:predecessors")).getData()));
         dataKeeper.save(changesLogDelete);
         // remove version history
         dataKeeper.save(changesLog);
         userSession.save();

         // import new version history
         Map<String, Object> context = new HashMap<String, Object>();
         //context.put("versionablenode", versionableNode);
         context.put(ContentImporter.RESPECT_PROPERTY_DEFINITIONS_CONSTRAINTS, true);
         userSession.getWorkspace().importXML(path, versionHistoryStream, 0, context);
         userSession.save();

         if (LOG.isDebugEnabled())
         {
            LOG.debug("Completed: Import version history for node with path=" + path + " and UUID=" + uuid);
         }

         // fetch list of imported child nodes versions
         List<String> versionUuids = (List<String>)context.get(ContentImporter.LIST_OF_IMPORTED_VERSION_HISTORIES);
         if (versionUuids != null && !versionUuids.isEmpty())
         {
            updateVersionedChildNodes(versionUuids);
         }
      }
      catch (RepositoryException exception)
      {
         LOG.error("Failed: Import version history for node with path=" + path + " and UUID=" + uuid, exception);
         throw new RepositoryException(exception);
      }
      catch (IOException exception)
      {
         LOG.error("Failed: Import version history for node with path=" + path + " and UUID=" + uuid, exception);
         IOException newException = new IOException();
         newException.initCause(exception);
         throw newException;
      }
   }

   /**
    * Update child nodes that owns versions from versionUuids list. 
    * 
    * @param versionUuids - list of version histories uuids.
    * @throws RepositoryException
    * @throws IOException
    */
   private void updateVersionedChildNodes(List<String> versionUuids) throws RepositoryException, IOException
   {
      SessionDataManager dataManager = userSession.getTransientNodesManager();

      NodeData versionStorage = (NodeData)dataManager.getItemData(Constants.VERSIONSTORAGE_UUID);

      for (String versionUuid : versionUuids)
      {
         NodeData versionHistoryData =
            (NodeData)dataManager.getItemData(versionStorage, new QPathEntry("", versionUuid, 1), ItemType.NODE);

         PropertyData versionableUuidProp =
            (PropertyData)dataManager.getItemData(versionHistoryData, new QPathEntry(Constants.JCR_VERSIONABLEUUID, 1),
               ItemType.PROPERTY);

         String versionableUuid = ValueDataConvertor.readString(versionableUuidProp.getValues().get(0));

         // fetch child versionable node

         NodeData versionedChild = (NodeData)dataManager.getItemData(versionableUuid);

         if (versionedChild != null && versionedChild.getQPath().isDescendantOf(versionableNode.getData().getQPath()))
         {
            // find latest version
            String latestVersionUuid = null;
            for (int versionNumber = 1;; versionNumber++)
            {
               NodeData nodeData =
                  (NodeData)dataManager.getItemData(versionHistoryData, new QPathEntry("", Integer
                     .toString(versionNumber), 1), ItemType.NODE);

               if (nodeData == null)
               {
                  break;
               }
               else
               {
                  latestVersionUuid = nodeData.getIdentifier();
               }
            }

            if (latestVersionUuid == null)
            {
               // fetch root version
               NodeData rootVersion =
                  (NodeData)dataManager.getItemData(versionHistoryData, new QPathEntry(Constants.JCR_ROOTVERSION, 1),
                     ItemType.NODE);
               latestVersionUuid = rootVersion.getIdentifier();
            }

            PropertyData propVersionHistory =
               (PropertyData)dataManager.getItemData(versionedChild, new QPathEntry(Constants.JCR_VERSIONHISTORY, 1),
                  ItemType.PROPERTY);
            String prevVerHistoryId = ValueDataConvertor.readString(propVersionHistory.getValues().get(0));

            PropertyData propBaseVersion =
               (PropertyData)dataManager.getItemData(versionedChild, new QPathEntry(Constants.JCR_BASEVERSION, 1),
                  ItemType.PROPERTY);

            PropertyData propPredecessors =
               (PropertyData)dataManager.getItemData(versionedChild, new QPathEntry(Constants.JCR_PREDECESSORS, 1),
                  ItemType.PROPERTY);

            TransientPropertyData newVersionHistoryProp =
               TransientPropertyData.createPropertyData(versionedChild, Constants.JCR_VERSIONHISTORY,
                  PropertyType.REFERENCE, false, new TransientValueData(new Identifier(versionUuid)));

            // jcr:baseVersion
            TransientPropertyData newBaseVersionProp =
               TransientPropertyData.createPropertyData(versionedChild, Constants.JCR_BASEVERSION,
                  PropertyType.REFERENCE, false, new TransientValueData(new Identifier(latestVersionUuid)));

            // jcr:predecessors
            List<ValueData> predecessorValues = new ArrayList<ValueData>();
            predecessorValues.add(new TransientValueData(new Identifier(latestVersionUuid)));
            TransientPropertyData newPredecessorsProp =
               TransientPropertyData.createPropertyData(versionedChild, Constants.JCR_PREDECESSORS,
                  PropertyType.REFERENCE, true, predecessorValues);

            //remove previous version of childnode nad update properties
            NodeData prevVersionHistory = (NodeData)dataManager.getItemData(prevVerHistoryId);

            PlainChangesLogImpl changesLog = new PlainChangesLogImpl();
            if (!prevVerHistoryId.equals(versionUuid))
            {
               RemoveVisitor rv = new RemoveVisitor();
               rv.visit(prevVersionHistory);
               changesLog.addAll(rv.getRemovedStates());
            }
            changesLog.add(ItemState.createAddedState(newVersionHistoryProp));
            changesLog.add(ItemState.createAddedState(newBaseVersionProp));
            changesLog.add(ItemState.createAddedState(newPredecessorsProp));

            PlainChangesLogImpl changesLogDelete = new PlainChangesLogImpl();
            changesLogDelete.add(ItemState.createDeletedState(propVersionHistory));
            changesLogDelete.add(ItemState.createDeletedState(propBaseVersion));
            changesLogDelete.add(ItemState.createDeletedState(propPredecessors));
            dataKeeper.save(changesLogDelete);
            // remove version history
            dataKeeper.save(changesLog);
            userSession.save();
            if (LOG.isDebugEnabled())
            {
               LOG.debug("Completed: Import version history for node with path="
                  + versionedChild.getQPath().getAsString() + " and UUID=" + versionedChild.getIdentifier());
            }
         }
      }
   }

   /**
    * Remover helper.
    * 
    * @author sj
    */
   protected class RemoveVisitor extends ItemDataRemoveVisitor
   {
      /**
       * Default constructor.
       * 
       * @throws RepositoryException - exception.
       */
      RemoveVisitor() throws RepositoryException
      {
         super(userSession.getTransientNodesManager(), null,
         // userSession.getWorkspace().getNodeTypeManager(),
            nodeTypeDataManager, userSession.getAccessManager(), userSession.getUserState());
      }

      /**
       * {@inheritDoc}
       */
      protected void validateReferential(NodeData node) throws RepositoryException
      {
         // no REFERENCE validation here
      }
   };
}
