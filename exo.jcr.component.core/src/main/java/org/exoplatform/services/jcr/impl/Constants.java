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
package org.exoplatform.services.jcr.impl;

import org.exoplatform.services.jcr.datamodel.IllegalPathException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.QPath;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: Constants.java 13986 2008-05-08 10:48:43Z pnedonosko $
 */
public class Constants
{

   /**
    * Default namespace prefix (empty uri).
    */
   public static final String NS_EMPTY_PREFIX = "";

   /**
    * Default namespace URI (empty).
    */
   public static final String NS_DEFAULT_URI = "";

   /**
    * Reserved namespace for exo node types.
    */
   public static final String NS_EXO_PREFIX = "exo";

   /**
    * Reserved namespace URI for exo node types.
    */
   public static final String NS_EXO_URI = "http://www.exoplatform.com/jcr/exo/1.0";

   /**
    * Reserved namespace for items defined by built-in node types.
    */
   public static final String NS_JCR_PREFIX = "jcr";

   /**
    * Reserved namespace URI for items defined by built-in node types.
    */
   public static final String NS_JCR_URI = "http://www.jcp.org/jcr/1.0";

   /**
    * Reserved namespace for built-in primary node types.
    */
   public static final String NS_NT_PREFIX = "nt";

   /**
    * Reserved namespace URI for built-in primary node types.
    */
   public static final String NS_NT_URI = "http://www.jcp.org/jcr/nt/1.0";

   /**
    * Reserved namespace for built-in mixin node types.
    */
   public static final String NS_MIX_PREFIX = "mix";

   /**
    * Reserved namespace URI for built-in mixin node types.
    */
   public static final String NS_MIX_URI = "http://www.jcp.org/jcr/mix/1.0";

   /**
    * Reserved namespace used in the system view XML serialization format.
    */
   public static final String NS_SV_PREFIX = "sv";

   /**
    * Reserved namespace URI used in the system view XML serialization format.
    */
   public static final String NS_SV_URI = "http://www.jcp.org/jcr/sv/1.0";

   /**
    * Reserved namespaces that must not be redefined and should not be used.
    */
   public static final String NS_XML_PREFIX = "xml";

   /**
    * Reserved namespaces URI that must not be redefined and should not be used.
    */
   public static final String NS_XML_URI = "http://www.w3.org/XML/1998/namespace";

   // public static final String NS_XMLNS_PREFIX = "xmlns";

   // public static final String NS_XMLNS_URI = "http://www.w3.org/2000/xmlns/";

   // public static final String NS_XS_URI = "http://www.w3.org/2001/XMLSchema";

   // public static final String NS_FN_URI = "http://www.w3.org/2004/10/xpath-functions";

   /**
    * Default JCR name (empty).
    */
   public static final InternalQName JCR_DEFAULT_NAME = new InternalQName(NS_DEFAULT_URI, "");

   /**
    * The special wildcard name used as the name of residual item definitions.
    */
   public static final InternalQName JCR_ANY_NAME = new InternalQName("", "*");

   /**
    * Reserved namespace for exo node types (Portal/ECM).
    */
   public static final String NS_REP_PREFIX = "rep";

   /**
    * Reserved namespace URI for exo node types (Portal/ECM).
    */
   public static final String NS_REP_URI = "internal";

   /**
    * jcr:primaryType internal name.
    */
   public static final InternalQName JCR_PRIMARYTYPE = new InternalQName(NS_JCR_URI, "primaryType");

   /**
    * jcr:system internal name.
    */
   public static final InternalQName JCR_SYSTEM = new InternalQName(NS_JCR_URI, "system");

   /**
    * jcr:mixinTypes internal name.
    */
   public static final InternalQName JCR_MIXINTYPES = new InternalQName(NS_JCR_URI, "mixinTypes");

   /**
    * jcr:uuid internal name.
    */
   public static final InternalQName JCR_UUID = new InternalQName(NS_JCR_URI, "uuid");

