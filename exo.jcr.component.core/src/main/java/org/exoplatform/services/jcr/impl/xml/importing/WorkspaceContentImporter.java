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
package org.exoplatform.services.jcr.impl.xml.importing;

import java.util.Map;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;

import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.AccessManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.exoplatform.services.jcr.impl.xml.importing.dataflow.ImportNodeData;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: WorkspaceContentImporter.java 14100 2008-05-12 10:53:47Z
 *          gazarenkov $
 */
public class WorkspaceContentImporter extends SystemViewImporter
{
   /**
    * Class logger.
    */
   protected final Log log = ExoLogger.getLogger("exo.jcr.component.core.WorkspaceContentImporter");

   /**
    * The flag indicates whether a verified that the first element is the root.
    */
   protected boolean isFirstElementChecked = false;

   /**
    * Class used to import content of workspace, using "System View XML Mapping",
    * e.g. for restore data during backup. <br/> Assumes that root node of the
    * workspace was already created, initialized and given as parent. <br/> If
    * <b>system</b> workspace initialized from a scratch it will already contains
    * root (/) and /jcr:system nodes, namespaces and nodetypes were registered.
    * 
    * @param parent , should not be null
    * @param uuidBehavior
    * @param saveType
    * @param context
    */
   public WorkspaceContentImporter(NodeData parent, QPath ancestorToSave, int uuidBehavior,
      ItemDataConsumer dataConsumer, NodeTypeDataManager ntManager, LocationFactory locationFactory,
      ValueFactoryImpl valueFactory, NamespaceRegistry namespaceRegistry, AccessManager accessManager,
      ConversationState userState, Map<String, Object> context, RepositoryImpl repository, String currentWorkspaceName)
   {
      super(parent, ancestorToSave, uuidBehavior, dataConsumer, ntManager, locationFactory, valueFactory,
         namespaceRegistry, accessManager, userState, context, repository, currentWorkspaceName);
   }

   /*
    * (non-Javadoc)
    * @see
    * org.exoplatform.services.jcr.impl.xml.importing.SystemViewImporter#startElement
    * (java.lang.String , java.lang.String, java.lang.String, java.util.Map)
    */
   @Override
   public void startElement(String namespaceURI, String localName, String name, Map<String, String> atts)
      throws RepositoryException
   {
      InternalQName elementName = locationFactory.parseJCRName(name).getInternalName();

      if (Constants.SV_NODE_NAME.equals(elementName))
      {
         // sv:node element

         // node name (value of sv:name attribute)
         String svName = getAttribute(atts, Constants.SV_NAME_NAME);
         if (svName == null)
         {
            throw new RepositoryException("Missing mandatory sv:name attribute of element sv:node");
         }
         String svId = getAttribute(atts, Constants.EXO_ID_NAME);
         if (svId == null)
         {
            throw new RepositoryException("Missing mandatory exo:id attribute of element sv:node");
         }

         ImportNodeData newNodeData;
         InternalQName currentNodeName;
         int nodeIndex = 1;
         NodeData parentData = getParent();
         if (!isFirstElementChecked)
         {
            if (!ROOT_NODE_NAME.equals(svName))
               throw new RepositoryException("The first element must be root. But found '" + svName + "'");
            isFirstElementChecked = true;
         }

         if (ROOT_NODE_NAME.equals(svName))
         {
            currentNodeName = Constants.ROOT_PATH.getName();

            newNodeData = processRootNode(parentData.getQPath());
         }
         else
         {

            currentNodeName = locationFactory.parseJCRName(svName).getInternalName();
            nodeIndex = getNodeIndex(parentData, currentNodeName, null);
            newNodeData = new ImportNodeData(parentData, currentNodeName, nodeIndex);
            newNodeData.setOrderNumber(getNextChildOrderNum(parentData));
            newNodeData.setIdentifier(svId);
            changesLog.add(new ItemState(newNodeData, ItemState.ADDED, true, parentData.getQPath()));
         }
         tree.push(newNodeData);

         mapNodePropertiesInfo.put(newNodeData.getIdentifier(), new NodePropertiesInfo(newNodeData));
      }
      else
      {
         super.startElement(namespaceURI, localName, name, atts);
         if (Constants.SV_PROPERTY_NAME.equals(elementName))
         {
            String svId = getAttribute(atts, Constants.EXO_ID_NAME);
            if (svId == null)
            {
               throw new RepositoryException("Missing mandatory exo:id attribute of element sv:property");
            }
            propertyInfo.setIndentifer(svId);
         }
      }
   }

   protected ImportNodeData processRootNode(QPath parentPath)
   {
      // remove the wrong root from the stack
      tree.pop();

      ImportNodeData newNodeData =
         new ImportNodeData(Constants.ROOT_PATH, Constants.ROOT_UUID, -1, Constants.NT_UNSTRUCTURED,
            new InternalQName[0], 0, null, new AccessControlList());

      // Not persistent state. Root created during the creation workspace.
      changesLog.add(new ItemState(newNodeData, ItemState.ADDED, true, parentPath, false, false));

      return newNodeData;
   }

}
