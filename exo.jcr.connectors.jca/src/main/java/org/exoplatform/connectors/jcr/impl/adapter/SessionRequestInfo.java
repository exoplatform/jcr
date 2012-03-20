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
package org.exoplatform.connectors.jcr.impl.adapter;

import javax.resource.spi.ConnectionRequestInfo;

/**
 * @author <a href="mailto:nfilotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public class SessionRequestInfo implements ConnectionRequestInfo
{

   /**
    * The name of the expected workspace
    */
   private final String workspace;

   /**
    * The user name to use for the authentication
    */
   private final String userName;

   /**
    * The password to use for the authentication
    */
   private final String password;

   /**
    * The default constructor
    */
   SessionRequestInfo(String workspace, String userName, String password)
   {
      this.workspace = workspace;
      this.userName = userName;
      this.password = password;
   }

   /**
    * @return the workspace
    */
   public String getWorkspace()
   {
      return workspace;
   }

   /**
    * @return the userName
    */
   public String getUserName()
   {
      return userName;
   }

   /**
    * @return the password
    */
   public String getPassword()
   {
      return password;
   }

   /**
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString()
   {
      return "SessionRequestInfo [workspace=" + workspace + ", userName=" + userName + ", password=" + password + "]";
   }

   /**
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode()
   {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((workspace == null) ? 0 : workspace.hashCode());
      result = prime * result + ((userName == null) ? 0 : userName.hashCode());
      result = prime * result + ((password == null) ? 0 : password.hashCode());
      return result;
   }

   /**
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      SessionRequestInfo other = (SessionRequestInfo)obj;
      if (workspace == null)
      {
         if (other.workspace != null)
            return false;
      }
      else if (!workspace.equals(other.workspace))
         return false;
      if (userName == null)
      {
         if (other.userName != null)
            return false;
      }
      else if (!userName.equals(other.userName))
         return false;
      if (password == null)
      {
         if (other.password != null)
            return false;
      }
      else if (!password.equals(other.password))
         return false;
      return true;
   }
}
