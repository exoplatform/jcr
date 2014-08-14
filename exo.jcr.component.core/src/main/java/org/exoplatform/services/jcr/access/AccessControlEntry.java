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

import org.exoplatform.services.security.MembershipEntry;

/**
 * Created by The eXo Platform SAS<br>
 *
 * A data structure consisting of a user name, group and a permission granted to it.
 * 
 * @author Gennady Azarenkov
 * @version $Id: AccessControlEntry.java 14464 2008-05-19 11:05:20Z pnedonosko $
 * @LevelAPI Experimental
 */
public class AccessControlEntry
{

   private final String identity;

   private final String permission;

   private volatile MembershipEntry membership;

   public static final String DELIMITER = " ";

   private Integer hashcode = null;

   private String asString = null;

   public AccessControlEntry(String identity, String permission)
   {
      this.identity = identity;
      this.permission = permission;
   }
   /**
    * @return returns the identity
    */
   public String getIdentity()
   {
      return identity;
   }
   /**
    * @return returns the permission type
    */
   public String getPermission()
   {
      return permission;
   }
   /**
    * @return returns the membership
    */
   public MembershipEntry getMembershipEntry()
   {
      if (membership == null)
      {
         synchronized (this)
         {
            if (membership == null)
            {
               membership = MembershipEntry.parse(getIdentity());
            }
         }
      }
      return membership;
   }
   
   public String getAsString()
   {
      if (asString == null)
      {
         asString = identity + AccessControlEntry.DELIMITER + permission;
      }

      return asString;
   }

   @Override
   public int hashCode()
   {
      if (hashcode == null)
      {
         hashcode = getAsString().hashCode();
      }

      return hashcode;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj == this)
         return true;
      if (obj instanceof AccessControlEntry)
      {
         AccessControlEntry another = (AccessControlEntry)obj;
         return getAsString().equals(another.getAsString());
      }
      return false;
   }

   @Override
   public String toString()
   {
      return super.toString() + " (" + getAsString() + ")";
   }

   /**
    * Factory method.
    */
   public static AccessControlEntry parse(String pstring)
   {
      if (pstring == null)
         throw new IllegalArgumentException("The string representation of an AccessControlEntry cannot be null");
      int index = pstring.lastIndexOf(AccessControlEntry.DELIMITER);
      if (index == -1)
         throw new IllegalArgumentException("The string representation of an AccessControlEntry must contain the delimiter '" + AccessControlEntry.DELIMITER + "'");
      String identity = pstring.substring(0, index);
      String permission = pstring.substring(index + 1);
      return new AccessControlEntry(identity, permission);
   }
}
