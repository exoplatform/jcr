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

package org.exoplatform.services.jcr.webdav.command;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.common.util.HierarchicalProperty;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.webdav.command.acl.ACLProperties;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.IdentityConstants;

import java.security.AccessControlException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.Response;

/**
 * 
 * Created by The eXo Platform SAS.
 * ACL method implementation for Web Distributed Authoring and Versioning (WebDAV)
 * protocol extension - Access Control Protocol: RFC3744.
 * 
 * @author <a href="mailto:gavrikvetal@gmail.com">Vitaliy Gulyy</a>
 * @version $
 */

public class AclCommand
{

   /**
    * logger.
    */
   private static Log log = ExoLogger.getLogger(AclCommand.class);

   /**
    * Applies changes for JCR node's {@link AccessControlList} 
    * according to WebDAV ACL method request body represented by {@link HierarchicalProperty} 
    * @param session - actual session
    * @param path - absolute path to jcr node
    * @param requestBody - tree like structure to contain ACL method request body
    * @return response - http response for ACL method request
    */
   public Response acl(Session session, String path, HierarchicalProperty requestBody)
   {
      NodeImpl node;
      try
      {
         node = (NodeImpl)session.getItem(path);

         boolean isSessionToBeSaved = false;

         boolean nodeIsNotCheckedOut = node.isNodeType("mix:versionable") && !node.isCheckedOut();

         // to set ACL the node necessarily must be exo:owneable 
         if (!node.isNodeType("exo:owneable"))
         {
            if (nodeIsNotCheckedOut)
            {
               node.checkout();
            }
            node.addMixin("exo:owneable");
            isSessionToBeSaved = true;
         }

         // to set ACL the node necessarily must be exo:privilegeable
         if (!node.isNodeType("exo:privilegeable"))
         {
            if (nodeIsNotCheckedOut)
            {
               node.checkout();
            }
            node.addMixin("exo:privilegeable");
            isSessionToBeSaved = true;
         }

         if (isSessionToBeSaved)
         {
            session.save();
            if (nodeIsNotCheckedOut)
            {
               node.checkin();
               session.save();
            }
         }


         changeNodeACL(node, requestBody);

      }
      catch (PathNotFoundException e)
      {
         return Response.status(HTTPStatus.NOT_FOUND).entity(e.getMessage()).build();
      }
      catch (RepositoryException exc)
      {
         log.error(exc.getMessage(), exc);
         return Response.status(HTTPStatus.INTERNAL_ERROR).entity(exc.getMessage()).build();
      }
      catch (AccessControlException exc)
      {
         log.error(exc.getMessage(), exc);
         return Response.status(HTTPStatus.FORBIDDEN).entity(exc.getMessage()).build();
      }
      catch (IllegalArgumentException exc)
      {
         log.error(exc.getMessage(), exc);
         return Response.status(HTTPStatus.BAD_REQUEST).entity(exc.getMessage()).build();
      }
      catch (Exception exc)
      {
         log.error(exc.getMessage(), exc);
         return Response.status(HTTPStatus.BAD_REQUEST).build();
      }

      return Response.status(HTTPStatus.OK).build();
   }

