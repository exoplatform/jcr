/*
 * Copyright (C) 2011 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.xml.importing;

import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: ACLInitializationHelper.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class ACLInitializationHelper
{

   public static AccessControlList initAcl(AccessControlList parentACL, String owner, List<String> exoPermissions)
   {
      boolean isOwneable = owner != null;
      boolean isPrivilegeable = exoPermissions != null;

      AccessControlList acl;
      if (isOwneable)
      {
         // has own owner
         if (isPrivilegeable)
         {
            // and permissions
            acl = new AccessControlList(owner, readACLPermisions(exoPermissions));
         }
         else if (parentACL != null)
         {
            // use permissions from existed parent
            acl = new AccessControlList(owner, parentACL.hasPermissions() ? parentACL.getPermissionEntries() : null);
         }
         else
         {
            // have to search nearest ancestor permissions in ACL manager
            // acl = new AccessControlList(owner,
            // traverseACLPermissions(cpid));
            acl = new AccessControlList(owner, null);
         }
      }
      else if (isPrivilegeable)
      {
         // has own permissions
         if (isOwneable)
         {
            // and owner
            acl = new AccessControlList(owner, readACLPermisions(exoPermissions));
         }
         else if (parentACL != null)
         {
            // use owner from existed parent
            acl = new AccessControlList(parentACL.getOwner(), readACLPermisions(exoPermissions));
         }
         else
         {
            // have to search nearest ancestor owner in ACL manager
            // acl = new AccessControlList(traverseACLOwner(cpid),
            // readACLPermisions(cid));
            acl = new AccessControlList(null, readACLPermisions(exoPermissions));
         }
      }
      else
      {
         if (parentACL != null)
            // construct ACL from existed parent ACL
            acl =
               new AccessControlList(parentACL.getOwner(), parentACL.hasPermissions()
                  ? parentACL.getPermissionEntries() : null);
         else
            // have to search nearest ancestor owner and permissions in ACL manager
            // acl = traverseACL(cpid);
            acl = null;
      }
      return acl;
   }

   /**
    * Return permission values or throw an exception. We assume the node is
    * mix:privilegeable.
    */
   private static List<AccessControlEntry> readACLPermisions(List<String> exoPermissions)
   {
      List<AccessControlEntry> naPermissions = new ArrayList<AccessControlEntry>();

      for (String perm : exoPermissions)
      {
         StringTokenizer parser = new StringTokenizer(perm, AccessControlEntry.DELIMITER);
         naPermissions.add(new AccessControlEntry(parser.nextToken(), parser.nextToken()));
      }

      return naPermissions;
   }

}
