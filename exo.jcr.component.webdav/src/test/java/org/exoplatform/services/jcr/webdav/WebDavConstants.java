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
package org.exoplatform.services.jcr.webdav;

/**
 * Created by The eXo Platform SAS Author : Dmytro Katayev
 * work.visor.ck@gmail.com Aug 18, 2008
 */
public class WebDavConstants
{

   public class WebDav
   {
      public static final String REALM = "eXo REST services";

      public static final String PORT_STRING = "8080";

      public static final int PORT_INT = 8080;
   }

   public class WebDavProp
   {

      public static final String NAMESPACE = "DAV:";

      public static final String ACTIVELOCK = "activelock";

      public static final String COMMENT = "comment";

      public static final String CHECKEDIN = "checked-in";

      public static final String CHECKEDOUT = "checked-out";

      public static final String DEPTH = "depth";

      public static final String DISPLAYNAME = "displayname";

      public static final String EXCLUSIVE = "exclusive";

      public static final String GETCONTENTLENGTH = "getcontentlength";

      public static final String GETCONTENTTYPE = "getcontenttype";

      public static final String GETLASTMODIFIED = "getlastmodified";

      public static final String HREF = "href";

      public static final String MULTISTATUS = "multistatus";

      public static final String OWNER = "owner";

      public static final String PROP = "prop";

      public static final String PROPSTAT = "propstat";

      public static final String RESOURCETYPE = "resourcetype";

      public static final String RESPONSE = "response";

      public static final String RESPONSEDESCRIPTION = "responsedescription";

      public static final String STATUS = "status";

      public static final String TIMEOUT = "timeout";

      public static final String VERSIONNAME = "version-name";

      public static final String LOCKDISCOVERY = "lockdiscovery";

      public static final String LOCKENTRY = "lockentry";

      public static final String LOCKSCOPE = "lockscope";

      public static final String LOCKTOKEN = "locktoken";

      public static final String LOCKTYPE = "locktype";

      public static final String WRITE = "write";

      public static final String ALLPROP = "allprop";

      public static final String COLLECTION = "collection";

      public static final String SHARED = "shared";

      public static final String REMOVE = "remove";

      public static final String SET = "set";

      public static final String CREATORDISPLAYNAME = "creator-displayname";

      public static final String SUCCESSORSET = "successor-set";

      public static final String PREDECESSORSET = "predecessor-set";

      public static final String SUPPORTEDLOCK = "supportedlock";

      public static final String ISCOLLECTION = "iscollection";

      public static final String CREATIONDATE = "creationdate";

      public static final String VERSIONHISTORY = "version-history";

      public static final String ORDERMEMBER = "order-member";

      public static final String SEGMENT = "segment";

      public static final String POSITION = "position";

      public static final String FIRST = "first";

      public static final String LAST = "last";

      public static final String BEFORE = "before";

      public static final String AFTER = "after";

      public static final String BASICSEARCH = "basicsearch";

      public static final String SELECT = "select";

      public static final String FROM = "from";

      public static final String WHERE = "where";

      public static final String SCOPE = "scope";

      public static final String SUPPORTEDQUERYGRAMMARSET = "supported-query-grammar-set";

      public static final String SUPPORTEDQUERYGRAMMAR = "supported-query-grammar";

      public static final String GRAMMAR = "grammar";

      public static final String SUPPORTEDMETHODSET = "supported-method-set";

      public static final String SUPPORTEDMETHOD = "supported-method";

      public static final String NAME = "name";

      public static final String PARENTNAME = "parentname";

      public static final String ISROOT = "isroot";

      public static final String ISVERSIONED = "isversioned";

      public static final String GETETAG = "getetag";

      public static final String HASCHILDREN = "haschildren";

      public static final String CHILDCOUNT = "childcount";

      public static final String ISFOLDER = "isfolder";

      public static final String CURRENT_USER_PRIVILEGE_SET = "current-user-privilege-set";

      public static final String SUPPORTED_PRIVILEGE_SET = "supported-privilege-set";

      public static final String ORDERING_TYPE = "ordering-type";
   }

   public class WebDAVMethods
   {
      public static final String PUT = "PUT";

      public static final String GET = "GET";

      public static final String HEAD = "HEAD";

      public static final String COPY = "COPY";

      public static final String MOVE = "MOVE";

      public static final String PROPFIND = "PROPFIND";

      public static final String PROPPATCH = "PROPPATCH";

      public static final String DELETE = "DELETE";

      public static final String MKCOL = "MKCOL";

      public static final String LOCK = "LOCK";

      public static final String UNLOCK = "UNLOCK";

      public static final String SEARCH = "SEARCH";

      public static final String CHECKIN = "CHECKIN";

      public static final String CHECKOUT = "CHECKOUT";

      public static final String REPORT = "REPORT";

      public static final String ORDERPATCH = "ORDERPATCH";

      public static final String ACL = "ACL";

   }

}
