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

import org.exoplatform.common.http.HTTPStatus;

import java.util.Hashtable;

/**
 * Constants used for webdav service implemetation.
 * 
 * @author <a href="mailto:gavrik-vetal@gmail.com">Vitaly Guly</a>
 */
public final class WebDavConst
{

   /**
    * Private constructor.
    */
   private WebDavConst()
   {
   }

   /**
    *
    */
   public static final String WDBDAV_COMMAND_CATALOG = "WEBDAV";

   /**
    * HTTP-protocol version.
    */
   public static final String HTTPVER = "HTTP/1.1";

   /**
    * Some boundary const.
    */
   public static final String BOUNDARY = "1234567890";

   /**
    * WebDav "MS-Author-Via" header value.
    */
   public static final String DAV_MSAUTHORVIA = "DAV";

   /**
    * WebDav "DAV" namespace value.
    */
   public static final String DAV_NAMESPACE = "DAV:";

   /**
    * WebDav "eXo" namespace value.
    */
   public static final String EXO_NAMESPACE = "http://exoplatform.com/jcr";

   /**
    * WebDav "DAV" prefix.
    */
   public static final String DAV_PREFIX = "D:";

   /**
    * WebDav server version.
    */
   public static final String DAV_SERVER = "eXo-Webdav Server /1.0";

   /**
    * WebDav default header value.
    */
   public static final String DAV_HEADER = "1, 2, ordered-collections";

   /**
    * DAV Searching And Locating request value.
    */
   public static final String DASL_VALUE =
      "<DAV:basicsearch>" + "<exo:sql xmlns:exo=\"http://exoplatform.com/jcr\"/>"
         + "<exo:xpath xmlns:exo=\"http://exoplatform.com/jcr\"/>";

   /**
    * WebDav version identifier.
    */
   public static final String DAV_VERSIONIDENTIFIER = "VERSIONID";

   /**
    * WebDav version prefix.
    */
   public static final String DAV_VERSIONPREFIX = "?" + DAV_VERSIONIDENTIFIER + "=";

   /**
    * WebDav default mime-type.
    */
   public static final String DAV_DEFAULT_MIME_TYPE = "text/plain";

   /**
    * Webdav document properties.
    * 
    * @author <a href="mailto:dkatayev@gmail.com">Dmytro Katayev</a>
    */
   public final class DavDocument
   {

      /**
       * Private constructor.
       */
      private DavDocument()
      {
      }

      /**
       * Webdav document "acl-principal-props" property. See <a
       * href='http://tools.ietf.org/html/draft-ietf-webdav-acl-06'>WebDAV Access
       * Control Protocol</a> for more information.
       */
      public static final String ACLPRINCIPALPROPS = "acl-principal-props";

      /**
       * Webdav document "acl-principal-prop-set" property. See <a
       * href='http://tools.ietf.org/html/draft-ietf-webdav-acl-06'>WebDAV Access
       * Control Protocol</a> for more information.
       */
      public static final String ACLPRINCIPALPROPSET = "acl-principal-prop-set";

      /**
       * Webdav document "expand property" property. See <a
       * href='http://www.ietf.org/rfc/rfc3253.txt'>Versioning Extensions to
       * WebDAV</a> for more information.
       */
      public static final String EXPANDPROPERTY = "";

      /**
       * Webdav document "lockinfo" property.
       */
      public static final String LOCKINFO = "lockinfo";

      /**
       * Webdav document "property behavior" property. See <a
       * href='http://www.ietf.org/rfc/rfc2518.txt'>HTTP Extensions for
       * Distributed Authoring </a> for more information.
       */
      public static final String PROPERTYBEHAVIOR = "";

      /**
       * Webdav document "propertyupdate" property. See <a
       * href='http://www.ietf.org/rfc/rfc2518.txt'>HTTP Extensions for
       * Distributed Authoring </a> for more information.
       */
      public static final String PROPERTYUPDATE = "propertyupdate";

      /**
       * Webdav document "propfind" property. See <a
       * href='http://www.ietf.org/rfc/rfc2518.txt'>HTTP Extensions for
       * Distributed Authoring </a> for more information.
       */
      public static final String PROPFIND = "propfind";

      /**
       * Webdav document "version-tree" property. See <a
       * href='http://www.ietf.org/rfc/rfc2518.txt'>HTTP Extensions for
       * Distributed Authoring </a> for more information.
       */
      public static final String VERSIONTREE = "version-tree";

