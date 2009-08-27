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

import org.exoplatform.services.log.Log;

import org.exoplatform.services.jcr.access.AccessManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.dataflow.ItemDataKeeper;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.security.ConversationState;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: WorkspaceDataImporter.java 14100 2008-05-12 10:53:47Z
 *          gazarenkov $
 */
public class WorkspaceDataImporter
   extends StreamImporter
{
   private final Log log = ExoLogger.getLogger("jcr.WorkspaceDataImporter");

   public WorkspaceDataImporter(NodeData parent, int uuidBehavior, ItemDataKeeper dataKeeper,
            ItemDataConsumer dataConsumer, NodeTypeDataManager ntManager, LocationFactory locationFactory,
            ValueFactoryImpl valueFactory, NamespaceRegistry namespaceRegistry, AccessManager accessManager,
            ConversationState userState, Map<String, Object> context, RepositoryImpl repository,
            String currentWorkspaceName)
   {
      super(parent, uuidBehavior, dataKeeper, dataConsumer, ntManager, locationFactory, valueFactory,
               namespaceRegistry, accessManager, userState, context, repository, currentWorkspaceName);
      if (!Constants.ROOT_PATH.equals(parent.getQPath()))
      {
         throw new IllegalArgumentException("Current element should be root");
      }
   }

   /*
    * (non-Javadoc)
    * @seeorg.exoplatform.services.jcr.impl.xml.importing.StreamImporter#
    * createContentImporter(org. exoplatform.services.jcr.impl.core.NodeImpl,
    * int, org.exoplatform.services.jcr.impl.xml.XmlSaveType,
    * org.exoplatform.services.ext.action.InvocationContext)
    */
   @Override
   public ContentImporter createContentImporter(NodeData parent, int uuidBehavior, ItemDataConsumer dataConsumer,
            NodeTypeDataManager ntManager, LocationFactory locationFactory, ValueFactoryImpl valueFactory,
            NamespaceRegistry namespaceRegistry, AccessManager accessManager, ConversationState userState,
            Map<String, Object> context, RepositoryImpl repository, String currentWorkspaceName)
   {

      return new WorkspaceContentImporter(parent, parent.getQPath(), uuidBehavior, dataConsumer, ntManager,
               locationFactory, valueFactory, namespaceRegistry, accessManager, userState, context, repository,
               currentWorkspaceName);
   }

}
