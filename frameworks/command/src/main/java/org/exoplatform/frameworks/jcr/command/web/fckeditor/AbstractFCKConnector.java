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
package org.exoplatform.frameworks.jcr.command.web.fckeditor;

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
      return "/rest/jcr/" + repoName + "/" + workspace + resource;
   }

}