      /**
       * Webdav document "orderpatch" property. See <a
       * href='http://www.ietf.org/rfc/rfc2518.txt'>HTTP Extensions for
       * Distributed Authoring </a> for more information.
       */
      public static final String ORDERPATCH = "orderpatch";

      /**
       * Webdav document "searchrequest" property. See <a
       * href='http://www.ietf.org/rfc/rfc2518.txt'>HTTP Extensions for
       * Distributed Authoring </a> for more information.
       */
      public static final String SEARCHREQUEST = "searchrequest";
   }

   /**
    * Webdav custom properties.
    * 
    * @author <a href="mailto:dkatayev@gmail.com">Dmytro Katayev</a>
    */
   public final class DavProperty
   {

      /**
       * Private constructor.
       */
      private DavProperty()
      {
      }

      /**
       * Webdav "depth" property. See <a
       * href='http://www.ietf.org/rfc/rfc2518.txt'>HTTP Extensions for
       * Distributed Authoring </a> for more information.
       */
      public static final String DEPTH = "depth";

      /**
       * Webdav "multistatus" property. See <a
       * href='http://www.ietf.org/rfc/rfc2518.txt'>HTTP Extensions for
       * Distributed Authoring </a> for more information.
       */
      public static final String MULTISTATUS = "multistatus";

      /**
       * Webdav "propfind" property. See <a
       * href='http://www.ietf.org/rfc/rfc2518.txt'>HTTP Extensions for
       * Distributed Authoring </a> for more information.
       */
      public static final String PROPFIND = "propfind";

      /**
       * Webdav "supported-method-set" property. See <a
       * href='http://www.ietf.org/rfc/rfc2518.txt'>HTTP Extensions for
       * Distributed Authoring </a> for more information.
       */
      public static final String SUPPORDEDMETHODSET = "supported-method-set";

   }

   /**
    * Webdav resource types.
    * 
    * @author <a href="mailto:dkatayev@gmail.com">Dmytro Katayev</a>
    */
   public final class ResourceType
   {

      /**
       * Private constructor.
       */
      private ResourceType()
      {
      }

      /**
       * Webdav "collection" resource type.
       */
      public static final String COLLECTION = "collection";

      /**
       * Webdav "resource" resource type.
       */
      public static final String RESOURCE = "resource";

   }

   /**
    * Webdav Cache constants.
    * 
    * @author <a href="mailto:dkatayev@gmail.com">Dmytro Katayev</a>
    */
   public final class CacheConstants
   {

      /**
       * Private constructor.
       */
      private CacheConstants()
      {
      }

      /**
       * Default Cache-Control header value.
       */
      public static final String NO_CACHE = "no-cache";

   }

   /**
    * Webdav locks types.
    * 
    * @author <a href="mailto:dkatayev@gmail.com">Dmytro Katayev</a>
    */
   public final class Lock
   {

      /**
       * Private constructor.
       */
      private Lock()
      {
      }

      /**
       * Webdav "shared" lock type.
       */
      public static final String SCOPE_SHARED = "shared";

      /**
       * Webdav "exclusive" lock type.
       */
      public static final String SCOPE_EXCLUSIVE = "exclusive";

      /**
       * Webdav "write" lock type.
       */
      public static final String TYPE_WRITE = "write";

      /**
       * opaquelocktoken for LockToken generation.
       */
      public static final String OPAQUE_LOCK_TOKEN = "opaquelocktoken";
   }

   /**
    * Jcr node types used by webdav.
    * 
    * @author <a href="mailto:dkatayev@gmail.com">Dmytro Katayev</a>
    */
   public final class NodeTypes
   {

      /**
       * Private constructor.
       */
      private NodeTypes()
      {
      }

      /**
       * JCR "jcr:content" NodeType. See <a
       * href='http://jcp.org/en/jsr/detail?id=170'> JSR 170: Content Repository
       * for JavaTM technology API</a> for more information.
       */
      public static final String JCR_CONTENT = "jcr:content";

      /**
       * JCR "jcr:nodeType" NodeType. See <a
       * href='http://jcp.org/en/jsr/detail?id=170'> JSR 170: Content Repository
       * for JavaTM technology API</a> for more information.
       */
      public static final String JCR_NODE_TYPE = "jcr:nodeType";

