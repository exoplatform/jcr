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

public interface PropertyConstants
{

   /**
    * WebDAV childcount property. See <a
    * href='http://www.ietf.org/rfc/rfc2518.txt'>Versioning Extensions to
    * WebDAV</a> for more information.
    */
   QName CHILDCOUNT = new QName("DAV:", "childcount");

   /**
    * WebDAV creationdate property. See <a
    * href='http://www.ietf.org/rfc/rfc2518.txt'>Versioning Extensions to
    * WebDAV</a> for more information.
    */
   QName CREATIONDATE = new QName("DAV:", "creationdate");

   /**
    * WebDAV displayname property. See <a
    * href='http://www.ietf.org/rfc/rfc2518.txt'>Versioning Extensions to
    * WebDAV</a> for more information.
    */
   QName DISPLAYNAME = new QName("DAV:", "displayname");

   /**
    * WebDAV getcontentlanguage property. See <a
    * href='http://www.ietf.org/rfc/rfc2518.txt'>Versioning Extensions to
    * WebDAV</a> for more information.
    */
   QName GETCONTENTLANGUAGE = new QName("DAV:", "getcontentlanguage");

   /**
    * WebDAV getcontentlength property. See <a
    * href='http://www.ietf.org/rfc/rfc2518.txt'>Versioning Extensions to
    * WebDAV</a> for more information.
    */
   QName GETCONTENTLENGTH = new QName("DAV:", "getcontentlength");

   /**
    * WebDAV getcontenttype property. See <a
    * href='http://www.ietf.org/rfc/rfc2518.txt'>Versioning Extensions to
    * WebDAV</a> for more information.
    */
   QName GETCONTENTTYPE = new QName("DAV:", "getcontenttype");

   /**
    * WebDAV property. See <a
    * href='http://www.ietf.org/rfc/rfc2518.txt'>Versioning Extensions to
    * WebDAV</a> for more information.
    */
   QName GETLASTMODIFIED = new QName("DAV:", "getlastmodified");

   /**
    * WebDAV getlastmodified property. See <a
    * href='http://www.ietf.org/rfc/rfc2518.txt'>Versioning Extensions to
    * WebDAV</a> for more information.
    */
   QName HASCHILDREN = new QName("DAV:", "haschildren");

   /**
    * WebDAV iscollection property. See <a
    * href='http://www.ietf.org/rfc/rfc2518.txt'>Versioning Extensions to
    * WebDAV</a> for more information.
    */
   QName ISCOLLECTION = new QName("DAV:", "iscollection");

   /**
    * WebDAV isfolder property. See <a
    * href='http://www.ietf.org/rfc/rfc2518.txt'>Versioning Extensions to
    * WebDAV</a> for more information.
    */
   QName ISFOLDER = new QName("DAV:", "isfolder");

   /**
    * WebDAV isroot property. See <a
    * href='http://www.ietf.org/rfc/rfc2518.txt'>Versioning Extensions to
    * WebDAV</a> for more information.
    */
   QName ISROOT = new QName("DAV:", "isroot");

   /**
    * WebDAV isversioned property. See <a
    * href='http://www.ietf.org/rfc/rfc2518.txt'>Versioning Extensions to
    * WebDAV</a> for more information.
    */
   QName ISVERSIONED = new QName("DAV:", "isversioned");

   /**
    * WebDAV parentname property. See <a
    * href='http://www.ietf.org/rfc/rfc2518.txt'>Versioning Extensions to
    * WebDAV</a> for more information.
    */
   QName PARENTNAME = new QName("DAV:", "parentname");

   /**
    * WebDAV resourcetype property. See <a
    * href='http://www.ietf.org/rfc/rfc2518.txt'>Versioning Extensions to
    * WebDAV</a> for more information.
    */
   QName RESOURCETYPE = new QName("DAV:", "resourcetype");

   /**
    * WebDAV supportedlock property. See <a
    * href='http://www.ietf.org/rfc/rfc2518.txt'>Versioning Extensions to
    * WebDAV</a> for more information.
    */
   QName SUPPORTEDLOCK = new QName("DAV:", "supportedlock");

   /**
    * WebDAV lockdiscovery property. See <a
    * href='http://www.ietf.org/rfc/rfc2518.txt'>Versioning Extensions to
    * WebDAV</a> for more information.
    */
   QName LOCKDISCOVERY = new QName("DAV:", "lockdiscovery");

   /**
    * WebDAV supported-method-set property. See <a
    * href='http://www.ietf.org/rfc/rfc2518.txt'>Versioning Extensions to
    * WebDAV</a> for more information.
    */
   QName SUPPORTEDMETHODSET = new QName("DAV:", "supported-method-set");

   /**
    * WebDAV lockscope property. See <a
    * href='http://www.ietf.org/rfc/rfc2518.txt'>Versioning Extensions to
    * WebDAV</a> for more information.
    */
   QName LOCKSCOPE = new QName("DAV:", "lockscope");

   /**
    * WebDAV locktype property. See <a
    * href='http://www.ietf.org/rfc/rfc2518.txt'>Versioning Extensions to
    * WebDAV</a> for more information.
    */
   QName LOCKTYPE = new QName("DAV:", "locktype");

   /**
    * WebDAV owner property. See <a
    * href='http://www.ietf.org/rfc/rfc2518.txt'>Versioning Extensions to
    * WebDAV</a> for more information.
    */
   QName OWNER = new QName("DAV:", "owner");

   /**
    * WebDAV exclusive property. See <a
    * href='http://www.ietf.org/rfc/rfc2518.txt'>Versioning Extensions to
    * WebDAV</a> for more information.
    */
   QName EXCLUSIVE = new QName("DAV:", "exclusive");

   /**
    * WebDAV write property. See <a
    * href='http://www.ietf.org/rfc/rfc2518.txt'>Versioning Extensions to
    * WebDAV</a> for more information.
    */
   QName WRITE = new QName("DAV:", "write");

   /**
    * WebDAV ordering-type property. See <a
    * href='http://www.ietf.org/rfc/rfc2518.txt'>Versioning Extensions to
    * WebDAV</a> for more information.
    */
   QName ORDERING_TYPE = new QName("DAV:", "ordering-type");

   /**
    * jcr:data property.
    */
   QName JCR_DATA = new QName("jcr:", "data");

   /**
    * jcr:content property.
    */
   QName JCR_CONTENT = new QName("jcr:", "content");

   /**
    * dav:isreadonly property for MicroSoft Webfolders extension.
    */
   QName IS_READ_ONLY = new QName("DAV:", "isreadonly");

   /**
    * dav:include element for dav:allprop of PROPFIND method
    * See <a href='http://www.webdav.org/specs/rfc4918.html#METHOD_PROPFIND'>HTTP Extensions for Web Distributed Authoring 
    * and Versioning (WebDAV)</a> for more information..
    */
   QName DAV_ALLPROP_INCLUDE = new QName("DAV:", "include");

   /**
    * dav:allprop element for dav:allprop of PROPFIND method
    * See <a href='http://www.webdav.org/specs/rfc4918.html#METHOD_PROPFIND'>HTTP Extensions for Web Distributed Authoring 
    * and Versioning (WebDAV)</a> for more information..
    */
   QName DAV_ALLPROP = new QName("DAV:", "allprop");

   /**
    * Creation date pattern.
    */
   String CREATION_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";

   /**
    * Last-Modified date pattern.
    */
   String MODIFICATION_PATTERN = "EEE, dd MMM yyyy HH:mm:ss z";

}
