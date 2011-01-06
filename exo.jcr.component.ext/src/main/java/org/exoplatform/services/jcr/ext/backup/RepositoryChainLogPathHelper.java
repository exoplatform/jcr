/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.ext.backup;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 2011
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id$
 */
public class RepositoryChainLogPathHelper
{

   public RepositoryChainLogPathHelper()
   {
   }

   /**
    * Will be returned relative path <name>/<name>.xml for all OS.
    * 
    * @param path
    *          String, path to 
    * @param backupDirCanonicalPath
    *          String, path to backup dir
    * @return String
    *           Will be returned relative path <name>/<name>.xml for all OS
    * @throws MalformedURLException
    *           
    */
   public String getRelativePath(String path, String backupDirCanonicalPath) throws MalformedURLException
   {
      URL urlPath = new URL(resolveFileURL("file:" + path));
      URL urlBackupDir = new URL(resolveFileURL("file:" + backupDirCanonicalPath));

      return urlPath.toString().replace(urlBackupDir.toString() + "/", "");
   }

   /**
    * Will be returned absolute path.
    * 
    * @param relativePath
    *          String, relative path.
    * @param backupDirCanonicalPath
    *          String, path to backup dir
    * @return String
    *           Will be returned absolute path.          
    * @throws MalformedURLException
    */
   public String getPath(String relativePath, String backupDirCanonicalPath) throws MalformedURLException
   {
      String path = "file:" + backupDirCanonicalPath + "/" + relativePath;

      URL urlPath = new URL(resolveFileURL(path));

      return urlPath.getFile();
   }

   private String resolveFileURL(String url)
   {
      // we ensure that we don't have windows path separator in the url
      url = url.replace('\\', '/');
      if (!url.startsWith("file:///"))
      {
         // The url is invalid, so we will fix it
         // it happens when we use a path of type file://${path}, under
         // linux or mac os the path will start with a '/' so the url
         // will be correct but under windows we will have something
         // like C:\ so the first '/' is missing
         if (url.startsWith("file://"))
         {
            // The url is of type file://, so one '/' is missing
            url = "file:///" + url.substring(7);
         }
         else if (url.startsWith("file:/"))
         {
            // The url is of type file:/, so two '/' are missing
            url = "file:///" + url.substring(6);
         }
         else
         {
            // The url is of type file:, so three '/' are missing
            url = "file:///" + url.substring(5);
         }
      }
      return url;
   }

}