      /**
       * JCR "jcr:data" NodeType. See <a
       * href='http://jcp.org/en/jsr/detail?id=170'> JSR 170: Content Repository
       * for JavaTM technology API</a> for more information.
       */
      public static final String JCR_DATA = "jcr:data";

      /**
       * JCR "jcr:frozenNode" NodeType. See <a
       * href='http://jcp.org/en/jsr/detail?id=170'> JSR 170: Content Repository
       * for JavaTM technology API</a> for more information.
       */
      public static final String JCR_FROZENNODE = "jcr:frozenNode";

      /**
       * JCR "jcr:lockOwner" NodeType. See <a
       * href='http://jcp.org/en/jsr/detail?id=170'> JSR 170: Content Repository
       * for JavaTM technology API</a> for more information.
       */
      public static final String JCR_LOCKOWNER = "jcr:lockOwner";

      /**
       * JCR "nt:version" NodeType. See <a
       * href='http://jcp.org/en/jsr/detail?id=170'> JSR 170: Content Repository
       * for JavaTM technology API</a> for more information.
       */
      public static final String NT_VERSION = "nt:version";

      /**
       * JCR "jcr:created" NodeType. See <a
       * href='http://jcp.org/en/jsr/detail?id=170'> JSR 170: Content Repository
       * for JavaTM technology API</a> for more information.
       */
      public static final String JCR_CREATED = "jcr:created";

      /**
       * JCR "nt:file" NodeType. See <a
       * href='http://jcp.org/en/jsr/detail?id=170'> JSR 170: Content Repository
       * for JavaTM technology API</a> for more information.
       */
      public static final String NT_FILE = "nt:file";

      /**
       * JCR "jcr:rootVersion" NodeType. See <a
       * href='http://jcp.org/en/jsr/detail?id=170'> JSR 170: Content Repository
       * for JavaTM technology API</a> for more information.
       */
      public static final String JCR_ROOTVERSION = "jcr:rootVersion";

      /**
       * JCR "jcr:lastModified" NodeType. See <a
       * href='http://jcp.org/en/jsr/detail?id=170'> JSR 170: Content Repository
       * for JavaTM technology API</a> for more information.
       */
      public static final String JCR_LASTMODIFIED = "jcr:lastModified";

      /**
       * JCR "jcr:mimeType" NodeType. See <a
       * href='http://jcp.org/en/jsr/detail?id=170'> JSR 170: Content Repository
       * for JavaTM technology API</a> for more information.
       */
      public static final String JCR_MIMETYPE = "jcr:mimeType";

      /**
       * JCR "nt:resource" NodeType. See <a
       * href='http://jcp.org/en/jsr/detail?id=170'> JSR 170: Content Repository
       * for JavaTM technology API</a> for more information.
       */
      public static final String NT_RESOURCE = "nt:resource";

      /**
       * JCR "mix:lockable" NodeType. See <a
       * href='http://jcp.org/en/jsr/detail?id=170'> JSR 170: Content Repository
       * for JavaTM technology API</a> for more information.
       */
      public static final String MIX_LOCKABLE = "mix:lockable";

      /**
       * JCR "mix:versionable" NodeType. See <a
       * href='http://jcp.org/en/jsr/detail?id=170'> JSR 170: Content Repository
       * for JavaTM technology API</a> for more information.
       */
      public static final String MIX_VERSIONABLE = "mix:versionable";

      /**
       * JCR "nt:folder" NodeType. See <a
       * href='http://jcp.org/en/jsr/detail?id=170'> JSR 170: Content Repository
       * for JavaTM technology API</a> for more information.
       */
      public static final String NT_FOLDER = "nt:folder";
   }

   /**
    * Date format patterns used by webdav.
    * 
    * @author <a href="mailto:dkatayev@gmail.com">Dmytro Katayev</a>
    */
   public final class DateFormat
   {

      /**
       * Private constructor.
       */
      private DateFormat()
      {
      }

      /**
       * Creation date pattern.
       */
      public static final String CREATION = "yyyy-MM-dd'T'HH:mm:ss'Z'";

      /**
       * Last modification date psttern.
       */
      public static final String MODIFICATION = "EEE, dd MMM yyyy HH:mm:ss z";
      
      /**
       * If-Modified-Since date psttern.
       */
      public static final String IF_MODIFIED_SINCE_PATTERN = "EEE, dd MMM yyyy HH:mm:ss z";
   }