   /**
    * jcr:frozenUuid internal name.
    */
   public static final InternalQName JCR_FROZENUUID = new InternalQName(NS_JCR_URI, "frozenUuid");

   /**
    * jcr:frozenNode internal name.
    */
   public static final InternalQName JCR_FROZENNODE = new InternalQName(NS_JCR_URI, "frozenNode");

   /**
    * jcr:path internal name.
    */
   public static final InternalQName JCR_PATH = new InternalQName(NS_JCR_URI, "path");

   /**
    * jcr:versionHistory internal name.
    */
   public static final InternalQName JCR_VERSIONHISTORY = new InternalQName(NS_JCR_URI, "versionHistory");

   /**
    * jcr:childVersionHistory internal name.
    */
   public static final InternalQName JCR_CHILDVERSIONHISTORY = new InternalQName(NS_JCR_URI, "childVersionHistory");

   /**
    * jcr:baseVersion internal name.
    */
   public static final InternalQName JCR_BASEVERSION = new InternalQName(NS_JCR_URI, "baseVersion");

   /**
    * jcr:isCheckedOut internal name.
    */
   public static final InternalQName JCR_ISCHECKEDOUT = new InternalQName(NS_JCR_URI, "isCheckedOut");

   /**
    * jcr:versionLabels internal name.
    */
   public static final InternalQName JCR_VERSIONLABELS = new InternalQName(NS_JCR_URI, "versionLabels");

   /**
    * jcr:versionStorage internal name.
    */
   public static final InternalQName JCR_VERSIONSTORAGE = new InternalQName(NS_JCR_URI, "versionStorage");

   /**
    * jcr:versionableUuid internal name.
    */
   public static final InternalQName JCR_VERSIONABLEUUID = new InternalQName(NS_JCR_URI, "versionableUuid");

   /**
    * jcr:predecessors internal name.
    */
   public static final InternalQName JCR_PREDECESSORS = new InternalQName(NS_JCR_URI, "predecessors");

   /**
    * jcr:rootVersion internal name.
    */
   public static final InternalQName JCR_ROOTVERSION = new InternalQName(NS_JCR_URI, "rootVersion");

   /**
    * jcr:created internal name.
    */
   public static final InternalQName JCR_CREATED = new InternalQName(NS_JCR_URI, "created");

   /**
    * jcr:mimeType internal name.
    */
   public static final InternalQName JCR_MIMETYPE = new InternalQName(NS_JCR_URI, "mimeType");

   /**
    * jcr:encoding internal name.
    */
   public static final InternalQName JCR_ENCODING = new InternalQName(NS_JCR_URI, "encoding");

   /**
    * jcr:content internal name.
    */
   public static final InternalQName JCR_CONTENT = new InternalQName(NS_JCR_URI, "content");

   /**
    * jcr:content internal name.
    */
   public static final InternalQName JCR_XMLTEXT = new InternalQName(NS_JCR_URI, "xmltext");

   /**
    * jcr:xmlcharacters internal name.
    */
   public static final InternalQName JCR_XMLCHARACTERS = new InternalQName(NS_JCR_URI, "xmlcharacters");

   /**
    * jcr:score internal name.
    */
   public static final InternalQName JCR_SCORE = new InternalQName(NS_JCR_URI, "score");

   /**
    * jcr:nodeTypeName internal name.
    */
   public static final InternalQName JCR_NODETYPENAME = new InternalQName(NS_JCR_URI, "nodeTypeName");

   /**
    * jcr:isMixin internal name.
    */
   public static final InternalQName JCR_ISMIXIN = new InternalQName(NS_JCR_URI, "isMixin");

   /**
    * jcr:hasOrderableChildNodes internal name.
    */
   public static final InternalQName JCR_HASORDERABLECHILDNODES =
      new InternalQName(NS_JCR_URI, "hasOrderableChildNodes");

   /**
    * jcr:primaryItemName internal name.
    */
   public static final InternalQName JCR_PRIMARYITEMNAME = new InternalQName(NS_JCR_URI, "primaryItemName");

