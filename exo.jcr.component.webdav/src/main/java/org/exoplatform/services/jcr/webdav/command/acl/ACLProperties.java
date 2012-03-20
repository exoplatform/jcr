/**
 * Copyright (C) 2010 eXo Platform SAS.
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
 *
 */

package org.exoplatform.services.jcr.webdav.command.acl;

import org.exoplatform.common.util.HierarchicalProperty;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.webdav.util.PropertyConstants;
import org.exoplatform.services.security.IdentityConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.xml.namespace.QName;

/**
 * Created by The eXo Platform SAS.
 * Utility class to simplify operations with ACL properties of JCR nodes
 * for PROPFIND method.
 * 
 * @author <a href="mailto:gavrikvetal@gmail.com">Vitaliy Gulyy</a>
 * @version $
 */

public class ACLProperties
{
   /**
    * Defines the name of the element corresponding to a protected property that specifies
    * the list of access control entries.
    * More details can be found <a href="http://www.webdav.org/specs/rfc3744.html#PROPERTY_acl">here</a>.   
    */
   public final static QName ACL = new QName("DAV:", "acl");

   /**
    * Defines the name of the element corresponding to a property that the set of privileges
    * to be either granted or denied to a single principal. 
    * More details can be found <a href="http://www.webdav.org/specs/rfc3744.html#PROPERTY_acl">here</a>. 
    */
   public final static QName ACE = new QName("DAV:", "ace");

   /**
    * Defines the name of the element corresponding to a property which identifies the principal
    * to which this ACE applies.
    * More details can be found <a href='http://www.webdav.org/specs/rfc3744.html#principals'>here</a>
    * and <a href="http://www.webdav.org/specs/rfc3744.html#PROPERTY_acl">here</a>. 
    */
   public final static QName PRINCIPAL = new QName("DAV:", "principal");

   /**
    * Defines the name of the element corresponding to a property that can be either an aggregate
    * privilege that contains the entire set of privileges that can be applied to the resource or
    * an aggregate principal that contains the entire set of principals.
    * More details can be found <a href="http://www.webdav.org/specs/rfc3744.html#PRIVILEGE_all">here</a>. 
    */
   public final static QName ALL = new QName("DAV:", "all");

   /**
    * Defines the name of the element corresponding to a property which is used to uniquely
    * identify a principal.
    * More details can be found <a href="http://www.webdav.org/specs/rfc3744.html#PROPERTY_principal-URL">here</a>. 
    */
   public final static QName HREF = new QName("DAV:", "href");

   /**
    * Defines the name of the element containing privilege's name.
    * More details can be found <a href="http://www.webdav.org/specs/rfc3744.html#privileges">here</a>. 
    */
   public final static QName PRIVILEGE = new QName("DAV:", "privilege");

   /**
    * Defines the name of the element containing privileges to be granted.
    * More details can be found <a href="http://www.webdav.org/specs/rfc3744.html#rfc.section.5.5.2">here</a>. 
    */
   public final static QName GRANT = new QName("DAV:", "grant");

   /**
    * Defines the name of the element containing privileges to be denied.
    * More details can be found <a href="http://www.webdav.org/specs/rfc3744.html#rfc.section.5.5.2">here</a>. 
    */
   public final static QName DENY = new QName("DAV:", "deny");

   /**
    * Defines the name of the element corresponding to write privilege
    * which in current implementation aggregate: 
    * ADD_NODE, SET_PROPERTY, REMOVE permissions.
    * More details can be found <a href="http://www.webdav.org/specs/rfc3744.html#privileges">here</a>. 
    */
   public final static QName WRITE = new QName("DAV:", "write");

   /**
    * Defines the name of the element corresponding to read privilege
    * which in current implementation aggregate: 
    * READ permission.
    * More details can be found <a href="http://www.webdav.org/specs/rfc3744.html#privileges">here</a>. 
    */
   public final static QName READ = new QName("DAV:", "read");

   /**
    * Gets {@link AccessControlList} and transform it to DAV:acl property view 
    * represented by a {@link HierarchicalProperty} instance.
    * @param node - {@link NodeImpl} from which we are to get an ACL
    * @return HierarchicalProperty - tree like structure corresponding to an DAV:acl property 
    * @throws RepositoryException
    */
   public static HierarchicalProperty getACL(NodeImpl node) throws RepositoryException
   {
      HierarchicalProperty property = new HierarchicalProperty(ACL);

      AccessControlList acl = node.getACL();

      HashMap<String, List<String>> principals = new HashMap<String, List<String>>();

      List<AccessControlEntry> entryList = acl.getPermissionEntries();
      for (AccessControlEntry entry : entryList)
      {
         String principal = entry.getIdentity();
         String grant = entry.getPermission();

         List<String> grantList = principals.get(principal);
         if (grantList == null)
         {
            grantList = new ArrayList<String>();
            principals.put(principal, grantList);
         }

         grantList.add(grant);
      }

      Iterator<String> principalIter = principals.keySet().iterator();
      while (principalIter.hasNext())
      {
         HierarchicalProperty aceProperty = new HierarchicalProperty(ACE);

         String curPrincipal = principalIter.next();

         aceProperty.addChild(getPrincipalProperty(curPrincipal));

         aceProperty.addChild(getGrantProperty(principals.get(curPrincipal)));

         property.addChild(aceProperty);
      }

      return property;
   }

   /**
    * Transform owner got from node's {@link AccessControlList} 
    * to tree like {@link HierarchicalProperty} instance to use in PROPFIND response body 
    * @param node 
    * @return {@link HierarchicalProperty} representation of node owner
    * @throws RepositoryException
    */
   public static HierarchicalProperty getOwner(NodeImpl node) throws RepositoryException
   {
      HierarchicalProperty ownerProperty = new HierarchicalProperty(PropertyConstants.OWNER);

      HierarchicalProperty href = new HierarchicalProperty(new QName("DAV:", "href"));
      href.setValue(node.getACL().getOwner());

      ownerProperty.addChild(href);

      return ownerProperty;
   }

   private static HierarchicalProperty getPrincipalProperty(String principal)
   {
      HierarchicalProperty principalProperty = new HierarchicalProperty(PRINCIPAL);

      if (IdentityConstants.ANY.equals(principal))
      {
         HierarchicalProperty all = new HierarchicalProperty(ALL);
         principalProperty.addChild(all);
      }
      else
      {
         HierarchicalProperty href = new HierarchicalProperty(HREF);
         href.setValue(principal);
         principalProperty.addChild(href);
      }

      return principalProperty;
   }

   private static HierarchicalProperty getGrantProperty(List<String> grantList)
   {
      HierarchicalProperty grant = new HierarchicalProperty(GRANT);

      if (grantList.contains(PermissionType.ADD_NODE) || grantList.contains(PermissionType.SET_PROPERTY)
         || grantList.contains(PermissionType.REMOVE))
      {
         HierarchicalProperty privilege = new HierarchicalProperty(PRIVILEGE);
         privilege.addChild(new HierarchicalProperty(WRITE));
         grant.addChild(privilege);
      }

      if (grantList.contains(PermissionType.READ))
      {
         HierarchicalProperty privilege = new HierarchicalProperty(PRIVILEGE);
         privilege.addChild(new HierarchicalProperty(READ));
         grant.addChild(privilege);
      }

      return grant;
   }

}
