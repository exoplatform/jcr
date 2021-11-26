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

package org.exoplatform.frameworks.jcr.command.web.fckeditor;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.frameworks.jcr.command.web.GenericWebAppContext;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: AbstractFCKConnector.java 5800 2006-05-28 18:03:31Z geaz $
 */

public abstract class AbstractFCKConnector
{

   /**
    * Return FCKeditor current folder path.
    * 
    * @param context
    * @return String path
    */
   protected String getCurrentFolderPath(GenericWebAppContext context)
   {
      // To limit browsing set Servlet init param "digitalAssetsPath" with desired JCR path
      String rootFolderStr =
         (String)context.get("org.exoplatform.frameworks.jcr.command.web.fckeditor.digitalAssetsPath");

      if (rootFolderStr == null)
         rootFolderStr = "/";

      // set current folder
      String currentFolderStr = (String)context.get("CurrentFolder");
      if (currentFolderStr == null)
         currentFolderStr = "";
      else if (currentFolderStr.length() < rootFolderStr.length())
         currentFolderStr = rootFolderStr;

      return currentFolderStr;
   }

   /**
    * Compile REST path of the given resource.
    * 
    * @param workspace
    * @param resource
    *          , we assume that path starts with '/'
    * @return
    */
   protected String makeRESTPath(String repoName, String workspace, String resource)
   {
      final StringBuilder sb = new StringBuilder(512);
      ExoContainer container = ExoContainerContext.getCurrentContainerIfPresent();
      if (container instanceof PortalContainer)
      {
         PortalContainer pContainer = (PortalContainer)container;
         sb.append('/').append(pContainer.getRestContextName()).append('/');
      }
      else
      {
         sb.append('/').append(PortalContainer.DEFAULT_REST_CONTEXT_NAME).append('/');
      }
      return sb.append("jcr/").append(repoName).append('/').append(workspace).append(resource).toString();
   }

}