   /**
    * jcr:frozenNode internal name.
    */
   public static final InternalQName JCR_SUPERTYPES = new InternalQName(NS_JCR_URI, "supertypes");

   /**
    * jcr:supertypes internal name.
    */
   public static final InternalQName JCR_PROPERTYDEFINITION = new InternalQName(NS_JCR_URI, "propertyDefinition");

   /**
    * jcr:childNodeDefinition internal name.
    */
   public static final InternalQName JCR_CHILDNODEDEFINITION = new InternalQName(NS_JCR_URI, "childNodeDefinition");

   /**
    * jcr:name internal name.
    */
   public static final InternalQName JCR_NAME = new InternalQName(NS_JCR_URI, "name");

   /**
    * jcr:autoCreated internal name.
    */
   public static final InternalQName JCR_AUTOCREATED = new InternalQName(NS_JCR_URI, "autoCreated");

   /**
    * jcr:protected internal name.
    */
   public static final InternalQName JCR_PROTECTED = new InternalQName(NS_JCR_URI, "protected");

   /**
    * jcr:multiple internal name.
    */
   public static final InternalQName JCR_MULTIPLE = new InternalQName(NS_JCR_URI, "multiple");

   /**
    * jcr:onParentVersion internal name.
    */
   public static final InternalQName JCR_ONPARENTVERSION = new InternalQName(NS_JCR_URI, "onParentVersion");

   /**
    * jcr:mandatory internal name.
    */
   public static final InternalQName JCR_MANDATORY = new InternalQName(NS_JCR_URI, "mandatory");

   /**
    * jcr:requiredType internal name.
    */
   public static final InternalQName JCR_REQUIREDTYPE = new InternalQName(NS_JCR_URI, "requiredType");

   /**
    * jcr:requiredType internal name.
    */
   public static final InternalQName JCR_VALUECONSTRAINTS = new InternalQName(NS_JCR_URI, "valueConstraints");

   /**
    * jcr:defaultValues internal name.
    */
   public static final InternalQName JCR_DEFAULTVALUES = new InternalQName(NS_JCR_URI, "defaultValues");

   /**
    * jcr:requiredPrimaryTypes internal name.
    */
   public static final InternalQName JCR_REQUIREDPRIMARYTYPES = new InternalQName(NS_JCR_URI, "requiredPrimaryTypes");

   /**
    * jcr:sameNameSiblings internal name.
    */
   public static final InternalQName JCR_SAMENAMESIBLINGS = new InternalQName(NS_JCR_URI, "sameNameSiblings");

   /**
    * jcr:defaultPrimaryType internal name.
    */
   public static final InternalQName JCR_DEFAULTPRIMNARYTYPE = new InternalQName(NS_JCR_URI, "defaultPrimaryType");

   /**
    * jcr:mergeFailed internal name.
    */
   public static final InternalQName JCR_MERGEFAILED = new InternalQName(NS_JCR_URI, "mergeFailed");

   /**
    * jcr:lockOwner internal name.
    */
   public static final InternalQName JCR_LOCKOWNER = new InternalQName(NS_JCR_URI, "lockOwner");

   /**
    * jcr:lockIsDeep internal name.
    */
   public static final InternalQName JCR_LOCKISDEEP = new InternalQName(NS_JCR_URI, "lockIsDeep");

   /**
    * jcr:nodetypes internal name.
    */
   public static final InternalQName JCR_NODETYPES = new InternalQName(NS_JCR_URI, "nodetypes");

   /**
    * jcr:frozenPrimaryType internal name.
    */
   public static final InternalQName JCR_FROZENPRIMARYTYPE = new InternalQName(NS_JCR_URI, "frozenPrimaryType");

   /**
    * jcr:frozenMixinTypes internal name.
    */
   public static final InternalQName JCR_FROZENMIXINTYPES = new InternalQName(NS_JCR_URI, "frozenMixinTypes");

   /**
    * jcr:successors internal name.
    */
   public static final InternalQName JCR_SUCCESSORS = new InternalQName(NS_JCR_URI, "successors");

