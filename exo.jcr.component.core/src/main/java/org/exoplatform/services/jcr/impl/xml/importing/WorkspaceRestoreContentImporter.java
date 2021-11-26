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

package org.exoplatform.services.jcr.impl.xml.importing;

import org.exoplatform.services.jcr.access.AccessManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;

import java.util.Map;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.NamespaceRegistry;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter
 *         Nedonosko</a>
 * @version $Id: WorkspaceRestoreContentImporter.java 14100 2008-05-12 10:53:47Z
 *          gazarenkov $
 */
public class WorkspaceRestoreContentImporter extends WorkspaceContentImporter
{

   protected final Log log = ExoLogger.getLogger("exo.jcr.component.core.WorkspaceRestoreContentImporter");

   /**
    * Class used to import content of workspace, using "System View XML Mapping",
    * e.g. for restore data during backup. <br> Assumes that there is not root,
    * i.e. workspace not initialized.
    * 
    * @param dataConsumer
    * @param ntManager
    * @param locationFactory
    * @param valueFactory
    * @param namespaceRegistry
    * @param accessManager
    * @param context
    */
   public WorkspaceRestoreContentImporter(ItemDataConsumer dataConsumer, NodeTypeDataManager ntManager,
      LocationFactory locationFactory, ValueFactoryImpl valueFactory, NamespaceRegistry namespaceRegistry,
      AccessManager accessManager, ConversationState userState, Map<String, Object> context, RepositoryImpl repository,
      String currentWorkspaceName)
   {
      super(null, Constants.ROOT_PATH, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW, dataConsumer, ntManager,
         locationFactory, valueFactory, namespaceRegistry, accessManager, userState, context, repository,
         currentWorkspaceName);

   }
}