   /**
    * Contains HTTP/1.1 status description.
    */
   private static Hashtable<Integer, String> statusDescriptions = new Hashtable<Integer, String>();

   /**
    * Registers Status code and it's description.
    * 
    * @param status Status code
    * @param descr Description
    */
   private static void registerDescr(int status, String descr)
   {
      statusDescriptions.put(new Integer(status), descr);
   }

   static
   {
      registerDescr(HTTPStatus.CONTINUE, "Continue");
      registerDescr(HTTPStatus.SWITCHING_PROTOCOLS, "Switching Protocols");
      registerDescr(HTTPStatus.OK, "OK");
      registerDescr(HTTPStatus.CREATED, "Created");
      registerDescr(HTTPStatus.ACCEPTED, "Accepted");
      registerDescr(HTTPStatus.NOT_AUTHORITATIVE, "Non-Authoritative Information");
      registerDescr(HTTPStatus.NO_CONTENT, "No Content");
      registerDescr(HTTPStatus.RESET, "Reset Content");
      registerDescr(HTTPStatus.PARTIAL, "Partial Content");
      registerDescr(HTTPStatus.MULTISTATUS, "Multi Status");
      registerDescr(HTTPStatus.MULT_CHOICE, "Multiple Choices");
      registerDescr(HTTPStatus.MOVED_PERM, "Moved Permanently");
      registerDescr(HTTPStatus.FOUND, "Found");
      registerDescr(HTTPStatus.SEE_OTHER, "See Other");
      registerDescr(HTTPStatus.NOT_MODIFIED, "Not Modified");
      registerDescr(HTTPStatus.USE_PROXY, "Use Proxy");
      registerDescr(HTTPStatus.TEMP_REDIRECT, "Temporary Redirect");
      registerDescr(HTTPStatus.BAD_REQUEST, "Bad Request");
      registerDescr(HTTPStatus.UNAUTHORIZED, "Unauthorized");
      registerDescr(HTTPStatus.PAYMENT_REQUIRED, "Payment Required");
      registerDescr(HTTPStatus.FORBIDDEN, "Forbidden");
      registerDescr(HTTPStatus.NOT_FOUND, "Not Found");
      registerDescr(HTTPStatus.METHOD_NOT_ALLOWED, "Method Not Allowed");
      registerDescr(HTTPStatus.NOT_ACCEPTABLE, "Not Acceptable");
      registerDescr(HTTPStatus.PROXY_AUTH, "Proxy Authentication Required");
      registerDescr(HTTPStatus.REQUEST_TIMEOUT, "Request Timeout");
      registerDescr(HTTPStatus.CONFLICT, "Conflict");
      registerDescr(HTTPStatus.GONE, "Gone");
      registerDescr(HTTPStatus.LENGTH_REQUIRED, "Length Required");
      registerDescr(HTTPStatus.PRECON_FAILED, "Precondition Failed");
      registerDescr(HTTPStatus.REQ_TOO_LONG, "Request Entity Too Large");
      registerDescr(HTTPStatus.REQUEST_URI_TOO_LONG, "Request-URI Too Long");
      registerDescr(HTTPStatus.UNSUPPORTED_TYPE, "Unsupported Media Type");
      registerDescr(HTTPStatus.REQUESTED_RANGE_NOT_SATISFIABLE, "Requested Range Not Satisfiable");
      registerDescr(HTTPStatus.EXPECTATION_FAILED, "Expectation Failed");
      registerDescr(HTTPStatus.INTERNAL_ERROR, "Internal Server Error");
      registerDescr(HTTPStatus.NOT_IMPLEMENTED, "Not Implemented");
      registerDescr(HTTPStatus.BAD_GATEWAY, "Bad Gateway");
      registerDescr(HTTPStatus.UNAVAILABLE, "Service Unavailable");
      registerDescr(HTTPStatus.GATEWAY_TIMEOUT, "Gateway Timeout");
      registerDescr(HTTPStatus.HTTP_VERSION_NOT_SUPPORTED, "HTTP Version Not Supported");
   }

   /**
    * Returns status description by it's code.
    * 
    * @param status Status code
    * @return Status Description
    */
   public static String getStatusDescription(int status)
   {
      String description = "";

      Integer statusKey = new Integer(status);
      if (statusDescriptions.containsKey(statusKey))
      {
         description = statusDescriptions.get(statusKey);
      }

      return String.format("%s %d %s", WebDavConst.HTTPVER, status, description);
   }

}
