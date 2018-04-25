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
import org.exoplatform.services.organization.User;

import java.util.Date;

/**
 * Created by The eXo Platform SAS.
 * 
 * Date: 24.07.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: UserImpl.java 80140 2012-03-07 11:08:07Z aplotnikov $
 */
public class UserImpl implements User, ExtendedCloneable
{

   /**
    * The user's created date
    */
   private Date createdDate;

   /**
    * The email of the user
    */
   private String email;

   /**
    * The first name of the user
    */
   private String firstName;

   /**
    * The last login time of the user
    */
   private Date lastLoginTime;

   /**
    * The last name of the user
    */
   private String lastName;

   /**
    * The display name
    */
   private String displayName;

   /**
    * The password of the user
    */
   private transient String password;

   /**
    * The user name
    */
   private String userName;

   /**
    * The internal identifier of the user.
    */
   private String internalId;

   /**
    * Indicates whether the user is enabled or not
    */
   private boolean enabled = true;

   /**
    * UserImpl constructor.
    */
   UserImpl()
   {
   }

   /**
    * UserImpl constructor.
    */
   UserImpl(String name)
   {
      this.userName = name;
   }

   /**
    * {@inheritDoc}
    */
   public Date getCreatedDate()
   {
      return createdDate;
   }

   /**
    * {@inheritDoc}
    */
   public String getEmail()
   {
      return email;
   }

   /**
    * {@inheritDoc}
    */
   public String getFirstName()
   {
      return firstName;
   }

   /**
    * {@inheritDoc}
    */
   public String getDisplayName()
   {
      return displayName != null ? displayName : getFirstName() + " " + getLastName();
   }

   /**
    * {@inheritDoc}
    */
   public String getFullName()
   {
      return getDisplayName();
   }

   /**
    * {@inheritDoc}
    */
   public Date getLastLoginTime()
   {
      return lastLoginTime;
   }

   /**
    * {@inheritDoc}
    */
   public String getLastName()
   {
      return lastName;
   }

   /**
    * {@inheritDoc}
    */
   public String getOrganizationId()
   {
      return null;
   }

   /**
    * {@inheritDoc}
    */
   public String getPassword()
   {
      return password;
   }

   /**
    * {@inheritDoc}
    */
   public String getUserName()
   {
      return userName;
   }

   /**
    * Returns internal identifier.
    */
   String getInternalId()
   {
      return internalId;
   }

   /**
    * {@inheritDoc}
    */
   public void setCreatedDate(Date t)
   {
      createdDate = t;
   }

   /**
    * {@inheritDoc}
    */
   public void setEmail(String s)
   {
      email = s;
   }

   /**
    * {@inheritDoc}
    */
   public void setDisplayName(String displayName)
   {
      this.displayName = displayName;
   }

   /**
    * {@inheritDoc}
    */
   public void setFirstName(String s)
   {
      firstName = s;
   }

   /**
    * {@inheritDoc}
    */
   public void setFullName(String s)
   {
      setDisplayName(s);
   }

   /**
    * {@inheritDoc}
    */
   public void setLastLoginTime(Date t)
   {
      lastLoginTime = t;
   }

   /**
    * {@inheritDoc}
    */
   public void setLastName(String s)
   {
      lastName = s;
   }

   /**
    * {@inheritDoc}
    */
   public void setOrganizationId(String s)
   {
   }

   /**
    * {@inheritDoc}
    */
   public void setPassword(String s)
   {
      password = s;
   }

   /**
    * {@inheritDoc}
    */
   public void setUserName(String s)
   {
      userName = s;
   }

   /**
    * @return <code>true</code> if the user is enabled, <code>false</code> otherwise
    */
   public boolean isEnabled()
   {
      return enabled;
   }

   /**
    * Set it to <code>true</code> to enable the user, 
    * <code>false</code> otherwise
    */
   void setEnabled(Boolean enabled)
   {
      this.enabled = enabled;
   }

   /**
    * Set internal id.
    */
   void setInternalId(String internalId)
   {
      this.internalId = internalId;
   }


   /**
    * {@inheritDoc}
    */
   public String toString()
   {
      return "[user=" + getUserName() + "]";
   }

   /**
    * {@inheritDoc}
    */
   public UserImpl clone()
   {
      UserImpl ui;
      try
      {
         ui = (UserImpl)super.clone();
         if (createdDate != null)
         {
            ui.createdDate = (Date)createdDate.clone();
         }
         if (lastLoginTime != null)
         {
            ui.lastLoginTime = (Date)lastLoginTime.clone();
         }
      }
      catch (CloneNotSupportedException e)
      {
         return this;
      }

      return ui;
   }

}
