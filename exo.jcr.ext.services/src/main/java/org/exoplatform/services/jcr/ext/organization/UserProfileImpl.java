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
import org.exoplatform.services.organization.UserProfile;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by The eXo Platform SAS Date: 24.07.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: UserProfileImpl.java 78288 2011-12-30 09:39:45Z aplotnikov $
 */
public class UserProfileImpl implements UserProfile, ExtendedCloneable
{

   /**
    * The profile attributes.
    */
   private Map<String, String> attributes;

   /**
    * The user name.
    */
   private String userName;

   /**
    * UserProfileImpl constructor.
    */
   public UserProfileImpl()
   {
      attributes = new HashMap<String, String>();
   }

   /**
    * UserProfileImpl constructor.
    */
   public UserProfileImpl(String name)
   {
      attributes = new HashMap<String, String>();
      userName = name;
   }

   /**
    * {@inheritDoc}
    */
   public String getAttribute(String attName)
   {
      return attributes.get(attName);
   }

   /**
    * {@inheritDoc}
    */
   public Map<String, String> getUserInfoMap()
   {
      return attributes;
   }

   /**
    * {@inheritDoc}
    */
   public String getUserName()
   {
      return userName;
   }

   /**
    * {@inheritDoc}
    */
   public void setAttribute(String key, String value)
   {
      attributes.put(key, value);
   }

   /**
    * {@inheritDoc}
    */
   public void setUserInfoMap(Map<String, String> map)
   {
      attributes = map;
   }

   /**
    * {@inheritDoc}
    */
   public void setUserName(String username)
   {
      userName = username;
   }

   /**
    * {@inheritDoc}
    */
   public String toString()
   {
      StringBuilder result = new StringBuilder();

      Object[] keys = getUserInfoMap().keySet().toArray();
      for (int i = 0; i < keys.length; i++)
      {
         String key = (String)keys[i];
         result.append("[").append(key).append("=").append(getAttribute(key)).append("]");
      }

      return result.toString();
   }

   /**
    * {@inheritDoc}
    */
   public UserProfileImpl clone()
   {
      UserProfileImpl profile;
      try
      {
         profile = (UserProfileImpl)super.clone();
         profile.attributes = new HashMap<String, String>(attributes);
      }
      catch (CloneNotSupportedException e)
      {
         return this;
      }

      return profile;
   }
}
