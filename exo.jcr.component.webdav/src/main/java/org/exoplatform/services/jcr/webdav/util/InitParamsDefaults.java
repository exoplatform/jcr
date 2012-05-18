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
 * Default values of default WebDAV service parameters.
 * Don't be confused, this values are used if none are configured,
 * but they may be changed (e.g. via configuration files).
 * 
 * @author  <a href="mailto:dmi3.kuleshov@gmail.com">Dmitry Kuleshov</a>
 * @version $Id$
 */
public interface InitParamsDefaults
{
   /**
    * Folder node type parameter default value.
    */
   String FOLDER_NODE_TYPE = "nt:folder";

   /**
    * File node type parameter default value.
    */
   String FILE_NODE_TYPE = "nt:file";

   /**
    * File mime type parameter default value.
    */
   String FILE_MIME_TYPE = "application/octet-stream";

   /**
    * Update policy parameter default value.
    */
   String UPDATE_POLICY = "create-version";

   /**
    * Auto-version parameter default value.
    */
   String AUTO_VERSION = "checkout-checkin";

}