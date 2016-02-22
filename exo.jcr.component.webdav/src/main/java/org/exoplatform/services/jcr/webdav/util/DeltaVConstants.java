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
package org.exoplatform.services.jcr.webdav.util;

import javax.xml.namespace.QName;

/**
 * Created by The eXo Platform SAS.
 * @author Vitaly Guly - gavrikvetal@gmail.com
 * 
 * @version $Id: $
 */

public interface DeltaVConstants extends PropertyConstants
{

   /**
    * WebDAV DeltaV checked-in property. See <a
    * href='http://www.ietf.org/rfc/rfc3253.txt'>Versioning Extensions to
    * WebDAV</a> for more information.
    */
   QName CHECKEDIN = new QName("DAV:", "checked-in");

   /**
    * WebDAV DeltaV checked-out property. See <a
    * href='http://www.ietf.org/rfc/rfc3253.txt'>Versioning Extensions to
    * WebDAV</a> for more information.
    */
   QName CHECKEDOUT = new QName("DAV:", "checked-out");

   /**
    * WebDAV DeltaV label-name-set property. See <a
    * href='http://www.ietf.org/rfc/rfc3253.txt'>Versioning Extensions to
    * WebDAV</a> for more information.
    */
   QName LABELNAMESET = new QName("DAV:", "label-name-set");

   /**
    * WebDAV DeltaV predecessor-set property. See <a
    * href='http://www.ietf.org/rfc/rfc3253.txt'>Versioning Extensions to
    * WebDAV</a> for more information.
    */
   QName PREDECESSORSET = new QName("DAV:", "predecessor-set");

   /**
    * WebDAV DeltaV successor-set property. See <a
    * href='http://www.ietf.org/rfc/rfc3253.txt'>Versioning Extensions to
    * WebDAV</a> for more information.
    */
   QName SUCCESSORSET = new QName("DAV:", "successor-set");

   /**
    * WebDAV DeltaV version-history property. See <a
    * href='http://www.ietf.org/rfc/rfc3253.txt'>Versioning Extensions to
    * WebDAV</a> for more information.
    */
   QName VERSIONHISTORY = new QName("DAV:", "version-history");

   /**
    * WebDAV DeltaV version-name property. See <a
    * href='http://www.ietf.org/rfc/rfc3253.txt'>Versioning Extensions to
    * WebDAV</a> for more information.
    */
   QName VERSIONNAME = new QName("DAV:", "version-name");

}
