/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.ext.organization;

import org.exoplatform.services.organization.ExtendedCloneable;
import org.exoplatform.services.organization.Membership;

/**
 * Created by The eXo Platform SAS.
 * 
 * Date: 24.07.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: MembershipImpl.java 76870 2011-11-22 10:38:54Z dkuleshov $
 */
public class MembershipImpl implements Membership, ExtendedCloneable
{

   /**
    * The group id.
    */
   private String groupId;

   /**
    * The membership type id.
    */
   private String membershipType;

   /**
    * The user name.
    */
   private String userName;

   /**
    * The identifier of the membership. Consists of 3 elements separated by comma:<br>
    * <li>group node identifier</li>
    * <li>user name</li>
    * <li>type name</li>
    */
   private String id;

   /**
    * MembershipImpl constructor.
    */
   MembershipImpl()
   {
   }

   /**
    * {@inheritDoc}
    */
   public String getGroupId()
   {
      return groupId;
   }

   /**
    * {@inheritDoc}
    */
   public String getId()
   {
      return id;
   }

   /**
    * {@inheritDoc}
    */
   public String getMembershipType()
   {
      return membershipType;
   }

   /**
    * {@inheritDoc}
    */
   public String getUserName()
   {
      return userName;
   }

   /**
    * Set membership identifier.
    */
   void setId(String id)
   {
      this.id = id;
   }

   /**
    * Set group id.
    */
   void setGroupId(String groupId)
   {
      this.groupId = groupId;
   }

   /**
    * Set user name.
    */
   void setUserName(String userName)
   {
      this.userName = userName;
   }

   /**
    * {@inheritDoc}
    */
   public void setMembershipType(String type)
   {
      membershipType = type;
   }

   /**
    * {@inheritDoc}
    */
   public String toString()
   {
      return "[groupId=" + getGroupId() + "][type=" + getMembershipType() + "][user=" + getUserName() + "]";
   }

   /**
    * {@inheritDoc}
    */
   public MembershipImpl clone()
   {
      try
      {
         return (MembershipImpl)super.clone();
      }
      catch (CloneNotSupportedException e)
      {
         return this;
      }
   }
}