   /**
    * Changes JCR node ACL by addition or removal mentioned in {@link HierarchicalProperty} requestBody permissions.
    * @param node - node to change its ACL 
    * @param requestBody - tree like structure to contain ACL method request body
    * @throws AccessControlException
    * @throws RepositoryException
    */
   private void changeNodeACL(NodeImpl node, HierarchicalProperty requestBody) throws AccessControlException,
      RepositoryException
   {
      Map<String, String[]> permissionsToGrant = new HashMap<String, String[]>();
      Map<String, String[]> permissionsToDeny = new HashMap<String, String[]>();

      for (HierarchicalProperty ace : requestBody.getChildren())
      {

         HierarchicalProperty principalProperty = ace.getChild(ACLProperties.PRINCIPAL);
         // each ace element must contain principal element
         // TODO invert element is not implemented
         // <!ELEMENT ace ((principal | invert), (grant|deny), protected?, inherited?)>
         if (principalProperty == null)
         {
            throw new IllegalArgumentException("Malformed ace element (seems that no principal element specified)");
         }

         String principal;

         if (principalProperty.getChild(ACLProperties.HREF) != null)
         {
            principal = principalProperty.getChild(ACLProperties.HREF).getValue();
         }
         else if (principalProperty.getChild(ACLProperties.ALL) != null)
         {
            principal = IdentityConstants.ANY;
         }

         // each principal must contain either href or all element
         // TODO authenticated, unauthenticated, property, self are not implemented
         // <!ELEMENT principal (href | all | authenticated | unauthenticated | property | self)> 
         else
         {
            throw new IllegalArgumentException("Malformed principal element");
         }

         HierarchicalProperty denyProperty = ace.getChild(ACLProperties.DENY);
         HierarchicalProperty grantProperty = ace.getChild(ACLProperties.GRANT);

         // each ace element must contain at least one grant or deny property
         // <!ELEMENT ace ((principal | invert), (grant|deny), protected?, inherited?)> 
         if (denyProperty == null && grantProperty == null)
         {
            throw new IllegalArgumentException("Malformed ace element (seems that no deny|grant element specified)");
         }
         
         if (denyProperty != null)
         {
            permissionsToDeny.put(principal, getPermissions(denyProperty));
         }

         if (grantProperty != null)
         {
            permissionsToGrant.put(principal, getPermissions(grantProperty));
         }

         // request must not grant and deny the same privilege in a single ace
         // http://www.webdav.org/specs/rfc3744.html#rfc.section.8.1.5
         if (permissionsToDeny.size() != 0 && permissionsToGrant.size() != 0)
         {
            for (String denyPermission : permissionsToDeny.get(principal))
            {
               for (String grantPermission : permissionsToGrant.get(principal))
               {
                  if (denyPermission.equals(grantPermission))
                  {
                     throw new IllegalArgumentException(
                        "Malformed ace element (seems that a client is trying to grant and denay the same privilege in a single ace)");

                  }
               }
            }
         }
      }
      if (permissionsToDeny.size() != 0)
      {
         for (Entry<String, String[]> entry : permissionsToDeny.entrySet())
         {
            for (String p : entry.getValue())
            {
               node.removePermission(entry.getKey(), p);
            }
         }
         node.getSession().save();
      }
      if (permissionsToGrant.size() != 0)
      {
         for (Entry<String, String[]> entry : permissionsToGrant.entrySet())
         {
            node.setPermission(entry.getKey(), entry.getValue());
         }
         node.getSession().save();
      }
   }

   /**
    * Processes {@link HierarchicalProperty} instance, representing grant or deny 
    * element of ACL request body, to pull out permissions set represented as {@link String} array.
    * @param property
    * @return String[] - permissions set
    */
   private String[] getPermissions(HierarchicalProperty property)
   {
      Set<String> permissionsToBeChanged = new HashSet<String>();

      // grant|deny element must have at least one privilege element
      // <!ELEMENT grant (privilege+)> 
      // <!ELEMENT deny (privilege+)> 
      if (property.getChildren().size() == 0)
      {
         throw new IllegalArgumentException("Malformed grant|deny element (seems that no privilige is specified)");
      }

      for (HierarchicalProperty propertyRunner : property.getChildren())
      {
         HierarchicalProperty permissionProperty;

         // obviously privilege must be single named
         // <!ELEMENT privilege ANY>
         if (ACLProperties.PRIVILEGE.equals(propertyRunner.getName()))
         {
            if (propertyRunner.getChildren().size() > 1)
            {
               throw new IllegalArgumentException(
                  "Malformed privilege name (element privilege must contain only one element)");
            }
            permissionProperty = propertyRunner.getChild(0);
         }
         else
         {
            permissionProperty = propertyRunner;
         }

         if (ACLProperties.READ.equals(permissionProperty.getName()))
         {
            permissionsToBeChanged.add(PermissionType.READ);

         }
         else if (ACLProperties.WRITE.equals(permissionProperty.getName()))
         {
            permissionsToBeChanged.add(PermissionType.ADD_NODE);
            permissionsToBeChanged.add(PermissionType.SET_PROPERTY);
            permissionsToBeChanged.add(PermissionType.REMOVE);

         }
         else if (ACLProperties.ALL.equals(permissionProperty.getName()))
         {
            permissionsToBeChanged.add(PermissionType.READ);
            permissionsToBeChanged.add(PermissionType.ADD_NODE);
            permissionsToBeChanged.add(PermissionType.SET_PROPERTY);
            permissionsToBeChanged.add(PermissionType.REMOVE);
         }
         // in case privilege with specified name is unsupported
         // or simply incorrect privilege name
         else
         {
            throw new IllegalArgumentException("Malformed privilege element (unsupported privilege name)");
         }
      }
      return permissionsToBeChanged.toArray(new String[0]);
      
   }

}