   /**
    * jcr:language internal name.
    */
   public static final InternalQName JCR_LANGUAGE = new InternalQName(NS_JCR_URI, "language");

   /**
    * jcr:statement internal name.
    */
   public static final InternalQName JCR_STATEMENT = new InternalQName(NS_JCR_URI, "statement");

   /**
    * jcr:data internal name.
    */
   public static final InternalQName JCR_DATA = new InternalQName(NS_JCR_URI, "data");

   /**
    * jcr:lastModified internal name.
    */
   public static final InternalQName JCR_LASTMODIFIED = new InternalQName(NS_JCR_URI, "lastModified");

   /**
    * nt:base internal name.
    */
   public static final InternalQName NT_BASE = new InternalQName(NS_NT_URI, "base");

   /**
    * mix:referenceable internal name.
    */
   public static final InternalQName MIX_REFERENCEABLE = new InternalQName(NS_MIX_URI, "referenceable");

   /**
    * mix:versionable internal name.
    */
   public static final InternalQName MIX_VERSIONABLE = new InternalQName(NS_MIX_URI, "versionable");

   /**
    * mix:lockable internal name.
    */
   public static final InternalQName MIX_LOCKABLE = new InternalQName(NS_MIX_URI, "lockable");

   /**
    * nt:versionHistory internal name.
    */
   public static final InternalQName NT_VERSIONHISTORY = new InternalQName(NS_NT_URI, "versionHistory");

   /**
    * nt:versionLabels internal name.
    */
   public static final InternalQName NT_VERSIONLABELS = new InternalQName(NS_NT_URI, "versionLabels");

   /**
    * nt:version internal name.
    */
   public static final InternalQName NT_VERSION = new InternalQName(NS_NT_URI, "version");

   /**
    * nt:frozenNode internal name.
    */
   public static final InternalQName NT_FROZENNODE = new InternalQName(NS_NT_URI, "frozenNode");

   /**
    * nt:unstructured internal name.
    */
   public static final InternalQName NT_UNSTRUCTURED = new InternalQName(NS_NT_URI, "unstructured");

   /**
    * nt:resource internal name.
    */
   public static final InternalQName NT_RESOURCE = new InternalQName(NS_NT_URI, "resource");

   /**
    * nt:nodeType internal name.
    */
   public static final InternalQName NT_NODETYPE = new InternalQName(NS_NT_URI, "nodeType");

   /**
    * nt:propertyDefinition internal name.
    */
   public static final InternalQName NT_PROPERTYDEFINITION = new InternalQName(NS_NT_URI, "propertyDefinition");

   /**
    * nt:childNodeDefinition internal name.
    */
   public static final InternalQName NT_CHILDNODEDEFINITION = new InternalQName(NS_NT_URI, "childNodeDefinition");

   /**
    * nt:hierarchyNode internal name.
    */
   public static final InternalQName NT_HIERARCHYNODE = new InternalQName(NS_NT_URI, "hierarchyNode");

   /**
    * nt:versionedChild internal name.
    */
   public static final InternalQName NT_VERSIONEDCHILD = new InternalQName(NS_NT_URI, "versionedChild");

   /**
    * nt:query internal name.
    */
   public static final InternalQName NT_QUERY = new InternalQName(NS_NT_URI, "query");

   /**
    * nt:file internal name.
    */
   public static final InternalQName NT_FILE = new InternalQName(NS_NT_URI, "file");

   /**
    * nt:folder internal name.
    */
   public static final InternalQName NT_FOLDER = new InternalQName(NS_NT_URI, "folder");

   /**
    * Workspace root node URI.
    */
   public static final String ROOT_URI = "[]:1";

   /**
    * Workspace root node virtual parent node UUID.
    */
   public static final String ROOT_PARENT_UUID = " ".intern(); // empty

   /**
    * Workspace root node virtual parent node name.
    */
   public static final String ROOT_PARENT_NAME = "__root_parent";

