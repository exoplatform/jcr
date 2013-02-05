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
package org.exoplatform.services.jcr.access;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.<br/> Access Control List.
 * 
 * @author Gennady Azarenkov
 * @version $Id: AccessControlList.java 14556 2008-05-21 15:22:15Z pnedonosko $
 */

public class AccessControlList implements Externalizable
{

   private static final long serialVersionUID = 5848327750178729120L;

   public static final String DELIMITER = ";";

   private String owner;

   private final List<AccessControlEntry> accessList;

   public AccessControlList()
   {
      this(SystemIdentity.SYSTEM);
   }

   /**
    * Default ACL owned by ownerName.
    * 
    * @param ownerName
    *          owner name
    */
   AccessControlList(String ownerName)
   {
      this.owner = ownerName;
      this.accessList = new ArrayList<AccessControlEntry>();
      for (String str : PermissionType.ALL)
      {
         accessList.add(new AccessControlEntry(SystemIdentity.ANY, str));
      }
   }

   /**
    * Create ACL from owner name and collection of permission entries.
    * 
    * @param owner
    * @param accessList
    *          - permission entries
    */
   public AccessControlList(String owner, List<AccessControlEntry> accessList)
   {
      this.owner = owner;
      this.accessList = accessList;
   }

   public boolean hasPermissions()
   {
      return accessList != null;
   }

   public boolean hasOwner()
   {
      return owner != null;
   }

   public void addPermissions(String rawData) throws RepositoryException
   {
      StringTokenizer listTokenizer = new StringTokenizer(rawData, AccessControlList.DELIMITER);
      if (listTokenizer.countTokens() < 1)
         throw new RepositoryException("AccessControlList " + rawData + " is empty or have a bad format");

      while (listTokenizer.hasMoreTokens())
      {
         String entry = listTokenizer.nextToken();
         StringTokenizer entryTokenizer = new StringTokenizer(entry, AccessControlEntry.DELIMITER);
         if (entryTokenizer.countTokens() != 2)
            throw new RepositoryException("AccessControlEntry " + entry + " is empty or have a bad format");
         accessList.add(new AccessControlEntry(entryTokenizer.nextToken(), entryTokenizer.nextToken()));
      }
   }

   public void addPermissions(String identity, String[] perm) throws RepositoryException
   {
      for (String p : perm)
      {
         accessList.add(new AccessControlEntry(identity, p));
      }
   }

   public void removePermissions(String identity)
   {
      for (Iterator<AccessControlEntry> iter = accessList.iterator(); iter.hasNext();)
      {
         AccessControlEntry a = iter.next();
         if (a.getIdentity().equals(identity))
            iter.remove();
      }
   }

   public void removePermissions(String identity, String permission)
   {
      for (Iterator<AccessControlEntry> iter = accessList.iterator(); iter.hasNext();)
      {
         AccessControlEntry a = iter.next();
         if (a.getIdentity().equals(identity) && a.getPermission().equals(permission))
            iter.remove();
      }
   }

   /**
    * Get owner.
    * 
    * @return Returns the owner.
    */
   public String getOwner()
   {
      return owner;
   }

   public void setOwner(String owner)
   {
      this.owner = owner;
   }

   // Create safe copy of list <AccessControlEntry>
   public List<AccessControlEntry> getPermissionEntries()
   {
      List<AccessControlEntry> list = new ArrayList<AccessControlEntry>();
      for (int i = 0, length = accessList.size(); i < length; i++)
      {
         AccessControlEntry entry = accessList.get(i);
         list.add(new AccessControlEntry(entry.getIdentity(), entry.getPermission()));
      }
      return list;
   }

   public List<String> getPermissions(String identity)
   {
      List<String> permissions = new ArrayList<String>();
      for (int i = 0, length = accessList.size(); i < length; i++)
      {
         AccessControlEntry entry = accessList.get(i);
         if (entry.getIdentity().equals(identity))
            permissions.add(entry.getPermission());
      }
      return permissions;
   }

   public boolean equals(Object obj)
   {
      if (obj == this)
         return true;
      if (obj instanceof AccessControlList)
      {
         AccessControlList another = (AccessControlList)obj;

         // check owners, it may be null
         if (!((owner == null && another.owner == null) || (owner != null && owner.equals(another.owner))))
         {
            return false;
         }

         // check accessList
         List<AccessControlEntry> anotherAccessList = another.accessList;
         if (accessList == null && anotherAccessList == null)
         {
            return true;
         }
         else if (accessList != null && anotherAccessList != null && accessList.size() == anotherAccessList.size())
         {
            // check content of both accessLists
            for (int i = 0; i < accessList.size(); i++)
            {
               if (!accessList.get(i).getAsString().equals(anotherAccessList.get(i).getAsString()))
               {
                  return false;
               }
            }
            return true;
         }
         else
         {
            return false;
         }

         //return dump().equals(another.dump());
      }
      return false;
   }

   public String dump()
   {
      StringBuilder res = new StringBuilder("OWNER: ").append(owner != null ? owner : "null").append("\n");
      if (accessList != null)
      {
         for (AccessControlEntry a : accessList)
         {
            res.append(a.getAsString()).append("\n");
         }
      }
      else
      {
         res.append("null");
      }
      return res.toString();
   }

   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      // reading owner
      byte[] buf;
      int ownLength = in.readInt();
      if (ownLength != 0)
      {
         buf = new byte[ownLength];
         in.readFully(buf);
         this.owner = new String(buf, "UTF-8");
      }
      else
      {
         this.owner = null;
      }
      accessList.clear();
      // reading access control entrys size
      int listSize = in.readInt();
      for (int i = 0; i < listSize; i++)
      {
         // reading access control entrys identity
         buf = new byte[in.readInt()];
         in.readFully(buf);
         String ident = new String(buf, "UTF-8");
         // reading permission
         buf = new byte[in.readInt()];
         in.readFully(buf);
         String perm = new String(buf, "UTF-8");

         accessList.add(new AccessControlEntry(ident, perm));
      }
   }

   public void writeExternal(ObjectOutput out) throws IOException
   {
      // Writing owner
      if (owner != null)
      {
         out.writeInt(owner.getBytes().length);
         out.write(owner.getBytes());
      }
      else
      {
         out.writeInt(0);
      }

      // writing access control entrys size
      out.writeInt(accessList.size());

      for (AccessControlEntry entry : accessList)
      {
         // writing access control entrys identity
         out.writeInt(entry.getIdentity().getBytes().length);
         out.write(entry.getIdentity().getBytes());
         // writing permission
         out.writeInt(entry.getPermission().getBytes().length);
         out.write(entry.getPermission().getBytes());
      }
   }

   /**
    * Get access list size.
    * 
    * @return size of access list
    */
   public int getPermissionsSize()
   {
      return accessList.size();
   }

   /**
    * Special method for internal JCR use.
    * 
    * @return list of AccessControlEntry
    */
   List<AccessControlEntry> getPermissionsList()
   {
      return accessList;
   }
}
