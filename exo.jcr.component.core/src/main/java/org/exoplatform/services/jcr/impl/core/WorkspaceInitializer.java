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

package org.exoplatform.services.jcr.impl.core;

import org.exoplatform.services.jcr.datamodel.NodeData;
import org.picocontainer.Startable;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * Date: 05.05.2008
 * 
 * The interface responsible for workspace storage initialization including: - root node - for all
 * workspaces - /jcr:system - for system workspace /exo:namespaces /jcr:nodetypes
 * /jcr:versionStorage - search index root if configured
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: WorkspaceInitializer.java 13891 2008-05-05 16:02:30Z pnedonosko $
 */
public interface WorkspaceInitializer extends Startable
{

   public static final String ROOT_NODETYPE_PARAMETER = "root-nodetype";

   public static final String ROOT_PERMISSIONS_PARAMETER = "root-permissions";

   /**
    * Tell if the workspace is initialize.
    * 
    * @return boolean, if true - the workspace is initialized, false - otherwise.
    */
   boolean isWorkspaceInitialized() throws RepositoryException;

   /**
    * Perform the workspace initialization process.
    * 
    * @return
    * @throws RepositoryException
    */
   NodeData initWorkspace() throws RepositoryException;

}