   /**
    * Workspace root node virtual container name.
    */
   public static final String ROOT_PARENT_CONAINER_NAME = "__root_parent_container";

   /**
    * Workspace root node UUID.
    */
   public static final String ROOT_UUID = "00exo0jcr0root0uuid0000000000000";

   /**
    * /jcr:system UUID.
    */
   public static final String SYSTEM_UUID = "00exo0jcr0system0uuid00000000000";

   /**
    * /jcr:system/jcr:versionStorage UUID.
    */
   public static final String VERSIONSTORAGE_UUID = "00exo0jcr0version0storage0uuid00";

   /**
    * /jcr:system/jcr:nodetypes UUID.
    */
   public static final String NODETYPESROOT_UUID = "00exo0jcr0node0types0root0uuid00";

   /**
    * JCR base URI.
    */
   public static final String JCR_URI = "[http://www.jcp.org/jcr/1.0]";

   /**
    * jcr:primaryType URI.
    */
   public static final String PRIMARY_TYPE_URI = "[http://www.jcp.org/jcr/1.0]primaryType";

   /**
    * jcr:mixinTypes URI.
    */
   public static final String MIXIN_TYPE_URI = "[http://www.jcp.org/jcr/1.0]mixinTypes";

   public static final String ACCESS_TYPE_URI = "[http://www.exoplatform.com/jcr/exo/1.0]accessControllable";

   public static final String PRIVILEGABLE_TYPE_URI = "[http://www.exoplatform.com/jcr/exo/1.0]privilegeable";

   public static final String JCR_VERSION_STORAGE_URI =
      "[]:1[http://www.jcp.org/jcr/1.0]system:1[http://www.jcp.org/jcr/1.0]versionStorage:1";

   public static final String JCR_NODETYPES_URI =
      "[]:1[http://www.jcp.org/jcr/1.0]system:1[http://www.jcp.org/jcr/1.0]nodetypes:1";

   public static final String JCR_SYSTEM_URI = "[]:1[http://www.jcp.org/jcr/1.0]system:1";

   public static final InternalQName EXO_NAMESPACE = new InternalQName(NS_EXO_URI, "namespace");

   public static final InternalQName EXO_NAMESPACES = new InternalQName(NS_EXO_URI, "namespaces");

   public static final InternalQName EXO_URI_NAME = new InternalQName(NS_EXO_URI, "uri");

   public static final InternalQName EXO_PREFIX = new InternalQName(NS_EXO_URI, "prefix");

   public static final InternalQName EXO_VERSIONSTORAGE = new InternalQName(NS_EXO_URI, "versionStorage");

   public static final InternalQName EXO_ACCESS_CONTROLLABLE = new InternalQName(NS_EXO_URI, "accessControllable");

   public static final InternalQName EXO_OWNEABLE = new InternalQName(NS_EXO_URI, "owneable");

   public static final InternalQName EXO_PRIVILEGEABLE = new InternalQName(NS_EXO_URI, "privilegeable");

   public static final InternalQName EXO_OWNER = new InternalQName(NS_EXO_URI, "owner");

   public static final InternalQName EXO_PERMISSIONS = new InternalQName(NS_EXO_URI, "permissions");

   // -------- system view name constants
   /**
    * SV_NODE.
    */
   public static final String SV_NODE = "node";

   /**
    * SV_PROPERTY.
    */
   public static final String SV_PROPERTY = "property";

   /**
    * SV_VALUE.
    */
   public static final String SV_VALUE = "value";

   /**
    * SV_TYPE.
    */
   public static final String SV_TYPE = "type";

   /**
    * SV_NAME.
    */
   public static final String SV_NAME = "name";

   /**
    * EXO_ID.
    */
   public static final String EXO_ID = "id";

   /**
    * EXO_MULTIVALUED.
    */
   public static final String EXO_MULTIVALUED = "multivalued";

   /**
    * sv:node internal name.
    */
   public static final InternalQName SV_NODE_NAME = new InternalQName(NS_SV_URI, SV_NODE);

