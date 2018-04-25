/*
 * Copyright (C) 20012 eXo Platform SAS.
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
package org.exoplatform.services.jcr.webdav.util;

/**
 * WebDAV service initial parameters names {@link String} representation.
 * 
 * @author  <a href="mailto:dmi3.kuleshov@gmail.com">Dmitry Kuleshov</a>
 * @version $Id$
 */
public interface InitParamsNames
{
   /**
    * Initialization parameter {@link String} representation: default folder node type.
    */
   String DEF_FOLDER_NODE_TYPE = "def-folder-node-type";

   /**
    * Initialization parameter {@link String} representation: default file node type.
    */
   String DEF_FILE_NODE_TYPE = "def-file-node-type";

   /**
    * Initialization parameter {@link String} representation: default file mime type.
    */
   String DEF_FILE_MIME_TYPE = "def-file-mimetype";

   /**
    * Initialization parameter {@link String} representation: update policy.
    */
   String UPDATE_POLICY = "update-policy";

   /**
    * Initialization parameter {@link String} representation: autor version.
    */
   String AUTO_VERSION = "auto-version";

   /**
    * Initialization parameter {@link String} representation: cache control.
    */
   String CACHE_CONTROL = "cache-control";

   /**
    * Initialization parameter {@link String} representation: folder icon path.
    */
   String FOLDER_ICON_PATH = "folder-icon-path";

   /**
    * Initialization parameter {@link String} representation: file icon path.
    */
   String FILE_ICON_PATH = "file-icon-path";

   /**
    * Initialization parameter {@link String} representation: untrusted user agents list.
    */
   String UNTRUSTED_USER_AGENTS = "untrusted-user-agents";

   /**
    * Initialization parameter {@link String} representation: allowed file node types list.
    */
   String ALLOWED_FILE_NODE_TYPES = "allowed-file-node-types";

   /**
    * Initialization parameter {@link String} representation: allowed folder node types list.
    */
   String ALLOWED_FOLDER_NODE_TYPES = "allowed-folder-node-types";

   /**
    * Initialization parameter {@link String} representation: allowed folder auto version.
    */
   String ALLOWED_JCR_PATH_AUTO_VERSION = "allowed.folder.auto-version";

   /**
    * Initialization parameter {@link String} representation: enable auto creation version.
    */
   String ENABLE_AUTO_VERSION = "enableAutoVersion";

   /**
    * Initialization parameter {@link String} representation: folder listing paths allowed regex
    */
   String FOLDER_LISTING_PATHS_ALLOWED_REGEX = "folder-listing-paths-allowed-regex";

}