   /**
    * sv:property internal name.
    */
   public static final InternalQName SV_PROPERTY_NAME = new InternalQName(NS_SV_URI, SV_PROPERTY);

   /**
    * sv:value internal name.
    */
   public static final InternalQName SV_VALUE_NAME = new InternalQName(NS_SV_URI, SV_VALUE);

   /**
    * sv:type internal name.
    */
   public static final InternalQName SV_TYPE_NAME = new InternalQName(NS_SV_URI, SV_TYPE);

   /**
    * sv:name internal name.
    */
   public static final InternalQName SV_NAME_NAME = new InternalQName(NS_SV_URI, SV_NAME);

   /**
    * exo:id internal name.
    */
   public static final InternalQName EXO_ID_NAME = new InternalQName(NS_EXO_URI, EXO_ID);

   /**
    * /jcr:system/jcr:versionStorage internal path.
    */
   public static QPath JCR_VERSION_STORAGE_PATH;

   /**
    * /jcr:system/jcr:nodetypes internal path.
    */
   public static QPath JCR_NODETYPES_PATH;

   /**
    * /jcr:system internal path.
    */
   public static QPath JCR_SYSTEM_PATH;

   /**
    * /jcr:system/exo:namespaces internal path.
    */
   public static QPath EXO_NAMESPACES_PATH;

   /**
    * Workspace root node path.
    */
   public static QPath ROOT_PATH;

   /**
    * Chars quantity in a UUID String.
    */
   public static final int UUID_UNFORMATTED_LENGTH = 32;

   /**
    * Chars quantity in a UUID String.
    */
   public static final int UUID_FORMATTED_LENGTH = 32;

   /**
    * eXo JCR default Strings encoding.
    */
   public static final String DEFAULT_ENCODING = "UTF-8";
   
   /**
    * System identifier for remote workspace initializer changes.
    */
   public static final String JCR_CORE_RESTORE_WORKSPACE_INITIALIZER_SYSTEM_ID =
      "JCR_CORE_RESOTRE_WORKSPACE_INITIALIZER_SYSTEM_ID";

   /**
    * "unknown" constant.
    */
   public static final String UNKNOWN = "unknown";

   static
   {

      try
      {
         JCR_SYSTEM_PATH = QPath.parse(JCR_SYSTEM_URI);
      }
      catch (IllegalPathException e)
      {
         e.printStackTrace();
         System.err.println("ERROR: Can't parse JCR_SYSTEM_URI for constant JCR_SYSTEM (" + JCR_SYSTEM_URI + "): " + e);
      }

      try
      {
         JCR_VERSION_STORAGE_PATH = QPath.parse(JCR_VERSION_STORAGE_URI);
      }
      catch (IllegalPathException e)
      {
         e.printStackTrace();
         System.err.println("ERROR: Can't parse JCR_VERSION_STORAGE_URI for constant JCR_VERSION_STORAGE_PATH ("
            + JCR_VERSION_STORAGE_URI + "): " + e);
      }

      try
      {
         JCR_NODETYPES_PATH = QPath.parse(JCR_NODETYPES_URI);
      }
      catch (IllegalPathException e)
      {
         e.printStackTrace();
         System.err.println("ERROR: Can't parse JCR_NODETYPES_URI for constant JCR_NODETYPES_PATH ("
            + JCR_NODETYPES_URI + "): " + e);
      }

      String nsUri = JCR_SYSTEM_URI + EXO_NAMESPACES.getAsString() + ":1";
      try
      {
         EXO_NAMESPACES_PATH = QPath.parse(nsUri);
      }
      catch (IllegalPathException e)
      {
         e.printStackTrace();
         System.err.println("ERROR: Can't parse EXO_NAMESPACES_URI for constant EXO_NAMESPACES (" + nsUri + "): " + e);
      }

      try
      {
         ROOT_PATH = QPath.parse(ROOT_URI);
      }
      catch (IllegalPathException e)
      {
         e.printStackTrace();
         System.err.println("ERROR: Can't parse ROOT_URI " + e);
      }

   }

}
